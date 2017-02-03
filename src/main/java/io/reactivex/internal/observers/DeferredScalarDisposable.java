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

package io.reactivex.internal.observers;

import io.reactivex.Observer;
import io.reactivex.annotations.Nullable;
import io.reactivex.plugins.RxJavaPlugins;

/**
 * Represents a fuseable container for a single value.
 *
 * @param <T> the value type received and emitted
 */
public class DeferredScalarDisposable<T> extends BasicIntQueueDisposable<T> {

    private static final long serialVersionUID = -5502432239815349361L;

    /** The target of the events. */
    protected final Observer<? super T> actual;

    /** The value stored temporarily when in fusion mode. */
    protected T value;

    /** Indicates there was a call to complete(T). */
    static final int TERMINATED = 2;

    /** Indicates the Disposable has been disposed. */
    static final int DISPOSED = 4;

    /** Indicates this Disposable is in fusion mode and is currently empty. */
    static final int FUSED_EMPTY = 8;
    /** Indicates this Disposable is in fusion mode and has a value. */
    static final int FUSED_READY = 16;
    /** Indicates this Disposable is in fusion mode and its value has been consumed. */
    static final int FUSED_CONSUMED = 32;

    /**
     * Constructs a DeferredScalarDisposable by wrapping the Observer.
     * @param actual the Observer to wrap, not null (not verified)
     */
    public DeferredScalarDisposable(Observer<? super T> actual) {
        this.actual = actual;
    }

    @Override
    public final int requestFusion(int mode) {
        if ((mode & ASYNC) != 0) {
            lazySet(FUSED_EMPTY);
            return ASYNC;
        }
        return NONE;
    }

    /**
     * Complete the target with a single value or indicate there is a value available in
     * fusion mode.
     * @param value the value to signal, not null (not verified)
     */
    public final void complete(T value) {
        int state = get();
        if ((state & (FUSED_READY | FUSED_CONSUMED | TERMINATED | DISPOSED)) != 0) {
            return;
        }
        if (state == FUSED_EMPTY) {
            this.value = value;
            lazySet(FUSED_READY);
        } else {
            lazySet(TERMINATED);
        }
        Observer<? super T> a = actual;
        a.onNext(value);
        if (get() != DISPOSED) {
            a.onComplete();
        }
    }

    /**
     * Complete the target with an error signal.
     * @param t the Throwable to signal, not null (not verified)
     */
     public final void error(Throwable t) {
        int state = get();
        if ((state & (FUSED_READY | FUSED_CONSUMED | TERMINATED | DISPOSED)) != 0) {
            RxJavaPlugins.onError(t);
            return;
        }
        lazySet(TERMINATED);
        actual.onError(t);
    }

     /**
      * Complete the target without any value.
      */
    public final void complete() {
        int state = get();
        if ((state & (FUSED_READY | FUSED_CONSUMED | TERMINATED | DISPOSED)) != 0) {
            return;
        }
        lazySet(TERMINATED);
        actual.onComplete();
    }

    @Nullable
    @Override
    public final T poll() throws Exception {
        if (get() == FUSED_READY) {
            T v = value;
            value = null;
            lazySet(FUSED_CONSUMED);
            return v;
        }
        return null;
    }

    @Override
    public final boolean isEmpty() {
        return get() != FUSED_READY;
    }

    @Override
    public final void clear() {
        lazySet(FUSED_CONSUMED);
        value = null;
    }

    @Override
    public void dispose() {
        set(DISPOSED);
        value = null;
    }

    /**
     * Try disposing this Disposable and return true if the current thread succeeded.
     * @return true if the current thread succeeded
     */
    public final boolean tryDispose() {
        return getAndSet(DISPOSED) != DISPOSED;
    }

    @Override
    public final boolean isDisposed() {
        return get() == DISPOSED;
    }

}
