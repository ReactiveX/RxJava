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

package io.reactivex.internal.operators;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.*;

import org.reactivestreams.*;

import io.reactivex.Observable.Operator;
import io.reactivex.Scheduler;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.subscriptions.SubscriptionHelper;
import io.reactivex.internal.util.BackpressureHelper;
import io.reactivex.subscribers.SerializedSubscriber;

public final class OperatorSampleTimed<T> implements Operator<T, T> {
    final long period;
    final TimeUnit unit;
    final Scheduler scheduler;
    
    public OperatorSampleTimed(long period, TimeUnit unit, Scheduler scheduler) {
        this.period = period;
        this.unit = unit;
        this.scheduler = scheduler;
    }
    
    
    @Override
    public Subscriber<? super T> apply(Subscriber<? super T> t) {
        SerializedSubscriber<T> serial = new SerializedSubscriber<>(t);
        return new SampleTimedSubscriber<>(serial, period, unit, scheduler);
    }
    
    static final class SampleTimedSubscriber<T> extends AtomicReference<T> implements Subscriber<T>, Subscription, Runnable {
        /** */
        private static final long serialVersionUID = -3517602651313910099L;

        final Subscriber<? super T> actual;
        final long period;
        final TimeUnit unit;
        final Scheduler scheduler;
        
        volatile long requested;
        @SuppressWarnings("rawtypes")
        static final AtomicLongFieldUpdater<SampleTimedSubscriber> REQUESTED =
                AtomicLongFieldUpdater.newUpdater(SampleTimedSubscriber.class, "requested");

        volatile Disposable timer;
        @SuppressWarnings("rawtypes")
        static final AtomicReferenceFieldUpdater<SampleTimedSubscriber, Disposable> TIMER =
                AtomicReferenceFieldUpdater.newUpdater(SampleTimedSubscriber.class, Disposable.class, "timer");
        
        static final Disposable DISPOSED = () -> { };
        
        Subscription s;
        
        public SampleTimedSubscriber(Subscriber<? super T> actual, long period, TimeUnit unit, Scheduler scheduler) {
            this.actual = actual;
            this.period = period;
            this.unit = unit;
            this.scheduler = scheduler;
        }
        
        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.validateSubscription(this.s, s)) {
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
                s.request(Long.MAX_VALUE);
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
        public void request(long n) {
            if (SubscriptionHelper.validateRequest(n)) {
                return;
            }
            
            BackpressureHelper.add(REQUESTED, this, n);
        }
        
        @Override
        public void cancel() {
            cancelTimer();
            s.cancel();
        }
        
        @Override
        public void run() {
            T value = getAndSet(null);
            if (value != null) {
                long r = requested;
                if (r != 0L) {
                    actual.onNext(value);
                    if (r != Long.MAX_VALUE) {
                        REQUESTED.decrementAndGet(this);
                    }
                } else {
                    cancel();
                    actual.onError(new IllegalStateException("Couldn't emit value due to lack of requests!"));
                }
            }
        }
    }
}
