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

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

import java.util.*;
import java.util.concurrent.*;

import org.junit.Test;
import org.mockito.Mockito;

import io.reactivex.*;
import io.reactivex.NbpObservable.NbpSubscriber;

public class NbpOperatorToObservableSortedListTest {

    @Test
    public void testSortedList() {
        NbpObservable<Integer> w = NbpObservable.just(1, 3, 2, 5, 4);
        NbpObservable<List<Integer>> NbpObservable = w.toSortedList();

        NbpSubscriber<List<Integer>> NbpObserver = TestHelper.mockNbpSubscriber();
        NbpObservable.subscribe(NbpObserver);
        verify(NbpObserver, times(1)).onNext(Arrays.asList(1, 2, 3, 4, 5));
        verify(NbpObserver, Mockito.never()).onError(any(Throwable.class));
        verify(NbpObserver, times(1)).onComplete();
    }

    @Test
    public void testSortedListWithCustomFunction() {
        NbpObservable<Integer> w = NbpObservable.just(1, 3, 2, 5, 4);
        NbpObservable<List<Integer>> NbpObservable = w.toSortedList(new Comparator<Integer>() {

            @Override
            public int compare(Integer t1, Integer t2) {
                return t2 - t1;
            }

        });

        NbpSubscriber<List<Integer>> NbpObserver = TestHelper.mockNbpSubscriber();
        NbpObservable.subscribe(NbpObserver);
        verify(NbpObserver, times(1)).onNext(Arrays.asList(5, 4, 3, 2, 1));
        verify(NbpObserver, Mockito.never()).onError(any(Throwable.class));
        verify(NbpObserver, times(1)).onComplete();
    }

    @Test
    public void testWithFollowingFirst() {
        NbpObservable<Integer> o = NbpObservable.just(1, 3, 2, 5, 4);
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), o.toSortedList().toBlocking().first());
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
}