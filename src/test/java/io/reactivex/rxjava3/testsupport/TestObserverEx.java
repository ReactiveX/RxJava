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

import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.internal.disposables.DisposableHelper;
import io.reactivex.rxjava3.internal.fuseable.*;

/**
 * An  extended test Observer that records events and allows making assertions about them.
 *
 * <p>You can override the onSubscribe, onNext, onError, onComplete, onSuccess and
 * cancel methods but not the others (this is by design).
 *
 * <p>The TestObserver implements Disposable for convenience where dispose calls cancel.
 *
 * @param <T> the value type
 */
public class TestObserverEx<T>
extends BaseTestConsumerEx<T, TestObserverEx<T>>
implements Observer<T>, Disposable, MaybeObserver<T>, SingleObserver<T>, CompletableObserver {
    /** The actual observer to forward events to. */
    private final Observer<? super T> downstream;

    /** Holds the current subscription if any. */
    private final AtomicReference<Disposable> upstream = new AtomicReference<>();

    private QueueDisposable<T> qd;

    /**
     * Constructs a non-forwarding TestObserver.
     */
    public TestObserverEx() {
        this(EmptyObserver.INSTANCE);
    }

    /**
     * Constructs a forwarding TestObserver.
     * @param downstream the actual Observer to forward events to
     */
    public TestObserverEx(Observer<? super T> downstream) {
        this.downstream = downstream;
    }

    /**
     * Constructs a TestObserverEx with the given initial fusion mode.
     * @param fusionMode the fusion mode, see {@link QueueFuseable}
     */
    public TestObserverEx(int fusionMode) {
        this();
        setInitialFusionMode(fusionMode);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onSubscribe(Disposable d) {
        lastThread = Thread.currentThread();

        if (d == null) {
            errors.add(new NullPointerException("onSubscribe received a null Subscription"));
            return;
        }
        if (!upstream.compareAndSet(null, d)) {
            d.dispose();
            if (upstream.get() != DisposableHelper.DISPOSED) {
                errors.add(new IllegalStateException("onSubscribe received multiple subscriptions: " + d));
            }
            return;
        }

        if (initialFusionMode != 0) {
            if (d instanceof QueueDisposable) {
                qd = (QueueDisposable<T>)d;

                int m = qd.requestFusion(initialFusionMode);
                establishedFusionMode = m;

                if (m == QueueFuseable.SYNC) {
                    checkSubscriptionOnce = true;
                    lastThread = Thread.currentThread();
                    try {
                        T t;
                        while ((t = qd.poll()) != null) {
                            values.add(t);
                        }
                        completions++;

                        upstream.lazySet(DisposableHelper.DISPOSED);
                    } catch (Throwable ex) {
                        errors.add(ex);
                    }
                    return;
                }
            }
        }

        downstream.onSubscribe(d);
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
                while ((t = qd.poll()) != null) {
                    values.add(t);
                }
            } catch (Throwable ex) {
                errors.add(ex);
                qd.dispose();
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
                errors.add(new IllegalStateException("onSubscribe not called in proper order"));
            }
        }

        try {
            lastThread = Thread.currentThread();
            if (t == null) {
                errors.add(new NullPointerException("onError received a null Throwable"));
            } else {
                errors.add(t);
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
    public final void dispose() {
        DisposableHelper.dispose(upstream);
    }

    @Override
    public final boolean isDisposed() {
        return DisposableHelper.isDisposed(upstream.get());
    }

    // state retrieval methods
    /**
     * Returns true if this TestObserver received a subscription.
     * @return true if this TestObserver received a subscription
     */
    public final boolean hasSubscription() {
        return upstream.get() != null;
    }

    /**
     * Assert that the onSubscribe method was called exactly once.
     * @return this;
     */
    @Override
    public final TestObserverEx<T> assertSubscribed() {
        if (upstream.get() == null) {
            throw fail("Not subscribed!");
        }
        return this;
    }

    /**
     * Assert that the onSubscribe method hasn't been called at all.
     * @return this;
     */
    public final TestObserverEx<T> assertNotSubscribed() {
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
     * Use ObserverFusion to work with such tests.
     * @param mode the mode to establish, see the {@link QueueDisposable} constants
     * @return this
     */
    public final TestObserverEx<T> setInitialFusionMode(int mode) {
        this.initialFusionMode = mode;
        return this;
    }

    /**
     * Asserts that the given fusion mode has been established
     * <p>Package-private: avoid leaking the now internal fusion properties into the public API.
     * Use ObserverFusion to work with such tests.
     * @param mode the expected mode
     * @return this
     */
    public final TestObserverEx<T> assertFusionMode(int mode) {
        int m = establishedFusionMode;
        if (m != mode) {
            if (qd != null) {
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
     * Use ObserverFusion to work with such tests.
     * @return this
     */
    public final TestObserverEx<T> assertFuseable() {
        if (qd == null) {
            throw new AssertionError("Upstream is not fuseable.");
        }
        return this;
    }

    /**
     * Assert that the upstream is not a fuseable source.
     * <p>Package-private: avoid leaking the now internal fusion properties into the public API.
     * Use ObserverFusion to work with such tests.
     * @return this
     */
    public final TestObserverEx<T> assertNotFuseable() {
        if (qd != null) {
            throw new AssertionError("Upstream is fuseable.");
        }
        return this;
    }

    @Override
    public void onSuccess(T value) {
        onNext(value);
        onComplete();
    }

    /**
     * An observer that ignores all events and does not report errors.
     */
    enum EmptyObserver implements Observer<Object> {
        INSTANCE;

        @Override
        public void onSubscribe(Disposable d) {
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
