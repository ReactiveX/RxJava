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
package rx;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Test;

import rx.Observable.OnSubscribe;
import rx.exceptions.MissingBackpressureException;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.internal.util.RxRingBuffer;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;

public class BackpressureTests {

    @Test
    public void testObserveOn() {
        int NUM = (int) (RxRingBuffer.SIZE * 2.1);
        AtomicInteger c = new AtomicInteger();
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        incrementingIntegers(c).observeOn(Schedulers.computation()).take(NUM).subscribe(ts);
        ts.awaitTerminalEvent();
        ts.assertNoErrors();
        System.out.println("testObserveOn => Received: " + ts.getOnNextEvents().size() + "  Emitted: " + c.get());
        assertEquals(NUM, ts.getOnNextEvents().size());
        assertTrue(c.get() < RxRingBuffer.SIZE * 4);
    }

    @Test
    public void testObserveOnWithSlowConsumer() {
        int NUM = (int) (RxRingBuffer.SIZE * 0.2);
        AtomicInteger c = new AtomicInteger();
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        incrementingIntegers(c).observeOn(Schedulers.computation()).map(new Func1<Integer, Integer>() {

            @Override
            public Integer call(Integer i) {
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return i;
            }

        }).take(NUM).subscribe(ts);
        ts.awaitTerminalEvent();
        ts.assertNoErrors();
        System.out.println("testObserveOnWithSlowConsumer => Received: " + ts.getOnNextEvents().size() + "  Emitted: " + c.get());
        assertEquals(NUM, ts.getOnNextEvents().size());
        assertTrue(c.get() < RxRingBuffer.SIZE * 2);
    }

    @Test
    public void testMergeSync() {
        int NUM = (int) (RxRingBuffer.SIZE * 4.1);
        AtomicInteger c1 = new AtomicInteger();
        AtomicInteger c2 = new AtomicInteger();
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        Observable<Integer> merged = Observable.merge(incrementingIntegers(c1), incrementingIntegers(c2));

        merged.take(NUM).subscribe(ts);
        ts.awaitTerminalEvent();
        ts.assertNoErrors();
        System.out.println("Expected: " + NUM + " got: " + ts.getOnNextEvents().size());
        System.out.println("testMergeSync => Received: " + ts.getOnNextEvents().size() + "  Emitted: " + c1.get() + " / " + c2.get());
        assertEquals(NUM, ts.getOnNextEvents().size());
        // either one can starve the other, but neither should be capable of doing more than 5 batches (taking 4.1)
        // TODO is it possible to make this deterministic rather than one possibly starving the other?
        // benjchristensen => In general I'd say it's not worth trying to make it so, as "fair" algoritms generally take a performance hit
        assertTrue(c1.get() < RxRingBuffer.SIZE * 5);
        assertTrue(c2.get() < RxRingBuffer.SIZE * 5);
    }

    @Test
    public void testMergeAsync() {
        int NUM = (int) (RxRingBuffer.SIZE * 4.1);
        AtomicInteger c1 = new AtomicInteger();
        AtomicInteger c2 = new AtomicInteger();
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        Observable<Integer> merged = Observable.merge(
                incrementingIntegers(c1).subscribeOn(Schedulers.computation()),
                incrementingIntegers(c2).subscribeOn(Schedulers.computation()));

        merged.take(NUM).subscribe(ts);
        ts.awaitTerminalEvent();
        ts.assertNoErrors();
        System.out.println("testMergeAsync => Received: " + ts.getOnNextEvents().size() + "  Emitted: " + c1.get() + " / " + c2.get());
        assertEquals(NUM, ts.getOnNextEvents().size());
        // either one can starve the other, but neither should be capable of doing more than 5 batches (taking 4.1)
        // TODO is it possible to make this deterministic rather than one possibly starving the other?
        // benjchristensen => In general I'd say it's not worth trying to make it so, as "fair" algoritms generally take a performance hit
        assertTrue(c1.get() < RxRingBuffer.SIZE * 5);
        assertTrue(c2.get() < RxRingBuffer.SIZE * 5);
    }

    @Test
    public void testMergeAsyncThenObserveOn() {
        int NUM = (int) (RxRingBuffer.SIZE * 4.1);
        AtomicInteger c1 = new AtomicInteger();
        AtomicInteger c2 = new AtomicInteger();
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        Observable<Integer> merged = Observable.merge(
                incrementingIntegers(c1).subscribeOn(Schedulers.computation()),
                incrementingIntegers(c2).subscribeOn(Schedulers.computation()));

        merged.observeOn(Schedulers.newThread()).take(NUM).subscribe(ts);
        ts.awaitTerminalEvent();
        ts.assertNoErrors();
        System.out.println("testMergeAsyncThenObserveOn => Received: " + ts.getOnNextEvents().size() + "  Emitted: " + c1.get() + " / " + c2.get());
        assertEquals(NUM, ts.getOnNextEvents().size());
        // either one can starve the other, but neither should be capable of doing more than 5 batches (taking 4.1)
        // TODO is it possible to make this deterministic rather than one possibly starving the other?
        // benjchristensen => In general I'd say it's not worth trying to make it so, as "fair" algoritms generally take a performance hit
        assertTrue(c1.get() < RxRingBuffer.SIZE * 5);
        assertTrue(c2.get() < RxRingBuffer.SIZE * 5);
    }

    @Test
    public void testFlatMapSync() {
        int NUM = (int) (RxRingBuffer.SIZE * 2.1);
        AtomicInteger c = new AtomicInteger();
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        incrementingIntegers(c).flatMap(new Func1<Integer, Observable<Integer>>() {

            @Override
            public Observable<Integer> call(Integer i) {
                return incrementingIntegers(new AtomicInteger()).take(10);
            }

        }).take(NUM).subscribe(ts);
        ts.awaitTerminalEvent();
        ts.assertNoErrors();
        System.out.println("testFlatMapSync => Received: " + ts.getOnNextEvents().size() + "  Emitted: " + c.get());
        assertEquals(NUM, ts.getOnNextEvents().size());
        // expect less than 1 buffer since the flatMap is emitting 10 each time, so it is NUM/10 that will be taken.
        assertTrue(c.get() < RxRingBuffer.SIZE);
    }

    @Test
    public void testFlatMapAsync() {
        int NUM = (int) (RxRingBuffer.SIZE * 2.1);
        AtomicInteger c = new AtomicInteger();
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        incrementingIntegers(c).subscribeOn(Schedulers.computation()).flatMap(new Func1<Integer, Observable<Integer>>() {

            @Override
            public Observable<Integer> call(Integer i) {
                return incrementingIntegers(new AtomicInteger()).take(10).subscribeOn(Schedulers.computation());
            }

        }).take(NUM).subscribe(ts);
        ts.awaitTerminalEvent();
        ts.assertNoErrors();
        System.out.println("testFlatMapAsync => Received: " + ts.getOnNextEvents().size() + "  Emitted: " + c.get() + " Size: " + RxRingBuffer.SIZE);
        assertEquals(NUM, ts.getOnNextEvents().size());
        // even though we only need 10, it will request at least RxRingBuffer.SIZE, and then as it drains keep requesting more
        // and then it will be non-deterministic when the take() causes the unsubscribe as it is scheduled on 10 different schedulers (threads)
        // normally this number is ~250 but can get up to ~1200 when RxRingBuffer.SIZE == 1024
        assertTrue(c.get() <= RxRingBuffer.SIZE * 2);
    }

    @Test
    public void testZipSync() {
        int NUM = (int) (RxRingBuffer.SIZE * 4.1);
        AtomicInteger c1 = new AtomicInteger();
        AtomicInteger c2 = new AtomicInteger();
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        Observable<Integer> zipped = Observable.zip(
                incrementingIntegers(c1),
                incrementingIntegers(c2),
                new Func2<Integer, Integer, Integer>() {

                    @Override
                    public Integer call(Integer t1, Integer t2) {
                        return t1 + t2;
                    }

                });

        zipped.take(NUM).subscribe(ts);
        ts.awaitTerminalEvent();
        ts.assertNoErrors();
        System.out.println("testZipSync => Received: " + ts.getOnNextEvents().size() + "  Emitted: " + c1.get() + " / " + c2.get());
        assertEquals(NUM, ts.getOnNextEvents().size());
        assertTrue(c1.get() < RxRingBuffer.SIZE * 5);
        assertTrue(c2.get() < RxRingBuffer.SIZE * 5);
    }

    @Test
    public void testZipAsync() {
        int NUM = (int) (RxRingBuffer.SIZE * 2.1);
        AtomicInteger c1 = new AtomicInteger();
        AtomicInteger c2 = new AtomicInteger();
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        Observable<Integer> zipped = Observable.zip(
                incrementingIntegers(c1).subscribeOn(Schedulers.computation()),
                incrementingIntegers(c2).subscribeOn(Schedulers.computation()),
                new Func2<Integer, Integer, Integer>() {

                    @Override
                    public Integer call(Integer t1, Integer t2) {
                        return t1 + t2;
                    }

                });

        zipped.take(NUM).subscribe(ts);
        ts.awaitTerminalEvent();
        ts.assertNoErrors();
        System.out.println("testZipAsync => Received: " + ts.getOnNextEvents().size() + "  Emitted: " + c1.get() + " / " + c2.get());
        assertEquals(NUM, ts.getOnNextEvents().size());
        assertTrue(c1.get() < RxRingBuffer.SIZE * 3);
        assertTrue(c2.get() < RxRingBuffer.SIZE * 3);
    }

    @Test
    public void testSubscribeOnScheduling() {
        // in a loop for repeating the concurrency in this to increase chance of failure
        for (int i = 0; i < 100; i++) {
            int NUM = (int) (RxRingBuffer.SIZE * 2.1);
            AtomicInteger c = new AtomicInteger();
            ConcurrentLinkedQueue<Thread> threads = new ConcurrentLinkedQueue<Thread>();
            TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
            // observeOn is there to make it async and need backpressure
            incrementingIntegers(c, threads).subscribeOn(Schedulers.computation()).observeOn(Schedulers.computation()).take(NUM).subscribe(ts);
            ts.awaitTerminalEvent();
            ts.assertNoErrors();
            System.out.println("testSubscribeOnScheduling => Received: " + ts.getOnNextEvents().size() + "  Emitted: " + c.get());
            assertEquals(NUM, ts.getOnNextEvents().size());
            assertTrue(c.get() < RxRingBuffer.SIZE * 4);
            Thread first = null;
            for (Thread t : threads) {
                System.out.println("testSubscribeOnScheduling => thread: " + t);
                if (first == null) {
                    first = t;
                } else {
                    if (!first.equals(t)) {
                        fail("Expected to see the same thread");
                    }
                }
            }
            System.out.println("testSubscribeOnScheduling => Number of batch requests seen: " + threads.size());
            assertTrue(threads.size() > 1);
            System.out.println("-------------------------------------------------------------------------------------------");
        }
    }

    @Test
    public void testTakeFilterSkipChainAsync() {
        int NUM = (int) (RxRingBuffer.SIZE * 2.1);
        AtomicInteger c = new AtomicInteger();
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        incrementingIntegers(c).observeOn(Schedulers.computation())
                .skip(10000)
                .filter(new Func1<Integer, Boolean>() {

                    @Override
                    public Boolean call(Integer i) {
                        return i > 11000;
                    }

                }).take(NUM).subscribe(ts);

        ts.awaitTerminalEvent();
        ts.assertNoErrors();

        // emit 10000 that are skipped
        // emit next 1000 that are filtered out
        // take NUM
        // so emitted is at least 10000+1000+NUM + extra for buffer size/threshold
        int expected = 10000 + 1000 + RxRingBuffer.SIZE * 3 + RxRingBuffer.SIZE / 2;

        System.out.println("testTakeFilterSkipChain => Received: " + ts.getOnNextEvents().size() + "  Emitted: " + c.get() + " Expected: " + expected);
        assertEquals(NUM, ts.getOnNextEvents().size());
        assertTrue(c.get() < expected);
    }

    @Test
    public void testUserSubscriberUsingRequestSync() {
        AtomicInteger c = new AtomicInteger();
        final AtomicInteger totalReceived = new AtomicInteger();
        final AtomicInteger batches = new AtomicInteger();
        final AtomicInteger received = new AtomicInteger();
        incrementingIntegers(c).subscribe(new Subscriber<Integer>() {

            @Override
            public void onStart() {
                request(100);
            }

            @Override
            public void onCompleted() {

            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onNext(Integer t) {
                int total = totalReceived.incrementAndGet();
                received.incrementAndGet();
                if (total >= 2000) {
                    unsubscribe();
                }
                if (received.get() == 100) {
                    batches.incrementAndGet();
                    request(100);
                    received.set(0);
                }
            }

        });

        System.out.println("testUserSubscriberUsingRequestSync => Received: " + totalReceived.get() + "  Emitted: " + c.get() + " Request Batches: " + batches.get());
        assertEquals(2000, c.get());
        assertEquals(2000, totalReceived.get());
        assertEquals(20, batches.get());
    }

    @Test
    public void testUserSubscriberUsingRequestAsync() throws InterruptedException {
        AtomicInteger c = new AtomicInteger();
        final AtomicInteger totalReceived = new AtomicInteger();
        final AtomicInteger received = new AtomicInteger();
        final AtomicInteger batches = new AtomicInteger();
        final CountDownLatch latch = new CountDownLatch(1);
        incrementingIntegers(c).subscribeOn(Schedulers.newThread()).subscribe(new Subscriber<Integer>() {

            @Override
            public void onStart() {
                request(100);
            }

            @Override
            public void onCompleted() {
                latch.countDown();
            }

            @Override
            public void onError(Throwable e) {
                latch.countDown();
            }

            @Override
            public void onNext(Integer t) {
                int total = totalReceived.incrementAndGet();
                received.incrementAndGet();
                boolean done = false;
                if (total >= 2000) {
                    done = true;
                    unsubscribe();
                }
                if (received.get() == 100) {
                    batches.incrementAndGet();
                    received.set(0);
                    if (!done) {
                        request(100);
                    }
                }
                if (done) {
                    latch.countDown();
                }
            }

        });

        latch.await();
        System.out.println("testUserSubscriberUsingRequestAsync => Received: " + totalReceived.get() + "  Emitted: " + c.get() + " Request Batches: " + batches.get());
        assertEquals(2000, c.get());
        assertEquals(2000, totalReceived.get());
        assertEquals(20, batches.get());
    }

    @Test(timeout = 2000)
    public void testFirehoseFailsAsExpected() {
        AtomicInteger c = new AtomicInteger();
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        firehose(c).observeOn(Schedulers.computation()).map(SLOW_PASS_THRU).subscribe(ts);
        ts.awaitTerminalEvent();
        System.out.println("testFirehoseFailsAsExpected => Received: " + ts.getOnNextEvents().size() + "  Emitted: " + c.get());
        assertEquals(1, ts.getOnErrorEvents().size());
        assertTrue(ts.getOnErrorEvents().get(0) instanceof MissingBackpressureException);
    }

    @Test(timeout = 2000)
    public void testOnBackpressureDrop() {
        int NUM = (int) (RxRingBuffer.SIZE * 1.1); // > 1 so that take doesn't prevent buffer overflow
        AtomicInteger c = new AtomicInteger();
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        firehose(c).onBackpressureDrop().observeOn(Schedulers.computation()).map(SLOW_PASS_THRU).take(NUM).subscribe(ts);
        ts.awaitTerminalEvent();
        ts.assertNoErrors();
        System.out.println("testOnBackpressureDrop => Received: " + ts.getOnNextEvents().size() + "  Emitted: " + c.get() + " Last value: " + ts.getOnNextEvents().get(NUM - 1));
        assertEquals(NUM, ts.getOnNextEvents().size());
        // it drop, so we should get some number far higher than what would have sequentially incremented
        assertTrue(NUM < ts.getOnNextEvents().get(NUM - 1).intValue());
    }

    @Test(timeout = 2000)
    public void testOnBackpressureBuffer() {
        int NUM = (int) (RxRingBuffer.SIZE * 1.1); // > 1 so that take doesn't prevent buffer overflow
        AtomicInteger c = new AtomicInteger();
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        firehose(c).takeWhile(new Func1<Integer, Boolean>() {

            @Override
            public Boolean call(Integer t1) {
                return t1 < 100000;
            }

        }).onBackpressureBuffer().observeOn(Schedulers.computation()).map(SLOW_PASS_THRU).take(NUM).subscribe(ts);
        ts.awaitTerminalEvent();
        ts.assertNoErrors();
        System.out.println("testOnBackpressureBuffer => Received: " + ts.getOnNextEvents().size() + "  Emitted: " + c.get());
        assertEquals(NUM, ts.getOnNextEvents().size());
        // it buffers, so we should get the right value sequentially
        assertEquals(NUM - 1, ts.getOnNextEvents().get(NUM - 1).intValue());
    }

    /**
     * A synchronous Observable that will emit incrementing integers as requested.
     * 
     * @param counter
     * @return
     */
    private static Observable<Integer> incrementingIntegers(final AtomicInteger counter) {
        return incrementingIntegers(counter, null);
    }

    private static Observable<Integer> incrementingIntegers(final AtomicInteger counter, final ConcurrentLinkedQueue<Thread> threadsSeen) {
        return Observable.create(new OnSubscribe<Integer>() {

            final AtomicLong requested = new AtomicLong();

            @Override
            public void call(final Subscriber<? super Integer> s) {
                s.setProducer(new Producer() {
                    int i = 0;

                    @Override
                    public void request(long n) {
                        if (n == 0) {
                            // nothing to do
                            return;
                        }
                        if (threadsSeen != null) {
                            threadsSeen.offer(Thread.currentThread());
                        }
                        long _c = requested.getAndAdd(n);
                        if (_c == 0) {
                            while (!s.isUnsubscribed()) {
                                counter.incrementAndGet();
                                s.onNext(i++);
                                if (requested.decrementAndGet() == 0) {
                                    // we're done emitting the number requested so return
                                    return;
                                }
                            }
                        }
                    }

                });
            }

        });
    }

    /**
     * Incrementing int without backpressure.
     * 
     * @param counter
     * @return
     */
    private static Observable<Integer> firehose(final AtomicInteger counter) {
        return Observable.create(new OnSubscribe<Integer>() {

            int i = 0;

            @Override
            public void call(final Subscriber<? super Integer> s) {
                while (!s.isUnsubscribed()) {
                    s.onNext(i++);
                    counter.incrementAndGet();
                }
                System.out.println("unsubscribed after: " + i);
            }

        });
    }

    final static Func1<Integer, Integer> SLOW_PASS_THRU = new Func1<Integer, Integer>() {

        @Override
        public Integer call(Integer t1) {
            // be slow ... but faster than Thread.sleep(1)
            String t = "";
            for (int i = 0; i < 10000; i++) {
                t = String.valueOf(i);
            }
            return t1;
        }

    };
}
