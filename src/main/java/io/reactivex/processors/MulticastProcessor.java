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

import java.util.concurrent.atomic.*;

import org.reactivestreams.*;

import io.reactivex.annotations.*;
import io.reactivex.exceptions.*;
import io.reactivex.internal.functions.ObjectHelper;
import io.reactivex.internal.fuseable.*;
import io.reactivex.internal.queue.*;
import io.reactivex.internal.subscriptions.*;
import io.reactivex.plugins.RxJavaPlugins;

/**
 * A {@link FlowableProcessor} implementation that coordinates downstream requests through
 * a front-buffer and stable-prefetching, optionally canceling the upstream if all
 * subscribers have cancelled.
 * <p>
 * <img width="640" height="360" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/MulticastProcessor.png" alt="">
 * <p>
 * This processor does not have a public constructor by design; a new empty instance of this
 * {@code MulticastProcessor} can be created via the following {@code create} methods that
 * allow configuring it:
 * <ul>
 * <li>{@link #create()}: create an empty {@code MulticastProcessor} with
 *      {@link io.reactivex.Flowable#bufferSize() Flowable.bufferSize()} prefetch amount
 *      and no reference counting behavior.</li>
 * <li>{@link #create(int)}: create an empty {@code MulticastProcessor} with
 *      the given prefetch amount and no reference counting behavior.</li>
 * <li>{@link #create(boolean)}: create an empty {@code MulticastProcessor} with
 *      {@link io.reactivex.Flowable#bufferSize() Flowable.bufferSize()} prefetch amount
 *      and an optional reference counting behavior.</li>
 * <li>{@link #create(int, boolean)}: create an empty {@code MulticastProcessor} with
 *      the given prefetch amount and an optional reference counting behavior.</li>
 * </ul>
 * <p>
 * When the reference counting behavior is enabled, the {@code MulticastProcessor} cancels its
 * upstream when all {@link Subscriber}s have cancelled. Late {@code Subscriber}s will then be
 * immediately completed.
 * <p>
 * Because {@code MulticastProcessor} implements the {@link Subscriber} interface, calling
 * {@code onSubscribe} is mandatory (<a href="https://github.com/reactive-streams/reactive-streams-jvm#2.12">Rule 2.12</a>).
 * If {@code MulticastProcessor} should run standalone, i.e., without subscribing the {@code MulticastProcessor} to another {@link Publisher},
 * use {@link #start()} or {@link #startUnbounded()} methods to initialize the internal buffer.
 * Failing to do so will lead to a {@link NullPointerException} at runtime.
 * <p>
 * Use {@link #offer(Object)} to try and offer/emit items but don't fail if the
 * internal buffer is full.
 * <p>
 * A {@code MulticastProcessor} is a {@link Processor} type in the Reactive Streams specification,
 * {@code null}s are not allowed (<a href="https://github.com/reactive-streams/reactive-streams-jvm#2.13">Rule 2.13</a>) as
 * parameters to {@link #onSubscribe(Subscription)}, {@link #offer(Object)}, {@link #onNext(Object)} and {@link #onError(Throwable)}.
 * Such calls will result in a {@link NullPointerException} being thrown and the processor's state is not changed.
 * <p>
 * Since a {@code MulticastProcessor} is a {@link io.reactivex.Flowable}, it supports backpressure.
 * The backpressure from the currently subscribed {@link Subscriber}s are coordinated by emitting upstream
 * items only if all of those {@code Subscriber}s have requested at least one item. This behavior
 * is also called <em>lockstep-mode</em> because even if some {@code Subscriber}s can take any number
 * of items, other {@code Subscriber}s requesting less or infrequently will slow down the overall
 * throughput of the flow.
 * <p>
 * Calling {@link #onNext(Object)}, {@link #offer(Object)}, {@link #onError(Throwable)} and {@link #onComplete()}
 * is required to be serialized (called from the same thread or called non-overlappingly from different threads
 * through external means of serialization). The {@link #toSerialized()} method available to all {@link FlowableProcessor}s
 * provides such serialization and also protects against reentrance (i.e., when a downstream {@code Subscriber}
 * consuming this processor also wants to call {@link #onNext(Object)} on this processor recursively).
 * <p>
 * This {@code MulticastProcessor} supports the standard state-peeking methods {@link #hasComplete()}, {@link #hasThrowable()},
 * {@link #getThrowable()} and {@link #hasSubscribers()}. This processor doesn't allow peeking into its buffer.
 * <p>
 * When this {@code MulticastProcessor} is terminated via {@link #onError(Throwable)} or {@link #onComplete()},
 * all previously signaled but not yet consumed items will be still available to {@code Subscriber}s and the respective
 * terminal even is only emitted when all previous items have been successfully delivered to {@code Subscriber}s.
 * If there are no {@code Subscriber}s, the remaining items will be buffered indefinitely.
 * <p>
 * The {@code MulticastProcessor} does not support clearing its cached events (to appear empty again).
 * <dl>
 *  <dt><b>Backpressure:</b></dt>
 *  <dd>The backpressure from the currently subscribed {@code Subscriber}s are coordinated by emitting upstream
 *  items only if all of those {@code Subscriber}s have requested at least one item. This behavior
 *  is also called <em>lockstep-mode</em> because even if some {@code Subscriber}s can take any number
 *  of items, other {@code Subscriber}s requesting less or infrequently will slow down the overall
 *  throughput of the flow.</dd>
 *  <dt><b>Scheduler:</b></dt>
 *  <dd>{@code MulticastProcessor} does not operate by default on a particular {@link io.reactivex.Scheduler} and
 *  the {@code Subscriber}s get notified on an arbitrary thread in a serialized fashion.</dd>
 * </dl>
 * <p>
 * Example:
 * <pre><code>
    MulticastProcessor&lt;Integer&gt; mp = Flowable.range(1, 10)
    .subscribeWith(MulticastProcessor.create());

    mp.test().assertResult(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);

    // --------------------

    MulticastProcessor&lt;Integer&gt; mp2 = MulticastProcessor.create(4);
    mp2.start();

    assertTrue(mp2.offer(1));
    assertTrue(mp2.offer(2));
    assertTrue(mp2.offer(3));
    assertTrue(mp2.offer(4));

    assertFalse(mp2.offer(5));

    mp2.onComplete();

    mp2.test().assertResult(1, 2, 3, 4);
 * </code></pre>
 * <p>History: 2.1.14 - experimental
 * @param <T> the input and output value type
 * @since 2.2
 */
@BackpressureSupport(BackpressureKind.FULL)
@SchedulerSupport(SchedulerSupport.NONE)
public final class MulticastProcessor<T> extends FlowableProcessor<T> {

    final AtomicInteger wip;

    final AtomicReference<Subscription> upstream;

    final AtomicReference<MulticastSubscription<T>[]> subscribers;

    final AtomicBoolean once;

    final int bufferSize;

    final int limit;

    final boolean refcount;

    volatile SimpleQueue<T> queue;

    volatile boolean done;
    volatile Throwable error;

    int consumed;

    int fusionMode;

    @SuppressWarnings("rawtypes")
    static final MulticastSubscription[] EMPTY = new MulticastSubscription[0];

    @SuppressWarnings("rawtypes")
    static final MulticastSubscription[] TERMINATED = new MulticastSubscription[0];

    /**
     * Constructs a fresh instance with the default Flowable.bufferSize() prefetch
     * amount and no refCount-behavior.
     * @param <T> the input and output value type
     * @return the new MulticastProcessor instance
     */
    @CheckReturnValue
    @NonNull
    public static <T> MulticastProcessor<T> create() {
        return new MulticastProcessor<T>(bufferSize(), false);
    }

    /**
     * Constructs a fresh instance with the default Flowable.bufferSize() prefetch
     * amount and the optional refCount-behavior.
     * @param <T> the input and output value type
     * @param refCount if true and if all Subscribers have canceled, the upstream
     * is cancelled
     * @return the new MulticastProcessor instance
     */
    @CheckReturnValue
    @NonNull
    public static <T> MulticastProcessor<T> create(boolean refCount) {
        return new MulticastProcessor<T>(bufferSize(), refCount);
    }

    /**
     * Constructs a fresh instance with the given prefetch amount and no refCount behavior.
     * @param bufferSize the prefetch amount
     * @param <T> the input and output value type
     * @return the new MulticastProcessor instance
     */
    @CheckReturnValue
    @NonNull
    public static <T> MulticastProcessor<T> create(int bufferSize) {
        return new MulticastProcessor<T>(bufferSize, false);
    }

    /**
     * Constructs a fresh instance with the given prefetch amount and the optional
     * refCount-behavior.
     * @param bufferSize the prefetch amount
     * @param refCount if true and if all Subscribers have canceled, the upstream
     * is cancelled
     * @param <T> the input and output value type
     * @return the new MulticastProcessor instance
     */
    @CheckReturnValue
    @NonNull
    public static <T> MulticastProcessor<T> create(int bufferSize, boolean refCount) {
        return new MulticastProcessor<T>(bufferSize, refCount);
    }

    /**
     * Constructs a fresh instance with the given prefetch amount and the optional
     * refCount-behavior.
     * @param bufferSize the prefetch amount
     * @param refCount if true and if all Subscribers have canceled, the upstream
     * is cancelled
     */
    @SuppressWarnings("unchecked")
    MulticastProcessor(int bufferSize, boolean refCount) {
        ObjectHelper.verifyPositive(bufferSize, "bufferSize");
        this.bufferSize = bufferSize;
        this.limit = bufferSize - (bufferSize >> 2);
        this.wip = new AtomicInteger();
        this.subscribers = new AtomicReference<MulticastSubscription<T>[]>(EMPTY);
        this.upstream = new AtomicReference<Subscription>();
        this.refcount = refCount;
        this.once = new AtomicBoolean();
    }

    /**
     * Initializes this Processor by setting an upstream Subscription that
     * ignores request amounts, uses a fixed buffer
     * and allows using the onXXX and offer methods
     * afterwards.
     */
    public void start() {
        if (SubscriptionHelper.setOnce(upstream, EmptySubscription.INSTANCE)) {
            queue = new SpscArrayQueue<T>(bufferSize);
        }
    }

    /**
     * Initializes this Processor by setting an upstream Subscription that
     * ignores request amounts, uses an unbounded buffer
     * and allows using the onXXX and offer methods
     * afterwards.
     */
    public void startUnbounded() {
        if (SubscriptionHelper.setOnce(upstream, EmptySubscription.INSTANCE)) {
            queue = new SpscLinkedArrayQueue<T>(bufferSize);
        }
    }

    @Override
    public void onSubscribe(Subscription s) {
        if (SubscriptionHelper.setOnce(upstream, s)) {
            if (s instanceof QueueSubscription) {
                @SuppressWarnings("unchecked")
                QueueSubscription<T> qs = (QueueSubscription<T>)s;

                int m = qs.requestFusion(QueueSubscription.ANY);
                if (m == QueueSubscription.SYNC) {
                    fusionMode = m;
                    queue = qs;
                    done = true;
                    drain();
                    return;
                }
                if (m == QueueSubscription.ASYNC) {
                    fusionMode = m;
                    queue = qs;

                    s.request(bufferSize);
                    return;
                }
            }

            queue = new SpscArrayQueue<T>(bufferSize);

            s.request(bufferSize);
        }
    }

    @Override
    public void onNext(T t) {
        if (once.get()) {
            return;
        }
        if (fusionMode == QueueSubscription.NONE) {
            ObjectHelper.requireNonNull(t, "onNext called with null. Null values are generally not allowed in 2.x operators and sources.");
            if (!queue.offer(t)) {
                SubscriptionHelper.cancel(upstream);
                onError(new MissingBackpressureException());
                return;
            }
        }
        drain();
    }

    /**
     * Tries to offer an item into the internal queue and returns false
     * if the queue is full.
     * @param t the item to offer, not null
     * @return true if successful, false if the queue is full
     */
    public boolean offer(T t) {
        if (once.get()) {
            return false;
        }
        ObjectHelper.requireNonNull(t, "offer called with null. Null values are generally not allowed in 2.x operators and sources.");
        if (fusionMode == QueueSubscription.NONE) {
            if (queue.offer(t)) {
                drain();
                return true;
            }
        }
        return false;
    }

    @Override
    public void onError(Throwable t) {
        ObjectHelper.requireNonNull(t, "onError called with null. Null values are generally not allowed in 2.x operators and sources.");
        if (once.compareAndSet(false, true)) {
            error = t;
            done = true;
            drain();
        } else {
            RxJavaPlugins.onError(t);
        }
    }

    @Override
    public void onComplete() {
        if (once.compareAndSet(false, true)) {
            done = true;
            drain();
        }
    }

    @Override
    public boolean hasSubscribers() {
        return subscribers.get().length != 0;
    }

    @Override
    public boolean hasThrowable() {
        return once.get() && error != null;
    }

    @Override
    public boolean hasComplete() {
        return once.get() && error == null;
    }

    @Override
    public Throwable getThrowable() {
        return once.get() ? error : null;
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> s) {
        MulticastSubscription<T> ms = new MulticastSubscription<T>(s, this);
        s.onSubscribe(ms);
        if (add(ms)) {
            if (ms.get() == Long.MIN_VALUE) {
                remove(ms);
            } else {
                drain();
            }
        } else {
            if (once.get() || !refcount) {
                Throwable ex = error;
                if (ex != null) {
                    s.onError(ex);
                    return;
                }
            }
            s.onComplete();
        }
    }

    boolean add(MulticastSubscription<T> inner) {
        for (;;) {
            MulticastSubscription<T>[] a = subscribers.get();
            if (a == TERMINATED) {
                return false;
            }
            int n = a.length;
            @SuppressWarnings("unchecked")
            MulticastSubscription<T>[] b = new MulticastSubscription[n + 1];
            System.arraycopy(a, 0, b, 0, n);
            b[n] = inner;
            if (subscribers.compareAndSet(a, b)) {
                return true;
            }
        }
    }

    @SuppressWarnings("unchecked")
    void remove(MulticastSubscription<T> inner) {
        for (;;) {
            MulticastSubscription<T>[] a = subscribers.get();
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
                break;
            }

            if (n == 1) {
                if (refcount) {
                    if (subscribers.compareAndSet(a, TERMINATED)) {
                        SubscriptionHelper.cancel(upstream);
                        once.set(true);
                        break;
                    }
                } else {
                    if (subscribers.compareAndSet(a, EMPTY)) {
                        break;
                    }
                }
            } else {
                MulticastSubscription<T>[] b = new MulticastSubscription[n - 1];
                System.arraycopy(a, 0, b, 0, j);
                System.arraycopy(a, j + 1, b, j, n - j - 1);
                if (subscribers.compareAndSet(a, b)) {
                    break;
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    void drain() {
        if (wip.getAndIncrement() != 0) {
            return;
        }

        int missed = 1;
        AtomicReference<MulticastSubscription<T>[]> subs = subscribers;
        int c = consumed;
        int lim = limit;
        int fm = fusionMode;

        outer:
        for (;;) {

            SimpleQueue<T> q = queue;

            if (q != null) {
                MulticastSubscription<T>[] as = subs.get();
                int n = as.length;

                if (n != 0) {
                    long r = -1L;

                    for (MulticastSubscription<T> a : as) {
                        long ra = a.get();
                        if (ra >= 0L) {
                            if (r == -1L) {
                                r = ra - a.emitted;
                            } else {
                                r = Math.min(r, ra - a.emitted);
                            }
                        }
                    }

                    while (r > 0L) {
                        MulticastSubscription<T>[] bs = subs.get();

                        if (bs == TERMINATED) {
                            q.clear();
                            return;
                        }

                        if (as != bs) {
                            continue outer;
                        }

                        boolean d = done;

                        T v;

                        try {
                            v = q.poll();
                        } catch (Throwable ex) {
                            Exceptions.throwIfFatal(ex);
                            SubscriptionHelper.cancel(upstream);
                            d = true;
                            v = null;
                            error = ex;
                            done = true;
                        }
                        boolean empty = v == null;

                        if (d && empty) {
                            Throwable ex = error;
                            if (ex != null) {
                                for (MulticastSubscription<T> inner : subs.getAndSet(TERMINATED)) {
                                    inner.onError(ex);
                                }
                            } else {
                                for (MulticastSubscription<T> inner : subs.getAndSet(TERMINATED)) {
                                    inner.onComplete();
                                }
                            }
                            return;
                        }

                        if (empty) {
                            break;
                        }

                        for (MulticastSubscription<T> inner : as) {
                            inner.onNext(v);
                        }

                        r--;

                        if (fm != QueueSubscription.SYNC) {
                            if (++c == lim) {
                                c = 0;
                                upstream.get().request(lim);
                            }
                        }
                    }

                    if (r == 0) {
                        MulticastSubscription<T>[] bs = subs.get();

                        if (bs == TERMINATED) {
                            q.clear();
                            return;
                        }

                        if (as != bs) {
                            continue outer;
                        }

                        if (done && q.isEmpty()) {
                            Throwable ex = error;
                            if (ex != null) {
                                for (MulticastSubscription<T> inner : subs.getAndSet(TERMINATED)) {
                                    inner.onError(ex);
                                }
                            } else {
                                for (MulticastSubscription<T> inner : subs.getAndSet(TERMINATED)) {
                                    inner.onComplete();
                                }
                            }
                            return;
                        }
                    }
                }
            }

            missed = wip.addAndGet(-missed);
            if (missed == 0) {
                break;
            }
        }
    }

    static final class MulticastSubscription<T> extends AtomicLong implements Subscription {

        private static final long serialVersionUID = -363282618957264509L;

        final Subscriber<? super T> actual;

        final MulticastProcessor<T> parent;

        long emitted;

        MulticastSubscription(Subscriber<? super T> actual, MulticastProcessor<T> parent) {
            this.actual = actual;
            this.parent = parent;
        }

        @Override
        public void request(long n) {
            if (SubscriptionHelper.validate(n)) {
                for (;;) {
                    long r = get();
                    if (r == Long.MIN_VALUE || r == Long.MAX_VALUE) {
                        break;
                    }
                    long u = r + n;
                    if (u < 0L) {
                        u = Long.MAX_VALUE;
                    }
                    if (compareAndSet(r, u)) {
                        parent.drain();
                        break;
                    }
                }
            }
        }

        @Override
        public void cancel() {
            if (getAndSet(Long.MIN_VALUE) != Long.MIN_VALUE) {
                parent.remove(this);
            }
        }

        void onNext(T t) {
            if (get() != Long.MIN_VALUE) {
                emitted++;
                actual.onNext(t);
            }
        }

        void onError(Throwable t) {
            if (get() != Long.MIN_VALUE) {
                actual.onError(t);
            }
        }

        void onComplete() {
            if (get() != Long.MIN_VALUE) {
                actual.onComplete();
            }
        }
    }
}
