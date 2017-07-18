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

import java.util.Arrays;
import java.util.concurrent.atomic.*;

import org.reactivestreams.*;

import io.reactivex.*;
import io.reactivex.annotations.*;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.functions.Function;
import io.reactivex.internal.functions.ObjectHelper;
import io.reactivex.internal.fuseable.ConditionalSubscriber;
import io.reactivex.internal.subscriptions.*;
import io.reactivex.internal.util.*;
import io.reactivex.plugins.RxJavaPlugins;

/**
 * Combines a main sequence of values with the latest from multiple other sequences via
 * a selector function.
 *
 * @param <T> the main sequence's type
 * @param <R> the output type
 */
public final class FlowableWithLatestFromMany<T, R> extends AbstractFlowableWithUpstream<T, R> {
    @Nullable
    final Publisher<?>[] otherArray;

    @Nullable
    final Iterable<? extends Publisher<?>> otherIterable;

    final Function<? super Object[], R> combiner;

    public FlowableWithLatestFromMany(@NonNull Flowable<T> source, @NonNull Publisher<?>[] otherArray, Function<? super Object[], R> combiner) {
        super(source);
        this.otherArray = otherArray;
        this.otherIterable = null;
        this.combiner = combiner;
    }

    public FlowableWithLatestFromMany(@NonNull Flowable<T> source, @NonNull Iterable<? extends Publisher<?>> otherIterable, @NonNull Function<? super Object[], R> combiner) {
        super(source);
        this.otherArray = null;
        this.otherIterable = otherIterable;
        this.combiner = combiner;
    }

    @Override
    protected void subscribeActual(Subscriber<? super R> s) {
        Publisher<?>[] others = otherArray;
        int n = 0;
        if (others == null) {
            others = new Publisher[8];

            try {
                for (Publisher<?> p : otherIterable) {
                    if (n == others.length) {
                        others = Arrays.copyOf(others, n + (n >> 1));
                    }
                    others[n++] = p;
                }
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                EmptySubscription.error(ex, s);
                return;
            }

        } else {
            n = others.length;
        }

        if (n == 0) {
            new FlowableMap<T, R>(source, new SingletonArrayFunc()).subscribeActual(s);
            return;
        }

        WithLatestFromSubscriber<T, R> parent = new WithLatestFromSubscriber<T, R>(s, combiner, n);
        s.onSubscribe(parent);
        parent.subscribe(others, n);

        source.subscribe(parent);
    }

    static final class WithLatestFromSubscriber<T, R>
    extends AtomicInteger
    implements ConditionalSubscriber<T>, Subscription {

        private static final long serialVersionUID = 1577321883966341961L;

        final Subscriber<? super R> actual;

        final Function<? super Object[], R> combiner;

        final WithLatestInnerSubscriber[] subscribers;

        final AtomicReferenceArray<Object> values;

        final AtomicReference<Subscription> s;

        final AtomicLong requested;

        final AtomicThrowable error;

        volatile boolean done;

        WithLatestFromSubscriber(Subscriber<? super R> actual, Function<? super Object[], R> combiner, int n) {
            this.actual = actual;
            this.combiner = combiner;
            WithLatestInnerSubscriber[] s = new WithLatestInnerSubscriber[n];
            for (int i = 0; i < n; i++) {
                s[i] = new WithLatestInnerSubscriber(this, i);
            }
            this.subscribers = s;
            this.values = new AtomicReferenceArray<Object>(n);
            this.s = new AtomicReference<Subscription>();
            this.requested = new AtomicLong();
            this.error = new AtomicThrowable();
        }

        void subscribe(Publisher<?>[] others, int n) {
            WithLatestInnerSubscriber[] subscribers = this.subscribers;
            AtomicReference<Subscription> s = this.s;
            for (int i = 0; i < n; i++) {
                if (SubscriptionHelper.isCancelled(s.get())) {
                    return;
                }
                others[i].subscribe(subscribers[i]);
            }
        }

        @Override
        public void onSubscribe(Subscription s) {
            SubscriptionHelper.deferredSetOnce(this.s, requested, s);
        }

        @Override
        public void onNext(T t) {
            if (!tryOnNext(t) && !done) {
                s.get().request(1);
            }
        }

        @Override
        public boolean tryOnNext(T t) {
            if (done) {
                return false;
            }
            AtomicReferenceArray<Object> ara = values;
            int n = ara.length();
            Object[] objects = new Object[n + 1];
            objects[0] = t;

            for (int i = 0; i < n; i++) {
                Object o = ara.get(i);
                if (o == null) {
                    // somebody hasn't signalled yet, skip this T
                    return false;
                }
                objects[i + 1] = o;
            }

            R v;

            try {
                v = ObjectHelper.requireNonNull(combiner.apply(objects), "The combiner returned a null value");
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                cancel();
                onError(ex);
                return false;
            }

            HalfSerializer.onNext(actual, v, this, error);
            return true;
        }

        @Override
        public void onError(Throwable t) {
            if (done) {
                RxJavaPlugins.onError(t);
                return;
            }
            done = true;
            cancelAllBut(-1);
            HalfSerializer.onError(actual, t, this, error);
        }

        @Override
        public void onComplete() {
            if (!done) {
                done = true;
                cancelAllBut(-1);
                HalfSerializer.onComplete(actual, this, error);
            }
        }

        @Override
        public void request(long n) {
            SubscriptionHelper.deferredRequest(s, requested, n);
        }

        @Override
        public void cancel() {
            SubscriptionHelper.cancel(s);
            for (WithLatestInnerSubscriber s : subscribers) {
                s.dispose();
            }
        }

        void innerNext(int index, Object o) {
            values.set(index, o);
        }

        void innerError(int index, Throwable t) {
            done = true;
            SubscriptionHelper.cancel(s);
            cancelAllBut(index);
            HalfSerializer.onError(actual, t, this, error);
        }

        void innerComplete(int index, boolean nonEmpty) {
            if (!nonEmpty) {
                done = true;
                SubscriptionHelper.cancel(s);
                cancelAllBut(index);
                HalfSerializer.onComplete(actual, this, error);
            }
        }

        void cancelAllBut(int index) {
            WithLatestInnerSubscriber[] subscribers = this.subscribers;
            for (int i = 0; i < subscribers.length; i++) {
                if (i != index) {
                    subscribers[i].dispose();
                }
            }
        }
    }

    static final class WithLatestInnerSubscriber
    extends AtomicReference<Subscription>
    implements FlowableSubscriber<Object> {

        private static final long serialVersionUID = 3256684027868224024L;

        final WithLatestFromSubscriber<?, ?> parent;

        final int index;

        boolean hasValue;

        WithLatestInnerSubscriber(WithLatestFromSubscriber<?, ?> parent, int index) {
            this.parent = parent;
            this.index = index;
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.setOnce(this, s)) {
                s.request(Long.MAX_VALUE);
            }
        }

        @Override
        public void onNext(Object t) {
            if (!hasValue) {
                hasValue = true;
            }
            parent.innerNext(index, t);
        }

        @Override
        public void onError(Throwable t) {
            parent.innerError(index, t);
        }

        @Override
        public void onComplete() {
            parent.innerComplete(index, hasValue);
        }

        void dispose() {
            SubscriptionHelper.cancel(this);
        }
    }

    final class SingletonArrayFunc implements Function<T, R> {
        @Override
        public R apply(T t) throws Exception {
            return ObjectHelper.requireNonNull(combiner.apply(new Object[] { t }), "The combiner returned a null value");
        }
    }
}
