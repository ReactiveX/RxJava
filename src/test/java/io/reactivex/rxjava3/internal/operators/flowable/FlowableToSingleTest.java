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

package io.reactivex.rxjava3.internal.operators.flowable;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.*;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.functions.Action;
import io.reactivex.rxjava3.subscribers.TestSubscriber;

public class FlowableToSingleTest extends RxJavaTest {

    @Test
    public void justSingleItemObservable() {
        TestSubscriber<String> subscriber = TestSubscriber.create();
        Single<String> single = Flowable.just("Hello World!").single("");
        single.toFlowable().subscribe(subscriber);

        subscriber.assertResult("Hello World!");
    }

    @Test
    public void errorObservable() {
        TestSubscriber<String> subscriber = TestSubscriber.create();
        IllegalArgumentException error = new IllegalArgumentException("Error");
        Single<String> single = Flowable.<String>error(error).single("");
        single.toFlowable().subscribe(subscriber);

        subscriber.assertError(error);
    }

    @Test
    public void justTwoEmissionsObservableThrowsError() {
        TestSubscriber<String> subscriber = TestSubscriber.create();
        Single<String> single = Flowable.just("First", "Second").single("");
        single.toFlowable().subscribe(subscriber);

        subscriber.assertError(IllegalArgumentException.class);
    }

    @Test
    public void emptyObservable() {
        TestSubscriber<String> subscriber = TestSubscriber.create();
        Single<String> single = Flowable.<String>empty().single("");
        single.toFlowable().subscribe(subscriber);

        subscriber.assertResult("");
    }

    @Test
    public void repeatObservableThrowsError() {
        TestSubscriber<String> subscriber = TestSubscriber.create();
        Single<String> single = Flowable.just("First", "Second").repeat().single("");
        single.toFlowable().subscribe(subscriber);

        subscriber.assertError(IllegalArgumentException.class);
    }

    @Test
    public void shouldUseUnsafeSubscribeInternallyNotSubscribe() {
        TestSubscriber<String> subscriber = TestSubscriber.create();
        final AtomicBoolean unsubscribed = new AtomicBoolean(false);
        Single<String> single = Flowable.just("Hello World!").doOnCancel(new Action() {

            @Override
            public void run() {
                unsubscribed.set(true);
            }}).single("");
        single.toFlowable().subscribe(subscriber);
        subscriber.assertComplete();
        Assert.assertFalse(unsubscribed.get());
    }
}
