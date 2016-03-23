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

import java.util.concurrent.atomic.AtomicInteger;

import org.reactivestreams.*;

import io.reactivex.Flowable.Operator;
import io.reactivex.internal.subscriptions.SubscriptionHelper;

public enum OperatorTakeLastOne implements Operator<Object, Object> {
    INSTANCE
    ;
    
    @SuppressWarnings("unchecked")
    public static <T> Operator<T, T> instance() {
        return (Operator<T, T>)INSTANCE;
    }
    
    @Override
    public Subscriber<? super Object> apply(Subscriber<? super Object> s) {
        return new TakeLastOneSubscriber<Object>(s);
    }
    
    static final class TakeLastOneSubscriber<T> extends AtomicInteger implements Subscriber<T>, Subscription {
        /** */
        private static final long serialVersionUID = -5467847744262967226L;
        final Subscriber<? super T> actual;
        Subscription s;
        
        T value;
        
        static final int NO_REQUEST_NO_VALUE = 0;
        static final int NO_REQUEST_HAS_VALUE = 1;
        static final int HAS_REQUEST_NO_VALUE = 2;
        static final int HAS_REQUEST_HAS_VALUE = 3;

        public TakeLastOneSubscriber(Subscriber<? super T> actual) {
            this.actual = actual;
        }
        
        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.validateSubscription(this.s, s)) {
                return;
            }
            this.s = s;
            actual.onSubscribe(this);
            s.request(Long.MAX_VALUE);
        }
        
        @Override
        public void onNext(T t) {
            value = t;
        }
        
        @Override
        public void onError(Throwable t) {
            value = null;
            getAndSet(HAS_REQUEST_HAS_VALUE);
            actual.onError(t);
        }
        
        @Override
        public void onComplete() {
            for (;;) {
                int s = get();
                if (s == HAS_REQUEST_NO_VALUE) {
                    lazySet(HAS_REQUEST_HAS_VALUE); // this is okay since onComplete is called at most once
                    emit();
                    return;
                }
                if (s == HAS_REQUEST_HAS_VALUE || s == NO_REQUEST_HAS_VALUE) {
                    return;
                }
                if (compareAndSet(NO_REQUEST_NO_VALUE, NO_REQUEST_HAS_VALUE)) {
                    return;
                }
            }
        }
        
        @Override
        public void request(long n) {
            if (SubscriptionHelper.validateRequest(n)) {
                return;
            }
            for (;;) {
                int s = get();
                if (s == NO_REQUEST_HAS_VALUE) {
                    if (compareAndSet(NO_REQUEST_HAS_VALUE, HAS_REQUEST_HAS_VALUE)) {
                        emit();
                        return;
                    }
                } else
                if (s == NO_REQUEST_NO_VALUE) {
                    if (compareAndSet(NO_REQUEST_NO_VALUE, HAS_REQUEST_NO_VALUE)) {
                        return;
                    }
                } else
                if (s == HAS_REQUEST_HAS_VALUE || s == HAS_REQUEST_NO_VALUE) {
                    return;
                }
            }
        }
        
        void emit() {
            T v = value;
            if (v != null) {
                value = null;
                actual.onNext(v);
            }
            actual.onComplete();
        }
        
        @Override
        public void cancel() {
            getAndSet(HAS_REQUEST_HAS_VALUE);
            value = null;
            s.cancel();
        }
    }
}
