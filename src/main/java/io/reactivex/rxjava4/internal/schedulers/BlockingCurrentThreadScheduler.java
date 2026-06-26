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

import java.io.Serial;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.concurrent.locks.*;

import io.reactivex.rxjava4.core.Scheduler;
import io.reactivex.rxjava4.disposables.*;
import io.reactivex.rxjava4.exceptions.Exceptions;
import io.reactivex.rxjava4.functions.Action;
import io.reactivex.rxjava4.internal.disposables.SequentialDisposable;
import io.reactivex.rxjava4.plugins.RxJavaPlugins;
import io.reactivex.rxjava4.schedulers.Schedulers;

/**
 * A Scheduler that uses the current thread, in an event-loop and
 * blocking fashion to execute actions.
 * <p>
 * Use the {@link #execute(Action)} to start waiting for tasks (from other
 * threads) or {@link #execute(Action)} to start with a first action.
 * <pre><code>
 * public static void main(String[] args) {
 *     var scheduler = new BlockingCurrentThreadScheduler();
 *     scheduler.execute(() -&gt; {
 *         // This executes in the blocking event loop.
 *         // Usually the rest of the "main" method should
 *         // be moved here.
 *
 *         someApi.methodCall()
 *           .subscribeOn(Schedulers.io())
 *           .observeOn(scheduler)
 *           .subscribe(v -&gt; { /* on the main thread *&#47; });
 *     });
 * }
 * </code></pre>
 * 
 * In the example code above, {@code observeOn(scheduler)} will execute
 * on the main thread of the Java application.
 * 
 * @since 4.0.0
 */
public final class BlockingCurrentThreadScheduler extends Scheduler {

    static final Action SHUTDOWN = new ShutdownAction();

    final class RunInDirect implements Runnable {
        private final BlockingDirectTask task;
        private final SequentialDisposable outer;

        private RunInDirect(BlockingDirectTask task, SequentialDisposable outer) {
            this.task = task;
            this.outer = outer;
        }

        @Override
        public void run() {
            outer.replace(task);
            enqueue(task);
        }
    }

    static final class ShutdownAction implements Action {
        @Override
        public void run() throws Throwable {
            // deliberately no-op
        }
    }

    static final int SPIN_LIMIT = 64;

    final ConcurrentLinkedQueue<Action> queue;

    final AtomicLong wip;

    final Lock lock;

    final Condition condition;

    final AtomicBoolean running;

    final AtomicBoolean shutdown;

    final Scheduler timedHelper;

    volatile Thread thread;

    public BlockingCurrentThreadScheduler() {
        this.queue = new ConcurrentLinkedQueue<>();
        this.lock = new ReentrantLock();
        this.condition = this.lock.newCondition();
        this.running = new AtomicBoolean();
        this.shutdown = new AtomicBoolean();
        this.wip = new AtomicLong();
        this.timedHelper = Schedulers.single();
    }

    /**
     * Begin executing the blocking event loop with the given initial action
     * (usually contain the rest of the 'main' method).
     * <p>
     * This method will block until the {@link #shutdown()} is invoked.
     * @param action the action to execute
     */
    public void execute(Action action) {
        Objects.requireNonNull(action, "action is null");
        if (!running.get() && running.compareAndSet(false, true)) {
            thread = Thread.currentThread();
            queue.offer(action);
            wip.getAndIncrement();
            drainLoop();
        }
    }

    void drainLoop() {
        final AtomicBoolean stop = shutdown;
        final AtomicLong wip = this.wip;

        for (;;) {
            if (stop.get()) {
                cancelAll();
                return;
            }
            do {
                Action a = queue.poll();
                if (a == SHUTDOWN) {
                    cancelAll();
                    return;
                }
                try {
                    a.run();
                } catch (Throwable ex) {
                    Exceptions.throwIfFatal(ex);
                    RxJavaPlugins.onError(ex);
                }
            } while (wip.decrementAndGet() != 0);

            if (wip.get() == 0 && !stop.get()) {
                lock.lock();
                try {
                    while (wip.get() == 0 && !stop.get()) {
                        condition.await();
                    }
                } catch (InterruptedException ex) {
                    // deliberately ignored
                } finally {
                    lock.unlock();
                }
            }
        }
    }

    void cancelAll() {
        final ConcurrentLinkedQueue<Action> q = queue;

        Action a;

        while ((a = q.poll()) != null) {
            if (a instanceof Disposable) {
                ((Disposable)a).dispose();
            }
        }
    }

    @Override
    public Disposable scheduleDirect(Runnable run, long delay, TimeUnit unit) {
        Objects.requireNonNull(run, "run is null");
        Objects.requireNonNull(unit, "unit is null");
        if (shutdown.get()) {
            return Disposable.disposed();
        }

        final BlockingDirectTask task = new BlockingDirectTask(run);

        if (delay == 0L) {
            enqueue(task);
            return task;
        }

        SequentialDisposable inner = new SequentialDisposable();
        final SequentialDisposable outer = new SequentialDisposable(inner);

        Disposable d = timedHelper.scheduleDirect(new RunInDirect(task, outer), delay, unit);

        if (d == Disposable.disposed()) {
            return d;
        }

        inner.replace(d);

        return outer;
    }

    @Override
    public void shutdown() {
        if (shutdown.compareAndSet(false, true)) {
            enqueue(SHUTDOWN);
        }
    }

    void enqueue(Action action) {
        queue.offer(action);
        if (wip.getAndIncrement() == 0L) {
            lock.lock();
            try {
                condition.signal();
            } finally {
                lock.unlock();
            }
        }
    }

    @Override
    public Worker createWorker() {
        return new BlockingWorker();
    }

    static final int READY = 0;
    static final int RUNNING = 1;
    static final int INTERRUPTING = 2;
    static final int INTERRUPTED = 3;
    static final int FINISHED = 4;
    static final int CANCELLED = 5;

    final class BlockingDirectTask
    extends AtomicInteger
    implements Action, Disposable {

        @Serial
        private static final long serialVersionUID = -9165914884456950194L;
        final Runnable task;

        BlockingDirectTask(Runnable task) {
            this.task = task;
        }

        @Override
        public void run() throws Exception {
            try {
                if (compareAndSet(READY, RUNNING)) {
                    try {
                        task.run();
                    } finally {
                        compareAndSet(RUNNING, FINISHED);
                    }
                }
            } finally {
                while (get() == INTERRUPTING) { Thread.onSpinWait(); }

                if (get() == INTERRUPTED) {
                    Thread.interrupted();
                }
            }
        }

        @Override
        public void dispose() {
            for (;;) {
                int s = get();
                if (s >= INTERRUPTING) {
                    break;
                }

                if (s == READY && compareAndSet(READY, CANCELLED)) {
                    break;
                }
                if (compareAndSet(RUNNING, INTERRUPTING)) {
                    Thread t = thread;
                    if (t != null) {
                        t.interrupt();
                    }
                    set(INTERRUPTED);
                    break;
                }
            }
        }

        @Override
        public boolean isDisposed() {
            return get() >= INTERRUPTING;
        }
    }

    final class BlockingWorker extends Worker {

        final CompositeDisposable tasks;

        BlockingWorker() {
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
        public Disposable schedule(Runnable run, long delay, TimeUnit unit) {
            Objects.requireNonNull(run, "run is null");
            Objects.requireNonNull(unit, "unit is null");

            if (shutdown.get() || isDisposed()) {
                return Disposable.disposed();
            }

            final BlockingTask task = new BlockingTask(run);
            tasks.add(task);

            if (delay == 0L) {
                enqueue(task);
                return task;
            }

            SequentialDisposable inner = new SequentialDisposable();
            final SequentialDisposable outer = new SequentialDisposable(inner);

            Disposable d = timedHelper.scheduleDirect(new RunInWorker(outer, task), delay, unit);

            if (d == Disposable.disposed()) {
                return d;
            }

            inner.replace(d);

            return outer;
        }

        final class RunInWorker implements Runnable {
            private final SequentialDisposable outer;
            private final BlockingTask task;

            private RunInWorker(SequentialDisposable outer, BlockingTask task) {
                this.outer = outer;
                this.task = task;
            }

            @Override
            public void run() {
                outer.replace(task);
                enqueue(task);
            }
        }

        final class BlockingTask
        extends AtomicInteger
        implements Action, Disposable {

            @Serial
            private static final long serialVersionUID = -9165914884456950194L;

            final Runnable task;

            BlockingTask(Runnable task) {
                this.task = task;
            }

            @Override
            public void run() throws Exception {
                try {
                    if (compareAndSet(READY, RUNNING)) {
                        try {
                            task.run();
                        } finally {
                            compareAndSet(RUNNING, FINISHED);
                            tasks.remove(this);
                        }
                    }
                } finally {
                    while (get() == INTERRUPTING) { Thread.onSpinWait(); }

                    if (get() == INTERRUPTED) {
                        Thread.interrupted();
                    }
                }
            }

            @Override
            public void dispose() {
                for (;;) {
                    int s = get();
                    if (s >= INTERRUPTING) {
                        return;
                    }

                    if (s == READY && compareAndSet(READY, CANCELLED)) {
                        break;
                    }
                    if (compareAndSet(RUNNING, INTERRUPTING)) {
                        Thread t = thread;
                        if (t != null) {
                            t.interrupt();
                        }
                        set(INTERRUPTED);
                        break;
                    }
                }
                tasks.remove(this);
            }

            @Override
            public boolean isDisposed() {
                return get() >= INTERRUPTING;
            }
        }
    }
}
