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
package rx.internal.util;

import static org.junit.Assert.assertEquals;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import rx.*;
import rx.exceptions.MissingBackpressureException;
import rx.functions.Action0;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;

public class RxRingBufferWithoutUnsafeTest extends RxRingBufferBase {

    @Override
    protected RxRingBuffer createRingBuffer() {
        return new RxRingBuffer();
    }

    @Test(timeout = 20000)
    public void testConcurrencyLoop() throws InterruptedException {
        for (int i = 0; i < 50; i++) {
            testConcurrency();
        }
    }
    
    /**
     * Single producer, 2 consumers. The request() ensures it gets scheduled back on the same Producer thread.
     * @throws InterruptedException if the wait is interrupted
     */
    @Test(timeout = 10000)
    public void testConcurrency() throws InterruptedException {
        final RxRingBuffer b = createRingBuffer();
        final CountDownLatch emitLatch = new CountDownLatch(127);
        int drainers = 3;
        final CountDownLatch drainLatch = new CountDownLatch(drainers);

        final Scheduler.Worker w1 = Schedulers.newThread().createWorker();
        List<Scheduler.Worker> drainerWorkers = new ArrayList<Scheduler.Worker>();

        final AtomicInteger emit = new AtomicInteger();
        final AtomicInteger poll = new AtomicInteger();
        final AtomicInteger backpressureExceptions = new AtomicInteger();

        final Producer p = new Producer() {

//            AtomicInteger c = new AtomicInteger();

            @Override
            public void request(final long n) {
                //                System.out.println("request[" + c.incrementAndGet() + "]: " + n + "  Thread: " + Thread.currentThread());
                w1.schedule(new Action0() {

                    @Override
                    public void call() {
                        if (emitLatch.getCount() == 0) {
                            return;
                        }
                        for (int i = 0; i < n; i++) {
                            try {
                                b.onNext("one");
                                emit.incrementAndGet();
                            } catch (MissingBackpressureException e) {
                                System.out.println("BackpressureException => item: " + i + "  requested: " + n + " emit: " + emit.get() + "  poll: " + poll.get());
                                backpressureExceptions.incrementAndGet();
                            }
                        }
                        // we'll release after n batches
                        emitLatch.countDown();
                    }

                });
            }

        };
        final TestSubscriber<String> ts = new TestSubscriber<String>();
        w1.schedule(new Action0() {

            @Override
            public void call() {
                ts.requestMore(RxRingBuffer.SIZE);
                ts.setProducer(p);
            }

        });

        Action0 drainer = new Action0() {

            @Override
            public void call() {
                int emitted = 0;
                while (true) {
                    Object o = b.poll();
                    if (o != null) {
                        emitted++;
                        poll.incrementAndGet();
                    } else {
                        if (emitted > 0) {
                            ts.requestMore(emitted);
                            emitted = 0;
                        } else {
                            try {
                                Thread.sleep(1);
                            } catch (InterruptedException ex) {
                                // ignored
                            }
                            if (emitLatch.getCount() == 0 && b.isEmpty()) {
                                // this works with SynchronizedQueue, if changing to a non-blocking Queue
                                // then this will likely need to change like the SpmcTest version
                                drainLatch.countDown();
                                return;
                            }
                        }
                    }
                }

            }

        };

        for (int i = 0; i < drainers; i++) {
            Scheduler.Worker w = Schedulers.newThread().createWorker();
            w.schedule(drainer);
            drainerWorkers.add(w);
        }

        emitLatch.await();
        drainLatch.await();

        for (Scheduler.Worker w : drainerWorkers) {
            w.unsubscribe();
        }
        w1.unsubscribe(); // put this one last as unsubscribing from it can cause Exceptions to be throw in w2/w3

        System.out.println("emit: " + emit.get() + " poll: " + poll.get());
        assertEquals(0, backpressureExceptions.get());
        assertEquals(emit.get(), poll.get());
    }
}
