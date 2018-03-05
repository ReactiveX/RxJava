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

import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.*;

import org.reactivestreams.*;

import io.reactivex.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.functions.Function;
import io.reactivex.internal.disposables.SequentialDisposable;
import io.reactivex.internal.functions.ObjectHelper;
import io.reactivex.internal.operators.flowable.FlowableTimeoutTimed.TimeoutSupport;
import io.reactivex.internal.subscriptions.*;
import io.reactivex.plugins.RxJavaPlugins;

public final class FlowableTimeout<T, U, V> extends AbstractFlowableWithUpstream<T, T> {
    final Publisher<U> firstTimeoutIndicator;
    final Function<? super T, ? extends Publisher<V>> itemTimeoutIndicator;
    final Publisher<? extends T> other;

    public FlowableTimeout(
            Flowable<T> source,
            Publisher<U> firstTimeoutIndicator,
            Function<? super T, ? extends Publisher<V>> itemTimeoutIndicator,
            Publisher<? extends T> other) {
        super(source);
        this.firstTimeoutIndicator = firstTimeoutIndicator;
        this.itemTimeoutIndicator = itemTimeoutIndicator;
        this.other = other;
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> s) {
        if (other == null) {
            TimeoutSubscriber<T> parent = new TimeoutSubscriber<T>(s, itemTimeoutIndicator);
            s.onSubscribe(parent);
            parent.startFirstTimeout(firstTimeoutIndicator);
            source.subscribe(parent);
        } else {
            TimeoutFallbackSubscriber<T> parent = new TimeoutFallbackSubscriber<T>(s, itemTimeoutIndicator, other);
            s.onSubscribe(parent);
            parent.startFirstTimeout(firstTimeoutIndicator);
            source.subscribe(parent);
        }
    }

    interface TimeoutSelectorSupport extends TimeoutSupport {
        void onTimeoutError(long idx, Throwable ex);
    }

    static final class TimeoutSubscriber<T> extends AtomicLong
    implements FlowableSubscriber<T>, Subscription, TimeoutSelectorSupport {

        private static final long serialVersionUID = 3764492702657003550L;

        final Subscriber<? super T> actual;

        final Function<? super T, ? extends Publisher<?>> itemTimeoutIndicator;

        final SequentialDisposable task;

        final AtomicReference<Subscription> upstream;

        final AtomicLong requested;

        TimeoutSubscriber(Subscriber<? super T> actual, Function<? super T, ? extends Publisher<?>> itemTimeoutIndicator) {
            this.actual = actual;
            this.itemTimeoutIndicator = itemTimeoutIndicator;
            this.task = new SequentialDisposable();
            this.upstream = new AtomicReference<Subscription>();
            this.requested = new AtomicLong();
        }

        @Override
        public void onSubscribe(Subscription s) {
            SubscriptionHelper.deferredSetOnce(upstream, requested, s);
        }

        @Override
        public void onNext(T t) {
            long idx = get();
            if (idx == Long.MAX_VALUE || !compareAndSet(idx, idx + 1)) {
                return;
            }

            Disposable d = task.get();
            if (d != null) {
                d.dispose();
            }

            actual.onNext(t);

            Publisher<?> itemTimeoutPublisher;

            try {
                itemTimeoutPublisher = ObjectHelper.requireNonNull(
                        itemTimeoutIndicator.apply(t),
                        "The itemTimeoutIndicator returned a null Publisher.");
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                upstream.get().cancel();
                getAndSet(Long.MAX_VALUE);
                actual.onError(ex);
                return;
            }

            TimeoutConsumer consumer = new TimeoutConsumer(idx + 1, this);
            if (task.replace(consumer)) {
                itemTimeoutPublisher.subscribe(consumer);
            }
        }

        void startFirstTimeout(Publisher<?> firstTimeoutIndicator) {
            if (firstTimeoutIndicator != null) {
                TimeoutConsumer consumer = new TimeoutConsumer(0L, this);
                if (task.replace(consumer)) {
                    firstTimeoutIndicator.subscribe(consumer);
                }
            }
        }

        @Override
        public void onError(Throwable t) {
            if (getAndSet(Long.MAX_VALUE) != Long.MAX_VALUE) {
                task.dispose();

                actual.onError(t);
            } else {
                RxJavaPlugins.onError(t);
            }
        }

        @Override
        public void onComplete() {
            if (getAndSet(Long.MAX_VALUE) != Long.MAX_VALUE) {
                task.dispose();

                actual.onComplete();
            }
        }

        @Override
        public void onTimeout(long idx) {
            if (compareAndSet(idx, Long.MAX_VALUE)) {
                SubscriptionHelper.cancel(upstream);

                actual.onError(new TimeoutException());
            }
        }

        @Override
        public void onTimeoutError(long idx, Throwable ex) {
            if (compareAndSet(idx, Long.MAX_VALUE)) {
                SubscriptionHelper.cancel(upstream);

                actual.onError(ex);
            } else {
                RxJavaPlugins.onError(ex);
            }
        }

        @Override
        public void request(long n) {
            SubscriptionHelper.deferredRequest(upstream, requested, n);
        }

        @Override
        public void cancel() {
            SubscriptionHelper.cancel(upstream);
            task.dispose();
        }
    }

    static final class TimeoutFallbackSubscriber<T> extends SubscriptionArbiter
    implements FlowableSubscriber<T>, TimeoutSelectorSupport {

        private static final long serialVersionUID = 3764492702657003550L;

        final Subscriber<? super T> actual;

        final Function<? super T, ? extends Publisher<?>> itemTimeoutIndicator;

        final SequentialDisposable task;

        final AtomicReference<Subscription> upstream;

        final AtomicLong index;

        Publisher<? extends T> fallback;

        long consumed;

        TimeoutFallbackSubscriber(Subscriber<? super T> actual,
                Function<? super T, ? extends Publisher<?>> itemTimeoutIndicator,
                        Publisher<? extends T> fallback) {
            this.actual = actual;
            this.itemTimeoutIndicator = itemTimeoutIndicator;
            this.task = new SequentialDisposable();
            this.upstream = new AtomicReference<Subscription>();
            this.fallback = fallback;
            this.index = new AtomicLong();
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.setOnce(this.upstream, s)) {
                setSubscription(s);
            }
        }

        @Override
        public void onNext(T t) {
            long idx = index.get();
            if (idx == Long.MAX_VALUE || !index.compareAndSet(idx, idx + 1)) {
                return;
            }

            Disposable d = task.get();
            if (d != null) {
                d.dispose();
            }

            consumed++;

            actual.onNext(t);

            Publisher<?> itemTimeoutPublisher;

            try {
                itemTimeoutPublisher = ObjectHelper.requireNonNull(
                        itemTimeoutIndicator.apply(t),
                        "The itemTimeoutIndicator returned a null Publisher.");
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                upstream.get().cancel();
                index.getAndSet(Long.MAX_VALUE);
                actual.onError(ex);
                return;
            }

            TimeoutConsumer consumer = new TimeoutConsumer(idx + 1, this);
            if (task.replace(consumer)) {
                itemTimeoutPublisher.subscribe(consumer);
            }
        }

        void startFirstTimeout(Publisher<?> firstTimeoutIndicator) {
            if (firstTimeoutIndicator != null) {
                TimeoutConsumer consumer = new TimeoutConsumer(0L, this);
                if (task.replace(consumer)) {
                    firstTimeoutIndicator.subscribe(consumer);
                }
            }
        }

        @Override
        public void onError(Throwable t) {
            if (index.getAndSet(Long.MAX_VALUE) != Long.MAX_VALUE) {
                task.dispose();

                actual.onError(t);

                task.dispose();
            } else {
                RxJavaPlugins.onError(t);
            }
        }

        @Override
        public void onComplete() {
            if (index.getAndSet(Long.MAX_VALUE) != Long.MAX_VALUE) {
                task.dispose();

                actual.onComplete();

                task.dispose();
            }
        }

        @Override
        public void onTimeout(long idx) {
            if (index.compareAndSet(idx, Long.MAX_VALUE)) {
                SubscriptionHelper.cancel(upstream);

                Publisher<? extends T> f = fallback;
                fallback = null;

                long c = consumed;
                if (c != 0L) {
                    produced(c);
                }

                f.subscribe(new FlowableTimeoutTimed.FallbackSubscriber<T>(actual, this));
            }
        }

        @Override
        public void onTimeoutError(long idx, Throwable ex) {
            if (index.compareAndSet(idx, Long.MAX_VALUE)) {
                SubscriptionHelper.cancel(upstream);

                actual.onError(ex);
            } else {
                RxJavaPlugins.onError(ex);
            }
        }

        @Override
        public void cancel() {
            super.cancel();
            task.dispose();
        }
    }

    static final class TimeoutConsumer extends AtomicReference<Subscription>
    implements FlowableSubscriber<Object>, Disposable {

        private static final long serialVersionUID = 8708641127342403073L;

        final TimeoutSelectorSupport parent;

        final long idx;

        TimeoutConsumer(long idx, TimeoutSelectorSupport parent) {
            this.idx = idx;
            this.parent = parent;
        }

        @Override
        public void onSubscribe(Subscription s) {
            SubscriptionHelper.setOnce(this, s, Long.MAX_VALUE);
        }

        @Override
        public void onNext(Object t) {
            Subscription upstream = get();
            if (upstream != SubscriptionHelper.CANCELLED) {
                upstream.cancel();
                lazySet(SubscriptionHelper.CANCELLED);
                parent.onTimeout(idx);
            }
        }

        @Override
        public void onError(Throwable t) {
            if (get() != SubscriptionHelper.CANCELLED) {
                lazySet(SubscriptionHelper.CANCELLED);
                parent.onTimeoutError(idx, t);
            } else {
                RxJavaPlugins.onError(t);
            }
        }

        @Override
        public void onComplete() {
            if (get() != SubscriptionHelper.CANCELLED) {
                lazySet(SubscriptionHelper.CANCELLED);
                parent.onTimeout(idx);
            }
        }

        @Override
        public void dispose() {
            SubscriptionHelper.cancel(this);
        }

        @Override
        public boolean isDisposed() {
            return SubscriptionHelper.isCancelled(this.get());
        }
    }

}
