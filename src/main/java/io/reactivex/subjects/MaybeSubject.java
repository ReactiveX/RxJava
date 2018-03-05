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
 * Represents a hot Maybe-like source and consumer of events similar to Subjects.
 * <p>
 * <img width="640" height="164" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/MaybeSubject.png" alt="">
 * <p>
 * This subject does not have a public constructor by design; a new non-terminated instance of this
 * {@code MaybeSubject} can be created via the {@link #create()} method.
 * <p>
 * Since the {@code MaybeSubject} is conceptionally derived from the {@code Processor} type in the Reactive Streams specification,
 * {@code null}s are not allowed (<a href="https://github.com/reactive-streams/reactive-streams-jvm#2.13">Rule 2.13</a>)
 * as parameters to  {@link #onSuccess(Object)} and {@link #onError(Throwable)}. Such calls will result in a
 * {@link NullPointerException} being thrown and the subject's state is not changed.
 * <p>
 * Since a {@code MaybeSubject} is a {@link io.reactivex.Maybe}, calling {@code onSuccess}, {@code onError}
 * or {@code onComplete} will move this {@code MaybeSubject} into its terminal state atomically.
 * <p>
 * All methods are thread safe. Calling {@link #onSuccess(Object)} or {@link #onComplete()} multiple
 * times has no effect. Calling {@link #onError(Throwable)} multiple times relays the {@code Throwable} to
 * the {@link io.reactivex.plugins.RxJavaPlugins#onError(Throwable)} global error handler.
 * <p>
 * Even though {@code MaybeSubject} implements the {@code MaybeObserver} interface, calling
 * {@code onSubscribe} is not required (<a href="https://github.com/reactive-streams/reactive-streams-jvm#2.12">Rule 2.12</a>)
 * if the subject is used as a standalone source. However, calling {@code onSubscribe}
 * after the {@code MaybeSubject} reached its terminal state will result in the
 * given {@code Disposable} being disposed immediately.
 * <p>
 * This {@code MaybeSubject} supports the standard state-peeking methods {@link #hasComplete()}, {@link #hasThrowable()},
 * {@link #getThrowable()} and {@link #hasObservers()} as well as means to read any success item in a non-blocking
 * and thread-safe manner via {@link #hasValue()} and {@link #getValue()}.
 * <p>
 * The {@code MaybeSubject} does not support clearing its cached {@code onSuccess} value.
 * <dl>
 *  <dt><b>Scheduler:</b></dt>
 *  <dd>{@code MaybeSubject} does not operate by default on a particular {@link io.reactivex.Scheduler} and
 *  the {@code MaybeObserver}s get notified on the thread where the terminating {@code onSuccess}, {@code onError} or {@code onComplete}
 *  methods were invoked.</dd>
 *  <dt><b>Error handling:</b></dt>
 *  <dd>When the {@link #onError(Throwable)} is called, the {@code MaybeSubject} enters into a terminal state
 *  and emits the same {@code Throwable} instance to the last set of {@code MaybeObserver}s. During this emission,
 *  if one or more {@code MaybeObserver}s dispose their respective {@code Disposable}s, the
 *  {@code Throwable} is delivered to the global error handler via
 *  {@link io.reactivex.plugins.RxJavaPlugins#onError(Throwable)} (multiple times if multiple {@code MaybeObserver}s
 *  cancel at once).
 *  If there were no {@code MaybeObserver}s subscribed to this {@code MaybeSubject} when the {@code onError()}
 *  was called, the global error handler is not invoked.
 *  </dd>
 * </dl>
 * <p>
 * Example usage:
 * <pre><code>
 * MaybeSubject&lt;Integer&gt; subject1 = MaybeSubject.create();
 *
 * TestObserver&lt;Integer&gt; to1 = subject1.test();
 *
 * // MaybeSubjects are empty by default
 * to1.assertEmpty();
 *
 * subject1.onSuccess(1);
 *
 * // onSuccess is a terminal event with MaybeSubjects
 * // TestObserver converts onSuccess into onNext + onComplete
 * to1.assertResult(1);
 *
 * TestObserver&lt;Integer&gt; to2 = subject1.test();
 *
 * // late Observers receive the terminal signal (onSuccess) too
 * to2.assertResult(1);
 *
 * // -----------------------------------------------------
 *
 * MaybeSubject&lt;Integer&gt; subject2 = MaybeSubject.create();
 *
 * TestObserver&lt;Integer&gt; to3 = subject2.test();
 *
 * subject2.onComplete();
 *
 * // a completed MaybeSubject completes its MaybeObservers
 * to3.assertResult();
 *
 * TestObserver&lt;Integer&gt; to4 = subject1.test();
 *
 * // late Observers receive the terminal signal (onComplete) too
 * to4.assertResult();
 * </code></pre>
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
        ObjectHelper.requireNonNull(value, "onSuccess called with null. Null values are generally not allowed in 2.x operators and sources.");
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
        ObjectHelper.requireNonNull(e, "onError called with null. Null values are generally not allowed in 2.x operators and sources.");
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
    @Nullable
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
    @Nullable
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
