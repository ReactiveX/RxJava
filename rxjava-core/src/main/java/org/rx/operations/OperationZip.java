package org.rx.operations;

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
import org.rx.functions.Func2;
import org.rx.functions.Func3;
import org.rx.functions.Func4;
import org.rx.functions.FuncN;
import org.rx.functions.Functions;
import org.rx.reactive.AbstractIObservable;
import org.rx.reactive.IObservable;
import org.rx.reactive.IDisposable;
import org.rx.reactive.IObserver;


class OperationZip {

    public static <R, T0, T1> IObservable<R> zip(IObservable<T0> w0, IObservable<T1> w1, Func2<R, T0, T1> zipFunction) {
        Aggregator<R> a = new Aggregator<R>(Functions.fromFunc(zipFunction));
        a.addWatcher(new ZipWatcher<R, T0>(a, w0));
        a.addWatcher(new ZipWatcher<R, T1>(a, w1));
        return a;
    }

    public static <R, T0, T1, T2> IObservable<R> zip(IObservable<T0> w0, IObservable<T1> w1, IObservable<T2> w2, Func3<R, T0, T1, T2> zipFunction) {
        Aggregator<R> a = new Aggregator<R>(Functions.fromFunc(zipFunction));
        a.addWatcher(new ZipWatcher<R, T0>(a, w0));
        a.addWatcher(new ZipWatcher<R, T1>(a, w1));
        a.addWatcher(new ZipWatcher<R, T2>(a, w2));
        return a;
    }

    public static <R, T0, T1, T2, T3> IObservable<R> zip(IObservable<T0> w0, IObservable<T1> w1, IObservable<T2> w2, IObservable<T3> w3, Func4<R, T0, T1, T2, T3> zipFunction) {
        Aggregator<R> a = new Aggregator<R>(Functions.fromFunc(zipFunction));
        a.addWatcher(new ZipWatcher<R, T0>(a, w0));
        a.addWatcher(new ZipWatcher<R, T1>(a, w1));
        a.addWatcher(new ZipWatcher<R, T2>(a, w2));
        a.addWatcher(new ZipWatcher<R, T3>(a, w3));
        return a;
    }

    @ThreadSafe
    private static class ZipWatcher<R, T> implements IObserver<T> {
        final IObservable<T> w;
        final Aggregator<R> a;
        private final AtomicWatchableSubscription subscription = new AtomicWatchableSubscription();
        private final AtomicBoolean subscribed = new AtomicBoolean(false);

        public ZipWatcher(Aggregator<R> a, IObservable<T> w) {
            this.a = a;
            this.w = w;
        }

        public void startWatching() {
            if (subscribed.compareAndSet(false, true)) {
                // only subscribe once even if called more than once
                subscription.setActual(w.subscribe(this));
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
     * Receive notifications from each of the Watchables we are reducing and execute the zipFunction whenever we have received events from all Watchables.
     * 
     * @param <R>
     */
    @ThreadSafe
    private static class Aggregator<R> extends AbstractIObservable<R> {

        private final FuncN<R> zipFunction;
        private volatile AtomicWatcher<R> watcher = null;
        private volatile AtomicWatchableSubscription subscription = new AtomicWatchableSubscription();
        private AtomicBoolean started = new AtomicBoolean(false);
        private AtomicBoolean running = new AtomicBoolean(true);
        private ConcurrentHashMap<ZipWatcher<R, ?>, Boolean> completed = new ConcurrentHashMap<ZipWatcher<R, ?>, Boolean>();

        /* we use ConcurrentHashMap despite synchronization of methods because stop() does NOT use synchronization and this map is used by it and can be called by other threads */
        private ConcurrentHashMap<ZipWatcher<R, ?>, ConcurrentLinkedQueue<Object>> receivedValuesPerWatcher = new ConcurrentHashMap<ZipWatcher<R, ?>, ConcurrentLinkedQueue<Object>>();
        /* we use a ConcurrentLinkedQueue to retain ordering (I'd like to just use a ConcurrentLinkedHashMap for 'receivedValuesPerWatcher' but that doesn't exist in standard java */
        private ConcurrentLinkedQueue<ZipWatcher<R, ?>> watchers = new ConcurrentLinkedQueue<ZipWatcher<R, ?>>();

        public Aggregator(FuncN<R> zipFunction) {
            this.zipFunction = zipFunction;
        }

        /**
         * Receive notification of a watcher starting (meaning we should require it for aggregation)
         * 
         * @param w
         */
        @GuardedBy("Invoked ONLY from the static factory methods at top of this class which are always an atomic execution by a single thread.")
        private void addWatcher(ZipWatcher<R, ?> w) {
            // initialize this ZipWatcher
            watchers.add(w);
            receivedValuesPerWatcher.put(w, new ConcurrentLinkedQueue<Object>());
        }

        /**
         * Receive notification of a watcher completing its iterations.
         * 
         * @param w
         */
        void complete(ZipWatcher<R, ?> w) {
            // store that this ZipWatcher is completed
            completed.put(w, Boolean.TRUE);
            // if all ZipWatchers are completed, we mark the whole thing as completed
            if (completed.size() == watchers.size()) {
                if (running.compareAndSet(true, false)) {
                    // this thread succeeded in setting running=false so let's propagate the completion
                    // mark ourselves as done
                    watcher.onCompleted();
                }
            }
        }

        /**
         * Receive error for a watcher. Throw the error up the chain and stop processing.
         * 
         * @param w
         */
        void error(ZipWatcher<R, ?> w, Exception e) {
            if (running.compareAndSet(true, false)) {
                // this thread succeeded in setting running=false so let's propagate the error
                watcher.onError(e);
                /* since we receive an error we want to tell everyone to stop */
                stop();
            }
        }

        /**
         * Receive the next value from a watcher.
         * <p>
         * If we have received values from all watchers, trigger the zip function, otherwise store the value and keep waiting.
         * 
         * @param w
         * @param arg
         */
        void next(ZipWatcher<R, ?> w, Object arg) {
            if (watcher == null) {
                throw new RuntimeException("This shouldn't be running if a watcher isn't registered");
            }

            /* if we've been 'unsubscribed' don't process anything further even if the things we're watching keep sending (likely because they are not responding to the unsubscribe call) */
            if (!running.get()) {
                return;
            }

            // store the value we received and below we'll decide if we are to send it to the watcher
            receivedValuesPerWatcher.get(w).add(arg);

            // define here so the variable is out of the synchronized scope
            Object[] argsToZip = new Object[watchers.size()];

            /* we have to synchronize here despite using concurrent data structures because the compound logic here must all be done atomically */
            synchronized (this) {
                // if all ZipWatchers in 'receivedValues' map have a value, invoke the zipFunction
                for (ZipWatcher<R, ?> rw : receivedValuesPerWatcher.keySet()) {
                    if (receivedValuesPerWatcher.get(rw).peek() == null) {
                        // we have a null meaning the queues aren't all populated so won't do anything
                        return;
                    }
                }
                // if we get to here this means all the queues have data
                int i = 0;
                for (ZipWatcher<R, ?> rw : watchers) {
                    argsToZip[i++] = receivedValuesPerWatcher.get(rw).remove();
                }
            }
            // if we did not return above from the synchronized block we can now invoke the zipFunction with all of the args
            // we do this outside the synchronized block as it is now safe to call this concurrently and don't need to block other threads from calling
            // this 'next' method while another thread finishes calling this zipFunction
            watcher.onNext(zipFunction.call(argsToZip));
        }

        @Override
        public IDisposable subscribe(IObserver<R> watcher) {
            if (started.compareAndSet(false, true)) {
                this.watcher = new AtomicWatcher<R>(watcher, subscription);
                /* start the watchers */
                for (ZipWatcher<R, ?> rw : watchers) {
                    rw.startWatching();
                }

                return subscription.setActual(new IDisposable() {

                    @Override
                    public void unsubscribe() {
                        stop();
                    }

                });
            } else {
                /* a watcher already has subscribed so blow up */
                throw new IllegalStateException("Only one watcher can subscribe to this Watchable.");
            }
        }

        /*
         * Do NOT synchronize this because it gets called via unsubscribe which can occur on other threads
         * and result in deadlocks. (http://jira/browse/API-4060)
         * 
         * AtomicWatchableSubscription uses compareAndSet instead of locking to avoid deadlocks but ensure single-execution.
         * 
         * We do the same in the implementation of this method.
         * 
         * ThreadSafety of this method is provided by:
         * - AtomicBoolean[running].compareAndSet
         * - ConcurrentLinkedQueue[watchers]
         * - ZipWatcher.subscription being an AtomicWatchableSubscription
         */
        private void stop() {
            /* tell ourselves to stop processing onNext events by setting running=false */
            if (running.compareAndSet(true, false)) {
                /* propogate to all watchers to unsubscribe if this thread succeeded in setting running=false */
                for (ZipWatcher<R, ?> rw : watchers) {
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
        public void testZippingDifferentLengthWatchableSequences1() {
            IObserver<String> w = mock(IObserver.class);

            TestWatchable w1 = new TestWatchable();
            TestWatchable w2 = new TestWatchable();
            TestWatchable w3 = new TestWatchable();

            IObservable<String> zipW = zip(w1, w2, w3, getConcat3StringsZipr());
            zipW.subscribe(w);

            /* simulate sending data */
            // once for w1
            w1.watcher.onNext("1a");
            w1.watcher.onCompleted();
            // twice for w2
            w2.watcher.onNext("2a");
            w2.watcher.onNext("2b");
            w2.watcher.onCompleted();
            // 4 times for w3
            w3.watcher.onNext("3a");
            w3.watcher.onNext("3b");
            w3.watcher.onNext("3c");
            w3.watcher.onNext("3d");
            w3.watcher.onCompleted();

            /* we should have been called 1 time on the watcher */
            InOrder inOrder = inOrder(w);
            inOrder.verify(w).onNext("1a2a3a");

            inOrder.verify(w, times(1)).onCompleted();
        }

        @Test
        public void testZippingDifferentLengthWatchableSequences2() {
            @SuppressWarnings("unchecked")
            IObserver<String> w = mock(IObserver.class);

            TestWatchable w1 = new TestWatchable();
            TestWatchable w2 = new TestWatchable();
            TestWatchable w3 = new TestWatchable();

            IObservable<String> zipW = zip(w1, w2, w3, getConcat3StringsZipr());
            zipW.subscribe(w);

            /* simulate sending data */
            // 4 times for w1
            w1.watcher.onNext("1a");
            w1.watcher.onNext("1b");
            w1.watcher.onNext("1c");
            w1.watcher.onNext("1d");
            w1.watcher.onCompleted();
            // twice for w2
            w2.watcher.onNext("2a");
            w2.watcher.onNext("2b");
            w2.watcher.onCompleted();
            // 1 times for w3
            w3.watcher.onNext("3a");
            w3.watcher.onCompleted();

            /* we should have been called 1 time on the watcher */
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
            /* create the aggregator which will execute the zip function when all watchables provide values */
            Aggregator<String> a = new Aggregator<String>(zipr);

            /* define a Watcher to receive aggregated events */
            IObserver<String> aWatcher = mock(IObserver.class);
            a.subscribe(aWatcher);

            /* mock the Watchable Watchers that are 'pushing' data for us */
            ZipWatcher<String, String> r1 = mock(ZipWatcher.class);
            ZipWatcher<String, String> r2 = mock(ZipWatcher.class);

            /* pretend we're starting up */
            a.addWatcher(r1);
            a.addWatcher(r2);

            /* simulate the watchables pushing data into the aggregator */
            a.next(r1, "hello");
            a.next(r2, "world");

            InOrder inOrder = inOrder(aWatcher);

            verify(aWatcher, never()).onError(any(Exception.class));
            verify(aWatcher, never()).onCompleted();
            inOrder.verify(aWatcher, times(1)).onNext("helloworld");

            a.next(r1, "hello ");
            a.next(r2, "again");

            verify(aWatcher, never()).onError(any(Exception.class));
            verify(aWatcher, never()).onCompleted();
            inOrder.verify(aWatcher, times(1)).onNext("hello again");

            a.complete(r1);
            a.complete(r2);

            inOrder.verify(aWatcher, never()).onNext(anyString());
            verify(aWatcher, times(1)).onCompleted();
        }

        @SuppressWarnings("unchecked")
        /* mock calls don't do generics */
        @Test
        public void testAggregatorDifferentSizedResultsWithOnComplete() {
            FuncN<String> zipr = getConcatZipr();
            /* create the aggregator which will execute the zip function when all watchables provide values */
            Aggregator<String> a = new Aggregator<String>(zipr);

            /* define a Watcher to receive aggregated events */
            IObserver<String> aWatcher = mock(IObserver.class);
            a.subscribe(aWatcher);

            /* mock the Watchable Watchers that are 'pushing' data for us */
            ZipWatcher<String, String> r1 = mock(ZipWatcher.class);
            ZipWatcher<String, String> r2 = mock(ZipWatcher.class);

            /* pretend we're starting up */
            a.addWatcher(r1);
            a.addWatcher(r2);

            /* simulate the watchables pushing data into the aggregator */
            a.next(r1, "hello");
            a.next(r2, "world");
            a.complete(r2);

            InOrder inOrder = inOrder(aWatcher);

            inOrder.verify(aWatcher, never()).onError(any(Exception.class));
            inOrder.verify(aWatcher, never()).onCompleted();
            inOrder.verify(aWatcher, times(1)).onNext("helloworld");

            a.next(r1, "hi");
            a.complete(r1);

            inOrder.verify(aWatcher, never()).onError(any(Exception.class));
            inOrder.verify(aWatcher, times(1)).onCompleted();
            inOrder.verify(aWatcher, never()).onNext(anyString());
        }

        @SuppressWarnings("unchecked")
        /* mock calls don't do generics */
        @Test
        public void testAggregateMultipleTypes() {
            FuncN<String> zipr = getConcatZipr();
            /* create the aggregator which will execute the zip function when all watchables provide values */
            Aggregator<String> a = new Aggregator<String>(zipr);

            /* define a Watcher to receive aggregated events */
            IObserver<String> aWatcher = mock(IObserver.class);
            a.subscribe(aWatcher);

            /* mock the Watchable Watchers that are 'pushing' data for us */
            ZipWatcher<String, String> r1 = mock(ZipWatcher.class);
            ZipWatcher<String, Integer> r2 = mock(ZipWatcher.class);

            /* pretend we're starting up */
            a.addWatcher(r1);
            a.addWatcher(r2);

            /* simulate the watchables pushing data into the aggregator */
            a.next(r1, "hello");
            a.next(r2, "world");
            a.complete(r2);

            InOrder inOrder = inOrder(aWatcher);

            inOrder.verify(aWatcher, never()).onError(any(Exception.class));
            inOrder.verify(aWatcher, never()).onCompleted();
            inOrder.verify(aWatcher, times(1)).onNext("helloworld");

            a.next(r1, "hi");
            a.complete(r1);

            inOrder.verify(aWatcher, never()).onError(any(Exception.class));
            inOrder.verify(aWatcher, times(1)).onCompleted();
            inOrder.verify(aWatcher, never()).onNext(anyString());
        }

        @SuppressWarnings("unchecked")
        /* mock calls don't do generics */
        @Test
        public void testAggregate3Types() {
            FuncN<String> zipr = getConcatZipr();
            /* create the aggregator which will execute the zip function when all watchables provide values */
            Aggregator<String> a = new Aggregator<String>(zipr);

            /* define a Watcher to receive aggregated events */
            IObserver<String> aWatcher = mock(IObserver.class);
            a.subscribe(aWatcher);

            /* mock the Watchable Watchers that are 'pushing' data for us */
            ZipWatcher<String, String> r1 = mock(ZipWatcher.class);
            ZipWatcher<String, Integer> r2 = mock(ZipWatcher.class);
            ZipWatcher<String, int[]> r3 = mock(ZipWatcher.class);

            /* pretend we're starting up */
            a.addWatcher(r1);
            a.addWatcher(r2);
            a.addWatcher(r3);

            /* simulate the watchables pushing data into the aggregator */
            a.next(r1, "hello");
            a.next(r2, 2);
            a.next(r3, new int[] { 5, 6, 7 });

            verify(aWatcher, never()).onError(any(Exception.class));
            verify(aWatcher, never()).onCompleted();
            verify(aWatcher, times(1)).onNext("hello2[5, 6, 7]");
        }

        @SuppressWarnings("unchecked")
        /* mock calls don't do generics */
        @Test
        public void testAggregatorsWithDifferentSizesAndTiming() {
            FuncN<String> zipr = getConcatZipr();
            /* create the aggregator which will execute the zip function when all watchables provide values */
            Aggregator<String> a = new Aggregator<String>(zipr);

            /* define a Watcher to receive aggregated events */
            IObserver<String> aWatcher = mock(IObserver.class);
            a.subscribe(aWatcher);

            /* mock the Watchable Watchers that are 'pushing' data for us */
            ZipWatcher<String, String> r1 = mock(ZipWatcher.class);
            ZipWatcher<String, String> r2 = mock(ZipWatcher.class);

            /* pretend we're starting up */
            a.addWatcher(r1);
            a.addWatcher(r2);

            /* simulate the watchables pushing data into the aggregator */
            a.next(r1, "one");
            a.next(r1, "two");
            a.next(r1, "three");
            a.next(r2, "A");

            verify(aWatcher, never()).onError(any(Exception.class));
            verify(aWatcher, never()).onCompleted();
            verify(aWatcher, times(1)).onNext("oneA");

            a.next(r1, "four");
            a.complete(r1);
            a.next(r2, "B");
            verify(aWatcher, times(1)).onNext("twoB");
            a.next(r2, "C");
            verify(aWatcher, times(1)).onNext("threeC");
            a.next(r2, "D");
            verify(aWatcher, times(1)).onNext("fourD");
            a.next(r2, "E");
            verify(aWatcher, never()).onNext("E");
            a.complete(r2);

            verify(aWatcher, never()).onError(any(Exception.class));
            verify(aWatcher, times(1)).onCompleted();
        }

        @SuppressWarnings("unchecked")
        /* mock calls don't do generics */
        @Test
        public void testAggregatorError() {
            FuncN<String> zipr = getConcatZipr();
            /* create the aggregator which will execute the zip function when all watchables provide values */
            Aggregator<String> a = new Aggregator<String>(zipr);

            /* define a Watcher to receive aggregated events */
            IObserver<String> aWatcher = mock(IObserver.class);
            a.subscribe(aWatcher);

            /* mock the Watchable Watchers that are 'pushing' data for us */
            ZipWatcher<String, String> r1 = mock(ZipWatcher.class);
            ZipWatcher<String, String> r2 = mock(ZipWatcher.class);

            /* pretend we're starting up */
            a.addWatcher(r1);
            a.addWatcher(r2);

            /* simulate the watchables pushing data into the aggregator */
            a.next(r1, "hello");
            a.next(r2, "world");

            verify(aWatcher, never()).onError(any(Exception.class));
            verify(aWatcher, never()).onCompleted();
            verify(aWatcher, times(1)).onNext("helloworld");

            a.error(r1, new RuntimeException(""));
            a.next(r1, "hello");
            a.next(r2, "again");

            verify(aWatcher, times(1)).onError(any(Exception.class));
            verify(aWatcher, never()).onCompleted();
            // we don't want to be called again after an error
            verify(aWatcher, times(0)).onNext("helloagain");
        }

        @SuppressWarnings("unchecked")
        /* mock calls don't do generics */
        @Test
        public void testAggregatorUnsubscribe() {
            FuncN<String> zipr = getConcatZipr();
            /* create the aggregator which will execute the zip function when all watchables provide values */
            Aggregator<String> a = new Aggregator<String>(zipr);

            /* define a Watcher to receive aggregated events */
            IObserver<String> aWatcher = mock(IObserver.class);
            IDisposable subscription = a.subscribe(aWatcher);

            /* mock the Watchable Watchers that are 'pushing' data for us */
            ZipWatcher<String, String> r1 = mock(ZipWatcher.class);
            ZipWatcher<String, String> r2 = mock(ZipWatcher.class);

            /* pretend we're starting up */
            a.addWatcher(r1);
            a.addWatcher(r2);

            /* simulate the watchables pushing data into the aggregator */
            a.next(r1, "hello");
            a.next(r2, "world");

            verify(aWatcher, never()).onError(any(Exception.class));
            verify(aWatcher, never()).onCompleted();
            verify(aWatcher, times(1)).onNext("helloworld");

            subscription.unsubscribe();
            a.next(r1, "hello");
            a.next(r2, "again");

            verify(aWatcher, times(0)).onError(any(Exception.class));
            verify(aWatcher, never()).onCompleted();
            // we don't want to be called again after an error
            verify(aWatcher, times(0)).onNext("helloagain");
        }

        @SuppressWarnings("unchecked")
        /* mock calls don't do generics */
        @Test
        public void testAggregatorEarlyCompletion() {
            FuncN<String> zipr = getConcatZipr();
            /* create the aggregator which will execute the zip function when all watchables provide values */
            Aggregator<String> a = new Aggregator<String>(zipr);

            /* define a Watcher to receive aggregated events */
            IObserver<String> aWatcher = mock(IObserver.class);
            a.subscribe(aWatcher);

            /* mock the Watchable Watchers that are 'pushing' data for us */
            ZipWatcher<String, String> r1 = mock(ZipWatcher.class);
            ZipWatcher<String, String> r2 = mock(ZipWatcher.class);

            /* pretend we're starting up */
            a.addWatcher(r1);
            a.addWatcher(r2);

            /* simulate the watchables pushing data into the aggregator */
            a.next(r1, "one");
            a.next(r1, "two");
            a.complete(r1);
            a.next(r2, "A");

            InOrder inOrder = inOrder(aWatcher);

            inOrder.verify(aWatcher, never()).onError(any(Exception.class));
            inOrder.verify(aWatcher, never()).onCompleted();
            inOrder.verify(aWatcher, times(1)).onNext("oneA");

            a.complete(r2);

            inOrder.verify(aWatcher, never()).onError(any(Exception.class));
            inOrder.verify(aWatcher, times(1)).onCompleted();
            inOrder.verify(aWatcher, never()).onNext(anyString());
        }

        @SuppressWarnings("unchecked")
        /* mock calls don't do generics */
        @Test
        public void testZip2Types() {
            Func2<String, String, Integer> zipr = getConcatStringIntegerZipr();

            /* define a Watcher to receive aggregated events */
            IObserver<String> aWatcher = mock(IObserver.class);

            IObservable<String> w = zip(WatchableExtensions.toWatchable("one", "two"), WatchableExtensions.toWatchable(2, 3, 4), zipr);
            w.subscribe(aWatcher);

            verify(aWatcher, never()).onError(any(Exception.class));
            verify(aWatcher, times(1)).onCompleted();
            verify(aWatcher, times(1)).onNext("one2");
            verify(aWatcher, times(1)).onNext("two3");
            verify(aWatcher, never()).onNext("4");
        }

        @SuppressWarnings("unchecked")
        /* mock calls don't do generics */
        @Test
        public void testZip3Types() {
            Func3<String, String, Integer, int[]> zipr = getConcatStringIntegerIntArrayZipr();

            /* define a Watcher to receive aggregated events */
            IObserver<String> aWatcher = mock(IObserver.class);

            IObservable<String> w = zip(WatchableExtensions.toWatchable("one", "two"), WatchableExtensions.toWatchable(2), WatchableExtensions.toWatchable(new int[] { 4, 5, 6 }), zipr);
            w.subscribe(aWatcher);

            verify(aWatcher, never()).onError(any(Exception.class));
            verify(aWatcher, times(1)).onCompleted();
            verify(aWatcher, times(1)).onNext("one2[4, 5, 6]");
            verify(aWatcher, never()).onNext("two");
        }

        @Test
        public void testOnNextExceptionInvokesOnError() {
            Func2<Integer, Integer, Integer> zipr = getDivideZipr();

            IObserver<Integer> aWatcher = mock(IObserver.class);

            IObservable<Integer> w = zip(WatchableExtensions.toWatchable(10, 20, 30), WatchableExtensions.toWatchable(0, 1, 2), zipr);
            w.subscribe(aWatcher);

            verify(aWatcher, times(1)).onError(any(Exception.class));
        }

        private Func2<Integer, Integer, Integer> getDivideZipr() {
            Func2<Integer, Integer, Integer> zipr = new Func2<Integer, Integer, Integer>() {

                @Override
                public Integer call(Integer i1, Integer i2) {
                    return i1/i2;
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

        private Func2<String, String, Integer> getConcatStringIntegerZipr() {
            Func2<String, String, Integer> zipr = new Func2<String, String, Integer>() {

                @Override
                public String call(String s, Integer i) {
                    return getStringValue(s) + getStringValue(i);
                }

            };
            return zipr;
        }

        private Func3<String, String, Integer, int[]> getConcatStringIntegerIntArrayZipr() {
            Func3<String, String, Integer, int[]> zipr = new Func3<String, String, Integer, int[]>() {

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

        private static class TestWatchable extends AbstractIObservable<String> {

            IObserver<String> watcher;

            @Override
            public IDisposable subscribe(IObserver<String> watcher) {
                // just store the variable where it can be accessed so we can manually trigger it
                this.watcher = watcher;
                return WatchableExtensions.noOpSubscription();
            }

        }
    }

}
