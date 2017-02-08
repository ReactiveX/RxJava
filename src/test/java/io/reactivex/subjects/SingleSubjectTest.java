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

public class SingleSubjectTest {

    @Test
    public void success() {
        SingleSubject<Integer> ss = SingleSubject.create();

        assertFalse(ss.hasValue());
        assertNull(ss.getValue());
        assertFalse(ss.hasThrowable());
        assertNull(ss.getThrowable());
        assertFalse(ss.hasObservers());
        assertEquals(0, ss.observerCount());

        TestObserver<Integer> to = ss.test();

        to.assertEmpty();

        assertTrue(ss.hasObservers());
        assertEquals(1, ss.observerCount());

        ss.onSuccess(1);

        assertTrue(ss.hasValue());
        assertEquals(1, ss.getValue().intValue());
        assertFalse(ss.hasThrowable());
        assertNull(ss.getThrowable());
        assertFalse(ss.hasObservers());
        assertEquals(0, ss.observerCount());

        to.assertResult(1);

        ss.test().assertResult(1);

        assertTrue(ss.hasValue());
        assertEquals(1, ss.getValue().intValue());
        assertFalse(ss.hasThrowable());
        assertNull(ss.getThrowable());
        assertFalse(ss.hasObservers());
        assertEquals(0, ss.observerCount());
    }

    @Test
    public void once() {
        SingleSubject<Integer> ss = SingleSubject.create();

        TestObserver<Integer> to = ss.test();

        ss.onSuccess(1);
        ss.onSuccess(2);

        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            ss.onError(new IOException());

            TestHelper.assertUndeliverable(errors, 0, IOException.class);
        } finally {
            RxJavaPlugins.reset();
        }

        to.assertResult(1);
    }

    @Test
    public void error() {
        SingleSubject<Integer> ss = SingleSubject.create();

        assertFalse(ss.hasValue());
        assertNull(ss.getValue());
        assertFalse(ss.hasThrowable());
        assertNull(ss.getThrowable());
        assertFalse(ss.hasObservers());
        assertEquals(0, ss.observerCount());

        TestObserver<Integer> to = ss.test();

        to.assertEmpty();

        assertTrue(ss.hasObservers());
        assertEquals(1, ss.observerCount());

        ss.onError(new IOException());

        assertFalse(ss.hasValue());
        assertNull(ss.getValue());
        assertTrue(ss.hasThrowable());
        assertTrue(ss.getThrowable().toString(), ss.getThrowable() instanceof IOException);
        assertFalse(ss.hasObservers());
        assertEquals(0, ss.observerCount());

        to.assertFailure(IOException.class);

        ss.test().assertFailure(IOException.class);

        assertFalse(ss.hasValue());
        assertNull(ss.getValue());
        assertTrue(ss.hasThrowable());
        assertTrue(ss.getThrowable().toString(), ss.getThrowable() instanceof IOException);
        assertFalse(ss.hasObservers());
        assertEquals(0, ss.observerCount());
    }

    @Test
    public void nullValue() {
        SingleSubject<Integer> ss = SingleSubject.create();

        TestObserver<Integer> to = ss.test();

        ss.onSuccess(null);

        to.assertFailure(NullPointerException.class);
    }

    @Test
    public void nullThrowable() {
        SingleSubject<Integer> ss = SingleSubject.create();

        TestObserver<Integer> to = ss.test();

        ss.onError(null);

        to.assertFailure(NullPointerException.class);
    }

    @Test
    public void cancelOnArrival() {
        SingleSubject.create()
        .test(true)
        .assertEmpty();
    }

    @Test
    public void cancelOnArrival2() {
        SingleSubject<Integer> ss = SingleSubject.create();

        ss.test();

        ss
        .test(true)
        .assertEmpty();
    }

    @Test
    public void dispose() {
        TestHelper.checkDisposed(SingleSubject.create());
    }

    @Test
    public void disposeTwice() {
        SingleSubject.create()
        .subscribe(new SingleObserver<Object>() {
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
        });
    }

    @Test
    public void onSubscribeDispose() {
        SingleSubject<Integer> ss = SingleSubject.create();

        Disposable d = Disposables.empty();

        ss.onSubscribe(d);

        assertFalse(d.isDisposed());

        ss.onSuccess(1);

        d = Disposables.empty();

        ss.onSubscribe(d);

        assertTrue(d.isDisposed());
    }

    @Test
    public void addRemoveRace() {
        for (int i = 0; i < 500; i++) {
            final SingleSubject<Integer> ss = SingleSubject.create();

            final TestObserver<Integer> to = ss.test();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    ss.test();
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
