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
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.exceptions.Exceptions;
import io.reactivex.rxjava3.functions.BiFunction;
import io.reactivex.rxjava3.internal.subscriptions.SubscriptionHelper;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;

import java.util.Objects;

/**
 * Reduce a sequence of values, starting from a seed value and by using
 * an accumulator function and return the last accumulated value.
 *
 * @param <T> the source value type
 * @param <R> the accumulated result type
 */
public final class FlowableReduceSeedSingle<T, R> extends Single<R> {

    final Publisher<T> source;

    final R seed;

    final BiFunction<R, ? super T, R> reducer;

    public FlowableReduceSeedSingle(Publisher<T> source, R seed, BiFunction<R, ? super T, R> reducer) {
        this.source = source;
        this.seed = seed;
        this.reducer = reducer;
    }

    @Override
    protected void subscribeActual(SingleObserver<? super R> observer) {
        source.subscribe(new ReduceSeedObserver<T, R>(observer, reducer, seed));
    }

    static final class ReduceSeedObserver<T, R> implements FlowableSubscriber<T>, Disposable {

        final SingleObserver<? super R> downstream;

        final BiFunction<R, ? super T, R> reducer;

        R value;

        Subscription upstream;

        ReduceSeedObserver(SingleObserver<? super R> actual, BiFunction<R, ? super T, R> reducer, R value) {
            this.downstream = actual;
            this.value = value;
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
        public void onNext(T value) {
            R v = this.value;
            if (v != null) {
                try {
                    this.value = Objects.requireNonNull(reducer.apply(v, value), "The reducer returned a null value");
                } catch (Throwable ex) {
                    Exceptions.throwIfFatal(ex);
                    upstream.cancel();
                    onError(ex);
                }
            }
        }

        @Override
        public void onError(Throwable e) {
            if (value != null) {
                value = null;
                upstream = SubscriptionHelper.CANCELLED;
                downstream.onError(e);
            } else {
                RxJavaPlugins.onError(e);
            }
        }

        @Override
        public void onComplete() {
            R v = value;
            if (v != null) {
                value = null;
                upstream = SubscriptionHelper.CANCELLED;
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
