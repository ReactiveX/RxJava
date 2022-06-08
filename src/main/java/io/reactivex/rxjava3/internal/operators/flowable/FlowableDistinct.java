/*
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

import io.reactivex.rxjava3.annotations.Nullable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.exceptions.Exceptions;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.internal.observers.ConsumerSingleObserver;
import io.reactivex.rxjava3.internal.subscribers.BasicFuseableSubscriber;
import io.reactivex.rxjava3.internal.subscriptions.EmptySubscription;
import io.reactivex.rxjava3.internal.util.ExceptionHelper;
import io.reactivex.rxjava3.operators.QueueFuseable;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import org.reactivestreams.Subscriber;

import java.util.Objects;

public final class FlowableDistinct<T, K> extends AbstractFlowableWithUpstream<T, T> {

    final Function<? super T, Single<Boolean>> duplicateDetector;

    public FlowableDistinct(Flowable<T> source, Function<? super T, Single<Boolean>> duplicateDetector) {
        super(source);
        this.duplicateDetector = duplicateDetector;
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> subscriber) {
        Function<? super T, Single<Boolean>> duplicateDetector;
        try {
            duplicateDetector = ExceptionHelper.nullCheck(this.duplicateDetector, "The duplicateDetector cannot be null");
        } catch (Throwable ex) {
            Exceptions.throwIfFatal(ex);
            EmptySubscription.error(ex, subscriber);
            return;
        }

        source.subscribe(new DistinctSubscriber<>(subscriber, duplicateDetector));
    }

    static final class DistinctSubscriber<T, K> extends BasicFuseableSubscriber<T, T> {

        final Function<? super T, Single<Boolean>> duplicateDetector;

        DistinctSubscriber(Subscriber<? super T> actual, Function<? super T, Single<Boolean>> duplicateDetector) {
            super(actual);
            this.duplicateDetector = duplicateDetector;
        }

        @Override
        public void onNext(T value) {
            if (done) {
                return;
            }
            if (sourceMode == NONE) {
                try {
                    Objects.requireNonNull(duplicateDetector.apply(value), "The duplicateDetector returned null").subscribe(new ConsumerSingleObserver<>(
                            isDuplicate -> {
                                if (isDuplicate) {
                                    upstream.request(1);
                                } else {
                                    downstream.onNext(value);
                                }
                            }, this::fail));
                } catch (Throwable ex) {
                    fail(ex);
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
                downstream.onError(e);
            }
        }

        @Override
        public void onComplete() {
            if (!done) {
                done = true;
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
                if (v == null || !Objects.requireNonNull(duplicateDetector.apply(v), "The duplicateDetector returned null").blockingGet()) {
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
            super.clear();
        }
    }
}
