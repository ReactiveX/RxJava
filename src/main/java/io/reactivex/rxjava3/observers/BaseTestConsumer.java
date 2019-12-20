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

import io.reactivex.rxjava3.annotations.*;
import io.reactivex.rxjava3.exceptions.CompositeException;
import io.reactivex.rxjava3.functions.Predicate;
import io.reactivex.rxjava3.internal.functions.Functions;
import io.reactivex.rxjava3.internal.util.*;

/**
 * Base class with shared infrastructure to support
 * {@link io.reactivex.rxjava3.subscribers.TestSubscriber TestSubscriber} and {@link TestObserver}.
 * @param <T> the value type consumed
 * @param <U> the subclass of this {@code BaseTestConsumer}
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
     * Indicates that one of the {@code awaitX} method has timed out.
     * @since 2.0.7
     */
    protected boolean timeout;

    public BaseTestConsumer() {
        this.values = new VolatileSizeArrayList<>();
        this.errors = new VolatileSizeArrayList<>();
        this.done = new CountDownLatch(1);
    }

    /**
     * Returns a shared list of received {@code onNext} values or the single {@code onSuccess} value.
     * <p>
     * Note that accessing the items via certain methods of the {@link List}
     * interface while the upstream is still actively emitting
     * more items may result in a {@code ConcurrentModificationException}.
     * <p>
     * The {@link List#size()} method will return the number of items
     * already received by this {@code TestObserver}/{@code TestSubscriber} in a thread-safe
     * manner that can be read via {@link List#get(int)}) method
     * (index range of 0 to {@code List.size() - 1}).
     * <p>
     * A view of the returned List can be created via {@link List#subList(int, int)}
     * by using the bounds 0 (inclusive) to {@link List#size()} (exclusive) which,
     * when accessed in a read-only fashion, should be also thread-safe and not throw any
     * {@code ConcurrentModificationException}.
     * @return a list of received onNext values
     */
    @NonNull
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
    @NonNull
    protected final AssertionError fail(@NonNull String message) {
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
     * Awaits until this {@code TestObserver}/{@code TestSubscriber} receives an {@code onError} or {@code onComplete} events.
     * @return this
     * @throws InterruptedException if the current thread is interrupted while waiting
     */
    @SuppressWarnings("unchecked")
    @NonNull
    public final U await() throws InterruptedException {
        if (done.getCount() == 0) {
            return (U)this;
        }

        done.await();
        return (U)this;
    }

    /**
     * Awaits the specified amount of time or until this {@code TestObserver}/{@code TestSubscriber}
     * receives an {@code onError} or {@code onComplete} events, whichever happens first.
     * @param time the waiting time
     * @param unit the time unit of the waiting time
     * @return true if the {@code TestObserver}/{@code TestSubscriber} terminated, false if timeout happened
     * @throws InterruptedException if the current thread is interrupted while waiting
     */
    public final boolean await(long time, @NonNull TimeUnit unit) throws InterruptedException {
        boolean d = done.getCount() == 0 || (done.await(time, unit));
        timeout = !d;
        return d;
    }

    // assertion methods

    /**
     * Assert that this {@code TestObserver}/{@code TestSubscriber} received exactly one {@code onComplete} event.
     * @return this
     */
    @SuppressWarnings("unchecked")
    @NonNull
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
     * Assert that this {@code TestObserver}/{@code TestSubscriber} has not received an {@code onComplete} event.
     * @return this
     */
    @SuppressWarnings("unchecked")
    @NonNull
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
     * Assert that this {@code TestObserver}/{@code TestSubscriber} has not received an {@code onError} event.
     * @return this
     */
    @SuppressWarnings("unchecked")
    @NonNull
    public final U assertNoErrors() {
        int s = errors.size();
        if (s != 0) {
            throw fail("Error(s) present: " + errors);
        }
        return (U)this;
    }

    /**
     * Assert that this {@code TestObserver}/{@code TestSubscriber} received exactly the specified {@code onError} event value.
     *
     * <p>The comparison is performed via {@link Objects#equals(Object, Object)}; since most exceptions don't
     * implement equals(), this assertion may fail. Use the {@link #assertError(Class)}
     * overload to test against the class of an error instead of an instance of an error
     * or {@link #assertError(Predicate)} to test with different condition.
     * @param error the error to check
     * @return this
     * @see #assertError(Class)
     * @see #assertError(Predicate)
     */
    @NonNull
    public final U assertError(@NonNull Throwable error) {
        return assertError(Functions.equalsWith(error));
    }

    /**
     * Asserts that this {@code TestObserver}/{@code TestSubscriber} received exactly one {@code onError} event which is an
     * instance of the specified {@code errorClass} {@link Class}.
     * @param errorClass the error {@code Class} to expect
     * @return this
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @NonNull
    public final U assertError(@NonNull Class<? extends Throwable> errorClass) {
        return (U)assertError((Predicate)Functions.isInstanceOf(errorClass));
    }

    /**
     * Asserts that this {@code TestObserver}/{@code TestSubscriber} received exactly one {@code onError} event for which
     * the provided predicate returns {@code true}.
     * @param errorPredicate
     *            the predicate that receives the error {@link Throwable}
     *            and should return {@code true} for expected errors.
     * @return this
     */
    @SuppressWarnings("unchecked")
    @NonNull
    public final U assertError(@NonNull Predicate<Throwable> errorPredicate) {
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
     * Assert that this {@code TestObserver}/{@code TestSubscriber} received exactly one {@code onNext} value which is equal to
     * the given value with respect to {@link Objects#equals(Object, Object)}.
     * @param value the value to expect
     * @return this
     */
    @SuppressWarnings("unchecked")
    @NonNull
    public final U assertValue(@NonNull T value) {
        int s = values.size();
        if (s != 1) {
            throw fail("expected: " + valueAndClass(value) + " but was: " + values);
        }
        T v = values.get(0);
        if (!Objects.equals(value, v)) {
            throw fail("expected: " + valueAndClass(value) + " but was: " + valueAndClass(v));
        }
        return (U)this;
    }

    /**
     * Asserts that this {@code TestObserver}/{@code TestSubscriber} received exactly one {@code onNext} value for which
     * the provided predicate returns {@code true}.
     * @param valuePredicate
     *            the predicate that receives the {@code onNext} value
     *            and should return {@code true} for the expected value.
     * @return this
     */
    @SuppressWarnings("unchecked")
    @NonNull
    public final U assertValue(@NonNull Predicate<T> valuePredicate) {
        assertValueAt(0, valuePredicate);

        if (values.size() > 1) {
            throw fail("Value present but other values as well");
        }

        return (U)this;
    }

    /**
     * Asserts that this {@code TestObserver}/{@code TestSubscriber} received an {@code onNext} value at the given index
     * which is equal to the given value with respect to {@code null}-safe {@link Objects#equals(Object, Object)}.
     * <p>History: 2.1.3 - experimental
     * @param index the position to assert on
     * @param value the value to expect
     * @return this
     * @since 2.2
     */
    @SuppressWarnings("unchecked")
    @NonNull
    public final U assertValueAt(int index, @NonNull T value) {
        int s = values.size();
        if (s == 0) {
            throw fail("No values");
        }

        if (index >= s) {
            throw fail("Invalid index: " + index);
        }

        T v = values.get(index);
        if (!Objects.equals(value, v)) {
            throw fail("expected: " + valueAndClass(value) + " but was: " + valueAndClass(v));
        }
        return (U)this;
    }

    /**
     * Asserts that this {@code TestObserver}/{@code TestSubscriber} received an {@code onNext} value at the given index
     * for the provided predicate returns {@code true}.
     * @param index the position to assert on
     * @param valuePredicate
     *            the predicate that receives the {@code onNext} value
     *            and should return {@code true} for the expected value.
     * @return this
     */
    @SuppressWarnings("unchecked")
    @NonNull
    public final U assertValueAt(int index, @NonNull Predicate<T> valuePredicate) {
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
     * Appends the class name to a non-{@code null} value or returns {@code "null"}.
     * @param o the object
     * @return the string representation
     */
    @NonNull
    public static String valueAndClass(@Nullable Object o) {
        if (o != null) {
            return o + " (class: " + o.getClass().getSimpleName() + ")";
        }
        return "null";
    }

    /**
     * Assert that this {@code TestObserver}/{@code TestSubscriber} received the specified number {@code onNext} events.
     * @param count the expected number of {@code onNext} events
     * @return this
     */
    @SuppressWarnings("unchecked")
    @NonNull
    public final U assertValueCount(int count) {
        int s = values.size();
        if (s != count) {
            throw fail("Value counts differ; expected: " + count + " but was: " + s);
        }
        return (U)this;
    }

    /**
     * Assert that this {@code TestObserver}/{@code TestSubscriber} has not received any {@code onNext} events.
     * @return this
     */
    @NonNull
    public final U assertNoValues() {
        return assertValueCount(0);
    }

    /**
     * Assert that the {@code TestObserver}/{@code TestSubscriber} received only the specified values in the specified order.
     * @param values the values expected
     * @return this
     */
    @SuppressWarnings("unchecked")
    @SafeVarargs
    @NonNull
    public final U assertValues(@NonNull T... values) {
        int s = this.values.size();
        if (s != values.length) {
            throw fail("Value count differs; expected: " + values.length + " " + Arrays.toString(values)
            + " but was: " + s + " " + this.values);
        }
        for (int i = 0; i < s; i++) {
            T v = this.values.get(i);
            T u = values[i];
            if (!Objects.equals(u, v)) {
                throw fail("Values at position " + i + " differ; expected: " + valueAndClass(u) + " but was: " + valueAndClass(v));
            }
        }
        return (U)this;
    }

    /**
     * Assert that the {@code TestObserver}/{@code TestSubscriber} received only the specified values in the specified order without terminating.
     * <p>History: 2.1.4 - experimental
     * @param values the values expected
     * @return this
     * @since 2.2
     */
    @SafeVarargs
    @NonNull
    public final U assertValuesOnly(@NonNull T... values) {
        return assertSubscribed()
                .assertValues(values)
                .assertNoErrors()
                .assertNotComplete();
    }

    /**
     * Assert that the {@code TestObserver}/{@code TestSubscriber} received only the specified sequence of values in the same order.
     * @param sequence the sequence of expected values in order
     * @return this
     */
    @SuppressWarnings("unchecked")
    @NonNull
    public final U assertValueSequence(@NonNull Iterable<? extends T> sequence) {
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

            if (!Objects.equals(u, v)) {
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
     * Assert that the {@code onSubscribe} method was called exactly once.
     * @return this
     */
    @NonNull
    protected abstract U assertSubscribed();

    /**
     * Assert that the upstream signaled the specified values in order and
     * completed normally.
     * @param values the expected values, asserted in order
     * @return this
     * @see #assertFailure(Class, Object...)
     */
    @SafeVarargs
    @NonNull
    public final U assertResult(@NonNull T... values) {
        return assertSubscribed()
                .assertValues(values)
                .assertNoErrors()
                .assertComplete();
    }

    /**
     * Assert that the upstream signaled the specified values in order
     * and then failed with a specific class or subclass of {@link Throwable}.
     * @param error the expected exception (parent) {@link Class}
     * @param values the expected values, asserted in order
     * @return this
     */
    @SafeVarargs
    @NonNull
    public final U assertFailure(@NonNull Class<? extends Throwable> error, @NonNull T... values) {
        return assertSubscribed()
                .assertValues(values)
                .assertError(error)
                .assertNotComplete();
    }

    /**
     * Awaits until the internal latch is counted down.
     * <p>If the wait times out or gets interrupted, the {@code TestObserver}/{@code TestSubscriber} is cancelled.
     * @param time the waiting time
     * @param unit the time unit of the waiting time
     * @return this
     * @throws RuntimeException wrapping an {@link InterruptedException} if the wait is interrupted
     */
    @SuppressWarnings("unchecked")
    @NonNull
    public final U awaitDone(long time, @NonNull TimeUnit unit) {
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
     * Assert that the {@code TestObserver}/{@code TestSubscriber} has received a
     * {@link io.reactivex.rxjava3.disposables.Disposable Disposable}/{@link org.reactivestreams.Subscription Subscription}
     * via {@code onSubscribe} but no other events.
     * @return this
     */
    @NonNull
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
     * @param tag the string to display ({@code null} won't print any tag)
     * @return this
     * @since 2.1
     */
    @SuppressWarnings("unchecked")
    @NonNull
    public final U withTag(@Nullable CharSequence tag) {
        this.tag = tag;
        return (U)this;
    }

    /**
     * Await until the {@code TestObserver}/{@code TestSubscriber} receives the given
     * number of items or terminates by sleeping 10 milliseconds at a time
     * up to 5000 milliseconds of timeout.
     * <p>History: 2.0.7 - experimental
     * @param atLeast the number of items expected at least
     * @return this
     * @since 2.1
     */
    @SuppressWarnings("unchecked")
    @NonNull
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
