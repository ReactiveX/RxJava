/**
 * Copyright 2015 Netflix, Inc.
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

package io.reactivex.internal.operators;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

import java.util.function.Supplier;

import org.junit.Test;
import org.reactivestreams.Subscriber;

import io.reactivex.*;
import io.reactivex.exceptions.TestException;

@SuppressWarnings("unchecked")
public class OnSubscribeDeferTest {

    @Test
    public void testDefer() throws Throwable {

        Supplier<Observable<String>> factory = mock(Supplier.class);

        Observable<String> firstObservable = Observable.just("one", "two");
        Observable<String> secondObservable = Observable.just("three", "four");
        when(factory.get()).thenReturn(firstObservable, secondObservable);

        Observable<String> deferred = Observable.defer(factory);

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
        Supplier<Observable<String>> factory = mock(Supplier.class);
        
        when(factory.get()).thenThrow(new TestException());
        
        Observable<String> result = Observable.defer(factory);
        
        Observer<String> o = mock(Observer.class);
        
        result.subscribe(o);
        
        verify(o).onError(any(TestException.class));
        verify(o, never()).onNext(any(String.class));
        verify(o, never()).onComplete();
    }
}