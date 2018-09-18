/**
 * Copyright (c) 2016-present, RxJava Contributors.
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

import java.util.concurrent.atomic.AtomicBoolean;

import org.reactivestreams.*;

import io.reactivex.*;
import io.reactivex.internal.subscriptions.*;
import io.reactivex.plugins.RxJavaPlugins;

public final class FlowableTake<T> extends AbstractFlowableWithUpstream<T, T> {
    final long limit;
    public FlowableTake(Flowable<T> source, long limit) {
        super(source);
        this.limit = limit;
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> s) {
        source.subscribe(new TakeSubscriber<T>(s, limit));
    }

    static final class TakeSubscriber<T> extends AtomicBoolean implements FlowableSubscriber<T>, Subscription {

        private static final long serialVersionUID = -5636543848937116287L;

        final Subscriber<? super T> downstream;

        final long limit;

        boolean done;

        Subscription upstream;

        long remaining;

        TakeSubscriber(Subscriber<? super T> actual, long limit) {
            this.downstream = actual;
            this.limit = limit;
            this.remaining = limit;
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.validate(this.upstream, s)) {
                upstream = s;
                if (limit == 0L) {
                    s.cancel();
                    done = true;
                    EmptySubscription.complete(downstream);
                } else {
                    downstream.onSubscribe(this);
                }
            }
        }

        @Override
        public void onNext(T t) {
            if (!done && remaining-- > 0) {
                boolean stop = remaining == 0;
                downstream.onNext(t);
                if (stop) {
                    upstream.cancel();
                    onComplete();
                }
            }
        }

        @Override
        public void onError(Throwable t) {
            if (!done) {
                done = true;
                upstream.cancel();
                downstream.onError(t);
            } else {
                RxJavaPlugins.onError(t);
            }
        }

        @Override
        public void onComplete() {
            if (!done) {
                done = true;
                downstream.onComplete();
            }
        }

        @Override
        public void request(long n) {
            if (!SubscriptionHelper.validate(n)) {
                return;
            }
            if (!get() && compareAndSet(false, true)) {
                if (n >= limit) {
                    upstream.request(Long.MAX_VALUE);
                    return;
                }
            }
            upstream.request(n);
        }

        @Override
        public void cancel() {
            upstream.cancel();
        }
    }
}
