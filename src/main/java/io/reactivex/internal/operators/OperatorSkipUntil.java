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

import java.util.concurrent.atomic.AtomicBoolean;

import org.reactivestreams.*;

import io.reactivex.Observable.Operator;
import io.reactivex.internal.disposables.ArrayCompositeResource;
import io.reactivex.internal.subscriptions.*;
import io.reactivex.subscribers.SerializedSubscriber;

public final class OperatorSkipUntil<T, U> implements Operator<T, T> {
    final Publisher<U> other;
    public OperatorSkipUntil(Publisher<U> other) {
        this.other = other;
    }
    
    @Override
    public Subscriber<? super T> apply(Subscriber<? super T> child) {
        
        SerializedSubscriber<T> serial = new SerializedSubscriber<>(child);
        
        ArrayCompositeResource<Subscription> frc = new ArrayCompositeResource<>(2, Subscription::cancel);
        
        SkipUntilSubscriber<T> sus = new SkipUntilSubscriber<>(serial, frc);
        
        other.subscribe(new Subscriber<U>() {
            Subscription s;
            @Override
            public void onSubscribe(Subscription s) {
                if (SubscriptionHelper.validateSubscription(this.s, s)) {
                    return;
                }
                this.s = s;
                if (frc.setResource(1, s)) {
                    s.request(Long.MAX_VALUE);
                }
            }
            
            @Override
            public void onNext(U t) {
                s.cancel();
                sus.notSkipping = true;
            }
            
            @Override
            public void onError(Throwable t) {
                frc.dispose();
                // in case the other emits an onError before the main even sets a subscription
                if (sus.compareAndSet(false, true)) {
                    EmptySubscription.error(t, serial);
                } else {
                    serial.onError(t);
                }
            }
            
            @Override
            public void onComplete() {
                sus.notSkipping = true;
            }
        });
        
        return sus;
    }
    
    static final class SkipUntilSubscriber<T> extends AtomicBoolean implements Subscriber<T>, Subscription {
        /** */
        private static final long serialVersionUID = -1113667257122396604L;
        final Subscriber<? super T> actual;
        final ArrayCompositeResource<Subscription> frc;
        
        Subscription s;
        
        volatile boolean notSkipping;
        boolean notSkippingLocal;

        public SkipUntilSubscriber(Subscriber<? super T> actual, ArrayCompositeResource<Subscription> frc) {
            this.actual = actual;
            this.frc = frc;
        }
        
        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.validateSubscription(this.s, s)) {
                return;
            }
            this.s = s;
            if (frc.setResource(0, s)) {
                if (compareAndSet(false, true)) {
                    actual.onSubscribe(this);
                }
            }
        }
        
        @Override
        public void onNext(T t) {
            if (notSkippingLocal) {
                actual.onNext(t);
            } else
            if (notSkipping) {
                notSkippingLocal = true;
                actual.onNext(t);
            } else {
                s.request(1);
            }
        }
        
        @Override
        public void onError(Throwable t) {
            frc.dispose();
            actual.onError(t);
        }
        
        @Override
        public void onComplete() {
            frc.dispose();
            actual.onComplete();
        }
        
        @Override
        public void request(long n) {
            s.request(n);
        }
        
        @Override
        public void cancel() {
            frc.dispose();
        }
    }
}
