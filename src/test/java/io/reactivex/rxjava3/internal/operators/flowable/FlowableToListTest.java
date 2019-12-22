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

package io.reactivex.rxjava3.internal.operators.flowable;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.*;
import java.util.concurrent.*;

import org.junit.*;
import org.mockito.Mockito;
import org.reactivestreams.Subscriber;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.exceptions.TestException;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.internal.util.ExceptionHelper;
import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.rxjava3.processors.PublishProcessor;
import io.reactivex.rxjava3.subscribers.TestSubscriber;
import io.reactivex.rxjava3.testsupport.TestHelper;

public class FlowableToListTest extends RxJavaTest {

    @Test
    public void listFlowable() {
        Flowable<String> w = Flowable.fromIterable(Arrays.asList("one", "two", "three"));
        Flowable<List<String>> flowable = w.toList().toFlowable();

        Subscriber<List<String>> subscriber = TestHelper.mockSubscriber();
        flowable.subscribe(subscriber);

        verify(subscriber, times(1)).onNext(Arrays.asList("one", "two", "three"));
        verify(subscriber, Mockito.never()).onError(any(Throwable.class));
        verify(subscriber, times(1)).onComplete();
    }

    @Test
    public void listViaFlowableFlowable() {
        Flowable<String> w = Flowable.fromIterable(Arrays.asList("one", "two", "three"));
        Flowable<List<String>> flowable = w.toList().toFlowable();

        Subscriber<List<String>> subscriber = TestHelper.mockSubscriber();
        flowable.subscribe(subscriber);

        verify(subscriber, times(1)).onNext(Arrays.asList("one", "two", "three"));
        verify(subscriber, Mockito.never()).onError(any(Throwable.class));
        verify(subscriber, times(1)).onComplete();
    }

    @Test
    public void listMultipleSubscribersFlowable() {
        Flowable<String> w = Flowable.fromIterable(Arrays.asList("one", "two", "three"));
        Flowable<List<String>> flowable = w.toList().toFlowable();

        Subscriber<List<String>> subscriber1 = TestHelper.mockSubscriber();
        flowable.subscribe(subscriber1);

        Subscriber<List<String>> subscriber2 = TestHelper.mockSubscriber();
        flowable.subscribe(subscriber2);

        List<String> expected = Arrays.asList("one", "two", "three");

        verify(subscriber1, times(1)).onNext(expected);
        verify(subscriber1, Mockito.never()).onError(any(Throwable.class));
        verify(subscriber1, times(1)).onComplete();

        verify(subscriber2, times(1)).onNext(expected);
        verify(subscriber2, Mockito.never()).onError(any(Throwable.class));
        verify(subscriber2, times(1)).onComplete();
    }

    @Test
    public void listWithBlockingFirstFlowable() {
        Flowable<String> f = Flowable.fromIterable(Arrays.asList("one", "two", "three"));
        List<String> actual = f.toList().toFlowable().blockingFirst();
        Assert.assertEquals(Arrays.asList("one", "two", "three"), actual);
    }

    @Test
    public void backpressureHonoredFlowable() {
        Flowable<List<Integer>> w = Flowable.just(1, 2, 3, 4, 5).toList().toFlowable();
        TestSubscriber<List<Integer>> ts = new TestSubscriber<>(0L);

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

    @Test
    public void capacityHintFlowable() {
        Flowable.range(1, 10)
        .toList(4)
        .toFlowable()
        .test()
        .assertResult(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
    }

    @Test
    public void list() {
        Flowable<String> w = Flowable.fromIterable(Arrays.asList("one", "two", "three"));
        Single<List<String>> single = w.toList();

        SingleObserver<List<String>> observer = TestHelper.mockSingleObserver();
        single.subscribe(observer);
        verify(observer, times(1)).onSuccess(Arrays.asList("one", "two", "three"));
        verify(observer, Mockito.never()).onError(any(Throwable.class));
    }

    @Test
    public void listViaFlowable() {
        Flowable<String> w = Flowable.fromIterable(Arrays.asList("one", "two", "three"));
        Single<List<String>> single = w.toList();

        SingleObserver<List<String>> observer = TestHelper.mockSingleObserver();
        single.subscribe(observer);
        verify(observer, times(1)).onSuccess(Arrays.asList("one", "two", "three"));
        verify(observer, Mockito.never()).onError(any(Throwable.class));
    }

    @Test
    public void listMultipleSubscribers() {
        Flowable<String> w = Flowable.fromIterable(Arrays.asList("one", "two", "three"));
        Single<List<String>> single = w.toList();

        SingleObserver<List<String>> o1 = TestHelper.mockSingleObserver();
        single.subscribe(o1);

        SingleObserver<List<String>> o2 = TestHelper.mockSingleObserver();
        single.subscribe(o2);

        List<String> expected = Arrays.asList("one", "two", "three");

        verify(o1, times(1)).onSuccess(expected);
        verify(o1, Mockito.never()).onError(any(Throwable.class));

        verify(o2, times(1)).onSuccess(expected);
        verify(o2, Mockito.never()).onError(any(Throwable.class));
    }

    @Test
    public void listWithBlockingFirst() {
        Flowable<String> f = Flowable.fromIterable(Arrays.asList("one", "two", "three"));
        List<String> actual = f.toList().blockingGet();
        Assert.assertEquals(Arrays.asList("one", "two", "three"), actual);
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

    @Test
    public void error() {
        Flowable.error(new TestException())
        .toList()
        .toFlowable()
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void errorSingle() {
        Flowable.error(new TestException())
        .toList()
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void collectionSupplierThrows() {
        Flowable.just(1)
        .toList(new Supplier<Collection<Integer>>() {
            @Override
            public Collection<Integer> get() throws Exception {
                throw new TestException();
            }
        })
        .toFlowable()
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void collectionSupplierReturnsNull() {
        Flowable.just(1)
        .toList(new Supplier<Collection<Integer>>() {
            @Override
            public Collection<Integer> get() throws Exception {
                return null;
            }
        })
        .toFlowable()
        .to(TestHelper.<Collection<Integer>>testConsumer())
        .assertFailure(NullPointerException.class)
        .assertErrorMessage(ExceptionHelper.nullWarning("The collectionSupplier returned a null Collection."));
    }

    @Test
    public void singleCollectionSupplierThrows() {
        Flowable.just(1)
        .toList(new Supplier<Collection<Integer>>() {
            @Override
            public Collection<Integer> get() throws Exception {
                throw new TestException();
            }
        })
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void singleCollectionSupplierReturnsNull() {
        Flowable.just(1)
        .toList(new Supplier<Collection<Integer>>() {
            @Override
            public Collection<Integer> get() throws Exception {
                return null;
            }
        })
        .to(TestHelper.<Collection<Integer>>testConsumer())
        .assertFailure(NullPointerException.class)
        .assertErrorMessage(ExceptionHelper.nullWarning("The collectionSupplier returned a null Collection."));
    }

    @Test
    public void onNextCancelRace() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            final PublishProcessor<Integer> pp = PublishProcessor.create();
            final TestObserver<List<Integer>> to = pp.toList().test();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    pp.onNext(1);
                }
            };
            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    to.dispose();
                }
            };

            TestHelper.race(r1, r2);
        }
    }

    @Test
    public void onNextCancelRaceFlowable() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
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
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
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

            if (ts.values().size() != 0) {
                ts.assertValue(Arrays.asList(1))
                .assertNoErrors();
            }
        }
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeFlowable(new Function<Flowable<Object>, Flowable<List<Object>>>() {
            @Override
            public Flowable<List<Object>> apply(Flowable<Object> f)
                    throws Exception {
                return f.toList().toFlowable();
            }
        });
        TestHelper.checkDoubleOnSubscribeFlowableToSingle(new Function<Flowable<Object>, Single<List<Object>>>() {
            @Override
            public Single<List<Object>> apply(Flowable<Object> f)
                    throws Exception {
                return f.toList();
            }
        });
    }
}
