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
package io.reactivex.rxjava3.processors;

import java.util.concurrent.atomic.AtomicReference;

import org.reactivestreams.*;

import io.reactivex.rxjava3.annotations.*;
import io.reactivex.rxjava3.internal.subscriptions.DeferredScalarSubscription;
import io.reactivex.rxjava3.internal.util.ExceptionHelper;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;

/**
 * Processor that emits the very last value followed by a completion event or the received error
 * to {@link Subscriber}s.
 * <p>
 * <img width="640" height="239" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/AsyncProcessor.png" alt="">
 * <p>
 * This processor does not have a public constructor by design; a new empty instance of this
 * {@code AsyncProcessor} can be created via the {@link #create()} method.
 * <p>
 * Since an {@code AsyncProcessor} is a Reactive Streams {@code Processor} type,
 * {@code null}s are not allowed (<a href="https://github.com/reactive-streams/reactive-streams-jvm#2.13">Rule 2.13</a>)
 * as parameters to {@link #onNext(Object)} and {@link #onError(Throwable)}. Such calls will result in a
 * {@link NullPointerException} being thrown and the processor's state is not changed.
 * <p>
 * {@code AsyncProcessor} is a {@link io.reactivex.rxjava3.core.Flowable} as well as a {@link FlowableProcessor} and supports backpressure from the downstream but
 * its {@link Subscriber}-side consumes items in an unbounded manner.
 * <p>
 * When this {@code AsyncProcessor} is terminated via {@link #onError(Throwable)}, the
 * last observed item (if any) is cleared and late {@link Subscriber}s only receive
 * the {@code onError} event.
 * <p>
 * The {@code AsyncProcessor} caches the latest item internally and it emits this item only when {@code onComplete} is called.
 * Therefore, it is not recommended to use this {@code Processor} with infinite or never-completing sources.
 * <p>
 * Even though {@code AsyncProcessor} implements the {@link Subscriber} interface, calling
 * {@code onSubscribe} is not required (<a href="https://github.com/reactive-streams/reactive-streams-jvm#2.12">Rule 2.12</a>)
 * if the processor is used as a standalone source. However, calling {@code onSubscribe}
 * after the {@code AsyncProcessor} reached its terminal state will result in the
 * given {@link Subscription} being canceled immediately.
 * <p>
 * Calling {@link #onNext(Object)}, {@link #onError(Throwable)} and {@link #onComplete()}
 * is required to be serialized (called from the same thread or called non-overlappingly from different threads
 * through external means of serialization). The {@link #toSerialized()} method available to all {@code FlowableProcessor}s
 * provides such serialization and also protects against reentrance (i.e., when a downstream {@code Subscriber}
 * consuming this processor also wants to call {@link #onNext(Object)} on this processor recursively).
 * The implementation of {@code onXXX} methods are technically thread-safe but non-serialized calls
 * to them may lead to undefined state in the currently subscribed {@code Subscriber}s.
 * <p>
 * This {@code AsyncProcessor} supports the standard state-peeking methods {@link #hasComplete()}, {@link #hasThrowable()},
 * {@link #getThrowable()} and {@link #hasSubscribers()} as well as means to read the very last observed value -
 * after this {@code AsyncProcessor} has been completed - in a non-blocking and thread-safe
 * manner via {@link #hasValue()} or {@link #getValue()}.
 * <dl>
 *  <dt><b>Backpressure:</b></dt>
 *  <dd>The {@code AsyncProcessor} honors the backpressure of the downstream {@code Subscriber}s and won't emit
 *  its single value to a particular {@code Subscriber} until that {@code Subscriber} has requested an item.
 *  When the {@code AsyncProcessor} is subscribed to a {@link io.reactivex.rxjava3.core.Flowable}, the processor consumes this
 *  {@code Flowable} in an unbounded manner (requesting {@link Long#MAX_VALUE}) as only the very last upstream item is
 *  retained by it.
 *  </dd>
 *  <dt><b>Scheduler:</b></dt>
 *  <dd>{@code AsyncProcessor} does not operate by default on a particular {@link io.reactivex.rxjava3.core.Scheduler} and
 *  the {@code Subscriber}s get notified on the thread where the terminating {@code onError} or {@code onComplete}
 *  methods were invoked.</dd>
 *  <dt><b>Error handling:</b></dt>
 *  <dd>When the {@link #onError(Throwable)} is called, the {@code AsyncProcessor} enters into a terminal state
 *  and emits the same {@code Throwable} instance to the last set of {@code Subscriber}s. During this emission,
 *  if one or more {@code Subscriber}s dispose their respective {@code Subscription}s, the
 *  {@code Throwable} is delivered to the global error handler via
 *  {@link io.reactivex.rxjava3.plugins.RxJavaPlugins#onError(Throwable)} (multiple times if multiple {@code Subscriber}s
 *  cancel at once).
 *  If there were no {@code Subscriber}s subscribed to this {@code AsyncProcessor} when the {@code onError()}
 *  was called, the global error handler is not invoked.
 *  </dd>
 * </dl>
 * <p>
 * Example usage:
 * <pre><code>
 * AsyncProcessor&lt;Object&gt; processor = AsyncProcessor.create();
 * 
 * TestSubscriber&lt;Object&gt; ts1 = processor.test();
 *
 * ts1.assertEmpty();
 *
 * processor.onNext(1);
 *
 * // AsyncProcessor only emits when onComplete was called.
 * ts1.assertEmpty();
 *
 * processor.onNext(2);
 * processor.onComplete();
 *
 * // onComplete triggers the emission of the last cached item and the onComplete event.
 * ts1.assertResult(2);
 *
 * TestSubscriber&lt;Object&gt; ts2 = processor.test();
 *
 * // late Subscribers receive the last cached item too
 * ts2.assertResult(2);
 * </code></pre>
 * @param <T> the value type
 */
public final class AsyncProcessor<T> extends FlowableProcessor<T> {

    @SuppressWarnings("rawtypes")
    static final AsyncSubscription[] EMPTY = new AsyncSubscription[0];

    @SuppressWarnings("rawtypes")
    static final AsyncSubscription[] TERMINATED = new AsyncSubscription[0];

    final AtomicReference<AsyncSubscription<T>[]> subscribers;

    /** Write before updating subscribers, read after reading subscribers as TERMINATED. */
    Throwable error;

    /** Write before updating subscribers, read after reading subscribers as TERMINATED. */
    T value;

    /**
     * Creates a new AsyncProcessor.
     * @param <T> the value type to be received and emitted
     * @return the new AsyncProcessor instance
     */
    @CheckReturnValue
    @NonNull
    public static <T> AsyncProcessor<T> create() {
        return new AsyncProcessor<T>();
    }

    /**
     * Constructs an AsyncProcessor.
     * @since 2.0
     */
    @SuppressWarnings("unchecked")
    AsyncProcessor() {
        this.subscribers = new AtomicReference<AsyncSubscription<T>[]>(EMPTY);
    }

    @Override
    public void onSubscribe(Subscription s) {
        if (subscribers.get() == TERMINATED) {
            s.cancel();
            return;
        }
        // AsyncProcessor doesn't bother with request coordination.
        s.request(Long.MAX_VALUE);
    }

    @Override
    public void onNext(T t) {
        ExceptionHelper.nullCheck(t, "onNext called with a null value.");
        if (subscribers.get() == TERMINATED) {
            return;
        }
        value = t;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onError(Throwable t) {
        ExceptionHelper.nullCheck(t, "onError called with a null Throwable.");
        if (subscribers.get() == TERMINATED) {
            RxJavaPlugins.onError(t);
            return;
        }
        value = null;
        error = t;
        for (AsyncSubscription<T> as : subscribers.getAndSet(TERMINATED)) {
            as.onError(t);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onComplete() {
        if (subscribers.get() == TERMINATED) {
            return;
        }
        T v = value;
        AsyncSubscription<T>[] array = subscribers.getAndSet(TERMINATED);
        if (v == null) {
            for (AsyncSubscription<T> as : array) {
                as.onComplete();
            }
        } else {
            for (AsyncSubscription<T> as : array) {
                as.complete(v);
            }
        }
    }

    @Override
    public boolean hasSubscribers() {
        return subscribers.get().length != 0;
    }

    @Override
    public boolean hasThrowable() {
        return subscribers.get() == TERMINATED && error != null;
    }

    @Override
    public boolean hasComplete() {
        return subscribers.get() == TERMINATED && error == null;
    }

    @Override
    @Nullable
    public Throwable getThrowable() {
        return subscribers.get() == TERMINATED ? error : null;
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> s) {
        AsyncSubscription<T> as = new AsyncSubscription<T>(s, this);
        s.onSubscribe(as);
        if (add(as)) {
            if (as.isCancelled()) {
                remove(as);
            }
        } else {
            Throwable ex = error;
            if (ex != null) {
                s.onError(ex);
            } else {
                T v = value;
                if (v != null) {
                    as.complete(v);
                } else {
                    as.onComplete();
                }
            }
        }
    }

    /**
     * Tries to add the given subscriber to the subscribers array atomically
     * or returns false if the processor has terminated.
     * @param ps the subscriber to add
     * @return true if successful, false if the processor has terminated
     */
    boolean add(AsyncSubscription<T> ps) {
        for (;;) {
            AsyncSubscription<T>[] a = subscribers.get();
            if (a == TERMINATED) {
                return false;
            }

            int n = a.length;
            @SuppressWarnings("unchecked")
            AsyncSubscription<T>[] b = new AsyncSubscription[n + 1];
            System.arraycopy(a, 0, b, 0, n);
            b[n] = ps;

            if (subscribers.compareAndSet(a, b)) {
                return true;
            }
        }
    }

    /**
     * Atomically removes the given subscriber if it is subscribed to this processor.
     * @param ps the subscriber's subscription wrapper to remove
     */
    @SuppressWarnings("unchecked")
    void remove(AsyncSubscription<T> ps) {
        for (;;) {
            AsyncSubscription<T>[] a = subscribers.get();
            int n = a.length;
            if (n == 0) {
                return;
            }

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

            AsyncSubscription<T>[] b;

            if (n == 1) {
                b = EMPTY;
            } else {
                b = new AsyncSubscription[n - 1];
                System.arraycopy(a, 0, b, 0, j);
                System.arraycopy(a, j + 1, b, j, n - j - 1);
            }
            if (subscribers.compareAndSet(a, b)) {
                return;
            }
        }
    }

    /**
     * Returns true if this processor has any value.
     * <p>The method is thread-safe.
     * @return true if this processor has any value
     */
    public boolean hasValue() {
        return subscribers.get() == TERMINATED && value != null;
    }

    /**
     * Returns a single value this processor currently has or null if no such value exists.
     * <p>The method is thread-safe.
     * @return a single value this processor currently has or null if no such value exists
     */
    @Nullable
    public T getValue() {
        return subscribers.get() == TERMINATED ? value : null;
    }

    static final class AsyncSubscription<T> extends DeferredScalarSubscription<T> {
        private static final long serialVersionUID = 5629876084736248016L;

        final AsyncProcessor<T> parent;

        AsyncSubscription(Subscriber<? super T> actual, AsyncProcessor<T> parent) {
            super(actual);
            this.parent = parent;
        }

        @Override
        public void cancel() {
            if (super.tryCancel()) {
                parent.remove(this);
            }
        }

        void onComplete() {
            if (!isCancelled()) {
                downstream.onComplete();
            }
        }

        void onError(Throwable t) {
            if (isCancelled()) {
                RxJavaPlugins.onError(t);
            } else {
                downstream.onError(t);
            }
        }
    }
}
