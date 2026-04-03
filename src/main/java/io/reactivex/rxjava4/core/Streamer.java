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

import java.util.NoSuchElementException;
import java.util.concurrent.CompletionStage;

import io.reactivex.rxjava4.annotations.*;
import io.reactivex.rxjava4.disposables.*;

/**
 * A realized stream which can then be consumed asynchronously in steps.
 * Think of it as the {@IAsyncEnumerator} of the Java world. Runs best on Virtual Threads.
 * @param <T> the element type.
 * TODO proper docs
 * @since 4.0.0
 */
public interface Streamer<@NonNull T> extends AutoCloseable {

    /**
     * Determine if there are more elements available from the source.
     * @param cancellation ability to perform cancellation on a per-virtual-pull request.
     * @return eventually true or false, indicating availability or termination
     */
    @NonNull
    CompletionStage<Boolean> next(@NonNull DisposableContainer cancellation);

    /**
     * Determine if there are more elements available from the source.
     * Uses a default, individual {@link CompositeDisposable} to manage cancellation.
     * @return eventually true or false, indicating availability or termination
     */
    @NonNull
    default CompletionStage<Boolean> next() {
        return next(new CompositeDisposable());
    }

    /**
     * Returns the current element if {@link #next()} yielded {@code true}.
     * Can be called multiple times between {@link #next()} calls.
     * @return the current element
     * @throws NoSuchElementException before the very first {@link #next()} or after {@link #next()} returned {@code false}
     */
    @NonNull
    T current();

    /**
     * Called when the stream ends or gets cancelled. Should be always invoked.
     * TODO, this is inherited from {@code IAsyncDisposable} in C#...
     * @return the stage you can await to cleanups to happen
     */
    @NonNull
    CompletionStage<Void> cancel();

    /**
     * Make this Streamer a resource and a Closeable, allowing virtually blocking closing.
     */
    default void close() {
        cancel().toCompletableFuture().join();
    }

    /**
     * The {@code await} keyword for async/await.
     * @param <T> the type of the returned value if any.
     * @param stage the stage to await virtual-blockingly
     * @return the awaited value
     */
    @Nullable
    static <T> T await(@NonNull CompletionStage<T> stage) {
        return await(stage, null);
    }

    /**
     * The cancellable {@code await} keyword for async/await.
     * @param <T> the type of the returned value if any.
     * @param stage the stage to await virtual-blockingly
     * @param cancellation the container that can trigger a cancellation on demand
     * @return the awaited value
     */
    @Nullable
    static <T> T await(@NonNull CompletionStage<T> stage, @Nullable DisposableContainer cancellation) {
        var f = stage.toCompletableFuture();
        if (cancellation == null) {
            return f.join();
        }
        var d = Disposable.fromFuture(f, true);
        try (var _ = cancellation.subscribe(d)) {
            return f.join();
        }
    }
}
