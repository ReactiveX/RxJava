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
import java.util.*;
import java.util.concurrent.*;

import io.reactivex.rxjava4.annotations.*;
import io.reactivex.rxjava4.disposables.*;
import io.reactivex.rxjava4.exceptions.Exceptions;
import io.reactivex.rxjava4.functions.*;
import io.reactivex.rxjava4.internal.operators.streamable.*;
import io.reactivex.rxjava4.internal.util.AwaitCoordinatorStatic;
import io.reactivex.rxjava4.schedulers.Schedulers;
import io.reactivex.rxjava4.subscribers.TestSubscriber;

/**
 * The holographically emergent {@code IAsyncEnumerable} of the Java world.
 * Runs best with Virtual Threads.
 * TODO proper docs
 * @param <T> the element type of the stream.
 * @since 4.0.0
 */
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

    // TODO, why no public final so it is not unnecessarily reimplemented, Java?
    /**
     * Realizes the stream and returns an interface that lets one consume it.
     * @return the Streamer instance to consume.
     */
    @CheckReturnValue
    @NonNull
    default Streamer<T> stream() {
        return stream(new CompositeDisposable()); // FIXME, use a practically no-op disposable container instead
    }

    // oooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooo
    // Data sources and wrappers
    // oooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooo

    /**
     * Returns an empty {@code Streamable} that never produces an item and just completes.
     * @param <T> the element type
     * @return the {@code Streamable} instance
     */
    @CheckReturnValue
    @NonNull
    static <@NonNull T> Streamable<T> empty() {
        return new StreamableEmpty<>();
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
        return new StreamableJust<>(item);
    }

    /**
     * Convert any Flow.Publisher into a Streamable sequence.
     * @param <T> the element type
     * @param source Flow.Publisher to convert
     * @return the new Streamable instance
     */
    @CheckReturnValue
    @NonNull
    static <T> Streamable<T> fromPublisher(@NonNull Flow.Publisher<T> source) {
        Objects.requireNonNull(source, "source is null");
        return fromPublisher(source, Executors.newVirtualThreadPerTaskExecutor());
    }

    /**
     * Convert any Flow.Publisher into a Streamable sequence.
     * @param <T> the element type
     * @param source Flow.Publisher to convert
     * @param executor where the conversion will run
     * @return the new Streamable instance
     */
    @CheckReturnValue
    @NonNull
    static <T> Streamable<T> fromPublisher(@NonNull Flow.Publisher<T> source, @NonNull ExecutorService executor) {
        Objects.requireNonNull(source, "source is null");
        return new StreamableFromPublisher<T>(source, executor);
    }

    /**
     * Generate a sequence of values via a virtual generator callback (yielder)
     * which is free to block and is natively backpressured.
     * <p>
     * Runs on the {@link Schedulers#virtual()} scheduler.
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
     * Generates a sequence in order which the stages complete in any form.
     * @param <T> the common element type
     * @param stages the iterable of stages to be relayed in the order they complete
     * @param executor the executor to run the blocking operator
     * @return the new Streamable instance
     */
    @SuppressWarnings("unchecked")
    @NonNull
    static <@NonNull T> Streamable<CompletionStage<T>> fromStages(@NonNull Iterable<? extends CompletionStage<? extends T>> stages, ExecutorService executor) {
        return create(emitter -> {
            var list = new ArrayList<CompletionStage<? extends T>>();
            for(var stage : stages) {
                list.add(stage);
            }
            while (list.size() != 0) {
                var winner = AwaitCoordinatorStatic.awaitFirstIndex(list, emitter.canceller());
                emitter.emit((CompletionStage<T>)list.remove(winner));
            }
        }, executor);
    }

    /**
     * Emits the elements of each inner sequence produced by the outher sequence.
     * @param <T> the common element type
     * @param sources a streamable of inner streamables
     * @param exec the executorservice where to run the virtual wait
     * @return the new Streamable instance.
     */
    static <@NonNull T> Streamable<T> concat(Streamable<? extends Streamable<? extends T>> sources, ExecutorService exec) {
        return create(emitter -> {
            try (var mainSource = sources.forEach(item -> {
                try (var innerSource = item.forEach(emitter::emit, emitter.canceller().derive(), exec)) {
                    innerSource.await(emitter.canceller());
                }
            }, emitter.canceller(), exec)) {
                mainSource.await(emitter.canceller());
            };
        }, exec);
    }

    // oooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooo
    // Operators
    // oooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooo

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
        return Flowable.virtualCreate(emitter -> {
            me.forEach(emitter::emit).await(emitter.canceller());
        }, executor);
    }

    /**
     * Transforms the upstream sequence into zero or more elements for the downstream.
     * @param <R> the result element type
     * @param transformer the interface to implement the transforming logic
     * @return the new Streamable instance
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
        return create(emitter -> {
            me.forEach((item, stopper) -> {
                // System.out.println("item " + item);
                transformer.transform(item, emitter, stopper);
            }, emitter.canceller(), executor)
            .await(emitter.canceller());
        }, executor);
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
    @SuppressWarnings("unchecked")
    default CompletionStageDisposable<Void> forEach(@NonNull Consumer<? super T> consumer, @NonNull DisposableContainer canceller, @NonNull ExecutorService executor) {
        Objects.requireNonNull(consumer, "consumer is null");
        Objects.requireNonNull(canceller, "canceller is null");
        Objects.requireNonNull(executor, "executor is null");
        final Streamable<T> me = this;
        var future = executor.submit(() -> {
            try (var str = me.stream(canceller)) {
                while (!canceller.isDisposed()) {
                    if (str.awaitNext(canceller)) {
                        // System.out.println("Received " + str.current());
                        consumer.accept(Objects.requireNonNull(str.current(), "The upstream Streamable " + me.getClass() + " produced a null element!"));
                    } else {
                        // System.out.println("EOF ");
                        break;
                    }
                }
                // System.out.println("Canceller status after loop: " + canceller.isDisposed());
            } catch (final Throwable crash) {
                Exceptions.throwIfFatal(crash);
                // System.out.println("Canceller status in error: " + canceller.isDisposed());
                crash.printStackTrace();
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
     * Consumes elements from this {@code Streamable} via the provided executor service.
     * @param consumer the callback that gets the elements until completion
     * @param canceller the container to trigger cancellation of the sequence
     * @param executor the service that hosts the blocking waits.
     * @return the {@code CompletionStage} that gets notified when the sequence ends
     */
    @CheckReturnValue
    @NonNull
    @SuppressWarnings("unchecked")
    default CompletionStageDisposable<Void> forEach(
            @NonNull BiConsumer<? super T, ? super Disposable> consumer,
            @NonNull DisposableContainer canceller,
            @NonNull ExecutorService executor) {
        Objects.requireNonNull(consumer, "consumer is null");
        Objects.requireNonNull(canceller, "canceller is null");
        Objects.requireNonNull(executor, "executor is null");
        final Streamable<T> me = this;
        var future = executor.submit(() -> {
            try (var str = me.stream(canceller)) {
                var stopper = Disposable.empty();
                while (!canceller.isDisposed() && !stopper.isDisposed()) {
                    if (str.awaitNext(canceller)) {
                        // System.out.println("Received " + str.current());
                        var v = Objects.requireNonNull(str.current(), "The upstream Streamable " + me.getClass() + " produced a null element!");
                        consumer.accept(v, stopper);
                    } else {
                        // System.out.println("EOF ");
                        break;
                    }
                }
                // System.out.println("Canceller status after loop: " + canceller.isDisposed());
            } catch (final Throwable crash) {
                Exceptions.throwIfFatal(crash);
                // System.out.println("Canceller status in error: " + canceller.isDisposed());
                // crash.printStackTrace();
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
        return new CompletionStageDisposable<Void>(
                StreamableHelper.toCompletionStage((Future<Void>)(Future<?>)future), canceller);
    }

    /**
     * Consume this {@code Streamable} via the given flow-reactive-streams subscriber.
     * @param subscriber the subscriber to consume with.
     * @param executor the service that hosts the blocking waits.
     */
    default void subscribe(@NonNull Flow.Subscriber<? super T> subscriber, @NonNull ExecutorService executor) {
        final Streamable<T> me = this;
        Flowable.<T>virtualCreate(emitter -> {
            // System.out.println("subscribe::virtualCreate");
                    // System.out.println("subscribe::virtualCreate::forEach::emit");
                    me.forEach(emitter::emit).await(emitter.canceller());
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
                    // System.out.println("Emitting " + v);
                    me.forEach(emitter::emit).await(emitter.canceller());
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
