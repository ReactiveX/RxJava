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

import org.junit.Test;
import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Producer;
import rx.Subscriber;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;
import rx.schedulers.TestScheduler;

import java.util.concurrent.TimeUnit;

public class OperatorMergeEmptyWithTest {

    @Test(timeout = 60000)
    public void testWithVoid() {
        final String soleValue = "Hello";
        Observable<String> source = Observable.<Void>empty()
                                              .mergeEmptyWith(Observable.just(soleValue));

        TestSubscriber<String> testSubscriber = new TestSubscriber<String>();
        source.subscribe(testSubscriber);

        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertNoErrors();
        testSubscriber.assertValue(soleValue);
    }

    @Test(timeout = 60000)
    public void testErrorOnSourceEmitItem() {
        TestScheduler testScheduler = Schedulers.test();
        Observable<String> source = Observable.just(1)
                                              .mergeEmptyWith(Observable.just("Hello").observeOn(testScheduler));

        TestSubscriber<String> testSubscriber = new TestSubscriber<String>();
        source.subscribe(testSubscriber);

        testScheduler.triggerActions();
        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertNoValues();
        testSubscriber.assertError(IllegalStateException.class);
    }

    @Test(timeout = 60000)
    public void testSourceError() throws Exception {
        TestScheduler testScheduler = Schedulers.test();
        Observable<String> source = Observable.<Void>error(new IllegalStateException())
                                              .mergeEmptyWith(Observable.just("Hello").observeOn(testScheduler));

        TestSubscriber<String> testSubscriber = new TestSubscriber<String>();
        source.subscribe(testSubscriber);

        testScheduler.triggerActions();
        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertNoValues();
        testSubscriber.assertError(IllegalStateException.class);
    }

    @Test(timeout = 60000)
    public void testSourceComplete() throws Exception {
        final String soleValue = "Hello";
        Observable<String> source = Observable.<Void>empty()
                                              .mergeEmptyWith(Observable.just(soleValue));

        TestSubscriber<String> testSubscriber = new TestSubscriber<String>();
        source.subscribe(testSubscriber);

        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertNoErrors();
        testSubscriber.assertValue(soleValue);
    }

    @Test(timeout = 60000)
    public void testErrorFromSourcePostEmission() {
        final String soleValue = "Hello";
        final TestScheduler testScheduler = Schedulers.test();

        /*Delaying error event*/
        Observable<String> source = Observable.<Void>error(new IllegalArgumentException())
                                              .observeOn(testScheduler)
                                              .mergeEmptyWith(Observable.just(soleValue));

        TestSubscriber<String> testSubscriber = new TestSubscriber<String>();
        source.subscribe(testSubscriber);

        testSubscriber.assertNotCompleted();
        testSubscriber.assertNoErrors();
        testSubscriber.assertValue(soleValue);

        testScheduler.advanceTimeBy(1, TimeUnit.HOURS);
        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertError(IllegalArgumentException.class);
    }

    @Test(timeout = 60000)
    public void testSourceNeverCompletes() throws Exception {
        TestSubscriber<String> subscriber = new TestSubscriber<String>();
        Observable.never()
                  .mergeEmptyWith(Observable.just("Hello"))
                  .subscribe(subscriber);

        subscriber.assertValue("Hello");
        subscriber.assertNoTerminalEvent();
    }

    @Test(timeout = 60000)
    public void testSourceDoesntCompleteWithoutRequest() throws Exception {
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
        }).mergeEmptyWith(Observable.just(soleValue)).subscribe(testSubscriber);

        testSubscriber.requestMore(1);

        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertNoErrors();
        testSubscriber.assertValue(soleValue);
    }
}