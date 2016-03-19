/**
 * Copyright 2016 Netflix, Inc.
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

package io.reactivex.internal.operators.flowable;

import static java.util.Arrays.asList;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.junit.*;
import org.reactivestreams.*;

import io.reactivex.*;
import io.reactivex.Scheduler.Worker;
import io.reactivex.flowable.TestHelper;
import io.reactivex.functions.*;
import io.reactivex.internal.subscriptions.*;
import io.reactivex.schedulers.*;
import io.reactivex.subscribers.DefaultObserver;
import io.reactivex.subscribers.TestSubscriber;

public class OperatorMergeTest {

    Subscriber<String> stringObserver;

    int count;
    
    @Before
    public void before() {
        stringObserver = TestHelper.mockSubscriber();
        
        for (Thread t : Thread.getAllStackTraces().keySet()) {
            if (t.getName().startsWith("RxNewThread")) {
                count++;
            }
        }
    }
    
    @After
    public void after() {
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        for (Thread t : Thread.getAllStackTraces().keySet()) {
            if (t.getName().startsWith("RxNewThread")) {
                --count;
            }
        }
        if (count != 0) {
            throw new IllegalStateException("NewThread leak!");
        }
    }

    @Test
    public void testMergeObservableOfObservables() {
        final Flowable<String> o1 = Flowable.create(new TestSynchronousObservable());
        final Flowable<String> o2 = Flowable.create(new TestSynchronousObservable());

        Flowable<Flowable<String>> observableOfObservables = Flowable.create(new Publisher<Flowable<String>>() {

            @Override
            public void subscribe(Subscriber<? super Flowable<String>> observer) {
                observer.onSubscribe(EmptySubscription.INSTANCE);
                // simulate what would happen in an observable
                observer.onNext(o1);
                observer.onNext(o2);
                observer.onComplete();
            }

        });
        Flowable<String> m = Flowable.merge(observableOfObservables);
        m.subscribe(stringObserver);

        verify(stringObserver, never()).onError(any(Throwable.class));
        verify(stringObserver, times(1)).onComplete();
        verify(stringObserver, times(2)).onNext("hello");
    }

    @Test
    public void testMergeArray() {
        final Flowable<String> o1 = Flowable.create(new TestSynchronousObservable());
        final Flowable<String> o2 = Flowable.create(new TestSynchronousObservable());

        Flowable<String> m = Flowable.merge(o1, o2);
        m.subscribe(stringObserver);

        verify(stringObserver, never()).onError(any(Throwable.class));
        verify(stringObserver, times(2)).onNext("hello");
        verify(stringObserver, times(1)).onComplete();
    }

    @Test
    public void testMergeList() {
        final Flowable<String> o1 = Flowable.create(new TestSynchronousObservable());
        final Flowable<String> o2 = Flowable.create(new TestSynchronousObservable());
        List<Flowable<String>> listOfObservables = new ArrayList<Flowable<String>>();
        listOfObservables.add(o1);
        listOfObservables.add(o2);

        Flowable<String> m = Flowable.merge(listOfObservables);
        m.subscribe(stringObserver);

        verify(stringObserver, never()).onError(any(Throwable.class));
        verify(stringObserver, times(1)).onComplete();
        verify(stringObserver, times(2)).onNext("hello");
    }

    @Test(timeout = 1000)
    public void testUnSubscribeObservableOfObservables() throws InterruptedException {

        final AtomicBoolean unsubscribed = new AtomicBoolean();
        final CountDownLatch latch = new CountDownLatch(1);

        Flowable<Flowable<Long>> source = Flowable.create(new Publisher<Flowable<Long>>() {

            @Override
            public void subscribe(final Subscriber<? super Flowable<Long>> observer) {
                // verbose on purpose so I can track the inside of it
                final Subscription s = new Subscription() {

                    @Override
                    public void request(long n) {
                        
                    }
                    
                    @Override
                    public void cancel() {
                        System.out.println("*** unsubscribed");
                        unsubscribed.set(true);
                    }

                };
                observer.onSubscribe(s);

                new Thread(new Runnable() {

                    @Override
                    public void run() {

                        while (!unsubscribed.get()) {
                            observer.onNext(Flowable.just(1L, 2L));
                        }
                        System.out.println("Done looping after unsubscribe: " + unsubscribed.get());
                        observer.onComplete();

                        // mark that the thread is finished
                        latch.countDown();
                    }
                }).start();
            }

        });

        final AtomicInteger count = new AtomicInteger();
        Flowable.merge(source).take(6).toBlocking().forEach(new Consumer<Long>() {

            @Override
            public void accept(Long v) {
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

        Flowable<String> m = Flowable.merge(Flowable.create(o1), Flowable.create(o2));
        TestSubscriber<String> ts = new TestSubscriber<String>(stringObserver);
        m.subscribe(ts);

        ts.awaitTerminalEvent();
        ts.assertNoErrors();

        verify(stringObserver, never()).onError(any(Throwable.class));
        verify(stringObserver, times(2)).onNext("hello");
        verify(stringObserver, times(1)).onComplete();
    }

    @Test
    public void testSynchronizationOfMultipleSequencesLoop() throws Throwable {
        for (int i = 0; i < 100; i++) {
            System.out.println("testSynchronizationOfMultipleSequencesLoop > " + i);
            testSynchronizationOfMultipleSequences();
        }
    }
    
    @Test
    public void testSynchronizationOfMultipleSequences() throws Throwable {
        final TestASynchronousObservable o1 = new TestASynchronousObservable();
        final TestASynchronousObservable o2 = new TestASynchronousObservable();

        // use this latch to cause onNext to wait until we're ready to let it go
        final CountDownLatch endLatch = new CountDownLatch(1);

        final AtomicInteger concurrentCounter = new AtomicInteger();
        final AtomicInteger totalCounter = new AtomicInteger();

        Flowable<String> m = Flowable.merge(Flowable.create(o1), Flowable.create(o2));
        m.subscribe(new DefaultObserver<String>() {

            @Override
            public void onComplete() {

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
                    // avoid deadlocking the main thread
                    if (Thread.currentThread().getName().equals("TestASynchronousObservable")) {
                        // wait here until we're done asserting
                        endLatch.await();
                    }
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
        final Flowable<String> o1 = Flowable.create(new TestErrorObservable("four", null, "six")); // we expect to lose "six"
        final Flowable<String> o2 = Flowable.create(new TestErrorObservable("one", "two", "three")); // we expect to lose all of these since o1 is done first and fails

        Flowable<String> m = Flowable.merge(o1, o2);
        m.subscribe(stringObserver);

        verify(stringObserver, times(1)).onError(any(NullPointerException.class));
        verify(stringObserver, never()).onComplete();
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
        final Flowable<String> o1 = Flowable.create(new TestErrorObservable("one", "two", "three"));
        final Flowable<String> o2 = Flowable.create(new TestErrorObservable("four", null, "six")); // we expect to lose "six"
        final Flowable<String> o3 = Flowable.create(new TestErrorObservable("seven", "eight", null));// we expect to lose all of these since o2 is done first and fails
        final Flowable<String> o4 = Flowable.create(new TestErrorObservable("nine"));// we expect to lose all of these since o2 is done first and fails

        Flowable<String> m = Flowable.merge(o1, o2, o3, o4);
        m.subscribe(stringObserver);

        verify(stringObserver, times(1)).onError(any(NullPointerException.class));
        verify(stringObserver, never()).onComplete();
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
    @Ignore("Subscribe should not throw")
    public void testThrownErrorHandling() {
        TestSubscriber<String> ts = new TestSubscriber<String>();
        Flowable<String> o1 = Flowable.create(new Publisher<String>() {

            @Override
            public void subscribe(Subscriber<? super String> s) {
                throw new RuntimeException("fail");
            }

        });

        Flowable.merge(o1, o1).subscribe(ts);
        ts.awaitTerminalEvent(1000, TimeUnit.MILLISECONDS);
        ts.assertTerminated();
        System.out.println("Error: " + ts.errors());
    }

    private static class TestSynchronousObservable implements Publisher<String> {

        @Override
        public void subscribe(Subscriber<? super String> observer) {
            observer.onSubscribe(EmptySubscription.INSTANCE);
            observer.onNext("hello");
            observer.onComplete();
        }
    }

    private static class TestASynchronousObservable implements Publisher<String> {
        Thread t;
        final CountDownLatch onNextBeingSent = new CountDownLatch(1);

        @Override
        public void subscribe(final Subscriber<? super String> observer) {
            observer.onSubscribe(EmptySubscription.INSTANCE);
            t = new Thread(new Runnable() {

                @Override
                public void run() {
                    onNextBeingSent.countDown();
                    try {
                        observer.onNext("hello");
                        // I can't use a countDownLatch to prove we are actually sending 'onNext'
                        // since it will block if synchronized and I'll deadlock
                        observer.onComplete();
                    } catch (Exception e) {
                        observer.onError(e);
                    }
                }

            }, "TestASynchronousObservable");
            t.start();
        }
    }

    private static class TestErrorObservable implements Publisher<String> {

        String[] valuesToReturn;

        TestErrorObservable(String... values) {
            valuesToReturn = values;
        }

        @Override
        public void subscribe(Subscriber<? super String> observer) {
            observer.onSubscribe(EmptySubscription.INSTANCE);
            for (String s : valuesToReturn) {
                if (s == null) {
                    System.out.println("throwing exception");
                    observer.onError(new NullPointerException());
                } else {
                    observer.onNext(s);
                }
            }
            observer.onComplete();
        }
    }

    @Test
    public void testUnsubscribeAsObservablesComplete() {
        TestScheduler scheduler1 = Schedulers.test();
        AtomicBoolean os1 = new AtomicBoolean(false);
        Flowable<Long> o1 = createObservableOf5IntervalsOf1SecondIncrementsWithSubscriptionHook(scheduler1, os1);

        TestScheduler scheduler2 = Schedulers.test();
        AtomicBoolean os2 = new AtomicBoolean(false);
        Flowable<Long> o2 = createObservableOf5IntervalsOf1SecondIncrementsWithSubscriptionHook(scheduler2, os2);

        TestSubscriber<Long> ts = new TestSubscriber<Long>();
        Flowable.merge(o1, o2).subscribe(ts);

        // we haven't incremented time so nothing should be received yet
        ts.assertNoValues();

        scheduler1.advanceTimeBy(3, TimeUnit.SECONDS);
        scheduler2.advanceTimeBy(2, TimeUnit.SECONDS);

        ts.assertValues(0L, 1L, 2L, 0L, 1L);
        // not unsubscribed yet
        assertFalse(os1.get());
        assertFalse(os2.get());

        // advance to the end at which point it should complete
        scheduler1.advanceTimeBy(3, TimeUnit.SECONDS);

        ts.assertValues(0L, 1L, 2L, 0L, 1L, 3L, 4L);
        assertTrue(os1.get());
        assertFalse(os2.get());

        // both should be completed now
        scheduler2.advanceTimeBy(3, TimeUnit.SECONDS);

        ts.assertValues(0L, 1L, 2L, 0L, 1L, 3L, 4L, 2L, 3L, 4L);
        assertTrue(os1.get());
        assertTrue(os2.get());

        ts.assertTerminated();
    }

    @Test
    public void testEarlyUnsubscribe() {
        for (int i = 0; i < 10; i++) {
            TestScheduler scheduler1 = Schedulers.test();
            AtomicBoolean os1 = new AtomicBoolean(false);
            Flowable<Long> o1 = createObservableOf5IntervalsOf1SecondIncrementsWithSubscriptionHook(scheduler1, os1);

            TestScheduler scheduler2 = Schedulers.test();
            AtomicBoolean os2 = new AtomicBoolean(false);
            Flowable<Long> o2 = createObservableOf5IntervalsOf1SecondIncrementsWithSubscriptionHook(scheduler2, os2);

            TestSubscriber<Long> ts = new TestSubscriber<Long>();
            Flowable.merge(o1, o2).subscribe(ts);

            // we haven't incremented time so nothing should be received yet
            ts.assertNoValues();

            scheduler1.advanceTimeBy(3, TimeUnit.SECONDS);
            scheduler2.advanceTimeBy(2, TimeUnit.SECONDS);

            ts.assertValues(0L, 1L, 2L, 0L, 1L);
            // not unsubscribed yet
            assertFalse(os1.get());
            assertFalse(os2.get());

            // early unsubscribe
            ts.dispose();

            assertTrue(os1.get());
            assertTrue(os2.get());

            ts.assertValues(0L, 1L, 2L, 0L, 1L);
            // FIXME not happening anymore
//            ts.assertUnsubscribed();
        }
    }

    private Flowable<Long> createObservableOf5IntervalsOf1SecondIncrementsWithSubscriptionHook(final Scheduler scheduler, final AtomicBoolean unsubscribed) {
        return Flowable.create(new Publisher<Long>() {

            @Override
            public void subscribe(final Subscriber<? super Long> child) {
                Flowable.interval(1, TimeUnit.SECONDS, scheduler)
                .take(5)
                .subscribe(new Subscriber<Long>() {
                    @Override
                    public void onSubscribe(final Subscription s) {
                        child.onSubscribe(new Subscription() {
                            @Override
                            public void request(long n) {
                                s.request(n);
                            }
                            
                            @Override
                            public void cancel() {
                                unsubscribed.set(true);
                                s.cancel();
                            }
                        });
                    }
                    
                    @Override
                    public void onNext(Long t) {
                        child.onNext(t);
                    }
                    
                    @Override
                    public void onError(Throwable t) {
                        unsubscribed.set(true);
                        child.onError(t);
                    }
                    
                    @Override
                    public void onComplete() {
                        unsubscribed.set(true);
                        child.onComplete();
                    }
                    
                });
            }
        });
    }

    @Test//(timeout = 10000)
    public void testConcurrency() {
        Flowable<Integer> o = Flowable.range(1, 10000).subscribeOn(Schedulers.newThread());

        for (int i = 0; i < 10; i++) {
            Flowable<Integer> merge = Flowable.merge(o.onBackpressureBuffer(), o.onBackpressureBuffer(), o.onBackpressureBuffer());
            TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
            merge.subscribe(ts);

            ts.awaitTerminalEvent(3, TimeUnit.SECONDS);
            ts.assertTerminated();
            ts.assertNoErrors();
            ts.assertComplete();
            List<Integer> onNextEvents = ts.values();
            assertEquals(30000, onNextEvents.size());
            //            System.out.println("onNext: " + onNextEvents.size() + " onCompleted: " + ts.getOnCompletedEvents().size());
        }
    }

    @Test
    public void testConcurrencyWithSleeping() {

        Flowable<Integer> o = Flowable.create(new Publisher<Integer>() {

            @Override
            public void subscribe(final Subscriber<? super Integer> s) {
                Worker inner = Schedulers.newThread().createWorker();
                final AsyncSubscription as = new AsyncSubscription();
                as.setSubscription(EmptySubscription.INSTANCE);
                as.setResource(inner);
                
                s.onSubscribe(as);
                
                inner.schedule(new Runnable() {

                    @Override
                    public void run() {
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
                        as.dispose();
                        s.onComplete();
                    }

                });
            }
        });

        for (int i = 0; i < 10; i++) {
            Flowable<Integer> merge = Flowable.merge(o, o, o);
            TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
            merge.subscribe(ts);

            ts.awaitTerminalEvent();
            ts.assertComplete();
            List<Integer> onNextEvents = ts.values();
            assertEquals(300, onNextEvents.size());
            //            System.out.println("onNext: " + onNextEvents.size() + " onCompleted: " + ts.getOnCompletedEvents().size());
        }
    }

    @Test
    public void testConcurrencyWithBrokenOnCompleteContract() {
        Flowable<Integer> o = Flowable.create(new Publisher<Integer>() {

            @Override
            public void subscribe(final Subscriber<? super Integer> s) {
                Worker inner = Schedulers.newThread().createWorker();
                final AsyncSubscription as = new AsyncSubscription();
                as.setSubscription(EmptySubscription.INSTANCE);
                as.setResource(inner);
                
                s.onSubscribe(as);
                
                inner.schedule(new Runnable() {

                    @Override
                    public void run() {
                        try {
                            for (int i = 0; i < 10000; i++) {
                                s.onNext(i);
                            }
                        } catch (Exception e) {
                            s.onError(e);
                        }
                        as.dispose();
                        s.onComplete();
                        s.onComplete();
                        s.onComplete();
                    }

                });
            }
        });

        for (int i = 0; i < 10; i++) {
            Flowable<Integer> merge = Flowable.merge(o.onBackpressureBuffer(), o.onBackpressureBuffer(), o.onBackpressureBuffer());
            TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
            merge.subscribe(ts);

            ts.awaitTerminalEvent();
            ts.assertNoErrors();
            ts.assertComplete();
            List<Integer> onNextEvents = ts.values();
            assertEquals(30000, onNextEvents.size());
            //                System.out.println("onNext: " + onNextEvents.size() + " onCompleted: " + ts.getOnCompletedEvents().size());
        }
    }

    @Test
    public void testBackpressureUpstream() throws InterruptedException {
        final AtomicInteger generated1 = new AtomicInteger();
        Flowable<Integer> o1 = createInfiniteObservable(generated1).subscribeOn(Schedulers.computation());
        final AtomicInteger generated2 = new AtomicInteger();
        Flowable<Integer> o2 = createInfiniteObservable(generated2).subscribeOn(Schedulers.computation());

        TestSubscriber<Integer> testSubscriber = new TestSubscriber<Integer>() {
            @Override
            public void onNext(Integer t) {
                System.err.println("testSubscriber received => " + t + "  on thread " + Thread.currentThread());
                super.onNext(t);
            }
        };

        Flowable.merge(o1.take(Flowable.bufferSize() * 2), o2.take(Flowable.bufferSize() * 2)).subscribe(testSubscriber);
        testSubscriber.awaitTerminalEvent();
        if (testSubscriber.errors().size() > 0) {
            testSubscriber.errors().get(0).printStackTrace();
        }
        testSubscriber.assertNoErrors();
        System.err.println(testSubscriber.values());
        assertEquals(Flowable.bufferSize() * 4, testSubscriber.values().size());
        // it should be between the take num and requested batch size across the async boundary
        System.out.println("Generated 1: " + generated1.get());
        System.out.println("Generated 2: " + generated2.get());
        assertTrue(generated1.get() >= Flowable.bufferSize() * 2 && generated1.get() <= Flowable.bufferSize() * 4);
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
        Flowable<Integer> o1 = createInfiniteObservable(generated1).subscribeOn(Schedulers.computation());

        TestSubscriber<Integer> testSubscriber = new TestSubscriber<Integer>() {
            @Override
            public void onNext(Integer t) {
                super.onNext(t);
            }
        };

        Flowable.merge(o1.take(Flowable.bufferSize() * 2), Flowable.just(-99)).subscribe(testSubscriber);
        testSubscriber.awaitTerminalEvent();
        
        List<Integer> onNextEvents = testSubscriber.values();
        
        System.out.println("Generated 1: " + generated1.get() + " / received: " + onNextEvents.size());
        System.out.println(onNextEvents);

        if (testSubscriber.errors().size() > 0) {
            testSubscriber.errors().get(0).printStackTrace();
        }
        testSubscriber.assertNoErrors();
        assertEquals(Flowable.bufferSize() * 2 + 1, onNextEvents.size());
        // it should be between the take num and requested batch size across the async boundary
        assertTrue(generated1.get() >= Flowable.bufferSize() * 2 && generated1.get() <= Flowable.bufferSize() * 3);
    }

    /**
     * This is the same as the upstreams ones, but now adds the downstream as well by using observeOn.
     * 
     * This requires merge to also obey the Product.request values coming from it's child subscriber.
     * @throws InterruptedException if the test is interrupted
     */
    @Test(timeout = 10000)
    public void testBackpressureDownstreamWithConcurrentStreams() throws InterruptedException {
        final AtomicInteger generated1 = new AtomicInteger();
        Flowable<Integer> o1 = createInfiniteObservable(generated1).subscribeOn(Schedulers.computation());
        final AtomicInteger generated2 = new AtomicInteger();
        Flowable<Integer> o2 = createInfiniteObservable(generated2).subscribeOn(Schedulers.computation());

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

        Flowable.merge(o1.take(Flowable.bufferSize() * 2), o2.take(Flowable.bufferSize() * 2)).observeOn(Schedulers.computation()).subscribe(testSubscriber);
        testSubscriber.awaitTerminalEvent();
        if (testSubscriber.errors().size() > 0) {
            testSubscriber.errors().get(0).printStackTrace();
        }
        testSubscriber.assertNoErrors();
        System.err.println(testSubscriber.values());
        assertEquals(Flowable.bufferSize() * 4, testSubscriber.values().size());
        // it should be between the take num and requested batch size across the async boundary
        System.out.println("Generated 1: " + generated1.get());
        System.out.println("Generated 2: " + generated2.get());
        assertTrue(generated1.get() >= Flowable.bufferSize() * 2 && generated1.get() <= Flowable.bufferSize() * 4);
    }

    @Test
    public void testBackpressureBothUpstreamAndDownstreamWithSynchronousScalarObservables() throws InterruptedException {
        final AtomicInteger generated1 = new AtomicInteger();
        Flowable<Flowable<Integer>> o1 = createInfiniteObservable(generated1)
        .map(new Function<Integer, Flowable<Integer>>() {

            @Override
            public Flowable<Integer> apply(Integer t1) {
                return Flowable.just(t1);
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

        Flowable.merge(o1).observeOn(Schedulers.computation()).take(Flowable.bufferSize() * 2).subscribe(testSubscriber);
        testSubscriber.awaitTerminalEvent();
        if (testSubscriber.errors().size() > 0) {
            testSubscriber.errors().get(0).printStackTrace();
        }
        testSubscriber.assertNoErrors();
        System.out.println("Generated 1: " + generated1.get());
        System.err.println(testSubscriber.values());
        assertEquals(Flowable.bufferSize() * 2, testSubscriber.values().size());
        // it should be between the take num and requested batch size across the async boundary
        assertTrue(generated1.get() >= Flowable.bufferSize() * 2 && generated1.get() <= Flowable.bufferSize() * 4);
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
        Flowable<Flowable<Integer>> o1 = createInfiniteObservable(generated1).map(new Function<Integer, Flowable<Integer>>() {

            @Override
            public Flowable<Integer> apply(Integer t1) {
                return Flowable.just(1, 2, 3);
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

        Flowable.merge(o1).observeOn(Schedulers.computation()).take(Flowable.bufferSize() * 2).subscribe(testSubscriber);
        testSubscriber.awaitTerminalEvent();
        if (testSubscriber.errors().size() > 0) {
            testSubscriber.errors().get(0).printStackTrace();
        }
        testSubscriber.assertNoErrors();
        System.out.println("Generated 1: " + generated1.get());
        System.err.println(testSubscriber.values());
        System.out.println("done1 testBackpressureBothUpstreamAndDownstreamWithRegularObservables ");
        assertEquals(Flowable.bufferSize() * 2, testSubscriber.values().size());
        System.out.println("done2 testBackpressureBothUpstreamAndDownstreamWithRegularObservables ");
        // we can't restrict this ... see comment above
        //        assertTrue(generated1.get() >= Observable.bufferSize() && generated1.get() <= Observable.bufferSize() * 4);
    }

    @Test
    @Ignore("Null values not permitted")
    public void mergeWithNullValues() {
        System.out.println("mergeWithNullValues");
        TestSubscriber<String> ts = new TestSubscriber<String>();
        Flowable.merge(Flowable.just(null, "one"), Flowable.just("two", null)).subscribe(ts);
        ts.assertTerminated();
        ts.assertNoErrors();
        ts.assertValues(null, "one", "two", null);
    }

    @Test
    @Ignore("Null values are no longer permitted")
    public void mergeWithTerminalEventAfterUnsubscribe() {
        System.out.println("mergeWithTerminalEventAfterUnsubscribe");
        TestSubscriber<String> ts = new TestSubscriber<String>();
        Flowable<String> bad = Flowable.create(new Publisher<String>() {

            @Override
            public void subscribe(Subscriber<? super String> s) {
                s.onNext("two");
                // FIXME can't cancel downstream
//                s.unsubscribe();
//                s.onComplete();
            }

        });
        Flowable.merge(Flowable.just(null, "one"), bad).subscribe(ts);
        ts.assertNoErrors();
        ts.assertValues(null, "one", "two");
    }

    @Test
    @Ignore("Null values are not permitted")
    public void mergingNullObservable() {
        TestSubscriber<String> ts = new TestSubscriber<String>();
        Flowable.merge(Flowable.just("one"), null).subscribe(ts);
        ts.assertNoErrors();
        ts.assertValue("one");
    }

    @Test
    public void merge1AsyncStreamOf1() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        mergeNAsyncStreamsOfN(1, 1).subscribe(ts);
        ts.awaitTerminalEvent();
        ts.assertNoErrors();
        assertEquals(1, ts.values().size());
    }

    @Test
    public void merge1AsyncStreamOf1000() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        mergeNAsyncStreamsOfN(1, 1000).subscribe(ts);
        ts.awaitTerminalEvent();
        ts.assertNoErrors();
        assertEquals(1000, ts.values().size());
    }

    @Test
    public void merge10AsyncStreamOf1000() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        mergeNAsyncStreamsOfN(10, 1000).subscribe(ts);
        ts.awaitTerminalEvent();
        ts.assertNoErrors();
        assertEquals(10000, ts.values().size());
    }

    @Test
    public void merge1000AsyncStreamOf1000() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        mergeNAsyncStreamsOfN(1000, 1000).subscribe(ts);
        ts.awaitTerminalEvent();
        ts.assertNoErrors();
        assertEquals(1000000, ts.values().size());
    }

    @Test
    public void merge2000AsyncStreamOf100() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        mergeNAsyncStreamsOfN(2000, 100).subscribe(ts);
        ts.awaitTerminalEvent();
        ts.assertNoErrors();
        assertEquals(200000, ts.values().size());
    }

    @Test
    public void merge100AsyncStreamOf1() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        mergeNAsyncStreamsOfN(100, 1).subscribe(ts);
        ts.awaitTerminalEvent();
        ts.assertNoErrors();
        assertEquals(100, ts.values().size());
    }

    private Flowable<Integer> mergeNAsyncStreamsOfN(final int outerSize, final int innerSize) {
        Flowable<Flowable<Integer>> os = Flowable.range(1, outerSize)
        .map(new Function<Integer, Flowable<Integer>>() {

            @Override
            public Flowable<Integer> apply(Integer i) {
                return Flowable.range(1, innerSize).subscribeOn(Schedulers.computation());
            }

        });
        return Flowable.merge(os);
    }

    @Test
    public void merge1SyncStreamOf1() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        mergeNSyncStreamsOfN(1, 1).subscribe(ts);
        ts.awaitTerminalEvent();
        ts.assertNoErrors();
        assertEquals(1, ts.values().size());
    }

    @Test
    public void merge1SyncStreamOf1000000() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        mergeNSyncStreamsOfN(1, 1000000).subscribe(ts);
        ts.awaitTerminalEvent();
        ts.assertNoErrors();
        assertEquals(1000000, ts.values().size());
    }

    @Test
    public void merge1000SyncStreamOf1000() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        mergeNSyncStreamsOfN(1000, 1000).subscribe(ts);
        ts.awaitTerminalEvent();
        ts.assertNoErrors();
        assertEquals(1000000, ts.values().size());
    }

    @Test
    public void merge10000SyncStreamOf10() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        mergeNSyncStreamsOfN(10000, 10).subscribe(ts);
        ts.awaitTerminalEvent();
        ts.assertNoErrors();
        assertEquals(100000, ts.values().size());
    }

    @Test
    public void merge1000000SyncStreamOf1() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        mergeNSyncStreamsOfN(1000000, 1).subscribe(ts);
        ts.awaitTerminalEvent();
        ts.assertNoErrors();
        assertEquals(1000000, ts.values().size());
    }

    private Flowable<Integer> mergeNSyncStreamsOfN(final int outerSize, final int innerSize) {
        Flowable<Flowable<Integer>> os = Flowable.range(1, outerSize)
        .map(new Function<Integer, Flowable<Integer>>() {

            @Override
            public Flowable<Integer> apply(Integer i) {
                return Flowable.range(1, innerSize);
            }

        });
        return Flowable.merge(os);
    }

    private Flowable<Integer> createInfiniteObservable(final AtomicInteger generated) {
        Flowable<Integer> observable = Flowable.fromIterable(new Iterable<Integer>() {
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
        return observable;
    }

    @Test
    public void mergeManyAsyncSingle() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        Flowable<Flowable<Integer>> os = Flowable.range(1, 10000)
        .map(new Function<Integer, Flowable<Integer>>() {

            @Override
            public Flowable<Integer> apply(final Integer i) {
                return Flowable.create(new Publisher<Integer>() {

                    @Override
                    public void subscribe(Subscriber<? super Integer> s) {
                        s.onSubscribe(EmptySubscription.INSTANCE);
                        if (i < 500) {
                            try {
                                Thread.sleep(1);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                        s.onNext(i);
                        s.onComplete();
                    }

                }).subscribeOn(Schedulers.computation()).cache();
            }

        });
        Flowable.merge(os).subscribe(ts);
        ts.awaitTerminalEvent();
        ts.assertNoErrors();
        assertEquals(10000, ts.values().size());
    }

    @Test
    public void shouldCompleteAfterApplyingBackpressure_NormalPath() {
        Flowable<Integer> source = Flowable.mergeDelayError(Flowable.just(Flowable.range(1, 2)));
        TestSubscriber<Integer> subscriber = new TestSubscriber<Integer>((Long)null);
        source.subscribe(subscriber);
        subscriber.request(3); // 1, 2, <complete> - with request(2) we get the 1 and 2 but not the <complete>
        subscriber.assertValues(1, 2);
        subscriber.assertTerminated();
    }

    @Test
    public void shouldCompleteAfterApplyingBackpressure_FastPath() {
        Flowable<Integer> source = Flowable.mergeDelayError(Flowable.just(Flowable.just(1)));
        TestSubscriber<Integer> subscriber = new TestSubscriber<Integer>((Long)null);
        source.subscribe(subscriber);
        subscriber.request(2); // 1, <complete> - should work as per .._NormalPath above
        subscriber.assertValue(1);
        subscriber.assertTerminated();
    }

    @Test
    public void shouldNotCompleteIfThereArePendingScalarSynchronousEmissionsWhenTheLastInnerSubscriberCompletes() {
        TestScheduler scheduler = Schedulers.test();
        Flowable<Long> source = Flowable.mergeDelayError(Flowable.just(1L), Flowable.timer(1, TimeUnit.SECONDS, scheduler).skip(1));
        TestSubscriber<Long> subscriber = new TestSubscriber<Long>((Long)null);
        source.subscribe(subscriber);
        scheduler.advanceTimeBy(1, TimeUnit.SECONDS);
        subscriber.assertNoValues();
        subscriber.assertNotComplete();
        subscriber.request(1);
        subscriber.assertValue(1L);
// TODO: it should be acceptable to get a completion event without requests
//        assertEquals(Collections.<Notification<Long>>emptyList(), subscriber.getOnCompletedEvents());
//        subscriber.request(1);
        subscriber.assertTerminated();
    }

    @Test
    public void delayedErrorsShouldBeEmittedWhenCompleteAfterApplyingBackpressure_NormalPath() {
        Throwable exception = new Throwable();
        Flowable<Integer> source = Flowable.mergeDelayError(Flowable.range(1, 2), Flowable.<Integer>error(exception));
        TestSubscriber<Integer> subscriber = new TestSubscriber<Integer>((Long)null);
        source.subscribe(subscriber);
        subscriber.request(3); // 1, 2, <error>
        subscriber.assertValues(1, 2);
        subscriber.assertTerminated();
        assertEquals(asList(exception), subscriber.errors());
    }

    @Test
    public void delayedErrorsShouldBeEmittedWhenCompleteAfterApplyingBackpressure_FastPath() {
        Throwable exception = new Throwable();
        Flowable<Integer> source = Flowable.mergeDelayError(Flowable.just(1), Flowable.<Integer>error(exception));
        TestSubscriber<Integer> subscriber = new TestSubscriber<Integer>((Long)null);
        source.subscribe(subscriber);
        subscriber.request(2); // 1, <error>
        subscriber.assertValue(1);
        subscriber.assertTerminated();
        assertEquals(asList(exception), subscriber.errors());
    }

    @Test
    public void shouldNotCompleteWhileThereAreStillScalarSynchronousEmissionsInTheQueue() {
        Flowable<Integer> source = Flowable.merge(Flowable.just(1), Flowable.just(2));
        TestSubscriber<Integer> subscriber = new TestSubscriber<Integer>(1L);
        source.subscribe(subscriber);
        subscriber.assertValue(1);
        subscriber.request(1);
        subscriber.assertValues(1, 2);
    }

    @Test
    public void shouldNotReceivedDelayedErrorWhileThereAreStillScalarSynchronousEmissionsInTheQueue() {
        Throwable exception = new Throwable();
        Flowable<Integer> source = Flowable.mergeDelayError(Flowable.just(1), Flowable.just(2), Flowable.<Integer>error(exception));
        TestSubscriber<Integer> subscriber = new TestSubscriber<Integer>((Long)null);
        subscriber.request(1);
        source.subscribe(subscriber);
        subscriber.assertValue(1);
        assertEquals(Collections.<Throwable>emptyList(), subscriber.errors());
        subscriber.request(1);
        subscriber.assertValues(1, 2);
        assertEquals(asList(exception), subscriber.errors());
    }

    @Test
    public void shouldNotReceivedDelayedErrorWhileThereAreStillNormalEmissionsInTheQueue() {
        Throwable exception = new Throwable();
        Flowable<Integer> source = Flowable.mergeDelayError(Flowable.range(1, 2), Flowable.range(3, 2), Flowable.<Integer>error(exception));
        TestSubscriber<Integer> subscriber = new TestSubscriber<Integer>((Long)null);
        subscriber.request(3);
        source.subscribe(subscriber);
        subscriber.assertValues(1, 2, 3);
        assertEquals(Collections.<Throwable>emptyList(), subscriber.errors());
        subscriber.request(2);
        subscriber.assertValues(1, 2, 3, 4);
        assertEquals(asList(exception), subscriber.errors());
    }
    
    @Test
    public void testMergeKeepsRequesting() throws InterruptedException {
        //for (int i = 0; i < 5000; i++) {
            //System.out.println(i + ".......................................................................");
            final CountDownLatch latch = new CountDownLatch(1);
            final ConcurrentLinkedQueue<String> messages = new ConcurrentLinkedQueue<String>();

            Flowable.range(1, 2)
                    // produce many integers per second
                    .flatMap(new Function<Integer, Flowable<Integer>>() {
                        @Override
                        public Flowable<Integer> apply(final Integer number) {
                            return Flowable.range(1, Integer.MAX_VALUE)
                                    .doOnRequest(new LongConsumer() {

                                        @Override
                                        public void accept(long n) {
                                            messages.add(">>>>>>>> A requested[" + number + "]: " + n);
                                        }

                                    })
                                    // pause a bit
                                    .doOnNext(pauseForMs(3))
                                    // buffer on backpressure
                                    .onBackpressureBuffer()
                                    // do in parallel
                                    .subscribeOn(Schedulers.computation())
                                    .doOnRequest(new LongConsumer() {

                                        @Override
                                        public void accept(long n) {
                                            messages.add(">>>>>>>> B requested[" + number + "]: " + n);
                                        }

                                    });
                        }

                    })
                    // take a number bigger than 2* Observable.bufferSize() (used by OperatorMerge)
                    .take(Flowable.bufferSize() * 2 + 1)
                    // log count
                    .doOnNext(printCount())
                    // release latch
                    .doOnComplete(new Runnable() {
                        @Override
                        public void run() {
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
        Flowable<Integer> o = Flowable.fromIterable(Arrays.asList(1,2))
                .mergeWith(Flowable.fromIterable(Arrays.asList(3,4)));
        final int expectedCount = 4;
        final CountDownLatch latch = new CountDownLatch(expectedCount);
        o.subscribeOn(Schedulers.computation()).subscribe(new DefaultObserver<Integer>() {
            
            @Override
            public void onStart() {
                request(1);
            }

            @Override
            public void onComplete() {
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

    private static Consumer<Integer> printCount() {
        return new Consumer<Integer>() {
            long count;

            @Override
            public void accept(Integer t1) {
                count++;
                System.out.println("count=" + count);
            }
        };
    }

    private static Consumer<Integer> pauseForMs(final long time) {
        return new Consumer<Integer>() {
            @Override
            public void accept(Integer s) {
                try {
                    Thread.sleep(time);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }
    
    Function<Integer, Flowable<Integer>> toScalar = new Function<Integer, Flowable<Integer>>() {
        @Override
        public Flowable<Integer> apply(Integer v) {
            return Flowable.just(v);
        }
    };
    
    Function<Integer, Flowable<Integer>> toHiddenScalar = new Function<Integer, Flowable<Integer>>() {
        @Override
        public Flowable<Integer> apply(Integer t) {
            return Flowable.just(t).asObservable();
        }
    };
    ;
    
    void runMerge(Function<Integer, Flowable<Integer>> func, TestSubscriber<Integer> ts) {
        List<Integer> list = new ArrayList<Integer>();
        for (int i = 0; i < 1000; i++) {
            list.add(i);
        }
        Flowable<Integer> source = Flowable.fromIterable(list);
        source.flatMap(func).subscribe(ts);
        
        if (ts.values().size() != 1000) {
            System.out.println(ts.values());
        }
        
        ts.assertTerminated();
        ts.assertNoErrors();
        ts.assertValueSequence(list);
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
            TestSubscriber<Integer> ts = new TestSubscriber<Integer>((long)req) {
                int remaining = req;

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
            TestSubscriber<Integer> ts = new TestSubscriber<Integer>((long)req) {
                int remaining = req;
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
}