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
package rx.schedulers;

import static org.junit.Assert.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import rx.Observable;
import rx.Observable.OnSubscribeFunc;
import rx.Scheduler;
import rx.Subscriber;
import rx.Subscription;
import rx.operators.SafeObservableSubscription;
import rx.subscriptions.Subscriptions;
import rx.util.functions.Action0;
import rx.util.functions.Action1;
import rx.util.functions.Func1;
import rx.util.functions.Func2;

/**
 * Base tests for schedulers that involve threads (concurrency).
 * 
 * These can only run on Schedulers that launch threads since they expect async/concurrent behavior.
 * 
 * The Current/Immediate schedulers will not work with these tests.
 */
public abstract class AbstractSchedulerConcurrencyTests extends AbstractSchedulerTests {

    /**
     * Bug report: https://github.com/Netflix/RxJava/issues/431
     */
    @Test
    public final void testUnSubscribeForScheduler() throws InterruptedException {
        final AtomicInteger countReceived = new AtomicInteger();
        final AtomicInteger countGenerated = new AtomicInteger();
        final SafeObservableSubscription s = new SafeObservableSubscription();
        final CountDownLatch latch = new CountDownLatch(1);

        s.wrap(Observable.interval(50, TimeUnit.MILLISECONDS)
                .map(new Func1<Long, Long>() {
                    @Override
                    public Long call(Long aLong) {
                        countGenerated.incrementAndGet();
                        return aLong;
                    }
                })
                .subscribeOn(getScheduler())
                .observeOn(getScheduler())
                .subscribe(new Subscriber<Long>() {
                    @Override
                    public void onCompleted() {
                        System.out.println("--- completed");
                    }

                    @Override
                    public void onError(Throwable e) {
                        System.out.println("--- onError");
                    }

                    @Override
                    public void onNext(Long args) {
                        if (countReceived.incrementAndGet() == 2) {
                            s.unsubscribe();
                            latch.countDown();
                        }
                        System.out.println("==> Received " + args);
                    }
                }));

        latch.await(1000, TimeUnit.MILLISECONDS);

        System.out.println("----------- it thinks it is finished ------------------ ");
        Thread.sleep(100);

        assertEquals(2, countGenerated.get());
    }

    @Test
    public void testUnsubscribeRecursiveScheduleWithStateAndFunc2() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final CountDownLatch unsubscribeLatch = new CountDownLatch(1);
        final AtomicInteger counter = new AtomicInteger();
        Subscription s = getScheduler().schedule(1L, new Func2<Scheduler, Long, Subscription>() {

            @Override
            public Subscription call(Scheduler innerScheduler, Long i) {
                System.out.println("Run: " + i);
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
                return innerScheduler.schedule(i + 1, this);
            }
        });

        latch.await();
        s.unsubscribe();
        unsubscribeLatch.countDown();
        Thread.sleep(200); // let time pass to see if the scheduler is still doing work
        assertEquals(10, counter.get());
    }

    @Test
    public void testUnsubscribeRecursiveScheduleWithStateAndFunc2AndDelay() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final CountDownLatch unsubscribeLatch = new CountDownLatch(1);
        final AtomicInteger counter = new AtomicInteger();
        Subscription s = getScheduler().schedule(1L, new Func2<Scheduler, Long, Subscription>() {

            @Override
            public Subscription call(Scheduler innerScheduler, Long i) {
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
                return innerScheduler.schedule(i + 1, this, 10, TimeUnit.MILLISECONDS);
            }
        }, 10, TimeUnit.MILLISECONDS);

        latch.await();
        s.unsubscribe();
        unsubscribeLatch.countDown();
        Thread.sleep(200); // let time pass to see if the scheduler is still doing work
        assertEquals(10, counter.get());
    }

    @Test
    public void recursionUsingFunc2() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        getScheduler().schedule(1L, new Func2<Scheduler, Long, Subscription>() {

            @Override
            public Subscription call(Scheduler innerScheduler, Long i) {
                if (i % 100000 == 0) {
                    System.out.println(i + "  Total Memory: " + Runtime.getRuntime().totalMemory() + "  Free: " + Runtime.getRuntime().freeMemory());
                }
                if (i < 1000000L) {
                    return innerScheduler.schedule(i + 1, this);
                } else {
                    latch.countDown();
                    return Subscriptions.empty();
                }
            }
        });

        latch.await();
    }

    @Test
    public void recursionUsingAction0() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        getScheduler().schedule(new Action1<Action0>() {

            private long i = 0;

            @Override
            public void call(Action0 self) {
                i++;
                if (i % 100000 == 0) {
                    System.out.println(i + "  Total Memory: " + Runtime.getRuntime().totalMemory() + "  Free: " + Runtime.getRuntime().freeMemory());
                }
                if (i < 1000000L) {
                    self.call();
                } else {
                    latch.countDown();
                }
            }
        });

        latch.await();
    }

    @Test
    public void testRecursiveScheduler2() throws InterruptedException {
        // use latches instead of Thread.sleep
        final CountDownLatch latch = new CountDownLatch(10);
        final CountDownLatch completionLatch = new CountDownLatch(1);

        Observable<Integer> obs = Observable.create(new OnSubscribeFunc<Integer>() {
            @Override
            public Subscription onSubscribe(final Subscriber<? super Integer> observer) {

                return getScheduler().schedule(null, new Func2<Scheduler, Void, Subscription>() {
                    @Override
                    public Subscription call(Scheduler scheduler, Void v) {
                        observer.onNext(42);
                        latch.countDown();

                        // this will recursively schedule this task for execution again
                        scheduler.schedule(null, this);

                        return Subscriptions.create(new Action0() {

                            @Override
                            public void call() {
                                observer.onCompleted();
                                completionLatch.countDown();
                            }

                        });
                    }
                });
            }
        });

        final AtomicInteger count = new AtomicInteger();
        final AtomicBoolean completed = new AtomicBoolean(false);
        Subscription subscribe = obs.subscribe(new Subscriber<Integer>() {
            @Override
            public void onCompleted() {
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
        });

        if (!latch.await(5000, TimeUnit.MILLISECONDS)) {
            fail("Timed out waiting on onNext latch");
        }

        // now unsubscribe and ensure it stops the recursive loop
        subscribe.unsubscribe();
        System.out.println("unsubscribe");

        if (!completionLatch.await(5000, TimeUnit.MILLISECONDS)) {
            fail("Timed out waiting on completion latch");
        }

        // the count can be 10 or higher due to thread scheduling of the unsubscribe vs the scheduler looping to emit the count
        assertTrue(count.get() >= 10);
        assertTrue(completed.get());
    }

    @Test
    public final void testSubscribeWithScheduler() throws InterruptedException {
        final Scheduler scheduler = getScheduler();

        final AtomicInteger count = new AtomicInteger();

        Observable<Integer> o1 = Observable.<Integer> from(1, 2, 3, 4, 5);

        o1.subscribe(new Action1<Integer>() {

            @Override
            public void call(Integer t) {
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

        o1.subscribe(new Action1<Integer>() {

            @Override
            public void call(Integer t) {
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
        }, scheduler);

        // assert we are async
        assertEquals(0, count.get());
        // release the latch so it can go forward
        first.countDown();

        // wait for all 5 responses
        latch.await();
        assertEquals(5, count.get());
    }

}
