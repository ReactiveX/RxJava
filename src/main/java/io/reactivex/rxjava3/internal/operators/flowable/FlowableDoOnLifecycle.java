/*
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
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.internal.subscriptions.*;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;

public final class FlowableDoOnLifecycle<T> extends AbstractFlowableWithUpstream<T, T> {
    private final Consumer<? super Subscription> onSubscribe;
    private final LongConsumer onRequest;
    private final Action onCancel;

    public FlowableDoOnLifecycle(Flowable<T> source, Consumer<? super Subscription> onSubscribe,
            LongConsumer onRequest, Action onCancel) {
        super(source);
        this.onSubscribe = onSubscribe;
        this.onRequest = onRequest;
        this.onCancel = onCancel;
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> s) {
        source.subscribe(new SubscriptionLambdaSubscriber<>(s, onSubscribe, onRequest, onCancel));
    }

    static final class SubscriptionLambdaSubscriber<T> implements FlowableSubscriber<T>, Subscription {
        final Subscriber<? super T> downstream;
        final Consumer<? super Subscription> onSubscribe;
        final LongConsumer onRequest;
        final Action onCancel;

        Subscription upstream;

        SubscriptionLambdaSubscriber(Subscriber<? super T> actual,
                Consumer<? super Subscription> onSubscribe,
                LongConsumer onRequest,
                Action onCancel) {
            this.downstream = actual;
            this.onSubscribe = onSubscribe;
            this.onCancel = onCancel;
            this.onRequest = onRequest;
        }

        @Override
        public void onSubscribe(Subscription s) {
            // this way, multiple calls to onSubscribe can show up in tests that use doOnSubscribe to validate behavior
            try {
                onSubscribe.accept(s);
            } catch (Throwable e) {
                Exceptions.throwIfFatal(e);
                s.cancel();
                this.upstream = SubscriptionHelper.CANCELLED;
                EmptySubscription.error(e, downstream);
                return;
            }
            if (SubscriptionHelper.validate(this.upstream, s)) {
                this.upstream = s;
                downstream.onSubscribe(this);
            }
        }

        @Override
        public void onNext(T t) {
            downstream.onNext(t);
        }

        @Override
        public void onError(Throwable t) {
            if (upstream != SubscriptionHelper.CANCELLED) {
                downstream.onError(t);
            } else {
                RxJavaPlugins.onError(t);
            }
        }

        @Override
        public void onComplete() {
            if (upstream != SubscriptionHelper.CANCELLED) {
                downstream.onComplete();
            }
        }

        @Override
        public void request(long n) {
            try {
                onRequest.accept(n);
            } catch (Throwable e) {
                Exceptions.throwIfFatal(e);
                RxJavaPlugins.onError(e);
            }
            upstream.request(n);
        }

        @Override
        public void cancel() {
            Subscription s = upstream;
            if (s != SubscriptionHelper.CANCELLED) {
                upstream = SubscriptionHelper.CANCELLED;
                try {
                    onCancel.run();
                } catch (Throwable e) {
                    Exceptions.throwIfFatal(e);
                    RxJavaPlugins.onError(e);
                }
                s.cancel();
            }
        }
    }
}
