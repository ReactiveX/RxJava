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

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

import java.util.*;
import java.util.concurrent.*;

import org.junit.*;
import org.mockito.Mockito;
import org.reactivestreams.Subscriber;

import io.reactivex.*;
import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subscribers.TestSubscriber;

public class OperatorToObservableSortedListTest {

    @Test
    public void testSortedList() {
        Observable<Integer> w = Observable.just(1, 3, 2, 5, 4);
        Observable<List<Integer>> observable = w.toSortedList();

        Subscriber<List<Integer>> observer = TestHelper.mockSubscriber();
        observable.subscribe(observer);
        verify(observer, times(1)).onNext(Arrays.asList(1, 2, 3, 4, 5));
        verify(observer, Mockito.never()).onError(any(Throwable.class));
        verify(observer, times(1)).onComplete();
    }

    @Test
    public void testSortedListWithCustomFunction() {
        Observable<Integer> w = Observable.just(1, 3, 2, 5, 4);
        Observable<List<Integer>> observable = w.toSortedList(new Comparator<Integer>() {

            @Override
            public int compare(Integer t1, Integer t2) {
                return t2 - t1;
            }

        });

        Subscriber<List<Integer>> observer = TestHelper.mockSubscriber();
        observable.subscribe(observer);
        verify(observer, times(1)).onNext(Arrays.asList(5, 4, 3, 2, 1));
        verify(observer, Mockito.never()).onError(any(Throwable.class));
        verify(observer, times(1)).onComplete();
    }

    @Test
    public void testWithFollowingFirst() {
        Observable<Integer> o = Observable.just(1, 3, 2, 5, 4);
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), o.toSortedList().toBlocking().first());
    }
    @Test
    public void testBackpressureHonored() {
        Observable<List<Integer>> w = Observable.just(1, 3, 2, 5, 4).toSortedList();
        TestSubscriber<List<Integer>> ts = new TestSubscriber<>((Long)null);
        
        w.subscribe(ts);
        
        ts.assertNoValues();
        ts.assertNoErrors();
        ts.assertNotComplete();
        
        ts.request(1);
        
        ts.assertValue(Arrays.asList(1, 2, 3, 4, 5));
        ts.assertNoErrors();
        ts.assertComplete();

        ts.request(1);

        ts.assertValue(Arrays.asList(1, 2, 3, 4, 5));
        ts.assertNoErrors();
        ts.assertComplete();
    }
    @Test(timeout = 2000)
    @Ignore("PublishSubject no longer emits without requests so this test fails due to the race of onComplete and request")
    public void testAsyncRequested() {
        Scheduler.Worker w = Schedulers.newThread().createWorker();
        try {
            for (int i = 0; i < 1000; i++) {
                if (i % 50 == 0) {
                    System.out.println("testAsyncRequested -> " + i);
                }
                PublishSubject<Integer> source = PublishSubject.create();
                Observable<List<Integer>> sorted = source.toSortedList();

                final CyclicBarrier cb = new CyclicBarrier(2);
                final TestSubscriber<List<Integer>> ts = new TestSubscriber<>((Long)null);
                sorted.subscribe(ts);
                w.schedule(new Runnable() {
                    @Override
                    public void run() {
                        await(cb);
                        ts.request(1);
                    }
                });
                source.onNext(1);
                await(cb);
                source.onComplete();
                ts.awaitTerminalEvent(1, TimeUnit.SECONDS);
                ts.assertTerminated();
                ts.assertNoErrors();
                ts.assertValue(Arrays.asList(1));
            }
        } finally {
            w.dispose();
        }
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