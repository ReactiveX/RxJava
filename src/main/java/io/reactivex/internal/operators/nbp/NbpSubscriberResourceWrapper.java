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

package io.reactivex.internal.operators.nbp;

import java.util.concurrent.atomic.*;
import java.util.function.Consumer;

import io.reactivex.NbpObservable.NbpSubscriber;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.subscriptions.SubscriptionHelper;

public final class NbpSubscriberResourceWrapper<T, R> extends AtomicReference<Object> implements NbpSubscriber<T>, Disposable {
    /** */
    private static final long serialVersionUID = -8612022020200669122L;

    final NbpSubscriber<? super T> actual;
    final Consumer<? super R> disposer;
    
    volatile Disposable subscription;
    @SuppressWarnings("rawtypes")
    static final AtomicReferenceFieldUpdater<NbpSubscriberResourceWrapper, Disposable> SUBSCRIPTION =
            AtomicReferenceFieldUpdater.newUpdater(NbpSubscriberResourceWrapper.class, Disposable.class, "subscription");
    
    static final Disposable TERMINATED = () -> { };
    
    public NbpSubscriberResourceWrapper(NbpSubscriber<? super T> actual, Consumer<? super R> disposer) {
        this.actual = actual;
        this.disposer = disposer;
    }
    
    @Override
    public void onSubscribe(Disposable s) {
        for (;;) {
            Disposable current = subscription;
            if (current == TERMINATED) {
                s.dispose();
                return;
            }
            if (current != null) {
                s.dispose();
                SubscriptionHelper.reportDisposableSet();
                return;
            }
            if (SUBSCRIPTION.compareAndSet(this, null, s)) {
                actual.onSubscribe(this);
                return;
            }
        }
    }
    
    @Override
    public void onNext(T t) {
        actual.onNext(t);
    }
    
    @Override
    public void onError(Throwable t) {
        dispose();
        actual.onError(t);
    }
    
    @Override
    public void onComplete() {
        dispose();
        actual.onComplete();
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public void dispose() {
        Disposable s = subscription;
        if (s != TERMINATED) {
            s = SUBSCRIPTION.getAndSet(this, TERMINATED);
            if (s != TERMINATED && s != null) {
                s.dispose();
            }
        }
        
        Object o = get();
        if (o != TERMINATED) {
            o = getAndSet(TERMINATED);
            if (o != TERMINATED && o != null) {
                disposer.accept((R)o);
            }
        }
    }
    
    @SuppressWarnings("unchecked")
    public void setResource(R resource) {
        for (;;) {
            Object r = get();
            if (r == TERMINATED) {
                disposer.accept(resource);
                return;
            }
            if (compareAndSet(r, resource)) {
                if (r != null) {
                    disposer.accept((R)r);
                }
                return;
            }
        }
    }
}
