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
import java.util.concurrent.atomic.AtomicReference;

import org.reactivestreams.*;

import io.reactivex.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.functions.Function;
import io.reactivex.internal.disposables.DisposableHelper;
import io.reactivex.internal.functions.ObjectHelper;
import io.reactivex.internal.subscribers.FullArbiterSubscriber;
import io.reactivex.internal.subscriptions.*;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.subscribers.*;

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
            source.subscribe(new TimeoutSubscriber<T, U, V>(
                    new SerializedSubscriber<T>(s),
                    firstTimeoutIndicator, itemTimeoutIndicator));
        } else {
            source.subscribe(new TimeoutOtherSubscriber<T, U, V>(
                    s, firstTimeoutIndicator, itemTimeoutIndicator, other));
        }
    }

    static final class TimeoutSubscriber<T, U, V> implements FlowableSubscriber<T>, Subscription, OnTimeout {
        final Subscriber<? super T> actual;
        final Publisher<U> firstTimeoutIndicator;
        final Function<? super T, ? extends Publisher<V>> itemTimeoutIndicator;

        Subscription s;

        volatile boolean cancelled;

        volatile long index;

        final AtomicReference<Disposable> timeout = new AtomicReference<Disposable>();

        TimeoutSubscriber(Subscriber<? super T> actual,
                Publisher<U> firstTimeoutIndicator,
                Function<? super T, ? extends Publisher<V>> itemTimeoutIndicator) {
            this.actual = actual;
            this.firstTimeoutIndicator = firstTimeoutIndicator;
            this.itemTimeoutIndicator = itemTimeoutIndicator;
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (!SubscriptionHelper.validate(this.s, s)) {
                return;
            }
            this.s = s;

            if (cancelled) {
                return;
            }

            Subscriber<? super T> a = actual;

            Publisher<U> p = firstTimeoutIndicator;

            if (p != null) {
                TimeoutInnerSubscriber<T, U, V> tis = new TimeoutInnerSubscriber<T, U, V>(this, 0);

                if (timeout.compareAndSet(null, tis)) {
                    a.onSubscribe(this);
                    p.subscribe(tis);
                }
            } else {
                a.onSubscribe(this);
            }
        }

        @Override
        public void onNext(T t) {
            long idx = index + 1;
            index = idx;

            actual.onNext(t);

            Disposable d = timeout.get();
            if (d != null) {
                d.dispose();
            }

            Publisher<V> p;

            try {
                p = ObjectHelper.requireNonNull(itemTimeoutIndicator.apply(t), "The publisher returned is null");
            } catch (Throwable e) {
                Exceptions.throwIfFatal(e);
                cancel();
                actual.onError(e);
                return;
            }

            TimeoutInnerSubscriber<T, U, V> tis = new TimeoutInnerSubscriber<T, U, V>(this, idx);

            if (timeout.compareAndSet(d, tis)) {
                p.subscribe(tis);
            }
        }

        @Override
        public void onError(Throwable t) {
            cancel();
            actual.onError(t);
        }

        @Override
        public void onComplete() {
            cancel();
            actual.onComplete();
        }

        @Override
        public void request(long n) {
            s.request(n);
        }

        @Override
        public void cancel() {
            cancelled = true;
            s.cancel();
            DisposableHelper.dispose(timeout);
        }

        @Override
        public void timeout(long idx) {
            if (idx == index) {
                cancel();
                actual.onError(new TimeoutException());
            }
        }
    }

    interface OnTimeout {
        void timeout(long index);

        void onError(Throwable e);
    }

    static final class TimeoutInnerSubscriber<T, U, V> extends DisposableSubscriber<Object> {
        final OnTimeout parent;
        final long index;

        boolean done;

        TimeoutInnerSubscriber(OnTimeout parent, final long index) {
            this.parent = parent;
            this.index = index;
        }

        @Override
        public void onNext(Object t) {
            if (done) {
                return;
            }
            done = true;
            cancel();
            parent.timeout(index);
        }

        @Override
        public void onError(Throwable t) {
            if (done) {
                RxJavaPlugins.onError(t);
                return;
            }
            done = true;
            parent.onError(t);
        }

        @Override
        public void onComplete() {
            if (done) {
                return;
            }
            done = true;
            parent.timeout(index);
        }
    }

    static final class TimeoutOtherSubscriber<T, U, V> implements FlowableSubscriber<T>, Disposable, OnTimeout {
        final Subscriber<? super T> actual;
        final Publisher<U> firstTimeoutIndicator;
        final Function<? super T, ? extends Publisher<V>> itemTimeoutIndicator;
        final Publisher<? extends T> other;
        final FullArbiter<T> arbiter;

        Subscription s;

        boolean done;

        volatile boolean cancelled;

        volatile long index;

        final AtomicReference<Disposable> timeout = new AtomicReference<Disposable>();

        TimeoutOtherSubscriber(Subscriber<? super T> actual,
                Publisher<U> firstTimeoutIndicator,
                Function<? super T, ? extends Publisher<V>> itemTimeoutIndicator, Publisher<? extends T> other) {
            this.actual = actual;
            this.firstTimeoutIndicator = firstTimeoutIndicator;
            this.itemTimeoutIndicator = itemTimeoutIndicator;
            this.other = other;
            this.arbiter = new FullArbiter<T>(actual, this, 8);
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (!SubscriptionHelper.validate(this.s, s)) {
                return;
            }
            this.s = s;

            if (!arbiter.setSubscription(s)) {
                return;
            }
            Subscriber<? super T> a = actual;

            Publisher<U> p = firstTimeoutIndicator;

            if (p != null) {
                TimeoutInnerSubscriber<T, U, V> tis = new TimeoutInnerSubscriber<T, U, V>(this, 0);

                if (timeout.compareAndSet(null, tis)) {
                    a.onSubscribe(arbiter);
                    p.subscribe(tis);
                }
            } else {
                a.onSubscribe(arbiter);
            }
        }

        @Override
        public void onNext(T t) {
            if (done) {
                return;
            }
            long idx = index + 1;
            index = idx;

            if (!arbiter.onNext(t, s)) {
                return;
            }

            Disposable d = timeout.get();
            if (d != null) {
                d.dispose();
            }

            Publisher<V> p;

            try {
                p = ObjectHelper.requireNonNull(itemTimeoutIndicator.apply(t), "The publisher returned is null");
            } catch (Throwable e) {
                Exceptions.throwIfFatal(e);
                actual.onError(e);
                return;
            }

            TimeoutInnerSubscriber<T, U, V> tis = new TimeoutInnerSubscriber<T, U, V>(this, idx);

            if (timeout.compareAndSet(d, tis)) {
                p.subscribe(tis);
            }
        }

        @Override
        public void onError(Throwable t) {
            if (done) {
                RxJavaPlugins.onError(t);
                return;
            }
            done = true;
            dispose();
            arbiter.onError(t, s);
        }

        @Override
        public void onComplete() {
            if (done) {
                return;
            }
            done = true;
            dispose();
            arbiter.onComplete(s);
        }

        @Override
        public void dispose() {
            cancelled = true;
            s.cancel();
            DisposableHelper.dispose(timeout);
        }

        @Override
        public boolean isDisposed() {
            return cancelled;
        }

        @Override
        public void timeout(long idx) {
            if (idx == index) {
                dispose();
                other.subscribe(new FullArbiterSubscriber<T>(arbiter));
            }
        }
    }
}
