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
package io.reactivex.rxjava3.internal.util;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.disposables.*;
import io.reactivex.rxjava3.exceptions.TestException;
import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.rxjava3.testsupport.*;

public class HalfSerializerObserverTest extends RxJavaTest {

    @Test
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void reentrantOnNextOnNext() {
        final AtomicInteger wip = new AtomicInteger();
        final AtomicThrowable error = new AtomicThrowable();

        final Observer[] a = { null };

        final TestObserver to = new TestObserver();

        Observer observer = new Observer() {
            @Override
            public void onSubscribe(Disposable d) {
                to.onSubscribe(d);
            }

            @Override
            public void onNext(Object t) {
                if (t.equals(1)) {
                    HalfSerializer.onNext(a[0], 2, wip, error);
                }
                to.onNext(t);
            }

            @Override
            public void onError(Throwable t) {
                to.onError(t);
            }

            @Override
            public void onComplete() {
                to.onComplete();
            }
        };

        a[0] = observer;

        observer.onSubscribe(Disposable.empty());

        HalfSerializer.onNext(observer, 1, wip, error);

        to.assertValue(1).assertNoErrors().assertNotComplete();
    }

    @Test
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void reentrantOnNextOnError() {
        final AtomicInteger wip = new AtomicInteger();
        final AtomicThrowable error = new AtomicThrowable();

        final Observer[] a = { null };

        final TestObserver to = new TestObserver();

        Observer observer = new Observer() {
            @Override
            public void onSubscribe(Disposable d) {
                to.onSubscribe(d);
            }

            @Override
            public void onNext(Object t) {
                if (t.equals(1)) {
                    HalfSerializer.onError(a[0], new TestException(), wip, error);
                }
                to.onNext(t);
            }

            @Override
            public void onError(Throwable t) {
                to.onError(t);
            }

            @Override
            public void onComplete() {
                to.onComplete();
            }
        };

        a[0] = observer;

        observer.onSubscribe(Disposable.empty());

        HalfSerializer.onNext(observer, 1, wip, error);

        to.assertFailure(TestException.class, 1);
    }

    @Test
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void reentrantOnNextOnComplete() {
        final AtomicInteger wip = new AtomicInteger();
        final AtomicThrowable error = new AtomicThrowable();

        final Observer[] a = { null };

        final TestObserver to = new TestObserver();

        Observer observer = new Observer() {
            @Override
            public void onSubscribe(Disposable d) {
                to.onSubscribe(d);
            }

            @Override
            public void onNext(Object t) {
                if (t.equals(1)) {
                    HalfSerializer.onComplete(a[0], wip, error);
                }
                to.onNext(t);
            }

            @Override
            public void onError(Throwable t) {
                to.onError(t);
            }

            @Override
            public void onComplete() {
                to.onComplete();
            }
        };

        a[0] = observer;

        observer.onSubscribe(Disposable.empty());

        HalfSerializer.onNext(observer, 1, wip, error);

        to.assertResult(1);
    }

    @Test
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void reentrantErrorOnError() {
        final AtomicInteger wip = new AtomicInteger();
        final AtomicThrowable error = new AtomicThrowable();

        final Observer[] a = { null };

        final TestObserver to = new TestObserver();

        Observer observer = new Observer() {
            @Override
            public void onSubscribe(Disposable d) {
                to.onSubscribe(d);
            }

            @Override
            public void onNext(Object t) {
                to.onNext(t);
            }

            @Override
            public void onError(Throwable t) {
                to.onError(t);
                HalfSerializer.onError(a[0], new IOException(), wip, error);
            }

            @Override
            public void onComplete() {
                to.onComplete();
            }
        };

        a[0] = observer;

        observer.onSubscribe(Disposable.empty());

        HalfSerializer.onError(observer, new TestException(), wip, error);

        to.assertFailure(TestException.class);
    }

    @Test
    public void onNextOnCompleteRace() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {

            final AtomicInteger wip = new AtomicInteger();
            final AtomicThrowable error = new AtomicThrowable();

            final TestObserver<Integer> to = new TestObserver<>();
            to.onSubscribe(Disposable.empty());

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    HalfSerializer.onNext(to, 1, wip, error);
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    HalfSerializer.onComplete(to, wip, error);
                }
            };

            TestHelper.race(r1, r2);

            to.assertComplete().assertNoErrors();

            assertTrue(to.values().size() <= 1);
        }
    }

    @Test
    public void onErrorOnCompleteRace() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {

            final AtomicInteger wip = new AtomicInteger();
            final AtomicThrowable error = new AtomicThrowable();

            final TestObserverEx<Integer> to = new TestObserverEx<>();

            to.onSubscribe(Disposable.empty());

            final TestException ex = new TestException();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    HalfSerializer.onError(to, ex, wip, error);
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    HalfSerializer.onComplete(to, wip, error);
                }
            };

            TestHelper.race(r1, r2);

            if (to.completions() != 0) {
                to.assertResult();
            } else {
                to.assertFailure(TestException.class);
            }
        }
    }

}
