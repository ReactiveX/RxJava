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

import rx.Observer;
import rx.Producer;
import rx.functions.Action0;

public interface AssertableSubscriber<T> extends Observer<T> {

    void onStart();

    void setProducer(Producer p);

    int getCompletions();

    List<Throwable> getOnErrorEvents();

    int getValueCount();

    AssertableSubscriber<T> requestMore(long n);

    List<T> getOnNextEvents();

    AssertableSubscriber<T> assertReceivedOnNext(List<T> items);

    boolean awaitValueCount(int expected, long timeout, TimeUnit unit);

    AssertableSubscriber<T> assertTerminalEvent();

    AssertableSubscriber<T> assertUnsubscribed();

    AssertableSubscriber<T> assertNoErrors();

    AssertableSubscriber<T> awaitTerminalEvent();

    AssertableSubscriber<T> awaitTerminalEvent(long timeout, TimeUnit unit);

    AssertableSubscriber<T> awaitTerminalEventAndUnsubscribeOnTimeout(long timeout,
            TimeUnit unit);

    Thread getLastSeenThread();

    AssertableSubscriber<T> assertCompleted();

    AssertableSubscriber<T> assertNotCompleted();

    AssertableSubscriber<T> assertError(Class<? extends Throwable> clazz);

    AssertableSubscriber<T> assertError(Throwable throwable);

    AssertableSubscriber<T> assertNoTerminalEvent();

    AssertableSubscriber<T> assertNoValues();

    AssertableSubscriber<T> assertValueCount(int count);

    AssertableSubscriber<T> assertValues(T... values);

    AssertableSubscriber<T> assertValue(T value);

    AssertableSubscriber<T> assertValuesAndClear(T expectedFirstValue,
            T... expectedRestValues);

    AssertableSubscriber<T> perform(Action0 action);
    
    void unsubscribe();
    
    boolean isUnsubscribed();

}