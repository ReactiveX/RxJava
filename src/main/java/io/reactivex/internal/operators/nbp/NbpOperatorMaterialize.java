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

import java.util.Optional;

import io.reactivex.*;
import io.reactivex.NbpObservable.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.subscriptions.SubscriptionHelper;

public enum NbpOperatorMaterialize implements NbpOperator<Try<Optional<Object>>, Object> {
    INSTANCE;
    
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static <T> NbpOperator<Try<Optional<T>>, T> instance() {
        return (NbpOperator)INSTANCE;
    }
    
    @Override
    public NbpSubscriber<? super Object> apply(NbpSubscriber<? super Try<Optional<Object>>> t) {
        return new MaterializeSubscriber<>(t);
    }
    
    static final class MaterializeSubscriber<T> implements NbpSubscriber<T> {
        final NbpSubscriber<? super Try<Optional<T>>> actual;
        
        Disposable s;
        
        volatile boolean done;
        
        public MaterializeSubscriber(NbpSubscriber<? super Try<Optional<T>>> actual) {
            this.actual = actual;
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
            actual.onNext(Notification.next(t));
        }
        
        void tryEmit(Try<Optional<T>> v) {
                
        }
        
        @Override
        public void onError(Throwable t) {
            Try<Optional<T>> v = Notification.error(t);
            actual.onNext(v);
            actual.onComplete();
        }
        
        @Override
        public void onComplete() {
            Try<Optional<T>> v = Notification.complete();
            
            actual.onNext(v);
            actual.onComplete();
        }
    }
}
