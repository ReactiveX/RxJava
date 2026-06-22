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

package io.reactivex.rxjava4.internal.schedulers;

import static org.junit.Assert.*;

import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import io.reactivex.rxjava4.core.*;
import io.reactivex.rxjava4.core.Scheduler.Worker;
import io.reactivex.rxjava4.core.config.ParallelSchedulerConfig;
import io.reactivex.rxjava4.disposables.*;
import io.reactivex.rxjava4.internal.functions.Functions;
import io.reactivex.rxjava4.internal.schedulers.ParallelScheduler.TrackingParallelWorker.TrackedAction;
import io.reactivex.rxjava4.schedulers.Schedulers;
import io.reactivex.rxjava4.testsupport.TestHelper;

public class ParallelSchedulerTest implements Runnable {

    final AtomicInteger calls = new AtomicInteger();

    @Override
    public void run() {
        calls.getAndIncrement();
    }

    @Test
    public void normalNonTracking() {
        Scheduler s = new ParallelScheduler(2, false, ParallelScheduler.DEFAULT_FACTORY);

        for (int i = 0; i < 100; i++) {
            Flowable.range(1, 10).hide()
            .observeOn(s, false, 4)
            .test()
            .awaitDone(5, TimeUnit.SECONDS)
            .assertResult(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        }
    }

    @Test
    public void normalNonTrackingVia() {
        Scheduler s = Schedulers.createParallel(new ParallelSchedulerConfig(2, false, ParallelScheduler.DEFAULT_FACTORY));

        for (int i = 0; i < 100; i++) {
            Flowable.range(1, 10).hide()
            .observeOn(s, false, 4)
            .test()
            .awaitDone(5, TimeUnit.SECONDS)
            .assertResult(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        }
    }

    @Test
    public void delayedNonTracking() {
        Scheduler s = new ParallelScheduler(2, false, ParallelScheduler.DEFAULT_FACTORY);

        try {
            for (int i = 0; i < 100; i++) {
                Flowable.range(1, 10).hide()
                .delay(50, TimeUnit.MILLISECONDS, s)
                .test()
                .awaitDone(5, TimeUnit.SECONDS)
                .assertResult(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
            }
        } finally {
            s.shutdown();
        }
    }

    @Test
    public void normalTracking() {
        Scheduler s = new ParallelScheduler(2, true, ParallelScheduler.DEFAULT_FACTORY);

        try {
            for (int i = 0; i < 100; i++) {
                Flowable.range(1, 10).hide()
                .observeOn(s, false, 4)
                .test()
                .awaitDone(5, TimeUnit.SECONDS)
                .assertResult(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
            }
        } finally {
            s.shutdown();
        }
    }

    @Test
    public void delayedTracking() {
        Scheduler s = new ParallelScheduler(2, true, ParallelScheduler.DEFAULT_FACTORY);

        try {
            for (int i = 0; i < 100; i++) {
                Flowable.range(1, 10).hide()
                .delay(50, TimeUnit.MILLISECONDS, s)
                .test()
                .awaitDone(5, TimeUnit.SECONDS)
                .assertResult(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
            }
        } finally {
            s.shutdown();
        }
    }

    @Test
    public void shutdownNonTracking() throws Exception {
        Scheduler s = new ParallelScheduler(2, false, ParallelScheduler.DEFAULT_FACTORY);

        shutdown(s);
    }

    @Test
    public void shutdownTracking() throws Exception {
        Scheduler s = new ParallelScheduler(2, true, ParallelScheduler.DEFAULT_FACTORY);

        shutdown(s);
    }

    protected void shutdown(Scheduler s) throws InterruptedException {
        try {
            Worker w = s.createWorker();

            w.dispose();

            assertSame(Disposable.disposed(), w.schedule(this));

            assertSame(Disposable.disposed(), w.schedule(this, 100, TimeUnit.MILLISECONDS));

            assertSame(Disposable.disposed(), w.schedulePeriodically(this, 100, 100, TimeUnit.MILLISECONDS));

            s.shutdown();

            assertSame(Disposable.disposed(), s.scheduleDirect(this));

            assertSame(Disposable.disposed(), s.scheduleDirect(this, 100, TimeUnit.MILLISECONDS));

            assertSame(Disposable.disposed(), s.schedulePeriodicallyDirect(this, 100, 100, TimeUnit.MILLISECONDS));

            w = s.createWorker();

            assertSame(Disposable.disposed(), w.schedule(this));

            assertSame(Disposable.disposed(), w.schedule(this, 100, TimeUnit.MILLISECONDS));

            assertSame(Disposable.disposed(), w.schedulePeriodically(this, 100, 100, TimeUnit.MILLISECONDS));

            assertEquals(0, calls.get());

            s.start();

            s.scheduleDirect(this);

            s.scheduleDirect(this, 100, TimeUnit.MILLISECONDS);

            s.schedulePeriodicallyDirect(this, 100, 100, TimeUnit.MILLISECONDS);

            w = s.createWorker();

            w.schedule(this);

            w.schedule(this, 100, TimeUnit.MILLISECONDS);

            w.schedulePeriodically(this, 100, 100, TimeUnit.MILLISECONDS);

            Thread.sleep(1000);

            int c = calls.get();
            assertTrue("" + c, c > 6);
        } finally {
            s.shutdown();
        }
    }

    @Test(timeout = 5000)
    public void taskThrowsNonTracking() throws Exception {
        Scheduler s = new ParallelScheduler(2, false, ParallelScheduler.DEFAULT_FACTORY);
        taskThrows(s);
    }

    @Test(timeout = 5000)
    public void taskThrowsTracking() throws Exception {
        Scheduler s = new ParallelScheduler(2, true, ParallelScheduler.DEFAULT_FACTORY);
        taskThrows(s);
    }

    protected void taskThrows(Scheduler s) throws InterruptedException {
        try {
            List<Throwable> errors = TestHelper.trackPluginErrors();

            Worker w = s.createWorker();

            w.schedule(new Runnable() {
                @Override
                public void run() {
                    calls.getAndIncrement();
                    throw new IllegalStateException();
                }
            });

            while (errors.isEmpty()) {
                Thread.sleep(20);
            }

            TestHelper.assertError(errors, 0, IllegalStateException.class);
        } finally {
            s.shutdown();
        }
    }

    @Test
    public void cancelledTaskNotTracking() throws Exception {
        Scheduler s = new ParallelScheduler(2, false, ParallelScheduler.DEFAULT_FACTORY);
        cancelledTask(s);
    }

    @Test
    public void cancelledTaskTracking() throws Exception {
        Scheduler s = new ParallelScheduler(2, true, ParallelScheduler.DEFAULT_FACTORY);
        cancelledTask(s);
    }

    void cancelledTask(Scheduler s) throws InterruptedException {
        try {
            Worker w = s.createWorker();

            try {
                assertFalse(w.isDisposed());

                Disposable d = w.schedule(this, 200, TimeUnit.MILLISECONDS);

                assertFalse(d.isDisposed());

                d.dispose();

                assertTrue(d.isDisposed());

                Thread.sleep(300);

                assertEquals(0, calls.get());
                w.dispose();

                assertTrue(w.isDisposed());
            } finally {
                w.dispose();
            }
        } finally {
            s.shutdown();
        }
    }

    @Test
    public void constructors() {
        startStop(new ParallelScheduler(1, true, new RxThreadFactory("Test")));
    }

    private void startStop(Scheduler s) {
        s.start();
        s.shutdown();
        s.shutdown();
    }

    @Test
    public void shutdownBackingTracking() {
        ParallelScheduler s = new ParallelScheduler(2, true, ParallelScheduler.DEFAULT_FACTORY);

        shutdownBacking(s);
    }

    @Test
    public void shutdownBackingNonTracking() {
        ParallelScheduler s = new ParallelScheduler(2, false, ParallelScheduler.DEFAULT_FACTORY);

        shutdownBacking(s);
    }

    private void shutdownBacking(ParallelScheduler s) {
        for (ScheduledExecutorService exec : s.pool.get()) {
            exec.shutdown();
        }

        assertSame(Disposable.disposed(), s.scheduleDirect(this));

        assertSame(Disposable.disposed(), s.scheduleDirect(this, 100, TimeUnit.MILLISECONDS));

        assertSame(Disposable.disposed(), s.schedulePeriodicallyDirect(this, 100, 100, TimeUnit.MILLISECONDS));

        Worker w = s.createWorker();

        assertSame(Disposable.disposed(), w.schedule(this));

        assertSame(Disposable.disposed(), w.schedule(this, 100, TimeUnit.MILLISECONDS));

        assertSame(Disposable.disposed(), w.schedulePeriodically(this, 100, 100, TimeUnit.MILLISECONDS));

        assertEquals(0, calls.get());
    }

    @Test
    public void startRace() {
        for (int i = 0; i < 1000; i++) {
            final Scheduler s = new ParallelScheduler(2, true, ParallelScheduler.DEFAULT_FACTORY);
            s.shutdown();

            Runnable r = new Runnable() {
                @Override
                public void run() {
                    s.start();
                }
            };

            TestHelper.race(r, r, Schedulers.single());
        }
    }

    @Test
    public void setFutureRace() {
        final Scheduler s = new ParallelScheduler(2, true, ParallelScheduler.DEFAULT_FACTORY);
        try {
            for (int i = 0; i < 1000; i++) {
                final Worker w = s.createWorker();

                Runnable r1 = new Runnable() {
                    @Override
                    public void run() {
                        w.schedule(ParallelSchedulerTest.this);
                    }
                };

                Runnable r2 = new Runnable() {
                    @Override
                    public void run() {
                        w.dispose();
                    }
                };
                TestHelper.race(r1, r2, Schedulers.single());
            }
        } finally {
            s.shutdown();
        }
    }

    @Test
    public void setFutureRace2() {
        final Scheduler s = new ParallelScheduler(2, true, ParallelScheduler.DEFAULT_FACTORY);
        try {
            for (int i = 0; i < 1000; i++) {
                final CompositeDisposable cd = new CompositeDisposable();
                try (final TrackedAction tt = new TrackedAction(this, cd)) {
                    final FutureTask<Object> ft = new FutureTask<>(Functions.EMPTY_RUNNABLE, null);

                    Runnable r1 = new Runnable() {
                        @Override
                        public void run() {
                            tt.setFuture(ft);
                        }
                    };

                    Runnable r2 = new Runnable() {
                        @Override
                        public void run() {
                            tt.future.set(TrackedAction.FINISHED);
                        }
                    };
                    TestHelper.race(r1, r2, Schedulers.single());
                }
            }
        } finally {
            s.shutdown();
        }
    }

    @Test
    public void setFutureRace3() {
        final Scheduler s = new ParallelScheduler(2, true, ParallelScheduler.DEFAULT_FACTORY);
        try {
            for (int i = 0; i < 1000; i++) {
                final CompositeDisposable cd = new CompositeDisposable();
                try (final TrackedAction tt = new TrackedAction(this, cd)) {
                    final FutureTask<Object> ft = new FutureTask<>(Functions.EMPTY_RUNNABLE, null);

                    Runnable r1 = new Runnable() {
                        @Override
                        public void run() {
                            tt.setFuture(ft);
                        }
                    };

                    Runnable r2 = new Runnable() {
                        @Override
                        public void run() {
                            tt.future.set(TrackedAction.DISPOSED);
                        }
                    };
                    TestHelper.race(r1, r2, Schedulers.single());
                }
            }
        } finally {
            s.shutdown();
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void illegalParallelism() {
        new ParallelScheduler(0, true, ParallelScheduler.DEFAULT_FACTORY);
    }

    @Test
    public void parallelSchedulerConfig() {
        {
        var psc1 = new ParallelSchedulerConfig();
        assertEquals("Parallelism", psc1.parallelism(), Runtime.getRuntime().availableProcessors());
        assertTrue("Tracking", psc1.tracking());
        assertEquals("threadNamePrefix", psc1.threadNamePrefix(), "RxParallelScheduler");
        }

        {
        var psc2 = new ParallelSchedulerConfig(1);
        assertEquals("Parallelism", psc2.parallelism(), 1);
        assertTrue("Tracking", psc2.tracking());
        assertEquals("threadNamePrefix", psc2.threadNamePrefix(), "RxParallelScheduler");
        }

        {
        var psc3 = new ParallelSchedulerConfig(1, false);
        assertEquals("Parallelism", psc3.parallelism(), 1);
        assertFalse("Tracking", psc3.tracking());
        assertEquals("threadNamePrefix", psc3.threadNamePrefix(), "RxParallelScheduler");
        }

        {
        var psc4 = new ParallelSchedulerConfig(1, "Test");
        assertEquals("Parallelism", psc4.parallelism(), 1);
        assertTrue("Tracking", psc4.tracking());
        assertEquals("threadNamePrefix", psc4.threadNamePrefix(), "Test");
        }

        {
        var psc5 = new ParallelSchedulerConfig(1, false, "Test");
        assertEquals("Parallelism", psc5.parallelism(), 1);
        assertFalse("Tracking", psc5.tracking());
        assertEquals("threadNamePrefix", psc5.threadNamePrefix(), "Test");
        }
    }
}
