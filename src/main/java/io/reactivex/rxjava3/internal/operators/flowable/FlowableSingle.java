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

import java.util.NoSuchElementException;

import org.reactivestreams.*;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.internal.subscriptions.*;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;

public final class FlowableSingle<T> extends AbstractFlowableWithUpstream<T, T> {

    final T defaultValue;

    final boolean failOnEmpty;

    public FlowableSingle(Flowable<T> source, T defaultValue, boolean failOnEmpty) {
        super(source);
        this.defaultValue = defaultValue;
        this.failOnEmpty = failOnEmpty;
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> s) {
        source.subscribe(new SingleElementSubscriber<T>(s, defaultValue, failOnEmpty));
    }

    static final class SingleElementSubscriber<T> extends DeferredScalarSubscription<T>
    implements FlowableSubscriber<T> {

        private static final long serialVersionUID = -5526049321428043809L;

        final T defaultValue;

        final boolean failOnEmpty;

        Subscription upstream;

        boolean done;

        SingleElementSubscriber(Subscriber<? super T> actual, T defaultValue, boolean failOnEmpty) {
            super(actual);
            this.defaultValue = defaultValue;
            this.failOnEmpty = failOnEmpty;
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
            downstream.onError(t);
        }

        @Override
        public void onComplete() {
            if (done) {
                return;
            }
            done = true;
            T v = value;
            value = null;
            if (v == null) {
                v = defaultValue;
            }
            if (v == null) {
                if (failOnEmpty) {
                    downstream.onError(new NoSuchElementException());
                } else {
                    downstream.onComplete();
                }
            } else {
                complete(v);
            }
        }

        @Override
        public void cancel() {
            super.cancel();
            upstream.cancel();
        }
    }
}
