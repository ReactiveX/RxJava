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

package io.reactivex.rxjava3.internal.subscribers;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;
import org.reactivestreams.*;

import io.reactivex.rxjava3.core.RxJavaTest;
import io.reactivex.rxjava3.disposables.*;
import io.reactivex.rxjava3.exceptions.MissingBackpressureException;
import io.reactivex.rxjava3.internal.queue.SpscArrayQueue;
import io.reactivex.rxjava3.internal.subscriptions.BooleanSubscription;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.subscribers.TestSubscriber;
import io.reactivex.rxjava3.testsupport.TestHelper;

public class QueueDrainSubscriberTest extends RxJavaTest {

    static final QueueDrainSubscriber<Integer, Integer, Integer> createUnordered(TestSubscriber<Integer> ts, final Disposable d) {
        return new QueueDrainSubscriber<Integer, Integer, Integer>(ts, new SpscArrayQueue<>(4)) {
            @Override
            public void onNext(Integer t) {
                fastPathEmitMax(t, false, d);
            }

            @Override
            public void onError(Throwable t) {
            }

            @Override
            public void onComplete() {
            }

            @Override
            public void onSubscribe(Subscription s) {
            }

            @Override
            public boolean accept(Subscriber<? super Integer> a, Integer v) {
                super.accept(a, v);
                a.onNext(v);
                return true;
            }
        };
    }

    static final QueueDrainSubscriber<Integer, Integer, Integer> createOrdered(TestSubscriber<Integer> ts, final Disposable d) {
        return new QueueDrainSubscriber<Integer, Integer, Integer>(ts, new SpscArrayQueue<>(4)) {
            @Override
            public void onNext(Integer t) {
                fastPathOrderedEmitMax(t, false, d);
            }

            @Override
            public void onError(Throwable t) {
            }

            @Override
            public void onComplete() {
            }

            @Override
            public void onSubscribe(Subscription s) {
            }

            @Override
            public boolean accept(Subscriber<? super Integer> a, Integer v) {
                super.accept(a, v);
                a.onNext(v);
                return true;
            }
        };
    }

    static final QueueDrainSubscriber<Integer, Integer, Integer> createUnorderedReject(TestSubscriber<Integer> ts, final Disposable d) {
        return new QueueDrainSubscriber<Integer, Integer, Integer>(ts, new SpscArrayQueue<>(4)) {
            @Override
            public void onNext(Integer t) {
                fastPathEmitMax(t, false, d);
            }

            @Override
            public void onError(Throwable t) {
            }

            @Override
            public void onComplete() {
            }

            @Override
            public void onSubscribe(Subscription s) {
            }

            @Override
            public boolean accept(Subscriber<? super Integer> a, Integer v) {
                super.accept(a, v);
                a.onNext(v);
                return false;
            }
        };
    }

    static final QueueDrainSubscriber<Integer, Integer, Integer> createOrderedReject(TestSubscriber<Integer> ts, final Disposable d) {
        return new QueueDrainSubscriber<Integer, Integer, Integer>(ts, new SpscArrayQueue<>(4)) {
            @Override
            public void onNext(Integer t) {
                fastPathOrderedEmitMax(t, false, d);
            }

            @Override
            public void onError(Throwable t) {
            }

            @Override
            public void onComplete() {
            }

            @Override
            public void onSubscribe(Subscription s) {
            }

            @Override
            public boolean accept(Subscriber<? super Integer> a, Integer v) {
                super.accept(a, v);
                a.onNext(v);
                return false;
            }
        };
    }

    @Test
    public void unorderedFastPathNoRequest() {
        TestSubscriber<Integer> ts = new TestSubscriber<>(0);
        Disposable d = Disposable.empty();
        QueueDrainSubscriber<Integer, Integer, Integer> qd = createUnordered(ts, d);
        ts.onSubscribe(new BooleanSubscription());

        qd.onNext(1);

        ts.assertFailure(MissingBackpressureException.class);

        assertTrue(d.isDisposed());
    }

    @Test
    public void orderedFastPathNoRequest() {
        TestSubscriber<Integer> ts = new TestSubscriber<>(0);
        Disposable d = Disposable.empty();
        QueueDrainSubscriber<Integer, Integer, Integer> qd = createOrdered(ts, d);
        ts.onSubscribe(new BooleanSubscription());

        qd.onNext(1);

        ts.assertFailure(MissingBackpressureException.class);

        assertTrue(d.isDisposed());
    }

    @Test
    public void acceptBadRequest() {
        TestSubscriber<Integer> ts = new TestSubscriber<>(0);
        Disposable d = Disposable.empty();
        QueueDrainSubscriber<Integer, Integer, Integer> qd = createUnordered(ts, d);
        ts.onSubscribe(new BooleanSubscription());

        assertTrue(qd.accept(ts, 0));

        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            qd.requested(-1);
            TestHelper.assertError(errors, 0, IllegalArgumentException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void unorderedFastPathRequest1() {
        TestSubscriber<Integer> ts = new TestSubscriber<>(1);
        Disposable d = Disposable.empty();
        QueueDrainSubscriber<Integer, Integer, Integer> qd = createUnordered(ts, d);
        ts.onSubscribe(new BooleanSubscription());

        qd.requested(1);

        qd.onNext(1);

        ts.assertValuesOnly(1);
    }

    @Test
    public void orderedFastPathRequest1() {
        TestSubscriber<Integer> ts = new TestSubscriber<>(1);
        Disposable d = Disposable.empty();
        QueueDrainSubscriber<Integer, Integer, Integer> qd = createOrdered(ts, d);
        ts.onSubscribe(new BooleanSubscription());

        qd.requested(1);

        qd.onNext(1);

        ts.assertValuesOnly(1);
    }

    @Test
    public void unorderedSlowPath() {
        TestSubscriber<Integer> ts = new TestSubscriber<>(1);
        Disposable d = Disposable.empty();
        QueueDrainSubscriber<Integer, Integer, Integer> qd = createUnordered(ts, d);
        ts.onSubscribe(new BooleanSubscription());

        qd.enter();
        qd.onNext(1);

        ts.assertEmpty();
    }

    @Test
    public void orderedSlowPath() {
        TestSubscriber<Integer> ts = new TestSubscriber<>(1);
        Disposable d = Disposable.empty();
        QueueDrainSubscriber<Integer, Integer, Integer> qd = createOrdered(ts, d);
        ts.onSubscribe(new BooleanSubscription());

        qd.enter();
        qd.onNext(1);

        ts.assertEmpty();
    }

    @Test
    public void orderedSlowPathNonEmptyQueue() {
        TestSubscriber<Integer> ts = new TestSubscriber<>(1);
        Disposable d = Disposable.empty();
        QueueDrainSubscriber<Integer, Integer, Integer> qd = createOrdered(ts, d);
        ts.onSubscribe(new BooleanSubscription());

        qd.queue.offer(0);
        qd.requested(2);
        qd.onNext(1);

        ts.assertValuesOnly(0, 1);
    }

    @Test
    public void unorderedOnNextRace() {
        for (int i = 0; i < TestHelper.RACE_LONG_LOOPS; i++) {

            TestSubscriber<Integer> ts = new TestSubscriber<>(1);
            Disposable d = Disposable.empty();
            final QueueDrainSubscriber<Integer, Integer, Integer> qd = createUnordered(ts, d);
            ts.onSubscribe(new BooleanSubscription());

            qd.requested(Long.MAX_VALUE);
            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    qd.onNext(1);
                }
            };

            TestHelper.race(r1, r1);

            ts.assertValuesOnly(1, 1);
        }
    }

    @Test
    public void orderedOnNextRace() {
        for (int i = 0; i < TestHelper.RACE_LONG_LOOPS; i++) {

            TestSubscriber<Integer> ts = new TestSubscriber<>(1);
            Disposable d = Disposable.empty();
            final QueueDrainSubscriber<Integer, Integer, Integer> qd = createOrdered(ts, d);
            ts.onSubscribe(new BooleanSubscription());

            qd.requested(Long.MAX_VALUE);
            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    qd.onNext(1);
                }
            };

            TestHelper.race(r1, r1);

            ts.assertValuesOnly(1, 1);
        }
    }

    @Test
    public void unorderedFastPathReject() {
        TestSubscriber<Integer> ts = new TestSubscriber<>(1);
        Disposable d = Disposable.empty();
        QueueDrainSubscriber<Integer, Integer, Integer> qd = createUnorderedReject(ts, d);
        ts.onSubscribe(new BooleanSubscription());

        qd.requested(1);

        qd.onNext(1);

        ts.assertValuesOnly(1);

        assertEquals(1, qd.requested());
    }

    @Test
    public void orderedFastPathReject() {
        TestSubscriber<Integer> ts = new TestSubscriber<>(1);
        Disposable d = Disposable.empty();
        QueueDrainSubscriber<Integer, Integer, Integer> qd = createOrderedReject(ts, d);
        ts.onSubscribe(new BooleanSubscription());

        qd.requested(1);

        qd.onNext(1);

        ts.assertValuesOnly(1);

        assertEquals(1, qd.requested());
    }
}
