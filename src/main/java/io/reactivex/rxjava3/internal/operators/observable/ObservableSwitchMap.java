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

package io.reactivex.rxjava3.internal.operators.observable;

import java.util.Objects;
import java.util.concurrent.atomic.*;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.exceptions.Exceptions;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.internal.disposables.DisposableHelper;
import io.reactivex.rxjava3.internal.fuseable.*;
import io.reactivex.rxjava3.internal.queue.SpscLinkedArrayQueue;
import io.reactivex.rxjava3.internal.util.AtomicThrowable;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;

public final class ObservableSwitchMap<T, R> extends AbstractObservableWithUpstream<T, R> {
    final Function<? super T, ? extends ObservableSource<? extends R>> mapper;
    final int bufferSize;

    final boolean delayErrors;

    public ObservableSwitchMap(ObservableSource<T> source,
                               Function<? super T, ? extends ObservableSource<? extends R>> mapper, int bufferSize,
                                       boolean delayErrors) {
        super(source);
        this.mapper = mapper;
        this.bufferSize = bufferSize;
        this.delayErrors = delayErrors;
    }

    @Override
    public void subscribeActual(Observer<? super R> t) {

        if (ObservableScalarXMap.tryScalarXMapSubscribe(source, t, mapper)) {
            return;
        }

        source.subscribe(new SwitchMapObserver<T, R>(t, mapper, bufferSize, delayErrors));
    }

    static final class SwitchMapObserver<T, R> extends AtomicInteger implements Observer<T>, Disposable {

        private static final long serialVersionUID = -3491074160481096299L;
        final Observer<? super R> downstream;
        final Function<? super T, ? extends ObservableSource<? extends R>> mapper;
        final int bufferSize;

        final boolean delayErrors;

        final AtomicThrowable errors;

        volatile boolean done;

        volatile boolean cancelled;

        Disposable upstream;

        final AtomicReference<SwitchMapInnerObserver<T, R>> active = new AtomicReference<SwitchMapInnerObserver<T, R>>();

        static final SwitchMapInnerObserver<Object, Object> CANCELLED;
        static {
            CANCELLED = new SwitchMapInnerObserver<Object, Object>(null, -1L, 1);
            CANCELLED.cancel();
        }

        volatile long unique;

        SwitchMapObserver(Observer<? super R> actual,
                Function<? super T, ? extends ObservableSource<? extends R>> mapper, int bufferSize,
                        boolean delayErrors) {
            this.downstream = actual;
            this.mapper = mapper;
            this.bufferSize = bufferSize;
            this.delayErrors = delayErrors;
            this.errors = new AtomicThrowable();
        }

        @Override
        public void onSubscribe(Disposable d) {
            if (DisposableHelper.validate(this.upstream, d)) {
                this.upstream = d;
                downstream.onSubscribe(this);
            }
        }

        @Override
        public void onNext(T t) {
            long c = unique + 1;
            unique = c;

            SwitchMapInnerObserver<T, R> inner = active.get();
            if (inner != null) {
                inner.cancel();
            }

            ObservableSource<? extends R> p;
            try {
                p = Objects.requireNonNull(mapper.apply(t), "The ObservableSource returned is null");
            } catch (Throwable e) {
                Exceptions.throwIfFatal(e);
                upstream.dispose();
                onError(e);
                return;
            }

            SwitchMapInnerObserver<T, R> nextInner = new SwitchMapInnerObserver<T, R>(this, c, bufferSize);

            for (;;) {
                inner = active.get();
                if (inner == CANCELLED) {
                    break;
                }
                if (active.compareAndSet(inner, nextInner)) {
                    p.subscribe(nextInner);
                    break;
                }
            }
        }

        @Override
        public void onError(Throwable t) {
            if (!done && errors.tryAddThrowable(t)) {
                if (!delayErrors) {
                    disposeInner();
                }
                done = true;
                drain();
            } else {
                RxJavaPlugins.onError(t);
            }
        }

        @Override
        public void onComplete() {
            if (!done) {
                done = true;
                drain();
            }
        }

        @Override
        public void dispose() {
            if (!cancelled) {
                cancelled = true;
                upstream.dispose();
                disposeInner();

                errors.tryTerminateAndReport();
            }
        }

        @Override
        public boolean isDisposed() {
            return cancelled;
        }

        @SuppressWarnings("unchecked")
        void disposeInner() {
            SwitchMapInnerObserver<T, R> a = active.get();
            if (a != CANCELLED) {
                a = active.getAndSet((SwitchMapInnerObserver<T, R>)CANCELLED);
                if (a != CANCELLED && a != null) {
                    a.cancel();
                }
            }
        }

        void drain() {
            if (getAndIncrement() != 0) {
                return;
            }

            final Observer<? super R> a = downstream;
            final AtomicReference<SwitchMapInnerObserver<T, R>> active = this.active;
            final boolean delayErrors = this.delayErrors;

            int missing = 1;

            for (;;) {

                if (cancelled) {
                    return;
                }

                if (done) {
                    boolean empty = active.get() == null;
                    if (delayErrors) {
                        if (empty) {
                            Throwable ex = errors.get();
                            if (ex != null) {
                                a.onError(ex);
                            } else {
                                a.onComplete();
                            }
                            return;
                        }
                    } else {
                        Throwable ex = errors.get();
                        if (ex != null) {
                            errors.tryTerminateConsumer(a);
                            return;
                        }
                        if (empty) {
                            a.onComplete();
                            return;
                        }
                    }
                }

                SwitchMapInnerObserver<T, R> inner = active.get();

                if (inner != null) {
                    SimpleQueue<R> q = inner.queue;

                    if (q != null) {
                        if (inner.done) {
                            boolean empty = q.isEmpty();
                            if (delayErrors) {
                                if (empty) {
                                    active.compareAndSet(inner, null);
                                    continue;
                                }
                            } else {
                                Throwable ex = errors.get();
                                if (ex != null) {
                                    errors.tryTerminateConsumer(a);
                                    return;
                                }
                                if (empty) {
                                    active.compareAndSet(inner, null);
                                    continue;
                                }
                            }
                        }

                        boolean retry = false;

                        for (;;) {
                            if (cancelled) {
                                return;
                            }
                            if (inner != active.get()) {
                                retry = true;
                                break;
                            }

                            if (!delayErrors) {
                                Throwable ex = errors.get();
                                if (ex != null) {
                                    errors.tryTerminateConsumer(a);
                                    return;
                                }
                            }

                            boolean d = inner.done;
                            R v;

                            try {
                                v = q.poll();
                            } catch (Throwable ex) {
                                Exceptions.throwIfFatal(ex);
                                errors.tryAddThrowableOrReport(ex);
                                active.compareAndSet(inner, null);
                                if (!delayErrors) {
                                    disposeInner();
                                    upstream.dispose();
                                    done = true;
                                } else {
                                    inner.cancel();
                                }
                                v = null;
                                retry = true;
                            }
                            boolean empty = v == null;

                            if (d && empty) {
                                active.compareAndSet(inner, null);
                                retry = true;
                                break;
                            }

                            if (empty) {
                                break;
                            }

                            a.onNext(v);
                        }

                        if (retry) {
                            continue;
                        }
                    }
                }

                missing = addAndGet(-missing);
                if (missing == 0) {
                    break;
                }
            }
        }

        void innerError(SwitchMapInnerObserver<T, R> inner, Throwable ex) {
            if (inner.index == unique && errors.tryAddThrowable(ex)) {
                if (!delayErrors) {
                    upstream.dispose();
                    done = true;
                }
                inner.done = true;
                drain();
            } else {
                RxJavaPlugins.onError(ex);
            }
        }
    }

    static final class SwitchMapInnerObserver<T, R> extends AtomicReference<Disposable> implements Observer<R> {

        private static final long serialVersionUID = 3837284832786408377L;
        final SwitchMapObserver<T, R> parent;
        final long index;

        final int bufferSize;

        volatile SimpleQueue<R> queue;

        volatile boolean done;

        SwitchMapInnerObserver(SwitchMapObserver<T, R> parent, long index, int bufferSize) {
            this.parent = parent;
            this.index = index;
            this.bufferSize = bufferSize;
        }

        @Override
        public void onSubscribe(Disposable d) {
            if (DisposableHelper.setOnce(this, d)) {
                if (d instanceof QueueDisposable) {
                    @SuppressWarnings("unchecked")
                    QueueDisposable<R> qd = (QueueDisposable<R>) d;

                    int m = qd.requestFusion(QueueDisposable.ANY | QueueDisposable.BOUNDARY);
                    if (m == QueueDisposable.SYNC) {
                        queue = qd;
                        done = true;
                        parent.drain();
                        return;
                    }
                    if (m == QueueDisposable.ASYNC) {
                        queue = qd;
                        return;
                    }
                }

                queue = new SpscLinkedArrayQueue<R>(bufferSize);
            }
        }

        @Override
        public void onNext(R t) {
            if (index == parent.unique) {
                if (t != null) {
                    queue.offer(t);
                }
                parent.drain();
            }
        }

        @Override
        public void onError(Throwable t) {
            parent.innerError(this, t);
        }

        @Override
        public void onComplete() {
            if (index == parent.unique) {
                done = true;
                parent.drain();
            }
        }

        public void cancel() {
            DisposableHelper.dispose(this);
        }
    }
}
