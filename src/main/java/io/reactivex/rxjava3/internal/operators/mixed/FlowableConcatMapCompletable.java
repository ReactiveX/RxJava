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
import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.exceptions.Exceptions;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.internal.disposables.DisposableHelper;
import io.reactivex.rxjava3.internal.fuseable.SimpleQueue;
import io.reactivex.rxjava3.internal.util.*;

/**
 * Maps the upstream items into {@link CompletableSource}s and subscribes to them one after the
 * other completes or terminates (in error-delaying mode).
 * <p>History: 2.1.11 - experimental
 * @param <T> the upstream value type
 * @since 2.2
 */
public final class FlowableConcatMapCompletable<T> extends Completable {

    final Flowable<T> source;

    final Function<? super T, ? extends CompletableSource> mapper;

    final ErrorMode errorMode;

    final int prefetch;

    public FlowableConcatMapCompletable(Flowable<T> source,
            Function<? super T, ? extends CompletableSource> mapper,
            ErrorMode errorMode,
            int prefetch) {
        this.source = source;
        this.mapper = mapper;
        this.errorMode = errorMode;
        this.prefetch = prefetch;
    }

    @Override
    protected void subscribeActual(CompletableObserver observer) {
        source.subscribe(new ConcatMapCompletableObserver<>(observer, mapper, errorMode, prefetch));
    }

    static final class ConcatMapCompletableObserver<T>
    extends ConcatMapXMainSubscriber<T>
    implements Disposable {

        private static final long serialVersionUID = 3610901111000061034L;

        final CompletableObserver downstream;

        final Function<? super T, ? extends CompletableSource> mapper;

        final ConcatMapInnerObserver inner;

        volatile boolean active;

        int consumed;

        ConcatMapCompletableObserver(CompletableObserver downstream,
                Function<? super T, ? extends CompletableSource> mapper,
                ErrorMode errorMode, int prefetch) {
            super(prefetch, errorMode);
            this.downstream = downstream;
            this.mapper = mapper;
            this.inner = new ConcatMapInnerObserver(this);
        }

        @Override
        void onSubscribeDownstream() {
            downstream.onSubscribe(this);
        }

        @Override
        void disposeInner() {
            inner.dispose();
        }

        @Override
        public void dispose() {
            stop();
        }

        @Override
        public boolean isDisposed() {
            return cancelled;
        }

        void innerError(Throwable ex) {
            if (errors.tryAddThrowableOrReport(ex)) {
                if (errorMode == ErrorMode.IMMEDIATE) {
                    upstream.cancel();
                    errors.tryTerminateConsumer(downstream);
                    if (getAndIncrement() == 0) {
                        queue.clear();
                    }
                } else {
                    active = false;
                    drain();
                }
            }
        }

        void innerComplete() {
            active = false;
            drain();
        }

        @Override
        void drain() {
            if (getAndIncrement() != 0) {
                return;
            }

            ErrorMode errorMode = this.errorMode;
            SimpleQueue<T> queue = this.queue;
            AtomicThrowable errors = this.errors;
            boolean syncFused = this.syncFused;

            do {
                if (cancelled) {
                    queue.clear();
                    return;
                }

                if (errors.get() != null) {
                    if (errorMode == ErrorMode.IMMEDIATE
                            || (errorMode == ErrorMode.BOUNDARY && !active)) {
                        queue.clear();
                        errors.tryTerminateConsumer(downstream);
                        return;
                    }
                }

                if (!active) {

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

                    if (!empty) {

                        int limit = prefetch - (prefetch >> 1);

                        if (!syncFused) {
                            int c = consumed + 1;
                            if (c == limit) {
                                consumed = 0;
                                upstream.request(limit);
                            } else {
                                consumed = c;
                            }
                        }

                        CompletableSource cs;

                        try {
                            cs = Objects.requireNonNull(mapper.apply(v), "The mapper returned a null CompletableSource");
                        } catch (Throwable ex) {
                            Exceptions.throwIfFatal(ex);
                            queue.clear();
                            upstream.cancel();
                            errors.tryAddThrowableOrReport(ex);
                            errors.tryTerminateConsumer(downstream);
                            return;
                        }
                        active = true;
                        cs.subscribe(inner);
                    }
                }
            } while (decrementAndGet() != 0);
        }

        static final class ConcatMapInnerObserver extends AtomicReference<Disposable>
        implements CompletableObserver {

            private static final long serialVersionUID = 5638352172918776687L;

            final ConcatMapCompletableObserver<?> parent;

            ConcatMapInnerObserver(ConcatMapCompletableObserver<?> parent) {
                this.parent = parent;
            }

            @Override
            public void onSubscribe(Disposable d) {
                DisposableHelper.replace(this, d);
            }

            @Override
            public void onError(Throwable e) {
                parent.innerError(e);
            }

            @Override
            public void onComplete() {
                parent.innerComplete();
            }

            void dispose() {
                DisposableHelper.dispose(this);
            }
        }
    }
}
