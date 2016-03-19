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
import io.reactivex.functions.BiFunction;
import io.reactivex.internal.subscriptions.SubscriptionHelper;

public final class NbpOperatorScan<T> implements NbpOperator<T, T> {
    final BiFunction<T, T, T> accumulator;
    public NbpOperatorScan(BiFunction<T, T, T> accumulator) {
        this.accumulator = accumulator;
    }
    
    @Override
    public Observer<? super T> apply(Observer<? super T> t) {
        return new ScanSubscriber<T>(t, accumulator);
    }
    
    static final class ScanSubscriber<T> implements Observer<T> {
        final Observer<? super T> actual;
        final BiFunction<T, T, T> accumulator;
        
        Disposable s;
        
        T value;

        public ScanSubscriber(Observer<? super T> actual, BiFunction<T, T, T> accumulator) {
            this.actual = actual;
            this.accumulator = accumulator;
        }
        
        @Override
        public void onSubscribe(Disposable s) {
            if (SubscriptionHelper.validateDisposable(this.s, s)) {
                return;
            }
            this.s = s;
            actual.onSubscribe(s);
        }
        
        @Override
        public void onNext(T t) {
            final Observer<? super T> a = actual;
            T v = value;
            if (v == null) {
                value = t;
                a.onNext(t);
            } else {
                T u;
                
                try {
                    u = accumulator.apply(v, t);
                } catch (Throwable e) {
                    s.dispose();
                    a.onError(e);
                    return;
                }
                
                if (u == null) {
                    s.dispose();
                    a.onError(new NullPointerException("The value returned by the accumulator is null"));
                    return;
                }
                
                value = u;
                a.onNext(u);
            }
        }
        
        @Override
        public void onError(Throwable t) {
            actual.onError(t);
        }
        
        @Override
        public void onComplete() {
            actual.onComplete();
        }
    }
}
