/**
 * Copyright 2013 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package rx.operators;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;
import static rx.operators.OperationRetry.*;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;
import org.mockito.InOrder;

import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.subscriptions.Subscriptions;

public class OperationRetryTest {

    @Test
    public void testOriginFails() {
        @SuppressWarnings("unchecked")
        Observer<String> observer = mock(Observer.class);
        Observable<String> origin = Observable.create(new FuncWithErrors(2));
        origin.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onNext("beginningEveryTime");
        inOrder.verify(observer, times(1)).onError(any(RuntimeException.class));
        inOrder.verify(observer, never()).onNext("onSuccessOnly");
        inOrder.verify(observer, never()).onCompleted();
    }

    @Test
    public void testRetryFail() {
        int NUM_RETRIES = 1;
        int NUM_FAILURES = 2;
        @SuppressWarnings("unchecked")
        Observer<String> observer = mock(Observer.class);
        Observable<String> origin = Observable.create(new FuncWithErrors(NUM_FAILURES));
        Observable.create(retry(origin, NUM_RETRIES)).subscribe(observer);

        InOrder inOrder = inOrder(observer);
        // should show 2 attempts (first time fail, second time (1st retry) fail)
        inOrder.verify(observer, times(1 + NUM_RETRIES)).onNext("beginningEveryTime");
        // should only retry once, fail again and emit onError
        inOrder.verify(observer, times(1)).onError(any(RuntimeException.class));
        // no success
        inOrder.verify(observer, never()).onNext("onSuccessOnly");
        inOrder.verify(observer, never()).onCompleted();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testRetrySuccess() {
        int NUM_RETRIES = 3;
        int NUM_FAILURES = 2;
        @SuppressWarnings("unchecked")
        Observer<String> observer = mock(Observer.class);
        Observable<String> origin = Observable.create(new FuncWithErrors(NUM_FAILURES));
        Observable.create(retry(origin, NUM_RETRIES)).subscribe(observer);

        InOrder inOrder = inOrder(observer);
        // should show 3 attempts
        inOrder.verify(observer, times(1 + NUM_FAILURES)).onNext("beginningEveryTime");
        // should have no errors
        inOrder.verify(observer, never()).onError(any(Throwable.class));
        // should have a single success
        inOrder.verify(observer, times(1)).onNext("onSuccessOnly");
        // should have a single successful onCompleted
        inOrder.verify(observer, times(1)).onCompleted();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testInfiniteRetry() {
        int NUM_FAILURES = 20;
        @SuppressWarnings("unchecked")
        Observer<String> observer = mock(Observer.class);
        Observable<String> origin = Observable.create(new FuncWithErrors(NUM_FAILURES));
        Observable.create(retry(origin)).subscribe(observer);

        InOrder inOrder = inOrder(observer);
        // should show 3 attempts
        inOrder.verify(observer, times(1 + NUM_FAILURES)).onNext("beginningEveryTime");
        // should have no errors
        inOrder.verify(observer, never()).onError(any(Throwable.class));
        // should have a single success
        inOrder.verify(observer, times(1)).onNext("onSuccessOnly");
        // should have a single successful onCompleted
        inOrder.verify(observer, times(1)).onCompleted();
        inOrder.verifyNoMoreInteractions();
    }

    public static class FuncWithErrors implements Observable.OnSubscribeFunc<String> {

        private final int numFailures;
        private final AtomicInteger count = new AtomicInteger(0);

        FuncWithErrors(int count) {
            this.numFailures = count;
        }

        @Override
        public Subscription onSubscribe(Observer<? super String> o) {
            o.onNext("beginningEveryTime");
            if (count.incrementAndGet() <= numFailures) {
                o.onError(new RuntimeException("forced failure: " + count.get()));
            } else {
                o.onNext("onSuccessOnly");
                o.onCompleted();
            }
            return Subscriptions.empty();
        }
    }
}
