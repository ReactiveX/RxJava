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

package io.reactivex.rxjava3.internal.operators.observable;

import java.util.*;
import java.util.concurrent.atomic.*;

import io.reactivex.rxjava3.core.ObservableSource;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.exceptions.Exceptions;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.internal.disposables.DisposableHelper;
import io.reactivex.rxjava3.internal.util.AtomicThrowable;
import io.reactivex.rxjava3.operators.*;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;

public final class ObservableFlatMap<T, U> extends AbstractObservableWithUpstream<T, U> {
    final Function<? super T, ? extends ObservableSource<? extends U>> mapper;
    final boolean delayErrors;
    final int maxConcurrency;
    final int bufferSize;

    public ObservableFlatMap(ObservableSource<T> source,
            Function<? super T, ? extends ObservableSource<? extends U>> mapper,
            boolean delayErrors, int maxConcurrency, int bufferSize) {
        super(source);
        this.mapper = mapper;
        this.delayErrors = delayErrors;
        this.maxConcurrency = maxConcurrency;
        this.bufferSize = bufferSize;
    }

    @Override
    public void subscribeActual(Observer<? super U> t) {

        if (ObservableScalarXMap.tryScalarXMapSubscribe(source, t, mapper)) {
            return;
        }

        source.subscribe(new MergeObserver<>(t, mapper, delayErrors, maxConcurrency, bufferSize));
    }

    static final class MergeObserver<T, U> extends AtomicInteger implements Disposable, Observer<T> {

        private static final long serialVersionUID = -2117620485640801370L;

        final Observer<? super U> downstream;
        final Function<? super T, ? extends ObservableSource<? extends U>> mapper;
        final boolean delayErrors;
        final int maxConcurrency;
        final int bufferSize;

        volatile SimplePlainQueue<U> queue;

        volatile boolean done;

        final AtomicThrowable errors = new AtomicThrowable();

        volatile boolean disposed;

        final AtomicReference<InnerObserver<?, ?>[]> observers;

        static final InnerObserver<?, ?>[] EMPTY = new InnerObserver<?, ?>[0];

        static final InnerObserver<?, ?>[] CANCELLED = new InnerObserver<?, ?>[0];

        Disposable upstream;

        long uniqueId;
        int lastIndex;

        Queue<ObservableSource<? extends U>> sources;

        int wip;

        MergeObserver(Observer<? super U> actual, Function<? super T, ? extends ObservableSource<? extends U>> mapper,
                boolean delayErrors, int maxConcurrency, int bufferSize) {
            this.downstream = actual;
            this.mapper = mapper;
            this.delayErrors = delayErrors;
            this.maxConcurrency = maxConcurrency;
            this.bufferSize = bufferSize;
            if (maxConcurrency != Integer.MAX_VALUE) {
                sources = new ArrayDeque<>(maxConcurrency);
            }
            this.observers = new AtomicReference<>(EMPTY);
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
            // safeguard against misbehaving sources
            if (done) {
                return;
            }
            ObservableSource<? extends U> p;
            try {
                p = Objects.requireNonNull(mapper.apply(t), "The mapper returned a null ObservableSource");
            } catch (Throwable e) {
                Exceptions.throwIfFatal(e);
                upstream.dispose();
                onError(e);
                return;
            }

            if (maxConcurrency != Integer.MAX_VALUE) {
                synchronized (this) {
                    if (wip == maxConcurrency) {
                        sources.offer(p);
                        return;
                    }
                    wip++;
                }
            }

            subscribeInner(p);
        }

        @SuppressWarnings("unchecked")
        void subscribeInner(ObservableSource<? extends U> p) {
            for (;;) {
                if (p instanceof Supplier) {
                    if (tryEmitScalar(((Supplier<? extends U>)p)) && maxConcurrency != Integer.MAX_VALUE) {
                        boolean empty = false;
                        synchronized (this) {
                            p = sources.poll();
                            if (p == null) {
                                wip--;
                                empty = true;
                            }
                        }
                        if (empty) {
                            drain();
                            break;
                        }
                    } else {
                        break;
                    }
                } else {
                    InnerObserver<T, U> inner = new InnerObserver<>(this, uniqueId++);
                    if (addInner(inner)) {
                        p.subscribe(inner);
                    }
                    break;
                }
            }
        }

        boolean addInner(InnerObserver<T, U> inner) {
            for (;;) {
                InnerObserver<?, ?>[] a = observers.get();
                if (a == CANCELLED) {
                    inner.dispose();
                    return false;
                }
                int n = a.length;
                InnerObserver<?, ?>[] b = new InnerObserver[n + 1];
                System.arraycopy(a, 0, b, 0, n);
                b[n] = inner;
                if (observers.compareAndSet(a, b)) {
                    return true;
                }
            }
        }

        void removeInner(InnerObserver<T, U> inner) {
            for (;;) {
                InnerObserver<?, ?>[] a = observers.get();
                int n = a.length;
                int j = -1;
                for (int i = 0; i < n; i++) {
                    if (a[i] == inner) {
                        j = i;
                        break;
                    }
                }
                if (j < 0) {
                    return;
                }
                InnerObserver<?, ?>[] b;
                if (n == 1) {
                    b = EMPTY;
                } else {
                    b = new InnerObserver<?, ?>[n - 1];
                    System.arraycopy(a, 0, b, 0, j);
                    System.arraycopy(a, j + 1, b, j, n - j - 1);
                }
                if (observers.compareAndSet(a, b)) {
                    return;
                }
            }
        }

        boolean tryEmitScalar(Supplier<? extends U> value) {
            U u;
            try {
                u = value.get();
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                errors.tryAddThrowableOrReport(ex);
                drain();
                return true;
            }

            if (u == null) {
                return true;
            }

            if (get() == 0 && compareAndSet(0, 1)) {
                downstream.onNext(u);
                if (decrementAndGet() == 0) {
                    return true;
                }
            } else {
                SimplePlainQueue<U> q = queue;
                if (q == null) {
                    if (maxConcurrency == Integer.MAX_VALUE) {
                        q = new SpscLinkedArrayQueue<>(bufferSize);
                    } else {
                        q = new SpscArrayQueue<>(maxConcurrency);
                    }
                    queue = q;
                }

                q.offer(u);
                if (getAndIncrement() != 0) {
                    return false;
                }
            }
            drainLoop();
            return true;
        }

        void tryEmit(U value, InnerObserver<T, U> inner) {
            if (get() == 0 && compareAndSet(0, 1)) {
                downstream.onNext(value);
                if (decrementAndGet() == 0) {
                    return;
                }
            } else {
                SimpleQueue<U> q = inner.queue;
                if (q == null) {
                    q = new SpscLinkedArrayQueue<>(bufferSize);
                    inner.queue = q;
                }
                q.offer(value);
                if (getAndIncrement() != 0) {
                    return;
                }
            }
            drainLoop();
        }

        @Override
        public void onError(Throwable t) {
            if (done) {
                RxJavaPlugins.onError(t);
                return;
            }
            if (errors.tryAddThrowableOrReport(t)) {
                done = true;
                drain();
            }
        }

        @Override
        public void onComplete() {
            if (done) {
                return;
            }
            done = true;
            drain();
        }

        @Override
        public void dispose() {
            disposed = true;
            if (disposeAll()) {
                errors.tryTerminateAndReport();
            }
        }

        @Override
        public boolean isDisposed() {
            return disposed;
        }

        void drain() {
            if (getAndIncrement() == 0) {
                drainLoop();
            }
        }

        void drainLoop() {
            final Observer<? super U> child = this.downstream;
            int missed = 1;
            for (;;) {
                if (checkTerminate()) {
                    return;
                }
                int innerCompleted = 0;
                SimplePlainQueue<U> svq = queue;

                if (svq != null) {
                    for (;;) {
                        if (checkTerminate()) {
                            return;
                        }

                        U o = svq.poll();

                        if (o == null) {
                            break;
                        }

                        child.onNext(o);
                        innerCompleted++;
                    }
                }

                if (innerCompleted != 0) {
                    if (maxConcurrency != Integer.MAX_VALUE) {
                        subscribeMore(innerCompleted);
                        innerCompleted = 0;
                    }
                    continue;
                }

                boolean d = done;
                svq = queue;
                InnerObserver<?, ?>[] inner = observers.get();
                int n = inner.length;

                int nSources = 0;
                if (maxConcurrency != Integer.MAX_VALUE) {
                    synchronized (this) {
                        nSources = sources.size();
                    }
                }

                if (d && (svq == null || svq.isEmpty()) && n == 0 && nSources == 0) {
                    errors.tryTerminateConsumer(downstream);
                    return;
                }

                if (n != 0) {
                    int j = Math.min(n - 1, lastIndex);

                    sourceLoop:
                    for (int i = 0; i < n; i++) {
                        if (checkTerminate()) {
                            return;
                        }

                        @SuppressWarnings("unchecked")
                        InnerObserver<T, U> is = (InnerObserver<T, U>)inner[j];
                        SimpleQueue<U> q = is.queue;
                        if (q != null) {
                            for (;;) {
                                U o;
                                try {
                                    o = q.poll();
                                } catch (Throwable ex) {
                                    Exceptions.throwIfFatal(ex);
                                    is.dispose();
                                    errors.tryAddThrowableOrReport(ex);
                                    if (checkTerminate()) {
                                        return;
                                    }
                                    removeInner(is);
                                    innerCompleted++;
                                    j++;
                                    if (j == n) {
                                        j = 0;
                                    }
                                    continue sourceLoop;
                                }
                                if (o == null) {
                                    break;
                                }

                                child.onNext(o);

                                if (checkTerminate()) {
                                    return;
                                }
                            }
                        }

                        boolean innerDone = is.done;
                        SimpleQueue<U> innerQueue = is.queue;
                        if (innerDone && (innerQueue == null || innerQueue.isEmpty())) {
                            removeInner(is);
                            innerCompleted++;
                        }

                        j++;
                        if (j == n) {
                            j = 0;
                        }
                    }
                    lastIndex = j;
                }

                if (innerCompleted != 0) {
                    if (maxConcurrency != Integer.MAX_VALUE) {
                        subscribeMore(innerCompleted);
                        innerCompleted = 0;
                    }
                    continue;
                }

                missed = addAndGet(-missed);
                if (missed == 0) {
                    break;
                }
            }
        }

        void subscribeMore(int innerCompleted) {
            while (innerCompleted-- != 0) {
                ObservableSource<? extends U> p;
                synchronized (this) {
                    p = sources.poll();
                    if (p == null) {
                        wip--;
                        continue;
                    }
                }
                subscribeInner(p);
            }
        }

        boolean checkTerminate() {
            if (disposed) {
                return true;
            }
            Throwable e = errors.get();
            if (!delayErrors && (e != null)) {
                disposeAll();
                errors.tryTerminateConsumer(downstream);
                return true;
            }
            return false;
        }

        boolean disposeAll() {
            upstream.dispose();
            InnerObserver<?, ?>[] a = observers.getAndSet(CANCELLED);
            if (a != CANCELLED) {
                for (InnerObserver<?, ?> inner : a) {
                    inner.dispose();
                }
                return true;
            }
            return false;
        }
    }

    static final class InnerObserver<T, U> extends AtomicReference<Disposable>
    implements Observer<U> {

        private static final long serialVersionUID = -4606175640614850599L;
        final long id;
        final MergeObserver<T, U> parent;

        volatile boolean done;
        volatile SimpleQueue<U> queue;

        int fusionMode;

        InnerObserver(MergeObserver<T, U> parent, long id) {
            this.id = id;
            this.parent = parent;
        }

        @Override
        public void onSubscribe(Disposable d) {
            if (DisposableHelper.setOnce(this, d)) {
                if (d instanceof QueueDisposable) {
                    @SuppressWarnings("unchecked")
                    QueueDisposable<U> qd = (QueueDisposable<U>) d;

                    int m = qd.requestFusion(QueueDisposable.ANY | QueueDisposable.BOUNDARY);
                    if (m == QueueDisposable.SYNC) {
                        fusionMode = m;
                        queue = qd;
                        done = true;
                        parent.drain();
                        return;
                    }
                    if (m == QueueDisposable.ASYNC) {
                        fusionMode = m;
                        queue = qd;
                    }
                }
            }
        }

        @Override
        public void onNext(U t) {
            if (fusionMode == QueueDisposable.NONE) {
                parent.tryEmit(t, this);
            } else {
                parent.drain();
            }
        }

        @Override
        public void onError(Throwable t) {
            if (parent.errors.tryAddThrowableOrReport(t)) {
                if (!parent.delayErrors) {
                    parent.disposeAll();
                }
                done = true;
                parent.drain();
            }
        }

        @Override
        public void onComplete() {
            done = true;
            parent.drain();
        }

        public void dispose() {
            DisposableHelper.dispose(this);
        }
    }
}
