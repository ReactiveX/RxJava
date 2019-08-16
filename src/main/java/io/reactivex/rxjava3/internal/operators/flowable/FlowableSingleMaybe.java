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

import org.reactivestreams.Subscription;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.internal.fuseable.FuseToFlowable;
import io.reactivex.rxjava3.internal.subscriptions.SubscriptionHelper;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;

public final class FlowableSingleMaybe<T> extends Maybe<T> implements FuseToFlowable<T> {

    final Flowable<T> source;

    public FlowableSingleMaybe(Flowable<T> source) {
        this.source = source;
    }

    @Override
    protected void subscribeActual(MaybeObserver<? super T> observer) {
        source.subscribe(new SingleElementSubscriber<T>(observer));
    }

    @Override
    public Flowable<T> fuseToFlowable() {
        return RxJavaPlugins.onAssembly(new FlowableSingle<T>(source, null, false));
    }

    static final class SingleElementSubscriber<T>
    implements FlowableSubscriber<T>, Disposable {

        final MaybeObserver<? super T> downstream;

        Subscription upstream;

        boolean done;

        T value;

        SingleElementSubscriber(MaybeObserver<? super T> downstream) {
            this.downstream = downstream;
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.validate(this.upstream, s)) {
                this.upstream = s;
                downstream.onSubscribe(this);
                s.request(Long.MAX_VALUE);
            }
        }

        @Override
        public void onNext(T t) {
            if (done) {
                return;
            }
            if (value != null) {
                done = true;
                upstream.cancel();
                upstream = SubscriptionHelper.CANCELLED;
                downstream.onError(new IllegalArgumentException("Sequence contains more than one element!"));
                return;
            }
            value = t;
        }

        @Override
        public void onError(Throwable t) {
            if (done) {
                RxJavaPlugins.onError(t);
                return;
            }
            done = true;
            upstream = SubscriptionHelper.CANCELLED;
            downstream.onError(t);
        }

        @Override
        public void onComplete() {
            if (done) {
                return;
            }
            done = true;
            upstream = SubscriptionHelper.CANCELLED;
            T v = value;
            value = null;
            if (v == null) {
                downstream.onComplete();
            } else {
                downstream.onSuccess(v);
            }
        }

        @Override
        public void dispose() {
            upstream.cancel();
            upstream = SubscriptionHelper.CANCELLED;
        }

        @Override
        public boolean isDisposed() {
            return upstream == SubscriptionHelper.CANCELLED;
        }
    }
}
