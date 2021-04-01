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

package io.reactivex.rxjava3.internal.schedulers;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.disposables.*;
import io.reactivex.rxjava3.internal.disposables.*;
import io.reactivex.rxjava3.internal.functions.ObjectHelper;

/**
 * Holds a fixed pool of worker threads and assigns them
 * to requested Scheduler.Workers in a round-robin fashion.
 */
public final class ComputationScheduler extends Scheduler implements SchedulerMultiWorkerSupport {
    /** This will indicate no pool is active. */
    static final FixedSchedulerPool NONE;
    /** Manages a fixed number of workers. */
    private static final String THREAD_NAME_PREFIX = "RxComputationThreadPool";
    static final RxThreadFactory THREAD_FACTORY;
    /**
     * Key to setting the maximum number of computation scheduler threads.
     * Zero or less is interpreted as use available. Capped by available.
     */
    static final String KEY_MAX_THREADS = "rx3.computation-threads";
    /** The maximum number of computation scheduler threads. */
    static final int MAX_THREADS;

    static final PoolWorker SHUTDOWN_WORKER;

    final ThreadFactory threadFactory;
    final AtomicReference<FixedSchedulerPool> pool;
    /** The name of the system property for setting the thread priority for this Scheduler. */
    private static final String KEY_COMPUTATION_PRIORITY = "rx3.computation-priority";

    static {
        MAX_THREADS = cap(Runtime.getRuntime().availableProcessors(), Integer.getInteger(KEY_MAX_THREADS, 0));

        SHUTDOWN_WORKER = new PoolWorker(new RxThreadFactory("RxComputationShutdown"));
        SHUTDOWN_WORKER.dispose();

        int priority = Math.max(Thread.MIN_PRIORITY, Math.min(Thread.MAX_PRIORITY,
                Integer.getInteger(KEY_COMPUTATION_PRIORITY, Thread.NORM_PRIORITY)));

        THREAD_FACTORY = new RxThreadFactory(THREAD_NAME_PREFIX, priority, true);

        NONE = new FixedSchedulerPool(0, THREAD_FACTORY);
        NONE.shutdown();
    }

    static int cap(int cpuCount, int paramThreads) {
        return paramThreads <= 0 || paramThreads > cpuCount ? cpuCount : paramThreads;
    }

    static final class FixedSchedulerPool implements SchedulerMultiWorkerSupport {
        final int cores;

        final PoolWorker[] eventLoops;
        long n;

        FixedSchedulerPool(int maxThreads, ThreadFactory threadFactory) {
            // initialize event loops
            this.cores = maxThreads;
            this.eventLoops = new PoolWorker[maxThreads];
            for (int i = 0; i < maxThreads; i++) {
                this.eventLoops[i] = new PoolWorker(threadFactory);
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

        @Override
        public void createWorkers(int number, WorkerCallback callback) {
            int c = cores;
            if (c == 0) {
                for (int i = 0; i < number; i++) {
                    callback.onWorker(i, SHUTDOWN_WORKER);
                }
            } else {
                int index = (int)n % c;
                for (int i = 0; i < number; i++) {
                    callback.onWorker(i, new EventLoopWorker(eventLoops[index]));
                    if (++index == c) {
                        index = 0;
                    }
                }
                n = index;
            }
        }
    }

    /**
     * Create a scheduler with pool size equal to the available processor
     * count and using least-recent worker selection policy.
     */
    public ComputationScheduler() {
        this(THREAD_FACTORY);
    }

    /**
     * Create a scheduler with pool size equal to the available processor
     * count and using least-recent worker selection policy.
     *
     * @param threadFactory thread factory to use for creating worker threads. Note that this takes precedence over any
     *                      system properties for configuring new thread creation. Cannot be null.
     */
    public ComputationScheduler(ThreadFactory threadFactory) {
        this.threadFactory = threadFactory;
        this.pool = new AtomicReference<>(NONE);
        start();
    }

    @NonNull
    @Override
    public Worker createWorker() {
        return new EventLoopWorker(pool.get().getEventLoop());
    }

    @Override
    public void createWorkers(int number, WorkerCallback callback) {
        ObjectHelper.verifyPositive(number, "number > 0 required");
        pool.get().createWorkers(number, callback);
    }

    @NonNull
    @Override
    public Disposable scheduleDirect(@NonNull Runnable run, long delay, TimeUnit unit) {
        PoolWorker w = pool.get().getEventLoop();
        return w.scheduleDirect(run, delay, unit);
    }

    @NonNull
    @Override
    public Disposable schedulePeriodicallyDirect(@NonNull Runnable run, long initialDelay, long period, TimeUnit unit) {
        PoolWorker w = pool.get().getEventLoop();
        return w.schedulePeriodicallyDirect(run, initialDelay, period, unit);
    }

    @Override
    public void start() {
        FixedSchedulerPool update = new FixedSchedulerPool(MAX_THREADS, threadFactory);
        if (!pool.compareAndSet(NONE, update)) {
            update.shutdown();
        }
    }

    @Override
    public void shutdown() {
        FixedSchedulerPool curr = pool.getAndSet(NONE);
        if (curr != NONE) {
            curr.shutdown();
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

        @NonNull
        @Override
        public Disposable schedule(@NonNull Runnable action) {
            if (disposed) {
                return EmptyDisposable.INSTANCE;
            }

            return poolWorker.scheduleActual(action, 0, TimeUnit.MILLISECONDS, serial);
        }

        @NonNull
        @Override
        public Disposable schedule(@NonNull Runnable action, long delayTime, @NonNull TimeUnit unit) {
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
