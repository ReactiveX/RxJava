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

import io.reactivex.rxjava3.exceptions.Exceptions;
import io.reactivex.rxjava3.functions.Predicate;
import io.reactivex.rxjava3.internal.fuseable.ConditionalSubscriber;
import io.reactivex.rxjava3.internal.subscriptions.SubscriptionHelper;
import io.reactivex.rxjava3.parallel.ParallelFlowable;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;

/**
 * Filters each 'rail' of the source ParallelFlowable with a predicate function.
 *
 * @param <T> the input value type
 */
public final class ParallelFilter<T> extends ParallelFlowable<T> {

    final ParallelFlowable<T> source;

    final Predicate<? super T> predicate;

    public ParallelFilter(ParallelFlowable<T> source, Predicate<? super T> predicate) {
        this.source = source;
        this.predicate = predicate;
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
                parents[i] = new ParallelFilterConditionalSubscriber<T>((ConditionalSubscriber<? super T>)a, predicate);
            } else {
                parents[i] = new ParallelFilterSubscriber<T>(a, predicate);
            }
        }

        source.subscribe(parents);
    }

    @Override
    public int parallelism() {
        return source.parallelism();
    }

    abstract static class BaseFilterSubscriber<T> implements ConditionalSubscriber<T>, Subscription {
        final Predicate<? super T> predicate;

        Subscription upstream;

        boolean done;

        BaseFilterSubscriber(Predicate<? super T> predicate) {
            this.predicate = predicate;
        }

        @Override
        public final void request(long n) {
            upstream.request(n);
        }

        @Override
        public final void cancel() {
            upstream.cancel();
        }

        @Override
        public final void onNext(T t) {
            if (!tryOnNext(t) && !done) {
                upstream.request(1);
            }
        }
    }

    static final class ParallelFilterSubscriber<T> extends BaseFilterSubscriber<T> {

        final Subscriber<? super T> downstream;

        ParallelFilterSubscriber(Subscriber<? super T> actual, Predicate<? super T> predicate) {
            super(predicate);
            this.downstream = actual;
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.validate(this.upstream, s)) {
                this.upstream = s;

                downstream.onSubscribe(this);
            }
        }

        @Override
        public boolean tryOnNext(T t) {
            if (!done) {
                boolean b;

                try {
                    b = predicate.test(t);
                } catch (Throwable ex) {
                    Exceptions.throwIfFatal(ex);
                    cancel();
                    onError(ex);
                    return false;
                }

                if (b) {
                    downstream.onNext(t);
                    return true;
                }
            }
            return false;
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
            if (!done) {
                done = true;
                downstream.onComplete();
            }
        }
    }

    static final class ParallelFilterConditionalSubscriber<T> extends BaseFilterSubscriber<T> {

        final ConditionalSubscriber<? super T> downstream;

        ParallelFilterConditionalSubscriber(ConditionalSubscriber<? super T> actual, Predicate<? super T> predicate) {
            super(predicate);
            this.downstream = actual;
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.validate(this.upstream, s)) {
                this.upstream = s;

                downstream.onSubscribe(this);
            }
        }

        @Override
        public boolean tryOnNext(T t) {
            if (!done) {
                boolean b;

                try {
                    b = predicate.test(t);
                } catch (Throwable ex) {
                    Exceptions.throwIfFatal(ex);
                    cancel();
                    onError(ex);
                    return false;
                }

                if (b) {
                    return downstream.tryOnNext(t);
                }
            }
            return false;
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
            if (!done) {
                done = true;
                downstream.onComplete();
            }
        }
    }}
