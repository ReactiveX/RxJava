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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;

import org.junit.Test;
import org.mockito.InOrder;

import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.util.AtomicObservableSubscription;
import rx.util.SynchronizedObserver;
import rx.util.functions.Func1;
import rx.util.functions.Func2;
import rx.util.functions.Func3;
import rx.util.functions.Func4;
import rx.util.functions.FuncN;
import rx.util.functions.Functions;

public final class OperationZip {

    public static <T0, T1, R> Func1<Observer<R>, Subscription> zip(Observable<T0> w0, Observable<T1> w1, Func2<T0, T1, R> zipFunction) {
        Aggregator<R> a = new Aggregator<R>(Functions.fromFunc(zipFunction));
        a.addObserver(new ZipObserver<R, T0>(a, w0));
        a.addObserver(new ZipObserver<R, T1>(a, w1));
        return a;
    }

    public static <T0, T1, T2, R> Func1<Observer<R>, Subscription> zip(Observable<T0> w0, Observable<T1> w1, Observable<T2> w2, Func3<T0, T1, T2, R> zipFunction) {
        Aggregator<R> a = new Aggregator<R>(Functions.fromFunc(zipFunction));
        a.addObserver(new ZipObserver<R, T0>(a, w0));
        a.addObserver(new ZipObserver<R, T1>(a, w1));
        a.addObserver(new ZipObserver<R, T2>(a, w2));
        return a;
    }

    public static <T0, T1, T2, T3, R> Func1<Observer<R>, Subscription> zip(Observable<T0> w0, Observable<T1> w1, Observable<T2> w2, Observable<T3> w3, Func4<T0, T1, T2, T3, R> zipFunction) {
        Aggregator<R> a = new Aggregator<R>(Functions.fromFunc(zipFunction));
        a.addObserver(new ZipObserver<R, T0>(a, w0));
        a.addObserver(new ZipObserver<R, T1>(a, w1));
        a.addObserver(new ZipObserver<R, T2>(a, w2));
        a.addObserver(new ZipObserver<R, T3>(a, w3));
        return a;
    }

    @ThreadSafe
    private static class ZipObserver<R, T> implements Observer<T> {
        final Observable<T> w;
        final Aggregator<R> a;
        private final AtomicObservableSubscription subscription = new AtomicObservableSubscription();
        private final AtomicBoolean subscribed = new AtomicBoolean(false);

        public ZipObserver(Aggregator<R> a, Observable<T> w) {
            this.a = a;
            this.w = w;
        }

        public void startWatching() {
            if (subscribed.compareAndSet(false, true)) {
                // only subscribe once even if called more than once
                subscription.wrap(w.subscribe(this));
            }
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
        public void onNext(T args) {
            try {
                a.next(this, args);
            } catch (Exception e) {
                onError(e);
            }
        }
    }

    /**
     * Receive notifications from each of the Observables we are reducing and execute the zipFunction whenever we have received events from all Observables.
     * 
     * @param <T>
     */
    @ThreadSafe
    private static class Aggregator<T> implements Func1<Observer<T>, Subscription> {

        private volatile SynchronizedObserver<T> observer;
        private final FuncN<T> zipFunction;
        private final AtomicBoolean started = new AtomicBoolean(false);
        private final AtomicBoolean running = new AtomicBoolean(true);
        private final ConcurrentHashMap<ZipObserver<T, ?>, Boolean> completed = new ConcurrentHashMap<ZipObserver<T, ?>, Boolean>();

        /* we use ConcurrentHashMap despite synchronization of methods because stop() does NOT use synchronization and this map is used by it and can be called by other threads */
        private ConcurrentHashMap<ZipObserver<T, ?>, ConcurrentLinkedQueue<Object>> receivedValuesPerObserver = new ConcurrentHashMap<ZipObserver<T, ?>, ConcurrentLinkedQueue<Object>>();
        /* we use a ConcurrentLinkedQueue to retain ordering (I'd like to just use a ConcurrentLinkedHashMap for 'receivedValuesPerObserver' but that doesn't exist in standard java */
        private ConcurrentLinkedQueue<ZipObserver<T, ?>> observers = new ConcurrentLinkedQueue<ZipObserver<T, ?>>();

        public Aggregator(FuncN<T> zipFunction) {
            this.zipFunction = zipFunction;
        }

        /**
         * Receive notification of a Observer starting (meaning we should require it for aggregation)
         * 
         * @param w
         */
        @GuardedBy("Invoked ONLY from the static factory methods at top of this class which are always an atomic execution by a single thread.")
        private void addObserver(ZipObserver<T, ?> w) {
            // initialize this ZipObserver
            observers.add(w);
            receivedValuesPerObserver.put(w, new ConcurrentLinkedQueue<Object>());
        }

        /**
         * Receive notification of a Observer completing its iterations.
         * 
         * @param w
         */
        void complete(ZipObserver<T, ?> w) {
            // store that this ZipObserver is completed
            completed.put(w, Boolean.TRUE);
            // if all ZipObservers are completed, we mark the whole thing as completed
            if (completed.size() == observers.size()) {
                if (running.compareAndSet(true, false)) {
                    // this thread succeeded in setting running=false so let's propagate the completion
                    // mark ourselves as done
                    observer.onCompleted();
                }
            }
        }

        /**
         * Receive error for a Observer. Throw the error up the chain and stop processing.
         * 
         * @param w
         */
        void error(ZipObserver<T, ?> w, Exception e) {
            if (running.compareAndSet(true, false)) {
                // this thread succeeded in setting running=false so let's propagate the error
                observer.onError(e);
                /* since we receive an error we want to tell everyone to stop */
                stop();
            }
        }

        /**
         * Receive the next value from a Observer.
         * <p>
         * If we have received values from all Observers, trigger the zip function, otherwise store the value and keep waiting.
         * 
         * @param w
         * @param arg
         */
        void next(ZipObserver<T, ?> w, Object arg) {
            if (observer == null) {
                throw new RuntimeException("This shouldn't be running if a Observer isn't registered");
            }

            /* if we've been 'unsubscribed' don't process anything further even if the things we're watching keep sending (likely because they are not responding to the unsubscribe call) */
            if (!running.get()) {
                return;
            }

            // store the value we received and below we'll decide if we are to send it to the Observer
            receivedValuesPerObserver.get(w).add(arg);

            // define here so the variable is out of the synchronized scope
            Object[] argsToZip = new Object[observers.size()];

            /* we have to synchronize here despite using concurrent data structures because the compound logic here must all be done atomically */
            synchronized (this) {
                // if all ZipObservers in 'receivedValues' map have a value, invoke the zipFunction
                for (ZipObserver<T, ?> rw : receivedValuesPerObserver.keySet()) {
                    if (receivedValuesPerObserver.get(rw).peek() == null) {
                        // we have a null meaning the queues aren't all populated so won't do anything
                        return;
                    }
                }
                // if we get to here this means all the queues have data
                int i = 0;
                for (ZipObserver<T, ?> rw : observers) {
                    argsToZip[i++] = receivedValuesPerObserver.get(rw).remove();
                }
            }
            // if we did not return above from the synchronized block we can now invoke the zipFunction with all of the args
            // we do this outside the synchronized block as it is now safe to call this concurrently and don't need to block other threads from calling
            // this 'next' method while another thread finishes calling this zipFunction
            observer.onNext(zipFunction.call(argsToZip));
        }

        @Override
        public Subscription call(Observer<T> observer) {
            if (started.compareAndSet(false, true)) {
                AtomicObservableSubscription subscription = new AtomicObservableSubscription();
                this.observer = new SynchronizedObserver<T>(observer, subscription);
                /* start the Observers */
                for (ZipObserver<T, ?> rw : observers) {
                    rw.startWatching();
                }

                return subscription.wrap(new Subscription() {

                    @Override
                    public void unsubscribe() {
                        stop();
                    }

                });
            } else {
                /* a Observer already has subscribed so blow up */
                throw new IllegalStateException("Only one Observer can subscribe to this Observable.");
            }
        }

        /*
         * Do NOT synchronize this because it gets called via unsubscribe which can occur on other threads
         * and result in deadlocks. (http://jira/browse/API-4060)
         * 
         * AtomicObservableSubscription uses compareAndSet instead of locking to avoid deadlocks but ensure single-execution.
         * 
         * We do the same in the implementation of this method.
         * 
         * ThreadSafety of this method is provided by:
         * - AtomicBoolean[running].compareAndSet
         * - ConcurrentLinkedQueue[Observers]
         * - ZipObserver.subscription being an AtomicObservableSubscription
         */
        private void stop() {
            /* tell ourselves to stop processing onNext events by setting running=false */
            if (running.compareAndSet(true, false)) {
                /* propogate to all Observers to unsubscribe if this thread succeeded in setting running=false */
                for (ZipObserver<T, ?> rw : observers) {
                    if (rw.subscription != null) {
                        rw.subscription.unsubscribe();
                    }
                }
            }
        }

    }

    public static class UnitTest {

        @SuppressWarnings("unchecked")
        /* mock calls don't do generics */
        @Test
        public void testZippingDifferentLengthObservableSequences1() {
            Observer<String> w = mock(Observer.class);

            TestObservable w1 = new TestObservable();
            TestObservable w2 = new TestObservable();
            TestObservable w3 = new TestObservable();

            Observable<String> zipW = Observable.create(zip(w1, w2, w3, getConcat3StringsZipr()));
            zipW.subscribe(w);

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

            /* we should have been called 1 time on the Observer */
            InOrder inOrder = inOrder(w);
            inOrder.verify(w).onNext("1a2a3a");

            inOrder.verify(w, times(1)).onCompleted();
        }

        @Test
        public void testZippingDifferentLengthObservableSequences2() {
            @SuppressWarnings("unchecked")
            Observer<String> w = mock(Observer.class);

            TestObservable w1 = new TestObservable();
            TestObservable w2 = new TestObservable();
            TestObservable w3 = new TestObservable();

            Observable<String> zipW = Observable.create(zip(w1, w2, w3, getConcat3StringsZipr()));
            zipW.subscribe(w);

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

            /* we should have been called 1 time on the Observer */
            InOrder inOrder = inOrder(w);
            inOrder.verify(w).onNext("1a2a3a");

            inOrder.verify(w, times(1)).onCompleted();

        }

        /**
         * Testing internal private logic due to the complexity so I want to use TDD to test as a I build it rather than relying purely on the overall functionality expected by the public methods.
         */
        @SuppressWarnings("unchecked")
        /* mock calls don't do generics */
        @Test
        public void testAggregatorSimple() {
            FuncN<String> zipr = getConcatZipr();
            /* create the aggregator which will execute the zip function when all Observables provide values */
            Aggregator<String> a = new Aggregator<String>(zipr);

            /* define a Observer to receive aggregated events */
            Observer<String> aObserver = mock(Observer.class);
            a.call(aObserver);

            /* mock the Observable Observers that are 'pushing' data for us */
            ZipObserver<String, String> r1 = mock(ZipObserver.class);
            ZipObserver<String, String> r2 = mock(ZipObserver.class);

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
            FuncN<String> zipr = getConcatZipr();
            /* create the aggregator which will execute the zip function when all Observables provide values */
            Aggregator<String> a = new Aggregator<String>(zipr);

            /* define a Observer to receive aggregated events */
            Observer<String> aObserver = mock(Observer.class);
            a.call(aObserver);

            /* mock the Observable Observers that are 'pushing' data for us */
            ZipObserver<String, String> r1 = mock(ZipObserver.class);
            ZipObserver<String, String> r2 = mock(ZipObserver.class);

            /* pretend we're starting up */
            a.addObserver(r1);
            a.addObserver(r2);

            /* simulate the Observables pushing data into the aggregator */
            a.next(r1, "hello");
            a.next(r2, "world");
            a.complete(r2);

            InOrder inOrder = inOrder(aObserver);

            inOrder.verify(aObserver, never()).onError(any(Exception.class));
            inOrder.verify(aObserver, never()).onCompleted();
            inOrder.verify(aObserver, times(1)).onNext("helloworld");

            a.next(r1, "hi");
            a.complete(r1);

            inOrder.verify(aObserver, never()).onError(any(Exception.class));
            inOrder.verify(aObserver, times(1)).onCompleted();
            inOrder.verify(aObserver, never()).onNext(anyString());
        }

        @SuppressWarnings("unchecked")
        /* mock calls don't do generics */
        @Test
        public void testAggregateMultipleTypes() {
            FuncN<String> zipr = getConcatZipr();
            /* create the aggregator which will execute the zip function when all Observables provide values */
            Aggregator<String> a = new Aggregator<String>(zipr);

            /* define a Observer to receive aggregated events */
            Observer<String> aObserver = mock(Observer.class);
            a.call(aObserver);

            /* mock the Observable Observers that are 'pushing' data for us */
            ZipObserver<String, String> r1 = mock(ZipObserver.class);
            ZipObserver<String, Integer> r2 = mock(ZipObserver.class);

            /* pretend we're starting up */
            a.addObserver(r1);
            a.addObserver(r2);

            /* simulate the Observables pushing data into the aggregator */
            a.next(r1, "hello");
            a.next(r2, "world");
            a.complete(r2);

            InOrder inOrder = inOrder(aObserver);

            inOrder.verify(aObserver, never()).onError(any(Exception.class));
            inOrder.verify(aObserver, never()).onCompleted();
            inOrder.verify(aObserver, times(1)).onNext("helloworld");

            a.next(r1, "hi");
            a.complete(r1);

            inOrder.verify(aObserver, never()).onError(any(Exception.class));
            inOrder.verify(aObserver, times(1)).onCompleted();
            inOrder.verify(aObserver, never()).onNext(anyString());
        }

        @SuppressWarnings("unchecked")
        /* mock calls don't do generics */
        @Test
        public void testAggregate3Types() {
            FuncN<String> zipr = getConcatZipr();
            /* create the aggregator which will execute the zip function when all Observables provide values */
            Aggregator<String> a = new Aggregator<String>(zipr);

            /* define a Observer to receive aggregated events */
            Observer<String> aObserver = mock(Observer.class);
            a.call(aObserver);

            /* mock the Observable Observers that are 'pushing' data for us */
            ZipObserver<String, String> r1 = mock(ZipObserver.class);
            ZipObserver<String, Integer> r2 = mock(ZipObserver.class);
            ZipObserver<String, int[]> r3 = mock(ZipObserver.class);

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
            FuncN<String> zipr = getConcatZipr();
            /* create the aggregator which will execute the zip function when all Observables provide values */
            Aggregator<String> a = new Aggregator<String>(zipr);

            /* define a Observer to receive aggregated events */
            Observer<String> aObserver = mock(Observer.class);
            a.call(aObserver);

            /* mock the Observable Observers that are 'pushing' data for us */
            ZipObserver<String, String> r1 = mock(ZipObserver.class);
            ZipObserver<String, String> r2 = mock(ZipObserver.class);

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
            verify(aObserver, never()).onNext("E");
            a.complete(r2);

            verify(aObserver, never()).onError(any(Exception.class));
            verify(aObserver, times(1)).onCompleted();
        }

        @SuppressWarnings("unchecked")
        /* mock calls don't do generics */
        @Test
        public void testAggregatorError() {
            FuncN<String> zipr = getConcatZipr();
            /* create the aggregator which will execute the zip function when all Observables provide values */
            Aggregator<String> a = new Aggregator<String>(zipr);

            /* define a Observer to receive aggregated events */
            Observer<String> aObserver = mock(Observer.class);
            a.call(aObserver);

            /* mock the Observable Observers that are 'pushing' data for us */
            ZipObserver<String, String> r1 = mock(ZipObserver.class);
            ZipObserver<String, String> r2 = mock(ZipObserver.class);

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
            FuncN<String> zipr = getConcatZipr();
            /* create the aggregator which will execute the zip function when all Observables provide values */
            Aggregator<String> a = new Aggregator<String>(zipr);

            /* define a Observer to receive aggregated events */
            Observer<String> aObserver = mock(Observer.class);
            Subscription subscription = a.call(aObserver);

            /* mock the Observable Observers that are 'pushing' data for us */
            ZipObserver<String, String> r1 = mock(ZipObserver.class);
            ZipObserver<String, String> r2 = mock(ZipObserver.class);

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
            FuncN<String> zipr = getConcatZipr();
            /* create the aggregator which will execute the zip function when all Observables provide values */
            Aggregator<String> a = new Aggregator<String>(zipr);

            /* define a Observer to receive aggregated events */
            Observer<String> aObserver = mock(Observer.class);
            a.call(aObserver);

            /* mock the Observable Observers that are 'pushing' data for us */
            ZipObserver<String, String> r1 = mock(ZipObserver.class);
            ZipObserver<String, String> r2 = mock(ZipObserver.class);

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
            inOrder.verify(aObserver, times(1)).onNext("oneA");

            a.complete(r2);

            inOrder.verify(aObserver, never()).onError(any(Exception.class));
            inOrder.verify(aObserver, times(1)).onCompleted();
            inOrder.verify(aObserver, never()).onNext(anyString());
        }

        @SuppressWarnings("unchecked")
        /* mock calls don't do generics */
        @Test
        public void testZip2Types() {
            Func2<String, Integer, String> zipr = getConcatStringIntegerZipr();

            /* define a Observer to receive aggregated events */
            Observer<String> aObserver = mock(Observer.class);

            Observable<String> w = Observable.create(zip(Observable.toObservable("one", "two"), Observable.toObservable(2, 3, 4), zipr));
            w.subscribe(aObserver);

            verify(aObserver, never()).onError(any(Exception.class));
            verify(aObserver, times(1)).onCompleted();
            verify(aObserver, times(1)).onNext("one2");
            verify(aObserver, times(1)).onNext("two3");
            verify(aObserver, never()).onNext("4");
        }

        @SuppressWarnings("unchecked")
        /* mock calls don't do generics */
        @Test
        public void testZip3Types() {
            Func3<String, Integer, int[], String> zipr = getConcatStringIntegerIntArrayZipr();

            /* define a Observer to receive aggregated events */
            Observer<String> aObserver = mock(Observer.class);

            Observable<String> w = Observable.create(zip(Observable.toObservable("one", "two"), Observable.toObservable(2), Observable.toObservable(new int[] { 4, 5, 6 }), zipr));
            w.subscribe(aObserver);

            verify(aObserver, never()).onError(any(Exception.class));
            verify(aObserver, times(1)).onCompleted();
            verify(aObserver, times(1)).onNext("one2[4, 5, 6]");
            verify(aObserver, never()).onNext("two");
        }

        @Test
        public void testOnNextExceptionInvokesOnError() {
            Func2<Integer, Integer, Integer> zipr = getDivideZipr();

            @SuppressWarnings("unchecked")
            Observer<Integer> aObserver = mock(Observer.class);

            Observable<Integer> w = Observable.create(zip(Observable.toObservable(10, 20, 30), Observable.toObservable(0, 1, 2), zipr));
            w.subscribe(aObserver);

            verify(aObserver, times(1)).onError(any(Exception.class));
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
