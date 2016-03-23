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

package io.reactivex.internal.operators.observable;

import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.*;
import io.reactivex.Observable.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.CompositeException;
import io.reactivex.functions.*;
import io.reactivex.internal.disposables.EmptyDisposable;
import io.reactivex.internal.subscriptions.SubscriptionHelper;
import io.reactivex.plugins.RxJavaPlugins;

public final class NbpOnSubscribeUsing<T, D> implements NbpOnSubscribe<T> {
    final Supplier<? extends D> resourceSupplier;
    final Function<? super D, ? extends Observable<? extends T>> sourceSupplier;
    final Consumer<? super D> disposer;
    final boolean eager;
    
    public NbpOnSubscribeUsing(Supplier<? extends D> resourceSupplier,
            Function<? super D, ? extends Observable<? extends T>> sourceSupplier, 
            Consumer<? super D> disposer,
            boolean eager) {
        this.resourceSupplier = resourceSupplier;
        this.sourceSupplier = sourceSupplier;
        this.disposer = disposer;
        this.eager = eager;
    }
    
    @Override
    public void accept(Observer<? super T> s) {
        D resource;
        
        try {
            resource = resourceSupplier.get();
        } catch (Throwable e) {
            EmptyDisposable.error(e, s);
            return;
        }
        
        Observable<? extends T> source;
        try {
            source = sourceSupplier.apply(resource);
        } catch (Throwable e) {
            try {
                disposer.accept(resource);
            } catch (Throwable ex) {
                EmptyDisposable.error(new CompositeException(ex, e), s);
                return;
            }
            EmptyDisposable.error(e, s);
            return;
        }
        
        UsingSubscriber<T, D> us = new UsingSubscriber<T, D>(s, resource, disposer, eager);
        
        source.subscribe(us);
    }
    
    static final class UsingSubscriber<T, D> extends AtomicBoolean implements Observer<T>, Disposable {
        /** */
        private static final long serialVersionUID = 5904473792286235046L;
        
        final Observer<? super T> actual;
        final D resource;
        final Consumer<? super D> disposer;
        final boolean eager;
        
        Disposable s;

        public UsingSubscriber(Observer<? super T> actual, D resource, Consumer<? super D> disposer, boolean eager) {
            this.actual = actual;
            this.resource = resource;
            this.disposer = disposer;
            this.eager = eager;
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
            actual.onNext(t);
        }
        
        @Override
        public void onError(Throwable t) {
            if (eager) {
                if (compareAndSet(false, true)) {
                    try {
                        disposer.accept(resource);
                    } catch (Throwable e) {
                        t = new CompositeException(e, t);
                    }
                }
                
                s.dispose();
                actual.onError(t);
            } else {
                actual.onError(t);
                s.dispose();
                disposeAfter();
            }
        }
        
        @Override
        public void onComplete() {
            if (eager) {
                if (compareAndSet(false, true)) {
                    try {
                        disposer.accept(resource);
                    } catch (Throwable e) {
                        actual.onError(e);
                        return;
                    }
                }
                
                s.dispose();
                actual.onComplete();
            } else {
                actual.onComplete();
                s.dispose();
                disposeAfter();
            }
        }
        
        @Override
        public void dispose() {
            disposeAfter();
            s.dispose();
        }
        
        void disposeAfter() {
            if (compareAndSet(false, true)) {
                try {
                    disposer.accept(resource);
                } catch (Throwable e) {
                    // can't call actual.onError unless it is serialized, which is expensive
                    RxJavaPlugins.onError(e);
                }
            }
        }
    }
}
