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

import java.util.concurrent.*;

import io.reactivex.rxjava4.annotations.NonNull;
import io.reactivex.rxjava4.core.*;
import io.reactivex.rxjava4.disposables.*;

public record StreamableError<T>(@NonNull Throwable throwable) implements Streamable<T> {

    @Override
    public @NonNull Streamer<@NonNull T> stream(@NonNull DisposableContainer cancellation) {
        return createFailed(throwable);
    }

    public static <@NonNull T> Streamer<@NonNull T> createFailed(@NonNull Throwable throwable) {
        return new ErrorStreamer<>(throwable);
    }

    static final class ErrorStreamer<T> implements Streamer<T> {

        final CompletionStage<Boolean> throwable;

        ErrorStreamer(Throwable throwable) {
            this.throwable = CompletableFuture.failedFuture(throwable);
        }

        @Override
        public @NonNull CompletionStage<Boolean> next() {
            return throwable;
        }

        @Override
        public @NonNull T current() {
            throw new IllegalStateException("current cannot be called if next() did not result in a true CompletionStage");
        }

        @Override
        public @NonNull CompletionStage<Void> finish() {
            return FINISHED;
        }
    }
}
