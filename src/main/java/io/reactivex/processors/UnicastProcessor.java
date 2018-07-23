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

import io.reactivex.annotations.CheckReturnValue;
import java.util.concurrent.atomic.*;

import io.reactivex.annotations.Nullable;
import io.reactivex.annotations.NonNull;
import org.reactivestreams.*;

import io.reactivex.internal.functions.ObjectHelper;
import io.reactivex.internal.fuseable.QueueSubscription;
import io.reactivex.internal.queue.SpscLinkedArrayQueue;
import io.reactivex.internal.subscriptions.*;
import io.reactivex.internal.util.BackpressureHelper;
import io.reactivex.plugins.RxJavaPlugins;

/**
 * A {@link FlowableProcessor} variant that queues up events until a single {@link Subscriber} subscribes to it, replays
 * those events to it until the {@code Subscriber} catches up and then switches to relaying events live to
 * this single {@code Subscriber} until this {@code UnicastProcessor} terminates or the {@code Subscriber} cancels
 * its subscription.
 * <p>
 * <img width="640" height="370" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/UnicastProcessor.png" alt="">
 * <p>
 * This processor does not have a public constructor by design; a new empty instance of this
 * {@code UnicastProcessor} can be created via the following {@code create} methods that
 * allow specifying the retention policy for items:
 * <ul>
 * <li>{@link #create()} - creates an empty, unbounded {@code UnicastProcessor} that
 *     caches all items and the terminal event it receives.</li>
 * <li>{@link #create(int)} - creates an empty, unbounded {@code UnicastProcessor}
 *     with a hint about how many <b>total</b> items one expects to retain.</li>
 * <li>{@link #create(boolean)} - creates an empty, unbounded {@code UnicastProcessor} that
 *     optionally delays an error it receives and replays it after the regular items have been emitted.</li>
 * <li>{@link #create(int, Runnable)} - creates an empty, unbounded {@code UnicastProcessor}
 *     with a hint about how many <b>total</b> items one expects to retain and a callback that will be
 *     called exactly once when the {@code UnicastProcessor} gets terminated or the single {@code Subscriber} cancels.</li>
 * <li>{@link #create(int, Runnable, boolean)} - creates an empty, unbounded {@code UnicastProcessor}
 *     with a hint about how many <b>total</b> items one expects to retain and a callback that will be
 *     called exactly once when the {@code UnicastProcessor} gets terminated or the single {@code Subscriber} cancels
 *     and optionally delays an error it receives and replays it after the regular items have been emitted.</li>
 * </ul>
 * <p>
 * If more than one {@code Subscriber} attempts to subscribe to this Processor, they
 * will receive an {@link IllegalStateException} if this {@link UnicastProcessor} hasn't terminated yet,
 * or the Subscribers receive the terminal event (error or completion) if this
 * Processor has terminated.
 * <p>
 * The {@code UnicastProcessor} buffers notifications and replays them to the single {@code Subscriber} as requested,
 * for which it holds upstream items an unbounded internal buffer until they can be emitted.
 * <p>
 * Since a {@code UnicastProcessor} is a Reactive Streams {@code Processor},
 * {@code null}s are not allowed (<a href="https://github.com/reactive-streams/reactive-streams-jvm#2.13">Rule 2.13</a>) as
 * parameters to {@link #onNext(Object)} and {@link #onError(Throwable)}. Such calls will result in a
 * {@link NullPointerException} being thrown and the processor's state is not changed.
 * <p>
 * Since a {@code UnicastProcessor} is a {@link io.reactivex.Flowable} as well as a {@link FlowableProcessor}, it
 * honors the downstream backpressure but consumes an upstream source in an unbounded manner (requesting {@code Long.MAX_VALUE}).
 * <p>
 * When this {@code UnicastProcessor} is terminated via {@link #onError(Throwable)} the current or late single {@code Subscriber}
 * may receive the {@code Throwable} before any available items could be emitted. To make sure an {@code onError} event is delivered
 * to the {@code Subscriber} after the normal items, create a {@code UnicastProcessor} with the {@link #create(boolean)} or
 * {@link #create(int, Runnable, boolean)} factory methods.
 * <p>
 * Even though {@code UnicastProcessor} implements the {@code Subscriber} interface, calling
 * {@code onSubscribe} is not required (<a href="https://github.com/reactive-streams/reactive-streams-jvm#2.12">Rule 2.12</a>)
 * if the processor is used as a standalone source. However, calling {@code onSubscribe}
 * after the {@code UnicastProcessor} reached its terminal state will result in the
 * given {@code Subscription} being canceled immediately.
 * <p>
 * Calling {@link #onNext(Object)}, {@link #onError(Throwable)} and {@link #onComplete()}
 * is required to be serialized (called from the same thread or called non-overlappingly from different threads
 * through external means of serialization). The {@link #toSerialized()} method available to all {@link FlowableProcessor}s
 * provides such serialization and also protects against reentrance (i.e., when a downstream {@code Subscriber}
 * consuming this processor also wants to call {@link #onNext(Object)} on this processor recursively).
 * <p>
 * This {@code UnicastProcessor} supports the standard state-peeking methods {@link #hasComplete()}, {@link #hasThrowable()},
 * {@link #getThrowable()} and {@link #hasSubscribers()}.
 * <dl>
 *  <dt><b>Backpressure:</b></dt>
 *  <dd>{@code UnicastProcessor} honors the downstream backpressure but consumes an upstream source
 *  (if any) in an unbounded manner (requesting {@code Long.MAX_VALUE}).</dd>
 *  <dt><b>Scheduler:</b></dt>
 *  <dd>{@code UnicastProcessor} does not operate by default on a particular {@link io.reactivex.Scheduler} and
 *  the single {@code Subscriber} gets notified on the thread the respective {@code onXXX} methods were invoked.</dd>
 *  <dt><b>Error handling:</b></dt>
 *  <dd>When the {@link #onError(Throwable)} is called, the {@code UnicastProcessor} enters into a terminal state
 *  and emits the same {@code Throwable} instance to the current single {@code Subscriber}. During this emission,
 *  if the single {@code Subscriber}s cancels its respective {@code Subscription}s, the
 *  {@code Throwable} is delivered to the global error handler via
 *  {@link io.reactivex.plugins.RxJavaPlugins#onError(Throwable)}.
 *  If there were no {@code Subscriber}s subscribed to this {@code UnicastProcessor} when the {@code onError()}
 *  was called, the global error handler is not invoked.
 *  </dd>
 * </dl>
 * <p>
 * Example usage:
 * <pre><code>
 * UnicastProcessor&lt;Integer&gt; processor = UnicastProcessor.create();
 *
 * TestSubscriber&lt;Integer&gt; ts1 = processor.test();
 *
 * // fresh UnicastProcessors are empty
 * ts1.assertEmpty();
 *
 * TestSubscriber&lt;Integer&gt; ts2 = processor.test();
 *
 * // A UnicastProcessor only allows one Subscriber during its lifetime
 * ts2.assertFailure(IllegalStateException.class);
 *
 * processor.onNext(1);
 * ts1.assertValue(1);
 *
 * processor.onNext(2);
 * ts1.assertValues(1, 2);
 *
 * processor.onComplete();
 * ts1.assertResult(1, 2);
 *
 * // ----------------------------------------------------
 *
 * UnicastProcessor&lt;Integer&gt; processor2 = UnicastProcessor.create();
 *
 * // a UnicastProcessor caches events until its single Subscriber subscribes
 * processor2.onNext(1);
 * processor2.onNext(2);
 * processor2.onComplete();
 *
 * TestSubscriber&lt;Integer&gt; ts3 = processor2.test();
 *
 * // the cached events are emitted in order
 * ts3.assertResult(1, 2);
 * </code></pre>
 *
 * @param <T> the value type received and emitted by this Processor subclass
 * @since 2.0
 */
public final class UnicastProcessor<T> extends FlowableProcessor<T> {

    final SpscLinkedArrayQueue<T> queue;

    final AtomicReference<Runnable> onTerminate;

    final boolean delayError;

    volatile boolean done;

    Throwable error;

    final AtomicReference<Subscriber<? super T>> actual;

    volatile boolean cancelled;

    final AtomicBoolean once;

    final BasicIntQueueSubscription<T> wip;

    final AtomicLong requested;

    boolean enableOperatorFusion;

    /**
     * Creates an UnicastSubject with an internal buffer capacity hint 16.
     * @param <T> the value type
     * @return an UnicastSubject instance
     */
    @CheckReturnValue
    @NonNull
    public static <T> UnicastProcessor<T> create() {
        return new UnicastProcessor<T>(bufferSize());
    }

    /**
     * Creates an UnicastProcessor with the given internal buffer capacity hint.
     * @param <T> the value type
     * @param capacityHint the hint to size the internal unbounded buffer
     * @return an UnicastProcessor instance
     */
    @CheckReturnValue
    @NonNull
    public static <T> UnicastProcessor<T> create(int capacityHint) {
        return new UnicastProcessor<T>(capacityHint);
    }

    /**
     * Creates an UnicastProcessor with default internal buffer capacity hint and delay error flag.
     * <p>History: 2.0.8 - experimental
     * @param <T> the value type
     * @param delayError deliver pending onNext events before onError
     * @return an UnicastProcessor instance
     * @since 2.2
     */
    @CheckReturnValue
    @NonNull
    public static <T> UnicastProcessor<T> create(boolean delayError) {
        return new UnicastProcessor<T>(bufferSize(), null, delayError);
    }

    /**
     * Creates an UnicastProcessor with the given internal buffer capacity hint and a callback for
     * the case when the single Subscriber cancels its subscription.
     *
     * <p>The callback, if not null, is called exactly once and
     * non-overlapped with any active replay.
     *
     * @param <T> the value type
     * @param capacityHint the hint to size the internal unbounded buffer
     * @param onCancelled the non null callback
     * @return an UnicastProcessor instance
     */
    @CheckReturnValue
    @NonNull
    public static <T> UnicastProcessor<T> create(int capacityHint, Runnable onCancelled) {
        ObjectHelper.requireNonNull(onCancelled, "onTerminate");
        return new UnicastProcessor<T>(capacityHint, onCancelled);
    }

    /**
     * Creates an UnicastProcessor with the given internal buffer capacity hint, delay error flag and a callback for
     * the case when the single Subscriber cancels its subscription.
     *
     * <p>The callback, if not null, is called exactly once and
     * non-overlapped with any active replay.
     * <p>History: 2.0.8 - experimental
     * @param <T> the value type
     * @param capacityHint the hint to size the internal unbounded buffer
     * @param onCancelled the non null callback
     * @param delayError deliver pending onNext events before onError
     * @return an UnicastProcessor instance
     * @since 2.2
     */
    @CheckReturnValue
    @NonNull
    public static <T> UnicastProcessor<T> create(int capacityHint, Runnable onCancelled, boolean delayError) {
        ObjectHelper.requireNonNull(onCancelled, "onTerminate");
        return new UnicastProcessor<T>(capacityHint, onCancelled, delayError);
    }

    /**
     * Creates an UnicastProcessor with the given capacity hint.
     * @param capacityHint the capacity hint for the internal, unbounded queue
     * @since 2.0
     */
    UnicastProcessor(int capacityHint) {
        this(capacityHint,null, true);
    }

    /**
     * Creates an UnicastProcessor with the given capacity hint and callback
     * for when the Processor is terminated normally or its single Subscriber cancels.
     * @param capacityHint the capacity hint for the internal, unbounded queue
     * @param onTerminate the callback to run when the Processor is terminated or cancelled, null not allowed
     * @since 2.0
     */
    UnicastProcessor(int capacityHint, Runnable onTerminate) {
        this(capacityHint, onTerminate, true);
    }

    /**
     * Creates an UnicastProcessor with the given capacity hint and callback
     * for when the Processor is terminated normally or its single Subscriber cancels.
     * <p>History: 2.0.8 - experimental
     * @param capacityHint the capacity hint for the internal, unbounded queue
     * @param onTerminate the callback to run when the Processor is terminated or cancelled, null not allowed
     * @param delayError deliver pending onNext events before onError
     * @since 2.2
     */
    UnicastProcessor(int capacityHint, Runnable onTerminate, boolean delayError) {
        this.queue = new SpscLinkedArrayQueue<T>(ObjectHelper.verifyPositive(capacityHint, "capacityHint"));
        this.onTerminate = new AtomicReference<Runnable>(onTerminate);
        this.delayError = delayError;
        this.actual = new AtomicReference<Subscriber<? super T>>();
        this.once = new AtomicBoolean();
        this.wip = new UnicastQueueSubscription();
        this.requested = new AtomicLong();
    }

    void doTerminate() {
        Runnable r = onTerminate.getAndSet(null);
        if (r != null) {
            r.run();
        }
    }

    void drainRegular(Subscriber<? super T> a) {
        int missed = 1;

        final SpscLinkedArrayQueue<T> q = queue;
        final boolean failFast = !delayError;
        for (;;) {

            long r = requested.get();
            long e = 0L;

            while (r != e) {
                boolean d = done;

                T t = q.poll();
                boolean empty = t == null;

                if (checkTerminated(failFast, d, empty, a, q)) {
                    return;
                }

                if (empty) {
                    break;
                }

                a.onNext(t);

                e++;
            }

            if (r == e && checkTerminated(failFast, done, q.isEmpty(), a, q)) {
                return;
            }

            if (e != 0 && r != Long.MAX_VALUE) {
                requested.addAndGet(-e);
            }

            missed = wip.addAndGet(-missed);
            if (missed == 0) {
                break;
            }
        }
    }

    void drainFused(Subscriber<? super T> a) {
        int missed = 1;

        final SpscLinkedArrayQueue<T> q = queue;
        final boolean failFast = !delayError;
        for (;;) {

            if (cancelled) {
                q.clear();
                actual.lazySet(null);
                return;
            }

            boolean d = done;

            if (failFast && d && error != null) {
                q.clear();
                actual.lazySet(null);
                a.onError(error);
                return;
            }
            a.onNext(null);

            if (d) {
                actual.lazySet(null);

                Throwable ex = error;
                if (ex != null) {
                    a.onError(ex);
                } else {
                    a.onComplete();
                }
                return;
            }

            missed = wip.addAndGet(-missed);
            if (missed == 0) {
                break;
            }
        }
    }

    void drain() {
        if (wip.getAndIncrement() != 0) {
            return;
        }

        int missed = 1;

        Subscriber<? super T> a = actual.get();
        for (;;) {
            if (a != null) {

                if (enableOperatorFusion) {
                    drainFused(a);
                } else {
                    drainRegular(a);
                }
                return;
            }

            missed = wip.addAndGet(-missed);
            if (missed == 0) {
                break;
            }
            a = actual.get();
        }
    }

    boolean checkTerminated(boolean failFast, boolean d, boolean empty, Subscriber<? super T> a, SpscLinkedArrayQueue<T> q) {
        if (cancelled) {
            q.clear();
            actual.lazySet(null);
            return true;
        }

        if (d) {
            if (failFast && error != null) {
                q.clear();
                actual.lazySet(null);
                a.onError(error);
                return true;
            }
            if (empty) {
                Throwable e = error;
                actual.lazySet(null);
                if (e != null) {
                    a.onError(e);
                } else {
                    a.onComplete();
                }
                return true;
            }
        }

        return false;
    }

    @Override
    public void onSubscribe(Subscription s) {
        if (done || cancelled) {
            s.cancel();
        } else {
            s.request(Long.MAX_VALUE);
        }
    }

    @Override
    public void onNext(T t) {
        ObjectHelper.requireNonNull(t, "onNext called with null. Null values are generally not allowed in 2.x operators and sources.");

        if (done || cancelled) {
            return;
        }

        queue.offer(t);
        drain();
    }

    @Override
    public void onError(Throwable t) {
        ObjectHelper.requireNonNull(t, "onError called with null. Null values are generally not allowed in 2.x operators and sources.");

        if (done || cancelled) {
            RxJavaPlugins.onError(t);
            return;
        }

        error = t;
        done = true;

        doTerminate();

        drain();
    }

    @Override
    public void onComplete() {
        if (done || cancelled) {
            return;
        }

        done = true;

        doTerminate();

        drain();
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> s) {
        if (!once.get() && once.compareAndSet(false, true)) {

            s.onSubscribe(wip);
            actual.set(s);
            if (cancelled) {
                actual.lazySet(null);
            } else {
                drain();
            }
        } else {
            EmptySubscription.error(new IllegalStateException("This processor allows only a single Subscriber"), s);
        }
    }

    final class UnicastQueueSubscription extends BasicIntQueueSubscription<T> {


        private static final long serialVersionUID = -4896760517184205454L;

        @Nullable
        @Override
        public T poll() {
            return queue.poll();
        }

        @Override
        public boolean isEmpty() {
            return queue.isEmpty();
        }

        @Override
        public void clear() {
            queue.clear();
        }

        @Override
        public int requestFusion(int requestedMode) {
            if ((requestedMode & QueueSubscription.ASYNC) != 0) {
                enableOperatorFusion = true;
                return QueueSubscription.ASYNC;
            }
            return QueueSubscription.NONE;
        }

        @Override
        public void request(long n) {
            if (SubscriptionHelper.validate(n)) {
                BackpressureHelper.add(requested, n);
                drain();
            }
        }

        @Override
        public void cancel() {
            if (cancelled) {
                return;
            }
            cancelled = true;

            doTerminate();

            if (!enableOperatorFusion) {
                if (wip.getAndIncrement() == 0) {
                    queue.clear();
                    actual.lazySet(null);
                }
            }
        }
    }

    @Override
    public boolean hasSubscribers() {
        return actual.get() != null;
    }

    @Override
    @Nullable
    public Throwable getThrowable() {
        if (done) {
            return error;
        }
        return null;
    }

    @Override
    public boolean hasComplete() {
        return done && error == null;
    }

    @Override
    public boolean hasThrowable() {
        return done && error != null;
    }
}
