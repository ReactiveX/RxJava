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

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.*;

import org.reactivestreams.*;

import io.reactivex.exceptions.Exceptions;
import io.reactivex.functions.Function;
import io.reactivex.internal.subscriptions.SubscriptionHelper;
import io.reactivex.internal.util.BackpressureHelper;

public final class FlowableMapNotification<T, R> extends AbstractFlowableWithUpstream<T, R> {

    final Function<? super T, ? extends R> onNextMapper;
    final Function<? super Throwable, ? extends R> onErrorMapper;
    final Callable<? extends R> onCompleteSupplier;

    public FlowableMapNotification(
            Publisher<T> source,
            Function<? super T, ? extends R> onNextMapper, 
            Function<? super Throwable, ? extends R> onErrorMapper, 
            Callable<? extends R> onCompleteSupplier) {
        super(source);
        this.onNextMapper = onNextMapper;
        this.onErrorMapper = onErrorMapper;
        this.onCompleteSupplier = onCompleteSupplier;
    }
    
    @Override
    protected void subscribeActual(Subscriber<? super R> s) {
        source.subscribe(new MapNotificationSubscriber<T, R>(s, onNextMapper, onErrorMapper, onCompleteSupplier));
    }
    
    // FIXME needs post-complete drain management
    static final class MapNotificationSubscriber<T, R>
    extends AtomicLong
    implements Subscriber<T>, Subscription {
        /** */
        private static final long serialVersionUID = 2757120512858778108L;
        
        final Subscriber<? super R> actual;
        final Function<? super T, ? extends R> onNextMapper;
        final Function<? super Throwable, ? extends R> onErrorMapper;
        final Callable<? extends R> onCompleteSupplier;
        
        Subscription s;
        
        R value;
        
        volatile boolean done;

        final AtomicInteger state = new AtomicInteger();
        
        static final int NO_REQUEST_NO_VALUE = 0;
        static final int NO_REQUEST_HAS_VALUE = 1;
        static final int HAS_REQUEST_NO_VALUE = 2;
        static final int HAS_REQUEST_HAS_VALUE = 3;

        public MapNotificationSubscriber(Subscriber<? super R> actual,
                Function<? super T, ? extends R> onNextMapper,
                Function<? super Throwable, ? extends R> onErrorMapper,
                Callable<? extends R> onCompleteSupplier) {
            this.actual = actual;
            this.onNextMapper = onNextMapper;
            this.onErrorMapper = onErrorMapper;
            this.onCompleteSupplier = onCompleteSupplier;
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
            R p;
            
            try {
                p = onNextMapper.apply(t);
            } catch (Throwable e) {
                Exceptions.throwIfFatal(e);
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
            R p;
            
            try {
                p = onErrorMapper.apply(t);
            } catch (Throwable e) {
                Exceptions.throwIfFatal(e);
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
            R p;
            
            try {
                p = onCompleteSupplier.call();
            } catch (Throwable e) {
                Exceptions.throwIfFatal(e);
                actual.onError(e);
                return;
            }

            if (p == null) {
                actual.onError(new NullPointerException("The onComplete publisher returned is null"));
                return;
            }

            tryEmit(p);
        }
        
        
        void tryEmit(R p) {
            long r = get();
            if (r != 0L) {
                actual.onNext(p);
                actual.onComplete();
            } else {
                for (;;) {
                    int s = state.get();
                    if (s == HAS_REQUEST_NO_VALUE) {
                        if (state.compareAndSet(HAS_REQUEST_NO_VALUE, HAS_REQUEST_HAS_VALUE)) {
                            actual.onNext(p);
                            actual.onComplete();
                        }
                        return;
                    } else
                    if (s == NO_REQUEST_NO_VALUE) {
                        value = p;
                        done = true;
                        if (state.compareAndSet(NO_REQUEST_NO_VALUE, NO_REQUEST_HAS_VALUE)) {
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
            if (!SubscriptionHelper.validate(n)) {
                return;
            }
            
            BackpressureHelper.add(this, n);
            if (done) {
                for (;;) {
                    int s = state.get();
                    
                    if (s == HAS_REQUEST_NO_VALUE || s == HAS_REQUEST_HAS_VALUE) {
                        return;
                    } else
                    if (s == NO_REQUEST_HAS_VALUE) {
                        if (state.compareAndSet(NO_REQUEST_HAS_VALUE, HAS_REQUEST_HAS_VALUE)) {
                            R p = value;
                            value = null;
                            actual.onNext(p);
                            actual.onComplete();
                        }
                        return;
                    } else
                    if (state.compareAndSet(NO_REQUEST_NO_VALUE, HAS_REQUEST_NO_VALUE)) {
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
