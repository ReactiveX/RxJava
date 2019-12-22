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
package io.reactivex.rxjava3.testsupport;

import java.util.concurrent.atomic.*;

import org.reactivestreams.*;

import io.reactivex.rxjava3.core.FlowableSubscriber;
import io.reactivex.rxjava3.internal.fuseable.*;
import io.reactivex.rxjava3.internal.subscriptions.SubscriptionHelper;

/**
 * An extended test subscriber that records events and allows making assertions about them.
 *
 * <p>You can override the onSubscribe, onNext, onError, onComplete, request and
 * cancel methods but not the others (this is by design).
 *
 * <p>The TestSubscriber implements Disposable for convenience where dispose calls cancel.
 *
 * <p>When calling the default request method, you are requesting on behalf of the
 * wrapped actual subscriber.
 *
 * @param <T> the value type
 */
public class TestSubscriberEx<T>
extends BaseTestConsumerEx<T, TestSubscriberEx<T>>
implements FlowableSubscriber<T>, Subscription {
    /** The actual subscriber to forward events to. */
    private final Subscriber<? super T> downstream;

    /** Makes sure the incoming Subscriptions get cancelled immediately. */
    private volatile boolean cancelled;

    /** Holds the current subscription if any. */
    private final AtomicReference<Subscription> upstream;

    /** Holds the requested amount until a subscription arrives. */
    private final AtomicLong missedRequested;

    private QueueSubscription<T> qs;

    /**
     * Constructs a non-forwarding TestSubscriber with an initial request value of {@link Long#MAX_VALUE}.
     */
    public TestSubscriberEx() {
        this(EmptySubscriber.INSTANCE, Long.MAX_VALUE);
    }

    /**
     * Constructs a non-forwarding TestSubscriber with the specified initial request value.
     * <p>The TestSubscriber doesn't validate the initialRequest value so one can
     * test sources with invalid values as well.
     * @param initialRequest the initial request value
     */
    public TestSubscriberEx(long initialRequest) {
        this(EmptySubscriber.INSTANCE, initialRequest);
    }

    /**
     * Constructs a forwarding TestSubscriber but leaves the requesting to the wrapped subscriber.
     * @param downstream the actual Subscriber to forward events to
     */
    public TestSubscriberEx(Subscriber<? super T> downstream) {
        this(downstream, Long.MAX_VALUE);
    }

    /**
     * Constructs a forwarding TestSubscriber with the specified initial request value.
     * <p>The TestSubscriber doesn't validate the initialRequest value so one can
     * test sources with invalid values as well.
     * @param actual the actual Subscriber to forward events to
     * @param initialRequest the initial request value
     */
    public TestSubscriberEx(Subscriber<? super T> actual, long initialRequest) {
        super();
        if (initialRequest < 0) {
            throw new IllegalArgumentException("Negative initial request not allowed");
        }
        this.downstream = actual;
        this.upstream = new AtomicReference<>();
        this.missedRequested = new AtomicLong(initialRequest);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onSubscribe(Subscription s) {
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

        if (initialFusionMode != 0) {
            if (s instanceof QueueSubscription) {
                qs = (QueueSubscription<T>)s;

                int m = qs.requestFusion(initialFusionMode);
                establishedFusionMode = m;

                if (m == QueueFuseable.SYNC) {
                    checkSubscriptionOnce = true;
                    lastThread = Thread.currentThread();
                    try {
                        T t;
                        while ((t = qs.poll()) != null) {
                            values.add(t);
                        }
                        completions++;
                    } catch (Throwable ex) {
                        // Exceptions.throwIfFatal(e); TODO add fatal exceptions?
                        errors.add(ex);
                    }
                    return;
                }
            }
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
    public void onNext(T t) {
        if (!checkSubscriptionOnce) {
            checkSubscriptionOnce = true;
            if (upstream.get() == null) {
                errors.add(new IllegalStateException("onSubscribe not called in proper order"));
            }
        }
        lastThread = Thread.currentThread();

        if (establishedFusionMode == QueueFuseable.ASYNC) {
            try {
                while ((t = qs.poll()) != null) {
                    values.add(t);
                }
            } catch (Throwable ex) {
                // Exceptions.throwIfFatal(e); TODO add fatal exceptions?
                errors.add(ex);
                qs.cancel();
            }
            return;
        }

        values.add(t);

        if (t == null) {
            errors.add(new NullPointerException("onNext received a null value"));
        }

        downstream.onNext(t);
    }

    @Override
    public void onError(Throwable t) {
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
     * Returns true if this TestSubscriber has been cancelled.
     * @return true if this TestSubscriber has been cancelled
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
     * Returns true if this TestSubscriber received a subscription.
     * @return true if this TestSubscriber received a subscription
     */
    public final boolean hasSubscription() {
        return upstream.get() != null;
    }

    // assertion methods

    /**
     * Assert that the onSubscribe method was called exactly once.
     * @return this
     */
    @Override
    public final TestSubscriberEx<T> assertSubscribed() {
        if (upstream.get() == null) {
            throw fail("Not subscribed!");
        }
        return this;
    }

    /**
     * Assert that the onSubscribe method hasn't been called at all.
     * @return this
     */
    public final TestSubscriberEx<T> assertNotSubscribed() {
        if (upstream.get() != null) {
            throw fail("Subscribed!");
        } else
        if (!errors.isEmpty()) {
            throw fail("Not subscribed but errors found");
        }
        return this;
    }

    /**
     * Sets the initial fusion mode if the upstream supports fusion.
     * <p>Package-private: avoid leaking the now internal fusion properties into the public API.
     * Use SubscriberFusion to work with such tests.
     * @param mode the mode to establish, see the {@link QueueSubscription} constants
     * @return this
     */
    public final TestSubscriberEx<T> setInitialFusionMode(int mode) {
        this.initialFusionMode = mode;
        return this;
    }

    /**
     * Asserts that the given fusion mode has been established
     * <p>Package-private: avoid leaking the now internal fusion properties into the public API.
     * Use SubscriberFusion to work with such tests.
     * @param mode the expected mode
     * @return this
     */
    public final TestSubscriberEx<T> assertFusionMode(int mode) {
        int m = establishedFusionMode;
        if (m != mode) {
            if (qs != null) {
                throw new AssertionError("Fusion mode different. Expected: " + fusionModeToString(mode)
                + ", actual: " + fusionModeToString(m));
            } else {
                throw fail("Upstream is not fuseable");
            }
        }
        return this;
    }

    /**
     * Assert that the upstream is a fuseable source.
     * <p>Package-private: avoid leaking the now internal fusion properties into the public API.
     * Use SubscriberFusion to work with such tests.
     * @return this
     */
    public final TestSubscriberEx<T> assertFuseable() {
        if (qs == null) {
            throw new AssertionError("Upstream is not fuseable.");
        }
        return this;
    }

    /**
     * Assert that the upstream is not a fuseable source.
     * <p>Package-private: avoid leaking the now internal fusion properties into the public API.
     * Use SubscriberFusion to work with such tests.
     * @return this
     */
    public final TestSubscriberEx<T> assertNotFuseable() {
        if (qs != null) {
            throw new AssertionError("Upstream is fuseable.");
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
    public final TestSubscriberEx<T> requestMore(long n) {
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
