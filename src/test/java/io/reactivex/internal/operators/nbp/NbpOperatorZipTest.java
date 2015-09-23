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

import org.junit.*;
import org.mockito.InOrder;

import io.reactivex.*;
import io.reactivex.NbpObservable.*;
import io.reactivex.Observable;
import io.reactivex.disposables.BooleanDisposable;
import io.reactivex.functions.Function3;
import io.reactivex.internal.disposables.EmptyDisposable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.nbp.NbpPublishSubject;
import io.reactivex.subscribers.nbp.NbpTestSubscriber;

public class NbpOperatorZipTest {
    BiFunction<String, String, String> concat2Strings;
    NbpPublishSubject<String> s1;
    NbpPublishSubject<String> s2;
    NbpObservable<String> zipped;

    NbpSubscriber<String> NbpObserver;
    InOrder inOrder;

    @Before
    public void setUp() {
        concat2Strings = new BiFunction<String, String, String>() {
            @Override
            public String apply(String t1, String t2) {
                return t1 + "-" + t2;
            }
        };

        s1 = NbpPublishSubject.create();
        s2 = NbpPublishSubject.create();
        zipped = NbpObservable.zip(s1, s2, concat2Strings);

        NbpObserver = TestHelper.mockNbpSubscriber();
        inOrder = inOrder(NbpObserver);

        zipped.subscribe(NbpObserver);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCollectionSizeDifferentThanFunction() {
        Function<Object[], String> zipr = getConcatStringIntegerIntArrayZipr();
        //Function3<String, Integer, int[], String>

        /* define a NbpSubscriber to receive aggregated events */
        NbpSubscriber<String> NbpObserver = TestHelper.mockNbpSubscriber();

        @SuppressWarnings("rawtypes")
        Collection ws = java.util.Collections.singleton(NbpObservable.just("one", "two"));
        NbpObservable<String> w = NbpObservable.zip(ws, zipr);
        w.subscribe(NbpObserver);

        verify(NbpObserver, times(1)).onError(any(Throwable.class));
        verify(NbpObserver, never()).onComplete();
        verify(NbpObserver, never()).onNext(any(String.class));
    }

    @Test
    public void testStartpingDifferentLengthObservableSequences1() {
        NbpSubscriber<String> w = TestHelper.mockNbpSubscriber();

        TestObservable w1 = new TestObservable();
        TestObservable w2 = new TestObservable();
        TestObservable w3 = new TestObservable();

        NbpObservable<String> zipW = NbpObservable.zip(
                NbpObservable.create(w1), NbpObservable.create(w2), 
                NbpObservable.create(w3), getConcat3StringsZipr());
        zipW.subscribe(w);

        /* simulate sending data */
        // once for w1
        w1.NbpObserver.onNext("1a");
        w1.NbpObserver.onComplete();
        // twice for w2
        w2.NbpObserver.onNext("2a");
        w2.NbpObserver.onNext("2b");
        w2.NbpObserver.onComplete();
        // 4 times for w3
        w3.NbpObserver.onNext("3a");
        w3.NbpObserver.onNext("3b");
        w3.NbpObserver.onNext("3c");
        w3.NbpObserver.onNext("3d");
        w3.NbpObserver.onComplete();

        /* we should have been called 1 time on the NbpSubscriber */
        InOrder io = inOrder(w);
        io.verify(w).onNext("1a2a3a");

        io.verify(w, times(1)).onComplete();
    }

    @Test
    public void testStartpingDifferentLengthObservableSequences2() {
        NbpSubscriber<String> w = TestHelper.mockNbpSubscriber();

        TestObservable w1 = new TestObservable();
        TestObservable w2 = new TestObservable();
        TestObservable w3 = new TestObservable();

        NbpObservable<String> zipW = NbpObservable.zip(NbpObservable.create(w1), NbpObservable.create(w2), NbpObservable.create(w3), getConcat3StringsZipr());
        zipW.subscribe(w);

        /* simulate sending data */
        // 4 times for w1
        w1.NbpObserver.onNext("1a");
        w1.NbpObserver.onNext("1b");
        w1.NbpObserver.onNext("1c");
        w1.NbpObserver.onNext("1d");
        w1.NbpObserver.onComplete();
        // twice for w2
        w2.NbpObserver.onNext("2a");
        w2.NbpObserver.onNext("2b");
        w2.NbpObserver.onComplete();
        // 1 times for w3
        w3.NbpObserver.onNext("3a");
        w3.NbpObserver.onComplete();

        /* we should have been called 1 time on the NbpSubscriber */
        InOrder io = inOrder(w);
        io.verify(w).onNext("1a2a3a");

        io.verify(w, times(1)).onComplete();

    }

    BiFunction<Object, Object, String> zipr2 = new BiFunction<Object, Object, String>() {

        @Override
        public String apply(Object t1, Object t2) {
            return "" + t1 + t2;
        }

    };
    Function3<Object, Object, Object, String> zipr3 = new Function3<Object, Object, Object, String>() {

        @Override
        public String apply(Object t1, Object t2, Object t3) {
            return "" + t1 + t2 + t3;
        }

    };

    /**
     * Testing internal private logic due to the complexity so I want to use TDD to test as a I build it rather than relying purely on the overall functionality expected by the public methods.
     */
    @Test
    public void testAggregatorSimple() {
        NbpPublishSubject<String> r1 = NbpPublishSubject.create();
        NbpPublishSubject<String> r2 = NbpPublishSubject.create();
        /* define a NbpSubscriber to receive aggregated events */
        NbpSubscriber<String> NbpObserver = TestHelper.mockNbpSubscriber();

        NbpObservable.zip(r1, r2, zipr2).subscribe(NbpObserver);

        /* simulate the Observables pushing data into the aggregator */
        r1.onNext("hello");
        r2.onNext("world");

        InOrder inOrder = inOrder(NbpObserver);

        verify(NbpObserver, never()).onError(any(Throwable.class));
        verify(NbpObserver, never()).onComplete();
        inOrder.verify(NbpObserver, times(1)).onNext("helloworld");

        r1.onNext("hello ");
        r2.onNext("again");

        verify(NbpObserver, never()).onError(any(Throwable.class));
        verify(NbpObserver, never()).onComplete();
        inOrder.verify(NbpObserver, times(1)).onNext("hello again");

        r1.onComplete();
        r2.onComplete();

        inOrder.verify(NbpObserver, never()).onNext(anyString());
        verify(NbpObserver, times(1)).onComplete();
    }

    @Test
    public void testAggregatorDifferentSizedResultsWithOnComplete() {
        /* create the aggregator which will execute the zip function when all Observables provide values */
        /* define a NbpSubscriber to receive aggregated events */
        NbpPublishSubject<String> r1 = NbpPublishSubject.create();
        NbpPublishSubject<String> r2 = NbpPublishSubject.create();
        /* define a NbpSubscriber to receive aggregated events */
        NbpSubscriber<String> NbpObserver = TestHelper.mockNbpSubscriber();
        NbpObservable.zip(r1, r2, zipr2).subscribe(NbpObserver);

        /* simulate the Observables pushing data into the aggregator */
        r1.onNext("hello");
        r2.onNext("world");
        r2.onComplete();

        InOrder inOrder = inOrder(NbpObserver);

        inOrder.verify(NbpObserver, never()).onError(any(Throwable.class));
        inOrder.verify(NbpObserver, times(1)).onNext("helloworld");
        inOrder.verify(NbpObserver, times(1)).onComplete();

        r1.onNext("hi");
        r1.onComplete();

        inOrder.verify(NbpObserver, never()).onError(any(Throwable.class));
        inOrder.verify(NbpObserver, never()).onComplete();
        inOrder.verify(NbpObserver, never()).onNext(anyString());
    }

    @Test
    public void testAggregateMultipleTypes() {
        NbpPublishSubject<String> r1 = NbpPublishSubject.create();
        NbpPublishSubject<Integer> r2 = NbpPublishSubject.create();
        /* define a NbpSubscriber to receive aggregated events */
        NbpSubscriber<String> NbpObserver = TestHelper.mockNbpSubscriber();

        NbpObservable.zip(r1, r2, zipr2).subscribe(NbpObserver);

        /* simulate the Observables pushing data into the aggregator */
        r1.onNext("hello");
        r2.onNext(1);
        r2.onComplete();

        InOrder inOrder = inOrder(NbpObserver);

        inOrder.verify(NbpObserver, never()).onError(any(Throwable.class));
        inOrder.verify(NbpObserver, times(1)).onNext("hello1");
        inOrder.verify(NbpObserver, times(1)).onComplete();

        r1.onNext("hi");
        r1.onComplete();

        inOrder.verify(NbpObserver, never()).onError(any(Throwable.class));
        inOrder.verify(NbpObserver, never()).onComplete();
        inOrder.verify(NbpObserver, never()).onNext(anyString());
    }

    @Test
    public void testAggregate3Types() {
        NbpPublishSubject<String> r1 = NbpPublishSubject.create();
        NbpPublishSubject<Integer> r2 = NbpPublishSubject.create();
        NbpPublishSubject<List<Integer>> r3 = NbpPublishSubject.create();
        /* define a NbpSubscriber to receive aggregated events */
        NbpSubscriber<String> NbpObserver = TestHelper.mockNbpSubscriber();

        NbpObservable.zip(r1, r2, r3, zipr3).subscribe(NbpObserver);

        /* simulate the Observables pushing data into the aggregator */
        r1.onNext("hello");
        r2.onNext(2);
        r3.onNext(Arrays.asList(5, 6, 7));

        verify(NbpObserver, never()).onError(any(Throwable.class));
        verify(NbpObserver, never()).onComplete();
        verify(NbpObserver, times(1)).onNext("hello2[5, 6, 7]");
    }

    @Test
    public void testAggregatorsWithDifferentSizesAndTiming() {
        NbpPublishSubject<String> r1 = NbpPublishSubject.create();
        NbpPublishSubject<String> r2 = NbpPublishSubject.create();
        /* define a NbpSubscriber to receive aggregated events */
        NbpSubscriber<String> NbpObserver = TestHelper.mockNbpSubscriber();

        NbpObservable.zip(r1, r2, zipr2).subscribe(NbpObserver);

        /* simulate the Observables pushing data into the aggregator */
        r1.onNext("one");
        r1.onNext("two");
        r1.onNext("three");
        r2.onNext("A");

        verify(NbpObserver, never()).onError(any(Throwable.class));
        verify(NbpObserver, never()).onComplete();
        verify(NbpObserver, times(1)).onNext("oneA");

        r1.onNext("four");
        r1.onComplete();
        r2.onNext("B");
        verify(NbpObserver, times(1)).onNext("twoB");
        r2.onNext("C");
        verify(NbpObserver, times(1)).onNext("threeC");
        r2.onNext("D");
        verify(NbpObserver, times(1)).onNext("fourD");
        r2.onNext("E");
        verify(NbpObserver, never()).onNext("E");
        r2.onComplete();

        verify(NbpObserver, never()).onError(any(Throwable.class));
        verify(NbpObserver, times(1)).onComplete();
    }

    @Test
    public void testAggregatorError() {
        NbpPublishSubject<String> r1 = NbpPublishSubject.create();
        NbpPublishSubject<String> r2 = NbpPublishSubject.create();
        /* define a NbpSubscriber to receive aggregated events */
        NbpSubscriber<String> NbpObserver = TestHelper.mockNbpSubscriber();

        NbpObservable.zip(r1, r2, zipr2).subscribe(NbpObserver);

        /* simulate the Observables pushing data into the aggregator */
        r1.onNext("hello");
        r2.onNext("world");

        verify(NbpObserver, never()).onError(any(Throwable.class));
        verify(NbpObserver, never()).onComplete();
        verify(NbpObserver, times(1)).onNext("helloworld");

        r1.onError(new RuntimeException(""));
        r1.onNext("hello");
        r2.onNext("again");

        verify(NbpObserver, times(1)).onError(any(Throwable.class));
        verify(NbpObserver, never()).onComplete();
        // we don't want to be called again after an error
        verify(NbpObserver, times(0)).onNext("helloagain");
    }

    @Test
    public void testAggregatorUnsubscribe() {
        NbpPublishSubject<String> r1 = NbpPublishSubject.create();
        NbpPublishSubject<String> r2 = NbpPublishSubject.create();
        /* define a NbpSubscriber to receive aggregated events */
        NbpSubscriber<String> NbpObserver = TestHelper.mockNbpSubscriber();
        NbpTestSubscriber<String> ts = new NbpTestSubscriber<>(NbpObserver);

        NbpObservable.zip(r1, r2, zipr2).subscribe(ts);

        /* simulate the Observables pushing data into the aggregator */
        r1.onNext("hello");
        r2.onNext("world");

        verify(NbpObserver, never()).onError(any(Throwable.class));
        verify(NbpObserver, never()).onComplete();
        verify(NbpObserver, times(1)).onNext("helloworld");

        ts.dispose();
        r1.onNext("hello");
        r2.onNext("again");

        verify(NbpObserver, times(0)).onError(any(Throwable.class));
        verify(NbpObserver, never()).onComplete();
        // we don't want to be called again after an error
        verify(NbpObserver, times(0)).onNext("helloagain");
    }

    @Test
    public void testAggregatorEarlyCompletion() {
        NbpPublishSubject<String> r1 = NbpPublishSubject.create();
        NbpPublishSubject<String> r2 = NbpPublishSubject.create();
        /* define a NbpSubscriber to receive aggregated events */
        NbpSubscriber<String> NbpObserver = TestHelper.mockNbpSubscriber();

        NbpObservable.zip(r1, r2, zipr2).subscribe(NbpObserver);

        /* simulate the Observables pushing data into the aggregator */
        r1.onNext("one");
        r1.onNext("two");
        r1.onComplete();
        r2.onNext("A");

        InOrder inOrder = inOrder(NbpObserver);

        inOrder.verify(NbpObserver, never()).onError(any(Throwable.class));
        inOrder.verify(NbpObserver, never()).onComplete();
        inOrder.verify(NbpObserver, times(1)).onNext("oneA");

        r2.onComplete();

        inOrder.verify(NbpObserver, never()).onError(any(Throwable.class));
        inOrder.verify(NbpObserver, times(1)).onComplete();
        inOrder.verify(NbpObserver, never()).onNext(anyString());
    }

    @Test
    public void testStart2Types() {
        BiFunction<String, Integer, String> zipr = getConcatStringIntegerZipr();

        /* define a NbpSubscriber to receive aggregated events */
        NbpSubscriber<String> NbpObserver = TestHelper.mockNbpSubscriber();

        NbpObservable<String> w = NbpObservable.zip(NbpObservable.just("one", "two"), NbpObservable.just(2, 3, 4), zipr);
        w.subscribe(NbpObserver);

        verify(NbpObserver, never()).onError(any(Throwable.class));
        verify(NbpObserver, times(1)).onComplete();
        verify(NbpObserver, times(1)).onNext("one2");
        verify(NbpObserver, times(1)).onNext("two3");
        verify(NbpObserver, never()).onNext("4");
    }

    @Test
    public void testStart3Types() {
        Function3<String, Integer, int[], String> zipr = getConcatStringIntegerIntArrayZipr();

        /* define a NbpSubscriber to receive aggregated events */
        NbpSubscriber<String> NbpObserver = TestHelper.mockNbpSubscriber();

        NbpObservable<String> w = NbpObservable.zip(NbpObservable.just("one", "two"), NbpObservable.just(2), NbpObservable.just(new int[] { 4, 5, 6 }), zipr);
        w.subscribe(NbpObserver);

        verify(NbpObserver, never()).onError(any(Throwable.class));
        verify(NbpObserver, times(1)).onComplete();
        verify(NbpObserver, times(1)).onNext("one2[4, 5, 6]");
        verify(NbpObserver, never()).onNext("two");
    }

    @Test
    public void testOnNextExceptionInvokesOnError() {
        BiFunction<Integer, Integer, Integer> zipr = getDivideZipr();

        NbpSubscriber<Integer> NbpObserver = TestHelper.mockNbpSubscriber();

        NbpObservable<Integer> w = NbpObservable.zip(NbpObservable.just(10, 20, 30), NbpObservable.just(0, 1, 2), zipr);
        w.subscribe(NbpObserver);

        verify(NbpObserver, times(1)).onError(any(Throwable.class));
    }

    @Test
    public void testOnFirstCompletion() {
        NbpPublishSubject<String> oA = NbpPublishSubject.create();
        NbpPublishSubject<String> oB = NbpPublishSubject.create();

        NbpSubscriber<String> obs = TestHelper.mockNbpSubscriber();

        NbpObservable<String> o = NbpObservable.zip(oA, oB, getConcat2Strings());
        o.subscribe(obs);

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
        oA.onComplete();

        // SHOULD ONCOMPLETE BE EMITTED HERE INSTEAD OF WAITING
        // FOR B3, B4, B5 TO BE EMITTED?

        oB.onNext("b3");
        oB.onNext("b4");
        oB.onNext("b5");

        io.verify(obs, times(1)).onNext("a3-b3");
        io.verify(obs, times(1)).onNext("a4-b4");
        io.verify(obs, times(1)).onNext("a5-b5");

        // WE RECEIVE THE ONCOMPLETE HERE
        io.verify(obs, times(1)).onComplete();

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
        NbpPublishSubject<String> oA = NbpPublishSubject.create();
        NbpPublishSubject<String> oB = NbpPublishSubject.create();

        NbpSubscriber<String> obs = TestHelper.mockNbpSubscriber();

        NbpObservable<String> o = NbpObservable.zip(oA, oB, getConcat2Strings());
        o.subscribe(obs);

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

    private BiFunction<String, String, String> getConcat2Strings() {
        return new BiFunction<String, String, String>() {

            @Override
            public String apply(String t1, String t2) {
                return t1 + "-" + t2;
            }
        };
    }

    private BiFunction<Integer, Integer, Integer> getDivideZipr() {
        BiFunction<Integer, Integer, Integer> zipr = new BiFunction<Integer, Integer, Integer>() {

            @Override
            public Integer apply(Integer i1, Integer i2) {
                return i1 / i2;
            }

        };
        return zipr;
    }

    private Function3<String, String, String, String> getConcat3StringsZipr() {
        Function3<String, String, String, String> zipr = new Function3<String, String, String, String>() {

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
        return zipr;
    }

    private BiFunction<String, Integer, String> getConcatStringIntegerZipr() {
        BiFunction<String, Integer, String> zipr = new BiFunction<String, Integer, String>() {

            @Override
            public String apply(String s, Integer i) {
                return getStringValue(s) + getStringValue(i);
            }

        };
        return zipr;
    }

    private Function3<String, Integer, int[], String> getConcatStringIntegerIntArrayZipr() {
        Function3<String, Integer, int[], String> zipr = new Function3<String, Integer, int[], String>() {

            @Override
            public String apply(String s, Integer i, int[] iArray) {
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

    private static class TestObservable implements NbpOnSubscribe<String> {

        NbpSubscriber<? super String> NbpObserver;

        @Override
        public void accept(NbpSubscriber<? super String> NbpObserver) {
            // just store the variable where it can be accessed so we can manually trigger it
            this.NbpObserver = NbpObserver;
            NbpObserver.onSubscribe(EmptyDisposable.INSTANCE);
        }

    }

    @Test
    public void testFirstCompletesThenSecondInfinite() {
        s1.onNext("a");
        s1.onNext("b");
        s1.onComplete();
        s2.onNext("1");
        inOrder.verify(NbpObserver, times(1)).onNext("a-1");
        s2.onNext("2");
        inOrder.verify(NbpObserver, times(1)).onNext("b-2");
        inOrder.verify(NbpObserver, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testSecondInfiniteThenFirstCompletes() {
        s2.onNext("1");
        s2.onNext("2");
        s1.onNext("a");
        inOrder.verify(NbpObserver, times(1)).onNext("a-1");
        s1.onNext("b");
        inOrder.verify(NbpObserver, times(1)).onNext("b-2");
        s1.onComplete();
        inOrder.verify(NbpObserver, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testSecondCompletesThenFirstInfinite() {
        s2.onNext("1");
        s2.onNext("2");
        s2.onComplete();
        s1.onNext("a");
        inOrder.verify(NbpObserver, times(1)).onNext("a-1");
        s1.onNext("b");
        inOrder.verify(NbpObserver, times(1)).onNext("b-2");
        inOrder.verify(NbpObserver, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testFirstInfiniteThenSecondCompletes() {
        s1.onNext("a");
        s1.onNext("b");
        s2.onNext("1");
        inOrder.verify(NbpObserver, times(1)).onNext("a-1");
        s2.onNext("2");
        inOrder.verify(NbpObserver, times(1)).onNext("b-2");
        s2.onComplete();
        inOrder.verify(NbpObserver, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testFirstFails() {
        s2.onNext("a");
        s1.onError(new RuntimeException("Forced failure"));

        inOrder.verify(NbpObserver, times(1)).onError(any(RuntimeException.class));

        s2.onNext("b");
        s1.onNext("1");
        s1.onNext("2");

        inOrder.verify(NbpObserver, never()).onComplete();
        inOrder.verify(NbpObserver, never()).onNext(any(String.class));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testSecondFails() {
        s1.onNext("a");
        s1.onNext("b");
        s2.onError(new RuntimeException("Forced failure"));

        inOrder.verify(NbpObserver, times(1)).onError(any(RuntimeException.class));

        s2.onNext("1");
        s2.onNext("2");

        inOrder.verify(NbpObserver, never()).onComplete();
        inOrder.verify(NbpObserver, never()).onNext(any(String.class));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testStartWithOnCompletedTwice() {
        // issue: https://groups.google.com/forum/#!topic/rxjava/79cWTv3TFp0
        // The problem is the original "zip" implementation does not wrap
        // an internal NbpObserver with a SafeSubscriber. However, in the "zip",
        // it may calls "onCompleted" twice. That breaks the Rx contract.

        // This test tries to emulate this case.
        // As "TestHelper.mockNbpSubscriber()" will create an instance in the package "rx",
        // we need to wrap "TestHelper.mockNbpSubscriber()" with an NbpObserver instance
        // which is in the package "rx.operators".
        final NbpSubscriber<Integer> NbpObserver = TestHelper.mockNbpSubscriber();

        NbpObservable.zip(NbpObservable.just(1),
                NbpObservable.just(1), new BiFunction<Integer, Integer, Integer>() {
                    @Override
                    public Integer apply(Integer a, Integer b) {
                        return a + b;
                    }
                }).subscribe(new NbpObserver<Integer>() {

            @Override
            public void onComplete() {
                NbpObserver.onComplete();
            }

            @Override
            public void onError(Throwable e) {
                NbpObserver.onError(e);
            }

            @Override
            public void onNext(Integer args) {
                NbpObserver.onNext(args);
            }

        });

        InOrder inOrder = inOrder(NbpObserver);
        inOrder.verify(NbpObserver, times(1)).onNext(2);
        inOrder.verify(NbpObserver, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testStart() {
        NbpObservable<String> os = OBSERVABLE_OF_5_INTEGERS
                .zipWith(OBSERVABLE_OF_5_INTEGERS, new BiFunction<Integer, Integer, String>() {

                    @Override
                    public String apply(Integer a, Integer b) {
                        return a + "-" + b;
                    }
                });

        final ArrayList<String> list = new ArrayList<>();
        os.subscribe(new Consumer<String>() {

            @Override
            public void accept(String s) {
                System.out.println(s);
                list.add(s);
            }
        });

        assertEquals(5, list.size());
        assertEquals("1-1", list.get(0));
        assertEquals("2-2", list.get(1));
        assertEquals("5-5", list.get(4));
    }

    @Test
    public void testStartAsync() throws InterruptedException {
        NbpObservable<String> os = ASYNC_OBSERVABLE_OF_INFINITE_INTEGERS(new CountDownLatch(1))
                .zipWith(ASYNC_OBSERVABLE_OF_INFINITE_INTEGERS(new CountDownLatch(1)), new BiFunction<Integer, Integer, String>() {

                    @Override
                    public String apply(Integer a, Integer b) {
                        return a + "-" + b;
                    }
                }).take(5);

        NbpTestSubscriber<String> ts = new NbpTestSubscriber<>();
        os.subscribe(ts);

        ts.awaitTerminalEvent();
        ts.assertNoErrors();

        assertEquals(5, ts.valueCount());
        assertEquals("1-1", ts.values().get(0));
        assertEquals("2-2", ts.values().get(1));
        assertEquals("5-5", ts.values().get(4));
    }

    @Test
    public void testStartInfiniteAndFinite() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final CountDownLatch infiniteObservable = new CountDownLatch(1);
        NbpObservable<String> os = OBSERVABLE_OF_5_INTEGERS
                .zipWith(ASYNC_OBSERVABLE_OF_INFINITE_INTEGERS(infiniteObservable), new BiFunction<Integer, Integer, String>() {

                    @Override
                    public String apply(Integer a, Integer b) {
                        return a + "-" + b;
                    }
                });

        final ArrayList<String> list = new ArrayList<>();
        os.subscribe(new NbpObserver<String>() {

            @Override
            public void onComplete() {
                latch.countDown();
            }

            @Override
            public void onError(Throwable e) {
                e.printStackTrace();
                latch.countDown();
            }

            @Override
            public void onNext(String s) {
                System.out.println(s);
                list.add(s);
            }
        });

        latch.await(1000, TimeUnit.MILLISECONDS);
        if (!infiniteObservable.await(2000, TimeUnit.MILLISECONDS)) {
            throw new RuntimeException("didn't unsubscribe");
        }

        assertEquals(5, list.size());
        assertEquals("1-1", list.get(0));
        assertEquals("2-2", list.get(1));
        assertEquals("5-5", list.get(4));
    }

    @Test
    @Ignore("Null values not allowed")
    public void testEmitNull() {
        NbpObservable<Integer> oi = NbpObservable.just(1, null, 3);
        NbpObservable<String> os = NbpObservable.just("a", "b", null);
        NbpObservable<String> o = NbpObservable.zip(oi, os, new BiFunction<Integer, String, String>() {

            @Override
            public String apply(Integer t1, String t2) {
                return t1 + "-" + t2;
            }

        });

        final ArrayList<String> list = new ArrayList<>();
        o.subscribe(new Consumer<String>() {

            @Override
            public void accept(String s) {
                System.out.println(s);
                list.add(s);
            }
        });

        assertEquals(3, list.size());
        assertEquals("1-a", list.get(0));
        assertEquals("null-b", list.get(1));
        assertEquals("3-null", list.get(2));
    }
    
    @SuppressWarnings("rawtypes")
    static String kind(Try notification) {
        if (notification.hasError()) {
            return "OnError";
        }
        if (((Optional<?>)notification.value()).isPresent()) {
            return "OnNext";
        }
        return "OnComplete";
    }
    
    @SuppressWarnings("rawtypes")
    static String value(Try notification) {
        Optional<?> optional = (Optional<?>)notification.value();
        if (optional.isPresent()) {
            return String.valueOf(optional.get());
        }
        return "null";
    }

    @Test
    public void testEmitMaterializedNotifications() {
        NbpObservable<Try<Optional<Integer>>> oi = NbpObservable.just(1, 2, 3).materialize();
        NbpObservable<Try<Optional<String>>> os = NbpObservable.just("a", "b", "c").materialize();
        NbpObservable<String> o = NbpObservable.zip(oi, os, new BiFunction<Try<Optional<Integer>>, Try<Optional<String>>, String>() {

            @Override
            public String apply(Try<Optional<Integer>> t1, Try<Optional<String>> t2) {
                return kind(t1) + "_" + value(t1) + "-" + kind(t2) + "_" + value(t2);
            }

        });

        final ArrayList<String> list = new ArrayList<>();
        o.subscribe(new Consumer<String>() {

            @Override
            public void accept(String s) {
                System.out.println(s);
                list.add(s);
            }
        });

        assertEquals(4, list.size());
        assertEquals("OnNext_1-OnNext_a", list.get(0));
        assertEquals("OnNext_2-OnNext_b", list.get(1));
        assertEquals("OnNext_3-OnNext_c", list.get(2));
        assertEquals("OnComplete_null-OnComplete_null", list.get(3));
    }

    @Test
    public void testStartEmptyObservables() {

        NbpObservable<String> o = NbpObservable.zip(NbpObservable.<Integer> empty(), NbpObservable.<String> empty(), new BiFunction<Integer, String, String>() {

            @Override
            public String apply(Integer t1, String t2) {
                return t1 + "-" + t2;
            }

        });

        final ArrayList<String> list = new ArrayList<>();
        o.subscribe(new Consumer<String>() {

            @Override
            public void accept(String s) {
                System.out.println(s);
                list.add(s);
            }
        });

        assertEquals(0, list.size());
    }

    @Test
    public void testStartEmptyList() {

        final Object invoked = new Object();
        Collection<NbpObservable<Object>> observables = Collections.emptyList();

        NbpObservable<Object> o = NbpObservable.zip(observables, new Function<Object[], Object>() {
            @Override
            public Object apply(final Object[] args) {
                assertEquals("No argument should have been passed", 0, args.length);
                return invoked;
            }
        });

        NbpTestSubscriber<Object> ts = new NbpTestSubscriber<>();
        o.subscribe(ts);
        ts.awaitTerminalEvent(200, TimeUnit.MILLISECONDS);
        ts.assertNoValues();
    }

    /**
     * Expect NoSuchElementException instead of blocking forever as zip should emit onCompleted and no onNext
     * and last() expects at least a single response.
     */
    @Test(expected = NoSuchElementException.class)
    public void testStartEmptyListBlocking() {

        final Object invoked = new Object();
        Collection<NbpObservable<Object>> observables = Collections.emptyList();

        NbpObservable<Object> o = NbpObservable.zip(observables, new Function<Object[], Object>() {
            @Override
            public Object apply(final Object[] args) {
                assertEquals("No argument should have been passed", 0, args.length);
                return invoked;
            }
        });

        o.toBlocking().last();
    }

    @Test
    public void testDownstreamBackpressureRequestsWithFiniteSyncObservables() {
        AtomicInteger generatedA = new AtomicInteger();
        AtomicInteger generatedB = new AtomicInteger();
        NbpObservable<Integer> o1 = createInfiniteObservable(generatedA).take(Observable.bufferSize() * 2);
        NbpObservable<Integer> o2 = createInfiniteObservable(generatedB).take(Observable.bufferSize() * 2);

        NbpTestSubscriber<String> ts = new NbpTestSubscriber<>();
        NbpObservable.zip(o1, o2, new BiFunction<Integer, Integer, String>() {

            @Override
            public String apply(Integer t1, Integer t2) {
                return t1 + "-" + t2;
            }

        }).observeOn(Schedulers.computation()).take(Observable.bufferSize() * 2).subscribe(ts);

        ts.awaitTerminalEvent();
        ts.assertNoErrors();
        assertEquals(Observable.bufferSize() * 2, ts.valueCount());
        System.out.println("Generated => A: " + generatedA.get() + " B: " + generatedB.get());
        assertTrue(generatedA.get() < (Observable.bufferSize() * 3));
        assertTrue(generatedB.get() < (Observable.bufferSize() * 3));
    }

    private NbpObservable<Integer> createInfiniteObservable(final AtomicInteger generated) {
        NbpObservable<Integer> o = NbpObservable.fromIterable(new Iterable<Integer>() {
            @Override
            public Iterator<Integer> iterator() {
                return new Iterator<Integer>() {

                    @Override
                    public void remove() {
                    }

                    @Override
                    public Integer next() {
                        return generated.getAndIncrement();
                    }

                    @Override
                    public boolean hasNext() {
                        return true;
                    }
                };
            }
        });
        return o;
    }

    NbpObservable<Integer> OBSERVABLE_OF_5_INTEGERS = OBSERVABLE_OF_5_INTEGERS(new AtomicInteger());

    NbpObservable<Integer> OBSERVABLE_OF_5_INTEGERS(final AtomicInteger numEmitted) {
        return NbpObservable.create(new NbpOnSubscribe<Integer>() {

            @Override
            public void accept(final NbpSubscriber<? super Integer> o) {
                BooleanDisposable bs = new BooleanDisposable();
                o.onSubscribe(bs);
                for (int i = 1; i <= 5; i++) {
                    if (bs.isDisposed()) {
                        break;
                    }
                    numEmitted.incrementAndGet();
                    o.onNext(i);
                    Thread.yield();
                }
                o.onComplete();
            }

        });
    }

    NbpObservable<Integer> ASYNC_OBSERVABLE_OF_INFINITE_INTEGERS(final CountDownLatch latch) {
        return NbpObservable.create(new NbpOnSubscribe<Integer>() {

            @Override
            public void accept(final NbpSubscriber<? super Integer> o) {
                BooleanDisposable bs = new BooleanDisposable();
                o.onSubscribe(bs);
                Thread t = new Thread(new Runnable() {

                    @Override
                    public void run() {
                        System.out.println("-------> subscribe to infinite sequence");
                        System.out.println("Starting thread: " + Thread.currentThread());
                        int i = 1;
                        while (!bs.isDisposed()) {
                            o.onNext(i++);
                            Thread.yield();
                        }
                        o.onComplete();
                        latch.countDown();
                        System.out.println("Ending thread: " + Thread.currentThread());
                    }
                });
                t.start();

            }

        });
    }

    @Test(timeout = 30000)
    public void testIssue1812() {
        // https://github.com/ReactiveX/RxJava/issues/1812
        NbpObservable<Integer> zip1 = NbpObservable.zip(NbpObservable.range(0, 1026), NbpObservable.range(0, 1026),
                new BiFunction<Integer, Integer, Integer>() {

                    @Override
                    public Integer apply(Integer i1, Integer i2) {
                        return i1 + i2;
                    }
                });
        NbpObservable<Integer> zip2 = NbpObservable.zip(zip1, NbpObservable.range(0, 1026),
                new BiFunction<Integer, Integer, Integer>() {

                    @Override
                    public Integer apply(Integer i1, Integer i2) {
                        return i1 + i2;
                    }
                });
        List<Integer> expected = new ArrayList<>();
        for (int i = 0; i < 1026; i++) {
            expected.add(i * 3);
        }
        assertEquals(expected, zip2.toList().toBlocking().single());
    }

    @Test(timeout = 10000)
    public void testZipRace() {
        long startTime = System.currentTimeMillis();
        NbpObservable<Integer> src = NbpObservable.just(1).subscribeOn(Schedulers.computation());
        
        // now try and generate a hang by zipping src with itself repeatedly. A
        // time limit of 9 seconds ( 1 second less than the test timeout) is
        // used so that this test will not timeout on slow machines.
        int i = 0;
        while (System.currentTimeMillis()-startTime < 9000 && i++ < 100000) {
            int value = NbpObservable.zip(src, src, new BiFunction<Integer, Integer, Integer>() {
                @Override
                public Integer apply(Integer t1, Integer t2) {
                    return t1 + t2 * 10;
                }
            }).toBlocking().single(0);
            
            Assert.assertEquals(11, value);
        }
    }
}