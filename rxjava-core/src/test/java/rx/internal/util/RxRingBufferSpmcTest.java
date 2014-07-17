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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import rx.Producer;
import rx.Scheduler;
import rx.exceptions.MissingBackpressureException;
import rx.functions.Action0;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;

public class RxRingBufferSpmcTest extends RxRingBufferBase {

    @Override
    protected RxRingBuffer createRingBuffer() {
        return RxRingBuffer.getSpmcInstance();
    }

    /**
     * Single producer, 2 consumers. The request() ensures it gets scheduled back on the same Producer thread.
     */
    @Test(timeout = 2000)
    public void testConcurrency() throws InterruptedException {
        final RxRingBuffer b = createRingBuffer();
        final CountDownLatch latch = new CountDownLatch(255);

        final Scheduler.Worker w1 = Schedulers.newThread().createWorker();
        Scheduler.Worker w2 = Schedulers.newThread().createWorker();
        Scheduler.Worker w3 = Schedulers.newThread().createWorker();

        final AtomicInteger emit = new AtomicInteger();
        final AtomicInteger poll = new AtomicInteger();
        final AtomicInteger backpressureExceptions = new AtomicInteger();

        final Producer p = new Producer() {

            AtomicInteger c = new AtomicInteger();

            @Override
            public void request(final long n) {
                System.out.println("request[" + c.incrementAndGet() + "]: " + n + "  Thread: " + Thread.currentThread());
                w1.schedule(new Action0() {

                    @Override
                    public void call() {
                        if (latch.getCount() == 0) {
                            return;
                        }
                        for (int i = 0; i < n; i++) {
                            try {
                                emit.incrementAndGet();
                                b.onNext("one");
                            } catch (MissingBackpressureException e) {
                                System.out.println("BackpressureException => item: " + i + "  requested: " + n + " emit: " + emit.get() + "  poll: " + poll.get());
                                backpressureExceptions.incrementAndGet();
                            }
                        }
                        // we'll release after n batches
                        latch.countDown();
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

        w2.schedule(new Action0() {

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
                        }
                    }
                }

            }

        });

        w3.schedule(new Action0() {

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
                        }
                    }
                }
            }

        });

        latch.await();
        w1.unsubscribe();
        w2.unsubscribe();
        w3.unsubscribe();

        System.out.println("emit: " + emit.get() + " poll: " + poll.get());
        assertEquals(0, backpressureExceptions.get());
        assertEquals(emit.get(), poll.get());
    }
}
