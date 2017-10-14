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

package io.reactivex.disposables;

import java.util.concurrent.Future;

import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Action;
import io.reactivex.internal.disposables.EmptyDisposable;
import io.reactivex.internal.functions.*;
import org.reactivestreams.Subscription;

/**
 * Utility class to help create disposables by wrapping
 * other types.
 * @since 2.0
 */
public final class Disposables {
    /** Utility class. */
    private Disposables() {
        throw new IllegalStateException("No instances!");
    }

    /**
     * Construct a Disposable by wrapping a Runnable that is
     * executed exactly once when the Disposable is disposed.
     * @param run the Runnable to wrap
     * @return the new Disposable instance
     */
    @NonNull
    public static Disposable fromRunnable(@NonNull Runnable run) {
        ObjectHelper.requireNonNull(run, "run is null");
        return new RunnableDisposable(run);
    }

    /**
     * Construct a Disposable by wrapping a Action that is
     * executed exactly once when the Disposable is disposed.
     * @param run the Action to wrap
     * @return the new Disposable instance
     */
    @NonNull
    public static Disposable fromAction(@NonNull Action run) {
        ObjectHelper.requireNonNull(run, "run is null");
        return new ActionDisposable(run);
    }

    /**
     * Construct a Disposable by wrapping a Future that is
     * cancelled exactly once when the Disposable is disposed.
     * @param future the Future to wrap
     * @return the new Disposable instance
     */
    @NonNull
    public static Disposable fromFuture(@NonNull Future<?> future) {
        ObjectHelper.requireNonNull(future, "future is null");
        return fromFuture(future, true);
    }

    /**
     * Construct a Disposable by wrapping a Future that is
     * cancelled exactly once when the Disposable is disposed.
     * @param future the Future to wrap
     * @param allowInterrupt if true, the future cancel happens via Future.cancel(true)
     * @return the new Disposable instance
     */
    @NonNull
    public static Disposable fromFuture(@NonNull Future<?> future, boolean allowInterrupt) {
        ObjectHelper.requireNonNull(future, "future is null");
        return new FutureDisposable(future, allowInterrupt);
    }

    /**
     * Construct a Disposable by wrapping a Subscription that is
     * cancelled exactly once when the Disposable is disposed.
     * @param subscription the Runnable to wrap
     * @return the new Disposable instance
     */
    @NonNull
    public static Disposable fromSubscription(@NonNull Subscription subscription) {
        ObjectHelper.requireNonNull(subscription, "subscription is null");
        return new SubscriptionDisposable(subscription);
    }

    /**
     * Returns a new, non-disposed Disposable instance.
     * @return a new, non-disposed Disposable instance
     */
    @NonNull
    public static Disposable empty() {
        return fromRunnable(Functions.EMPTY_RUNNABLE);
    }

    /**
     * Returns a disposed Disposable instance.
     * @return a disposed Disposable instance
     */
    @NonNull
    public static Disposable disposed() {
        return EmptyDisposable.INSTANCE;
    }
}
