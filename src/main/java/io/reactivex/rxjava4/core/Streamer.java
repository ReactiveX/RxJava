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

import java.util.*;
import java.util.concurrent.*;

import io.reactivex.rxjava4.annotations.NonNull;
import io.reactivex.rxjava4.disposables.*;
import io.reactivex.rxjava4.internal.util.AwaitCoordinator;

/**
 * A realized stream which can then be consumed asynchronously in steps.
 * Think of it as the {@IAsyncEnumerator} of the Java world. Runs best on Virtual Threads.
 * <p>
 * To make sure you can run finish, use {@link DisposableContainer#clear()} or {@link DisposableContainer#reset()}
 * to get rid of all previous registered disposables. finish() will create its own, and if that
 * gets stuck, just call clear()/dispose() on the container to get rid of this sequence for good.
 * @param <T> the element type.
 * TODO proper docs
 * @since 4.0.0
 */
public interface Streamer<@NonNull T> extends AwaitCoordinator {

    // oooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooo
    // API
    // oooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooo

    /**
     * Determine if there are more elements available from the source.
     * @param cancellation ability to perform cancellation on a per-virtual-pull request.
     * @return eventually true or false, indicating availability or termination
     */
    @NonNull
    CompletionStage<Boolean> next(@NonNull DisposableContainer cancellation);

    /**
     * Returns the current element if {@link #next(DisposableContainer)} yielded {@code true}.
     * Can be called multiple times between {@link #next(DisposableContainer)} calls.
     * @return the current element
     * @throws NoSuchElementException before the very first {@link #next(DisposableContainer)}
     *  or after {@link #next(DisposableContainer)} returned {@code false}
     */
    @NonNull
    T current();

    /**
     * Called when the stream ends or gets cancelled. Should be always invoked.
     * TODO, this is inherited from {@code IAsyncDisposable} in C#...
     * @param cancellation to cancel a stuck finish operation, just in case.
     * @return the stage you can await to cleanups to happen
     */
    @NonNull
    CompletionStage<Void> finish(@NonNull DisposableContainer cancellation);

    // oooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooo
    // HELPERS
    // oooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooo

    /**
     * Moves and awaits the sequence's next element, returns false if there are no more
     * data.
     * @param cancellation to efficiently cancel this await if necessary
     * @return true if the next element via {@link #current()} can be read, or false if
     * the stream ended.
     */
    default boolean awaitNext(@NonNull DisposableContainer cancellation) {
        return await(next(cancellation), cancellation);
    }

    /**
     * Who cancels the cancellation attempt? Another cancellation attempt!
     * @param cancellation the token to cancel and ongoing cancel attempt
     */
    default void awaitFinish(@NonNull DisposableContainer cancellation) {
        await(finish(cancellation), cancellation);
    }

    /**
     * Use this constant in {@link #next(DisposableContainer)} to indicate
     * the next value is available, synchronously.
     */
    CompletionStage<Boolean> NEXT_TRUE = CompletableFuture.completedStage(true);

    /**
     * Use this constant in {@link #next(DisposableContainer)} to indicate
     * no more values will be available, synchronously.
     */
    CompletionStage<Boolean> NEXT_FALSE = CompletableFuture.completedStage(false);

    /**
     * Use this constant in {@link #finish(DisposableContainer)} to indicate
     * the cleanup was done synchronously.
     */
    CompletionStage<Void> FINISHED = CompletableFuture.completedStage(null);
}
