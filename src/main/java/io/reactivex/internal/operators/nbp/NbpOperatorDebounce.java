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

import java.util.concurrent.atomic.*;
import java.util.function.Function;

import io.reactivex.NbpObservable;
import io.reactivex.NbpObservable.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.subscribers.nbp.NbpDisposableSubscriber;
import io.reactivex.internal.subscriptions.SubscriptionHelper;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.subscribers.nbp.NbpSerializedSubscriber;

public final class NbpOperatorDebounce<T, U> implements NbpOperator<T, T> {
    final Function<? super T, ? extends NbpObservable<U>> debounceSelector;

    public NbpOperatorDebounce(Function<? super T, ? extends NbpObservable<U>> debounceSelector) {
        this.debounceSelector = debounceSelector;
    }
    
    @Override
    public NbpSubscriber<? super T> apply(NbpSubscriber<? super T> t) {
        return new DebounceSubscriber<>(new NbpSerializedSubscriber<>(t), debounceSelector);
    }
    
    static final class DebounceSubscriber<T, U> 
    implements NbpSubscriber<T>, Disposable {
        final NbpSubscriber<? super T> actual;
        final Function<? super T, ? extends NbpObservable<U>> debounceSelector;
        
        volatile boolean gate;

        Disposable s;
        
        volatile Disposable debouncer;
        @SuppressWarnings("rawtypes")
        static final AtomicReferenceFieldUpdater<DebounceSubscriber, Disposable> DEBOUNCER =
                AtomicReferenceFieldUpdater.newUpdater(DebounceSubscriber.class, Disposable.class, "debouncer");
        
        static final Disposable CANCELLED = () -> { };

        volatile long index;
        
        boolean done;

        public DebounceSubscriber(NbpSubscriber<? super T> actual,
                Function<? super T, ? extends NbpObservable<U>> debounceSelector) {
            this.actual = actual;
            this.debounceSelector = debounceSelector;
        }
        
        @Override
        public void onSubscribe(Disposable s) {
            if (SubscriptionHelper.validateDisposable(this.s, s)) {
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
            
            long idx = index + 1;
            index = idx;
            
            Disposable d = debouncer;
            if (d != null) {
                d.dispose();
            }
            
            NbpObservable<U> p;
            
            try {
                p = debounceSelector.apply(t);
            } catch (Throwable e) {
                dispose();
                actual.onError(e);
                return;
            }
            
            if (p == null) {
                dispose();
                actual.onError(new NullPointerException("The publisher supplied is null"));
                return;
            }
            
            DebounceInnerSubscriber<T, U> dis = new DebounceInnerSubscriber<>(this, idx, t);
            
            if (DEBOUNCER.compareAndSet(this, d, dis)) {
                p.subscribe(dis);
            }
        }
        
        @Override
        public void onError(Throwable t) {
            disposeDebouncer();
            actual.onError(t);
        }
        
        @Override
        public void onComplete() {
            if (done) {
                return;
            }
            done = true;
            Disposable d = debouncer;
            if (d != CANCELLED) {
                @SuppressWarnings("unchecked")
                DebounceInnerSubscriber<T, U> dis = (DebounceInnerSubscriber<T, U>)d;
                dis.emit();
                disposeDebouncer();
                actual.onComplete();
            }
        }
        
        @Override
        public void dispose() {
            s.dispose();
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
        
        void emit(long idx, T value) {
            if (idx == index) {
                actual.onNext(value);
            }
        }
        
        static final class DebounceInnerSubscriber<T, U> extends NbpDisposableSubscriber<U> {
            final DebounceSubscriber<T, U> parent;
            final long index;
            final T value;
            
            boolean done;
            
            volatile int once;
            @SuppressWarnings("rawtypes")
            static final AtomicIntegerFieldUpdater<DebounceInnerSubscriber> ONCE =
                    AtomicIntegerFieldUpdater.newUpdater(DebounceInnerSubscriber.class, "once");
            
            public DebounceInnerSubscriber(DebounceSubscriber<T, U> parent, long index, T value) {
                this.parent = parent;
                this.index = index;
                this.value = value;
            }
            
            @Override
            public void onNext(U t) {
                if (done) {
                    return;
                }
                done = true;
                dispose();
                emit();
            }
            
            void emit() {
                if (ONCE.compareAndSet(this, 0, 1)) {
                    parent.emit(index, value);
                }
            }
            
            @Override
            public void onError(Throwable t) {
                if (done) {
                    RxJavaPlugins.onError(t);
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
                emit();
            }
        }
    }
}
