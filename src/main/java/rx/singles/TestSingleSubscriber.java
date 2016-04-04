package rx.singles;

import rx.Observer;
import rx.SingleSubscriber;
import rx.Subscriber;
import rx.annotations.Beta;
import rx.exceptions.CompositeException;
import rx.observers.TestObserver;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * A {@code TestSingleSubscriber} is a variety of {@link SingleSubscriber} that you can use for unit testing,
 * to perform assertions, inspect received events, or wrap a mocked {@code SingleSubscriber}.
 *
 * @param <T> the value type
 */
@Beta
public class TestSingleSubscriber<T> extends SingleSubscriber<T> {

    private final TestObserver<T> testObserver;
    private final CountDownLatch latch = new CountDownLatch(1);
    private volatile Thread lastSeenThread;
    /**
     * The shared no-op observer.
     */
    private static final Observer<Object> INERT = new Observer<Object>() {

        @Override
        public void onCompleted() {
            // do nothing
        }

        @Override
        public void onError(Throwable e) {
            // do nothing
        }

        @Override
        public void onNext(Object t) {
            // do nothing
        }

    };

    /**
     * Constructs a TestSingleSubscriber with no delegation.
     */
    @SuppressWarnings("unchecked")
    public TestSingleSubscriber() {
        this((Observer<T>) INERT);
    }

    /**
     * Constructs a TestSingleSubscriber with a delegate Observer to wrap.
     *
     * @param delegate the Observer instance to wrap
     * @throws NullPointerException if delegate is null
     */
    public TestSingleSubscriber(Observer<T> delegate) {
        if (delegate == null) {
            throw new NullPointerException();
        }
        this.testObserver = new TestObserver<T>(delegate);
    }

    /**
     * Factory method to construct a TestSingleSubscriber with no delegation.
     *
     * @param <T> the value type
     * @return the created TestSingleSubscriber instance
     */
    public static <T> TestSingleSubscriber<T> create() {
        return new TestSingleSubscriber<T>();
    }

    /**
     * Factory method to construct a TestSingleSubscriber which delegates events to the given Subscriber.
     *
     * @param <T>      the value type
     * @param delegate the subscriber to delegate events to
     * @return the created TestSingleSubscriber instance
     * @throws NullPointerException if delegate is null
     */
    public static <T> TestSingleSubscriber<T> create(Subscriber<T> delegate) {
        return new TestSingleSubscriber<T>(delegate);
    }

    /**
     * Factory method to construct a TestSingleSubscriber which delegates events to the given Observer.
     *
     * @param <T>      the value type
     * @param delegate the observer to delegate events to
     * @return the created TestSingleSubscriber instance
     * @throws NullPointerException if delegate is null
     */
    public static <T> TestSingleSubscriber<T> create(Observer<T> delegate) {
        return new TestSingleSubscriber<T>(delegate);
    }

    /**
     * Notifies the SingleSubscriber with a single item and that the {@code Single} has finished sending
     * push-based notifications.
     * <p>
     * The {@code Single} will not call this method if it calls {@link #onError}.
     *
     * @param t the item emitted by the Single
     */
    @Override
    public void onSuccess(T t) {
        try {
            lastSeenThread = Thread.currentThread();
            testObserver.onNext(t);
            testObserver.onCompleted();
        } finally {
            latch.countDown();
        }
    }

    /**
     * Notifies the Subscriber that the {@code Single} has experienced an error condition.
     * <p>
     * If the {@code Single} calls this method, it will not thereafter call {@link #onSuccess}.
     *
     * @param error the exception encountered by the Single
     */
    @Override
    public void onError(Throwable error) {
        try {
            lastSeenThread = Thread.currentThread();
            testObserver.onError(error);
        } finally {
            latch.countDown();
        }
    }

    /**
     * Returns the {@link Throwable}s this {@link SingleSubscriber} was notified of via {@link #onError} as a
     * {@link List}.
     *
     * @return a list of the Throwables that were passed to this SingleSubscriber's {@link #onError} method
     */
    public List<Throwable> getOnErrorEvents() {
        return testObserver.getOnErrorEvents();
    }

    /**
     * Returns the item observed by this {@link SingleSubscriber}.
     *
     * @return an item observed by this SingleSubscriber
     */
    public T getOnSuccessEvent() {
        List<T> onNextEvents = testObserver.getOnNextEvents();
        if (onNextEvents.size() != 1) {
            throw new AssertionError("Number of items does not match. Expected: 1  Actual: " + onNextEvents.size());
        }
        return onNextEvents.get(0);
    }

    /**
     * Asserts that there is a received onSuccess event and that it marks the emission of a specific item.
     *
     * @param value the item to check
     * @throws AssertionError if the Single does not emit only the single item specified by {@code value}
     */
    public void assertValue(T value) {
        testObserver.assertReceivedOnNext(Collections.singletonList(value));
    }

    /**
     * Asserts that a single terminal event occurred, either {@link #onSuccess} or {@link #onError}.
     *
     * @throws AssertionError if not exactly one terminal event notification was received
     */
    public void assertTerminalEvent() {
        testObserver.assertTerminalEvent();
    }

    /**
     * Blocks until this {@link SingleSubscriber} receives a notification that the {@code Single} is complete
     * (either an {@code onSuccess} or {@code onError} notification).
     *
     * @throws RuntimeException if the Subscriber is interrupted before the Single is able to complete
     */
    public void awaitTerminalEvent() {
        try {
            latch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted", e);
        }
    }

    /**
     * Blocks until this {@link SingleSubscriber} receives a notification that the {@code Single} is complete
     * (either an {@code onSuccess} or {@code onError} notification), or until a timeout expires.
     *
     * @param timeout the duration of the timeout
     * @param unit    the units in which {@code timeout} is expressed
     * @throws RuntimeException if the Subscriber is interrupted before the Single is able to complete
     */
    public void awaitTerminalEvent(long timeout, TimeUnit unit) {
        try {
            latch.await(timeout, unit);
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted", e);
        }
    }

    /**
     * Blocks until this {@link SingleSubscriber} receives a notification that the {@code Single} is complete
     * (either an {@code onSuccess} or {@code onError} notification), or until a timeout expires; if the
     * SingleSubscriber is interrupted before either of these events take place, this method unsubscribes the
     * SingleSubscriber from the Single). If timeout expires then the SingleSubscriber is unsubscribed from the Single.
     *
     * @param timeout the duration of the timeout
     * @param unit    the units in which {@code timeout} is expressed
     */
    public void awaitTerminalEventAndUnsubscribeOnTimeout(long timeout, TimeUnit unit) {
        try {
            boolean result = latch.await(timeout, unit);
            if (!result) {
                // timeout occurred
                unsubscribe();
            }
        } catch (InterruptedException e) {
            unsubscribe();
        }
    }

    /**
     * Returns the last thread that was in use when an item or notification was received by this
     * {@link SingleSubscriber}.
     *
     * @return the {@code Thread} on which this SingleSubscriber last received an item
     * or notification from the Single it is subscribed to
     */
    public Thread getLastSeenThread() {
        return lastSeenThread;
    }

    /**
     * Asserts that there is exactly one success event.
     *
     * @throws AssertionError if there were zero, or more than one, onSuccess events
     */
    public void assertSuccess() {
        int s = testObserver.getOnCompletedEvents().size();
        int ns = testObserver.getOnNextEvents().size();
        if (s == 0 || ns == 0) {
            throw new AssertionError("Not success!");
        } else if (s > 1 || ns > 1) {
            throw new AssertionError("Success multiple times: " + s);
        }
    }

    /**
     * Asserts that there is no success event.
     *
     * @throws AssertionError if there were one or more than one onSuccess events
     */
    public void assertNotSuccess() {
        int s = testObserver.getOnCompletedEvents().size();
        int ns = testObserver.getOnNextEvents().size();
        if (s == 1 || ns == 1) {
            throw new AssertionError("Success!");
        } else if (s > 1 || ns > 1) {
            throw new AssertionError("Success multiple times: " + s);
        }
    }

    /**
     * Asserts that there is exactly one error event which is a subclass of the given class.
     *
     * @param clazz the class to check the error against.
     * @throws AssertionError if there were zero, or more than one, onError events, or if the single onError
     *                        event did not carry an error of a subclass of the given class
     */
    public void assertError(Class<? extends Throwable> clazz) {
        List<Throwable> err = testObserver.getOnErrorEvents();
        if (err.size() == 0) {
            throw new AssertionError("No errors");
        } else if (err.size() > 1) {
            AssertionError ae = new AssertionError("Multiple errors: " + err.size());
            ae.initCause(new CompositeException(err));
            throw ae;
        } else if (!clazz.isInstance(err.get(0))) {
            AssertionError ae = new AssertionError("Exceptions differ; expected: " + clazz + ", actual: " + err.get(0));
            ae.initCause(err.get(0));
            throw ae;
        }
    }

    /**
     * Asserts that there is a single onError event with the exact exception.
     *
     * @param throwable the throwable to check
     * @throws AssertionError if there were zero, or more than one, onError events, or if the single onError
     *                        event did not carry an error that matches the specified throwable
     */
    public void assertError(Throwable throwable) {
        List<Throwable> err = testObserver.getOnErrorEvents();
        if (err.size() == 0) {
            throw new AssertionError("No errors");
        } else if (err.size() > 1) {
            AssertionError ae = new AssertionError("Multiple errors: " + err.size());
            ae.initCause(new CompositeException(err));
            throw ae;
        } else if (!throwable.equals(err.get(0))) {
            AssertionError ae = new AssertionError("Exceptions differ; expected: " + throwable + ", actual: " + err.get(0));
            ae.initCause(err.get(0));
            throw ae;
        }
    }

    /**
     * Asserts that there are no onError and onSuccess events.
     *
     * @throws AssertionError if there was either an onError or onSuccess event
     */
    public void assertNoTerminalEvent() {
        List<Throwable> err = testObserver.getOnErrorEvents();
        int s = testObserver.getOnCompletedEvents().size();
        if (err.size() > 0 || s > 0) {
            if (err.isEmpty()) {
                throw new AssertionError("Found " + err.size() + " errors and " + s + " completion events instead of none");
            } else if (err.size() == 1) {
                AssertionError ae = new AssertionError("Found " + err.size() + " errors and " + s + " completion events instead of none");
                ae.initCause(err.get(0));
                throw ae;
            } else {
                AssertionError ae = new AssertionError("Found " + err.size() + " errors and " + s + " completion events instead of none");
                ae.initCause(new CompositeException(err));
                throw ae;
            }
        }
    }

    /**
     * Asserts that this {@code SingleSubscriber} is unsubscribed.
     *
     * @throws AssertionError if this {@code SingleSubscriber} is not unsubscribed
     */
    public void assertUnsubscribed() {
        if (!isUnsubscribed()) {
            throw new AssertionError("Not unsubscribed.");
        }
    }

    /**
     * Asserts that this {@code SingleSubscriber} has received no {@code onError} notifications.
     *
     * @throws AssertionError if this {@code SingleSubscriber} has received one or more {@code onError} notifications
     */
    public void assertNoErrors() {
        List<Throwable> onErrorEvents = getOnErrorEvents();
        if (onErrorEvents.size() > 0) {
            AssertionError ae = new AssertionError("Unexpected onError events: " + getOnErrorEvents().size());
            if (onErrorEvents.size() == 1) {
                ae.initCause(getOnErrorEvents().get(0));
            } else {
                ae.initCause(new CompositeException(onErrorEvents));
            }
            throw ae;
        }
    }
}
