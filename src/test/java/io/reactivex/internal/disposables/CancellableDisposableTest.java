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

package io.reactivex.internal.disposables;

import static org.junit.Assert.*;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import io.reactivex.TestHelper;
import io.reactivex.exceptions.TestException;
import io.reactivex.functions.Cancellable;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.schedulers.Schedulers;

public class CancellableDisposableTest {

    @Test
    public void normal() {
        final AtomicInteger count = new AtomicInteger();

        Cancellable c = new Cancellable() {
            @Override
            public void cancel() throws Exception {
                count.getAndIncrement();
            }
        };

        CancellableDisposable cd = new CancellableDisposable(c);

        assertFalse(cd.isDisposed());

        cd.dispose();
        cd.dispose();

        assertTrue(cd.isDisposed());

        assertEquals(1, count.get());
    }

    @Test
    public void cancelThrows() {
        final AtomicInteger count = new AtomicInteger();

        Cancellable c = new Cancellable() {
            @Override
            public void cancel() throws Exception {
                count.getAndIncrement();
                throw new TestException();
            }
        };

        CancellableDisposable cd = new CancellableDisposable(c);

        assertFalse(cd.isDisposed());

        List<Throwable> list = TestHelper.trackPluginErrors();
        try {
            cd.dispose();
            cd.dispose();

            TestHelper.assertUndeliverable(list, 0, TestException.class);
        } finally {
            RxJavaPlugins.reset();
        }
        assertTrue(cd.isDisposed());

        assertEquals(1, count.get());
    }

    @Test
    public void disposeRace() {

        for (int i = 0; i < 100; i++) {
            final AtomicInteger count = new AtomicInteger();

            Cancellable c = new Cancellable() {
                @Override
                public void cancel() throws Exception {
                    count.getAndIncrement();
                }
            };

            final CancellableDisposable cd = new CancellableDisposable(c);

            Runnable r = new Runnable() {
                @Override
                public void run() {
                    cd.dispose();
                }
            };

            TestHelper.race(r, r, Schedulers.io());

            assertEquals(1, count.get());
        }
    }

}
