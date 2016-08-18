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

import java.util.concurrent.atomic.*;

import org.reactivestreams.*;

import io.reactivex.exceptions.*;
import io.reactivex.functions.Function;
import io.reactivex.internal.subscriptions.SubscriptionHelper;
import io.reactivex.internal.util.BackpressureHelper;

public final class FlowableOnErrorReturn<T> extends AbstractFlowableWithUpstream<T, T> {
    final Function<? super Throwable, ? extends T> valueSupplier;
    public FlowableOnErrorReturn(Publisher<T> source, Function<? super Throwable, ? extends T> valueSupplier) {
        super(source);
        this.valueSupplier = valueSupplier;
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> s) {
        source.subscribe(new OnErrorReturnSubscriber<T>(s, valueSupplier));
    }
    
    // FIXME requires post-complete drain management
    static final class OnErrorReturnSubscriber<T> extends AtomicLong implements Subscriber<T>, Subscription {
        /** */
        private static final long serialVersionUID = -3740826063558713822L;
        final Subscriber<? super T> actual;
        final Function<? super Throwable, ? extends T> valueSupplier;
        
        Subscription s;
        
        final AtomicInteger state = new AtomicInteger();
        
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
            if (SubscriptionHelper.validate(this.s, s)) {
                this.s = s;
                actual.onSubscribe(this);
            }
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
                Exceptions.throwIfFatal(e);
                state.lazySet(HAS_REQUEST_HAS_VALUE);
                actual.onError(new CompositeException(e, t));
                return;
            }
            
            if (v == null) {
                state.lazySet(HAS_REQUEST_HAS_VALUE);
                NullPointerException npe = new NullPointerException("The supplied value is null");
                npe.initCause(t);
                actual.onError(npe);
                return;
            }
            
            if (get() != 0L) {
                state.lazySet(HAS_REQUEST_HAS_VALUE);
                actual.onNext(v);
                actual.onComplete();
            } else {
                for (;;) {
                    int s = state.get();
                    if (s == HAS_REQUEST_NO_VALUE) {
                        if (state.compareAndSet(s, HAS_REQUEST_HAS_VALUE)) {
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
                        if (state.compareAndSet(s, NO_REQUEST_HAS_VALUE)) {
                            return;
                        }
                    }
                }
            }
        }
        
        @Override
        public void onComplete() {
            state.lazySet(HAS_REQUEST_HAS_VALUE);
            actual.onComplete();
        }
        
        @Override
        public void request(long n) {
            if (!SubscriptionHelper.validate(n)) {
                return;
            }
            BackpressureHelper.add(this, n);
            if (done) {
                for (;;) {
                    int s = state.get();
                    if (s == NO_REQUEST_HAS_VALUE) {
                        if (state.compareAndSet(s, HAS_REQUEST_HAS_VALUE)) {
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
                    if (state.compareAndSet(s, HAS_REQUEST_NO_VALUE)) {
                        return;
                    }
                }
            } else {
                s.request(n);
            }
        }
        
        @Override
        public void cancel() {
            state.lazySet(HAS_REQUEST_HAS_VALUE);
            s.cancel();
        }
    }
}