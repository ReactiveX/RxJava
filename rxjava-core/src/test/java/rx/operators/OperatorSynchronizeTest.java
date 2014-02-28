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
package rx.operators;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.Subscription;
import rx.observers.TestSubscriber;

public class OperatorSynchronizeTest {

    @Mock
    Observer<String> observer;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testSingleThreadedBasic() {
        Subscription s = mock(Subscription.class);
        TestSingleThreadedObservable onSubscribe = new TestSingleThreadedObservable(s, "one", "two", "three");
        Observable<String> w = Observable.create(onSubscribe);

        w.synchronize().subscribe(observer);
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
        Subscription s = mock(Subscription.class);
        TestMultiThreadedObservable onSubscribe = new TestMultiThreadedObservable(s, "one", "two", "three");
        Observable<String> w = Observable.create(onSubscribe);

        BusyObserver busyobserver = new BusyObserver();

        w.synchronize().subscribe(busyobserver);
        onSubscribe.waitToFinish();

        assertEquals(3, busyobserver.onNextCount.get());
        assertFalse(busyobserver.onError);
        assertTrue(busyobserver.onCompleted);
        // non-deterministic because unsubscribe happens after 'waitToFinish' releases
        // so commenting out for now as this is not a critical thing to test here
        //            verify(s, times(1)).unsubscribe();

        // we can have concurrency ...
        assertTrue(onSubscribe.maxConcurrentThreads.get() > 1);
        // ... but the onNext execution should be single threaded
        assertEquals(1, busyobserver.maxConcurrentThreads.get());
    }

    @Test
    public void testMultiThreadedBasicWithLock() {
        Subscription s = mock(Subscription.class);
        TestMultiThreadedObservable onSubscribe = new TestMultiThreadedObservable(s, "one", "two", "three");
        Observable<String> w = Observable.create(onSubscribe);

        BusyObserver busyobserver = new BusyObserver();

        Object lock = new Object();
        ExternalBusyThread externalBusyThread = new ExternalBusyThread(busyobserver, lock, 10, 100);

        externalBusyThread.start();

        w.synchronize(lock).subscribe(busyobserver);
        onSubscribe.waitToFinish();

        try {
            externalBusyThread.join(10000);
            assertFalse(externalBusyThread.isAlive());
            assertFalse(externalBusyThread.fail);
        } catch (InterruptedException e) {
            // ignore
        }

        assertEquals(3, busyobserver.onNextCount.get());
        assertFalse(busyobserver.onError);
        assertTrue(busyobserver.onCompleted);
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
        Subscription s = mock(Subscription.class);
        TestMultiThreadedObservable onSubscribe = new TestMultiThreadedObservable(s, "one", "two", "three", null);
        Observable<String> w = Observable.create(onSubscribe);

        BusyObserver busyobserver = new BusyObserver();

        w.synchronize().subscribe(busyobserver);
        onSubscribe.waitToFinish();

        System.out.println("maxConcurrentThreads: " + onSubscribe.maxConcurrentThreads.get());

        // we can't know how many onNext calls will occur since they each run on a separate thread
        // that depends on thread scheduling so 0, 1, 2 and 3 are all valid options
        // assertEquals(3, busyobserver.onNextCount.get());
        assertTrue(busyobserver.onNextCount.get() < 4);
        assertTrue(busyobserver.onError);
        // no onCompleted because onError was invoked
        assertFalse(busyobserver.onCompleted);
        // non-deterministic because unsubscribe happens after 'waitToFinish' releases
        // so commenting out for now as this is not a critical thing to test here
        //verify(s, times(1)).unsubscribe();

        // we can have concurrency ...
        assertTrue(onSubscribe.maxConcurrentThreads.get() > 1);
        // ... but the onNext execution should be single threaded
        assertEquals(1, busyobserver.maxConcurrentThreads.get());
    }

    @Test
    public void testMultiThreadedWithNPEAndLock() {
        Subscription s = mock(Subscription.class);
        TestMultiThreadedObservable onSubscribe = new TestMultiThreadedObservable(s, "one", "two", "three", null);
        Observable<String> w = Observable.create(onSubscribe);

        BusyObserver busyobserver = new BusyObserver();

        Object lock = new Object();
        ExternalBusyThread externalBusyThread = new ExternalBusyThread(busyobserver, lock, 10, 100);

        externalBusyThread.start();

        w.synchronize(lock).subscribe(busyobserver);
        onSubscribe.waitToFinish();

        try {
            externalBusyThread.join(10000);
            assertFalse(externalBusyThread.isAlive());
            assertFalse(externalBusyThread.fail);
        } catch (InterruptedException e) {
            // ignore
        }

        System.out.println("maxConcurrentThreads: " + onSubscribe.maxConcurrentThreads.get());

        // we can't know how many onNext calls will occur since they each run on a separate thread
        // that depends on thread scheduling so 0, 1, 2 and 3 are all valid options
        // assertEquals(3, busyobserver.onNextCount.get());
        assertTrue(busyobserver.onNextCount.get() < 4);
        assertTrue(busyobserver.onError);
        // no onCompleted because onError was invoked
        assertFalse(busyobserver.onCompleted);
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
        Subscription s = mock(Subscription.class);
        TestMultiThreadedObservable onSubscribe = new TestMultiThreadedObservable(s, "one", "two", "three", null, "four", "five", "six", "seven", "eight", "nine");
        Observable<String> w = Observable.create(onSubscribe);

        BusyObserver busyobserver = new BusyObserver();

        w.synchronize().subscribe(busyobserver);
        onSubscribe.waitToFinish();

        System.out.println("maxConcurrentThreads: " + onSubscribe.maxConcurrentThreads.get());
        // this should not be the full number of items since the error should stop it before it completes all 9
        System.out.println("onNext count: " + busyobserver.onNextCount.get());
        assertTrue(busyobserver.onNextCount.get() < 9);
        assertTrue(busyobserver.onError);
        // no onCompleted because onError was invoked
        assertFalse(busyobserver.onCompleted);
        // non-deterministic because unsubscribe happens after 'waitToFinish' releases
        // so commenting out for now as this is not a critical thing to test here
        // verify(s, times(1)).unsubscribe();

        // we can have concurrency ...
        assertTrue(onSubscribe.maxConcurrentThreads.get() > 1);
        // ... but the onNext execution should be single threaded
        assertEquals(1, busyobserver.maxConcurrentThreads.get());
    }

    @Test
    public void testMultiThreadedWithNPEinMiddleAndLock() {
        Subscription s = mock(Subscription.class);
        TestMultiThreadedObservable onSubscribe = new TestMultiThreadedObservable(s, "one", "two", "three", null, "four", "five", "six", "seven", "eight", "nine");
        Observable<String> w = Observable.create(onSubscribe);

        BusyObserver busyobserver = new BusyObserver();

        Object lock = new Object();
        ExternalBusyThread externalBusyThread = new ExternalBusyThread(busyobserver, lock, 10, 100);

        externalBusyThread.start();

        w.synchronize(lock).subscribe(busyobserver);
        onSubscribe.waitToFinish();

        try {
            externalBusyThread.join(10000);
            assertFalse(externalBusyThread.isAlive());
            assertFalse(externalBusyThread.fail);
        } catch (InterruptedException e) {
            // ignore
        }

        System.out.println("maxConcurrentThreads: " + onSubscribe.maxConcurrentThreads.get());
        // this should not be the full number of items since the error should stop it before it completes all 9
        System.out.println("onNext count: " + busyobserver.onNextCount.get());
        assertTrue(busyobserver.onNextCount.get() < 9);
        assertTrue(busyobserver.onError);
        // no onCompleted because onError was invoked
        assertFalse(busyobserver.onCompleted);
        // non-deterministic because unsubscribe happens after 'waitToFinish' releases
        // so commenting out for now as this is not a critical thing to test here
        // verify(s, times(1)).unsubscribe();

        // we can have concurrency ...
        assertTrue(onSubscribe.maxConcurrentThreads.get() > 1);
        // ... but the onNext execution should be single threaded
        assertEquals(1, busyobserver.maxConcurrentThreads.get());
    }

    /**
     * A thread that will pass data to onNext
     */
    public static class OnNextThread implements Runnable {

        private final Observer<String> observer;
        private final int numStringsToSend;

        OnNextThread(Observer<String> observer, int numStringsToSend) {
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
     * A thread that will call onError or onNext
     */
    public static class CompletionThread implements Runnable {

        private final Observer<String> observer;
        private final TestConcurrencyobserverEvent event;
        private final Future<?>[] waitOnThese;

        CompletionThread(Observer<String> observer, TestConcurrencyobserverEvent event, Future<?>... waitOnThese) {
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
            } else if (event == TestConcurrencyobserverEvent.onCompleted) {
                observer.onCompleted();

            } else {
                throw new IllegalArgumentException("Expecting either onError or onCompleted");
            }
        }
    }

    private static enum TestConcurrencyobserverEvent {
        onCompleted, onError, onNext
    }

    private static class TestConcurrencyobserver extends Subscriber<String> {

        /**
         * used to store the order and number of events received
         */
        private final LinkedBlockingQueue<TestConcurrencyobserverEvent> events = new LinkedBlockingQueue<TestConcurrencyobserverEvent>();
        private final int waitTime;

        @SuppressWarnings("unused")
        public TestConcurrencyobserver(int waitTimeInNext) {
            this.waitTime = waitTimeInNext;
        }

        public TestConcurrencyobserver() {
            this.waitTime = 0;
        }

        @Override
        public void onCompleted() {
            events.add(TestConcurrencyobserverEvent.onCompleted);
        }

        @Override
        public void onError(Throwable e) {
            events.add(TestConcurrencyobserverEvent.onError);
        }

        @Override
        public void onNext(String args) {
            events.add(TestConcurrencyobserverEvent.onNext);
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
        public int assertEvents(TestConcurrencyobserverEvent expectedEndingEvent) throws IllegalStateException {
            int nextCount = 0;
            boolean finished = false;
            for (TestConcurrencyobserverEvent e : events) {
                if (e == TestConcurrencyobserverEvent.onNext) {
                    if (finished) {
                        // already finished, we shouldn't get this again
                        throw new IllegalStateException("Received onNext but we're already finished.");
                    }
                    nextCount++;
                } else if (e == TestConcurrencyobserverEvent.onError) {
                    if (finished) {
                        // already finished, we shouldn't get this again
                        throw new IllegalStateException("Received onError but we're already finished.");
                    }
                    if (expectedEndingEvent != null && TestConcurrencyobserverEvent.onError != expectedEndingEvent) {
                        throw new IllegalStateException("Received onError ending event but expected " + expectedEndingEvent);
                    }
                    finished = true;
                } else if (e == TestConcurrencyobserverEvent.onCompleted) {
                    if (finished) {
                        // already finished, we shouldn't get this again
                        throw new IllegalStateException("Received onCompleted but we're already finished.");
                    }
                    if (expectedEndingEvent != null && TestConcurrencyobserverEvent.onCompleted != expectedEndingEvent) {
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
    private static class TestSingleThreadedObservable implements Observable.OnSubscribeFunc<String> {

        final Subscription s;
        final String[] values;
        private Thread t = null;

        public TestSingleThreadedObservable(final Subscription s, final String... values) {
            this.s = s;
            this.values = values;

        }

        public Subscription onSubscribe(final Observer<? super String> observer) {
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
     */
    private static class TestMultiThreadedObservable implements Observable.OnSubscribeFunc<String> {

        final Subscription s;
        final String[] values;
        Thread t = null;
        AtomicInteger threadsRunning = new AtomicInteger();
        AtomicInteger maxConcurrentThreads = new AtomicInteger();
        ExecutorService threadPool;

        public TestMultiThreadedObservable(Subscription s, String... values) {
            this.s = s;
            this.values = values;
            this.threadPool = Executors.newCachedThreadPool();
        }

        @Override
        public Subscription onSubscribe(final Observer<? super String> observer) {
            System.out.println("TestMultiThreadedObservable subscribed to ...");
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
                                        System.out.println("TestMultiThreadedObservable onNext: " + s);
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
                    observer.onCompleted();
                }
            });
            System.out.println("starting TestMultiThreadedObservable thread");
            t.start();
            System.out.println("done starting TestMultiThreadedObservable thread");
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

    private static class BusyObserver extends Subscriber<String> {
        volatile boolean onCompleted = false;
        volatile boolean onError = false;
        AtomicInteger onNextCount = new AtomicInteger();
        AtomicInteger threadsRunning = new AtomicInteger();
        AtomicInteger maxConcurrentThreads = new AtomicInteger();

        @Override
        public void onCompleted() {
            threadsRunning.incrementAndGet();

            System.out.println(">>> Busyobserver received onCompleted");
            onCompleted = true;

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

    private static class ExternalBusyThread extends Thread {

        private BusyObserver observer;
        private Object lock;
        private int lockTimes;
        private int waitTime;
        public volatile boolean fail;

        public ExternalBusyThread(BusyObserver observer, Object lock, int lockTimes, int waitTime) {
            this.observer = observer;
            this.lock = lock;
            this.lockTimes = lockTimes;
            this.waitTime = waitTime;
            this.fail = false;
        }

        @Override
        public void run() {
            Random r = new Random();
            for (int i = 0; i < lockTimes; i++) {
                synchronized (lock) {
                    int oldOnNextCount = observer.onNextCount.get();
                    boolean oldOnCompleted = observer.onCompleted;
                    boolean oldOnError = observer.onError;
                    try {
                        Thread.sleep(r.nextInt(waitTime));
                    } catch (InterruptedException e) {
                        // ignore
                    }
                    // Since we own the lock, onNextCount, onCompleted and
                    // onError must not be changed.
                    int newOnNextCount = observer.onNextCount.get();
                    boolean newOnCompleted = observer.onCompleted;
                    boolean newOnError = observer.onError;
                    if (oldOnNextCount != newOnNextCount) {
                        System.out.println(">>> ExternalBusyThread received different onNextCount: "
                                + oldOnNextCount
                                + " -> "
                                + newOnNextCount);
                        fail = true;
                        break;
                    }
                    if (oldOnCompleted != newOnCompleted) {
                        System.out.println(">>> ExternalBusyThread received different onCompleted: "
                                + oldOnCompleted
                                + " -> "
                                + newOnCompleted);
                        fail = true;
                        break;
                    }
                    if (oldOnError != newOnError) {
                        System.out.println(">>> ExternalBusyThread received different onError: "
                                + oldOnError
                                + " -> "
                                + newOnError);
                        fail = true;
                        break;
                    }
                }
            }
        }

    }
}
