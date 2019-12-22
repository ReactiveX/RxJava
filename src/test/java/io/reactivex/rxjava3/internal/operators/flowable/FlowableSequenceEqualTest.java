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

import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.mockito.InOrder;
import org.reactivestreams.Subscriber;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.exceptions.*;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.internal.subscriptions.BooleanSubscription;
import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.processors.PublishProcessor;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subscribers.TestSubscriber;
import io.reactivex.rxjava3.testsupport.TestHelper;

public class FlowableSequenceEqualTest extends RxJavaTest {

    @Test
    public void flowable1() {
        Flowable<Boolean> flowable = Flowable.sequenceEqual(
                Flowable.just("one", "two", "three"),
                Flowable.just("one", "two", "three")).toFlowable();
        verifyResult(flowable, true);
    }

    @Test
    public void flowable2() {
        Flowable<Boolean> flowable = Flowable.sequenceEqual(
                Flowable.just("one", "two", "three"),
                Flowable.just("one", "two", "three", "four")).toFlowable();
        verifyResult(flowable, false);
    }

    @Test
    public void flowable3() {
        Flowable<Boolean> flowable = Flowable.sequenceEqual(
                Flowable.just("one", "two", "three", "four"),
                Flowable.just("one", "two", "three")).toFlowable();
        verifyResult(flowable, false);
    }

    @Test
    public void withError1Flowable() {
        Flowable<Boolean> flowable = Flowable.sequenceEqual(
                Flowable.concat(Flowable.just("one"),
                        Flowable.<String> error(new TestException())),
                Flowable.just("one", "two", "three")).toFlowable();
        verifyError(flowable);
    }

    @Test
    public void withError2Flowable() {
        Flowable<Boolean> flowable = Flowable.sequenceEqual(
                Flowable.just("one", "two", "three"),
                Flowable.concat(Flowable.just("one"),
                        Flowable.<String> error(new TestException()))).toFlowable();
        verifyError(flowable);
    }

    @Test
    public void withError3Flowable() {
        Flowable<Boolean> flowable = Flowable.sequenceEqual(
                Flowable.concat(Flowable.just("one"),
                        Flowable.<String> error(new TestException())),
                Flowable.concat(Flowable.just("one"),
                        Flowable.<String> error(new TestException()))).toFlowable();
        verifyError(flowable);
    }

    @Test
    public void withEmpty1Flowable() {
        Flowable<Boolean> flowable = Flowable.sequenceEqual(
                Flowable.<String> empty(),
                Flowable.just("one", "two", "three")).toFlowable();
        verifyResult(flowable, false);
    }

    @Test
    public void withEmpty2Flowable() {
        Flowable<Boolean> flowable = Flowable.sequenceEqual(
                Flowable.just("one", "two", "three"),
                Flowable.<String> empty()).toFlowable();
        verifyResult(flowable, false);
    }

    @Test
    public void withEmpty3Flowable() {
        Flowable<Boolean> flowable = Flowable.sequenceEqual(
                Flowable.<String> empty(), Flowable.<String> empty()).toFlowable();
        verifyResult(flowable, true);
    }

    @Test
    public void withEqualityErrorFlowable() {
        Flowable<Boolean> flowable = Flowable.sequenceEqual(
                Flowable.just("one"), Flowable.just("one"),
                new BiPredicate<String, String>() {
                    @Override
                    public boolean test(String t1, String t2) {
                        throw new TestException();
                    }
                }).toFlowable();
        verifyError(flowable);
    }

    @Test
    public void one() {
        Single<Boolean> single = Flowable.sequenceEqual(
                Flowable.just("one", "two", "three"),
                Flowable.just("one", "two", "three"));
        verifyResult(single, true);
    }

    @Test
    public void two() {
        Single<Boolean> single = Flowable.sequenceEqual(
                Flowable.just("one", "two", "three"),
                Flowable.just("one", "two", "three", "four"));
        verifyResult(single, false);
    }

    @Test
    public void three() {
        Single<Boolean> single = Flowable.sequenceEqual(
                Flowable.just("one", "two", "three", "four"),
                Flowable.just("one", "two", "three"));
        verifyResult(single, false);
    }

    @Test
    public void withError1() {
        Single<Boolean> single = Flowable.sequenceEqual(
                Flowable.concat(Flowable.just("one"),
                        Flowable.<String> error(new TestException())),
                Flowable.just("one", "two", "three"));
        verifyError(single);
    }

    @Test
    public void withError2() {
        Single<Boolean> single = Flowable.sequenceEqual(
                Flowable.just("one", "two", "three"),
                Flowable.concat(Flowable.just("one"),
                        Flowable.<String> error(new TestException())));
        verifyError(single);
    }

    @Test
    public void withError3() {
        Single<Boolean> single = Flowable.sequenceEqual(
                Flowable.concat(Flowable.just("one"),
                        Flowable.<String> error(new TestException())),
                Flowable.concat(Flowable.just("one"),
                        Flowable.<String> error(new TestException())));
        verifyError(single);
    }

    @Test
    public void withEmpty1() {
        Single<Boolean> single = Flowable.sequenceEqual(
                Flowable.<String> empty(),
                Flowable.just("one", "two", "three"));
        verifyResult(single, false);
    }

    @Test
    public void withEmpty2() {
        Single<Boolean> single = Flowable.sequenceEqual(
                Flowable.just("one", "two", "three"),
                Flowable.<String> empty());
        verifyResult(single, false);
    }

    @Test
    public void withEmpty3() {
        Single<Boolean> single = Flowable.sequenceEqual(
                Flowable.<String> empty(), Flowable.<String> empty());
        verifyResult(single, true);
    }

    @Test
    public void withEqualityError() {
        Single<Boolean> single = Flowable.sequenceEqual(
                Flowable.just("one"), Flowable.just("one"),
                new BiPredicate<String, String>() {
                    @Override
                    public boolean test(String t1, String t2) {
                        throw new TestException();
                    }
                });
        verifyError(single);
    }

    private void verifyResult(Flowable<Boolean> flowable, boolean result) {
        Subscriber<Boolean> subscriber = TestHelper.mockSubscriber();

        flowable.subscribe(subscriber);

        InOrder inOrder = inOrder(subscriber);
        inOrder.verify(subscriber, times(1)).onNext(result);
        inOrder.verify(subscriber).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    private void verifyResult(Single<Boolean> single, boolean result) {
        SingleObserver<Boolean> observer = TestHelper.mockSingleObserver();

        single.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onSuccess(result);
        inOrder.verifyNoMoreInteractions();
    }

    private void verifyError(Flowable<Boolean> flowable) {
        Subscriber<Boolean> subscriber = TestHelper.mockSubscriber();
        flowable.subscribe(subscriber);

        InOrder inOrder = inOrder(subscriber);
        inOrder.verify(subscriber, times(1)).onError(isA(TestException.class));
        inOrder.verifyNoMoreInteractions();
    }

    private void verifyError(Single<Boolean> single) {
        SingleObserver<Boolean> observer = TestHelper.mockSingleObserver();
        single.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onError(isA(TestException.class));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void prefetch() {

        Flowable.sequenceEqual(Flowable.range(1, 20), Flowable.range(1, 20), 2)
        .test()
        .assertResult(true);
    }

    @Test
    public void disposed() {
        TestHelper.checkDisposed(Flowable.sequenceEqual(Flowable.just(1), Flowable.just(2)));
    }

    @Test
    public void simpleInequal() {
        Flowable.sequenceEqual(Flowable.just(1), Flowable.just(2))
        .test()
        .assertResult(false);
    }

    @Test
    public void simpleInequalObservable() {
        Flowable.sequenceEqual(Flowable.just(1), Flowable.just(2))
        .toFlowable()
        .test()
        .assertResult(false);
    }

    @Test
    public void onNextCancelRace() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            final PublishProcessor<Integer> pp = PublishProcessor.create();

            final TestObserver<Boolean> to = Flowable.sequenceEqual(Flowable.never(), pp).test();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    to.dispose();
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    pp.onNext(1);
                }
            };

            TestHelper.race(r1, r2);

            to.assertEmpty();
        }
    }

    @Test
    public void onNextCancelRaceObservable() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            final PublishProcessor<Integer> pp = PublishProcessor.create();

            final TestSubscriber<Boolean> ts = Flowable.sequenceEqual(Flowable.never(), pp).toFlowable().test();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    ts.cancel();
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    pp.onNext(1);
                }
            };

            TestHelper.race(r1, r2);

            ts.assertEmpty();
        }
    }

    @Test
    public void disposedFlowable() {
        TestHelper.checkDisposed(Flowable.sequenceEqual(Flowable.just(1), Flowable.just(2)).toFlowable());
    }

    @Test
    public void prefetchFlowable() {
        Flowable.sequenceEqual(Flowable.range(1, 20), Flowable.range(1, 20), 2)
        .toFlowable()
        .test()
        .assertResult(true);
    }

    @Test
    public void longSequenceEqualsFlowable() {
        Flowable<Integer> source = Flowable.range(1, Flowable.bufferSize() * 4).subscribeOn(Schedulers.computation());

        Flowable.sequenceEqual(source, source)
        .toFlowable()
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(true);
    }

    @Test
    public void syncFusedCrashFlowable() {
        Flowable<Integer> source = Flowable.range(1, 10).map(new Function<Integer, Integer>() {
            @Override
            public Integer apply(Integer v) throws Exception { throw new TestException(); }
        });

        Flowable.sequenceEqual(source, Flowable.range(1, 10).hide())
        .toFlowable()
        .test()
        .assertFailure(TestException.class);

        Flowable.sequenceEqual(Flowable.range(1, 10).hide(), source)
        .toFlowable()
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void cancelAndDrainRaceFlowable() {
        Flowable<Object> neverNever = new Flowable<Object>() {
            @Override
            protected void subscribeActual(Subscriber<? super Object> s) {
            }
        };

        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            final TestSubscriber<Boolean> ts = new TestSubscriber<>();

            final PublishProcessor<Integer> pp = PublishProcessor.create();

            boolean swap = (i & 1) == 0;

            Flowable.sequenceEqual(swap ? pp : neverNever, swap ? neverNever : pp)
            .toFlowable()
            .subscribe(ts);

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

            ts.assertEmpty();
        }
    }

    @Test
    public void sourceOverflowsFlowable() {
        Flowable.sequenceEqual(Flowable.never(), new Flowable<Object>() {
            @Override
            protected void subscribeActual(Subscriber<? super Object> s) {
                s.onSubscribe(new BooleanSubscription());
                for (int i = 0; i < 10; i++) {
                    s.onNext(i);
                }
            }
        }, 8)
        .toFlowable()
        .test()
        .assertFailure(MissingBackpressureException.class);
    }

    @Test
    public void doubleErrorFlowable() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            Flowable.sequenceEqual(Flowable.never(), new Flowable<Object>() {
                @Override
                protected void subscribeActual(Subscriber<? super Object> s) {
                    s.onSubscribe(new BooleanSubscription());
                    s.onError(new TestException("First"));
                    s.onError(new TestException("Second"));
                }
            }, 8)
            .toFlowable()
            .to(TestHelper.<Boolean>testConsumer())
            .assertFailureAndMessage(TestException.class, "First");

            TestHelper.assertUndeliverable(errors, 0, TestException.class, "Second");
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void longSequenceEquals() {
        Flowable<Integer> source = Flowable.range(1, Flowable.bufferSize() * 4).subscribeOn(Schedulers.computation());

        Flowable.sequenceEqual(source, source)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(true);
    }

    @Test
    public void syncFusedCrash() {
        Flowable<Integer> source = Flowable.range(1, 10).map(new Function<Integer, Integer>() {
            @Override
            public Integer apply(Integer v) throws Exception { throw new TestException(); }
        });

        Flowable.sequenceEqual(source, Flowable.range(1, 10).hide())
        .test()
        .assertFailure(TestException.class);

        Flowable.sequenceEqual(Flowable.range(1, 10).hide(), source)
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void cancelAndDrainRace() {
        Flowable<Object> neverNever = new Flowable<Object>() {
            @Override
            protected void subscribeActual(Subscriber<? super Object> s) {
            }
        };

        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            final TestObserver<Boolean> to = new TestObserver<>();

            final PublishProcessor<Integer> pp = PublishProcessor.create();

            boolean swap = (i & 1) == 0;

            Flowable.sequenceEqual(swap ? pp : neverNever, swap ? neverNever : pp)
            .subscribe(to);

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

            to.assertEmpty();
        }
    }

    @Test
    public void sourceOverflows() {
        Flowable.sequenceEqual(Flowable.never(), new Flowable<Object>() {
            @Override
            protected void subscribeActual(Subscriber<? super Object> s) {
                s.onSubscribe(new BooleanSubscription());
                for (int i = 0; i < 10; i++) {
                    s.onNext(i);
                }
            }
        }, 8)
        .test()
        .assertFailure(MissingBackpressureException.class);
    }

    @Test
    public void doubleError() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            Flowable.sequenceEqual(Flowable.never(), new Flowable<Object>() {
                @Override
                protected void subscribeActual(Subscriber<? super Object> s) {
                    s.onSubscribe(new BooleanSubscription());
                    s.onError(new TestException("First"));
                    s.onError(new TestException("Second"));
                }
            }, 8)
            .to(TestHelper.<Boolean>testConsumer())
            .assertFailureAndMessage(TestException.class, "First");

            TestHelper.assertUndeliverable(errors, 0, TestException.class, "Second");
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void undeliverableUponCancel() {
        TestHelper.checkUndeliverableUponCancel(new FlowableConverter<Integer, Single<Boolean>>() {
            @Override
            public Single<Boolean> apply(Flowable<Integer> upstream) {
                return Flowable.sequenceEqual(Flowable.just(1).hide(), upstream);
            }
        });
    }

    @Test
    public void undeliverableUponCancelAsFlowable() {
        TestHelper.checkUndeliverableUponCancel(new FlowableConverter<Integer, Flowable<Boolean>>() {
            @Override
            public Flowable<Boolean> apply(Flowable<Integer> upstream) {
                return Flowable.sequenceEqual(Flowable.just(1).hide(), upstream).toFlowable();
            }
        });
    }

    @Test
    public void undeliverableUponCancel2() {
        TestHelper.checkUndeliverableUponCancel(new FlowableConverter<Integer, Single<Boolean>>() {
            @Override
            public Single<Boolean> apply(Flowable<Integer> upstream) {
                return Flowable.sequenceEqual(upstream, Flowable.just(1).hide());
            }
        });
    }

    @Test
    public void undeliverableUponCancelAsFlowable2() {
        TestHelper.checkUndeliverableUponCancel(new FlowableConverter<Integer, Flowable<Boolean>>() {
            @Override
            public Flowable<Boolean> apply(Flowable<Integer> upstream) {
                return Flowable.sequenceEqual(upstream, Flowable.just(1).hide()).toFlowable();
            }
        });
    }
}
