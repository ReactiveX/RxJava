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

import java.io.Serial;
import java.util.Collection;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;

import io.reactivex.rxjava4.annotations.*;
import io.reactivex.rxjava4.core.*;
import io.reactivex.rxjava4.core.config.StreamableInterceptConfig;
import io.reactivex.rxjava4.disposables.Disposable;
import io.reactivex.rxjava4.exceptions.*;
import io.reactivex.rxjava4.functions.Consumer;
import io.reactivex.rxjava4.internal.util.*;

/**
 * Helper static methods for {@link Streamable}s, {@link CompletableFuture}s and {@link CompletionStage}s.
 * @since 4.0.0
 */
public enum StreamableHelper {
    ;

    /**
     * Checks which source completes first, calls the given acceptor with 1 or 2 indicating the winner,
     * then terminates the resulting {@link CompletableFuture} with said terminal event.
     * <p>
     * Use the {@code thenAcceptor} to do something about the winner and/or loser before the terminal
     * signal is propagated further.
     * @param <T> the common element type of the stages
     * @param first the first stage to wait for
     * @param second the second stage to wait for
     * @param thenAcceptor the callback that receives who won
     * @return the new {@code CompletableFuture} that gets completed with the winner's event.
     */
    @CheckReturnValue
    @NonNull
    public static <T> CompletableFuture<T> race(CompletionStage<T> first, CompletionStage<T> second, IntConsumer thenAcceptor) {
        var result = new CompletableFuture<T>();
        var winner = new AtomicInteger();
        first.whenComplete((v, e) -> {
            if (winner.compareAndSet(0, 1)) {
                thenAcceptor.accept(1);
                if (e != null) {
                    result.completeExceptionally(e);
                } else {
                    result.complete(v);
                }
            }
        });
        second.whenComplete((v, e) -> {
            if (winner.compareAndSet(0, 2)) {
                thenAcceptor.accept(2);
                if (e != null) {
                    result.completeExceptionally(e);
                } else {
                    result.complete(v);
                }
            }
        });

        return result;
    }

    /**
     * Checks which source completes first, calls the given acceptor with 1 or 2 indicating the winner.
     * <p>
     * Use the {@code thenAcceptor} to do something about the winner and/or loser.
     * @param <T> the common element type of the stages
     * @param first the first stage to wait for
     * @param second the second stage to wait for
     * @param thenAcceptor the callback that receives who won
     */
    @CheckReturnValue
    @NonNull
    public static <T> void whenEither(CompletionStage<T> first, CompletionStage<T> second, IntConsumer thenAcceptor) {
        var result = new CompletableFuture<T>();
        var winner = new AtomicInteger();
        first.whenComplete((v, e) -> {
            if (winner.compareAndSet(0, 1)) {
                thenAcceptor.accept(1);
                if (e != null) {
                    result.completeExceptionally(e);
                } else {
                    result.complete(v);
                }
            }
        });
        second.whenComplete((v, e) -> {
            if (winner.compareAndSet(0, 2)) {
                thenAcceptor.accept(2);
                if (e != null) {
                    result.completeExceptionally(e);
                } else {
                    result.complete(v);
                }
            }
        });
    }

    /**
     * Creates a {@link CompletableFuture} that completes when both sources complete in a way,
     * relaying one or the other's exception, or both exceptions via {@link CompositeException}.
     * @param one a stage to wait for completion
     * @param two a stage to wait for completion
     * @return then new {@code CompletableFuture} instance
     */
    @CheckReturnValue
    @NonNull
    public static CompletableFuture<Void> whenBoth(CompletionStage<?> one, CompletionStage<?> two) {
        var result = new CompletableFuture<Void>();
        var errors = new AtomicThrowable();
        var wip = new AtomicInteger(2);
        BiConsumer<Object, Throwable> handler = (_, e) -> {
            if (e != null) {
                errors.tryAddThrowableOrReport(e);
            }
            if (wip.decrementAndGet() == 0) {
                var err = errors.get();
                if (err != null) {
                    result.completeExceptionally(err);
                } else {
                    result.complete(null);
                }
            }
        };
        one.whenComplete(handler);
        two.whenComplete(handler);
        return result;
    }

    /**
     * Creates a {@link CompletableFuture} that completes when the second completes after the first completes,
     * chaining their completion into a sequence and relays either or both's exception as is or via a
     * {@link CompositeException}.
     * @param first the first stage to wait for completion
     * @param supplier produces the next stage when the first stage completes in any way
     * @return the new {@code CompletableFuture} instance
     */
    @CheckReturnValue
    @NonNull
    public static CompletableFuture<Void> andThenSupply(@NonNull CompletionStage<?> first,
            @NonNull Supplier<@NonNull ? extends CompletionStage<?>> supplier) {
        var result = new CompletableFuture<Void>();
        var errors = new AtomicThrowable();
        first.whenComplete((_, e) -> {
            if (e != null) {
                errors.tryAddThrowableOrReport(e);
            }
            supplier.get().whenComplete((_, e1) -> {
                if (e1 != null) {
                    errors.tryAddThrowableOrReport(e1);
                }
                var err = errors.get();
                if (err != null) {
                    result.completeExceptionally(err);
                } else {
                    result.complete(null);
                }
            });
        });
        return result;
    }

    /**
     * Check if the throwable is itself a {@link CancellationException} or
     * it is a {@link CompletionException} caused by a {@code CancellationException}.
     * <p>
     * Why is that {@code completableFuture.completeExceptionally(new CancellationException())}
     * results in the wrapping? no idea.
     * @param e the throwable to check
     * @return true if the throwable holds a cancellation exception in some fashion
     */
    @CheckReturnValue
    static boolean isCancellation(@Nullable Throwable e) {
        return e instanceof CancellationException
                || (e instanceof CompletionException && e.getCause() instanceof CancellationException);
    }

    /**
     * Creates a {@link CompletableFuture} that relays the terminal events of the source {@code stage}
     * except any {@link CancellationException} if the given {@link Disposable#isDisposed()} returns {@code true}.
     * <p>
     * Use it to suppress expected cancellation errors.
     * @param <T> the element type of the stage
     * @param stage the original stage to gate the cancellation exception of
     * @param disposable the {@code Disposable} to check
     * @param defaultValue to signal if the cancellation exception was gated
     * @return the new {@code CompletableFuture} instance
     */
    @CheckReturnValue
    @NonNull
    public static <T> CompletableFuture<T> suppressCancel(
            @NonNull CompletionStage<T> stage, @NonNull Disposable disposable, T defaultValue) {
        var result = new CompletableFuture<T>();
        stage.whenComplete((v, e) -> {
            if (disposable.isDisposed() && isCancellation(e)) {
                result.complete(defaultValue);
            } else
            if (e != null) {
                result.completeExceptionally(e);
            } else {
                result.complete(v);
            }
        });
        return result;
    }

    /**
     * Creates a {@link CompletableFuture} that suppresses the source value or cancellation exception and
     * replaces them with a normal completion signaling the {@code defaultValue}.
     * <p>
     * Use it to suppress expected cancellation errors and any source value.
     * @param <T> the element type of the stage
     * @param stage the original stage to gate the cancellation exception of
     * @param defaultValue to signal if the cancellation exception was gated
     * @return the new {@code CompletableFuture} instance
     */
    @CheckReturnValue
    @NonNull
    public static <T> CompletableFuture<T> suppressValueAndCancel(
            @NonNull CompletionStage<T> stage, T defaultValue) {
        var result = new CompletableFuture<T>();
        stage.whenComplete((_, e) -> {
            if (isCancellation(e)) { // FIXME coverage possible even?
                result.complete(defaultValue);
            } else
            if (e != null) {
                result.completeExceptionally(e);
            } else {
                result.complete(defaultValue);
            }
        });
        return result;
    }

    /**
     * When the stage completes, the future is completed with the very same value or exception.
     * @param <T> the element type
     * @param stage the stage to forward its completion signals
     * @param future the future to receive the completion signals
     */
    public static <T> void forward(CompletionStage<T> stage, CompletableFuture<T> future) {
        stage.whenComplete((u, e) -> {
            if (e != null) {
                future.completeExceptionally(e);
            } else {
                future.complete(u);
            }
        });
    }

    /**
     * Awaits all the stages to complete in any form, then completes the returned {@link CompletableFuture} normally,
     * or via the possible {@link CompositeException}s of all the failed stages.
     * @param stages the collection of {@link CompletionStage}s to await
     * @return the new {@code CompletableFuture} that gets completed
     */
    public static CompletableFuture<Void> awaitAllVoid(Collection<? extends CompletionStage<?>> stages) {
        int size = stages.size();
        if (size == 0) {
            return Streamer.FINISHED;
        }
        var cf = new CompletableFuture<Void>();
        var wip = new AtomicIntegerCompleter(cf, size);

        for (var stage : stages) {
            stage.whenComplete(wip);
        }

        return cf;
    }

    public static CompletableFuture<Boolean> awaitAllBoolean(Collection<? extends CompletionStage<Boolean>> stages) {
        int size = stages.size();
        if (size == 0) {
            return Streamer.NEXT_FALSE;
        }
        var cf = new CompletableFuture<Boolean>();
        var wip = new AtomicIntegerBooleanCompleter(cf, size);

        for (var stage : stages) {
            stage.whenComplete(wip);
        }

        return cf;
    }

    /**
     * A counter and callback composite to save on allocation cost for awaiting many stages via
     * {@link #awaitAll}.
     */
    static final class AtomicIntegerCompleter extends AtomicInteger implements BiConsumer<Object, Throwable> {

        @Serial
        private static final long serialVersionUID = 1598649149973711008L;

        final CompletableFuture<Void> future;

        final AtomicThrowable errors;

        public AtomicIntegerCompleter(CompletableFuture<Void> future, int count) {
            super(count);
            this.future = future;
            this.errors = new AtomicThrowable();
        }

        @Override
        public void accept(Object t, Throwable u) {
            if (u != null && !isCancellation(u)) {
                errors.tryAddThrowable(ExceptionHelper.unwrap(u));
            }
            if (decrementAndGet() == 0) {
                var err = errors.terminate();
                if (err != null) {
                    future.completeExceptionally(err);
                } else {
                    future.complete(null);
                }
            }
        }
    }
    /**
     * A counter and callback composite to save on allocation cost for awaiting many stages via
     * {@link #awaitAll}.
     */
    static final class AtomicIntegerBooleanCompleter extends AtomicInteger implements BiConsumer<Boolean, Throwable> {

        @Serial
        private static final long serialVersionUID = 1598649149973711008L;

        final CompletableFuture<Boolean> future;

        final AtomicThrowable errors;

        final AtomicBoolean outcome;

        public AtomicIntegerBooleanCompleter(CompletableFuture<Boolean> future, int count) {
            super(count);
            this.future = future;
            this.errors = new AtomicThrowable();
            this.outcome = new AtomicBoolean(true);
        }

        @Override
        public void accept(Boolean t, Throwable u) {
            if (u != null && !isCancellation(u)) {
                errors.tryAddThrowable(ExceptionHelper.unwrap(u));
            }
            if (t != null && !t) {
                if (outcome.get()) {
                    outcome.getAndSet(false);
                }
            }
            if (decrementAndGet() == 0) {
                var err = errors.terminate();
                if (err != null) {
                    future.completeExceptionally(err);
                } else {
                    future.complete(outcome.get());
                }
            }
        }
    }

    /**
     * Create a {@link StreamableInterceptConfig} that can consume the {@link Streamer#next()} errors.
     * @param <T> the element type of the {@link Streamable}
     * @param consumer the consumer to be called with the error
     * @return the new {@code StreamableInterceptConfig} instance
     */
    public static <T> StreamableInterceptConfig<T> createOnError(Consumer<? super Throwable> consumer) {
        return new StreamableInterceptConfig<>((_, v) -> v, (_, v) -> {
            var cf = new CompletableFuture<Boolean>();
            v.whenComplete((u, e) -> {
                if (e != null) {
                    try {
                        consumer.accept(e);
                    } catch (Throwable ex) {
                        Exceptions.throwIfFatal(ex);
                        ex.addSuppressed(e);
                        e = ex;
                    }
                    cf.completeExceptionally(e);
                } else {
                    cf.complete(u);
                }
            });
            return cf;
        }, v -> v, (_, v) -> v);
    }

    /**
     * Shortcuts the stage to call the consumer directly if it is detected as completed.
     * @param <T> the signal type of the {@link CompletionStage}
     * @param stage the stage to handle
     * @param consumer the consumer called with the stage's value or its exception
     */
    public static <T> void whenComplete(CompletionStage<T> stage, java.util.function.BiConsumer<? super T, ? super Throwable> consumer) {
        if (stage instanceof CompletableFuture<T> cf) {
            switch (cf.state()) {
            case SUCCESS -> consumer.accept(cf.getNow(null), null);
            case CANCELLED, FAILED -> consumer.accept(null, cf.exceptionNow());
            default -> stage.whenComplete(consumer);
            }
        } else {
            stage.whenComplete(consumer);
        }
    }
}
