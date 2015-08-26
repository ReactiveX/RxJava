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

package io.reactivex.internal.operators;

import java.util.concurrent.atomic.*;
import java.util.function.Consumer;

import org.reactivestreams.*;

import io.reactivex.disposables.Disposable;
import io.reactivex.plugins.RxJavaPlugins;

public final class SubscriberResourceWrapper<T, R> extends AtomicReference<Object> implements Subscriber<T>, Disposable, Subscription {
    /** */
    private static final long serialVersionUID = -8612022020200669122L;

    final Subscriber<? super T> actual;
    final Consumer<? super R> disposer;
    
    volatile Subscription subscription;
    @SuppressWarnings("rawtypes")
    static final AtomicReferenceFieldUpdater<SubscriberResourceWrapper, Subscription> SUBSCRIPTION =
            AtomicReferenceFieldUpdater.newUpdater(SubscriberResourceWrapper.class, Subscription.class, "subscription");
    
    static final Subscription TERMINATED = new Subscription() {
        @Override
        public void request(long n) {
            if (n <= 0) {
                RxJavaPlugins.onError(new IllegalArgumentException("n > 0 required but it was " + n));
            }
        }
        
        @Override
        public void cancel() {
            
        }
    };
    
    public SubscriberResourceWrapper(Subscriber<? super T> actual, Consumer<? super R> disposer) {
        this.actual = actual;
        this.disposer = disposer;
    }
    
    @Override
    public void onSubscribe(Subscription s) {
        for (;;) {
            Subscription current = subscription;
            if (current == TERMINATED) {
                s.cancel();
                return;
            }
            if (current != null) {
                s.cancel();
                RxJavaPlugins.onError(new IllegalStateException("Subscription already set!"));
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
    public void request(long n) {
        if (n <= 0) {
            RxJavaPlugins.onError(new IllegalArgumentException("n > 0 required but it was " + n));
            return;
        }
        subscription.request(n);
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public void dispose() {
        Subscription s = subscription;
        if (s != TERMINATED) {
            s = SUBSCRIPTION.getAndSet(this, TERMINATED);
            if (s != TERMINATED && s != null) {
                s.cancel();
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
    
    @Override
    public void cancel() {
        dispose();
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
