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
package rx.internal.operators;

import static java.util.Arrays.asList;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.junit.*;
import org.mockito.*;

import rx.*;
import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Observer;
import rx.Scheduler.Worker;
import rx.functions.*;
import rx.internal.util.RxRingBuffer;
import rx.observers.TestSubscriber;
import rx.schedulers.*;
import rx.subscriptions.Subscriptions;

public class OperatorMergeTest {

    @Mock
    Observer<String> stringObserver;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testMergeObservableOfObservables() {
        final Observable<String> o1 = Observable.create(new TestSynchronousObservable());
        final Observable<String> o2 = Observable.create(new TestSynchronousObservable());

        Observable<Observable<String>> observableOfObservables = Observable.create(new Observable.OnSubscribe<Observable<String>>() {

            @Override
            public void call(Subscriber<? super Observable<String>> observer) {
                // simulate what would happen in an observable
                observer.onNext(o1);
                observer.onNext(o2);
                observer.onCompleted();
            }

        });
        Observable<String> m = Observable.merge(observableOfObservables);
        m.subscribe(stringObserver);

        verify(stringObserver, never()).onError(any(Throwable.class));
        verify(stringObserver, times(1)).onCompleted();
        verify(stringObserver, times(2)).onNext("hello");
    }

    @Test
    public void testMergeArray() {
        final Observable<String> o1 = Observable.create(new TestSynchronousObservable());
        final Observable<String> o2 = Observable.create(new TestSynchronousObservable());

        Observable<String> m = Observable.merge(o1, o2);
        m.subscribe(stringObserver);

        verify(stringObserver, never()).onError(any(Throwable.class));
        verify(stringObserver, times(2)).onNext("hello");
        verify(stringObserver, times(1)).onCompleted();
    }

    @Test
    public void testMergeList() {
        final Observable<String> o1 = Observable.create(new TestSynchronousObservable());
        final Observable<String> o2 = Observable.create(new TestSynchronousObservable());
        List<Observable<String>> listOfObservables = new ArrayList<Observable<String>>();
        listOfObservables.add(o1);
        listOfObservables.add(o2);

        Observable<String> m = Observable.merge(listOfObservables);
        m.subscribe(stringObserver);

        verify(stringObserver, never()).onError(any(Throwable.class));
        verify(stringObserver, times(1)).onCompleted();
        verify(stringObserver, times(2)).onNext("hello");
    }

    @Test(timeout = 1000)
    public void testUnSubscribeObservableOfObservables() throws InterruptedException {

        final AtomicBoolean unsubscribed = new AtomicBoolean();
        final CountDownLatch latch = new CountDownLatch(1);

        Observable<Observable<Long>> source = Observable.create(new Observable.OnSubscribe<Observable<Long>>() {

            @Override
            public void call(final Subscriber<? super Observable<Long>> observer) {
                // verbose on purpose so I can track the inside of it
                final Subscription s = Subscriptions.create(new Action0() {

                    @Override
                    public void call() {
                        System.out.println("*** unsubscribed");
                        unsubscribed.set(true);
                    }

                });
                observer.add(s);

                new Thread(new Runnable() {

                    @Override
                    public void run() {

                        while (!unsubscribed.get()) {
                            observer.onNext(Observable.just(1L, 2L));
                        }
                        System.out.println("Done looping after unsubscribe: " + unsubscribed.get());
                        observer.onCompleted();

                        // mark that the thread is finished
                        latch.countDown();
                    }
                }).start();
            }

        });

        final AtomicInteger count = new AtomicInteger();
        Observable.merge(source).take(6).toBlocking().forEach(new Action1<Long>() {

            @Override
            public void call(Long v) {
                System.out.println("Value: " + v);
                int c = count.incrementAndGet();
                if (c > 6) {
                    fail("Should be only 6");
                }

            }
        });

        latch.await(1000, TimeUnit.MILLISECONDS);

        System.out.println("unsubscribed: " + unsubscribed.get());

        assertTrue(unsubscribed.get());

    }

    @Test
    public void testMergeArrayWithThreading() {
        final TestASynchronousObservable o1 = new TestASynchronousObservable();
        final TestASynchronousObservable o2 = new TestASynchronousObservable();

        Observable<String> m = Observable.merge(Observable.create(o1), Observable.create(o2));
        TestSubscriber<String> ts = new TestSubscriber<String>(stringObserver);
        m.subscribe(ts);

        ts.awaitTerminalEvent();
        ts.assertNoErrors();

        verify(stringObserver, never()).onError(any(Throwable.class));
        verify(stringObserver, times(2)).onNext("hello");
        verify(stringObserver, times(1)).onCompleted();
    }

    @Test
    public void testSynchronizationOfMultipleSequences() throws Throwable {
        final TestASynchronousObservable o1 = new TestASynchronousObservable();
        final TestASynchronousObservable o2 = new TestASynchronousObservable();

        // use this latch to cause onNext to wait until we're ready to let it go
        final CountDownLatch endLatch = new CountDownLatch(1);

        final AtomicInteger concurrentCounter = new AtomicInteger();
        final AtomicInteger totalCounter = new AtomicInteger();

        Observable<String> m = Observable.merge(Observable.create(o1), Observable.create(o2));
        m.subscribe(new Subscriber<String>() {

            @Override
            public void onCompleted() {

            }

            @Override
            public void onError(Throwable e) {
                throw new RuntimeException("failed", e);
            }

            @Override
            public void onNext(String v) {
                totalCounter.incrementAndGet();
                concurrentCounter.incrementAndGet();
                try {
                    // wait here until we're done asserting
                    endLatch.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    throw new RuntimeException("failed", e);
                } finally {
                    concurrentCounter.decrementAndGet();
                }
            }

        });

        // wait for both observables to send (one should be blocked)
        o1.onNextBeingSent.await();
        o2.onNextBeingSent.await();

        // I can't think of a way to know for sure that both threads have or are trying to send onNext
        // since I can't use a CountDownLatch for "after" onNext since I want to catch during it
        // but I can't know for sure onNext is invoked
        // so I'm unfortunately reverting to using a Thread.sleep to allow the process scheduler time
        // to make sure after o1.onNextBeingSent and o2.onNextBeingSent are hit that the following
        // onNext is invoked.

        Thread.sleep(300);

        try { // in try/finally so threads are released via latch countDown even if assertion fails
            assertEquals(1, concurrentCounter.get());
        } finally {
            // release so it can finish
            endLatch.countDown();
        }

        try {
            o1.t.join();
            o2.t.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        assertEquals(2, totalCounter.get());
        assertEquals(0, concurrentCounter.get());
    }

    /**
     * unit test from OperationMergeDelayError backported here to show how these use cases work with normal merge
     */
    @Test
    public void testError1() {
        // we are using synchronous execution to test this exactly rather than non-deterministic concurrent behavior
        final Observable<String> o1 = Observable.create(new TestErrorObservable("four", null, "six")); // we expect to lose "six"
        final Observable<String> o2 = Observable.create(new TestErrorObservable("one", "two", "three")); // we expect to lose all of these since o1 is done first and fails

        Observable<String> m = Observable.merge(o1, o2);
        m.subscribe(stringObserver);

        verify(stringObserver, times(1)).onError(any(NullPointerException.class));
        verify(stringObserver, never()).onCompleted();
        verify(stringObserver, times(0)).onNext("one");
        verify(stringObserver, times(0)).onNext("two");
        verify(stringObserver, times(0)).onNext("three");
        verify(stringObserver, times(1)).onNext("four");
        verify(stringObserver, times(0)).onNext("five");
        verify(stringObserver, times(0)).onNext("six");
    }

    /**
     * unit test from OperationMergeDelayError backported here to show how these use cases work with normal merge
     */
    @Test
    public void testError2() {
        // we are using synchronous execution to test this exactly rather than non-deterministic concurrent behavior
        final Observable<String> o1 = Observable.create(new TestErrorObservable("one", "two", "three"));
        final Observable<String> o2 = Observable.create(new TestErrorObservable("four", null, "six")); // we expect to lose "six"
        final Observable<String> o3 = Observable.create(new TestErrorObservable("seven", "eight", null));// we expect to lose all of these since o2 is done first and fails
        final Observable<String> o4 = Observable.create(new TestErrorObservable("nine"));// we expect to lose all of these since o2 is done first and fails

        Observable<String> m = Observable.merge(o1, o2, o3, o4);
        m.subscribe(stringObserver);

        verify(stringObserver, times(1)).onError(any(NullPointerException.class));
        verify(stringObserver, never()).onCompleted();
        verify(stringObserver, times(1)).onNext("one");
        verify(stringObserver, times(1)).onNext("two");
        verify(stringObserver, times(1)).onNext("three");
        verify(stringObserver, times(1)).onNext("four");
        verify(stringObserver, times(0)).onNext("five");
        verify(stringObserver, times(0)).onNext("six");
        verify(stringObserver, times(0)).onNext("seven");
        verify(stringObserver, times(0)).onNext("eight");
        verify(stringObserver, times(0)).onNext("nine");
    }

    @Test
    public void testThrownErrorHandling() {
        TestSubscriber<String> ts = new TestSubscriber<String>();
        Observable<String> o1 = Observable.create(new OnSubscribe<String>() {

            @Override
            public void call(Subscriber<? super String> s) {
                throw new RuntimeException("fail");
            }

        });

        Observable.merge(o1, o1).subscribe(ts);
        ts.awaitTerminalEvent(1000, TimeUnit.MILLISECONDS);
        ts.assertTerminalEvent();
        System.out.println("Error: " + ts.getOnErrorEvents());
    }

    private static class TestSynchronousObservable implements Observable.OnSubscribe<String> {

        @Override
        public void call(Subscriber<? super String> observer) {

            observer.onNext("hello");
            observer.onCompleted();
        }
    }

    private static class TestASynchronousObservable implements Observable.OnSubscribe<String> {
        Thread t;
        final CountDownLatch onNextBeingSent = new CountDownLatch(1);

        @Override
        public void call(final Subscriber<? super String> observer) {
            t = new Thread(new Runnable() {

                @Override
                public void run() {
                    onNextBeingSent.countDown();
                    try {
                        observer.onNext("hello");
                        // I can't use a countDownLatch to prove we are actually sending 'onNext'
                        // since it will block if synchronized and I'll deadlock
                        observer.onCompleted();
                    } catch (Exception e) {
                        observer.onError(e);
                    }
                }

            });
            t.start();
        }
    }

    private static class TestErrorObservable implements Observable.OnSubscribe<String> {

        String[] valuesToReturn;

        TestErrorObservable(String... values) {
            valuesToReturn = values;
        }

        @Override
        public void call(Subscriber<? super String> observer) {

            for (String s : valuesToReturn) {
                if (s == null) {
                    System.out.println("throwing exception");
                    observer.onError(new NullPointerException());
                } else {
                    observer.onNext(s);
                }
            }
            observer.onCompleted();
        }
    }

    @Test
    public void testUnsubscribeAsObservablesComplete() {
        TestScheduler scheduler1 = Schedulers.test();
        AtomicBoolean os1 = new AtomicBoolean(false);
        Observable<Long> o1 = createObservableOf5IntervalsOf1SecondIncrementsWithSubscriptionHook(scheduler1, os1);

        TestScheduler scheduler2 = Schedulers.test();
        AtomicBoolean os2 = new AtomicBoolean(false);
        Observable<Long> o2 = createObservableOf5IntervalsOf1SecondIncrementsWithSubscriptionHook(scheduler2, os2);

        TestSubscriber<Long> ts = new TestSubscriber<Long>();
        Observable.merge(o1, o2).subscribe(ts);

        // we haven't incremented time so nothing should be received yet
        ts.assertReceivedOnNext(Collections.<Long> emptyList());

        scheduler1.advanceTimeBy(3, TimeUnit.SECONDS);
        scheduler2.advanceTimeBy(2, TimeUnit.SECONDS);

        ts.assertReceivedOnNext(Arrays.asList(0L, 1L, 2L, 0L, 1L));
        // not unsubscribed yet
        assertFalse(os1.get());
        assertFalse(os2.get());

        // advance to the end at which point it should complete
        scheduler1.advanceTimeBy(3, TimeUnit.SECONDS);

        ts.assertReceivedOnNext(Arrays.asList(0L, 1L, 2L, 0L, 1L, 3L, 4L));
        assertTrue(os1.get());
        assertFalse(os2.get());

        // both should be completed now
        scheduler2.advanceTimeBy(3, TimeUnit.SECONDS);

        ts.assertReceivedOnNext(Arrays.asList(0L, 1L, 2L, 0L, 1L, 3L, 4L, 2L, 3L, 4L));
        assertTrue(os1.get());
        assertTrue(os2.get());

        ts.assertTerminalEvent();
    }

    @Test
    public void testEarlyUnsubscribe() {
        for (int i = 0; i < 10; i++) {
            TestScheduler scheduler1 = Schedulers.test();
            AtomicBoolean os1 = new AtomicBoolean(false);
            Observable<Long> o1 = createObservableOf5IntervalsOf1SecondIncrementsWithSubscriptionHook(scheduler1, os1);

            TestScheduler scheduler2 = Schedulers.test();
            AtomicBoolean os2 = new AtomicBoolean(false);
            Observable<Long> o2 = createObservableOf5IntervalsOf1SecondIncrementsWithSubscriptionHook(scheduler2, os2);

            TestSubscriber<Long> ts = new TestSubscriber<Long>();
            Subscription s = Observable.merge(o1, o2).subscribe(ts);

            // we haven't incremented time so nothing should be received yet
            ts.assertReceivedOnNext(Collections.<Long> emptyList());

            scheduler1.advanceTimeBy(3, TimeUnit.SECONDS);
            scheduler2.advanceTimeBy(2, TimeUnit.SECONDS);

            ts.assertReceivedOnNext(Arrays.asList(0L, 1L, 2L, 0L, 1L));
            // not unsubscribed yet
            assertFalse(os1.get());
            assertFalse(os2.get());

            // early unsubscribe
            s.unsubscribe();

            assertTrue(os1.get());
            assertTrue(os2.get());

            ts.assertReceivedOnNext(Arrays.asList(0L, 1L, 2L, 0L, 1L));
            ts.assertUnsubscribed();
        }
    }

    private Observable<Long> createObservableOf5IntervalsOf1SecondIncrementsWithSubscriptionHook(final Scheduler scheduler, final AtomicBoolean unsubscribed) {
        return Observable.create(new OnSubscribe<Long>() {

            @Override
            public void call(Subscriber<? super Long> s) {
                s.add(Subscriptions.create(new Action0() {

                    @Override
                    public void call() {
                        unsubscribed.set(true);
                    }

                }));
                Observable.interval(1, TimeUnit.SECONDS, scheduler).take(5).subscribe(s);
            }
        });
    }

    @Test//(timeout = 10000)
    public void testConcurrency() {
        Observable<Integer> o = Observable.range(1, 10000).subscribeOn(Schedulers.newThread());

        for (int i = 0; i < 10; i++) {
            Observable<Integer> merge = Observable.merge(o.onBackpressureBuffer(), o.onBackpressureBuffer(), o.onBackpressureBuffer());
            TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
            merge.subscribe(ts);

            ts.awaitTerminalEvent(3, TimeUnit.SECONDS);
            ts.assertTerminalEvent();
            ts.assertNoErrors();
            assertEquals(1, ts.getOnCompletedEvents().size());
            List<Integer> onNextEvents = ts.getOnNextEvents();
            assertEquals(30000, onNextEvents.size());
            //            System.out.println("onNext: " + onNextEvents.size() + " onCompleted: " + ts.getOnCompletedEvents().size());
        }
    }

    @Test
    public void testConcurrencyWithSleeping() {

        Observable<Integer> o = Observable.create(new OnSubscribe<Integer>() {

            @Override
            public void call(final Subscriber<? super Integer> s) {
                Worker inner = Schedulers.newThread().createWorker();
                s.add(inner);
                inner.schedule(new Action0() {

                    @Override
                    public void call() {
                        try {
                            for (int i = 0; i < 100; i++) {
                                s.onNext(1);
                                try {
                                    Thread.sleep(1);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        } catch (Exception e) {
                            s.onError(e);
                        }
                        s.onCompleted();
                    }

                });
            }
        });

        for (int i = 0; i < 10; i++) {
            Observable<Integer> merge = Observable.merge(o, o, o);
            TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
            merge.subscribe(ts);

            ts.awaitTerminalEvent();
            assertEquals(1, ts.getOnCompletedEvents().size());
            List<Integer> onNextEvents = ts.getOnNextEvents();
            assertEquals(300, onNextEvents.size());
            //            System.out.println("onNext: " + onNextEvents.size() + " onCompleted: " + ts.getOnCompletedEvents().size());
        }
    }

    @Test
    public void testConcurrencyWithBrokenOnCompleteContract() {
        Observable<Integer> o = Observable.create(new OnSubscribe<Integer>() {

            @Override
            public void call(final Subscriber<? super Integer> s) {
                Worker inner = Schedulers.newThread().createWorker();
                s.add(inner);
                inner.schedule(new Action0() {

                    @Override
                    public void call() {
                        try {
                            for (int i = 0; i < 10000; i++) {
                                s.onNext(i);
                            }
                        } catch (Exception e) {
                            s.onError(e);
                        }
                        s.onCompleted();
                        s.onCompleted();
                        s.onCompleted();
                    }

                });
            }
        });

        for (int i = 0; i < 10; i++) {
            Observable<Integer> merge = Observable.merge(o.onBackpressureBuffer(), o.onBackpressureBuffer(), o.onBackpressureBuffer());
            TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
            merge.subscribe(ts);

            ts.awaitTerminalEvent();
            ts.assertNoErrors();
            assertEquals(1, ts.getOnCompletedEvents().size());
            List<Integer> onNextEvents = ts.getOnNextEvents();
            assertEquals(30000, onNextEvents.size());
            //                System.out.println("onNext: " + onNextEvents.size() + " onCompleted: " + ts.getOnCompletedEvents().size());
        }
    }

    @Test
    public void testBackpressureUpstream() throws InterruptedException {
        final AtomicInteger generated1 = new AtomicInteger();
        Observable<Integer> o1 = createInfiniteObservable(generated1).subscribeOn(Schedulers.computation());
        final AtomicInteger generated2 = new AtomicInteger();
        Observable<Integer> o2 = createInfiniteObservable(generated2).subscribeOn(Schedulers.computation());

        TestSubscriber<Integer> testSubscriber = new TestSubscriber<Integer>() {
            @Override
            public void onNext(Integer t) {
                System.err.println("testSubscriber received => " + t + "  on thread " + Thread.currentThread());
                super.onNext(t);
            }
        };

        Observable.merge(o1.take(RxRingBuffer.SIZE * 2), o2.take(RxRingBuffer.SIZE * 2)).subscribe(testSubscriber);
        testSubscriber.awaitTerminalEvent();
        if (testSubscriber.getOnErrorEvents().size() > 0) {
            testSubscriber.getOnErrorEvents().get(0).printStackTrace();
        }
        testSubscriber.assertNoErrors();
        System.err.println(testSubscriber.getOnNextEvents());
        assertEquals(RxRingBuffer.SIZE * 4, testSubscriber.getOnNextEvents().size());
        // it should be between the take num and requested batch size across the async boundary
        System.out.println("Generated 1: " + generated1.get());
        System.out.println("Generated 2: " + generated2.get());
        assertTrue(generated1.get() >= RxRingBuffer.SIZE * 2 && generated1.get() <= RxRingBuffer.SIZE * 4);
    }

    @Test
    public void testBackpressureUpstream2InLoop() throws InterruptedException {
        for (int i = 0; i < 1000; i++) {
            System.err.flush();
            System.out.println("---");
            System.out.flush();
            testBackpressureUpstream2();
        }
    }
    
    @Test
    public void testBackpressureUpstream2() throws InterruptedException {
        final AtomicInteger generated1 = new AtomicInteger();
        Observable<Integer> o1 = createInfiniteObservable(generated1).subscribeOn(Schedulers.computation());

        TestSubscriber<Integer> testSubscriber = new TestSubscriber<Integer>() {
            @Override
            public void onNext(Integer t) {
                super.onNext(t);
            }
        };

        Observable.merge(o1.take(RxRingBuffer.SIZE * 2), Observable.just(-99)).subscribe(testSubscriber);
        testSubscriber.awaitTerminalEvent();
        
        List<Integer> onNextEvents = testSubscriber.getOnNextEvents();
        
        System.out.println("Generated 1: " + generated1.get() + " / received: " + onNextEvents.size());
        System.out.println(onNextEvents);

        if (testSubscriber.getOnErrorEvents().size() > 0) {
            testSubscriber.getOnErrorEvents().get(0).printStackTrace();
        }
        testSubscriber.assertNoErrors();
        assertEquals(RxRingBuffer.SIZE * 2 + 1, onNextEvents.size());
        // it should be between the take num and requested batch size across the async boundary
        assertTrue(generated1.get() >= RxRingBuffer.SIZE * 2 && generated1.get() <= RxRingBuffer.SIZE * 3);
    }

    /**
     * This is the same as the upstreams ones, but now adds the downstream as well by using observeOn.
     * 
     * This requires merge to also obey the Product.request values coming from it's child subscriber.
     * @throws InterruptedException if the wait is interrupted
     */
    @Test(timeout = 10000)
    public void testBackpressureDownstreamWithConcurrentStreams() throws InterruptedException {
        final AtomicInteger generated1 = new AtomicInteger();
        Observable<Integer> o1 = createInfiniteObservable(generated1).subscribeOn(Schedulers.computation());
        final AtomicInteger generated2 = new AtomicInteger();
        Observable<Integer> o2 = createInfiniteObservable(generated2).subscribeOn(Schedulers.computation());

        TestSubscriber<Integer> testSubscriber = new TestSubscriber<Integer>() {
            @Override
            public void onNext(Integer t) {
                if (t < 100)
                    try {
                        // force a slow consumer
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                //                System.err.println("testSubscriber received => " + t + "  on thread " + Thread.currentThread());
                super.onNext(t);
            }
        };

        Observable.merge(o1.take(RxRingBuffer.SIZE * 2), o2.take(RxRingBuffer.SIZE * 2)).observeOn(Schedulers.computation()).subscribe(testSubscriber);
        testSubscriber.awaitTerminalEvent();
        if (testSubscriber.getOnErrorEvents().size() > 0) {
            testSubscriber.getOnErrorEvents().get(0).printStackTrace();
        }
        testSubscriber.assertNoErrors();
        System.err.println(testSubscriber.getOnNextEvents());
        assertEquals(RxRingBuffer.SIZE * 4, testSubscriber.getOnNextEvents().size());
        // it should be between the take num and requested batch size across the async boundary
        System.out.println("Generated 1: " + generated1.get());
        System.out.println("Generated 2: " + generated2.get());
        assertTrue(generated1.get() >= RxRingBuffer.SIZE * 2 && generated1.get() <= RxRingBuffer.SIZE * 4);
    }

    @Test
    public void testBackpressureBothUpstreamAndDownstreamWithSynchronousScalarObservables() throws InterruptedException {
        final AtomicInteger generated1 = new AtomicInteger();
        Observable<Observable<Integer>> o1 = createInfiniteObservable(generated1).map(new Func1<Integer, Observable<Integer>>() {

            @Override
            public Observable<Integer> call(Integer t1) {
                return Observable.just(t1);
            }

        });

        TestSubscriber<Integer> testSubscriber = new TestSubscriber<Integer>() {
            @Override
            public void onNext(Integer t) {
                if (t < 100)
                    try {
                        // force a slow consumer
                        Thread.sleep(2);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                //                System.err.println("testSubscriber received => " + t + "  on thread " + Thread.currentThread());
                super.onNext(t);
            }
        };

        int limit = RxRingBuffer.SIZE; // the default unbounded behavior makes this test fail 100% of the time: source is too fast
        Observable.merge(o1, limit).observeOn(Schedulers.computation()).take(RxRingBuffer.SIZE * 2).subscribe(testSubscriber);
        testSubscriber.awaitTerminalEvent();
        if (testSubscriber.getOnErrorEvents().size() > 0) {
            testSubscriber.getOnErrorEvents().get(0).printStackTrace();
        }
        testSubscriber.assertNoErrors();
        System.out.println("Generated 1: " + generated1.get());
        System.err.println(testSubscriber.getOnNextEvents());
        assertEquals(RxRingBuffer.SIZE * 2, testSubscriber.getOnNextEvents().size());
        // it should be between the take num and requested batch size across the async boundary
        assertTrue(generated1.get() >= RxRingBuffer.SIZE * 2 && generated1.get() <= RxRingBuffer.SIZE * 4);
    }

    /**
     * Currently there is no solution to this ... we can't exert backpressure on the outer Observable if we
     * can't know if the ones we've received so far are going to emit or not, otherwise we could starve the system.
     * 
     * For example, 10,000 Observables are being merged (bad use case to begin with, but ...) and it's only one of them
     * that will ever emit. If backpressure only allowed the first 1,000 to be sent, we would hang and never receive an event.
     * 
     * Thus, we must allow all Observables to be sent. The ScalarSynchronousObservable use case is an exception to this since
     * we can grab the value synchronously.
     * 
     * @throws InterruptedException
     */
    @Test(timeout = 5000)
    public void testBackpressureBothUpstreamAndDownstreamWithRegularObservables() throws InterruptedException {
        final AtomicInteger generated1 = new AtomicInteger();
        Observable<Observable<Integer>> o1 = createInfiniteObservable(generated1).map(new Func1<Integer, Observable<Integer>>() {

            @Override
            public Observable<Integer> call(Integer t1) {
                return Observable.just(1, 2, 3);
            }

        });

        TestSubscriber<Integer> testSubscriber = new TestSubscriber<Integer>() {
            int i = 0;

            @Override
            public void onNext(Integer t) {
                if (i++ < 400)
                    try {
                        // force a slow consumer
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                //                System.err.println("testSubscriber received => " + t + "  on thread " + Thread.currentThread());
                super.onNext(t);
            }
        };

        Observable.merge(o1).observeOn(Schedulers.computation()).take(RxRingBuffer.SIZE * 2).subscribe(testSubscriber);
        testSubscriber.awaitTerminalEvent();
        if (testSubscriber.getOnErrorEvents().size() > 0) {
            testSubscriber.getOnErrorEvents().get(0).printStackTrace();
        }
        testSubscriber.assertNoErrors();
        System.out.println("Generated 1: " + generated1.get());
        System.err.println(testSubscriber.getOnNextEvents());
        System.out.println("done1 testBackpressureBothUpstreamAndDownstreamWithRegularObservables ");
        assertEquals(RxRingBuffer.SIZE * 2, testSubscriber.getOnNextEvents().size());
        System.out.println("done2 testBackpressureBothUpstreamAndDownstreamWithRegularObservables ");
        // we can't restrict this ... see comment above
        //        assertTrue(generated1.get() >= RxRingBuffer.SIZE && generated1.get() <= RxRingBuffer.SIZE * 4);
    }

    @Test
    public void mergeWithNullValues() {
        System.out.println("mergeWithNullValues");
        TestSubscriber<String> ts = new TestSubscriber<String>();
        Observable.merge(Observable.just(null, "one"), Observable.just("two", null)).subscribe(ts);
        ts.assertTerminalEvent();
        ts.assertNoErrors();
        ts.assertReceivedOnNext(Arrays.asList(null, "one", "two", null));
    }

    @Test
    public void mergeWithTerminalEventAfterUnsubscribe() {
        System.out.println("mergeWithTerminalEventAfterUnsubscribe");
        TestSubscriber<String> ts = new TestSubscriber<String>();
        Observable<String> bad = Observable.create(new OnSubscribe<String>() {

            @Override
            public void call(Subscriber<? super String> s) {
                s.onNext("two");
                s.unsubscribe();
                s.onCompleted();
            }

        });
        Observable.merge(Observable.just(null, "one"), bad).subscribe(ts);
        ts.assertNoErrors();
        ts.assertReceivedOnNext(Arrays.asList(null, "one", "two"));
    }

    @Test
    public void mergingNullObservable() {
        TestSubscriber<String> ts = new TestSubscriber<String>();
        Observable.merge(Observable.just("one"), null).subscribe(ts);
        ts.assertNoErrors();
        ts.assertReceivedOnNext(Arrays.asList("one"));
    }

    @Test
    public void merge1AsyncStreamOf1() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        mergeNAsyncStreamsOfN(1, 1).subscribe(ts);
        ts.awaitTerminalEvent();
        ts.assertNoErrors();
        assertEquals(1, ts.getOnNextEvents().size());
    }

    @Test
    public void merge1AsyncStreamOf1000() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        mergeNAsyncStreamsOfN(1, 1000).subscribe(ts);
        ts.awaitTerminalEvent();
        ts.assertNoErrors();
        assertEquals(1000, ts.getOnNextEvents().size());
    }

    @Test
    public void merge10AsyncStreamOf1000() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        mergeNAsyncStreamsOfN(10, 1000).subscribe(ts);
        ts.awaitTerminalEvent();
        ts.assertNoErrors();
        assertEquals(10000, ts.getOnNextEvents().size());
    }

    @Test
    public void merge1000AsyncStreamOf1000() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        mergeNAsyncStreamsOfN(1000, 1000).subscribe(ts);
        ts.awaitTerminalEvent();
        ts.assertNoErrors();
        assertEquals(1000000, ts.getOnNextEvents().size());
    }

    @Test
    public void merge2000AsyncStreamOf100() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        mergeNAsyncStreamsOfN(2000, 100).subscribe(ts);
        ts.awaitTerminalEvent();
        ts.assertNoErrors();
        assertEquals(200000, ts.getOnNextEvents().size());
    }

    @Test
    public void merge100AsyncStreamOf1() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        mergeNAsyncStreamsOfN(100, 1).subscribe(ts);
        ts.awaitTerminalEvent();
        ts.assertNoErrors();
        assertEquals(100, ts.getOnNextEvents().size());
    }

    private Observable<Integer> mergeNAsyncStreamsOfN(final int outerSize, final int innerSize) {
        Observable<Observable<Integer>> os = Observable.range(1, outerSize).map(new Func1<Integer, Observable<Integer>>() {

            @Override
            public Observable<Integer> call(Integer i) {
                return Observable.range(1, innerSize).subscribeOn(Schedulers.computation());
            }

        });
        return Observable.merge(os);
    }

    @Test
    public void merge1SyncStreamOf1() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        mergeNSyncStreamsOfN(1, 1).subscribe(ts);
        ts.awaitTerminalEvent();
        ts.assertNoErrors();
        assertEquals(1, ts.getOnNextEvents().size());
    }

    @Test
    public void merge1SyncStreamOf1000000() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        mergeNSyncStreamsOfN(1, 1000000).subscribe(ts);
        ts.awaitTerminalEvent();
        ts.assertNoErrors();
        assertEquals(1000000, ts.getOnNextEvents().size());
    }

    @Test
    public void merge1000SyncStreamOf1000() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        mergeNSyncStreamsOfN(1000, 1000).subscribe(ts);
        ts.awaitTerminalEvent();
        ts.assertNoErrors();
        assertEquals(1000000, ts.getOnNextEvents().size());
    }

    @Test
    public void merge10000SyncStreamOf10() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        mergeNSyncStreamsOfN(10000, 10).subscribe(ts);
        ts.awaitTerminalEvent();
        ts.assertNoErrors();
        assertEquals(100000, ts.getOnNextEvents().size());
    }

    @Test
    public void merge1000000SyncStreamOf1() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        mergeNSyncStreamsOfN(1000000, 1).subscribe(ts);
        ts.awaitTerminalEvent();
        ts.assertNoErrors();
        assertEquals(1000000, ts.getOnNextEvents().size());
    }

    private Observable<Integer> mergeNSyncStreamsOfN(final int outerSize, final int innerSize) {
        Observable<Observable<Integer>> os = Observable.range(1, outerSize).map(new Func1<Integer, Observable<Integer>>() {

            @Override
            public Observable<Integer> call(Integer i) {
                return Observable.range(1, innerSize);
            }

        });
        return Observable.merge(os);
    }

    private Observable<Integer> createInfiniteObservable(final AtomicInteger generated) {
        return Observable.from(new Iterable<Integer>() {
            @Override
            public Iterator<Integer> iterator() {
                return new Iterator<Integer>() {

                    @Override
                    public void remove() {
                    }

                    @Override
                    public Integer next() {
                        return generated.getAndIncrement();
                    }

                    @Override
                    public boolean hasNext() {
                        return true;
                    }
                };
            }
        });
    }

    @Test
    public void mergeManyAsyncSingle() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        Observable<Observable<Integer>> os = Observable.range(1, 10000).map(new Func1<Integer, Observable<Integer>>() {

            @Override
            public Observable<Integer> call(final Integer i) {
                return Observable.create(new OnSubscribe<Integer>() {

                    @Override
                    public void call(Subscriber<? super Integer> s) {
                        if (i < 500) {
                            try {
                                Thread.sleep(1);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                        s.onNext(i);
                        s.onCompleted();
                    }

                }).subscribeOn(Schedulers.computation()).cache();
            }

        });
        Observable.merge(os).subscribe(ts);
        ts.awaitTerminalEvent();
        ts.assertNoErrors();
        assertEquals(10000, ts.getOnNextEvents().size());
    }

    @Test
    public void shouldCompleteAfterApplyingBackpressure_NormalPath() {
        Observable<Integer> source = Observable.mergeDelayError(Observable.just(Observable.range(1, 2)));
        TestSubscriber<Integer> subscriber = new TestSubscriber<Integer>();
        subscriber.requestMore(0);
        source.subscribe(subscriber);
        subscriber.requestMore(3); // 1, 2, <complete> - with requestMore(2) we get the 1 and 2 but not the <complete>
        subscriber.assertReceivedOnNext(asList(1, 2));
        subscriber.assertTerminalEvent();
    }

    @Test
    public void shouldCompleteAfterApplyingBackpressure_FastPath() {
        Observable<Integer> source = Observable.mergeDelayError(Observable.just(Observable.just(1)));
        TestSubscriber<Integer> subscriber = new TestSubscriber<Integer>();
        subscriber.requestMore(0);
        source.subscribe(subscriber);
        subscriber.requestMore(2); // 1, <complete> - should work as per .._NormalPath above
        subscriber.assertReceivedOnNext(asList(1));
        subscriber.assertTerminalEvent();
    }

    @Test
    public void shouldNotCompleteIfThereArePendingScalarSynchronousEmissionsWhenTheLastInnerSubscriberCompletes() {
        TestScheduler scheduler = Schedulers.test();
        Observable<Long> source = Observable.mergeDelayError(Observable.just(1L), Observable.timer(1, TimeUnit.SECONDS, scheduler).skip(1));
        TestSubscriber<Long> subscriber = new TestSubscriber<Long>();
        subscriber.requestMore(0);
        source.subscribe(subscriber);
        scheduler.advanceTimeBy(1, TimeUnit.SECONDS);
        subscriber.assertReceivedOnNext(Collections.<Long>emptyList());
        assertEquals(Collections.<Notification<Long>>emptyList(), subscriber.getOnCompletedEvents());
        subscriber.requestMore(1);
        subscriber.assertReceivedOnNext(asList(1L));
// TODO: it should be acceptable to get a completion event without requests
//        assertEquals(Collections.<Notification<Long>>emptyList(), subscriber.getOnCompletedEvents());
//        subscriber.requestMore(1);
        subscriber.assertTerminalEvent();
    }

    @Test
    public void delayedErrorsShouldBeEmittedWhenCompleteAfterApplyingBackpressure_NormalPath() {
        Throwable exception = new Throwable();
        Observable<Integer> source = Observable.mergeDelayError(Observable.range(1, 2), Observable.<Integer>error(exception));
        TestSubscriber<Integer> subscriber = new TestSubscriber<Integer>();
        subscriber.requestMore(0);
        source.subscribe(subscriber);
        subscriber.requestMore(3); // 1, 2, <error>
        subscriber.assertReceivedOnNext(asList(1, 2));
        subscriber.assertTerminalEvent();
        assertEquals(asList(exception), subscriber.getOnErrorEvents());
    }

    @Test
    public void delayedErrorsShouldBeEmittedWhenCompleteAfterApplyingBackpressure_FastPath() {
        Throwable exception = new Throwable();
        Observable<Integer> source = Observable.mergeDelayError(Observable.just(1), Observable.<Integer>error(exception));
        TestSubscriber<Integer> subscriber = new TestSubscriber<Integer>();
        subscriber.requestMore(0);
        source.subscribe(subscriber);
        subscriber.requestMore(2); // 1, <error>
        subscriber.assertReceivedOnNext(asList(1));
        subscriber.assertTerminalEvent();
        assertEquals(asList(exception), subscriber.getOnErrorEvents());
    }

    @Test
    public void shouldNotCompleteWhileThereAreStillScalarSynchronousEmissionsInTheQueue() {
        Observable<Integer> source = Observable.merge(Observable.just(1), Observable.just(2));
        TestSubscriber<Integer> subscriber = new TestSubscriber<Integer>();
        subscriber.requestMore(1);
        source.subscribe(subscriber);
        subscriber.assertReceivedOnNext(asList(1));
        subscriber.requestMore(1);
        subscriber.assertReceivedOnNext(asList(1, 2));
    }

    @Test
    public void shouldNotReceivedDelayedErrorWhileThereAreStillScalarSynchronousEmissionsInTheQueue() {
        Throwable exception = new Throwable();
        Observable<Integer> source = Observable.mergeDelayError(Observable.just(1), Observable.just(2), Observable.<Integer>error(exception));
        TestSubscriber<Integer> subscriber = new TestSubscriber<Integer>();
        subscriber.requestMore(1);
        source.subscribe(subscriber);
        subscriber.assertReceivedOnNext(asList(1));
        assertEquals(Collections.<Throwable>emptyList(), subscriber.getOnErrorEvents());
        subscriber.requestMore(1);
        subscriber.assertReceivedOnNext(asList(1, 2));
        assertEquals(asList(exception), subscriber.getOnErrorEvents());
    }

    @Test
    public void shouldNotReceivedDelayedErrorWhileThereAreStillNormalEmissionsInTheQueue() {
        Throwable exception = new Throwable();
        Observable<Integer> source = Observable.mergeDelayError(Observable.range(1, 2), Observable.range(3, 2), Observable.<Integer>error(exception));
        TestSubscriber<Integer> subscriber = new TestSubscriber<Integer>();
        subscriber.requestMore(3);
        source.subscribe(subscriber);
        subscriber.assertReceivedOnNext(asList(1, 2, 3));
        assertEquals(Collections.<Throwable>emptyList(), subscriber.getOnErrorEvents());
        subscriber.requestMore(2);
        subscriber.assertReceivedOnNext(asList(1, 2, 3, 4));
        assertEquals(asList(exception), subscriber.getOnErrorEvents());
    }
    
    @Test
    public void testMergeKeepsRequesting() throws InterruptedException {
        //for (int i = 0; i < 5000; i++) {
            //System.out.println(i + ".......................................................................");
            final CountDownLatch latch = new CountDownLatch(1);
            final ConcurrentLinkedQueue<String> messages = new ConcurrentLinkedQueue<String>();

            Observable.range(1, 2)
                    // produce many integers per second
                    .flatMap(new Func1<Integer, Observable<Integer>>() {
                        @Override
                        public Observable<Integer> call(final Integer number) {
                            return Observable.range(1, Integer.MAX_VALUE)
                                    .doOnRequest(new Action1<Long>() {

                                        @Override
                                        public void call(Long n) {
                                            messages.add(">>>>>>>> A requested[" + number + "]: " + n);
                                        }

                                    })
                                    // pause a bit
                                    .doOnNext(pauseForMs(3))
                                    // buffer on backpressure
                                    .onBackpressureBuffer()
                                    // do in parallel
                                    .subscribeOn(Schedulers.computation())
                                    .doOnRequest(new Action1<Long>() {

                                        @Override
                                        public void call(Long n) {
                                            messages.add(">>>>>>>> B requested[" + number + "]: " + n);
                                        }

                                    });
                        }

                    })
                    // take a number bigger than 2* RxRingBuffer.SIZE (used by OperatorMerge)
                    .take(RxRingBuffer.SIZE * 2 + 1)
                    // log count
                    .doOnNext(printCount())
                    // release latch
                    .doOnCompleted(new Action0() {
                        @Override
                        public void call() {
                            latch.countDown();
                        }
                    }).subscribe();
            boolean a = latch.await(2, TimeUnit.SECONDS);
            if (!a) {
                for (String s : messages) {
                    System.out.println("DEBUG => " + s);
                }
            }
            assertTrue(a);
        //}
    }
    
    @Test
    public void testMergeRequestOverflow() throws InterruptedException {
        //do a non-trivial merge so that future optimisations with EMPTY don't invalidate this test
        Observable<Integer> o = Observable.from(Arrays.asList(1,2)).mergeWith(Observable.from(Arrays.asList(3,4)));
        final int expectedCount = 4;
        final CountDownLatch latch = new CountDownLatch(expectedCount);
        o.subscribeOn(Schedulers.computation()).subscribe(new Subscriber<Integer>() {
            
            @Override
            public void onStart() {
                request(1);
            }

            @Override
            public void onCompleted() {
                //ignore
            }

            @Override
            public void onError(Throwable e) {
                throw new RuntimeException(e);
            }

            @Override
            public void onNext(Integer t) {
                latch.countDown();
                request(2);
                request(Long.MAX_VALUE-1);
            }});
        assertTrue(latch.await(10, TimeUnit.SECONDS));
    }

    private static Action1<Integer> printCount() {
        return new Action1<Integer>() {
            long count;

            @Override
            public void call(Integer t1) {
                count++;
                System.out.println("count=" + count);
            }
        };
    }

    private static Action1<Integer> pauseForMs(final long time) {
        return new Action1<Integer>() {
            @Override
            public void call(Integer s) {
                try {
                    Thread.sleep(time);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }
    
    Func1<Integer, Observable<Integer>> toScalar = new Func1<Integer, Observable<Integer>>() {
        @Override
        public Observable<Integer> call(Integer t) {
            return Observable.just(t);
        }
    };
    Func1<Integer, Observable<Integer>> toHiddenScalar = new Func1<Integer, Observable<Integer>>() {
        @Override
        public Observable<Integer> call(Integer t) {
            return Observable.just(t).asObservable();
        }
    };
    
    void runMerge(Func1<Integer, Observable<Integer>> func, TestSubscriber<Integer> ts) {
        List<Integer> list = new ArrayList<Integer>();
        for (int i = 0; i < 1000; i++) {
            list.add(i);
        }
        Observable<Integer> source = Observable.from(list);
        source.flatMap(func).subscribe(ts);
        
        if (ts.getOnNextEvents().size() != 1000) {
            System.out.println(ts.getOnNextEvents());
        }
        
        ts.assertTerminalEvent();
        ts.assertNoErrors();
        ts.assertReceivedOnNext(list);
    }
    
    @Test
    public void testFastMergeFullScalar() {
        runMerge(toScalar, new TestSubscriber<Integer>());
    }
    @Test
    public void testFastMergeHiddenScalar() {
        runMerge(toHiddenScalar, new TestSubscriber<Integer>());
    }
    @Test
    public void testSlowMergeFullScalar() {
        for (final int req : new int[] { 16, 32, 64, 128, 256 }) {
            TestSubscriber<Integer> ts = new TestSubscriber<Integer>() {
                int remaining = req;
                @Override
                public void onStart() {
                    request(req);
                }
                @Override
                public void onNext(Integer t) {
                    super.onNext(t);
                    if (--remaining == 0) {
                        remaining = req;
                        request(req);
                    }
                }
            };
            runMerge(toScalar, ts);
        }
    }
    @Test
    public void testSlowMergeHiddenScalar() {
        for (final int req : new int[] { 16, 32, 64, 128, 256 }) {
            TestSubscriber<Integer> ts = new TestSubscriber<Integer>() {
                int remaining = req;
                @Override
                public void onStart() {
                    request(req);
                }
                @Override
                public void onNext(Integer t) {
                    super.onNext(t);
                    if (--remaining == 0) {
                        remaining = req;
                        request(req);
                    }
                }
            };
            runMerge(toHiddenScalar, ts);
        }
    }
    
    @Test
    public void testUnboundedDefaultConcurrency() {
        List<Observable<Integer>> os = new ArrayList<Observable<Integer>>();
        for(int i=0; i < 2000; i++) {
            os.add(Observable.<Integer>never());
        }
        os.add(Observable.range(0, 100));       

        TestSubscriber<Integer> ts = TestSubscriber.create();
        Observable.merge(os).take(1).subscribe(ts);
        ts.awaitTerminalEvent(5000, TimeUnit.MILLISECONDS);
        ts.assertValue(0);
        ts.assertCompleted();
    }

    @Test
    public void testConcurrencyLimit() {
        List<Observable<Integer>> os = new ArrayList<Observable<Integer>>();
        for(int i=0; i < 2000; i++) {
            os.add(Observable.<Integer>never());
        }
        os.add(Observable.range(0, 100));       

        TestSubscriber<Integer> ts = TestSubscriber.create();
        Observable.merge(os, Integer.MAX_VALUE).take(1).subscribe(ts);
        ts.awaitTerminalEvent(5000, TimeUnit.MILLISECONDS);
        ts.assertValue(0);
        ts.assertCompleted();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void negativeMaxConcurrent() {
        try {
            Observable.merge(Arrays.asList(Observable.just(1), Observable.just(2)), -1);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertEquals("maxConcurrent > 0 required but it was -1", e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void zeroMaxConcurrent() {
        try {
            Observable.merge(Arrays.asList(Observable.just(1), Observable.just(2)), 0);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertEquals("maxConcurrent > 0 required but it was 0", e.getMessage());
        }
    }
    
    @Test
    public void mergeJustNull() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>(0);
        
        Observable.range(1, 2).flatMap(new Func1<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> call(Integer t) {
                return Observable.just(null);
            }
        }).subscribe(ts);
        
        ts.requestMore(2);
        ts.assertValues(null, null);
        ts.assertNoErrors();
        ts.assertCompleted();
    }
}
