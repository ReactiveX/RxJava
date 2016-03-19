/**
 * Copyright 2016 Netflix, Inc.
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

package io.reactivex.internal.operators.flowable;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

import org.junit.Test;
import org.reactivestreams.Subscriber;

import io.reactivex.Flowable;
import io.reactivex.exceptions.TestException;
import io.reactivex.flowable.TestHelper;
import io.reactivex.functions.Supplier;
import io.reactivex.subscribers.DefaultObserver;

@SuppressWarnings("unchecked")
public class OnSubscribeDeferTest {

    @Test
    public void testDefer() throws Throwable {

        Supplier<Flowable<String>> factory = mock(Supplier.class);

        Flowable<String> firstObservable = Flowable.just("one", "two");
        Flowable<String> secondObservable = Flowable.just("three", "four");
        when(factory.get()).thenReturn(firstObservable, secondObservable);

        Flowable<String> deferred = Flowable.defer(factory);

        verifyZeroInteractions(factory);

        Subscriber<String> firstObserver = TestHelper.mockSubscriber();
        deferred.subscribe(firstObserver);

        verify(factory, times(1)).get();
        verify(firstObserver, times(1)).onNext("one");
        verify(firstObserver, times(1)).onNext("two");
        verify(firstObserver, times(0)).onNext("three");
        verify(firstObserver, times(0)).onNext("four");
        verify(firstObserver, times(1)).onComplete();

        Subscriber<String> secondObserver = TestHelper.mockSubscriber();
        deferred.subscribe(secondObserver);

        verify(factory, times(2)).get();
        verify(secondObserver, times(0)).onNext("one");
        verify(secondObserver, times(0)).onNext("two");
        verify(secondObserver, times(1)).onNext("three");
        verify(secondObserver, times(1)).onNext("four");
        verify(secondObserver, times(1)).onComplete();

    }
    
    @Test
    public void testDeferFunctionThrows() {
        Supplier<Flowable<String>> factory = mock(Supplier.class);
        
        when(factory.get()).thenThrow(new TestException());
        
        Flowable<String> result = Flowable.defer(factory);
        
        DefaultObserver<String> o = mock(DefaultObserver.class);
        
        result.subscribe(o);
        
        verify(o).onError(any(TestException.class));
        verify(o, never()).onNext(any(String.class));
        verify(o, never()).onComplete();
    }
}