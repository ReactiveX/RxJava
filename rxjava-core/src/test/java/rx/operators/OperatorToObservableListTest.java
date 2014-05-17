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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.mockito.Mockito;

import rx.Observable;
import rx.Observer;

public class OperatorToObservableListTest {

    @Test
    public void testList() {
        Observable<String> w = Observable.from(Arrays.asList("one", "two", "three"));
        Observable<List<String>> observable = w.lift(new OperatorToObservableList<String>());

        @SuppressWarnings("unchecked")
        Observer<List<String>> observer = mock(Observer.class);
        observable.subscribe(observer);
        verify(observer, times(1)).onNext(Arrays.asList("one", "two", "three"));
        verify(observer, Mockito.never()).onError(any(Throwable.class));
        verify(observer, times(1)).onCompleted();
    }
    
    @Test
    public void testListViaObservable() {
        Observable<String> w = Observable.from(Arrays.asList("one", "two", "three"));
        Observable<List<String>> observable = w.toList();

        @SuppressWarnings("unchecked")
        Observer<List<String>> observer = mock(Observer.class);
        observable.subscribe(observer);
        verify(observer, times(1)).onNext(Arrays.asList("one", "two", "three"));
        verify(observer, Mockito.never()).onError(any(Throwable.class));
        verify(observer, times(1)).onCompleted();
    }

    @Test
    public void testListMultipleObservers() {
        Observable<String> w = Observable.from(Arrays.asList("one", "two", "three"));
        Observable<List<String>> observable = w.lift(new OperatorToObservableList<String>());

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
        Observable<String> w = Observable.from(Arrays.asList("one", null, "three"));
        Observable<List<String>> observable = w.lift(new OperatorToObservableList<String>());

        @SuppressWarnings("unchecked")
        Observer<List<String>> observer = mock(Observer.class);
        observable.subscribe(observer);
        verify(observer, times(1)).onNext(Arrays.asList("one", null, "three"));
        verify(observer, Mockito.never()).onError(any(Throwable.class));
        verify(observer, times(1)).onCompleted();
    }
}
