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

import org.reactivestreams.*;

import io.reactivex.*;
import io.reactivex.internal.subscriptions.SubscriptionArbiter;
import io.reactivex.plugins.RxJavaPlugins;

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
        final SubscriptionArbiter serial = new SubscriptionArbiter();
        child.onSubscribe(serial);

        FlowableSubscriber<U> otherSubscriber = new DelaySubscriber(serial, child);

        other.subscribe(otherSubscriber);
    }

    final class DelaySubscriber implements FlowableSubscriber<U> {
        final SubscriptionArbiter serial;
        final Subscriber<? super T> child;
        boolean done;

        DelaySubscriber(SubscriptionArbiter serial, Subscriber<? super T> child) {
            this.serial = serial;
            this.child = child;
        }

        @Override
        public void onSubscribe(final Subscription s) {
            serial.setSubscription(new DelaySubscription(s));
            s.request(Long.MAX_VALUE);
        }

        @Override
        public void onNext(U t) {
            onComplete();
        }

        @Override
        public void onError(Throwable e) {
            if (done) {
                RxJavaPlugins.onError(e);
                return;
            }
            done = true;
            child.onError(e);
        }

        @Override
        public void onComplete() {
            if (done) {
                return;
            }
            done = true;

            main.subscribe(new OnCompleteSubscriber());
        }

        final class DelaySubscription implements Subscription {
            private final Subscription s;

            DelaySubscription(Subscription s) {
                this.s = s;
            }

            @Override
            public void request(long n) {
                // ignored
            }

            @Override
            public void cancel() {
                s.cancel();
            }
        }

        final class OnCompleteSubscriber implements FlowableSubscriber<T> {
            @Override
            public void onSubscribe(Subscription s) {
                serial.setSubscription(s);
            }

            @Override
            public void onNext(T t) {
                child.onNext(t);
            }

            @Override
            public void onError(Throwable t) {
                child.onError(t);
            }

            @Override
            public void onComplete() {
                child.onComplete();
            }
        }
    }
}
