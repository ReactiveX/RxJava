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

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.exceptions.*;
import io.reactivex.rxjava3.internal.functions.Functions;
import io.reactivex.rxjava3.internal.fuseable.QueueFuseable;
import io.reactivex.rxjava3.internal.subscriptions.BooleanSubscription;
import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subscribers.TestSubscriber;
import io.reactivex.rxjava3.testsupport.*;

public class UnicastProcessorTest extends FlowableProcessorTest<Object> {

    @Override
    protected FlowableProcessor<Object> create() {
        return UnicastProcessor.create();
    }

    @Test
    public void fusionLive() {
        UnicastProcessor<Integer> ap = UnicastProcessor.create();

        TestSubscriberEx<Integer> ts = new TestSubscriberEx<Integer>().setInitialFusionMode(QueueFuseable.ANY);

        ap.subscribe(ts);

        ts
        .assertFuseable()
        .assertFusionMode(QueueFuseable.ASYNC);

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

        TestSubscriberEx<Integer> ts = new TestSubscriberEx<Integer>().setInitialFusionMode(QueueFuseable.ANY);

        ap.subscribe(ts);

        ts
        .assertFuseable()
        .assertFusionMode(QueueFuseable.ASYNC)
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

        TestSubscriberEx<Integer> ts = new TestSubscriberEx<Integer>().setInitialFusionMode(QueueFuseable.ANY);

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
        UnicastProcessor<Integer> ap = UnicastProcessor.create(16, noop, false);
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

        assertFalse(didRunOnTerminate.get());
        us.onError(new RuntimeException("some error"));
        assertTrue(didRunOnTerminate.get());
    }

    @Test
    public void onTerminateCalledWhenOnComplete() {
        final AtomicBoolean didRunOnTerminate = new AtomicBoolean();

        UnicastProcessor<Integer> us = UnicastProcessor.create(Observable.bufferSize(), new Runnable() {
            @Override public void run() {
                didRunOnTerminate.set(true);
            }
        });

        assertFalse(didRunOnTerminate.get());
        us.onComplete();
        assertTrue(didRunOnTerminate.get());
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

        assertFalse(didRunOnTerminate.get());
        subscribe.dispose();
        assertTrue(didRunOnTerminate.get());
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
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
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

            TestHelper.race(r1, r2);

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

        TestSubscriberEx<Object> ts = new TestSubscriberEx<>().setInitialFusionMode(QueueFuseable.SYNC);

        p.subscribe(ts);

        ts.assertFusionMode(QueueFuseable.NONE);
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
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            final UnicastProcessor<Object> p = UnicastProcessor.create();

            final TestSubscriberEx<Object> ts = new TestSubscriberEx<>().setInitialFusionMode(QueueFuseable.ANY);

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

            TestHelper.race(r1, r2);
        }
    }

    @Test
    public void subscribeRace() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            final UnicastProcessor<Integer> us = UnicastProcessor.create();

            final TestSubscriberEx<Integer> ts1 = new TestSubscriberEx<>();
            final TestSubscriberEx<Integer> ts2 = new TestSubscriberEx<>();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    us.subscribe(ts1);
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    us.subscribe(ts2);
                }
            };

            TestHelper.race(r1, r2);

            if (ts1.errors().size() == 0) {
                ts2.assertFailure(IllegalStateException.class);
            } else
            if (ts2.errors().size() == 0) {
                ts1.assertFailure(IllegalStateException.class);
            } else {
                fail("Neither TestObserver failed");
            }
        }
    }

    @Test
    public void hasObservers() {
        UnicastProcessor<Integer> us = UnicastProcessor.create();

        assertFalse(us.hasSubscribers());

        TestSubscriber<Integer> ts = us.test();

        assertTrue(us.hasSubscribers());

        ts.cancel();

        assertFalse(us.hasSubscribers());
    }

    @Test
    public void drainFusedFailFast() {
        UnicastProcessor<Integer> us = UnicastProcessor.create(false);

        TestSubscriberEx<Integer> ts = us.to(TestHelper.<Integer>testSubscriber(1, QueueFuseable.ANY, false));

        us.done = true;
        us.drainFused(ts);

        ts.assertResult();
    }

    @Test
    public void drainFusedFailFastEmpty() {
        UnicastProcessor<Integer> us = UnicastProcessor.create(false);

        TestSubscriberEx<Integer> ts = us.to(TestHelper.<Integer>testSubscriber(1, QueueFuseable.ANY, false));

        us.drainFused(ts);

        ts.assertEmpty();
    }

    @Test
    public void checkTerminatedFailFastEmpty() {
        UnicastProcessor<Integer> us = UnicastProcessor.create(false);

        TestSubscriberEx<Integer> ts = us.to(TestHelper.<Integer>testSubscriber(1, QueueFuseable.ANY, false));

        us.checkTerminated(true, true, false, ts, us.queue);

        ts.assertEmpty();
    }

    @Test
    public void alreadyCancelled() {
        UnicastProcessor<Integer> us = UnicastProcessor.create(false);

        us.test().cancel();

        BooleanSubscription bs = new BooleanSubscription();
        us.onSubscribe(bs);

        assertTrue(bs.isCancelled());

        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            us.onError(new TestException());

            TestHelper.assertUndeliverable(errors, 0, TestException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void unicastSubscriptionBadRequest() {
        UnicastProcessor<Integer> us = UnicastProcessor.create(false);

        UnicastProcessor<Integer>.UnicastQueueSubscription usc = (UnicastProcessor<Integer>.UnicastQueueSubscription)us.wip;

        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            usc.request(-1);
            TestHelper.assertError(errors, 0, IllegalArgumentException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void fusedNoConcurrentCleanDueToCancel() {
        for (int j = 0; j < TestHelper.RACE_LONG_LOOPS; j++) {
            List<Throwable> errors = TestHelper.trackPluginErrors();
            try {
                final UnicastProcessor<Integer> us = UnicastProcessor.create();

                TestObserver<Integer> to = us
                .observeOn(Schedulers.io())
                .map(Functions.<Integer>identity())
                .observeOn(Schedulers.single())
                .firstOrError()
                .test();

                for (int i = 0; us.hasSubscribers(); i++) {
                    us.onNext(i);
                }

                to
                .awaitDone(5, TimeUnit.SECONDS)
                ;

                if (!errors.isEmpty()) {
                    throw new CompositeException(errors);
                }

                to.assertResult(0);
            } finally {
                RxJavaPlugins.reset();
            }
        }
    }
}
