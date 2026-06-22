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

package io.reactivex.rxjava4.internal.operators.maybe;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;

import io.reactivex.rxjava4.core.*;
import io.reactivex.rxjava4.disposables.*;
import io.reactivex.rxjava4.exceptions.*;
import io.reactivex.rxjava4.functions.*;
import io.reactivex.rxjava4.internal.functions.Functions;
import io.reactivex.rxjava4.observers.TestObserver;
import io.reactivex.rxjava4.plugins.RxJavaPlugins;
import io.reactivex.rxjava4.subjects.PublishSubject;
import io.reactivex.rxjava4.testsupport.*;

public class MaybeUsingTest extends RxJavaTest {

    @Test
    public void resourceSupplierThrows() {

        Maybe.using(() -> {
            throw new TestException();
        }, _ -> Maybe.just(1), _ -> {

        })
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void errorEager() {

        Maybe.using(() -> 1, _ -> Maybe.error(new TestException()), _ -> {

        }, true)
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void emptyEager() {

        Maybe.using(() -> 1, _ -> Maybe.empty(), _ -> {

        }, true)
        .test()
        .assertResult();
    }

    @Test
    public void errorNonEager() {

        Maybe.using(() -> 1, _ -> Maybe.error(new TestException()), _ -> {

        }, false)
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void emptyNonEager() {

        Maybe.using(() -> 1, _ -> Maybe.empty(), _ -> {

        }, false)
        .test()
        .assertResult();
    }

    @Test
    public void supplierCrashEager() {

        Maybe.using(() -> 1, _ -> {
            throw new TestException();
        }, _ -> {

        }, true)
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void supplierCrashNonEager() {

        Maybe.using(() -> 1, _ -> {
            throw new TestException();
        }, _ -> {

        }, false)
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void supplierAndDisposerCrashEager() {
        var to = Maybe.using(() -> 1, _ -> {
            throw new TestException("Main");
        }, _ -> {
            throw new TestException("Disposer");
        }, true)
        .to(TestHelper.testConsumer())
        .assertFailure(CompositeException.class);

        List<Throwable> list = TestHelper.compositeList(to.errors().get(0));

        TestHelper.assertError(list, 0, TestException.class, "Main");
        TestHelper.assertError(list, 1, TestException.class, "Disposer");
    }

    @Test
    public void supplierAndDisposerCrashNonEager() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            Maybe.using(() -> 1, _ -> {
                throw new TestException("Main");
            }, _ -> {
                throw new TestException("Disposer");
            }, false)
            .to(TestHelper.testConsumer())
            .assertFailureAndMessage(TestException.class, "Main");

            TestHelper.assertUndeliverable(errors, 0, TestException.class, "Disposer");
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void dispose() {
        final int[] call = {0 };

        var to = Maybe.using(() -> 1, _ -> Maybe.never(), _ -> call[0]++, false)
        .test();

        to.dispose();

        assertEquals(1, call[0]);
    }

    @Test
    public void disposeCrashes() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            var to = Maybe.using(() -> 1, _ -> Maybe.never(), _ -> {
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
        TestHelper.checkDisposed(Maybe.using(() -> 1, _ -> Maybe.never(), _ -> {

        }, false));
    }

    @Test
    public void justDisposerCrashes() {
        Maybe.using(() -> 1, _ -> Maybe.just(1), _ -> {
            throw new TestException("Disposer");
        }, true)
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void emptyDisposerCrashes() {
        Maybe.using(() -> 1, _ -> Maybe.empty(), _ -> {
            throw new TestException("Disposer");
        }, true)
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void errorDisposerCrash() {
        var to = Maybe.using(() -> 1, _ -> Maybe.error(new TestException("Main")), _ -> {
            throw new TestException("Disposer");
        }, true)
        .to(TestHelper.testConsumer())
        .assertFailure(CompositeException.class);

        List<Throwable> list = TestHelper.compositeList(to.errors().get(0));

        TestHelper.assertError(list, 0, TestException.class, "Main");
        TestHelper.assertError(list, 1, TestException.class, "Disposer");
    }

    @Test
    public void doubleOnSubscribe() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            Maybe.using(() -> 1, _ -> Maybe.wrap((MaybeSource<Integer>) observer -> {
                Disposable d1 = Disposable.empty();

                observer.onSubscribe(d1);

                Disposable d2 = Disposable.empty();

                observer.onSubscribe(d2);

                assertFalse(d1.isDisposed());

                assertTrue(d2.isDisposed());
            }), _ -> {

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

            final TestObserver<Integer> to = Maybe.using(() -> 1, _ -> ps.lastElement(), _ -> {
            }, true)
            .test();

            ps.onNext(1);

            Runnable r1 = to::dispose;

            Runnable r2 = ps::onComplete;

            TestHelper.race(r1, r2);
        }
    }

    @Test
    @SuppressUndeliverable
    public void errorDisposeRace() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {

            final PublishSubject<Integer> ps = PublishSubject.create();

            final TestObserver<Integer> to = Maybe.using(() -> 1, _ -> ps.firstElement(), _ -> {
            }, true)
            .test();

            final TestException ex = new TestException();

            Runnable r1 = to::dispose;

            Runnable r2 = () -> ps.onError(ex);

            TestHelper.race(r1, r2);
        }
    }

    @Test
    public void emptyDisposeRace() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {

            final PublishSubject<Integer> ps = PublishSubject.create();

            final TestObserver<Integer> to = Maybe.using(() -> 1, _ -> ps.firstElement(), _ -> {

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

        TestObserver<Integer> to = Maybe.using(Functions.justSupplier(1),
                        (Function<Integer, Maybe<Integer>>) _ -> Maybe.<Integer>never()
                                .doOnDispose(() -> sb.append("Dispose")), _ -> sb.append("Resource"), true)
        .test()
        ;
        to.assertEmpty();

        to.dispose();

        assertEquals("ResourceDispose", sb.toString());
    }

    @Test
    public void nonEagerDisposeUpstreamThenDisposeResource() {
        final StringBuilder sb = new StringBuilder();

        TestObserver<Integer> to = Maybe.using(Functions.justSupplier(1),
                        (Function<Integer, Maybe<Integer>>) _ -> Maybe.<Integer>never()
                                .doOnDispose(() -> sb.append("Dispose")), _ -> sb.append("Resource"), false)
        .test()
        ;
        to.assertEmpty();

        to.dispose();

        assertEquals("DisposeResource", sb.toString());
    }
}
