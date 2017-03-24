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

import io.reactivex.FlowableSubscriber;
import io.reactivex.exceptions.*;
import io.reactivex.functions.*;
import io.reactivex.internal.functions.ObjectHelper;
import io.reactivex.internal.subscriptions.*;
import io.reactivex.parallel.ParallelFlowable;
import io.reactivex.plugins.RxJavaPlugins;

/**
 * Execute a Consumer in each 'rail' for the current element passing through.
 *
 * @param <T> the value type
 */
public final class ParallelPeek<T> extends ParallelFlowable<T> {

    final ParallelFlowable<T> source;

    final Consumer<? super T> onNext;
    final Consumer<? super T> onAfterNext;
    final Consumer<? super Throwable> onError;
    final Action onComplete;
    final Action onAfterTerminated;
    final Consumer<? super Subscription> onSubscribe;
    final LongConsumer onRequest;
    final Action onCancel;

    public ParallelPeek(ParallelFlowable<T> source,
            Consumer<? super T> onNext,
            Consumer<? super T> onAfterNext,
            Consumer<? super Throwable> onError,
            Action onComplete,
            Action onAfterTerminated,
            Consumer<? super Subscription> onSubscribe,
            LongConsumer onRequest,
            Action onCancel
    ) {
        this.source = source;

        this.onNext = ObjectHelper.requireNonNull(onNext, "onNext is null");
        this.onAfterNext = ObjectHelper.requireNonNull(onAfterNext, "onAfterNext is null");
        this.onError = ObjectHelper.requireNonNull(onError, "onError is null");
        this.onComplete = ObjectHelper.requireNonNull(onComplete, "onComplete is null");
        this.onAfterTerminated = ObjectHelper.requireNonNull(onAfterTerminated, "onAfterTerminated is null");
        this.onSubscribe = ObjectHelper.requireNonNull(onSubscribe, "onSubscribe is null");
        this.onRequest = ObjectHelper.requireNonNull(onRequest, "onRequest is null");
        this.onCancel = ObjectHelper.requireNonNull(onCancel, "onCancel is null");
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
            parents[i] = new ParallelPeekSubscriber<T>(subscribers[i], this);
        }

        source.subscribe(parents);
    }

    @Override
    public int parallelism() {
        return source.parallelism();
    }

    static final class ParallelPeekSubscriber<T> implements FlowableSubscriber<T>, Subscription {

        final Subscriber<? super T> actual;

        final ParallelPeek<T> parent;

        Subscription s;

        boolean done;

        ParallelPeekSubscriber(Subscriber<? super T> actual, ParallelPeek<T> parent) {
            this.actual = actual;
            this.parent = parent;
        }

        @Override
        public void request(long n) {
            try {
                parent.onRequest.accept(n);
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                RxJavaPlugins.onError(ex);
            }
            s.request(n);
        }

        @Override
        public void cancel() {
            try {
                parent.onCancel.run();
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                RxJavaPlugins.onError(ex);
            }
            s.cancel();
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.validate(this.s, s)) {
                this.s = s;

                try {
                    parent.onSubscribe.accept(s);
                } catch (Throwable ex) {
                    Exceptions.throwIfFatal(ex);
                    s.cancel();
                    actual.onSubscribe(EmptySubscription.INSTANCE);
                    onError(ex);
                    return;
                }

                actual.onSubscribe(this);
            }
        }

        @Override
        public void onNext(T t) {
            if (!done) {
                try {
                    parent.onNext.accept(t);
                } catch (Throwable ex) {
                    Exceptions.throwIfFatal(ex);
                    onError(ex);
                    return;
                }

                actual.onNext(t);

                try {
                    parent.onAfterNext.accept(t);
                } catch (Throwable ex) {
                    Exceptions.throwIfFatal(ex);
                    onError(ex);
                }
            }
        }

        @Override
        public void onError(Throwable t) {
            if (done) {
                RxJavaPlugins.onError(t);
                return;
            }
            done = true;

            try {
                parent.onError.accept(t);
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                t = new CompositeException(t, ex);
            }
            actual.onError(t);

            try {
                parent.onAfterTerminated.run();
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                RxJavaPlugins.onError(ex);
            }
        }

        @Override
        public void onComplete() {
            if (!done) {
                done = true;
                try {
                    parent.onComplete.run();
                } catch (Throwable ex) {
                    Exceptions.throwIfFatal(ex);
                    actual.onError(ex);
                    return;
                }
                actual.onComplete();

                try {
                    parent.onAfterTerminated.run();
                } catch (Throwable ex) {
                    Exceptions.throwIfFatal(ex);
                    RxJavaPlugins.onError(ex);
                }
            }
        }
    }
}
