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

import java.util.concurrent.*;

import io.reactivex.rxjava4.annotations.NonNull;

/// A realized stream which can then be consumed asynchronously in steps.
/// Think of it as the `IAsyncEnumerator` from C# ported to the clumsy Java world.
///
/// The `Streamer` methods must be invoked sequentially and non-overlappingly, similar to the
/// <a href='https://github.com/reactive-streams/reactive-streams-jvm#1.3'>Reactive Streams rule §1.3</a>.
///
/// For an optimized synchronous operation, please consider using the {@link #NEXT_TRUE}, {@link #NEXT_FALSE}
/// and {@link #FINISHED} constant CompletionStages.
/// @param <T> the element type.
/// @since 4.0.0
public interface Streamer<@NonNull T> {

    // oooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooo
    // API
    // oooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooo

    /**
     * Determine if there are more elements available from the source.
     * @return a `CompletionStage` with 3 outcomes
     * <ul>
     * <li>`true` indicates there is an item available for consumption via {@link #current()}
     * <li>`false` indicates there are no more items available
     *< li>`Throwable` indicates there was an upstream error
     * </ul>
     */
    @NonNull
    CompletionStage<Boolean> next();

    /**
     * Returns the currently available item synchronously if the previous call to [#next()] yielded `true`.
     * Calling it during an ongoing [#next()] or [#finish()] call, or beyond the lifecycle of the `Streamer`
     * is an undefined behavior. It may yield `null` or throw.
     * @return the current item
     */
    @NonNull
    T current();

    /**
     * Finish the sequence once all processing has been done to it either via exhaustion or via cancellation.
     * <p>
     * Usually involves resource cleanup, so this method must be always called.
     * <p>
     * If the cleanup crashes and the [#next()] crashed too, the cleanup `Throwable` will be added as suppressed
     * to the main crash `Throwable` from `next`.
     *
     * @return a `CompletionStage` that completes when the resource cleanup completes normally or exceptionally
     */
    @NonNull
    CompletionStage<Void> finish();

    // oooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooo
    // HELPERS
    // oooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooo

    /**
     * Convenience method to blockingly await the CompletionStage returned by the {@link #next()} method.
     * @return true if there are more items, false if no more items are coming, or crashes
     */
    default boolean awaitNext() {
        var s = next();
        if (s == NEXT_TRUE) {
            return true;
        } else
        if (s == NEXT_FALSE) {
            return false;
        }
        return s.toCompletableFuture().join();
    }

    /**
     * Convenience method to blockingly await the CompletionStage returned by the {@link #finish()} method.
     */
    default void awaitFinish() {
        var s = finish();
        if (s == FINISHED) {
            return;
        }
        s.toCompletableFuture().join();
    }

    /**
     * Use this constant in {@link #next()} to indicate
     * the next value is available, synchronously.
     */
    CompletionStage<Boolean> NEXT_TRUE = CompletableFuture.completedStage(true);

    /**
     * Use this constant in {@link #next()} to indicate
     * no more values will be available, synchronously.
     */
    CompletionStage<Boolean> NEXT_FALSE = CompletableFuture.completedStage(false);

    /**
     * Use this constant in {@link #finish()} to indicate
     * the cleanup was done synchronously.
     */
    CompletionStage<Void> FINISHED = CompletableFuture.completedStage(null);
}
