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

package io.reactivex.rxjava3.tck;

import java.util.concurrent.atomic.*;

import org.reactivestreams.*;

import io.reactivex.rxjava3.core.FlowableSubscriber;
import io.reactivex.rxjava3.internal.subscriptions.*;
import io.reactivex.rxjava3.processors.FlowableProcessor;

/**
 * A FlowableProcessor wrapper that disposes the Subscription set via
 * onSubscribe if the number of subscribers reaches zero.
 *
 * @param <T> the upstream and downstream value type
 * @since 2.1.8
 */
/* public */final class RefCountProcessor<T> extends FlowableProcessor<T> implements Subscription {

    final FlowableProcessor<T> actual;

    final AtomicReference<Subscription> upstream;

    final AtomicReference<RefCountSubscriber<T>[]> subscribers;

    @SuppressWarnings("rawtypes")
    static final RefCountSubscriber[] EMPTY = new RefCountSubscriber[0];

    @SuppressWarnings("rawtypes")
    static final RefCountSubscriber[] TERMINATED = new RefCountSubscriber[0];

    @SuppressWarnings("unchecked")
    RefCountProcessor(FlowableProcessor<T> actual) {
        this.actual = actual;
        this.upstream = new AtomicReference<>();
        this.subscribers = new AtomicReference<>(EMPTY);
    }

    @Override
    public void onSubscribe(Subscription s) {
        if (SubscriptionHelper.setOnce(upstream, s)) {
            actual.onSubscribe(this);
        }
    }

    @Override
    public void onNext(T t) {
        actual.onNext(t);
    }

    @Override
    public void onError(Throwable t) {
        upstream.lazySet(SubscriptionHelper.CANCELLED);
        actual.onError(t);
    }

    @Override
    public void onComplete() {
        upstream.lazySet(SubscriptionHelper.CANCELLED);
        actual.onComplete();
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> s) {
        RefCountSubscriber<T> rcs = new RefCountSubscriber<>(s, this);
        if (!add(rcs)) {
            EmptySubscription.error(new IllegalStateException("RefCountProcessor terminated"), s);
            return;
        }
        actual.subscribe(rcs);
    }

    @Override
    public boolean hasComplete() {
        return actual.hasComplete();
    }

    @Override
    public boolean hasThrowable() {
        return actual.hasThrowable();
    }

    @Override
    public Throwable getThrowable() {
        return actual.getThrowable();
    }

    @Override
    public boolean hasSubscribers() {
        return actual.hasSubscribers();
    }

    @Override
    public void cancel() {
        SubscriptionHelper.cancel(upstream);
    }

    @Override
    public void request(long n) {
        upstream.get().request(n);
    }

    boolean add(RefCountSubscriber<T> rcs) {
        for (;;) {
            RefCountSubscriber<T>[] a = subscribers.get();
            if (a == TERMINATED) {
                return false;
            }
            int n = a.length;
            @SuppressWarnings("unchecked")
            RefCountSubscriber<T>[] b = new RefCountSubscriber[n + 1];
            System.arraycopy(a, 0, b, 0, n);
            b[n] = rcs;
            if (subscribers.compareAndSet(a, b)) {
                return true;
            }
        }
    }

    @SuppressWarnings("unchecked")
    void remove(RefCountSubscriber<T> rcs) {
        for (;;) {
            RefCountSubscriber<T>[] a = subscribers.get();
            int n = a.length;
            if (n == 0) {
                break;
            }
            int j = -1;

            for (int i = 0; i < n; i++) {
                if (rcs == a[i]) {
                    j = i;
                    break;
                }
            }

            if (j < 0) {
                break;
            }

            RefCountSubscriber<T>[] b;
            if (n == 1) {
                b = TERMINATED;
            } else {
                b = new RefCountSubscriber[n - 1];
                System.arraycopy(a, 0, b, 0, j);
                System.arraycopy(a, j + 1, b, j, n - j - 1);
            }
            if (subscribers.compareAndSet(a, b)) {
                if (b == TERMINATED) {
                    cancel();
                }
                break;
            }
        }
    }

    static final class RefCountSubscriber<T> extends AtomicBoolean implements FlowableSubscriber<T>, Subscription {

        private static final long serialVersionUID = -4317488092687530631L;

        final Subscriber<? super T> downstream;

        final RefCountProcessor<T> parent;

        Subscription upstream;

        RefCountSubscriber(Subscriber<? super T> actual, RefCountProcessor<T> parent) {
            this.downstream = actual;
            this.parent = parent;
        }

        @Override
        public void request(long n) {
            upstream.request(n);
        }

        @Override
        public void cancel() {
            lazySet(true);
            upstream.cancel();
            parent.remove(this);
        }

        @Override
        public void onSubscribe(Subscription s) {
            this.upstream = s;
            downstream.onSubscribe(this);
        }

        @Override
        public void onNext(T t) {
            downstream.onNext(t);
        }

        @Override
        public void onError(Throwable t) {
            downstream.onError(t);
        }

        @Override
        public void onComplete() {
            downstream.onComplete();
        }
    }
}
