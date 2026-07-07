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

package io.reactivex.rxjava4.internal.operators.streamable;

import java.util.Objects;
import java.util.concurrent.CompletionStage;

import io.reactivex.rxjava4.annotations.*;
import io.reactivex.rxjava4.core.StreamSink;
import io.reactivex.rxjava4.disposables.DisposableContainer;

/**
 * Wraps a {@link StreamSink} and uses the given {@link DisposableContainer} to be
 * returned via {@link #cancellation()}.
 * @param <T> the element type of the stream
 * @param downstream the {@code StreamSink} to relay events to
 * @param cancellation the {@code DisposableContainer} to be used for indicating cancellation
 * @since 4.0.0
 */
public record StreamSinkWithCancellation<@NonNull T>(
        @NonNull StreamSink<T> downstream, @NonNull DisposableContainer cancellation)
implements StreamSink<T> {

    public StreamSinkWithCancellation {
        Objects.requireNonNull(downstream, "downstream is null");
    }

    @Override
    @NonNull
    public CompletionStage<Boolean> next(@NonNull T item) {
        return downstream.next(item);
    }

    @Override
    @NonNull
    public CompletionStage<Void> finish(@Nullable Throwable throwable) {
        return downstream.finish(throwable);
    }
}
