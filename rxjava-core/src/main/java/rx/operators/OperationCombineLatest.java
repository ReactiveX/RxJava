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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;
import org.mockito.InOrder;

import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.util.functions.Func1;
import rx.util.functions.Func2;
import rx.util.functions.Func3;
import rx.util.functions.Func4;
import rx.util.functions.FuncN;
import rx.util.functions.Functions;

public class OperationCombineLatest {

    public static <T0, T1, R> Func1<Observer<R>, Subscription> combineLatest(Observable<T0> w0, Observable<T1> w1, Func2<T0, T1, R> combineLatestFunction) {
        Aggregator<R> a = new Aggregator<R>(Functions.fromFunc(combineLatestFunction));
        a.addObserver(new CombineObserver<R, T0>(a, w0));
        a.addObserver(new CombineObserver<R, T1>(a, w1));
        return a;
    }

    public static <T0, T1, T2, R> Func1<Observer<R>, Subscription> combineLatest(Observable<T0> w0, Observable<T1> w1, Observable<T2> w2, Func3<T0, T1, T2, R> combineLatestFunction) {
        Aggregator<R> a = new Aggregator<R>(Functions.fromFunc(combineLatestFunction));
        a.addObserver(new CombineObserver<R, T0>(a, w0));
        a.addObserver(new CombineObserver<R, T1>(a, w1));
        a.addObserver(new CombineObserver<R, T2>(a, w2));
        return a;
    }

    public static <T0, T1, T2, T3, R> Func1<Observer<R>, Subscription> combineLatest(Observable<T0> w0, Observable<T1> w1, Observable<T2> w2, Observable<T3> w3, Func4<T0, T1, T2, T3, R> combineLatestFunction) {
        Aggregator<R> a = new Aggregator<R>(Functions.fromFunc(combineLatestFunction));
        a.addObserver(new CombineObserver<R, T0>(a, w0));
        a.addObserver(new CombineObserver<R, T1>(a, w1));
        a.addObserver(new CombineObserver<R, T2>(a, w2));
        a.addObserver(new CombineObserver<R, T3>(a, w3));
        return a;
    }

    private static class CombineObserver<R, T> implements Observer<T> {
        final Observable<T> w;
        final Aggregator<R> a;
        private Subscription subscription;

        public CombineObserver(Aggregator<R> a, Observable<T> w) {
            this.a = a;
            this.w = w;
        }

        public synchronized void startWatching() {
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
            a.error(this, e);
        }

        @Override
        public void onNext(Object args) {
            a.next(this, args);
        }
    }

    /**
     * Receive notifications from each of the Observables we are reducing and execute the combineLatestFunction whenever we have received events from all Observables.
     * 
     * @param <R>
     */
    private static class Aggregator<R> implements Func1<Observer<R>, Subscription> {

        private final FuncN<R> combineLatestFunction;
        private Observer<R> Observer;
        private AtomicBoolean running = new AtomicBoolean(true);

        /**
         * Use LinkedHashMap to retain the order we receive the CombineLatestObserver objects in.
         * <p>
         * Note that access to this LinkedList inside MUST BE SYNCHRONIZED
         */
        private Map<CombineObserver<R, ?>, LinkedList<Object>> receivedValuesPerObserver = new LinkedHashMap<CombineObserver<R, ?>, LinkedList<Object>>();

        /**
         * store when a Observer completes
         * <p>
         * Note that access to this set MUST BE SYNCHRONIZED
         * */
        private HashSet<CombineObserver<R, ?>> completed = new HashSet<CombineObserver<R, ?>>();

        /**
         * The last value from a Observer
         * <p>
         * Note that access to this set MUST BE SYNCHRONIZED
         * */
        private HashMap<CombineObserver<R, ?>, Object> lastValue = new HashMap<CombineObserver<R, ?>, Object>();

        public Aggregator(FuncN<R> combineLatestFunction) {
            this.combineLatestFunction = combineLatestFunction;
        }

        /**
         * Receive notification of a Observer starting (meaning we should require it for aggregation)
         * 
         * @param w
         */
        synchronized void addObserver(CombineObserver<R, ?> w) {
            // initialize this CombineLatestObserver
            receivedValuesPerObserver.put(w, new LinkedList<Object>());
        }

        /**
         * Receive notification of a Observer completing its iterations.
         * 
         * @param w
         */
        synchronized void complete(CombineObserver<R, ?> w) {
            // store that this ZipObserver is completed
            completed.add(w);
            // if all CombineObservers are completed, we mark the whole thing as completed
            if (completed.size() == receivedValuesPerObserver.size()) {
                if (running.get()) {
                    // mark ourselves as done
                    Observer.onCompleted();
                    // just to ensure we stop processing in case we receive more onNext/complete/error calls after this
                    running.set(false);
                }
            }
        }

        /**
         * Receive error for a Observer. Throw the error up the chain and stop processing.
         * 
         * @param w
         */
        synchronized void error(CombineObserver<R, ?> w, Exception e) {
            Observer.onError(e);
            /* tell ourselves to stop processing onNext events, event if the Observers don't obey the unsubscribe we're about to send */
            running.set(false);
            /* tell all Observers to unsubscribe since we had an error */
            stop();
        }

        /**
         * Receive the next value from a Observer.
         * <p>
         * If we have received values from all Observers, trigger the combineLatest function, otherwise store the value and keep waiting.
         * 
         * @param w
         * @param arg
         */
        void next(CombineObserver<R, ?> w, Object arg) {
            if (Observer == null) {
                throw new RuntimeException("This shouldn't be running if a Observer isn't registered");
            }

            /* if we've been 'unsubscribed' don't process anything further even if the things we're watching keep sending (likely because they are not responding to the unsubscribe call) */
            if (!running.get()) {
                return;
            }

            // define here so the variable is out of the synchronized scope
            Object[] argsToCombineLatest = new Object[receivedValuesPerObserver.size()];

            // we synchronize everything that touches receivedValues and the internal LinkedList objects
            synchronized (this) {
                // add this value to the queue of the CombineLatestObserver for values received
                receivedValuesPerObserver.get(w).add(arg);
                // remember this as the last value for this Observer
                lastValue.put(w, arg);

                // if all CombineLatestObservers in 'receivedValues' map have a value, invoke the combineLatestFunction
                for (CombineObserver<R, ?> rw : receivedValuesPerObserver.keySet()) {
                    if (receivedValuesPerObserver.get(rw).peek() == null && !completed.contains(rw)) {
                        // we have a null (and the Observer isn't completed) meaning the queues aren't all populated so won't do anything
                        return;
                    }
                }
                // if we get to here this means all the queues have data (or some are completed)
                int i = 0;
                boolean foundData = false;
                for (CombineObserver<R, ?> _w : receivedValuesPerObserver.keySet()) {
                    LinkedList<Object> q = receivedValuesPerObserver.get(_w);
                    if (q.peek() == null) {
                        // this is a completed Observer
                        // we rely on the check above looking at completed.contains to mean that NULL here represents a completed Observer
                        argsToCombineLatest[i++] = lastValue.get(_w);
                    } else {
                        foundData = true;
                        argsToCombineLatest[i++] = q.remove();
                    }
                }
                if (completed.size() == receivedValuesPerObserver.size() && !foundData) {
                    // all are completed and queues have run out of data, so return and don't send empty data
                    return;
                }
            }
            // if we did not return above from the synchronized block we can now invoke the combineLatestFunction with all of the args
            // we do this outside the synchronized block as it is now safe to call this concurrently and don't need to block other threads from calling
            // this 'next' method while another thread finishes calling this combineLatestFunction
            Observer.onNext(combineLatestFunction.call(argsToCombineLatest));
        }

        @Override
        public Subscription call(Observer<R> Observer) {
            if (this.Observer != null) {
                throw new IllegalStateException("Only one Observer can subscribe to this Observable.");
            }
            this.Observer = Observer;

            /* start the Observers */
            for (CombineObserver<R, ?> rw : receivedValuesPerObserver.keySet()) {
                rw.startWatching();
            }

            return new Subscription() {

                @Override
                public void unsubscribe() {
                    stop();
                }

            };
        }

        private void stop() {
            /* tell ourselves to stop processing onNext events */
            running.set(false);
            /* propogate to all Observers to unsubscribe */
            for (CombineObserver<R, ?> rw : receivedValuesPerObserver.keySet()) {
                if (rw.subscription != null) {
                    rw.subscription.unsubscribe();
                }
            }
        }

    }

    public static class UnitTest {

        @SuppressWarnings("unchecked")
        /* mock calls don't do generics */
        @Test
        public void testCombineLatestDifferentLengthObservableSequences1() {
            Observer<String> w = mock(Observer.class);

            TestObservable w1 = new TestObservable();
            TestObservable w2 = new TestObservable();
            TestObservable w3 = new TestObservable();

            Observable<String> combineLatestW = Observable.create(combineLatest(w1, w2, w3, getConcat3StringsCombineLatestFunction()));
            combineLatestW.subscribe(w);

            /* simulate sending data */
            // once for w1
            w1.Observer.onNext("1a");
            w1.Observer.onCompleted();
            // twice for w2
            w2.Observer.onNext("2a");
            w2.Observer.onNext("2b");
            w2.Observer.onCompleted();
            // 4 times for w3
            w3.Observer.onNext("3a");
            w3.Observer.onNext("3b");
            w3.Observer.onNext("3c");
            w3.Observer.onNext("3d");
            w3.Observer.onCompleted();

            /* we should have been called 4 times on the Observer */
            InOrder inOrder = inOrder(w);
            inOrder.verify(w).onNext("1a2a3a");
            inOrder.verify(w).onNext("1a2b3b");
            inOrder.verify(w).onNext("1a2b3c");
            inOrder.verify(w).onNext("1a2b3d");

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
            inOrder.verify(w, times(1)).onNext("1a2a3a");
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
            CombineObserver<String, Integer> r2 = mock(CombineObserver.class);

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
            verify(aObserver, times(1)).onNext("oneA");

            a.next(r1, "four");
            a.complete(r1);
            a.next(r2, "B");
            verify(aObserver, times(1)).onNext("twoB");
            a.next(r2, "C");
            verify(aObserver, times(1)).onNext("threeC");
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

            a.error(r1, new RuntimeException(""));
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

            verify(aObserver, never()).onError(any(Exception.class));
            verify(aObserver, never()).onCompleted();
            verify(aObserver, times(1)).onNext("oneA");

            a.complete(r2);

            verify(aObserver, never()).onError(any(Exception.class));
            verify(aObserver, times(1)).onCompleted();
            // we shouldn't get this since completed is called before any other onNext calls could trigger this
            verify(aObserver, never()).onNext("twoA");
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
            verify(aObserver, times(1)).onNext("one2");
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
            verify(aObserver, times(1)).onNext("one2[4, 5, 6]");
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
                return Observable.noOpSubscription();
            }

        }
    }

}
