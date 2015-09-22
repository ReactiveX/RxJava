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

package io.reactivex.internal.operators.nbp;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

import java.util.function.Supplier;

import org.junit.Test;

import io.reactivex.*;
import io.reactivex.NbpObservable.NbpSubscriber;
import io.reactivex.exceptions.TestException;

@SuppressWarnings("unchecked")
public class NbpOnSubscribeDeferTest {

    @Test
    public void testDefer() throws Throwable {

        Supplier<NbpObservable<String>> factory = mock(Supplier.class);

        NbpObservable<String> firstObservable = NbpObservable.just("one", "two");
        NbpObservable<String> secondObservable = NbpObservable.just("three", "four");
        when(factory.get()).thenReturn(firstObservable, secondObservable);

        NbpObservable<String> deferred = NbpObservable.defer(factory);

        verifyZeroInteractions(factory);

        NbpSubscriber<String> firstObserver = TestHelper.mockNbpSubscriber();
        deferred.subscribe(firstObserver);

        verify(factory, times(1)).get();
        verify(firstObserver, times(1)).onNext("one");
        verify(firstObserver, times(1)).onNext("two");
        verify(firstObserver, times(0)).onNext("three");
        verify(firstObserver, times(0)).onNext("four");
        verify(firstObserver, times(1)).onComplete();

        NbpSubscriber<String> secondObserver = TestHelper.mockNbpSubscriber();
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
        Supplier<NbpObservable<String>> factory = mock(Supplier.class);
        
        when(factory.get()).thenThrow(new TestException());
        
        NbpObservable<String> result = NbpObservable.defer(factory);
        
        NbpObserver<String> o = mock(NbpObserver.class);
        
        result.subscribe(o);
        
        verify(o).onError(any(TestException.class));
        verify(o, never()).onNext(any(String.class));
        verify(o, never()).onComplete();
    }
}