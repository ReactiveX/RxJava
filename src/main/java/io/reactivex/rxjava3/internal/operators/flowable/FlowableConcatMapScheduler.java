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

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import org.reactivestreams.*;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.exceptions.Exceptions;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.internal.fuseable.*;
import io.reactivex.rxjava3.internal.operators.flowable.FlowableConcatMap.*;
import io.reactivex.rxjava3.internal.queue.SpscArrayQueue;
import io.reactivex.rxjava3.internal.subscriptions.SubscriptionHelper;
import io.reactivex.rxjava3.internal.util.*;

public final class FlowableConcatMapScheduler<T, R> extends AbstractFlowableWithUpstream<T, R> {

    final Function<? super T, ? extends Publisher<? extends R>> mapper;

    final int prefetch;

    final ErrorMode errorMode;

    final Scheduler scheduler;

    public FlowableConcatMapScheduler(Flowable<T> source,
            Function<? super T, ? extends Publisher<? extends R>> mapper,
            int prefetch, ErrorMode errorMode, Scheduler scheduler) {
        super(source);
        this.mapper = mapper;
        this.prefetch = prefetch;
        this.errorMode = errorMode;
        this.scheduler = scheduler;
    }

    @Override
    protected void subscribeActual(Subscriber<? super R> s) {
        switch (errorMode) {
        case BOUNDARY:
            source.subscribe(new ConcatMapDelayed<T, R>(s, mapper, prefetch, false, scheduler.createWorker()));
            break;
        case END:
            source.subscribe(new ConcatMapDelayed<T, R>(s, mapper, prefetch, true, scheduler.createWorker()));
            break;
        default:
            source.subscribe(new ConcatMapImmediate<T, R>(s, mapper, prefetch, scheduler.createWorker()));
        }
    }

    abstract static class BaseConcatMapSubscriber<T, R>
    extends AtomicInteger
    implements FlowableSubscriber<T>, ConcatMapSupport<R>, Subscription, Runnable {

        private static final long serialVersionUID = -3511336836796789179L;

        final ConcatMapInner<R> inner;

        final Function<? super T, ? extends Publisher<? extends R>> mapper;

        final int prefetch;

        final int limit;

        final Scheduler.Worker worker;

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
                int prefetch, Scheduler.Worker worker) {
            this.mapper = mapper;
            this.prefetch = prefetch;
            this.limit = prefetch - (prefetch >> 2);
            this.inner = new ConcatMapInner<R>(this);
            this.errors = new AtomicThrowable();
            this.worker = worker;
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

                        schedule();
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

                queue = new SpscArrayQueue<T>(prefetch);

                subscribeActual();

                s.request(prefetch);
            }
        }

        abstract void schedule();

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
            schedule();
        }

        @Override
        public final void onComplete() {
            done = true;
            schedule();
        }

        @Override
        public final void innerComplete() {
            active = false;
            schedule();
        }

    }

    static final class ConcatMapImmediate<T, R>
    extends BaseConcatMapSubscriber<T, R> {

        private static final long serialVersionUID = 7898995095634264146L;

        final Subscriber<? super R> downstream;

        final AtomicInteger wip;

        ConcatMapImmediate(Subscriber<? super R> actual,
                Function<? super T, ? extends Publisher<? extends R>> mapper,
                int prefetch, Scheduler.Worker worker) {
            super(mapper, prefetch, worker);
            this.downstream = actual;
            this.wip = new AtomicInteger();
        }

        @Override
        void subscribeActual() {
            downstream.onSubscribe(this);
        }

        @Override
        public void onError(Throwable t) {
            if (errors.tryAddThrowableOrReport(t)) {
                inner.cancel();

                if (getAndIncrement() == 0) {
                    errors.tryTerminateConsumer(downstream);
                    worker.dispose();
                }
            }
        }

        @Override
        public void innerNext(R value) {
            if (get() == 0 && compareAndSet(0, 1)) {
                downstream.onNext(value);
                if (compareAndSet(1, 0)) {
                    return;
                }
                errors.tryTerminateConsumer(downstream);
                worker.dispose();
            }
        }

        @Override
        public void innerError(Throwable e) {
            if (errors.tryAddThrowableOrReport(e)) {
                upstream.cancel();

                if (getAndIncrement() == 0) {
                    errors.tryTerminateConsumer(downstream);
                    worker.dispose();
                }
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
                worker.dispose();
                errors.tryTerminateAndReport();
            }
        }

        @Override
        void schedule() {
            if (wip.getAndIncrement() == 0) {
                worker.schedule(this);
            }
        }

        @Override
        public void run() {
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
                        worker.dispose();
                        return;
                    }

                    boolean empty = v == null;

                    if (d && empty) {
                        downstream.onComplete();
                        worker.dispose();
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
                            worker.dispose();
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
                                worker.dispose();
                                return;
                            }

                            if (vr == null) {
                                continue;
                            }

                            if (inner.isUnbounded()) {
                                if (get() == 0 && compareAndSet(0, 1)) {
                                    downstream.onNext(vr);
                                    if (!compareAndSet(1, 0)) {
                                        errors.tryTerminateConsumer(downstream);
                                        worker.dispose();
                                        return;
                                    }
                                }
                                continue;
                            } else {
                                active = true;
                                inner.setSubscription(new WeakScalarSubscription<R>(vr, inner));
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

    static final class ConcatMapDelayed<T, R>
    extends BaseConcatMapSubscriber<T, R> {

        private static final long serialVersionUID = -2945777694260521066L;

        final Subscriber<? super R> downstream;

        final boolean veryEnd;

        ConcatMapDelayed(Subscriber<? super R> actual,
                Function<? super T, ? extends Publisher<? extends R>> mapper,
                int prefetch, boolean veryEnd, Scheduler.Worker worker) {
            super(mapper, prefetch, worker);
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
                schedule();
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
                schedule();
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
                worker.dispose();
                errors.tryTerminateAndReport();
            }
        }

        @Override
        void schedule() {
            if (getAndIncrement() == 0) {
                worker.schedule(this);
            }
        }

        @Override
        public void run() {

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
                            worker.dispose();
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
                        worker.dispose();
                        return;
                    }

                    boolean empty = v == null;

                    if (d && empty) {
                        errors.tryTerminateConsumer(downstream);
                        worker.dispose();
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
                            worker.dispose();
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
                                    worker.dispose();
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
                                inner.setSubscription(new WeakScalarSubscription<R>(vr, inner));
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
