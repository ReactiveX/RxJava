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
import io.reactivex.rxjava3.exceptions.TestException;
import io.reactivex.rxjava3.functions.Supplier;
import io.reactivex.rxjava3.operators.QueueFuseable;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subscribers.TestSubscriber;
import io.reactivex.rxjava3.testsupport.*;

public class FlowableFromRunnableTest extends RxJavaTest {
    @Test
    public void fromRunnable() {
        final AtomicInteger atomicInteger = new AtomicInteger();

        Flowable.fromRunnable(new Runnable() {
            @Override
            public void run() {
                atomicInteger.incrementAndGet();
            }
        })
            .test()
            .assertResult();

        assertEquals(1, atomicInteger.get());
    }

    @Test
    public void fromRunnableTwice() {
        final AtomicInteger atomicInteger = new AtomicInteger();

        Runnable run = new Runnable() {
            @Override
            public void run() {
                atomicInteger.incrementAndGet();
            }
        };

        Flowable.fromRunnable(run)
            .test()
            .assertResult();

        assertEquals(1, atomicInteger.get());

        Flowable.fromRunnable(run)
            .test()
            .assertResult();

        assertEquals(2, atomicInteger.get());
    }

    @Test
    public void fromRunnableInvokesLazy() {
        final AtomicInteger atomicInteger = new AtomicInteger();

        Flowable<Object> source = Flowable.fromRunnable(new Runnable() {
            @Override
            public void run() {
                atomicInteger.incrementAndGet();
            }
        });

        assertEquals(0, atomicInteger.get());

        source
            .test()
            .assertResult();

        assertEquals(1, atomicInteger.get());
    }

    @Test
    public void fromRunnableThrows() {
        Flowable.fromRunnable(new Runnable() {
            @Override
            public void run() {
                throw new UnsupportedOperationException();
            }
        })
            .test()
            .assertFailure(UnsupportedOperationException.class);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void callable() throws Throwable {
        final int[] counter = { 0 };

        Flowable<Void> m = Flowable.fromRunnable(new Runnable() {
            @Override
            public void run() {
                counter[0]++;
            }
        });

        assertTrue(m.getClass().toString(), m instanceof Supplier);

        assertNull(((Supplier<Void>)m).get());

        assertEquals(1, counter[0]);
    }

    @Test
    public void noErrorLoss() throws Exception {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            final CountDownLatch cdl1 = new CountDownLatch(1);
            final CountDownLatch cdl2 = new CountDownLatch(1);

            TestSubscriber<Object> ts = Flowable.fromRunnable(new Runnable() {
                @Override
                public void run() {
                    cdl1.countDown();
                    try {
                        cdl2.await(5, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        throw new TestException(e);
                    }
                }
            }).subscribeOn(Schedulers.single()).test();

            assertTrue(cdl1.await(5, TimeUnit.SECONDS));

            ts.cancel();

            int timeout = 10;

            while (timeout-- > 0 && errors.isEmpty()) {
                Thread.sleep(100);
            }

            TestHelper.assertUndeliverable(errors, 0, TestException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void disposedUpfront() throws Throwable {
        Runnable run = mock(Runnable.class);

        Flowable.fromRunnable(run)
        .test(1L, true)
        .assertEmpty();

        verify(run, never()).run();
    }

    @Test
    public void cancelWhileRunning() {
        final TestSubscriber<Object> ts = new TestSubscriber<>();

        Flowable.fromRunnable(new Runnable() {
            @Override
            public void run() {
                ts.cancel();
            }
        })
        .subscribeWith(ts)
        .assertEmpty();

        assertTrue(ts.isCancelled());
    }

    @Test
    public void asyncFused() throws Throwable {
        TestSubscriberEx<Object> ts = new TestSubscriberEx<>();
        ts.setInitialFusionMode(QueueFuseable.ASYNC);

        Runnable action = mock(Runnable.class);

        Flowable.fromRunnable(action)
        .subscribe(ts);

        ts.assertFusionMode(QueueFuseable.ASYNC)
        .assertResult();

        verify(action).run();
    }

    @Test
    public void syncFusedRejected() throws Throwable {
        TestSubscriberEx<Object> ts = new TestSubscriberEx<>();
        ts.setInitialFusionMode(QueueFuseable.SYNC);

        Runnable action = mock(Runnable.class);

        Flowable.fromRunnable(action)
        .subscribe(ts);

        ts.assertFusionMode(QueueFuseable.NONE)
        .assertResult();

        verify(action).run();
    }
}
