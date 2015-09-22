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

import java.util.function.Consumer;

import io.reactivex.NbpObservable.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.subscriptions.SubscriptionHelper;
import io.reactivex.plugins.RxJavaPlugins;

public final class NbpOperatorDoOnEach<T> implements NbpOperator<T, T> {
    final Consumer<? super T> onNext;
    final Consumer<? super Throwable> onError;
    final Runnable onComplete;
    final Runnable onAfterTerminate;
    
    public NbpOperatorDoOnEach(Consumer<? super T> onNext, 
            Consumer<? super Throwable> onError, 
            Runnable onComplete,
            Runnable onAfterTerminate) {
        this.onNext = onNext;
        this.onError = onError;
        this.onComplete = onComplete;
        this.onAfterTerminate = onAfterTerminate;
    }
    
    @Override
    public NbpSubscriber<? super T> apply(NbpSubscriber<? super T> t) {
        return new DoOnEachSubscriber<>(t, onNext, onError, onComplete, onAfterTerminate);
    }
    
    static final class DoOnEachSubscriber<T> implements NbpSubscriber<T> {
        final NbpSubscriber<? super T> actual;
        final Consumer<? super T> onNext;
        final Consumer<? super Throwable> onError;
        final Runnable onComplete;
        final Runnable onAfterTerminate;
        
        Disposable s;
        
        boolean done;
        
        public DoOnEachSubscriber(
                NbpSubscriber<? super T> actual,
                Consumer<? super T> onNext, 
                Consumer<? super Throwable> onError, 
                Runnable onComplete,
                Runnable onAfterTerminate) {
            this.actual = actual;
            this.onNext = onNext;
            this.onError = onError;
            this.onComplete = onComplete;
            this.onAfterTerminate = onAfterTerminate;
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
            if (done) {
                return;
            }
            try {
                onNext.accept(t);
            } catch (Throwable e) {
                s.dispose();
                onError(e);
                return;
            }
            
            actual.onNext(t);
        }
        
        @Override
        public void onError(Throwable t) {
            if (done) {
                RxJavaPlugins.onError(t);
                return;
            }
            done = true;
            try {
                onError.accept(t);
            } catch (Throwable e) {
                t.addSuppressed(e);
            }
            actual.onError(t);
            
            try {
                onAfterTerminate.run();
            } catch (Throwable e) {
                RxJavaPlugins.onError(e);
            }
        }
        
        @Override
        public void onComplete() {
            if (done) {
                return;
            }
            done = true;
            try {
                onComplete.run();
            } catch (Throwable e) {
                onError(e);
                return;
            }
            
            actual.onComplete();
            
            try {
                onAfterTerminate.run();
            } catch (Throwable e) {
                RxJavaPlugins.onError(e);
            }
        }
    }
}
