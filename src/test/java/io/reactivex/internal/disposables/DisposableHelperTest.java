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
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

import io.reactivex.TestHelper;
import io.reactivex.disposables.*;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.schedulers.Schedulers;

public class DisposableHelperTest {
    @Test
    public void enumMethods() {
        assertEquals(1, DisposableHelper.values().length);
        assertNotNull(DisposableHelper.valueOf("DISPOSED"));
    }

    @Test
    public void innerDisposed() {
        assertTrue(DisposableHelper.DISPOSED.isDisposed());
        DisposableHelper.DISPOSED.dispose();
        assertTrue(DisposableHelper.DISPOSED.isDisposed());
    }

    @Test
    public void validationNull() {
        List<Throwable> list = TestHelper.trackPluginErrors();
        try {
            assertFalse(DisposableHelper.validate(null, null));

            TestHelper.assertError(list, 0, NullPointerException.class, "next is null");
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void disposeRace() {
        for (int i = 0; i < 500; i++) {
            final AtomicReference<Disposable> d = new AtomicReference<Disposable>();

            Runnable r = new Runnable() {
                @Override
                public void run() {
                    DisposableHelper.dispose(d);
                }
            };

            TestHelper.race(r, r, Schedulers.io());
        }
    }

    @Test
    public void setReplace() {
        for (int i = 0; i < 500; i++) {
            final AtomicReference<Disposable> d = new AtomicReference<Disposable>();

            Runnable r = new Runnable() {
                @Override
                public void run() {
                    DisposableHelper.replace(d, Disposables.empty());
                }
            };

            TestHelper.race(r, r, Schedulers.io());
        }
    }

    @Test
    public void setRace() {
        for (int i = 0; i < 500; i++) {
            final AtomicReference<Disposable> d = new AtomicReference<Disposable>();

            Runnable r = new Runnable() {
                @Override
                public void run() {
                    DisposableHelper.set(d, Disposables.empty());
                }
            };

            TestHelper.race(r, r, Schedulers.io());
        }
    }

    @Test
    public void setReplaceNull() {
        final AtomicReference<Disposable> d = new AtomicReference<Disposable>();

        DisposableHelper.dispose(d);

        assertFalse(DisposableHelper.set(d, null));
        assertFalse(DisposableHelper.replace(d, null));
    }

    @Test
    public void dispose() {
        Disposable u = Disposables.empty();
        final AtomicReference<Disposable> d = new AtomicReference<Disposable>(u);

        DisposableHelper.dispose(d);

        assertTrue(u.isDisposed());
    }

    @Test
    public void trySet() {
        AtomicReference<Disposable> ref = new AtomicReference<Disposable>();

        Disposable d1 = Disposables.empty();

        assertTrue(DisposableHelper.trySet(ref, d1));

        Disposable d2 = Disposables.empty();

        assertFalse(DisposableHelper.trySet(ref, d2));

        assertFalse(d1.isDisposed());

        assertFalse(d2.isDisposed());

        DisposableHelper.dispose(ref);

        Disposable d3 = Disposables.empty();

        assertFalse(DisposableHelper.trySet(ref, d3));

        assertTrue(d3.isDisposed());
    }
}
