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

package io.reactivex.internal.operators.nbp;

import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.function.*;

import io.reactivex.NbpObservable;
import io.reactivex.NbpObservable.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.disposables.*;
import io.reactivex.internal.subscribers.NbpFullArbiterSubscriber;
import io.reactivex.internal.subscribers.nbp.NbpDisposableSubscriber;
import io.reactivex.internal.subscriptions.SubscriptionHelper;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.subscribers.nbp.NbpSerializedSubscriber;

public final class NbpOperatorTimeout<T, U, V> implements NbpOperator<T, T> {
    final Supplier<? extends NbpObservable<U>> firstTimeoutSelector; 
    final Function<? super T, ? extends NbpObservable<V>> timeoutSelector; 
    final NbpObservable<? extends T> other;

    public NbpOperatorTimeout(Supplier<? extends NbpObservable<U>> firstTimeoutSelector,
            Function<? super T, ? extends NbpObservable<V>> timeoutSelector, NbpObservable<? extends T> other) {
        this.firstTimeoutSelector = firstTimeoutSelector;
        this.timeoutSelector = timeoutSelector;
        this.other = other;
    }

    @Override
    public NbpSubscriber<? super T> apply(NbpSubscriber<? super T> t) {
        if (other == null) {
            return new TimeoutSubscriber<>(
                    new NbpSerializedSubscriber<>(t), 
                    firstTimeoutSelector, timeoutSelector);
        }
        return new TimeoutOtherSubscriber<>(t, firstTimeoutSelector, timeoutSelector, other);
    }
    
    static final class TimeoutSubscriber<T, U, V> implements NbpSubscriber<T>, Disposable, OnTimeout {
        final NbpSubscriber<? super T> actual;
        final Supplier<? extends NbpObservable<U>> firstTimeoutSelector; 
        final Function<? super T, ? extends NbpObservable<V>> timeoutSelector; 

        Disposable s;
        
        volatile boolean cancelled;
        
        volatile long index;
        
        volatile Disposable timeout;
        @SuppressWarnings("rawtypes")
        static final AtomicReferenceFieldUpdater<TimeoutSubscriber, Disposable> TIMEOUT =
                AtomicReferenceFieldUpdater.newUpdater(TimeoutSubscriber.class, Disposable.class, "timeout");
        
        static final Disposable CANCELLED = () -> { };

        public TimeoutSubscriber(NbpSubscriber<? super T> actual, 
                Supplier<? extends NbpObservable<U>> firstTimeoutSelector,
                Function<? super T, ? extends NbpObservable<V>> timeoutSelector) {
            this.actual = actual;
            this.firstTimeoutSelector = firstTimeoutSelector;
            this.timeoutSelector = timeoutSelector;
        }
        
        @Override
        public void onSubscribe(Disposable s) {
            if (SubscriptionHelper.validateDisposable(this.s, s)) {
                return;
            }
            this.s = s;
            
            NbpSubscriber<? super T> a = actual;
            
            NbpObservable<U> p;
            
            if (firstTimeoutSelector != null) {
                try {
                    p = firstTimeoutSelector.get();
                } catch (Exception ex) {
                    dispose();
                    EmptyDisposable.error(ex, a);
                    return;
                }
                
                if (p == null) {
                    dispose();
                    EmptyDisposable.error(new NullPointerException("The first timeout NbpObservable is null"), a);
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
            
            NbpObservable<V> p;
            
            try {
                p = timeoutSelector.apply(t);
            } catch (Throwable e) {
                dispose();
                actual.onError(e);
                return;
            }
            
            if (p == null) {
                dispose();
                actual.onError(new NullPointerException("The NbpObservable returned is null"));
                return;
            }
            
            TimeoutInnerSubscriber<T, U, V> tis = new TimeoutInnerSubscriber<>(this, idx);
            
            if (TIMEOUT.compareAndSet(this, d, tis)) {
                p.subscribe(tis);
            }
        }
        
        @Override
        public void onError(Throwable t) {
            dispose();
            actual.onError(t);
        }
        
        @Override
        public void onComplete() {
            dispose();
            actual.onComplete();
        }
        
        @Override
        public void dispose() {
            if (!cancelled) {
                cancelled = true;
                s.dispose();
                
                Disposable d = timeout;
                if (d != CANCELLED) {
                    d = TIMEOUT.getAndSet(this, CANCELLED);
                    if (d != CANCELLED && d != null) {
                        d.dispose();
                    }
                }
            }
        }
        
        @Override
        public void timeout(long idx) {
            if (idx == index) {
                dispose();
                actual.onError(new TimeoutException());
            }
        }
    }
    
    interface OnTimeout {
        void timeout(long index);
        
        void onError(Throwable e);
    }
    
    static final class TimeoutInnerSubscriber<T, U, V> extends NbpDisposableSubscriber<Object> {
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
            dispose();
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
    
    static final class TimeoutOtherSubscriber<T, U, V> implements NbpSubscriber<T>, Disposable, OnTimeout {
        final NbpSubscriber<? super T> actual;
        final Supplier<? extends NbpObservable<U>> firstTimeoutSelector; 
        final Function<? super T, ? extends NbpObservable<V>> timeoutSelector;
        final NbpObservable<? extends T> other;
        final NbpFullArbiter<T> arbiter;
        
        Disposable s;
        
        boolean done;
        
        volatile boolean cancelled;
        
        volatile long index;
        
        volatile Disposable timeout;
        @SuppressWarnings("rawtypes")
        static final AtomicReferenceFieldUpdater<TimeoutOtherSubscriber, Disposable> TIMEOUT =
                AtomicReferenceFieldUpdater.newUpdater(TimeoutOtherSubscriber.class, Disposable.class, "timeout");
        
        static final Disposable CANCELLED = () -> { };

        public TimeoutOtherSubscriber(NbpSubscriber<? super T> actual,
                Supplier<? extends NbpObservable<U>> firstTimeoutSelector,
                Function<? super T, ? extends NbpObservable<V>> timeoutSelector, NbpObservable<? extends T> other) {
            this.actual = actual;
            this.firstTimeoutSelector = firstTimeoutSelector;
            this.timeoutSelector = timeoutSelector;
            this.other = other;
            this.arbiter = new NbpFullArbiter<>(actual, this, 8);
        }
        
        @Override
        public void onSubscribe(Disposable s) {
            if (SubscriptionHelper.validateDisposable(this.s, s)) {
                return;
            }
            this.s = s;
            
            if (!arbiter.setSubscription(s)) {
                return;
            }
            NbpSubscriber<? super T> a = actual;
            
            if (firstTimeoutSelector != null) {
                NbpObservable<U> p;
                
                try {
                    p = firstTimeoutSelector.get();
                } catch (Exception ex) {
                    dispose();
                    EmptyDisposable.error(ex, a);
                    return;
                }
                
                if (p == null) {
                    dispose();
                    EmptyDisposable.error(new NullPointerException("The first timeout NbpObservable is null"), a);
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
            
            NbpObservable<V> p;
            
            try {
                p = timeoutSelector.apply(t);
            } catch (Throwable e) {
                actual.onError(e);
                return;
            }
            
            if (p == null) {
                actual.onError(new NullPointerException("The NbpObservable returned is null"));
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
            if (!cancelled) {
                cancelled = true;
                s.dispose();
                
                Disposable d = timeout;
                if (d != CANCELLED) {
                    d = TIMEOUT.getAndSet(this, CANCELLED);
                    if (d != CANCELLED && d != null) {
                        d.dispose();
                    }
                }
            }
        }
        
        @Override
        public void timeout(long idx) {
            if (idx == index) {
                dispose();
                other.subscribe(new NbpFullArbiterSubscriber<>(arbiter));
            }
        }
    }
}
