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

package io.reactivex.rxjava3.parallel;

import java.util.*;
import java.util.stream.*;

import org.reactivestreams.*;

import io.reactivex.rxjava3.annotations.*;
import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.exceptions.MissingBackpressureException;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.internal.functions.*;
import io.reactivex.rxjava3.internal.jdk8.*;
import io.reactivex.rxjava3.internal.operators.parallel.*;
import io.reactivex.rxjava3.internal.subscriptions.EmptySubscription;
import io.reactivex.rxjava3.internal.util.*;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;

/**
 * Abstract base class for parallel publishing of events signaled to an array of {@link Subscriber}s.
 * <p>
 * Use {@link #from(Publisher)} to start processing a regular {@link Publisher} in 'rails'.
 * Use {@link #runOn(Scheduler)} to introduce where each 'rail' should run on thread-vise.
 * Use {@link #sequential()} to merge the sources back into a single {@link Flowable}.
 *
 * <p>History: 2.0.5 - experimental; 2.1 - beta
 * @param <T> the value type
 * @since 2.2
 */
public abstract class ParallelFlowable<@NonNull T> {

    /**
     * Subscribes an array of {@link Subscriber}s to this {@code ParallelFlowable} and triggers
     * the execution chain for all 'rails'.
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The backpressure behavior/expectation is determined by the supplied {@code Subscriber}.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code subscribe} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param subscribers the subscribers array to run in parallel, the number
     * of items must be equal to the parallelism level of this {@code ParallelFlowable}
     * @throws NullPointerException if {@code subscribers} is {@code null}
     * @see #parallelism()
     */
    @BackpressureSupport(BackpressureKind.SPECIAL)
    @SchedulerSupport(SchedulerSupport.NONE)
    public abstract void subscribe(@NonNull Subscriber<? super T>[] subscribers);

    /**
     * Returns the number of expected parallel {@link Subscriber}s.
     * @return the number of expected parallel {@code Subscriber}s
     */
    @CheckReturnValue
    public abstract int parallelism();

    /**
     * Validates the number of subscribers and returns {@code true} if their number
     * matches the parallelism level of this {@code ParallelFlowable}.
     *
     * @param subscribers the array of {@link Subscriber}s
     * @return {@code true} if the number of subscribers equals to the parallelism level
     * @throws NullPointerException if {@code subscribers} is {@code null}
     * @throws IllegalArgumentException if {@code subscribers.length} is different from {@link #parallelism()}
     */
    protected final boolean validate(@NonNull Subscriber<@NonNull ?>[] subscribers) {
        Objects.requireNonNull(subscribers, "subscribers is null");
        int p = parallelism();
        if (subscribers.length != p) {
            Throwable iae = new IllegalArgumentException("parallelism = " + p + ", subscribers = " + subscribers.length);
            for (Subscriber<@NonNull ?> s : subscribers) {
                EmptySubscription.error(iae, s);
            }
            return false;
        }
        return true;
    }

    /**
     * Take a {@link Publisher} and prepare to consume it on multiple 'rails' (number of CPUs)
     * in a round-robin fashion.
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The operator honors the backpressure of the parallel rails and
     *  requests {@link Flowable#bufferSize} amount from the upstream, followed
     *  by 75% of that amount requested after every 75% received.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code from} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the value type
     * @param source the source {@code Publisher}
     * @return the new {@code ParallelFlowable} instance
     * @throws NullPointerException if {@code source} is {@code null}
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    @BackpressureSupport(BackpressureKind.FULL)
    public static <@NonNull T> ParallelFlowable<T> from(@NonNull Publisher<? extends T> source) {
        return from(source, Runtime.getRuntime().availableProcessors(), Flowable.bufferSize());
    }

    /**
     * Take a {@link Publisher} and prepare to consume it on parallelism number of 'rails' in a round-robin fashion.
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The operator honors the backpressure of the parallel rails and
     *  requests {@link Flowable#bufferSize} amount from the upstream, followed
     *  by 75% of that amount requested after every 75% received.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code from} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the value type
     * @param source the source {@code Publisher}
     * @param parallelism the number of parallel rails
     * @return the new {@code ParallelFlowable} instance
     * @throws NullPointerException if {@code source} is {@code null}
     * @throws IllegalArgumentException if {@code parallelism} is non-positive
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    @BackpressureSupport(BackpressureKind.FULL)
    public static <@NonNull T> ParallelFlowable<T> from(@NonNull Publisher<? extends T> source, int parallelism) {
        return from(source, parallelism, Flowable.bufferSize());
    }

    /**
     * Take a {@link Publisher} and prepare to consume it on parallelism number of 'rails' ,
     * possibly ordered and round-robin fashion and use custom prefetch amount and queue
     * for dealing with the source {@code Publisher}'s values.
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The operator honors the backpressure of the parallel rails and
     *  requests the {@code prefetch} amount from the upstream, followed
     *  by 75% of that amount requested after every 75% received.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code from} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the value type
     * @param source the source {@code Publisher}
     * @param parallelism the number of parallel rails
     * @param prefetch the number of values to prefetch from the source
     * the source until there is a rail ready to process it.
     * @return the new {@code ParallelFlowable} instance
     * @throws NullPointerException if {@code source} is {@code null}
     * @throws IllegalArgumentException if {@code parallelism} or {@code prefetch} is non-positive
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    @BackpressureSupport(BackpressureKind.FULL)
    public static <@NonNull T> ParallelFlowable<T> from(@NonNull Publisher<? extends T> source,
            int parallelism, int prefetch) {
        Objects.requireNonNull(source, "source is null");
        ObjectHelper.verifyPositive(parallelism, "parallelism");
        ObjectHelper.verifyPositive(prefetch, "prefetch");

        return RxJavaPlugins.onAssembly(new ParallelFromPublisher<>(source, parallelism, prefetch));
    }

    /**
     * Maps the source values on each 'rail' to another value.
     * <p>
     * Note that the same {@code mapper} function may be called from multiple threads concurrently.
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The operator is a pass-through for backpressure and the behavior
     *  is determined by the upstream and downstream rail behaviors.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code map} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <R> the output value type
     * @param mapper the mapper function turning Ts into Rs.
     * @return the new {@code ParallelFlowable} instance
     * @throws NullPointerException if {@code mapper} is {@code null}
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    @BackpressureSupport(BackpressureKind.PASS_THROUGH)
    public final <@NonNull R> ParallelFlowable<R> map(@NonNull Function<? super T, ? extends R> mapper) {
        Objects.requireNonNull(mapper, "mapper is null");
        return RxJavaPlugins.onAssembly(new ParallelMap<>(this, mapper));
    }

    /**
     * Maps the source values on each 'rail' to another value and
     * handles errors based on the given {@link ParallelFailureHandling} enumeration value.
     * <p>
     * Note that the same {@code mapper} function may be called from multiple threads concurrently.
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The operator is a pass-through for backpressure and the behavior
     *  is determined by the upstream and downstream rail behaviors.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code map} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * <p>History: 2.0.8 - experimental
     * @param <R> the output value type
     * @param mapper the mapper function turning Ts into Rs.
     * @param errorHandler the enumeration that defines how to handle errors thrown
     *                     from the {@code mapper} function
     * @return the new {@code ParallelFlowable} instance
     * @throws NullPointerException if {@code mapper} or {@code errorHandler} is {@code null}
     * @since 2.2
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    @BackpressureSupport(BackpressureKind.PASS_THROUGH)
    public final <@NonNull R> ParallelFlowable<R> map(@NonNull Function<? super T, ? extends R> mapper, @NonNull ParallelFailureHandling errorHandler) {
        Objects.requireNonNull(mapper, "mapper is null");
        Objects.requireNonNull(errorHandler, "errorHandler is null");
        return RxJavaPlugins.onAssembly(new ParallelMapTry<>(this, mapper, errorHandler));
    }

    /**
     * Maps the source values on each 'rail' to another value and
     * handles errors based on the returned value by the handler function.
     * <p>
     * Note that the same {@code mapper} function may be called from multiple threads concurrently.
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The operator is a pass-through for backpressure and the behavior
     *  is determined by the upstream and downstream rail behaviors.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code map} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * <p>History: 2.0.8 - experimental
     * @param <R> the output value type
     * @param mapper the mapper function turning Ts into Rs.
     * @param errorHandler the function called with the current repeat count and
     *                     failure {@link Throwable} and should return one of the {@link ParallelFailureHandling}
     *                     enumeration values to indicate how to proceed.
     * @return the new {@code ParallelFlowable} instance
     * @throws NullPointerException if {@code mapper} or {@code errorHandler} is {@code null}
     * @since 2.2
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    @BackpressureSupport(BackpressureKind.PASS_THROUGH)
    public final <@NonNull R> ParallelFlowable<R> map(@NonNull Function<? super T, ? extends R> mapper, @NonNull BiFunction<? super Long, ? super Throwable, ParallelFailureHandling> errorHandler) {
        Objects.requireNonNull(mapper, "mapper is null");
        Objects.requireNonNull(errorHandler, "errorHandler is null");
        return RxJavaPlugins.onAssembly(new ParallelMapTry<>(this, mapper, errorHandler));
    }

    /**
     * Filters the source values on each 'rail'.
     * <p>
     * Note that the same predicate may be called from multiple threads concurrently.
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The operator is a pass-through for backpressure and the behavior
     *  is determined by the upstream and downstream rail behaviors.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code filter} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param predicate the function returning {@code true} to keep a value or {@code false} to drop a value
     * @return the new {@code ParallelFlowable} instance
     * @throws NullPointerException if {@code predicate} is {@code null}
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    @BackpressureSupport(BackpressureKind.PASS_THROUGH)
    public final ParallelFlowable<T> filter(@NonNull Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        return RxJavaPlugins.onAssembly(new ParallelFilter<>(this, predicate));
    }

    /**
     * Filters the source values on each 'rail' and
     * handles errors based on the given {@link ParallelFailureHandling} enumeration value.
     * <p>
     * Note that the same predicate may be called from multiple threads concurrently.
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The operator is a pass-through for backpressure and the behavior
     *  is determined by the upstream and downstream rail behaviors.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code filter} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * <p>History: 2.0.8 - experimental
     * @param predicate the function returning {@code true} to keep a value or {@code false} to drop a value
     * @param errorHandler the enumeration that defines how to handle errors thrown
     *                     from the {@code predicate}
     * @return the new {@code ParallelFlowable} instance
     * @throws NullPointerException if {@code predicate} or {@code errorHandler} is {@code null}
     * @since 2.2
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    @BackpressureSupport(BackpressureKind.PASS_THROUGH)
    public final ParallelFlowable<T> filter(@NonNull Predicate<? super T> predicate, @NonNull ParallelFailureHandling errorHandler) {
        Objects.requireNonNull(predicate, "predicate is null");
        Objects.requireNonNull(errorHandler, "errorHandler is null");
        return RxJavaPlugins.onAssembly(new ParallelFilterTry<>(this, predicate, errorHandler));
    }

    /**
     * Filters the source values on each 'rail' and
     * handles errors based on the returned value by the handler function.
     * <p>
     * Note that the same predicate may be called from multiple threads concurrently.
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The operator is a pass-through for backpressure and the behavior
     *  is determined by the upstream and downstream rail behaviors.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code map} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * <p>History: 2.0.8 - experimental
     * @param predicate the function returning {@code true} to keep a value or {@code false} to drop a value
     * @param errorHandler the function called with the current repeat count and
     *                     failure {@link Throwable} and should return one of the {@link ParallelFailureHandling}
     *                     enumeration values to indicate how to proceed.
     * @return the new {@code ParallelFlowable} instance
     * @throws NullPointerException if {@code predicate} or {@code errorHandler} is {@code null}
     * @since 2.2
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    @BackpressureSupport(BackpressureKind.PASS_THROUGH)
    public final ParallelFlowable<T> filter(@NonNull Predicate<? super T> predicate, @NonNull BiFunction<? super Long, ? super Throwable, ParallelFailureHandling> errorHandler) {
        Objects.requireNonNull(predicate, "predicate is null");
        Objects.requireNonNull(errorHandler, "errorHandler is null");
        return RxJavaPlugins.onAssembly(new ParallelFilterTry<>(this, predicate, errorHandler));
    }

    /**
     * Specifies where each 'rail' will observe its incoming values, specified via a {@link Scheduler}, with
     * no work-stealing and default prefetch amount.
     * <p>
     * This operator uses the default prefetch size returned by {@link Flowable#bufferSize()}.
     * <p>
     * The operator will call {@link Scheduler#createWorker()} as many
     * times as this {@code ParallelFlowable}'s parallelism level is.
     * <p>
     * No assumptions are made about the {@code Scheduler}'s parallelism level,
     * if the {@code Scheduler}'s parallelism level is lower than the {@code ParallelFlowable}'s,
     * some rails may end up on the same thread/worker.
     * <p>
     * This operator doesn't require the {@code Scheduler} to be trampolining as it
     * does its own built-in trampolining logic.
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The operator honors the backpressure of the parallel rails and
     *  requests {@link Flowable#bufferSize} amount from the upstream, followed
     *  by 75% of that amount requested after every 75% received.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code runOn} drains the upstream rails on the specified {@code Scheduler}'s
     *  {@link io.reactivex.rxjava3.core.Scheduler.Worker Worker}s.</dd>
     * </dl>
     *
     * @param scheduler the scheduler to use
     * @return the new {@code ParallelFlowable} instance
     * @throws NullPointerException if {@code scheduler} is {@code null}
     */
    @CheckReturnValue
    @NonNull
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    public final ParallelFlowable<T> runOn(@NonNull Scheduler scheduler) {
        return runOn(scheduler, Flowable.bufferSize());
    }

    /**
     * Specifies where each 'rail' will observe its incoming values, specified via a {@link Scheduler}, with
     * possibly work-stealing and a given prefetch amount.
     * <p>
     * This operator uses the default prefetch size returned by {@link Flowable#bufferSize()}.
     * <p>
     * The operator will call {@link Scheduler#createWorker()} as many
     * times as this {@code ParallelFlowable}'s parallelism level is.
     * <p>
     * No assumptions are made about the {@code Scheduler}'s parallelism level,
     * if the {@code Scheduler}'s parallelism level is lower than the {@code ParallelFlowable}'s,
     * some rails may end up on the same thread/worker.
     * <p>
     * This operator doesn't require the {@code Scheduler} to be trampolining as it
     * does its own built-in trampolining logic.
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The operator honors the backpressure of the parallel rails and
     *  requests the {@code prefetch} amount from the upstream, followed
     *  by 75% of that amount requested after every 75% received.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code runOn} drains the upstream rails on the specified {@code Scheduler}'s
     *  {@link io.reactivex.rxjava3.core.Scheduler.Worker Worker}s.</dd>
     * </dl>
     *
     * @param scheduler the scheduler to use
     * that rail's worker has run out of work.
     * @param prefetch the number of values to request on each 'rail' from the source
     * @return the new {@code ParallelFlowable} instance
     * @throws NullPointerException if {@code scheduler} is {@code null}
     * @throws IllegalArgumentException if {@code prefetch} is non-positive
     */
    @CheckReturnValue
    @NonNull
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    public final ParallelFlowable<T> runOn(@NonNull Scheduler scheduler, int prefetch) {
        Objects.requireNonNull(scheduler, "scheduler is null");
        ObjectHelper.verifyPositive(prefetch, "prefetch");
        return RxJavaPlugins.onAssembly(new ParallelRunOn<>(this, scheduler, prefetch));
    }

    /**
     * Reduces all values within a 'rail' and across 'rails' with a reducer function into one
     * {@link Flowable} sequence.
     * <p>
     * Note that the same reducer function may be called from multiple threads concurrently.
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The operator honors backpressure from the downstream and consumes
     *  the upstream rails in an unbounded manner (requesting {@link Long#MAX_VALUE}).</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code reduce} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param reducer the function to reduce two values into one.
     * @return the new {@code Flowable} instance emitting the reduced value or empty if the current {@code ParallelFlowable} is empty
     * @throws NullPointerException if {@code reducer} is {@code null}
     */
    @CheckReturnValue
    @NonNull
    @BackpressureSupport(BackpressureKind.UNBOUNDED_IN)
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Flowable<T> reduce(@NonNull BiFunction<T, T, T> reducer) {
        Objects.requireNonNull(reducer, "reducer is null");
        return RxJavaPlugins.onAssembly(new ParallelReduceFull<>(this, reducer));
    }

    /**
     * Reduces all values within a 'rail' to a single value (with a possibly different type) via
     * a reducer function that is initialized on each rail from an {@code initialSupplier} value.
     * <p>
     * Note that the same mapper function may be called from multiple threads concurrently.
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The operator honors backpressure from the downstream rails and consumes
     *  the upstream rails in an unbounded manner (requesting {@link Long#MAX_VALUE}).</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code reduce} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <R> the reduced output type
     * @param initialSupplier the supplier for the initial value
     * @param reducer the function to reduce a previous output of reduce (or the initial value supplied)
     * with a current source value.
     * @return the new {@code ParallelFlowable} instance
     * @throws NullPointerException if {@code initialSupplier} or {@code reducer} is {@code null}
     */
    @CheckReturnValue
    @NonNull
    @BackpressureSupport(BackpressureKind.UNBOUNDED_IN)
    @SchedulerSupport(SchedulerSupport.NONE)
    public final <@NonNull R> ParallelFlowable<R> reduce(@NonNull Supplier<R> initialSupplier, @NonNull BiFunction<R, ? super T, R> reducer) {
        Objects.requireNonNull(initialSupplier, "initialSupplier is null");
        Objects.requireNonNull(reducer, "reducer is null");
        return RxJavaPlugins.onAssembly(new ParallelReduce<>(this, initialSupplier, reducer));
    }

    /**
     * Merges the values from each 'rail' in a round-robin or same-order fashion and
     * exposes it as a regular {@link Flowable} sequence, running with a default prefetch value
     * for the rails.
     * <p>
     * This operator uses the default prefetch size returned by {@code Flowable.bufferSize()}.
     * <img width="640" height="602" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/parallelflowable.sequential.png" alt="">
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The operator honors backpressure from the downstream and
     *  requests {@link Flowable#bufferSize()} amount from each rail, then
     *  requests from each rail 75% of this amount after 75% received.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code sequential} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @return the new {@code Flowable} instance
     * @see ParallelFlowable#sequential(int)
     * @see ParallelFlowable#sequentialDelayError()
     */
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerSupport.NONE)
    @CheckReturnValue
    @NonNull
    public final Flowable<T> sequential() {
        return sequential(Flowable.bufferSize());
    }

    /**
     * Merges the values from each 'rail' in a round-robin or same-order fashion and
     * exposes it as a regular {@link Flowable} sequence, running with a give prefetch value
     * for the rails.
     * <img width="640" height="602" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/parallelflowable.sequential.png" alt="">
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The operator honors backpressure from the downstream and
     *  requests the {@code prefetch} amount from each rail, then
     *  requests from each rail 75% of this amount after 75% received.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code sequential} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param prefetch the prefetch amount to use for each rail
     * @return the new {@code Flowable} instance
     * @throws IllegalArgumentException if {@code prefetch} is non-positive
     * @see ParallelFlowable#sequential()
     * @see ParallelFlowable#sequentialDelayError(int)
     */
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerSupport.NONE)
    @CheckReturnValue
    @NonNull
    public final Flowable<T> sequential(int prefetch) {
        ObjectHelper.verifyPositive(prefetch, "prefetch");
        return RxJavaPlugins.onAssembly(new ParallelJoin<>(this, prefetch, false));
    }

    /**
     * Merges the values from each 'rail' in a round-robin or same-order fashion and
     * exposes it as a regular {@link Flowable} sequence, running with a default prefetch value
     * for the rails and delaying errors from all rails till all terminate.
     * <p>
     * This operator uses the default prefetch size returned by {@code Flowable.bufferSize()}.
     * <img width="640" height="602" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/parallelflowable.sequential.png" alt="">
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The operator honors backpressure from the downstream and
     *  requests {@link Flowable#bufferSize()} amount from each rail, then
     *  requests from each rail 75% of this amount after 75% received.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code sequentialDelayError} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * <p>History: 2.0.7 - experimental
     * @return the new {@code Flowable} instance
     * @see ParallelFlowable#sequentialDelayError(int)
     * @see ParallelFlowable#sequential()
     * @since 2.2
     */
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerSupport.NONE)
    @CheckReturnValue
    @NonNull
    public final Flowable<T> sequentialDelayError() {
        return sequentialDelayError(Flowable.bufferSize());
    }

    /**
     * Merges the values from each 'rail' in a round-robin or same-order fashion and
     * exposes it as a regular {@link Flowable} sequence, running with a give prefetch value
     * for the rails and delaying errors from all rails till all terminate.
     * <img width="640" height="602" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/parallelflowable.sequential.png" alt="">
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The operator honors backpressure from the downstream and
     *  requests the {@code prefetch} amount from each rail, then
     *  requests from each rail 75% of this amount after 75% received.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code sequentialDelayError} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * <p>History: 2.0.7 - experimental
     * @param prefetch the prefetch amount to use for each rail
     * @return the new {@code Flowable} instance
     * @throws IllegalArgumentException if {@code prefetch} is non-positive
     * @see ParallelFlowable#sequential()
     * @see ParallelFlowable#sequentialDelayError()
     * @since 2.2
     */
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerSupport.NONE)
    @CheckReturnValue
    @NonNull
    public final Flowable<T> sequentialDelayError(int prefetch) {
        ObjectHelper.verifyPositive(prefetch, "prefetch");
        return RxJavaPlugins.onAssembly(new ParallelJoin<>(this, prefetch, true));
    }

    /**
     * Sorts the 'rails' of this {@code ParallelFlowable} and returns a {@link Flowable} that sequentially
     * picks the smallest next value from the rails.
     * <p>
     * This operator requires a finite source {@code ParallelFlowable}.
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The operator honors backpressure from the downstream and
     *  consumes the upstream rails in an unbounded manner (requesting {@link Long#MAX_VALUE}).</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code sorted} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param comparator the comparator to use
     * @return the new {@code Flowable} instance
     * @throws NullPointerException if {@code comparator} is {@code null}
     */
    @CheckReturnValue
    @NonNull
    @BackpressureSupport(BackpressureKind.UNBOUNDED_IN)
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Flowable<T> sorted(@NonNull Comparator<? super T> comparator) {
        return sorted(comparator, 16);
    }

    /**
     * Sorts the 'rails' of this {@code ParallelFlowable} and returns a {@link Flowable} that sequentially
     * picks the smallest next value from the rails.
     * <p>
     * This operator requires a finite source {@code ParallelFlowable}.
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The operator honors backpressure from the downstream and
     *  consumes the upstream rails in an unbounded manner (requesting {@link Long#MAX_VALUE}).</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code sorted} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param comparator the comparator to use
     * @param capacityHint the expected number of total elements
     * @return the new {@code Flowable} instance
     * @throws NullPointerException if {@code comparator} is {@code null}
     * @throws IllegalArgumentException if {@code capacityHint} is non-positive
     */
    @CheckReturnValue
    @NonNull
    @BackpressureSupport(BackpressureKind.UNBOUNDED_IN)
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Flowable<T> sorted(@NonNull Comparator<? super T> comparator, int capacityHint) {
        Objects.requireNonNull(comparator, "comparator is null");
        ObjectHelper.verifyPositive(capacityHint, "capacityHint");
        int ch = capacityHint / parallelism() + 1;
        ParallelFlowable<List<T>> railReduced = reduce(Functions.createArrayList(ch), ListAddBiConsumer.instance());
        ParallelFlowable<List<T>> railSorted = railReduced.map(new SorterFunction<>(comparator));

        return RxJavaPlugins.onAssembly(new ParallelSortedJoin<>(railSorted, comparator));
    }

    /**
     * Sorts the 'rails' according to the comparator and returns a full sorted {@link List} as a {@link Flowable}.
     * <p>
     * This operator requires a finite source {@code ParallelFlowable}.
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The operator honors backpressure from the downstream and
     *  consumes the upstream rails in an unbounded manner (requesting {@link Long#MAX_VALUE}).</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code toSortedList} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param comparator the comparator to compare elements
     * @return the new {@code Flowable} instance
     * @throws NullPointerException if {@code comparator} is {@code null}
     */
    @CheckReturnValue
    @NonNull
    @BackpressureSupport(BackpressureKind.UNBOUNDED_IN)
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Flowable<List<T>> toSortedList(@NonNull Comparator<? super T> comparator) {
        return toSortedList(comparator, 16);
    }
    /**
     * Sorts the 'rails' according to the comparator and returns a full sorted {@link List} as a {@link Flowable}.
     * <p>
     * This operator requires a finite source {@code ParallelFlowable}.
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The operator honors backpressure from the downstream and
     *  consumes the upstream rails in an unbounded manner (requesting {@link Long#MAX_VALUE}).</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code toSortedList} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param comparator the comparator to compare elements
     * @param capacityHint the expected number of total elements
     * @return the new {@code Flowable} instance
     * @throws NullPointerException if {@code comparator} is {@code null}
     * @throws IllegalArgumentException if {@code capacityHint} is non-positive
     */
    @CheckReturnValue
    @NonNull
    @BackpressureSupport(BackpressureKind.UNBOUNDED_IN)
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Flowable<@NonNull List<T>> toSortedList(@NonNull Comparator<? super T> comparator, int capacityHint) {
        Objects.requireNonNull(comparator, "comparator is null");
        ObjectHelper.verifyPositive(capacityHint, "capacityHint");

        int ch = capacityHint / parallelism() + 1;
        ParallelFlowable<List<T>> railReduced = reduce(Functions.createArrayList(ch), ListAddBiConsumer.instance());
        ParallelFlowable<List<T>> railSorted = railReduced.map(new SorterFunction<>(comparator));

        Flowable<List<T>> merged = railSorted.reduce(new MergerBiFunction<>(comparator));

        return RxJavaPlugins.onAssembly(merged);
    }

    /**
     * Call the specified consumer with the current element passing through any 'rail'.
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The operator is a pass-through for backpressure and the behavior
     *  is determined by the upstream and downstream rail behaviors.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code map} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param onNext the callback
     * @return the new {@code ParallelFlowable} instance
     * @throws NullPointerException if {@code onNext} is {@code null}
     */
    @CheckReturnValue
    @NonNull
    @BackpressureSupport(BackpressureKind.PASS_THROUGH)
    @SchedulerSupport(SchedulerSupport.NONE)
    public final ParallelFlowable<T> doOnNext(@NonNull Consumer<? super T> onNext) {
        Objects.requireNonNull(onNext, "onNext is null");
        return RxJavaPlugins.onAssembly(new ParallelPeek<>(this,
                onNext,
                Functions.emptyConsumer(),
                Functions.emptyConsumer(),
                Functions.EMPTY_ACTION,
                Functions.EMPTY_ACTION,
                Functions.emptyConsumer(),
                Functions.EMPTY_LONG_CONSUMER,
                Functions.EMPTY_ACTION
                ));
    }

    /**
     * Call the specified consumer with the current element passing through any 'rail' and
     * handles errors based on the given {@link ParallelFailureHandling} enumeration value.
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The operator is a pass-through for backpressure and the behavior
     *  is determined by the upstream and downstream rail behaviors.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code map} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * <p>History: 2.0.8 - experimental
     * @param onNext the callback
     * @param errorHandler the enumeration that defines how to handle errors thrown
     *                     from the {@code onNext} consumer
     * @return the new {@code ParallelFlowable} instance
     * @throws NullPointerException if {@code onNext} or {@code errorHandler} is {@code null}
     * @since 2.2
     */
    @CheckReturnValue
    @NonNull
    @BackpressureSupport(BackpressureKind.PASS_THROUGH)
    @SchedulerSupport(SchedulerSupport.NONE)
    public final ParallelFlowable<T> doOnNext(@NonNull Consumer<? super T> onNext, @NonNull ParallelFailureHandling errorHandler) {
        Objects.requireNonNull(onNext, "onNext is null");
        Objects.requireNonNull(errorHandler, "errorHandler is null");
        return RxJavaPlugins.onAssembly(new ParallelDoOnNextTry<>(this, onNext, errorHandler));
    }

    /**
     * Call the specified consumer with the current element passing through any 'rail' and
     * handles errors based on the returned value by the handler function.
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The operator is a pass-through for backpressure and the behavior
     *  is determined by the upstream and downstream rail behaviors.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code map} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * <p>History: 2.0.8 - experimental
     * @param onNext the callback
     * @param errorHandler the function called with the current repeat count and
     *                     failure {@link Throwable} and should return one of the {@link ParallelFailureHandling}
     *                     enumeration values to indicate how to proceed.
     * @return the new {@code ParallelFlowable} instance
     * @throws NullPointerException if {@code onNext} or {@code errorHandler} is {@code null}
     * @since 2.2
     */
    @CheckReturnValue
    @NonNull
    @BackpressureSupport(BackpressureKind.PASS_THROUGH)
    @SchedulerSupport(SchedulerSupport.NONE)
    public final ParallelFlowable<T> doOnNext(@NonNull Consumer<? super T> onNext, @NonNull BiFunction<? super Long, ? super Throwable, ParallelFailureHandling> errorHandler) {
        Objects.requireNonNull(onNext, "onNext is null");
        Objects.requireNonNull(errorHandler, "errorHandler is null");
        return RxJavaPlugins.onAssembly(new ParallelDoOnNextTry<>(this, onNext, errorHandler));
    }

    /**
     * Call the specified consumer with the current element passing through any 'rail'
     * after it has been delivered to downstream within the rail.
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The operator is a pass-through for backpressure and the behavior
     *  is determined by the upstream and downstream rail behaviors.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code map} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param onAfterNext the callback
     * @return the new {@code ParallelFlowable} instance
     * @throws NullPointerException if {@code onAfterNext} is {@code null}
     */
    @CheckReturnValue
    @NonNull
    @BackpressureSupport(BackpressureKind.PASS_THROUGH)
    @SchedulerSupport(SchedulerSupport.NONE)
    public final ParallelFlowable<T> doAfterNext(@NonNull Consumer<? super T> onAfterNext) {
        Objects.requireNonNull(onAfterNext, "onAfterNext is null");
        return RxJavaPlugins.onAssembly(new ParallelPeek<>(this,
                Functions.emptyConsumer(),
                onAfterNext,
                Functions.emptyConsumer(),
                Functions.EMPTY_ACTION,
                Functions.EMPTY_ACTION,
                Functions.emptyConsumer(),
                Functions.EMPTY_LONG_CONSUMER,
                Functions.EMPTY_ACTION
                ));
    }

    /**
     * Call the specified consumer with the exception passing through any 'rail'.
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The operator is a pass-through for backpressure and the behavior
     *  is determined by the upstream and downstream rail behaviors.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code map} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param onError the callback
     * @return the new {@code ParallelFlowable} instance
     * @throws NullPointerException if {@code onError} is {@code null}
     */
    @CheckReturnValue
    @NonNull
    @BackpressureSupport(BackpressureKind.PASS_THROUGH)
    @SchedulerSupport(SchedulerSupport.NONE)
    public final ParallelFlowable<T> doOnError(@NonNull Consumer<? super Throwable> onError) {
        Objects.requireNonNull(onError, "onError is null");
        return RxJavaPlugins.onAssembly(new ParallelPeek<>(this,
                Functions.emptyConsumer(),
                Functions.emptyConsumer(),
                onError,
                Functions.EMPTY_ACTION,
                Functions.EMPTY_ACTION,
                Functions.emptyConsumer(),
                Functions.EMPTY_LONG_CONSUMER,
                Functions.EMPTY_ACTION
                ));
    }

    /**
     * Run the specified {@link Action} when a 'rail' completes.
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The operator is a pass-through for backpressure and the behavior
     *  is determined by the upstream and downstream rail behaviors.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code map} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param onComplete the callback
     * @return the new {@code ParallelFlowable} instance
     * @throws NullPointerException if {@code onComplete} is {@code null}
     */
    @CheckReturnValue
    @NonNull
    @BackpressureSupport(BackpressureKind.PASS_THROUGH)
    @SchedulerSupport(SchedulerSupport.NONE)
    public final ParallelFlowable<T> doOnComplete(@NonNull Action onComplete) {
        Objects.requireNonNull(onComplete, "onComplete is null");
        return RxJavaPlugins.onAssembly(new ParallelPeek<>(this,
                Functions.emptyConsumer(),
                Functions.emptyConsumer(),
                Functions.emptyConsumer(),
                onComplete,
                Functions.EMPTY_ACTION,
                Functions.emptyConsumer(),
                Functions.EMPTY_LONG_CONSUMER,
                Functions.EMPTY_ACTION
                ));
    }

    /**
     * Run the specified {@link Action} when a 'rail' completes or signals an error.
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The operator is a pass-through for backpressure and the behavior
     *  is determined by the upstream and downstream rail behaviors.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code map} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param onAfterTerminate the callback
     * @return the new {@code ParallelFlowable} instance
     * @throws NullPointerException if {@code onAfterTerminate} is {@code null}
     */
    @CheckReturnValue
    @NonNull
    @BackpressureSupport(BackpressureKind.PASS_THROUGH)
    @SchedulerSupport(SchedulerSupport.NONE)
    public final ParallelFlowable<T> doAfterTerminated(@NonNull Action onAfterTerminate) {
        Objects.requireNonNull(onAfterTerminate, "onAfterTerminate is null");
        return RxJavaPlugins.onAssembly(new ParallelPeek<>(this,
                Functions.emptyConsumer(),
                Functions.emptyConsumer(),
                Functions.emptyConsumer(),
                Functions.EMPTY_ACTION,
                onAfterTerminate,
                Functions.emptyConsumer(),
                Functions.EMPTY_LONG_CONSUMER,
                Functions.EMPTY_ACTION
                ));
    }

    /**
     * Call the specified callback when a 'rail' receives a {@link Subscription} from its upstream.
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The operator is a pass-through for backpressure and the behavior
     *  is determined by the upstream and downstream rail behaviors.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code map} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param onSubscribe the callback
     * @return the new {@code ParallelFlowable} instance
     * @throws NullPointerException if {@code onSubscribe} is {@code null}
     */
    @CheckReturnValue
    @NonNull
    @BackpressureSupport(BackpressureKind.PASS_THROUGH)
    @SchedulerSupport(SchedulerSupport.NONE)
    public final ParallelFlowable<T> doOnSubscribe(@NonNull Consumer<? super Subscription> onSubscribe) {
        Objects.requireNonNull(onSubscribe, "onSubscribe is null");
        return RxJavaPlugins.onAssembly(new ParallelPeek<>(this,
                Functions.emptyConsumer(),
                Functions.emptyConsumer(),
                Functions.emptyConsumer(),
                Functions.EMPTY_ACTION,
                Functions.EMPTY_ACTION,
                onSubscribe,
                Functions.EMPTY_LONG_CONSUMER,
                Functions.EMPTY_ACTION
                ));
    }

    /**
     * Call the specified consumer with the request amount if any rail receives a request.
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The operator is a pass-through for backpressure and the behavior
     *  is determined by the upstream and downstream rail behaviors.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code map} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param onRequest the callback
     * @return the new {@code ParallelFlowable} instance
     * @throws NullPointerException if {@code onRequest} is {@code null}
     */
    @CheckReturnValue
    @NonNull
    @BackpressureSupport(BackpressureKind.PASS_THROUGH)
    @SchedulerSupport(SchedulerSupport.NONE)
    public final ParallelFlowable<T> doOnRequest(@NonNull LongConsumer onRequest) {
        Objects.requireNonNull(onRequest, "onRequest is null");
        return RxJavaPlugins.onAssembly(new ParallelPeek<>(this,
                Functions.emptyConsumer(),
                Functions.emptyConsumer(),
                Functions.emptyConsumer(),
                Functions.EMPTY_ACTION,
                Functions.EMPTY_ACTION,
                Functions.emptyConsumer(),
                onRequest,
                Functions.EMPTY_ACTION
                ));
    }

    /**
     * Run the specified {@link Action} when a 'rail' receives a cancellation.
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The operator is a pass-through for backpressure and the behavior
     *  is determined by the upstream and downstream rail behaviors.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code map} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param onCancel the callback
     * @return the new {@code ParallelFlowable} instance
     * @throws NullPointerException if {@code onCancel} is {@code null}
     */
    @BackpressureSupport(BackpressureKind.PASS_THROUGH)
    @SchedulerSupport(SchedulerSupport.NONE)
    @CheckReturnValue
    @NonNull
    public final ParallelFlowable<T> doOnCancel(@NonNull Action onCancel) {
        Objects.requireNonNull(onCancel, "onCancel is null");
        return RxJavaPlugins.onAssembly(new ParallelPeek<>(this,
                Functions.emptyConsumer(),
                Functions.emptyConsumer(),
                Functions.emptyConsumer(),
                Functions.EMPTY_ACTION,
                Functions.EMPTY_ACTION,
                Functions.emptyConsumer(),
                Functions.EMPTY_LONG_CONSUMER,
                onCancel
                ));
    }

    /**
     * Collect the elements in each rail into a collection supplied via a {@code collectionSupplier}
     * and collected into with a collector action, emitting the collection at the end.
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The operator honors backpressure from the downstream rails and
     *  consumes the upstream rails in an unbounded manner (requesting {@link Long#MAX_VALUE}).</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code map} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <C> the collection type
     * @param collectionSupplier the supplier of the collection in each rail
     * @param collector the collector, taking the per-rail collection and the current item
     * @return the new {@code ParallelFlowable} instance
     * @throws NullPointerException if {@code collectionSupplier} or {@code collector} is {@code null}
     */
    @CheckReturnValue
    @NonNull
    @BackpressureSupport(BackpressureKind.UNBOUNDED_IN)
    @SchedulerSupport(SchedulerSupport.NONE)
    public final <@NonNull C> ParallelFlowable<C> collect(@NonNull Supplier<? extends C> collectionSupplier, @NonNull BiConsumer<? super C, ? super T> collector) {
        Objects.requireNonNull(collectionSupplier, "collectionSupplier is null");
        Objects.requireNonNull(collector, "collector is null");
        return RxJavaPlugins.onAssembly(new ParallelCollect<>(this, collectionSupplier, collector));
    }

    /**
     * Wraps multiple {@link Publisher}s into a {@code ParallelFlowable} which runs them
     * in parallel and unordered.
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The operator is a pass-through for backpressure and the behavior
     *  is determined by the upstream and downstream rail behaviors.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code map} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T> the value type
     * @param publishers the array of publishers
     * @return the new {@code ParallelFlowable} instance
     * @throws NullPointerException if {@code publishers} is {@code null}
     * @throws IllegalArgumentException if {@code publishers} is an empty array
     */
    @CheckReturnValue
    @NonNull
    @SafeVarargs
    @BackpressureSupport(BackpressureKind.PASS_THROUGH)
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <@NonNull T> ParallelFlowable<T> fromArray(@NonNull Publisher<T>... publishers) {
        Objects.requireNonNull(publishers, "publishers is null");
        if (publishers.length == 0) {
            throw new IllegalArgumentException("Zero publishers not supported");
        }
        return RxJavaPlugins.onAssembly(new ParallelFromArray<>(publishers));
    }

    /**
     * Calls the specified converter function during assembly time and returns its resulting value.
     * <p>
     * This allows fluent conversion to any other type.
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The operator is a pass-through for backpressure and the behavior
     *  is determined by how the converter function composes over the upstream source.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code to} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * <p>History: 2.1.7 - experimental
     * @param <R> the resulting object type
     * @param converter the function that receives the current {@code ParallelFlowable} instance and returns a value
     * @return the converted value
     * @throws NullPointerException if {@code converter} is {@code null}
     * @since 2.2
     */
    @CheckReturnValue
    @NonNull
    @BackpressureSupport(BackpressureKind.PASS_THROUGH)
    @SchedulerSupport(SchedulerSupport.NONE)
    public final <@NonNull R> R to(@NonNull ParallelFlowableConverter<T, R> converter) {
        return Objects.requireNonNull(converter, "converter is null").apply(this);
    }

    /**
     * Allows composing operators, in assembly time, on top of this {@code ParallelFlowable}
     * and returns another {@code ParallelFlowable} with composed features.
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The operator is a pass-through for backpressure and the behavior
     *  is determined by how the converter function composes over the upstream source.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code compose} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <U> the output value type
     * @param composer the composer function from {@code ParallelFlowable} (this) to another {@code ParallelFlowable}
     * @return the {@code ParallelFlowable} returned by the function
     * @throws NullPointerException if {@code composer} is {@code null}
     */
    @CheckReturnValue
    @NonNull
    @BackpressureSupport(BackpressureKind.PASS_THROUGH)
    @SchedulerSupport(SchedulerSupport.NONE)
    public final <@NonNull U> ParallelFlowable<U> compose(@NonNull ParallelTransformer<T, U> composer) {
        return RxJavaPlugins.onAssembly(Objects.requireNonNull(composer, "composer is null").apply(this));
    }

    /**
     * Generates and flattens {@link Publisher}s on each 'rail'.
     * <p>
     * The errors are not delayed and uses unbounded concurrency along with default inner prefetch.
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The operator honors backpressure from the downstream rails and
     *  requests {@link Flowable#bufferSize()} amount from each rail upfront
     *  and keeps requesting as many items per rail as many inner sources on
     *  that rail completed. The inner sources are requested {@link Flowable#bufferSize()}
     *  amount upfront, then 75% of this amount requested after 75% received.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code flatMap} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <R> the result type
     * @param mapper the function to map each rail's value into a {@code Publisher}
     * @return the new {@code ParallelFlowable} instance
     * @throws NullPointerException if {@code mapper} is {@code null}
     */
    @CheckReturnValue
    @NonNull
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerSupport.NONE)
    public final <@NonNull R> ParallelFlowable<R> flatMap(@NonNull Function<? super T, @NonNull ? extends Publisher<? extends R>> mapper) {
        return flatMap(mapper, false, Flowable.bufferSize(), Flowable.bufferSize());
    }

    /**
     * Generates and flattens {@link Publisher}s on each 'rail', optionally delaying errors.
     * <p>
     * It uses unbounded concurrency along with default inner prefetch.
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The operator honors backpressure from the downstream rails and
     *  requests {@link Flowable#bufferSize()} amount from each rail upfront
     *  and keeps requesting as many items per rail as many inner sources on
     *  that rail completed. The inner sources are requested {@link Flowable#bufferSize()}
     *  amount upfront, then 75% of this amount requested after 75% received.
     *  </dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code flatMap} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <R> the result type
     * @param mapper the function to map each rail's value into a {@code Publisher}
     * @param delayError should the errors from the main and the inner sources delayed till everybody terminates?
     * @return the new {@code ParallelFlowable} instance
     * @throws NullPointerException if {@code mapper} is {@code null}
     */
    @CheckReturnValue
    @NonNull
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerSupport.NONE)
    public final <@NonNull R> ParallelFlowable<R> flatMap(
            @NonNull Function<? super T, @NonNull ? extends Publisher<? extends R>> mapper, boolean delayError) {
        return flatMap(mapper, delayError, Flowable.bufferSize(), Flowable.bufferSize());
    }

    /**
     * Generates and flattens {@link Publisher}s on each 'rail', optionally delaying errors
     * and having a total number of simultaneous subscriptions to the inner {@code Publisher}s.
     * <p>
     * It uses a default inner prefetch.
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The operator honors backpressure from the downstream rails and
     *  requests {@code maxConcurrency} amount from each rail upfront
     *  and keeps requesting as many items per rail as many inner sources on
     *  that rail completed. The inner sources are requested {@link Flowable#bufferSize()}
     *  amount upfront, then 75% of this amount requested after 75% received.
     *  </dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code flatMap} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <R> the result type
     * @param mapper the function to map each rail's value into a {@code Publisher}
     * @param delayError should the errors from the main and the inner sources delayed till everybody terminates?
     * @param maxConcurrency the maximum number of simultaneous subscriptions to the generated inner {@code Publisher}s
     * @return the new {@code ParallelFlowable} instance
     * @throws NullPointerException if {@code mapper} is {@code null}
     * @throws IllegalArgumentException if {@code maxConcurrency} is non-positive
     */
    @CheckReturnValue
    @NonNull
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerSupport.NONE)
    public final <@NonNull R> ParallelFlowable<R> flatMap(
            @NonNull Function<? super T, @NonNull ? extends Publisher<? extends R>> mapper, boolean delayError, int maxConcurrency) {
        return flatMap(mapper, delayError, maxConcurrency, Flowable.bufferSize());
    }

    /**
     * Generates and flattens {@link Publisher}s on each 'rail', optionally delaying errors,
     * having a total number of simultaneous subscriptions to the inner {@code Publisher}s
     * and using the given prefetch amount for the inner {@code Publisher}s.
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The operator honors backpressure from the downstream rails and
     *  requests {@code maxConcurrency} amount from each rail upfront
     *  and keeps requesting as many items per rail as many inner sources on
     *  that rail completed. The inner sources are requested the {@code prefetch}
     *  amount upfront, then 75% of this amount requested after 75% received.
     *  </dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code flatMap} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <R> the result type
     * @param mapper the function to map each rail's value into a {@code Publisher}
     * @param delayError should the errors from the main and the inner sources delayed till everybody terminates?
     * @param maxConcurrency the maximum number of simultaneous subscriptions to the generated inner {@code Publisher}s
     * @param prefetch the number of items to prefetch from each inner {@code Publisher}
     * @return the new {@code ParallelFlowable} instance
     * @throws NullPointerException if {@code mapper} is {@code null}
     * @throws IllegalArgumentException if {@code maxConcurrency} or {@code prefetch} is non-positive
     */
    @CheckReturnValue
    @NonNull
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerSupport.NONE)
    public final <@NonNull R> ParallelFlowable<R> flatMap(
            @NonNull Function<? super T, @NonNull ? extends Publisher<? extends R>> mapper,
            boolean delayError, int maxConcurrency, int prefetch) {
        Objects.requireNonNull(mapper, "mapper is null");
        ObjectHelper.verifyPositive(maxConcurrency, "maxConcurrency");
        ObjectHelper.verifyPositive(prefetch, "prefetch");
        return RxJavaPlugins.onAssembly(new ParallelFlatMap<>(this, mapper, delayError, maxConcurrency, prefetch));
    }

    /**
     * Generates and concatenates {@link Publisher}s on each 'rail', signalling errors immediately
     * and generating 2 publishers upfront.
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The operator honors backpressure from the downstream rails and
     *  requests 2 from each rail upfront and keeps requesting 1 when the inner source complete.
     *  Requests for the inner sources are determined by the downstream rails'
     *  backpressure behavior.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code concatMap} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <R> the result type
     * @param mapper the function to map each rail's value into a {@code Publisher}
     * source and the inner {@code Publisher}s (immediate, boundary, end)
     * @return the new {@code ParallelFlowable} instance
     * @throws NullPointerException if {@code mapper} is {@code null}
     */
    @CheckReturnValue
    @NonNull
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerSupport.NONE)
    public final <@NonNull R> ParallelFlowable<R> concatMap(
            @NonNull Function<? super T, @NonNull ? extends Publisher<? extends R>> mapper) {
        return concatMap(mapper, 2);
    }

    /**
     * Generates and concatenates {@link Publisher}s on each 'rail', signalling errors immediately
     * and using the given prefetch amount for generating {@code Publisher}s upfront.
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The operator honors backpressure from the downstream rails and
     *  requests the {@code prefetch} amount from each rail upfront and keeps
     *  requesting 75% of this amount after 75% received and the inner sources completed.
     *  Requests for the inner sources are determined by the downstream rails'
     *  backpressure behavior.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code concatMap} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <R> the result type
     * @param mapper the function to map each rail's value into a {@code Publisher}
     * @param prefetch the number of items to prefetch from each inner {@code Publisher}
     * source and the inner {@code Publisher}s (immediate, boundary, end)
     * @return the new {@code ParallelFlowable} instance
     * @throws NullPointerException if {@code mapper} is {@code null}
     * @throws IllegalArgumentException if {@code prefetch} is non-positive
     */
    @CheckReturnValue
    @NonNull
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerSupport.NONE)
    public final <@NonNull R> ParallelFlowable<R> concatMap(
            @NonNull Function<? super T, @NonNull ? extends Publisher<? extends R>> mapper,
            int prefetch) {
        Objects.requireNonNull(mapper, "mapper is null");
        ObjectHelper.verifyPositive(prefetch, "prefetch");
        return RxJavaPlugins.onAssembly(new ParallelConcatMap<>(this, mapper, prefetch, ErrorMode.IMMEDIATE));
    }

    /**
     * Generates and concatenates {@link Publisher}s on each 'rail', optionally delaying errors
     * and generating 2 publishers upfront.
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The operator honors backpressure from the downstream rails and
     *  requests 2 from each rail upfront and keeps requesting 1 when the inner source complete.
     *  Requests for the inner sources are determined by the downstream rails'
     *  backpressure behavior.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code concatMap} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <R> the result type
     * @param mapper the function to map each rail's value into a {@code Publisher}
     * @param tillTheEnd if {@code true}, all errors from the upstream and inner {@code Publisher}s are delayed
     * till all of them terminate, if {@code false}, the error is emitted when an inner {@code Publisher} terminates.
     * source and the inner {@code Publisher}s (immediate, boundary, end)
     * @return the new {@code ParallelFlowable} instance
     * @throws NullPointerException if {@code mapper} is {@code null}
     */
    @CheckReturnValue
    @NonNull
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerSupport.NONE)
    public final <@NonNull R> ParallelFlowable<R> concatMapDelayError(
            @NonNull Function<? super T, @NonNull ? extends Publisher<? extends R>> mapper,
                    boolean tillTheEnd) {
        return concatMapDelayError(mapper, 2, tillTheEnd);
    }

    /**
     * Generates and concatenates {@link Publisher}s on each 'rail', optionally delaying errors
     * and using the given prefetch amount for generating {@code Publisher}s upfront.
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The operator honors backpressure from the downstream rails and
     *  requests the {@code prefetch} amount from each rail upfront and keeps
     *  requesting 75% of this amount after 75% received and the inner sources completed.
     *  Requests for the inner sources are determined by the downstream rails'
     *  backpressure behavior.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code concatMap} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <R> the result type
     * @param mapper the function to map each rail's value into a {@code Publisher}
     * @param prefetch the number of items to prefetch from each inner {@code Publisher}
     * @param tillTheEnd if {@code true}, all errors from the upstream and inner {@code Publisher}s are delayed
     * till all of them terminate, if {@code false}, the error is emitted when an inner {@code Publisher} terminates.
     * @return the new {@code ParallelFlowable} instance
     * @throws NullPointerException if {@code mapper} is {@code null}
     * @throws IllegalArgumentException if {@code prefetch} is non-positive
     */
    @CheckReturnValue
    @NonNull
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerSupport.NONE)
    public final <@NonNull R> ParallelFlowable<R> concatMapDelayError(
            @NonNull Function<? super T, @NonNull ? extends Publisher<? extends R>> mapper,
                    int prefetch, boolean tillTheEnd) {
        Objects.requireNonNull(mapper, "mapper is null");
        ObjectHelper.verifyPositive(prefetch, "prefetch");
        return RxJavaPlugins.onAssembly(new ParallelConcatMap<>(
                this, mapper, prefetch, tillTheEnd ? ErrorMode.END : ErrorMode.BOUNDARY));
    }

    /**
     * Returns a {@code ParallelFlowable} that merges each item emitted by the source on each rail with the values in an
     * {@link Iterable} corresponding to that item that is generated by a selector.
     * <p>
     * <img width="640" height="342" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/flatMapIterable.f.png" alt="">
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The operator honors backpressure from each downstream rail. The source {@code ParallelFlowable}s is
     *  expected to honor backpressure as well. If the source {@code ParallelFlowable} violates the rule, the operator will
     *  signal a {@link MissingBackpressureException}.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code flatMapIterable} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <U>
     *            the type of item emitted by the resulting {@code Iterable}
     * @param mapper
     *            a function that returns an {@code Iterable} sequence of values for when given an item emitted by the
     *            source {@code ParallelFlowable}
     * @return the new {@code ParallelFlowable} instance
     * @see <a href="http://reactivex.io/documentation/operators/flatmap.html">ReactiveX operators documentation: FlatMap</a>
     * @see #flatMapStream(Function)
     * @since 3.0.0
     * @throws NullPointerException if {@code mapper} is {@code null}
     */
    @CheckReturnValue
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <@NonNull U> ParallelFlowable<U> flatMapIterable(@NonNull Function<? super T, @NonNull ? extends Iterable<? extends U>> mapper) {
        return flatMapIterable(mapper, Flowable.bufferSize());
    }

    /**
     * Returns a {@code ParallelFlowable} that merges each item emitted by the source {@code ParallelFlowable} with the values in an
     * {@link Iterable} corresponding to that item that is generated by a selector.
     * <p>
     * <img width="640" height="342" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/flatMapIterable.f.png" alt="">
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The operator honors backpressure from each downstream rail. The source {@code ParallelFlowable}s is
     *  expected to honor backpressure as well. If the source {@code ParallelFlowable} violates the rule, the operator will
     *  signal a {@link MissingBackpressureException}.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code flatMapIterable} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <U>
     *            the type of item emitted by the resulting {@code Iterable}
     * @param mapper
     *            a function that returns an {@code Iterable} sequence of values for when given an item emitted by the
     *            source {@code ParallelFlowable}
     * @param bufferSize
     *            the number of elements to prefetch from each upstream rail
     * @return the new {@code ParallelFlowable} instance
     * @see <a href="http://reactivex.io/documentation/operators/flatmap.html">ReactiveX operators documentation: FlatMap</a>
     * @see #flatMapStream(Function, int)
     * @since 3.0.0
     * @throws NullPointerException if {@code mapper} is {@code null}
     * @throws IllegalArgumentException if {@code bufferSize} is non-positive
     */
    @CheckReturnValue
    @NonNull
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerSupport.NONE)
    public final <@NonNull U> ParallelFlowable<U> flatMapIterable(@NonNull Function<? super T, @NonNull ? extends Iterable<? extends U>> mapper, int bufferSize) {
        Objects.requireNonNull(mapper, "mapper is null");
        ObjectHelper.verifyPositive(bufferSize, "bufferSize");
        return RxJavaPlugins.onAssembly(new ParallelFlatMapIterable<>(this, mapper, bufferSize));
    }

    // -------------------------------------------------------------------------
    // JDK 8 Support
    // -------------------------------------------------------------------------

    /**
     * Maps the source values on each 'rail' to an optional and emits its value if any.
     * <p>
     * Note that the same mapper function may be called from multiple threads concurrently.
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The operator is a pass-through for backpressure and the behavior
     *  is determined by the upstream and downstream rail behaviors.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code map} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <R> the output value type
     * @param mapper the mapper function turning Ts into optional of Rs.
     * @return the new {@code ParallelFlowable} instance
     * @since 3.0.0
     * @throws NullPointerException if {@code mapper} is {@code null}
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    @BackpressureSupport(BackpressureKind.PASS_THROUGH)
    public final <@NonNull R> ParallelFlowable<R> mapOptional(@NonNull Function<? super T, @NonNull Optional<? extends R>> mapper) {
        Objects.requireNonNull(mapper, "mapper is null");
        return RxJavaPlugins.onAssembly(new ParallelMapOptional<>(this, mapper));
    }

    /**
     * Maps the source values on each 'rail' to an optional and emits its value if any and
     * handles errors based on the given {@link ParallelFailureHandling} enumeration value.
     * <p>
     * Note that the same mapper function may be called from multiple threads concurrently.
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The operator is a pass-through for backpressure and the behavior
     *  is determined by the upstream and downstream rail behaviors.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code map} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * <p>History: 2.0.8 - experimental
     * @param <R> the output value type
     * @param mapper the mapper function turning Ts into optional of Rs.
     * @param errorHandler the enumeration that defines how to handle errors thrown
     *                     from the mapper function
     * @return the new {@code ParallelFlowable} instance
     * @since 3.0.0
     * @throws NullPointerException if {@code mapper} or {@code errorHandler} is {@code null}
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    @BackpressureSupport(BackpressureKind.PASS_THROUGH)
    public final <@NonNull R> ParallelFlowable<R> mapOptional(@NonNull Function<? super T, @NonNull Optional<? extends R>> mapper, @NonNull ParallelFailureHandling errorHandler) {
        Objects.requireNonNull(mapper, "mapper is null");
        Objects.requireNonNull(errorHandler, "errorHandler is null");
        return RxJavaPlugins.onAssembly(new ParallelMapTryOptional<>(this, mapper, errorHandler));
    }

    /**
     * Maps the source values on each 'rail' to an optional and emits its value if any and
     * handles errors based on the returned value by the handler function.
     * <p>
     * Note that the same mapper function may be called from multiple threads concurrently.
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The operator is a pass-through for backpressure and the behavior
     *  is determined by the upstream and downstream rail behaviors.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code map} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * <p>History: 2.0.8 - experimental
     * @param <R> the output value type
     * @param mapper the mapper function turning Ts into optional of Rs.
     * @param errorHandler the function called with the current repeat count and
     *                     failure {@link Throwable} and should return one of the {@link ParallelFailureHandling}
     *                     enumeration values to indicate how to proceed.
     * @return the new {@code ParallelFlowable} instance
     * @throws NullPointerException if {@code mapper} or {@code errorHandler} is {@code null}
     * @since 3.0.0
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    @BackpressureSupport(BackpressureKind.PASS_THROUGH)
    public final <@NonNull R> ParallelFlowable<R> mapOptional(@NonNull Function<? super T, @NonNull Optional<? extends R>> mapper, @NonNull BiFunction<? super Long, ? super Throwable, ParallelFailureHandling> errorHandler) {
        Objects.requireNonNull(mapper, "mapper is null");
        Objects.requireNonNull(errorHandler, "errorHandler is null");
        return RxJavaPlugins.onAssembly(new ParallelMapTryOptional<>(this, mapper, errorHandler));
    }

    /**
     * Maps each upstream item on each rail into a {@link Stream} and emits the {@code Stream}'s items to the downstream in a sequential fashion.
     * <p>
     * <img width="640" height="328" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/flatMapStream.f.png" alt="">
     * <p>
     * Due to the blocking and sequential nature of Java {@code Stream}s, the streams are mapped and consumed in a sequential fashion
     * without interleaving (unlike a more general {@link #flatMap(Function)}). Therefore, {@code flatMapStream} and
     * {@code concatMapStream} are identical operators and are provided as aliases.
     * <p>
     * The operator closes the {@code Stream} upon cancellation and when it terminates. The exceptions raised when
     * closing a {@code Stream} are routed to the global error handler ({@link RxJavaPlugins#onError(Throwable)}.
     * If a {@code Stream} should not be closed, turn it into an {@link Iterable} and use {@link #flatMapIterable(Function)}:
     * <pre><code>
     * source.flatMapIterable(v -&gt; createStream(v)::iterator);
     * </code></pre>
     * <p>
     * Note that {@code Stream}s can be consumed only once; any subsequent attempt to consume a {@code Stream}
     * will result in an {@link IllegalStateException}.
     * <p>
     * Primitive streams are not supported and items have to be boxed manually (e.g., via {@link IntStream#boxed()}):
     * <pre><code>
     * source.flatMapStream(v -&gt; IntStream.rangeClosed(v + 1, v + 10).boxed());
     * </code></pre>
     * <p>
     * {@code Stream} does not support concurrent usage so creating and/or consuming the same instance multiple times
     * from multiple threads can lead to undefined behavior.
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The operator honors the downstream backpressure and consumes the inner stream only on demand. The operator
     *  prefetches {@link Flowable#bufferSize()} items of the upstream (then 75% of it after the 75% received)
     *  and caches them until they are ready to be mapped into {@code Stream}s
     *  after the current {@code Stream} has been consumed.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code flatMapStream} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <R> the element type of the {@code Stream}s and the result
     * @param mapper the function that receives an upstream item and should return a {@code Stream} whose elements
     * will be emitted to the downstream
     * @return the new {@code ParallelFlowable} instance
     * @throws NullPointerException if {@code mapper} is {@code null}
     * @see #flatMap(Function)
     * @see #flatMapIterable(Function)
     * @see #flatMapStream(Function, int)
     * @since 3.0.0
     */
    @CheckReturnValue
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <@NonNull R> ParallelFlowable<R> flatMapStream(@NonNull Function<? super T, @NonNull ? extends Stream<? extends R>> mapper) {
        return flatMapStream(mapper, Flowable.bufferSize());
    }

    /**
     * Maps each upstream item of each rail into a {@link Stream} and emits the {@code Stream}'s items to the downstream in a sequential fashion.
     * <p>
     * <img width="640" height="270" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/flatMapStream.fi.png" alt="">
     * <p>
     * Due to the blocking and sequential nature of Java {@code Stream}s, the streams are mapped and consumed in a sequential fashion
     * without interleaving (unlike a more general {@link #flatMap(Function)}). Therefore, {@code flatMapStream} and
     * {@code concatMapStream} are identical operators and are provided as aliases.
     * <p>
     * The operator closes the {@code Stream} upon cancellation and when it terminates. The exceptions raised when
     * closing a {@code Stream} are routed to the global error handler ({@link RxJavaPlugins#onError(Throwable)}.
     * If a {@code Stream} should not be closed, turn it into an {@link Iterable} and use {@link #flatMapIterable(Function, int)}:
     * <pre><code>
     * source.flatMapIterable(v -&gt; createStream(v)::iterator, 32);
     * </code></pre>
     * <p>
     * Note that {@code Stream}s can be consumed only once; any subsequent attempt to consume a {@code Stream}
     * will result in an {@link IllegalStateException}.
     * <p>
     * Primitive streams are not supported and items have to be boxed manually (e.g., via {@link IntStream#boxed()}):
     * <pre><code>
     * source.flatMapStream(v -&gt; IntStream.rangeClosed(v + 1, v + 10).boxed(), 32);
     * </code></pre>
     * <p>
     * {@code Stream} does not support concurrent usage so creating and/or consuming the same instance multiple times
     * from multiple threads can lead to undefined behavior.
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The operator honors the downstream backpressure and consumes the inner stream only on demand. The operator
     *  prefetches the given amount of upstream items and caches them until they are ready to be mapped into {@code Stream}s
     *  after the current {@code Stream} has been consumed.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code flatMapStream} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <R> the element type of the {@code Stream}s and the result
     * @param mapper the function that receives an upstream item and should return a {@code Stream} whose elements
     * will be emitted to the downstream
     * @param prefetch the number of upstream items to request upfront, then 75% of this amount after each 75% upstream items received
     * @return the new {@code ParallelFlowable} instance
     * @throws NullPointerException if {@code mapper} is {@code null}
     * @throws IllegalArgumentException if {@code prefetch} is non-positive
     * @see #flatMap(Function, boolean, int)
     * @see #flatMapIterable(Function, int)
     * @since 3.0.0
     */
    @CheckReturnValue
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <@NonNull R> ParallelFlowable<R> flatMapStream(@NonNull Function<? super T, @NonNull ? extends Stream<? extends R>> mapper, int prefetch) {
        Objects.requireNonNull(mapper, "mapper is null");
        ObjectHelper.verifyPositive(prefetch, "prefetch");
        return RxJavaPlugins.onAssembly(new ParallelFlatMapStream<>(this, mapper, prefetch));
    }

    /**
     * Reduces all values within a 'rail' and across 'rails' with a callbacks
     * of the given {@link Collector} into one {@link Flowable} containing a single value.
     * <p>
     * Each parallel rail receives its own {@link Collector#accumulator()} and
     * {@link Collector#combiner()}.
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The operator honors backpressure from the downstream and consumes
     *  the upstream rails in an unbounded manner (requesting {@link Long#MAX_VALUE}).</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code collect} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <A> the accumulator type
     * @param <R> the output value type
     * @param collector the {@code Collector} instance
     * @return the new {@code Flowable} instance emitting the collected value.
     * @throws NullPointerException if {@code collector} is {@code null}
     * @since 3.0.0
     */
    @CheckReturnValue
    @NonNull
    @BackpressureSupport(BackpressureKind.UNBOUNDED_IN)
    @SchedulerSupport(SchedulerSupport.NONE)
    public final <@NonNull A, @NonNull R> Flowable<R> collect(@NonNull Collector<T, A, R> collector) {
        Objects.requireNonNull(collector, "collector is null");
        return RxJavaPlugins.onAssembly(new ParallelCollector<>(this, collector));
    }
}
