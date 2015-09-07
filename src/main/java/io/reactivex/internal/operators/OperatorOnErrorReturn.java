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
import java.util.function.Function;

import org.reactivestreams.*;

import io.reactivex.Observable.Operator;
import io.reactivex.internal.subscriptions.SubscriptionHelper;
import io.reactivex.internal.util.BackpressureHelper;

public final class OperatorOnErrorReturn<T> implements Operator<T, T> {
    final Function<? super Throwable, ? extends T> valueSupplier;
    public OperatorOnErrorReturn(Function<? super Throwable, ? extends T> valueSupplier) {
        this.valueSupplier = valueSupplier;
    }
    
    @Override
    public Subscriber<? super T> apply(Subscriber<? super T> t) {
        return new OnErrorReturnSubscriber<>(t, valueSupplier);
    }
    
    static final class OnErrorReturnSubscriber<T> extends AtomicLong implements Subscriber<T>, Subscription {
        /** */
        private static final long serialVersionUID = -3740826063558713822L;
        final Subscriber<? super T> actual;
        final Function<? super Throwable, ? extends T> valueSupplier;
        
        Subscription s;
        
        volatile int state;
        @SuppressWarnings("rawtypes")
        static final AtomicIntegerFieldUpdater<OnErrorReturnSubscriber> STATE =
                AtomicIntegerFieldUpdater.newUpdater(OnErrorReturnSubscriber.class, "state");
        T value;
        
        volatile boolean done;
        
        static final int NO_REQUEST_NO_VALUE = 0;
        static final int NO_REQUEST_HAS_VALUE = 1;
        static final int HAS_REQUEST_NO_VALUE = 2;
        static final int HAS_REQUEST_HAS_VALUE = 3;
        
        public OnErrorReturnSubscriber(Subscriber<? super T> actual, Function<? super Throwable, ? extends T> valueSupplier) {
            this.actual = actual;
            this.valueSupplier = valueSupplier;
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
            actual.onNext(t);
            
            if (get() != Long.MAX_VALUE) {
                decrementAndGet();
            }
        }
        
        @Override
        public void onError(Throwable t) {
            done = true;
            T v;
            try {
                v = valueSupplier.apply(t);
            } catch (Throwable e) {
                STATE.lazySet(this, HAS_REQUEST_HAS_VALUE);
                t.addSuppressed(e);
                actual.onError(t);
                return;
            }
            
            if (v == null) {
                STATE.lazySet(this, HAS_REQUEST_HAS_VALUE);
                t.addSuppressed(new NullPointerException("The supplied value is null"));
                actual.onError(t);
                return;
            }
            
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
        public void onComplete() {
            STATE.lazySet(this, HAS_REQUEST_HAS_VALUE);
            actual.onComplete();
        }
        
        @Override
        public void request(long n) {
            if (SubscriptionHelper.validateRequest(n)) {
                return;
            }
            BackpressureHelper.add(this, n);
            if (done) {
                for (;;) {
                    int s = state;
                    if (s == NO_REQUEST_HAS_VALUE) {
                        if (STATE.compareAndSet(this, s, HAS_REQUEST_HAS_VALUE)) {
                            T v = value;
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
            } else {
                s.request(n);
            }
        }
        
        @Override
        public void cancel() {
            STATE.lazySet(this, HAS_REQUEST_HAS_VALUE);
            s.cancel();
        }
    }
}
