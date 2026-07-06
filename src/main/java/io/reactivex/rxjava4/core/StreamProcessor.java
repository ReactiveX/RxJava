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

import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow.Processor;

import io.reactivex.rxjava4.annotations.*;

/**
 * A {@link Processor}-like interface combining the {@code Streamable} interface and the
 * {@link StreamerInput} interface to establish a push-pull bridge based on {@link CompletionStage}-based
 * asynchronous processing and dispatching of values and errors.
 * @param <In> the element type of the input side
 * @param <Out> the element type of the output side
 * @since 4.0.0
 */
public interface StreamProcessor<@NonNull In, @NonNull Out> extends Streamable<Out>, StreamerInput<In> {

    /**
     * Returns {@code true} if this {@link StreamProcessor} has {@link Streamer}s.
     * @return {@code true} if this {@link StreamProcessor} has {@link Streamer}s.
     */
    boolean hasStreamers();

    /**
     * Returns {@code true} if this {@code StreamProcessor} was completed normally via {@link #finish(Throwable)}.
     * @return {@code true} if this {@code StreamProcessor} was completed normally via {@link #finish(Throwable)}.
     */
    boolean hasComplete();

    /**
     * Returns {@code true} if this {@code StreamProcessor} was completed with a {@link Throwable} via {@link #finish(Throwable)}.
     * @return {@code true} if this {@code StreamProcessor} was completed with a {@link Throwable} via {@link #finish(Throwable)}.
     */
    boolean hasThrowable();

    /**
     * Returns the terminal {@link Throwable} if this {@code StreamProcessor} was completed
     * with a {@code Throwable} via {@link #finish(Throwable)}.
     * @return the {@link Throwable} if any
     */
    @Nullable
    Throwable getThrowable();
}
