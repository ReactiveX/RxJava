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

package io.reactivex.internal.operators.flowable;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.*;
import org.mockito.InOrder;
import org.reactivestreams.*;

import io.reactivex.*;
import io.reactivex.exceptions.*;
import io.reactivex.functions.*;
import io.reactivex.internal.functions.Functions;
import io.reactivex.internal.fuseable.QueueSubscription;
import io.reactivex.internal.subscriptions.BooleanSubscription;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.processors.PublishProcessor;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subscribers.*;

public class FlowableZipTest {
    BiFunction<String, String, String> concat2Strings;
    PublishProcessor<String> s1;
    PublishProcessor<String> s2;
    Flowable<String> zipped;

    Subscriber<String> observer;
    InOrder inOrder;

    @Before
    public void setUp() {
        concat2Strings = new BiFunction<String, String, String>() {
            @Override
            public String apply(String t1, String t2) {
                return t1 + "-" + t2;
            }
        };

        s1 = PublishProcessor.create();
        s2 = PublishProcessor.create();
        zipped = Flowable.zip(s1, s2, concat2Strings);

        observer = TestHelper.mockSubscriber();
        inOrder = inOrder(observer);

        zipped.subscribe(observer);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCollectionSizeDifferentThanFunction() {
        Function<Object[], String> zipr = Functions.toFunction(getConcatStringIntegerIntArrayZipr());
        //Function3<String, Integer, int[], String>

        /* define a Subscriber to receive aggregated events */
        Subscriber<String> observer = TestHelper.mockSubscriber();

        @SuppressWarnings("rawtypes")
        Collection ws = java.util.Collections.singleton(Flowable.just("one", "two"));
        Flowable<String> w = Flowable.zip(ws, zipr);
        w.subscribe(observer);

        verify(observer, times(1)).onError(any(Throwable.class));
        verify(observer, never()).onComplete();
        verify(observer, never()).onNext(any(String.class));
    }

    @Test
    public void testStartpingDifferentLengthFlowableSequences1() {
        Subscriber<String> w = TestHelper.mockSubscriber();

        TestFlowable w1 = new TestFlowable();
        TestFlowable w2 = new TestFlowable();
        TestFlowable w3 = new TestFlowable();

        Flowable<String> zipW = Flowable.zip(
                Flowable.unsafeCreate(w1), Flowable.unsafeCreate(w2),
                Flowable.unsafeCreate(w3), getConcat3StringsZipr());
        zipW.subscribe(w);

        /* simulate sending data */
        // once for w1
        w1.observer.onNext("1a");
        w1.observer.onComplete();
        // twice for w2
        w2.observer.onNext("2a");
        w2.observer.onNext("2b");
        w2.observer.onComplete();
        // 4 times for w3
        w3.observer.onNext("3a");
        w3.observer.onNext("3b");
        w3.observer.onNext("3c");
        w3.observer.onNext("3d");
        w3.observer.onComplete();

        /* we should have been called 1 time on the Subscriber */
        InOrder io = inOrder(w);
        io.verify(w).onNext("1a2a3a");

        io.verify(w, times(1)).onComplete();
    }

    @Test
    public void testStartpingDifferentLengthFlowableSequences2() {
        Subscriber<String> w = TestHelper.mockSubscriber();

        TestFlowable w1 = new TestFlowable();
        TestFlowable w2 = new TestFlowable();
        TestFlowable w3 = new TestFlowable();

        Flowable<String> zipW = Flowable.zip(Flowable.unsafeCreate(w1), Flowable.unsafeCreate(w2), Flowable.unsafeCreate(w3), getConcat3StringsZipr());
        zipW.subscribe(w);

        /* simulate sending data */
        // 4 times for w1
        w1.observer.onNext("1a");
        w1.observer.onNext("1b");
        w1.observer.onNext("1c");
        w1.observer.onNext("1d");
        w1.observer.onComplete();
        // twice for w2
        w2.observer.onNext("2a");
        w2.observer.onNext("2b");
        w2.observer.onComplete();
        // 1 times for w3
        w3.observer.onNext("3a");
        w3.observer.onComplete();

        /* we should have been called 1 time on the Subscriber */
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
        PublishProcessor<String> r1 = PublishProcessor.create();
        PublishProcessor<String> r2 = PublishProcessor.create();
        /* define a Subscriber to receive aggregated events */
        Subscriber<String> observer = TestHelper.mockSubscriber();

        Flowable.zip(r1, r2, zipr2).subscribe(observer);

        /* simulate the Flowables pushing data into the aggregator */
        r1.onNext("hello");
        r2.onNext("world");

        InOrder inOrder = inOrder(observer);

        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, never()).onComplete();
        inOrder.verify(observer, times(1)).onNext("helloworld");

        r1.onNext("hello ");
        r2.onNext("again");

        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, never()).onComplete();
        inOrder.verify(observer, times(1)).onNext("hello again");

        r1.onComplete();
        r2.onComplete();

        inOrder.verify(observer, never()).onNext(anyString());
        verify(observer, times(1)).onComplete();
    }

    @Test
    public void testAggregatorDifferentSizedResultsWithOnComplete() {
        /* create the aggregator which will execute the zip function when all Flowables provide values */
        /* define a Subscriber to receive aggregated events */
        PublishProcessor<String> r1 = PublishProcessor.create();
        PublishProcessor<String> r2 = PublishProcessor.create();
        /* define a Subscriber to receive aggregated events */
        Subscriber<String> observer = TestHelper.mockSubscriber();
        Flowable.zip(r1, r2, zipr2).subscribe(observer);

        /* simulate the Flowables pushing data into the aggregator */
        r1.onNext("hello");
        r2.onNext("world");
        r2.onComplete();

        InOrder inOrder = inOrder(observer);

        inOrder.verify(observer, never()).onError(any(Throwable.class));
        inOrder.verify(observer, times(1)).onNext("helloworld");
        inOrder.verify(observer, times(1)).onComplete();

        r1.onNext("hi");
        r1.onComplete();

        inOrder.verify(observer, never()).onError(any(Throwable.class));
        inOrder.verify(observer, never()).onComplete();
        inOrder.verify(observer, never()).onNext(anyString());
    }

    @Test
    public void testAggregateMultipleTypes() {
        PublishProcessor<String> r1 = PublishProcessor.create();
        PublishProcessor<Integer> r2 = PublishProcessor.create();
        /* define a Subscriber to receive aggregated events */
        Subscriber<String> observer = TestHelper.mockSubscriber();

        Flowable.zip(r1, r2, zipr2).subscribe(observer);

        /* simulate the Flowables pushing data into the aggregator */
        r1.onNext("hello");
        r2.onNext(1);
        r2.onComplete();

        InOrder inOrder = inOrder(observer);

        inOrder.verify(observer, never()).onError(any(Throwable.class));
        inOrder.verify(observer, times(1)).onNext("hello1");
        inOrder.verify(observer, times(1)).onComplete();

        r1.onNext("hi");
        r1.onComplete();

        inOrder.verify(observer, never()).onError(any(Throwable.class));
        inOrder.verify(observer, never()).onComplete();
        inOrder.verify(observer, never()).onNext(anyString());
    }

    @Test
    public void testAggregate3Types() {
        PublishProcessor<String> r1 = PublishProcessor.create();
        PublishProcessor<Integer> r2 = PublishProcessor.create();
        PublishProcessor<List<Integer>> r3 = PublishProcessor.create();
        /* define a Subscriber to receive aggregated events */
        Subscriber<String> observer = TestHelper.mockSubscriber();

        Flowable.zip(r1, r2, r3, zipr3).subscribe(observer);

        /* simulate the Flowables pushing data into the aggregator */
        r1.onNext("hello");
        r2.onNext(2);
        r3.onNext(Arrays.asList(5, 6, 7));

        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, never()).onComplete();
        verify(observer, times(1)).onNext("hello2[5, 6, 7]");
    }

    @Test
    public void testAggregatorsWithDifferentSizesAndTiming() {
        PublishProcessor<String> r1 = PublishProcessor.create();
        PublishProcessor<String> r2 = PublishProcessor.create();
        /* define a Subscriber to receive aggregated events */
        Subscriber<String> observer = TestHelper.mockSubscriber();

        Flowable.zip(r1, r2, zipr2).subscribe(observer);

        /* simulate the Flowables pushing data into the aggregator */
        r1.onNext("one");
        r1.onNext("two");
        r1.onNext("three");
        r2.onNext("A");

        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, never()).onComplete();
        verify(observer, times(1)).onNext("oneA");

        r1.onNext("four");
        r1.onComplete();
        r2.onNext("B");
        verify(observer, times(1)).onNext("twoB");
        r2.onNext("C");
        verify(observer, times(1)).onNext("threeC");
        r2.onNext("D");
        verify(observer, times(1)).onNext("fourD");
        r2.onNext("E");
        verify(observer, never()).onNext("E");
        r2.onComplete();

        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onComplete();
    }

    @Test
    public void testAggregatorError() {
        PublishProcessor<String> r1 = PublishProcessor.create();
        PublishProcessor<String> r2 = PublishProcessor.create();
        /* define a Subscriber to receive aggregated events */
        Subscriber<String> observer = TestHelper.mockSubscriber();

        Flowable.zip(r1, r2, zipr2).subscribe(observer);

        /* simulate the Flowables pushing data into the aggregator */
        r1.onNext("hello");
        r2.onNext("world");

        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, never()).onComplete();
        verify(observer, times(1)).onNext("helloworld");

        r1.onError(new RuntimeException(""));
        r1.onNext("hello");
        r2.onNext("again");

        verify(observer, times(1)).onError(any(Throwable.class));
        verify(observer, never()).onComplete();
        // we don't want to be called again after an error
        verify(observer, times(0)).onNext("helloagain");
    }

    @Test
    public void testAggregatorUnsubscribe() {
        PublishProcessor<String> r1 = PublishProcessor.create();
        PublishProcessor<String> r2 = PublishProcessor.create();
        /* define a Subscriber to receive aggregated events */
        Subscriber<String> observer = TestHelper.mockSubscriber();
        TestSubscriber<String> ts = new TestSubscriber<String>(observer);

        Flowable.zip(r1, r2, zipr2).subscribe(ts);

        /* simulate the Flowables pushing data into the aggregator */
        r1.onNext("hello");
        r2.onNext("world");

        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, never()).onComplete();
        verify(observer, times(1)).onNext("helloworld");

        ts.dispose();
        r1.onNext("hello");
        r2.onNext("again");

        verify(observer, times(0)).onError(any(Throwable.class));
        verify(observer, never()).onComplete();
        // we don't want to be called again after an error
        verify(observer, times(0)).onNext("helloagain");
    }

    @Test
    public void testAggregatorEarlyCompletion() {
        PublishProcessor<String> r1 = PublishProcessor.create();
        PublishProcessor<String> r2 = PublishProcessor.create();
        /* define a Subscriber to receive aggregated events */
        Subscriber<String> observer = TestHelper.mockSubscriber();

        Flowable.zip(r1, r2, zipr2).subscribe(observer);

        /* simulate the Flowables pushing data into the aggregator */
        r1.onNext("one");
        r1.onNext("two");
        r1.onComplete();
        r2.onNext("A");

        InOrder inOrder = inOrder(observer);

        inOrder.verify(observer, never()).onError(any(Throwable.class));
        inOrder.verify(observer, never()).onComplete();
        inOrder.verify(observer, times(1)).onNext("oneA");

        r2.onComplete();

        inOrder.verify(observer, never()).onError(any(Throwable.class));
        inOrder.verify(observer, times(1)).onComplete();
        inOrder.verify(observer, never()).onNext(anyString());
    }

    @Test
    public void testStart2Types() {
        BiFunction<String, Integer, String> zipr = getConcatStringIntegerZipr();

        /* define a Subscriber to receive aggregated events */
        Subscriber<String> observer = TestHelper.mockSubscriber();

        Flowable<String> w = Flowable.zip(Flowable.just("one", "two"), Flowable.just(2, 3, 4), zipr);
        w.subscribe(observer);

        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onComplete();
        verify(observer, times(1)).onNext("one2");
        verify(observer, times(1)).onNext("two3");
        verify(observer, never()).onNext("4");
    }

    @Test
    public void testStart3Types() {
        Function3<String, Integer, int[], String> zipr = getConcatStringIntegerIntArrayZipr();

        /* define a Subscriber to receive aggregated events */
        Subscriber<String> observer = TestHelper.mockSubscriber();

        Flowable<String> w = Flowable.zip(Flowable.just("one", "two"), Flowable.just(2), Flowable.just(new int[] { 4, 5, 6 }), zipr);
        w.subscribe(observer);

        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onComplete();
        verify(observer, times(1)).onNext("one2[4, 5, 6]");
        verify(observer, never()).onNext("two");
    }

    @Test
    public void testOnNextExceptionInvokesOnError() {
        BiFunction<Integer, Integer, Integer> zipr = getDivideZipr();

        Subscriber<Integer> observer = TestHelper.mockSubscriber();

        Flowable<Integer> w = Flowable.zip(Flowable.just(10, 20, 30), Flowable.just(0, 1, 2), zipr);
        w.subscribe(observer);

        verify(observer, times(1)).onError(any(Throwable.class));
    }

    @Test
    public void testOnFirstCompletion() {
        PublishProcessor<String> oA = PublishProcessor.create();
        PublishProcessor<String> oB = PublishProcessor.create();

        Subscriber<String> obs = TestHelper.mockSubscriber();

        Flowable<String> o = Flowable.zip(oA, oB, getConcat2Strings());
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
        PublishProcessor<String> oA = PublishProcessor.create();
        PublishProcessor<String> oB = PublishProcessor.create();

        Subscriber<String> obs = TestHelper.mockSubscriber();

        Flowable<String> o = Flowable.zip(oA, oB, getConcat2Strings());
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

    private static class TestFlowable implements Publisher<String> {

        Subscriber<? super String> observer;

        @Override
        public void subscribe(Subscriber<? super String> observer) {
            // just store the variable where it can be accessed so we can manually trigger it
            this.observer = observer;
            observer.onSubscribe(new BooleanSubscription());
        }

    }

    @Test
    public void testFirstCompletesThenSecondInfinite() {
        s1.onNext("a");
        s1.onNext("b");
        s1.onComplete();
        s2.onNext("1");
        inOrder.verify(observer, times(1)).onNext("a-1");
        s2.onNext("2");
        inOrder.verify(observer, times(1)).onNext("b-2");
        inOrder.verify(observer, times(1)).onComplete();
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
        s1.onComplete();
        inOrder.verify(observer, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testSecondCompletesThenFirstInfinite() {
        s2.onNext("1");
        s2.onNext("2");
        s2.onComplete();
        s1.onNext("a");
        inOrder.verify(observer, times(1)).onNext("a-1");
        s1.onNext("b");
        inOrder.verify(observer, times(1)).onNext("b-2");
        inOrder.verify(observer, times(1)).onComplete();
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
        s2.onComplete();
        inOrder.verify(observer, times(1)).onComplete();
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

        inOrder.verify(observer, never()).onComplete();
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

        inOrder.verify(observer, never()).onComplete();
        inOrder.verify(observer, never()).onNext(any(String.class));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testStartWithOnCompletedTwice() {
        // issue: https://groups.google.com/forum/#!topic/rxjava/79cWTv3TFp0
        // The problem is the original "zip" implementation does not wrap
        // an internal observer with a SafeSubscriber. However, in the "zip",
        // it may calls "onComplete" twice. That breaks the Rx contract.

        // This test tries to emulate this case.
        // As "TestHelper.mockSubscriber()" will create an instance in the package "rx",
        // we need to wrap "TestHelper.mockSubscriber()" with an observer instance
        // which is in the package "rx.operators".
        final Subscriber<Integer> observer = TestHelper.mockSubscriber();

        Flowable.zip(Flowable.just(1),
                Flowable.just(1), new BiFunction<Integer, Integer, Integer>() {
                    @Override
                    public Integer apply(Integer a, Integer b) {
                        return a + b;
                    }
                }).subscribe(new DefaultSubscriber<Integer>() {

            @Override
            public void onComplete() {
                observer.onComplete();
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
        inOrder.verify(observer, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testStart() {
        Flowable<String> os = OBSERVABLE_OF_5_INTEGERS
                .zipWith(OBSERVABLE_OF_5_INTEGERS, new BiFunction<Integer, Integer, String>() {

                    @Override
                    public String apply(Integer a, Integer b) {
                        return a + "-" + b;
                    }
                });

        final ArrayList<String> list = new ArrayList<String>();
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
        Flowable<String> os = ASYNC_OBSERVABLE_OF_INFINITE_INTEGERS(new CountDownLatch(1)).onBackpressureBuffer()
                .zipWith(ASYNC_OBSERVABLE_OF_INFINITE_INTEGERS(new CountDownLatch(1)).onBackpressureBuffer(), new BiFunction<Integer, Integer, String>() {

                    @Override
                    public String apply(Integer a, Integer b) {
                        return a + "-" + b;
                    }
                }).take(5);

        TestSubscriber<String> ts = new TestSubscriber<String>();
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
        final CountDownLatch infiniteFlowable = new CountDownLatch(1);
        Flowable<String> os = OBSERVABLE_OF_5_INTEGERS
                .zipWith(ASYNC_OBSERVABLE_OF_INFINITE_INTEGERS(infiniteFlowable), new BiFunction<Integer, Integer, String>() {

                    @Override
                    public String apply(Integer a, Integer b) {
                        return a + "-" + b;
                    }
                });

        final ArrayList<String> list = new ArrayList<String>();
        os.subscribe(new DefaultSubscriber<String>() {

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
        if (!infiniteFlowable.await(2000, TimeUnit.MILLISECONDS)) {
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
        Flowable<Integer> oi = Flowable.just(1, null, 3);
        Flowable<String> os = Flowable.just("a", "b", null);
        Flowable<String> o = Flowable.zip(oi, os, new BiFunction<Integer, String, String>() {

            @Override
            public String apply(Integer t1, String t2) {
                return t1 + "-" + t2;
            }

        });

        final ArrayList<String> list = new ArrayList<String>();
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
    static String kind(Notification notification) {
        if (notification.isOnError()) {
            return "OnError";
        }
        if (notification.isOnNext()) {
            return "OnNext";
        }
        return "OnComplete";
    }

    @SuppressWarnings("rawtypes")
    static String value(Notification notification) {
        if (notification.isOnNext()) {
            return String.valueOf(notification.getValue());
        }
        return "null";
    }

    @Test
    public void testEmitMaterializedNotifications() {
        Flowable<Notification<Integer>> oi = Flowable.just(1, 2, 3).materialize();
        Flowable<Notification<String>> os = Flowable.just("a", "b", "c").materialize();
        Flowable<String> o = Flowable.zip(oi, os, new BiFunction<Notification<Integer>, Notification<String>, String>() {

            @Override
            public String apply(Notification<Integer> t1, Notification<String> t2) {
                return kind(t1) + "_" + value(t1) + "-" + kind(t2) + "_" + value(t2);
            }

        });

        final ArrayList<String> list = new ArrayList<String>();
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
    public void testStartEmptyFlowables() {

        Flowable<String> o = Flowable.zip(Flowable.<Integer> empty(), Flowable.<String> empty(), new BiFunction<Integer, String, String>() {

            @Override
            public String apply(Integer t1, String t2) {
                return t1 + "-" + t2;
            }

        });

        final ArrayList<String> list = new ArrayList<String>();
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
        Collection<Flowable<Object>> observables = Collections.emptyList();

        Flowable<Object> o = Flowable.zip(observables, new Function<Object[], Object>() {
            @Override
            public Object apply(final Object[] args) {
                assertEquals("No argument should have been passed", 0, args.length);
                return invoked;
            }
        });

        TestSubscriber<Object> ts = new TestSubscriber<Object>();
        o.subscribe(ts);
        ts.awaitTerminalEvent(200, TimeUnit.MILLISECONDS);
        ts.assertNoValues();
    }

    /**
     * Expect NoSuchElementException instead of blocking forever as zip should emit onComplete and no onNext
     * and last() expects at least a single response.
     */
    @Test(expected = NoSuchElementException.class)
    public void testStartEmptyListBlocking() {

        final Object invoked = new Object();
        Collection<Flowable<Object>> observables = Collections.emptyList();

        Flowable<Object> o = Flowable.zip(observables, new Function<Object[], Object>() {
            @Override
            public Object apply(final Object[] args) {
                assertEquals("No argument should have been passed", 0, args.length);
                return invoked;
            }
        });

        o.blockingLast();
    }

    @Test
    public void testBackpressureSync() {
        AtomicInteger generatedA = new AtomicInteger();
        AtomicInteger generatedB = new AtomicInteger();
        Flowable<Integer> o1 = createInfiniteFlowable(generatedA);
        Flowable<Integer> o2 = createInfiniteFlowable(generatedB);

        TestSubscriber<String> ts = new TestSubscriber<String>();
        Flowable.zip(o1, o2, new BiFunction<Integer, Integer, String>() {

            @Override
            public String apply(Integer t1, Integer t2) {
                return t1 + "-" + t2;
            }

        }).take(Flowable.bufferSize() * 2).subscribe(ts);

        ts.awaitTerminalEvent();
        ts.assertNoErrors();
        assertEquals(Flowable.bufferSize() * 2, ts.valueCount());
        assertTrue(generatedA.get() < (Flowable.bufferSize() * 3));
        assertTrue(generatedB.get() < (Flowable.bufferSize() * 3));
    }

    @Test
    public void testBackpressureAsync() {
        AtomicInteger generatedA = new AtomicInteger();
        AtomicInteger generatedB = new AtomicInteger();
        Flowable<Integer> o1 = createInfiniteFlowable(generatedA).subscribeOn(Schedulers.computation());
        Flowable<Integer> o2 = createInfiniteFlowable(generatedB).subscribeOn(Schedulers.computation());

        TestSubscriber<String> ts = new TestSubscriber<String>();
        Flowable.zip(o1, o2, new BiFunction<Integer, Integer, String>() {

            @Override
            public String apply(Integer t1, Integer t2) {
                return t1 + "-" + t2;
            }

        }).take(Flowable.bufferSize() * 2).subscribe(ts);

        ts.awaitTerminalEvent();
        ts.assertNoErrors();
        assertEquals(Flowable.bufferSize() * 2, ts.valueCount());
        assertTrue(generatedA.get() < (Flowable.bufferSize() * 3));
        assertTrue(generatedB.get() < (Flowable.bufferSize() * 3));
    }

    @Test
    public void testDownstreamBackpressureRequestsWithFiniteSyncFlowables() {
        AtomicInteger generatedA = new AtomicInteger();
        AtomicInteger generatedB = new AtomicInteger();
        Flowable<Integer> o1 = createInfiniteFlowable(generatedA).take(Flowable.bufferSize() * 2);
        Flowable<Integer> o2 = createInfiniteFlowable(generatedB).take(Flowable.bufferSize() * 2);

        TestSubscriber<String> ts = new TestSubscriber<String>();
        Flowable.zip(o1, o2, new BiFunction<Integer, Integer, String>() {

            @Override
            public String apply(Integer t1, Integer t2) {
                return t1 + "-" + t2;
            }

        }).observeOn(Schedulers.computation()).take(Flowable.bufferSize() * 2).subscribe(ts);

        ts.awaitTerminalEvent();
        ts.assertNoErrors();
        assertEquals(Flowable.bufferSize() * 2, ts.valueCount());
        System.out.println("Generated => A: " + generatedA.get() + " B: " + generatedB.get());
        assertTrue(generatedA.get() < (Flowable.bufferSize() * 3));
        assertTrue(generatedB.get() < (Flowable.bufferSize() * 3));
    }

    @Test
    public void testDownstreamBackpressureRequestsWithInfiniteAsyncFlowables() {
        AtomicInteger generatedA = new AtomicInteger();
        AtomicInteger generatedB = new AtomicInteger();
        Flowable<Integer> o1 = createInfiniteFlowable(generatedA).subscribeOn(Schedulers.computation());
        Flowable<Integer> o2 = createInfiniteFlowable(generatedB).subscribeOn(Schedulers.computation());

        TestSubscriber<String> ts = new TestSubscriber<String>();
        Flowable.zip(o1, o2, new BiFunction<Integer, Integer, String>() {

            @Override
            public String apply(Integer t1, Integer t2) {
                return t1 + "-" + t2;
            }

        }).observeOn(Schedulers.computation()).take(Flowable.bufferSize() * 2).subscribe(ts);

        ts.awaitTerminalEvent();
        ts.assertNoErrors();
        assertEquals(Flowable.bufferSize() * 2, ts.valueCount());
        System.out.println("Generated => A: " + generatedA.get() + " B: " + generatedB.get());
        assertTrue(generatedA.get() < (Flowable.bufferSize() * 4));
        assertTrue(generatedB.get() < (Flowable.bufferSize() * 4));
    }

    @Test
    public void testDownstreamBackpressureRequestsWithInfiniteSyncFlowables() {
        AtomicInteger generatedA = new AtomicInteger();
        AtomicInteger generatedB = new AtomicInteger();
        Flowable<Integer> o1 = createInfiniteFlowable(generatedA);
        Flowable<Integer> o2 = createInfiniteFlowable(generatedB);

        TestSubscriber<String> ts = new TestSubscriber<String>();
        Flowable.zip(o1, o2, new BiFunction<Integer, Integer, String>() {

            @Override
            public String apply(Integer t1, Integer t2) {
                return t1 + "-" + t2;
            }

        }).observeOn(Schedulers.computation()).take(Flowable.bufferSize() * 2).subscribe(ts);

        ts.awaitTerminalEvent();
        ts.assertNoErrors();
        assertEquals(Flowable.bufferSize() * 2, ts.valueCount());
        System.out.println("Generated => A: " + generatedA.get() + " B: " + generatedB.get());
        assertTrue(generatedA.get() < (Flowable.bufferSize() * 4));
        assertTrue(generatedB.get() < (Flowable.bufferSize() * 4));
    }

    private Flowable<Integer> createInfiniteFlowable(final AtomicInteger generated) {
        Flowable<Integer> observable = Flowable.fromIterable(new Iterable<Integer>() {
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
        return observable;
    }

    Flowable<Integer> OBSERVABLE_OF_5_INTEGERS = OBSERVABLE_OF_5_INTEGERS(new AtomicInteger());

    Flowable<Integer> OBSERVABLE_OF_5_INTEGERS(final AtomicInteger numEmitted) {
        return Flowable.unsafeCreate(new Publisher<Integer>() {

            @Override
            public void subscribe(final Subscriber<? super Integer> o) {
                BooleanSubscription bs = new BooleanSubscription();
                o.onSubscribe(bs);
                for (int i = 1; i <= 5; i++) {
                    if (bs.isCancelled()) {
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

    Flowable<Integer> ASYNC_OBSERVABLE_OF_INFINITE_INTEGERS(final CountDownLatch latch) {
        return Flowable.unsafeCreate(new Publisher<Integer>() {

            @Override
            public void subscribe(final Subscriber<? super Integer> o) {
                final BooleanSubscription bs = new BooleanSubscription();
                o.onSubscribe(bs);
                Thread t = new Thread(new Runnable() {

                    @Override
                    public void run() {
                        System.out.println("-------> subscribe to infinite sequence");
                        System.out.println("Starting thread: " + Thread.currentThread());
                        int i = 1;
                        while (!bs.isCancelled()) {
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
        Flowable<Integer> zip1 = Flowable.zip(Flowable.range(0, 1026), Flowable.range(0, 1026),
                new BiFunction<Integer, Integer, Integer>() {

                    @Override
                    public Integer apply(Integer i1, Integer i2) {
                        return i1 + i2;
                    }
                });
        Flowable<Integer> zip2 = Flowable.zip(zip1, Flowable.range(0, 1026),
                new BiFunction<Integer, Integer, Integer>() {

                    @Override
                    public Integer apply(Integer i1, Integer i2) {
                        return i1 + i2;
                    }
                });
        List<Integer> expected = new ArrayList<Integer>();
        for (int i = 0; i < 1026; i++) {
            expected.add(i * 3);
        }
        assertEquals(expected, zip2.toList().blockingGet());
    }
    @Test
    public void testUnboundedDownstreamOverrequesting() {
        Flowable<Integer> source = Flowable.range(1, 2).zipWith(Flowable.range(1, 2), new BiFunction<Integer, Integer, Integer>() {
            @Override
            public Integer apply(Integer t1, Integer t2) {
                return t1 + 10 * t2;
            }
        });

        TestSubscriber<Integer> ts = new TestSubscriber<Integer>() {
            @Override
            public void onNext(Integer t) {
                super.onNext(t);
                request(5);
            }
        };

        source.subscribe(ts);

        ts.assertNoErrors();
        ts.assertTerminated();
        ts.assertValues(11, 22);
    }
    @Test(timeout = 10000)
    public void testZipRace() {
        long startTime = System.currentTimeMillis();
        Flowable<Integer> src = Flowable.just(1).subscribeOn(Schedulers.computation());

        // now try and generate a hang by zipping src with itself repeatedly. A
        // time limit of 9 seconds ( 1 second less than the test timeout) is
        // used so that this test will not timeout on slow machines.
        int i = 0;
        while (System.currentTimeMillis() - startTime < 9000 && i++ < 100000) {
            int value = Flowable.zip(src, src, new BiFunction<Integer, Integer, Integer>() {
                @Override
                public Integer apply(Integer t1, Integer t2) {
                    return t1 + t2 * 10;
                }
            }).blockingSingle(0);

            Assert.assertEquals(11, value);
        }
    }
    /**
     * Request only a single value and don't wait for another request just
     * to emit an onComplete.
     */
    @Test
    public void testZipRequest1() {
        Flowable<Integer> src = Flowable.just(1).subscribeOn(Schedulers.computation());
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>(1L);

        Flowable.zip(src, src, new BiFunction<Integer, Integer, Integer>() {
            @Override
            public Integer apply(Integer t1, Integer t2) {
                return t1 + t2 * 10;
            }
        }).subscribe(ts);

        ts.awaitTerminalEvent(1, TimeUnit.SECONDS);
        ts.assertNoErrors();
        ts.assertValue(11);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void zipNArguments() throws Exception {
        Flowable source = Flowable.just(1);

        for (int i = 2; i < 10; i++) {
            Class<?>[] types = new Class[i + 1];
            Arrays.fill(types, Publisher.class);
            types[i] = i == 2 ? BiFunction.class : Class.forName("io.reactivex.functions.Function" + i);

            Method m = Flowable.class.getMethod("zip", types);

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
                        assertEquals("f is null", ex.getCause().getMessage());
                    }
                }
            }
        }
    }

    /**
     * Implements all Function types which return a String concatenating their inputs.
     */
    @SuppressWarnings("rawtypes")
    public enum ArgsToString implements Function, BiFunction, Function3, Function4, Function5, Function6, Function7, Function8, Function9 {
        INSTANCE;

        @Override
        public Object apply(Object t1, Object t2, Object t3, Object t4, Object t5, Object t6, Object t7, Object t8,
                Object t9) throws Exception {
            return "" + t1 + t2 + t3 + t4 + t5 + t6 + t7 + t8 + t9;
        }

        @Override
        public Object apply(Object t1, Object t2, Object t3, Object t4, Object t5, Object t6, Object t7, Object t8)
                throws Exception {
            return "" + t1 + t2 + t3 + t4 + t5 + t6 + t7 + t8;
        }

        @Override
        public Object apply(Object t1, Object t2, Object t3, Object t4, Object t5, Object t6, Object t7)
                throws Exception {
            return "" + t1 + t2 + t3 + t4 + t5 + t6 + t7;
        }

        @Override
        public Object apply(Object t1, Object t2, Object t3, Object t4, Object t5, Object t6) throws Exception {
            return "" + t1 + t2 + t3 + t4 + t5 + t6;
        }

        @Override
        public Object apply(Object t1, Object t2, Object t3, Object t4, Object t5) throws Exception {
            return "" + t1 + t2 + t3 + t4 + t5;
        }

        @Override
        public Object apply(Object t1, Object t2, Object t3, Object t4) throws Exception {
            return "" + t1 + t2 + t3 + t4;
        }

        @Override
        public Object apply(Object t1, Object t2, Object t3) throws Exception {
            return "" + t1 + t2 + t3;
        }

        @Override
        public Object apply(Object t1, Object t2) throws Exception {
            return "" + t1 + t2;
        }

        @Override
        public Object apply(Object t1) throws Exception {
            return "" + t1;
        }
    }

    @Test
    public void zip2DelayError() {
        Flowable<Integer> error1 = Flowable.error(new TestException("One"));
        Flowable<Integer> source1 = Flowable.range(1, 3).concatWith(error1);

        Flowable<Integer> error2 = Flowable.error(new TestException("Two"));
        Flowable<Integer> source2 = Flowable.range(1, 2).concatWith(error2);

        TestSubscriber<Object> ts = Flowable.zip(source1, source2, new BiFunction<Integer, Integer, Object>() {
            @Override
            public Object apply(Integer a, Integer b) throws Exception {
                return "" + a + b;
            }
        }, true)
        .test()
        .assertFailure(CompositeException.class, "11", "22");

        List<Throwable> errors = TestHelper.compositeList(ts.errors().get(0));
        TestHelper.assertError(errors, 0, TestException.class, "One");
        TestHelper.assertError(errors, 1, TestException.class, "Two");
        assertEquals(2, errors.size());
    }

    @Test
    public void zip2DelayErrorPrefetch() {
        Flowable<Integer> error1 = Flowable.error(new TestException("One"));
        Flowable<Integer> source1 = Flowable.range(1, 3).concatWith(error1);

        Flowable<Integer> error2 = Flowable.error(new TestException("Two"));
        Flowable<Integer> source2 = Flowable.range(1, 2).concatWith(error2);

        TestSubscriber<Object> ts = Flowable.zip(source1, source2, new BiFunction<Integer, Integer, Object>() {
            @Override
            public Object apply(Integer a, Integer b) throws Exception {
                return "" + a + b;
            }
        }, true, 1)
        .test()
        .assertFailure(CompositeException.class, "11", "22");

        List<Throwable> errors = TestHelper.compositeList(ts.errors().get(0));
        TestHelper.assertError(errors, 0, TestException.class, "One");
        TestHelper.assertError(errors, 1, TestException.class, "Two");
        assertEquals(2, errors.size());
    }

    @Test
    public void zip2Prefetch() {
        Flowable.zip(Flowable.range(1, 9),
                Flowable.range(21, 9),
            new BiFunction<Integer, Integer, Object>() {
                @Override
                public Object apply(Integer a, Integer b) throws Exception {
                    return "" + a + b;
                }
            }, false, 2
        )
        .takeLast(1)
        .test()
        .assertResult("929");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void zipArrayEmpty() {
        assertSame(Flowable.empty(), Flowable.zipArray(Functions.<Object[]>identity(), false, 16));
    }

    @Test
    public void zip2() {
        Flowable.zip(Flowable.just(1),
                Flowable.just(2),
            new BiFunction<Integer, Integer, Object>() {
                @Override
                public Object apply(Integer a, Integer b) throws Exception {
                    return "" + a + b;
                }
            }
        )
        .test()
        .assertResult("12");
    }

    @Test
    public void zip3() {
        Flowable.zip(Flowable.just(1),
                Flowable.just(2), Flowable.just(3),
            new Function3<Integer, Integer, Integer, Object>() {
                @Override
                public Object apply(Integer a, Integer b, Integer c) throws Exception {
                    return "" + a + b + c;
                }
            }
        )
        .test()
        .assertResult("123");
    }

    @Test
    public void zip4() {
        Flowable.zip(Flowable.just(1),
                Flowable.just(2), Flowable.just(3),
                Flowable.just(4),
            new Function4<Integer, Integer, Integer, Integer, Object>() {
                @Override
                public Object apply(Integer a, Integer b, Integer c, Integer d) throws Exception {
                    return "" + a + b + c + d;
                }
            }
        )
        .test()
        .assertResult("1234");
    }

    @Test
    public void zip5() {
        Flowable.zip(Flowable.just(1),
                Flowable.just(2), Flowable.just(3),
                Flowable.just(4), Flowable.just(5),
            new Function5<Integer, Integer, Integer, Integer, Integer, Object>() {
                @Override
                public Object apply(Integer a, Integer b, Integer c, Integer d, Integer e) throws Exception {
                    return "" + a + b + c + d + e;
                }
            }
        )
        .test()
        .assertResult("12345");
    }

    @Test
    public void zip6() {
        Flowable.zip(Flowable.just(1),
                Flowable.just(2), Flowable.just(3),
                Flowable.just(4), Flowable.just(5),
                Flowable.just(6),
            new Function6<Integer, Integer, Integer, Integer, Integer, Integer, Object>() {
                @Override
                public Object apply(Integer a, Integer b, Integer c, Integer d, Integer e, Integer f) throws Exception {
                    return "" + a + b + c + d + e + f;
                }
            }
        )
        .test()
        .assertResult("123456");
    }

    @Test
    public void zip7() {
        Flowable.zip(Flowable.just(1),
                Flowable.just(2), Flowable.just(3),
                Flowable.just(4), Flowable.just(5),
                Flowable.just(6), Flowable.just(7),
            new Function7<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Object>() {
                @Override
                public Object apply(Integer a, Integer b, Integer c, Integer d, Integer e, Integer f, Integer g)
                        throws Exception {
                    return "" + a + b + c + d + e + f + g;
                }
            }
        )
        .test()
        .assertResult("1234567");
    }

    @Test
    public void zip8() {
        Flowable.zip(Flowable.just(1),
                Flowable.just(2), Flowable.just(3),
                Flowable.just(4), Flowable.just(5),
                Flowable.just(6), Flowable.just(7),
                Flowable.just(8),
            new Function8<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer, Object>() {
                @Override
                public Object apply(Integer a, Integer b, Integer c, Integer d, Integer e, Integer f, Integer g,
                        Integer h) throws Exception {
                    return "" + a + b + c + d + e + f + g + h;
                }
            }
        )
        .test()
        .assertResult("12345678");
    }
    @Test
    public void zip9() {
        Flowable.zip(Flowable.just(1),
                Flowable.just(2), Flowable.just(3),
                Flowable.just(4), Flowable.just(5),
                Flowable.just(6), Flowable.just(7),
                Flowable.just(8), Flowable.just(9),
            new Function9<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer, Object>() {
                @Override
                public Object apply(Integer a, Integer b, Integer c, Integer d, Integer e, Integer f, Integer g,
                        Integer h, Integer i) throws Exception {
                    return "" + a + b + c + d + e + f + g + h + i;
                }
            }
        )
        .test()
        .assertResult("123456789");
    }


    @Test
    public void zipArrayMany() {
        @SuppressWarnings("unchecked")
        Flowable<Integer>[] arr = new Flowable[10];

        Arrays.fill(arr, Flowable.just(1));

        Flowable.zip(Arrays.asList(arr), new Function<Object[], Object>() {
            @Override
            public Object apply(Object[] a) throws Exception {
                return Arrays.toString(a);
            }
        })
        .test()
        .assertResult("[1, 1, 1, 1, 1, 1, 1, 1, 1, 1]");
    }

    @Test
    public void dispose() {
        TestHelper.checkDisposed(Flowable.zip(Flowable.just(1), Flowable.just(1), new BiFunction<Integer, Integer, Object>() {
            @Override
            public Object apply(Integer a, Integer b) throws Exception {
                return a + b;
            }
        }));
    }

    @Test
    public void badRequest() {
        TestHelper.assertBadRequestReported(Flowable.zip(Flowable.just(1), Flowable.just(1), new BiFunction<Integer, Integer, Object>() {
            @Override
            public Object apply(Integer a, Integer b) throws Exception {
                return a + b;
            }
        }));
    }

    @Test
    public void multiError() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            PublishProcessor<Object> pp = PublishProcessor.create();

            @SuppressWarnings("rawtypes")
            final Subscriber[] sub = { null };
            TestSubscriber<Object> ts = Flowable.zip(pp, new Flowable<Object>() {
                @Override
                protected void subscribeActual(Subscriber<? super Object> s) {
                    sub[0] = s;
                }
            }, new BiFunction<Object, Object, Object>() {
                @Override
                public Object apply(Object a, Object b) throws Exception {
                    return a;
                }
            })
            .test();

            pp.onError(new TestException("First"));

            ts
            .assertFailureAndMessage(TestException.class, "First");

            sub[0].onError(new TestException("Second"));

            TestHelper.assertUndeliverable(errors, 0, TestException.class, "Second");
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void singleErrorDelayed() {
        PublishProcessor<Object> pp1 = PublishProcessor.create();
        PublishProcessor<Object> pp2 = PublishProcessor.create();

        TestSubscriber<Object> ts = Flowable.zip(pp1, pp2, new BiFunction<Object, Object, Object>() {
            @Override
            public Object apply(Object a, Object b) throws Exception {
                return a;
            }
        }, true)
        .test();

        pp1.onError(new TestException("First"));
        pp2.onComplete();

        ts
        .assertFailureAndMessage(TestException.class, "First");
    }

    @Test
    public void singleErrorDelayedBackpressured() {
        PublishProcessor<Object> pp1 = PublishProcessor.create();
        PublishProcessor<Object> pp2 = PublishProcessor.create();

        TestSubscriber<Object> ts = Flowable.zip(pp1, pp2, new BiFunction<Object, Object, Object>() {
            @Override
            public Object apply(Object a, Object b) throws Exception {
                return a;
            }
        })
        .test(0L);

        pp1.onError(new TestException("First"));
        pp2.onComplete();

        ts
        .assertFailureAndMessage(TestException.class, "First");
    }

    @Test
    public void fusedInputThrows() {
        Flowable.zip(Flowable.just(1).map(new Function<Integer, Integer>() {
            @Override
            public Integer apply(Integer v) throws Exception {
                throw new TestException();
            }
        }), Flowable.just(2), new BiFunction<Integer, Integer, Integer>() {
            @Override
            public Integer apply(Integer a, Integer b) throws Exception {
                return a + b;
            }
        })
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void fusedInputThrowsDelayError() {
        Flowable.zip(Flowable.just(1).map(new Function<Integer, Integer>() {
            @Override
            public Integer apply(Integer v) throws Exception {
                throw new TestException();
            }
        }), Flowable.just(2), new BiFunction<Integer, Integer, Integer>() {
            @Override
            public Integer apply(Integer a, Integer b) throws Exception {
                return a + b;
            }
        }, true)
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void fusedInputThrowsBackpressured() {
        Flowable.zip(Flowable.just(1).map(new Function<Integer, Integer>() {
            @Override
            public Integer apply(Integer v) throws Exception {
                throw new TestException();
            }
        }), Flowable.just(2), new BiFunction<Integer, Integer, Integer>() {
            @Override
            public Integer apply(Integer a, Integer b) throws Exception {
                return a + b;
            }
        })
        .test(0L)
        .assertFailure(TestException.class);
    }

    @Test
    public void fusedInputThrowsDelayErrorBackpressured() {
        Flowable.zip(Flowable.just(1).map(new Function<Integer, Integer>() {
            @Override
            public Integer apply(Integer v) throws Exception {
                throw new TestException();
            }
        }), Flowable.just(2), new BiFunction<Integer, Integer, Integer>() {
            @Override
            public Integer apply(Integer a, Integer b) throws Exception {
                return a + b;
            }
        }, true)
        .test(0L)
        .assertFailure(TestException.class);
    }

    @Test
    public void noCrossBoundaryFusion() {
        for (int i = 0; i < 500; i++) {
            TestSubscriber<List<Object>> ts = Flowable.zip(
                    Flowable.just(1).observeOn(Schedulers.single()).map(new Function<Integer, Object>() {
                        @Override
                        public Object apply(Integer v) throws Exception {
                            return Thread.currentThread().getName().substring(0, 4);
                        }
                    }),
                    Flowable.just(1).observeOn(Schedulers.computation()).map(new Function<Integer, Object>() {
                        @Override
                        public Object apply(Integer v) throws Exception {
                            return Thread.currentThread().getName().substring(0, 4);
                        }
                    }),
                    new BiFunction<Object, Object, List<Object>>() {
                        @Override
                        public List<Object> apply(Object t1, Object t2) throws Exception {
                            return Arrays.asList(t1, t2);
                        }
                    }
            )
            .test()
            .awaitDone(5, TimeUnit.SECONDS)
            .assertValueCount(1);

            List<Object> list = ts.values().get(0);

            assertTrue(list.toString(), list.contains("RxSi"));
            assertTrue(list.toString(), list.contains("RxCo"));
        }
    }

    static final class ThrowingQueueSubscription implements QueueSubscription<Integer>, Publisher<Integer> {

        @Override
        public int requestFusion(int mode) {
            return mode & SYNC;
        }

        @Override
        public boolean offer(Integer value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean offer(Integer v1, Integer v2) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Integer poll() throws Exception {
            throw new TestException();
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public void clear() {
        }

        @Override
        public void request(long n) {
        }

        @Override
        public void cancel() {
        }

        @Override
        public void subscribe(Subscriber<? super Integer> s) {
            s.onSubscribe(this);
        }
    }

    @Test
    public void fusedInputThrows2() {
        Flowable.zip(new ThrowingQueueSubscription(), Flowable.just(1), new BiFunction<Integer, Integer, Integer>() {
            @Override
            public Integer apply(Integer a, Integer b) throws Exception {
                return a + b;
            }
        })
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void fusedInputThrows2Backpressured() {
        Flowable.zip(new ThrowingQueueSubscription(), Flowable.just(1), new BiFunction<Integer, Integer, Integer>() {
            @Override
            public Integer apply(Integer a, Integer b) throws Exception {
                return a + b;
            }
        })
        .test(0)
        .assertFailure(TestException.class);
    }

    @Test
    public void cancelOnBackpressureBoundary() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>(1L) {
            @Override
            public void onNext(Integer t) {
                super.onNext(t);
                cancel();
                onComplete();
            }
        };

        Flowable.zip(Flowable.range(1, 2), Flowable.range(3, 2), new BiFunction<Integer, Integer, Integer>() {
            @Override
            public Integer apply(Integer a, Integer b) throws Exception {
                return a + b;
            }
        })
        .subscribe(ts);

        ts.assertResult(4);
    }
}
