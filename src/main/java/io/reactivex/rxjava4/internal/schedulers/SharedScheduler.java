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

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.rxjava4.core.Scheduler;
import io.reactivex.rxjava4.disposables.*;
import io.reactivex.rxjava4.internal.disposables.DisposableHelper;

/**
 * A Scheduler implementation that uses one of the Workers from another Scheduler
 * and shares the access to it through its own Workers.
 * <p>
 * Disposing a worker doesn't dispose the underlying shared worker so other
 * workers of this class can continue their work; use {@link #shutdown()} to release
 * the underlying shared worker.
 * <p>
 * This scheduler doesn't support {@link #start()} (it's a no-op) and once {@link #shutdown()}
 * it can't be revived.
 * @since 4.0.0
 */
public final class SharedScheduler extends Scheduler {

    final Worker worker;

    /**
     * Constructs a SharedScheduler and uses the Worker instance provided.
     * @param worker the worker to use, not null
     */
    public SharedScheduler(Worker worker) {
        this.worker = worker;
    }

    @Override
    public void shutdown() {
        worker.dispose();
    }

    @Override
    public Disposable scheduleDirect(Runnable run) {
        return worker.schedule(run);
    }

    @Override
    public Disposable scheduleDirect(Runnable run, long delay, TimeUnit unit) {
        return worker.schedule(run, delay, unit);
    }

    @Override
    public Disposable schedulePeriodicallyDirect(Runnable run, long initialDelay, long period, TimeUnit unit) {
        return worker.schedulePeriodically(run, initialDelay, period, unit);
    }

    @Override
    public long now(TimeUnit unit) {
        return worker.now(unit);
    }

    @Override
    public Worker createWorker() {
        return new SharedWorker(worker);
    }

    static final class SharedWorker extends Worker {

        final Worker worker;

        final CompositeDisposable tasks;

        SharedWorker(Worker worker) {
            this.worker = worker;
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
            if (isDisposed() || worker.isDisposed()) {
                return Disposable.disposed();
            }
            SharedAction sa = new SharedAction(run, tasks);
            tasks.add(sa);

            Disposable task;
            if (delay <= 0L) {
                task = worker.schedule(sa);
            } else {
                task = worker.schedule(sa, delay, unit);
            }
            sa.setFuture(task);

            return sa;
        }

        @Override
        public long now(TimeUnit unit) {
            return worker.now(unit);
        }

        static final class SharedAction
        extends AtomicReference<DisposableContainer>
        implements Runnable, Disposable {
            private static final long serialVersionUID = 4949851341419870956L;

            final AtomicReference<Disposable> future;

            final Runnable actual;

            SharedAction(Runnable actual, DisposableContainer parent) {
                this.actual = actual;
                this.lazySet(parent);
                this.future = new AtomicReference<>();
            }

            @Override
            public void run() {
                try {
                    actual.run();
                } finally {
                    complete();
                }
            }

            void complete() {
                DisposableContainer cd = get();
                if (cd != null && compareAndSet(cd, null)) {
                    cd.delete(this);
                }
                for (;;) {
                    Disposable f = future.get();
                    if (f == DisposableHelper.DISPOSED || future.compareAndSet(f, this)) {
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
                DisposableHelper.dispose(future);
            }

            @Override
            public boolean isDisposed() {
                return get() == null;
            }

            void setFuture(Disposable d) {
                Disposable f = future.get();
                if (f != this) {
                    if (f == DisposableHelper.DISPOSED) {
                        d.dispose();
                    } else
                    if (!future.compareAndSet(f, d)) {
                        f = future.get();
                        if (f == DisposableHelper.DISPOSED) {
                            d.dispose();
                        }
                    }
                }
            }
        }
    }
}
