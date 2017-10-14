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
package io.reactivex.subscribers;

import java.util.concurrent.atomic.*;

import org.reactivestreams.*;

import io.reactivex.FlowableSubscriber;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.internal.fuseable.QueueSubscription;
import io.reactivex.internal.subscriptions.SubscriptionHelper;
import io.reactivex.internal.util.ExceptionHelper;
import io.reactivex.observers.BaseTestConsumer;

/**
 * A subscriber that records events and allows making assertions about them.
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
public class TestSubscriber<T>
extends BaseTestConsumer<T, TestSubscriber<T>>
implements FlowableSubscriber<T>, Subscription, Disposable {
    /** The actual subscriber to forward events to. */
    private final Subscriber<? super T> actual;

    /** Makes sure the incoming Subscriptions get cancelled immediately. */
    private volatile boolean cancelled;

    /** Holds the current subscription if any. */
    private final AtomicReference<Subscription> subscription;

    /** Holds the requested amount until a subscription arrives. */
    private final AtomicLong missedRequested;

    private QueueSubscription<T> qs;

    /**
     * Creates a TestSubscriber with Long.MAX_VALUE initial request.
     * @param <T> the value type
     * @return the new TestSubscriber instance.
     */
    public static <T> TestSubscriber<T> create() {
        return new TestSubscriber<T>();
    }

    /**
     * Creates a TestSubscriber with the given initial request.
     * @param <T> the value type
     * @param initialRequested the initial requested amount
     * @return the new TestSubscriber instance.
     */
    public static <T> TestSubscriber<T> create(long initialRequested) {
        return new TestSubscriber<T>(initialRequested);
    }

    /**
     * Constructs a forwarding TestSubscriber.
     * @param <T> the value type received
     * @param delegate the actual Subscriber to forward events to
     * @return the new TestObserver instance
     */
    public static <T> TestSubscriber<T> create(Subscriber<? super T> delegate) {
        return new TestSubscriber<T>(delegate);
    }

    /**
     * Constructs a non-forwarding TestSubscriber with an initial request value of Long.MAX_VALUE.
     */
    public TestSubscriber() {
        this(EmptySubscriber.INSTANCE, Long.MAX_VALUE);
    }

    /**
     * Constructs a non-forwarding TestSubscriber with the specified initial request value.
     * <p>The TestSubscriber doesn't validate the initialRequest value so one can
     * test sources with invalid values as well.
     * @param initialRequest the initial request value
     */
    public TestSubscriber(long initialRequest) {
        this(EmptySubscriber.INSTANCE, initialRequest);
    }

    /**
     * Constructs a forwarding TestSubscriber but leaves the requesting to the wrapped subscriber.
     * @param actual the actual Subscriber to forward events to
     */
    public TestSubscriber(Subscriber<? super T> actual) {
        this(actual, Long.MAX_VALUE);
    }

    /**
     * Constructs a forwarding TestSubscriber with the specified initial request value.
     * <p>The TestSubscriber doesn't validate the initialRequest value so one can
     * test sources with invalid values as well.
     * @param actual the actual Subscriber to forward events to
     * @param initialRequest the initial request value
     */
    public TestSubscriber(Subscriber<? super T> actual, long initialRequest) {
        super();
        if (initialRequest < 0) {
            throw new IllegalArgumentException("Negative initial request not allowed");
        }
        this.actual = actual;
        this.subscription = new AtomicReference<Subscription>();
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
        if (!subscription.compareAndSet(null, s)) {
            s.cancel();
            if (subscription.get() != SubscriptionHelper.CANCELLED) {
                errors.add(new IllegalStateException("onSubscribe received multiple subscriptions: " + s));
            }
            return;
        }

        if (initialFusionMode != 0) {
            if (s instanceof QueueSubscription) {
                qs = (QueueSubscription<T>)s;

                int m = qs.requestFusion(initialFusionMode);
                establishedFusionMode = m;

                if (m == QueueSubscription.SYNC) {
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


        actual.onSubscribe(s);

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
            if (subscription.get() == null) {
                errors.add(new IllegalStateException("onSubscribe not called in proper order"));
            }
        }
        lastThread = Thread.currentThread();

        if (establishedFusionMode == QueueSubscription.ASYNC) {
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

        actual.onNext(t);
    }

    @Override
    public void onError(Throwable t) {
        if (!checkSubscriptionOnce) {
            checkSubscriptionOnce = true;
            if (subscription.get() == null) {
                errors.add(new NullPointerException("onSubscribe not called in proper order"));
            }
        }
        try {
            lastThread = Thread.currentThread();
            errors.add(t);

            if (t == null) {
                errors.add(new IllegalStateException("onError received a null Throwable"));
            }

            actual.onError(t);
        } finally {
            done.countDown();
        }
    }

    @Override
    public void onComplete() {
        if (!checkSubscriptionOnce) {
            checkSubscriptionOnce = true;
            if (subscription.get() == null) {
                errors.add(new IllegalStateException("onSubscribe not called in proper order"));
            }
        }
        try {
            lastThread = Thread.currentThread();
            completions++;

            actual.onComplete();
        } finally {
            done.countDown();
        }
    }

    @Override
    public final void request(long n) {
        SubscriptionHelper.deferredRequest(subscription, missedRequested, n);
    }

    @Override
    public final void cancel() {
        if (!cancelled) {
            cancelled = true;
            SubscriptionHelper.cancel(subscription);
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
    public final void dispose() {
        cancel();
    }

    @Override
    public final boolean isDisposed() {
        return cancelled;
    }

    // state retrieval methods

    /**
     * Returns true if this TestSubscriber received a subscription.
     * @return true if this TestSubscriber received a subscription
     */
    public final boolean hasSubscription() {
        return subscription.get() != null;
    }

    // assertion methods

    /**
     * Assert that the onSubscribe method was called exactly once.
     * @return this
     */
    @Override
    public final TestSubscriber<T> assertSubscribed() {
        if (subscription.get() == null) {
            throw fail("Not subscribed!");
        }
        return this;
    }

    /**
     * Assert that the onSubscribe method hasn't been called at all.
     * @return this
     */
    @Override
    public final TestSubscriber<T> assertNotSubscribed() {
        if (subscription.get() != null) {
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
    final TestSubscriber<T> setInitialFusionMode(int mode) {
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
    final TestSubscriber<T> assertFusionMode(int mode) {
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

    static String fusionModeToString(int mode) {
        switch (mode) {
        case QueueSubscription.NONE : return "NONE";
        case QueueSubscription.SYNC : return "SYNC";
        case QueueSubscription.ASYNC : return "ASYNC";
        default: return "Unknown(" + mode + ")";
        }
    }

    /**
     * Assert that the upstream is a fuseable source.
     * <p>Package-private: avoid leaking the now internal fusion properties into the public API.
     * Use SubscriberFusion to work with such tests.
     * @return this
     */
    final TestSubscriber<T> assertFuseable() {
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
    final TestSubscriber<T> assertNotFuseable() {
        if (qs != null) {
            throw new AssertionError("Upstream is fuseable.");
        }
        return this;
    }

    /**
     * Run a check consumer with this TestSubscriber instance.
     * @param check the check consumer to run
     * @return this
     */
    public final TestSubscriber<T> assertOf(Consumer<? super TestSubscriber<T>> check) {
        try {
            check.accept(this);
        } catch (Throwable ex) {
            throw ExceptionHelper.wrapOrThrow(ex);
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
