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

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

import org.reactivestreams.*;

import io.reactivex.*;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.functions.Function;
import io.reactivex.internal.functions.ObjectHelper;
import io.reactivex.internal.fuseable.*;
import io.reactivex.internal.queue.SpscArrayQueue;
import io.reactivex.internal.subscriptions.*;
import io.reactivex.internal.util.*;
import io.reactivex.plugins.RxJavaPlugins;

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
            return new ConcatMapDelayed<T, R>(s, mapper, prefetch, false);
        case END:
            return new ConcatMapDelayed<T, R>(s, mapper, prefetch, true);
        default:
            return new ConcatMapImmediate<T, R>(s, mapper, prefetch);
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

        Subscription s;

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
            this.inner = new ConcatMapInner<R>(this);
            this.errors = new AtomicThrowable();
        }

        @Override
        public final void onSubscribe(Subscription s) {
            if (SubscriptionHelper.validate(this.s, s))  {
                this.s = s;

                if (s instanceof QueueSubscription) {
                    @SuppressWarnings("unchecked") QueueSubscription<T> f = (QueueSubscription<T>)s;
                    int m = f.requestFusion(QueueSubscription.ANY);
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

                queue = new SpscArrayQueue<T>(prefetch);

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
                    s.cancel();
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

        final Subscriber<? super R> actual;

        final AtomicInteger wip;

        ConcatMapImmediate(Subscriber<? super R> actual,
                Function<? super T, ? extends Publisher<? extends R>> mapper,
                int prefetch) {
            super(mapper, prefetch);
            this.actual = actual;
            this.wip = new AtomicInteger();
        }

        @Override
        void subscribeActual() {
            actual.onSubscribe(this);
        }

        @Override
        public void onError(Throwable t) {
            if (errors.addThrowable(t)) {
                inner.cancel();

                if (getAndIncrement() == 0) {
                    actual.onError(errors.terminate());
                }
            } else {
                RxJavaPlugins.onError(t);
            }
        }

        @Override
        public void innerNext(R value) {
            if (get() == 0 && compareAndSet(0, 1)) {
                actual.onNext(value);
                if (compareAndSet(1, 0)) {
                    return;
                }
                actual.onError(errors.terminate());
            }
        }

        @Override
        public void innerError(Throwable e) {
            if (errors.addThrowable(e)) {
                s.cancel();

                if (getAndIncrement() == 0) {
                    actual.onError(errors.terminate());
                }
            } else {
                RxJavaPlugins.onError(e);
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
                s.cancel();
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
                            s.cancel();
                            errors.addThrowable(e);
                            actual.onError(errors.terminate());
                            return;
                        }

                        boolean empty = v == null;

                        if (d && empty) {
                            actual.onComplete();
                            return;
                        }

                        if (!empty) {
                            Publisher<? extends R> p;

                            try {
                                p = ObjectHelper.requireNonNull(mapper.apply(v), "The mapper returned a null Publisher");
                            } catch (Throwable e) {
                                Exceptions.throwIfFatal(e);

                                s.cancel();
                                errors.addThrowable(e);
                                actual.onError(errors.terminate());
                                return;
                            }

                            if (sourceMode != QueueSubscription.SYNC) {
                                int c = consumed + 1;
                                if (c == limit) {
                                    consumed = 0;
                                    s.request(c);
                                } else {
                                    consumed = c;
                                }
                            }


                            if (p instanceof Callable) {
                                @SuppressWarnings("unchecked")
                                Callable<R> callable = (Callable<R>) p;

                                R vr;

                                try {
                                    vr = callable.call();
                                } catch (Throwable e) {
                                    Exceptions.throwIfFatal(e);
                                    s.cancel();
                                    errors.addThrowable(e);
                                    actual.onError(errors.terminate());
                                    return;
                                }


                                if (vr == null) {
                                    continue;
                                }

                                if (inner.isUnbounded()) {
                                    if (get() == 0 && compareAndSet(0, 1)) {
                                        actual.onNext(vr);
                                        if (!compareAndSet(1, 0)) {
                                            actual.onError(errors.terminate());
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
    }

    static final class WeakScalarSubscription<T> implements Subscription {
        final Subscriber<? super T> actual;
        final T value;
        boolean once;

        WeakScalarSubscription(T value, Subscriber<? super T> actual) {
            this.value = value;
            this.actual = actual;
        }

        @Override
        public void request(long n) {
            if (n > 0 && !once) {
                once = true;
                Subscriber<? super T> a = actual;
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

        final Subscriber<? super R> actual;

        final boolean veryEnd;

        ConcatMapDelayed(Subscriber<? super R> actual,
                Function<? super T, ? extends Publisher<? extends R>> mapper,
                int prefetch, boolean veryEnd) {
            super(mapper, prefetch);
            this.actual = actual;
            this.veryEnd = veryEnd;
        }

        @Override
        void subscribeActual() {
            actual.onSubscribe(this);
        }

        @Override
        public void onError(Throwable t) {
            if (errors.addThrowable(t)) {
                done = true;
                drain();
            } else {
                RxJavaPlugins.onError(t);
            }
        }

        @Override
        public void innerNext(R value) {
            actual.onNext(value);
        }


        @Override
        public void innerError(Throwable e) {
            if (errors.addThrowable(e)) {
                if (!veryEnd) {
                    s.cancel();
                    done = true;
                }
                active = false;
                drain();
            } else {
                RxJavaPlugins.onError(e);
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
                s.cancel();
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
                                actual.onError(errors.terminate());
                                return;
                            }
                        }

                        T v;

                        try {
                            v = queue.poll();
                        } catch (Throwable e) {
                            Exceptions.throwIfFatal(e);
                            s.cancel();
                            errors.addThrowable(e);
                            actual.onError(errors.terminate());
                            return;
                        }

                        boolean empty = v == null;

                        if (d && empty) {
                            Throwable ex = errors.terminate();
                            if (ex != null) {
                                actual.onError(ex);
                            } else {
                                actual.onComplete();
                            }
                            return;
                        }

                        if (!empty) {
                            Publisher<? extends R> p;

                            try {
                                p = ObjectHelper.requireNonNull(mapper.apply(v), "The mapper returned a null Publisher");
                            } catch (Throwable e) {
                                Exceptions.throwIfFatal(e);

                                s.cancel();
                                errors.addThrowable(e);
                                actual.onError(errors.terminate());
                                return;
                            }

                            if (sourceMode != QueueSubscription.SYNC) {
                                int c = consumed + 1;
                                if (c == limit) {
                                    consumed = 0;
                                    s.request(c);
                                } else {
                                    consumed = c;
                                }
                            }

                            if (p instanceof Callable) {
                                @SuppressWarnings("unchecked")
                                Callable<R> supplier = (Callable<R>) p;

                                R vr;

                                try {
                                    vr = supplier.call();
                                } catch (Throwable e) {
                                    Exceptions.throwIfFatal(e);
                                    s.cancel();
                                    errors.addThrowable(e);
                                    actual.onError(errors.terminate());
                                    return;
                                }

                                if (vr == null) {
                                    continue;
                                }

                                if (inner.isUnbounded()) {
                                    actual.onNext(vr);
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
