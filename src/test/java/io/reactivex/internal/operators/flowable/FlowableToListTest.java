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

package io.reactivex.internal.operators.flowable;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.*;
import java.util.concurrent.*;

import org.junit.*;
import org.mockito.Mockito;
import org.reactivestreams.Subscriber;

import io.reactivex.*;
import io.reactivex.exceptions.TestException;
import io.reactivex.observers.TestObserver;
import io.reactivex.processors.PublishProcessor;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subscribers.TestSubscriber;

public class FlowableToListTest {

    @Test
    public void testListFlowable() {
        Flowable<String> w = Flowable.fromIterable(Arrays.asList("one", "two", "three"));
        Flowable<List<String>> observable = w.toList().toFlowable();

        Subscriber<List<String>> observer = TestHelper.mockSubscriber();
        observable.subscribe(observer);
        verify(observer, times(1)).onNext(Arrays.asList("one", "two", "three"));
        verify(observer, Mockito.never()).onError(any(Throwable.class));
        verify(observer, times(1)).onComplete();
    }

    @Test
    public void testListViaFlowableFlowable() {
        Flowable<String> w = Flowable.fromIterable(Arrays.asList("one", "two", "three"));
        Flowable<List<String>> observable = w.toList().toFlowable();

        Subscriber<List<String>> observer = TestHelper.mockSubscriber();
        observable.subscribe(observer);
        verify(observer, times(1)).onNext(Arrays.asList("one", "two", "three"));
        verify(observer, Mockito.never()).onError(any(Throwable.class));
        verify(observer, times(1)).onComplete();
    }

    @Test
    public void testListMultipleSubscribersFlowable() {
        Flowable<String> w = Flowable.fromIterable(Arrays.asList("one", "two", "three"));
        Flowable<List<String>> observable = w.toList().toFlowable();

        Subscriber<List<String>> o1 = TestHelper.mockSubscriber();
        observable.subscribe(o1);

        Subscriber<List<String>> o2 = TestHelper.mockSubscriber();
        observable.subscribe(o2);

        List<String> expected = Arrays.asList("one", "two", "three");

        verify(o1, times(1)).onNext(expected);
        verify(o1, Mockito.never()).onError(any(Throwable.class));
        verify(o1, times(1)).onComplete();

        verify(o2, times(1)).onNext(expected);
        verify(o2, Mockito.never()).onError(any(Throwable.class));
        verify(o2, times(1)).onComplete();
    }

    @Test
    @Ignore("Null values are not allowed")
    public void testListWithNullValueFlowable() {
        Flowable<String> w = Flowable.fromIterable(Arrays.asList("one", null, "three"));
        Flowable<List<String>> observable = w.toList().toFlowable();

        Subscriber<List<String>> observer = TestHelper.mockSubscriber();
        observable.subscribe(observer);
        verify(observer, times(1)).onNext(Arrays.asList("one", null, "three"));
        verify(observer, Mockito.never()).onError(any(Throwable.class));
        verify(observer, times(1)).onComplete();
    }

    @Test
    public void testListWithBlockingFirstFlowable() {
        Flowable<String> o = Flowable.fromIterable(Arrays.asList("one", "two", "three"));
        List<String> actual = o.toList().toFlowable().blockingFirst();
        Assert.assertEquals(Arrays.asList("one", "two", "three"), actual);
    }
    @Test
    public void testBackpressureHonoredFlowable() {
        Flowable<List<Integer>> w = Flowable.just(1, 2, 3, 4, 5).toList().toFlowable();
        TestSubscriber<List<Integer>> ts = new TestSubscriber<List<Integer>>(0L);

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
    @Ignore("PublishProcessor no longer emits without requests so this test fails due to the race of onComplete and request")
    public void testAsyncRequestedFlowable() {
        Scheduler.Worker w = Schedulers.newThread().createWorker();
        try {
            for (int i = 0; i < 1000; i++) {
                if (i % 50 == 0) {
                    System.out.println("testAsyncRequested -> " + i);
                }
                PublishProcessor<Integer> source = PublishProcessor.create();
                Flowable<List<Integer>> sorted = source.toList().toFlowable();

                final CyclicBarrier cb = new CyclicBarrier(2);
                final TestSubscriber<List<Integer>> ts = new TestSubscriber<List<Integer>>(0L);
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

    @SuppressWarnings("unchecked")
    @Test
    public void capacityHintFlowable() {
        Flowable.range(1, 10)
        .toList(4)
        .toFlowable()
        .test()
        .assertResult(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
    }

    @Test
    public void testList() {
        Flowable<String> w = Flowable.fromIterable(Arrays.asList("one", "two", "three"));
        Single<List<String>> observable = w.toList();

        SingleObserver<List<String>> observer = TestHelper.mockSingleObserver();
        observable.subscribe(observer);
        verify(observer, times(1)).onSuccess(Arrays.asList("one", "two", "three"));
        verify(observer, Mockito.never()).onError(any(Throwable.class));
    }

    @Test
    public void testListViaFlowable() {
        Flowable<String> w = Flowable.fromIterable(Arrays.asList("one", "two", "three"));
        Single<List<String>> observable = w.toList();

        SingleObserver<List<String>> observer = TestHelper.mockSingleObserver();
        observable.subscribe(observer);
        verify(observer, times(1)).onSuccess(Arrays.asList("one", "two", "three"));
        verify(observer, Mockito.never()).onError(any(Throwable.class));
    }

    @Test
    public void testListMultipleSubscribers() {
        Flowable<String> w = Flowable.fromIterable(Arrays.asList("one", "two", "three"));
        Single<List<String>> observable = w.toList();

        SingleObserver<List<String>> o1 = TestHelper.mockSingleObserver();
        observable.subscribe(o1);

        SingleObserver<List<String>> o2 = TestHelper.mockSingleObserver();
        observable.subscribe(o2);

        List<String> expected = Arrays.asList("one", "two", "three");

        verify(o1, times(1)).onSuccess(expected);
        verify(o1, Mockito.never()).onError(any(Throwable.class));

        verify(o2, times(1)).onSuccess(expected);
        verify(o2, Mockito.never()).onError(any(Throwable.class));
    }

    @Test
    @Ignore("Null values are not allowed")
    public void testListWithNullValue() {
        Flowable<String> w = Flowable.fromIterable(Arrays.asList("one", null, "three"));
        Single<List<String>> observable = w.toList();

        SingleObserver<List<String>> observer = TestHelper.mockSingleObserver();
        observable.subscribe(observer);
        verify(observer, times(1)).onSuccess(Arrays.asList("one", null, "three"));
        verify(observer, Mockito.never()).onError(any(Throwable.class));
    }

    @Test
    public void testListWithBlockingFirst() {
        Flowable<String> o = Flowable.fromIterable(Arrays.asList("one", "two", "three"));
        List<String> actual = o.toList().blockingGet();
        Assert.assertEquals(Arrays.asList("one", "two", "three"), actual);
    }
    @Test
    @Ignore("Single doesn't do backpressure")
    public void testBackpressureHonored() {
        Single<List<Integer>> w = Flowable.just(1, 2, 3, 4, 5).toList();
        TestObserver<List<Integer>> ts = new TestObserver<List<Integer>>();

        w.subscribe(ts);

        ts.assertNoValues();
        ts.assertNoErrors();
        ts.assertNotComplete();

//        ts.request(1);

        ts.assertValue(Arrays.asList(1, 2, 3, 4, 5));
        ts.assertNoErrors();
        ts.assertComplete();

//        ts.request(1);

        ts.assertValue(Arrays.asList(1, 2, 3, 4, 5));
        ts.assertNoErrors();
        ts.assertComplete();
    }
    @Test(timeout = 2000)
    @Ignore("PublishProcessor no longer emits without requests so this test fails due to the race of onComplete and request")
    public void testAsyncRequested() {
        Scheduler.Worker w = Schedulers.newThread().createWorker();
        try {
            for (int i = 0; i < 1000; i++) {
                if (i % 50 == 0) {
                    System.out.println("testAsyncRequested -> " + i);
                }
                PublishProcessor<Integer> source = PublishProcessor.create();
                Single<List<Integer>> sorted = source.toList();

                final CyclicBarrier cb = new CyclicBarrier(2);
                final TestObserver<List<Integer>> ts = new TestObserver<List<Integer>>();
                sorted.subscribe(ts);

                w.schedule(new Runnable() {
                    @Override
                    public void run() {
                        await(cb);
//                        ts.request(1);
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

    @SuppressWarnings("unchecked")
    @Test
    public void capacityHint() {
        Flowable.range(1, 10)
        .toList(4)
        .test()
        .assertResult(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
    }

    @Test
    public void dispose() {
        TestHelper.checkDisposed(Flowable.just(1).toList().toFlowable());

        TestHelper.checkDisposed(Flowable.just(1).toList());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void error() {
        Flowable.error(new TestException())
        .toList()
        .toFlowable()
        .test()
        .assertFailure(TestException.class);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void errorSingle() {
        Flowable.error(new TestException())
        .toList()
        .test()
        .assertFailure(TestException.class);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void collectionSupplierThrows() {
        Flowable.just(1)
        .toList(new Callable<Collection<Integer>>() {
            @Override
            public Collection<Integer> call() throws Exception {
                throw new TestException();
            }
        })
        .toFlowable()
        .test()
        .assertFailure(TestException.class);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void collectionSupplierReturnsNull() {
        Flowable.just(1)
        .toList(new Callable<Collection<Integer>>() {
            @Override
            public Collection<Integer> call() throws Exception {
                return null;
            }
        })
        .toFlowable()
        .test()
        .assertFailure(NullPointerException.class)
        .assertErrorMessage("The collectionSupplier returned a null collection. Null values are generally not allowed in 2.x operators and sources.");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void singleCollectionSupplierThrows() {
        Flowable.just(1)
        .toList(new Callable<Collection<Integer>>() {
            @Override
            public Collection<Integer> call() throws Exception {
                throw new TestException();
            }
        })
        .test()
        .assertFailure(TestException.class);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void singleCollectionSupplierReturnsNull() {
        Flowable.just(1)
        .toList(new Callable<Collection<Integer>>() {
            @Override
            public Collection<Integer> call() throws Exception {
                return null;
            }
        })
        .test()
        .assertFailure(NullPointerException.class)
        .assertErrorMessage("The collectionSupplier returned a null collection. Null values are generally not allowed in 2.x operators and sources.");
    }

    @Test
    public void onNextCancelRace() {
        for (int i = 0; i < 1000; i++) {
            final PublishProcessor<Integer> pp = PublishProcessor.create();
            final TestObserver<List<Integer>> ts = pp.toList().test();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    pp.onNext(1);
                }
            };
            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    ts.cancel();
                }
            };

            TestHelper.race(r1, r2);
        }
    }

    @Test
    public void onNextCancelRaceFlowable() {
        for (int i = 0; i < 1000; i++) {
            final PublishProcessor<Integer> pp = PublishProcessor.create();
            final TestSubscriber<List<Integer>> ts = pp.toList().toFlowable().test();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    pp.onNext(1);
                }
            };
            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    ts.cancel();
                }
            };

            TestHelper.race(r1, r2);
        }

    }

    @Test
    public void onCompleteCancelRaceFlowable() {
        for (int i = 0; i < 1000; i++) {
            final PublishProcessor<Integer> pp = PublishProcessor.create();
            final TestSubscriber<List<Integer>> ts = pp.toList().toFlowable().test();

            pp.onNext(1);

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    pp.onComplete();
                }
            };
            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    ts.cancel();
                }
            };

            TestHelper.race(r1, r2);

            if (ts.valueCount() != 0) {
                ts.assertValue(Arrays.asList(1))
                .assertNoErrors();
            }
        }
    }
}
