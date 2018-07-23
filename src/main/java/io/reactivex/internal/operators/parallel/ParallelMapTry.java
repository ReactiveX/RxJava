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

package io.reactivex.internal.operators.parallel;

import org.reactivestreams.*;

import io.reactivex.exceptions.*;
import io.reactivex.functions.*;
import io.reactivex.internal.functions.ObjectHelper;
import io.reactivex.internal.fuseable.ConditionalSubscriber;
import io.reactivex.internal.subscriptions.SubscriptionHelper;
import io.reactivex.parallel.*;
import io.reactivex.plugins.RxJavaPlugins;

/**
 * Maps each 'rail' of the source ParallelFlowable with a mapper function
 * and handle any failure based on a handler function.
 * <p>History: 2.0.8 - experimental
 * @param <T> the input value type
 * @param <R> the output value type
 * @since 2.2
 */
public final class ParallelMapTry<T, R> extends ParallelFlowable<R> {

    final ParallelFlowable<T> source;

    final Function<? super T, ? extends R> mapper;

    final BiFunction<? super Long, ? super Throwable, ParallelFailureHandling> errorHandler;

    public ParallelMapTry(ParallelFlowable<T> source, Function<? super T, ? extends R> mapper,
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
                parents[i] = new ParallelMapTryConditionalSubscriber<T, R>((ConditionalSubscriber<? super R>)a, mapper, errorHandler);
            } else {
                parents[i] = new ParallelMapTrySubscriber<T, R>(a, mapper, errorHandler);
            }
        }

        source.subscribe(parents);
    }

    @Override
    public int parallelism() {
        return source.parallelism();
    }

    static final class ParallelMapTrySubscriber<T, R> implements ConditionalSubscriber<T>, Subscription {

        final Subscriber<? super R> actual;

        final Function<? super T, ? extends R> mapper;

        final BiFunction<? super Long, ? super Throwable, ParallelFailureHandling> errorHandler;

        Subscription s;

        boolean done;

        ParallelMapTrySubscriber(Subscriber<? super R> actual, Function<? super T, ? extends R> mapper,
                BiFunction<? super Long, ? super Throwable, ParallelFailureHandling> errorHandler) {
            this.actual = actual;
            this.mapper = mapper;
            this.errorHandler = errorHandler;
        }

        @Override
        public void request(long n) {
            s.request(n);
        }

        @Override
        public void cancel() {
            s.cancel();
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.validate(this.s, s)) {
                this.s = s;

                actual.onSubscribe(this);
            }
        }

        @Override
        public void onNext(T t) {
            if (!tryOnNext(t) && !done) {
                s.request(1);
            }
        }

        @Override
        public boolean tryOnNext(T t) {
            if (done) {
                return false;
            }
            long retries = 0;

            for (;;) {
                R v;

                try {
                    v = ObjectHelper.requireNonNull(mapper.apply(t), "The mapper returned a null value");
                } catch (Throwable ex) {
                    Exceptions.throwIfFatal(ex);

                    ParallelFailureHandling h;

                    try {
                        h = ObjectHelper.requireNonNull(errorHandler.apply(++retries, ex), "The errorHandler returned a null item");
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

                actual.onNext(v);
                return true;
            }
        }

        @Override
        public void onError(Throwable t) {
            if (done) {
                RxJavaPlugins.onError(t);
                return;
            }
            done = true;
            actual.onError(t);
        }

        @Override
        public void onComplete() {
            if (done) {
                return;
            }
            done = true;
            actual.onComplete();
        }

    }
    static final class ParallelMapTryConditionalSubscriber<T, R> implements ConditionalSubscriber<T>, Subscription {

        final ConditionalSubscriber<? super R> actual;

        final Function<? super T, ? extends R> mapper;

        final BiFunction<? super Long, ? super Throwable, ParallelFailureHandling> errorHandler;
        Subscription s;

        boolean done;

        ParallelMapTryConditionalSubscriber(ConditionalSubscriber<? super R> actual,
                Function<? super T, ? extends R> mapper,
                BiFunction<? super Long, ? super Throwable, ParallelFailureHandling> errorHandler) {
            this.actual = actual;
            this.mapper = mapper;
            this.errorHandler = errorHandler;
        }

        @Override
        public void request(long n) {
            s.request(n);
        }

        @Override
        public void cancel() {
            s.cancel();
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.validate(this.s, s)) {
                this.s = s;

                actual.onSubscribe(this);
            }
        }

        @Override
        public void onNext(T t) {
            if (!tryOnNext(t) && !done) {
                s.request(1);
            }
        }

        @Override
        public boolean tryOnNext(T t) {
            if (done) {
                return false;
            }
            long retries = 0;

            for (;;) {
                R v;

                try {
                    v = ObjectHelper.requireNonNull(mapper.apply(t), "The mapper returned a null value");
                } catch (Throwable ex) {
                    Exceptions.throwIfFatal(ex);

                    ParallelFailureHandling h;

                    try {
                        h = ObjectHelper.requireNonNull(errorHandler.apply(++retries, ex), "The errorHandler returned a null item");
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

                return actual.tryOnNext(v);
            }
        }

        @Override
        public void onError(Throwable t) {
            if (done) {
                RxJavaPlugins.onError(t);
                return;
            }
            done = true;
            actual.onError(t);
        }

        @Override
        public void onComplete() {
            if (done) {
                return;
            }
            done = true;
            actual.onComplete();
        }

    }
}
