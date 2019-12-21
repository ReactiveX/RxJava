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
package io.reactivex.rxjava3.subscribers;

import java.util.concurrent.atomic.*;

import org.reactivestreams.*;

import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.FlowableSubscriber;
import io.reactivex.rxjava3.internal.subscriptions.SubscriptionHelper;
import io.reactivex.rxjava3.observers.BaseTestConsumer;

/**
 * A {@link Subscriber} implementation that records events and allows making assertions about them.
 *
 * <p>You can override the {@link #onSubscribe(Subscription)}, {@link #onNext(Object)}, {@link #onError(Throwable)} and
 * {@link #onComplete()} methods but not the others (this is by design).
 *
 * <p>When calling the default request method, you are requesting on behalf of the
 * wrapped actual {@link Subscriber} if any.
 *
 * @param <T> the value type
 */
public class TestSubscriber<T>
extends BaseTestConsumer<T, TestSubscriber<T>>
implements FlowableSubscriber<T>, Subscription {
    /** The actual subscriber to forward events to. */
    private final Subscriber<? super T> downstream;

    /** Makes sure the incoming Subscriptions get cancelled immediately. */
    private volatile boolean cancelled;

    /** Holds the current subscription if any. */
    private final AtomicReference<Subscription> upstream;

    /** Holds the requested amount until a subscription arrives. */
    private final AtomicLong missedRequested;

    /**
     * Creates a {@code TestSubscriber} with {@link Long#MAX_VALUE} initial request amount.
     * @param <T> the value type
     * @return the new {@code TestSubscriber} instance.
     * @see #create(long)
     */
    @NonNull
    public static <T> TestSubscriber<T> create() {
        return new TestSubscriber<>();
    }

    /**
     * Creates a {@code TestSubscriber} with the given initial request amount.
     * @param <T> the value type
     * @param initialRequested the initial requested amount
     * @return the new {@code TestSubscriber} instance.
     */
    @NonNull
    public static <T> TestSubscriber<T> create(long initialRequested) {
        return new TestSubscriber<>(initialRequested);
    }

    /**
     * Constructs a forwarding {@code TestSubscriber}.
     * @param <T> the value type received
     * @param delegate the actual {@link Subscriber} to forward events to
     * @return the new TestObserver instance
     */
    public static <T> TestSubscriber<T> create(@NonNull Subscriber<? super T> delegate) {
        return new TestSubscriber<>(delegate);
    }

    /**
     * Constructs a non-forwarding {@code TestSubscriber} with an initial request value of {@link Long#MAX_VALUE}.
     */
    public TestSubscriber() {
        this(EmptySubscriber.INSTANCE, Long.MAX_VALUE);
    }

    /**
     * Constructs a non-forwarding {@code TestSubscriber} with the specified initial request value.
     * <p>The {@code TestSubscriber} doesn't validate the {@code initialRequest} amount so one can
     * test sources with invalid values as well.
     * @param initialRequest the initial request amount
     */
    public TestSubscriber(long initialRequest) {
        this(EmptySubscriber.INSTANCE, initialRequest);
    }

    /**
     * Constructs a forwarding {@code TestSubscriber} but leaves the requesting to the wrapped {@link Subscriber}.
     * @param downstream the actual {@code Subscriber} to forward events to
     */
    public TestSubscriber(@NonNull Subscriber<? super T> downstream) {
        this(downstream, Long.MAX_VALUE);
    }

    /**
     * Constructs a forwarding {@code TestSubscriber} with the specified initial request amount
     * and an actual {@link Subscriber} to forward events to.
     * <p>The {@code TestSubscriber} doesn't validate the initialRequest value so one can
     * test sources with invalid values as well.
     * @param actual the actual {@code Subscriber} to forward events to
     * @param initialRequest the initial request amount
     */
    public TestSubscriber(@NonNull Subscriber<? super T> actual, long initialRequest) {
        super();
        if (initialRequest < 0) {
            throw new IllegalArgumentException("Negative initial request not allowed");
        }
        this.downstream = actual;
        this.upstream = new AtomicReference<>();
        this.missedRequested = new AtomicLong(initialRequest);
    }

    @Override
    public void onSubscribe(@NonNull Subscription s) {
        lastThread = Thread.currentThread();

        if (s == null) {
            errors.add(new NullPointerException("onSubscribe received a null Subscription"));
            return;
        }
        if (!upstream.compareAndSet(null, s)) {
            s.cancel();
            if (upstream.get() != SubscriptionHelper.CANCELLED) {
                errors.add(new IllegalStateException("onSubscribe received multiple subscriptions: " + s));
            }
            return;
        }

        downstream.onSubscribe(s);

        long mr = missedRequested.getAndSet(0L);
        if (mr != 0L) {
            s.request(mr);
        }

        onStart();
    }

    /**
     * Called after the onSubscribe is called and handled.
     */
    protected void onStart() {

    }

    @Override
    public void onNext(@NonNull T t) {
        if (!checkSubscriptionOnce) {
            checkSubscriptionOnce = true;
            if (upstream.get() == null) {
                errors.add(new IllegalStateException("onSubscribe not called in proper order"));
            }
        }
        lastThread = Thread.currentThread();

        values.add(t);

        if (t == null) {
            errors.add(new NullPointerException("onNext received a null value"));
        }

        downstream.onNext(t);
    }

    @Override
    public void onError(@NonNull Throwable t) {
        if (!checkSubscriptionOnce) {
            checkSubscriptionOnce = true;
            if (upstream.get() == null) {
                errors.add(new NullPointerException("onSubscribe not called in proper order"));
            }
        }
        try {
            lastThread = Thread.currentThread();
            errors.add(t);

            if (t == null) {
                errors.add(new IllegalStateException("onError received a null Throwable"));
            }

            downstream.onError(t);
        } finally {
            done.countDown();
        }
    }

    @Override
    public void onComplete() {
        if (!checkSubscriptionOnce) {
            checkSubscriptionOnce = true;
            if (upstream.get() == null) {
                errors.add(new IllegalStateException("onSubscribe not called in proper order"));
            }
        }
        try {
            lastThread = Thread.currentThread();
            completions++;

            downstream.onComplete();
        } finally {
            done.countDown();
        }
    }

    @Override
    public final void request(long n) {
        SubscriptionHelper.deferredRequest(upstream, missedRequested, n);
    }

    @Override
    public final void cancel() {
        if (!cancelled) {
            cancelled = true;
            SubscriptionHelper.cancel(upstream);
        }
    }

    /**
     * Returns true if this {@code TestSubscriber} has been cancelled.
     * @return true if this {@code TestSubscriber} has been cancelled
     */
    public final boolean isCancelled() {
        return cancelled;
    }

    @Override
    protected final void dispose() {
        cancel();
    }

    @Override
    protected final boolean isDisposed() {
        return cancelled;
    }

    // state retrieval methods

    /**
     * Returns true if this {@code TestSubscriber} received a {@link Subscription} via {@link #onSubscribe(Subscription)}.
     * @return true if this {@code TestSubscriber} received a {@link Subscription} via {@link #onSubscribe(Subscription)}
     */
    public final boolean hasSubscription() {
        return upstream.get() != null;
    }

    // assertion methods

    /**
     * Assert that the {@link #onSubscribe(Subscription)} method was called exactly once.
     * @return this
     */
    @Override
    protected final TestSubscriber<T> assertSubscribed() {
        if (upstream.get() == null) {
            throw fail("Not subscribed!");
        }
        return this;
    }

    /**
     * Calls {@link #request(long)} and returns this.
     * <p>History: 2.0.1 - experimental
     * @param n the request amount
     * @return this
     * @since 2.1
     */
    public final TestSubscriber<T> requestMore(long n) {
        request(n);
        return this;
    }

    /**
     * A subscriber that ignores all events and does not report errors.
     */
    enum EmptySubscriber implements FlowableSubscriber<Object> {
        INSTANCE;

        @Override
        public void onSubscribe(Subscription s) {
        }

        @Override
        public void onNext(Object t) {
        }

        @Override
        public void onError(Throwable t) {
        }

        @Override
        public void onComplete() {
        }
    }
}
