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

package io.reactivex.rxjava3.testsupport;

import java.util.List;

import io.reactivex.rxjava3.functions.Predicate;
import io.reactivex.rxjava3.internal.fuseable.QueueFuseable;
import io.reactivex.rxjava3.internal.util.ExceptionHelper;
import io.reactivex.rxjava3.observers.BaseTestConsumer;
import java.util.Objects;

/**
 * Base class with shared infrastructure to support TestSubscriber and TestObserver.
 * @param <T> the value type consumed
 * @param <U> the subclass of this BaseTestConsumer
 */
public abstract class BaseTestConsumerEx<T, U extends BaseTestConsumerEx<T, U>>
extends BaseTestConsumer<T, U> {

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

    public BaseTestConsumerEx() {
        super();
    }

    /**
     * Returns the last thread which called the onXXX methods of this TestObserver/TestSubscriber.
     * @return the last thread which called the onXXX methods
     */
    public final Thread lastThread() {
        return lastThread;
    }

    // assertion methods

    /**
     * Assert that this TestObserver/TestSubscriber did not receive an onNext value which is equal to
     * the given value with respect to null-safe Object.equals.
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
            if (Objects.equals(v, value)) {
                throw fail("Value at position " + i + " is equal to " + valueAndClass(value) + "; Expected them to be different");
            }
        }
        return (U) this;
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
            } catch (Throwable ex) {
                throw ExceptionHelper.wrapOrThrow(ex);
            }
        }
        return (U)this;
    }

    /**
     * Assert that the TestObserver/TestSubscriber terminated (i.e., the terminal latch reached zero).
     * @return this
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
     * @return this
     */
    @SuppressWarnings("unchecked")
    public final U assertNotTerminated() {
        if (done.getCount() == 0) {
            throw fail("Subscriber terminated!");
        }
        return (U)this;
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
            if (!Objects.equals(message, errorMessage)) {
                throw fail("Error message differs; exptected: " + message + " but was: " + errorMessage);
            }
        } else {
            throw fail("Multiple errors");
        }
        return (U)this;
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
    @SafeVarargs
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
    @SafeVarargs
    public final U assertFailureAndMessage(Class<? extends Throwable> error,
            String message, T... values) {
        return assertSubscribed()
                .assertValues(values)
                .assertError(error)
                .assertErrorMessage(message)
                .assertNotComplete();
    }

    /**
     * Returns true if an await timed out.
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

    /**
     * Returns the internal shared list of errors.
     * @return Returns the internal shared list of errors.
     */
    public final List<Throwable> errors() {
        return errors;
    }

    /**
     * Returns true if this test consumer has terminated in any fashion.
     * @return true if this test consumer has terminated in any fashion
     */
    public final boolean isTerminated() {
        return done.getCount() == 0;
    }

    /**
     * Returns the number of times onComplete() was called.
     * @return the number of times onComplete() was called
     */
    public final long completions() {
        return completions;
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
    public final AssertionError failWith(String message) {
        return fail(message);
    }

    static String fusionModeToString(int mode) {
        switch (mode) {
        case QueueFuseable.NONE : return "NONE";
        case QueueFuseable.SYNC : return "SYNC";
        case QueueFuseable.ASYNC : return "ASYNC";
        default: return "Unknown(" + mode + ")";
        }
    }
}
