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

package io.reactivex.rxjava3.processors;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.reactivestreams.Subscription;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.exceptions.*;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.internal.subscriptions.BooleanSubscription;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.subscribers.TestSubscriber;
import io.reactivex.rxjava3.testsupport.TestHelper;

public class MulticastProcessorTest extends RxJavaTest {

    @Test
    public void complete() {
        MulticastProcessor<Integer> mp = MulticastProcessor.create();
        mp.start();

        assertFalse(mp.hasSubscribers());
        assertFalse(mp.hasComplete());
        assertFalse(mp.hasThrowable());
        assertNull(mp.getThrowable());

        TestSubscriber<Integer> ts = mp.test();

        assertTrue(mp.hasSubscribers());
        assertFalse(mp.hasComplete());
        assertFalse(mp.hasThrowable());
        assertNull(mp.getThrowable());

        mp.onNext(1);
        mp.onComplete();

        ts.assertResult(1);

        assertFalse(mp.hasSubscribers());
        assertTrue(mp.hasComplete());
        assertFalse(mp.hasThrowable());
        assertNull(mp.getThrowable());

        mp.test().assertResult();
    }

    @Test
    public void error() {
        MulticastProcessor<Integer> mp = MulticastProcessor.create();
        mp.start();

        assertFalse(mp.hasSubscribers());
        assertFalse(mp.hasComplete());
        assertFalse(mp.hasThrowable());
        assertNull(mp.getThrowable());

        TestSubscriber<Integer> ts = mp.test();

        assertTrue(mp.hasSubscribers());
        assertFalse(mp.hasComplete());
        assertFalse(mp.hasThrowable());
        assertNull(mp.getThrowable());

        mp.onNext(1);
        mp.onError(new IOException());

        ts.assertFailure(IOException.class, 1);

        assertFalse(mp.hasSubscribers());
        assertFalse(mp.hasComplete());
        assertTrue(mp.hasThrowable());
        assertNotNull(mp.getThrowable());
        assertTrue("" + mp.getThrowable(), mp.getThrowable() instanceof IOException);

        mp.test().assertFailure(IOException.class);
    }

    @Test
    public void overflow() {
        MulticastProcessor<Integer> mp = MulticastProcessor.create(1);
        mp.start();

        TestSubscriber<Integer> ts = mp.test(0);

        assertTrue(mp.offer(1));
        assertFalse(mp.offer(2));

        mp.onNext(3);

        ts.assertEmpty();

        ts.request(1);

        ts.assertFailure(MissingBackpressureException.class, 1);

        mp.test().assertFailure(MissingBackpressureException.class);
    }

    @Test
    public void backpressure() {
        MulticastProcessor<Integer> mp = MulticastProcessor.create(16, false);
        mp.start();

        for (int i = 0; i < 10; i++) {
            mp.onNext(i);
        }
        mp.onComplete();

        mp.test(0)
        .assertEmpty()
        .requestMore(1)
        .assertValuesOnly(0)
        .requestMore(2)
        .assertValuesOnly(0, 1, 2)
        .requestMore(3)
        .assertValuesOnly(0, 1, 2, 3, 4, 5)
        .requestMore(4)
        .assertResult(0, 1, 2, 3, 4, 5, 6, 7, 8, 9);
    }

    @Test
    public void refCounted() {
        MulticastProcessor<Integer> mp = MulticastProcessor.create(true);
        BooleanSubscription bs = new BooleanSubscription();

        mp.onSubscribe(bs);

        assertFalse(bs.isCancelled());

        mp.test().cancel();

        assertTrue(bs.isCancelled());

        assertFalse(mp.hasSubscribers());
        assertTrue(mp.hasComplete());
        assertFalse(mp.hasThrowable());
        assertNull(mp.getThrowable());

        mp.test().assertResult();
    }

    @Test
    public void refCounted2() {
        MulticastProcessor<Integer> mp = MulticastProcessor.create(16, true);
        BooleanSubscription bs = new BooleanSubscription();

        mp.onSubscribe(bs);

        assertFalse(bs.isCancelled());

        mp.test(1, true);

        assertTrue(bs.isCancelled());

        assertFalse(mp.hasSubscribers());
        assertTrue(mp.hasComplete());
        assertFalse(mp.hasThrowable());
        assertNull(mp.getThrowable());

        mp.test().assertResult();
    }

    @Test
    public void longRunning() {
        MulticastProcessor<Integer> mp = MulticastProcessor.create(16);
        Flowable.range(1, 1000).subscribe(mp);

        mp.test().assertValueCount(1000).assertNoErrors().assertComplete();
    }

    @Test
    public void oneByOne() {
        MulticastProcessor<Integer> mp = MulticastProcessor.create(16);
        Flowable.range(1, 1000).subscribe(mp);

        mp
        .rebatchRequests(1)
        .test()
        .assertValueCount(1000)
        .assertNoErrors()
        .assertComplete();
    }

    @Test
    public void take() {
        MulticastProcessor<Integer> mp = MulticastProcessor.create(16);
        Flowable.range(1, 1000).subscribe(mp);

        mp.take(10).test().assertResult(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
    }

    @Test
    public void takeRefCount() {
        MulticastProcessor<Integer> mp = MulticastProcessor.create(16, true);
        Flowable.range(1, 1000).subscribe(mp);

        mp.take(10).test().assertResult(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
    }

    @Test
    public void takeRefCountExact() {
        MulticastProcessor<Integer> mp = MulticastProcessor.create(16, true);
        Flowable.range(1, 10).subscribe(mp);

        mp
        .rebatchRequests(10)
        .take(10)
        .test().assertResult(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
    }

    @Test
    public void crossCancel() {

        final TestSubscriber<Integer> ts1 = new TestSubscriber<>();

        TestSubscriber<Integer> ts2 = new TestSubscriber<Integer>() {
            @Override
            public void onNext(Integer t) {
                super.onNext(t);
                ts1.cancel();
                ts1.onComplete();
            }
        };

        MulticastProcessor<Integer> mp = MulticastProcessor.create(false);

        mp.subscribe(ts2);
        mp.subscribe(ts1);

        mp.start();

        mp.onNext(1);
        mp.onComplete();

        ts1.assertResult();
        ts2.assertResult(1);
    }

    @Test
    public void crossCancelError() {

        final TestSubscriber<Integer> ts1 = new TestSubscriber<>();

        TestSubscriber<Integer> ts2 = new TestSubscriber<Integer>() {
            @Override
            public void onError(Throwable t) {
                super.onError(t);
                ts1.cancel();
                ts1.onComplete();
            }
        };

        MulticastProcessor<Integer> mp = MulticastProcessor.create(false);

        mp.subscribe(ts2);
        mp.subscribe(ts1);

        mp.start();

        mp.onNext(1);
        mp.onError(new IOException());

        ts1.assertResult(1);
        ts2.assertFailure(IOException.class, 1);
    }

    @Test
    public void crossCancelComplete() {

        final TestSubscriber<Integer> ts1 = new TestSubscriber<>();

        TestSubscriber<Integer> ts2 = new TestSubscriber<Integer>() {
            @Override
            public void onComplete() {
                super.onComplete();
                ts1.cancel();
                ts1.onNext(2);
                ts1.onComplete();
            }
        };

        MulticastProcessor<Integer> mp = MulticastProcessor.create(false);

        mp.subscribe(ts2);
        mp.subscribe(ts1);

        mp.start();

        mp.onNext(1);
        mp.onComplete();

        ts1.assertResult(1, 2);
        ts2.assertResult(1);
    }

    @Test
    public void crossCancel1() {

        final TestSubscriber<Integer> ts1 = new TestSubscriber<>(1);

        TestSubscriber<Integer> ts2 = new TestSubscriber<Integer>(1) {
            @Override
            public void onNext(Integer t) {
                super.onNext(t);
                ts1.cancel();
                ts1.onComplete();
            }
        };

        MulticastProcessor<Integer> mp = MulticastProcessor.create(false);

        mp.subscribe(ts2);
        mp.subscribe(ts1);

        mp.start();

        mp.onNext(1);
        mp.onComplete();

        ts1.assertResult();
        ts2.assertResult(1);
    }

    @Test
    public void requestCancel() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            MulticastProcessor<Integer> mp = MulticastProcessor.create(false);

            mp.subscribe(new FlowableSubscriber<Integer>() {

                @Override
                public void onNext(Integer t) {
                }

                @Override
                public void onError(Throwable t) {
                }

                @Override
                public void onComplete() {
                }

                @Override
                public void onSubscribe(Subscription t) {
                    t.request(-1);
                    t.request(1);
                    t.request(Long.MAX_VALUE);
                    t.request(Long.MAX_VALUE);
                    t.cancel();
                    t.cancel();
                    t.request(2);
                }
            });

            TestHelper.assertError(errors, 0, IllegalArgumentException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void unbounded() {
        MulticastProcessor<Integer> mp = MulticastProcessor.create(4, false);
        mp.startUnbounded();

        for (int i = 0; i < 10; i++) {
            assertTrue(mp.offer(i));
        }
        mp.onComplete();

        mp.test().assertResult(0, 1, 2, 3, 4, 5, 6, 7, 8, 9);
    }

    @Test
    public void multiStart() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            MulticastProcessor<Integer> mp = MulticastProcessor.create(4, false);

            mp.start();
            mp.start();
            mp.startUnbounded();
            BooleanSubscription bs = new BooleanSubscription();
            mp.onSubscribe(bs);

            assertTrue(bs.isCancelled());

            TestHelper.assertError(errors, 0, ProtocolViolationException.class);
            TestHelper.assertError(errors, 1, ProtocolViolationException.class);
            TestHelper.assertError(errors, 2, ProtocolViolationException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test(expected = NullPointerException.class)
    public void onNextNull() {
        MulticastProcessor<Integer> mp = MulticastProcessor.create(4, false);
        mp.start();
        mp.onNext(null);
    }

    @Test(expected = NullPointerException.class)
    public void onOfferNull() {
        MulticastProcessor<Integer> mp = MulticastProcessor.create(4, false);
        mp.start();
        mp.offer(null);
    }

    @Test(expected = NullPointerException.class)
    public void onErrorNull() {
        MulticastProcessor<Integer> mp = MulticastProcessor.create(4, false);
        mp.start();
        mp.onError(null);
    }

    @Test
    public void afterTerminated() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            MulticastProcessor<Integer> mp = MulticastProcessor.create();
            mp.start();
            mp.onComplete();
            mp.onComplete();
            mp.onError(new IOException());
            mp.onNext(1);
            mp.offer(1);

            mp.test().assertResult();

            TestHelper.assertUndeliverable(errors, 0, IOException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void asyncFused() {
        UnicastProcessor<Integer> up = UnicastProcessor.create();
        MulticastProcessor<Integer> mp = MulticastProcessor.create(4);

        up.subscribe(mp);

        TestSubscriber<Integer> ts = mp.test();

        for (int i = 0; i < 10; i++) {
            up.onNext(i);
        }

        assertFalse(mp.offer(10));

        up.onComplete();

        ts.assertResult(0, 1, 2, 3, 4, 5, 6, 7, 8, 9);
    }

    @Test
    public void fusionCrash() {
        MulticastProcessor<Integer> mp = Flowable.range(1, 5)
        .map(new Function<Integer, Integer>() {
            @Override
            public Integer apply(Integer v) throws Exception {
                throw new IOException();
            }
        })
        .subscribeWith(MulticastProcessor.<Integer>create());

        mp.test().assertFailure(IOException.class);
    }

    @Test
    public void lockstep() {
        MulticastProcessor<Integer> mp = MulticastProcessor.create();

        TestSubscriber<Integer> ts1 = mp.test();
        mp.start();

        mp.onNext(1);
        mp.onNext(2);

        ts1.assertValues(1, 2);

        TestSubscriber<Integer> ts2 = mp.test(0);

        ts2.assertEmpty();

        mp.onNext(3);

        ts1.assertValues(1, 2);
        ts2.assertEmpty();

        mp.onComplete();

        ts1.assertValues(1, 2);
        ts2.assertEmpty();

        ts2.request(1);

        ts1.assertResult(1, 2, 3);
        ts2.assertResult(3);
    }

    @Test
    public void rejectedFusion() {

        MulticastProcessor<Integer> mp = MulticastProcessor.create();

        TestHelper.<Integer>rejectFlowableFusion()
        .subscribe(mp);

        mp.test().assertEmpty();
    }

    @Test
    public void addRemoveRaceNoRefCount() {
        for (int i = 0; i < TestHelper.RACE_LONG_LOOPS; i++) {
            final MulticastProcessor<Integer> mp = MulticastProcessor.create();

            final TestSubscriber<Integer> ts = mp.test();
            final TestSubscriber<Integer> ts2 = new TestSubscriber<>();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    ts.cancel();
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    mp.subscribe(ts2);
                }
            };

            TestHelper.race(r1, r2);

            assertTrue(mp.hasSubscribers());
        }
    }

    @Test
    public void addRemoveRaceNoRefCountNonEmpty() {
        for (int i = 0; i < TestHelper.RACE_LONG_LOOPS; i++) {
            final MulticastProcessor<Integer> mp = MulticastProcessor.create();

            mp.test();
            final TestSubscriber<Integer> ts = mp.test();
            final TestSubscriber<Integer> ts2 = new TestSubscriber<>();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    ts.cancel();
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    mp.subscribe(ts2);
                }
            };

            TestHelper.race(r1, r2);

            assertTrue(mp.hasSubscribers());
        }
    }

    @Test
    public void addRemoveRaceWitRefCount() {
        for (int i = 0; i < TestHelper.RACE_LONG_LOOPS; i++) {
            final MulticastProcessor<Integer> mp = MulticastProcessor.create(true);

            final TestSubscriber<Integer> ts = mp.test();
            final TestSubscriber<Integer> ts2 = new TestSubscriber<>();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    ts.cancel();
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    mp.subscribe(ts2);
                }
            };

            TestHelper.race(r1, r2);
        }
    }

    @Test
    public void cancelUpfront() {
        MulticastProcessor<Integer> mp = MulticastProcessor.create();

        mp.test(0, true).assertEmpty();

        assertFalse(mp.hasSubscribers());
    }

    @Test
    public void cancelUpfrontOtherConsumersPresent() {
        MulticastProcessor<Integer> mp = MulticastProcessor.create();

        mp.test();

        mp.test(0, true).assertEmpty();

        assertTrue(mp.hasSubscribers());
    }

    @Test
    public void consumerRequestRace() {
        for (int i = 0; i < TestHelper.RACE_LONG_LOOPS; i++) {
            final MulticastProcessor<Integer> mp = MulticastProcessor.create(true);
            mp.startUnbounded();
            mp.onNext(1);
            mp.onNext(2);

            final TestSubscriber<Integer> ts = mp.test();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    ts.request(1);
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    ts.request(1);
                }
            };

            TestHelper.race(r1, r2);

            ts.assertValuesOnly(1, 2);
        }
    }

    @Test
    public void consumerUpstreamRace() {
        for (int i = 0; i < TestHelper.RACE_LONG_LOOPS; i++) {
            final MulticastProcessor<Integer> mp = MulticastProcessor.create(true);

            final Flowable<Integer> source = Flowable.range(1, 5);

            final TestSubscriber<Integer> ts = mp.test();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    ts.request(5);
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    source.subscribe(mp);
                }
            };

            TestHelper.race(r1, r2);

            ts
            .awaitDone(5, TimeUnit.SECONDS)
            .assertResult(1, 2, 3, 4, 5);
        }
    }

    @Test
    public void emitCancelRace() {
        for (int i = 0; i < TestHelper.RACE_LONG_LOOPS; i++) {
            final MulticastProcessor<Integer> mp = MulticastProcessor.create(true);
            mp.startUnbounded();

            final TestSubscriber<Integer> ts = mp.test();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    ts.cancel();
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    mp.onNext(1);
                }
            };

            TestHelper.race(r1, r2);
        }
    }

    @Test
    public void cancelCancelDrain() {
        for (int i = 0; i < TestHelper.RACE_LONG_LOOPS; i++) {
            final MulticastProcessor<Integer> mp = MulticastProcessor.create(true);

            final TestSubscriber<Integer> ts1 = mp.test();
            final TestSubscriber<Integer> ts2 = mp.test();

            mp.test();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    ts1.cancel();
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    ts2.cancel();
                }
            };

            TestHelper.race(r1, r2);
        }
    }

    @Test
    public void requestCancelRace() {
        for (int i = 0; i < TestHelper.RACE_LONG_LOOPS; i++) {
            final MulticastProcessor<Integer> mp = MulticastProcessor.create(true);

            final TestSubscriber<Integer> ts1 = mp.test();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    ts1.cancel();
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    ts1.request(1);
                }
            };

            TestHelper.race(r1, r2);
        }
    }

    @Test
    public void noUpstream() {
        MulticastProcessor<Integer> mp = MulticastProcessor.create();

        TestSubscriber<Integer> ts = mp.test(0);

        ts.request(1);

        assertTrue(mp.hasSubscribers());
    }

    @Test
    public void requestUpstreamPrefetchNonFused() {
        for (int j = 1; j < 12; j++) {
            MulticastProcessor<Integer> mp = MulticastProcessor.create(j, true);

            TestSubscriber<Integer> ts = mp.test(0).withTag("Prefetch: " + j);

            Flowable.range(1, 10).hide().subscribe(mp);

            ts.assertEmpty()
            .requestMore(3)
            .assertValuesOnly(1, 2, 3)
            .requestMore(3)
            .assertValuesOnly(1, 2, 3, 4, 5, 6)
            .requestMore(4)
            .assertResult(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        }
    }

    @Test
    public void requestUpstreamPrefetchNonFused2() {
        for (int j = 1; j < 12; j++) {
            MulticastProcessor<Integer> mp = MulticastProcessor.create(j, true);

            TestSubscriber<Integer> ts = mp.test(0).withTag("Prefetch: " + j);

            Flowable.range(1, 10).hide().subscribe(mp);

            ts.assertEmpty()
            .requestMore(2)
            .assertValuesOnly(1, 2)
            .requestMore(2)
            .assertValuesOnly(1, 2, 3, 4)
            .requestMore(6)
            .assertResult(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        }
    }
}
