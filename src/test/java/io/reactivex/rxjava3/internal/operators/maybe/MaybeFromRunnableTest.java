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

package io.reactivex.rxjava3.internal.operators.maybe;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.functions.Supplier;
import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.testsupport.TestHelper;

public class MaybeFromRunnableTest extends RxJavaTest {
    @Test(expected = NullPointerException.class)
    public void fromRunnableNull() {
        Maybe.fromRunnable(null);
    }

    @Test
    public void fromRunnable() {
        final AtomicInteger atomicInteger = new AtomicInteger();

        Maybe.fromRunnable(new Runnable() {
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

        Maybe.fromRunnable(run)
            .test()
            .assertResult();

        assertEquals(1, atomicInteger.get());

        Maybe.fromRunnable(run)
            .test()
            .assertResult();

        assertEquals(2, atomicInteger.get());
    }

    @Test
    public void fromRunnableInvokesLazy() {
        final AtomicInteger atomicInteger = new AtomicInteger();

        final Maybe<Object> maybe = Maybe.fromRunnable(new Runnable() {
            @Override public void run() {
                atomicInteger.incrementAndGet();
            }
        });

        assertEquals(0, atomicInteger.get());

        maybe
            .test()
            .assertResult();

        assertEquals(1, atomicInteger.get());
    }

    @Test
    public void fromRunnableThrows() {
        Maybe.fromRunnable(new Runnable() {
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

        Maybe<Void> m = Maybe.fromRunnable(new Runnable() {
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

            TestObserver<Object> to = Maybe.fromRunnable(new Runnable() {
                @Override
                public void run() {
                    cdl1.countDown();
                    try {
                        cdl2.await(5, TimeUnit.SECONDS);
                    } catch (InterruptedException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            }).subscribeOn(Schedulers.single()).test();

            assertTrue(cdl1.await(5, TimeUnit.SECONDS));

            to.dispose();

            int timeout = 10;

            while (timeout-- > 0 && errors.isEmpty()) {
                Thread.sleep(100);
            }

            TestHelper.assertUndeliverable(errors, 0, RuntimeException.class);

            assertTrue(errors.get(0).toString(), errors.get(0).getCause().getCause() instanceof InterruptedException);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void disposedUpfront() {
        Runnable run = mock(Runnable.class);

        Maybe.fromRunnable(run)
        .test(true)
        .assertEmpty();

        verify(run, never()).run();
    }

    @Test
    public void cancelWhileRunning() {
        final TestObserver<Object> to = new TestObserver<>();

        Maybe.fromRunnable(new Runnable() {
            @Override
            public void run() {
                to.dispose();
            }
        })
        .subscribeWith(to)
        .assertEmpty();

        assertTrue(to.isDisposed());
    }
}
