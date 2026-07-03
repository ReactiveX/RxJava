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

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

import io.reactivex.rxjava4.annotations.*;
import io.reactivex.rxjava4.core.config.*;
import io.reactivex.rxjava4.disposables.*;
import io.reactivex.rxjava4.exceptions.Exceptions;
import io.reactivex.rxjava4.functions.*;
import io.reactivex.rxjava4.internal.functions.ObjectHelper;
import io.reactivex.rxjava4.internal.operators.streamable.*;
import io.reactivex.rxjava4.internal.util.ExceptionHelper;
import io.reactivex.rxjava4.plugins.RxJavaPlugins;
import io.reactivex.rxjava4.schedulers.Schedulers;
import io.reactivex.rxjava4.subscribers.TestSubscriber;

/// Represents a virtual-thread capable, multi-valued (a)synchronous sequence of values that
/// builds upon the Java [CompletionStage]-based concurrency-coordination model.
///
/// The lifecycle of the sequence is as follows:
///
/// - consumer calls {@link #stream(DisposableContainer)}
/// - consumer calls {@link Streamer#next()} in a loop and consumer checks if what
///   the {@link CompletionStage} outcome is:
///   - if it succeeded with a boolean {@code true}, it is safe to call {@link Streamer#current()}.
///   - if it succeeded with a boolean {@code false}, no further values are coming.
///   - if it failed with a {@code Throwable}, no further values are coming and the error can be propagated further
/// - consumer calls {@link Streamer#finish()}
///
/// It is always necessary to have the consumer call {@code finish} because that is responsible for cleaning up
/// resources of the upstream.
///
/// Downstream cancellations are signaled via the [DisposableContainer], where operators can register their own
/// [Disposable]s that get disposed. Because dispose can happen at any time and asynchronously to the consumption loop,
/// the sensitive sources must complete their waiting `CompletionStage` returned by `next` exceptionally via a
/// [CancellationException]. This will unblock the loops and invoke the `finish` method of the lifecycle at
/// the consumer thread. Depending on the operator, the `CancellationException` may not be propagated further.
///
/// If a source wishes to fail, it must signal the [Throwable] via the returned {@code CompletionStage} of {@code next}.
/// If the `finish` also throws, its `Throwable` should be added as suppressed exception to the original `Throwable`.
///
/// The `Streamer` methods must be invoked sequentially and non-overlappingly, similar to the
/// <a href='https://github.com/reactive-streams/reactive-streams-jvm#1.3'>Reactive Streams rule §1.3</a>.
///
/// This reactive type was modeled after the C# `IAsyncEnumerable` and `IAsyncEnumerator` interfaces. Unfortunately,
/// Java never added any `async`/`await` infrastructure, plus `CompletionStage` doesn't even have any native way to blockingly
/// join to it. Therefore, when running in a (virtual) blocking fashion, one may use the {@link Streamer#awaitNext()}
/// or {@link Streamer#awaitFinish()} helper methods.
///
/// @param <T> the element type of the {@code Streamable} sequence.
/// @since 4.0.0
@FunctionalInterface
public interface Streamable<@NonNull T> {

    // oooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooo
    // API
    // oooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooo

    /**
     * Realizes the stream and returns an interface that lets one consume it.
     * @param cancellation where to register and listen for cancellation calls.
     * @return the Streamer instance to consume.
     */
    @CheckReturnValue
    @NonNull
    Streamer<T> stream(@NonNull DisposableContainer cancellation);

    // oooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooo
    // HELPERS
    // oooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooo

    // oooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooo
    // Data sources and wrappers
    // oooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooo

    /**
     * Returns an empty {@code Streamable} that never produces an item and just completes.
     * @param <T> the element type
     * @return the {@code Streamable} instance
     */
    @SuppressWarnings("unchecked")
    @CheckReturnValue
    @NonNull
    static <@NonNull T> Streamable<T> empty() {
        return RxJavaPlugins.onAssembly((Streamable<T>)StreamableEmpty.INSTANCE);
    }

    /**
     * Returns a single-element {@code Streamable} that produces the constant item and completes.
     * @param <T> the element type
     * @param item the constant item to produce
     * @return the {@code Streamable} instance
     */
    @CheckReturnValue
    @NonNull
    static <@NonNull T> Streamable<T> just(@NonNull T item) {
        Objects.requireNonNull(item, "item is null");
        return RxJavaPlugins.onAssembly(new StreamableJust<>(item));
    }

    /**
     * Filters out the upstream items that do not pass the given predicate
     * @param predicate the callback that should return {@code true} to let the upstream value pass
     *                  or {@code false} to ignore it and continue with the next upstream item
     * @return the new {@code Streamable} instance
     * @throw NullPointerException if {@code predicate} is {@code null}
     */
    @CheckReturnValue
    @NonNull
    default Streamable<T> filter(@NonNull Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        return RxJavaPlugins.onAssembly(new StreamableFilter<>(this, predicate));
    }

    /**
     * Streams all elements of the given items array.
     * @param <T> the element type of the items
     * @param items the array of items to stream
     * @return the new {@code Streamable} instance
     * @throws NullPointerException if {@code items} is {@code null}
     */
    @SafeVarargs
    @CheckReturnValue
    @NonNull
    static <@NonNull T> Streamable<T> fromArray(@NonNull T... items) {
        Objects.requireNonNull(items, "items is null");
        return RxJavaPlugins.onAssembly(new StreamableFromArray<>(items));
    }

    /**
     * Streams all elements of the given {@link Iterable} sequence.
     * @param <T> the element type of the items
     * @param items the iterable of items to stream
     * @return the new {@code Streamable} instance
     * @throws NullPointerException if {@code items} is {@code null}
     */
    @CheckReturnValue
    @NonNull
    static <@NonNull T> Streamable<T> fromIterable(@NonNull Iterable<? extends T> items) {
        Objects.requireNonNull(items, "items is null");
        return RxJavaPlugins.onAssembly(new StreamableFromIterable<>(items));
    }

    /**
     * Streams all elements of the given {@link Stream} sequence.
     * @param <T> the element type of the items
     * @param items the stream of items to stream
     * @return the new {@code Streamable} instance
     * @throws NullPointerException if {@code items} is {@code null}
     */
    @CheckReturnValue
    @NonNull
    static <@NonNull T> Streamable<T> fromStream(@NonNull Stream<? extends T> items) {
        Objects.requireNonNull(items, "items is null");
        return RxJavaPlugins.onAssembly(new StreamableFromStream<>(items));
    }

    /**
     * Convert any {@link java.util.concurrent.Flow.Publisher} into a {@code Streamable} sequence.
     * @param <T> the element type
     * @param source Flow.Publisher to convert
     * @return the new {@code Streamable} instance
     */
    @CheckReturnValue
    @NonNull
    static <T> Streamable<T> fromPublisher(@NonNull Flow.Publisher<T> source) {
        Objects.requireNonNull(source, "source is null");
        return fromPublisher(source, Executors.newVirtualThreadPerTaskExecutor());
    }

    /**
     * Convert any {@link java.util.concurrent.Flow.Publisher} into a {@code Streamable} sequence.
     * @param <T> the element type
     * @param source Flow.Publisher to convert
     * @param executor where the conversion will run
     * @return the new {@code Streamable} instance
     */
    @CheckReturnValue
    @NonNull
    static <T> Streamable<T> fromPublisher(@NonNull Flow.Publisher<T> source, @NonNull ExecutorService executor) {
        Objects.requireNonNull(source, "source is null");
        Objects.requireNonNull(executor, "executor is null");
        return RxJavaPlugins.onAssembly(new StreamableFromPublisher<>(source, executor));
    }

    /**
     * Returns an {@code Streamable} that never produces an item and never terminates.
     * @param <T> the element type
     * @return the {@code Streamable} instance
     */
    @SuppressWarnings("unchecked")
    @CheckReturnValue
    @NonNull
    static <@NonNull T> Streamable<T> never() {
        return RxJavaPlugins.onAssembly((Streamable<T>)StreamableNever.INSTANCE);
    }

    /**
     * Generate a sequence of values via a virtual generator callback (yielder)
     * which is free to block and is natively backpressured.
     * <p>
     * Runs on the {@link Schedulers#virtual()} scheduler.
     * <p>
     * Example
     * <pre><code>
     * Streamable.create(emitter -> {
     *     emitter.emit(1);
     *     emitter.emit(2);
     *     emitter.emit(3);
     * })
     * .forEach(System.out::println)
     * ;
     * </code></pre>
     * @param <T> the element type
     * @param generator the generator to use
     * @return the streamable instance
     */
    @CheckReturnValue
    @NonNull
    static <@NonNull T> Streamable<T> create(@NonNull VirtualGenerator<T> generator) {
        // FIXME native implementation
        return Flowable.virtualCreate(generator)
                .toStreamable();
    }

    /**
     * Generate a sequence of values via a virtual generator callback (yielder)
     * which is free to block and is natively backpressured.
     * <p>
     * Runs on the given scheduler.
     * <p>
     * Example
     * <pre><code>
     * Streamable.create(emitter -> {
     *     emitter.emit(1);
     *     emitter.emit(2);
     *     emitter.emit(3);
     * }, Schedulers.cached())
     * .forEach(System.out::println)
     * ;
     * </code></pre>
     * @param <T> the element type
     * @param generator the generator to use
     * @param scheduler the scheduler to run the virtual generator on
     * @return the streamable instance
     */
    @CheckReturnValue
    @NonNull
    static <@NonNull T> Streamable<T> create(@NonNull VirtualGenerator<T> generator, @NonNull Scheduler scheduler) {
        // FIXME native implementation
        return Flowable.virtualCreate(generator, scheduler)
                .toStreamable();
    }

    /**
     * Generate a sequence of values via a virtual generator callback (yielder)
     * which is free to block and is natively backpressured.
     * <p>
     * Runs on the given executor service.
     * @param <T> the element type
     * @param generator the generator to use
     * @param executor the executor to run the virtual generator on
     * @return the streamable instance
     */
    @CheckReturnValue
    @NonNull
    static <@NonNull T> Streamable<T> create(@NonNull VirtualGenerator<T> generator, @NonNull ExecutorService executor) {
        // FIXME native implementation
        return Flowable.virtualCreate(generator, executor)
                .toStreamable();
    }

    /**
     * Defers the creation of the actual {@code Streamable}, allowing a per streamer
     * state to be created along with it.
     * @param <T> the element type of the {@code Streamable}
     * @param supplier the callback that returns the actual {@code Streamable} to be streamed
     * @return the new {@code Streamable} instance
     * @throws NullPointerException if {@code supplier} is {@code null}
     */
    @CheckReturnValue
    @NonNull
    static <@NonNull T> Streamable<T> defer(Supplier<? extends Streamable<? extends T>> supplier) {
        Objects.requireNonNull(supplier, "supplier is null");
        return RxJavaPlugins.onAssembly(new StreamableDefer<>(supplier));
    }

    /**
     * Creates a {@code Streamable} that signals the given {@link Throwable} immediately when it begins streaming,
     * ending the sequence.
     * @param <T> the element type of the sequence
     * @param throwable the {@code Throwable} to signal immediately upon streaming
     * @return the new {@code Streamable} instance
     * @throws NullPointerException if {@code throwable} is {@code null}
     */
    @CheckReturnValue
    @NonNull
    static <@NonNull T> Streamable<T> error(Throwable throwable) {
        Objects.requireNonNull(throwable, "throwable is null");
        return RxJavaPlugins.onAssembly(new StreamableError<>(throwable));
    }

    /**
     * Emits the elements of each inner sequence produced by the outer sequence.
     * @param <T> the common element type
     * @param sources a streamable of inner streamables
     * @param executor the executorservice where to run the virtual wait
     * @return the new {@code Streamable} instance.
     * @throws NullPointerException if {@code sources} or {@code exec} is {@code null}
     */
    @CheckReturnValue
    @NonNull
    static <@NonNull T> Streamable<T> concat(Streamable<? extends Streamable<? extends T>> sources, ExecutorService executor) {
        Objects.requireNonNull(sources, "sources is null");
        Objects.requireNonNull(executor, "executor is null");
        return create(emitter -> {
            try (var mainSource = sources.forEach(item -> {
                try (var innerSource = item.forEach(emitter::emit, emitter.canceller().derive(), executor)) {
                    innerSource.await();
                }
            }, emitter.canceller(), executor)) {
                mainSource.await();
            }
        }, executor);
    }

    /**
     * Emits elements from start up to start + count exclusive.
     * @param start the start element
     * @param count the number of elements to emit
     * @return the new {@code Streamable} instance
     */
    @CheckReturnValue
    @NonNull
    static Streamable<Integer> range(int start, int count) {
        if (count < 0) {
            throw new IllegalArgumentException("count >= 0 required but it was " + count);
        } else
        if (count == 0) {
            return empty();
        } else
        if (count == 1) {
            return just(start);
        } else
        if ((long)start + (count - 1) > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Integer overflow");
        }
        return RxJavaPlugins.onAssembly(new StreamableRange(start, count));
    }

    /**
     * Emits elements from start up to start + count exclusive.
     * @param start the start element
     * @param count the number of elements to emit
     * @return the new {@code Streamable} instance
     */
    @CheckReturnValue
    @NonNull
    static Streamable<Long> rangeLong(long start, long count) {
        if (count < 0) {
            throw new IllegalArgumentException("count >= 0 required but it was " + count);
        }

        if (count == 0) {
            return empty();
        }

        if (count == 1) {
            return just(start);
        }

        long end = start + (count - 1);
        if (start > 0 && end < 0) {
            throw new IllegalArgumentException("Overflow! start + count is bigger than Long.MAX_VALUE");
        }
        return RxJavaPlugins.onAssembly(new StreamableRangeLong(start, count));
    }

    // oooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooo
    // Operators
    // oooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooo

    /**
     * Calls the given consumer whenever an upstream item becomes available.
     * @param consumer the callback to invoke with the next item from upstream
     * @return the new {@code Streamable} instance
     * @throws NullPointerException if {@code consumer} is {@code null}
     */
    default Streamable<T> doOnNext(Consumer<? super T> consumer) {
        Objects.requireNonNull(consumer, "consumer is null");
        return intercept(new StreamableInterceptConfig<>(v -> { consumer.accept(v); return v; } ));
    }

    /**
     * Maps each upstream item onto a {@code Streamable} and runs them concurrently while
     * relaying inner items as first-come-first-served manner.
     * @param <R> the element type of the output sequence
     * @param mapper the function that turns an upstream item into a {@code Streamable} inner sequence
     * @param config the configuration record for this operator
     * @return the new {@code Streamable} instance
     * @throws NullPointerException if {@code mapper} or {@code config} is {@code null}
     */
    @CheckReturnValue
    @NonNull
    default <R> Streamable<R> flatMap(@NonNull Function<? super T, ? extends Streamable<? extends R>> mapper,
            @NonNull StandardConcurrentConfig config) {
        Objects.requireNonNull(mapper, "mapper is null");
        Objects.requireNonNull(config, "config is null");
        return RxJavaPlugins.onAssembly(new StreamableFlatMap<>(this, mapper, config.maxConcurrency()));
    }

    /**
     * Hides the identity of this {@code Streamable} and its {@link Streamer}.
     * <p>
     * Use it to break optimizations or hide concrete implementations.
     * @return the new {@code Streamable} instance
     */
    @CheckReturnValue
    @NonNull
    default Streamable<T> hide() {
        return RxJavaPlugins.onAssembly(new StreamableHide<>(this));
    }

    /**
     * Intercepts the lifecycle method calls of {@code Streamable} and {@link Streamer}
     * and allows the modification of them via Function callbacks.
     * @param config the configuration record for this operator
     * @return the new {@code Streamable} instance
     * @thros NullPointerException if {@code config} is {@code null}
     */
    @CheckReturnValue
    @NonNull
    default Streamable<T> intercept(StreamableInterceptConfig<T> config) {
        Objects.requireNonNull(config, "config is null");
        return RxJavaPlugins.onAssembly(new StreamableIntercept<>(this,
                config.onStream(), config.onNext(), config.onCurrent(), config.onFinish()));
    }

    /**
     * <strong>This method requires advanced knowledge about building operators, please consider
     * other standard composition methods first;</strong>
     * Returns a {@code Streamable} instance which when its {@link #stream(DisposableContainer)} is invoked,
     * applies the specified operator callback to the upstream {@link Streamer} to produce
     * an actual {@code Streamer} instance to be handed downstream.
     * <p>
     * Use it to implement operators without creating the surrounding {@code Streamable} class.
     * <p>
     * If the {@code lifter} returns {@code null} or throws, the downstream will receive a
     * standard error streamer.
     * @param <R> the downstream type of the sequence
     * @param lifter the callback that will be invoked with the upstream {@code Streamer} and is expected
     *               to produce a {@code Streamer} for the downstream.
     * @return the new {@code Streamable} instance
     * @throws NullPointerException if {@code lifter} is {@code null}
     */
    @CheckReturnValue
    @NonNull
    default <@NonNull R> Streamable<R> lift(@NonNull StreamableOperator<? super T, ? extends R> lifter) {
        Objects.requireNonNull(lifter, "lifter is null");
        return RxJavaPlugins.onAssembly(new StreamableLift<T, R>(this, lifter));
    }

    /**
     * Maps each upstream item into another item via a mapper function.
     * @param <R> the element type of the mapping
     * @param mapper the function that takes an upstream item and returns an item to be emitted
     *               to the downstream
     * @return the new {@code Streamable} instance
     * @throw NullPointerException if {@code mapper} is {@code null}
     */
    @CheckReturnValue
    @NonNull
    default <@NonNull R> Streamable<R> map(@NonNull Function<? super T, ? extends R> mapper) {
        Objects.requireNonNull(mapper, "mapper is null");
        return RxJavaPlugins.onAssembly(new StreamableMap<>(this, mapper));
    }

    /**
     * Maps each upstream item into another, optional item via a mapper function that skips the empty optionals.
     * @param <R> the element type of the mapping
     * @param mapper the function that takes an upstream item and returns an optional item to be emitted / skipped
     *               to the downstream
     * @return the new {@code Streamable} instance
     * @throw NullPointerException if {@code mapper} is {@code null}
     */
    @CheckReturnValue
    @NonNull
    default <@NonNull R> Streamable<R> mapOptional(@NonNull Function<? super T, ? extends Optional<? extends R>> mapper) {
        Objects.requireNonNull(mapper, "mapper is null");
        return RxJavaPlugins.onAssembly(new StreamableMapOptional<>(this, mapper));
    }

    /**
     * Takes at most the given number of items from the upstream and relays it to the downstream,
     * then cancels the rest of the sequence.
     * @param n the maximum number of items to relay
     * @return the new {@code Streamable} instance
     */
    @CheckReturnValue
    @NonNull
    default Streamable<T> take(long n) {
        ObjectHelper.verifyPositive(n, "n");
        return defer(() -> {
            var countdown = new AtomicLong(n);
            return transform((item, emitter, stopper) -> {
                emitter.emit(item);
                if (countdown.decrementAndGet() <= 0) {
                    stopper.dispose();
                }
            });
        });
    }

    /**
     * Calls the specified converter function during assembly time and returns its resulting value.
     * <p>
     * This allows fluent conversion to any other type.
     * @param <R> the resulting object type
     * @param converter the function that receives the current {@code Observable} instance and returns a value
     * @return the converted value
     * @throws NullPointerException if {@code converter} is {@code null}
     */
    @CheckReturnValue
    @NonNull
    default <@NonNull R> R to(@NonNull StreamableConverter<T, ? extends R> converter) {
        return Objects.requireNonNull(converter, "converter is null").apply(this);
    }

    /**
     * Converts the streamable into a Flowable representation, running
     * on the default Executors.newVirtualThreadPerTaskExecutor() virtual thread.
     * @return the new Flowable instance
     */
    @CheckReturnValue
    @NonNull
    default Flowable<T> toFlowable() {
        return toFlowable(Executors.newVirtualThreadPerTaskExecutor());
    }

    /**
     * Converts the streamable into a Flowable representation, running
     * on the provided executor service.
     * @param executor the executor to use
     * @return the new Flowable instance
     */
    @CheckReturnValue
    @NonNull
    default Flowable<T> toFlowable(@NonNull ExecutorService executor) {
        Objects.requireNonNull(executor, "executir is null");
        var me = this;
        return Flowable.virtualCreate(emitter -> me.forEach(emitter::emit).await(), executor);
    }

    /**
     * Transforms the upstream sequence into zero or more elements for the downstream.
     * @param <R> the result element type
     * @param transformer the interface to implement the transforming logic
     * @return the new {@code Streamable} instance
     */
    @CheckReturnValue
    @NonNull
    default <@NonNull R> Streamable<R> transform(@NonNull VirtualTransformer<T, R> transformer) {
        return transform(transformer, Executors.newVirtualThreadPerTaskExecutor());
    }

    /**
     * Transforms the upstream sequence into zero or more elements for the downstream.
     * @param <R> the result element type
     * @param transformer the interface to implement the transforming logic
     * @param executor where to run the transform and blocking operations
     * @return the new Streamable instance
     */
    @CheckReturnValue
    @NonNull
    default <@NonNull R> Streamable<R> transform(@NonNull VirtualTransformer<T, R> transformer,
            @NonNull ExecutorService executor) {
        Objects.requireNonNull(transformer, "transformer is null");
        Objects.requireNonNull(executor, "executor is null");
        var me = this;
        return create(emitter -> me.forEach((item, stopper) -> {
            // System.out.println("item " + item);
            transformer.transform(item, emitter, stopper);
        }, emitter.canceller(), executor)
        .await(), executor);
    }

    // oooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooo
    // Consumption methods and outgoing converters
    // oooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooo

    /**
     * Consumes elements from this {@code Streamable} via the provided executor service.
     * @param consumer the callback that gets the elements until completion
     * @return a Disposable that lets one cancel the sequence asynchronously.
     */
    @CheckReturnValue
    @NonNull
    default CompletionStageDisposable<Void> forEach(@NonNull Consumer<? super T> consumer) {
        CompositeDisposable canceller = new CompositeDisposable();
        return forEach(consumer, canceller, Executors.newVirtualThreadPerTaskExecutor());
    }

    /**
     * Consumes elements from this {@code Streamable} via the provided executor service.
     * @param consumer the callback that gets the elements until completion
     * @param canceller the container to trigger cancellation of the sequence
     * @return the {@code CompletionStage} that gets notified when the sequence ends
     */
    @CheckReturnValue
    @NonNull
    default CompletionStageDisposable<Void> forEach(@NonNull Consumer<? super T> consumer, @NonNull DisposableContainer canceller) {
        return forEach(consumer, canceller, Executors.newVirtualThreadPerTaskExecutor());
    }

    /**
     * Consumes elements from this {@code Streamable} via the provided executor service.
     * @param consumer the callback that gets the elements until completion
     * @param executor the service that hosts the blocking waits.
     * @return a Disposable that lets one cancel the sequence asynchronously.
     */
    @CheckReturnValue
    @NonNull
    default CompletionStageDisposable<Void> forEach(@NonNull Consumer<? super T> consumer, @NonNull ExecutorService executor) {
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
    @CheckReturnValue
    @NonNull
    default CompletionStageDisposable<Void> forEach(@NonNull Consumer<? super T> consumer, @NonNull DisposableContainer canceller, @NonNull ExecutorService executor) {
        Objects.requireNonNull(consumer, "consumer is null");
        Objects.requireNonNull(canceller, "canceller is null");
        Objects.requireNonNull(executor, "executor is null");
        final Streamable<T> me = this;
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

    /**
     * Consumes elements from this {@code Streamable} via the provided executor service.
     * @param consumer the callback that gets the elements until completion
     * @param canceller the container to trigger cancellation of the sequence
     * @param executor the service that hosts the blocking waits.
     * @return the {@code CompletionStage} that gets notified when the sequence ends
     */
    @CheckReturnValue
    @NonNull
    default CompletionStageDisposable<Void> forEach(
            @NonNull BiConsumer<? super T, ? super Disposable> consumer,
            @NonNull DisposableContainer canceller,
            @NonNull ExecutorService executor) {
        Objects.requireNonNull(consumer, "consumer is null");
        Objects.requireNonNull(canceller, "canceller is null");
        Objects.requireNonNull(executor, "executor is null");
        final Streamable<T> me = this;
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

    /**
     * Consume this {@code Streamable} via the given flow-reactive-streams subscriber.
     * @param subscriber the subscriber to consume with.
     * @param executor the service that hosts the blocking waits.
     */
    default void subscribe(@NonNull Flow.Subscriber<? super T> subscriber, @NonNull ExecutorService executor) {
        final Streamable<T> me = this;
        Flowable.<T>virtualCreate(emitter -> {
            me.forEach(emitter::emit, emitter.canceller().derive(), executor)
            .await();
        }, executor)
        .subscribe(subscriber);
    }

    /**
     * Consume this {@code Streamable} via the given flow-reactive-streams subscriber.
     * @param subscriber the subscriber to consume with.
     */
    default void subscribe(@NonNull Flow.Subscriber<? super T> subscriber) {
        final Streamable<T> me = this;
        Flowable.<T>virtualCreate(emitter -> {
            me.forEach(emitter::emit).await();
        })
        .subscribe(subscriber);
    }

    /**
     * Creates a new {@link TestSubscriber} and subscribes it to this {@code Streamable}.
     * @return the created test subscriber
     */
    @CheckReturnValue
    @NonNull
    default TestSubscriber<T> test() {
        var ts = new TestSubscriber<T>();
        subscribe(ts);
        return ts;
    }

    /**
     * Creates a new {@link TestSubscriber} and subscribes it to this {@code Streamable}.
     * @param executor the executor to use
     * @return the created test subscriber
     */
    @CheckReturnValue
    @NonNull
    default TestSubscriber<T> test(@NonNull ExecutorService executor) {
        var ts = new TestSubscriber<T>();
        subscribe(ts, executor);
        return ts;
    }
}
