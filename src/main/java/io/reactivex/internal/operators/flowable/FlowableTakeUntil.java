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
import io.reactivex.internal.subscriptions.SubscriptionHelper;
import io.reactivex.internal.util.*;

public final class FlowableTakeUntil<T, U> extends AbstractFlowableWithUpstream<T, T> {
    final Publisher<? extends U> other;
    public FlowableTakeUntil(Flowable<T> source, Publisher<? extends U> other) {
        super(source);
        this.other = other;
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> child) {
        TakeUntilMainSubscriber<T> parent = new TakeUntilMainSubscriber<T>(child);
        child.onSubscribe(parent);

        other.subscribe(parent.other);

        source.subscribe(parent);
    }

    static final class TakeUntilMainSubscriber<T> extends AtomicInteger implements FlowableSubscriber<T>, Subscription {

        private static final long serialVersionUID = -4945480365982832967L;

        final Subscriber<? super T> actual;

        final AtomicLong requested;

        final AtomicReference<Subscription> s;

        final AtomicThrowable error;

        final OtherSubscriber other;

        TakeUntilMainSubscriber(Subscriber<? super T> actual) {
            this.actual = actual;
            this.requested = new AtomicLong();
            this.s = new AtomicReference<Subscription>();
            this.other = new OtherSubscriber();
            this.error = new AtomicThrowable();
        }

        @Override
        public void onSubscribe(Subscription s) {
            SubscriptionHelper.deferredSetOnce(this.s, requested, s);
        }

        @Override
        public void onNext(T t) {
            HalfSerializer.onNext(actual, t, this, error);
        }

        @Override
        public void onError(Throwable t) {
            SubscriptionHelper.cancel(other);
            HalfSerializer.onError(actual, t, this, error);
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

        final class OtherSubscriber extends AtomicReference<Subscription> implements FlowableSubscriber<Object> {

            private static final long serialVersionUID = -3592821756711087922L;

            @Override
            public void onSubscribe(Subscription s) {
                if (SubscriptionHelper.setOnce(this, s)) {
                    s.request(Long.MAX_VALUE);
                }
            }

            @Override
            public void onNext(Object t) {
                SubscriptionHelper.cancel(this);
                onComplete();
            }

            @Override
            public void onError(Throwable t) {
                SubscriptionHelper.cancel(s);
                HalfSerializer.onError(actual, t, TakeUntilMainSubscriber.this, error);
            }

            @Override
            public void onComplete() {
                SubscriptionHelper.cancel(s);
                HalfSerializer.onComplete(actual, TakeUntilMainSubscriber.this, error);
            }

        }
    }
}
