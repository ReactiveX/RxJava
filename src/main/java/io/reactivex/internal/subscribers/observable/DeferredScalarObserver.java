/**
 * Copyright 2016 Netflix, Inc.
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

package io.reactivex.internal.subscribers.observable;

import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.disposables.DisposableHelper;

/**
 * A fuseable Observer that can generate 0 or 1 resulting value. 
 * @param <T> the input value type
 * @param <R> the output value type
 */
public abstract class DeferredScalarObserver<T, R> 
extends BaseIntQueueDisposable<R>
implements Observer<T> {
    /** */
    private static final long serialVersionUID = -266195175408988651L;

    protected final Observer<? super R> actual;
    
    /** The upstream disposable. */
    protected Disposable s;
    
    /** Can indicate if there was at least on onNext call. */
    protected boolean hasValue;
    
    /** The result value. */
    protected R value;

    static final int NOT_FUSED = 0;
    static final int EMPTY = 1;
    static final int READY = 2;
    static final int CONSUMED = 3;
    
    /** True if this has been disposed. */ 
    volatile boolean disposed;
    
    /**
     * Creates a DeferredScalarObserver instance and wraps a downstream Observer.
     * @param actual the downstream subscriber, not null (not verified)
     */
    public DeferredScalarObserver(Observer<? super R> actual) {
        this.actual = actual;
    }

    @Override
    public void onSubscribe(Disposable s) {
        if (DisposableHelper.validate(this.s, s)) {
            this.s = s;
            
            actual.onSubscribe(this);
        }
    }
    
    @Override
    public void onError(Throwable t) {
        int state = get();
        if (state == NOT_FUSED || state == EMPTY) {
            value = null;
            lazySet(CONSUMED);
            actual.onError(t);
        }
    }
    
    @Override
    public void onComplete() {
        if (hasValue) {
            complete(value);
        } else {
            int state = get();
            if (state == NOT_FUSED || state == EMPTY) {
                lazySet(CONSUMED);
                actual.onComplete();
            }
        }
    }
    
    protected final void complete(R value) {
        int state = get();
        if (state == READY || state == CONSUMED || disposed) {
            return;
        }
        if (state == EMPTY) {
            this.value = value;
            lazySet(READY);
        } else {
            lazySet(CONSUMED);
        }
        actual.onNext(value);
        if (disposed) {
            return;
        }
        actual.onComplete();
    }
    
    @Override
    public final R poll() {
        if (get() == READY) {
            R v = value;
            value = null;
            lazySet(CONSUMED);
            return v;
        }
        return null;
    }
    
    @Override
    public final boolean isDisposed() {
        return disposed;
    }
    
    @Override
    public final void dispose() {
        disposed = true;
        s.dispose();
    }
    
    @Override
    public final boolean isEmpty() {
        return get() != READY;
    }
    
    @Override
    public final int requestFusion(int mode) {
        if ((mode & ASYNC) != 0) {
            lazySet(EMPTY);
            return ASYNC;
        }
        return NONE;
    }
    
    @Override
    public void clear() {
        value = null;
        lazySet(CONSUMED);
    }
}
