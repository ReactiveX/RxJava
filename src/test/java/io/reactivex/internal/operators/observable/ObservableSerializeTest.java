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

package io.reactivex.internal.operators.observable;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.*;

import io.reactivex.*;
import io.reactivex.disposables.Disposables;
import io.reactivex.observers.DefaultObserver;

public class ObservableSerializeTest {

    Observer<String> observer;

    @Before
    public void before() {
        observer = TestHelper.mockObserver();
    }

    @Test
    public void testSingleThreadedBasic() {
        TestSingleThreadedObservable onSubscribe = new TestSingleThreadedObservable("one", "two", "three");
        Observable<String> w = Observable.unsafeCreate(onSubscribe);

        w.serialize().subscribe(observer);
        onSubscribe.waitToFinish();

        verify(observer, times(1)).onNext("one");
        verify(observer, times(1)).onNext("two");
        verify(observer, times(1)).onNext("three");
        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onComplete();
        // non-deterministic because unsubscribe happens after 'waitToFinish' releases
        // so commenting out for now as this is not a critical thing to test here
        //            verify(s, times(1)).unsubscribe();
    }

    @Test
    public void testMultiThreadedBasic() {
        TestMultiThreadedObservable onSubscribe = new TestMultiThreadedObservable("one", "two", "three");
        Observable<String> w = Observable.unsafeCreate(onSubscribe);

        BusyObserver busyobserver = new BusyObserver();

        w.serialize().subscribe(busyobserver);
        onSubscribe.waitToFinish();

        assertEquals(3, busyobserver.onNextCount.get());
        assertFalse(busyobserver.onError);
        assertTrue(busyobserver.onComplete);
        // non-deterministic because unsubscribe happens after 'waitToFinish' releases
        // so commenting out for now as this is not a critical thing to test here
        //            verify(s, times(1)).unsubscribe();

        // we can have concurrency ...
        assertTrue(onSubscribe.maxConcurrentThreads.get() > 1);
        // ... but the onNext execution should be single threaded
        assertEquals(1, busyobserver.maxConcurrentThreads.get());
    }

    @Test
    public void testMultiThreadedWithNPE() {
        TestMultiThreadedObservable onSubscribe = new TestMultiThreadedObservable("one", "two", "three", null);
        Observable<String> w = Observable.unsafeCreate(onSubscribe);

        BusyObserver busyobserver = new BusyObserver();

        w.serialize().subscribe(busyobserver);
        onSubscribe.waitToFinish();

        System.out.println("maxConcurrentThreads: " + onSubscribe.maxConcurrentThreads.get());

        // we can't know how many onNext calls will occur since they each run on a separate thread
        // that depends on thread scheduling so 0, 1, 2 and 3 are all valid options
        // assertEquals(3, busyobserver.onNextCount.get());
        assertTrue(busyobserver.onNextCount.get() < 4);
        assertTrue(busyobserver.onError);
        // no onComplete because onError was invoked
        assertFalse(busyobserver.onComplete);
        // non-deterministic because unsubscribe happens after 'waitToFinish' releases
        // so commenting out for now as this is not a critical thing to test here
        //verify(s, times(1)).unsubscribe();

        // we can have concurrency ...
        assertTrue(onSubscribe.maxConcurrentThreads.get() > 1);
        // ... but the onNext execution should be single threaded
        assertEquals(1, busyobserver.maxConcurrentThreads.get());
    }

    @Test
    public void testMultiThreadedWithNPEinMiddle() {
        boolean lessThan9 = false;
        for (int i = 0; i < 3; i++) {
            TestMultiThreadedObservable onSubscribe = new TestMultiThreadedObservable("one", "two", "three", null, "four", "five", "six", "seven", "eight", "nine");
            Observable<String> w = Observable.unsafeCreate(onSubscribe);

            BusyObserver busyobserver = new BusyObserver();

            w.serialize().subscribe(busyobserver);
            onSubscribe.waitToFinish();

            System.out.println("maxConcurrentThreads: " + onSubscribe.maxConcurrentThreads.get());
            // this should not always be the full number of items since the error should (very often)
            // stop it before it completes all 9
            System.out.println("onNext count: " + busyobserver.onNextCount.get());
            if (busyobserver.onNextCount.get() < 9) {
                lessThan9 = true;
            }
            assertTrue(busyobserver.onError);
            // no onComplete because onError was invoked
            assertFalse(busyobserver.onComplete);
            // non-deterministic because unsubscribe happens after 'waitToFinish' releases
            // so commenting out for now as this is not a critical thing to test here
            // verify(s, times(1)).unsubscribe();

            // we can have concurrency ...
            int n = onSubscribe.maxConcurrentThreads.get();
            assertTrue("" + n, n > 1);
            // ... but the onNext execution should be single threaded
            assertEquals(1, busyobserver.maxConcurrentThreads.get());
        }
        assertTrue(lessThan9);
    }

    /**
     * A thread that will pass data to onNext.
     */
    public static class OnNextThread implements Runnable {

        private final DefaultObserver<String> observer;
        private final int numStringsToSend;

        OnNextThread(DefaultObserver<String> observer, int numStringsToSend) {
            this.observer = observer;
            this.numStringsToSend = numStringsToSend;
        }

        @Override
        public void run() {
            for (int i = 0; i < numStringsToSend; i++) {
                observer.onNext("aString");
            }
        }
    }

    /**
     * A thread that will call onError or onNext.
     */
    public static class CompletionThread implements Runnable {

        private final DefaultObserver<String> observer;
        private final TestConcurrencyobserverEvent event;
        private final Future<?>[] waitOnThese;

        CompletionThread(DefaultObserver<String> observer, TestConcurrencyobserverEvent event, Future<?>... waitOnThese) {
            this.observer = observer;
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
            if (event == TestConcurrencyobserverEvent.onError) {
                observer.onError(new RuntimeException("mocked exception"));
            } else if (event == TestConcurrencyobserverEvent.onComplete) {
                observer.onComplete();

            } else {
                throw new IllegalArgumentException("Expecting either onError or onComplete");
            }
        }
    }

    enum TestConcurrencyobserverEvent {
        onComplete, onError, onNext
    }

    /**
     * This spawns a single thread for the subscribe execution.
     */
    static class TestSingleThreadedObservable implements ObservableSource<String> {

        final String[] values;
        private Thread t;

        TestSingleThreadedObservable(final String... values) {
            this.values = values;

        }

        @Override
        public void subscribe(final Observer<? super String> observer) {
            observer.onSubscribe(Disposables.empty());
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
                        observer.onComplete();
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
    private static class TestMultiThreadedObservable implements ObservableSource<String> {
        final String[] values;
        Thread t;
        AtomicInteger threadsRunning = new AtomicInteger();
        AtomicInteger maxConcurrentThreads = new AtomicInteger();
        ExecutorService threadPool;

        TestMultiThreadedObservable(String... values) {
            this.values = values;
            this.threadPool = Executors.newCachedThreadPool();
        }

        @Override
        public void subscribe(final Observer<? super String> observer) {
            observer.onSubscribe(Disposables.empty());
            System.out.println("TestMultiThreadedObservable subscribed to ...");
            final NullPointerException npe = new NullPointerException();
            t = new Thread(new Runnable() {

                @Override
                public void run() {
                    try {
                        System.out.println("running TestMultiThreadedObservable thread");
                        for (final String s : values) {
                            threadPool.execute(new Runnable() {

                                @Override
                                public void run() {
                                    threadsRunning.incrementAndGet();
                                    try {
                                        // perform onNext call
                                        if (s == null) {
                                            System.out.println("TestMultiThreadedObservable onNext: null");
                                            // force an error
                                            throw npe;
                                        } else {
                                            System.out.println("TestMultiThreadedObservable onNext: " + s);
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
                        threadPool.awaitTermination(2, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    observer.onComplete();
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

    private static class BusyObserver extends DefaultObserver<String> {
        volatile boolean onComplete;
        volatile boolean onError;
        AtomicInteger onNextCount = new AtomicInteger();
        AtomicInteger threadsRunning = new AtomicInteger();
        AtomicInteger maxConcurrentThreads = new AtomicInteger();

        @Override
        public void onComplete() {
            threadsRunning.incrementAndGet();

            System.out.println(">>> Busyobserver received onComplete");
            onComplete = true;

            int concurrentThreads = threadsRunning.get();
            int maxThreads = maxConcurrentThreads.get();
            if (concurrentThreads > maxThreads) {
                maxConcurrentThreads.compareAndSet(maxThreads, concurrentThreads);
            }
            threadsRunning.decrementAndGet();
        }

        @Override
        public void onError(Throwable e) {
            threadsRunning.incrementAndGet();

            System.out.println(">>> Busyobserver received onError: " + e.getMessage());
            onError = true;

            int concurrentThreads = threadsRunning.get();
            int maxThreads = maxConcurrentThreads.get();
            if (concurrentThreads > maxThreads) {
                maxConcurrentThreads.compareAndSet(maxThreads, concurrentThreads);
            }
            threadsRunning.decrementAndGet();
        }

        @Override
        public void onNext(String args) {
            threadsRunning.incrementAndGet();
            try {
                onNextCount.incrementAndGet();
                System.out.println(">>> Busyobserver received onNext: " + args);
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
