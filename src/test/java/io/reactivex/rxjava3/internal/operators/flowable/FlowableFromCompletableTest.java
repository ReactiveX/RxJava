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

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.internal.fuseable.*;
import io.reactivex.rxjava3.operators.QueueFuseable;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subscribers.TestSubscriber;
import io.reactivex.rxjava3.testsupport.*;

public class FlowableFromCompletableTest extends RxJavaTest {
    @Test
    public void fromCompletable() {
        final AtomicInteger atomicInteger = new AtomicInteger();

        Flowable.fromCompletable(Completable.fromAction(new Action() {
            @Override
            public void run() throws Exception {
                atomicInteger.incrementAndGet();
            }
        }))
            .test()
            .assertResult();

        assertEquals(1, atomicInteger.get());
    }

    @Test
    public void fromCompletableTwice() {
        final AtomicInteger atomicInteger = new AtomicInteger();

        Action run = new Action() {
            @Override
            public void run() throws Exception {
                atomicInteger.incrementAndGet();
            }
        };

        Flowable.fromCompletable(Completable.fromAction(run))
            .test()
            .assertResult();

        assertEquals(1, atomicInteger.get());

        Flowable.fromCompletable(Completable.fromAction(run))
            .test()
            .assertResult();

        assertEquals(2, atomicInteger.get());
    }

    @Test
    public void fromCompletableInvokesLazy() {
        final AtomicInteger atomicInteger = new AtomicInteger();

        Flowable<Object> source = Flowable.fromCompletable(Completable.fromAction(new Action() {
            @Override
            public void run() throws Exception {
                atomicInteger.incrementAndGet();
            }
        }));

        assertEquals(0, atomicInteger.get());

        source
            .test()
            .assertResult();

        assertEquals(1, atomicInteger.get());
    }

    @Test
    public void fromCompletableThrows() {
        Flowable.fromCompletable(Completable.fromAction(new Action() {
            @Override
            public void run() throws Exception {
                throw new UnsupportedOperationException();
            }
        }))
            .test()
            .assertFailure(UnsupportedOperationException.class);
    }

    @Test
    public void noErrorLoss() throws Exception {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            final CountDownLatch cdl1 = new CountDownLatch(1);
            final CountDownLatch cdl2 = new CountDownLatch(1);

            TestSubscriber<Object> ts = Flowable.fromCompletable(Completable.fromAction(new Action() {
                @Override
                public void run() throws Exception {
                    cdl1.countDown();
                    cdl2.await(5, TimeUnit.SECONDS);
                }
            }))
            .subscribeOn(Schedulers.single()).test();

            assertTrue(cdl1.await(5, TimeUnit.SECONDS));

            ts.cancel();

            int timeout = 10;

            while (timeout-- > 0 && errors.isEmpty()) {
                Thread.sleep(100);
            }

            TestHelper.assertUndeliverable(errors, 0, InterruptedException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void disposedUpfront() throws Throwable {
        Action run = mock(Action.class);

        Flowable.fromCompletable(Completable.fromAction(run))
        .test(1L, true)
        .assertEmpty();

        verify(run, never()).run();
    }

    @Test
    public void cancelWhileRunning() {
        final TestSubscriber<Object> ts = new TestSubscriber<>();

        Flowable.fromCompletable(Completable.fromAction(new Action() {
            @Override
            public void run() throws Exception {
                ts.cancel();
            }
        }))
        .subscribeWith(ts)
        .assertEmpty();

        assertTrue(ts.isCancelled());
    }

    @Test
    public void asyncFused() throws Throwable {
        TestSubscriberEx<Object> ts = new TestSubscriberEx<>();
        ts.setInitialFusionMode(QueueFuseable.ASYNC);

        Action action = mock(Action.class);

        Flowable.fromCompletable(Completable.fromAction(action))
        .subscribe(ts);

        ts.assertFusionMode(QueueFuseable.ASYNC)
        .assertResult();

        verify(action).run();
    }

    @Test
    public void syncFusedRejected() throws Throwable {
        TestSubscriberEx<Object> ts = new TestSubscriberEx<>();
        ts.setInitialFusionMode(QueueFuseable.SYNC);

        Action action = mock(Action.class);

        Flowable.fromCompletable(Completable.fromAction(action))
        .subscribe(ts);

        ts.assertFusionMode(QueueFuseable.NONE)
        .assertResult();

        verify(action).run();
    }

    @Test
    public void upstream() {
        Flowable<?> f = Flowable.fromCompletable(Completable.never());
        assertTrue(f instanceof HasUpstreamCompletableSource);
        assertSame(Completable.never(), ((HasUpstreamCompletableSource)f).source());
    }
}
