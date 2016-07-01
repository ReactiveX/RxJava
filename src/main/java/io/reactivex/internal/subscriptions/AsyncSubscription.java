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

import io.reactivex.internal.disposables.DisposableHelper;
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
    
    final AtomicReference<Subscription> actual;

    final AtomicReference<Disposable> resource;

    public AsyncSubscription() {
        resource = new AtomicReference<Disposable>();
        actual = new AtomicReference<Subscription>();
    }
    
    public AsyncSubscription(Disposable resource) {
        this();
        this.resource.lazySet(resource);
    }
    
    @Override
    public void request(long n) {
        Subscription s = actual.get();
        if (s != null) {
            s.request(n);
        } else if (SubscriptionHelper.validate(n)) {
            BackpressureHelper.add(this, n);
            s = actual.get();
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
        dispose();
    }
    
    @Override
    public void dispose() {
        SubscriptionHelper.dispose(actual);
        DisposableHelper.dispose(resource);
    }

    @Override
    public boolean isDisposed() {
        return actual.get() == SubscriptionHelper.CANCELLED;
    }

    /**
     * Sets a new resource and disposes the currently held resource.
     * @param r the new resource to set
     * @return false if this AyncSubscription has been cancelled/disposed
     * @see #replaceResource(Disposable)
     */
    public boolean setResource(Disposable r) {
        return DisposableHelper.set(resource, r);
    }
    
    /**
     * Replaces the currently held resource with the given new one without disposing the old.
     * @param r the new resource to set
     * @return false if this AsyncSubscription has been cancelled/disposed
     */
    public boolean replaceResource(Disposable r) {
        return DisposableHelper.replace(resource, r);
    }
    
    /**
     * Sets the given subscription if there isn't any subscription held.
     * @param s the first and only subscription to set
     * @return false if this AsyncSubscription has been cancelled/disposed
     */
    public boolean setSubscription(Subscription s) {
        for (;;) {
            Subscription a = actual.get();
            if (a == SubscriptionHelper.CANCELLED) {
                s.cancel();
                return false;
            }
            if (a != null) {
                s.cancel();
                SubscriptionHelper.reportSubscriptionSet();
                return true;
            }
            if (actual.compareAndSet(null, s)) {
                long mr = getAndSet(0L);
                if (mr != 0L) {
                    s.request(mr);
                }
                return true;
            }
        }
    }
}
