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

package io.reactivex.rxjava3.subjects;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.*;
import io.reactivex.rxjava3.exceptions.*;
import io.reactivex.rxjava3.internal.functions.Functions;
import io.reactivex.rxjava3.internal.fuseable.QueueFuseable;
import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.testsupport.*;

public class UnicastSubjectTest extends SubjectTest<Integer> {

    @Override
    protected Subject<Integer> create() {
        return UnicastSubject.create();
    }

    @Test
    public void fusionLive() {
        UnicastSubject<Integer> ap = UnicastSubject.create();

        TestObserverEx<Integer> to = new TestObserverEx<>(QueueFuseable.ANY);

        ap.subscribe(to);

        to
        .assertFuseable()
        .assertFusionMode(QueueFuseable.ASYNC);

        to.assertNoValues().assertNoErrors().assertNotComplete();

        ap.onNext(1);

        to.assertValue(1).assertNoErrors().assertNotComplete();

        ap.onComplete();

        to.assertResult(1);
    }

    @Test
    public void fusionOfflie() {
        UnicastSubject<Integer> ap = UnicastSubject.create();
        ap.onNext(1);
        ap.onComplete();

        TestObserverEx<Integer> to = new TestObserverEx<>(QueueFuseable.ANY);

        ap.subscribe(to);

        to
        .assertFuseable()
        .assertFusionMode(QueueFuseable.ASYNC)
        .assertResult(1);
    }

    @Test
    public void failFast() {
        UnicastSubject<Integer> ap = UnicastSubject.create(false);
        ap.onNext(1);
        ap.onError(new RuntimeException());
        TestObserver<Integer> to = TestObserver.create();
        ap.subscribe(to);

        to
                .assertValueCount(0)
                .assertError(RuntimeException.class);
    }

    @Test
    public void threeArgsFactoryFailFast() {
        Runnable noop = mock(Runnable.class);
        UnicastSubject<Integer> ap = UnicastSubject.create(16, noop, false);
        ap.onNext(1);
        ap.onError(new RuntimeException());
        TestObserver<Integer> to = TestObserver.create();
        ap.subscribe(to);

        to
                .assertValueCount(0)
                .assertError(RuntimeException.class);
    }

    @Test
    public void threeArgsFactoryDelayError() {
        Runnable noop = mock(Runnable.class);
        UnicastSubject<Integer> ap = UnicastSubject.create(16, noop, true);
        ap.onNext(1);
        ap.onError(new RuntimeException());
        TestObserver<Integer> to = TestObserver.create();
        ap.subscribe(to);

        to
                .assertValueCount(1)
                .assertError(RuntimeException.class);
    }

    @Test
    public void fusionOfflineFailFast() {
        UnicastSubject<Integer> ap = UnicastSubject.create(false);
        ap.onNext(1);
        ap.onError(new RuntimeException());
        TestObserverEx<Integer> to = new TestObserverEx<>(QueueFuseable.ANY);
        ap.subscribe(to);

        to
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
        TestObserverEx<Integer> to = new TestObserverEx<>(QueueFuseable.ANY);
        ap.subscribe(to);

        to
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
        TestObserver<Integer> to = TestObserver.create();
        ap.subscribe(to);

        to
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

        assertFalse(didRunOnTerminate.get());
        us.onError(new RuntimeException("some error"));
        assertTrue(didRunOnTerminate.get());
    }

    @Test
    public void onTerminateCalledWhenOnComplete() {
        final AtomicBoolean didRunOnTerminate = new AtomicBoolean();

        UnicastSubject<Integer> us = UnicastSubject.create(Observable.bufferSize(), new Runnable() {
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

        UnicastSubject<Integer> us = UnicastSubject.create(Observable.bufferSize(), new Runnable() {
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
    public void completeCancelRace() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            final int[] calls = { 0 };
            final UnicastSubject<Object> up = UnicastSubject.create(100, new Runnable() {
                @Override
                public void run() {
                    calls[0]++;
                }
            });

            final TestObserver<Object> to = up.test();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    to.dispose();
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
        UnicastSubject<Object> p = UnicastSubject.create();
        p.onComplete();

        Disposable bs = Disposable.empty();
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

        TestObserverEx<Object> to = new TestObserverEx<>(QueueFuseable.SYNC);

        p.subscribe(to);

        to.assertFusionMode(QueueFuseable.NONE);
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

        TestObserver<Object> to = p.test();

        p.test()
        .assertFailure(IllegalStateException.class);

        p.onNext(1);
        p.onComplete();

        to.assertResult(1);
    }

    @Test
    public void fusedDrainCancel() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            final UnicastSubject<Object> p = UnicastSubject.create();

            final TestObserverEx<Object> to = new TestObserverEx<>(QueueFuseable.ANY);

            p.subscribe(to);

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    p.onNext(1);
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    to.dispose();
                }
            };

            TestHelper.race(r1, r2);
        }
    }

    @Test
    public void dispose() {
        final int[] calls = { 0 };

        UnicastSubject<Integer> us = new UnicastSubject<>(128, new Runnable() {
            @Override
            public void run() {
                calls[0]++;
            }
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

        Disposable d = Disposable.empty();

        us.onSubscribe(d);

        assertTrue(d.isDisposed());
    }

    @Test
    public void subscribeRace() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            final UnicastSubject<Integer> us = UnicastSubject.create();

            final TestObserverEx<Integer> to1 = new TestObserverEx<>();
            final TestObserverEx<Integer> to2 = new TestObserverEx<>();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    us.subscribe(to1);
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    us.subscribe(to2);
                }
            };

            TestHelper.race(r1, r2);

            if (to1.errors().size() == 0) {
                to2.assertFailure(IllegalStateException.class);
            } else
            if (to2.errors().size() == 0) {
                to1.assertFailure(IllegalStateException.class);
            } else {
                fail("Neither TestObserver failed");
            }
        }
    }

    @Test
    public void hasObservers() {
        UnicastSubject<Integer> us = UnicastSubject.create();

        assertFalse(us.hasObservers());

        TestObserver<Integer> to = us.test();

        assertTrue(us.hasObservers());

        to.dispose();

        assertFalse(us.hasObservers());
    }

    @Test
    public void drainFusedFailFast() {
        UnicastSubject<Integer> us = UnicastSubject.create(false);

        TestObserverEx<Integer> to = us.to(TestHelper.<Integer>testConsumer(QueueFuseable.ANY, false));

        us.done = true;
        us.drainFused(to);

        to.assertResult();
    }

    @Test
    public void drainFusedFailFastEmpty() {
        UnicastSubject<Integer> us = UnicastSubject.create(false);

        TestObserverEx<Integer> to = us.to(TestHelper.<Integer>testConsumer(QueueFuseable.ANY, false));

        us.drainFused(to);

        to.assertEmpty();
    }

    @Test
    public void fusedNoConcurrentCleanDueToCancel() {
        for (int j = 0; j < TestHelper.RACE_LONG_LOOPS; j++) {
            List<Throwable> errors = TestHelper.trackPluginErrors();
            try {
                final UnicastSubject<Integer> us = UnicastSubject.create();

                TestObserver<Integer> to = us
                .observeOn(Schedulers.io())
                .map(Functions.<Integer>identity())
                .observeOn(Schedulers.single())
                .firstOrError()
                .test();

                for (int i = 0; us.hasObservers(); i++) {
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
