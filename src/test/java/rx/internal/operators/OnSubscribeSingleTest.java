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

import java.util.Collections;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

import rx.Observable;
import rx.Single;
import rx.functions.Action0;
import rx.observers.TestSubscriber;

public class OnSubscribeSingleTest {

    @Test
    public void testJustSingleItemObservable() {
        TestSubscriber<String> subscriber = TestSubscriber.create();
        Single<String> single = Observable.just("Hello World!").toSingle();
        single.subscribe(subscriber);

        subscriber.assertReceivedOnNext(Collections.singletonList("Hello World!"));
    }

    @Test
    public void testErrorObservable() {
        TestSubscriber<String> subscriber = TestSubscriber.create();
        IllegalArgumentException error = new IllegalArgumentException("Error");
        Single<String> single = Observable.<String>error(error).toSingle();
        single.subscribe(subscriber);

        subscriber.assertError(error);
    }

    @Test
    public void testJustTwoEmissionsObservableThrowsError() {
        TestSubscriber<String> subscriber = TestSubscriber.create();
        Single<String> single = Observable.just("First", "Second").toSingle();
        single.subscribe(subscriber);

        subscriber.assertError(IllegalArgumentException.class);
    }

    @Test
    public void testEmptyObservable() {
        TestSubscriber<String> subscriber = TestSubscriber.create();
        Single<String> single = Observable.<String>empty().toSingle();
        single.subscribe(subscriber);

        subscriber.assertError(NoSuchElementException.class);
    }

    @Test
    public void testRepeatObservableThrowsError() {
        TestSubscriber<String> subscriber = TestSubscriber.create();
        Single<String> single = Observable.just("First", "Second").repeat().toSingle();
        single.subscribe(subscriber);

        subscriber.assertError(IllegalArgumentException.class);
    }
    
    @Test
    public void testShouldUseUnsafeSubscribeInternallyNotSubscribe() {
        TestSubscriber<String> subscriber = TestSubscriber.create();
        final AtomicBoolean unsubscribed = new AtomicBoolean(false);
        Single<String> single = Observable.just("Hello World!").doOnUnsubscribe(new Action0() {

            @Override
            public void call() {
                unsubscribed.set(true);
            }}).toSingle();
        single.unsafeSubscribe(subscriber);
        subscriber.assertCompleted();
        assertFalse(unsubscribed.get());
    }
}
