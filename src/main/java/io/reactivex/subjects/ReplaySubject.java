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

import java.lang.reflect.Array;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.*;

import io.reactivex.Observer;
import io.reactivex.Scheduler;
import io.reactivex.annotations.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.functions.ObjectHelper;
import io.reactivex.internal.util.NotificationLite;
import io.reactivex.plugins.RxJavaPlugins;

/**
 * Replays events (in a configurable bounded or unbounded manner) to current and late {@link Observer}s.
 * <p>
 * This subject does not have a public constructor by design; a new empty instance of this
 * {@code ReplaySubject} can be created via the following {@code create} methods that
 * allow specifying the retention policy for items:
 * <ul>
 * <li>{@link #create()} - creates an empty, unbounded {@code ReplaySubject} that
 *     caches all items and the terminal event it receives.
 * <p>
 * <img width="640" height="299" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/ReplaySubject.u.png" alt="">
 * <p>
 * <img width="640" height="398" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/ReplaySubject.ue.png" alt="">
 * </li>
 * <li>{@link #create(int)} - creates an empty, unbounded {@code ReplaySubject}
 *     with a hint about how many <b>total</b> items one expects to retain.
 * </li>
 * <li>{@link #createWithSize(int)} - creates an empty, size-bound {@code ReplaySubject}
 *     that retains at most the given number of the latest item it receives.
 * <p>
 * <img width="640" height="420" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/ReplaySubject.n.png" alt="">
 * </li>
 * <li>{@link #createWithTime(long, TimeUnit, Scheduler)} - creates an empty, time-bound
 *     {@code ReplaySubject} that retains items no older than the specified time amount.
 * <p>
 * <img width="640" height="415" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/ReplaySubject.t.png" alt="">
 * </li>
 * <li>{@link #createWithTimeAndSize(long, TimeUnit, Scheduler, int)} - creates an empty,
 *     time- and size-bound {@code ReplaySubject} that retains at most the given number
 *     items that are also not older than the specified time amount.
 * <p>
 * <img width="640" height="404" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/ReplaySubject.nt.png" alt="">
 * </li>
 * </ul>
 * <p>
 * Since a {@code Subject} is conceptionally derived from the {@code Processor} type in the Reactive Streams specification,
 * {@code null}s are not allowed (<a href="https://github.com/reactive-streams/reactive-streams-jvm#2.13">Rule 2.13</a>) as
 * parameters to {@link #onNext(Object)} and {@link #onError(Throwable)}. Such calls will result in a
 * {@link NullPointerException} being thrown and the subject's state is not changed.
 * <p>
 * Since a {@code ReplaySubject} is an {@link io.reactivex.Observable}, it does not support backpressure.
 * <p>
 * When this {@code ReplaySubject} is terminated via {@link #onError(Throwable)} or {@link #onComplete()},
 * late {@link io.reactivex.Observer}s will receive the retained/cached items first (if any) followed by the respective
 * terminal event. If the {@code ReplaySubject} has a time-bound, the age of the retained/cached items are still considered
 * when replaying and thus it may result in no items being emitted before the terminal event.
 * <p>
 * Once an {@code Observer} has subscribed, it will receive items continuously from that point on. Bounds only affect how
 * many past items a new {@code Observer} will receive before it catches up with the live event feed.
 * <p>
 * Even though {@code ReplaySubject} implements the {@code Observer} interface, calling
 * {@code onSubscribe} is not required (<a href="https://github.com/reactive-streams/reactive-streams-jvm#2.12">Rule 2.12</a>)
 * if the subject is used as a standalone source. However, calling {@code onSubscribe}
 * after the {@code ReplaySubject} reached its terminal state will result in the
 * given {@code Disposable} being disposed immediately.
 * <p>
 * Calling {@link #onNext(Object)}, {@link #onError(Throwable)} and {@link #onComplete()}
 * is required to be serialized (called from the same thread or called non-overlappingly from different threads
 * through external means of serialization). The {@link #toSerialized()} method available to all {@code Subject}s
 * provides such serialization and also protects against reentrance (i.e., when a downstream {@code Observer}
 * consuming this subject also wants to call {@link #onNext(Object)} on this subject recursively).
 * <p>
 * This {@code ReplaySubject} supports the standard state-peeking methods {@link #hasComplete()}, {@link #hasThrowable()},
 * {@link #getThrowable()} and {@link #hasObservers()} as well as means to read the retained/cached items
 * in a non-blocking and thread-safe manner via {@link #hasValue()}, {@link #getValue()},
 * {@link #getValues()} or {@link #getValues(Object[])}.
 * <p>
 * Note that due to concurrency requirements, a size- and time-bounded {@code ReplaySubject} may hold strong references to more
 * source emissions than specified while it isn't terminated yet. Use the {@link #cleanupBuffer()} to allow
 * such inaccessible items to be cleaned up by GC once no consumer references it anymore.
 * <dl>
 *  <dt><b>Scheduler:</b></dt>
 *  <dd>{@code ReplaySubject} does not operate by default on a particular {@link io.reactivex.Scheduler} and
 *  the {@code Observer}s get notified on the thread the respective {@code onXXX} methods were invoked.
 *  Time-bound {@code ReplaySubject}s use the given {@code Scheduler} in their {@code create} methods
 *  as time source to timestamp of items received for the age checks.</dd>
 *  <dt><b>Error handling:</b></dt>
 *  <dd>When the {@link #onError(Throwable)} is called, the {@code ReplaySubject} enters into a terminal state
 *  and emits the same {@code Throwable} instance to the last set of {@code Observer}s. During this emission,
 *  if one or more {@code Observer}s dispose their respective {@code Disposable}s, the
 *  {@code Throwable} is delivered to the global error handler via
 *  {@link io.reactivex.plugins.RxJavaPlugins#onError(Throwable)} (multiple times if multiple {@code Observer}s
 *  cancel at once).
 *  If there were no {@code Observer}s subscribed to this {@code ReplaySubject} when the {@code onError()}
 *  was called, the global error handler is not invoked.
 *  </dd>
 * </dl>
 * <p>
 * Example usage:
 * <pre> {@code

  ReplaySubject<Object> subject = ReplaySubject.create();
  subject.onNext("one");
  subject.onNext("two");
  subject.onNext("three");
  subject.onComplete();

  // both of the following will get the onNext/onComplete calls from above
  subject.subscribe(observer1);
  subject.subscribe(observer2);

  } </pre>
 *
 * @param <T> the value type
 */
public final class ReplaySubject<T> extends Subject<T> {
    final ReplayBuffer<T> buffer;

    final AtomicReference<ReplayDisposable<T>[]> observers;

    @SuppressWarnings("rawtypes")
    static final ReplayDisposable[] EMPTY = new ReplayDisposable[0];

    @SuppressWarnings("rawtypes")
    static final ReplayDisposable[] TERMINATED = new ReplayDisposable[0];

    boolean done;

    /**
     * Creates an unbounded replay subject.
     * <p>
     * The internal buffer is backed by an {@link ArrayList} and starts with an initial capacity of 16. Once the
     * number of items reaches this capacity, it will grow as necessary (usually by 50%). However, as the
     * number of items grows, this causes frequent array reallocation and copying, and may hurt performance
     * and latency. This can be avoided with the {@link #create(int)} overload which takes an initial capacity
     * parameter and can be tuned to reduce the array reallocation frequency as needed.
     *
     * @param <T>
     *          the type of items observed and emitted by the Subject
     * @return the created subject
     */
    @CheckReturnValue
    @NonNull
    public static <T> ReplaySubject<T> create() {
        return new ReplaySubject<T>(new UnboundedReplayBuffer<T>(16));
    }

    /**
     * Creates an unbounded replay subject with the specified initial buffer capacity.
     * <p>
     * Use this method to avoid excessive array reallocation while the internal buffer grows to accommodate new
     * items. For example, if you know that the buffer will hold 32k items, you can ask the
     * {@code ReplaySubject} to preallocate its internal array with a capacity to hold that many items. Once
     * the items start to arrive, the internal array won't need to grow, creating less garbage and no overhead
     * due to frequent array-copying.
     *
     * @param <T>
     *          the type of items observed and emitted by the Subject
     * @param capacityHint
     *          the initial buffer capacity
     * @return the created subject
     */
    @CheckReturnValue
    @NonNull
    public static <T> ReplaySubject<T> create(int capacityHint) {
        return new ReplaySubject<T>(new UnboundedReplayBuffer<T>(capacityHint));
    }

    /**
     * Creates a size-bounded replay subject.
     * <p>
     * In this setting, the {@code ReplaySubject} holds at most {@code size} items in its internal buffer and
     * discards the oldest item.
     * <p>
     * When observers subscribe to a terminated {@code ReplaySubject}, they are guaranteed to see at most
     * {@code size} {@code onNext} events followed by a termination event.
     * <p>
     * If an observer subscribes while the {@code ReplaySubject} is active, it will observe all items in the
     * buffer at that point in time and each item observed afterwards, even if the buffer evicts items due to
     * the size constraint in the mean time. In other words, once an Observer subscribes, it will receive items
     * without gaps in the sequence.
     *
     * @param <T>
     *          the type of items observed and emitted by the Subject
     * @param maxSize
     *          the maximum number of buffered items
     * @return the created subject
     */
    @CheckReturnValue
    @NonNull
    public static <T> ReplaySubject<T> createWithSize(int maxSize) {
        return new ReplaySubject<T>(new SizeBoundReplayBuffer<T>(maxSize));
    }

    /**
     * Creates an unbounded replay subject with the bounded-implementation for testing purposes.
     * <p>
     * This variant behaves like the regular unbounded {@code ReplaySubject} created via {@link #create()} but
     * uses the structures of the bounded-implementation. This is by no means intended for the replacement of
     * the original, array-backed and unbounded {@code ReplaySubject} due to the additional overhead of the
     * linked-list based internal buffer. The sole purpose is to allow testing and reasoning about the behavior
     * of the bounded implementations without the interference of the eviction policies.
     *
     * @param <T>
     *          the type of items observed and emitted by the Subject
     * @return the created subject
     */
    /* test */ static <T> ReplaySubject<T> createUnbounded() {
        return new ReplaySubject<T>(new SizeBoundReplayBuffer<T>(Integer.MAX_VALUE));
    }

    /**
     * Creates a time-bounded replay subject.
     * <p>
     * In this setting, the {@code ReplaySubject} internally tags each observed item with a timestamp value
     * supplied by the {@link Scheduler} and keeps only those whose age is less than the supplied time value
     * converted to milliseconds. For example, an item arrives at T=0 and the max age is set to 5; at T&gt;=5
     * this first item is then evicted by any subsequent item or termination event, leaving the buffer empty.
     * <p>
     * Once the subject is terminated, observers subscribing to it will receive items that remained in the
     * buffer after the terminal event, regardless of their age.
     * <p>
     * If an observer subscribes while the {@code ReplaySubject} is active, it will observe only those items
     * from within the buffer that have an age less than the specified time, and each item observed thereafter,
     * even if the buffer evicts items due to the time constraint in the mean time. In other words, once an
     * observer subscribes, it observes items without gaps in the sequence except for any outdated items at the
     * beginning of the sequence.
     * <p>
     * Note that terminal notifications ({@code onError} and {@code onComplete}) trigger eviction as well. For
     * example, with a max age of 5, the first item is observed at T=0, then an {@code onComplete} notification
     * arrives at T=10. If an observer subscribes at T=11, it will find an empty {@code ReplaySubject} with just
     * an {@code onComplete} notification.
     *
     * @param <T>
     *          the type of items observed and emitted by the Subject
     * @param maxAge
     *          the maximum age of the contained items
     * @param unit
     *          the time unit of {@code time}
     * @param scheduler
     *          the {@link Scheduler} that provides the current time
     * @return the created subject
     */
    @CheckReturnValue
    @NonNull
    public static <T> ReplaySubject<T> createWithTime(long maxAge, TimeUnit unit, Scheduler scheduler) {
        return new ReplaySubject<T>(new SizeAndTimeBoundReplayBuffer<T>(Integer.MAX_VALUE, maxAge, unit, scheduler));
    }

    /**
     * Creates a time- and size-bounded replay subject.
     * <p>
     * In this setting, the {@code ReplaySubject} internally tags each received item with a timestamp value
     * supplied by the {@link Scheduler} and holds at most {@code size} items in its internal buffer. It evicts
     * items from the start of the buffer if their age becomes less-than or equal to the supplied age in
     * milliseconds or the buffer reaches its {@code size} limit.
     * <p>
     * When observers subscribe to a terminated {@code ReplaySubject}, they observe the items that remained in
     * the buffer after the terminal notification, regardless of their age, but at most {@code size} items.
     * <p>
     * If an observer subscribes while the {@code ReplaySubject} is active, it will observe only those items
     * from within the buffer that have age less than the specified time and each subsequent item, even if the
     * buffer evicts items due to the time constraint in the mean time. In other words, once an observer
     * subscribes, it observes items without gaps in the sequence except for the outdated items at the beginning
     * of the sequence.
     * <p>
     * Note that terminal notifications ({@code onError} and {@code onComplete}) trigger eviction as well. For
     * example, with a max age of 5, the first item is observed at T=0, then an {@code onComplete} notification
     * arrives at T=10. If an observer subscribes at T=11, it will find an empty {@code ReplaySubject} with just
     * an {@code onComplete} notification.
     *
     * @param <T>
     *          the type of items observed and emitted by the Subject
     * @param maxAge
     *          the maximum age of the contained items
     * @param unit
     *          the time unit of {@code time}
     * @param maxSize
     *          the maximum number of buffered items
     * @param scheduler
     *          the {@link Scheduler} that provides the current time
     * @return the created subject
     */
    @CheckReturnValue
    @NonNull
    public static <T> ReplaySubject<T> createWithTimeAndSize(long maxAge, TimeUnit unit, Scheduler scheduler, int maxSize) {
        return new ReplaySubject<T>(new SizeAndTimeBoundReplayBuffer<T>(maxSize, maxAge, unit, scheduler));
    }

    /**
     * Constructs a ReplayProcessor with the given custom ReplayBuffer instance.
     * @param buffer the ReplayBuffer instance, not null (not verified)
     */
    @SuppressWarnings("unchecked")
    ReplaySubject(ReplayBuffer<T> buffer) {
        this.buffer = buffer;
        this.observers = new AtomicReference<ReplayDisposable<T>[]>(EMPTY);
    }

    @Override
    protected void subscribeActual(Observer<? super T> observer) {
        ReplayDisposable<T> rs = new ReplayDisposable<T>(observer, this);
        observer.onSubscribe(rs);

        if (!rs.cancelled) {
            if (add(rs)) {
                if (rs.cancelled) {
                    remove(rs);
                    return;
                }
            }
            buffer.replay(rs);
        }
    }

    @Override
    public void onSubscribe(Disposable s) {
        if (done) {
            s.dispose();
        }
    }

    @Override
    public void onNext(T t) {
        ObjectHelper.requireNonNull(t, "onNext called with null. Null values are generally not allowed in 2.x operators and sources.");
        if (done) {
            return;
        }

        ReplayBuffer<T> b = buffer;
        b.add(t);

        for (ReplayDisposable<T> rs : observers.get()) {
            b.replay(rs);
        }
    }

    @Override
    public void onError(Throwable t) {
        ObjectHelper.requireNonNull(t, "onError called with null. Null values are generally not allowed in 2.x operators and sources.");
        if (done) {
            RxJavaPlugins.onError(t);
            return;
        }
        done = true;

        Object o = NotificationLite.error(t);

        ReplayBuffer<T> b = buffer;

        b.addFinal(o);

        for (ReplayDisposable<T> rs : terminate(o)) {
            b.replay(rs);
        }
    }

    @Override
    public void onComplete() {
        if (done) {
            return;
        }
        done = true;

        Object o = NotificationLite.complete();

        ReplayBuffer<T> b = buffer;

        b.addFinal(o);

        for (ReplayDisposable<T> rs : terminate(o)) {
            b.replay(rs);
        }
    }

    @Override
    public boolean hasObservers() {
        return observers.get().length != 0;
    }

    /* test */ int observerCount() {
        return observers.get().length;
    }

    @Override
    @Nullable
    public Throwable getThrowable() {
        Object o = buffer.get();
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
        return buffer.getValue();
    }

    /**
     * Makes sure the item cached by the head node in a bounded
     * ReplaySubject is released (as it is never part of a replay).
     * <p>
     * By default, live bounded buffers will remember one item before
     * the currently receivable one to ensure subscribers can always
     * receive a continuous sequence of items. A terminated ReplaySubject
     * automatically releases this inaccessible item.
     * <p>
     * The method must be called sequentially, similar to the standard
     * {@code onXXX} methods.
     * <p>History: 2.1.11 - experimental
     * @since 2.2
     */
    public void cleanupBuffer() {
        buffer.trimHead();
    }

    /** An empty array to avoid allocation in getValues(). */
    private static final Object[] EMPTY_ARRAY = new Object[0];

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
    public T[] getValues(T[] array) {
        return buffer.getValues(array);
    }

    @Override
    public boolean hasComplete() {
        Object o = buffer.get();
        return NotificationLite.isComplete(o);
    }

    @Override
    public boolean hasThrowable() {
        Object o = buffer.get();
        return NotificationLite.isError(o);
    }

    /**
     * Returns true if the subject has any value.
     * <p>The method is thread-safe.
     * @return true if the subject has any value
     */
    public boolean hasValue() {
        return buffer.size() != 0; // NOPMD
    }

    /* test*/ int size() {
        return buffer.size();
    }

    boolean add(ReplayDisposable<T> rs) {
        for (;;) {
            ReplayDisposable<T>[] a = observers.get();
            if (a == TERMINATED) {
                return false;
            }
            int len = a.length;
            @SuppressWarnings("unchecked")
            ReplayDisposable<T>[] b = new ReplayDisposable[len + 1];
            System.arraycopy(a, 0, b, 0, len);
            b[len] = rs;
            if (observers.compareAndSet(a, b)) {
                return true;
            }
        }
    }

    @SuppressWarnings("unchecked")
    void remove(ReplayDisposable<T> rs) {
        for (;;) {
            ReplayDisposable<T>[] a = observers.get();
            if (a == TERMINATED || a == EMPTY) {
                return;
            }
            int len = a.length;
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
            ReplayDisposable<T>[] b;
            if (len == 1) {
                b = EMPTY;
            } else {
                b = new ReplayDisposable[len - 1];
                System.arraycopy(a, 0, b, 0, j);
                System.arraycopy(a, j + 1, b, j, len - j - 1);
            }
            if (observers.compareAndSet(a, b)) {
                return;
            }
        }
    }

    @SuppressWarnings("unchecked")
    ReplayDisposable<T>[] terminate(Object terminalValue) {
        if (buffer.compareAndSet(null, terminalValue)) {
            return observers.getAndSet(TERMINATED);
        }
        return TERMINATED;
    }

    /**
     * Abstraction over a buffer that receives events and replays them to
     * individual Observers.
     *
     * @param <T> the value type
     */
    interface ReplayBuffer<T> {

        void add(T value);

        void addFinal(Object notificationLite);

        void replay(ReplayDisposable<T> rs);

        int size();

        @Nullable
        T getValue();

        T[] getValues(T[] array);
        /**
         * Returns the terminal NotificationLite object or null if not yet terminated.
         * @return the terminal NotificationLite object or null if not yet terminated
         */
        Object get();

        /**
         * Atomically compares and sets the next terminal NotificationLite object if the
         * current equals to the expected NotificationLite object.
         * @param expected the expected NotificationLite object
         * @param next the next NotificationLite object
         * @return true if successful
         */
        boolean compareAndSet(Object expected, Object next);

        /**
         * Make sure an old inaccessible head value is released
         * in a bounded buffer.
         */
        void trimHead();
    }

    static final class ReplayDisposable<T> extends AtomicInteger implements Disposable {

        private static final long serialVersionUID = 466549804534799122L;
        final Observer<? super T> actual;
        final ReplaySubject<T> state;

        Object index;

        volatile boolean cancelled;

        ReplayDisposable(Observer<? super T> actual, ReplaySubject<T> state) {
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
    }

    static final class UnboundedReplayBuffer<T>
    extends AtomicReference<Object>
    implements ReplayBuffer<T> {

        private static final long serialVersionUID = -733876083048047795L;

        final List<Object> buffer;

        volatile boolean done;

        volatile int size;

        UnboundedReplayBuffer(int capacityHint) {
            this.buffer = new ArrayList<Object>(ObjectHelper.verifyPositive(capacityHint, "capacityHint"));
        }

        @Override
        public void add(T value) {
            buffer.add(value);
            size++;
        }

        @Override
        public void addFinal(Object notificationLite) {
            buffer.add(notificationLite);
            trimHead();
            size++;
            done = true;
        }

        @Override
        public void trimHead() {
            // no-op in this type of buffer
        }

        @Override
        @Nullable
        @SuppressWarnings("unchecked")
        public T getValue() {
            int s = size;
            if (s != 0) {
                List<Object> b = buffer;
                Object o = b.get(s - 1);
                if (NotificationLite.isComplete(o) || NotificationLite.isError(o)) {
                    if (s == 1) {
                        return null;
                    }
                    return (T)b.get(s - 2);
                }
                return (T)o;
            }
            return null;
        }

        @Override
        @SuppressWarnings("unchecked")
        public T[] getValues(T[] array) {
            int s = size;
            if (s == 0) {
                if (array.length != 0) {
                    array[0] = null;
                }
                return array;
            }
            List<Object> b = buffer;
            Object o = b.get(s - 1);

            if (NotificationLite.isComplete(o) || NotificationLite.isError(o)) {
                s--;
                if (s == 0) {
                    if (array.length != 0) {
                        array[0] = null;
                    }
                    return array;
                }
            }


            if (array.length < s) {
                array = (T[])Array.newInstance(array.getClass().getComponentType(), s);
            }
            for (int i = 0; i < s; i++) {
                array[i] = (T)b.get(i);
            }
            if (array.length > s) {
                array[s] = null;
            }

            return array;
        }

        @Override
        @SuppressWarnings("unchecked")
        public void replay(ReplayDisposable<T> rs) {
            if (rs.getAndIncrement() != 0) {
                return;
            }

            int missed = 1;
            final List<Object> b = buffer;
            final Observer<? super T> a = rs.actual;

            Integer indexObject = (Integer)rs.index;
            int index;
            if (indexObject != null) {
                index = indexObject;
            } else {
                index = 0;
                rs.index = 0;
            }

            for (;;) {

                if (rs.cancelled) {
                    rs.index = null;
                    return;
                }

                int s = size;

                while (s != index) {

                    if (rs.cancelled) {
                        rs.index = null;
                        return;
                    }

                    Object o = b.get(index);

                    if (done) {
                        if (index + 1 == s) {
                            s = size;
                            if (index + 1 == s) {
                                if (NotificationLite.isComplete(o)) {
                                    a.onComplete();
                                } else {
                                    a.onError(NotificationLite.getError(o));
                                }
                                rs.index = null;
                                rs.cancelled = true;
                                return;
                            }
                        }
                    }

                    a.onNext((T)o);
                    index++;
                }

                if (index != size) {
                    continue;
                }

                rs.index = index;

                missed = rs.addAndGet(-missed);
                if (missed == 0) {
                    break;
                }
            }
        }

        @Override
        public int size() {
            int s = size;
            if (s != 0) {
                Object o = buffer.get(s - 1);
                if (NotificationLite.isComplete(o) || NotificationLite.isError(o)) {
                    return s - 1;
                }
                return s;
            }
            return 0;
        }
    }

    static final class Node<T> extends AtomicReference<Node<T>> {

        private static final long serialVersionUID = 6404226426336033100L;

        final T value;

        Node(T value) {
            this.value = value;
        }
    }

    static final class TimedNode<T> extends AtomicReference<TimedNode<T>> {

        private static final long serialVersionUID = 6404226426336033100L;

        final T value;
        final long time;

        TimedNode(T value, long time) {
            this.value = value;
            this.time = time;
        }
    }

    static final class SizeBoundReplayBuffer<T>
    extends AtomicReference<Object>
    implements ReplayBuffer<T> {

        private static final long serialVersionUID = 1107649250281456395L;

        final int maxSize;
        int size;

        volatile Node<Object> head;

        Node<Object> tail;

        volatile boolean done;

        SizeBoundReplayBuffer(int maxSize) {
            this.maxSize = ObjectHelper.verifyPositive(maxSize, "maxSize");
            Node<Object> h = new Node<Object>(null);
            this.tail = h;
            this.head = h;
        }

        void trim() {
            if (size > maxSize) {
                size--;
                Node<Object> h = head;
                head = h.get();
            }
        }

        @Override
        public void add(T value) {
            Node<Object> n = new Node<Object>(value);
            Node<Object> t = tail;

            tail = n;
            size++;
            t.set(n); // releases both the tail and size

            trim();
        }

        @Override
        public void addFinal(Object notificationLite) {
            Node<Object> n = new Node<Object>(notificationLite);
            Node<Object> t = tail;

            tail = n;
            size++;
            t.lazySet(n); // releases both the tail and size

            trimHead();
            done = true;
        }

        /**
         * Replace a non-empty head node with an empty one to
         * allow the GC of the inaccessible old value.
         */
        @Override
        public void trimHead() {
            Node<Object> h = head;
            if (h.value != null) {
                Node<Object> n = new Node<Object>(null);
                n.lazySet(h.get());
                head = n;
            }
        }

        @Override
        @Nullable
        @SuppressWarnings("unchecked")
        public T getValue() {
            Node<Object> prev = null;
            Node<Object> h = head;

            for (;;) {
                Node<Object> next = h.get();
                if (next == null) {
                    break;
                }
                prev = h;
                h = next;
            }

            Object v = h.value;
            if (v == null) {
                return null;
            }
            if (NotificationLite.isComplete(v) || NotificationLite.isError(v)) {
                return (T)prev.value;
            }

            return (T)v;
        }

        @Override
        @SuppressWarnings("unchecked")
        public T[] getValues(T[] array) {
            Node<Object> h = head;
            int s = size();

            if (s == 0) {
                if (array.length != 0) {
                    array[0] = null;
                }
            } else {
                if (array.length < s) {
                    array = (T[])Array.newInstance(array.getClass().getComponentType(), s);
                }

                int i = 0;
                while (i != s) {
                    Node<Object> next = h.get();
                    array[i] = (T)next.value;
                    i++;
                    h = next;
                }
                if (array.length > s) {
                    array[s] = null;
                }
            }

            return array;
        }

        @Override
        @SuppressWarnings("unchecked")
        public void replay(ReplayDisposable<T> rs) {
            if (rs.getAndIncrement() != 0) {
                return;
            }

            int missed = 1;
            final Observer<? super T> a = rs.actual;

            Node<Object> index = (Node<Object>)rs.index;
            if (index == null) {
                index = head;
            }

            for (;;) {

                for (;;) {
                    if (rs.cancelled) {
                        rs.index = null;
                        return;
                    }

                    Node<Object> n = index.get();

                    if (n == null) {
                        break;
                    }

                    Object o = n.value;

                    if (done) {
                        if (n.get() == null) {

                            if (NotificationLite.isComplete(o)) {
                                a.onComplete();
                            } else {
                                a.onError(NotificationLite.getError(o));
                            }
                            rs.index = null;
                            rs.cancelled = true;
                            return;
                        }
                    }

                    a.onNext((T)o);

                    index = n;
                }

                if (index.get() != null) {
                    continue;
                }

                rs.index = index;

                missed = rs.addAndGet(-missed);
                if (missed == 0) {
                    break;
                }
            }
        }

        @Override
        public int size() {
            int s = 0;
            Node<Object> h = head;
            while (s != Integer.MAX_VALUE) {
                Node<Object> next = h.get();
                if (next == null) {
                    Object o = h.value;
                    if (NotificationLite.isComplete(o) || NotificationLite.isError(o)) {
                        s--;
                    }
                    break;
                }
                s++;
                h = next;
            }

            return s;
        }
    }

    static final class SizeAndTimeBoundReplayBuffer<T>
    extends AtomicReference<Object>
    implements ReplayBuffer<T> {

        private static final long serialVersionUID = -8056260896137901749L;

        final int maxSize;
        final long maxAge;
        final TimeUnit unit;
        final Scheduler scheduler;
        int size;

        volatile TimedNode<Object> head;

        TimedNode<Object> tail;

        volatile boolean done;


        SizeAndTimeBoundReplayBuffer(int maxSize, long maxAge, TimeUnit unit, Scheduler scheduler) {
            this.maxSize = ObjectHelper.verifyPositive(maxSize, "maxSize");
            this.maxAge = ObjectHelper.verifyPositive(maxAge, "maxAge");
            this.unit = ObjectHelper.requireNonNull(unit, "unit is null");
            this.scheduler = ObjectHelper.requireNonNull(scheduler, "scheduler is null");
            TimedNode<Object> h = new TimedNode<Object>(null, 0L);
            this.tail = h;
            this.head = h;
        }

        void trim() {
            if (size > maxSize) {
                size--;
                TimedNode<Object> h = head;
                head = h.get();
            }
            long limit = scheduler.now(unit) - maxAge;

            TimedNode<Object> h = head;

            for (;;) {
                TimedNode<Object> next = h.get();
                if (next == null) {
                    head = h;
                    break;
                }

                if (next.time > limit) {
                    head = h;
                    break;
                }

                h = next;
            }

        }

        void trimFinal() {
            long limit = scheduler.now(unit) - maxAge;

            TimedNode<Object> h = head;

            for (;;) {
                TimedNode<Object> next = h.get();
                if (next.get() == null) {
                    if (h.value != null) {
                        TimedNode<Object> lasth = new TimedNode<Object>(null, 0L);
                        lasth.lazySet(h.get());
                        head = lasth;
                    } else {
                        head = h;
                    }
                    break;
                }

                if (next.time > limit) {
                    if (h.value != null) {
                        TimedNode<Object> lasth = new TimedNode<Object>(null, 0L);
                        lasth.lazySet(h.get());
                        head = lasth;
                    } else {
                        head = h;
                    }
                    break;
                }

                h = next;
            }
        }

        @Override
        public void add(T value) {
            TimedNode<Object> n = new TimedNode<Object>(value, scheduler.now(unit));
            TimedNode<Object> t = tail;

            tail = n;
            size++;
            t.set(n); // releases both the tail and size

            trim();
        }

        @Override
        public void addFinal(Object notificationLite) {
            TimedNode<Object> n = new TimedNode<Object>(notificationLite, Long.MAX_VALUE);
            TimedNode<Object> t = tail;

            tail = n;
            size++;
            t.lazySet(n); // releases both the tail and size
            trimFinal();

            done = true;
        }

        /**
         * Replace a non-empty head node with an empty one to
         * allow the GC of the inaccessible old value.
         */
        @Override
        public void trimHead() {
            TimedNode<Object> h = head;
            if (h.value != null) {
                TimedNode<Object> n = new TimedNode<Object>(null, 0);
                n.lazySet(h.get());
                head = n;
            }
        }

        @Override
        @Nullable
        @SuppressWarnings("unchecked")
        public T getValue() {
            TimedNode<Object> prev = null;
            TimedNode<Object> h = head;

            for (;;) {
                TimedNode<Object> next = h.get();
                if (next == null) {
                    break;
                }
                prev = h;
                h = next;
            }

            long limit = scheduler.now(unit) - maxAge;
            if (h.time < limit) {
                return null;
            }

            Object v = h.value;
            if (v == null) {
                return null;
            }
            if (NotificationLite.isComplete(v) || NotificationLite.isError(v)) {
                return (T)prev.value;
            }

            return (T)v;
        }

        TimedNode<Object> getHead() {
            TimedNode<Object> index = head;
            // skip old entries
            long limit = scheduler.now(unit) - maxAge;
            TimedNode<Object> next = index.get();
            while (next != null) {
                long ts = next.time;
                if (ts > limit) {
                    break;
                }
                index = next;
                next = index.get();
            }
            return index;
        }

        @Override
        @SuppressWarnings("unchecked")
        public T[] getValues(T[] array) {
            TimedNode<Object> h = getHead();
            int s = size(h);

            if (s == 0) {
                if (array.length != 0) {
                    array[0] = null;
                }
            } else {
                if (array.length < s) {
                    array = (T[])Array.newInstance(array.getClass().getComponentType(), s);
                }

                int i = 0;
                while (i != s) {
                    TimedNode<Object> next = h.get();
                    array[i] = (T)next.value;
                    i++;
                    h = next;
                }
                if (array.length > s) {
                    array[s] = null;
                }
            }

            return array;
        }

        @Override
        @SuppressWarnings("unchecked")
        public void replay(ReplayDisposable<T> rs) {
            if (rs.getAndIncrement() != 0) {
                return;
            }

            int missed = 1;
            final Observer<? super T> a = rs.actual;

            TimedNode<Object> index = (TimedNode<Object>)rs.index;
            if (index == null) {
                index = getHead();
            }

            for (;;) {

                if (rs.cancelled) {
                    rs.index = null;
                    return;
                }

                for (;;) {
                    if (rs.cancelled) {
                        rs.index = null;
                        return;
                    }

                    TimedNode<Object> n = index.get();

                    if (n == null) {
                        break;
                    }

                    Object o = n.value;

                    if (done) {
                        if (n.get() == null) {

                            if (NotificationLite.isComplete(o)) {
                                a.onComplete();
                            } else {
                                a.onError(NotificationLite.getError(o));
                            }
                            rs.index = null;
                            rs.cancelled = true;
                            return;
                        }
                    }

                    a.onNext((T)o);

                    index = n;
                }

                if (index.get() != null) {
                    continue;
                }

                rs.index = index;

                missed = rs.addAndGet(-missed);
                if (missed == 0) {
                    break;
                }
            }
        }

        @Override
        public int size() {
            return size(getHead());
        }

        int size(TimedNode<Object> h) {
            int s = 0;
            while (s != Integer.MAX_VALUE) {
                TimedNode<Object> next = h.get();
                if (next == null) {
                    Object o = h.value;
                    if (NotificationLite.isComplete(o) || NotificationLite.isError(o)) {
                        s--;
                    }
                    break;
                }
                s++;
                h = next;
            }

            return s;
        }
    }
}
