/**
 * Copyright 2015 Netflix, Inc.
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

package io.reactivex.internal.operators.nbp;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.*;

import org.junit.Test;
import org.mockito.*;

import io.reactivex.*;
import io.reactivex.NbpObservable.NbpSubscriber;
import io.reactivex.functions.*;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.nbp.NbpPublishSubject;
import io.reactivex.subscribers.nbp.NbpTestSubscriber;

public class NbpOnSubscribeCombineLatestTest {

    @Test
    public void testCombineLatestWithFunctionThatThrowsAnException() {
        NbpSubscriber<String> w = TestHelper.mockNbpSubscriber();

        NbpPublishSubject<String> w1 = NbpPublishSubject.create();
        NbpPublishSubject<String> w2 = NbpPublishSubject.create();

        NbpObservable<String> combined = NbpObservable.combineLatest(w1, w2, (v1, v2) -> {
            throw new RuntimeException("I don't work.");
        });
        combined.subscribe(w);

        w1.onNext("first value of w1");
        w2.onNext("first value of w2");

        verify(w, never()).onNext(anyString());
        verify(w, never()).onComplete();
        verify(w, times(1)).onError(Matchers.<RuntimeException> any());
    }

    @Test
    public void testCombineLatestDifferentLengthObservableSequences1() {
        NbpSubscriber<String> w = TestHelper.mockNbpSubscriber();

        NbpPublishSubject<String> w1 = NbpPublishSubject.create();
        NbpPublishSubject<String> w2 = NbpPublishSubject.create();
        NbpPublishSubject<String> w3 = NbpPublishSubject.create();

        NbpObservable<String> combineLatestW = NbpObservable.combineLatest(w1, w2, w3, 
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

        /* we should have been called 4 times on the NbpObserver */
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
    public void testCombineLatestDifferentLengthObservableSequences2() {
        NbpSubscriber<String> w = TestHelper.mockNbpSubscriber();

        NbpPublishSubject<String> w1 = NbpPublishSubject.create();
        NbpPublishSubject<String> w2 = NbpPublishSubject.create();
        NbpPublishSubject<String> w3 = NbpPublishSubject.create();

        NbpObservable<String> combineLatestW = NbpObservable.combineLatest(w1, w2, w3, getConcat3StringsCombineLatestFunction());
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

        /* we should have been called 1 time only on the NbpObserver since we only combine the "latest" we don't go back and loop through others once completed */
        InOrder inOrder = inOrder(w);
        inOrder.verify(w, times(1)).onNext("1d2b3a");
        inOrder.verify(w, never()).onNext(anyString());

        inOrder.verify(w, times(1)).onComplete();

    }

    @Test
    public void testCombineLatestWithInterleavingSequences() {
        NbpSubscriber<String> w = TestHelper.mockNbpSubscriber();

        NbpPublishSubject<String> w1 = NbpPublishSubject.create();
        NbpPublishSubject<String> w2 = NbpPublishSubject.create();
        NbpPublishSubject<String> w3 = NbpPublishSubject.create();

        NbpObservable<String> combineLatestW = NbpObservable.combineLatest(w1, w2, w3, getConcat3StringsCombineLatestFunction());
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

        /* we should have been called 5 times on the NbpObserver */
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
    public void testCombineLatest2Types() {
        BiFunction<String, Integer, String> combineLatestFunction = getConcatStringIntegerCombineLatestFunction();

        /* define a NbpObserver to receive aggregated events */
        NbpSubscriber<String> NbpObserver = TestHelper.mockNbpSubscriber();

        NbpObservable<String> w = NbpObservable.combineLatest(NbpObservable.just("one", "two"), NbpObservable.just(2, 3, 4), combineLatestFunction);
        w.subscribe(NbpObserver);

        verify(NbpObserver, never()).onError(any(Throwable.class));
        verify(NbpObserver, times(1)).onComplete();
        verify(NbpObserver, times(1)).onNext("two2");
        verify(NbpObserver, times(1)).onNext("two3");
        verify(NbpObserver, times(1)).onNext("two4");
    }

    @Test
    public void testCombineLatest3TypesA() {
        Function3<String, Integer, int[], String> combineLatestFunction = getConcatStringIntegerIntArrayCombineLatestFunction();

        /* define a NbpObserver to receive aggregated events */
        NbpSubscriber<String> NbpObserver = TestHelper.mockNbpSubscriber();

        NbpObservable<String> w = NbpObservable.combineLatest(NbpObservable.just("one", "two"), NbpObservable.just(2), NbpObservable.just(new int[] { 4, 5, 6 }), combineLatestFunction);
        w.subscribe(NbpObserver);

        verify(NbpObserver, never()).onError(any(Throwable.class));
        verify(NbpObserver, times(1)).onComplete();
        verify(NbpObserver, times(1)).onNext("two2[4, 5, 6]");
    }

    @Test
    public void testCombineLatest3TypesB() {
        Function3<String, Integer, int[], String> combineLatestFunction = getConcatStringIntegerIntArrayCombineLatestFunction();

        /* define a NbpObserver to receive aggregated events */
        NbpSubscriber<String> NbpObserver = TestHelper.mockNbpSubscriber();

        NbpObservable<String> w = NbpObservable.combineLatest(NbpObservable.just("one"), NbpObservable.just(2), NbpObservable.just(new int[] { 4, 5, 6 }, new int[] { 7, 8 }), combineLatestFunction);
        w.subscribe(NbpObserver);

        verify(NbpObserver, never()).onError(any(Throwable.class));
        verify(NbpObserver, times(1)).onComplete();
        verify(NbpObserver, times(1)).onNext("one2[4, 5, 6]");
        verify(NbpObserver, times(1)).onNext("one2[7, 8]");
    }

    private Function3<String, String, String, String> getConcat3StringsCombineLatestFunction() {
        Function3<String, String, String, String> combineLatestFunction = (a1, a2, a3) -> {
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
        return combineLatestFunction;
    }

    private BiFunction<String, Integer, String> getConcatStringIntegerCombineLatestFunction() {
        BiFunction<String, Integer, String> combineLatestFunction = (s, i) -> getStringValue(s) + getStringValue(i);
        return combineLatestFunction;
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
        NbpPublishSubject<Integer> a = NbpPublishSubject.create();
        NbpPublishSubject<Integer> b = NbpPublishSubject.create();

        NbpObservable<Integer> source = NbpObservable.combineLatest(a, b, or);

        NbpSubscriber<Object> NbpObserver = TestHelper.mockNbpSubscriber();
        InOrder inOrder = inOrder(NbpObserver);

        source.subscribe(NbpObserver);

        a.onNext(1);

        inOrder.verify(NbpObserver, never()).onNext(any());

        a.onNext(2);

        inOrder.verify(NbpObserver, never()).onNext(any());

        b.onNext(0x10);

        inOrder.verify(NbpObserver, times(1)).onNext(0x12);

        b.onNext(0x20);
        inOrder.verify(NbpObserver, times(1)).onNext(0x22);

        b.onComplete();

        inOrder.verify(NbpObserver, never()).onComplete();

        a.onComplete();

        inOrder.verify(NbpObserver, times(1)).onComplete();

        a.onNext(3);
        b.onNext(0x30);
        a.onComplete();
        b.onComplete();

        inOrder.verifyNoMoreInteractions();
        verify(NbpObserver, never()).onError(any(Throwable.class));
    }

    @Test
    public void combineMultipleObservers() {
        NbpPublishSubject<Integer> a = NbpPublishSubject.create();
        NbpPublishSubject<Integer> b = NbpPublishSubject.create();

        NbpObservable<Integer> source = NbpObservable.combineLatest(a, b, or);

        NbpSubscriber<Object> observer1 = TestHelper.mockNbpSubscriber();

        NbpSubscriber<Object> observer2 = TestHelper.mockNbpSubscriber();

        InOrder inOrder1 = inOrder(observer1);
        InOrder inOrder2 = inOrder(observer2);

        source.subscribe(observer1);
        source.subscribe(observer2);

        a.onNext(1);

        inOrder1.verify(observer1, never()).onNext(any());
        inOrder2.verify(observer2, never()).onNext(any());

        a.onNext(2);

        inOrder1.verify(observer1, never()).onNext(any());
        inOrder2.verify(observer2, never()).onNext(any());

        b.onNext(0x10);

        inOrder1.verify(observer1, times(1)).onNext(0x12);
        inOrder2.verify(observer2, times(1)).onNext(0x12);

        b.onNext(0x20);
        inOrder1.verify(observer1, times(1)).onNext(0x22);
        inOrder2.verify(observer2, times(1)).onNext(0x22);

        b.onComplete();

        inOrder1.verify(observer1, never()).onComplete();
        inOrder2.verify(observer2, never()).onComplete();

        a.onComplete();

        inOrder1.verify(observer1, times(1)).onComplete();
        inOrder2.verify(observer2, times(1)).onComplete();

        a.onNext(3);
        b.onNext(0x30);
        a.onComplete();
        b.onComplete();

        inOrder1.verifyNoMoreInteractions();
        inOrder2.verifyNoMoreInteractions();
        verify(observer1, never()).onError(any(Throwable.class));
        verify(observer2, never()).onError(any(Throwable.class));
    }

    @Test
    public void testFirstNeverProduces() {
        NbpPublishSubject<Integer> a = NbpPublishSubject.create();
        NbpPublishSubject<Integer> b = NbpPublishSubject.create();

        NbpObservable<Integer> source = NbpObservable.combineLatest(a, b, or);

        NbpSubscriber<Object> NbpObserver = TestHelper.mockNbpSubscriber();
        InOrder inOrder = inOrder(NbpObserver);

        source.subscribe(NbpObserver);

        b.onNext(0x10);
        b.onNext(0x20);

        a.onComplete();

        inOrder.verify(NbpObserver, times(1)).onComplete();
        verify(NbpObserver, never()).onNext(any());
        verify(NbpObserver, never()).onError(any(Throwable.class));
    }

    @Test
    public void testSecondNeverProduces() {
        NbpPublishSubject<Integer> a = NbpPublishSubject.create();
        NbpPublishSubject<Integer> b = NbpPublishSubject.create();

        NbpObservable<Integer> source = NbpObservable.combineLatest(a, b, or);

        NbpSubscriber<Object> NbpObserver = TestHelper.mockNbpSubscriber();
        InOrder inOrder = inOrder(NbpObserver);

        source.subscribe(NbpObserver);

        a.onNext(0x1);
        a.onNext(0x2);

        b.onComplete();
        a.onComplete();

        inOrder.verify(NbpObserver, times(1)).onComplete();
        verify(NbpObserver, never()).onNext(any());
        verify(NbpObserver, never()).onError(any(Throwable.class));
    }

    public void test0Sources() {

    }

    @Test
    public void test1ToNSources() {
        int n = 30;
        Function<Object[], List<Object>> func = new Function<Object[], List<Object>>() {

            @Override
            public List<Object> apply(Object[] args) {
                return Arrays.asList(args);
            }
        };
        for (int i = 1; i <= n; i++) {
            System.out.println("test1ToNSources: " + i + " sources");
            List<NbpObservable<Integer>> sources = new ArrayList<>();
            List<Object> values = new ArrayList<>();
            for (int j = 0; j < i; j++) {
                sources.add(NbpObservable.just(j));
                values.add(j);
            }

            NbpObservable<List<Object>> result = NbpObservable.combineLatest(sources, func);

            NbpSubscriber<List<Object>> o = TestHelper.mockNbpSubscriber();

            result.subscribe(o);

            verify(o).onNext(values);
            verify(o).onComplete();
            verify(o, never()).onError(any(Throwable.class));
        }
    }

    @Test(timeout = 5000)
    public void test1ToNSourcesScheduled() throws InterruptedException {
        int n = 10;
        Function<Object[], List<Object>> func = new Function<Object[], List<Object>>() {

            @Override
            public List<Object> apply(Object[] args) {
                return Arrays.asList(args);
            }
        };
        for (int i = 1; i <= n; i++) {
            System.out.println("test1ToNSourcesScheduled: " + i + " sources");
            List<NbpObservable<Integer>> sources = new ArrayList<>();
            List<Object> values = new ArrayList<>();
            for (int j = 0; j < i; j++) {
                sources.add(NbpObservable.just(j).subscribeOn(Schedulers.io()));
                values.add(j);
            }

            NbpObservable<List<Object>> result = NbpObservable.combineLatest(sources, func);

            final NbpSubscriber<List<Object>> o = TestHelper.mockNbpSubscriber();

            final CountDownLatch cdl = new CountDownLatch(1);

            NbpSubscriber<List<Object>> s = new NbpObserver<List<Object>>() {

                @Override
                public void onNext(List<Object> t) {
                    o.onNext(t);
                }

                @Override
                public void onError(Throwable e) {
                    o.onError(e);
                    cdl.countDown();
                }

                @Override
                public void onComplete() {
                    o.onComplete();
                    cdl.countDown();
                }
            };

            result.subscribe(s);

            cdl.await();

            verify(o).onNext(values);
            verify(o).onComplete();
            verify(o, never()).onError(any(Throwable.class));
        }
    }

    @Test
    public void test2SourcesOverload() {
        NbpObservable<Integer> s1 = NbpObservable.just(1);
        NbpObservable<Integer> s2 = NbpObservable.just(2);

        NbpObservable<List<Integer>> result = NbpObservable.combineLatest(s1, s2, 
                (t1, t2) -> Arrays.asList(t1, t2));

        NbpSubscriber<Object> o = TestHelper.mockNbpSubscriber();

        result.subscribe(o);

        verify(o).onNext(Arrays.asList(1, 2));
        verify(o).onComplete();
        verify(o, never()).onError(any(Throwable.class));
    }

    @Test
    public void test3SourcesOverload() {
        NbpObservable<Integer> s1 = NbpObservable.just(1);
        NbpObservable<Integer> s2 = NbpObservable.just(2);
        NbpObservable<Integer> s3 = NbpObservable.just(3);

        NbpObservable<List<Integer>> result = NbpObservable.combineLatest(s1, s2, s3,
                (Function3<Integer, Integer, Integer, List<Integer>>) (t1, t2, t3) -> Arrays.asList(t1, t2, t3));

        NbpSubscriber<Object> o = TestHelper.mockNbpSubscriber();

        result.subscribe(o);

        verify(o).onNext(Arrays.asList(1, 2, 3));
        verify(o).onComplete();
        verify(o, never()).onError(any(Throwable.class));
    }

    @Test
    public void test4SourcesOverload() {
        NbpObservable<Integer> s1 = NbpObservable.just(1);
        NbpObservable<Integer> s2 = NbpObservable.just(2);
        NbpObservable<Integer> s3 = NbpObservable.just(3);
        NbpObservable<Integer> s4 = NbpObservable.just(4);

        NbpObservable<List<Integer>> result = NbpObservable.combineLatest(s1, s2, s3, s4,
                new Function4<Integer, Integer, Integer, Integer, List<Integer>>() {
                    @Override
                    public List<Integer> apply(Integer t1, Integer t2, Integer t3, Integer t4) {
                        return Arrays.asList(t1, t2, t3, t4);
                    }
                });

        NbpSubscriber<Object> o = TestHelper.mockNbpSubscriber();

        result.subscribe(o);

        verify(o).onNext(Arrays.asList(1, 2, 3, 4));
        verify(o).onComplete();
        verify(o, never()).onError(any(Throwable.class));
    }

    @Test
    public void test5SourcesOverload() {
        NbpObservable<Integer> s1 = NbpObservable.just(1);
        NbpObservable<Integer> s2 = NbpObservable.just(2);
        NbpObservable<Integer> s3 = NbpObservable.just(3);
        NbpObservable<Integer> s4 = NbpObservable.just(4);
        NbpObservable<Integer> s5 = NbpObservable.just(5);

        NbpObservable<List<Integer>> result = NbpObservable.combineLatest(s1, s2, s3, s4, s5,
                new Function5<Integer, Integer, Integer, Integer, Integer, List<Integer>>() {
                    @Override
                    public List<Integer> apply(Integer t1, Integer t2, Integer t3, Integer t4, Integer t5) {
                        return Arrays.asList(t1, t2, t3, t4, t5);
                    }
                });

        NbpSubscriber<Object> o = TestHelper.mockNbpSubscriber();

        result.subscribe(o);

        verify(o).onNext(Arrays.asList(1, 2, 3, 4, 5));
        verify(o).onComplete();
        verify(o, never()).onError(any(Throwable.class));
    }

    @Test
    public void test6SourcesOverload() {
        NbpObservable<Integer> s1 = NbpObservable.just(1);
        NbpObservable<Integer> s2 = NbpObservable.just(2);
        NbpObservable<Integer> s3 = NbpObservable.just(3);
        NbpObservable<Integer> s4 = NbpObservable.just(4);
        NbpObservable<Integer> s5 = NbpObservable.just(5);
        NbpObservable<Integer> s6 = NbpObservable.just(6);

        NbpObservable<List<Integer>> result = NbpObservable.combineLatest(s1, s2, s3, s4, s5, s6,
                new Function6<Integer, Integer, Integer, Integer, Integer, Integer, List<Integer>>() {
                    @Override
                    public List<Integer> apply(Integer t1, Integer t2, Integer t3, Integer t4, Integer t5, Integer t6) {
                        return Arrays.asList(t1, t2, t3, t4, t5, t6);
                    }
                });

        NbpSubscriber<Object> o = TestHelper.mockNbpSubscriber();

        result.subscribe(o);

        verify(o).onNext(Arrays.asList(1, 2, 3, 4, 5, 6));
        verify(o).onComplete();
        verify(o, never()).onError(any(Throwable.class));
    }

    @Test
    public void test7SourcesOverload() {
        NbpObservable<Integer> s1 = NbpObservable.just(1);
        NbpObservable<Integer> s2 = NbpObservable.just(2);
        NbpObservable<Integer> s3 = NbpObservable.just(3);
        NbpObservable<Integer> s4 = NbpObservable.just(4);
        NbpObservable<Integer> s5 = NbpObservable.just(5);
        NbpObservable<Integer> s6 = NbpObservable.just(6);
        NbpObservable<Integer> s7 = NbpObservable.just(7);

        NbpObservable<List<Integer>> result = NbpObservable.combineLatest(s1, s2, s3, s4, s5, s6, s7,
                new Function7<Integer, Integer, Integer, Integer, Integer, Integer, Integer, List<Integer>>() {
                    @Override
                    public List<Integer> apply(Integer t1, Integer t2, Integer t3, Integer t4, Integer t5, Integer t6, Integer t7) {
                        return Arrays.asList(t1, t2, t3, t4, t5, t6, t7);
                    }
                });

        NbpSubscriber<Object> o = TestHelper.mockNbpSubscriber();

        result.subscribe(o);

        verify(o).onNext(Arrays.asList(1, 2, 3, 4, 5, 6, 7));
        verify(o).onComplete();
        verify(o, never()).onError(any(Throwable.class));
    }

    @Test
    public void test8SourcesOverload() {
        NbpObservable<Integer> s1 = NbpObservable.just(1);
        NbpObservable<Integer> s2 = NbpObservable.just(2);
        NbpObservable<Integer> s3 = NbpObservable.just(3);
        NbpObservable<Integer> s4 = NbpObservable.just(4);
        NbpObservable<Integer> s5 = NbpObservable.just(5);
        NbpObservable<Integer> s6 = NbpObservable.just(6);
        NbpObservable<Integer> s7 = NbpObservable.just(7);
        NbpObservable<Integer> s8 = NbpObservable.just(8);

        NbpObservable<List<Integer>> result = NbpObservable.combineLatest(s1, s2, s3, s4, s5, s6, s7, s8,
                new Function8<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer, List<Integer>>() {
                    @Override
                    public List<Integer> apply(Integer t1, Integer t2, Integer t3, Integer t4, Integer t5, Integer t6, Integer t7, Integer t8) {
                        return Arrays.asList(t1, t2, t3, t4, t5, t6, t7, t8);
                    }
                });

        NbpSubscriber<Object> o = TestHelper.mockNbpSubscriber();

        result.subscribe(o);

        verify(o).onNext(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8));
        verify(o).onComplete();
        verify(o, never()).onError(any(Throwable.class));
    }

    @Test
    public void test9SourcesOverload() {
        NbpObservable<Integer> s1 = NbpObservable.just(1);
        NbpObservable<Integer> s2 = NbpObservable.just(2);
        NbpObservable<Integer> s3 = NbpObservable.just(3);
        NbpObservable<Integer> s4 = NbpObservable.just(4);
        NbpObservable<Integer> s5 = NbpObservable.just(5);
        NbpObservable<Integer> s6 = NbpObservable.just(6);
        NbpObservable<Integer> s7 = NbpObservable.just(7);
        NbpObservable<Integer> s8 = NbpObservable.just(8);
        NbpObservable<Integer> s9 = NbpObservable.just(9);

        NbpObservable<List<Integer>> result = NbpObservable.combineLatest(s1, s2, s3, s4, s5, s6, s7, s8, s9,
                new Function9<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer, List<Integer>>() {
                    @Override
                    public List<Integer> apply(Integer t1, Integer t2, Integer t3, Integer t4, Integer t5, Integer t6, Integer t7, Integer t8, Integer t9) {
                        return Arrays.asList(t1, t2, t3, t4, t5, t6, t7, t8, t9);
                    }
                });

        NbpSubscriber<Object> o = TestHelper.mockNbpSubscriber();

        result.subscribe(o);

        verify(o).onNext(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9));
        verify(o).onComplete();
        verify(o, never()).onError(any(Throwable.class));
    }

    @Test
    public void testZeroSources() {
        NbpObservable<Object> result = NbpObservable.combineLatest(
                Collections.<NbpObservable<Object>> emptyList(), new Function<Object[], Object>() {

            @Override
            public Object apply(Object[] args) {
                return args;
            }

        });

        NbpSubscriber<Object> o = TestHelper.mockNbpSubscriber();

        result.subscribe(o);

        verify(o).onComplete();
        verify(o, never()).onNext(any());
        verify(o, never()).onError(any(Throwable.class));

    }

    @Test
    public void testWithCombineLatestIssue1717() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicInteger count = new AtomicInteger();
        final int SIZE = 2000;
        NbpObservable<Long> timer = NbpObservable.interval(0, 1, TimeUnit.MILLISECONDS)
                .observeOn(Schedulers.newThread())
                .doOnEach(n -> {
                        //                        System.out.println(n);
                        if (count.incrementAndGet() >= SIZE) {
                            latch.countDown();
                        }
                }).take(SIZE);

        NbpTestSubscriber<Long> ts = new NbpTestSubscriber<>();

        NbpObservable.combineLatest(timer, NbpObservable.<Integer> never(), (t1, t2) -> t1).subscribe(ts);

        if (!latch.await(SIZE + 1000, TimeUnit.MILLISECONDS)) {
            fail("timed out");
        }

        assertEquals(SIZE, count.get());
    }
}