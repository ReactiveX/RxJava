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

package io.reactivex.processors;

import java.lang.reflect.Array;
import java.util.concurrent.atomic.*;
import java.util.concurrent.locks.*;

import org.reactivestreams.*;

import io.reactivex.annotations.*;
import io.reactivex.exceptions.MissingBackpressureException;
import io.reactivex.internal.functions.ObjectHelper;
import io.reactivex.internal.subscriptions.SubscriptionHelper;
import io.reactivex.internal.util.*;
import io.reactivex.internal.util.AppendOnlyLinkedArrayList.NonThrowingPredicate;
import io.reactivex.plugins.RxJavaPlugins;

/**
 * Processor that emits the most recent item it has observed and all subsequent observed items to each subscribed
 * {@link Subscriber}.
 * <p>
 * <img width="640" height="460" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/S.BehaviorProcessor.png" alt="">
 * <p>
 * This processor does not have a public constructor by design; a new empty instance of this
 * {@code BehaviorProcessor} can be created via the {@link #create()} method and
 * a new non-empty instance can be created via {@link #createDefault(Object)} (named as such to avoid
 * overload resolution conflict with {@code Flowable.create} that creates a Flowable, not a {@code BehaviorProcessor}).
 * <p>
 * In accordance with the Reactive Streams specification (<a href="https://github.com/reactive-streams/reactive-streams-jvm#2.13">Rule 2.13</a>)
 * {@code null}s are not allowed as default initial values in {@link #createDefault(Object)} or as parameters to {@link #onNext(Object)} and
 * {@link #onError(Throwable)}.
 * <p>
 * When this {@code BehaviorProcessor} is terminated via {@link #onError(Throwable)} or {@link #onComplete()}, the
 * last observed item (if any) is cleared and late {@link org.reactivestreams.Subscriber}s only receive
 * the respective terminal event.
 * <p>
 * The {@code BehaviorProcessor} does not support clearing its cached value (to appear empty again), however, the
 * effect can be achieved by using a special item and making sure {@code Subscriber}s subscribe through a
 * filter whose predicate filters out this special item:
 * <pre><code>
 * BehaviorProcessor&lt;Integer&gt; processor = BehaviorProcessor.create();
 *
 * final Integer EMPTY = Integer.MIN_VALUE;
 *
 * Flowable&lt;Integer&gt; flowable = processor.filter(v -&gt; v != EMPTY);
 *
 * TestSubscriber&lt;Integer&gt; ts1 = flowable.test();
 *
 * processor.onNext(1);
 * // this will "clear" the cache
 * processor.onNext(EMPTY);
 * 
 * TestSubscriber&lt;Integer&gt; ts2 = flowable.test();
 * 
 * processor.onNext(2);
 * processor.onComplete();
 * 
 * // ts1 received both non-empty items
 * ts1.assertResult(1, 2);
 * 
 * // ts2 received only 2 even though the current item was EMPTY
 * // when it got subscribed
 * ts2.assertResult(2);
 * 
 * // Subscribers coming after the processor was terminated receive
 * // no items and only the onComplete event in this case.
 * flowable.test().assertResult();
 * </code></pre>
 * <p>
 * Even though {@code BehaviorProcessor} implements the {@code Subscriber} interface, calling
 * {@code onSubscribe} is not required (<a href="https://github.com/reactive-streams/reactive-streams-jvm#2.12">Rule 2.12</a>)
 * if the processor is used as a standalone source. However, calling {@code onSubscribe}
 * after the {@code BehaviorProcessor} reached its terminal state will result in the
 * given {@code Subscription} being cancelled immediately.
 * <p>
 * Calling {@link #onNext(Object)}, {@link #offer(Object)}, {@link #onError(Throwable)} and {@link #onComplete()}
 * is required to be serialized (called from the same thread or called non-overlappingly from different threads
 * through external means of serialization). The {@link #toSerialized()} method available to all {@code FlowableProcessor}s
 * provides such serialization and also protects against reentrance (i.e., when a downstream {@code Subscriber}
 * consuming this processor also wants to call {@link #onNext(Object)} on this processor recursively).
 * Note that serializing over {@link #offer(Object)} is not supported through {@code toSerialized()} because it is a method
 * available on the {@code PublishProcessor} and {@code BehaviorProcessor} classes only.
 * <p>
 * This {@code BehaviorProcessor} supports the standard state-peeking methods {@link #hasComplete()}, {@link #hasThrowable()},
 * {@link #getThrowable()} and {@link #hasSubscribers()} as well as means to read the latest observed value
 * in a non-blocking and thread-safe manner via {@link #hasValue()}, {@link #getValue()},
 * {@link #getValues()} or {@link #getValues(Object[])}.
 * <p>
 * Note that this processor signals {@code MissingBackpressureException} if a particular {@code Subscriber} is not
 * ready to receive {@code onNext} events. To avoid this exception being signaled, use {@link #offer(Object)} to only
 * try to emit an item when all {@code Subscriber}s have requested item(s).
 * <dl>
 *  <dt><b>Backpressure:</b></dt>
 *  <dd>The {@code BehaviorProcessor} does not coordinate requests of its downstream {@code Subscriber}s and
 *  expects each individual {@code Subscriber} is ready to receive {@code onNext} items when {@link #onNext(Object)}
 *  is called. If a {@code Subscriber} is not ready, a {@code MissingBackpressureException} is signalled to it.
 *  To avoid overflowing the current {@code Subscriber}s, the conditional {@link #offer(Object)} method is available
 *  that returns true if any of the {@code Subscriber}s is not ready to receive {@code onNext} events. If
 *  there are no {@code Subscriber}s to the processor, {@code offer()} always succeeds.
 *  If the {@code BehaviorProcessor} is (optionally) subscribed to another {@code Publisher}, this upstream
 *  {@code Publisher} is consumed in an unbounded fashion (requesting {@code Long.MAX_VALUE}).</dd>
 *  <dt><b>Scheduler:</b></dt>
 *  <dd>{@code BehaviorProcessor} does not operate by default on a particular {@link io.reactivex.Scheduler} and
 *  the {@code Subscriber}s get notified on the thread the respective {@code onXXX} methods were invoked.</dd>
 *  <dt><b>Error handling:</b></dt>
 *  <dd>When the {@link #onError(Throwable)} is called, the {@code BehaviorProcessor} enters into a terminal state
 *  and emits the same {@code Throwable} instance to the last set of {@code Subscriber}s. During this emission,
 *  if one or more {@code Subscriber}s cancel their respective {@code Subscription}s, the
 *  {@code Throwable} is delivered to the global error handler via
 *  {@link io.reactivex.plugins.RxJavaPlugins#onError(Throwable)} (multiple times if multiple {@code Subscriber}s
 *  cancel at once).
 *  If there were no {@code Subscriber}s subscribed to this {@code BehaviorProcessor} when the {@code onError()}
 *  was called, the global error handler is not invoked.
 *  </dd>
 * </dl>
 * <p>
 * Example usage:
 * <pre> {@code

  // subscriber will receive all events.
  BehaviorProcessor<Object> processor = BehaviorProcessor.create("default");
  processor.subscribe(subscriber);
  processor.onNext("one");
  processor.onNext("two");
  processor.onNext("three");

  // subscriber will receive the "one", "two" and "three" events, but not "zero"
  BehaviorProcessor<Object> processor = BehaviorProcessor.create("default");
  processor.onNext("zero");
  processor.onNext("one");
  processor.subscribe(subscriber);
  processor.onNext("two");
  processor.onNext("three");

  // subscriber will receive only onComplete
  BehaviorProcessor<Object> processor = BehaviorProcessor.create("default");
  processor.onNext("zero");
  processor.onNext("one");
  processor.onComplete();
  processor.subscribe(subscriber);

  // subscriber will receive only onError
  BehaviorProcessor<Object> processor = BehaviorProcessor.create("default");
  processor.onNext("zero");
  processor.onNext("one");
  processor.onError(new RuntimeException("error"));
  processor.subscribe(subscriber);
  } </pre>
 *
 * @param <T>
 *          the type of item expected to be observed and emitted by the Processor
 */
public final class BehaviorProcessor<T> extends FlowableProcessor<T> {
    final AtomicReference<BehaviorSubscription<T>[]> subscribers;

    static final Object[] EMPTY_ARRAY = new Object[0];

    @SuppressWarnings("rawtypes")
    static final BehaviorSubscription[] EMPTY = new BehaviorSubscription[0];

    @SuppressWarnings("rawtypes")
    static final BehaviorSubscription[] TERMINATED = new BehaviorSubscription[0];

    final ReadWriteLock lock;
    final Lock readLock;
    final Lock writeLock;

    final AtomicReference<Object> value;

    final AtomicReference<Throwable> terminalEvent;

    long index;

    /**
     * Creates a {@link BehaviorProcessor} without a default item.
     *
     * @param <T>
     *            the type of item the BehaviorProcessor will emit
     * @return the constructed {@link BehaviorProcessor}
     */
    @CheckReturnValue
    @NonNull
    public static <T> BehaviorProcessor<T> create() {
        return new BehaviorProcessor<T>();
    }

    /**
     * Creates a {@link BehaviorProcessor} that emits the last item it observed and all subsequent items to each
     * {@link Subscriber} that subscribes to it.
     *
     * @param <T>
     *            the type of item the BehaviorProcessor will emit
     * @param defaultValue
     *            the item that will be emitted first to any {@link Subscriber} as long as the
     *            {@link BehaviorProcessor} has not yet observed any items from its source {@code Observable}
     * @return the constructed {@link BehaviorProcessor}
     */
    @CheckReturnValue
    @NonNull
    public static <T> BehaviorProcessor<T> createDefault(T defaultValue) {
        ObjectHelper.requireNonNull(defaultValue, "defaultValue is null");
        return new BehaviorProcessor<T>(defaultValue);
    }

    /**
     * Constructs an empty BehaviorProcessor.
     * @since 2.0
     */
    @SuppressWarnings("unchecked")
    BehaviorProcessor() {
        this.value = new AtomicReference<Object>();
        this.lock = new ReentrantReadWriteLock();
        this.readLock = lock.readLock();
        this.writeLock = lock.writeLock();
        this.subscribers = new AtomicReference<BehaviorSubscription<T>[]>(EMPTY);
        this.terminalEvent = new AtomicReference<Throwable>();
    }

    /**
     * Constructs a BehaviorProcessor with the given initial value.
     * @param defaultValue the initial value, not null (verified)
     * @throws NullPointerException if {@code defaultValue} is null
     * @since 2.0
     */
    BehaviorProcessor(T defaultValue) {
        this();
        this.value.lazySet(ObjectHelper.requireNonNull(defaultValue, "defaultValue is null"));
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> s) {
        BehaviorSubscription<T> bs = new BehaviorSubscription<T>(s, this);
        s.onSubscribe(bs);
        if (add(bs)) {
            if (bs.cancelled) {
                remove(bs);
            } else {
                bs.emitFirst();
            }
        } else {
            Throwable ex = terminalEvent.get();
            if (ex == ExceptionHelper.TERMINATED) {
                s.onComplete();
            } else {
                s.onError(ex);
            }
        }
    }

    @Override
    public void onSubscribe(Subscription s) {
        if (terminalEvent.get() != null) {
            s.cancel();
            return;
        }
        s.request(Long.MAX_VALUE);
    }

    @Override
    public void onNext(T t) {
        ObjectHelper.requireNonNull(t, "onNext called with null. Null values are generally not allowed in 2.x operators and sources.");

        if (terminalEvent.get() != null) {
            return;
        }
        Object o = NotificationLite.next(t);
        setCurrent(o);
        for (BehaviorSubscription<T> bs : subscribers.get()) {
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
        for (BehaviorSubscription<T> bs : terminate(o)) {
            bs.emitNext(o, index);
        }
    }

    @Override
    public void onComplete() {
        if (!terminalEvent.compareAndSet(null, ExceptionHelper.TERMINATED)) {
            return;
        }
        Object o = NotificationLite.complete();
        for (BehaviorSubscription<T> bs : terminate(o)) {
            bs.emitNext(o, index);  // relaxed read okay since this is the only mutator thread
        }
    }

    /**
     * Tries to emit the item to all currently subscribed Subscribers if all of them
     * has requested some value, returns false otherwise.
     * <p>
     * This method should be called in a sequential manner just like the onXXX methods
     * of the PublishProcessor.
     * <p>
     * Calling with null will terminate the PublishProcessor and a NullPointerException
     * is signalled to the Subscribers.
     * <p>History: 2.0.8 - experimental
     * @param t the item to emit, not null
     * @return true if the item was emitted to all Subscribers
     * @since 2.2
     */
    public boolean offer(T t) {
        if (t == null) {
            onError(new NullPointerException("onNext called with null. Null values are generally not allowed in 2.x operators and sources."));
            return true;
        }
        BehaviorSubscription<T>[] array = subscribers.get();

        for (BehaviorSubscription<T> s : array) {
            if (s.isFull()) {
                return false;
            }
        }

        Object o = NotificationLite.next(t);
        setCurrent(o);
        for (BehaviorSubscription<T> bs : array) {
            bs.emitNext(o, index);
        }
        return true;
    }

    @Override
    public boolean hasSubscribers() {
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
     * Returns a single value the BehaviorProcessor currently has or null if no such value exists.
     * <p>The method is thread-safe.
     * @return a single value the BehaviorProcessor currently has or null if no such value exists
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
     * Returns an Object array containing snapshot all values of the BehaviorProcessor.
     * <p>The method is thread-safe.
     * @return the array containing the snapshot of all values of the BehaviorProcessor
     * @deprecated in 2.1.14; put the result of {@link #getValue()} into an array manually, will be removed in 3.x
     */
    @Deprecated
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
     * Returns a typed array containing a snapshot of all values of the BehaviorProcessor.
     * <p>The method follows the conventions of Collection.toArray by setting the array element
     * after the last value to null (if the capacity permits).
     * <p>The method is thread-safe.
     * @param array the target array to copy values into if it fits
     * @return the given array if the values fit into it or a new array containing all values
     * @deprecated in 2.1.14; put the result of {@link #getValue()} into an array manually, will be removed in 3.x
     */
    @Deprecated
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
     * Returns true if the BehaviorProcessor has any value.
     * <p>The method is thread-safe.
     * @return true if the BehaviorProcessor has any value
     */
    public boolean hasValue() {
        Object o = value.get();
        return o != null && !NotificationLite.isComplete(o) && !NotificationLite.isError(o);
    }


    boolean add(BehaviorSubscription<T> rs) {
        for (;;) {
            BehaviorSubscription<T>[] a = subscribers.get();
            if (a == TERMINATED) {
                return false;
            }
            int len = a.length;
            @SuppressWarnings("unchecked")
            BehaviorSubscription<T>[] b = new BehaviorSubscription[len + 1];
            System.arraycopy(a, 0, b, 0, len);
            b[len] = rs;
            if (subscribers.compareAndSet(a, b)) {
                return true;
            }
        }
    }

    @SuppressWarnings("unchecked")
    void remove(BehaviorSubscription<T> rs) {
        for (;;) {
            BehaviorSubscription<T>[] a = subscribers.get();
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
            BehaviorSubscription<T>[] b;
            if (len == 1) {
                b = EMPTY;
            } else {
                b = new BehaviorSubscription[len - 1];
                System.arraycopy(a, 0, b, 0, j);
                System.arraycopy(a, j + 1, b, j, len - j - 1);
            }
            if (subscribers.compareAndSet(a, b)) {
                return;
            }
        }
    }

    @SuppressWarnings("unchecked")
    BehaviorSubscription<T>[] terminate(Object terminalValue) {

        BehaviorSubscription<T>[] a = subscribers.get();
        if (a != TERMINATED) {
            a = subscribers.getAndSet(TERMINATED);
            if (a != TERMINATED) {
                // either this or atomics with lots of allocation
                setCurrent(terminalValue);
            }
        }

        return a;
    }

    void setCurrent(Object o) {
        Lock wl = writeLock;
        wl.lock();
        index++;
        value.lazySet(o);
        wl.unlock();
    }

    static final class BehaviorSubscription<T> extends AtomicLong implements Subscription, NonThrowingPredicate<Object> {

        private static final long serialVersionUID = 3293175281126227086L;

        final Subscriber<? super T> actual;
        final BehaviorProcessor<T> state;

        boolean next;
        boolean emitting;
        AppendOnlyLinkedArrayList<Object> queue;

        boolean fastPath;

        volatile boolean cancelled;

        long index;

        BehaviorSubscription(Subscriber<? super T> actual, BehaviorProcessor<T> state) {
            this.actual = actual;
            this.state = state;
        }

        @Override
        public void request(long n) {
            if (SubscriptionHelper.validate(n)) {
                BackpressureHelper.add(this, n);
            }
        }

        @Override
        public void cancel() {
            if (!cancelled) {
                cancelled = true;

                state.remove(this);
            }
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

                BehaviorProcessor<T> s = state;

                Lock readLock = s.readLock;
                readLock.lock();
                index = s.index;
                o = s.value.get();
                readLock.unlock();

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
            if (cancelled) {
                return true;
            }

            if (NotificationLite.isComplete(o)) {
                actual.onComplete();
                return true;
            } else
            if (NotificationLite.isError(o)) {
                actual.onError(NotificationLite.getError(o));
                return true;
            }

            long r = get();
            if (r != 0L) {
                actual.onNext(NotificationLite.<T>getValue(o));
                if (r != Long.MAX_VALUE) {
                    decrementAndGet();
                }
                return false;
            }
            cancel();
            actual.onError(new MissingBackpressureException("Could not deliver value due to lack of requests"));
            return true;
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

        public boolean isFull() {
            return get() == 0L;
        }
    }
}
