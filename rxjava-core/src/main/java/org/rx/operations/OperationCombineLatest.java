package org.rx.operations;

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
import org.rx.functions.Func2;
import org.rx.functions.Func3;
import org.rx.functions.Func4;
import org.rx.functions.FuncN;
import org.rx.functions.Functions;
import org.rx.reactive.AbstractIObservable;
import org.rx.reactive.IDisposable;
import org.rx.reactive.IObservable;
import org.rx.reactive.IObserver;

class OperationCombineLatest {

    public static <R, T0, T1> IObservable<R> combineLatest(IObservable<T0> w0, IObservable<T1> w1, Func2<R, T0, T1> combineLatestFunction) {
        Aggregator<R> a = new Aggregator<R>(Functions.fromFunc(combineLatestFunction));
        a.addWatcher(new CombineWatcher<R, T0>(a, w0));
        a.addWatcher(new CombineWatcher<R, T1>(a, w1));
        return a;
    }

    public static <R, T0, T1, T2> IObservable<R> combineLatest(IObservable<T0> w0, IObservable<T1> w1, IObservable<T2> w2, Func3<R, T0, T1, T2> combineLatestFunction) {
        Aggregator<R> a = new Aggregator<R>(Functions.fromFunc(combineLatestFunction));
        a.addWatcher(new CombineWatcher<R, T0>(a, w0));
        a.addWatcher(new CombineWatcher<R, T1>(a, w1));
        a.addWatcher(new CombineWatcher<R, T2>(a, w2));
        return a;
    }

    public static <R, T0, T1, T2, T3> IObservable<R> combineLatest(IObservable<T0> w0, IObservable<T1> w1, IObservable<T2> w2, IObservable<T3> w3, Func4<R, T0, T1, T2, T3> combineLatestFunction) {
        Aggregator<R> a = new Aggregator<R>(Functions.fromFunc(combineLatestFunction));
        a.addWatcher(new CombineWatcher<R, T0>(a, w0));
        a.addWatcher(new CombineWatcher<R, T1>(a, w1));
        a.addWatcher(new CombineWatcher<R, T2>(a, w2));
        a.addWatcher(new CombineWatcher<R, T3>(a, w3));
        return a;
    }

    private static class CombineWatcher<R, T> implements IObserver<T> {
        final IObservable<T> w;
        final Aggregator<R> a;
        private IDisposable subscription;

        public CombineWatcher(Aggregator<R> a, IObservable<T> w) {
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
     * Receive notifications from each of the Watchables we are reducing and execute the combineLatestFunction whenever we have received events from all Watchables.
     * 
     * @param <R>
     */
    private static class Aggregator<R> extends AbstractIObservable<R> {

        private final FuncN<R> combineLatestFunction;
        private IObserver<R> watcher;
        private AtomicBoolean running = new AtomicBoolean(true);

        /**
         * Use LinkedHashMap to retain the order we receive the CombineLatestWatcher objects in.
         * <p>
         * Note that access to this LinkedList inside MUST BE SYNCHRONIZED
         */
        private Map<CombineWatcher<R, ?>, LinkedList<Object>> receivedValuesPerWatcher = new LinkedHashMap<CombineWatcher<R, ?>, LinkedList<Object>>();

        /**
         * store when a watcher completes
         * <p>
         * Note that access to this set MUST BE SYNCHRONIZED
         * */
        private HashSet<CombineWatcher<R, ?>> completed = new HashSet<CombineWatcher<R, ?>>();

        /**
         * The last value from a watcher
         * <p>
         * Note that access to this set MUST BE SYNCHRONIZED
         * */
        private HashMap<CombineWatcher<R, ?>, Object> lastValue = new HashMap<CombineWatcher<R, ?>, Object>();

        public Aggregator(FuncN<R> combineLatestFunction) {
            this.combineLatestFunction = combineLatestFunction;
        }

        /**
         * Receive notification of a watcher starting (meaning we should require it for aggregation)
         * 
         * @param w
         */
        synchronized void addWatcher(CombineWatcher<R, ?> w) {
            // initialize this CombineLatestWatcher
            receivedValuesPerWatcher.put(w, new LinkedList<Object>());
        }

        /**
         * Receive notification of a watcher completing its iterations.
         * 
         * @param w
         */
        synchronized void complete(CombineWatcher<R, ?> w) {
            // store that this ZipWatcher is completed
            completed.add(w);
            // if all CombineWatchers are completed, we mark the whole thing as completed
            if (completed.size() == receivedValuesPerWatcher.size()) {
                if (running.get()) {
                    // mark ourselves as done
                    watcher.onCompleted();
                    // just to ensure we stop processing in case we receive more onNext/complete/error calls after this
                    running.set(false);
                }
            }
        }

        /**
         * Receive error for a watcher. Throw the error up the chain and stop processing.
         * 
         * @param w
         */
        synchronized void error(CombineWatcher<R, ?> w, Exception e) {
            watcher.onError(e);
            /* tell ourselves to stop processing onNext events, event if the watchers don't obey the unsubscribe we're about to send */
            running.set(false);
            /* tell all watchers to unsubscribe since we had an error */
            stop();
        }

        /**
         * Receive the next value from a watcher.
         * <p>
         * If we have received values from all watchers, trigger the combineLatest function, otherwise store the value and keep waiting.
         * 
         * @param w
         * @param arg
         */
        void next(CombineWatcher<R, ?> w, Object arg) {
            if (watcher == null) {
                throw new RuntimeException("This shouldn't be running if a watcher isn't registered");
            }

            /* if we've been 'unsubscribed' don't process anything further even if the things we're watching keep sending (likely because they are not responding to the unsubscribe call) */
            if (!running.get()) {
                return;
            }

            // define here so the variable is out of the synchronized scope
            Object[] argsToCombineLatest = new Object[receivedValuesPerWatcher.size()];

            // we synchronize everything that touches receivedValues and the internal LinkedList objects
            synchronized (this) {
                // add this value to the queue of the CombineLatestWatcher for values received
                receivedValuesPerWatcher.get(w).add(arg);
                // remember this as the last value for this Watcher
                lastValue.put(w, arg);

                // if all CombineLatestWatchers in 'receivedValues' map have a value, invoke the combineLatestFunction
                for (CombineWatcher<R, ?> rw : receivedValuesPerWatcher.keySet()) {
                    if (receivedValuesPerWatcher.get(rw).peek() == null && !completed.contains(rw)) {
                        // we have a null (and the watcher isn't completed) meaning the queues aren't all populated so won't do anything
                        return;
                    }
                }
                // if we get to here this means all the queues have data (or some are completed)
                int i = 0;
                boolean foundData = false;
                for (CombineWatcher<R, ?> _w : receivedValuesPerWatcher.keySet()) {
                    LinkedList<Object> q = receivedValuesPerWatcher.get(_w);
                    if (q.peek() == null) {
                        // this is a completed watcher
                        // we rely on the check above looking at completed.contains to mean that NULL here represents a completed watcher
                        argsToCombineLatest[i++] = lastValue.get(_w);
                    } else {
                        foundData = true;
                        argsToCombineLatest[i++] = q.remove();
                    }
                }
                if (completed.size() == receivedValuesPerWatcher.size() && !foundData) {
                    // all are completed and queues have run out of data, so return and don't send empty data
                    return;
                }
            }
            // if we did not return above from the synchronized block we can now invoke the combineLatestFunction with all of the args
            // we do this outside the synchronized block as it is now safe to call this concurrently and don't need to block other threads from calling
            // this 'next' method while another thread finishes calling this combineLatestFunction
            watcher.onNext(combineLatestFunction.call(argsToCombineLatest));
        }

        @Override
        public IDisposable subscribe(IObserver<R> watcher) {
            if (this.watcher != null) {
                throw new IllegalStateException("Only one watcher can subscribe to this Watchable.");
            }
            this.watcher = watcher;

            /* start the watchers */
            for (CombineWatcher<R, ?> rw : receivedValuesPerWatcher.keySet()) {
                rw.startWatching();
            }

            return new IDisposable() {

                @Override
                public void unsubscribe() {
                    stop();
                }

            };
        }

        private void stop() {
            /* tell ourselves to stop processing onNext events */
            running.set(false);
            /* propogate to all watchers to unsubscribe */
            for (CombineWatcher<R, ?> rw : receivedValuesPerWatcher.keySet()) {
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
        public void testCombineLatestDifferentLengthWatchableSequences1() {
            IObserver<String> w = mock(IObserver.class);

            TestWatchable w1 = new TestWatchable();
            TestWatchable w2 = new TestWatchable();
            TestWatchable w3 = new TestWatchable();

            IObservable<String> combineLatestW = combineLatest(w1, w2, w3, getConcat3StringsCombineLatestFunction());
            combineLatestW.subscribe(w);

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

            /* we should have been called 4 times on the watcher */
            InOrder inOrder = inOrder(w);
            inOrder.verify(w).onNext("1a2a3a");
            inOrder.verify(w).onNext("1a2b3b");
            inOrder.verify(w).onNext("1a2b3c");
            inOrder.verify(w).onNext("1a2b3d");

            inOrder.verify(w, times(1)).onCompleted();
        }

        @Test
        public void testCombineLatestDifferentLengthWatchableSequences2() {
            @SuppressWarnings("unchecked")
            IObserver<String> w = mock(IObserver.class);

            TestWatchable w1 = new TestWatchable();
            TestWatchable w2 = new TestWatchable();
            TestWatchable w3 = new TestWatchable();

            IObservable<String> combineLatestW = combineLatest(w1, w2, w3, getConcat3StringsCombineLatestFunction());
            combineLatestW.subscribe(w);

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

            /* we should have been called 1 time only on the watcher since we only combine the "latest" we don't go back and loop through others once completed */
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
            /* create the aggregator which will execute the combineLatest function when all watchables provide values */
            Aggregator<String> a = new Aggregator<String>(combineLatestFunction);

            /* define a Watcher to receive aggregated events */
            IObserver<String> aWatcher = mock(IObserver.class);
            a.subscribe(aWatcher);

            /* mock the Watchable Watchers that are 'pushing' data for us */
            CombineWatcher<String, String> r1 = mock(CombineWatcher.class);
            CombineWatcher<String, String> r2 = mock(CombineWatcher.class);

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
            FuncN<String> combineLatestFunction = getConcatCombineLatestFunction();
            /* create the aggregator which will execute the combineLatest function when all watchables provide values */
            Aggregator<String> a = new Aggregator<String>(combineLatestFunction);

            /* define a Watcher to receive aggregated events */
            IObserver<String> aWatcher = mock(IObserver.class);
            a.subscribe(aWatcher);

            /* mock the Watchable Watchers that are 'pushing' data for us */
            CombineWatcher<String, String> r1 = mock(CombineWatcher.class);
            CombineWatcher<String, String> r2 = mock(CombineWatcher.class);

            /* pretend we're starting up */
            a.addWatcher(r1);
            a.addWatcher(r2);

            /* simulate the watchables pushing data into the aggregator */
            a.next(r1, "hello");
            a.next(r2, "world");
            a.complete(r2);

            verify(aWatcher, never()).onError(any(Exception.class));
            verify(aWatcher, never()).onCompleted();
            verify(aWatcher, times(1)).onNext("helloworld");

            a.next(r1, "hi");
            a.complete(r1);

            verify(aWatcher, never()).onError(any(Exception.class));
            verify(aWatcher, times(1)).onCompleted();
            verify(aWatcher, times(1)).onNext("hiworld");
        }

        @SuppressWarnings("unchecked")
        /* mock calls don't do generics */
        @Test
        public void testAggregateMultipleTypes() {
            FuncN<String> combineLatestFunction = getConcatCombineLatestFunction();
            /* create the aggregator which will execute the combineLatest function when all watchables provide values */
            Aggregator<String> a = new Aggregator<String>(combineLatestFunction);

            /* define a Watcher to receive aggregated events */
            IObserver<String> aWatcher = mock(IObserver.class);
            a.subscribe(aWatcher);

            /* mock the Watchable Watchers that are 'pushing' data for us */
            CombineWatcher<String, String> r1 = mock(CombineWatcher.class);
            CombineWatcher<String, Integer> r2 = mock(CombineWatcher.class);

            /* pretend we're starting up */
            a.addWatcher(r1);
            a.addWatcher(r2);

            /* simulate the watchables pushing data into the aggregator */
            a.next(r1, "hello");
            a.next(r2, "world");
            a.complete(r2);

            verify(aWatcher, never()).onError(any(Exception.class));
            verify(aWatcher, never()).onCompleted();
            verify(aWatcher, times(1)).onNext("helloworld");

            a.next(r1, "hi");
            a.complete(r1);

            verify(aWatcher, never()).onError(any(Exception.class));
            verify(aWatcher, times(1)).onCompleted();
            verify(aWatcher, times(1)).onNext("hiworld");
        }

        @SuppressWarnings("unchecked")
        /* mock calls don't do generics */
        @Test
        public void testAggregate3Types() {
            FuncN<String> combineLatestFunction = getConcatCombineLatestFunction();
            /* create the aggregator which will execute the combineLatest function when all watchables provide values */
            Aggregator<String> a = new Aggregator<String>(combineLatestFunction);

            /* define a Watcher to receive aggregated events */
            IObserver<String> aWatcher = mock(IObserver.class);
            a.subscribe(aWatcher);

            /* mock the Watchable Watchers that are 'pushing' data for us */
            CombineWatcher<String, String> r1 = mock(CombineWatcher.class);
            CombineWatcher<String, Integer> r2 = mock(CombineWatcher.class);
            CombineWatcher<String, int[]> r3 = mock(CombineWatcher.class);

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
            FuncN<String> combineLatestFunction = getConcatCombineLatestFunction();
            /* create the aggregator which will execute the combineLatest function when all watchables provide values */
            Aggregator<String> a = new Aggregator<String>(combineLatestFunction);

            /* define a Watcher to receive aggregated events */
            IObserver<String> aWatcher = mock(IObserver.class);
            a.subscribe(aWatcher);

            /* mock the Watchable Watchers that are 'pushing' data for us */
            CombineWatcher<String, String> r1 = mock(CombineWatcher.class);
            CombineWatcher<String, String> r2 = mock(CombineWatcher.class);

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
            verify(aWatcher, times(1)).onNext("fourE");
            a.complete(r2);

            verify(aWatcher, never()).onError(any(Exception.class));
            verify(aWatcher, times(1)).onCompleted();
        }

        @SuppressWarnings("unchecked")
        /* mock calls don't do generics */
        @Test
        public void testAggregatorError() {
            FuncN<String> combineLatestFunction = getConcatCombineLatestFunction();
            /* create the aggregator which will execute the combineLatest function when all watchables provide values */
            Aggregator<String> a = new Aggregator<String>(combineLatestFunction);

            /* define a Watcher to receive aggregated events */
            IObserver<String> aWatcher = mock(IObserver.class);
            a.subscribe(aWatcher);

            /* mock the Watchable Watchers that are 'pushing' data for us */
            CombineWatcher<String, String> r1 = mock(CombineWatcher.class);
            CombineWatcher<String, String> r2 = mock(CombineWatcher.class);

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
            FuncN<String> combineLatestFunction = getConcatCombineLatestFunction();
            /* create the aggregator which will execute the combineLatest function when all watchables provide values */
            Aggregator<String> a = new Aggregator<String>(combineLatestFunction);

            /* define a Watcher to receive aggregated events */
            IObserver<String> aWatcher = mock(IObserver.class);
            IDisposable subscription = a.subscribe(aWatcher);

            /* mock the Watchable Watchers that are 'pushing' data for us */
            CombineWatcher<String, String> r1 = mock(CombineWatcher.class);
            CombineWatcher<String, String> r2 = mock(CombineWatcher.class);

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
            FuncN<String> combineLatestFunction = getConcatCombineLatestFunction();
            /* create the aggregator which will execute the combineLatest function when all watchables provide values */
            Aggregator<String> a = new Aggregator<String>(combineLatestFunction);

            /* define a Watcher to receive aggregated events */
            IObserver<String> aWatcher = mock(IObserver.class);
            a.subscribe(aWatcher);

            /* mock the Watchable Watchers that are 'pushing' data for us */
            CombineWatcher<String, String> r1 = mock(CombineWatcher.class);
            CombineWatcher<String, String> r2 = mock(CombineWatcher.class);

            /* pretend we're starting up */
            a.addWatcher(r1);
            a.addWatcher(r2);

            /* simulate the watchables pushing data into the aggregator */
            a.next(r1, "one");
            a.next(r1, "two");
            a.complete(r1);
            a.next(r2, "A");

            verify(aWatcher, never()).onError(any(Exception.class));
            verify(aWatcher, never()).onCompleted();
            verify(aWatcher, times(1)).onNext("oneA");

            a.complete(r2);

            verify(aWatcher, never()).onError(any(Exception.class));
            verify(aWatcher, times(1)).onCompleted();
            // we shouldn't get this since completed is called before any other onNext calls could trigger this
            verify(aWatcher, never()).onNext("twoA");
        }

        @SuppressWarnings("unchecked")
        /* mock calls don't do generics */
        @Test
        public void testCombineLatest2Types() {
            Func2<String, String, Integer> combineLatestFunction = getConcatStringIntegerCombineLatestFunction();

            /* define a Watcher to receive aggregated events */
            IObserver<String> aWatcher = mock(IObserver.class);

            IObservable<String> w = combineLatest(WatchableExtensions.toWatchable("one", "two"), WatchableExtensions.toWatchable(2, 3, 4), combineLatestFunction);
            w.subscribe(aWatcher);

            verify(aWatcher, never()).onError(any(Exception.class));
            verify(aWatcher, times(1)).onCompleted();
            verify(aWatcher, times(1)).onNext("one2");
            verify(aWatcher, times(1)).onNext("two3");
            verify(aWatcher, times(1)).onNext("two4");
        }

        @SuppressWarnings("unchecked")
        /* mock calls don't do generics */
        @Test
        public void testCombineLatest3TypesA() {
            Func3<String, String, Integer, int[]> combineLatestFunction = getConcatStringIntegerIntArrayCombineLatestFunction();

            /* define a Watcher to receive aggregated events */
            IObserver<String> aWatcher = mock(IObserver.class);

            IObservable<String> w = combineLatest(WatchableExtensions.toWatchable("one", "two"), WatchableExtensions.toWatchable(2), WatchableExtensions.toWatchable(new int[] { 4, 5, 6 }), combineLatestFunction);
            w.subscribe(aWatcher);

            verify(aWatcher, never()).onError(any(Exception.class));
            verify(aWatcher, times(1)).onCompleted();
            verify(aWatcher, times(1)).onNext("one2[4, 5, 6]");
        }

        @SuppressWarnings("unchecked")
        /* mock calls don't do generics */
        @Test
        public void testCombineLatest3TypesB() {
            Func3<String, String, Integer, int[]> combineLatestFunction = getConcatStringIntegerIntArrayCombineLatestFunction();

            /* define a Watcher to receive aggregated events */
            IObserver<String> aWatcher = mock(IObserver.class);

            IObservable<String> w = combineLatest(WatchableExtensions.toWatchable("one"), WatchableExtensions.toWatchable(2), WatchableExtensions.toWatchable(new int[] { 4, 5, 6 }, new int[] { 7, 8 }), combineLatestFunction);
            w.subscribe(aWatcher);

            verify(aWatcher, never()).onError(any(Exception.class));
            verify(aWatcher, times(1)).onCompleted();
            verify(aWatcher, times(1)).onNext("one2[4, 5, 6]");
            verify(aWatcher, times(1)).onNext("one2[7, 8]");
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

        private Func2<String, String, Integer> getConcatStringIntegerCombineLatestFunction() {
            Func2<String, String, Integer> combineLatestFunction = new Func2<String, String, Integer>() {

                @Override
                public String call(String s, Integer i) {
                    return getStringValue(s) + getStringValue(i);
                }

            };
            return combineLatestFunction;
        }

        private Func3<String, String, Integer, int[]> getConcatStringIntegerIntArrayCombineLatestFunction() {
            Func3<String, String, Integer, int[]> combineLatestFunction = new Func3<String, String, Integer, int[]>() {

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
