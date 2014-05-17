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

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import org.junit.Test;

import rx.Observable;
import rx.Observer;
import rx.exceptions.TestException;
import rx.functions.Func0;

@SuppressWarnings("unchecked")
public class OperatorDeferTest {

    @Test
    public void testDefer() throws Throwable {

        Func0<Observable<String>> factory = mock(Func0.class);

        Observable<String> firstObservable = Observable.from("one", "two");
        Observable<String> secondObservable = Observable.from("three", "four");
        when(factory.call()).thenReturn(firstObservable, secondObservable);

        Observable<String> deferred = Observable.defer(factory);

        verifyZeroInteractions(factory);

        Observer<String> firstObserver = mock(Observer.class);
        deferred.subscribe(firstObserver);

        verify(factory, times(1)).call();
        verify(firstObserver, times(1)).onNext("one");
        verify(firstObserver, times(1)).onNext("two");
        verify(firstObserver, times(0)).onNext("three");
        verify(firstObserver, times(0)).onNext("four");
        verify(firstObserver, times(1)).onCompleted();

        Observer<String> secondObserver = mock(Observer.class);
        deferred.subscribe(secondObserver);

        verify(factory, times(2)).call();
        verify(secondObserver, times(0)).onNext("one");
        verify(secondObserver, times(0)).onNext("two");
        verify(secondObserver, times(1)).onNext("three");
        verify(secondObserver, times(1)).onNext("four");
        verify(secondObserver, times(1)).onCompleted();

    }
    
    @Test
    public void testDeferFunctionThrows() {
        Func0<Observable<String>> factory = mock(Func0.class);
        
        when(factory.call()).thenThrow(new TestException());
        
        Observable<String> result = Observable.defer(factory);
        
        Observer<String> o = mock(Observer.class);
        
        result.subscribe(o);
        
        verify(o).onError(any(TestException.class));
        verify(o, never()).onNext(any(String.class));
        verify(o, never()).onCompleted();
    }
}
