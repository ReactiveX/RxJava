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

import org.reactivestreams.*;

import io.reactivex.Observable.Operator;
import io.reactivex.internal.disposables.ArrayCompositeResource;
import io.reactivex.subscribers.SerializedSubscriber;

/**
 * 
 */
public final class OperatorTakeUntil<T, U> implements Operator<T, T> {
    final Publisher<? extends U> other;
    public OperatorTakeUntil(Publisher<? extends U> other) {
        this.other = other;
    }
    @Override
    public Subscriber<? super T> apply(Subscriber<? super T> child) {
        SerializedSubscriber<T> serial = new SerializedSubscriber<>(child);
        
        ArrayCompositeResource<Subscription> frc = new ArrayCompositeResource<>(2, Subscription::cancel);
        
        TakeUntilSubscriber<T> tus = new TakeUntilSubscriber<>(serial, frc); 
        
        other.subscribe(new Subscriber<U>() {
            @Override
            public void onSubscribe(Subscription s) {
                frc.setResource(0, s);
                s.request(Long.MAX_VALUE);
            }
            @Override
            public void onNext(U t) {
                frc.dispose();
                serial.onComplete();
            }
            @Override
            public void onError(Throwable t) {
                frc.dispose();
                serial.onError(t);
            }
            @Override
            public void onComplete() {
                frc.dispose();
                serial.onComplete();
            }
        });
        
        return tus;
    }
    
    static final class TakeUntilSubscriber<T> implements Subscriber<T>, Subscription {
        final Subscriber<? super T> actual;
        final ArrayCompositeResource<Subscription> frc;
        
        Subscription s;
        
        public TakeUntilSubscriber(Subscriber<? super T> actual, ArrayCompositeResource<Subscription> frc) {
            this.actual = actual;
            this.frc = frc;
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (this.s != null) {
                s.cancel();
                onError(new IllegalStateException("Subscription already set!"));
                return;
            }
            this.s = s;
            frc.setResource(0, s);
            actual.onSubscribe(this);
        }
        
        @Override
        public void onNext(T t) {
            actual.onNext(t);
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
