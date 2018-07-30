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
import io.reactivex.exceptions.MissingBackpressureException;
import io.reactivex.internal.functions.ObjectHelper;
import io.reactivex.internal.subscriptions.SubscriptionHelper;
import io.reactivex.internal.util.BackpressureHelper;
import io.reactivex.plugins.RxJavaPlugins;

/**
 * Processor that multicasts all subsequently observed items to its current {@link Subscriber}s.
 *
 * <p>
 * <img width="640" height="278" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/PublishProcessor.png" alt="">
 * <p>
 * This processor does not have a public constructor by design; a new empty instance of this
 * {@code PublishProcessor} can be created via the {@link #create()} method.
 * <p>
 * Since a {@code PublishProcessor} is a Reactive Streams {@code Processor} type,
 * {@code null}s are not allowed (<a href="https://github.com/reactive-streams/reactive-streams-jvm#2.13">Rule 2.13</a>) as
 * parameters to {@link #onNext(Object)} and {@link #onError(Throwable)}. Such calls will result in a
 * {@link NullPointerException} being thrown and the processor's state is not changed.
 * <p>
 * {@code PublishProcessor} is a {@link io.reactivex.Flowable} as well as a {@link FlowableProcessor},
 * however, it does not coordinate backpressure between different subscribers and between an
 * upstream source and a subscriber. If an upstream item is received via {@link #onNext(Object)}, if
 * a subscriber is not ready to receive an item, that subscriber is terminated via a {@link MissingBackpressureException}.
 * To avoid this case, use {@link #offer(Object)} and retry sometime later if it returned false.
 * The {@code PublishProcessor}'s {@link Subscriber}-side consumes items in an unbounded manner.
 * <p>
 * For a multicasting processor type that also coordinates between the downstream {@code Subscriber}s and the upstream
 * source as well, consider using {@link MulticastProcessor}.
 * <p>
 * When this {@code PublishProcessor} is terminated via {@link #onError(Throwable)} or {@link #onComplete()},
 * late {@link Subscriber}s only receive the respective terminal event.
 * <p>
 * Unlike a {@link BehaviorProcessor}, a {@code PublishProcessor} doesn't retain/cache items, therefore, a new
 * {@code Subscriber} won't receive any past items.
 * <p>
 * Even though {@code PublishProcessor} implements the {@link Subscriber} interface, calling
 * {@code onSubscribe} is not required (<a href="https://github.com/reactive-streams/reactive-streams-jvm#2.12">Rule 2.12</a>)
 * if the processor is used as a standalone source. However, calling {@code onSubscribe}
 * after the {@code PublishProcessor} reached its terminal state will result in the
 * given {@link Subscription} being canceled immediately.
 * <p>
 * Calling {@link #onNext(Object)}, {@link #offer(Object)}, {@link #onError(Throwable)} and {@link #onComplete()}
 * is required to be serialized (called from the same thread or called non-overlappingly from different threads
 * through external means of serialization). The {@link #toSerialized()} method available to all {@link FlowableProcessor}s
 * provides such serialization and also protects against reentrance (i.e., when a downstream {@code Subscriber}
 * consuming this processor also wants to call {@link #onNext(Object)} on this processor recursively).
 * Note that serializing over {@link #offer(Object)} is not supported through {@code toSerialized()} because it is a method
 * available on the {@code PublishProcessor} and {@code BehaviorProcessor} classes only.
 * <p>
 * This {@code PublishProcessor} supports the standard state-peeking methods {@link #hasComplete()}, {@link #hasThrowable()},
 * {@link #getThrowable()} and {@link #hasSubscribers()}.
 * <dl>
 *  <dt><b>Backpressure:</b></dt>
 *  <dd>The processor does not coordinate backpressure for its subscribers and implements a weaker {@code onSubscribe} which
 *  calls requests Long.MAX_VALUE from the incoming Subscriptions. This makes it possible to subscribe the {@code PublishProcessor}
 *  to multiple sources (note on serialization though) unlike the standard {@code Subscriber} contract. Child subscribers, however, are not overflown but receive an
 *  {@link IllegalStateException} in case their requested amount is zero.</dd>
 *  <dt><b>Scheduler:</b></dt>
 *  <dd>{@code PublishProcessor} does not operate by default on a particular {@link io.reactivex.Scheduler} and
 *  the {@code Subscriber}s get notified on the thread the respective {@code onXXX} methods were invoked.</dd>
 *  <dt><b>Error handling:</b></dt>
 *  <dd>When the {@link #onError(Throwable)} is called, the {@code PublishProcessor} enters into a terminal state
 *  and emits the same {@code Throwable} instance to the last set of {@code Subscriber}s. During this emission,
 *  if one or more {@code Subscriber}s cancel their respective {@code Subscription}s, the
 *  {@code Throwable} is delivered to the global error handler via
 *  {@link io.reactivex.plugins.RxJavaPlugins#onError(Throwable)} (multiple times if multiple {@code Subscriber}s
 *  cancel at once).
 *  If there were no {@code Subscriber}s subscribed to this {@code PublishProcessor} when the {@code onError()}
 *  was called, the global error handler is not invoked.
 *  </dd>
 * </dl>
 *
 * Example usage:
 * <pre> {@code

  PublishProcessor<Object> processor = PublishProcessor.create();
  // subscriber1 will receive all onNext and onComplete events
  processor.subscribe(subscriber1);
  processor.onNext("one");
  processor.onNext("two");
  // subscriber2 will only receive "three" and onComplete
  processor.subscribe(subscriber2);
  processor.onNext("three");
  processor.onComplete();

  } </pre>
 * @param <T> the value type multicasted to Subscribers.
 * @see MulticastProcessor
 */
public final class PublishProcessor<T> extends FlowableProcessor<T> {
    /** The terminated indicator for the subscribers array. */
    @SuppressWarnings("rawtypes")
    static final PublishSubscription[] TERMINATED = new PublishSubscription[0];
    /** An empty subscribers array to avoid allocating it all the time. */
    @SuppressWarnings("rawtypes")
    static final PublishSubscription[] EMPTY = new PublishSubscription[0];

    /** The array of currently subscribed subscribers. */
    final AtomicReference<PublishSubscription<T>[]> subscribers;

    /** The error, write before terminating and read after checking subscribers. */
    Throwable error;

    /**
     * Constructs a PublishProcessor.
     * @param <T> the value type
     * @return the new PublishProcessor
     */
    @CheckReturnValue
    @NonNull
    public static <T> PublishProcessor<T> create() {
        return new PublishProcessor<T>();
    }

    /**
     * Constructs a PublishProcessor.
     * @since 2.0
     */
    @SuppressWarnings("unchecked")
    PublishProcessor() {
        subscribers = new AtomicReference<PublishSubscription<T>[]>(EMPTY);
    }


    @Override
    protected void subscribeActual(Subscriber<? super T> t) {
        PublishSubscription<T> ps = new PublishSubscription<T>(t, this);
        t.onSubscribe(ps);
        if (add(ps)) {
            // if cancellation happened while a successful add, the remove() didn't work
            // so we need to do it again
            if (ps.isCancelled()) {
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
     * or returns false if this processor has terminated.
     * @param ps the subscriber to add
     * @return true if successful, false if this processor has terminated
     */
    boolean add(PublishSubscription<T> ps) {
        for (;;) {
            PublishSubscription<T>[] a = subscribers.get();
            if (a == TERMINATED) {
                return false;
            }

            int n = a.length;
            @SuppressWarnings("unchecked")
            PublishSubscription<T>[] b = new PublishSubscription[n + 1];
            System.arraycopy(a, 0, b, 0, n);
            b[n] = ps;

            if (subscribers.compareAndSet(a, b)) {
                return true;
            }
        }
    }

    /**
     * Atomically removes the given subscriber if it is subscribed to this processor.
     * @param ps the subscription wrapping a subscriber to remove
     */
    @SuppressWarnings("unchecked")
    void remove(PublishSubscription<T> ps) {
        for (;;) {
            PublishSubscription<T>[] a = subscribers.get();
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

            PublishSubscription<T>[] b;

            if (n == 1) {
                b = EMPTY;
            } else {
                b = new PublishSubscription[n - 1];
                System.arraycopy(a, 0, b, 0, j);
                System.arraycopy(a, j + 1, b, j, n - j - 1);
            }
            if (subscribers.compareAndSet(a, b)) {
                return;
            }
        }
    }

    @Override
    public void onSubscribe(Subscription s) {
        if (subscribers.get() == TERMINATED) {
            s.cancel();
            return;
        }
        // PublishProcessor doesn't bother with request coordination.
        s.request(Long.MAX_VALUE);
    }

    @Override
    public void onNext(T t) {
        ObjectHelper.requireNonNull(t, "onNext called with null. Null values are generally not allowed in 2.x operators and sources.");
        for (PublishSubscription<T> s : subscribers.get()) {
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

        for (PublishSubscription<T> s : subscribers.getAndSet(TERMINATED)) {
            s.onError(t);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onComplete() {
        if (subscribers.get() == TERMINATED) {
            return;
        }
        for (PublishSubscription<T> s : subscribers.getAndSet(TERMINATED)) {
            s.onComplete();
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
        PublishSubscription<T>[] array = subscribers.get();

        for (PublishSubscription<T> s : array) {
            if (s.isFull()) {
                return false;
            }
        }

        for (PublishSubscription<T> s : array) {
            s.onNext(t);
        }
        return true;
    }

    @Override
    public boolean hasSubscribers() {
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
    static final class PublishSubscription<T> extends AtomicLong implements Subscription {

        private static final long serialVersionUID = 3562861878281475070L;
        /** The actual subscriber. */
        final Subscriber<? super T> actual;
        /** The parent processor servicing this subscriber. */
        final PublishProcessor<T> parent;

        /**
         * Constructs a PublishSubscriber, wraps the actual subscriber and the state.
         * @param actual the actual subscriber
         * @param parent the parent PublishProcessor
         */
        PublishSubscription(Subscriber<? super T> actual, PublishProcessor<T> parent) {
            this.actual = actual;
            this.parent = parent;
        }

        public void onNext(T t) {
            long r = get();
            if (r == Long.MIN_VALUE) {
                return;
            }
            if (r != 0L) {
                actual.onNext(t);
                BackpressureHelper.producedCancel(this, 1);
            } else {
                cancel();
                actual.onError(new MissingBackpressureException("Could not emit value due to lack of requests"));
            }
        }

        public void onError(Throwable t) {
            if (get() != Long.MIN_VALUE) {
                actual.onError(t);
            } else {
                RxJavaPlugins.onError(t);
            }
        }

        public void onComplete() {
            if (get() != Long.MIN_VALUE) {
                actual.onComplete();
            }
        }

        @Override
        public void request(long n) {
            if (SubscriptionHelper.validate(n)) {
                BackpressureHelper.addCancel(this, n);
            }
        }

        @Override
        public void cancel() {
            if (getAndSet(Long.MIN_VALUE) != Long.MIN_VALUE) {
                parent.remove(this);
            }
        }

        public boolean isCancelled() {
            return get() == Long.MIN_VALUE;
        }

        boolean isFull() {
            return get() == 0L;
        }
    }
}
