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
package rx.observers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Observer;
import rx.Subscriber;
import rx.Subscription;
import rx.schedulers.Schedulers;

public class SerializedObserverTest {

    @Mock
    Subscriber<String> observer;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
    }

    private Observer<String> serializedObserver(Observer<String> o) {
        return new SerializedObserver<String>(o);
    }

    @Test
    public void testSingleThreadedBasic() {
        TestSingleThreadedObservable onSubscribe = new TestSingleThreadedObservable("one", "two", "three");
        Observable<String> w = Observable.create(onSubscribe);

        Observer<String> aw = serializedObserver(observer);

        w.subscribe(aw);
        onSubscribe.waitToFinish();

        verify(observer, times(1)).onNext("one");
        verify(observer, times(1)).onNext("two");
        verify(observer, times(1)).onNext("three");
        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onCompleted();
        // non-deterministic because unsubscribe happens after 'waitToFinish' releases
        // so commenting out for now as this is not a critical thing to test here
        //            verify(s, times(1)).unsubscribe();
    }

    @Test
    public void testMultiThreadedBasic() {
        TestMultiThreadedObservable onSubscribe = new TestMultiThreadedObservable("one", "two", "three");
        Observable<String> w = Observable.create(onSubscribe);

        BusyObserver busyObserver = new BusyObserver();
        Observer<String> aw = serializedObserver(busyObserver);

        w.subscribe(aw);
        onSubscribe.waitToFinish();

        assertEquals(3, busyObserver.onNextCount.get());
        assertFalse(busyObserver.onError);
        assertTrue(busyObserver.onCompleted);
        // non-deterministic because unsubscribe happens after 'waitToFinish' releases
        // so commenting out for now as this is not a critical thing to test here
        //            verify(s, times(1)).unsubscribe();

        // we can have concurrency ...
        assertTrue(onSubscribe.maxConcurrentThreads.get() > 1);
        // ... but the onNext execution should be single threaded
        assertEquals(1, busyObserver.maxConcurrentThreads.get());
    }

    @Test(timeout = 1000)
    public void testMultiThreadedWithNPE() throws InterruptedException {
        TestMultiThreadedObservable onSubscribe = new TestMultiThreadedObservable("one", "two", "three", null);
        Observable<String> w = Observable.create(onSubscribe);

        BusyObserver busyObserver = new BusyObserver();
        Observer<String> aw = serializedObserver(busyObserver);

        w.subscribe(aw);
        onSubscribe.waitToFinish();
        busyObserver.terminalEvent.await();

        System.out.println("OnSubscribe maxConcurrentThreads: " + onSubscribe.maxConcurrentThreads.get() + "  Observer maxConcurrentThreads: " + busyObserver.maxConcurrentThreads.get());

        // we can't know how many onNext calls will occur since they each run on a separate thread
        // that depends on thread scheduling so 0, 1, 2 and 3 are all valid options
        // assertEquals(3, busyObserver.onNextCount.get());
        assertTrue(busyObserver.onNextCount.get() < 4);
        assertTrue(busyObserver.onError);
        // no onCompleted because onError was invoked
        assertFalse(busyObserver.onCompleted);
        // non-deterministic because unsubscribe happens after 'waitToFinish' releases
        // so commenting out for now as this is not a critical thing to test here
        //verify(s, times(1)).unsubscribe();

        // we can have concurrency ...
        assertTrue(onSubscribe.maxConcurrentThreads.get() > 1);
        // ... but the onNext execution should be single threaded
        assertEquals(1, busyObserver.maxConcurrentThreads.get());
    }

    @Test
    public void testMultiThreadedWithNPEinMiddle() {
        int n = 10;
        for (int i = 0; i < n; i++) {
            TestMultiThreadedObservable onSubscribe = new TestMultiThreadedObservable("one", "two", "three", null, 
                    "four", "five", "six", "seven", "eight", "nine");
            Observable<String> w = Observable.create(onSubscribe);

            BusyObserver busyObserver = new BusyObserver();
            Observer<String> aw = serializedObserver(busyObserver);

            w.subscribe(aw);
            onSubscribe.waitToFinish();

            System.out.println("OnSubscribe maxConcurrentThreads: " + onSubscribe.maxConcurrentThreads.get() + "  Observer maxConcurrentThreads: " + busyObserver.maxConcurrentThreads.get());

            // we can have concurrency ...
            assertTrue(onSubscribe.maxConcurrentThreads.get() > 1);
            // ... but the onNext execution should be single threaded
            assertEquals(1, busyObserver.maxConcurrentThreads.get());

            // this should not be the full number of items since the error should stop it before it completes all 9
            System.out.println("onNext count: " + busyObserver.onNextCount.get());
            assertFalse(busyObserver.onCompleted);
            assertTrue(busyObserver.onError);
            assertTrue(busyObserver.onNextCount.get() < 9);
            // no onCompleted because onError was invoked
            // non-deterministic because unsubscribe happens after 'waitToFinish' releases
            // so commenting out for now as this is not a critical thing to test here
            // verify(s, times(1)).unsubscribe();
        }
    }

    /**
     * A non-realistic use case that tries to expose thread-safety issues by throwing lots of out-of-order
     * events on many threads.
     */
    @Test
    public void runOutOfOrderConcurrencyTest() {
        ExecutorService tp = Executors.newFixedThreadPool(20);
        try {
            TestConcurrencyObserver tw = new TestConcurrencyObserver();
            // we need Synchronized + SafeSubscriber to handle synchronization plus life-cycle
            Observer<String> w = serializedObserver(new SafeSubscriber<String>(tw));

            Future<?> f1 = tp.submit(new OnNextThread(w, 12000));
            Future<?> f2 = tp.submit(new OnNextThread(w, 5000));
            Future<?> f3 = tp.submit(new OnNextThread(w, 75000));
            Future<?> f4 = tp.submit(new OnNextThread(w, 13500));
            Future<?> f5 = tp.submit(new OnNextThread(w, 22000));
            Future<?> f6 = tp.submit(new OnNextThread(w, 15000));
            Future<?> f7 = tp.submit(new OnNextThread(w, 7500));
            Future<?> f8 = tp.submit(new OnNextThread(w, 23500));

            Future<?> f10 = tp.submit(new CompletionThread(w, TestConcurrencyObserverEvent.onCompleted, f1, f2, f3, f4));
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                // ignore
            }
            Future<?> f11 = tp.submit(new CompletionThread(w, TestConcurrencyObserverEvent.onCompleted, f4, f6, f7));
            Future<?> f12 = tp.submit(new CompletionThread(w, TestConcurrencyObserverEvent.onCompleted, f4, f6, f7));
            Future<?> f13 = tp.submit(new CompletionThread(w, TestConcurrencyObserverEvent.onCompleted, f4, f6, f7));
            Future<?> f14 = tp.submit(new CompletionThread(w, TestConcurrencyObserverEvent.onCompleted, f4, f6, f7));
            // // the next 4 onError events should wait on same as f10
            Future<?> f15 = tp.submit(new CompletionThread(w, TestConcurrencyObserverEvent.onError, f1, f2, f3, f4));
            Future<?> f16 = tp.submit(new CompletionThread(w, TestConcurrencyObserverEvent.onError, f1, f2, f3, f4));
            Future<?> f17 = tp.submit(new CompletionThread(w, TestConcurrencyObserverEvent.onError, f1, f2, f3, f4));
            Future<?> f18 = tp.submit(new CompletionThread(w, TestConcurrencyObserverEvent.onError, f1, f2, f3, f4));

            waitOnThreads(f1, f2, f3, f4, f5, f6, f7, f8, f10, f11, f12, f13, f14, f15, f16, f17, f18);
            @SuppressWarnings("unused")
            int numNextEvents = tw.assertEvents(null); // no check of type since we don't want to test barging results here, just interleaving behavior
            //            System.out.println("Number of events executed: " + numNextEvents);
        } catch (Throwable e) {
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

    @Test
    public void runConcurrencyTest() {
        ExecutorService tp = Executors.newFixedThreadPool(20);
        try {
            TestConcurrencyObserver tw = new TestConcurrencyObserver();
            // we need Synchronized + SafeSubscriber to handle synchronization plus life-cycle
            Observer<String> w = serializedObserver(new SafeSubscriber<String>(tw));

            Future<?> f1 = tp.submit(new OnNextThread(w, 12000));
            Future<?> f2 = tp.submit(new OnNextThread(w, 5000));
            Future<?> f3 = tp.submit(new OnNextThread(w, 75000));
            Future<?> f4 = tp.submit(new OnNextThread(w, 13500));
            Future<?> f5 = tp.submit(new OnNextThread(w, 22000));
            Future<?> f6 = tp.submit(new OnNextThread(w, 15000));
            Future<?> f7 = tp.submit(new OnNextThread(w, 7500));
            Future<?> f8 = tp.submit(new OnNextThread(w, 23500));

            // 12000 + 5000 + 75000 + 13500 + 22000 + 15000 + 7500 + 23500 = 173500

            Future<?> f10 = tp.submit(new CompletionThread(w, TestConcurrencyObserverEvent.onCompleted, f1, f2, f3, f4, f5, f6, f7, f8));
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                // ignore
            }

            waitOnThreads(f1, f2, f3, f4, f5, f6, f7, f8, f10);
            int numNextEvents = tw.assertEvents(null); // no check of type since we don't want to test barging results here, just interleaving behavior
            assertEquals(173500, numNextEvents);
            // System.out.println("Number of events executed: " + numNextEvents);
        } catch (Throwable e) {
            fail("Concurrency test failed: " + e.getMessage());
            e.printStackTrace();
        } finally {
            tp.shutdown();
            try {
                tp.awaitTermination(25000, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Test that a notification does not get delayed in the queue waiting for the next event to push it through.
     * 
     * @throws InterruptedException
     */
    @Test
    public void testNotificationDelay() throws InterruptedException {
        ExecutorService tp = Executors.newFixedThreadPool(2);
        try {
            int n = 10;
            for (int i = 0; i < n; i++) {
                final CountDownLatch firstOnNext = new CountDownLatch(1);
                final CountDownLatch onNextCount = new CountDownLatch(2);
                final CountDownLatch latch = new CountDownLatch(1);
                final CountDownLatch running = new CountDownLatch(2);

                TestSubscriber<String> to = new TestSubscriber<String>(new Observer<String>() {

                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onNext(String t) {
                        firstOnNext.countDown();
                        // force it to take time when delivering so the second one is enqueued
                        try {
                            latch.await();
                        } catch (InterruptedException e) {
                        }
                    }

                });
                Observer<String> o = serializedObserver(to);

                Future<?> f1 = tp.submit(new OnNextThread(o, 1, onNextCount, running));
                Future<?> f2 = tp.submit(new OnNextThread(o, 1, onNextCount, running));

                running.await(); // let one of the OnNextThread actually run before proceeding
                
                firstOnNext.await();

                Thread t1 = to.getLastSeenThread();
                System.out.println("first onNext on thread: " + t1);

                latch.countDown();

                waitOnThreads(f1, f2);
                // not completed yet

                assertEquals(2, to.getOnNextEvents().size());

                Thread t2 = to.getLastSeenThread();
                System.out.println("second onNext on thread: " + t2);

                assertSame(t1, t2);

                System.out.println(to.getOnNextEvents());
                o.onCompleted();
                System.out.println(to.getOnNextEvents());
            }
        } finally {
            tp.shutdown();
        }
    }

    /**
     * Demonstrates thread starvation problem.
     * 
     * No solution on this for now. Trade-off in this direction as per https://github.com/Netflix/RxJava/issues/998#issuecomment-38959474
     * Probably need backpressure for this to work
     * 
     * When using SynchronizedObserver we get this output:
     * 
     * p1: 18 p2: 68 => should be close to each other unless we have thread starvation
     * 
     * When using SerializedObserver we get:
     * 
     * p1: 1 p2: 2445261 => should be close to each other unless we have thread starvation
     * 
     * This demonstrates how SynchronizedObserver balances back and forth better, and blocks emission.
     * The real issue in this example is the async buffer-bloat, so we need backpressure.
     * 
     * 
     * @throws InterruptedException
     */
    @Ignore
    @Test
    public void testThreadStarvation() throws InterruptedException {

        TestSubscriber<String> to = new TestSubscriber<String>(new Observer<String>() {

            @Override
            public void onCompleted() {

            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onNext(String t) {
                // force it to take time when delivering
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                }
            }

        });
        Observer<String> o = serializedObserver(to);

        AtomicInteger p1 = new AtomicInteger();
        AtomicInteger p2 = new AtomicInteger();

        Subscription s1 = infinite(p1).subscribe(o);
        Subscription s2 = infinite(p2).subscribe(o);

        Thread.sleep(100);

        System.out.println("p1: " + p1.get() + " p2: " + p2.get() + " => should be close to each other unless we have thread starvation");
        assertEquals(p1.get(), p2.get(), 10000); // fairly distributed within 10000 of each other

        s1.unsubscribe();
        s2.unsubscribe();
    }

    private static void waitOnThreads(Future<?>... futures) {
        for (Future<?> f : futures) {
            try {
                f.get(20, TimeUnit.SECONDS);
            } catch (Throwable e) {
                System.err.println("Failed while waiting on future.");
                e.printStackTrace();
            }
        }
    }

    private static Observable<String> infinite(final AtomicInteger produced) {
        return Observable.create(new OnSubscribe<String>() {

            @Override
            public void call(Subscriber<? super String> s) {
                while (!s.isUnsubscribed()) {
                    s.onNext("onNext");
                    produced.incrementAndGet();
                }
            }

        }).subscribeOn(Schedulers.newThread());
    }

    /**
     * A thread that will pass data to onNext
     */
    public static class OnNextThread implements Runnable {

        private final CountDownLatch latch;
        private final Observer<String> observer;
        private final int numStringsToSend;
        final AtomicInteger produced;
        private final CountDownLatch running;

        OnNextThread(Observer<String> observer, int numStringsToSend, CountDownLatch latch, CountDownLatch running) {
            this(observer, numStringsToSend, new AtomicInteger(), latch, running);
        }

        OnNextThread(Observer<String> observer, int numStringsToSend, AtomicInteger produced) {
            this(observer, numStringsToSend, produced, null, null);
        }

        OnNextThread(Observer<String> observer, int numStringsToSend, AtomicInteger produced, CountDownLatch latch, CountDownLatch running) {
            this.observer = observer;
            this.numStringsToSend = numStringsToSend;
            this.produced = produced;
            this.latch = latch;
            this.running = running;
        }

        OnNextThread(Observer<String> observer, int numStringsToSend) {
            this(observer, numStringsToSend, new AtomicInteger());
        }

        @Override
        public void run() {
            if (running != null) {
                running.countDown();
            }
            for (int i = 0; i < numStringsToSend; i++) {
                observer.onNext(Thread.currentThread().getId() + "-" + i);
                if (latch != null) {
                    latch.countDown();
                }
                produced.incrementAndGet();
            }
        }
    }

    /**
     * A thread that will call onError or onNext
     */
    public static class CompletionThread implements Runnable {

        private final Observer<String> observer;
        private final TestConcurrencyObserverEvent event;
        private final Future<?>[] waitOnThese;

        CompletionThread(Observer<String> Observer, TestConcurrencyObserverEvent event, Future<?>... waitOnThese) {
            this.observer = Observer;
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
                    } catch (Throwable e) {
                        System.err.println("Error while waiting on future in CompletionThread");
                    }
                }
            }

            /* send the event */
            if (event == TestConcurrencyObserverEvent.onError) {
                observer.onError(new RuntimeException("mocked exception"));
            } else if (event == TestConcurrencyObserverEvent.onCompleted) {
                observer.onCompleted();

            } else {
                throw new IllegalArgumentException("Expecting either onError or onCompleted");
            }
        }
    }

    private static enum TestConcurrencyObserverEvent {
        onCompleted, onError, onNext
    }

    private static class TestConcurrencyObserver extends Subscriber<String> {

        /**
         * used to store the order and number of events received
         */
        private final LinkedBlockingQueue<TestConcurrencyObserverEvent> events = new LinkedBlockingQueue<TestConcurrencyObserverEvent>();
        private final int waitTime;

        @SuppressWarnings("unused")
        public TestConcurrencyObserver(int waitTimeInNext) {
            this.waitTime = waitTimeInNext;
        }

        public TestConcurrencyObserver() {
            this.waitTime = 0;
        }

        @Override
        public void onCompleted() {
            events.add(TestConcurrencyObserverEvent.onCompleted);
        }

        @Override
        public void onError(Throwable e) {
            events.add(TestConcurrencyObserverEvent.onError);
        }

        @Override
        public void onNext(String args) {
            events.add(TestConcurrencyObserverEvent.onNext);
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
        public int assertEvents(TestConcurrencyObserverEvent expectedEndingEvent) throws IllegalStateException {
            int nextCount = 0;
            boolean finished = false;
            for (TestConcurrencyObserverEvent e : events) {
                if (e == TestConcurrencyObserverEvent.onNext) {
                    if (finished) {
                        // already finished, we shouldn't get this again
                        throw new IllegalStateException("Received onNext but we're already finished.");
                    }
                    nextCount++;
                } else if (e == TestConcurrencyObserverEvent.onError) {
                    if (finished) {
                        // already finished, we shouldn't get this again
                        throw new IllegalStateException("Received onError but we're already finished.");
                    }
                    if (expectedEndingEvent != null && TestConcurrencyObserverEvent.onError != expectedEndingEvent) {
                        throw new IllegalStateException("Received onError ending event but expected " + expectedEndingEvent);
                    }
                    finished = true;
                } else if (e == TestConcurrencyObserverEvent.onCompleted) {
                    if (finished) {
                        // already finished, we shouldn't get this again
                        throw new IllegalStateException("Received onCompleted but we're already finished.");
                    }
                    if (expectedEndingEvent != null && TestConcurrencyObserverEvent.onCompleted != expectedEndingEvent) {
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
     */
    private static class TestSingleThreadedObservable implements Observable.OnSubscribe<String> {

        final String[] values;
        private Thread t = null;

        public TestSingleThreadedObservable(final String... values) {
            this.values = values;

        }

        @Override
        public void call(final Subscriber<? super String> observer) {
            System.out.println("TestSingleThreadedObservable subscribed to ...");
            t = new Thread(new Runnable() {

                @Override
                public void run() {
                    try {
                        System.out.println("running TestSingleThreadedObservable thread");
                        for (String s : values) {
                            System.out.println("TestSingleThreadedObservable onNext: " + s);
                            observer.onNext(s);
                        }
                        observer.onCompleted();
                    } catch (Throwable e) {
                        throw new RuntimeException(e);
                    }
                }

            });
            System.out.println("starting TestSingleThreadedObservable thread");
            t.start();
            System.out.println("done starting TestSingleThreadedObservable thread");
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
     */
    private static class TestMultiThreadedObservable implements Observable.OnSubscribe<String> {

        final String[] values;
        Thread t = null;
        AtomicInteger threadsRunning = new AtomicInteger();
        AtomicInteger maxConcurrentThreads = new AtomicInteger();
        ExecutorService threadPool;

        public TestMultiThreadedObservable(String... values) {
            this.values = values;
            this.threadPool = Executors.newCachedThreadPool();
        }

        @Override
        public void call(final Subscriber<? super String> observer) {
            System.out.println("TestMultiThreadedObservable subscribed to ...");
            t = new Thread(new Runnable() {

                @Override
                public void run() {
                    try {
                        System.out.println("running TestMultiThreadedObservable thread");
                        int j = 0;
                        for (final String s : values) {
                            final int fj = ++j;
                            threadPool.execute(new Runnable() {

                                @Override
                                public void run() {
                                    threadsRunning.incrementAndGet();
                                    try {
                                        // perform onNext call
                                        System.out.println("TestMultiThreadedObservable onNext: " + s + " on thread " + Thread.currentThread().getName());
                                        if (s == null) {
                                            // force an error
                                            throw new NullPointerException();
                                        } else {
                                             // allow the exception to queue up
                                            int sleep = (fj % 3) * 10;
                                            if (sleep != 0) {
                                                Thread.sleep(sleep);
                                            }
                                        }
                                        observer.onNext(s);
                                        // capture 'maxThreads'
                                        int concurrentThreads = threadsRunning.get();
                                        int maxThreads = maxConcurrentThreads.get();
                                        if (concurrentThreads > maxThreads) {
                                            maxConcurrentThreads.compareAndSet(maxThreads, concurrentThreads);
                                        }
                                    } catch (Throwable e) {
                                        observer.onError(e);
                                    } finally {
                                        threadsRunning.decrementAndGet();
                                    }
                                }
                            });
                        }
                        // we are done spawning threads
                        threadPool.shutdown();
                    } catch (Throwable e) {
                        throw new RuntimeException(e);
                    }

                    // wait until all threads are done, then mark it as COMPLETED
                    try {
                        // wait for all the threads to finish
                        if (!threadPool.awaitTermination(5, TimeUnit.SECONDS)) {
                            System.out.println("Threadpool did not terminate in time.");
                        }
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    observer.onCompleted();
                }
            });
            System.out.println("starting TestMultiThreadedObservable thread");
            t.start();
            System.out.println("done starting TestMultiThreadedObservable thread");
        }

        public void waitToFinish() {
            try {
                t.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static class BusyObserver extends Subscriber<String> {
        volatile boolean onCompleted = false;
        volatile boolean onError = false;
        AtomicInteger onNextCount = new AtomicInteger();
        AtomicInteger threadsRunning = new AtomicInteger();
        AtomicInteger maxConcurrentThreads = new AtomicInteger();
        final CountDownLatch terminalEvent = new CountDownLatch(1);

        @Override
        public void onCompleted() {
            threadsRunning.incrementAndGet();
            try {
                onCompleted = true;
            } finally {
                captureMaxThreads();
                threadsRunning.decrementAndGet();
                terminalEvent.countDown();
            }
        }

        @Override
        public void onError(Throwable e) {
            System.out.println(">>>>>>>>>>>>>>>>>>>> onError received: " + e);
            threadsRunning.incrementAndGet();
            try {
                onError = true;
            } finally {
                captureMaxThreads();
                threadsRunning.decrementAndGet();
                terminalEvent.countDown();
            }
        }

        @Override
        public void onNext(String args) {
            threadsRunning.incrementAndGet();
            try {
                onNextCount.incrementAndGet();
                try {
                    // simulate doing something computational
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } finally {
                // capture 'maxThreads'
                captureMaxThreads();
                threadsRunning.decrementAndGet();
            }
        }

        protected void captureMaxThreads() {
            int concurrentThreads = threadsRunning.get();
            int maxThreads = maxConcurrentThreads.get();
            if (concurrentThreads > maxThreads) {
                maxConcurrentThreads.compareAndSet(maxThreads, concurrentThreads);
                if (concurrentThreads > 1) {
                    new RuntimeException("should not be greater than 1").printStackTrace();
                }
            }
        }

    }
}
