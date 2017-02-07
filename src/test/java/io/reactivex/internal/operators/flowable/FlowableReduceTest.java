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

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.*;
import org.reactivestreams.*;

import io.reactivex.*;
import io.reactivex.exceptions.TestException;
import io.reactivex.functions.*;
import io.reactivex.internal.fuseable.HasUpstreamPublisher;
import io.reactivex.internal.subscriptions.BooleanSubscription;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subscribers.TestSubscriber;

public class FlowableReduceTest {
    Subscriber<Object> observer;

    SingleObserver<Object> singleObserver;

    @Before
    public void before() {
        observer = TestHelper.mockSubscriber();
        singleObserver = TestHelper.mockSingleObserver();
    }

    BiFunction<Integer, Integer, Integer> sum = new BiFunction<Integer, Integer, Integer>() {
        @Override
        public Integer apply(Integer t1, Integer t2) {
            return t1 + t2;
        }
    };

    @Test
    public void testAggregateAsIntSumFlowable() {

        Flowable<Integer> result = Flowable.just(1, 2, 3, 4, 5).reduce(0, sum).toFlowable()
                .map(new Function<Integer, Integer>() {
                    @Override
                    public Integer apply(Integer v) {
                        return v;
                    }
                });

        result.subscribe(observer);

        verify(observer).onNext(1 + 2 + 3 + 4 + 5);
        verify(observer).onComplete();
        verify(observer, never()).onError(any(Throwable.class));
    }

    @Test
    public void testAggregateAsIntSumSourceThrowsFlowable() {
        Flowable<Integer> result = Flowable.concat(Flowable.just(1, 2, 3, 4, 5),
                Flowable.<Integer> error(new TestException()))
                .reduce(0, sum).toFlowable().map(new Function<Integer, Integer>() {
                    @Override
                    public Integer apply(Integer v) {
                        return v;
                    }
                });

        result.subscribe(observer);

        verify(observer, never()).onNext(any());
        verify(observer, never()).onComplete();
        verify(observer, times(1)).onError(any(TestException.class));
    }

    @Test
    public void testAggregateAsIntSumAccumulatorThrowsFlowable() {
        BiFunction<Integer, Integer, Integer> sumErr = new BiFunction<Integer, Integer, Integer>() {
            @Override
            public Integer apply(Integer t1, Integer t2) {
                throw new TestException();
            }
        };

        Flowable<Integer> result = Flowable.just(1, 2, 3, 4, 5)
                .reduce(0, sumErr).toFlowable().map(new Function<Integer, Integer>() {
                    @Override
                    public Integer apply(Integer v) {
                        return v;
                    }
                });

        result.subscribe(observer);

        verify(observer, never()).onNext(any());
        verify(observer, never()).onComplete();
        verify(observer, times(1)).onError(any(TestException.class));
    }

    @Test
    public void testAggregateAsIntSumResultSelectorThrowsFlowable() {

        Function<Integer, Integer> error = new Function<Integer, Integer>() {

            @Override
            public Integer apply(Integer t1) {
                throw new TestException();
            }
        };

        Flowable<Integer> result = Flowable.just(1, 2, 3, 4, 5)
                .reduce(0, sum).toFlowable().map(error);

        result.subscribe(observer);

        verify(observer, never()).onNext(any());
        verify(observer, never()).onComplete();
        verify(observer, times(1)).onError(any(TestException.class));
    }

    @Test
    public void testBackpressureWithInitialValueFlowable() throws InterruptedException {
        Flowable<Integer> source = Flowable.just(1, 2, 3, 4, 5, 6);
        Flowable<Integer> reduced = source.reduce(0, sum).toFlowable();

        Integer r = reduced.blockingFirst();
        assertEquals(21, r.intValue());
    }


    @Test
    public void testAggregateAsIntSum() {

        Single<Integer> result = Flowable.just(1, 2, 3, 4, 5).reduce(0, sum)
                .map(new Function<Integer, Integer>() {
                    @Override
                    public Integer apply(Integer v) {
                        return v;
                    }
                });

        result.subscribe(singleObserver);

        verify(singleObserver).onSuccess(1 + 2 + 3 + 4 + 5);
        verify(singleObserver, never()).onError(any(Throwable.class));
    }

    @Test
    public void testAggregateAsIntSumSourceThrows() {
        Single<Integer> result = Flowable.concat(Flowable.just(1, 2, 3, 4, 5),
                Flowable.<Integer> error(new TestException()))
                .reduce(0, sum).map(new Function<Integer, Integer>() {
                    @Override
                    public Integer apply(Integer v) {
                        return v;
                    }
                });

        result.subscribe(singleObserver);

        verify(singleObserver, never()).onSuccess(any());
        verify(singleObserver, times(1)).onError(any(TestException.class));
    }

    @Test
    public void testAggregateAsIntSumAccumulatorThrows() {
        BiFunction<Integer, Integer, Integer> sumErr = new BiFunction<Integer, Integer, Integer>() {
            @Override
            public Integer apply(Integer t1, Integer t2) {
                throw new TestException();
            }
        };

        Single<Integer> result = Flowable.just(1, 2, 3, 4, 5)
                .reduce(0, sumErr).map(new Function<Integer, Integer>() {
                    @Override
                    public Integer apply(Integer v) {
                        return v;
                    }
                });

        result.subscribe(singleObserver);

        verify(singleObserver, never()).onSuccess(any());
        verify(singleObserver, times(1)).onError(any(TestException.class));
    }

    @Test
    public void testAggregateAsIntSumResultSelectorThrows() {

        Function<Integer, Integer> error = new Function<Integer, Integer>() {

            @Override
            public Integer apply(Integer t1) {
                throw new TestException();
            }
        };

        Single<Integer> result = Flowable.just(1, 2, 3, 4, 5)
                .reduce(0, sum).map(error);

        result.subscribe(singleObserver);

        verify(singleObserver, never()).onSuccess(any());
        verify(singleObserver, times(1)).onError(any(TestException.class));
    }

    @Test
    public void testBackpressureWithNoInitialValue() throws InterruptedException {
        Flowable<Integer> source = Flowable.just(1, 2, 3, 4, 5, 6);
        Maybe<Integer> reduced = source.reduce(sum);

        Integer r = reduced.blockingGet();
        assertEquals(21, r.intValue());
    }

    @Test
    public void testBackpressureWithInitialValue() throws InterruptedException {
        Flowable<Integer> source = Flowable.just(1, 2, 3, 4, 5, 6);
        Single<Integer> reduced = source.reduce(0, sum);

        Integer r = reduced.blockingGet();
        assertEquals(21, r.intValue());
    }

    @Test
    public void reducerCrashSuppressOnError() {
        List<Throwable> errors = TestHelper.trackPluginErrors();

        try {
            Flowable.<Integer>fromPublisher(new Publisher<Integer>() {
                @Override
                public void subscribe(Subscriber<? super Integer> s) {
                    s.onSubscribe(new BooleanSubscription());
                    s.onNext(1);
                    s.onNext(1);
                    s.onError(new TestException("Source"));
                    s.onComplete();
                }
            })
            .reduce(new BiFunction<Integer, Integer, Integer>() {
                @Override
                public Integer apply(Integer a, Integer b) throws Exception {
                    throw new TestException("Reducer");
                }
            })
            .toFlowable()
            .test()
            .assertFailureAndMessage(TestException.class, "Reducer");

            TestHelper.assertUndeliverable(errors, 0, TestException.class, "Source");
        } finally {
            RxJavaPlugins.reset();
        }

    }

    @Test
    public void cancel() {

        TestSubscriber<Integer> ts = Flowable.just(1)
        .concatWith(Flowable.<Integer>never())
        .reduce(new BiFunction<Integer, Integer, Integer>() {
            @Override
            public Integer apply(Integer a, Integer b) throws Exception {
                return a + b;
            }
        }).toFlowable()
        .test();

        ts.assertEmpty();

        ts.cancel();

        ts.assertEmpty();

    }

    @Test
    public void testBackpressureWithNoInitialValueObservable() throws InterruptedException {
        Flowable<Integer> source = Flowable.just(1, 2, 3, 4, 5, 6);
        Flowable<Integer> reduced = source.reduce(sum).toFlowable();

        Integer r = reduced.blockingFirst();
        assertEquals(21, r.intValue());
    }

    @Test
    public void source() {
        Flowable<Integer> source = Flowable.just(1);

        assertSame(source, (((HasUpstreamPublisher<?>)source.reduce(sum))).source());
    }

    @Test
    public void dispose() {
        TestHelper.checkDisposed(Flowable.range(1, 2).reduce(sum));
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeFlowableToMaybe(new Function<Flowable<Integer>, MaybeSource<Integer>>() {
            @Override
            public MaybeSource<Integer> apply(Flowable<Integer> f) throws Exception {
                return f.reduce(sum);
            }
        });
    }

    @Test
    public void error() {
        Flowable.<Integer>error(new TestException())
        .reduce(sum)
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void errorFlowable() {
        Flowable.<Integer>error(new TestException())
        .reduce(sum)
        .toFlowable()
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void empty() {
        Flowable.<Integer>empty()
        .reduce(sum)
        .test()
        .assertResult();
    }

    @Test
    public void emptyFlowable() {
        Flowable.<Integer>empty()
        .reduce(sum)
        .toFlowable()
        .test()
        .assertResult();
    }

    @Test
    public void badSource() {
        TestHelper.checkBadSourceFlowable(new Function<Flowable<Integer>, Object>() {
            @Override
            public Object apply(Flowable<Integer> f) throws Exception {
                return f.reduce(sum);
            }
        }, false, 1, 1, 1);
    }

    @Test
    public void badSourceFlowable() {
        TestHelper.checkBadSourceFlowable(new Function<Flowable<Integer>, Object>() {
            @Override
            public Object apply(Flowable<Integer> f) throws Exception {
                return f.reduce(sum).toFlowable();
            }
        }, false, 1, 1, 1);
    }

    @Test
    public void reducerThrows() {
        Flowable.just(1, 2)
        .reduce(new BiFunction<Integer, Integer, Integer>() {
            @Override
            public Integer apply(Integer a, Integer b) throws Exception {
                throw new TestException();
            }
        })
        .test()
        .assertFailure(TestException.class);
    }

    /**
     * https://gist.github.com/jurna/353a2bd8ff83f0b24f0b5bc772077d61
     */
    @Test
    public void shouldReduceTo10Events() {
        final AtomicInteger count = new AtomicInteger();

        Flowable.range(0, 10).flatMap(new Function<Integer, Publisher<String>>() {
            @Override
            public Publisher<String> apply(final Integer x) throws Exception {
                return Flowable.range(0, 2)
                    .map(new Function<Integer, String>() {
                    @Override
                    public String apply(Integer y) throws Exception {
                        return blockingOp(x, y);
                    }
                }).subscribeOn(Schedulers.io())
                .reduce(new BiFunction<String, String, String>() {
                    @Override
                    public String apply(String l, String r) throws Exception {
                        return l + "_" + r;
                    }
                })
                .doOnSuccess(new Consumer<String>() {
                    @Override
                    public void accept(String s) throws Exception {
                        count.incrementAndGet();
                        System.out.println("Completed with " + s);}
                })
                .toFlowable();
            }
        }
        ).blockingLast();

        assertEquals(10, count.get());
    }

    /**
     * https://gist.github.com/jurna/353a2bd8ff83f0b24f0b5bc772077d61
     */
    @Test
    public void shouldReduceTo10EventsFlowable() {
        final AtomicInteger count = new AtomicInteger();

        Flowable.range(0, 10).flatMap(new Function<Integer, Publisher<String>>() {
            @Override
            public Publisher<String> apply(final Integer x) throws Exception {
                return Flowable.range(0, 2)
                    .map(new Function<Integer, String>() {
                    @Override
                    public String apply(Integer y) throws Exception {
                        return blockingOp(x, y);
                    }
                }).subscribeOn(Schedulers.io())
                .reduce(new BiFunction<String, String, String>() {
                    @Override
                    public String apply(String l, String r) throws Exception {
                        return l + "_" + r;
                    }
                })
                .toFlowable()
                .doOnNext(new Consumer<String>() {
                    @Override
                    public void accept(String s) throws Exception {
                        count.incrementAndGet();
                        System.out.println("Completed with " + s);}
                })
                ;
            }
        }
        ).blockingLast();

        assertEquals(10, count.get());
    }

    static String blockingOp(Integer x, Integer y) {
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return "x" + x + "y" + y;
    }
}
