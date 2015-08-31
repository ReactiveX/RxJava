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
import io.reactivex.Scheduler.Worker;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.subscriptions.SubscriptionHelper;
import io.reactivex.internal.util.BackpressureHelper;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.subscribers.SerializedSubscriber;

public final class OperatorDebounceTimed<T> implements Operator<T, T> {
    final long timeout;
    final TimeUnit unit;
    final Scheduler scheduler;

    public OperatorDebounceTimed(long timeout, TimeUnit unit, Scheduler scheduler) {
        this.timeout = timeout;
        this.unit = unit;
        this.scheduler = scheduler;
    }
    
    @Override
    public Subscriber<? super T> apply(Subscriber<? super T> t) {
        return new DebounceTimedSubscriber<>(
                new SerializedSubscriber<>(t), 
                timeout, unit, scheduler.createWorker());
    }
    
    static final class DebounceTimedSubscriber<T> extends AtomicInteger 
    implements Subscriber<T>, Subscription {
        /** */
        private static final long serialVersionUID = -9102637559663639004L;
        final Subscriber<? super T> actual;
        final long timeout;
        final TimeUnit unit;
        final Scheduler.Worker worker;
        
        Subscription s;
        
        volatile Disposable timer;
        @SuppressWarnings("rawtypes")
        static final AtomicReferenceFieldUpdater<DebounceTimedSubscriber, Disposable> TIMER =
                AtomicReferenceFieldUpdater.newUpdater(DebounceTimedSubscriber.class, Disposable.class, "timer");

        static final Disposable CANCELLED = () -> { };

        static final Disposable NEW_TIMER = () -> { };
        
        volatile long index;
        
        volatile long requested;
        @SuppressWarnings("rawtypes")
        static final AtomicLongFieldUpdater<DebounceTimedSubscriber> REQUESTED =
                AtomicLongFieldUpdater.newUpdater(DebounceTimedSubscriber.class, "requested");

        boolean done;
        
        public DebounceTimedSubscriber(Subscriber<? super T> actual, long timeout, TimeUnit unit, Worker worker) {
            this.actual = actual;
            this.timeout = timeout;
            this.unit = unit;
            this.worker = worker;
            lazySet(1);
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
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.validateSubscription(this.s, s)) {
                return;
            }
            
            this.s = s;
            actual.onSubscribe(this);
            s.request(Long.MAX_VALUE);
        }
        
        @Override
        public void onNext(T t) {
            if (done) {
                return;
            }
            long idx = index + 1;
            index = idx;
            
            Disposable d = timer;
            if (d != null) {
                d.dispose();
            }
            
            if (TIMER.compareAndSet(this, d, NEW_TIMER)) {
                getAndIncrement();
                d = worker.schedule(() -> {
                    if (idx == index) {
                        long r = requested;
                        if (r != 0L) {
                            actual.onNext(t);
                            if (r != Long.MAX_VALUE) {
                                REQUESTED.decrementAndGet(this);
                            }
                            
                            if (decrementAndGet() == 0) {
                                disposeTimer();
                                worker.dispose();
                                actual.onComplete();
                            }
                        } else {
                            cancel();
                            actual.onError(new IllegalStateException("Could not deliver value due to lack of requests"));
                        }
                    }
                }, timeout, unit);
                
                if (TIMER.compareAndSet(this, NEW_TIMER, d)) {
                    if (timer == CANCELLED) {
                        d.dispose();
                    }
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
            disposeTimer();
            actual.onError(t);
        }
        
        @Override
        public void onComplete() {
            if (done) {
                return;
            }
            done = true;
            if (decrementAndGet() == 0) {
                disposeTimer();
                worker.dispose();
                actual.onComplete();
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
            disposeTimer();
            worker.dispose();
            s.cancel();
        }
    }
}
