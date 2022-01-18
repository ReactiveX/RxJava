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

package io.reactivex.rxjava3.core;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.*;

import org.reactivestreams.*;

import io.reactivex.rxjava3.annotations.*;
import io.reactivex.rxjava3.disposables.*;
import io.reactivex.rxjava3.exceptions.*;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.internal.functions.*;
import io.reactivex.rxjava3.internal.fuseable.*;
import io.reactivex.rxjava3.internal.jdk8.*;
import io.reactivex.rxjava3.internal.observers.*;
import io.reactivex.rxjava3.internal.operators.flowable.*;
import io.reactivex.rxjava3.internal.operators.maybe.*;
import io.reactivex.rxjava3.internal.operators.mixed.*;
import io.reactivex.rxjava3.internal.operators.observable.ObservableElementAtMaybe;
import io.reactivex.rxjava3.internal.util.ErrorMode;
import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.schedulers.*;

/**
 * The {@code Maybe} class represents a deferred computation and emission of a single value, no value at all or an exception.
 * <p>
 * The {@code Maybe} class implements the {@link MaybeSource} base interface and the default consumer
 * type it interacts with is the {@link MaybeObserver} via the {@link #subscribe(MaybeObserver)} method.
 * <p>
 * The {@code Maybe} operates with the following sequential protocol:
 * <pre><code>
 *     onSubscribe (onSuccess | onError | onComplete)?
 * </code></pre>
 * <p>
 * Note that {@code onSuccess}, {@code onError} and {@code onComplete} are mutually exclusive events; unlike {@link Observable},
 * {@code onSuccess} is never followed by {@code onError} or {@code onComplete}.
 * <p>
 * Like {@code Observable}, a running {@code Maybe} can be stopped through the {@link Disposable} instance
 * provided to consumers through {@link MaybeObserver#onSubscribe}.
 * <p>
 * Like an {@code Observable}, a {@code Maybe} is lazy, can be either "hot" or "cold", synchronous or
 * asynchronous. {@code Maybe} instances returned by the methods of this class are <em>cold</em>
 * and there is a standard <em>hot</em> implementation in the form of a subject:
 * {@link io.reactivex.rxjava3.subjects.MaybeSubject MaybeSubject}.
 * <p>
 * The documentation for this class makes use of marble diagrams. The following legend explains these diagrams:
 * <p>
 * <img width="640" height="370" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/maybe.png" alt="">
 * <p>
 * See {@link Flowable} or {@code Observable} for the
 * implementation of the Reactive Pattern for a stream or vector of values.
 * <p>
 * Example:
 * <pre><code>
 * Disposable d = Maybe.just("Hello World")
 *    .delay(10, TimeUnit.SECONDS, Schedulers.io())
 *    .subscribeWith(new DisposableMaybeObserver&lt;String&gt;() {
 *        &#64;Override
 *        public void onStart() {
 *            System.out.println("Started");
 *        }
 *
 *        &#64;Override
 *        public void onSuccess(String value) {
 *            System.out.println("Success: " + value);
 *        }
 *
 *        &#64;Override
 *        public void onError(Throwable error) {
 *            error.printStackTrace();
 *        }
 *
 *        &#64;Override
 *        public void onComplete() {
 *            System.out.println("Done!");
 *        }
 *    });
 * 
 * Thread.sleep(5000);
 * 
 * d.dispose();
 * </code></pre>
 * <p>
 * Note that by design, subscriptions via {@link #subscribe(MaybeObserver)} can't be disposed
 * from the outside (hence the
 * {@code void} return of the {@link #subscribe(MaybeObserver)} method) and it is the
 * responsibility of the implementor of the {@code MaybeObserver} to allow this to happen.
 * RxJava supports such usage with the standard
 * {@link io.reactivex.rxjava3.observers.DisposableMaybeObserver DisposableMaybeObserver} instance.
 * For convenience, the {@link #subscribeWith(MaybeObserver)} method is provided as well to
 * allow working with a {@code MaybeObserver} (or subclass) instance to be applied with in
 * a fluent manner (such as in the example above).
 *
 * @param <T> the value type
 * @since 2.0
 * @see io.reactivex.rxjava3.observers.DisposableMaybeObserver
 */
public abstract class Maybe<T> implements MaybeSource<T> {

    /**
     * Runs multiple {@link MaybeSource}s provided by an {@link Iterable} sequence and
     * signals the events of the first one that signals (disposing the rest).
     * <p>
     * <img width="640" height="518" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.amb.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code amb} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the value type
     * @param sources the {@code Iterable} sequence of sources. A subscription to each source will
     *            occur in the same order as in the {@code Iterable}.
     * @return the new {@code Maybe} instance
     * @throws NullPointerException if {@code sources} is {@code null}
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <@NonNull T> Maybe<T> amb(@NonNull Iterable<@NonNull ? extends MaybeSource<? extends T>> sources) {
        Objects.requireNonNull(sources, "sources is null");
        return RxJavaPlugins.onAssembly(new MaybeAmb<>(null, sources));
    }

    /**
     * Runs multiple {@link MaybeSource}s and signals the events of the first one that signals (disposing
     * the rest).
     * <p>
     * <img width="640" height="519" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.ambArray.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code ambArray} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the value type
     * @param sources the array of sources. A subscription to each source will
     *            occur in the same order as in the array.
     * @return the new {@code Maybe} instance
     * @throws NullPointerException if {@code sources} is {@code null}
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    @SafeVarargs
    public static <@NonNull T> Maybe<T> ambArray(@NonNull MaybeSource<? extends T>... sources) {
        Objects.requireNonNull(sources, "sources is null");
        if (sources.length == 0) {
            return empty();
        }
        if (sources.length == 1) {
            @SuppressWarnings("unchecked")
            MaybeSource<T> source = (MaybeSource<T>)sources[0];
            return wrap(source);
        }
        return RxJavaPlugins.onAssembly(new MaybeAmb<>(sources, null));
    }

    /**
     * Concatenate the single values, in a non-overlapping fashion, of the {@link MaybeSource} sources provided by
     * an {@link Iterable} sequence as a {@link Flowable} sequence.
     * <p>
     * <img width="640" height="526" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.concat.i.png" alt="">
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The returned {@code Flowable} honors the backpressure of the downstream consumer.</dd>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code concat} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the value type
     * @param sources the {@code Iterable} sequence of {@code MaybeSource} instances
     * @return the new {@code Flowable} instance
     * @throws NullPointerException if {@code sources} is {@code null}
     */
    @BackpressureSupport(BackpressureKind.FULL)
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <@NonNull T> Flowable<T> concat(@NonNull Iterable<@NonNull ? extends MaybeSource<? extends T>> sources) {
        Objects.requireNonNull(sources, "sources is null");
        return RxJavaPlugins.onAssembly(new MaybeConcatIterable<>(sources));
    }

    /**
     * Returns a {@link Flowable} that emits the items emitted by two {@link MaybeSource}s, one after the other.
     * <p>
     * <img width="640" height="423" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.concat.png" alt="">
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The returned {@code Flowable} honors the backpressure of the downstream consumer.</dd>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code concat} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T> the common value type
     * @param source1
     *            a {@code MaybeSource} to be concatenated
     * @param source2
     *            a {@code MaybeSource} to be concatenated
     * @return the new {@code Flowable} instance
     * @throws NullPointerException if {@code source1} or {@code source2} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/concat.html">ReactiveX operators documentation: Concat</a>
     */
    @BackpressureSupport(BackpressureKind.FULL)
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <@NonNull T> Flowable<T> concat(@NonNull MaybeSource<? extends T> source1, @NonNull MaybeSource<? extends T> source2) {
        Objects.requireNonNull(source1, "source1 is null");
        Objects.requireNonNull(source2, "source2 is null");
        return concatArray(source1, source2);
    }

    /**
     * Returns a {@link Flowable} that emits the items emitted by three {@link MaybeSource}s, one after the other.
     * <p>
     * <img width="640" height="423" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.concat.png" alt="">
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The returned {@code Flowable} honors the backpressure of the downstream consumer.</dd>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code concat} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T> the common value type
     * @param source1
     *            a {@code MaybeSource} to be concatenated
     * @param source2
     *            a {@code MaybeSource} to be concatenated
     * @param source3
     *            a {@code MaybeSource} to be concatenated
     * @return the new {@code Flowable} instance
     * @throws NullPointerException if {@code source1}, {@code source2} or {@code source3} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/concat.html">ReactiveX operators documentation: Concat</a>
     */
    @BackpressureSupport(BackpressureKind.FULL)
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <@NonNull T> Flowable<T> concat(
            @NonNull MaybeSource<? extends T> source1, @NonNull MaybeSource<? extends T> source2, @NonNull MaybeSource<? extends T> source3) {
        Objects.requireNonNull(source1, "source1 is null");
        Objects.requireNonNull(source2, "source2 is null");
        Objects.requireNonNull(source3, "source3 is null");
        return concatArray(source1, source2, source3);
    }

    /**
     * Returns a {@link Flowable} that emits the items emitted by four {@link MaybeSource}s, one after the other.
     * <p>
     * <img width="640" height="423" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.concat.png" alt="">
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The returned {@code Flowable} honors the backpressure of the downstream consumer.</dd>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code concat} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T> the common value type
     * @param source1
     *            a {@code MaybeSource} to be concatenated
     * @param source2
     *            a {@code MaybeSource} to be concatenated
     * @param source3
     *            a {@code MaybeSource} to be concatenated
     * @param source4
     *            a {@code MaybeSource} to be concatenated
     * @return the new {@code Flowable} instance
     * @throws NullPointerException if {@code source1}, {@code source2}, {@code source3} or {@code source4} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/concat.html">ReactiveX operators documentation: Concat</a>
     */
    @BackpressureSupport(BackpressureKind.FULL)
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <@NonNull T> Flowable<T> concat(
            @NonNull MaybeSource<? extends T> source1, @NonNull MaybeSource<? extends T> source2, @NonNull MaybeSource<? extends T> source3, @NonNull MaybeSource<? extends T> source4) {
        Objects.requireNonNull(source1, "source1 is null");
        Objects.requireNonNull(source2, "source2 is null");
        Objects.requireNonNull(source3, "source3 is null");
        Objects.requireNonNull(source4, "source4 is null");
        return concatArray(source1, source2, source3, source4);
    }

    /**
     * Concatenate the single values, in a non-overlapping fashion, of the {@link MaybeSource} sources provided by
     * a {@link Publisher} sequence as a {@link Flowable} sequence.
     * <p>
     * <img width="640" height="416" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.concat.p.png" alt="">
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The returned {@code Flowable} honors the backpressure of the downstream consumer and
     *  expects the {@code Publisher} to honor backpressure as well. If the sources {@code Publisher}
     *  violates this, a {@link io.reactivex.rxjava3.exceptions.MissingBackpressureException} is signaled.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code concat} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the value type
     * @param sources the {@code Publisher} of {@code MaybeSource} instances
     * @return the new {@code Flowable} instance
     * @throws NullPointerException if {@code sources} is {@code null}
     */
    @BackpressureSupport(BackpressureKind.FULL)
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public static <@NonNull T> Flowable<T> concat(@NonNull Publisher<@NonNull ? extends MaybeSource<? extends T>> sources) {
        return concat(sources, 2);
    }

    /**
     * Concatenate the single values, in a non-overlapping fashion, of the {@link MaybeSource} sources provided by
     * a {@link Publisher} sequence as a {@link Flowable} sequence.
     * <p>
     * <img width="640" height="416" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.concat.pn.png" alt="">
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The returned {@code Flowable} honors the backpressure of the downstream consumer and
     *  expects the {@code Publisher} to honor backpressure as well. If the sources {@code Publisher}
     *  violates this, a {@link io.reactivex.rxjava3.exceptions.MissingBackpressureException} is signaled.</dd>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code concat} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the value type
     * @param sources the {@code Publisher} of {@code MaybeSource} instances
     * @param prefetch the number of {@code MaybeSource}s to prefetch from the {@code Publisher}
     * @throws NullPointerException if {@code sources} is {@code null}
     * @throws IllegalArgumentException if {@code prefetch} is non-positive
     * @return the new {@code Flowable} instance
     */
    @BackpressureSupport(BackpressureKind.FULL)
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <@NonNull T> Flowable<T> concat(@NonNull Publisher<@NonNull ? extends MaybeSource<? extends T>> sources, int prefetch) {
        Objects.requireNonNull(sources, "sources is null");
        ObjectHelper.verifyPositive(prefetch, "prefetch");
        return RxJavaPlugins.onAssembly(new FlowableConcatMapMaybePublisher<>(sources, Functions.identity(), ErrorMode.IMMEDIATE, prefetch));
    }

    /**
     * Concatenate the single values, in a non-overlapping fashion, of the {@link MaybeSource} sources in the array
     * as a {@link Flowable} sequence.
     * <p>
     * <img width="640" height="526" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.concatArray.png" alt="">
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The returned {@code Flowable} honors the backpressure of the downstream consumer.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code concatArray} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the value type
     * @param sources the array of {@code MaybeSource} instances
     * @return the new {@code Flowable} instance
     * @throws NullPointerException if {@code sources} is {@code null}
     */
    @BackpressureSupport(BackpressureKind.FULL)
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    @SafeVarargs
    public static <@NonNull T> Flowable<T> concatArray(@NonNull MaybeSource<? extends T>... sources) {
        Objects.requireNonNull(sources, "sources is null");
        if (sources.length == 0) {
            return Flowable.empty();
        }
        if (sources.length == 1) {
            @SuppressWarnings("unchecked")
            MaybeSource<T> source = (MaybeSource<T>)sources[0];
            return RxJavaPlugins.onAssembly(new MaybeToFlowable<>(source));
        }
        return RxJavaPlugins.onAssembly(new MaybeConcatArray<>(sources));
    }

    /**
     * Concatenates a variable number of {@link MaybeSource} sources and delays errors from any of them
     * till all terminate as a {@link Flowable} sequence.
     * <p>
     * <img width="640" height="425" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.concatArrayDelayError.png" alt="">
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The operator honors backpressure from downstream.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code concatArrayDelayError} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param sources the array of sources
     * @param <T> the common base value type
     * @return the new {@code Flowable} instance
     * @throws NullPointerException if {@code sources} is {@code null}
     */
    @BackpressureSupport(BackpressureKind.FULL)
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @SafeVarargs
    @NonNull
    public static <@NonNull T> Flowable<T> concatArrayDelayError(@NonNull MaybeSource<? extends T>... sources) {
        Objects.requireNonNull(sources, "sources is null");
        if (sources.length == 0) {
            return Flowable.empty();
        } else
        if (sources.length == 1) {
            @SuppressWarnings("unchecked")
            MaybeSource<T> source = (MaybeSource<T>)sources[0];
            return RxJavaPlugins.onAssembly(new MaybeToFlowable<>(source));
        }
        return RxJavaPlugins.onAssembly(new MaybeConcatArrayDelayError<>(sources));
    }

    /**
     * Concatenates a sequence of {@link MaybeSource} eagerly into a {@link Flowable} sequence.
     * <p>
     * Eager concatenation means that once an observer subscribes, this operator subscribes to all of the
     * source {@code MaybeSource}s. The operator buffers the value emitted by these {@code MaybeSource}s and then drains them
     * in order, each one after the previous one completes.
     * <p>
     * <img width="640" height="490" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.concatArrayEager.png" alt="">
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The operator honors backpressure from downstream.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This method does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the value type
     * @param sources a sequence of {@code MaybeSource}s that need to be eagerly concatenated
     * @return the new {@code Flowable} instance with the specified concatenation behavior
     * @throws NullPointerException if {@code sources} is {@code null}
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @BackpressureSupport(BackpressureKind.FULL)
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    @SafeVarargs
    public static <@NonNull T> Flowable<T> concatArrayEager(@NonNull MaybeSource<? extends T>... sources) {
        return Flowable.fromArray(sources).concatMapEager((Function)MaybeToPublisher.instance());
    }
    /**
     * Concatenates a sequence of {@link MaybeSource} eagerly into a {@link Flowable} sequence.
     * <p>
     * Eager concatenation means that once an observer subscribes, this operator subscribes to all of the
     * source {@code MaybeSource}s. The operator buffers the value emitted by these {@code MaybeSource}s and then drains them
     * in order, each one after the previous one completes.
     * <p>
     * <img width="640" height="428" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.concatArrayEagerDelayError.png" alt="">
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The operator honors backpressure from downstream.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This method does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the value type
     * @param sources a sequence of {@code MaybeSource}s that need to be eagerly concatenated
     * @return the new {@code Flowable} instance with the specified concatenation behavior
     * @throws NullPointerException if {@code sources} is {@code null}
     * @since 3.0.0
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @BackpressureSupport(BackpressureKind.FULL)
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    @SafeVarargs
    public static <@NonNull T> Flowable<T> concatArrayEagerDelayError(@NonNull MaybeSource<? extends T>... sources) {
        return Flowable.fromArray(sources).concatMapEagerDelayError((Function)MaybeToPublisher.instance(), true);
    }

    /**
     * Concatenates the {@link Iterable} sequence of {@link MaybeSource}s into a single sequence by subscribing to each {@code MaybeSource},
     * one after the other, one at a time and delays any errors till the all inner {@code MaybeSource}s terminate
     * as a {@link Flowable} sequence.
     * <p>
     * <img width="640" height="451" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.concatDelayError3.i.png" alt="">
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The operator honors backpressure from downstream.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code concatDelayError} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T> the common element base type
     * @param sources the {@code Iterable} sequence of {@code MaybeSource}s
     * @return the new {@code Flowable} with the concatenating behavior
     * @throws NullPointerException if {@code sources} is {@code null}
     */
    @BackpressureSupport(BackpressureKind.FULL)
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <@NonNull T> Flowable<T> concatDelayError(@NonNull Iterable<@NonNull ? extends MaybeSource<? extends T>> sources) {
        return Flowable.fromIterable(sources).concatMapMaybeDelayError(Functions.identity());
    }

    /**
     * Concatenates the {@link Publisher} sequence of {@link MaybeSource}s into a single sequence by subscribing to each inner {@code MaybeSource},
     * one after the other, one at a time and delays any errors till the all inner and the outer {@code Publisher} terminate
     * as a {@link Flowable} sequence.
     * <p>
     * <img width="640" height="360" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.concatDelayError.p.png" alt="">
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>{@code concatDelayError} fully supports backpressure.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code concatDelayError} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T> the common element base type
     * @param sources the {@code Publisher} sequence of {@code MaybeSource}s
     * @return the new {@code Flowable} with the concatenating behavior
     * @throws NullPointerException if {@code sources} is {@code null}
     */
    @BackpressureSupport(BackpressureKind.FULL)
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public static <@NonNull T> Flowable<T> concatDelayError(@NonNull Publisher<@NonNull ? extends MaybeSource<? extends T>> sources) {
        return Flowable.fromPublisher(sources).concatMapMaybeDelayError(Functions.identity());
    }
    /**
     * Concatenates the {@link Publisher} sequence of {@link MaybeSource}s into a single sequence by subscribing to each inner {@code MaybeSource},
     * one after the other, one at a time and delays any errors till the all inner and the outer {@code Publisher} terminate
     * as a {@link Flowable} sequence.
     * <p>
     * <img width="640" height="299" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.concatDelayError.pn.png" alt="">
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>{@code concatDelayError} fully supports backpressure.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code concatDelayError} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T> the common element base type
     * @param sources the {@code Publisher} sequence of {@code MaybeSource}s
     * @param prefetch The number of upstream items to prefetch so that fresh items are
     *                 ready to be mapped when a previous {@code MaybeSource} terminates.
     *                 The operator replenishes after half of the prefetch amount has been consumed
     *                 and turned into {@code MaybeSource}s.
     * @return the new {@code Flowable} with the concatenating behavior
     * @throws NullPointerException if {@code sources} is {@code null}
     * @throws IllegalArgumentException if {@code prefetch} is non-positive
     * @since 3.0.0
     */
    @BackpressureSupport(BackpressureKind.FULL)
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public static <@NonNull T> Flowable<T> concatDelayError(@NonNull Publisher<@NonNull ? extends MaybeSource<? extends T>> sources, int prefetch) {
        return Flowable.fromPublisher(sources).concatMapMaybeDelayError(Functions.identity(), true, prefetch);
    }

    /**
     * Concatenates a sequence of {@link MaybeSource}s eagerly into a {@link Flowable} sequence.
     * <p>
     * <img width="640" height="526" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.concatEager.i.png" alt="">
     * <p>
     * Eager concatenation means that once an observer subscribes, this operator subscribes to all of the
     * source {@code MaybeSource}s. The operator buffers the values emitted by these {@code MaybeSource}s and then drains them
     * in order, each one after the previous one completes.
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>Backpressure is honored towards the downstream.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This method does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the value type
     * @param sources a sequence of {@code MaybeSource} that need to be eagerly concatenated
     * @return the new {@code Flowable} instance with the specified concatenation behavior
     * @throws NullPointerException if {@code sources} is {@code null}
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @BackpressureSupport(BackpressureKind.FULL)
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public static <@NonNull T> Flowable<T> concatEager(@NonNull Iterable<@NonNull ? extends MaybeSource<? extends T>> sources) {
        return Flowable.fromIterable(sources).concatMapEagerDelayError((Function)MaybeToPublisher.instance(), false);
    }

    /**
     * Concatenates a sequence of {@link MaybeSource}s eagerly into a {@link Flowable} sequence and
     * runs a limited number of the inner sequences at once.
     * <p>
     * <img width="640" height="439" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.concatEager.in.png" alt="">
     * <p>
     * Eager concatenation means that once an observer subscribes, this operator subscribes to all of the
     * source {@code MaybeSource}s. The operator buffers the values emitted by these {@code MaybeSource}s and then drains them
     * in order, each one after the previous one completes.
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>Backpressure is honored towards the downstream.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This method does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the value type
     * @param sources a sequence of {@code MaybeSource} that need to be eagerly concatenated
     * @param maxConcurrency the maximum number of concurrently running inner {@code MaybeSource}s; {@link Integer#MAX_VALUE}
     *                       is interpreted as all inner {@code MaybeSource}s can be active at the same time
     * @return the new {@code Flowable} instance with the specified concatenation behavior
     * @throws NullPointerException if {@code sources} is {@code null}
     * @throws IllegalArgumentException if {@code maxConcurrency} is non-positive
     * @since 3.0.0
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @BackpressureSupport(BackpressureKind.FULL)
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public static <@NonNull T> Flowable<T> concatEager(@NonNull Iterable<@NonNull ? extends MaybeSource<? extends T>> sources, int maxConcurrency) {
        return Flowable.fromIterable(sources).concatMapEagerDelayError((Function)MaybeToPublisher.instance(), false, maxConcurrency, 1);
    }

    /**
     * Concatenates a {@link Publisher} sequence of {@link MaybeSource}s eagerly into a {@link Flowable} sequence.
     * <p>
     * Eager concatenation means that once a subscriber subscribes, this operator subscribes to all of the
     * emitted source {@code MaybeSource}s as they are observed. The operator buffers the values emitted by these
     * {@code MaybeSource}s and then drains them in order, each one after the previous one completes.
     * <p>
     * <img width="640" height="511" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.concatEager.p.png" alt="">
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>Backpressure is honored towards the downstream and the outer {@code Publisher} is
     *  expected to support backpressure. Violating this assumption, the operator will
     *  signal {@link io.reactivex.rxjava3.exceptions.MissingBackpressureException}.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This method does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the value type
     * @param sources a sequence of {@code MaybeSource}s that need to be eagerly concatenated
     * @return the new {@code Flowable} instance with the specified concatenation behavior
     * @throws NullPointerException if {@code sources} is {@code null}
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @BackpressureSupport(BackpressureKind.FULL)
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public static <@NonNull T> Flowable<T> concatEager(@NonNull Publisher<@NonNull ? extends MaybeSource<? extends T>> sources) {
        return Flowable.fromPublisher(sources).concatMapEager((Function)MaybeToPublisher.instance());
    }

    /**
     * Concatenates a {@link Publisher} sequence of {@link MaybeSource}s eagerly into a {@link Flowable} sequence,
     * running at most the given number of inner {@code MaybeSource}s at once.
     * <p>
     * <img width="640" height="425" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.concatEager.pn.png" alt="">
     * <p>
     * Eager concatenation means that once a subscriber subscribes, this operator subscribes to all of the
     * emitted source {@code MaybeSource}s as they are observed. The operator buffers the values emitted by these
     * {@code MaybeSource}s and then drains them in order, each one after the previous one completes.
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>Backpressure is honored towards the downstream and the outer {@code Publisher} is
     *  expected to support backpressure. Violating this assumption, the operator will
     *  signal {@link io.reactivex.rxjava3.exceptions.MissingBackpressureException}.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This method does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the value type
     * @param sources a sequence of {@code MaybeSource}s that need to be eagerly concatenated
     * @param maxConcurrency the maximum number of concurrently running inner {@code MaybeSource}s; {@link Integer#MAX_VALUE}
     *                       is interpreted as all inner {@code MaybeSource}s can be active at the same time
     * @return the new {@code Flowable} instance with the specified concatenation behavior
     * @throws NullPointerException if {@code sources} is {@code null}
     * @throws IllegalArgumentException if {@code maxConcurrency} is non-positive
     * @since 3.0.0
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @BackpressureSupport(BackpressureKind.FULL)
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public static <@NonNull T> Flowable<T> concatEager(@NonNull Publisher<@NonNull ? extends MaybeSource<? extends T>> sources, int maxConcurrency) {
        return Flowable.fromPublisher(sources).concatMapEager((Function)MaybeToPublisher.instance(), maxConcurrency, 1);
    }

    /**
     * Concatenates a sequence of {@link MaybeSource}s eagerly into a {@link Flowable} sequence,
     * delaying errors until all inner {@code MaybeSource}s terminate.
     * <p>
     * <img width="640" height="428" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.concatEagerDelayError.i.png" alt="">
     * <p>
     * Eager concatenation means that once an observer subscribes, this operator subscribes to all of the
     * source {@code MaybeSource}s. The operator buffers the values emitted by these {@code MaybeSource}s and then drains them
     * in order, each one after the previous one completes.
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>Backpressure is honored towards the downstream.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This method does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the value type
     * @param sources a sequence of {@code MaybeSource} that need to be eagerly concatenated
     * @return the new {@code Flowable} instance with the specified concatenation behavior
     * @throws NullPointerException if {@code sources} is {@code null}
     * @since 3.0.0
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @BackpressureSupport(BackpressureKind.FULL)
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public static <@NonNull T> Flowable<T> concatEagerDelayError(@NonNull Iterable<@NonNull ? extends MaybeSource<? extends T>> sources) {
        return Flowable.fromIterable(sources).concatMapEagerDelayError((Function)MaybeToPublisher.instance(), true);
    }

    /**
     * Concatenates a sequence of {@link MaybeSource}s eagerly into a {@link Flowable} sequence,
     * delaying errors until all inner {@code MaybeSource}s terminate and
     * runs a limited number of inner {@code MaybeSource}s at once.
     * <p>
     * <img width="640" height="379" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.concatEagerDelayError.in.png" alt="">
     * <p>
     * Eager concatenation means that once an observer subscribes, this operator subscribes to all of the
     * source {@code MaybeSource}s. The operator buffers the values emitted by these {@code MaybeSource}s and then drains them
     * in order, each one after the previous one completes.
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>Backpressure is honored towards the downstream.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This method does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the value type
     * @param sources a sequence of {@code MaybeSource} that need to be eagerly concatenated
     * @param maxConcurrency the maximum number of concurrently running inner {@code MaybeSource}s; {@link Integer#MAX_VALUE}
     *                       is interpreted as all inner {@code MaybeSource}s can be active at the same time
     * @return the new {@code Flowable} instance with the specified concatenation behavior
     * @throws NullPointerException if {@code sources} is {@code null}
     * @throws IllegalArgumentException if {@code maxConcurrency} is non-positive
     * @since 3.0.0
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @BackpressureSupport(BackpressureKind.FULL)
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public static <@NonNull T> Flowable<T> concatEagerDelayError(@NonNull Iterable<@NonNull ? extends MaybeSource<? extends T>> sources, int maxConcurrency) {
        return Flowable.fromIterable(sources).concatMapEagerDelayError((Function)MaybeToPublisher.instance(), true, maxConcurrency, 1);
    }

    /**
     * Concatenates a {@link Publisher} sequence of {@link MaybeSource}s eagerly into a {@link Flowable} sequence,
     * delaying errors until all the inner and the outer sequence terminate.
     * <p>
     * <img width="640" height="495" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.concatEagerDelayError.p.png" alt="">
     * <p>
     * Eager concatenation means that once a subscriber subscribes, this operator subscribes to all of the
     * emitted source {@code MaybeSource}s as they are observed. The operator buffers the values emitted by these
     * {@code MaybeSource}s and then drains them in order, each one after the previous one completes.
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>Backpressure is honored towards the downstream and the outer {@code Publisher} is
     *  expected to support backpressure. Violating this assumption, the operator will
     *  signal {@link io.reactivex.rxjava3.exceptions.MissingBackpressureException}.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This method does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the value type
     * @param sources a sequence of {@code MaybeSource}s that need to be eagerly concatenated
     * @return the new {@code Flowable} instance with the specified concatenation behavior
     * @throws NullPointerException if {@code sources} is {@code null}
     * @since 3.0.0
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @BackpressureSupport(BackpressureKind.FULL)
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public static <@NonNull T> Flowable<T> concatEagerDelayError(@NonNull Publisher<@NonNull ? extends MaybeSource<? extends T>> sources) {
        return Flowable.fromPublisher(sources).concatMapEagerDelayError((Function)MaybeToPublisher.instance(), true);
    }

    /**
     * Concatenates a {@link Publisher} sequence of {@link MaybeSource}s eagerly into a {@link Flowable} sequence,
     * delaying errors until all the inner and the outer sequence terminate and
     * runs a limited number of the inner {@code MaybeSource}s at once.
     * <p>
     * <img width="640" height="421" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.concatEagerDelayError.pn.png" alt="">
     * <p>
     * Eager concatenation means that once a subscriber subscribes, this operator subscribes to all of the
     * emitted source {@code MaybeSource}s as they are observed. The operator buffers the values emitted by these
     * {@code MaybeSource}s and then drains them in order, each one after the previous one completes.
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>Backpressure is honored towards the downstream and the outer {@code Publisher} is
     *  expected to support backpressure. Violating this assumption, the operator will
     *  signal {@link io.reactivex.rxjava3.exceptions.MissingBackpressureException}.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This method does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the value type
     * @param sources a sequence of {@code MaybeSource}s that need to be eagerly concatenated
     * @param maxConcurrency the maximum number of concurrently running inner {@code MaybeSource}s; {@link Integer#MAX_VALUE}
     *                       is interpreted as all inner {@code MaybeSource}s can be active at the same time
     * @return the new {@code Flowable} instance with the specified concatenation behavior
     * @throws NullPointerException if {@code sources} is {@code null}
     * @throws IllegalArgumentException if {@code maxConcurrency} is non-positive
     * @since 3.0.0
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @BackpressureSupport(BackpressureKind.FULL)
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public static <@NonNull T> Flowable<T> concatEagerDelayError(@NonNull Publisher<@NonNull ? extends MaybeSource<? extends T>> sources, int maxConcurrency) {
        return Flowable.fromPublisher(sources).concatMapEagerDelayError((Function)MaybeToPublisher.instance(), true, maxConcurrency, 1);
    }

    /**
     * Provides an API (via a cold {@code Maybe}) that bridges the reactive world with the callback-style world.
     * <p>
     * <img width="640" height="499" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.create.png" alt="">
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
     * Whenever a {@link MaybeObserver} subscribes to the returned {@code Maybe}, the provided
     * {@link MaybeOnSubscribe} callback is invoked with a fresh instance of a {@link MaybeEmitter}
     * that will interact only with that specific {@code MaybeObserver}. If this {@code MaybeObserver}
     * disposes the flow (making {@link MaybeEmitter#isDisposed} return {@code true}),
     * other observers subscribed to the same returned {@code Maybe} are not affected.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code create} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the value type
     * @param onSubscribe the emitter that is called when a {@code MaybeObserver} subscribes to the returned {@code Maybe}
     * @return the new {@code Maybe} instance
     * @throws NullPointerException if {@code onSubscribe} is {@code null}
     * @see MaybeOnSubscribe
     * @see Cancellable
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <@NonNull T> Maybe<T> create(@NonNull MaybeOnSubscribe<T> onSubscribe) {
        Objects.requireNonNull(onSubscribe, "onSubscribe is null");
        return RxJavaPlugins.onAssembly(new MaybeCreate<>(onSubscribe));
    }

    /**
     * Calls a {@link Supplier} for each individual {@link MaybeObserver} to return the actual {@link MaybeSource} source to
     * be subscribed to.
     * <p>
     * <img width="640" height="498" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.defer.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code defer} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the value type
     * @param supplier the {@code Supplier} that is called for each individual {@code MaybeObserver} and
     * returns a {@code MaybeSource} instance to subscribe to
     * @return the new {@code Maybe} instance
     * @throws NullPointerException if {@code supplier} is {@code null}
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <@NonNull T> Maybe<T> defer(@NonNull Supplier<? extends @NonNull MaybeSource<? extends T>> supplier) {
        Objects.requireNonNull(supplier, "supplier is null");
        return RxJavaPlugins.onAssembly(new MaybeDefer<>(supplier));
    }

    /**
     * Returns a (singleton) {@code Maybe} instance that calls {@link MaybeObserver#onComplete onComplete}
     * immediately.
     * <p>
     * <img width="640" height="190" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/empty.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code empty} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the value type
     * @return the shared {@code Maybe} instance
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @SuppressWarnings("unchecked")
    @NonNull
    public static <@NonNull T> Maybe<T> empty() {
        return RxJavaPlugins.onAssembly((Maybe<T>)MaybeEmpty.INSTANCE);
    }

    /**
     * Returns a {@code Maybe} that invokes a subscriber's {@link MaybeObserver#onError onError} method when the
     * subscriber subscribes to it.
     * <p>
     * <img width="640" height="447" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.error.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code error} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param throwable
     *            the particular {@link Throwable} to pass to {@link MaybeObserver#onError onError}
     * @param <T>
     *            the type of the item (ostensibly) emitted by the {@code Maybe}
     * @return the new {@code Maybe} instance
     * @throws NullPointerException if {@code throwable} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/empty-never-throw.html">ReactiveX operators documentation: Throw</a>
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <@NonNull T> Maybe<T> error(@NonNull Throwable throwable) {
        Objects.requireNonNull(throwable, "throwable is null");
        return RxJavaPlugins.onAssembly(new MaybeError<>(throwable));
    }

    /**
     * Returns a {@code Maybe} that invokes a {@link MaybeObserver}'s {@link MaybeObserver#onError onError} method when the
     * {@code MaybeObserver} subscribes to it.
     * <p>
     * <img width="640" height="440" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.error.f.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code error} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param supplier
     *            a {@link Supplier} factory to return a {@link Throwable} for each individual {@code MaybeObserver}
     * @param <T>
     *            the type of the items (ostensibly) emitted by the {@code Maybe}
     * @return the new {@code Maybe} instance
     * @throws NullPointerException if {@code supplier} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/empty-never-throw.html">ReactiveX operators documentation: Throw</a>
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <@NonNull T> Maybe<T> error(@NonNull Supplier<? extends @NonNull Throwable> supplier) {
        Objects.requireNonNull(supplier, "supplier is null");
        return RxJavaPlugins.onAssembly(new MaybeErrorCallable<>(supplier));
    }

    /**
     * Returns a {@code Maybe} instance that runs the given {@link Action} for each {@link MaybeObserver} and
     * emits either its exception or simply completes.
     * <p>
     * <img width="640" height="287" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.fromAction.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code fromAction} does not operate by default on a particular {@link Scheduler}.</dd>
     *  <dt><b>Error handling:</b></dt>
     *  <dd> If the {@code Action} throws an exception, the respective {@link Throwable} is
     *  delivered to the downstream via {@link MaybeObserver#onError(Throwable)},
     *  except when the downstream has disposed the resulting {@code Maybe} source.
     *  In this latter case, the {@code Throwable} is delivered to the global error handler via
     *  {@link RxJavaPlugins#onError(Throwable)} as an {@link io.reactivex.rxjava3.exceptions.UndeliverableException UndeliverableException}.
     *  </dd>
     * </dl>
     * @param <T> the target type
     * @param action the {@code Action} to run for each {@code MaybeObserver}
     * @return the new {@code Maybe} instance
     * @throws NullPointerException if {@code action} is {@code null}
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <@NonNull T> Maybe<T> fromAction(@NonNull Action action) {
        Objects.requireNonNull(action, "action is null");
        return RxJavaPlugins.onAssembly(new MaybeFromAction<>(action));
    }

    /**
     * Wraps a {@link CompletableSource} into a {@code Maybe}.
     * <p>
     * <img width="640" height="280" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.fromCompletable.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code fromCompletable} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the target type
     * @param completableSource the {@code CompletableSource} to convert from
     * @return the new {@code Maybe} instance
     * @throws NullPointerException if {@code completableSource} is {@code null}
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <@NonNull T> Maybe<T> fromCompletable(@NonNull CompletableSource completableSource) {
        Objects.requireNonNull(completableSource, "completableSource is null");
        return RxJavaPlugins.onAssembly(new MaybeFromCompletable<>(completableSource));
    }

    /**
     * Wraps a {@link SingleSource} into a {@code Maybe}.
     * <p>
     * <img width="640" height="344" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.fromSingle.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code fromSingle} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the target type
     * @param single the {@code SingleSource} to convert from
     * @return the new {@code Maybe} instance
     * @throws NullPointerException if {@code single} is {@code null}
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <@NonNull T> Maybe<T> fromSingle(@NonNull SingleSource<T> single) {
        Objects.requireNonNull(single, "single is null");
        return RxJavaPlugins.onAssembly(new MaybeFromSingle<>(single));
    }

    /**
     * Returns a {@code Maybe} that invokes the given {@link Callable} for each individual {@link MaybeObserver} that
     * subscribes and emits the resulting non-{@code null} item via {@code onSuccess} while
     * considering a {@code null} result from the {@code Callable} as indication for valueless completion
     * via {@code onComplete}.
     * <p>
     * <img width="640" height="183" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.fromCallable.png" alt="">
     * <p>
     * This operator allows you to defer the execution of the given {@code Callable} until a {@code MaybeObserver}
     * subscribes to the  returned {@code Maybe}. In other terms, this source operator evaluates the given
     * {@code Callable} "lazily".
     * <p>
     * Note that the {@code null} handling of this operator differs from the similar source operators in the other
     * {@link io.reactivex.rxjava3.core base reactive classes}. Those operators signal a {@link NullPointerException} if the value returned by their
     * {@code Callable} is {@code null} while this {@code fromCallable} considers it to indicate the
     * returned {@code Maybe} is empty.
     * <dl>
     *   <dt><b>Scheduler:</b></dt>
     *   <dd>{@code fromCallable} does not operate by default on a particular {@link Scheduler}.</dd>
     *   <dt><b>Error handling:</b></dt>
     *   <dd>Any non-fatal exception thrown by {@link Callable#call()} will be forwarded to {@code onError},
     *   except if the {@code MaybeObserver} disposed the subscription in the meantime. In this latter case,
     *   the exception is forwarded to the global error handler via
     *   {@link io.reactivex.rxjava3.plugins.RxJavaPlugins#onError(Throwable)} wrapped into a
     *   {@link io.reactivex.rxjava3.exceptions.UndeliverableException UndeliverableException}.
     *   Fatal exceptions are rethrown and usually will end up in the executing thread's
     *   {@link java.lang.Thread.UncaughtExceptionHandler#uncaughtException(Thread, Throwable)} handler.</dd>
     * </dl>
     *
     * @param callable
     *         a {@code Callable} instance whose execution should be deferred and performed for each individual
     *         {@code MaybeObserver} that subscribes to the returned {@code Maybe}.
     * @param <T>
     *         the type of the item emitted by the {@code Maybe}.
     * @return the new {@code Maybe} instance
     * @throws NullPointerException if {@code callable} is {@code null}
     * @see #defer(Supplier)
     * @see #fromSupplier(Supplier)
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <T> Maybe<@NonNull T> fromCallable(@NonNull Callable<? extends @Nullable T> callable) {
        Objects.requireNonNull(callable, "callable is null");
        return RxJavaPlugins.onAssembly(new MaybeFromCallable<>(callable));
    }

    /**
     * Converts a {@link Future} into a {@code Maybe}, treating a {@code null} result as an indication of emptiness.
     * <p>
     * <img width="640" height="204" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.fromFuture.png" alt="">
     * <p>
     * The operator calls {@link Future#get()}, which is a blocking method, on the subscription thread.
     * It is recommended applying {@link #subscribeOn(Scheduler)} to move this blocking wait to a
     * background thread, and if the {@link Scheduler} supports it, interrupt the wait when the flow
     * is disposed.
     * <p>
     * Unlike 1.x, disposing the {@code Maybe} won't cancel the future. If necessary, one can use composition to achieve the
     * cancellation effect: {@code futureMaybe.doOnDispose(() -> future.cancel(true));}.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code fromFuture} does not operate by default on a particular {@code Scheduler}.</dd>
     * </dl>
     *
     * @param future
     *            the source {@code Future}
     * @param <T>
     *            the type of object that the {@code Future} returns, and also the type of item to be emitted by
     *            the resulting {@code Maybe}
     * @return the new {@code Maybe} instance
     * @throws NullPointerException if {@code future} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/from.html">ReactiveX operators documentation: From</a>
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <@NonNull T> Maybe<T> fromFuture(@NonNull Future<? extends T> future) {
        Objects.requireNonNull(future, "future is null");
        return RxJavaPlugins.onAssembly(new MaybeFromFuture<>(future, 0L, null));
    }

    /**
     * Converts a {@link Future} into a {@code Maybe}, with a timeout on the {@code Future}.
     * <p>
     * <img width="640" height="176" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.fromFuture.t.png" alt="">
     * <p>
     * The operator calls {@link Future#get(long, TimeUnit)}, which is a blocking method, on the subscription thread.
     * It is recommended applying {@link #subscribeOn(Scheduler)} to move this blocking wait to a
     * background thread, and if the {@link Scheduler} supports it, interrupt the wait when the flow
     * is disposed.
     * <p>
     * Unlike 1.x, disposing the {@code Maybe} won't cancel the future. If necessary, one can use composition to achieve the
     * cancellation effect: {@code futureMaybe.doOnCancel(() -> future.cancel(true));}.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code fromFuture} does not operate by default on a particular {@code Scheduler}.</dd>
     * </dl>
     *
     * @param future
     *            the source {@code Future}
     * @param timeout
     *            the maximum time to wait before calling {@code get}
     * @param unit
     *            the {@link TimeUnit} of the {@code timeout} argument
     * @param <T>
     *            the type of object that the {@code Future} returns, and also the type of item to be emitted by
     *            the resulting {@code Maybe}
     * @return the new {@code Maybe} instance
     * @throws NullPointerException if {@code future} or {@code unit} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/from.html">ReactiveX operators documentation: From</a>
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <@NonNull T> Maybe<T> fromFuture(@NonNull Future<? extends T> future, long timeout, @NonNull TimeUnit unit) {
        Objects.requireNonNull(future, "future is null");
        Objects.requireNonNull(unit, "unit is null");
        return RxJavaPlugins.onAssembly(new MaybeFromFuture<>(future, timeout, unit));
    }

    /**
     * Wraps an {@link ObservableSource} into a {@code Maybe} and emits the very first item
     * or completes if the source is empty.
     * <p>
     * <img width="640" height="276" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.fromObservable.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code fromObservable} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the target type
     * @param source the {@code ObservableSource} to convert from
     * @return the new {@code Maybe} instance
     * @throws NullPointerException if {@code source} is {@code null}
     * @since 3.0.0
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <@NonNull T> Maybe<T> fromObservable(@NonNull ObservableSource<T> source) {
        Objects.requireNonNull(source, "source is null");
        return RxJavaPlugins.onAssembly(new ObservableElementAtMaybe<>(source, 0L));
    }

    /**
     * Wraps a {@link Publisher} into a {@code Maybe} and emits the very first item
     * or completes if the source is empty.
     * <p>
     * <img width="640" height="309" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.fromPublisher.png" alt="">
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The operator consumes the given {@code Publisher} in an unbounded manner
     *  (requesting {@link Long#MAX_VALUE}) but cancels it after one item received.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code fromPublisher} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the target type
     * @param source the {@code Publisher} to convert from
     * @return the new {@code Maybe} instance
     * @throws NullPointerException if {@code source} is {@code null}
     * @since 3.0.0
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    @BackpressureSupport(BackpressureKind.UNBOUNDED_IN)
    public static <@NonNull T> Maybe<T> fromPublisher(@NonNull Publisher<T> source) {
        Objects.requireNonNull(source, "source is null");
        return RxJavaPlugins.onAssembly(new FlowableElementAtMaybePublisher<>(source, 0L));
    }

    /**
     * Returns a {@code Maybe} instance that runs the given {@link Runnable} for each {@link MaybeObserver} and
     * emits either its unchecked exception or simply completes.
     * <p>
     * <img width="640" height="287" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.fromRunnable.png" alt="">
     * <p>
     * If the code to be wrapped needs to throw a checked or more broader {@link Throwable} exception, that
     * exception has to be converted to an unchecked exception by the wrapped code itself. Alternatively,
     * use the {@link #fromAction(Action)} method which allows the wrapped code to throw any {@code Throwable}
     * exception and will signal it to observers as-is.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code fromRunnable} does not operate by default on a particular {@link Scheduler}.</dd>
     *  <dt><b>Error handling:</b></dt>
     *  <dd> If the {@code Runnable} throws an exception, the respective {@code Throwable} is
     *  delivered to the downstream via {@link MaybeObserver#onError(Throwable)},
     *  except when the downstream has disposed this {@code Maybe} source.
     *  In this latter case, the {@code Throwable} is delivered to the global error handler via
     *  {@link RxJavaPlugins#onError(Throwable)} as an {@link io.reactivex.rxjava3.exceptions.UndeliverableException UndeliverableException}.
     *  </dd>
     * </dl>
     * @param <T> the target type
     * @param run the {@code Runnable} to run for each {@code MaybeObserver}
     * @return the new {@code Maybe} instance
     * @throws NullPointerException if {@code run} is {@code null}
     * @see #fromAction(Action)
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <@NonNull T> Maybe<T> fromRunnable(@NonNull Runnable run) {
        Objects.requireNonNull(run, "run is null");
        return RxJavaPlugins.onAssembly(new MaybeFromRunnable<>(run));
    }

    /**
     * Returns a {@code Maybe} that invokes the given {@link Supplier} for each individual {@link MaybeObserver} that
     * subscribes and emits the resulting non-{@code null} item via {@code onSuccess} while
     * considering a {@code null} result from the {@code Supplier} as indication for valueless completion
     * via {@code onComplete}.
     * <p>
     * This operator allows you to defer the execution of the given {@code Supplier} until a {@code MaybeObserver}
     * subscribes to the  returned {@code Maybe}. In other terms, this source operator evaluates the given
     * {@code Supplier} "lazily".
     * <p>
     * <img width="640" height="311" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.fromSupplier.v3.png" alt="">
     * <p>
     * Note that the {@code null} handling of this operator differs from the similar source operators in the other
     * {@link io.reactivex.rxjava3.core base reactive classes}. Those operators signal a {@link NullPointerException} if the value returned by their
     * {@code Supplier} is {@code null} while this {@code fromSupplier} considers it to indicate the
     * returned {@code Maybe} is empty.
     * <dl>
     *   <dt><b>Scheduler:</b></dt>
     *   <dd>{@code fromSupplier} does not operate by default on a particular {@link Scheduler}.</dd>
     *   <dt><b>Error handling:</b></dt>
     *   <dd>Any non-fatal exception thrown by {@link Supplier#get()} will be forwarded to {@code onError},
     *   except if the {@code MaybeObserver} disposed the subscription in the meantime. In this latter case,
     *   the exception is forwarded to the global error handler via
     *   {@link io.reactivex.rxjava3.plugins.RxJavaPlugins#onError(Throwable)} wrapped into a
     *   {@link io.reactivex.rxjava3.exceptions.UndeliverableException UndeliverableException}.
     *   Fatal exceptions are rethrown and usually will end up in the executing thread's
     *   {@link java.lang.Thread.UncaughtExceptionHandler#uncaughtException(Thread, Throwable)} handler.</dd>
     * </dl>
     *
     * @param supplier
     *         a {@code Supplier} instance whose execution should be deferred and performed for each individual
     *         {@code MaybeObserver} that subscribes to the returned {@code Maybe}.
     * @param <T>
     *         the type of the item emitted by the {@code Maybe}.
     * @return the new {@code Maybe} instance
     * @throws NullPointerException if {@code supplier} is {@code null}
     * @see #defer(Supplier)
     * @see #fromCallable(Callable)
     * @since 3.0.0
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <T> Maybe<@NonNull T> fromSupplier(@NonNull Supplier<? extends @Nullable T> supplier) {
        Objects.requireNonNull(supplier, "supplier is null");
        return RxJavaPlugins.onAssembly(new MaybeFromSupplier<>(supplier));
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
     * @return the new {@code Maybe} instance
     * @throws NullPointerException if {@code item} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/just.html">ReactiveX operators documentation: Just</a>
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <@NonNull T> Maybe<T> just(T item) {
        Objects.requireNonNull(item, "item is null");
        return RxJavaPlugins.onAssembly(new MaybeJust<>(item));
    }

    /**
     * Merges an {@link Iterable} sequence of {@link MaybeSource} instances into a single {@link Flowable} sequence,
     * running all {@code MaybeSource}s at once.
     * <p>
     * <img width="640" height="301" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.merge.i.png" alt="">
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The operator honors backpressure from downstream.</dd>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code merge} does not operate by default on a particular {@link Scheduler}.</dd>
     *  <dt><b>Error handling:</b></dt>
     *  <dd>If any of the source {@code MaybeSource}s signal a {@link Throwable} via {@code onError}, the resulting
     *  {@code Flowable} terminates with that {@code Throwable} and all other source {@code MaybeSource}s are disposed.
     *  If more than one {@code MaybeSource} signals an error, the resulting {@code Flowable} may terminate with the
     *  first one's error or, depending on the concurrency of the sources, may terminate with a
     *  {@link CompositeException} containing two or more of the various error signals.
     *  {@code Throwable}s that didn't make into the composite will be sent (individually) to the global error handler via
     *  {@link RxJavaPlugins#onError(Throwable)} method as {@link UndeliverableException} errors. Similarly, {@code Throwable}s
     *  signaled by source(s) after the returned {@code Flowable} has been cancelled or terminated with a
     *  (composite) error will be sent to the same global error handler.
     *  Use {@link #mergeDelayError(Iterable)} to merge sources and terminate only when all source {@code MaybeSource}s
     *  have completed or failed with an error.
     *  </dd>
     * </dl>
     * @param <T> the common and resulting value type
     * @param sources the {@code Iterable} sequence of {@code MaybeSource} sources
     * @return the new {@code Flowable} instance
     * @throws NullPointerException if {@code sources} is {@code null}
     * @see #mergeDelayError(Iterable)
     */
    @BackpressureSupport(BackpressureKind.FULL)
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public static <@NonNull T> Flowable<T> merge(@NonNull Iterable<@NonNull ? extends MaybeSource<? extends T>> sources) {
        return Flowable.fromIterable(sources).flatMapMaybe(Functions.identity(), false, Integer.MAX_VALUE);
    }

    /**
     * Merges a {@link Publisher} sequence of {@link MaybeSource} instances into a single {@link Flowable} sequence,
     * running all {@code MaybeSource}s at once.
     * <p>
     * <img width="640" height="325" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.merge.p.png" alt="">
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The operator honors backpressure from downstream.</dd>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code merge} does not operate by default on a particular {@link Scheduler}.</dd>
     *  <dt><b>Error handling:</b></dt>
     *  <dd>If any of the source {@code MaybeSource}s signal a {@link Throwable} via {@code onError}, the resulting
     *  {@code Flowable} terminates with that {@code Throwable} and all other source {@code MaybeSource}s are disposed.
     *  If more than one {@code MaybeSource} signals an error, the resulting {@code Flowable} may terminate with the
     *  first one's error or, depending on the concurrency of the sources, may terminate with a
     *  {@link CompositeException} containing two or more of the various error signals.
     *  {@code Throwable}s that didn't make into the composite will be sent (individually) to the global error handler via
     *  {@link RxJavaPlugins#onError(Throwable)} method as {@link UndeliverableException} errors. Similarly, {@code Throwable}s
     *  signaled by source(s) after the returned {@code Flowable} has been cancelled or terminated with a
     *  (composite) error will be sent to the same global error handler.
     *  Use {@link #mergeDelayError(Publisher)} to merge sources and terminate only when all source {@code MaybeSource}s
     *  have completed or failed with an error.
     *  </dd>
     * </dl>
     * @param <T> the common and resulting value type
     * @param sources the {@code Flowable} sequence of {@code MaybeSource} sources
     * @return the new {@code Flowable} instance
     * @throws NullPointerException if {@code sources} is {@code null}
     * @see #mergeDelayError(Publisher)
     */
    @BackpressureSupport(BackpressureKind.FULL)
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public static <@NonNull T> Flowable<T> merge(@NonNull Publisher<@NonNull ? extends MaybeSource<? extends T>> sources) {
        return merge(sources, Integer.MAX_VALUE);
    }

    /**
     * Merges a {@link Publisher} sequence of {@link MaybeSource} instances into a single {@link Flowable} sequence,
     * running at most maxConcurrency {@code MaybeSource}s at once.
     * <p>
     * <img width="640" height="260" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.merge.pn.png" alt="">
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The operator honors backpressure from downstream.</dd>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code merge} does not operate by default on a particular {@link Scheduler}.</dd>
     *  <dt><b>Error handling:</b></dt>
     *  <dd>If any of the source {@code MaybeSource}s signal a {@link Throwable} via {@code onError}, the resulting
     *  {@code Flowable} terminates with that {@code Throwable} and all other source {@code MaybeSource}s are disposed.
     *  If more than one {@code MaybeSource} signals an error, the resulting {@code Flowable} may terminate with the
     *  first one's error or, depending on the concurrency of the sources, may terminate with a
     *  {@link CompositeException} containing two or more of the various error signals.
     *  {@code Throwable}s that didn't make into the composite will be sent (individually) to the global error handler via
     *  {@link RxJavaPlugins#onError(Throwable)} method as {@link UndeliverableException} errors. Similarly, {@code Throwable}s
     *  signaled by source(s) after the returned {@code Flowable} has been cancelled or terminated with a
     *  (composite) error will be sent to the same global error handler.
     *  Use {@link #mergeDelayError(Publisher, int)} to merge sources and terminate only when all source {@code MaybeSource}s
     *  have completed or failed with an error.
     *  </dd>
     * </dl>
     * @param <T> the common and resulting value type
     * @param sources the {@code Flowable} sequence of {@code MaybeSource} sources
     * @param maxConcurrency the maximum number of concurrently running {@code MaybeSource}s
     * @return the new {@code Flowable} instance
     * @throws NullPointerException if {@code sources} is {@code null}
     * @throws IllegalArgumentException if {@code maxConcurrency} is non-positive
     * @see #mergeDelayError(Publisher, int)
     */
    @BackpressureSupport(BackpressureKind.FULL)
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <@NonNull T> Flowable<T> merge(@NonNull Publisher<@NonNull ? extends MaybeSource<? extends T>> sources, int maxConcurrency) {
        Objects.requireNonNull(sources, "sources is null");
        ObjectHelper.verifyPositive(maxConcurrency, "maxConcurrency");
        return RxJavaPlugins.onAssembly(new FlowableFlatMapMaybePublisher<>(sources, Functions.identity(), false, maxConcurrency));
    }

    /**
     * Flattens a {@link MaybeSource} that emits a {@code MaybeSource} into a single {@code MaybeSource} that emits the item
     * emitted by the nested {@code MaybeSource}, without any transformation.
     * <p>
     * <img width="640" height="394" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.merge.oo.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code merge} does not operate by default on a particular {@link Scheduler}.</dd>
     * <dt><b>Error handling:</b></dt>
     * <dd>The resulting {@code Maybe} emits the outer source's or the inner {@code MaybeSource}'s {@link Throwable} as is.
     * Unlike the other {@code merge()} operators, this operator won't and can't produce a {@link CompositeException} because there is
     * only one possibility for the outer or the inner {@code MaybeSource} to emit an {@code onError} signal.
     * Therefore, there is no need for a {@code mergeDelayError(MaybeSource<MaybeSource<T>>)} operator.
     * </dd>
     * </dl>
     *
     * @param <T> the value type of the sources and the output
     * @param source
     *            a {@code MaybeSource} that emits a {@code MaybeSource}
     * @return the new {@code Maybe} instance
     * @throws NullPointerException if {@code source} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/merge.html">ReactiveX operators documentation: Merge</a>
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static <@NonNull T> Maybe<T> merge(@NonNull MaybeSource<? extends MaybeSource<? extends T>> source) {
        Objects.requireNonNull(source, "source is null");
        return RxJavaPlugins.onAssembly(new MaybeFlatten(source, Functions.identity()));
    }

    /**
     * Flattens two {@link MaybeSource}s into a single {@link Flowable}, without any transformation.
     * <p>
     * <img width="640" height="279" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.merge.2.png" alt="">
     * <p>
     * You can combine items emitted by multiple {@code MaybeSource}s so that they appear as a single {@code Flowable}, by
     * using the {@code merge} method.
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The operator honors backpressure from downstream.</dd>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code merge} does not operate by default on a particular {@link Scheduler}.</dd>
     *  <dt><b>Error handling:</b></dt>
     *  <dd>If any of the source {@code MaybeSource}s signal a {@link Throwable} via {@code onError}, the resulting
     *  {@code Flowable} terminates with that {@code Throwable} and all other source {@code MaybeSource}s are disposed.
     *  If more than one {@code MaybeSource} signals an error, the resulting {@code Flowable} may terminate with the
     *  first one's error or, depending on the concurrency of the sources, may terminate with a
     *  {@link CompositeException} containing two or more of the various error signals.
     *  {@code Throwable}s that didn't make into the composite will be sent (individually) to the global error handler via
     *  {@link RxJavaPlugins#onError(Throwable)} method as {@link UndeliverableException} errors. Similarly, {@code Throwable}s
     *  signaled by source(s) after the returned {@code Flowable} has been cancelled or terminated with a
     *  (composite) error will be sent to the same global error handler.
     *  Use {@link #mergeDelayError(MaybeSource, MaybeSource)} to merge sources and terminate only when all source {@code MaybeSource}s
     *  have completed or failed with an error.
     *  </dd>
     * </dl>
     *
     * @param <T> the common value type
     * @param source1
     *            a {@code MaybeSource} to be merged
     * @param source2
     *            a {@code MaybeSource} to be merged
     * @return the new {@code Flowable} instance
     * @throws NullPointerException if {@code source1} or {@code source2} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/merge.html">ReactiveX operators documentation: Merge</a>
     * @see #mergeDelayError(MaybeSource, MaybeSource)
     */
    @BackpressureSupport(BackpressureKind.FULL)
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <@NonNull T> Flowable<T> merge(
            @NonNull MaybeSource<? extends T> source1, @NonNull MaybeSource<? extends T> source2
     ) {
        Objects.requireNonNull(source1, "source1 is null");
        Objects.requireNonNull(source2, "source2 is null");
        return mergeArray(source1, source2);
    }

    /**
     * Flattens three {@link MaybeSource}s into a single {@link Flowable}, without any transformation.
     * <p>
     * <img width="640" height="301" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.merge.3.png" alt="">
     * <p>
     * You can combine items emitted by multiple {@code MaybeSource}s so that they appear as a single {@code Flowable}, by using
     * the {@code merge} method.
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The operator honors backpressure from downstream.</dd>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code merge} does not operate by default on a particular {@link Scheduler}.</dd>
     *  <dt><b>Error handling:</b></dt>
     *  <dd>If any of the source {@code MaybeSource}s signal a {@link Throwable} via {@code onError}, the resulting
     *  {@code Flowable} terminates with that {@code Throwable} and all other source {@code MaybeSource}s are disposed.
     *  If more than one {@code MaybeSource} signals an error, the resulting {@code Flowable} may terminate with the
     *  first one's error or, depending on the concurrency of the sources, may terminate with a
     *  {@link CompositeException} containing two or more of the various error signals.
     *  {@code Throwable}s that didn't make into the composite will be sent (individually) to the global error handler via
     *  {@link RxJavaPlugins#onError(Throwable)} method as {@link UndeliverableException} errors. Similarly, {@code Throwable}s
     *  signaled by source(s) after the returned {@code Flowable} has been cancelled or terminated with a
     *  (composite) error will be sent to the same global error handler.
     *  Use {@link #mergeDelayError(MaybeSource, MaybeSource, MaybeSource)} to merge sources and terminate only when all source {@code MaybeSource}s
     *  have completed or failed with an error.
     *  </dd>
     * </dl>
     *
     * @param <T> the common value type
     * @param source1
     *            a {@code MaybeSource} to be merged
     * @param source2
     *            a {@code MaybeSource} to be merged
     * @param source3
     *            a {@code MaybeSource} to be merged
     * @return the new {@code Flowable} instance
     * @throws NullPointerException if {@code source1}, {@code source2} or {@code source3} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/merge.html">ReactiveX operators documentation: Merge</a>
     * @see #mergeDelayError(MaybeSource, MaybeSource, MaybeSource)
     */
    @BackpressureSupport(BackpressureKind.FULL)
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <@NonNull T> Flowable<T> merge(
            @NonNull MaybeSource<? extends T> source1, @NonNull MaybeSource<? extends T> source2,
            @NonNull MaybeSource<? extends T> source3
     ) {
        Objects.requireNonNull(source1, "source1 is null");
        Objects.requireNonNull(source2, "source2 is null");
        Objects.requireNonNull(source3, "source3 is null");
        return mergeArray(source1, source2, source3);
    }

    /**
     * Flattens four {@link MaybeSource}s into a single {@link Flowable}, without any transformation.
     * <p>
     * <img width="640" height="289" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.merge.4.png" alt="">
     * <p>
     * You can combine items emitted by multiple {@code MaybeSource}s so that they appear as a single {@code Flowable}, by using
     * the {@code merge} method.
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The operator honors backpressure from downstream.</dd>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code merge} does not operate by default on a particular {@link Scheduler}.</dd>
     *  <dt><b>Error handling:</b></dt>
     *  <dd>If any of the source {@code MaybeSource}s signal a {@link Throwable} via {@code onError}, the resulting
     *  {@code Flowable} terminates with that {@code Throwable} and all other source {@code MaybeSource}s are disposed.
     *  If more than one {@code MaybeSource} signals an error, the resulting {@code Flowable} may terminate with the
     *  first one's error or, depending on the concurrency of the sources, may terminate with a
     *  {@link CompositeException} containing two or more of the various error signals.
     *  {@code Throwable}s that didn't make into the composite will be sent (individually) to the global error handler via
     *  {@link RxJavaPlugins#onError(Throwable)} method as {@link UndeliverableException} errors. Similarly, {@code Throwable}s
     *  signaled by source(s) after the returned {@code Flowable} has been cancelled or terminated with a
     *  (composite) error will be sent to the same global error handler.
     *  Use {@link #mergeDelayError(MaybeSource, MaybeSource, MaybeSource, MaybeSource)} to merge sources and terminate only when all source {@code MaybeSource}s
     *  have completed or failed with an error.
     *  </dd>
     * </dl>
     *
     * @param <T> the common value type
     * @param source1
     *            a {@code MaybeSource} to be merged
     * @param source2
     *            a {@code MaybeSource} to be merged
     * @param source3
     *            a {@code MaybeSource} to be merged
     * @param source4
     *            a {@code MaybeSource} to be merged
     * @return the new {@code Flowable} instance
     * @throws NullPointerException if {@code source1}, {@code source2}, {@code source3} or {@code source4} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/merge.html">ReactiveX operators documentation: Merge</a>
     * @see #mergeDelayError(MaybeSource, MaybeSource, MaybeSource, MaybeSource)
     */
    @BackpressureSupport(BackpressureKind.FULL)
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <@NonNull T> Flowable<T> merge(
            @NonNull MaybeSource<? extends T> source1, @NonNull MaybeSource<? extends T> source2,
            @NonNull MaybeSource<? extends T> source3, @NonNull MaybeSource<? extends T> source4
     ) {
        Objects.requireNonNull(source1, "source1 is null");
        Objects.requireNonNull(source2, "source2 is null");
        Objects.requireNonNull(source3, "source3 is null");
        Objects.requireNonNull(source4, "source4 is null");
        return mergeArray(source1, source2, source3, source4);
    }

    /**
     * Merges an array of {@link MaybeSource} instances into a single {@link Flowable} sequence,
     * running all {@code MaybeSource}s at once.
     * <p>
     * <img width="640" height="272" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.mergeArray.png" alt="">
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The operator honors backpressure from downstream.</dd>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code mergeArray} does not operate by default on a particular {@link Scheduler}.</dd>
     *  <dt><b>Error handling:</b></dt>
     *  <dd>If any of the source {@code MaybeSource}s signal a {@link Throwable} via {@code onError}, the resulting
     *  {@code Flowable} terminates with that {@code Throwable} and all other source {@code MaybeSource}s are disposed.
     *  If more than one {@code MaybeSource} signals an error, the resulting {@code Flowable} may terminate with the
     *  first one's error or, depending on the concurrency of the sources, may terminate with a
     *  {@link CompositeException} containing two or more of the various error signals.
     *  {@code Throwable}s that didn't make into the composite will be sent (individually) to the global error handler via
     *  {@link RxJavaPlugins#onError(Throwable)} method as {@link UndeliverableException} errors. Similarly, {@code Throwable}s
     *  signaled by source(s) after the returned {@code Flowable} has been cancelled or terminated with a
     *  (composite) error will be sent to the same global error handler.
     *  Use {@link #mergeArrayDelayError(MaybeSource...)} to merge sources and terminate only when all source {@code MaybeSource}s
     *  have completed or failed with an error.
     *  </dd>
     * </dl>
     * @param <T> the common and resulting value type
     * @param sources the array sequence of {@code MaybeSource} sources
     * @return the new {@code Flowable} instance
     * @throws NullPointerException if {@code sources} is {@code null}
     * @see #mergeArrayDelayError(MaybeSource...)
     */
    @BackpressureSupport(BackpressureKind.FULL)
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    @SafeVarargs
    public static <@NonNull T> Flowable<T> mergeArray(MaybeSource<? extends T>... sources) {
        Objects.requireNonNull(sources, "sources is null");
        if (sources.length == 0) {
            return Flowable.empty();
        }
        if (sources.length == 1) {
            @SuppressWarnings("unchecked")
            MaybeSource<T> source = (MaybeSource<T>)sources[0];
            return RxJavaPlugins.onAssembly(new MaybeToFlowable<>(source));
        }
        return RxJavaPlugins.onAssembly(new MaybeMergeArray<>(sources));
    }

    /**
     * Flattens an array of {@link MaybeSource}s into one {@link Flowable}, in a way that allows a subscriber to receive all
     * successfully emitted items from each of the source {@code MaybeSource}s without being interrupted by an error
     * notification from one of them.
     * <p>
     * <img width="640" height="422" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.mergeArrayDelayError.png" alt="">
     * <p>
     * This behaves like {@link #merge(Publisher)} except that if any of the merged {@code MaybeSource}s notify of an
     * error via {@link Subscriber#onError onError}, {@code mergeArrayDelayError} will refrain from propagating that
     * error notification until all of the merged {@code MaybeSource}s have finished emitting items.
     * <p>
     * Even if multiple merged {@code MaybeSource}s send {@code onError} notifications, {@code mergeArrayDelayError} will only
     * invoke the {@code onError} method of its subscribers once.
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The operator honors backpressure from downstream.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code mergeArrayDelayError} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T> the common element base type
     * @param sources
     *            the array of {@code MaybeSource}s
     * @return the new {@code Flowable} instance
     * @throws NullPointerException if {@code sources} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/merge.html">ReactiveX operators documentation: Merge</a>
     */
    @BackpressureSupport(BackpressureKind.FULL)
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @SafeVarargs
    @NonNull
    public static <@NonNull T> Flowable<T> mergeArrayDelayError(@NonNull MaybeSource<? extends T>... sources) {
        Objects.requireNonNull(sources, "sources is null");
        return Flowable.fromArray(sources).flatMapMaybe(Functions.identity(), true, Math.max(1, sources.length));
    }

    /**
     * Flattens an {@link Iterable} sequence of {@link MaybeSource}s into one {@link Flowable}, in a way that allows a subscriber to receive all
     * successfully emitted items from each of the source {@code MaybeSource}s without being interrupted by an error
     * notification from one of them.
     * <p>
     * <img width="640" height="467" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.mergeDelayError.i.png" alt="">
     * <p>
     * This behaves like {@link #merge(Publisher)} except that if any of the merged {@code MaybeSource}s notify of an
     * error via {@link Subscriber#onError onError}, {@code mergeDelayError} will refrain from propagating that
     * error notification until all of the merged {@code MaybeSource}s have finished emitting items.
     * <p>
     * <img width="640" height="467" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.mergeDelayError.i.png" alt="">
     * <p>
     * Even if multiple merged {@code MaybeSource}s send {@code onError} notifications, {@code mergeDelayError} will only
     * invoke the {@code onError} method of its subscribers once.
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The operator honors backpressure from downstream.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code mergeDelayError} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T> the common element base type
     * @param sources
     *            the {@code Iterable} of {@code MaybeSource}s
     * @return the new {@code Flowable} instance
     * @throws NullPointerException if {@code sources} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/merge.html">ReactiveX operators documentation: Merge</a>
     */
    @BackpressureSupport(BackpressureKind.FULL)
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public static <@NonNull T> Flowable<T> mergeDelayError(@NonNull Iterable<@NonNull ? extends MaybeSource<? extends T>> sources) {
        return Flowable.fromIterable(sources).flatMapMaybe(Functions.identity(), true, Integer.MAX_VALUE);
    }

    /**
     * Flattens a {@link Publisher} that emits {@link MaybeSource}s into one {@link Flowable}, in a way that allows a subscriber to
     * receive all successfully emitted items from all of the source {@code MaybeSource}s without being interrupted by
     * an error notification from one of them or even the main {@code Publisher}.
     * <p>
     * <img width="640" height="456" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.mergeDelayError.p.png" alt="">
     * <p>
     * This behaves like {@link #merge(Publisher)} except that if any of the merged {@code MaybeSource}s notify of an
     * error via {@link Subscriber#onError onError}, {@code mergeDelayError} will refrain from propagating that
     * error notification until all of the merged {@code MaybeSource}s and the main {@code Publisher} have finished emitting items.
     * <p>
     * Even if multiple merged {@code MaybeSource}s send {@code onError} notifications, {@code mergeDelayError} will only
     * invoke the {@code onError} method of its subscribers once.
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
     *            a {@code Publisher} that emits {@code MaybeSource}s
     * @return the new {@code Flowable} instance
     * @throws NullPointerException if {@code sources} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/merge.html">ReactiveX operators documentation: Merge</a>
     */
    @BackpressureSupport(BackpressureKind.FULL)
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public static <@NonNull T> Flowable<T> mergeDelayError(@NonNull Publisher<@NonNull ? extends MaybeSource<? extends T>> sources) {
        return mergeDelayError(sources, Integer.MAX_VALUE);
    }

    /**
     * Flattens a {@link Publisher} that emits {@link MaybeSource}s into one {@link Flowable}, in a way that allows a subscriber to
     * receive all successfully emitted items from all of the source {@code MaybeSource}s without being interrupted by
     * an error notification from one of them or even the main {@code Publisher} as well as limiting the total number of active {@code MaybeSource}s.
     * <p>
     * <img width="640" height="429" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.mergeDelayError.pn.png" alt="">
     * <p>
     * This behaves like {@link #merge(Publisher, int)} except that if any of the merged {@code MaybeSource}s notify of an
     * error via {@link Subscriber#onError onError}, {@code mergeDelayError} will refrain from propagating that
     * error notification until all of the merged {@code MaybeSource}s and the main {@code Publisher} have finished emitting items.
     * <p>
     * Even if multiple merged {@code MaybeSource}s send {@code onError} notifications, {@code mergeDelayError} will only
     * invoke the {@code onError} method of its subscribers once.
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The operator honors backpressure from downstream. The outer {@code Publisher} is consumed
     *  in unbounded mode (i.e., no backpressure is applied to it).</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code mergeDelayError} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * <p>History: 2.1.9 - experimental
     * @param <T> the common element base type
     * @param sources
     *            a {@code Publisher} that emits {@code MaybeSource}s
     * @param maxConcurrency the maximum number of active inner {@code MaybeSource}s to be merged at a time
     * @return the new {@code Flowable} instance
     * @throws NullPointerException if {@code sources} is {@code null}
     * @throws IllegalArgumentException if {@code maxConcurrency} is non-positive
     * @see <a href="http://reactivex.io/documentation/operators/merge.html">ReactiveX operators documentation: Merge</a>
     * @since 2.2
     */
    @BackpressureSupport(BackpressureKind.FULL)
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <@NonNull T> Flowable<T> mergeDelayError(@NonNull Publisher<@NonNull ? extends MaybeSource<? extends T>> sources, int maxConcurrency) {
        Objects.requireNonNull(sources, "sources is null");
        ObjectHelper.verifyPositive(maxConcurrency, "maxConcurrency");
        return RxJavaPlugins.onAssembly(new FlowableFlatMapMaybePublisher<>(sources, Functions.identity(), true, maxConcurrency));
    }

    /**
     * Flattens two {@link MaybeSource}s into one {@link Flowable}, in a way that allows a subscriber to receive all
     * successfully emitted items from each of the source {@code MaybeSource}s without being interrupted by an error
     * notification from one of them.
     * <p>
     * <img width="640" height="414" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.mergeDelayError.2.png" alt="">
     * <p>
     * This behaves like {@link #merge(MaybeSource, MaybeSource)} except that if any of the merged {@code MaybeSource}s
     * notify of an error via {@link Subscriber#onError onError}, {@code mergeDelayError} will refrain from
     * propagating that error notification until all of the merged {@code MaybeSource}s have finished emitting items.
     * <p>
     * Even if both merged {@code MaybeSource}s send {@code onError} notifications, {@code mergeDelayError} will only
     * invoke the {@code onError} method of its subscribers once.
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The operator honors backpressure from downstream.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code mergeDelayError} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T> the common element base type
     * @param source1
     *            a {@code MaybeSource} to be merged
     * @param source2
     *            a {@code MaybeSource} to be merged
     * @return the new {@code Flowable} instance
     * @throws NullPointerException if {@code source1} or {@code source2} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/merge.html">ReactiveX operators documentation: Merge</a>
     */
    @BackpressureSupport(BackpressureKind.FULL)
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <@NonNull T> Flowable<T> mergeDelayError(@NonNull MaybeSource<? extends T> source1, @NonNull MaybeSource<? extends T> source2) {
        Objects.requireNonNull(source1, "source1 is null");
        Objects.requireNonNull(source2, "source2 is null");
        return mergeArrayDelayError(source1, source2);
    }

    /**
     * Flattens three {@link MaybeSource} into one {@link Flowable}, in a way that allows a subscriber to receive all
     * successfully emitted items from all of the source {@code MaybeSource}s without being interrupted by an error
     * notification from one of them.
     * <p>
     * <img width="640" height="467" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.mergeDelayError.3.png" alt="">
     * <p>
     * This behaves like {@link #merge(MaybeSource, MaybeSource, MaybeSource)} except that if any of the merged
     * {@code MaybeSource}s notify of an error via {@link Subscriber#onError onError}, {@code mergeDelayError} will refrain
     * from propagating that error notification until all of the merged {@code MaybeSource}s have finished emitting
     * items.
     * <p>
     * Even if multiple merged {@code MaybeSource}s send {@code onError} notifications, {@code mergeDelayError} will only
     * invoke the {@code onError} method of its subscribers once.
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The operator honors backpressure from downstream.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code mergeDelayError} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T> the common element base type
     * @param source1
     *            a {@code MaybeSource} to be merged
     * @param source2
     *            a {@code MaybeSource} to be merged
     * @param source3
     *            a {@code MaybeSource} to be merged
     * @return the new {@code Flowable} instance
     * @throws NullPointerException if {@code source1}, {@code source2} or {@code source3} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/merge.html">ReactiveX operators documentation: Merge</a>
     */
    @BackpressureSupport(BackpressureKind.FULL)
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <@NonNull T> Flowable<T> mergeDelayError(@NonNull MaybeSource<? extends T> source1,
            @NonNull MaybeSource<? extends T> source2, @NonNull MaybeSource<? extends T> source3) {
        Objects.requireNonNull(source1, "source1 is null");
        Objects.requireNonNull(source2, "source2 is null");
        Objects.requireNonNull(source3, "source3 is null");
        return mergeArrayDelayError(source1, source2, source3);
    }

    /**
     * Flattens four {@link MaybeSource}s into one {@link Flowable}, in a way that allows a subscriber to receive all
     * successfully emitted items from all of the source {@code MaybeSource}s without being interrupted by an error
     * notification from one of them.
     * <p>
     * <img width="640" height="461" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.mergeDelayError.4.png" alt="">
     * <p>
     * This behaves like {@link #merge(MaybeSource, MaybeSource, MaybeSource, MaybeSource)} except that if any of
     * the merged {@code MaybeSource}s notify of an error via {@link Subscriber#onError onError}, {@code mergeDelayError}
     * will refrain from propagating that error notification until all of the merged {@code MaybeSource}s have finished
     * emitting items.
     * <p>
     * Even if multiple merged {@code MaybeSource}s send {@code onError} notifications, {@code mergeDelayError} will only
     * invoke the {@code onError} method of its subscribers once.
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The operator honors backpressure from downstream.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code mergeDelayError} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T> the common element base type
     * @param source1
     *            a {@code MaybeSource} to be merged
     * @param source2
     *            a {@code MaybeSource} to be merged
     * @param source3
     *            a {@code MaybeSource} to be merged
     * @param source4
     *            a {@code MaybeSource} to be merged
     * @return the new {@code Flowable} instance
     * @throws NullPointerException if {@code source1}, {@code source2}, {@code source3} or {@code source4} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/merge.html">ReactiveX operators documentation: Merge</a>
     */
    @BackpressureSupport(BackpressureKind.FULL)
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <@NonNull T> Flowable<T> mergeDelayError(
            @NonNull MaybeSource<? extends T> source1, @NonNull MaybeSource<? extends T> source2,
            @NonNull MaybeSource<? extends T> source3, @NonNull MaybeSource<? extends T> source4) {
        Objects.requireNonNull(source1, "source1 is null");
        Objects.requireNonNull(source2, "source2 is null");
        Objects.requireNonNull(source3, "source3 is null");
        Objects.requireNonNull(source4, "source4 is null");
        return mergeArrayDelayError(source1, source2, source3, source4);
    }

    /**
     * Returns a {@code Maybe} that never sends any items or notifications to a {@link MaybeObserver}.
     * <p>
     * <img width="640" height="185" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/never.v3.png" alt="">
     * <p>
     * This {@code Maybe} is useful primarily for testing purposes.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code never} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T>
     *            the type of items (not) emitted by the {@code Maybe}
     * @return the shared {@code Maybe} instance
     * @see <a href="http://reactivex.io/documentation/operators/empty-never-throw.html">ReactiveX operators documentation: Never</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @SuppressWarnings("unchecked")
    @NonNull
    public static <@NonNull T> Maybe<T> never() {
        return RxJavaPlugins.onAssembly((Maybe<T>)MaybeNever.INSTANCE);
    }

    /**
     * Returns a {@link Single} that emits a {@link Boolean} value that indicates whether two {@link MaybeSource} sequences are the
     * same by comparing the items emitted by each {@code MaybeSource} pairwise.
     * <p>
     * <img width="640" height="187" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.sequenceEqual.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code sequenceEqual} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param source1
     *            the first {@code MaybeSource} to compare
     * @param source2
     *            the second {@code MaybeSource} to compare
     * @param <T>
     *            the type of items emitted by each {@code MaybeSource}
     * @return the new {@code Single} instance
     * @throws NullPointerException if {@code source1} or {@code source2} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/sequenceequal.html">ReactiveX operators documentation: SequenceEqual</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public static <@NonNull T> Single<Boolean> sequenceEqual(@NonNull MaybeSource<? extends T> source1, @NonNull MaybeSource<? extends T> source2) {
        return sequenceEqual(source1, source2, ObjectHelper.equalsPredicate());
    }

    /**
     * Returns a {@link Single} that emits a {@link Boolean} value that indicates whether two {@link MaybeSource}s are the
     * same by comparing the items emitted by each {@code MaybeSource} pairwise based on the results of a specified
     * equality function.
     * <p>
     * <img width="640" height="247" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.sequenceEqual.f.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code sequenceEqual} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param source1
     *            the first {@code MaybeSource} to compare
     * @param source2
     *            the second {@code MaybeSource} to compare
     * @param isEqual
     *            a function used to compare items emitted by each {@code MaybeSource}
     * @param <T>
     *            the type of items emitted by each {@code MaybeSource}
     * @return the new {@code Single} instance
     * @throws NullPointerException if {@code source1}, {@code source2} or {@code isEqual} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/sequenceequal.html">ReactiveX operators documentation: SequenceEqual</a>
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <@NonNull T> Single<Boolean> sequenceEqual(@NonNull MaybeSource<? extends T> source1, @NonNull MaybeSource<? extends T> source2,
            @NonNull BiPredicate<? super T, ? super T> isEqual) {
        Objects.requireNonNull(source1, "source1 is null");
        Objects.requireNonNull(source2, "source2 is null");
        Objects.requireNonNull(isEqual, "isEqual is null");
        return RxJavaPlugins.onAssembly(new MaybeEqualSingle<>(source1, source2, isEqual));
    }

    /**
     * Switches between {@link MaybeSource}s emitted by the source {@link Publisher} whenever
     * a new {@code MaybeSource} is emitted, disposing the previously running {@code MaybeSource},
     * exposing the success items as a {@link Flowable} sequence.
     * <p>
     * <img width="640" height="521" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.switchOnNext.png" alt="">
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The {@code sources} {@code Publisher} is consumed in an unbounded manner (requesting {@link Long#MAX_VALUE}).
     *  The returned {@code Flowable} respects the backpressure from the downstream.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code switchOnNext} does not operate by default on a particular {@link Scheduler}.</dd>
     *  <dt><b>Error handling:</b></dt>
     *  <dd>The returned sequence fails with the first error signaled by the {@code sources} {@code Publisher}
     *  or the currently running {@code MaybeSource}, disposing the rest. Late errors are
     *  forwarded to the global error handler via {@link RxJavaPlugins#onError(Throwable)}.</dd>
     * </dl>
     * @param <T> the element type of the {@code MaybeSource}s
     * @param sources the {@code Publisher} sequence of inner {@code MaybeSource}s to switch between
     * @return the new {@code Flowable} instance
     * @throws NullPointerException if {@code sources} is {@code null}
     * @since 3.0.0
     * @see #switchOnNextDelayError(Publisher)
     * @see <a href="http://reactivex.io/documentation/operators/switch.html">ReactiveX operators documentation: Switch</a>
     */
    @BackpressureSupport(BackpressureKind.UNBOUNDED_IN)
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <@NonNull T> Flowable<T> switchOnNext(@NonNull Publisher<@NonNull ? extends MaybeSource<? extends T>> sources) {
        Objects.requireNonNull(sources, "sources is null");
        return RxJavaPlugins.onAssembly(new FlowableSwitchMapMaybePublisher<>(sources, Functions.identity(), false));
    }

    /**
     * Switches between {@link MaybeSource}s emitted by the source {@link Publisher} whenever
     * a new {@code MaybeSource} is emitted, disposing the previously running {@code MaybeSource},
     * exposing the success items as a {@link Flowable} sequence and delaying all errors from
     * all of them until all terminate.
     * <p>
     * <img width="640" height="423" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.switchOnNextDelayError.png" alt="">
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The {@code sources} {@code Publisher} is consumed in an unbounded manner (requesting {@link Long#MAX_VALUE}).
     *  The returned {@code Flowable} respects the backpressure from the downstream.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code switchOnNextDelayError} does not operate by default on a particular {@link Scheduler}.</dd>
     *  <dt><b>Error handling:</b></dt>
     *  <dd>The returned {@code Flowable} collects all errors emitted by either the {@code sources}
     *  {@code Publisher} or any inner {@code MaybeSource} and emits them as a {@link CompositeException}
     *  when all sources terminate. If only one source ever failed, its error is emitted as-is at the end.</dd>
     * </dl>
     * @param <T> the element type of the {@code MaybeSource}s
     * @param sources the {@code Publisher} sequence of inner {@code MaybeSource}s to switch between
     * @return the new {@code Flowable} instance
     * @throws NullPointerException if {@code sources} is {@code null}
     * @since 3.0.0
     * @see #switchOnNext(Publisher)
     * @see <a href="http://reactivex.io/documentation/operators/switch.html">ReactiveX operators documentation: Switch</a>
     */
    @BackpressureSupport(BackpressureKind.FULL)
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <@NonNull T> Flowable<T> switchOnNextDelayError(@NonNull Publisher<@NonNull ? extends MaybeSource<? extends T>> sources) {
        Objects.requireNonNull(sources, "sources is null");
        return RxJavaPlugins.onAssembly(new FlowableSwitchMapMaybePublisher<>(sources, Functions.identity(), true));
    }

    /**
     * Returns a {@code Maybe} that emits {@code 0L} after a specified delay.
     * <p>
     * <img width="640" height="391" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.timer.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code timer} operates by default on the {@code computation} {@link Scheduler}.</dd>
     * </dl>
     *
     * @param delay
     *            the initial delay before emitting a single {@code 0L}
     * @param unit
     *            time units to use for {@code delay}
     * @return the new {@code Maybe} instance
     * @throws NullPointerException if {@code unit} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/timer.html">ReactiveX operators documentation: Timer</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.COMPUTATION)
    @NonNull
    public static Maybe<Long> timer(long delay, @NonNull TimeUnit unit) {
        return timer(delay, unit, Schedulers.computation());
    }

    /**
     * Returns a {@code Maybe} that emits {@code 0L} after a specified delay on a specified {@link Scheduler}.
     * <p>
     * <img width="640" height="392" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.timer.s.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>You specify which {@code Scheduler} this operator will use.</dd>
     * </dl>
     *
     * @param delay
     *            the initial delay before emitting a single 0L
     * @param unit
     *            time units to use for {@code delay}
     * @param scheduler
     *            the {@code Scheduler} to use for scheduling the item
     * @return the new {@code Maybe} instance
     * @throws NullPointerException if {@code unit} or {@code scheduler} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/timer.html">ReactiveX operators documentation: Timer</a>
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    public static Maybe<Long> timer(long delay, @NonNull TimeUnit unit, @NonNull Scheduler scheduler) {
        Objects.requireNonNull(unit, "unit is null");
        Objects.requireNonNull(scheduler, "scheduler is null");

        return RxJavaPlugins.onAssembly(new MaybeTimer(Math.max(0L, delay), unit, scheduler));
    }

    /**
     * <strong>Advanced use only:</strong> creates a {@code Maybe} instance without
     * any safeguards by using a callback that is called with a {@link MaybeObserver}.
     * <p>
     * <img width="640" height="262" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.unsafeCreate.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code unsafeCreate} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the value type
     * @param onSubscribe the function that is called with the subscribing {@code MaybeObserver}
     * @return the new {@code Maybe} instance
     * @throws IllegalArgumentException if {@code onSubscribe} is a {@code Maybe}
     * @throws NullPointerException if {@code onSubscribe} is {@code null}
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <@NonNull T> Maybe<T> unsafeCreate(@NonNull MaybeSource<T> onSubscribe) {
        if (onSubscribe instanceof Maybe) {
            throw new IllegalArgumentException("unsafeCreate(Maybe) should be upgraded");
        }
        Objects.requireNonNull(onSubscribe, "onSubscribe is null");
        return RxJavaPlugins.onAssembly(new MaybeUnsafeCreate<>(onSubscribe));
    }

    /**
     * Constructs a {@code Maybe} that creates a dependent resource object which is disposed of when the
     * generated {@link MaybeSource} terminates or the downstream calls dispose().
     * <p>
     * <img width="640" height="378" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.using.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code using} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T> the element type of the generated {@code MaybeSource}
     * @param <D> the type of the resource associated with the output sequence
     * @param resourceSupplier
     *            the factory function to create a resource object that depends on the {@code Maybe}
     * @param sourceSupplier
     *            the factory function to create a {@code MaybeSource}
     * @param resourceCleanup
     *            the function that will dispose of the resource
     * @return the new {@code Maybe} instance
     * @throws NullPointerException if {@code resourceSupplier}, {@code sourceSupplier} or {@code resourceCleanup} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/using.html">ReactiveX operators documentation: Using</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public static <@NonNull T, @NonNull D> Maybe<T> using(@NonNull Supplier<? extends D> resourceSupplier,
            @NonNull Function<? super D, ? extends MaybeSource<? extends T>> sourceSupplier,
            @NonNull Consumer<? super D> resourceCleanup) {
        return using(resourceSupplier, sourceSupplier, resourceCleanup, true);
    }

    /**
     * Constructs a {@code Maybe} that creates a dependent resource object which is disposed first ({code eager == true})
     * when the generated {@link MaybeSource} terminates or the downstream disposes; or after ({code eager == false}).
     * <p>
     * <img width="640" height="323" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.using.b.png" alt="">
     * <p>
     * Eager disposal is particularly appropriate for a synchronous {@code Maybe} that reuses resources. {@code disposeAction} will
     * only be called once per subscription.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code using} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T> the element type of the generated {@code MaybeSource}
     * @param <D> the type of the resource associated with the output sequence
     * @param resourceSupplier
     *            the factory function to create a resource object that depends on the {@code Maybe}
     * @param sourceSupplier
     *            the factory function to create a {@code MaybeSource}
     * @param resourceCleanup
     *            the function that will dispose of the resource
     * @param eager
     *            If {@code true} then resource disposal will happen either on a {@code dispose()} call before the upstream is disposed
     *            or just before the emission of a terminal event ({@code onSuccess}, {@code onComplete} or {@code onError}).
     *            If {@code false} the resource disposal will happen either on a {@code dispose()} call after the upstream is disposed
     *            or just after the emission of a terminal event ({@code onSuccess}, {@code onComplete} or {@code onError}).
     * @return the new {@code Maybe} instance
     * @throws NullPointerException if {@code resourceSupplier}, {@code sourceSupplier} or {@code resourceCleanup} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/using.html">ReactiveX operators documentation: Using</a>
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <@NonNull T, @NonNull D> Maybe<T> using(@NonNull Supplier<? extends D> resourceSupplier,
            @NonNull Function<? super D, ? extends MaybeSource<? extends T>> sourceSupplier,
            @NonNull Consumer<? super D> resourceCleanup, boolean eager) {
        Objects.requireNonNull(resourceSupplier, "resourceSupplier is null");
        Objects.requireNonNull(sourceSupplier, "sourceSupplier is null");
        Objects.requireNonNull(resourceCleanup, "resourceCleanup is null");
        return RxJavaPlugins.onAssembly(new MaybeUsing<T, D>(resourceSupplier, sourceSupplier, resourceCleanup, eager));
    }

    /**
     * Wraps a {@link MaybeSource} instance into a new {@code Maybe} instance if not already a {@code Maybe}
     * instance.
     * <p>
     * <img width="640" height="232" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.wrap.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code wrap} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the value type
     * @param source the source to wrap
     * @return the new wrapped or cast {@code Maybe} instance
     * @throws NullPointerException if {@code source} is {@code null}
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <@NonNull T> Maybe<T> wrap(@NonNull MaybeSource<T> source) {
        if (source instanceof Maybe) {
            return RxJavaPlugins.onAssembly((Maybe<T>)source);
        }
        Objects.requireNonNull(source, "source is null");
        return RxJavaPlugins.onAssembly(new MaybeUnsafeCreate<>(source));
    }

    /**
     * Returns a {@code Maybe} that emits the results of a specified combiner function applied to combinations of
     * items emitted, in sequence, by an {@link Iterable} of other {@link MaybeSource}s.
     * <p>
     * <img width="640" height="341" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.zip.i.png" alt="">
     * <p>
     * Note on method signature: since Java doesn't allow creating a generic array with {@code new T[]}, the
     * implementation of this operator has to create an {@code Object[]} instead. Unfortunately, a
     * {@code Function<Integer[], R>} passed to the method would trigger a {@link ClassCastException}.
     * <p>
     * This operator terminates eagerly if any of the source {@code MaybeSource}s signal an {@code onError} or {@code onComplete}. This
     * also means it is possible some sources may not get subscribed to at all.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code zip} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T> the common value type
     * @param <R> the zipped result type
     * @param sources
     *            an {@code Iterable} of source {@code MaybeSource}s
     * @param zipper
     *            a function that, when applied to an item emitted by each of the source {@code MaybeSource}s, results in
     *            an item that will be emitted by the resulting {@code Maybe}
     * @return the new {@code Maybe} instance
     * @throws NullPointerException if {@code zipper} or {@code sources} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/zip.html">ReactiveX operators documentation: Zip</a>
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <@NonNull T, @NonNull R> Maybe<R> zip(@NonNull Iterable<@NonNull ? extends MaybeSource<? extends T>> sources, @NonNull Function<? super Object[], ? extends R> zipper) {
        Objects.requireNonNull(zipper, "zipper is null");
        Objects.requireNonNull(sources, "sources is null");
        return RxJavaPlugins.onAssembly(new MaybeZipIterable<>(sources, zipper));
    }

    /**
     * Returns a {@code Maybe} that emits the results of a specified combiner function applied to combinations of
     * two items emitted, in sequence, by two other {@link MaybeSource}s.
     * <p>
     * <img width="640" height="434" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.zip.n.png" alt="">
     * <p>
     * This operator terminates eagerly if any of the source {@code MaybeSource}s signal an {@code onError} or {@code onComplete}. This
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
     *            the first source {@code MaybeSource}
     * @param source2
     *            a second source {@code MaybeSource}
     * @param zipper
     *            a function that, when applied to an item emitted by each of the source {@code MaybeSource}s, results
     *            in an item that will be emitted by the resulting {@code Maybe}
     * @return the new {@code Maybe} instance
     * @throws NullPointerException if {@code source1}, {@code source2} or {@code zipper} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/zip.html">ReactiveX operators documentation: Zip</a>
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <@NonNull T1, @NonNull T2, @NonNull R> Maybe<R> zip(
            @NonNull MaybeSource<? extends T1> source1, @NonNull MaybeSource<? extends T2> source2,
            @NonNull BiFunction<? super T1, ? super T2, ? extends R> zipper) {
        Objects.requireNonNull(source1, "source1 is null");
        Objects.requireNonNull(source2, "source2 is null");
        Objects.requireNonNull(zipper, "zipper is null");
        return zipArray(Functions.toFunction(zipper), source1, source2);
    }

    /**
     * Returns a {@code Maybe} that emits the results of a specified combiner function applied to combinations of
     * three items emitted, in sequence, by three other {@link MaybeSource}s.
     * <p>
     * <img width="640" height="434" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.zip.n.png" alt="">
     * <p>
     * This operator terminates eagerly if any of the source {@code MaybeSource}s signal an {@code onError} or {@code onComplete}. This
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
     *            the first source {@code MaybeSource}
     * @param source2
     *            a second source {@code MaybeSource}
     * @param source3
     *            a third source {@code MaybeSource}
     * @param zipper
     *            a function that, when applied to an item emitted by each of the source {@code MaybeSource}s, results in
     *            an item that will be emitted by the resulting {@code Maybe}
     * @return the new {@code Maybe} instance
     * @throws NullPointerException if {@code source1}, {@code source2}, {@code source3} or {@code zipper} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/zip.html">ReactiveX operators documentation: Zip</a>
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <@NonNull T1, @NonNull T2, @NonNull T3, @NonNull R> Maybe<R> zip(
            @NonNull MaybeSource<? extends T1> source1, @NonNull MaybeSource<? extends T2> source2, @NonNull MaybeSource<? extends T3> source3,
            @NonNull Function3<? super T1, ? super T2, ? super T3, ? extends R> zipper) {
        Objects.requireNonNull(source1, "source1 is null");
        Objects.requireNonNull(source2, "source2 is null");
        Objects.requireNonNull(source3, "source3 is null");
        Objects.requireNonNull(zipper, "zipper is null");
        return zipArray(Functions.toFunction(zipper), source1, source2, source3);
    }

    /**
     * Returns a {@code Maybe} that emits the results of a specified combiner function applied to combinations of
     * four items emitted, in sequence, by four other {@link MaybeSource}s.
     * <p>
     * <img width="640" height="434" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.zip.n.png" alt="">
     * <p>
     * This operator terminates eagerly if any of the source {@code MaybeSource}s signal an {@code onError} or {@code onComplete}. This
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
     *            the first source {@code MaybeSource}
     * @param source2
     *            a second source {@code MaybeSource}
     * @param source3
     *            a third source {@code MaybeSource}
     * @param source4
     *            a fourth source {@code MaybeSource}
     * @param zipper
     *            a function that, when applied to an item emitted by each of the source {@code MaybeSource}s, results in
     *            an item that will be emitted by the resulting {@code Maybe}
     * @return the new {@code Maybe} instance
     * @throws NullPointerException if {@code source1}, {@code source2}, {@code source3},
     *                              {@code source4} or {@code zipper} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/zip.html">ReactiveX operators documentation: Zip</a>
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <@NonNull T1, @NonNull T2, @NonNull T3, @NonNull T4, @NonNull R> Maybe<R> zip(
            @NonNull MaybeSource<? extends T1> source1, @NonNull MaybeSource<? extends T2> source2, @NonNull MaybeSource<? extends T3> source3,
            @NonNull MaybeSource<? extends T4> source4,
            @NonNull Function4<? super T1, ? super T2, ? super T3, ? super T4, ? extends R> zipper) {
        Objects.requireNonNull(source1, "source1 is null");
        Objects.requireNonNull(source2, "source2 is null");
        Objects.requireNonNull(source3, "source3 is null");
        Objects.requireNonNull(source4, "source4 is null");
        Objects.requireNonNull(zipper, "zipper is null");
        return zipArray(Functions.toFunction(zipper), source1, source2, source3, source4);
    }

    /**
     * Returns a {@code Maybe} that emits the results of a specified combiner function applied to combinations of
     * five items emitted, in sequence, by five other {@link MaybeSource}s.
     * <p>
     * <img width="640" height="434" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.zip.n.png" alt="">
     * <p>
     * This operator terminates eagerly if any of the source {@code MaybeSource}s signal an {@code onError} or {@code onComplete}. This
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
     *            the first source {@code MaybeSource}
     * @param source2
     *            a second source {@code MaybeSource}
     * @param source3
     *            a third source {@code MaybeSource}
     * @param source4
     *            a fourth source {@code MaybeSource}
     * @param source5
     *            a fifth source {@code MaybeSource}
     * @param zipper
     *            a function that, when applied to an item emitted by each of the source {@code MaybeSource}s, results in
     *            an item that will be emitted by the resulting {@code Maybe}
     * @return the new {@code Maybe} instance
     * @throws NullPointerException if {@code source1}, {@code source2}, {@code source3},
     *                              {@code source4}, {@code source5} or {@code zipper} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/zip.html">ReactiveX operators documentation: Zip</a>
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <@NonNull T1, @NonNull T2, @NonNull T3, @NonNull T4, @NonNull T5, @NonNull R> Maybe<R> zip(
            @NonNull MaybeSource<? extends T1> source1, @NonNull MaybeSource<? extends T2> source2, @NonNull MaybeSource<? extends T3> source3,
            @NonNull MaybeSource<? extends T4> source4, @NonNull MaybeSource<? extends T5> source5,
            @NonNull Function5<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? extends R> zipper) {
        Objects.requireNonNull(source1, "source1 is null");
        Objects.requireNonNull(source2, "source2 is null");
        Objects.requireNonNull(source3, "source3 is null");
        Objects.requireNonNull(source4, "source4 is null");
        Objects.requireNonNull(source5, "source5 is null");
        Objects.requireNonNull(zipper, "zipper is null");
        return zipArray(Functions.toFunction(zipper), source1, source2, source3, source4, source5);
    }

    /**
     * Returns a {@code Maybe} that emits the results of a specified combiner function applied to combinations of
     * six items emitted, in sequence, by six other {@link MaybeSource}s.
     * <p>
     * <img width="640" height="434" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.zip.n.png" alt="">
     * <p>
     * This operator terminates eagerly if any of the source {@code MaybeSource}s signal an {@code onError} or {@code onComplete}. This
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
     *            the first source {@code MaybeSource}
     * @param source2
     *            a second source {@code MaybeSource}
     * @param source3
     *            a third source {@code MaybeSource}
     * @param source4
     *            a fourth source {@code MaybeSource}
     * @param source5
     *            a fifth source {@code MaybeSource}
     * @param source6
     *            a sixth source {@code MaybeSource}
     * @param zipper
     *            a function that, when applied to an item emitted by each of the source {@code MaybeSource}s, results in
     *            an item that will be emitted by the resulting {@code Maybe}
     * @return the new {@code Maybe} instance
     * @throws NullPointerException if {@code source1}, {@code source2}, {@code source3},
     *                              {@code source4}, {@code source5}, {@code source6} or {@code zipper} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/zip.html">ReactiveX operators documentation: Zip</a>
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <@NonNull T1, @NonNull T2, @NonNull T3, @NonNull T4, @NonNull T5, @NonNull T6, @NonNull R> Maybe<R> zip(
            @NonNull MaybeSource<? extends T1> source1, @NonNull MaybeSource<? extends T2> source2, @NonNull MaybeSource<? extends T3> source3,
            @NonNull MaybeSource<? extends T4> source4, @NonNull MaybeSource<? extends T5> source5, @NonNull MaybeSource<? extends T6> source6,
            @NonNull Function6<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? extends R> zipper) {
        Objects.requireNonNull(source1, "source1 is null");
        Objects.requireNonNull(source2, "source2 is null");
        Objects.requireNonNull(source3, "source3 is null");
        Objects.requireNonNull(source4, "source4 is null");
        Objects.requireNonNull(source5, "source5 is null");
        Objects.requireNonNull(source6, "source6 is null");
        Objects.requireNonNull(zipper, "zipper is null");
        return zipArray(Functions.toFunction(zipper), source1, source2, source3, source4, source5, source6);
    }

    /**
     * Returns a {@code Maybe} that emits the results of a specified combiner function applied to combinations of
     * seven items emitted, in sequence, by seven other {@link MaybeSource}s.
     * <p>
     * <img width="640" height="434" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.zip.n.png" alt="">
     * <p>
     * This operator terminates eagerly if any of the source {@code MaybeSource}s signal an {@code onError} or {@code onComplete}. This
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
     *            the first source {@code MaybeSource}
     * @param source2
     *            a second source {@code MaybeSource}
     * @param source3
     *            a third source {@code MaybeSource}
     * @param source4
     *            a fourth source {@code MaybeSource}
     * @param source5
     *            a fifth source {@code MaybeSource}
     * @param source6
     *            a sixth source {@code MaybeSource}
     * @param source7
     *            a seventh source {@code MaybeSource}
     * @param zipper
     *            a function that, when applied to an item emitted by each of the source {@code MaybeSource}s, results in
     *            an item that will be emitted by the resulting {@code Maybe}
     * @throws NullPointerException if {@code source1}, {@code source2}, {@code source3},
     *                              {@code source4}, {@code source5}, {@code source6},
     *                              {@code source7} or {@code zipper} is {@code null}
     * @return the new {@code Maybe} instance
     * @see <a href="http://reactivex.io/documentation/operators/zip.html">ReactiveX operators documentation: Zip</a>
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <@NonNull T1, @NonNull T2, @NonNull T3, @NonNull T4, @NonNull T5, @NonNull T6, @NonNull T7, @NonNull R> Maybe<R> zip(
            @NonNull MaybeSource<? extends T1> source1, @NonNull MaybeSource<? extends T2> source2, @NonNull MaybeSource<? extends T3> source3,
            @NonNull MaybeSource<? extends T4> source4, @NonNull MaybeSource<? extends T5> source5, @NonNull MaybeSource<? extends T6> source6,
            @NonNull MaybeSource<? extends T7> source7,
            @NonNull Function7<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? extends R> zipper) {
        Objects.requireNonNull(source1, "source1 is null");
        Objects.requireNonNull(source2, "source2 is null");
        Objects.requireNonNull(source3, "source3 is null");
        Objects.requireNonNull(source4, "source4 is null");
        Objects.requireNonNull(source5, "source5 is null");
        Objects.requireNonNull(source6, "source6 is null");
        Objects.requireNonNull(source7, "source7 is null");
        Objects.requireNonNull(zipper, "zipper is null");
        return zipArray(Functions.toFunction(zipper), source1, source2, source3, source4, source5, source6, source7);
    }

    /**
     * Returns a {@code Maybe} that emits the results of a specified combiner function applied to combinations of
     * eight items emitted, in sequence, by eight other {@link MaybeSource}s.
     * <p>
     * <img width="640" height="434" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.zip.n.png" alt="">
     * <p>
     * This operator terminates eagerly if any of the source {@code MaybeSource}s signal an {@code onError} or {@code onComplete}. This
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
     *            the first source {@code MaybeSource}
     * @param source2
     *            a second source {@code MaybeSource}
     * @param source3
     *            a third source {@code MaybeSource}
     * @param source4
     *            a fourth source {@code MaybeSource}
     * @param source5
     *            a fifth source {@code MaybeSource}
     * @param source6
     *            a sixth source {@code MaybeSource}
     * @param source7
     *            a seventh source {@code MaybeSource}
     * @param source8
     *            an eighth source {@code MaybeSource}
     * @param zipper
     *            a function that, when applied to an item emitted by each of the source {@code MaybeSource}s, results in
     *            an item that will be emitted by the resulting {@code Maybe}
     * @return the new {@code Maybe} instance
     * @throws NullPointerException if {@code source1}, {@code source2}, {@code source3},
     *                              {@code source4}, {@code source5}, {@code source6},
     *                              {@code source7}, {@code source8} or {@code zipper} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/zip.html">ReactiveX operators documentation: Zip</a>
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <@NonNull T1, @NonNull T2, @NonNull T3, @NonNull T4, @NonNull T5, @NonNull T6, @NonNull T7, @NonNull T8, @NonNull R> Maybe<R> zip(
            @NonNull MaybeSource<? extends T1> source1, @NonNull MaybeSource<? extends T2> source2, @NonNull MaybeSource<? extends T3> source3,
            @NonNull MaybeSource<? extends T4> source4, @NonNull MaybeSource<? extends T5> source5, @NonNull MaybeSource<? extends T6> source6,
            @NonNull MaybeSource<? extends T7> source7, @NonNull MaybeSource<? extends T8> source8,
            @NonNull Function8<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? super T8, ? extends R> zipper) {
        Objects.requireNonNull(source1, "source1 is null");
        Objects.requireNonNull(source2, "source2 is null");
        Objects.requireNonNull(source3, "source3 is null");
        Objects.requireNonNull(source4, "source4 is null");
        Objects.requireNonNull(source5, "source5 is null");
        Objects.requireNonNull(source6, "source6 is null");
        Objects.requireNonNull(source7, "source7 is null");
        Objects.requireNonNull(source8, "source8 is null");
        Objects.requireNonNull(zipper, "zipper is null");
        return zipArray(Functions.toFunction(zipper), source1, source2, source3, source4, source5, source6, source7, source8);
    }

    /**
     * Returns a {@code Maybe} that emits the results of a specified combiner function applied to combinations of
     * nine items emitted, in sequence, by nine other {@link MaybeSource}s.
     * <p>
     * <img width="640" height="434" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.zip.n.png" alt="">
     * <p>
     * This operator terminates eagerly if any of the source {@code MaybeSource}s signal an {@code onError} or {@code onComplete}. This
     * also means it is possible some sources may not get subscribed to at all.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code zip} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
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
     *            the first source {@code MaybeSource}
     * @param source2
     *            a second source {@code MaybeSource}
     * @param source3
     *            a third source {@code MaybeSource}
     * @param source4
     *            a fourth source {@code MaybeSource}
     * @param source5
     *            a fifth source {@code MaybeSource}
     * @param source6
     *            a sixth source {@code MaybeSource}
     * @param source7
     *            a seventh source {@code MaybeSource}
     * @param source8
     *            an eighth source {@code MaybeSource}
     * @param source9
     *            a ninth source {@code MaybeSource}
     * @param zipper
     *            a function that, when applied to an item emitted by each of the source {@code MaybeSource}s, results in
     *            an item that will be emitted by the resulting {@code Maybe}
     * @return the new {@code Maybe} instance
     * @throws NullPointerException if {@code source1}, {@code source2}, {@code source3},
     *                              {@code source4}, {@code source5}, {@code source6},
     *                              {@code source7}, {@code source8}, {@code source9} or {@code zipper} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/zip.html">ReactiveX operators documentation: Zip</a>
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <@NonNull T1, @NonNull T2, @NonNull T3, @NonNull T4, @NonNull T5, @NonNull T6, @NonNull T7, @NonNull T8, @NonNull T9, @NonNull R> Maybe<R> zip(
            @NonNull MaybeSource<? extends T1> source1, @NonNull MaybeSource<? extends T2> source2, @NonNull MaybeSource<? extends T3> source3,
            @NonNull MaybeSource<? extends T4> source4, @NonNull MaybeSource<? extends T5> source5, @NonNull MaybeSource<? extends T6> source6,
            @NonNull MaybeSource<? extends T7> source7, @NonNull MaybeSource<? extends T8> source8, @NonNull MaybeSource<? extends T9> source9,
            @NonNull Function9<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? super T8, ? super T9, ? extends R> zipper) {

        Objects.requireNonNull(source1, "source1 is null");
        Objects.requireNonNull(source2, "source2 is null");
        Objects.requireNonNull(source3, "source3 is null");
        Objects.requireNonNull(source4, "source4 is null");
        Objects.requireNonNull(source5, "source5 is null");
        Objects.requireNonNull(source6, "source6 is null");
        Objects.requireNonNull(source7, "source7 is null");
        Objects.requireNonNull(source8, "source8 is null");
        Objects.requireNonNull(source9, "source9 is null");
        Objects.requireNonNull(zipper, "zipper is null");
        return zipArray(Functions.toFunction(zipper), source1, source2, source3, source4, source5, source6, source7, source8, source9);
    }

    /**
     * Returns a {@code Maybe} that emits the results of a specified combiner function applied to combinations of
     * items emitted, in sequence, by an array of other {@link MaybeSource}s.
     * <p>
     * Note on method signature: since Java doesn't allow creating a generic array with {@code new T[]}, the
     * implementation of this operator has to create an {@code Object[]} instead. Unfortunately, a
     * {@code Function<Integer[], R>} passed to the method would trigger a {@link ClassCastException}.
     *
     * <p>
     * <img width="640" height="340" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.zipArray.png" alt="">
     * <p>This operator terminates eagerly if any of the source {@code MaybeSource}s signal an {@code onError} or {@code onComplete}. This
     * also means it is possible some sources may not get subscribed to at all.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code zipArray} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T> the common element type
     * @param <R> the result type
     * @param sources
     *            an array of source {@code MaybeSource}s
     * @param zipper
     *            a function that, when applied to an item emitted by each of the source {@code MaybeSource}s, results in
     *            an item that will be emitted by the resulting {@code Maybe}
     * @return the new {@code Maybe} instance
     * @throws NullPointerException if {@code sources} or {@code zipper} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/zip.html">ReactiveX operators documentation: Zip</a>
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    @SafeVarargs
    public static <@NonNull T, @NonNull R> Maybe<R> zipArray(@NonNull Function<? super Object[], ? extends R> zipper,
            @NonNull MaybeSource<? extends T>... sources) {
        Objects.requireNonNull(sources, "sources is null");
        if (sources.length == 0) {
            return empty();
        }
        Objects.requireNonNull(zipper, "zipper is null");
        return RxJavaPlugins.onAssembly(new MaybeZipArray<>(sources, zipper));
    }

    // ------------------------------------------------------------------
    // Instance methods
    // ------------------------------------------------------------------

    /**
     * Mirrors the {@link MaybeSource} (current or provided) that first signals an event.
     * <p>
     * <img width="640" height="310" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.ambWith.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code ambWith} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param other
     *            a {@code MaybeSource} competing to react first. A subscription to this provided source will occur after
     *            subscribing to the current source.
     * @return the new {@code Maybe} instance
     * @throws NullPointerException if {@code other} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/amb.html">ReactiveX operators documentation: Amb</a>
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Maybe<T> ambWith(@NonNull MaybeSource<? extends T> other) {
        Objects.requireNonNull(other, "other is null");
        return ambArray(this, other);
    }

    /**
     * Waits in a blocking fashion until the current {@code Maybe} signals a success value (which is returned),
     * {@code null} if completed or an exception (which is propagated).
     * <p>
     * <img width="640" height="285" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.blockingGet.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code blockingGet} does not operate by default on a particular {@link Scheduler}.</dd>
     * <dt><b>Error handling:</b></dt>
     * <dd>If the source signals an error, the operator wraps a checked {@link Exception}
     * into {@link RuntimeException} and throws that. Otherwise, {@code RuntimeException}s and
     * {@link Error}s are rethrown as they are.</dd>
     * </dl>
     * @return the success value
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @Nullable
    public final T blockingGet() {
        BlockingMultiObserver<T> observer = new BlockingMultiObserver<>();
        subscribe(observer);
        return observer.blockingGet();
    }

    /**
     * Waits in a blocking fashion until the current {@code Maybe} signals a success value (which is returned),
     * defaultValue if completed or an exception (which is propagated).
     * <p>
     * <img width="640" height="297" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.blockingGet.v.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code blockingGet} does not operate by default on a particular {@link Scheduler}.</dd>
     * <dt><b>Error handling:</b></dt>
     * <dd>If the source signals an error, the operator wraps a checked {@link Exception}
     * into {@link RuntimeException} and throws that. Otherwise, {@code RuntimeException}s and
     * {@link Error}s are rethrown as they are.</dd>
     * </dl>
     * @param defaultValue the default item to return if this {@code Maybe} is empty
     * @return the success value
     * @throws NullPointerException if {@code defaultValue} is {@code null}
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final T blockingGet(@NonNull T defaultValue) {
        Objects.requireNonNull(defaultValue, "defaultValue is null");
        BlockingMultiObserver<T> observer = new BlockingMultiObserver<>();
        subscribe(observer);
        return observer.blockingGet(defaultValue);
    }

    /**
     * Subscribes to the current {@code Maybe} and <em>blocks the current thread</em> until it terminates.
     * <p>
     * <img width="640" height="238" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.blockingSubscribe.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code blockingSubscribe} does not operate by default on a particular {@link Scheduler}.</dd>
     *  <dt><b>Error handling:</b></dt>
     *  <dd>If the current {@code Maybe} signals an error,
     *  the {@link Throwable} is routed to the global error handler via {@link RxJavaPlugins#onError(Throwable)}.
     *  If the current thread is interrupted, an {@link InterruptedException} is routed to the same global error handler.
     *  </dd>
     * </dl>
     * @since 3.0.0
     * @see #blockingSubscribe(Consumer)
     * @see #blockingSubscribe(Consumer, Consumer)
     * @see #blockingSubscribe(Consumer, Consumer, Action)
     */
    @SchedulerSupport(SchedulerSupport.NONE)
    public final void blockingSubscribe() {
        blockingSubscribe(Functions.emptyConsumer(), Functions.ERROR_CONSUMER, Functions.EMPTY_ACTION);
    }

    /**
     * Subscribes to the current {@code Maybe} and calls given {@code onSuccess} callback on the <em>current thread</em>
     * when it completes normally.
     * <p>
     * <img width="640" height="245" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.blockingSubscribe.c.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code blockingSubscribe} does not operate by default on a particular {@link Scheduler}.</dd>
     *  <dt><b>Error handling:</b></dt>
     *  <dd>If either the current {@code Maybe} signals an error or {@code onSuccess} throws,
     *  the respective {@link Throwable} is routed to the global error handler via {@link RxJavaPlugins#onError(Throwable)}.
     *  If the current thread is interrupted, an {@link InterruptedException} is routed to the same global error handler.
     *  </dd>
     * </dl>
     * @param onSuccess the {@link Consumer} to call if the current {@code Maybe} succeeds
     * @throws NullPointerException if {@code onSuccess} is {@code null}
     * @since 3.0.0
     * @see #blockingSubscribe(Consumer, Consumer)
     * @see #blockingSubscribe(Consumer, Consumer, Action)
     */
    @SchedulerSupport(SchedulerSupport.NONE)
    public final void blockingSubscribe(@NonNull Consumer<? super T> onSuccess) {
        blockingSubscribe(onSuccess, Functions.ERROR_CONSUMER, Functions.EMPTY_ACTION);
    }

    /**
     * Subscribes to the current {@code Maybe} and calls the appropriate callback on the <em>current thread</em>
     * when it terminates.
     * <p>
     * <img width="640" height="256" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.blockingSubscribe.cc.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code blockingSubscribe} does not operate by default on a particular {@link Scheduler}.</dd>
     *  <dt><b>Error handling:</b></dt>
     *  <dd>If either {@code onSuccess} or {@code onError} throw, the {@link Throwable} is routed to the
     *  global error handler via {@link RxJavaPlugins#onError(Throwable)}.
     *  If the current thread is interrupted, the {@code onError} consumer is called with an {@link InterruptedException}.
     *  </dd>
     * </dl>
     * @param onSuccess the {@link Consumer} to call if the current {@code Maybe} succeeds
     * @param onError the {@code Consumer} to call if the current {@code Maybe} signals an error
     * @throws NullPointerException if {@code onSuccess} or {@code onError} is {@code null}
     * @since 3.0.0
     * @see #blockingSubscribe(Consumer, Consumer, Action)
     */
    @SchedulerSupport(SchedulerSupport.NONE)
    public final void blockingSubscribe(@NonNull Consumer<? super T> onSuccess, @NonNull Consumer<? super Throwable> onError) {
        blockingSubscribe(onSuccess, onError, Functions.EMPTY_ACTION);
    }

    /**
     * Subscribes to the current {@code Maybe} and calls the appropriate callback on the <em>current thread</em>
     * when it terminates.
     * <p>
     * <img width="640" height="251" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.blockingSubscribe.cca.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code blockingSubscribe} does not operate by default on a particular {@link Scheduler}.</dd>
     *  <dt><b>Error handling:</b></dt>
     *  <dd>If either {@code onSuccess}, {@code onError} or {@code onComplete} throw, the {@link Throwable} is routed to the
     *  global error handler via {@link RxJavaPlugins#onError(Throwable)}.
     *  If the current thread is interrupted, the {@code onError} consumer is called with an {@link InterruptedException}.
     *  </dd>
     * </dl>
     * @param onSuccess the {@link Consumer} to call if the current {@code Maybe} succeeds
     * @param onError the {@code Consumer} to call if the current {@code Maybe} signals an error
     * @param onComplete the {@link Action} to call if the current {@code Maybe} completes without a value
     * @throws NullPointerException if {@code onSuccess}, {@code onError} or {@code onComplete} is {@code null}
     * @since 3.0.0
     */
    @SchedulerSupport(SchedulerSupport.NONE)
    public final void blockingSubscribe(@NonNull Consumer<? super T> onSuccess, @NonNull Consumer<? super Throwable> onError, @NonNull Action onComplete) {
        Objects.requireNonNull(onSuccess, "onSuccess is null");
        Objects.requireNonNull(onError, "onError is null");
        Objects.requireNonNull(onComplete, "onComplete is null");
        BlockingMultiObserver<T> observer = new BlockingMultiObserver<>();
        subscribe(observer);
        observer.blockingConsume(onSuccess, onError, onComplete);
    }

    /**
     * Subscribes to the current {@code Maybe} and calls the appropriate {@link MaybeObserver} method on the <em>current thread</em>.
     * <p>
     * <img width="640" height="398" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.blockingSubscribe.o.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code blockingSubscribe} does not operate by default on a particular {@link Scheduler}.</dd>
     *  <dt><b>Error handling:</b></dt>
     *  <dd>An {@code onError} signal is delivered to the {@link MaybeObserver#onError(Throwable)} method.
     *  If any of the {@code MaybeObserver}'s methods throw, the {@link RuntimeException} is propagated to the caller of this method.
     *  If the current thread is interrupted, an {@link InterruptedException} is delivered to {@code observer.onError}.
     *  </dd>
     * </dl>
     * @param observer the {@code MaybeObserver} to call methods on the current thread
     * @throws NullPointerException if {@code observer} is {@code null}
     * @since 3.0.0
     */
    @SchedulerSupport(SchedulerSupport.NONE)
    public final void blockingSubscribe(@NonNull MaybeObserver<? super T> observer) {
        Objects.requireNonNull(observer, "observer is null");
        BlockingDisposableMultiObserver<T> blockingObserver = new BlockingDisposableMultiObserver<>();
        observer.onSubscribe(blockingObserver);
        subscribe(blockingObserver);
        blockingObserver.blockingConsume(observer);
    }

    /**
     * Returns a {@code Maybe} that subscribes to this {@code Maybe} lazily, caches its event
     * and replays it, to all the downstream subscribers.
     * <p>
     * <img width="640" height="244" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.cache.png" alt="">
     * <p>
     * The operator subscribes only when the first downstream subscriber subscribes and maintains
     * a single subscription towards this {@code Maybe}.
     * <p>
     * <em>Note:</em> You sacrifice the ability to dispose the origin when you use the {@code cache}.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code cache} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @return the new {@code Maybe} instance
     * @see <a href="http://reactivex.io/documentation/operators/replay.html">ReactiveX operators documentation: Replay</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Maybe<T> cache() {
        return RxJavaPlugins.onAssembly(new MaybeCache<>(this));
    }

    /**
     * Casts the success value of the current {@code Maybe} into the target type or signals a
     * {@link ClassCastException} if not compatible.
     * <p>
     * <img width="640" height="318" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.cast.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code cast} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <U> the target type
     * @param clazz the type token to use for casting the success result from the current {@code Maybe}
     * @return the new {@code Maybe} instance
     * @throws NullPointerException if {@code clazz} is {@code null}
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public final <@NonNull U> Maybe<U> cast(@NonNull Class<? extends U> clazz) {
        Objects.requireNonNull(clazz, "clazz is null");
        return map(Functions.castFunction(clazz));
    }

    /**
     * Transform a {@code Maybe} by applying a particular {@link MaybeTransformer} function to it.
     * <p>
     * <img width="640" height="615" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.compose.png" alt="">
     * <p>
     * This method operates on the {@code Maybe} itself whereas {@link #lift} operates on the {@code Maybe}'s {@link MaybeObserver}s.
     * <p>
     * If the operator you are creating is designed to act on the individual item emitted by a {@code Maybe}, use
     * {@link #lift}. If your operator is designed to transform the current {@code Maybe} as a whole (for instance, by
     * applying a particular set of existing RxJava operators to it) use {@code compose}.
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code compose} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <R> the value type of the {@code Maybe} returned by the transformer function
     * @param transformer the transformer function, not {@code null}
     * @return the new {@code Maybe} instance
     * @throws NullPointerException if {@code transformer} is {@code null}
     * @see <a href="https://github.com/ReactiveX/RxJava/wiki/Implementing-Your-Own-Operators">RxJava wiki: Implementing Your Own Operators</a>
     */
    @SuppressWarnings("unchecked")
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <@NonNull R> Maybe<R> compose(@NonNull MaybeTransformer<? super T, ? extends R> transformer) {
        return wrap(((MaybeTransformer<T, R>) Objects.requireNonNull(transformer, "transformer is null")).apply(this));
    }

    /**
     * Returns a {@code Maybe} that is based on applying a specified function to the item emitted by the current {@code Maybe},
     * where that function returns a {@link MaybeSource}.
     * <p>
     * <img width="640" height="216" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.concatMap.png" alt="">
     * <p>
     * Note that flatMap and concatMap for {@code Maybe} is the same operation.
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code concatMap} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <R> the result value type
     * @param mapper
     *            a function that, when applied to the item emitted by the current {@code Maybe}, returns a {@code MaybeSource}
     * @return the new {@code Maybe} instance
     * @see <a href="http://reactivex.io/documentation/operators/flatmap.html">ReactiveX operators documentation: FlatMap</a>
     * @throws NullPointerException if {@code mapper} is {@code null}
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public final <@NonNull R> Maybe<R> concatMap(@NonNull Function<? super T, ? extends MaybeSource<? extends R>> mapper) {
        return flatMap(mapper);
    }

    /**
     * Returns a {@link Completable} that completes based on applying a specified function to the item emitted by the
     * current {@code Maybe}, where that function returns a {@code Completable}.
     * <p>
     * <img width="640" height="304" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.concatMapCompletable.png" alt="">
     * <p>
     * This operator is an alias for {@link #flatMapCompletable(Function)}.
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code concatMapCompletable} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param mapper
     *            a function that, when applied to the item emitted by the current {@code Maybe}, returns a
     *            {@code Completable}
     * @return the new {@code Completable} instance
     * @throws NullPointerException if {@code mapper} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/flatmap.html">ReactiveX operators documentation: FlatMap</a>
     * @since 3.0.0
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Completable concatMapCompletable(@NonNull Function<? super T, ? extends CompletableSource> mapper) {
        return flatMapCompletable(mapper);
    }

    /**
     * Returns a {@code Maybe} based on applying a specified function to the item emitted by the
     * current {@code Maybe}, where that function returns a {@link Single}.
     * When this {@code Maybe} just completes the resulting {@code Maybe} completes as well.
     * <p>
     * <img width="640" height="315" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.concatMapSingle.png" alt="">
     * <p>
     * This operator is an alias for {@link #flatMapSingle(Function)}.
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code concatMapSingle} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <R> the result value type
     * @param mapper
     *            a function that, when applied to the item emitted by the current {@code Maybe}, returns a
     *            {@code Single}
     * @return the new {@code Maybe} instance
     * @throws NullPointerException if {@code mapper} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/flatmap.html">ReactiveX operators documentation: FlatMap</a>
     * @since 3.0.0
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public final <@NonNull R> Maybe<R> concatMapSingle(@NonNull Function<? super T, ? extends SingleSource<? extends R>> mapper) {
        return flatMapSingle(mapper);
    }

    /**
     * Returns a {@link Flowable} that emits the items emitted from the current {@code Maybe}, then the {@code other} {@link MaybeSource}, one after
     * the other, without interleaving them.
     * <p>
     * <img width="640" height="172" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.concatWith.png" alt="">
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The operator honors backpressure from downstream.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code concatWith} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param other
     *            a {@code MaybeSource} to be concatenated after the current
     * @return the new {@code Flowable} instance
     * @throws NullPointerException if {@code other} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/concat.html">ReactiveX operators documentation: Concat</a>
     */
    @BackpressureSupport(BackpressureKind.FULL)
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Flowable<T> concatWith(@NonNull MaybeSource<? extends T> other) {
        Objects.requireNonNull(other, "other is null");
        return concat(this, other);
    }

    /**
     * Returns a {@link Single} that emits a {@link Boolean} that indicates whether the current {@code Maybe} emitted a
     * specified item.
     * <p>
     * <img width="640" height="310" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.contains.o.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code contains} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param item
     *            the item to search for in the emissions from the current {@code Maybe}, not {@code null}
     * @return the new {@code Single} instance
     * @throws NullPointerException if {@code item} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/contains.html">ReactiveX operators documentation: Contains</a>
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Single<Boolean> contains(@NonNull Object item) {
        Objects.requireNonNull(item, "item is null");
        return RxJavaPlugins.onAssembly(new MaybeContains<>(this, item));
    }

    /**
     * Returns a {@link Single} that counts the total number of items emitted (0 or 1) by the current {@code Maybe} and emits
     * this count as a 64-bit {@link Long}.
     * <p>
     * <img width="640" height="434" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.count.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code count} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @return the new {@code Single} instance
     * @see <a href="http://reactivex.io/documentation/operators/count.html">ReactiveX operators documentation: Count</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Single<Long> count() {
        return RxJavaPlugins.onAssembly(new MaybeCount<>(this));
    }

    /**
     * Returns a {@link Single} that emits the item emitted by the current {@code Maybe} or a specified default item
     * if the current {@code Maybe} is empty.
     * <p>
     * <img width="640" height="390" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.defaultIfEmpty.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code defaultIfEmpty} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param defaultItem
     *            the item to emit if the current {@code Maybe} emits no items
     * @return the new {@code Single} instance
     * @throws NullPointerException if {@code defaultItem} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/defaultifempty.html">ReactiveX operators documentation: DefaultIfEmpty</a>
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Single<T> defaultIfEmpty(@NonNull T defaultItem) {
        Objects.requireNonNull(defaultItem, "defaultItem is null");
        return RxJavaPlugins.onAssembly(new MaybeToSingle<>(this, defaultItem));
    }

    /**
     * Maps the {@link Notification} success value of the current {@code Maybe} back into normal
     * {@code onSuccess}, {@code onError} or {@code onComplete} signals.
     * <p>
     * <img width="640" height="268" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.dematerialize.png" alt="">
     * <p>
     * The intended use of the {@code selector} function is to perform a
     * type-safe identity mapping (see example) on a source that is already of type
     * {@code Notification<T>}. The Java language doesn't allow
     * limiting instance methods to a certain generic argument shape, therefore,
     * a function is used to ensure the conversion remains type safe.
     * <p>
     * Regular {@code onError} or {@code onComplete} signals from the current {@code Maybe} are passed along to the downstream.
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     *  <dd>{@code dematerialize} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * <p>
     * Example:
     * <pre><code>
     * Maybe.just(Notification.createOnNext(1))
     * .dematerialize(notification -&gt; notification)
     * .test()
     * .assertResult(1);
     * </code></pre>
     * @param <R> the result type
     * @param selector the function called with the success item and should
     * return a {@code Notification} instance.
     * @return the new {@code Maybe} instance
     * @throws NullPointerException if {@code selector} is {@code null}
     * @since 3.0.0
     * @see #materialize()
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public final <@NonNull R> Maybe<R> dematerialize(@NonNull Function<? super T, @NonNull Notification<R>> selector) {
        Objects.requireNonNull(selector, "selector is null");
        return RxJavaPlugins.onAssembly(new MaybeDematerialize<>(this, selector));
    }

    /**
     * Returns a {@code Maybe} that signals the events emitted by the current {@code Maybe} shifted forward in time by a
     * specified delay.
     * An error signal will not be delayed.
     * <p>
     * <img width="640" height="434" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.delay.t.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This version of {@code delay} operates by default on the {@code computation} {@link Scheduler}.</dd>
     * </dl>
     *
     * @param time
     *            the delay to shift the source by
     * @param unit
     *            the {@link TimeUnit} in which {@code time} is defined
     * @return the new {@code Maybe} instance
     * @throws NullPointerException if {@code unit} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/delay.html">ReactiveX operators documentation: Delay</a>
     * @see #delay(long, TimeUnit, Scheduler, boolean)
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.COMPUTATION)
    @NonNull
    public final Maybe<T> delay(long time, @NonNull TimeUnit unit) {
        return delay(time, unit, Schedulers.computation(), false);
    }

    /**
     * Returns a {@code Maybe} that signals the events emitted by the current {@code Maybe} shifted forward in time by a
     * specified delay.
     * <p>
     * <img width="640" height="340" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.delay.tb.png" alt="">
     * <dl>
     *   <dt><b>Scheduler:</b></dt>
     *   <dd>This version of {@code delay} operates by default on the {@code computation} {@link Scheduler}.</dd>
     * </dl>
     *
     * @param time the delay to shift the source by
     * @param unit the {@link TimeUnit} in which {@code time} is defined
     * @param delayError if {@code true}, both success and error signals are delayed. if {@code false}, only success signals are delayed.
     * @return the new {@code Maybe} instance
     * @throws NullPointerException if {@code unit} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/delay.html">ReactiveX operators documentation: Delay</a>
     * @see #delay(long, TimeUnit, Scheduler, boolean)
     * @since 3.0.0
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.COMPUTATION)
    @NonNull
    public final Maybe<T> delay(long time, @NonNull TimeUnit unit, boolean delayError) {
        return delay(time, unit, Schedulers.computation(), delayError);
    }

    /**
     * Returns a {@code Maybe} that signals the events emitted by the current {@code Maybe} shifted forward in time by a
     * specified delay.
     * An error signal will not be delayed.
     * <p>
     * <img width="640" height="434" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.delay.ts.png" alt="">
     * <dl>
     *   <dt><b>Scheduler:</b></dt>
     *   <dd>you specify the {@link Scheduler} where the non-blocking wait and emission happens</dd>
     * </dl>
     *
     * @param time the delay to shift the source by
     * @param unit the {@link TimeUnit} in which {@code time} is defined
     * @param scheduler the {@code Scheduler} to use for delaying
     * @return the new {@code Maybe} instance
     * @throws NullPointerException if {@code unit} or {@code scheduler} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/delay.html">ReactiveX operators documentation: Delay</a>
     * @see #delay(long, TimeUnit, Scheduler, boolean)
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    @NonNull
    public final Maybe<T> delay(long time, @NonNull TimeUnit unit, @NonNull Scheduler scheduler) {
        return delay(time, unit, scheduler, false);
    }

    /**
     * Returns a {@code Maybe} that signals the events emitted by the current {@code Maybe} shifted forward in time by a
     * specified delay running on the specified {@link Scheduler}.
     * <p>
     * <img width="640" height="352" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.delay.tsb.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>you specify which {@code Scheduler} this operator will use.</dd>
     * </dl>
     *
     * @param time
     *            the delay to shift the source by
     * @param unit
     *            the {@link TimeUnit} in which {@code time} is defined
     * @param scheduler
     *            the {@code Scheduler} to use for delaying
     * @param delayError if {@code true}, both success and error signals are delayed. if {@code false}, only success signals are delayed.
     * @return the new {@code Maybe} instance
     * @throws NullPointerException if {@code unit} or {@code scheduler} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/delay.html">ReactiveX operators documentation: Delay</a>
     * @since 3.0.0
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    public final Maybe<T> delay(long time, @NonNull TimeUnit unit, @NonNull Scheduler scheduler, boolean delayError) {
        Objects.requireNonNull(unit, "unit is null");
        Objects.requireNonNull(scheduler, "scheduler is null");
        return RxJavaPlugins.onAssembly(new MaybeDelay<>(this, Math.max(0L, time), unit, scheduler, delayError));
    }

    /**
     * Delays the emission of this {@code Maybe} until the given {@link Publisher} signals an item or completes.
     * <p>
     * <img width="640" height="175" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.delay.p.png" alt="">
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
     * @param delayIndicator
     *            the {@code Publisher} that gets subscribed to when this {@code Maybe} signals an event and that
     *            signal is emitted when the {@code Publisher} signals an item or completes
     * @return the new {@code Maybe} instance
     * @throws NullPointerException if {@code delayIndicator} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/delay.html">ReactiveX operators documentation: Delay</a>
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    @BackpressureSupport(BackpressureKind.UNBOUNDED_IN)
    public final <@NonNull U> Maybe<T> delay(@NonNull Publisher<U> delayIndicator) {
        Objects.requireNonNull(delayIndicator, "delayIndicator is null");
        return RxJavaPlugins.onAssembly(new MaybeDelayOtherPublisher<>(this, delayIndicator));
    }

    /**
     * Returns a {@code Maybe} that delays the subscription to this {@code Maybe}
     * until the other {@link Publisher} emits an element or completes normally.
     * <p>
     * <img width="640" height="214" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.delaySubscription.p.png" alt="">
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The {@code Publisher} source is consumed in an unbounded fashion (without applying backpressure).</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This method does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <U> the value type of the other {@code Publisher}, irrelevant
     * @param subscriptionIndicator the other {@code Publisher} that should trigger the subscription
     *        to this {@code Publisher}.
     * @throws NullPointerException if {@code subscriptionIndicator} is {@code null}
     * @return the new {@code Maybe} instance
     */
    @BackpressureSupport(BackpressureKind.UNBOUNDED_IN)
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public final <@NonNull U> Maybe<T> delaySubscription(@NonNull Publisher<U> subscriptionIndicator) {
        Objects.requireNonNull(subscriptionIndicator, "subscriptionIndicator is null");
        return RxJavaPlugins.onAssembly(new MaybeDelaySubscriptionOtherPublisher<>(this, subscriptionIndicator));
    }

    /**
     * Returns a {@code Maybe} that delays the subscription to the current {@code Maybe} by a given amount of time.
     * <p>
     * <img width="640" height="471" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.delaySubscription.t.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This version of {@code delaySubscription} operates by default on the {@code computation} {@link Scheduler}.</dd>
     * </dl>
     *
     * @param time
     *            the time to delay the subscription
     * @param unit
     *            the time unit of {@code delay}
     * @return the new {@code Maybe} instance
     * @throws NullPointerException if {@code unit} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/delay.html">ReactiveX operators documentation: Delay</a>
     * @see #delaySubscription(long, TimeUnit, Scheduler)
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.COMPUTATION)
    @NonNull
    public final Maybe<T> delaySubscription(long time, @NonNull TimeUnit unit) {
        return delaySubscription(time, unit, Schedulers.computation());
    }

    /**
     * Returns a {@code Maybe} that delays the subscription to the current {@code Maybe} by a given amount of time,
     * both waiting and subscribing on a given {@link Scheduler}.
     * <p>
     * <img width="640" height="420" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.delaySubscription.ts.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>You specify which {@code Scheduler} this operator will use.</dd>
     * </dl>
     *
     * @param time
     *            the time to delay the subscription
     * @param unit
     *            the time unit of {@code delay}
     * @param scheduler
     *            the {@code Scheduler} on which the waiting and subscription will happen
     * @return the new {@code Maybe} instance
     * @throws NullPointerException if {@code unit} or {@code scheduler} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/delay.html">ReactiveX operators documentation: Delay</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    @NonNull
    public final Maybe<T> delaySubscription(long time, @NonNull TimeUnit unit, @NonNull Scheduler scheduler) {
        return delaySubscription(Flowable.timer(time, unit, scheduler));
    }

    /**
     * Calls the specified {@link Consumer} with the success item after this item has been emitted to the downstream.
     * <p>Note that the {@code onAfterSuccess} action is shared between subscriptions and as such
     * should be thread-safe.
     * <p>
     * <img width="640" height="527" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.doAfterSuccess.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code doAfterSuccess} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * <p>History: 2.0.1 - experimental
     * @param onAfterSuccess the {@code Consumer} that will be called after emitting an item from upstream to the downstream
     * @return the new {@code Maybe} instance
     * @throws NullPointerException if {@code onAfterSuccess} is {@code null}
     * @since 2.1
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Maybe<T> doAfterSuccess(@NonNull Consumer<? super T> onAfterSuccess) {
        Objects.requireNonNull(onAfterSuccess, "onAfterSuccess is null");
        return RxJavaPlugins.onAssembly(new MaybeDoAfterSuccess<>(this, onAfterSuccess));
    }

    /**
     * Registers an {@link Action} to be called when this {@code Maybe} invokes either
     * {@link MaybeObserver#onComplete onSuccess},
     * {@link MaybeObserver#onComplete onComplete} or {@link MaybeObserver#onError onError}.
     * <p>
     * <img width="640" height="249" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.doAfterTerminate.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code doAfterTerminate} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param onAfterTerminate
     *            an {@code Action} to be invoked when the current {@code Maybe} finishes
     * @return the new {@code Maybe} instance
     * @throws NullPointerException if {@code onAfterTerminate} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/do.html">ReactiveX operators documentation: Do</a>
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Maybe<T> doAfterTerminate(@NonNull Action onAfterTerminate) {
        return RxJavaPlugins.onAssembly(new MaybePeek<>(this,
                Functions.emptyConsumer(), // onSubscribe
                Functions.emptyConsumer(), // onSuccess
                Functions.emptyConsumer(), // onError
                Functions.EMPTY_ACTION,    // onComplete
                Objects.requireNonNull(onAfterTerminate, "onAfterTerminate is null"),
                Functions.EMPTY_ACTION     // dispose
        ));
    }

    /**
     * Calls the specified action after this {@code Maybe} signals {@code onSuccess}, {@code onError} or {@code onComplete} or gets disposed by
     * the downstream.
     * <p>
     * <img width="640" height="247" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.doFinally.png" alt="">
     * <p>
     * In case of a race between a terminal event and a dispose call, the provided {@code onFinally} action
     * is executed once per subscription.
     * <p>Note that the {@code onFinally} action is shared between subscriptions and as such
     * should be thread-safe.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code doFinally} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * <p>History: 2.0.1 - experimental
     * @param onFinally the action called when this {@code Maybe} terminates or gets disposed
     * @return the new {@code Maybe} instance
     * @throws NullPointerException if {@code onFinally} is {@code null}
     * @since 2.1
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Maybe<T> doFinally(@NonNull Action onFinally) {
        Objects.requireNonNull(onFinally, "onFinally is null");
        return RxJavaPlugins.onAssembly(new MaybeDoFinally<>(this, onFinally));
    }

    /**
     * Calls the shared {@link Action} if a {@link MaybeObserver} subscribed to the current {@code Maybe}
     * disposes the common {@link Disposable} it received via {@code onSubscribe}.
     * <p>
     * <img width="640" height="277" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.doOnDispose.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code doOnDispose} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param onDispose the action called when the subscription is disposed
     * @throws NullPointerException if {@code onDispose} is {@code null}
     * @return the new {@code Maybe} instance
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Maybe<T> doOnDispose(@NonNull Action onDispose) {
        return RxJavaPlugins.onAssembly(new MaybePeek<>(this,
                Functions.emptyConsumer(), // onSubscribe
                Functions.emptyConsumer(), // onSuccess
                Functions.emptyConsumer(), // onError
                Functions.EMPTY_ACTION,    // onComplete
                Functions.EMPTY_ACTION,    // (onSuccess | onError | onComplete) after
                Objects.requireNonNull(onDispose, "onDispose is null")
        ));
    }

    /**
     * Invokes an {@link Action} just before the current {@code Maybe} calls {@code onComplete}.
     * <p>
     * <img width="640" height="358" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/doOnComplete.m.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code doOnComplete} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param onComplete
     *            the action to invoke when the current {@code Maybe} calls {@code onComplete}
     * @return the new {@code Maybe} with the side-effecting behavior applied
     * @throws NullPointerException if {@code onComplete} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/do.html">ReactiveX operators documentation: Do</a>
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Maybe<T> doOnComplete(@NonNull Action onComplete) {
        return RxJavaPlugins.onAssembly(new MaybePeek<>(this,
                Functions.emptyConsumer(), // onSubscribe
                Functions.emptyConsumer(), // onSuccess
                Functions.emptyConsumer(), // onError
                Objects.requireNonNull(onComplete, "onComplete is null"),
                Functions.EMPTY_ACTION,    // (onSuccess | onError | onComplete)
                Functions.EMPTY_ACTION     // dispose
        ));
    }

    /**
     * Calls the shared {@link Consumer} with the error sent via {@code onError} for each
     * {@link MaybeObserver} that subscribes to the current {@code Maybe}.
     * <p>
     * <img width="640" height="358" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/doOnError.m.v3.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code doOnError} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param onError the consumer called with the success value of {@code onError}
     * @return the new {@code Maybe} instance
     * @throws NullPointerException if {@code onError} is {@code null}
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Maybe<T> doOnError(@NonNull Consumer<? super Throwable> onError) {
        return RxJavaPlugins.onAssembly(new MaybePeek<>(this,
                Functions.emptyConsumer(), // onSubscribe
                Functions.emptyConsumer(), // onSuccess
                Objects.requireNonNull(onError, "onError is null"),
                Functions.EMPTY_ACTION,    // onComplete
                Functions.EMPTY_ACTION,    // (onSuccess | onError | onComplete)
                Functions.EMPTY_ACTION     // dispose
        ));
    }

    /**
     * Calls the given {@code onEvent} callback with the (success value, {@code null}) for an {@code onSuccess}, ({@code null}, throwable) for
     * an {@code onError} or ({@code null}, {@code null}) for an {@code onComplete} signal from this {@code Maybe} before delivering said
     * signal to the downstream.
     * <p>
     * <img width="640" height="297" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.doOnEvent.png" alt="">
     * <p>
     * The exceptions thrown from the callback will override the event so the downstream receives the
     * error instead of the original signal.
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code doOnEvent} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param onEvent the callback to call with the success value or the exception, whichever is not {@code null}
     * @return the new {@code Maybe} instance
     * @throws NullPointerException if {@code onEvent} is {@code null}
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Maybe<T> doOnEvent(@NonNull BiConsumer<@Nullable ? super T, @Nullable ? super Throwable> onEvent) {
        Objects.requireNonNull(onEvent, "onEvent is null");
        return RxJavaPlugins.onAssembly(new MaybeDoOnEvent<>(this, onEvent));
    }

    /**
     * Calls the appropriate {@code onXXX} method (shared between all {@link MaybeObserver}s) for the lifecycle events of
     * the sequence (subscription, disposal).
     * <p>
     * <img width="640" height="183" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.doOnLifecycle.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code doOnLifecycle} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param onSubscribe
     *              a {@link Consumer} called with the {@link Disposable} sent via {@link MaybeObserver#onSubscribe(Disposable)}
     * @param onDispose
     *              called when the downstream disposes the {@code Disposable} via {@code dispose()}
     * @return the new {@code Maybe} instance
     * @throws NullPointerException if {@code onSubscribe} or {@code onDispose} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/do.html">ReactiveX operators documentation: Do</a>
     * @since 3.0.0
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Maybe<T> doOnLifecycle(@NonNull Consumer<? super Disposable> onSubscribe, @NonNull Action onDispose) {
        Objects.requireNonNull(onSubscribe, "onSubscribe is null");
        Objects.requireNonNull(onDispose, "onDispose is null");
        return RxJavaPlugins.onAssembly(new MaybeDoOnLifecycle<>(this, onSubscribe, onDispose));
    }

    /**
     * Calls the shared {@link Consumer} with the {@link Disposable} sent through the {@code onSubscribe} for each
     * {@link MaybeObserver} that subscribes to the current {@code Maybe}.
     * <p>
     * <img width="640" height="506" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.doOnSubscribe.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code doOnSubscribe} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param onSubscribe the {@code Consumer} called with the {@code Disposable} sent via {@code onSubscribe}
     * @return the new {@code Maybe} instance
     * @throws NullPointerException if {@code onSubscribe} is {@code null}
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Maybe<T> doOnSubscribe(@NonNull Consumer<? super Disposable> onSubscribe) {
        return RxJavaPlugins.onAssembly(new MaybePeek<>(this,
                Objects.requireNonNull(onSubscribe, "onSubscribe is null"),
                Functions.emptyConsumer(), // onSuccess
                Functions.emptyConsumer(), // onError
                Functions.EMPTY_ACTION,    // onComplete
                Functions.EMPTY_ACTION,    // (onSuccess | onError | onComplete)
                Functions.EMPTY_ACTION     // dispose
        ));
    }

    /**
     * Returns a {@code Maybe} instance that calls the given onTerminate callback
     * just before this {@code Maybe} completes normally or with an exception.
     * <p>
     * <img width="640" height="249" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.doOnTerminate.png" alt="">
     * <p>
     * This differs from {@code doAfterTerminate} in that this happens <em>before</em> the {@code onComplete} or
     * {@code onError} notification.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code doOnTerminate} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * <p>History: 2.2.7 - experimental
     * @param onTerminate the action to invoke when the consumer calls {@code onComplete} or {@code onError}
     * @return the new {@code Maybe} instance
     * @throws NullPointerException if {@code onTerminate} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/do.html">ReactiveX operators documentation: Do</a>
     * @see #doOnTerminate(Action)
     * @since 3.0.0
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Maybe<T> doOnTerminate(@NonNull Action onTerminate) {
        Objects.requireNonNull(onTerminate, "onTerminate is null");
        return RxJavaPlugins.onAssembly(new MaybeDoOnTerminate<>(this, onTerminate));
    }

    /**
     * Calls the shared {@link Consumer} with the success value sent via {@code onSuccess} for each
     * {@link MaybeObserver} that subscribes to the current {@code Maybe}.
     * <p>
     * <img width="640" height="358" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/doOnSuccess.m.v3.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code doOnSuccess} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param onSuccess the {@code Consumer} called with the success value of the upstream
     * @return the new {@code Maybe} instance
     * @throws NullPointerException if {@code onSuccess} is {@code null}
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Maybe<T> doOnSuccess(@NonNull Consumer<? super T> onSuccess) {
        return RxJavaPlugins.onAssembly(new MaybePeek<>(this,
                Functions.emptyConsumer(), // onSubscribe
                Objects.requireNonNull(onSuccess, "onSuccess is null"),
                Functions.emptyConsumer(), // onError
                Functions.EMPTY_ACTION,    // onComplete
                Functions.EMPTY_ACTION,    // (onSuccess | onError | onComplete)
                Functions.EMPTY_ACTION     // dispose
        ));
    }

    /**
     * Filters the success item of the {@code Maybe} via a predicate function and emitting it if the predicate
     * returns {@code true}, completing otherwise.
     * <p>
     * <img width="640" height="291" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.filter.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code filter} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param predicate
     *            a function that evaluates the item emitted by the current {@code Maybe}, returning {@code true}
     *            if it passes the filter
     * @return the new {@code Maybe} instance
     * @throws NullPointerException if {@code predicate} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/filter.html">ReactiveX operators documentation: Filter</a>
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Maybe<T> filter(@NonNull Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        return RxJavaPlugins.onAssembly(new MaybeFilter<>(this, predicate));
    }

    /**
     * Returns a {@code Maybe} that is based on applying a specified function to the item emitted by the current {@code Maybe},
     * where that function returns a {@link MaybeSource}.
     * <p>
     * <img width="640" height="357" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.flatMap.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code flatMap} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * <p>Note that flatMap and concatMap for {@code Maybe} is the same operation.
     *
     * @param <R> the result value type
     * @param mapper
     *            a function that, when applied to the item emitted by the current {@code Maybe}, returns a {@code MaybeSource}
     * @return the new {@code Maybe} instance
     * @throws NullPointerException if {@code mapper} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/flatmap.html">ReactiveX operators documentation: FlatMap</a>
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public final <@NonNull R> Maybe<R> flatMap(@NonNull Function<? super T, ? extends MaybeSource<? extends R>> mapper) {
        Objects.requireNonNull(mapper, "mapper is null");
        return RxJavaPlugins.onAssembly(new MaybeFlatten<>(this, mapper));
    }

    /**
     * Maps the {@code onSuccess}, {@code onError} or {@code onComplete} signals of the current {@code Maybe} into a {@link MaybeSource} and emits that
     * {@code MaybeSource}'s signals.
     * <p>
     * <img width="640" height="354" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.flatMap.mmm.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code flatMap} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <R>
     *            the result type
     * @param onSuccessMapper
     *            a function that returns a {@code MaybeSource} to merge for the {@code onSuccess} item emitted by this {@code Maybe}
     * @param onErrorMapper
     *            a function that returns a {@code MaybeSource} to merge for an {@code onError} notification from this {@code Maybe}
     * @param onCompleteSupplier
     *            a function that returns a {@code MaybeSource} to merge for an {@code onComplete} notification this {@code Maybe}
     * @return the new {@code Maybe} instance
     * @throws NullPointerException if {@code onSuccessMapper}, {@code onErrorMapper} or {@code onCompleteSupplier} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/flatmap.html">ReactiveX operators documentation: FlatMap</a>
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public final <@NonNull R> Maybe<R> flatMap(
            @NonNull Function<? super T, ? extends MaybeSource<? extends R>> onSuccessMapper,
            @NonNull Function<? super Throwable, ? extends MaybeSource<? extends R>> onErrorMapper,
            @NonNull Supplier<? extends MaybeSource<? extends R>> onCompleteSupplier) {
        Objects.requireNonNull(onSuccessMapper, "onSuccessMapper is null");
        Objects.requireNonNull(onErrorMapper, "onErrorMapper is null");
        Objects.requireNonNull(onCompleteSupplier, "onCompleteSupplier is null");
        return RxJavaPlugins.onAssembly(new MaybeFlatMapNotification<>(this, onSuccessMapper, onErrorMapper, onCompleteSupplier));
    }

    /**
     * Returns a {@code Maybe} that emits the results of a specified function to the pair of values emitted by the
     * current {@code Maybe} and a specified mapped {@link MaybeSource}.
     * <p>
     * <img width="640" height="268" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.flatMap.combiner.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code flatMap} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <U>
     *            the type of items emitted by the {@code MaybeSource} returned by the {@code mapper} function
     * @param <R>
     *            the type of items emitted by the resulting {@code Maybe}
     * @param mapper
     *            a function that returns a {@code MaybeSource} for the item emitted by the current {@code Maybe}
     * @param combiner
     *            a function that combines one item emitted by each of the source and collection {@code MaybeSource} and
     *            returns an item to be emitted by the resulting {@code MaybeSource}
     * @return the new {@code Maybe} instance
     * @throws NullPointerException if {@code mapper} or {@code combiner} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/flatmap.html">ReactiveX operators documentation: FlatMap</a>
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public final <@NonNull U, @NonNull R> Maybe<R> flatMap(@NonNull Function<? super T, ? extends MaybeSource<? extends U>> mapper,
            @NonNull BiFunction<? super T, ? super U, ? extends R> combiner) {
        Objects.requireNonNull(mapper, "mapper is null");
        Objects.requireNonNull(combiner, "combiner is null");
        return RxJavaPlugins.onAssembly(new MaybeFlatMapBiSelector<>(this, mapper, combiner));
    }

    /**
     * Maps the success value of the current {@code Maybe} into an {@link Iterable} and emits its items as a
     * {@link Flowable} sequence.
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
     *            the type of item emitted by the inner {@code Iterable}
     * @param mapper
     *            a function that returns an {@code Iterable} sequence of values for when given an item emitted by the
     *            current {@code Maybe}
     * @return the new {@code Flowable} instance
     * @throws NullPointerException if {@code mapper} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/flatmap.html">ReactiveX operators documentation: FlatMap</a>
     * @see #flattenStreamAsFlowable(Function)
     */
    @BackpressureSupport(BackpressureKind.FULL)
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public final <@NonNull U> Flowable<U> flattenAsFlowable(@NonNull Function<? super T, @NonNull ? extends Iterable<? extends U>> mapper) {
        Objects.requireNonNull(mapper, "mapper is null");
        return RxJavaPlugins.onAssembly(new MaybeFlatMapIterableFlowable<>(this, mapper));
    }

    /**
     * Maps the success value of the current {@code Maybe} into an {@link Iterable} and emits its items as an
     * {@link Observable} sequence.
     * <p>
     * <img width="640" height="373" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/flattenAsObservable.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code flattenAsObservable} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <U>
     *            the type of item emitted by the resulting {@code Iterable}
     * @param mapper
     *            a function that returns an {@code Iterable} sequence of values for when given an item emitted by the
     *            current {@code Maybe}
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code mapper} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/flatmap.html">ReactiveX operators documentation: FlatMap</a>
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public final <@NonNull U> Observable<U> flattenAsObservable(@NonNull Function<? super T, @NonNull ? extends Iterable<? extends U>> mapper) {
        Objects.requireNonNull(mapper, "mapper is null");
        return RxJavaPlugins.onAssembly(new MaybeFlatMapIterableObservable<>(this, mapper));
    }

    /**
     * Returns an {@link Observable} that is based on applying a specified function to the item emitted by the current {@code Maybe},
     * where that function returns an {@link ObservableSource}.
     * <p>
     * <img width="640" height="302" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.flatMapObservable.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code flatMapObservable} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <R> the result value type
     * @param mapper
     *            a function that, when applied to the item emitted by the current {@code Maybe}, returns an {@code ObservableSource}
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code mapper} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/flatmap.html">ReactiveX operators documentation: FlatMap</a>
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public final <@NonNull R> Observable<R> flatMapObservable(@NonNull Function<? super T, ? extends ObservableSource<? extends R>> mapper) {
        Objects.requireNonNull(mapper, "mapper is null");
        return RxJavaPlugins.onAssembly(new MaybeFlatMapObservable<>(this, mapper));
    }

    /**
     * Returns a {@link Flowable} that emits items based on applying a specified function to the item emitted by the
     * current {@code Maybe}, where that function returns a {@link Publisher}.
     * <p>
     * <img width="640" height="312" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.flatMapPublisher.png" alt="">
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The returned {@code Flowable} honors the downstream backpressure.</dd>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code flatMapPublisher} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <R> the result value type
     * @param mapper
     *            a function that, when applied to the item emitted by the current {@code Maybe}, returns a
     *            {@code Flowable}
     * @return the new {@code Flowable} instance
     * @throws NullPointerException if {@code mapper} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/flatmap.html">ReactiveX operators documentation: FlatMap</a>
     */
    @BackpressureSupport(BackpressureKind.FULL)
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public final <@NonNull R> Flowable<R> flatMapPublisher(@NonNull Function<? super T, @NonNull ? extends Publisher<? extends R>> mapper) {
        Objects.requireNonNull(mapper, "mapper is null");
        return RxJavaPlugins.onAssembly(new MaybeFlatMapPublisher<>(this, mapper));
    }

    /**
     * Returns a {@code Maybe} based on applying a specified function to the item emitted by the
     * current {@code Maybe}, where that function returns a {@link Single}.
     * When this {@code Maybe} just completes the resulting {@code Maybe} completes as well.
     * <p>
     * <img width="640" height="357" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.flatMapSingle.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code flatMapSingle} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * <p>History: 2.0.2 - experimental
     * @param <R> the result value type
     * @param mapper
     *            a function that, when applied to the item emitted by the current {@code Maybe}, returns a
     *            {@code Single}
     * @return the new {@code Maybe} instance
     * @throws NullPointerException if {@code mapper} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/flatmap.html">ReactiveX operators documentation: FlatMap</a>
     * @since 2.1
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public final <@NonNull R> Maybe<R> flatMapSingle(@NonNull Function<? super T, ? extends SingleSource<? extends R>> mapper) {
        Objects.requireNonNull(mapper, "mapper is null");
        return RxJavaPlugins.onAssembly(new MaybeFlatMapSingle<>(this, mapper));
    }

    /**
     * Returns a {@link Completable} that completes based on applying a specified function to the item emitted by the
     * current {@code Maybe}, where that function returns a {@code Completable}.
     * <p>
     * <img width="640" height="303" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.flatMapCompletable3.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code flatMapCompletable} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param mapper
     *            a function that, when applied to the item emitted by the current {@code Maybe}, returns a
     *            {@code Completable}
     * @return the new {@code Completable} instance
     * @throws NullPointerException if {@code mapper} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/flatmap.html">ReactiveX operators documentation: FlatMap</a>
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Completable flatMapCompletable(@NonNull Function<? super T, ? extends CompletableSource> mapper) {
        Objects.requireNonNull(mapper, "mapper is null");
        return RxJavaPlugins.onAssembly(new MaybeFlatMapCompletable<>(this, mapper));
    }

    /**
     * Hides the identity of this {@code Maybe} and its {@link Disposable}.
     * <p>
     * <img width="640" height="300" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.hide.png" alt="">
     * <p>Allows preventing certain identity-based
     * optimizations (fusion).
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code hide} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @return the new {@code Maybe} instance
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Maybe<T> hide() {
        return RxJavaPlugins.onAssembly(new MaybeHide<>(this));
    }

    /**
     * Returns a {@link Completable} that ignores the item emitted by the current {@code Maybe} and only calls {@code onComplete} or {@code onError}.
     * <p>
     * <img width="640" height="390" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.ignoreElement.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code ignoreElement} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @return the new {@code Completable} instance
     * @see <a href="http://reactivex.io/documentation/operators/ignoreelements.html">ReactiveX operators documentation: IgnoreElements</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Completable ignoreElement() {
        return RxJavaPlugins.onAssembly(new MaybeIgnoreElementCompletable<>(this));
    }

    /**
     * Returns a {@link Single} that emits {@code true} if the current {@code Maybe} is empty, otherwise {@code false}.
     * <p>
     * <img width="640" height="444" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.isEmpty.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code isEmpty} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @return the new {@code Single} instance
     * @see <a href="http://reactivex.io/documentation/operators/contains.html">ReactiveX operators documentation: Contains</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Single<Boolean> isEmpty() {
        return RxJavaPlugins.onAssembly(new MaybeIsEmptySingle<>(this));
    }

    /**
     * <strong>This method requires advanced knowledge about building operators, please consider
     * other standard composition methods first;</strong>
     * Returns a {@code Maybe} which, when subscribed to, invokes the {@link MaybeOperator#apply(MaybeObserver) apply(MaybeObserver)} method
     * of the provided {@link MaybeOperator} for each individual downstream {@code Maybe} and allows the
     * insertion of a custom operator by accessing the downstream's {@link MaybeObserver} during this subscription phase
     * and providing a new {@code MaybeObserver}, containing the custom operator's intended business logic, that will be
     * used in the subscription process going further upstream.
     * <p>
     * <img width="640" height="352" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.lift.png" alt="">
     * <p>
     * Generally, such a new {@code MaybeObserver} will wrap the downstream's {@code MaybeObserver} and forwards the
     * {@code onSuccess}, {@code onError} and {@code onComplete} events from the upstream directly or according to the
     * emission pattern the custom operator's business logic requires. In addition, such operator can intercept the
     * flow control calls of {@code dispose} and {@code isDisposed} that would have traveled upstream and perform
     * additional actions depending on the same business logic requirements.
     * <p>
     * Example:
     * <pre><code>
     * // Step 1: Create the consumer type that will be returned by the MaybeOperator.apply():
     * 
     * public final class CustomMaybeObserver&lt;T&gt; implements MaybeObserver&lt;T&gt;, Disposable {
     *
     *     // The downstream's MaybeObserver that will receive the onXXX events
     *     final MaybeObserver&lt;? super String&gt; downstream;
     *
     *     // The connection to the upstream source that will call this class' onXXX methods
     *     Disposable upstream;
     *
     *     // The constructor takes the downstream subscriber and usually any other parameters
     *     public CustomMaybeObserver(MaybeObserver&lt;? super String&gt; downstream) {
     *         this.downstream = downstream;
     *     }
     *
     *     // In the subscription phase, the upstream sends a Disposable to this class
     *     // and subsequently this class has to send a Disposable to the downstream.
     *     // Note that relaying the upstream's Disposable directly is not allowed in RxJava
     *     &#64;Override
     *     public void onSubscribe(Disposable d) {
     *         if (upstream != null) {
     *             d.dispose();
     *         } else {
     *             upstream = d;
     *             downstream.onSubscribe(this);
     *         }
     *     }
     *
     *     // The upstream calls this with the next item and the implementation's
     *     // responsibility is to emit an item to the downstream based on the intended
     *     // business logic, or if it can't do so for the particular item,
     *     // request more from the upstream
     *     &#64;Override
     *     public void onSuccess(T item) {
     *         String str = item.toString();
     *         if (str.length() &lt; 2) {
     *             downstream.onSuccess(str);
     *         } else {
     *             // Maybe is expected to produce one of the onXXX events only
     *             downstream.onComplete();
     *         }
     *     }
     *
     *     // Some operators may handle the upstream's error while others
     *     // could just forward it to the downstream.
     *     &#64;Override
     *     public void onError(Throwable throwable) {
     *         downstream.onError(throwable);
     *     }
     *
     *     // When the upstream completes, usually the downstream should complete as well.
     *     &#64;Override
     *     public void onComplete() {
     *         downstream.onComplete();
     *     }
     *
     *     // Some operators may use their own resources which should be cleaned up if
     *     // the downstream disposes the flow before it completed. Operators without
     *     // resources can simply forward the dispose to the upstream.
     *     // In some cases, a disposed flag may be set by this method so that other parts
     *     // of this class may detect the dispose and stop sending events
     *     // to the downstream.
     *     &#64;Override
     *     public void dispose() {
     *         upstream.dispose();
     *     }
     *
     *     // Some operators may simply forward the call to the upstream while others
     *     // can return the disposed flag set in dispose().
     *     &#64;Override
     *     public boolean isDisposed() {
     *         return upstream.isDisposed();
     *     }
     * }
     *
     * // Step 2: Create a class that implements the MaybeOperator interface and
     * //         returns the custom consumer type from above in its apply() method.
     * //         Such class may define additional parameters to be submitted to
     * //         the custom consumer type.
     *
     * final class CustomMaybeOperator&lt;T&gt; implements MaybeOperator&lt;String&gt; {
     *     &#64;Override
     *     public MaybeObserver&lt;? super String&gt; apply(MaybeObserver&lt;? super T&gt; upstream) {
     *         return new CustomMaybeObserver&lt;T&gt;(upstream);
     *     }
     * }
     *
     * // Step 3: Apply the custom operator via lift() in a flow by creating an instance of it
     * //         or reusing an existing one.
     *
     * Maybe.just(5)
     * .lift(new CustomMaybeOperator&lt;Integer&gt;())
     * .test()
     * .assertResult("5");
     *
     * Maybe.just(15)
     * .lift(new CustomMaybeOperator&lt;Integer&gt;())
     * .test()
     * .assertResult();
     * </code></pre>
     * <p>
     * Creating custom operators can be complicated and it is recommended one consults the
     * <a href="https://github.com/ReactiveX/RxJava/wiki/Writing-operators-for-2.0">RxJava wiki: Writing operators</a> page about
     * the tools, requirements, rules, considerations and pitfalls of implementing them.
     * <p>
     * Note that implementing custom operators via this {@code lift()} method adds slightly more overhead by requiring
     * an additional allocation and indirection per assembled flows. Instead, extending the abstract {@code Maybe}
     * class and creating a {@link MaybeTransformer} with it is recommended.
     * <p>
     * Note also that it is not possible to stop the subscription phase in {@code lift()} as the {@code apply()} method
     * requires a non-{@code null} {@code MaybeObserver} instance to be returned, which is then unconditionally subscribed to
     * the current {@code Maybe}. For example, if the operator decided there is no reason to subscribe to the
     * upstream source because of some optimization possibility or a failure to prepare the operator, it still has to
     * return a {@code MaybeObserver} that should immediately dispose the upstream's {@link Disposable} in its
     * {@code onSubscribe} method. Again, using a {@code MaybeTransformer} and extending the {@code Maybe} is
     * a better option as {@link #subscribeActual} can decide to not subscribe to its upstream after all.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code lift} does not operate by default on a particular {@link Scheduler}, however, the
     *  {@code MaybeOperator} may use a {@code Scheduler} to support its own asynchronous behavior.</dd>
     * </dl>
     *
     * @param <R> the output value type
     * @param lift the {@code MaybeOperator} that receives the downstream's {@code MaybeObserver} and should return
     *               a {@code MaybeObserver} with custom behavior to be used as the consumer for the current
     *               {@code Maybe}.
     * @return the new {@code Maybe} instance
     * @throws NullPointerException if {@code lift} is {@code null}
     * @see <a href="https://github.com/ReactiveX/RxJava/wiki/Writing-operators-for-2.0">RxJava wiki: Writing operators</a>
     * @see #compose(MaybeTransformer)
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public final <@NonNull R> Maybe<R> lift(@NonNull MaybeOperator<? extends R, ? super T> lift) {
        Objects.requireNonNull(lift, "lift is null");
        return RxJavaPlugins.onAssembly(new MaybeLift<>(this, lift));
    }

    /**
     * Returns a {@code Maybe} that applies a specified function to the item emitted by the current {@code Maybe} and
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
     *            a function to apply to the item emitted by the {@code Maybe}
     * @return the new {@code Maybe} instance
     * @throws NullPointerException if {@code mapper} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/map.html">ReactiveX operators documentation: Map</a>
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public final <@NonNull R> Maybe<R> map(@NonNull Function<? super T, ? extends R> mapper) {
        Objects.requireNonNull(mapper, "mapper is null");
        return RxJavaPlugins.onAssembly(new MaybeMap<>(this, mapper));
    }

    /**
     * Maps the signal types of this {@code Maybe} into a {@link Notification} of the same kind
     * and emits it as a {@link Single}'s {@code onSuccess} value to downstream.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/materialize.v3.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code materialize} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * <p>History: 2.2.4 - experimental
     * @return the new {@code Single} instance
     * @since 3.0.0
     * @see Single#dematerialize(Function)
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Single<Notification<T>> materialize() {
        return RxJavaPlugins.onAssembly(new MaybeMaterialize<>(this));
    }

    /**
     * Flattens this {@code Maybe} and another {@link MaybeSource} into a single {@link Flowable}, without any transformation.
     * <p>
     * <img width="640" height="218" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.mergeWith.png" alt="">
     * <p>
     * You can combine items emitted by multiple {@code Maybe}s so that they appear as a single {@code Flowable}, by
     * using the {@code mergeWith} method.
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The operator honors backpressure from downstream.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code mergeWith} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param other
     *            a {@code MaybeSource} to be merged
     * @return the new {@code Flowable} instance
     * @throws NullPointerException if {@code other} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/merge.html">ReactiveX operators documentation: Merge</a>
     */
    @BackpressureSupport(BackpressureKind.FULL)
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Flowable<T> mergeWith(@NonNull MaybeSource<? extends T> other) {
        Objects.requireNonNull(other, "other is null");
        return merge(this, other);
    }

    /**
     * Wraps a {@code Maybe} to emit its item (or notify of its error) on a specified {@link Scheduler},
     * asynchronously.
     * <p>
     * <img width="640" height="183" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.observeOn.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>you specify which {@code Scheduler} this operator will use.</dd>
     * </dl>
     *
     * @param scheduler
     *            the {@code Scheduler} to notify subscribers on
     * @return the new {@code Maybe} instance that its subscribers are notified on the specified
     *         {@code Scheduler}
     * @throws NullPointerException if {@code scheduler} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/observeon.html">ReactiveX operators documentation: ObserveOn</a>
     * @see <a href="http://www.grahamlea.com/2014/07/rxjava-threading-examples/">RxJava Threading Examples</a>
     * @see #subscribeOn
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    public final Maybe<T> observeOn(@NonNull Scheduler scheduler) {
        Objects.requireNonNull(scheduler, "scheduler is null");
        return RxJavaPlugins.onAssembly(new MaybeObserveOn<>(this, scheduler));
    }

    /**
     * Filters the items emitted by the current {@code Maybe}, only emitting its success value if that
     * is an instance of the supplied {@link Class}.
     * <p>
     * <img width="640" height="291" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.ofType.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code ofType} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <U> the output type
     * @param clazz
     *            the class type to filter the items emitted by the current {@code Maybe}
     * @return the new {@code Maybe} instance
     * @throws NullPointerException if {@code clazz} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/filter.html">ReactiveX operators documentation: Filter</a>
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public final <@NonNull U> Maybe<U> ofType(@NonNull Class<U> clazz) {
        Objects.requireNonNull(clazz, "clazz is null");
        return filter(Functions.isInstanceOf(clazz)).cast(clazz);
    }

    /**
     * Calls the specified converter function during assembly time and returns its resulting value.
     * <p>
     * <img width="640" height="731" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.to.png" alt="">
     * <p>
     * This allows fluent conversion to any other type.
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code to} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * <p>History: 2.1.7 - experimental
     * @param <R> the resulting object type
     * @param converter the function that receives the current {@code Maybe} instance and returns a value
     * @return the converted value
     * @throws NullPointerException if {@code converter} is {@code null}
     * @since 2.2
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public final <R> R to(@NonNull MaybeConverter<T, ? extends R> converter) {
        return Objects.requireNonNull(converter, "converter is null").apply(this);
    }

    /**
     * Converts this {@code Maybe} into a backpressure-aware {@link Flowable} instance composing cancellation
     * through.
     * <p>
     * <img width="640" height="346" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.toFlowable.png" alt="">
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The returned {@code Flowable} honors the backpressure of the downstream.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code toFlowable} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @return the new {@code Flowable} instance
     */
    @SuppressWarnings("unchecked")
    @BackpressureSupport(BackpressureKind.FULL)
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Flowable<T> toFlowable() {
        if (this instanceof FuseToFlowable) {
            return ((FuseToFlowable<T>)this).fuseToFlowable();
        }
        return RxJavaPlugins.onAssembly(new MaybeToFlowable<>(this));
    }

    /**
     * Returns a {@link Future} representing the single value emitted by the current {@code Maybe}
     * or {@code null} if the current {@code Maybe} is empty.
     * <p>
     * <img width="640" height="292" src="https://github.com/ReactiveX/RxJava/wiki/images/rx-operators/Maybe.toFuture.png" alt="">
     * <p>
     * Cancelling the {@code Future} will cancel the subscription to the current {@code Maybe}.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code toFuture} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @return the new {@code Future} instance
     * @see <a href="http://reactivex.io/documentation/operators/to.html">ReactiveX documentation: To</a>
     * @since 3.0.0
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Future<T> toFuture() {
        return subscribeWith(new FutureMultiObserver<>());
    }

    /**
     * Converts this {@code Maybe} into an {@link Observable} instance composing disposal
     * through.
     * <p>
     * <img width="640" height="346" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.toObservable.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code toObservable} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @return the new {@code Observable} instance
     */
    @SuppressWarnings("unchecked")
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Observable<T> toObservable() {
        if (this instanceof FuseToObservable) {
            return ((FuseToObservable<T>)this).fuseToObservable();
        }
        return RxJavaPlugins.onAssembly(new MaybeToObservable<>(this));
    }

    /**
     * Converts this {@code Maybe} into a {@link Single} instance composing disposal
     * through and turning an empty {@code Maybe} into a signal of {@link NoSuchElementException}.
     * <p>
     * <img width="640" height="361" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.toSingle.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code toSingle} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @return the new {@code Single} instance
     * @see #defaultIfEmpty(Object)
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Single<T> toSingle() {
        return RxJavaPlugins.onAssembly(new MaybeToSingle<>(this, null));
    }

    /**
     * Returns a {@code Maybe} instance that if this {@code Maybe} emits an error, it will emit an {@code onComplete}
     * and swallow the throwable.
     * <p>
     * <img width="640" height="372" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.onErrorComplete.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code onErrorComplete} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @return the new {@code Maybe} instance
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Maybe<T> onErrorComplete() {
        return onErrorComplete(Functions.alwaysTrue());
    }

    /**
     * Returns a {@code Maybe} instance that if this {@code Maybe} emits an error and the predicate returns
     * {@code true}, it will emit an {@code onComplete} and swallow the throwable.
     * <p>
     * <img width="640" height="220" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.onErrorComplete.f.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code onErrorComplete} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param predicate the predicate to call when an {@link Throwable} is emitted which should return {@code true}
     * if the {@code Throwable} should be swallowed and replaced with an {@code onComplete}.
     * @return the new {@code Maybe} instance
     * @throws NullPointerException if {@code predicate} is {@code null}
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Maybe<T> onErrorComplete(@NonNull Predicate<? super Throwable> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");

        return RxJavaPlugins.onAssembly(new MaybeOnErrorComplete<>(this, predicate));
    }

    /**
     * Resumes the flow with the given {@link MaybeSource} when the current {@code Maybe} fails instead of
     * signaling the error via {@code onError}.
     * <p>
     * <img width="640" height="298" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.onErrorResumeWith.png" alt="">
     * <p>
     * You can use this to prevent errors from propagating or to supply fallback data should errors be
     * encountered.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code onErrorResumeWith} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param fallback
     *            the next {@code MaybeSource} that will take over if the current {@code Maybe} encounters
     *            an error
     * @return the new {@code Maybe} instance
     * @throws NullPointerException if {@code fallback} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/catch.html">ReactiveX operators documentation: Catch</a>
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Maybe<T> onErrorResumeWith(@NonNull MaybeSource<? extends T> fallback) {
        Objects.requireNonNull(fallback, "fallback is null");
        return onErrorResumeNext(Functions.justFunction(fallback));
    }

    /**
     * Resumes the flow with a {@link MaybeSource} returned for the failure {@link Throwable} of the current {@code Maybe} by a
     * function instead of signaling the error via {@code onError}.
     * <p>
     * <img width="640" height="298" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.onErrorResumeNext.png" alt="">
     * <p>
     * You can use this to prevent errors from propagating or to supply fallback data should errors be
     * encountered.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code onErrorResumeNext} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param fallbackSupplier
     *            a function that returns a {@code MaybeSource} that will take over if the current {@code Maybe} encounters
     *            an error
     * @return the new {@code Maybe} instance
     * @throws NullPointerException if {@code fallbackSupplier} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/catch.html">ReactiveX operators documentation: Catch</a>
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Maybe<T> onErrorResumeNext(@NonNull Function<? super Throwable, ? extends MaybeSource<? extends T>> fallbackSupplier) {
        Objects.requireNonNull(fallbackSupplier, "fallbackSupplier is null");
        return RxJavaPlugins.onAssembly(new MaybeOnErrorNext<>(this, fallbackSupplier));
    }

    /**
     * Ends the flow with a success item returned by a function for the {@link Throwable} error signaled by the current
     * {@code Maybe} instead of signaling the error via {@code onError}.
     * <p>
     * <img width="640" height="377" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.onErrorReturn.png" alt="">
     * <p>
     * You can use this to prevent errors from propagating or to supply fallback data should errors be
     * encountered.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code onErrorReturn} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param itemSupplier
     *            a function that returns a single value that will be emitted as success value
     *            the current {@code Maybe} signals an {@code onError} event
     * @return the new {@code Maybe} instance
     * @throws NullPointerException if {@code itemSupplier} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/catch.html">ReactiveX operators documentation: Catch</a>
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Maybe<T> onErrorReturn(@NonNull Function<? super Throwable, ? extends T> itemSupplier) {
        Objects.requireNonNull(itemSupplier, "itemSupplier is null");
        return RxJavaPlugins.onAssembly(new MaybeOnErrorReturn<>(this, itemSupplier));
    }

    /**
     * Ends the flow with the given success item when the current {@code Maybe} fails instead of signaling the error via {@code onError}.
     * <p>
     * <img width="640" height="377" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.onErrorReturnItem.png" alt="">
     * <p>
     * You can use this to prevent errors from propagating or to supply fallback data should errors be
     * encountered.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code onErrorReturnItem} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param item
     *            the value that is emitted as {@code onSuccess} in case the current {@code Maybe} signals an {@code onError}
     * @return the new {@code Maybe} instance
     * @throws NullPointerException if {@code item} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/catch.html">ReactiveX operators documentation: Catch</a>
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Maybe<T> onErrorReturnItem(@NonNull T item) {
        Objects.requireNonNull(item, "item is null");
        return onErrorReturn(Functions.justFunction(item));
    }

    /**
     * Nulls out references to the upstream producer and downstream {@link MaybeObserver} if
     * the sequence is terminated or downstream calls {@code dispose()}.
     * <p>
     * <img width="640" height="263" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.onTerminateDetach.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code onTerminateDetach} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @return the new {@code Maybe} instance
     * the sequence is terminated or downstream calls {@code dispose()}
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Maybe<T> onTerminateDetach() {
        return RxJavaPlugins.onAssembly(new MaybeDetach<>(this));
    }

    /**
     * Returns a {@link Flowable} that repeats the sequence of items emitted by the current {@code Maybe} indefinitely.
     * <p>
     * <img width="640" height="276" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.repeat.png" alt="">
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The operator honors downstream backpressure.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code repeat} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @return the new {@code Flowable} instance
     * @see <a href="http://reactivex.io/documentation/operators/repeat.html">ReactiveX operators documentation: Repeat</a>
     */
    @BackpressureSupport(BackpressureKind.FULL)
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Flowable<T> repeat() {
        return repeat(Long.MAX_VALUE);
    }

    /**
     * Returns a {@link Flowable} that repeats the sequence of items emitted by the current {@code Maybe} at most
     * {@code count} times.
     * <p>
     * <img width="640" height="294" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.repeat.n.png" alt="">
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>This operator honors downstream backpressure.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code repeat} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param times
     *            the number of times the current {@code Maybe} items are repeated, a count of 0 will yield an empty
     *            sequence
     * @return the new {@code Flowable} instance
     * @throws IllegalArgumentException
     *             if {@code times} is negative
     * @see <a href="http://reactivex.io/documentation/operators/repeat.html">ReactiveX operators documentation: Repeat</a>
     */
    @BackpressureSupport(BackpressureKind.FULL)
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Flowable<T> repeat(long times) {
        return toFlowable().repeat(times);
    }

    /**
     * Returns a {@link Flowable} that repeats the sequence of items emitted by the current {@code Maybe} until
     * the provided stop function returns {@code true}.
     * <p>
     * <img width="640" height="329" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.repeatUntil.png" alt="">
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>This operator honors downstream backpressure.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code repeatUntil} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param stop
     *                a boolean supplier that is called when the current {@code Flowable} completes and unless it returns
     *                {@code false}, the current {@code Flowable} is resubscribed
     * @return the new {@code Flowable} instance
     * @throws NullPointerException
     *             if {@code stop} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/repeat.html">ReactiveX operators documentation: Repeat</a>
     */
    @BackpressureSupport(BackpressureKind.FULL)
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Flowable<T> repeatUntil(@NonNull BooleanSupplier stop) {
        return toFlowable().repeatUntil(stop);
    }

    /**
     * Returns a {@link Flowable} that emits the same values as the current {@code Maybe} with the exception of an
     * {@code onComplete}. An {@code onComplete} notification from the source will result in the emission of
     * a {@code void} item to the {@code Flowable} provided as an argument to the {@code notificationHandler}
     * function. If that {@link Publisher} calls {@code onComplete} or {@code onError} then {@code repeatWhen} will
     * call {@code onComplete} or {@code onError} on the child observer. Otherwise, this operator will
     * resubscribe to the current {@code Maybe}.
     * <p>
     * <img width="640" height="562" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.repeatWhen.png" alt="">
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The operator honors downstream backpressure and expects the source {@code Publisher} to honor backpressure as well.
     *  If this expectation is violated, the operator <em>may</em> throw an {@link IllegalStateException}.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code repeatWhen} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param handler
     *            receives a {@code Publisher} of notifications with which a user can complete or error, aborting the repeat.
     * @return the new {@code Flowable} instance
     * @throws NullPointerException if {@code handler} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/repeat.html">ReactiveX operators documentation: Repeat</a>
     */
    @BackpressureSupport(BackpressureKind.FULL)
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Flowable<T> repeatWhen(@NonNull Function<? super Flowable<Object>, @NonNull ? extends Publisher<@NonNull ?>> handler) {
        return toFlowable().repeatWhen(handler);
    }

    /**
     * Returns a {@code Maybe} that mirrors the current {@code Maybe}, resubscribing to it if it calls {@code onError}
     * (infinite retry count).
     * <p>
     * <img width="640" height="393" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.retry.png" alt="">
     * <p>
     * If the current {@code Maybe} calls {@link MaybeObserver#onError}, this operator will resubscribe to the current
     * {@code Maybe} rather than propagating the {@code onError} call.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code retry} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @return the new {@code Maybe} instance
     * @see <a href="http://reactivex.io/documentation/operators/retry.html">ReactiveX operators documentation: Retry</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Maybe<T> retry() {
        return retry(Long.MAX_VALUE, Functions.alwaysTrue());
    }

    /**
     * Returns a {@code Maybe} that mirrors the current {@code Maybe}, resubscribing to it if it calls {@code onError}
     * and the predicate returns {@code true} for that specific exception and retry count.
     * <p>
     * <img width="640" height="230" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.retry.f.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code retry} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param predicate
     *            the predicate that determines if a resubscription may happen in case of a specific exception
     *            and retry count
     * @return the new {@code Maybe} instance
     * @throws NullPointerException if {@code predicate} is {@code null}
     * @see #retry()
     * @see <a href="http://reactivex.io/documentation/operators/retry.html">ReactiveX operators documentation: Retry</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Maybe<T> retry(@NonNull BiPredicate<? super Integer, ? super Throwable> predicate) {
        return toFlowable().retry(predicate).singleElement();
    }

    /**
     * Returns a {@code Maybe} that mirrors the current {@code Maybe}, resubscribing to it if it calls {@code onError}
     * up to a specified number of retries.
     * <p>
     * <img width="640" height="329" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.retry.n.png" alt="">
     * <p>
     * If the current {@code Maybe} calls {@link MaybeObserver#onError}, this operator will resubscribe to the current
     * {@code Maybe} for a maximum of {@code count} resubscriptions rather than propagating the
     * {@code onError} call.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code retry} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param times
     *            the number of times to resubscribe if the current {@code Maybe} fails
     * @return the new {@code Maybe} instance
     * @throws IllegalArgumentException if {@code times} is negative
     * @see <a href="http://reactivex.io/documentation/operators/retry.html">ReactiveX operators documentation: Retry</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Maybe<T> retry(long times) {
        return retry(times, Functions.alwaysTrue());
    }

    /**
     * Retries at most {@code times} or until the predicate returns {@code false}, whichever happens first.
     * <p>
     * <img width="640" height="259" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.retry.nf.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code retry} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param times the number of times to resubscribe if the current {@code Maybe} fails
     * @param predicate the predicate called with the failure {@link Throwable} and should return {@code true} to trigger a retry.
     * @return the new {@code Maybe} instance
     * @throws NullPointerException if {@code predicate} is {@code null}
     * @throws IllegalArgumentException if {@code times} is negative
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Maybe<T> retry(long times, @NonNull Predicate<? super Throwable> predicate) {
        return toFlowable().retry(times, predicate).singleElement();
    }

    /**
     * Retries the current {@code Maybe} if it fails and the predicate returns {@code true}.
     * <p>
     * <img width="640" height="240" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.retry.g.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code retry} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param predicate the predicate that receives the failure {@link Throwable} and should return {@code true} to trigger a retry.
     * @return the new {@code Maybe} instance
     * @throws NullPointerException if {@code predicate} is {@code null}
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Maybe<T> retry(@NonNull Predicate<? super Throwable> predicate) {
        return retry(Long.MAX_VALUE, predicate);
    }

    /**
     * Retries until the given stop function returns {@code true}.
     * <p>
     * <img width="640" height="285" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.retryUntil.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code retryUntil} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param stop the function that should return {@code true} to stop retrying
     * @return the new {@code Maybe} instance
     * @throws NullPointerException if {@code stop} is {@code null}
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Maybe<T> retryUntil(@NonNull BooleanSupplier stop) {
        Objects.requireNonNull(stop, "stop is null");
        return retry(Long.MAX_VALUE, Functions.predicateReverseFor(stop));
    }

    /**
     * Returns a {@code Maybe} that emits the same values as the current {@code Maybe} with the exception of an
     * {@code onError}. An {@code onError} notification from the source will result in the emission of a
     * {@link Throwable} item to the {@link Flowable} provided as an argument to the {@code notificationHandler}
     * function. If the returned {@link Publisher} calls {@code onComplete} or {@code onError} then {@code retry} will call
     * {@code onComplete} or {@code onError} on the child subscription. Otherwise, this operator will
     * resubscribe to the current {@code Maybe}.
     * <p>
     * <img width="640" height="405" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.retryWhen.png" alt="">
     * <p>
     * Example:
     *
     * This retries 3 times, each time incrementing the number of seconds it waits.
     *
     * <pre><code>
     *  Maybe.create((MaybeEmitter&lt;? super String&gt; s) -&gt; {
     *      System.out.println("subscribing");
     *      s.onError(new RuntimeException("always fails"));
     *  }, BackpressureStrategy.BUFFER).retryWhen(attempts -&gt; {
     *      return attempts.zipWith(Publisher.range(1, 3), (n, i) -&gt; i).flatMap(i -&gt; {
     *          System.out.println("delay retry by " + i + " second(s)");
     *          return Flowable.timer(i, TimeUnit.SECONDS);
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
     * <p>
     * Note that the inner {@code Publisher} returned by the handler function should signal
     * either {@code onNext}, {@code onError} or {@code onComplete} in response to the received
     * {@code Throwable} to indicate the operator should retry or terminate. If the upstream to
     * the operator is asynchronous, signalling {@code onNext} followed by {@code onComplete} immediately may
     * result in the sequence to be completed immediately. Similarly, if this inner
     * {@code Publisher} signals {@code onError} or {@code onComplete} while the upstream is
     * active, the sequence is terminated with the same signal immediately.
     * <p>
     * The following example demonstrates how to retry an asynchronous source with a delay:
     * <pre><code>
     * Maybe.timer(1, TimeUnit.SECONDS)
     *     .doOnSubscribe(s -&gt; System.out.println("subscribing"))
     *     .map(v -&gt; { throw new RuntimeException(); })
     *     .retryWhen(errors -&gt; {
     *         AtomicInteger counter = new AtomicInteger();
     *         return errors
     *                   .takeWhile(e -&gt; counter.getAndIncrement() != 3)
     *                   .flatMap(e -&gt; {
     *                       System.out.println("delay retry by " + counter.get() + " second(s)");
     *                       return Flowable.timer(counter.get(), TimeUnit.SECONDS);
     *                   });
     *     })
     *     .blockingGet();
     * </code></pre>
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code retryWhen} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param handler
     *            receives a {@code Publisher} of notifications with which a user can complete or error, aborting the
     *            retry
     * @return the new {@code Maybe} instance
     * @throws NullPointerException if {@code handler} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/retry.html">ReactiveX operators documentation: Retry</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Maybe<T> retryWhen(
            @NonNull Function<? super Flowable<Throwable>, @NonNull ? extends Publisher<@NonNull ?>> handler) {
        return toFlowable().retryWhen(handler).singleElement();
    }

    /**
     * Wraps the given {@link MaybeObserver}, catches any {@link RuntimeException}s thrown by its
     * {@link MaybeObserver#onSubscribe(Disposable)}, {@link MaybeObserver#onSuccess(Object)},
     * {@link MaybeObserver#onError(Throwable)} or {@link MaybeObserver#onComplete()} methods
     * and routes those to the global error handler via {@link RxJavaPlugins#onError(Throwable)}.
     * <p>
     * By default, the {@code Maybe} protocol forbids the {@code onXXX} methods to throw, but some
     * {@code MaybeObserver} implementation may do it anyway, causing undefined behavior in the
     * upstream. This method and the underlying safe wrapper ensures such misbehaving consumers don't
     * disrupt the protocol.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code safeSubscribe} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param observer the potentially misbehaving {@code MaybeObserver}
     * @throws NullPointerException if {@code observer} is {@code null}
     * @see #subscribe(Consumer,Consumer, Action)
     * @since 3.0.0
     */
    @SchedulerSupport(SchedulerSupport.NONE)
    public final void safeSubscribe(@NonNull MaybeObserver<? super T> observer) {
        Objects.requireNonNull(observer, "observer is null");
        subscribe(new SafeMaybeObserver<>(observer));
    }

    /**
     * Returns a {@link Flowable} which first runs the other {@link CompletableSource}
     * then the current {@code Maybe} if the other completed normally.
     * <p>
     * <img width="640" height="268" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.startWith.c.png" alt="">
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The returned {@code Flowable} honors the backpressure of the downstream consumer.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code startWith} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param other the other {@code CompletableSource} to run first
     * @return the new {@code Flowable} instance
     * @throws NullPointerException if {@code other} is {@code null}
     * @since 3.0.0
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    @BackpressureSupport(BackpressureKind.FULL)
    public final Flowable<T> startWith(@NonNull CompletableSource other) {
        Objects.requireNonNull(other, "other is null");
        return Flowable.concat(Completable.wrap(other).<T>toFlowable(), toFlowable());
    }

    /**
     * Returns a {@link Flowable} which first runs the other {@link SingleSource}
     * then the current {@code Maybe} if the other succeeded normally.
     * <p>
     * <img width="640" height="237" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.startWith.s.png" alt="">
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The returned {@code Flowable} honors the backpressure of the downstream consumer.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code startWith} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param other the other {@code SingleSource} to run first
     * @return the new {@code Flowable} instance
     * @throws NullPointerException if {@code other} is {@code null}
     * @since 3.0.0
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    @BackpressureSupport(BackpressureKind.FULL)
    public final Flowable<T> startWith(@NonNull SingleSource<T> other) {
        Objects.requireNonNull(other, "other is null");
        return Flowable.concat(Single.wrap(other).toFlowable(), toFlowable());
    }

    /**
     * Returns a {@link Flowable} which first runs the other {@link MaybeSource}
     * then the current {@code Maybe} if the other succeeded or completed normally.
     * <p>
     * <img width="640" height="178" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.startWith.m.png" alt="">
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The returned {@code Flowable} honors the backpressure of the downstream consumer.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code startWith} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param other the other {@code MaybeSource} to run first
     * @return the new {@code Flowable} instance
     * @throws NullPointerException if {@code other} is {@code null}
     * @since 3.0.0
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    @BackpressureSupport(BackpressureKind.FULL)
    public final Flowable<T> startWith(@NonNull MaybeSource<T> other) {
        Objects.requireNonNull(other, "other is null");
        return Flowable.concat(Maybe.wrap(other).toFlowable(), toFlowable());
    }

    /**
     * Returns an {@link Observable} which first delivers the events
     * of the other {@link ObservableSource} then runs the current {@code Maybe}.
     * <p>
     * <img width="640" height="179" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.startWith.o.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code startWith} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param other the other {@code ObservableSource} to run first
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code other} is {@code null}
     * @since 3.0.0
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Observable<T> startWith(@NonNull ObservableSource<T> other) {
        Objects.requireNonNull(other, "other is null");
        return Observable.wrap(other).concatWith(this.toObservable());
    }

    /**
     * Returns a {@link Flowable} which first delivers the events
     * of the other {@link Publisher} then runs the current {@code Maybe}.
     * <p>
     * <img width="640" height="179" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.startWith.p.png" alt="">
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The returned {@code Flowable} honors the backpressure of the downstream consumer
     *  and expects the other {@code Publisher} to honor it as well.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code startWith} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param other the other {@code Publisher} to run first
     * @return the new {@code Flowable} instance
     * @throws NullPointerException if {@code other} is {@code null}
     * @since 3.0.0
     */
    @CheckReturnValue
    @NonNull
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Flowable<T> startWith(@NonNull Publisher<T> other) {
        Objects.requireNonNull(other, "other is null");
        return toFlowable().startWith(other);
    }

    /**
     * Subscribes to a {@code Maybe} and ignores {@code onSuccess} and {@code onComplete} emissions.
     * <p>
     * If the {@code Maybe} emits an error, it is wrapped into an
     * {@link io.reactivex.rxjava3.exceptions.OnErrorNotImplementedException OnErrorNotImplementedException}
     * and routed to the {@link RxJavaPlugins#onError(Throwable)} handler.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code subscribe} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @return the new {@link Disposable} instance that can be used for disposing the subscription at any time
     * @see <a href="http://reactivex.io/documentation/operators/subscribe.html">ReactiveX operators documentation: Subscribe</a>
     * @see #subscribe(Consumer, Consumer, Action, DisposableContainer)
     */
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Disposable subscribe() {
        return subscribe(Functions.emptyConsumer(), Functions.ON_ERROR_MISSING, Functions.EMPTY_ACTION);
    }

    /**
     * Subscribes to a {@code Maybe} and provides a callback to handle the items it emits.
     * <p>
     * If the {@code Maybe} emits an error, it is wrapped into an
     * {@link io.reactivex.rxjava3.exceptions.OnErrorNotImplementedException OnErrorNotImplementedException}
     * and routed to the {@link RxJavaPlugins#onError(Throwable)} handler.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code subscribe} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param onSuccess
     *             the {@code Consumer<T>} you have designed to accept a success value from the {@code Maybe}
     * @return the new {@link Disposable} instance that can be used for disposing the subscription at any time
     * @throws NullPointerException
     *             if {@code onSuccess} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/subscribe.html">ReactiveX operators documentation: Subscribe</a>
     * @see #subscribe(Consumer, Consumer, Action, DisposableContainer)
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Disposable subscribe(@NonNull Consumer<? super T> onSuccess) {
        return subscribe(onSuccess, Functions.ON_ERROR_MISSING, Functions.EMPTY_ACTION);
    }

    /**
     * Subscribes to a {@code Maybe} and provides callbacks to handle the items it emits and any error
     * notification it issues.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code subscribe} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param onSuccess
     *             the {@code Consumer<T>} you have designed to accept a success value from the {@code Maybe}
     * @param onError
     *             the {@code Consumer<Throwable>} you have designed to accept any error notification from the
     *             {@code Maybe}
     * @return the new {@link Disposable} instance that can be used for disposing the subscription at any time
     * @throws NullPointerException
     *             if {@code onSuccess} is {@code null}, or
     *             if {@code onError} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/subscribe.html">ReactiveX operators documentation: Subscribe</a>
     * @see #subscribe(Consumer, Consumer, Action, DisposableContainer)
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Disposable subscribe(@NonNull Consumer<? super T> onSuccess, @NonNull Consumer<? super Throwable> onError) {
        return subscribe(onSuccess, onError, Functions.EMPTY_ACTION);
    }

    /**
     * Subscribes to a {@code Maybe} and provides callbacks to handle the items it emits and any error or
     * completion notification it issues.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code subscribe} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param onSuccess
     *             the {@code Consumer<T>} you have designed to accept a success value from the {@code Maybe}
     * @param onError
     *             the {@code Consumer<Throwable>} you have designed to accept any error notification from the
     *             {@code Maybe}
     * @param onComplete
     *             the {@link Action} you have designed to accept a completion notification from the
     *             {@code Maybe}
     * @return the new {@link Disposable} instance that can be used for disposing the subscription at any time
     * @throws NullPointerException
     *             if {@code onSuccess}, {@code onError} or
     *             {@code onComplete} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/subscribe.html">ReactiveX operators documentation: Subscribe</a>
     * @see #subscribe(Consumer, Consumer, Action, DisposableContainer)
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Disposable subscribe(@NonNull Consumer<? super T> onSuccess, @NonNull Consumer<? super Throwable> onError,
            @NonNull Action onComplete) {
        Objects.requireNonNull(onSuccess, "onSuccess is null");
        Objects.requireNonNull(onError, "onError is null");
        Objects.requireNonNull(onComplete, "onComplete is null");
        return subscribeWith(new MaybeCallbackObserver<>(onSuccess, onError, onComplete));
    }

    /**
     * Wraps the given onXXX callbacks into a {@link Disposable} {@link MaybeObserver},
     * adds it to the given {@link DisposableContainer} and ensures, that if the upstream
     * terminates or this particular {@code Disposable} is disposed, the {@code MaybeObserver} is removed
     * from the given composite.
     * <p>
     * The {@code MaybeObserver} will be removed after the callback for the terminal event has been invoked.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code subscribe} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param onSuccess the callback for upstream items
     * @param onError the callback for an upstream error
     * @param onComplete the callback for an upstream completion without any value or error
     * @param container the {@code DisposableContainer} (such as {@link CompositeDisposable}) to add and remove the
     *                  created {@code Disposable} {@code MaybeObserver}
     * @return the {@code Disposable} that allows disposing the particular subscription.
     * @throws NullPointerException
     *             if {@code onSuccess}, {@code onError},
     *             {@code onComplete} or {@code container} is {@code null}
     * @since 3.1.0
     */
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Disposable subscribe(
            @NonNull Consumer<? super T> onSuccess,
            @NonNull Consumer<? super Throwable> onError,
            @NonNull Action onComplete,
            @NonNull DisposableContainer container) {
        Objects.requireNonNull(onSuccess, "onSuccess is null");
        Objects.requireNonNull(onError, "onError is null");
        Objects.requireNonNull(onComplete, "onComplete is null");
        Objects.requireNonNull(container, "container is null");

        DisposableAutoReleaseMultiObserver<T> observer = new DisposableAutoReleaseMultiObserver<>(
                container, onSuccess, onError, onComplete);
        container.add(observer);
        subscribe(observer);
        return observer;
    }

    @SchedulerSupport(SchedulerSupport.NONE)
    @Override
    public final void subscribe(@NonNull MaybeObserver<? super T> observer) {
        Objects.requireNonNull(observer, "observer is null");

        observer = RxJavaPlugins.onSubscribe(this, observer);

        Objects.requireNonNull(observer, "The RxJavaPlugins.onSubscribe hook returned a null MaybeObserver. Please check the handler provided to RxJavaPlugins.setOnMaybeSubscribe for invalid null returns. Further reading: https://github.com/ReactiveX/RxJava/wiki/Plugins");

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
     * Implement this method in subclasses to handle the incoming {@link MaybeObserver}s.
     * <p>There is no need to call any of the plugin hooks on the current {@code Maybe} instance or
     * the {@code MaybeObserver}; all hooks and basic safeguards have been
     * applied by {@link #subscribe(MaybeObserver)} before this method gets called.
     * @param observer the {@code MaybeObserver} to handle, not {@code null}
     */
    protected abstract void subscribeActual(@NonNull MaybeObserver<? super T> observer);

    /**
     * Asynchronously subscribes subscribers to this {@code Maybe} on the specified {@link Scheduler}.
     * <p>
     * <img width="640" height="753" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.subscribeOn.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>you specify which {@code Scheduler} this operator will use.</dd>
     * </dl>
     *
     * @param scheduler
     *            the {@code Scheduler} to perform subscription actions on
     * @return the new {@code Maybe} instance
     * @throws NullPointerException if {@code scheduler} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/subscribeon.html">ReactiveX operators documentation: SubscribeOn</a>
     * @see <a href="http://www.grahamlea.com/2014/07/rxjava-threading-examples/">RxJava Threading Examples</a>
     * @see #observeOn
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    public final Maybe<T> subscribeOn(@NonNull Scheduler scheduler) {
        Objects.requireNonNull(scheduler, "scheduler is null");
        return RxJavaPlugins.onAssembly(new MaybeSubscribeOn<>(this, scheduler));
    }

    /**
     * Subscribes a given {@link MaybeObserver} (subclass) to this {@code Maybe} and returns the given
     * {@code MaybeObserver} as is.
     * <p>Usage example:
     * <pre><code>
     * Maybe&lt;Integer&gt; source = Maybe.just(1);
     * CompositeDisposable composite = new CompositeDisposable();
     *
     * DisposableMaybeObserver&lt;Integer&gt; ds = new DisposableMaybeObserver&lt;&gt;() {
     *     // ...
     * };
     *
     * composite.add(source.subscribeWith(ds));
     * </code></pre>
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code subscribeWith} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <E> the type of the {@code MaybeObserver} to use and return
     * @param observer the {@code MaybeObserver} (subclass) to use and return, not {@code null}
     * @return the input {@code observer}
     * @throws NullPointerException if {@code observer} is {@code null}
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <@NonNull E extends MaybeObserver<? super T>> E subscribeWith(E observer) {
        subscribe(observer);
        return observer;
    }

    /**
     * Returns a {@code Maybe} that emits the items emitted by the current {@code Maybe} or the items of an alternate
     * {@link MaybeSource} if the current {@code Maybe} is empty.
     * <p>
     * <img width="640" height="222" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.switchIfEmpty.m.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code switchIfEmpty} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param other
     *              the alternate {@code MaybeSource} to subscribe to if the main does not emit any items
     * @return the new {@code Maybe} instance
     * @throws NullPointerException if {@code other} is {@code null}
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Maybe<T> switchIfEmpty(@NonNull MaybeSource<? extends T> other) {
        Objects.requireNonNull(other, "other is null");
        return RxJavaPlugins.onAssembly(new MaybeSwitchIfEmpty<>(this, other));
    }

    /**
     * Returns a {@link Single} that emits the items emitted by the current {@code Maybe} or the item of an alternate
     * {@link SingleSource} if the current {@code Maybe} is empty.
     * <p>
     * <img width="640" height="312" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.switchIfEmpty.s.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code switchIfEmpty} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * <p>History: 2.1.4 - experimental
     * @param other
     *              the alternate {@code SingleSource} to subscribe to if the main does not emit any items
     * @return the new {@code Single} instance
     * @throws NullPointerException if {@code other} is {@code null}
     * @since 2.2
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Single<T> switchIfEmpty(@NonNull SingleSource<? extends T> other) {
        Objects.requireNonNull(other, "other is null");
        return RxJavaPlugins.onAssembly(new MaybeSwitchIfEmptySingle<>(this, other));
    }

    /**
     * Returns a {@code Maybe} that emits the items emitted by the current {@code Maybe} until a second {@link MaybeSource}
     * emits an item.
     * <p>
     * <img width="640" height="219" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.takeUntil.m.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code takeUntil} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param other
     *            the {@code MaybeSource} whose first emitted item will cause {@code takeUntil} to stop emitting items
     *            from the current {@code Maybe}
     * @param <U>
     *            the type of items emitted by {@code other}
     * @return the new {@code Maybe} instance
     * @throws NullPointerException if {@code other} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/takeuntil.html">ReactiveX operators documentation: TakeUntil</a>
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public final <@NonNull U> Maybe<T> takeUntil(@NonNull MaybeSource<U> other) {
        Objects.requireNonNull(other, "other is null");
        return RxJavaPlugins.onAssembly(new MaybeTakeUntilMaybe<>(this, other));
    }

    /**
     * Returns a {@code Maybe} that emits the item emitted by the current {@code Maybe} until a second {@link Publisher}
     * emits an item.
     * <p>
     * <img width="640" height="199" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.takeUntil.p.png" alt="">
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The {@code Publisher} is consumed in an unbounded fashion and is cancelled after the first item
     *  emitted.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code takeUntil} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param other
     *            the {@code Publisher} whose first emitted item will cause {@code takeUntil} to stop emitting items
     *            from the source {@code Publisher}
     * @param <U>
     *            the type of items emitted by {@code other}
     * @return the new {@code Maybe} instance
     * @throws NullPointerException if {@code other} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/takeuntil.html">ReactiveX operators documentation: TakeUntil</a>
     */
    @BackpressureSupport(BackpressureKind.UNBOUNDED_IN)
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public final <@NonNull U> Maybe<T> takeUntil(@NonNull Publisher<U> other) {
        Objects.requireNonNull(other, "other is null");
        return RxJavaPlugins.onAssembly(new MaybeTakeUntilPublisher<>(this, other));
    }

    /**
     * Measures the time (in milliseconds) between the subscription and success item emission
     * of the current {@code Maybe} and signals it as a tuple ({@link Timed})
     * success value.
     * <p>
     * <img width="640" height="352" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.timeInterval.png" alt="">
     * <p>
     * If the current {@code Maybe} is empty or fails, the resulting {@code Maybe} will
     * pass along the signals to the downstream. To measure the time to termination,
     * use {@link #materialize()} and apply {@link Single#timeInterval()}.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code timeInterval} uses the {@code computation} {@link Scheduler}
     *  for determining the current time upon subscription and upon receiving the
     *  success item from the current {@code Maybe}.</dd>
     * </dl>
     * @return the new {@code Maybe} instance
     * @since 3.0.0
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.COMPUTATION)
    public final Maybe<Timed<T>> timeInterval() {
        return timeInterval(TimeUnit.MILLISECONDS, Schedulers.computation());
    }

    /**
     * Measures the time (in milliseconds) between the subscription and success item emission
     * of the current {@code Maybe} and signals it as a tuple ({@link Timed})
     * success value.
     * <p>
     * <img width="640" height="355" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.timeInterval.s.png" alt="">
     * <p>
     * If the current {@code Maybe} is empty or fails, the resulting {@code Maybe} will
     * pass along the signals to the downstream. To measure the time to termination,
     * use {@link #materialize()} and apply {@link Single#timeInterval(Scheduler)}.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code timeInterval} uses the provided {@link Scheduler}
     *  for determining the current time upon subscription and upon receiving the
     *  success item from the current {@code Maybe}.</dd>
     * </dl>
     * @param scheduler the {@code Scheduler} used for providing the current time
     * @return the new {@code Maybe} instance
     * @throws NullPointerException if {@code scheduler} is {@code null}
     * @since 3.0.0
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    public final Maybe<Timed<T>> timeInterval(@NonNull Scheduler scheduler) {
        return timeInterval(TimeUnit.MILLISECONDS, scheduler);
    }

    /**
     * Measures the time between the subscription and success item emission
     * of the current {@code Maybe} and signals it as a tuple ({@link Timed})
     * success value.
     * <p>
     * <img width="640" height="352" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.timeInterval.png" alt="">
     * <p>
     * If the current {@code Maybe} is empty or fails, the resulting {@code Maybe} will
     * pass along the signals to the downstream. To measure the time to termination,
     * use {@link #materialize()} and apply {@link Single#timeInterval(TimeUnit)}.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code timeInterval} uses the {@code computation} {@link Scheduler}
     *  for determining the current time upon subscription and upon receiving the
     *  success item from the current {@code Maybe}.</dd>
     * </dl>
     * @param unit the time unit for measurement
     * @return the new {@code Maybe} instance
     * @throws NullPointerException if {@code unit} is {@code null}
     * @since 3.0.0
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.COMPUTATION)
    public final Maybe<Timed<T>> timeInterval(@NonNull TimeUnit unit) {
        return timeInterval(unit, Schedulers.computation());
    }

    /**
     * Measures the time between the subscription and success item emission
     * of the current {@code Maybe} and signals it as a tuple ({@link Timed})
     * success value.
     * <p>
     * <img width="640" height="355" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.timeInterval.s.png" alt="">
     * <p>
     * If the current {@code Maybe} is empty or fails, the resulting {@code Maybe} will
     * pass along the signals to the downstream. To measure the time to termination,
     * use {@link #materialize()} and apply {@link Single#timeInterval(TimeUnit, Scheduler)}.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code timeInterval} uses the provided {@link Scheduler}
     *  for determining the current time upon subscription and upon receiving the
     *  success item from the current {@code Maybe}.</dd>
     * </dl>
     * @param unit the time unit for measurement
     * @param scheduler the {@code Scheduler} used for providing the current time
     * @return the new {@code Maybe} instance
     * @throws NullPointerException if {@code unit} or {@code scheduler} is {@code null}
     * @since 3.0.0
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    public final Maybe<Timed<T>> timeInterval(@NonNull TimeUnit unit, @NonNull Scheduler scheduler) {
        Objects.requireNonNull(unit, "unit is null");
        Objects.requireNonNull(scheduler, "scheduler is null");
        return RxJavaPlugins.onAssembly(new MaybeTimeInterval<>(this, unit, scheduler, true));
    }

    /**
     * Combines the success value from the current {@code Maybe} with the current time (in milliseconds) of
     * its reception, using the {@code computation} {@link Scheduler} as time source,
     * then signals them as a {@link Timed} instance.
     * <p>
     * <img width="640" height="352" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.timestamp.png" alt="">
     * <p>
     * If the current {@code Maybe} is empty or fails, the resulting {@code Maybe} will
     * pass along the signals to the downstream. To measure the time to termination,
     * use {@link #materialize()} and apply {@link Single#timestamp()}.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code timestamp} uses the {@code computation} {@code Scheduler}
     *  for determining the current time upon receiving the
     *  success item from the current {@code Maybe}.</dd>
     * </dl>
     * @return the new {@code Maybe} instance
     * @since 3.0.0
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.COMPUTATION)
    public final Maybe<Timed<T>> timestamp() {
        return timestamp(TimeUnit.MILLISECONDS, Schedulers.computation());
    }

    /**
     * Combines the success value from the current {@code Maybe} with the current time (in milliseconds) of
     * its reception, using the given {@link Scheduler} as time source,
     * then signals them as a {@link Timed} instance.
     * <p>
     * <img width="640" height="355" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.timestamp.s.png" alt="">
     * <p>
     * If the current {@code Maybe} is empty or fails, the resulting {@code Maybe} will
     * pass along the signals to the downstream. To measure the time to termination,
     * use {@link #materialize()} and apply {@link Single#timestamp(Scheduler)}.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code timestamp} uses the provided {@code Scheduler}
     *  for determining the current time upon receiving the
     *  success item from the current {@code Maybe}.</dd>
     * </dl>
     * @param scheduler the {@code Scheduler} used for providing the current time
     * @return the new {@code Maybe} instance
     * @throws NullPointerException if {@code scheduler} is {@code null}
     * @since 3.0.0
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    public final Maybe<Timed<T>> timestamp(@NonNull Scheduler scheduler) {
        return timestamp(TimeUnit.MILLISECONDS, scheduler);
    }

    /**
     * Combines the success value from the current {@code Maybe} with the current time of
     * its reception, using the {@code computation} {@link Scheduler} as time source,
     * then signals it as a {@link Timed} instance.
     * <p>
     * <img width="640" height="352" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.timestamp.png" alt="">
     * <p>
     * If the current {@code Maybe} is empty or fails, the resulting {@code Maybe} will
     * pass along the signals to the downstream. To measure the time to termination,
     * use {@link #materialize()} and apply {@link Single#timestamp(TimeUnit)}.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code timestamp} uses the {@code computation} {@code Scheduler},
     *  for determining the current time upon receiving the
     *  success item from the current {@code Maybe}.</dd>
     * </dl>
     * @param unit the time unit for measurement
     * @return the new {@code Maybe} instance
     * @throws NullPointerException if {@code unit} is {@code null}
     * @since 3.0.0
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.COMPUTATION)
    public final Maybe<Timed<T>> timestamp(@NonNull TimeUnit unit) {
        return timestamp(unit, Schedulers.computation());
    }

    /**
     * Combines the success value from the current {@code Maybe} with the current time of
     * its reception, using the given {@link Scheduler} as time source,
     * then signals it as a {@link Timed} instance.
     * <p>
     * <img width="640" height="355" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.timestamp.s.png" alt="">
     * <p>
     * If the current {@code Maybe} is empty or fails, the resulting {@code Maybe} will
     * pass along the signals to the downstream. To measure the time to termination,
     * use {@link #materialize()} and apply {@link Single#timestamp(TimeUnit, Scheduler)}.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code timestamp} uses the provided {@code Scheduler},
     *  which is used for determining the current time upon receiving the
     *  success item from the current {@code Maybe}.</dd>
     * </dl>
     * @param unit the time unit for measurement
     * @param scheduler the {@code Scheduler} used for providing the current time
     * @return the new {@code Maybe} instance
     * @throws NullPointerException if {@code unit} or {@code scheduler} is {@code null}
     * @since 3.0.0
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    public final Maybe<Timed<T>> timestamp(@NonNull TimeUnit unit, @NonNull Scheduler scheduler) {
        Objects.requireNonNull(unit, "unit is null");
        Objects.requireNonNull(scheduler, "scheduler is null");
        return RxJavaPlugins.onAssembly(new MaybeTimeInterval<>(this, unit, scheduler, false));
    }

    /**
     * Returns a {@code Maybe} that mirrors the current {@code Maybe} but applies a timeout policy for each emitted
     * item. If the next item isn't emitted within the specified timeout duration starting from its predecessor,
     * the resulting {@code Maybe} terminates and notifies {@link MaybeObserver}s of a {@link TimeoutException}.
     * <p>
     * <img width="640" height="261" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.timeout.t.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This version of {@code timeout} operates by default on the {@code computation} {@link Scheduler}.</dd>
     * </dl>
     *
     * @param timeout
     *            maximum duration between emitted items before a timeout occurs
     * @param unit
     *            the unit of time that applies to the {@code timeout} argument.
     * @return the new {@code Maybe} instance
     * @throws NullPointerException if {@code unit} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/timeout.html">ReactiveX operators documentation: Timeout</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.COMPUTATION)
    @NonNull
    public final Maybe<T> timeout(long timeout, @NonNull TimeUnit unit) {
        return timeout(timeout, unit, Schedulers.computation());
    }

    /**
     * Returns a {@code Maybe} that mirrors the current {@code Maybe} but applies a timeout policy for each emitted
     * item. If the next item isn't emitted within the specified timeout duration starting from its predecessor,
     * the current {@code Maybe} is disposed and resulting {@code Maybe} begins instead to mirror a fallback {@link MaybeSource}.
     * <p>
     * <img width="640" height="226" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.timeout.tm.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This version of {@code timeout} operates by default on the {@code computation} {@link Scheduler}.</dd>
     * </dl>
     *
     * @param timeout
     *            maximum duration between items before a timeout occurs
     * @param unit
     *            the unit of time that applies to the {@code timeout} argument
     * @param fallback
     *            the fallback {@code MaybeSource} to use in case of a timeout
     * @return the new {@code Maybe} instance
     * @throws NullPointerException if {@code unit} or {@code fallback} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/timeout.html">ReactiveX operators documentation: Timeout</a>
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.COMPUTATION)
    public final Maybe<T> timeout(long timeout, @NonNull TimeUnit unit, @NonNull MaybeSource<? extends T> fallback) {
        Objects.requireNonNull(fallback, "fallback is null");
        return timeout(timeout, unit, Schedulers.computation(), fallback);
    }

    /**
     * Returns a {@code Maybe} that mirrors the current {@code Maybe} but applies a timeout policy for each emitted
     * item using a specified {@link Scheduler}. If the next item isn't emitted within the specified timeout duration
     * starting from its predecessor, the current {@code Maybe} is disposed and resulting {@code Maybe} begins instead
     * to mirror a fallback {@link MaybeSource}.
     * <p>
     * <img width="640" height="227" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.timeout.tsm.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>You specify which {@code Scheduler} this operator will use.</dd>
     * </dl>
     *
     * @param timeout
     *            maximum duration between items before a timeout occurs
     * @param unit
     *            the unit of time that applies to the {@code timeout} argument
     * @param fallback
     *            the {@code MaybeSource} to use as the fallback in case of a timeout
     * @param scheduler
     *            the {@code Scheduler} to run the timeout timers on
     * @return the new {@code Maybe} instance
     * @throws NullPointerException if {@code fallback}, {@code unit} or {@code scheduler} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/timeout.html">ReactiveX operators documentation: Timeout</a>
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    public final Maybe<T> timeout(long timeout, @NonNull TimeUnit unit, @NonNull Scheduler scheduler, @NonNull MaybeSource<? extends T> fallback) {
        Objects.requireNonNull(fallback, "fallback is null");
        return timeout(timer(timeout, unit, scheduler), fallback);
    }

    /**
     * Returns a {@code Maybe} that mirrors the current {@code Maybe} but applies a timeout policy for each emitted
     * item, where this policy is governed on a specified {@link Scheduler}. If the next item isn't emitted within the
     * specified timeout duration starting from its predecessor, the resulting {@code Maybe} terminates and
     * notifies {@link MaybeObserver}s of a {@link TimeoutException}.
     * <p>
     * <img width="640" height="261" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.timeout.ts.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>You specify which {@code Scheduler} this operator will use.</dd>
     * </dl>
     *
     * @param timeout
     *            maximum duration between items before a timeout occurs
     * @param unit
     *            the unit of time that applies to the {@code timeout} argument
     * @param scheduler
     *            the {@code Scheduler} to run the timeout timers on
     * @return the new {@code Maybe} instance
     * @see <a href="http://reactivex.io/documentation/operators/timeout.html">ReactiveX operators documentation: Timeout</a>
     * @throws NullPointerException if {@code unit} or {@code scheduler} is {@code null}
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    @NonNull
    public final Maybe<T> timeout(long timeout, @NonNull TimeUnit unit, @NonNull Scheduler scheduler) {
        return timeout(timer(timeout, unit, scheduler));
    }

    /**
     * If the current {@code Maybe} didn't signal an event before the {@code timeoutIndicator} {@link MaybeSource} signals, a
     * {@link TimeoutException} is signaled instead.
     * <p>
     * <img width="640" height="235" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.timeout.m.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code timeout} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <U> the value type of the
     * @param timeoutIndicator the {@code MaybeSource} that indicates the timeout by signaling {@code onSuccess}
     * or {@code onComplete}.
     * @return the new {@code Maybe} instance
     * @throws NullPointerException if {@code timeoutIndicator} is {@code null}
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public final <@NonNull U> Maybe<T> timeout(@NonNull MaybeSource<U> timeoutIndicator) {
        Objects.requireNonNull(timeoutIndicator, "timeoutIndicator is null");
        return RxJavaPlugins.onAssembly(new MaybeTimeoutMaybe<>(this, timeoutIndicator, null));
    }

    /**
     * If the current {@code Maybe} didn't signal an event before the {@code timeoutIndicator} {@link MaybeSource} signals,
     * the current {@code Maybe} is disposed and the {@code fallback} {@code MaybeSource} subscribed to
     * as a continuation.
     * <p>
     * <img width="640" height="194" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.timeout.mm.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code timeout} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <U> the value type of the
     * @param timeoutIndicator the {@code MaybeSource} that indicates the timeout by signaling {@code onSuccess}
     * or {@code onComplete}.
     * @param fallback the {@code MaybeSource} that is subscribed to if the current {@code Maybe} times out
     * @return the new {@code Maybe} instance
     * @throws NullPointerException if {@code timeoutIndicator} or {@code fallback} is {@code null}
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public final <@NonNull U> Maybe<T> timeout(@NonNull MaybeSource<U> timeoutIndicator, @NonNull MaybeSource<? extends T> fallback) {
        Objects.requireNonNull(timeoutIndicator, "timeoutIndicator is null");
        Objects.requireNonNull(fallback, "fallback is null");
        return RxJavaPlugins.onAssembly(new MaybeTimeoutMaybe<>(this, timeoutIndicator, fallback));
    }

    /**
     * If the current {@code Maybe} source didn't signal an event before the {@code timeoutIndicator} {@link Publisher} signals, a
     * {@link TimeoutException} is signaled instead.
     * <p>
     * <img width="640" height="212" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.timeout.p.png" alt="">
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The {@code timeoutIndicator} {@code Publisher} is consumed in an unbounded manner and
     *  is cancelled after its first item.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code timeout} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <U> the value type of the
     * @param timeoutIndicator the {@code Publisher} that indicates the timeout by signaling {@code onSuccess}
     * or {@code onComplete}.
     * @return the new {@code Maybe} instance
     * @throws NullPointerException if {@code timeoutIndicator} is {@code null}
     */
    @BackpressureSupport(BackpressureKind.UNBOUNDED_IN)
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public final <@NonNull U> Maybe<T> timeout(@NonNull Publisher<U> timeoutIndicator) {
        Objects.requireNonNull(timeoutIndicator, "timeoutIndicator is null");
        return RxJavaPlugins.onAssembly(new MaybeTimeoutPublisher<>(this, timeoutIndicator, null));
    }

    /**
     * If the current {@code Maybe} didn't signal an event before the {@code timeoutIndicator} {@link Publisher} signals,
     * the current {@code Maybe} is disposed and the {@code fallback} {@link MaybeSource} subscribed to
     * as a continuation.
     * <p>
     * <img width="640" height="169" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.timeout.pm.png" alt="">
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The {@code timeoutIndicator} {@code Publisher} is consumed in an unbounded manner and
     *  is cancelled after its first item.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code timeout} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <U> the value type of the
     * @param timeoutIndicator the {@code MaybeSource} that indicates the timeout by signaling {@code onSuccess}
     * or {@code onComplete}
     * @param fallback the {@code MaybeSource} that is subscribed to if the current {@code Maybe} times out
     * @return the new {@code Maybe} instance
     * @throws NullPointerException if {@code timeoutIndicator} or {@code fallback} is {@code null}
     */
    @BackpressureSupport(BackpressureKind.UNBOUNDED_IN)
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public final <@NonNull U> Maybe<T> timeout(@NonNull Publisher<U> timeoutIndicator, @NonNull MaybeSource<? extends T> fallback) {
        Objects.requireNonNull(timeoutIndicator, "timeoutIndicator is null");
        Objects.requireNonNull(fallback, "fallback is null");
        return RxJavaPlugins.onAssembly(new MaybeTimeoutPublisher<>(this, timeoutIndicator, fallback));
    }

    /**
     * Returns a {@code Maybe} which makes sure when a {@link MaybeObserver} disposes the {@link Disposable},
     * that call is propagated up on the specified {@link Scheduler}.
     * <p>
     * <img width="640" height="693" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.unsubscribeOn.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code unsubscribeOn} calls {@code dispose()} of the upstream on the {@code Scheduler} you specify.</dd>
     * </dl>
     * @param scheduler the target scheduler where to execute the disposal
     * @return the new {@code Maybe} instance
     * @throws NullPointerException if {@code scheduler} is {@code null}
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    public final Maybe<T> unsubscribeOn(@NonNull Scheduler scheduler) {
        Objects.requireNonNull(scheduler, "scheduler is null");
        return RxJavaPlugins.onAssembly(new MaybeUnsubscribeOn<>(this, scheduler));
    }

    /**
     * Waits until this and the other {@link MaybeSource} signal a success value then applies the given {@link BiFunction}
     * to those values and emits the {@code BiFunction}'s resulting value to downstream.
     *
     * <img width="640" height="451" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Maybe.zipWith.png" alt="">
     *
     * <p>If either this or the other {@code MaybeSource} is empty or signals an error, the resulting {@code Maybe} will
     * terminate immediately and dispose the other source.
     *
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code zipWith} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <U>
     *            the type of items emitted by the {@code other} {@code MaybeSource}
     * @param <R>
     *            the type of items emitted by the resulting {@code Maybe}
     * @param other
     *            the other {@code MaybeSource}
     * @param zipper
     *            a function that combines the pairs of items from the two {@code MaybeSource}s to generate the items to
     *            be emitted by the resulting {@code Maybe}
     * @return the new {@code Maybe} instance
     * @throws NullPointerException if {@code other} or {@code zipper} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/zip.html">ReactiveX operators documentation: Zip</a>
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public final <@NonNull U, @NonNull R> Maybe<R> zipWith(@NonNull MaybeSource<? extends U> other, @NonNull BiFunction<? super T, ? super U, ? extends R> zipper) {
        Objects.requireNonNull(other, "other is null");
        return zip(this, other, zipper);
    }

    // ------------------------------------------------------------------
    // Test helper
    // ------------------------------------------------------------------

    /**
     * Creates a {@link TestObserver} and subscribes
     * it to this {@code Maybe}.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code test} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @return the new {@code TestObserver} instance
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final TestObserver<T> test() {
        TestObserver<T> to = new TestObserver<>();
        subscribe(to);
        return to;
    }

    /**
     * Creates a {@link TestObserver} optionally in cancelled state, then subscribes it to this {@code Maybe}.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code test} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param dispose if {@code true}, the {@code TestObserver} will be disposed before subscribing to this
     * {@code Maybe}.
     * @return the new {@code TestObserver} instance
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final TestObserver<T> test(boolean dispose) {
        TestObserver<T> to = new TestObserver<>();

        if (dispose) {
            to.dispose();
        }

        subscribe(to);
        return to;
    }

    // -------------------------------------------------------------------------
    // JDK 8 Support
    // -------------------------------------------------------------------------

    /**
     * Converts the existing value of the provided optional into a {@link #just(Object)}
     * or an empty optional into an {@link #empty()} {@code Maybe} instance.
     * <p>
     * <img width="640" height="335" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/fromOptional.m.png" alt="">
     * <p>
     * Note that the operator takes an already instantiated optional reference and does not
     * by any means create this original optional. If the optional is to be created per
     * consumer upon subscription, use {@link #defer(Supplier)} around {@code fromOptional}:
     * <pre><code>
     * Maybe.defer(() -&gt; Maybe.fromOptional(createOptional()));
     * </code></pre>
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code fromOptional} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the element type of the optional value
     * @param optional the optional value to convert into a {@code Maybe}
     * @return the new {@code Maybe} instance
     * @throws NullPointerException if {@code optional} is {@code null}
     * @since 3.0.0
     * @see #just(Object)
     * @see #empty()
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public static <@NonNull T> Maybe<@NonNull T> fromOptional(@NonNull Optional<T> optional) {
        Objects.requireNonNull(optional, "optional is null");
        return optional.map(Maybe::just).orElseGet(Maybe::empty);
    }

    /**
     * Signals the completion value or error of the given (hot) {@link CompletionStage}-based asynchronous calculation.
     * <p>
     * <img width="640" height="262" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/fromCompletionStage.s.png" alt="">
     * <p>
     * Note that the operator takes an already instantiated, running or terminated {@code CompletionStage}.
     * If the optional is to be created per consumer upon subscription, use {@link #defer(Supplier)}
     * around {@code fromCompletionStage}:
     * <pre><code>
     * Maybe.defer(() -&gt; Maybe.fromCompletionStage(createCompletionStage()));
     * </code></pre>
     * <p>
     * If the {@code CompletionStage} completes with {@code null}, the resulting {@code Maybe} is completed via {@code onComplete}.
     * <p>
     * Canceling the flow can't cancel the execution of the {@code CompletionStage} because {@code CompletionStage}
     * itself doesn't support cancellation. Instead, the operator detaches from the {@code CompletionStage}.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code fromCompletionStage} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the element type of the {@code CompletionStage}
     * @param stage the {@code CompletionStage} to convert to {@code Maybe} and signal its terminal value or error
     * @return the new {@code Maybe} instance
     * @throws NullPointerException if {@code stage} is {@code null}
     * @since 3.0.0
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public static <@NonNull T> Maybe<@NonNull T> fromCompletionStage(@NonNull CompletionStage<T> stage) {
        Objects.requireNonNull(stage, "stage is null");
        return RxJavaPlugins.onAssembly(new MaybeFromCompletionStage<>(stage));
    }

    /**
     * Maps the upstream success value into an {@link Optional} and emits the contained item if not empty.
     * <p>
     * <img width="640" height="323" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/mapOptional.m.png" alt="">
     *
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code mapOptional} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <R> the non-{@code null} output type
     * @param mapper the function that receives the upstream success item and should return a <em>non-empty</em> {@code Optional}
     * to emit as the success output or an <em>empty</em> {@code Optional} to complete the {@code Maybe}
     * @return the new {@code Maybe} instance
     * @throws NullPointerException if {@code mapper} is {@code null}
     * @since 3.0.0
     * @see #map(Function)
     * @see #filter(Predicate)
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <@NonNull R> Maybe<R> mapOptional(@NonNull Function<? super T, @NonNull Optional<? extends R>> mapper) {
        Objects.requireNonNull(mapper, "mapper is null");
        return RxJavaPlugins.onAssembly(new MaybeMapOptional<>(this, mapper));
    }

    /**
     * Signals the upstream success item (or a {@link NoSuchElementException} if the upstream is empty) via
     * a {@link CompletionStage}.
     * <p>
     * <img width="640" height="349" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/toCompletionStage.m.png" alt="">
     * <p>
     * The upstream can be canceled by converting the resulting {@code CompletionStage} into
     * {@link CompletableFuture} via {@link CompletionStage#toCompletableFuture()} and
     * calling {@link CompletableFuture#cancel(boolean)} on it.
     * The upstream will be also cancelled if the resulting {@code CompletionStage} is converted to and
     * completed manually by {@link CompletableFuture#complete(Object)} or {@link CompletableFuture#completeExceptionally(Throwable)}.
     * <p>
     * {@code CompletionStage}s don't have a notion of emptiness and allow {@code null}s, therefore, one can either use
     * {@link #toCompletionStage(Object)} with {@code null} or turn the upstream into a sequence of {@link Optional}s and
     * default to {@link Optional#empty()}:
     * <pre><code>
     * CompletionStage&lt;Optional&lt;T&gt;&gt; stage = source.map(Optional::of).toCompletionStage(Optional.empty());
     * </code></pre>
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code toCompletionStage} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @return the new {@code CompletionStage} instance
     * @since 3.0.0
     * @see #toCompletionStage(Object)
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final CompletionStage<T> toCompletionStage() {
        return subscribeWith(new CompletionStageConsumer<>(false, null));
    }

    /**
     * Signals the upstream success item (or the default item if the upstream is empty) via
     * a {@link CompletionStage}.
     * <p>
     * <img width="640" height="323" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/toCompletionStage.mv.png" alt="">
     * <p>
     * The upstream can be canceled by converting the resulting {@code CompletionStage} into
     * {@link CompletableFuture} via {@link CompletionStage#toCompletableFuture()} and
     * calling {@link CompletableFuture#cancel(boolean)} on it.
     * The upstream will be also cancelled if the resulting {@code CompletionStage} is converted to and
     * completed manually by {@link CompletableFuture#complete(Object)} or {@link CompletableFuture#completeExceptionally(Throwable)}.
     * <p>
     * {@code CompletionStage}s don't have a notion of emptiness and allow {@code null}s, therefore, one can either use
     * a {@code defaultItem} of {@code null} or turn the flow into a sequence of {@link Optional}s and default to {@link Optional#empty()}:
     * <pre><code>
     * CompletionStage&lt;Optional&lt;T&gt;&gt; stage = source.map(Optional::of).toCompletionStage(Optional.empty());
     * </code></pre>
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code toCompletionStage} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param defaultItem the item to signal if the upstream is empty
     * @return the new {@code CompletionStage} instance
     * @since 3.0.0
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final CompletionStage<T> toCompletionStage(@Nullable T defaultItem) {
        return subscribeWith(new CompletionStageConsumer<>(true, defaultItem));
    }

    /**
     * Maps the upstream succecss value into a Java {@link Stream} and emits its
     * items to the downstream consumer as a {@link Flowable}.
     * <p>
     * <img width="640" height="246" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/flattenStreamAsFlowable.m.png" alt="">
     * <p>
     * The operator closes the {@code Stream} upon cancellation and when it terminates. The exceptions raised when
     * closing a {@code Stream} are routed to the global error handler ({@link RxJavaPlugins#onError(Throwable)}.
     * If a {@code Stream} should not be closed, turn it into an {@link Iterable} and use {@link #flattenAsFlowable(Function)}:
     * <pre><code>
     * source.flattenAsFlowable(item -&gt; createStream(item)::iterator);
     * </code></pre>
     * <p>
     * Primitive streams are not supported and items have to be boxed manually (e.g., via {@link IntStream#boxed()}):
     * <pre><code>
     * source.flattenStreamAsFlowable(item -&gt; IntStream.rangeClosed(1, 10).boxed());
     * </code></pre>
     * <p>
     * {@code Stream} does not support concurrent usage so creating and/or consuming the same instance multiple times
     * from multiple threads can lead to undefined behavior.
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The operator honors backpressure from downstream and iterates the given {@code Stream}
     *  on demand (i.e., when requested).</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code flattenStreamAsFlowable} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <R> the element type of the {@code Stream} and the output {@code Flowable}
     * @param mapper the function that receives the upstream success item and should
     * return a {@code Stream} of values to emit.
     * @return the new {@code Flowable} instance
     * @throws NullPointerException if {@code mapper} is {@code null}
     * @since 3.0.0
     * @see #flattenAsFlowable(Function)
     * @see #flattenStreamAsObservable(Function)
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @BackpressureSupport(BackpressureKind.FULL)
    @NonNull
    public final <@NonNull R> Flowable<R> flattenStreamAsFlowable(@NonNull Function<? super T, @NonNull ? extends Stream<? extends R>> mapper) {
        Objects.requireNonNull(mapper, "mapper is null");
        return RxJavaPlugins.onAssembly(new MaybeFlattenStreamAsFlowable<>(this, mapper));
    }

    /**
     * Maps the upstream succecss value into a Java {@link Stream} and emits its
     * items to the downstream consumer as an {@link Observable}.
     * <img width="640" height="241" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/flattenStreamAsObservable.m.png" alt="">
     * <p>
     * The operator closes the {@code Stream} upon cancellation and when it terminates. The exceptions raised when
     * closing a {@code Stream} are routed to the global error handler ({@link RxJavaPlugins#onError(Throwable)}.
     * If a {@code Stream} should not be closed, turn it into an {@link Iterable} and use {@link #flattenAsObservable(Function)}:
     * <pre><code>
     * source.flattenAsObservable(item -&gt; createStream(item)::iterator);
     * </code></pre>
     * <p>
     * Primitive streams are not supported and items have to be boxed manually (e.g., via {@link IntStream#boxed()}):
     * <pre><code>
     * source.flattenStreamAsObservable(item -&gt; IntStream.rangeClosed(1, 10).boxed());
     * </code></pre>
     * <p>
     * {@code Stream} does not support concurrent usage so creating and/or consuming the same instance multiple times
     * from multiple threads can lead to undefined behavior.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code flattenStreamAsObservable} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <R> the element type of the {@code Stream} and the output {@code Observable}
     * @param mapper the function that receives the upstream success item and should
     * return a {@code Stream} of values to emit.
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code mapper} is {@code null}
     * @since 3.0.0
     * @see #flattenAsObservable(Function)
     * @see #flattenStreamAsFlowable(Function)
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <@NonNull R> Observable<R> flattenStreamAsObservable(@NonNull Function<? super T, @NonNull ? extends Stream<? extends R>> mapper) {
        Objects.requireNonNull(mapper, "mapper is null");
        return RxJavaPlugins.onAssembly(new MaybeFlattenStreamAsObservable<>(this, mapper));
    }
}
