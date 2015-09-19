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

package io.reactivex.internal.subscribers.nbp;

import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

import io.reactivex.NbpObservable.NbpSubscriber;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.subscriptions.SubscriptionHelper;

/**
 * An abstract subscription that allows asynchronous cancellation.
 * 
 * @param <T>
 */
public abstract class NbpDisposableSubscriber<T> implements NbpSubscriber<T>, Disposable {
    volatile Disposable s;
    @SuppressWarnings("rawtypes")
    static final AtomicReferenceFieldUpdater<NbpDisposableSubscriber, Disposable> S =
            AtomicReferenceFieldUpdater.newUpdater(NbpDisposableSubscriber.class, Disposable.class, "s");
    
    static final Disposable CANCELLED = () -> { };
    
    @Override
    public final void onSubscribe(Disposable s) {
        if (!S.compareAndSet(this, null, s)) {
            s.dispose();
            if (this.s != CANCELLED) {
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
        Disposable a = s;
        if (a != CANCELLED) {
            a = S.getAndSet(this, CANCELLED);
            if (a != CANCELLED && a != null) {
                a.dispose();
            }
        }
    }
}
