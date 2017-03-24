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

import java.util.concurrent.atomic.*;

import org.reactivestreams.*;

import io.reactivex.*;
import io.reactivex.exceptions.MissingBackpressureException;
import io.reactivex.internal.fuseable.*;
import io.reactivex.internal.queue.SpscArrayQueue;
import io.reactivex.internal.subscriptions.SubscriptionHelper;
import io.reactivex.internal.util.*;
import io.reactivex.parallel.ParallelFlowable;
import io.reactivex.plugins.RxJavaPlugins;

/**
 * Merges the individual 'rails' of the source ParallelFlowable, unordered,
 * into a single regular Publisher sequence (exposed as Flowable).
 *
 * @param <T> the value type
 */
public final class ParallelJoin<T> extends Flowable<T> {

    final ParallelFlowable<? extends T> source;

    final int prefetch;

    final boolean delayErrors;

    public ParallelJoin(ParallelFlowable<? extends T> source, int prefetch, boolean delayErrors) {
        this.source = source;
        this.prefetch = prefetch;
        this.delayErrors = delayErrors;
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> s) {
        JoinSubscriptionBase<T> parent;
        if (delayErrors) {
            parent = new JoinSubscriptionDelayError<T>(s, source.parallelism(), prefetch);
        } else {
            parent = new JoinSubscription<T>(s, source.parallelism(), prefetch);
        }
        s.onSubscribe(parent);
        source.subscribe(parent.subscribers);
    }

    abstract static class JoinSubscriptionBase<T> extends AtomicInteger
    implements Subscription {

        private static final long serialVersionUID = 3100232009247827843L;

        final Subscriber<? super T> actual;

        final JoinInnerSubscriber<T>[] subscribers;

        final AtomicThrowable errors = new AtomicThrowable();

        final AtomicLong requested = new AtomicLong();

        volatile boolean cancelled;

        final AtomicInteger done = new AtomicInteger();

        JoinSubscriptionBase(Subscriber<? super T> actual, int n, int prefetch) {
            this.actual = actual;
            @SuppressWarnings("unchecked")
            JoinInnerSubscriber<T>[] a = new JoinInnerSubscriber[n];

            for (int i = 0; i < n; i++) {
                a[i] = new JoinInnerSubscriber<T>(this, prefetch);
            }

            this.subscribers = a;
            done.lazySet(n);
        }

        @Override
        public void request(long n) {
            if (SubscriptionHelper.validate(n)) {
                BackpressureHelper.add(requested, n);
                drain();
            }
        }

        @Override
        public void cancel() {
            if (!cancelled) {
                cancelled = true;

                cancelAll();

                if (getAndIncrement() == 0) {
                    cleanup();
                }
            }
        }

        void cancelAll() {
            for (int i = 0; i < subscribers.length; i++) {
                JoinInnerSubscriber<T> s = subscribers[i];
                s.cancel();
            }
        }

        void cleanup() {
            for (int i = 0; i < subscribers.length; i++) {
                JoinInnerSubscriber<T> s = subscribers[i];
                s.queue = null;
            }
        }

        abstract void onNext(JoinInnerSubscriber<T> inner, T value);

        abstract void onError(Throwable e);

        abstract void onComplete();

        abstract void drain();
    }

    static final class JoinSubscription<T> extends JoinSubscriptionBase<T> {

        private static final long serialVersionUID = 6312374661811000451L;

        JoinSubscription(Subscriber<? super T> actual, int n, int prefetch) {
            super(actual, n, prefetch);
        }

        @Override
        public void onNext(JoinInnerSubscriber<T> inner, T value) {
            if (get() == 0 && compareAndSet(0, 1)) {
                if (requested.get() != 0) {
                    actual.onNext(value);
                    if (requested.get() != Long.MAX_VALUE) {
                        requested.decrementAndGet();
                    }
                    inner.request(1);
                } else {
                    SimplePlainQueue<T> q = inner.getQueue();

                    if (!q.offer(value)) {
                        cancelAll();
                        Throwable mbe = new MissingBackpressureException("Queue full?!");
                        if (errors.compareAndSet(null, mbe)) {
                            actual.onError(mbe);
                        } else {
                            RxJavaPlugins.onError(mbe);
                        }
                        return;
                    }
                }
                if (decrementAndGet() == 0) {
                    return;
                }
            } else {
                SimplePlainQueue<T> q = inner.getQueue();

                if (!q.offer(value)) {
                    cancelAll();
                    onError(new MissingBackpressureException("Queue full?!"));
                    return;
                }

                if (getAndIncrement() != 0) {
                    return;
                }
            }

            drainLoop();
        }

        @Override
        public void onError(Throwable e) {
            if (errors.compareAndSet(null, e)) {
                cancelAll();
                drain();
            } else {
                if (e != errors.get()) {
                    RxJavaPlugins.onError(e);
                }
            }
        }

        @Override
        public void onComplete() {
            done.decrementAndGet();
            drain();
        }

        @Override
        void drain() {
            if (getAndIncrement() != 0) {
                return;
            }

            drainLoop();
        }

        void drainLoop() {
            int missed = 1;

            JoinInnerSubscriber<T>[] s = this.subscribers;
            int n = s.length;
            Subscriber<? super T> a = this.actual;

            for (;;) {

                long r = requested.get();
                long e = 0;

                middle:
                while (e != r) {
                    if (cancelled) {
                        cleanup();
                        return;
                    }

                    Throwable ex = errors.get();
                    if (ex != null) {
                        cleanup();
                        a.onError(ex);
                        return;
                    }

                    boolean d = done.get() == 0;

                    boolean empty = true;

                    for (int i = 0; i < s.length; i++) {
                        JoinInnerSubscriber<T> inner = s[i];
                        SimplePlainQueue<T> q = inner.queue;
                        if (q != null) {
                            T v = q.poll();

                            if (v != null) {
                                empty = false;
                                a.onNext(v);
                                inner.requestOne();
                                if (++e == r) {
                                    break middle;
                                }
                            }
                        }
                    }

                    if (d && empty) {
                        a.onComplete();
                        return;
                    }

                    if (empty) {
                        break;
                    }
                }

                if (e == r) {
                    if (cancelled) {
                        cleanup();
                        return;
                    }

                    Throwable ex = errors.get();
                    if (ex != null) {
                        cleanup();
                        a.onError(ex);
                        return;
                    }

                    boolean d = done.get() == 0;

                    boolean empty = true;

                    for (int i = 0; i < n; i++) {
                        JoinInnerSubscriber<T> inner = s[i];

                        SimpleQueue<T> q = inner.queue;
                        if (q != null && !q.isEmpty()) {
                            empty = false;
                            break;
                        }
                    }

                    if (d && empty) {
                        a.onComplete();
                        return;
                    }
                }

                if (e != 0 && r != Long.MAX_VALUE) {
                    requested.addAndGet(-e);
                }

                int w = get();
                if (w == missed) {
                    missed = addAndGet(-missed);
                    if (missed == 0) {
                        break;
                    }
                } else {
                    missed = w;
                }
            }
        }
    }

    static final class JoinSubscriptionDelayError<T> extends JoinSubscriptionBase<T> {

        private static final long serialVersionUID = -5737965195918321883L;

        JoinSubscriptionDelayError(Subscriber<? super T> actual, int n, int prefetch) {
            super(actual, n, prefetch);
        }

        @Override
        void onNext(JoinInnerSubscriber<T> inner, T value) {
            if (get() == 0 && compareAndSet(0, 1)) {
                if (requested.get() != 0) {
                    actual.onNext(value);
                    if (requested.get() != Long.MAX_VALUE) {
                        requested.decrementAndGet();
                    }
                    inner.request(1);
                } else {
                    SimplePlainQueue<T> q = inner.getQueue();

                    if (!q.offer(value)) {
                        inner.cancel();
                        errors.addThrowable(new MissingBackpressureException("Queue full?!"));
                        done.decrementAndGet();
                        drainLoop();
                        return;
                    }
                }
                if (decrementAndGet() == 0) {
                    return;
                }
            } else {
                SimplePlainQueue<T> q = inner.getQueue();

                if (!q.offer(value)) {
                    if (inner.cancel()) {
                        errors.addThrowable(new MissingBackpressureException("Queue full?!"));
                        done.decrementAndGet();
                    }
                }

                if (getAndIncrement() != 0) {
                    return;
                }
            }

            drainLoop();
        }

        @Override
        void onError(Throwable e) {
            errors.addThrowable(e);
            done.decrementAndGet();
            drain();
        }

        @Override
        void onComplete() {
            done.decrementAndGet();
            drain();
        }

        @Override
        void drain() {
            if (getAndIncrement() != 0) {
                return;
            }

            drainLoop();
        }

        void drainLoop() {
            int missed = 1;

            JoinInnerSubscriber<T>[] s = this.subscribers;
            int n = s.length;
            Subscriber<? super T> a = this.actual;

            for (;;) {

                long r = requested.get();
                long e = 0;

                middle:
                while (e != r) {
                    if (cancelled) {
                        cleanup();
                        return;
                    }

                    boolean d = done.get() == 0;

                    boolean empty = true;

                    for (int i = 0; i < n; i++) {
                        JoinInnerSubscriber<T> inner = s[i];

                        SimplePlainQueue<T> q = inner.queue;
                        if (q != null) {
                            T v = q.poll();

                            if (v != null) {
                                empty = false;
                                a.onNext(v);
                                inner.requestOne();
                                if (++e == r) {
                                    break middle;
                                }
                            }
                        }
                    }

                    if (d && empty) {
                        Throwable ex = errors.get();
                        if (ex != null) {
                            a.onError(errors.terminate());
                        } else {
                            a.onComplete();
                        }
                        return;
                    }

                    if (empty) {
                        break;
                    }
                }

                if (e == r) {
                    if (cancelled) {
                        cleanup();
                        return;
                    }

                    boolean d = done.get() == 0;

                    boolean empty = true;

                    for (int i = 0; i < n; i++) {
                        JoinInnerSubscriber<T> inner = s[i];

                        SimpleQueue<T> q = inner.queue;
                        if (q != null && !q.isEmpty()) {
                            empty = false;
                            break;
                        }
                    }

                    if (d && empty) {
                        Throwable ex = errors.get();
                        if (ex != null) {
                            a.onError(errors.terminate());
                        } else {
                            a.onComplete();
                        }
                        return;
                    }
                }

                if (e != 0 && r != Long.MAX_VALUE) {
                    requested.addAndGet(-e);
                }

                int w = get();
                if (w == missed) {
                    missed = addAndGet(-missed);
                    if (missed == 0) {
                        break;
                    }
                } else {
                    missed = w;
                }
            }
        }
    }

    static final class JoinInnerSubscriber<T>
    extends AtomicReference<Subscription>
    implements FlowableSubscriber<T> {

        private static final long serialVersionUID = 8410034718427740355L;

        final JoinSubscriptionBase<T> parent;

        final int prefetch;

        final int limit;

        long produced;

        volatile SimplePlainQueue<T> queue;

        JoinInnerSubscriber(JoinSubscriptionBase<T> parent, int prefetch) {
            this.parent = parent;
            this.prefetch = prefetch ;
            this.limit = prefetch - (prefetch >> 2);
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.setOnce(this, s)) {
                s.request(prefetch);
            }
        }

        @Override
        public void onNext(T t) {
            parent.onNext(this, t);
        }

        @Override
        public void onError(Throwable t) {
            parent.onError(t);
        }

        @Override
        public void onComplete() {
            parent.onComplete();
        }

        public void requestOne() {
            long p = produced + 1;
            if (p == limit) {
                produced = 0;
                get().request(p);
            } else {
                produced = p;
            }
        }

        public void request(long n) {
            long p = produced + n;
            if (p >= limit) {
                produced = 0;
                get().request(p);
            } else {
                produced = p;
            }
        }

        public boolean cancel() {
            return SubscriptionHelper.cancel(this);
        }

        SimplePlainQueue<T> getQueue() {
            SimplePlainQueue<T> q = queue;
            if (q == null) {
                q = new SpscArrayQueue<T>(prefetch);
                this.queue = q;
            }
            return q;
        }
    }
}
