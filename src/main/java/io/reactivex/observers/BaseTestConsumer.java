/**
 * Copyright (c) 2016-present, RxJava Contributors.
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

import io.reactivex.Notification;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.CompositeException;
import io.reactivex.functions.Predicate;
import io.reactivex.internal.functions.*;
import io.reactivex.internal.util.*;

/**
 * Base class with shared infrastructure to support TestSubscriber and TestObserver.
 * @param <T> the value type consumed
 * @param <U> the subclass of this BaseTestConsumer
 */
public abstract class BaseTestConsumer<T, U extends BaseTestConsumer<T, U>> implements Disposable {
    /** The latch that indicates an onError or onComplete has been called. */
    protected final CountDownLatch done;
    /** The list of values received. */
    protected final List<T> values;
    /** The list of errors received. */
    protected final List<Throwable> errors;
    /** The number of completions. */
    protected long completions;
    /** The last thread seen by the observer. */
    protected Thread lastThread;

    protected boolean checkSubscriptionOnce;

    protected int initialFusionMode;

    protected int establishedFusionMode;

    /**
     * The optional tag associated with this test consumer.
     * @since 2.0.7
     */
    protected CharSequence tag;

    /**
     * Indicates that one of the awaitX method has timed out.
     * @since 2.0.7
     */
    protected boolean timeout;

    public BaseTestConsumer() {
        this.values = new VolatileSizeArrayList<T>();
        this.errors = new VolatileSizeArrayList<Throwable>();
        this.done = new CountDownLatch(1);
    }

    /**
     * Returns the last thread which called the onXXX methods of this TestObserver/TestSubscriber.
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
     * Returns true if TestObserver/TestSubscriber received any onError or onComplete events.
     * @return true if TestObserver/TestSubscriber received any onError or onComplete events
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
     * Fail with the given message and add the sequence of errors as suppressed ones.
     * <p>Note this is deliberately the only fail method. Most of the times an assertion
     * would fail but it is possible it was due to an exception somewhere. This construct
     * will capture those potential errors and report it along with the original failure.
     *
     * @param message the message to use
     * @return AssertionError the prepared AssertionError instance
     */
    protected final AssertionError fail(String message) {
        StringBuilder b = new StringBuilder(64 + message.length());
        b.append(message);

        b.append(" (")
        .append("latch = ").append(done.getCount()).append(", ")
        .append("values = ").append(values.size()).append(", ")
        .append("errors = ").append(errors.size()).append(", ")
        .append("completions = ").append(completions)
        ;

        if (timeout) {
            b.append(", timeout!");
        }

        if (isDisposed()) {
            b.append(", disposed!");
        }

        CharSequence tag = this.tag;
        if (tag != null) {
            b.append(", tag = ")
            .append(tag);
        }

        b
        .append(')')
        ;

        AssertionError ae = new AssertionError(b.toString());
        if (!errors.isEmpty()) {
            if (errors.size() == 1) {
                ae.initCause(errors.get(0));
            } else {
                CompositeException ce = new CompositeException(errors);
                ae.initCause(ce);
            }
        }
        return ae;
    }

    /**
     * Awaits until this TestObserver/TestSubscriber receives an onError or onComplete events.
     * @return this
     * @throws InterruptedException if the current thread is interrupted while waiting
     * @see #awaitTerminalEvent()
     */
    @SuppressWarnings("unchecked")
    public final U await() throws InterruptedException {
        if (done.getCount() == 0) {
            return (U)this;
        }

        done.await();
        return (U)this;
    }

    /**
     * Awaits the specified amount of time or until this TestObserver/TestSubscriber
     * receives an onError or onComplete events, whichever happens first.
     * @param time the waiting time
     * @param unit the time unit of the waiting time
     * @return true if the TestObserver/TestSubscriber terminated, false if timeout happened
     * @throws InterruptedException if the current thread is interrupted while waiting
     * @see #awaitTerminalEvent(long, TimeUnit)
     */
    public final boolean await(long time, TimeUnit unit) throws InterruptedException {
        boolean d = done.getCount() == 0 || (done.await(time, unit));
        timeout = !d;
        return d;
    }

    // assertion methods

    /**
     * Assert that this TestObserver/TestSubscriber received exactly one onComplete event.
     * @return this;
     */
    @SuppressWarnings("unchecked")
    public final U assertComplete() {
        long c = completions;
        if (c == 0) {
            throw fail("Not completed");
        } else
        if (c > 1) {
            throw fail("Multiple completions: " + c);
        }
        return (U)this;
    }

    /**
     * Assert that this TestObserver/TestSubscriber has not received any onComplete event.
     * @return this;
     */
    @SuppressWarnings("unchecked")
    public final U assertNotComplete() {
        long c = completions;
        if (c == 1) {
            throw fail("Completed!");
        } else
        if (c > 1) {
            throw fail("Multiple completions: " + c);
        }
        return (U)this;
    }

    /**
     * Assert that this TestObserver/TestSubscriber has not received any onError event.
     * @return this;
     */
    @SuppressWarnings("unchecked")
    public final U assertNoErrors() {
        int s = errors.size();
        if (s != 0) {
            throw fail("Error(s) present: " + errors);
        }
        return (U)this;
    }

    /**
     * Assert that this TestObserver/TestSubscriber received exactly the specified onError event value.
     *
     * <p>The comparison is performed via Objects.equals(); since most exceptions don't
     * implement equals(), this assertion may fail. Use the {@link #assertError(Class)}
     * overload to test against the class of an error instead of an instance of an error
     * or {@link #assertError(Predicate)} to test with different condition.
     * @param error the error to check
     * @return this;
     * @see #assertError(Class)
     * @see #assertError(Predicate)
     */
    public final U assertError(Throwable error) {
        return assertError(Functions.equalsWith(error));
    }

    /**
     * Asserts that this TestObserver/TestSubscriber received exactly one onError event which is an
     * instance of the specified errorClass class.
     * @param errorClass the error class to expect
     * @return this;
     */
    @SuppressWarnings({ "unchecked", "rawtypes", "cast" })
    public final U assertError(Class<? extends Throwable> errorClass) {
        return (U)assertError((Predicate)Functions.isInstanceOf(errorClass));
    }

    /**
     * Asserts that this TestObserver/TestSubscriber received exactly one onError event for which
     * the provided predicate returns true.
     * @param errorPredicate
     *            the predicate that receives the error Throwable
     *            and should return true for expected errors.
     * @return this
     */
    @SuppressWarnings("unchecked")
    public final U assertError(Predicate<Throwable> errorPredicate) {
        int s = errors.size();
        if (s == 0) {
            throw fail("No errors");
        }

        boolean found = false;

        for (Throwable e : errors) {
            try {
                if (errorPredicate.test(e)) {
                    found = true;
                    break;
                }
            } catch (Exception ex) {
                throw ExceptionHelper.wrapOrThrow(ex);
            }
        }

        if (found) {
            if (s != 1) {
                throw fail("Error present but other errors as well");
            }
        } else {
            throw fail("Error not present");
        }
        return (U)this;
    }

    /**
     * Assert that this TestObserver/TestSubscriber received exactly one onNext value which is equal to
     * the given value with respect to Objects.equals.
     * @param value the value to expect
     * @return this;
     */
    @SuppressWarnings("unchecked")
    public final U assertValue(T value) {
        int s = values.size();
        if (s != 1) {
            throw fail("Expected: " + valueAndClass(value) + ", Actual: " + values);
        }
        T v = values.get(0);
        if (!ObjectHelper.equals(value, v)) {
            throw fail("Expected: " + valueAndClass(value) + ", Actual: " + valueAndClass(v));
        }
        return (U)this;
    }

    /**
     * Assert that this TestObserver/TestSubscriber did not receive an onNext value which is equal to
     * the given value with respect to Objects.equals.
     *
     * <p>History: 2.0.5 - experimental
     * @param value the value to expect not being received
     * @return this
     * @since 2.1
     */
    @SuppressWarnings("unchecked")
    public final U assertNever(T value) {
        int s = values.size();

        for (int i = 0; i < s; i++) {
            T v = this.values.get(i);
            if (ObjectHelper.equals(v, value)) {
                throw fail("Value at position " + i + " is equal to " + valueAndClass(value) + "; Expected them to be different");
            }
        }
        return (U) this;
    }

    /**
     * Asserts that this TestObserver/TestSubscriber received exactly one onNext value for which
     * the provided predicate returns true.
     * @param valuePredicate
     *            the predicate that receives the onNext value
     *            and should return true for the expected value.
     * @return this
     */
    @SuppressWarnings("unchecked")
    public final U assertValue(Predicate<T> valuePredicate) {
        assertValueAt(0, valuePredicate);

        if (values.size() > 1) {
            throw fail("Value present but other values as well");
        }

        return (U)this;
    }

    /**
     * Asserts that this TestObserver/TestSubscriber did not receive any onNext value for which
     * the provided predicate returns true.
     *
     * <p>History: 2.0.5 - experimental
     * @param valuePredicate the predicate that receives the onNext value
     *                       and should return true for the expected value.
     * @return this
     * @since 2.1
     */
    @SuppressWarnings("unchecked")
    public final U assertNever(Predicate<? super T> valuePredicate) {
        int s = values.size();

        for (int i = 0; i < s; i++) {
            T v = this.values.get(i);
            try {
                if (valuePredicate.test(v)) {
                    throw fail("Value at position " + i + " matches predicate " + valuePredicate.toString() + ", which was not expected.");
                }
            } catch (Exception ex) {
                throw ExceptionHelper.wrapOrThrow(ex);
            }
        }
        return (U)this;
    }

    /**
     * Asserts that this TestObserver/TestSubscriber received an onNext value at the given index
     * for the provided predicate returns true.
     * @param index the position to assert on
     * @param valuePredicate
     *            the predicate that receives the onNext value
     *            and should return true for the expected value.
     * @return this
     */
    @SuppressWarnings("unchecked")
    public final U assertValueAt(int index, Predicate<T> valuePredicate) {
        int s = values.size();
        if (s == 0) {
            throw fail("No values");
        }

        if (index >= values.size()) {
            throw fail("Invalid index: " + index);
        }

        boolean found = false;

        try {
            if (valuePredicate.test(values.get(index))) {
                found = true;
            }
        } catch (Exception ex) {
            throw ExceptionHelper.wrapOrThrow(ex);
        }

        if (!found) {
            throw fail("Value not present");
        }
        return (U)this;
    }

    /**
     * Appends the class name to a non-null value.
     * @param o the object
     * @return the string representation
     */
    public static String valueAndClass(Object o) {
        if (o != null) {
            return o + " (class: " + o.getClass().getSimpleName() + ")";
        }
        return "null";
    }

    /**
     * Assert that this TestObserver/TestSubscriber received the specified number onNext events.
     * @param count the expected number of onNext events
     * @return this;
     */
    @SuppressWarnings("unchecked")
    public final U assertValueCount(int count) {
        int s = values.size();
        if (s != count) {
            throw fail("Value counts differ; Expected: " + count + ", Actual: " + s);
        }
        return (U)this;
    }

    /**
     * Assert that this TestObserver/TestSubscriber has not received any onNext events.
     * @return this;
     */
    public final U assertNoValues() {
        return assertValueCount(0);
    }

    /**
     * Assert that the TestObserver/TestSubscriber received only the specified values in the specified order.
     * @param values the values expected
     * @return this;
     * @see #assertValueSet(Collection)
     */
    @SuppressWarnings("unchecked")
    public final U assertValues(T... values) {
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
        return (U)this;
    }

    /**
     * Assert that the TestObserver/TestSubscriber received only the specified values in any order.
     * <p>This helps asserting when the order of the values is not guaranteed, i.e., when merging
     * asynchronous streams.
     *
     * @param expected the collection of values expected in any order
     * @return this;
     */
    @SuppressWarnings("unchecked")
    public final U assertValueSet(Collection<? extends T> expected) {
        if (expected.isEmpty()) {
            assertNoValues();
            return (U)this;
        }
        for (T v : this.values) {
            if (!expected.contains(v)) {
                throw fail("Value not in the expected collection: " + valueAndClass(v));
            }
        }
        return (U)this;
    }

    /**
     * Assert that the TestObserver/TestSubscriber received only the specified sequence of values in the same order.
     * @param sequence the sequence of expected values in order
     * @return this;
     */
    @SuppressWarnings("unchecked")
    public final U assertValueSequence(Iterable<? extends T> sequence) {
        int i = 0;
        Iterator<T> vit = values.iterator();
        Iterator<? extends T> it = sequence.iterator();
        boolean actualNext;
        boolean expectedNext;
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
        }

        if (actualNext) {
            throw fail("More values received than expected (" + i + ")");
        }
        if (expectedNext) {
            throw fail("Fewer values received than expected (" + i + ")");
        }
        return (U)this;
    }

    /**
     * Assert that the TestObserver/TestSubscriber terminated (i.e., the terminal latch reached zero).
     * @return this;
     */
    @SuppressWarnings("unchecked")
    public final U assertTerminated() {
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
        return (U)this;
    }

    /**
     * Assert that the TestObserver/TestSubscriber has not terminated (i.e., the terminal latch is still non-zero).
     * @return this;
     */
    @SuppressWarnings("unchecked")
    public final U assertNotTerminated() {
        if (done.getCount() == 0) {
            throw fail("Subscriber terminated!");
        }
        return (U)this;
    }

    /**
     * Waits until the any terminal event has been received by this TestObserver/TestSubscriber
     * or returns false if the wait has been interrupted.
     * @return true if the TestObserver/TestSubscriber terminated, false if the wait has been interrupted
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
     * Awaits the specified amount of time or until this TestObserver/TestSubscriber
     * receives an onError or onComplete events, whichever happens first.
     * @param duration the waiting time
     * @param unit the time unit of the waiting time
     * @return true if the TestObserver/TestSubscriber terminated, false if timeout or interrupt happened
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
    @SuppressWarnings("unchecked")
    public final U assertErrorMessage(String message) {
        int s = errors.size();
        if (s == 0) {
            throw fail("No errors");
        } else
        if (s == 1) {
            Throwable e = errors.get(0);
            String errorMessage = e.getMessage();
            if (!ObjectHelper.equals(message, errorMessage)) {
                throw fail("Error message differs; Expected: " + message + ", Actual: " + errorMessage);
            }
        } else {
            throw fail("Multiple errors");
        }
        return (U)this;
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
     * Assert that the onSubscribe method was called exactly once.
     * @return this;
     */
    public abstract U assertSubscribed();

    /**
     * Assert that the onSubscribe method hasn't been called at all.
     * @return this;
     */
    public abstract U assertNotSubscribed();

    /**
     * Assert that the upstream signalled the specified values in order and
     * completed normally.
     * @param values the expected values, asserted in order
     * @return this
     * @see #assertFailure(Class, Object...)
     * @see #assertFailure(Predicate, Object...)
     * @see #assertFailureAndMessage(Class, String, Object...)
     */
    public final U assertResult(T... values) {
        return assertSubscribed()
                .assertValues(values)
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
    public final U assertFailure(Class<? extends Throwable> error, T... values) {
        return assertSubscribed()
                .assertValues(values)
                .assertError(error)
                .assertNotComplete();
    }

    /**
     * Assert that the upstream signalled the specified values in order and then failed
     * with a Throwable for which the provided predicate returns true.
     * @param errorPredicate
     *            the predicate that receives the error Throwable
     *            and should return true for expected errors.
     * @param values the expected values, asserted in order
     * @return this
     */
    public final U assertFailure(Predicate<Throwable> errorPredicate, T... values) {
        return assertSubscribed()
                .assertValues(values)
                .assertError(errorPredicate)
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
    public final U assertFailureAndMessage(Class<? extends Throwable> error,
            String message, T... values) {
        return assertSubscribed()
                .assertValues(values)
                .assertError(error)
                .assertErrorMessage(message)
                .assertNotComplete();
    }

    /**
     * Awaits until the internal latch is counted down.
     * <p>If the wait times out or gets interrupted, the TestObserver/TestSubscriber is cancelled.
     * @param time the waiting time
     * @param unit the time unit of the waiting time
     * @return this
     * @throws RuntimeException wrapping an InterruptedException if the wait is interrupted
     */
    @SuppressWarnings("unchecked")
    public final U awaitDone(long time, TimeUnit unit) {
        try {
            if (!done.await(time, unit)) {
                timeout = true;
                dispose();
            }
        } catch (InterruptedException ex) {
            dispose();
            throw ExceptionHelper.wrapOrThrow(ex);
        }
        return (U)this;
    }


    /**
     * Assert that the TestObserver/TestSubscriber has received a Disposable but no other events.
     * @return this
     */
    public final U assertEmpty() {
        return assertSubscribed()
                .assertNoValues()
                .assertNoErrors()
                .assertNotComplete();
    }

    /**
     * Set the tag displayed along with an assertion failure's
     * other state information.
     * <p>History: 2.0.7 - experimental
     * @param tag the string to display (null won't print any tag)
     * @return this
     * @since 2.1
     */
    @SuppressWarnings("unchecked")
    public final U withTag(CharSequence tag) {
        this.tag = tag;
        return (U)this;
    }

    /**
     * Enumeration of default wait strategies when waiting for a specific number of
     * items in {@link BaseTestConsumer#awaitCount(int, Runnable)}.
     * <p>History: 2.0.7 - experimental
     * @since 2.1
     */
    public enum TestWaitStrategy implements Runnable {
        /** The wait loop will spin as fast as possible. */
        SPIN {
            @Override
            public void run() {
                // nothing to do
            }
        },
        /** The current thread will be yielded. */
        YIELD {
            @Override
            public void run() {
                Thread.yield();
            }
        },
        /** The current thread sleeps for 1 millisecond. */
        SLEEP_1MS {
            @Override
            public void run() {
                sleep(1);
            }
        },
        /** The current thread sleeps for 10 milliseconds. */
        SLEEP_10MS {
            @Override
            public void run() {
                sleep(10);
            }
        },
        /** The current thread sleeps for 100 milliseconds. */
        SLEEP_100MS {
            @Override
            public void run() {
                sleep(100);
            }
        },
        /** The current thread sleeps for 1000 milliseconds. */
        SLEEP_1000MS {
            @Override
            public void run() {
                sleep(1000);
            }
        }
        ;

        @Override
        public abstract void run();

        static void sleep(int millis) {
            try {
                Thread.sleep(millis);
            } catch (InterruptedException ex) {
                throw new RuntimeException(ex);
            }
        }
    }


    /**
     * Await until the TestObserver/TestSubscriber receives the given
     * number of items or terminates by sleeping 10 milliseconds at a time
     * up to 5000 milliseconds of timeout.
     * <p>History: 2.0.7 - experimental
     * @param atLeast the number of items expected at least
     * @return this
     * @see #awaitCount(int, Runnable, long)
     * @since 2.1
     */
    public final U awaitCount(int atLeast) {
        return awaitCount(atLeast, TestWaitStrategy.SLEEP_10MS, 5000);
    }

    /**
     * Await until the TestObserver/TestSubscriber receives the given
     * number of items or terminates by waiting according to the wait
     * strategy and up to 5000 milliseconds of timeout.
     * <p>History: 2.0.7 - experimental
     * @param atLeast the number of items expected at least
     * @param waitStrategy a Runnable called when the current received count
     *                     hasn't reached the expected value and there was
     *                     no terminal event either, see {@link TestWaitStrategy}
     *                     for examples
     * @return this
     * @see #awaitCount(int, Runnable, long)
     * @since 2.1
     */
    public final U awaitCount(int atLeast, Runnable waitStrategy) {
        return awaitCount(atLeast, waitStrategy, 5000);
    }

    /**
     * Await until the TestObserver/TestSubscriber receives the given
     * number of items or terminates.
     * <p>History: 2.0.7 - experimental
     * @param atLeast the number of items expected at least
     * @param waitStrategy a Runnable called when the current received count
     *                     hasn't reached the expected value and there was
     *                     no terminal event either, see {@link TestWaitStrategy}
     *                     for examples
     * @param timeoutMillis if positive, the await ends if the specified amount of
     *                      time has passed no matter how many items were received
     * @return this
     * @since 2.1
     */
    @SuppressWarnings("unchecked")
    public final U awaitCount(int atLeast, Runnable waitStrategy, long timeoutMillis) {
        long start = System.currentTimeMillis();
        for (;;) {
            if (timeoutMillis > 0L && System.currentTimeMillis() - start >= timeoutMillis) {
                timeout = true;
                break;
            }
            if (done.getCount() == 0L) {
                break;
            }
            if (values.size() >= atLeast) {
                break;
            }

            waitStrategy.run();
        }
        return (U)this;
    }

    /**
     * @return true if one of the timeout-based await methods has timed out.
     * <p>History: 2.0.7 - experimental
     * @see #clearTimeout()
     * @see #assertTimeout()
     * @see #assertNoTimeout()
     * @since 2.1
     */
    public final boolean isTimeout() {
        return timeout;
    }

    /**
     * Clears the timeout flag set by the await methods when they timed out.
     * <p>History: 2.0.7 - experimental
     * @return this
     * @since 2.1
     * @see #isTimeout()
     */
    @SuppressWarnings("unchecked")
    public final U clearTimeout() {
        timeout = false;
        return (U)this;
    }

    /**
     * Asserts that some awaitX method has timed out.
     * <p>History: 2.0.7 - experimental
     * @return this
     * @since 2.1
     */
    @SuppressWarnings("unchecked")
    public final U assertTimeout() {
        if (!timeout) {
            throw fail("No timeout?!");
        }
        return (U)this;
    }


    /**
     * Asserts that some awaitX method has not timed out.
     * <p>History: 2.0.7 - experimental
     * @return this
     * @since 2.1
     */
    @SuppressWarnings("unchecked")
    public final U assertNoTimeout() {
        if (timeout) {
            throw fail("Timeout?!");
        }
        return (U)this;
    }
}
