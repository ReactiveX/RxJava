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

package io.reactivex.rxjava4.internal.util;

import java.util.concurrent.*;
import java.util.function.Function;

import io.reactivex.rxjava4.annotations.*;
import io.reactivex.rxjava4.disposables.DisposableContainer;

/**
 * Static methods to coordinate {@link CompletionStage}s for various operators.
 */
public interface AwaitCoordinator {

    /**
     * The {@code await} keyword for async/await.
     * @param <T> the type of the returned value if any.
     * @param stage the stage to await virtual-blockingly
     * @return the awaited value
     */
    @Nullable
    default <T> T await(@NonNull CompletionStage<T> stage) {
        return AwaitCoordinatorStatic.await(stage, null);
    }

    /**
     * The cancellable {@code await} keyword for async/await.
     * @param <T> the type of the returned value if any.
     * @param stage the stage to await virtual-blockingly
     * @param canceller the container that can trigger a cancellation on demand
     * @return the awaited value
     */
    @Nullable
    default <T> T await(@NonNull CompletionStage<T> stage, @Nullable DisposableContainer canceller) {
        return AwaitCoordinatorStatic.await(stage, canceller);
    }

    /**
     * Runs a function while turning it into a CompletionStage with a canceller supplied too.
     * @param <U> the return type of the function
     * @param function the function to apply
     * @param canceller the canceller to use
     * @param executor the executor to use
     * @return the new stage
     */
    default <U> CompletionStage<U> runStage(Function<DisposableContainer, U> function,
            DisposableContainer canceller, Executor executor) {
        return AwaitCoordinatorStatic.<U>runStage(function, canceller, executor);
    }

    /**
     * Runs a function while turning it into a CompletionStage with a canceller supplied too.
     * @param <U> the return type of the function
     * @param function the function to apply
     * @param canceller the canceller to use
     * @return the new stage
     */
    default <U> CompletionStage<U> runStage(Function<DisposableContainer, U> function,
            DisposableContainer canceller) {
        return runStage(function, canceller, Executors.newVirtualThreadPerTaskExecutor());
    }
}
