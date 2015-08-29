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
import io.reactivex.internal.subscribers.CancelledSubscriber;
import io.reactivex.internal.subscriptions.*;

public final class OperatorCollect<T, U> implements Operator<U, T> {
    final Supplier<? extends U> initialSupplier;
    final BiConsumer<? super U, ? super T> collector;
    
    public OperatorCollect(Supplier<? extends U> initialSupplier, BiConsumer<? super U, ? super T> collector) {
        this.initialSupplier = initialSupplier;
        this.collector = collector;
    }

    @Override
    public Subscriber<? super T> apply(Subscriber<? super U> t) {
        
        U u;
        try {
            u = initialSupplier.get();
        } catch (Throwable e) {
            EmptySubscription.error(e, t);
            return CancelledSubscriber.INSTANCE;
        }
        
        return new CollectSubscriber<>(t, u, collector);
    }
    
    static final class CollectSubscriber<T, U> implements Subscriber<T>, Subscription {
        final Subscriber<? super U> actual;
        final BiConsumer<? super U, ? super T> collector;
        final U u;
        
        Subscription s;
        
        public CollectSubscriber(Subscriber<? super U> actual, U u, BiConsumer<? super U, ? super T> collector) {
            this.actual = actual;
            this.collector = collector;
            this.u = u;
        }
        
        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.validateSubscription(this.s, s)) {
                return;
            }
            this.s = s;
            actual.onSubscribe(this);
        }
        
        @Override
        public void onNext(T t) {
            try {
                collector.accept(u, t);
            } catch (Throwable e) {
                s.cancel();
                actual.onError(e);
            }
        }
        
        @Override
        public void onError(Throwable t) {
            actual.onError(t);
        }
        
        @Override
        public void onComplete() {
            actual.onNext(u);
            actual.onComplete();
        }
        
        @Override
        public void request(long n) {
            if (SubscriptionHelper.validateRequest(n)) {
                return;
            }
            s.request(Long.MAX_VALUE);
        }
        
        @Override
        public void cancel() {
            s.cancel();
        }
    }
}
