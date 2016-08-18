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

import java.util.concurrent.atomic.AtomicReference;

import org.reactivestreams.*;

import io.reactivex.disposables.Disposable;
import io.reactivex.internal.subscriptions.SubscriptionHelper;

/**
 * An abstract Subscriber that allows asynchronous cancellation by implementing Disposable.
 * 
 * @param <T> the received value type.
 */
public abstract class DisposableSubscriber<T> implements Subscriber<T>, Disposable {
    final AtomicReference<Subscription> s = new AtomicReference<Subscription>();

    @Override
    public final void onSubscribe(Subscription s) {
        if (SubscriptionHelper.setOnce(this.s, s)) {
            onStart();
        }
    }
    
    /**
     * Returns the current Subscription sent to this Subscriber via onSubscribe().
     * @return the current Subscription, may be null
     */
    protected final Subscription subscription() {
        return s.get();
    }
    
    /**
     * Called once the single upstream Subscription is set via onSubscribe.
     */
    protected void onStart() {
        s.get().request(Long.MAX_VALUE);
    }
    
    /**
     * Requests the specified amount from the upstream if its Subscription is set via
     * onSubscribe already.
     * <p>Note that calling this method before a Subscription is set via onSubscribe
     * leads to NullPointerException and meant to be called from inside onStart or
     * onNext.
     * @param n the request amount, positive
     */
    protected final void request(long n) {
        s.get().request(n);
    }
    
    /**
     * Cancels the Subscription set via onSubscribe or makes sure a
     * Subscription set asynchronously (later) is cancelled immediately.
     * <p>This method is thread-safe and can be exposed as a public API.
     */
    protected final void cancel() {
        dispose();
    }
    
    @Override
    public final boolean isDisposed() {
        return s.get() == SubscriptionHelper.CANCELLED;
    }
    
    @Override
    public final void dispose() {
        SubscriptionHelper.cancel(s);
    }
}
