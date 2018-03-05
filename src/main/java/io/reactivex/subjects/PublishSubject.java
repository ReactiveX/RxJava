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
import io.reactivex.annotations.Nullable;
import java.util.concurrent.atomic.*;

import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.functions.ObjectHelper;
import io.reactivex.plugins.RxJavaPlugins;

/**
 * A Subject that emits (multicasts) items to currently subscribed {@link Observer}s and terminal events to current
 * or late {@code Observer}s.
 * <p>
 * <img width="640" height="281" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/PublishSubject.png" alt="">
 * <p>
 * This subject does not have a public constructor by design; a new empty instance of this
 * {@code PublishSubject} can be created via the {@link #create()} method.
 * <p>
 * Since a {@code Subject} is conceptionally derived from the {@code Processor} type in the Reactive Streams specification,
 * {@code null}s are not allowed (<a href="https://github.com/reactive-streams/reactive-streams-jvm#2.13">Rule 2.13</a>) as
 * parameters to {@link #onNext(Object)} and {@link #onError(Throwable)}. Such calls will result in a
 * {@link NullPointerException} being thrown and the subject's state is not changed.
 * <p>
 * Since a {@code PublishSubject} is an {@link io.reactivex.Observable}, it does not support backpressure.
 * <p>
 * When this {@code PublishSubject} is terminated via {@link #onError(Throwable)} or {@link #onComplete()},
 * late {@link io.reactivex.Observer}s only receive the respective terminal event.
 * <p>
 * Unlike a {@link BehaviorSubject}, a {@code PublishSubject} doesn't retain/cache items, therefore, a new
 * {@code Observer} won't receive any past items.
 * <p>
 * Even though {@code PublishSubject} implements the {@code Observer} interface, calling
 * {@code onSubscribe} is not required (<a href="https://github.com/reactive-streams/reactive-streams-jvm#2.12">Rule 2.12</a>)
 * if the subject is used as a standalone source. However, calling {@code onSubscribe}
 * after the {@code PublishSubject} reached its terminal state will result in the
 * given {@code Disposable} being disposed immediately.
 * <p>
 * Calling {@link #onNext(Object)}, {@link #onError(Throwable)} and {@link #onComplete()}
 * is required to be serialized (called from the same thread or called non-overlappingly from different threads
 * through external means of serialization). The {@link #toSerialized()} method available to all {@code Subject}s
 * provides such serialization and also protects against reentrance (i.e., when a downstream {@code Observer}
 * consuming this subject also wants to call {@link #onNext(Object)} on this subject recursively).
 * <p>
 * This {@code PublishSubject} supports the standard state-peeking methods {@link #hasComplete()}, {@link #hasThrowable()},
 * {@link #getThrowable()} and {@link #hasObservers()}.
 * <dl>
 *  <dt><b>Scheduler:</b></dt>
 *  <dd>{@code PublishSubject} does not operate by default on a particular {@link io.reactivex.Scheduler} and
 *  the {@code Observer}s get notified on the thread the respective {@code onXXX} methods were invoked.</dd>
 *  <dt><b>Error handling:</b></dt>
 *  <dd>When the {@link #onError(Throwable)} is called, the {@code PublishSubject} enters into a terminal state
 *  and emits the same {@code Throwable} instance to the last set of {@code Observer}s. During this emission,
 *  if one or more {@code Observer}s dispose their respective {@code Disposable}s, the
 *  {@code Throwable} is delivered to the global error handler via
 *  {@link io.reactivex.plugins.RxJavaPlugins#onError(Throwable)} (multiple times if multiple {@code Observer}s
 *  cancel at once).
 *  If there were no {@code Observer}s subscribed to this {@code PublishSubject} when the {@code onError()}
 *  was called, the global error handler is not invoked.
 *  </dd>
 * </dl>
 * <p>
 * Example usage:
 * <pre> {@code

  PublishSubject<Object> subject = PublishSubject.create();
  // observer1 will receive all onNext and onComplete events
  subject.subscribe(observer1);
  subject.onNext("one");
  subject.onNext("two");
  // observer2 will only receive "three" and onComplete
  subject.subscribe(observer2);
  subject.onNext("three");
  subject.onComplete();

  // late Observers only receive the terminal event
  subject.test().assertEmpty();
  } </pre>
 *
 * @param <T>
 *          the type of items observed and emitted by the Subject
 */
public final class PublishSubject<T> extends Subject<T> {
    /** The terminated indicator for the subscribers array. */
    @SuppressWarnings("rawtypes")
    static final PublishDisposable[] TERMINATED = new PublishDisposable[0];
    /** An empty subscribers array to avoid allocating it all the time. */
    @SuppressWarnings("rawtypes")
    static final PublishDisposable[] EMPTY = new PublishDisposable[0];

    /** The array of currently subscribed subscribers. */
    final AtomicReference<PublishDisposable<T>[]> subscribers;

    /** The error, write before terminating and read after checking subscribers. */
    Throwable error;

    /**
     * Constructs a PublishSubject.
     * @param <T> the value type
     * @return the new PublishSubject
     */
    @CheckReturnValue
    public static <T> PublishSubject<T> create() {
        return new PublishSubject<T>();
    }

    /**
     * Constructs a PublishSubject.
     * @since 2.0
     */
    @SuppressWarnings("unchecked")
    PublishSubject() {
        subscribers = new AtomicReference<PublishDisposable<T>[]>(EMPTY);
    }


    @Override
    public void subscribeActual(Observer<? super T> t) {
        PublishDisposable<T> ps = new PublishDisposable<T>(t, this);
        t.onSubscribe(ps);
        if (add(ps)) {
            // if cancellation happened while a successful add, the remove() didn't work
            // so we need to do it again
            if (ps.isDisposed()) {
                remove(ps);
            }
        } else {
            Throwable ex = error;
            if (ex != null) {
                t.onError(ex);
            } else {
                t.onComplete();
            }
        }
    }

    /**
     * Tries to add the given subscriber to the subscribers array atomically
     * or returns false if the subject has terminated.
     * @param ps the subscriber to add
     * @return true if successful, false if the subject has terminated
     */
    boolean add(PublishDisposable<T> ps) {
        for (;;) {
            PublishDisposable<T>[] a = subscribers.get();
            if (a == TERMINATED) {
                return false;
            }

            int n = a.length;
            @SuppressWarnings("unchecked")
            PublishDisposable<T>[] b = new PublishDisposable[n + 1];
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
    void remove(PublishDisposable<T> ps) {
        for (;;) {
            PublishDisposable<T>[] a = subscribers.get();
            if (a == TERMINATED || a == EMPTY) {
                return;
            }

            int n = a.length;
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

            PublishDisposable<T>[] b;

            if (n == 1) {
                b = EMPTY;
            } else {
                b = new PublishDisposable[n - 1];
                System.arraycopy(a, 0, b, 0, j);
                System.arraycopy(a, j + 1, b, j, n - j - 1);
            }
            if (subscribers.compareAndSet(a, b)) {
                return;
            }
        }
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
        for (PublishDisposable<T> s : subscribers.get()) {
            s.onNext(t);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onError(Throwable t) {
        ObjectHelper.requireNonNull(t, "onError called with null. Null values are generally not allowed in 2.x operators and sources.");
        if (subscribers.get() == TERMINATED) {
            RxJavaPlugins.onError(t);
            return;
        }
        error = t;

        for (PublishDisposable<T> s : subscribers.getAndSet(TERMINATED)) {
            s.onError(t);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onComplete() {
        if (subscribers.get() == TERMINATED) {
            return;
        }
        for (PublishDisposable<T> s : subscribers.getAndSet(TERMINATED)) {
            s.onComplete();
        }
    }

    @Override
    public boolean hasObservers() {
        return subscribers.get().length != 0;
    }

    @Override
    @Nullable
    public Throwable getThrowable() {
        if (subscribers.get() == TERMINATED) {
            return error;
        }
        return null;
    }

    @Override
    public boolean hasThrowable() {
        return subscribers.get() == TERMINATED && error != null;
    }

    @Override
    public boolean hasComplete() {
        return subscribers.get() == TERMINATED && error == null;
    }

    /**
     * Wraps the actual subscriber, tracks its requests and makes cancellation
     * to remove itself from the current subscribers array.
     *
     * @param <T> the value type
     */
    static final class PublishDisposable<T> extends AtomicBoolean implements Disposable {

        private static final long serialVersionUID = 3562861878281475070L;
        /** The actual subscriber. */
        final Observer<? super T> actual;
        /** The subject state. */
        final PublishSubject<T> parent;

        /**
         * Constructs a PublishSubscriber, wraps the actual subscriber and the state.
         * @param actual the actual subscriber
         * @param parent the parent PublishProcessor
         */
        PublishDisposable(Observer<? super T> actual, PublishSubject<T> parent) {
            this.actual = actual;
            this.parent = parent;
        }

        public void onNext(T t) {
            if (!get()) {
                actual.onNext(t);
            }
        }

        public void onError(Throwable t) {
            if (get()) {
                RxJavaPlugins.onError(t);
            } else {
                actual.onError(t);
            }
        }

        public void onComplete() {
            if (!get()) {
                actual.onComplete();
            }
        }

        @Override
        public void dispose() {
            if (compareAndSet(false, true)) {
                parent.remove(this);
            }
        }

        @Override
        public boolean isDisposed() {
            return get();
        }
    }
}
