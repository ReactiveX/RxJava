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

import io.reactivex.NbpObservable.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.subscriptions.SubscriptionHelper;

public enum NbpOperatorTakeLastOne implements NbpOperator<Object, Object> {
    INSTANCE
    ;
    
    @SuppressWarnings("unchecked")
    public static <T> NbpOperator<T, T> instance() {
        return (NbpOperator<T, T>)INSTANCE;
    }
    
    @Override
    public NbpSubscriber<? super Object> apply(NbpSubscriber<? super Object> s) {
        return new TakeLastOneSubscriber<>(s);
    }
    
    static final class TakeLastOneSubscriber<T> implements NbpSubscriber<T>, Disposable {
        final NbpSubscriber<? super T> actual;
        
        Disposable s;
        
        T value;
        
        public TakeLastOneSubscriber(NbpSubscriber<? super T> actual) {
            this.actual = actual;
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
            value = t;
        }
        
        @Override
        public void onError(Throwable t) {
            value = null;
            actual.onError(t);
        }
        
        @Override
        public void onComplete() {
                emit();
        }
        
        void emit() {
            T v = value;
            if (v != null) {
                value = null;
                actual.onNext(v);
            }
            actual.onComplete();
        }
        
        @Override
        public void dispose() {
            value = null;
            s.dispose();
        }
    }
}
