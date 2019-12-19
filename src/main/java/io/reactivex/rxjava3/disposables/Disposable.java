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
package io.reactivex.rxjava3.disposables;

import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.functions.Action;
import io.reactivex.rxjava3.internal.disposables.EmptyDisposable;
import io.reactivex.rxjava3.internal.functions.Functions;
import org.reactivestreams.Subscription;

import java.util.Objects;
import java.util.concurrent.Future;

/**
 * Represents a disposable resource.
 */
public interface Disposable {
    /**
     * Dispose the resource, the operation should be idempotent.
     */
    void dispose();

    /**
     * Returns true if this resource has been disposed.
     * @return true if this resource has been disposed
     */
    boolean isDisposed();

    /**
     * Construct a {@code Disposable} by wrapping a {@link Runnable} that is
     * executed exactly once when the {@code Disposable} is disposed.
     * @param run the Runnable to wrap
     * @return the new Disposable instance
     * @since 3.0.0
     */
    @NonNull
    static Disposable fromRunnable(@NonNull Runnable run) {
        Objects.requireNonNull(run, "run is null");
        return new RunnableDisposable(run);
    }

    /**
     * Construct a {@code Disposable} by wrapping a {@link Action} that is
     * executed exactly once when the {@code Disposable} is disposed.
     * @param run the Action to wrap
     * @return the new Disposable instance
     * @since 3.0.0
     */
    @NonNull
    static Disposable fromAction(@NonNull Action run) {
        Objects.requireNonNull(run, "run is null");
        return new ActionDisposable(run);
    }

    /**
     * Construct a {@code Disposable} by wrapping a {@link Future} that is
     * cancelled exactly once when the {@code Disposable} is disposed.
     * <p>
     * The {@code Future} is cancelled with {@code mayInterruptIfRunning == true}.
     * @param future the Future to wrap
     * @return the new Disposable instance
     * @see #fromFuture(Future, boolean)
     * @since 3.0.0
     */
    @NonNull
    static Disposable fromFuture(@NonNull Future<?> future) {
        Objects.requireNonNull(future, "future is null");
        return fromFuture(future, true);
    }

    /**
     * Construct a {@code Disposable} by wrapping a {@link Future} that is
     * cancelled exactly once when the {@code Disposable} is disposed.
     * @param future the Future to wrap
     * @param allowInterrupt if true, the future cancel happens via {@code Future.cancel(true)}
     * @return the new Disposable instance
     * @since 3.0.0
     */
    @NonNull
    static Disposable fromFuture(@NonNull Future<?> future, boolean allowInterrupt) {
        Objects.requireNonNull(future, "future is null");
        return new FutureDisposable(future, allowInterrupt);
    }

    /**
     * Construct a {@code Disposable} by wrapping a {@link Subscription} that is
     * cancelled exactly once when the {@code Disposable} is disposed.
     * @param subscription the Runnable to wrap
     * @return the new Disposable instance
     * @since 3.0.0
     */
    @NonNull
    static Disposable fromSubscription(@NonNull Subscription subscription) {
        Objects.requireNonNull(subscription, "subscription is null");
        return new SubscriptionDisposable(subscription);
    }

    /**
     * Construct a {@code Disposable} by wrapping an {@link AutoCloseable} that is
     * closed exactly once when the {@code Disposable} is disposed.
     * @param autoCloseable the AutoCloseable to wrap
     * @return the new Disposable instance
     * @since 3.0.0
     */
    @NonNull
    static Disposable fromAutoCloseable(@NonNull AutoCloseable autoCloseable) {
        Objects.requireNonNull(autoCloseable, "autoCloseable is null");
        return new AutoCloseableDisposable(autoCloseable);
    }

    /**
     * Construct an {@link AutoCloseable} by wrapping a {@code Disposable} that is
     * disposed when the returned {@code AutoCloseable} is closed.
     * @param disposable the Disposable instance
     * @return the new AutoCloseable instance
     * @since 3.0.0
     */
    @NonNull
    static AutoCloseable toAutoCloseable(@NonNull Disposable disposable) {
        return disposable::dispose;
    }

    /**
     * Returns a new, non-disposed {@code Disposable} instance.
     * @return a new, non-disposed {@code Disposable} instance
     * @since 3.0.0
     */
    @NonNull
    static Disposable empty() {
        return fromRunnable(Functions.EMPTY_RUNNABLE);
    }

    /**
     * Returns a shared, disposed {@code Disposable} instance.
     * @return a shared, disposed {@code Disposable} instance
     * @since 3.0.0
     */
    @NonNull
    static Disposable disposed() {
        return EmptyDisposable.INSTANCE;
    }
}
