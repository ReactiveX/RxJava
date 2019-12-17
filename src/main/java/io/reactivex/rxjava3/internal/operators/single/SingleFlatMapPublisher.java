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

package io.reactivex.rxjava3.internal.operators.single;

import java.util.Objects;
import java.util.concurrent.atomic.*;

import org.reactivestreams.*;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.exceptions.Exceptions;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.internal.subscriptions.SubscriptionHelper;

/**
 * A Flowable that emits items based on applying a specified function to the item emitted by the
 * source Single, where that function returns a Publisher.
 * <p>
 * <img width="640" height="305" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.flatMapPublisher.png" alt="">
 * <dl>
 *  <dt><b>Backpressure:</b></dt>
 *  <dd>The returned {@code Flowable} honors the backpressure of the downstream consumer
 *  and the {@code Publisher} returned by the mapper function is expected to honor it as well.</dd>
 * <dt><b>Scheduler:</b></dt>
 * <dd>{@code flatMapPublisher} does not operate by default on a particular {@link Scheduler}.</dd>
 * </dl>
 * 
 * @param <T> the source value type
 * @param <R> the result value type
 * 
 * @see <a href="http://reactivex.io/documentation/operators/flatmap.html">ReactiveX operators documentation: FlatMap</a>
 * @since 2.1.15
 */
public final class SingleFlatMapPublisher<T, R> extends Flowable<R> {

    final SingleSource<T> source;
    final Function<? super T, ? extends Publisher<? extends R>> mapper;

    public SingleFlatMapPublisher(SingleSource<T> source,
            Function<? super T, ? extends Publisher<? extends R>> mapper) {
        this.source = source;
        this.mapper = mapper;
    }

    @Override
    protected void subscribeActual(Subscriber<? super R> downstream) {
        source.subscribe(new SingleFlatMapPublisherObserver<T, R>(downstream, mapper));
    }

    static final class SingleFlatMapPublisherObserver<S, T> extends AtomicLong
            implements SingleObserver<S>, FlowableSubscriber<T>, Subscription {

        private static final long serialVersionUID = 7759721921468635667L;

        final Subscriber<? super T> downstream;
        final Function<? super S, ? extends Publisher<? extends T>> mapper;
        final AtomicReference<Subscription> parent;
        Disposable disposable;

        SingleFlatMapPublisherObserver(Subscriber<? super T> actual,
                Function<? super S, ? extends Publisher<? extends T>> mapper) {
            this.downstream = actual;
            this.mapper = mapper;
            this.parent = new AtomicReference<Subscription>();
        }

        @Override
        public void onSubscribe(Disposable d) {
            this.disposable = d;
            downstream.onSubscribe(this);
        }

        @Override
        public void onSuccess(S value) {
            Publisher<? extends T> f;
            try {
                f = Objects.requireNonNull(mapper.apply(value), "the mapper returned a null Publisher");
            } catch (Throwable e) {
                Exceptions.throwIfFatal(e);
                downstream.onError(e);
                return;
            }
            f.subscribe(this);
        }

        @Override
        public void onSubscribe(Subscription s) {
            SubscriptionHelper.deferredSetOnce(parent, this, s);
        }

        @Override
        public void onNext(T t) {
            downstream.onNext(t);
        }

        @Override
        public void onComplete() {
            downstream.onComplete();
        }

        @Override
        public void onError(Throwable e) {
            downstream.onError(e);
        }

        @Override
        public void request(long n) {
            SubscriptionHelper.deferredRequest(parent, this, n);
        }

        @Override
        public void cancel() {
            disposable.dispose();
            SubscriptionHelper.cancel(parent);
        }
    }

}
