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
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.rxjava4.annotations.NonNull;
import io.reactivex.rxjava4.core.*;
import io.reactivex.rxjava4.disposables.DisposableContainer;
import io.reactivex.rxjava4.exceptions.Exceptions;
import io.reactivex.rxjava4.functions.*;
import io.reactivex.rxjava4.internal.fuseable.HasUpstreamStreamableSource;

public record StreamableIntercept<T>(
        Streamable<T> source,
        @NonNull BiFunction<? super DisposableContainer, ? super Streamer<? extends T>, ? extends Streamer<? extends T>> onStream,
        @NonNull BiFunction<? super DisposableContainer, ? super CompletionStage<Boolean>, ? extends CompletionStage<Boolean>> onNext,
        @NonNull Function<? super T, ? extends T> onCurrent,
        @NonNull BiFunction<? super DisposableContainer, ? super CompletionStage<Void>, ? extends CompletionStage<Void>> onFinish
) implements Streamable<T>, HasUpstreamStreamableSource<T> {

    @Override
    public @NonNull Streamer<@NonNull T> stream(@NonNull DisposableContainer cancellation) {
        Streamer<? extends T> streamer;
        try {
            streamer = Objects.requireNonNull(onStream.apply(cancellation, source.stream(cancellation)), "onStream returned a null Streaner");
        } catch (Throwable ex) {
            Exceptions.throwIfFatal(ex);
            return StreamableError.createFailed(ex);
        }
        return new InterceptStreamer<>(streamer, onNext, onCurrent, onFinish, new AtomicReference<>(), new AtomicReference<>(cancellation));
    }

    record InterceptStreamer<T>(
            @NonNull Streamer<? extends T> upstream,
            @NonNull BiFunction<? super DisposableContainer, ? super CompletionStage<Boolean>, ? extends CompletionStage<Boolean>> onNext,
            @NonNull Function<? super T, ? extends T> onCurrent,
            @NonNull BiFunction<? super DisposableContainer, ? super CompletionStage<Void>, ? extends CompletionStage<Void>> onFinish,
            @NonNull AtomicReference<T> currentRef,
            @NonNull AtomicReference<DisposableContainer> cancellation
    ) implements Streamer<T> {
        @Override
        public @NonNull CompletionStage<Boolean> next() {
            CompletionStage<Boolean> result;
            try {
                result = Objects.requireNonNull(onNext.apply(cancellation.get(), upstream.next()), "The onNext returned a null CompletionStage");
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                return CompletableFuture.failedStage(ex);
            }
            return result.thenCompose(v -> {
                if (v) {
                    T t;
                    try {
                        t = Objects.requireNonNull(onCurrent.apply(upstream.current()), "The onCurrent returned a null value");
                    } catch (Throwable ex) {
                        Exceptions.throwIfFatal(ex);
                        currentRef.lazySet(null);
                        return CompletableFuture.failedStage(ex);
                    }
                    currentRef.lazySet(t);
                    return NEXT_TRUE;
                }
                currentRef.lazySet(null);
                return NEXT_FALSE;
            });
        }

        @Override
        public @NonNull T current() {
            return currentRef.get();
        }

        @Override
        public @NonNull CompletionStage<Void> finish() {
            CompletionStage<Void> result;
            try {
                result = Objects.requireNonNull(onFinish.apply(cancellation.get(), upstream.finish()), "onFinish returned a null CompletionStage");
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                result = CompletableFuture.failedStage(ex);
            }
            return result;
        }
    }
}
