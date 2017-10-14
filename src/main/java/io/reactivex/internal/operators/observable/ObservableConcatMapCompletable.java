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

import java.util.concurrent.atomic.*;

import io.reactivex.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.functions.Function;
import io.reactivex.internal.disposables.DisposableHelper;
import io.reactivex.internal.functions.ObjectHelper;
import io.reactivex.internal.fuseable.*;
import io.reactivex.internal.queue.SpscLinkedArrayQueue;
import io.reactivex.plugins.RxJavaPlugins;

public final class ObservableConcatMapCompletable<T> extends Completable {

    final ObservableSource<T> source;
    final Function<? super T, ? extends CompletableSource> mapper;
    final int bufferSize;

    public ObservableConcatMapCompletable(ObservableSource<T> source,
            Function<? super T, ? extends CompletableSource> mapper, int bufferSize) {
        this.source = source;
        this.mapper = mapper;
        this.bufferSize = Math.max(8, bufferSize);
    }
    @Override
    public void subscribeActual(CompletableObserver observer) {
        source.subscribe(new SourceObserver<T>(observer, mapper, bufferSize));
    }

    static final class SourceObserver<T> extends AtomicInteger implements Observer<T>, Disposable {

        private static final long serialVersionUID = 6893587405571511048L;
        final CompletableObserver actual;
        final Function<? super T, ? extends CompletableSource> mapper;
        final InnerObserver inner;
        final int bufferSize;

        SimpleQueue<T> queue;

        Disposable s;

        volatile boolean active;

        volatile boolean disposed;

        volatile boolean done;

        int sourceMode;

        SourceObserver(CompletableObserver actual,
                                Function<? super T, ? extends CompletableSource> mapper, int bufferSize) {
            this.actual = actual;
            this.mapper = mapper;
            this.bufferSize = bufferSize;
            this.inner = new InnerObserver(actual, this);
        }
        @Override
        public void onSubscribe(Disposable s) {
            if (DisposableHelper.validate(this.s, s)) {
                this.s = s;
                if (s instanceof QueueDisposable) {
                    @SuppressWarnings("unchecked")
                    QueueDisposable<T> qd = (QueueDisposable<T>) s;

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

                queue = new SpscLinkedArrayQueue<T>(bufferSize);

                actual.onSubscribe(this);
            }
        }
        @Override
        public void onNext(T t) {
            if (done) {
                return;
            }
            if (sourceMode == QueueDisposable.NONE) {
                queue.offer(t);
            }
            drain();
        }
        @Override
        public void onError(Throwable t) {
            if (done) {
                RxJavaPlugins.onError(t);
                return;
            }
            done = true;
            dispose();
            actual.onError(t);
        }
        @Override
        public void onComplete() {
            if (done) {
                return;
            }
            done = true;
            drain();
        }

        void innerComplete() {
            active = false;
            drain();
        }

        @Override
        public boolean isDisposed() {
            return disposed;
        }

        @Override
        public void dispose() {
            disposed = true;
            inner.dispose();
            s.dispose();

            if (getAndIncrement() == 0) {
                queue.clear();
            }
        }

        void drain() {
            if (getAndIncrement() != 0) {
                return;
            }

            for (;;) {
                if (disposed) {
                    queue.clear();
                    return;
                }
                if (!active) {

                    boolean d = done;

                    T t;

                    try {
                        t = queue.poll();
                    } catch (Throwable ex) {
                        Exceptions.throwIfFatal(ex);
                        dispose();
                        queue.clear();
                        actual.onError(ex);
                        return;
                    }

                    boolean empty = t == null;

                    if (d && empty) {
                        disposed = true;
                        actual.onComplete();
                        return;
                    }

                    if (!empty) {
                        CompletableSource c;

                        try {
                            c = ObjectHelper.requireNonNull(mapper.apply(t), "The mapper returned a null CompletableSource");
                        } catch (Throwable ex) {
                            Exceptions.throwIfFatal(ex);
                            dispose();
                            queue.clear();
                            actual.onError(ex);
                            return;
                        }

                        active = true;
                        c.subscribe(inner);
                    }
                }

                if (decrementAndGet() == 0) {
                    break;
                }
            }
        }

        static final class InnerObserver extends AtomicReference<Disposable> implements CompletableObserver {
            private static final long serialVersionUID = -5987419458390772447L;
            final CompletableObserver actual;
            final SourceObserver<?> parent;

            InnerObserver(CompletableObserver actual, SourceObserver<?> parent) {
                this.actual = actual;
                this.parent = parent;
            }

            @Override
            public void onSubscribe(Disposable s) {
                DisposableHelper.set(this, s);
            }

            @Override
            public void onError(Throwable t) {
                parent.dispose();
                actual.onError(t);
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
