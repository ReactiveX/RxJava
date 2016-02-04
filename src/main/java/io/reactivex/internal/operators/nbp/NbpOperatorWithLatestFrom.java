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

package io.reactivex.internal.operators.nbp;

import java.util.concurrent.atomic.*;

import io.reactivex.NbpObservable;
import io.reactivex.NbpObservable.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.BiFunction;
import io.reactivex.internal.disposables.EmptyDisposable;
import io.reactivex.internal.subscriptions.SubscriptionHelper;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.subscribers.nbp.NbpSerializedSubscriber;

public final class NbpOperatorWithLatestFrom<T, U, R> implements NbpOperator<R, T> {
    final BiFunction<? super T, ? super U, ? extends R> combiner;
    final NbpObservable<? extends U> other;
    public NbpOperatorWithLatestFrom(BiFunction<? super T, ? super U, ? extends R> combiner, NbpObservable<? extends U> other) {
        this.combiner = combiner;
        this.other = other;
    }
    
    @Override
    public NbpSubscriber<? super T> apply(NbpSubscriber<? super R> t) {
        final NbpSerializedSubscriber<R> serial = new NbpSerializedSubscriber<R>(t);
        final WithLatestFromSubscriber<T, U, R> wlf = new WithLatestFromSubscriber<T, U, R>(serial, combiner);
        
        other.subscribe(new NbpSubscriber<U>() {
            @Override
            public void onSubscribe(Disposable s) {
                wlf.setOther(s);
            }
            
            @Override
            public void onNext(U t) {
                wlf.lazySet(t);
            }
            
            @Override
            public void onError(Throwable t) {
                wlf.otherError(t);
            }
            
            @Override
            public void onComplete() {
                // nothing to do, the wlf will complete on its own pace
            }
        });
        
        return wlf;
    }
    
    static final class WithLatestFromSubscriber<T, U, R> extends AtomicReference<U> implements NbpSubscriber<T>, Disposable {
        /** */
        private static final long serialVersionUID = -312246233408980075L;
        
        final NbpSubscriber<? super R> actual;
        final BiFunction<? super T, ? super U, ? extends R> combiner;
        
        final AtomicReference<Disposable> s = new AtomicReference<Disposable>();
        
        final AtomicReference<Disposable> other = new AtomicReference<Disposable>();
        
        static final Disposable CANCELLED = new Disposable() {
            @Override
            public void dispose() { }
        };
        
        public WithLatestFromSubscriber(NbpSubscriber<? super R> actual, BiFunction<? super T, ? super U, ? extends R> combiner) {
            this.actual = actual;
            this.combiner = combiner;
        }
        @Override
        public void onSubscribe(Disposable s) {
            if (this.s.compareAndSet(null, s)) {
                actual.onSubscribe(this);
            } else {
                s.dispose();
                if (this.s.get() != CANCELLED) {
                    SubscriptionHelper.reportDisposableSet();
                }
            }
        }
        
        @Override
        public void onNext(T t) {
            U u = get();
            if (u != null) {
                R r;
                try {
                    r = combiner.apply(t, u);
                } catch (Throwable e) {
                    dispose();
                    actual.onError(e);
                    return;
                }
                actual.onNext(r);
            }
        }
        
        @Override
        public void onError(Throwable t) {
            cancelOther();
            actual.onError(t);
        }
        
        @Override
        public void onComplete() {
            cancelOther();
            actual.onComplete();
        }
        
        @Override
        public void dispose() {
            s.get().dispose();
            cancelOther();
        }
        
        void cancelOther() {
            Disposable o = other.get();
            if (o != CANCELLED) {
                o = other.getAndSet(CANCELLED);
                if (o != CANCELLED && o != null) {
                    o.dispose();
                }
            }
        }
        
        public boolean setOther(Disposable o) {
            for (;;) {
                Disposable current = other.get();
                if (current == CANCELLED) {
                    o.dispose();
                    return false;
                }
                if (current != null) {
                    RxJavaPlugins.onError(new IllegalStateException("Other subscription already set!"));
                    o.dispose();
                    return false;
                }
                if (other.compareAndSet(null, o)) {
                    return true;
                }
            }
        }
        
        public void otherError(Throwable e) {
            if (this.s.compareAndSet(null, CANCELLED)) {
                EmptyDisposable.error(e, actual);
            } else {
                if (this.s.get() != CANCELLED) {
                    dispose();
                    actual.onError(e);
                } else {
                    RxJavaPlugins.onError(e);
                }
            }
        }
    }
}
