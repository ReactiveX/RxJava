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

package io.reactivex.internal.operators.single;

import java.util.concurrent.atomic.*;

import io.reactivex.*;
import io.reactivex.disposables.Disposable;

public final class SingleCache<T> extends Single<T> implements SingleObserver<T> {

    @SuppressWarnings("rawtypes")
    static final CacheDisposable[] EMPTY = new CacheDisposable[0];
    @SuppressWarnings("rawtypes")
    static final CacheDisposable[] TERMINATED = new CacheDisposable[0];

    final SingleSource<? extends T> source;

    final AtomicInteger wip;

    final AtomicReference<CacheDisposable<T>[]> observers;

    T value;

    Throwable error;

    @SuppressWarnings("unchecked")
    public SingleCache(SingleSource<? extends T> source) {
        this.source = source;
        this.wip = new AtomicInteger();
        this.observers = new AtomicReference<CacheDisposable<T>[]>(EMPTY);
    }

    @Override
    protected void subscribeActual(final SingleObserver<? super T> s) {
        CacheDisposable<T> d = new CacheDisposable<T>(s, this);
        s.onSubscribe(d);

        if (add(d)) {
            if (d.isDisposed()) {
                remove(d);
            }
        } else {
            Throwable ex = error;
            if (ex != null) {
                s.onError(ex);
            } else {
                s.onSuccess(value);
            }
            return;
        }

        if (wip.getAndIncrement() == 0) {
            source.subscribe(this);
        }
    }

    boolean add(CacheDisposable<T> observer) {
        for (;;) {
            CacheDisposable<T>[] a = observers.get();
            if (a == TERMINATED) {
                return false;
            }
            int n = a.length;
            @SuppressWarnings("unchecked")
            CacheDisposable<T>[] b = new CacheDisposable[n + 1];
            System.arraycopy(a, 0, b, 0, n);
            b[n] = observer;
            if (observers.compareAndSet(a, b)) {
                return true;
            }
        }
    }

    @SuppressWarnings("unchecked")
    void remove(CacheDisposable<T> observer) {
        for (;;) {
            CacheDisposable<T>[] a = observers.get();
            int n = a.length;
            if (n == 0) {
                return;
            }

            int j = -1;
            for (int i = 0; i < n; i++) {
                if (a[i] == observer) {
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

    @Override
    public void onSubscribe(Disposable d) {
        // not supported by this operator
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onSuccess(T value) {
        this.value = value;

        for (CacheDisposable<T> d : observers.getAndSet(TERMINATED)) {
            if (!d.isDisposed()) {
                d.actual.onSuccess(value);
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onError(Throwable e) {
        this.error = e;

        for (CacheDisposable<T> d : observers.getAndSet(TERMINATED)) {
            if (!d.isDisposed()) {
                d.actual.onError(e);
            }
        }
    }

    static final class CacheDisposable<T>
    extends AtomicBoolean
    implements Disposable {

        private static final long serialVersionUID = 7514387411091976596L;

        final SingleObserver<? super T> actual;

        final SingleCache<T> parent;

        CacheDisposable(SingleObserver<? super T> actual, SingleCache<T> parent) {
            this.actual = actual;
            this.parent = parent;
        }

        @Override
        public boolean isDisposed() {
            return get();
        }

        @Override
        public void dispose() {
            if (compareAndSet(false, true)) {
                parent.remove(this);
            }
        }
    }
}
