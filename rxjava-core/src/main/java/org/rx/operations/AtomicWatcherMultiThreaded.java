package org.rx.operations;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;

import javax.annotation.concurrent.ThreadSafe;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.rx.reactive.AbstractIObservable;
import org.rx.reactive.IDisposable;
import org.rx.reactive.IObserver;


/**
 * A thread-safe Watcher for transitioning states in operators.
 * <p>
 * Execution rules are:
 * <ul>
 * <li>Allows multiple threads to perform onNext concurrently</li>
 * <li>When an onComplete, onError or unsubscribe request is received, block until all current onNext calls are completed</li>
 * <li>When an unsubscribe is received, block until all current onNext are completed</li>
 * <li>Once an onComplete or onError are performed, no further calls can be executed</li>
 * <li>If unsubscribe is called, this means we call completed() and don't allow any further onNext calls.</li>
 * </ul>
 * 
 * @param <T>
 */
@ThreadSafe
/* package */class AtomicWatcherMultiThreaded<T> implements IObserver<T> {

    private final IObserver<T> watcher;
    private final AtomicWatchableSubscription subscription;
    private final Sync sync = new Sync();
    private volatile boolean finishRequested = false;

    public AtomicWatcherMultiThreaded(IObserver<T> watcher, AtomicWatchableSubscription subscription) {
        this.watcher = watcher;
        this.subscription = subscription;
    }

    public void onNext(T arg) {
        try {
            while (true) {
                // get shared lock to do NEXT operation
                if (finishRequested || !sync.isNonTerminalState()) {
                    // if we're already stopped, or a finish request has been received, we won't allow further onNext requests
                    return;
                }
                // get a shared lock (multiple concurrent threads can get this)
                if (sync.tryAcquireSharedNanos(Sync.TYPE_NEXT, TimeUnit.MILLISECONDS.toNanos(100))) {
                    // break out of the loop as we have the lock
                    break;
                }
                // we failed to acquire (we timed-out) so loop and try again
                // we do this in a loop with timeout so we have the opportunity to stop waiting
                // if the state changes
            }
            // immediately enter a try/finally that will release the lock once done the work
            try {
                watcher.onNext(arg);
            } finally {
                // we finished this work so release it
                sync.releaseShared(Sync.TYPE_NEXT);
            }
        } catch (InterruptedException ie) {
            throw new RuntimeException("onNext interrupted", ie);
        }
    }

    public void onError(Exception e) {
        if (finishRequested || !sync.isNonTerminalState()) {
            // if we're already stopped, or a finish request has been received, we won't allow further onNext requests
            return;
        }
        finishRequested = true;
        // get exclusive lock to do COMPLETE operation
        // this will wait on all onNext events being completed
        try {
            // loop and wait until we get the lock (when all NEXT events are done) or another finishing events beats us
            while (true) {
                if (!sync.tryAcquireNanos(Sync.TYPE_FINISH, TimeUnit.MILLISECONDS.toNanos(100))) {
                    // we failed to acquire (we timed-out)
                    if (!sync.isNonTerminalState()) {
                        // state has changed and no longer permits change so return without doing anything
                        // this could occur if there was a race between multiple onError and onNext calls
                        return;
                    }
                    // timed-out so loop and try again
                    // we do this in a loop with timeout so we have the opportunity to stop waiting
                    // if the state changes
                    continue;
                }
                // break out of the loop as we have the lock
                break;
            }
            // immediately enter a try/finally that will release the lock once done the work
            try {
                watcher.onError(e);
            } finally {
                // we finished this work so release it
                sync.release(Sync.TYPE_FINISH);
            }
        } catch (InterruptedException ie) {
            throw new RuntimeException("OnError interrupted", ie);
        }
    }

    public void onCompleted() {
        if (finishRequested || !sync.isNonTerminalState()) {
            // if we're already stopped, or a finish request has been received, we won't allow further onNext requests
            return;
        }
        finishRequested = true;
        // get exclusive lock to do COMPLETE operation
        // this will wait on all onNext events being completed
        try {
            // loop and wait until we get the lock (when all NEXT events are done) or another finishing events beats us
            while (true) {
                if (!sync.tryAcquireNanos(Sync.TYPE_FINISH, TimeUnit.MILLISECONDS.toNanos(100))) {
                    // we failed to acquire (we timed-out)
                    if (!sync.isNonTerminalState()) {
                        // state has changed and no longer permits change so return without doing anything
                        // this could occur if there was a race between multiple onError and onNext calls
                        return;
                    }
                    // timed-out so loop and try again
                    // we do this in a loop with timeout so we have the opportunity to stop waiting
                    // if the state changes
                    continue;
                }
                // break out of the loop as we have the lock
                break;
            }
            // immediately enter a try/finally that will release the lock once done the work
            try {
                watcher.onCompleted();
            } finally {
                // we finished this work so release it
                sync.release(Sync.TYPE_FINISH);
            }
        } catch (InterruptedException ie) {
            throw new RuntimeException("onCompleted interrupted", ie);
        }
    }

    private class Sync extends AbstractQueuedSynchronizer {
        private static final long serialVersionUID = 1L;

        private static final int TYPE_NEXT = 100;
        private static final int TYPE_FINISH = 200;

        private Sync() {
            // default state
            setState(0);
        }

        /**
         * getState() values are:
         * >0 to represent count of concurrent NEXT executions
         * 0 if no NEXT executions are running
         * -1 if ERROR or COMPLETE have been run
         */

        /**
         * Allow multiple threads to execute NEXT states if getState() >= 0
         * 
         * @throws IllegalMonitorStateException
         *             If isStateChangePermitted() returns false.
         */
        @Override
        protected int tryAcquireShared(int ignoredCauseAlwaysNEXT) {
            /**
             * Reason we're spinning is to optimize via a spin-lock to increment the state
             * instead of returning -1 which can result in the thread being queued and rescheduled.
             */
            while (true) {
                int currentState = getState();
                // check at the beginning of each loop if we are able to make changes
                // we could end up in a race where a finishing event interleaves and thus we need to stop
                if (!isNotTerminalState(currentState)) {
                    return -1;
                }

                // let's try and get a shared lock for NEXT events to occur
                // we only allow NEXT events if state >= 0
                // (this is a double-check on what isStateChangePermitted() checked above
                // increment the number of NEXT threads
                if (compareAndSetState(currentState, currentState + 1)) {
                    // we return a positive number since we still allow more threads to acquire
                    return 1;
                } else {
                    // failed to set it as the state changed from another thread so try again
                    continue;
                }
            }
        }

        /**
         * As NEXT events completed, we decrement down until we hit 0
         * <p>
         * If the state has been set <0 we won't change the value as this means a finishing event has been requested
         */
        @Override
        protected boolean tryReleaseShared(int ignoredCauseAlwaysNEXT) {
            // loop until we succeed
            while (true) {
                // decrement the number of NEXT threads
                int currentState = getState();
                // don't decrement below 0
                // if currentState <0 and this method is invoked, that means NEXT events are finishing, but an exclusive lock has been requested
                if (currentState > 0) {
                    if (compareAndSetState(currentState, currentState - 1)) {
                        return true;
                    }
                } else {
                    /* this means we have concurrency bugs */
                    throw new RuntimeException("We should never be in a state where a release tries to decrement below 0");
                }
            }
        }

        /**
         * A lock for a single thread to execute finishing events (onComplete, onError)
         * 
         * @throws IllegalMonitorStateException
         *             If isStateChangePermitted() returns false.
         */
        @Override
        protected boolean tryAcquire(int ignore) {
            int currentState = getState();
            if (!isNotTerminalState(currentState)) {
                return false;
            }

            /*
             * Commented out the following to be non-fair as we are deeming it a rare event that multiple should be received
             * as that means the sequence is sending invalid data and if it does occur then whichever gets scheduled first
             * will win.
             */
            // if (hasQueuedThreads() && !getFirstQueuedThread().equals(Thread.currentThread())) {
            // // we need to be FIFO on acquiring the exclusive lock
            // // in other words, if both onComplete and onError are requested, whichever is first should retain it's order
            // return false;
            // }

            /*
             * We only allow a finishing event to occur if state is 0
             * which means no NEXT events are processing (state > 0)
             * and a finishing event has not already occurred (state == -1)
             */
            if (compareAndSetState(0, -1)) {
                return true;
            }
            // we didn't get the state so we return false which will cause this thread to be queued
            return false;
        }

        @Override
        protected boolean tryRelease(int desiredState) {
            // no state to change ... we're done and will not allow any further work on this lock to occur in this state (it can't be reused which is why we don't change state)
            return true;
        }

        /**
         * A finishing state has been requested, is being executed or is finished so no further actions should be permitted.
         * 
         * @return
         */
        public boolean isFinishing(int state) {
            // all values < 0 mean it's attempting to finish or has finished
            return state < 0;
        }

        /**
         * Returns true if we have not yet reached terminal state (a finishing event) and state changes are permitted, false it not.
         * 
         * @return
         */
        public boolean isNonTerminalState() {
            return isNotTerminalState(getState());
        }

        /**
         * Returns true if we have not yet reached terminal state (a finishing event) and state changes are permitted, false it not.
         * 
         * @return
         */
        public boolean isNotTerminalState(int state) {
            boolean permitted = !(isFinishing(state) || subscription.isUnsubscribed());
            return permitted;
        }

    }

    public static class UnitTest {
        @Mock
        IObserver<String> aWatcher;

        @Before
        public void before() {
            MockitoAnnotations.initMocks(this);
        }

        @Test
        public void testSingleThreadedBasic() {
            IDisposable s = mock(IDisposable.class);
            TestSingleThreadedWatchable w = new TestSingleThreadedWatchable(s, "one", "two", "three");

            AtomicWatchableSubscription as = new AtomicWatchableSubscription(s);
            AtomicWatcherMultiThreaded<String> aw = new AtomicWatcherMultiThreaded<String>(aWatcher, as);

            w.subscribe(aw);
            w.waitToFinish();

            verify(aWatcher, times(1)).onNext("one");
            verify(aWatcher, times(1)).onNext("two");
            verify(aWatcher, times(1)).onNext("three");
            verify(aWatcher, never()).onError(any(Exception.class));
            verify(aWatcher, times(1)).onCompleted();
            verify(s, never()).unsubscribe();
        }

        @Test
        public void testMultiThreadedBasic() {
            IDisposable s = mock(IDisposable.class);
            TestMultiThreadedWatchable w = new TestMultiThreadedWatchable(s, "one", "two", "three");

            AtomicWatchableSubscription as = new AtomicWatchableSubscription(s);
            BusyWatcher busyWatcher = new BusyWatcher();
            AtomicWatcherMultiThreaded<String> aw = new AtomicWatcherMultiThreaded<String>(busyWatcher, as);

            w.subscribe(aw);
            w.waitToFinish();

            assertEquals(3, busyWatcher.onNextCount.get());
            assertFalse(busyWatcher.onError);
            assertTrue(busyWatcher.onCompleted);
            verify(s, never()).unsubscribe();

            assertTrue(w.maxConcurrentThreads.get() > 1);
            assertTrue(busyWatcher.maxConcurrentThreads.get() > 1);
            System.out.println("maxConcurrentThreads: " + w.maxConcurrentThreads.get());
        }

        @Test
        public void testMultiThreadedWithNPE() {
            IDisposable s = mock(IDisposable.class);
            TestMultiThreadedWatchable w = new TestMultiThreadedWatchable(s, "one", "two", "three", null);

            AtomicWatchableSubscription as = new AtomicWatchableSubscription(s);
            BusyWatcher busyWatcher = new BusyWatcher();
            AtomicWatcherMultiThreaded<String> aw = new AtomicWatcherMultiThreaded<String>(busyWatcher, as);

            w.subscribe(aw);
            w.waitToFinish();

            System.out.println("maxConcurrentThreads: " + w.maxConcurrentThreads.get());

            /*
             * we can't be exact here with a count of 3 because we allow interleaving
             * so the null could cause onError to occur before one or more of the other values
             * resulting in less onNext calls than 3.
             */
            assertTrue(busyWatcher.onNextCount.get() >= 0 && busyWatcher.onNextCount.get() <= 3);
            // we expect an onError because of the null throwing an NPE
            assertTrue(busyWatcher.onError);
            // no onCompleted because onError was invoked
            assertFalse(busyWatcher.onCompleted);
            verify(s, never()).unsubscribe();

            assertTrue(w.maxConcurrentThreads.get() > 1);
            assertTrue(busyWatcher.maxConcurrentThreads.get() > 1);
        }

        @Test
        public void testMultiThreadedWithNPEinMiddle() {
            IDisposable s = mock(IDisposable.class);
            TestMultiThreadedWatchable w = new TestMultiThreadedWatchable(s, "one", "two", "three", null, "four", "five", "six", "seven", "eight", "nine");

            AtomicWatchableSubscription as = new AtomicWatchableSubscription(s);
            BusyWatcher busyWatcher = new BusyWatcher();
            AtomicWatcherMultiThreaded<String> aw = new AtomicWatcherMultiThreaded<String>(busyWatcher, as);

            w.subscribe(aw);
            w.waitToFinish();

            System.out.println("maxConcurrentThreads: " + w.maxConcurrentThreads.get());
            // this should not be the full number of items since the error should stop it before it completes all 9
            System.out.println("onNext count: " + busyWatcher.onNextCount.get());
            assertTrue(busyWatcher.onNextCount.get() < 9);
            assertTrue(busyWatcher.onError);
            // no onCompleted because onError was invoked
            assertFalse(busyWatcher.onCompleted);
            verify(s, never()).unsubscribe();

            assertTrue(w.maxConcurrentThreads.get() > 1);
            assertTrue(busyWatcher.maxConcurrentThreads.get() > 1);
        }

        /**
         * A non-realistic use case that tries to expose thread-safety issues by throwing lots of out-of-order
         * events on many threads.
         * 
         * @param w
         * @param tw
         */
        @Test
        public void runConcurrencyTest() {
            ExecutorService tp = Executors.newFixedThreadPool(20);
            try {
                TestConcurrencyWatcher tw = new TestConcurrencyWatcher();
                AtomicWatchableSubscription s = new AtomicWatchableSubscription();
                AtomicWatcherMultiThreaded<String> w = new AtomicWatcherMultiThreaded<String>(tw, s);

                Future<?> f1 = tp.submit(new OnNextThread(w, 12000));
                Future<?> f2 = tp.submit(new OnNextThread(w, 5000));
                Future<?> f3 = tp.submit(new OnNextThread(w, 75000));
                Future<?> f4 = tp.submit(new OnNextThread(w, 13500));
                Future<?> f5 = tp.submit(new OnNextThread(w, 22000));
                Future<?> f6 = tp.submit(new OnNextThread(w, 15000));

                Future<?> f10 = tp.submit(new CompletionThread(w, TestConcurrencyWatcherEvent.onCompleted, f1, f2, f3, f4));
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    // ignore
                }

                // simulate more onNext calls happening after an onComplete
                Future<?> f7 = tp.submit(new OnNextThread(w, 7500));
                Future<?> f8 = tp.submit(new OnNextThread(w, 23500));

                Future<?> f11 = tp.submit(new CompletionThread(w, TestConcurrencyWatcherEvent.onCompleted, f4, f6, f7));
                Future<?> f12 = tp.submit(new CompletionThread(w, TestConcurrencyWatcherEvent.onCompleted, f4, f6, f7));
                Future<?> f13 = tp.submit(new CompletionThread(w, TestConcurrencyWatcherEvent.onCompleted, f4, f6, f7));
                Future<?> f14 = tp.submit(new CompletionThread(w, TestConcurrencyWatcherEvent.onCompleted, f4, f6, f7));
                // // the next 4 onError events should wait on same as f10
                Future<?> f15 = tp.submit(new CompletionThread(w, TestConcurrencyWatcherEvent.onError, f1, f2, f3, f4));
                Future<?> f16 = tp.submit(new CompletionThread(w, TestConcurrencyWatcherEvent.onError, f1, f2, f3, f4));
                Future<?> f17 = tp.submit(new CompletionThread(w, TestConcurrencyWatcherEvent.onError, f1, f2, f3, f4));
                Future<?> f18 = tp.submit(new CompletionThread(w, TestConcurrencyWatcherEvent.onError, f1, f2, f3, f4));

                waitOnThreads(f1, f2, f3, f4, f5, f6, f7, f8, f10, f11, f12, f13, f14, f15, f16, f17, f18);
                int numNextEvents = tw.assertEvents();
                System.out.println("Number of events executed: " + numNextEvents);
            } catch (Exception e) {
                fail("Concurrency test failed: " + e.getMessage());
                e.printStackTrace();
            } finally {
                tp.shutdown();
                try {
                    tp.awaitTermination(5000, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        /**
         * This spawns a single thread for the subscribe execution
         * 
         */
        private static class TestSingleThreadedWatchable extends AbstractIObservable<String> {

            final IDisposable s;
            final String[] values;
            Thread t = null;

            public TestSingleThreadedWatchable(IDisposable s, String... values) {
                this.s = s;
                this.values = values;
            }

            @Override
            public IDisposable subscribe(final IObserver<String> observer) {
                System.out.println("TestSingleThreadedWatchable subscribed to ...");
                t = new Thread(new Runnable() {

                    @Override
                    public void run() {
                        try {
                            System.out.println("running TestSingleThreadedWatchable thread");
                            for (String s : values) {
                                System.out.println("TestSingleThreadedWatchable onNext: " + s);
                                observer.onNext(s);
                            }
                            observer.onCompleted();
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }

                });
                System.out.println("starting TestSingleThreadedWatchable thread");
                t.start();
                System.out.println("done starting TestSingleThreadedWatchable thread");
                return s;
            }

            public void waitToFinish() {
                try {
                    t.join();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }

        }

        /**
         * This spawns a thread for the subscription, then a separate thread for each onNext call.
         * 
         */
        private static class TestMultiThreadedWatchable extends AbstractIObservable<String> {

            final IDisposable s;
            final String[] values;
            Thread t = null;
            AtomicInteger threadsRunning = new AtomicInteger();
            AtomicInteger maxConcurrentThreads = new AtomicInteger();
            ExecutorService threadPool;

            public TestMultiThreadedWatchable(IDisposable s, String... values) {
                this.s = s;
                this.values = values;
                this.threadPool = Executors.newCachedThreadPool();
            }

            @Override
            public IDisposable subscribe(final IObserver<String> observer) {
                System.out.println("TestMultiThreadedWatchable subscribed to ...");
                t = new Thread(new Runnable() {

                    @Override
                    public void run() {
                        try {
                            System.out.println("running TestMultiThreadedWatchable thread");
                            for (final String s : values) {
                                threadPool.execute(new Runnable() {

                                    @Override
                                    public void run() {
                                        threadsRunning.incrementAndGet();
                                        try {
                                            // perform onNext call
                                            System.out.println("TestMultiThreadedWatchable onNext: " + s);
                                            if (s == null) {
                                                // force an error
                                                throw new NullPointerException();
                                            }
                                            observer.onNext(s);
                                            // capture 'maxThreads'
                                            int concurrentThreads = threadsRunning.get();
                                            int maxThreads = maxConcurrentThreads.get();
                                            if (concurrentThreads > maxThreads) {
                                                maxConcurrentThreads.compareAndSet(maxThreads, concurrentThreads);
                                            }
                                        } catch (Exception e) {
                                            observer.onError(e);
                                        } finally {
                                            threadsRunning.decrementAndGet();
                                        }
                                    }
                                });
                            }
                            // we are done spawning threads
                            threadPool.shutdown();
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }

                        // wait until all threads are done, then mark it as COMPLETED
                        try {
                            // wait for all the threads to finish
                            threadPool.awaitTermination(2, TimeUnit.SECONDS);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                        observer.onCompleted();
                    }
                });
                System.out.println("starting TestMultiThreadedWatchable thread");
                t.start();
                System.out.println("done starting TestMultiThreadedWatchable thread");
                return s;
            }

            public void waitToFinish() {
                try {
                    t.join();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        private static class BusyWatcher implements IObserver<String> {
            volatile boolean onCompleted = false;
            volatile boolean onError = false;
            AtomicInteger onNextCount = new AtomicInteger();
            AtomicInteger threadsRunning = new AtomicInteger();
            AtomicInteger maxConcurrentThreads = new AtomicInteger();

            @Override
            public void onCompleted() {
                System.out.println(">>> BusyWatcher received onCompleted");
                onCompleted = true;
            }

            @Override
            public void onError(Exception e) {
                System.out.println(">>> BusyWatcher received onError: " + e.getMessage());
                onError = true;
            }

            @Override
            public void onNext(String args) {
                threadsRunning.incrementAndGet();
                try {
                    onNextCount.incrementAndGet();
                    System.out.println(">>> BusyWatcher received onNext: " + args);
                    try {
                        // simulate doing something computational
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } finally {
                    // capture 'maxThreads'
                    int concurrentThreads = threadsRunning.get();
                    int maxThreads = maxConcurrentThreads.get();
                    if (concurrentThreads > maxThreads) {
                        maxConcurrentThreads.compareAndSet(maxThreads, concurrentThreads);
                    }
                    threadsRunning.decrementAndGet();
                }
            }

        }

        private static enum TestConcurrencyWatcherEvent {
            onCompleted, onError, onNext;
        }

        private static class TestConcurrencyWatcher implements IObserver<String> {

            /** used to store the order and number of events received */
            private final LinkedBlockingQueue<TestConcurrencyWatcherEvent> events = new LinkedBlockingQueue<TestConcurrencyWatcherEvent>();
            private final int waitTime;

            public TestConcurrencyWatcher(int waitTimeInNext) {
                this.waitTime = waitTimeInNext;
            }

            public TestConcurrencyWatcher() {
                this.waitTime = 0;
            }

            @Override
            public void onCompleted() {
                events.add(TestConcurrencyWatcherEvent.onCompleted);
            }

            @Override
            public void onError(Exception e) {
                events.add(TestConcurrencyWatcherEvent.onError);
            }

            @Override
            public void onNext(String args) {
                events.add(TestConcurrencyWatcherEvent.onNext);
                // do some artificial work to make the thread scheduling/timing vary
                int s = 0;
                for (int i = 0; i < 20; i++) {
                    s += s * i;
                }

                if (waitTime > 0) {
                    try {
                        Thread.sleep(waitTime);
                    } catch (InterruptedException e) {
                        // ignore
                    }
                }
            }

            /**
             * Assert the order of events is correct and return the number of onNext executions.
             * 
             * @param expectedEndingEvent
             * @return int count of onNext calls
             * @throws IllegalStateException
             *             If order of events was invalid.
             */
            public int assertEvents() throws IllegalStateException {
                int nextCount = 0;
                boolean finished = false;
                for (TestConcurrencyWatcherEvent e : events) {
                    if (e == TestConcurrencyWatcherEvent.onNext) {
                        if (finished) {
                            // already finished, we shouldn't get this again
                            throw new IllegalStateException("Received onNext but we're already finished.");
                        }
                        nextCount++;
                    } else if (e == TestConcurrencyWatcherEvent.onError) {
                        if (finished) {
                            // already finished, we shouldn't get this again
                            throw new IllegalStateException("Received onError but we're already finished.");
                        }
                        finished = true;
                    } else if (e == TestConcurrencyWatcherEvent.onCompleted) {
                        if (finished) {
                            // already finished, we shouldn't get this again
                            throw new IllegalStateException("Received onCompleted but we're already finished.");
                        }
                        finished = true;
                    }
                }

                return nextCount;
            }

        }

        /**
         * A thread that will pass data to onNext
         */
        public static class OnNextThread implements Runnable {

            private final IObserver<String> watcher;
            private final int numStringsToSend;

            OnNextThread(IObserver<String> watcher, int numStringsToSend) {
                this.watcher = watcher;
                this.numStringsToSend = numStringsToSend;
            }

            @Override
            public void run() {
                for (int i = 0; i < numStringsToSend; i++) {
                    watcher.onNext("aString");
                }
            }
        }

        /**
         * A thread that will call onError or onNext
         */
        public static class CompletionThread implements Runnable {

            private final IObserver<String> watcher;
            private final TestConcurrencyWatcherEvent event;
            private final Future<?>[] waitOnThese;

            CompletionThread(IObserver<String> watcher, TestConcurrencyWatcherEvent event, Future<?>... waitOnThese) {
                this.watcher = watcher;
                this.event = event;
                this.waitOnThese = waitOnThese;
            }

            @Override
            public void run() {
                /* if we have 'waitOnThese' futures, we'll wait on them before proceeding */
                if (waitOnThese != null) {
                    for (Future<?> f : waitOnThese) {
                        try {
                            f.get();
                        } catch (Exception e) {
                            System.err.println("Error while waiting on future in CompletionThread");
                        }
                    }
                }

                /* send the event */
                if (event == TestConcurrencyWatcherEvent.onError) {
                    watcher.onError(new RuntimeException("mocked exception"));
                } else if (event == TestConcurrencyWatcherEvent.onCompleted) {
                    watcher.onCompleted();

                } else {
                    throw new IllegalArgumentException("Expecting either onError or onCompleted");
                }
            }
        }

        private static void waitOnThreads(Future<?>... futures) {
            for (Future<?> f : futures) {
                try {
                    f.get(10, TimeUnit.SECONDS);
                } catch (Exception e) {
                    System.err.println("Failed while waiting on future.");
                    e.printStackTrace();
                }
            }
        }

    }

}