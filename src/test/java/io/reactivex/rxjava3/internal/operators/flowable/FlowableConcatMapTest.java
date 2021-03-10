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

package io.reactivex.rxjava3.internal.operators.flowable;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;
import org.reactivestreams.Publisher;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.exceptions.*;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.internal.operators.flowable.FlowableConcatMap.SimpleScalarSubscription;
import io.reactivex.rxjava3.processors.*;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subscribers.TestSubscriber;
import io.reactivex.rxjava3.testsupport.TestHelper;

public class FlowableConcatMapTest extends RxJavaTest {

    @Test
    public void simpleSubscriptionRequest() {
        TestSubscriber<Integer> ts = new TestSubscriber<>(0);
        SimpleScalarSubscription<Integer> ws = new SimpleScalarSubscription<>(1, ts);
        ts.onSubscribe(ws);

        ws.request(0);

        ts.assertEmpty();

        ws.request(1);

        ts.assertResult(1);

        ws.request(1);

        ts.assertResult(1);
    }

    @Test
    public void boundaryFusion() {
        Flowable.range(1, 10000)
        .observeOn(Schedulers.single())
        .map(t -> {
            String name = Thread.currentThread().getName();
            if (name.contains("RxSingleScheduler")) {
                return "RxSingleScheduler";
            }
            return name;
        })
        .concatMap((Function<String, Publisher<?>>) Flowable::just)
        .observeOn(Schedulers.computation())
        .distinct()
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult("RxSingleScheduler");
    }

    @Test
    public void innerScalarRequestRace() {
        Flowable<Integer> just = Flowable.just(1);
        int n = 1000;
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            PublishProcessor<Flowable<Integer>> source = PublishProcessor.create();

            TestSubscriber<Integer> ts = source
                    .concatMap(v -> v, n + 1)
                    .test(1L);

            TestHelper.race(() -> {
                for (int j = 0; j < n; j++) {
                    source.onNext(just);
                }
            }, () -> {
                for (int j = 0; j < n; j++) {
                    ts.request(1);
                }
            });

            ts.assertValueCount(n);
        }
    }

    @Test
    public void innerScalarRequestRaceDelayError() {
        Flowable<Integer> just = Flowable.just(1);
        int n = 1000;
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            PublishProcessor<Flowable<Integer>> source = PublishProcessor.create();

            TestSubscriber<Integer> ts = source
                    .concatMapDelayError(v -> v, true, n + 1)
                    .test(1L);

            TestHelper.race(() -> {
                for (int j = 0; j < n; j++) {
                    source.onNext(just);
                }
            }, () -> {
                for (int j = 0; j < n; j++) {
                    ts.request(1);
                }
            });

            ts.assertValueCount(n);
        }
    }

    @Test
    public void boundaryFusionDelayError() {
        Flowable.range(1, 10000)
        .observeOn(Schedulers.single())
        .map(t -> {
            String name = Thread.currentThread().getName();
            if (name.contains("RxSingleScheduler")) {
                return "RxSingleScheduler";
            }
            return name;
        })
        .concatMapDelayError((Function<String, Publisher<?>>) Flowable::just)
        .observeOn(Schedulers.computation())
        .distinct()
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult("RxSingleScheduler");
    }

    @Test
    public void pollThrows() {
        Flowable.just(1)
        .map((Function<Integer, Integer>) v -> {
            throw new TestException();
        })
        .compose(TestHelper.flowableStripBoundary())
        .concatMap((Function<Integer, Publisher<Integer>>) Flowable::just)
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void pollThrowsDelayError() {
        Flowable.just(1)
        .map((Function<Integer, Integer>) v -> {
            throw new TestException();
        })
        .compose(TestHelper.flowableStripBoundary())
        .concatMapDelayError((Function<Integer, Publisher<Integer>>) Flowable::just)
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void noCancelPrevious() {
        final AtomicInteger counter = new AtomicInteger();

        Flowable.range(1, 5)
        .concatMap((Function<Integer, Flowable<Integer>>) v -> Flowable.just(v).doOnCancel(counter::getAndIncrement))
        .test()
        .assertResult(1, 2, 3, 4, 5);

        assertEquals(0, counter.get());
    }

    @Test
    public void delayErrorCallableTillTheEnd() {
        Flowable.just(1, 2, 3, 101, 102, 23, 890, 120, 32)
        .concatMapDelayError((Function<Integer, Flowable<Integer>>) integer -> Flowable.fromCallable(() -> {
            if (integer >= 100) {
                throw new NullPointerException("test null exp");
            }
            return integer;
        }))
        .test()
        .assertFailure(CompositeException.class, 1, 2, 3, 23, 32);
    }

    @Test
    public void delayErrorCallableEager() {
        Flowable.just(1, 2, 3, 101, 102, 23, 890, 120, 32)
        .concatMapDelayError((Function<Integer, Flowable<Integer>>) integer -> Flowable.fromCallable(() -> {
            if (integer >= 100) {
                throw new NullPointerException("test null exp");
            }
            return integer;
        }), false, 2)
        .test()
        .assertFailure(NullPointerException.class, 1, 2, 3);
    }

    @Test
    public void undeliverableUponCancel() {
        TestHelper.checkUndeliverableUponCancel((FlowableConverter<Integer, Flowable<Integer>>) upstream -> upstream.concatMap((Function<Integer, Publisher<Integer>>) v -> Flowable.just(v).hide()));
    }

    @Test
    public void undeliverableUponCancelDelayError() {
        TestHelper.checkUndeliverableUponCancel((FlowableConverter<Integer, Flowable<Integer>>) upstream -> upstream.concatMapDelayError((Function<Integer, Publisher<Integer>>) v -> Flowable.just(v).hide(), false, 2));
    }

    @Test
    public void undeliverableUponCancelDelayErrorTillEnd() {
        TestHelper.checkUndeliverableUponCancel((FlowableConverter<Integer, Flowable<Integer>>) upstream -> upstream.concatMapDelayError((Function<Integer, Publisher<Integer>>) v -> Flowable.just(v).hide(), true, 2));
    }

    @Test
    public void asyncFusedSource() {
        UnicastProcessor<Integer> up = UnicastProcessor.create();
        up.onNext(1);
        up.onComplete();

        up.concatMap(v -> Flowable.just(1).hide())
        .test()
        .assertResult(1);
    }

    @Test
    public void scalarCallableSource() {
        Flowable.fromCallable(() -> 1)
        .concatMap(v -> Flowable.just(1))
        .test()
        .assertResult(1);
    }
}
