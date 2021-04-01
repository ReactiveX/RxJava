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

package io.reactivex.rxjava3.processors;

import java.lang.reflect.Array;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.*;

import org.reactivestreams.*;

import io.reactivex.rxjava3.annotations.*;
import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.internal.functions.ObjectHelper;
import io.reactivex.rxjava3.internal.subscriptions.SubscriptionHelper;
import io.reactivex.rxjava3.internal.util.*;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;

/**
 * Replays events to Subscribers.
 * <p>
 * The {@code ReplayProcessor} supports the following item retention strategies:
 * <ul>
 * <li>{@link #create()} and {@link #create(int)}: retains and replays all events to current and
 * future {@code Subscriber}s.
 * <p>
 * <img width="640" height="269" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/ReplayProcessor.u.png" alt="">
 * <p>
 * <img width="640" height="345" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/ReplayProcessor.ue.png" alt="">
 * </li>
 * <li>{@link #createWithSize(int)}: retains at most the given number of items and replays only these
 * latest items to new {@code Subscriber}s.
 * <p>
 * <img width="640" height="332" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/ReplayProcessor.n.png" alt="">
 * </li>
 * <li>{@link #createWithTime(long, TimeUnit, Scheduler)}: retains items no older than the specified time
 * and replays them to new {@code Subscriber}s (which could mean all items age out).
 * <p>
 * <img width="640" height="415" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/ReplayProcessor.t.png" alt="">
 * </li>
 * <li>{@link #createWithTimeAndSize(long, TimeUnit, Scheduler, int)}: retains no more than the given number of items
 * which are also no older than the specified time and replays them to new {@code Subscriber}s (which could mean all items age out).
 * <p>
 * <img width="640" height="404" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/ReplayProcessor.nt.png" alt="">
 * </li>
 * </ul>
 * <p>
 * The {@code ReplayProcessor} can be created in bounded and unbounded mode. It can be bounded by
 * size (maximum number of elements retained at most) and/or time (maximum age of elements replayed).
 * <p>
 * Since a {@code ReplayProcessor} is a Reactive Streams {@code Processor},
 * {@code null}s are not allowed (<a href="https://github.com/reactive-streams/reactive-streams-jvm#2.13">Rule 2.13</a>) as
 * parameters to {@link #onNext(Object)} and {@link #onError(Throwable)}. Such calls will result in a
 * {@link NullPointerException} being thrown and the processor's state is not changed.
 * <p>
 * This {@code ReplayProcessor} respects the individual backpressure behavior of its {@code Subscriber}s but
 * does not coordinate their request amounts towards the upstream (because there might not be any) and
 * consumes the upstream in an unbounded manner (requesting {@link Long#MAX_VALUE}).
 * Note that {@code Subscriber}s receive a continuous sequence of values after they subscribed even
 * if an individual item gets delayed due to backpressure.
 * Due to concurrency requirements, a size-bounded {@code ReplayProcessor} may hold strong references to more source
 * emissions than specified.
 * <p>
 * When this {@code ReplayProcessor} is terminated via {@link #onError(Throwable)} or {@link #onComplete()},
 * late {@link Subscriber}s will receive the retained/cached items first (if any) followed by the respective
 * terminal event. If the {@code ReplayProcessor} has a time-bound, the age of the retained/cached items are still considered
 * when replaying and thus it may result in no items being emitted before the terminal event.
 * <p>
 * Once an {@code Subscriber} has subscribed, it will receive items continuously from that point on. Bounds only affect how
 * many past items a new {@code Subscriber} will receive before it catches up with the live event feed.
 * <p>
 * Even though {@code ReplayProcessor} implements the {@code Subscriber} interface, calling
 * {@code onSubscribe} is not required (<a href="https://github.com/reactive-streams/reactive-streams-jvm#2.12">Rule 2.12</a>)
 * if the processor is used as a standalone source. However, calling {@code onSubscribe}
 * after the {@code ReplayProcessor} reached its terminal state will result in the
 * given {@code Subscription} being canceled immediately.
 * <p>
 * Calling {@link #onNext(Object)}, {@link #onError(Throwable)} and {@link #onComplete()}
 * is required to be serialized (called from the same thread or called non-overlappingly from different threads
 * through external means of serialization). The {@link #toSerialized()} method available to all {@code FlowableProcessor}s
 * provides such serialization and also protects against reentrance (i.e., when a downstream {@code Subscriber}
 * consuming this processor also wants to call {@link #onNext(Object)} on this processor recursively).
 * <p>
 * This {@code ReplayProcessor} supports the standard state-peeking methods {@link #hasComplete()}, {@link #hasThrowable()},
 * {@link #getThrowable()} and {@link #hasSubscribers()} as well as means to read the retained/cached items
 * in a non-blocking and thread-safe manner via {@link #hasValue()}, {@link #getValue()},
 * {@link #getValues()} or {@link #getValues(Object[])}.
 * <p>
 * Note that due to concurrency requirements, a size- and time-bounded {@code ReplayProcessor} may hold strong references to more
 * source emissions than specified while it isn't terminated yet. Use the {@link #cleanupBuffer()} to allow
 * such inaccessible items to be cleaned up by GC once no consumer references them anymore.
 * <dl>
 *  <dt><b>Backpressure:</b></dt>
 *  <dd>This {@code ReplayProcessor} respects the individual backpressure behavior of its {@code Subscriber}s but
 *  does not coordinate their request amounts towards the upstream (because there might not be any) and
 *  consumes the upstream in an unbounded manner (requesting {@link Long#MAX_VALUE}).
 *  Note that {@code Subscriber}s receive a continuous sequence of values after they subscribed even
 *  if an individual item gets delayed due to backpressure.</dd>
 *  <dt><b>Scheduler:</b></dt>
 *  <dd>{@code ReplayProcessor} does not operate by default on a particular {@link io.reactivex.rxjava3.core.Scheduler} and
 *  the {@code Subscriber}s get notified on the thread the respective {@code onXXX} methods were invoked.
 *  Time-bound {@code ReplayProcessor}s use the given {@code Scheduler} in their {@code create} methods
 *  as time source to timestamp of items received for the age checks.</dd>
 *  <dt><b>Error handling:</b></dt>
 *  <dd>When the {@link #onError(Throwable)} is called, the {@code ReplayProcessor} enters into a terminal state
 *  and emits the same {@code Throwable} instance to the last set of {@code Subscriber}s. During this emission,
 *  if one or more {@code Subscriber}s cancel their respective {@code Subscription}s, the
 *  {@code Throwable} is delivered to the global error handler via
 *  {@link io.reactivex.rxjava3.plugins.RxJavaPlugins#onError(Throwable)} (multiple times if multiple {@code Subscriber}s
 *  cancel at once).
 *  If there were no {@code Subscriber}s subscribed to this {@code ReplayProcessor} when the {@code onError()}
 *  was called, the global error handler is not invoked.
 *  </dd>
 * </dl>
 * <p>
 * Example usage:
 * <pre> {@code

  ReplayProcessor<Object> processor = new ReplayProcessor<T>();
  processor.onNext("one");
  processor.onNext("two");
  processor.onNext("three");
  processor.onComplete();

  // both of the following will get the onNext/onComplete calls from above
  processor.subscribe(subscriber1);
  processor.subscribe(subscriber2);

  } </pre>
 *
 * @param <T> the value type
 */
public final class ReplayProcessor<T> extends FlowableProcessor<T> {
    /** An empty array to avoid allocation in getValues(). */
    private static final Object[] EMPTY_ARRAY = new Object[0];

    final ReplayBuffer<T> buffer;

    boolean done;

    final AtomicReference<ReplaySubscription<T>[]> subscribers;

    @SuppressWarnings("rawtypes")
    static final ReplaySubscription[] EMPTY = new ReplaySubscription[0];

    @SuppressWarnings("rawtypes")
    static final ReplaySubscription[] TERMINATED = new ReplaySubscription[0];

    /**
     * Creates an unbounded ReplayProcessor.
     * <p>
     * The internal buffer is backed by an {@link ArrayList} and starts with an initial capacity of 16. Once the
     * number of items reaches this capacity, it will grow as necessary (usually by 50%). However, as the
     * number of items grows, this causes frequent array reallocation and copying, and may hurt performance
     * and latency. This can be avoided with the {@link #create(int)} overload which takes an initial capacity
     * parameter and can be tuned to reduce the array reallocation frequency as needed.
     *
     * @param <T>
     *          the type of items observed and emitted by the ReplayProcessor
     * @return the created ReplayProcessor
     */
    @CheckReturnValue
    @NonNull
    public static <T> ReplayProcessor<T> create() {
        return new ReplayProcessor<>(new UnboundedReplayBuffer<>(16));
    }

    /**
     * Creates an unbounded ReplayProcessor with the specified initial buffer capacity.
     * <p>
     * Use this method to avoid excessive array reallocation while the internal buffer grows to accommodate new
     * items. For example, if you know that the buffer will hold 32k items, you can ask the
     * {@code ReplayProcessor} to preallocate its internal array with a capacity to hold that many items. Once
     * the items start to arrive, the internal array won't need to grow, creating less garbage and no overhead
     * due to frequent array-copying.
     *
     * @param <T>
     *          the type of items observed and emitted by this type of processor
     * @param capacityHint
     *          the initial buffer capacity
     * @return the created processor
     * @throws IllegalArgumentException if {@code capacityHint} is non-positive
     */
    @CheckReturnValue
    @NonNull
    public static <T> ReplayProcessor<T> create(int capacityHint) {
        ObjectHelper.verifyPositive(capacityHint, "capacityHint");
        return new ReplayProcessor<>(new UnboundedReplayBuffer<>(capacityHint));
    }

    /**
     * Creates a size-bounded ReplayProcessor.
     * <p>
     * In this setting, the {@code ReplayProcessor} holds at most {@code size} items in its internal buffer and
     * discards the oldest item.
     * <p>
     * When {@code Subscriber}s subscribe to a terminated {@code ReplayProcessor}, they are guaranteed to see at most
     * {@code size} {@code onNext} events followed by a termination event.
     * <p>
     * If a {@code Subscriber} subscribes while the {@code ReplayProcessor} is active, it will observe all items in the
     * buffer at that point in time and each item observed afterwards, even if the buffer evicts items due to
     * the size constraint in the mean time. In other words, once a {@code Subscriber} subscribes, it will receive items
     * without gaps in the sequence.
     *
     * @param <T>
     *          the type of items observed and emitted by this type of processor
     * @param maxSize
     *          the maximum number of buffered items
     * @return the created processor
     * @throws IllegalArgumentException if {@code maxSize} is non-positive
     */
    @CheckReturnValue
    @NonNull
    public static <T> ReplayProcessor<T> createWithSize(int maxSize) {
        ObjectHelper.verifyPositive(maxSize, "maxSize");
        return new ReplayProcessor<>(new SizeBoundReplayBuffer<>(maxSize));
    }

    /**
     * Creates an unbounded ReplayProcessor with the bounded-implementation for testing purposes.
     * <p>
     * This variant behaves like the regular unbounded {@code ReplayProcessor} created via {@link #create()} but
     * uses the structures of the bounded-implementation. This is by no means intended for the replacement of
     * the original, array-backed and unbounded {@code ReplayProcessor} due to the additional overhead of the
     * linked-list based internal buffer. The sole purpose is to allow testing and reasoning about the behavior
     * of the bounded implementations without the interference of the eviction policies.
     *
     * @param <T>
     *          the type of items observed and emitted by this type of processor
     * @return the created processor
     */
    @CheckReturnValue
    /* test */ static <T> ReplayProcessor<T> createUnbounded() {
        return new ReplayProcessor<>(new SizeBoundReplayBuffer<>(Integer.MAX_VALUE));
    }

    /**
     * Creates a time-bounded ReplayProcessor.
     * <p>
     * In this setting, the {@code ReplayProcessor} internally tags each observed item with a timestamp value
     * supplied by the {@link Scheduler} and keeps only those whose age is less than the supplied time value
     * converted to milliseconds. For example, an item arrives at T=0 and the max age is set to 5; at T&gt;=5
     * this first item is then evicted by any subsequent item or termination event, leaving the buffer empty.
     * <p>
     * Once the processor is terminated, {@code Subscriber}s subscribing to it will receive items that remained in the
     * buffer after the terminal event, regardless of their age.
     * <p>
     * If a {@code Subscriber} subscribes while the {@code ReplayProcessor} is active, it will observe only those items
     * from within the buffer that have an age less than the specified time, and each item observed thereafter,
     * even if the buffer evicts items due to the time constraint in the mean time. In other words, once a
     * {@code Subscriber} subscribes, it observes items without gaps in the sequence except for any outdated items at the
     * beginning of the sequence.
     * <p>
     * Note that terminal notifications ({@code onError} and {@code onComplete}) trigger eviction as well. For
     * example, with a max age of 5, the first item is observed at T=0, then an {@code onComplete} notification
     * arrives at T=10. If a {@code Subscriber} subscribes at T=11, it will find an empty {@code ReplayProcessor} with just
     * an {@code onComplete} notification.
     *
     * @param <T>
     *          the type of items observed and emitted by this type of processor
     * @param maxAge
     *          the maximum age of the contained items
     * @param unit
     *          the time unit of {@code time}
     * @param scheduler
     *          the {@link Scheduler} that provides the current time
     * @return the created processor
     * @throws NullPointerException if {@code unit} or {@code scheduler} is {@code null}
     * @throws IllegalArgumentException if {@code maxAge} is non-positive
     */
    @CheckReturnValue
    @NonNull
    public static <T> ReplayProcessor<T> createWithTime(long maxAge, @NonNull TimeUnit unit, @NonNull Scheduler scheduler) {
        ObjectHelper.verifyPositive(maxAge, "maxAge");
        Objects.requireNonNull(unit, "unit is null");
        Objects.requireNonNull(scheduler, "scheduler is null");
        return new ReplayProcessor<>(new SizeAndTimeBoundReplayBuffer<>(Integer.MAX_VALUE, maxAge, unit, scheduler));
    }

    /**
     * Creates a time- and size-bounded ReplayProcessor.
     * <p>
     * In this setting, the {@code ReplayProcessor} internally tags each received item with a timestamp value
     * supplied by the {@link Scheduler} and holds at most {@code size} items in its internal buffer. It evicts
     * items from the start of the buffer if their age becomes less-than or equal to the supplied age in
     * milliseconds or the buffer reaches its {@code size} limit.
     * <p>
     * When {@code Subscriber}s subscribe to a terminated {@code ReplayProcessor}, they observe the items that remained in
     * the buffer after the terminal notification, regardless of their age, but at most {@code size} items.
     * <p>
     * If a {@code Subscriber} subscribes while the {@code ReplayProcessor} is active, it will observe only those items
     * from within the buffer that have age less than the specified time and each subsequent item, even if the
     * buffer evicts items due to the time constraint in the mean time. In other words, once a {@code Subscriber}
     * subscribes, it observes items without gaps in the sequence except for the outdated items at the beginning
     * of the sequence.
     * <p>
     * Note that terminal notifications ({@code onError} and {@code onComplete}) trigger eviction as well. For
     * example, with a max age of 5, the first item is observed at T=0, then an {@code onComplete} notification
     * arrives at T=10. If a {@code Subscriber} subscribes at T=11, it will find an empty {@code ReplayProcessor} with just
     * an {@code onComplete} notification.
     *
     * @param <T>
     *          the type of items observed and emitted by this type of processor
     * @param maxAge
     *          the maximum age of the contained items
     * @param unit
     *          the time unit of {@code time}
     * @param maxSize
     *          the maximum number of buffered items
     * @param scheduler
     *          the {@link Scheduler} that provides the current time
     * @return the created processor
     * @throws NullPointerException if {@code unit} or {@code scheduler} is {@code null}
     * @throws IllegalArgumentException if {@code maxAge} or {@code maxSize} is non-positive
     */
    @CheckReturnValue
    @NonNull
    public static <T> ReplayProcessor<T> createWithTimeAndSize(long maxAge, @NonNull TimeUnit unit, @NonNull Scheduler scheduler, int maxSize) {
        ObjectHelper.verifyPositive(maxSize, "maxSize");
        ObjectHelper.verifyPositive(maxAge, "maxAge");
        Objects.requireNonNull(unit, "unit is null");
        Objects.requireNonNull(scheduler, "scheduler is null");
        return new ReplayProcessor<>(new SizeAndTimeBoundReplayBuffer<>(maxSize, maxAge, unit, scheduler));
    }

    /**
     * Constructs a ReplayProcessor with the given custom ReplayBuffer instance.
     * @param buffer the ReplayBuffer instance, not null (not verified)
     */
    @SuppressWarnings("unchecked")
    ReplayProcessor(ReplayBuffer<T> buffer) {
        this.buffer = buffer;
        this.subscribers = new AtomicReference<>(EMPTY);
    }

    @Override
    protected void subscribeActual(Subscriber<@NonNull ? super T> s) {
        ReplaySubscription<T> rs = new ReplaySubscription<>(s, this);
        s.onSubscribe(rs);

        if (add(rs)) {
            if (rs.cancelled) {
                remove(rs);
                return;
            }
        }
        buffer.replay(rs);
    }

    @Override
    public void onSubscribe(Subscription s) {
        if (done) {
            s.cancel();
            return;
        }
        s.request(Long.MAX_VALUE);
    }

    @Override
    public void onNext(T t) {
        ExceptionHelper.nullCheck(t, "onNext called with a null value.");

        if (done) {
            return;
        }

        ReplayBuffer<T> b = buffer;
        b.next(t);

        for (ReplaySubscription<T> rs : subscribers.get()) {
            b.replay(rs);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onError(Throwable t) {
        ExceptionHelper.nullCheck(t, "onError called with a null Throwable.");

        if (done) {
            RxJavaPlugins.onError(t);
            return;
        }
        done = true;

        ReplayBuffer<T> b = buffer;
        b.error(t);

        for (ReplaySubscription<T> rs : subscribers.getAndSet(TERMINATED)) {
            b.replay(rs);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onComplete() {
        if (done) {
            return;
        }
        done = true;

        ReplayBuffer<T> b = buffer;

        b.complete();

        for (ReplaySubscription<T> rs : subscribers.getAndSet(TERMINATED)) {
            b.replay(rs);
        }
    }

    @Override
    @CheckReturnValue
    public boolean hasSubscribers() {
        return subscribers.get().length != 0;
    }

    @CheckReturnValue
    /* test */ int subscriberCount() {
        return subscribers.get().length;
    }

    @Override
    @Nullable
    @CheckReturnValue
    public Throwable getThrowable() {
        ReplayBuffer<T> b = buffer;
        if (b.isDone()) {
            return b.getError();
        }
        return null;
    }

    /**
     * Makes sure the item cached by the head node in a bounded
     * ReplayProcessor is released (as it is never part of a replay).
     * <p>
     * By default, live bounded buffers will remember one item before
     * the currently receivable one to ensure subscribers can always
     * receive a continuous sequence of items. A terminated ReplayProcessor
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

    /**
     * Returns the latest value this processor has or null if no such value exists.
     * <p>The method is thread-safe.
     * @return the latest value this processor currently has or null if no such value exists
     */
    @CheckReturnValue
    public T getValue() {
        return buffer.getValue();
    }

    /**
     * Returns an Object array containing snapshot all values of this processor.
     * <p>The method is thread-safe.
     * @return the array containing the snapshot of all values of this processor
     */
    @CheckReturnValue
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
     * Returns a typed array containing a snapshot of all values of this processor.
     * <p>The method follows the conventions of Collection.toArray by setting the array element
     * after the last value to null (if the capacity permits).
     * <p>The method is thread-safe.
     * @param array the target array to copy values into if it fits
     * @return the given array if the values fit into it or a new array containing all values
     */
    @CheckReturnValue
    public T[] getValues(T[] array) {
        return buffer.getValues(array);
    }

    @Override
    @CheckReturnValue
    public boolean hasComplete() {
        ReplayBuffer<T> b = buffer;
        return b.isDone() && b.getError() == null;
    }

    @Override
    @CheckReturnValue
    public boolean hasThrowable() {
        ReplayBuffer<T> b = buffer;
        return b.isDone() && b.getError() != null;
    }

    /**
     * Returns true if this processor has any value.
     * <p>The method is thread-safe.
     * @return true if the processor has any value
     */
    @CheckReturnValue
    public boolean hasValue() {
        return buffer.size() != 0; // NOPMD
    }

    @CheckReturnValue
    /* test*/ int size() {
        return buffer.size();
    }

    boolean add(ReplaySubscription<T> rs) {
        for (;;) {
            ReplaySubscription<T>[] a = subscribers.get();
            if (a == TERMINATED) {
                return false;
            }
            int len = a.length;
            @SuppressWarnings("unchecked")
            ReplaySubscription<T>[] b = new ReplaySubscription[len + 1];
            System.arraycopy(a, 0, b, 0, len);
            b[len] = rs;
            if (subscribers.compareAndSet(a, b)) {
                return true;
            }
        }
    }

    @SuppressWarnings("unchecked")
    void remove(ReplaySubscription<T> rs) {
        for (;;) {
            ReplaySubscription<T>[] a = subscribers.get();
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
            ReplaySubscription<T>[] b;
            if (len == 1) {
                b = EMPTY;
            } else {
                b = new ReplaySubscription[len - 1];
                System.arraycopy(a, 0, b, 0, j);
                System.arraycopy(a, j + 1, b, j, len - j - 1);
            }
            if (subscribers.compareAndSet(a, b)) {
                return;
            }
        }
    }

    /**
     * Abstraction over a buffer that receives events and replays them to
     * individual Subscribers.
     *
     * @param <T> the value type
     */
    interface ReplayBuffer<T> {

        void next(T value);

        void error(Throwable ex);

        void complete();

        void replay(ReplaySubscription<T> rs);

        int size();

        @Nullable
        T getValue();

        T[] getValues(T[] array);

        boolean isDone();

        Throwable getError();

        /**
         * Make sure an old inaccessible head value is released
         * in a bounded buffer.
         */
        void trimHead();
    }

    static final class ReplaySubscription<T> extends AtomicInteger implements Subscription {

        private static final long serialVersionUID = 466549804534799122L;
        final Subscriber<@NonNull ? super T> downstream;
        final ReplayProcessor<T> state;

        Object index;

        final AtomicLong requested;

        volatile boolean cancelled;

        long emitted;

        ReplaySubscription(Subscriber<@NonNull ? super T> actual, ReplayProcessor<T> state) {
            this.downstream = actual;
            this.state = state;
            this.requested = new AtomicLong();
        }

        @Override
        public void request(long n) {
            if (SubscriptionHelper.validate(n)) {
                BackpressureHelper.add(requested, n);
                state.buffer.replay(this);
            }
        }

        @Override
        public void cancel() {
            if (!cancelled) {
                cancelled = true;
                state.remove(this);
            }
        }
    }

    static final class UnboundedReplayBuffer<T>
    implements ReplayBuffer<T> {

        final List<T> buffer;

        Throwable error;
        volatile boolean done;

        volatile int size;

        UnboundedReplayBuffer(int capacityHint) {
            this.buffer = new ArrayList<>(capacityHint);
        }

        @Override
        public void next(T value) {
            buffer.add(value);
            size++;
        }

        @Override
        public void error(Throwable ex) {
            error = ex;
            done = true;
        }

        @Override
        public void complete() {
            done = true;
        }

        @Override
        public void trimHead() {
            // not applicable for an unbounded buffer
        }

        @Override
        @Nullable
        public T getValue() {
            int s = size;
            if (s == 0) {
                return null;
            }
            return buffer.get(s - 1);
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
            List<T> b = buffer;

            if (array.length < s) {
                array = (T[])Array.newInstance(array.getClass().getComponentType(), s);
            }
            for (int i = 0; i < s; i++) {
                array[i] = b.get(i);
            }
            if (array.length > s) {
                array[s] = null;
            }

            return array;
        }

        @Override
        public void replay(ReplaySubscription<T> rs) {
            if (rs.getAndIncrement() != 0) {
                return;
            }

            int missed = 1;
            final List<T> b = buffer;
            final Subscriber<@NonNull ? super T> a = rs.downstream;

            Integer indexObject = (Integer)rs.index;
            int index;
            if (indexObject != null) {
                index = indexObject;
            } else {
                index = 0;
                rs.index = 0;
            }
            long e = rs.emitted;

            for (;;) {

                long r = rs.requested.get();

                while (e != r) {
                    if (rs.cancelled) {
                        rs.index = null;
                        return;
                    }

                    boolean d = done;
                    int s = size;

                    if (d && index == s) {
                        rs.index = null;
                        rs.cancelled = true;
                        Throwable ex = error;
                        if (ex == null) {
                            a.onComplete();
                        } else {
                            a.onError(ex);
                        }
                        return;
                    }

                    if (index == s) {
                        break;
                    }

                    a.onNext(b.get(index));

                    index++;
                    e++;
                }

                if (e == r) {
                    if (rs.cancelled) {
                        rs.index = null;
                        return;
                    }

                    boolean d = done;
                    int s = size;

                    if (d && index == s) {
                        rs.index = null;
                        rs.cancelled = true;
                        Throwable ex = error;
                        if (ex == null) {
                            a.onComplete();
                        } else {
                            a.onError(ex);
                        }
                        return;
                    }
                }

                rs.index = index;
                rs.emitted = e;
                missed = rs.addAndGet(-missed);
                if (missed == 0) {
                    break;
                }
            }
        }

        @Override
        public int size() {
            return size;
        }

        @Override
        public boolean isDone() {
            return done;
        }

        @Override
        public Throwable getError() {
            return error;
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
    implements ReplayBuffer<T> {

        final int maxSize;
        int size;

        volatile Node<T> head;

        Node<T> tail;

        Throwable error;
        volatile boolean done;

        SizeBoundReplayBuffer(int maxSize) {
            this.maxSize = maxSize;
            Node<T> h = new Node<>(null);
            this.tail = h;
            this.head = h;
        }

        void trim() {
            if (size > maxSize) {
                size--;
                Node<T> h = head;
                head = h.get();
            }
        }

        @Override
        public void next(T value) {
            Node<T> n = new Node<>(value);
            Node<T> t = tail;

            tail = n;
            size++;
            t.set(n); // releases both the tail and size

            trim();
        }

        @Override
        public void error(Throwable ex) {
            error = ex;
            trimHead();
            done = true;
        }

        @Override
        public void complete() {
            trimHead();
            done = true;
        }

        @Override
        public void trimHead() {
            if (head.value != null) {
                Node<T> n = new Node<>(null);
                n.lazySet(head.get());
                head = n;
            }
        }

        @Override
        public boolean isDone() {
            return done;
        }

        @Override
        public Throwable getError() {
            return error;
        }

        @Override
        public T getValue() {
            Node<T> h = head;
            for (;;) {
                Node<T> n = h.get();
                if (n == null) {
                    return h.value;
                }
                h = n;
            }
        }

        @Override
        @SuppressWarnings("unchecked")
        public T[] getValues(T[] array) {
            int s = 0;
            Node<T> h = head;
            Node<T> h0 = h;
            for (;;) {
                Node<T> next = h0.get();
                if (next == null) {
                    break;
                }
                s++;
                h0 = next;
            }
            if (array.length < s) {
                array = (T[])Array.newInstance(array.getClass().getComponentType(), s);
            }

            for (int j = 0; j < s; j++) {
                h = h.get();
                array[j] = h.value;
            }

            if (array.length > s) {
                array[s] = null;
            }
            return array;
        }

        @Override
        @SuppressWarnings("unchecked")
        public void replay(ReplaySubscription<T> rs) {
            if (rs.getAndIncrement() != 0) {
                return;
            }

            int missed = 1;
            final Subscriber<@NonNull ? super T> a = rs.downstream;

            Node<T> index = (Node<T>)rs.index;
            if (index == null) {
                index = head;
            }

            long e = rs.emitted;

            for (;;) {

                long r = rs.requested.get();

                while (e != r) {
                    if (rs.cancelled) {
                        rs.index = null;
                        return;
                    }

                    boolean d = done;
                    Node<T> next = index.get();
                    boolean empty = next == null;

                    if (d && empty) {
                        rs.index = null;
                        rs.cancelled = true;
                        Throwable ex = error;
                        if (ex == null) {
                            a.onComplete();
                        } else {
                            a.onError(ex);
                        }
                        return;
                    }

                    if (empty) {
                        break;
                    }

                    a.onNext(next.value);
                    e++;
                    index = next;
                }

                if (e == r) {
                    if (rs.cancelled) {
                        rs.index = null;
                        return;
                    }

                    boolean d = done;

                    if (d && index.get() == null) {
                        rs.index = null;
                        rs.cancelled = true;
                        Throwable ex = error;
                        if (ex == null) {
                            a.onComplete();
                        } else {
                            a.onError(ex);
                        }
                        return;
                    }
                }

                rs.index = index;
                rs.emitted = e;

                missed = rs.addAndGet(-missed);
                if (missed == 0) {
                    break;
                }
            }
        }

        @Override
        public int size() {
            int s = 0;
            Node<T> h = head;
            while (s != Integer.MAX_VALUE) {
                Node<T> next = h.get();
                if (next == null) {
                    break;
                }
                s++;
                h = next;
            }

            return s;
        }
    }

    static final class SizeAndTimeBoundReplayBuffer<T>
    implements ReplayBuffer<T> {

        final int maxSize;
        final long maxAge;
        final TimeUnit unit;
        final Scheduler scheduler;
        int size;

        volatile TimedNode<T> head;

        TimedNode<T> tail;

        Throwable error;
        volatile boolean done;

        SizeAndTimeBoundReplayBuffer(int maxSize, long maxAge, TimeUnit unit, Scheduler scheduler) {
            this.maxSize = maxSize;
            this.maxAge = maxAge;
            this.unit = unit;
            this.scheduler = scheduler;
            TimedNode<T> h = new TimedNode<>(null, 0L);
            this.tail = h;
            this.head = h;
        }

        void trim() {
            if (size > maxSize) {
                size--;
                TimedNode<T> h = head;
                head = h.get();
            }
            long limit = scheduler.now(unit) - maxAge;

            TimedNode<T> h = head;

            for (;;) {
                if (size <= 1) {
                    head = h;
                    break;
                }
                TimedNode<T> next = h.get();

                if (next.time > limit) {
                    head = h;
                    break;
                }

                h = next;
                size--;
            }

        }

        void trimFinal() {
            long limit = scheduler.now(unit) - maxAge;

            TimedNode<T> h = head;

            for (;;) {
                TimedNode<T> next = h.get();
                if (next == null) {
                    if (h.value != null) {
                        head = new TimedNode<>(null, 0L);
                    } else {
                        head = h;
                    }
                    break;
                }

                if (next.time > limit) {
                    if (h.value != null) {
                        TimedNode<T> n = new TimedNode<>(null, 0L);
                        n.lazySet(h.get());
                        head = n;
                    } else {
                        head = h;
                    }
                    break;
                }

                h = next;
            }
        }

        @Override
        public void trimHead() {
            if (head.value != null) {
                TimedNode<T> n = new TimedNode<>(null, 0L);
                n.lazySet(head.get());
                head = n;
            }
        }

        @Override
        public void next(T value) {
            TimedNode<T> n = new TimedNode<>(value, scheduler.now(unit));
            TimedNode<T> t = tail;

            tail = n;
            size++;
            t.set(n); // releases both the tail and size

            trim();
        }

        @Override
        public void error(Throwable ex) {
            trimFinal();
            error = ex;
            done = true;
        }

        @Override
        public void complete() {
            trimFinal();
            done = true;
        }

        @Override
        @Nullable
        public T getValue() {
            TimedNode<T> h = head;

            for (;;) {
                TimedNode<T> next = h.get();
                if (next == null) {
                    break;
                }
                h = next;
            }

            long limit = scheduler.now(unit) - maxAge;
            if (h.time < limit) {
                return null;
            }

            return h.value;
        }

        @Override
        @SuppressWarnings("unchecked")
        public T[] getValues(T[] array) {
            TimedNode<T> h = getHead();
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
                    TimedNode<T> next = h.get();
                    array[i] = next.value;
                    i++;
                    h = next;
                }
                if (array.length > s) {
                    array[s] = null;
                }
            }

            return array;
        }

        TimedNode<T> getHead() {
            TimedNode<T> index = head;
            // skip old entries
            long limit = scheduler.now(unit) - maxAge;
            TimedNode<T> next = index.get();
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
        public void replay(ReplaySubscription<T> rs) {
            if (rs.getAndIncrement() != 0) {
                return;
            }

            int missed = 1;
            final Subscriber<@NonNull ? super T> a = rs.downstream;

            TimedNode<T> index = (TimedNode<T>)rs.index;
            if (index == null) {
                index = getHead();
            }

            long e = rs.emitted;

            for (;;) {

                long r = rs.requested.get();

                while (e != r) {
                    if (rs.cancelled) {
                        rs.index = null;
                        return;
                    }

                    boolean d = done;
                    TimedNode<T> next = index.get();
                    boolean empty = next == null;

                    if (d && empty) {
                        rs.index = null;
                        rs.cancelled = true;
                        Throwable ex = error;
                        if (ex == null) {
                            a.onComplete();
                        } else {
                            a.onError(ex);
                        }
                        return;
                    }

                    if (empty) {
                        break;
                    }

                    a.onNext(next.value);
                    e++;
                    index = next;
                }

                if (e == r) {
                    if (rs.cancelled) {
                        rs.index = null;
                        return;
                    }

                    boolean d = done;

                    if (d && index.get() == null) {
                        rs.index = null;
                        rs.cancelled = true;
                        Throwable ex = error;
                        if (ex == null) {
                            a.onComplete();
                        } else {
                            a.onError(ex);
                        }
                        return;
                    }
                }

                rs.index = index;
                rs.emitted = e;

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

        int size(TimedNode<T> h) {
            int s = 0;
            while (s != Integer.MAX_VALUE) {
                TimedNode<T> next = h.get();
                if (next == null) {
                    break;
                }
                s++;
                h = next;
            }

            return s;
        }

        @Override
        public Throwable getError() {
            return error;
        }

        @Override
        public boolean isDone() {
            return done;
        }
    }
}
