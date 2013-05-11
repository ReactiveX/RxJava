/**
 * Copyright 2013 Netflix, Inc.
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

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Matchers;

import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.subscriptions.Subscriptions;
import rx.util.AtomicObservableSubscription;
import rx.util.SynchronizedObserver;
import rx.util.functions.Func1;
import rx.util.functions.Func2;
import rx.util.functions.Func3;
import rx.util.functions.Func4;
import rx.util.functions.FuncN;
import rx.util.functions.Functions;

public class OperationCombineLatest {

    /**
     * Combines the two given observables, emitting an event containing an aggregation of the latest values of each of the source observables
     * each time an event is received from one of the source observables, where the aggregation is defined by the given function.
     * @param w0 
     *          The first source observable.
     * @param w1 
     *          The second source observable.
     * @param combineLatestFunction 
     *          The aggregation function used to combine the source observable values.
     * @return A function from an observer to a subscription. This can be used to create an observable from.
     */
    public static <T0, T1, R> Func1<Observer<? super R>, Subscription> combineLatest(Observable<? super T0> w0, Observable<? super T1> w1, Func2<? super T0, ? super T1, ? extends R> combineLatestFunction) {
        Aggregator<R> a = new Aggregator<R>(Functions.fromFunc(combineLatestFunction));
        a.addObserver(new CombineObserver<R, T0>(a, w0));
        a.addObserver(new CombineObserver<R, T1>(a, w1));
        return a;
    }

    /**
     * @see #combineLatest(Observable w0, Observable w1, Func2 combineLatestFunction)
     */
    public static <T0, T1, T2, R> Func1<Observer<? super R>, Subscription> combineLatest(Observable<? super T0> w0, Observable<? super T1> w1, Observable<? super T2> w2, Func3<? super T0, ? super T1, ? super T2, ? extends R> combineLatestFunction) {
        Aggregator<R> a = new Aggregator<R>(Functions.fromFunc(combineLatestFunction));
        a.addObserver(new CombineObserver<R, T0>(a, w0));
        a.addObserver(new CombineObserver<R, T1>(a, w1));
        a.addObserver(new CombineObserver<R, T2>(a, w2));
        return a;
    }

    /**
     * @see #combineLatest(Observable w0, Observable w1, Func2 combineLatestFunction)
     */
    public static <T0, T1, T2, T3, R> Func1<Observer<? super R>, Subscription> combineLatest(Observable<? super T0> w0, Observable<? super T1> w1, Observable<? super T2> w2, Observable<? super T3> w3, Func4<? super T0, ? super T1, ? super T2, ? super T3, ? extends R> combineLatestFunction) {
        Aggregator<R> a = new Aggregator<R>(Functions.fromFunc(combineLatestFunction));
        a.addObserver(new CombineObserver<R, T0>(a, w0));
        a.addObserver(new CombineObserver<R, T1>(a, w1));
        a.addObserver(new CombineObserver<R, T2>(a, w2));
        a.addObserver(new CombineObserver<R, T3>(a, w3));
        return a;
    }

    private static class CombineObserver<R, T> implements Observer<T> {
        final Observable<? super T> w;
        final Aggregator<? super R> a;
        private Subscription subscription;

        public CombineObserver(Aggregator<? super R> a, Observable<? super T> w) {
            this.a = a;
            this.w = w;
        }

        private void startWatching() {
            if (subscription != null) {
                throw new RuntimeException("This should only be called once.");
            }
            subscription = w.subscribe(this);
        }

        @Override
        public void onCompleted() {
            a.complete(this);
        }

        @Override
        public void onError(Exception e) {
            a.error(e);
        }

        @Override
        public void onNext(T args) {
            a.next(this, args);
        }
    }

    /**
     * Receive notifications from each of the observables we are reducing and execute the combineLatestFunction 
     * whenever we have received an event from one of the observables, as soon as each Observable has received 
     * at least one event.
     */
    private static class Aggregator<R> implements Func1<Observer<? super R>, Subscription> {

        private volatile Observer<? super R> observer;

        private final FuncN<? extends R> combineLatestFunction;
        private final AtomicBoolean running = new AtomicBoolean(true);
        
        // Stores how many observers have already completed
        private final AtomicInteger numCompleted = new AtomicInteger(0);
        
        /**
         * The latest value from each observer.
         */
        private final Map<CombineObserver<? extends R, ?>, Object> latestValue = new ConcurrentHashMap<CombineObserver<? extends R, ?>, Object>();
        
        /**
         * Ordered list of observers to combine.
         * No synchronization is necessary as these can not be added or changed asynchronously.
         */
        private final List<CombineObserver<R, ?>> observers = new LinkedList<CombineObserver<R, ?>>();

        public Aggregator(FuncN<? extends R> combineLatestFunction) {
            this.combineLatestFunction = combineLatestFunction;
        }

        /**
         * Receive notification of a Observer starting (meaning we should require it for aggregation)
         * 
         * @param w The observer to add.
         */
        <T> void addObserver(CombineObserver<R, T> w) {
            observers.add(w);
        }

        /**
         * Receive notification of a Observer completing its iterations.
         * 
         * @param w The observer that has completed.
         */
        <T> void complete(CombineObserver<? extends R, ? super T> w) {
            int completed = numCompleted.incrementAndGet();
            // if all CombineObservers are completed, we mark the whole thing as completed
            if (completed == observers.size()) {
                if (running.get()) {
                    // mark ourselves as done
                    observer.onCompleted();
                    // just to ensure we stop processing in case we receive more onNext/complete/error calls after this
                    running.set(false);
                }
            }
        }

        /**
         * Receive error for a Observer. Throw the error up the chain and stop processing.
         */
        void error(Exception e) {
            observer.onError(e);
            /* tell all observers to unsubscribe since we had an error */
            stop();
        }

        /**
         * Receive the next value from an observer.
         * <p>
         * If we have received values from all observers, trigger the combineLatest function, otherwise store the value and keep waiting.
         * 
         * @param w
         * @param arg
         */
        <T> void next(CombineObserver<? extends R, ? super T> w, T arg) {
            if (observer == null) {
                throw new RuntimeException("This shouldn't be running if an Observer isn't registered");
            }

            /* if we've been 'unsubscribed' don't process anything further even if the things we're watching keep sending (likely because they are not responding to the unsubscribe call) */
            if (!running.get()) {
                return;
            }

            // remember this as the latest value for this observer
            latestValue.put(w, arg);
            
            if (latestValue.size() < observers.size()) {
              // we don't have a value yet for each observer to combine, so we don't have a combined value yet either
              return;
            }
            
            Object[] argsToCombineLatest = new Object[observers.size()];
            int i = 0;
            for (CombineObserver<R, ?> _w : observers) {
                argsToCombineLatest[i++] = latestValue.get(_w);
            }
            
            try {
                R combinedValue = combineLatestFunction.call(argsToCombineLatest);
                observer.onNext(combinedValue);
            } catch(Exception ex) {
                observer.onError(ex);
            }
        }

        @Override
        public Subscription call(Observer<? super R> observer) {
            if (this.observer != null) {
                throw new IllegalStateException("Only one Observer can subscribe to this Observable.");
            }
            
            AtomicObservableSubscription subscription = new AtomicObservableSubscription(new Subscription() {
                @Override
                public void unsubscribe() {
                    stop();
                }
            });
            this.observer = new SynchronizedObserver<R>(observer, subscription);

            /* start the observers */
            for (CombineObserver<R, ?> rw : observers) {
                rw.startWatching();
            }

            return subscription;
        }

        private void stop() {
            /* tell ourselves to stop processing onNext events */
            running.set(false);
            /* propogate to all observers to unsubscribe */
            for (CombineObserver<R, ?> rw : observers) {
                if (rw.subscription != null) {
                    rw.subscription.unsubscribe();
                }
            }
        }
    }

    public static class UnitTest {

        @Test
        public void testCombineLatestWithFunctionThatThrowsAnException() {
            @SuppressWarnings("unchecked") // mock calls don't do generics
            Observer<String> w = mock(Observer.class);
            
            TestObservable w1 = new TestObservable();
            TestObservable w2 = new TestObservable();
            
            Observable<String> combined = Observable.create(combineLatest(w1, w2, new Func2<String, String, String>() {
                @Override
                public String call(String v1, String v2) {
                    throw new RuntimeException("I don't work.");
                }
            }));
            combined.subscribe(w);
            
            w1.Observer.onNext("first value of w1");
            w2.Observer.onNext("first value of w2");
            
            verify(w, never()).onNext(anyString());
            verify(w, never()).onCompleted();
            verify(w, times(1)).onError(Matchers.<RuntimeException>any());
        }
        
        @Test
        public void testCombineLatestDifferentLengthObservableSequences1() {
            @SuppressWarnings("unchecked") // mock calls don't do generics
            Observer<String> w = mock(Observer.class);

            TestObservable w1 = new TestObservable();
            TestObservable w2 = new TestObservable();
            TestObservable w3 = new TestObservable();

            Observable<String> combineLatestW = Observable.create(combineLatest(w1, w2, w3, getConcat3StringsCombineLatestFunction()));
            combineLatestW.subscribe(w);

            /* simulate sending data */
            // once for w1
            w1.Observer.onNext("1a");
            w2.Observer.onNext("2a");
            w3.Observer.onNext("3a");
            w1.Observer.onCompleted();
            // twice for w2
            w2.Observer.onNext("2b");
            w2.Observer.onCompleted();
            // 4 times for w3
            w3.Observer.onNext("3b");
            w3.Observer.onNext("3c");
            w3.Observer.onNext("3d");
            w3.Observer.onCompleted();

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

            TestObservable w1 = new TestObservable();
            TestObservable w2 = new TestObservable();
            TestObservable w3 = new TestObservable();

            Observable<String> combineLatestW = Observable.create(combineLatest(w1, w2, w3, getConcat3StringsCombineLatestFunction()));
            combineLatestW.subscribe(w);

            /* simulate sending data */
            // 4 times for w1
            w1.Observer.onNext("1a");
            w1.Observer.onNext("1b");
            w1.Observer.onNext("1c");
            w1.Observer.onNext("1d");
            w1.Observer.onCompleted();
            // twice for w2
            w2.Observer.onNext("2a");
            w2.Observer.onNext("2b");
            w2.Observer.onCompleted();
            // 1 times for w3
            w3.Observer.onNext("3a");
            w3.Observer.onCompleted();

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

            TestObservable w1 = new TestObservable();
            TestObservable w2 = new TestObservable();
            TestObservable w3 = new TestObservable();

            Observable<String> combineLatestW = Observable.create(combineLatest(w1, w2, w3, getConcat3StringsCombineLatestFunction()));
            combineLatestW.subscribe(w);

            /* simulate sending data */
            w1.Observer.onNext("1a");
            w2.Observer.onNext("2a");
            w2.Observer.onNext("2b");
            w3.Observer.onNext("3a");

            w1.Observer.onNext("1b");
            w2.Observer.onNext("2c");
            w2.Observer.onNext("2d");
            w3.Observer.onNext("3b");
            
            w1.Observer.onCompleted();
            w2.Observer.onCompleted();
            w3.Observer.onCompleted();

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
        
        /**
         * Testing internal private logic due to the complexity so I want to use TDD to test as a I build it rather than relying purely on the overall functionality expected by the public methods.
         */
        @SuppressWarnings("unchecked")
        /* mock calls don't do generics */
        @Test
        public void testAggregatorSimple() {
            FuncN<String> combineLatestFunction = getConcatCombineLatestFunction();
            /* create the aggregator which will execute the combineLatest function when all Observables provide values */
            Aggregator<String> a = new Aggregator<String>(combineLatestFunction);

            /* define a Observer to receive aggregated events */
            Observer<String> aObserver = mock(Observer.class);
            Observable.create(a).subscribe(aObserver);

            /* mock the Observable Observers that are 'pushing' data for us */
            CombineObserver<String, String> r1 = mock(CombineObserver.class);
            CombineObserver<String, String> r2 = mock(CombineObserver.class);

            /* pretend we're starting up */
            a.addObserver(r1);
            a.addObserver(r2);

            /* simulate the Observables pushing data into the aggregator */
            a.next(r1, "hello");
            a.next(r2, "world");

            InOrder inOrder = inOrder(aObserver);

            verify(aObserver, never()).onError(any(Exception.class));
            verify(aObserver, never()).onCompleted();
            inOrder.verify(aObserver, times(1)).onNext("helloworld");

            a.next(r1, "hello ");
            a.next(r2, "again");

            verify(aObserver, never()).onError(any(Exception.class));
            verify(aObserver, never()).onCompleted();
            inOrder.verify(aObserver, times(1)).onNext("hello again");

            a.complete(r1);
            a.complete(r2);

            inOrder.verify(aObserver, never()).onNext(anyString());
            verify(aObserver, times(1)).onCompleted();
        }

        @SuppressWarnings("unchecked")
        /* mock calls don't do generics */
        @Test
        public void testAggregatorDifferentSizedResultsWithOnComplete() {
            FuncN<String> combineLatestFunction = getConcatCombineLatestFunction();
            /* create the aggregator which will execute the combineLatest function when all Observables provide values */
            Aggregator<String> a = new Aggregator<String>(combineLatestFunction);

            /* define a Observer to receive aggregated events */
            Observer<String> aObserver = mock(Observer.class);
            Observable.create(a).subscribe(aObserver);

            /* mock the Observable Observers that are 'pushing' data for us */
            CombineObserver<String, String> r1 = mock(CombineObserver.class);
            CombineObserver<String, String> r2 = mock(CombineObserver.class);

            /* pretend we're starting up */
            a.addObserver(r1);
            a.addObserver(r2);

            /* simulate the Observables pushing data into the aggregator */
            a.next(r1, "hello");
            a.next(r2, "world");
            a.complete(r2);

            verify(aObserver, never()).onError(any(Exception.class));
            verify(aObserver, never()).onCompleted();
            verify(aObserver, times(1)).onNext("helloworld");

            a.next(r1, "hi");
            a.complete(r1);

            verify(aObserver, never()).onError(any(Exception.class));
            verify(aObserver, times(1)).onCompleted();
            verify(aObserver, times(1)).onNext("hiworld");
        }

        @SuppressWarnings("unchecked")
        /* mock calls don't do generics */
        @Test
        public void testAggregateMultipleTypes() {
            FuncN<String> combineLatestFunction = getConcatCombineLatestFunction();
            /* create the aggregator which will execute the combineLatest function when all Observables provide values */
            Aggregator<String> a = new Aggregator<String>(combineLatestFunction);

            /* define a Observer to receive aggregated events */
            Observer<String> aObserver = mock(Observer.class);
            Observable.create(a).subscribe(aObserver);

            /* mock the Observable Observers that are 'pushing' data for us */
            CombineObserver<String, String> r1 = mock(CombineObserver.class);
            CombineObserver<String, String> r2 = mock(CombineObserver.class);

            /* pretend we're starting up */
            a.addObserver(r1);
            a.addObserver(r2);

            /* simulate the Observables pushing data into the aggregator */
            a.next(r1, "hello");
            a.next(r2, "world");
            a.complete(r2);

            verify(aObserver, never()).onError(any(Exception.class));
            verify(aObserver, never()).onCompleted();
            verify(aObserver, times(1)).onNext("helloworld");

            a.next(r1, "hi");
            a.complete(r1);

            verify(aObserver, never()).onError(any(Exception.class));
            verify(aObserver, times(1)).onCompleted();
            verify(aObserver, times(1)).onNext("hiworld");
        }

        @SuppressWarnings("unchecked")
        /* mock calls don't do generics */
        @Test
        public void testAggregate3Types() {
            FuncN<String> combineLatestFunction = getConcatCombineLatestFunction();
            /* create the aggregator which will execute the combineLatest function when all Observables provide values */
            Aggregator<String> a = new Aggregator<String>(combineLatestFunction);

            /* define a Observer to receive aggregated events */
            Observer<String> aObserver = mock(Observer.class);
            Observable.create(a).subscribe(aObserver);

            /* mock the Observable Observers that are 'pushing' data for us */
            CombineObserver<String, String> r1 = mock(CombineObserver.class);
            CombineObserver<String, Integer> r2 = mock(CombineObserver.class);
            CombineObserver<String, int[]> r3 = mock(CombineObserver.class);

            /* pretend we're starting up */
            a.addObserver(r1);
            a.addObserver(r2);
            a.addObserver(r3);

            /* simulate the Observables pushing data into the aggregator */
            a.next(r1, "hello");
            a.next(r2, 2);
            a.next(r3, new int[] { 5, 6, 7 });

            verify(aObserver, never()).onError(any(Exception.class));
            verify(aObserver, never()).onCompleted();
            verify(aObserver, times(1)).onNext("hello2[5, 6, 7]");
        }

        @SuppressWarnings("unchecked")
        /* mock calls don't do generics */
        @Test
        public void testAggregatorsWithDifferentSizesAndTiming() {
            FuncN<String> combineLatestFunction = getConcatCombineLatestFunction();
            /* create the aggregator which will execute the combineLatest function when all Observables provide values */
            Aggregator<String> a = new Aggregator<String>(combineLatestFunction);

            /* define a Observer to receive aggregated events */
            Observer<String> aObserver = mock(Observer.class);
            Observable.create(a).subscribe(aObserver);

            /* mock the Observable Observers that are 'pushing' data for us */
            CombineObserver<String, String> r1 = mock(CombineObserver.class);
            CombineObserver<String, String> r2 = mock(CombineObserver.class);

            /* pretend we're starting up */
            a.addObserver(r1);
            a.addObserver(r2);

            /* simulate the Observables pushing data into the aggregator */
            a.next(r1, "one");
            a.next(r1, "two");
            a.next(r1, "three");
            a.next(r2, "A");

            verify(aObserver, never()).onError(any(Exception.class));
            verify(aObserver, never()).onCompleted();
            verify(aObserver, times(1)).onNext("threeA");

            a.next(r1, "four");
            a.complete(r1);
            a.next(r2, "B");
            verify(aObserver, times(1)).onNext("fourB");
            a.next(r2, "C");
            verify(aObserver, times(1)).onNext("fourC");
            a.next(r2, "D");
            verify(aObserver, times(1)).onNext("fourD");
            a.next(r2, "E");
            verify(aObserver, times(1)).onNext("fourE");
            a.complete(r2);

            verify(aObserver, never()).onError(any(Exception.class));
            verify(aObserver, times(1)).onCompleted();
        }

        @SuppressWarnings("unchecked")
        /* mock calls don't do generics */
        @Test
        public void testAggregatorError() {
            FuncN<String> combineLatestFunction = getConcatCombineLatestFunction();
            /* create the aggregator which will execute the combineLatest function when all Observables provide values */
            Aggregator<String> a = new Aggregator<String>(combineLatestFunction);

            /* define a Observer to receive aggregated events */
            Observer<String> aObserver = mock(Observer.class);
            Observable.create(a).subscribe(aObserver);

            /* mock the Observable Observers that are 'pushing' data for us */
            CombineObserver<String, String> r1 = mock(CombineObserver.class);
            CombineObserver<String, String> r2 = mock(CombineObserver.class);

            /* pretend we're starting up */
            a.addObserver(r1);
            a.addObserver(r2);

            /* simulate the Observables pushing data into the aggregator */
            a.next(r1, "hello");
            a.next(r2, "world");

            verify(aObserver, never()).onError(any(Exception.class));
            verify(aObserver, never()).onCompleted();
            verify(aObserver, times(1)).onNext("helloworld");

            a.error(new RuntimeException(""));
            a.next(r1, "hello");
            a.next(r2, "again");

            verify(aObserver, times(1)).onError(any(Exception.class));
            verify(aObserver, never()).onCompleted();
            // we don't want to be called again after an error
            verify(aObserver, times(0)).onNext("helloagain");
        }

        @SuppressWarnings("unchecked")
        /* mock calls don't do generics */
        @Test
        public void testAggregatorUnsubscribe() {
            FuncN<String> combineLatestFunction = getConcatCombineLatestFunction();
            /* create the aggregator which will execute the combineLatest function when all Observables provide values */
            Aggregator<String> a = new Aggregator<String>(combineLatestFunction);

            /* define a Observer to receive aggregated events */
            Observer<String> aObserver = mock(Observer.class);
            Subscription subscription = Observable.create(a).subscribe(aObserver);

            /* mock the Observable Observers that are 'pushing' data for us */
            CombineObserver<String, String> r1 = mock(CombineObserver.class);
            CombineObserver<String, String> r2 = mock(CombineObserver.class);

            /* pretend we're starting up */
            a.addObserver(r1);
            a.addObserver(r2);

            /* simulate the Observables pushing data into the aggregator */
            a.next(r1, "hello");
            a.next(r2, "world");

            verify(aObserver, never()).onError(any(Exception.class));
            verify(aObserver, never()).onCompleted();
            verify(aObserver, times(1)).onNext("helloworld");

            subscription.unsubscribe();
            a.next(r1, "hello");
            a.next(r2, "again");

            verify(aObserver, times(0)).onError(any(Exception.class));
            verify(aObserver, never()).onCompleted();
            // we don't want to be called again after an error
            verify(aObserver, times(0)).onNext("helloagain");
        }

        @SuppressWarnings("unchecked")
        /* mock calls don't do generics */
        @Test
        public void testAggregatorEarlyCompletion() {
            FuncN<String> combineLatestFunction = getConcatCombineLatestFunction();
            /* create the aggregator which will execute the combineLatest function when all Observables provide values */
            Aggregator<String> a = new Aggregator<String>(combineLatestFunction);

            /* define a Observer to receive aggregated events */
            Observer<String> aObserver = mock(Observer.class);
            Observable.create(a).subscribe(aObserver);

            /* mock the Observable Observers that are 'pushing' data for us */
            CombineObserver<String, String> r1 = mock(CombineObserver.class);
            CombineObserver<String, String> r2 = mock(CombineObserver.class);

            /* pretend we're starting up */
            a.addObserver(r1);
            a.addObserver(r2);

            /* simulate the Observables pushing data into the aggregator */
            a.next(r1, "one");
            a.next(r1, "two");
            a.complete(r1);
            a.next(r2, "A");

            InOrder inOrder = inOrder(aObserver);
            
            inOrder.verify(aObserver, never()).onError(any(Exception.class));
            inOrder.verify(aObserver, never()).onCompleted();
            inOrder.verify(aObserver, times(1)).onNext("twoA");

            a.complete(r2);

            inOrder.verify(aObserver, never()).onError(any(Exception.class));
            inOrder.verify(aObserver, times(1)).onCompleted();
            // we shouldn't get this since completed is called before any other onNext calls could trigger this
            inOrder.verify(aObserver, never()).onNext(anyString());
        }

        @SuppressWarnings("unchecked")
        /* mock calls don't do generics */
        @Test
        public void testCombineLatest2Types() {
            Func2<String, Integer, String> combineLatestFunction = getConcatStringIntegerCombineLatestFunction();

            /* define a Observer to receive aggregated events */
            Observer<String> aObserver = mock(Observer.class);

            Observable<String> w = Observable.create(combineLatest(Observable.toObservable("one", "two"), Observable.toObservable(2, 3, 4), combineLatestFunction));
            w.subscribe(aObserver);

            verify(aObserver, never()).onError(any(Exception.class));
            verify(aObserver, times(1)).onCompleted();
            verify(aObserver, times(1)).onNext("two2");
            verify(aObserver, times(1)).onNext("two3");
            verify(aObserver, times(1)).onNext("two4");
        }

        @SuppressWarnings("unchecked")
        /* mock calls don't do generics */
        @Test
        public void testCombineLatest3TypesA() {
            Func3<String, Integer, int[], String> combineLatestFunction = getConcatStringIntegerIntArrayCombineLatestFunction();

            /* define a Observer to receive aggregated events */
            Observer<String> aObserver = mock(Observer.class);

            Observable<String> w = Observable.create(combineLatest(Observable.toObservable("one", "two"), Observable.toObservable(2), Observable.toObservable(new int[] { 4, 5, 6 }), combineLatestFunction));
            w.subscribe(aObserver);

            verify(aObserver, never()).onError(any(Exception.class));
            verify(aObserver, times(1)).onCompleted();
            verify(aObserver, times(1)).onNext("two2[4, 5, 6]");
        }

        @SuppressWarnings("unchecked")
        /* mock calls don't do generics */
        @Test
        public void testCombineLatest3TypesB() {
            Func3<String, Integer, int[], String> combineLatestFunction = getConcatStringIntegerIntArrayCombineLatestFunction();

            /* define a Observer to receive aggregated events */
            Observer<String> aObserver = mock(Observer.class);

            Observable<String> w = Observable.create(combineLatest(Observable.toObservable("one"), Observable.toObservable(2), Observable.toObservable(new int[] { 4, 5, 6 }, new int[] { 7, 8 }), combineLatestFunction));
            w.subscribe(aObserver);

            verify(aObserver, never()).onError(any(Exception.class));
            verify(aObserver, times(1)).onCompleted();
            verify(aObserver, times(1)).onNext("one2[4, 5, 6]");
            verify(aObserver, times(1)).onNext("one2[7, 8]");
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

        private FuncN<String> getConcatCombineLatestFunction() {
            FuncN<String> combineLatestFunction = new FuncN<String>() {

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

        private static class TestObservable extends Observable<String> {

            Observer<String> Observer;

            @Override
            public Subscription subscribe(Observer<String> Observer) {
                // just store the variable where it can be accessed so we can manually trigger it
                this.Observer = Observer;
                return Subscriptions.empty();
            }

        }
    }

}
