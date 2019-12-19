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

import java.io.IOException;
import java.util.List;

import org.junit.Test;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.disposables.*;
import io.reactivex.rxjava3.internal.util.ExceptionHelper;
import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.testsupport.TestHelper;

public class CompletableSubjectTest extends RxJavaTest {

    @Test
    public void once() {
        CompletableSubject cs = CompletableSubject.create();

        TestObserver<Void> to = cs.test();

        cs.onComplete();

        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            cs.onError(new IOException());

            TestHelper.assertUndeliverable(errors, 0, IOException.class);
        } finally {
            RxJavaPlugins.reset();
        }
        cs.onComplete();

        to.assertResult();
    }

    @Test
    public void error() {
        CompletableSubject cs = CompletableSubject.create();

        assertFalse(cs.hasComplete());
        assertFalse(cs.hasThrowable());
        assertNull(cs.getThrowable());
        assertFalse(cs.hasObservers());
        assertEquals(0, cs.observerCount());

        TestObserver<Void> to = cs.test();

        to.assertEmpty();

        assertTrue(cs.hasObservers());
        assertEquals(1, cs.observerCount());

        cs.onError(new IOException());

        assertFalse(cs.hasComplete());
        assertTrue(cs.hasThrowable());
        assertTrue(cs.getThrowable().toString(), cs.getThrowable() instanceof IOException);
        assertFalse(cs.hasObservers());
        assertEquals(0, cs.observerCount());

        to.assertFailure(IOException.class);

        cs.test().assertFailure(IOException.class);

        assertFalse(cs.hasComplete());
        assertTrue(cs.hasThrowable());
        assertTrue(cs.getThrowable().toString(), cs.getThrowable() instanceof IOException);
        assertFalse(cs.hasObservers());
        assertEquals(0, cs.observerCount());
    }

    @Test
    public void complete() {
        CompletableSubject cs = CompletableSubject.create();

        assertFalse(cs.hasComplete());
        assertFalse(cs.hasThrowable());
        assertNull(cs.getThrowable());
        assertFalse(cs.hasObservers());
        assertEquals(0, cs.observerCount());

        TestObserver<Void> to = cs.test();

        to.assertEmpty();

        assertTrue(cs.hasObservers());
        assertEquals(1, cs.observerCount());

        cs.onComplete();

        assertTrue(cs.hasComplete());
        assertFalse(cs.hasThrowable());
        assertNull(cs.getThrowable());
        assertFalse(cs.hasObservers());
        assertEquals(0, cs.observerCount());

        to.assertResult();

        cs.test().assertResult();

        assertTrue(cs.hasComplete());
        assertFalse(cs.hasThrowable());
        assertNull(cs.getThrowable());
        assertFalse(cs.hasObservers());
        assertEquals(0, cs.observerCount());
    }

    @Test
    public void nullThrowable() {
        CompletableSubject cs = CompletableSubject.create();

        try {
            cs.onError(null);
            fail("No NullPointerException thrown");
        } catch (NullPointerException ex) {
            assertEquals(ExceptionHelper.nullWarning("onError called with a null Throwable."), ex.getMessage());
        }

        cs.test().assertEmpty().dispose();
    }

    @Test
    public void cancelOnArrival() {
        CompletableSubject.create()
        .test(true)
        .assertEmpty();
    }

    @Test
    public void cancelOnArrival2() {
        CompletableSubject cs = CompletableSubject.create();

        cs.test();

        cs
        .test(true)
        .assertEmpty();
    }

    @Test
    public void dispose() {
        TestHelper.checkDisposed(CompletableSubject.create());
    }

    @Test
    public void disposeTwice() {
        CompletableSubject.create()
        .subscribe(new CompletableObserver() {
            @Override
            public void onSubscribe(Disposable d) {
                assertFalse(d.isDisposed());

                d.dispose();
                d.dispose();

                assertTrue(d.isDisposed());
            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onComplete() {

            }
        });
    }

    @Test
    public void onSubscribeDispose() {
        CompletableSubject cs = CompletableSubject.create();

        Disposable d = Disposable.empty();

        cs.onSubscribe(d);

        assertFalse(d.isDisposed());

        cs.onComplete();

        d = Disposable.empty();

        cs.onSubscribe(d);

        assertTrue(d.isDisposed());
    }

    @Test
    public void addRemoveRace() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            final CompletableSubject cs = CompletableSubject.create();

            final TestObserver<Void> to = cs.test();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    cs.test();
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
}
