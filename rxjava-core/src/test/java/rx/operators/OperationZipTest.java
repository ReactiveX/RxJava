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

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;
import static rx.operators.OperationZip.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;

import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.observers.TestObserver;
import rx.operators.OperationReduceTest.CustomException;
import rx.subjects.PublishSubject;
import rx.subscriptions.Subscriptions;
import rx.util.functions.Func2;
import rx.util.functions.Func3;
import rx.util.functions.FuncN;
import rx.util.functions.Functions;

public class OperationZipTest {
    Func2<String, String, String> concat2Strings;
    PublishSubject<String> s1;
    PublishSubject<String> s2;
    Observable<String> zipped;

    Observer<String> observer;
    InOrder inOrder;

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() {
        concat2Strings = new Func2<String, String, String>() {
            @Override
            public String call(String t1, String t2) {
                return t1 + "-" + t2;
            }
        };

        s1 = PublishSubject.create();
        s2 = PublishSubject.create();
        zipped = Observable.zip(s1.toObservable(), s2.toObservable(), concat2Strings);

        observer = mock(Observer.class);
        inOrder = inOrder(observer);

        zipped.subscribe(new TestObserver<String>(observer));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCollectionSizeDifferentThanFunction() {
        FuncN<String> zipr = Functions.fromFunc(getConcatStringIntegerIntArrayZipr());
        //Func3<String, Integer, int[], String>

        /* define a Observer to receive aggregated events */
        Observer<String> observer = mock(Observer.class);

        @SuppressWarnings("rawtypes")
        Collection ws = java.util.Collections.singleton(Observable.from("one", "two"));
        Observable<String> w = Observable.create(zip(ws, zipr));
        w.subscribe(new TestObserver<String>(observer));

        verify(observer, times(1)).onError(any(Throwable.class));
        verify(observer, never()).onCompleted();
        verify(observer, never()).onNext(any(String.class));
    }

    @SuppressWarnings("unchecked")
    /* mock calls don't do generics */
    @Test
    public void testZippingDifferentLengthObservableSequences1() {
        Observer<String> w = mock(Observer.class);

        TestObservable w1 = new TestObservable();
        TestObservable w2 = new TestObservable();
        TestObservable w3 = new TestObservable();

        Observable<String> zipW = Observable.create(zip(Observable.create(w1), Observable.create(w2), Observable.create(w3), getConcat3StringsZipr()));
        zipW.subscribe(new TestObserver<String>(w));

        /* simulate sending data */
        // once for w1
        w1.observer.onNext("1a");
        w1.observer.onCompleted();
        // twice for w2
        w2.observer.onNext("2a");
        w2.observer.onNext("2b");
        w2.observer.onCompleted();
        // 4 times for w3
        w3.observer.onNext("3a");
        w3.observer.onNext("3b");
        w3.observer.onNext("3c");
        w3.observer.onNext("3d");
        w3.observer.onCompleted();

        /* we should have been called 1 time on the Observer */
        InOrder io = inOrder(w);
        io.verify(w).onNext("1a2a3a");

        io.verify(w, times(1)).onCompleted();
    }

    @Test
    public void testZippingDifferentLengthObservableSequences2() {
        @SuppressWarnings("unchecked")
        Observer<String> w = mock(Observer.class);

        TestObservable w1 = new TestObservable();
        TestObservable w2 = new TestObservable();
        TestObservable w3 = new TestObservable();

        Observable<String> zipW = Observable.create(zip(Observable.create(w1), Observable.create(w2), Observable.create(w3), getConcat3StringsZipr()));
        zipW.subscribe(new TestObserver<String>(w));

        /* simulate sending data */
        // 4 times for w1
        w1.observer.onNext("1a");
        w1.observer.onNext("1b");
        w1.observer.onNext("1c");
        w1.observer.onNext("1d");
        w1.observer.onCompleted();
        // twice for w2
        w2.observer.onNext("2a");
        w2.observer.onNext("2b");
        w2.observer.onCompleted();
        // 1 times for w3
        w3.observer.onNext("3a");
        w3.observer.onCompleted();

        /* we should have been called 1 time on the Observer */
        InOrder io = inOrder(w);
        io.verify(w).onNext("1a2a3a");

        io.verify(w, times(1)).onCompleted();

    }

    Func2<Object, Object, String> zipr2 = new Func2<Object, Object, String>() {

        @Override
        public String call(Object t1, Object t2) {
            return "" + t1 + t2;
        }

    };
    Func3<Object, Object, Object, String> zipr3 = new Func3<Object, Object, Object, String>() {

        @Override
        public String call(Object t1, Object t2, Object t3) {
            return "" + t1 + t2 + t3;
        }

    };

    /**
     * Testing internal private logic due to the complexity so I want to use TDD to test as a I build it rather than relying purely on the overall functionality expected by the public methods.
     */
    @SuppressWarnings("unchecked")
    /* mock calls don't do generics */
    @Test
    public void testAggregatorSimple() {
        PublishSubject<String> r1 = PublishSubject.create();
        PublishSubject<String> r2 = PublishSubject.create();
        /* define a Observer to receive aggregated events */
        Observer<String> observer = mock(Observer.class);

        Observable.zip(r1.toObservable(), r2.toObservable(), zipr2).subscribe(new TestObserver<String>(observer));

        /* simulate the Observables pushing data into the aggregator */
        r1.onNext("hello");
        r2.onNext("world");

        InOrder inOrder = inOrder(observer);

        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, never()).onCompleted();
        inOrder.verify(observer, times(1)).onNext("helloworld");

        r1.onNext("hello ");
        r2.onNext("again");

        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, never()).onCompleted();
        inOrder.verify(observer, times(1)).onNext("hello again");

        r1.onCompleted();
        r2.onCompleted();

        inOrder.verify(observer, never()).onNext(anyString());
        verify(observer, times(1)).onCompleted();
    }

    @SuppressWarnings("unchecked")
    /* mock calls don't do generics */
    @Test
    public void testAggregatorDifferentSizedResultsWithOnComplete() {
        /* create the aggregator which will execute the zip function when all Observables provide values */
        /* define a Observer to receive aggregated events */
        PublishSubject<String> r1 = PublishSubject.create();
        PublishSubject<String> r2 = PublishSubject.create();
        /* define a Observer to receive aggregated events */
        Observer<String> observer = mock(Observer.class);

        Observable.zip(r1.toObservable(), r2.toObservable(), zipr2).subscribe(new TestObserver<String>(observer));

        /* simulate the Observables pushing data into the aggregator */
        r1.onNext("hello");
        r2.onNext("world");
        r2.onCompleted();

        InOrder inOrder = inOrder(observer);

        inOrder.verify(observer, never()).onError(any(Throwable.class));
        inOrder.verify(observer, times(1)).onNext("helloworld");
        inOrder.verify(observer, times(1)).onCompleted();

        r1.onNext("hi");
        r1.onCompleted();

        inOrder.verify(observer, never()).onError(any(Throwable.class));
        inOrder.verify(observer, never()).onCompleted();
        inOrder.verify(observer, never()).onNext(anyString());
    }

    @SuppressWarnings("unchecked")
    /* mock calls don't do generics */
    @Test
    public void testAggregateMultipleTypes() {
        PublishSubject<String> r1 = PublishSubject.create();
        PublishSubject<Integer> r2 = PublishSubject.create();
        /* define a Observer to receive aggregated events */
        Observer<String> observer = mock(Observer.class);

        Observable.zip(r1.toObservable(), r2.toObservable(), zipr2).subscribe(new TestObserver<String>(observer));

        /* simulate the Observables pushing data into the aggregator */
        r1.onNext("hello");
        r2.onNext(1);
        r2.onCompleted();

        InOrder inOrder = inOrder(observer);

        inOrder.verify(observer, never()).onError(any(Throwable.class));
        inOrder.verify(observer, times(1)).onNext("hello1");
        inOrder.verify(observer, times(1)).onCompleted();

        r1.onNext("hi");
        r1.onCompleted();

        inOrder.verify(observer, never()).onError(any(Throwable.class));
        inOrder.verify(observer, never()).onCompleted();
        inOrder.verify(observer, never()).onNext(anyString());
    }

    @SuppressWarnings("unchecked")
    /* mock calls don't do generics */
    @Test
    public void testAggregate3Types() {
        PublishSubject<String> r1 = PublishSubject.create();
        PublishSubject<Integer> r2 = PublishSubject.create();
        PublishSubject<List<Integer>> r3 = PublishSubject.create();
        /* define a Observer to receive aggregated events */
        Observer<String> observer = mock(Observer.class);

        Observable.zip(r1.toObservable(), r2.toObservable(), r3.toObservable(), zipr3).subscribe(new TestObserver<String>(observer));

        /* simulate the Observables pushing data into the aggregator */
        r1.onNext("hello");
        r2.onNext(2);
        r3.onNext(Arrays.asList(5, 6, 7));

        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, never()).onCompleted();
        verify(observer, times(1)).onNext("hello2[5, 6, 7]");
    }

    @SuppressWarnings("unchecked")
    /* mock calls don't do generics */
    @Test
    public void testAggregatorsWithDifferentSizesAndTiming() {
        PublishSubject<String> r1 = PublishSubject.create();
        PublishSubject<String> r2 = PublishSubject.create();
        /* define a Observer to receive aggregated events */
        Observer<String> observer = mock(Observer.class);

        Observable.zip(r1.toObservable(), r2.toObservable(), zipr2).subscribe(new TestObserver<String>(observer));

        /* simulate the Observables pushing data into the aggregator */
        r1.onNext("one");
        r1.onNext("two");
        r1.onNext("three");
        r2.onNext("A");

        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, never()).onCompleted();
        verify(observer, times(1)).onNext("oneA");

        r1.onNext("four");
        r1.onCompleted();
        r2.onNext("B");
        verify(observer, times(1)).onNext("twoB");
        r2.onNext("C");
        verify(observer, times(1)).onNext("threeC");
        r2.onNext("D");
        verify(observer, times(1)).onNext("fourD");
        r2.onNext("E");
        verify(observer, never()).onNext("E");
        r2.onCompleted();

        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onCompleted();
    }

    @SuppressWarnings("unchecked")
    /* mock calls don't do generics */
    @Test
    public void testAggregatorError() {
        PublishSubject<String> r1 = PublishSubject.create();
        PublishSubject<String> r2 = PublishSubject.create();
        /* define a Observer to receive aggregated events */
        Observer<String> observer = mock(Observer.class);

        Observable.zip(r1.toObservable(), r2.toObservable(), zipr2).subscribe(new TestObserver<String>(observer));

        /* simulate the Observables pushing data into the aggregator */
        r1.onNext("hello");
        r2.onNext("world");

        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, never()).onCompleted();
        verify(observer, times(1)).onNext("helloworld");

        r1.onError(new RuntimeException(""));
        r1.onNext("hello");
        r2.onNext("again");

        verify(observer, times(1)).onError(any(Throwable.class));
        verify(observer, never()).onCompleted();
        // we don't want to be called again after an error
        verify(observer, times(0)).onNext("helloagain");
    }

    @SuppressWarnings("unchecked")
    /* mock calls don't do generics */
    @Test
    public void testAggregatorUnsubscribe() {
        PublishSubject<String> r1 = PublishSubject.create();
        PublishSubject<String> r2 = PublishSubject.create();
        /* define a Observer to receive aggregated events */
        Observer<String> observer = mock(Observer.class);

        Subscription subscription = Observable.zip(r1.toObservable(), r2.toObservable(), zipr2).subscribe(new TestObserver<String>(observer));

        /* simulate the Observables pushing data into the aggregator */
        r1.onNext("hello");
        r2.onNext("world");

        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, never()).onCompleted();
        verify(observer, times(1)).onNext("helloworld");

        subscription.unsubscribe();
        r1.onNext("hello");
        r2.onNext("again");

        verify(observer, times(0)).onError(any(Throwable.class));
        verify(observer, never()).onCompleted();
        // we don't want to be called again after an error
        verify(observer, times(0)).onNext("helloagain");
    }

    @SuppressWarnings("unchecked")
    /* mock calls don't do generics */
    @Test
    public void testAggregatorEarlyCompletion() {
        PublishSubject<String> r1 = PublishSubject.create();
        PublishSubject<String> r2 = PublishSubject.create();
        /* define a Observer to receive aggregated events */
        Observer<String> observer = mock(Observer.class);

        Observable.zip(r1.toObservable(), r2.toObservable(), zipr2).subscribe(new TestObserver<String>(observer));

        /* simulate the Observables pushing data into the aggregator */
        r1.onNext("one");
        r1.onNext("two");
        r1.onCompleted();
        r2.onNext("A");

        InOrder inOrder = inOrder(observer);

        inOrder.verify(observer, never()).onError(any(Throwable.class));
        inOrder.verify(observer, never()).onCompleted();
        inOrder.verify(observer, times(1)).onNext("oneA");

        r2.onCompleted();

        inOrder.verify(observer, never()).onError(any(Throwable.class));
        inOrder.verify(observer, times(1)).onCompleted();
        inOrder.verify(observer, never()).onNext(anyString());
    }

    @SuppressWarnings("unchecked")
    /* mock calls don't do generics */
    @Test
    public void testZip2Types() {
        Func2<String, Integer, String> zipr = getConcatStringIntegerZipr();

        /* define a Observer to receive aggregated events */
        Observer<String> observer = mock(Observer.class);

        Observable<String> w = Observable.create(zip(Observable.from("one", "two"), Observable.from(2, 3, 4), zipr));
        w.subscribe(new TestObserver<String>(observer));

        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onCompleted();
        verify(observer, times(1)).onNext("one2");
        verify(observer, times(1)).onNext("two3");
        verify(observer, never()).onNext("4");
    }

    @SuppressWarnings("unchecked")
    /* mock calls don't do generics */
    @Test
    public void testZip3Types() {
        Func3<String, Integer, int[], String> zipr = getConcatStringIntegerIntArrayZipr();

        /* define a Observer to receive aggregated events */
        Observer<String> observer = mock(Observer.class);

        Observable<String> w = Observable.create(zip(Observable.from("one", "two"), Observable.from(2), Observable.from(new int[] { 4, 5, 6 }), zipr));
        w.subscribe(new TestObserver<String>(observer));

        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onCompleted();
        verify(observer, times(1)).onNext("one2[4, 5, 6]");
        verify(observer, never()).onNext("two");
    }

    @Test
    public void testOnNextExceptionInvokesOnError() {
        Func2<Integer, Integer, Integer> zipr = getDivideZipr();

        @SuppressWarnings("unchecked")
        Observer<Integer> observer = mock(Observer.class);

        Observable<Integer> w = Observable.create(zip(Observable.from(10, 20, 30), Observable.from(0, 1, 2), zipr));
        w.subscribe(new TestObserver<Integer>(observer));

        verify(observer, times(1)).onError(any(Throwable.class));
    }

    @Test
    public void testOnFirstCompletion() {
        PublishSubject<String> oA = PublishSubject.create();
        PublishSubject<String> oB = PublishSubject.create();

        @SuppressWarnings("unchecked")
        Observer<String> obs = mock(Observer.class);

        Observable<String> o = Observable.create(zip(oA.toObservable(), oB.toObservable(), getConcat2Strings()));
        o.subscribe(new TestObserver<String>(obs));

        InOrder io = inOrder(obs);

        oA.onNext("a1");
        io.verify(obs, never()).onNext(anyString());
        oB.onNext("b1");
        io.verify(obs, times(1)).onNext("a1-b1");
        oB.onNext("b2");
        io.verify(obs, never()).onNext(anyString());
        oA.onNext("a2");
        io.verify(obs, times(1)).onNext("a2-b2");

        oA.onNext("a3");
        oA.onNext("a4");
        oA.onNext("a5");
        oA.onCompleted();

        // SHOULD ONCOMPLETE BE EMITTED HERE INSTEAD OF WAITING
        // FOR B3, B4, B5 TO BE EMITTED?

        oB.onNext("b3");
        oB.onNext("b4");
        oB.onNext("b5");

        io.verify(obs, times(1)).onNext("a3-b3");
        io.verify(obs, times(1)).onNext("a4-b4");
        io.verify(obs, times(1)).onNext("a5-b5");

        // WE RECEIVE THE ONCOMPLETE HERE
        io.verify(obs, times(1)).onCompleted();

        oB.onNext("b6");
        oB.onNext("b7");
        oB.onNext("b8");
        oB.onNext("b9");
        // never completes (infinite stream for example)

        // we should receive nothing else despite oB continuing after oA completed
        io.verifyNoMoreInteractions();
    }

    @Test
    public void testOnErrorTermination() {
        PublishSubject<String> oA = PublishSubject.create();
        PublishSubject<String> oB = PublishSubject.create();

        @SuppressWarnings("unchecked")
        Observer<String> obs = mock(Observer.class);

        Observable<String> o = Observable.create(zip(oA.toObservable(), oB.toObservable(), getConcat2Strings()));
        o.subscribe(new TestObserver<String>(obs));

        InOrder io = inOrder(obs);

        oA.onNext("a1");
        io.verify(obs, never()).onNext(anyString());
        oB.onNext("b1");
        io.verify(obs, times(1)).onNext("a1-b1");
        oB.onNext("b2");
        io.verify(obs, never()).onNext(anyString());
        oA.onNext("a2");
        io.verify(obs, times(1)).onNext("a2-b2");

        oA.onNext("a3");
        oA.onNext("a4");
        oA.onNext("a5");
        oA.onError(new RuntimeException("forced failure"));

        // it should emit failure immediately
        io.verify(obs, times(1)).onError(any(RuntimeException.class));

        oB.onNext("b3");
        oB.onNext("b4");
        oB.onNext("b5");
        oB.onNext("b6");
        oB.onNext("b7");
        oB.onNext("b8");
        oB.onNext("b9");
        // never completes (infinite stream for example)

        // we should receive nothing else despite oB continuing after oA completed
        io.verifyNoMoreInteractions();
    }

    private Func2<String, String, String> getConcat2Strings() {
        return new Func2<String, String, String>() {

            @Override
            public String call(String t1, String t2) {
                return t1 + "-" + t2;
            }
        };
    }

    private Func2<Integer, Integer, Integer> getDivideZipr() {
        Func2<Integer, Integer, Integer> zipr = new Func2<Integer, Integer, Integer>() {

            @Override
            public Integer call(Integer i1, Integer i2) {
                return i1 / i2;
            }

        };
        return zipr;
    }

    private Func3<String, String, String, String> getConcat3StringsZipr() {
        Func3<String, String, String, String> zipr = new Func3<String, String, String, String>() {

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
        return zipr;
    }

    private FuncN<String> getConcatZipr() {
        FuncN<String> zipr = new FuncN<String>() {

            @Override
            public String call(Object... args) {
                String returnValue = "";
                for (Object o : args) {
                    if (o != null) {
                        returnValue += getStringValue(o);
                    }
                }
                System.out.println("returning: " + returnValue);
                return returnValue;
            }

        };
        return zipr;
    }

    private Func2<String, Integer, String> getConcatStringIntegerZipr() {
        Func2<String, Integer, String> zipr = new Func2<String, Integer, String>() {

            @Override
            public String call(String s, Integer i) {
                return getStringValue(s) + getStringValue(i);
            }

        };
        return zipr;
    }

    private Func3<String, Integer, int[], String> getConcatStringIntegerIntArrayZipr() {
        Func3<String, Integer, int[], String> zipr = new Func3<String, Integer, int[], String>() {

            @Override
            public String call(String s, Integer i, int[] iArray) {
                return getStringValue(s) + getStringValue(i) + getStringValue(iArray);
            }

        };
        return zipr;
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

    private static class TestObservable implements Observable.OnSubscribeFunc<String> {

        Observer<? super String> observer;

        @Override
        public Subscription onSubscribe(Observer<? super String> Observer) {
            // just store the variable where it can be accessed so we can manually trigger it
            this.observer = Observer;
            return Subscriptions.empty();
        }

    }

    @Test
    public void testFirstCompletesThenSecondInfinite() {
        s1.onNext("a");
        s1.onNext("b");
        s1.onCompleted();
        s2.onNext("1");
        inOrder.verify(observer, times(1)).onNext("a-1");
        s2.onNext("2");
        inOrder.verify(observer, times(1)).onNext("b-2");
        inOrder.verify(observer, times(1)).onCompleted();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testSecondInfiniteThenFirstCompletes() {
        s2.onNext("1");
        s2.onNext("2");
        s1.onNext("a");
        inOrder.verify(observer, times(1)).onNext("a-1");
        s1.onNext("b");
        inOrder.verify(observer, times(1)).onNext("b-2");
        s1.onCompleted();
        inOrder.verify(observer, times(1)).onCompleted();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testSecondCompletesThenFirstInfinite() {
        s2.onNext("1");
        s2.onNext("2");
        s2.onCompleted();
        s1.onNext("a");
        inOrder.verify(observer, times(1)).onNext("a-1");
        s1.onNext("b");
        inOrder.verify(observer, times(1)).onNext("b-2");
        inOrder.verify(observer, times(1)).onCompleted();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testFirstInfiniteThenSecondCompletes() {
        s1.onNext("a");
        s1.onNext("b");
        s2.onNext("1");
        inOrder.verify(observer, times(1)).onNext("a-1");
        s2.onNext("2");
        inOrder.verify(observer, times(1)).onNext("b-2");
        s2.onCompleted();
        inOrder.verify(observer, times(1)).onCompleted();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testFirstFails() {
        s2.onNext("a");
        s1.onError(new RuntimeException("Forced failure"));

        inOrder.verify(observer, times(1)).onError(any(RuntimeException.class));

        s2.onNext("b");
        s1.onNext("1");
        s1.onNext("2");

        inOrder.verify(observer, never()).onCompleted();
        inOrder.verify(observer, never()).onNext(any(String.class));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testSecondFails() {
        s1.onNext("a");
        s1.onNext("b");
        s2.onError(new RuntimeException("Forced failure"));

        inOrder.verify(observer, times(1)).onError(any(RuntimeException.class));

        s2.onNext("1");
        s2.onNext("2");

        inOrder.verify(observer, never()).onCompleted();
        inOrder.verify(observer, never()).onNext(any(String.class));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testZipIterableSameSize() {
        PublishSubject<String> r1 = PublishSubject.create();
        /* define a Observer to receive aggregated events */
        Observer<String> o = mock(Observer.class);
        InOrder io = inOrder(o);

        Iterable<String> r2 = Arrays.asList("1", "2", "3");

        r1.toObservable().zip(r2, zipr2).subscribe(new TestObserver<String>(o));

        r1.onNext("one-");
        r1.onNext("two-");
        r1.onNext("three-");
        r1.onCompleted();

        io.verify(o).onNext("one-1");
        io.verify(o).onNext("two-2");
        io.verify(o).onNext("three-3");
        io.verify(o).onCompleted();

        verify(o, never()).onError(any(Throwable.class));

    }

    @Test
    public void testZipIterableEmptyFirstSize() {
        PublishSubject<String> r1 = PublishSubject.create();
        /* define a Observer to receive aggregated events */
        Observer<String> o = mock(Observer.class);
        InOrder io = inOrder(o);

        Iterable<String> r2 = Arrays.asList("1", "2", "3");

        r1.toObservable().zip(r2, zipr2).subscribe(new TestObserver<String>(o));

        r1.onCompleted();

        io.verify(o).onCompleted();

        verify(o, never()).onNext(any(String.class));
        verify(o, never()).onError(any(Throwable.class));

    }

    @Test
    public void testZipIterableEmptySecond() {
        PublishSubject<String> r1 = PublishSubject.create();
        /* define a Observer to receive aggregated events */
        Observer<String> o = mock(Observer.class);
        InOrder io = inOrder(o);

        Iterable<String> r2 = Arrays.asList();

        r1.toObservable().zip(r2, zipr2).subscribe(new TestObserver<String>(o));

        r1.onNext("one-");
        r1.onNext("two-");
        r1.onNext("three-");
        r1.onCompleted();

        io.verify(o).onCompleted();

        verify(o, never()).onNext(any(String.class));
        verify(o, never()).onError(any(Throwable.class));
    }

    @Test
    public void testZipIterableFirstShorter() {
        PublishSubject<String> r1 = PublishSubject.create();
        /* define a Observer to receive aggregated events */
        Observer<String> o = mock(Observer.class);
        InOrder io = inOrder(o);

        Iterable<String> r2 = Arrays.asList("1", "2", "3");

        r1.toObservable().zip(r2, zipr2).subscribe(new TestObserver<String>(o));

        r1.onNext("one-");
        r1.onNext("two-");
        r1.onCompleted();

        io.verify(o).onNext("one-1");
        io.verify(o).onNext("two-2");
        io.verify(o).onCompleted();

        verify(o, never()).onError(any(Throwable.class));

    }

    @Test
    public void testZipIterableSecondShorter() {
        PublishSubject<String> r1 = PublishSubject.create();
        /* define a Observer to receive aggregated events */
        Observer<String> o = mock(Observer.class);
        InOrder io = inOrder(o);

        Iterable<String> r2 = Arrays.asList("1", "2");

        r1.toObservable().zip(r2, zipr2).subscribe(new TestObserver<String>(o));

        r1.onNext("one-");
        r1.onNext("two-");
        r1.onNext("three-");
        r1.onCompleted();

        io.verify(o).onNext("one-1");
        io.verify(o).onNext("two-2");
        io.verify(o).onCompleted();

        verify(o, never()).onError(any(Throwable.class));

    }

    @Test
    public void testZipIterableFirstThrows() {
        PublishSubject<String> r1 = PublishSubject.create();
        /* define a Observer to receive aggregated events */
        Observer<String> o = mock(Observer.class);
        InOrder io = inOrder(o);

        Iterable<String> r2 = Arrays.asList("1", "2", "3");

        r1.toObservable().zip(r2, zipr2).subscribe(new TestObserver<String>(o));

        r1.onNext("one-");
        r1.onNext("two-");
        r1.onError(new OperationReduceTest.CustomException());

        io.verify(o).onNext("one-1");
        io.verify(o).onNext("two-2");
        io.verify(o).onError(any(OperationReduceTest.CustomException.class));

        verify(o, never()).onCompleted();

    }

    @Test
    public void testZipIterableIteratorThrows() {
        PublishSubject<String> r1 = PublishSubject.create();
        /* define a Observer to receive aggregated events */
        Observer<String> o = mock(Observer.class);
        InOrder io = inOrder(o);

        Iterable<String> r2 = new Iterable<String>() {
            @Override
            public Iterator<String> iterator() {
                throw new OperationReduceTest.CustomException();
            }
        };

        r1.toObservable().zip(r2, zipr2).subscribe(new TestObserver<String>(o));

        r1.onNext("one-");
        r1.onNext("two-");
        r1.onError(new OperationReduceTest.CustomException());

        io.verify(o).onError(any(OperationReduceTest.CustomException.class));

        verify(o, never()).onCompleted();
        verify(o, never()).onNext(any(String.class));

    }

    @Test
    public void testZipIterableHasNextThrows() {
        PublishSubject<String> r1 = PublishSubject.create();
        /* define a Observer to receive aggregated events */
        Observer<String> o = mock(Observer.class);
        InOrder io = inOrder(o);

        Iterable<String> r2 = new Iterable<String>() {

            @Override
            public Iterator<String> iterator() {
                return new Iterator<String>() {
                    int count;

                    @Override
                    public boolean hasNext() {
                        if (count == 0) {
                            return true;
                        }
                        throw new CustomException();
                    }

                    @Override
                    public String next() {
                        count++;
                        return "1";
                    }

                    @Override
                    public void remove() {
                        throw new UnsupportedOperationException("Not supported yet.");
                    }

                };
            }

        };

        r1.toObservable().zip(r2, zipr2).subscribe(new TestObserver<String>(o));

        r1.onNext("one-");
        r1.onError(new OperationReduceTest.CustomException());

        io.verify(o).onNext("one-1");
        io.verify(o).onError(any(OperationReduceTest.CustomException.class));

        verify(o, never()).onCompleted();

    }

    @Test
    public void testZipIterableNextThrows() {
        PublishSubject<String> r1 = PublishSubject.create();
        /* define a Observer to receive aggregated events */
        Observer<String> o = mock(Observer.class);
        InOrder io = inOrder(o);

        Iterable<String> r2 = new Iterable<String>() {

            @Override
            public Iterator<String> iterator() {
                return new Iterator<String>() {
                    int count;

                    @Override
                    public boolean hasNext() {
                        return true;
                    }

                    @Override
                    public String next() {
                        throw new CustomException();
                    }

                    @Override
                    public void remove() {
                        throw new UnsupportedOperationException("Not supported yet.");
                    }

                };
            }

        };

        r1.toObservable().zip(r2, zipr2).subscribe(new TestObserver<String>(o));

        r1.onError(new OperationReduceTest.CustomException());

        io.verify(o).onError(any(OperationReduceTest.CustomException.class));

        verify(o, never()).onNext(any(String.class));
        verify(o, never()).onCompleted();

    }
    
    @Test
    public void testZipWithOnCompletedTwice() {
        // issue: https://groups.google.com/forum/#!topic/rxjava/79cWTv3TFp0
        // The problem is the original "zip" implementation does not wrap
        // an internal observer with a SafeObserver. However, in the "zip",
        // it may calls "onCompleted" twice. That breaks the Rx contract.

        // This test tries to emulate this case.
        // As "mock(Observer.class)" will create an instance in the package "rx",
        // we need to wrap "mock(Observer.class)" with an observer instance
        // which is in the package "rx.operators".
        @SuppressWarnings("unchecked")
        final Observer<Integer> observer = mock(Observer.class);

        Observable.zip(Observable.from(1),
                Observable.from(1), new Func2<Integer, Integer, Integer>() {
                    @Override
                    public Integer call(Integer a, Integer b) {
                        return a + b;
                    }
                }).subscribe(new Observer<Integer>() {

            @Override
            public void onCompleted() {
                observer.onCompleted();
            }

            @Override
            public void onError(Throwable e) {
                observer.onError(e);
            }

            @Override
            public void onNext(Integer args) {
                observer.onNext(args);
            }

        });

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onNext(2);
        inOrder.verify(observer, times(1)).onCompleted();
        inOrder.verifyNoMoreInteractions();
    }
}
