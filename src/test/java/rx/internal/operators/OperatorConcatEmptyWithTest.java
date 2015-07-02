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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Producer;
import rx.Subscriber;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;
import rx.schedulers.TestScheduler;

public class OperatorConcatEmptyWithTest {

    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

    @Test(timeout = 60000)
    public void testWithVoid() {
        final String soleValue = "Hello";
        Observable<String> source = Observable.<Void>empty()
                                              .concatEmptyWith(Observable.just(soleValue));

        TestSubscriber<String> testSubscriber = new TestSubscriber<String>();
        source.subscribe(testSubscriber);

        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertNoErrors();
        testSubscriber.assertValue(soleValue);
    }

    @Test(timeout = 60000)
    public void testErrorOnSourceEmitItem() {
        Observable<String> source = Observable.just(1)
                                              .concatEmptyWith(Observable.just("Hello"));

        TestSubscriber<String> testSubscriber = new TestSubscriber<String>();
        source.subscribe(testSubscriber);

        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertNoValues();
        testSubscriber.assertError(IllegalStateException.class);
    }

    @Test(timeout = 60000)
    public void testSourceError() throws Exception {
        Observable<String> source = Observable.<Void>error(new IllegalStateException())
                                              .concatEmptyWith(Observable.just("Hello"));

        TestSubscriber<String> testSubscriber = new TestSubscriber<String>();
        source.subscribe(testSubscriber);

        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertNoValues();
        testSubscriber.assertError(IllegalStateException.class);
    }

    @Test(timeout = 60000)
    public void testNoSubscribeBeforeSourceCompletion() {
        final String soleValue = "Hello";
        final TestScheduler testScheduler = Schedulers.test();

        /*Delaying on complete event so to check that the subscription does not happen before completion*/
        Observable<String> source = Observable.<Void>empty()
                                              .observeOn(testScheduler)
                                              .concatEmptyWith(Observable.just(soleValue));

        TestSubscriber<String> testSubscriber = new TestSubscriber<String>();
        source.subscribe(testSubscriber);

        testSubscriber.assertNoTerminalEvent();
        testSubscriber.assertNoValues();

        testScheduler.triggerActions();

        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertNoErrors();
        testSubscriber.assertValue(soleValue);
    }

    @Test(timeout = 60000)
    public void testRequestNSingle() throws Exception {
        final String[] values = {"Hello1", "Hello2"};
        Observable<String> source = Observable.<Void>empty()
                                              .concatEmptyWith(Observable.from(values));

        TestSubscriber<String> testSubscriber = new TestSubscriber<String>(0);
        source.subscribe(testSubscriber);

        testSubscriber.assertNoTerminalEvent();
        testSubscriber.assertNoErrors();
        testSubscriber.assertNoValues();

        testSubscriber.requestMore(2);

        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertNoErrors();
        testSubscriber.assertValues(values);
    }

    @Test(timeout = 60000)
    public void testRequestNMulti() throws Exception {
        final String[] values = {"Hello1", "Hello2"};
        Observable<String> source = Observable.<Void>empty()
                                              .concatEmptyWith(Observable.from(values));

        TestSubscriber<String> testSubscriber = new TestSubscriber<String>(0);
        source.subscribe(testSubscriber);

        testSubscriber.assertNoTerminalEvent();
        testSubscriber.assertNoErrors();
        testSubscriber.assertNoValues();

        testSubscriber.requestMore(1);

        testSubscriber.assertNoTerminalEvent();
        testSubscriber.assertNoErrors();
        testSubscriber.assertValues(values[0]);

        testSubscriber.requestMore(1);

        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertNoErrors();
        testSubscriber.assertValues(values);
    }

    @Test(timeout = 60000)
    public void testSourceDontCompleteWithoutRequest() throws Exception {

        TestSubscriber<String> testSubscriber = new TestSubscriber<String>(0);

        String soleValue = "Hello";
        Observable.create(new OnSubscribe<Void>() {
            @Override
            public void call(final Subscriber<? super Void> subscriber) {
                subscriber.setProducer(new Producer() {
                    @Override
                    public void request(long n) {
                        subscriber.onCompleted();
                    }
                });
            }
        }).concatEmptyWith(Observable.just(soleValue)).subscribe(testSubscriber);

        testSubscriber.requestMore(1);

        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertNoErrors();
        testSubscriber.assertValue(soleValue);
    }
}
