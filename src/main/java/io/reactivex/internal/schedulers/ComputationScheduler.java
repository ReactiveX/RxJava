/**
 * Copyright 2016 Netflix, Inc.
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
package io.reactivex.internal.schedulers;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.Scheduler;
import io.reactivex.disposables.*;
import io.reactivex.internal.disposables.*;

/**
 * Holds a fixed pool of worker threads and assigns them
 * to requested Scheduler.Workers in a round-robin fashion.
 */
public final class ComputationScheduler extends Scheduler {
    /** This will indicate no pool is active. */
    static final FixedSchedulerPool NONE = new FixedSchedulerPool(0);
    /** Manages a fixed number of workers. */
    private static final String THREAD_NAME_PREFIX = "RxComputationThreadPool";
    static final RxThreadFactory THREAD_FACTORY;
    /**
     * Key to setting the maximum number of computation scheduler threads.
     * Zero or less is interpreted as use available. Capped by available.
     */
    static final String KEY_MAX_THREADS = "rx2.computation-threads";
    /** The maximum number of computation scheduler threads. */
    static final int MAX_THREADS;

    static final PoolWorker SHUTDOWN_WORKER;

    final AtomicReference<FixedSchedulerPool> pool;
    /** The name of the system property for setting the thread priority for this Scheduler. */
    private static final String KEY_COMPUTATION_PRIORITY = "rx2.computation-priority";

    static {
        MAX_THREADS = cap(Runtime.getRuntime().availableProcessors(), Integer.getInteger(KEY_MAX_THREADS, 0));

        SHUTDOWN_WORKER = new PoolWorker(new RxThreadFactory("RxComputationShutdown"));
        SHUTDOWN_WORKER.dispose();

        int priority = Math.max(Thread.MIN_PRIORITY, Math.min(Thread.MAX_PRIORITY,
                Integer.getInteger(KEY_COMPUTATION_PRIORITY, Thread.NORM_PRIORITY)));

        THREAD_FACTORY = new RxThreadFactory(THREAD_NAME_PREFIX, priority);
    }

    static int cap(int cpuCount, int paramThreads) {
        return paramThreads <= 0 || paramThreads > cpuCount ? cpuCount : paramThreads;
    }

    static final class FixedSchedulerPool {
        final int cores;

        final PoolWorker[] eventLoops;
        long n;

        FixedSchedulerPool(int maxThreads) {
            // initialize event loops
            this.cores = maxThreads;
            this.eventLoops = new PoolWorker[maxThreads];
            for (int i = 0; i < maxThreads; i++) {
                this.eventLoops[i] = new PoolWorker(THREAD_FACTORY);
            }
        }

        public PoolWorker getEventLoop() {
            int c = cores;
            if (c == 0) {
                return SHUTDOWN_WORKER;
            }
            // simple round robin, improvements to come
            return eventLoops[(int)(n++ % c)];
        }

        public void shutdown() {
            for (PoolWorker w : eventLoops) {
                w.dispose();
            }
        }
    }

    /**
     * Create a scheduler with pool size equal to the available processor
     * count and using least-recent worker selection policy.
     */
    public ComputationScheduler() {
        this.pool = new AtomicReference<FixedSchedulerPool>(NONE);
        start();
    }

    @Override
    public Worker createWorker() {
        return new EventLoopWorker(pool.get().getEventLoop());
    }

    @Override
    public Disposable scheduleDirect(Runnable run, long delay, TimeUnit unit) {
        PoolWorker w = pool.get().getEventLoop();
        return w.scheduleDirect(run, delay, unit);
    }

    @Override
    public Disposable schedulePeriodicallyDirect(Runnable run, long initialDelay, long period, TimeUnit unit) {
        PoolWorker w = pool.get().getEventLoop();
        return w.schedulePeriodicallyDirect(run, initialDelay, period, unit);
    }

    @Override
    public void start() {
        FixedSchedulerPool update = new FixedSchedulerPool(MAX_THREADS);
        if (!pool.compareAndSet(NONE, update)) {
            update.shutdown();
        }
    }

    @Override
    public void shutdown() {
        for (;;) {
            FixedSchedulerPool curr = pool.get();
            if (curr == NONE) {
                return;
            }
            if (pool.compareAndSet(curr, NONE)) {
                curr.shutdown();
                return;
            }
        }
    }


    static final class EventLoopWorker extends Scheduler.Worker {
        private final ListCompositeDisposable serial;
        private final CompositeDisposable timed;
        private final ListCompositeDisposable both;
        private final PoolWorker poolWorker;

        volatile boolean disposed;

        EventLoopWorker(PoolWorker poolWorker) {
            this.poolWorker = poolWorker;
            this.serial = new ListCompositeDisposable();
            this.timed = new CompositeDisposable();
            this.both = new ListCompositeDisposable();
            this.both.add(serial);
            this.both.add(timed);
        }

        @Override
        public void dispose() {
            if (!disposed) {
                disposed = true;
                both.dispose();
            }
        }

        @Override
        public boolean isDisposed() {
            return disposed;
        }

        @Override
        public Disposable schedule(Runnable action) {
            if (disposed) {
                return EmptyDisposable.INSTANCE;
            }

            return poolWorker.scheduleActual(action, 0, null, serial);
        }
        @Override
        public Disposable schedule(Runnable action, long delayTime, TimeUnit unit) {
            if (disposed) {
                return EmptyDisposable.INSTANCE;
            }

            return poolWorker.scheduleActual(action, delayTime, unit, timed);
        }
    }

    static final class PoolWorker extends NewThreadWorker {
        PoolWorker(ThreadFactory threadFactory) {
            super(threadFactory);
        }
    }
}