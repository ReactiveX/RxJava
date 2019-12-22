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

package io.reactivex.rxjava3.internal.disposables;

import static org.junit.Assert.*;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

import io.reactivex.rxjava3.core.RxJavaTest;
import io.reactivex.rxjava3.disposables.*;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.testsupport.TestHelper;

public class DisposableHelperTest extends RxJavaTest {
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
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            final AtomicReference<Disposable> d = new AtomicReference<>();

            Runnable r = new Runnable() {
                @Override
                public void run() {
                    DisposableHelper.dispose(d);
                }
            };

            TestHelper.race(r, r);
        }
    }

    @Test
    public void setReplace() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            final AtomicReference<Disposable> d = new AtomicReference<>();

            Runnable r = new Runnable() {
                @Override
                public void run() {
                    DisposableHelper.replace(d, Disposable.empty());
                }
            };

            TestHelper.race(r, r);
        }
    }

    @Test
    public void setRace() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            final AtomicReference<Disposable> d = new AtomicReference<>();

            Runnable r = new Runnable() {
                @Override
                public void run() {
                    DisposableHelper.set(d, Disposable.empty());
                }
            };

            TestHelper.race(r, r);
        }
    }

    @Test
    public void setReplaceNull() {
        final AtomicReference<Disposable> d = new AtomicReference<>();

        DisposableHelper.dispose(d);

        assertFalse(DisposableHelper.set(d, null));
        assertFalse(DisposableHelper.replace(d, null));
    }

    @Test
    public void dispose() {
        Disposable u = Disposable.empty();
        final AtomicReference<Disposable> d = new AtomicReference<>(u);

        DisposableHelper.dispose(d);

        assertTrue(u.isDisposed());
    }

    @Test
    public void trySet() {
        AtomicReference<Disposable> ref = new AtomicReference<>();

        Disposable d1 = Disposable.empty();

        assertTrue(DisposableHelper.trySet(ref, d1));

        Disposable d2 = Disposable.empty();

        assertFalse(DisposableHelper.trySet(ref, d2));

        assertFalse(d1.isDisposed());

        assertFalse(d2.isDisposed());

        DisposableHelper.dispose(ref);

        Disposable d3 = Disposable.empty();

        assertFalse(DisposableHelper.trySet(ref, d3));

        assertTrue(d3.isDisposed());
    }
}
