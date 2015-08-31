/**
 * Copyright 2015 Netflix, Inc.
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

package io.reactivex;

import org.reactivestreams.*;

import io.reactivex.internal.subscriptions.SubscriptionHelper;

public abstract class Observer<T> implements Subscriber<T> {
    private Subscription s;
    @Override
    public final void onSubscribe(Subscription s) {
        if (SubscriptionHelper.validateSubscription(this.s, s)) {
            return;
        }
        this.s = s;
        onStart();
    }
    
    protected final Subscription subscription() {
        return s;
    }
    
    protected final void request(long n) {
        subscription().request(n);
    }
    
    protected final void cancel() {
        subscription().cancel();
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
