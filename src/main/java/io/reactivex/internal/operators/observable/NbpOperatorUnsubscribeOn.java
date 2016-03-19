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
import io.reactivex.internal.subscriptions.SubscriptionHelper;

public final class NbpOperatorUnsubscribeOn<T> implements NbpOperator<T, T> {
    final Scheduler scheduler;
    public NbpOperatorUnsubscribeOn(Scheduler scheduler) {
        this.scheduler = scheduler;
    }
    
    @Override
    public Observer<? super T> apply(Observer<? super T> t) {
        return new UnsubscribeSubscriber<T>(t, scheduler);
    }
    
    static final class UnsubscribeSubscriber<T> extends AtomicBoolean implements Observer<T>, Disposable {
        /** */
        private static final long serialVersionUID = 1015244841293359600L;
        
        final Observer<? super T> actual;
        final Scheduler scheduler;
        
        Disposable s;
        
        public UnsubscribeSubscriber(Observer<? super T> actual, Scheduler scheduler) {
            this.actual = actual;
            this.scheduler = scheduler;
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
            actual.onError(t);
        }
        
        @Override
        public void onComplete() {
            actual.onComplete();
        }
        
        @Override
        public void dispose() {
            if (compareAndSet(false, true)) {
                scheduler.scheduleDirect(new Runnable() {
                    @Override
                    public void run() {
                        s.dispose();
                    }
                });
            }
        }
    }
}
