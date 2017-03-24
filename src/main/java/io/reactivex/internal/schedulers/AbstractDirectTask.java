/**
 * Copyright (c) 2016-present, RxJava Contributors.
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

import io.reactivex.disposables.Disposable;
import io.reactivex.internal.functions.Functions;

/**
 * Base functionality for direct tasks that manage a runnable and cancellation/completion.
 * @since 2.0.8
 */
abstract class AbstractDirectTask
extends AtomicReference<Future<?>>
implements Disposable {

    private static final long serialVersionUID = 1811839108042568751L;

    protected final Runnable runnable;

    protected Thread runner;

    protected static final FutureTask<Void> FINISHED = new FutureTask<Void>(Functions.EMPTY_RUNNABLE, null);

    protected static final FutureTask<Void> DISPOSED = new FutureTask<Void>(Functions.EMPTY_RUNNABLE, null);

    AbstractDirectTask(Runnable runnable) {
        this.runnable = runnable;
    }

    @Override
    public final void dispose() {
        Future<?> f = get();
        if (f != FINISHED && f != DISPOSED) {
            if (compareAndSet(f, DISPOSED)) {
                if (f != null) {
                    f.cancel(runner != Thread.currentThread());
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
                future.cancel(runner != Thread.currentThread());
                break;
            }
            if (compareAndSet(f, future)) {
                break;
            }
        }
    }
}
