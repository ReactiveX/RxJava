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

import java.util.concurrent.Callable;

import org.reactivestreams.Subscriber;

import io.reactivex.Flowable;
import io.reactivex.exceptions.*;
import io.reactivex.functions.Function;
import io.reactivex.internal.functions.ObjectHelper;
import io.reactivex.internal.subscribers.SinglePostCompleteSubscriber;

public final class FlowableMapNotification<T, R> extends AbstractFlowableWithUpstream<T, R> {

    final Function<? super T, ? extends R> onNextMapper;
    final Function<? super Throwable, ? extends R> onErrorMapper;
    final Callable<? extends R> onCompleteSupplier;

    public FlowableMapNotification(
            Flowable<T> source,
            Function<? super T, ? extends R> onNextMapper,
            Function<? super Throwable, ? extends R> onErrorMapper,
            Callable<? extends R> onCompleteSupplier) {
        super(source);
        this.onNextMapper = onNextMapper;
        this.onErrorMapper = onErrorMapper;
        this.onCompleteSupplier = onCompleteSupplier;
    }

    @Override
    protected void subscribeActual(Subscriber<? super R> s) {
        source.subscribe(new MapNotificationSubscriber<T, R>(s, onNextMapper, onErrorMapper, onCompleteSupplier));
    }

    static final class MapNotificationSubscriber<T, R>
    extends SinglePostCompleteSubscriber<T, R> {

        private static final long serialVersionUID = 2757120512858778108L;
        final Function<? super T, ? extends R> onNextMapper;
        final Function<? super Throwable, ? extends R> onErrorMapper;
        final Callable<? extends R> onCompleteSupplier;

        MapNotificationSubscriber(Subscriber<? super R> actual,
                Function<? super T, ? extends R> onNextMapper,
                Function<? super Throwable, ? extends R> onErrorMapper,
                Callable<? extends R> onCompleteSupplier) {
            super(actual);
            this.onNextMapper = onNextMapper;
            this.onErrorMapper = onErrorMapper;
            this.onCompleteSupplier = onCompleteSupplier;
        }

        @Override
        public void onNext(T t) {
            R p;

            try {
                p = ObjectHelper.requireNonNull(onNextMapper.apply(t), "The onNext publisher returned is null");
            } catch (Throwable e) {
                Exceptions.throwIfFatal(e);
                actual.onError(e);
                return;
            }

            produced++;
            actual.onNext(p);
        }

        @Override
        public void onError(Throwable t) {
            R p;

            try {
                p = ObjectHelper.requireNonNull(onErrorMapper.apply(t), "The onError publisher returned is null");
            } catch (Throwable e) {
                Exceptions.throwIfFatal(e);
                actual.onError(new CompositeException(t, e));
                return;
            }

            complete(p);
        }

        @Override
        public void onComplete() {
            R p;

            try {
                p = ObjectHelper.requireNonNull(onCompleteSupplier.call(), "The onComplete publisher returned is null");
            } catch (Throwable e) {
                Exceptions.throwIfFatal(e);
                actual.onError(e);
                return;
            }

            complete(p);
        }
    }
}
