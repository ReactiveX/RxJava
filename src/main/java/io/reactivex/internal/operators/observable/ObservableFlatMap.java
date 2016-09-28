/**
 * Copyright 2016 Netflix, Inc.
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

import io.reactivex.plugins.RxJavaPlugins;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.*;

import io.reactivex.ObservableSource;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.*;
import io.reactivex.functions.Function;
import io.reactivex.internal.disposables.*;
import io.reactivex.internal.fuseable.*;
import io.reactivex.internal.queue.*;

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

        source.subscribe(new MergeObserver<T, U>(t, mapper, delayErrors, maxConcurrency, bufferSize));
    }

    static final class MergeObserver<T, U> extends AtomicInteger implements Disposable, Observer<T> {

        private static final long serialVersionUID = -2117620485640801370L;

        final Observer<? super U> actual;
        final Function<? super T, ? extends ObservableSource<? extends U>> mapper;
        final boolean delayErrors;
        final int maxConcurrency;
        final int bufferSize;

        volatile SimpleQueue<U> queue;

        volatile boolean done;

        final AtomicReference<SimpleQueue<Throwable>> errors = new AtomicReference<SimpleQueue<Throwable>>();

        static final SimpleQueue<Throwable> ERRORS_CLOSED = new RejectingQueue<Throwable>();

        volatile boolean cancelled;

        final AtomicReference<InnerObserver<?, ?>[]> observers;

        static final InnerObserver<?, ?>[] EMPTY = new InnerObserver<?, ?>[0];

        static final InnerObserver<?, ?>[] CANCELLED = new InnerObserver<?, ?>[0];

        Disposable s;

        long uniqueId;
        long lastId;
        int lastIndex;

        Queue<ObservableSource<? extends U>> sources;

        int wip;

        MergeObserver(Observer<? super U> actual, Function<? super T, ? extends ObservableSource<? extends U>> mapper,
                boolean delayErrors, int maxConcurrency, int bufferSize) {
            this.actual = actual;
            this.mapper = mapper;
            this.delayErrors = delayErrors;
            this.maxConcurrency = maxConcurrency;
            this.bufferSize = bufferSize;
            if (maxConcurrency != Integer.MAX_VALUE) {
                sources = new ArrayDeque<ObservableSource<? extends U>>(maxConcurrency);
            }
            this.observers = new AtomicReference<InnerObserver<?, ?>[]>(EMPTY);
        }

        @Override
        public void onSubscribe(Disposable s) {
            if (DisposableHelper.validate(this.s, s)) {
                this.s = s;
                actual.onSubscribe(this);
            }
        }

        @SuppressWarnings("unchecked")
        @Override
        public void onNext(T t) {
            // safeguard against misbehaving sources
            if (done) {
                return;
            }
            ObservableSource<? extends U> p;
            try {
                p = mapper.apply(t);
            } catch (Throwable e) {
                Exceptions.throwIfFatal(e);
                onError(e);
                return;
            }
            if (p instanceof Callable) {
                tryEmitScalar(((Callable<? extends U>)p));
            } else {
                if (maxConcurrency == Integer.MAX_VALUE) {
                    subscribeInner(p);
                } else {
                    synchronized (this) {
                        if (wip == maxConcurrency) {
                            sources.offer(p);
                            return;
                        }
                        wip++;
                    }
                    subscribeInner(p);
                }
            }
        }

        void subscribeInner(ObservableSource<? extends U> p) {
            InnerObserver<T, U> inner = new InnerObserver<T, U>(this, uniqueId++);
            addInner(inner);
            p.subscribe(inner);
        }

        void addInner(InnerObserver<T, U> inner) {
            for (;;) {
                InnerObserver<?, ?>[] a = observers.get();
                if (a == CANCELLED) {
                    inner.dispose();
                    return;
                }
                int n = a.length;
                InnerObserver<?, ?>[] b = new InnerObserver[n + 1];
                System.arraycopy(a, 0, b, 0, n);
                b[n] = inner;
                if (observers.compareAndSet(a, b)) {
                    return;
                }
            }
        }

        void removeInner(InnerObserver<T, U> inner) {
            for (;;) {
                InnerObserver<?, ?>[] a = observers.get();
                if (a == CANCELLED || a == EMPTY) {
                    return;
                }
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

        SimpleQueue<U> getMainQueue() {
            SimpleQueue<U> q = queue;
            if (q == null) {
                if (maxConcurrency == Integer.MAX_VALUE) {
                    q = new SpscLinkedArrayQueue<U>(bufferSize);
                } else {
                    q = new SpscArrayQueue<U>(maxConcurrency);
                }
                queue = q;
            }
            return q;
        }

        void tryEmitScalar(Callable<? extends U> value) {
            U u;
            try {
                u = value.call();
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                getErrorQueue().offer(ex);
                drain();
                return;
            }

            if (u == null) {
                return;
            }


            if (get() == 0 && compareAndSet(0, 1)) {
                actual.onNext(u);
                if (decrementAndGet() == 0) {
                    return;
                }
            } else {
                SimpleQueue<U> q = getMainQueue();
                if (!q.offer(u)) {
                    onError(new IllegalStateException("Scalar queue full?!"));
                    return;
                }
                if (getAndIncrement() != 0) {
                    return;
                }
            }
            drainLoop();
        }

        SimpleQueue<U> getInnerQueue(InnerObserver<T, U> inner) {
            SimpleQueue<U> q = inner.queue;
            if (q == null) {
                q = new SpscArrayQueue<U>(bufferSize);
                inner.queue = q;
            }
            return q;
        }

        void tryEmit(U value, InnerObserver<T, U> inner) {
            if (get() == 0 && compareAndSet(0, 1)) {
                actual.onNext(value);
                if (decrementAndGet() == 0) {
                    return;
                }
            } else {
                SimpleQueue<U> q = inner.queue;
                if (q == null) {
                    q = new SpscLinkedArrayQueue<U>(bufferSize);
                    inner.queue = q;
                }
                if (!q.offer(value)) {
                    onError(new MissingBackpressureException("Inner queue full?!"));
                    return;
                }
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
            getErrorQueue().offer(t);
            done = true;
            drain();
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
            if (!cancelled) {
                cancelled = true;
                if (getAndIncrement() == 0) {
                    s.dispose();
                    disposeAll();
                }
            }
        }

        @Override
        public boolean isDisposed() {
            return cancelled;
        }

        void drain() {
            if (getAndIncrement() == 0) {
                drainLoop();
            }
        }

        void drainLoop() {
            final Observer<? super U> child = this.actual;
            int missed = 1;
            for (;;) {
                if (checkTerminate()) {
                    return;
                }
                SimpleQueue<U> svq = queue;

                if (svq != null) {
                    for (;;) {
                        U o;
                        for (;;) {
                            if (checkTerminate()) {
                                return;
                            }
                            try {
                                o = svq.poll();
                            } catch (Throwable ex) {
                                Exceptions.throwIfFatal(ex);
                                getErrorQueue().offer(ex);
                                continue;
                            }
                            if (o == null) {
                                break;
                            }

                            child.onNext(o);
                        }
                        if (o == null) {
                            break;
                        }
                    }
                }

                boolean d = done;
                svq = queue;
                InnerObserver<?, ?>[] inner = observers.get();
                int n = inner.length;

                if (d && (svq == null || svq.isEmpty()) && n == 0) {
                    SimpleQueue<Throwable> e = errors.get();
                    if (e == null || e.isEmpty()) {
                        child.onComplete();
                    } else {
                        reportError(e);
                    }
                    return;
                }

                boolean innerCompleted = false;
                if (n != 0) {
                    long startId = lastId;
                    int index = lastIndex;

                    if (n <= index || inner[index].id != startId) {
                        if (n <= index) {
                            index = 0;
                        }
                        int j = index;
                        for (int i = 0; i < n; i++) {
                            if (inner[j].id == startId) {
                                break;
                            }
                            j++;
                            if (j == n) {
                                j = 0;
                            }
                        }
                        index = j;
                        lastIndex = j;
                        lastId = inner[j].id;
                    }

                    int j = index;
                    for (int i = 0; i < n; i++) {
                        if (checkTerminate()) {
                            return;
                        }
                        @SuppressWarnings("unchecked")
                        InnerObserver<T, U> is = (InnerObserver<T, U>)inner[j];

                        U o = null;
                        for (;;) {
                            for (;;) {
                                if (checkTerminate()) {
                                    return;
                                }
                                SimpleQueue<U> q = is.queue;
                                if (q == null) {
                                    break;
                                }
                                try {
                                    o = q.poll();
                                } catch (Throwable ex) {
                                    Exceptions.throwIfFatal(ex);
                                    getErrorQueue().offer(ex);
                                    continue;
                                }
                                if (o == null) {
                                    break;
                                }

                                child.onNext(o);
                            }
                            if (o == null) {
                                break;
                            }
                        }
                        boolean innerDone = is.done;
                        SimpleQueue<U> innerQueue = is.queue;
                        if (innerDone && (innerQueue == null || innerQueue.isEmpty())) {
                            removeInner(is);
                            if (checkTerminate()) {
                                return;
                            }
                            innerCompleted = true;
                        }

                        j++;
                        if (j == n) {
                            j = 0;
                        }
                    }
                    lastIndex = j;
                    lastId = inner[j].id;
                }

                if (innerCompleted) {
                    if (maxConcurrency != Integer.MAX_VALUE) {
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
                    continue;
                }
                missed = addAndGet(-missed);
                if (missed == 0) {
                    break;
                }
            }
        }

        boolean checkTerminate() {
            if (cancelled) {
                s.dispose();
                disposeAll();
                return true;
            }
            SimpleQueue<Throwable> e = errors.get();
            if (!delayErrors && (e != null && !e.isEmpty())) {
                try {
                    reportError(e);
                } finally {
                    disposeAll();
                }
                return true;
            }
            return false;
        }

        void reportError(SimpleQueue<Throwable> q) {
            List<Throwable> composite = null;
            Throwable ex = null;

            for (;;) {
                Throwable t;
                try {
                    t = q.poll();
                } catch (Throwable exc) {
                    Exceptions.throwIfFatal(exc);
                    if (ex == null) {
                        ex = exc;
                    } else {
                        if (composite == null) {
                            composite = new ArrayList<Throwable>();
                            composite.add(ex);
                        }
                        composite.add(exc);
                    }
                    break;
                }

                if (t == null) {
                    break;
                }
                if (ex == null) {
                    ex = t;
                } else {
                    if (composite == null) {
                        composite = new ArrayList<Throwable>();
                        composite.add(ex);
                    }
                    composite.add(t);
                }
            }
            if (composite != null) {
                actual.onError(new CompositeException(composite));
            } else {
                actual.onError(ex);
            }
        }

        void disposeAll() {
            InnerObserver<?, ?>[] a = observers.get();
            if (a != CANCELLED) {
                a = observers.getAndSet(CANCELLED);
                if (a != CANCELLED) {
                    errors.getAndSet(ERRORS_CLOSED);
                    for (InnerObserver<?, ?> inner : a) {
                        inner.dispose();
                    }
                }
            }
        }

        SimpleQueue<Throwable> getErrorQueue() {
            for (;;) {
                SimpleQueue<Throwable> q = errors.get();
                if (q != null) {
                    return q;
                }
                q = new MpscLinkedQueue<Throwable>();
                if (errors.compareAndSet(null, q)) {
                    return q;
                }
            }
        }
    }

    static final class InnerObserver<T, U> extends AtomicReference<Disposable>
    implements Observer<U>, Disposable {

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
        public void onSubscribe(Disposable s) {
            if (DisposableHelper.setOnce(this, s)) {
                if (s instanceof QueueDisposable) {
                    @SuppressWarnings("unchecked")
                    QueueDisposable<U> qd = (QueueDisposable<U>) s;

                    int m = qd.requestFusion(QueueDisposable.ANY);
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
            parent.getErrorQueue().offer(t);
            done = true;
            parent.drain();
        }
        @Override
        public void onComplete() {
            done = true;
            parent.drain();
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

    static final class RejectingQueue<T> implements SimpleQueue<T> {
        @Override
        public boolean offer(T e) {
            return false;
        }

        @Override
        public T poll() {
            return null;
        }

        @Override
        public boolean offer(T v1, T v2) {
            return false;
        }

        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        public void clear() {

        }

    }
}
