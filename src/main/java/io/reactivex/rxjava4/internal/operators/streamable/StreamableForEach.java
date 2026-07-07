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
            Throwable finallyCrash = null;
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
                    try {
                        str.awaitFinish();
                    } catch (Throwable ex) {
                        Exceptions.throwIfFatal(ex);
                        finallyCrash = ex;
                    }
                }
            } catch (Throwable crash) {
                Exceptions.throwIfFatal(crash);
                finallyCrash = ExceptionHelper.unwrapAndCombine(crash, finallyCrash);
            }
            if (finallyCrash != null) {
                throw ExceptionHelper.wrapOrThrow(finallyCrash);
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
            Throwable finallyCrash = null;
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
                    try {
                        str.awaitFinish();
                    } catch (Throwable ex) {
                        Exceptions.throwIfFatal(ex);
                        finallyCrash = ex;
                    }
                }
            } catch (final Throwable crash) {
                Exceptions.throwIfFatal(crash);
                finallyCrash = ExceptionHelper.unwrapAndCombine(crash, finallyCrash);
            }
            if (finallyCrash != null) {
                throw ExceptionHelper.wrapOrThrow(finallyCrash);
            }
            return null;
        });
        canceller.add(Disposable.fromFuture(future));
        return new CompletionStageDisposable<>(future, canceller);
    }

    public static <T> CompletionStage<Void> forEach(Streamable<T> me, StreamerInput<? super T> consumer, ExecutorService executor) {
        var cf = new CompletableFuture<Void>();
        CompletableFuture.runAsync(() -> {
            Throwable error = null;
            var cancellation = consumer.cancellation();
            var streamer = me.stream(cancellation);
            try {
                try {
                    while (!cancellation.isDisposed()) {
                        if (streamer.awaitNext()) {
                            Streamer.awaitBoolean(consumer.next(streamer.current()));
                        } else {
                            break;
                        }
                    }
                } finally {
                    try {
                        streamer.awaitFinish();
                    } catch (Throwable ex) {
                        Exceptions.throwIfFatal(ex);
                        error = ExceptionHelper.unwrap(ex);
                    }
                }
            } catch (Throwable crash) {
                Exceptions.throwIfFatal(crash);
                crash = ExceptionHelper.unwrap(crash);
                if (error != null) {
                    crash.addSuppressed(error);
                }
                error = crash;
            }
            try {
                Streamer.awaitVoid(consumer.finish(error));
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                if (error != null) {
                    ex.addSuppressed(error);
                }
                cf.completeExceptionally(ex);
                return;
            }
            cf.complete(null);
        }, executor);
        return cf;
    }
}
