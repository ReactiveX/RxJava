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

import org.reactivestreams.*;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.exceptions.Exceptions;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.internal.subscriptions.SubscriptionHelper;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;

import java.util.Objects;

public final class FlowableDematerialize<T, R> extends AbstractFlowableWithUpstream<T, R> {

    final Function<? super T, ? extends Notification<R>> selector;

    public FlowableDematerialize(Flowable<T> source, Function<? super T, ? extends Notification<R>> selector) {
        super(source);
        this.selector = selector;
    }

    @Override
    protected void subscribeActual(Subscriber<? super R> subscriber) {
        source.subscribe(new DematerializeSubscriber<T, R>(subscriber, selector));
    }

    static final class DematerializeSubscriber<T, R> implements FlowableSubscriber<T>, Subscription {

        final Subscriber<? super R> downstream;

        final Function<? super T, ? extends Notification<R>> selector;

        boolean done;

        Subscription upstream;

        DematerializeSubscriber(Subscriber<? super R> downstream, Function<? super T, ? extends Notification<R>> selector) {
            this.downstream = downstream;
            this.selector = selector;
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.validate(this.upstream, s)) {
                this.upstream = s;
                downstream.onSubscribe(this);
            }
        }

        @Override
        public void onNext(T item) {
            if (done) {
                if (item instanceof Notification) {
                    Notification<?> notification = (Notification<?>)item;
                    if (notification.isOnError()) {
                        RxJavaPlugins.onError(notification.getError());
                    }
                }
                return;
            }

            Notification<R> notification;

            try {
                notification = Objects.requireNonNull(selector.apply(item), "The selector returned a null Notification");
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                upstream.cancel();
                onError(ex);
                return;
            }
            if (notification.isOnError()) {
                upstream.cancel();
                onError(notification.getError());
            } else if (notification.isOnComplete()) {
                upstream.cancel();
                onComplete();
            } else {
                downstream.onNext(notification.getValue());
            }
        }

        @Override
        public void onError(Throwable t) {
            if (done) {
                RxJavaPlugins.onError(t);
                return;
            }
            done = true;

            downstream.onError(t);
        }

        @Override
        public void onComplete() {
            if (done) {
                return;
            }
            done = true;

            downstream.onComplete();
        }

        @Override
        public void request(long n) {
            upstream.request(n);
        }

        @Override
        public void cancel() {
            upstream.cancel();
        }
    }
}
