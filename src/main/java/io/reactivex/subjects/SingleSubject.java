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
import io.reactivex.internal.functions.ObjectHelper;
import io.reactivex.plugins.RxJavaPlugins;

/**
 * Represents a hot Single-like source and consumer of events similar to Subjects.
 * <p>
 * <img width="640" height="236" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/SingleSubject.png" alt="">
 * <p>
 * This subject does not have a public constructor by design; a new non-terminated instance of this
 * {@code SingleSubject} can be created via the {@link #create()} method.
 * <p>
 * Since the {@code SingleSubject} is conceptionally derived from the {@code Processor} type in the Reactive Streams specification,
 * {@code null}s are not allowed (<a href="https://github.com/reactive-streams/reactive-streams-jvm#2.13">Rule 2.13</a>)
 * as parameters to  {@link #onSuccess(Object)} and {@link #onError(Throwable)}. Such calls will result in a
 * {@link NullPointerException} being thrown and the subject's state is not changed.
 * <p>
 * Since a {@code SingleSubject} is a {@link io.reactivex.Single}, calling {@code onSuccess} or {@code onError}
 * will move this {@code SingleSubject} into its terminal state atomically.
 * <p>
 * All methods are thread safe. Calling {@link #onSuccess(Object)} multiple
 * times has no effect. Calling {@link #onError(Throwable)} multiple times relays the {@code Throwable} to
 * the {@link io.reactivex.plugins.RxJavaPlugins#onError(Throwable)} global error handler.
 * <p>
 * Even though {@code SingleSubject} implements the {@code SingleObserver} interface, calling
 * {@code onSubscribe} is not required (<a href="https://github.com/reactive-streams/reactive-streams-jvm#2.12">Rule 2.12</a>)
 * if the subject is used as a standalone source. However, calling {@code onSubscribe}
 * after the {@code SingleSubject} reached its terminal state will result in the
 * given {@code Disposable} being disposed immediately.
 * <p>
 * This {@code SingleSubject} supports the standard state-peeking methods {@link #hasThrowable()},
 * {@link #getThrowable()} and {@link #hasObservers()} as well as means to read any success item in a non-blocking
 * and thread-safe manner via {@link #hasValue()} and {@link #getValue()}.
 * <p>
 * The {@code SingleSubject} does not support clearing its cached {@code onSuccess} value.
 * <dl>
 *  <dt><b>Scheduler:</b></dt>
 *  <dd>{@code SingleSubject} does not operate by default on a particular {@link io.reactivex.Scheduler} and
 *  the {@code SingleObserver}s get notified on the thread where the terminating {@code onSuccess} or {@code onError}
 *  methods were invoked.</dd>
 *  <dt><b>Error handling:</b></dt>
 *  <dd>When the {@link #onError(Throwable)} is called, the {@code SingleSubject} enters into a terminal state
 *  and emits the same {@code Throwable} instance to the last set of {@code SingleObserver}s. During this emission,
 *  if one or more {@code SingleObserver}s dispose their respective {@code Disposable}s, the
 *  {@code Throwable} is delivered to the global error handler via
 *  {@link io.reactivex.plugins.RxJavaPlugins#onError(Throwable)} (multiple times if multiple {@code SingleObserver}s
 *  cancel at once).
 *  If there were no {@code SingleObserver}s subscribed to this {@code SingleSubject} when the {@code onError()}
 *  was called, the global error handler is not invoked.
 *  </dd>
 * </dl>
 * <p>
 * Example usage:
 * <pre><code>
 * SingleSubject&lt;Integer&gt; subject1 = SingleSubject.create();
 * 
 * TestObserver&lt;Integer&gt; to1 = subject1.test();
 * 
 * // SingleSubjects are empty by default
 * to1.assertEmpty();
 * 
 * subject1.onSuccess(1);
 * 
 * // onSuccess is a terminal event with SingleSubjects
 * // TestObserver converts onSuccess into onNext + onComplete
 * to1.assertResult(1);
 *
 * TestObserver&lt;Integer&gt; to2 = subject1.test();
 * 
 * // late Observers receive the terminal signal (onSuccess) too
 * to2.assertResult(1);
 * </code></pre>
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
        ObjectHelper.requireNonNull(value, "onSuccess called with null. Null values are generally not allowed in 2.x operators and sources.");
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
        ObjectHelper.requireNonNull(e, "onError called with null. Null values are generally not allowed in 2.x operators and sources.");
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
