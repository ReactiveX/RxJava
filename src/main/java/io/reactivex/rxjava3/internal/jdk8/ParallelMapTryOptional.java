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

package io.reactivex.rxjava3.internal.jdk8;

import java.util.*;

import org.reactivestreams.*;

import io.reactivex.rxjava3.exceptions.*;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.internal.subscriptions.SubscriptionHelper;
import io.reactivex.rxjava3.operators.ConditionalSubscriber;
import io.reactivex.rxjava3.parallel.*;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;

/**
 * Maps each 'rail' of the source ParallelFlowable with a mapper function
 * and handle any failure based on a handler function.
 * @param <T> the input value type
 * @param <R> the output value type
 * @since 3.0.0
 */
public final class ParallelMapTryOptional<T, R> extends ParallelFlowable<R> {

    final ParallelFlowable<T> source;

    final Function<? super T, Optional<? extends R>> mapper;

    final BiFunction<? super Long, ? super Throwable, ParallelFailureHandling> errorHandler;

    public ParallelMapTryOptional(
            ParallelFlowable<T> source,
            Function<? super T, Optional<? extends R>> mapper,
            BiFunction<? super Long, ? super Throwable, ParallelFailureHandling> errorHandler) {
        this.source = source;
        this.mapper = mapper;
        this.errorHandler = errorHandler;
    }

    @Override
    public void subscribe(Subscriber<? super R>[] subscribers) {
        if (!validate(subscribers)) {
            return;
        }

        int n = subscribers.length;
        @SuppressWarnings("unchecked")
        Subscriber<? super T>[] parents = new Subscriber[n];

        for (int i = 0; i < n; i++) {
            Subscriber<? super R> a = subscribers[i];
            if (a instanceof ConditionalSubscriber) {
                parents[i] = new ParallelMapTryConditionalSubscriber<>((ConditionalSubscriber<? super R>)a, mapper, errorHandler);
            } else {
                parents[i] = new ParallelMapTrySubscriber<>(a, mapper, errorHandler);
            }
        }

        source.subscribe(parents);
    }

    @Override
    public int parallelism() {
        return source.parallelism();
    }

    static final class ParallelMapTrySubscriber<T, R> implements ConditionalSubscriber<T>, Subscription {

        final Subscriber<? super R> downstream;

        final Function<? super T, Optional<? extends R>> mapper;

        final BiFunction<? super Long, ? super Throwable, ParallelFailureHandling> errorHandler;

        Subscription upstream;

        boolean done;

        ParallelMapTrySubscriber(Subscriber<? super R> actual,
                Function<? super T, Optional<? extends R>> mapper,
                BiFunction<? super Long, ? super Throwable, ParallelFailureHandling> errorHandler) {
            this.downstream = actual;
            this.mapper = mapper;
            this.errorHandler = errorHandler;
        }

        @Override
        public void request(long n) {
            upstream.request(n);
        }

        @Override
        public void cancel() {
            upstream.cancel();
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
            if (!tryOnNext(t) && !done) {
                upstream.request(1);
            }
        }

        @Override
        public boolean tryOnNext(T t) {
            if (done) {
                return false;
            }
            long retries = 0;

            for (;;) {
                Optional<? extends R> v;

                try {
                    v = Objects.requireNonNull(mapper.apply(t), "The mapper returned a null Optional");
                } catch (Throwable ex) {
                    Exceptions.throwIfFatal(ex);

                    ParallelFailureHandling h;

                    try {
                        h = Objects.requireNonNull(errorHandler.apply(++retries, ex), "The errorHandler returned a null ParallelFailureHandling");
                    } catch (Throwable exc) {
                        Exceptions.throwIfFatal(exc);
                        cancel();
                        onError(new CompositeException(ex, exc));
                        return false;
                    }

                    switch (h) {
                    case RETRY:
                        continue;
                    case SKIP:
                        return false;
                    case STOP:
                        cancel();
                        onComplete();
                        return false;
                    default:
                        cancel();
                        onError(ex);
                        return false;
                    }
                }

                if (v.isPresent()) {
                    downstream.onNext(v.get());
                    return true;
                }
                return false;
            }
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

    }
    static final class ParallelMapTryConditionalSubscriber<T, R> implements ConditionalSubscriber<T>, Subscription {

        final ConditionalSubscriber<? super R> downstream;

        final Function<? super T, Optional<? extends R>> mapper;

        final BiFunction<? super Long, ? super Throwable, ParallelFailureHandling> errorHandler;

        Subscription upstream;

        boolean done;

        ParallelMapTryConditionalSubscriber(ConditionalSubscriber<? super R> actual,
                Function<? super T, Optional<? extends R>> mapper,
                BiFunction<? super Long, ? super Throwable, ParallelFailureHandling> errorHandler) {
            this.downstream = actual;
            this.mapper = mapper;
            this.errorHandler = errorHandler;
        }

        @Override
        public void request(long n) {
            upstream.request(n);
        }

        @Override
        public void cancel() {
            upstream.cancel();
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
            if (!tryOnNext(t) && !done) {
                upstream.request(1);
            }
        }

        @Override
        public boolean tryOnNext(T t) {
            if (done) {
                return false;
            }
            long retries = 0;

            for (;;) {
                Optional<? extends R> v;

                try {
                    v = Objects.requireNonNull(mapper.apply(t), "The mapper returned a null Optional");
                } catch (Throwable ex) {
                    Exceptions.throwIfFatal(ex);

                    ParallelFailureHandling h;

                    try {
                        h = Objects.requireNonNull(errorHandler.apply(++retries, ex), "The errorHandler returned a null ParallelFailureHandling");
                    } catch (Throwable exc) {
                        Exceptions.throwIfFatal(exc);
                        cancel();
                        onError(new CompositeException(ex, exc));
                        return false;
                    }

                    switch (h) {
                    case RETRY:
                        continue;
                    case SKIP:
                        return false;
                    case STOP:
                        cancel();
                        onComplete();
                        return false;
                    default:
                        cancel();
                        onError(ex);
                        return false;
                    }
                }

                return v.isPresent() && downstream.tryOnNext(v.get());
            }
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

    }
}
