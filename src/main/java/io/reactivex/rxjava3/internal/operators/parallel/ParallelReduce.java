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

package io.reactivex.rxjava3.internal.operators.parallel;

import org.reactivestreams.*;

import io.reactivex.rxjava3.exceptions.Exceptions;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.internal.subscribers.DeferredScalarSubscriber;
import io.reactivex.rxjava3.internal.subscriptions.*;
import io.reactivex.rxjava3.parallel.ParallelFlowable;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;

import java.util.Objects;

/**
 * Reduce the sequence of values in each 'rail' to a single value.
 *
 * @param <T> the input value type
 * @param <R> the result value type
 */
public final class ParallelReduce<T, R> extends ParallelFlowable<R> {

    final ParallelFlowable<? extends T> source;

    final Supplier<R> initialSupplier;

    final BiFunction<R, ? super T, R> reducer;

    public ParallelReduce(ParallelFlowable<? extends T> source, Supplier<R> initialSupplier, BiFunction<R, ? super T, R> reducer) {
        this.source = source;
        this.initialSupplier = initialSupplier;
        this.reducer = reducer;
    }

    @Override
    public void subscribe(Subscriber<? super R>[] subscribers) {
        if (!validate(subscribers)) {
            return;
        }

        int n = subscribers.length;
        @SuppressWarnings("unchecked")
        Subscriber<T>[] parents = new Subscriber[n];

        for (int i = 0; i < n; i++) {

            R initialValue;

            try {
                initialValue = Objects.requireNonNull(initialSupplier.get(), "The initialSupplier returned a null value");
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                reportError(subscribers, ex);
                return;
            }

            parents[i] = new ParallelReduceSubscriber<T, R>(subscribers[i], initialValue, reducer);
        }

        source.subscribe(parents);
    }

    void reportError(Subscriber<?>[] subscribers, Throwable ex) {
        for (Subscriber<?> s : subscribers) {
            EmptySubscription.error(ex, s);
        }
    }

    @Override
    public int parallelism() {
        return source.parallelism();
    }

    static final class ParallelReduceSubscriber<T, R> extends DeferredScalarSubscriber<T, R> {

        private static final long serialVersionUID = 8200530050639449080L;

        final BiFunction<R, ? super T, R> reducer;

        R accumulator;

        boolean done;

        ParallelReduceSubscriber(Subscriber<? super R> subscriber, R initialValue, BiFunction<R, ? super T, R> reducer) {
            super(subscriber);
            this.accumulator = initialValue;
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
            if (!done) {
                R v;

                try {
                    v = Objects.requireNonNull(reducer.apply(accumulator, t), "The reducer returned a null value");
                } catch (Throwable ex) {
                    Exceptions.throwIfFatal(ex);
                    cancel();
                    onError(ex);
                    return;
                }

                accumulator = v;
            }
        }

        @Override
        public void onError(Throwable t) {
            if (done) {
                RxJavaPlugins.onError(t);
                return;
            }
            done = true;
            accumulator = null;
            downstream.onError(t);
        }

        @Override
        public void onComplete() {
            if (!done) {
                done = true;

                R a = accumulator;
                accumulator = null;
                complete(a);
            }
        }

        @Override
        public void cancel() {
            super.cancel();
            upstream.cancel();
        }
    }
}
