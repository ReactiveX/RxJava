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

package io.reactivex.rxjava3.disposables;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.*;

import org.junit.Test;
import org.reactivestreams.Subscription;

import io.reactivex.rxjava3.core.RxJavaTest;
import io.reactivex.rxjava3.exceptions.TestException;
import io.reactivex.rxjava3.functions.Action;
import io.reactivex.rxjava3.internal.disposables.DisposableHelper;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.testsupport.TestHelper;

public class DisposableTest extends RxJavaTest {

    @Test
    public void unsubscribeOnlyOnce() {
        Runnable run = mock(Runnable.class);

        Disposable d = Disposable.fromRunnable(run);

        assertTrue(d.toString(), d.toString().contains("RunnableDisposable(disposed=false, "));

        d.dispose();
        assertTrue(d.toString(), d.toString().contains("RunnableDisposable(disposed=true, "));

        d.dispose();
        assertTrue(d.toString(), d.toString().contains("RunnableDisposable(disposed=true, "));

        verify(run, times(1)).run();
    }

    @Test
    public void empty() {
        Disposable empty = Disposable.empty();
        assertFalse(empty.isDisposed());
        empty.dispose();
        assertTrue(empty.isDisposed());
    }

    @Test
    public void unsubscribed() {
        Disposable disposed = Disposable.disposed();
        assertTrue(disposed.isDisposed());
    }

    @Test
    public void fromAction() throws Throwable {
        Action action = mock(Action.class);

        Disposable d = Disposable.fromAction(action);

        assertTrue(d.toString(), d.toString().contains("ActionDisposable(disposed=false, "));

        d.dispose();
        assertTrue(d.toString(), d.toString().contains("ActionDisposable(disposed=true, "));

        d.dispose();
        assertTrue(d.toString(), d.toString().contains("ActionDisposable(disposed=true, "));

        verify(action, times(1)).run();
    }

    @Test
    public void fromActionThrows() {
        try {
            Disposable.fromAction(new Action() {
                @Override
                public void run() throws Exception {
                    throw new IllegalArgumentException();
                }
            }).dispose();
            fail("Should have thrown!");
        } catch (IllegalArgumentException ex) {
            // expected
        }

        try {
            Disposable.fromAction(new Action() {
                @Override
                public void run() throws Exception {
                    throw new InternalError();
                }
            }).dispose();
            fail("Should have thrown!");
        } catch (InternalError ex) {
            // expected
        }

        try {
            Disposable.fromAction(new Action() {
                @Override
                public void run() throws Exception {
                    throw new IOException();
                }
            }).dispose();
            fail("Should have thrown!");
        } catch (RuntimeException ex) {
            if (!(ex.getCause() instanceof IOException)) {
                fail(ex.toString() + ": Should have cause of IOException");
            }
            // expected
        }

    }

    @Test
    public void disposeRace() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            final Disposable d = Disposable.empty();

            Runnable r = new Runnable() {
                @Override
                public void run() {
                    d.dispose();
                }
            };

            TestHelper.race(r, r);
        }
    }

    @Test(expected = NullPointerException.class)
    public void fromSubscriptionNull() {
        Disposable.fromSubscription(null);
    }

    @Test
    public void fromSubscription() {
        Subscription s = mock(Subscription.class);

        Disposable.fromSubscription(s).dispose();

        verify(s).cancel();
        verify(s, never()).request(anyInt());
    }

    @Test
    public void setOnceTwice() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {

            AtomicReference<Disposable> target = new AtomicReference<>();
            Disposable d = Disposable.empty();

            DisposableHelper.setOnce(target, d);

            Disposable d1 = Disposable.empty();

            DisposableHelper.setOnce(target, d1);

            assertTrue(d1.isDisposed());

            TestHelper.assertError(errors, 0, IllegalStateException.class, "Disposable already set!");
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void fromAutoCloseable() {
        AtomicInteger counter = new AtomicInteger();

        AutoCloseable ac = () -> counter.getAndIncrement();

        Disposable d = Disposable.fromAutoCloseable(ac);

        assertFalse(d.isDisposed());
        assertEquals(0, counter.get());
        assertTrue(d.toString(), d.toString().contains("AutoCloseableDisposable(disposed=false, "));

        d.dispose();

        assertTrue(d.isDisposed());
        assertEquals(1, counter.get());
        assertTrue(d.toString(), d.toString().contains("AutoCloseableDisposable(disposed=true, "));

        d.dispose();

        assertTrue(d.isDisposed());
        assertEquals(1, counter.get());
        assertTrue(d.toString(), d.toString().contains("AutoCloseableDisposable(disposed=true, "));
    }

    @Test
    public void fromAutoCloseableThrows() throws Throwable {
        TestHelper.withErrorTracking(errors -> {
            AutoCloseable ac = () -> { throw new TestException(); };

            Disposable d = Disposable.fromAutoCloseable(ac);

            assertFalse(d.isDisposed());

            assertTrue(errors.isEmpty());

            d.dispose();

            assertTrue(d.isDisposed());
            assertEquals(1, errors.size());

            d.dispose();

            assertTrue(d.isDisposed());
            assertEquals(1, errors.size());

            TestHelper.assertUndeliverable(errors, 0, TestException.class);
        });
    }

    @Test
    public void toAutoCloseable() throws Exception {
        AtomicInteger counter = new AtomicInteger();

        Disposable d = Disposable.fromAction(() -> counter.getAndIncrement());

        AutoCloseable ac = Disposable.toAutoCloseable(d);

        assertFalse(d.isDisposed());
        assertEquals(0, counter.get());

        ac.close();

        assertTrue(d.isDisposed());
        assertEquals(1, counter.get());

        ac.close();

        assertTrue(d.isDisposed());
        assertEquals(1, counter.get());
    }
}
