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

import java.util.concurrent.TimeUnit;

import io.reactivex.*;
import io.reactivex.Observable.*;
import io.reactivex.Scheduler.Worker;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.subscriptions.SubscriptionHelper;
import io.reactivex.observers.SerializedObserver;

public final class NbpOperatorDelay<T> implements NbpOperator<T, T> {
    final long delay;
    final TimeUnit unit;
    final Scheduler scheduler;
    final boolean delayError;
    
    public NbpOperatorDelay(long delay, TimeUnit unit, Scheduler scheduler, boolean delayError) {
        this.delay = delay;
        this.unit = unit;
        this.scheduler = scheduler;
        this.delayError = delayError;
    }



    @Override
    @SuppressWarnings("unchecked")
    public Observer<? super T> apply(Observer<? super T> t) {
        Observer<T> s;
        if (delayError) {
            s = (Observer<T>)t;
        } else {
            s = new SerializedObserver<T>(t);
        }
        
        Scheduler.Worker w = scheduler.createWorker();
        
        return new DelaySubscriber<T>(s, delay, unit, w, delayError);
    }
    
    static final class DelaySubscriber<T> implements Observer<T>, Disposable {
        final Observer<? super T> actual;
        final long delay;
        final TimeUnit unit;
        final Scheduler.Worker w;
        final boolean delayError;
        
        Disposable s;
        
        public DelaySubscriber(Observer<? super T> actual, long delay, TimeUnit unit, Worker w, boolean delayError) {
            super();
            this.actual = actual;
            this.delay = delay;
            this.unit = unit;
            this.w = w;
            this.delayError = delayError;
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
        public void onNext(final T t) {
            w.schedule(new Runnable() {
                @Override
                public void run() {
                    actual.onNext(t);
                }
            }, delay, unit);
        }
        
        @Override
        public void onError(final Throwable t) {
            if (delayError) {
                w.schedule(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            actual.onError(t);
                        } finally {
                            w.dispose();
                        }
                    }
                }, delay, unit);
            } else {
                actual.onError(t);
            }
        }
        
        @Override
        public void onComplete() {
            w.schedule(new Runnable() {
                @Override
                public void run() {
                    try {
                        actual.onComplete();
                    } finally {
                        w.dispose();
                    }
                }
            }, delay, unit);
        }
        
        @Override
        public void dispose() {
            w.dispose();
            s.dispose();
        }
        
    }
}
