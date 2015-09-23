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

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

import io.reactivex.*;
import io.reactivex.NbpObservable.*;
import io.reactivex.Scheduler.Worker;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.disposables.NbpFullArbiter;
import io.reactivex.internal.subscribers.NbpFullArbiterSubscriber;
import io.reactivex.internal.subscriptions.SubscriptionHelper;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.subscribers.nbp.NbpSerializedSubscriber;

public final class NbpOperatorTimeoutTimed<T> implements NbpOperator<T, T> {
    final long timeout;
    final TimeUnit unit;
    final Scheduler scheduler;
    final NbpObservable<? extends T> other;
    
    public NbpOperatorTimeoutTimed(long timeout, TimeUnit unit, Scheduler scheduler, NbpObservable<? extends T> other) {
        this.timeout = timeout;
        this.unit = unit;
        this.scheduler = scheduler;
        this.other = other;
    }

    @Override
    public NbpSubscriber<? super T> apply(NbpSubscriber<? super T> t) {
        if (other == null) {
            return new TimeoutTimedSubscriber<>(
                    new NbpSerializedSubscriber<>(t), // because errors can race 
                    timeout, unit, scheduler.createWorker());
        }
        return new TimeoutTimedOtherSubscriber<>(
                t, // the FullArbiter serializes
                timeout, unit, scheduler.createWorker(), other);
    }
    
    static final class TimeoutTimedOtherSubscriber<T> implements NbpSubscriber<T>, Disposable {
        final NbpSubscriber<? super T> actual;
        final long timeout;
        final TimeUnit unit;
        final Scheduler.Worker worker;
        final NbpObservable<? extends T> other;
        
        Disposable s; 
        
        final NbpFullArbiter<T> arbiter;

        volatile Disposable timer;
        @SuppressWarnings("rawtypes")
        static final AtomicReferenceFieldUpdater<TimeoutTimedOtherSubscriber, Disposable> TIMER =
                AtomicReferenceFieldUpdater.newUpdater(TimeoutTimedOtherSubscriber.class, Disposable.class, "timer");

        static final Disposable CANCELLED = () -> { };

        static final Disposable NEW_TIMER = () -> { };

        volatile long index;
        
        volatile boolean done;
        
        public TimeoutTimedOtherSubscriber(NbpSubscriber<? super T> actual, long timeout, TimeUnit unit, Worker worker,
                NbpObservable<? extends T> other) {
            this.actual = actual;
            this.timeout = timeout;
            this.unit = unit;
            this.worker = worker;
            this.other = other;
            this.arbiter = new NbpFullArbiter<>(actual, this, 8);
        }

        @Override
        public void onSubscribe(Disposable s) {
            if (SubscriptionHelper.validateDisposable(this.s, s)) {
                return;
            }
            
            this.s = s;
            if (arbiter.setSubscription(s)) {
                actual.onSubscribe(arbiter);
                
                scheduleTimeout(0L);
            }
        }
        
        @Override
        public void onNext(T t) {
            if (done) {
                return;
            }
            long idx = index + 1;
            index = idx;
            
            if (arbiter.onNext(t, s)) {
                scheduleTimeout(idx);
            }
        }
        
        void scheduleTimeout(long idx) {
            Disposable d = timer;
            if (d != null) {
                d.dispose();
            }
            
            if (TIMER.compareAndSet(this, d, NEW_TIMER)) {
                d = worker.schedule(() -> {
                    if (idx == index) {
                        done = true;
                        s.dispose();
                        disposeTimer();
                        worker.dispose();
                        
                        if (other == null) {
                            actual.onError(new TimeoutException());
                        } else {
                            subscribeNext();
                        }
                    }
                }, timeout, unit);
                
                if (!TIMER.compareAndSet(this, NEW_TIMER, d)) {
                    d.dispose();
                }
            }
        }
        
        void subscribeNext() {
            other.subscribe(new NbpFullArbiterSubscriber<>(arbiter));
        }
        
        @Override
        public void onError(Throwable t) {
            if (done) {
                RxJavaPlugins.onError(t);
                return;
            }
            done = true;
            worker.dispose();
            disposeTimer();
            arbiter.onError(t, s);
        }
        
        @Override
        public void onComplete() {
            if (done) {
                return;
            }
            done = true;
            worker.dispose();
            disposeTimer();
            arbiter.onComplete(s);
        }
        
        @Override
        public void dispose() {
            worker.dispose();
            disposeTimer();
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
    }
    
    static final class TimeoutTimedSubscriber<T> implements NbpSubscriber<T>, Disposable {
        final NbpSubscriber<? super T> actual;
        final long timeout;
        final TimeUnit unit;
        final Scheduler.Worker worker;
        
        Disposable s; 
        
        volatile Disposable timer;
        @SuppressWarnings("rawtypes")
        static final AtomicReferenceFieldUpdater<TimeoutTimedSubscriber, Disposable> TIMER =
                AtomicReferenceFieldUpdater.newUpdater(TimeoutTimedSubscriber.class, Disposable.class, "timer");

        static final Disposable CANCELLED = () -> { };

        static final Disposable NEW_TIMER = () -> { };

        volatile long index;
        
        volatile boolean done;
        
        public TimeoutTimedSubscriber(NbpSubscriber<? super T> actual, long timeout, TimeUnit unit, Worker worker) {
            this.actual = actual;
            this.timeout = timeout;
            this.unit = unit;
            this.worker = worker;
        }

        @Override
        public void onSubscribe(Disposable s) {
            if (SubscriptionHelper.validateDisposable(this.s, s)) {
                return;
            }
            
            this.s = s;
            actual.onSubscribe(s);
            scheduleTimeout(0L);
        }
        
        @Override
        public void onNext(T t) {
            if (done) {
                return;
            }
            long idx = index + 1;
            index = idx;

            actual.onNext(t);
            
            scheduleTimeout(idx);
        }
        
        void scheduleTimeout(long idx) {
            Disposable d = timer;
            if (d != null) {
                d.dispose();
            }
            
            if (TIMER.compareAndSet(this, d, NEW_TIMER)) {
                d = worker.schedule(() -> {
                    if (idx == index) {
                        done = true;
                        s.dispose();
                        dispose();
                        
                        actual.onError(new TimeoutException());
                    }
                }, timeout, unit);
                
                if (!TIMER.compareAndSet(this, NEW_TIMER, d)) {
                    d.dispose();
                }
            }
        }
        
        @Override
        public void onError(Throwable t) {
            if (done) {
                RxJavaPlugins.onError(t);
                return;
            }
            done = true;
            dispose();
            
            actual.onError(t);
        }
        
        @Override
        public void onComplete() {
            if (done) {
                return;
            }
            done = true;
            dispose();
            
            actual.onComplete();
        }
        
        @Override
        public void dispose() {
            worker.dispose();
            disposeTimer();
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
    }
}
