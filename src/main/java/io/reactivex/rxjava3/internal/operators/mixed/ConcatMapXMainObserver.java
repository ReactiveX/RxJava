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

package io.reactivex.rxjava3.internal.operators.mixed;

import java.util.concurrent.atomic.AtomicInteger;

import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.internal.disposables.DisposableHelper;
import io.reactivex.rxjava3.internal.util.*;
import io.reactivex.rxjava3.operators.QueueDisposable;
import io.reactivex.rxjava3.operators.QueueFuseable;
import io.reactivex.rxjava3.operators.SimpleQueue;
import io.reactivex.rxjava3.operators.SpscLinkedArrayQueue;

/**
 * Base class for implementing concatMapX main observers.
 *
 * @param <T> the upstream value type
 * @since 3.0.10
 */
public abstract class ConcatMapXMainObserver<T> extends AtomicInteger
implements Observer<T>, Disposable {

    private static final long serialVersionUID = -3214213361171757852L;

    final AtomicThrowable errors;

    final int prefetch;

    final ErrorMode errorMode;

    SimpleQueue<T> queue;

    Disposable upstream;

    volatile boolean done;

    volatile boolean disposed;

    public ConcatMapXMainObserver(int prefetch, ErrorMode errorMode) {
        this.errorMode = errorMode;
        this.errors = new AtomicThrowable();
        this.prefetch = prefetch;
    }

    @Override
    public final void onSubscribe(Disposable d) {
        if (DisposableHelper.validate(upstream, d)) {
            upstream = d;
            if (d instanceof QueueDisposable) {
                @SuppressWarnings("unchecked")
                QueueDisposable<T> qd = (QueueDisposable<T>)d;
                int mode = qd.requestFusion(QueueFuseable.ANY | QueueFuseable.BOUNDARY);
                if (mode == QueueFuseable.SYNC) {
                    queue = qd;
                    done = true;

                    onSubscribeDownstream();

                    drain();
                    return;
                }
                else if (mode == QueueFuseable.ASYNC) {
                    queue = qd;

                    onSubscribeDownstream();

                    return;
                }
            }

            queue = new SpscLinkedArrayQueue<>(prefetch);
            onSubscribeDownstream();
        }
    }

    @Override
    public final void onNext(T t) {
        // In async fusion mode, t is a drain indicator
        if (t != null) {
            queue.offer(t);
        }
        drain();
    }

    @Override
    public final void onError(Throwable t) {
        if (errors.tryAddThrowableOrReport(t)) {
            if (errorMode == ErrorMode.IMMEDIATE) {
                disposeInner();
            }
            done = true;
            drain();
        }
    }

    @Override
    public final void onComplete() {
        done = true;
        drain();
    }

    @Override
    public final void dispose() {
        disposed = true;
        upstream.dispose();
        disposeInner();
        errors.tryTerminateAndReport();
        if (getAndIncrement() == 0) {
            queue.clear();
            clearValue();
        }
    }

    @Override
    public final boolean isDisposed() {
        return disposed;
    }

    /**
     * Override this to clear values when the downstream disposes.
     */
    void clearValue() {
    }

    /**
     * Typically, this should be {@code downstream.onSubscribe(this)}.
     */
    abstract void onSubscribeDownstream();

    /**
     * Typically, this should be {@code inner.dispose()}.
     */
    abstract void disposeInner();

    /**
     * Implement the serialized inner subscribing and value emission here.
     */
    abstract void drain();
}
