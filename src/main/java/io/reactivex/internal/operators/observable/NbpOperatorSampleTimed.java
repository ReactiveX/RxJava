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
import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.*;
import io.reactivex.Observable.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.subscriptions.SubscriptionHelper;
import io.reactivex.observers.SerializedObserver;

public final class NbpOperatorSampleTimed<T> implements NbpOperator<T, T> {
    final long period;
    final TimeUnit unit;
    final Scheduler scheduler;
    
    public NbpOperatorSampleTimed(long period, TimeUnit unit, Scheduler scheduler) {
        this.period = period;
        this.unit = unit;
        this.scheduler = scheduler;
    }
    
    
    @Override
    public Observer<? super T> apply(Observer<? super T> t) {
        SerializedObserver<T> serial = new SerializedObserver<T>(t);
        return new SampleTimedSubscriber<T>(serial, period, unit, scheduler);
    }
    
    static final class SampleTimedSubscriber<T> extends AtomicReference<T> implements Observer<T>, Disposable, Runnable {
        /** */
        private static final long serialVersionUID = -3517602651313910099L;

        final Observer<? super T> actual;
        final long period;
        final TimeUnit unit;
        final Scheduler scheduler;
        
        final AtomicReference<Disposable> timer = new AtomicReference<Disposable>();
        
        static final Disposable DISPOSED = new Disposable() {
            @Override
            public void dispose() { }
        };
        
        Disposable s;
        
        public SampleTimedSubscriber(Observer<? super T> actual, long period, TimeUnit unit, Scheduler scheduler) {
            this.actual = actual;
            this.period = period;
            this.unit = unit;
            this.scheduler = scheduler;
        }
        
        @Override
        public void onSubscribe(Disposable s) {
            if (SubscriptionHelper.validateDisposable(this.s, s)) {
                return;
            }
            
            this.s = s;
            actual.onSubscribe(this);
            if (timer.get() == null) {
                Disposable d = scheduler.schedulePeriodicallyDirect(this, period, period, unit);
                if (!timer.compareAndSet(null, d)) {
                    d.dispose();
                    return;
                }
            }
        }
        
        @Override
        public void onNext(T t) {
            lazySet(t);
        }
        
        @Override
        public void onError(Throwable t) {
            cancelTimer();
            actual.onError(t);
        }
        
        @Override
        public void onComplete() {
            cancelTimer();
            actual.onComplete();
        }
        
        void cancelTimer() {
            Disposable d = timer.get();
            if (d != DISPOSED) {
                d = timer.getAndSet(DISPOSED);
                if (d != DISPOSED && d != null) {
                    d.dispose();
                }
            }
        }
        
        @Override
        public void dispose() {
            cancelTimer();
            s.dispose();
        }
        
        @Override
        public void run() {
            T value = getAndSet(null);
            if (value != null) {
                actual.onNext(value);
            }
        }
    }
}
