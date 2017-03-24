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

import io.reactivex.annotations.Experimental;
import io.reactivex.annotations.Nullable;
import org.reactivestreams.*;

import io.reactivex.internal.functions.ObjectHelper;
import io.reactivex.internal.fuseable.QueueSubscription;
import io.reactivex.internal.queue.SpscLinkedArrayQueue;
import io.reactivex.internal.subscriptions.*;
import io.reactivex.internal.util.BackpressureHelper;
import io.reactivex.plugins.RxJavaPlugins;

/**
 * Processor that allows only a single Subscriber to subscribe to it during its lifetime.
 *
 * <p>This processor buffers notifications and replays them to the Subscriber as requested.
 *
 * <p>This processor holds an unbounded internal buffer.
 *
 * <p>If more than one Subscriber attempts to subscribe to this Processor, they
 * will receive an IllegalStateException if this Processor hasn't terminated yet,
 * or the Subscribers receive the terminal event (error or completion) if this
 * Processor has terminated.
 * <p>
 * <img width="640" height="370" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/UnicastProcessor.png" alt="">
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
    public static <T> UnicastProcessor<T> create(int capacityHint) {
        return new UnicastProcessor<T>(capacityHint);
    }

    /**
     * Creates an UnicastProcessor with default internal buffer capacity hint and delay error flag.
     * @param <T> the value type
     * @param delayError deliver pending onNext events before onError
     * @return an UnicastProcessor instance
     * @since 2.0.8 - experimental
     */
    @CheckReturnValue
    @Experimental
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
     *
     * @param <T> the value type
     * @param capacityHint the hint to size the internal unbounded buffer
     * @param onCancelled the non null callback
     * @param delayError deliver pending onNext events before onError
     * @return an UnicastProcessor instance
     * @since 2.0.8 - experimental
     */
    @CheckReturnValue
    @Experimental
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
     * @param capacityHint the capacity hint for the internal, unbounded queue
     * @param onTerminate the callback to run when the Processor is terminated or cancelled, null not allowed
     * @param delayError deliver pending onNext events before onError
     * @since 2.0.8 - experimental
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
        Runnable r = onTerminate.get();
        if (r != null && onTerminate.compareAndSet(r, null)) {
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
        if (done || cancelled) {
            return;
        }

        if (t == null) {
            onError(new NullPointerException("onNext called with null. Null values are generally not allowed in 2.x operators and sources."));
            return;
        }

        queue.offer(t);
        drain();
    }

    @Override
    public void onError(Throwable t) {
        if (done || cancelled) {
            RxJavaPlugins.onError(t);
            return;
        }

        if (t == null) {
            t = new NullPointerException("onError called with null. Null values are generally not allowed in 2.x operators and sources.");
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
