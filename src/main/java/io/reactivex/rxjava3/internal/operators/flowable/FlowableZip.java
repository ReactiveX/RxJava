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

package io.reactivex.rxjava3.internal.operators.flowable;

import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.atomic.*;

import org.reactivestreams.*;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.exceptions.Exceptions;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.internal.fuseable.*;
import io.reactivex.rxjava3.internal.queue.SpscArrayQueue;
import io.reactivex.rxjava3.internal.subscriptions.*;
import io.reactivex.rxjava3.internal.util.*;

public final class FlowableZip<T, R> extends Flowable<R> {

    final Publisher<? extends T>[] sources;
    final Iterable<? extends Publisher<? extends T>> sourcesIterable;
    final Function<? super Object[], ? extends R> zipper;
    final int bufferSize;
    final boolean delayError;

    public FlowableZip(Publisher<? extends T>[] sources,
            Iterable<? extends Publisher<? extends T>> sourcesIterable,
                    Function<? super Object[], ? extends R> zipper,
                    int bufferSize,
                    boolean delayError) {
        this.sources = sources;
        this.sourcesIterable = sourcesIterable;
        this.zipper = zipper;
        this.bufferSize = bufferSize;
        this.delayError = delayError;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void subscribeActual(Subscriber<? super R> s) {
        Publisher<? extends T>[] sources = this.sources;
        int count = 0;
        if (sources == null) {
            sources = new Publisher[8];
            for (Publisher<? extends T> p : sourcesIterable) {
                if (count == sources.length) {
                    Publisher<? extends T>[] b = new Publisher[count + (count >> 2)];
                    System.arraycopy(sources, 0, b, 0, count);
                    sources = b;
                }
                sources[count++] = p;
            }
        } else {
            count = sources.length;
        }

        if (count == 0) {
            EmptySubscription.complete(s);
            return;
        }

        ZipCoordinator<T, R> coordinator = new ZipCoordinator<T, R>(s, zipper, count, bufferSize, delayError);

        s.onSubscribe(coordinator);

        coordinator.subscribe(sources, count);
    }

    static final class ZipCoordinator<T, R>
    extends AtomicInteger
    implements Subscription {

        private static final long serialVersionUID = -2434867452883857743L;

        final Subscriber<? super R> downstream;

        final ZipSubscriber<T, R>[] subscribers;

        final Function<? super Object[], ? extends R> zipper;

        final AtomicLong requested;

        final AtomicThrowable errors;

        final boolean delayErrors;

        volatile boolean cancelled;

        final Object[] current;

        ZipCoordinator(Subscriber<? super R> actual,
                Function<? super Object[], ? extends R> zipper, int n, int prefetch, boolean delayErrors) {
            this.downstream = actual;
            this.zipper = zipper;
            this.delayErrors = delayErrors;
            @SuppressWarnings("unchecked")
            ZipSubscriber<T, R>[] a = new ZipSubscriber[n];
            for (int i = 0; i < n; i++) {
                a[i] = new ZipSubscriber<T, R>(this, prefetch);
            }
            this.current = new Object[n];
            this.subscribers = a;
            this.requested = new AtomicLong();
            this.errors = new AtomicThrowable();
        }

        void subscribe(Publisher<? extends T>[] sources, int n) {
            ZipSubscriber<T, R>[] a = subscribers;
            for (int i = 0; i < n; i++) {
                if (cancelled || (!delayErrors && errors.get() != null)) {
                    return;
                }
                sources[i].subscribe(a[i]);
            }
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
            }
        }

        void error(ZipSubscriber<T, R> inner, Throwable e) {
            if (errors.tryAddThrowableOrReport(e)) {
                inner.done = true;
                drain();
            }
        }

        void cancelAll() {
            for (ZipSubscriber<T, R> s : subscribers) {
                s.cancel();
            }
        }

        void drain() {

            if (getAndIncrement() != 0) {
                return;
            }

            final Subscriber<? super R> a = downstream;
            final ZipSubscriber<T, R>[] qs = subscribers;
            final int n = qs.length;
            Object[] values = current;

            int missed = 1;

            for (;;) {

                long r = requested.get();
                long e = 0L;

                while (r != e) {

                    if (cancelled) {
                        return;
                    }

                    if (!delayErrors && errors.get() != null) {
                        cancelAll();
                        errors.tryTerminateConsumer(a);
                        return;
                    }

                    boolean empty = false;

                    for (int j = 0; j < n; j++) {
                        ZipSubscriber<T, R> inner = qs[j];
                        if (values[j] == null) {
                            try {
                                boolean d = inner.done;
                                SimpleQueue<T> q = inner.queue;

                                T v = q != null ? q.poll() : null;

                                boolean sourceEmpty = v == null;
                                if (d && sourceEmpty) {
                                    cancelAll();
                                    errors.tryTerminateConsumer(a);
                                    return;
                                }
                                if (!sourceEmpty) {
                                    values[j] = v;
                                } else {
                                    empty = true;
                                }
                            } catch (Throwable ex) {
                                Exceptions.throwIfFatal(ex);

                                errors.tryAddThrowableOrReport(ex);
                                if (!delayErrors) {
                                    cancelAll();
                                    errors.tryTerminateConsumer(a);
                                    return;
                                }
                                empty = true;
                            }
                        }
                    }

                    if (empty) {
                        break;
                    }

                    R v;

                    try {
                        v = Objects.requireNonNull(zipper.apply(values.clone()), "The zipper returned a null value");
                    } catch (Throwable ex) {
                        Exceptions.throwIfFatal(ex);
                        cancelAll();
                        errors.tryAddThrowableOrReport(ex);
                        errors.tryTerminateConsumer(a);
                        return;
                    }

                    a.onNext(v);

                    e++;

                    Arrays.fill(values, null);
                }

                if (r == e) {
                    if (cancelled) {
                        return;
                    }

                    if (!delayErrors && errors.get() != null) {
                        cancelAll();
                        errors.tryTerminateConsumer(a);
                        return;
                    }

                    for (int j = 0; j < n; j++) {
                        ZipSubscriber<T, R> inner = qs[j];
                        if (values[j] == null) {
                            try {
                                boolean d = inner.done;
                                SimpleQueue<T> q = inner.queue;
                                T v = q != null ? q.poll() : null;

                                boolean empty = v == null;
                                if (d && empty) {
                                    cancelAll();
                                    errors.tryTerminateConsumer(a);
                                    return;
                                }
                                if (!empty) {
                                    values[j] = v;
                                }
                            } catch (Throwable ex) {
                                Exceptions.throwIfFatal(ex);
                                errors.tryAddThrowableOrReport(ex);
                                if (!delayErrors) {
                                    cancelAll();
                                    errors.tryTerminateConsumer(a);
                                    return;
                                }
                            }
                        }
                    }

                }

                if (e != 0L) {

                    for (ZipSubscriber<T, R> inner : qs) {
                        inner.request(e);
                    }

                    if (r != Long.MAX_VALUE) {
                        requested.addAndGet(-e);
                    }
                }

                missed = addAndGet(-missed);
                if (missed == 0) {
                    break;
                }
            }
        }
    }

    static final class ZipSubscriber<T, R> extends AtomicReference<Subscription> implements FlowableSubscriber<T>, Subscription {

        private static final long serialVersionUID = -4627193790118206028L;

        final ZipCoordinator<T, R> parent;

        final int prefetch;

        final int limit;

        SimpleQueue<T> queue;

        long produced;

        volatile boolean done;

        int sourceMode;

        ZipSubscriber(ZipCoordinator<T, R> parent, int prefetch) {
            this.parent = parent;
            this.prefetch = prefetch;
            this.limit = prefetch - (prefetch >> 2);
        }

        @SuppressWarnings("unchecked")
        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.setOnce(this, s)) {
                if (s instanceof QueueSubscription) {
                    QueueSubscription<T> f = (QueueSubscription<T>) s;

                    int m = f.requestFusion(QueueSubscription.ANY | QueueSubscription.BOUNDARY);

                    if (m == QueueSubscription.SYNC) {
                        sourceMode = m;
                        queue = f;
                        done = true;
                        parent.drain();
                        return;
                    }
                    if (m == QueueSubscription.ASYNC) {
                        sourceMode = m;
                        queue = f;
                        s.request(prefetch);
                        return;
                    }
                }

                queue = new SpscArrayQueue<T>(prefetch);

                s.request(prefetch);
            }
        }

        @Override
        public void onNext(T t) {
            if (sourceMode != QueueSubscription.ASYNC) {
                queue.offer(t);
            }
            parent.drain();
        }

        @Override
        public void onError(Throwable t) {
            parent.error(this, t);
        }

        @Override
        public void onComplete() {
            done = true;
            parent.drain();
        }

        @Override
        public void cancel() {
            SubscriptionHelper.cancel(this);
        }

        @Override
        public void request(long n) {
            if (sourceMode != QueueSubscription.SYNC) {
                long p = produced + n;
                if (p >= limit) {
                    produced = 0L;
                    get().request(p);
                } else {
                    produced = p;
                }
            }
        }
    }
}
