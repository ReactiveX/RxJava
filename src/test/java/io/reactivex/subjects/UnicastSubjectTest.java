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

package io.reactivex.subjects;

import static org.junit.Assert.*;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

import io.reactivex.*;
import io.reactivex.disposables.*;
import io.reactivex.exceptions.TestException;
import io.reactivex.internal.fuseable.*;
import io.reactivex.observers.*;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.schedulers.Schedulers;
import static org.mockito.Mockito.mock;

public class UnicastSubjectTest {

    @Test
    public void fusionLive() {
        UnicastSubject<Integer> ap = UnicastSubject.create();

        TestObserver<Integer> ts = ObserverFusion.newTest(QueueDisposable.ANY);

        ap.subscribe(ts);

        ts
        .assertOf(ObserverFusion.<Integer>assertFuseable())
        .assertOf(ObserverFusion.<Integer>assertFusionMode(QueueDisposable.ASYNC));

        ts.assertNoValues().assertNoErrors().assertNotComplete();

        ap.onNext(1);

        ts.assertValue(1).assertNoErrors().assertNotComplete();

        ap.onComplete();

        ts.assertResult(1);
    }

    @Test
    public void fusionOfflie() {
        UnicastSubject<Integer> ap = UnicastSubject.create();
        ap.onNext(1);
        ap.onComplete();

        TestObserver<Integer> ts = ObserverFusion.newTest(QueueDisposable.ANY);

        ap.subscribe(ts);

        ts
        .assertOf(ObserverFusion.<Integer>assertFuseable())
        .assertOf(ObserverFusion.<Integer>assertFusionMode(QueueDisposable.ASYNC))
        .assertResult(1);
    }

    @Test
    public void failFast() {
        UnicastSubject<Integer> ap = UnicastSubject.create(false);
        ap.onNext(1);
        ap.onError(new RuntimeException());
        TestObserver<Integer> ts = TestObserver.create();
        ap.subscribe(ts);

        ts
                .assertValueCount(0)
                .assertError(RuntimeException.class);
    }

    @Test
    public void threeArgsFactoryFailFast() {
        Runnable noop = mock(Runnable.class);
        UnicastSubject<Integer> ap = UnicastSubject.create(16, noop, false);
        ap.onNext(1);
        ap.onError(new RuntimeException());
        TestObserver<Integer> ts = TestObserver.create();
        ap.subscribe(ts);

        ts
                .assertValueCount(0)
                .assertError(RuntimeException.class);
    }

    @Test
    public void threeArgsFactoryDelayError() {
        Runnable noop = mock(Runnable.class);
        UnicastSubject<Integer> ap = UnicastSubject.create(16, noop, true);
        ap.onNext(1);
        ap.onError(new RuntimeException());
        TestObserver<Integer> ts = TestObserver.create();
        ap.subscribe(ts);

        ts
                .assertValueCount(1)
                .assertError(RuntimeException.class);
    }

    @Test
    public void fusionOfflineFailFast() {
        UnicastSubject<Integer> ap = UnicastSubject.create(false);
        ap.onNext(1);
        ap.onError(new RuntimeException());
        TestObserver<Integer> ts = ObserverFusion.newTest(QueueDisposable.ANY);
        ap.subscribe(ts);

        ts
                .assertValueCount(0)
                .assertError(RuntimeException.class);
    }

    @Test
    public void fusionOfflineFailFastMultipleEvents() {
        UnicastSubject<Integer> ap = UnicastSubject.create(false);
        ap.onNext(1);
        ap.onNext(2);
        ap.onNext(3);
        ap.onComplete();
        TestObserver<Integer> ts = ObserverFusion.newTest(QueueDisposable.ANY);
        ap.subscribe(ts);

        ts
                .assertValueCount(3)
                .assertComplete();
    }

    @Test
    public void failFastMultipleEvents() {
        UnicastSubject<Integer> ap = UnicastSubject.create(false);
        ap.onNext(1);
        ap.onNext(2);
        ap.onNext(3);
        ap.onComplete();
        TestObserver<Integer> ts = TestObserver.create();
        ap.subscribe(ts);

        ts
                .assertValueCount(3)
                .assertComplete();
    }

    @Test
    public void onTerminateCalledWhenOnError() {
        final AtomicBoolean didRunOnTerminate = new AtomicBoolean();

        UnicastSubject<Integer> us = UnicastSubject.create(Observable.bufferSize(), new Runnable() {
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

        UnicastSubject<Integer> us = UnicastSubject.create(Observable.bufferSize(), new Runnable() {
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

        UnicastSubject<Integer> us = UnicastSubject.create(Observable.bufferSize(), new Runnable() {
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
        UnicastSubject.create(5, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void negativeCapacityHint() {
        UnicastSubject.create(-1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void zeroCapacityHint() {
        UnicastSubject.create(0);
    }

    @Test
    public void onNextNull() {
        final UnicastSubject<Object> s = UnicastSubject.create();

        s.onNext(null);

        s.test()
            .assertNoValues()
            .assertError(NullPointerException.class)
            .assertErrorMessage("onNext called with null. Null values are generally not allowed in 2.x operators and sources.");
    }

    @Test
    public void onErrorNull() {
        final UnicastSubject<Object> s = UnicastSubject.create();

        s.onError(null);

        s.test()
            .assertNoValues()
            .assertError(NullPointerException.class)
            .assertErrorMessage("onError called with null. Null values are generally not allowed in 2.x operators and sources.");
    }

    @Test
    public void onNextNullDelayed() {
        final UnicastSubject<Object> p = UnicastSubject.create();

        TestObserver<Object> ts = p.test();

        p.onNext(null);

        ts
            .assertNoValues()
            .assertError(NullPointerException.class)
            .assertErrorMessage("onNext called with null. Null values are generally not allowed in 2.x operators and sources.");
    }

    @Test
    public void onErrorNullDelayed() {
        final UnicastSubject<Object> p = UnicastSubject.create();

        assertFalse(p.hasObservers());

        TestObserver<Object> ts = p.test();

        assertTrue(p.hasObservers());

        p.onError(null);

        assertFalse(p.hasObservers());

        ts
            .assertNoValues()
            .assertError(NullPointerException.class)
            .assertErrorMessage("onError called with null. Null values are generally not allowed in 2.x operators and sources.");
    }

    @Test
    public void completeCancelRace() {
        for (int i = 0; i < 500; i++) {
            final int[] calls = { 0 };
            final UnicastSubject<Object> up = UnicastSubject.create(100, new Runnable() {
                @Override
                public void run() {
                    calls[0]++;
                }
            });

            final TestObserver<Object> ts = up.test();

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
        UnicastSubject<Object> p = UnicastSubject.create();
        p.onComplete();

        Disposable bs = Disposables.empty();
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
        UnicastSubject<Object> p = UnicastSubject.create();

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
        UnicastSubject<Object> p = UnicastSubject.create();

        TestObserver<Object> ts = ObserverFusion.newTest(QueueDisposable.SYNC);

        p.subscribe(ts);

        ObserverFusion.assertFusion(ts, QueueDisposable.NONE);
    }

    @Test
    public void cancelOnArrival() {
        UnicastSubject.create()
        .test(true)
        .assertEmpty();
    }

    @Test
    public void multiSubscriber() {
        UnicastSubject<Object> p = UnicastSubject.create();

        TestObserver<Object> ts = p.test();

        p.test()
        .assertFailure(IllegalStateException.class);

        p.onNext(1);
        p.onComplete();

        ts.assertResult(1);
    }

    @Test
    public void fusedDrainCancel() {
        for (int i = 0; i < 500; i++) {
            final UnicastSubject<Object> p = UnicastSubject.create();

            final TestObserver<Object> ts = ObserverFusion.newTest(QueueSubscription.ANY);

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

    @Test
    public void dispose() {
        final int[] calls = { 0 };

        UnicastSubject<Integer> us = new UnicastSubject<Integer>(128, new Runnable() {
            @Override
            public void run() { calls[0]++; }
        });

        TestHelper.checkDisposed(us);

        assertEquals(1, calls[0]);

        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            us.onError(new TestException());

            TestHelper.assertUndeliverable(errors, 0, TestException.class);
        } finally {
            RxJavaPlugins.reset();
        }

        Disposable d = Disposables.empty();

        us.onSubscribe(d);

        assertTrue(d.isDisposed());
    }
}
