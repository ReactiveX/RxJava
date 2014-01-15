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
import static rx.operators.OperationToObservableIterable.*;

import java.util.Arrays;

import org.junit.Test;
import org.mockito.Mockito;

import rx.IObservable;
import rx.Observer;
import rx.schedulers.Schedulers;

public class OperationToObservableIterableTest {

    @Test
    public void testIterable() {
        IObservable<String> observable = toObservableIterable(Arrays.<String> asList("one", "two", "three"));

        @SuppressWarnings("unchecked")
        Observer<String> aObserver = mock(Observer.class);
        observable.subscribe(aObserver);
        verify(aObserver, times(1)).onNext("one");
        verify(aObserver, times(1)).onNext("two");
        verify(aObserver, times(1)).onNext("three");
        verify(aObserver, Mockito.never()).onError(any(Throwable.class));
        verify(aObserver, times(1)).onCompleted();
    }
    
    @Test
    public void testIterableScheduled() {
        IObservable<String> observable = toObservableIterable(Arrays.<String> asList("one", "two", "three"), Schedulers.currentThread());

        @SuppressWarnings("unchecked")
        Observer<String> aObserver = mock(Observer.class);
        observable.subscribe(aObserver);
        verify(aObserver, times(1)).onNext("one");
        verify(aObserver, times(1)).onNext("two");
        verify(aObserver, times(1)).onNext("three");
        verify(aObserver, Mockito.never()).onError(any(Throwable.class));
        verify(aObserver, times(1)).onCompleted();
    }
}
