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

package io.reactivex.processors;

import io.reactivex.Observable;
import io.reactivex.TestHelper;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.TestException;
import io.reactivex.internal.fuseable.QueueSubscription;
import io.reactivex.internal.subscriptions.BooleanSubscription;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subscribers.SubscriberFusion;
import io.reactivex.subscribers.TestSubscriber;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.*;

public class UnicastProcessorTest extends DelayedFlowableProcessorTest<Object> {

    @Override
    protected FlowableProcessor<Object> create() {
        return UnicastProcessor.create();
    }

    @Test
    public void fusionLive() {
        UnicastProcessor<Integer> ap = UnicastProcessor.create();

        TestSubscriber<Integer> ts = SubscriberFusion.newTest(QueueSubscription.ANY);

        ap.subscribe(ts);

        ts
        .assertOf(SubscriberFusion.<Integer>assertFuseable())
        .assertOf(SubscriberFusion.<Integer>assertFusionMode(QueueSubscription.ASYNC));

        ts.assertNoValues().assertNoErrors().assertNotComplete();

        ap.onNext(1);

        ts.assertValue(1).assertNoErrors().assertNotComplete();

        ap.onComplete();

        ts.assertResult(1);
    }

    @Test
    public void fusionOfflie() {
        UnicastProcessor<Integer> ap = UnicastProcessor.create();
        ap.onNext(1);
        ap.onComplete();

        TestSubscriber<Integer> ts = SubscriberFusion.newTest(QueueSubscription.ANY);

        ap.subscribe(ts);

        ts
        .assertOf(SubscriberFusion.<Integer>assertFuseable())
        .assertOf(SubscriberFusion.<Integer>assertFusionMode(QueueSubscription.ASYNC))
        .assertResult(1);
    }

    @Test
    public void failFast() {
        UnicastProcessor<Integer> ap = UnicastProcessor.create(false);
        ap.onNext(1);
        ap.onError(new RuntimeException());

        TestSubscriber<Integer> ts = TestSubscriber.create();

        ap.subscribe(ts);

        ts
                .assertValueCount(0)
                .assertError(RuntimeException.class);
    }

    @Test
    public void failFastFusionOffline() {
        UnicastProcessor<Integer> ap = UnicastProcessor.create(false);
        ap.onNext(1);
        ap.onError(new RuntimeException());

        TestSubscriber<Integer> ts = SubscriberFusion.newTest(QueueSubscription.ANY);

        ap.subscribe(ts);
        ts
                .assertValueCount(0)
                .assertError(RuntimeException.class);
    }

    @Test
    public void threeArgsFactory() {
        Runnable noop = new Runnable() {
            @Override
            public void run() {
            }
        };
        UnicastProcessor<Integer> ap = UnicastProcessor.create(16, noop,false);
        ap.onNext(1);
        ap.onError(new RuntimeException());

        TestSubscriber<Integer> ts = TestSubscriber.create();

        ap.subscribe(ts);
        ts
                .assertValueCount(0)
                .assertError(RuntimeException.class);
    }

    @Test
    public void onTerminateCalledWhenOnError() {
        final AtomicBoolean didRunOnTerminate = new AtomicBoolean();

        UnicastProcessor<Integer> us = UnicastProcessor.create(Observable.bufferSize(), new Runnable() {
            @Override public void run() {
                didRunOnTerminate.set(true);
            }
        });

        assertEquals(false, didRunOnTerminate.get());
        us.onError(new RuntimeException("some error"));
        assertEquals(true, didRunOnTerminate.get());
    }

    @Test
    public void onTerminateCalledWhenOnComplete() {
        final AtomicBoolean didRunOnTerminate = new AtomicBoolean();

        UnicastProcessor<Integer> us = UnicastProcessor.create(Observable.bufferSize(), new Runnable() {
            @Override public void run() {
                didRunOnTerminate.set(true);
            }
        });

        assertEquals(false, didRunOnTerminate.get());
        us.onComplete();
        assertEquals(true, didRunOnTerminate.get());
    }

    @Test
    public void onTerminateCalledWhenCanceled() {
        final AtomicBoolean didRunOnTerminate = new AtomicBoolean();

        UnicastProcessor<Integer> us = UnicastProcessor.create(Observable.bufferSize(), new Runnable() {
            @Override public void run() {
                didRunOnTerminate.set(true);
            }
        });

        final Disposable subscribe = us.subscribe();

        assertEquals(false, didRunOnTerminate.get());
        subscribe.dispose();
        assertEquals(true, didRunOnTerminate.get());
    }

    @Test(expected = NullPointerException.class)
    public void nullOnTerminate() {
        UnicastProcessor.create(5, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void negativeCapacityHint() {
        UnicastProcessor.create(-1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void zeroCapacityHint() {
        UnicastProcessor.create(0);
    }

    @Test
    public void completeCancelRace() {
        for (int i = 0; i < 500; i++) {
            final int[] calls = { 0 };
            final UnicastProcessor<Object> up = UnicastProcessor.create(100, new Runnable() {
                @Override
                public void run() {
                    calls[0]++;
                }
            });

            final TestSubscriber<Object> ts = up.test();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    ts.cancel();
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    up.onComplete();
                }
            };

            TestHelper.race(r1, r2, Schedulers.single());

            assertEquals(1, calls[0]);
        }
    }

    @Test
    public void afterDone() {
        UnicastProcessor<Object> p = UnicastProcessor.create();
        p.onComplete();

        BooleanSubscription bs = new BooleanSubscription();
        p.onSubscribe(bs);

        p.onNext(1);
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            p.onError(new TestException());

            TestHelper.assertUndeliverable(errors, 0, TestException.class);
        } finally {
            RxJavaPlugins.reset();
        }
        p.onComplete();

        p.test().assertResult();

        assertNull(p.getThrowable());
        assertTrue(p.hasComplete());
        assertFalse(p.hasThrowable());
    }

    @Test
    public void onErrorStatePeeking() {
        UnicastProcessor<Object> p = UnicastProcessor.create();

        assertFalse(p.hasComplete());
        assertFalse(p.hasThrowable());
        assertNull(p.getThrowable());

        TestException ex = new TestException();
        p.onError(ex);

        assertFalse(p.hasComplete());
        assertTrue(p.hasThrowable());
        assertSame(ex, p.getThrowable());
    }

    @Test
    public void rejectSyncFusion() {
        UnicastProcessor<Object> p = UnicastProcessor.create();

        TestSubscriber<Object> ts = SubscriberFusion.newTest(QueueSubscription.SYNC);

        p.subscribe(ts);

        SubscriberFusion.assertFusion(ts, QueueSubscription.NONE);
    }

    @Test
    public void cancelOnArrival() {
        UnicastProcessor.create()
        .test(0L, true)
        .assertEmpty();
    }

    @Test
    public void multiSubscriber() {
        UnicastProcessor<Object> p = UnicastProcessor.create();

        TestSubscriber<Object> ts = p.test();

        p.test()
        .assertFailure(IllegalStateException.class);

        p.onNext(1);
        p.onComplete();

        ts.assertResult(1);
    }

    @Test
    public void fusedDrainCancel() {
        for (int i = 0; i < 500; i++) {
            final UnicastProcessor<Object> p = UnicastProcessor.create();

            final TestSubscriber<Object> ts = SubscriberFusion.newTest(QueueSubscription.ANY);

            p.subscribe(ts);

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    p.onNext(1);
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    ts.cancel();
                }
            };

            TestHelper.race(r1, r2, Schedulers.single());
        }
    }
}
