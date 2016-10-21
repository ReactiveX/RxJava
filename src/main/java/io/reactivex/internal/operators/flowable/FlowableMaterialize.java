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

import io.reactivex.Notification;
import io.reactivex.internal.subscriptions.SubscriptionHelper;
import io.reactivex.internal.util.BackpressureHelper;
import io.reactivex.plugins.RxJavaPlugins;

public final class FlowableMaterialize<T> extends AbstractFlowableWithUpstream<T, Notification<T>> {

    public FlowableMaterialize(Publisher<T> source) {
        super(source);
    }

    @Override
    protected void subscribeActual(Subscriber<? super Notification<T>> s) {
        source.subscribe(new MaterializeSubscriber<T>(s));
    }

    static final class MaterializeSubscriber<T> extends AtomicLong implements Subscriber<T>, Subscription {

        private static final long serialVersionUID = -3740826063558713822L;

        final Subscriber<? super Notification<T>> actual;

        Subscription s;

        Notification<T> value;

        long produced;

        static final long COMPLETE_MASK = Long.MIN_VALUE;
        static final long REQUEST_MASK = Long.MAX_VALUE;

        MaterializeSubscriber(Subscriber<? super Notification<T>> actual) {
            this.actual = actual;
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.validate(this.s, s)) {
                this.s = s;
                actual.onSubscribe(this);
            }
        }

        @Override
        public void onNext(T t) {
            produced++;
            actual.onNext(Notification.createOnNext(t));
        }

        @Override
        public void onError(Throwable t) {
            complete(Notification.<T>createOnError(t));
        }

        @Override
        public void onComplete() {
            complete(Notification.<T>createOnComplete());
        }

        void complete(Notification<T> n) {
            long p = produced;
            if (p != 0) {
                BackpressureHelper.produced(this, p);
            }

            for (;;) {
                long r = get();
                if ((r & COMPLETE_MASK) != 0) {
                    if (n.isOnError()) {
                        RxJavaPlugins.onError(n.getError());
                    }
                    return;
                }
                if ((r & REQUEST_MASK) != 0) {
                    lazySet(COMPLETE_MASK + 1);
                    actual.onNext(n);
                    actual.onComplete();
                    return;
                }
                value = n;
                if (compareAndSet(0, COMPLETE_MASK)) {
                    return;
                }
            }
        }

        @Override
        public void request(long n) {
            if (SubscriptionHelper.validate(n)) {
                for (;;) {
                    long r = get();
                    if ((r & COMPLETE_MASK) != 0) {
                        if (compareAndSet(COMPLETE_MASK, COMPLETE_MASK + 1)) {
                            actual.onNext(value);
                            actual.onComplete();
                        }
                        break;
                    }
                    long u = BackpressureHelper.addCap(r, n);
                    if (compareAndSet(r, u)) {
                        s.request(n);
                        break;
                    }
                }
            }
        }

        @Override
        public void cancel() {
            s.cancel();
        }
    }
}
