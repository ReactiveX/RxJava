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

import java.util.Iterator;
import java.util.Objects;

import org.reactivestreams.*;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.exceptions.Exceptions;
import io.reactivex.rxjava3.functions.BiFunction;
import io.reactivex.rxjava3.internal.subscriptions.*;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;

public final class FlowableZipIterable<T, U, V> extends AbstractFlowableWithUpstream<T, V> {
    final Iterable<U> other;
    final BiFunction<? super T, ? super U, ? extends V> zipper;

    public FlowableZipIterable(
            Flowable<T> source,
            Iterable<U> other, BiFunction<? super T, ? super U, ? extends V> zipper) {
        super(source);
        this.other = other;
        this.zipper = zipper;
    }

    @Override
    public void subscribeActual(Subscriber<? super V> t) {
        Iterator<U> it;

        try {
            it = Objects.requireNonNull(other.iterator(), "The iterator returned by other is null");
        } catch (Throwable e) {
            Exceptions.throwIfFatal(e);
            EmptySubscription.error(e, t);
            return;
        }

        boolean b;

        try {
            b = it.hasNext();
        } catch (Throwable e) {
            Exceptions.throwIfFatal(e);
            EmptySubscription.error(e, t);
            return;
        }

        if (!b) {
            EmptySubscription.complete(t);
            return;
        }

        source.subscribe(new ZipIterableSubscriber<T, U, V>(t, it, zipper));
    }

    static final class ZipIterableSubscriber<T, U, V> implements FlowableSubscriber<T>, Subscription {
        final Subscriber<? super V> downstream;
        final Iterator<U> iterator;
        final BiFunction<? super T, ? super U, ? extends V> zipper;

        Subscription upstream;

        boolean done;

        ZipIterableSubscriber(Subscriber<? super V> actual, Iterator<U> iterator,
                BiFunction<? super T, ? super U, ? extends V> zipper) {
            this.downstream = actual;
            this.iterator = iterator;
            this.zipper = zipper;
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

            U u;

            try {
                u = Objects.requireNonNull(iterator.next(), "The iterator returned a null value");
            } catch (Throwable e) {
                error(e);
                return;
            }

            V v;
            try {
                v = Objects.requireNonNull(zipper.apply(t, u), "The zipper function returned a null value");
            } catch (Throwable e) {
                error(e);
                return;
            }

            downstream.onNext(v);

            boolean b;

            try {
                b = iterator.hasNext();
            } catch (Throwable e) {
                error(e);
                return;
            }

            if (!b) {
                done = true;
                upstream.cancel();
                downstream.onComplete();
            }
        }

        void error(Throwable e) {
            Exceptions.throwIfFatal(e);
            done = true;
            upstream.cancel();
            downstream.onError(e);
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
