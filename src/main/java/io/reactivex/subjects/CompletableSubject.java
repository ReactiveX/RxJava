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

package io.reactivex.subjects;

import java.util.concurrent.atomic.*;

import io.reactivex.*;
import io.reactivex.annotations.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.plugins.RxJavaPlugins;

/**
 * Represents a hot Completable-like source and consumer of events similar to Subjects.
 * <p>
 * All methods are thread safe. Calling onComplete multiple
 * times has no effect. Calling onError multiple times relays the Throwable to
 * the RxJavaPlugins' error handler.
 * <p>
 * The CompletableSubject doesn't store the Disposables coming through onSubscribe but
 * disposes them once the other onXXX methods were called (terminal state reached).
 * <p>History: 2.0.5 - experimental
 * @since 2.1
 */
public final class CompletableSubject extends Completable implements CompletableObserver {

    final AtomicReference<CompletableDisposable[]> observers;

    static final CompletableDisposable[] EMPTY = new CompletableDisposable[0];

    static final CompletableDisposable[] TERMINATED = new CompletableDisposable[0];

    final AtomicBoolean once;
    Throwable error;

    /**
     * Creates a fresh CompletableSubject.
     * @return the new CompletableSubject instance
     */
    @CheckReturnValue
    public static CompletableSubject create() {
        return new CompletableSubject();
    }

    CompletableSubject() {
        once = new AtomicBoolean();
        observers = new AtomicReference<CompletableDisposable[]>(EMPTY);
    }

    @Override
    public void onSubscribe(Disposable d) {
        if (observers.get() == TERMINATED) {
            d.dispose();
        }
    }

    @Override
    public void onError(Throwable e) {
        if (e == null) {
            e = new NullPointerException("Null errors are not allowed in 2.x");
        }
        if (once.compareAndSet(false, true)) {
            this.error = e;
            for (CompletableDisposable md : observers.getAndSet(TERMINATED)) {
                md.actual.onError(e);
            }
        } else {
            RxJavaPlugins.onError(e);
        }
    }

    @Override
    public void onComplete() {
        if (once.compareAndSet(false, true)) {
            for (CompletableDisposable md : observers.getAndSet(TERMINATED)) {
                md.actual.onComplete();
            }
        }
    }

    @Override
    protected void subscribeActual(CompletableObserver observer) {
        CompletableDisposable md = new CompletableDisposable(observer, this);
        observer.onSubscribe(md);
        if (add(md)) {
            if (md.isDisposed()) {
                remove(md);
            }
        } else {
            Throwable ex = error;
            if (ex != null) {
                observer.onError(ex);
            } else {
                observer.onComplete();
            }
        }
    }

    boolean add(CompletableDisposable inner) {
        for (;;) {
            CompletableDisposable[] a = observers.get();
            if (a == TERMINATED) {
                return false;
            }

            int n = a.length;

            CompletableDisposable[] b = new CompletableDisposable[n + 1];
            System.arraycopy(a, 0, b, 0, n);
            b[n] = inner;
            if (observers.compareAndSet(a, b)) {
                return true;
            }
        }
    }

    void remove(CompletableDisposable inner) {
        for (;;) {
            CompletableDisposable[] a = observers.get();
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
            CompletableDisposable[] b;
            if (n == 1) {
                b = EMPTY;
            } else {
                b = new CompletableDisposable[n - 1];
                System.arraycopy(a, 0, b, 0, j);
                System.arraycopy(a, j + 1, b, j, n - j - 1);
            }

            if (observers.compareAndSet(a, b)) {
                return;
            }
        }
    }

    /**
     * Returns the terminal error if this CompletableSubject has been terminated with an error, null otherwise.
     * @return the terminal error or null if not terminated or not with an error
     */
    public Throwable getThrowable() {
        if (observers.get() == TERMINATED) {
            return error;
        }
        return null;
    }

    /**
     * Returns true if this CompletableSubject has been terminated with an error.
     * @return true if this CompletableSubject has been terminated with an error
     */
    public boolean hasThrowable() {
        return observers.get() == TERMINATED && error != null;
    }

    /**
     * Returns true if this CompletableSubject has been completed.
     * @return true if this CompletableSubject has been completed
     */
    public boolean hasComplete() {
        return observers.get() == TERMINATED && error == null;
    }

    /**
     * Returns true if this CompletableSubject has observers.
     * @return true if this CompletableSubject has observers
     */
    public boolean hasObservers() {
        return observers.get().length != 0;
    }

    /**
     * Returns the number of current observers.
     * @return the number of current observers
     */
    /* test */ int observerCount() {
        return observers.get().length;
    }

    static final class CompletableDisposable
    extends AtomicReference<CompletableSubject> implements Disposable {
        private static final long serialVersionUID = -7650903191002190468L;

        final CompletableObserver actual;

        CompletableDisposable(CompletableObserver actual, CompletableSubject parent) {
            this.actual = actual;
            lazySet(parent);
        }

        @Override
        public void dispose() {
            CompletableSubject parent = getAndSet(null);
            if (parent != null) {
                parent.remove(this);
            }
        }

        @Override
        public boolean isDisposed() {
            return get() == null;
        }
    }
}
