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

import io.reactivex.FlowableSubscriber;
import io.reactivex.exceptions.*;
import io.reactivex.internal.fuseable.*;
import io.reactivex.internal.queue.SpscArrayQueue;
import io.reactivex.internal.subscriptions.SubscriptionHelper;
import io.reactivex.internal.util.BackpressureHelper;
import io.reactivex.parallel.ParallelFlowable;

/**
 * Dispatches the values from upstream in a round robin fashion to subscribers which are
 * ready to consume elements. A value from upstream is sent to only one of the subscribers.
 *
 * @param <T> the value type
 */
public final class ParallelFromPublisher<T> extends ParallelFlowable<T> {
    final Publisher<? extends T> source;

    final int parallelism;

    final int prefetch;

    public ParallelFromPublisher(Publisher<? extends T> source, int parallelism, int prefetch) {
        this.source = source;
        this.parallelism = parallelism;
        this.prefetch = prefetch;
    }

    @Override
    public int parallelism() {
        return parallelism;
    }

    @Override
    public void subscribe(Subscriber<? super T>[] subscribers) {
        if (!validate(subscribers)) {
            return;
        }

        source.subscribe(new ParallelDispatcher<T>(subscribers, prefetch));
    }

    static final class ParallelDispatcher<T>
    extends AtomicInteger
    implements FlowableSubscriber<T> {


        private static final long serialVersionUID = -4470634016609963609L;

        final Subscriber<? super T>[] subscribers;

        final AtomicLongArray requests;

        final long[] emissions;

        final int prefetch;

        final int limit;

        Subscription s;

        SimpleQueue<T> queue;

        Throwable error;

        volatile boolean done;

        int index;

        volatile boolean cancelled;

        /**
         * Counts how many subscribers were setup to delay triggering the
         * drain of upstream until all of them have been setup.
         */
        final AtomicInteger subscriberCount = new AtomicInteger();

        int produced;

        int sourceMode;

        ParallelDispatcher(Subscriber<? super T>[] subscribers, int prefetch) {
            this.subscribers = subscribers;
            this.prefetch = prefetch;
            this.limit = prefetch - (prefetch >> 2);
            int m = subscribers.length;
            this.requests = new AtomicLongArray(m + m + 1);
            this.requests.lazySet(m + m, m);
            this.emissions = new long[m];
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.validate(this.s, s)) {
                this.s = s;

                if (s instanceof QueueSubscription) {
                    @SuppressWarnings("unchecked")
                    QueueSubscription<T> qs = (QueueSubscription<T>) s;

                    int m = qs.requestFusion(QueueSubscription.ANY | QueueSubscription.BOUNDARY);

                    if (m == QueueSubscription.SYNC) {
                        sourceMode = m;
                        queue = qs;
                        done = true;
                        setupSubscribers();
                        drain();
                        return;
                    } else
                    if (m == QueueSubscription.ASYNC) {
                        sourceMode = m;
                        queue = qs;

                        setupSubscribers();

                        s.request(prefetch);

                        return;
                    }
                }

                queue = new SpscArrayQueue<T>(prefetch);

                setupSubscribers();

                s.request(prefetch);
            }
        }

        void setupSubscribers() {
            Subscriber<? super T>[] subs = subscribers;
            final int m = subs.length;

            for (int i = 0; i < m; i++) {
                if (cancelled) {
                    return;
                }

                subscriberCount.lazySet(i + 1);

                subs[i].onSubscribe(new RailSubscription(i, m));
            }
        }

        final class RailSubscription implements Subscription {

            final int j;

            final int m;

            RailSubscription(int j, int m) {
                this.j = j;
                this.m = m;
            }

            @Override
            public void request(long n) {
                if (SubscriptionHelper.validate(n)) {
                    AtomicLongArray ra = requests;
                    for (;;) {
                        long r = ra.get(j);
                        if (r == Long.MAX_VALUE) {
                            return;
                        }
                        long u = BackpressureHelper.addCap(r, n);
                        if (ra.compareAndSet(j, r, u)) {
                            break;
                        }
                    }
                    if (subscriberCount.get() == m) {
                        drain();
                    }
                }
            }

            @Override
            public void cancel() {
                if (requests.compareAndSet(m + j, 0L, 1L)) {
                    ParallelDispatcher.this.cancel(m + m);
                }
            }
        }

        @Override
        public void onNext(T t) {
            if (sourceMode == QueueSubscription.NONE) {
                if (!queue.offer(t)) {
                    s.cancel();
                    onError(new MissingBackpressureException("Queue is full?"));
                    return;
                }
            }
            drain();
        }

        @Override
        public void onError(Throwable t) {
            error = t;
            done = true;
            drain();
        }

        @Override
        public void onComplete() {
            done = true;
            drain();
        }

        void cancel(int m) {
            if (requests.decrementAndGet(m) == 0L) {
                cancelled = true;
                this.s.cancel();

                if (getAndIncrement() == 0) {
                    queue.clear();
                }
            }
        }

        void drainAsync() {
            int missed = 1;

            SimpleQueue<T> q = queue;
            Subscriber<? super T>[] a = this.subscribers;
            AtomicLongArray r = this.requests;
            long[] e = this.emissions;
            int n = e.length;
            int idx = index;
            int consumed = produced;

            for (;;) {

                int notReady = 0;

                for (;;) {
                    if (cancelled) {
                        q.clear();
                        return;
                    }

                    boolean d = done;
                    if (d) {
                        Throwable ex = error;
                        if (ex != null) {
                            q.clear();
                            for (Subscriber<? super T> s : a) {
                                s.onError(ex);
                            }
                            return;
                        }
                    }

                    boolean empty = q.isEmpty();

                    if (d && empty) {
                        for (Subscriber<? super T> s : a) {
                            s.onComplete();
                        }
                        return;
                    }

                    if (empty) {
                        break;
                    }

                    long requestAtIndex = r.get(idx);
                    long emissionAtIndex = e[idx];
                    if (requestAtIndex != emissionAtIndex && r.get(n + idx) == 0) {

                        T v;

                        try {
                            v = q.poll();
                        } catch (Throwable ex) {
                            Exceptions.throwIfFatal(ex);
                            s.cancel();
                            for (Subscriber<? super T> s : a) {
                                s.onError(ex);
                            }
                            return;
                        }

                        if (v == null) {
                            break;
                        }

                        a[idx].onNext(v);

                        e[idx] = emissionAtIndex + 1;

                        int c = ++consumed;
                        if (c == limit) {
                            consumed = 0;
                            s.request(c);
                        }
                        notReady = 0;
                    } else {
                        notReady++;
                    }

                    idx++;
                    if (idx == n) {
                        idx = 0;
                    }

                    if (notReady == n) {
                        break;
                    }
                }

                int w = get();
                if (w == missed) {
                    index = idx;
                    produced = consumed;
                    missed = addAndGet(-missed);
                    if (missed == 0) {
                        break;
                    }
                } else {
                    missed = w;
                }
            }
        }

        void drainSync() {
            int missed = 1;

            SimpleQueue<T> q = queue;
            Subscriber<? super T>[] a = this.subscribers;
            AtomicLongArray r = this.requests;
            long[] e = this.emissions;
            int n = e.length;
            int idx = index;

            for (;;) {

                int notReady = 0;

                for (;;) {
                    if (cancelled) {
                        q.clear();
                        return;
                    }

                    boolean empty = q.isEmpty();

                    if (empty) {
                        for (Subscriber<? super T> s : a) {
                            s.onComplete();
                        }
                        return;
                    }

                    long requestAtIndex = r.get(idx);
                    long emissionAtIndex = e[idx];
                    if (requestAtIndex != emissionAtIndex && r.get(n + idx) == 0) {

                        T v;

                        try {
                            v = q.poll();
                        } catch (Throwable ex) {
                            Exceptions.throwIfFatal(ex);
                            s.cancel();
                            for (Subscriber<? super T> s : a) {
                                s.onError(ex);
                            }
                            return;
                        }

                        if (v == null) {
                            for (Subscriber<? super T> s : a) {
                                s.onComplete();
                            }
                            return;
                        }

                        a[idx].onNext(v);

                        e[idx] = emissionAtIndex + 1;

                        notReady = 0;
                    } else {
                        notReady++;
                    }

                    idx++;
                    if (idx == n) {
                        idx = 0;
                    }

                    if (notReady == n) {
                        break;
                    }
                }

                int w = get();
                if (w == missed) {
                    index = idx;
                    missed = addAndGet(-missed);
                    if (missed == 0) {
                        break;
                    }
                } else {
                    missed = w;
                }
            }
        }

        void drain() {
            if (getAndIncrement() != 0) {
                return;
            }

            if (sourceMode == QueueSubscription.SYNC) {
                drainSync();
            } else {
                drainAsync();
            }
        }
    }
}
