/**
 * Copyright 2016 Netflix, Inc.
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
package io.reactivex;

import java.util.concurrent.*;

import org.reactivestreams.*;

import io.reactivex.annotations.SchedulerSupport;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.functions.*;
import io.reactivex.internal.functions.*;
import io.reactivex.internal.operators.completable.*;
import io.reactivex.internal.operators.flowable.FlowableDelaySubscriptionOther;
import io.reactivex.internal.operators.observable.ObservableDelaySubscriptionOther;
import io.reactivex.internal.operators.single.*;
import io.reactivex.internal.subscribers.completable.*;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.schedulers.Schedulers;

/**
 * Represents a deferred computation without any value but only indication for completion or exception.
 * 
 * The class follows a similar event pattern as Reactive-Streams: onSubscribe (onError|onComplete)?
 */
public abstract class Completable implements CompletableSource {
    /**
     * Returns a Completable which terminates as soon as one of the source Completables
     * terminates (normally or with an error) and cancels all other Completables.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code amb} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param sources the array of source Completables
     * @return the new Completable instance
     * @throws NullPointerException if sources is null
     */
    @SchedulerSupport(SchedulerSupport.NONE)
    public static Completable amb(final CompletableSource... sources) {
        Objects.requireNonNull(sources, "sources is null");
        if (sources.length == 0) {
            return complete();
        }
        if (sources.length == 1) {
            return wrap(sources[0]);
        }
        
        return new CompletableAmbArray(sources);
    }
    
    /**
     * Returns a Completable which terminates as soon as one of the source Completables
     * terminates (normally or with an error) and cancels all other Completables.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code amb} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param sources the array of source Completables
     * @return the new Completable instance
     * @throws NullPointerException if sources is null
     */
    @SchedulerSupport(SchedulerSupport.NONE)
    public static Completable amb(final Iterable<? extends CompletableSource> sources) {
        Objects.requireNonNull(sources, "sources is null");
        
        return new CompletableAmbIterable(sources);
    }
    
    /**
     * Returns a Completable instance that completes immediately when subscribed to.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code complete} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @return a Completable instance that completes immediately 
     */
    @SchedulerSupport(SchedulerSupport.NONE)
    public static Completable complete() {
        return CompletableEmpty.INSTANCE;
    }
    
    /**
     * Returns a Completable which completes only when all sources complete, one after another.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code concat} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param sources the sources to concatenate
     * @return the Completable instance which completes only when all sources complete
     * @throws NullPointerException if sources is null
     */
    @SchedulerSupport(SchedulerSupport.NONE)
    public static Completable concat(CompletableSource... sources) {
        Objects.requireNonNull(sources, "sources is null");
        if (sources.length == 0) {
            return complete();
        } else
        if (sources.length == 1) {
            return wrap(sources[0]);
        }
        return new CompletableConcatArray(sources);
    }
    
    /**
     * Returns a Completable which completes only when all sources complete, one after another.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code concat} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param sources the sources to concatenate
     * @return the Completable instance which completes only when all sources complete
     * @throws NullPointerException if sources is null
     */
    @SchedulerSupport(SchedulerSupport.NONE)
    public static Completable concat(Iterable<? extends CompletableSource> sources) {
        Objects.requireNonNull(sources, "sources is null");
        
        return new CompletableConcatIterable(sources);
    }
    
    /**
     * Returns a Completable which completes only when all sources complete, one after another.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code concat} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param sources the sources to concatenate
     * @return the Completable instance which completes only when all sources complete
     * @throws NullPointerException if sources is null
     */
    @SchedulerSupport(SchedulerSupport.NONE)
    public static Completable concat(Publisher<? extends CompletableSource> sources) {
        return concat(sources, 2);
    }
    
    /**
     * Returns a Completable which completes only when all sources complete, one after another.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code concat} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param sources the sources to concatenate
     * @param prefetch the number of sources to prefetch from the sources
     * @return the Completable instance which completes only when all sources complete
     * @throws NullPointerException if sources is null
     */
    @SchedulerSupport(SchedulerSupport.NONE)
    public static Completable concat(Publisher<? extends CompletableSource> sources, int prefetch) {
        Objects.requireNonNull(sources, "sources is null");
        if (prefetch < 1) {
            throw new IllegalArgumentException("prefetch > 0 required but it was " + prefetch);
        }
        return new CompletableConcat(sources, prefetch);
    }

    /**
     * Constructs a Completable instance by wrapping the given source callback.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code create} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param source the callback which will receive the CompletableObserver instances
     * when the Completable is subscribed to.
     * @return the created Completable instance
     * @throws NullPointerException if source is null
     */
    @SchedulerSupport(SchedulerSupport.NONE)
    public static Completable create(CompletableSource source) {
        Objects.requireNonNull(source, "source is null");
        if (source instanceof Completable) {
            throw new IllegalArgumentException("Use of create(Completable)!");
        }
        try {
            // TODO plugin wrapping source

            return RxJavaPlugins.onAssembly(new CompletableFromSource(source));
        } catch (NullPointerException ex) { // NOPMD
            throw ex;
        } catch (Throwable ex) {
            Exceptions.throwIfFatal(ex);
            RxJavaPlugins.onError(ex);
            throw toNpe(ex);
        }
    }
    
    /**
     * Constructs a Completable instance by wrapping the given source callback
     * <strong>without any safeguards; you should manage the lifecycle and response
     * to downstream cancellation/dispose</strong>.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code unsafeCreate} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param source the callback which will receive the CompletableObserver instances
     * when the Completable is subscribed to.
     * @return the created Completable instance
     * @throws NullPointerException if source is null
     */
    @SchedulerSupport(SchedulerSupport.NONE)
    public static Completable unsafeCreate(CompletableSource source) {
        Objects.requireNonNull(source, "source is null");
        if (source instanceof Completable) {
            throw new IllegalArgumentException("Use of unsafeCreate(Completable)!");
        }
        try {
            // TODO plugin wrapping source
            
            return RxJavaPlugins.onAssembly(new CompletableFromUnsafeSource(source));
        } catch (NullPointerException ex) { // NOPMD
            throw ex;
        } catch (Throwable ex) {
            Exceptions.throwIfFatal(ex);
            RxJavaPlugins.onError(ex);
            throw toNpe(ex);
        } 
    }
    
    /**
     * Defers the subscription to a Completable instance returned by a supplier.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code defer} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param completableSupplier the supplier that returns the Completable that will be subscribed to.
     * @return the Completable instance
     */
    @SchedulerSupport(SchedulerSupport.NONE)
    public static Completable defer(final Callable<? extends CompletableSource> completableSupplier) {
        Objects.requireNonNull(completableSupplier, "completableSupplier");
        return new CompletableDefer(completableSupplier);
    }

    /**
     * Creates a Completable which calls the given error supplier for each subscriber
     * and emits its returned Throwable.
     * <p>
     * If the errorSupplier returns null, the child CompletableSubscribers will receive a
     * NullPointerException.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code error} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param errorSupplier the error supplier, not null
     * @return the new Completable instance
     * @throws NullPointerException if errorSupplier is null
     */
    @SchedulerSupport(SchedulerSupport.NONE)
    public static Completable error(final Callable<? extends Throwable> errorSupplier) {
        Objects.requireNonNull(errorSupplier, "errorSupplier is null");
        return new CompletableErrorSupplier(errorSupplier);
    }
    
    /**
     * Creates a Completable instance that emits the given Throwable exception to subscribers.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code error} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param error the Throwable instance to emit, not null
     * @return the new Completable instance
     * @throws NullPointerException if error is null
     */
    @SchedulerSupport(SchedulerSupport.NONE)
    public static Completable error(final Throwable error) {
        Objects.requireNonNull(error, "error is null");
        return new CompletableError(error);
    }
    
    
    /**
     * Returns a Completable instance that runs the given Action for each subscriber and
     * emits either an unchecked exception or simply completes.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code fromAction} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param run the runnable to run for each subscriber
     * @return the new Completable instance
     * @throws NullPointerException if run is null
     */
    @SchedulerSupport(SchedulerSupport.NONE)
    public static Completable fromAction(final Action run) {
        Objects.requireNonNull(run, "run is null");
        return new CompletableFromAction(run);
    }

    /**
     * Returns a Completable which when subscribed, executes the callable function, ignores its
     * normal result and emits onError or onCompleted only.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code fromCallable} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param callable the callable instance to execute for each subscriber
     * @return the new Completable instance
     */
    @SchedulerSupport(SchedulerSupport.NONE)
    public static Completable fromCallable(final Callable<?> callable) {
        Objects.requireNonNull(callable, "callable is null");
        return new CompletableFromCallable(callable);
    }
    
    /**
     * Returns a Completable instance that reacts to the termination of the given Future in a blocking fashion.
     * <p>
     * Note that cancellation from any of the subscribers to this Completable will cancel the future.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code fromFuture} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param future the future to react to
     * @return the new Completable instance
     */
    @SchedulerSupport(SchedulerSupport.NONE)
    public static Completable fromFuture(final Future<?> future) {
        Objects.requireNonNull(future, "future is null");
        return fromAction(Functions.futureAction(future));
    }
    
    /**
     * Returns a Completable instance that subscribes to the given Observable, ignores all values and
     * emits only the terminal event.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code fromObservable} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the type of the Observable
     * @param observable the Observable instance to subscribe to, not null
     * @return the new Completable instance
     * @throws NullPointerException if flowable is null
     */
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <T> Completable fromObservable(final ObservableSource<T> observable) {
        Objects.requireNonNull(observable, "observable is null");
        return new CompletableFromObservable<T>(observable);
    }
    
    /**
     * Returns a Completable instance that subscribes to the given publisher, ignores all values and
     * emits only the terminal event.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code fromPublisher} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the type of the publisher
     * @param publisher the Publisher instance to subscribe to, not null
     * @return the new Completable instance
     * @throws NullPointerException if publisher is null
     */
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <T> Completable fromPublisher(final Publisher<T> publisher) {
        Objects.requireNonNull(publisher, "publisher is null");
        return new CompletableFromPublisher<T>(publisher);
    }

    /**
     * Returns a Completable instance that when subscribed to, subscribes to the Single instance and
     * emits a completion event if the single emits onSuccess or forwards any onError events.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code fromSingle} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the value type of the Single
     * @param single the Single instance to subscribe to, not null
     * @return the new Completable instance
     * @throws NullPointerException if single is null
     */
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <T> Completable fromSingle(final SingleSource<T> single) {
        Objects.requireNonNull(single, "single is null");
        return new CompletableFromSingle<T>(single);
    }
    
    /**
     * Returns a Completable instance that subscribes to all sources at once and
     * completes only when all source Completables complete or one of them emits an error.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code merge} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param sources the iterable sequence of sources.
     * @return the new Completable instance
     * @throws NullPointerException if sources is null
     */
    @SchedulerSupport(SchedulerSupport.NONE)
    public static Completable merge(CompletableSource... sources) {
        Objects.requireNonNull(sources, "sources is null");
        if (sources.length == 0) {
            return complete();
        } else
        if (sources.length == 1) {
            return wrap(sources[0]);
        }
        return new CompletableMergeArray(sources);
    }

    /**
     * Returns a Completable instance that subscribes to all sources at once and
     * completes only when all source Completables complete or one of them emits an error.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code merge} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param sources the iterable sequence of sources.
     * @return the new Completable instance
     * @throws NullPointerException if sources is null
     */
    @SchedulerSupport(SchedulerSupport.NONE)
    public static Completable merge(Iterable<? extends CompletableSource> sources) {
        Objects.requireNonNull(sources, "sources is null");
        return new CompletableMergeIterable(sources);
    }
    
    /**
     * Returns a Completable instance that subscribes to all sources at once and
     * completes only when all source Completables complete or one of them emits an error.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code merge} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param sources the iterable sequence of sources.
     * @return the new Completable instance
     * @throws NullPointerException if sources is null
     */
    @SchedulerSupport(SchedulerSupport.NONE)
    public static Completable merge(Publisher<? extends CompletableSource> sources) {
        return merge0(sources, Integer.MAX_VALUE, false);
    }
    
    /**
     * Returns a Completable instance that keeps subscriptions to a limited number of sources at once and
     * completes only when all source Completables complete or one of them emits an error.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code merge} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param sources the iterable sequence of sources.
     * @param maxConcurrency the maximum number of concurrent subscriptions
     * @return the new Completable instance
     * @throws NullPointerException if sources is null
     * @throws IllegalArgumentException if maxConcurrency is less than 1
     */
    @SchedulerSupport(SchedulerSupport.NONE)
    public static Completable merge(Publisher<? extends CompletableSource> sources, int maxConcurrency) {
        return merge0(sources, maxConcurrency, false);
    }
    
    /**
     * Returns a Completable instance that keeps subscriptions to a limited number of sources at once and
     * completes only when all source Completables terminate in one way or another, combining any exceptions
     * thrown by either the sources Observable or the inner Completable instances.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code merge0} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param sources the iterable sequence of sources.
     * @param maxConcurrency the maximum number of concurrent subscriptions
     * @param delayErrors delay all errors from the main source and from the inner Completables?
     * @return the new Completable instance
     * @throws NullPointerException if sources is null
     * @throws IllegalArgumentException if maxConcurrency is less than 1
     */
    @SchedulerSupport(SchedulerSupport.NONE)
    private static Completable merge0(Publisher<? extends CompletableSource> sources, int maxConcurrency, boolean delayErrors) {
        Objects.requireNonNull(sources, "sources is null");
        if (maxConcurrency < 1) {
            throw new IllegalArgumentException("maxConcurrency > 0 required but it was " + maxConcurrency);
        }
        return new CompletableMerge(sources, maxConcurrency, delayErrors);
    }

    /**
     * Returns a CompletableConsumable that subscribes to all Completables in the source array and delays
     * any error emitted by either the sources observable or any of the inner Completables until all of
     * them terminate in a way or another.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code mergeDelayError} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param sources the array of Completables
     * @return the new Completable instance
     * @throws NullPointerException if sources is null
     */
    @SchedulerSupport(SchedulerSupport.NONE)
    public static Completable mergeDelayError(CompletableSource... sources) {
        Objects.requireNonNull(sources, "sources is null");
        return new CompletableMergeDelayErrorArray(sources);
    }

    /**
     * Returns a Completable that subscribes to all Completables in the source sequence and delays
     * any error emitted by either the sources observable or any of the inner Completables until all of
     * them terminate in a way or another.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code mergeDelayError} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param sources the sequence of Completables
     * @return the new Completable instance
     * @throws NullPointerException if sources is null
     */
    @SchedulerSupport(SchedulerSupport.NONE)
    public static Completable mergeDelayError(Iterable<? extends CompletableSource> sources) {
        Objects.requireNonNull(sources, "sources is null");
        return new CompletableMergeDelayErrorIterable(sources);
    }

    
    /**
     * Returns a Completable that subscribes to all Completables in the source sequence and delays
     * any error emitted by either the sources observable or any of the inner Completables until all of
     * them terminate in a way or another.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code mergeDelayError} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param sources the sequence of Completables
     * @return the new Completable instance
     * @throws NullPointerException if sources is null
     */
    @SchedulerSupport(SchedulerSupport.NONE)
    public static Completable mergeDelayError(Publisher<? extends CompletableSource> sources) {
        return merge0(sources, Integer.MAX_VALUE, true);
    }
    
    /**
     * Returns a Completable that subscribes to a limited number of inner Completables at once in 
     * the source sequence and delays any error emitted by either the sources 
     * observable or any of the inner Completables until all of
     * them terminate in a way or another.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code mergeDelayError} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param sources the sequence of Completables
     * @param maxConcurrency the maximum number of concurrent subscriptions to Completables
     * @return the new Completable instance
     * @throws NullPointerException if sources is null
     */
    @SchedulerSupport(SchedulerSupport.NONE)
    public static Completable mergeDelayError(Publisher<? extends CompletableSource> sources, int maxConcurrency) {
        return merge0(sources, maxConcurrency, true);
    }
    
    /**
     * Returns a Completable that never calls onError or onComplete.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code never} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @return the singleton instance that never calls onError or onComplete
     */
    @SchedulerSupport(SchedulerSupport.NONE)
    public static Completable never() {
        return CompletableNever.INSTANCE;
    }
    
    /**
     * Returns a Completable instance that fires its onComplete event after the given delay ellapsed.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code timer} does operate by default on the {@code computation} {@link Scheduler}.</dd>
     * </dl>
     * @param delay the delay time
     * @param unit the delay unit
     * @return the new Completable instance
     */
    @SchedulerSupport(SchedulerSupport.COMPUTATION)
    public static Completable timer(long delay, TimeUnit unit) {
        return timer(delay, unit, Schedulers.computation());
    }
    
    /**
     * Returns a Completable instance that fires its onComplete event after the given delay ellapsed
     * by using the supplied scheduler.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code timer} operates on the {@link Scheduler} you specify.</dd>
     * </dl>
     * @param delay the delay time
     * @param unit the delay unit
     * @param scheduler the scheduler where to emit the complete event
     * @return the new Completable instance
     */
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    public static Completable timer(final long delay, final TimeUnit unit, final Scheduler scheduler) {
        Objects.requireNonNull(unit, "unit is null");
        Objects.requireNonNull(scheduler, "scheduler is null");
        return new CompletableTimer(delay, unit, scheduler);
    }
    
    /**
     * Creates a NullPointerException instance and sets the given Throwable as its initial cause.
     * @param ex the Throwable instance to use as cause, not null (not verified)
     * @return the created NullPointerException
     */
    private static NullPointerException toNpe(Throwable ex) {
        NullPointerException npe = new NullPointerException("Actually not, but can't pass out an exception otherwise...");
        npe.initCause(ex);
        return npe;
    }
    
    /**
     * Returns a Completable instance which manages a resource along 
     * with a custom Completable instance while the subscription is active.
     * <p>
     * This overload performs an eager unsubscription before the terminal event is emitted.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code using} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <R> the resource type
     * @param resourceSupplier the supplier that returns a resource to be managed. 
     * @param completableFunction the function that given a resource returns a Completable instance that will be subscribed to
     * @param disposer the consumer that disposes the resource created by the resource supplier
     * @return the new Completable instance
     */
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <R> Completable using(Callable<R> resourceSupplier, 
            Function<? super R, ? extends CompletableSource> completableFunction,
            Consumer<? super R> disposer) {
        return using(resourceSupplier, completableFunction, disposer, true);
    }
    
    /**
     * Returns a Completable instance which manages a resource along 
     * with a custom Completable instance while the subscription is active and performs eager or lazy
     * resource disposition.
     * <p>
     * If this overload performs a lazy unsubscription after the terminal event is emitted.
     * Exceptions thrown at this time will be delivered to RxJavaPlugins only.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code using} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <R> the resource type
     * @param resourceSupplier the supplier that returns a resource to be managed
     * @param completableFunction the function that given a resource returns a non-null
     * Completable instance that will be subscribed to
     * @param disposer the consumer that disposes the resource created by the resource supplier
     * @param eager if true, the resource is disposed before the terminal event is emitted, if false, the
     * resource is disposed after the terminal event has been emitted
     * @return the new Completable instance
     */
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <R> Completable using(
            final Callable<R> resourceSupplier, 
            final Function<? super R, ? extends CompletableSource> completableFunction,
            final Consumer<? super R> disposer, 
            final boolean eager) {
        Objects.requireNonNull(resourceSupplier, "resourceSupplier is null");
        Objects.requireNonNull(completableFunction, "completableFunction is null");
        Objects.requireNonNull(disposer, "disposer is null");
        
        return new CompletableUsing<R>(resourceSupplier, completableFunction, disposer, eager);
    }

    /**
     * Wraps the given CompletableSource into a Completable
     * if not already Completable.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code amb} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param source the source to wrap
     * @return the source or its wrapper Completable
     * @throws NullPointerException if source is null
     */
    @SchedulerSupport(SchedulerSupport.NONE)
    public static Completable wrap(CompletableSource source) {
        Objects.requireNonNull(source, "source is null");
        if (source instanceof Completable) {
            return (Completable)source;
        }
        return new CompletableFromUnsafeSource(source);
    }
    
    /**
     * Returns a Completable that emits the a terminated event of either this Completable
     * or the other Completable whichever fires first.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code ambWith} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param other the other Completable, not null
     * @return the new Completable instance
     * @throws NullPointerException if other is null
     */
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Completable ambWith(CompletableSource other) {
        Objects.requireNonNull(other, "other is null");
        return amb(this, other);
    }

    /**
     * Returns an Observable which will subscribe to this Completable and once that is completed then 
     * will subscribe to the {@code next} ObservableSource. An error event from this Completable will be 
     * propagated to the downstream subscriber and will result in skipping the subscription of the 
     * Observable.  
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code andThen} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the value type of the next ObservableSource
     * @param next the Observable to subscribe after this Completable is completed, not null
     * @return Observable that composes this Completable and next
     * @throws NullPointerException if next is null
     */
    @SchedulerSupport(SchedulerSupport.NONE)
    public final <T> Observable<T> andThen(ObservableSource<T> next) {
        Objects.requireNonNull(next, "next is null");
        return new ObservableDelaySubscriptionOther<T, Object>(next, toObservable());
    }

    /**
     * Returns an Flowable which will subscribe to this Completable and once that is completed then 
     * will subscribe to the {@code next} Flowable. An error event from this Completable will be 
     * propagated to the downstream subscriber and will result in skipping the subscription of the 
     * Observable.  
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code andThen} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the value type of the next Publisher
     * @param next the Observable to subscribe after this Completable is completed, not null
     * @return Flowable that composes this Completable and next
     * @throws NullPointerException if next is null
     */
    @SchedulerSupport(SchedulerSupport.NONE)
    public final <T> Flowable<T> andThen(Publisher<T> next) {
        Objects.requireNonNull(next, "next is null");
        return new FlowableDelaySubscriptionOther<T, Object>(next, toFlowable());
    }

    /**
     * Returns a Single which will subscribe to this Completable and once that is completed then
     * will subscribe to the {@code next} SingleSource. An error event from this Completable will be
     * propagated to the downstream subscriber and will result in skipping the subscription of the
     * Single.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code andThen} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T> the value type of the next SingleSource
     * @param next the Single to subscribe after this Completable is completed, not null
     * @return Single that composes this Completable and next
     */
    @SchedulerSupport(SchedulerSupport.NONE)
    public final <T> Single<T> andThen(SingleSource<T> next) {
        Objects.requireNonNull(next, "next is null");
        return new SingleDelayWithCompletable<T>(next, this);
    }

    /**
     * Returns a Completable that first runs this Completable
     * and then the other completable.
     * <p>
     * This is an alias for {@link #concatWith(CompletableSource)}.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code andThen} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param next the other Completable, not null
     * @return the new Completable instance
     * @throws NullPointerException if other is null
     */
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Completable andThen(CompletableSource next) {
        return concatWith(next);
    }
    
    /**
     * Subscribes to and awaits the termination of this Completable instance in a blocking manner and
     * rethrows any exception emitted.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code blockingAwait} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @throws RuntimeException wrapping an InterruptedException if the current thread is interrupted
     */
    @SchedulerSupport(SchedulerSupport.NONE)
    public final void blockingAwait() {
        CompletableAwait.await(this);
    }
    
    /**
     * Subscribes to and awaits the termination of this Completable instance in a blocking manner
     * with a specific timeout and rethrows any exception emitted within the timeout window.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code blockingAwait} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param timeout the timeout value
     * @param unit the timeout unit
     * @return true if the this Completable instance completed normally within the time limit,
     * false if the timeout ellapsed before this Completable terminated.
     * @throws RuntimeException wrapping an InterruptedException if the current thread is interrupted
     */
    @SchedulerSupport(SchedulerSupport.NONE)
    public final boolean blockingAwait(long timeout, TimeUnit unit) {
        return CompletableAwait.await(this, timeout, unit);
    }

    /**
     * Subscribes to this Completable instance and blocks until it terminates, then returns null or
     * the emitted exception if any.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code doAfterTerminate} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @return the throwable if this terminated with an error, null otherwise
     * @throws RuntimeException that wraps an InterruptedException if the wait is interrupted
     */
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Throwable blockingGet() {
        return CompletableAwait.get(this);
    }
    
    /**
     * Subscribes to this Completable instance and blocks until it terminates or the specified timeout 
     * ellapses, then returns null for normal termination or the emitted exception if any.
     * @param timeout the timeout value
     * @param unit the time unit
     * @return the throwable if this terminated with an error, null otherwise
     * @throws RuntimeException that wraps an InterruptedException if the wait is interrupted or
     * TimeoutException if the specified timeout ellapsed before it
     */
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Throwable blockingGet(long timeout, TimeUnit unit) {
        return CompletableAwait.get(this, timeout, unit);
    }

    /**
     * Calls the given transformer function with this instance and returns the function's resulting
     * Completable.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code compose} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param transformer the transformer function, not null
     * @return the Completable returned by the function
     * @throws NullPointerException if transformer is null
     */
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Completable compose(CompletableTransformer transformer) {
        return wrap(to(transformer));
    }
    
    /**
     * Concatenates this Completable with another Completable.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code concatWith} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param other the other Completable, not null
     * @return the new Completable which subscribes to this and then the other Completable
     * @throws NullPointerException if other is null
     */
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Completable concatWith(CompletableSource other) {
        Objects.requireNonNull(other, "other is null");
        return concat(this, other);
    }

    /**
     * Returns a Completable which delays the emission of the completion event by the given time.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code delay} does operate by default on the {@code computation} {@link Scheduler}.</dd>
     * </dl>
     * @param delay the delay time
     * @param unit the delay unit
     * @return the new Completable instance
     * @throws NullPointerException if unit is null
     */
    @SchedulerSupport(SchedulerSupport.COMPUTATION)
    public final Completable delay(long delay, TimeUnit unit) {
        return delay(delay, unit, Schedulers.computation(), false);
    }
    
    /**
     * Returns a Completable which delays the emission of the completion event by the given time while
     * running on the specified scheduler.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code delay} operates on the {@link Scheduler} you specify.</dd>
     * </dl>
     * @param delay the delay time
     * @param unit the delay unit
     * @param scheduler the scheduler to run the delayed completion on
     * @return the new Completable instance
     * @throws NullPointerException if unit or scheduler is null
     */
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    public final Completable delay(long delay, TimeUnit unit, Scheduler scheduler) {
        return delay(delay, unit, scheduler, false);
    }
    
    /**
     * Returns a Completable which delays the emission of the completion event, and optionally the error as well, by the given time while
     * running on the specified scheduler.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code delay} operates on the {@link Scheduler} you specify.</dd>
     * </dl>
     * @param delay the delay time
     * @param unit the delay unit
     * @param scheduler the scheduler to run the delayed completion on
     * @param delayError delay the error emission as well?
     * @return the new Completable instance
     * @throws NullPointerException if unit or scheduler is null
     */
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    public final Completable delay(final long delay, final TimeUnit unit, final Scheduler scheduler, final boolean delayError) {
        Objects.requireNonNull(unit, "unit is null");
        Objects.requireNonNull(scheduler, "scheduler is null");
        return new CompletableDelay(this, delay, unit, scheduler, delayError);
    }

    /**
     * Returns a Completable which calls the given onComplete callback if this Completable completes.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code doOnComplete} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param onComplete the callback to call when this emits an onComplete event
     * @return the new Completable instance
     * @throws NullPointerException if onComplete is null
     */
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Completable doOnComplete(Action onComplete) {
        return doOnLifecycle(Functions.emptyConsumer(), Functions.emptyConsumer(),
                onComplete, Functions.EMPTY_ACTION,
                Functions.EMPTY_ACTION, Functions.EMPTY_ACTION);
    }
    
    /**
     * Returns a Completable which calls the giveon onDispose callback if the child subscriber cancels
     * the subscription.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code doOnDispose} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param onDispose the callback to call when the child subscriber cancels the subscription
     * @return the new Completable instance
     * @throws NullPointerException if onDispose is null
     */
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Completable doOnDispose(Action onDispose) {
        return doOnLifecycle(Functions.emptyConsumer(), Functions.emptyConsumer(),
                Functions.EMPTY_ACTION, Functions.EMPTY_ACTION,
                Functions.EMPTY_ACTION, onDispose);
    }
    
    /**
     * Returns a Completable which calls the given onError callback if this Completable emits an error.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code doOnError} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param onError the error callback
     * @return the new Completable instance
     * @throws NullPointerException if onError is null
     */
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Completable doOnError(Consumer<? super Throwable> onError) {
        return doOnLifecycle(Functions.emptyConsumer(), onError,
                Functions.EMPTY_ACTION, Functions.EMPTY_ACTION,
                Functions.EMPTY_ACTION, Functions.EMPTY_ACTION);
    }

    /**
     * Returns a Completable instance that calls the various callbacks on the specific
     * lifecycle events.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code doOnLifecycle} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param onSubscribe the consumer called when a CompletableSubscriber subscribes.
     * @param onError the consumer called when this emits an onError event
     * @param onComplete the runnable called just before when this Completable completes normally
     * @param onAfterTerminate the runnable called after this Completable completes normally
     * @param onDisposed the runnable called when the child cancels the subscription
     * @return the new Completable instance
     */
    @SchedulerSupport(SchedulerSupport.NONE)
    private Completable doOnLifecycle(
            final Consumer<? super Disposable> onSubscribe, 
            final Consumer<? super Throwable> onError, 
            final Action onComplete, 
            final Action onTerminate,
            final Action onAfterTerminate,
            final Action onDisposed) {
        Objects.requireNonNull(onSubscribe, "onSubscribe is null");
        Objects.requireNonNull(onError, "onError is null");
        Objects.requireNonNull(onComplete, "onComplete is null");
        Objects.requireNonNull(onTerminate, "onTerminate is null");
        Objects.requireNonNull(onAfterTerminate, "onAfterTerminate is null");
        Objects.requireNonNull(onDisposed, "onDisposed is null");
        return new CompletablePeek(this, onSubscribe, onError, onComplete, onTerminate, onAfterTerminate, onDisposed);
    }
    
    /**
     * Returns a Completable instance that calls the given onSubscribe callback with the disposable
     * that child subscribers receive on subscription.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code doOnSubscribe} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param onSubscribe the callback called when a child subscriber subscribes
     * @return the new Completable instance
     * @throws NullPointerException if onSubscribe is null
     */
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Completable doOnSubscribe(Consumer<? super Disposable> onSubscribe) {
        return doOnLifecycle(onSubscribe, Functions.emptyConsumer(),
                Functions.EMPTY_ACTION, Functions.EMPTY_ACTION,
                Functions.EMPTY_ACTION, Functions.EMPTY_ACTION);
    }
    
    /**
     * Returns a Completable instance that calls the given onTerminate callback just before this Completable
     * completes normally or with an exception
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code doOnTerminate} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param onTerminate the callback to call just before this Completable terminates
     * @return the new Completable instance
     */
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Completable doOnTerminate(final Action onTerminate) {
        return doOnLifecycle(Functions.emptyConsumer(), Functions.emptyConsumer(),
                Functions.EMPTY_ACTION, onTerminate,
                Functions.EMPTY_ACTION, Functions.EMPTY_ACTION);
    }

    /**
     * Returns a Completable instance that calls the given onTerminate callback after this Completable
     * completes normally or with an exception
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code doAfterTerminate} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param onAfterTerminate the callback to call after this Completable terminates
     * @return the new Completable instance
     */
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Completable doAfterTerminate(final Action onAfterTerminate) {
        return doOnLifecycle(Functions.emptyConsumer(), Functions.emptyConsumer(),
                onAfterTerminate, Functions.EMPTY_ACTION,
                Functions.EMPTY_ACTION, Functions.EMPTY_ACTION);
    }

    /**
     * <strong>Advanced use without safeguards:</strong> lifts a CompletableSubscriber 
     * transformation into the chain of Completables.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code lift} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param onLift the lifting function that transforms the child subscriber with a parent subscriber.
     * @return the new Completable instance
     * @throws NullPointerException if onLift is null
     */
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Completable lift(final CompletableOperator onLift) {
        Objects.requireNonNull(onLift, "onLift is null");
        return new CompletableLift(this, onLift);
    }

    /**
     * Returns a Completable which subscribes to this and the other Completable and completes
     * when both of them complete or one emits an error.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code mergeWith} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param other the other Completable instance
     * @return the new Completable instance
     * @throws NullPointerException if other is null
     */
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Completable mergeWith(CompletableSource other) {
        Objects.requireNonNull(other, "other is null");
        return merge(this, other);
    }
    
    /**
     * Returns a Completable which emits the terminal events from the thread of the specified scheduler.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code observeOn} operates on a {@link Scheduler} you specify.</dd>
     * </dl>
     * @param scheduler the scheduler to emit terminal events on
     * @return the new Completable instance
     * @throws NullPointerException if scheduler is null
     */
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    public final Completable observeOn(final Scheduler scheduler) {
        Objects.requireNonNull(scheduler, "scheduler is null");
        return new CompletableObserveOn(this, scheduler);
    }
    
    /**
     * Returns a Completable instance that if this Completable emits an error, it will emit an onComplete
     * and swallow the throwable.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code onErrorComplete} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @return the new Completable instance
     */
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Completable onErrorComplete() {
        return onErrorComplete(Functions.alwaysTrue());
    }

    /**
     * Returns a Completable instance that if this Completable emits an error and the predicate returns
     * true, it will emit an onComplete and swallow the throwable.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code doErrorComplete} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param predicate the predicate to call when an Throwable is emitted which should return true
     * if the Throwable should be swallowed and replaced with an onComplete.
     * @return the new Completable instance
     */
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Completable onErrorComplete(final Predicate<? super Throwable> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        
        return new CompletableOnErrorComplete(this, predicate);
    }
    
    /**
     * Returns a Completable instance that when encounters an error from this Completable, calls the
     * specified mapper function that returns another Completable instance for it and resumes the
     * execution with it.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code onErrorResumeNext} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param errorMapper the mapper function that takes the error and should return a Completable as
     * continuation.
     * @return the new Completable instance
     */
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Completable onErrorResumeNext(final Function<? super Throwable, ? extends CompletableSource> errorMapper) {
        Objects.requireNonNull(errorMapper, "errorMapper is null");
        return new CompletableResumeNext(this, errorMapper);
    }
    
    /**
     * Returns a Completable that repeatedly subscribes to this Completable until cancelled.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code repeat} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @return the new Completable instance
     */
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Completable repeat() {
        return fromPublisher(toFlowable().repeat());
    }
    
    /**
     * Returns a Completable that subscribes repeatedly at most the given times to this Completable.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code repeat} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param times the number of times the resubscription should happen
     * @return the new Completable instance
     * @throws IllegalArgumentException if times is less than zero
     */
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Completable repeat(long times) {
        return fromPublisher(toFlowable().repeat(times));
    }
    
    /**
     * Returns a Completable that repeatedly subscribes to this Completable so long as the given
     * stop supplier returns false.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code repeatUntil} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param stop the supplier that should return true to stop resubscribing.
     * @return the new Completable instance
     * @throws NullPointerException if stop is null
     */
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Completable repeatUntil(BooleanSupplier stop) {
        return fromPublisher(toFlowable().repeatUntil(stop));
    }
    
    /**
     * Returns a Completable instance that repeats when the Publisher returned by the handler
     * emits an item or completes when this Publisher emits a completed event.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code repeatWhen} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param handler the function that transforms the stream of values indicating the completion of
     * this Completable and returns a Publisher that emits items for repeating or completes to indicate the
     * repetition should stop
     * @return the new Completable instance
     * @throws NullPointerException if stop is null
     */
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Completable repeatWhen(Function<? super Flowable<Object>, ? extends Publisher<Object>> handler) {
        return fromPublisher(toFlowable().repeatWhen(handler));
    }
    
    /**
     * Returns a Completable that retries this Completable as long as it emits an onError event.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code retry} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @return the new Completable instance
     */
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Completable retry() {
        return fromPublisher(toFlowable().retry());
    }
    
    /**
     * Returns a Completable that retries this Completable in case of an error as long as the predicate
     * returns true.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code retry} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param predicate the predicate called when this emits an error with the repeat count and the latest exception
     * and should return true to retry.
     * @return the new Completable instance
     */
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Completable retry(BiPredicate<? super Integer, ? super Throwable> predicate) {
        return fromPublisher(toFlowable().retry(predicate));
    }

    /**
     * Returns a Completable that when this Completable emits an error, retries at most the given
     * number of times before giving up and emitting the last error.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code retry} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param times the number of times the returned Completable should retry this Completable
     * @return the new Completable instance
     * @throws IllegalArgumentException if times is negative
     */
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Completable retry(long times) {
        return fromPublisher(toFlowable().retry(times));
    }

    /**
     * Returns a Completable that when this Completable emits an error, calls the given predicate with
     * the latest exception to decide whether to resubscribe to this or not.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code retry} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param predicate the predicate that is called with the latest throwable and should return
     * true to indicate the returned Completable should resubscribe to this Completable.
     * @return the new Completable instance
     * @throws NullPointerException if predicate is null
     */
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Completable retry(Predicate<? super Throwable> predicate) {
        return fromPublisher(toFlowable().retry(predicate));
    }
    
    /**
     * Returns a Completable which given a Publisher and when this Completable emits an error, delivers
     * that error through an Observable and the Publisher should return a value indicating a retry in response
     * or a terminal event indicating a termination.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code retryWhen} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param handler the handler that receives an Observable delivering Throwables and should return a Publisher that
     * emits items to indicate retries or emits terminal events to indicate termination.
     * @return the new Completable instance
     * @throws NullPointerException if handler is null
     */
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Completable retryWhen(Function<? super Flowable<? extends Throwable>, ? extends Publisher<Object>> handler) {
        return fromPublisher(toFlowable().retryWhen(handler));
    }

    /**
     * Returns a Completable which first runs the other Completable
     * then this completable if the other completed normally.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code startWith} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param other the other completable to run first
     * @return the new Completable instance
     * @throws NullPointerException if other is null
     */
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Completable startWith(CompletableSource other) {
        Objects.requireNonNull(other, "other is null");
        return concat(other, this);
    }

    /**
     * Returns an NbpObservable which first delivers the events
     * of the other NbpObservable then runs this CompletableConsumable.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code startWith} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the value type
     * @param other the other NbpObservable to run first
     * @return the new NbpObservable instance
     * @throws NullPointerException if other is null
     */
    @SchedulerSupport(SchedulerSupport.NONE)
    public final <T> Observable<T> startWith(Observable<T> other) {
        Objects.requireNonNull(other, "other is null");
        return other.concatWith(this.<T>toObservable());
    }
    /**
     * Returns an Observable which first delivers the events
     * of the other Observable then runs this Completable.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code startWith} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the value type
     * @param other the other Observable to run first
     * @return the new Observable instance
     * @throws NullPointerException if other is null
     */
    @SchedulerSupport(SchedulerSupport.NONE)
    public final <T> Flowable<T> startWith(Publisher<T> other) {
        Objects.requireNonNull(other, "other is null");
        return this.<T>toFlowable().startWith(other);
    }
    
    /**
     * Subscribes to this CompletableConsumable and returns a Disposable which can be used to cancel
     * the subscription.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code subscribe} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @return the Disposable that allows cancelling the subscription
     */
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Disposable subscribe() {
        EmptyCompletableObserver s = new EmptyCompletableObserver();
        subscribe(s);
        return s;
    }
    
    @SchedulerSupport(SchedulerSupport.NONE)
    @Override
    public final void subscribe(CompletableObserver s) {
        Objects.requireNonNull(s, "s is null");
        try {
            
            s = RxJavaPlugins.onSubscribe(this, s);
            
            subscribeActual(s);
        } catch (NullPointerException ex) { // NOPMD
            throw ex;
        } catch (Throwable ex) {
            Exceptions.throwIfFatal(ex);
            RxJavaPlugins.onError(ex);
            throw toNpe(ex);
        }
    }
    
    /**
     * Implement this to handle the incoming CompletableObserver and
     * perform the business logic in your operator.
     * @param s the CompletableObserver instance, never null
     */
    protected abstract void subscribeActual(CompletableObserver s);

    /**
     * Subscribes to this Completable and calls back either the onError or onComplete functions.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code subscribe} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param onComplete the runnable that is called if the Completable completes normally
     * @param onError the consumer that is called if this Completable emits an error
     * @return the Disposable that can be used for cancelling the subscription asynchronously
     * @throws NullPointerException if either callback is null
     */
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Disposable subscribe(final Action onComplete, final Consumer<? super Throwable> onError) {
        Objects.requireNonNull(onError, "onError is null");
        Objects.requireNonNull(onComplete, "onComplete is null");
        
        CallbackCompletableObserver s = new CallbackCompletableObserver(onError, onComplete);
        subscribe(s);
        return s;
    }
    
    /**
     * Subscribes a non-backpressure Observer to this Completable instance which
     * will receive only an onError or onComplete event.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code subscribe} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the Observer's value type
     * @param observer the Observer instance, not null
     * @throws NullPointerException if s is null
     */
    @SchedulerSupport(SchedulerSupport.NONE)
    public final <T> void subscribe(final Observer<? super T> observer) {
        Objects.requireNonNull(observer, "s is null");
        
        ObserverCompletableObserver<T> os = new ObserverCompletableObserver<T>(observer);
        subscribe(os);
    }
    
    /**
     * Subscribes to this Completable and calls the given Action when this Completable
     * completes normally.
     * <p>
     * If this Completable emits an error, it is sent to RxJavaPlugins.onError and gets swallowed.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code subscribe} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param onComplete the runnable called when this Completable completes normally
     * @return the Disposable that allows cancelling the subscription
     */
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Disposable subscribe(final Action onComplete) {
        Objects.requireNonNull(onComplete, "onComplete is null");
        
        CallbackCompletableObserver s = new CallbackCompletableObserver(onComplete);
        subscribe(s);
        return s;
    }

    /**
     * Subscribes a reactive-streams Subscriber to this Completable instance which
     * will receive only an onError or onComplete event.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code subscribe} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the value type of the subscriber
     * @param s the reactive-streams Subscriber, not null
     * @throws NullPointerException if s is null
     */
    @SchedulerSupport(SchedulerSupport.NONE)
    public final <T> void subscribe(Subscriber<T> s) {
        Objects.requireNonNull(s, "s is null");
        SubscriberCompletableObserver<T> os = new SubscriberCompletableObserver<T>(s);
        subscribe(os);
    }

    /**
     * Returns a Completable which subscribes the child subscriber on the specified scheduler, making
     * sure the subscription side-effects happen on that specific thread of the scheduler.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code subscribeOn} operates on a {@link Scheduler} you specify.</dd>
     * </dl>
     * @param scheduler the Scheduler to subscribe on
     * @return the new Completable instance
     * @throws NullPointerException if scheduler is null
     */
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    public final Completable subscribeOn(final Scheduler scheduler) {
        Objects.requireNonNull(scheduler, "scheduler is null");
        
        return new CompletableSubscribeOn(this, scheduler);
    }

    /**
     * Returns a Completable that runs this Completable and emits a TimeoutException in case
     * this Completable doesn't complete within the given time.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code timeout} signals the TimeoutException on the {@code computation} {@link Scheduler}.</dd>
     * </dl>
     * @param timeout the timeout value
     * @param unit the timeout unit
     * @return the new Completable instance
     * @throws NullPointerException if unit is null
     */
    @SchedulerSupport(SchedulerSupport.COMPUTATION)
    public final Completable timeout(long timeout, TimeUnit unit) {
        return timeout0(timeout, unit, Schedulers.computation(), null);
    }
    
    /**
     * Returns a Completable that runs this Completable and switches to the other Completable
     * in case this Completable doesn't complete within the given time.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code timeout} subscribes to the other CompletableSource on 
     *  the {@code computation} {@link Scheduler}.</dd>
     * </dl>
     * @param timeout the timeout value
     * @param unit the timeout unit
     * @param other the other Completable instance to switch to in case of a timeout
     * @return the new Completable instance
     * @throws NullPointerException if unit or other is null
     */
    @SchedulerSupport(SchedulerSupport.COMPUTATION)
    public final Completable timeout(long timeout, TimeUnit unit, CompletableSource other) {
        Objects.requireNonNull(other, "other is null");
        return timeout0(timeout, unit, Schedulers.computation(), other);
    }
    
    /**
     * Returns a Completable that runs this Completable and emits a TimeoutException in case
     * this Completable doesn't complete within the given time while "waiting" on the specified
     * Scheduler.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code timeout} signals the TimeoutException on the {@link Scheduler} you specify.</dd>
     * </dl>
     * @param timeout the timeout value
     * @param unit the timeout unit
     * @param scheduler the scheduler to use to wait for completion
     * @return the new Completable instance
     * @throws NullPointerException if unit or scheduler is null
     */
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    public final Completable timeout(long timeout, TimeUnit unit, Scheduler scheduler) {
        return timeout0(timeout, unit, scheduler, null);
    }
    
    /**
     * Returns a Completable that runs this Completable and switches to the other Completable
     * in case this Completable doesn't complete within the given time while "waiting" on
     * the specified scheduler.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code timeout} subscribes to the other CompletableSource on 
     *  the {@link Scheduler} you specify.</dd>
     * </dl>
     * @param timeout the timeout value
     * @param unit the timeout unit
     * @param scheduler the scheduler to use to wait for completion
     * @param other the other Completable instance to switch to in case of a timeout
     * @return the new Completable instance
     * @throws NullPointerException if unit, scheduler or other is null
     */
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    public final Completable timeout(long timeout, TimeUnit unit, Scheduler scheduler, CompletableSource other) {
        Objects.requireNonNull(other, "other is null");
        return timeout0(timeout, unit, scheduler, other);
    }
    
    /**
     * Returns a Completable that runs this Completable and optionally switches to the other Completable
     * in case this Completable doesn't complete within the given time while "waiting" on
     * the specified scheduler.
     * @param timeout the timeout value
     * @param unit the timeout unit
     * @param scheduler the scheduler to use to wait for completion
     * @param other the other Completable instance to switch to in case of a timeout, 
     * if null a TimeoutException is emitted instead
     * @return the new Completable instance
     * @throws NullPointerException if unit or scheduler
     */
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    private Completable timeout0(long timeout, TimeUnit unit, Scheduler scheduler, CompletableSource other) {
        Objects.requireNonNull(unit, "unit is null");
        Objects.requireNonNull(scheduler, "scheduler is null");
        return new CompletableTimeout(this, timeout, unit, scheduler, other);
    }
    
    /**
     * Allows fluent conversion to another type via a function callback.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code to} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <U> the output type
     * @param converter the function called with this which should return some other value.
     * @return the converted value
     * @throws NullPointerException if converter is null
     */
    @SchedulerSupport(SchedulerSupport.NONE)
    public final <U> U to(Function<? super Completable, U> converter) {
        try {
            return converter.apply(this);
        } catch (Throwable ex) {
            Exceptions.throwIfFatal(ex);
            throw Exceptions.propagate(ex);
        }
    }

    /**
     * Returns an Observable which when subscribed to subscribes to this Completable and
     * relays the terminal events to the subscriber.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code toFlowable} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the value type
     * @return the new Observable created
     */
    @SchedulerSupport(SchedulerSupport.NONE)
    public final <T> Flowable<T> toFlowable() {
        return new CompletableToFlowable<T>(this);
    }
    
    /**
     * Returns an NbpObservable which when subscribed to subscribes to this Completable and
     * relays the terminal events to the subscriber.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code toObservable} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the value type
     * @return the new NbpObservable created
     */
    @SchedulerSupport(SchedulerSupport.NONE)
    public final <T> Observable<T> toObservable() {
        return new CompletableToObservable<T>(this);
    }
    
    /**
     * Convers this Completable into a Single which when this Completable completes normally,
     * calls the given supplier and emits its returned value through onSuccess.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code toSingle} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the value type
     * @param completionValueSupplier the value supplier called when this Completable completes normally
     * @return the new Single instance
     * @throws NullPointerException if completionValueSupplier is null
     */
    @SchedulerSupport(SchedulerSupport.NONE)
    public final <T> Single<T> toSingle(final Callable<? extends T> completionValueSupplier) {
        Objects.requireNonNull(completionValueSupplier, "completionValueSupplier is null");
        return new CompletableToSingle<T>(this, completionValueSupplier, null);
    }
    
    /**
     * Convers this Completable into a Single which when this Completable completes normally,
     * emits the given value through onSuccess.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code toSingleDefault} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the value type
     * @param completionValue the value to emit when this Completable completes normally
     * @return the new Single instance
     * @throws NullPointerException if completionValue is null
     */
    @SchedulerSupport(SchedulerSupport.NONE)
    public final <T> Single<T> toSingleDefault(final T completionValue) {
        Objects.requireNonNull(completionValue, "completionValue is null");
        return new CompletableToSingle<T>(this, null, completionValue);
    }
    
    /**
     * Returns a Completable which makes sure when a subscriber cancels the subscription, the 
     * dispose is called on the specified scheduler
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code unsubscribeOn} calls dispose() of the upstream on the {@link Scheduler} you specify.</dd>
     * </dl>
     * @param scheduler the target scheduler where to execute the cancellation
     * @return the new Completable instance
     * @throws NullPointerException if scheduler is null
     */
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    public final Completable unsubscribeOn(final Scheduler scheduler) {
        Objects.requireNonNull(scheduler, "scheduler is null");
        return new CompletableUnsubscribeOn(this, scheduler);
    }
}
