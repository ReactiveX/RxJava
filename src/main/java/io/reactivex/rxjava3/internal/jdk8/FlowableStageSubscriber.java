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
package io.reactivex.rxjava3.internal.jdk8;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import org.reactivestreams.Subscription;

import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.FlowableSubscriber;
import io.reactivex.rxjava3.internal.subscriptions.SubscriptionHelper;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;

/**
 * Base class that extends CompletableFuture and provides basic infrastructure
 * to notify watchers upon upstream signals.
 * @param <T> the element type
 * @since 3.0.0
 */
abstract class FlowableStageSubscriber<T> extends CompletableFuture<T> implements FlowableSubscriber<T> {

    final AtomicReference<Subscription> upstream = new AtomicReference<>();

    T value;

    @Override
    public final void onSubscribe(@NonNull Subscription s) {
        if (SubscriptionHelper.setOnce(upstream, s)) {
            afterSubscribe(s);
        }
    }

    protected abstract void afterSubscribe(Subscription s);

    @Override
    public final void onError(Throwable t) {
        clear();
        if (!completeExceptionally(t)) {
            RxJavaPlugins.onError(t);
        }
    }

    protected final void cancelUpstream() {
        SubscriptionHelper.cancel(upstream);
    }

    protected final void clear() {
        value = null;
        upstream.lazySet(SubscriptionHelper.CANCELLED);
    }

    @Override
    public final boolean cancel(boolean mayInterruptIfRunning) {
        cancelUpstream();
        return super.cancel(mayInterruptIfRunning);
    }

    @Override
    public final boolean complete(T value) {
        cancelUpstream();
        return super.complete(value);
    }

    @Override
    public final boolean completeExceptionally(Throwable ex) {
        cancelUpstream();
        return super.completeExceptionally(ex);
    }
}
