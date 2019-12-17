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

import java.util.Collection;
import java.util.Objects;

import org.reactivestreams.Subscriber;

import io.reactivex.rxjava3.annotations.Nullable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.exceptions.Exceptions;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.internal.fuseable.QueueFuseable;
import io.reactivex.rxjava3.internal.subscribers.BasicFuseableSubscriber;
import io.reactivex.rxjava3.internal.subscriptions.EmptySubscription;
import io.reactivex.rxjava3.internal.util.ExceptionHelper;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;

public final class FlowableDistinct<T, K> extends AbstractFlowableWithUpstream<T, T> {

    final Function<? super T, K> keySelector;

    final Supplier<? extends Collection<? super K>> collectionSupplier;

    public FlowableDistinct(Flowable<T> source, Function<? super T, K> keySelector, Supplier<? extends Collection<? super K>> collectionSupplier) {
        super(source);
        this.keySelector = keySelector;
        this.collectionSupplier = collectionSupplier;
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> subscriber) {
        Collection<? super K> collection;

        try {
            collection = ExceptionHelper.nullCheck(collectionSupplier.get(), "The collectionSupplier returned a null Collection.");
        } catch (Throwable ex) {
            Exceptions.throwIfFatal(ex);
            EmptySubscription.error(ex, subscriber);
            return;
        }

        source.subscribe(new DistinctSubscriber<T, K>(subscriber, keySelector, collection));
    }

    static final class DistinctSubscriber<T, K> extends BasicFuseableSubscriber<T, T> {

        final Collection<? super K> collection;

        final Function<? super T, K> keySelector;

        DistinctSubscriber(Subscriber<? super T> actual, Function<? super T, K> keySelector, Collection<? super K> collection) {
            super(actual);
            this.keySelector = keySelector;
            this.collection = collection;
        }

        @Override
        public void onNext(T value) {
            if (done) {
                return;
            }
            if (sourceMode == NONE) {
                K key;
                boolean b;

                try {
                    key = Objects.requireNonNull(keySelector.apply(value), "The keySelector returned a null key");
                    b = collection.add(key);
                } catch (Throwable ex) {
                    fail(ex);
                    return;
                }

                if (b) {
                    downstream.onNext(value);
                } else {
                    upstream.request(1);
                }
            } else {
                downstream.onNext(null);
            }
        }

        @Override
        public void onError(Throwable e) {
            if (done) {
                RxJavaPlugins.onError(e);
            } else {
                done = true;
                collection.clear();
                downstream.onError(e);
            }
        }

        @Override
        public void onComplete() {
            if (!done) {
                done = true;
                collection.clear();
                downstream.onComplete();
            }
        }

        @Override
        public int requestFusion(int mode) {
            return transitiveBoundaryFusion(mode);
        }

        @Nullable
        @Override
        public T poll() throws Throwable {
            for (;;) {
                T v = qs.poll();

                if (v == null || collection.add(Objects.requireNonNull(keySelector.apply(v), "The keySelector returned a null key"))) {
                    return v;
                } else {
                    if (sourceMode == QueueFuseable.ASYNC) {
                        upstream.request(1);
                    }
                }
            }
        }

        @Override
        public void clear() {
            collection.clear();
            super.clear();
        }
    }
}
