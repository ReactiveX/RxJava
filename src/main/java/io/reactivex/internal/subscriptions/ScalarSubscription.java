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

package io.reactivex.internal.subscriptions;

import java.util.concurrent.atomic.AtomicInteger;

import io.reactivex.annotations.Nullable;
import org.reactivestreams.Subscriber;

import io.reactivex.internal.fuseable.QueueSubscription;

/**
 * A Subscription that holds a constant value and emits it only when requested.
 * @param <T> the value type
 */
public final class ScalarSubscription<T> extends AtomicInteger implements QueueSubscription<T> {

    private static final long serialVersionUID = -3830916580126663321L;
    /** The single value to emit, set to null. */
    final T value;
    /** The actual subscriber. */
    final Subscriber<? super T> subscriber;

    /** No request has been issued yet. */
    static final int NO_REQUEST = 0;
    /** Request has been called.*/
    static final int REQUESTED = 1;
    /** Cancel has been called. */
    static final int CANCELLED = 2;

    public ScalarSubscription(Subscriber<? super T> subscriber, T value) {
        this.subscriber = subscriber;
        this.value = value;
    }

    @Override
    public void request(long n) {
        if (!SubscriptionHelper.validate(n)) {
            return;
        }
        if (compareAndSet(NO_REQUEST, REQUESTED)) {
            Subscriber<? super T> s = subscriber;

            s.onNext(value);
            if (get() != CANCELLED) {
                s.onComplete();
            }
        }

    }

    @Override
    public void cancel() {
        lazySet(CANCELLED);
    }

    /**
     * Returns true if this Subscription was cancelled.
     * @return true if this Subscription was cancelled
     */
    public boolean isCancelled() {
        return get() == CANCELLED;
    }

    @Override
    public boolean offer(T e) {
        throw new UnsupportedOperationException("Should not be called!");
    }

    @Override
    public boolean offer(T v1, T v2) {
        throw new UnsupportedOperationException("Should not be called!");
    }

    @Nullable
    @Override
    public T poll() {
        if (get() == NO_REQUEST) {
            lazySet(REQUESTED);
            return value;
        }
        return null;
    }

    @Override
    public boolean isEmpty() {
        return get() != NO_REQUEST;
    }

    @Override
    public void clear() {
        lazySet(1);
    }

    @Override
    public int requestFusion(int mode) {
        return mode & SYNC;
    }
}
