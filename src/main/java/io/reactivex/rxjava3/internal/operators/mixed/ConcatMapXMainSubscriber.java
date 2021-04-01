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

import org.reactivestreams.Subscription;

import io.reactivex.rxjava3.core.FlowableSubscriber;
import io.reactivex.rxjava3.exceptions.MissingBackpressureException;
import io.reactivex.rxjava3.internal.fuseable.*;
import io.reactivex.rxjava3.internal.queue.SpscArrayQueue;
import io.reactivex.rxjava3.internal.subscriptions.SubscriptionHelper;
import io.reactivex.rxjava3.internal.util.*;

/**
 * Base class for implementing concatMapX main subscribers.
 *
 * @param <T> the upstream value type
 * @since 3.0.10
 */
public abstract class ConcatMapXMainSubscriber<T> extends AtomicInteger
implements FlowableSubscriber<T> {

    private static final long serialVersionUID = -3214213361171757852L;

    final AtomicThrowable errors;

    final int prefetch;

    final ErrorMode errorMode;

    SimpleQueue<T> queue;

    Subscription upstream;

    volatile boolean done;

    volatile boolean cancelled;

    boolean syncFused;

    public ConcatMapXMainSubscriber(int prefetch, ErrorMode errorMode) {
        this.errorMode = errorMode;
        this.errors = new AtomicThrowable();
        this.prefetch = prefetch;
    }

    @Override
    public final void onSubscribe(Subscription s) {
        if (SubscriptionHelper.validate(upstream, s)) {
            upstream = s;
            if (s instanceof QueueSubscription) {
                @SuppressWarnings("unchecked")
                QueueSubscription<T> qs = (QueueSubscription<T>)s;
                int mode = qs.requestFusion(QueueFuseable.ANY | QueueFuseable.BOUNDARY);
                if (mode == QueueFuseable.SYNC) {
                    queue = qs;
                    syncFused = true;
                    done = true;

                    onSubscribeDownstream();

                    drain();
                    return;
                }
                else if (mode == QueueFuseable.ASYNC) {
                    queue = qs;

                    onSubscribeDownstream();

                    upstream.request(prefetch);
                    return;
                }
            }

            queue = new SpscArrayQueue<>(prefetch);
            onSubscribeDownstream();
            upstream.request(prefetch);
        }
    }

    @Override
    public final void onNext(T t) {
        // In async fusion mode, t is a drain indicator
        if (t != null) {
            if (!queue.offer(t)) {
                upstream.cancel();
                onError(new MissingBackpressureException("queue full?!"));
                return;
            }
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

    final void stop() {
        cancelled = true;
        upstream.cancel();
        disposeInner();
        errors.tryTerminateAndReport();
        if (getAndIncrement() == 0) {
            queue.clear();
            clearValue();
        }
    }

    /**
     * Override this to clear values when the downstream disposes.
     */
    void clearValue() {
    }

    /**
     * Typically, this should be {@code downstream.onSubscribe(this);}.
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
