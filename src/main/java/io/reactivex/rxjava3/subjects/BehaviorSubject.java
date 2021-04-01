/*
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

package io.reactivex.rxjava3.subjects;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.*;

import io.reactivex.rxjava3.annotations.*;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.internal.util.*;
import io.reactivex.rxjava3.internal.util.AppendOnlyLinkedArrayList.NonThrowingPredicate;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;

/**
 * Subject that emits the most recent item it has observed and all subsequent observed items to each subscribed
 * {@link Observer}.
 * <p>
 * <img width="640" height="415" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/S.BehaviorSubject.v3.png" alt="">
 * <p>
 * This subject does not have a public constructor by design; a new empty instance of this
 * {@code BehaviorSubject} can be created via the {@link #create()} method and
 * a new non-empty instance can be created via {@link #createDefault(Object)} (named as such to avoid
 * overload resolution conflict with {@code Observable.create} that creates an Observable, not a {@code BehaviorSubject}).
 * <p>
 * Since a {@code Subject} is conceptionally derived from the {@code Processor} type in the Reactive Streams specification,
 * {@code null}s are not allowed (<a href="https://github.com/reactive-streams/reactive-streams-jvm#2.13">Rule 2.13</a>) as
 * default initial values in {@link #createDefault(Object)} or as parameters to {@link #onNext(Object)} and
 * {@link #onError(Throwable)}. Such calls will result in a
 * {@link NullPointerException} being thrown and the subject's state is not changed.
 * <p>
 * Since a {@code BehaviorSubject} is an {@link io.reactivex.rxjava3.core.Observable}, it does not support backpressure.
 * <p>
 * When this {@code BehaviorSubject} is terminated via {@link #onError(Throwable)} or {@link #onComplete()}, the
 * last observed item (if any) is cleared and late {@link io.reactivex.rxjava3.core.Observer}s only receive
 * the respective terminal event.
 * <p>
 * The {@code BehaviorSubject} does not support clearing its cached value (to appear empty again), however, the
 * effect can be achieved by using a special item and making sure {@code Observer}s subscribe through a
 * filter whose predicate filters out this special item:
 * <pre><code>
 * BehaviorSubject&lt;Integer&gt; subject = BehaviorSubject.create();
 *
 * final Integer EMPTY = Integer.MIN_VALUE;
 *
 * Observable&lt;Integer&gt; observable = subject.filter(v -&gt; v != EMPTY);
 *
 * TestObserver&lt;Integer&gt; to1 = observable.test();
 *
 * observable.onNext(1);
 * // this will "clear" the cache
 * observable.onNext(EMPTY);
 *
 * TestObserver&lt;Integer&gt; to2 = observable.test();
 *
 * subject.onNext(2);
 * subject.onComplete();
 *
 * // to1 received both non-empty items
 * to1.assertResult(1, 2);
 *
 * // to2 received only 2 even though the current item was EMPTY
 * // when it got subscribed
 * to2.assertResult(2);
 *
 * // Observers coming after the subject was terminated receive
 * // no items and only the onComplete event in this case.
 * observable.test().assertResult();
 * </code></pre>
 * <p>
 * Even though {@code BehaviorSubject} implements the {@code Observer} interface, calling
 * {@code onSubscribe} is not required (<a href="https://github.com/reactive-streams/reactive-streams-jvm#2.12">Rule 2.12</a>)
 * if the subject is used as a standalone source. However, calling {@code onSubscribe}
 * after the {@code BehaviorSubject} reached its terminal state will result in the
 * given {@code Disposable} being disposed immediately.
 * <p>
 * Calling {@link #onNext(Object)}, {@link #onError(Throwable)} and {@link #onComplete()}
 * is required to be serialized (called from the same thread or called non-overlappingly from different threads
 * through external means of serialization). The {@link #toSerialized()} method available to all {@code Subject}s
 * provides such serialization and also protects against reentrance (i.e., when a downstream {@code Observer}
 * consuming this subject also wants to call {@link #onNext(Object)} on this subject recursively).
 * <p>
 * This {@code BehaviorSubject} supports the standard state-peeking methods {@link #hasComplete()}, {@link #hasThrowable()},
 * {@link #getThrowable()} and {@link #hasObservers()} as well as means to read the latest observed value
 * in a non-blocking and thread-safe manner via {@link #hasValue()} or {@link #getValue()}.
 * <dl>
 *  <dt><b>Scheduler:</b></dt>
 *  <dd>{@code BehaviorSubject} does not operate by default on a particular {@link io.reactivex.rxjava3.core.Scheduler} and
 *  the {@code Observer}s get notified on the thread the respective {@code onXXX} methods were invoked.</dd>
 *  <dt><b>Error handling:</b></dt>
 *  <dd>When the {@link #onError(Throwable)} is called, the {@code BehaviorSubject} enters into a terminal state
 *  and emits the same {@code Throwable} instance to the last set of {@code Observer}s. During this emission,
 *  if one or more {@code Observer}s dispose their respective {@code Disposable}s, the
 *  {@code Throwable} is delivered to the global error handler via
 *  {@link io.reactivex.rxjava3.plugins.RxJavaPlugins#onError(Throwable)} (multiple times if multiple {@code Observer}s
 *  cancel at once).
 *  If there were no {@code Observer}s subscribed to this {@code BehaviorSubject} when the {@code onError()}
 *  was called, the global error handler is not invoked.
 *  </dd>
 * </dl>
 * <p>
 * Example usage:
 * <pre> {@code

  // observer will receive all 4 events (including "default").
  BehaviorSubject<Object> subject = BehaviorSubject.createDefault("default");
  subject.subscribe(observer);
  subject.onNext("one");
  subject.onNext("two");
  subject.onNext("three");

  // observer will receive the "one", "two" and "three" events, but not "zero"
  BehaviorSubject<Object> subject = BehaviorSubject.create();
  subject.onNext("zero");
  subject.onNext("one");
  subject.subscribe(observer);
  subject.onNext("two");
  subject.onNext("three");

  // observer will receive only onComplete
  BehaviorSubject<Object> subject = BehaviorSubject.create();
  subject.onNext("zero");
  subject.onNext("one");
  subject.onComplete();
  subject.subscribe(observer);

  // observer will receive only onError
  BehaviorSubject<Object> subject = BehaviorSubject.create();
  subject.onNext("zero");
  subject.onNext("one");
  subject.onError(new RuntimeException("error"));
  subject.subscribe(observer);
  } </pre>
 *
 * @param <T>
 *          the type of item expected to be observed by the Subject
 */
public final class BehaviorSubject<T> extends Subject<T> {

    final AtomicReference<Object> value;

    final AtomicReference<BehaviorDisposable<T>[]> observers;

    @SuppressWarnings("rawtypes")
    static final BehaviorDisposable[] EMPTY = new BehaviorDisposable[0];

    @SuppressWarnings("rawtypes")
    static final BehaviorDisposable[] TERMINATED = new BehaviorDisposable[0];
    final ReadWriteLock lock;
    final Lock readLock;
    final Lock writeLock;

    final AtomicReference<Throwable> terminalEvent;

    long index;

    /**
     * Creates a {@link BehaviorSubject} without a default item.
     *
     * @param <T>
     *            the type of item the Subject will emit
     * @return the constructed {@link BehaviorSubject}
     */
    @CheckReturnValue
    @NonNull
    public static <T> BehaviorSubject<T> create() {
        return new BehaviorSubject<>(null);
    }

    /**
     * Creates a {@link BehaviorSubject} that emits the last item it observed and all subsequent items to each
     * {@link Observer} that subscribes to it.
     *
     * @param <T>
     *            the type of item the Subject will emit
     * @param defaultValue
     *            the item that will be emitted first to any {@link Observer} as long as the
     *            {@link BehaviorSubject} has not yet observed any items from its source {@code Observable}
     * @return the constructed {@link BehaviorSubject}
     * @throws NullPointerException if {@code defaultValue} is {@code null}
     */
    @CheckReturnValue
    @NonNull
    public static <@NonNull T> BehaviorSubject<T> createDefault(T defaultValue) {
        Objects.requireNonNull(defaultValue, "defaultValue is null");
        return new BehaviorSubject<>(defaultValue);
    }

    /**
     * Constructs an empty BehaviorSubject.
     * @param defaultValue the initial value, not null (verified)
     * @since 2.0
     */
    @SuppressWarnings("unchecked")
    BehaviorSubject(T defaultValue) {
        this.lock = new ReentrantReadWriteLock();
        this.readLock = lock.readLock();
        this.writeLock = lock.writeLock();
        this.observers = new AtomicReference<>(EMPTY);
        this.value = new AtomicReference<>(defaultValue);
        this.terminalEvent = new AtomicReference<>();
    }

    @Override
    protected void subscribeActual(Observer<? super T> observer) {
        BehaviorDisposable<T> bs = new BehaviorDisposable<>(observer, this);
        observer.onSubscribe(bs);
        if (add(bs)) {
            if (bs.cancelled) {
                remove(bs);
            } else {
                bs.emitFirst();
            }
        } else {
            Throwable ex = terminalEvent.get();
            if (ex == ExceptionHelper.TERMINATED) {
                observer.onComplete();
            } else {
                observer.onError(ex);
            }
        }
    }

    @Override
    public void onSubscribe(Disposable d) {
        if (terminalEvent.get() != null) {
            d.dispose();
        }
    }

    @Override
    public void onNext(T t) {
        ExceptionHelper.nullCheck(t, "onNext called with a null value.");

        if (terminalEvent.get() != null) {
            return;
        }
        Object o = NotificationLite.next(t);
        setCurrent(o);
        for (BehaviorDisposable<T> bs : observers.get()) {
            bs.emitNext(o, index);
        }
    }

    @Override
    public void onError(Throwable t) {
        ExceptionHelper.nullCheck(t, "onError called with a null Throwable.");
        if (!terminalEvent.compareAndSet(null, t)) {
            RxJavaPlugins.onError(t);
            return;
        }
        Object o = NotificationLite.error(t);
        for (BehaviorDisposable<T> bs : terminate(o)) {
            bs.emitNext(o, index);
        }
    }

    @Override
    public void onComplete() {
        if (!terminalEvent.compareAndSet(null, ExceptionHelper.TERMINATED)) {
            return;
        }
        Object o = NotificationLite.complete();
        for (BehaviorDisposable<T> bs : terminate(o)) {
            bs.emitNext(o, index);  // relaxed read okay since this is the only mutator thread
        }
    }

    @Override
    @CheckReturnValue
    public boolean hasObservers() {
        return observers.get().length != 0;
    }

    @CheckReturnValue
    /* test support*/ int subscriberCount() {
        return observers.get().length;
    }

    @Override
    @Nullable
    @CheckReturnValue
    public Throwable getThrowable() {
        Object o = value.get();
        if (NotificationLite.isError(o)) {
            return NotificationLite.getError(o);
        }
        return null;
    }

    /**
     * Returns a single value the Subject currently has or null if no such value exists.
     * <p>The method is thread-safe.
     * @return a single value the Subject currently has or null if no such value exists
     */
    @Nullable
    @CheckReturnValue
    public T getValue() {
        Object o = value.get();
        if (NotificationLite.isComplete(o) || NotificationLite.isError(o)) {
            return null;
        }
        return NotificationLite.getValue(o);
    }

    @Override
    @CheckReturnValue
    public boolean hasComplete() {
        Object o = value.get();
        return NotificationLite.isComplete(o);
    }

    @Override
    @CheckReturnValue
    public boolean hasThrowable() {
        Object o = value.get();
        return NotificationLite.isError(o);
    }

    /**
     * Returns true if the subject has any value.
     * <p>The method is thread-safe.
     * @return true if the subject has any value
     */
    @CheckReturnValue
    public boolean hasValue() {
        Object o = value.get();
        return o != null && !NotificationLite.isComplete(o) && !NotificationLite.isError(o);
    }

    boolean add(BehaviorDisposable<T> rs) {
        for (;;) {
            BehaviorDisposable<T>[] a = observers.get();
            if (a == TERMINATED) {
                return false;
            }
            int len = a.length;
            @SuppressWarnings("unchecked")
            BehaviorDisposable<T>[] b = new BehaviorDisposable[len + 1];
            System.arraycopy(a, 0, b, 0, len);
            b[len] = rs;
            if (observers.compareAndSet(a, b)) {
                return true;
            }
        }
    }

    @SuppressWarnings("unchecked")
    void remove(BehaviorDisposable<T> rs) {
        for (;;) {
            BehaviorDisposable<T>[] a = observers.get();
            int len = a.length;
            if (len == 0) {
                return;
            }
            int j = -1;
            for (int i = 0; i < len; i++) {
                if (a[i] == rs) {
                    j = i;
                    break;
                }
            }

            if (j < 0) {
                return;
            }
            BehaviorDisposable<T>[] b;
            if (len == 1) {
                b = EMPTY;
            } else {
                b = new BehaviorDisposable[len - 1];
                System.arraycopy(a, 0, b, 0, j);
                System.arraycopy(a, j + 1, b, j, len - j - 1);
            }
            if (observers.compareAndSet(a, b)) {
                return;
            }
        }
    }

    @SuppressWarnings("unchecked")
    BehaviorDisposable<T>[] terminate(Object terminalValue) {

        setCurrent(terminalValue);

        return observers.getAndSet(TERMINATED);
    }

    void setCurrent(Object o) {
        writeLock.lock();
        index++;
        value.lazySet(o);
        writeLock.unlock();
    }

    static final class BehaviorDisposable<T> implements Disposable, NonThrowingPredicate<Object> {

        final Observer<? super T> downstream;
        final BehaviorSubject<T> state;

        boolean next;
        boolean emitting;
        AppendOnlyLinkedArrayList<Object> queue;

        boolean fastPath;

        volatile boolean cancelled;

        long index;

        BehaviorDisposable(Observer<? super T> actual, BehaviorSubject<T> state) {
            this.downstream = actual;
            this.state = state;
        }

        @Override
        public void dispose() {
            if (!cancelled) {
                cancelled = true;

                state.remove(this);
            }
        }

        @Override
        public boolean isDisposed() {
            return cancelled;
        }

        void emitFirst() {
            if (cancelled) {
                return;
            }
            Object o;
            synchronized (this) {
                if (cancelled) {
                    return;
                }
                if (next) {
                    return;
                }

                BehaviorSubject<T> s = state;
                Lock lock = s.readLock;

                lock.lock();
                index = s.index;
                o = s.value.get();
                lock.unlock();

                emitting = o != null;
                next = true;
            }

            if (o != null) {
                if (test(o)) {
                    return;
                }

                emitLoop();
            }
        }

        void emitNext(Object value, long stateIndex) {
            if (cancelled) {
                return;
            }
            if (!fastPath) {
                synchronized (this) {
                    if (cancelled) {
                        return;
                    }
                    if (index == stateIndex) {
                        return;
                    }
                    if (emitting) {
                        AppendOnlyLinkedArrayList<Object> q = queue;
                        if (q == null) {
                            q = new AppendOnlyLinkedArrayList<>(4);
                            queue = q;
                        }
                        q.add(value);
                        return;
                    }
                    next = true;
                }
                fastPath = true;
            }

            test(value);
        }

        @Override
        public boolean test(Object o) {
            return cancelled || NotificationLite.accept(o, downstream);
        }

        void emitLoop() {
            for (;;) {
                if (cancelled) {
                    return;
                }
                AppendOnlyLinkedArrayList<Object> q;
                synchronized (this) {
                    q = queue;
                    if (q == null) {
                        emitting = false;
                        return;
                    }
                    queue = null;
                }

                q.forEachWhile(this);
            }
        }
    }
}
