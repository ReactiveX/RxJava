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

package io.reactivex.rxjava3.parallel;

import static org.junit.Assert.*;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.reactivestreams.Subscriber;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.exceptions.*;
import io.reactivex.rxjava3.functions.Predicate;
import io.reactivex.rxjava3.internal.functions.Functions;
import io.reactivex.rxjava3.internal.schedulers.ImmediateThinScheduler;
import io.reactivex.rxjava3.internal.subscriptions.BooleanSubscription;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.processors.PublishProcessor;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subscribers.TestSubscriber;
import io.reactivex.rxjava3.testsupport.TestHelper;

public class ParallelRunOnTest extends RxJavaTest {

    @Test
    public void subscriberCount() {
        ParallelFlowableTest.checkSubscriberCount(Flowable.range(1, 5).parallel()
        .runOn(Schedulers.computation()));
    }

    @Test
    public void doubleError() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            new ParallelInvalid()
            .runOn(ImmediateThinScheduler.INSTANCE)
            .sequential()
            .test()
            .assertFailure(TestException.class);

            assertFalse(errors.isEmpty());
            for (Throwable ex : errors) {
                assertTrue(ex.toString(), ex.getCause() instanceof TestException);
            }
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void conditionalPath() {
        Flowable.range(1, 1000)
        .parallel(2)
        .runOn(Schedulers.computation())
        .filter(new Predicate<Integer>() {
            @Override
            public boolean test(Integer v) throws Exception {
                return v % 2 == 0;
            }
        })
        .sequential()
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertValueCount(500)
        .assertNoErrors()
        .assertComplete();
    }

    @Test
    public void missingBackpressure() {
        new ParallelFlowable<Integer>() {
            @Override
            public int parallelism() {
                return 1;
            }

            @Override
            public void subscribe(Subscriber<? super Integer>[] subscribers) {
                subscribers[0].onSubscribe(new BooleanSubscription());
                subscribers[0].onNext(1);
                subscribers[0].onNext(2);
                subscribers[0].onNext(3);
            }
        }
        .runOn(ImmediateThinScheduler.INSTANCE, 1)
        .sequential(1)
        .test(0)
        .assertFailure(MissingBackpressureException.class);
    }

    @Test
    public void error() {
        Flowable.error(new TestException())
        .parallel(1)
        .runOn(ImmediateThinScheduler.INSTANCE)
        .sequential()
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void errorBackpressured() {
        Flowable.error(new TestException())
        .parallel(1)
        .runOn(ImmediateThinScheduler.INSTANCE)
        .sequential(1)
        .test(0)
        .assertFailure(TestException.class);
    }

    @Test
    public void errorConditional() {
        Flowable.error(new TestException())
        .parallel(1)
        .runOn(ImmediateThinScheduler.INSTANCE)
        .filter(Functions.alwaysTrue())
        .sequential()
        .test()
        .assertFailure(TestException.class);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void errorConditionalBackpressured() {
        TestSubscriber<Object> ts = new TestSubscriber<>(0L);

        Flowable.error(new TestException())
        .parallel(1)
        .runOn(ImmediateThinScheduler.INSTANCE)
        .filter(Functions.alwaysTrue())
        .subscribe(new Subscriber[] { ts });

        ts
        .assertFailure(TestException.class);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void emptyConditionalBackpressured() {
        TestSubscriber<Object> ts = new TestSubscriber<>(0L);

        Flowable.empty()
        .parallel(1)
        .runOn(ImmediateThinScheduler.INSTANCE)
        .filter(Functions.alwaysTrue())
        .subscribe(new Subscriber[] { ts });

        ts
        .assertResult();
    }

    @Test
    public void nextCancelRace() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            final PublishProcessor<Integer> pp = PublishProcessor.create();

            final TestSubscriber<Integer> ts = pp.parallel(1)
            .runOn(Schedulers.computation())
            .sequential()
            .test();

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

    @SuppressWarnings("unchecked")
    @Test
    public void nextCancelRaceBackpressured() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            final PublishProcessor<Integer> pp = PublishProcessor.create();

            final TestSubscriber<Integer> ts = TestSubscriber.create(0L);

            pp.parallel(1)
            .runOn(Schedulers.computation())
            .subscribe(new Subscriber[] { ts });

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
    public void nextCancelRaceConditional() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            final PublishProcessor<Integer> pp = PublishProcessor.create();

            final TestSubscriber<Integer> ts = pp.parallel(1)
            .runOn(Schedulers.computation())
            .filter(Functions.alwaysTrue())
            .sequential()
            .test();

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

    @SuppressWarnings("unchecked")
    @Test
    public void nextCancelRaceBackpressuredConditional() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            final PublishProcessor<Integer> pp = PublishProcessor.create();

            final TestSubscriber<Integer> ts = TestSubscriber.create(0L);

            pp.parallel(1)
            .runOn(Schedulers.computation())
            .filter(Functions.alwaysTrue())
            .subscribe(new Subscriber[] { ts });

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

    @SuppressWarnings("unchecked")
    @Test
    public void normalCancelAfterRequest1() {

        TestSubscriber<Integer> ts = new TestSubscriber<Integer>(1) {
            @Override
            public void onNext(Integer t) {
                super.onNext(t);
                cancel();
                onComplete();
            }
        };

        Flowable.range(1, 5)
        .parallel(1)
        .runOn(ImmediateThinScheduler.INSTANCE)
        .subscribe(new Subscriber[] { ts });

        ts.assertResult(1);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void conditionalCancelAfterRequest1() {

        TestSubscriber<Integer> ts = new TestSubscriber<Integer>(1) {
            @Override
            public void onNext(Integer t) {
                super.onNext(t);
                cancel();
                onComplete();
            }
        };

        Flowable.range(1, 5)
        .parallel(1)
        .runOn(ImmediateThinScheduler.INSTANCE)
        .filter(Functions.alwaysTrue())
        .subscribe(new Subscriber[] { ts });

        ts.assertResult(1);
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeParallel(pf -> pf.runOn(ImmediateThinScheduler.INSTANCE));
    }

    @Test
    public void doubleOnSubscribeConditional() {
        TestHelper.checkDoubleOnSubscribeParallel(pf ->
            pf.runOn(ImmediateThinScheduler.INSTANCE)
            .filter(v -> true)
        );
    }

    @Test
    public void badRequest() {
        TestHelper.assertBadRequestReported(
                ParallelFlowable.fromArray(PublishProcessor.create())
                .runOn(ImmediateThinScheduler.INSTANCE)
        );
    }

    @SuppressWarnings("unchecked")
    @Test
    public void asManyItemsAsRequested() {
        TestSubscriber<Integer> ts = new TestSubscriber<>(0);

        Flowable.range(1, 5)
        .parallel(1)
        .runOn(ImmediateThinScheduler.INSTANCE)
        .subscribe(new Subscriber[] {
                ts
        });

        ts
        .requestMore(5)
        .assertResult(1, 2, 3, 4, 5);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void asManyItemsAsRequestedConditional() {
        TestSubscriber<Integer> ts = new TestSubscriber<>(0);

        Flowable.range(1, 5)
        .parallel(1)
        .runOn(ImmediateThinScheduler.INSTANCE)
        .filter(v -> true)
        .subscribe(new Subscriber[] {
                ts
        });

        ts
        .requestMore(5)
        .assertResult(1, 2, 3, 4, 5);
    }
}
