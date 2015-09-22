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

public enum NbpOperatorCount implements NbpOperator<Long, Object> {
    INSTANCE;
    
    @SuppressWarnings("unchecked")
    public static <T> NbpOperator<Long, T> instance() {
        return (NbpOperator<Long, T>)INSTANCE;
    }
    
    @Override
    public NbpSubscriber<? super Object> apply(NbpSubscriber<? super Long> t) {
        return new CountSubscriber(t);
    }
    
    static final class CountSubscriber implements NbpSubscriber<Object> {
        final NbpSubscriber<? super Long> actual;
        
        Disposable s;
        
        long count;
        
        public CountSubscriber(NbpSubscriber<? super Long> actual) {
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
        public void onNext(Object t) {
            count++;
        }
        
        @Override
        public void onError(Throwable t) {
            actual.onError(t);
        }
        
        @Override
        public void onComplete() {
            actual.onNext(count);
            actual.onComplete();
        }
    }
}
