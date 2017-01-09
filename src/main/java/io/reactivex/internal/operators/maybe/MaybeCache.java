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

package io.reactivex.internal.operators.maybe;

import java.util.concurrent.atomic.*;

import io.reactivex.*;
import io.reactivex.disposables.Disposable;

/**
 * Consumes the source once and replays its signal to any current or future MaybeObservers.
 *
 * @param <T> the value type
 */
public final class MaybeCache<T> extends Maybe<T> implements MaybeObserver<T> {

    @SuppressWarnings("rawtypes")
    static final CacheDisposable[] EMPTY = new CacheDisposable[0];

    @SuppressWarnings("rawtypes")
    static final CacheDisposable[] TERMINATED = new CacheDisposable[0];

    final AtomicReference<MaybeSource<T>> source;

    final AtomicReference<CacheDisposable<T>[]> observers;

    T value;

    Throwable error;

    @SuppressWarnings("unchecked")
    public MaybeCache(MaybeSource<T> source) {
        this.source = new AtomicReference<MaybeSource<T>>(source);
        this.observers = new AtomicReference<CacheDisposable<T>[]>(EMPTY);
    }

    @Override
    protected void subscribeActual(MaybeObserver<? super T> observer) {
        CacheDisposable<T> parent = new CacheDisposable<T>(observer, this);
        observer.onSubscribe(parent);

        if (add(parent)) {
            if (parent.isDisposed()) {
                remove(parent);
                return;
            }
        } else {
            if (!parent.isDisposed()) {
                Throwable ex = error;
                if (ex != null) {
                    observer.onError(ex);
                } else {
                    T v = value;
                    if (v != null) {
                        observer.onSuccess(v);
                    } else {
                        observer.onComplete();
                    }
                }
            }
            return;
        }

        MaybeSource<T> src = source.getAndSet(null);
        if (src != null) {
            src.subscribe(this);
        }
    }

    @Override
    public void onSubscribe(Disposable d) {
        // deliberately ignored
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onSuccess(T value) {
        this.value = value;
        for (CacheDisposable<T> inner : observers.getAndSet(TERMINATED)) {
            if (!inner.isDisposed()) {
                inner.actual.onSuccess(value);
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onError(Throwable e) {
        this.error = e;
        for (CacheDisposable<T> inner : observers.getAndSet(TERMINATED)) {
            if (!inner.isDisposed()) {
                inner.actual.onError(e);
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onComplete() {
        for (CacheDisposable<T> inner : observers.getAndSet(TERMINATED)) {
            if (!inner.isDisposed()) {
                inner.actual.onComplete();
            }
        }
    }

    boolean add(CacheDisposable<T> inner) {
        for (;;) {
            CacheDisposable<T>[] a = observers.get();
            if (a == TERMINATED) {
                return false;
            }
            int n = a.length;

            @SuppressWarnings("unchecked")
            CacheDisposable<T>[] b = new CacheDisposable[n + 1];
            System.arraycopy(a, 0, b, 0, n);
            b[n] = inner;
            if (observers.compareAndSet(a, b)) {
                return true;
            }
        }
    }

    @SuppressWarnings("unchecked")
    void remove(CacheDisposable<T> inner) {
        for (;;) {
            CacheDisposable<T>[] a = observers.get();
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

            CacheDisposable<T>[] b;
            if (n == 1) {
                b = EMPTY;
            } else {
                b = new CacheDisposable[n - 1];
                System.arraycopy(a, 0, b, 0, j);
                System.arraycopy(a, j + 1, b, j, n - j - 1);
            }
            if (observers.compareAndSet(a, b)) {
                return;
            }
        }
    }

    static final class CacheDisposable<T>
    extends AtomicReference<MaybeCache<T>>
    implements Disposable {

        private static final long serialVersionUID = -5791853038359966195L;

        final MaybeObserver<? super T> actual;

        CacheDisposable(MaybeObserver<? super T> actual, MaybeCache<T> parent) {
            super(parent);
            this.actual = actual;
        }

        @Override
        public void dispose() {
            MaybeCache<T> mc = getAndSet(null);
            if (mc != null) {
                mc.remove(this);
            }
        }

        @Override
        public boolean isDisposed() {
            return get() == null;
        }
    }
}
