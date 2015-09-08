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

import java.util.concurrent.atomic.AtomicBoolean;

import org.reactivestreams.*;

import io.reactivex.Observable.Operator;
import io.reactivex.Scheduler;
import io.reactivex.internal.subscriptions.SubscriptionHelper;

public final class OperatorUnsubscribeOn<T> implements Operator<T, T> {
    final Scheduler scheduler;
    public OperatorUnsubscribeOn(Scheduler scheduler) {
        this.scheduler = scheduler;
    }
    
    @Override
    public Subscriber<? super T> apply(Subscriber<? super T> t) {
        return new UnsubscribeSubscriber<>(t, scheduler);
    }
    
    static final class UnsubscribeSubscriber<T> extends AtomicBoolean implements Subscriber<T>, Subscription {
        /** */
        private static final long serialVersionUID = 1015244841293359600L;
        
        final Subscriber<? super T> actual;
        final Scheduler scheduler;
        
        Subscription s;
        
        public UnsubscribeSubscriber(Subscriber<? super T> actual, Scheduler scheduler) {
            this.actual = actual;
            this.scheduler = scheduler;
        }
        
        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.validateSubscription(this.s, s)) {
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
        public void request(long n) {
            s.request(n);
        }
        
        @Override
        public void cancel() {
            if (compareAndSet(false, true)) {
                scheduler.scheduleDirect(() -> {
                    s.cancel();
                });
            }
        }
    }
}
