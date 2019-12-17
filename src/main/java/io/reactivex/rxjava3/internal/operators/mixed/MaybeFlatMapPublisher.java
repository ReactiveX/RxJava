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

package io.reactivex.rxjava3.internal.operators.mixed;

import java.util.Objects;
import java.util.concurrent.atomic.*;

import org.reactivestreams.*;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.exceptions.Exceptions;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.internal.disposables.DisposableHelper;
import io.reactivex.rxjava3.internal.subscriptions.SubscriptionHelper;

/**
 * Maps the success value of a Maybe onto a Publisher and
 * relays its signals to the downstream subscriber.
 *
 * @param <T> the success value type of the Maybe source
 * @param <R> the result type of the Publisher and this operator
 * @since 2.1.15
 */
public final class MaybeFlatMapPublisher<T, R> extends Flowable<R> {

    final MaybeSource<T> source;

    final Function<? super T, ? extends Publisher<? extends R>> mapper;

    public MaybeFlatMapPublisher(MaybeSource<T> source,
            Function<? super T, ? extends Publisher<? extends R>> mapper) {
        this.source = source;
        this.mapper = mapper;
    }

    @Override
    protected void subscribeActual(Subscriber<? super R> s) {
        source.subscribe(new FlatMapPublisherSubscriber<T, R>(s, mapper));
    }

    static final class FlatMapPublisherSubscriber<T, R>
    extends AtomicReference<Subscription>
    implements FlowableSubscriber<R>, MaybeObserver<T>, Subscription {

        private static final long serialVersionUID = -8948264376121066672L;

        final Subscriber<? super R> downstream;

        final Function<? super T, ? extends Publisher<? extends R>> mapper;

        Disposable upstream;

        final AtomicLong requested;

        FlatMapPublisherSubscriber(Subscriber<? super R> downstream, Function<? super T, ? extends Publisher<? extends R>> mapper) {
            this.downstream = downstream;
            this.mapper = mapper;
            this.requested = new AtomicLong();
        }

        @Override
        public void onNext(R t) {
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

        @Override
        public void request(long n) {
            SubscriptionHelper.deferredRequest(this, requested, n);
        }

        @Override
        public void cancel() {
            upstream.dispose();
            SubscriptionHelper.cancel(this);
        }

        @Override
        public void onSubscribe(Disposable d) {
            if (DisposableHelper.validate(upstream, d)) {
                this.upstream = d;
                downstream.onSubscribe(this);
            }
        }

        @Override
        public void onSuccess(T t) {
            Publisher<? extends R> p;

            try {
                p = Objects.requireNonNull(mapper.apply(t), "The mapper returned a null Publisher");
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                downstream.onError(ex);
                return;
            }

            p.subscribe(this);
        }

        @Override
        public void onSubscribe(Subscription s) {
            SubscriptionHelper.deferredSetOnce(this, requested, s);
        }
    }
}
