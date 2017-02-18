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

import java.util.concurrent.atomic.*;

import org.reactivestreams.*;

import io.reactivex.*;
import io.reactivex.internal.fuseable.ConditionalSubscriber;
import io.reactivex.internal.subscriptions.SubscriptionHelper;
import io.reactivex.internal.util.*;

public final class FlowableSkipUntil<T, U> extends AbstractFlowableWithUpstream<T, T> {
    final Publisher<U> other;
    public FlowableSkipUntil(Flowable<T> source, Publisher<U> other) {
        super(source);
        this.other = other;
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> child) {
        SkipUntilMainSubscriber<T> parent = new SkipUntilMainSubscriber<T>(child);
        child.onSubscribe(parent);

        other.subscribe(parent.other);

        source.subscribe(parent);
    }

    static final class SkipUntilMainSubscriber<T> extends AtomicInteger
    implements ConditionalSubscriber<T>, Subscription {
        private static final long serialVersionUID = -6270983465606289181L;

        final Subscriber<? super T> actual;

        final AtomicReference<Subscription> s;

        final AtomicLong requested;

        final OtherSubscriber other;

        final AtomicThrowable error;

        volatile boolean gate;

        SkipUntilMainSubscriber(Subscriber<? super T> actual) {
            this.actual = actual;
            this.s = new AtomicReference<Subscription>();
            this.requested = new AtomicLong();
            this.other = new OtherSubscriber();
            this.error = new AtomicThrowable();
        }

        @Override
        public void onSubscribe(Subscription s) {
            SubscriptionHelper.deferredSetOnce(this.s, requested, s);
        }

        @Override
        public void onNext(T t) {
            if (!tryOnNext(t)) {
                s.get().request(1);
            }
        }

        @Override
        public boolean tryOnNext(T t) {
            if (gate) {
                HalfSerializer.onNext(actual, t, this, error);
                return true;
            }
            return false;
        }

        @Override
        public void onError(Throwable t) {
            SubscriptionHelper.cancel(other);
            HalfSerializer.onError(actual, t, SkipUntilMainSubscriber.this, error);
        }

        @Override
        public void onComplete() {
            SubscriptionHelper.cancel(other);
            HalfSerializer.onComplete(actual, this, error);
        }

        @Override
        public void request(long n) {
            SubscriptionHelper.deferredRequest(s, requested, n);
        }

        @Override
        public void cancel() {
            SubscriptionHelper.cancel(s);
            SubscriptionHelper.cancel(other);
        }

        final class OtherSubscriber extends AtomicReference<Subscription>
        implements FlowableSubscriber<Object> {

            private static final long serialVersionUID = -5592042965931999169L;

            @Override
            public void onSubscribe(Subscription s) {
                if (SubscriptionHelper.setOnce(this, s)) {
                    s.request(Long.MAX_VALUE);
                }
            }

            @Override
            public void onNext(Object t) {
                gate = true;
                get().cancel();
            }

            @Override
            public void onError(Throwable t) {
                SubscriptionHelper.cancel(s);
                HalfSerializer.onError(actual, t, SkipUntilMainSubscriber.this, error);
            }

            @Override
            public void onComplete() {
                gate = true;
            }
        }
    }
}
