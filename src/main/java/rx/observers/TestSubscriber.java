/**
 * Copyright 2014 Netflix, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package rx.observers;

import java.util.*;
import java.util.concurrent.*;

import rx.*;
import rx.Observer;
import rx.exceptions.CompositeException;

/**
 * A {@code TestSubscriber} is a variety of {@link Subscriber} that you can use for unit testing, to perform
 * assertions, inspect received events, or wrap a mocked {@code Subscriber}.
 * @param <T> the value type
 */
public class TestSubscriber<T> extends Subscriber<T> {

    private final TestObserver<T> testObserver;
    private final CountDownLatch latch = new CountDownLatch(1);
    private volatile Thread lastSeenThread;
    /** The shared no-op observer. */
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
     * Constructs a TestSubscriber with the initial request to be requested from upstream.
     *
     * @param initialRequest the initial request value, negative value will revert to the default unbounded behavior
     * @since 1.1.0
     */
    @SuppressWarnings("unchecked")
    public TestSubscriber(long initialRequest) {
        this((Observer<T>)INERT, initialRequest);
    }
    
    /**
     * Constructs a TestSubscriber with the initial request to be requested from upstream
     * and a delegate Observer to wrap.
     *
     * @param initialRequest the initial request value, negative value will revert to the default unbounded behavior
     * @param delegate the Observer instance to wrap
     * @throws NullPointerException if delegate is null
     * @since 1.1.0
     */
    public TestSubscriber(Observer<T> delegate, long initialRequest) {
        if (delegate == null) {
            throw new NullPointerException();
        }
        this.testObserver = new TestObserver<T>(delegate);
        if (initialRequest >= 0L) {
            this.request(initialRequest);
        }
    }

    /**
     * Constructs a TestSubscriber which requests Long.MAX_VALUE and delegates events to
     * the given Subscriber.
     * @param delegate the subscriber to delegate to.
     * @throws NullPointerException if delegate is null
     * @since 1.1.0
     */
    public TestSubscriber(Subscriber<T> delegate) {
        this(delegate, -1);
    }

    /**
     * Constructs a TestSubscriber which requests Long.MAX_VALUE and delegates events to
     * the given Observer.
     * @param delegate the observer to delegate to.
     * @throws NullPointerException if delegate is null
     * @since 1.1.0
     */
    public TestSubscriber(Observer<T> delegate) {
        this(delegate, -1);
    }

    /**
     * Constructs a TestSubscriber with an initial request of Long.MAX_VALUE and no delegation.
     */
    public TestSubscriber() {
        this(-1);
    }

    /**
     * Factory method to construct a TestSubscriber with an initial request of Long.MAX_VALUE and no delegation.
     * @param <T> the value type
     * @return the created TestSubscriber instance
     * @since 1.1.0
     */
    public static <T> TestSubscriber<T> create() {
        return new TestSubscriber<T>();
    }
    
    /**
     * Factory method to construct a TestSubscriber with the given initial request amount and no delegation.
     * @param <T> the value type
     * @param initialRequest the initial request amount, negative values revert to the default unbounded mode
     * @return the created TestSubscriber instance
     * @since 1.1.0
     */
    public static <T> TestSubscriber<T> create(long initialRequest) {
        return new TestSubscriber<T>(initialRequest);
    }
    
    /**
     * Factory method to construct a TestSubscriber which delegates events to the given Observer and
     * issues the given initial request amount.
     * @param <T> the value type
     * @param delegate the observer to delegate events to
     * @param initialRequest the initial request amount, negative values revert to the default unbounded mode
     * @return the created TestSubscriber instance
     * @throws NullPointerException if delegate is null
     * @since 1.1.0
     */
    public static <T> TestSubscriber<T> create(Observer<T> delegate, long initialRequest) {
        return new TestSubscriber<T>(delegate, initialRequest);
    }

    /**
     * Factory method to construct a TestSubscriber which delegates events to the given Subscriber and
     * an issues an initial request of Long.MAX_VALUE.
     * @param <T> the value type
     * @param delegate the subscriber to delegate events to
     * @return the created TestSubscriber instance
     * @throws NullPointerException if delegate is null
     * @since 1.1.0
     */
    public static <T> TestSubscriber<T> create(Subscriber<T> delegate) {
        return new TestSubscriber<T>(delegate);
    }
    
    /**
     * Factory method to construct a TestSubscriber which delegates events to the given Observer and
     * an issues an initial request of Long.MAX_VALUE.
     * @param <T> the value type
     * @param delegate the observer to delegate events to
     * @return the created TestSubscriber instance
     * @throws NullPointerException if delegate is null
     * @since 1.1.0
     */
    public static <T> TestSubscriber<T> create(Observer<T> delegate) {
        return new TestSubscriber<T>(delegate);
    }
    
    /**
     * Notifies the Subscriber that the {@code Observable} has finished sending push-based notifications.
     * <p>
     * The {@code Observable} will not call this method if it calls {@link #onError}.
     */
    @Override
    public void onCompleted() {
        try {
            lastSeenThread = Thread.currentThread();
            testObserver.onCompleted();
        } finally {
            latch.countDown();
        }
    }

    /**
     * Returns the {@link Notification}s representing each time this {@link Subscriber} was notified of sequence
     * completion via {@link #onCompleted}, as a {@link List}.
     *
     * @return a list of Notifications representing calls to this Subscriber's {@link #onCompleted} method
     */
    public List<Notification<T>> getOnCompletedEvents() {
        return testObserver.getOnCompletedEvents();
    }

    /**
     * Notifies the Subscriber that the {@code Observable} has experienced an error condition.
     * <p>
     * If the {@code Observable} calls this method, it will not thereafter call {@link #onNext} or
     * {@link #onCompleted}.
     * 
     * @param e
     *          the exception encountered by the Observable
     */
    @Override
    public void onError(Throwable e) {
        try {
            lastSeenThread = Thread.currentThread();
            testObserver.onError(e);
        } finally {
            latch.countDown();
        }
    }

    /**
     * Returns the {@link Throwable}s this {@link Subscriber} was notified of via {@link #onError} as a
     * {@link List}.
     *
     * @return a list of the Throwables that were passed to this Subscriber's {@link #onError} method
     */
    public List<Throwable> getOnErrorEvents() {
        return testObserver.getOnErrorEvents();
    }

    /**
     * Provides the Subscriber with a new item to observe.
     * <p>
     * The {@code Observable} may call this method 0 or more times.
     * <p>
     * The {@code Observable} will not call this method again after it calls either {@link #onCompleted} or
     * {@link #onError}.
     * 
     * @param t
     *          the item emitted by the Observable
     */
    @Override
    public void onNext(T t) {
        lastSeenThread = Thread.currentThread();
        testObserver.onNext(t);
    }
    
    /**
     * Allows calling the protected {@link #request(long)} from unit tests.
     *
     * @param n the maximum number of items you want the Observable to emit to the Subscriber at this time, or
     *           {@code Long.MAX_VALUE} if you want the Observable to emit items at its own pace
     */
    public void requestMore(long n) {
        request(n);
    }

    /**
     * Returns the sequence of items observed by this {@link Subscriber}, as an ordered {@link List}.
     *
     * @return a list of items observed by this Subscriber, in the order in which they were observed
     */
    public List<T> getOnNextEvents() {
        return testObserver.getOnNextEvents();
    }

    /**
     * Asserts that a particular sequence of items was received by this {@link Subscriber} in order.
     *
     * @param items
     *          the sequence of items expected to have been observed
     * @throws AssertionError
     *          if the sequence of items observed does not exactly match {@code items}
     */
    public void assertReceivedOnNext(List<T> items) {
        testObserver.assertReceivedOnNext(items);
    }

    /**
     * Asserts that a single terminal event occurred, either {@link #onCompleted} or {@link #onError}.
     *
     * @throws AssertionError
     *          if not exactly one terminal event notification was received
     */
    public void assertTerminalEvent() {
        testObserver.assertTerminalEvent();
    }

    /**
     * Asserts that this {@code Subscriber} is unsubscribed.
     *
     * @throws AssertionError
     *          if this {@code Subscriber} is not unsubscribed
     */
    public void assertUnsubscribed() {
        if (!isUnsubscribed()) {
            testObserver.assertionError("Not unsubscribed.");
        }
    }

    /**
     * Asserts that this {@code Subscriber} has received no {@code onError} notifications.
     * 
     * @throws AssertionError
     *          if this {@code Subscriber} has received one or more {@code onError} notifications
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

    
    /**
     * Blocks until this {@link Subscriber} receives a notification that the {@code Observable} is complete
     * (either an {@code onCompleted} or {@code onError} notification).
     *
     * @throws RuntimeException
     *          if the Subscriber is interrupted before the Observable is able to complete
     */
    public void awaitTerminalEvent() {
        try {
            latch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted", e);
        }
    }

    /**
     * Blocks until this {@link Subscriber} receives a notification that the {@code Observable} is complete
     * (either an {@code onCompleted} or {@code onError} notification), or until a timeout expires.
     *
     * @param timeout
     *          the duration of the timeout
     * @param unit
     *          the units in which {@code timeout} is expressed
     * @throws RuntimeException
     *          if the Subscriber is interrupted before the Observable is able to complete
     */
    public void awaitTerminalEvent(long timeout, TimeUnit unit) {
        try {
            latch.await(timeout, unit);
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted", e);
        }
    }

    /**
     * Blocks until this {@link Subscriber} receives a notification that the {@code Observable} is complete
     * (either an {@code onCompleted} or {@code onError} notification), or until a timeout expires; if the
     * Subscriber is interrupted before either of these events take place, this method unsubscribes the
     * Subscriber from the Observable). If timeout expires then the Subscriber is unsubscribed from the Observable.
     *
     * @param timeout
     *          the duration of the timeout
     * @param unit
     *          the units in which {@code timeout} is expressed
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
     * {@link Subscriber}.
     *
     * @return the {@code Thread} on which this Subscriber last received an item or notification from the
     *         Observable it is subscribed to
     */
    public Thread getLastSeenThread() {
        return lastSeenThread;
    }
    
    /**
     * Asserts that there is exactly one completion event.
     *
     * @throws AssertionError if there were zero, or more than one, onCompleted events
     * @since 1.1.0
     */
    public void assertCompleted() {
        int s = testObserver.getOnCompletedEvents().size();
        if (s == 0) {
            testObserver.assertionError("Not completed!");
        } else
        if (s > 1) {
            testObserver.assertionError("Completed multiple times: " + s);
        }
    }

    /**
     * Asserts that there is no completion event.
     *
     * @throws AssertionError if there were one or more than one onCompleted events
     * @since 1.1.0
     */
    public void assertNotCompleted() {
        int s = testObserver.getOnCompletedEvents().size();
        if (s == 1) {
            testObserver.assertionError("Completed!");
        } else
        if (s > 1) {
            testObserver.assertionError("Completed multiple times: " + s);
        }
    }

    /**
     * Asserts that there is exactly one error event which is a subclass of the given class.
     *
     * @param clazz the class to check the error against.
     * @throws AssertionError if there were zero, or more than one, onError events, or if the single onError
     *                        event did not carry an error of a subclass of the given class
     * @since 1.1.0
     */
    public void assertError(Class<? extends Throwable> clazz) {
        List<Throwable> err = testObserver.getOnErrorEvents();
        if (err.size() == 0) {
            testObserver.assertionError("No errors");
        } else
        if (err.size() > 1) {
            AssertionError ae = new AssertionError("Multiple errors: " + err.size());
            ae.initCause(new CompositeException(err));
            throw ae;
        } else
        if (!clazz.isInstance(err.get(0))) {
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
     * @since 1.1.0
     */
    public void assertError(Throwable throwable) {
        List<Throwable> err = testObserver.getOnErrorEvents();
        if (err.size() == 0) {
            testObserver.assertionError("No errors");
        } else
        if (err.size() > 1) {
            AssertionError ae = new AssertionError("Multiple errors: " + err.size());
            ae.initCause(new CompositeException(err));
            throw ae;
        } else
        if (!throwable.equals(err.get(0))) {
            AssertionError ae = new AssertionError("Exceptions differ; expected: " + throwable + ", actual: " + err.get(0));
            ae.initCause(err.get(0));
            throw ae;
        }
    }

    /**
     * Asserts that there are no onError and onCompleted events.
     *
     * @throws AssertionError if there was either an onError or onCompleted event
     * @since 1.1.0
     */
    public void assertNoTerminalEvent() {
        List<Throwable> err = testObserver.getOnErrorEvents();
        int s = testObserver.getOnCompletedEvents().size();
        if (err.size() > 0 || s > 0) {
            if (err.isEmpty()) {
                testObserver.assertionError("Found " + err.size() + " errors and " + s + " completion events instead of none");
            } else
            if (err.size() == 1) {
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
     * Asserts that there are no onNext events received.
     *
     * @throws AssertionError if there were any onNext events
     * @since 1.1.0
     */
    public void assertNoValues() {
        int s = testObserver.getOnNextEvents().size();
        if (s > 0) {
            testObserver.assertionError("No onNext events expected yet some received: " + s);
        }
    }

    /**
     * Asserts that the given number of onNext events are received.
     *
     * @param count the expected number of onNext events
     * @throws AssertionError if there were more or fewer onNext events than specified by {@code count}
     * @since 1.1.0
     */
    public void assertValueCount(int count) {
        int s = testObserver.getOnNextEvents().size();
        if (s != count) {
            testObserver.assertionError("Number of onNext events differ; expected: " + count + ", actual: " + s);
        }
    }
    
    /**
     * Asserts that the received onNext events, in order, are the specified items.
     *
     * @param values the items to check
     * @throws AssertionError if the items emitted do not exactly match those specified by {@code values}
     * @since 1.1.0
     */
    public void assertValues(T... values) {
        assertReceivedOnNext(Arrays.asList(values));
    }

    /**
     * Asserts that there is only a single received onNext event and that it marks the emission of a specific item.
     *
     * @param value the item to check
     * @throws AssertionError if the Observable does not emit only the single item specified by {@code value}
     * @since 1.1.0
     */
    public void assertValue(T value) {
        assertReceivedOnNext(Collections.singletonList(value));
    }
}
