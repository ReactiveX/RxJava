/**
 * Copyright 2015 Netflix, Inc.
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
package io.reactivex.subscribers;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.reactivestreams.*;

import io.reactivex.disposables.Disposable;
import io.reactivex.internal.subscribers.EmptySubscriber;
import io.reactivex.internal.util.BackpressureHelper;

/**
 * A subscriber that records events and allows making assertions about them.
 *
 * <p>You can override the onSubscribe, onNext, onError, onComplete, request and
 * cancel methods but not the others (this is by desing).
 * 
 * <p>The TestSubscriber implements Disposable for convenience where dispose calls cancel.
 * 
 * <p>When calling the default request method, you are requesting on behalf of the
 * wrapped actual subscriber.
 * 
 * @param <T> the value type
 */
public class TestSubscriber<T> implements Subscriber<T>, Subscription, Disposable {
    /** The actual subscriber to forward events to. */
    private final Subscriber<? super T> actual;
    /** The initial request amount if not null. */
    private final Long initialRequest;
    /** The latch that indicates an onError or onCompleted has been called. */
    private final CountDownLatch done;
    /** The list of values received. */
    private final List<T> values;
    /** The list of errors received. */
    private final List<Throwable> errors;
    /** The number of completions. */
    private long completions;
    /** The last thread seen by the subscriber. */
    private Thread lastThread;
    
    /** Makes sure the incoming Subscriptions get cancelled immediately. */
    private volatile boolean cancelled;

    /** Holds the current subscription if any. */
    private volatile Subscription subscription;
    /** Updater for subscription. */
    @SuppressWarnings("rawtypes")
    private static final AtomicReferenceFieldUpdater<TestSubscriber, Subscription> SUBSCRIPTION =
            AtomicReferenceFieldUpdater.newUpdater(TestSubscriber.class, Subscription.class, "subscription");
    
    /** Holds the requested amount until a subscription arrives. */
    @SuppressWarnings("unused")
    private volatile long missedRequested;
    /** Updater for subscription. */
    @SuppressWarnings("rawtypes")
    private static final AtomicLongFieldUpdater<TestSubscriber> MISSED_REQUESTED =
            AtomicLongFieldUpdater.newUpdater(TestSubscriber.class, "missedRequested");
    
    /** Indicates a cancelled subscription. */
    private static final Subscription CANCELLED = new Subscription() {
        @Override
        public void request(long n) {
            
        }
        
        @Override
        public void cancel() {
            
        }
    };
    /**
     * Constructs a non-forwarding TestSubscriber with an initial request value of Long.MAX_VALUE.
     */
    public TestSubscriber() {
        this(EmptySubscriber.INSTANCE, Long.MAX_VALUE);
    }

    /**
     * Constructs a non-forwarding TestSubscriber with the specified initial request value.
     * <p>The TestSubscriber doesn't validate the initialRequest value so one can
     * test sources with invalid values as well.
     * @param initialRequest the initial request value if not null
     */
    public TestSubscriber(Long initialRequest) {
        this(EmptySubscriber.INSTANCE, initialRequest);
    }

    /**
     * Constructs a forwarding TestSubscriber with an initial request value of Long.MAX_VALUE.
     * @param actual the actual Subscriber to forward events to
     */
    public TestSubscriber(Subscriber<? super T> actual) {
        this(actual, Long.MAX_VALUE);
    }

    /**
     * Constructs a forwarding TestSubscriber with the specified initial request value.
     * <p>The TestSubscriber doesn't validate the initialRequest value so one can
     * test sources with invalid values as well.
     * @param actual the actual Subscriber to forward events to
     * @param initialRequest the initial request value if not null
     */
    public TestSubscriber(Subscriber<? super T> actual, Long initialRequest) {
        this.actual = actual;
        this.initialRequest = initialRequest;
        this.values = new ArrayList<>();
        this.errors = new ArrayList<>();
        this.done = new CountDownLatch(1);
    }
    
    @Override
    public void onSubscribe(Subscription s) {
        lastThread = Thread.currentThread();
        
        if (s == null) {
            errors.add(new NullPointerException("onSubscribe received a null Subscription"));
            return;
        }
        if (!SUBSCRIPTION.compareAndSet(this, null, s)) {
            s.cancel();
            if (subscription != CANCELLED) {
                errors.add(new NullPointerException("onSubscribe received multiple subscriptions: " + s));
            }
            return;
        }
        
        if (cancelled) {
            s.cancel();
        }
        
        actual.onSubscribe(s);
        
        if (cancelled) {
            return;
        }
        
        if (initialRequest != null) {
            s.request(initialRequest);
        }
        
        long mr = MISSED_REQUESTED.getAndSet(this, 0L);
        if (mr != 0L) {
            s.request(mr);
        }
    }
    
    @Override
    public void onNext(T t) {
        lastThread = Thread.currentThread();
        values.add(t);
        
        if (t == null) {
            errors.add(new NullPointerException("onNext received a null Subscription"));
        }
        
        actual.onNext(t);
    }
    
    @Override
    public void onError(Throwable t) {
        try {
            lastThread = Thread.currentThread();
            errors.add(t);

            if (t == null) {
                errors.add(new NullPointerException("onError received a null Subscription"));
            }

            actual.onError(t);
        } finally {
            done.countDown();
        }
    }
    
    @Override
    public void onComplete() {
        try {
            lastThread = Thread.currentThread();
            completions++;
            
            actual.onComplete();
        } finally {
            done.countDown();
        }
    }
    
    @Override
    public void request(long n) {
        if (n <= 0) {
            errors.add(new IllegalArgumentException("n > 0 required but it was " + n));
            return;
        }
        Subscription s = subscription;
        if (s != null) {
            s.request(n);
        } else {
            BackpressureHelper.add(MISSED_REQUESTED, this, n);
            s = subscription;
            if (s != null) {
                long mr = MISSED_REQUESTED.getAndSet(this, 0L);
                if (mr != 0L) {
                    s.request(mr);
                }
            }
        }
    }
    
    @Override
    public void cancel() {
        if (!cancelled) {
            cancelled = true;
            Subscription s = subscription;
            if (s != CANCELLED) {
                s = SUBSCRIPTION.getAndSet(this, CANCELLED);
                if (s != CANCELLED && s != null) {
                    s.cancel();
                }
            }
        }
    }
    
    /**
     * Returns true if this TestSubscriber has been cancelled.
     * @return true if this TestSubscriber has been cancelled
     */
    public final boolean isCancelled() {
        return cancelled;
    }
    
    @Override
    public final void dispose() {
        cancel();
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
        return subscription != null;
    }
    
    /**
     * Awaits until this TestSubscriber receives an onError or onComplete events.
     * @throws InterruptedException if the current thread is interrupted while waiting
     * @see #awaitTerminalEvent()
     */
    public final void await() throws InterruptedException {
        if (done.getCount() == 0) {
            return;
        }
        
        done.await();
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
        if (done.getCount() == 0) {
            return true;
        }
        return done.await(time, unit);
    }
    
    // assertion methods
    
    /**
     * Fail with the given message and add the sequence of errors as suppressed ones.
     * <p>Note this is delibarately the only fail method. Most of the times an assertion
     * would fail but it is possible it was due to an exception somewhere. This construct
     * will capture those potential errors and report it along with the original failure.
     * 
     * @param message the message to use
     * @param errors the sequence of errors to add as suppressed exception
     */
    private void fail(String prefix, String message, Iterable<? extends Throwable> errors) {
        AssertionError ae = new AssertionError(prefix + message);
        errors.forEach(e -> {
            if (e == null) {
                ae.addSuppressed(new NullPointerException("Throwable was null!"));
            } else {
                ae.addSuppressed(e);
            }
        });
        throw ae;
    }
    
    /**
     * Assert that this TestSubscriber received exactly one onComplete event.
     */
    public void assertComplete() {
        String prefix = "";
        /*
         * This creates a happens-before relation with the possible completion of the TestSubscriber.
         * Don't move it after the instance reads or into fail()!
         */
        if (done.getCount() != 0) {
            prefix = "Subscriber still running! ";
        }
        long c = completions;
        if (c == 0) {
            fail(prefix, "Not completed", errors);
        } else
        if (c > 1) {
            fail(prefix, "Multiple completions: " + c, errors);
        }
    }
    
    /**
     * Assert that this TestSubscriber has not received any onComplete event.
     */
    public void assertNotComplete() {
        String prefix = "";
        if (done.getCount() != 0) {
            prefix = "Subscriber still running! ";
        }
        long c = completions;
        if (c == 1) {
            fail(prefix, "Completed!", errors);
        } else 
        if (c > 1) {
            fail(prefix, "Multiple completions: " + c, errors);
        }
    }
    
    /**
     * Assert that this TestSubscriber has not received any onError event.
     */
    public void assertNoError() {
        String prefix = "";
        if (done.getCount() != 0) {
            prefix = "Subscriber still running! ";
        }
        int s = errors.size();
        if (s != 0) {
            fail(prefix, "Error(s) present", errors);
        }
    }
    
    /**
     * Assert that this TestSubscriber received exactly the specified onError event value.
     * 
     * <p>The comparison is performed via Objects.equals(); since most exceptions don't
     * implement equals(), this assertion may fail. Use the {@link #assertError(Class)}
     * overload to test against the class of an error instead of an instance of an error.
     * @param error the error to check
     * @see #assertError(Class)
     */
    public void assertError(Throwable error) {
        String prefix = "";
        if (done.getCount() != 0) {
            prefix = "Subscriber still running! ";
        }
        int s = errors.size();
        if (s == 0) {
            fail(prefix, "No errors", Collections.emptyList());
        }
        if (errors.contains(error)) {
            if (s != 1) {
                fail(prefix, "Error present but other errors as well", errors);
            }
        } else {
            fail(prefix, "Error not present", errors);
        }
    }
    
    /**
     * Asserts that this TestSubscriber received exactly one onError event which is an
     * instance of the specified errorClass class.
     * @param errorClass the error class to expect
     */
    public void assertError(Class<? extends Throwable> errorClass) {
        String prefix = "";
        if (done.getCount() != 0) {
            prefix = "Subscriber still running! ";
        }
        int s = errors.size();
        if (s == 0) {
            fail(prefix, "No errors", Collections.emptyList());
        }
        
        boolean found = errors.stream()
                .anyMatch(errorClass::isInstance);
        
        if (found) {
            if (s != 1) {
                fail(prefix, "Error present but other errors as well", errors);
            }
        } else {
            fail(prefix, "Error not present", errors);
        }
    }
    
    /**
     * Assert that this TestSubscriber received exactly one onNext value which is equal to
     * the given value with respect to Objects.equals.
     * @param value the value to expect
     */
    public final void assertValue(T value) {
        String prefix = "";
        if (done.getCount() != 0) {
            prefix = "Subscriber still running! ";
        }
        int s = values.size();
        if (s != 1) {
            fail(prefix, "Expected: " + value + ", Actual: " + values, errors);
        }
        T v = values.get(0);
        if (Objects.equals(value, v)) {
            fail(prefix, "Expected: " + value + ", Actual: " + v, errors);
        }
    }
    
    /**
     * Assert that this TestSubscriber received the specified number onNext events.
     * @param count the expected number of onNext events
     */
    public final void assertValueCount(int count) {
        String prefix = "";
        if (done.getCount() != 0) {
            prefix = "Subscriber still running! ";
        }
        int s = values.size();
        if (s != count) {
            fail(prefix, "Value counts differ; Expected: " + count + ", Actual: " + s, errors);
        }
    }
    
    /**
     * Assert that this TestSubscriber has not received any onNext events.
     */
    public final void assertNoValues() {
        assertValueCount(0);
    }
    
    /**
     * Assert that the TestSubscriber received only the specified values in the specified order.
     * @param values the values expected
     * @see #assertValueSet(Collection)
     */
    @SafeVarargs
    public final void assertValues(T... values) {
        String prefix = "";
        if (done.getCount() != 0) {
            prefix = "Subscriber still running! ";
        }
        int s = this.values.size();
        if (s != values.length) {
            fail(prefix, "Value count differs; Expected: " + values.length + " " + Arrays.toString(values)
            + ", Actual: " + s + " " + this.values, errors);
        }
        for (int i = 0; i < s; i++) {
            T v = this.values.get(i);
            T u = values[i];
            if (!Objects.equals(u, v)) {
                fail(prefix, "Values at position " + i + " differ; Expected: " + u + ", Actual: " + v, errors);
            }
        }
    }
    
    /**
     * Assert that the TestSubscriber received only the specified values in any order.
     * <p>This helps asserting when the order of the values is not guaranteed, i.e., when merging
     * asynchronous streams.
     * 
     * @param values the collection of values expected in any order
     */
    public final void assertValueSet(Collection<? extends T> values) {
        String prefix = "";
        if (done.getCount() != 0) {
            prefix = "Subscriber still running! ";
        }
        int s = this.values.size();
        if (s != values.size()) {
            fail(prefix, "Value count differs; Expected: " + values.size() + " " + values
            + ", Actual: " + s + " " + this.values, errors);
        }
        for (int i = 0; i < s; i++) {
            T v = this.values.get(i);
            
            if (!values.contains(v)) {
                fail(prefix, "Value not in the expected collection: " + v, errors);
            }
        }
    }
    
    /**
     * Assert that the TestSubscriber received only the specified sequence of values in the same order.
     * @param sequence the sequence of expected values in order
     */
    public final void assertValueSequence(Iterable<? extends T> sequence) {
        String prefix = "";
        if (done.getCount() != 0) {
            prefix = "Subscriber still running! ";
        }
        int i = 0;
        Iterator<T> vit = values.iterator();
        Iterator<? extends T> it = sequence.iterator();
        boolean itNext = false;
        boolean vitNext = false;
        while ((itNext = it.hasNext()) && (vitNext = vit.hasNext())) {
            T v = it.next();
            T u = vit.next();
            
            if (!Objects.equals(u, v)) {
                fail(prefix, "Values at position " + i + " differ; Expected: " + u + ", Actual: " + v, errors);
            }
            i++;
        }
        
        if (itNext && !vitNext) {
            fail(prefix, "More values received than expected (" + i + ")", errors);
        }
        if (!itNext && !vitNext) {
            fail(prefix, "Fever values received than expected (" + i + ")", errors);
        }
    }
    
    /**
     * Assert that the TestSubscriber terminated (i.e., the terminal latch reached zero).
     */
    public final void assertTerminated() {
        if (done.getCount() != 0) {
            fail("", "Subscriber still running!", errors);
        }
    }
    
    /**
     * Assert that the TestSubscriber has not terminated (i.e., the terminal latch is still non-zero).
     */
    public final void assertNotTerminated() {
        if (done.getCount() == 0) {
            fail("", "Subscriber terminated!", errors);
        }
    }
    
    /**
     * Assert that the onSubscribe method was called exactly once.
     */
    public final void assertSubscribed() {
        String prefix = "";
        if (done.getCount() != 0) {
            prefix = "Subscriber still running! ";
        }
        if (subscription == null) {
            fail(prefix, "Not subscribed!", errors);
        }
    }
    
    /**
     * Assert that the onSubscribe method hasn't been called at all.
     */
    public final void assertNotSubscribed() {
        String prefix = "";
        if (done.getCount() != 0) {
            prefix = "Subscriber still running! ";
        }
        if (subscription != null) {
            fail(prefix, "Subscribed!", errors);
        } else
        if (!errors.isEmpty()) {
            fail(prefix, "Not subscribed but errors found", errors);
        }
    }
    
    /**
     * Waits until the any terminal event has been received by this TestSubscriber
     * or returns false if the wait has been interrupted.
     * @return true if the TestSubscriber terminated, false if the wait has been interrupted
     */
    public boolean awaitTerminalEvent() {
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
     * @param time the waiting time
     * @param unit the time unit of the waiting time
     * @return true if the TestSubscriber terminated, false if timeout or interrupt happened
     */
    public boolean awaitTerminalEvent(long duration, TimeUnit unit) {
        try {
            return await(duration, unit);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
    
    public void assertErrorMessage(String message) {
        String prefix = "";
        if (done.getCount() != 0) {
            prefix = "Subscriber still running! ";
        }
        int s = errors.size();
        if (s == 0) {
            fail(prefix, "No errors", Collections.emptyList());
        } else
        if (s == 1) {
            Throwable e = errors.get(0);
            if (e == null) {
                fail(prefix, "Error is null", Collections.emptyList());
            }
            String errorMessage = e.getMessage();
            if (!Objects.equals(message, errorMessage)) {
                fail(prefix, "Error message differs; Expected: " + message + ", Actual: " + errorMessage, Collections.singletonList(e));
            }
        } else {
            fail(prefix, "Multiple errors", errors);
        }
    }
}
