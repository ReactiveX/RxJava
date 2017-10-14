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
 * Represents a hot Single-like source and consumer of events similar to Subjects.
 * <p>
 * All methods are thread safe. Calling onSuccess multiple
 * times has no effect. Calling onError multiple times relays the Throwable to
 * the RxJavaPlugins' error handler.
 * <p>
 * The SingleSubject doesn't store the Disposables coming through onSubscribe but
 * disposes them once the other onXXX methods were called (terminal state reached).
 * <p>History: 2.0.5 - experimental
 * @param <T> the value type received and emitted
 * @since 2.1
 */
public final class SingleSubject<T> extends Single<T> implements SingleObserver<T> {

    final AtomicReference<SingleDisposable<T>[]> observers;

    @SuppressWarnings("rawtypes")
    static final SingleDisposable[] EMPTY = new SingleDisposable[0];

    @SuppressWarnings("rawtypes")
    static final SingleDisposable[] TERMINATED = new SingleDisposable[0];

    final AtomicBoolean once;
    T value;
    Throwable error;

    /**
     * Creates a fresh SingleSubject.
     * @param <T> the value type received and emitted
     * @return the new SingleSubject instance
     */
    @CheckReturnValue
    @NonNull
    public static <T> SingleSubject<T> create() {
        return new SingleSubject<T>();
    }

    @SuppressWarnings("unchecked")
    SingleSubject() {
        once = new AtomicBoolean();
        observers = new AtomicReference<SingleDisposable<T>[]>(EMPTY);
    }

    @Override
    public void onSubscribe(@NonNull Disposable d) {
        if (observers.get() == TERMINATED) {
            d.dispose();
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onSuccess(@NonNull T value) {
        if (value == null) {
            onError(new NullPointerException("Null values are not allowed in 2.x"));
            return;
        }
        if (once.compareAndSet(false, true)) {
            this.value = value;
            for (SingleDisposable<T> md : observers.getAndSet(TERMINATED)) {
                md.actual.onSuccess(value);
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onError(@NonNull Throwable e) {
        if (e == null) {
            e = new NullPointerException("Null errors are not allowed in 2.x");
        }
        if (once.compareAndSet(false, true)) {
            this.error = e;
            for (SingleDisposable<T> md : observers.getAndSet(TERMINATED)) {
                md.actual.onError(e);
            }
        } else {
            RxJavaPlugins.onError(e);
        }
    }

    @Override
    protected void subscribeActual(@NonNull SingleObserver<? super T> observer) {
        SingleDisposable<T> md = new SingleDisposable<T>(observer, this);
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
                observer.onSuccess(value);
            }
        }
    }

    boolean add(@NonNull SingleDisposable<T> inner) {
        for (;;) {
            SingleDisposable<T>[] a = observers.get();
            if (a == TERMINATED) {
                return false;
            }

            int n = a.length;
            @SuppressWarnings("unchecked")
            SingleDisposable<T>[] b = new SingleDisposable[n + 1];
            System.arraycopy(a, 0, b, 0, n);
            b[n] = inner;
            if (observers.compareAndSet(a, b)) {
                return true;
            }
        }
    }

    @SuppressWarnings("unchecked")
    void remove(@NonNull SingleDisposable<T> inner) {
        for (;;) {
            SingleDisposable<T>[] a = observers.get();
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
            SingleDisposable<T>[] b;
            if (n == 1) {
                b = EMPTY;
            } else {
                b = new SingleDisposable[n - 1];
                System.arraycopy(a, 0, b, 0, j);
                System.arraycopy(a, j + 1, b, j, n - j - 1);
            }

            if (observers.compareAndSet(a, b)) {
                return;
            }
        }
    }

    /**
     * Returns the success value if this SingleSubject was terminated with a success value.
     * @return the success value or null
     */
    @Nullable
    public T getValue() {
        if (observers.get() == TERMINATED) {
            return value;
        }
        return null;
    }

    /**
     * Returns true if this SingleSubject was terminated with a success value.
     * @return true if this SingleSubject was terminated with a success value
     */
    public boolean hasValue() {
        return observers.get() == TERMINATED && value != null;
    }

    /**
     * Returns the terminal error if this SingleSubject has been terminated with an error, null otherwise.
     * @return the terminal error or null if not terminated or not with an error
     */
    @Nullable
    public Throwable getThrowable() {
        if (observers.get() == TERMINATED) {
            return error;
        }
        return null;
    }

    /**
     * Returns true if this SingleSubject has been terminated with an error.
     * @return true if this SingleSubject has been terminated with an error
     */
    public boolean hasThrowable() {
        return observers.get() == TERMINATED && error != null;
    }

    /**
     * Returns true if this SingleSubject has observers.
     * @return true if this SingleSubject has observers
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

    static final class SingleDisposable<T>
    extends AtomicReference<SingleSubject<T>> implements Disposable {
        private static final long serialVersionUID = -7650903191002190468L;

        final SingleObserver<? super T> actual;

        SingleDisposable(SingleObserver<? super T> actual, SingleSubject<T> parent) {
            this.actual = actual;
            lazySet(parent);
        }

        @Override
        public void dispose() {
            SingleSubject<T> parent = getAndSet(null);
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
