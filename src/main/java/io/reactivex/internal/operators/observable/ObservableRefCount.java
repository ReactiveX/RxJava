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

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.*;

import io.reactivex.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.internal.disposables.*;
import io.reactivex.observables.ConnectableObservable;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.schedulers.Schedulers;

/**
 * Returns an observable sequence that stays connected to the source as long as
 * there is at least one subscription to the observable sequence.
 *
 * @param <T>
 *            the value type
 */
public final class ObservableRefCount<T> extends Observable<T> {

    final ConnectableObservable<T> source;

    final int n;

    final long timeout;

    final TimeUnit unit;

    final Scheduler scheduler;

    RefConnection connection;

    public ObservableRefCount(ConnectableObservable<T> source) {
        this(source, 1, 0L, TimeUnit.NANOSECONDS, Schedulers.trampoline());
    }

    public ObservableRefCount(ConnectableObservable<T> source, int n, long timeout, TimeUnit unit,
            Scheduler scheduler) {
        this.source = source;
        this.n = n;
        this.timeout = timeout;
        this.unit = unit;
        this.scheduler = scheduler;
    }

    @Override
    protected void subscribeActual(Observer<? super T> s) {

        RefConnection conn;

        boolean connect = false;
        synchronized (this) {
            conn = connection;
            if (conn == null) {
                conn = new RefConnection(this);
                connection = conn;
            }

            long c = conn.subscriberCount;
            if (c == 0L && conn.timer != null) {
                conn.timer.dispose();
            }
            conn.subscriberCount = c + 1;
            if (!conn.connected && c + 1 == n) {
                connect = true;
                conn.connected = true;
            }
        }

        source.subscribe(new RefCountObserver<T>(s, this, conn));

        if (connect) {
            source.connect(conn);
        }
    }

    void cancel(RefConnection rc) {
        SequentialDisposable sd;
        synchronized (this) {
            if (connection == null) {
                return;
            }
            long c = rc.subscriberCount - 1;
            rc.subscriberCount = c;
            if (c != 0L || !rc.connected) {
                return;
            }
            if (timeout == 0L) {
                timeout(rc);
                return;
            }
            sd = new SequentialDisposable();
            rc.timer = sd;
        }

        sd.replace(scheduler.scheduleDirect(rc, timeout, unit));
    }

    void terminated(RefConnection rc) {
        synchronized (this) {
            if (connection != null) {
                connection = null;
                if (rc.timer != null) {
                    rc.timer.dispose();
                }
                if (source instanceof Disposable) {
                    ((Disposable)source).dispose();
                }
            }
        }
    }

    void timeout(RefConnection rc) {
        synchronized (this) {
            if (rc.subscriberCount == 0 && rc == connection) {
                connection = null;
                DisposableHelper.dispose(rc);
                if (source instanceof Disposable) {
                    ((Disposable)source).dispose();
                }
            }
        }
    }

    static final class RefConnection extends AtomicReference<Disposable>
    implements Runnable, Consumer<Disposable> {

        private static final long serialVersionUID = -4552101107598366241L;

        final ObservableRefCount<?> parent;

        Disposable timer;

        long subscriberCount;

        boolean connected;

        RefConnection(ObservableRefCount<?> parent) {
            this.parent = parent;
        }

        @Override
        public void run() {
            parent.timeout(this);
        }

        @Override
        public void accept(Disposable t) throws Exception {
            DisposableHelper.replace(this, t);
        }
    }

    static final class RefCountObserver<T>
    extends AtomicBoolean implements Observer<T>, Disposable {

        private static final long serialVersionUID = -7419642935409022375L;

        final Observer<? super T> actual;

        final ObservableRefCount<T> parent;

        final RefConnection connection;

        Disposable upstream;

        RefCountObserver(Observer<? super T> actual, ObservableRefCount<T> parent, RefConnection connection) {
            this.actual = actual;
            this.parent = parent;
            this.connection = connection;
        }

        @Override
        public void onNext(T t) {
            actual.onNext(t);
        }

        @Override
        public void onError(Throwable t) {
            if (compareAndSet(false, true)) {
                parent.terminated(connection);
                actual.onError(t);
            } else {
                RxJavaPlugins.onError(t);
            }
        }

        @Override
        public void onComplete() {
            if (compareAndSet(false, true)) {
                parent.terminated(connection);
                actual.onComplete();
            }
        }

        @Override
        public void dispose() {
            upstream.dispose();
            if (compareAndSet(false, true)) {
                parent.cancel(connection);
            }
        }

        @Override
        public boolean isDisposed() {
            return upstream.isDisposed();
        }

        @Override
        public void onSubscribe(Disposable d) {
            if (DisposableHelper.validate(upstream, d)) {
                this.upstream = d;

                actual.onSubscribe(this);
            }
        }
    }
}
