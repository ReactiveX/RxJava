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

import java.util.Optional;
import java.util.concurrent.atomic.*;

import org.reactivestreams.*;

import io.reactivex.*;
import io.reactivex.Observable.Operator;
import io.reactivex.internal.subscriptions.SubscriptionHelper;
import io.reactivex.internal.util.BackpressureHelper;

public enum OperatorMaterialize implements Operator<Try<Optional<Object>>, Object> {
    INSTANCE;
    
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static <T> Operator<Try<Optional<T>>, T> instance() {
        return (Operator)INSTANCE;
    }
    
    @Override
    public Subscriber<? super Object> apply(Subscriber<? super Try<Optional<Object>>> t) {
        return new OnErrorReturnSubscriber<>(t);
    }
    
    static final class OnErrorReturnSubscriber<T> extends AtomicLong implements Subscriber<T>, Subscription {
        /** */
        private static final long serialVersionUID = -3740826063558713822L;
        final Subscriber<? super Try<Optional<T>>> actual;
        
        Subscription s;
        
        volatile int state;
        @SuppressWarnings("rawtypes")
        static final AtomicIntegerFieldUpdater<OnErrorReturnSubscriber> STATE =
                AtomicIntegerFieldUpdater.newUpdater(OnErrorReturnSubscriber.class, "state");
        Try<Optional<T>> value;
        
        volatile boolean done;
        
        static final int NO_REQUEST_NO_VALUE = 0;
        static final int NO_REQUEST_HAS_VALUE = 1;
        static final int HAS_REQUEST_NO_VALUE = 2;
        static final int HAS_REQUEST_HAS_VALUE = 3;
        
        public OnErrorReturnSubscriber(Subscriber<? super Try<Optional<T>>> actual) {
            this.actual = actual;
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
            actual.onNext(Notification.next(t));
            
            if (get() != Long.MAX_VALUE) {
                decrementAndGet();
            }
        }
        
        void tryEmit(Try<Optional<T>> v) {
            if (get() != 0L) {
                STATE.lazySet(this, HAS_REQUEST_HAS_VALUE);
                actual.onNext(v);
                actual.onComplete();
            } else {
                for (;;) {
                    int s = state;
                    if (s == HAS_REQUEST_NO_VALUE) {
                        if (STATE.compareAndSet(this, s, HAS_REQUEST_HAS_VALUE)) {
                            actual.onNext(v);
                            actual.onComplete();
                            return;
                        }
                    } else
                    if (s == NO_REQUEST_HAS_VALUE) {
                        return;
                    } else
                    if (s == HAS_REQUEST_HAS_VALUE) {
                        value = null;
                        return;
                    } else {
                        value = v;
                        if (STATE.compareAndSet(this, s, NO_REQUEST_HAS_VALUE)) {
                            return;
                        }
                    }
                }
            }
        }
        
        @Override
        public void onError(Throwable t) {
            done = true;

            Try<Optional<T>> v = Notification.error(t);
            
            tryEmit(v);
        }
        
        @Override
        public void onComplete() {
            done = true;
            
            Try<Optional<T>> v = Notification.complete();
            
            tryEmit(v);
        }
        
        @Override
        public void request(long n) {
            if (SubscriptionHelper.validateRequest(n)) {
                return;
            }
            if (BackpressureHelper.add(this, n) == 0) {
                if (done) {
                    for (;;) {
                        int s = state;
                        if (s == NO_REQUEST_HAS_VALUE) {
                            if (STATE.compareAndSet(this, s, HAS_REQUEST_HAS_VALUE)) {
                                Try<Optional<T>> v = value;
                                value = null;
                                actual.onNext(v);
                                actual.onComplete();
                                return;
                            }
                        } else
                        if (s == HAS_REQUEST_NO_VALUE || s == HAS_REQUEST_HAS_VALUE) {
                            return;
                        } else
                        if (STATE.compareAndSet(this, s, HAS_REQUEST_NO_VALUE)) {
                            return;
                        }
                    }
                }
            }
        }
        
        @Override
        public void cancel() {
            STATE.lazySet(this, HAS_REQUEST_HAS_VALUE);
            s.cancel();
        }
    }
}
