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

package io.reactivex.schedulers;

import static org.junit.Assert.*;

import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.junit.Test;
import org.reactivestreams.*;

import io.reactivex.*;
import io.reactivex.Scheduler.Worker;
import io.reactivex.functions.*;
import io.reactivex.subscribers.*;

/**
 * Base tests for schedulers that involve threads (concurrency).
 *
 * These can only run on Schedulers that launch threads since they expect async/concurrent behavior.
 *
 * The Current/Immediate schedulers will not work with these tests.
 */
public abstract class AbstractSchedulerConcurrencyTests extends AbstractSchedulerTests {

    /**
     * Bug report: https://github.com/ReactiveX/RxJava/issues/431
     * @throws InterruptedException if the test is interrupted
     */
    @Test
    public final void testUnSubscribeForScheduler() throws InterruptedException {
        final AtomicInteger countReceived = new AtomicInteger();
        final AtomicInteger countGenerated = new AtomicInteger();
        final CountDownLatch latch = new CountDownLatch(1);

        Flowable.interval(50, TimeUnit.MILLISECONDS)
                .map(new Function<Long, Long>() {
                    @Override
                    public Long apply(Long aLong) {
                        countGenerated.incrementAndGet();
                        return aLong;
                    }
                })
                .subscribeOn(getScheduler())
                .observeOn(getScheduler())
                .subscribe(new DefaultSubscriber<Long>() {
                    @Override
                    public void onComplete() {
                        System.out.println("--- completed");
                    }

                    @Override
                    public void onError(Throwable e) {
                        System.out.println("--- onError");
                    }

                    @Override
                    public void onNext(Long args) {
                        if (countReceived.incrementAndGet() == 2) {
                            cancel();
                            latch.countDown();
                        }
                        System.out.println("==> Received " + args);
                    }
                });

        latch.await(1000, TimeUnit.MILLISECONDS);

        System.out.println("----------- it thinks it is finished ------------------ ");

        int timeout = 10;

        while (timeout-- > 0 && countGenerated.get() != 2) {
            Thread.sleep(100);
        }

        assertEquals(2, countGenerated.get());
    }

    @Test
    public void testUnsubscribeRecursiveScheduleFromOutside() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final CountDownLatch unsubscribeLatch = new CountDownLatch(1);
        final AtomicInteger counter = new AtomicInteger();
        final Worker inner = getScheduler().createWorker();
        try {
            inner.schedule(new Runnable() {

                @Override
                public void run() {
                    inner.schedule(new Runnable() {

                        int i;

                        @Override
                        public void run() {
                            System.out.println("Run: " + i++);
                            if (i == 10) {
                                latch.countDown();
                                try {
                                    // wait for unsubscribe to finish so we are not racing it
                                    unsubscribeLatch.await();
                                } catch (InterruptedException e) {
                                    // we expect the countDown if unsubscribe is not working
                                    // or to be interrupted if unsubscribe is successful since
                                    // the unsubscribe will interrupt it as it is calling Future.cancel(true)
                                    // so we will ignore the stacktrace
                                }
                            }

                            counter.incrementAndGet();
                            inner.schedule(this);
                        }
                    });
                }

            });

            latch.await();
            inner.dispose();
            unsubscribeLatch.countDown();
            Thread.sleep(200); // let time pass to see if the scheduler is still doing work
            assertEquals(10, counter.get());
        } finally {
            inner.dispose();
        }
    }

    @Test
    public void testUnsubscribeRecursiveScheduleFromInside() throws InterruptedException {
        final CountDownLatch unsubscribeLatch = new CountDownLatch(1);
        final AtomicInteger counter = new AtomicInteger();
        final Worker inner = getScheduler().createWorker();
        try {
            inner.schedule(new Runnable() {

                @Override
                public void run() {
                    inner.schedule(new Runnable() {

                        int i;

                        @Override
                        public void run() {
                            System.out.println("Run: " + i++);
                            if (i == 10) {
                                inner.dispose();
                            }

                            counter.incrementAndGet();
                            inner.schedule(this);
                        }
                    });
                }

            });

            unsubscribeLatch.countDown();
            Thread.sleep(200); // let time pass to see if the scheduler is still doing work
            assertEquals(10, counter.get());
        } finally {
            inner.dispose();
        }
    }

    @Test
    public void testUnsubscribeRecursiveScheduleWithDelay() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final CountDownLatch unsubscribeLatch = new CountDownLatch(1);
        final AtomicInteger counter = new AtomicInteger();
        final Worker inner = getScheduler().createWorker();

        try {
            inner.schedule(new Runnable() {

                @Override
                public void run() {
                    inner.schedule(new Runnable() {

                        long i = 1L;

                        @Override
                        public void run() {
                            if (i++ == 10) {
                                latch.countDown();
                                try {
                                    // wait for unsubscribe to finish so we are not racing it
                                    unsubscribeLatch.await();
                                } catch (InterruptedException e) {
                                    // we expect the countDown if unsubscribe is not working
                                    // or to be interrupted if unsubscribe is successful since
                                    // the unsubscribe will interrupt it as it is calling Future.cancel(true)
                                    // so we will ignore the stacktrace
                                }
                            }

                            counter.incrementAndGet();
                            inner.schedule(this, 10, TimeUnit.MILLISECONDS);
                        }
                    }, 10, TimeUnit.MILLISECONDS);
                }
            });

            latch.await();
            inner.dispose();
            unsubscribeLatch.countDown();
            Thread.sleep(200); // let time pass to see if the scheduler is still doing work
            assertEquals(10, counter.get());
        } finally {
            inner.dispose();
        }
    }

    @Test
    public void recursionFromOuterActionAndUnsubscribeInside() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final Worker inner = getScheduler().createWorker();
        try {
            inner.schedule(new Runnable() {

                int i;

                @Override
                public void run() {
                    i++;
                    if (i % 100000 == 0) {
                        System.out.println(i + "  Total Memory: " + Runtime.getRuntime().totalMemory() + "  Free: " + Runtime.getRuntime().freeMemory());
                    }
                    if (i < 1000000L) {
                        inner.schedule(this);
                    } else {
                        latch.countDown();
                    }
                }
            });

            latch.await();
        } finally {
            inner.dispose();
        }
    }

    @Test
    public void testRecursion() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final Worker inner = getScheduler().createWorker();
        try {
            inner.schedule(new Runnable() {

                private long i;

                @Override
                public void run() {
                    i++;
                    if (i % 100000 == 0) {
                        System.out.println(i + "  Total Memory: " + Runtime.getRuntime().totalMemory() + "  Free: " + Runtime.getRuntime().freeMemory());
                    }
                    if (i < 1000000L) {
                        inner.schedule(this);
                    } else {
                        latch.countDown();
                    }
                }
            });

            latch.await();
        } finally {
            inner.dispose();
        }
    }

    @Test
    public void testRecursionAndOuterUnsubscribe() throws InterruptedException {
        // use latches instead of Thread.sleep
        final CountDownLatch latch = new CountDownLatch(10);
        final CountDownLatch completionLatch = new CountDownLatch(1);
        final Worker inner = getScheduler().createWorker();
        try {
            Flowable<Integer> obs = Flowable.unsafeCreate(new Publisher<Integer>() {
                @Override
                public void subscribe(final Subscriber<? super Integer> observer) {
                    inner.schedule(new Runnable() {
                        @Override
                        public void run() {
                            observer.onNext(42);
                            latch.countDown();

                            // this will recursively schedule this task for execution again
                            inner.schedule(this);
                        }
                    });

                    observer.onSubscribe(new Subscription() {

                        @Override
                        public void cancel() {
                            inner.dispose();
                            observer.onComplete();
                            completionLatch.countDown();
                        }

                        @Override
                        public void request(long n) {

                        }
                    });

                }
            });

            final AtomicInteger count = new AtomicInteger();
            final AtomicBoolean completed = new AtomicBoolean(false);
            ResourceSubscriber<Integer> s = new ResourceSubscriber<Integer>() {
                @Override
                public void onComplete() {
                    System.out.println("Completed");
                    completed.set(true);
                }

                @Override
                public void onError(Throwable e) {
                    System.out.println("Error");
                }

                @Override
                public void onNext(Integer args) {
                    count.incrementAndGet();
                    System.out.println(args);
                }
            };
            obs.subscribe(s);

            if (!latch.await(5000, TimeUnit.MILLISECONDS)) {
                fail("Timed out waiting on onNext latch");
            }

            // now unsubscribe and ensure it stops the recursive loop
            s.dispose();
            System.out.println("unsubscribe");

            if (!completionLatch.await(5000, TimeUnit.MILLISECONDS)) {
                fail("Timed out waiting on completion latch");
            }

            // the count can be 10 or higher due to thread scheduling of the unsubscribe vs the scheduler looping to emit the count
            assertTrue(count.get() >= 10);
            assertTrue(completed.get());
        } finally {
            inner.dispose();
        }
    }

    @Test
    public final void testSubscribeWithScheduler() throws InterruptedException {
        final Scheduler scheduler = getScheduler();

        final AtomicInteger count = new AtomicInteger();

        Flowable<Integer> o1 = Flowable.<Integer> just(1, 2, 3, 4, 5);

        o1.subscribe(new Consumer<Integer>() {

            @Override
            public void accept(Integer t) {
                System.out.println("Thread: " + Thread.currentThread().getName());
                System.out.println("t: " + t);
                count.incrementAndGet();
            }
        });

        // the above should be blocking so we should see a count of 5
        assertEquals(5, count.get());

        count.set(0);

        // now we'll subscribe with a scheduler and it should be async

        final String currentThreadName = Thread.currentThread().getName();

        // latches for deterministically controlling the test below across threads
        final CountDownLatch latch = new CountDownLatch(5);
        final CountDownLatch first = new CountDownLatch(1);

        o1.subscribeOn(scheduler).subscribe(new Consumer<Integer>() {

            @Override
            public void accept(Integer t) {
                try {
                    // we block the first one so we can assert this executes asynchronously with a count
                    first.await(1000, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    throw new RuntimeException("The latch should have released if we are async.", e);
                }

                assertFalse(Thread.currentThread().getName().equals(currentThreadName));
                System.out.println("Thread: " + Thread.currentThread().getName());
                System.out.println("t: " + t);
                count.incrementAndGet();
                latch.countDown();
            }
        });

        // assert we are async
        assertEquals(0, count.get());
        // release the latch so it can go forward
        first.countDown();

        // wait for all 5 responses
        latch.await();
        assertEquals(5, count.get());
    }

}
