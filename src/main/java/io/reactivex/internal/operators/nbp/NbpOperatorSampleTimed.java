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
import java.util.concurrent.atomic.*;

import io.reactivex.NbpObservable.*;
import io.reactivex.Scheduler;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.subscriptions.SubscriptionHelper;
import io.reactivex.subscribers.nbp.NbpSerializedSubscriber;

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
    public NbpSubscriber<? super T> apply(NbpSubscriber<? super T> t) {
        NbpSerializedSubscriber<T> serial = new NbpSerializedSubscriber<>(t);
        return new SampleTimedSubscriber<>(serial, period, unit, scheduler);
    }
    
    static final class SampleTimedSubscriber<T> extends AtomicReference<T> implements NbpSubscriber<T>, Disposable, Runnable {
        /** */
        private static final long serialVersionUID = -3517602651313910099L;

        final NbpSubscriber<? super T> actual;
        final long period;
        final TimeUnit unit;
        final Scheduler scheduler;
        
        volatile Disposable timer;
        @SuppressWarnings("rawtypes")
        static final AtomicReferenceFieldUpdater<SampleTimedSubscriber, Disposable> TIMER =
                AtomicReferenceFieldUpdater.newUpdater(SampleTimedSubscriber.class, Disposable.class, "timer");
        
        static final Disposable DISPOSED = () -> { };
        
        Disposable s;
        
        public SampleTimedSubscriber(NbpSubscriber<? super T> actual, long period, TimeUnit unit, Scheduler scheduler) {
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
            if (timer == null) {
                Disposable d = scheduler.schedulePeriodicallyDirect(this, period, period, unit);
                if (!TIMER.compareAndSet(this, null, d)) {
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
            Disposable d = timer;
            if (d != DISPOSED) {
                d = TIMER.getAndSet(this, DISPOSED);
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
