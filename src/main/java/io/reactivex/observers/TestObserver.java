/**
 * Copyright 2016 Netflix, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See
 * the License for the specific language governing permissions and limitations under the License.
 */
package io.reactivex.observers;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.Notification;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.CompositeException;
import io.reactivex.functions.Consumer;
import io.reactivex.internal.disposables.*;
import io.reactivex.internal.functions.ObjectHelper;
import io.reactivex.internal.fuseable.QueueDisposable;
import io.reactivex.internal.util.ExceptionHelper;

/**
 * An Observer that records events and allows making assertions about them.
 *
 * <p>You can override the onSubscribe, onNext, onError, onComplete and
 * cancel methods but not the others (this is by design).
 * 
 * <p>The TestSubscriber implements Disposable for convenience where dispose calls cancel.
 * 
 * @param <T> the value type
 */
public class TestObserver<T> implements Observer<T>, Disposable {
    /** The actual observer to forward events to. */
    private final Observer<? super T> actual;
    /** The latch that indicates an onError or onCompleted has been called. */
    private final CountDownLatch done;
    /** The list of values received. */
    private final List<T> values;
    /** The list of errors received. */
    private final List<Throwable> errors;
    /** The number of completions. */
    private long completions;
    /** The last thread seen by the observer. */
    private Thread lastThread;
    
    /** Holds the current subscription if any. */
    private final AtomicReference<Disposable> subscription = new AtomicReference<Disposable>();

    private boolean checkSubscriptionOnce;

    private int initialFusionMode;
    
    private int establishedFusionMode;
    
    private QueueDisposable<T> qs;
    
    /**
     * Constructs a non-forwarding TestObserver.
     * @param <T> the value type received
     * @return the new TestObserver instance
     */
    public static <T> TestObserver<T> create() {
        return new TestObserver<T>();
    }
    
    /**
     * Constructs a forwarding TestObserver.
     * @param <T> the value type received
     * @param delegate the actual Observer to forward events to
     * @return the new TestObserver instance
     */
    public static <T> TestObserver<T> create(Observer<? super T> delegate) {
        return new TestObserver<T>(delegate);
    }

    /**
     * Constructs a non-forwarding TestObserver.
     */
    public TestObserver() {
        this(EmptyObserver.INSTANCE);
    }

    /**
     * Constructs a forwarding TestObserver.
     * @param actual the actual Observer to forward events to
     */
    public TestObserver(Observer<? super T> actual) {
        this.actual = actual;
        this.values = new ArrayList<T>();
        this.errors = new ArrayList<Throwable>();
        this.done = new CountDownLatch(1);
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public void onSubscribe(Disposable s) {
        lastThread = Thread.currentThread();
        
        if (s == null) {
            errors.add(new NullPointerException("onSubscribe received a null Subscription"));
            return;
        }
        if (!subscription.compareAndSet(null, s)) {
            s.dispose();
            if (subscription.get() != DisposableHelper.DISPOSED) {
                errors.add(new IllegalStateException("onSubscribe received multiple subscriptions: " + s));
            }
            return;
        }
        
        if (initialFusionMode != 0) {
            if (s instanceof QueueDisposable) {
                qs = (QueueDisposable<T>)s;
                
                int m = qs.requestFusion(initialFusionMode);
                establishedFusionMode = m;
                
                if (m == QueueDisposable.SYNC) {
                    checkSubscriptionOnce = true;
                    lastThread = Thread.currentThread();
                    try {
                        T t;
                        while ((t = qs.poll()) != null) {
                            values.add(t);
                        }
                        completions++;
                    } catch (Throwable ex) {
                        // Exceptions.throwIfFatal(e); TODO add fatal exceptions?
                        errors.add(ex);
                    }
                    return;
                }
            }
        }

        actual.onSubscribe(s);
    }
    
    @Override
    public void onNext(T t) {
        if (!checkSubscriptionOnce) {
            checkSubscriptionOnce = true;
            if (subscription.get() == null) {
                errors.add(new IllegalStateException("onSubscribe not called in proper order"));
            }
        }

        lastThread = Thread.currentThread();

        if (establishedFusionMode == QueueDisposable.ASYNC) {
            try {
                while ((t = qs.poll()) != null) {
                    values.add(t);
                }
            } catch (Throwable ex) {
                // Exceptions.throwIfFatal(e); TODO add fatal exceptions?
                errors.add(ex);
            }
            return;
        }

        values.add(t);
        
        if (t == null) {
            errors.add(new NullPointerException("onNext received a null Subscription"));
        }
        
        actual.onNext(t);
    }
    
    @Override
    public void onError(Throwable t) {
        if (!checkSubscriptionOnce) {
            checkSubscriptionOnce = true;
            if (subscription.get() == null) {
                errors.add(new IllegalStateException("onSubscribe not called in proper order"));
            }
        }

        try {
            lastThread = Thread.currentThread();
            if (t == null) {
                errors.add(new NullPointerException("onError received a null Throwable"));
            } else {
                errors.add(t);
            }

            actual.onError(t);
        } finally {
            done.countDown();
        }
    }
    
    @Override
    public void onComplete() {
        if (!checkSubscriptionOnce) {
            checkSubscriptionOnce = true;
            if (subscription.get() == null) {
                errors.add(new IllegalStateException("onSubscribe not called in proper order"));
            }
        }

        try {
            lastThread = Thread.currentThread();
            completions++;
            
            actual.onComplete();
        } finally {
            done.countDown();
        }
    }
    
    /**
     * Returns true if this TestSubscriber has been cancelled.
     * @return true if this TestSubscriber has been cancelled
     */
    public final boolean isCancelled() {
        return isDisposed();
    }
    
    /**
     * Cancels the TestObserver (before or after the subscription happened).
     * <p>This operation is thread-safe.
     * <p>This method is provided as a convenience when converting Flowable tests that cancel.
     */
    public final void cancel() {
        dispose();
    }
    
    @Override
    public final void dispose() {
        DisposableHelper.dispose(subscription);
    }

    @Override
    public final boolean isDisposed() {
        return DisposableHelper.isDisposed(subscription.get());
    }

    // state retrieval methods
    
    /**
     * Returns the last thread which called the onXXX methods of this TestSubscriber.
     * @return the last thread which called the onXXX methods
     */
    public final Thread lastThread() {
        return lastThread;
    }
    
    /**
     * Returns a shared list of received onNext values.
     * @return a list of received onNext values
     */
    public final List<T> values() {
        return values;
    }
    
    /**
     * Returns a shared list of received onError exceptions.
     * @return a list of received events onError exceptions
     */
    public final List<Throwable> errors() {
        return errors;
    }
    
    /**
     * Returns the number of times onComplete was called.
     * @return the number of times onComplete was called
     */
    public final long completions() {
        return completions;
    }

    /**
     * Returns true if TestSubscriber received any onError or onComplete events.
     * @return true if TestSubscriber received any onError or onComplete events
     */
    public final boolean isTerminated() {
        return done.getCount() == 0;
    }
    
    /**
     * Returns the number of onNext values received.
     * @return the number of onNext values received
     */
    public final int valueCount() {
        return values.size();
    }
    
    /**
     * Returns the number of onError exceptions received.
     * @return the number of onError exceptions received
     */
    public final int errorCount() {
        return errors.size();
    }

    /**
     * Returns true if this TestSubscriber received a subscription.
     * @return true if this TestSubscriber received a subscription
     */
    public final boolean hasSubscription() {
        return subscription.get() != null;
    }
    
    /**
     * Awaits until this TestSubscriber receives an onError or onComplete events.
     * @return this
     * @throws InterruptedException if the current thread is interrupted while waiting
     * @see #awaitTerminalEvent()
     */
    public final TestObserver<T> await() throws InterruptedException {
        if (done.getCount() == 0) {
            return this;
        }
        
        done.await();
        return this;
    }
    
    /**
     * Awaits the specified amount of time or until this TestSubscriber 
     * receives an onError or onComplete events, whichever happens first.
     * @param time the waiting time
     * @param unit the time unit of the waiting time
     * @return true if the TestSubscriber terminated, false if timeout happened
     * @throws InterruptedException if the current thread is interrupted while waiting
     * @see #awaitTerminalEvent(long, TimeUnit)
     */
    public final boolean await(long time, TimeUnit unit) throws InterruptedException {
        return done.getCount() == 0 || done.await(time, unit);
    }
    
    // assertion methods
    
    /**
     * Fail with the given message and add the sequence of errors as suppressed ones.
     * <p>Note this is deliberately the only fail method. Most of the times an assertion
     * would fail but it is possible it was due to an exception somewhere. This construct
     * will capture those potential errors and report it along with the original failure.
     * 
     * @param message the message to use
     */
    private AssertionError fail(String message) {
        StringBuilder b = new StringBuilder(64 + message.length());
        b.append(message);
        
        b.append(" (")
        .append("latch = ").append(done.getCount()).append(", ")
        .append("values = ").append(values.size()).append(", ")
        .append("errors = ").append(errors.size()).append(", ")
        .append("completions = ").append(completions)
        .append(')')
        ;
        
        AssertionError ae = new AssertionError(b.toString());
        CompositeException ce = new CompositeException();
        for (Throwable e : errors) {
            ce.suppress(e);
        }
        if (!ce.isEmpty()) {
            ae.initCause(ce);
        }
        return ae;
    }
    
    /**
     * Assert that this TestSubscriber received exactly one onComplete event.
     * @return this;
     */
    public final TestObserver<T> assertComplete() {
        long c = completions;
        if (c == 0) {
            throw fail("Not completed");
        } else
        if (c > 1) {
            throw fail("Multiple completions: " + c);
        }
        return this;
    }
    
    /**
     * Assert that this TestSubscriber has not received any onComplete event.
     * @return this;
     */
    public final TestObserver<T> assertNotComplete() {
        long c = completions;
        if (c == 1) {
            throw fail("Completed!");
        } else 
        if (c > 1) {
            throw fail("Multiple completions: " + c);
        }
        return this;
    }
    
    /**
     * Assert that this TestSubscriber has not received any onError event.
     * @return this;
     */
    public final TestObserver<T> assertNoErrors() {
        int s = errors.size();
        if (s != 0) {
            throw fail("Error(s) present: " + errors);
        }
        return this;
    }
    
    /**
     * Assert that this TestSubscriber received exactly the specified onError event value.
     * 
     * <p>The comparison is performed via Objects.equals(); since most exceptions don't
     * implement equals(), this assertion may fail. Use the {@link #assertError(Class)}
     * overload to test against the class of an error instead of an instance of an error.
     * @param error the error to check
     * @return this;
     * @see #assertError(Class)
     */
    public final TestObserver<T> assertError(Throwable error) {
        int s = errors.size();
        if (s == 0) {
            throw fail("No errors");
        }
        if (errors.contains(error)) {
            if (s != 1) {
                throw fail("Error present but other errors as well");
            }
        } else {
            throw fail("Error not present");
        }
        return this;
    }
    
    /**
     * Asserts that this TestSubscriber received exactly one onError event which is an
     * instance of the specified errorClass class.
     * @param errorClass the error class to expect
     * @return this;
     */
    public final TestObserver<T> assertError(Class<? extends Throwable> errorClass) {
        int s = errors.size();
        if (s == 0) {
            throw fail("No errors");
        }
        
        boolean found = false;
        
        for (Throwable e : errors) {
            if (errorClass.isInstance(e)) {
                found = true;
                break;
            }
        }
        
        if (found) {
            if (s != 1) {
                throw fail("Error present but other errors as well");
            }
        } else {
            throw fail("Error not present");
        }
        return this;
    }
    
    /**
     * Assert that this TestSubscriber received exactly one onNext value which is equal to
     * the given value with respect to Objects.equals.
     * @param value the value to expect
     * @return this;
     */
    public final TestObserver<T> assertValue(T value) {
        int s = values.size();
        if (s != 1) {
            throw fail("Expected: " + valueAndClass(value) + ", Actual: " + values);
        }
        T v = values.get(0);
        if (!ObjectHelper.equals(value, v)) {
            throw fail("Expected: " + valueAndClass(value) + ", Actual: " + valueAndClass(v));
        }
        return this;
    }
    
    /** Appends the class name to a non-null value. */
    static String valueAndClass(Object o) {
        if (o != null) {
            return o + " (class: " + o.getClass().getSimpleName() + ")";
        }
        return "null";
    }
    
    /**
     * Assert that this TestSubscriber received the specified number onNext events.
     * @param count the expected number of onNext events
     * @return this;
     */
    public final TestObserver<T> assertValueCount(int count) {
        int s = values.size();
        if (s != count) {
            throw fail("Value counts differ; Expected: " + count + ", Actual: " + s);
        }
        return this;
    }
    
    /**
     * Assert that this TestSubscriber has not received any onNext events.
     * @return this;
     */
    public final TestObserver<T> assertNoValues() {
        return assertValueCount(0);
    }
    
    /**
     * Assert that the TestSubscriber received only the specified values in the specified order.
     * @param values the values expected
     * @return this;
     * @see #assertValueSet(Collection)
     */
    public final TestObserver<T> assertValues(T... values) {
        int s = this.values.size();
        if (s != values.length) {
            throw fail("Value count differs; Expected: " + values.length + " " + Arrays.toString(values)
            + ", Actual: " + s + " " + this.values);
        }
        for (int i = 0; i < s; i++) {
            T v = this.values.get(i);
            T u = values[i];
            if (!ObjectHelper.equals(u, v)) {
                throw fail("Values at position " + i + " differ; Expected: " + valueAndClass(u) + ", Actual: " + valueAndClass(v));
            }
        }
        return this;
    }
    
    /**
     * Assert that the TestSubscriber received only the specified values in any order.
     * <p>This helps asserting when the order of the values is not guaranteed, i.e., when merging
     * asynchronous streams.
     * 
     * @param expected the collection of values expected in any order
     * @return this;
     */
    public final TestObserver<T> assertValueSet(Collection<? extends T> expected) {
        if (expected.isEmpty()) {
            assertNoValues();
            return this;
        }
        for (T v : this.values) {
            if (!expected.contains(v)) {
                throw fail("Value not in the expected collection: " + valueAndClass(v));
            }
        }
        return this;
    }
    
    /**
     * Assert that the TestSubscriber received only the specified sequence of values in the same order.
     * @param sequence the sequence of expected values in order
     * @return this;
     */
    public final TestObserver<T> assertValueSequence(Iterable<? extends T> sequence) {
        int i = 0;
        Iterator<T> vit = values.iterator();
        Iterator<? extends T> it = sequence.iterator();
        boolean actualNext = false;
        boolean expectedNext = false;
        for (;;) {
            actualNext = it.hasNext();
            expectedNext = vit.hasNext();

            if (!actualNext || !expectedNext) {
                break;
            }
            
            T v = it.next();
            T u = vit.next();
            
            if (!ObjectHelper.equals(u, v)) {
                throw fail("Values at position " + i + " differ; Expected: " + valueAndClass(u) + ", Actual: " + valueAndClass(v));
            }
            i++;
            actualNext = false;
            expectedNext = false;
        }
        
        if (actualNext) {
            throw fail("More values received than expected (" + i + ")");
        }
        if (expectedNext) {
            throw fail("Fever values received than expected (" + i + ")");
        }
        return this;
    }
    
    /**
     * Assert that the TestSubscriber terminated (i.e., the terminal latch reached zero).
     * @return this;
     */
    public final TestObserver<T> assertTerminated() {
        if (done.getCount() != 0) {
            throw fail("Subscriber still running!");
        }
        long c = completions;
        if (c > 1) {
            throw fail("Terminated with multiple completions: " + c);
        }
        int s = errors.size();
        if (s > 1) {
            throw fail("Terminated with multiple errors: " + s);
        }
        
        if (c != 0 && s != 0) {
            throw fail("Terminated with multiple completions and errors: " + c);
        }
        return this;
    }
    
    /**
     * Assert that the TestSubscriber has not terminated (i.e., the terminal latch is still non-zero).
     * @return this;
     */
    public final TestObserver<T> assertNotTerminated() {
        if (done.getCount() == 0) {
            throw fail("Subscriber terminated!");
        }
        return this;
    }
    
    /**
     * Assert that the onSubscribe method was called exactly once.
     * @return this;
     */
    public final TestObserver<T> assertSubscribed() {
        if (subscription.get() == null) {
            throw fail("Not subscribed!");
        }
        return this;
    }
    
    /**
     * Assert that the onSubscribe method hasn't been called at all.
     * @return this;
     */
    public final TestObserver<T> assertNotSubscribed() {
        if (subscription.get() != null) {
            throw fail("Subscribed!");
        } else
        if (!errors.isEmpty()) {
            throw fail("Not subscribed but errors found");
        }
        return this;
    }
    
    /**
     * Waits until the any terminal event has been received by this TestSubscriber
     * or returns false if the wait has been interrupted.
     * @return true if the TestSubscriber terminated, false if the wait has been interrupted
     */
    public final boolean awaitTerminalEvent() {
        try {
            await();
            return true;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
    
    /**
     * Awaits the specified amount of time or until this TestSubscriber 
     * receives an onError or onComplete events, whichever happens first.
     * @param duration the waiting time
     * @param unit the time unit of the waiting time
     * @return true if the TestSubscriber terminated, false if timeout or interrupt happened
     */
    public final boolean awaitTerminalEvent(long duration, TimeUnit unit) {
        try {
            return await(duration, unit);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
    
    /**
     * Assert that there is a single error and it has the given message.
     * @param message the message expected
     * @return this
     */
    public final TestObserver<T> assertErrorMessage(String message) {
        int s = errors.size();
        if (s == 0) {
            throw fail("No errors");
        } else
        if (s == 1) {
            Throwable e = errors.get(0);
            if (e == null) {
                throw fail("Error is null");
            }
            String errorMessage = e.getMessage();
            if (!ObjectHelper.equals(message, errorMessage)) {
                throw fail("Error message differs; Expected: " + message + ", Actual: " + errorMessage);
            }
        } else {
            throw fail("Multiple errors");
        }
        return this;
    }
    
    /**
     * Returns a list of 3 other lists: the first inner list contains the plain
     * values received; the second list contains the potential errors
     * and the final list contains the potential completions as Notifications.
     * 
     * @return a list of (values, errors, completion-notifications)
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public final List<List<Object>> getEvents() {
        List<List<Object>> result = new ArrayList<List<Object>>();
        
        result.add((List)values());
        
        result.add((List)errors());
        
        List<Object> completeList = new ArrayList<Object>();
        for (long i = 0; i < completions; i++) {
            completeList.add(Notification.createOnComplete());
        }
        result.add(completeList);
        
        return result;
    }

    /**
     * Sets the initial fusion mode if the upstream supports fusion.
     * <p>Package-private: avoid leaking the now internal fusion properties into the public API.
     * Use ObserverFusion to work with such tests.
     * @param mode the mode to establish, see the {@link QueueDisposable} constants
     * @return this
     */
    final TestObserver<T> setInitialFusionMode(int mode) {
        this.initialFusionMode = mode;
        return this;
    }
    
    /**
     * Asserts that the given fusion mode has been established
     * <p>Package-private: avoid leaking the now internal fusion properties into the public API.
     * Use ObserverFusion to work with such tests.
     * @param mode the expected mode
     * @return this
     */
    final TestObserver<T> assertFusionMode(int mode) {
        int m = establishedFusionMode;
        if (m != mode) {
            if (qs != null) {
                throw new AssertionError("Fusion mode different. Expected: " + fusionModeToString(mode)
                + ", actual: " + fusionModeToString(m));
            } else {
                throw new AssertionError("Upstream is not fuseable");
            }
        }
        return this;
    }
    
    static String fusionModeToString(int mode) {
        switch (mode) {
        case QueueDisposable.NONE : return "NONE";
        case QueueDisposable.SYNC : return "SYNC";
        case QueueDisposable.ASYNC : return "ASYNC";
        default: return "Unknown(" + mode + ")";
        }
    }
    
    /**
     * Assert that the upstream is a fuseable source.
     * <p>Package-private: avoid leaking the now internal fusion properties into the public API.
     * Use ObserverFusion to work with such tests.
     * @return this
     */
    final TestObserver<T> assertFuseable() {
        if (qs == null) {
            throw new AssertionError("Upstream is not fuseable.");
        }
        return this;
    }

    /**
     * Assert that the upstream is not a fuseable source.
     * <p>Package-private: avoid leaking the now internal fusion properties into the public API.
     * Use ObserverFusion to work with such tests.
     * @return this
     */
    final TestObserver<T> assertNotFuseable() {
        if (qs != null) {
            throw new AssertionError("Upstream is fuseable.");
        }
        return this;
    }

    /**
     * Run a check consumer with this TestObserver instance.
     * @param check the check consumer to run
     * @return this
     */
    public final TestObserver<T> assertOf(Consumer<? super TestObserver<T>> check) {
        try {
            check.accept(this);
        } catch (Throwable ex) {
            throw ExceptionHelper.wrapOrThrow(ex);
        }
        return this;
    }

    /**
     * Assert that the upstream signalled the specified values in order and
     * completed normally.
     * @param values the expected values, asserted in order
     * @return this
     * @see #assertFailure(Class, Object...)
     * @see #assertFailureAndMessage(Class, String, Object...)
     */
    public final TestObserver<T> assertResult(T... values) {
        return assertValues(values)
                .assertNoErrors()
                .assertComplete();
    }

    /**
     * Assert that the upstream signalled the specified values in order
     * and then failed with a specific class or subclass of Throwable.
     * @param error the expected exception (parent) class
     * @param values the expected values, asserted in order
     * @return this
     */
    public final TestObserver<T> assertFailure(Class<? extends Throwable> error, T... values) {
        return assertValues(values)
                .assertError(error)
                .assertNotComplete();
    }

    /**
     * Assert that the upstream signalled the specified values in order,
     * then failed with a specific class or subclass of Throwable
     * and with the given exact error message.
     * @param error the expected exception (parent) class
     * @param message the expected failure message
     * @param values the expected values, asserted in order
     * @return this
     */
    public final TestObserver<T> assertFailureAndMessage(Class<? extends Throwable> error, 
            String message, T... values) {
        return assertValues(values)
                .assertError(error)
                .assertErrorMessage(message)
                .assertNotComplete();
    }

    /**
     * Awaits until the internal latch is counted down.
     * <p>If the wait times out or gets interrupted, the TestSubscriber is cancelled.
     * @param time the waiting time
     * @param unit the time unit of the waiting time
     * @return this
     * @throws RuntimeException wrapping an InterruptedException if the wait is interrupted
     */
    public final TestObserver<T> awaitDone(long time, TimeUnit unit) {
        try {
            if (!done.await(time, unit)) {
                cancel();
            }
        } catch (InterruptedException ex) {
            cancel();
            throw ExceptionHelper.wrapOrThrow(ex);
        }
        return this;
    }


    /**
     * An observer that ignores all events and does not report errors.
     */
    enum EmptyObserver implements Observer<Object> {
        INSTANCE;

        @Override
        public void onSubscribe(Disposable d) {
        }

        @Override
        public void onNext(Object t) {
        }

        @Override
        public void onError(Throwable t) {
        }

        @Override
        public void onComplete() {
        }
    }
}
