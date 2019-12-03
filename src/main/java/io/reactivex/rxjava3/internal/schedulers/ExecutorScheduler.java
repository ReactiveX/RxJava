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

package io.reactivex.rxjava3.internal.schedulers;

import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.disposables.*;
import io.reactivex.rxjava3.internal.disposables.*;
import io.reactivex.rxjava3.internal.functions.Functions;
import io.reactivex.rxjava3.internal.queue.MpscLinkedQueue;
import io.reactivex.rxjava3.internal.schedulers.ExecutorScheduler.ExecutorWorker.*;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.schedulers.*;

/**
 * Wraps an Executor and provides the Scheduler API over it.
 */
public final class ExecutorScheduler extends Scheduler {

    final boolean interruptibleWorker;

    final boolean fair;

    @NonNull
    final Executor executor;

    static final Scheduler HELPER = Schedulers.single();

    public ExecutorScheduler(@NonNull Executor executor, boolean interruptibleWorker, boolean fair) {
        this.executor = executor;
        this.interruptibleWorker = interruptibleWorker;
        this.fair = fair;
    }

    @NonNull
    @Override
    public Worker createWorker() {
        return new ExecutorWorker(executor, interruptibleWorker, fair);
    }

    @NonNull
    @Override
    public Disposable scheduleDirect(@NonNull Runnable run) {
        Runnable decoratedRun = RxJavaPlugins.onSchedule(run);
        try {
            if (executor instanceof ExecutorService) {
                ScheduledDirectTask task = new ScheduledDirectTask(decoratedRun);
                Future<?> f = ((ExecutorService)executor).submit(task);
                task.setFuture(f);
                return task;
            }

            if (interruptibleWorker) {
                InterruptibleRunnable interruptibleTask = new InterruptibleRunnable(decoratedRun, null);
                executor.execute(interruptibleTask);
                return interruptibleTask;
            } else {
                BooleanRunnable br = new BooleanRunnable(decoratedRun);
                executor.execute(br);
                return br;
            }
        } catch (RejectedExecutionException ex) {
            RxJavaPlugins.onError(ex);
            return EmptyDisposable.INSTANCE;
        }
    }

    @NonNull
    @Override
    public Disposable scheduleDirect(@NonNull Runnable run, final long delay, final TimeUnit unit) {
        final Runnable decoratedRun = RxJavaPlugins.onSchedule(run);
        if (executor instanceof ScheduledExecutorService) {
            try {
                ScheduledDirectTask task = new ScheduledDirectTask(decoratedRun);
                Future<?> f = ((ScheduledExecutorService)executor).schedule(task, delay, unit);
                task.setFuture(f);
                return task;
            } catch (RejectedExecutionException ex) {
                RxJavaPlugins.onError(ex);
                return EmptyDisposable.INSTANCE;
            }
        }

        final DelayedRunnable dr = new DelayedRunnable(decoratedRun);

        Disposable delayed = HELPER.scheduleDirect(new DelayedDispose(dr), delay, unit);

        dr.timed.replace(delayed);

        return dr;
    }

    @NonNull
    @Override
    public Disposable schedulePeriodicallyDirect(@NonNull Runnable run, long initialDelay, long period, TimeUnit unit) {
        if (executor instanceof ScheduledExecutorService) {
            Runnable decoratedRun = RxJavaPlugins.onSchedule(run);
            try {
                ScheduledDirectPeriodicTask task = new ScheduledDirectPeriodicTask(decoratedRun);
                Future<?> f = ((ScheduledExecutorService)executor).scheduleAtFixedRate(task, initialDelay, period, unit);
                task.setFuture(f);
                return task;
            } catch (RejectedExecutionException ex) {
                RxJavaPlugins.onError(ex);
                return EmptyDisposable.INSTANCE;
            }
        }
        return super.schedulePeriodicallyDirect(run, initialDelay, period, unit);
    }
    /* public: test support. */
    public static final class ExecutorWorker extends Scheduler.Worker implements Runnable {

        final boolean interruptibleWorker;

        final boolean fair;

        final Executor executor;

        final MpscLinkedQueue<Runnable> queue;

        volatile boolean disposed;

        final AtomicInteger wip = new AtomicInteger();

        final CompositeDisposable tasks = new CompositeDisposable();

        public ExecutorWorker(Executor executor, boolean interruptibleWorker, boolean fair) {
            this.executor = executor;
            this.queue = new MpscLinkedQueue<Runnable>();
            this.interruptibleWorker = interruptibleWorker;
            this.fair = fair;
        }

        @NonNull
        @Override
        public Disposable schedule(@NonNull Runnable run) {
            if (disposed) {
                return EmptyDisposable.INSTANCE;
            }

            Runnable decoratedRun = RxJavaPlugins.onSchedule(run);

            Runnable task;
            Disposable disposable;

            if (interruptibleWorker) {
                InterruptibleRunnable interruptibleTask = new InterruptibleRunnable(decoratedRun, tasks);
                tasks.add(interruptibleTask);

                task = interruptibleTask;
                disposable = interruptibleTask;
            } else {
                BooleanRunnable runnableTask = new BooleanRunnable(decoratedRun);

                task = runnableTask;
                disposable = runnableTask;
            }

            queue.offer(task);

            if (wip.getAndIncrement() == 0) {
                try {
                    executor.execute(this);
                } catch (RejectedExecutionException ex) {
                    disposed = true;
                    queue.clear();
                    RxJavaPlugins.onError(ex);
                    return EmptyDisposable.INSTANCE;
                }
            }

            return disposable;
        }

        @NonNull
        @Override
        public Disposable schedule(@NonNull Runnable run, long delay, @NonNull TimeUnit unit) {
            if (delay <= 0) {
                return schedule(run);
            }
            if (disposed) {
                return EmptyDisposable.INSTANCE;
            }

            SequentialDisposable first = new SequentialDisposable();

            final SequentialDisposable mar = new SequentialDisposable(first);

            final Runnable decoratedRun = RxJavaPlugins.onSchedule(run);

            ScheduledRunnable sr = new ScheduledRunnable(new SequentialDispose(mar, decoratedRun), tasks);
            tasks.add(sr);

            if (executor instanceof ScheduledExecutorService) {
                try {
                    Future<?> f = ((ScheduledExecutorService)executor).schedule((Callable<Object>)sr, delay, unit);
                    sr.setFuture(f);
                } catch (RejectedExecutionException ex) {
                    disposed = true;
                    RxJavaPlugins.onError(ex);
                    return EmptyDisposable.INSTANCE;
                }
            } else {
                final Disposable d = HELPER.scheduleDirect(sr, delay, unit);
                sr.setFuture(new DisposeOnCancel(d));
            }

            first.replace(sr);

            return mar;
        }

        @Override
        public void dispose() {
            if (!disposed) {
                disposed = true;
                tasks.dispose();
                if (wip.getAndIncrement() == 0) {
                    queue.clear();
                }
            }
        }

        @Override
        public boolean isDisposed() {
            return disposed;
        }

        @Override
        public void run() {
            if (fair) {
                runFair();
            } else {
                runEager();
            }
        }

        void runFair() {
            final MpscLinkedQueue<Runnable> q = queue;
            if (disposed) {
                q.clear();
                return;
            }

            Runnable run = q.poll();
            if (run != null) {
                run.run();
            }

            if (disposed) {
                q.clear();
                return;
            }

            if (wip.decrementAndGet() != 0) {
                executor.execute(this);
            }
        }

        void runEager() {
            int missed = 1;
            final MpscLinkedQueue<Runnable> q = queue;
            for (;;) {

                if (disposed) {
                    q.clear();
                    return;
                }

                for (;;) {
                    Runnable run = q.poll();
                    if (run == null) {
                        break;
                    }
                    run.run();

                    if (disposed) {
                        q.clear();
                        return;
                    }
                }

                if (disposed) {
                    q.clear();
                    return;
                }

                missed = wip.addAndGet(-missed);
                if (missed == 0) {
                    break;
                }
            }
        }

        static final class BooleanRunnable extends AtomicBoolean implements Runnable, Disposable {

            private static final long serialVersionUID = -2421395018820541164L;

            final Runnable actual;
            BooleanRunnable(Runnable actual) {
                this.actual = actual;
            }

            @Override
            public void run() {
                if (get()) {
                    return;
                }
                try {
                    actual.run();
                } finally {
                    lazySet(true);
                }
            }

            @Override
            public void dispose() {
                lazySet(true);
            }

            @Override
            public boolean isDisposed() {
                return get();
            }
        }

        final class SequentialDispose implements Runnable {
            private final SequentialDisposable mar;
            private final Runnable decoratedRun;

            SequentialDispose(SequentialDisposable mar, Runnable decoratedRun) {
                this.mar = mar;
                this.decoratedRun = decoratedRun;
            }

            @Override
            public void run() {
                mar.replace(schedule(decoratedRun));
            }
        }

        /**
         * Wrapper for a {@link Runnable} with additional logic for handling interruption on
         * a shared thread, similar to how Java Executors do it.
         */
        static final class InterruptibleRunnable extends AtomicInteger implements Runnable, Disposable {

            private static final long serialVersionUID = -3603436687413320876L;

            final Runnable run;

            final DisposableContainer tasks;

            volatile Thread thread;

            static final int READY = 0;

            static final int RUNNING = 1;

            static final int FINISHED = 2;

            static final int INTERRUPTING = 3;

            static final int INTERRUPTED = 4;

            InterruptibleRunnable(Runnable run, DisposableContainer tasks) {
                this.run = run;
                this.tasks = tasks;
            }

            @Override
            public void run() {
                if (get() == READY) {
                    thread = Thread.currentThread();
                    if (compareAndSet(READY, RUNNING)) {
                        try {
                            run.run();
                        } finally {
                            thread = null;
                            if (compareAndSet(RUNNING, FINISHED)) {
                                cleanup();
                            } else {
                                while (get() == INTERRUPTING) {
                                    Thread.yield();
                                }
                                Thread.interrupted();
                            }
                        }
                    } else {
                        thread = null;
                    }
                }
            }

            @Override
            public void dispose() {
                for (;;) {
                    int state = get();
                    if (state >= FINISHED) {
                        break;
                    } else if (state == READY) {
                        if (compareAndSet(READY, INTERRUPTED)) {
                            cleanup();
                            break;
                        }
                    } else {
                        if (compareAndSet(RUNNING, INTERRUPTING)) {
                            Thread t = thread;
                            if (t != null) {
                                t.interrupt();
                                thread = null;
                            }
                            set(INTERRUPTED);
                            cleanup();
                            break;
                        }
                    }
                }
            }

            void cleanup() {
                if (tasks != null) {
                    tasks.delete(this);
                }
            }

            @Override
            public boolean isDisposed() {
                return get() >= FINISHED;
            }
        }
    }

    static final class DelayedRunnable extends AtomicReference<Runnable>
            implements Runnable, Disposable, SchedulerRunnableIntrospection {

        private static final long serialVersionUID = -4101336210206799084L;

        final SequentialDisposable timed;

        final SequentialDisposable direct;

        DelayedRunnable(Runnable run) {
            super(run);
            this.timed = new SequentialDisposable();
            this.direct = new SequentialDisposable();
        }

        @Override
        public void run() {
            Runnable r = get();
            if (r != null) {
                try {
                    r.run();
                } finally {
                    lazySet(null);
                    timed.lazySet(DisposableHelper.DISPOSED);
                    direct.lazySet(DisposableHelper.DISPOSED);
                }
            }
        }

        @Override
        public boolean isDisposed() {
            return get() == null;
        }

        @Override
        public void dispose() {
            if (getAndSet(null) != null) {
                timed.dispose();
                direct.dispose();
            }
        }

        @Override
        public Runnable getWrappedRunnable() {
            Runnable r = get();
            return r != null ? r : Functions.EMPTY_RUNNABLE;
        }
    }

    final class DelayedDispose implements Runnable {
        private final DelayedRunnable dr;

        DelayedDispose(DelayedRunnable dr) {
            this.dr = dr;
        }

        @Override
        public void run() {
            dr.direct.replace(scheduleDirect(dr));
        }
    }
}
