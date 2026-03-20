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

import java.util.NoSuchElementException;
import java.util.concurrent.*;

import io.reactivex.rxjava4.annotations.NonNull;
import io.reactivex.rxjava4.core.*;
import io.reactivex.rxjava4.disposables.DisposableContainer;

public final class StreamableEmpty<T> extends Streamable<T> {

    @Override
    public @NonNull Streamer<@NonNull T> stream(@NonNull DisposableContainer cancellation) {
        return new EmptyStreamer<T>();
    }

    static final class EmptyStreamer<T> implements Streamer<T> {

        @Override
        public @NonNull CompletionStage<Boolean> next() {
            return CompletableFuture.completedStage(false); // TODO would constant stages work here or is that contention?
        }

        @Override
        public @NonNull T current() {
            throw new NoSuchElementException("This Streamable/Streamer never has elements");
        }

        @Override
        public @NonNull CompletionStage<Void> cancel() {
            return CompletableFuture.completedStage(null); // TODO would constant stages work here or is that contention?
        }
    }
}
