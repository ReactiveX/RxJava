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
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.subscribers.DisposableSubscriber;
import io.reactivex.internal.subscriptions.SubscriptionHelper;
import io.reactivex.internal.util.BackpressureHelper;
import io.reactivex.subscribers.SerializedSubscriber;

public final class OperatorDebounce<T, U> implements Operator<T, T> {
    final Function<? super T, ? extends Publisher<U>> debounceSelector;

    public OperatorDebounce(Function<? super T, ? extends Publisher<U>> debounceSelector) {
        this.debounceSelector = debounceSelector;
    }
    
    @Override
    public Subscriber<? super T> apply(Subscriber<? super T> t) {
        return new DebounceSubscriber<>(new SerializedSubscriber<>(t), debounceSelector);
    }
    
    static final class DebounceSubscriber<T, U> extends AtomicLong implements Subscriber<T>, Subscription {
        /** */
        private static final long serialVersionUID = 6725975399620862591L;
        final Subscriber<? super T> actual;
        final Function<? super T, ? extends Publisher<U>> debounceSelector;
        
        volatile boolean gate;

        Subscription s;
        
        volatile Disposable debouncer;
        @SuppressWarnings("rawtypes")
        static final AtomicReferenceFieldUpdater<DebounceSubscriber, Disposable> DEBOUNCER =
                AtomicReferenceFieldUpdater.newUpdater(DebounceSubscriber.class, Disposable.class, "debouncer");
        
        static final Disposable CANCELLED = () -> { };
        
        public DebounceSubscriber(Subscriber<? super T> actual,
                Function<? super T, ? extends Publisher<U>> debounceSelector) {
            this.actual = actual;
            this.debounceSelector = debounceSelector;
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
            if (!gate) {
                gate = true;

                Publisher<U> p;
                
                try {
                    p = debounceSelector.apply(t);
                } catch (Throwable e) {
                    s.cancel();
                    actual.onError(e);
                    return;
                }
                
                if (p == null) {
                    s.cancel();
                    actual.onError(new NullPointerException("The publisher returned by the selector is null"));
                    return;
                }
                
                long r = get();
                if (r != 0) {
                    actual.onNext(t);
                    if (r != Long.MAX_VALUE) {
                        decrementAndGet();
                    }
                } else {
                    s.cancel();
                    actual.onError(new IllegalStateException("Could not deliver value due to lack of requests"));
                    return;
                }
                
                DebounceInnerSubscriber<T, U> dis = new DebounceInnerSubscriber<>(this);
                if (DEBOUNCER.compareAndSet(this, null, dis)) {
                    p.subscribe(dis);
                }
            }
        }
        
        @Override
        public void onError(Throwable t) {
            disposeDebouncer();
            actual.onError(t);
        }
        
        @Override
        public void onComplete() {
            disposeDebouncer();
            actual.onComplete();
        }
        
        @Override
        public void request(long n) {
            if (SubscriptionHelper.validateRequest(n)) {
                return;
            }
            
            BackpressureHelper.add(this, n);
        }
        
        @Override
        public void cancel() {
            s.cancel();
            disposeDebouncer();
        }
        
        public void disposeDebouncer() {
            Disposable d = debouncer;
            if (d != CANCELLED) {
                d = DEBOUNCER.getAndSet(this, CANCELLED);
                if (d != CANCELLED && d != null) {
                    d.dispose();
                }
            }
        }
        
        void open(Disposable d) {
            if (DEBOUNCER.compareAndSet(this, d, null)) {
                gate = false;
            }
        }
        
        static final class DebounceInnerSubscriber<T, U> extends DisposableSubscriber<U> {
            final DebounceSubscriber<T, U> parent;
            
            boolean done;
            
            public DebounceInnerSubscriber(DebounceSubscriber<T, U> parent) {
                this.parent = parent;
            }
            
            @Override
            public void onNext(U t) {
                if (done) {
                    return;
                }
                done = true;
                cancel();
                parent.open(this);
            }
            
            @Override
            public void onError(Throwable t) {
                if (done) {
                    return;
                }
                done = true;
                parent.onError(t);
            }
            
            @Override
            public void onComplete() {
                if (done) {
                    return;
                }
                done = true;
                parent.open(this);
            }
        }
    }
}
