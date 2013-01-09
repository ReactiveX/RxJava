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
 * <li>Allow only single-threaded, synchronous, ordered execution of onNext, onCompleted, onError</li>
 * <li>Once an onComplete or onError are performed, no further calls can be executed</li>
 * <li>If unsubscribe is called, this means we call completed() and don't allow any further onNext calls.</li>
 * </ul>
 * 
 * @param <T>
 */
@ThreadSafe
/* package */class AtomicWatcherSingleThreaded<T> implements IObserver<T> {

    /**
     * Intrinsic synchronized locking with double-check short-circuiting was chosen after testing several other implementations.
     * 
     * The code and results can be found here:
     * - https://github.com/benjchristensen/JavaLockPerformanceTests/tree/master/results/watcher
     * - https://github.com/benjchristensen/JavaLockPerformanceTests/tree/master/src/com/benjchristensen/performance/locks/watcher
     * 
     * The major characteristic that made me choose synchronized instead of Reentrant or a customer AbstractQueueSynchronizer implementation
     * is that intrinsic locking performed better when nested, and AtomicWatcherSingleThreaded will end up nested most of the time since Rx is
     * compositional by its very nature.
     */

    private final IObserver<T> watcher;
    private final AtomicWatchableSubscription subscription;
    private volatile boolean finishRequested = false;
    private volatile boolean finished = false;

    public AtomicWatcherSingleThreaded(IObserver<T> watcher, AtomicWatchableSubscription subscription) {
        this.watcher = watcher;
        this.subscription = subscription;
    }

    public void onNext(T arg) {
        if (finished || finishRequested || subscription.isUnsubscribed()) {
            // if we're already stopped, or a finish request has been received, we won't allow further onNext requests
            return;
        }
        synchronized (this) {
            // check again since this could have changed while waiting
            if (finished || finishRequested || subscription.isUnsubscribed()) {
                // if we're already stopped, or a finish request has been received, we won't allow further onNext requests
                return;
            }
            watcher.onNext(arg);
        }
    }

    public void onError(Exception e) {
        if (finished || subscription.isUnsubscribed()) {
            // another thread has already finished us, so we won't proceed
            return;
        }
        finishRequested = true;
        synchronized (this) {
            // check again since this could have changed while waiting
            if (finished || subscription.isUnsubscribed()) {
                return;
            }
            watcher.onError(e);
            finished = true;
        }
    }

    public void onCompleted() {
        if (finished || subscription.isUnsubscribed()) {
            // another thread has already finished us, so we won't proceed
            return;
        }
        finishRequested = true;
        synchronized (this) {
            // check again since this could have changed while waiting
            if (finished || subscription.isUnsubscribed()) {
                return;
            }
            watcher.onCompleted();
            finished = true;
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
            AtomicWatcherSingleThreaded<String> aw = new AtomicWatcherSingleThreaded<String>(aWatcher, as);

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
            AtomicWatcherSingleThreaded<String> aw = new AtomicWatcherSingleThreaded<String>(busyWatcher, as);

            w.subscribe(aw);
            w.waitToFinish();

            assertEquals(3, busyWatcher.onNextCount.get());
            assertFalse(busyWatcher.onError);
            assertTrue(busyWatcher.onCompleted);
            verify(s, never()).unsubscribe();

            // we can have concurrency ...
            assertTrue(w.maxConcurrentThreads.get() > 1);
            // ... but the onNext execution should be single threaded
            assertEquals(1, busyWatcher.maxConcurrentThreads.get());
        }

        @Test
        public void testMultiThreadedWithNPE() {
            IDisposable s = mock(IDisposable.class);
            TestMultiThreadedWatchable w = new TestMultiThreadedWatchable(s, "one", "two", "three", null);

            AtomicWatchableSubscription as = new AtomicWatchableSubscription(s);
            BusyWatcher busyWatcher = new BusyWatcher();
            AtomicWatcherSingleThreaded<String> aw = new AtomicWatcherSingleThreaded<String>(busyWatcher, as);

            w.subscribe(aw);
            w.waitToFinish();

            System.out.println("maxConcurrentThreads: " + w.maxConcurrentThreads.get());

            // we can't know how many onNext calls will occur since they each run on a separate thread
            // that depends on thread scheduling so 0, 1, 2 and 3 are all valid options
            // assertEquals(3, busyWatcher.onNextCount.get());
            assertTrue(busyWatcher.onNextCount.get() < 4);
            assertTrue(busyWatcher.onError);
            // no onCompleted because onError was invoked
            assertFalse(busyWatcher.onCompleted);
            verify(s, never()).unsubscribe();

            // we can have concurrency ...
            assertTrue(w.maxConcurrentThreads.get() > 1);
            // ... but the onNext execution should be single threaded
            assertEquals(1, busyWatcher.maxConcurrentThreads.get());
        }

        @Test
        public void testMultiThreadedWithNPEinMiddle() {
            IDisposable s = mock(IDisposable.class);
            TestMultiThreadedWatchable w = new TestMultiThreadedWatchable(s, "one", "two", "three", null, "four", "five", "six", "seven", "eight", "nine");

            AtomicWatchableSubscription as = new AtomicWatchableSubscription(s);
            BusyWatcher busyWatcher = new BusyWatcher();
            AtomicWatcherSingleThreaded<String> aw = new AtomicWatcherSingleThreaded<String>(busyWatcher, as);

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

            // we can have concurrency ...
            assertTrue(w.maxConcurrentThreads.get() > 1);
            // ... but the onNext execution should be single threaded
            assertEquals(1, busyWatcher.maxConcurrentThreads.get());
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
                AtomicWatcherSingleThreaded<String> w = new AtomicWatcherSingleThreaded<String>(tw, s);

                Future<?> f1 = tp.submit(new OnNextThread(w, 12000));
                Future<?> f2 = tp.submit(new OnNextThread(w, 5000));
                Future<?> f3 = tp.submit(new OnNextThread(w, 75000));
                Future<?> f4 = tp.submit(new OnNextThread(w, 13500));
                Future<?> f5 = tp.submit(new OnNextThread(w, 22000));
                Future<?> f6 = tp.submit(new OnNextThread(w, 15000));
                Future<?> f7 = tp.submit(new OnNextThread(w, 7500));
                Future<?> f8 = tp.submit(new OnNextThread(w, 23500));

                Future<?> f10 = tp.submit(new CompletionThread(w, TestConcurrencyWatcherEvent.onCompleted, f1, f2, f3, f4));
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    // ignore
                }
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
                int numNextEvents = tw.assertEvents(null); // no check of type since we don't want to test barging results here, just interleaving behavior
                // System.out.println("Number of events executed: " + numNextEvents);
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
            public int assertEvents(TestConcurrencyWatcherEvent expectedEndingEvent) throws IllegalStateException {
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
                        if (expectedEndingEvent != null && TestConcurrencyWatcherEvent.onError != expectedEndingEvent) {
                            throw new IllegalStateException("Received onError ending event but expected " + expectedEndingEvent);
                        }
                        finished = true;
                    } else if (e == TestConcurrencyWatcherEvent.onCompleted) {
                        if (finished) {
                            // already finished, we shouldn't get this again
                            throw new IllegalStateException("Received onCompleted but we're already finished.");
                        }
                        if (expectedEndingEvent != null && TestConcurrencyWatcherEvent.onCompleted != expectedEndingEvent) {
                            throw new IllegalStateException("Received onCompleted ending event but expected " + expectedEndingEvent);
                        }
                        finished = true;
                    }
                }

                return nextCount;
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

    }

}