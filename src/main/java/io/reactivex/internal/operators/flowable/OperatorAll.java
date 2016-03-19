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
import io.reactivex.functions.Predicate;
import io.reactivex.internal.subscriptions.SubscriptionHelper;
import io.reactivex.plugins.RxJavaPlugins;

public final class OperatorAll<T> implements Operator<Boolean, T> {
    final Predicate<? super T> predicate;
    public OperatorAll(Predicate<? super T> predicate) {
        this.predicate = predicate;
    }
    
    @Override
    public Subscriber<? super T> apply(Subscriber<? super Boolean> t) {
        return new AllSubscriber<T>(t, predicate);
    }
    
    static final class AllSubscriber<T> extends AtomicInteger implements Subscriber<T>, Subscription {
        /** */
        private static final long serialVersionUID = -3521127104134758517L;
        final Subscriber<? super Boolean> actual;
        final Predicate<? super T> predicate;
        
        Subscription s;
        
        boolean done;
        
        static final int NO_REQUEST_NO_VALUE = 0;
        static final int NO_REQUEST_HAS_VALUE = 1;
        static final int HAS_REQUEST_NO_VALUE = 2;
        static final int HAS_REQUEST_HAS_VALUE = 3;
        
        public AllSubscriber(Subscriber<? super Boolean> actual, Predicate<? super T> predicate) {
            this.actual = actual;
            this.predicate = predicate;
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
            if (done) {
                return;
            }
            boolean b;
            try {
                b = predicate.test(t);
            } catch (Throwable e) {
                lazySet(HAS_REQUEST_HAS_VALUE);
                done = true;
                s.cancel();
                actual.onError(e);
                return;
            }
            if (!b) {
                lazySet(HAS_REQUEST_HAS_VALUE);
                done = true;
                s.cancel();
                actual.onNext(false);
                actual.onComplete();
            }
        }
        
        @Override
        public void onError(Throwable t) {
            if (done) {
                RxJavaPlugins.onError(t);
                return;
            }
            done = true;
            actual.onError(t);
        }
        
        @Override
        public void onComplete() {
            if (done) {
                return;
            }
            done = true;

            for (;;) {
                int state = get();
                if (state == NO_REQUEST_HAS_VALUE || state == HAS_REQUEST_HAS_VALUE) {
                    break;
                }
                if (state == HAS_REQUEST_NO_VALUE) {
                    if (compareAndSet(HAS_REQUEST_NO_VALUE, HAS_REQUEST_HAS_VALUE)) {
                        actual.onNext(true);
                        actual.onComplete();
                    }
                    break;
                }
                if (compareAndSet(NO_REQUEST_NO_VALUE, NO_REQUEST_HAS_VALUE)) {
                    break;
                }
            }
        }
        
        @Override
        public void request(long n) {
            if (SubscriptionHelper.validateRequest(n)) {
                return;
            }
            
            for (;;) {
                int state = get();
                if (state == HAS_REQUEST_NO_VALUE || state == HAS_REQUEST_HAS_VALUE) {
                    break;
                }
                if (state == NO_REQUEST_HAS_VALUE) {
                    if (compareAndSet(state, HAS_REQUEST_HAS_VALUE)) {
                        actual.onNext(true);
                        actual.onComplete();
                    }
                    break;
                }
                if (compareAndSet(NO_REQUEST_NO_VALUE, HAS_REQUEST_NO_VALUE)) {
                    s.request(Long.MAX_VALUE);
                    break;
                }
            }
        }
        
        @Override
        public void cancel() {
            lazySet(HAS_REQUEST_HAS_VALUE);
            s.cancel();
        }
    }
}
