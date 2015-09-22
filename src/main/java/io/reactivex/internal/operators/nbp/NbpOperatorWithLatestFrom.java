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
import java.util.function.BiFunction;

import io.reactivex.NbpObservable;
import io.reactivex.NbpObservable.*;
import io.reactivex.disposables.Disposable;
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
        NbpSerializedSubscriber<R> serial = new NbpSerializedSubscriber<>(t);
        WithLatestFromSubscriber<T, U, R> wlf = new WithLatestFromSubscriber<>(serial, combiner);
        
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
        
        volatile Disposable s;
        @SuppressWarnings("rawtypes")
        static final AtomicReferenceFieldUpdater<WithLatestFromSubscriber, Disposable> S =
                AtomicReferenceFieldUpdater.newUpdater(WithLatestFromSubscriber.class, Disposable.class, "s");
        
        volatile Disposable other;
        @SuppressWarnings("rawtypes")
        static final AtomicReferenceFieldUpdater<WithLatestFromSubscriber, Disposable> OTHER =
                AtomicReferenceFieldUpdater.newUpdater(WithLatestFromSubscriber.class, Disposable.class, "other");
        
        static final Disposable CANCELLED = () -> { };
        
        public WithLatestFromSubscriber(NbpSubscriber<? super R> actual, BiFunction<? super T, ? super U, ? extends R> combiner) {
            this.actual = actual;
            this.combiner = combiner;
        }
        @Override
        public void onSubscribe(Disposable s) {
            if (S.compareAndSet(this, null, s)) {
                actual.onSubscribe(this);
            } else {
                s.dispose();
                if (s != CANCELLED) {
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
            s.dispose();
            cancelOther();
        }
        
        void cancelOther() {
            Disposable o = other;
            if (o != CANCELLED) {
                o = OTHER.getAndSet(this, CANCELLED);
                if (o != CANCELLED && o != null) {
                    o.dispose();
                }
            }
        }
        
        public boolean setOther(Disposable o) {
            for (;;) {
                Disposable current = other;
                if (current == CANCELLED) {
                    o.dispose();
                    return false;
                }
                if (current != null) {
                    RxJavaPlugins.onError(new IllegalStateException("Other subscription already set!"));
                    o.dispose();
                    return false;
                }
                if (OTHER.compareAndSet(this, null, o)) {
                    return true;
                }
            }
        }
        
        public void otherError(Throwable e) {
            if (S.compareAndSet(this, null, CANCELLED)) {
                EmptyDisposable.error(e, actual);
            } else {
                if (s != CANCELLED) {
                    dispose();
                    actual.onError(e);
                } else {
                    RxJavaPlugins.onError(e);
                }
            }
        }
    }
}
