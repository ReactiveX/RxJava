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

/*
 * Copyright 2016-2019 David Karnok
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.reactivex.rxjava4.internal.observers;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.*;

import org.junit.jupiter.api.Test;

import io.reactivex.rxjava4.core.Observable;
import io.reactivex.rxjava4.core.Observer;
import io.reactivex.rxjava4.disposables.*;
import io.reactivex.rxjava4.exceptions.*;
import io.reactivex.rxjava4.functions.*;
import io.reactivex.rxjava4.internal.functions.Functions;
import io.reactivex.rxjava4.observers.LambdaConsumerIntrospection;
import io.reactivex.rxjava4.plugins.RxJavaPlugins;
import io.reactivex.rxjava4.subjects.PublishSubject;
import io.reactivex.rxjava4.testsupport.TestHelper;

public class ObservableConsumersTest implements Consumer<Object>, Action {

    final CompositeDisposable composite = new CompositeDisposable();

    final PublishSubject<Integer> processor = PublishSubject.create();

    final List<Object> events = new ArrayList<>();

    @Override
    public void run() throws Exception {
        events.add("OnComplete");
    }

    @Override
    public void accept(Object t) throws Exception {
        events.add(t);
    }

    static <T> Disposable subscribeAutoDispose(Observable<T> source, CompositeDisposable composite,
            Consumer<? super T> onNext, Consumer<? super Throwable> onError, Action onComplete) {
        return source.subscribe(onNext, onError, onComplete, composite);
    }

    @Test
    public void onNextNormal() {

        Disposable d = subscribeAutoDispose(processor, composite, this, Functions.ON_ERROR_MISSING, () -> { });

        assertFalse(((LambdaConsumerIntrospection)d).hasCustomOnError(), d.getClass().toString());

        assertTrue(composite.size() > 0);

        assertTrue(events.isEmpty(), events.toString());

        processor.onNext(1);

        assertTrue(composite.size() > 0);

        assertEquals(List.<Object>of(1), events);

        processor.onComplete();

        assertEquals(List.<Object>of(1), events);

        assertEquals(0, composite.size());
    }

    @Test
    public void onErrorNormal() {

        subscribeAutoDispose(processor, composite, this, Functions.ON_ERROR_MISSING, () -> { });

        assertTrue(composite.size() > 0);

        assertTrue(events.isEmpty(), events.toString());

        processor.onNext(1);

        assertTrue(composite.size() > 0);

        assertEquals(List.<Object>of(1), events);

        processor.onComplete();

        assertEquals(List.<Object>of(1), events);

        assertEquals(0, composite.size());
    }

    @Test
    public void onErrorError() {

        Disposable d = subscribeAutoDispose(processor, composite, this, this, this);

        assertTrue(((LambdaConsumerIntrospection)d).hasCustomOnError(), d.getClass().toString());

        assertTrue(composite.size() > 0);

        assertTrue(events.isEmpty(), events.toString());

        processor.onNext(1);

        assertTrue(composite.size() > 0);

        assertEquals(List.<Object>of(1), events);

        processor.onError(new IOException());

        assertEquals(1, events.get(0), events.toString());
        assertTrue(events.get(1) instanceof IOException, events.toString());

        assertEquals(0, composite.size());
    }

    @Test
    public void onCompleteNormal() {

        subscribeAutoDispose(processor, composite, this, this, this);

        assertTrue(composite.size() > 0);

        assertTrue(events.isEmpty(), events.toString());

        processor.onNext(1);

        assertTrue(composite.size() > 0);

        assertEquals(List.<Object>of(1), events);

        processor.onComplete();

        assertEquals(Arrays.<Object>asList(1, "OnComplete"), events);

        assertEquals(0, composite.size());
    }

    @Test
    public void onCompleteError() {

        subscribeAutoDispose(processor, composite, this, this, this);

        assertTrue(composite.size() > 0);

        assertTrue(events.isEmpty(), events.toString());

        processor.onNext(1);

        assertTrue(composite.size() > 0);

        assertEquals(List.<Object>of(1), events);

        processor.onError(new IOException());

        assertEquals(1, events.get(0), events.toString());
        assertTrue(events.get(1) instanceof IOException, events.toString());

        assertEquals(0, composite.size());
    }

    @Test
    public void onCompleteDispose() {

        Disposable d = subscribeAutoDispose(processor, composite, this, this, this);

        assertTrue(composite.size() > 0);

        assertTrue(events.isEmpty(), events.toString());

        assertFalse(d.isDisposed());

        d.dispose();
        d.dispose();

        assertTrue(d.isDisposed());

        assertEquals(0, composite.size());

        assertFalse(processor.hasObservers());
    }

    @Test
    public void onNextCrash() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            subscribeAutoDispose(processor, composite, _ -> {
                throw new IOException();
            }, this, this);

            processor.onNext(1);

            assertTrue(errors.isEmpty(), errors.toString());

            assertTrue(events.getFirst() instanceof IOException, events.toString());
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void onNextCrashOnError() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            subscribeAutoDispose(processor, composite, this, t -> {
                throw new IOException(t);
            }, this);

            processor.onError(new IllegalArgumentException());

            assertTrue(events.isEmpty(), events.toString());

            TestHelper.assertError(errors, 0, CompositeException.class);
            List<Throwable> inners = TestHelper.compositeList(errors.getFirst());
            TestHelper.assertError(inners, 0, IllegalArgumentException.class);
            TestHelper.assertError(inners, 1, IOException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void onNextCrashNoError() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            subscribeAutoDispose(processor, composite, _ -> {
                throw new IOException();
            }, Functions.ON_ERROR_MISSING, () -> { });

            processor.onNext(1);

            assertTrue(events.isEmpty(), events.toString());

            TestHelper.assertError(errors, 0, OnErrorNotImplementedException.class);
            assertTrue(errors.getFirst().getCause() instanceof IOException);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void onCompleteCrash() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            subscribeAutoDispose(processor, composite, this, this, () -> {
                throw new IOException();
            });

            processor.onNext(1);
            processor.onComplete();

            assertEquals(List.of(1), events);

            TestHelper.assertUndeliverable(errors, 0, IOException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void badSource() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            subscribeAutoDispose(
                    new Observable<Integer>() /* NFI */ {
                        @Override
                        protected void subscribeActual(
                                Observer<? super Integer> observer) {
                            observer.onSubscribe(Disposable.empty());
                            observer.onNext(1);
                            observer.onComplete();

                            observer.onSubscribe(Disposable.empty());
                            observer.onNext(2);
                            observer.onComplete();
                            observer.onError(new IOException());
                        }
                    }, composite, this, this, this
                );

            assertEquals(Arrays.<Object>asList(1, "OnComplete"), events);

            TestHelper.assertUndeliverable(errors, 0, IOException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }
}
