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

import java.util.*;
import java.util.concurrent.atomic.*;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableSource;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.disposables.*;
import io.reactivex.rxjava3.exceptions.Exceptions;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.internal.disposables.DisposableHelper;
import io.reactivex.rxjava3.internal.fuseable.SimplePlainQueue;
import io.reactivex.rxjava3.internal.queue.MpscLinkedQueue;
import io.reactivex.rxjava3.internal.util.*;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.subjects.UnicastSubject;

public final class ObservableWindowBoundarySelector<T, B, V> extends AbstractObservableWithUpstream<T, Observable<T>> {
    final ObservableSource<B> open;
    final Function<? super B, ? extends ObservableSource<V>> closingIndicator;
    final int bufferSize;

    public ObservableWindowBoundarySelector(
            ObservableSource<T> source,
            ObservableSource<B> open, Function<? super B, ? extends ObservableSource<V>> closingIndicator,
            int bufferSize) {
        super(source);
        this.open = open;
        this.closingIndicator = closingIndicator;
        this.bufferSize = bufferSize;
    }

    @Override
    public void subscribeActual(Observer<? super Observable<T>> t) {
        source.subscribe(new WindowBoundaryMainObserver<T, B, V>(
                t, open, closingIndicator, bufferSize));
    }

    static final class WindowBoundaryMainObserver<T, B, V>
    extends AtomicInteger
    implements Observer<T>, Disposable, Runnable {
        private static final long serialVersionUID = 8646217640096099753L;

        final Observer<? super Observable<T>> downstream;
        final ObservableSource<B> open;
        final Function<? super B, ? extends ObservableSource<V>> closingIndicator;
        final int bufferSize;
        final CompositeDisposable resources;

        final WindowStartObserver<B> startObserver;

        final List<UnicastSubject<T>> windows;

        final SimplePlainQueue<Object> queue;

        final AtomicLong windowCount;

        final AtomicBoolean downstreamDisposed;

        final AtomicLong requested;
        long emitted;

        volatile boolean upstreamCanceled;

        volatile boolean upstreamDone;
        volatile boolean openDone;
        final AtomicThrowable error;

        Disposable upstream;

        WindowBoundaryMainObserver(Observer<? super Observable<T>> downstream,
                ObservableSource<B> open, Function<? super B, ? extends ObservableSource<V>> closingIndicator, int bufferSize) {
            this.downstream = downstream;
            this.queue = new MpscLinkedQueue<Object>();
            this.open = open;
            this.closingIndicator = closingIndicator;
            this.bufferSize = bufferSize;
            this.resources = new CompositeDisposable();
            this.windows = new ArrayList<UnicastSubject<T>>();
            this.windowCount = new AtomicLong(1L);
            this.downstreamDisposed = new AtomicBoolean();
            this.error = new AtomicThrowable();
            this.startObserver = new WindowStartObserver<B>(this);
            this.requested = new AtomicLong();
        }

        @Override
        public void onSubscribe(Disposable d) {
            if (DisposableHelper.validate(this.upstream, d)) {
                this.upstream = d;

                downstream.onSubscribe(this);

                open.subscribe(startObserver);
            }
        }

        @Override
        public void onNext(T t) {
            queue.offer(t);
            drain();
        }

        @Override
        public void onError(Throwable t) {
            startObserver.dispose();
            resources.dispose();
            if (error.tryAddThrowableOrReport(t)) {
                upstreamDone = true;
                drain();
            }
        }

        @Override
        public void onComplete() {
            startObserver.dispose();
            resources.dispose();
            upstreamDone = true;
            drain();
        }

        @Override
        public void dispose() {
            if (downstreamDisposed.compareAndSet(false, true)) {
                if (windowCount.decrementAndGet() == 0) {
                    upstream.dispose();
                    startObserver.dispose();
                    resources.dispose();
                    error.tryTerminateAndReport();
                    upstreamCanceled = true;
                    drain();
                } else {
                    startObserver.dispose();
                }
            }
        }

        @Override
        public boolean isDisposed() {
            return downstreamDisposed.get();
        }

        @Override
        public void run() {
            if (windowCount.decrementAndGet() == 0) {
                upstream.dispose();
                startObserver.dispose();
                resources.dispose();
                error.tryTerminateAndReport();
                upstreamCanceled = true;
                drain();
            }
        }

        void open(B startValue) {
            queue.offer(new WindowStartItem<B>(startValue));
            drain();
        }

        void openError(Throwable t) {
            upstream.dispose();
            resources.dispose();
            if (error.tryAddThrowableOrReport(t)) {
                upstreamDone = true;
                drain();
            }
        }

        void openComplete() {
            openDone = true;
            drain();
        }

        void close(WindowEndObserverIntercept<T, V> what) {
            queue.offer(what);
            drain();
        }

        void closeError(Throwable t) {
            upstream.dispose();
            startObserver.dispose();
            resources.dispose();
            if (error.tryAddThrowableOrReport(t)) {
                upstreamDone = true;
                drain();
            }
        }

        void drain() {
            if (getAndIncrement() != 0) {
                return;
            }

            int missed = 1;
            final Observer<? super Observable<T>> downstream = this.downstream;
            final SimplePlainQueue<Object> queue = this.queue;
            final List<UnicastSubject<T>> windows = this.windows;

            for (;;) {
                if (upstreamCanceled) {
                    queue.clear();
                    windows.clear();
                } else {
                    boolean isDone = upstreamDone;
                    Object o = queue.poll();
                    boolean isEmpty = o == null;

                    if (isDone) {
                        if (isEmpty || error.get() != null) {
                            terminateDownstream(downstream);
                            upstreamCanceled = true;
                            continue;
                        }
                    }

                    if (!isEmpty) {
                        if (o instanceof WindowStartItem) {
                            if (!downstreamDisposed.get()) {
                                @SuppressWarnings("unchecked")
                                B startItem = ((WindowStartItem<B>)o).item;

                                ObservableSource<V> endSource;
                                try {
                                    endSource = Objects.requireNonNull(closingIndicator.apply(startItem), "The closingIndicator returned a null ObservableSource");
                                } catch (Throwable ex) {
                                    upstream.dispose();
                                    startObserver.dispose();
                                    resources.dispose();
                                    Exceptions.throwIfFatal(ex);
                                    error.tryAddThrowableOrReport(ex);
                                    upstreamDone = true;
                                    continue;
                                }

                                windowCount.getAndIncrement();
                                UnicastSubject<T> newWindow = UnicastSubject.create(bufferSize, this);
                                WindowEndObserverIntercept<T, V> endObserver = new WindowEndObserverIntercept<T, V>(this, newWindow);

                                downstream.onNext(endObserver);

                                if (endObserver.tryAbandon()) {
                                    newWindow.onComplete();
                                } else {
                                    windows.add(newWindow);
                                    resources.add(endObserver);
                                    endSource.subscribe(endObserver);
                                }
                            }
                        }
                        else if (o instanceof WindowEndObserverIntercept) {
                            @SuppressWarnings("unchecked")
                            UnicastSubject<T> w = ((WindowEndObserverIntercept<T, V>)o).window;

                            windows.remove(w);
                            resources.delete((Disposable)o);
                            w.onComplete();
                        } else {
                            @SuppressWarnings("unchecked")
                            T item = (T)o;

                            for (UnicastSubject<T> w : windows) {
                                w.onNext(item);
                            }
                        }

                        continue;
                    }
                    else if (openDone && windows.size() == 0) {
                        upstream.dispose();
                        startObserver.dispose();
                        resources.dispose();
                        terminateDownstream(downstream);
                        upstreamCanceled = true;
                        continue;
                    }
                }

                missed = addAndGet(-missed);
                if (missed == 0) {
                    break;
                }
            }
        }

        void terminateDownstream(Observer<?> downstream) {
            Throwable ex = error.terminate();
            if (ex == null) {
                for (UnicastSubject<T> w : windows) {
                    w.onComplete();
                }
                downstream.onComplete();
            } else if (ex != ExceptionHelper.TERMINATED) {
                for (UnicastSubject<T> w : windows) {
                    w.onError(ex);
                }
                downstream.onError(ex);
            }
        }

        static final class WindowStartItem<B> {

            final B item;

            WindowStartItem(B item) {
                this.item = item;
            }
        }

        static final class WindowStartObserver<B> extends AtomicReference<Disposable>
        implements Observer<B> {

            private static final long serialVersionUID = -3326496781427702834L;

            final WindowBoundaryMainObserver<?, B, ?> parent;

            WindowStartObserver(WindowBoundaryMainObserver<?, B, ?> parent) {
                this.parent = parent;
            }

            @Override
            public void onSubscribe(Disposable d) {
                DisposableHelper.setOnce(this, d);
            }

            @Override
            public void onNext(B t) {
                parent.open(t);
            }

            @Override
            public void onError(Throwable t) {
                parent.openError(t);
            }

            @Override
            public void onComplete() {
                parent.openComplete();
            }

            void dispose() {
                DisposableHelper.dispose(this);
            }
        }

        static final class WindowEndObserverIntercept<T, V> extends Observable<T>
        implements Observer<V>, Disposable {

            final WindowBoundaryMainObserver<T, ?, V> parent;

            final UnicastSubject<T> window;

            final AtomicReference<Disposable> upstream;

            final AtomicBoolean once;

            WindowEndObserverIntercept(WindowBoundaryMainObserver<T, ?, V> parent, UnicastSubject<T> window) {
                this.parent = parent;
                this.window = window;
                this.upstream = new AtomicReference<Disposable>();
                this.once = new AtomicBoolean();
            }

            @Override
            public void onSubscribe(Disposable d) {
                DisposableHelper.setOnce(upstream, d);
            }

            @Override
            public void onNext(V t) {
                if (DisposableHelper.dispose(upstream)) {
                    parent.close(this);
                }
            }

            @Override
            public void onError(Throwable t) {
                if (isDisposed()) {
                    RxJavaPlugins.onError(t);
                } else {
                    parent.closeError(t);
                }
            }

            @Override
            public void onComplete() {
                parent.close(this);
            }

            @Override
            public void dispose() {
                DisposableHelper.dispose(upstream);
            }

            @Override
            public boolean isDisposed() {
                return upstream.get() == DisposableHelper.DISPOSED;
            }

            @Override
            protected void subscribeActual(Observer<? super T> o) {
                window.subscribe(o);
                once.set(true);
            }

            boolean tryAbandon() {
                return !once.get() && once.compareAndSet(false, true);
            }
        }
    }
}
