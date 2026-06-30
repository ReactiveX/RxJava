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

package io.reactivex.rxjava4.internal.observers;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.*;

import org.junit.jupiter.api.Test;

import io.reactivex.rxjava4.core.Observable;
import io.reactivex.rxjava4.core.Observer;
import io.reactivex.rxjava4.core.RxJavaTest;
import io.reactivex.rxjava4.disposables.Disposable;
import io.reactivex.rxjava4.exceptions.*;
import io.reactivex.rxjava4.internal.functions.Functions;
import io.reactivex.rxjava4.plugins.RxJavaPlugins;
import io.reactivex.rxjava4.subjects.PublishSubject;
import io.reactivex.rxjava4.testsupport.TestHelper;

public class LambdaObserverTest extends RxJavaTest {

    @Test
    public void onSubscribeThrows() {
        final List<Object> received = new ArrayList<>();

        LambdaObserver<Object> o = new LambdaObserver<>(
                received::add,
                received::add,
                () -> received.add(100),
                _ -> { throw new TestException(); });

        assertFalse(o.isDisposed());

        Observable.just(1).subscribe(o);

        assertTrue(received.getFirst() instanceof TestException, received.toString());
        assertEquals(1, received.size(), received.toString());

        assertTrue(o.isDisposed());
    }

    @Test
    public void onNextThrows() {
        final List<Object> received = new ArrayList<>();

        LambdaObserver<Object> o = new LambdaObserver<>(
                _ -> { throw new TestException(); },
                received::add,
                () -> received.add(100),
                _ -> { });

        assertFalse(o.isDisposed());

        Observable.just(1).subscribe(o);

        assertTrue(received.getFirst() instanceof TestException, received.toString());
        assertEquals(1, received.size(), received.toString());

        assertTrue(o.isDisposed());
    }

    @Test
    public void onErrorThrows() {
        List<Throwable> errors = TestHelper.trackPluginErrors();

        try {
            final List<Object> received = new ArrayList<>();

            LambdaObserver<Object> o = new LambdaObserver<>(
                    received::add,
                    _ -> { throw new TestException("Inner"); },
                    () -> received.add(100),
                    _ -> { });

            assertFalse(o.isDisposed());

            Observable.<Integer>error(new TestException("Outer")).subscribe(o);

            assertTrue(received.isEmpty(), received.toString());

            assertTrue(o.isDisposed());

            TestHelper.assertError(errors, 0, CompositeException.class);
            List<Throwable> ce = TestHelper.compositeList(errors.getFirst());
            TestHelper.assertError(ce, 0, TestException.class, "Outer");
            TestHelper.assertError(ce, 1, TestException.class, "Inner");
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void onCompleteThrows() {
        List<Throwable> errors = TestHelper.trackPluginErrors();

        try {
            final List<Object> received = new ArrayList<>();

            LambdaObserver<Object> o = new LambdaObserver<>(
                    received::add,
                    received::add,
                    () -> { throw new TestException(); },
                    _ -> { });

            assertFalse(o.isDisposed());

            Observable.<Integer>empty().subscribe(o);

            assertTrue(received.isEmpty(), received.toString());

            assertTrue(o.isDisposed());

            TestHelper.assertUndeliverable(errors, 0, TestException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void badSourceOnSubscribe() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            var source = new Observable<Integer>() /* NFI */ {
                @Override
                public void subscribeActual(Observer<? super Integer> observer) {
                    Disposable d1 = Disposable.empty();
                    observer.onSubscribe(d1);
                    Disposable d2 = Disposable.empty();
                    observer.onSubscribe(d2);

                    assertFalse(d1.isDisposed());
                    assertTrue(d2.isDisposed());

                    observer.onNext(1);
                    observer.onComplete();
                }
            };

            final List<Object> received = new ArrayList<>();

            LambdaObserver<Object> o = new LambdaObserver<>(
                    received::add,
                    received::add,
                    () -> received.add(100),
                    _ -> { });

            source.subscribe(o);

            assertEquals(Arrays.asList(1, 100), received);

            TestHelper.assertError(errors, 0, ProtocolViolationException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void badSourceEmitAfterDone() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            var source = new Observable<Integer>() /* NFI */ {
                @Override
                public void subscribeActual(Observer<? super Integer> observer) {
                    observer.onSubscribe(Disposable.empty());

                    observer.onNext(1);
                    observer.onComplete();
                    observer.onNext(2);
                    observer.onError(new TestException());
                    observer.onComplete();
                }
            };

            final List<Object> received = new ArrayList<>();

            LambdaObserver<Object> o = new LambdaObserver<>(received::add,
                    received::add, () -> received.add(100), _ -> { });

            source.subscribe(o);

            assertEquals(Arrays.asList(1, 100), received);

            TestHelper.assertUndeliverable(errors, 0, TestException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void onNextThrowsCancelsUpstream() {
        PublishSubject<Integer> ps = PublishSubject.create();

        final List<Throwable> errors = new ArrayList<>();

        ps.subscribe(_ -> {
            throw new TestException();
        }, errors::add);

        assertTrue(ps.hasObservers(), "No observers?!");
        assertTrue(errors.isEmpty(), "Has errors already?!");

        ps.onNext(1);

        assertFalse(ps.hasObservers(), "Has observers?!");
        assertFalse(errors.isEmpty(), "No errors?!");

        assertTrue(errors.getFirst() instanceof TestException, errors.toString());
    }

    @Test
    public void onSubscribeThrowsCancelsUpstream() {
        PublishSubject<Integer> ps = PublishSubject.create();

        final List<Throwable> errors = new ArrayList<>();

        ps.subscribe(new LambdaObserver<>(_ -> {
        }, errors::add, () -> {
        }, _ -> {
            throw new TestException();
        }));

        assertFalse(ps.hasObservers(), "Has observers?!");
        assertFalse(errors.isEmpty(), "No errors?!");

        assertTrue(errors.getFirst() instanceof TestException, errors.toString());
    }

    @Test
    public void onErrorMissingShouldReportNoCustomOnError() {
        LambdaObserver<Integer> o = new LambdaObserver<>(Functions.<Integer>emptyConsumer(),
                Functions.ON_ERROR_MISSING,
                Functions.EMPTY_ACTION,
                Functions.<Disposable>emptyConsumer());

        assertFalse(o.hasCustomOnError());
    }

    @Test
    public void customOnErrorShouldReportCustomOnError() {
        LambdaObserver<Integer> o = new LambdaObserver<>(Functions.<Integer>emptyConsumer(),
                Functions.<Throwable>emptyConsumer(),
                Functions.EMPTY_ACTION,
                Functions.<Disposable>emptyConsumer());

        assertTrue(o.hasCustomOnError());
    }

    @Test
    public void disposedObserverShouldReportErrorOnGlobalErrorHandler() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            final List<Throwable> observerErrors = Collections.synchronizedList(new ArrayList<>());

            LambdaObserver<Integer> o = new LambdaObserver<>(Functions.<Integer>emptyConsumer(),
                    observerErrors::add,
                    Functions.EMPTY_ACTION,
                    Functions.<Disposable>emptyConsumer());

            o.dispose();
            o.onError(new IOException());
            o.onError(new IOException());

            assertTrue(observerErrors.isEmpty());
            TestHelper.assertUndeliverable(errors, 0, IOException.class);
            TestHelper.assertUndeliverable(errors, 1, IOException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }
}
