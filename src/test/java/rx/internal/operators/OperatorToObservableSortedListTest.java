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

        TestSubscriber<List<Integer>> testSubscriber = new TestSubscriber<List<Integer>>();
        observable.subscribe(testSubscriber);
        testSubscriber.assertValue(Arrays.asList(1,2,3,4,5));
        testSubscriber.assertNoErrors();
        testSubscriber.assertCompleted();
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
        assertEquals(0, ts.getCompletions());

        ts.requestMore(1);

        ts.assertReceivedOnNext(Collections.singletonList(Arrays.asList(1, 2, 3, 4, 5)));
        assertTrue(ts.getOnErrorEvents().isEmpty());
        assertEquals(1, ts.getCompletions());

        ts.requestMore(1);

        ts.assertReceivedOnNext(Collections.singletonList(Arrays.asList(1, 2, 3, 4, 5)));
        assertTrue(ts.getOnErrorEvents().isEmpty());
        assertEquals(1, ts.getCompletions());
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

    @Test
    public void testSortedListCapacity() {
        Observable<Integer> w = Observable.just(1, 3, 2, 5, 4);
        Observable<List<Integer>> observable = w.toSortedList(4);

        TestSubscriber<List<Integer>> testSubscriber = new TestSubscriber<List<Integer>>();
        observable.subscribe(testSubscriber);
        testSubscriber.assertValue(Arrays.asList(1,2,3,4,5));
        testSubscriber.assertNoErrors();
        testSubscriber.assertCompleted();
    }

    @Test
    public void testSortedCustomComparer() {
        Observable<Integer> w = Observable.just(1, 3, 2, 5, 4);
        Observable<List<Integer>> observable = w.toSortedList(new Func2<Integer, Integer, Integer>() {
            @Override
            public Integer call(Integer t1, Integer t2) {
                return t2.compareTo(t1);
            }
        });

        TestSubscriber<List<Integer>> testSubscriber = new TestSubscriber<List<Integer>>();
        observable.subscribe(testSubscriber);
        testSubscriber.assertValue(Arrays.asList(5, 4, 3, 2, 1));
        testSubscriber.assertNoErrors();
        testSubscriber.assertCompleted();
    }

    @Test
    public void testSortedCustomComparerHinted() {
        Observable<Integer> w = Observable.just(1, 3, 2, 5, 4);
        Observable<List<Integer>> observable = w.toSortedList(new Func2<Integer, Integer, Integer>() {
            @Override
            public Integer call(Integer t1, Integer t2) {
                return t2.compareTo(t1);
            }
        }, 4);

        TestSubscriber<List<Integer>> testSubscriber = new TestSubscriber<List<Integer>>();
        observable.subscribe(testSubscriber);
        testSubscriber.assertValue(Arrays.asList(5, 4, 3, 2, 1));
        testSubscriber.assertNoErrors();
        testSubscriber.assertCompleted();
    }

    @Test
    public void testSorted() {
        Observable<Integer> w = Observable.just(1, 3, 2, 5, 4);
        Observable<Integer> observable = w.sorted();

        TestSubscriber<Integer> testSubscriber = new TestSubscriber<Integer>();
        observable.subscribe(testSubscriber);
        testSubscriber.assertValues(1,2,3,4,5);
        testSubscriber.assertNoErrors();
        testSubscriber.assertCompleted();
    }

    @Test
    public void testSortedWithCustomFunction() {
        Observable<Integer> w = Observable.just(1, 3, 2, 5, 4);
        Observable<Integer> observable = w.sorted(new Func2<Integer, Integer, Integer>() {

            @Override
            public Integer call(Integer t1, Integer t2) {
                return t2 - t1;
            }

        });

        TestSubscriber<Integer> testSubscriber = new TestSubscriber<Integer>();
        observable.subscribe(testSubscriber);
        testSubscriber.assertValues(5,4,3,2,1);
        testSubscriber.assertNoErrors();
        testSubscriber.assertCompleted();
    }

    @Test
    public void testSortedCustomComparator() {
        Observable<Integer> w = Observable.just(1, 3, 2, 5, 4);
        Observable<Integer> observable = w.sorted(new Func2<Integer, Integer, Integer>() {
            @Override
            public Integer call(Integer t1, Integer t2) {
                return t1.compareTo(t2);
            }

        });

        TestSubscriber<Integer> testSubscriber = new TestSubscriber<Integer>();
        observable.subscribe(testSubscriber);
        testSubscriber.assertValues(1,2,3,4,5);
        testSubscriber.assertNoErrors();
        testSubscriber.assertCompleted();
    }

    @Test
    public void testSortedWithNonComparable() {
        NonComparable n1 = new NonComparable(1,"a");
        NonComparable n2 = new NonComparable(2,"b");
        NonComparable n3 = new NonComparable(3,"c");
        Observable<NonComparable> w = Observable.just(n1,n2,n3);

        Observable<NonComparable> observable = w.sorted();

        TestSubscriber<NonComparable> testSubscriber = new TestSubscriber<NonComparable>();
        observable.subscribe(testSubscriber);
        testSubscriber.assertNoValues();
        testSubscriber.assertError(ClassCastException.class);
        testSubscriber.assertNotCompleted();
    }

    final static class NonComparable {
        public int i;
        public String s;

        NonComparable(int i, String s) {
            this.i = i;
            this.s = s;
        }
    }
}
