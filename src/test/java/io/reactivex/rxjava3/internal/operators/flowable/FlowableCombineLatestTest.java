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

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import io.reactivex.rxjava3.annotations.NonNull;
import org.junit.Test;
import org.mockito.*;
import org.reactivestreams.*;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.exceptions.*;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.internal.functions.Functions;
import io.reactivex.rxjava3.internal.fuseable.QueueFuseable;
import io.reactivex.rxjava3.internal.operators.flowable.FlowableZipTest.ArgsToString;
import io.reactivex.rxjava3.internal.subscriptions.BooleanSubscription;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.processors.PublishProcessor;
import io.reactivex.rxjava3.schedulers.*;
import io.reactivex.rxjava3.subscribers.*;
import io.reactivex.rxjava3.testsupport.*;

public class FlowableCombineLatestTest extends RxJavaTest {

    @Test
    public void combineLatestWithFunctionThatThrowsAnException() {
        Subscriber<String> w = TestHelper.mockSubscriber();

        PublishProcessor<String> w1 = PublishProcessor.create();
        PublishProcessor<String> w2 = PublishProcessor.create();

        Flowable<String> combined = Flowable.combineLatest(w1, w2, (v1, v2) -> {
            throw new RuntimeException("I don't work.");
        });
        combined.subscribe(w);

        w1.onNext("first value of w1");
        w2.onNext("first value of w2");

        verify(w, never()).onNext(anyString());
        verify(w, never()).onComplete();
        verify(w, times(1)).onError(Mockito.<RuntimeException> any());
    }

    @Test
    public void combineLatestDifferentLengthFlowableSequences1() {
        Subscriber<String> w = TestHelper.mockSubscriber();

        PublishProcessor<String> w1 = PublishProcessor.create();
        PublishProcessor<String> w2 = PublishProcessor.create();
        PublishProcessor<String> w3 = PublishProcessor.create();

        Flowable<String> combineLatestW = Flowable.combineLatest(w1, w2, w3,
                getConcat3StringsCombineLatestFunction());
        combineLatestW.subscribe(w);

        /* simulate sending data */
        // once for w1
        w1.onNext("1a");
        w2.onNext("2a");
        w3.onNext("3a");
        w1.onComplete();
        // twice for w2
        w2.onNext("2b");
        w2.onComplete();
        // 4 times for w3
        w3.onNext("3b");
        w3.onNext("3c");
        w3.onNext("3d");
        w3.onComplete();

        /* we should have been called 4 times on the Observer */
        InOrder inOrder = inOrder(w);
        inOrder.verify(w).onNext("1a2a3a");
        inOrder.verify(w).onNext("1a2b3a");
        inOrder.verify(w).onNext("1a2b3b");
        inOrder.verify(w).onNext("1a2b3c");
        inOrder.verify(w).onNext("1a2b3d");
        inOrder.verify(w, never()).onNext(anyString());
        inOrder.verify(w, times(1)).onComplete();
    }

    @Test
    public void combineLatestDifferentLengthFlowableSequences2() {
        Subscriber<String> w = TestHelper.mockSubscriber();

        PublishProcessor<String> w1 = PublishProcessor.create();
        PublishProcessor<String> w2 = PublishProcessor.create();
        PublishProcessor<String> w3 = PublishProcessor.create();

        Flowable<String> combineLatestW = Flowable.combineLatest(w1, w2, w3, getConcat3StringsCombineLatestFunction());
        combineLatestW.subscribe(w);

        /* simulate sending data */
        // 4 times for w1
        w1.onNext("1a");
        w1.onNext("1b");
        w1.onNext("1c");
        w1.onNext("1d");
        w1.onComplete();
        // twice for w2
        w2.onNext("2a");
        w2.onNext("2b");
        w2.onComplete();
        // 1 times for w3
        w3.onNext("3a");
        w3.onComplete();

        /* we should have been called 1 time only on the Observer since we only combine the "latest" we don't go back and loop through others once completed */
        InOrder inOrder = inOrder(w);
        inOrder.verify(w, times(1)).onNext("1d2b3a");
        inOrder.verify(w, never()).onNext(anyString());

        inOrder.verify(w, times(1)).onComplete();

    }

    @Test
    public void combineLatestWithInterleavingSequences() {
        Subscriber<String> w = TestHelper.mockSubscriber();

        PublishProcessor<String> w1 = PublishProcessor.create();
        PublishProcessor<String> w2 = PublishProcessor.create();
        PublishProcessor<String> w3 = PublishProcessor.create();

        Flowable<String> combineLatestW = Flowable.combineLatest(w1, w2, w3, getConcat3StringsCombineLatestFunction());
        combineLatestW.subscribe(w);

        /* simulate sending data */
        w1.onNext("1a");
        w2.onNext("2a");
        w2.onNext("2b");
        w3.onNext("3a");

        w1.onNext("1b");
        w2.onNext("2c");
        w2.onNext("2d");
        w3.onNext("3b");

        w1.onComplete();
        w2.onComplete();
        w3.onComplete();

        /* we should have been called 5 times on the Observer */
        InOrder inOrder = inOrder(w);
        inOrder.verify(w).onNext("1a2b3a");
        inOrder.verify(w).onNext("1b2b3a");
        inOrder.verify(w).onNext("1b2c3a");
        inOrder.verify(w).onNext("1b2d3a");
        inOrder.verify(w).onNext("1b2d3b");

        inOrder.verify(w, never()).onNext(anyString());
        inOrder.verify(w, times(1)).onComplete();
    }

    @Test
    public void combineLatest2Types() {
        BiFunction<String, Integer, String> combineLatestFunction = getConcatStringIntegerCombineLatestFunction();

        /* define an Observer to receive aggregated events */
        Subscriber<String> subscriber = TestHelper.mockSubscriber();

        Flowable<String> w = Flowable.combineLatest(Flowable.just("one", "two"), Flowable.just(2, 3, 4), combineLatestFunction);
        w.subscribe(subscriber);

        verify(subscriber, never()).onError(any(Throwable.class));
        verify(subscriber, times(1)).onComplete();
        verify(subscriber, times(1)).onNext("two2");
        verify(subscriber, times(1)).onNext("two3");
        verify(subscriber, times(1)).onNext("two4");
    }

    @Test
    public void combineLatest3TypesA() {
        Function3<String, Integer, int[], String> combineLatestFunction = getConcatStringIntegerIntArrayCombineLatestFunction();

        /* define an Observer to receive aggregated events */
        Subscriber<String> subscriber = TestHelper.mockSubscriber();

        Flowable<String> w = Flowable.combineLatest(Flowable.just("one", "two"), Flowable.just(2), Flowable.just(new int[] { 4, 5, 6 }), combineLatestFunction);
        w.subscribe(subscriber);

        verify(subscriber, never()).onError(any(Throwable.class));
        verify(subscriber, times(1)).onComplete();
        verify(subscriber, times(1)).onNext("two2[4, 5, 6]");
    }

    @Test
    public void combineLatest3TypesB() {
        Function3<String, Integer, int[], String> combineLatestFunction = getConcatStringIntegerIntArrayCombineLatestFunction();

        /* define an Observer to receive aggregated events */
        Subscriber<String> subscriber = TestHelper.mockSubscriber();

        Flowable<String> w = Flowable.combineLatest(Flowable.just("one"), Flowable.just(2), Flowable.just(new int[] { 4, 5, 6 }, new int[] { 7, 8 }), combineLatestFunction);
        w.subscribe(subscriber);

        verify(subscriber, never()).onError(any(Throwable.class));
        verify(subscriber, times(1)).onComplete();
        verify(subscriber, times(1)).onNext("one2[4, 5, 6]");
        verify(subscriber, times(1)).onNext("one2[7, 8]");
    }

    private Function3<String, String, String, String> getConcat3StringsCombineLatestFunction() {
        return (a1, a2, a3) -> {
            if (a1 == null) {
                a1 = "";
            }
            if (a2 == null) {
                a2 = "";
            }
            if (a3 == null) {
                a3 = "";
            }
            return a1 + a2 + a3;
        };
    }

    private BiFunction<String, Integer, String> getConcatStringIntegerCombineLatestFunction() {
        return (s, i) -> getStringValue(s) + getStringValue(i);
    }

    private Function3<String, Integer, int[], String> getConcatStringIntegerIntArrayCombineLatestFunction() {
        return (s, i, iArray) -> getStringValue(s) + getStringValue(i) + getStringValue(iArray);
    }

    private static String getStringValue(Object o) {
        if (o == null) {
            return "";
        } else {
            if (o instanceof int[]) {
                return Arrays.toString((int[]) o);
            } else {
                return String.valueOf(o);
            }
        }
    }

    BiFunction<Integer, Integer, Integer> or = (t1, t2) -> t1 | t2;

    @Test
    public void combineSimple() {
        PublishProcessor<Integer> a = PublishProcessor.create();
        PublishProcessor<Integer> b = PublishProcessor.create();

        Flowable<Integer> source = Flowable.combineLatest(a, b, or);

        Subscriber<Object> subscriber = TestHelper.mockSubscriber();
        InOrder inOrder = inOrder(subscriber);

        source.subscribe(subscriber);

        a.onNext(1);

        inOrder.verify(subscriber, never()).onNext(any());

        a.onNext(2);

        inOrder.verify(subscriber, never()).onNext(any());

        b.onNext(0x10);

        inOrder.verify(subscriber, times(1)).onNext(0x12);

        b.onNext(0x20);
        inOrder.verify(subscriber, times(1)).onNext(0x22);

        b.onComplete();

        inOrder.verify(subscriber, never()).onComplete();

        a.onComplete();

        inOrder.verify(subscriber, times(1)).onComplete();

        a.onNext(3);
        b.onNext(0x30);
        a.onComplete();
        b.onComplete();

        inOrder.verifyNoMoreInteractions();
        verify(subscriber, never()).onError(any(Throwable.class));
    }

    @Test
    public void combineMultipleObservers() {
        PublishProcessor<Integer> a = PublishProcessor.create();
        PublishProcessor<Integer> b = PublishProcessor.create();

        Flowable<Integer> source = Flowable.combineLatest(a, b, or);

        Subscriber<Object> subscriber1 = TestHelper.mockSubscriber();

        Subscriber<Object> subscriber2 = TestHelper.mockSubscriber();

        InOrder inOrder1 = inOrder(subscriber1);
        InOrder inOrder2 = inOrder(subscriber2);

        source.subscribe(subscriber1);
        source.subscribe(subscriber2);

        a.onNext(1);

        inOrder1.verify(subscriber1, never()).onNext(any());
        inOrder2.verify(subscriber2, never()).onNext(any());

        a.onNext(2);

        inOrder1.verify(subscriber1, never()).onNext(any());
        inOrder2.verify(subscriber2, never()).onNext(any());

        b.onNext(0x10);

        inOrder1.verify(subscriber1, times(1)).onNext(0x12);
        inOrder2.verify(subscriber2, times(1)).onNext(0x12);

        b.onNext(0x20);
        inOrder1.verify(subscriber1, times(1)).onNext(0x22);
        inOrder2.verify(subscriber2, times(1)).onNext(0x22);

        b.onComplete();

        inOrder1.verify(subscriber1, never()).onComplete();
        inOrder2.verify(subscriber2, never()).onComplete();

        a.onComplete();

        inOrder1.verify(subscriber1, times(1)).onComplete();
        inOrder2.verify(subscriber2, times(1)).onComplete();

        a.onNext(3);
        b.onNext(0x30);
        a.onComplete();
        b.onComplete();

        inOrder1.verifyNoMoreInteractions();
        inOrder2.verifyNoMoreInteractions();
        verify(subscriber1, never()).onError(any(Throwable.class));
        verify(subscriber2, never()).onError(any(Throwable.class));
    }

    @Test
    public void firstNeverProduces() {
        PublishProcessor<Integer> a = PublishProcessor.create();
        PublishProcessor<Integer> b = PublishProcessor.create();

        Flowable<Integer> source = Flowable.combineLatest(a, b, or);

        Subscriber<Object> subscriber = TestHelper.mockSubscriber();
        InOrder inOrder = inOrder(subscriber);

        source.subscribe(subscriber);

        b.onNext(0x10);
        b.onNext(0x20);

        a.onComplete();

        inOrder.verify(subscriber, times(1)).onComplete();
        verify(subscriber, never()).onNext(any());
        verify(subscriber, never()).onError(any(Throwable.class));
    }

    @Test
    public void secondNeverProduces() {
        PublishProcessor<Integer> a = PublishProcessor.create();
        PublishProcessor<Integer> b = PublishProcessor.create();

        Flowable<Integer> source = Flowable.combineLatest(a, b, or);

        Subscriber<Object> subscriber = TestHelper.mockSubscriber();
        InOrder inOrder = inOrder(subscriber);

        source.subscribe(subscriber);

        a.onNext(0x1);
        a.onNext(0x2);

        b.onComplete();
        a.onComplete();

        inOrder.verify(subscriber, times(1)).onComplete();
        verify(subscriber, never()).onNext(any());
        verify(subscriber, never()).onError(any(Throwable.class));
    }

    @Test
    public void oneToNSources() {
        int n = 30;
        Function<Object[], List<Object>> func = Arrays::asList;
        for (int i = 1; i <= n; i++) {
            System.out.println("test1ToNSources: " + i + " sources");
            List<Flowable<Integer>> sources = new ArrayList<>();
            List<Object> values = new ArrayList<>();
            for (int j = 0; j < i; j++) {
                sources.add(Flowable.just(j));
                values.add(j);
            }

            Flowable<List<Object>> result = Flowable.combineLatest(sources, func);

            Subscriber<List<Object>> subscriber = TestHelper.mockSubscriber();

            result.subscribe(subscriber);

            verify(subscriber).onNext(values);
            verify(subscriber).onComplete();
            verify(subscriber, never()).onError(any(Throwable.class));
        }
    }

    @Test
    public void oneToNSourcesScheduled() throws InterruptedException {
        int n = 10;
        Function<Object[], List<Object>> func = Arrays::asList;
        for (int i = 1; i <= n; i++) {
            System.out.println("test1ToNSourcesScheduled: " + i + " sources");
            List<Flowable<Integer>> sources = new ArrayList<>();
            List<Object> values = new ArrayList<>();
            for (int j = 0; j < i; j++) {
                sources.add(Flowable.just(j).subscribeOn(Schedulers.io()));
                values.add(j);
            }

            Flowable<List<Object>> result = Flowable.combineLatest(sources, func);

            final Subscriber<List<Object>> subscriber = TestHelper.mockSubscriber();

            final CountDownLatch cdl = new CountDownLatch(1);

            Subscriber<List<Object>> s = new DefaultSubscriber<List<Object>>() {

                @Override
                public void onNext(List<Object> t) {
                    subscriber.onNext(t);
                }

                @Override
                public void onError(Throwable e) {
                    subscriber.onError(e);
                    cdl.countDown();
                }

                @Override
                public void onComplete() {
                    subscriber.onComplete();
                    cdl.countDown();
                }
            };

            result.subscribe(s);

            cdl.await();

            verify(subscriber).onNext(values);
            verify(subscriber).onComplete();
            verify(subscriber, never()).onError(any(Throwable.class));
        }
    }

    @Test
    public void twoSourcesOverload() {
        Flowable<Integer> s1 = Flowable.just(1);
        Flowable<Integer> s2 = Flowable.just(2);

        Flowable<List<Integer>> result = Flowable.combineLatest(s1, s2,
                Arrays::asList);

        Subscriber<Object> subscriber = TestHelper.mockSubscriber();

        result.subscribe(subscriber);

        verify(subscriber).onNext(Arrays.asList(1, 2));
        verify(subscriber).onComplete();
        verify(subscriber, never()).onError(any(Throwable.class));
    }

    @Test
    public void threeSourcesOverload() {
        Flowable<Integer> s1 = Flowable.just(1);
        Flowable<Integer> s2 = Flowable.just(2);
        Flowable<Integer> s3 = Flowable.just(3);

        Flowable<List<Integer>> result = Flowable.combineLatest(s1, s2, s3,
                Arrays::asList);

        Subscriber<Object> subscriber = TestHelper.mockSubscriber();

        result.subscribe(subscriber);

        verify(subscriber).onNext(Arrays.asList(1, 2, 3));
        verify(subscriber).onComplete();
        verify(subscriber, never()).onError(any(Throwable.class));
    }

    @Test
    public void fourSourcesOverload() {
        Flowable<Integer> s1 = Flowable.just(1);
        Flowable<Integer> s2 = Flowable.just(2);
        Flowable<Integer> s3 = Flowable.just(3);
        Flowable<Integer> s4 = Flowable.just(4);

        Flowable<List<Integer>> result = Flowable.combineLatest(s1, s2, s3, s4,
                Arrays::asList);

        Subscriber<Object> subscriber = TestHelper.mockSubscriber();

        result.subscribe(subscriber);

        verify(subscriber).onNext(Arrays.asList(1, 2, 3, 4));
        verify(subscriber).onComplete();
        verify(subscriber, never()).onError(any(Throwable.class));
    }

    @Test
    public void fiveSourcesOverload() {
        Flowable<Integer> s1 = Flowable.just(1);
        Flowable<Integer> s2 = Flowable.just(2);
        Flowable<Integer> s3 = Flowable.just(3);
        Flowable<Integer> s4 = Flowable.just(4);
        Flowable<Integer> s5 = Flowable.just(5);

        Flowable<List<Integer>> result = Flowable.combineLatest(s1, s2, s3, s4, s5,
                Arrays::asList);

        Subscriber<Object> subscriber = TestHelper.mockSubscriber();

        result.subscribe(subscriber);

        verify(subscriber).onNext(Arrays.asList(1, 2, 3, 4, 5));
        verify(subscriber).onComplete();
        verify(subscriber, never()).onError(any(Throwable.class));
    }

    @Test
    public void sixSourcesOverload() {
        Flowable<Integer> s1 = Flowable.just(1);
        Flowable<Integer> s2 = Flowable.just(2);
        Flowable<Integer> s3 = Flowable.just(3);
        Flowable<Integer> s4 = Flowable.just(4);
        Flowable<Integer> s5 = Flowable.just(5);
        Flowable<Integer> s6 = Flowable.just(6);

        Flowable<List<Integer>> result = Flowable.combineLatest(s1, s2, s3, s4, s5, s6,
                Arrays::asList);

        Subscriber<Object> subscriber = TestHelper.mockSubscriber();

        result.subscribe(subscriber);

        verify(subscriber).onNext(Arrays.asList(1, 2, 3, 4, 5, 6));
        verify(subscriber).onComplete();
        verify(subscriber, never()).onError(any(Throwable.class));
    }

    @Test
    public void sevenSourcesOverload() {
        Flowable<Integer> s1 = Flowable.just(1);
        Flowable<Integer> s2 = Flowable.just(2);
        Flowable<Integer> s3 = Flowable.just(3);
        Flowable<Integer> s4 = Flowable.just(4);
        Flowable<Integer> s5 = Flowable.just(5);
        Flowable<Integer> s6 = Flowable.just(6);
        Flowable<Integer> s7 = Flowable.just(7);

        Flowable<List<Integer>> result = Flowable.combineLatest(s1, s2, s3, s4, s5, s6, s7,
                Arrays::asList);

        Subscriber<Object> subscriber = TestHelper.mockSubscriber();

        result.subscribe(subscriber);

        verify(subscriber).onNext(Arrays.asList(1, 2, 3, 4, 5, 6, 7));
        verify(subscriber).onComplete();
        verify(subscriber, never()).onError(any(Throwable.class));
    }

    @Test
    public void eightSourcesOverload() {
        Flowable<Integer> s1 = Flowable.just(1);
        Flowable<Integer> s2 = Flowable.just(2);
        Flowable<Integer> s3 = Flowable.just(3);
        Flowable<Integer> s4 = Flowable.just(4);
        Flowable<Integer> s5 = Flowable.just(5);
        Flowable<Integer> s6 = Flowable.just(6);
        Flowable<Integer> s7 = Flowable.just(7);
        Flowable<Integer> s8 = Flowable.just(8);

        Flowable<List<Integer>> result = Flowable.combineLatest(s1, s2, s3, s4, s5, s6, s7, s8,
                Arrays::asList);

        Subscriber<Object> subscriber = TestHelper.mockSubscriber();

        result.subscribe(subscriber);

        verify(subscriber).onNext(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8));
        verify(subscriber).onComplete();
        verify(subscriber, never()).onError(any(Throwable.class));
    }

    @Test
    public void nineSourcesOverload() {
        Flowable<Integer> s1 = Flowable.just(1);
        Flowable<Integer> s2 = Flowable.just(2);
        Flowable<Integer> s3 = Flowable.just(3);
        Flowable<Integer> s4 = Flowable.just(4);
        Flowable<Integer> s5 = Flowable.just(5);
        Flowable<Integer> s6 = Flowable.just(6);
        Flowable<Integer> s7 = Flowable.just(7);
        Flowable<Integer> s8 = Flowable.just(8);
        Flowable<Integer> s9 = Flowable.just(9);

        Flowable<List<Integer>> result = Flowable.combineLatest(s1, s2, s3, s4, s5, s6, s7, s8, s9,
                Arrays::asList);

        Subscriber<Object> subscriber = TestHelper.mockSubscriber();

        result.subscribe(subscriber);

        verify(subscriber).onNext(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9));
        verify(subscriber).onComplete();
        verify(subscriber, never()).onError(any(Throwable.class));
    }

    @Test
    public void zeroSources() {
        Flowable<Object> result = Flowable.combineLatest(
                Collections.<Flowable<Object>> emptyList(), args -> args);

        Subscriber<Object> subscriber = TestHelper.mockSubscriber();

        result.subscribe(subscriber);

        verify(subscriber).onComplete();
        verify(subscriber, never()).onNext(any());
        verify(subscriber, never()).onError(any(Throwable.class));

    }

    @Test
    public void backpressureLoop() {
        for (int i = 0; i < 5000; i++) {
            backpressure();
        }
    }

    @Test
    public void backpressure() {
        BiFunction<String, Integer, String> combineLatestFunction = getConcatStringIntegerCombineLatestFunction();

        int num = Flowable.bufferSize() * 4;
        TestSubscriber<String> ts = new TestSubscriber<>();
        Flowable.combineLatest(
                Flowable.just("one", "two"),
                Flowable.range(2, num),
                combineLatestFunction
        )
        .observeOn(Schedulers.computation())
        .subscribe(ts);

        ts.awaitDone(5, TimeUnit.SECONDS);
        ts.assertNoErrors();
        List<String> events = ts.values();
        assertEquals("two2", events.get(0));
        assertEquals("two3", events.get(1));
        assertEquals("two4", events.get(2));
        assertEquals(num, events.size());
    }

    @Test
    public void withCombineLatestIssue1717() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicInteger count = new AtomicInteger();
        final int SIZE = 2000;
        Flowable<Long> timer = Flowable.interval(0, 1, TimeUnit.MILLISECONDS)
                .observeOn(Schedulers.newThread())
                .doOnEach(n -> {
                        //                        System.out.println(n);
                        if (count.incrementAndGet() >= SIZE) {
                            latch.countDown();
                        }
                }).take(SIZE);

        TestSubscriber<Long> ts = new TestSubscriber<>();

        Flowable.combineLatest(timer, Flowable.<Integer> never(), (t1, t2) -> t1).subscribe(ts);

        if (!latch.await(SIZE + 2000, TimeUnit.MILLISECONDS)) {
            fail("timed out");
        }

        assertEquals(SIZE, count.get());
    }

    @Test
    public void combineLatestRequestOverflow() throws InterruptedException {
        List<Flowable<Integer>> sources = Arrays.asList(Flowable.fromArray(1, 2, 3, 4),
                Flowable.fromArray(5, 6, 7, 8));
        Flowable<Integer> f = Flowable.combineLatest(sources, args -> (Integer) args[0]);
        //should get at least 4
        final CountDownLatch latch = new CountDownLatch(4);
        f.subscribeOn(Schedulers.computation()).subscribe(new DefaultSubscriber<Integer>() {

            @Override
            public void onStart() {
                request(2);
            }

            @Override
            public void onComplete() {
                //ignore
            }

            @Override
            public void onError(Throwable e) {
                throw new RuntimeException(e);
            }

            @Override
            public void onNext(Integer t) {
                latch.countDown();
                request(Long.MAX_VALUE - 1);
            }});
        assertTrue(latch.await(10, TimeUnit.SECONDS));
    }

    private static final Function<Object[], Integer> THROW_NON_FATAL = args -> {
        throw new RuntimeException();
    };

    @Test
    public void nonFatalExceptionThrownByCombinatorForSingleSourceIsNotReportedByUpstreamOperator() {
        final AtomicBoolean errorOccurred = new AtomicBoolean(false);
        TestSubscriber<Integer> ts = TestSubscriber.create(1);
        Flowable<Integer> source = Flowable.just(1)
          // if haven't caught exception in combineLatest operator then would incorrectly
          // be picked up by this call to doOnError
          .doOnError(t -> errorOccurred.set(true));
        Flowable
          .combineLatest(Collections.singletonList(source), THROW_NON_FATAL)
          .subscribe(ts);
        assertFalse(errorOccurred.get());
    }

    @Test
    public void combineLatestIterable() {
        Flowable<Integer> source = Flowable.just(1);

        TestSubscriber<Integer> ts = TestSubscriber.create();

        Flowable.combineLatest(Arrays.asList(source, source),
                args -> (Integer)args[0] + (Integer)args[1])
        .subscribe(ts);

        ts.assertValue(2);
        ts.assertNoErrors();
        ts.assertComplete();
    }

    @Test
    public void combineMany() {
        int n = Flowable.bufferSize() * 3;

        List<Flowable<Integer>> sources = new ArrayList<>();

        StringBuilder expected = new StringBuilder(n * 2);

        for (int i = 0; i < n; i++) {
            sources.add(Flowable.just(i));
            expected.append(i);
        }

        TestSubscriber<String> ts = TestSubscriber.create();

        Flowable.combineLatest(sources, args -> {
            StringBuilder b = new StringBuilder();
            for (Object o : args) {
                b.append(o);
            }
            return b.toString();
        }).subscribe(ts);

        ts.assertNoErrors();
        ts.assertValue(expected.toString());
        ts.assertComplete();
    }

    @Test
    public void firstJustError() {
        TestSubscriber<Integer> ts = TestSubscriber.create();

        Flowable.combineLatestDelayError(
                Arrays.asList(Flowable.just(1), Flowable.<Integer>error(new TestException())),
                args -> ((Integer)args[0]) + ((Integer)args[1])
        ).subscribe(ts);

        ts.assertNoValues();
        ts.assertError(TestException.class);
        ts.assertNotComplete();
    }

    @Test
    public void secondJustError() {
        TestSubscriber<Integer> ts = TestSubscriber.create();

        Flowable.combineLatestDelayError(
                Arrays.asList(Flowable.<Integer>error(new TestException()), Flowable.just(1)),
                args -> ((Integer)args[0]) + ((Integer)args[1])
        ).subscribe(ts);

        ts.assertNoValues();
        ts.assertError(TestException.class);
        ts.assertNotComplete();
    }

    @Test
    public void oneErrors() {
        TestSubscriber<Integer> ts = TestSubscriber.create();

        Flowable.combineLatestDelayError(
                Arrays.asList(Flowable.just(10).concatWith(Flowable.<Integer>error(new TestException())), Flowable.just(1)),
                args -> ((Integer)args[0]) + ((Integer)args[1])
        ).subscribe(ts);

        ts.assertValues(11);
        ts.assertError(TestException.class);
        ts.assertNotComplete();
    }

    @Test
    public void twoErrors() {
        TestSubscriber<Integer> ts = TestSubscriber.create();

        Flowable.combineLatestDelayError(
                Arrays.asList(Flowable.just(1), Flowable.just(10).concatWith(Flowable.<Integer>error(new TestException()))),
                args -> ((Integer)args[0]) + ((Integer)args[1])
        ).subscribe(ts);

        ts.assertValues(11);
        ts.assertError(TestException.class);
        ts.assertNotComplete();
    }

    @Test
    public void bothError() {
        TestSubscriber<Integer> ts = TestSubscriber.create();

        Flowable.combineLatestDelayError(
                Arrays.asList(Flowable.just(1).concatWith(Flowable.<Integer>error(new TestException())),
                        Flowable.just(10).concatWith(Flowable.<Integer>error(new TestException()))),
                args -> ((Integer)args[0]) + ((Integer)args[1])
        ).subscribe(ts);

        ts.assertValues(11);
        ts.assertError(CompositeException.class);
        ts.assertNotComplete();
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void combineLatestNArguments() throws Exception {
        Flowable source = Flowable.just(1);

        for (int i = 2; i < 10; i++) {
            Class<?>[] types = new Class[i + 1];
            Arrays.fill(types, Publisher.class);
            types[i] = i == 2 ? BiFunction.class : Class.forName("io.reactivex.rxjava3.functions.Function" + i);

            Method m = Flowable.class.getMethod("combineLatest", types);

            Object[] params = new Object[i + 1];
            Arrays.fill(params, source);
            params[i] = ArgsToString.INSTANCE;

            StringBuilder b = new StringBuilder();
            for (int j = 0; j < i; j++) {
                b.append('1');
            }

            ((Flowable)m.invoke(null, params)).test().assertResult(b.toString());

            for (int j = 0; j < params.length; j++) {
                Object[] params0 = params.clone();
                params0[j] = null;

                try {
                    m.invoke(null, params0);
                    fail("Should have thrown @ " + m);
                } catch (InvocationTargetException ex) {
                    assertTrue(ex.toString(), ex.getCause() instanceof NullPointerException);

                    if (j < i) {
                        assertEquals("source" + (j + 1) + " is null", ex.getCause().getMessage());
                    } else {
                        assertEquals("combiner is null", ex.getCause().getMessage());
                    }
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void combineLatestArrayNSources() {
        for (int i = 1; i < 100; i++) {
            Flowable<Integer>[] sources = new Flowable[i];
            Arrays.fill(sources, Flowable.just(1));
            List<Object> expected = new ArrayList<>(i);
            for (int j = 1; j <= i; j++) {
                expected.add(1);
            }

            Flowable.combineLatestArray(sources, Arrays::asList)
            .test()
            .assertResult(expected);

            Flowable.combineLatestArrayDelayError(sources, Arrays::asList)
            .test()
            .assertResult(expected);
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void combineLatestArrayOfSources() {

        Flowable.combineLatestArray(new Flowable[] {
                Flowable.just(1), Flowable.just(2)
        }, (Function<Object[], Object>) Arrays::toString)
        .test()
        .assertResult("[1, 2]");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void combineLatestDelayErrorArrayOfSources() {

        Flowable.combineLatestArrayDelayError(new Flowable[] {
                Flowable.just(1), Flowable.just(2)
        }, (Function<Object[], Object>) Arrays::toString)
        .test()
        .assertResult("[1, 2]");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void combineLatestDelayErrorArrayOfSourcesWithError() {

        Flowable.combineLatestArrayDelayError(new Flowable[] {
                Flowable.just(1), Flowable.just(2).concatWith(Flowable.<Integer>error(new TestException()))
        }, (Function<Object[], Object>) Arrays::toString)
        .test()
        .assertFailure(TestException.class, "[1, 2]");
    }

    @Test
    public void combineLatestDelayErrorIterableOfSources() {

        Flowable.combineLatestDelayError(Arrays.asList(
                Flowable.just(1), Flowable.just(2)
        ), (Function<Object[], Object>) Arrays::toString)
        .test()
        .assertResult("[1, 2]");
    }

    @Test
    public void combineLatestDelayErrorIterableOfSourcesWithError() {

        Flowable.combineLatestDelayError(Arrays.asList(
                Flowable.just(1), Flowable.just(2).concatWith(Flowable.<Integer>error(new TestException()))
        ), (Function<Object[], Object>) Arrays::toString)
        .test()
        .assertFailure(TestException.class, "[1, 2]");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void combineLatestArrayEmpty() {
        assertSame(Flowable.empty(), Flowable.combineLatestArray(new Flowable[0], Functions.<Object[]>identity(), 16));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void combineLatestDelayErrorEmpty() {
        assertSame(Flowable.empty(), Flowable.combineLatestArrayDelayError(new Flowable[0], Functions.<Object[]>identity(), 16));
    }

    @Test
    public void error() {
        Flowable.combineLatest(Flowable.never(), Flowable.error(new TestException()), (a, b) -> a)
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void disposed() {
        TestHelper.checkDisposed(Flowable.combineLatest(Flowable.never(), Flowable.never(), (a, b) -> a));
    }

    @Test
    public void cancelWhileSubscribing() {
        final TestSubscriber<Object> ts = new TestSubscriber<>();

        Flowable.combineLatest(
                Flowable.just(1)
                .doOnNext(v -> ts.cancel()),
                Flowable.never(),
                (BiFunction<Object, Object, Object>) (a, b) -> a)
        .subscribe(ts);
    }

    @Test
    public void onErrorRace() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            List<Throwable> errors = TestHelper.trackPluginErrors();
            try {
                final PublishProcessor<Integer> pp1 = PublishProcessor.create();
                final PublishProcessor<Integer> pp2 = PublishProcessor.create();

                TestSubscriberEx<Integer> ts = Flowable.combineLatest(pp1, pp2, (a, b) -> a).to(TestHelper.<Integer>testConsumer());

                final TestException ex1 = new TestException();
                final TestException ex2 = new TestException();

                Runnable r1 = () -> pp1.onError(ex1);
                Runnable r2 = () -> pp2.onError(ex2);

                TestHelper.race(r1, r2);

                if (ts.errors().size() != 0) {
                    if (ts.errors().get(0) instanceof CompositeException) {
                        ts.assertSubscribed()
                        .assertNotComplete()
                        .assertNoValues();

                        for (Throwable e : TestHelper.errorList(ts)) {
                            assertTrue(e.toString(), e instanceof TestException);
                        }

                    } else {
                        ts.assertFailure(TestException.class);
                    }
                }

                for (Throwable e : errors) {
                    assertTrue(e.toString(), e.getCause() instanceof TestException);
                }
            } finally {
                RxJavaPlugins.reset();
            }
        }
    }

    @Test
    public void combineAsync() {
        Flowable<Integer> source = Flowable.range(1, 1000).subscribeOn(Schedulers.computation());

        Flowable.combineLatest(source, source, (BiFunction<Object, Object, Object>) (a, b) -> a)
        .take(500)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertNoErrors()
        .assertComplete();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void errorDelayed() {
        Flowable.combineLatestArrayDelayError(
                new Publisher[] { Flowable.error(new TestException()), Flowable.just(1) },
                (Function<Object[], Object>) a -> a,
                128
        )
        .test()
        .assertFailure(TestException.class);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void errorDelayed2() {
        Flowable.combineLatestArrayDelayError(
                new Publisher[] { Flowable.error(new TestException()).startWithItem(1), Flowable.empty() },
                (Function<Object[], Object>) a -> a,
                128
        )
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void dontSubscribeIfDone() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            final int[] count = { 0 };

            Flowable.combineLatest(Flowable.empty(),
                    Flowable.error(new TestException())
                    .doOnSubscribe(s -> count[0]++),
                    (BiFunction<Object, Object, Object>) (a, b) -> 0)
            .test()
            .assertResult();

            assertEquals(0, count[0]);

            assertTrue(errors.toString(), errors.isEmpty());
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void dontSubscribeIfDone2() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            final int[] count = { 0 };

            Flowable.combineLatestDelayError(
                    Arrays.asList(Flowable.empty(),
                        Flowable.error(new TestException())
                        .doOnSubscribe(s -> count[0]++)
                    ),
                    (Function<Object[], Object>) a -> 0)
            .test()
            .assertResult();

            assertEquals(0, count[0]);

            assertTrue(errors.toString(), errors.isEmpty());
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void combine2Flowable2Errors() throws Exception {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            TestSubscriber<Object> testSubscriber = TestSubscriber.create();

            TestScheduler testScheduler = new TestScheduler();

            Flowable<Integer> emptyFlowable = Flowable.timer(10, TimeUnit.MILLISECONDS, testScheduler)
                    .flatMap((Function<Long, Publisher<Integer>>) aLong -> Flowable.error(new Exception()));
            Flowable<Object> errorFlowable = Flowable.timer(100, TimeUnit.MILLISECONDS, testScheduler).map(aLong -> {
                throw new Exception();
            });

            Flowable.combineLatestDelayError(
                    Arrays.asList(
                            emptyFlowable
                                    .doOnEach(integerNotification -> System.out.println("emptyFlowable: " + integerNotification))
                                    .doFinally(() -> System.out.println("emptyFlowable: doFinally")),
                            errorFlowable
                                    .doOnEach(integerNotification -> System.out.println("errorFlowable: " + integerNotification))
                                    .doFinally(() -> System.out.println("errorFlowable: doFinally"))),
                    (Function<Object[], Object>) objects -> 0
            )
                    .doOnEach(integerNotification -> System.out.println("combineLatestDelayError: " + integerNotification))
                    .doFinally(() -> System.out.println("combineLatestDelayError: doFinally"))
                    .subscribe(testSubscriber);

            testScheduler.advanceTimeBy(100, TimeUnit.MILLISECONDS);

            testSubscriber.awaitDone(5, TimeUnit.SECONDS);

            assertTrue(errors.toString(), errors.isEmpty());
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void eagerDispose() {
        final PublishProcessor<Integer> pp1 = PublishProcessor.create();
        final PublishProcessor<Integer> pp2 = PublishProcessor.create();

        TestSubscriber<Integer> ts = new TestSubscriber<Integer>() {
            @Override
            public void onNext(@NonNull Integer t) {
                super.onNext(t);
                cancel();
                if (pp1.hasSubscribers()) {
                    onError(new IllegalStateException("pp1 not disposed"));
                } else
                if (pp2.hasSubscribers()) {
                    onError(new IllegalStateException("pp2 not disposed"));
                } else {
                    onComplete();
                }
            }
        };

        Flowable.combineLatest(pp1, pp2, Integer::sum)
        .subscribe(ts);

        pp1.onNext(1);
        pp2.onNext(2);
        ts.assertResult(3);
    }

    @Test
    public void fusedNullCheck() {
        TestSubscriberEx<Integer> ts = new TestSubscriberEx<Integer>().setInitialFusionMode(QueueFuseable.ASYNC);

        Flowable.combineLatest(Flowable.just(1), Flowable.just(2), (BiFunction<Integer, Integer, Integer>) (t1, t2) -> null)
        .subscribe(ts);

        ts
        .assertFusionMode(QueueFuseable.ASYNC)
        .assertFailureAndMessage(NullPointerException.class, "The combiner returned a null value");
    }

    @Test
    public void syncFirstErrorsAfterItemDelayError() {
        Flowable.combineLatestDelayError(Arrays.asList(
                    Flowable.just(21).concatWith(Flowable.<Integer>error(new TestException())),
                    Flowable.just(21).delay(100, TimeUnit.MILLISECONDS)
                ),
                (Function<Object[], Object>) a -> (Integer)a[0] + (Integer)a[1]
        )
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(TestException.class, 42);
    }

    @Test
    public void publishersInIterable() {
        Publisher<Integer> source = subscriber -> Flowable.just(1).subscribe(subscriber);

        Flowable.combineLatest(Arrays.asList(source, source), t -> 2)
        .test()
        .assertResult(2);
    }

    @Test
    public void FlowableSourcesInIterable() {
        Flowable<Integer> source = new Flowable<Integer>() {
            @Override
            public void subscribeActual(@NonNull Subscriber<? super Integer> s) {
                Flowable.just(1).subscribe(s);
            }
        };

        Flowable.combineLatest(Arrays.asList(source, source), t -> 2)
        .test()
        .assertResult(2);
    }

    @Test
    public void onCompleteDisposeRace() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {

            TestSubscriber<Integer> ts = new TestSubscriber<>();
            PublishProcessor<Integer> pp = PublishProcessor.create();

            Flowable.combineLatest(pp, Flowable.never(), (a, b) -> a)
            .subscribe(ts);

            TestHelper.race(pp::onComplete, ts::cancel);
        }
    }

    @Test
    public void onErrorDisposeDelayErrorRace() throws Throwable {
        TestHelper.withErrorTracking(errors -> {
            TestException ex = new TestException();

            for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {

                TestSubscriberEx<Object[]> ts = new TestSubscriberEx<>();
                AtomicReference<Subscriber<? super Object>> ref = new AtomicReference<>();
                Flowable<Object> f = new Flowable<Object>() {
                    @Override
                    public void subscribeActual(@NonNull Subscriber<? super Object> s) {
                        ref.set(s);
                    }
                };

                Flowable.combineLatestDelayError(Arrays.asList(f, Flowable.never()), (a) -> a)
                .subscribe(ts);

                ref.get().onSubscribe(new BooleanSubscription());

                TestHelper.race(() -> ref.get().onError(ex), ts::cancel);

                if (ts.errors().isEmpty()) {
                    TestHelper.assertUndeliverable(errors, 0, TestException.class);
                }
            }
        });
    }

    @Test
    public void doneButNotEmpty() {
        PublishProcessor<Integer> pp1 = PublishProcessor.create();
        PublishProcessor<Integer> pp2 = PublishProcessor.create();

        TestSubscriber<Integer> ts = Flowable.combineLatest(pp1, pp2, Integer::sum)
        .doOnNext(v -> {
            if (v == 2) {
                pp2.onNext(3);
                pp2.onComplete();
                pp1.onComplete();
            }
        })
        .test();

        pp1.onNext(1);
        pp2.onNext(1);

        ts.assertResult(2, 4);
    }

    @Test
    public void iterableNullPublisher() {
        Flowable.combineLatest(Arrays.asList(Flowable.never(), null), (a) -> a)
        .test()
        .assertFailure(NullPointerException.class);
    }

    @Test
    public void badRequest() {
        TestHelper.assertBadRequestReported(Flowable.combineLatest(Flowable.never(), Flowable.never(), (a, b) -> a));
    }

    @Test
    public void syncFusionRejected() {
        TestSubscriberEx<Object> ts = new TestSubscriberEx<>();
        ts.setInitialFusionMode(QueueFuseable.SYNC);

        Flowable.combineLatest(Flowable.never(), Flowable.never(), (a, b) -> a)
        .subscribe(ts);

        ts.assertFuseable()
        .assertFusionMode(QueueFuseable.NONE);
    }

    @Test
    public void bounderyFusionRejected() {
        TestSubscriberEx<Object> ts = new TestSubscriberEx<>();
        ts.setInitialFusionMode(QueueFuseable.ANY | QueueFuseable.BOUNDARY);

        Flowable.combineLatest(Flowable.never(), Flowable.never(), (a, b) -> a)
        .subscribe(ts);

        ts.assertFuseable()
        .assertFusionMode(QueueFuseable.NONE);
    }

    @Test
    public void fusedNormal() {
        TestSubscriberEx<Integer> ts = new TestSubscriberEx<>();
        ts.setInitialFusionMode(QueueFuseable.ANY);

        Flowable.combineLatest(Flowable.just(1), Flowable.just(2), Integer::sum)
        .subscribeWith(ts)
        .assertFuseable()
        .assertFusionMode(QueueFuseable.ASYNC)
        .assertResult(3);
    }

    @Test
    public void fusedToParallel() {
        TestSubscriberEx<Integer> ts = new TestSubscriberEx<>();
        ts.setInitialFusionMode(QueueFuseable.ANY);

        Flowable.combineLatest(Flowable.just(1), Flowable.just(2), Integer::sum)
        .parallel()
        .sequential()
        .subscribeWith(ts)
        .assertResult(3);
    }

    @Test
    public void fusedToParallel2() {
        TestSubscriberEx<Integer> ts = new TestSubscriberEx<>();
        ts.setInitialFusionMode(QueueFuseable.ANY);

        Flowable.combineLatest(Flowable.just(1), Flowable.just(2), Integer::sum)
        .compose(TestHelper.flowableStripBoundary())
        .parallel()
        .sequential()
        .subscribeWith(ts)
        .assertResult(3);
    }

    @Test
    public void fusedError() {
        TestSubscriberEx<Integer> ts = new TestSubscriberEx<>();
        ts.setInitialFusionMode(QueueFuseable.ANY);

        Flowable.combineLatest(Flowable.just(1), Flowable.<Integer>error(new TestException()), Integer::sum)
        .subscribeWith(ts)
        .assertFuseable()
        .assertFusionMode(QueueFuseable.ASYNC)
        .assertFailure(TestException.class);
    }

    @Test
    public void nonFusedMoreWorkBeforeTermination() {
        PublishProcessor<Integer> pp = PublishProcessor.create();

        TestSubscriber<Integer> ts = Flowable.combineLatest(pp, Flowable.just(1), Integer::sum)
        .doOnNext(v -> {
            if (v == 1) {
                pp.onNext(2);
                pp.onComplete();
            }
        })
        .test();

        pp.onNext(0);

        ts.assertResult(1, 3);
    }

    @Test
    public void nonFusedDelayErrorMoreWorkBeforeTermination() {
        PublishProcessor<Integer> pp = PublishProcessor.create();

        TestSubscriber<List<Object>> ts = Flowable.combineLatestDelayError(Arrays.asList(pp, Flowable.just(1)), Arrays::asList)
        .doOnNext(v -> {
            if (((Integer)v.get(0)) == 0) {
                pp.onNext(2);
                pp.onComplete();
            }
        })
        .test();

        pp.onNext(0);

        ts.assertResult(Arrays.asList(0, 1), Arrays.asList(2, 1));
    }

    @Test
    public void fusedCombinerCrashError() {
        TestSubscriberEx<Object> ts = new TestSubscriberEx<>();
        ts.setInitialFusionMode(QueueFuseable.ANY);

        Flowable.combineLatest(Flowable.just(1), Flowable.just(1), (a, b) -> { throw new TestException(); })
        .subscribeWith(ts)
        .assertFuseable()
        .assertFusionMode(QueueFuseable.ASYNC)
        .assertFailure(TestException.class);
    }

    @Test
    public void fusedCombinerCrashError2() {
        Flowable.combineLatest(Flowable.just(1), Flowable.just(1), (a, b) -> { throw new TestException(); })
        .compose(TestHelper.flowableStripBoundary())
        .rebatchRequests(10)
        .test()
        .assertFailure(TestException.class);
    }
}
