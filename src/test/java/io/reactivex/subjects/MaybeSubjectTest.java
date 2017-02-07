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

package io.reactivex.subjects;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.List;

import org.junit.Test;

import io.reactivex.*;
import io.reactivex.disposables.*;
import io.reactivex.observers.TestObserver;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.schedulers.Schedulers;

public class MaybeSubjectTest {

    @Test
    public void success() {
        MaybeSubject<Integer> ms = MaybeSubject.create();

        assertFalse(ms.hasValue());
        assertNull(ms.getValue());
        assertFalse(ms.hasComplete());
        assertFalse(ms.hasThrowable());
        assertNull(ms.getThrowable());
        assertFalse(ms.hasObservers());
        assertEquals(0, ms.observerCount());

        TestObserver<Integer> to = ms.test();

        to.assertEmpty();

        assertTrue(ms.hasObservers());
        assertEquals(1, ms.observerCount());

        ms.onSuccess(1);

        assertTrue(ms.hasValue());
        assertEquals(1, ms.getValue().intValue());
        assertFalse(ms.hasComplete());
        assertFalse(ms.hasThrowable());
        assertNull(ms.getThrowable());
        assertFalse(ms.hasObservers());
        assertEquals(0, ms.observerCount());

        to.assertResult(1);

        ms.test().assertResult(1);

        assertTrue(ms.hasValue());
        assertEquals(1, ms.getValue().intValue());
        assertFalse(ms.hasComplete());
        assertFalse(ms.hasThrowable());
        assertNull(ms.getThrowable());
        assertFalse(ms.hasObservers());
        assertEquals(0, ms.observerCount());
    }

    @Test
    public void once() {
        MaybeSubject<Integer> ms = MaybeSubject.create();

        TestObserver<Integer> to = ms.test();

        ms.onSuccess(1);
        ms.onSuccess(2);

        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            ms.onError(new IOException());

            TestHelper.assertUndeliverable(errors, 0, IOException.class);
        } finally {
            RxJavaPlugins.reset();
        }
        ms.onComplete();

        to.assertResult(1);
    }

    @Test
    public void error() {
        MaybeSubject<Integer> ms = MaybeSubject.create();

        assertFalse(ms.hasValue());
        assertNull(ms.getValue());
        assertFalse(ms.hasComplete());
        assertFalse(ms.hasThrowable());
        assertNull(ms.getThrowable());
        assertFalse(ms.hasObservers());
        assertEquals(0, ms.observerCount());

        TestObserver<Integer> to = ms.test();

        to.assertEmpty();

        assertTrue(ms.hasObservers());
        assertEquals(1, ms.observerCount());

        ms.onError(new IOException());

        assertFalse(ms.hasValue());
        assertNull(ms.getValue());
        assertFalse(ms.hasComplete());
        assertTrue(ms.hasThrowable());
        assertTrue(ms.getThrowable().toString(), ms.getThrowable() instanceof IOException);
        assertFalse(ms.hasObservers());
        assertEquals(0, ms.observerCount());

        to.assertFailure(IOException.class);

        ms.test().assertFailure(IOException.class);

        assertFalse(ms.hasValue());
        assertNull(ms.getValue());
        assertFalse(ms.hasComplete());
        assertTrue(ms.hasThrowable());
        assertTrue(ms.getThrowable().toString(), ms.getThrowable() instanceof IOException);
        assertFalse(ms.hasObservers());
        assertEquals(0, ms.observerCount());
    }

    @Test
    public void complete() {
        MaybeSubject<Integer> ms = MaybeSubject.create();

        assertFalse(ms.hasValue());
        assertNull(ms.getValue());
        assertFalse(ms.hasComplete());
        assertFalse(ms.hasThrowable());
        assertNull(ms.getThrowable());
        assertFalse(ms.hasObservers());
        assertEquals(0, ms.observerCount());

        TestObserver<Integer> to = ms.test();

        to.assertEmpty();

        assertTrue(ms.hasObservers());
        assertEquals(1, ms.observerCount());

        ms.onComplete();

        assertFalse(ms.hasValue());
        assertNull(ms.getValue());
        assertTrue(ms.hasComplete());
        assertFalse(ms.hasThrowable());
        assertNull(ms.getThrowable());
        assertFalse(ms.hasObservers());
        assertEquals(0, ms.observerCount());

        to.assertResult();

        ms.test().assertResult();

        assertFalse(ms.hasValue());
        assertNull(ms.getValue());
        assertTrue(ms.hasComplete());
        assertFalse(ms.hasThrowable());
        assertNull(ms.getThrowable());
        assertFalse(ms.hasObservers());
        assertEquals(0, ms.observerCount());
    }

    @Test
    public void nullValue() {
        MaybeSubject<Integer> ms = MaybeSubject.create();

        TestObserver<Integer> to = ms.test();

        ms.onSuccess(null);

        to.assertFailure(NullPointerException.class);
    }

    @Test
    public void nullThrowable() {
        MaybeSubject<Integer> ms = MaybeSubject.create();

        TestObserver<Integer> to = ms.test();

        ms.onError(null);

        to.assertFailure(NullPointerException.class);
    }

    @Test
    public void cancelOnArrival() {
        MaybeSubject.create()
        .test(true)
        .assertEmpty();
    }

    @Test
    public void cancelOnArrival2() {
        MaybeSubject<Integer> ms = MaybeSubject.create();

        ms.test();

        ms
        .test(true)
        .assertEmpty();
    }

    @Test
    public void dispose() {
        TestHelper.checkDisposed(MaybeSubject.create());
    }

    @Test
    public void disposeTwice() {
        MaybeSubject.create()
        .subscribe(new MaybeObserver<Object>() {
            @Override
            public void onSubscribe(Disposable d) {
                assertFalse(d.isDisposed());

                d.dispose();
                d.dispose();

                assertTrue(d.isDisposed());
            }

            @Override
            public void onSuccess(Object value) {

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
        MaybeSubject<Integer> ms = MaybeSubject.create();

        Disposable d = Disposables.empty();

        ms.onSubscribe(d);

        assertFalse(d.isDisposed());

        ms.onComplete();

        d = Disposables.empty();

        ms.onSubscribe(d);

        assertTrue(d.isDisposed());
    }

    @Test
    public void addRemoveRace() {
        for (int i = 0; i < 500; i++) {
            final MaybeSubject<Integer> ms = MaybeSubject.create();

            final TestObserver<Integer> to = ms.test();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    ms.test();
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    to.cancel();
                }
            };
            TestHelper.race(r1, r2, Schedulers.single());
        }
    }
}
