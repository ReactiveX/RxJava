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

import org.reactivestreams.*;

import io.reactivex.internal.subscriptions.SubscriptionHelper;

/**
 * Abstract base implementation of an Subscriber with support for requesting via
 * {@link #request(long)}, cancelling via
 * via {@link #cancel()} (both synchronously) and calls {@link #onStart()}
 * when the subscription happens.
 * 
 * @param <T> the value type
 */
public abstract class DefaultSubscriber<T> implements Subscriber<T> {
    private Subscription s;
    @Override
    public final void onSubscribe(Subscription s) {
        if (SubscriptionHelper.validate(this.s, s)) {
            this.s = s;
            onStart();
        }
    }
    
    /**
     * Requests from the upstream Subscription.
     */
    protected final void request(long n) {
        Subscription s = this.s;
        if (s != null) {
            s.request(n);
        }
    }
    
    /**
     * Cancels the upstream's Subscription.
     */
    protected final void cancel() {
        s.cancel();
        s = null;
    }
    /**
     * Called once the subscription has been set on this observer; override this
     * to perform initialization or issue an initial request.
     * <p>
     * The default implementation requests {@link Long#MAX_VALUE}.
     */
    protected void onStart() {
        request(Long.MAX_VALUE);
    }
    
}
