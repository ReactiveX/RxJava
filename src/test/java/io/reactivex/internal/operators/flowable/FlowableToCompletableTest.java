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
package io.reactivex.internal.operators.flowable;

import static org.junit.Assert.assertFalse;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

import io.reactivex.*;
import io.reactivex.functions.Action;
import io.reactivex.subscribers.TestSubscriber;

public class FlowableToCompletableTest {

    @Test
    public void testJustSingleItemObservable() {
        TestSubscriber<String> subscriber = TestSubscriber.create();
        Completable cmp = Flowable.just("Hello World!").ignoreElements();
        cmp.<String>toFlowable().subscribe(subscriber);

        subscriber.assertNoValues();
        subscriber.assertComplete();
        subscriber.assertNoErrors();
    }

    @Test
    public void testErrorObservable() {
        TestSubscriber<String> subscriber = TestSubscriber.create();
        IllegalArgumentException error = new IllegalArgumentException("Error");
        Completable cmp = Flowable.<String>error(error).ignoreElements();
        cmp.<String>toFlowable().subscribe(subscriber);

        subscriber.assertError(error);
        subscriber.assertNoValues();
    }

    @Test
    public void testJustTwoEmissionsObservableThrowsError() {
        TestSubscriber<String> subscriber = TestSubscriber.create();
        Completable cmp = Flowable.just("First", "Second").ignoreElements();
        cmp.<String>toFlowable().subscribe(subscriber);

        subscriber.assertNoErrors();
        subscriber.assertNoValues();
    }

    @Test
    public void testEmptyObservable() {
        TestSubscriber<String> subscriber = TestSubscriber.create();
        Completable cmp = Flowable.<String>empty().ignoreElements();
        cmp.<String>toFlowable().subscribe(subscriber);

        subscriber.assertNoErrors();
        subscriber.assertNoValues();
        subscriber.assertComplete();
    }

    @Test
    public void testNeverObservable() {
        TestSubscriber<String> subscriber = TestSubscriber.create();
        Completable cmp = Flowable.<String>never().ignoreElements();
        cmp.<String>toFlowable().subscribe(subscriber);

        subscriber.assertNotTerminated();
        subscriber.assertNoValues();
    }

    @Test
    public void testShouldUseUnsafeSubscribeInternallyNotSubscribe() {
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
