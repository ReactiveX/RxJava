/**
 * Copyright (c) 2016-present, RxJava Contributors.
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

package io.reactivex.internal.operators.observable;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

import java.util.*;
import java.util.concurrent.*;

import org.junit.Test;
import org.mockito.Mockito;

import io.reactivex.*;
import io.reactivex.Observable;
import io.reactivex.Observer;

public class ObservableToSortedListTest {

    @Test
    public void testSortedListObservable() {
        Observable<Integer> w = Observable.just(1, 3, 2, 5, 4);
        Observable<List<Integer>> observable = w.toSortedList().toObservable();

        Observer<List<Integer>> observer = TestHelper.mockObserver();
        observable.subscribe(observer);
        verify(observer, times(1)).onNext(Arrays.asList(1, 2, 3, 4, 5));
        verify(observer, Mockito.never()).onError(any(Throwable.class));
        verify(observer, times(1)).onComplete();
    }

    @Test
    public void testSortedListWithCustomFunctionFlowable() {
        Observable<Integer> w = Observable.just(1, 3, 2, 5, 4);
        Observable<List<Integer>> observable = w.toSortedList(new Comparator<Integer>() {

            @Override
            public int compare(Integer t1, Integer t2) {
                return t2 - t1;
            }

        }).toObservable();

        Observer<List<Integer>> observer = TestHelper.mockObserver();
        observable.subscribe(observer);
        verify(observer, times(1)).onNext(Arrays.asList(5, 4, 3, 2, 1));
        verify(observer, Mockito.never()).onError(any(Throwable.class));
        verify(observer, times(1)).onComplete();
    }

    @Test
    public void testWithFollowingFirstObservable() {
        Observable<Integer> o = Observable.just(1, 3, 2, 5, 4);
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), o.toSortedList().toObservable().blockingFirst());
    }

    static void await(CyclicBarrier cb) {
        try {
            cb.await();
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        } catch (BrokenBarrierException ex) {
            ex.printStackTrace();
        }
    }

    @Test
    public void sorted() {
        Observable.just(5, 1, 2, 4, 3).sorted()
        .test()
        .assertResult(1, 2, 3, 4, 5);
    }

    @Test
    public void sortedComparator() {
        Observable.just(5, 1, 2, 4, 3).sorted(new Comparator<Integer>() {
            @Override
            public int compare(Integer a, Integer b) {
                return b - a;
            }
        })
        .test()
        .assertResult(5, 4, 3, 2, 1);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void toSortedListCapacityObservable() {
        Observable.just(5, 1, 2, 4, 3).toSortedList(4).toObservable()
        .test()
        .assertResult(Arrays.asList(1, 2, 3, 4, 5));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void toSortedListComparatorCapacityObservable() {
        Observable.just(5, 1, 2, 4, 3).toSortedList(new Comparator<Integer>() {
            @Override
            public int compare(Integer a, Integer b) {
                return b - a;
            }
        }, 4).toObservable()
        .test()
        .assertResult(Arrays.asList(5, 4, 3, 2, 1));
    }


    @Test
    public void testSortedList() {
        Observable<Integer> w = Observable.just(1, 3, 2, 5, 4);
        Single<List<Integer>> observable = w.toSortedList();

        SingleObserver<List<Integer>> observer = TestHelper.mockSingleObserver();
        observable.subscribe(observer);
        verify(observer, times(1)).onSuccess(Arrays.asList(1, 2, 3, 4, 5));
        verify(observer, Mockito.never()).onError(any(Throwable.class));
    }

    @Test
    public void testSortedListWithCustomFunction() {
        Observable<Integer> w = Observable.just(1, 3, 2, 5, 4);
        Single<List<Integer>> observable = w.toSortedList(new Comparator<Integer>() {

            @Override
            public int compare(Integer t1, Integer t2) {
                return t2 - t1;
            }

        });

        SingleObserver<List<Integer>> observer = TestHelper.mockSingleObserver();
        observable.subscribe(observer);
        verify(observer, times(1)).onSuccess(Arrays.asList(5, 4, 3, 2, 1));
        verify(observer, Mockito.never()).onError(any(Throwable.class));
    }

    @Test
    public void testWithFollowingFirst() {
        Observable<Integer> o = Observable.just(1, 3, 2, 5, 4);
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), o.toSortedList().blockingGet());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void toSortedListCapacity() {
        Observable.just(5, 1, 2, 4, 3).toSortedList(4)
        .test()
        .assertResult(Arrays.asList(1, 2, 3, 4, 5));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void toSortedListComparatorCapacity() {
        Observable.just(5, 1, 2, 4, 3).toSortedList(new Comparator<Integer>() {
            @Override
            public int compare(Integer a, Integer b) {
                return b - a;
            }
        }, 4)
        .test()
        .assertResult(Arrays.asList(5, 4, 3, 2, 1));
    }

}
