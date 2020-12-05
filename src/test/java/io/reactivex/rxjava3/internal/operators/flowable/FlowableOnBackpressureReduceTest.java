/**
 * Copyright (c) 2016-present, RxJava Contributors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See
 * the License for the specific language governing permissions and limitations under the License.
 */

package io.reactivex.rxjava3.internal.operators.flowable;

import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.RxJavaTest;
import io.reactivex.rxjava3.exceptions.TestException;
import io.reactivex.rxjava3.functions.BiFunction;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.processors.PublishProcessor;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subscribers.TestSubscriber;
import io.reactivex.rxjava3.testsupport.TestHelper;
import io.reactivex.rxjava3.testsupport.TestSubscriberEx;
import org.junit.Assert;
import org.junit.Test;
import org.reactivestreams.Publisher;

import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class FlowableOnBackpressureReduceTest extends RxJavaTest {

    static final BiFunction<Integer, Integer, Integer> TEST_INT_REDUCER = new BiFunction<Integer, Integer, Integer>() {
        @Override
        public Integer apply(Integer previous, Integer current) throws Throwable {
            return previous + current + 50;
        }
    };

    static final BiFunction<Object, Object, Object> TEST_OBJECT_REDUCER = new BiFunction<Object, Object, Object>() {
        @Override
        public Object apply(Object previous, Object current) throws Throwable {
            return current;
        }
    };

    @Test
    public void simple() {
        TestSubscriberEx<Integer> ts = new TestSubscriberEx<>();

        Flowable.range(1, 5).onBackpressureReduce(TEST_INT_REDUCER).subscribe(ts);

        ts.assertNoErrors();
        ts.assertTerminated();
        ts.assertValues(1, 2, 3, 4, 5);
    }

    @Test
    public void simpleError() {
        TestSubscriberEx<Integer> ts = new TestSubscriberEx<>();

        Flowable.range(1, 5).concatWith(Flowable.<Integer>error(new TestException()))
                .onBackpressureReduce(TEST_INT_REDUCER).subscribe(ts);

        ts.assertTerminated();
        ts.assertError(TestException.class);
        ts.assertValues(1, 2, 3, 4, 5);
    }

    @Test
    public void simpleBackpressure() {
        TestSubscriber<Integer> ts = new TestSubscriber<>(2L);

        Flowable.range(1, 5).onBackpressureReduce(TEST_INT_REDUCER).subscribe(ts);

        ts.assertNoErrors();
        ts.assertValues(1, 2);
        ts.assertNotComplete();
    }

    @Test
    public void synchronousDrop() {
        PublishProcessor<Integer> source = PublishProcessor.create();
        TestSubscriberEx<Integer> ts = new TestSubscriberEx<>(0L);

        source.onBackpressureReduce(TEST_INT_REDUCER).subscribe(ts);

        ts.assertNoValues();

        source.onNext(1);
        ts.request(2);

        ts.assertValue(1);

        source.onNext(2);

        ts.assertValues(1, 2);

        source.onNext(3);
        source.onNext(4);//3 + 4 + 50 == 57
        source.onNext(5);//57 + 5 + 50 == 112
        source.onNext(6);//112 + 6 + 50 == 168

        ts.request(2);

        ts.assertValues(1, 2, 168);

        source.onNext(7);

        ts.assertValues(1, 2, 168, 7);

        source.onNext(8);
        source.onNext(9);//8 + 9 + 50 == 67
        source.onComplete();

        ts.request(1);

        ts.assertValues(1, 2, 168, 7, 67);
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
                .onBackpressureReduce(new BiFunction<Integer, Integer, Integer>() {
                    @Override
                    public Integer apply(Integer previous, Integer current) throws Throwable {
                        //in that case it works like onBackpressureLatest
                        //the output sequence of number must be increasing
                        return current;
                    }
                })
                .observeOn(Schedulers.io())
                .subscribe(ts);

        ts.awaitDone(2, TimeUnit.SECONDS);
        ts.assertTerminated();
        int n = ts.values().size();
        System.out.println("testAsynchronousDrop -> " + n);
        Assert.assertTrue("All events received?", n < m);
        int i = 0;
        for (Integer value : ts.values()) {
            Assert.assertTrue("The sequence must be increasing, same values: " + i, i < value);
            i = value;
        }

    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeFlowable(new Function<Flowable<Object>, Publisher<Object>>() {
            @Override
            public Publisher<Object> apply(Flowable<Object> f) throws Exception {
                return f.onBackpressureReduce(TEST_OBJECT_REDUCER);
            }
        });
    }

    @Test
    public void take() {
        Flowable.just(1, 2)
                .onBackpressureReduce(TEST_INT_REDUCER)
                .take(1)
                .test()
                .assertResult(1);
    }

    @Test
    public void dispose() {
        TestHelper.checkDisposed(Flowable.never().onBackpressureReduce(TEST_OBJECT_REDUCER));
    }

    @Test
    public void badRequest() {
        TestHelper.assertBadRequestReported(Flowable.never().onBackpressureReduce(TEST_OBJECT_REDUCER));
    }
}
