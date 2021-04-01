/*
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

import java.util.*;
import java.util.concurrent.atomic.*;

import org.reactivestreams.*;

import io.reactivex.rxjava3.annotations.Nullable;
import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.exceptions.*;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.internal.fuseable.*;
import io.reactivex.rxjava3.internal.queue.SpscArrayQueue;
import io.reactivex.rxjava3.internal.subscriptions.*;
import io.reactivex.rxjava3.internal.util.*;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;

public final class FlowableFlattenIterable<T, R> extends AbstractFlowableWithUpstream<T, R> {

    final Function<? super T, ? extends Iterable<? extends R>> mapper;

    final int prefetch;

    public FlowableFlattenIterable(Flowable<T> source,
            Function<? super T, ? extends Iterable<? extends R>> mapper, int prefetch) {
        super(source);
        this.mapper = mapper;
        this.prefetch = prefetch;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void subscribeActual(Subscriber<? super R> s) {
        if (source instanceof Supplier) {
            T v;

            try {
                v = ((Supplier<T>)source).get();
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                EmptySubscription.error(ex, s);
                return;
            }

            if (v == null) {
                EmptySubscription.complete(s);
                return;
            }

            Iterator<? extends R> it;

            try {
                Iterable<? extends R> iterable = mapper.apply(v);

                it = iterable.iterator();
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                EmptySubscription.error(ex, s);
                return;
            }

            FlowableFromIterable.subscribe(s, it);

            return;
        }
        source.subscribe(new FlattenIterableSubscriber<>(s, mapper, prefetch));
    }

    /**
     * Create a {@link Subscriber} with the given parameters.
     * @param <T> the upstream value type
     * @param <R> the {@link Iterable} and output value type
     * @param downstream the downstream {@code Subscriber} to wrap
     * @param mapper the mapper function
     * @param prefetch the number of items to prefetch
     * @return the new {@code Subscriber}
     */
    public static <T, R> Subscriber<T> subscribe(Subscriber<? super R> downstream, Function<? super T, ? extends Iterable<? extends R>> mapper, int prefetch) {
        return new FlattenIterableSubscriber<>(downstream, mapper, prefetch);
    }

    static final class FlattenIterableSubscriber<T, R>
    extends BasicIntQueueSubscription<R>
    implements FlowableSubscriber<T> {

        private static final long serialVersionUID = -3096000382929934955L;

        final Subscriber<? super R> downstream;

        final Function<? super T, ? extends Iterable<? extends R>> mapper;

        final int prefetch;

        final int limit;

        final AtomicLong requested;

        Subscription upstream;

        SimpleQueue<T> queue;

        volatile boolean done;

        volatile boolean cancelled;

        final AtomicReference<Throwable> error;

        Iterator<? extends R> current;

        int consumed;

        int fusionMode;

        FlattenIterableSubscriber(Subscriber<? super R> actual,
                Function<? super T, ? extends Iterable<? extends R>> mapper, int prefetch) {
            this.downstream = actual;
            this.mapper = mapper;
            this.prefetch = prefetch;
            this.limit = prefetch - (prefetch >> 2);
            this.error = new AtomicReference<>();
            this.requested = new AtomicLong();
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.validate(this.upstream, s)) {
                this.upstream = s;

                if (s instanceof QueueSubscription) {
                    @SuppressWarnings("unchecked")
                    QueueSubscription<T> qs = (QueueSubscription<T>) s;

                    int m = qs.requestFusion(ANY);

                    if (m == SYNC) {
                        fusionMode = m;
                        this.queue = qs;
                        done = true;

                        downstream.onSubscribe(this);

                        return;
                    }
                    if (m == ASYNC) {
                        fusionMode = m;
                        this.queue = qs;

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
            if (done) {
                return;
            }
            if (fusionMode == NONE && !queue.offer(t)) {
                onError(new MissingBackpressureException("Queue is full?!"));
                return;
            }
            drain();
        }

        @Override
        public void onError(Throwable t) {
            if (!done && ExceptionHelper.addThrowable(error, t)) {
                done = true;
                drain();
            } else {
                RxJavaPlugins.onError(t);
            }
        }

        @Override
        public void onComplete() {
            if (done) {
                return;
            }
            done = true;
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
            if (!cancelled) {
                cancelled = true;

                upstream.cancel();

                if (getAndIncrement() == 0) {
                    queue.clear();
                }
            }
        }

        void drain() {
            if (getAndIncrement() != 0) {
                return;
            }

            final Subscriber<? super R> a = downstream;
            final SimpleQueue<T> q = queue;
            final boolean replenish = fusionMode != SYNC;

            int missed = 1;

            Iterator<? extends R> it = current;

            for (;;) {

                if (it == null) {

                    boolean d = done;

                    T t;

                    try {
                        t = q.poll();
                    } catch (Throwable ex) {
                        Exceptions.throwIfFatal(ex);
                        upstream.cancel();
                        ExceptionHelper.addThrowable(error, ex);
                        ex = ExceptionHelper.terminate(error);

                        current = null;
                        q.clear();

                        a.onError(ex);
                        return;
                    }

                    boolean empty = t == null;

                    if (checkTerminated(d, empty, a, q)) {
                        return;
                    }

                    if (t != null) {
                        Iterable<? extends R> iterable;

                        boolean b;

                        try {
                            iterable = mapper.apply(t);

                            it = iterable.iterator();

                            b = it.hasNext();
                        } catch (Throwable ex) {
                            Exceptions.throwIfFatal(ex);
                            upstream.cancel();
                            ExceptionHelper.addThrowable(error, ex);
                            ex = ExceptionHelper.terminate(error);
                            a.onError(ex);
                            return;
                        }

                        if (!b) {
                            it = null;
                            consumedOne(replenish);
                            continue;
                        }

                        current = it;
                    }
                }

                if (it != null) {
                    long r = requested.get();
                    long e = 0L;

                    while (e != r) {
                        if (checkTerminated(done, false, a, q)) {
                            return;
                        }

                        R v;

                        try {
                            v = Objects.requireNonNull(it.next(), "The iterator returned a null value");
                        } catch (Throwable ex) {
                            Exceptions.throwIfFatal(ex);
                            current = null;
                            upstream.cancel();
                            ExceptionHelper.addThrowable(error, ex);
                            ex = ExceptionHelper.terminate(error);
                            a.onError(ex);
                            return;
                        }

                        a.onNext(v);

                        if (checkTerminated(done, false, a, q)) {
                            return;
                        }

                        e++;

                        boolean b;

                        try {
                            b = it.hasNext();
                        } catch (Throwable ex) {
                            Exceptions.throwIfFatal(ex);
                            current = null;
                            upstream.cancel();
                            ExceptionHelper.addThrowable(error, ex);
                            ex = ExceptionHelper.terminate(error);
                            a.onError(ex);
                            return;
                        }

                        if (!b) {
                            consumedOne(replenish);
                            it = null;
                            current = null;
                            break;
                        }
                    }

                    if (e == r) {
                        boolean d = done;
                        boolean empty = q.isEmpty() && it == null;

                        if (checkTerminated(d, empty, a, q)) {
                            return;
                        }
                    }

                    if (e != 0L) {
                        if (r != Long.MAX_VALUE) {
                            requested.addAndGet(-e);
                        }
                    }

                    if (it == null) {
                        continue;
                    }
                }

                missed = addAndGet(-missed);
                if (missed == 0) {
                    break;
                }
            }
        }

        void consumedOne(boolean enabled) {
            if (enabled) {
                int c = consumed + 1;
                if (c == limit) {
                    consumed = 0;
                    upstream.request(c);
                } else {
                    consumed = c;
                }
            }
        }

        boolean checkTerminated(boolean d, boolean empty, Subscriber<?> a, SimpleQueue<?> q) {
            if (cancelled) {
                current = null;
                q.clear();
                return true;
            }
            if (d) {
                Throwable ex = error.get();
                if (ex != null) {
                    ex = ExceptionHelper.terminate(error);

                    current = null;
                    q.clear();

                    a.onError(ex);
                    return true;
                } else if (empty) {
                    a.onComplete();
                    return true;
                }
            }
            return false;
        }

        @Override
        public void clear() {
            current = null;
            queue.clear();
        }

        @Override
        public boolean isEmpty() {
            return current == null && queue.isEmpty();
        }

        @Nullable
        @Override
        public R poll() throws Throwable {
            Iterator<? extends R> it = current;
            for (;;) {
                if (it == null) {
                    T v = queue.poll();
                    if (v == null) {
                        return null;
                    }

                    it = mapper.apply(v).iterator();

                    if (!it.hasNext()) {
                        it = null;
                        continue;
                    }
                    current = it;
                }

                R r = Objects.requireNonNull(it.next(), "The iterator returned a null value");

                if (!it.hasNext()) {
                    current = null;
                }

                return r;
            }
        }

        @Override
        public int requestFusion(int requestedMode) {
            if ((requestedMode & SYNC) != 0 && fusionMode == SYNC) {
                return SYNC;
            }
            return NONE;
        }
    }
}
