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

import org.reactivestreams.Subscription;

import io.reactivex.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.*;
import io.reactivex.functions.Function;
import io.reactivex.internal.disposables.DisposableHelper;
import io.reactivex.internal.functions.ObjectHelper;
import io.reactivex.internal.fuseable.SimplePlainQueue;
import io.reactivex.internal.queue.SpscArrayQueue;
import io.reactivex.internal.subscriptions.SubscriptionHelper;
import io.reactivex.internal.util.*;
import io.reactivex.plugins.RxJavaPlugins;

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
    protected void subscribeActual(CompletableObserver s) {
        source.subscribe(new ConcatMapCompletableObserver<T>(s, mapper, errorMode, prefetch));
    }

    static final class ConcatMapCompletableObserver<T>
    extends AtomicInteger
    implements FlowableSubscriber<T>, Disposable {

        private static final long serialVersionUID = 3610901111000061034L;

        final CompletableObserver downstream;

        final Function<? super T, ? extends CompletableSource> mapper;

        final ErrorMode errorMode;

        final AtomicThrowable errors;

        final ConcatMapInnerObserver inner;

        final int prefetch;

        final SimplePlainQueue<T> queue;

        Subscription upstream;

        volatile boolean active;

        volatile boolean done;

        volatile boolean disposed;

        int consumed;

        ConcatMapCompletableObserver(CompletableObserver downstream,
                Function<? super T, ? extends CompletableSource> mapper,
                ErrorMode errorMode, int prefetch) {
            this.downstream = downstream;
            this.mapper = mapper;
            this.errorMode = errorMode;
            this.prefetch = prefetch;
            this.errors = new AtomicThrowable();
            this.inner = new ConcatMapInnerObserver(this);
            this.queue = new SpscArrayQueue<T>(prefetch);
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.validate(upstream, s)) {
                this.upstream = s;
                downstream.onSubscribe(this);
                s.request(prefetch);
            }
        }

        @Override
        public void onNext(T t) {
            if (queue.offer(t)) {
                drain();
            } else {
                upstream.cancel();
                onError(new MissingBackpressureException("Queue full?!"));
            }
        }

        @Override
        public void onError(Throwable t) {
            if (errors.addThrowable(t)) {
                if (errorMode == ErrorMode.IMMEDIATE) {
                    inner.dispose();
                    t = errors.terminate();
                    if (t != ExceptionHelper.TERMINATED) {
                        downstream.onError(t);
                    }
                    if (getAndIncrement() == 0) {
                        queue.clear();
                    }
                } else {
                    done = true;
                    drain();
                }
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
            disposed = true;
            upstream.cancel();
            inner.dispose();
            if (getAndIncrement() == 0) {
                queue.clear();
            }
        }

        @Override
        public boolean isDisposed() {
            return disposed;
        }

        void innerError(Throwable ex) {
            if (errors.addThrowable(ex)) {
                if (errorMode == ErrorMode.IMMEDIATE) {
                    upstream.cancel();
                    ex = errors.terminate();
                    if (ex != ExceptionHelper.TERMINATED) {
                        downstream.onError(ex);
                    }
                    if (getAndIncrement() == 0) {
                        queue.clear();
                    }
                } else {
                    active = false;
                    drain();
                }
            } else {
                RxJavaPlugins.onError(ex);
            }
        }

        void innerComplete() {
            active = false;
            drain();
        }

        void drain() {
            if (getAndIncrement() != 0) {
                return;
            }

            do {
                if (disposed) {
                    queue.clear();
                    return;
                }

                if (!active) {

                    if (errorMode == ErrorMode.BOUNDARY) {
                        if (errors.get() != null) {
                            queue.clear();
                            Throwable ex = errors.terminate();
                            downstream.onError(ex);
                            return;
                        }
                    }

                    boolean d = done;
                    T v = queue.poll();
                    boolean empty = v == null;

                    if (d && empty) {
                        Throwable ex = errors.terminate();
                        if (ex != null) {
                            downstream.onError(ex);
                        } else {
                            downstream.onComplete();
                        }
                        return;
                    }

                    if (!empty) {

                        int limit = prefetch - (prefetch >> 1);
                        int c = consumed + 1;
                        if (c == limit) {
                            consumed = 0;
                            upstream.request(limit);
                        } else {
                            consumed = c;
                        }

                        CompletableSource cs;

                        try {
                            cs = ObjectHelper.requireNonNull(mapper.apply(v), "The mapper returned a null CompletableSource");
                        } catch (Throwable ex) {
                            Exceptions.throwIfFatal(ex);
                            queue.clear();
                            upstream.cancel();
                            errors.addThrowable(ex);
                            ex = errors.terminate();
                            downstream.onError(ex);
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
