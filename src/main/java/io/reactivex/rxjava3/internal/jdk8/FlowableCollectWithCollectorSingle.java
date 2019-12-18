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
package io.reactivex.rxjava3.internal.jdk8;

import java.util.Objects;
import java.util.function.*;
import java.util.stream.Collector;

import org.reactivestreams.Subscription;

import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.exceptions.Exceptions;
import io.reactivex.rxjava3.internal.disposables.EmptyDisposable;
import io.reactivex.rxjava3.internal.fuseable.FuseToFlowable;
import io.reactivex.rxjava3.internal.subscriptions.SubscriptionHelper;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;

/**
 * Collect items into a container defined by a Stream {@link Collector} callback set.
 *
 * @param <T> the upstream value type
 * @param <A> the intermediate accumulator type
 * @param <R> the result type
 * @since 3.0.0
 */
public final class FlowableCollectWithCollectorSingle<T, A, R> extends Single<R> implements FuseToFlowable<R> {

    final Flowable<T> source;

    final Collector<T, A, R> collector;

    public FlowableCollectWithCollectorSingle(Flowable<T> source, Collector<T, A, R> collector) {
        this.source = source;
        this.collector = collector;
    }

    @Override
    public Flowable<R> fuseToFlowable() {
        return new FlowableCollectWithCollector<>(source, collector);
    }

    @Override
    protected void subscribeActual(@NonNull SingleObserver<? super R> observer) {
        A container;
        BiConsumer<A, T> accumulator;
        Function<A, R> finisher;

        try {
            container = collector.supplier().get();
            accumulator = collector.accumulator();
            finisher = collector.finisher();
        } catch (Throwable ex) {
            Exceptions.throwIfFatal(ex);
            EmptyDisposable.error(ex, observer);
            return;
        }

        source.subscribe(new CollectorSingleObserver<>(observer, container, accumulator, finisher));
    }

    static final class CollectorSingleObserver<T, A, R> implements FlowableSubscriber<T>, Disposable {

        final SingleObserver<? super R> downstream;

        final BiConsumer<A, T> accumulator;

        final Function<A, R> finisher;

        Subscription upstream;

        boolean done;

        A container;

        CollectorSingleObserver(SingleObserver<? super R> downstream, A container, BiConsumer<A, T> accumulator, Function<A, R> finisher) {
            this.downstream = downstream;
            this.container = container;
            this.accumulator = accumulator;
            this.finisher = finisher;
        }

        @Override
        public void onSubscribe(@NonNull Subscription s) {
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
                accumulator.accept(container, t);
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                upstream.cancel();
                onError(ex);
            }
        }

        @Override
        public void onError(Throwable t) {
            if (done) {
                RxJavaPlugins.onError(t);
            } else {
                done = true;
                upstream = SubscriptionHelper.CANCELLED;
                this.container = null;
                downstream.onError(t);
            }
        }

        @Override
        public void onComplete() {
            if (done) {
                return;
            }

            done = true;
            upstream = SubscriptionHelper.CANCELLED;
            A container = this.container;
            this.container = null;
            R result;
            try {
                result = Objects.requireNonNull(finisher.apply(container), "The finisher returned a null value");
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                downstream.onError(ex);
                return;
            }

            downstream.onSuccess(result);
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
