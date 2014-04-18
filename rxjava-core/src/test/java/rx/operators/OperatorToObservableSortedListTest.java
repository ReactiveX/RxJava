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
import rx.functions.Func2;

public class OperatorToObservableSortedListTest {

    @Test
    public void testSortedList() {
        Observable<Integer> w = Observable.from(1, 3, 2, 5, 4);
        Observable<List<Integer>> observable = w.lift(new OperatorToObservableSortedList<Integer>());

        @SuppressWarnings("unchecked")
        Observer<List<Integer>> observer = mock(Observer.class);
        observable.subscribe(observer);
        verify(observer, times(1)).onNext(Arrays.asList(1, 2, 3, 4, 5));
        verify(observer, Mockito.never()).onError(any(Throwable.class));
        verify(observer, times(1)).onCompleted();
    }

    @Test
    public void testSortedListWithCustomFunction() {
        Observable<Integer> w = Observable.from(1, 3, 2, 5, 4);
        Observable<List<Integer>> observable = w.lift(new OperatorToObservableSortedList<Integer>(new Func2<Integer, Integer, Integer>() {

            @Override
            public Integer call(Integer t1, Integer t2) {
                return t2 - t1;
            }

        }));

        @SuppressWarnings("unchecked")
        Observer<List<Integer>> observer = mock(Observer.class);
        observable.subscribe(observer);
        verify(observer, times(1)).onNext(Arrays.asList(5, 4, 3, 2, 1));
        verify(observer, Mockito.never()).onError(any(Throwable.class));
        verify(observer, times(1)).onCompleted();
    }
}
