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

package io.reactivex.internal.operators.observable;

import java.util.ArrayDeque;
import java.util.concurrent.atomic.AtomicInteger;

import io.reactivex.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.functions.Function;
import io.reactivex.internal.disposables.DisposableHelper;
import io.reactivex.internal.functions.ObjectHelper;
import io.reactivex.internal.fuseable.*;
import io.reactivex.internal.observers.*;
import io.reactivex.internal.queue.SpscLinkedArrayQueue;
import io.reactivex.internal.util.*;
import io.reactivex.plugins.RxJavaPlugins;

public final class ObservableConcatMapEager<T, R> extends AbstractObservableWithUpstream<T, R> {

    final Function<? super T, ? extends ObservableSource<? extends R>> mapper;

    final ErrorMode errorMode;

    final int maxConcurrency;

    final int prefetch;

    public ObservableConcatMapEager(ObservableSource<T> source,
            Function<? super T, ? extends ObservableSource<? extends R>> mapper,
            ErrorMode errorMode,
            int maxConcurrency, int prefetch) {
        super(source);
        this.mapper = mapper;
        this.errorMode = errorMode;
        this.maxConcurrency = maxConcurrency;
        this.prefetch = prefetch;
    }

    @Override
    protected void subscribeActual(Observer<? super R> observer) {
        source.subscribe(new ConcatMapEagerMainObserver<T, R>(observer, mapper, maxConcurrency, prefetch, errorMode));
    }

    static final class ConcatMapEagerMainObserver<T, R>
    extends AtomicInteger
    implements Observer<T>, Disposable, InnerQueuedObserverSupport<R> {

        private static final long serialVersionUID = 8080567949447303262L;

        final Observer<? super R> actual;

        final Function<? super T, ? extends ObservableSource<? extends R>> mapper;

        final int maxConcurrency;

        final int prefetch;

        final ErrorMode errorMode;

        final AtomicThrowable error;

        final ArrayDeque<InnerQueuedObserver<R>> observers;

        SimpleQueue<T> queue;

        Disposable d;

        volatile boolean done;

        int sourceMode;

        volatile boolean cancelled;

        InnerQueuedObserver<R> current;

        int activeCount;

        ConcatMapEagerMainObserver(Observer<? super R> actual,
                Function<? super T, ? extends ObservableSource<? extends R>> mapper,
                int maxConcurrency, int prefetch, ErrorMode errorMode) {
            this.actual = actual;
            this.mapper = mapper;
            this.maxConcurrency = maxConcurrency;
            this.prefetch = prefetch;
            this.errorMode = errorMode;
            this.error = new AtomicThrowable();
            this.observers = new ArrayDeque<InnerQueuedObserver<R>>();
        }

        @SuppressWarnings("unchecked")
        @Override
        public void onSubscribe(Disposable d) {
            if (DisposableHelper.validate(this.d, d)) {
                this.d = d;

                if (d instanceof QueueDisposable) {
                    QueueDisposable<T> qd = (QueueDisposable<T>) d;

                    int m = qd.requestFusion(QueueDisposable.ANY);
                    if (m == QueueDisposable.SYNC) {
                        sourceMode = m;
                        queue = qd;
                        done = true;

                        actual.onSubscribe(this);

                        drain();
                        return;
                    }
                    if (m == QueueDisposable.ASYNC) {
                        sourceMode = m;
                        queue = qd;

                        actual.onSubscribe(this);

                        return;
                    }
                }

                queue = new SpscLinkedArrayQueue<T>(prefetch);

                actual.onSubscribe(this);
            }
        }

        @Override
        public void onNext(T value) {
            if (sourceMode == QueueDisposable.NONE) {
                queue.offer(value);
            }
            drain();
        }

        @Override
        public void onError(Throwable e) {
            if (error.addThrowable(e)) {
                done = true;
                drain();
            } else {
                RxJavaPlugins.onError(e);
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
            if (getAndIncrement() == 0) {
                queue.clear();
                disposeAll();
            }
        }

        @Override
        public boolean isDisposed() {
            return cancelled;
        }

        void disposeAll() {
            InnerQueuedObserver<R> inner = current;

            if (inner != null) {
                inner.dispose();
            }

            for (;;) {

                inner = observers.poll();

                if (inner == null) {
                    return;
                }

                inner.dispose();
            }
        }

        @Override
        public void innerNext(InnerQueuedObserver<R> inner, R value) {
            inner.queue().offer(value);
            drain();
        }

        @Override
        public void innerError(InnerQueuedObserver<R> inner, Throwable e) {
            if (error.addThrowable(e)) {
                if (errorMode == ErrorMode.IMMEDIATE) {
                    d.dispose();
                }
                inner.setDone();
                drain();
            } else {
                RxJavaPlugins.onError(e);
            }
        }

        @Override
        public void innerComplete(InnerQueuedObserver<R> inner) {
            inner.setDone();
            drain();
        }

        @Override
        public void drain() {
            if (getAndIncrement() != 0) {
                return;
            }

            int missed = 1;

            SimpleQueue<T> q = queue;
            ArrayDeque<InnerQueuedObserver<R>> observers = this.observers;
            Observer<? super R> a = this.actual;
            ErrorMode errorMode = this.errorMode;

            outer:
            for (;;) {

                int ac = activeCount;

                while (ac != maxConcurrency) {
                    if (cancelled) {
                        q.clear();
                        disposeAll();
                        return;
                    }

                    if (errorMode == ErrorMode.IMMEDIATE) {
                        Throwable ex = error.get();
                        if (ex != null) {
                            q.clear();
                            disposeAll();

                            a.onError(error.terminate());
                            return;
                        }
                    }

                    T v;
                    ObservableSource<? extends R> source;

                    try {
                        v = q.poll();

                        if (v == null) {
                            break;
                        }

                        source = ObjectHelper.requireNonNull(mapper.apply(v), "The mapper returned a null ObservableSource");
                    } catch (Throwable ex) {
                        Exceptions.throwIfFatal(ex);
                        d.dispose();
                        q.clear();
                        disposeAll();
                        error.addThrowable(ex);
                        a.onError(error.terminate());
                        return;
                    }

                    InnerQueuedObserver<R> inner = new InnerQueuedObserver<R>(this, prefetch);

                    observers.offer(inner);

                    source.subscribe(inner);

                    ac++;
                }

                activeCount = ac;

                if (cancelled) {
                    q.clear();
                    disposeAll();
                    return;
                }

                if (errorMode == ErrorMode.IMMEDIATE) {
                    Throwable ex = error.get();
                    if (ex != null) {
                        q.clear();
                        disposeAll();

                        a.onError(error.terminate());
                        return;
                    }
                }

                InnerQueuedObserver<R> active = current;

                if (active == null) {
                    if (errorMode == ErrorMode.BOUNDARY) {
                        Throwable ex = error.get();
                        if (ex != null) {
                            q.clear();
                            disposeAll();

                            a.onError(error.terminate());
                            return;
                        }
                    }
                    boolean d = done;

                    active = observers.poll();

                    boolean empty = active == null;

                    if (d && empty) {
                        Throwable ex = error.get();
                        if (ex != null) {
                            q.clear();
                            disposeAll();

                            a.onError(error.terminate());
                        } else {
                            a.onComplete();
                        }
                        return;
                    }

                    if (!empty) {
                        current = active;
                    }

                }

                if (active != null) {
                    SimpleQueue<R> aq = active.queue();

                    for (;;) {
                        if (cancelled) {
                            q.clear();
                            disposeAll();
                            return;
                        }

                        boolean d = active.isDone();

                        if (errorMode == ErrorMode.IMMEDIATE) {
                            Throwable ex = error.get();
                            if (ex != null) {
                                q.clear();
                                disposeAll();

                                a.onError(error.terminate());
                                return;
                            }
                        }

                        R w;

                        try {
                            w = aq.poll();
                        } catch (Throwable ex) {
                            Exceptions.throwIfFatal(ex);
                            error.addThrowable(ex);

                            current = null;
                            activeCount--;
                            continue outer;
                        }

                        boolean empty = w == null;

                        if (d && empty) {
                            current = null;
                            activeCount--;
                            continue outer;
                        }

                        if (empty) {
                            break;
                        }

                        a.onNext(w);
                    }
                }

                missed = addAndGet(-missed);
                if (missed == 0) {
                    break;
                }
            }
        }
    }

}
