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

package io.reactivex.internal.operators.mixed;

import java.util.concurrent.atomic.*;

import io.reactivex.*;
import io.reactivex.annotations.Experimental;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.*;
import io.reactivex.functions.Function;
import io.reactivex.internal.disposables.DisposableHelper;
import io.reactivex.internal.functions.ObjectHelper;
import io.reactivex.internal.fuseable.SimplePlainQueue;
import io.reactivex.internal.queue.SpscArrayQueue;
import io.reactivex.internal.util.*;
import io.reactivex.plugins.RxJavaPlugins;

/**
 * Maps each upstream item into a {@link SingleSource}, subscribes to them one after the other terminates
 * and relays their success values, optionally delaying any errors till the main and inner sources
 * terminate.
 *
 * @param <T> the upstream element type
 * @param <R> the output element type
 *
 * @since 2.1.11 - experimental
 */
@Experimental
public final class ObservableConcatMapSingle<T, R> extends Observable<R> {

    final Observable<T> source;

    final Function<? super T, ? extends SingleSource<? extends R>> mapper;

    final ErrorMode errorMode;

    final int prefetch;

    public ObservableConcatMapSingle(Observable<T> source,
            Function<? super T, ? extends SingleSource<? extends R>> mapper,
                    ErrorMode errorMode, int prefetch) {
        this.source = source;
        this.mapper = mapper;
        this.errorMode = errorMode;
        this.prefetch = prefetch;
    }

    @Override
    protected void subscribeActual(Observer<? super R> s) {
        if (!ScalarXMapZHelper.tryAsSingle(source, mapper, s)) {
            source.subscribe(new ConcatMapSingleMainObserver<T, R>(s, mapper, prefetch, errorMode));
        }
    }

    static final class ConcatMapSingleMainObserver<T, R>
    extends AtomicInteger
    implements Observer<T>, Disposable {

        private static final long serialVersionUID = -9140123220065488293L;

        final Observer<? super R> downstream;

        final Function<? super T, ? extends SingleSource<? extends R>> mapper;

        final AtomicThrowable errors;

        final ConcatMapSingleObserver<R> inner;

        final SimplePlainQueue<T> queue;

        final ErrorMode errorMode;

        Disposable upstream;

        volatile boolean done;

        volatile boolean cancelled;

        R item;

        volatile int state;

        /** No inner SingleSource is running. */
        static final int STATE_INACTIVE = 0;
        /** An inner SingleSource is running but there are no results yet. */
        static final int STATE_ACTIVE = 1;
        /** The inner SingleSource succeeded with a value in {@link #item}. */
        static final int STATE_RESULT_VALUE = 2;

        ConcatMapSingleMainObserver(Observer<? super R> downstream,
                Function<? super T, ? extends SingleSource<? extends R>> mapper,
                        int prefetch, ErrorMode errorMode) {
            this.downstream = downstream;
            this.mapper = mapper;
            this.errorMode = errorMode;
            this.errors = new AtomicThrowable();
            this.inner = new ConcatMapSingleObserver<R>(this);
            this.queue = new SpscArrayQueue<T>(prefetch);
        }

        @Override
        public void onSubscribe(Disposable s) {
            if (DisposableHelper.validate(upstream, s)) {
                upstream = s;
                downstream.onSubscribe(this);
            }
        }

        @Override
        public void onNext(T t) {
            queue.offer(t);
            drain();
        }

        @Override
        public void onError(Throwable t) {
            if (errors.addThrowable(t)) {
                if (errorMode == ErrorMode.IMMEDIATE) {
                    inner.dispose();
                }
                done = true;
                drain();
            } else {
                RxJavaPlugins.onError(t);
            }
        }

        @Override
        public void onComplete() {
            done = true;
            drain();
        }

        @Override
        public void dispose() {
            cancelled = true;
            upstream.dispose();
            inner.dispose();
            if (getAndIncrement() == 0) {
                queue.clear();
                item = null;
            }
        }

        @Override
        public boolean isDisposed() {
            return cancelled;
        }

        void innerSuccess(R item) {
            this.item = item;
            this.state = STATE_RESULT_VALUE;
            drain();
        }

        void innerError(Throwable ex) {
            if (errors.addThrowable(ex)) {
                if (errorMode != ErrorMode.END) {
                    upstream.dispose();
                }
                this.state = STATE_INACTIVE;
                drain();
            } else {
                RxJavaPlugins.onError(ex);
            }
        }

        void drain() {
            if (getAndIncrement() != 0) {
                return;
            }

            int missed = 1;
            Observer<? super R> downstream = this.downstream;
            ErrorMode errorMode = this.errorMode;
            SimplePlainQueue<T> queue = this.queue;
            AtomicThrowable errors = this.errors;

            for (;;) {

                for (;;) {
                    if (cancelled) {
                        queue.clear();
                        item = null;
                    }

                    int s = state;

                    if (errors.get() != null) {
                        if (errorMode == ErrorMode.IMMEDIATE
                                || (errorMode == ErrorMode.BOUNDARY && s == STATE_INACTIVE)) {
                            queue.clear();
                            item = null;
                            Throwable ex = errors.terminate();
                            downstream.onError(ex);
                            return;
                        }
                    }

                    if (s == STATE_INACTIVE) {
                        boolean d = done;
                        T v = queue.poll();
                        boolean empty = v == null;

                        if (d && empty) {
                            Throwable ex = errors.terminate();
                            if (ex == null) {
                                downstream.onComplete();
                            } else {
                                downstream.onError(ex);
                            }
                            return;
                        }

                        if (empty) {
                            break;
                        }

                        SingleSource<? extends R> ss;

                        try {
                            ss = ObjectHelper.requireNonNull(mapper.apply(v), "The mapper returned a null SingleSource");
                        } catch (Throwable ex) {
                            Exceptions.throwIfFatal(ex);
                            upstream.dispose();
                            queue.clear();
                            errors.addThrowable(ex);
                            ex = errors.terminate();
                            downstream.onError(ex);
                            return;
                        }

                        state = STATE_ACTIVE;
                        ss.subscribe(inner);
                        break;
                    } else if (s == STATE_RESULT_VALUE) {
                        R w = item;
                        item = null;
                        downstream.onNext(w);

                        state = STATE_INACTIVE;
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

            final ConcatMapSingleMainObserver<?, R> parent;

            ConcatMapSingleObserver(ConcatMapSingleMainObserver<?, R> parent) {
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
