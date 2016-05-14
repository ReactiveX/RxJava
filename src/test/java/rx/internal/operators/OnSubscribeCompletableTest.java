/**
 * Copyright 2014 Netflix, Inc.
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
package rx.internal.operators;

import static org.junit.Assert.assertFalse;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

import rx.Completable;
import rx.Observable;
import rx.functions.Action0;
import rx.observers.TestSubscriber;

public class OnSubscribeCompletableTest {

    @Test
    public void testJustSingleItemObservable() {
        TestSubscriber<String> subscriber = TestSubscriber.create();
        Completable cmp = Observable.just("Hello World!").toCompletable();
        cmp.unsafeSubscribe(subscriber);

        subscriber.assertNoValues();
        subscriber.assertCompleted();
        subscriber.assertNoErrors();
    }

    @Test
    public void testErrorObservable() {
        TestSubscriber<String> subscriber = TestSubscriber.create();
        IllegalArgumentException error = new IllegalArgumentException("Error");
        Completable cmp = Observable.<String>error(error).toCompletable();
        cmp.unsafeSubscribe(subscriber);

        subscriber.assertError(error);
        subscriber.assertNoValues();
    }

    @Test
    public void testJustTwoEmissionsObservableThrowsError() {
        TestSubscriber<String> subscriber = TestSubscriber.create();
        Completable cmp = Observable.just("First", "Second").toCompletable();
        cmp.unsafeSubscribe(subscriber);

        subscriber.assertNoErrors();
        subscriber.assertNoValues();
    }

    @Test
    public void testEmptyObservable() {
        TestSubscriber<String> subscriber = TestSubscriber.create();
        Completable cmp = Observable.<String>empty().toCompletable();
        cmp.unsafeSubscribe(subscriber);

        subscriber.assertNoErrors();
        subscriber.assertNoValues();
        subscriber.assertCompleted();
    }

    @Test
    public void testNeverObservable() {
        TestSubscriber<String> subscriber = TestSubscriber.create();
        Completable cmp = Observable.<String>never().toCompletable();
        cmp.unsafeSubscribe(subscriber);

        subscriber.assertNoTerminalEvent();
        subscriber.assertNoValues();
    }

    @Test
    public void testShouldUseUnsafeSubscribeInternallyNotSubscribe() {
        TestSubscriber<String> subscriber = TestSubscriber.create();
        final AtomicBoolean unsubscribed = new AtomicBoolean(false);
        Completable cmp = Observable.just("Hello World!").doOnUnsubscribe(new Action0() {

            @Override
            public void call() {
                unsubscribed.set(true);
            }}).toCompletable();
        cmp.unsafeSubscribe(subscriber);
        subscriber.assertCompleted();
        assertFalse(unsubscribed.get());
    }
}
