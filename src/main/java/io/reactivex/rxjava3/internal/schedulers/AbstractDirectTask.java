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

import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.internal.functions.Functions;
import io.reactivex.rxjava3.schedulers.SchedulerRunnableIntrospection;

/**
 * Base functionality for direct tasks that manage a runnable and cancellation/completion.
 * @since 2.0.8
 */
abstract class AbstractDirectTask
extends AtomicReference<Future<?>>
implements Disposable, SchedulerRunnableIntrospection {

    private static final long serialVersionUID = 1811839108042568751L;

    protected final Runnable runnable;

    protected final boolean interruptOnCancel;

    protected Thread runner;

    protected static final FutureTask<Void> FINISHED = new FutureTask<>(Functions.EMPTY_RUNNABLE, null);

    protected static final FutureTask<Void> DISPOSED = new FutureTask<>(Functions.EMPTY_RUNNABLE, null);

    AbstractDirectTask(Runnable runnable, boolean interruptOnCancel) {
        this.runnable = runnable;
        this.interruptOnCancel = interruptOnCancel;
    }

    @Override
    public final void dispose() {
        Future<?> f = get();
        if (f != FINISHED && f != DISPOSED) {
            if (compareAndSet(f, DISPOSED)) {
                if (f != null) {
                    cancelFuture(f);
                }
            }
        }
    }

    @Override
    public final boolean isDisposed() {
        Future<?> f = get();
        return f == FINISHED || f == DISPOSED;
    }

    public final void setFuture(Future<?> future) {
        for (;;) {
            Future<?> f = get();
            if (f == FINISHED) {
                break;
            }
            if (f == DISPOSED) {
                cancelFuture(future);
                break;
            }
            if (compareAndSet(f, future)) {
                break;
            }
        }
    }

    private void cancelFuture(Future<?> future) {
        if (runner == Thread.currentThread()) {
            future.cancel(false);
        } else {
            future.cancel(interruptOnCancel);
        }
    }

    @Override
    public Runnable getWrappedRunnable() {
        return runnable;
    }

    @Override
    public String toString() {
        String status;
        Future<?> f = get();
        if (f == FINISHED) {
            status = "Finished";
        } else if (f == DISPOSED) {
            status = "Disposed";
        } else {
            Thread r = runner;
            if (r != null) {
                status = "Running on " + runner;
            } else {
                status = "Waiting";
            }
        }

        return getClass().getSimpleName() + "[" + status + "]";
    }
}
