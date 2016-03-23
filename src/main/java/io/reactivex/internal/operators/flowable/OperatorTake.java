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

import java.util.concurrent.atomic.AtomicLong;

import org.reactivestreams.*;

import io.reactivex.Flowable.Operator;
import io.reactivex.internal.subscriptions.SubscriptionHelper;
import io.reactivex.internal.util.BackpressureHelper;
import io.reactivex.plugins.RxJavaPlugins;

public final class OperatorTake<T> implements Operator<T, T> {
    final long limit;
    public OperatorTake(long limit) {
        this.limit = limit;
    }
    
    @Override
    public Subscriber<? super T> apply(Subscriber<? super T> t) {
        return new TakeSubscriber<T>(t, limit);
    }
    
    static final class TakeSubscriber<T> extends AtomicLong implements Subscriber<T>, Subscription {
        /** */
        private static final long serialVersionUID = -5636543848937116287L;
        boolean done;
        Subscription subscription;
        final Subscriber<? super T> actual;
        final long limit;
        long remaining;
        public TakeSubscriber(Subscriber<? super T> actual, long limit) {
            this.actual = actual;
            this.limit = limit;
            this.remaining = limit;
        }
        @Override
        public void onSubscribe(Subscription s) {
            Subscription s0 = subscription;
            if (s0 != null) {
                s.cancel();
                RxJavaPlugins.onError(new IllegalStateException("Subscription already set"));
            } else {
                subscription = s;
                actual.onSubscribe(this);
            }
        }
        @Override
        public void onNext(T t) {
            if (!done && remaining-- > 0) {
                boolean stop = remaining == 0;
                actual.onNext(t);
                if (stop) {
                    onComplete();
                }
            }
        }
        @Override
        public void onError(Throwable t) {
            if (!done) {
                done = true;
                subscription.cancel();
                actual.onError(t);
            }
        }
        @Override
        public void onComplete() {
            if (!done) {
                done = true;
                subscription.cancel();
                actual.onComplete();
            }
        }
        @Override
        public void request(long n) {
            if (SubscriptionHelper.validateRequest(n)) {
                return;
            }
            for (;;) {
                long r = get();
                if (r >= limit) {
                    return;
                }
                long u = BackpressureHelper.addCap(r, n);
                if (compareAndSet(r, u)) {
                    if (u >= limit) {
                        subscription.request(Long.MAX_VALUE);
                    } else {
                        subscription.request(n);
                    }
                    return;
                }
            }
        }
        @Override
        public void cancel() {
            subscription.cancel();
        }
    }
}
