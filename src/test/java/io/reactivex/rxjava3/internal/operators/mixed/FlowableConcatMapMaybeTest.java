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

package io.reactivex.rxjava3.internal.operators.mixed;

import static org.junit.Assert.*;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.rxjava3.disposables.Disposable;
import org.junit.Test;
import org.reactivestreams.Subscriber;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.exceptions.*;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.internal.functions.Functions;
import io.reactivex.rxjava3.internal.operators.mixed.FlowableConcatMapMaybe.ConcatMapMaybeSubscriber;
import io.reactivex.rxjava3.internal.subscriptions.BooleanSubscription;
import io.reactivex.rxjava3.internal.util.ErrorMode;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.processors.PublishProcessor;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subjects.MaybeSubject;
import io.reactivex.rxjava3.subscribers.TestSubscriber;
import io.reactivex.rxjava3.testsupport.*;

public class FlowableConcatMapMaybeTest extends RxJavaTest {

    @Test
    public void simple() {
        Flowable.range(1, 5)
        .concatMapMaybe(new Function<Integer, MaybeSource<Integer>>() {
            @Override
            public MaybeSource<Integer> apply(Integer v)
                    throws Exception {
                return Maybe.just(v);
            }
        })
        .test()
        .assertResult(1, 2, 3, 4, 5);
    }

    @Test
    public void simpleLong() {
        Flowable.range(1, 1024)
        .concatMapMaybe(new Function<Integer, MaybeSource<Integer>>() {
            @Override
            public MaybeSource<Integer> apply(Integer v)
                    throws Exception {
                return Maybe.just(v);
            }
        }, 32)
        .test()
        .assertValueCount(1024)
        .assertNoErrors()
        .assertComplete();
    }

    @Test
    public void backpressure() {
        TestSubscriber<Integer> ts = Flowable.range(1, 1024)
        .concatMapMaybe(new Function<Integer, MaybeSource<Integer>>() {
            @Override
            public MaybeSource<Integer> apply(Integer v)
                    throws Exception {
                return Maybe.just(v);
            }
        }, 32)
        .test(0);

        for (int i = 1; i <= 1024; i++) {
            ts.assertValueCount(i - 1)
            .assertNoErrors()
            .assertNotComplete()
            .requestMore(1)
            .assertValueCount(i)
            .assertNoErrors();
        }

        ts.assertComplete();
    }

    @Test
    public void empty() {
        Flowable.range(1, 10)
        .concatMapMaybe(new Function<Integer, MaybeSource<Integer>>() {
            @Override
            public MaybeSource<Integer> apply(Integer v)
                    throws Exception {
                return Maybe.empty();
            }
        })
        .test()
        .assertResult();
    }

    @Test
    public void mixed() {
        Flowable.range(1, 10)
        .concatMapMaybe(new Function<Integer, MaybeSource<Integer>>() {
            @Override
            public MaybeSource<Integer> apply(Integer v)
                    throws Exception {
                if (v % 2 == 0) {
                    return Maybe.just(v);
                }
                return Maybe.empty();
            }
        })
        .test()
        .assertResult(2, 4, 6, 8, 10);
    }

    @Test
    public void mixedLong() {
        TestSubscriberEx<Integer> ts = Flowable.range(1, 1024)
        .concatMapMaybe(new Function<Integer, MaybeSource<Integer>>() {
            @Override
            public MaybeSource<Integer> apply(Integer v)
                    throws Exception {
                if (v % 2 == 0) {
                    return Maybe.just(v).subscribeOn(Schedulers.computation());
                }
                return Maybe.<Integer>empty().subscribeOn(Schedulers.computation());
            }
        })
        .to(TestHelper.<Integer>testConsumer())
        .awaitDone(5, TimeUnit.SECONDS)
        .assertValueCount(512)
        .assertNoErrors()
        .assertComplete()
        ;

        for (int i = 0; i < 512; i ++) {
            ts.assertValueAt(i, (i + 1) * 2);
        }
    }

    @Test
    public void mainError() {
        Flowable.error(new TestException())
        .concatMapMaybe(Functions.justFunction(Maybe.just(1)))
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void innerError() {
        Flowable.just(1)
        .concatMapMaybe(Functions.justFunction(Maybe.error(new TestException())))
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void mainBoundaryErrorInnerSuccess() {
        PublishProcessor<Integer> pp = PublishProcessor.create();
        MaybeSubject<Integer> ms = MaybeSubject.create();

        TestSubscriber<Integer> ts = pp.concatMapMaybeDelayError(Functions.justFunction(ms), false).test();

        ts.assertEmpty();

        pp.onNext(1);

        assertTrue(ms.hasObservers());

        pp.onError(new TestException());

        assertTrue(ms.hasObservers());

        ts.assertEmpty();

        ms.onSuccess(1);

        ts.assertFailure(TestException.class, 1);
    }

    @Test
    public void mainBoundaryErrorInnerEmpty() {
        PublishProcessor<Integer> pp = PublishProcessor.create();
        MaybeSubject<Integer> ms = MaybeSubject.create();

        TestSubscriber<Integer> ts = pp.concatMapMaybeDelayError(Functions.justFunction(ms), false).test();

        ts.assertEmpty();

        pp.onNext(1);

        assertTrue(ms.hasObservers());

        pp.onError(new TestException());

        assertTrue(ms.hasObservers());

        ts.assertEmpty();

        ms.onComplete();

        ts.assertFailure(TestException.class);
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeFlowable(
                new Function<Flowable<Object>, Flowable<Object>>() {
                    @Override
                    public Flowable<Object> apply(Flowable<Object> f)
                            throws Exception {
                        return f.concatMapMaybeDelayError(
                                Functions.justFunction(Maybe.empty()));
                    }
                }
        );
    }

    @Test
    public void queueOverflow() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            new Flowable<Integer>() {
                @Override
                protected void subscribeActual(Subscriber<? super Integer> s) {
                    s.onSubscribe(new BooleanSubscription());
                    s.onNext(1);
                    s.onNext(2);
                    s.onNext(3);
                    s.onError(new TestException());
                }
            }
            .concatMapMaybe(
                    Functions.justFunction(Maybe.never()), 1
            )
            .test()
            .assertFailure(MissingBackpressureException.class);

            TestHelper.assertUndeliverable(errors, 0, TestException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void limit() {
        Flowable.range(1, 5)
        .concatMapMaybe(new Function<Integer, MaybeSource<Integer>>() {
            @Override
            public MaybeSource<Integer> apply(Integer v)
                    throws Exception {
                return Maybe.just(v);
            }
        })
        .take(3)
        .test()
        .assertResult(1, 2, 3);
    }

    @Test
    public void cancel() {
        Flowable.range(1, 5)
        .concatMapMaybe(new Function<Integer, MaybeSource<Integer>>() {
            @Override
            public MaybeSource<Integer> apply(Integer v)
                    throws Exception {
                return Maybe.just(v);
            }
        })
        .test(3)
        .assertValues(1, 2, 3)
        .assertNoErrors()
        .assertNotComplete()
        .cancel();
    }

    @Test
    public void innerErrorAfterMainError() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            final PublishProcessor<Integer> pp = PublishProcessor.create();

            final AtomicReference<MaybeObserver<? super Integer>> obs = new AtomicReference<>();

            TestSubscriberEx<Integer> ts = pp.concatMapMaybe(
                    new Function<Integer, MaybeSource<Integer>>() {
                        @Override
                        public MaybeSource<Integer> apply(Integer v)
                                throws Exception {
                            return new Maybe<Integer>() {
                                    @Override
                                    protected void subscribeActual(
                                            MaybeObserver<? super Integer> observer) {
                                        observer.onSubscribe(Disposable.empty());
                                        obs.set(observer);
                                    }
                            };
                        }
                    }
            ).to(TestHelper.<Integer>testConsumer());

            pp.onNext(1);

            pp.onError(new TestException("outer"));
            obs.get().onError(new TestException("inner"));

            ts.assertFailureAndMessage(TestException.class, "outer");

            TestHelper.assertUndeliverable(errors, 0, TestException.class, "inner");
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void delayAllErrors() {
        TestSubscriberEx<Object> ts = Flowable.range(1, 5)
        .concatMapMaybeDelayError(new Function<Integer, MaybeSource<? extends Object>>() {
            @Override
            public MaybeSource<? extends Object> apply(Integer v)
                    throws Exception {
                return Maybe.error(new TestException());
            }
        })
        .to(TestHelper.<Object>testConsumer())
        .assertFailure(CompositeException.class)
        ;

        CompositeException ce = (CompositeException)ts.errors().get(0);
        assertEquals(5, ce.getExceptions().size());
    }

    @Test
    public void mapperCrash() {
        final PublishProcessor<Integer> pp = PublishProcessor.create();

        TestSubscriber<Object> ts = pp
        .concatMapMaybe(new Function<Integer, MaybeSource<? extends Object>>() {
            @Override
            public MaybeSource<? extends Object> apply(Integer v)
                    throws Exception {
                        throw new TestException();
                    }
        })
        .test();

        ts.assertEmpty();

        assertTrue(pp.hasSubscribers());

        pp.onNext(1);

        ts.assertFailure(TestException.class);

        assertFalse(pp.hasSubscribers());
    }

    @Test
    public void cancelNoConcurrentClean() {
        TestSubscriber<Integer> ts = new TestSubscriber<>();
        ConcatMapMaybeSubscriber<Integer, Integer> operator =
                new ConcatMapMaybeSubscriber<>(
                        ts, Functions.justFunction(Maybe.<Integer>never()), 16, ErrorMode.IMMEDIATE);

        operator.onSubscribe(new BooleanSubscription());

        operator.queue.offer(1);

        operator.getAndIncrement();

        ts.cancel();

        assertFalse(operator.queue.isEmpty());

        operator.addAndGet(-2);

        operator.cancel();

        assertTrue(operator.queue.isEmpty());
    }

    @Test
    public void innerSuccessDisposeRace() {
        for (int i = 0; i < TestHelper.RACE_LONG_LOOPS; i++) {

            final MaybeSubject<Integer> ms = MaybeSubject.create();

            final TestSubscriber<Integer> ts = Flowable.just(1)
                    .hide()
                    .concatMapMaybe(Functions.justFunction(ms))
                    .test();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    ms.onSuccess(1);
                }
            };
            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    ts.cancel();
                }
            };

            TestHelper.race(r1, r2);

            ts.assertNoErrors();
        }
    }

    @Test
    public void undeliverableUponCancel() {
        TestHelper.checkUndeliverableUponCancel(new FlowableConverter<Integer, Flowable<Integer>>() {
            @Override
            public Flowable<Integer> apply(Flowable<Integer> upstream) {
                return upstream.concatMapMaybe(new Function<Integer, Maybe<Integer>>() {
                    @Override
                    public Maybe<Integer> apply(Integer v) throws Throwable {
                        return Maybe.just(v).hide();
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
                return upstream.concatMapMaybeDelayError(new Function<Integer, Maybe<Integer>>() {
                    @Override
                    public Maybe<Integer> apply(Integer v) throws Throwable {
                        return Maybe.just(v).hide();
                    }
                }, false, 2);
            }
        });
    }

    @Test
    public void undeliverableUponCancelDelayErrorTillEnd() {
        TestHelper.checkUndeliverableUponCancel(new FlowableConverter<Integer, Flowable<Integer>>() {
            @Override
            public Flowable<Integer> apply(Flowable<Integer> upstream) {
                return upstream.concatMapMaybeDelayError(new Function<Integer, Maybe<Integer>>() {
                    @Override
                    public Maybe<Integer> apply(Integer v) throws Throwable {
                        return Maybe.just(v).hide();
                    }
                }, true, 2);
            }
        });
    }
}
