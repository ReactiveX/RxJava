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
package rx.internal.operators;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

import java.util.*;
import java.util.concurrent.*;

import org.junit.Test;
import org.mockito.Mockito;

import rx.*;
import rx.Observable;
import rx.Observer;
import rx.functions.*;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;

public class OperatorToObservableSortedListTest {

    @Test
    public void testSortedList() {
        Observable<Integer> w = Observable.just(1, 3, 2, 5, 4);
        Observable<List<Integer>> observable = w.toSortedList();

        @SuppressWarnings("unchecked")
        Observer<List<Integer>> observer = mock(Observer.class);
        observable.subscribe(observer);
        verify(observer, times(1)).onNext(Arrays.asList(1, 2, 3, 4, 5));
        verify(observer, Mockito.never()).onError(any(Throwable.class));
        verify(observer, times(1)).onCompleted();
    }

    @Test
    public void testSortedListWithCustomFunction() {
        Observable<Integer> w = Observable.just(1, 3, 2, 5, 4);
        Observable<List<Integer>> observable = w.toSortedList(new Func2<Integer, Integer, Integer>() {

            @Override
            public Integer call(Integer t1, Integer t2) {
                return t2 - t1;
            }

        });

        @SuppressWarnings("unchecked")
        Observer<List<Integer>> observer = mock(Observer.class);
        observable.subscribe(observer);
        verify(observer, times(1)).onNext(Arrays.asList(5, 4, 3, 2, 1));
        verify(observer, Mockito.never()).onError(any(Throwable.class));
        verify(observer, times(1)).onCompleted();
    }

    @Test
    public void testWithFollowingFirst() {
        Observable<Integer> o = Observable.just(1, 3, 2, 5, 4);
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), o.toSortedList().toBlocking().first());
    }
    @Test
    public void testBackpressureHonored() {
        Observable<List<Integer>> w = Observable.just(1, 3, 2, 5, 4).toSortedList();
        TestSubscriber<List<Integer>> ts = new TestSubscriber<List<Integer>>() {
            @Override
            public void onStart() {
                requestMore(0);
            }
        };
        
        w.subscribe(ts);
        
        assertTrue(ts.getOnNextEvents().isEmpty());
        assertTrue(ts.getOnErrorEvents().isEmpty());
        assertTrue(ts.getOnCompletedEvents().isEmpty());
        
        ts.requestMore(1);
        
        ts.assertReceivedOnNext(Collections.singletonList(Arrays.asList(1, 2, 3, 4, 5)));
        assertTrue(ts.getOnErrorEvents().isEmpty());
        assertEquals(1, ts.getOnCompletedEvents().size());

        ts.requestMore(1);

        ts.assertReceivedOnNext(Collections.singletonList(Arrays.asList(1, 2, 3, 4, 5)));
        assertTrue(ts.getOnErrorEvents().isEmpty());
        assertEquals(1, ts.getOnCompletedEvents().size());
    }
    @Test(timeout = 2000)
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
                final TestSubscriber<List<Integer>> ts = new TestSubscriber<List<Integer>>() {
                    @Override
                    public void onStart() {
                        requestMore(0);
                    }
                };
                sorted.subscribe(ts);
                w.schedule(new Action0() {
                    @Override
                    public void call() {
                        await(cb);
                        ts.requestMore(1);
                    }
                });
                source.onNext(1);
                await(cb);
                source.onCompleted();
                ts.awaitTerminalEvent(1, TimeUnit.SECONDS);
                ts.assertTerminalEvent();
                ts.assertNoErrors();
                ts.assertReceivedOnNext(Collections.singletonList(Arrays.asList(1)));
            }
        } finally {
            w.unsubscribe();
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
