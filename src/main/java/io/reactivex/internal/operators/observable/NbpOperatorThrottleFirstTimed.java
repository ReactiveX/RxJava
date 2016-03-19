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
import io.reactivex.Scheduler.Worker;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.subscriptions.SubscriptionHelper;
import io.reactivex.observers.SerializedObserver;
import io.reactivex.plugins.RxJavaPlugins;

public final class NbpOperatorThrottleFirstTimed<T> implements NbpOperator<T, T> {
    final long timeout;
    final TimeUnit unit;
    final Scheduler scheduler;

    public NbpOperatorThrottleFirstTimed(long timeout, TimeUnit unit, Scheduler scheduler) {
        this.timeout = timeout;
        this.unit = unit;
        this.scheduler = scheduler;
    }
    
    @Override
    public Observer<? super T> apply(Observer<? super T> t) {
        return new DebounceTimedSubscriber<T>(
                new SerializedObserver<T>(t), 
                timeout, unit, scheduler.createWorker());
    }
    
    static final class DebounceTimedSubscriber<T>
    implements Observer<T>, Disposable, Runnable {
        final Observer<? super T> actual;
        final long timeout;
        final TimeUnit unit;
        final Scheduler.Worker worker;
        
        Disposable s;
        
        final AtomicReference<Disposable> timer = new AtomicReference<Disposable>();

        static final Disposable CANCELLED = new Disposable() {
            @Override
            public void dispose() { }
        };

        static final Disposable NEW_TIMER = new Disposable() {
            @Override
            public void dispose() { }
        };

        volatile boolean gate;
        
        boolean done;
        
        public DebounceTimedSubscriber(Observer<? super T> actual, long timeout, TimeUnit unit, Worker worker) {
            this.actual = actual;
            this.timeout = timeout;
            this.unit = unit;
            this.worker = worker;
        }
        
        public void disposeTimer() {
            Disposable d = timer.get();
            if (d != CANCELLED) {
                d = timer.getAndSet(CANCELLED);
                if (d != CANCELLED && d != null) {
                    d.dispose();
                }
            }
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
            if (done) {
                return;
            }

            if (!gate) {
                gate = true;
                
                actual.onNext(t);
                
                // FIXME should this be a periodic blocking or a value-relative blocking?
                Disposable d = timer.get();
                if (d != null) {
                    d.dispose();
                }
                
                if (timer.compareAndSet(d, NEW_TIMER)) {
                    d = worker.schedule(this, timeout, unit);
                    if (!timer.compareAndSet(NEW_TIMER, d)) {
                        d.dispose();
                    }
                }
            }
            
            
        }
        
        @Override
        public void run() {
            gate = false;
        }
        
        @Override
        public void onError(Throwable t) {
            if (done) {
                RxJavaPlugins.onError(t);
                return;
            }
            done = true;
            disposeTimer();
            actual.onError(t);
        }
        
        @Override
        public void onComplete() {
            if (done) {
                return;
            }
            done = true;
            disposeTimer();
            worker.dispose();
            actual.onComplete();
        }
        
        @Override
        public void dispose() {
            disposeTimer();
            worker.dispose();
            s.dispose();
        }
    }
}
