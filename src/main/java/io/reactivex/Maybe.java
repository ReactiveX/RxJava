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

package io.reactivex;

import java.util.NoSuchElementException;
import java.util.concurrent.*;

import org.reactivestreams.*;

import io.reactivex.annotations.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.functions.*;
import io.reactivex.internal.functions.*;
import io.reactivex.internal.fuseable.*;
import io.reactivex.internal.observers.BlockingMultiObserver;
import io.reactivex.internal.operators.flowable.*;
import io.reactivex.internal.operators.maybe.*;
import io.reactivex.internal.util.*;
import io.reactivex.observers.TestObserver;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.schedulers.Schedulers;

/**
 * Represents a deferred computation and emission of a maybe value or exception.
 * <p>
 * <img width="640" height="370" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/maybe.png" alt="">
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
     * @param sources the Iterable sequence of sources. A subscription to each source will
     *            occur in the same order as in the Iterable.
     * @return the new Maybe instance
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <T> Maybe<T> amb(final Iterable<? extends MaybeSource<? extends T>> sources) {
        ObjectHelper.requireNonNull(sources, "sources is null");
        return RxJavaPlugins.onAssembly(new MaybeAmb<T>(null, sources));
    }

    /**
     * Runs multiple Maybe sources and signals the events of the first one that signals (cancelling
     * the rest).
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code ambArray} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the value type
     * @param sources the array of sources. A subscription to each source will
     *            occur in the same order as in the array.
     * @return the new Maybe instance
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @SuppressWarnings("unchecked")
    public static <T> Maybe<T> ambArray(final MaybeSource<? extends T>... sources) {
        if (sources.length == 0) {
            return empty();
        }
        if (sources.length == 1) {
            return wrap((MaybeSource<T>)sources[0]);
        }
        return RxJavaPlugins.onAssembly(new MaybeAmb<T>(sources, null));
    }

    /**
     * Concatenate the single values, in a non-overlapping fashion, of the MaybeSource sources provided by
     * an Iterable sequence.
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The returned {@code Flowable} honors the backpressure of the downstream consumer.</dd>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code concat} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the value type
     * @param sources the Iterable sequence of MaybeSource instances
     * @return the new Flowable instance
     */
    @BackpressureSupport(BackpressureKind.FULL)
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <T> Flowable<T> concat(Iterable<? extends MaybeSource<? extends T>> sources) {
        ObjectHelper.requireNonNull(sources, "sources is null");
        return RxJavaPlugins.onAssembly(new MaybeConcatIterable<T>(sources));
    }

    /**
     * Returns a Flowable that emits the items emitted by two MaybeSources, one after the other.
     * <p>
     * <img width="640" height="422" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.concat.png" alt="">
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The returned {@code Flowable} honors the backpressure of the downstream consumer.</dd>
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
    @BackpressureSupport(BackpressureKind.FULL)
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @SuppressWarnings("unchecked")
    public static <T> Flowable<T> concat(MaybeSource<? extends T> source1, MaybeSource<? extends T> source2) {
        ObjectHelper.requireNonNull(source1, "source1 is null");
        ObjectHelper.requireNonNull(source2, "source2 is null");
        return concatArray(source1, source2);
    }

    /**
     * Returns a Flowable that emits the items emitted by three MaybeSources, one after the other.
     * <p>
     * <img width="640" height="422" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.concat.png" alt="">
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The returned {@code Flowable} honors the backpressure of the downstream consumer.</dd>
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
    @BackpressureSupport(BackpressureKind.FULL)
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
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
     * <img width="640" height="422" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.concat.png" alt="">
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The returned {@code Flowable} honors the backpressure of the downstream consumer.</dd>
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
    @BackpressureSupport(BackpressureKind.FULL)
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
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
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The returned {@code Flowable} honors the backpressure of the downstream consumer and
     *  expects the {@code Publisher} to honor backpressure as well. If the sources {@code Publisher}
     *  violates this, a {@link io.reactivex.exceptions.MissingBackpressureException} is signalled.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code concat} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the value type
     * @param sources the Publisher of MaybeSource instances
     * @return the new Flowable instance
     */
    @BackpressureSupport(BackpressureKind.FULL)
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <T> Flowable<T> concat(Publisher<? extends MaybeSource<? extends T>> sources) {
        return concat(sources, 2);
    }

    /**
     * Concatenate the single values, in a non-overlapping fashion, of the MaybeSource sources provided by
     * a Publisher sequence.
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The returned {@code Flowable} honors the backpressure of the downstream consumer and
     *  expects the {@code Publisher} to honor backpressure as well. If the sources {@code Publisher}
     *  violates this, a {@link io.reactivex.exceptions.MissingBackpressureException} is signalled.</dd>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code concat} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the value type
     * @param sources the Publisher of MaybeSource instances
     * @param prefetch the number of MaybeSources to prefetch from the Publisher
     * @return the new Flowable instance
     */
    @BackpressureSupport(BackpressureKind.FULL)
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static <T> Flowable<T> concat(Publisher<? extends MaybeSource<? extends T>> sources, int prefetch) {
        ObjectHelper.requireNonNull(sources, "sources is null");
        ObjectHelper.verifyPositive(prefetch, "prefetch");
        return RxJavaPlugins.onAssembly(new FlowableConcatMapPublisher(sources, MaybeToPublisher.instance(), prefetch, ErrorMode.IMMEDIATE));
    }

    /**
     * Concatenate the single values, in a non-overlapping fashion, of the MaybeSource sources in the array.
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The returned {@code Flowable} honors the backpressure of the downstream consumer.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code concatArray} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the value type
     * @param sources the array of MaybeSource instances
     * @return the new Flowable instance
     */
    @BackpressureSupport(BackpressureKind.FULL)
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
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
     * Concatenates a variable number of MaybeSource sources and delays errors from any of them
     * till all terminate.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/concat.png" alt="">
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The operator honors backpressure from downstream.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code concatArrayDelayError} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param sources the array of sources
     * @param <T> the common base value type
     * @return the new Flowable instance
     * @throws NullPointerException if sources is null
     */
    @SuppressWarnings("unchecked")
    @BackpressureSupport(BackpressureKind.FULL)
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <T> Flowable<T> concatArrayDelayError(MaybeSource<? extends T>... sources) {
        if (sources.length == 0) {
            return Flowable.empty();
        } else
        if (sources.length == 1) {
            return RxJavaPlugins.onAssembly(new MaybeToFlowable<T>((MaybeSource<T>)sources[0]));
        }
        return RxJavaPlugins.onAssembly(new MaybeConcatArrayDelayError<T>(sources));
    }

    /**
     * Concatenates a sequence of MaybeSource eagerly into a single stream of values.
     * <p>
     * Eager concatenation means that once a subscriber subscribes, this operator subscribes to all of the
     * source MaybeSources. The operator buffers the value emitted by these MaybeSources and then drains them
     * in order, each one after the previous one completes.
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The operator honors backpressure from downstream.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This method does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the value type
     * @param sources a sequence of MaybeSources that need to be eagerly concatenated
     * @return the new Flowable instance with the specified concatenation behavior
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @BackpressureSupport(BackpressureKind.FULL)
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <T> Flowable<T> concatArrayEager(MaybeSource<? extends T>... sources) {
        return Flowable.fromArray(sources).concatMapEager((Function)MaybeToPublisher.instance());
    }

    /**
     * Concatenates the Iterable sequence of MaybeSources into a single sequence by subscribing to each MaybeSource,
     * one after the other, one at a time and delays any errors till the all inner MaybeSources terminate.
     *
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The operator honors backpressure from downstream.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code concatDelayError} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T> the common element base type
     * @param sources the Iterable sequence of MaybeSources
     * @return the new Flowable with the concatenating behavior
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @BackpressureSupport(BackpressureKind.FULL)
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <T> Flowable<T> concatDelayError(Iterable<? extends MaybeSource<? extends T>> sources) {
        ObjectHelper.requireNonNull(sources, "sources is null");
        return Flowable.fromIterable(sources).concatMapDelayError((Function)MaybeToPublisher.instance());
    }

    /**
     * Concatenates the Publisher sequence of Publishers into a single sequence by subscribing to each inner Publisher,
     * one after the other, one at a time and delays any errors till the all inner and the outer Publishers terminate.
     *
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>{@code concatDelayError} fully supports backpressure.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code concatDelayError} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T> the common element base type
     * @param sources the Publisher sequence of Publishers
     * @return the new Publisher with the concatenating behavior
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @BackpressureSupport(BackpressureKind.FULL)
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <T> Flowable<T> concatDelayError(Publisher<? extends MaybeSource<? extends T>> sources) {
        return Flowable.fromPublisher(sources).concatMapDelayError((Function)MaybeToPublisher.instance());
    }

    /**
     * Concatenates a sequence of MaybeSources eagerly into a single stream of values.
     * <p>
     * Eager concatenation means that once a subscriber subscribes, this operator subscribes to all of the
     * source MaybeSources. The operator buffers the values emitted by these MaybeSources and then drains them
     * in order, each one after the previous one completes.
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>Backpressure is honored towards the downstream.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This method does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the value type
     * @param sources a sequence of MaybeSource that need to be eagerly concatenated
     * @return the new Flowable instance with the specified concatenation behavior
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @BackpressureSupport(BackpressureKind.FULL)
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <T> Flowable<T> concatEager(Iterable<? extends MaybeSource<? extends T>> sources) {
        return Flowable.fromIterable(sources).concatMapEager((Function)MaybeToPublisher.instance());
    }

    /**
     * Concatenates a Publisher sequence of Publishers eagerly into a single stream of values.
     * <p>
     * Eager concatenation means that once a subscriber subscribes, this operator subscribes to all of the
     * emitted source Publishers as they are observed. The operator buffers the values emitted by these
     * Publishers and then drains them in order, each one after the previous one completes.
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>Backpressure is honored towards the downstream and the outer Publisher is
     *  expected to support backpressure. Violating this assumption, the operator will
     *  signal {@link io.reactivex.exceptions.MissingBackpressureException}.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This method does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the value type
     * @param sources a sequence of Publishers that need to be eagerly concatenated
     * @return the new Publisher instance with the specified concatenation behavior
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @BackpressureSupport(BackpressureKind.FULL)
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <T> Flowable<T> concatEager(Publisher<? extends MaybeSource<? extends T>> sources) {
        return Flowable.fromPublisher(sources).concatMapEager((Function)MaybeToPublisher.instance());
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
     * @param onSubscribe the emitter that is called when a MaybeObserver subscribes to the returned {@code Maybe}
     * @return the new Maybe instance
     * @see MaybeOnSubscribe
     * @see Cancellable
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <T> Maybe<T> create(MaybeOnSubscribe<T> onSubscribe) {
        ObjectHelper.requireNonNull(onSubscribe, "onSubscribe is null");
        return RxJavaPlugins.onAssembly(new MaybeCreate<T>(onSubscribe));
    }

    /**
     * Calls a Callable for each individual MaybeObserver to return the actual MaybeSource source to
     * be subscribed to.
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code defer} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the value type
     * @param maybeSupplier the Callable that is called for each individual MaybeObserver and
     * returns a MaybeSource instance to subscribe to
     * @return the new Maybe instance
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
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
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @SuppressWarnings("unchecked")
    public static <T> Maybe<T> empty() {
        return RxJavaPlugins.onAssembly((Maybe<T>)MaybeEmpty.INSTANCE);
    }

    /**
     * Returns a Maybe that invokes a subscriber's {@link MaybeObserver#onError onError} method when the
     * subscriber subscribes to it.
     * <p>
     * <img width="640" height="447" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.error.png" alt="">
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
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <T> Maybe<T> error(Throwable exception) {
        ObjectHelper.requireNonNull(exception, "exception is null");
        return RxJavaPlugins.onAssembly(new MaybeError<T>(exception));
    }

    /**
     * Returns a Maybe that invokes a {@link MaybeObserver}'s {@link MaybeObserver#onError onError} method when the
     * MaybeObserver subscribes to it.
     * <p>
     * <img width="640" height="190" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/error.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code error} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param supplier
     *            a Callable factory to return a Throwable for each individual MaybeObserver
     * @param <T>
     *            the type of the items (ostensibly) emitted by the Maybe
     * @return a Maybe that invokes the {@link MaybeObserver}'s {@link MaybeObserver#onError onError} method when
     *         the MaybeObserver subscribes to it
     * @see <a href="http://reactivex.io/documentation/operators/empty-never-throw.html">ReactiveX operators documentation: Throw</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
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
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <T> Maybe<T> fromAction(final Action run) {
        ObjectHelper.requireNonNull(run, "run is null");
        return RxJavaPlugins.onAssembly(new MaybeFromAction<T>(run));
    }

    /**
     * Wraps a CompletableSource into a Maybe.
     *
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code fromCompletable} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the target type
     * @param completableSource the CompletableSource to convert from
     * @return the new Maybe instance
     * @throws NullPointerException if completable is null
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <T> Maybe<T> fromCompletable(CompletableSource completableSource) {
        ObjectHelper.requireNonNull(completableSource, "completableSource is null");
        return RxJavaPlugins.onAssembly(new MaybeFromCompletable<T>(completableSource));
    }

    /**
     * Wraps a SingleSource into a Maybe.
     *
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code fromSingle} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the target type
     * @param singleSource the SingleSource to convert from
     * @return the new Maybe instance
     * @throws NullPointerException if single is null
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <T> Maybe<T> fromSingle(SingleSource<T> singleSource) {
        ObjectHelper.requireNonNull(singleSource, "singleSource is null");
        return RxJavaPlugins.onAssembly(new MaybeFromSingle<T>(singleSource));
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
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <T> Maybe<T> fromCallable(final Callable<? extends T> callable) {
        ObjectHelper.requireNonNull(callable, "callable is null");
        return RxJavaPlugins.onAssembly(new MaybeFromCallable<T>(callable));
    }

    /**
     * Converts a {@link Future} into a Maybe, treating a null result as an indication of emptiness.
     * <p>
     * <img width="640" height="315" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/from.Future.png" alt="">
     * <p>
     * You can convert any object that supports the {@link Future} interface into a Maybe that emits the
     * return value of the {@link Future#get} method of that object, by passing the object into the {@code from}
     * method.
     * <p>
     * <em>Important note:</em> This Maybe is blocking; you cannot dispose it.
     * <p>
     * Unlike 1.x, cancelling the Maybe won't cancel the future. If necessary, one can use composition to achieve the
     * cancellation effect: {@code futureMaybe.doOnDispose(() -> future.cancel(true));}.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code fromFuture} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param future
     *            the source {@link Future}
     * @param <T>
     *            the type of object that the {@link Future} returns, and also the type of item to be emitted by
     *            the resulting Maybe
     * @return a Maybe that emits the item from the source {@link Future}
     * @see <a href="http://reactivex.io/documentation/operators/from.html">ReactiveX operators documentation: From</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <T> Maybe<T> fromFuture(Future<? extends T> future) {
        ObjectHelper.requireNonNull(future, "future is null");
        return RxJavaPlugins.onAssembly(new MaybeFromFuture<T>(future, 0L, null));
    }

    /**
     * Converts a {@link Future} into a Maybe, with a timeout on the Future.
     * <p>
     * <img width="640" height="315" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/from.Future.png" alt="">
     * <p>
     * You can convert any object that supports the {@link Future} interface into a Maybe that emits the
     * return value of the {@link Future#get} method of that object, by passing the object into the {@code fromFuture}
     * method.
     * <p>
     * Unlike 1.x, cancelling the Maybe won't cancel the future. If necessary, one can use composition to achieve the
     * cancellation effect: {@code futureMaybe.doOnCancel(() -> future.cancel(true));}.
     * <p>
     * <em>Important note:</em> This Maybe is blocking on the thread it gets subscribed on; you cannot dispose it.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code fromFuture} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param future
     *            the source {@link Future}
     * @param timeout
     *            the maximum time to wait before calling {@code get}
     * @param unit
     *            the {@link TimeUnit} of the {@code timeout} argument
     * @param <T>
     *            the type of object that the {@link Future} returns, and also the type of item to be emitted by
     *            the resulting Maybe
     * @return a Maybe that emits the item from the source {@link Future}
     * @see <a href="http://reactivex.io/documentation/operators/from.html">ReactiveX operators documentation: From</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <T> Maybe<T> fromFuture(Future<? extends T> future, long timeout, TimeUnit unit) {
        ObjectHelper.requireNonNull(future, "future is null");
        ObjectHelper.requireNonNull(unit, "unit is null");
        return RxJavaPlugins.onAssembly(new MaybeFromFuture<T>(future, timeout, unit));
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
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <T> Maybe<T> fromRunnable(final Runnable run) {
        ObjectHelper.requireNonNull(run, "run is null");
        return RxJavaPlugins.onAssembly(new MaybeFromRunnable<T>(run));
    }


    /**
     * Returns a {@code Maybe} that emits a specified item.
     * <p>
     * <img width="640" height="485" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.just.png" alt="">
     * <p>
     * To convert any object into a {@code Maybe} that emits that object, pass that object into the
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
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <T> Maybe<T> just(T item) {
        ObjectHelper.requireNonNull(item, "item is null");
        return RxJavaPlugins.onAssembly(new MaybeJust<T>(item));
    }

    /**
     * Merges an Iterable sequence of MaybeSource instances into a single Flowable sequence,
     * running all MaybeSources at once.
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The operator honors backpressure from downstream.</dd>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code merge} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the common and resulting value type
     * @param sources the Iterable sequence of MaybeSource sources
     * @return the new Flowable instance
     */
    @BackpressureSupport(BackpressureKind.FULL)
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <T> Flowable<T> merge(Iterable<? extends MaybeSource<? extends T>> sources) {
        return merge(Flowable.fromIterable(sources));
    }

    /**
     * Merges a Flowable sequence of MaybeSource instances into a single Flowable sequence,
     * running all MaybeSources at once.
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The operator honors backpressure from downstream.</dd>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code merge} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the common and resulting value type
     * @param sources the Flowable sequence of MaybeSource sources
     * @return the new Flowable instance
     */
    @BackpressureSupport(BackpressureKind.FULL)
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <T> Flowable<T> merge(Publisher<? extends MaybeSource<? extends T>> sources) {
        return merge(sources, Integer.MAX_VALUE);
    }

    /**
     * Merges a Flowable sequence of MaybeSource instances into a single Flowable sequence,
     * running at most maxConcurrency MaybeSources at once.
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The operator honors backpressure from downstream.</dd>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code merge} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the common and resulting value type
     * @param sources the Flowable sequence of MaybeSource sources
     * @param maxConcurrency the maximum number of concurrently running MaybeSources
     * @return the new Flowable instance
     */
    @BackpressureSupport(BackpressureKind.FULL)
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static <T> Flowable<T> merge(Publisher<? extends MaybeSource<? extends T>> sources, int maxConcurrency) {
        ObjectHelper.requireNonNull(sources, "source is null");
        ObjectHelper.verifyPositive(maxConcurrency, "maxConcurrency");
        return RxJavaPlugins.onAssembly(new FlowableFlatMapPublisher(sources, MaybeToPublisher.instance(), false, maxConcurrency, Flowable.bufferSize()));
    }

    /**
     * Flattens a {@code MaybeSource} that emits a {@code MaybeSource} into a single {@code MaybeSource} that emits the item
     * emitted by the nested {@code MaybeSource}, without any transformation.
     * <p>
     * <img width="640" height="393" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.merge.oo.png" alt="">
     * <p>
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code merge} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T> the value type of the sources and the output
     * @param source
     *            a {@code MaybeSource} that emits a {@code MaybeSource}
     * @return a {@code Maybe} that emits the item that is the result of flattening the {@code MaybeSource} emitted
     *         by {@code source}
     * @see <a href="http://reactivex.io/documentation/operators/merge.html">ReactiveX operators documentation: Merge</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static <T> Maybe<T> merge(MaybeSource<? extends MaybeSource<? extends T>> source) {
        ObjectHelper.requireNonNull(source, "source is null");
        return RxJavaPlugins.onAssembly(new MaybeFlatten(source, Functions.identity()));
    }

    /**
     * Flattens two MaybeSources into a single Flowable, without any transformation.
     * <p>
     * <img width="640" height="483" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.merge.png" alt="">
     * <p>
     * You can combine items emitted by multiple MaybeSources so that they appear as a single Flowable, by
     * using the {@code merge} method.
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The operator honors backpressure from downstream.</dd>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code merge} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T> the common value type
     * @param source1
     *            a MaybeSource to be merged
     * @param source2
     *            a MaybeSource to be merged
     * @return a Flowable that emits all of the items emitted by the source MaybeSources
     * @see <a href="http://reactivex.io/documentation/operators/merge.html">ReactiveX operators documentation: Merge</a>
     */
    @BackpressureSupport(BackpressureKind.FULL)
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @SuppressWarnings("unchecked")
    public static <T> Flowable<T> merge(
            MaybeSource<? extends T> source1, MaybeSource<? extends T> source2
     ) {
        ObjectHelper.requireNonNull(source1, "source1 is null");
        ObjectHelper.requireNonNull(source2, "source2 is null");
        return mergeArray(source1, source2);
    }

    /**
     * Flattens three MaybeSources into a single Flowable, without any transformation.
     * <p>
     * <img width="640" height="483" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.merge.png" alt="">
     * <p>
     * You can combine items emitted by multiple MaybeSources so that they appear as a single Flowable, by using
     * the {@code merge} method.
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The operator honors backpressure from downstream.</dd>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code merge} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T> the common value type
     * @param source1
     *            a MaybeSource to be merged
     * @param source2
     *            a MaybeSource to be merged
     * @param source3
     *            a MaybeSource to be merged
     * @return a Flowable that emits all of the items emitted by the source MaybeSources
     * @see <a href="http://reactivex.io/documentation/operators/merge.html">ReactiveX operators documentation: Merge</a>
     */
    @BackpressureSupport(BackpressureKind.FULL)
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
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
     * Flattens four MaybeSources into a single Flowable, without any transformation.
     * <p>
     * <img width="640" height="483" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.merge.png" alt="">
     * <p>
     * You can combine items emitted by multiple MaybeSources so that they appear as a single Flowable, by using
     * the {@code merge} method.
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The operator honors backpressure from downstream.</dd>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code merge} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T> the common value type
     * @param source1
     *            a MaybeSource to be merged
     * @param source2
     *            a MaybeSource to be merged
     * @param source3
     *            a MaybeSource to be merged
     * @param source4
     *            a MaybeSource to be merged
     * @return a Flowable that emits all of the items emitted by the source MaybeSources
     * @see <a href="http://reactivex.io/documentation/operators/merge.html">ReactiveX operators documentation: Merge</a>
     */
    @BackpressureSupport(BackpressureKind.FULL)
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
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
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The operator honors backpressure from downstream.</dd>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code mergeArray} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the common and resulting value type
     * @param sources the array sequence of MaybeSource sources
     * @return the new Flowable instance
     */
    @BackpressureSupport(BackpressureKind.FULL)
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
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
     * Flattens an array of MaybeSources into one Flowable, in a way that allows a Subscriber to receive all
     * successfully emitted items from each of the source MaybeSources without being interrupted by an error
     * notification from one of them.
     * <p>
     * This behaves like {@link #merge(Publisher)} except that if any of the merged MaybeSources notify of an
     * error via {@link Subscriber#onError onError}, {@code mergeDelayError} will refrain from propagating that
     * error notification until all of the merged MaybeSources have finished emitting items.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/mergeDelayError.png" alt="">
     * <p>
     * Even if multiple merged MaybeSources send {@code onError} notifications, {@code mergeDelayError} will only
     * invoke the {@code onError} method of its Subscribers once.
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The operator honors backpressure from downstream.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code mergeArrayDelayError} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T> the common element base type
     * @param sources
     *            the Iterable of MaybeSources
     * @return a Flowable that emits items that are the result of flattening the items emitted by the
     *         MaybeSources in the Iterable
     * @see <a href="http://reactivex.io/documentation/operators/merge.html">ReactiveX operators documentation: Merge</a>
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @BackpressureSupport(BackpressureKind.FULL)
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <T> Flowable<T> mergeArrayDelayError(MaybeSource<? extends T>... sources) {
        if (sources.length == 0) {
            return Flowable.empty();
        }
        return Flowable.fromArray(sources).flatMap((Function)MaybeToPublisher.instance(), true, sources.length);
    }


    /**
     * Flattens an Iterable of MaybeSources into one Flowable, in a way that allows a Subscriber to receive all
     * successfully emitted items from each of the source MaybeSources without being interrupted by an error
     * notification from one of them.
     * <p>
     * This behaves like {@link #merge(Publisher)} except that if any of the merged MaybeSources notify of an
     * error via {@link Subscriber#onError onError}, {@code mergeDelayError} will refrain from propagating that
     * error notification until all of the merged MaybeSources have finished emitting items.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/mergeDelayError.png" alt="">
     * <p>
     * Even if multiple merged MaybeSources send {@code onError} notifications, {@code mergeDelayError} will only
     * invoke the {@code onError} method of its Subscribers once.
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The operator honors backpressure from downstream.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code mergeDelayError} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T> the common element base type
     * @param sources
     *            the Iterable of MaybeSources
     * @return a Flowable that emits items that are the result of flattening the items emitted by the
     *         MaybeSources in the Iterable
     * @see <a href="http://reactivex.io/documentation/operators/merge.html">ReactiveX operators documentation: Merge</a>
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @BackpressureSupport(BackpressureKind.FULL)
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <T> Flowable<T> mergeDelayError(Iterable<? extends MaybeSource<? extends T>> sources) {
        return Flowable.fromIterable(sources).flatMap((Function)MaybeToPublisher.instance(), true);
    }


    /**
     * Flattens a Publisher that emits MaybeSources into one Publisher, in a way that allows a Subscriber to
     * receive all successfully emitted items from all of the source Publishers without being interrupted by
     * an error notification from one of them.
     * <p>
     * This behaves like {@link #merge(Publisher)} except that if any of the merged Publishers notify of an
     * error via {@link Subscriber#onError onError}, {@code mergeDelayError} will refrain from propagating that
     * error notification until all of the merged Publishers have finished emitting items.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/mergeDelayError.png" alt="">
     * <p>
     * Even if multiple merged Publishers send {@code onError} notifications, {@code mergeDelayError} will only
     * invoke the {@code onError} method of its Subscribers once.
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The operator honors backpressure from downstream. The outer {@code Publisher} is consumed
     *  in unbounded mode (i.e., no backpressure is applied to it).</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code mergeDelayError} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T> the common element base type
     * @param sources
     *            a Publisher that emits MaybeSources
     * @return a Flowable that emits all of the items emitted by the Publishers emitted by the
     *         {@code source} Publisher
     * @see <a href="http://reactivex.io/documentation/operators/merge.html">ReactiveX operators documentation: Merge</a>
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @BackpressureSupport(BackpressureKind.FULL)
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <T> Flowable<T> mergeDelayError(Publisher<? extends MaybeSource<? extends T>> sources) {
        return Flowable.fromPublisher(sources).flatMap((Function)MaybeToPublisher.instance(), true);
    }

    /**
     * Flattens two MaybeSources into one Flowable, in a way that allows a Subscriber to receive all
     * successfully emitted items from each of the source MaybeSources without being interrupted by an error
     * notification from one of them.
     * <p>
     * This behaves like {@link #merge(MaybeSource, MaybeSource)} except that if any of the merged MaybeSources
     * notify of an error via {@link Subscriber#onError onError}, {@code mergeDelayError} will refrain from
     * propagating that error notification until all of the merged MaybeSources have finished emitting items.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/mergeDelayError.png" alt="">
     * <p>
     * Even if both merged MaybeSources send {@code onError} notifications, {@code mergeDelayError} will only
     * invoke the {@code onError} method of its Subscribers once.
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The operator honors backpressure from downstream.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code mergeDelayError} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T> the common element base type
     * @param source1
     *            a MaybeSource to be merged
     * @param source2
     *            a MaybeSource to be merged
     * @return a Flowable that emits all of the items that are emitted by the two source MaybeSources
     * @see <a href="http://reactivex.io/documentation/operators/merge.html">ReactiveX operators documentation: Merge</a>
     */
    @SuppressWarnings({ "unchecked" })
    @BackpressureSupport(BackpressureKind.FULL)
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <T> Flowable<T> mergeDelayError(MaybeSource<? extends T> source1, MaybeSource<? extends T> source2) {
        ObjectHelper.requireNonNull(source1, "source1 is null");
        ObjectHelper.requireNonNull(source2, "source2 is null");
        return mergeArrayDelayError(source1, source2);
    }

    /**
     * Flattens three MaybeSource into one Flowable, in a way that allows a Subscriber to receive all
     * successfully emitted items from all of the source MaybeSources without being interrupted by an error
     * notification from one of them.
     * <p>
     * This behaves like {@link #merge(MaybeSource, MaybeSource, MaybeSource)} except that if any of the merged
     * MaybeSources notify of an error via {@link Subscriber#onError onError}, {@code mergeDelayError} will refrain
     * from propagating that error notification until all of the merged MaybeSources have finished emitting
     * items.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/mergeDelayError.png" alt="">
     * <p>
     * Even if multiple merged MaybeSources send {@code onError} notifications, {@code mergeDelayError} will only
     * invoke the {@code onError} method of its Subscribers once.
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The operator honors backpressure from downstream.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code mergeDelayError} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T> the common element base type
     * @param source1
     *            a MaybeSource to be merged
     * @param source2
     *            a MaybeSource to be merged
     * @param source3
     *            a MaybeSource to be merged
     * @return a Flowable that emits all of the items that are emitted by the source MaybeSources
     * @see <a href="http://reactivex.io/documentation/operators/merge.html">ReactiveX operators documentation: Merge</a>
     */
    @SuppressWarnings({ "unchecked" })
    @BackpressureSupport(BackpressureKind.FULL)
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <T> Flowable<T> mergeDelayError(MaybeSource<? extends T> source1,
            MaybeSource<? extends T> source2, MaybeSource<? extends T> source3) {
        ObjectHelper.requireNonNull(source1, "source1 is null");
        ObjectHelper.requireNonNull(source2, "source2 is null");
        ObjectHelper.requireNonNull(source3, "source3 is null");
        return mergeArrayDelayError(source1, source2, source3);
    }


    /**
     * Flattens four MaybeSources into one Flowable, in a way that allows a Subscriber to receive all
     * successfully emitted items from all of the source MaybeSources without being interrupted by an error
     * notification from one of them.
     * <p>
     * This behaves like {@link #merge(MaybeSource, MaybeSource, MaybeSource, MaybeSource)} except that if any of
     * the merged MaybeSources notify of an error via {@link Subscriber#onError onError}, {@code mergeDelayError}
     * will refrain from propagating that error notification until all of the merged MaybeSources have finished
     * emitting items.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/mergeDelayError.png" alt="">
     * <p>
     * Even if multiple merged MaybeSources send {@code onError} notifications, {@code mergeDelayError} will only
     * invoke the {@code onError} method of its Subscribers once.
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The operator honors backpressure from downstream.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code mergeDelayError} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T> the common element base type
     * @param source1
     *            a MaybeSource to be merged
     * @param source2
     *            a MaybeSource to be merged
     * @param source3
     *            a MaybeSource to be merged
     * @param source4
     *            a MaybeSource to be merged
     * @return a Flowable that emits all of the items that are emitted by the source MaybeSources
     * @see <a href="http://reactivex.io/documentation/operators/merge.html">ReactiveX operators documentation: Merge</a>
     */
    @SuppressWarnings({ "unchecked" })
    @BackpressureSupport(BackpressureKind.FULL)
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <T> Flowable<T> mergeDelayError(
            MaybeSource<? extends T> source1, MaybeSource<? extends T> source2,
            MaybeSource<? extends T> source3, MaybeSource<? extends T> source4) {
        ObjectHelper.requireNonNull(source1, "source1 is null");
        ObjectHelper.requireNonNull(source2, "source2 is null");
        ObjectHelper.requireNonNull(source3, "source3 is null");
        ObjectHelper.requireNonNull(source4, "source4 is null");
        return mergeArrayDelayError(source1, source2, source3, source4);
    }

    /**
     * Returns a Maybe that never sends any items or notifications to a {@link MaybeObserver}.
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
     * @return a Maybe that never emits any items or sends any notifications to a {@link MaybeObserver}
     * @see <a href="http://reactivex.io/documentation/operators/empty-never-throw.html">ReactiveX operators documentation: Never</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @SuppressWarnings("unchecked")
    public static <T> Maybe<T> never() {
        return RxJavaPlugins.onAssembly((Maybe<T>)MaybeNever.INSTANCE);
    }


    /**
     * Returns a Single that emits a Boolean value that indicates whether two MaybeSource sequences are the
     * same by comparing the items emitted by each MaybeSource pairwise.
     * <p>
     * <img width="640" height="385" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/sequenceEqual.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code sequenceEqual} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param source1
     *            the first MaybeSource to compare
     * @param source2
     *            the second MaybeSource to compare
     * @param <T>
     *            the type of items emitted by each MaybeSource
     * @return a Single that emits a Boolean value that indicates whether the two sequences are the same
     * @see <a href="http://reactivex.io/documentation/operators/sequenceequal.html">ReactiveX operators documentation: SequenceEqual</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <T> Single<Boolean> sequenceEqual(MaybeSource<? extends T> source1, MaybeSource<? extends T> source2) {
        return sequenceEqual(source1, source2, ObjectHelper.equalsPredicate());
    }

    /**
     * Returns a Single that emits a Boolean value that indicates whether two MaybeSources are the
     * same by comparing the items emitted by each MaybeSource pairwise based on the results of a specified
     * equality function.
     * <p>
     * <img width="640" height="385" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/sequenceEqual.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code sequenceEqual} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param source1
     *            the first MaybeSource to compare
     * @param source2
     *            the second MaybeSource to compare
     * @param isEqual
     *            a function used to compare items emitted by each MaybeSource
     * @param <T>
     *            the type of items emitted by each MaybeSource
     * @return a Single that emits a Boolean value that indicates whether the two MaybeSource sequences
     *         are the same according to the specified function
     * @see <a href="http://reactivex.io/documentation/operators/sequenceequal.html">ReactiveX operators documentation: SequenceEqual</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <T> Single<Boolean> sequenceEqual(MaybeSource<? extends T> source1, MaybeSource<? extends T> source2,
            BiPredicate<? super T, ? super T> isEqual) {
        ObjectHelper.requireNonNull(source1, "source1 is null");
        ObjectHelper.requireNonNull(source2, "source2 is null");
        ObjectHelper.requireNonNull(isEqual, "isEqual is null");
        return RxJavaPlugins.onAssembly(new MaybeEqualSingle<T>(source1, source2, isEqual));
    }

    /**
     * Returns a Maybe that emits {@code 0L} after a specified delay.
     * <p>
     * <img width="640" height="200" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/timer.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code timer} operates by default on the {@code computation} {@link Scheduler}.</dd>
     * </dl>
     *
     * @param delay
     *            the initial delay before emitting a single {@code 0L}
     * @param unit
     *            time units to use for {@code delay}
     * @return a Maybe that emits {@code 0L} after a specified delay
     * @see <a href="http://reactivex.io/documentation/operators/timer.html">ReactiveX operators documentation: Timer</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.COMPUTATION)
    public static Maybe<Long> timer(long delay, TimeUnit unit) {
        return timer(delay, unit, Schedulers.computation());
    }

    /**
     * Returns a Maybe that emits {@code 0L} after a specified delay on a specified Scheduler.
     * <p>
     * <img width="640" height="200" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/timer.s.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>You specify which {@link Scheduler} this operator will use</dd>
     * </dl>
     *
     * @param delay
     *            the initial delay before emitting a single 0L
     * @param unit
     *            time units to use for {@code delay}
     * @param scheduler
     *            the {@link Scheduler} to use for scheduling the item
     * @return a Maybe that emits {@code 0L} after a specified delay, on a specified Scheduler
     * @see <a href="http://reactivex.io/documentation/operators/timer.html">ReactiveX operators documentation: Timer</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    public static Maybe<Long> timer(long delay, TimeUnit unit, Scheduler scheduler) {
        ObjectHelper.requireNonNull(unit, "unit is null");
        ObjectHelper.requireNonNull(scheduler, "scheduler is null");

        return RxJavaPlugins.onAssembly(new MaybeTimer(Math.max(0L, delay), unit, scheduler));
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
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <T> Maybe<T> unsafeCreate(MaybeSource<T> onSubscribe) {
        if (onSubscribe instanceof Maybe) {
            throw new IllegalArgumentException("unsafeCreate(Maybe) should be upgraded");
        }
        ObjectHelper.requireNonNull(onSubscribe, "onSubscribe is null");
        return RxJavaPlugins.onAssembly(new MaybeUnsafeCreate<T>(onSubscribe));
    }

    /**
     * Constructs a Maybe that creates a dependent resource object which is disposed of when the
     * upstream terminates or the downstream calls dispose().
     * <p>
     * <img width="640" height="400" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/using.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code using} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T> the element type of the generated MaybeSource
     * @param <D> the type of the resource associated with the output sequence
     * @param resourceSupplier
     *            the factory function to create a resource object that depends on the Maybe
     * @param sourceSupplier
     *            the factory function to create a MaybeSource
     * @param resourceDisposer
     *            the function that will dispose of the resource
     * @return the Maybe whose lifetime controls the lifetime of the dependent resource object
     * @see <a href="http://reactivex.io/documentation/operators/using.html">ReactiveX operators documentation: Using</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <T, D> Maybe<T> using(Callable<? extends D> resourceSupplier,
            Function<? super D, ? extends MaybeSource<? extends T>> sourceSupplier,
                    Consumer<? super D> resourceDisposer) {
        return using(resourceSupplier, sourceSupplier, resourceDisposer, true);
    }

    /**
     * Constructs a Maybe that creates a dependent resource object which is disposed of just before
     * termination if you have set {@code disposeEagerly} to {@code true} and a downstream dispose() does not occur
     * before termination. Otherwise resource disposal will occur on call to dispose().  Eager disposal is
     * particularly appropriate for a synchronous Maybe that reuses resources. {@code disposeAction} will
     * only be called once per subscription.
     * <p>
     * <img width="640" height="400" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/using.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code using} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T> the element type of the generated MaybeSource
     * @param <D> the type of the resource associated with the output sequence
     * @param resourceSupplier
     *            the factory function to create a resource object that depends on the Maybe
     * @param sourceSupplier
     *            the factory function to create a MaybeSource
     * @param resourceDisposer
     *            the function that will dispose of the resource
     * @param eager
     *            if {@code true} then disposal will happen either on a dispose() call or just before emission of
     *            a terminal event ({@code onComplete} or {@code onError}).
     * @return the Maybe whose lifetime controls the lifetime of the dependent resource object
     * @see <a href="http://reactivex.io/documentation/operators/using.html">ReactiveX operators documentation: Using</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <T, D> Maybe<T> using(Callable<? extends D> resourceSupplier,
            Function<? super D, ? extends MaybeSource<? extends T>> sourceSupplier,
                    Consumer<? super D> resourceDisposer, boolean eager) {
        ObjectHelper.requireNonNull(resourceSupplier, "resourceSupplier is null");
        ObjectHelper.requireNonNull(sourceSupplier, "sourceSupplier is null");
        ObjectHelper.requireNonNull(resourceDisposer, "disposer is null");
        return RxJavaPlugins.onAssembly(new MaybeUsing<T, D>(resourceSupplier, sourceSupplier, resourceDisposer, eager));
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
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <T> Maybe<T> wrap(MaybeSource<T> source) {
        if (source instanceof Maybe) {
            return RxJavaPlugins.onAssembly((Maybe<T>)source);
        }
        ObjectHelper.requireNonNull(source, "onSubscribe is null");
        return RxJavaPlugins.onAssembly(new MaybeUnsafeCreate<T>(source));
    }

    /**
     * Returns a Maybe that emits the results of a specified combiner function applied to combinations of
     * items emitted, in sequence, by an Iterable of other MaybeSources.
     * <p>
     * Note on method signature: since Java doesn't allow creating a generic array with {@code new T[]}, the
     * implementation of this operator has to create an {@code Object[]} instead. Unfortunately, a
     * {@code Function<Integer[], R>} passed to the method would trigger a {@code ClassCastException}.
     *
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/zip.png" alt="">
     * <p>This operator terminates eagerly if any of the source MaybeSources signal an onError or onComplete. This
     * also means it is possible some sources may not get subscribed to at all.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code zip} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T> the common value type
     * @param <R> the zipped result type
     * @param sources
     *            an Iterable of source MaybeSources
     * @param zipper
     *            a function that, when applied to an item emitted by each of the source MaybeSources, results in
     *            an item that will be emitted by the resulting Maybe
     * @return a Maybe that emits the zipped results
     * @see <a href="http://reactivex.io/documentation/operators/zip.html">ReactiveX operators documentation: Zip</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <T, R> Maybe<R> zip(Iterable<? extends MaybeSource<? extends T>> sources, Function<? super Object[], ? extends R> zipper) {
        ObjectHelper.requireNonNull(zipper, "zipper is null");
        ObjectHelper.requireNonNull(sources, "sources is null");
        return RxJavaPlugins.onAssembly(new MaybeZipIterable<T, R>(sources, zipper));
    }

    /**
     * Returns a Maybe that emits the results of a specified combiner function applied to combinations of
     * two items emitted, in sequence, by two other MaybeSources.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/zip.png" alt="">
     * <p>This operator terminates eagerly if any of the source MaybeSources signal an onError or onComplete. This
     * also means it is possible some sources may not get subscribed to at all.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code zip} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T1> the value type of the first source
     * @param <T2> the value type of the second source
     * @param <R> the zipped result type
     * @param source1
     *            the first source MaybeSource
     * @param source2
     *            a second source MaybeSource
     * @param zipper
     *            a function that, when applied to an item emitted by each of the source MaybeSources, results
     *            in an item that will be emitted by the resulting Maybe
     * @return a Maybe that emits the zipped results
     * @see <a href="http://reactivex.io/documentation/operators/zip.html">ReactiveX operators documentation: Zip</a>
     */
    @SuppressWarnings("unchecked")
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <T1, T2, R> Maybe<R> zip(
            MaybeSource<? extends T1> source1, MaybeSource<? extends T2> source2,
            BiFunction<? super T1, ? super T2, ? extends R> zipper) {
        ObjectHelper.requireNonNull(source1, "source1 is null");
        ObjectHelper.requireNonNull(source2, "source2 is null");
        return zipArray(Functions.toFunction(zipper), source1, source2);
    }

    /**
     * Returns a Maybe that emits the results of a specified combiner function applied to combinations of
     * three items emitted, in sequence, by three other MaybeSources.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/zip.png" alt="">
     * <p>This operator terminates eagerly if any of the source MaybeSources signal an onError or onComplete. This
     * also means it is possible some sources may not get subscribed to at all.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code zip} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T1> the value type of the first source
     * @param <T2> the value type of the second source
     * @param <T3> the value type of the third source
     * @param <R> the zipped result type
     * @param source1
     *            the first source MaybeSource
     * @param source2
     *            a second source MaybeSource
     * @param source3
     *            a third source MaybeSource
     * @param zipper
     *            a function that, when applied to an item emitted by each of the source MaybeSources, results in
     *            an item that will be emitted by the resulting Maybe
     * @return a Maybe that emits the zipped results
     * @see <a href="http://reactivex.io/documentation/operators/zip.html">ReactiveX operators documentation: Zip</a>
     */
    @SuppressWarnings("unchecked")
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <T1, T2, T3, R> Maybe<R> zip(
            MaybeSource<? extends T1> source1, MaybeSource<? extends T2> source2, MaybeSource<? extends T3> source3,
            Function3<? super T1, ? super T2, ? super T3, ? extends R> zipper) {
        ObjectHelper.requireNonNull(source1, "source1 is null");
        ObjectHelper.requireNonNull(source2, "source2 is null");
        ObjectHelper.requireNonNull(source3, "source3 is null");
        return zipArray(Functions.toFunction(zipper), source1, source2, source3);
    }

    /**
     * Returns a Maybe that emits the results of a specified combiner function applied to combinations of
     * four items emitted, in sequence, by four other MaybeSources.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/zip.png" alt="">
     * <p>This operator terminates eagerly if any of the source MaybeSources signal an onError or onComplete. This
     * also means it is possible some sources may not get subscribed to at all.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code zip} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T1> the value type of the first source
     * @param <T2> the value type of the second source
     * @param <T3> the value type of the third source
     * @param <T4> the value type of the fourth source
     * @param <R> the zipped result type
     * @param source1
     *            the first source MaybeSource
     * @param source2
     *            a second source MaybeSource
     * @param source3
     *            a third source MaybeSource
     * @param source4
     *            a fourth source MaybeSource
     * @param zipper
     *            a function that, when applied to an item emitted by each of the source MaybeSources, results in
     *            an item that will be emitted by the resulting Maybe
     * @return a Maybe that emits the zipped results
     * @see <a href="http://reactivex.io/documentation/operators/zip.html">ReactiveX operators documentation: Zip</a>
     */
    @SuppressWarnings("unchecked")
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <T1, T2, T3, T4, R> Maybe<R> zip(
            MaybeSource<? extends T1> source1, MaybeSource<? extends T2> source2, MaybeSource<? extends T3> source3,
            MaybeSource<? extends T4> source4,
            Function4<? super T1, ? super T2, ? super T3, ? super T4, ? extends R> zipper) {
        ObjectHelper.requireNonNull(source1, "source1 is null");
        ObjectHelper.requireNonNull(source2, "source2 is null");
        ObjectHelper.requireNonNull(source3, "source3 is null");
        ObjectHelper.requireNonNull(source4, "source4 is null");
        return zipArray(Functions.toFunction(zipper), source1, source2, source3, source4);
    }

    /**
     * Returns a Maybe that emits the results of a specified combiner function applied to combinations of
     * five items emitted, in sequence, by five other MaybeSources.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/zip.png" alt="">
     * <p>This operator terminates eagerly if any of the source MaybeSources signal an onError or onComplete. This
     * also means it is possible some sources may not get subscribed to at all.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code zip} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T1> the value type of the first source
     * @param <T2> the value type of the second source
     * @param <T3> the value type of the third source
     * @param <T4> the value type of the fourth source
     * @param <T5> the value type of the fifth source
     * @param <R> the zipped result type
     * @param source1
     *            the first source MaybeSource
     * @param source2
     *            a second source MaybeSource
     * @param source3
     *            a third source MaybeSource
     * @param source4
     *            a fourth source MaybeSource
     * @param source5
     *            a fifth source MaybeSource
     * @param zipper
     *            a function that, when applied to an item emitted by each of the source MaybeSources, results in
     *            an item that will be emitted by the resulting Maybe
     * @return a Maybe that emits the zipped results
     * @see <a href="http://reactivex.io/documentation/operators/zip.html">ReactiveX operators documentation: Zip</a>
     */
    @SuppressWarnings("unchecked")
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <T1, T2, T3, T4, T5, R> Maybe<R> zip(
            MaybeSource<? extends T1> source1, MaybeSource<? extends T2> source2, MaybeSource<? extends T3> source3,
            MaybeSource<? extends T4> source4, MaybeSource<? extends T5> source5,
            Function5<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? extends R> zipper) {
        ObjectHelper.requireNonNull(source1, "source1 is null");
        ObjectHelper.requireNonNull(source2, "source2 is null");
        ObjectHelper.requireNonNull(source3, "source3 is null");
        ObjectHelper.requireNonNull(source4, "source4 is null");
        ObjectHelper.requireNonNull(source5, "source5 is null");
        return zipArray(Functions.toFunction(zipper), source1, source2, source3, source4, source5);
    }

    /**
     * Returns a Maybe that emits the results of a specified combiner function applied to combinations of
     * six items emitted, in sequence, by six other MaybeSources.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/zip.png" alt="">
     * <p>This operator terminates eagerly if any of the source MaybeSources signal an onError or onComplete. This
     * also means it is possible some sources may not get subscribed to at all.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code zip} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T1> the value type of the first source
     * @param <T2> the value type of the second source
     * @param <T3> the value type of the third source
     * @param <T4> the value type of the fourth source
     * @param <T5> the value type of the fifth source
     * @param <T6> the value type of the sixth source
     * @param <R> the zipped result type
     * @param source1
     *            the first source MaybeSource
     * @param source2
     *            a second source MaybeSource
     * @param source3
     *            a third source MaybeSource
     * @param source4
     *            a fourth source MaybeSource
     * @param source5
     *            a fifth source MaybeSource
     * @param source6
     *            a sixth source MaybeSource
     * @param zipper
     *            a function that, when applied to an item emitted by each of the source MaybeSources, results in
     *            an item that will be emitted by the resulting Maybe
     * @return a Maybe that emits the zipped results
     * @see <a href="http://reactivex.io/documentation/operators/zip.html">ReactiveX operators documentation: Zip</a>
     */
    @SuppressWarnings("unchecked")
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <T1, T2, T3, T4, T5, T6, R> Maybe<R> zip(
            MaybeSource<? extends T1> source1, MaybeSource<? extends T2> source2, MaybeSource<? extends T3> source3,
            MaybeSource<? extends T4> source4, MaybeSource<? extends T5> source5, MaybeSource<? extends T6> source6,
            Function6<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? extends R> zipper) {
        ObjectHelper.requireNonNull(source1, "source1 is null");
        ObjectHelper.requireNonNull(source2, "source2 is null");
        ObjectHelper.requireNonNull(source3, "source3 is null");
        ObjectHelper.requireNonNull(source4, "source4 is null");
        ObjectHelper.requireNonNull(source5, "source5 is null");
        ObjectHelper.requireNonNull(source6, "source6 is null");
        return zipArray(Functions.toFunction(zipper), source1, source2, source3, source4, source5, source6);
    }

    /**
     * Returns a Maybe that emits the results of a specified combiner function applied to combinations of
     * seven items emitted, in sequence, by seven other MaybeSources.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/zip.png" alt="">
     * <p>This operator terminates eagerly if any of the source MaybeSources signal an onError or onComplete. This
     * also means it is possible some sources may not get subscribed to at all.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code zip} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T1> the value type of the first source
     * @param <T2> the value type of the second source
     * @param <T3> the value type of the third source
     * @param <T4> the value type of the fourth source
     * @param <T5> the value type of the fifth source
     * @param <T6> the value type of the sixth source
     * @param <T7> the value type of the seventh source
     * @param <R> the zipped result type
     * @param source1
     *            the first source MaybeSource
     * @param source2
     *            a second source MaybeSource
     * @param source3
     *            a third source MaybeSource
     * @param source4
     *            a fourth source MaybeSource
     * @param source5
     *            a fifth source MaybeSource
     * @param source6
     *            a sixth source MaybeSource
     * @param source7
     *            a seventh source MaybeSource
     * @param zipper
     *            a function that, when applied to an item emitted by each of the source MaybeSources, results in
     *            an item that will be emitted by the resulting Maybe
     * @return a Maybe that emits the zipped results
     * @see <a href="http://reactivex.io/documentation/operators/zip.html">ReactiveX operators documentation: Zip</a>
     */
    @SuppressWarnings("unchecked")
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <T1, T2, T3, T4, T5, T6, T7, R> Maybe<R> zip(
            MaybeSource<? extends T1> source1, MaybeSource<? extends T2> source2, MaybeSource<? extends T3> source3,
            MaybeSource<? extends T4> source4, MaybeSource<? extends T5> source5, MaybeSource<? extends T6> source6,
            MaybeSource<? extends T7> source7,
            Function7<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? extends R> zipper) {
        ObjectHelper.requireNonNull(source1, "source1 is null");
        ObjectHelper.requireNonNull(source2, "source2 is null");
        ObjectHelper.requireNonNull(source3, "source3 is null");
        ObjectHelper.requireNonNull(source4, "source4 is null");
        ObjectHelper.requireNonNull(source5, "source5 is null");
        ObjectHelper.requireNonNull(source6, "source6 is null");
        ObjectHelper.requireNonNull(source7, "source7 is null");
        return zipArray(Functions.toFunction(zipper), source1, source2, source3, source4, source5, source6, source7);
    }

    /**
     * Returns a Maybe that emits the results of a specified combiner function applied to combinations of
     * eight items emitted, in sequence, by eight other MaybeSources.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/zip.png" alt="">
     * <p>This operator terminates eagerly if any of the source MaybeSources signal an onError or onComplete. This
     * also means it is possible some sources may not get subscribed to at all.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code zip} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T1> the value type of the first source
     * @param <T2> the value type of the second source
     * @param <T3> the value type of the third source
     * @param <T4> the value type of the fourth source
     * @param <T5> the value type of the fifth source
     * @param <T6> the value type of the sixth source
     * @param <T7> the value type of the seventh source
     * @param <T8> the value type of the eighth source
     * @param <R> the zipped result type
     * @param source1
     *            the first source MaybeSource
     * @param source2
     *            a second source MaybeSource
     * @param source3
     *            a third source MaybeSource
     * @param source4
     *            a fourth source MaybeSource
     * @param source5
     *            a fifth source MaybeSource
     * @param source6
     *            a sixth source MaybeSource
     * @param source7
     *            a seventh source MaybeSource
     * @param source8
     *            an eighth source MaybeSource
     * @param zipper
     *            a function that, when applied to an item emitted by each of the source MaybeSources, results in
     *            an item that will be emitted by the resulting Maybe
     * @return a Maybe that emits the zipped results
     * @see <a href="http://reactivex.io/documentation/operators/zip.html">ReactiveX operators documentation: Zip</a>
     */
    @SuppressWarnings("unchecked")
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <T1, T2, T3, T4, T5, T6, T7, T8, R> Maybe<R> zip(
            MaybeSource<? extends T1> source1, MaybeSource<? extends T2> source2, MaybeSource<? extends T3> source3,
            MaybeSource<? extends T4> source4, MaybeSource<? extends T5> source5, MaybeSource<? extends T6> source6,
            MaybeSource<? extends T7> source7, MaybeSource<? extends T8> source8,
            Function8<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? super T8, ? extends R> zipper) {
        ObjectHelper.requireNonNull(source1, "source1 is null");
        ObjectHelper.requireNonNull(source2, "source2 is null");
        ObjectHelper.requireNonNull(source3, "source3 is null");
        ObjectHelper.requireNonNull(source4, "source4 is null");
        ObjectHelper.requireNonNull(source5, "source5 is null");
        ObjectHelper.requireNonNull(source6, "source6 is null");
        ObjectHelper.requireNonNull(source7, "source7 is null");
        ObjectHelper.requireNonNull(source8, "source8 is null");
        return zipArray(Functions.toFunction(zipper), source1, source2, source3, source4, source5, source6, source7, source8);
    }

    /**
     * Returns a Maybe that emits the results of a specified combiner function applied to combinations of
     * nine items emitted, in sequence, by nine other MaybeSources.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/zip.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code zip} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * <p>This operator terminates eagerly if any of the source MaybeSources signal an onError or onComplete. This
     * also means it is possible some sources may not get subscribed to at all.
     *
     * @param <T1> the value type of the first source
     * @param <T2> the value type of the second source
     * @param <T3> the value type of the third source
     * @param <T4> the value type of the fourth source
     * @param <T5> the value type of the fifth source
     * @param <T6> the value type of the sixth source
     * @param <T7> the value type of the seventh source
     * @param <T8> the value type of the eighth source
     * @param <T9> the value type of the ninth source
     * @param <R> the zipped result type
     * @param source1
     *            the first source MaybeSource
     * @param source2
     *            a second source MaybeSource
     * @param source3
     *            a third source MaybeSource
     * @param source4
     *            a fourth source MaybeSource
     * @param source5
     *            a fifth source MaybeSource
     * @param source6
     *            a sixth source MaybeSource
     * @param source7
     *            a seventh source MaybeSource
     * @param source8
     *            an eighth source MaybeSource
     * @param source9
     *            a ninth source MaybeSource
     * @param zipper
     *            a function that, when applied to an item emitted by each of the source MaybeSources, results in
     *            an item that will be emitted by the resulting MaybeSource
     * @return a Maybe that emits the zipped results
     * @see <a href="http://reactivex.io/documentation/operators/zip.html">ReactiveX operators documentation: Zip</a>
     */
    @SuppressWarnings("unchecked")
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <T1, T2, T3, T4, T5, T6, T7, T8, T9, R> Maybe<R> zip(
            MaybeSource<? extends T1> source1, MaybeSource<? extends T2> source2, MaybeSource<? extends T3> source3,
            MaybeSource<? extends T4> source4, MaybeSource<? extends T5> source5, MaybeSource<? extends T6> source6,
            MaybeSource<? extends T7> source7, MaybeSource<? extends T8> source8, MaybeSource<? extends T9> source9,
            Function9<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? super T8, ? super T9, ? extends R> zipper) {

        ObjectHelper.requireNonNull(source1, "source1 is null");
        ObjectHelper.requireNonNull(source2, "source2 is null");
        ObjectHelper.requireNonNull(source3, "source3 is null");
        ObjectHelper.requireNonNull(source4, "source4 is null");
        ObjectHelper.requireNonNull(source5, "source5 is null");
        ObjectHelper.requireNonNull(source6, "source6 is null");
        ObjectHelper.requireNonNull(source7, "source7 is null");
        ObjectHelper.requireNonNull(source8, "source8 is null");
        ObjectHelper.requireNonNull(source9, "source9 is null");
        return zipArray(Functions.toFunction(zipper), source1, source2, source3, source4, source5, source6, source7, source8, source9);
    }

    /**
     * Returns a Maybe that emits the results of a specified combiner function applied to combinations of
     * items emitted, in sequence, by an array of other MaybeSources.
     * <p>
     * Note on method signature: since Java doesn't allow creating a generic array with {@code new T[]}, the
     * implementation of this operator has to create an {@code Object[]} instead. Unfortunately, a
     * {@code Function<Integer[], R>} passed to the method would trigger a {@code ClassCastException}.
     *
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/zip.png" alt="">
     * <p>This operator terminates eagerly if any of the source MaybeSources signal an onError or onComplete. This
     * also means it is possible some sources may not get subscribed to at all.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code zipArray} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T> the common element type
     * @param <R> the result type
     * @param sources
     *            an array of source MaybeSources
     * @param zipper
     *            a function that, when applied to an item emitted by each of the source MaybeSources, results in
     *            an item that will be emitted by the resulting MaybeSource
     * @return a Maybe that emits the zipped results
     * @see <a href="http://reactivex.io/documentation/operators/zip.html">ReactiveX operators documentation: Zip</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <T, R> Maybe<R> zipArray(Function<? super Object[], ? extends R> zipper,
            MaybeSource<? extends T>... sources) {
        ObjectHelper.requireNonNull(sources, "sources is null");
        if (sources.length == 0) {
            return empty();
        }
        ObjectHelper.requireNonNull(zipper, "zipper is null");
        return RxJavaPlugins.onAssembly(new MaybeZipArray<T, R>(sources, zipper));
    }

    // ------------------------------------------------------------------
    // Instance methods
    // ------------------------------------------------------------------

    /**
     * Mirrors the MaybeSource (current or provided) that first signals an event.
     * <p>
     * <img width="640" height="385" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/amb.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code ambWith} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param other
     *            a MaybeSource competing to react first. A subscription to this provided source will occur after
     *            subscribing to the current source.
     * @return a Maybe that emits the same sequence as whichever of the source MaybeSources first
     *         signalled
     * @see <a href="http://reactivex.io/documentation/operators/amb.html">ReactiveX operators documentation: Amb</a>
     */
    @SuppressWarnings("unchecked")
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Maybe<T> ambWith(MaybeSource<? extends T> other) {
        ObjectHelper.requireNonNull(other, "other is null");
        return ambArray(this, other);
    }

    /**
     * Waits in a blocking fashion until the current Maybe signals a success value (which is returned),
     * null if completed or an exception (which is propagated).
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code blockingGet} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @return the success value
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public final T blockingGet() {
        BlockingMultiObserver<T> observer = new BlockingMultiObserver<T>();
        subscribe(observer);
        return observer.blockingGet();
    }

    /**
     * Waits in a blocking fashion until the current Maybe signals a success value (which is returned),
     * defaultValue if completed or an exception (which is propagated).
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code blockingGet} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param defaultValue the default item to return if this Maybe is empty
     * @return the success value
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public final T blockingGet(T defaultValue) {
        ObjectHelper.requireNonNull(defaultValue, "defaultValue is null");
        BlockingMultiObserver<T> observer = new BlockingMultiObserver<T>();
        subscribe(observer);
        return observer.blockingGet(defaultValue);
    }

    /**
     * Returns a Maybe that subscribes to this Maybe lazily, caches its event
     * and replays it, to all the downstream subscribers.
     * <p>
     * <img width="640" height="410" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/cache.png" alt="">
     * <p>
     * The operator subscribes only when the first downstream subscriber subscribes and maintains
     * a single subscription towards this Maybe.
     * <p>
     * <em>Note:</em> You sacrifice the ability to dispose the origin when you use the {@code cache}.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code cache} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @return a Maybe that, when first subscribed to, caches all of its items and notifications for the
     *         benefit of subsequent subscribers
     * @see <a href="http://reactivex.io/documentation/operators/replay.html">ReactiveX operators documentation: Replay</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Maybe<T> cache() {
        return RxJavaPlugins.onAssembly(new MaybeCache<T>(this));
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
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
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
     * @param transformer the transformer function, not null
     * @return a Maybe, transformed by the transformer function
     * @see <a href="https://github.com/ReactiveX/RxJava/wiki/Implementing-Your-Own-Operators">RxJava wiki: Implementing Your Own Operators</a>
     */
    @SuppressWarnings("unchecked")
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public final <R> Maybe<R> compose(MaybeTransformer<? super T, ? extends R> transformer) {
        return wrap(((MaybeTransformer<T, R>) ObjectHelper.requireNonNull(transformer, "transformer is null")).apply(this));
    }

    /**
     * Returns a Maybe that is based on applying a specified function to the item emitted by the source Maybe,
     * where that function returns a MaybeSource.
     * <p>
     * <img width="640" height="356" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.flatMap.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code concatMap} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * <p>Note that flatMap and concatMap for Maybe is the same operation.
     * @param <R> the result value type
     * @param mapper
     *            a function that, when applied to the item emitted by the source Maybe, returns a MaybeSource
     * @return the Maybe returned from {@code func} when applied to the item emitted by the source Maybe
     * @see <a href="http://reactivex.io/documentation/operators/flatmap.html">ReactiveX operators documentation: FlatMap</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public final <R> Maybe<R> concatMap(Function<? super T, ? extends MaybeSource<? extends R>> mapper) {
        ObjectHelper.requireNonNull(mapper, "mapper is null");
        return RxJavaPlugins.onAssembly(new MaybeFlatten<T, R>(this, mapper));
    }


    /**
     * Returns a Flowable that emits the items emitted from the current MaybeSource, then the next, one after
     * the other, without interleaving them.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/concat.png" alt="">
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The operator honors backpressure from downstream.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code concatWith} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param other
     *            a MaybeSource to be concatenated after the current
     * @return a Flowable that emits items emitted by the two source MaybeSources, one after the other,
     *         without interleaving them
     * @see <a href="http://reactivex.io/documentation/operators/concat.html">ReactiveX operators documentation: Concat</a>
     */
    @BackpressureSupport(BackpressureKind.FULL)
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Flowable<T> concatWith(MaybeSource<? extends T> other) {
        ObjectHelper.requireNonNull(other, "other is null");
        return concat(this, other);
    }

    /**
     * Returns a Single that emits a Boolean that indicates whether the source Maybe emitted a
     * specified item.
     * <p>
     * <img width="640" height="320" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/contains.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code contains} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param item
     *            the item to search for in the emissions from the source Maybe, not null
     * @return a Single that emits {@code true} if the specified item is emitted by the source Maybe,
     *         or {@code false} if the source Maybe completes without emitting that item
     * @see <a href="http://reactivex.io/documentation/operators/contains.html">ReactiveX operators documentation: Contains</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Single<Boolean> contains(final Object item) {
        ObjectHelper.requireNonNull(item, "item is null");
        return RxJavaPlugins.onAssembly(new MaybeContains<T>(this, item));
    }

    /**
     * Returns a Maybe that counts the total number of items emitted (0 or 1) by the source Maybe and emits
     * this count as a 64-bit Long.
     * <p>
     * <img width="640" height="310" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/longCount.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code count} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @return a Single that emits a single item: the number of items emitted by the source Maybe as a
     *         64-bit Long item
     * @see <a href="http://reactivex.io/documentation/operators/count.html">ReactiveX operators documentation: Count</a>
     * @see #count()
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Single<Long> count() {
        return RxJavaPlugins.onAssembly(new MaybeCount<T>(this));
    }

    /**
     * Returns a Maybe that emits the item emitted by the source Maybe or a specified default item
     * if the source Maybe is empty.
     * <p>
     * Note that the result Maybe is semantically equivalent to a {@code Single}, since it's guaranteed
     * to emit exactly one item or an error. See {@link #toSingle(Object)} for a method with equivalent
     * behavior which returns a {@code Single}.
     * <p>
     * <img width="640" height="305" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/defaultIfEmpty.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code defaultIfEmpty} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param defaultItem
     *            the item to emit if the source Maybe emits no items
     * @return a Maybe that emits either the specified default item if the source Maybe emits no
     *         items, or the items emitted by the source Maybe
     * @see <a href="http://reactivex.io/documentation/operators/defaultifempty.html">ReactiveX operators documentation: DefaultIfEmpty</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Maybe<T> defaultIfEmpty(T defaultItem) {
        ObjectHelper.requireNonNull(defaultItem, "item is null");
        return switchIfEmpty(just(defaultItem));
    }


    /**
     * Returns a Maybe that signals the events emitted by the source Maybe shifted forward in time by a
     * specified delay.
     * <p>
     * <img width="640" height="310" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/delay.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This version of {@code delay} operates by default on the {@code computation} {@link Scheduler}.</dd>
     * </dl>
     *
     * @param delay
     *            the delay to shift the source by
     * @param unit
     *            the {@link TimeUnit} in which {@code period} is defined
     * @return the new Maybe instance
     * @see <a href="http://reactivex.io/documentation/operators/delay.html">ReactiveX operators documentation: Delay</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.COMPUTATION)
    public final Maybe<T> delay(long delay, TimeUnit unit) {
        return delay(delay, unit, Schedulers.computation());
    }

    /**
     * Returns a Maybe that signals the events emitted by the source Maybe shifted forward in time by a
     * specified delay running on the specified Scheduler.
     * <p>
     * <img width="640" height="310" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/delay.s.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>you specify which {@link Scheduler} this operator will use</dd>
     * </dl>
     *
     * @param delay
     *            the delay to shift the source by
     * @param unit
     *            the time unit of {@code delay}
     * @param scheduler
     *            the {@link Scheduler} to use for delaying
     * @return the new Maybe instance
     * @see <a href="http://reactivex.io/documentation/operators/delay.html">ReactiveX operators documentation: Delay</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    public final Maybe<T> delay(long delay, TimeUnit unit, Scheduler scheduler) {
        ObjectHelper.requireNonNull(unit, "unit is null");
        ObjectHelper.requireNonNull(scheduler, "scheduler is null");
        return RxJavaPlugins.onAssembly(new MaybeDelay<T>(this, Math.max(0L, delay), unit, scheduler));
    }

    /**
     * Delays the emission of this Maybe until the given Publisher signals an item or completes.
     * <p>
     * <img width="640" height="450" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/delay.oo.png" alt="">
     * <p>
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The {@code delayIndicator} is consumed in an unbounded manner but is cancelled after
     *  the first item it produces.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This version of {@code delay} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <U>
     *            the subscription delay value type (ignored)
     * @param <V>
     *            the item delay value type (ignored)
     * @param delayIndicator
     *            the Publisher that gets subscribed to when this Maybe signals an event and that
     *            signal is emitted when the Publisher signals an item or completes
     * @return the new Maybe instance
     * @see <a href="http://reactivex.io/documentation/operators/delay.html">ReactiveX operators documentation: Delay</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @BackpressureSupport(BackpressureKind.UNBOUNDED_IN)
    public final <U, V> Maybe<T> delay(Publisher<U> delayIndicator) {
        ObjectHelper.requireNonNull(delayIndicator, "delayIndicator is null");
        return RxJavaPlugins.onAssembly(new MaybeDelayOtherPublisher<T, U>(this, delayIndicator));
    }

    /**
     * Returns a Maybe that delays the subscription to this Maybe
     * until the other Publisher emits an element or completes normally.
     * <p>
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The {@code Publisher} source is consumed in an unbounded fashion (without applying backpressure).</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This method does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <U> the value type of the other Publisher, irrelevant
     * @param subscriptionIndicator the other Publisher that should trigger the subscription
     *        to this Publisher.
     * @return a Maybe that delays the subscription to this Maybe
     *         until the other Publisher emits an element or completes normally.
     */
    @BackpressureSupport(BackpressureKind.UNBOUNDED_IN)
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public final <U> Maybe<T> delaySubscription(Publisher<U> subscriptionIndicator) {
        ObjectHelper.requireNonNull(subscriptionIndicator, "subscriptionIndicator is null");
        return RxJavaPlugins.onAssembly(new MaybeDelaySubscriptionOtherPublisher<T, U>(this, subscriptionIndicator));
    }

    /**
     * Returns a Maybe that delays the subscription to the source Maybe by a given amount of time.
     * <p>
     * <img width="640" height="310" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/delaySubscription.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This version of {@code delaySubscription} operates by default on the {@code computation} {@link Scheduler}.</dd>
     * </dl>
     *
     * @param delay
     *            the time to delay the subscription
     * @param unit
     *            the time unit of {@code delay}
     * @return a Maybe that delays the subscription to the source Maybe by the given amount
     * @see <a href="http://reactivex.io/documentation/operators/delay.html">ReactiveX operators documentation: Delay</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.COMPUTATION)
    public final Maybe<T> delaySubscription(long delay, TimeUnit unit) {
        return delaySubscription(delay, unit, Schedulers.computation());
    }

    /**
     * Returns a Maybe that delays the subscription to the source Maybe by a given amount of time,
     * both waiting and subscribing on a given Scheduler.
     * <p>
     * <img width="640" height="310" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/delaySubscription.s.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>You specify which {@link Scheduler} this operator will use</dd>
     * </dl>
     *
     * @param delay
     *            the time to delay the subscription
     * @param unit
     *            the time unit of {@code delay}
     * @param scheduler
     *            the Scheduler on which the waiting and subscription will happen
     * @return a Maybe that delays the subscription to the source Maybe by a given
     *         amount, waiting and subscribing on the given Scheduler
     * @see <a href="http://reactivex.io/documentation/operators/delay.html">ReactiveX operators documentation: Delay</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    public final Maybe<T> delaySubscription(long delay, TimeUnit unit, Scheduler scheduler) {
        return delaySubscription(Flowable.timer(delay, unit, scheduler));
    }

    /**
     * Calls the specified consumer with the success item after this item has been emitted to the downstream.
     * <p>Note that the {@code onAfterNext} action is shared between subscriptions and as such
     * should be thread-safe.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code doAfterSuccess} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * <p>History: 2.0.1 - experimental
     * @param onAfterSuccess the Consumer that will be called after emitting an item from upstream to the downstream
     * @return the new Maybe instance
     * @since 2.1
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Maybe<T> doAfterSuccess(Consumer<? super T> onAfterSuccess) {
        ObjectHelper.requireNonNull(onAfterSuccess, "doAfterSuccess is null");
        return RxJavaPlugins.onAssembly(new MaybeDoAfterSuccess<T>(this, onAfterSuccess));
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
    @CheckReturnValue
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
     * Calls the specified action after this Maybe signals onSuccess, onError or onComplete or gets disposed by
     * the downstream.
     * <p>In case of a race between a terminal event and a dispose call, the provided {@code onFinally} action
     * is executed once per subscription.
     * <p>Note that the {@code onFinally} action is shared between subscriptions and as such
     * should be thread-safe.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code doFinally} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * <p>History: 2.0.1 - experimental
     * @param onFinally the action called when this Maybe terminates or gets cancelled
     * @return the new Maybe instance
     * @since 2.1
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Maybe<T> doFinally(Action onFinally) {
        ObjectHelper.requireNonNull(onFinally, "onFinally is null");
        return RxJavaPlugins.onAssembly(new MaybeDoFinally<T>(this, onFinally));
    }

    /**
     * Calls the shared {@code Action} if a MaybeObserver subscribed to the current Maybe
     * disposes the common Disposable it received via onSubscribe.
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code doOnDispose} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param onDispose the action called when the subscription is cancelled (disposed)
     * @throws NullPointerException if onDispose is null
     * @return the new Maybe instance
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
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
     * Modifies the source Maybe so that it invokes an action when it calls {@code onComplete}.
     * <p>
     * <img width="640" height="305" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/doOnComplete.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code doOnComplete} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param onComplete
     *            the action to invoke when the source Maybe calls {@code onComplete}
     * @return the new Maybe with the side-effecting behavior applied
     * @see <a href="http://reactivex.io/documentation/operators/do.html">ReactiveX operators documentation: Do</a>
     */
    @CheckReturnValue
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
     * MaybeObserver that subscribes to the current Maybe.
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code doOnError} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param onError the consumer called with the success value of onError
     * @return the new Maybe instance
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
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
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Maybe<T> doOnEvent(BiConsumer<? super T, ? super Throwable> onEvent) {
        ObjectHelper.requireNonNull(onEvent, "onEvent is null");
        return RxJavaPlugins.onAssembly(new MaybeDoOnEvent<T>(this, onEvent));
    }

    /**
     * Calls the shared consumer with the Disposable sent through the onSubscribe for each
     * MaybeObserver that subscribes to the current Maybe.
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code doOnSubscribe} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param onSubscribe the consumer called with the Disposable sent via onSubscribe
     * @return the new Maybe instance
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
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
     * MaybeObserver that subscribes to the current Maybe.
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code doOnSuccess} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param onSuccess the consumer called with the success value of onSuccess
     * @return the new Maybe instance
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
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
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Maybe<T> filter(Predicate<? super T> predicate) {
        ObjectHelper.requireNonNull(predicate, "predicate is null");
        return RxJavaPlugins.onAssembly(new MaybeFilter<T>(this, predicate));
    }

    /**
     * Returns a Maybe that is based on applying a specified function to the item emitted by the source Maybe,
     * where that function returns a MaybeSource.
     * <p>
     * <img width="640" height="356" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.flatMap.png" alt="">
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
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
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
     *            a function that returns a MaybeSource to merge for the onSuccess item emitted by this Maybe
     * @param onErrorMapper
     *            a function that returns a MaybeSource to merge for an onError notification from this Maybe
     * @param onCompleteSupplier
     *            a function that returns a MaybeSource to merge for an onComplete notification this Maybe
     * @return the new Maybe instance
     * @see <a href="http://reactivex.io/documentation/operators/flatmap.html">ReactiveX operators documentation: FlatMap</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public final <R> Maybe<R> flatMap(
            Function<? super T, ? extends MaybeSource<? extends R>> onSuccessMapper,
            Function<? super Throwable, ? extends MaybeSource<? extends R>> onErrorMapper,
            Callable<? extends MaybeSource<? extends R>> onCompleteSupplier) {
        ObjectHelper.requireNonNull(onSuccessMapper, "onSuccessMapper is null");
        ObjectHelper.requireNonNull(onErrorMapper, "onErrorMapper is null");
        ObjectHelper.requireNonNull(onCompleteSupplier, "onCompleteSupplier is null");
        return RxJavaPlugins.onAssembly(new MaybeFlatMapNotification<T, R>(this, onSuccessMapper, onErrorMapper, onCompleteSupplier));
    }

    /**
     * Returns a Maybe that emits the results of a specified function to the pair of values emitted by the
     * source Maybe and a specified mapped MaybeSource.
     * <p>
     * <img width="640" height="390" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/mergeMap.r.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code flatMap} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <U>
     *            the type of items emitted by the MaybeSource returned by the {@code mapper} function
     * @param <R>
     *            the type of items emitted by the resulting Maybe
     * @param mapper
     *            a function that returns a MaybeSource for the item emitted by the source Maybe
     * @param resultSelector
     *            a function that combines one item emitted by each of the source and collection MaybeSource and
     *            returns an item to be emitted by the resulting MaybeSource
     * @return the new Maybe instance
     * @see <a href="http://reactivex.io/documentation/operators/flatmap.html">ReactiveX operators documentation: FlatMap</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public final <U, R> Maybe<R> flatMap(Function<? super T, ? extends MaybeSource<? extends U>> mapper,
            BiFunction<? super T, ? super U, ? extends R> resultSelector) {
        ObjectHelper.requireNonNull(mapper, "mapper is null");
        ObjectHelper.requireNonNull(resultSelector, "resultSelector is null");
        return RxJavaPlugins.onAssembly(new MaybeFlatMapBiSelector<T, U, R>(this, mapper, resultSelector));
    }

    /**
     * Returns a Flowable that merges each item emitted by the source Maybe with the values in an
     * Iterable corresponding to that item that is generated by a selector.
     * <p>
     * <img width="640" height="373" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/flattenAsFlowable.png" alt="">
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The operator honors backpressure from downstream.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code flattenAsFlowable} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <U>
     *            the type of item emitted by the resulting Iterable
     * @param mapper
     *            a function that returns an Iterable sequence of values for when given an item emitted by the
     *            source Maybe
     * @return the new Flowable instance
     * @see <a href="http://reactivex.io/documentation/operators/flatmap.html">ReactiveX operators documentation: FlatMap</a>
     */
    @BackpressureSupport(BackpressureKind.FULL)
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public final <U> Flowable<U> flattenAsFlowable(final Function<? super T, ? extends Iterable<? extends U>> mapper) {
        ObjectHelper.requireNonNull(mapper, "mapper is null");
        return RxJavaPlugins.onAssembly(new MaybeFlatMapIterableFlowable<T, U>(this, mapper));
    }

    /**
     * Returns an Observable that maps a success value into an Iterable and emits its items.
     * <p>
     * <img width="640" height="373" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/flattenAsObservable.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code flattenAsObservable} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <U>
     *            the type of item emitted by the resulting Iterable
     * @param mapper
     *            a function that returns an Iterable sequence of values for when given an item emitted by the
     *            source Maybe
     * @return the new Observable instance
     * @see <a href="http://reactivex.io/documentation/operators/flatmap.html">ReactiveX operators documentation: FlatMap</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public final <U> Observable<U> flattenAsObservable(final Function<? super T, ? extends Iterable<? extends U>> mapper) {
        ObjectHelper.requireNonNull(mapper, "mapper is null");
        return RxJavaPlugins.onAssembly(new MaybeFlatMapIterableObservable<T, U>(this, mapper));
    }

    /**
     * Returns an Observable that is based on applying a specified function to the item emitted by the source Maybe,
     * where that function returns an ObservableSource.
     * <p>
     * <img width="640" height="356" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.flatMap.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code flatMapObservable} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <R> the result value type
     * @param mapper
     *            a function that, when applied to the item emitted by the source Maybe, returns an ObservableSource
     * @return the Observable returned from {@code func} when applied to the item emitted by the source Maybe
     * @see <a href="http://reactivex.io/documentation/operators/flatmap.html">ReactiveX operators documentation: FlatMap</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public final <R> Observable<R> flatMapObservable(Function<? super T, ? extends ObservableSource<? extends R>> mapper) {
        return toObservable().flatMap(mapper);
    }

    /**
     * Returns a Flowable that emits items based on applying a specified function to the item emitted by the
     * source Maybe, where that function returns a Publisher.
     * <p>
     * <img width="640" height="260" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.flatMapPublisher.png" alt="">
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The returned Flowable honors the downstream backpressure.</dd>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code flatMapPublisher} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <R> the result value type
     * @param mapper
     *            a function that, when applied to the item emitted by the source Maybe, returns an
     *            Flowable
     * @return the Flowable returned from {@code func} when applied to the item emitted by the source Maybe
     * @see <a href="http://reactivex.io/documentation/operators/flatmap.html">ReactiveX operators documentation: FlatMap</a>
     */
    @BackpressureSupport(BackpressureKind.FULL)
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public final <R> Flowable<R> flatMapPublisher(Function<? super T, ? extends Publisher<? extends R>> mapper) {
        return toFlowable().flatMap(mapper);
    }

    /**
     * Returns a {@link Single} based on applying a specified function to the item emitted by the
     * source {@link Maybe}, where that function returns a {@link Single}.
     * When this Maybe completes a {@link NoSuchElementException} will be thrown.
     * <p>
     * <img width="640" height="356" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.flatMapSingle.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code flatMapSingle} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <R> the result value type
     * @param mapper
     *            a function that, when applied to the item emitted by the source Maybe, returns a
     *            Single
     * @return the Single returned from {@code mapper} when applied to the item emitted by the source Maybe
     * @see <a href="http://reactivex.io/documentation/operators/flatmap.html">ReactiveX operators documentation: FlatMap</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public final <R> Single<R> flatMapSingle(final Function<? super T, ? extends SingleSource<? extends R>> mapper) {
        ObjectHelper.requireNonNull(mapper, "mapper is null");
        return RxJavaPlugins.onAssembly(new MaybeFlatMapSingle<T, R>(this, mapper));
    }

    /**
     * Returns a {@link Maybe} based on applying a specified function to the item emitted by the
     * source {@link Maybe}, where that function returns a {@link Single}.
     * When this Maybe just completes the resulting {@code Maybe} completes as well.
     * <p>
     * <img width="640" height="356" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.flatMapSingle.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code flatMapSingleElement} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * <p>History: 2.0.2 - experimental
     * @param <R> the result value type
     * @param mapper
     *            a function that, when applied to the item emitted by the source Maybe, returns a
     *            Single
     * @return the new Maybe instance
     * @see <a href="http://reactivex.io/documentation/operators/flatmap.html">ReactiveX operators documentation: FlatMap</a>
     * @since 2.1
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public final <R> Maybe<R> flatMapSingleElement(final Function<? super T, ? extends SingleSource<? extends R>> mapper) {
        ObjectHelper.requireNonNull(mapper, "mapper is null");
        return RxJavaPlugins.onAssembly(new MaybeFlatMapSingleElement<T, R>(this, mapper));
    }

    /**
     * Returns a {@link Completable} that completes based on applying a specified function to the item emitted by the
     * source {@link Maybe}, where that function returns a {@link Completable}.
     * <p>
     * <img width="640" height="267" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.flatMapCompletable.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code flatMapCompletable} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param mapper
     *            a function that, when applied to the item emitted by the source Maybe, returns a
     *            Completable
     * @return the Completable returned from {@code mapper} when applied to the item emitted by the source Maybe
     * @see <a href="http://reactivex.io/documentation/operators/flatmap.html">ReactiveX operators documentation: FlatMap</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Completable flatMapCompletable(final Function<? super T, ? extends CompletableSource> mapper) {
        ObjectHelper.requireNonNull(mapper, "mapper is null");
        return RxJavaPlugins.onAssembly(new MaybeFlatMapCompletable<T>(this, mapper));
    }

    /**
     * Hides the identity of this Maybe and its Disposable.
     * <p>Allows preventing certain identity-based
     * optimizations (fusion).
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code hide} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @return the new Maybe instance
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Maybe<T> hide() {
        return RxJavaPlugins.onAssembly(new MaybeHide<T>(this));
    }

    /**
     * Ignores the item emitted by the source Maybe and only calls {@code onComplete} or {@code onError}.
     * <p>
     * <img width="640" height="305" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/ignoreElements.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code ignoreElement} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @return an empty Completable that only calls {@code onComplete} or {@code onError}, based on which one is
     *         called by the source Maybe
     * @see <a href="http://reactivex.io/documentation/operators/ignoreelements.html">ReactiveX operators documentation: IgnoreElements</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Completable ignoreElement() {
        return RxJavaPlugins.onAssembly(new MaybeIgnoreElementCompletable<T>(this));
    }

    /**
     * Returns a Single that emits {@code true} if the source Maybe is empty, otherwise {@code false}.
     * <p>
     * <img width="640" height="320" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/isEmpty.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code isEmpty} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @return a Single that emits a Boolean
     * @see <a href="http://reactivex.io/documentation/operators/contains.html">ReactiveX operators documentation: Contains</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Single<Boolean> isEmpty() {
        return RxJavaPlugins.onAssembly(new MaybeIsEmptySingle<T>(this));
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
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public final <R> Maybe<R> lift(final MaybeOperator<? extends R, ? super T> lift) {
        ObjectHelper.requireNonNull(lift, "onLift is null");
        return RxJavaPlugins.onAssembly(new MaybeLift<T, R>(this, lift));
    }

    /**
     * Returns a Maybe that applies a specified function to the item emitted by the source Maybe and
     * emits the result of this function application.
     * <p>
     * <img width="640" height="515" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.map.png" alt="">
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
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public final <R> Maybe<R> map(Function<? super T, ? extends R> mapper) {
        ObjectHelper.requireNonNull(mapper, "mapper is null");
        return RxJavaPlugins.onAssembly(new MaybeMap<T, R>(this, mapper));
    }

    /**
     * Flattens this and another Maybe into a single Flowable, without any transformation.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/merge.png" alt="">
     * <p>
     * You can combine items emitted by multiple Maybes so that they appear as a single Flowable, by
     * using the {@code mergeWith} method.
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The operator honors backpressure from downstream.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code mergeWith} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param other
     *            a MaybeSource to be merged
     * @return a new Flowable instance
     * @see <a href="http://reactivex.io/documentation/operators/merge.html">ReactiveX operators documentation: Merge</a>
     */
    @BackpressureSupport(BackpressureKind.FULL)
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Flowable<T> mergeWith(MaybeSource<? extends T> other) {
        ObjectHelper.requireNonNull(other, "other is null");
        return merge(this, other);
    }

    /**
     * Wraps a Maybe to emit its item (or notify of its error) on a specified {@link Scheduler},
     * asynchronously.
     * <p>
     * <img width="640" height="182" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.observeOn.png" alt="">
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
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    public final Maybe<T> observeOn(final Scheduler scheduler) {
        ObjectHelper.requireNonNull(scheduler, "scheduler is null");
        return RxJavaPlugins.onAssembly(new MaybeObserveOn<T>(this, scheduler));
    }

    /**
     * Filters the items emitted by a Maybe, only emitting its success value if that
     * is an instance of the supplied Class.
     * <p>
     * <img width="640" height="310" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/ofClass.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code ofType} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <U> the output type
     * @param clazz
     *            the class type to filter the items emitted by the source Maybe
     * @return the new Maybe instance
     * @see <a href="http://reactivex.io/documentation/operators/filter.html">ReactiveX operators documentation: Filter</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public final <U> Maybe<U> ofType(final Class<U> clazz) {
        ObjectHelper.requireNonNull(clazz, "clazz is null");
        return filter(Functions.isInstanceOf(clazz)).cast(clazz);
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
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public final <R> R to(Function<? super Maybe<T>, R> convert) {
        try {
            return ObjectHelper.requireNonNull(convert, "convert is null").apply(this);
        } catch (Throwable ex) {
            Exceptions.throwIfFatal(ex);
            throw ExceptionHelper.wrapOrThrow(ex);
        }
    }

    /**
     * Converts this Maybe into a backpressure-aware Flowable instance composing cancellation
     * through.
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The returned Flowable honors the backpressure of the downstream.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code toFlowable} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @return the new Flowable instance
     */
    @SuppressWarnings("unchecked")
    @BackpressureSupport(BackpressureKind.FULL)
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Flowable<T> toFlowable() {
        if (this instanceof FuseToFlowable) {
            return ((FuseToFlowable<T>)this).fuseToFlowable();
        }
        return RxJavaPlugins.onAssembly(new MaybeToFlowable<T>(this));
    }

    /**
     * Converts this Maybe into an Observable instance composing cancellation
     * through.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code toObservable} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @return the new Observable instance
     */
    @SuppressWarnings("unchecked")
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Observable<T> toObservable() {
        if (this instanceof FuseToObservable) {
            return ((FuseToObservable<T>)this).fuseToObservable();
        }
        return RxJavaPlugins.onAssembly(new MaybeToObservable<T>(this));
    }

    /**
     * Converts this Maybe into a Single instance composing cancellation
     * through and turning an empty Maybe into a Single that emits the given
     * value through onSuccess.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code toSingle} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param defaultValue the default item to signal in Single if this Maybe is empty
     * @return the new Single instance
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Single<T> toSingle(T defaultValue) {
        ObjectHelper.requireNonNull(defaultValue, "defaultValue is null");
        return RxJavaPlugins.onAssembly(new MaybeToSingle<T>(this, defaultValue));
    }

    /**
     * Converts this Maybe into a Single instance composing cancellation
     * through and turning an empty Maybe into a signal of NoSuchElementException.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code toSingle} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @return the new Single instance
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Single<T> toSingle() {
        return RxJavaPlugins.onAssembly(new MaybeToSingle<T>(this, null));
    }

    /**
     * Returns a Maybe instance that if this Maybe emits an error, it will emit an onComplete
     * and swallow the throwable.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code onErrorComplete} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @return the new Completable instance
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Maybe<T> onErrorComplete() {
        return onErrorComplete(Functions.alwaysTrue());
    }

    /**
     * Returns a Maybe instance that if this Maybe emits an error and the predicate returns
     * true, it will emit an onComplete and swallow the throwable.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code onErrorComplete} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param predicate the predicate to call when an Throwable is emitted which should return true
     * if the Throwable should be swallowed and replaced with an onComplete.
     * @return the new Completable instance
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Maybe<T> onErrorComplete(final Predicate<? super Throwable> predicate) {
        ObjectHelper.requireNonNull(predicate, "predicate is null");

        return RxJavaPlugins.onAssembly(new MaybeOnErrorComplete<T>(this, predicate));
    }

    /**
     * Instructs a Maybe to pass control to another MaybeSource rather than invoking
     * {@link MaybeObserver#onError onError} if it encounters an error.
     * <p>
     * <img width="640" height="310" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/onErrorResumeNext.png" alt="">
     * <p>
     * You can use this to prevent errors from propagating or to supply fallback data should errors be
     * encountered.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code onErrorResumeNext} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param next
     *            the next Maybe source that will take over if the source Maybe encounters
     *            an error
     * @return the new Maybe instance
     * @see <a href="http://reactivex.io/documentation/operators/catch.html">ReactiveX operators documentation: Catch</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Maybe<T> onErrorResumeNext(final MaybeSource<? extends T> next) {
        ObjectHelper.requireNonNull(next, "next is null");
        return onErrorResumeNext(Functions.justFunction(next));
    }

    /**
     * Instructs a Maybe to pass control to another Maybe rather than invoking
     * {@link MaybeObserver#onError onError} if it encounters an error.
     * <p>
     * <img width="640" height="310" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/onErrorResumeNext.png" alt="">
     * <p>
     * You can use this to prevent errors from propagating or to supply fallback data should errors be
     * encountered.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code onErrorResumeNext} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param resumeFunction
     *            a function that returns a MaybeSource that will take over if the source Maybe encounters
     *            an error
     * @return the new Maybe instance
     * @see <a href="http://reactivex.io/documentation/operators/catch.html">ReactiveX operators documentation: Catch</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Maybe<T> onErrorResumeNext(Function<? super Throwable, ? extends MaybeSource<? extends T>> resumeFunction) {
        ObjectHelper.requireNonNull(resumeFunction, "resumeFunction is null");
        return RxJavaPlugins.onAssembly(new MaybeOnErrorNext<T>(this, resumeFunction, true));
    }

    /**
     * Instructs a Maybe to emit an item (returned by a specified function) rather than invoking
     * {@link MaybeObserver#onError onError} if it encounters an error.
     * <p>
     * <img width="640" height="310" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/onErrorReturn.png" alt="">
     * <p>
     * You can use this to prevent errors from propagating or to supply fallback data should errors be
     * encountered.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code onErrorReturn} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param valueSupplier
     *            a function that returns a single value that will be emitted as success value
     *            the current Maybe signals an onError event
     * @return the new Maybe instance
     * @see <a href="http://reactivex.io/documentation/operators/catch.html">ReactiveX operators documentation: Catch</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Maybe<T> onErrorReturn(Function<? super Throwable, ? extends T> valueSupplier) {
        ObjectHelper.requireNonNull(valueSupplier, "valueSupplier is null");
        return RxJavaPlugins.onAssembly(new MaybeOnErrorReturn<T>(this, valueSupplier));
    }

    /**
     * Instructs a Maybe to emit an item (returned by a specified function) rather than invoking
     * {@link MaybeObserver#onError onError} if it encounters an error.
     * <p>
     * <img width="640" height="310" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/onErrorReturn.png" alt="">
     * <p>
     * You can use this to prevent errors from propagating or to supply fallback data should errors be
     * encountered.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code onErrorReturnItem} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param item
     *            the value that is emitted as onSuccess in case this Maybe signals an onError
     * @return the new Maybe instance
     * @see <a href="http://reactivex.io/documentation/operators/catch.html">ReactiveX operators documentation: Catch</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Maybe<T> onErrorReturnItem(final T item) {
        ObjectHelper.requireNonNull(item, "item is null");
        return onErrorReturn(Functions.justFunction(item));
    }

    /**
     * Instructs a Maybe to pass control to another MaybeSource rather than invoking
     * {@link MaybeObserver#onError onError} if it encounters an {@link java.lang.Exception}.
     * <p>
     * This differs from {@link #onErrorResumeNext} in that this one does not handle {@link java.lang.Throwable}
     * or {@link java.lang.Error} but lets those continue through.
     * <p>
     * <img width="640" height="333" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/onExceptionResumeNextViaMaybe.png" alt="">
     * <p>
     * You can use this to prevent exceptions from propagating or to supply fallback data should exceptions be
     * encountered.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code onExceptionResumeNext} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param next
     *            the next MaybeSource that will take over if the source Maybe encounters
     *            an exception
     * @return the new Maybe instance
     * @see <a href="http://reactivex.io/documentation/operators/catch.html">ReactiveX operators documentation: Catch</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Maybe<T> onExceptionResumeNext(final MaybeSource<? extends T> next) {
        ObjectHelper.requireNonNull(next, "next is null");
        return RxJavaPlugins.onAssembly(new MaybeOnErrorNext<T>(this, Functions.justFunction(next), false));
    }
    /**
     * Nulls out references to the upstream producer and downstream MaybeObserver if
     * the sequence is terminated or downstream calls dispose().
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code onTerminateDetach} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @return a Maybe which out references to the upstream producer and downstream MaybeObserver if
     * the sequence is terminated or downstream calls dispose()
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Maybe<T> onTerminateDetach() {
        return RxJavaPlugins.onAssembly(new MaybeDetach<T>(this));
    }

    /**
     * Returns a Flowable that repeats the sequence of items emitted by the source Maybe indefinitely.
     * <p>
     * <img width="640" height="309" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/repeat.o.png" alt="">
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The operator honors downstream backpressure.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code repeat} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @return a Flowable that emits the items emitted by the source Maybe repeatedly and in sequence
     * @see <a href="http://reactivex.io/documentation/operators/repeat.html">ReactiveX operators documentation: Repeat</a>
     */
    @BackpressureSupport(BackpressureKind.FULL)
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Flowable<T> repeat() {
        return repeat(Long.MAX_VALUE);
    }

    /**
     * Returns a Flowable that repeats the sequence of items emitted by the source Maybe at most
     * {@code count} times.
     * <p>
     * <img width="640" height="310" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/repeat.on.png" alt="">
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>This operator honors downstream backpressure.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code repeat} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param times
     *            the number of times the source Maybe items are repeated, a count of 0 will yield an empty
     *            sequence
     * @return a Flowable that repeats the sequence of items emitted by the source Maybe at most
     *         {@code count} times
     * @throws IllegalArgumentException
     *             if {@code count} is less than zero
     * @see <a href="http://reactivex.io/documentation/operators/repeat.html">ReactiveX operators documentation: Repeat</a>
     */
    @BackpressureSupport(BackpressureKind.FULL)
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Flowable<T> repeat(long times) {
        return toFlowable().repeat(times);
    }

    /**
     * Returns a Flowable that repeats the sequence of items emitted by the source Maybe until
     * the provided stop function returns true.
     * <p>
     * <img width="640" height="310" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/repeat.on.png" alt="">
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>This operator honors downstream backpressure.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code repeatUntil} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param stop
     *                a boolean supplier that is called when the current Flowable completes and unless it returns
     *                false, the current Flowable is resubscribed
     * @return the new Flowable instance
     * @throws NullPointerException
     *             if {@code stop} is null
     * @see <a href="http://reactivex.io/documentation/operators/repeat.html">ReactiveX operators documentation: Repeat</a>
     */
    @BackpressureSupport(BackpressureKind.FULL)
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Flowable<T> repeatUntil(BooleanSupplier stop) {
        return toFlowable().repeatUntil(stop);
    }

    /**
     * Returns a Flowable that emits the same values as the source Publisher with the exception of an
     * {@code onComplete}. An {@code onComplete} notification from the source will result in the emission of
     * a {@code void} item to the Publisher provided as an argument to the {@code notificationHandler}
     * function. If that Publisher calls {@code onComplete} or {@code onError} then {@code repeatWhen} will
     * call {@code onComplete} or {@code onError} on the child subscription. Otherwise, this Publisher will
     * resubscribe to the source Publisher.
     * <p>
     * <img width="640" height="430" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/repeatWhen.f.png" alt="">
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The operator honors downstream backpressure and expects the source {@code Publisher} to honor backpressure as well.
     *  If this expectation is violated, the operator <em>may</em> throw an {@code IllegalStateException}.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code repeatWhen} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param handler
     *            receives a Publisher of notifications with which a user can complete or error, aborting the repeat.
     * @return the source Publisher modified with repeat logic
     * @see <a href="http://reactivex.io/documentation/operators/repeat.html">ReactiveX operators documentation: Repeat</a>
     */
    @BackpressureSupport(BackpressureKind.FULL)
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Flowable<T> repeatWhen(final Function<? super Flowable<Object>, ? extends Publisher<?>> handler) {
        return toFlowable().repeatWhen(handler);
    }


    /**
     * Returns a Maybe that mirrors the source Maybe, resubscribing to it if it calls {@code onError}
     * (infinite retry count).
     * <p>
     * <img width="640" height="315" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/retry.png" alt="">
     * <p>
     * If the source Maybe calls {@link MaybeObserver#onError}, this method will resubscribe to the source
     * Maybe rather than propagating the {@code onError} call.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code retry} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @return the nww Maybe instance
     * @see <a href="http://reactivex.io/documentation/operators/retry.html">ReactiveX operators documentation: Retry</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Maybe<T> retry() {
        return retry(Long.MAX_VALUE, Functions.alwaysTrue());
    }

    /**
     * Returns a Maybe that mirrors the source Maybe, resubscribing to it if it calls {@code onError}
     * and the predicate returns true for that specific exception and retry count.
     * <p>
     * <img width="640" height="315" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/retry.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code retry} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param predicate
     *            the predicate that determines if a resubscription may happen in case of a specific exception
     *            and retry count
     * @return the nww Maybe instance
     * @see #retry()
     * @see <a href="http://reactivex.io/documentation/operators/retry.html">ReactiveX operators documentation: Retry</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Maybe<T> retry(BiPredicate<? super Integer, ? super Throwable> predicate) {
        return toFlowable().retry(predicate).singleElement();
    }

    /**
     * Returns a Maybe that mirrors the source Maybe, resubscribing to it if it calls {@code onError}
     * up to a specified number of retries.
     * <p>
     * <img width="640" height="315" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/retry.png" alt="">
     * <p>
     * If the source Maybe calls {@link MaybeObserver#onError}, this method will resubscribe to the source
     * Maybe for a maximum of {@code count} resubscriptions rather than propagating the
     * {@code onError} call.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code retry} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param count
     *            number of retry attempts before failing
     * @return the new Maybe instance
     * @see <a href="http://reactivex.io/documentation/operators/retry.html">ReactiveX operators documentation: Retry</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Maybe<T> retry(long count) {
        return retry(count, Functions.alwaysTrue());
    }

    /**
     * Retries at most times or until the predicate returns false, whichever happens first.
     *
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code retry} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param times the number of times to repeat
     * @param predicate the predicate called with the failure Throwable and should return true to trigger a retry.
     * @return the new Maybe instance
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Maybe<T> retry(long times, Predicate<? super Throwable> predicate) {
        return toFlowable().retry(times, predicate).singleElement();
    }

    /**
     * Retries the current Maybe if it fails and the predicate returns true.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code retry} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param predicate the predicate that receives the failure Throwable and should return true to trigger a retry.
     * @return the new Maybe instance
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Maybe<T> retry(Predicate<? super Throwable> predicate) {
        return retry(Long.MAX_VALUE, predicate);
    }

    /**
     * Retries until the given stop function returns true.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code retryUntil} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param stop the function that should return true to stop retrying
     * @return the new Maybe instance
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Maybe<T> retryUntil(final BooleanSupplier stop) {
        ObjectHelper.requireNonNull(stop, "stop is null");
        return retry(Long.MAX_VALUE, Functions.predicateReverseFor(stop));
    }

    /**
     * Returns a Maybe that emits the same values as the source Maybe with the exception of an
     * {@code onError}. An {@code onError} notification from the source will result in the emission of a
     * {@link Throwable} item to the Publisher provided as an argument to the {@code notificationHandler}
     * function. If that Publisher calls {@code onComplete} or {@code onError} then {@code retry} will call
     * {@code onComplete} or {@code onError} on the child subscription. Otherwise, this Publisher will
     * resubscribe to the source Publisher.
     * <p>
     * <img width="640" height="430" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/retryWhen.f.png" alt="">
     *
     * Example:
     *
     * This retries 3 times, each time incrementing the number of seconds it waits.
     *
     * <pre><code>
     *  Flowable.create((FlowableEmitter&lt;? super String&gt; s) -&gt; {
     *      System.out.println("subscribing");
     *      s.onError(new RuntimeException("always fails"));
     *  }, BackpressureStrategy.BUFFER).retryWhen(attempts -&gt; {
     *      return attempts.zipWith(Publisher.range(1, 3), (n, i) -&gt; i).flatMap(i -&gt; {
     *          System.out.println("delay retry by " + i + " second(s)");
     *          return Publisher.timer(i, TimeUnit.SECONDS);
     *      });
     *  }).blockingForEach(System.out::println);
     * </code></pre>
     *
     * Output is:
     *
     * <pre> {@code
     * subscribing
     * delay retry by 1 second(s)
     * subscribing
     * delay retry by 2 second(s)
     * subscribing
     * delay retry by 3 second(s)
     * subscribing
     * } </pre>
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code retryWhen} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param handler
     *            receives a Publisher of notifications with which a user can complete or error, aborting the
     *            retry
     * @return the new Maybe instance
     * @see <a href="http://reactivex.io/documentation/operators/retry.html">ReactiveX operators documentation: Retry</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Maybe<T> retryWhen(
            final Function<? super Flowable<Throwable>, ? extends Publisher<?>> handler) {
        return toFlowable().retryWhen(handler).singleElement();
    }

    /**
     * Subscribes to a Maybe and ignores {@code onSuccess} and {@code onComplete} emissions.
     * <p>
     * If the Maybe emits an error, it is wrapped into an
     * {@link io.reactivex.exceptions.OnErrorNotImplementedException OnErrorNotImplementedException}
     * and routed to the RxJavaPlugins.onError handler.
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
        return subscribe(Functions.emptyConsumer(), Functions.ON_ERROR_MISSING, Functions.EMPTY_ACTION);
    }

    /**
     * Subscribes to a Maybe and provides a callback to handle the items it emits.
     * <p>
     * If the Maybe emits an error, it is wrapped into an
     * {@link io.reactivex.exceptions.OnErrorNotImplementedException OnErrorNotImplementedException}
     * and routed to the RxJavaPlugins.onError handler.
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
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Disposable subscribe(Consumer<? super T> onSuccess) {
        return subscribe(onSuccess, Functions.ON_ERROR_MISSING, Functions.EMPTY_ACTION);
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
     * @return a {@link Disposable} reference with which the caller can stop receiving items before
     *         the Maybe has finished sending them
     * @see <a href="http://reactivex.io/documentation/operators/subscribe.html">ReactiveX operators documentation: Subscribe</a>
     * @throws NullPointerException
     *             if {@code onSuccess} is null, or
     *             if {@code onError} is null
     */
    @CheckReturnValue
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
     * @throws NullPointerException
     *             if {@code onSuccess} is null, or
     *             if {@code onError} is null, or
     *             if {@code onComplete} is null
     * @see <a href="http://reactivex.io/documentation/operators/subscribe.html">ReactiveX operators documentation: Subscribe</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Disposable subscribe(Consumer<? super T> onSuccess, Consumer<? super Throwable> onError,
            Action onComplete) {
        ObjectHelper.requireNonNull(onSuccess, "onSuccess is null");
        ObjectHelper.requireNonNull(onError, "onError is null");
        ObjectHelper.requireNonNull(onComplete, "onComplete is null");
        return subscribeWith(new MaybeCallbackObserver<T>(onSuccess, onError, onComplete));
    }

    @SchedulerSupport(SchedulerSupport.NONE)
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
     * <img width="640" height="752" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.subscribeOn.png" alt="">
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
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    public final Maybe<T> subscribeOn(Scheduler scheduler) {
        ObjectHelper.requireNonNull(scheduler, "scheduler is null");
        return RxJavaPlugins.onAssembly(new MaybeSubscribeOn<T>(this, scheduler));
    }

    /**
     * Subscribes a given MaybeObserver (subclass) to this Maybe and returns the given
     * MaybeObserver as is.
     * <p>Usage example:
     * <pre><code>
     * Maybe&lt;Integer&gt; source = Maybe.just(1);
     * CompositeDisposable composite = new CompositeDisposable();
     *
     * MaybeObserver&lt;Integer&gt; ms = new MaybeObserver&lt;&gt;() {
     *     // ...
     * };
     *
     * composite.add(source.subscribeWith(ms));
     * </code></pre>
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code subscribeWith} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <E> the type of the MaybeObserver to use and return
     * @param observer the MaybeObserver (subclass) to use and return, not null
     * @return the input {@code subscriber}
     * @throws NullPointerException if {@code subscriber} is null
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public final <E extends MaybeObserver<? super T>> E subscribeWith(E observer) {
        subscribe(observer);
        return observer;
    }

    /**
     * Returns a Maybe that emits the items emitted by the source Maybe or the items of an alternate
     * MaybeSource if the current Maybe is empty.
     * <p>
     * <img width="640" height="445" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/switchifempty.m.png" alt="">
     * <p/>
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code switchIfEmpty} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param other
     *              the alternate MaybeSource to subscribe to if the main does not emit any items
     * @return  a Maybe that emits the items emitted by the source Maybe or the items of an
     *          alternate MaybeSource if the source Maybe is empty.
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Maybe<T> switchIfEmpty(MaybeSource<? extends T> other) {
        ObjectHelper.requireNonNull(other, "other is null");
        return RxJavaPlugins.onAssembly(new MaybeSwitchIfEmpty<T>(this, other));
    }

    /**
     * Returns a Maybe that emits the items emitted by the source Maybe until a second MaybeSource
     * emits an item.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/takeUntil.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code takeUntil} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param other
     *            the MaybeSource whose first emitted item will cause {@code takeUntil} to stop emitting items
     *            from the source Maybe
     * @param <U>
     *            the type of items emitted by {@code other}
     * @return a Maybe that emits the items emitted by the source Maybe until such time as {@code other} emits its first item
     * @see <a href="http://reactivex.io/documentation/operators/takeuntil.html">ReactiveX operators documentation: TakeUntil</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public final <U> Maybe<T> takeUntil(MaybeSource<U> other) {
        ObjectHelper.requireNonNull(other, "other is null");
        return RxJavaPlugins.onAssembly(new MaybeTakeUntilMaybe<T, U>(this, other));
    }

    /**
     * Returns a Maybe that emits the item emitted by the source Maybe until a second Publisher
     * emits an item.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/takeUntil.png" alt="">
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The {@code Publisher} is consumed in an unbounded fashion and is cancelled after the first item
     *  emitted.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code takeUntil} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param other
     *            the Publisher whose first emitted item will cause {@code takeUntil} to stop emitting items
     *            from the source Publisher
     * @param <U>
     *            the type of items emitted by {@code other}
     * @return a Maybe that emits the items emitted by the source Maybe until such time as {@code other} emits its first item
     * @see <a href="http://reactivex.io/documentation/operators/takeuntil.html">ReactiveX operators documentation: TakeUntil</a>
     */
    @BackpressureSupport(BackpressureKind.UNBOUNDED_IN)
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public final <U> Maybe<T> takeUntil(Publisher<U> other) {
        ObjectHelper.requireNonNull(other, "other is null");
        return RxJavaPlugins.onAssembly(new MaybeTakeUntilPublisher<T, U>(this, other));
    }

    /**
     * Returns a Maybe that mirrors the source Maybe but applies a timeout policy for each emitted
     * item. If the next item isn't emitted within the specified timeout duration starting from its predecessor,
     * the resulting Maybe terminates and notifies MaybeObservers of a {@code TimeoutException}.
     * <p>
     * <img width="640" height="305" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/timeout.1.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This version of {@code timeout} operates by default on the {@code computation} {@link Scheduler}.</dd>
     * </dl>
     *
     * @param timeout
     *            maximum duration between emitted items before a timeout occurs
     * @param timeUnit
     *            the unit of time that applies to the {@code timeout} argument.
     * @return the new Maybe instance
     * @see <a href="http://reactivex.io/documentation/operators/timeout.html">ReactiveX operators documentation: Timeout</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.COMPUTATION)
    public final Maybe<T> timeout(long timeout, TimeUnit timeUnit) {
        return timeout(timeout, timeUnit, Schedulers.computation());
    }

    /**
     * Returns a Maybe that mirrors the source Maybe but applies a timeout policy for each emitted
     * item. If the next item isn't emitted within the specified timeout duration starting from its predecessor,
     * the resulting Maybe begins instead to mirror a fallback MaybeSource.
     * <p>
     * <img width="640" height="305" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/timeout.2.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This version of {@code timeout} operates by default on the {@code computation} {@link Scheduler}.</dd>
     * </dl>
     *
     * @param timeout
     *            maximum duration between items before a timeout occurs
     * @param timeUnit
     *            the unit of time that applies to the {@code timeout} argument
     * @param fallback
     *            the fallback MaybeSource to use in case of a timeout
     * @return the new Maybe instance
     * @see <a href="http://reactivex.io/documentation/operators/timeout.html">ReactiveX operators documentation: Timeout</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.COMPUTATION)
    public final Maybe<T> timeout(long timeout, TimeUnit timeUnit, MaybeSource<? extends T> fallback) {
        ObjectHelper.requireNonNull(fallback, "other is null");
        return timeout(timeout, timeUnit, Schedulers.computation(), fallback);
    }

    /**
     * Returns a Maybe that mirrors the source Maybe but applies a timeout policy for each emitted
     * item using a specified Scheduler. If the next item isn't emitted within the specified timeout duration
     * starting from its predecessor, the resulting Maybe begins instead to mirror a fallback MaybeSource.
     * <p>
     * <img width="640" height="305" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/timeout.2s.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>You specify which {@link Scheduler} this operator will use</dd>
     * </dl>
     *
     * @param timeout
     *            maximum duration between items before a timeout occurs
     * @param timeUnit
     *            the unit of time that applies to the {@code timeout} argument
     * @param fallback
     *            the MaybeSource to use as the fallback in case of a timeout
     * @param scheduler
     *            the {@link Scheduler} to run the timeout timers on
     * @return the new Maybe instance
     * @see <a href="http://reactivex.io/documentation/operators/timeout.html">ReactiveX operators documentation: Timeout</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    public final Maybe<T> timeout(long timeout, TimeUnit timeUnit, Scheduler scheduler, MaybeSource<? extends T> fallback) {
        ObjectHelper.requireNonNull(fallback, "fallback is null");
        return timeout(timer(timeout, timeUnit, scheduler), fallback);
    }

    /**
     * Returns a Maybe that mirrors the source Maybe but applies a timeout policy for each emitted
     * item, where this policy is governed on a specified Scheduler. If the next item isn't emitted within the
     * specified timeout duration starting from its predecessor, the resulting Maybe terminates and
     * notifies MaybeObservers of a {@code TimeoutException}.
     * <p>
     * <img width="640" height="305" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/timeout.1s.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>You specify which {@link Scheduler} this operator will use</dd>
     * </dl>
     *
     * @param timeout
     *            maximum duration between items before a timeout occurs
     * @param timeUnit
     *            the unit of time that applies to the {@code timeout} argument
     * @param scheduler
     *            the Scheduler to run the timeout timers on
     * @return the new Maybe instance
     * @see <a href="http://reactivex.io/documentation/operators/timeout.html">ReactiveX operators documentation: Timeout</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    public final Maybe<T> timeout(long timeout, TimeUnit timeUnit, Scheduler scheduler) {
        return timeout(timer(timeout, timeUnit, scheduler));
    }

    /**
     * If this Maybe source didn't signal an event before the timeoutIndicator MaybeSource signals, a
     * TimeoutException is signalled instead.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code timeout} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <U> the value type of the
     * @param timeoutIndicator the MaybeSource that indicates the timeout by signalling onSuccess
     * or onComplete.
     * @return the new Maybe instance
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public final <U> Maybe<T> timeout(MaybeSource<U> timeoutIndicator) {
        ObjectHelper.requireNonNull(timeoutIndicator, "timeoutIndicator is null");
        return RxJavaPlugins.onAssembly(new MaybeTimeoutMaybe<T, U>(this, timeoutIndicator, null));
    }

    /**
     * If the current Maybe source didn't signal an event before the timeoutIndicator MaybeSource signals,
     * the current Maybe is cancelled and the {@code fallback} MaybeSource subscribed to
     * as a continuation.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code timeout} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <U> the value type of the
     * @param timeoutIndicator the MaybeSource that indicates the timeout by signalling onSuccess
     * or onComplete.
     * @param fallback the MaybeSource that is subscribed to if the current Maybe times out
     * @return the new Maybe instance
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public final <U> Maybe<T> timeout(MaybeSource<U> timeoutIndicator, MaybeSource<? extends T> fallback) {
        ObjectHelper.requireNonNull(timeoutIndicator, "timeoutIndicator is null");
        ObjectHelper.requireNonNull(fallback, "fallback is null");
        return RxJavaPlugins.onAssembly(new MaybeTimeoutMaybe<T, U>(this, timeoutIndicator, fallback));
    }

    /**
     * If this Maybe source didn't signal an event before the timeoutIndicator Publisher signals, a
     * TimeoutException is signalled instead.
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The {@code timeoutIndicator} {@link Publisher} is consumed in an unbounded manner and
     *  is cancelled after its first item.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code timeout} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <U> the value type of the
     * @param timeoutIndicator the MaybeSource that indicates the timeout by signalling onSuccess
     * or onComplete.
     * @return the new Maybe instance
     */
    @BackpressureSupport(BackpressureKind.UNBOUNDED_IN)
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public final <U> Maybe<T> timeout(Publisher<U> timeoutIndicator) {
        ObjectHelper.requireNonNull(timeoutIndicator, "timeoutIndicator is null");
        return RxJavaPlugins.onAssembly(new MaybeTimeoutPublisher<T, U>(this, timeoutIndicator, null));
    }

    /**
     * If the current Maybe source didn't signal an event before the timeoutIndicator Publisher signals,
     * the current Maybe is cancelled and the {@code fallback} MaybeSource subscribed to
     * as a continuation.
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The {@code timeoutIndicator} {@link Publisher} is consumed in an unbounded manner and
     *  is cancelled after its first item.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code timeout} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <U> the value type of the
     * @param timeoutIndicator the MaybeSource that indicates the timeout by signalling onSuccess
     * or onComplete
     * @param fallback the MaybeSource that is subscribed to if the current Maybe times out
     * @return the new Maybe instance
     */
    @BackpressureSupport(BackpressureKind.UNBOUNDED_IN)
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public final <U> Maybe<T> timeout(Publisher<U> timeoutIndicator, MaybeSource<? extends T> fallback) {
        ObjectHelper.requireNonNull(timeoutIndicator, "timeoutIndicator is null");
        ObjectHelper.requireNonNull(fallback, "fallback is null");
        return RxJavaPlugins.onAssembly(new MaybeTimeoutPublisher<T, U>(this, timeoutIndicator, fallback));
    }

    /**
     * Returns a Maybe which makes sure when a MaybeObserver disposes the Disposable,
     * that call is propagated up on the specified scheduler
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code unsubscribeOn} calls dispose() of the upstream on the {@link Scheduler} you specify.</dd>
     * </dl>
     * @param scheduler the target scheduler where to execute the cancellation
     * @return the new Maybe instance
     * @throws NullPointerException if scheduler is null
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    public final Maybe<T> unsubscribeOn(final Scheduler scheduler) {
        ObjectHelper.requireNonNull(scheduler, "scheduler is null");
        return RxJavaPlugins.onAssembly(new MaybeUnsubscribeOn<T>(this, scheduler));
    }

    /**
     * Waits until this and the other MaybeSource signal a success value then applies the given BiFunction
     * to those values and emits the BiFunction's resulting value to downstream.
     *
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/zip.png" alt="">
     *
     * <p>If either this or the other MaybeSource is empty or signals an error, the resulting Maybe will
     * terminate immediately and dispose the other source.
     *
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code zipWith} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <U>
     *            the type of items emitted by the {@code other} MaybeSource
     * @param <R>
     *            the type of items emitted by the resulting Maybe
     * @param other
     *            the other MaybeSource
     * @param zipper
     *            a function that combines the pairs of items from the two MaybeSources to generate the items to
     *            be emitted by the resulting Maybe
     * @return the new Maybe instance
     * @see <a href="http://reactivex.io/documentation/operators/zip.html">ReactiveX operators documentation: Zip</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public final <U, R> Maybe<R> zipWith(MaybeSource<? extends U> other, BiFunction<? super T, ? super U, ? extends R> zipper) {
        ObjectHelper.requireNonNull(other, "other is null");
        return zip(this, other, zipper);
    }

    // ------------------------------------------------------------------
    // Test helper
    // ------------------------------------------------------------------

    /**
     * Creates a TestObserver and subscribes
     * it to this Maybe.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code test} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @return the new TestObserver instance
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public final TestObserver<T> test() {
        TestObserver<T> ts = new TestObserver<T>();
        subscribe(ts);
        return ts;
    }

    /**
     * Creates a TestObserver optionally in cancelled state, then subscribes it to this Maybe.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code test} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param cancelled if true, the TestObserver will be cancelled before subscribing to this
     * Maybe.
     * @return the new TestObserver instance
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public final TestObserver<T> test(boolean cancelled) {
        TestObserver<T> ts = new TestObserver<T>();

        if (cancelled) {
            ts.cancel();
        }

        subscribe(ts);
        return ts;
    }
}
