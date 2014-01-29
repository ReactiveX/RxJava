/**
 * Copyright 2014 Netflix, Inc.
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
package rx.operators;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;
import static rx.operators.OperationSubscribeOn.*;

import org.junit.Test;

import rx.Observable;
import rx.Subscriber;
import rx.Scheduler;
import rx.Subscription;
import rx.observers.TestObserver;
import rx.schedulers.Schedulers;
import rx.test.OperatorTester;
import rx.util.functions.Action0;
import rx.util.functions.Func2;

public class OperationSubscribeOnTest {

    @Test
    @SuppressWarnings("unchecked")
    public void testSubscribeOn() {
        Observable<Integer> w = Observable.from(1, 2, 3);

        Scheduler scheduler = spy(OperatorTester.forwardingScheduler(Schedulers.immediate()));

        Subscriber<Integer> observer = mock(Subscriber.class);
        Subscription subscription = Observable.create(subscribeOn(w, scheduler)).subscribe(new TestObserver<Integer>(observer));

        verify(scheduler, times(1)).schedule(isNull(), any(Func2.class));
        subscription.unsubscribe();
        verify(scheduler, times(1)).schedule(any(Action0.class));
        verifyNoMoreInteractions(scheduler);

        verify(observer, times(1)).onNext(1);
        verify(observer, times(1)).onNext(2);
        verify(observer, times(1)).onNext(3);
        verify(observer, times(1)).onCompleted();
    }
}
