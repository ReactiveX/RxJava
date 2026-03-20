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

import java.lang.reflect.InvocationTargetException;
import java.util.Objects;
import java.util.concurrent.*;

import io.reactivex.rxjava4.annotations.NonNull;
import io.reactivex.rxjava4.disposables.*;
import io.reactivex.rxjava4.exceptions.Exceptions;
import io.reactivex.rxjava4.functions.Consumer;
import io.reactivex.rxjava4.internal.operators.streamable.*;

/**
 * The {@code IAsyncEnumerable} of the Java world.
 * Runs best with Virtual Threads.
 * TODO proper docs
 * @param <T> the element type of the stream.
 * @since 4.0.0
 */
public abstract class Streamable<@NonNull T> {

    /**
     * Realizes the stream and returns an interface that let's one consume it.
     * @param cancellation where to register and listen for cancellation calls.
     * @return the Streamer instance to consume.
     */
    @NonNull
    public abstract Streamer<T> stream(@NonNull DisposableContainer cancellation);

    /**
     * Realizes the stream and returns an interface that let's one consume it.
     * @return the Streamer instance to consume.
     */
    @NonNull
    public final Streamer<T> stream() {
        return stream(new CompositeDisposable()); // FIXME, use a practically no-op disposable container instead
    }

    /**
     * Returns an empty {@code Streamable} that never produces an item and just completes.
     * @param <T> the element type
     * @return the {@code Streamable} instance
     */
    @NonNull
    public static <@NonNull T> Streamable<T> empty() {
        return new StreamableEmpty<>();
    }

    /**
     * Returns a single-element {@code Streamable} that produces the constant item and completes.
     * @param <T> the element type
     * @param item the constant item to produce
     * @return the {@code Streamable} instance
     */
    public static <@NonNull T> Streamable<T> just(@NonNull T item) {
        Objects.requireNonNull(item, "item is null");
        return new StreamableJust<>(item);
    }

    /**
     * Consumes elements from this {@code Streamable} via the provided executor service.
     * @param consumer the callback that gets the elements until completion
     * @return a Disposable that let's one cancel the sequence asynchronously.
     */
    @NonNull
    public final CompletionStageDisposable<Void> forEach(@NonNull Consumer<? super T> consumer) {
        CompositeDisposable canceller = new CompositeDisposable();
        return forEach(consumer, canceller, Executors.newVirtualThreadPerTaskExecutor());
    }

    /**
     * Consumes elements from this {@code Streamable} via the provided executor service.
     * @param consumer the callback that gets the elements until completion
     * @param canceller the container to trigger cancellation of the sequence
     * @return the {@code CompletionStage} that gets notified when the sequence ends
     */
    public final CompletionStageDisposable<Void> forEach(@NonNull Consumer<? super T> consumer, @NonNull DisposableContainer canceller) {
        return forEach(consumer, canceller, Executors.newVirtualThreadPerTaskExecutor());
    }

    /**
     * Consumes elements from this {@code Streamable} via the provided executor service.
     * @param consumer the callback that gets the elements until completion
     * @param executor the service that hosts the blocking waits.
     * @return a Disposable that let's one cancel the sequence asynchronously.
     */
    @NonNull
    public final CompletionStageDisposable<Void> forEach(@NonNull Consumer<? super T> consumer, @NonNull ExecutorService executor) {
        CompositeDisposable canceller = new CompositeDisposable();
        return forEach(consumer, canceller, executor);
    }

    /**
     * Consumes elements from this {@code Streamable} via the provided executor service.
     * @param consumer the callback that gets the elements until completion
     * @param canceller the container to trigger cancellation of the sequence
     * @param executor the service that hosts the blocking waits.
     * @return the {@code CompletionStage} that gets notified when the sequence ends
     */
    @SuppressWarnings("unchecked")
    public final CompletionStageDisposable<Void> forEach(@NonNull Consumer<? super T> consumer, @NonNull DisposableContainer canceller, @NonNull ExecutorService executor) {
        Objects.requireNonNull(consumer, "consumer is null");
        Objects.requireNonNull(canceller, "canceller is null");
        Objects.requireNonNull(executor, "executor is null");
        final Streamable<T> me = this;
        var future = executor.submit(() -> {
            try (var str = me.stream(canceller)) {
                while (!canceller.isDisposed()) {
                    if (str.next().toCompletableFuture().join()) {
                        consumer.accept(Objects.requireNonNull(str.current(), "The upstream Streamable " + me.getClass() + " produced a null element!"));
                    } else {
                        break;
                    }
                }
            } catch (final Throwable crash) {
                Exceptions.throwIfFatal(crash);
                if (crash instanceof RuntimeException ex) {
                    throw ex;
                }
                if (crash instanceof Exception ex) {
                    throw ex;
                }
                throw new InvocationTargetException(crash);
            }
            return null;
        });
        canceller.add(Disposable.fromFuture(future));
        return new CompletionStageDisposable<Void>(StreamableHelper.toCompletionStage((Future<Void>)(Future<?>)future), canceller);
    }

    /**
     * Consume this {@code Streamable} via the given flow-reactive-streams subscriber.
     * @param subscriber the subscriber to consume with.
     * @param executor the service that hosts the blocking waits.
     */
    public final void subscribe(@NonNull Flow.Subscriber<? super T> subscriber, @NonNull ExecutorService executor) {

    }
}
