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

package io.reactivex.rxjava3.internal.operators.mixed;

import java.util.Objects;
import java.util.concurrent.atomic.*;

import org.reactivestreams.*;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.exceptions.Exceptions;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.internal.disposables.DisposableHelper;
import io.reactivex.rxjava3.internal.util.*;
import io.reactivex.rxjava3.operators.SimpleQueue;

/**
 * Maps each upstream item into a {@link SingleSource}, subscribes to them one after the other terminates
 * and relays their success values, optionally delaying any errors till the main and inner sources
 * terminate.
 * <p>History: 2.1.11 - experimental
 * @param <T> the upstream element type
 * @param <R> the output element type
 * @since 2.2
 */
public final class FlowableConcatMapSingle<T, R> extends Flowable<R> {

    final Flowable<T> source;

    final Function<? super T, ? extends SingleSource<? extends R>> mapper;

    final ErrorMode errorMode;

    final int prefetch;

    public FlowableConcatMapSingle(Flowable<T> source,
            Function<? super T, ? extends SingleSource<? extends R>> mapper,
                    ErrorMode errorMode, int prefetch) {
        this.source = source;
        this.mapper = mapper;
        this.errorMode = errorMode;
        this.prefetch = prefetch;
    }

    @Override
    protected void subscribeActual(Subscriber<? super R> s) {
        source.subscribe(new ConcatMapSingleSubscriber<>(s, mapper, prefetch, errorMode));
    }

    static final class ConcatMapSingleSubscriber<T, R>
    extends ConcatMapXMainSubscriber<T> implements Subscription {

        private static final long serialVersionUID = -9140123220065488293L;

        final Subscriber<? super R> downstream;

        final Function<? super T, ? extends SingleSource<? extends R>> mapper;

        final AtomicLong requested;

        final ConcatMapSingleObserver<R> inner;

        long emitted;

        int consumed;

        R item;

        volatile int state;

        /** No inner SingleSource is running. */
        static final int STATE_INACTIVE = 0;
        /** An inner SingleSource is running but there are no results yet. */
        static final int STATE_ACTIVE = 1;
        /** The inner SingleSource succeeded with a value in {@link #item}. */
        static final int STATE_RESULT_VALUE = 2;

        ConcatMapSingleSubscriber(Subscriber<? super R> downstream,
                Function<? super T, ? extends SingleSource<? extends R>> mapper,
                        int prefetch, ErrorMode errorMode) {
            super(prefetch, errorMode);
            this.downstream = downstream;
            this.mapper = mapper;
            this.requested = new AtomicLong();
            this.inner = new ConcatMapSingleObserver<>(this);
        }

        @Override
        void onSubscribeDownstream() {
            downstream.onSubscribe(this);
        }

        @Override
        public void request(long n) {
            BackpressureHelper.add(requested, n);
            drain();
        }

        @Override
        public void cancel() {
            stop();
        }

        @Override
        void clearValue() {
            item = null;
        }

        @Override
        void disposeInner() {
            inner.dispose();
        }

        void innerSuccess(R item) {
            this.item = item;
            this.state = STATE_RESULT_VALUE;
            drain();
        }

        void innerError(Throwable ex) {
            if (errors.tryAddThrowableOrReport(ex)) {
                if (errorMode != ErrorMode.END) {
                    upstream.cancel();
                }
                this.state = STATE_INACTIVE;
                drain();
            }
        }

        @Override
        void drain() {
            if (getAndIncrement() != 0) {
                return;
            }

            int missed = 1;
            Subscriber<? super R> downstream = this.downstream;
            ErrorMode errorMode = this.errorMode;
            SimpleQueue<T> queue = this.queue;
            AtomicThrowable errors = this.errors;
            AtomicLong requested = this.requested;
            int limit = prefetch - (prefetch >> 1);
            boolean syncFused = this.syncFused;

            for (;;) {

                for (;;) {
                    if (cancelled) {
                        queue.clear();
                        item = null;
                        break;
                    }

                    int s = state;

                    if (errors.get() != null) {
                        if (errorMode == ErrorMode.IMMEDIATE
                                || (errorMode == ErrorMode.BOUNDARY && s == STATE_INACTIVE)) {
                            queue.clear();
                            item = null;
                            errors.tryTerminateConsumer(downstream);
                            return;
                        }
                    }

                    if (s == STATE_INACTIVE) {
                        boolean d = done;
                        T v;
                        try {
                            v = queue.poll();
                        } catch (Throwable ex) {
                            Exceptions.throwIfFatal(ex);
                            upstream.cancel();
                            errors.tryAddThrowableOrReport(ex);
                            errors.tryTerminateConsumer(downstream);
                            return;
                        }
                        boolean empty = v == null;

                        if (d && empty) {
                            errors.tryTerminateConsumer(downstream);
                            return;
                        }

                        if (empty) {
                            break;
                        }

                        if (!syncFused) {
                            int c = consumed + 1;
                            if (c == limit) {
                                consumed = 0;
                                upstream.request(limit);
                            } else {
                                consumed = c;
                            }
                        }

                        SingleSource<? extends R> ss;

                        try {
                            ss = Objects.requireNonNull(mapper.apply(v), "The mapper returned a null SingleSource");
                        } catch (Throwable ex) {
                            Exceptions.throwIfFatal(ex);
                            upstream.cancel();
                            queue.clear();
                            errors.tryAddThrowableOrReport(ex);
                            errors.tryTerminateConsumer(downstream);
                            return;
                        }

                        state = STATE_ACTIVE;
                        ss.subscribe(inner);
                        break;
                    } else if (s == STATE_RESULT_VALUE) {
                        long e = emitted;
                        if (e != requested.get()) {
                            R w = item;
                            item = null;

                            downstream.onNext(w);

                            emitted = e + 1;
                            state = STATE_INACTIVE;
                        } else {
                            break;
                        }
                    } else {
                        break;
                    }
                }

                missed = addAndGet(-missed);
                if (missed == 0) {
                    break;
                }
            }
        }

        static final class ConcatMapSingleObserver<R>
        extends AtomicReference<Disposable>
        implements SingleObserver<R> {

            private static final long serialVersionUID = -3051469169682093892L;

            final ConcatMapSingleSubscriber<?, R> parent;

            ConcatMapSingleObserver(ConcatMapSingleSubscriber<?, R> parent) {
                this.parent = parent;
            }

            @Override
            public void onSubscribe(Disposable d) {
                DisposableHelper.replace(this, d);
            }

            @Override
            public void onSuccess(R t) {
                parent.innerSuccess(t);
            }

            @Override
            public void onError(Throwable e) {
                parent.innerError(e);
            }

            void dispose() {
                DisposableHelper.dispose(this);
            }
        }
    }
}
