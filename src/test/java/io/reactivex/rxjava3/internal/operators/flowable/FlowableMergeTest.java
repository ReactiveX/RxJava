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

package io.reactivex.rxjava3.internal.operators.flowable;

import static java.util.Arrays.asList;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.junit.*;
import org.reactivestreams.*;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.core.Scheduler.Worker;
import io.reactivex.rxjava3.exceptions.TestException;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.internal.functions.Functions;
import io.reactivex.rxjava3.internal.subscriptions.*;
import io.reactivex.rxjava3.internal.util.*;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.processors.PublishProcessor;
import io.reactivex.rxjava3.schedulers.*;
import io.reactivex.rxjava3.subscribers.*;
import io.reactivex.rxjava3.testsupport.*;

public class FlowableMergeTest extends RxJavaTest {

    Subscriber<String> stringSubscriber;

    int count;

    @Before
    public void before() {
        stringSubscriber = TestHelper.mockSubscriber();

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
    public void mergeFlowableOfFlowables() {
        final Flowable<String> f1 = Flowable.unsafeCreate(new TestSynchronousFlowable());
        final Flowable<String> f2 = Flowable.unsafeCreate(new TestSynchronousFlowable());

        Flowable<Flowable<String>> flowableOfFlowables = Flowable.unsafeCreate(new Publisher<Flowable<String>>() {

            @Override
            public void subscribe(Subscriber<? super Flowable<String>> subscriber) {
                subscriber.onSubscribe(new BooleanSubscription());
                // simulate what would happen in a Flowable
                subscriber.onNext(f1);
                subscriber.onNext(f2);
                subscriber.onComplete();
            }

        });
        Flowable<String> m = Flowable.merge(flowableOfFlowables);
        m.subscribe(stringSubscriber);

        verify(stringSubscriber, never()).onError(any(Throwable.class));
        verify(stringSubscriber, times(1)).onComplete();
        verify(stringSubscriber, times(2)).onNext("hello");
    }

    @Test
    public void mergeArray() {
        final Flowable<String> f1 = Flowable.unsafeCreate(new TestSynchronousFlowable());
        final Flowable<String> f2 = Flowable.unsafeCreate(new TestSynchronousFlowable());

        Flowable<String> m = Flowable.merge(f1, f2);
        m.subscribe(stringSubscriber);

        verify(stringSubscriber, never()).onError(any(Throwable.class));
        verify(stringSubscriber, times(2)).onNext("hello");
        verify(stringSubscriber, times(1)).onComplete();
    }

    @Test
    public void mergeList() {
        final Flowable<String> f1 = Flowable.unsafeCreate(new TestSynchronousFlowable());
        final Flowable<String> f2 = Flowable.unsafeCreate(new TestSynchronousFlowable());
        List<Flowable<String>> listOfFlowables = new ArrayList<>();
        listOfFlowables.add(f1);
        listOfFlowables.add(f2);

        Flowable<String> m = Flowable.merge(listOfFlowables);
        m.subscribe(stringSubscriber);

        verify(stringSubscriber, never()).onError(any(Throwable.class));
        verify(stringSubscriber, times(1)).onComplete();
        verify(stringSubscriber, times(2)).onNext("hello");
    }

    @Test
    public void unSubscribeFlowableOfFlowables() throws InterruptedException {

        final AtomicBoolean unsubscribed = new AtomicBoolean();
        final CountDownLatch latch = new CountDownLatch(1);

        Flowable<Flowable<Long>> source = Flowable.unsafeCreate(new Publisher<Flowable<Long>>() {

            @Override
            public void subscribe(final Subscriber<? super Flowable<Long>> subscriber) {
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
                subscriber.onSubscribe(s);

                new Thread(new Runnable() {

                    @Override
                    public void run() {

                        while (!unsubscribed.get()) {
                            subscriber.onNext(Flowable.just(1L, 2L));
                        }
                        System.out.println("Done looping after unsubscribe: " + unsubscribed.get());
                        subscriber.onComplete();

                        // mark that the thread is finished
                        latch.countDown();
                    }
                }).start();
            }

        });

        final AtomicInteger count = new AtomicInteger();
        Flowable.merge(source).take(6).blockingForEach(new Consumer<Long>() {

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
    public void mergeArrayWithThreading() {
        final TestASynchronousFlowable f1 = new TestASynchronousFlowable();
        final TestASynchronousFlowable f2 = new TestASynchronousFlowable();

        Flowable<String> m = Flowable.merge(Flowable.unsafeCreate(f1), Flowable.unsafeCreate(f2));
        TestSubscriber<String> ts = new TestSubscriber<>(stringSubscriber);
        m.subscribe(ts);

        ts.awaitDone(5, TimeUnit.SECONDS);
        ts.assertNoErrors();

        verify(stringSubscriber, never()).onError(any(Throwable.class));
        verify(stringSubscriber, times(2)).onNext("hello");
        verify(stringSubscriber, times(1)).onComplete();
    }

    @Test
    public void synchronizationOfMultipleSequencesLoop() throws Throwable {
        for (int i = 0; i < 100; i++) {
            System.out.println("testSynchronizationOfMultipleSequencesLoop > " + i);
            synchronizationOfMultipleSequences();
        }
    }

    @Test
    public void synchronizationOfMultipleSequences() throws Throwable {
        final TestASynchronousFlowable f1 = new TestASynchronousFlowable();
        final TestASynchronousFlowable f2 = new TestASynchronousFlowable();

        // use this latch to cause onNext to wait until we're ready to let it go
        final CountDownLatch endLatch = new CountDownLatch(1);

        final AtomicInteger concurrentCounter = new AtomicInteger();
        final AtomicInteger totalCounter = new AtomicInteger();

        final AtomicReference<Throwable> error = new AtomicReference<>();

        Flowable<String> m = Flowable.merge(Flowable.unsafeCreate(f1), Flowable.unsafeCreate(f2));
        m.subscribe(new DefaultSubscriber<String>() {

            @Override
            public void onComplete() {

            }

            @Override
            public void onError(Throwable e) {
                error.set(e);
            }

            @Override
            public void onNext(String v) {
                totalCounter.incrementAndGet();
                concurrentCounter.incrementAndGet();
                try {
                    // avoid deadlocking the main thread
                    if (Thread.currentThread().getName().equals("TestASynchronousFlowable")) {
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

        // wait for both Flowables to send (one should be blocked)
        f1.onNextBeingSent.await();
        f2.onNextBeingSent.await();

        // I can't think of a way to know for sure that both threads have or are trying to send onNext
        // since I can't use a CountDownLatch for "after" onNext since I want to catch during it
        // but I can't know for sure onNext is invoked
        // so I'm unfortunately reverting to using a Thread.sleep to allow the process scheduler time
        // to make sure after o1.onNextBeingSent and o2.onNextBeingSent are hit that the following
        // onNext is invoked.

        int timeout = 20;

        while (timeout-- > 0 && concurrentCounter.get() != 1) {
            Thread.sleep(100);
        }

        try { // in try/finally so threads are released via latch countDown even if assertion fails
            if (error.get() != null) {
                throw ExceptionHelper.wrapOrThrow(error.get());
            }

            assertEquals(1, concurrentCounter.get());
        } finally {
            // release so it can finish
            endLatch.countDown();
        }

        try {
            f1.t.join();
            f2.t.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        assertEquals(2, totalCounter.get());
        assertEquals(0, concurrentCounter.get());
    }

    /**
     * Unit test from OperationMergeDelayError backported here to show how these use cases work with normal merge.
     */
    @Test
    public void error1() {
        // we are using synchronous execution to test this exactly rather than non-deterministic concurrent behavior
        final Flowable<String> f1 = Flowable.unsafeCreate(new TestErrorFlowable("four", null, "six")); // we expect to lose "six"
        final Flowable<String> f2 = Flowable.unsafeCreate(new TestErrorFlowable("one", "two", "three")); // we expect to lose all of these since o1 is done first and fails

        Flowable<String> m = Flowable.merge(f1, f2);
        m.subscribe(stringSubscriber);

        verify(stringSubscriber, times(1)).onError(any(NullPointerException.class));
        verify(stringSubscriber, never()).onComplete();
        verify(stringSubscriber, times(0)).onNext("one");
        verify(stringSubscriber, times(0)).onNext("two");
        verify(stringSubscriber, times(0)).onNext("three");
        verify(stringSubscriber, times(1)).onNext("four");
        verify(stringSubscriber, times(0)).onNext("five");
        verify(stringSubscriber, times(0)).onNext("six");
    }

    /**
     * Unit test from OperationMergeDelayError backported here to show how these use cases work with normal merge.
     */
    @Test
    public void error2() {
        // we are using synchronous execution to test this exactly rather than non-deterministic concurrent behavior
        final Flowable<String> f1 = Flowable.unsafeCreate(new TestErrorFlowable("one", "two", "three"));
        final Flowable<String> f2 = Flowable.unsafeCreate(new TestErrorFlowable("four", null, "six")); // we expect to lose "six"
        final Flowable<String> f3 = Flowable.unsafeCreate(new TestErrorFlowable("seven", "eight", null)); // we expect to lose all of these since o2 is done first and fails
        final Flowable<String> f4 = Flowable.unsafeCreate(new TestErrorFlowable("nine")); // we expect to lose all of these since o2 is done first and fails

        Flowable<String> m = Flowable.merge(f1, f2, f3, f4);
        m.subscribe(stringSubscriber);

        verify(stringSubscriber, times(1)).onError(any(NullPointerException.class));
        verify(stringSubscriber, never()).onComplete();
        verify(stringSubscriber, times(1)).onNext("one");
        verify(stringSubscriber, times(1)).onNext("two");
        verify(stringSubscriber, times(1)).onNext("three");
        verify(stringSubscriber, times(1)).onNext("four");
        verify(stringSubscriber, times(0)).onNext("five");
        verify(stringSubscriber, times(0)).onNext("six");
        verify(stringSubscriber, times(0)).onNext("seven");
        verify(stringSubscriber, times(0)).onNext("eight");
        verify(stringSubscriber, times(0)).onNext("nine");
    }

    private static class TestSynchronousFlowable implements Publisher<String> {

        @Override
        public void subscribe(Subscriber<? super String> subscriber) {
            subscriber.onSubscribe(new BooleanSubscription());
            subscriber.onNext("hello");
            subscriber.onComplete();
        }
    }

    private static class TestASynchronousFlowable implements Publisher<String> {
        Thread t;
        final CountDownLatch onNextBeingSent = new CountDownLatch(1);

        @Override
        public void subscribe(final Subscriber<? super String> subscriber) {
            subscriber.onSubscribe(new BooleanSubscription());
            t = new Thread(new Runnable() {

                @Override
                public void run() {
                    onNextBeingSent.countDown();
                    try {
                        subscriber.onNext("hello");
                        // I can't use a countDownLatch to prove we are actually sending 'onNext'
                        // since it will block if synchronized and I'll deadlock
                        subscriber.onComplete();
                    } catch (Exception e) {
                        subscriber.onError(e);
                    }
                }

            }, "TestASynchronousFlowable");
            t.start();
        }
    }

    private static class TestErrorFlowable implements Publisher<String> {

        String[] valuesToReturn;

        TestErrorFlowable(String... values) {
            valuesToReturn = values;
        }

        @Override
        public void subscribe(Subscriber<? super String> subscriber) {
            subscriber.onSubscribe(new BooleanSubscription());
            for (String s : valuesToReturn) {
                if (s == null) {
                    System.out.println("throwing exception");
                    subscriber.onError(new NullPointerException());
                } else {
                    subscriber.onNext(s);
                }
            }
            subscriber.onComplete();
        }
    }

    @Test
    public void unsubscribeAsFlowablesComplete() {
        TestScheduler scheduler1 = new TestScheduler();
        AtomicBoolean os1 = new AtomicBoolean(false);
        Flowable<Long> f1 = createFlowableOf5IntervalsOf1SecondIncrementsWithSubscriptionHook(scheduler1, os1);

        TestScheduler scheduler2 = new TestScheduler();
        AtomicBoolean os2 = new AtomicBoolean(false);
        Flowable<Long> f2 = createFlowableOf5IntervalsOf1SecondIncrementsWithSubscriptionHook(scheduler2, os2);

        TestSubscriberEx<Long> ts = new TestSubscriberEx<>();
        Flowable.merge(f1, f2).subscribe(ts);

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
    public void earlyUnsubscribe() {
        for (int i = 0; i < 10; i++) {
            TestScheduler scheduler1 = new TestScheduler();
            AtomicBoolean os1 = new AtomicBoolean(false);
            Flowable<Long> f1 = createFlowableOf5IntervalsOf1SecondIncrementsWithSubscriptionHook(scheduler1, os1);

            TestScheduler scheduler2 = new TestScheduler();
            AtomicBoolean os2 = new AtomicBoolean(false);
            Flowable<Long> f2 = createFlowableOf5IntervalsOf1SecondIncrementsWithSubscriptionHook(scheduler2, os2);

            TestSubscriber<Long> ts = new TestSubscriber<>();
            Flowable.merge(f1, f2).subscribe(ts);

            // we haven't incremented time so nothing should be received yet
            ts.assertNoValues();

            scheduler1.advanceTimeBy(3, TimeUnit.SECONDS);
            scheduler2.advanceTimeBy(2, TimeUnit.SECONDS);

            ts.assertValues(0L, 1L, 2L, 0L, 1L);
            // not unsubscribed yet
            assertFalse(os1.get());
            assertFalse(os2.get());

            // early unsubscribe
            ts.cancel();

            assertTrue(os1.get());
            assertTrue(os2.get());

            ts.assertValues(0L, 1L, 2L, 0L, 1L);
        }
    }

    private Flowable<Long> createFlowableOf5IntervalsOf1SecondIncrementsWithSubscriptionHook(final Scheduler scheduler, final AtomicBoolean unsubscribed) {
        return Flowable.unsafeCreate(new Publisher<Long>() {

            @Override
            public void subscribe(final Subscriber<? super Long> child) {
                Flowable.interval(1, TimeUnit.SECONDS, scheduler)
                .take(5)
                .subscribe(new FlowableSubscriber<Long>() {
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

    @Test
    public void concurrency() {
        Flowable<Integer> f = Flowable.range(1, 10000).subscribeOn(Schedulers.newThread());

        for (int i = 0; i < 10; i++) {
            Flowable<Integer> merge = Flowable.merge(f.onBackpressureBuffer(), f.onBackpressureBuffer(), f.onBackpressureBuffer());
            TestSubscriberEx<Integer> ts = new TestSubscriberEx<>();
            merge.subscribe(ts);

            ts.awaitDone(3, TimeUnit.SECONDS);
            ts.assertTerminated();
            ts.assertNoErrors();
            ts.assertComplete();
            List<Integer> onNextEvents = ts.values();
            assertEquals(30000, onNextEvents.size());
            //            System.out.println("onNext: " + onNextEvents.size() + " onComplete: " + ts.getOnCompletedEvents().size());
        }
    }

    @Test
    public void concurrencyWithSleeping() {

        Flowable<Integer> f = Flowable.unsafeCreate(new Publisher<Integer>() {

            @Override
            public void subscribe(final Subscriber<? super Integer> s) {
                Worker inner = Schedulers.newThread().createWorker();
                final AsyncSubscription as = new AsyncSubscription();
                as.setSubscription(new BooleanSubscription());
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
            Flowable<Integer> merge = Flowable.merge(f, f, f);
            TestSubscriber<Integer> ts = new TestSubscriber<>();
            merge.subscribe(ts);

            ts.awaitDone(5, TimeUnit.SECONDS);
            ts.assertComplete();
            List<Integer> onNextEvents = ts.values();
            assertEquals(300, onNextEvents.size());
            //            System.out.println("onNext: " + onNextEvents.size() + " onComplete: " + ts.getOnCompletedEvents().size());
        }
    }

    @Test
    public void concurrencyWithBrokenOnCompleteContract() {
        Flowable<Integer> f = Flowable.unsafeCreate(new Publisher<Integer>() {

            @Override
            public void subscribe(final Subscriber<? super Integer> s) {
                Worker inner = Schedulers.newThread().createWorker();
                final AsyncSubscription as = new AsyncSubscription();
                as.setSubscription(new BooleanSubscription());
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
            Flowable<Integer> merge = Flowable.merge(f.onBackpressureBuffer(), f.onBackpressureBuffer(), f.onBackpressureBuffer());
            TestSubscriber<Integer> ts = new TestSubscriber<>();
            merge.subscribe(ts);

            ts.awaitDone(5, TimeUnit.SECONDS);
            ts.assertNoErrors();
            ts.assertComplete();
            List<Integer> onNextEvents = ts.values();
            assertEquals(30000, onNextEvents.size());
            //                System.out.println("onNext: " + onNextEvents.size() + " onComplete: " + ts.getOnCompletedEvents().size());
        }
    }

    @Test
    public void backpressureUpstream() throws InterruptedException {
        final AtomicInteger generated1 = new AtomicInteger();
        Flowable<Integer> f1 = createInfiniteFlowable(generated1).subscribeOn(Schedulers.computation());
        final AtomicInteger generated2 = new AtomicInteger();
        Flowable<Integer> f2 = createInfiniteFlowable(generated2).subscribeOn(Schedulers.computation());

        TestSubscriberEx<Integer> testSubscriber = new TestSubscriberEx<Integer>() {
            @Override
            public void onNext(Integer t) {
                System.err.println("testSubscriber received => " + t + "  on thread " + Thread.currentThread());
                super.onNext(t);
            }
        };

        Flowable.merge(f1.take(Flowable.bufferSize() * 2), f2.take(Flowable.bufferSize() * 2)).subscribe(testSubscriber);
        testSubscriber.awaitDone(5, TimeUnit.SECONDS);
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
    public void backpressureUpstream2InLoop() throws InterruptedException {
        for (int i = 0; i < 1000; i++) {
            System.err.flush();
            System.out.println("---");
            System.out.flush();
            backpressureUpstream2();
        }
    }

    @Test
    public void backpressureUpstream2() throws InterruptedException {
        final AtomicInteger generated1 = new AtomicInteger();
        Flowable<Integer> f1 = createInfiniteFlowable(generated1).subscribeOn(Schedulers.computation());

        TestSubscriberEx<Integer> testSubscriber = new TestSubscriberEx<Integer>() {
            @Override
            public void onNext(Integer t) {
                super.onNext(t);
            }
        };

        Flowable.merge(f1.take(Flowable.bufferSize() * 2), Flowable.just(-99)).subscribe(testSubscriber);
        testSubscriber.awaitDone(5, TimeUnit.SECONDS);

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
    @Test
    public void backpressureDownstreamWithConcurrentStreams() throws InterruptedException {
        final AtomicInteger generated1 = new AtomicInteger();
        Flowable<Integer> f1 = createInfiniteFlowable(generated1).subscribeOn(Schedulers.computation());
        final AtomicInteger generated2 = new AtomicInteger();
        Flowable<Integer> f2 = createInfiniteFlowable(generated2).subscribeOn(Schedulers.computation());

        TestSubscriberEx<Integer> testSubscriber = new TestSubscriberEx<Integer>() {
            @Override
            public void onNext(Integer t) {
                if (t < 100) {
                    try {
                        // force a slow consumer
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                //                System.err.println("testSubscriber received => " + t + "  on thread " + Thread.currentThread());
                super.onNext(t);
            }
        };

        Flowable.merge(f1.take(Flowable.bufferSize() * 2), f2.take(Flowable.bufferSize() * 2)).observeOn(Schedulers.computation()).subscribe(testSubscriber);
        testSubscriber.awaitDone(5, TimeUnit.SECONDS);
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
    public void backpressureBothUpstreamAndDownstreamWithSynchronousScalarFlowables() throws InterruptedException {
        final AtomicInteger generated1 = new AtomicInteger();
        Flowable<Flowable<Integer>> f1 = createInfiniteFlowable(generated1)
        .map(new Function<Integer, Flowable<Integer>>() {

            @Override
            public Flowable<Integer> apply(Integer t1) {
                return Flowable.just(t1);
            }

        });

        TestSubscriberEx<Integer> testSubscriber = new TestSubscriberEx<Integer>() {
            @Override
            public void onNext(Integer t) {
                if (t < 100) {
                    try {
                        // force a slow consumer
                        Thread.sleep(2);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                //                System.err.println("testSubscriber received => " + t + "  on thread " + Thread.currentThread());
                super.onNext(t);
            }
        };

        Flowable.merge(f1).observeOn(Schedulers.computation()).take(Flowable.bufferSize() * 2).subscribe(testSubscriber);
        testSubscriber.awaitDone(5, TimeUnit.SECONDS);
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
     * Currently there is no solution to this ... we can't exert backpressure on the outer Flowable if we
     * can't know if the ones we've received so far are going to emit or not, otherwise we could starve the system.
     *
     * For example, 10,000 Flowables are being merged (bad use case to begin with, but ...) and it's only one of them
     * that will ever emit. If backpressure only allowed the first 1,000 to be sent, we would hang and never receive an event.
     *
     * Thus, we must allow all Flowables to be sent. The ScalarSynchronousFlowable use case is an exception to this since
     * we can grab the value synchronously.
     *
     * @throws InterruptedException if the await is interrupted
     */
    @Test
    public void backpressureBothUpstreamAndDownstreamWithRegularFlowables() throws InterruptedException {
        final AtomicInteger generated1 = new AtomicInteger();
        Flowable<Flowable<Integer>> f1 = createInfiniteFlowable(generated1).map(new Function<Integer, Flowable<Integer>>() {

            @Override
            public Flowable<Integer> apply(Integer t1) {
                return Flowable.just(1, 2, 3);
            }

        });

        TestSubscriberEx<Integer> testSubscriber = new TestSubscriberEx<Integer>() {
            int i;

            @Override
            public void onNext(Integer t) {
                if (i++ < 400) {
                    try {
                        // force a slow consumer
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                //                System.err.println("testSubscriber received => " + t + "  on thread " + Thread.currentThread());
                super.onNext(t);
            }
        };

        Flowable.merge(f1).observeOn(Schedulers.computation()).take(Flowable.bufferSize() * 2).subscribe(testSubscriber);
        testSubscriber.awaitDone(5, TimeUnit.SECONDS);
        if (testSubscriber.errors().size() > 0) {
            testSubscriber.errors().get(0).printStackTrace();
        }
        testSubscriber.assertNoErrors();
        System.out.println("Generated 1: " + generated1.get());
        System.err.println(testSubscriber.values());
        System.out.println("done1 testBackpressureBothUpstreamAndDownstreamWithRegularFlowables ");
        assertEquals(Flowable.bufferSize() * 2, testSubscriber.values().size());
        System.out.println("done2 testBackpressureBothUpstreamAndDownstreamWithRegularFlowables ");
        // we can't restrict this ... see comment above
        //        assertTrue(generated1.get() >= Flowable.bufferSize() && generated1.get() <= Flowable.bufferSize() * 4);
    }

    @Test
    public void merge1AsyncStreamOf1() {
        TestSubscriber<Integer> ts = new TestSubscriber<>();
        mergeNAsyncStreamsOfN(1, 1).subscribe(ts);
        ts.awaitDone(5, TimeUnit.SECONDS);
        ts.assertNoErrors();
        assertEquals(1, ts.values().size());
    }

    @Test
    public void merge1AsyncStreamOf1000() {
        TestSubscriber<Integer> ts = new TestSubscriber<>();
        mergeNAsyncStreamsOfN(1, 1000).subscribe(ts);
        ts.awaitDone(5, TimeUnit.SECONDS);
        ts.assertNoErrors();
        assertEquals(1000, ts.values().size());
    }

    @Test
    public void merge10AsyncStreamOf1000() {
        TestSubscriber<Integer> ts = new TestSubscriber<>();
        mergeNAsyncStreamsOfN(10, 1000).subscribe(ts);
        ts.awaitDone(5, TimeUnit.SECONDS);
        ts.assertNoErrors();
        assertEquals(10000, ts.values().size());
    }

    @Test
    public void merge1000AsyncStreamOf1000() {
        TestSubscriber<Integer> ts = new TestSubscriber<>();
        mergeNAsyncStreamsOfN(1000, 1000).subscribe(ts);
        ts.awaitDone(5, TimeUnit.SECONDS);
        ts.assertNoErrors();
        assertEquals(1000000, ts.values().size());
    }

    @Test
    public void merge2000AsyncStreamOf100() {
        TestSubscriber<Integer> ts = new TestSubscriber<>();
        mergeNAsyncStreamsOfN(2000, 100).subscribe(ts);
        ts.awaitDone(5, TimeUnit.SECONDS);
        ts.assertNoErrors();
        assertEquals(200000, ts.values().size());
    }

    @Test
    public void merge100AsyncStreamOf1() {
        TestSubscriber<Integer> ts = new TestSubscriber<>();
        mergeNAsyncStreamsOfN(100, 1).subscribe(ts);
        ts.awaitDone(5, TimeUnit.SECONDS);
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
        TestSubscriber<Integer> ts = new TestSubscriber<>();
        mergeNSyncStreamsOfN(1, 1).subscribe(ts);
        ts.awaitDone(5, TimeUnit.SECONDS);
        ts.assertNoErrors();
        assertEquals(1, ts.values().size());
    }

    @Test
    public void merge1SyncStreamOf1000000() {
        TestSubscriber<Integer> ts = new TestSubscriber<>();
        mergeNSyncStreamsOfN(1, 1000000).subscribe(ts);
        ts.awaitDone(5, TimeUnit.SECONDS);
        ts.assertNoErrors();
        assertEquals(1000000, ts.values().size());
    }

    @Test
    public void merge1000SyncStreamOf1000() {
        TestSubscriber<Integer> ts = new TestSubscriber<>();
        mergeNSyncStreamsOfN(1000, 1000).subscribe(ts);
        ts.awaitDone(5, TimeUnit.SECONDS);
        ts.assertNoErrors();
        assertEquals(1000000, ts.values().size());
    }

    @Test
    public void merge10000SyncStreamOf10() {
        TestSubscriber<Integer> ts = new TestSubscriber<>();
        mergeNSyncStreamsOfN(10000, 10).subscribe(ts);
        ts.awaitDone(5, TimeUnit.SECONDS);
        ts.assertNoErrors();
        assertEquals(100000, ts.values().size());
    }

    @Test
    public void merge1000000SyncStreamOf1() {
        TestSubscriber<Integer> ts = new TestSubscriber<>();
        mergeNSyncStreamsOfN(1000000, 1).subscribe(ts);
        ts.awaitDone(5, TimeUnit.SECONDS);
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

    private Flowable<Integer> createInfiniteFlowable(final AtomicInteger generated) {
        Flowable<Integer> flowable = Flowable.fromIterable(new Iterable<Integer>() {
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
        return flowable;
    }

    @Test
    public void mergeManyAsyncSingle() {
        TestSubscriber<Integer> ts = new TestSubscriber<>();
        Flowable<Flowable<Integer>> os = Flowable.range(1, 10000)
        .map(new Function<Integer, Flowable<Integer>>() {

            @Override
            public Flowable<Integer> apply(final Integer i) {
                return Flowable.unsafeCreate(new Publisher<Integer>() {

                    @Override
                    public void subscribe(Subscriber<? super Integer> s) {
                        s.onSubscribe(new BooleanSubscription());
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
        ts.awaitDone(5, TimeUnit.SECONDS);
        ts.assertNoErrors();
        assertEquals(10000, ts.values().size());
    }

    @Test
    public void shouldCompleteAfterApplyingBackpressure_NormalPath() {
        Flowable<Integer> source = Flowable.mergeDelayError(Flowable.just(Flowable.range(1, 2)));
        TestSubscriberEx<Integer> subscriber = new TestSubscriberEx<>(0L);
        source.subscribe(subscriber);
        subscriber.request(3); // 1, 2, <complete> - with request(2) we get the 1 and 2 but not the <complete>
        subscriber.assertValues(1, 2);
        subscriber.assertTerminated();
    }

    @Test
    public void shouldCompleteAfterApplyingBackpressure_FastPath() {
        Flowable<Integer> source = Flowable.mergeDelayError(Flowable.just(Flowable.just(1)));
        TestSubscriberEx<Integer> subscriber = new TestSubscriberEx<>(0L);
        source.subscribe(subscriber);
        subscriber.request(2); // 1, <complete> - should work as per .._NormalPath above
        subscriber.assertValue(1);
        subscriber.assertTerminated();
    }

    @Test
    public void shouldNotCompleteIfThereArePendingScalarSynchronousEmissionsWhenTheLastInnerSubscriberCompletes() {
        TestScheduler scheduler = new TestScheduler();
        Flowable<Long> source = Flowable.mergeDelayError(Flowable.just(1L), Flowable.timer(1, TimeUnit.SECONDS, scheduler).skip(1));
        TestSubscriberEx<Long> subscriber = new TestSubscriberEx<>(0L);
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
        TestSubscriberEx<Integer> subscriber = new TestSubscriberEx<>(0L);
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
        TestSubscriberEx<Integer> subscriber = new TestSubscriberEx<>(0L);
        source.subscribe(subscriber);
        subscriber.request(2); // 1, <error>
        subscriber.assertValue(1);
        subscriber.assertTerminated();
        assertEquals(asList(exception), subscriber.errors());
    }

    @Test
    public void shouldNotCompleteWhileThereAreStillScalarSynchronousEmissionsInTheQueue() {
        Flowable<Integer> source = Flowable.merge(Flowable.just(1), Flowable.just(2));
        TestSubscriber<Integer> subscriber = new TestSubscriber<>(1L);
        source.subscribe(subscriber);
        subscriber.assertValue(1);
        subscriber.request(1);
        subscriber.assertValues(1, 2);
    }

    @Test
    public void shouldNotReceivedDelayedErrorWhileThereAreStillScalarSynchronousEmissionsInTheQueue() {
        Throwable exception = new Throwable();
        Flowable<Integer> source = Flowable.mergeDelayError(Flowable.just(1), Flowable.just(2), Flowable.<Integer>error(exception));
        TestSubscriberEx<Integer> subscriber = new TestSubscriberEx<>(0L);
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
        TestSubscriberEx<Integer> subscriber = new TestSubscriberEx<>(0L);
        subscriber.request(3);
        source.subscribe(subscriber);
        subscriber.assertValues(1, 2, 3);
        assertEquals(Collections.<Throwable>emptyList(), subscriber.errors());
        subscriber.request(2);
        subscriber.assertValues(1, 2, 3, 4);
        assertEquals(asList(exception), subscriber.errors());
    }

    @Test
    public void mergeKeepsRequesting() throws InterruptedException {
        //for (int i = 0; i < 5000; i++) {
            //System.out.println(i + ".......................................................................");
            final CountDownLatch latch = new CountDownLatch(1);
            final ConcurrentLinkedQueue<String> messages = new ConcurrentLinkedQueue<>();

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
                    // take a number bigger than 2* Flowable.bufferSize() (used by OperatorMerge)
                    .take(Flowable.bufferSize() * 2 + 1)
                    // log count
                    .doOnNext(printCount())
                    // release latch
                    .doOnComplete(new Action() {
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
    public void mergeRequestOverflow() throws InterruptedException {
        //do a non-trivial merge so that future optimisations with EMPTY don't invalidate this test
        Flowable<Integer> f = Flowable.fromIterable(Arrays.asList(1, 2))
                .mergeWith(Flowable.fromIterable(Arrays.asList(3, 4)));
        final int expectedCount = 4;
        final CountDownLatch latch = new CountDownLatch(expectedCount);
        f.subscribeOn(Schedulers.computation()).subscribe(new DefaultSubscriber<Integer>() {

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
                request(Long.MAX_VALUE - 1);
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
            return Flowable.just(t).hide();
        }
    };
    ;

    void runMerge(Function<Integer, Flowable<Integer>> func, TestSubscriberEx<Integer> ts) {
        List<Integer> list = new ArrayList<>();
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
    public void fastMergeFullScalar() {
        runMerge(toScalar, new TestSubscriberEx<>());
    }

    @Test
    public void fastMergeHiddenScalar() {
        runMerge(toHiddenScalar, new TestSubscriberEx<>());
    }

    @Test
    public void slowMergeFullScalar() {
        for (final int req : new int[] { 16, 32, 64, 128, 256 }) {
            TestSubscriberEx<Integer> ts = new TestSubscriberEx<Integer>(req) {
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
    public void slowMergeHiddenScalar() {
        for (final int req : new int[] { 16, 32, 64, 128, 256 }) {
            TestSubscriberEx<Integer> ts = new TestSubscriberEx<Integer>(req) {
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

    @Test
    public void negativeMaxConcurrent() {
        try {
            Flowable.merge(Arrays.asList(Flowable.just(1), Flowable.just(2)), -1);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertEquals("maxConcurrency > 0 required but it was -1", e.getMessage());
        }
    }

    @Test
    public void zeroMaxConcurrent() {
        try {
            Flowable.merge(Arrays.asList(Flowable.just(1), Flowable.just(2)), 0);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertEquals("maxConcurrency > 0 required but it was 0", e.getMessage());
        }
    }

    @Test
    public void mergeConcurrentJustJust() {
        TestSubscriber<Integer> ts = TestSubscriber.create();

        Flowable.merge(Flowable.just(Flowable.just(1)), 5).subscribe(ts);

        ts.assertValue(1);
        ts.assertNoErrors();
        ts.assertComplete();
    }

    @Test
    public void mergeConcurrentJustRange() {
        TestSubscriber<Integer> ts = TestSubscriber.create();

        Flowable.merge(Flowable.just(Flowable.range(1, 5)), 5).subscribe(ts);

        ts.assertValues(1, 2, 3, 4, 5);
        ts.assertNoErrors();
        ts.assertComplete();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void mergeArrayMaxConcurrent() {
        TestSubscriber<Integer> ts = TestSubscriber.create();

        PublishProcessor<Integer> pp1 = PublishProcessor.create();
        PublishProcessor<Integer> pp2 = PublishProcessor.create();

        Flowable.mergeArray(1, 128, new Flowable[] { pp1, pp2 }).subscribe(ts);

        assertTrue("ps1 has no subscribers?!", pp1.hasSubscribers());
        assertFalse("ps2 has subscribers?!", pp2.hasSubscribers());

        pp1.onNext(1);
        pp1.onComplete();

        assertFalse("ps1 has subscribers?!", pp1.hasSubscribers());
        assertTrue("ps2 has no subscribers?!", pp2.hasSubscribers());

        pp2.onNext(2);
        pp2.onComplete();

        ts.assertValues(1, 2);
        ts.assertNoErrors();
        ts.assertComplete();
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void flatMapJustJust() {
        TestSubscriber<Integer> ts = TestSubscriber.create();

        Flowable.just(Flowable.just(1)).flatMap((Function)Functions.identity()).subscribe(ts);

        ts.assertValue(1);
        ts.assertNoErrors();
        ts.assertComplete();
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void flatMapJustRange() {
        TestSubscriber<Integer> ts = TestSubscriber.create();

        Flowable.just(Flowable.range(1, 5)).flatMap((Function)Functions.identity()).subscribe(ts);

        ts.assertValues(1, 2, 3, 4, 5);
        ts.assertNoErrors();
        ts.assertComplete();
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void flatMapMaxConcurrentJustJust() {
        TestSubscriber<Integer> ts = TestSubscriber.create();

        Flowable.just(Flowable.just(1)).flatMap((Function)Functions.identity(), 5).subscribe(ts);

        ts.assertValue(1);
        ts.assertNoErrors();
        ts.assertComplete();
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void flatMapMaxConcurrentJustRange() {
        TestSubscriber<Integer> ts = TestSubscriber.create();

        Flowable.just(Flowable.range(1, 5)).flatMap((Function)Functions.identity(), 5).subscribe(ts);

        ts.assertValues(1, 2, 3, 4, 5);
        ts.assertNoErrors();
        ts.assertComplete();
    }

    @Test
    public void noInnerReordering() {
        TestSubscriber<Integer> ts = TestSubscriber.create(0);
        FlowableFlatMap.MergeSubscriber<Publisher<Integer>, Integer> ms =
                new FlowableFlatMap.MergeSubscriber<>(ts, Functions.<Publisher<Integer>>identity(), false, 128, 128);
        ms.onSubscribe(new BooleanSubscription());

        PublishProcessor<Integer> pp = PublishProcessor.create();

        ms.onNext(pp);

        pp.onNext(1);

        BackpressureHelper.add(ms.requested, 2);

        pp.onNext(2);

        ms.drain();

        ts.assertValues(1, 2);
    }

    @Test
    public void noOuterScalarReordering() {
        TestSubscriber<Integer> ts = TestSubscriber.create(0);
        FlowableFlatMap.MergeSubscriber<Publisher<Integer>, Integer> ms =
                new FlowableFlatMap.MergeSubscriber<>(ts, Functions.<Publisher<Integer>>identity(), false, 128, 128);
        ms.onSubscribe(new BooleanSubscription());

        ms.onNext(Flowable.just(1));

        BackpressureHelper.add(ms.requested, 2);

        ms.onNext(Flowable.just(2));

        ms.drain();

        ts.assertValues(1, 2);
    }

    @Test
    public void array() {
        for (int i = 1; i < 100; i++) {

            @SuppressWarnings("unchecked")
            Flowable<Integer>[] sources = new Flowable[i];
            Arrays.fill(sources, Flowable.just(1));
            Integer[] expected = new Integer[i];
            for (int j = 0; j < i; j++) {
                expected[j] = 1;
            }

            Flowable.mergeArray(sources)
            .test()
            .assertResult(expected);
        }
    }

    @Test
    public void mergeArray2() {
        Flowable.mergeArray(Flowable.just(1), Flowable.just(2))
        .test()
        .assertResult(1, 2);
    }

    @Test
    public void mergeErrors() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            Flowable<Integer> source1 = Flowable.error(new TestException("First"));
            Flowable<Integer> source2 = Flowable.error(new TestException("Second"));

            Flowable.merge(source1, source2)
            .to(TestHelper.<Integer>testConsumer())
            .assertFailureAndMessage(TestException.class, "First");

            assertTrue(errors.toString(), errors.isEmpty());
        } finally {
            RxJavaPlugins.reset();
        }
    }
}
