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
package io.reactivex.observers;

import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.internal.disposables.DisposableHelper;
import io.reactivex.internal.fuseable.QueueDisposable;
import io.reactivex.internal.util.*;

/**
 * An Observer that records events and allows making assertions about them.
 *
 * <p>You can override the onSubscribe, onNext, onError, onComplete, onSuccess and
 * cancel methods but not the others (this is by design).
 *
 * <p>The TestObserver implements Disposable for convenience where dispose calls cancel.
 *
 * @param <T> the value type
 */
public class TestObserver<T>
extends BaseTestConsumer<T, TestObserver<T>>
implements Observer<T>, Disposable, MaybeObserver<T>, SingleObserver<T>, CompletableObserver {
    /** The actual observer to forward events to. */
    private final Observer<? super T> actual;

    /** Holds the current subscription if any. */
    private final AtomicReference<Disposable> subscription = new AtomicReference<Disposable>();

    private QueueDisposable<T> qs;

    /**
     * Constructs a non-forwarding TestObserver.
     * @param <T> the value type received
     * @return the new TestObserver instance
     */
    public static <T> TestObserver<T> create() {
        return new TestObserver<T>();
    }

    /**
     * Constructs a forwarding TestObserver.
     * @param <T> the value type received
     * @param delegate the actual Observer to forward events to
     * @return the new TestObserver instance
     */
    public static <T> TestObserver<T> create(Observer<? super T> delegate) {
        return new TestObserver<T>(delegate);
    }

    /**
     * Constructs a non-forwarding TestObserver.
     */
    public TestObserver() {
        this(EmptyObserver.INSTANCE);
    }

    /**
     * Constructs a forwarding TestObserver.
     * @param actual the actual Observer to forward events to
     */
    public TestObserver(Observer<? super T> actual) {
        this.actual = actual;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onSubscribe(Disposable s) {
        lastThread = Thread.currentThread();

        if (s == null) {
            errors.add(new NullPointerException("onSubscribe received a null Subscription"));
            return;
        }
        if (!subscription.compareAndSet(null, s)) {
            s.dispose();
            if (subscription.get() != DisposableHelper.DISPOSED) {
                errors.add(new IllegalStateException("onSubscribe received multiple subscriptions: " + s));
            }
            return;
        }

        if (initialFusionMode != 0) {
            if (s instanceof QueueDisposable) {
                qs = (QueueDisposable<T>)s;

                int m = qs.requestFusion(initialFusionMode);
                establishedFusionMode = m;

                if (m == QueueDisposable.SYNC) {
                    checkSubscriptionOnce = true;
                    lastThread = Thread.currentThread();
                    try {
                        T t;
                        while ((t = qs.poll()) != null) {
                            values.add(t);
                        }
                        completions++;

                        subscription.lazySet(DisposableHelper.DISPOSED);
                    } catch (Throwable ex) {
                        // Exceptions.throwIfFatal(e); TODO add fatal exceptions?
                        errors.add(ex);
                    }
                    return;
                }
            }
        }

        actual.onSubscribe(s);
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

        if (establishedFusionMode == QueueDisposable.ASYNC) {
            try {
                while ((t = qs.poll()) != null) {
                    values.add(t);
                }
            } catch (Throwable ex) {
                // Exceptions.throwIfFatal(e); TODO add fatal exceptions?
                errors.add(ex);
                qs.dispose();
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

    /**
     * Returns true if this TestObserver has been cancelled.
     * @return true if this TestObserver has been cancelled
     */
    public final boolean isCancelled() {
        return isDisposed();
    }

    /**
     * Cancels the TestObserver (before or after the subscription happened).
     * <p>This operation is thread-safe.
     * <p>This method is provided as a convenience when converting Flowable tests that cancel.
     */
    public final void cancel() {
        dispose();
    }

    @Override
    public final void dispose() {
        DisposableHelper.dispose(subscription);
    }

    @Override
    public final boolean isDisposed() {
        return DisposableHelper.isDisposed(subscription.get());
    }

    // state retrieval methods
    /**
     * Returns true if this TestObserver received a subscription.
     * @return true if this TestObserver received a subscription
     */
    public final boolean hasSubscription() {
        return subscription.get() != null;
    }

    /**
     * Assert that the onSubscribe method was called exactly once.
     * @return this;
     */
    @Override
    public final TestObserver<T> assertSubscribed() {
        if (subscription.get() == null) {
            throw fail("Not subscribed!");
        }
        return this;
    }

    /**
     * Assert that the onSubscribe method hasn't been called at all.
     * @return this;
     */
    @Override
    public final TestObserver<T> assertNotSubscribed() {
        if (subscription.get() != null) {
            throw fail("Subscribed!");
        } else
        if (!errors.isEmpty()) {
            throw fail("Not subscribed but errors found");
        }
        return this;
    }

    /**
     * Run a check consumer with this TestObserver instance.
     * @param check the check consumer to run
     * @return this
     */
    public final TestObserver<T> assertOf(Consumer<? super TestObserver<T>> check) {
        try {
            check.accept(this);
        } catch (Throwable ex) {
            throw ExceptionHelper.wrapOrThrow(ex);
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
    final TestObserver<T> setInitialFusionMode(int mode) {
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
    final TestObserver<T> assertFusionMode(int mode) {
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
        case QueueDisposable.NONE : return "NONE";
        case QueueDisposable.SYNC : return "SYNC";
        case QueueDisposable.ASYNC : return "ASYNC";
        default: return "Unknown(" + mode + ")";
        }
    }

    /**
     * Assert that the upstream is a fuseable source.
     * <p>Package-private: avoid leaking the now internal fusion properties into the public API.
     * Use ObserverFusion to work with such tests.
     * @return this
     */
    final TestObserver<T> assertFuseable() {
        if (qs == null) {
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
    final TestObserver<T> assertNotFuseable() {
        if (qs != null) {
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
