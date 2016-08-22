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

package io.reactivex.subscribers;

import java.util.concurrent.atomic.*;

import org.reactivestreams.*;

import io.reactivex.disposables.*;
import io.reactivex.internal.functions.ObjectHelper;
import io.reactivex.internal.subscriptions.SubscriptionHelper;
import io.reactivex.internal.util.BackpressureHelper;

/**
 * An abstract Subscriber that allows asynchronous cancellation of its
 * subscription.
 * 
 * <p>This implementation let's you chose if the AsyncObserver manages resources or not,
 * thus saving memory on cases where there is no need for that.
 * 
 * <p>All pre-implemented final methods are thread-safe.
 * 
 * @param <T> the value type
 */
public abstract class ResourceSubscriber<T> implements Subscriber<T>, Disposable {
    /** The active subscription. */
    private final AtomicReference<Subscription> s = new AtomicReference<Subscription>();

    /** The resource composite, can be null. */
    private final CompositeDisposable resources = new CompositeDisposable();
    
    /** Remembers the request(n) counts until a subscription arrives. */
    private final AtomicLong missedRequested = new AtomicLong();

    /**
     * Adds a resource to this AsyncObserver.
     * 
     * @param resource the resource to add
     * 
     * @throws NullPointerException if resource is null
     */
    public final void add(Disposable resource) {
        ObjectHelper.requireNonNull(resource, "resource is null");
        resources.add(resource);
    }
    
    @Override
    public final void onSubscribe(Subscription s) {
        if (SubscriptionHelper.setOnce(this.s, s)) {
            long mr = missedRequested.getAndSet(0L);
            if (mr != 0L) {
                s.request(mr);
            }
            onStart();
        }
    }
    
    /**
     * Called once the upstream sets a Subscription on this AsyncObserver.
     * 
     * <p>You can perform initialization at this moment. The default
     * implementation requests Long.MAX_VALUE from upstream.
     */
    protected void onStart() {
        request(Long.MAX_VALUE);
    }
    
    /**
     * Request the specified amount of elements from upstream.
     * 
     * <p>This method can be called before the upstream calls onSubscribe().
     * When the subscription happens, all missed requests are requested.
     * 
     * @param n the request amount, must be positive
     */
    protected final void request(long n) {
        if (!SubscriptionHelper.validate(n)) {
            return;
        }
        Subscription a = s.get();
        if (a == null) {
            BackpressureHelper.add(missedRequested, n);
            a = s.get();
            if (a != null) {
                long mr = missedRequested.getAndSet(0L);
                if (mr != 0L) {
                    a.request(mr);
                }
            }
        } else {
            a.request(n);
        }
    }
    
    /**
     * Cancels the subscription (if any) and disposes the resources associated with
     * this AsyncObserver (if any).
     * 
     * <p>This method can be called before the upstream calls onSubscribe at which
     * case the Subscription will be immediately cancelled.
     */
    protected final void cancel() {
        if (SubscriptionHelper.cancel(s)) {
            resources.dispose();
        }
    }
    
    @Override
    public final void dispose() {
        cancel();
    }
    
    /**
     * Returns true if this AsyncObserver has been disposed/cancelled.
     * @return true if this AsyncObserver has been disposed/cancelled
     */
    @Override
    public final boolean isDisposed() {
        return SubscriptionHelper.isCancelled(s.get());
    }
}
