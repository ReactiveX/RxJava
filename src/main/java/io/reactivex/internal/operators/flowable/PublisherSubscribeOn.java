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

package io.reactivex.internal.operators.flowable;

import java.util.concurrent.atomic.AtomicReference;

import org.reactivestreams.*;

import io.reactivex.Scheduler;
import io.reactivex.internal.subscriptions.SubscriptionHelper;

public final class PublisherSubscribeOn<T> implements Publisher<T> {
    final Publisher<? extends T> source;
    final Scheduler scheduler;
    final boolean requestOn;
    
    public PublisherSubscribeOn(Publisher<? extends T> source, Scheduler scheduler, boolean requestOn) {
        this.source = source;
        this.scheduler = scheduler;
        this.requestOn = requestOn;
    }
    
    @Override
    public void subscribe(final Subscriber<? super T> s) {
        /*
         * TODO can't use the returned disposable because to dispose it,
         * one must set a Subscription on s on the current thread, but
         * it is expected that onSubscribe is run on the target scheduler.
         */
        if (requestOn) {
            Scheduler.Worker w = scheduler.createWorker();
            final SubscribeOnSubscriber<T> sos = new SubscribeOnSubscriber<T>(s, w);
            w.schedule(new Runnable() {
                @Override
                public void run() {
                    source.subscribe(sos);
                }
            });
        } else {
            scheduler.scheduleDirect(new Runnable() {
                @Override
                public void run() {
                    source.subscribe(s);
                }
            });
        }
    }
    
    static final class SubscribeOnSubscriber<T> extends AtomicReference<Thread> implements Subscriber<T>, Subscription {
        /** */
        private static final long serialVersionUID = 8094547886072529208L;
        final Subscriber<? super T> actual;
        final Scheduler.Worker worker;
        
        Subscription s;
        
        public SubscribeOnSubscriber(Subscriber<? super T> actual, Scheduler.Worker worker) {
            this.actual = actual;
            this.worker = worker;
        }
        
        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.validateSubscription(this.s, s)) {
                return;
            }
            this.s = s;
            lazySet(Thread.currentThread());
            actual.onSubscribe(this);
        }
        
        @Override
        public void onNext(T t) {
            actual.onNext(t);
        }
        
        @Override
        public void onError(Throwable t) {
            try {
                actual.onError(t);
            } finally {
                worker.dispose();
            }
        }
        
        @Override
        public void onComplete() {
            try {
                actual.onComplete();
            } finally {
                worker.dispose();
            }
        }
        
        @Override
        public void request(final long n) {
            if (SubscriptionHelper.validateRequest(n)) {
                return;
            }
            if (Thread.currentThread() == get()) {
                s.request(n);
            } else {
                worker.schedule(new Runnable() {
                    @Override
                    public void run() {
                        s.request(n);
                    }
                });
            }
        }
        
        @Override
        public void cancel() {
            s.cancel();
            worker.dispose();
        }
    }
}
