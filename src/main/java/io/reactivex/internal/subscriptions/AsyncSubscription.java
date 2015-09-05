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

package io.reactivex.internal.subscriptions;

import java.util.concurrent.atomic.*;

import org.reactivestreams.Subscription;

import io.reactivex.disposables.Disposable;
import io.reactivex.internal.util.BackpressureHelper;

/**
 * A subscription implementation that arbitrates exactly one other Subscription and can
 * hold a single disposable resource.
 * 
 * <p>All methods are thread-safe.
 */
public final class AsyncSubscription extends AtomicLong implements Subscription, Disposable {
    /** */
    private static final long serialVersionUID = 7028635084060361255L;
    
    volatile Subscription actual;
    static final AtomicReferenceFieldUpdater<AsyncSubscription, Subscription> ACTUAL =
            AtomicReferenceFieldUpdater.newUpdater(AsyncSubscription.class, Subscription.class, "actual");

    volatile Disposable resource;
    static final AtomicReferenceFieldUpdater<AsyncSubscription, Disposable> RESOURCE =
            AtomicReferenceFieldUpdater.newUpdater(AsyncSubscription.class, Disposable.class, "resource");
    
    static final Subscription CANCELLED = new Subscription() {
        @Override
        public void request(long n) {
            
        }
        @Override
        public void cancel() {
            
        }
        
        @Override
        public String toString() {
            return "AsyncSubscription.CANCELLED";
        }
    };
    
    static final Disposable DISPOSED = new Disposable() {
        @Override
        public void dispose() { 
            
        }
        
        @Override
        public String toString() {
            return "AsyncSubscription.DISPOSED";
        };
    };
    
    public AsyncSubscription() {
        
    }
    
    public AsyncSubscription(Disposable resource) {
        RESOURCE.lazySet(this, resource);
    }
    
    @Override
    public void request(long n) {
        Subscription s = actual;
        if (s != null) {
            s.request(n);
        } else {
            if (SubscriptionHelper.validateRequest(n)) {
                return;
            }
            BackpressureHelper.add(this, n);
            s = actual;
            if (s != null) {
                long mr = getAndSet(0L);
                if (mr != 0L) {
                    s.request(mr);
                }
            }
        }
    }
    
    @Override
    public void cancel() {
        Subscription s = actual;
        if (s != CANCELLED) {
            s = ACTUAL.getAndSet(this, CANCELLED);
            if (s != CANCELLED && s != null) {
                s.cancel();
            }
        }
        
        Disposable d = resource;
        if (d != DISPOSED) {
            d = RESOURCE.getAndSet(this, DISPOSED);
            if (d != CANCELLED && d != null) {
                d.dispose();
            }
        }
    }
    
    @Override
    public void dispose() {
        cancel();
    }
    
    /**
     * Sets a new resource and disposes the currently held resource.
     * @param r the new resource to set
     * @return false if this AyncSubscription has been cancelled/disposed
     * @see #replaceResource(Disposable)
     */
    public boolean setResource(Disposable r) {
        for (;;) {
            Disposable d = resource;
            if (d == DISPOSED) {
                if (r != null) {
                    r.dispose();
                }
                return false;
            }
            if (RESOURCE.compareAndSet(this, d, r)) {
                if (d != null) {
                    d.dispose();
                }
                return true;
            }
        }
    }
    
    /**
     * Replaces the currently held resource with the given new one without disposing the old.
     * @param r the new resource to set
     * @return false if this AsyncSubscription has been cancelled/disposed
     */
    public boolean replaceResource(Disposable r) {
        for (;;) {
            Disposable d = resource;
            if (d == DISPOSED) {
                if (r != null) {
                    r.dispose();
                }
                return false;
            }
            if (RESOURCE.compareAndSet(this, d, r)) {
                return true;
            }
        }
    }
    
    /**
     * Sets the given subscription if there isn't any subscription held.
     * @param s the first and only subscription to set
     * @return false if this AsyncSubscription has been cancelled/disposed
     */
    public boolean setSubscription(Subscription s) {
        for (;;) {
            Subscription a = actual;
            if (a == CANCELLED) {
                s.cancel();
                return false;
            }
            if (a != null) {
                s.cancel();
                SubscriptionHelper.reportSubscriptionSet();
                return true;
            }
            if (ACTUAL.compareAndSet(this, null, s)) {
                long mr = getAndSet(0L);
                if (mr != 0L) {
                    s.request(mr);
                }
                return true;
            }
        }
    }
}
