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
import java.util.function.*;

import org.reactivestreams.*;

import io.reactivex.Observable.Operator;
import io.reactivex.internal.subscriptions.SubscriptionHelper;
import io.reactivex.internal.util.BackpressureHelper;

public final class OperatorMapNotification<T, R> implements Operator<Publisher<? extends R>, T>{

    final Function<? super T, ? extends Publisher<? extends R>> onNextMapper;
    final Function<? super Throwable, ? extends Publisher<? extends R>> onErrorMapper;
    final Supplier<? extends Publisher<? extends R>> onCompleteSupplier;

    public OperatorMapNotification(Function<? super T, ? extends Publisher<? extends R>> onNextMapper, 
            Function<? super Throwable, ? extends Publisher<? extends R>> onErrorMapper, 
            Supplier<? extends Publisher<? extends R>> onCompleteSupplier) {
        this.onNextMapper = onNextMapper;
        this.onErrorMapper = onErrorMapper;
        this.onCompleteSupplier = onCompleteSupplier;
    }
    
    @Override
    public Subscriber<? super T> apply(Subscriber<? super Publisher<? extends R>> t) {
        return new MapNotificationSubscriber<>(t, onNextMapper, onErrorMapper, onCompleteSupplier);
    }
    
    static final class MapNotificationSubscriber<T, R>
    extends AtomicLong
    implements Subscriber<T>, Subscription {
        /** */
        private static final long serialVersionUID = 2757120512858778108L;
        
        final Subscriber<? super Publisher<? extends R>> actual;
        final Function<? super T, ? extends Publisher<? extends R>> onNextMapper;
        final Function<? super Throwable, ? extends Publisher<? extends R>> onErrorMapper;
        final Supplier<? extends Publisher<? extends R>> onCompleteSupplier;
        
        Subscription s;
        
        Publisher<? extends R> value;
        
        volatile boolean done;

        volatile int state;
        @SuppressWarnings("rawtypes")
        static final AtomicIntegerFieldUpdater<MapNotificationSubscriber> STATE =
                AtomicIntegerFieldUpdater.newUpdater(MapNotificationSubscriber.class, "state");
        
        static final int NO_REQUEST_NO_VALUE = 0;
        static final int NO_REQUEST_HAS_VALUE = 1;
        static final int HAS_REQUEST_NO_VALUE = 2;
        static final int HAS_REQUEST_HAS_VALUE = 3;

        public MapNotificationSubscriber(Subscriber<? super Publisher<? extends R>> actual,
                Function<? super T, ? extends Publisher<? extends R>> onNextMapper,
                Function<? super Throwable, ? extends Publisher<? extends R>> onErrorMapper,
                Supplier<? extends Publisher<? extends R>> onCompleteSupplier) {
            this.actual = actual;
            this.onNextMapper = onNextMapper;
            this.onErrorMapper = onErrorMapper;
            this.onCompleteSupplier = onCompleteSupplier;
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
            Publisher<? extends R> p;
            
            try {
                p = onNextMapper.apply(t);
            } catch (Throwable e) {
                actual.onError(e);
                return;
            }
            
            if (p == null) {
                actual.onError(new NullPointerException("The onNext publisher returned is null"));
                return;
            }
            
            actual.onNext(p);
            
            long r = get();
            if (r != Long.MAX_VALUE) {
                decrementAndGet();
            }
        }
        
        @Override
        public void onError(Throwable t) {
            Publisher<? extends R> p;
            
            try {
                p = onErrorMapper.apply(t);
            } catch (Throwable e) {
                actual.onError(e);
                return;
            }

            if (p == null) {
                actual.onError(new NullPointerException("The onError publisher returned is null"));
                return;
            }

            tryEmit(p);
        }
        
        @Override
        public void onComplete() {
            Publisher<? extends R> p;
            
            try {
                p = onCompleteSupplier.get();
            } catch (Throwable e) {
                actual.onError(e);
                return;
            }

            if (p == null) {
                actual.onError(new NullPointerException("The onComplete publisher returned is null"));
                return;
            }

            tryEmit(p);
        }
        
        
        void tryEmit(Publisher<? extends R> p) {
            long r = get();
            if (r != 0L) {
                actual.onNext(p);
                actual.onComplete();
            } else {
                for (;;) {
                    int s = state;
                    if (s == HAS_REQUEST_NO_VALUE) {
                        if (STATE.compareAndSet(this, HAS_REQUEST_NO_VALUE, HAS_REQUEST_HAS_VALUE)) {
                            actual.onNext(p);
                            actual.onComplete();
                        }
                        return;
                    } else
                    if (s == NO_REQUEST_NO_VALUE) {
                        value = p;
                        done = true;
                        if (STATE.compareAndSet(this, NO_REQUEST_NO_VALUE, NO_REQUEST_HAS_VALUE)) {
                            return;
                        }
                    } else
                    if (s == NO_REQUEST_HAS_VALUE || s == HAS_REQUEST_HAS_VALUE) {
                        return;
                    }
                }
            }
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
                    
                    if (s == HAS_REQUEST_NO_VALUE || s == HAS_REQUEST_HAS_VALUE) {
                        return;
                    } else
                    if (s == NO_REQUEST_HAS_VALUE) {
                        if (STATE.compareAndSet(this, NO_REQUEST_HAS_VALUE, HAS_REQUEST_HAS_VALUE)) {
                            Publisher<? extends R> p = value;
                            value = null;
                            actual.onNext(p);
                            actual.onComplete();
                        }
                        return;
                    } else
                    if (STATE.compareAndSet(this, NO_REQUEST_NO_VALUE, HAS_REQUEST_NO_VALUE)) {
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
