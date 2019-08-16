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
import io.reactivex.rxjava3.internal.subscriptions.SubscriptionHelper;

/**
 * Hides the identity of the wrapped Flowable and its Subscription.
 * @param <T> the value type
 *
 * @since 2.0
 */
public final class FlowableHide<T> extends AbstractFlowableWithUpstream<T, T> {

    public FlowableHide(Flowable<T> source) {
        super(source);
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> s) {
        source.subscribe(new HideSubscriber<T>(s));
    }

    static final class HideSubscriber<T> implements FlowableSubscriber<T>, Subscription {

        final Subscriber<? super T> downstream;

        Subscription upstream;

        HideSubscriber(Subscriber<? super T> downstream) {
            this.downstream = downstream;
        }

        @Override
        public void request(long n) {
            upstream.request(n);
        }

        @Override
        public void cancel() {
            upstream.cancel();
        }

        @Override
        public void onSubscribe(Subscription s) {
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
            downstream.onError(t);
        }

        @Override
        public void onComplete() {
            downstream.onComplete();
        }
    }
}
