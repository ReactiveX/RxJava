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

package io.reactivex.rxjava4.core.config;

import java.util.Objects;
import java.util.concurrent.CompletionStage;

import io.reactivex.rxjava4.annotations.NonNull;
import io.reactivex.rxjava4.core.*;
import io.reactivex.rxjava4.disposables.*;
import io.reactivex.rxjava4.functions.*;

/**
 * Configuration record the intercept() operator with various lifecylce-stage transforming callbacks
 * @param <T> the element type of the sequence
 * @param onStream called when the {@link Streamable#stream(StreamerCancellation)} is invoked
 * @param onNext called when the {@link Streamer#next()} is invoked
 * @param onCurrent called when the {@link Streamer#current()} is invoked
 * @param onFinish called when the {@link Streamer#finish()} is invoked
 * @since 4.0.0
 */
public record StreamableInterceptConfig<T>(
        @NonNull BiFunction<? super StreamerCancellation, ? super Streamer<? extends T>, ? extends Streamer<? extends T>> onStream,
        @NonNull BiFunction<? super StreamerCancellation, ? super CompletionStage<Boolean>, ? extends CompletionStage<Boolean>> onNext,
        @NonNull Function<? super T, ? extends T> onCurrent,
        @NonNull BiFunction<? super StreamerCancellation, ? super CompletionStage<Void>, ? extends CompletionStage<Void>> onFinish
) {

    /**
     * Constructs a configuration with a custom {@link #onNext()} intercept and everything else is pass-through.
     * @param onNext the callback for intercepting the {@code next()} calls
     */
    public StreamableInterceptConfig(
            @NonNull BiFunction<? super StreamerCancellation, ? super CompletionStage<Boolean>, ? extends CompletionStage<Boolean>> onNext) {
        this((_, v) -> v, onNext, v -> v, (_, v) -> v);
    }

    /**
     * Constructs a configuration with a custom {@link #onCurrent()} intercept and everything else is pass-through.
     * @param onCurrent the callback for when an item is ready
     */
    public StreamableInterceptConfig(@NonNull Function<? super T, ? extends T> onCurrent) {
        this((_, v) -> v, (_, v) -> v, onCurrent, (_, v) -> v);
    }

    /**
     * Constructs a fully configured record.
     * @param onStream called when the {@link Streamable#stream(StreamerCancellation)} is invoked
     * @param onNext called when the {@link Streamer#next()} is invoked
     * @param onCurrent called when the {@link Streamer#current()} is invoked
     * @param onFinish called when the {@link Streamer#finish()} is invoked
     */
    public StreamableInterceptConfig {
        Objects.requireNonNull(onStream, "onStream is null");
        Objects.requireNonNull(onNext, "onNext is null");
        Objects.requireNonNull(onCurrent, "onCurrent is null");
        Objects.requireNonNull(onFinish, "onFinish is null");
    }
}
