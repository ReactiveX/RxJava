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

import io.reactivex.rxjava3.exceptions.*;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.internal.fuseable.ConditionalSubscriber;
import io.reactivex.rxjava3.internal.subscriptions.SubscriptionHelper;
import io.reactivex.rxjava3.parallel.*;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;

import java.util.Objects;

/**
 * Calls a Consumer for each upstream value passing by
 * and handles any failure with a handler function.
 * <p>History: 2.0.8 - experimental
 * @param <T> the input value type
 * @since 2.2
 */
public final class ParallelDoOnNextTry<T> extends ParallelFlowable<T> {

    final ParallelFlowable<T> source;

    final Consumer<? super T> onNext;

    final BiFunction<? super Long, ? super Throwable, ParallelFailureHandling> errorHandler;

    public ParallelDoOnNextTry(ParallelFlowable<T> source, Consumer<? super T> onNext,
            BiFunction<? super Long, ? super Throwable, ParallelFailureHandling> errorHandler) {
        this.source = source;
        this.onNext = onNext;
        this.errorHandler = errorHandler;
    }

    @Override
    public void subscribe(Subscriber<? super T>[] subscribers) {
        if (!validate(subscribers)) {
            return;
        }

        int n = subscribers.length;
        @SuppressWarnings("unchecked")
        Subscriber<? super T>[] parents = new Subscriber[n];

        for (int i = 0; i < n; i++) {
            Subscriber<? super T> a = subscribers[i];
            if (a instanceof ConditionalSubscriber) {
                parents[i] = new ParallelDoOnNextConditionalSubscriber<T>((ConditionalSubscriber<? super T>)a, onNext, errorHandler);
            } else {
                parents[i] = new ParallelDoOnNextSubscriber<T>(a, onNext, errorHandler);
            }
        }

        source.subscribe(parents);
    }

    @Override
    public int parallelism() {
        return source.parallelism();
    }

    static final class ParallelDoOnNextSubscriber<T> implements ConditionalSubscriber<T>, Subscription {

        final Subscriber<? super T> downstream;

        final Consumer<? super T> onNext;

        final BiFunction<? super Long, ? super Throwable, ParallelFailureHandling> errorHandler;

        Subscription upstream;

        boolean done;

        ParallelDoOnNextSubscriber(Subscriber<? super T> actual, Consumer<? super T> onNext,
                BiFunction<? super Long, ? super Throwable, ParallelFailureHandling> errorHandler) {
            this.downstream = actual;
            this.onNext = onNext;
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
            if (!tryOnNext(t)) {
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
                try {
                    onNext.accept(t);
                } catch (Throwable ex) {
                    Exceptions.throwIfFatal(ex);

                    ParallelFailureHandling h;

                    try {
                        h = Objects.requireNonNull(errorHandler.apply(++retries, ex), "The errorHandler returned a null item");
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

                downstream.onNext(t);
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
    static final class ParallelDoOnNextConditionalSubscriber<T> implements ConditionalSubscriber<T>, Subscription {

        final ConditionalSubscriber<? super T> downstream;

        final Consumer<? super T> onNext;

        final BiFunction<? super Long, ? super Throwable, ParallelFailureHandling> errorHandler;

        Subscription upstream;

        boolean done;

        ParallelDoOnNextConditionalSubscriber(ConditionalSubscriber<? super T> actual,
                Consumer<? super T> onNext,
                BiFunction<? super Long, ? super Throwable, ParallelFailureHandling> errorHandler) {
            this.downstream = actual;
            this.onNext = onNext;
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
                try {
                    onNext.accept(t);
                } catch (Throwable ex) {
                    Exceptions.throwIfFatal(ex);

                    ParallelFailureHandling h;

                    try {
                        h = Objects.requireNonNull(errorHandler.apply(++retries, ex), "The errorHandler returned a null item");
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

                return downstream.tryOnNext(t);
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
