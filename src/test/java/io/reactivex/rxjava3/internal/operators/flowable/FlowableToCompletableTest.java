/**
 * Copyright (c) 2016-present, RxJava Contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.reactivex.rxjava3.internal.operators.flowable;

import static org.junit.Assert.assertFalse;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.functions.Action;
import io.reactivex.rxjava3.subscribers.TestSubscriber;
import io.reactivex.rxjava3.testsupport.TestSubscriberEx;

public class FlowableToCompletableTest extends RxJavaTest {

    @Test
    public void justSingleItemObservable() {
        TestSubscriber<String> subscriber = TestSubscriber.create();
        Completable cmp = Flowable.just("Hello World!").ignoreElements();
        cmp.<String>toFlowable().subscribe(subscriber);

        subscriber.assertNoValues();
        subscriber.assertComplete();
        subscriber.assertNoErrors();
    }

    @Test
    public void errorObservable() {
        TestSubscriber<String> subscriber = TestSubscriber.create();
        IllegalArgumentException error = new IllegalArgumentException("Error");
        Completable cmp = Flowable.<String>error(error).ignoreElements();
        cmp.<String>toFlowable().subscribe(subscriber);

        subscriber.assertError(error);
        subscriber.assertNoValues();
    }

    @Test
    public void justTwoEmissionsObservableThrowsError() {
        TestSubscriber<String> subscriber = TestSubscriber.create();
        Completable cmp = Flowable.just("First", "Second").ignoreElements();
        cmp.<String>toFlowable().subscribe(subscriber);

        subscriber.assertNoErrors();
        subscriber.assertNoValues();
    }

    @Test
    public void emptyObservable() {
        TestSubscriber<String> subscriber = TestSubscriber.create();
        Completable cmp = Flowable.<String>empty().ignoreElements();
        cmp.<String>toFlowable().subscribe(subscriber);

        subscriber.assertNoErrors();
        subscriber.assertNoValues();
        subscriber.assertComplete();
    }

    @Test
    public void neverObservable() {
        TestSubscriberEx<String> subscriber = new TestSubscriberEx<>();
        Completable cmp = Flowable.<String>never().ignoreElements();
        cmp.<String>toFlowable().subscribe(subscriber);

        subscriber.assertNotTerminated();
        subscriber.assertNoValues();
    }

    @Test
    public void shouldUseUnsafeSubscribeInternallyNotSubscribe() {
        TestSubscriber<String> subscriber = TestSubscriber.create();
        final AtomicBoolean unsubscribed = new AtomicBoolean(false);
        Completable cmp = Flowable.just("Hello World!").doOnCancel(new Action() {

            @Override
            public void run() {
                unsubscribed.set(true);
            }}).ignoreElements();

        cmp.<String>toFlowable().subscribe(subscriber);

        subscriber.assertComplete();

        assertFalse(unsubscribed.get());
    }
}
