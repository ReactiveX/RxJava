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

package io.reactivex.internal.operators.flowable;

import org.reactivestreams.*;

import io.reactivex.exceptions.Exceptions;
import io.reactivex.functions.BiFunction;
import io.reactivex.internal.subscriptions.SubscriptionHelper;

public final class FlowableScan<T> extends AbstractFlowableWithUpstream<T, T> {
    final BiFunction<T, T, T> accumulator;
    public FlowableScan(Publisher<T> source, BiFunction<T, T, T> accumulator) {
        super(source);
        this.accumulator = accumulator;
    }
    
    @Override
    protected void subscribeActual(Subscriber<? super T> s) {
        source.subscribe(new ScanSubscriber<T>(s, accumulator));
    }
    
    static final class ScanSubscriber<T> implements Subscriber<T>, Subscription {
        final Subscriber<? super T> actual;
        final BiFunction<T, T, T> accumulator;
        
        Subscription s;
        
        T value;

        public ScanSubscriber(Subscriber<? super T> actual, BiFunction<T, T, T> accumulator) {
            this.actual = actual;
            this.accumulator = accumulator;
        }
        
        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.validate(this.s, s)) {
                this.s = s;
                actual.onSubscribe(this);
            }
        }
        
        @Override
        public void onNext(T t) {
            final Subscriber<? super T> a = actual;
            T v = value;
            if (v == null) {
                value = t;
                a.onNext(t);
            } else {
                T u;
                
                try {
                    u = accumulator.apply(v, t);
                } catch (Throwable e) {
                    Exceptions.throwIfFatal(e);
                    s.cancel();
                    a.onError(e);
                    return;
                }
                
                if (u == null) {
                    s.cancel();
                    a.onError(new NullPointerException("The value returned by the accumulator is null"));
                    return;
                }
                
                value = u;
                a.onNext(u);
            }
        }
        
        @Override
        public void onError(Throwable t) {
            actual.onError(t);
        }
        
        @Override
        public void onComplete() {
            actual.onComplete();
        }
        
        @Override
        public void request(long n) {
            s.request(n);
        }
        
        @Override
        public void cancel() {
            s.cancel();
        }
    }
}
