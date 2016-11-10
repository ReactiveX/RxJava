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
package rx.internal.observers;

import java.util.List;
import java.util.concurrent.TimeUnit;

import rx.Producer;
import rx.Subscriber;
import rx.annotations.Experimental;
import rx.functions.Action0;
import rx.observers.TestSubscriber;
import rx.observers.AssertableSubscriber;

/**
 * A {@code AssertableSubscriber} is a variety of {@link Subscriber} that you can use
 * for unit testing, to perform assertions or inspect received events.
 * AssertableSubscriber is a duplicate of TestSubscriber but supports method chaining
 * where possible.
 * 
 * @param <T>
 *            the value type
 */
@Experimental
public class AssertableSubscriberObservable<T> extends Subscriber<T> implements AssertableSubscriber<T> {

    private final TestSubscriber<T> ts;

    public AssertableSubscriberObservable(TestSubscriber<T> ts) {
        this.ts = ts;
    }

    public static <T> AssertableSubscriberObservable<T> create(long initialRequest) {
        TestSubscriber<T> t1 = new TestSubscriber<T>(initialRequest);
        AssertableSubscriberObservable<T> t2 = new AssertableSubscriberObservable<T>(t1);
        t2.add(t1);
        return t2;
    }

    /* (non-Javadoc)
     * @see rx.observers.AssertableSubscriber#onStart()
     */
    @Override
    public void onStart() {
        ts.onStart();
    }

    @Override
    public void onCompleted() {
        ts.onCompleted();
    }

    /* (non-Javadoc)
     * @see rx.observers.AssertableSubscriber#setProducer(rx.Producer)
     */
    @Override
    public void setProducer(Producer p) {
        ts.setProducer(p);
    }

    /* (non-Javadoc)
     * @see rx.observers.AssertableSubscriber#getCompletions()
     */
    @Override
    public final int getCompletions() {
        return ts.getCompletions();
    }

    @Override
    public void onError(Throwable e) {
        ts.onError(e);
    }

    /* (non-Javadoc)
     * @see rx.observers.AssertableSubscriber#getOnErrorEvents()
     */
    @Override
    public List<Throwable> getOnErrorEvents() {
        return ts.getOnErrorEvents();
    }

    @Override
    public void onNext(T t) {
        ts.onNext(t);
    }

    /* (non-Javadoc)
     * @see rx.observers.AssertableSubscriber#getValueCount()
     */
    @Override
    public final int getValueCount() {
        return ts.getValueCount();
    }

    /* (non-Javadoc)
     * @see rx.observers.AssertableSubscriber#requestMore(long)
     */
    @Override
    public AssertableSubscriber<T> requestMore(long n) {
        ts.requestMore(n);
        return this;
    }

    /* (non-Javadoc)
     * @see rx.observers.AssertableSubscriber#getOnNextEvents()
     */
    @Override
    public List<T> getOnNextEvents() {
        return ts.getOnNextEvents();
    }

    /* (non-Javadoc)
     * @see rx.observers.AssertableSubscriber#assertReceivedOnNext(java.util.List)
     */
    @Override
    public AssertableSubscriber<T> assertReceivedOnNext(List<T> items) {
        ts.assertReceivedOnNext(items);
        return this;
    }

    /* (non-Javadoc)
     * @see rx.observers.AssertableSubscriber#awaitValueCount(int, long, java.util.concurrent.TimeUnit)
     */
    @Override
    public final AssertableSubscriber<T> awaitValueCount(int expected, long timeout, TimeUnit unit) {
        if (!ts.awaitValueCount(expected, timeout, unit)) {
            throw new AssertionError("Did not receive enough values in time. Expected: " + expected + ", Actual: " + ts.getValueCount());
        }
        return this;
    }

    /* (non-Javadoc)
     * @see rx.observers.AssertableSubscriber#assertTerminalEvent()
     */
    @Override
    public AssertableSubscriber<T> assertTerminalEvent() {
        ts.assertTerminalEvent();
        return this;
    }

    /* (non-Javadoc)
     * @see rx.observers.AssertableSubscriber#assertUnsubscribed()
     */
    @Override
    public AssertableSubscriber<T> assertUnsubscribed() {
        ts.assertUnsubscribed();
        return this;
    }

    /* (non-Javadoc)
     * @see rx.observers.AssertableSubscriber#assertNoErrors()
     */
    @Override
    public AssertableSubscriber<T> assertNoErrors() {
        ts.assertNoErrors();
        return this;
    }

    /* (non-Javadoc)
     * @see rx.observers.AssertableSubscriber#awaitTerminalEvent()
     */
    @Override
    public AssertableSubscriber<T> awaitTerminalEvent() {
        ts.awaitTerminalEvent();
        return this;
    }

    /* (non-Javadoc)
     * @see rx.observers.AssertableSubscriber#awaitTerminalEvent(long, java.util.concurrent.TimeUnit)
     */
    @Override
    public AssertableSubscriber<T> awaitTerminalEvent(long timeout, TimeUnit unit) {
        ts.awaitTerminalEvent(timeout, unit);
        return this;
    }

    /* (non-Javadoc)
     * @see rx.observers.AssertableSubscriber#awaitTerminalEventAndUnsubscribeOnTimeout(long, java.util.concurrent.TimeUnit)
     */
    @Override
    public AssertableSubscriber<T> awaitTerminalEventAndUnsubscribeOnTimeout(long timeout,
            TimeUnit unit) {
        ts.awaitTerminalEventAndUnsubscribeOnTimeout(timeout, unit);
        return this;
    }

    /* (non-Javadoc)
     * @see rx.observers.AssertableSubscriber#getLastSeenThread()
     */
    @Override
    public Thread getLastSeenThread() {
        return ts.getLastSeenThread();
    }

    /* (non-Javadoc)
     * @see rx.observers.AssertableSubscriber#assertCompleted()
     */
    @Override
    public AssertableSubscriber<T> assertCompleted() {
        ts.assertCompleted();
        return this;
    }

    /* (non-Javadoc)
     * @see rx.observers.AssertableSubscriber#assertNotCompleted()
     */
    @Override
    public AssertableSubscriber<T> assertNotCompleted() {
        ts.assertNotCompleted();
        return this;
    }

    /* (non-Javadoc)
     * @see rx.observers.AssertableSubscriber#assertError(java.lang.Class)
     */
    @Override
    public AssertableSubscriber<T> assertError(Class<? extends Throwable> clazz) {
        ts.assertError(clazz);
        return this;
    }

    /* (non-Javadoc)
     * @see rx.observers.AssertableSubscriber#assertError(java.lang.Throwable)
     */
    @Override
    public AssertableSubscriber<T> assertError(Throwable throwable) {
        ts.assertError(throwable);
        return this;
    }

    /* (non-Javadoc)
     * @see rx.observers.AssertableSubscriber#assertNoTerminalEvent()
     */
    @Override
    public AssertableSubscriber<T> assertNoTerminalEvent() {
        ts.assertNoTerminalEvent();
        return this;
    }

    /* (non-Javadoc)
     * @see rx.observers.AssertableSubscriber#assertNoValues()
     */
    @Override
    public AssertableSubscriber<T> assertNoValues() {
        ts.assertNoValues();
        return this;
    }

    /* (non-Javadoc)
     * @see rx.observers.AssertableSubscriber#assertValueCount(int)
     */
    @Override
    public AssertableSubscriber<T> assertValueCount(int count) {
        ts.assertValueCount(count);
        return this;
    }

    /* (non-Javadoc)
     * @see rx.observers.AssertableSubscriber#assertValues(T)
     */
    @Override
    public AssertableSubscriber<T> assertValues(T... values) {
        ts.assertValues(values);
        return this;
    }

    /* (non-Javadoc)
     * @see rx.observers.AssertableSubscriber#assertValue(T)
     */
    @Override
    public AssertableSubscriber<T> assertValue(T value) {
        ts.assertValue(value);
        return this;
    }

    /* (non-Javadoc)
     * @see rx.observers.AssertableSubscriber#assertValuesAndClear(T, T)
     */
    @Override
    public final AssertableSubscriber<T> assertValuesAndClear(T expectedFirstValue,
            T... expectedRestValues) {
        ts.assertValuesAndClear(expectedFirstValue, expectedRestValues);
        return this;
    }

    /* (non-Javadoc)
     * @see rx.observers.AssertableSubscriber#perform(rx.functions.Action0)
     */
    @Override
    public final AssertableSubscriber<T> perform(Action0 action) {
        action.call();
        return this;
    }

    @Override
    public String toString() {
        return ts.toString();
    }

    @Override
    public final AssertableSubscriber<T> assertResult(T... values) {
        ts.assertValues(values);
        ts.assertNoErrors();
        ts.assertCompleted();
        return this;
    }

    @Override
    public final AssertableSubscriber<T> assertFailure(Class<? extends Throwable> errorClass, T... values) {
        ts.assertValues(values);
        ts.assertError(errorClass);
        ts.assertNotCompleted();
        return this;
    }

    @Override
    public final AssertableSubscriber<T> assertFailureAndMessage(Class<? extends Throwable> errorClass, String message,
            T... values) {
        ts.assertValues(values);
        ts.assertError(errorClass);
        ts.assertNotCompleted();

        String actualMessage = ts.getOnErrorEvents().get(0).getMessage();
        if (!(actualMessage == message || (message != null && message.equals(actualMessage)))) {
            throw new AssertionError("Error message differs. Expected: \'" + message + "\', Received: \'" + actualMessage + "\'");
        }

        return this;
    }
}
