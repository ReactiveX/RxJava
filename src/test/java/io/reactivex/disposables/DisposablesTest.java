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

package io.reactivex.disposables;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.*;

import org.junit.Test;
import org.reactivestreams.Subscription;

import io.reactivex.TestHelper;
import io.reactivex.functions.Action;
import io.reactivex.internal.disposables.DisposableHelper;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.schedulers.Schedulers;

public class DisposablesTest {

    @Test
    public void testUnsubscribeOnlyOnce() {
        Runnable dispose = mock(Runnable.class);
        Disposable subscription = Disposables.fromRunnable(dispose);
        subscription.dispose();
        subscription.dispose();
        verify(dispose, times(1)).run();
    }

    @Test
    public void testEmpty() {
        Disposable empty = Disposables.empty();
        assertFalse(empty.isDisposed());
        empty.dispose();
        assertTrue(empty.isDisposed());
    }

    @Test
    public void testUnsubscribed() {
        Disposable disposed = Disposables.disposed();
        assertTrue(disposed.isDisposed());
    }

    @Test
    public void utilityClass() {
        TestHelper.checkUtilityClass(Disposables.class);
    }

    @Test
    public void fromAction() {
        class AtomicAction extends AtomicBoolean implements Action {

            private static final long serialVersionUID = -1517510584253657229L;

            @Override
            public void run() throws Exception {
                set(true);
            }
        }

        AtomicAction aa = new AtomicAction();

        Disposables.fromAction(aa).dispose();

        assertTrue(aa.get());
    }

    @Test
    public void fromActionThrows() {
        try {
            Disposables.fromAction(new Action() {
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
            Disposables.fromAction(new Action() {
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
            Disposables.fromAction(new Action() {
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
        for (int i = 0; i < 100; i++) {
            final Disposable d = Disposables.empty();

            Runnable r = new Runnable() {
                @Override
                public void run() {
                    d.dispose();
                }
            };

            TestHelper.race(r, r, Schedulers.io());
        }
    }

    @Test(expected = NullPointerException.class)
    public void fromSubscriptionNull() {
        Disposables.fromSubscription(null);
    }

    @Test
    public void fromSubscription() {
        Subscription s = mock(Subscription.class);

        Disposables.fromSubscription(s).dispose();

        verify(s).cancel();
        verify(s, never()).request(anyInt());
    }

    @Test
    public void setOnceTwice() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {

            AtomicReference<Disposable> target = new AtomicReference<Disposable>();
            Disposable d = Disposables.empty();

            DisposableHelper.setOnce(target, d);

            Disposable d1 = Disposables.empty();

            DisposableHelper.setOnce(target, d1);

            assertTrue(d1.isDisposed());

            TestHelper.assertError(errors, 0, IllegalStateException.class, "Disposable already set!");
        } finally {
            RxJavaPlugins.reset();
        }
    }
}
