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

package io.reactivex.internal.operators.maybe;

import static org.junit.Assert.*;

import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import io.reactivex.*;
import io.reactivex.functions.Action;
import io.reactivex.observers.TestObserver;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.schedulers.Schedulers;

public class MaybeFromActionTest {
    @Test(expected = NullPointerException.class)
    public void fromActionNull() {
        Maybe.fromAction(null);
    }

    @Test
    public void fromAction() {
        final AtomicInteger atomicInteger = new AtomicInteger();

        Maybe.fromAction(new Action() {
            @Override
            public void run() throws Exception {
                atomicInteger.incrementAndGet();
            }
        })
            .test()
            .assertResult();

        assertEquals(1, atomicInteger.get());
    }

    @Test
    public void fromActionTwice() {
        final AtomicInteger atomicInteger = new AtomicInteger();

        Action run = new Action() {
            @Override
            public void run() throws Exception {
                atomicInteger.incrementAndGet();
            }
        };

        Maybe.fromAction(run)
            .test()
            .assertResult();

        assertEquals(1, atomicInteger.get());

        Maybe.fromAction(run)
            .test()
            .assertResult();

        assertEquals(2, atomicInteger.get());
    }

    @Test
    public void fromActionInvokesLazy() {
        final AtomicInteger atomicInteger = new AtomicInteger();

        Maybe<Object> maybe = Maybe.fromAction(new Action() {
            @Override
            public void run() throws Exception {
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
    public void fromActionThrows() {
        Maybe.fromAction(new Action() {
            @Override
            public void run() throws Exception {
                throw new UnsupportedOperationException();
            }
        })
            .test()
            .assertFailure(UnsupportedOperationException.class);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void callable() throws Exception {
        final int[] counter = { 0 };

        Maybe<Void> m = Maybe.fromAction(new Action() {
            @Override
            public void run() throws Exception {
                counter[0]++;
            }
        });

        assertTrue(m.getClass().toString(), m instanceof Callable);

        assertNull(((Callable<Void>)m).call());

        assertEquals(1, counter[0]);
    }

    @Test
    public void noErrorLoss() throws Exception {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            final CountDownLatch cdl1 = new CountDownLatch(1);
            final CountDownLatch cdl2 = new CountDownLatch(1);

            TestObserver<Object> to = Maybe.fromAction(new Action() {
                @Override
                public void run() throws Exception {
                    cdl1.countDown();
                    cdl2.await();
                }
            }).subscribeOn(Schedulers.single()).test();

            assertTrue(cdl1.await(5, TimeUnit.SECONDS));

            to.cancel();

            cdl2.countDown();

            int timeout = 10;

            while (timeout-- > 0 && errors.isEmpty()) {
                Thread.sleep(100);
            }

            TestHelper.assertUndeliverable(errors, 0, InterruptedException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }
}
