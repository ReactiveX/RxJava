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

package io.reactivex.internal.subscribers.observable;

import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.subscriptions.SubscriptionHelper;

/**
 * An abstract subscription that allows asynchronous cancellation.
 * 
 * @param <T>
 */
public abstract class NbpDisposableSubscriber<T> implements Observer<T>, Disposable {
    final AtomicReference<Disposable> s = new AtomicReference<Disposable>();
    
    static final Disposable CANCELLED = new Disposable() {
        @Override
        public void dispose() { }
    };
    
    @Override
    public final void onSubscribe(Disposable s) {
        if (!this.s.compareAndSet(null, s)) {
            s.dispose();
            if (this.s.get() != CANCELLED) {
                SubscriptionHelper.reportSubscriptionSet();
            }
            return;
        }
        onStart();
    }
    
    protected void onStart() {
    }
    
    public final boolean isDisposed() {
        return s == CANCELLED;
    }
    
    @Override
    public final void dispose() {
        Disposable a = s.get();
        if (a != CANCELLED) {
            a = s.getAndSet(CANCELLED);
            if (a != CANCELLED && a != null) {
                a.dispose();
            }
        }
    }
}
