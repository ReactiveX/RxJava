/**
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

package io.reactivex.parallel;

import java.util.*;
import java.util.concurrent.Callable;

import io.reactivex.*;
import io.reactivex.annotations.*;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.functions.*;
import io.reactivex.internal.functions.*;
import io.reactivex.internal.operators.parallel.*;
import io.reactivex.internal.subscriptions.EmptySubscription;
import io.reactivex.internal.util.*;
import io.reactivex.plugins.RxJavaPlugins;
import org.reactivestreams.*;

/**
 * Abstract base class for Parallel publishers that take an array of Subscribers.
 * <p>
 * Use {@code from()} to start processing a regular Publisher in 'rails'.
 * Use {@code runOn()} to introduce where each 'rail' should run on thread-vise.
 * Use {@code sequential()} to merge the sources back into a single Flowable.
 *
 * <p>History: 2.0.5 - experimental
 * @param <T> the value type
 * @since 2.1 - beta
 */
@Beta
public abstract class ParallelFlowable<T> {

    /**
     * Subscribes an array of Subscribers to this ParallelFlowable and triggers
     * the execution chain for all 'rails'.
     *
     * @param subscribers the subscribers array to run in parallel, the number
     * of items must be equal to the parallelism level of this ParallelFlowable
     * @see #parallelism()
     */
    public abstract void subscribe(@NonNull Subscriber<? super T>[] subscribers);

    /**
     * Returns the number of expected parallel Subscribers.
     * @return the number of expected parallel Subscribers
     */
    public abstract int parallelism();

    /**
     * Validates the number of subscribers and returns true if their number
     * matches the parallelism level of this ParallelFlowable.
     *
     * @param subscribers the array of Subscribers
     * @return true if the number of subscribers equals to the parallelism level
     */
    protected final boolean validate(@NonNull Subscriber<?>[] subscribers) {
        int p = parallelism();
        if (subscribers.length != p) {
            Throwable iae = new IllegalArgumentException("parallelism = " + p + ", subscribers = " + subscribers.length);
            for (Subscriber<?> s : subscribers) {
                EmptySubscription.error(iae, s);
            }
            return false;
        }
        return true;
    }

    /**
     * Take a Publisher and prepare to consume it on multiple 'rails' (number of CPUs)
     * in a round-robin fashion.
     * @param <T> the value type
     * @param source the source Publisher
     * @return the ParallelFlowable instance
     */
    @CheckReturnValue
    public static <T> ParallelFlowable<T> from(@NonNull Publisher<? extends T> source) {
        return from(source, Runtime.getRuntime().availableProcessors(), Flowable.bufferSize());
    }

    /**
     * Take a Publisher and prepare to consume it on parallelism number of 'rails' in a round-robin fashion.
     * @param <T> the value type
     * @param source the source Publisher
     * @param parallelism the number of parallel rails
     * @return the new ParallelFlowable instance
     */
    @CheckReturnValue
    public static <T> ParallelFlowable<T> from(@NonNull Publisher<? extends T> source, int parallelism) {
        return from(source, parallelism, Flowable.bufferSize());
    }

    /**
     * Take a Publisher and prepare to consume it on parallelism number of 'rails' ,
     * possibly ordered and round-robin fashion and use custom prefetch amount and queue
     * for dealing with the source Publisher's values.
     * @param <T> the value type
     * @param source the source Publisher
     * @param parallelism the number of parallel rails
     * @param prefetch the number of values to prefetch from the source
     * the source until there is a rail ready to process it.
     * @return the new ParallelFlowable instance
     */
    @CheckReturnValue
    @NonNull
    public static <T> ParallelFlowable<T> from(@NonNull Publisher<? extends T> source,
            int parallelism, int prefetch) {
        ObjectHelper.requireNonNull(source, "source");
        ObjectHelper.verifyPositive(parallelism, "parallelism");
        ObjectHelper.verifyPositive(prefetch, "prefetch");

        return RxJavaPlugins.onAssembly(new ParallelFromPublisher<T>(source, parallelism, prefetch));
    }

    /**
     * Maps the source values on each 'rail' to another value.
     * <p>
     * Note that the same mapper function may be called from multiple threads concurrently.
     * @param <R> the output value type
     * @param mapper the mapper function turning Ts into Us.
     * @return the new ParallelFlowable instance
     */
    @CheckReturnValue
    @NonNull
    public final <R> ParallelFlowable<R> map(@NonNull Function<? super T, ? extends R> mapper) {
        ObjectHelper.requireNonNull(mapper, "mapper");
        return RxJavaPlugins.onAssembly(new ParallelMap<T, R>(this, mapper));
    }

    /**
     * Maps the source values on each 'rail' to another value and
     * handles errors based on the given {@link ParallelFailureHandling} enumeration value.
     * <p>
     * Note that the same mapper function may be called from multiple threads concurrently.
     * @param <R> the output value type
     * @param mapper the mapper function turning Ts into Us.
     * @param errorHandler the enumeration that defines how to handle errors thrown
     *                     from the mapper function
     * @return the new ParallelFlowable instance
     * @since 2.0.8 - experimental
     */
    @CheckReturnValue
    @Experimental
    @NonNull
    public final <R> ParallelFlowable<R> map(@NonNull Function<? super T, ? extends R> mapper, @NonNull ParallelFailureHandling errorHandler) {
        ObjectHelper.requireNonNull(mapper, "mapper");
        ObjectHelper.requireNonNull(errorHandler, "errorHandler is null");
        return RxJavaPlugins.onAssembly(new ParallelMapTry<T, R>(this, mapper, errorHandler));
    }

    /**
     * Maps the source values on each 'rail' to another value and
     * handles errors based on the returned value by the handler function.
     * <p>
     * Note that the same mapper function may be called from multiple threads concurrently.
     * @param <R> the output value type
     * @param mapper the mapper function turning Ts into Us.
     * @param errorHandler the function called with the current repeat count and
     *                     failure Throwable and should return one of the {@link ParallelFailureHandling}
     *                     enumeration values to indicate how to proceed.
     * @return the new ParallelFlowable instance
     * @since 2.0.8 - experimental
     */
    @CheckReturnValue
    @Experimental
    @NonNull
    public final <R> ParallelFlowable<R> map(@NonNull Function<? super T, ? extends R> mapper, @NonNull BiFunction<? super Long, ? super Throwable, ParallelFailureHandling> errorHandler) {
        ObjectHelper.requireNonNull(mapper, "mapper");
        ObjectHelper.requireNonNull(errorHandler, "errorHandler is null");
        return RxJavaPlugins.onAssembly(new ParallelMapTry<T, R>(this, mapper, errorHandler));
    }

    /**
     * Filters the source values on each 'rail'.
     * <p>
     * Note that the same predicate may be called from multiple threads concurrently.
     * @param predicate the function returning true to keep a value or false to drop a value
     * @return the new ParallelFlowable instance
     */
    @CheckReturnValue
    public final ParallelFlowable<T> filter(@NonNull Predicate<? super T> predicate) {
        ObjectHelper.requireNonNull(predicate, "predicate");
        return RxJavaPlugins.onAssembly(new ParallelFilter<T>(this, predicate));
    }

    /**
     * Filters the source values on each 'rail' and
     * handles errors based on the given {@link ParallelFailureHandling} enumeration value.
     * <p>
     * Note that the same predicate may be called from multiple threads concurrently.
     * @param predicate the function returning true to keep a value or false to drop a value
     * @param errorHandler the enumeration that defines how to handle errors thrown
     *                     from the predicate
     * @return the new ParallelFlowable instance
     * @since 2.0.8 - experimental
     */
    @CheckReturnValue
    @Experimental
    public final ParallelFlowable<T> filter(@NonNull Predicate<? super T> predicate, @NonNull ParallelFailureHandling errorHandler) {
        ObjectHelper.requireNonNull(predicate, "predicate");
        ObjectHelper.requireNonNull(errorHandler, "errorHandler is null");
        return RxJavaPlugins.onAssembly(new ParallelFilterTry<T>(this, predicate, errorHandler));
    }


    /**
     * Filters the source values on each 'rail' and
     * handles errors based on the returned value by the handler function.
     * <p>
     * Note that the same predicate may be called from multiple threads concurrently.
     * @param predicate the function returning true to keep a value or false to drop a value
     * @param errorHandler the function called with the current repeat count and
     *                     failure Throwable and should return one of the {@link ParallelFailureHandling}
     *                     enumeration values to indicate how to proceed.
     * @return the new ParallelFlowable instance
     * @since 2.0.8 - experimental
     */
    @CheckReturnValue
    @Experimental
    public final ParallelFlowable<T> filter(@NonNull Predicate<? super T> predicate, @NonNull BiFunction<? super Long, ? super Throwable, ParallelFailureHandling> errorHandler) {
        ObjectHelper.requireNonNull(predicate, "predicate");
        ObjectHelper.requireNonNull(errorHandler, "errorHandler is null");
        return RxJavaPlugins.onAssembly(new ParallelFilterTry<T>(this, predicate, errorHandler));
    }

    /**
     * Specifies where each 'rail' will observe its incoming values with
     * no work-stealing and default prefetch amount.
     * <p>
     * This operator uses the default prefetch size returned by {@code Flowable.bufferSize()}.
     * <p>
     * The operator will call {@code Scheduler.createWorker()} as many
     * times as this ParallelFlowable's parallelism level is.
     * <p>
     * No assumptions are made about the Scheduler's parallelism level,
     * if the Scheduler's parallelism level is lower than the ParallelFlowable's,
     * some rails may end up on the same thread/worker.
     * <p>
     * This operator doesn't require the Scheduler to be trampolining as it
     * does its own built-in trampolining logic.
     *
     * @param scheduler the scheduler to use
     * @return the new ParallelFlowable instance
     */
    @CheckReturnValue
    @NonNull
    public final ParallelFlowable<T> runOn(@NonNull Scheduler scheduler) {
        return runOn(scheduler, Flowable.bufferSize());
    }

    /**
     * Specifies where each 'rail' will observe its incoming values with
     * possibly work-stealing and a given prefetch amount.
     * <p>
     * This operator uses the default prefetch size returned by {@code Flowable.bufferSize()}.
     * <p>
     * The operator will call {@code Scheduler.createWorker()} as many
     * times as this ParallelFlowable's parallelism level is.
     * <p>
     * No assumptions are made about the Scheduler's parallelism level,
     * if the Scheduler's parallelism level is lower than the ParallelFlowable's,
     * some rails may end up on the same thread/worker.
     * <p>
     * This operator doesn't require the Scheduler to be trampolining as it
     * does its own built-in trampolining logic.
     *
     * @param scheduler the scheduler to use
     * that rail's worker has run out of work.
     * @param prefetch the number of values to request on each 'rail' from the source
     * @return the new ParallelFlowable instance
     */
    @CheckReturnValue
    @NonNull
    public final ParallelFlowable<T> runOn(@NonNull Scheduler scheduler, int prefetch) {
        ObjectHelper.requireNonNull(scheduler, "scheduler");
        ObjectHelper.verifyPositive(prefetch, "prefetch");
        return RxJavaPlugins.onAssembly(new ParallelRunOn<T>(this, scheduler, prefetch));
    }

    /**
     * Reduces all values within a 'rail' and across 'rails' with a reducer function into a single
     * sequential value.
     * <p>
     * Note that the same reducer function may be called from multiple threads concurrently.
     * @param reducer the function to reduce two values into one.
     * @return the new Flowable instance emitting the reduced value or empty if the ParallelFlowable was empty
     */
    @CheckReturnValue
    @NonNull
    public final Flowable<T> reduce(@NonNull BiFunction<T, T, T> reducer) {
        ObjectHelper.requireNonNull(reducer, "reducer");
        return RxJavaPlugins.onAssembly(new ParallelReduceFull<T>(this, reducer));
    }

    /**
     * Reduces all values within a 'rail' to a single value (with a possibly different type) via
     * a reducer function that is initialized on each rail from an initialSupplier value.
     * <p>
     * Note that the same mapper function may be called from multiple threads concurrently.
     * @param <R> the reduced output type
     * @param initialSupplier the supplier for the initial value
     * @param reducer the function to reduce a previous output of reduce (or the initial value supplied)
     * with a current source value.
     * @return the new ParallelFlowable instance
     */
    @CheckReturnValue
    @NonNull
    public final <R> ParallelFlowable<R> reduce(@NonNull Callable<R> initialSupplier, @NonNull BiFunction<R, ? super T, R> reducer) {
        ObjectHelper.requireNonNull(initialSupplier, "initialSupplier");
        ObjectHelper.requireNonNull(reducer, "reducer");
        return RxJavaPlugins.onAssembly(new ParallelReduce<T, R>(this, initialSupplier, reducer));
    }

    /**
     * Merges the values from each 'rail' in a round-robin or same-order fashion and
     * exposes it as a regular Publisher sequence, running with a default prefetch value
     * for the rails.
     * <p>
     * This operator uses the default prefetch size returned by {@code Flowable.bufferSize()}.
     * <img width="640" height="602" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/parallelflowable.sequential.png" alt="">
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The operator honors backpressure.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code sequential} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @return the new Flowable instance
     * @see ParallelFlowable#sequential(int)
     * @see ParallelFlowable#sequentialDelayError()
     */
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerSupport.NONE)
    @CheckReturnValue
    public final Flowable<T> sequential() {
        return sequential(Flowable.bufferSize());
    }

    /**
     * Merges the values from each 'rail' in a round-robin or same-order fashion and
     * exposes it as a regular Publisher sequence, running with a give prefetch value
     * for the rails.
     * <img width="640" height="602" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/parallelflowable.sequential.png" alt="">
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The operator honors backpressure.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code sequential} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param prefetch the prefetch amount to use for each rail
     * @return the new Flowable instance
     * @see ParallelFlowable#sequential()
     * @see ParallelFlowable#sequentialDelayError(int)
     */
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerSupport.NONE)
    @CheckReturnValue
    @NonNull
    public final Flowable<T> sequential(int prefetch) {
        ObjectHelper.verifyPositive(prefetch, "prefetch");
        return RxJavaPlugins.onAssembly(new ParallelJoin<T>(this, prefetch, false));
    }

    /**
     * Merges the values from each 'rail' in a round-robin or same-order fashion and
     * exposes it as a regular Flowable sequence, running with a default prefetch value
     * for the rails and delaying errors from all rails till all terminate.
     * <p>
     * This operator uses the default prefetch size returned by {@code Flowable.bufferSize()}.
     * <img width="640" height="602" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/parallelflowable.sequential.png" alt="">
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The operator honors backpressure.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code sequentialDelayError} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @return the new Flowable instance
     * @see ParallelFlowable#sequentialDelayError(int)
     * @see ParallelFlowable#sequential()
     * @since 2.0.7 - experimental
     */
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerSupport.NONE)
    @CheckReturnValue
    @Experimental
    @NonNull
    public final Flowable<T> sequentialDelayError() {
        return sequentialDelayError(Flowable.bufferSize());
    }

    /**
     * Merges the values from each 'rail' in a round-robin or same-order fashion and
     * exposes it as a regular Publisher sequence, running with a give prefetch value
     * for the rails and delaying errors from all rails till all terminate.
     * <img width="640" height="602" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/parallelflowable.sequential.png" alt="">
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The operator honors backpressure.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code sequentialDelayError} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param prefetch the prefetch amount to use for each rail
     * @return the new Flowable instance
     * @see ParallelFlowable#sequential()
     * @see ParallelFlowable#sequentialDelayError()
     * @since 2.0.7 - experimental
     */
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerSupport.NONE)
    @CheckReturnValue
    @NonNull
    public final Flowable<T> sequentialDelayError(int prefetch) {
        ObjectHelper.verifyPositive(prefetch, "prefetch");
        return RxJavaPlugins.onAssembly(new ParallelJoin<T>(this, prefetch, true));
    }

    /**
     * Sorts the 'rails' of this ParallelFlowable and returns a Publisher that sequentially
     * picks the smallest next value from the rails.
     * <p>
     * This operator requires a finite source ParallelFlowable.
     *
     * @param comparator the comparator to use
     * @return the new Flowable instance
     */
    @CheckReturnValue
    @NonNull
    public final Flowable<T> sorted(@NonNull Comparator<? super T> comparator) {
        return sorted(comparator, 16);
    }

    /**
     * Sorts the 'rails' of this ParallelFlowable and returns a Publisher that sequentially
     * picks the smallest next value from the rails.
     * <p>
     * This operator requires a finite source ParallelFlowable.
     *
     * @param comparator the comparator to use
     * @param capacityHint the expected number of total elements
     * @return the new Flowable instance
     */
    @CheckReturnValue
    @NonNull
    public final Flowable<T> sorted(@NonNull Comparator<? super T> comparator, int capacityHint) {
        ObjectHelper.requireNonNull(comparator, "comparator is null");
        ObjectHelper.verifyPositive(capacityHint, "capacityHint");
        int ch = capacityHint / parallelism() + 1;
        ParallelFlowable<List<T>> railReduced = reduce(Functions.<T>createArrayList(ch), ListAddBiConsumer.<T>instance());
        ParallelFlowable<List<T>> railSorted = railReduced.map(new SorterFunction<T>(comparator));

        return RxJavaPlugins.onAssembly(new ParallelSortedJoin<T>(railSorted, comparator));
    }

    /**
     * Sorts the 'rails' according to the comparator and returns a full sorted list as a Publisher.
     * <p>
     * This operator requires a finite source ParallelFlowable.
     *
     * @param comparator the comparator to compare elements
     * @return the new Flowable instance
     */
    @CheckReturnValue
    @NonNull
    public final Flowable<List<T>> toSortedList(@NonNull Comparator<? super T> comparator) {
        return toSortedList(comparator, 16);
    }
    /**
     * Sorts the 'rails' according to the comparator and returns a full sorted list as a Publisher.
     * <p>
     * This operator requires a finite source ParallelFlowable.
     *
     * @param comparator the comparator to compare elements
     * @param capacityHint the expected number of total elements
     * @return the new Flowable instance
     */
    @CheckReturnValue
    @NonNull
    public final Flowable<List<T>> toSortedList(@NonNull Comparator<? super T> comparator, int capacityHint) {
        ObjectHelper.requireNonNull(comparator, "comparator is null");
        ObjectHelper.verifyPositive(capacityHint, "capacityHint");

        int ch = capacityHint / parallelism() + 1;
        ParallelFlowable<List<T>> railReduced = reduce(Functions.<T>createArrayList(ch), ListAddBiConsumer.<T>instance());
        ParallelFlowable<List<T>> railSorted = railReduced.map(new SorterFunction<T>(comparator));

        Flowable<List<T>> merged = railSorted.reduce(new MergerBiFunction<T>(comparator));

        return RxJavaPlugins.onAssembly(merged);
    }

    /**
     * Call the specified consumer with the current element passing through any 'rail'.
     *
     * @param onNext the callback
     * @return the new ParallelFlowable instance
     */
    @CheckReturnValue
    @NonNull
    public final ParallelFlowable<T> doOnNext(@NonNull Consumer<? super T> onNext) {
        ObjectHelper.requireNonNull(onNext, "onNext is null");
        return RxJavaPlugins.onAssembly(new ParallelPeek<T>(this,
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
     *
     * @param onNext the callback
     * @param errorHandler the enumeration that defines how to handle errors thrown
     *                     from the onNext consumer
     * @return the new ParallelFlowable instance
     * @since 2.0.8 - experimental
     */
    @CheckReturnValue
    @Experimental
    @NonNull
    public final ParallelFlowable<T> doOnNext(@NonNull Consumer<? super T> onNext, @NonNull ParallelFailureHandling errorHandler) {
        ObjectHelper.requireNonNull(onNext, "onNext is null");
        ObjectHelper.requireNonNull(errorHandler, "errorHandler is null");
        return RxJavaPlugins.onAssembly(new ParallelDoOnNextTry<T>(this, onNext, errorHandler));
    }

    /**
     * Call the specified consumer with the current element passing through any 'rail' and
     * handles errors based on the returned value by the handler function.
     *
     * @param onNext the callback
     * @param errorHandler the function called with the current repeat count and
     *                     failure Throwable and should return one of the {@link ParallelFailureHandling}
     *                     enumeration values to indicate how to proceed.
     * @return the new ParallelFlowable instance
     * @since 2.0.8 - experimental
     */
    @CheckReturnValue
    @Experimental
    @NonNull
    public final ParallelFlowable<T> doOnNext(@NonNull Consumer<? super T> onNext, @NonNull BiFunction<? super Long, ? super Throwable, ParallelFailureHandling> errorHandler) {
        ObjectHelper.requireNonNull(onNext, "onNext is null");
        ObjectHelper.requireNonNull(errorHandler, "errorHandler is null");
        return RxJavaPlugins.onAssembly(new ParallelDoOnNextTry<T>(this, onNext, errorHandler));
    }

    /**
     * Call the specified consumer with the current element passing through any 'rail'
     * after it has been delivered to downstream within the rail.
     *
     * @param onAfterNext the callback
     * @return the new ParallelFlowable instance
     */
    @CheckReturnValue
    @NonNull
    public final ParallelFlowable<T> doAfterNext(@NonNull Consumer<? super T> onAfterNext) {
        ObjectHelper.requireNonNull(onAfterNext, "onAfterNext is null");
        return RxJavaPlugins.onAssembly(new ParallelPeek<T>(this,
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
     *
     * @param onError the callback
     * @return the new ParallelFlowable instance
     */
    @CheckReturnValue
    @NonNull
    public final ParallelFlowable<T> doOnError(@NonNull Consumer<Throwable> onError) {
        ObjectHelper.requireNonNull(onError, "onError is null");
        return RxJavaPlugins.onAssembly(new ParallelPeek<T>(this,
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
     * Run the specified Action when a 'rail' completes.
     *
     * @param onComplete the callback
     * @return the new ParallelFlowable instance
     */
    @CheckReturnValue
    @NonNull
    public final ParallelFlowable<T> doOnComplete(@NonNull Action onComplete) {
        ObjectHelper.requireNonNull(onComplete, "onComplete is null");
        return RxJavaPlugins.onAssembly(new ParallelPeek<T>(this,
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
     * Run the specified Action when a 'rail' completes or signals an error.
     *
     * @param onAfterTerminate the callback
     * @return the new ParallelFlowable instance
     */
    @CheckReturnValue
    @NonNull
    public final ParallelFlowable<T> doAfterTerminated(@NonNull Action onAfterTerminate) {
        ObjectHelper.requireNonNull(onAfterTerminate, "onAfterTerminate is null");
        return RxJavaPlugins.onAssembly(new ParallelPeek<T>(this,
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
     * Call the specified callback when a 'rail' receives a Subscription from its upstream.
     *
     * @param onSubscribe the callback
     * @return the new ParallelFlowable instance
     */
    @CheckReturnValue
    @NonNull
    public final ParallelFlowable<T> doOnSubscribe(@NonNull Consumer<? super Subscription> onSubscribe) {
        ObjectHelper.requireNonNull(onSubscribe, "onSubscribe is null");
        return RxJavaPlugins.onAssembly(new ParallelPeek<T>(this,
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
     *
     * @param onRequest the callback
     * @return the new ParallelFlowable instance
     */
    @CheckReturnValue
    @NonNull
    public final ParallelFlowable<T> doOnRequest(@NonNull LongConsumer onRequest) {
        ObjectHelper.requireNonNull(onRequest, "onRequest is null");
        return RxJavaPlugins.onAssembly(new ParallelPeek<T>(this,
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
     * Run the specified Action when a 'rail' receives a cancellation.
     *
     * @param onCancel the callback
     * @return the new ParallelFlowable instance
     */
    @CheckReturnValue
    @NonNull
    public final ParallelFlowable<T> doOnCancel(@NonNull Action onCancel) {
        ObjectHelper.requireNonNull(onCancel, "onCancel is null");
        return RxJavaPlugins.onAssembly(new ParallelPeek<T>(this,
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
     * Collect the elements in each rail into a collection supplied via a collectionSupplier
     * and collected into with a collector action, emitting the collection at the end.
     *
     * @param <C> the collection type
     * @param collectionSupplier the supplier of the collection in each rail
     * @param collector the collector, taking the per-rail collection and the current item
     * @return the new ParallelFlowable instance
     */
    @CheckReturnValue
    @NonNull
    public final <C> ParallelFlowable<C> collect(@NonNull Callable<? extends C> collectionSupplier, @NonNull BiConsumer<? super C, ? super T> collector) {
        ObjectHelper.requireNonNull(collectionSupplier, "collectionSupplier is null");
        ObjectHelper.requireNonNull(collector, "collector is null");
        return RxJavaPlugins.onAssembly(new ParallelCollect<T, C>(this, collectionSupplier, collector));
    }

    /**
     * Wraps multiple Publishers into a ParallelFlowable which runs them
     * in parallel and unordered.
     *
     * @param <T> the value type
     * @param publishers the array of publishers
     * @return the new ParallelFlowable instance
     */
    @CheckReturnValue
    @NonNull
    public static <T> ParallelFlowable<T> fromArray(@NonNull Publisher<T>... publishers) {
        if (publishers.length == 0) {
            throw new IllegalArgumentException("Zero publishers not supported");
        }
        return RxJavaPlugins.onAssembly(new ParallelFromArray<T>(publishers));
    }

    /**
     * Perform a fluent transformation to a value via a converter function which
     * receives this ParallelFlowable.
     *
     * @param <U> the output value type
     * @param converter the converter function from ParallelFlowable to some type
     * @return the value returned by the converter function
     */
    @CheckReturnValue
    @NonNull
    public final <U> U to(@NonNull Function<? super ParallelFlowable<T>, U> converter) {
        try {
            return ObjectHelper.requireNonNull(converter, "converter is null").apply(this);
        } catch (Throwable ex) {
            Exceptions.throwIfFatal(ex);
            throw ExceptionHelper.wrapOrThrow(ex);
        }
    }

    /**
     * Allows composing operators, in assembly time, on top of this ParallelFlowable
     * and returns another ParallelFlowable with composed features.
     *
     * @param <U> the output value type
     * @param composer the composer function from ParallelFlowable (this) to another ParallelFlowable
     * @return the ParallelFlowable returned by the function
     */
    @CheckReturnValue
    @NonNull
    public final <U> ParallelFlowable<U> compose(@NonNull ParallelTransformer<T, U> composer) {
        return RxJavaPlugins.onAssembly(ObjectHelper.requireNonNull(composer, "composer is null").apply(this));
    }

    /**
     * Generates and flattens Publishers on each 'rail'.
     * <p>
     * Errors are not delayed and uses unbounded concurrency along with default inner prefetch.
     *
     * @param <R> the result type
     * @param mapper the function to map each rail's value into a Publisher
     * @return the new ParallelFlowable instance
     */
    @CheckReturnValue
    @NonNull
    public final <R> ParallelFlowable<R> flatMap(@NonNull Function<? super T, ? extends Publisher<? extends R>> mapper) {
        return flatMap(mapper, false, Integer.MAX_VALUE, Flowable.bufferSize());
    }

    /**
     * Generates and flattens Publishers on each 'rail', optionally delaying errors.
     * <p>
     * It uses unbounded concurrency along with default inner prefetch.
     *
     * @param <R> the result type
     * @param mapper the function to map each rail's value into a Publisher
     * @param delayError should the errors from the main and the inner sources delayed till everybody terminates?
     * @return the new ParallelFlowable instance
     */
    @CheckReturnValue
    @NonNull
    public final <R> ParallelFlowable<R> flatMap(
            @NonNull Function<? super T, ? extends Publisher<? extends R>> mapper, boolean delayError) {
        return flatMap(mapper, delayError, Integer.MAX_VALUE, Flowable.bufferSize());
    }

    /**
     * Generates and flattens Publishers on each 'rail', optionally delaying errors
     * and having a total number of simultaneous subscriptions to the inner Publishers.
     * <p>
     * It uses a default inner prefetch.
     *
     * @param <R> the result type
     * @param mapper the function to map each rail's value into a Publisher
     * @param delayError should the errors from the main and the inner sources delayed till everybody terminates?
     * @param maxConcurrency the maximum number of simultaneous subscriptions to the generated inner Publishers
     * @return the new ParallelFlowable instance
     */
    @CheckReturnValue
    @NonNull
    public final <R> ParallelFlowable<R> flatMap(
            @NonNull Function<? super T, ? extends Publisher<? extends R>> mapper, boolean delayError, int maxConcurrency) {
        return flatMap(mapper, delayError, maxConcurrency, Flowable.bufferSize());
    }

    /**
     * Generates and flattens Publishers on each 'rail', optionally delaying errors,
     * having a total number of simultaneous subscriptions to the inner Publishers
     * and using the given prefetch amount for the inner Publishers.
     *
     * @param <R> the result type
     * @param mapper the function to map each rail's value into a Publisher
     * @param delayError should the errors from the main and the inner sources delayed till everybody terminates?
     * @param maxConcurrency the maximum number of simultaneous subscriptions to the generated inner Publishers
     * @param prefetch the number of items to prefetch from each inner Publisher
     * @return the new ParallelFlowable instance
     */
    @CheckReturnValue
    @NonNull
    public final <R> ParallelFlowable<R> flatMap(
            @NonNull Function<? super T, ? extends Publisher<? extends R>> mapper,
            boolean delayError, int maxConcurrency, int prefetch) {
        ObjectHelper.requireNonNull(mapper, "mapper is null");
        ObjectHelper.verifyPositive(maxConcurrency, "maxConcurrency");
        ObjectHelper.verifyPositive(prefetch, "prefetch");
        return RxJavaPlugins.onAssembly(new ParallelFlatMap<T, R>(this, mapper, delayError, maxConcurrency, prefetch));
    }

    /**
     * Generates and concatenates Publishers on each 'rail', signalling errors immediately
     * and generating 2 publishers upfront.
     *
     * @param <R> the result type
     * @param mapper the function to map each rail's value into a Publisher
     * source and the inner Publishers (immediate, boundary, end)
     * @return the new ParallelFlowable instance
     */
    @CheckReturnValue
    @NonNull
    public final <R> ParallelFlowable<R> concatMap(
            @NonNull Function<? super T, ? extends Publisher<? extends R>> mapper) {
        return concatMap(mapper, 2);
    }

    /**
     * Generates and concatenates Publishers on each 'rail', signalling errors immediately
     * and using the given prefetch amount for generating Publishers upfront.
     *
     * @param <R> the result type
     * @param mapper the function to map each rail's value into a Publisher
     * @param prefetch the number of items to prefetch from each inner Publisher
     * source and the inner Publishers (immediate, boundary, end)
     * @return the new ParallelFlowable instance
     */
    @CheckReturnValue
    @NonNull
    public final <R> ParallelFlowable<R> concatMap(
            @NonNull Function<? super T, ? extends Publisher<? extends R>> mapper,
                    int prefetch) {
        ObjectHelper.requireNonNull(mapper, "mapper is null");
        ObjectHelper.verifyPositive(prefetch, "prefetch");
        return RxJavaPlugins.onAssembly(new ParallelConcatMap<T, R>(this, mapper, prefetch, ErrorMode.IMMEDIATE));
    }

    /**
     * Generates and concatenates Publishers on each 'rail', optionally delaying errors
     * and generating 2 publishers upfront.
     *
     * @param <R> the result type
     * @param mapper the function to map each rail's value into a Publisher
     * @param tillTheEnd if true all errors from the upstream and inner Publishers are delayed
     * till all of them terminate, if false, the error is emitted when an inner Publisher terminates.
     * source and the inner Publishers (immediate, boundary, end)
     * @return the new ParallelFlowable instance
     */
    @CheckReturnValue
    @NonNull
    public final <R> ParallelFlowable<R> concatMapDelayError(
            @NonNull Function<? super T, ? extends Publisher<? extends R>> mapper,
                    boolean tillTheEnd) {
        return concatMapDelayError(mapper, 2, tillTheEnd);
    }

    /**
     * Generates and concatenates Publishers on each 'rail', optionally delaying errors
     * and using the given prefetch amount for generating Publishers upfront.
     *
     * @param <R> the result type
     * @param mapper the function to map each rail's value into a Publisher
     * @param prefetch the number of items to prefetch from each inner Publisher
     * @param tillTheEnd if true all errors from the upstream and inner Publishers are delayed
     * till all of them terminate, if false, the error is emitted when an inner Publisher terminates.
     * @return the new ParallelFlowable instance
     */
    @CheckReturnValue
    @NonNull
    public final <R> ParallelFlowable<R> concatMapDelayError(
            @NonNull Function<? super T, ? extends Publisher<? extends R>> mapper,
                    int prefetch, boolean tillTheEnd) {
        ObjectHelper.requireNonNull(mapper, "mapper is null");
        ObjectHelper.verifyPositive(prefetch, "prefetch");
        return RxJavaPlugins.onAssembly(new ParallelConcatMap<T, R>(
                this, mapper, prefetch, tillTheEnd ? ErrorMode.END : ErrorMode.BOUNDARY));
    }
}
