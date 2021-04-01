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

import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.RxJavaTest;
import io.reactivex.rxjava3.exceptions.TestException;
import io.reactivex.rxjava3.functions.BiFunction;
import io.reactivex.rxjava3.functions.Supplier;
import io.reactivex.rxjava3.processors.PublishProcessor;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.testsupport.TestHelper;
import io.reactivex.rxjava3.testsupport.TestSubscriberEx;
import org.junit.Assert;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class FlowableOnBackpressureReduceWithTest extends RxJavaTest {

    private static <T> BiFunction<List<T>, T, List<T>> createTestReducer() {
        return (list, number) -> {
            list.add(number);
            return list;
        };
    }

    private static <T> Supplier<List<T>> createTestSupplier() {
        return ArrayList::new;
    }

    @Test
    public void simple() {
        TestSubscriberEx<List<Integer>> ts = new TestSubscriberEx<>();

        Flowable.range(1, 5).onBackpressureReduce(createTestSupplier(), createTestReducer()).subscribe(ts);

        ts.assertNoErrors();
        ts.assertTerminated();
        ts.assertValues(
                Collections.singletonList(1),
                Collections.singletonList(2),
                Collections.singletonList(3),
                Collections.singletonList(4),
                Collections.singletonList(5)
        );
    }

    @Test
    public void simpleError() {
        TestSubscriberEx<List<Integer>> ts = new TestSubscriberEx<>();

        Flowable.range(1, 5).concatWith(Flowable.error(new TestException()))
                .onBackpressureReduce(createTestSupplier(), createTestReducer()).subscribe(ts);

        ts.assertTerminated();
        ts.assertError(TestException.class);
        ts.assertValues(
                Collections.singletonList(1),
                Collections.singletonList(2),
                Collections.singletonList(3),
                Collections.singletonList(4),
                Collections.singletonList(5)
        );
    }

    @Test
    public void simpleBackpressure() {
        TestSubscriberEx<List<Integer>> ts = new TestSubscriberEx<>(2L);

        Flowable.range(1, 5).onBackpressureReduce(createTestSupplier(), createTestReducer()).subscribe(ts);

        ts.assertNoErrors();
        ts.assertValues(
                Collections.singletonList(1),
                Collections.singletonList(2)
        );
        ts.assertNotComplete();
    }

    @Test
    public void reduceBackpressuredSync() {
        PublishProcessor<Integer> source = PublishProcessor.create();
        TestSubscriberEx<Integer> ts = new TestSubscriberEx<>(0L);

        source.onBackpressureReduce(() -> 0, Integer::sum).subscribe(ts);

        source.onNext(1);
        source.onNext(2);
        source.onNext(3);

        ts.request(1);

        ts.assertValuesOnly(6);

        source.onNext(4);
        source.onComplete();

        ts.assertValuesOnly(6);

        ts.request(1);
        ts.assertResult(6, 4);
    }

    @Test
    public void synchronousDrop() {
        PublishProcessor<Integer> source = PublishProcessor.create();
        TestSubscriberEx<List<Integer>> ts = new TestSubscriberEx<>(0L);

        source.onBackpressureReduce(createTestSupplier(), createTestReducer()).subscribe(ts);

        ts.assertNoValues();

        source.onNext(1);
        ts.request(2);

        ts.assertValues(Collections.singletonList(1));

        source.onNext(2);

        ts.assertValues(
                Collections.singletonList(1),
                Collections.singletonList(2)
        );

        source.onNext(3);
        source.onNext(4);
        source.onNext(5);
        source.onNext(6);

        ts.request(2);

        ts.assertValues(
                Collections.singletonList(1),
                Collections.singletonList(2),
                Arrays.asList(3, 4, 5, 6)
        );

        source.onNext(7);

        ts.assertValues(
                Collections.singletonList(1),
                Collections.singletonList(2),
                Arrays.asList(3, 4, 5, 6),
                Collections.singletonList(7)
        );

        source.onNext(8);
        source.onNext(9);
        source.onComplete();

        ts.request(1);

        ts.assertValues(
                Collections.singletonList(1),
                Collections.singletonList(2),
                Arrays.asList(3, 4, 5, 6),
                Collections.singletonList(7),
                Arrays.asList(8, 9)
        );
        ts.assertNoErrors();
        ts.assertTerminated();
    }

    private <T> TestSubscriberEx<T> createDelayedSubscriber() {
        return new TestSubscriberEx<T>(1L) {
            final Random rnd = new Random();

            @Override
            public void onNext(T t) {
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
    }

    private <T> void assertValuesDropped(TestSubscriberEx<T> ts, int totalValues) {
        int n = ts.values().size();
        System.out.println("testAsynchronousDrop -> " + n);
        Assert.assertTrue("All events received?", n < totalValues);
    }

    private void assertIncreasingSequence(TestSubscriberEx<Integer> ts) {
        int previous = 0;
        for (Integer current : ts.values()) {
            Assert.assertTrue("The sequence must be increasing [current value=" + previous +
                    ", previous value=" + current + "]", previous <= current);
            previous = current;
        }
    }

    @Test
    public void asynchronousDrop() {
        TestSubscriberEx<Integer> ts = createDelayedSubscriber();
        int m = 100000;
        Flowable.range(1, m)
                .subscribeOn(Schedulers.computation())
                .onBackpressureReduce((Supplier<List<Integer>>) Collections::emptyList, (list, current) -> {
                    //in that case it works like onBackpressureLatest
                    //the output sequence of number must be increasing
                    return Collections.singletonList(current);
                })
                .observeOn(Schedulers.io())
                .concatMap(Flowable::fromIterable)
                .subscribe(ts);

        ts.awaitDone(2, TimeUnit.SECONDS);
        ts.assertTerminated();
        assertValuesDropped(ts, m);
        assertIncreasingSequence(ts);
    }

    @Test
    public void asynchronousDrop2() {
        TestSubscriberEx<Long> ts = createDelayedSubscriber();
        int m = 100000;
        Flowable.rangeLong(1, m)
                .subscribeOn(Schedulers.computation())
                .onBackpressureReduce(createTestSupplier(), createTestReducer())
                .observeOn(Schedulers.io())
                .concatMap(list -> Flowable.just(list.stream().reduce(Long::sum).orElseThrow(() -> {
                    throw new IllegalArgumentException("No value in list");
                })))
                .subscribe(ts);

        ts.awaitDone(2, TimeUnit.SECONDS);
        ts.assertTerminated();
        assertValuesDropped(ts, m);
        long sum = 0;
        for (Long i : ts.values()) {
            sum += i;
        }
        //sum = (A1 + An) * n / 2 = 100_001 * 50_000 = 50_000_00000 + 50_000 = 50_000_50_000
        Assert.assertEquals("Wrong sum: " + sum, 5000050000L, sum);
    }

    @Test
    public void nullPointerFromReducer() {
        PublishProcessor<Integer> source = PublishProcessor.create();
        TestSubscriberEx<List<Integer>> ts = new TestSubscriberEx<>(0L);
        source.onBackpressureReduce(createTestSupplier(), (BiFunction<List<Integer>, ? super Integer, List<Integer>>) (list, number) -> null).subscribe(ts);

        source.onNext(1);
        source.onNext(2);

        TestHelper.assertError(ts.errors(), 0, NullPointerException.class, "The reducer returned a null value");
    }

    @Test
    public void nullPointerFromSupplier() {
        PublishProcessor<Integer> source = PublishProcessor.create();
        TestSubscriberEx<List<Integer>> ts = new TestSubscriberEx<>(0L);
        source.onBackpressureReduce(() -> null, createTestReducer()).subscribe(ts);

        source.onNext(1);
        source.onNext(2);

        TestHelper.assertError(ts.errors(), 0, NullPointerException.class, "The supplier returned a null value");
    }

    @Test
    public void exceptionFromReducer() {
        PublishProcessor<Integer> source = PublishProcessor.create();
        TestSubscriberEx<List<Integer>> ts = new TestSubscriberEx<>(0L);
        source.onBackpressureReduce(createTestSupplier(), (BiFunction<List<Integer>, ? super Integer, List<Integer>>) (l, r) -> {
            throw new TestException("Test exception");
        }).subscribe(ts);

        source.onNext(1);
        source.onNext(2);

        TestHelper.assertError(ts.errors(), 0, TestException.class, "Test exception");
    }

    @Test
    public void exceptionFromSupplier() {
        PublishProcessor<Integer> source = PublishProcessor.create();
        TestSubscriberEx<List<Integer>> ts = new TestSubscriberEx<>(0L);
        source.onBackpressureReduce(() -> {
            throw new TestException("Test exception");
        }, createTestReducer()).subscribe(ts);

        source.onNext(1);
        source.onNext(2);

        TestHelper.assertError(ts.errors(), 0, TestException.class, "Test exception");
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeFlowable(f -> f.onBackpressureReduce(createTestSupplier(), createTestReducer()));
    }

    @Test
    public void take() {
        Flowable.just(1, 2)
                .onBackpressureReduce(createTestSupplier(), createTestReducer())
                .take(1)
                .test()
                .assertResult(Collections.singletonList(1));
    }

    @Test
    public void dispose() {
        TestHelper.checkDisposed(Flowable.never().onBackpressureReduce(createTestSupplier(), createTestReducer()));
    }

    @Test
    public void badRequest() {
        TestHelper.assertBadRequestReported(Flowable.never().onBackpressureReduce(createTestSupplier(), createTestReducer()));
    }
}
