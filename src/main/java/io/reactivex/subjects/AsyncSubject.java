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

import io.reactivex.annotations.Nullable;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.Observer;
import io.reactivex.annotations.CheckReturnValue;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.functions.ObjectHelper;
import io.reactivex.internal.observers.DeferredScalarDisposable;
import io.reactivex.plugins.RxJavaPlugins;

/**
 * A Subject that emits the very last value followed by a completion event or the received error to Observers.
 * <p>
 * <img width="640" height="239" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/AsyncSubject.png" alt="">
 * <p>
 * This subject does not have a public constructor by design; a new empty instance of this
 * {@code AsyncSubject} can be created via the {@link #create()} method.
 * <p>
 * Since a {@code Subject} is conceptionally derived from the {@code Processor} type in the Reactive Streams specification,
 * {@code null}s are not allowed (<a href="https://github.com/reactive-streams/reactive-streams-jvm#2.13">Rule 2.13</a>)
 * as parameters to {@link #onNext(Object)} and {@link #onError(Throwable)}. Such calls will result in a
 * {@link NullPointerException} being thrown and the subject's state is not changed.
 * <p>
 * Since an {@code AsyncSubject} is an {@link io.reactivex.Observable}, it does not support backpressure.
 * <p>
 * When this {@code AsyncSubject} is terminated via {@link #onError(Throwable)}, the
 * last observed item (if any) is cleared and late {@link io.reactivex.Observer}s only receive
 * the {@code onError} event.
 * <p>
 * The {@code AsyncSubject} caches the latest item internally and it emits this item only when {@code onComplete} is called.
 * Therefore, it is not recommended to use this {@code Subject} with infinite or never-completing sources.
 * <p>
 * Even though {@code AsyncSubject} implements the {@code Observer} interface, calling
 * {@code onSubscribe} is not required (<a href="https://github.com/reactive-streams/reactive-streams-jvm#2.12">Rule 2.12</a>)
 * if the subject is used as a standalone source. However, calling {@code onSubscribe}
 * after the {@code AsyncSubject} reached its terminal state will result in the
 * given {@code Disposable} being disposed immediately.
 * <p>
 * Calling {@link #onNext(Object)}, {@link #onError(Throwable)} and {@link #onComplete()}
 * is required to be serialized (called from the same thread or called non-overlappingly from different threads
 * through external means of serialization). The {@link #toSerialized()} method available to all {@code Subject}s
 * provides such serialization and also protects against reentrance (i.e., when a downstream {@code Observer}
 * consuming this subject also wants to call {@link #onNext(Object)} on this subject recursively).
 * The implementation of onXXX methods are technically thread-safe but non-serialized calls
 * to them may lead to undefined state in the currently subscribed Observers.
 * <p>
 * This {@code AsyncSubject} supports the standard state-peeking methods {@link #hasComplete()}, {@link #hasThrowable()},
 * {@link #getThrowable()} and {@link #hasObservers()} as well as means to read the very last observed value -
 * after this {@code AsyncSubject} has been completed - in a non-blocking and thread-safe
 * manner via {@link #hasValue()}, {@link #getValue()}, {@link #getValues()} or {@link #getValues(Object[])}.
 * <dl>
 *  <dt><b>Scheduler:</b></dt>
 *  <dd>{@code AsyncSubject} does not operate by default on a particular {@link io.reactivex.Scheduler} and
 *  the {@code Observer}s get notified on the thread where the terminating {@code onError} or {@code onComplete}
 *  methods were invoked.</dd>
 *  <dt><b>Error handling:</b></dt>
 *  <dd>When the {@link #onError(Throwable)} is called, the {@code AsyncSubject} enters into a terminal state
 *  and emits the same {@code Throwable} instance to the last set of {@code Observer}s. During this emission,
 *  if one or more {@code Observer}s dispose their respective {@code Disposable}s, the
 *  {@code Throwable} is delivered to the global error handler via
 *  {@link io.reactivex.plugins.RxJavaPlugins#onError(Throwable)} (multiple times if multiple {@code Observer}s
 *  cancel at once).
 *  If there were no {@code Observer}s subscribed to this {@code AsyncSubject} when the {@code onError()}
 *  was called, the global error handler is not invoked.
 *  </dd>
 * </dl>
 * <p>
 * Example usage:
 * <pre><code>
 * AsyncSubject&lt;Object&gt; subject = AsyncSubject.create();
 * 
 * TestObserver&lt;Object&gt; to1 = subject.test();
 *
 * to1.assertEmpty();
 *
 * subject.onNext(1);
 *
 * // AsyncSubject only emits when onComplete was called.
 * to1.assertEmpty();
 *
 * subject.onNext(2);
 * subject.onComplete();
 *
 * // onComplete triggers the emission of the last cached item and the onComplete event.
 * to1.assertResult(2);
 *
 * TestObserver&lt;Object&gt; to2 = subject.test();
 *
 * // late Observers receive the last cached item too
 * to2.assertResult(2);
 * </code></pre>
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
        ObjectHelper.requireNonNull(t, "onNext called with null. Null values are generally not allowed in 2.x operators and sources.");
        if (subscribers.get() == TERMINATED) {
            return;
        }
        value = t;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onError(Throwable t) {
        ObjectHelper.requireNonNull(t, "onError called with null. Null values are generally not allowed in 2.x operators and sources.");
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
    @Nullable
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
