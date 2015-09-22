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

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

import io.reactivex.NbpObservable.*;
import io.reactivex.Scheduler;
import io.reactivex.Scheduler.Worker;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.subscriptions.SubscriptionHelper;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.subscribers.nbp.NbpSerializedSubscriber;

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
    public NbpSubscriber<? super T> apply(NbpSubscriber<? super T> t) {
        return new DebounceTimedSubscriber<>(
                new NbpSerializedSubscriber<>(t), 
                timeout, unit, scheduler.createWorker());
    }
    
    static final class DebounceTimedSubscriber<T>
    implements NbpSubscriber<T>, Disposable, Runnable {
        final NbpSubscriber<? super T> actual;
        final long timeout;
        final TimeUnit unit;
        final Scheduler.Worker worker;
        
        Disposable s;
        
        volatile Disposable timer;
        @SuppressWarnings("rawtypes")
        static final AtomicReferenceFieldUpdater<DebounceTimedSubscriber, Disposable> TIMER =
                AtomicReferenceFieldUpdater.newUpdater(DebounceTimedSubscriber.class, Disposable.class, "timer");

        static final Disposable CANCELLED = () -> { };

        static final Disposable NEW_TIMER = () -> { };

        volatile boolean gate;
        
        boolean done;
        
        public DebounceTimedSubscriber(NbpSubscriber<? super T> actual, long timeout, TimeUnit unit, Worker worker) {
            this.actual = actual;
            this.timeout = timeout;
            this.unit = unit;
            this.worker = worker;
        }
        
        public void disposeTimer() {
            Disposable d = timer;
            if (d != CANCELLED) {
                d = TIMER.getAndSet(this, CANCELLED);
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
                Disposable d = timer;
                if (d != null) {
                    d.dispose();
                }
                
                if (TIMER.compareAndSet(this, d, NEW_TIMER)) {
                    d = worker.schedule(this, timeout, unit);
                    if (!TIMER.compareAndSet(this, NEW_TIMER, d)) {
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
