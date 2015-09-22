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

import java.util.function.Function;

import io.reactivex.NbpObservable.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.subscriptions.SubscriptionHelper;
import io.reactivex.plugins.RxJavaPlugins;

/**
 * 
 */
public final class NbpOperatorMap<T, U> implements NbpOperator<U, T> {
    final Function<? super T, ? extends U> function;
    
    public NbpOperatorMap(Function<? super T, ? extends U> function) {
        this.function = function;
    }
    
    @Override
    public NbpSubscriber<? super T> apply(NbpSubscriber<? super U> t) {
        return new MapperSubscriber<>(t, function);
    }
    
    static final class MapperSubscriber<T, U> implements NbpSubscriber<T> {
        final NbpSubscriber<? super U> actual;
        final Function<? super T, ? extends U> function;
        
        Disposable subscription;
        
        boolean done;
        
        public MapperSubscriber(NbpSubscriber<? super U> actual, Function<? super T, ? extends U> function) {
            this.actual = actual;
            this.function = function;
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
            if (done) {
                return;
            }
            U u;
            try {
                u = function.apply(t);
            } catch (Throwable e) {
                done = true;
                subscription.dispose();
                actual.onError(e);
                return;
            }
            if (u == null) {
                done = true;
                subscription.dispose();
                actual.onError(new NullPointerException("Value returned by the function is null"));
                return;
            }
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
