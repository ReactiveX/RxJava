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

import io.reactivex.rxjava4.annotations.NonNull;

/**
 * A realized stream which can then be consumed asynchronously in steps.
 * Think of it as the {@IAsyncEnumerator} of the Java world. Runs best on Virtual Threads.
 * @param <T> the element type.
 * TODO proper docs
 * @since 4.0.0
 */
public interface Streamer<@NonNull T> {

    /**
     * Determine if there are more elements available from the source.
     * @return eventually true or false, indicating availability or termination
     */
    @NonNull
    CompletionStage<Boolean> next();

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
     * @return the stage you can await to cleanups to happen
     */
    @NonNull
    CompletionStage<Void> close();
}
