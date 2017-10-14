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
import io.reactivex.disposables.*;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.functions.Function;
import io.reactivex.internal.disposables.DisposableHelper;
import io.reactivex.internal.functions.ObjectHelper;
import io.reactivex.internal.queue.SpscLinkedArrayQueue;
import io.reactivex.internal.util.AtomicThrowable;
import io.reactivex.plugins.RxJavaPlugins;

/**
 * Maps upstream values into SingleSources and merges their signals into one sequence.
 * @param <T> the source value type
 * @param <R> the result value type
 */
public final class ObservableFlatMapSingle<T, R> extends AbstractObservableWithUpstream<T, R> {

    final Function<? super T, ? extends SingleSource<? extends R>> mapper;

    final boolean delayErrors;

    public ObservableFlatMapSingle(ObservableSource<T> source, Function<? super T, ? extends SingleSource<? extends R>> mapper,
            boolean delayError) {
        super(source);
        this.mapper = mapper;
        this.delayErrors = delayError;
    }

    @Override
    protected void subscribeActual(Observer<? super R> s) {
        source.subscribe(new FlatMapSingleObserver<T, R>(s, mapper, delayErrors));
    }

    static final class FlatMapSingleObserver<T, R>
    extends AtomicInteger
    implements Observer<T>, Disposable {

        private static final long serialVersionUID = 8600231336733376951L;

        final Observer<? super R> actual;

        final boolean delayErrors;

        final CompositeDisposable set;

        final AtomicInteger active;

        final AtomicThrowable errors;

        final Function<? super T, ? extends SingleSource<? extends R>> mapper;

        final AtomicReference<SpscLinkedArrayQueue<R>> queue;

        Disposable d;

        volatile boolean cancelled;

        FlatMapSingleObserver(Observer<? super R> actual,
                Function<? super T, ? extends SingleSource<? extends R>> mapper, boolean delayErrors) {
            this.actual = actual;
            this.mapper = mapper;
            this.delayErrors = delayErrors;
            this.set = new CompositeDisposable();
            this.errors = new AtomicThrowable();
            this.active = new AtomicInteger(1);
            this.queue = new AtomicReference<SpscLinkedArrayQueue<R>>();
        }

        @Override
        public void onSubscribe(Disposable d) {
            if (DisposableHelper.validate(this.d, d)) {
                this.d = d;

                actual.onSubscribe(this);
            }
        }

        @Override
        public void onNext(T t) {
            SingleSource<? extends R> ms;

            try {
                ms = ObjectHelper.requireNonNull(mapper.apply(t), "The mapper returned a null SingleSource");
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                d.dispose();
                onError(ex);
                return;
            }

            active.getAndIncrement();

            InnerObserver inner = new InnerObserver();

            if (!cancelled && set.add(inner)) {
                ms.subscribe(inner);
            }
        }

        @Override
        public void onError(Throwable t) {
            active.decrementAndGet();
            if (errors.addThrowable(t)) {
                if (!delayErrors) {
                    set.dispose();
                }
                drain();
            } else {
                RxJavaPlugins.onError(t);
            }
        }

        @Override
        public void onComplete() {
            active.decrementAndGet();
            drain();
        }

        @Override
        public void dispose() {
            cancelled = true;
            d.dispose();
            set.dispose();
        }

        @Override
        public boolean isDisposed() {
            return cancelled;
        }

        void innerSuccess(InnerObserver inner, R value) {
            set.delete(inner);
            if (get() == 0 && compareAndSet(0, 1)) {
                actual.onNext(value);

                boolean d = active.decrementAndGet() == 0;
                SpscLinkedArrayQueue<R> q = queue.get();

                if (d && (q == null || q.isEmpty())) {
                    Throwable ex = errors.terminate();
                    if (ex != null) {
                        actual.onError(ex);
                    } else {
                        actual.onComplete();
                    }
                    return;
                }
                if (decrementAndGet() == 0) {
                    return;
                }
            } else {
                SpscLinkedArrayQueue<R> q = getOrCreateQueue();
                synchronized (q) {
                    q.offer(value);
                }
                active.decrementAndGet();
                if (getAndIncrement() != 0) {
                    return;
                }
            }
            drainLoop();
        }

        SpscLinkedArrayQueue<R> getOrCreateQueue() {
            for (;;) {
                SpscLinkedArrayQueue<R> current = queue.get();
                if (current != null) {
                    return current;
                }
                current = new SpscLinkedArrayQueue<R>(Observable.bufferSize());
                if (queue.compareAndSet(null, current)) {
                    return current;
                }
            }
        }

        void innerError(InnerObserver inner, Throwable e) {
            set.delete(inner);
            if (errors.addThrowable(e)) {
                if (!delayErrors) {
                    d.dispose();
                    set.dispose();
                }
                active.decrementAndGet();
                drain();
            } else {
                RxJavaPlugins.onError(e);
            }
        }

        void drain() {
            if (getAndIncrement() == 0) {
                drainLoop();
            }
        }

        void clear() {
            SpscLinkedArrayQueue<R> q = queue.get();
            if (q != null) {
                q.clear();
            }
        }

        void drainLoop() {
            int missed = 1;
            Observer<? super R> a = actual;
            AtomicInteger n = active;
            AtomicReference<SpscLinkedArrayQueue<R>> qr = queue;

            for (;;) {
                for (;;) {
                    if (cancelled) {
                        clear();
                        return;
                    }

                    if (!delayErrors) {
                        Throwable ex = errors.get();
                        if (ex != null) {
                            ex = errors.terminate();
                            clear();
                            a.onError(ex);
                            return;
                        }
                    }

                    boolean d = n.get() == 0;
                    SpscLinkedArrayQueue<R> q = qr.get();
                    R v = q != null ? q.poll() : null;
                    boolean empty = v == null;

                    if (d && empty) {
                        Throwable ex = errors.terminate();
                        if (ex != null) {
                            a.onError(ex);
                        } else {
                            a.onComplete();
                        }
                        return;
                    }

                    if (empty) {
                        break;
                    }

                    a.onNext(v);
                }

                missed = addAndGet(-missed);
                if (missed == 0) {
                    break;
                }
            }
        }

        final class InnerObserver extends AtomicReference<Disposable>
        implements SingleObserver<R>, Disposable {
            private static final long serialVersionUID = -502562646270949838L;

            @Override
            public void onSubscribe(Disposable d) {
                DisposableHelper.setOnce(this, d);
            }

            @Override
            public void onSuccess(R value) {
                innerSuccess(this, value);
            }

            @Override
            public void onError(Throwable e) {
                innerError(this, e);
            }

            @Override
            public boolean isDisposed() {
                return DisposableHelper.isDisposed(get());
            }

            @Override
            public void dispose() {
                DisposableHelper.dispose(this);
            }
        }
    }
}
