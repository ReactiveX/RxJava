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
package rx.operators;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Matchers;

import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.functions.Func2;
import rx.functions.Func3;
import rx.functions.Func4;
import rx.functions.Func5;
import rx.functions.Func6;
import rx.functions.Func7;
import rx.functions.Func8;
import rx.functions.Func9;
import rx.functions.FuncN;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;

public class OperatorCombineLatestTest {

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

        Observable<String> w = Observable.combineLatest(Observable.from("one", "two"), Observable.from(2, 3, 4), combineLatestFunction);
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

        Observable<String> w = Observable.combineLatest(Observable.from("one", "two"), Observable.from(2), Observable.from(new int[] { 4, 5, 6 }), combineLatestFunction);
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

        Observable<String> w = Observable.combineLatest(Observable.from("one"), Observable.from(2), Observable.from(new int[] { 4, 5, 6 }, new int[] { 7, 8 }), combineLatestFunction);
        w.subscribe(observer);

        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onCompleted();
        verify(observer, times(1)).onNext("one2[4, 5, 6]");
        verify(observer, times(1)).onNext("one2[7, 8]");
    }

    private Func3<String, String, String, String> getConcat3StringsCombineLatestFunction() {
        Func3<String, String, String, String> combineLatestFunction = new Func3<String, String, String, String>() {

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
        return combineLatestFunction;
    }

    private Func2<String, Integer, String> getConcatStringIntegerCombineLatestFunction() {
        Func2<String, Integer, String> combineLatestFunction = new Func2<String, Integer, String>() {

            @Override
            public String call(String s, Integer i) {
                return getStringValue(s) + getStringValue(i);
            }

        };
        return combineLatestFunction;
    }

    private Func3<String, Integer, int[], String> getConcatStringIntegerIntArrayCombineLatestFunction() {
        Func3<String, Integer, int[], String> combineLatestFunction = new Func3<String, Integer, int[], String>() {

            @Override
            public String call(String s, Integer i, int[] iArray) {
                return getStringValue(s) + getStringValue(i) + getStringValue(iArray);
            }

        };
        return combineLatestFunction;
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
                sources.add(Observable.just(j, Schedulers.io()));
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
        Observable<Object> result = Observable.combineLatest(Collections.<Observable<Object>>emptyList(), new FuncN<Object>() {

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
}
