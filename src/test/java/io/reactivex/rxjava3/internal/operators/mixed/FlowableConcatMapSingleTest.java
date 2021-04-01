/*
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
import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.rxjava3.disposables.Disposable;
import org.junit.Test;
import org.reactivestreams.Subscriber;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.exceptions.*;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.internal.functions.Functions;
import io.reactivex.rxjava3.internal.operators.mixed.FlowableConcatMapSingle.ConcatMapSingleSubscriber;
import io.reactivex.rxjava3.internal.subscriptions.BooleanSubscription;
import io.reactivex.rxjava3.internal.util.ErrorMode;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.processors.*;
import io.reactivex.rxjava3.subjects.SingleSubject;
import io.reactivex.rxjava3.subscribers.TestSubscriber;
import io.reactivex.rxjava3.testsupport.*;

public class FlowableConcatMapSingleTest extends RxJavaTest {

    @Test
    public void simple() {
        Flowable.range(1, 5)
        .concatMapSingle(new Function<Integer, SingleSource<Integer>>() {
            @Override
            public SingleSource<Integer> apply(Integer v)
                    throws Exception {
                return Single.just(v);
            }
        })
        .test()
        .assertResult(1, 2, 3, 4, 5);
    }

    @Test
    public void simpleLongPrefetch() {
        Flowable.range(1, 1024)
        .concatMapSingle(Single::just, 32)
        .test()
        .assertValueCount(1024)
        .assertNoErrors()
        .assertComplete();
    }

    @Test
    public void simpleLongPrefetchHidden() {
        Flowable.range(1, 1024).hide()
        .concatMapSingle(Single::just, 32)
        .test()
        .assertValueCount(1024)
        .assertNoErrors()
        .assertComplete();
    }

    @Test
    public void backpressure() {
        TestSubscriber<Integer> ts = Flowable.range(1, 1024)
        .concatMapSingle(new Function<Integer, SingleSource<Integer>>() {
            @Override
            public SingleSource<Integer> apply(Integer v)
                    throws Exception {
                return Single.just(v);
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
    public void mainError() {
        Flowable.error(new TestException())
        .concatMapSingle(Functions.justFunction(Single.just(1)))
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void innerError() {
        Flowable.just(1)
        .concatMapSingle(Functions.justFunction(Single.error(new TestException())))
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void mainBoundaryErrorInnerSuccess() {
        PublishProcessor<Integer> pp = PublishProcessor.create();
        SingleSubject<Integer> ms = SingleSubject.create();

        TestSubscriber<Integer> ts = pp.concatMapSingleDelayError(Functions.justFunction(ms), false).test();

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
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeFlowable(
                new Function<Flowable<Object>, Flowable<Object>>() {
                    @Override
                    public Flowable<Object> apply(Flowable<Object> f)
                            throws Exception {
                        return f.concatMapSingleDelayError(
                                Functions.justFunction(Single.just((Object)1)));
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
            .concatMapSingle(
                    Functions.justFunction(Single.never()), 1
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
        .concatMapSingle(new Function<Integer, SingleSource<Integer>>() {
            @Override
            public SingleSource<Integer> apply(Integer v)
                    throws Exception {
                return Single.just(v);
            }
        })
        .take(3)
        .test()
        .assertResult(1, 2, 3);
    }

    @Test
    public void cancel() {
        Flowable.range(1, 5)
        .concatMapSingle(new Function<Integer, SingleSource<Integer>>() {
            @Override
            public SingleSource<Integer> apply(Integer v)
                    throws Exception {
                return Single.just(v);
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

            final AtomicReference<SingleObserver<? super Integer>> obs = new AtomicReference<>();

            TestSubscriberEx<Integer> ts = pp.concatMapSingle(
                    new Function<Integer, SingleSource<Integer>>() {
                        @Override
                        public SingleSource<Integer> apply(Integer v)
                                throws Exception {
                            return new Single<Integer>() {
                                    @Override
                                    protected void subscribeActual(
                                            SingleObserver<? super Integer> observer) {
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
        .concatMapSingleDelayError(new Function<Integer, SingleSource<? extends Object>>() {
            @Override
            public SingleSource<? extends Object> apply(Integer v)
                    throws Exception {
                return Single.error(new TestException());
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
        .concatMapSingle(new Function<Integer, SingleSource<? extends Object>>() {
            @Override
            public SingleSource<? extends Object> apply(Integer v)
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
        ConcatMapSingleSubscriber<Integer, Integer> operator =
                new ConcatMapSingleSubscriber<>(
                        ts, Functions.justFunction(Single.<Integer>never()), 16, ErrorMode.IMMEDIATE);

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

            final SingleSubject<Integer> ss = SingleSubject.create();

            final TestSubscriber<Integer> ts = Flowable.just(1)
                    .hide()
                    .concatMapSingle(Functions.justFunction(ss))
                    .test();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    ss.onSuccess(1);
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
                return upstream.concatMapSingle(new Function<Integer, Single<Integer>>() {
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
                return upstream.concatMapSingleDelayError(new Function<Integer, Single<Integer>>() {
                    @Override
                    public Single<Integer> apply(Integer v) throws Throwable {
                        return Single.just(v).hide();
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
                return upstream.concatMapSingleDelayError(new Function<Integer, Single<Integer>>() {
                    @Override
                    public Single<Integer> apply(Integer v) throws Throwable {
                        return Single.just(v).hide();
                    }
                }, true, 2);
            }
        });
    }

    @Test
    public void basicNonFused() {
        Flowable.range(1, 5).hide()
        .concatMapSingle(v -> Single.just(v).hide())
        .test()
        .assertResult(1, 2, 3, 4, 5);
    }

    @Test
    public void basicSyncFused() {
        Flowable.range(1, 5)
        .concatMapSingle(v -> Single.just(v).hide())
        .test()
        .assertResult(1, 2, 3, 4, 5);
    }

    @Test
    public void basicAsyncFused() {
        UnicastProcessor<Integer> up = UnicastProcessor.create();
        TestHelper.emit(up, 1, 2, 3, 4, 5);

        up
        .concatMapSingle(v -> Single.just(v).hide())
        .test()
        .assertResult(1, 2, 3, 4, 5);
    }

    @Test
    public void basicFusionRejected() {
        TestHelper.<Integer>rejectFlowableFusion()
        .concatMapSingle(v -> Single.just(v).hide())
        .test()
        .assertEmpty();
    }

    @Test
    public void fusedPollCrash() {
        Flowable.range(1, 5)
        .map(v -> {
            if (v == 3) {
                throw new TestException();
            }
            return v;
        })
        .compose(TestHelper.flowableStripBoundary())
        .concatMapSingle(v -> Single.just(v).hide())
        .test()
        .assertFailure(TestException.class, 1, 2);
    }
}
