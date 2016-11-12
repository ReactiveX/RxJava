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

import com.google.j2objc.WeakProxy;
import com.google.j2objc.annotations.AutoreleasePool;
import com.google.j2objc.annotations.Weak;


import static org.junit.Assert.*;

import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.junit.*;

import org.junit.rules.TestName;

import co.touchlab.doppel.testing.DoppelHacks;
import rx.Observable.OnSubscribe;
import rx.exceptions.MissingBackpressureException;
import rx.functions.*;
import rx.internal.util.RxRingBuffer;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;
import rx.test.TestObstructionDetection;

@DoppelHacks//Lots of unsubscribe
public class BackpressureTests {

    @Rule
    public TestName testName = new TestName();

    @After
    public void doAfterTest() {
        TestObstructionDetection.checkObstruction();
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
                s.setProducer(new BPTPRoducer(counter, threadsSeen, requested, s));
            }

        });
    }

    static class BPTPRoducer implements Producer
    {
        final AtomicInteger counter;
        final ConcurrentLinkedQueue<Thread> threadsSeen;
        final AtomicLong requested;
//        @Weak
        final Subscriber<? super Integer> s;
        int i = 0;

        BPTPRoducer(AtomicInteger counter, ConcurrentLinkedQueue<Thread> threadsSeen, AtomicLong requested, Subscriber<? super Integer> s)
        {
            this.counter = counter;
            this.threadsSeen = threadsSeen;
            this.requested = requested;
            this.s = WeakProxy.forObject(s);
        }

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
        volatile int sink;
        @Override
        public Integer call(Integer t1) {
            // be slow ... but faster than Thread.sleep(1)
            String t = "";
            int s = sink;
            for (int i = 1000; i >= 0; i--) {
                t = String.valueOf(i + t.hashCode() + s);
            }
            sink = t.hashCode();
            return t1;
        }

    };
}
