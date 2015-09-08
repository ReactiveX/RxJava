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

import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.function.*;

import org.reactivestreams.*;

import io.reactivex.Observable.Operator;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.subscribers.*;
import io.reactivex.internal.subscriptions.*;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.subscribers.SerializedSubscriber;

public final class OperatorTimeout<T, U, V> implements Operator<T, T> {
    final Supplier<? extends Publisher<U>> firstTimeoutSelector; 
    final Function<? super T, ? extends Publisher<V>> timeoutSelector; 
    final Publisher<? extends T> other;

    public OperatorTimeout(Supplier<? extends Publisher<U>> firstTimeoutSelector,
            Function<? super T, ? extends Publisher<V>> timeoutSelector, Publisher<? extends T> other) {
        this.firstTimeoutSelector = firstTimeoutSelector;
        this.timeoutSelector = timeoutSelector;
        this.other = other;
    }

    @Override
    public Subscriber<? super T> apply(Subscriber<? super T> t) {
        if (other == null) {
            return new TimeoutSubscriber<>(
                    new SerializedSubscriber<>(t), 
                    firstTimeoutSelector, timeoutSelector);
        }
        return new TimeoutOtherSubscriber<>(t, firstTimeoutSelector, timeoutSelector, other);
    }
    
    static final class TimeoutSubscriber<T, U, V> implements Subscriber<T>, Subscription, OnTimeout {
        final Subscriber<? super T> actual;
        final Supplier<? extends Publisher<U>> firstTimeoutSelector; 
        final Function<? super T, ? extends Publisher<V>> timeoutSelector; 

        Subscription s;
        
        volatile boolean cancelled;
        
        volatile long index;
        
        volatile Disposable timeout;
        @SuppressWarnings("rawtypes")
        static final AtomicReferenceFieldUpdater<TimeoutSubscriber, Disposable> TIMEOUT =
                AtomicReferenceFieldUpdater.newUpdater(TimeoutSubscriber.class, Disposable.class, "timeout");
        
        static final Disposable CANCELLED = () -> { };

        public TimeoutSubscriber(Subscriber<? super T> actual, 
                Supplier<? extends Publisher<U>> firstTimeoutSelector,
                Function<? super T, ? extends Publisher<V>> timeoutSelector) {
            this.actual = actual;
            this.firstTimeoutSelector = firstTimeoutSelector;
            this.timeoutSelector = timeoutSelector;
        }
        
        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.validateSubscription(this.s, s)) {
                return;
            }
            this.s = s;
            
            if (cancelled) {
                return;
            }
            
            Subscriber<? super T> a = actual;
            
            Publisher<U> p;
            
            if (firstTimeoutSelector != null) {
                try {
                    p = firstTimeoutSelector.get();
                } catch (Exception ex) {
                    cancel();
                    EmptySubscription.error(ex, a);
                    return;
                }
                
                if (p == null) {
                    cancel();
                    EmptySubscription.error(new NullPointerException("The first timeout publisher is null"), a);
                    return;
                }
                
                TimeoutInnerSubscriber<T, U, V> tis = new TimeoutInnerSubscriber<>(this, 0);
                
                if (TIMEOUT.compareAndSet(this, null, tis)) {
                    a.onSubscribe(s);
                    p.subscribe(tis);
                }
            } else {
                a.onSubscribe(s);
            }
        }
        
        @Override
        public void onNext(T t) {
            long idx = index + 1;
            index = idx;

            actual.onNext(t);
            
            Disposable d = timeout;
            if (d != null) {
                d.dispose();
            }
            
            Publisher<V> p;
            
            try {
                p = timeoutSelector.apply(t);
            } catch (Throwable e) {
                cancel();
                actual.onError(e);
                return;
            }
            
            if (p == null) {
                cancel();
                actual.onError(new NullPointerException("The publisher returned is null"));
                return;
            }
            
            TimeoutInnerSubscriber<T, U, V> tis = new TimeoutInnerSubscriber<>(this, idx);
            
            if (TIMEOUT.compareAndSet(this, d, tis)) {
                p.subscribe(tis);
            }
        }
        
        @Override
        public void onError(Throwable t) {
            cancel();
            actual.onError(t);
        }
        
        @Override
        public void onComplete() {
            cancel();
            actual.onComplete();
        }
        
        @Override
        public void request(long n) {
            s.request(n);
        }
        
        @Override
        public void cancel() {
            cancelled = true;
            s.cancel();
            
            Disposable d = timeout;
            if (d != CANCELLED) {
                d = TIMEOUT.getAndSet(this, CANCELLED);
                if (d != CANCELLED && d != null) {
                    d.dispose();
                }
            }
        }
        
        @Override
        public void timeout(long idx) {
            if (idx == index) {
                cancel();
                actual.onError(new TimeoutException());
            }
        }
    }
    
    interface OnTimeout {
        void timeout(long index);
        
        void onError(Throwable e);
    }
    
    static final class TimeoutInnerSubscriber<T, U, V> extends DisposableSubscriber<Object> {
        final OnTimeout parent;
        final long index;
        
        boolean done;
        
        public TimeoutInnerSubscriber(OnTimeout parent, final long index) {
            this.parent = parent;
            this.index = index;
        }
        
        @Override
        public void onNext(Object t) {
            if (done) {
                return;
            }
            done = true;
            cancel();
            parent.timeout(index);
        }
        
        @Override
        public void onError(Throwable t) {
            parent.onError(t);
        }
        
        @Override
        public void onComplete() {
            if (done) {
                return;
            }
            done = true;
            parent.timeout(index);
        }
    }
    
    static final class TimeoutOtherSubscriber<T, U, V> implements Subscriber<T>, Disposable, OnTimeout {
        final Subscriber<? super T> actual;
        final Supplier<? extends Publisher<U>> firstTimeoutSelector; 
        final Function<? super T, ? extends Publisher<V>> timeoutSelector;
        final Publisher<? extends T> other;
        final FullArbiter<T> arbiter;
        
        Subscription s;
        
        boolean done;
        
        volatile boolean cancelled;
        
        volatile long index;
        
        volatile Disposable timeout;
        @SuppressWarnings("rawtypes")
        static final AtomicReferenceFieldUpdater<TimeoutOtherSubscriber, Disposable> TIMEOUT =
                AtomicReferenceFieldUpdater.newUpdater(TimeoutOtherSubscriber.class, Disposable.class, "timeout");
        
        static final Disposable CANCELLED = () -> { };

        public TimeoutOtherSubscriber(Subscriber<? super T> actual,
                Supplier<? extends Publisher<U>> firstTimeoutSelector,
                Function<? super T, ? extends Publisher<V>> timeoutSelector, Publisher<? extends T> other) {
            this.actual = actual;
            this.firstTimeoutSelector = firstTimeoutSelector;
            this.timeoutSelector = timeoutSelector;
            this.other = other;
            this.arbiter = new FullArbiter<>(actual, this, 8);
        }
        
        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.validateSubscription(this.s, s)) {
                return;
            }
            this.s = s;
            
            if (!arbiter.setSubscription(s)) {
                return;
            }
            Subscriber<? super T> a = actual;
            
            if (firstTimeoutSelector != null) {
                Publisher<U> p;
                
                try {
                    p = firstTimeoutSelector.get();
                } catch (Exception ex) {
                    dispose();
                    EmptySubscription.error(ex, a);
                    return;
                }
                
                if (p == null) {
                    dispose();
                    EmptySubscription.error(new NullPointerException("The first timeout publisher is null"), a);
                    return;
                }
                
                TimeoutInnerSubscriber<T, U, V> tis = new TimeoutInnerSubscriber<>(this, 0);
                
                if (TIMEOUT.compareAndSet(this, null, tis)) {
                    a.onSubscribe(arbiter);
                    p.subscribe(tis);
                }
            } else {
                a.onSubscribe(arbiter);
            }
        }
        
        @Override
        public void onNext(T t) {
            if (done) {
                return;
            }
            long idx = index + 1;
            index = idx;

            if (!arbiter.onNext(t, s)) {
                return;
            }
            
            Disposable d = timeout;
            if (d != null) {
                d.dispose();
            }
            
            Publisher<V> p;
            
            try {
                p = timeoutSelector.apply(t);
            } catch (Throwable e) {
                actual.onError(e);
                return;
            }
            
            if (p == null) {
                actual.onError(new NullPointerException("The publisher returned is null"));
                return;
            }
            
            TimeoutInnerSubscriber<T, U, V> tis = new TimeoutInnerSubscriber<>(this, idx);
            
            if (TIMEOUT.compareAndSet(this, d, tis)) {
                p.subscribe(tis);
            }
        }
        
        @Override
        public void onError(Throwable t) {
            if (done) {
                RxJavaPlugins.onError(t);
                return;
            }
            done = true;
            dispose();
            arbiter.onError(t, s);
        }
        
        @Override
        public void onComplete() {
            if (done) {
                return;
            }
            done = true;
            dispose();
            arbiter.onComplete(s);
        }
        
        @Override
        public void dispose() {
            cancelled = true;
            s.cancel();
            
            Disposable d = timeout;
            if (d != CANCELLED) {
                d = TIMEOUT.getAndSet(this, CANCELLED);
                if (d != CANCELLED && d != null) {
                    d.dispose();
                }
            }
        }
        
        @Override
        public void timeout(long idx) {
            if (idx == index) {
                dispose();
                other.subscribe(new FullArbiterSubscriber<>(arbiter));
            }
        }
    }
}
