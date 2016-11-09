/**
 * Copyright 2016 Netflix, Inc.
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

import java.util.List;
import java.util.concurrent.TimeUnit;

import rx.*;
import rx.annotations.Experimental;
import rx.functions.Action0;

/**
 * Interface for asserting the state of a sequence under testing with a {@code test()}
 * method of a reactive base class.
 * <p>
 * This interface is not intended to be implemented outside of RxJava.
 * <p>
 * This interface extends {@link Observer} and allows injecting onXXX signals into
 * the testing process.
 * @param <T> the value type consumed by this Observer
 * @since 1.2.3
 */
@Experimental
public interface AssertableSubscriber<T> extends Observer<T>, Subscription {

    /**
     * Allows manually calling the {@code onStart} method of the underlying Subscriber.
     */
    void onStart();

    /**
     * Allows manually calling the {@code setProducer} method of the underlying Subscriber.
     * @param p the producer to use, not null
     */
    void setProducer(Producer p);

    /**
     * Returns the number of {@code onCompleted} signals received by this Observer.
     * @return the number of {@code onCompleted} signals received
     */
    int getCompletions();

    /**
     * Returns a list of received {@code onError} signals.
     * @return this
     */
    List<Throwable> getOnErrorEvents();

    /**
     * Returns the number of {@code onNext} signals received by this Observer in
     * a thread-safe manner; one can read up to this number of elements from
     * the {@code List} returned by {@link #getOnNextEvents()}.
     * @return the number of {@code onNext} signals received.
     */
    int getValueCount();

    /**
     * Requests the specified amount of items from upstream.
     * @param n the amount requested, non-negative
     * @return this
     */
    AssertableSubscriber<T> requestMore(long n);

    /**
     * Returns the list of received {@code onNext} events.
     * <p>If the sequence hasn't completed yet and is asynchronous, use the
     * {@link #getValueCount()} method to determine how many elements are safe
     * to be read from the list returned by this method.
     * @return the List of received {@code onNext} events.
     */
    List<T> getOnNextEvents();

    /**
     * Assert that this Observer received the given list of items as {@code onNext} signals
     * in the same order and with the default null-safe object equals comparison.
     * @param items the List of items expected
     * @return this
     */
    AssertableSubscriber<T> assertReceivedOnNext(List<T> items);

    /**
     * Assert that this Observer receives at least the given number of {@code onNext}
     * signals within the specified timeout period.
     * <p>
     * Note that it is possible the AssertionError thrown by this method will
     * contain an actual value &gt;= to the expected one in case there is an emission
     * race or unexpected delay on the emitter side. In this case, increase the timeout
     * amount to avoid false positives.
     *
     * @param expected the expected (at least) number of {@code onNext} signals
     * @param timeout the timeout to wait to receive the given number of {@code onNext} events
     * @param unit the time unit
     * @return this
     */
    AssertableSubscriber<T> awaitValueCount(int expected, long timeout, TimeUnit unit);

    /**
     * Assert that this Observer received either an {@code onError} or {@code onCompleted} signal.
     * @return this
     */
    AssertableSubscriber<T> assertTerminalEvent();

    /**
     * Assert that this Observer has been unsubscribed via {@code unsubscribe()} or by a wrapping
     * {@code SafeSubscriber}.
     * @return this
     */
    AssertableSubscriber<T> assertUnsubscribed();

    /**
     * Assert that this Observer has not received any {@code onError} signal.
     * @return this
     */
    AssertableSubscriber<T> assertNoErrors();

    /**
     * Waits for an {@code onError} or {code onCompleted} terminal event indefinitely.
     * @return this
     */
    AssertableSubscriber<T> awaitTerminalEvent();


    /**
     * Waits for an {@code onError} or {code onCompleted} terminal event for the given
     * amount of timeout.
     * @param timeout the time to wait for the terminal event
     * @param unit the time unit of the wait time
     * @return this
     */
    AssertableSubscriber<T> awaitTerminalEvent(long timeout, TimeUnit unit);

    /**
     * Waits for an {@code onError} or {code onCompleted} terminal event for the given
     * amount of timeout and unsubscribes the sequence if the timeout passed or the
     * wait itself is interrupted.
     * @param timeout the time to wait for the terminal event
     * @param unit the time unit of the wait time
     * @return this
     */
    AssertableSubscriber<T> awaitTerminalEventAndUnsubscribeOnTimeout(long timeout,
            TimeUnit unit);

    /**
     * Returns the Thread that has called the last {@code onNext}, {@code onError} or
     * {@code onCompleted} methods of this Observer.
     * @return this
     */
    Thread getLastSeenThread();

    /**
     * Assert that this Observer received exaclty one {@code onCompleted} signal.
     * @return this
     */
    AssertableSubscriber<T> assertCompleted();

    /**
     * Assert that this Observer received no {@code onCompleted} signal.
     * @return this
     */
    AssertableSubscriber<T> assertNotCompleted();

    /**
     * Assert that this Observer received one {@code onError} signal with
     * the given subclass of a Throwable as type.
     * @param clazz the expected type of the {@code onError} signal received
     * @return this
     */
    AssertableSubscriber<T> assertError(Class<? extends Throwable> clazz);

    /**
     * Assert that this Observer received one {@code onError} signal with the
     * object-equals of the given Throwable instance
     * @param throwable the Throwable instance expected
     * @return this
     */
    AssertableSubscriber<T> assertError(Throwable throwable);

    /**
     * Assert that no {@code onError} or {@code onCompleted} signals were received so far.
     * @return this
     */
    AssertableSubscriber<T> assertNoTerminalEvent();

    /**
     * Assert that no {@code onNext} signals were received so far.
     * @return this
     */
    AssertableSubscriber<T> assertNoValues();

    /**
     * Assert that this Observer received exactly the given count of
     * {@code onNext} signals.
     * @param count the expected number of {@code onNext} signals
     * @return this
     */
    AssertableSubscriber<T> assertValueCount(int count);

    /**
     * Assert that this Observer received exactly the given expected values
     * (compared via null-safe object equals) in the given order.
     * @param values the expected values
     * @return this
     */
    AssertableSubscriber<T> assertValues(T... values);

    /**
     * Assert that this Observer received exactly the given single expected value
     * (compared via null-safe object equals).
     * @param value the single value expected
     * @return this
     */
    AssertableSubscriber<T> assertValue(T value);

    /**
     * Assert that this Observer received exactly the given values (compared via
     * null-safe object equals) and if so, clears the internal buffer of the
     * underlying Subscriber of these values.
     * @param expectedFirstValue the first value expected
     * @param expectedRestValues the rest of the values expected
     * @return this
     */
    AssertableSubscriber<T> assertValuesAndClear(T expectedFirstValue,
            T... expectedRestValues);

    /**
     * Performs an action given by the Action0 callback in a fluent manner.
     * @param action the action to perform, not null
     * @return this
     */
    AssertableSubscriber<T> perform(Action0 action);

    @Override
    void unsubscribe();

    @Override
    boolean isUnsubscribed();

    /**
     * Assert that this Observer received the specified items in the given order followed
     * by a completion signal and no errors.
     * @param values the values expected
     * @return this
     */
    AssertableSubscriber<T> assertResult(T... values);

    /**
     * Assert that this Observer received the specified items in the given order followed
     * by an error signal of the given type (but no completion signal).
     * @param errorClass the expected Throwable subclass type
     * @param values the expected values
     * @return this
     */
    AssertableSubscriber<T> assertFailure(Class<? extends Throwable> errorClass, T... values);

    /**
     * Assert that this Observer received the specified items in the given order followed
     * by an error signal of the given type and with the exact error message (but no completion signal).
     * @param errorClass the expected Throwable subclass type
     * @param message the expected error message returned by {@link Throwable#getMessage()}
     * @param values the expected values
     * @return this
     */
    AssertableSubscriber<T> assertFailureAndMessage(Class<? extends Throwable> errorClass, String message, T... values);
}