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
import io.reactivex.rxjava3.functions.Predicate;
import io.reactivex.rxjava3.internal.subscriptions.SubscriptionHelper;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;

public final class FlowableTakeWhile<T> extends AbstractFlowableWithUpstream<T, T> {
    final Predicate<? super T> predicate;
    public FlowableTakeWhile(Flowable<T> source, Predicate<? super T> predicate) {
        super(source);
        this.predicate = predicate;
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> s) {
        source.subscribe(new TakeWhileSubscriber<T>(s, predicate));
    }

    static final class TakeWhileSubscriber<T> implements FlowableSubscriber<T>, Subscription {
        final Subscriber<? super T> downstream;
        final Predicate<? super T> predicate;

        Subscription upstream;

        boolean done;

        TakeWhileSubscriber(Subscriber<? super T> actual, Predicate<? super T> predicate) {
            this.downstream = actual;
            this.predicate = predicate;
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
            if (done) {
                return;
            }
            boolean b;
            try {
                b = predicate.test(t);
            } catch (Throwable e) {
                Exceptions.throwIfFatal(e);
                upstream.cancel();
                onError(e);
                return;
            }

            if (!b) {
                done = true;
                upstream.cancel();
                downstream.onComplete();
                return;
            }

            downstream.onNext(t);
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
