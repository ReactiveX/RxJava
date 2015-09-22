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

import java.util.function.Predicate;

import io.reactivex.NbpObservable.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.subscriptions.SubscriptionHelper;

public final class NbpOperatorFilter<T> implements NbpOperator<T, T> {
    final Predicate<? super T> predicate;
    public NbpOperatorFilter(Predicate<? super T> predicate) {
        this.predicate = predicate;
    }
    
    @Override
    public NbpSubscriber<? super T> apply(NbpSubscriber<? super T> s) {
        return new FilterSubscriber<>(s, predicate);
    }
    
    static final class FilterSubscriber<T> implements NbpSubscriber<T> {
        final Predicate<? super T> filter;
        final NbpSubscriber<? super T> actual;
        
        Disposable subscription;
        
        public FilterSubscriber(NbpSubscriber<? super T> actual, Predicate<? super T> filter) {
            this.actual = actual;
            this.filter = filter;
        }
        
        @Override
        public void onSubscribe(Disposable s) {
            if (SubscriptionHelper.validateDisposable(this.subscription, s)) {
                return;
            }
            subscription = s;
            actual.onSubscribe(s);
        }
        
        @Override
        public void onNext(T t) {
            boolean b;
            try {
                b = filter.test(t);
            } catch (Throwable e) {
                subscription.dispose();
                actual.onError(e);
                return;
            }
            if (b) {
                actual.onNext(t);
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
