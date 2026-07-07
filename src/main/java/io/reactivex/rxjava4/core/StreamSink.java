/*
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

package io.reactivex.rxjava4.core;

import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.Flow.Subscriber;

import io.reactivex.rxjava4.annotations.*;
import io.reactivex.rxjava4.disposables.*;
import io.reactivex.rxjava4.functions.Function;
import io.reactivex.rxjava4.internal.operators.streamable.*;

/**
 * An interface to submit items and terminal events to a consumer that indacates when the processing of
 * said item or terminal event has completed, similar to how {@link Subscriber} can receive events.
 * <p>
 * The general contract is to call {@link #next(Object)} zero or more times, then
 * call {@link #finish(Throwable)} at most once, all in a non-overlapping fashion and only if the
 * returned {@link CompletionStage} has completed in some fashion.
 * @param <T> the item type to be offered
 * @since 4.0.0
 */
public interface StreamSink<@NonNull T> {

    /**
     * Offer the next item.
     * @param item the item being offered
     * @return a {@link CompletionStage} that completes with {@code true} if the value was successfully consumed,
     *         {@code false} if the value was rejected or exceptionally on error
     */
    @NonNull
    CompletionStage<Boolean> next(T item);

    /**
     * Offer the final, terminal event.
     * @param throwable the optional throwable to signal error, {@code null} to signal normal completion
     * @return a {@link CompletionStage} that completes with {@code null} if the call succeeded
     *         or exceptionally on error
     */
    @NonNull
    CompletionStage<Void> finish(@Nullable Throwable throwable);

    /**
     * Returns the {@link DisposableContainer} to use to detect if the consumer has indicated no more
     * items it is willing to accept.
     * <p>
     * The default implementation returns a fresh {@link CompositeDisposable}.
     * @return the {@code DisposableContainer}
     */
    @NonNull
    default DisposableContainer cancellation() {
        return new CompositeDisposable();
    }

    /**
     * Returns a new {@link StreamSink} that returns the given {@link DisposableContainer}
     * in its {@link #cancellation()}, allowing overriding the cancellation management
     * of this {@code StreamSink}
     * @param cancellation the {@link DisposableContainer} to use as cancellation management
     * @return the new {@code StreamSink} instance
     * @throws NullPointerException if {@code cancellation} is {@code null}
     */
    @NonNull
    default StreamSink<T> withCancellation(DisposableContainer cancellation) {
        Objects.requireNonNull(cancellation, "cancellation is null");
        return new StreamSinkWithCancellation<>(this, cancellation);
    }

    /**
     * Creates a {@link StreamSink} via lambda callbacks for {@link #next(Object)} and
     * {@link #finish(Throwable)}.
     * <p>
     * Non-fatal exceptions thrown by the callbacks are turned into failed
     * {@link CompletableFuture#failedFuture(Throwable)}s.
     * @param <T> the element type of the stream
     * @param onNext the callback for the {@code next} method
     * @param onFinish the callback for the {@code finish} method
     * @return the new {@link StreamSink} instance
     */
    @NonNull
    static <T> StreamSink<T> create(
            @NonNull Function<? super T, ? extends CompletionStage<Boolean>> onNext,
            @NonNull Function<? super Throwable, ? extends CompletionStage<Void>> onFinish
    ) {
        Objects.requireNonNull(onNext, "onNext is null");
        Objects.requireNonNull(onFinish, "onFinish is null");
        return new StreamSinkLambda<>(onNext, onFinish);
    }
}
