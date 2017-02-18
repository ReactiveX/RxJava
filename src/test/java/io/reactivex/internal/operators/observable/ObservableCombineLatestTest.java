/**
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

package io.reactivex.internal.operators.observable;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;
import org.mockito.*;

import io.reactivex.*;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.*;
import io.reactivex.functions.*;
import io.reactivex.internal.functions.Functions;
import io.reactivex.observers.*;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.schedulers.*;
import io.reactivex.subjects.PublishSubject;

public class ObservableCombineLatestTest {

    @Test
    public void testCombineLatestWithFunctionThatThrowsAnException() {
        Observer<String> w = TestHelper.mockObserver();

        PublishSubject<String> w1 = PublishSubject.create();
        PublishSubject<String> w2 = PublishSubject.create();

        Observable<String> combined = Observable.combineLatest(w1, w2, new BiFunction<String, String, String>() {
            @Override
            public String apply(String v1, String v2) {
                throw new RuntimeException("I don't work.");
            }
        });
        combined.subscribe(w);

        w1.onNext("first value of w1");
        w2.onNext("first value of w2");

        verify(w, never()).onNext(anyString());
        verify(w, never()).onComplete();
        verify(w, times(1)).onError(Mockito.<RuntimeException> any());
    }

    @Test
    public void testCombineLatestDifferentLengthObservableSequences1() {
        Observer<String> w = TestHelper.mockObserver();

        PublishSubject<String> w1 = PublishSubject.create();
        PublishSubject<String> w2 = PublishSubject.create();
        PublishSubject<String> w3 = PublishSubject.create();

        Observable<String> combineLatestW = Observable.combineLatest(w1, w2, w3,
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
    public void testCombineLatestDifferentLengthObservableSequences2() {
        Observer<String> w = TestHelper.mockObserver();

        PublishSubject<String> w1 = PublishSubject.create();
        PublishSubject<String> w2 = PublishSubject.create();
        PublishSubject<String> w3 = PublishSubject.create();

        Observable<String> combineLatestW = Observable.combineLatest(w1, w2, w3, getConcat3StringsCombineLatestFunction());
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
    public void testCombineLatestWithInterleavingSequences() {
        Observer<String> w = TestHelper.mockObserver();

        PublishSubject<String> w1 = PublishSubject.create();
        PublishSubject<String> w2 = PublishSubject.create();
        PublishSubject<String> w3 = PublishSubject.create();

        Observable<String> combineLatestW = Observable.combineLatest(w1, w2, w3, getConcat3StringsCombineLatestFunction());
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
    public void testCombineLatest2Types() {
        BiFunction<String, Integer, String> combineLatestFunction = getConcatStringIntegerCombineLatestFunction();

        /* define an Observer to receive aggregated events */
        Observer<String> observer = TestHelper.mockObserver();

        Observable<String> w = Observable.combineLatest(Observable.just("one", "two"), Observable.just(2, 3, 4), combineLatestFunction);
        w.subscribe(observer);

        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onComplete();
        verify(observer, times(1)).onNext("two2");
        verify(observer, times(1)).onNext("two3");
        verify(observer, times(1)).onNext("two4");
    }

    @Test
    public void testCombineLatest3TypesA() {
        Function3<String, Integer, int[], String> combineLatestFunction = getConcatStringIntegerIntArrayCombineLatestFunction();

        /* define an Observer to receive aggregated events */
        Observer<String> observer = TestHelper.mockObserver();

        Observable<String> w = Observable.combineLatest(Observable.just("one", "two"), Observable.just(2), Observable.just(new int[] { 4, 5, 6 }), combineLatestFunction);
        w.subscribe(observer);

        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onComplete();
        verify(observer, times(1)).onNext("two2[4, 5, 6]");
    }

    @Test
    public void testCombineLatest3TypesB() {
        Function3<String, Integer, int[], String> combineLatestFunction = getConcatStringIntegerIntArrayCombineLatestFunction();

        /* define an Observer to receive aggregated events */
        Observer<String> observer = TestHelper.mockObserver();

        Observable<String> w = Observable.combineLatest(Observable.just("one"), Observable.just(2), Observable.just(new int[] { 4, 5, 6 }, new int[] { 7, 8 }), combineLatestFunction);
        w.subscribe(observer);

        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onComplete();
        verify(observer, times(1)).onNext("one2[4, 5, 6]");
        verify(observer, times(1)).onNext("one2[7, 8]");
    }

    private Function3<String, String, String, String> getConcat3StringsCombineLatestFunction() {
        Function3<String, String, String, String> combineLatestFunction = new Function3<String, String, String, String>() {
            @Override
            public String apply(String a1, String a2, String a3) {
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
            }
        };
        return combineLatestFunction;
    }

    private BiFunction<String, Integer, String> getConcatStringIntegerCombineLatestFunction() {
        BiFunction<String, Integer, String> combineLatestFunction = new BiFunction<String, Integer, String>() {
            @Override
            public String apply(String s, Integer i) {
                return getStringValue(s) + getStringValue(i);
            }
        };
        return combineLatestFunction;
    }

    private Function3<String, Integer, int[], String> getConcatStringIntegerIntArrayCombineLatestFunction() {
        return new Function3<String, Integer, int[], String>() {
            @Override
            public String apply(String s, Integer i, int[] iArray) {
                return getStringValue(s) + getStringValue(i) + getStringValue(iArray);
            }
        };
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

    BiFunction<Integer, Integer, Integer> or = new BiFunction<Integer, Integer, Integer>() {
        @Override
        public Integer apply(Integer t1, Integer t2) {
            return t1 | t2;
        }
    };

    @Test
    public void combineSimple() {
        PublishSubject<Integer> a = PublishSubject.create();
        PublishSubject<Integer> b = PublishSubject.create();

        Observable<Integer> source = Observable.combineLatest(a, b, or);

        Observer<Object> observer = TestHelper.mockObserver();
        InOrder inOrder = inOrder(observer);

        source.subscribe(observer);

        a.onNext(1);

        inOrder.verify(observer, never()).onNext(any());

        a.onNext(2);

        inOrder.verify(observer, never()).onNext(any());

        b.onNext(0x10);

        inOrder.verify(observer, times(1)).onNext(0x12);

        b.onNext(0x20);
        inOrder.verify(observer, times(1)).onNext(0x22);

        b.onComplete();

        inOrder.verify(observer, never()).onComplete();

        a.onComplete();

        inOrder.verify(observer, times(1)).onComplete();

        a.onNext(3);
        b.onNext(0x30);
        a.onComplete();
        b.onComplete();

        inOrder.verifyNoMoreInteractions();
        verify(observer, never()).onError(any(Throwable.class));
    }

    @Test
    public void combineMultipleObservers() {
        PublishSubject<Integer> a = PublishSubject.create();
        PublishSubject<Integer> b = PublishSubject.create();

        Observable<Integer> source = Observable.combineLatest(a, b, or);

        Observer<Object> observer1 = TestHelper.mockObserver();

        Observer<Object> observer2 = TestHelper.mockObserver();

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
        PublishSubject<Integer> a = PublishSubject.create();
        PublishSubject<Integer> b = PublishSubject.create();

        Observable<Integer> source = Observable.combineLatest(a, b, or);

        Observer<Object> observer = TestHelper.mockObserver();
        InOrder inOrder = inOrder(observer);

        source.subscribe(observer);

        b.onNext(0x10);
        b.onNext(0x20);

        a.onComplete();

        inOrder.verify(observer, times(1)).onComplete();
        verify(observer, never()).onNext(any());
        verify(observer, never()).onError(any(Throwable.class));
    }

    @Test
    public void testSecondNeverProduces() {
        PublishSubject<Integer> a = PublishSubject.create();
        PublishSubject<Integer> b = PublishSubject.create();

        Observable<Integer> source = Observable.combineLatest(a, b, or);

        Observer<Object> observer = TestHelper.mockObserver();
        InOrder inOrder = inOrder(observer);

        source.subscribe(observer);

        a.onNext(0x1);
        a.onNext(0x2);

        b.onComplete();
        a.onComplete();

        inOrder.verify(observer, times(1)).onComplete();
        verify(observer, never()).onNext(any());
        verify(observer, never()).onError(any(Throwable.class));
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
            List<Observable<Integer>> sources = new ArrayList<Observable<Integer>>();
            List<Object> values = new ArrayList<Object>();
            for (int j = 0; j < i; j++) {
                sources.add(Observable.just(j));
                values.add(j);
            }

            Observable<List<Object>> result = Observable.combineLatest(sources, func);

            Observer<List<Object>> o = TestHelper.mockObserver();

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
            List<Observable<Integer>> sources = new ArrayList<Observable<Integer>>();
            List<Object> values = new ArrayList<Object>();
            for (int j = 0; j < i; j++) {
                sources.add(Observable.just(j).subscribeOn(Schedulers.io()));
                values.add(j);
            }

            Observable<List<Object>> result = Observable.combineLatest(sources, func);

            final Observer<List<Object>> o = TestHelper.mockObserver();

            final CountDownLatch cdl = new CountDownLatch(1);

            Observer<List<Object>> s = new DefaultObserver<List<Object>>() {

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
        Observable<Integer> s1 = Observable.just(1);
        Observable<Integer> s2 = Observable.just(2);

        Observable<List<Integer>> result = Observable.combineLatest(s1, s2,
                new BiFunction<Integer, Integer, List<Integer>>() {
                    @Override
                    public List<Integer> apply(Integer t1, Integer t2) {
                        return Arrays.asList(t1, t2);
                    }
                });

        Observer<Object> o = TestHelper.mockObserver();

        result.subscribe(o);

        verify(o).onNext(Arrays.asList(1, 2));
        verify(o).onComplete();
        verify(o, never()).onError(any(Throwable.class));
    }

    @Test
    public void test3SourcesOverload() {
        Observable<Integer> s1 = Observable.just(1);
        Observable<Integer> s2 = Observable.just(2);
        Observable<Integer> s3 = Observable.just(3);

        Observable<List<Integer>> result = Observable.combineLatest(s1, s2, s3,
                new Function3<Integer, Integer, Integer, List<Integer>>() {
            @Override
            public List<Integer> apply(Integer t1, Integer t2, Integer t3) {
                return Arrays.asList(t1, t2, t3);
            }
        });

        Observer<Object> o = TestHelper.mockObserver();

        result.subscribe(o);

        verify(o).onNext(Arrays.asList(1, 2, 3));
        verify(o).onComplete();
        verify(o, never()).onError(any(Throwable.class));
    }

    @Test
    public void test4SourcesOverload() {
        Observable<Integer> s1 = Observable.just(1);
        Observable<Integer> s2 = Observable.just(2);
        Observable<Integer> s3 = Observable.just(3);
        Observable<Integer> s4 = Observable.just(4);

        Observable<List<Integer>> result = Observable.combineLatest(s1, s2, s3, s4,
                new Function4<Integer, Integer, Integer, Integer, List<Integer>>() {
                    @Override
                    public List<Integer> apply(Integer t1, Integer t2, Integer t3, Integer t4) {
                        return Arrays.asList(t1, t2, t3, t4);
                    }
                });

        Observer<Object> o = TestHelper.mockObserver();

        result.subscribe(o);

        verify(o).onNext(Arrays.asList(1, 2, 3, 4));
        verify(o).onComplete();
        verify(o, never()).onError(any(Throwable.class));
    }

    @Test
    public void test5SourcesOverload() {
        Observable<Integer> s1 = Observable.just(1);
        Observable<Integer> s2 = Observable.just(2);
        Observable<Integer> s3 = Observable.just(3);
        Observable<Integer> s4 = Observable.just(4);
        Observable<Integer> s5 = Observable.just(5);

        Observable<List<Integer>> result = Observable.combineLatest(s1, s2, s3, s4, s5,
                new Function5<Integer, Integer, Integer, Integer, Integer, List<Integer>>() {
                    @Override
                    public List<Integer> apply(Integer t1, Integer t2, Integer t3, Integer t4, Integer t5) {
                        return Arrays.asList(t1, t2, t3, t4, t5);
                    }
                });

        Observer<Object> o = TestHelper.mockObserver();

        result.subscribe(o);

        verify(o).onNext(Arrays.asList(1, 2, 3, 4, 5));
        verify(o).onComplete();
        verify(o, never()).onError(any(Throwable.class));
    }

    @Test
    public void test6SourcesOverload() {
        Observable<Integer> s1 = Observable.just(1);
        Observable<Integer> s2 = Observable.just(2);
        Observable<Integer> s3 = Observable.just(3);
        Observable<Integer> s4 = Observable.just(4);
        Observable<Integer> s5 = Observable.just(5);
        Observable<Integer> s6 = Observable.just(6);

        Observable<List<Integer>> result = Observable.combineLatest(s1, s2, s3, s4, s5, s6,
                new Function6<Integer, Integer, Integer, Integer, Integer, Integer, List<Integer>>() {
                    @Override
                    public List<Integer> apply(Integer t1, Integer t2, Integer t3, Integer t4, Integer t5, Integer t6) {
                        return Arrays.asList(t1, t2, t3, t4, t5, t6);
                    }
                });

        Observer<Object> o = TestHelper.mockObserver();

        result.subscribe(o);

        verify(o).onNext(Arrays.asList(1, 2, 3, 4, 5, 6));
        verify(o).onComplete();
        verify(o, never()).onError(any(Throwable.class));
    }

    @Test
    public void test7SourcesOverload() {
        Observable<Integer> s1 = Observable.just(1);
        Observable<Integer> s2 = Observable.just(2);
        Observable<Integer> s3 = Observable.just(3);
        Observable<Integer> s4 = Observable.just(4);
        Observable<Integer> s5 = Observable.just(5);
        Observable<Integer> s6 = Observable.just(6);
        Observable<Integer> s7 = Observable.just(7);

        Observable<List<Integer>> result = Observable.combineLatest(s1, s2, s3, s4, s5, s6, s7,
                new Function7<Integer, Integer, Integer, Integer, Integer, Integer, Integer, List<Integer>>() {
                    @Override
                    public List<Integer> apply(Integer t1, Integer t2, Integer t3, Integer t4, Integer t5, Integer t6, Integer t7) {
                        return Arrays.asList(t1, t2, t3, t4, t5, t6, t7);
                    }
                });

        Observer<Object> o = TestHelper.mockObserver();

        result.subscribe(o);

        verify(o).onNext(Arrays.asList(1, 2, 3, 4, 5, 6, 7));
        verify(o).onComplete();
        verify(o, never()).onError(any(Throwable.class));
    }

    @Test
    public void test8SourcesOverload() {
        Observable<Integer> s1 = Observable.just(1);
        Observable<Integer> s2 = Observable.just(2);
        Observable<Integer> s3 = Observable.just(3);
        Observable<Integer> s4 = Observable.just(4);
        Observable<Integer> s5 = Observable.just(5);
        Observable<Integer> s6 = Observable.just(6);
        Observable<Integer> s7 = Observable.just(7);
        Observable<Integer> s8 = Observable.just(8);

        Observable<List<Integer>> result = Observable.combineLatest(s1, s2, s3, s4, s5, s6, s7, s8,
                new Function8<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer, List<Integer>>() {
                    @Override
                    public List<Integer> apply(Integer t1, Integer t2, Integer t3, Integer t4, Integer t5, Integer t6, Integer t7, Integer t8) {
                        return Arrays.asList(t1, t2, t3, t4, t5, t6, t7, t8);
                    }
                });

        Observer<Object> o = TestHelper.mockObserver();

        result.subscribe(o);

        verify(o).onNext(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8));
        verify(o).onComplete();
        verify(o, never()).onError(any(Throwable.class));
    }

    @Test
    public void test9SourcesOverload() {
        Observable<Integer> s1 = Observable.just(1);
        Observable<Integer> s2 = Observable.just(2);
        Observable<Integer> s3 = Observable.just(3);
        Observable<Integer> s4 = Observable.just(4);
        Observable<Integer> s5 = Observable.just(5);
        Observable<Integer> s6 = Observable.just(6);
        Observable<Integer> s7 = Observable.just(7);
        Observable<Integer> s8 = Observable.just(8);
        Observable<Integer> s9 = Observable.just(9);

        Observable<List<Integer>> result = Observable.combineLatest(s1, s2, s3, s4, s5, s6, s7, s8, s9,
                new Function9<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer, List<Integer>>() {
                    @Override
                    public List<Integer> apply(Integer t1, Integer t2, Integer t3, Integer t4, Integer t5, Integer t6, Integer t7, Integer t8, Integer t9) {
                        return Arrays.asList(t1, t2, t3, t4, t5, t6, t7, t8, t9);
                    }
                });

        Observer<Object> o = TestHelper.mockObserver();

        result.subscribe(o);

        verify(o).onNext(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9));
        verify(o).onComplete();
        verify(o, never()).onError(any(Throwable.class));
    }

    @Test
    public void testZeroSources() {
        Observable<Object> result = Observable.combineLatest(
                Collections.<Observable<Object>> emptyList(), new Function<Object[], Object>() {

            @Override
            public Object apply(Object[] args) {
                return args;
            }

        });

        Observer<Object> o = TestHelper.mockObserver();

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
        Observable<Long> timer = Observable.interval(0, 1, TimeUnit.MILLISECONDS)
                .observeOn(Schedulers.newThread())
                .doOnEach(new Consumer<Notification<Long>>() {
                    @Override
                    public void accept(Notification<Long> n) {
                            //                        System.out.println(n);
                            if (count.incrementAndGet() >= SIZE) {
                                latch.countDown();
                            }
                    }
                }).take(SIZE);

        TestObserver<Long> ts = new TestObserver<Long>();

        Observable.combineLatest(timer, Observable.<Integer> never(), new BiFunction<Long, Integer, Long>() {
            @Override
            public Long apply(Long t1, Integer t2) {
                return t1;
            }
        }).subscribe(ts);

        if (!latch.await(SIZE + 1000, TimeUnit.MILLISECONDS)) {
            fail("timed out");
        }

        assertEquals(SIZE, count.get());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void combineLatestArrayOfSources() {

        Observable.combineLatest(new ObservableSource[] {
                Observable.just(1), Observable.just(2)
        }, new Function<Object[], Object>() {
            @Override
            public Object apply(Object[] a) throws Exception {
                return Arrays.toString(a);
            }
        })
        .test()
        .assertResult("[1, 2]");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void combineLatestDelayErrorArrayOfSources() {

        Observable.combineLatestDelayError(new ObservableSource[] {
                Observable.just(1), Observable.just(2)
        }, new Function<Object[], Object>() {
            @Override
            public Object apply(Object[] a) throws Exception {
                return Arrays.toString(a);
            }
        })
        .test()
        .assertResult("[1, 2]");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void combineLatestDelayErrorArrayOfSourcesWithError() {

        Observable.combineLatestDelayError(new ObservableSource[] {
                Observable.just(1), Observable.just(2).concatWith(Observable.<Integer>error(new TestException()))
        }, new Function<Object[], Object>() {
            @Override
            public Object apply(Object[] a) throws Exception {
                return Arrays.toString(a);
            }
        })
        .test()
        .assertFailure(TestException.class, "[1, 2]");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void combineLatestDelayErrorIterableOfSources() {

        Observable.combineLatestDelayError(Arrays.asList(
                Observable.just(1), Observable.just(2)
        ), new Function<Object[], Object>() {
            @Override
            public Object apply(Object[] a) throws Exception {
                return Arrays.toString(a);
            }
        })
        .test()
        .assertResult("[1, 2]");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void combineLatestDelayErrorIterableOfSourcesWithError() {

        Observable.combineLatestDelayError(Arrays.asList(
                Observable.just(1), Observable.just(2).concatWith(Observable.<Integer>error(new TestException()))
        ), new Function<Object[], Object>() {
            @Override
            public Object apply(Object[] a) throws Exception {
                return Arrays.toString(a);
            }
        })
        .test()
        .assertFailure(TestException.class, "[1, 2]");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void combineLatestEmpty() {
        assertSame(Observable.empty(), Observable.combineLatest(new ObservableSource[0], Functions.<Object[]>identity(), 16));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void combineLatestDelayErrorEmpty() {
        assertSame(Observable.empty(), Observable.combineLatestDelayError(new ObservableSource[0], Functions.<Object[]>identity(), 16));
    }

    @Test
    public void disposed() {
        TestHelper.checkDisposed(Observable.combineLatest(Observable.never(), Observable.never(), new BiFunction<Object, Object, Object>() {
            @Override
            public Object apply(Object a, Object b) throws Exception {
                return a;
            }
        }));
    }

    @Test
    public void cancelWhileSubscribing() {
        final TestObserver<Object> to = new TestObserver<Object>();

        Observable.combineLatest(
                Observable.just(1)
                .doOnNext(new Consumer<Integer>() {
                    @Override
                    public void accept(Integer v) throws Exception {
                        to.cancel();
                    }
                }),
                Observable.never(),
                new BiFunction<Object, Object, Object>() {
            @Override
            public Object apply(Object a, Object b) throws Exception {
                return a;
            }
        })
        .subscribe(to);
    }

    @Test
    public void combineAsync() {
        Observable<Integer> source = Observable.range(1, 1000).subscribeOn(Schedulers.computation());

        Observable.combineLatest(source, source, new BiFunction<Object, Object, Object>() {
            @Override
            public Object apply(Object a, Object b) throws Exception {
                return a;
            }
        })
        .take(500)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertNoErrors()
        .assertComplete();
    }

    @Test
    public void error() {
        Observable.combineLatest(Observable.never(), Observable.error(new TestException()), new BiFunction<Object, Object, Object>() {
            @Override
            public Object apply(Object a, Object b) throws Exception {
                return a;
            }
        })
        .test()
        .assertFailure(TestException.class);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void errorDelayed() {
        Observable.combineLatestDelayError(
                new Function<Object[], Object>() {
                    @Override
                    public Object apply(Object[] a) throws Exception {
                        return a;
                    }
                },
                128,
                Observable.error(new TestException()),
                Observable.just(1)
        )
        .test()
        .assertFailure(TestException.class);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void errorDelayed2() {
        Observable.combineLatestDelayError(
                new Function<Object[], Object>() {
                    @Override
                    public Object apply(Object[] a) throws Exception {
                        return a;
                    }
                },
                128,
                Observable.error(new TestException()).startWith(1),
                Observable.empty()
        )
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void onErrorRace() {
        for (int i = 0; i < 500; i++) {
            List<Throwable> errors = TestHelper.trackPluginErrors();
            try {
                final PublishSubject<Integer> ps1 = PublishSubject.create();
                final PublishSubject<Integer> ps2 = PublishSubject.create();

                TestObserver<Integer> to = Observable.combineLatest(ps1, ps2, new BiFunction<Integer, Integer, Integer>() {
                    @Override
                    public Integer apply(Integer a, Integer b) throws Exception {
                        return a;
                    }
                }).test();

                final TestException ex1 = new TestException();
                final TestException ex2 = new TestException();

                Runnable r1 = new Runnable() {
                    @Override
                    public void run() {
                        ps1.onError(ex1);
                    }
                };
                Runnable r2 = new Runnable() {
                    @Override
                    public void run() {
                        ps2.onError(ex2);
                    }
                };

                TestHelper.race(r1, r2);

                if (to.errorCount() != 0) {
                    if (to.errors().get(0) instanceof CompositeException) {
                        to.assertSubscribed()
                        .assertNotComplete()
                        .assertNoValues();

                        for (Throwable e : TestHelper.errorList(to)) {
                            assertTrue(e.toString(), e instanceof TestException);
                        }

                    } else {
                        to.assertFailure(TestException.class);
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
    public void dontSubscribeIfDone() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            final int[] count = { 0 };

            Observable.combineLatest(Observable.empty(),
                    Observable.error(new TestException())
                    .doOnSubscribe(new Consumer<Disposable>() {
                        @Override
                        public void accept(Disposable d) throws Exception {
                            count[0]++;
                        }
                    }),
                    new BiFunction<Object, Object, Object>() {
                        @Override
                        public Object apply(Object a, Object b) throws Exception {
                            return 0;
                        }
                    })
            .test()
            .assertResult();

            assertEquals(0, count[0]);

            assertTrue(errors.toString(), errors.isEmpty());
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void dontSubscribeIfDone2() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            final int[] count = { 0 };

            Observable.combineLatestDelayError(
                    Arrays.asList(Observable.empty(),
                        Observable.error(new TestException())
                        .doOnSubscribe(new Consumer<Disposable>() {
                            @Override
                            public void accept(Disposable d) throws Exception {
                                count[0]++;
                            }
                        })
                    ),
                    new Function<Object[], Object>() {
                        @Override
                        public Object apply(Object[] a) throws Exception {
                            return 0;
                        }
                    })
            .test()
            .assertResult();

            assertEquals(0, count[0]);

            assertTrue(errors.toString(), errors.isEmpty());
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void combine2Observable2Errors() throws Exception {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            TestObserver<Object> testObserver = TestObserver.create();

            TestScheduler testScheduler = new TestScheduler();

            Observable<Integer> emptyObservable = Observable.timer(10, TimeUnit.MILLISECONDS, testScheduler)
                    .flatMap(new Function<Long, ObservableSource<Integer>>() {
                        @Override
                        public ObservableSource<Integer> apply(Long aLong) throws Exception {
                            return Observable.error(new Exception());
                        }
                    });
            Observable<Object> errorObservable = Observable.timer(100, TimeUnit.MILLISECONDS, testScheduler).map(new Function<Long, Object>() {
                @Override
                public Object apply(Long aLong) throws Exception {
                    throw new Exception();
                }
            });

            Observable.combineLatestDelayError(
                    Arrays.asList(
                            emptyObservable
                                    .doOnEach(new Consumer<Notification<Integer>>() {
                                        @Override
                                        public void accept(Notification<Integer> integerNotification) throws Exception {
                                            System.out.println("emptyObservable: " + integerNotification);
                                        }
                                    })
                                    .doFinally(new Action() {
                                        @Override
                                        public void run() throws Exception {
                                            System.out.println("emptyObservable: doFinally");
                                        }
                                    }),
                            errorObservable
                                    .doOnEach(new Consumer<Notification<Object>>() {
                                        @Override
                                        public void accept(Notification<Object> integerNotification) throws Exception {
                                            System.out.println("errorObservable: " + integerNotification);
                                        }
                                    })
                                    .doFinally(new Action() {
                                        @Override
                                        public void run() throws Exception {
                                            System.out.println("errorObservable: doFinally");
                                        }
                                    })),
                    new Function<Object[], Object>() {
                        @Override
                        public Object apply(Object[] objects) throws Exception {
                            return 0;
                        }
                    }
            )
                    .doOnEach(new Consumer<Notification<Object>>() {
                        @Override
                        public void accept(Notification<Object> integerNotification) throws Exception {
                            System.out.println("combineLatestDelayError: " + integerNotification);
                        }
                    })
                    .doFinally(new Action() {
                        @Override
                        public void run() throws Exception {
                            System.out.println("combineLatestDelayError: doFinally");
                        }
                    })
                    .subscribe(testObserver);

            testScheduler.advanceTimeBy(100, TimeUnit.MILLISECONDS);

            testObserver.awaitTerminalEvent();

            assertTrue(errors.toString(), errors.isEmpty());
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void eagerDispose() {
        final PublishSubject<Integer> ps1 = PublishSubject.create();
        final PublishSubject<Integer> ps2 = PublishSubject.create();

        TestObserver<Integer> ts = new TestObserver<Integer>() {
            @Override
            public void onNext(Integer t) {
                super.onNext(t);
                cancel();
                if (ps1.hasObservers()) {
                    onError(new IllegalStateException("ps1 not disposed"));
                } else
                if (ps2.hasObservers()) {
                    onError(new IllegalStateException("ps2 not disposed"));
                } else {
                    onComplete();
                }
            }
        };

        Observable.combineLatest(ps1, ps2, new BiFunction<Integer, Integer, Integer>() {
            @Override
            public Integer apply(Integer t1, Integer t2) throws Exception {
                return t1 + t2;
            }
        })
        .subscribe(ts);

        ps1.onNext(1);
        ps2.onNext(2);
        ts.assertResult(3);
    }
}
