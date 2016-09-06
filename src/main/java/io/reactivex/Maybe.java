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

import java.util.concurrent.Callable;

import org.reactivestreams.*;

import io.reactivex.annotations.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.functions.*;
import io.reactivex.internal.functions.*;
import io.reactivex.internal.operators.flowable.*;
import io.reactivex.internal.operators.maybe.*;
import io.reactivex.internal.util.*;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.subscribers.TestSubscriber;

/**
 * Represents a deferred computation and emission of a maybe value or exception.
 * <p>
 * The main consumer type of Maybe is {@link MaybeObserver} whose methods are called
 * in a sequential fashion following this protocol:<br>
 * {@code onSubscribe (onSuccess | onError | onComplete)?}.
 * <p>
 * @param <T> the value type
 * @since 2.0
 */
public abstract class Maybe<T> implements MaybeSource<T> {

    /**
     * Runs multiple Maybe sources and signals the events of the first one that signals (cancelling
     * the rest).
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code amb} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the value type
     * @param sources the Iterable sequence of sources
     * @return the new Maybe instance
     */
    public static <T> Maybe<T> amb(final Iterable<? extends MaybeSource<? extends T>> sources) {
        ObjectHelper.requireNonNull(sources, "sources is null");
        return RxJavaPlugins.onAssembly(new MaybeAmbIterable<T>(sources));
    }
    
    /**
     * Runs multiple Maybe sources and signals the events of the first one that signals (cancelling
     * the rest).
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code amb} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the value type
     * @param sources the array of sources
     * @return the new Maybe instance
     */
    @SuppressWarnings("unchecked")
    public static <T> Maybe<T> ambArray(final MaybeSource<? extends T>... sources) {
        if (sources.length == 0) {
            return empty();
        }
        if (sources.length == 1) {
            return wrap((MaybeSource<T>)sources[0]);
        }
        return RxJavaPlugins.onAssembly(new MaybeAmbArray<T>(sources));
    }

    /**
     * Provides an API (via a cold Maybe) that bridges the reactive world with the callback-style world.
     * <p>
     * Example:
     * <pre><code>
     * Maybe.&lt;Event&gt;create(emitter -&gt; {
     *     Callback listener = new Callback() {
     *         &#64;Override
     *         public void onEvent(Event e) {
     *             if (e.isNothing()) {
     *                 emitter.onComplete();
     *             } else {
     *                 emitter.onSuccess(e);
     *             }
     *         }
     *         
     *         &#64;Override
     *         public void onFailure(Exception e) {
     *             emitter.onError(e);
     *         }
     *     };
     *     
     *     AutoCloseable c = api.someMethod(listener);
     *     
     *     emitter.setCancellable(c::close);
     *     
     * });
     * </code></pre>
     * <p>
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code create} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the value type
     * @param onSubscribe the emitter that is called when a MaybeObserver subscribes to the returned {@code Flowable}
     * @return the new Maybe instance
     * @see MaybeOnSubscribe
     * @see Cancellable
     */
    public static <T> Maybe<T> create(MaybeOnSubscribe<T> onSubscribe) {
        ObjectHelper.requireNonNull(onSubscribe, "onSubscribe is null");
        return RxJavaPlugins.onAssembly(new MaybeCreate<T>(onSubscribe));
    }
    
    /**
     * Concatenate the single values, in a non-overlapping fashion, of the MaybeSource sources provided by
     * an Iterable sequence. 
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code concat} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the value type
     * @param sources the Iterable sequence of MaybeSource instances
     * @return the new Flowable instance
     */
    public static <T> Flowable<T> concat(Iterable<? extends MaybeSource<? extends T>> sources) {
        ObjectHelper.requireNonNull(sources, "sources is null");
        return RxJavaPlugins.onAssembly(new MaybeConcatIterable<T>(sources));
    }
    
    /**
     * Returns a Flowable that emits the items emitted by two MaybeSources, one after the other.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.concat.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code concat} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * 
     * @param <T> the common value type
     * @param source1
     *            a MaybeSource to be concatenated
     * @param source2
     *            a MaybeSource to be concatenated
     * @return a Flowable that emits items emitted by the two source MaybeSources, one after the other.
     * @see <a href="http://reactivex.io/documentation/operators/concat.html">ReactiveX operators documentation: Concat</a>
     */
    @SuppressWarnings("unchecked")
    public static <T> Flowable<T> concat(MaybeSource<? extends T> source1, MaybeSource<? extends T> source2) {
        ObjectHelper.requireNonNull(source1, "source1 is null");
        ObjectHelper.requireNonNull(source2, "source2 is null");
        return concatArray(source1, source2);
    }

    /**
     * Returns a Flowable that emits the items emitted by three MaybeSources, one after the other.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.concat.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code concat} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T> the common value type
     * @param source1
     *            a MaybeSource to be concatenated
     * @param source2
     *            a MaybeSource to be concatenated
     * @param source3
     *            a MaybeSource to be concatenated
     * @return a Flowable that emits items emitted by the three source MaybeSources, one after the other.
     * @see <a href="http://reactivex.io/documentation/operators/concat.html">ReactiveX operators documentation: Concat</a>
     */
    @SuppressWarnings("unchecked")
    public static <T> Flowable<T> concat(
            MaybeSource<? extends T> source1, MaybeSource<? extends T> source2, MaybeSource<? extends T> source3) {
        ObjectHelper.requireNonNull(source1, "source1 is null");
        ObjectHelper.requireNonNull(source2, "source2 is null");
        ObjectHelper.requireNonNull(source3, "source3 is null");
        return concatArray(source1, source2, source3);
    }

    /**
     * Returns a Flowable that emits the items emitted by four MaybeSources, one after the other.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.concat.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code concat} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * 
     * @param <T> the common value type
     * @param source1
     *            a MaybeSource to be concatenated
     * @param source2
     *            a MaybeSource to be concatenated
     * @param source3
     *            a MaybeSource to be concatenated
     * @param source4
     *            a MaybeSource to be concatenated
     * @return a Flowable that emits items emitted by the four source MaybeSources, one after the other.
     * @see <a href="http://reactivex.io/documentation/operators/concat.html">ReactiveX operators documentation: Concat</a>
     */
    @SuppressWarnings("unchecked")
    public static <T> Flowable<T> concat(
            MaybeSource<? extends T> source1, MaybeSource<? extends T> source2, MaybeSource<? extends T> source3, MaybeSource<? extends T> source4) {
        ObjectHelper.requireNonNull(source1, "source1 is null");
        ObjectHelper.requireNonNull(source2, "source2 is null");
        ObjectHelper.requireNonNull(source3, "source3 is null");
        ObjectHelper.requireNonNull(source4, "source4 is null");
        return concatArray(source1, source2, source3, source4);
    }

    /**
     * Concatenate the single values, in a non-overlapping fashion, of the MaybeSource sources provided by
     * a Publisher sequence. 
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code concat} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the value type
     * @param sources the Publisher of MaybeSource instances
     * @return the new Flowable instance
     */
    public static <T> Flowable<T> concat(Publisher<? extends MaybeSource<? extends T>> sources) {
        return concat(sources, 2);
    }

    /**
     * Concatenate the single values, in a non-overlapping fashion, of the MaybeSource sources provided by
     * a Publisher sequence. 
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code concat} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the value type
     * @param sources the Publisher of MaybeSource instances
     * @param prefetch the number of MaybeSources to prefetch from the Publisher
     * @return the new Flowable instance
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static <T> Flowable<T> concat(Publisher<? extends MaybeSource<? extends T>> sources, int prefetch) {
        ObjectHelper.requireNonNull(sources, "sources is null");
        ObjectHelper.verifyPositive(prefetch, "prefetch");
        return RxJavaPlugins.onAssembly(new FlowableConcatMap(sources, MaybeToPublisher.instance(), prefetch, ErrorMode.IMMEDIATE));
    }

    /**
     * Concatenate the single values, in a non-overlapping fashion, of the MaybeSource sources in the array. 
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code concat} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the value type
     * @param sources the Publisher of MaybeSource instances
     * @return the new Flowable instance
     */
    @SuppressWarnings("unchecked")
    public static <T> Flowable<T> concatArray(MaybeSource<? extends T>... sources) {
        ObjectHelper.requireNonNull(sources, "sources is null");
        if (sources.length == 0) {
            return Flowable.empty();
        }
        if (sources.length == 1) {
            return RxJavaPlugins.onAssembly(new MaybeToFlowable<T>((MaybeSource<T>)sources[0]));
        }
        return RxJavaPlugins.onAssembly(new MaybeConcatArray<T>(sources));
    }

    /**
     * Calls a Callable for each individual MaybeObserver to return the actual MaybeSource source to
     * be subscribe to.
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code defer} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the value type
     * @param maybeSupplier the Callable that is called for each individual MaybeObserver and
     * returns a MaybeSource instance to subscribe to
     * @return the new Maybe instance
     */
    public static <T> Maybe<T> defer(final Callable<? extends MaybeSource<? extends T>> maybeSupplier) {
        ObjectHelper.requireNonNull(maybeSupplier, "maybeSupplier is null");
        return RxJavaPlugins.onAssembly(new MaybeDefer<T>(maybeSupplier));
    }

    /**
     * Returns a (singleton) Maybe instance that calls {@link MaybeObserver#onComplete onComplete}
     * immediately.
     * <p>
     * <img width="640" height="190" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/empty.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code empty} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the value type
     * @return the new Maybe instance
     */
    @SuppressWarnings("unchecked")
    public static <T> Maybe<T> empty() {
        return RxJavaPlugins.onAssembly((Maybe<T>)MaybeEmpty.INSTANCE);
    }
    
    /**
     * Returns a Maybe that invokes a subscriber's {@link MaybeObserver#onError onError} method when the
     * subscriber subscribes to it.
     * <p>
     * <img width="640" height="190" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.error.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code error} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * 
     * @param exception
     *            the particular Throwable to pass to {@link MaybeObserver#onError onError}
     * @param <T>
     *            the type of the item (ostensibly) emitted by the Maybe
     * @return a Maybe that invokes the subscriber's {@link MaybeObserver#onError onError} method when
     *         the subscriber subscribes to it
     * @see <a href="http://reactivex.io/documentation/operators/empty-never-throw.html">ReactiveX operators documentation: Throw</a>
     */
    public static <T> Maybe<T> error(Throwable exception) {
        ObjectHelper.requireNonNull(exception, "exception is null");
        return RxJavaPlugins.onAssembly(new MaybeError<T>(exception));
    }
    
    /**
     * Returns a Maybe that invokes an {@link Observer}'s {@link MaybeObserver#onError onError} method when the
     * Observer subscribes to it.
     * <p>
     * <img width="640" height="190" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/error.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code error} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * 
     * @param supplier
     *            a Callable factory to return a Throwable for each individual Subscriber
     * @param <T>
     *            the type of the items (ostensibly) emitted by the Maybe
     * @return a Maybe that invokes the {@link MaybeObserver}'s {@link MaybeObserver#onError onError} method when
     *         the MaybeObserver subscribes to it
     * @see <a href="http://reactivex.io/documentation/operators/empty-never-throw.html">ReactiveX operators documentation: Throw</a>
     */
    public static <T> Maybe<T> error(Callable<? extends Throwable> supplier) {
        ObjectHelper.requireNonNull(supplier, "errorSupplier is null");
        return RxJavaPlugins.onAssembly(new MaybeErrorCallable<T>(supplier));
    }
    
    /**
     * Returns a Maybe instance that runs the given Action for each subscriber and
     * emits either its exception or simply completes.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code fromAction} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the target type
     * @param run the runnable to run for each subscriber
     * @return the new Maybe instance
     * @throws NullPointerException if run is null
     */
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <T> Maybe<T> fromAction(final Action run) {
        ObjectHelper.requireNonNull(run, "run is null");
        return RxJavaPlugins.onAssembly(new MaybeFromAction<T>(run));
    }
    
    /**
     * Returns a {@link Maybe} that invokes passed function and emits its result for each new MaybeObserver that subscribes
     * while considering {@code null} value from the callable as indication for valueless completion.
     * <p>
     * Allows you to defer execution of passed function until MaybeObserver subscribes to the {@link Maybe}.
     * It makes passed function "lazy".
     * Result of the function invocation will be emitted by the {@link Maybe}.
     * <dl>
     *   <dt><b>Scheduler:</b></dt>
     *   <dd>{@code fromCallable} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param callable
     *         function which execution should be deferred, it will be invoked when MaybeObserver will subscribe to the {@link Maybe}.
     * @param <T>
     *         the type of the item emitted by the {@link Maybe}.
     * @return a {@link Maybe} whose {@link MaybeObserver}s' subscriptions trigger an invocation of the given function.
     */
    public static <T> Maybe<T> fromCallable(final Callable<? extends T> callable) {
        ObjectHelper.requireNonNull(callable, "callable is null");
        return RxJavaPlugins.onAssembly(new MaybeFromCallable<T>(callable));
    }
    
    /**
     * Returns a Maybe instance that runs the given Action for each subscriber and
     * emits either its exception or simply completes.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code fromRunnable} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the target type
     * @param run the runnable to run for each subscriber
     * @return the new Maybe instance
     * @throws NullPointerException if run is null
     */
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <T> Maybe<T> fromRunnable(final Runnable run) {
        ObjectHelper.requireNonNull(run, "run is null");
        return RxJavaPlugins.onAssembly(new MaybeFromRunnable<T>(run));
    }

    
    /**
     * Returns a {@code Maybe} that emits a specified item.
     * <p>
     * <img width="640" height="310" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.just.png" alt="">
     * <p>
     * To convert any object into a {@code Single} that emits that object, pass that object into the
     * {@code just} method.
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code just} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * 
     * @param item
     *            the item to emit
     * @param <T>
     *            the type of that item
     * @return a {@code Maybe} that emits {@code item}
     * @see <a href="http://reactivex.io/documentation/operators/just.html">ReactiveX operators documentation: Just</a>
     */
    public static <T> Maybe<T> just(T item) {
        ObjectHelper.requireNonNull(item, "item is null");
        return RxJavaPlugins.onAssembly(new MaybeJust<T>(item));
    }

    /**
     * Merges an Iterable sequence of MaybeSource instances into a single Flowable sequence,
     * running all MaybeSources at once.
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code merge} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the common and resulting value type
     * @param sources the Iterable sequence of MaybeSource sources
     * @return the new Flowable instance
     * @since 2.0
     */
    public static <T> Flowable<T> merge(Iterable<? extends MaybeSource<? extends T>> sources) {
        return merge(Flowable.fromIterable(sources));
    }

    /**
     * Merges a Flowable sequence of MaybeSource instances into a single Flowable sequence,
     * running all MaybeSources at once.
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code merge} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the common and resulting value type
     * @param sources the Flowable sequence of MaybeSource sources
     * @return the new Flowable instance
     * @since 2.0
     */
    public static <T> Flowable<T> merge(Publisher<? extends MaybeSource<? extends T>> sources) {
        return merge(sources, Integer.MAX_VALUE);
    }
    
    /**
     * Merges a Flowable sequence of MaybeSource instances into a single Flowable sequence,
     * running at most maxConcurrency MaybeSources at once.
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code merge} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the common and resulting value type
     * @param sources the Flowable sequence of MaybeSource sources
     * @param maxConcurrency the maximum number of concurrently running MaybeSources
     * @return the new Flowable instance
     * @since 2.0
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static <T> Flowable<T> merge(Publisher<? extends MaybeSource<? extends T>> sources, int maxConcurrency) {
        return RxJavaPlugins.onAssembly(new FlowableFlatMap(sources, MaybeToPublisher.instance(), false, maxConcurrency, Flowable.bufferSize()));
    }

    /**
     * Flattens a {@code Single} that emits a {@code Single} into a single {@code Single} that emits the item
     * emitted by the nested {@code Single}, without any transformation.
     * <p>
     * <img width="640" height="370" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.merge.oo.png" alt="">
     * <p>
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code merge} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T> the value type of the sources and the output
     * @param source
     *            a {@code Single} that emits a {@code Single}
     * @return a {@code Single} that emits the item that is the result of flattening the {@code Single} emitted
     *         by {@code source}
     * @see <a href="http://reactivex.io/documentation/operators/merge.html">ReactiveX operators documentation: Merge</a>
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static <T> Maybe<T> merge(MaybeSource<? extends MaybeSource<? extends T>> source) {
        return new MaybeFlatten(source, Functions.identity());
    }
    
    /**
     * Flattens two Singles into a single Observable, without any transformation.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.merge.png" alt="">
     * <p>
     * You can combine items emitted by multiple Singles so that they appear as a single Observable, by
     * using the {@code merge} method.
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code merge} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * 
     * @param <T> the common value type
     * @param source1
     *            a Single to be merged
     * @param source2
     *            a Single to be merged
     * @return a Flowable that emits all of the items emitted by the source Singles
     * @see <a href="http://reactivex.io/documentation/operators/merge.html">ReactiveX operators documentation: Merge</a>
     */
    @SuppressWarnings("unchecked")
    public static <T> Flowable<T> merge(
            MaybeSource<? extends T> source1, MaybeSource<? extends T> source2
     ) {
        ObjectHelper.requireNonNull(source1, "source1 is null");
        ObjectHelper.requireNonNull(source2, "source2 is null");
        return mergeArray(source1, source2);
    }
    
    /**
     * Flattens three Singles into a single Observable, without any transformation.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.merge.png" alt="">
     * <p>
     * You can combine items emitted by multiple Singles so that they appear as a single Observable, by using
     * the {@code merge} method.
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code merge} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * 
     * @param <T> the common value type
     * @param source1
     *            a Single to be merged
     * @param source2
     *            a Single to be merged
     * @param source3
     *            a Single to be merged
     * @return a Flowable that emits all of the items emitted by the source Singles
     * @see <a href="http://reactivex.io/documentation/operators/merge.html">ReactiveX operators documentation: Merge</a>
     */
    @SuppressWarnings("unchecked")
    public static <T> Flowable<T> merge(
            MaybeSource<? extends T> source1, MaybeSource<? extends T> source2,
            MaybeSource<? extends T> source3
     ) {
        ObjectHelper.requireNonNull(source1, "source1 is null");
        ObjectHelper.requireNonNull(source2, "source2 is null");
        ObjectHelper.requireNonNull(source3, "source3 is null");
        return mergeArray(source1, source2, source3);
    }
    
    /**
     * Flattens four Singles into a single Observable, without any transformation.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.merge.png" alt="">
     * <p>
     * You can combine items emitted by multiple Singles so that they appear as a single Observable, by using
     * the {@code merge} method.
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code merge} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * 
     * @param <T> the common value type
     * @param source1
     *            a Single to be merged
     * @param source2
     *            a Single to be merged
     * @param source3
     *            a Single to be merged
     * @param source4
     *            a Single to be merged
     * @return a Flowable that emits all of the items emitted by the source Singles
     * @see <a href="http://reactivex.io/documentation/operators/merge.html">ReactiveX operators documentation: Merge</a>
     */
    @SuppressWarnings("unchecked")
    public static <T> Flowable<T> merge(
            MaybeSource<? extends T> source1, MaybeSource<? extends T> source2,
            MaybeSource<? extends T> source3, MaybeSource<? extends T> source4
     ) {
        ObjectHelper.requireNonNull(source1, "source1 is null");
        ObjectHelper.requireNonNull(source2, "source2 is null");
        ObjectHelper.requireNonNull(source3, "source3 is null");
        ObjectHelper.requireNonNull(source4, "source4 is null");
        return mergeArray(source1, source2, source3, source4);
    }

    /**
     * Merges an array sequence of MaybeSource instances into a single Flowable sequence,
     * running all MaybeSources at once.
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code merge} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the common and resulting value type
     * @param sources the array sequence of MaybeSource sources
     * @return the new Flowable instance
     * @since 2.0
     */
    @SuppressWarnings("unchecked")
    public static <T> Flowable<T> mergeArray(MaybeSource<? extends T>... sources) {
        ObjectHelper.requireNonNull(sources, "sources is null");
        if (sources.length == 0) {
            return Flowable.empty();
        }
        if (sources.length == 1) {
            return RxJavaPlugins.onAssembly(new MaybeToFlowable<T>((MaybeSource<T>)sources[0]));
        }
        return RxJavaPlugins.onAssembly(new MaybeMergeArray<T>(sources));
    }


    /**
     * Returns a Maybe that never sends any items or notifications to an {@link MaybeObserver}.
     * <p>
     * <img width="640" height="185" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/never.png" alt="">
     * <p>
     * This Maybe is useful primarily for testing purposes.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code never} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * 
     * @param <T>
     *            the type of items (not) emitted by the Maybe
     * @return a Maybe that never emits any items or sends any notifications to an {@link MaybeObserver}
     * @see <a href="http://reactivex.io/documentation/operators/empty-never-throw.html">ReactiveX operators documentation: Never</a>
     */
    @SuppressWarnings("unchecked")
    public static <T> Maybe<T> never() {
        return RxJavaPlugins.onAssembly((Maybe<T>)MaybeNever.INSTANCE);
    }
    
    /**
     * <strong>Advanced use only:</strong> creates a Maybe instance without 
     * any safeguards by using a callback that is called with a MaybeObserver.
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code unsafeCreate} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the value type
     * @param onSubscribe the function that is called with the subscribing MaybeObserver
     * @return the new Maybe instance
     */
    public static <T> Maybe<T> unsafeCreate(MaybeSource<T> onSubscribe) {
        if (onSubscribe instanceof Maybe) {
            throw new IllegalArgumentException("unsafeCreate(Maybe) should be upgraded");
        }
        ObjectHelper.requireNonNull(onSubscribe, "onSubscribe is null");
        return RxJavaPlugins.onAssembly(new MaybeUnsafeCreate<T>(onSubscribe));
    }
    
    /**
     * Wraps a MaybeSource instance into a new Maybe instance if not already a Maybe
     * instance.
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code wrap} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the value type
     * @param source the source to wrap
     * @return the Maybe wrapper or the source cast to Maybe (if possible)
     */
    public static <T> Maybe<T> wrap(MaybeSource<T> source) {
        if (source instanceof Maybe) {
            return RxJavaPlugins.onAssembly((Maybe<T>)source);
        }
        ObjectHelper.requireNonNull(source, "onSubscribe is null");
        return RxJavaPlugins.onAssembly(new MaybeUnsafeCreate<T>(source));
    }
    
    // ------------------------------------------------------------------
    // Instance methods
    // ------------------------------------------------------------------

    /**
     * Waits in a blocking fashion until the current Maybe signals a success value (which is returned) or
     * defaultValue if completed or an exception (which is propagated).
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code blockingGet} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @return the success value
     */
    public T blockingGet() {
        return MaybeAwait.get(this, null);
    }
    
    /**
     * Waits in a blocking fashion until the current Maybe signals a success value (which is returned) or
     * defaultValue if completed or an exception (which is propagated).
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code blockingGet} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param defaultValue the default item to return if this Maybe is empty
     * @return the success value
     */
    public T blockingGet(T defaultValue) {
        ObjectHelper.requireNonNull(defaultValue, "defaultValue is null");
        return MaybeAwait.get(this, defaultValue);
    }
    
    /**
     * Casts the success value of the current Maybe into the target type or signals a
     * ClassCastException if not compatible.
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code cast} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <U> the target type
     * @param clazz the type token to use for casting the success result from the current Maybe
     * @return the new Maybe instance
     * @since 2.0
     */
    public final <U> Maybe<U> cast(final Class<? extends U> clazz) {
        ObjectHelper.requireNonNull(clazz, "clazz is null");
        return map(Functions.castFunction(clazz));
    }

    /**
     * Transform a Maybe by applying a particular Transformer function to it.
     * <p>
     * This method operates on the Maybe itself whereas {@link #lift} operates on the Maybe's MaybeObservers.
     * <p>
     * If the operator you are creating is designed to act on the individual item emitted by a Maybe, use
     * {@link #lift}. If your operator is designed to transform the source Maybe as a whole (for instance, by
     * applying a particular set of existing RxJava operators to it) use {@code compose}.
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code compose} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * 
     * @param <R> the value type of the Maybe returned by the transformer function
     * @param transformer
     *            implements the function that transforms the source Maybe
     * @return a Maybe, transformed by the transformer function
     * @see <a href="https://github.com/ReactiveX/RxJava/wiki/Implementing-Your-Own-Operators">RxJava wiki: Implementing Your Own Operators</a>
     */
    public final <R> Maybe<R> compose(Function<? super Maybe<T>, ? extends MaybeSource<R>> transformer) {
        return wrap(to(transformer));
    }

    /**
     * Returns a Maybe that is based on applying a specified function to the item emitted by the source Maybe,
     * where that function returns a MaybeSource.
     * <p>
     * <img width="640" height="300" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.flatMap.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code flatMap} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * <p>Note that flatMap and concatMap for Maybe is the same operation.
     * @param <R> the result value type
     * @param mapper
     *            a function that, when applied to the item emitted by the source Maybe, returns a MaybeSource
     * @return the Maybe returned from {@code func} when applied to the item emitted by the source Maybe
     * @see <a href="http://reactivex.io/documentation/operators/flatmap.html">ReactiveX operators documentation: FlatMap</a>
     */
    public final <R> Maybe<R> concatMap(Function<? super T, ? extends MaybeSource<? extends R>> mapper) {
        ObjectHelper.requireNonNull(mapper, "mapper is null");
        return RxJavaPlugins.onAssembly(new MaybeFlatten<T, R>(this, mapper));
    }

    /**
     * Registers an {@link Action} to be called when this Maybe invokes either
     * {@link MaybeObserver#onComplete onSuccess},
     * {@link MaybeObserver#onComplete onComplete} or {@link MaybeObserver#onError onError}.
     * <p>
     * <img width="640" height="310" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/finallyDo.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code doAfterTerminate} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param onAfterTerminate
     *            an {@link Action} to be invoked when the source Maybe finishes
     * @return a Maybe that emits the same items as the source Maybe, then invokes the
     *         {@link Action}
     * @see <a href="http://reactivex.io/documentation/operators/do.html">ReactiveX operators documentation: Do</a>
     */
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Maybe<T> doAfterTerminate(Action onAfterTerminate) {
        return RxJavaPlugins.onAssembly(new MaybePeek<T>(this, 
                Functions.emptyConsumer(), // onSubscribe
                Functions.emptyConsumer(), // onSuccess
                Functions.emptyConsumer(), // onError
                Functions.EMPTY_ACTION,    // onComplete
                ObjectHelper.requireNonNull(onAfterTerminate, "onAfterTerminate is null"),
                Functions.EMPTY_ACTION     // dispose
        ));
    }

    /**
     * Calls the shared runnable if a MaybeObserver subscribed to the current Single
     * disposes the common Disposable it received via onSubscribe.
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code doOnSubscribe} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param onDispose the runnable called when the subscription is cancelled (disposed)
     * @return the new Maybe instance
     */
    public final Maybe<T> doOnDispose(Action onDispose) {
        return RxJavaPlugins.onAssembly(new MaybePeek<T>(this, 
                Functions.emptyConsumer(), // onSubscribe
                Functions.emptyConsumer(), // onSuccess
                Functions.emptyConsumer(), // onError
                Functions.EMPTY_ACTION,    // onComplete
                Functions.EMPTY_ACTION,    // (onSuccess | onError | onComplete) after
                ObjectHelper.requireNonNull(onDispose, "onDispose is null")
        ));
    }

    /**
     * Modifies the source Maybe so that it invokes an action when it calls {@code onCompleted}.
     * <p>
     * <img width="640" height="305" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/doOnComplete.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code doOnComplete} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * 
     * @param onComplete
     *            the action to invoke when the source Maybe calls {@code onCompleted}
     * @return the new Maybe with the side-effecting behavior applied
     * @see <a href="http://reactivex.io/documentation/operators/do.html">ReactiveX operators documentation: Do</a>
     */
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Maybe<T> doOnComplete(Action onComplete) {
        return RxJavaPlugins.onAssembly(new MaybePeek<T>(this, 
                Functions.emptyConsumer(), // onSubscribe
                Functions.emptyConsumer(), // onSuccess
                Functions.emptyConsumer(), // onError
                ObjectHelper.requireNonNull(onComplete, "onComplete is null"),
                Functions.EMPTY_ACTION,    // (onSuccess | onError | onComplete)
                Functions.EMPTY_ACTION     // dispose
        ));
    }

    /**
     * Calls the shared consumer with the error sent via onError for each
     * MaybeObserver that subscribes to the current Single.
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code doOnSubscribe} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param onError the consumer called with the success value of onError
     * @return the new Maybe instance
     */
    public final Maybe<T> doOnError(Consumer<? super Throwable> onError) {
        return RxJavaPlugins.onAssembly(new MaybePeek<T>(this, 
                Functions.emptyConsumer(), // onSubscribe
                Functions.emptyConsumer(), // onSuccess
                ObjectHelper.requireNonNull(onError, "onError is null"),
                Functions.EMPTY_ACTION,    // onComplete
                Functions.EMPTY_ACTION,    // (onSuccess | onError | onComplete)
                Functions.EMPTY_ACTION     // dispose
        ));
    }
    
    /**
     * Calls the given onEvent callback with the (success value, null) for an onSuccess, (null, throwable) for
     * an onError or (null, null) for an onComplete signal from this Maybe before delivering said
     * signal to the downstream.
     * <p>
     * Exceptions thrown from the callback will override the event so the downstream receives the
     * error instead of the original signal.
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code doOnEvent} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param onEvent the callback to call with the terminal event tuple
     * @return the new Maybe instance
     */
    public final Maybe<T> doOnEvent(BiConsumer<? super T, ? super Throwable> onEvent) {
        ObjectHelper.requireNonNull(onEvent, "onEvent is null");
        return RxJavaPlugins.onAssembly(new MaybeDoOnEvent<T>(this, onEvent));
    }

    /**
     * Calls the shared consumer with the Disposable sent through the onSubscribe for each
     * MaybeObserver that subscribes to the current Single.
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code doOnSubscribe} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param onSubscribe the consumer called with the Disposable sent via onSubscribe
     * @return the new Maybe instance
     */
    public final Maybe<T> doOnSubscribe(Consumer<? super Disposable> onSubscribe) {
        return RxJavaPlugins.onAssembly(new MaybePeek<T>(this, 
                ObjectHelper.requireNonNull(onSubscribe, "onSubscribe is null"),
                Functions.emptyConsumer(), // onSuccess
                Functions.emptyConsumer(), // onError
                Functions.EMPTY_ACTION,    // onComplete
                Functions.EMPTY_ACTION,    // (onSuccess | onError | onComplete)
                Functions.EMPTY_ACTION     // dispose
        ));
    }
    
    /**
     * Calls the shared consumer with the success value sent via onSuccess for each
     * MaybeObserver that subscribes to the current Single.
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code doOnSubscribe} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param onSuccess the consumer called with the success value of onSuccess
     * @return the new Maybe instance
     */
    public final Maybe<T> doOnSuccess(Consumer<? super T> onSuccess) {
        return RxJavaPlugins.onAssembly(new MaybePeek<T>(this, 
                Functions.emptyConsumer(), // onSubscribe
                ObjectHelper.requireNonNull(onSuccess, "onSubscribe is null"),
                Functions.emptyConsumer(), // onError
                Functions.EMPTY_ACTION,    // onComplete
                Functions.EMPTY_ACTION,    // (onSuccess | onError | onComplete)
                Functions.EMPTY_ACTION     // dispose
        ));
    }

    /**
     * Filters the success item of the Maybe via a predicate function and emitting it if the predicate
     * returns true, completing otherwise.
     * <p>
     * <img width="640" height="310" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/filter.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code filter} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * 
     * @param predicate
     *            a function that evaluates the item emitted by the source Maybe, returning {@code true}
     *            if it passes the filter
     * @return a Maybe that emit the item emitted by the source Maybe that the filter
     *         evaluates as {@code true}
     * @see <a href="http://reactivex.io/documentation/operators/filter.html">ReactiveX operators documentation: Filter</a>
     */
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Maybe<T> filter(Predicate<? super T> predicate) {
        ObjectHelper.requireNonNull(predicate, "predicate is null");
        return RxJavaPlugins.onAssembly(new MaybeFilter<T>(this, predicate));
    }

    /**
     * Returns a Maybe that is based on applying a specified function to the item emitted by the source Maybe,
     * where that function returns a MaybeSource.
     * <p>
     * <img width="640" height="300" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.flatMap.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code flatMap} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * <p>Note that flatMap and concatMap for Maybe is the same operation.
     * 
     * @param <R> the result value type
     * @param mapper
     *            a function that, when applied to the item emitted by the source Maybe, returns a MaybeSource
     * @return the Maybe returned from {@code func} when applied to the item emitted by the source Maybe
     * @see <a href="http://reactivex.io/documentation/operators/flatmap.html">ReactiveX operators documentation: FlatMap</a>
     */
    public final <R> Maybe<R> flatMap(Function<? super T, ? extends MaybeSource<? extends R>> mapper) {
        ObjectHelper.requireNonNull(mapper, "mapper is null");
        return RxJavaPlugins.onAssembly(new MaybeFlatten<T, R>(this, mapper));
    }

    /**
     * Maps the onSuccess, onError or onComplete signals of this Maybe into MaybeSource and emits that
     * MaybeSource's signals
     * <p>
     * <img width="640" height="410" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/mergeMap.nce.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code flatMap} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * 
     * @param <R>
     *            the result type
     * @param onSuccessMapper
     *            a function that returns a MaybeSource to merge for the success item emitted by this Maybe
     * @param onErrorMapper
     *            a function that returns a MaybeSource to merge for an onError notification from this Maybe
     * @param onCompleteSupplier
     *            a function that returns a MaybeSource to merge for an onCompleted notification this Maybe
     * @return the new Maybe instance
     * @see <a href="http://reactivex.io/documentation/operators/flatmap.html">ReactiveX operators documentation: FlatMap</a>
     */
    @SchedulerSupport(SchedulerSupport.NONE)
    public final <R> Maybe<R> flatMap(
            Function<? super T, ? extends MaybeSource<? extends R>> onSuccessMapper, 
            Function<? super Throwable, ? extends MaybeSource<? extends R>> onErrorMapper, 
            Callable<? extends MaybeSource<? extends R>> onCompleteSupplier) {
        ObjectHelper.requireNonNull(onSuccessMapper, "onSuccessMapper is null");
        ObjectHelper.requireNonNull(onErrorMapper, "onErrorMapper is null");
        ObjectHelper.requireNonNull(onCompleteSupplier, "onCompleteSupplier is null");
        return new MaybeFlatMapNotification<T, R>(this, onSuccessMapper, onErrorMapper, onCompleteSupplier);
    }

    /**
     * Returns a Observable that is based on applying a specified function to the item emitted by the source Maybe,
     * where that function returns a ObservableSource.
     * <p>
     * <img width="640" height="300" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.flatMap.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code flatMap} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * 
     * @param <R> the result value type
     * @param mapper
     *            a function that, when applied to the item emitted by the source Maybe, returns a ObservableSource
     * @return the Observable returned from {@code func} when applied to the item emitted by the source Maybe
     * @see <a href="http://reactivex.io/documentation/operators/flatmap.html">ReactiveX operators documentation: FlatMap</a>
     */
    public final <R> Observable<R> flatMapObservable(Function<? super T, ? extends ObservableSource<? extends R>> mapper) {
        return toObservable().flatMap(mapper);
    }
    
    /**
     * Returns a Flowable that emits items based on applying a specified function to the item emitted by the
     * source Maybe, where that function returns a Publisher.
     * <p>
     * <img width="640" height="305" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.flatMapObservable.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code flatMapObservable} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * 
     * @param <R> the result value type
     * @param mapper
     *            a function that, when applied to the item emitted by the source Maybe, returns an
     *            Flowable
     * @return the Flowable returned from {@code func} when applied to the item emitted by the source Maybe
     * @see <a href="http://reactivex.io/documentation/operators/flatmap.html">ReactiveX operators documentation: FlatMap</a>
     */
    public final <R> Flowable<R> flatMapPublisher(Function<? super T, ? extends Publisher<? extends R>> mapper) {
        return toFlowable().flatMap(mapper);
    }
    
    /**
     * Returns a {@link Completable} that completes based on applying a specified function to the item emitted by the
     * source {@link Single}, where that function returns a {@link Completable}.
     * <p>
     * <img width="640" height="305" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.flatMapCompletable.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code flatMapCompletable} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param mapper
     *            a function that, when applied to the item emitted by the source Single, returns a
     *            Completable
     * @return the Completable returned from {@code func} when applied to the item emitted by the source Single
     * @see <a href="http://reactivex.io/documentation/operators/flatmap.html">ReactiveX operators documentation: FlatMap</a>
     * @since 2.0
     */
    public final Completable flatMapCompletable(final Function<? super T, ? extends Completable> mapper) {
        ObjectHelper.requireNonNull(mapper, "mapper is null");
        return RxJavaPlugins.onAssembly(new MaybeFlatMapCompletable<T>(this, mapper));
    }
    
    /**
     * Ignores the item emitted by the source Maybe and only calls {@code onCompleted} or {@code onError}.
     * <p>
     * <img width="640" height="305" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/ignoreElements.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code ignoreElement} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * 
     * @return an empty Maybe that only calls {@code onComplete} or {@code onError}, based on which one is
     *         called by the source Maybe
     * @see <a href="http://reactivex.io/documentation/operators/ignoreelements.html">ReactiveX operators documentation: IgnoreElements</a>
     */
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Maybe<T> ignoreElement() {
        return RxJavaPlugins.onAssembly(new MaybeIgnoreElement<T>(this));
    }

    
    /**
     * Lifts a function to the current Maybe and returns a new Maybe that when subscribed to will pass the
     * values of the current Maybe through the MaybeOperator function.
     * <p>
     * In other words, this allows chaining TaskExecutors together on a Maybe for acting on the values within
     * the Maybe.
     * <p>
     * {@code task.map(...).filter(...).lift(new OperatorA()).lift(new OperatorB(...)).subscribe() }
     * <p>
     * If the operator you are creating is designed to act on the item emitted by a source Maybe, use
     * {@code lift}. If your operator is designed to transform the source Maybe as a whole (for instance, by
     * applying a particular set of existing RxJava operators to it) use {@link #compose}.
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code lift} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * 
     * @param <R> the downstream's value type (output)
     * @param lift
     *            the MaybeOperator that implements the Maybe-operating function to be applied to the source Maybe
     * @return a Maybe that is the result of applying the lifted Operator to the source Maybe
     * @see <a href="https://github.com/ReactiveX/RxJava/wiki/Implementing-Your-Own-Operators">RxJava wiki: Implementing Your Own Operators</a>
     */
    public final <R> Maybe<R> lift(final MaybeOperator<? extends R, ? super T> lift) {
        ObjectHelper.requireNonNull(lift, "onLift is null");
        return RxJavaPlugins.onAssembly(new MaybeLift<T, R>(this, lift));
    }
    
    /**
     * Returns a Maybe that applies a specified function to the item emitted by the source Maybe and
     * emits the result of this function application.
     * <p>
     * <img width="640" height="305" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.map.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code map} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <R> the result value type
     * @param mapper
     *            a function to apply to the item emitted by the Maybe
     * @return a Maybe that emits the item from the source Maybe, transformed by the specified function
     * @see <a href="http://reactivex.io/documentation/operators/map.html">ReactiveX operators documentation: Map</a>
     */
    public final <R> Maybe<R> map(Function<? super T, ? extends R> mapper) {
        ObjectHelper.requireNonNull(mapper, "mapper is null");
        return RxJavaPlugins.onAssembly(new MaybeMap<T, R>(this, mapper));
    }
    
    
    /**
     * Wraps a Maybe to emit its item (or notify of its error) on a specified {@link Scheduler},
     * asynchronously.
     * <p>
     * <img width="640" height="305" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.observeOn.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>you specify which {@link Scheduler} this operator will use</dd>
     * </dl>
     * 
     * @param scheduler
     *            the {@link Scheduler} to notify subscribers on
     * @return the new Maybe instance that its subscribers are notified on the specified
     *         {@link Scheduler}
     * @see <a href="http://reactivex.io/documentation/operators/observeon.html">ReactiveX operators documentation: ObserveOn</a>
     * @see <a href="http://www.grahamlea.com/2014/07/rxjava-threading-examples/">RxJava Threading Examples</a>
     * @see #subscribeOn
     */
    public final Maybe<T> observeOn(final Scheduler scheduler) {
        ObjectHelper.requireNonNull(scheduler, "scheduler is null");
        return RxJavaPlugins.onAssembly(new MaybeObserveOn<T>(this, scheduler));
    }
    /**
     * Calls the specified converter function with the current Maybe instance 
     * during assembly time and returns its result.
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code to} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <R> the result type
     * @param convert the function that is called with the current Maybe instance during
     *                assembly time that should return some value to be the result
     *                 
     * @return the value returned by the convert function
     */
    public final <R> R to(Function<? super Maybe<T>, R> convert) {
        try {
            return convert.apply(this);
        } catch (Throwable ex) {
            Exceptions.throwIfFatal(ex);
            throw ExceptionHelper.wrapOrThrow(ex);
        }
    }

    /**
     * Converts this Maybe into an Completable instance composing cancellation
     * through and dropping a success value if emitted.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code create} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @return the new Completable instance
     */
    public final Completable toCompletable() {
        return RxJavaPlugins.onAssembly(new MaybeToCompletable<T>(this));
    }

    /**
     * Converts this Maybe into a backpressure-aware Flowable instance composing cancellation
     * through.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code create} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @return the new Flowable instance
     */
    public final Flowable<T> toFlowable() {
        return RxJavaPlugins.onAssembly(new MaybeToFlowable<T>(this));
    }
    
    /**
     * Converts this Maybe into an Observable instance composing cancellation
     * through.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code create} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @return the new Observable instance
     */
    public final Observable<T> toObservable() {
        return RxJavaPlugins.onAssembly(new MaybeToObservable<T>(this));
    }

    /**
     * Converts this Maybe into an Single instance composing cancellation
     * through and turing an empty Maybe into a signal of NoSuchElementException.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code create} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param defaultValue the default item to signal in Single if this Maybe is empty
     * @return the new Single instance
     */
    public final Single<T> toSingle(T defaultValue) {
            ObjectHelper.requireNonNull(defaultValue, "defaultValue is null");
            return RxJavaPlugins.onAssembly(new MaybeToSingle<T>(this, defaultValue));
        }

    /**
     * Converts this Maybe into an Single instance composing cancellation
     * through and turing an empty Maybe into a signal of NoSuchElementException.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code create} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @return the new Single instance
     */
    public final Single<T> toSingle() {
        return RxJavaPlugins.onAssembly(new MaybeToSingle<T>(this, null));
    }

    /**
     * Subscribes to a Maybe and ignores {@code onSuccess} and {@code onComplete} emissions. 
     * <p>
     * If the Maybe emits an error, it is routed to the RxJavaPlugins.onError handler. 
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code subscribe} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * 
     * @return a {@link Disposable} reference with which the caller can stop receiving items before
     *         the Maybe has finished sending them
     * @see <a href="http://reactivex.io/documentation/operators/subscribe.html">ReactiveX operators documentation: Subscribe</a>
     */
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Disposable subscribe() {
        return subscribe(Functions.emptyConsumer(), Functions.ERROR_CONSUMER, Functions.EMPTY_ACTION);
    }

    /**
     * Subscribes to a Maybe and provides a callback to handle the items it emits.
     * <p>
     * If the Flowable emits an error, it is routed to the RxJavaPlugins.onError handler. 
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code subscribe} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * 
     * @param onSuccess
     *             the {@code Consumer<T>} you have designed to accept a success value from the Maybe
     * @return a {@link Disposable} reference with which the caller can stop receiving items before
     *         the Maybe has finished sending them
     * @throws NullPointerException
     *             if {@code onSuccess} is null
     * @see <a href="http://reactivex.io/documentation/operators/subscribe.html">ReactiveX operators documentation: Subscribe</a>
     */
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Disposable subscribe(Consumer<? super T> onSuccess) {
        return subscribe(onSuccess, Functions.ERROR_CONSUMER, Functions.EMPTY_ACTION);
    }

    /**
     * Subscribes to a Maybe and provides callbacks to handle the items it emits and any error
     * notification it issues.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code subscribe} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * 
     * @param onSuccess
     *             the {@code Consumer<T>} you have designed to accept a success value from the Maybe
     * @param onError
     *             the {@code Consumer<Throwable>} you have designed to accept any error notification from the
     *             Maybe
     * @return a {@link Subscription} reference with which the caller can stop receiving items before
     *         the Maybe has finished sending them
     * @see <a href="http://reactivex.io/documentation/operators/subscribe.html">ReactiveX operators documentation: Subscribe</a>
     * @throws IllegalArgumentException
     *             if {@code onSuccess} is null, or
     *             if {@code onError} is null
     */
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Disposable subscribe(Consumer<? super T> onSuccess, Consumer<? super Throwable> onError) {
        return subscribe(onSuccess, onError, Functions.EMPTY_ACTION);
    }

    /**
     * Subscribes to a Maybe and provides callbacks to handle the items it emits and any error or
     * completion notification it issues.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code subscribe} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * 
     * @param onSuccess
     *             the {@code Consumer<T>} you have designed to accept a success value from the Maybe
     * @param onError
     *             the {@code Consumer<Throwable>} you have designed to accept any error notification from the
     *             Maybe
     * @param onComplete
     *             the {@code Action} you have designed to accept a completion notification from the
     *             Maybe
     * @return a {@link Disposable} reference with which the caller can stop receiving items before
     *         the Maybe has finished sending them
     * @throws IllegalArgumentException
     *             if {@code onSuccess} is null, or
     *             if {@code onError} is null, or
     *             if {@code onComplete} is null
     * @see <a href="http://reactivex.io/documentation/operators/subscribe.html">ReactiveX operators documentation: Subscribe</a>
     */
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Disposable subscribe(Consumer<? super T> onSuccess, Consumer<? super Throwable> onError, 
            Action onComplete) {
        return subscribeWith(new MaybeCallbackObserver<T>(onSuccess, onError, onComplete));
    }
    
    
    
    @Override
    public final void subscribe(MaybeObserver<? super T> observer) {
        ObjectHelper.requireNonNull(observer, "observer is null");
        
        observer = RxJavaPlugins.onSubscribe(this, observer);
        
        ObjectHelper.requireNonNull(observer, "observer returned by the RxJavaPlugins hook is null");

        try {
            subscribeActual(observer);
        } catch (NullPointerException ex) {
            throw ex;
        } catch (Throwable ex) {
            Exceptions.throwIfFatal(ex);
            NullPointerException npe = new NullPointerException("subscribeActual failed");
            npe.initCause(ex);
            throw npe;
        }
    }
    
    /**
     * Override this method in subclasses to handle the incoming MaybeObservers.
     * @param observer the MaybeObserver to handle, not null
     */
    protected abstract void subscribeActual(MaybeObserver<? super T> observer);

    /**
     * Asynchronously subscribes subscribers to this Maybe on the specified {@link Scheduler}.
     * <p>
     * <img width="640" height="305" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.subscribeOn.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>you specify which {@link Scheduler} this operator will use</dd>
     * </dl>
     * 
     * @param scheduler
     *            the {@link Scheduler} to perform subscription actions on
     * @return the new Maybe instance that its subscriptions happen on the specified {@link Scheduler}
     * @see <a href="http://reactivex.io/documentation/operators/subscribeon.html">ReactiveX operators documentation: SubscribeOn</a>
     * @see <a href="http://www.grahamlea.com/2014/07/rxjava-threading-examples/">RxJava Threading Examples</a>
     * @see #observeOn
     */
    public final Maybe<T> subscribeOn(Scheduler scheduler) {
        ObjectHelper.requireNonNull(scheduler, "scheduler is null");
        return RxJavaPlugins.onAssembly(new MaybeSubscribeOn<T>(this, scheduler));
    }

    /**
     * Subscribes a given MaybeObserver (subclass) to this Maybe and returns the given
     * MaybeObserver as is.
     * <p>Usage example:
     * <pre><code>
     * Maybe<Integer> source = Maybe.just(1);
     * CompositeDisposable composite = new CompositeDisposable();
     * 
     * MaybeObserver&lt;Integer> ms = new MaybeObserver&lt;>() {
     *     // ...
     * };
     * 
     * composite.add(source.subscribeWith(ms));
     * </code></pre>
     * @param <E> the type of the MaybeObserver to use and return
     * @param observer the MaybeObserver (subclass) to use and return, not null
     * @return the input {@code subscriber}
     * @throws NullPointerException if {@code subscriber} is null
     */
    public final <E extends MaybeObserver<? super T>> E subscribeWith(E observer) {
        subscribe(observer);
        return observer;
    }


    // ------------------------------------------------------------------
    // Test helper
    // ------------------------------------------------------------------

    /**
     * Creates a TestSubscriber and subscribes
     * it to this Maybe.
     * @return the new TestSubscriber instance
     */
    public final TestSubscriber<T> test() {
        TestSubscriber<T> ts = new TestSubscriber<T>();
        toFlowable().subscribe(ts);
        return ts;
    }

    /**
     * Creates a TestSubscriber optionally in cancelled state, then subscribes it to this Maybe.
     * @param cancelled if true, the TestSubscriber will be cancelled before subscribing to this
     * Maybe.
     * @return the new TestSubscriber instance
     */
    public final TestSubscriber<T> test(boolean cancelled) {
        TestSubscriber<T> ts = new TestSubscriber<T>();

        if (cancelled) {
            ts.cancel();
        }

        toFlowable().subscribe(ts);
        return ts;
    }
}
