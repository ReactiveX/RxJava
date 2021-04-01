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

import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.junit.*;
import org.reactivestreams.Publisher;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.exceptions.TestException;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.processors.PublishProcessor;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subscribers.TestSubscriber;
import io.reactivex.rxjava3.testsupport.*;

public class FlowableOnBackpressureLatestTest extends RxJavaTest {
    @Test
    public void simple() {
        TestSubscriberEx<Integer> ts = new TestSubscriberEx<>();

        Flowable.range(1, 5).onBackpressureLatest().subscribe(ts);

        ts.assertNoErrors();
        ts.assertTerminated();
        ts.assertValues(1, 2, 3, 4, 5);
    }

    @Test
    public void simpleError() {
        TestSubscriberEx<Integer> ts = new TestSubscriberEx<>();

        Flowable.range(1, 5).concatWith(Flowable.<Integer>error(new TestException()))
        .onBackpressureLatest().subscribe(ts);

        ts.assertTerminated();
        ts.assertError(TestException.class);
        ts.assertValues(1, 2, 3, 4, 5);
    }

    @Test
    public void simpleBackpressure() {
        TestSubscriber<Integer> ts = new TestSubscriber<>(2L);

        Flowable.range(1, 5).onBackpressureLatest().subscribe(ts);

        ts.assertNoErrors();
        ts.assertValues(1, 2);
        ts.assertNotComplete();
    }

    @Test
    public void synchronousDrop() {
        PublishProcessor<Integer> source = PublishProcessor.create();
        TestSubscriberEx<Integer> ts = new TestSubscriberEx<>(0L);

        source.onBackpressureLatest().subscribe(ts);

        ts.assertNoValues();

        source.onNext(1);
        ts.request(2);

        ts.assertValue(1);

        source.onNext(2);

        ts.assertValues(1, 2);

        source.onNext(3);
        source.onNext(4);
        source.onNext(5);
        source.onNext(6);

        ts.request(2);

        ts.assertValues(1, 2, 6);

        source.onNext(7);

        ts.assertValues(1, 2, 6, 7);

        source.onNext(8);
        source.onNext(9);
        source.onComplete();

        ts.request(1);

        ts.assertValues(1, 2, 6, 7, 9);
        ts.assertNoErrors();
        ts.assertTerminated();
    }

    @Test
    public void asynchronousDrop() throws InterruptedException {
        TestSubscriberEx<Integer> ts = new TestSubscriberEx<Integer>(1L) {
            final Random rnd = new Random();
            @Override
            public void onNext(Integer t) {
                super.onNext(t);
                if (rnd.nextDouble() < 0.001) {
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
                }
                request(1);
            }
        };
        int m = 100000;
        Flowable.range(1, m)
        .subscribeOn(Schedulers.computation())
        .onBackpressureLatest()
        .observeOn(Schedulers.io())
        .subscribe(ts);

        ts.awaitDone(2, TimeUnit.SECONDS);
        ts.assertTerminated();
        int n = ts.values().size();
        System.out.println("testAsynchronousDrop -> " + n);
        Assert.assertTrue("All events received?", n < m);
        int previous = 0;
        for (Integer current : ts.values()) {
            Assert.assertTrue("The sequence must be increasing [current value=" + previous +
                    ", previous value=" + current + "]", previous <= current);
            previous = current;
        }
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeFlowable(new Function<Flowable<Object>, Publisher<Object>>() {
            @Override
            public Publisher<Object> apply(Flowable<Object> f) throws Exception {
                return f.onBackpressureLatest();
            }
        });
    }

    @Test
    public void take() {
        Flowable.just(1, 2)
        .onBackpressureLatest()
        .take(1)
        .test()
        .assertResult(1);
    }

    @Test
    public void dispose() {
        TestHelper.checkDisposed(Flowable.never().onBackpressureLatest());
    }

    @Test
    public void badRequest() {
        TestHelper.assertBadRequestReported(Flowable.never().onBackpressureLatest());
    }
}
