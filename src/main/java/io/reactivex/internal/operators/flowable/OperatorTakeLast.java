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

import java.util.ArrayDeque;
import java.util.concurrent.atomic.*;

import org.reactivestreams.*;

import io.reactivex.Flowable.Operator;
import io.reactivex.internal.subscriptions.SubscriptionHelper;
import io.reactivex.internal.util.BackpressureHelper;
import io.reactivex.plugins.RxJavaPlugins;

public final class OperatorTakeLast<T> implements Operator<T, T> {
    final int count;
    
    public OperatorTakeLast(int count) {
        this.count = count;
    }

    @Override
    public Subscriber<? super T> apply(Subscriber<? super T> t) {
        return new TakeLastSubscriber<T>(t, count);
    }
    
    static final class TakeLastSubscriber<T> extends ArrayDeque<T> implements Subscriber<T>, Subscription {
        /** */
        private static final long serialVersionUID = 7240042530241604978L;
        final Subscriber<? super T> actual;
        final int count;
        
        Subscription s;
        volatile boolean done;
        volatile boolean cancelled;
        
        final AtomicLong requested = new AtomicLong();
        
        final AtomicInteger wip = new AtomicInteger();
        
        public TakeLastSubscriber(Subscriber<? super T> actual, int count) {
            this.actual = actual;
            this.count = count;
        }
        
        @Override
        public void onSubscribe(Subscription s) {
            if (this.s != null) {
                s.cancel();
                RxJavaPlugins.onError(new IllegalStateException("Subscription already set!"));
                return;
            }
            this.s = s;
            actual.onSubscribe(this);
            s.request(Long.MAX_VALUE);
        }
        
        @Override
        public void onNext(T t) {
            if (count == size()) {
                poll();
            }
            offer(t);
        }
        
        @Override
        public void onError(Throwable t) {
            actual.onError(t);
        }
        
        @Override
        public void onComplete() {
            done = true;
            drain();
        }
        
        @Override
        public void request(long n) {
            if (SubscriptionHelper.validateRequest(n)) {
                return;
            }
            BackpressureHelper.add(requested, n);
            drain();
        }
        
        @Override
        public void cancel() {
            cancelled = true;
            s.cancel();
        }
        
        void drain() {
            if (wip.getAndIncrement() == 0) {
                Subscriber<? super T> a = actual;
                long r = requested.get();
                do {
                    if (cancelled) {
                        return;
                    }
                    if (done) {
                        boolean unbounded = r == Long.MAX_VALUE;
                        long e = 0L;
                        
                        while (r != 0L) {
                            if (cancelled) {
                                return;
                            }
                            T v = poll();
                            if (v == null) {
                                a.onComplete();
                                return;
                            }
                            a.onNext(v);
                            r--;
                            e++;
                        }
                        if (!unbounded && e != 0L) {
                            r = requested.addAndGet(-e);
                        }
                    }
                } while (wip.decrementAndGet() != 0);
            }
        }
    }
}
