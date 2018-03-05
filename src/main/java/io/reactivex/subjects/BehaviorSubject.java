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
import java.lang.reflect.Array;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.*;

import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.functions.ObjectHelper;
import io.reactivex.internal.util.*;
import io.reactivex.internal.util.AppendOnlyLinkedArrayList.NonThrowingPredicate;
import io.reactivex.plugins.RxJavaPlugins;

/**
 * Subject that emits the most recent item it has observed and all subsequent observed items to each subscribed
 * {@link Observer}.
 * <p>
 * <img width="640" height="415" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/S.BehaviorSubject.png" alt="">
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
 * Since a {@code BehaviorSubject} is an {@link io.reactivex.Observable}, it does not support backpressure.
 * <p>
 * When this {@code BehaviorSubject} is terminated via {@link #onError(Throwable)} or {@link #onComplete()}, the
 * last observed item (if any) is cleared and late {@link io.reactivex.Observer}s only receive
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
 * in a non-blocking and thread-safe manner via {@link #hasValue()}, {@link #getValue()},
 * {@link #getValues()} or {@link #getValues(Object[])}.
 * <dl>
 *  <dt><b>Scheduler:</b></dt>
 *  <dd>{@code BehaviorSubject} does not operate by default on a particular {@link io.reactivex.Scheduler} and
 *  the {@code Observer}s get notified on the thread the respective {@code onXXX} methods were invoked.</dd>
 *  <dt><b>Error handling:</b></dt>
 *  <dd>When the {@link #onError(Throwable)} is called, the {@code BehaviorSubject} enters into a terminal state
 *  and emits the same {@code Throwable} instance to the last set of {@code Observer}s. During this emission,
 *  if one or more {@code Observer}s dispose their respective {@code Disposable}s, the
 *  {@code Throwable} is delivered to the global error handler via
 *  {@link io.reactivex.plugins.RxJavaPlugins#onError(Throwable)} (multiple times if multiple {@code Observer}s
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

    /** An empty array to avoid allocation in getValues(). */
    private static final Object[] EMPTY_ARRAY = new Object[0];

    final AtomicReference<Object> value;

    final AtomicReference<BehaviorDisposable<T>[]> subscribers;

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
    public static <T> BehaviorSubject<T> create() {
        return new BehaviorSubject<T>();
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
     */
    @CheckReturnValue
    public static <T> BehaviorSubject<T> createDefault(T defaultValue) {
        return new BehaviorSubject<T>(defaultValue);
    }

    /**
     * Constructs an empty BehaviorSubject.
     * @since 2.0
     */
    @SuppressWarnings("unchecked")
    BehaviorSubject() {
        this.lock = new ReentrantReadWriteLock();
        this.readLock = lock.readLock();
        this.writeLock = lock.writeLock();
        this.subscribers = new AtomicReference<BehaviorDisposable<T>[]>(EMPTY);
        this.value = new AtomicReference<Object>();
        this.terminalEvent = new AtomicReference<Throwable>();
    }

    /**
     * Constructs a BehaviorSubject with the given initial value.
     * @param defaultValue the initial value, not null (verified)
     * @throws NullPointerException if {@code defaultValue} is null
     * @since 2.0
     */
    BehaviorSubject(T defaultValue) {
        this();
        this.value.lazySet(ObjectHelper.requireNonNull(defaultValue, "defaultValue is null"));
    }

    @Override
    protected void subscribeActual(Observer<? super T> observer) {
        BehaviorDisposable<T> bs = new BehaviorDisposable<T>(observer, this);
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
    public void onSubscribe(Disposable s) {
        if (terminalEvent.get() != null) {
            s.dispose();
        }
    }

    @Override
    public void onNext(T t) {
        ObjectHelper.requireNonNull(t, "onNext called with null. Null values are generally not allowed in 2.x operators and sources.");

        if (terminalEvent.get() != null) {
            return;
        }
        Object o = NotificationLite.next(t);
        setCurrent(o);
        for (BehaviorDisposable<T> bs : subscribers.get()) {
            bs.emitNext(o, index);
        }
    }

    @Override
    public void onError(Throwable t) {
        ObjectHelper.requireNonNull(t, "onError called with null. Null values are generally not allowed in 2.x operators and sources.");
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
    public boolean hasObservers() {
        return subscribers.get().length != 0;
    }


    /* test support*/ int subscriberCount() {
        return subscribers.get().length;
    }

    @Override
    @Nullable
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
    public T getValue() {
        Object o = value.get();
        if (NotificationLite.isComplete(o) || NotificationLite.isError(o)) {
            return null;
        }
        return NotificationLite.getValue(o);
    }

    /**
     * Returns an Object array containing snapshot all values of the Subject.
     * <p>The method is thread-safe.
     * @return the array containing the snapshot of all values of the Subject
     */
    public Object[] getValues() {
        @SuppressWarnings("unchecked")
        T[] a = (T[])EMPTY_ARRAY;
        T[] b = getValues(a);
        if (b == EMPTY_ARRAY) {
            return new Object[0];
        }
        return b;

    }

    /**
     * Returns a typed array containing a snapshot of all values of the Subject.
     * <p>The method follows the conventions of Collection.toArray by setting the array element
     * after the last value to null (if the capacity permits).
     * <p>The method is thread-safe.
     * @param array the target array to copy values into if it fits
     * @return the given array if the values fit into it or a new array containing all values
     */
    @SuppressWarnings("unchecked")
    public T[] getValues(T[] array) {
        Object o = value.get();
        if (o == null || NotificationLite.isComplete(o) || NotificationLite.isError(o)) {
            if (array.length != 0) {
                array[0] = null;
            }
            return array;
        }
        T v = NotificationLite.getValue(o);
        if (array.length != 0) {
            array[0] = v;
            if (array.length != 1) {
                array[1] = null;
            }
        } else {
            array = (T[])Array.newInstance(array.getClass().getComponentType(), 1);
            array[0] = v;
        }
        return array;
    }

    @Override
    public boolean hasComplete() {
        Object o = value.get();
        return NotificationLite.isComplete(o);
    }

    @Override
    public boolean hasThrowable() {
        Object o = value.get();
        return NotificationLite.isError(o);
    }

    /**
     * Returns true if the subject has any value.
     * <p>The method is thread-safe.
     * @return true if the subject has any value
     */
    public boolean hasValue() {
        Object o = value.get();
        return o != null && !NotificationLite.isComplete(o) && !NotificationLite.isError(o);
    }

    boolean add(BehaviorDisposable<T> rs) {
        for (;;) {
            BehaviorDisposable<T>[] a = subscribers.get();
            if (a == TERMINATED) {
                return false;
            }
            int len = a.length;
            @SuppressWarnings("unchecked")
            BehaviorDisposable<T>[] b = new BehaviorDisposable[len + 1];
            System.arraycopy(a, 0, b, 0, len);
            b[len] = rs;
            if (subscribers.compareAndSet(a, b)) {
                return true;
            }
        }
    }

    @SuppressWarnings("unchecked")
    void remove(BehaviorDisposable<T> rs) {
        for (;;) {
            BehaviorDisposable<T>[] a = subscribers.get();
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
            if (subscribers.compareAndSet(a, b)) {
                return;
            }
        }
    }

    @SuppressWarnings("unchecked")
    BehaviorDisposable<T>[] terminate(Object terminalValue) {

        BehaviorDisposable<T>[] a = subscribers.getAndSet(TERMINATED);
        if (a != TERMINATED) {
            // either this or atomics with lots of allocation
            setCurrent(terminalValue);
        }

        return a;
    }

    void setCurrent(Object o) {
        writeLock.lock();
        index++;
        value.lazySet(o);
        writeLock.unlock();
    }

    static final class BehaviorDisposable<T> implements Disposable, NonThrowingPredicate<Object> {

        final Observer<? super T> actual;
        final BehaviorSubject<T> state;

        boolean next;
        boolean emitting;
        AppendOnlyLinkedArrayList<Object> queue;

        boolean fastPath;

        volatile boolean cancelled;

        long index;

        BehaviorDisposable(Observer<? super T> actual, BehaviorSubject<T> state) {
            this.actual = actual;
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
                            q = new AppendOnlyLinkedArrayList<Object>(4);
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
            return cancelled || NotificationLite.accept(o, actual);
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
