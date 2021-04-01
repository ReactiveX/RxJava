/*
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

package io.reactivex.rxjava3.schedulers;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.core.Scheduler.Worker;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.internal.schedulers.ComputationScheduler;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.testsupport.SuppressUndeliverable;

public class ComputationSchedulerTests extends AbstractSchedulerConcurrencyTests {

    @Override
    protected Scheduler getScheduler() {
        // this is an implementation of ExecutorScheduler
        return Schedulers.computation();
    }

    @Test
    public void threadSafetyWhenSchedulerIsHoppingBetweenThreads() {

        final int NUM = 1000000;
        final CountDownLatch latch = new CountDownLatch(1);
        final HashMap<String, Integer> map = new HashMap<>();

        final Scheduler.Worker inner = Schedulers.computation().createWorker();

        try {
            inner.schedule(new Runnable() {

                private HashMap<String, Integer> statefulMap = map;
                int nonThreadSafeCounter;

                @Override
                public void run() {
                    Integer i = statefulMap.get("a");
                    if (i == null) {
                        i = 1;
                        statefulMap.put("a", i);
                        statefulMap.put("b", i);
                    } else {
                        i++;
                        statefulMap.put("a", i);
                        statefulMap.put("b", i);
                    }
                    nonThreadSafeCounter++;
                    statefulMap.put("nonThreadSafeCounter", nonThreadSafeCounter);
                    if (i < NUM) {
                        inner.schedule(this);
                    } else {
                        latch.countDown();
                    }
                }
            });

            try {
                latch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            System.out.println("Count A: " + map.get("a"));
            System.out.println("Count B: " + map.get("b"));
            System.out.println("nonThreadSafeCounter: " + map.get("nonThreadSafeCounter"));

            assertEquals(NUM, map.get("a").intValue());
            assertEquals(NUM, map.get("b").intValue());
            assertEquals(NUM, map.get("nonThreadSafeCounter").intValue());
        } finally {
            inner.dispose();
        }
    }

    @Test
    public final void computationThreadPool1() {
        Flowable<Integer> f1 = Flowable.<Integer> just(1, 2, 3, 4, 5);
        Flowable<Integer> f2 = Flowable.<Integer> just(6, 7, 8, 9, 10);
        Flowable<String> f = Flowable.<Integer> merge(f1, f2).map(new Function<Integer, String>() {

            @Override
            public String apply(Integer t) {
                assertTrue(Thread.currentThread().getName().startsWith("RxComputationThreadPool"));
                return "Value_" + t + "_Thread_" + Thread.currentThread().getName();
            }
        });

        f.subscribeOn(Schedulers.computation()).blockingForEach(new Consumer<String>() {

            @Override
            public void accept(String t) {
                System.out.println("t: " + t);
            }
        });
    }

    @Test
    public final void mergeWithExecutorScheduler() {

        final String currentThreadName = Thread.currentThread().getName();

        Flowable<Integer> f1 = Flowable.<Integer> just(1, 2, 3, 4, 5);
        Flowable<Integer> f2 = Flowable.<Integer> just(6, 7, 8, 9, 10);
        Flowable<String> f = Flowable.<Integer> merge(f1, f2).subscribeOn(Schedulers.computation()).map(new Function<Integer, String>() {

            @Override
            public String apply(Integer t) {
                assertNotEquals(Thread.currentThread().getName(), currentThreadName);
                assertTrue(Thread.currentThread().getName().startsWith("RxComputationThreadPool"));
                return "Value_" + t + "_Thread_" + Thread.currentThread().getName();
            }
        });

        f.blockingForEach(new Consumer<String>() {

            @Override
            public void accept(String t) {
                System.out.println("t: " + t);
            }
        });
    }

    @Test
    public final void handledErrorIsNotDeliveredToThreadHandler() throws InterruptedException {
        SchedulerTestHelper.handledErrorIsNotDeliveredToThreadHandler(getScheduler());
    }

    @Test
    public void cancelledTaskRetention() throws InterruptedException {
        Worker w = Schedulers.computation().createWorker();
        try {
            ExecutorSchedulerTest.cancelledRetention(w, false);
        } finally {
            w.dispose();
        }
        w = Schedulers.computation().createWorker();
        try {
            ExecutorSchedulerTest.cancelledRetention(w, true);
        } finally {
            w.dispose();
        }
    }

    @Test
    @SuppressUndeliverable
    public void shutdownRejects() {
        final int[] calls = { 0 };

        Runnable r = new Runnable() {
            @Override
            public void run() {
                calls[0]++;
            }
        };

        Scheduler s = new ComputationScheduler();
        s.shutdown();
        s.shutdown();

        assertEquals(Disposable.disposed(), s.scheduleDirect(r));

        assertEquals(Disposable.disposed(), s.scheduleDirect(r, 1, TimeUnit.SECONDS));

        assertEquals(Disposable.disposed(), s.schedulePeriodicallyDirect(r, 1, 1, TimeUnit.SECONDS));

        Worker w = s.createWorker();
        w.dispose();

        assertTrue(w.isDisposed());

        assertEquals(Disposable.disposed(), w.schedule(r));

        assertEquals(Disposable.disposed(), w.schedule(r, 1, TimeUnit.SECONDS));

        assertEquals(Disposable.disposed(), w.schedulePeriodically(r, 1, 1, TimeUnit.SECONDS));

        assertEquals(0, calls[0]);
    }

    @Test
    public void exceptionFromObservableShouldNotBeSwallowed() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);

        // #3 thread's uncaught exception handler
        Scheduler computationScheduler = new ComputationScheduler(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r);
                t.setUncaughtExceptionHandler((thread, throwable) -> {
                    latch.countDown();
                });
                return t;
            }
        });

        // #2 RxJava exception handler
        RxJavaPlugins.setErrorHandler(h -> {
            latch.countDown();
        });

        // Exceptions, fatal or not, should be handled by
        // #1 observer's onError(), or
        // #2 RxJava exception handler, or
        // #3 thread's uncaught exception handler,
        // and should not be swallowed.
        try {

            // #1 observer's onError()
            Observable.create(s -> {

                s.onNext(1);
                throw new OutOfMemoryError();
            })
            .subscribeOn(computationScheduler)
            .subscribe(v -> { },
                e -> { latch.countDown(); }
            );

            assertTrue(latch.await(2, TimeUnit.SECONDS));
        } finally {
            RxJavaPlugins.reset();
            computationScheduler.shutdown();
        }
    }

    @Test
    public void exceptionFromObserverShouldNotBeSwallowed() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);

        // #3 thread's uncaught exception handler
        Scheduler computationScheduler = new ComputationScheduler(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r);
                t.setUncaughtExceptionHandler((thread, throwable) -> {
                    latch.countDown();
                });
                return t;
            }
        });

        // #2 RxJava exception handler
        RxJavaPlugins.setErrorHandler(h -> {
            latch.countDown();
        });

        // Exceptions, fatal or not, should be handled by
        // #1 observer's onError(), or
        // #2 RxJava exception handler, or
        // #3 thread's uncaught exception handler,
        // and should not be swallowed.
        try {

            // #1 observer's onError()
            Flowable.interval(500, TimeUnit.MILLISECONDS, computationScheduler)
                    .subscribe(v -> {
                        throw new OutOfMemoryError();
                    }, e -> {
                        latch.countDown();
                    });

            assertTrue(latch.await(2, TimeUnit.SECONDS));
        } finally {
            RxJavaPlugins.reset();
            computationScheduler.shutdown();
        }
    }

    @Test
    @SuppressUndeliverable
    public void periodicTaskShouldStopOnError() throws Exception {
        AtomicInteger repeatCount = new AtomicInteger();

        Schedulers.computation().schedulePeriodicallyDirect(new Runnable() {
            @Override
            public void run() {
                repeatCount.incrementAndGet();
                throw new OutOfMemoryError();
            }
        }, 0, 1, TimeUnit.MILLISECONDS);

        Thread.sleep(200);

        assertEquals(1, repeatCount.get());
    }

    @Test
    @SuppressUndeliverable
    public void periodicTaskShouldStopOnError2() throws Exception {
        AtomicInteger repeatCount = new AtomicInteger();

        Schedulers.computation().schedulePeriodicallyDirect(new Runnable() {
            @Override
            public void run() {
                repeatCount.incrementAndGet();
                throw new OutOfMemoryError();
            }
        }, 0, 1, TimeUnit.NANOSECONDS);

        Thread.sleep(200);

        assertEquals(1, repeatCount.get());
    }
}
