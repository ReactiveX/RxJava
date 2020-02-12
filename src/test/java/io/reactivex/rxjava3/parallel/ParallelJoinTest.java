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

package io.reactivex.rxjava3.parallel;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;
import org.reactivestreams.Subscriber;

import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.exceptions.*;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.internal.subscriptions.BooleanSubscription;
import io.reactivex.rxjava3.processors.PublishProcessor;
import io.reactivex.rxjava3.subscribers.TestSubscriber;
import io.reactivex.rxjava3.testsupport.*;

public class ParallelJoinTest extends RxJavaTest {

    @Test
    public void overflowFastpath() {
        new ParallelFlowable<Integer>() {
            @Override
            public void subscribe(Subscriber<? super Integer>[] subscribers) {
                subscribers[0].onSubscribe(new BooleanSubscription());
                subscribers[0].onNext(1);
                subscribers[0].onNext(2);
                subscribers[0].onNext(3);
            }

            @Override
            public int parallelism() {
                return 1;
            }
        }
        .sequential(1)
        .test(0)
        .assertFailure(MissingBackpressureException.class);
    }

    @Test
    public void overflowSlowpath() {
        @SuppressWarnings("unchecked")
        final Subscriber<? super Integer>[] subs = new Subscriber[1];

        TestSubscriber<Integer> ts = new TestSubscriber<Integer>(1) {
            @Override
            public void onNext(Integer t) {
                super.onNext(t);
                subs[0].onNext(2);
                subs[0].onNext(3);
            }
        };

        new ParallelFlowable<Integer>() {
            @Override
            public void subscribe(Subscriber<? super Integer>[] subscribers) {
                subs[0] = subscribers[0];
                subscribers[0].onSubscribe(new BooleanSubscription());
                subscribers[0].onNext(1);
            }

            @Override
            public int parallelism() {
                return 1;
            }
        }
        .sequential(1)
        .subscribe(ts);

        ts.assertFailure(MissingBackpressureException.class, 1);
    }

    @Test
    public void emptyBackpressured() {
        Flowable.empty()
        .parallel()
        .sequential()
        .test(0)
        .assertResult();
    }

    @Test
    public void overflowFastpathDelayError() {
        new ParallelFlowable<Integer>() {
            @Override
            public void subscribe(Subscriber<? super Integer>[] subscribers) {
                subscribers[0].onSubscribe(new BooleanSubscription());
                subscribers[0].onNext(1);
                subscribers[0].onNext(2);
            }

            @Override
            public int parallelism() {
                return 1;
            }
        }
        .sequentialDelayError(1)
        .test(0)
        .requestMore(1)
        .assertFailure(MissingBackpressureException.class, 1);
    }

    @Test
    public void overflowSlowpathDelayError() {
        @SuppressWarnings("unchecked")
        final Subscriber<? super Integer>[] subs = new Subscriber[1];

        TestSubscriber<Integer> ts = new TestSubscriber<Integer>(1) {
            @Override
            public void onNext(Integer t) {
                super.onNext(t);
                if (t == 1) {
                    subs[0].onNext(2);
                    subs[0].onNext(3);
                }
            }
        };

        new ParallelFlowable<Integer>() {
            @Override
            public void subscribe(Subscriber<? super Integer>[] subscribers) {
                subs[0] = subscribers[0];
                subscribers[0].onSubscribe(new BooleanSubscription());
                subscribers[0].onNext(1);
            }

            @Override
            public int parallelism() {
                return 1;
            }
        }
        .sequentialDelayError(1)
        .subscribe(ts);

        ts.request(1);

        ts.assertFailure(MissingBackpressureException.class, 1, 2);
    }

    @Test
    public void emptyBackpressuredDelayError() {
        Flowable.empty()
        .parallel()
        .sequentialDelayError()
        .test(0)
        .assertResult();
    }

    @Test
    public void delayError() {
        TestSubscriberEx<Integer> flow = Flowable.range(1, 2)
        .parallel(2)
        .map(new Function<Integer, Integer>() {
            @Override
            public Integer apply(Integer v) throws Exception {
                throw new TestException();
            }
        })
        .sequentialDelayError()
        .to(TestHelper.<Integer>testConsumer())
        .assertFailure(CompositeException.class);

        List<Throwable> error = TestHelper.errorList(flow);
        TestHelper.assertError(error, 0, TestException.class);
        TestHelper.assertError(error, 1, TestException.class);
    }

    @Test
    public void normalDelayError() {
        Flowable.just(1)
        .parallel(1)
        .sequentialDelayError(1)
        .test()
        .assertResult(1);
    }

    @Test
    public void rangeDelayError() {
        Flowable.range(1, 2)
        .parallel(1)
        .sequentialDelayError(1)
        .take(1)
        .test()
        .assertResult(1);
    }

    @Test
    public void rangeDelayErrorBackpressure() {
        Flowable.range(1, 3)
        .parallel(1)
        .sequentialDelayError(1)
        .take(2)
        .rebatchRequests(1)
        .test()
        .assertResult(1, 2);
    }

    @Test
    public void rangeDelayErrorBackpressure2() {
        Flowable.range(1, 3)
        .parallel(1)
        .sequentialDelayError(1)
        .rebatchRequests(1)
        .test()
        .assertResult(1, 2, 3);
    }

    @Test
    public void delayErrorCancelBackpressured() {
        TestSubscriber<Integer> ts = Flowable.range(1, 3)
        .parallel(1)
        .sequentialDelayError(1)
        .test(0);

        ts
        .cancel();

        ts.assertEmpty();
    }

    @Test
    public void delayErrorCancelBackpressured2() {
        TestSubscriber<Integer> ts = Flowable.<Integer>empty()
        .parallel(1)
        .sequentialDelayError(1)
        .test(0);

        ts.assertResult();
    }

    @Test
    public void consumerCancelsAfterOne() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>(1) {
            @Override
            public void onNext(Integer t) {
                super.onNext(t);
                cancel();
                onComplete();
            }
        };

        Flowable.range(1, 3)
        .parallel(1)
        .sequential()
        .subscribe(ts);

        ts.assertResult(1);
    }

    @Test
    public void delayErrorConsumerCancelsAfterOne() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>(1) {
            @Override
            public void onNext(Integer t) {
                super.onNext(t);
                cancel();
                onComplete();
            }
        };

        Flowable.range(1, 3)
        .parallel(1)
        .sequentialDelayError()
        .subscribe(ts);

        ts.assertResult(1);
    }

    @Test
    public void delayErrorDrainTrigger() {
        Flowable.range(1, 3)
        .parallel(1)
        .sequentialDelayError()
        .test(0)
        .requestMore(1)
        .assertValues(1)
        .requestMore(1)
        .assertValues(1, 2)
        .requestMore(1)
        .assertResult(1, 2, 3);
    }

    @Test
    public void failedRailIsIgnored() {
        Flowable.range(1, 4)
        .parallel(2)
        .map(new Function<Integer, Integer>() {
            @Override
            public Integer apply(Integer v) throws Exception {
                if (v == 1) {
                    throw new TestException();
                }
                return v;
            }
        })
        .sequentialDelayError()
        .test()
        .assertFailure(TestException.class, 2, 3, 4);
    }

    @Test
    public void failedRailIsIgnoredHidden() {
        Flowable.range(1, 4).hide()
        .parallel(2)
        .map(new Function<Integer, Integer>() {
            @Override
            public Integer apply(Integer v) throws Exception {
                if (v == 1) {
                    throw new TestException();
                }
                return v;
            }
        })
        .sequentialDelayError()
        .test()
        .assertFailure(TestException.class, 2, 3, 4);
    }

    @Test
    public void takeUntil() {
        Flowable.range(1, 10)
        .parallel(1)
        .sequential()
        .takeUntil(v -> true)
        .test(0L)
        .requestMore(100)
        .assertResult(1);
    }

    @Test
    public void takeUntilDelayError() {
        Flowable.range(1, 10)
        .parallel(1)
        .sequentialDelayError()
        .takeUntil(v -> true)
        .test(0L)
        .requestMore(100)
        .assertResult(1);
    }

    @Test
    public void oneItemNext() {
        PublishProcessor<Integer> pp = PublishProcessor.create();

        TestSubscriber<Integer> ts = pp.parallel(1)
        .sequential()
        .test(0L);

        pp.onNext(1);

        ts.requestMore(10)
        .assertValuesOnly(1);
    }

    @Test
    public void delayErrorOneItemNext() {
        PublishProcessor<Integer> pp = PublishProcessor.create();

        TestSubscriber<Integer> ts = pp.parallel(1)
        .sequentialDelayError()
        .test(0L);

        pp.onNext(1);

        ts.requestMore(10)
        .assertValuesOnly(1);
    }

    @Test
    public void onNextWhileProcessingSlowPath() {
        PublishProcessor<Integer> pp = PublishProcessor.create();

        TestSubscriber<Integer> ts = new TestSubscriber<Integer>() {
            @Override
            public void onNext(@NonNull Integer t) {
                super.onNext(t);
                if (t == 1) {
                    pp.onNext(2);
                }
            }
        };

        ParallelFlowable.fromArray(pp)
        .sequential()
        .subscribeWith(ts);

        pp.onNext(1);

        ts
        .assertValuesOnly(1, 2);
    }

    @Test
    public void delayErrorOnNextWhileProcessingSlowPath() {
        PublishProcessor<Integer> pp = PublishProcessor.create();

        TestSubscriber<Integer> ts = new TestSubscriber<Integer>() {
            @Override
            public void onNext(@NonNull Integer t) {
                super.onNext(t);
                if (t == 1) {
                    pp.onNext(2);
                }
            }
        };

        ParallelFlowable.fromArray(pp)
        .sequentialDelayError()
        .subscribeWith(ts);

        pp.onNext(1);

        ts
        .assertValuesOnly(1, 2);
    }

    @Test
    public void badRequest() {
        TestHelper.assertBadRequestReported(
                ParallelFlowable.fromArray(PublishProcessor.create())
                .sequential()
        );
    }

    @Test
    public void onNextMissingBackpressureRace() throws Throwable {
        TestHelper.withErrorTracking(errors -> {
            for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {

                AtomicReference<Subscriber<? super Integer>> ref1 = new AtomicReference<>();
                AtomicReference<Subscriber<? super Integer>> ref2 = new AtomicReference<>();

                Flowable<Integer> f1 = new Flowable<Integer>() {
                    @Override
                    public void subscribeActual(Subscriber<? super Integer> s) {
                        s.onSubscribe(new BooleanSubscription());
                        ref1.set(s);
                    }
                };
                Flowable<Integer> f2 = new Flowable<Integer>() {
                    @Override
                    public void subscribeActual(Subscriber<? super Integer> s) {
                        s.onSubscribe(new BooleanSubscription());
                        ref2.set(s);
                    }
                };

                ParallelFlowable.fromArray(f1, f2)
                .sequential(1)
                .test(0)
                ;

                TestHelper.race(
                        () -> {
                            ref1.get().onNext(1);
                            ref1.get().onNext(2);
                        },
                        () -> {
                            ref2.get().onNext(3);
                            ref2.get().onNext(4);
                        }
                );

                errors.clear();
            }
        });
    }

    @Test
    public void onNextMissingBackpressureDelayErrorRace() throws Throwable {
        TestHelper.withErrorTracking(errors -> {
            for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {

                AtomicReference<Subscriber<? super Integer>> ref1 = new AtomicReference<>();
                AtomicReference<Subscriber<? super Integer>> ref2 = new AtomicReference<>();

                Flowable<Integer> f1 = new Flowable<Integer>() {
                    @Override
                    public void subscribeActual(Subscriber<? super Integer> s) {
                        s.onSubscribe(new BooleanSubscription());
                        ref1.set(s);
                    }
                };
                Flowable<Integer> f2 = new Flowable<Integer>() {
                    @Override
                    public void subscribeActual(Subscriber<? super Integer> s) {
                        s.onSubscribe(new BooleanSubscription());
                        ref2.set(s);
                    }
                };

                ParallelFlowable.fromArray(f1, f2)
                .sequentialDelayError(1)
                .test(0)
                ;

                TestHelper.race(
                        () -> {
                            ref1.get().onNext(1);
                            ref1.get().onNext(2);
                        },
                        () -> {
                            ref2.get().onNext(3);
                            ref2.get().onNext(4);
                        }
                );

                errors.clear();
            }
        });
    }
}
