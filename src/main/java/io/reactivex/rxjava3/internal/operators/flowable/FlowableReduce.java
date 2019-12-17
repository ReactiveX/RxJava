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
import io.reactivex.rxjava3.functions.BiFunction;
import io.reactivex.rxjava3.internal.subscriptions.*;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;

import java.util.Objects;

/**
 * Reduces a sequence via a function into a single value or signals NoSuchElementException for
 * an empty source.
 *
 * @param <T> the value type
 */
public final class FlowableReduce<T> extends AbstractFlowableWithUpstream<T, T> {

    final BiFunction<T, T, T> reducer;

    public FlowableReduce(Flowable<T> source, BiFunction<T, T, T> reducer) {
        super(source);
        this.reducer = reducer;
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> s) {
        source.subscribe(new ReduceSubscriber<T>(s, reducer));
    }

    static final class ReduceSubscriber<T> extends DeferredScalarSubscription<T> implements FlowableSubscriber<T> {

        private static final long serialVersionUID = -4663883003264602070L;

        final BiFunction<T, T, T> reducer;

        Subscription upstream;

        ReduceSubscriber(Subscriber<? super T> actual, BiFunction<T, T, T> reducer) {
            super(actual);
            this.reducer = reducer;
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
            if (upstream == SubscriptionHelper.CANCELLED) {
                return;
            }

            T v = value;
            if (v == null) {
                value = t;
            } else {
                try {
                    value = Objects.requireNonNull(reducer.apply(v, t), "The reducer returned a null value");
                } catch (Throwable ex) {
                    Exceptions.throwIfFatal(ex);
                    upstream.cancel();
                    onError(ex);
                }
            }
        }

        @Override
        public void onError(Throwable t) {
            if (upstream == SubscriptionHelper.CANCELLED) {
                RxJavaPlugins.onError(t);
                return;
            }
            upstream = SubscriptionHelper.CANCELLED;
            downstream.onError(t);
        }

        @Override
        public void onComplete() {
            if (upstream == SubscriptionHelper.CANCELLED) {
                return;
            }
            upstream = SubscriptionHelper.CANCELLED;

            T v = value;
            if (v != null) {
                complete(v);
            } else {
                downstream.onComplete();
            }
        }

        @Override
        public void cancel() {
            super.cancel();
            upstream.cancel();
            upstream = SubscriptionHelper.CANCELLED;
        }

    }
}
