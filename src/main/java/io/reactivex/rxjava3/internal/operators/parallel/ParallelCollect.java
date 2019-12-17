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
 * @param <C> the collection type
 */
public final class ParallelCollect<T, C> extends ParallelFlowable<C> {

    final ParallelFlowable<? extends T> source;

    final Supplier<? extends C> initialCollection;

    final BiConsumer<? super C, ? super T> collector;

    public ParallelCollect(ParallelFlowable<? extends T> source,
            Supplier<? extends C> initialCollection, BiConsumer<? super C, ? super T> collector) {
        this.source = source;
        this.initialCollection = initialCollection;
        this.collector = collector;
    }

    @Override
    public void subscribe(Subscriber<? super C>[] subscribers) {
        if (!validate(subscribers)) {
            return;
        }

        int n = subscribers.length;
        @SuppressWarnings("unchecked")
        Subscriber<T>[] parents = new Subscriber[n];

        for (int i = 0; i < n; i++) {

            C initialValue;

            try {
                initialValue = Objects.requireNonNull(initialCollection.get(), "The initialSupplier returned a null value");
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                reportError(subscribers, ex);
                return;
            }

            parents[i] = new ParallelCollectSubscriber<T, C>(subscribers[i], initialValue, collector);
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

    static final class ParallelCollectSubscriber<T, C> extends DeferredScalarSubscriber<T, C> {

        private static final long serialVersionUID = -4767392946044436228L;

        final BiConsumer<? super C, ? super T> collector;

        C collection;

        boolean done;

        ParallelCollectSubscriber(Subscriber<? super C> subscriber,
                C initialValue, BiConsumer<? super C, ? super T> collector) {
            super(subscriber);
            this.collection = initialValue;
            this.collector = collector;
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

            try {
                collector.accept(collection, t);
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                cancel();
                onError(ex);
            }
        }

        @Override
        public void onError(Throwable t) {
            if (done) {
                RxJavaPlugins.onError(t);
                return;
            }
            done = true;
            collection = null;
            downstream.onError(t);
        }

        @Override
        public void onComplete() {
            if (done) {
                return;
            }
            done = true;
            C c = collection;
            collection = null;
            complete(c);
        }

        @Override
        public void cancel() {
            super.cancel();
            upstream.cancel();
        }
    }
}
