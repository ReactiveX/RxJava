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
 * Represents a hot Maybe-like source and consumer of events similar to Subjects.
 * <p>
 * All methods are thread safe. Calling onSuccess or onComplete multiple
 * times has no effect. Calling onError multiple times relays the Throwable to
 * the RxJavaPlugins' error handler.
 * <p>
 * The MaybeSubject doesn't store the Disposables coming through onSubscribe but
 * disposes them once the other onXXX methods were called (terminal state reached).
 * <p>History: 2.0.5 - experimental
 * @param <T> the value type received and emitted
 * @since 2.1
 */
public final class MaybeSubject<T> extends Maybe<T> implements MaybeObserver<T> {

    final AtomicReference<MaybeDisposable<T>[]> observers;

    @SuppressWarnings("rawtypes")
    static final MaybeDisposable[] EMPTY = new MaybeDisposable[0];

    @SuppressWarnings("rawtypes")
    static final MaybeDisposable[] TERMINATED = new MaybeDisposable[0];

    final AtomicBoolean once;
    T value;
    Throwable error;

    /**
     * Creates a fresh MaybeSubject.
     * @param <T> the value type received and emitted
     * @return the new MaybeSubject instance
     */
    @CheckReturnValue
    public static <T> MaybeSubject<T> create() {
        return new MaybeSubject<T>();
    }

    @SuppressWarnings("unchecked")
    MaybeSubject() {
        once = new AtomicBoolean();
        observers = new AtomicReference<MaybeDisposable<T>[]>(EMPTY);
    }

    @Override
    public void onSubscribe(Disposable d) {
        if (observers.get() == TERMINATED) {
            d.dispose();
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onSuccess(T value) {
        if (value == null) {
            onError(new NullPointerException("Null values are not allowed in 2.x"));
            return;
        }
        if (once.compareAndSet(false, true)) {
            this.value = value;
            for (MaybeDisposable<T> md : observers.getAndSet(TERMINATED)) {
                md.actual.onSuccess(value);
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onError(Throwable e) {
        if (e == null) {
            e = new NullPointerException("Null errors are not allowed in 2.x");
        }
        if (once.compareAndSet(false, true)) {
            this.error = e;
            for (MaybeDisposable<T> md : observers.getAndSet(TERMINATED)) {
                md.actual.onError(e);
            }
        } else {
            RxJavaPlugins.onError(e);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onComplete() {
        if (once.compareAndSet(false, true)) {
            for (MaybeDisposable<T> md : observers.getAndSet(TERMINATED)) {
                md.actual.onComplete();
            }
        }
    }

    @Override
    protected void subscribeActual(MaybeObserver<? super T> observer) {
        MaybeDisposable<T> md = new MaybeDisposable<T>(observer, this);
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
                T v = value;
                if (v == null) {
                    observer.onComplete();
                } else {
                    observer.onSuccess(v);
                }
            }
        }
    }

    boolean add(MaybeDisposable<T> inner) {
        for (;;) {
            MaybeDisposable<T>[] a = observers.get();
            if (a == TERMINATED) {
                return false;
            }

            int n = a.length;
            @SuppressWarnings("unchecked")
            MaybeDisposable<T>[] b = new MaybeDisposable[n + 1];
            System.arraycopy(a, 0, b, 0, n);
            b[n] = inner;
            if (observers.compareAndSet(a, b)) {
                return true;
            }
        }
    }

    @SuppressWarnings("unchecked")
    void remove(MaybeDisposable<T> inner) {
        for (;;) {
            MaybeDisposable<T>[] a = observers.get();
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
            MaybeDisposable<T>[] b;
            if (n == 1) {
                b = EMPTY;
            } else {
                b = new MaybeDisposable[n - 1];
                System.arraycopy(a, 0, b, 0, j);
                System.arraycopy(a, j + 1, b, j, n - j - 1);
            }

            if (observers.compareAndSet(a, b)) {
                return;
            }
        }
    }

    /**
     * Returns the success value if this MaybeSubject was terminated with a success value.
     * @return the success value or null
     */
    public T getValue() {
        if (observers.get() == TERMINATED) {
            return value;
        }
        return null;
    }

    /**
     * Returns true if this MaybeSubject was terminated with a success value.
     * @return true if this MaybeSubject was terminated with a success value
     */
    public boolean hasValue() {
        return observers.get() == TERMINATED && value != null;
    }

    /**
     * Returns the terminal error if this MaybeSubject has been terminated with an error, null otherwise.
     * @return the terminal error or null if not terminated or not with an error
     */
    public Throwable getThrowable() {
        if (observers.get() == TERMINATED) {
            return error;
        }
        return null;
    }

    /**
     * Returns true if this MaybeSubject has been terminated with an error.
     * @return true if this MaybeSubject has been terminated with an error
     */
    public boolean hasThrowable() {
        return observers.get() == TERMINATED && error != null;
    }

    /**
     * Returns true if this MaybeSubject has been completed.
     * @return true if this MaybeSubject has been completed
     */
    public boolean hasComplete() {
        return observers.get() == TERMINATED && value == null && error == null;
    }

    /**
     * Returns true if this MaybeSubject has observers.
     * @return true if this MaybeSubject has observers
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

    static final class MaybeDisposable<T>
    extends AtomicReference<MaybeSubject<T>> implements Disposable {
        private static final long serialVersionUID = -7650903191002190468L;

        final MaybeObserver<? super T> actual;

        MaybeDisposable(MaybeObserver<? super T> actual, MaybeSubject<T> parent) {
            this.actual = actual;
            lazySet(parent);
        }

        @Override
        public void dispose() {
            MaybeSubject<T> parent = getAndSet(null);
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
