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

import io.reactivex.rxjava4.core.*;
import io.reactivex.rxjava4.disposables.*;
import io.reactivex.rxjava4.exceptions.CompositeException;
import io.reactivex.rxjava4.functions.Consumer;
import io.reactivex.rxjava4.internal.functions.Functions;
import io.reactivex.rxjava4.observers.LambdaConsumerIntrospection;
import io.reactivex.rxjava4.plugins.RxJavaPlugins;
import io.reactivex.rxjava4.subjects.SingleSubject;
import io.reactivex.rxjava4.testsupport.TestHelper;

public class SingleConsumersTest implements Consumer<Object> {

    final CompositeDisposable composite = new CompositeDisposable();

    final SingleSubject<Integer> processor = SingleSubject.create();

    final List<Object> events = new ArrayList<>();

    @Override
    public void accept(Object t) throws Exception {
        events.add(t);
    }

    static <T> Disposable subscribeAutoDispose(Single<T> source, CompositeDisposable composite,
            Consumer<? super T> onSuccess, Consumer<? super Throwable> onError) {
        return source.subscribe(onSuccess, onError, composite);
    }

    @Test
    public void onSuccessNormal() {

        Disposable d = subscribeAutoDispose(processor, composite, this, Functions.ON_ERROR_MISSING);

        assertFalse(((LambdaConsumerIntrospection)d).hasCustomOnError(), d.getClass().toString());

        assertTrue(composite.size() > 0);

        assertTrue(events.isEmpty(), events.toString());

        processor.onSuccess(1);

        assertEquals(0, composite.size());

        assertEquals(List.<Object>of(1), events);

    }

    @Test
    public void onErrorNormal() {

        subscribeAutoDispose(processor, composite, this, this);

        assertTrue(composite.size() > 0);

        assertTrue(events.isEmpty(), events.toString());

        processor.onSuccess(1);

        assertEquals(0, composite.size());

        assertEquals(List.<Object>of(1), events);

    }

    @Test
    public void onErrorError() {

        Disposable d = subscribeAutoDispose(processor, composite, this, this);

        assertTrue(((LambdaConsumerIntrospection)d).hasCustomOnError(), d.getClass().toString());

        assertTrue(composite.size() > 0);

        assertTrue(events.isEmpty(), events.toString());

        processor.onError(new IOException());

        assertTrue(events.getFirst() instanceof IOException, events.toString());

        assertEquals(0, composite.size());
    }

    @Test
    public void onSuccessCrash() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            subscribeAutoDispose(processor, composite, _ -> {
                throw new IOException();
            }, this);

            processor.onSuccess(1);

            assertTrue(events.isEmpty(), events.toString());

            TestHelper.assertUndeliverable(errors, 0, IOException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void onErrorCrash() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            subscribeAutoDispose(processor, composite, this, t -> {
                throw new IOException(t);
            });

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
    public void badSource() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            subscribeAutoDispose(
                    new Single<Integer>() /* NFI */ {
                        @Override
                        protected void subscribeActual(
                                SingleObserver<? super Integer> observer) {
                            observer.onSubscribe(Disposable.empty());
                            observer.onSuccess(1);

                            observer.onSubscribe(Disposable.empty());
                            observer.onSuccess(2);
                            observer.onError(new IOException());
                        }
                    }, composite, this, this
                );

            assertEquals(List.<Object>of(1), events);

            TestHelper.assertUndeliverable(errors, 0, IOException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }
}
