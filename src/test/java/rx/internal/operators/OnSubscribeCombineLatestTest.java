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

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.junit.Test;
import org.mockito.*;

import rx.*;
import rx.Observable;
import rx.Observer;
import rx.exceptions.*;
import rx.functions.*;
import rx.internal.util.RxRingBuffer;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;

public class OnSubscribeCombineLatestTest {

    @Test
    public void testCombineLatestWithFunctionThatThrowsAnException() {
        @SuppressWarnings("unchecked")
        // mock calls don't do generics
        Observer<String> w = mock(Observer.class);

        PublishSubject<String> w1 = PublishSubject.create();
        PublishSubject<String> w2 = PublishSubject.create();

        Observable<String> combined = Observable.combineLatest(w1, w2, new Func2<String, String, String>() {
            @Override
            public String call(String v1, String v2) {
                throw new RuntimeException("I don't work.");
            }
        });
        combined.subscribe(w);

        w1.onNext("first value of w1");
        w2.onNext("first value of w2");

        verify(w, never()).onNext(anyString());
        verify(w, never()).onCompleted();
        verify(w, times(1)).onError(Matchers.<RuntimeException> any());
    }

    @Test
    public void testCombineLatestDifferentLengthObservableSequences1() {
        @SuppressWarnings("unchecked")
        // mock calls don't do generics
        Observer<String> w = mock(Observer.class);

        PublishSubject<String> w1 = PublishSubject.create();
        PublishSubject<String> w2 = PublishSubject.create();
        PublishSubject<String> w3 = PublishSubject.create();

        Observable<String> combineLatestW = Observable.combineLatest(w1, w2, w3, getConcat3StringsCombineLatestFunction());
        combineLatestW.subscribe(w);

        /* simulate sending data */
        // once for w1
        w1.onNext("1a");
        w2.onNext("2a");
        w3.onNext("3a");
        w1.onCompleted();
        // twice for w2
        w2.onNext("2b");
        w2.onCompleted();
        // 4 times for w3
        w3.onNext("3b");
        w3.onNext("3c");
        w3.onNext("3d");
        w3.onCompleted();

        /* we should have been called 4 times on the Observer */
        InOrder inOrder = inOrder(w);
        inOrder.verify(w).onNext("1a2a3a");
        inOrder.verify(w).onNext("1a2b3a");
        inOrder.verify(w).onNext("1a2b3b");
        inOrder.verify(w).onNext("1a2b3c");
        inOrder.verify(w).onNext("1a2b3d");
        inOrder.verify(w, never()).onNext(anyString());
        inOrder.verify(w, times(1)).onCompleted();
    }

    @Test
    public void testCombineLatestDifferentLengthObservableSequences2() {
        @SuppressWarnings("unchecked")
        Observer<String> w = mock(Observer.class);

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
        w1.onCompleted();
        // twice for w2
        w2.onNext("2a");
        w2.onNext("2b");
        w2.onCompleted();
        // 1 times for w3
        w3.onNext("3a");
        w3.onCompleted();

        /* we should have been called 1 time only on the Observer since we only combine the "latest" we don't go back and loop through others once completed */
        InOrder inOrder = inOrder(w);
        inOrder.verify(w, times(1)).onNext("1d2b3a");
        inOrder.verify(w, never()).onNext(anyString());

        inOrder.verify(w, times(1)).onCompleted();

    }

    @Test
    public void testCombineLatestWithInterleavingSequences() {
        @SuppressWarnings("unchecked")
        Observer<String> w = mock(Observer.class);

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

        w1.onCompleted();
        w2.onCompleted();
        w3.onCompleted();

        /* we should have been called 5 times on the Observer */
        InOrder inOrder = inOrder(w);
        inOrder.verify(w).onNext("1a2b3a");
        inOrder.verify(w).onNext("1b2b3a");
        inOrder.verify(w).onNext("1b2c3a");
        inOrder.verify(w).onNext("1b2d3a");
        inOrder.verify(w).onNext("1b2d3b");

        inOrder.verify(w, never()).onNext(anyString());
        inOrder.verify(w, times(1)).onCompleted();
    }

    @SuppressWarnings("unchecked")
    /* mock calls don't do generics */
    @Test
    public void testCombineLatest2Types() {
        Func2<String, Integer, String> combineLatestFunction = getConcatStringIntegerCombineLatestFunction();

        /* define a Observer to receive aggregated events */
        Observer<String> observer = mock(Observer.class);

        Observable<String> w = Observable.combineLatest(Observable.just("one", "two"), Observable.just(2, 3, 4), combineLatestFunction);
        w.subscribe(observer);

        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onCompleted();
        verify(observer, times(1)).onNext("two2");
        verify(observer, times(1)).onNext("two3");
        verify(observer, times(1)).onNext("two4");
    }

    @SuppressWarnings("unchecked")
    /* mock calls don't do generics */
    @Test
    public void testCombineLatest3TypesA() {
        Func3<String, Integer, int[], String> combineLatestFunction = getConcatStringIntegerIntArrayCombineLatestFunction();

        /* define a Observer to receive aggregated events */
        Observer<String> observer = mock(Observer.class);

        Observable<String> w = Observable.combineLatest(Observable.just("one", "two"), Observable.just(2), Observable.just(new int[] { 4, 5, 6 }), combineLatestFunction);
        w.subscribe(observer);

        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onCompleted();
        verify(observer, times(1)).onNext("two2[4, 5, 6]");
    }

    @SuppressWarnings("unchecked")
    /* mock calls don't do generics */
    @Test
    public void testCombineLatest3TypesB() {
        Func3<String, Integer, int[], String> combineLatestFunction = getConcatStringIntegerIntArrayCombineLatestFunction();

        /* define a Observer to receive aggregated events */
        Observer<String> observer = mock(Observer.class);

        Observable<String> w = Observable.combineLatest(Observable.just("one"), Observable.just(2), Observable.just(new int[] { 4, 5, 6 }, new int[] { 7, 8 }), combineLatestFunction);
        w.subscribe(observer);

        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onCompleted();
        verify(observer, times(1)).onNext("one2[4, 5, 6]");
        verify(observer, times(1)).onNext("one2[7, 8]");
    }

    private Func3<String, String, String, String> getConcat3StringsCombineLatestFunction() {
        return new Func3<String, String, String, String>() {

            @Override
            public String call(String a1, String a2, String a3) {
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
    }

    private Func2<String, Integer, String> getConcatStringIntegerCombineLatestFunction() {
        return new Func2<String, Integer, String>() {

            @Override
            public String call(String s, Integer i) {
                return getStringValue(s) + getStringValue(i);
            }

        };
    }

    private Func3<String, Integer, int[], String> getConcatStringIntegerIntArrayCombineLatestFunction() {
        return new Func3<String, Integer, int[], String>() {

            @Override
            public String call(String s, Integer i, int[] iArray) {
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

    Func2<Integer, Integer, Integer> or = new Func2<Integer, Integer, Integer>() {
        @Override
        public Integer call(Integer t1, Integer t2) {
            return t1 | t2;
        }
    };

    @Test
    public void combineSimple() {
        PublishSubject<Integer> a = PublishSubject.create();
        PublishSubject<Integer> b = PublishSubject.create();

        Observable<Integer> source = Observable.combineLatest(a, b, or);

        @SuppressWarnings("unchecked")
        Observer<Object> observer = mock(Observer.class);
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

        b.onCompleted();

        inOrder.verify(observer, never()).onCompleted();

        a.onCompleted();

        inOrder.verify(observer, times(1)).onCompleted();

        a.onNext(3);
        b.onNext(0x30);
        a.onCompleted();
        b.onCompleted();

        inOrder.verifyNoMoreInteractions();
        verify(observer, never()).onError(any(Throwable.class));
    }

    @Test
    public void combineMultipleObservers() {
        PublishSubject<Integer> a = PublishSubject.create();
        PublishSubject<Integer> b = PublishSubject.create();

        Observable<Integer> source = Observable.combineLatest(a, b, or);

        @SuppressWarnings("unchecked")
        Observer<Object> observer1 = mock(Observer.class);
        @SuppressWarnings("unchecked")
        Observer<Object> observer2 = mock(Observer.class);

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

        b.onCompleted();

        inOrder1.verify(observer1, never()).onCompleted();
        inOrder2.verify(observer2, never()).onCompleted();

        a.onCompleted();

        inOrder1.verify(observer1, times(1)).onCompleted();
        inOrder2.verify(observer2, times(1)).onCompleted();

        a.onNext(3);
        b.onNext(0x30);
        a.onCompleted();
        b.onCompleted();

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

        @SuppressWarnings("unchecked")
        Observer<Object> observer = mock(Observer.class);
        InOrder inOrder = inOrder(observer);

        source.subscribe(observer);

        b.onNext(0x10);
        b.onNext(0x20);

        a.onCompleted();

        inOrder.verify(observer, times(1)).onCompleted();
        verify(observer, never()).onNext(any());
        verify(observer, never()).onError(any(Throwable.class));
    }

    @Test
    public void testSecondNeverProduces() {
        PublishSubject<Integer> a = PublishSubject.create();
        PublishSubject<Integer> b = PublishSubject.create();

        Observable<Integer> source = Observable.combineLatest(a, b, or);

        @SuppressWarnings("unchecked")
        Observer<Object> observer = mock(Observer.class);
        InOrder inOrder = inOrder(observer);

        source.subscribe(observer);

        a.onNext(0x1);
        a.onNext(0x2);

        b.onCompleted();
        a.onCompleted();

        inOrder.verify(observer, times(1)).onCompleted();
        verify(observer, never()).onNext(any());
        verify(observer, never()).onError(any(Throwable.class));
    }

    public void test0Sources() {

    }

    @Test
    public void test1ToNSources() {
        int n = 30;
        FuncN<List<Object>> func = new FuncN<List<Object>>() {

            @Override
            public List<Object> call(Object... args) {
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

            @SuppressWarnings("unchecked")
            Observer<List<Object>> o = mock(Observer.class);

            result.subscribe(o);

            verify(o).onNext(values);
            verify(o).onCompleted();
            verify(o, never()).onError(any(Throwable.class));
        }
    }

    @Test(timeout = 5000)
    public void test1ToNSourcesScheduled() throws InterruptedException {
        int n = 10;
        FuncN<List<Object>> func = new FuncN<List<Object>>() {

            @Override
            public List<Object> call(Object... args) {
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

            @SuppressWarnings("unchecked")
            final Observer<List<Object>> o = mock(Observer.class);

            final CountDownLatch cdl = new CountDownLatch(1);

            Subscriber<List<Object>> s = new Subscriber<List<Object>>() {

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
                public void onCompleted() {
                    o.onCompleted();
                    cdl.countDown();
                }
            };

            result.subscribe(s);

            cdl.await();

            verify(o).onNext(values);
            verify(o).onCompleted();
            verify(o, never()).onError(any(Throwable.class));
        }
    }

    @Test
    public void test2SourcesOverload() {
        Observable<Integer> s1 = Observable.just(1);
        Observable<Integer> s2 = Observable.just(2);

        Observable<List<Integer>> result = Observable.combineLatest(s1, s2, new Func2<Integer, Integer, List<Integer>>() {
            @Override
            public List<Integer> call(Integer t1, Integer t2) {
                return Arrays.asList(t1, t2);
            }
        });

        @SuppressWarnings("unchecked")
        Observer<Object> o = mock(Observer.class);

        result.subscribe(o);

        verify(o).onNext(Arrays.asList(1, 2));
        verify(o).onCompleted();
        verify(o, never()).onError(any(Throwable.class));
    }

    @Test
    public void test3SourcesOverload() {
        Observable<Integer> s1 = Observable.just(1);
        Observable<Integer> s2 = Observable.just(2);
        Observable<Integer> s3 = Observable.just(3);

        Observable<List<Integer>> result = Observable.combineLatest(s1, s2, s3,
                new Func3<Integer, Integer, Integer, List<Integer>>() {
                    @Override
                    public List<Integer> call(Integer t1, Integer t2, Integer t3) {
                        return Arrays.asList(t1, t2, t3);
                    }
                });

        @SuppressWarnings("unchecked")
        Observer<Object> o = mock(Observer.class);

        result.subscribe(o);

        verify(o).onNext(Arrays.asList(1, 2, 3));
        verify(o).onCompleted();
        verify(o, never()).onError(any(Throwable.class));
    }

    @Test
    public void test4SourcesOverload() {
        Observable<Integer> s1 = Observable.just(1);
        Observable<Integer> s2 = Observable.just(2);
        Observable<Integer> s3 = Observable.just(3);
        Observable<Integer> s4 = Observable.just(4);

        Observable<List<Integer>> result = Observable.combineLatest(s1, s2, s3, s4,
                new Func4<Integer, Integer, Integer, Integer, List<Integer>>() {
                    @Override
                    public List<Integer> call(Integer t1, Integer t2, Integer t3, Integer t4) {
                        return Arrays.asList(t1, t2, t3, t4);
                    }
                });

        @SuppressWarnings("unchecked")
        Observer<Object> o = mock(Observer.class);

        result.subscribe(o);

        verify(o).onNext(Arrays.asList(1, 2, 3, 4));
        verify(o).onCompleted();
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
                new Func5<Integer, Integer, Integer, Integer, Integer, List<Integer>>() {
                    @Override
                    public List<Integer> call(Integer t1, Integer t2, Integer t3, Integer t4, Integer t5) {
                        return Arrays.asList(t1, t2, t3, t4, t5);
                    }
                });

        @SuppressWarnings("unchecked")
        Observer<Object> o = mock(Observer.class);

        result.subscribe(o);

        verify(o).onNext(Arrays.asList(1, 2, 3, 4, 5));
        verify(o).onCompleted();
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
                new Func6<Integer, Integer, Integer, Integer, Integer, Integer, List<Integer>>() {
                    @Override
                    public List<Integer> call(Integer t1, Integer t2, Integer t3, Integer t4, Integer t5, Integer t6) {
                        return Arrays.asList(t1, t2, t3, t4, t5, t6);
                    }
                });

        @SuppressWarnings("unchecked")
        Observer<Object> o = mock(Observer.class);

        result.subscribe(o);

        verify(o).onNext(Arrays.asList(1, 2, 3, 4, 5, 6));
        verify(o).onCompleted();
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
                new Func7<Integer, Integer, Integer, Integer, Integer, Integer, Integer, List<Integer>>() {
                    @Override
                    public List<Integer> call(Integer t1, Integer t2, Integer t3, Integer t4, Integer t5, Integer t6, Integer t7) {
                        return Arrays.asList(t1, t2, t3, t4, t5, t6, t7);
                    }
                });

        @SuppressWarnings("unchecked")
        Observer<Object> o = mock(Observer.class);

        result.subscribe(o);

        verify(o).onNext(Arrays.asList(1, 2, 3, 4, 5, 6, 7));
        verify(o).onCompleted();
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
                new Func8<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer, List<Integer>>() {
                    @Override
                    public List<Integer> call(Integer t1, Integer t2, Integer t3, Integer t4, Integer t5, Integer t6, Integer t7, Integer t8) {
                        return Arrays.asList(t1, t2, t3, t4, t5, t6, t7, t8);
                    }
                });

        @SuppressWarnings("unchecked")
        Observer<Object> o = mock(Observer.class);

        result.subscribe(o);

        verify(o).onNext(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8));
        verify(o).onCompleted();
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
                new Func9<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer, List<Integer>>() {
                    @Override
                    public List<Integer> call(Integer t1, Integer t2, Integer t3, Integer t4, Integer t5, Integer t6, Integer t7, Integer t8, Integer t9) {
                        return Arrays.asList(t1, t2, t3, t4, t5, t6, t7, t8, t9);
                    }
                });

        @SuppressWarnings("unchecked")
        Observer<Object> o = mock(Observer.class);

        result.subscribe(o);

        verify(o).onNext(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9));
        verify(o).onCompleted();
        verify(o, never()).onError(any(Throwable.class));
    }

    @Test
    public void testZeroSources() {
        Observable<Object> result = Observable.combineLatest(Collections.<Observable<Object>> emptyList(), new FuncN<Object>() {

            @Override
            public Object call(Object... args) {
                return args;
            }

        });

        @SuppressWarnings("unchecked")
        Observer<Object> o = mock(Observer.class);

        result.subscribe(o);

        verify(o).onCompleted();
        verify(o, never()).onNext(any());
        verify(o, never()).onError(any(Throwable.class));

    }

    @Test
    public void testBackpressureLoop() {
        for (int i = 0; i < 5000; i++) {
            testBackpressure();
        }
    }
    
    @Test
    public void testBackpressure() {
        Func2<String, Integer, String> combineLatestFunction = getConcatStringIntegerCombineLatestFunction();

        int NUM = RxRingBuffer.SIZE * 4;
        TestSubscriber<String> ts = new TestSubscriber<String>();
        Observable.combineLatest(Observable.just("one", "two"),
                Observable.range(2, NUM), combineLatestFunction).
                observeOn(Schedulers.computation()).subscribe(ts);

        ts.awaitTerminalEvent();
        ts.assertNoErrors();
        List<String> events = ts.getOnNextEvents();
        assertEquals("two2", events.get(0));
        assertEquals("two3", events.get(1));
        assertEquals("two4", events.get(2));
        assertEquals(NUM, events.size());
    }

    @Test
    public void testWithCombineLatestIssue1717() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicInteger count = new AtomicInteger();
        final int SIZE = 2000;
        Observable<Long> timer = Observable.interval(0, 1, TimeUnit.MILLISECONDS)
                .onBackpressureBuffer()
                .observeOn(Schedulers.newThread())
                .doOnEach(new Action1<Notification<? super Long>>() {

                    @Override
                    public void call(Notification<? super Long> n) {
                        //                        System.out.println(n);
                        if (count.incrementAndGet() >= SIZE) {
                            latch.countDown();
                        }
                    }

                }).take(SIZE);

        TestSubscriber<Long> ts = new TestSubscriber<Long>();

        Observable.combineLatest(timer, Observable.<Integer> never(), new Func2<Long, Integer, Long>() {

            @Override
            public Long call(Long t1, Integer t2) {
                return t1;
            }

        }).subscribe(ts);

        if (!latch.await(SIZE + 1000, TimeUnit.MILLISECONDS)) {
            fail("timed out");
        }

        assertEquals(SIZE, count.get());
    }
    
    @Test(timeout=10000)
    public void testCombineLatestRequestOverflow() throws InterruptedException {
        @SuppressWarnings("unchecked")
        List<Observable<Integer>> sources = Arrays.asList(Observable.from(Arrays.asList(1,2,3,4)), Observable.from(Arrays.asList(5,6,7,8)));
        Observable<Integer> o = Observable.combineLatest(sources,new FuncN<Integer>() {
            @Override
            public Integer call(Object... args) {
               return (Integer) args[0];
            }});
        //should get at least 4
        final CountDownLatch latch = new CountDownLatch(4);
        o.subscribeOn(Schedulers.computation()).subscribe(new Subscriber<Integer>() {
            
            @Override
            public void onStart() {
                request(2);
            }

            @Override
            public void onCompleted() {
                //ignore
            }

            @Override
            public void onError(Throwable e) {
                throw new RuntimeException(e);
            }

            @Override
            public void onNext(Integer t) {
                latch.countDown();
                request(Long.MAX_VALUE-1);
            }});
        assertTrue(latch.await(10, TimeUnit.SECONDS));
    }

    @Test
    public void testCombineMany() {
        int n = RxRingBuffer.SIZE * 3;
        
        List<Observable<Integer>> sources = new ArrayList<Observable<Integer>>();
        
        StringBuilder expected = new StringBuilder(n * 2);
        
        for (int i = 0; i < n; i++) {
            sources.add(Observable.just(i));
            expected.append(i);
        }
        
        TestSubscriber<String> ts = TestSubscriber.create();
        
        Observable.combineLatest(sources, new FuncN<String>() {
            @Override
            public String call(Object... args) {
                StringBuilder b = new StringBuilder();
                for (Object o : args) {
                    b.append(o);
                }
                return b.toString();
            }
        }).subscribe(ts);
        
        ts.assertNoErrors();
        ts.assertValue(expected.toString());
        ts.assertCompleted();
    }
    
    @Test
    public void testCombineManyNulls() {
        int n = RxRingBuffer.SIZE * 3;
        
        Observable<Integer> source = Observable.just((Integer)null);
        
        List<Observable<Integer>> sources = new ArrayList<Observable<Integer>>();
        
        for (int i = 0; i < n; i++) {
            sources.add(source);
        }
        
        TestSubscriber<Integer> ts = TestSubscriber.create();
        
        Observable.combineLatest(sources, new FuncN<Integer>() {
            @Override
            public Integer call(Object... args) {
                int sum = 0;
                for (Object o : args) {
                    if (o == null) {
                        sum ++;
                    }
                }
                return sum;
            }
        }).subscribe(ts);
        
        ts.assertValue(n);
        ts.assertNoErrors();
        ts.assertCompleted();
    }

    @Test
    public void testNonFatalExceptionThrownByCombinatorForSingleSourceIsNotReportedByUpstreamOperator() {
        final AtomicBoolean errorOccurred = new AtomicBoolean(false);
        TestSubscriber<Integer> ts = TestSubscriber.create(1);
        Observable<Integer> source = Observable.just(1)
          // if haven't caught exception in combineLatest operator then would incorrectly
          // be picked up by this call to doOnError
          .doOnError(new Action1<Throwable>() {
                @Override
                public void call(Throwable t) {
                    errorOccurred.set(true);
                }
            });
        Observable
          .combineLatest(Collections.singletonList(source), THROW_NON_FATAL)
          .subscribe(ts);
        assertFalse(errorOccurred.get());
    }
    
    private static final FuncN<Integer> THROW_NON_FATAL = new FuncN<Integer>() {
        @Override
        public Integer call(Object... args) {
            throw new RuntimeException();
        }

    };
    
    @SuppressWarnings("unchecked")
    @Test
    public void firstJustError() {
        TestSubscriber<Integer> ts = TestSubscriber.create();
        
        Observable.combineLatestDelayError(
                Arrays.asList(Observable.just(1), Observable.<Integer>error(new TestException())),
                new FuncN<Integer>() {
                    @Override
                    public Integer call(Object... args) {
                        return ((Integer)args[0]) + ((Integer)args[1]);
                    }
                }
        ).subscribe(ts);
        
        ts.assertNoValues();
        ts.assertError(TestException.class);
        ts.assertNotCompleted();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void secondJustError() {
        TestSubscriber<Integer> ts = TestSubscriber.create();
        
        Observable.combineLatestDelayError(
                Arrays.asList(Observable.<Integer>error(new TestException()), Observable.just(1)),
                new FuncN<Integer>() {
                    @Override
                    public Integer call(Object... args) {
                        return ((Integer)args[0]) + ((Integer)args[1]);
                    }
                }
        ).subscribe(ts);
        
        ts.assertNoValues();
        ts.assertError(TestException.class);
        ts.assertNotCompleted();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void oneErrors() {
        TestSubscriber<Integer> ts = TestSubscriber.create();
        
        Observable.combineLatestDelayError(
                Arrays.asList(Observable.just(10).concatWith(Observable.<Integer>error(new TestException())), Observable.just(1)),
                new FuncN<Integer>() {
                    @Override
                    public Integer call(Object... args) {
                        return ((Integer)args[0]) + ((Integer)args[1]);
                    }
                }
        ).subscribe(ts);
        
        ts.assertValues(11);
        ts.assertError(TestException.class);
        ts.assertNotCompleted();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void twoErrors() {
        TestSubscriber<Integer> ts = TestSubscriber.create();
        
        Observable.combineLatestDelayError(
                Arrays.asList(Observable.just(1), Observable.just(10).concatWith(Observable.<Integer>error(new TestException()))),
                new FuncN<Integer>() {
                    @Override
                    public Integer call(Object... args) {
                        return ((Integer)args[0]) + ((Integer)args[1]);
                    }
                }
        ).subscribe(ts);
        
        ts.assertValues(11);
        ts.assertError(TestException.class);
        ts.assertNotCompleted();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void bothError() {
        TestSubscriber<Integer> ts = TestSubscriber.create();
        
        Observable.combineLatestDelayError(
                Arrays.asList(Observable.just(1).concatWith(Observable.<Integer>error(new TestException())), 
                        Observable.just(10).concatWith(Observable.<Integer>error(new TestException()))),
                new FuncN<Integer>() {
                    @Override
                    public Integer call(Object... args) {
                        return ((Integer)args[0]) + ((Integer)args[1]);
                    }
                }
        ).subscribe(ts);
        
        ts.assertValues(11);
        ts.assertError(CompositeException.class);
        ts.assertNotCompleted();
    }

}
