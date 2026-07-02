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

import io.reactivex.rxjava4.annotations.NonNull;
import io.reactivex.rxjava4.disposables.DisposableContainer;

/**
 * Interface to map/wrap an upstream {@link Streamer} to an downstream {@code Streamer}.
 *
 * @param <T> the value type of the upstream
 * @param <R> the value type of the downstream
 * @since 4.0.0
 */
@FunctionalInterface
public interface StreamableOperator<@NonNull T, @NonNull R> {
    /**
     * Applies a function to the upstream {@link Streamer} and returns a new downstream {@code Streamer}.
     * @param container the {@link DisposableContainer} handling the cancellation propagation for the downstream
     * @param streamer the upstream {@code Streamer} instance
     * @return the downstream {@code Streamer} instance
     * @throws Throwable on failure
     */
    @NonNull
    Streamer<? extends R> apply(@NonNull DisposableContainer container,
            @NonNull Streamer<? extends T> streamer) throws Throwable;
}
