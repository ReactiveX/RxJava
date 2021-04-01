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

import java.util.Objects;
import java.util.concurrent.atomic.*;

import org.reactivestreams.*;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.exceptions.Exceptions;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.internal.fuseable.*;
import io.reactivex.rxjava3.internal.queue.SpscArrayQueue;
import io.reactivex.rxjava3.internal.subscriptions.*;
import io.reactivex.rxjava3.internal.util.*;

public final class FlowableConcatMap<T, R> extends AbstractFlowableWithUpstream<T, R> {

    final Function<? super T, ? extends Publisher<? extends R>> mapper;

    final int prefetch;

    final ErrorMode errorMode;

    public FlowableConcatMap(Flowable<T> source,
            Function<? super T, ? extends Publisher<? extends R>> mapper,
            int prefetch, ErrorMode errorMode) {
        super(source);
        this.mapper = mapper;
        this.prefetch = prefetch;
        this.errorMode = errorMode;
    }

    public static <T, R> Subscriber<T> subscribe(Subscriber<? super R> s, Function<? super T, ? extends Publisher<? extends R>> mapper,
            int prefetch, ErrorMode errorMode) {
        switch (errorMode) {
        case BOUNDARY:
            return new ConcatMapDelayed<>(s, mapper, prefetch, false);
        case END:
            return new ConcatMapDelayed<>(s, mapper, prefetch, true);
        default:
            return new ConcatMapImmediate<>(s, mapper, prefetch);
        }
    }

    @Override
    protected void subscribeActual(Subscriber<? super R> s) {

        if (FlowableScalarXMap.tryScalarXMapSubscribe(source, s, mapper)) {
            return;
        }

        source.subscribe(subscribe(s, mapper, prefetch, errorMode));
    }

    abstract static class BaseConcatMapSubscriber<T, R>
    extends AtomicInteger
    implements FlowableSubscriber<T>, ConcatMapSupport<R>, Subscription {

        private static final long serialVersionUID = -3511336836796789179L;

        final ConcatMapInner<R> inner;

        final Function<? super T, ? extends Publisher<? extends R>> mapper;

        final int prefetch;

        final int limit;

        Subscription upstream;

        int consumed;

        SimpleQueue<T> queue;

        volatile boolean done;

        volatile boolean cancelled;

        final AtomicThrowable errors;

        volatile boolean active;

        int sourceMode;

        BaseConcatMapSubscriber(
                Function<? super T, ? extends Publisher<? extends R>> mapper,
                int prefetch) {
            this.mapper = mapper;
            this.prefetch = prefetch;
            this.limit = prefetch - (prefetch >> 2);
            this.inner = new ConcatMapInner<>(this);
            this.errors = new AtomicThrowable();
        }

        @Override
        public final void onSubscribe(Subscription s) {
            if (SubscriptionHelper.validate(this.upstream, s))  {
                this.upstream = s;

                if (s instanceof QueueSubscription) {
                    @SuppressWarnings("unchecked") QueueSubscription<T> f = (QueueSubscription<T>)s;
                    int m = f.requestFusion(QueueSubscription.ANY | QueueSubscription.BOUNDARY);
                    if (m == QueueSubscription.SYNC) {
                        sourceMode = m;
                        queue = f;
                        done = true;

                        subscribeActual();

                        drain();
                        return;
                    }
                    if (m == QueueSubscription.ASYNC) {
                        sourceMode = m;
                        queue = f;

                        subscribeActual();

                        s.request(prefetch);
                        return;
                    }
                }

                queue = new SpscArrayQueue<>(prefetch);

                subscribeActual();

                s.request(prefetch);
            }
        }

        abstract void drain();

        abstract void subscribeActual();

        @Override
        public final void onNext(T t) {
            if (sourceMode != QueueSubscription.ASYNC) {
                if (!queue.offer(t)) {
                    upstream.cancel();
                    onError(new IllegalStateException("Queue full?!"));
                    return;
                }
            }
            drain();
        }

        @Override
        public final void onComplete() {
            done = true;
            drain();
        }

        @Override
        public final void innerComplete() {
            active = false;
            drain();
        }

    }

    static final class ConcatMapImmediate<T, R>
    extends BaseConcatMapSubscriber<T, R> {

        private static final long serialVersionUID = 7898995095634264146L;

        final Subscriber<? super R> downstream;

        final AtomicInteger wip;

        ConcatMapImmediate(Subscriber<? super R> actual,
                Function<? super T, ? extends Publisher<? extends R>> mapper,
                int prefetch) {
            super(mapper, prefetch);
            this.downstream = actual;
            this.wip = new AtomicInteger();
        }

        @Override
        void subscribeActual() {
            downstream.onSubscribe(this);
        }

        @Override
        public void onError(Throwable t) {
            inner.cancel();
            HalfSerializer.onError(downstream, t, this, errors);
        }

        @Override
        public void innerNext(R value) {
            HalfSerializer.onNext(downstream, value, this, errors);
        }

        @Override
        public void innerError(Throwable e) {
            upstream.cancel();
            HalfSerializer.onError(downstream, e, this, errors);
        }

        @Override
        public void request(long n) {
            inner.request(n);
        }

        @Override
        public void cancel() {
            if (!cancelled) {
                cancelled = true;

                inner.cancel();
                upstream.cancel();

                errors.tryTerminateAndReport();
            }
        }

        @Override
        void drain() {
            if (wip.getAndIncrement() == 0) {
                for (;;) {
                    if (cancelled) {
                        return;
                    }

                    if (!active) {
                        boolean d = done;

                        T v;

                        try {
                            v = queue.poll();
                        } catch (Throwable e) {
                            Exceptions.throwIfFatal(e);
                            upstream.cancel();
                            errors.tryAddThrowableOrReport(e);
                            errors.tryTerminateConsumer(downstream);
                            return;
                        }

                        boolean empty = v == null;

                        if (d && empty) {
                            downstream.onComplete();
                            return;
                        }

                        if (!empty) {
                            Publisher<? extends R> p;

                            try {
                                p = Objects.requireNonNull(mapper.apply(v), "The mapper returned a null Publisher");
                            } catch (Throwable e) {
                                Exceptions.throwIfFatal(e);

                                upstream.cancel();
                                errors.tryAddThrowableOrReport(e);
                                errors.tryTerminateConsumer(downstream);
                                return;
                            }

                            if (sourceMode != QueueSubscription.SYNC) {
                                int c = consumed + 1;
                                if (c == limit) {
                                    consumed = 0;
                                    upstream.request(c);
                                } else {
                                    consumed = c;
                                }
                            }

                            if (p instanceof Supplier) {
                                @SuppressWarnings("unchecked")
                                Supplier<R> supplier = (Supplier<R>) p;

                                R vr;

                                try {
                                    vr = supplier.get();
                                } catch (Throwable e) {
                                    Exceptions.throwIfFatal(e);
                                    upstream.cancel();
                                    errors.tryAddThrowableOrReport(e);
                                    errors.tryTerminateConsumer(downstream);
                                    return;
                                }

                                if (vr == null) {
                                    continue;
                                }

                                if (inner.isUnbounded()) {
                                    if (!HalfSerializer.onNext(downstream, vr, this, errors)) {
                                        return;
                                    }
                                    continue;
                                } else {
                                    active = true;
                                    inner.setSubscription(new SimpleScalarSubscription<>(vr, inner));
                                }

                            } else {
                                active = true;
                                p.subscribe(inner);
                            }
                        }
                    }
                    if (wip.decrementAndGet() == 0) {
                        break;
                    }
                }
            }
        }
    }

    static final class SimpleScalarSubscription<T>
    extends AtomicBoolean
    implements Subscription {
        private static final long serialVersionUID = -7606889335172043256L;

        final Subscriber<? super T> downstream;
        final T value;

        SimpleScalarSubscription(T value, Subscriber<? super T> downstream) {
            this.value = value;
            this.downstream = downstream;
        }

        @Override
        public void request(long n) {
            if (n > 0L && compareAndSet(false, true)) {
                Subscriber<? super T> a = downstream;
                a.onNext(value);
                a.onComplete();
            }
        }

        @Override
        public void cancel() {

        }
    }

    static final class ConcatMapDelayed<T, R>
    extends BaseConcatMapSubscriber<T, R> {

        private static final long serialVersionUID = -2945777694260521066L;

        final Subscriber<? super R> downstream;

        final boolean veryEnd;

        ConcatMapDelayed(Subscriber<? super R> actual,
                Function<? super T, ? extends Publisher<? extends R>> mapper,
                int prefetch, boolean veryEnd) {
            super(mapper, prefetch);
            this.downstream = actual;
            this.veryEnd = veryEnd;
        }

        @Override
        void subscribeActual() {
            downstream.onSubscribe(this);
        }

        @Override
        public void onError(Throwable t) {
            if (errors.tryAddThrowableOrReport(t)) {
                done = true;
                drain();
            }
        }

        @Override
        public void innerNext(R value) {
            downstream.onNext(value);
        }

        @Override
        public void innerError(Throwable e) {
            if (errors.tryAddThrowableOrReport(e)) {
                if (!veryEnd) {
                    upstream.cancel();
                    done = true;
                }
                active = false;
                drain();
            }
        }

        @Override
        public void request(long n) {
            inner.request(n);
        }

        @Override
        public void cancel() {
            if (!cancelled) {
                cancelled = true;

                inner.cancel();
                upstream.cancel();

                errors.tryTerminateAndReport();
            }
        }

        @Override
        void drain() {
            if (getAndIncrement() == 0) {

                for (;;) {
                    if (cancelled) {
                        return;
                    }

                    if (!active) {

                        boolean d = done;

                        if (d && !veryEnd) {
                            Throwable ex = errors.get();
                            if (ex != null) {
                                errors.tryTerminateConsumer(downstream);
                                return;
                            }
                        }

                        T v;

                        try {
                            v = queue.poll();
                        } catch (Throwable e) {
                            Exceptions.throwIfFatal(e);
                            upstream.cancel();
                            errors.tryAddThrowableOrReport(e);
                            errors.tryTerminateConsumer(downstream);
                            return;
                        }

                        boolean empty = v == null;

                        if (d && empty) {
                            errors.tryTerminateConsumer(downstream);
                            return;
                        }

                        if (!empty) {
                            Publisher<? extends R> p;

                            try {
                                p = Objects.requireNonNull(mapper.apply(v), "The mapper returned a null Publisher");
                            } catch (Throwable e) {
                                Exceptions.throwIfFatal(e);

                                upstream.cancel();
                                errors.tryAddThrowableOrReport(e);
                                errors.tryTerminateConsumer(downstream);
                                return;
                            }

                            if (sourceMode != QueueSubscription.SYNC) {
                                int c = consumed + 1;
                                if (c == limit) {
                                    consumed = 0;
                                    upstream.request(c);
                                } else {
                                    consumed = c;
                                }
                            }

                            if (p instanceof Supplier) {
                                @SuppressWarnings("unchecked")
                                Supplier<R> supplier = (Supplier<R>) p;

                                R vr;

                                try {
                                    vr = supplier.get();
                                } catch (Throwable e) {
                                    Exceptions.throwIfFatal(e);
                                    errors.tryAddThrowableOrReport(e);
                                    if (!veryEnd) {
                                        upstream.cancel();
                                        errors.tryTerminateConsumer(downstream);
                                        return;
                                    }
                                    vr = null;
                                }

                                if (vr == null) {
                                    continue;
                                }

                                if (inner.isUnbounded()) {
                                    downstream.onNext(vr);
                                    continue;
                                } else {
                                    active = true;
                                    inner.setSubscription(new SimpleScalarSubscription<>(vr, inner));
                                }
                            } else {
                                active = true;
                                p.subscribe(inner);
                            }
                        }
                    }
                    if (decrementAndGet() == 0) {
                        break;
                    }
                }
            }
        }
    }

    interface ConcatMapSupport<T> {

        void innerNext(T value);

        void innerComplete();

        void innerError(Throwable e);
    }

    static final class ConcatMapInner<R>
    extends SubscriptionArbiter
    implements FlowableSubscriber<R> {

        private static final long serialVersionUID = 897683679971470653L;

        final ConcatMapSupport<R> parent;

        long produced;

        ConcatMapInner(ConcatMapSupport<R> parent) {
            super(false);
            this.parent = parent;
        }

        @Override
        public void onSubscribe(Subscription s) {
            setSubscription(s);
        }

        @Override
        public void onNext(R t) {
            produced++;

            parent.innerNext(t);
        }

        @Override
        public void onError(Throwable t) {
            long p = produced;

            if (p != 0L) {
                produced = 0L;
                produced(p);
            }

            parent.innerError(t);
        }

        @Override
        public void onComplete() {
            long p = produced;

            if (p != 0L) {
                produced = 0L;
                produced(p);
            }

            parent.innerComplete();
        }
    }
}
