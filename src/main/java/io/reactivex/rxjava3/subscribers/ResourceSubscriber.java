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

import java.util.Objects;
import java.util.concurrent.atomic.*;

import org.reactivestreams.Subscription;

import io.reactivex.rxjava3.core.FlowableSubscriber;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.internal.disposables.ListCompositeDisposable;
import io.reactivex.rxjava3.internal.subscriptions.SubscriptionHelper;
import io.reactivex.rxjava3.internal.util.EndConsumerHelper;

/**
 * An abstract Subscriber that allows asynchronous cancellation of its
 * subscription and associated resources.
 *
 * <p>All pre-implemented final methods are thread-safe.
 *
 * <p>To release the associated resources, one has to call {@link #dispose()}
 * in {@code onError()} and {@code onComplete()} explicitly.
 *
 * <p>Use {@link #add(Disposable)} to associate resources (as {@link io.reactivex.rxjava3.disposables.Disposable Disposable}s)
 * with this {@code ResourceSubscriber} that will be cleaned up when {@link #dispose()} is called.
 * Removing previously associated resources is not possible but one can create a
 * {@link io.reactivex.rxjava3.disposables.CompositeDisposable CompositeDisposable}, associate it with this
 * {@code ResourceSubscriber} and then add/remove resources to/from the {@code CompositeDisposable}
 * freely.
 *
 * <p>The default {@link #onStart()} requests {@link Long#MAX_VALUE} by default. Override
 * the method to request a custom <em>positive</em> amount. Use the protected {@link #request(long)}
 * to request more items and {@link #dispose()} to cancel the sequence from within an
 * {@code onNext} implementation.
 *
 * <p>Note that calling {@link #request(long)} from {@link #onStart()} may trigger
 * an immediate, asynchronous emission of data to {@link #onNext(Object)}. Make sure
 * all initialization happens before the call to {@code request()} in {@code onStart()}.
 * Calling {@link #request(long)} inside {@link #onNext(Object)} can happen at any time
 * because by design, {@code onNext} calls from upstream are non-reentrant and non-overlapping.
 *
 * <p>Like all other consumers, {@code ResourceSubscriber} can be subscribed only once.
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
 *     .subscribeWith(new ResourceSubscriber&lt;Integer&gt;() {
 *         &#64;Override public void onStart() {
 *             add(Schedulers.single()
 *                 .scheduleDirect(() -&gt; System.out.println("Time!"),
 *                     2, TimeUnit.SECONDS));
 *             request(1);
 *         }
 *         &#64;Override public void onNext(Integer t) {
 *             if (t == 3) {
 *                 dispose();
 *             }
 *             System.out.println(t);
 *             request(1);
 *         }
 *         &#64;Override public void onError(Throwable t) {
 *             t.printStackTrace();
 *             dispose();
 *         }
 *         &#64;Override public void onComplete() {
 *             System.out.println("Done!");
 *             dispose();
 *         }
 *     });
 * // ...
 * d.dispose();
 * </code></pre>
 *
 * @param <T> the value type
 */
public abstract class ResourceSubscriber<T> implements FlowableSubscriber<T>, Disposable {
    /** The active subscription. */
    private final AtomicReference<Subscription> upstream = new AtomicReference<>();

    /** The resource composite, can never be null. */
    private final ListCompositeDisposable resources = new ListCompositeDisposable();

    /** Remembers the request(n) counts until a subscription arrives. */
    private final AtomicLong missedRequested = new AtomicLong();

    /**
     * Adds a resource to this {@code ResourceSubscriber}.
     *
     * @param resource the resource to add
     *
     * @throws NullPointerException if {@code resource} is {@code null}
     */
    public final void add(Disposable resource) {
        Objects.requireNonNull(resource, "resource is null");
        resources.add(resource);
    }

    @Override
    public final void onSubscribe(Subscription s) {
        if (EndConsumerHelper.setOnce(this.upstream, s, getClass())) {
            long r = missedRequested.getAndSet(0L);
            if (r != 0L) {
                s.request(r);
            }
            onStart();
        }
    }

    /**
     * Called once the upstream sets a {@link Subscription} on this {@code ResourceSubscriber}.
     *
     * <p>You can perform initialization at this moment. The default
     * implementation requests {@link Long#MAX_VALUE} from upstream.
     */
    protected void onStart() {
        request(Long.MAX_VALUE);
    }

    /**
     * Request the specified amount of elements from upstream.
     *
     * <p>This method can be called before the upstream calls {@link #onSubscribe(Subscription)}.
     * When the subscription happens, all missed requests are requested.
     *
     * @param n the request amount, must be positive
     */
    protected final void request(long n) {
        SubscriptionHelper.deferredRequest(upstream, missedRequested, n);
    }

    /**
     * Cancels the subscription (if any) and disposes the resources associated with
     * this {@code ResourceSubscriber} (if any).
     *
     * <p>This method can be called before the upstream calls {@link #onSubscribe(Subscription)} at which
     * case the {@link Subscription} will be immediately cancelled.
     */
    @Override
    public final void dispose() {
        if (SubscriptionHelper.cancel(upstream)) {
            resources.dispose();
        }
    }

    /**
     * Returns true if this {@code ResourceSubscriber} has been disposed/cancelled.
     * @return true if this {@code ResourceSubscriber} has been disposed/cancelled
     */
    @Override
    public final boolean isDisposed() {
        return upstream.get() == SubscriptionHelper.CANCELLED;
    }
}
