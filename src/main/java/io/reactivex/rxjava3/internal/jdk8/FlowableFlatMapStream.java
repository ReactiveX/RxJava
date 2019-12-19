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

package io.reactivex.rxjava3.internal.jdk8;

import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.stream.Stream;

import org.reactivestreams.*;

import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.exceptions.MissingBackpressureException;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.internal.fuseable.*;
import io.reactivex.rxjava3.internal.queue.SpscArrayQueue;
import io.reactivex.rxjava3.internal.subscriptions.*;
import io.reactivex.rxjava3.internal.util.*;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;

/**
 * Maps the upstream values onto {@link Stream}s and emits their items in order to the downstream.
 *
 * @param <T> the upstream element type
 * @param <R> the inner {@code Stream} and result element type
 * @since 3.0.0
 */
public final class FlowableFlatMapStream<T, R> extends Flowable<R> {

    final Flowable<T> source;

    final Function<? super T, ? extends Stream<? extends R>> mapper;

    final int prefetch;

    public FlowableFlatMapStream(Flowable<T> source, Function<? super T, ? extends Stream<? extends R>> mapper, int prefetch) {
        this.source = source;
        this.mapper = mapper;
        this.prefetch = prefetch;
    }

    @Override
    protected void subscribeActual(Subscriber<? super R> s) {
        if (source instanceof Supplier) {
            Stream<? extends R> stream = null;
            try {
                @SuppressWarnings("unchecked")
                T t = ((Supplier<T>)source).get();
                if (t != null) {
                    stream = Objects.requireNonNull(mapper.apply(t), "The mapper returned a null Stream");
                }
            } catch (Throwable ex) {
                EmptySubscription.error(ex, s);
                return;
            }

            if (stream != null) {
                FlowableFromStream.subscribeStream(s, stream);
            } else {
                EmptySubscription.complete(s);
            }
        } else {
            source.subscribe(new FlatMapStreamSubscriber<>(s, mapper, prefetch));
        }
    }

    static final class FlatMapStreamSubscriber<T, R> extends AtomicInteger
    implements FlowableSubscriber<T>, Subscription {

        private static final long serialVersionUID = -5127032662980523968L;

        final Subscriber<? super R> downstream;

        final Function<? super T, ? extends Stream<? extends R>> mapper;

        final int prefetch;

        final AtomicLong requested;

        SimpleQueue<T> queue;

        Subscription upstream;

        Iterator<? extends R> currentIterator;

        AutoCloseable currentCloseable;

        volatile boolean cancelled;

        volatile boolean upstreamDone;
        final AtomicThrowable error;

        long emitted;

        int consumed;

        int sourceMode;

        FlatMapStreamSubscriber(Subscriber<? super R> downstream, Function<? super T, ? extends Stream<? extends R>> mapper, int prefetch) {
            this.downstream = downstream;
            this.mapper = mapper;
            this.prefetch = prefetch;
            this.requested = new AtomicLong();
            this.error = new AtomicThrowable();
        }

        @Override
        public void onSubscribe(@NonNull Subscription s) {
            if (SubscriptionHelper.validate(this.upstream, s)) {
                this.upstream = s;

                if (s instanceof QueueSubscription) {

                    @SuppressWarnings("unchecked")
                    QueueSubscription<T> qs = (QueueSubscription<T>)s;

                    int m = qs.requestFusion(QueueFuseable.ANY | QueueFuseable.BOUNDARY);
                    if (m == QueueFuseable.SYNC) {
                        sourceMode = m;
                        queue = qs;
                        upstreamDone = true;

                        downstream.onSubscribe(this);
                        return;
                    }
                    else if (m == QueueFuseable.ASYNC) {
                        sourceMode = m;
                        queue = qs;

                        downstream.onSubscribe(this);

                        s.request(prefetch);
                        return;
                    }
                }

                queue = new SpscArrayQueue<>(prefetch);

                downstream.onSubscribe(this);

                s.request(prefetch);
            }
        }

        @Override
        public void onNext(T t) {
            if (sourceMode != QueueFuseable.ASYNC) {
                if (!queue.offer(t)) {
                    upstream.cancel();
                    onError(new MissingBackpressureException("Queue full?!"));
                    return;
                }
            }
            drain();
        }

        @Override
        public void onError(Throwable t) {
            if (error.compareAndSet(null, t)) {
                upstreamDone = true;
                drain();
            } else {
                RxJavaPlugins.onError(t);
            }
        }

        @Override
        public void onComplete() {
            upstreamDone = true;
            drain();
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
            cancelled = true;
            upstream.cancel();
            drain();
        }

        void drain() {
            if (getAndIncrement() != 0) {
                return;
            }

            int missed = 1;

            final Subscriber<? super R> downstream = this.downstream;
            final SimpleQueue<T> queue = this.queue;
            final AtomicThrowable error = this.error;
            Iterator<? extends R> iterator = this.currentIterator;
            long requested = this.requested.get();
            long emitted = this.emitted;
            final int limit = prefetch - (prefetch >> 2);
            boolean canRequest = sourceMode != QueueFuseable.SYNC;

            for (;;) {
                if (cancelled) {
                    queue.clear();
                    clearCurrentSuppressCloseError();
                } else {
                    boolean isDone = upstreamDone;
                    if (error.get() != null) {
                        downstream.onError(error.get());
                        cancelled = true;
                        continue;
                    }

                    if (iterator == null) {
                        T t;

                        try {
                            t = queue.poll();
                        } catch (Throwable ex) {
                            trySignalError(downstream, ex);
                            continue;
                        }

                        boolean isEmpty = t == null;

                        if (isDone && isEmpty) {
                            downstream.onComplete();
                            cancelled = true;
                        }
                        else if (!isEmpty) {
                            if (canRequest && ++consumed == limit) {
                                consumed = 0;
                                upstream.request(limit);
                            }

                            Stream<? extends R> stream;
                            try {
                                stream = Objects.requireNonNull(mapper.apply(t), "The mapper returned a null Stream");
                                iterator = stream.iterator();

                                if (iterator.hasNext()) {
                                    currentIterator = iterator;
                                    currentCloseable = stream;
                                } else {
                                    iterator = null;
                                }
                            } catch (Throwable ex) {
                                trySignalError(downstream, ex);
                            }
                            continue;
                        }
                    }
                    if (iterator != null && emitted != requested) {
                        R item;

                        try {
                            item = Objects.requireNonNull(iterator.next(), "The Stream.Iterator returned a null value");
                        } catch (Throwable ex) {
                            trySignalError(downstream, ex);
                            continue;
                        }

                        if (!cancelled) {
                            downstream.onNext(item);
                            emitted++;

                            if (!cancelled) {
                                try {
                                    if (!iterator.hasNext()) {
                                        iterator = null;
                                        clearCurrentRethrowCloseError();
                                    }
                                } catch (Throwable ex) {
                                    trySignalError(downstream, ex);
                                }
                            }
                        }

                        continue;
                    }
                }

                this.emitted = emitted;
                missed = addAndGet(-missed);
                if (missed == 0) {
                    break;
                }
                requested = this.requested.get();
            }
        }

        void clearCurrentRethrowCloseError() throws Throwable {
            currentIterator = null;
            AutoCloseable ac = currentCloseable;
            currentCloseable = null;
            if (ac != null) {
                ac.close();
            }
        }

        void clearCurrentSuppressCloseError() {
            try {
                clearCurrentRethrowCloseError();
            } catch (Throwable ex) {
                RxJavaPlugins.onError(ex);
            }
        }

        void trySignalError(Subscriber<?> downstream, Throwable ex) {
            if (error.compareAndSet(null, ex)) {
                upstream.cancel();
                cancelled = true;
                downstream.onError(ex);
            } else {
                RxJavaPlugins.onError(ex);
            }
        }
    }
}
