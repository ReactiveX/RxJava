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

import java.util.concurrent.atomic.*;

import org.reactivestreams.*;

import io.reactivex.*;
import io.reactivex.internal.subscriptions.SubscriptionHelper;
import io.reactivex.internal.util.BackpressureHelper;

public final class FlowableSubscribeOn<T> extends AbstractFlowableWithUpstream<T , T> {
    final Scheduler scheduler;
    
    public FlowableSubscribeOn(Publisher<T> source, Scheduler scheduler) {
        super(source);
        this.scheduler = scheduler;
    }
    
    @Override
    public void subscribeActual(final Subscriber<? super T> s) {
        Scheduler.Worker w = scheduler.createWorker();
        final SubscribeOnSubscriber<T> sos = new SubscribeOnSubscriber<T>(s, w);
        s.onSubscribe(sos);
        
        w.schedule(new Runnable() {
            @Override
            public void run() {
                sos.lazySet(Thread.currentThread());
                source.subscribe(sos);
            }
        });
    }
    
    static final class SubscribeOnSubscriber<T> extends AtomicReference<Thread>
    implements Subscriber<T>, Subscription {
        /** */
        private static final long serialVersionUID = 8094547886072529208L;
        final Subscriber<? super T> actual;
        final Scheduler.Worker worker;
        
        final AtomicReference<Subscription> s;
        
        final AtomicLong requested;
        
        public SubscribeOnSubscriber(Subscriber<? super T> actual, Scheduler.Worker worker) {
            this.actual = actual;
            this.worker = worker;
            this.s = new AtomicReference<Subscription>();
            this.requested = new AtomicLong();
        }
        
        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.setOnce(this.s, s)) {
                long r = requested.getAndSet(0L);
                if (r != 0L) {
                    requestUpstream(r, s);
                }
            }
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
            if (!SubscriptionHelper.validate(n)) {
                return;
            }
            Subscription s = this.s.get();
            if (s != null) {
                requestUpstream(n, s);
            } else {
                BackpressureHelper.add(requested, n);
                s = this.s.get();
                if (s != null) {
                    long r = requested.getAndSet(0L);
                    if (r != 0L) {
                        requestUpstream(r, s);
                    }
                }
                
            }
        }
        
        void requestUpstream(final long n, final Subscription s) {
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
            SubscriptionHelper.cancel(s);
            worker.dispose();
        }
    }
}
