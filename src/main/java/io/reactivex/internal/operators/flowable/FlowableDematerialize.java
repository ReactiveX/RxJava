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
import io.reactivex.internal.subscriptions.SubscriptionHelper;
import io.reactivex.plugins.RxJavaPlugins;

public final class FlowableDematerialize<T> extends AbstractFlowableWithUpstream<Notification<T>, T> {

    public FlowableDematerialize(Flowable<Notification<T>> source) {
        super(source);
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> s) {
        source.subscribe(new DematerializeSubscriber<T>(s));
    }

    static final class DematerializeSubscriber<T> implements FlowableSubscriber<Notification<T>>, Subscription {
        final Subscriber<? super T> downstream;

        boolean done;

        Subscription upstream;

        DematerializeSubscriber(Subscriber<? super T> downstream) {
            this.downstream = downstream;
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.validate(this.upstream, s)) {
                this.upstream = s;
                downstream.onSubscribe(this);
            }
        }

        @Override
        public void onNext(Notification<T> t) {
            if (done) {
                if (t.isOnError()) {
                    RxJavaPlugins.onError(t.getError());
                }
                return;
            }
            if (t.isOnError()) {
                upstream.cancel();
                onError(t.getError());
            }
            else if (t.isOnComplete()) {
                upstream.cancel();
                onComplete();
            } else {
                downstream.onNext(t.getValue());
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
