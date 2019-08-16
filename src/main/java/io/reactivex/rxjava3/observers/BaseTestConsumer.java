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

package io.reactivex.rxjava3.observers;

import java.util.*;
import java.util.concurrent.*;

import io.reactivex.rxjava3.exceptions.CompositeException;
import io.reactivex.rxjava3.functions.Predicate;
import io.reactivex.rxjava3.internal.functions.*;
import io.reactivex.rxjava3.internal.util.*;

/**
 * Base class with shared infrastructure to support TestSubscriber and TestObserver.
 * @param <T> the value type consumed
 * @param <U> the subclass of this BaseTestConsumer
 */
public abstract class BaseTestConsumer<T, U extends BaseTestConsumer<T, U>> {
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
     * Returns a shared list of received onNext values.
     * <p>
     * Note that accessing the items via certain methods of the {@link List}
     * interface while the upstream is still actively emitting
     * more items may result in a {@code ConcurrentModificationException}.
     * <p>
     * The {@link List#size()} method will return the number of items
     * already received by this TestObserver/TestSubscriber in a thread-safe
     * manner that can be read via {@link List#get(int)}) method
     * (index range of 0 to {@code List.size() - 1}).
     * <p>
     * A view of the returned List can be created via {@link List#subList(int, int)}
     * by using the bounds 0 (inclusive) to {@link List#size()} (exclusive) which,
     * when accessed in a read-only fashion, should be also thread-safe and not throw any
     * {@code ConcurrentModificationException}.
     * @return a list of received onNext values
     */
    public final List<T> values() {
        return values;
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
     */
    public final boolean await(long time, TimeUnit unit) throws InterruptedException {
        boolean d = done.getCount() == 0 || (done.await(time, unit));
        timeout = !d;
        return d;
    }

    // assertion methods

    /**
     * Assert that this TestObserver/TestSubscriber received exactly one onComplete event.
     * @return this
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
     * @return this
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
     * @return this
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
     * @return this
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
     * @return this
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
            } catch (Throwable ex) {
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
     * @return this
     */
    @SuppressWarnings("unchecked")
    public final U assertValue(T value) {
        int s = values.size();
        if (s != 1) {
            throw fail("expected: " + valueAndClass(value) + " but was: " + values);
        }
        T v = values.get(0);
        if (!ObjectHelper.equals(value, v)) {
            throw fail("expected: " + valueAndClass(value) + " but was: " + valueAndClass(v));
        }
        return (U)this;
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
     * Asserts that this TestObserver/TestSubscriber received an onNext value at the given index
     * which is equal to the given value with respect to null-safe Object.equals.
     * <p>History: 2.1.3 - experimental
     * @param index the position to assert on
     * @param value the value to expect
     * @return this
     * @since 2.2
     */
    @SuppressWarnings("unchecked")
    public final U assertValueAt(int index, T value) {
        int s = values.size();
        if (s == 0) {
            throw fail("No values");
        }

        if (index >= s) {
            throw fail("Invalid index: " + index);
        }

        T v = values.get(index);
        if (!ObjectHelper.equals(value, v)) {
            throw fail("expected: " + valueAndClass(value) + " but was: " + valueAndClass(v));
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
        } catch (Throwable ex) {
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
     * @return this
     */
    @SuppressWarnings("unchecked")
    public final U assertValueCount(int count) {
        int s = values.size();
        if (s != count) {
            throw fail("Value counts differ; expected: " + count + " but was: " + s);
        }
        return (U)this;
    }

    /**
     * Assert that this TestObserver/TestSubscriber has not received any onNext events.
     * @return this
     */
    public final U assertNoValues() {
        return assertValueCount(0);
    }

    /**
     * Assert that the TestObserver/TestSubscriber received only the specified values in the specified order.
     * @param values the values expected
     * @return this
     */
    @SuppressWarnings("unchecked")
    public final U assertValues(T... values) {
        int s = this.values.size();
        if (s != values.length) {
            throw fail("Value count differs; expected: " + values.length + " " + Arrays.toString(values)
            + " but was: " + s + " " + this.values);
        }
        for (int i = 0; i < s; i++) {
            T v = this.values.get(i);
            T u = values[i];
            if (!ObjectHelper.equals(u, v)) {
                throw fail("Values at position " + i + " differ; expected: " + valueAndClass(u) + " but was: " + valueAndClass(v));
            }
        }
        return (U)this;
    }

    /**
     * Assert that the TestObserver/TestSubscriber received only the specified values in the specified order without terminating.
     * <p>History: 2.1.4 - experimental
     * @param values the values expected
     * @return this
     * @since 2.2
     */
    public final U assertValuesOnly(T... values) {
        return assertSubscribed()
                .assertValues(values)
                .assertNoErrors()
                .assertNotComplete();
    }

    /**
     * Assert that the TestObserver/TestSubscriber received only the specified sequence of values in the same order.
     * @param sequence the sequence of expected values in order
     * @return this
     */
    @SuppressWarnings("unchecked")
    public final U assertValueSequence(Iterable<? extends T> sequence) {
        int i = 0;
        Iterator<T> actualIterator = values.iterator();
        Iterator<? extends T> expectedIterator = sequence.iterator();
        boolean actualNext;
        boolean expectedNext;
        for (;;) {
            expectedNext = expectedIterator.hasNext();
            actualNext = actualIterator.hasNext();

            if (!actualNext || !expectedNext) {
                break;
            }

            T u = expectedIterator.next();
            T v = actualIterator.next();

            if (!ObjectHelper.equals(u, v)) {
                throw fail("Values at position " + i + " differ; expected: " + valueAndClass(u) + " but was: " + valueAndClass(v));
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
     * Assert that the onSubscribe method was called exactly once.
     * @return this
     */
    protected abstract U assertSubscribed();

    /**
     * Assert that the upstream signalled the specified values in order and
     * completed normally.
     * @param values the expected values, asserted in order
     * @return this
     * @see #assertFailure(Class, Object...)
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
     * Await until the TestObserver/TestSubscriber receives the given
     * number of items or terminates by sleeping 10 milliseconds at a time
     * up to 5000 milliseconds of timeout.
     * <p>History: 2.0.7 - experimental
     * @param atLeast the number of items expected at least
     * @return this
     * @since 2.1
     */
    @SuppressWarnings("unchecked")
    public final U awaitCount(int atLeast) {
        long start = System.currentTimeMillis();
        long timeoutMillis = 5000;
        for (;;) {
            if (System.currentTimeMillis() - start >= timeoutMillis) {
                timeout = true;
                break;
            }
            if (done.getCount() == 0L) {
                break;
            }
            if (values.size() >= atLeast) {
                break;
            }

            try {
                Thread.sleep(10);
            } catch (InterruptedException ex) {
                throw new RuntimeException(ex);
            }
        }
        return (U)this;
    }

    /**
     * Returns true if this test consumer was cancelled/disposed.
     * @return true if this test consumer was cancelled/disposed.
     */
    protected abstract boolean isDisposed();

    /**
     * Cancel/dispose this test consumer.
     */
    protected abstract void dispose();
}
