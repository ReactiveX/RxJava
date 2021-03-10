/*
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

package io.reactivex.rxjava3.internal.operators.completable;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.disposables.*;
import io.reactivex.rxjava3.exceptions.*;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.internal.functions.Functions;
import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.subjects.PublishSubject;
import io.reactivex.rxjava3.testsupport.*;

public class CompletableUsingTest extends RxJavaTest {

    @Test
    public void resourceSupplierThrows() {

        Completable.using(() -> {
            throw new TestException();
        }, v -> Completable.complete(), d -> {

        })
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void errorEager() {

        Completable.using((Supplier<Object>) () -> 1, v -> Completable.error(new TestException()), d -> {

        }, true)
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void emptyEager() {

        Completable.using((Supplier<Object>) () -> 1, v -> Completable.complete(), d -> {

        }, true)
        .test()
        .assertResult();
    }

    @Test
    public void errorNonEager() {

        Completable.using((Supplier<Object>) () -> 1, v -> Completable.error(new TestException()), d -> {

        }, false)
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void emptyNonEager() {

        Completable.using((Supplier<Object>) () -> 1, v -> Completable.complete(), d -> {

        }, false)
        .test()
        .assertResult();
    }

    @Test
    public void supplierCrashEager() {

        Completable.using((Supplier<Object>) () -> 1, v -> {
            throw new TestException();
        }, d -> {

        }, true)
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void supplierCrashNonEager() {

        Completable.using((Supplier<Object>) () -> 1, v -> {
            throw new TestException();
        }, d -> {

        }, false)
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void supplierAndDisposerCrashEager() {
        TestObserverEx<Void> to = Completable.using((Supplier<Object>) () -> 1, v -> {
            throw new TestException("Main");
        }, d -> {
            throw new TestException("Disposer");
        }, true)
        .to(TestHelper.<Void>testConsumer())
        .assertFailure(CompositeException.class);

        List<Throwable> list = TestHelper.compositeList(to.errors().get(0));

        TestHelper.assertError(list, 0, TestException.class, "Main");
        TestHelper.assertError(list, 1, TestException.class, "Disposer");
    }

    @Test
    public void supplierAndDisposerCrashNonEager() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            Completable.using((Supplier<Object>) () -> 1, v -> {
                throw new TestException("Main");
            }, d -> {
                throw new TestException("Disposer");
            }, false)
            .to(TestHelper.<Void>testConsumer())
            .assertFailureAndMessage(TestException.class, "Main");

            TestHelper.assertUndeliverable(errors, 0, TestException.class, "Disposer");
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void dispose() {
        final int[] call = {0 };

        TestObserverEx<Void> to = Completable.using((Supplier<Object>) () -> 1, v -> Completable.never(), d -> call[0]++, false)
        .to(TestHelper.<Void>testConsumer());

        to.dispose();

        assertEquals(1, call[0]);
    }

    @Test
    public void disposeCrashes() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            TestObserver<Void> to = Completable.using((Supplier<Object>) () -> 1, v -> Completable.never(), d -> {
                throw new TestException();
            }, false)
            .test();

            to.dispose();

            TestHelper.assertUndeliverable(errors, 0, TestException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void isDisposed() {
        TestHelper.checkDisposed(Completable.using((Supplier<Object>) () -> 1, v -> Completable.never(), d -> {

        }, false));
    }

    @Test
    public void justDisposerCrashes() {
        Completable.using((Supplier<Object>) () -> 1, v -> Completable.complete(), d -> {
            throw new TestException("Disposer");
        }, true)
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void emptyDisposerCrashes() {
        Completable.using((Supplier<Object>) () -> 1, v -> Completable.complete(), d -> {
            throw new TestException("Disposer");
        }, true)
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void errorDisposerCrash() {
        TestObserverEx<Void> to = Completable.using((Supplier<Object>) () -> 1, v -> Completable.error(new TestException("Main")), d -> {
            throw new TestException("Disposer");
        }, true)
        .to(TestHelper.<Void>testConsumer())
        .assertFailure(CompositeException.class);

        List<Throwable> list = TestHelper.compositeList(to.errors().get(0));

        TestHelper.assertError(list, 0, TestException.class, "Main");
        TestHelper.assertError(list, 1, TestException.class, "Disposer");
    }

    @Test
    public void doubleOnSubscribe() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            Completable.using((Supplier<Object>) () -> 1, v -> Completable.wrap(observer -> {
                Disposable d1 = Disposable.empty();

                observer.onSubscribe(d1);

                Disposable d2 = Disposable.empty();

                observer.onSubscribe(d2);

                assertFalse(d1.isDisposed());

                assertTrue(d2.isDisposed());
            }), d -> {

                }, false).test();
            TestHelper.assertError(errors, 0, IllegalStateException.class, "Disposable already set!");
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void successDisposeRace() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {

            final PublishSubject<Integer> ps = PublishSubject.create();

            final TestObserverEx<Void> to = Completable.using((Supplier<Object>) () -> 1, v -> ps.ignoreElements(), d -> {
            }, true)
            .to(TestHelper.<Void>testConsumer());

            ps.onNext(1);

            Runnable r1 = to::dispose;

            Runnable r2 = ps::onComplete;

            TestHelper.race(r1, r2);
        }
    }

    @Test
    public void errorDisposeRace() {
        RxJavaPlugins.setErrorHandler(Functions.emptyConsumer());
        try {
            for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {

                final PublishSubject<Integer> ps = PublishSubject.create();

                final TestObserver<Void> to = Completable.using((Supplier<Object>) () -> 1, v -> ps.ignoreElements(), d -> {
                }, true)
                .test();

                final TestException ex = new TestException();

                Runnable r1 = to::dispose;

                Runnable r2 = () -> ps.onError(ex);

                TestHelper.race(r1, r2);
            }
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void emptyDisposeRace() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {

            final PublishSubject<Integer> ps = PublishSubject.create();

            final TestObserver<Void> to = Completable.using((Supplier<Object>) () -> 1, v -> ps.ignoreElements(), d -> {

            }, true)
            .test();

            Runnable r1 = to::dispose;

            Runnable r2 = ps::onComplete;

            TestHelper.race(r1, r2);
        }
    }

    @Test
    public void eagerDisposeResourceThenDisposeUpstream() {
        final StringBuilder sb = new StringBuilder();

        TestObserver<Void> to = Completable.using(Functions.justSupplier(1),
                (Function<Integer, Completable>) t -> Completable.never()
                        .doOnDispose(() -> sb.append("Dispose")), t -> sb.append("Resource"), true)
        .test()
        ;
        to.assertEmpty();

        to.dispose();

        assertEquals("ResourceDispose", sb.toString());
    }

    @Test
    public void nonEagerDisposeUpstreamThenDisposeResource() {
        final StringBuilder sb = new StringBuilder();

        TestObserver<Void> to = Completable.using(Functions.justSupplier(1),
                (Function<Integer, Completable>) t -> Completable.never()
                        .doOnDispose(() -> sb.append("Dispose")), t -> sb.append("Resource"), false)
        .test()
        ;
        to.assertEmpty();

        to.dispose();

        assertEquals("DisposeResource", sb.toString());
    }
}
