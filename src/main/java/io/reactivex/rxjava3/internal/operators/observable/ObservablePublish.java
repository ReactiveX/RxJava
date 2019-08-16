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

import java.util.concurrent.atomic.*;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.exceptions.Exceptions;
import io.reactivex.rxjava3.functions.Consumer;
import io.reactivex.rxjava3.internal.disposables.DisposableHelper;
import io.reactivex.rxjava3.internal.fuseable.HasUpstreamObservableSource;
import io.reactivex.rxjava3.internal.util.ExceptionHelper;
import io.reactivex.rxjava3.observables.ConnectableObservable;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;

/**
 * Shares a single underlying connection to the upstream ObservableSource
 * and multicasts events to all subscribed observers until the upstream
 * completes or the connection is disposed.
 * <p>
 * The difference to ObservablePublish is that when the upstream terminates,
 * late observers will receive that terminal event until the connection is
 * disposed and the ConnectableObservable is reset to its fresh state.
 *
 * @param <T> the element type
 * @since 2.2.10
 */
public final class ObservablePublish<T> extends ConnectableObservable<T>
implements HasUpstreamObservableSource<T> {

    final ObservableSource<T> source;

    final AtomicReference<PublishConnection<T>> current;

    public ObservablePublish(ObservableSource<T> source) {
        this.source = source;
        this.current = new AtomicReference<PublishConnection<T>>();
    }

    @Override
    public void connect(Consumer<? super Disposable> connection) {
        boolean doConnect = false;
        PublishConnection<T> conn;

        for (;;) {
            conn = current.get();

            if (conn == null || conn.isDisposed()) {
                PublishConnection<T> fresh = new PublishConnection<T>(current);
                if (!current.compareAndSet(conn, fresh)) {
                    continue;
                }
                conn = fresh;
            }

            doConnect = !conn.connect.get() && conn.connect.compareAndSet(false, true);
            break;
        }

        try {
            connection.accept(conn);
        } catch (Throwable ex) {
            Exceptions.throwIfFatal(ex);
            throw ExceptionHelper.wrapOrThrow(ex);
        }

        if (doConnect) {
            source.subscribe(conn);
        }
    }

    @Override
    protected void subscribeActual(Observer<? super T> observer) {
        PublishConnection<T> conn;

        for (;;) {
            conn = current.get();
            // we don't create a fresh connection if the current is terminated
            if (conn == null) {
                PublishConnection<T> fresh = new PublishConnection<T>(current);
                if (!current.compareAndSet(conn, fresh)) {
                    continue;
                }
                conn = fresh;
            }
            break;
        }

        InnerDisposable<T> inner = new InnerDisposable<T>(observer, conn);
        observer.onSubscribe(inner);
        if (conn.add(inner)) {
            if (inner.isDisposed()) {
                conn.remove(inner);
            }
            return;
        }
        // Late observers will be simply terminated
        Throwable error = conn.error;
        if (error != null) {
            observer.onError(error);
        } else {
            observer.onComplete();
        }
    }

    @Override
    public void reset() {
        PublishConnection<T> conn = current.get();
        if (conn != null && conn.isDisposed()) {
            current.compareAndSet(conn, null);
        }
    }

    @Override
    public ObservableSource<T> source() {
        return source;
    }

    static final class PublishConnection<T>
    extends AtomicReference<InnerDisposable<T>[]>
    implements Observer<T>, Disposable {

        private static final long serialVersionUID = -3251430252873581268L;

        final AtomicBoolean connect;

        final AtomicReference<PublishConnection<T>> current;

        final AtomicReference<Disposable> upstream;

        @SuppressWarnings("rawtypes")
        static final InnerDisposable[] EMPTY = new InnerDisposable[0];

        @SuppressWarnings("rawtypes")
        static final InnerDisposable[] TERMINATED = new InnerDisposable[0];

        Throwable error;

        @SuppressWarnings("unchecked")
        PublishConnection(AtomicReference<PublishConnection<T>> current) {
            this.connect = new AtomicBoolean();
            this.current = current;
            this.upstream = new AtomicReference<Disposable>();
            lazySet(EMPTY);
        }

        @SuppressWarnings("unchecked")
        @Override
        public void dispose() {
            getAndSet(TERMINATED);
            current.compareAndSet(this, null);
            DisposableHelper.dispose(upstream);
        }

        @Override
        public boolean isDisposed() {
            return get() == TERMINATED;
        }

        @Override
        public void onSubscribe(Disposable d) {
            DisposableHelper.setOnce(upstream, d);
        }

        @Override
        public void onNext(T t) {
            for (InnerDisposable<T> inner : get()) {
                inner.downstream.onNext(t);
            }
        }

        @Override
        @SuppressWarnings("unchecked")
        public void onError(Throwable e) {
            if (upstream.get() != DisposableHelper.DISPOSED) {
                error = e;
                upstream.lazySet(DisposableHelper.DISPOSED);
                for (InnerDisposable<T> inner : getAndSet(TERMINATED)) {
                    inner.downstream.onError(e);
                }
            } else {
                RxJavaPlugins.onError(e);
            }
        }

        @Override
        @SuppressWarnings("unchecked")
        public void onComplete() {
            upstream.lazySet(DisposableHelper.DISPOSED);
            for (InnerDisposable<T> inner : getAndSet(TERMINATED)) {
                inner.downstream.onComplete();
            }
        }

        public boolean add(InnerDisposable<T> inner) {
            for (;;) {
                InnerDisposable<T>[] a = get();
                if (a == TERMINATED) {
                    return false;
                }
                int n = a.length;
                @SuppressWarnings("unchecked")
                InnerDisposable<T>[] b = new InnerDisposable[n + 1];
                System.arraycopy(a, 0, b, 0, n);
                b[n] = inner;
                if (compareAndSet(a, b)) {
                    return true;
                }
            }
        }

        @SuppressWarnings("unchecked")
        public void remove(InnerDisposable<T> inner) {
            for (;;) {
                InnerDisposable<T>[] a = get();
                int n = a.length;
                if (n == 0) {
                    return;
                }

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
                InnerDisposable<T>[] b = EMPTY;
                if (n != 1) {
                    b = new InnerDisposable[n - 1];
                    System.arraycopy(a, 0, b, 0, j);
                    System.arraycopy(a, j + 1, b, j, n - j - 1);
                }
                if (compareAndSet(a, b)) {
                    return;
                }
            }
        }
    }

    /**
     * Intercepts the dispose signal from the downstream and
     * removes itself from the connection's observers array
     * at most once.
     * @param <T> the element type
     */
    static final class InnerDisposable<T>
    extends AtomicReference<PublishConnection<T>>
    implements Disposable {

        private static final long serialVersionUID = 7463222674719692880L;

        final Observer<? super T> downstream;

        InnerDisposable(Observer<? super T> downstream, PublishConnection<T> parent) {
            this.downstream = downstream;
            lazySet(parent);
        }

        @Override
        public void dispose() {
            PublishConnection<T> p = getAndSet(null);
            if (p != null) {
                p.remove(this);
            }
        }

        @Override
        public boolean isDisposed() {
            return get() == null;
        }
    }
}

