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
package io.reactivex.rxjava3.observers;

import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.internal.disposables.DisposableHelper;

/**
 * An {@link Observer}, {@link MaybeObserver}, {@link SingleObserver} and
 * {@link CompletableObserver} composite that can record events from
 * {@link Observable}s, {@link Maybe}s, {@link Single}s and {@link Completable}s
 *  and allows making assertions about them.
 *
 * <p>You can override the {@link #onSubscribe(Disposable)}, {@link #onNext(Object)}, {@link #onError(Throwable)},
 * {@link #onComplete()} and {@link #onSuccess(Object)} methods but not the others (this is by design).
 *
 * <p>The {@code TestObserver} implements {@link Disposable} for convenience where dispose calls cancel.
 *
 * @param <T> the value type
 * @see io.reactivex.rxjava3.subscribers.TestSubscriber
 */
public class TestObserver<T>
extends BaseTestConsumer<T, TestObserver<T>>
implements Observer<T>, Disposable, MaybeObserver<T>, SingleObserver<T>, CompletableObserver {
    /** The actual observer to forward events to. */
    private final Observer<? super T> downstream;

    /** Holds the current subscription if any. */
    private final AtomicReference<Disposable> upstream = new AtomicReference<>();

    /**
     * Constructs a non-forwarding {@code TestObserver}.
     * @param <T> the value type received
     * @return the new {@code TestObserver} instance
     */
    @NonNull
    public static <T> TestObserver<T> create() {
        return new TestObserver<>();
    }

    /**
     * Constructs a forwarding {@code TestObserver}.
     * @param <T> the value type received
     * @param delegate the actual {@link Observer} to forward events to
     * @return the new {@code TestObserver} instance
     */
    @NonNull
    public static <T> TestObserver<T> create(@NonNull Observer<? super T> delegate) {
        return new TestObserver<>(delegate);
    }

    /**
     * Constructs a non-forwarding TestObserver.
     */
    public TestObserver() {
        this(EmptyObserver.INSTANCE);
    }

    /**
     * Constructs a forwarding {@code TestObserver}.
     * @param downstream the actual {@link Observer} to forward events to
     */
    public TestObserver(@NonNull Observer<? super T> downstream) {
        this.downstream = downstream;
    }

    @Override
    public void onSubscribe(@NonNull Disposable d) {
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

        downstream.onSubscribe(d);
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
     * Returns true if this {@code TestObserver} received a subscription.
     * @return true if this {@code TestObserver} received a subscription
     */
    public final boolean hasSubscription() {
        return upstream.get() != null;
    }

    /**
     * Assert that the {@link #onSubscribe(Disposable)} method was called exactly once.
     * @return this
     */
    @Override
    @NonNull
    protected final TestObserver<T> assertSubscribed() {
        if (upstream.get() == null) {
            throw fail("Not subscribed!");
        }
        return this;
    }

    @Override
    public void onSuccess(@NonNull T value) {
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
