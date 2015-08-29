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

import java.util.function.*;

import org.reactivestreams.*;

import io.reactivex.Observable.Operator;
import io.reactivex.internal.subscriptions.SubscriptionArbiter;

public final class OperatorOnErrorNext<T> implements Operator<T, T> {
    final Function<? super Throwable, ? extends Publisher<? extends T>> nextSupplier;
    final boolean allowFatal;
    
    public OperatorOnErrorNext(Function<? super Throwable, ? extends Publisher<? extends T>> nextSupplier, boolean allowFatal) {
        this.nextSupplier = nextSupplier;
        this.allowFatal = allowFatal;
    }
    
    @Override
    public Subscriber<? super T> apply(Subscriber<? super T> t) {
        OnErrorNextSubscriber<T> parent = new OnErrorNextSubscriber<>(t, nextSupplier, allowFatal);
        t.onSubscribe(parent.arbiter);
        return parent;
    }
    
    static final class OnErrorNextSubscriber<T> implements Subscriber<T> {
        final Subscriber<? super T> actual;
        final Function<? super Throwable, ? extends Publisher<? extends T>> nextSupplier;
        final boolean allowFatal;
        final SubscriptionArbiter arbiter;
        
        boolean once;
        
        public OnErrorNextSubscriber(Subscriber<? super T> actual, Function<? super Throwable, ? extends Publisher<? extends T>> nextSupplier, boolean allowFatal) {
            this.actual = actual;
            this.nextSupplier = nextSupplier;
            this.allowFatal = allowFatal;
            this.arbiter = new SubscriptionArbiter();
        }
        
        @Override
        public void onSubscribe(Subscription s) {
            arbiter.setSubscription(s);
        }
        
        @Override
        public void onNext(T t) {
            actual.onNext(t);
            if (!once) {
                arbiter.produced(1L);
            }
        }
        
        @Override
        public void onError(Throwable t) {
            if (once) {
                actual.onError(t);
                return;
            }
            once = true;

            if (allowFatal && !(t instanceof Exception)) {
                actual.onError(t);
                return;
            }
            
            Publisher<? extends T> p;
            
            try {
                p = nextSupplier.apply(t);
            } catch (Throwable e) {
                t.addSuppressed(e);
                actual.onError(t);
                return;
            }
            
            if (p == null) {
                NullPointerException npe = new NullPointerException("Publisher is null");
                t.addSuppressed(npe);
                actual.onError(t);
                return;
            }
            
            p.subscribe(this);
        }
        
        @Override
        public void onComplete() {
            once = true;
            actual.onComplete();
        }
    }
}
