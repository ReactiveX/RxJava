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

import static org.junit.Assert.*;

import java.util.List;
import java.util.concurrent.*;

import org.junit.Test;
import org.reactivestreams.Subscriber;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.disposables.*;
import io.reactivex.rxjava3.exceptions.*;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.internal.functions.Functions;
import io.reactivex.rxjava3.internal.subscriptions.BooleanSubscription;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.processors.PublishProcessor;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subscribers.TestSubscriber;
import io.reactivex.rxjava3.testsupport.*;

public class FlowableFlatMapSingleTest extends RxJavaTest {

    @Test
    public void normal() {
        Flowable.range(1, 10)
        .flatMapSingle(new Function<Integer, SingleSource<Integer>>() {
            @Override
            public SingleSource<Integer> apply(Integer v) throws Exception {
                return Single.just(v);
            }
        })
        .test()
        .assertResult(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
    }

    @Test
    public void normalDelayError() {
        Flowable.range(1, 10)
        .flatMapSingle(new Function<Integer, SingleSource<Integer>>() {
            @Override
            public SingleSource<Integer> apply(Integer v) throws Exception {
                return Single.just(v);
            }
        }, true, Integer.MAX_VALUE)
        .test()
        .assertResult(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
    }

    @Test
    public void normalAsync() {
        TestSubscriberEx<Integer> ts = Flowable.range(1, 10)
        .flatMapSingle(new Function<Integer, SingleSource<Integer>>() {
            @Override
            public SingleSource<Integer> apply(Integer v) throws Exception {
                return Single.just(v).subscribeOn(Schedulers.computation());
            }
        })
        .to(TestHelper.<Integer>testConsumer())
        .awaitDone(5, TimeUnit.SECONDS)
        .assertSubscribed()
        .assertValueCount(10)
        .assertNoErrors()
        .assertComplete();

        TestHelper.assertValueSet(ts, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
    }

    @Test
    public void normalAsyncMaxConcurrency() {
        TestSubscriberEx<Integer> ts = Flowable.range(1, 10)
        .flatMapSingle(new Function<Integer, SingleSource<Integer>>() {
            @Override
            public SingleSource<Integer> apply(Integer v) throws Exception {
                return Single.just(v).subscribeOn(Schedulers.computation());
            }
        }, false, 3)
        .to(TestHelper.<Integer>testConsumer())
        .awaitDone(5, TimeUnit.SECONDS)
        .assertSubscribed()
        .assertNoErrors()
        .assertComplete();

        TestHelper.assertValueSet(ts, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
     }

    @Test
    public void normalAsyncMaxConcurrency1() {
        Flowable.range(1, 10)
        .flatMapSingle(new Function<Integer, SingleSource<Integer>>() {
            @Override
            public SingleSource<Integer> apply(Integer v) throws Exception {
                return Single.just(v).subscribeOn(Schedulers.computation());
            }
        }, false, 1)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
    }

    @Test
    public void mapperThrowsFlowable() {
        PublishProcessor<Integer> pp = PublishProcessor.create();

        TestSubscriber<Integer> ts = pp
        .flatMapSingle(new Function<Integer, SingleSource<Integer>>() {
            @Override
            public SingleSource<Integer> apply(Integer v) throws Exception {
                throw new TestException();
            }
        })
        .test();

        assertTrue(pp.hasSubscribers());

        pp.onNext(1);

        ts.assertFailure(TestException.class);

        assertFalse(pp.hasSubscribers());
    }

    @Test
    public void mapperReturnsNullFlowable() {
        PublishProcessor<Integer> pp = PublishProcessor.create();

        TestSubscriber<Integer> ts = pp
        .flatMapSingle(new Function<Integer, SingleSource<Integer>>() {
            @Override
            public SingleSource<Integer> apply(Integer v) throws Exception {
                return null;
            }
        })
        .test();

        assertTrue(pp.hasSubscribers());

        pp.onNext(1);

        ts.assertFailure(NullPointerException.class);

        assertFalse(pp.hasSubscribers());
    }

    @Test
    public void normalDelayErrorAll() {
        TestSubscriberEx<Integer> ts = Flowable.range(1, 10).concatWith(Flowable.<Integer>error(new TestException()))
        .flatMapSingle(new Function<Integer, SingleSource<Integer>>() {
            @Override
            public SingleSource<Integer> apply(Integer v) throws Exception {
                return Single.error(new TestException());
            }
        }, true, Integer.MAX_VALUE)
        .to(TestHelper.<Integer>testConsumer())
        .assertFailure(CompositeException.class);

        List<Throwable> errors = TestHelper.compositeList(ts.errors().get(0));

        for (int i = 0; i < 11; i++) {
            TestHelper.assertError(errors, i, TestException.class);
        }
    }

    @Test
    public void normalBackpressured() {
        Flowable.range(1, 10)
        .flatMapSingle(new Function<Integer, SingleSource<Integer>>() {
            @Override
            public SingleSource<Integer> apply(Integer v) throws Exception {
                return Single.just(v);
            }
        })
        .rebatchRequests(1)
        .test()
        .assertResult(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
    }

    @Test
    public void normalMaxConcurrent1Backpressured() {
        Flowable.range(1, 10)
        .flatMapSingle(new Function<Integer, SingleSource<Integer>>() {
            @Override
            public SingleSource<Integer> apply(Integer v) throws Exception {
                return Single.just(v);
            }
        }, false, 1)
        .rebatchRequests(1)
        .test()
        .assertResult(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
    }

    @Test
    public void normalMaxConcurrent2Backpressured() {
        Flowable.range(1, 10)
        .flatMapSingle(new Function<Integer, SingleSource<Integer>>() {
            @Override
            public SingleSource<Integer> apply(Integer v) throws Exception {
                return Single.just(v);
            }
        }, false, 2)
        .rebatchRequests(1)
        .test()
        .assertResult(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
    }

    @Test
    public void takeAsync() {
        TestSubscriberEx<Integer> ts = Flowable.range(1, 10)
        .flatMapSingle(new Function<Integer, SingleSource<Integer>>() {
            @Override
            public SingleSource<Integer> apply(Integer v) throws Exception {
                return Single.just(v).subscribeOn(Schedulers.computation());
            }
        })
        .take(2)
        .to(TestHelper.<Integer>testConsumer())
        .awaitDone(5, TimeUnit.SECONDS)
        .assertSubscribed()
        .assertValueCount(2)
        .assertNoErrors()
        .assertComplete();

        TestHelper.assertValueSet(ts, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
    }

    @Test
    public void take() {
        Flowable.range(1, 10)
        .flatMapSingle(new Function<Integer, SingleSource<Integer>>() {
            @Override
            public SingleSource<Integer> apply(Integer v) throws Exception {
                return Single.just(v);
            }
        })
        .take(2)
        .test()
        .assertResult(1, 2);
    }

    @Test
    public void middleError() {
        Flowable.fromArray(new String[]{"1", "a", "2"})
        .flatMapSingle(new Function<String, SingleSource<Integer>>() {
            @Override
            public SingleSource<Integer> apply(final String s) throws NumberFormatException {
                //return Single.just(Integer.valueOf(s)); //This works
                return Single.fromCallable(new Callable<Integer>() {
                    @Override
                    public Integer call() throws NumberFormatException {
                        return Integer.valueOf(s);
                    }
                });
            }
        })
        .test()
        .assertFailure(NumberFormatException.class, 1);
    }

    @Test
    public void asyncFlatten() {
        Flowable.range(1, 1000)
        .flatMapSingle(new Function<Integer, SingleSource<Integer>>() {
            @Override
            public SingleSource<Integer> apply(Integer v) throws Exception {
                return Single.just(1).subscribeOn(Schedulers.computation());
            }
        })
        .take(500)
        .to(TestHelper.<Integer>testConsumer())
        .awaitDone(5, TimeUnit.SECONDS)
        .assertSubscribed()
        .assertValueCount(500)
        .assertNoErrors()
        .assertComplete();
    }

    @Test
    public void successError() {
        final PublishProcessor<Integer> pp = PublishProcessor.create();

        TestSubscriber<Integer> ts = Flowable.range(1, 2)
        .flatMapSingle(new Function<Integer, SingleSource<Integer>>() {
            @Override
            public SingleSource<Integer> apply(Integer v) throws Exception {
                if (v == 2) {
                    return pp.singleOrError();
                }
                return Single.error(new TestException());
            }
        }, true, Integer.MAX_VALUE)
        .test();

        pp.onNext(1);
        pp.onComplete();

        ts
        .assertFailure(TestException.class, 1);
    }

    @Test
    public void disposed() {
        TestHelper.checkDisposed(PublishProcessor.<Integer>create().flatMapSingle(new Function<Integer, SingleSource<Integer>>() {
            @Override
            public SingleSource<Integer> apply(Integer v) throws Exception {
                return Single.<Integer>just(1);
            }
        }));
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeFlowable(new Function<Flowable<Object>, Flowable<Integer>>() {
            @Override
            public Flowable<Integer> apply(Flowable<Object> f) throws Exception {
                return f.flatMapSingle(Functions.justFunction(Single.just(2)));
            }
        });
    }

    @Test
    public void badSource() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            new Flowable<Integer>() {
                @Override
                protected void subscribeActual(Subscriber<? super Integer> subscriber) {
                    subscriber.onSubscribe(new BooleanSubscription());
                    subscriber.onError(new TestException("First"));
                    subscriber.onError(new TestException("Second"));
                }
            }
            .flatMapSingle(Functions.justFunction(Single.just(2)))
            .to(TestHelper.<Integer>testConsumer())
            .assertFailureAndMessage(TestException.class, "First");

            TestHelper.assertUndeliverable(errors, 0, TestException.class, "Second");
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void badInnerSource() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            Flowable.just(1)
            .flatMapSingle(Functions.justFunction(new Single<Integer>() {
                @Override
                protected void subscribeActual(SingleObserver<? super Integer> observer) {
                    observer.onSubscribe(Disposable.empty());
                    observer.onError(new TestException("First"));
                    observer.onError(new TestException("Second"));
                }
            }))
            .to(TestHelper.<Integer>testConsumer())
            .assertFailureAndMessage(TestException.class, "First");

            TestHelper.assertUndeliverable(errors, 0, TestException.class, "Second");
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void emissionQueueTrigger() {
        final PublishProcessor<Integer> pp1 = PublishProcessor.create();
        final PublishProcessor<Integer> pp2 = PublishProcessor.create();

        TestSubscriber<Integer> ts = new TestSubscriber<Integer>() {
            @Override
            public void onNext(Integer t) {
                super.onNext(t);
                if (t == 1) {
                    pp2.onNext(2);
                    pp2.onComplete();
                }
            }
        };

        Flowable.just(pp1, pp2)
                .flatMapSingle(new Function<PublishProcessor<Integer>, SingleSource<Integer>>() {
                    @Override
                    public SingleSource<Integer> apply(PublishProcessor<Integer> v) throws Exception {
                        return v.singleOrError();
                    }
                })
        .subscribe(ts);

        pp1.onNext(1);
        pp1.onComplete();

        ts.assertResult(1, 2);
    }

    @Test
    public void disposeInner() {
        final TestSubscriber<Object> ts = new TestSubscriber<>();

        Flowable.just(1).flatMapSingle(new Function<Integer, SingleSource<Object>>() {
            @Override
            public SingleSource<Object> apply(Integer v) throws Exception {
                return new Single<Object>() {
                    @Override
                    protected void subscribeActual(SingleObserver<? super Object> observer) {
                        observer.onSubscribe(Disposable.empty());

                        assertFalse(((Disposable)observer).isDisposed());

                        ts.cancel();

                        assertTrue(((Disposable)observer).isDisposed());
                    }
                };
            }
        })
        .subscribe(ts);

        ts
        .assertEmpty();
    }

    @Test
    public void innerSuccessCompletesAfterMain() {
        PublishProcessor<Integer> pp = PublishProcessor.create();

        TestSubscriber<Integer> ts = Flowable.just(1).flatMapSingle(Functions.justFunction(pp.singleOrError()))
        .test();

        pp.onNext(2);
        pp.onComplete();

        ts
        .assertResult(2);
    }

    @Test
    public void backpressure() {
        TestSubscriber<Integer> ts = Flowable.just(1)
        .flatMapSingle(Functions.justFunction(Single.just(2)))
        .test(0L)
        .assertEmpty();

        ts.request(1);
        ts.assertResult(2);
    }

    @Test
    public void error() {
        Flowable.just(1)
        .flatMapSingle(Functions.justFunction(Single.<Integer>error(new TestException())))
        .test(0L)
        .assertFailure(TestException.class);
    }

    @Test
    public void errorDelayed() {
        Flowable.just(1)
        .flatMapSingle(Functions.justFunction(Single.<Integer>error(new TestException())), true, 16)
        .test(0L)
        .assertFailure(TestException.class);
    }

    @Test
    public void requestCancelRace() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            final TestSubscriber<Integer> ts = Flowable.just(1).concatWith(Flowable.<Integer>never())
            .flatMapSingle(Functions.justFunction(Single.just(2))).test(0);

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    ts.request(1);
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
    public void asyncFlattenErrorMaxConcurrency() {
        Flowable.range(1, 1000)
        .flatMapMaybe(new Function<Integer, MaybeSource<Integer>>() {
            @Override
            public MaybeSource<Integer> apply(Integer v) throws Exception {
                return Maybe.<Integer>error(new TestException()).subscribeOn(Schedulers.computation());
            }
        }, true, 128)
        .take(500)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(CompositeException.class);
    }

    @Test
    public void undeliverableUponCancel() {
        TestHelper.checkUndeliverableUponCancel(new FlowableConverter<Integer, Flowable<Integer>>() {
            @Override
            public Flowable<Integer> apply(Flowable<Integer> upstream) {
                return upstream.flatMapSingle(new Function<Integer, Single<Integer>>() {
                    @Override
                    public Single<Integer> apply(Integer v) throws Throwable {
                        return Single.just(v).hide();
                    }
                });
            }
        });
    }

    @Test
    public void undeliverableUponCancelDelayError() {
        TestHelper.checkUndeliverableUponCancel(new FlowableConverter<Integer, Flowable<Integer>>() {
            @Override
            public Flowable<Integer> apply(Flowable<Integer> upstream) {
                return upstream.flatMapSingle(new Function<Integer, Single<Integer>>() {
                    @Override
                    public Single<Integer> apply(Integer v) throws Throwable {
                        return Single.just(v).hide();
                    }
                }, true, 2);
            }
        });
    }
}
