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

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.*;

import io.reactivex.ObservableSource;
import io.reactivex.Observer;
import io.reactivex.disposables.*;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.functions.Function;
import io.reactivex.internal.disposables.DisposableHelper;
import io.reactivex.internal.functions.ObjectHelper;
import io.reactivex.internal.queue.SpscLinkedArrayQueue;
import io.reactivex.internal.util.AtomicThrowable;
import io.reactivex.plugins.RxJavaPlugins;

public final class ObservableBufferBoundary<T, U extends Collection<? super T>, Open, Close>
extends AbstractObservableWithUpstream<T, U> {
    final Callable<U> bufferSupplier;
    final ObservableSource<? extends Open> bufferOpen;
    final Function<? super Open, ? extends ObservableSource<? extends Close>> bufferClose;

    public ObservableBufferBoundary(ObservableSource<T> source, ObservableSource<? extends Open> bufferOpen,
                                    Function<? super Open, ? extends ObservableSource<? extends Close>> bufferClose, Callable<U> bufferSupplier) {
        super(source);
        this.bufferOpen = bufferOpen;
        this.bufferClose = bufferClose;
        this.bufferSupplier = bufferSupplier;
    }

    @Override
    protected void subscribeActual(Observer<? super U> t) {
        BufferBoundaryObserver<T, U, Open, Close> parent =
            new BufferBoundaryObserver<T, U, Open, Close>(
                t, bufferOpen, bufferClose, bufferSupplier
            );
        t.onSubscribe(parent);
        source.subscribe(parent);
    }

    static final class BufferBoundaryObserver<T, C extends Collection<? super T>, Open, Close>
    extends AtomicInteger implements Observer<T>, Disposable {

        private static final long serialVersionUID = -8466418554264089604L;

        final Observer<? super C> actual;

        final Callable<C> bufferSupplier;

        final ObservableSource<? extends Open> bufferOpen;

        final Function<? super Open, ? extends ObservableSource<? extends Close>> bufferClose;

        final CompositeDisposable observers;

        final AtomicReference<Disposable> upstream;

        final AtomicThrowable errors;

        volatile boolean done;

        final SpscLinkedArrayQueue<C> queue;

        volatile boolean cancelled;

        long index;

        Map<Long, C> buffers;

        BufferBoundaryObserver(Observer<? super C> actual,
                ObservableSource<? extends Open> bufferOpen,
                Function<? super Open, ? extends ObservableSource<? extends Close>> bufferClose,
                Callable<C> bufferSupplier
        ) {
            this.actual = actual;
            this.bufferSupplier = bufferSupplier;
            this.bufferOpen = bufferOpen;
            this.bufferClose = bufferClose;
            this.queue = new SpscLinkedArrayQueue<C>(bufferSize());
            this.observers = new CompositeDisposable();
            this.upstream = new AtomicReference<Disposable>();
            this.buffers = new LinkedHashMap<Long, C>();
            this.errors = new AtomicThrowable();
        }

        @Override
        public void onSubscribe(Disposable s) {
            if (DisposableHelper.setOnce(this.upstream, s)) {

                BufferOpenObserver<Open> open = new BufferOpenObserver<Open>(this);
                observers.add(open);

                bufferOpen.subscribe(open);
            }
        }

        @Override
        public void onNext(T t) {
            synchronized (this) {
                Map<Long, C> bufs = buffers;
                if (bufs == null) {
                    return;
                }
                for (C b : bufs.values()) {
                    b.add(t);
                }
            }
        }

        @Override
        public void onError(Throwable t) {
            if (errors.addThrowable(t)) {
                observers.dispose();
                synchronized (this) {
                    buffers = null;
                }
                done = true;
                drain();
            } else {
                RxJavaPlugins.onError(t);
            }
        }

        @Override
        public void onComplete() {
            observers.dispose();
            synchronized (this) {
                Map<Long, C> bufs = buffers;
                if (bufs == null) {
                    return;
                }
                for (C b : bufs.values()) {
                    queue.offer(b);
                }
                buffers = null;
            }
            done = true;
            drain();
        }

        @Override
        public void dispose() {
            if (DisposableHelper.dispose(upstream)) {
                cancelled = true;
                observers.dispose();
                synchronized (this) {
                    buffers = null;
                }
                if (getAndIncrement() != 0) {
                    queue.clear();
                }
            }
        }

        @Override
        public boolean isDisposed() {
            return DisposableHelper.isDisposed(upstream.get());
        }

        void open(Open token) {
            ObservableSource<? extends Close> p;
            C buf;
            try {
                buf = ObjectHelper.requireNonNull(bufferSupplier.call(), "The bufferSupplier returned a null Collection");
                p = ObjectHelper.requireNonNull(bufferClose.apply(token), "The bufferClose returned a null ObservableSource");
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                DisposableHelper.dispose(upstream);
                onError(ex);
                return;
            }

            long idx = index;
            index = idx + 1;
            synchronized (this) {
                Map<Long, C> bufs = buffers;
                if (bufs == null) {
                    return;
                }
                bufs.put(idx, buf);
            }

            BufferCloseObserver<T, C> bc = new BufferCloseObserver<T, C>(this, idx);
            observers.add(bc);
            p.subscribe(bc);
        }

        void openComplete(BufferOpenObserver<Open> os) {
            observers.delete(os);
            if (observers.size() == 0) {
                DisposableHelper.dispose(upstream);
                done = true;
                drain();
            }
        }

        void close(BufferCloseObserver<T, C> closer, long idx) {
            observers.delete(closer);
            boolean makeDone = false;
            if (observers.size() == 0) {
                makeDone = true;
                DisposableHelper.dispose(upstream);
            }
            synchronized (this) {
                Map<Long, C> bufs = buffers;
                if (bufs == null) {
                    return;
                }
                queue.offer(buffers.remove(idx));
            }
            if (makeDone) {
                done = true;
            }
            drain();
        }

        void boundaryError(Disposable observer, Throwable ex) {
            DisposableHelper.dispose(upstream);
            observers.delete(observer);
            onError(ex);
        }

        void drain() {
            if (getAndIncrement() != 0) {
                return;
            }

            int missed = 1;
            Observer<? super C> a = actual;
            SpscLinkedArrayQueue<C> q = queue;

            for (;;) {
                for (;;) {
                    if (cancelled) {
                        q.clear();
                        return;
                    }

                    boolean d = done;
                    if (d && errors.get() != null) {
                        q.clear();
                        Throwable ex = errors.terminate();
                        a.onError(ex);
                        return;
                    }

                    C v = q.poll();
                    boolean empty = v == null;

                    if (d && empty) {
                        a.onComplete();
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

        static final class BufferOpenObserver<Open>
        extends AtomicReference<Disposable>
        implements Observer<Open>, Disposable {

            private static final long serialVersionUID = -8498650778633225126L;

            final BufferBoundaryObserver<?, ?, Open, ?> parent;

            BufferOpenObserver(BufferBoundaryObserver<?, ?, Open, ?> parent) {
                this.parent = parent;
            }

            @Override
            public void onSubscribe(Disposable s) {
                DisposableHelper.setOnce(this, s);
            }

            @Override
            public void onNext(Open t) {
                parent.open(t);
            }

            @Override
            public void onError(Throwable t) {
                lazySet(DisposableHelper.DISPOSED);
                parent.boundaryError(this, t);
            }

            @Override
            public void onComplete() {
                lazySet(DisposableHelper.DISPOSED);
                parent.openComplete(this);
            }

            @Override
            public void dispose() {
                DisposableHelper.dispose(this);
            }

            @Override
            public boolean isDisposed() {
                return get() == DisposableHelper.DISPOSED;
            }
        }
    }

    static final class BufferCloseObserver<T, C extends Collection<? super T>>
    extends AtomicReference<Disposable>
    implements Observer<Object>, Disposable {

        private static final long serialVersionUID = -8498650778633225126L;

        final BufferBoundaryObserver<T, C, ?, ?> parent;

        final long index;

        BufferCloseObserver(BufferBoundaryObserver<T, C, ?, ?> parent, long index) {
            this.parent = parent;
            this.index = index;
        }

        @Override
        public void onSubscribe(Disposable s) {
            DisposableHelper.setOnce(this, s);
        }

        @Override
        public void onNext(Object t) {
            Disposable s = get();
            if (s != DisposableHelper.DISPOSED) {
                lazySet(DisposableHelper.DISPOSED);
                s.dispose();
                parent.close(this, index);
            }
        }

        @Override
        public void onError(Throwable t) {
            if (get() != DisposableHelper.DISPOSED) {
                lazySet(DisposableHelper.DISPOSED);
                parent.boundaryError(this, t);
            } else {
                RxJavaPlugins.onError(t);
            }
        }

        @Override
        public void onComplete() {
            if (get() != DisposableHelper.DISPOSED) {
                lazySet(DisposableHelper.DISPOSED);
                parent.close(this, index);
            }
        }

        @Override
        public void dispose() {
            DisposableHelper.dispose(this);
        }

        @Override
        public boolean isDisposed() {
            return get() == DisposableHelper.DISPOSED;
        }
    }
}
