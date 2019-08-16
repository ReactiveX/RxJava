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
package io.reactivex.rxjava3.internal.operators.flowable;

import java.util.concurrent.atomic.*;

import org.reactivestreams.*;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.internal.subscriptions.SubscriptionHelper;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;

/**
 * Delays the subscription to the main source until the other
 * observable fires an event or completes.
 * @param <T> the main type
 * @param <U> the other value type, ignored
 */
public final class FlowableDelaySubscriptionOther<T, U> extends Flowable<T> {
    final Publisher<? extends T> main;
    final Publisher<U> other;

    public FlowableDelaySubscriptionOther(Publisher<? extends T> main, Publisher<U> other) {
        this.main = main;
        this.other = other;
    }

    @Override
    public void subscribeActual(final Subscriber<? super T> child) {
        MainSubscriber<T> parent = new MainSubscriber<T>(child, main);
        child.onSubscribe(parent);
        other.subscribe(parent.other);
    }

    static final class MainSubscriber<T> extends AtomicLong implements FlowableSubscriber<T>, Subscription {

        private static final long serialVersionUID = 2259811067697317255L;

        final Subscriber<? super T> downstream;

        final Publisher<? extends T> main;

        final OtherSubscriber other;

        final AtomicReference<Subscription> upstream;

        MainSubscriber(Subscriber<? super T> downstream, Publisher<? extends T> main) {
            this.downstream = downstream;
            this.main = main;
            this.other = new OtherSubscriber();
            this.upstream = new AtomicReference<Subscription>();
        }

        void next() {
            main.subscribe(this);
        }

        @Override
        public void onNext(T t) {
            downstream.onNext(t);
        }

        @Override
        public void onError(Throwable t) {
            downstream.onError(t);
        }

        @Override
        public void onComplete() {
            downstream.onComplete();
        }

        @Override
        public void request(long n) {
            if (SubscriptionHelper.validate(n)) {
                SubscriptionHelper.deferredRequest(upstream, this, n);
            }
        }

        @Override
        public void cancel() {
            SubscriptionHelper.cancel(other);
            SubscriptionHelper.cancel(upstream);
        }

        @Override
        public void onSubscribe(Subscription s) {
            SubscriptionHelper.deferredSetOnce(upstream, this, s);
        }

        final class OtherSubscriber extends AtomicReference<Subscription> implements FlowableSubscriber<Object> {

            private static final long serialVersionUID = -3892798459447644106L;

            @Override
            public void onSubscribe(Subscription s) {
                if (SubscriptionHelper.setOnce(this, s)) {
                    s.request(Long.MAX_VALUE);
                }
            }

            @Override
            public void onNext(Object t) {
                Subscription s = get();
                if (s != SubscriptionHelper.CANCELLED) {
                    lazySet(SubscriptionHelper.CANCELLED);
                    s.cancel();
                    next();
                }
            }

            @Override
            public void onError(Throwable t) {
                Subscription s = get();
                if (s != SubscriptionHelper.CANCELLED) {
                    downstream.onError(t);
                } else {
                    RxJavaPlugins.onError(t);
                }
            }

            @Override
            public void onComplete() {
                Subscription s = get();
                if (s != SubscriptionHelper.CANCELLED) {
                    next();
                }
            }
        }
}
}
