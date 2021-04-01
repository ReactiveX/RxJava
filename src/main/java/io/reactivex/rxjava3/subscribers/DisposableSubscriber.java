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

package io.reactivex.rxjava3.subscribers;

import java.util.concurrent.atomic.AtomicReference;

import org.reactivestreams.Subscription;

import io.reactivex.rxjava3.core.FlowableSubscriber;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.internal.subscriptions.SubscriptionHelper;
import io.reactivex.rxjava3.internal.util.EndConsumerHelper;

/**
 * An abstract Subscriber that allows asynchronous, external cancellation by implementing {@link Disposable}.
 *
 * <p>All pre-implemented final methods are thread-safe.
 *
 * <p>The default {@link #onStart()} requests {@link Long#MAX_VALUE} by default. Override
 * the method to request a custom <em>positive</em> amount. Use the protected {@link #request(long)}
 * to request more items and {@link #cancel()} to cancel the sequence from within an
 * {@code onNext} implementation.
 *
 * <p>Note that calling {@link #request(long)} from {@link #onStart()} may trigger
 * an immediate, asynchronous emission of data to {@link #onNext(Object)}. Make sure
 * all initialization happens before the call to {@code request()} in {@code onStart()}.
 * Calling {@link #request(long)} inside {@link #onNext(Object)} can happen at any time
 * because by design, {@code onNext} calls from upstream are non-reentrant and non-overlapping.
 *
 * <p>Like all other consumers, {@code DisposableSubscriber} can be subscribed only once.
 * Any subsequent attempt to subscribe it to a new source will yield an
 * {@link IllegalStateException} with message {@code "It is not allowed to subscribe with a(n) <class name> multiple times."}.
 *
 * <p>Implementation of {@link #onStart()}, {@link #onNext(Object)}, {@link #onError(Throwable)}
 * and {@link #onComplete()} are not allowed to throw any unchecked exceptions.
 * If for some reason this can't be avoided, use {@link io.reactivex.rxjava3.core.Flowable#safeSubscribe(org.reactivestreams.Subscriber)}
 * instead of the standard {@code subscribe()} method.
 *
 * <p>Example<pre><code>
 * Disposable d =
 *     Flowable.range(1, 5)
 *     .subscribeWith(new DisposableSubscriber&lt;Integer&gt;() {
 *         &#64;Override public void onStart() {
 *             request(1);
 *         }
 *         &#64;Override public void onNext(Integer t) {
 *             if (t == 3) {
 *                 cancel();
 *             }
 *             System.out.println(t);
 *             request(1);
 *         }
 *         &#64;Override public void onError(Throwable t) {
 *             t.printStackTrace();
 *         }
 *         &#64;Override public void onComplete() {
 *             System.out.println("Done!");
 *         }
 *     });
 * // ...
 * d.dispose();
 * </code></pre>
 * @param <T> the received value type.
 */
public abstract class DisposableSubscriber<T> implements FlowableSubscriber<T>, Disposable {
    final AtomicReference<Subscription> upstream = new AtomicReference<>();

    @Override
    public final void onSubscribe(Subscription s) {
        if (EndConsumerHelper.setOnce(this.upstream, s, getClass())) {
            onStart();
        }
    }

    /**
     * Called once the single upstream {@link Subscription} is set via {@link #onSubscribe(Subscription)}.
     */
    protected void onStart() {
        upstream.get().request(Long.MAX_VALUE);
    }

    /**
     * Requests the specified amount from the upstream if its {@link Subscription} is set via
     * onSubscribe already.
     * <p>Note that calling this method before a {@link Subscription} is set via {@link #onSubscribe(Subscription)}
     * leads to {@link NullPointerException} and meant to be called from inside {@link #onStart()} or
     * {@link #onNext(Object)}.
     * @param n the request amount, positive
     */
    protected final void request(long n) {
        upstream.get().request(n);
    }

    /**
     * Cancels the Subscription set via {@link #onSubscribe(Subscription)} or makes sure a
     * {@link Subscription} set asynchronously (later) is cancelled immediately.
     * <p>This method is thread-safe and can be exposed as a public API.
     */
    protected final void cancel() {
        dispose();
    }

    @Override
    public final boolean isDisposed() {
        return upstream.get() == SubscriptionHelper.CANCELLED;
    }

    @Override
    public final void dispose() {
        SubscriptionHelper.cancel(upstream);
    }
}
