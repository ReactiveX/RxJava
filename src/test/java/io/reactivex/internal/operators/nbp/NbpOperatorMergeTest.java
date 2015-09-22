/**
 * Copyright 2015 Netflix, Inc.
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

package io.reactivex.internal.operators.nbp;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;

import org.junit.*;

import io.reactivex.*;
import io.reactivex.NbpObservable.*;
import io.reactivex.Observable;
import io.reactivex.Scheduler.Worker;
import io.reactivex.disposables.*;
import io.reactivex.internal.disposables.EmptyDisposable;
import io.reactivex.schedulers.*;
import io.reactivex.subscribers.nbp.NbpTestSubscriber;

public class NbpOperatorMergeTest {

    NbpSubscriber<String> stringObserver;

    int count;
    
    @Before
    public void before() {
        stringObserver = TestHelper.mockNbpSubscriber();
        
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
        final NbpObservable<String> o1 = NbpObservable.create(new TestSynchronousObservable());
        final NbpObservable<String> o2 = NbpObservable.create(new TestSynchronousObservable());

        NbpObservable<NbpObservable<String>> observableOfObservables = NbpObservable.create(new NbpOnSubscribe<NbpObservable<String>>() {

            @Override
            public void accept(NbpSubscriber<? super NbpObservable<String>> NbpObserver) {
                NbpObserver.onSubscribe(EmptyDisposable.INSTANCE);
                // simulate what would happen in an NbpObservable
                NbpObserver.onNext(o1);
                NbpObserver.onNext(o2);
                NbpObserver.onComplete();
            }

        });
        NbpObservable<String> m = NbpObservable.merge(observableOfObservables);
        m.subscribe(stringObserver);

        verify(stringObserver, never()).onError(any(Throwable.class));
        verify(stringObserver, times(1)).onComplete();
        verify(stringObserver, times(2)).onNext("hello");
    }

    @Test
    public void testMergeArray() {
        final NbpObservable<String> o1 = NbpObservable.create(new TestSynchronousObservable());
        final NbpObservable<String> o2 = NbpObservable.create(new TestSynchronousObservable());

        NbpObservable<String> m = NbpObservable.merge(o1, o2);
        m.subscribe(stringObserver);

        verify(stringObserver, never()).onError(any(Throwable.class));
        verify(stringObserver, times(2)).onNext("hello");
        verify(stringObserver, times(1)).onComplete();
    }

    @Test
    public void testMergeList() {
        final NbpObservable<String> o1 = NbpObservable.create(new TestSynchronousObservable());
        final NbpObservable<String> o2 = NbpObservable.create(new TestSynchronousObservable());
        List<NbpObservable<String>> listOfObservables = new ArrayList<>();
        listOfObservables.add(o1);
        listOfObservables.add(o2);

        NbpObservable<String> m = NbpObservable.merge(listOfObservables);
        m.subscribe(stringObserver);

        verify(stringObserver, never()).onError(any(Throwable.class));
        verify(stringObserver, times(1)).onComplete();
        verify(stringObserver, times(2)).onNext("hello");
    }

    @Test(timeout = 1000)
    public void testUnSubscribeObservableOfObservables() throws InterruptedException {

        final AtomicBoolean unsubscribed = new AtomicBoolean();
        final CountDownLatch latch = new CountDownLatch(1);

        NbpObservable<NbpObservable<Long>> source = NbpObservable.create(new NbpOnSubscribe<NbpObservable<Long>>() {

            @Override
            public void accept(final NbpSubscriber<? super NbpObservable<Long>> NbpObserver) {
                // verbose on purpose so I can track the inside of it
                final Disposable s = () -> {
                    System.out.println("*** unsubscribed");
                    unsubscribed.set(true);
                };
                NbpObserver.onSubscribe(s);

                new Thread(new Runnable() {

                    @Override
                    public void run() {

                        while (!unsubscribed.get()) {
                            NbpObserver.onNext(NbpObservable.just(1L, 2L));
                        }
                        System.out.println("Done looping after unsubscribe: " + unsubscribed.get());
                        NbpObserver.onComplete();

                        // mark that the thread is finished
                        latch.countDown();
                    }
                }).start();
            }

        });

        final AtomicInteger count = new AtomicInteger();
        NbpObservable.merge(source).take(6).toBlocking().forEach(new Consumer<Long>() {

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

        NbpObservable<String> m = NbpObservable.merge(NbpObservable.create(o1), NbpObservable.create(o2));
        NbpTestSubscriber<String> ts = new NbpTestSubscriber<>(stringObserver);
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

        NbpObservable<String> m = NbpObservable.merge(NbpObservable.create(o1), NbpObservable.create(o2));
        m.subscribe(new NbpObserver<String>() {

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
        final NbpObservable<String> o1 = NbpObservable.create(new TestErrorObservable("four", null, "six")); // we expect to lose "six"
        final NbpObservable<String> o2 = NbpObservable.create(new TestErrorObservable("one", "two", "three")); // we expect to lose all of these since o1 is done first and fails

        NbpObservable<String> m = NbpObservable.merge(o1, o2);
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
        final NbpObservable<String> o1 = NbpObservable.create(new TestErrorObservable("one", "two", "three"));
        final NbpObservable<String> o2 = NbpObservable.create(new TestErrorObservable("four", null, "six")); // we expect to lose "six"
        final NbpObservable<String> o3 = NbpObservable.create(new TestErrorObservable("seven", "eight", null));// we expect to lose all of these since o2 is done first and fails
        final NbpObservable<String> o4 = NbpObservable.create(new TestErrorObservable("nine"));// we expect to lose all of these since o2 is done first and fails

        NbpObservable<String> m = NbpObservable.merge(o1, o2, o3, o4);
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
        NbpTestSubscriber<String> ts = new NbpTestSubscriber<>();
        NbpObservable<String> o1 = NbpObservable.create(new NbpOnSubscribe<String>() {

            @Override
            public void accept(NbpSubscriber<? super String> s) {
                throw new RuntimeException("fail");
            }

        });

        NbpObservable.merge(o1, o1).subscribe(ts);
        ts.awaitTerminalEvent(1000, TimeUnit.MILLISECONDS);
        ts.assertTerminated();
        System.out.println("Error: " + ts.errors());
    }

    private static class TestSynchronousObservable implements NbpOnSubscribe<String> {

        @Override
        public void accept(NbpSubscriber<? super String> NbpObserver) {
            NbpObserver.onSubscribe(EmptyDisposable.INSTANCE);
            NbpObserver.onNext("hello");
            NbpObserver.onComplete();
        }
    }

    private static class TestASynchronousObservable implements NbpOnSubscribe<String> {
        Thread t;
        final CountDownLatch onNextBeingSent = new CountDownLatch(1);

        @Override
        public void accept(final NbpSubscriber<? super String> NbpObserver) {
            NbpObserver.onSubscribe(EmptyDisposable.INSTANCE);
            t = new Thread(new Runnable() {

                @Override
                public void run() {
                    onNextBeingSent.countDown();
                    try {
                        NbpObserver.onNext("hello");
                        // I can't use a countDownLatch to prove we are actually sending 'onNext'
                        // since it will block if synchronized and I'll deadlock
                        NbpObserver.onComplete();
                    } catch (Exception e) {
                        NbpObserver.onError(e);
                    }
                }

            }, "TestASynchronousObservable");
            t.start();
        }
    }

    private static class TestErrorObservable implements NbpOnSubscribe<String> {

        String[] valuesToReturn;

        TestErrorObservable(String... values) {
            valuesToReturn = values;
        }

        @Override
        public void accept(NbpSubscriber<? super String> NbpObserver) {
            NbpObserver.onSubscribe(EmptyDisposable.INSTANCE);
            for (String s : valuesToReturn) {
                if (s == null) {
                    System.out.println("throwing exception");
                    NbpObserver.onError(new NullPointerException());
                } else {
                    NbpObserver.onNext(s);
                }
            }
            NbpObserver.onComplete();
        }
    }

    @Test
    public void testUnsubscribeAsObservablesComplete() {
        TestScheduler scheduler1 = Schedulers.test();
        AtomicBoolean os1 = new AtomicBoolean(false);
        NbpObservable<Long> o1 = createObservableOf5IntervalsOf1SecondIncrementsWithSubscriptionHook(scheduler1, os1);

        TestScheduler scheduler2 = Schedulers.test();
        AtomicBoolean os2 = new AtomicBoolean(false);
        NbpObservable<Long> o2 = createObservableOf5IntervalsOf1SecondIncrementsWithSubscriptionHook(scheduler2, os2);

        NbpTestSubscriber<Long> ts = new NbpTestSubscriber<>();
        NbpObservable.merge(o1, o2).subscribe(ts);

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
            NbpObservable<Long> o1 = createObservableOf5IntervalsOf1SecondIncrementsWithSubscriptionHook(scheduler1, os1);

            TestScheduler scheduler2 = Schedulers.test();
            AtomicBoolean os2 = new AtomicBoolean(false);
            NbpObservable<Long> o2 = createObservableOf5IntervalsOf1SecondIncrementsWithSubscriptionHook(scheduler2, os2);

            NbpTestSubscriber<Long> ts = new NbpTestSubscriber<>();
            NbpObservable.merge(o1, o2).subscribe(ts);

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

    private NbpObservable<Long> createObservableOf5IntervalsOf1SecondIncrementsWithSubscriptionHook(final Scheduler scheduler, final AtomicBoolean unsubscribed) {
        return NbpObservable.create(new NbpOnSubscribe<Long>() {

            @Override
            public void accept(NbpSubscriber<? super Long> child) {
                NbpObservable.interval(1, TimeUnit.SECONDS, scheduler)
                .take(5)
                .subscribe(new NbpSubscriber<Long>() {
                    @Override
                    public void onSubscribe(Disposable s) {
                        child.onSubscribe(() -> {
                            unsubscribed.set(true);
                            s.dispose();
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
        NbpObservable<Integer> o = NbpObservable.range(1, 10000).subscribeOn(Schedulers.newThread());

        for (int i = 0; i < 10; i++) {
            NbpObservable<Integer> merge = NbpObservable.merge(o, o, o);
            NbpTestSubscriber<Integer> ts = new NbpTestSubscriber<>();
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

        NbpObservable<Integer> o = NbpObservable.create(new NbpOnSubscribe<Integer>() {

            @Override
            public void accept(final NbpSubscriber<? super Integer> s) {
                Worker inner = Schedulers.newThread().createWorker();
                CompositeDisposable as = new CompositeDisposable();
                as.add(EmptyDisposable.INSTANCE);
                as.add(inner);
                
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
            NbpObservable<Integer> merge = NbpObservable.merge(o, o, o);
            NbpTestSubscriber<Integer> ts = new NbpTestSubscriber<>();
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
        NbpObservable<Integer> o = NbpObservable.create(new NbpOnSubscribe<Integer>() {

            @Override
            public void accept(final NbpSubscriber<? super Integer> s) {
                Worker inner = Schedulers.newThread().createWorker();
                CompositeDisposable as = new CompositeDisposable();
                as.add(EmptyDisposable.INSTANCE);
                as.add(inner);
                
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
            NbpObservable<Integer> merge = NbpObservable.merge(o, o, o);
            NbpTestSubscriber<Integer> ts = new NbpTestSubscriber<>();
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
        NbpObservable<Integer> o1 = createInfiniteObservable(generated1).subscribeOn(Schedulers.computation());
        final AtomicInteger generated2 = new AtomicInteger();
        NbpObservable<Integer> o2 = createInfiniteObservable(generated2).subscribeOn(Schedulers.computation());

        NbpTestSubscriber<Integer> NbpTestSubscriber = new NbpTestSubscriber<Integer>() {
            @Override
            public void onNext(Integer t) {
                System.err.println("NbpTestSubscriber received => " + t + "  on thread " + Thread.currentThread());
                super.onNext(t);
            }
        };

        NbpObservable.merge(o1.take(Observable.bufferSize() * 2), o2.take(Observable.bufferSize() * 2)).subscribe(NbpTestSubscriber);
        NbpTestSubscriber.awaitTerminalEvent();
        if (NbpTestSubscriber.errors().size() > 0) {
            NbpTestSubscriber.errors().get(0).printStackTrace();
        }
        NbpTestSubscriber.assertNoErrors();
        System.err.println(NbpTestSubscriber.values());
        assertEquals(Observable.bufferSize() * 4, NbpTestSubscriber.values().size());
        // it should be between the take num and requested batch size across the async boundary
        System.out.println("Generated 1: " + generated1.get());
        System.out.println("Generated 2: " + generated2.get());
        assertTrue(generated1.get() >= Observable.bufferSize() * 2 
                && generated1.get() <= Observable.bufferSize() * 4);
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
        NbpObservable<Integer> o1 = createInfiniteObservable(generated1).subscribeOn(Schedulers.computation());

        NbpTestSubscriber<Integer> NbpTestSubscriber = new NbpTestSubscriber<Integer>() {
            @Override
            public void onNext(Integer t) {
                super.onNext(t);
            }
        };

        NbpObservable.merge(o1.take(Observable.bufferSize() * 2), NbpObservable.just(-99)).subscribe(NbpTestSubscriber);
        NbpTestSubscriber.awaitTerminalEvent();
        
        List<Integer> onNextEvents = NbpTestSubscriber.values();
        
        System.out.println("Generated 1: " + generated1.get() + " / received: " + onNextEvents.size());
        System.out.println(onNextEvents);

        if (NbpTestSubscriber.errors().size() > 0) {
            NbpTestSubscriber.errors().get(0).printStackTrace();
        }
        NbpTestSubscriber.assertNoErrors();
        assertEquals(Observable.bufferSize() * 2 + 1, onNextEvents.size());
        // it should be between the take num and requested batch size across the async boundary
        assertTrue(generated1.get() >= Observable.bufferSize() * 2 && generated1.get() <= Observable.bufferSize() * 3);
    }

    /**
     * This is the same as the upstreams ones, but now adds the downstream as well by using observeOn.
     * 
     * This requires merge to also obey the Product.request values coming from it's child NbpSubscriber.
     */
    @Test(timeout = 10000)
    public void testBackpressureDownstreamWithConcurrentStreams() throws InterruptedException {
        final AtomicInteger generated1 = new AtomicInteger();
        NbpObservable<Integer> o1 = createInfiniteObservable(generated1).subscribeOn(Schedulers.computation());
        final AtomicInteger generated2 = new AtomicInteger();
        NbpObservable<Integer> o2 = createInfiniteObservable(generated2).subscribeOn(Schedulers.computation());

        NbpTestSubscriber<Integer> NbpTestSubscriber = new NbpTestSubscriber<Integer>() {
            @Override
            public void onNext(Integer t) {
                if (t < 100)
                    try {
                        // force a slow consumer
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                //                System.err.println("NbpTestSubscriber received => " + t + "  on thread " + Thread.currentThread());
                super.onNext(t);
            }
        };

        NbpObservable.merge(o1.take(Observable.bufferSize() * 2), o2.take(Observable.bufferSize() * 2)).observeOn(Schedulers.computation()).subscribe(NbpTestSubscriber);
        NbpTestSubscriber.awaitTerminalEvent();
        if (NbpTestSubscriber.errors().size() > 0) {
            NbpTestSubscriber.errors().get(0).printStackTrace();
        }
        NbpTestSubscriber.assertNoErrors();
        System.err.println(NbpTestSubscriber.values());
        assertEquals(Observable.bufferSize() * 4, NbpTestSubscriber.values().size());
        // it should be between the take num and requested batch size across the async boundary
        System.out.println("Generated 1: " + generated1.get());
        System.out.println("Generated 2: " + generated2.get());
        assertTrue(generated1.get() >= Observable.bufferSize() * 2 && generated1.get() <= Observable.bufferSize() * 4);
    }

    /**
     * Currently there is no solution to this ... we can't exert backpressure on the outer NbpObservable if we
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
        NbpObservable<NbpObservable<Integer>> o1 = createInfiniteObservable(generated1).map(new Function<Integer, NbpObservable<Integer>>() {

            @Override
            public NbpObservable<Integer> apply(Integer t1) {
                return NbpObservable.just(1, 2, 3);
            }

        });

        NbpTestSubscriber<Integer> NbpTestSubscriber = new NbpTestSubscriber<Integer>() {
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
                //                System.err.println("NbpTestSubscriber received => " + t + "  on thread " + Thread.currentThread());
                super.onNext(t);
            }
        };

        NbpObservable.merge(o1).observeOn(Schedulers.computation()).take(Observable.bufferSize() * 2).subscribe(NbpTestSubscriber);
        NbpTestSubscriber.awaitTerminalEvent();
        if (NbpTestSubscriber.errors().size() > 0) {
            NbpTestSubscriber.errors().get(0).printStackTrace();
        }
        NbpTestSubscriber.assertNoErrors();
        System.out.println("Generated 1: " + generated1.get());
        System.err.println(NbpTestSubscriber.values());
        System.out.println("done1 testBackpressureBothUpstreamAndDownstreamWithRegularObservables ");
        assertEquals(Observable.bufferSize() * 2, NbpTestSubscriber.values().size());
        System.out.println("done2 testBackpressureBothUpstreamAndDownstreamWithRegularObservables ");
        // we can't restrict this ... see comment above
        //        assertTrue(generated1.get() >= Observable.bufferSize() && generated1.get() <= Observable.bufferSize() * 4);
    }

    @Test
    @Ignore("Null values not permitted")
    public void mergeWithNullValues() {
        System.out.println("mergeWithNullValues");
        NbpTestSubscriber<String> ts = new NbpTestSubscriber<>();
        NbpObservable.merge(NbpObservable.just(null, "one"), NbpObservable.just("two", null)).subscribe(ts);
        ts.assertTerminated();
        ts.assertNoErrors();
        ts.assertValues(null, "one", "two", null);
    }

    @Test
    @Ignore("Null values are no longer permitted")
    public void mergeWithTerminalEventAfterUnsubscribe() {
        System.out.println("mergeWithTerminalEventAfterUnsubscribe");
        NbpTestSubscriber<String> ts = new NbpTestSubscriber<>();
        NbpObservable<String> bad = NbpObservable.create(new NbpOnSubscribe<String>() {

            @Override
            public void accept(NbpSubscriber<? super String> s) {
                s.onNext("two");
                // FIXME can't cancel downstream
//                s.unsubscribe();
//                s.onComplete();
            }

        });
        NbpObservable.merge(NbpObservable.just(null, "one"), bad).subscribe(ts);
        ts.assertNoErrors();
        ts.assertValues(null, "one", "two");
    }

    @Test
    @Ignore("Null values are not permitted")
    public void mergingNullObservable() {
        NbpTestSubscriber<String> ts = new NbpTestSubscriber<>();
        NbpObservable.merge(NbpObservable.just("one"), null).subscribe(ts);
        ts.assertNoErrors();
        ts.assertValue("one");
    }

    @Test
    public void merge1AsyncStreamOf1() {
        NbpTestSubscriber<Integer> ts = new NbpTestSubscriber<>();
        mergeNAsyncStreamsOfN(1, 1).subscribe(ts);
        ts.awaitTerminalEvent();
        ts.assertNoErrors();
        assertEquals(1, ts.values().size());
    }

    @Test
    public void merge1AsyncStreamOf1000() {
        NbpTestSubscriber<Integer> ts = new NbpTestSubscriber<>();
        mergeNAsyncStreamsOfN(1, 1000).subscribe(ts);
        ts.awaitTerminalEvent();
        ts.assertNoErrors();
        assertEquals(1000, ts.values().size());
    }

    @Test
    public void merge10AsyncStreamOf1000() {
        NbpTestSubscriber<Integer> ts = new NbpTestSubscriber<>();
        mergeNAsyncStreamsOfN(10, 1000).subscribe(ts);
        ts.awaitTerminalEvent();
        ts.assertNoErrors();
        assertEquals(10000, ts.values().size());
    }

    @Test
    public void merge1000AsyncStreamOf1000() {
        NbpTestSubscriber<Integer> ts = new NbpTestSubscriber<>();
        mergeNAsyncStreamsOfN(1000, 1000).subscribe(ts);
        ts.awaitTerminalEvent();
        ts.assertNoErrors();
        assertEquals(1000000, ts.values().size());
    }

    @Test
    public void merge2000AsyncStreamOf100() {
        NbpTestSubscriber<Integer> ts = new NbpTestSubscriber<>();
        mergeNAsyncStreamsOfN(2000, 100).subscribe(ts);
        ts.awaitTerminalEvent();
        ts.assertNoErrors();
        assertEquals(200000, ts.values().size());
    }

    @Test
    public void merge100AsyncStreamOf1() {
        NbpTestSubscriber<Integer> ts = new NbpTestSubscriber<>();
        mergeNAsyncStreamsOfN(100, 1).subscribe(ts);
        ts.awaitTerminalEvent();
        ts.assertNoErrors();
        assertEquals(100, ts.values().size());
    }

    private NbpObservable<Integer> mergeNAsyncStreamsOfN(final int outerSize, final int innerSize) {
        NbpObservable<NbpObservable<Integer>> os = NbpObservable.range(1, outerSize)
        .map(new Function<Integer, NbpObservable<Integer>>() {

            @Override
            public NbpObservable<Integer> apply(Integer i) {
                return NbpObservable.range(1, innerSize).subscribeOn(Schedulers.computation());
            }

        });
        return NbpObservable.merge(os);
    }

    @Test
    public void merge1SyncStreamOf1() {
        NbpTestSubscriber<Integer> ts = new NbpTestSubscriber<>();
        mergeNSyncStreamsOfN(1, 1).subscribe(ts);
        ts.awaitTerminalEvent();
        ts.assertNoErrors();
        assertEquals(1, ts.values().size());
    }

    @Test
    public void merge1SyncStreamOf1000000() {
        NbpTestSubscriber<Integer> ts = new NbpTestSubscriber<>();
        mergeNSyncStreamsOfN(1, 1000000).subscribe(ts);
        ts.awaitTerminalEvent();
        ts.assertNoErrors();
        assertEquals(1000000, ts.values().size());
    }

    @Test
    public void merge1000SyncStreamOf1000() {
        NbpTestSubscriber<Integer> ts = new NbpTestSubscriber<>();
        mergeNSyncStreamsOfN(1000, 1000).subscribe(ts);
        ts.awaitTerminalEvent();
        ts.assertNoErrors();
        assertEquals(1000000, ts.values().size());
    }

    @Test
    public void merge10000SyncStreamOf10() {
        NbpTestSubscriber<Integer> ts = new NbpTestSubscriber<>();
        mergeNSyncStreamsOfN(10000, 10).subscribe(ts);
        ts.awaitTerminalEvent();
        ts.assertNoErrors();
        assertEquals(100000, ts.values().size());
    }

    @Test
    public void merge1000000SyncStreamOf1() {
        NbpTestSubscriber<Integer> ts = new NbpTestSubscriber<>();
        mergeNSyncStreamsOfN(1000000, 1).subscribe(ts);
        ts.awaitTerminalEvent();
        ts.assertNoErrors();
        assertEquals(1000000, ts.values().size());
    }

    private NbpObservable<Integer> mergeNSyncStreamsOfN(final int outerSize, final int innerSize) {
        NbpObservable<NbpObservable<Integer>> os = NbpObservable.range(1, outerSize)
        .map(new Function<Integer, NbpObservable<Integer>>() {

            @Override
            public NbpObservable<Integer> apply(Integer i) {
                return NbpObservable.range(1, innerSize);
            }

        });
        return NbpObservable.merge(os);
    }

    private NbpObservable<Integer> createInfiniteObservable(final AtomicInteger generated) {
        NbpObservable<Integer> o = NbpObservable.fromIterable(new Iterable<Integer>() {
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
        return o;
    }

    @Test
    public void mergeManyAsyncSingle() {
        NbpTestSubscriber<Integer> ts = new NbpTestSubscriber<>();
        NbpObservable<NbpObservable<Integer>> os = NbpObservable.range(1, 10000)
        .map(new Function<Integer, NbpObservable<Integer>>() {

            @Override
            public NbpObservable<Integer> apply(final Integer i) {
                return NbpObservable.create(new NbpOnSubscribe<Integer>() {

                    @Override
                    public void accept(NbpSubscriber<? super Integer> s) {
                        s.onSubscribe(EmptyDisposable.INSTANCE);
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
        NbpObservable.merge(os).subscribe(ts);
        ts.awaitTerminalEvent();
        ts.assertNoErrors();
        assertEquals(10000, ts.values().size());
    }

    Function<Integer, NbpObservable<Integer>> toScalar = NbpObservable::just;
    
    Function<Integer, NbpObservable<Integer>> toHiddenScalar = t ->
            NbpObservable.just(t).asObservable();
    ;
    
    void runMerge(Function<Integer, NbpObservable<Integer>> func, NbpTestSubscriber<Integer> ts) {
        List<Integer> list = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            list.add(i);
        }
        NbpObservable<Integer> source = NbpObservable.fromIterable(list);
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
        runMerge(toScalar, new NbpTestSubscriber<Integer>());
    }
    @Test
    public void testFastMergeHiddenScalar() {
        runMerge(toHiddenScalar, new NbpTestSubscriber<Integer>());
    }
    @Test
    public void testSlowMergeFullScalar() {
        for (final int req : new int[] { 16, 32, 64, 128, 256 }) {
            NbpTestSubscriber<Integer> ts = new NbpTestSubscriber<Integer>() {
                int remaining = req;

                @Override
                public void onNext(Integer t) {
                    super.onNext(t);
                    if (--remaining == 0) {
                        remaining = req;
                    }
                }
            };
            runMerge(toScalar, ts);
        }
    }
    @Test
    public void testSlowMergeHiddenScalar() {
        for (final int req : new int[] { 16, 32, 64, 128, 256 }) {
            NbpTestSubscriber<Integer> ts = new NbpTestSubscriber<Integer>() {
                int remaining = req;
                @Override
                public void onNext(Integer t) {
                    super.onNext(t);
                    if (--remaining == 0) {
                        remaining = req;
                    }
                }
            };
            runMerge(toHiddenScalar, ts);
        }
    }
}