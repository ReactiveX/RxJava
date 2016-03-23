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

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.*;
import io.reactivex.Observable.*;
import io.reactivex.Scheduler.Worker;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.disposables.NbpFullArbiter;
import io.reactivex.internal.subscribers.flowable.NbpFullArbiterSubscriber;
import io.reactivex.internal.subscriptions.SubscriptionHelper;
import io.reactivex.observers.SerializedObserver;
import io.reactivex.plugins.RxJavaPlugins;

public final class NbpOperatorTimeoutTimed<T> implements NbpOperator<T, T> {
    final long timeout;
    final TimeUnit unit;
    final Scheduler scheduler;
    final Observable<? extends T> other;
    
    public NbpOperatorTimeoutTimed(long timeout, TimeUnit unit, Scheduler scheduler, Observable<? extends T> other) {
        this.timeout = timeout;
        this.unit = unit;
        this.scheduler = scheduler;
        this.other = other;
    }

    @Override
    public Observer<? super T> apply(Observer<? super T> t) {
        if (other == null) {
            return new TimeoutTimedSubscriber<T>(
                    new SerializedObserver<T>(t), // because errors can race 
                    timeout, unit, scheduler.createWorker());
        }
        return new TimeoutTimedOtherSubscriber<T>(
                t, // the FullArbiter serializes
                timeout, unit, scheduler.createWorker(), other);
    }
    
    static final class TimeoutTimedOtherSubscriber<T> implements Observer<T>, Disposable {
        final Observer<? super T> actual;
        final long timeout;
        final TimeUnit unit;
        final Scheduler.Worker worker;
        final Observable<? extends T> other;
        
        Disposable s; 
        
        final NbpFullArbiter<T> arbiter;

        final AtomicReference<Disposable> timer = new AtomicReference<Disposable>();

        static final Disposable CANCELLED = new Disposable() {
            @Override
            public void dispose() { }
        };

        static final Disposable NEW_TIMER = new Disposable() {
            @Override
            public void dispose() { }
        };

        volatile long index;
        
        volatile boolean done;
        
        public TimeoutTimedOtherSubscriber(Observer<? super T> actual, long timeout, TimeUnit unit, Worker worker,
                Observable<? extends T> other) {
            this.actual = actual;
            this.timeout = timeout;
            this.unit = unit;
            this.worker = worker;
            this.other = other;
            this.arbiter = new NbpFullArbiter<T>(actual, this, 8);
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
        
        void scheduleTimeout(final long idx) {
            Disposable d = timer.get();
            if (d != null) {
                d.dispose();
            }
            
            if (timer.compareAndSet(d, NEW_TIMER)) {
                d = worker.schedule(new Runnable() {
                    @Override
                    public void run() {
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
                    }
                }, timeout, unit);
                
                if (!timer.compareAndSet(NEW_TIMER, d)) {
                    d.dispose();
                }
            }
        }
        
        void subscribeNext() {
            other.subscribe(new NbpFullArbiterSubscriber<T>(arbiter));
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
            Disposable d = timer.get();
            if (d != CANCELLED) {
                d = timer.getAndSet(CANCELLED);
                if (d != CANCELLED && d != null) {
                    d.dispose();
                }
            }
        }
    }
    
    static final class TimeoutTimedSubscriber<T> implements Observer<T>, Disposable {
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

        volatile long index;
        
        volatile boolean done;
        
        public TimeoutTimedSubscriber(Observer<? super T> actual, long timeout, TimeUnit unit, Worker worker) {
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
        
        void scheduleTimeout(final long idx) {
            Disposable d = timer.get();
            if (d != null) {
                d.dispose();
            }
            
            if (timer.compareAndSet(d, NEW_TIMER)) {
                d = worker.schedule(new Runnable() {
                    @Override
                    public void run() {
                        if (idx == index) {
                            done = true;
                            s.dispose();
                            dispose();
                            
                            actual.onError(new TimeoutException());
                        }
                    }
                }, timeout, unit);
                
                if (!timer.compareAndSet(NEW_TIMER, d)) {
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
            Disposable d = timer.get();
            if (d != CANCELLED) {
                d = timer.getAndSet(CANCELLED);
                if (d != CANCELLED && d != null) {
                    d.dispose();
                }
            }
        }
    }
}
