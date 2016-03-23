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

import io.reactivex.Observer;
import io.reactivex.Observable.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.*;
import io.reactivex.internal.disposables.EmptyDisposable;
import io.reactivex.internal.subscribers.observable.NbpEmptySubscriber;
import io.reactivex.internal.subscriptions.SubscriptionHelper;
import io.reactivex.plugins.RxJavaPlugins;

public final class NbpOperatorScanSeed<T, R> implements NbpOperator<R, T> {
    final BiFunction<R, ? super T, R> accumulator;
    final Supplier<R> seedSupplier;

    public NbpOperatorScanSeed(Supplier<R> seedSupplier, BiFunction<R, ? super T, R> accumulator) {
        this.accumulator = accumulator;
        this.seedSupplier = seedSupplier;
    }
    
    @Override
    public Observer<? super T> apply(Observer<? super R> t) {
        R r;
        
        try {
            r = seedSupplier.get();
        } catch (Throwable e) {
            EmptyDisposable.error(e, t);
            return NbpEmptySubscriber.INSTANCE;
        }
        
        if (r == null) {
            EmptyDisposable.error(new NullPointerException("The seed supplied is null"), t);
            return NbpEmptySubscriber.INSTANCE;
        }
        
        return new ScanSeedSubscriber<T, R>(t, accumulator, r);
    }
    
    static final class ScanSeedSubscriber<T, R> implements Observer<T> {
        final Observer<? super R> actual;
        final BiFunction<R, ? super T, R> accumulator;
        
        R value;
        
        Disposable s;
        
        boolean done;
        
        public ScanSeedSubscriber(Observer<? super R> actual, BiFunction<R, ? super T, R> accumulator, R value) {
            this.actual = actual;
            this.accumulator = accumulator;
            this.value = value;
        }

        @Override
        public void onSubscribe(Disposable s) {
            if (SubscriptionHelper.validateDisposable(this.s, s)) {
                return;
            }
            this.s = s;
            actual.onSubscribe(s);
            actual.onNext(value);
        }
        
        @Override
        public void onNext(T t) {
            R v = value;
            
            R u;
            
            try {
                u = accumulator.apply(v, t);
            } catch (Throwable e) {
                s.dispose();
                onError(e);
                return;
            }
            
            if (u == null) {
                s.dispose();
                onError(new NullPointerException("The accumulator returned a null value"));
                return;
            }
            
            value = u;
            
            actual.onNext(u);
        }
        
        @Override
        public void onError(Throwable t) {
            if (done) {
                RxJavaPlugins.onError(t);
                return;
            }
            done = true;
            actual.onError(t);
        }
        
        @Override
        public void onComplete() {
            if (done) {
                return;
            }
            done = true;
            actual.onComplete();
        }
    }
}
