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

package io.reactivex.rxjava3.internal.subscribers;

import java.util.concurrent.atomic.*;

import org.reactivestreams.*;

import io.reactivex.rxjava3.core.FlowableSubscriber;
import io.reactivex.rxjava3.internal.subscriptions.SubscriptionHelper;
import io.reactivex.rxjava3.internal.util.*;

/**
 * Ensures that the event flow between the upstream and downstream follow
 * the Reactive-Streams 1.0 specification by honoring the 3 additional rules
 * (which are omitted in standard operators due to performance reasons).
 * <ul>
 * <li>§1.3: onNext should not be called concurrently until onSubscribe returns</li>
 * <li>§2.3: onError or onComplete must not call cancel</li>
 * <li>§3.9: negative requests should emit an onError(IllegalArgumentException)</li>
 * </ul>
 * In addition, if rule §2.12 (onSubscribe must be called at most once) is violated,
 * the sequence is cancelled an onError(IllegalStateException) is emitted.
 * @param <T> the value type
 * @since 2.0.7
 */
public class StrictSubscriber<T>
extends AtomicInteger
implements FlowableSubscriber<T>, Subscription {

    private static final long serialVersionUID = -4945028590049415624L;

    final Subscriber<? super T> downstream;

    final AtomicThrowable error;

    final AtomicLong requested;

    final AtomicReference<Subscription> upstream;

    final AtomicBoolean once;

    volatile boolean done;

    public StrictSubscriber(Subscriber<? super T> downstream) {
        this.downstream = downstream;
        this.error = new AtomicThrowable();
        this.requested = new AtomicLong();
        this.upstream = new AtomicReference<>();
        this.once = new AtomicBoolean();
    }

    @Override
    public void request(long n) {
        if (n <= 0) {
            cancel();
            onError(new IllegalArgumentException("§3.9 violated: positive request amount required but it was " + n));
        } else {
            SubscriptionHelper.deferredRequest(upstream, requested, n);
        }
    }

    @Override
    public void cancel() {
        if (!done) {
            SubscriptionHelper.cancel(upstream);
        }
    }

    @Override
    public void onSubscribe(Subscription s) {
        if (once.compareAndSet(false, true)) {

            downstream.onSubscribe(this);

            SubscriptionHelper.deferredSetOnce(this.upstream, requested, s);
        } else {
            s.cancel();
            cancel();
            onError(new IllegalStateException("§2.12 violated: onSubscribe must be called at most once"));
        }
    }

    @Override
    public void onNext(T t) {
        HalfSerializer.onNext(downstream, t, this, error);
    }

    @Override
    public void onError(Throwable t) {
        done = true;
        HalfSerializer.onError(downstream, t, this, error);
    }

    @Override
    public void onComplete() {
        done = true;
        HalfSerializer.onComplete(downstream, this, error);
    }
}
