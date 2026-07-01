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

package io.reactivex.rxjava4.internal.util;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;

import io.reactivex.rxjava4.annotations.*;
import io.reactivex.rxjava4.core.Notification;
import io.reactivex.rxjava4.disposables.*;
import io.reactivex.rxjava4.exceptions.Exceptions;

/**
 * Static Async coordination methods.
 * @since 4.0.0
 */
public interface AwaitCoordinatorStatic {

    /**
     * The cancellable {@code await} keyword for async/await.
     * @param <T> the type of the returned value if any.
     * @param stage the stage to await virtual-blockingly
     * @param canceller the container that can trigger a cancellation on demand
     * @return the awaited value
     * @throws CancellationException if the computation was cancelled
     * @throws CompletionException if this future completed
     * exceptionally or a completion computation threw an exception
     */
    @Nullable
    static <T> T await(@NonNull CompletionStage<T> stage, @Nullable DisposableContainer canceller) {
        var f = stage.toCompletableFuture();
        if (canceller == null) {
            return f.join();
        }
        var d = Disposable.fromFuture(f, true);
        canceller.subscribe(d);
        return f.join();
    }

    /**
     * Runs a function while turning it into a CompletionStage with a canceller supplied too.
     * @param <U> the return type of the function
     * @param function the function to apply
     * @param canceller the canceller to use
     * @param executor the executor to use
     * @return the new stage
     */
    static <U> CompletionStage<U> runStage(Function<DisposableContainer, U> function,
            DisposableContainer canceller, Executor executor) {
        var loopback = new SerialDisposable();
        canceller.add(loopback);

        // new Exception().printStackTrace();

        var f =  CompletableFuture.supplyAsync(() -> {
            try {
                return function.apply(canceller);
            } finally {
                canceller.delete(loopback);
            }
        }, executor);

        var d = Disposable.fromFuture(f, true);
        loopback.replace(d);

        return f;
    }

    /**
     * Await all stages to completely some way and then return a list for each outcome in Notification format.
     * @param <T> the common element type
     * @param stages the stages to await all
     * @param canceller to trigger a cancellation
     * @return the list of outcomes as notifications
     */
    static <T> List<Notification<T>> awaitAll(Iterable<? extends CompletionStage<? extends T>> stages, DisposableContainer canceller) {
        var result = new ArrayList<Notification<T>>();
        for (var stage : stages) {
            try {
                var v = await(stage, canceller);
                if (v == null) {
                    result.add(Notification.createOnComplete());
                } else {
                    result.add(Notification.createOnNext(v));
                }
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                result.add(Notification.createOnError(ex));
            }
        }
        return result;
    }

    /**
     * Await for the very first responder, including value, failure or completion.
     * @param <T> the common element type
     * @param stages the stages to await all
     * @param canceller to trigger a cancellation
     * @return the list of outcomes as notifications
     */
    static <T> T awaitFirst(Iterable<? extends CompletionStage<? extends T>> stages, DisposableContainer canceller) {
        var winner = new CompletableFuture<CompletionStage<? extends T>>();

        for (var stage : stages) {
            stage.whenComplete((_, _) -> winner.complete(stage));
        }
        return await(winner, canceller).toCompletableFuture().getNow(null);
    }

    /**
     * Await for the very first successful responder and return its value.
     * @param <T> the common element type
     * @param stages the stages to await all
     * @param canceller to trigger a cancellation
     * @return the list of outcomes as notifications
     */
    @NonNull
    @SuppressWarnings("unchecked")
    static <T> CompletionStage<T> awaitFirstStage(
            @NonNull Iterable<? extends CompletionStage<? extends T>> stages,
            @NonNull DisposableContainer canceller) {
        var winner = new CompletableFuture<CompletionStage<? extends T>>();

        for (var stage : stages) {
            stage.whenComplete((_, _) -> winner.complete(stage));
        }
        return (CompletionStage<T>)await(winner, canceller).toCompletableFuture().getNow(null);
    }

    /**
     * Await for the very first successful responder and return its index.
     * @param <T> the common element type
     * @param stages the stages to await all
     * @param canceller to trigger a cancellation
     * @return the list of outcomes as notifications
     */
    @NonNull
    static <T> int awaitFirstIndex(
            @NonNull List<? extends CompletionStage<? extends T>> stages,
            @NonNull DisposableContainer canceller) {
        var winner = new CompletableFuture<Integer>();

        for (int i = 0; i < stages.size(); i++) {
            var stage = stages.get(i);
            var fi = i;
            stage.whenComplete((_, _) -> winner.complete(fi));
        }
        return await(winner, canceller);
    }

    /**
     * Await for the very first successful responder and return its value.
     * @param <T> the common element type
     * @param stages the stages to await all
     * @param canceller to trigger a cancellation
     * @return the list of outcomes as notifications
     */
    static <T> T awaitFirstSuccess(Iterable<? extends CompletionStage<? extends T>> stages, DisposableContainer canceller) {
        var winner = new CompletableFuture<CompletionStage<? extends T>>();

        for (var stage : stages) {
            stage.whenComplete((_, e) -> {
                if (e == null) {
                    winner.complete(stage);
                }
            });
        }
        return await(winner, canceller).toCompletableFuture().getNow(null);
    }

}
