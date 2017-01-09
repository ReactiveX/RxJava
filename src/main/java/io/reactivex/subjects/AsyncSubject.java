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

import io.reactivex.annotations.CheckReturnValue;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.observers.DeferredScalarDisposable;
import io.reactivex.plugins.RxJavaPlugins;

/**
 * An Subject that emits the very last value followed by a completion event or the received error to Observers.
 *
 * <p>The implementation of onXXX methods are technically thread-safe but non-serialized calls
 * to them may lead to undefined state in the currently subscribed Observers.
 *
 * @param <T> the value type
 */

public final class AsyncSubject<T> extends Subject<T> {

    @SuppressWarnings("rawtypes")
    static final AsyncDisposable[] EMPTY = new AsyncDisposable[0];

    @SuppressWarnings("rawtypes")
    static final AsyncDisposable[] TERMINATED = new AsyncDisposable[0];

    final AtomicReference<AsyncDisposable<T>[]> subscribers;

    /** Write before updating subscribers, read after reading subscribers as TERMINATED. */
    Throwable error;

    /** Write before updating subscribers, read after reading subscribers as TERMINATED. */
    T value;

    /**
     * Creates a new AsyncProcessor.
     * @param <T> the value type to be received and emitted
     * @return the new AsyncProcessor instance
     */
    @CheckReturnValue
    public static <T> AsyncSubject<T> create() {
        return new AsyncSubject<T>();
    }

    /**
     * Constructs an AsyncSubject.
     * @since 2.0
     */
    @SuppressWarnings("unchecked")
    AsyncSubject() {
        this.subscribers = new AtomicReference<AsyncDisposable<T>[]>(EMPTY);
    }

    @Override
    public void onSubscribe(Disposable s) {
        if (subscribers.get() == TERMINATED) {
            s.dispose();
        }
    }

    @Override
    public void onNext(T t) {
        if (subscribers.get() == TERMINATED) {
            return;
        }
        if (t == null) {
            nullOnNext();
            return;
        }
        value = t;
    }

    @SuppressWarnings("unchecked")
    void nullOnNext() {
        value = null;
        Throwable ex = new NullPointerException("onNext called with null. Null values are generally not allowed in 2.x operators and sources.");
        error = ex;
        for (AsyncDisposable<T> as : subscribers.getAndSet(TERMINATED)) {
            as.onError(ex);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onError(Throwable t) {
        if (t == null) {
            t = new NullPointerException("onError called with null. Null values are generally not allowed in 2.x operators and sources.");
        }
        if (subscribers.get() == TERMINATED) {
            RxJavaPlugins.onError(t);
            return;
        }
        value = null;
        error = t;
        for (AsyncDisposable<T> as : subscribers.getAndSet(TERMINATED)) {
            as.onError(t);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onComplete() {
        if (subscribers.get() == TERMINATED) {
            return;
        }
        T v = value;
        AsyncDisposable<T>[] array = subscribers.getAndSet(TERMINATED);
        if (v == null) {
            for (AsyncDisposable<T> as : array) {
                as.onComplete();
            }
        } else {
            for (AsyncDisposable<T> as : array) {
                as.complete(v);
            }
        }
    }

    @Override
    public boolean hasObservers() {
        return subscribers.get().length != 0;
    }

    @Override
    public boolean hasThrowable() {
        return subscribers.get() == TERMINATED && error != null;
    }

    @Override
    public boolean hasComplete() {
        return subscribers.get() == TERMINATED && error == null;
    }

    @Override
    public Throwable getThrowable() {
        return subscribers.get() == TERMINATED ? error : null;
    }

    @Override
    protected void subscribeActual(Observer<? super T> s) {
        AsyncDisposable<T> as = new AsyncDisposable<T>(s, this);
        s.onSubscribe(as);
        if (add(as)) {
            if (as.isDisposed()) {
                remove(as);
            }
        } else {
            Throwable ex = error;
            if (ex != null) {
                s.onError(ex);
            } else {
                T v = value;
                if (v != null) {
                    as.complete(v);
                } else {
                    as.onComplete();
                }
            }
        }
    }

    /**
     * Tries to add the given subscriber to the subscribers array atomically
     * or returns false if the subject has terminated.
     * @param ps the subscriber to add
     * @return true if successful, false if the subject has terminated
     */
    boolean add(AsyncDisposable<T> ps) {
        for (;;) {
            AsyncDisposable<T>[] a = subscribers.get();
            if (a == TERMINATED) {
                return false;
            }

            int n = a.length;
            @SuppressWarnings("unchecked")
            AsyncDisposable<T>[] b = new AsyncDisposable[n + 1];
            System.arraycopy(a, 0, b, 0, n);
            b[n] = ps;

            if (subscribers.compareAndSet(a, b)) {
                return true;
            }
        }
    }

    /**
     * Atomically removes the given subscriber if it is subscribed to the subject.
     * @param ps the subject to remove
     */
    @SuppressWarnings("unchecked")
    void remove(AsyncDisposable<T> ps) {
        for (;;) {
            AsyncDisposable<T>[] a = subscribers.get();
            int n = a.length;
            if (n == 0) {
                return;
            }

            int j = -1;
            for (int i = 0; i < n; i++) {
                if (a[i] == ps) {
                    j = i;
                    break;
                }
            }

            if (j < 0) {
                return;
            }

            AsyncDisposable<T>[] b;

            if (n == 1) {
                b = EMPTY;
            } else {
                b = new AsyncDisposable[n - 1];
                System.arraycopy(a, 0, b, 0, j);
                System.arraycopy(a, j + 1, b, j, n - j - 1);
            }
            if (subscribers.compareAndSet(a, b)) {
                return;
            }
        }
    }

    /**
     * Returns true if the subject has any value.
     * <p>The method is thread-safe.
     * @return true if the subject has any value
     */
    public boolean hasValue() {
        return subscribers.get() == TERMINATED && value != null;
    }

    /**
     * Returns a single value the Subject currently has or null if no such value exists.
     * <p>The method is thread-safe.
     * @return a single value the Subject currently has or null if no such value exists
     */
    public T getValue() {
        return subscribers.get() == TERMINATED ? value : null;
    }

    /**
     * Returns an Object array containing snapshot all values of the Subject.
     * <p>The method is thread-safe.
     * @return the array containing the snapshot of all values of the Subject
     */
    public Object[] getValues() {
        T v = getValue();
        return v != null ? new Object[] { v } : new Object[0];
    }

    /**
     * Returns a typed array containing a snapshot of all values of the Subject.
     * <p>The method follows the conventions of Collection.toArray by setting the array element
     * after the last value to null (if the capacity permits).
     * <p>The method is thread-safe.
     * @param array the target array to copy values into if it fits
     * @return the given array if the values fit into it or a new array containing all values
     */
    public T[] getValues(T[] array) {
        T v = getValue();
        if (v == null) {
            if (array.length != 0) {
                array[0] = null;
            }
            return array;
        }
        if (array.length == 0) {
            array = Arrays.copyOf(array, 1);
        }
        array[0] = v;
        if (array.length != 1) {
            array[1] = null;
        }
        return array;
    }

    static final class AsyncDisposable<T> extends DeferredScalarDisposable<T> {
        private static final long serialVersionUID = 5629876084736248016L;

        final AsyncSubject<T> parent;

        AsyncDisposable(Observer<? super T> actual, AsyncSubject<T> parent) {
            super(actual);
            this.parent = parent;
        }

        @Override
        public void dispose() {
            if (super.tryDispose()) {
                parent.remove(this);
            }
        }

        void onComplete() {
            if (!isDisposed()) {
                actual.onComplete();
            }
        }

        void onError(Throwable t) {
            if (isDisposed()) {
                RxJavaPlugins.onError(t);
            } else {
                actual.onError(t);
            }
        }
    }
}
