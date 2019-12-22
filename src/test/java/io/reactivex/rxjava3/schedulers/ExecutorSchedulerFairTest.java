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

package io.reactivex.rxjava3.schedulers;

import static org.junit.Assert.*;

import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.core.Scheduler.Worker;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.internal.disposables.EmptyDisposable;
import io.reactivex.rxjava3.internal.functions.Functions;
import io.reactivex.rxjava3.internal.schedulers.RxThreadFactory;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.processors.PublishProcessor;
import io.reactivex.rxjava3.subscribers.TestSubscriber;
import io.reactivex.rxjava3.testsupport.TestHelper;

public class ExecutorSchedulerFairTest extends AbstractSchedulerConcurrencyTests {

    static final Executor executor = Executors.newFixedThreadPool(2, new RxThreadFactory("TestCustomPool"));

    @Override
    protected Scheduler getScheduler() {
        return Schedulers.from(executor, false, true);
    }

    @Test
    public final void handledErrorIsNotDeliveredToThreadHandler() throws InterruptedException {
        SchedulerTestHelper.handledErrorIsNotDeliveredToThreadHandler(getScheduler());
    }

    @Test
    public void cancelledTaskRetention() throws InterruptedException {
        ExecutorService exec = Executors.newSingleThreadExecutor();
        Scheduler s = Schedulers.from(exec, false, true);
        try {
            Scheduler.Worker w = s.createWorker();
            try {
                ExecutorSchedulerTest.cancelledRetention(w, false);
            } finally {
                w.dispose();
            }

            w = s.createWorker();
            try {
                ExecutorSchedulerTest.cancelledRetention(w, true);
            } finally {
                w.dispose();
            }
        } finally {
            exec.shutdownNow();
        }
    }

    /** A simple executor which queues tasks and executes them one-by-one if executeOne() is called. */
    static final class TestExecutor implements Executor {
        final ConcurrentLinkedQueue<Runnable> queue = new ConcurrentLinkedQueue<>();
        @Override
        public void execute(Runnable command) {
            queue.offer(command);
        }
        public void executeOne() {
            Runnable r = queue.poll();
            if (r != null) {
                r.run();
            }
        }
        public void executeAll() {
            Runnable r;
            while ((r = queue.poll()) != null) {
                r.run();
            }
        }
    }

    @Test
    public void cancelledTasksDontRun() {
        final AtomicInteger calls = new AtomicInteger();
        Runnable task = new Runnable() {
            @Override
            public void run() {
                calls.getAndIncrement();
            }
        };
        TestExecutor exec = new TestExecutor();
        Scheduler custom = Schedulers.from(exec, false, true);
        Worker w = custom.createWorker();
        try {
            Disposable d1 = w.schedule(task);
            Disposable d2 = w.schedule(task);
            Disposable d3 = w.schedule(task);

            d1.dispose();
            d2.dispose();
            d3.dispose();

            exec.executeAll();

            assertEquals(0, calls.get());
        } finally {
            w.dispose();
        }
    }

    @Test
    public void cancelledWorkerDoesntRunTasks() {
        final AtomicInteger calls = new AtomicInteger();
        Runnable task = new Runnable() {
            @Override
            public void run() {
                calls.getAndIncrement();
            }
        };
        TestExecutor exec = new TestExecutor();
        Scheduler custom = Schedulers.from(exec, false, true);
        Worker w = custom.createWorker();
        try {
            w.schedule(task);
            w.schedule(task);
            w.schedule(task);
        } finally {
            w.dispose();
        }
        exec.executeAll();
        assertEquals(0, calls.get());
    }

    @Test
    public void plainExecutor() throws Exception {
        Scheduler s = Schedulers.from(new Executor() {
            @Override
            public void execute(Runnable r) {
                r.run();
            }
        }, false, true);

        final CountDownLatch cdl = new CountDownLatch(5);

        Runnable r = new Runnable() {
            @Override
            public void run() {
                cdl.countDown();
            }
        };

        s.scheduleDirect(r);

        s.scheduleDirect(r, 50, TimeUnit.MILLISECONDS);

        Disposable d = s.schedulePeriodicallyDirect(r, 10, 10, TimeUnit.MILLISECONDS);

        try {
            assertTrue(cdl.await(5, TimeUnit.SECONDS));
        } finally {
            d.dispose();
        }

        assertTrue(d.isDisposed());
    }

    @Test
    public void rejectingExecutor() {
        ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();
        exec.shutdown();

        Scheduler s = Schedulers.from(exec, false, true);

        List<Throwable> errors = TestHelper.trackPluginErrors();

        try {
            assertSame(EmptyDisposable.INSTANCE, s.scheduleDirect(Functions.EMPTY_RUNNABLE));

            assertSame(EmptyDisposable.INSTANCE, s.scheduleDirect(Functions.EMPTY_RUNNABLE, 10, TimeUnit.MILLISECONDS));

            assertSame(EmptyDisposable.INSTANCE, s.schedulePeriodicallyDirect(Functions.EMPTY_RUNNABLE, 10, 10, TimeUnit.MILLISECONDS));

            TestHelper.assertUndeliverable(errors, 0, RejectedExecutionException.class);
            TestHelper.assertUndeliverable(errors, 1, RejectedExecutionException.class);
            TestHelper.assertUndeliverable(errors, 2, RejectedExecutionException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void rejectingExecutorWorker() {
        ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();
        exec.shutdown();

        List<Throwable> errors = TestHelper.trackPluginErrors();

        try {
            Worker s = Schedulers.from(exec, false, true).createWorker();
            assertSame(EmptyDisposable.INSTANCE, s.schedule(Functions.EMPTY_RUNNABLE));

            s = Schedulers.from(exec, false, true).createWorker();
            assertSame(EmptyDisposable.INSTANCE, s.schedule(Functions.EMPTY_RUNNABLE, 10, TimeUnit.MILLISECONDS));

            s = Schedulers.from(exec, false, true).createWorker();
            assertSame(EmptyDisposable.INSTANCE, s.schedulePeriodically(Functions.EMPTY_RUNNABLE, 10, 10, TimeUnit.MILLISECONDS));

            TestHelper.assertUndeliverable(errors, 0, RejectedExecutionException.class);
            TestHelper.assertUndeliverable(errors, 1, RejectedExecutionException.class);
            TestHelper.assertUndeliverable(errors, 2, RejectedExecutionException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void reuseScheduledExecutor() throws Exception {
        ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();

        try {
            Scheduler s = Schedulers.from(exec, false, true);

            final CountDownLatch cdl = new CountDownLatch(8);

            Runnable r = new Runnable() {
                @Override
                public void run() {
                    cdl.countDown();
                }
            };

            s.scheduleDirect(r);

            s.scheduleDirect(r, 10, TimeUnit.MILLISECONDS);

            Disposable d = s.schedulePeriodicallyDirect(r, 10, 10, TimeUnit.MILLISECONDS);

            try {
                assertTrue(cdl.await(5, TimeUnit.SECONDS));
            } finally {
                d.dispose();
            }
        } finally {
            exec.shutdown();
        }
    }

    @Test
    public void reuseScheduledExecutorAsWorker() throws Exception {
        ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();

        Worker s = Schedulers.from(exec, false, true).createWorker();

        assertFalse(s.isDisposed());
        try {

            final CountDownLatch cdl = new CountDownLatch(8);

            Runnable r = new Runnable() {
                @Override
                public void run() {
                    cdl.countDown();
                }
            };

            s.schedule(r);

            s.schedule(r, 10, TimeUnit.MILLISECONDS);

            Disposable d = s.schedulePeriodically(r, 10, 10, TimeUnit.MILLISECONDS);

            try {
                assertTrue(cdl.await(5, TimeUnit.SECONDS));
            } finally {
                d.dispose();
            }
        } finally {
            s.dispose();
            exec.shutdown();
        }

        assertTrue(s.isDisposed());
    }

    @Test
    public void disposeRace() {
        ExecutorService exec = Executors.newSingleThreadExecutor();
        final Scheduler s = Schedulers.from(exec, false, true);
        try {
            for (int i = 0; i < 500; i++) {
                final Worker w = s.createWorker();

                final AtomicInteger c = new AtomicInteger(2);

                w.schedule(new Runnable() {
                    @Override
                    public void run() {
                        c.decrementAndGet();
                        while (c.get() != 0) { }
                    }
                });

                c.decrementAndGet();
                while (c.get() != 0) { }
                w.dispose();
            }
        } finally {
            exec.shutdownNow();
        }
    }

    @Test
    public void runnableDisposed() {
        final Scheduler s = Schedulers.from(new Executor() {
            @Override
            public void execute(Runnable r) {
                r.run();
            }
        }, false, true);
        Disposable d = s.scheduleDirect(Functions.EMPTY_RUNNABLE);

        assertTrue(d.isDisposed());
    }

    @Test
    public void runnableDisposedAsync() throws Exception {
        final Scheduler s = Schedulers.from(new Executor() {
            @Override
            public void execute(Runnable r) {
                new Thread(r).start();
            }
        }, false, true);
        Disposable d = s.scheduleDirect(Functions.EMPTY_RUNNABLE);

        while (!d.isDisposed()) {
            Thread.sleep(1);
        }
    }

    @Test
    public void runnableDisposedAsync2() throws Exception {
        final Scheduler s = Schedulers.from(executor, false, true);
        Disposable d = s.scheduleDirect(Functions.EMPTY_RUNNABLE);

        while (!d.isDisposed()) {
            Thread.sleep(1);
        }
    }

    @Test
    public void runnableDisposedAsyncCrash() throws Exception {
        final Scheduler s = Schedulers.from(new Executor() {
            @Override
            public void execute(Runnable r) {
                new Thread(r).start();
            }
        }, false, true);
        Disposable d = s.scheduleDirect(new Runnable() {
            @Override
            public void run() {
                throw new IllegalStateException();
            }
        });

        while (!d.isDisposed()) {
            Thread.sleep(1);
        }
    }

    @Test
    public void runnableDisposedAsyncTimed() throws Exception {
        final Scheduler s = Schedulers.from(new Executor() {
            @Override
            public void execute(Runnable r) {
                new Thread(r).start();
            }
        }, false, true);
        Disposable d = s.scheduleDirect(Functions.EMPTY_RUNNABLE, 1, TimeUnit.MILLISECONDS);

        while (!d.isDisposed()) {
            Thread.sleep(1);
        }
    }

    @Test
    public void runnableDisposedAsyncTimed2() throws Exception {
        ExecutorService executorScheduler = Executors.newScheduledThreadPool(1, new RxThreadFactory("TestCustomPoolTimed"));
        try {
            final Scheduler s = Schedulers.from(executorScheduler, false, true);
            Disposable d = s.scheduleDirect(Functions.EMPTY_RUNNABLE, 1, TimeUnit.MILLISECONDS);

            while (!d.isDisposed()) {
                Thread.sleep(1);
            }
        } finally {
            executorScheduler.shutdownNow();
        }
    }

    @Test
    public void unwrapScheduleDirectTaskAfterDispose() {
        Scheduler scheduler = getScheduler();
        final CountDownLatch cdl = new CountDownLatch(1);
        Runnable countDownRunnable = new Runnable() {
            @Override
            public void run() {
                cdl.countDown();
            }
        };
        Disposable disposable = scheduler.scheduleDirect(countDownRunnable, 100, TimeUnit.MILLISECONDS);
        SchedulerRunnableIntrospection wrapper = (SchedulerRunnableIntrospection) disposable;
        assertSame(countDownRunnable, wrapper.getWrappedRunnable());
        disposable.dispose();

        assertSame(Functions.EMPTY_RUNNABLE, wrapper.getWrappedRunnable());
    }

    @Test
    public void fairInterleaving() {
        ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();
        try {
            final Scheduler sch = Schedulers.from(exec, false, true);

            PublishProcessor<Integer> pp = PublishProcessor.create();

            TestSubscriber<Integer> ts = pp
            .publish(new Function<Flowable<Integer>, Flowable<Integer>>() {
                @Override
                public Flowable<Integer> apply(Flowable<Integer> v) throws Throwable {
                    return Flowable.merge(
                            v.filter(new Predicate<Integer>() {
                                @Override
                                public boolean test(Integer w) throws Throwable {
                                    return w % 2 == 0;
                                }
                            }).observeOn(sch, false, 1).hide(),
                            v.filter(new Predicate<Integer>() {
                                @Override
                                public boolean test(Integer w) throws Throwable {
                                    return w % 2 != 0;
                                }
                            }).observeOn(sch, false, 1).hide()
                    );
                }
            })
            .test();

            for (int i = 1; i < 11; i++) {
                pp.onNext(i);
            }
            pp.onComplete();

            ts
            .awaitDone(5, TimeUnit.SECONDS)
            .assertResult(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        } finally {
            exec.shutdown();
        }
    }

    @Test
    public void fairInterleavingWithDelay() {
        ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();
        try {
            final Scheduler sch = Schedulers.from(exec, false, true);

            PublishProcessor<Integer> pp = PublishProcessor.create();

            TestSubscriber<Integer> ts = pp
            .publish(new Function<Flowable<Integer>, Flowable<Integer>>() {
                @Override
                public Flowable<Integer> apply(Flowable<Integer> v) throws Throwable {
                    return Flowable.merge(
                            v.filter(new Predicate<Integer>() {
                                @Override
                                public boolean test(Integer w) throws Throwable {
                                    return w % 2 == 0;
                                }
                            }).delay(0, TimeUnit.SECONDS, sch).hide(),
                            v.filter(new Predicate<Integer>() {
                                @Override
                                public boolean test(Integer w) throws Throwable {
                                    return w % 2 != 0;
                                }
                            }).delay(0, TimeUnit.SECONDS, sch).hide()
                    );
                }
            })
            .test();

            for (int i = 1; i < 11; i++) {
                pp.onNext(i);
            }
            pp.onComplete();

            ts
            .awaitDone(5, TimeUnit.SECONDS)
            .assertResult(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        } finally {
            exec.shutdown();
        }
    }
}
