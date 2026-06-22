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

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.rxjava4.core.Scheduler;
import io.reactivex.rxjava4.disposables.*;
import io.reactivex.rxjava4.exceptions.Exceptions;
import io.reactivex.rxjava4.internal.functions.Functions;
import io.reactivex.rxjava4.plugins.RxJavaPlugins;

/**
 * Scheduler with a configurable fixed amount of thread-pools.
 * @since 4.0.0
 */
public final class ParallelScheduler extends Scheduler {

    static final ScheduledExecutorService[] SHUTDOWN;

    static final ScheduledExecutorService REJECTING;

    final ThreadFactory factory;

    final int parallelism;

    final boolean tracking;

    final AtomicReference<ScheduledExecutorService[]> pool;

    int n;

    /**
     * The default {@link RxThreadFactory} with "RxParallelScheduler" as a name and {@link Thread#NORM_PRIORITY} as priority.
     */
    public static final RxThreadFactory DEFAULT_FACTORY = new RxThreadFactory("RxParallelScheduler", Thread.NORM_PRIORITY);

    static {
        SHUTDOWN = new ScheduledExecutorService[0];

        REJECTING = Executors.newSingleThreadScheduledExecutor();
        REJECTING.shutdownNow();
    }

    public ParallelScheduler(int parallelism, boolean tracking, ThreadFactory factory) {
        if (parallelism <= 0) {
            throw new IllegalArgumentException("parallelism > 0 required but it was " + parallelism);
        }
        this.parallelism = parallelism;
        this.factory = factory;
        this.tracking = tracking;
        this.pool = new AtomicReference<>(SHUTDOWN);
        start();
    }

    @Override
    public void start() {
        ScheduledExecutorService[] next = null;
        for (;;) {
            ScheduledExecutorService[] current = pool.get();
            if (current != SHUTDOWN) {
                if (next != null) {
                    for (ScheduledExecutorService exec : next) {
                        exec.shutdownNow();
                    }
                }
                return;
            }
            if (next == null) {
                next = new ScheduledExecutorService[parallelism];
                for (int i = 0; i < next.length; i++) {
                    next[i] = Executors.newSingleThreadScheduledExecutor(factory);
                }
            }

            if (pool.compareAndSet(current, next)) {
                return;
            }
        }
    }

    @Override
    public void shutdown() {
        for (;;) {
            ScheduledExecutorService[] current = pool.get();
            if (current == SHUTDOWN) {
                return;
            }
            if (pool.compareAndSet(current, SHUTDOWN)) {
                for (ScheduledExecutorService exec : current) {
                    exec.shutdownNow();
                }
            }
        }
    }

    ScheduledExecutorService pick() {
        ScheduledExecutorService[] current = pool.get();
        if (current.length == 0) {
            return REJECTING;
        }
        int idx = this.n;
        if (idx >= parallelism) {
            idx = 0;
        }
        this.n = idx + 1; // may race, we don't care
        return current[idx];
    }

    @Override
    public Worker createWorker() {
        if (tracking) {
            return new TrackingParallelWorker(pick());
        }
        return new NonTrackingParallelWorker(pick());
    }

    @Override
    public Disposable scheduleDirect(Runnable run) {
        ScheduledExecutorService exec = pick();
        if (exec == REJECTING) {
            return Disposable.disposed();
        }
        try {
            return Disposable.fromFuture(exec.submit(RxJavaPlugins.onSchedule(run)));
        } catch (RejectedExecutionException ex) {
            return Disposable.disposed();
        }
    }

    @Override
    public Disposable scheduleDirect(Runnable run, long delay, TimeUnit unit) {
        ScheduledExecutorService exec = pick();
        if (exec == REJECTING) {
            return Disposable.disposed();
        }
        try {
            return Disposable.fromFuture(exec.schedule(RxJavaPlugins.onSchedule(run), delay, unit));
        } catch (RejectedExecutionException ex) {
            return Disposable.disposed();
        }
    }

    @Override
    public Disposable schedulePeriodicallyDirect(Runnable run, long initialDelay, long period, TimeUnit unit) {
        ScheduledExecutorService exec = pick();
        if (exec == REJECTING) {
            return Disposable.disposed();
        }
        try {
            return Disposable.fromFuture(exec.scheduleAtFixedRate(RxJavaPlugins.onSchedule(run), initialDelay, period, unit));
        } catch (RejectedExecutionException ex) {
            return Disposable.disposed();
        }
    }

    static final class NonTrackingParallelWorker extends Worker {

        final ScheduledExecutorService exec;

        volatile boolean shutdown;

        NonTrackingParallelWorker(ScheduledExecutorService exec) {
            this.exec = exec;
        }

        @Override
        public void dispose() {
            shutdown = true;
        }

        @Override
        public boolean isDisposed() {
            return shutdown;
        }

        @Override
        public Disposable schedule(Runnable run) {
            if (!shutdown) {
                try {
                    NonTrackingTask ntt = new NonTrackingTask(RxJavaPlugins.onSchedule(run));
                    exec.submit(ntt);
                    return ntt;
                } catch (RejectedExecutionException ex) {
                    // just let it fall through
                }
            }
            return Disposable.disposed();
        }

        @Override
        public Disposable schedule(Runnable run, long delay, TimeUnit unit) {
            if (!shutdown) {
                try {
                    NonTrackingTask ntt = new NonTrackingTask(RxJavaPlugins.onSchedule(run));
                    exec.schedule(ntt, delay, unit);
                    return ntt;
                } catch (RejectedExecutionException ex) {
                    // just let it fall through
                }
            }
            return Disposable.disposed();
        }

        // Not implementing a custom schedulePeriodically as it would require tracking the Future.

        final class NonTrackingTask implements Callable<Object>, Disposable {

            final Runnable actual;

            volatile boolean disposed;

            NonTrackingTask(Runnable actual) {
                this.actual = actual;
            }

            @Override
            public Object call() throws Exception {
                if (!disposed && !shutdown) {
                    try {
                        actual.run();
                    } catch (Throwable ex) {
                        Exceptions.throwIfFatal(ex);
                        RxJavaPlugins.onError(ex);
                    }
                }
                return null;
            }

            @Override
            public void dispose() {
                disposed = true;
            }

            @Override
            public boolean isDisposed() {
                return disposed;
            }
        }
    }

    static final class TrackingParallelWorker extends Worker {

        final ScheduledExecutorService exec;

        final CompositeDisposable tasks;

        TrackingParallelWorker(ScheduledExecutorService exec) {
            this.exec = exec;
            this.tasks = new CompositeDisposable();
        }

        @Override
        public void dispose() {
            tasks.dispose();
        }

        @Override
        public boolean isDisposed() {
            return tasks.isDisposed();
        }

        @Override
        public Disposable schedule(Runnable run) {
            if (!isDisposed()) {
                TrackedAction ta = new TrackedAction(RxJavaPlugins.onSchedule(run), tasks);
                if (tasks.add(ta)) {
                    try {
                        Future<?> f = exec.submit(ta);
                        ta.setFuture(f);
                        return ta;
                    } catch (RejectedExecutionException ex) {
                        // let it fall through
                    }
                }
            }
            return Disposable.disposed();
        }

        @Override
        public Disposable schedule(Runnable run, long delay, TimeUnit unit) {
            if (!isDisposed()) {
                TrackedAction ta = new TrackedAction(RxJavaPlugins.onSchedule(run), tasks);
                if (tasks.add(ta)) {
                    try {
                        Future<?> f = exec.schedule(ta, delay, unit);
                        ta.setFuture(f);
                        return ta;
                    } catch (RejectedExecutionException ex) {
                        // let it fall through
                    }
                }
            }
            return Disposable.disposed();
        }

        static final class TrackedAction
        extends AtomicReference<DisposableContainer>
        implements Callable<Object>, Disposable {

            static final Future<?> FINISHED;

            static final Future<?> DISPOSED;

            static {
                FINISHED = new FutureTask<>(Functions.EMPTY_RUNNABLE, null);
                FINISHED.cancel(false);
                DISPOSED = new FutureTask<>(Functions.EMPTY_RUNNABLE, null);
                DISPOSED.cancel(false);
            }

            private static final long serialVersionUID = 4949851341419870956L;

            final AtomicReference<Future<?>> future;

            final Runnable actual;

            TrackedAction(Runnable actual, DisposableContainer parent) {
                this.actual = actual;
                this.lazySet(parent);
                this.future = new AtomicReference<>();
            }

            @Override
            public Object call() {
                try {
                    actual.run();
                } catch (Throwable ex) {
                    Exceptions.throwIfFatal(ex);
                    RxJavaPlugins.onError(ex);
                }
                complete();
                return null;
            }

            void complete() {
                DisposableContainer cd = get();
                if (cd != null && compareAndSet(cd, null)) {
                    cd.delete(this);
                }
                for (;;) {
                    Future<?> f = future.get();
                    if (f == DISPOSED || future.compareAndSet(f, FINISHED)) {
                        break;
                    }
                }
            }

            @Override
            public void dispose() {
                DisposableContainer cd = getAndSet(null);
                if (cd != null) {
                    cd.delete(this);
                }
                Future<?> f = future.get();
                if (f != FINISHED && f != DISPOSED) {
                    f = future.getAndSet(DISPOSED);
                    if (f != null && f != FINISHED && f != DISPOSED) {
                        f.cancel(true);
                    }
                }
            }

            @Override
            public boolean isDisposed() {
                return get() == null;
            }

            void setFuture(Future<?> d) {
                Future<?> f = future.get();
                if (f != FINISHED) {
                    if (f == DISPOSED) {
                        d.cancel(true);
                    } else
                    if (!future.compareAndSet(f, d)) {
                        f = future.get();
                        if (f == DISPOSED) {
                            d.cancel(true);
                        }
                    }
                }
            }
        }
    }
}
