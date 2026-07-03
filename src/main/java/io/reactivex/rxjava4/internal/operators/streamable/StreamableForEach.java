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

import io.reactivex.rxjava4.annotations.NonNull;
import io.reactivex.rxjava4.core.*;
import io.reactivex.rxjava4.disposables.*;
import io.reactivex.rxjava4.exceptions.Exceptions;
import io.reactivex.rxjava4.functions.*;
import io.reactivex.rxjava4.internal.util.ExceptionHelper;

/**
 * ForEach implementation to unclutter the {@link Streamable} type.
 */
public record StreamableForEach() {

    public static <T> CompletionStageDisposable<Void> forEach(
            @NonNull Streamable<T> me,
            @NonNull Consumer<? super T> consumer,
            @NonNull DisposableContainer canceller,
            @NonNull ExecutorService executor) {
        var future = CompletableFuture.<Void>supplyAsync(() -> {
            var str = me.stream(canceller);
            try {
                try {
                    while (!canceller.isDisposed()) {
                        if (str.awaitNext()) {
                            // System.out.println("Received " + str.current());
                            consumer.accept(Objects.requireNonNull(str.current(), "The upstream Streamable " + me.getClass() + " produced a null element!"));
                        } else {
                            // System.out.println("EOF ");
                            break;
                        }
                    }
                } finally {
                    str.awaitFinish();
                }
            } catch (final Throwable crash) {
                Exceptions.throwIfFatal(crash);
                if (crash instanceof CompletionException ce) {
                    throw ExceptionHelper.wrapOrThrow(ce.getCause());
                }
                throw ExceptionHelper.wrapOrThrow(crash);
            }
            return null;
        }, executor);
        canceller.add(Disposable.fromFuture(future));
        return new CompletionStageDisposable<>(future, canceller);
    }

    public static <T> CompletionStageDisposable<Void> forEach(
            @NonNull Streamable<T> me,
            @NonNull BiConsumer<? super T, ? super Disposable> consumer,
            @NonNull DisposableContainer canceller,
            @NonNull ExecutorService executor) {
        var future = CompletableFuture.<Void>supplyAsync(() -> {
            var str = me.stream(canceller);
            try {
                try {
                var stopper = Disposable.empty();
                    while (!canceller.isDisposed() && !stopper.isDisposed()) {
                        if (str.awaitNext()) {
                            // System.out.println("Received " + str.current());
                            var v = Objects.requireNonNull(str.current(), "The upstream Streamable " + me.getClass() + " produced a null element!");
                            consumer.accept(v, stopper);
                        } else {
                            // System.out.println("EOF ");
                            break;
                        }
                    }
                } finally {
                    str.awaitFinish();
                }
            } catch (final Throwable crash) {
                Exceptions.throwIfFatal(crash);
                if (crash instanceof CompletionException ce) {
                    throw ExceptionHelper.wrapOrThrow(ce.getCause());
                }
                throw ExceptionHelper.wrapOrThrow(crash);
            }
            return null;
        });
        canceller.add(Disposable.fromFuture(future));
        return new CompletionStageDisposable<>(future, canceller);
    }
}
