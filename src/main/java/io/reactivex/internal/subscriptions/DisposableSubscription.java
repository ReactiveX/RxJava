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

package io.reactivex.internal.subscriptions;

import java.util.concurrent.atomic.AtomicReference;

import org.reactivestreams.Subscription;

import io.reactivex.disposables.Disposable;

/**
 * Subscription that ignores (but validates) requests and delegates cancel to a disposable instance.
 */
public final class DisposableSubscription extends AtomicReference<Disposable> implements Subscription {
    /** */
    private static final long serialVersionUID = -2358839743797425727L;
    /** The disposed state indicator. */
    static final Disposable DISPOSED = new Disposable() {
        @Override
        public void dispose() { }
    };
    
    /**
     * Constructs an empty DisposableSubscription.
     */
    public DisposableSubscription() {
    }

    /**
     * Constructs a DisposableSubscription by wrapping the given Disposable.
     * @param d the disposable to wrap, may be null
     */
    public DisposableSubscription(Disposable d) {
        this.lazySet(d);
    }
    
    @Override
    public void request(long n) {
        if (SubscriptionHelper.validateRequest(n)) {
            return;
        }
    }
    
    /**
     * Sets a new disposable resource and disposes any old one.
     * @param d the new disposable to set
     * @return false if this Subscription has been cancelled
     */
    public boolean setDisposable(Disposable d) {
        for (;;) {
            Disposable a = get();
            if (a == DISPOSED) {
                if (d != null) {
                    d.dispose();
                }
                return false;
            }
            if (compareAndSet(a, d)) {
                if (a != null) {
                    a.dispose();
                }
                return true;
            }
        }
    }
    
    /**
     * Replaces any existing disposable with a new disposable but doesn't dispose the old one.
     * @param d the new disposable to set
     * @return false if this disposable was disposed
     */
    public boolean replaceDisposable(Disposable d) {
        for (;;) {
            Disposable a = get();
            if (a == DISPOSED) {
                if (d != null) {
                    d.dispose();
                }
                return false;
            }
            if (compareAndSet(a, d)) {
                return true;
            }
        }
    }
    
    @Override
    public void cancel() {
        Disposable d = get();
        if (d != DISPOSED) {
            d = getAndSet(DISPOSED);
            if (d != DISPOSED && d != null) {
                d.dispose();
            }
        }
    }
}