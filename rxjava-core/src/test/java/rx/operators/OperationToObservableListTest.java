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
import static rx.operators.OperationToObservableList.*;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.mockito.Mockito;

import rx.IObservable;
import rx.Observable;
import rx.Observer;

public class OperationToObservableListTest {

    @Test
    public void testList() {
        Observable<String> w = Observable.from("one", "two", "three");
        IObservable<List<String>> observable = toObservableList(w);

        @SuppressWarnings("unchecked")
        Observer<List<String>> aObserver = mock(Observer.class);
        observable.subscribe(aObserver);
        verify(aObserver, times(1)).onNext(Arrays.asList("one", "two", "three"));
        verify(aObserver, Mockito.never()).onError(any(Throwable.class));
        verify(aObserver, times(1)).onCompleted();
    }

    @Test
    public void testListMultipleObservers() {
        Observable<String> w = Observable.from("one", "two", "three");
        IObservable<List<String>> observable = toObservableList(w);

        @SuppressWarnings("unchecked")
        Observer<List<String>> o1 = mock(Observer.class);
        observable.subscribe(o1);

        @SuppressWarnings("unchecked")
        Observer<List<String>> o2 = mock(Observer.class);
        observable.subscribe(o2);

        List<String> expected = Arrays.asList("one", "two", "three");

        verify(o1, times(1)).onNext(expected);
        verify(o1, Mockito.never()).onError(any(Throwable.class));
        verify(o1, times(1)).onCompleted();

        verify(o2, times(1)).onNext(expected);
        verify(o2, Mockito.never()).onError(any(Throwable.class));
        verify(o2, times(1)).onCompleted();
    }

    @Test
    public void testListWithNullValue() {
        Observable<String> w = Observable.from("one", null, "three");
        IObservable<List<String>> observable = toObservableList(w);

        @SuppressWarnings("unchecked")
        Observer<List<String>> aObserver = mock(Observer.class);
        observable.subscribe(aObserver);
        verify(aObserver, times(1)).onNext(Arrays.asList("one", null, "three"));
        verify(aObserver, Mockito.never()).onError(any(Throwable.class));
        verify(aObserver, times(1)).onCompleted();
    }
}
