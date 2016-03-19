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

package io.reactivex.internal.subscribers.flowable;

import java.util.concurrent.atomic.AtomicReference;

import org.reactivestreams.*;

import io.reactivex.disposables.Disposable;
import io.reactivex.internal.subscriptions.SubscriptionHelper;

/**
 * An abstract subscription that allows asynchronous cancellation.
 * 
 * @param <T>
 */
public abstract class DisposableSubscriber<T> implements Subscriber<T>, Disposable {
    final AtomicReference<Subscription> s = new AtomicReference<Subscription>();
    
    static final Subscription CANCELLED = new Subscription() {
        @Override
        public void request(long n) {
            
        }
        
        @Override
        public void cancel() {
            
        }
    };
    
    @Override
    public final void onSubscribe(Subscription s) {
        if (!this.s.compareAndSet(null, s)) {
            s.cancel();
            if (this.s.get() != CANCELLED) {
                SubscriptionHelper.reportSubscriptionSet();
            }
            return;
        }
        onStart();
    }
    
    protected final Subscription subscription() {
        return s.get();
    }
    
    protected void onStart() {
        s.get().request(Long.MAX_VALUE);
    }
    
    protected final void request(long n) {
        s.get().request(n);
    }
    
    protected final void cancel() {
        dispose();
    }
    
    public final boolean isDisposed() {
        return s.get() == CANCELLED;
    }
    
    @Override
    public final void dispose() {
        Subscription a = s.get();
        if (a != CANCELLED) {
            a = s.getAndSet(CANCELLED);
            if (a != CANCELLED && a != null) {
                a.cancel();
            }
        }
    }
}
