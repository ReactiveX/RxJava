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
import io.reactivex.rxjava3.internal.operators.completable.*;
import io.reactivex.rxjava3.internal.operators.flowable.*;
import io.reactivex.rxjava3.internal.operators.maybe.*;
import io.reactivex.rxjava3.internal.operators.mixed.*;
import io.reactivex.rxjava3.internal.operators.observable.ObservableSingleSingle;
import io.reactivex.rxjava3.internal.operators.single.*;
import io.reactivex.rxjava3.internal.util.ErrorMode;
import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.schedulers.*;

/**
 * The {@code Single} class implements the Reactive Pattern for a single value response.
 * <p>
 * {@code Single} behaves similarly to {@link Observable} except that it can only emit either a single successful
 * value or an error (there is no {@code onComplete} notification as there is for an {@code Observable}).
 * <p>
 * The {@code Single} class implements the {@link SingleSource} base interface and the default consumer
 * type it interacts with is the {@link SingleObserver} via the {@link #subscribe(SingleObserver)} method.
 * <p>
 * The {@code Single} operates with the following sequential protocol:
 * <pre>
 *     <code>onSubscribe (onSuccess | onError)?</code>
 * </pre>
 * <p>
 * Note that {@code onSuccess} and {@code onError} are mutually exclusive events; unlike {@code Observable},
 * {@code onSuccess} is never followed by {@code onError}.
 * <p>
 * Like {@code Observable}, a running {@code Single} can be stopped through the {@link Disposable} instance
 * provided to consumers through {@link SingleObserver#onSubscribe}.
 * <p>
 * Like an {@code Observable}, a {@code Single} is lazy, can be either "hot" or "cold", synchronous or
 * asynchronous. {@code Single} instances returned by the methods of this class are <em>cold</em>
 * and there is a standard <em>hot</em> implementation in the form of a subject:
 * {@link io.reactivex.rxjava3.subjects.SingleSubject SingleSubject}.
 * <p>
 * The documentation for this class makes use of marble diagrams. The following legend explains these diagrams:
 * <p>
 * <img width="640" height="301" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.legend.v3.png" alt="">
 * <p>
 * See {@link Flowable} or {@code Observable} for the
 * implementation of the Reactive Pattern for a stream or vector of values.
 * <p>
 * For more information see the <a href="http://reactivex.io/documentation/single.html">ReactiveX
 * documentation</a>.
 * <p>
 * Example:
 * <pre><code>
 * Disposable d = Single.just("Hello World")
 *    .delay(10, TimeUnit.SECONDS, Schedulers.io())
 *    .subscribeWith(new DisposableSingleObserver&lt;String&gt;() {
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
 *    });
 * 
 * Thread.sleep(5000);
 * 
 * d.dispose();
 * </code></pre>
 * <p>
 * Note that by design, subscriptions via {@link #subscribe(SingleObserver)} can't be disposed
 * from the outside (hence the
 * {@code void} return of the {@link #subscribe(SingleObserver)} method) and it is the
 * responsibility of the implementor of the {@code SingleObserver} to allow this to happen.
 * RxJava supports such usage with the standard
 * {@link io.reactivex.rxjava3.observers.DisposableSingleObserver DisposableSingleObserver} instance.
 * For convenience, the {@link #subscribeWith(SingleObserver)} method is provided as well to
 * allow working with a {@code SingleObserver} (or subclass) instance to be applied with in
 * a fluent manner (such as in the example above).
 * @param <T>
 *            the type of the item emitted by the {@code Single}
 * @since 2.0
 * @see io.reactivex.rxjava3.observers.DisposableSingleObserver
 */
public abstract class Single<@NonNull T> implements SingleSource<T> {

    /**
     * Runs multiple {@link SingleSource}s and signals the events of the first one that signals (disposing
     * the rest).
     * <p>
     * <img width="640" height="516" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.amb.v3.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code amb} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the value type
     * @param sources the {@link Iterable} sequence of sources. A subscription to each source will
     *            occur in the same order as in this {@code Iterable}.
     * @return the new {@code Single} instance
     * @throws NullPointerException if {@code sources} is {@code null}
     * @since 2.0
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <@NonNull T> Single<T> amb(@NonNull Iterable<@NonNull ? extends SingleSource<? extends T>> sources) {
        Objects.requireNonNull(sources, "sources is null");
        return RxJavaPlugins.onAssembly(new SingleAmb<>(null, sources));
    }

    /**
     * Runs multiple {@link SingleSource}s and signals the events of the first one that signals (disposing
     * the rest).
     * <p>
     * <img width="640" height="516" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.ambArray.v3.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code ambArray} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the value type
     * @param sources the array of sources. A subscription to each source will
     *            occur in the same order as in this array.
     * @return the new {@code Single} instance
     * @throws NullPointerException if {@code sources} is {@code null}
     * @since 2.0
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @SafeVarargs
    @NonNull
    public static <@NonNull T> Single<T> ambArray(@NonNull SingleSource<? extends T>... sources) {
        Objects.requireNonNull(sources, "sources is null");
        if (sources.length == 0) {
            return error(SingleInternalHelper.emptyThrower());
        }
        if (sources.length == 1) {
            @SuppressWarnings("unchecked")
            SingleSource<T> source = (SingleSource<T>)sources[0];
            return wrap(source);
        }
        return RxJavaPlugins.onAssembly(new SingleAmb<>(sources, null));
    }

    /**
     * Concatenate the single values, in a non-overlapping fashion, of the {@link SingleSource}s provided by
     * an {@link Iterable} sequence.
     * <p>
     * <img width="640" height="319" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.concat.i.v3.png" alt="">
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The returned {@link Flowable} honors the backpressure of the downstream consumer.</dd>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code concat} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the value type
     * @param sources the {@code Iterable} sequence of {@code SingleSource} instances
     * @return the new {@code Flowable} instance
     * @throws NullPointerException if {@code sources} is {@code null}
     * @since 2.0
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    @BackpressureSupport(BackpressureKind.FULL)
    public static <@NonNull T> Flowable<T> concat(@NonNull Iterable<@NonNull ? extends SingleSource<? extends T>> sources) {
        return Flowable.fromIterable(sources).concatMapSingleDelayError(Functions.identity(), false);
    }

    /**
     * Concatenate the single values, in a non-overlapping fashion, of the {@link SingleSource}s provided by
     * an {@link ObservableSource} sequence.
     * <p>
     * <img width="640" height="319" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.concat.o.v3.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code concat} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the value type
     * @param sources the {@code ObservableSource} of {@code SingleSource} instances
     * @return the new {@link Observable} instance
     * @throws NullPointerException if {@code sources} is {@code null}
     * @since 2.0
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <@NonNull T> Observable<T> concat(@NonNull ObservableSource<? extends SingleSource<? extends T>> sources) {
        Objects.requireNonNull(sources, "sources is null");
        return RxJavaPlugins.onAssembly(new ObservableConcatMapSingle<>(sources, Functions.identity(), ErrorMode.IMMEDIATE, 2));
    }

    /**
     * Concatenate the single values, in a non-overlapping fashion, of the {@link SingleSource}s provided by
     * a {@link Publisher} sequence.
     * <p>
     * <img width="640" height="309" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.concat.p.v3.png" alt="">
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The returned {@link Flowable} honors the backpressure of the downstream consumer
     *  and the sources {@code Publisher} is expected to honor it as well.</dd>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code concat} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the value type
     * @param sources the {@code Publisher} of {@code SingleSource} instances
     * @return the new {@code Flowable} instance
     * @throws NullPointerException if {@code sources} is {@code null}
     * @since 2.0
     */
    @CheckReturnValue
    @NonNull
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <@NonNull T> Flowable<T> concat(@NonNull Publisher<@NonNull ? extends SingleSource<? extends T>> sources) {
        return concat(sources, 2);
    }

    /**
     * Concatenate the single values, in a non-overlapping fashion, of the {@link SingleSource}s provided by
     * a {@link Publisher} sequence and prefetched by the specified amount.
     * <p>
     * <img width="640" height="310" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.concat.pn.v3.png" alt="">
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The returned {@link Flowable} honors the backpressure of the downstream consumer
     *  and the sources {@code Publisher} is expected to honor it as well.</dd>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code concat} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the value type
     * @param sources the {@code Publisher} of {@code SingleSource} instances
     * @param prefetch the number of {@code SingleSource}s to prefetch from the {@code Publisher}
     * @return the new {@code Flowable} instance
     * @throws NullPointerException if {@code sources} is {@code null}
     * @throws IllegalArgumentException if {@code prefetch} is non-positive
     * @since 2.0
     */
    @CheckReturnValue
    @NonNull
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <@NonNull T> Flowable<T> concat(@NonNull Publisher<@NonNull ? extends SingleSource<? extends T>> sources, int prefetch) {
        Objects.requireNonNull(sources, "sources is null");
        ObjectHelper.verifyPositive(prefetch, "prefetch");
        return RxJavaPlugins.onAssembly(new FlowableConcatMapSinglePublisher<>(sources, Functions.identity(), ErrorMode.IMMEDIATE, prefetch));
    }

    /**
     * Returns a {@link Flowable} that emits the items emitted by two {@link SingleSource}s, one after the other.
     * <p>
     * <img width="640" height="366" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.concat.v3.png" alt="">
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The returned {@code Flowable} honors the backpressure of the downstream consumer.</dd>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code concat} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T> the common value type
     * @param source1
     *            a {@code SingleSource} to be concatenated
     * @param source2
     *            a {@code SingleSource} to be concatenated
     * @return the new {@code Flowable} instance
     * @throws NullPointerException if {@code source1} or {@code source2} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/concat.html">ReactiveX operators documentation: Concat</a>
     */
    @CheckReturnValue
    @NonNull
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <@NonNull T> Flowable<T> concat(
            @NonNull SingleSource<? extends T> source1, @NonNull SingleSource<? extends T> source2
     ) {
        Objects.requireNonNull(source1, "source1 is null");
        Objects.requireNonNull(source2, "source2 is null");
        return Flowable.fromArray(source1, source2).concatMapSingleDelayError(Functions.identity(), false);
    }

    /**
     * Returns a {@link Flowable} that emits the items emitted by three {@link SingleSource}s, one after the other.
     * <p>
     * <img width="640" height="366" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.concat.o3.v3.png" alt="">
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The returned {@code Flowable} honors the backpressure of the downstream consumer.</dd>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code concat} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T> the common value type
     * @param source1
     *            a {@code SingleSource} to be concatenated
     * @param source2
     *            a {@code SingleSource} to be concatenated
     * @param source3
     *            a {@code SingleSource} to be concatenated
     * @return the new {@code Flowable} instance
     * @throws NullPointerException if {@code source1}, {@code source2} or {@code source3} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/concat.html">ReactiveX operators documentation: Concat</a>
     */
    @CheckReturnValue
    @NonNull
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <@NonNull T> Flowable<T> concat(
            @NonNull SingleSource<? extends T> source1, @NonNull SingleSource<? extends T> source2,
            @NonNull SingleSource<? extends T> source3
     ) {
        Objects.requireNonNull(source1, "source1 is null");
        Objects.requireNonNull(source2, "source2 is null");
        Objects.requireNonNull(source3, "source3 is null");
        return Flowable.fromArray(source1, source2, source3).concatMapSingleDelayError(Functions.identity(), false);
    }

    /**
     * Returns a {@link Flowable} that emits the items emitted by four {@link SingleSource}s, one after the other.
     * <p>
     * <img width="640" height="362" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.concat.o4.v3.png" alt="">
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The returned {@code Flowable} honors the backpressure of the downstream consumer.</dd>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code concat} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T> the common value type
     * @param source1
     *            a {@code SingleSource} to be concatenated
     * @param source2
     *            a {@code SingleSource} to be concatenated
     * @param source3
     *            a {@code SingleSource} to be concatenated
     * @param source4
     *            a {@code SingleSource} to be concatenated
     * @return the new {@code Flowable} instance
     * @throws NullPointerException if {@code source1}, {@code source2}, {@code source3} or {@code source4} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/concat.html">ReactiveX operators documentation: Concat</a>
     */
    @CheckReturnValue
    @NonNull
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <@NonNull T> Flowable<T> concat(
            @NonNull SingleSource<? extends T> source1, @NonNull SingleSource<? extends T> source2,
            @NonNull SingleSource<? extends T> source3, @NonNull SingleSource<? extends T> source4
     ) {
        Objects.requireNonNull(source1, "source1 is null");
        Objects.requireNonNull(source2, "source2 is null");
        Objects.requireNonNull(source3, "source3 is null");
        Objects.requireNonNull(source4, "source4 is null");
        return Flowable.fromArray(source1, source2, source3, source4).concatMapSingleDelayError(Functions.identity(), false);
    }

    /**
     * Concatenate the single values, in a non-overlapping fashion, of the {@link SingleSource}s provided in
     * an array.
     * <p>
     * <img width="640" height="319" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.concatArray.v3.png" alt="">
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The returned {@link Flowable} honors the backpressure of the downstream consumer.</dd>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code concatArray} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the value type
     * @param sources the array of {@code SingleSource} instances
     * @return the new {@code Flowable} instance
     * @throws NullPointerException if {@code sources} is {@code null}
     * @since 2.0
     */
    @CheckReturnValue
    @NonNull
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerSupport.NONE)
    @SafeVarargs
    public static <@NonNull T> Flowable<T> concatArray(@NonNull SingleSource<? extends T>... sources) {
        return Flowable.fromArray(sources).concatMapSingleDelayError(Functions.identity(), false);
    }

    /**
     * Concatenate the single values, in a non-overlapping fashion, of the {@link SingleSource}s provided in
     * an array.
     * <p>
     * <img width="640" height="408" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.concatArrayDelayError.png" alt="">
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The returned {@link Flowable} honors the backpressure of the downstream consumer.</dd>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code concatArrayDelayError} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the value type
     * @param sources the array of {@code SingleSource} instances
     * @return the new {@code Flowable} instance
     * @throws NullPointerException if {@code sources} is {@code null}
     * @since 3.0.0
     */
    @CheckReturnValue
    @NonNull
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerSupport.NONE)
    @SafeVarargs
    public static <@NonNull T> Flowable<T> concatArrayDelayError(@NonNull SingleSource<? extends T>... sources) {
        return Flowable.fromArray(sources).concatMapSingleDelayError(Functions.identity(), true);
    }

    /**
     * Concatenates a sequence of {@link SingleSource} eagerly into a single stream of values.
     * <p>
     * <img width="640" height="257" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.concatArrayEager.png" alt="">
     * <p>
     * Eager concatenation means that once a subscriber subscribes, this operator subscribes to all of the
     * source {@code SingleSource}s. The operator buffers the value emitted by these {@code SingleSource}s and then drains them
     * in order, each one after the previous one succeeds.
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The operator honors backpressure from downstream.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This method does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the value type
     * @param sources a sequence of {@code SingleSource}s that need to be eagerly concatenated
     * @return the new {@link Flowable} instance with the specified concatenation behavior
     * @throws NullPointerException if {@code sources} is {@code null}
     */
    @BackpressureSupport(BackpressureKind.FULL)
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    @SafeVarargs
    public static <@NonNull T> Flowable<T> concatArrayEager(@NonNull SingleSource<? extends T>... sources) {
        return Flowable.fromArray(sources).concatMapEager(SingleInternalHelper.toFlowable());
    }

    /**
     * Concatenates a sequence of {@link SingleSource} eagerly into a single stream of values.
     * <p>
     * <img width="640" height="426" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.concatArrayEagerDelayError.png" alt="">
     * <p>
     * Eager concatenation means that once a subscriber subscribes, this operator subscribes to all of the
     * source {@code SingleSource}s. The operator buffers the value emitted by these {@code SingleSource}s and then drains them
     * in order, each one after the previous one succeeds.
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The operator honors backpressure from downstream.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This method does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the value type
     * @param sources a sequence of {@code SingleSource}s that need to be eagerly concatenated
     * @return the new {@link Flowable} instance with the specified concatenation behavior
     * @throws NullPointerException if {@code sources} is {@code null}
     * @since 3.0.0
     */
    @BackpressureSupport(BackpressureKind.FULL)
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    @SafeVarargs
    public static <@NonNull T> Flowable<T> concatArrayEagerDelayError(@NonNull SingleSource<? extends T>... sources) {
        return Flowable.fromArray(sources).concatMapEagerDelayError(SingleInternalHelper.toFlowable(), true);
    }

    /**
     * Concatenates the {@link Iterable} sequence of {@link SingleSource}s into a single sequence by subscribing to each {@code SingleSource},
     * one after the other, one at a time and delays any errors till the all inner {@code SingleSource}s terminate
     * as a {@link Flowable} sequence.
     * <p>
     * <img width="640" height="451" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.concatDelayError.i.png" alt="">
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The operator honors backpressure from downstream.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code concatDelayError} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T> the common element base type
     * @param sources the {@code Iterable} sequence of {@code SingleSource}s
     * @return the new {@code Flowable} with the concatenating behavior
     * @throws NullPointerException if {@code sources} is {@code null}
     * @since 3.0.0
     */
    @BackpressureSupport(BackpressureKind.FULL)
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <@NonNull T> Flowable<T> concatDelayError(@NonNull Iterable<@NonNull ? extends SingleSource<? extends T>> sources) {
        return Flowable.fromIterable(sources).concatMapSingleDelayError(Functions.identity());
    }

    /**
     * Concatenates the {@link Publisher} sequence of {@link SingleSource}s into a single sequence by subscribing to each inner {@code SingleSource},
     * one after the other, one at a time and delays any errors till the all inner and the outer {@code Publisher} terminate
     * as a {@link Flowable} sequence.
     * <p>
     * <img width="640" height="345" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.concatDelayError.p.png" alt="">
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>{@code concatDelayError} fully supports backpressure.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code concatDelayError} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T> the common element base type
     * @param sources the {@code Publisher} sequence of {@code SingleSource}s
     * @return the new {@code Flowable} with the concatenating behavior
     * @throws NullPointerException if {@code sources} is {@code null}
     * @since 3.0.0
     */
    @BackpressureSupport(BackpressureKind.FULL)
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public static <@NonNull T> Flowable<T> concatDelayError(@NonNull Publisher<@NonNull ? extends SingleSource<? extends T>> sources) {
        return Flowable.fromPublisher(sources).concatMapSingleDelayError(Functions.identity());
    }

    /**
     * Concatenates the {@link Publisher} sequence of {@link SingleSource}s into a single sequence by subscribing to each inner {@code SingleSource},
     * one after the other, one at a time and delays any errors till the all inner and the outer {@code Publisher} terminate
     * as a {@link Flowable} sequence.
     * <p>
     * <img width="640" height="299" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.concatDelayError.pn.png" alt="">
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>{@code concatDelayError} fully supports backpressure.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code concatDelayError} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T> the common element base type
     * @param sources the {@code Publisher} sequence of {@code SingleSource}s
     * @param prefetch The number of upstream items to prefetch so that fresh items are
     *                 ready to be mapped when a previous {@code SingleSource} terminates.
     *                 The operator replenishes after half of the prefetch amount has been consumed
     *                 and turned into {@code SingleSource}s.
     * @return the new {@code Flowable} with the concatenating behavior
     * @throws NullPointerException if {@code sources} is {@code null}
     * @throws IllegalArgumentException if {@code prefetch} is non-positive
     * @since 3.0.0
     */
    @BackpressureSupport(BackpressureKind.FULL)
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public static <@NonNull T> Flowable<T> concatDelayError(@NonNull Publisher<@NonNull ? extends SingleSource<? extends T>> sources, int prefetch) {
        return Flowable.fromPublisher(sources).concatMapSingleDelayError(Functions.identity(), true, prefetch);
    }

    /**
     * Concatenates an {@link Iterable} sequence of {@link SingleSource}s eagerly into a single stream of values.
     * <p>
     * <img width="640" height="319" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.concatEager.i.v3.png" alt="">
     * <p>
     * Eager concatenation means that once a subscriber subscribes, this operator subscribes to all of the
     * source {@code SingleSource}s. The operator buffers the values emitted by these {@code SingleSource}s and then drains them
     * in order, each one after the previous one succeeds.
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>Backpressure is honored towards the downstream.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This method does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the value type
     * @param sources an {@code Iterable} sequence of {@code SingleSource} that need to be eagerly concatenated
     * @return the new {@link Flowable} instance with the specified concatenation behavior
     * @throws NullPointerException if {@code sources} is {@code null}
     */
    @BackpressureSupport(BackpressureKind.FULL)
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <@NonNull T> Flowable<T> concatEager(@NonNull Iterable<@NonNull ? extends SingleSource<? extends T>> sources) {
        return Flowable.fromIterable(sources).concatMapEagerDelayError(SingleInternalHelper.toFlowable(), false);
    }

    /**
     * Concatenates an {@link Iterable} sequence of {@link SingleSource}s eagerly into a single stream of values and
     * runs a limited number of the inner sources at once.
     * <p>
     * <img width="640" height="439" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.concatEager.in.png" alt="">
     * <p>
     * Eager concatenation means that once a subscriber subscribes, this operator subscribes to all of the
     * source {@code SingleSource}s. The operator buffers the values emitted by these {@code SingleSource}s and then drains them
     * in order, each one after the previous one succeeds.
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>Backpressure is honored towards the downstream.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This method does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the value type
     * @param sources an {@code Iterable} sequence of {@code SingleSource} that need to be eagerly concatenated
     * @param maxConcurrency the maximum number of concurrently running inner {@code SingleSource}s; {@link Integer#MAX_VALUE}
     *                       is interpreted as all inner {@code SingleSource}s can be active at the same time
     * @return the new {@link Flowable} instance with the specified concatenation behavior
     * @throws NullPointerException if {@code sources} is {@code null}
     * @throws IllegalArgumentException if {@code maxConcurrency} is non-positive
     * @since 3.0.0
     */
    @BackpressureSupport(BackpressureKind.FULL)
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <@NonNull T> Flowable<T> concatEager(@NonNull Iterable<@NonNull ? extends SingleSource<? extends T>> sources, int maxConcurrency) {
        return Flowable.fromIterable(sources).concatMapEagerDelayError(SingleInternalHelper.toFlowable(), false, maxConcurrency, 1);
    }

    /**
     * Concatenates a {@link Publisher} sequence of {@link SingleSource}s eagerly into a single stream of values.
     * <p>
     * <img width="640" height="307" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.concatEager.p.v3.png" alt="">
     * <p>
     * Eager concatenation means that once a subscriber subscribes, this operator subscribes to all of the
     * emitted source {@code SingleSource}s as they are observed. The operator buffers the values emitted by these
     * {@code SingleSource}s and then drains them in order, each one after the previous one succeeds.
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>Backpressure is honored towards the downstream and the outer {@code Publisher} is
     *  expected to support backpressure. Violating this assumption, the operator will
     *  signal {@link io.reactivex.rxjava3.exceptions.MissingBackpressureException}.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This method does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the value type
     * @param sources a sequence of {@code SingleSource}s that need to be eagerly concatenated
     * @return the new {@link Flowable} instance with the specified concatenation behavior
     * @throws NullPointerException if {@code sources} is {@code null}
     */
    @BackpressureSupport(BackpressureKind.FULL)
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <@NonNull T> Flowable<T> concatEager(@NonNull Publisher<@NonNull ? extends SingleSource<? extends T>> sources) {
        return Flowable.fromPublisher(sources).concatMapEager(SingleInternalHelper.toFlowable());
    }

    /**
     * Concatenates a {@link Publisher} sequence of {@link SingleSource}s eagerly into a single stream of values and
     * runs a limited number of those inner {@code SingleSource}s at once.
     * <p>
     * <img width="640" height="425" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.concatEager.pn.png" alt="">
     * <p>
     * Eager concatenation means that once a subscriber subscribes, this operator subscribes to all of the
     * emitted source {@code SingleSource}s as they are observed. The operator buffers the values emitted by these
     * {@code SingleSource}s and then drains them in order, each one after the previous one succeeds.
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>Backpressure is honored towards the downstream and the outer {@code Publisher} is
     *  expected to support backpressure. Violating this assumption, the operator will
     *  signal {@link io.reactivex.rxjava3.exceptions.MissingBackpressureException}.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This method does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the value type
     * @param sources a sequence of {@code SingleSource}s that need to be eagerly concatenated
     * @param maxConcurrency the maximum number of concurrently running inner {@code SingleSource}s; {@link Integer#MAX_VALUE}
     *                       is interpreted as all inner {@code SingleSource}s can be active at the same time
     * @return the new {@link Flowable} instance with the specified concatenation behavior
     * @throws NullPointerException if {@code sources} is {@code null}
     * @throws IllegalArgumentException if {@code maxConcurrency} is non-positive
     * @since 3.0.0
     */
    @BackpressureSupport(BackpressureKind.FULL)
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <@NonNull T> Flowable<T> concatEager(@NonNull Publisher<@NonNull ? extends SingleSource<? extends T>> sources, int maxConcurrency) {
        return Flowable.fromPublisher(sources).concatMapEager(SingleInternalHelper.toFlowable(), maxConcurrency, 1);
    }

    /**
     * Concatenates an {@link Iterable} sequence of {@link SingleSource}s eagerly into a single stream of values,
     * delaying errors until all the inner sources terminate.
     * <p>
     * <img width="640" height="431" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.concatEagerDelayError.i.png" alt="">
     * <p>
     * Eager concatenation means that once a subscriber subscribes, this operator subscribes to all of the
     * source {@code SingleSource}s. The operator buffers the values emitted by these {@code SingleSource}s and then drains them
     * in order, each one after the previous one succeeds.
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>Backpressure is honored towards the downstream.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This method does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the value type
     * @param sources an {@code Iterable} sequence of {@code SingleSource} that need to be eagerly concatenated
     * @return the new {@link Flowable} instance with the specified concatenation behavior
     * @throws NullPointerException if {@code sources} is {@code null}
     * @since 3.0.0
     */
    @BackpressureSupport(BackpressureKind.FULL)
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <@NonNull T> Flowable<T> concatEagerDelayError(@NonNull Iterable<@NonNull ? extends SingleSource<? extends T>> sources) {
        return Flowable.fromIterable(sources).concatMapEagerDelayError(SingleInternalHelper.toFlowable(), true);
    }

    /**
     * Concatenates an {@link Iterable} sequence of {@link SingleSource}s eagerly into a single stream of values,
     * delaying errors until all the inner sources terminate.
     * <p>
     * <img width="640" height="378" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.concatEagerDelayError.in.png" alt="">
     * <p>
     * Eager concatenation means that once a subscriber subscribes, this operator subscribes to all of the
     * source {@code SingleSource}s. The operator buffers the values emitted by these {@code SingleSource}s and then drains them
     * in order, each one after the previous one succeeds.
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>Backpressure is honored towards the downstream.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This method does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the value type
     * @param sources an {@code Iterable} sequence of {@code SingleSource} that need to be eagerly concatenated
     * @param maxConcurrency the maximum number of concurrently running inner {@code SingleSource}s; {@link Integer#MAX_VALUE}
     *                       is interpreted as all inner {@code SingleSource}s can be active at the same time
     * @return the new {@link Flowable} instance with the specified concatenation behavior
     * @throws NullPointerException if {@code sources} is {@code null}
     * @throws IllegalArgumentException if {@code maxConcurrency} is non-positive
     * @since 3.0.0
     */
    @BackpressureSupport(BackpressureKind.FULL)
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <@NonNull T> Flowable<T> concatEagerDelayError(@NonNull Iterable<@NonNull ? extends SingleSource<? extends T>> sources, int maxConcurrency) {
        return Flowable.fromIterable(sources).concatMapEagerDelayError(SingleInternalHelper.toFlowable(), true, maxConcurrency, 1);
    }

    /**
     * Concatenates a {@link Publisher} sequence of {@link SingleSource}s eagerly into a single stream of values,
     * delaying errors until all the inner and the outer sequence terminate.
     * <p>
     * <img width="640" height="444" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.concatEagerDelayError.p.png" alt="">
     * <p>
     * Eager concatenation means that once a subscriber subscribes, this operator subscribes to all of the
     * emitted source {@code SingleSource}s as they are observed. The operator buffers the values emitted by these
     * {@code SingleSource}s and then drains them in order, each one after the previous one succeeds.
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>Backpressure is honored towards the downstream and the outer {@code Publisher} is
     *  expected to support backpressure. Violating this assumption, the operator will
     *  signal {@link io.reactivex.rxjava3.exceptions.MissingBackpressureException}.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This method does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the value type
     * @param sources a sequence of {@code SingleSource}s that need to be eagerly concatenated
     * @return the new {@link Flowable} instance with the specified concatenation behavior
     * @throws NullPointerException if {@code sources} is {@code null}
     * @since 3.0.0
     */
    @BackpressureSupport(BackpressureKind.FULL)
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <@NonNull T> Flowable<T> concatEagerDelayError(@NonNull Publisher<@NonNull ? extends SingleSource<? extends T>> sources) {
        return Flowable.fromPublisher(sources).concatMapEagerDelayError(SingleInternalHelper.toFlowable(), true);
    }

    /**
     * Concatenates a {@link Publisher} sequence of {@link SingleSource}s eagerly into a single stream of values,
     * running at most the specified number of those inner {@code SingleSource}s at once and
     * delaying errors until all the inner and the outer sequence terminate.
     * <p>
     * <img width="640" height="421" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.concatEagerDelayError.pn.png" alt="">
     * <p>
     * Eager concatenation means that once a subscriber subscribes, this operator subscribes to all of the
     * emitted source {@code SingleSource}s as they are observed. The operator buffers the values emitted by these
     * {@code SingleSource}s and then drains them in order, each one after the previous one succeeds.
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>Backpressure is honored towards the downstream and the outer {@code Publisher} is
     *  expected to support backpressure. Violating this assumption, the operator will
     *  signal {@link io.reactivex.rxjava3.exceptions.MissingBackpressureException}.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This method does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the value type
     * @param sources a sequence of {@code SingleSource}s that need to be eagerly concatenated
     * @param maxConcurrency the number of inner {@code SingleSource}s to run at once
     * @return the new {@link Flowable} instance with the specified concatenation behavior
     * @throws NullPointerException if {@code sources} is {@code null}
     * @throws IllegalArgumentException if {@code maxConcurrency} is non-positive
     * @since 3.0.0
     */
    @BackpressureSupport(BackpressureKind.FULL)
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <@NonNull T> Flowable<T> concatEagerDelayError(@NonNull Publisher<@NonNull ? extends SingleSource<? extends T>> sources, int maxConcurrency) {
        return Flowable.fromPublisher(sources).concatMapEagerDelayError(SingleInternalHelper.toFlowable(), true, maxConcurrency, 1);
    }

    /**
     * Provides an API (via a cold {@code Single}) that bridges the reactive world with the callback-style world.
     * <p>
     * <img width="640" height="454" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.create.v3.png" alt="">
     * <p>
     * Example:
     * <pre><code>
     * Single.&lt;Event&gt;create(emitter -&gt; {
     *     Callback listener = new Callback() {
     *         &#64;Override
     *         public void onEvent(Event e) {
     *             emitter.onSuccess(e);
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
     * Whenever a {@link SingleObserver} subscribes to the returned {@code Single}, the provided
     * {@link SingleOnSubscribe} callback is invoked with a fresh instance of a {@link SingleEmitter}
     * that will interact only with that specific {@code SingleObserver}. If this {@code SingleObserver}
     * disposes the flow (making {@link SingleEmitter#isDisposed} return {@code true}),
     * other observers subscribed to the same returned {@code Single} are not affected.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code create} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the value type
     * @param source the emitter that is called when a {@code SingleObserver} subscribes to the returned {@code Single}
     * @return the new {@code Single} instance
     * @throws NullPointerException if {@code source} is {@code null}
     * @see SingleOnSubscribe
     * @see Cancellable
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <@NonNull T> Single<T> create(@NonNull SingleOnSubscribe<T> source) {
        Objects.requireNonNull(source, "source is null");
        return RxJavaPlugins.onAssembly(new SingleCreate<>(source));
    }

    /**
     * Calls a {@link Supplier} for each individual {@link SingleObserver} to return the actual {@link SingleSource} to
     * be subscribed to.
     * <p>
     * <img width="640" height="515" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.defer.v3.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code defer} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the value type
     * @param supplier the {@code Supplier} that is called for each individual {@code SingleObserver} and
     * returns a {@code SingleSource} instance to subscribe to
     * @throws NullPointerException if {@code supplier} is {@code null}
     * @return the new {@code Single} instance
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <@NonNull T> Single<T> defer(@NonNull Supplier<? extends @NonNull SingleSource<? extends T>> supplier) {
        Objects.requireNonNull(supplier, "supplier is null");
        return RxJavaPlugins.onAssembly(new SingleDefer<>(supplier));
    }

    /**
     * Signals a {@link Throwable} returned by the callback function for each individual {@link SingleObserver}.
     * <p>
     * <img width="640" height="283" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.error.c.v3.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code error} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the value type
     * @param supplier the {@link Supplier} that is called for each individual {@code SingleObserver} and
     * returns a {@code Throwable} instance to be emitted.
     * @throws NullPointerException if {@code supplier} is {@code null}
     * @return the new {@code Single} instance
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <@NonNull T> Single<T> error(@NonNull Supplier<? extends @NonNull Throwable> supplier) {
        Objects.requireNonNull(supplier, "supplier is null");
        return RxJavaPlugins.onAssembly(new SingleError<>(supplier));
    }

    /**
     * Returns a {@code Single} that invokes a subscriber's {@link SingleObserver#onError onError} method when the
     * subscriber subscribes to it.
     * <p>
     * <img width="640" height="283" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.error.v3.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code error} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param throwable
     *            the particular {@link Throwable} to pass to {@link SingleObserver#onError onError}
     * @param <T>
     *            the type of the item (ostensibly) emitted by the {@code Single}
     * @return the new {@code Single} that invokes the subscriber's {@link SingleObserver#onError onError} method when
     *         the subscriber subscribes to it
     * @throws NullPointerException if {@code throwable} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/empty-never-throw.html">ReactiveX operators documentation: Throw</a>
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <@NonNull T> Single<T> error(@NonNull Throwable throwable) {
        Objects.requireNonNull(throwable, "throwable is null");
        return error(Functions.justSupplier(throwable));
    }

    /**
     * Returns a {@code Single} that invokes the given {@link Callable} for each incoming {@link SingleObserver}
     * and emits its value or exception to them.
     * <p>
     * Allows you to defer execution of passed function until {@code SingleObserver} subscribes to the {@code Single}.
     * It makes passed function "lazy".
     * Result of the function invocation will be emitted by the {@link Single}.
     * <p>
     * <img width="640" height="467" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.fromCallable.v3.png" alt="">
     * <dl>
     *   <dt><b>Scheduler:</b></dt>
     *   <dd>{@code fromCallable} does not operate by default on a particular {@link Scheduler}.</dd>
     *   <dt><b>Error handling:</b></dt>
     *   <dd> If the {@code Callable} throws an exception, the respective {@link Throwable} is
     *   delivered to the downstream via {@link SingleObserver#onError(Throwable)},
     *   except when the downstream has disposed this {@code Single} source.
     *   In this latter case, the {@code Throwable} is delivered to the global error handler via
     *   {@link RxJavaPlugins#onError(Throwable)} as an {@link io.reactivex.rxjava3.exceptions.UndeliverableException UndeliverableException}.
     *   </dd>
     * </dl>
     *
     * @param callable
     *         function which execution should be deferred, it will be invoked when {@code SingleObserver} will subscribe to the {@link Single}.
     * @param <T>
     *         the type of the item emitted by the {@code Single}.
     * @return the new {@code Single} whose {@code SingleObserver}s' subscriptions trigger an invocation of the given function.
     * @throws NullPointerException if {@code callable} is {@code null}
     * @see #defer(Supplier)
     * @see #fromSupplier(Supplier)
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <@NonNull T> Single<T> fromCallable(@NonNull Callable<? extends T> callable) {
        Objects.requireNonNull(callable, "callable is null");
        return RxJavaPlugins.onAssembly(new SingleFromCallable<>(callable));
    }

    /**
     * Converts a {@link Future} into a {@code Single} and awaits its outcome in a blocking fashion.
     * <p>
     * <img width="640" height="315" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.from.Future.v3.png" alt="">
     * <p>
     * The operator calls {@link Future#get()}, which is a blocking method, on the subscription thread.
     * It is recommended applying {@link #subscribeOn(Scheduler)} to move this blocking wait to a
     * background thread, and if the {@link Scheduler} supports it, interrupt the wait when the flow
     * is disposed.
     * <p>
     * A non-{@code null} value is then emitted via {@code onSuccess} or any exception is emitted via
     * {@code onError}. If the {@code Future} completes with {@code null}, a {@link NullPointerException}
     * is signaled.
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code fromFuture} does not operate by default on a particular {@code Scheduler}.</dd>
     * </dl>
     *
     * @param future
     *            the source {@code Future}
     * @param <T>
     *            the type of object that the {@code Future} returns, and also the type of item to be emitted by
     *            the resulting {@code Single}
     * @return the new {@code Single} that emits the item from the source {@code Future}
     * @throws NullPointerException if {@code future} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/from.html">ReactiveX operators documentation: From</a>
     * @see #fromFuture(Future, long, TimeUnit)
     * @see #fromCompletionStage(CompletionStage)
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public static <@NonNull T> Single<T> fromFuture(@NonNull Future<? extends T> future) {
        return toSingle(Flowable.fromFuture(future));
    }

    /**
     * Converts a {@link Future} into a {@code Single} and awaits its outcome, or timeout, in a blocking fashion.
     * <p>
     * <img width="640" height="315" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.from.Future.v3.png" alt="">
     * <p>
     * The operator calls {@link Future#get(long, TimeUnit)}, which is a blocking method, on the subscription thread.
     * It is recommended applying {@link #subscribeOn(Scheduler)} to move this blocking wait to a
     * background thread, and if the {@link Scheduler} supports it, interrupt the wait when the flow
     * is disposed.
     * <p>
     * A non-{@code null} value is then emitted via {@code onSuccess} or any exception is emitted via
     * {@code onError}. If the {@code Future} completes with {@code null}, a {@link NullPointerException}
     * is signaled.
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code fromFuture} does not operate by default on a particular {@code Scheduler}.</dd>
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
     *            the resulting {@code Single}
     * @return the new {@code Single} that emits the item from the source {@code Future}
     * @throws NullPointerException if {@code future} or {@code unit} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/from.html">ReactiveX operators documentation: From</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public static <@NonNull T> Single<T> fromFuture(@NonNull Future<? extends T> future, long timeout, @NonNull TimeUnit unit) {
        return toSingle(Flowable.fromFuture(future, timeout, unit));
    }

    /**
     * Returns a {@code Single} instance that when subscribed to, subscribes to the {@link MaybeSource} instance and
     * emits {@code onSuccess} as a single item, turns an {@code onComplete} into {@link NoSuchElementException} error signal or
     * forwards the {@code onError} signal.
     * <p>
     * <img width="640" height="241" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.fromMaybe.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code fromMaybe} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the value type of the {@code MaybeSource} element
     * @param maybe the {@code MaybeSource} instance to subscribe to, not {@code null}
     * @return the new {@code Single} instance
     * @throws NullPointerException if {@code maybe} is {@code null}
     * @since 3.0.0
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <@NonNull T> Single<T> fromMaybe(@NonNull MaybeSource<T> maybe) {
        Objects.requireNonNull(maybe, "maybe is null");
        return RxJavaPlugins.onAssembly(new MaybeToSingle<>(maybe, null));
    }

    /**
     * Returns a {@code Single} instance that when subscribed to, subscribes to the {@link MaybeSource} instance and
     * emits {@code onSuccess} as a single item, emits the {@code defaultItem} for an {@code onComplete} signal or
     * forwards the {@code onError} signal.
     * <p>
     * <img width="640" height="353" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.fromMaybe.v.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code fromMaybe} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the value type of the {@code MaybeSource} element
     * @param maybe the {@code MaybeSource} instance to subscribe to, not {@code null}
     * @param defaultItem the item to signal if the current {@code MaybeSource} is empty
     * @return the new {@code Single} instance
     * @throws NullPointerException if {@code maybe} or {@code defaultItem} is {@code null}
     * @since 3.0.0
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <@NonNull T> Single<T> fromMaybe(@NonNull MaybeSource<T> maybe, @NonNull T defaultItem) {
        Objects.requireNonNull(maybe, "maybe is null");
        Objects.requireNonNull(defaultItem, "defaultItem is null");
        return RxJavaPlugins.onAssembly(new MaybeToSingle<>(maybe, defaultItem));
    }

    /**
     * Wraps a specific {@link Publisher} into a {@code Single} and signals its single element or error.
     * <p>
     * <img width="640" height="322" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.fromPublisher.v3.png" alt="">
     * <p>
     * If the source {@code Publisher} is empty, a {@link NoSuchElementException} is signaled. If
     * the source has more than one element, an {@link IndexOutOfBoundsException} is signaled.
     * <p>
     * The {@code Publisher} must follow the
     * <a href="https://github.com/reactive-streams/reactive-streams-jvm#reactive-streams">Reactive Streams specification</a>.
     * Violating the specification may result in undefined behavior.
     * <p>
     * If possible, use {@link #create(SingleOnSubscribe)} to create a
     * source-like {@code Single} instead.
     * <p>
     * Note that even though {@code Publisher} appears to be a functional interface, it
     * is not recommended to implement it through a lambda as the specification requires
     * state management that is not achievable with a stateless lambda.
     * <dl>
     * <dt><b>Backpressure:</b></dt>
     * <dd>The {@code publisher} is consumed in an unbounded fashion but will be cancelled
     * if it produced more than one item.</dd>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code fromPublisher} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the value type
     * @param publisher the source {@code Publisher} instance, not {@code null}
     * @return the new {@code Single} instance
     * @throws NullPointerException if {@code publisher} is {@code null}
     * @see #create(SingleOnSubscribe)
     */
    @BackpressureSupport(BackpressureKind.UNBOUNDED_IN)
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <@NonNull T> Single<T> fromPublisher(@NonNull Publisher<? extends T> publisher) {
        Objects.requireNonNull(publisher, "publisher is null");
        return RxJavaPlugins.onAssembly(new SingleFromPublisher<>(publisher));
    }

    /**
     * Wraps a specific {@link ObservableSource} into a {@code Single} and signals its single element or error.
     * <p>
     * <img width="640" height="343" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.fromObservable.v3.png" alt="">
     * <p>
     * If the {@code ObservableSource} is empty, a {@link NoSuchElementException} is signaled.
     * If the source has more than one element, an {@link IndexOutOfBoundsException} is signaled.
     * <dl>
     *   <dt><b>Scheduler:</b></dt>
     *   <dd>{@code fromObservable} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param observable the source sequence to wrap, not {@code null}
     * @param <T>
     *         the type of the item emitted by the {@code Single}.
     * @return the new {@code Single} instance
     * @throws NullPointerException if {@code observable} is {@code null}
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <@NonNull T> Single<T> fromObservable(@NonNull ObservableSource<? extends T> observable) {
        Objects.requireNonNull(observable, "observable is null");
        return RxJavaPlugins.onAssembly(new ObservableSingleSingle<>(observable, null));
    }

    /**
     * Returns a {@code Single} that invokes passed supplier and emits its result
     * for each individual {@link SingleObserver} that subscribes.
     * <p>
     * Allows you to defer execution of passed function until a {@code SingleObserver} subscribes to the {@link Single}.
     * It makes passed function "lazy".
     * Result of the function invocation will be emitted by the {@link Single}.
     * <p>
     * <img width="640" height="467" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.fromSupplier.v3.png" alt="">
     * <dl>
     *   <dt><b>Scheduler:</b></dt>
     *   <dd>{@code fromSupplier} does not operate by default on a particular {@link Scheduler}.</dd>
     *   <dt><b>Error handling:</b></dt>
     *   <dd> If the {@link Supplier} throws an exception, the respective {@link Throwable} is
     *   delivered to the downstream via {@link SingleObserver#onError(Throwable)},
     *   except when the downstream has disposed this {@code Single} source.
     *   In this latter case, the {@code Throwable} is delivered to the global error handler via
     *   {@link RxJavaPlugins#onError(Throwable)} as an {@link io.reactivex.rxjava3.exceptions.UndeliverableException UndeliverableException}.
     *   </dd>
     * </dl>
     *
     * @param supplier
     *         function which execution should be deferred, it will be invoked when {@code SingleObserver} subscribes to the {@code Single}.
     * @param <T>
     *         the type of the item emitted by the {@code Single}.
     * @return the new {@code Single} whose {@code SingleObserver}s' subscriptions trigger an invocation of the given function.
     * @throws NullPointerException if {@code supplier} is {@code null}
     * @see #defer(Supplier)
     * @see #fromCallable(Callable)
     * @since 3.0.0
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <@NonNull T> Single<T> fromSupplier(@NonNull Supplier<? extends T> supplier) {
        Objects.requireNonNull(supplier, "supplier is null");
        return RxJavaPlugins.onAssembly(new SingleFromSupplier<>(supplier));
    }

    /**
     * Returns a {@code Single} that emits a specified item.
     * <p>
     * <img width="640" height="310" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.just.v3.png" alt="">
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
     * @return the new {@code Single} that emits {@code item}
     * @throws NullPointerException if {@code item} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/just.html">ReactiveX operators documentation: Just</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public static <@NonNull T> Single<T> just(T item) {
        Objects.requireNonNull(item, "item is null");
        return RxJavaPlugins.onAssembly(new SingleJust<>(item));
    }

    /**
     * Merges an {@link Iterable} sequence of {@link SingleSource} instances into a single {@link Flowable} sequence,
     * running all {@code SingleSource}s at once.
     * <p>
     * <img width="640" height="319" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.merge.i.v3.png" alt="">
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The returned {@code Flowable} honors the backpressure of the downstream consumer.</dd>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code merge} does not operate by default on a particular {@link Scheduler}.</dd>
     *  <dt><b>Error handling:</b></dt>
     *  <dd>If any of the source {@code SingleSource}s signal a {@link Throwable} via {@code onError}, the resulting
     *  {@code Flowable} terminates with that {@code Throwable} and all other source {@code SingleSource}s are disposed.
     *  If more than one {@code SingleSource} signals an error, the resulting {@code Flowable} may terminate with the
     *  first one's error or, depending on the concurrency of the sources, may terminate with a
     *  {@link CompositeException} containing two or more of the various error signals.
     *  {@code Throwable}s that didn't make into the composite will be sent (individually) to the global error handler via
     *  {@link RxJavaPlugins#onError(Throwable)} method as {@link UndeliverableException} errors. Similarly, {@code Throwable}s
     *  signaled by source(s) after the returned {@code Flowable} has been cancelled or terminated with a
     *  (composite) error will be sent to the same global error handler.
     *  Use {@link #mergeDelayError(Iterable)} to merge sources and terminate only when all source {@code SingleSource}s
     *  have completed or failed with an error.
     *  </dd>
     * </dl>
     * @param <T> the common and resulting value type
     * @param sources the {@code Iterable} sequence of {@code SingleSource} sources
     * @return the new {@code Flowable} instance
     * @throws NullPointerException if {@code sources} is {@code null}
     * @since 2.0
     * @see #mergeDelayError(Iterable)
     */
    @CheckReturnValue
    @NonNull
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <@NonNull T> Flowable<T> merge(@NonNull Iterable<@NonNull ? extends SingleSource<? extends T>> sources) {
        return Flowable.fromIterable(sources).flatMapSingle(Functions.identity());
    }

    /**
     * Merges a sequence of {@link SingleSource} instances emitted by a {@link Publisher} into a single {@link Flowable} sequence,
     * running all {@code SingleSource}s at once.
     * <p>
     * <img width="640" height="307" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.merge.p.v3.png" alt="">
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The returned {@code Flowable} honors the backpressure of the downstream consumer.</dd>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code merge} does not operate by default on a particular {@link Scheduler}.</dd>
     *  <dt><b>Error handling:</b></dt>
     *  <dd>If any of the source {@code SingleSource}s signal a {@link Throwable} via {@code onError}, the resulting
     *  {@code Flowable} terminates with that {@code Throwable} and all other source {@code SingleSource}s are disposed.
     *  If more than one {@code SingleSource} signals an error, the resulting {@code Flowable} may terminate with the
     *  first one's error or, depending on the concurrency of the sources, may terminate with a
     *  {@link CompositeException} containing two or more of the various error signals.
     *  {@code Throwable}s that didn't make into the composite will be sent (individually) to the global error handler via
     *  {@link RxJavaPlugins#onError(Throwable)} method as {@link UndeliverableException} errors. Similarly, {@code Throwable}s
     *  signaled by source(s) after the returned {@code Flowable} has been cancelled or terminated with a
     *  (composite) error will be sent to the same global error handler.
     *  Use {@link #mergeDelayError(Publisher)} to merge sources and terminate only when all source {@code SingleSource}s
     *  have completed or failed with an error.
     *  </dd>
     * </dl>
     * @param <T> the common and resulting value type
     * @param sources the {@code Publisher} emitting a sequence of {@code SingleSource}s
     * @return the new {@code Flowable} instance
     * @throws NullPointerException if {@code sources} is {@code null}
     * @see #mergeDelayError(Publisher)
     * @since 2.0
     */
    @CheckReturnValue
    @NonNull
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <@NonNull T> Flowable<T> merge(@NonNull Publisher<@NonNull ? extends SingleSource<? extends T>> sources) {
        Objects.requireNonNull(sources, "sources is null");
        return RxJavaPlugins.onAssembly(new FlowableFlatMapSinglePublisher<>(sources, Functions.identity(), false, Integer.MAX_VALUE));
    }

    /**
     * Flattens a {@link SingleSource} that emits a {@code SingleSingle} into a single {@code Single} that emits the item
     * emitted by the nested {@code SingleSource}, without any transformation.
     * <p>
     * <img width="640" height="412" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.merge.oo.v3.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code merge} does not operate by default on a particular {@link Scheduler}.</dd>
     * <dd>The resulting {@code Single} emits the outer source's or the inner {@code SingleSource}'s {@link Throwable} as is.
     * Unlike the other {@code merge()} operators, this operator won't and can't produce a {@link CompositeException} because there is
     * only one possibility for the outer or the inner {@code SingleSource} to emit an {@code onError} signal.
     * Therefore, there is no need for a {@code mergeDelayError(SingleSource<SingleSource<T>>)} operator.
     * </dd>
     * </dl>
     *
     * @param <T> the value type of the sources and the output
     * @param source
     *            a {@code Single} that emits a {@code Single}
     * @return the new {@code Single} that emits the item that is the result of flattening the {@code Single} emitted
     *         by {@code source}
     * @throws NullPointerException if {@code source} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/merge.html">ReactiveX operators documentation: Merge</a>
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <@NonNull T> Single<T> merge(@NonNull SingleSource<? extends SingleSource<? extends T>> source) {
        Objects.requireNonNull(source, "source is null");
        return RxJavaPlugins.onAssembly(new SingleFlatMap<SingleSource<? extends T>, T>(source, Functions.identity()));
    }

    /**
     * Flattens two {@link SingleSource}s into one {@link Flowable} sequence, without any transformation.
     * <p>
     * <img width="640" height="414" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.merge.v3.png" alt="">
     * <p>
     * You can combine items emitted by multiple {@code SingleSource}s so that they appear as a single {@code Flowable}, by
     * using the {@code merge} method.
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The returned {@code Flowable} honors the backpressure of the downstream consumer.</dd>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code merge} does not operate by default on a particular {@link Scheduler}.</dd>
     *  <dt><b>Error handling:</b></dt>
     *  <dd>If any of the source {@code SingleSource}s signal a {@link Throwable} via {@code onError}, the resulting
     *  {@code Flowable} terminates with that {@code Throwable} and all other source {@code SingleSource}s are disposed.
     *  If more than one {@code SingleSource} signals an error, the resulting {@code Flowable} may terminate with the
     *  first one's error or, depending on the concurrency of the sources, may terminate with a
     *  {@link CompositeException} containing two or more of the various error signals.
     *  {@code Throwable}s that didn't make into the composite will be sent (individually) to the global error handler via
     *  {@link RxJavaPlugins#onError(Throwable)} method as {@link UndeliverableException} errors. Similarly, {@code Throwable}s
     *  signaled by source(s) after the returned {@code Flowable} has been cancelled or terminated with a
     *  (composite) error will be sent to the same global error handler.
     *  Use {@link #mergeDelayError(SingleSource, SingleSource)} to merge sources and terminate only when all source {@code SingleSource}s
     *  have completed or failed with an error.
     *  </dd>
     * </dl>
     *
     * @param <T> the common value type
     * @param source1
     *            a {@code SingleSource} to be merged
     * @param source2
     *            a {@code SingleSource} to be merged
     * @return the new {@code Flowable} that emits all of the items emitted by the source {@code SingleSource}s
     * @throws NullPointerException if {@code source1} or {@code source2} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/merge.html">ReactiveX operators documentation: Merge</a>
     * @see #mergeDelayError(SingleSource, SingleSource)
     */
    @CheckReturnValue
    @NonNull
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <@NonNull T> Flowable<T> merge(
            @NonNull SingleSource<? extends T> source1, @NonNull SingleSource<? extends T> source2
     ) {
        Objects.requireNonNull(source1, "source1 is null");
        Objects.requireNonNull(source2, "source2 is null");
        return Flowable.fromArray(source1, source2).flatMapSingle(Functions.identity(), false, Integer.MAX_VALUE);
    }

    /**
     * Flattens three {@link SingleSource}s into one {@link Flowable} sequence, without any transformation.
     * <p>
     * <img width="640" height="366" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.merge.o3.v3.png" alt="">
     * <p>
     * You can combine items emitted by multiple {@code SingleSource}s so that they appear as a single {@code Flowable}, by
     * the {@code merge} method.
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The returned {@code Flowable} honors the backpressure of the downstream consumer.</dd>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code merge} does not operate by default on a particular {@link Scheduler}.</dd>
     *  <dt><b>Error handling:</b></dt>
     *  <dd>If any of the source {@code SingleSource}s signal a {@link Throwable} via {@code onError}, the resulting
     *  {@code Flowable} terminates with that {@code Throwable} and all other source {@code SingleSource}s are disposed.
     *  If more than one {@code SingleSource} signals an error, the resulting {@code Flowable} may terminate with the
     *  first one's error or, depending on the concurrency of the sources, may terminate with a
     *  {@link CompositeException} containing two or more of the various error signals.
     *  {@code Throwable}s that didn't make into the composite will be sent (individually) to the global error handler via
     *  {@link RxJavaPlugins#onError(Throwable)} method as {@link UndeliverableException} errors. Similarly, {@code Throwable}s
     *  signaled by source(s) after the returned {@code Flowable} has been cancelled or terminated with a
     *  (composite) error will be sent to the same global error handler.
     *  Use {@link #mergeDelayError(SingleSource, SingleSource, SingleSource)} to merge sources and terminate only when all source {@code SingleSource}s
     *  have completed or failed with an error.
     *  </dd>
     * </dl>
     *
     * @param <T> the common value type
     * @param source1
     *            a {@code SingleSource} to be merged
     * @param source2
     *            a {@code SingleSource} to be merged
     * @param source3
     *            a {@code SingleSource} to be merged
     * @return the new {@code Flowable} that emits all of the items emitted by the source {@code SingleSource}s
     * @throws NullPointerException if {@code source1}, {@code source2} or {@code source3} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/merge.html">ReactiveX operators documentation: Merge</a>
     * @see #mergeDelayError(SingleSource, SingleSource, SingleSource)
     */
    @CheckReturnValue
    @NonNull
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <@NonNull T> Flowable<T> merge(
            @NonNull SingleSource<? extends T> source1, @NonNull SingleSource<? extends T> source2,
            @NonNull SingleSource<? extends T> source3
     ) {
        Objects.requireNonNull(source1, "source1 is null");
        Objects.requireNonNull(source2, "source2 is null");
        Objects.requireNonNull(source3, "source3 is null");
        return Flowable.fromArray(source1, source2, source3).flatMapSingle(Functions.identity(), false, Integer.MAX_VALUE);
    }

    /**
     * Flattens four {@link SingleSource}s into one {@link Flowable} sequence, without any transformation.
     * <p>
     * <img width="640" height="362" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.merge.o4.v3.png" alt="">
     * <p>
     * You can combine items emitted by multiple {@code SingleSource}s so that they appear as a single {@code Flowable}, by
     * the {@code merge} method.
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The returned {@code Flowable} honors the backpressure of the downstream consumer.</dd>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code merge} does not operate by default on a particular {@link Scheduler}.</dd>
     *  <dt><b>Error handling:</b></dt>
     *  <dd>If any of the source {@code SingleSource}s signal a {@link Throwable} via {@code onError}, the resulting
     *  {@code Flowable} terminates with that {@code Throwable} and all other source {@code SingleSource}s are disposed.
     *  If more than one {@code SingleSource} signals an error, the resulting {@code Flowable} may terminate with the
     *  first one's error or, depending on the concurrency of the sources, may terminate with a
     *  {@link CompositeException} containing two or more of the various error signals.
     *  {@code Throwable}s that didn't make into the composite will be sent (individually) to the global error handler via
     *  {@link RxJavaPlugins#onError(Throwable)} method as {@link UndeliverableException} errors. Similarly, {@code Throwable}s
     *  signaled by source(s) after the returned {@code Flowable} has been cancelled or terminated with a
     *  (composite) error will be sent to the same global error handler.
     *  Use {@link #mergeDelayError(SingleSource, SingleSource, SingleSource, SingleSource)} to merge sources and terminate only when all source {@code SingleSource}s
     *  have completed or failed with an error.
     *  </dd>
     * </dl>
     *
     * @param <T> the common value type
     * @param source1
     *            a {@code SingleSource} to be merged
     * @param source2
     *            a {@code SingleSource} to be merged
     * @param source3
     *            a {@code SingleSource} to be merged
     * @param source4
     *            a {@code SingleSource} to be merged
     * @return the new {@code Flowable} that emits all of the items emitted by the source {@code SingleSource}s
     * @throws NullPointerException if {@code source1}, {@code source2}, {@code source3} or {@code source4} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/merge.html">ReactiveX operators documentation: Merge</a>
     * @see #mergeDelayError(SingleSource, SingleSource, SingleSource, SingleSource)
     */
    @CheckReturnValue
    @NonNull
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <@NonNull T> Flowable<T> merge(
            @NonNull SingleSource<? extends T> source1, @NonNull SingleSource<? extends T> source2,
            @NonNull SingleSource<? extends T> source3, @NonNull SingleSource<? extends T> source4
     ) {
        Objects.requireNonNull(source1, "source1 is null");
        Objects.requireNonNull(source2, "source2 is null");
        Objects.requireNonNull(source3, "source3 is null");
        Objects.requireNonNull(source4, "source4 is null");
        return Flowable.fromArray(source1, source2, source3, source4).flatMapSingle(Functions.identity(), false, Integer.MAX_VALUE);
    }

    /**
     * Merges an array of {@link SingleSource} instances into a single {@link Flowable} sequence,
     * running all {@code SingleSource}s at once.
     * <p>
     * <img width="640" height="272" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.mergeArray.png" alt="">
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The operator honors backpressure from downstream.</dd>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code mergeArray} does not operate by default on a particular {@link Scheduler}.</dd>
     *  <dt><b>Error handling:</b></dt>
     *  <dd>If any of the source {@code SingleSource}s signal a {@link Throwable} via {@code onError}, the resulting
     *  {@code Flowable} terminates with that {@code Throwable} and all other source {@code SingleSource}s are disposed.
     *  If more than one {@code SingleSource} signals an error, the resulting {@code Flowable} may terminate with the
     *  first one's error or, depending on the concurrency of the sources, may terminate with a
     *  {@link CompositeException} containing two or more of the various error signals.
     *  {@code Throwable}s that didn't make into the composite will be sent (individually) to the global error handler via
     *  {@link RxJavaPlugins#onError(Throwable)} method as {@link UndeliverableException} errors. Similarly, {@code Throwable}s
     *  signaled by source(s) after the returned {@code Flowable} has been cancelled or terminated with a
     *  (composite) error will be sent to the same global error handler.
     *  Use {@link #mergeArrayDelayError(SingleSource...)} to merge sources and terminate only when all source {@code SingleSource}s
     *  have completed or failed with an error.
     *  </dd>
     * </dl>
     * @param <T> the common and resulting value type
     * @param sources the array sequence of {@code SingleSource} sources
     * @return the new {@code Flowable} instance
     * @throws NullPointerException if {@code sources} is {@code null}
     * @see #mergeArrayDelayError(SingleSource...)
     */
    @BackpressureSupport(BackpressureKind.FULL)
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    @SafeVarargs
    public static <@NonNull T> Flowable<T> mergeArray(SingleSource<? extends T>... sources) {
        return Flowable.fromArray(sources).flatMapSingle(Functions.identity(), false, Math.max(1, sources.length));
    }

    /**
     * Flattens an array of {@link SingleSource}s into one {@link Flowable}, in a way that allows a subscriber to receive all
     * successfully emitted items from each of the source {@code SingleSource}s without being interrupted by an error
     * notification from one of them.
     * <p>
     * <img width="640" height="422" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.mergeArrayDelayError.png" alt="">
     * <p>
     * This behaves like {@link #merge(Publisher)} except that if any of the merged {@code SingleSource}s notify of an
     * error via {@link Subscriber#onError onError}, {@code mergeArrayDelayError} will refrain from propagating that
     * error notification until all of the merged {@code SingleSource}s have finished emitting items.
     * <p>
     * Even if multiple merged {@code SingleSource}s send {@code onError} notifications, {@code mergeArrayDelayError} will only
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
     *            the array of {@code SingleSource}s
     * @return the new {@code Flowable} instance
     * @throws NullPointerException if {@code sources} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/merge.html">ReactiveX operators documentation: Merge</a>
     */
    @BackpressureSupport(BackpressureKind.FULL)
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @SafeVarargs
    @NonNull
    public static <@NonNull T> Flowable<T> mergeArrayDelayError(@NonNull SingleSource<? extends T>... sources) {
        return Flowable.fromArray(sources).flatMapSingle(Functions.identity(), true, Math.max(1, sources.length));
    }

    /**
     * Merges an {@link Iterable} sequence of {@link SingleSource} instances into one {@link Flowable} sequence,
     * running all {@code SingleSource}s at once and delaying any error(s) until all sources succeed or fail.
     * <p>
     * <img width="640" height="469" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.mergeDelayError.i.png" alt="">
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The returned {@code Flowable} honors the backpressure of the downstream consumer.</dd>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code mergeDelayError} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * <p>History: 2.1.9 - experimental
     * @param <T> the common and resulting value type
     * @param sources the {@code Iterable} sequence of {@code SingleSource}s
     * @return the new {@code Flowable} instance
     * @throws NullPointerException if {@code sources} is {@code null}
     * @see #merge(Iterable)
     * @since 2.2
     */
    @CheckReturnValue
    @NonNull
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <@NonNull T> Flowable<T> mergeDelayError(@NonNull Iterable<@NonNull ? extends SingleSource<? extends T>> sources) {
        return Flowable.fromIterable(sources).flatMapSingle(Functions.identity(), true, Integer.MAX_VALUE);
    }

    /**
     * Merges a sequence of {@link SingleSource} instances emitted by a {@link Publisher} into a {@link Flowable} sequence,
     * running all {@code SingleSource}s at once and delaying any error(s) until all sources succeed or fail.
     * <p>
     * <img width="640" height="356" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.mergeDelayError.p.png" alt="">
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The returned {@code Flowable} honors the backpressure of the downstream consumer.</dd>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code mergeDelayError} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * <p>History: 2.1.9 - experimental
     * @param <T> the common and resulting value type
     * @param sources the {@code Flowable} sequence of {@code SingleSource}s
     * @return the new {@code Flowable} instance
     * @throws NullPointerException if {@code sources} is {@code null}
     * @since 2.2
     * @see #merge(Publisher)
     */
    @CheckReturnValue
    @NonNull
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <@NonNull T> Flowable<T> mergeDelayError(@NonNull Publisher<@NonNull ? extends SingleSource<? extends T>> sources) {
        Objects.requireNonNull(sources, "sources is null");
        return RxJavaPlugins.onAssembly(new FlowableFlatMapSinglePublisher<>(sources, Functions.identity(), true, Integer.MAX_VALUE));
    }

    /**
     * Flattens two {@link SingleSource}s into one {@link Flowable}, without any transformation, delaying
     * any error(s) until all sources succeed or fail.
     * <p>
     * <img width="640" height="554" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.mergeDelayError.2.png" alt="">
     * <p>
     * You can combine items emitted by multiple {@code SingleSource}s so that they appear as one {@code Flowable}, by
     * using the {@code mergeDelayError} method.
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The returned {@code Flowable} honors the backpressure of the downstream consumer.</dd>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code mergeDelayError} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * <p>History: 2.1.9 - experimental
     * @param <T> the common value type
     * @param source1
     *            a {@code SingleSource} to be merged
     * @param source2
     *            a {@code SingleSource} to be merged
     * @return the new {@code Flowable} that emits all of the items emitted by the source {@code SingleSource}s
     * @throws NullPointerException if {@code source1} or {@code source2} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/merge.html">ReactiveX operators documentation: Merge</a>
     * @see #merge(SingleSource, SingleSource)
     * @since 2.2
     */
    @CheckReturnValue
    @NonNull
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <@NonNull T> Flowable<T> mergeDelayError(
            @NonNull SingleSource<? extends T> source1, @NonNull SingleSource<? extends T> source2
     ) {
        Objects.requireNonNull(source1, "source1 is null");
        Objects.requireNonNull(source2, "source2 is null");
        return Flowable.fromArray(source1, source2).flatMapSingle(Functions.identity(), true, Integer.MAX_VALUE);
    }

    /**
     * Flattens two {@link SingleSource}s into one {@link Flowable}, without any transformation, delaying
     * any error(s) until all sources succeed or fail.
     * <p>
     * <img width="640" height="496" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.mergeDelayError.3.png" alt="">
     * <p>
     * You can combine items emitted by multiple {@code SingleSource}s so that they appear as one {@code Flowable}, by
     * the {@code mergeDelayError} method.
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The returned {@code Flowable} honors the backpressure of the downstream consumer.</dd>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code mergeDelayError} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * <p>History: 2.1.9 - experimental
     * @param <T> the common value type
     * @param source1
     *            a {@code SingleSource} to be merged
     * @param source2
     *            a {@code SingleSource} to be merged
     * @param source3
     *            a {@code SingleSource} to be merged
     * @return the new {@code Flowable} that emits all of the items emitted by the source {@code SingleSource}s
     * @throws NullPointerException if {@code source1}, {@code source2} or {@code source3} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/merge.html">ReactiveX operators documentation: Merge</a>
     * @see #merge(SingleSource, SingleSource, SingleSource)
     * @since 2.2
     */
    @CheckReturnValue
    @NonNull
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <@NonNull T> Flowable<T> mergeDelayError(
            @NonNull SingleSource<? extends T> source1, @NonNull SingleSource<? extends T> source2,
            @NonNull SingleSource<? extends T> source3
     ) {
        Objects.requireNonNull(source1, "source1 is null");
        Objects.requireNonNull(source2, "source2 is null");
        Objects.requireNonNull(source3, "source3 is null");
        return Flowable.fromArray(source1, source2, source3).flatMapSingle(Functions.identity(), true, Integer.MAX_VALUE);
    }

    /**
     * Flattens two {@link SingleSource}s into one {@link Flowable}, without any transformation, delaying
     * any error(s) until all sources succeed or fail.
     * <p>
     * <img width="640" height="509" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.mergeDelayError.4.png" alt="">
     * <p>
     * You can combine items emitted by multiple {@code SingleSource}s so that they appear as one {@code Flowable}, by
     * the {@code mergeDelayError} method.
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The returned {@code Flowable} honors the backpressure of the downstream consumer.</dd>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code mergeDelayError} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * <p>History: 2.1.9 - experimental
     * @param <T> the common value type
     * @param source1
     *            a {@code SingleSource} to be merged
     * @param source2
     *            a {@code SingleSource} to be merged
     * @param source3
     *            a {@code SingleSource} to be merged
     * @param source4
     *            a {@code SingleSource} to be merged
     * @return the new {@code Flowable} that emits all of the items emitted by the source {@code SingleSource}s
     * @throws NullPointerException if {@code source1}, {@code source2}, {@code source3} or {@code source4} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/merge.html">ReactiveX operators documentation: Merge</a>
     * @see #merge(SingleSource, SingleSource, SingleSource, SingleSource)
     * @since 2.2
     */
    @CheckReturnValue
    @NonNull
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <@NonNull T> Flowable<T> mergeDelayError(
            @NonNull SingleSource<? extends T> source1, @NonNull SingleSource<? extends T> source2,
            @NonNull SingleSource<? extends T> source3, @NonNull SingleSource<? extends T> source4
     ) {
        Objects.requireNonNull(source1, "source1 is null");
        Objects.requireNonNull(source2, "source2 is null");
        Objects.requireNonNull(source3, "source3 is null");
        Objects.requireNonNull(source4, "source4 is null");
        return Flowable.fromArray(source1, source2, source3, source4).flatMapSingle(Functions.identity(), true, Integer.MAX_VALUE);
    }

    /**
     * Returns a singleton instance of a never-signaling {@code Single} (only calls {@code onSubscribe}).
     * <p>
     * <img width="640" height="244" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.never.v3.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code never} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the target value type
     * @return the singleton never instance
     * @since 2.0
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @SuppressWarnings("unchecked")
    @NonNull
    public static <@NonNull T> Single<T> never() {
        return RxJavaPlugins.onAssembly((Single<T>) SingleNever.INSTANCE);
    }

    /**
     * Signals success with 0L value after the given delay when a {@link SingleObserver} subscribes.
     * <p>
     * <img width="640" height="292" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.timer.v3.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code timer} operates by default on the {@code computation} {@link Scheduler}.</dd>
     * </dl>
     * @param delay the delay amount
     * @param unit the time unit of the delay
     * @return the new {@code Single} instance
     * @throws NullPointerException if {@code unit} is {@code null}
     * @since 2.0
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.COMPUTATION)
    @NonNull
    public static Single<Long> timer(long delay, @NonNull TimeUnit unit) {
        return timer(delay, unit, Schedulers.computation());
    }

    /**
     * Signals success with 0L value on the specified {@link Scheduler} after the given
     * delay when a {@link SingleObserver} subscribes.
     * <p>
     * <img width="640" height="292" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.timer.s.v3.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>you specify the {@code Scheduler} to signal on.</dd>
     * </dl>
     * @param delay the delay amount
     * @param unit the time unit of the delay
     * @param scheduler the {@code Scheduler} where the single 0L will be emitted
     * @return the new {@code Single} instance
     * @throws NullPointerException
     *             if {@code unit} is {@code null}, or
     *             if {@code scheduler} is {@code null}
     * @since 2.0
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    public static Single<Long> timer(long delay, @NonNull TimeUnit unit, @NonNull Scheduler scheduler) {
        Objects.requireNonNull(unit, "unit is null");
        Objects.requireNonNull(scheduler, "scheduler is null");
        return RxJavaPlugins.onAssembly(new SingleTimer(delay, unit, scheduler));
    }

    /**
     * Compares two {@link SingleSource}s and emits {@code true} if they emit the same value (compared via {@link Object#equals(Object)}).
     * <p>
     * <img width="640" height="465" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.equals.v3.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code sequenceEqual} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the common value type
     * @param source1 the first {@code SingleSource} instance
     * @param source2 the second {@code SingleSource} instance
     * @return the new {@code Single} instance
     * @throws NullPointerException if {@code source1} or {@code source2} is {@code null}
     * @since 2.0
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <@NonNull T> Single<Boolean> sequenceEqual(@NonNull SingleSource<? extends T> source1, @NonNull SingleSource<? extends T> source2) { // NOPMD
        Objects.requireNonNull(source1, "source1 is null");
        Objects.requireNonNull(source2, "source2 is null");
        return RxJavaPlugins.onAssembly(new SingleEquals<>(source1, source2));
    }

    /**
     * Switches between {@link SingleSource}s emitted by the source {@link Publisher} whenever
     * a new {@code SingleSource} is emitted, disposing the previously running {@code SingleSource},
     * exposing the success items as a {@link Flowable} sequence.
     * <p>
     * <img width="640" height="521" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.switchOnNext.png" alt="">
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The {@code sources} {@code Publisher} is consumed in an unbounded manner (requesting {@link Long#MAX_VALUE}).
     *  The returned {@code Flowable} respects the backpressure from the downstream.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code switchOnNext} does not operate by default on a particular {@link Scheduler}.</dd>
     *  <dt><b>Error handling:</b></dt>
     *  <dd>The returned sequence fails with the first error signaled by the {@code sources} {@code Publisher}
     *  or the currently running {@code SingleSource}, disposing the rest. Late errors are
     *  forwarded to the global error handler via {@link RxJavaPlugins#onError(Throwable)}.</dd>
     * </dl>
     * @param <T> the element type of the {@code SingleSource}s
     * @param sources the {@code Publisher} sequence of inner {@code SingleSource}s to switch between
     * @return the new {@code Flowable} instance
     * @throws NullPointerException if {@code sources} is {@code null}
     * @since 3.0.0
     * @see #switchOnNextDelayError(Publisher)
     * @see <a href="http://reactivex.io/documentation/operators/switch.html">ReactiveX operators documentation: Switch</a>
     */
    @BackpressureSupport(BackpressureKind.FULL)
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <@NonNull T> Flowable<T> switchOnNext(@NonNull Publisher<@NonNull ? extends SingleSource<? extends T>> sources) {
        Objects.requireNonNull(sources, "sources is null");
        return RxJavaPlugins.onAssembly(new FlowableSwitchMapSinglePublisher<>(sources, Functions.identity(), false));
    }

    /**
     * Switches between {@link SingleSource}s emitted by the source {@link Publisher} whenever
     * a new {@code SingleSource} is emitted, disposing the previously running {@code SingleSource},
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
     *  {@code Publisher} or any inner {@code SingleSource} and emits them as a {@link CompositeException}
     *  when all sources terminate. If only one source ever failed, its error is emitted as-is at the end.</dd>
     * </dl>
     * @param <T> the element type of the {@code SingleSource}s
     * @param sources the {@code Publisher} sequence of inner {@code SingleSource}s to switch between
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
    public static <@NonNull T> Flowable<T> switchOnNextDelayError(@NonNull Publisher<@NonNull ? extends SingleSource<? extends T>> sources) {
        Objects.requireNonNull(sources, "sources is null");
        return RxJavaPlugins.onAssembly(new FlowableSwitchMapSinglePublisher<>(sources, Functions.identity(), true));
    }

    /**
     * <strong>Advanced use only:</strong> creates a {@code Single} instance without
     * any safeguards by using a callback that is called with a {@link SingleObserver}.
     * <p>
     * <img width="640" height="261" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.unsafeCreate.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code unsafeCreate} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the value type
     * @param onSubscribe the function that is called with the subscribing {@code SingleObserver}
     * @return the new {@code Single} instance
     * @throws NullPointerException if {@code onSubscribe} is {@code null}
     * @throws IllegalArgumentException if {@code source} is a subclass of {@code Single}; such
     * instances don't need conversion and is possibly a port remnant from 1.x or one should use {@link #hide()}
     * instead.
     * @since 2.0
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <@NonNull T> Single<T> unsafeCreate(@NonNull SingleSource<T> onSubscribe) {
        Objects.requireNonNull(onSubscribe, "onSubscribe is null");
        if (onSubscribe instanceof Single) {
            throw new IllegalArgumentException("unsafeCreate(Single) should be upgraded");
        }
        return RxJavaPlugins.onAssembly(new SingleFromUnsafeSource<>(onSubscribe));
    }

    /**
     * Allows using and disposing a resource while running a {@link SingleSource} instance generated from
     * that resource (similar to a try-with-resources).
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.using.v3.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code using} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the value type of the {@code SingleSource} generated
     * @param <U> the resource type
     * @param resourceSupplier the {@link Supplier} called for each {@link SingleObserver} to generate a resource object
     * @param sourceSupplier the function called with the returned resource
     *                  object from {@code resourceSupplier} and should return a {@code SingleSource} instance
     *                  to be run by the operator
     * @param resourceCleanup the consumer of the generated resource that is called exactly once for
     *                  that particular resource when the generated {@code SingleSource} terminates
     *                  (successfully or with an error) or gets disposed.
     * @return the new {@code Single} instance
     * @throws NullPointerException if {@code resourceSupplier}, {@code sourceSupplier} and {@code resourceCleanup} is {@code null}
     * @since 2.0
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public static <@NonNull T, @NonNull U> Single<T> using(@NonNull Supplier<U> resourceSupplier,
            @NonNull Function<? super U, ? extends SingleSource<? extends T>> sourceSupplier,
            @NonNull Consumer<? super U> resourceCleanup) {
        return using(resourceSupplier, sourceSupplier, resourceCleanup, true);
    }

    /**
     * Allows using and disposing a resource while running a {@link SingleSource} instance generated from
     * that resource (similar to a try-with-resources).
     * <p>
     * <img width="640" height="325" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.using.b.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code using} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the value type of the {@code SingleSource} generated
     * @param <U> the resource type
     * @param resourceSupplier the {@link Supplier} called for each {@link SingleObserver} to generate a resource object
     * @param sourceSupplier the function called with the returned resource
     *                  object from {@code resourceSupplier} and should return a {@code SingleSource} instance
     *                  to be run by the operator
     * @param resourceCleanup the consumer of the generated resource that is called exactly once for
     *                  that particular resource when the generated {@code SingleSource} terminates
     *                  (successfully or with an error) or gets disposed.
     * @param eager
     *            If {@code true} then resource disposal will happen either on a {@code dispose()} call before the upstream is disposed
     *            or just before the emission of a terminal event ({@code onSuccess} or {@code onError}).
     *            If {@code false} the resource disposal will happen either on a {@code dispose()} call after the upstream is disposed
     *            or just after the emission of a terminal event ({@code onSuccess} or {@code onError}).
     * @return the new {@code Single} instance
     * @throws NullPointerException if {@code resourceSupplier}, {@code sourceSupplier} or {@code resourceCleanup} is {@code null}
     * @since 2.0
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <@NonNull T, @NonNull U> Single<T> using(
            @NonNull Supplier<U> resourceSupplier,
            @NonNull Function<? super U, ? extends SingleSource<? extends T>> sourceSupplier,
            @NonNull Consumer<? super U> resourceCleanup,
            boolean eager) {
        Objects.requireNonNull(resourceSupplier, "resourceSupplier is null");
        Objects.requireNonNull(sourceSupplier, "sourceSupplier is null");
        Objects.requireNonNull(resourceCleanup, "resourceCleanup is null");

        return RxJavaPlugins.onAssembly(new SingleUsing<>(resourceSupplier, sourceSupplier, resourceCleanup, eager));
    }

    /**
     * Wraps a {@link SingleSource} instance into a new {@code Single} instance if not already a {@code Single}
     * instance.
     * <p>
     * <img width="640" height="350" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.wrap.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code wrap} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the value type
     * @param source the source to wrap
     * @return the new {@code Single} instance
     * @throws NullPointerException if {@code source} is {@code null}
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <@NonNull T> Single<T> wrap(@NonNull SingleSource<T> source) {
        Objects.requireNonNull(source, "source is null");
        if (source instanceof Single) {
            return RxJavaPlugins.onAssembly((Single<T>)source);
        }
        return RxJavaPlugins.onAssembly(new SingleFromUnsafeSource<>(source));
    }

    /**
     * Waits until all {@link SingleSource} sources provided by the {@link Iterable} sequence signal a success
     * value and calls a zipper function with an array of these values to return a result
     * to be emitted to the downstream.
     * <p>
     * If the {@code Iterable} of {@code SingleSource}s is empty a {@link NoSuchElementException} error is signaled after subscription.
     * <p>
     * Note on method signature: since Java doesn't allow creating a generic array with {@code new T[]}, the
     * implementation of this operator has to create an {@code Object[]} instead. Unfortunately, a
     * {@code Function<Integer[], R>} passed to the method would trigger a {@link ClassCastException}.
     *
     * <p>
     * <img width="640" height="340" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.zip.i.png" alt="">
     * <p>
     * If any of the {@code SingleSources} signal an error, all other {@code SingleSource}s get disposed and the
     * error emitted to downstream immediately.
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code zip} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the common value type
     * @param <R> the result value type
     * @param sources the {@code Iterable} sequence of {@code SingleSource} instances. An empty sequence will result in an
     *                {@code onError} signal of {@code NoSuchElementException}.
     * @param zipper the function that receives an array with values from each {@code SingleSource}
     *               and should return a value to be emitted to downstream
     * @return the new {@code Single} instance
     * @throws NullPointerException if {@code zipper} or {@code sources} is {@code null}
     * @since 2.0
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <@NonNull T, @NonNull R> Single<R> zip(@NonNull Iterable<@NonNull ? extends SingleSource<? extends T>> sources,
            @NonNull Function<? super Object[], ? extends R> zipper) {
        Objects.requireNonNull(zipper, "zipper is null");
        Objects.requireNonNull(sources, "sources is null");
        return RxJavaPlugins.onAssembly(new SingleZipIterable<>(sources, zipper));
    }

    /**
     * Returns a {@code Single} that emits the results of a specified combiner function applied to two items emitted by
     * two other {@link SingleSource}s.
     * <p>
     * <img width="640" height="434" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.zip.n.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code zip} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T1> the first source {@code SingleSource}'s value type
     * @param <T2> the second source {@code SingleSource}'s value type
     * @param <R> the result value type
     * @param source1
     *            the first source {@code SingleSource}
     * @param source2
     *            a second source {@code SingleSource}
     * @param zipper
     *            a function that, when applied to the item emitted by each of the source {@code SingleSource}s, results in an
     *            item that will be emitted by the resulting {@code Single}
     * @return the new {@code Single} that emits the zipped results
     * @throws NullPointerException if {@code source1}, {@code source2} or {@code zipper} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/zip.html">ReactiveX operators documentation: Zip</a>
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <@NonNull T1, @NonNull T2, @NonNull R> Single<R> zip(
            @NonNull SingleSource<? extends T1> source1, @NonNull SingleSource<? extends T2> source2,
            @NonNull BiFunction<? super T1, ? super T2, ? extends R> zipper
     ) {
        Objects.requireNonNull(source1, "source1 is null");
        Objects.requireNonNull(source2, "source2 is null");
        Objects.requireNonNull(zipper, "zipper is null");
        return zipArray(Functions.toFunction(zipper), source1, source2);
    }

    /**
     * Returns a {@code Single} that emits the results of a specified combiner function applied to three items emitted
     * by three other {@link SingleSource}s.
     * <p>
     * <img width="640" height="434" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.zip.n.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code zip} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T1> the first source {@code SingleSource}'s value type
     * @param <T2> the second source {@code SingleSource}'s value type
     * @param <T3> the third source {@code SingleSource}'s value type
     * @param <R> the result value type
     * @param source1
     *            the first source {@code SingleSource}
     * @param source2
     *            a second source {@code SingleSource}
     * @param source3
     *            a third source {@code SingleSource}
     * @param zipper
     *            a function that, when applied to the item emitted by each of the source {@code SingleSource}s, results in an
     *            item that will be emitted by the resulting {@code Single}
     * @return the new {@code Single} that emits the zipped results
     * @throws NullPointerException if {@code source1}, {@code source2}, {@code source3} or {@code zipper} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/zip.html">ReactiveX operators documentation: Zip</a>
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <@NonNull T1, @NonNull T2, @NonNull T3, @NonNull R> Single<R> zip(
            @NonNull SingleSource<? extends T1> source1, @NonNull SingleSource<? extends T2> source2,
            @NonNull SingleSource<? extends T3> source3,
            @NonNull Function3<? super T1, ? super T2, ? super T3, ? extends R> zipper
     ) {
        Objects.requireNonNull(source1, "source1 is null");
        Objects.requireNonNull(source2, "source2 is null");
        Objects.requireNonNull(source3, "source3 is null");
        Objects.requireNonNull(zipper, "zipper is null");
        return zipArray(Functions.toFunction(zipper), source1, source2, source3);
    }

    /**
     * Returns a {@code Single} that emits the results of a specified combiner function applied to four items
     * emitted by four other {@link SingleSource}s.
     * <p>
     * <img width="640" height="434" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.zip.n.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code zip} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T1> the first source {@code SingleSource}'s value type
     * @param <T2> the second source {@code SingleSource}'s value type
     * @param <T3> the third source {@code SingleSource}'s value type
     * @param <T4> the fourth source {@code SingleSource}'s value type
     * @param <R> the result value type
     * @param source1
     *            the first source {@code SingleSource}
     * @param source2
     *            a second source {@code SingleSource}
     * @param source3
     *            a third source {@code SingleSource}
     * @param source4
     *            a fourth source {@code SingleSource}
     * @param zipper
     *            a function that, when applied to the item emitted by each of the source {@code SingleSource}s, results in an
     *            item that will be emitted by the resulting {@code Single}
     * @return the new {@code Single} that emits the zipped results
     * @throws NullPointerException if {@code source1}, {@code source2}, {@code source3}, {@code source4} or {@code zipper} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/zip.html">ReactiveX operators documentation: Zip</a>
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <@NonNull T1, @NonNull T2, @NonNull T3, @NonNull T4, @NonNull R> Single<R> zip(
            @NonNull SingleSource<? extends T1> source1, @NonNull SingleSource<? extends T2> source2,
            @NonNull SingleSource<? extends T3> source3, @NonNull SingleSource<? extends T4> source4,
            @NonNull Function4<? super T1, ? super T2, ? super T3, ? super T4, ? extends R> zipper
     ) {
        Objects.requireNonNull(source1, "source1 is null");
        Objects.requireNonNull(source2, "source2 is null");
        Objects.requireNonNull(source3, "source3 is null");
        Objects.requireNonNull(source4, "source4 is null");
        Objects.requireNonNull(zipper, "zipper is null");
        return zipArray(Functions.toFunction(zipper), source1, source2, source3, source4);
    }

    /**
     * Returns a {@code Single} that emits the results of a specified combiner function applied to five items
     * emitted by five other {@link SingleSource}s.
     * <p>
     * <img width="640" height="434" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.zip.n.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code zip} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T1> the first source {@code SingleSource}'s value type
     * @param <T2> the second source {@code SingleSource}'s value type
     * @param <T3> the third source {@code SingleSource}'s value type
     * @param <T4> the fourth source {@code SingleSource}'s value type
     * @param <T5> the fifth source {@code SingleSource}'s value type
     * @param <R> the result value type
     * @param source1
     *            the first source {@code SingleSource}
     * @param source2
     *            a second source {@code SingleSource}
     * @param source3
     *            a third source {@code SingleSource}
     * @param source4
     *            a fourth source {@code SingleSource}
     * @param source5
     *            a fifth source {@code SingleSource}
     * @param zipper
     *            a function that, when applied to the item emitted by each of the source {@code SingleSource}s, results in an
     *            item that will be emitted by the resulting {@code Single}
     * @return the new {@code Single} that emits the zipped results
     * @throws NullPointerException if {@code source1}, {@code source2}, {@code source3}, {@code source4}
     *                              {@code source5} or {@code zipper} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/zip.html">ReactiveX operators documentation: Zip</a>
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <@NonNull T1, @NonNull T2, @NonNull T3, @NonNull T4, @NonNull T5, @NonNull R> Single<R> zip(
            @NonNull SingleSource<? extends T1> source1, @NonNull SingleSource<? extends T2> source2,
            @NonNull SingleSource<? extends T3> source3, @NonNull SingleSource<? extends T4> source4,
            @NonNull SingleSource<? extends T5> source5,
            @NonNull Function5<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? extends R> zipper
     ) {
        Objects.requireNonNull(source1, "source1 is null");
        Objects.requireNonNull(source2, "source2 is null");
        Objects.requireNonNull(source3, "source3 is null");
        Objects.requireNonNull(source4, "source4 is null");
        Objects.requireNonNull(source5, "source5 is null");
        Objects.requireNonNull(zipper, "zipper is null");
        return zipArray(Functions.toFunction(zipper), source1, source2, source3, source4, source5);
    }

    /**
     * Returns a {@code Single} that emits the results of a specified combiner function applied to six items
     * emitted by six other {@link SingleSource}s.
     * <p>
     * <img width="640" height="434" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.zip.n.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code zip} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T1> the first source {@code SingleSource}'s value type
     * @param <T2> the second source {@code SingleSource}'s value type
     * @param <T3> the third source {@code SingleSource}'s value type
     * @param <T4> the fourth source {@code SingleSource}'s value type
     * @param <T5> the fifth source {@code SingleSource}'s value type
     * @param <T6> the sixth source {@code SingleSource}'s value type
     * @param <R> the result value type
     * @param source1
     *            the first source {@code SingleSource}
     * @param source2
     *            a second source {@code SingleSource}
     * @param source3
     *            a third source {@code SingleSource}
     * @param source4
     *            a fourth source {@code SingleSource}
     * @param source5
     *            a fifth source {@code SingleSource}
     * @param source6
     *            a sixth source {@code SingleSource}
     * @param zipper
     *            a function that, when applied to the item emitted by each of the source {@code SingleSource}s, results in an
     *            item that will be emitted by the resulting {@code Single}
     * @return the new {@code Single} that emits the zipped results
     * @throws NullPointerException if {@code source1}, {@code source2}, {@code source3}, {@code source4}
     *                              {@code source5}, {@code source6} or {@code zipper} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/zip.html">ReactiveX operators documentation: Zip</a>
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <@NonNull T1, @NonNull T2, @NonNull T3, @NonNull T4, @NonNull T5, @NonNull T6, @NonNull R> Single<R> zip(
            @NonNull SingleSource<? extends T1> source1, @NonNull SingleSource<? extends T2> source2,
            @NonNull SingleSource<? extends T3> source3, @NonNull SingleSource<? extends T4> source4,
            @NonNull SingleSource<? extends T5> source5, @NonNull SingleSource<? extends T6> source6,
            @NonNull Function6<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? extends R> zipper
     ) {
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
     * Returns a {@code Single} that emits the results of a specified combiner function applied to seven items
     * emitted by seven other {@link SingleSource}s.
     * <p>
     * <img width="640" height="434" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.zip.n.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code zip} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T1> the first source {@code SingleSource}'s value type
     * @param <T2> the second source {@code SingleSource}'s value type
     * @param <T3> the third source {@code SingleSource}'s value type
     * @param <T4> the fourth source {@code SingleSource}'s value type
     * @param <T5> the fifth source {@code SingleSource}'s value type
     * @param <T6> the sixth source {@code SingleSource}'s value type
     * @param <T7> the seventh source {@code SingleSource}'s value type
     * @param <R> the result value type
     * @param source1
     *            the first source {@code SingleSource}
     * @param source2
     *            a second source {@code SingleSource}
     * @param source3
     *            a third source {@code SingleSource}
     * @param source4
     *            a fourth source {@code SingleSource}
     * @param source5
     *            a fifth source {@code SingleSource}
     * @param source6
     *            a sixth source {@code SingleSource}
     * @param source7
     *            a seventh source {@code SingleSource}
     * @param zipper
     *            a function that, when applied to the item emitted by each of the source {@code SingleSource}s, results in an
     *            item that will be emitted by the resulting {@code Single}
     * @return the new {@code Single} that emits the zipped results
     * @throws NullPointerException if {@code source1}, {@code source2}, {@code source3}, {@code source4}
     *                              {@code source5}, {@code source6}, {@code source7} or {@code zipper} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/zip.html">ReactiveX operators documentation: Zip</a>
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <@NonNull T1, @NonNull T2, @NonNull T3, @NonNull T4, @NonNull T5, @NonNull T6, @NonNull T7, @NonNull R> Single<R> zip(
            @NonNull SingleSource<? extends T1> source1, @NonNull SingleSource<? extends T2> source2,
            @NonNull SingleSource<? extends T3> source3, @NonNull SingleSource<? extends T4> source4,
            @NonNull SingleSource<? extends T5> source5, @NonNull SingleSource<? extends T6> source6,
            @NonNull SingleSource<? extends T7> source7,
            @NonNull Function7<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? extends R> zipper
     ) {
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
     * Returns a {@code Single} that emits the results of a specified combiner function applied to eight items
     * emitted by eight other {@link SingleSource}s.
     * <p>
     * <img width="640" height="434" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.zip.n.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code zip} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T1> the first source {@code SingleSource}'s value type
     * @param <T2> the second source {@code SingleSource}'s value type
     * @param <T3> the third source {@code SingleSource}'s value type
     * @param <T4> the fourth source {@code SingleSource}'s value type
     * @param <T5> the fifth source {@code SingleSource}'s value type
     * @param <T6> the sixth source {@code SingleSource}'s value type
     * @param <T7> the seventh source {@code SingleSource}'s value type
     * @param <T8> the eighth source {@code SingleSource}'s value type
     * @param <R> the result value type
     * @param source1
     *            the first source {@code SingleSource}
     * @param source2
     *            a second source {@code SingleSource}
     * @param source3
     *            a third source {@code SingleSource}
     * @param source4
     *            a fourth source {@code SingleSource}
     * @param source5
     *            a fifth source {@code SingleSource}
     * @param source6
     *            a sixth source {@code SingleSource}
     * @param source7
     *            a seventh source {@code SingleSource}
     * @param source8
     *            an eighth source {@code SingleSource}
     * @param zipper
     *            a function that, when applied to the item emitted by each of the source {@code SingleSource}s, results in an
     *            item that will be emitted by the resulting {@code Single}
     * @return the new {@code Single} that emits the zipped results
     * @throws NullPointerException if {@code source1}, {@code source2}, {@code source3}, {@code source4}
     *                              {@code source5}, {@code source6}, {@code source7}, {@code source8} or {@code zipper} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/zip.html">ReactiveX operators documentation: Zip</a>
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <@NonNull T1, @NonNull T2, @NonNull T3, @NonNull T4, @NonNull T5, @NonNull T6, @NonNull T7, @NonNull T8, @NonNull R> Single<R> zip(
            @NonNull SingleSource<? extends T1> source1, @NonNull SingleSource<? extends T2> source2,
            @NonNull SingleSource<? extends T3> source3, @NonNull SingleSource<? extends T4> source4,
            @NonNull SingleSource<? extends T5> source5, @NonNull SingleSource<? extends T6> source6,
            @NonNull SingleSource<? extends T7> source7, @NonNull SingleSource<? extends T8> source8,
            @NonNull Function8<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? super T8, ? extends R> zipper
     ) {
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
     * Returns a {@code Single} that emits the results of a specified combiner function applied to nine items
     * emitted by nine other {@link SingleSource}s.
     * <p>
     * <img width="640" height="434" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.zip.n.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code zip} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T1> the first source {@code SingleSource}'s value type
     * @param <T2> the second source {@code SingleSource}'s value type
     * @param <T3> the third source {@code SingleSource}'s value type
     * @param <T4> the fourth source {@code SingleSource}'s value type
     * @param <T5> the fifth source {@code SingleSource}'s value type
     * @param <T6> the sixth source {@code SingleSource}'s value type
     * @param <T7> the seventh source {@code SingleSource}'s value type
     * @param <T8> the eighth source {@code SingleSource}'s value type
     * @param <T9> the ninth source {@code SingleSource}'s value type
     * @param <R> the result value type
     * @param source1
     *            the first source {@code SingleSource}
     * @param source2
     *            a second source {@code SingleSource}
     * @param source3
     *            a third source {@code SingleSource}
     * @param source4
     *            a fourth source {@code SingleSource}
     * @param source5
     *            a fifth source {@code SingleSource}
     * @param source6
     *            a sixth source {@code SingleSource}
     * @param source7
     *            a seventh source {@code SingleSource}
     * @param source8
     *            an eighth source {@code SingleSource}
     * @param source9
     *            a ninth source {@code SingleSource}
     * @param zipper
     *            a function that, when applied to the item emitted by each of the source {@code SingleSource}s, results in an
     *            item that will be emitted by the resulting {@code Single}
     * @return the new {@code Single} that emits the zipped results
     * @throws NullPointerException if {@code source1}, {@code source2}, {@code source3}, {@code source4}
     *                              {@code source5}, {@code source6}, {@code source7}, {@code source8},
     *                              {@code source9} or {@code zipper} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/zip.html">ReactiveX operators documentation: Zip</a>
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <@NonNull T1, @NonNull T2, @NonNull T3, @NonNull T4, @NonNull T5, @NonNull T6, @NonNull T7, @NonNull T8, @NonNull T9, @NonNull R> Single<R> zip(
            @NonNull SingleSource<? extends T1> source1, @NonNull SingleSource<? extends T2> source2,
            @NonNull SingleSource<? extends T3> source3, @NonNull SingleSource<? extends T4> source4,
            @NonNull SingleSource<? extends T5> source5, @NonNull SingleSource<? extends T6> source6,
            @NonNull SingleSource<? extends T7> source7, @NonNull SingleSource<? extends T8> source8,
            @NonNull SingleSource<? extends T9> source9,
            @NonNull Function9<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? super T8, ? super T9, ? extends R> zipper
     ) {
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
     * Waits until all {@link SingleSource} sources provided via an array signal a success
     * value and calls a zipper function with an array of these values to return a result
     * to be emitted to downstream.
     * <p>
     * <img width="640" height="340" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.zipArray.png" alt="">
     * <p>
     * If the array of {@code SingleSource}s is empty a {@link NoSuchElementException} error is signaled immediately.
     * <p>
     * Note on method signature: since Java doesn't allow creating a generic array with {@code new T[]}, the
     * implementation of this operator has to create an {@code Object[]} instead. Unfortunately, a
     * {@code Function<Integer[], R>} passed to the method would trigger a {@link ClassCastException}.
     * <p>
     * If any of the {@code SingleSource}s signal an error, all other {@code SingleSource}s get disposed and the
     * error emitted to downstream immediately.
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code zipArray} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the common value type
     * @param <R> the result value type
     * @param sources the array of {@code SingleSource} instances. An empty sequence will result in an
     *                {@code onError} signal of {@code NoSuchElementException}.
     * @param zipper the function that receives an array with values from each {@code SingleSource}
     *               and should return a value to be emitted to downstream
     * @return the new {@code Single} instance
     * @throws NullPointerException if {@code zipper} or {@code sources} is {@code null}
     * @since 2.0
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    @SafeVarargs
    public static <@NonNull T, @NonNull R> Single<R> zipArray(@NonNull Function<? super Object[], ? extends R> zipper, @NonNull SingleSource<? extends T>... sources) {
        Objects.requireNonNull(zipper, "zipper is null");
        Objects.requireNonNull(sources, "sources is null");
        if (sources.length == 0) {
            return error(new NoSuchElementException());
        }
        return RxJavaPlugins.onAssembly(new SingleZipArray<>(sources, zipper));
    }

    /**
     * Signals the event of this or the other {@link SingleSource} whichever signals first.
     * <p>
     * <img width="640" height="463" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.ambWith.v3.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code ambWith} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param other the other {@code SingleSource} to race for the first emission of success or error
     * @return the new {@code Single} instance. A subscription to this provided source will occur after subscribing
     *            to the current source.
     * @throws NullPointerException if {@code other} is {@code null}
     * @since 2.0
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Single<T> ambWith(@NonNull SingleSource<? extends T> other) {
        Objects.requireNonNull(other, "other is null");
        return ambArray(this, other);
    }

    /**
     * Hides the identity of the current {@code Single}, including the {@link Disposable} that is sent
     * to the downstream via {@code onSubscribe()}.
     * <p>
     * <img width="640" height="458" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.hide.v3.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code hide} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @return the new {@code Single} instance
     * @since 2.0
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Single<T> hide() {
        return RxJavaPlugins.onAssembly(new SingleHide<>(this));
    }

    /**
     * Transform a {@code Single} by applying a particular {@link SingleTransformer} function to it.
     * <p>
     * <img width="640" height="612" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.compose.v3.png" alt="">
     * <p>
     * This method operates on the {@code Single} itself whereas {@link #lift} operates on {@link SingleObserver}s.
     * <p>
     * If the operator you are creating is designed to act on the individual item emitted by a {@code Single}, use
     * {@link #lift}. If your operator is designed to transform the current {@code Single} as a whole (for instance, by
     * applying a particular set of existing RxJava operators to it) use {@code compose}.
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code compose} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <R> the value type of the single returned by the transformer function
     * @param transformer the transformer function, not {@code null}
     * @return the new {@code Single} instance
     * @throws NullPointerException if {@code transformer} is {@code null}
     * @see <a href="https://github.com/ReactiveX/RxJava/wiki/Implementing-Your-Own-Operators">RxJava wiki: Implementing Your Own Operators</a>
     */
    @SuppressWarnings("unchecked")
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <@NonNull R> Single<R> compose(@NonNull SingleTransformer<? super T, ? extends R> transformer) {
        return wrap(((SingleTransformer<T, R>) Objects.requireNonNull(transformer, "transformer is null")).apply(this));
    }

    /**
     * Stores the success value or exception from the current {@code Single} and replays it to late {@link SingleObserver}s.
     * <p>
     * <img width="640" height="363" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.cache.png" alt="">
     * <p>
     * The returned {@code Single} subscribes to the current {@code Single} when the first {@code SingleObserver} subscribes.
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code cache} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @return the new {@code Single} instance
     * @since 2.0
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Single<T> cache() {
        return RxJavaPlugins.onAssembly(new SingleCache<>(this));
    }

    /**
     * Casts the success value of the current {@code Single} into the target type or signals a
     * {@link ClassCastException} if not compatible.
     * <p>
     * <img width="640" height="393" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.cast.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code cast} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <U> the target type
     * @param clazz the type token to use for casting the success result from the current {@code Single}
     * @return the new {@code Single} instance
     * @throws NullPointerException if {@code clazz} is {@code null}
     * @since 2.0
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public final <@NonNull U> Single<U> cast(@NonNull Class<? extends U> clazz) {
        Objects.requireNonNull(clazz, "clazz is null");
        return map(Functions.castFunction(clazz));
    }

    /**
     * Returns a {@code Single} that is based on applying a specified function to the item emitted by the current {@code Single},
     * where that function returns a {@link SingleSource}.
     * <p>
     * <img width="640" height="313" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.concatMap.png" alt="">
     * <p>
     * The operator is an alias for {@link #flatMap(Function)}
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code concatMap} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <R> the result value type
     * @param mapper
     *            a function that, when applied to the item emitted by the current {@code Single}, returns a {@code SingleSource}
     * @return the new {@code Single} returned from {@code mapper} when applied to the item emitted by the current {@code Single}
     * @throws NullPointerException if {@code mapper} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/flatmap.html">ReactiveX operators documentation: FlatMap</a>
     * @since 3.0.0
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public final <@NonNull R> Single<R> concatMap(@NonNull Function<? super T, ? extends SingleSource<? extends R>> mapper) {
        Objects.requireNonNull(mapper, "mapper is null");
        return RxJavaPlugins.onAssembly(new SingleFlatMap<>(this, mapper));
    }

    /**
     * Returns a {@link Completable} that completes based on applying a specified function to the item emitted by the
     * current {@code Single}, where that function returns a {@link CompletableSource}.
     * <p>
     * <img width="640" height="298" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.concatMapCompletable.png" alt="">
     * <p>
     * The operator is an alias for {@link #flatMapCompletable(Function)}.
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code concatMapCompletable} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param mapper
     *            a function that, when applied to the item emitted by the current {@code Single}, returns a
     *            {@code CompletableSource}
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
     * Returns a {@link Maybe} that is based on applying a specified function to the item emitted by the current {@code Single},
     * where that function returns a {@link MaybeSource}.
     * <p>
     * <img width="640" height="254" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.concatMapMaybe.png" alt="">
     * <p>
     * The operator is an alias for {@link #flatMapMaybe(Function)}.
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code concatMapMaybe} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <R> the result value type
     * @param mapper
     *            a function that, when applied to the item emitted by the current {@code Single}, returns a {@code MaybeSource}
     * @return the new {@code Maybe} returned from {@code mapper} when applied to the item emitted by the current {@code Single}
     * @throws NullPointerException if {@code mapper} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/flatmap.html">ReactiveX operators documentation: FlatMap</a>
     * @since 3.0.0
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public final <@NonNull R> Maybe<R> concatMapMaybe(@NonNull Function<? super T, ? extends MaybeSource<? extends R>> mapper) {
        return flatMapMaybe(mapper);
    }

    /**
     * Returns a {@link Flowable} that emits the item emitted by the current {@code Single}, then the item emitted by the
     * specified {@link SingleSource}.
     * <p>
     * <img width="640" height="335" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.concatWith.v3.png" alt="">
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The returned {@code Flowable} honors the backpressure of the downstream consumer.</dd>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code concatWith} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param other
     *            a {@code SingleSource} to be concatenated after the current
     * @return the new {@code Flowable} that emits the item emitted by the current {@code Single}, followed by the item emitted by
     *         {@code other}
     * @throws NullPointerException if {@code other} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/concat.html">ReactiveX operators documentation: Concat</a>
     */
    @BackpressureSupport(BackpressureKind.FULL)
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Flowable<T> concatWith(@NonNull SingleSource<? extends T> other) {
        return concat(this, other);
    }

    /**
     * Delays the emission of the success signal from the current {@code Single} by the specified amount.
     * An error signal will not be delayed.
     * <p>
     * <img width="640" height="457" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.delay.v3.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code delay} operates by default on the {@code computation} {@link Scheduler}.</dd>
     * </dl>
     *
     * @param time the amount of time the success signal should be delayed for
     * @param unit the time unit
     * @return the new {@code Single} instance
     * @since 2.0
     * @throws NullPointerException if {@code unit} is {@code null}
     * @see #delay(long, TimeUnit, boolean)
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.COMPUTATION)
    @NonNull
    public final Single<T> delay(long time, @NonNull TimeUnit unit) {
        return delay(time, unit, Schedulers.computation(), false);
    }

    /**
     * Delays the emission of the success or error signal from the current {@code Single} by the specified amount.
     * <p>
     * <img width="640" height="457" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.delay.e.v3.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code delay} operates by default on the {@code computation} {@link Scheduler}.</dd>
     * </dl>
     * <p>History: 2.1.5 - experimental
     * @param time the amount of time the success or error signal should be delayed for
     * @param unit the time unit
     * @param delayError if {@code true}, both success and error signals are delayed. if {@code false}, only success signals are delayed.
     * @return the new {@code Single} instance
     * @throws NullPointerException if {@code unit} is {@code null}
     * @since 2.2
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.COMPUTATION)
    @NonNull
    public final Single<T> delay(long time, @NonNull TimeUnit unit, boolean delayError) {
        return delay(time, unit, Schedulers.computation(), delayError);
    }

    /**
     * Delays the emission of the success signal from the current {@code Single} by the specified amount.
     * An error signal will not be delayed.
     * <p>
     * <img width="640" height="457" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.delay.s.v3.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>you specify the {@link Scheduler} where the non-blocking wait and emission happens</dd>
     * </dl>
     *
     * @param time the amount of time the success signal should be delayed for
     * @param unit the time unit
     * @param scheduler the target scheduler to use for the non-blocking wait and emission
     * @return the new {@code Single} instance
     * @throws NullPointerException
     *             if {@code unit} is {@code null}, or
     *             if {@code scheduler} is {@code null}
     * @since 2.0
     * @see #delay(long, TimeUnit, Scheduler, boolean)
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    @NonNull
    public final Single<T> delay(long time, @NonNull TimeUnit unit, @NonNull Scheduler scheduler) {
        return delay(time, unit, scheduler, false);
    }

    /**
     * Delays the emission of the success or error signal from the current {@code Single} by the specified amount.
     * <p>
     * <img width="640" height="457" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.delay.se.v3.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>you specify the {@link Scheduler} where the non-blocking wait and emission happens</dd>
     * </dl>
     * <p>History: 2.1.5 - experimental
     * @param time the amount of time the success or error signal should be delayed for
     * @param unit the time unit
     * @param scheduler the target scheduler to use for the non-blocking wait and emission
     * @param delayError if {@code true}, both success and error signals are delayed. if {@code false}, only success signals are delayed.
     * @return the new {@code Single} instance
     * @throws NullPointerException
     *             if {@code unit} is {@code null}, or
     *             if {@code scheduler} is {@code null}
     * @since 2.2
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    public final Single<T> delay(long time, @NonNull TimeUnit unit, @NonNull Scheduler scheduler, boolean delayError) {
        Objects.requireNonNull(unit, "unit is null");
        Objects.requireNonNull(scheduler, "scheduler is null");
        return RxJavaPlugins.onAssembly(new SingleDelay<>(this, time, unit, scheduler, delayError));
    }

    /**
     * Delays the actual subscription to the current {@code Single} until the given other {@link CompletableSource}
     * completes.
     * <p>
     * <img width="640" height="309" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.delaySubscription.c.png" alt="">
     * <p>If the delaying source signals an error, that error is re-emitted and no subscription
     * to the current {@code Single} happens.
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code delaySubscription} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param subscriptionIndicator the {@code CompletableSource} that has to complete before the subscription to the
     *              current {@code Single} happens
     * @return the new {@code Single} instance
     * @throws NullPointerException if {@code subscriptionIndicator} is {@code null}
     * @since 2.0
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Single<T> delaySubscription(@NonNull CompletableSource subscriptionIndicator) {
        Objects.requireNonNull(subscriptionIndicator, "subscriptionIndicator is null");
        return RxJavaPlugins.onAssembly(new SingleDelayWithCompletable<>(this, subscriptionIndicator));
    }

    /**
     * Delays the actual subscription to the current {@code Single} until the given other {@link SingleSource}
     * signals success.
     * <p>
     * <img width="640" height="309" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.delaySubscription.s.png" alt="">
     * <p>If the delaying source signals an error, that error is re-emitted and no subscription
     * to the current {@code Single} happens.
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code delaySubscription} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <U> the element type of the other source
     * @param subscriptionIndicator the {@code SingleSource} that has to complete before the subscription to the
     *              current {@code Single} happens
     * @return the new {@code Single} instance
     * @throws NullPointerException if {@code subscriptionIndicator} is {@code null}
     * @since 2.0
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public final <@NonNull U> Single<T> delaySubscription(@NonNull SingleSource<U> subscriptionIndicator) {
        Objects.requireNonNull(subscriptionIndicator, "subscriptionIndicator is null");
        return RxJavaPlugins.onAssembly(new SingleDelayWithSingle<>(this, subscriptionIndicator));
    }

    /**
     * Delays the actual subscription to the current {@code Single} until the given other {@link ObservableSource}
     * signals its first value or completes.
     * <p>
     * <img width="640" height="214" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.delaySubscription.o.png" alt="">
     * <p>If the delaying source signals an error, that error is re-emitted and no subscription
     * to the current {@code Single} happens.
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code delaySubscription} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <U> the element type of the other source
     * @param subscriptionIndicator the {@code ObservableSource} that has to signal a value or complete before the
     *              subscription to the current {@code Single} happens
     * @return the new {@code Single} instance
     * @throws NullPointerException if {@code subscriptionIndicator} is {@code null}
     * @since 2.0
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public final <@NonNull U> Single<T> delaySubscription(@NonNull ObservableSource<U> subscriptionIndicator) {
        Objects.requireNonNull(subscriptionIndicator, "subscriptionIndicator is null");
        return RxJavaPlugins.onAssembly(new SingleDelayWithObservable<>(this, subscriptionIndicator));
    }

    /**
     * Delays the actual subscription to the current {@code Single} until the given other {@link Publisher}
     * signals its first value or completes.
     * <p>
     * <img width="640" height="214" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.delaySubscription.p.png" alt="">
     * <p>If the delaying source signals an error, that error is re-emitted and no subscription
     * to the current {@code Single} happens.
     * <p>The other source is consumed in an unbounded manner (requesting {@link Long#MAX_VALUE} from it).
     * <dl>
     * <dt><b>Backpressure:</b></dt>
     * <dd>The {@code other} publisher is consumed in an unbounded fashion but will be
     * cancelled after the first item it produced.</dd>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code delaySubscription} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <U> the element type of the other source
     * @param subscriptionIndicator the {@code Publisher} that has to signal a value or complete before the
     *              subscription to the current {@code Single} happens
     * @return the new {@code Single} instance
     * @throws NullPointerException if {@code subscriptionIndicator} is {@code null}
     * @since 2.0
     */
    @BackpressureSupport(BackpressureKind.FULL)
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public final <@NonNull U> Single<T> delaySubscription(@NonNull Publisher<U> subscriptionIndicator) {
        Objects.requireNonNull(subscriptionIndicator, "subscriptionIndicator is null");
        return RxJavaPlugins.onAssembly(new SingleDelayWithPublisher<>(this, subscriptionIndicator));
    }

    /**
     * Delays the actual subscription to the current {@code Single} until the given time delay elapsed.
     * <p>
     * <img width="640" height="472" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.delaySubscription.t.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code delaySubscription} does by default subscribe to the current {@code Single}
     * on the {@code computation} {@link Scheduler} after the delay.</dd>
     * </dl>
     * @param time the time amount to wait with the subscription
     * @param unit the time unit of the waiting
     * @return the new {@code Single} instance
     * @throws NullPointerException if {@code unit} is {@code null}
     * @since 2.0
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.COMPUTATION)
    @NonNull
    public final Single<T> delaySubscription(long time, @NonNull TimeUnit unit) {
        return delaySubscription(time, unit, Schedulers.computation());
    }

    /**
     * Delays the actual subscription to the current {@code Single} until the given time delay elapsed.
     * <p>
     * <img width="640" height="420" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.delaySubscription.ts.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code delaySubscription} does by default subscribe to the current {@code Single}
     * on the {@link Scheduler} you provided, after the delay.</dd>
     * </dl>
     * @param time the time amount to wait with the subscription
     * @param unit the time unit of the waiting
     * @param scheduler the {@code Scheduler} to wait on and subscribe on to the current {@code Single}
     * @return the new {@code Single} instance
     * @throws NullPointerException if {@code unit} or {@code scheduler} is {@code null}
     * @since 2.0
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    @NonNull
    public final Single<T> delaySubscription(long time, @NonNull TimeUnit unit, @NonNull Scheduler scheduler) {
        return delaySubscription(Observable.timer(time, unit, scheduler));
    }

    /**
     * Maps the {@link Notification} success value of the current {@code Single} back into normal
     * {@code onSuccess}, {@code onError} or {@code onComplete} signals as a
     * {@link Maybe} source.
     * <p>
     * <img width="640" height="341" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.dematerialize.png" alt="">
     * <p>
     * The intended use of the {@code selector} function is to perform a
     * type-safe identity mapping (see example) on a source that is already of type
     * {@code Notification<T>}. The Java language doesn't allow
     * limiting instance methods to a certain generic argument shape, therefore,
     * a function is used to ensure the conversion remains type safe.
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     *  <dd>{@code dematerialize} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * <p>
     * Example:
     * <pre><code>
     * Single.just(Notification.createOnNext(1))
     * .dematerialize(notification -&gt; notification)
     * .test()
     * .assertResult(1);
     * </code></pre>
     * <p>History: 2.2.4 - experimental
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
        return RxJavaPlugins.onAssembly(new SingleDematerialize<>(this, selector));
    }

    /**
     * Calls the specified consumer with the success item after this item has been emitted to the downstream.
     * <p>
     * <img width="640" height="461" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.doAfterSuccess.v3.png" alt="">
     * <p>
     * Note that the {@code doAfterSuccess} action is shared between subscriptions and as such
     * should be thread-safe.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code doAfterSuccess} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * <p>History: 2.0.1 - experimental
     * @param onAfterSuccess the {@link Consumer} that will be called after emitting an item from upstream to the downstream
     * @return the new {@code Single} instance
     * @throws NullPointerException if {@code onAfterSuccess} is {@code null}
     * @since 2.1
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Single<T> doAfterSuccess(@NonNull Consumer<? super T> onAfterSuccess) {
        Objects.requireNonNull(onAfterSuccess, "onAfterSuccess is null");
        return RxJavaPlugins.onAssembly(new SingleDoAfterSuccess<>(this, onAfterSuccess));
    }

    /**
     * Registers an {@link Action} to be called after this {@code Single} invokes either {@code onSuccess} or {@code onError}.
     * <p>
     * <img width="640" height="461" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.doAfterTerminate.v3.png" alt="">
     * <p>
     * Note that the {@code doAfterTerminate} action is shared between subscriptions and as such
     * should be thread-safe.</p>
     *
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code doAfterTerminate} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * <p>History: 2.0.6 - experimental
     * @param onAfterTerminate
     *            an {@code Action} to be invoked when the current {@code Single} finishes
     * @return the new {@code Single} that emits the same items as the current {@code Single}, then invokes the
     *         {@code Action}
     * @throws NullPointerException if {@code onAfterTerminate} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/do.html">ReactiveX operators documentation: Do</a>
     * @since 2.1
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Single<T> doAfterTerminate(@NonNull Action onAfterTerminate) {
        Objects.requireNonNull(onAfterTerminate, "onAfterTerminate is null");
        return RxJavaPlugins.onAssembly(new SingleDoAfterTerminate<>(this, onAfterTerminate));
    }

    /**
     * Calls the specified action after this {@code Single} signals {@code onSuccess} or {@code onError} or gets disposed by
     * the downstream.
     * <p>In case of a race between a terminal event and a dispose call, the provided {@code onFinally} action
     * is executed once per subscription.
     * <p>Note that the {@code onFinally} action is shared between subscriptions and as such
     * should be thread-safe.
     * <p>
     * <img width="640" height="291" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.doFinally.v3.png" alt="">
     * </p>
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code doFinally} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * <p>History: 2.0.1 - experimental
     * @param onFinally the action called when this {@code Single} terminates or gets disposed
     * @return the new {@code Single} instance
     * @throws NullPointerException if {@code onFinally} is {@code null}
     * @since 2.1
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Single<T> doFinally(@NonNull Action onFinally) {
        Objects.requireNonNull(onFinally, "onFinally is null");
        return RxJavaPlugins.onAssembly(new SingleDoFinally<>(this, onFinally));
    }

    /**
     * Calls the appropriate {@code onXXX} method (shared between all {@link SingleObserver}s) for the lifecycle events of
     * the sequence (subscription, disposal).
     * <p>
     * <img width="640" height="232" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.doOnLifecycle.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code doOnLifecycle} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param onSubscribe
     *              a {@link Consumer} called with the {@link Disposable} sent via {@link SingleObserver#onSubscribe(Disposable)}
     * @param onDispose
     *              called when the downstream disposes the {@code Disposable} via {@code dispose()}
     * @return the new {@code Single} instance
     * @throws NullPointerException if {@code onSubscribe} or {@code onDispose} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/do.html">ReactiveX operators documentation: Do</a>
     * @since 3.0.0
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Single<T> doOnLifecycle(@NonNull Consumer<? super Disposable> onSubscribe, @NonNull Action onDispose) {
        Objects.requireNonNull(onSubscribe, "onSubscribe is null");
        Objects.requireNonNull(onDispose, "onDispose is null");
        return RxJavaPlugins.onAssembly(new SingleDoOnLifecycle<>(this, onSubscribe, onDispose));
    }

    /**
     * Calls the shared consumer with the {@link Disposable} sent through the {@code onSubscribe} for each
     * {@link SingleObserver} that subscribes to the current {@code Single}.
     * <p>
     * <img width="640" height="347" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.doOnSubscribe.v3.png" alt="">
     * </p>
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code doOnSubscribe} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param onSubscribe the consumer called with the {@code Disposable} sent via {@code onSubscribe}
     * @return the new {@code Single} instance
     * @throws NullPointerException if {@code onSubscribe} is {@code null}
     * @since 2.0
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Single<T> doOnSubscribe(@NonNull Consumer<? super Disposable> onSubscribe) {
        Objects.requireNonNull(onSubscribe, "onSubscribe is null");
        return RxJavaPlugins.onAssembly(new SingleDoOnSubscribe<>(this, onSubscribe));
    }

    /**
     * Returns a {@code Single} instance that calls the given {@code onTerminate} callback
     * just before this {@code Single} completes normally or with an exception.
     * <p>
     * <img width="640" height="305" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/doOnTerminate.v3.png" alt="">
     * <p>
     * This differs from {@code doAfterTerminate} in that this happens <em>before</em> the {@code onSuccess} or
     * {@code onError} notification.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code doOnTerminate} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * <p>History: 2.2.7 - experimental
     * @param onTerminate the action to invoke when the consumer calls {@code onSuccess} or {@code onError}
     * @return the new {@code Single} instance
     * @throws NullPointerException if {@code onTerminate} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/do.html">ReactiveX operators documentation: Do</a>
     * @see #doOnTerminate(Action)
     * @since 3.0.0
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Single<T> doOnTerminate(@NonNull Action onTerminate) {
        Objects.requireNonNull(onTerminate, "onTerminate is null");
        return RxJavaPlugins.onAssembly(new SingleDoOnTerminate<>(this, onTerminate));
    }

    /**
     * Calls the shared consumer with the success value sent via {@code onSuccess} for each
     * {@link SingleObserver} that subscribes to the current {@code Single}.
     * <p>
     * <img width="640" height="347" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.doOnSuccess.2.v3.png" alt="">
     * </p>
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code doOnSuccess} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param onSuccess the consumer called with the success value of {@code onSuccess}
     * @return the new {@code Single} instance
     * @throws NullPointerException if {@code onSuccess} is {@code null}
     * @since 2.0
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Single<T> doOnSuccess(@NonNull Consumer<? super T> onSuccess) {
        Objects.requireNonNull(onSuccess, "onSuccess is null");
        return RxJavaPlugins.onAssembly(new SingleDoOnSuccess<>(this, onSuccess));
    }

    /**
     * Calls the shared consumer with the error sent via {@code onError} or the value
     * via {@code onSuccess} for each {@link SingleObserver} that subscribes to the current {@code Single}.
     * <p>
     * <img width="640" height="264" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.doOnEvent.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code doOnEvent} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param onEvent the consumer called with the success value of onEvent
     * @return the new {@code Single} instance
     * @throws NullPointerException if {@code onEvent} is {@code null}
     * @since 2.0
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Single<T> doOnEvent(@NonNull BiConsumer<@Nullable ? super T, @Nullable ? super Throwable> onEvent) {
        Objects.requireNonNull(onEvent, "onEvent is null");
        return RxJavaPlugins.onAssembly(new SingleDoOnEvent<>(this, onEvent));
    }

    /**
     * Calls the shared consumer with the error sent via {@code onError} for each
     * {@link SingleObserver} that subscribes to the current {@code Single}.
     * <p>
     * <img width="640" height="349" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.doOnError.2.v3.png" alt="">
     * </p>
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code doOnError} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param onError the consumer called with the success value of {@code onError}
     * @return the new {@code Single} instance
     * @throws NullPointerException if {@code onError} is {@code null}
     * @since 2.0
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Single<T> doOnError(@NonNull Consumer<? super Throwable> onError) {
        Objects.requireNonNull(onError, "onError is null");
        return RxJavaPlugins.onAssembly(new SingleDoOnError<>(this, onError));
    }

    /**
     * Calls the shared {@link Action} if a {@link SingleObserver} subscribed to the current {@code Single}
     * disposes the common {@link Disposable} it received via {@code onSubscribe}.
     * <p>
     * <img width="640" height="332" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.doOnDispose.v3.png" alt="">
     * </p>
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code doOnDispose} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param onDispose the action called when the subscription is disposed
     * @return the new {@code Single} instance
     * @throws NullPointerException if {@code onDispose} is {@code null}
     * @since 2.0
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Single<T> doOnDispose(@NonNull Action onDispose) {
        Objects.requireNonNull(onDispose, "onDispose is null");
        return RxJavaPlugins.onAssembly(new SingleDoOnDispose<>(this, onDispose));
    }

    /**
     * Filters the success item of the {@code Single} via a predicate function and emitting it if the predicate
     * returns {@code true}, completing otherwise.
     * <p>
     * <img width="640" height="457" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.filter.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code filter} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param predicate
     *            a function that evaluates the item emitted by the current {@code Single}, returning {@code true}
     *            if it passes the filter
     * @return the new {@link Maybe} that emit the item emitted by the current {@code Single} that the filter
     *         evaluates as {@code true}
     * @throws NullPointerException if {@code predicate} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/filter.html">ReactiveX operators documentation: Filter</a>
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Maybe<T> filter(@NonNull Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        return RxJavaPlugins.onAssembly(new MaybeFilterSingle<>(this, predicate));
    }

    /**
     * Returns a {@code Single} that is based on applying a specified function to the item emitted by the current {@code Single},
     * where that function returns a {@link SingleSource}.
     * <p>
     * <img width="640" height="300" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.flatMap.v3.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code flatMap} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <R> the result value type
     * @param mapper
     *            a function that, when applied to the item emitted by the current {@code Single}, returns a {@code SingleSource}
     * @return the new {@code Single} returned from {@code mapper} when applied to the item emitted by the current {@code Single}
     * @throws NullPointerException if {@code mapper} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/flatmap.html">ReactiveX operators documentation: FlatMap</a>
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public final <@NonNull R> Single<R> flatMap(@NonNull Function<? super T, ? extends SingleSource<? extends R>> mapper) {
        Objects.requireNonNull(mapper, "mapper is null");
        return RxJavaPlugins.onAssembly(new SingleFlatMap<>(this, mapper));
    }

    /**
     * Returns a {@code Single} that emits the results of a specified function to the pair of values emitted by the
     * current {@code Single} and a specified mapped {@link SingleSource}.
     * <p>
     * <img width="640" height="268" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.flatMap.combiner.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code flatMap} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <U>
     *            the type of items emitted by the {@code SingleSource} returned by the {@code mapper} function
     * @param <R>
     *            the type of items emitted by the resulting {@code Single}
     * @param mapper
     *            a function that returns a {@code SingleSource} for the item emitted by the current {@code Single}
     * @param combiner
     *            a function that combines one item emitted by each of the source and collection {@code SingleSource} and
     *            returns an item to be emitted by the resulting {@code SingleSource}
     * @return the new {@code Single} instance
     * @throws NullPointerException if {@code mapper} or {@code combiner} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/flatmap.html">ReactiveX operators documentation: FlatMap</a>
     * @since 3.0.0
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public final <@NonNull U, @NonNull R> Single<R> flatMap(@NonNull Function<? super T, ? extends SingleSource<? extends U>> mapper,
            @NonNull BiFunction<? super T, ? super U, ? extends R> combiner) {
        Objects.requireNonNull(mapper, "mapper is null");
        Objects.requireNonNull(combiner, "combiner is null");
        return RxJavaPlugins.onAssembly(new SingleFlatMapBiSelector<>(this, mapper, combiner));
    }

    /**
     * Maps the {@code onSuccess} or {@code onError} signals of the current {@code Single} into a {@link SingleSource} and emits that
     * {@code SingleSource}'s signals.
     * <p>
     * <img width="640" height="449" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.flatMap.notification.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code flatMap} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <R>
     *            the result type
     * @param onSuccessMapper
     *            a function that returns a {@code SingleSource} to merge for the {@code onSuccess} item emitted by this {@code Single}
     * @param onErrorMapper
     *            a function that returns a {@code SingleSource} to merge for an {@code onError} notification from this {@code Single}
     * @return the new {@code Single} instance
     * @throws NullPointerException if {@code onSuccessMapper} or {@code onErrorMapper} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/flatmap.html">ReactiveX operators documentation: FlatMap</a>
     * @since 3.0.0
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public final <@NonNull R> Single<R> flatMap(
            @NonNull Function<? super T, ? extends SingleSource<? extends R>> onSuccessMapper,
            @NonNull Function<? super Throwable, ? extends SingleSource<? extends R>> onErrorMapper) {
        Objects.requireNonNull(onSuccessMapper, "onSuccessMapper is null");
        Objects.requireNonNull(onErrorMapper, "onErrorMapper is null");
        return RxJavaPlugins.onAssembly(new SingleFlatMapNotification<>(this, onSuccessMapper, onErrorMapper));
    }

    /**
     * Returns a {@link Maybe} that is based on applying a specified function to the item emitted by the current {@code Single},
     * where that function returns a {@link MaybeSource}.
     * <p>
     * <img width="640" height="191" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.flatMapMaybe.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code flatMapMaybe} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <R> the result value type
     * @param mapper
     *            a function that, when applied to the item emitted by the current {@code Single}, returns a {@code MaybeSource}
     * @return the new {@code Maybe} returned from {@code mapper} when applied to the item emitted by the current {@code Single}
     * @throws NullPointerException if {@code mapper} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/flatmap.html">ReactiveX operators documentation: FlatMap</a>
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public final <@NonNull R> Maybe<R> flatMapMaybe(@NonNull Function<? super T, ? extends MaybeSource<? extends R>> mapper) {
        Objects.requireNonNull(mapper, "mapper is null");
        return RxJavaPlugins.onAssembly(new SingleFlatMapMaybe<>(this, mapper));
    }

    /**
     * Returns a {@link Flowable} that emits items based on applying a specified function to the item emitted by the
     * current {@code Single}, where that function returns a {@link Publisher}.
     * <p>
     * <img width="640" height="305" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.flatMapPublisher.v3.png" alt="">
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The returned {@code Flowable} honors the backpressure of the downstream consumer
     *  and the {@code Publisher} returned by the mapper function is expected to honor it as well.</dd>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code flatMapPublisher} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <R> the result value type
     * @param mapper
     *            a function that, when applied to the item emitted by the current {@code Single}, returns a
     *            {@code Publisher}
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
        return RxJavaPlugins.onAssembly(new SingleFlatMapPublisher<>(this, mapper));
    }

    /**
     * Maps the success value of the current {@code Single} into an {@link Iterable} and emits its items as a
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
     *            the type of item emitted by the resulting {@code Iterable}
     * @param mapper
     *            a function that returns an {@code Iterable} sequence of values for when given an item emitted by the
     *            current {@code Single}
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
        return RxJavaPlugins.onAssembly(new SingleFlatMapIterableFlowable<>(this, mapper));
    }

    /**
     * Maps the success value of the current {@code Single} into an {@link Iterable} and emits its items as an
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
     *            current {@code Single}
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code mapper} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/flatmap.html">ReactiveX operators documentation: FlatMap</a>
     * @see #flattenStreamAsObservable(Function)
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public final <@NonNull U> Observable<U> flattenAsObservable(@NonNull Function<@NonNull ? super T, @NonNull ? extends Iterable<? extends U>> mapper) {
        Objects.requireNonNull(mapper, "mapper is null");
        return RxJavaPlugins.onAssembly(new SingleFlatMapIterableObservable<>(this, mapper));
    }

    /**
     * Returns an {@link Observable} that is based on applying a specified function to the item emitted by the current {@code Single},
     * where that function returns an {@link ObservableSource}.
     * <p>
     * <img width="640" height="305" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.flatMapObservable.v3.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code flatMapObservable} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <R> the result value type
     * @param mapper
     *            a function that, when applied to the item emitted by the current {@code Single}, returns an {@code ObservableSource}
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code mapper} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/flatmap.html">ReactiveX operators documentation: FlatMap</a>
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public final <@NonNull R> Observable<R> flatMapObservable(@NonNull Function<? super T, ? extends ObservableSource<? extends R>> mapper) {
        Objects.requireNonNull(mapper, "mapper is null");
        return RxJavaPlugins.onAssembly(new SingleFlatMapObservable<>(this, mapper));
    }

    /**
     * Returns a {@link Completable} that completes based on applying a specified function to the item emitted by the
     * current {@code Single}, where that function returns a {@link CompletableSource}.
     * <p>
     * <img width="640" height="267" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.flatMapCompletable.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code flatMapCompletable} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param mapper
     *            a function that, when applied to the item emitted by the current {@code Single}, returns a
     *            {@code CompletableSource}
     * @return the new {@code Completable} instance
     * @throws NullPointerException if {@code mapper} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/flatmap.html">ReactiveX operators documentation: FlatMap</a>
     * @since 2.0
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Completable flatMapCompletable(@NonNull Function<? super T, ? extends CompletableSource> mapper) {
        Objects.requireNonNull(mapper, "mapper is null");
        return RxJavaPlugins.onAssembly(new SingleFlatMapCompletable<>(this, mapper));
    }

    /**
     * Waits in a blocking fashion until the current {@code Single} signals a success value (which is returned) or
     * an exception (which is propagated).
     * <p>
     * <img width="640" height="429" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.blockingGet.png" alt="">
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
    @NonNull
    public final T blockingGet() {
        BlockingMultiObserver<T> observer = new BlockingMultiObserver<>();
        subscribe(observer);
        return observer.blockingGet();
    }

    /**
     * Subscribes to the current {@code Single} and <em>blocks the current thread</em> until it terminates.
     * <p>
     * <img width="640" height="329" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.blockingSubscribe.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code blockingSubscribe} does not operate by default on a particular {@link Scheduler}.</dd>
     *  <dt><b>Error handling:</b></dt>
     *  <dd>If the current {@code Single} signals an error,
     *  the {@link Throwable} is routed to the global error handler via {@link RxJavaPlugins#onError(Throwable)}.
     *  If the current thread is interrupted, an {@link InterruptedException} is routed to the same global error handler.
     *  </dd>
     * </dl>
     * @since 3.0.0
     * @see #blockingSubscribe(Consumer)
     * @see #blockingSubscribe(Consumer, Consumer)
     */
    @SchedulerSupport(SchedulerSupport.NONE)
    public final void blockingSubscribe() {
        blockingSubscribe(Functions.emptyConsumer(), Functions.ERROR_CONSUMER);
    }

    /**
     * Subscribes to the current {@code Single} and calls given {@code onSuccess} callback on the <em>current thread</em>
     * when it completes normally.
     * <p>
     * <img width="640" height="351" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.blockingSubscribe.c.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code blockingSubscribe} does not operate by default on a particular {@link Scheduler}.</dd>
     *  <dt><b>Error handling:</b></dt>
     *  <dd>If either the current {@code Single} signals an error or {@code onSuccess} throws,
     *  the respective {@link Throwable} is routed to the global error handler via {@link RxJavaPlugins#onError(Throwable)}.
     *  If the current thread is interrupted, an {@link InterruptedException} is routed to the same global error handler.
     *  </dd>
     * </dl>
     * @param onSuccess the {@link Consumer} to call if the current {@code Single} succeeds
     * @throws NullPointerException if {@code onSuccess} is {@code null}
     * @since 3.0.0
     * @see #blockingSubscribe(Consumer, Consumer)
     */
    @SchedulerSupport(SchedulerSupport.NONE)
    public final void blockingSubscribe(@NonNull Consumer<? super T> onSuccess) {
        blockingSubscribe(onSuccess, Functions.ERROR_CONSUMER);
    }

    /**
     * Subscribes to the current {@code Single} and calls the appropriate callback on the <em>current thread</em>
     * when it terminates.
     * <p>
     * <img width="640" height="348" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.blockingSubscribe.cc.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code blockingSubscribe} does not operate by default on a particular {@link Scheduler}.</dd>
     *  <dt><b>Error handling:</b></dt>
     *  <dd>If either {@code onSuccess} or {@code onError} throw, the {@link Throwable} is routed to the
     *  global error handler via {@link RxJavaPlugins#onError(Throwable)}.
     *  If the current thread is interrupted, the {@code onError} consumer is called with an {@link InterruptedException}.
     *  </dd>
     * </dl>
     * @param onSuccess the {@link Consumer} to call if the current {@code Single} succeeds
     * @param onError the {@code Consumer} to call if the current {@code Single} signals an error
     * @throws NullPointerException if {@code onSuccess} or {@code onError} is {@code null}
     * @since 3.0.0
     */
    @SchedulerSupport(SchedulerSupport.NONE)
    public final void blockingSubscribe(@NonNull Consumer<? super T> onSuccess, @NonNull Consumer<? super Throwable> onError) {
        Objects.requireNonNull(onSuccess, "onSuccess is null");
        Objects.requireNonNull(onError, "onError is null");
        BlockingMultiObserver<T> observer = new BlockingMultiObserver<>();
        subscribe(observer);
        observer.blockingConsume(onSuccess, onError, Functions.EMPTY_ACTION);
    }

    /**
     * Subscribes to the current {@code Single} and calls the appropriate {@link SingleObserver} method on the <em>current thread</em>.
     * <p>
     * <img width="640" height="479" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.blockingSubscribe.o.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code blockingSubscribe} does not operate by default on a particular {@link Scheduler}.</dd>
     *  <dt><b>Error handling:</b></dt>
     *  <dd>An {@code onError} signal is delivered to the {@link SingleObserver#onError(Throwable)} method.
     *  If any of the {@code SingleObserver}'s methods throw, the {@link RuntimeException} is propagated to the caller of this method.
     *  If the current thread is interrupted, an {@link InterruptedException} is delivered to {@code observer.onError}.
     *  </dd>
     * </dl>
     * @param observer the {@code SingleObserver} to call methods on the current thread
     * @throws NullPointerException if {@code observer} is {@code null}
     * @since 3.0.0
     */
    @SchedulerSupport(SchedulerSupport.NONE)
    public final void blockingSubscribe(@NonNull SingleObserver<? super T> observer) {
        Objects.requireNonNull(observer, "observer is null");
        BlockingDisposableMultiObserver<T> blockingObserver = new BlockingDisposableMultiObserver<>();
        observer.onSubscribe(blockingObserver);
        subscribe(blockingObserver);
        blockingObserver.blockingConsume(observer);
    }

    /**
     * <strong>This method requires advanced knowledge about building operators, please consider
     * other standard composition methods first;</strong>
     * Returns a {@code Single} which, when subscribed to, invokes the {@link SingleOperator#apply(SingleObserver) apply(SingleObserver)} method
     * of the provided {@link SingleOperator} for each individual downstream {@link Single} and allows the
     * insertion of a custom operator by accessing the downstream's {@link SingleObserver} during this subscription phase
     * and providing a new {@code SingleObserver}, containing the custom operator's intended business logic, that will be
     * used in the subscription process going further upstream.
     * <p>
     * <img width="640" height="304" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.lift.png" alt="">
     * <p>
     * Generally, such a new {@code SingleObserver} will wrap the downstream's {@code SingleObserver} and forwards the
     * {@code onSuccess} and {@code onError} events from the upstream directly or according to the
     * emission pattern the custom operator's business logic requires. In addition, such operator can intercept the
     * flow control calls of {@code dispose} and {@code isDisposed} that would have traveled upstream and perform
     * additional actions depending on the same business logic requirements.
     * <p>
     * Example:
     * <pre><code>
     * // Step 1: Create the consumer type that will be returned by the SingleOperator.apply():
     *
     * public final class CustomSingleObserver&lt;T&gt; implements SingleObserver&lt;T&gt;, Disposable {
     *
     *     // The downstream's SingleObserver that will receive the onXXX events
     *     final SingleObserver&lt;? super String&gt; downstream;
     *
     *     // The connection to the upstream source that will call this class' onXXX methods
     *     Disposable upstream;
     *
     *     // The constructor takes the downstream subscriber and usually any other parameters
     *     public CustomSingleObserver(SingleObserver&lt;? super String&gt; downstream) {
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
     *             // Single is usually expected to produce one of the onXXX events
     *             downstream.onError(new NoSuchElementException());
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
     * // Step 2: Create a class that implements the SingleOperator interface and
     * //         returns the custom consumer type from above in its apply() method.
     * //         Such class may define additional parameters to be submitted to
     * //         the custom consumer type.
     *
     * final class CustomSingleOperator&lt;T&gt; implements SingleOperator&lt;String&gt; {
     *     &#64;Override
     *     public SingleObserver&lt;? super String&gt; apply(SingleObserver&lt;? super T&gt; upstream) {
     *         return new CustomSingleObserver&lt;T&gt;(upstream);
     *     }
     * }
     *
     * // Step 3: Apply the custom operator via lift() in a flow by creating an instance of it
     * //         or reusing an existing one.
     *
     * Single.just(5)
     * .lift(new CustomSingleOperator&lt;Integer&gt;())
     * .test()
     * .assertResult("5");
     *
     * Single.just(15)
     * .lift(new CustomSingleOperator&lt;Integer&gt;())
     * .test()
     * .assertFailure(NoSuchElementException.class);
     * </code></pre>
     * <p>
     * Creating custom operators can be complicated and it is recommended one consults the
     * <a href="https://github.com/ReactiveX/RxJava/wiki/Writing-operators-for-2.0">RxJava wiki: Writing operators</a> page about
     * the tools, requirements, rules, considerations and pitfalls of implementing them.
     * <p>
     * Note that implementing custom operators via this {@code lift()} method adds slightly more overhead by requiring
     * an additional allocation and indirection per assembled flows. Instead, extending the abstract {@code Single}
     * class and creating a {@link SingleTransformer} with it is recommended.
     * <p>
     * Note also that it is not possible to stop the subscription phase in {@code lift()} as the {@code apply()} method
     * requires a non-{@code null} {@code SingleObserver} instance to be returned, which is then unconditionally subscribed to
     * the current {@code Single}. For example, if the operator decided there is no reason to subscribe to the
     * upstream source because of some optimization possibility or a failure to prepare the operator, it still has to
     * return a {@code SingleObserver} that should immediately dispose the upstream's {@link Disposable} in its
     * {@code onSubscribe} method. Again, using a {@code SingleTransformer} and extending the {@code Single} is
     * a better option as {@link #subscribeActual} can decide to not subscribe to its upstream after all.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code lift} does not operate by default on a particular {@link Scheduler}, however, the
     *  {@code SingleOperator} may use a {@code Scheduler} to support its own asynchronous behavior.</dd>
     * </dl>
     *
     * @param <R> the output value type
     * @param lift the {@code SingleOperator} that receives the downstream's {@code SingleObserver} and should return
     *               a {@code SingleObserver} with custom behavior to be used as the consumer for the current
     *               {@code Single}.
     * @return the new {@code Single} instance
     * @throws NullPointerException if {@code lift} is {@code null}
     * @see <a href="https://github.com/ReactiveX/RxJava/wiki/Writing-operators-for-2.0">RxJava wiki: Writing operators</a>
     * @see #compose(SingleTransformer)
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public final <@NonNull R> Single<R> lift(@NonNull SingleOperator<? extends R, ? super T> lift) {
        Objects.requireNonNull(lift, "lift is null");
        return RxJavaPlugins.onAssembly(new SingleLift<>(this, lift));
    }

    /**
     * Returns a {@code Single} that applies a specified function to the item emitted by the current {@code Single} and
     * emits the result of this function application.
     * <p>
     * <img width="640" height="305" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.map.v3.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code map} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <R> the result value type
     * @param mapper
     *            a function to apply to the item emitted by the {@code Single}
     * @return the new {@code Single} that emits the item from the current {@code Single}, transformed by the specified function
     * @throws NullPointerException if {@code mapper} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/map.html">ReactiveX operators documentation: Map</a>
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public final <@NonNull R> Single<R> map(@NonNull Function<? super T, ? extends R> mapper) {
        Objects.requireNonNull(mapper, "mapper is null");
        return RxJavaPlugins.onAssembly(new SingleMap<>(this, mapper));
    }

    /**
     * Maps the signal types of this {@code Single} into a {@link Notification} of the same kind
     * and emits it as a single success value to downstream.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/materialize.v3.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code materialize} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * <p>History: 2.2.4 - experimental
     * @return the new {@code Single} instance
     * @since 3.0.0
     * @see #dematerialize(Function)
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Single<Notification<T>> materialize() {
        return RxJavaPlugins.onAssembly(new SingleMaterialize<>(this));
    }

    /**
     * Signals {@code true} if the current {@code Single} signals a success value that is {@link Object#equals(Object)} with the value
     * provided.
     * <p>
     * <img width="640" height="401" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.contains.png" alt="">
     * <p>
     * <img width="640" height="401" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.contains.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code contains} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param item the value to compare against the success value of this {@code Single}
     * @return the new {@code Single} instance
     * @throws NullPointerException if {@code item} is {@code null}
     * @since 2.0
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Single<Boolean> contains(@NonNull Object item) {
        return contains(item, ObjectHelper.equalsPredicate());
    }

    /**
     * Signals {@code true} if the current {@code Single} signals a success value that is equal with
     * the value provided by calling a {@link BiPredicate}.
     * <p>
     * <img width="640" height="401" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.contains.f.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code contains} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param item the value to compare against the success value of this {@code Single}
     * @param comparer the function that receives the success value of this {@code Single}, the value provided
     *                 and should return {@code true} if they are considered equal
     * @return the new {@code Single} instance
     * @throws NullPointerException if {@code item} or {@code comparer} is {@code null}
     * @since 2.0
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Single<Boolean> contains(@NonNull Object item, @NonNull BiPredicate<Object, Object> comparer) {
        Objects.requireNonNull(item, "item is null");
        Objects.requireNonNull(comparer, "comparer is null");
        return RxJavaPlugins.onAssembly(new SingleContains<>(this, item, comparer));
    }

    /**
     * Flattens this {@code Single} and another {@link SingleSource} into one {@link Flowable}, without any transformation.
     * <p>
     * <img width="640" height="416" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.mergeWith.v3.png" alt="">
     * <p>
     * You can combine items emitted by multiple {@code SingleSource}s so that they appear as one {@code Flowable}, by using
     * the {@code mergeWith} method.
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The returned {@code Flowable} honors the backpressure of the downstream consumer.</dd>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code mergeWith} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param other
     *            a {@code SingleSource} to be merged
     * @return the new {@code Flowable} instance
     * @throws NullPointerException if {@code other} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/merge.html">ReactiveX operators documentation: Merge</a>
     */
    @BackpressureSupport(BackpressureKind.FULL)
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Flowable<T> mergeWith(@NonNull SingleSource<? extends T> other) {
        return merge(this, other);
    }
    /**
     * Filters the items emitted by the current {@code Single}, only emitting its success value if that
     * is an instance of the supplied {@link Class}.
     * <p>
     * <img width="640" height="399" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.ofType.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code ofType} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <U> the output type
     * @param clazz
     *            the class type to filter the items emitted by the current {@code Single}
     * @return the new {@link Maybe} instance
     * @throws NullPointerException if {@code clazz} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/filter.html">ReactiveX operators documentation: Filter</a>
     * @since 3.0.0
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public final <@NonNull U> Maybe<U> ofType(@NonNull Class<U> clazz) {
        Objects.requireNonNull(clazz, "clazz is null");
        return filter(Functions.isInstanceOf(clazz)).cast(clazz);
    }

    /**
     * Signals the success item or the terminal signals of the current {@code Single} on the specified {@link Scheduler},
     * asynchronously.
     * <p>
     * <img width="640" height="305" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.observeOn.v3.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>you specify which {@code Scheduler} this operator will use.</dd>
     * </dl>
     *
     * @param scheduler
     *            the {@code Scheduler} to notify subscribers on
     * @return the new {@code Single} instance
     * @throws NullPointerException if {@code scheduler} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/observeon.html">ReactiveX operators documentation: ObserveOn</a>
     * @see <a href="http://www.grahamlea.com/2014/07/rxjava-threading-examples/">RxJava Threading Examples</a>
     * @see #subscribeOn
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    public final Single<T> observeOn(@NonNull Scheduler scheduler) {
        Objects.requireNonNull(scheduler, "scheduler is null");
        return RxJavaPlugins.onAssembly(new SingleObserveOn<>(this, scheduler));
    }

    /**
     * Ends the flow with a success item returned by a function for the {@link Throwable} error signaled by the current
     * {@code Single} instead of signaling the error via {@code onError}.
     * <p>
     * <img width="640" height="461" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.onErrorReturn.v3.png" alt="">
     * <p>
     * By default, when a {@code Single} encounters an error that prevents it from emitting the expected item to its
     * subscriber, the {@code Single} invokes its subscriber's {@link SingleObserver#onError} method, and then quits
     * without invoking any more of its observer's methods. The {@code onErrorReturn} method changes this
     * behavior. If you pass a function ({@code resumeFunction}) to a {@code Single}'s {@code onErrorReturn} method, if
     * the original {@code Single} encounters an error, instead of invoking its observer's
     * {@link SingleObserver#onError} method, it will instead emit the return value of {@code resumeFunction}.
     * <p>
     * You can use this to prevent errors from propagating or to supply fallback data should errors be
     * encountered.
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code onErrorReturn} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param itemSupplier
     *            a function that returns an item that the new {@code Single} will emit if the current {@code Single} encounters
     *            an error
     * @return the new {@code Single} instance
     * @throws NullPointerException if {@code itemSupplier} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/catch.html">ReactiveX operators documentation: Catch</a>
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Single<T> onErrorReturn(@NonNull Function<Throwable, ? extends T> itemSupplier) {
        Objects.requireNonNull(itemSupplier, "itemSupplier is null");
        return RxJavaPlugins.onAssembly(new SingleOnErrorReturn<>(this, itemSupplier, null));
    }

    /**
     * Signals the specified value as success in case the current {@code Single} signals an error.
     * <p>
     * <img width="640" height="461" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.onErrorReturnItem.v3.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code onErrorReturnItem} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param item the value to signal if the current {@code Single} fails
     * @return the new {@code Single} instance
     * @throws NullPointerException if {@code item} is {@code null}
     * @since 2.0
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Single<T> onErrorReturnItem(@NonNull T item) {
        Objects.requireNonNull(item, "item is null");
        return RxJavaPlugins.onAssembly(new SingleOnErrorReturn<>(this, null, item));
    }

    /**
     * Resumes the flow with the given {@link SingleSource} when the current {@code Single} fails instead of
     * signaling the error via {@code onError}.
     * <p>
     * <img width="640" height="461" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.onErrorResumeWith.png" alt="">
     * <p>
     * By default, when a {@code Single} encounters an error that prevents it from emitting the expected item to
     * its {@link SingleObserver}, the {@code Single} invokes its {@code SingleObserver}'s {@code onError} method, and then quits
     * without invoking any more of its {@code SingleObserver}'s methods. The {@code onErrorResumeWith} method changes this
     * behavior. If you pass another {@code Single} ({@code resumeSingleInCaseOfError}) to a {@code Single}'s
     * {@code onErrorResumeWith} method, if the original {@code Single} encounters an error, instead of invoking its
     * {@code SingleObserver}'s {@code onError} method, it will instead relinquish control to {@code resumeSingleInCaseOfError} which
     * will invoke the {@code SingleObserver}'s {@link SingleObserver#onSuccess onSuccess} method if it is able to do so. In such a case,
     * because no {@code Single} necessarily invokes {@code onError}, the {@code SingleObserver} may never know that an error
     * happened.
     * <p>
     * You can use this to prevent errors from propagating or to supply fallback data should errors be
     * encountered.
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code onErrorResumeWith} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param fallback a {@code Single} that will take control if source {@code Single} encounters an error.
     * @return the new {@code Single} instance
     * @throws NullPointerException if {@code fallback} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/catch.html">ReactiveX operators documentation: Catch</a>
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Single<T> onErrorResumeWith(@NonNull SingleSource<? extends T> fallback) {
        Objects.requireNonNull(fallback, "fallback is null");
        return onErrorResumeNext(Functions.justFunction(fallback));
    }

    /**
     * Returns a {@link Maybe} instance that if the current {@code Single} emits an error, it will emit an {@code onComplete}
     * and swallow the throwable.
     * <p>
     * <img width="640" height="554" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.onErrorComplete.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code onErrorComplete} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @return the new {@code Maybe} instance
     * @since 3.0.0
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Maybe<T> onErrorComplete() {
        return onErrorComplete(Functions.alwaysTrue());
    }

    /**
     * Returns a {@link Maybe} instance that if this {@code Single} emits an error and the predicate returns
     * {@code true}, it will emit an {@code onComplete} and swallow the throwable.
     * <p>
     * <img width="640" height="270" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.onErrorComplete.f.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code onErrorComplete} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param predicate the predicate to call when an {@link Throwable} is emitted which should return {@code true}
     * if the {@code Throwable} should be swallowed and replaced with an {@code onComplete}.
     * @return the new {@code Maybe} instance
     * @throws NullPointerException if {@code predicate} is {@code null}
     * @since 3.0.0
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Maybe<T> onErrorComplete(@NonNull Predicate<? super Throwable> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");

        return RxJavaPlugins.onAssembly(new SingleOnErrorComplete<>(this, predicate));
    }

    /**
     * Resumes the flow with a {@link SingleSource} returned for the failure {@link Throwable} of the current {@code Single} by a
     * function instead of signaling the error via {@code onError}.
     * <p>
     * <img width="640" height="461" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.onErrorResumeNext.f.v3.png" alt="">
     * <p>
     * By default, when a {@code Single} encounters an error that prevents it from emitting the expected item to
     * its {@link SingleObserver}, the {@code Single} invokes its {@code SingleObserver}'s {@code onError} method, and then quits
     * without invoking any more of its {@code SingleObserver}'s methods. The {@code onErrorResumeNext} method changes this
     * behavior. If you pass a function that will return another {@code Single} ({@code resumeFunctionInCaseOfError}) to a {@code Single}'s
     * {@code onErrorResumeNext} method, if the original {@code Single} encounters an error, instead of invoking its
     * {@code SingleObserver}'s {@code onError} method, it will instead relinquish control to {@code resumeSingleInCaseOfError} which
     * will invoke the {@code SingleObserver}'s {@link SingleObserver#onSuccess onSuccess} method if it is able to do so. In such a case,
     * because no {@code Single} necessarily invokes {@code onError}, the {@code SingleObserver} may never know that an error
     * happened.
     * <p>
     * You can use this to prevent errors from propagating or to supply fallback data should errors be
     * encountered.
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code onErrorResumeNext} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param fallbackSupplier a function that returns a {@code SingleSource} that will take control if source {@code Single} encounters an error.
     * @return the new {@code Single} instance
     * @throws NullPointerException if {@code fallbackSupplier} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/catch.html">ReactiveX operators documentation: Catch</a>
     * @since .20
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Single<T> onErrorResumeNext(
            @NonNull Function<? super Throwable, ? extends SingleSource<? extends T>> fallbackSupplier) {
        Objects.requireNonNull(fallbackSupplier, "fallbackSupplier is null");
        return RxJavaPlugins.onAssembly(new SingleResumeNext<>(this, fallbackSupplier));
    }

    /**
     * Nulls out references to the upstream producer and downstream {@link SingleObserver} if
     * the sequence is terminated or downstream calls {@code dispose()}.
     * <p>
     * <img width="640" height="346" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.onTerminateDetach.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code onTerminateDetach} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * <p>History: 2.1.5 - experimental
     * @return the new {@code Single} which {@code null}s out references to the upstream producer and downstream {@code SingleObserver} if
     * the sequence is terminated or downstream calls {@code dispose()}
     * @since 2.2
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Single<T> onTerminateDetach() {
        return RxJavaPlugins.onAssembly(new SingleDetach<>(this));
    }

    /**
     * Repeatedly re-subscribes to the current {@code Single} and emits each success value as a {@link Flowable} sequence.
     * <p>
     * <img width="640" height="463" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.repeat.v3.png" alt="">
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The returned {@code Flowable} honors the backpressure of the downstream consumer.</dd>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code repeat} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @return the new {@code Flowable} instance
     * @since 2.0
     */
    @BackpressureSupport(BackpressureKind.FULL)
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Flowable<T> repeat() {
        return toFlowable().repeat();
    }

    /**
     * Re-subscribes to the current {@code Single} at most the given number of times and emits each success value as a {@link Flowable} sequence.
     * <p>
     * <img width="640" height="463" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.repeat.n.v3.png" alt="">
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The returned {@code Flowable} honors the backpressure of the downstream consumer.</dd>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code repeat} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param times the number of times to re-subscribe to the current {@code Single}
     * @return the new {@code Flowable} instance
     * @throws IllegalArgumentException if {@code times} is negative
     * @since 2.0
     */
    @BackpressureSupport(BackpressureKind.FULL)
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Flowable<T> repeat(long times) {
        return toFlowable().repeat(times);
    }

    /**
     * Re-subscribes to the current {@code Single} if
     * the {@link Publisher} returned by the handler function signals a value in response to a
     * value signaled through the {@link Flowable} the handler receives.
     * <p>
     * <img width="640" height="1480" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.repeatWhen.v3.png" alt="">
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The returned {@code Flowable} honors the backpressure of the downstream consumer.
     *  The {@code Publisher} returned by the handler function is expected to honor backpressure as well.</dd>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code repeatWhen} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param handler the function that is called with a {@code Flowable} that signals a value when the {@code Single}
     *                signaled a success value and returns a {@code Publisher} that has to signal a value to
     *                trigger a resubscription to the current {@code Single}, otherwise the terminal signal of
     *                the {@code Publisher} will be the terminal signal of the sequence as well.
     * @return the new {@code Flowable} instance
     * @throws NullPointerException if {@code handler} is {@code null}
     * @since 2.0
     */
    @BackpressureSupport(BackpressureKind.FULL)
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Flowable<T> repeatWhen(@NonNull Function<? super Flowable<Object>, @NonNull ? extends Publisher<@NonNull ?>> handler) {
        return toFlowable().repeatWhen(handler);
    }

    /**
     * Re-subscribes to the current {@code Single} until the given {@link BooleanSupplier} returns {@code true}
     * and emits the success items as a {@link Flowable} sequence.
     * <p>
     * <img width="640" height="463" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.repeatUntil.v3.png" alt="">
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The returned {@code Flowable} honors the backpressure of the downstream consumer.</dd>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code repeatUntil} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param stop the {@code BooleanSupplier} called after the current {@code Single} succeeds and if returns {@code false},
     *             the {@code Single} is re-subscribed; otherwise the sequence completes.
     * @return the new {@code Flowable} instance
     * @throws NullPointerException if {@code stop} is {@code null}
     * @since 2.0
     */
    @BackpressureSupport(BackpressureKind.FULL)
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Flowable<T> repeatUntil(@NonNull BooleanSupplier stop) {
        return toFlowable().repeatUntil(stop);
    }

    /**
     * Repeatedly re-subscribes to the current {@code Single} indefinitely if it fails with an {@code onError}.
     * <p>
     * <img width="640" height="399" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.retry.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code retry} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @return the new {@code Single} instance
     * @since 2.0
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Single<T> retry() {
        return toSingle(toFlowable().retry());
    }

    /**
     * Repeatedly re-subscribe at most the specified times to the current {@code Single}
     * if it fails with an {@code onError}.
     * <p>
     * <img width="640" height="329" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.retry.n.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code retry} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param times the number of times to resubscribe if the current {@code Single} fails
     * @return the new {@code Single} instance
     * @throws IllegalArgumentException if {@code times} is negative
     * @since 2.0
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Single<T> retry(long times) {
        return toSingle(toFlowable().retry(times));
    }

    /**
     * Re-subscribe to the current {@code Single} if the given predicate returns {@code true} when the {@code Single} fails
     * with an {@code onError}.
     * <p>
     * <img width="640" height="230" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.retry.f2.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code retry} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param predicate the predicate called with the resubscription count and the failure {@link Throwable}
     *                  and should return {@code true} if a resubscription should happen
     * @return the new {@code Single} instance
     * @throws NullPointerException if {@code predicate} is {@code null}
     * @since 2.0
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Single<T> retry(@NonNull BiPredicate<? super Integer, ? super Throwable> predicate) {
        return toSingle(toFlowable().retry(predicate));
    }

    /**
     * Repeatedly re-subscribe at most times or until the predicate returns {@code false}, whichever happens first
     * if it fails with an {@code onError}.
     * <p>
     * <img width="640" height="259" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.retry.nf.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code retry} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * <p>History: 2.1.8 - experimental
     * @param times the number of times to resubscribe if the current {@code Single} fails
     * @param predicate the predicate called with the failure {@link Throwable}
     *                  and should return {@code true} if a resubscription should happen
     * @return the new {@code Single} instance
     * @throws NullPointerException if {@code predicate} is {@code null}
     * @throws IllegalArgumentException if {@code times} is negative
     * @since 2.2
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Single<T> retry(long times, @NonNull Predicate<? super Throwable> predicate) {
        return toSingle(toFlowable().retry(times, predicate));
    }

    /**
     * Re-subscribe to the current {@code Single} if the given predicate returns {@code true} when the {@code Single} fails
     * with an {@code onError}.
     * <p>
     * <img width="640" height="240" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.retry.f.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code retry} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param predicate the predicate called with the failure {@link Throwable}
     *                  and should return {@code true} if a resubscription should happen
     * @return the new {@code Single} instance
     * @throws NullPointerException if {@code predicate} is {@code null}
     * @since 2.0
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Single<T> retry(@NonNull Predicate<? super Throwable> predicate) {
        return toSingle(toFlowable().retry(predicate));
    }

    /**
     * Retries until the given stop function returns {@code true}.
     * <p>
     * <img width="640" height="364" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.retryUntil.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code retryUntil} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param stop the function that should return {@code true} to stop retrying
     * @return the new {@code Single} instance
     * @throws NullPointerException if {@code stop} is {@code null}
     * @since 3.0.0
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Single<T> retryUntil(@NonNull BooleanSupplier stop) {
        Objects.requireNonNull(stop, "stop is null");
        return retry(Long.MAX_VALUE, Functions.predicateReverseFor(stop));
    }

    /**
     * Re-subscribes to the current {@code Single} if and when the {@link Publisher} returned by the handler
     * function signals a value.
     * <p>
     * <img width="640" height="405" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.retryWhen.png" alt="">
     * <p>
     * If the {@code Publisher} signals an {@code onComplete}, the resulting {@code Single} will signal a {@link NoSuchElementException}.
     * <p>
     * Note that the inner {@code Publisher} returned by the handler function should signal
     * either {@code onNext}, {@code onError} or {@code onComplete} in response to the received
     * {@link Throwable} to indicate the operator should retry or terminate. If the upstream to
     * the operator is asynchronous, signaling {@code onNext} followed by {@code onComplete} immediately may
     * result in the sequence to be completed immediately. Similarly, if this inner
     * {@code Publisher} signals {@code onError} or {@code onComplete} while the upstream is
     * active, the sequence is terminated with the same signal immediately.
     * <p>
     * The following example demonstrates how to retry an asynchronous source with a delay:
     * <pre><code>
     * Single.timer(1, TimeUnit.SECONDS)
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
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code retryWhen} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param handler the function that receives a {@link Flowable} of the error the {@code Single} emits and should
     *                return a {@code Publisher} that should signal a normal value (in response to the
     *                throwable the {@code Flowable} emits) to trigger a resubscription or signal an error to
     *                be the output of the resulting {@code Single}
     * @return the new {@code Single} instance
     * @throws NullPointerException if {@code handler} is {@code null}
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Single<T> retryWhen(@NonNull Function<? super Flowable<Throwable>, @NonNull ? extends Publisher<@NonNull ?>> handler) {
        return toSingle(toFlowable().retryWhen(handler));
    }

    /**
     * Wraps the given {@link SingleObserver}, catches any {@link RuntimeException}s thrown by its
     * {@link SingleObserver#onSubscribe(Disposable)}, {@link SingleObserver#onSuccess(Object)} or
     * {@link SingleObserver#onError(Throwable)} methods* and routes those to the global error handler
     * via {@link RxJavaPlugins#onError(Throwable)}.
     * <p>
     * By default, the {@code Single} protocol forbids the {@code onXXX} methods to throw, but some
     * {@code SingleObserver} implementation may do it anyway, causing undefined behavior in the
     * upstream. This method and the underlying safe wrapper ensures such misbehaving consumers don't
     * disrupt the protocol.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code safeSubscribe} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param observer the potentially misbehaving {@code SingleObserver}
     * @throws NullPointerException if {@code observer} is {@code null}
     * @see #subscribe(Consumer,Consumer)
     * @since 3.0.0
     */
    @SchedulerSupport(SchedulerSupport.NONE)
    public final void safeSubscribe(@NonNull SingleObserver<? super T> observer) {
        Objects.requireNonNull(observer, "observer is null");
        subscribe(new SafeSingleObserver<>(observer));
    }

    /**
     * Returns a {@link Flowable} which first runs the other {@link CompletableSource}
     * then the current {@code Single} if the other completed normally.
     * <p>
     * <img width="640" height="360" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.startWith.c.png" alt="">
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
     * then the current {@code Single} if the other succeeded normally.
     * <p>
     * <img width="640" height="341" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.startWith.s.png" alt="">
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
     * then the current {@code Single} if the other succeeded or completed normally.
     * <p>
     * <img width="640" height="232" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.startWith.m.png" alt="">
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
     * of the other {@link ObservableSource} then runs the current {@code Single}.
     * <p>
     * <img width="640" height="175" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.startWith.o.png" alt="">
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
     * of the other {@link Publisher} then runs the current {@code Single}.
     * <p>
     * <img width="640" height="175" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.startWith.p.png" alt="">
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
     * Subscribes to a {@code Single} but ignore its emission or notification.
     * <p>
     * <img width="640" height="340" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.subscribe.png" alt="">
     * <p>
     * If the {@code Single} emits an error, it is wrapped into an
     * {@link io.reactivex.rxjava3.exceptions.OnErrorNotImplementedException OnErrorNotImplementedException}
     * and routed to the {@link RxJavaPlugins#onError(Throwable)} handler.
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code subscribe} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @return the new {@link Disposable} instance that can be used for disposing the subscription at any time
     * @see <a href="http://reactivex.io/documentation/operators/subscribe.html">ReactiveX operators documentation: Subscribe</a>
     * @see #subscribe(Consumer, Consumer, DisposableContainer)
     */
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Disposable subscribe() {
        return subscribe(Functions.emptyConsumer(), Functions.ON_ERROR_MISSING);
    }

    /**
     * Subscribes to a {@code Single} and provides a composite callback to handle the item it emits
     * or any error notification it issues.
     * <p>
     * <img width="640" height="340" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.subscribe.c2.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code subscribe} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param onCallback
     *            the callback that receives either the success value or the failure {@link Throwable}
     *            (whichever is not {@code null})
     * @return the new {@link Disposable} instance that can be used for disposing the subscription at any time
     * @throws NullPointerException
     *             if {@code onCallback} is {@code null}
     * @see #subscribe(Consumer, Consumer, DisposableContainer)
     * @see <a href="http://reactivex.io/documentation/operators/subscribe.html">ReactiveX operators documentation: Subscribe</a>
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Disposable subscribe(@NonNull BiConsumer<@Nullable ? super T, @Nullable ? super Throwable> onCallback) {
        Objects.requireNonNull(onCallback, "onCallback is null");

        BiConsumerSingleObserver<T> observer = new BiConsumerSingleObserver<>(onCallback);
        subscribe(observer);
        return observer;
    }

    /**
     * Subscribes to a {@code Single} and provides a callback to handle the item it emits.
     * <p>
     * <img width="640" height="341" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.subscribe.c.png" alt="">
     * <p>
     * If the {@code Single} emits an error, it is wrapped into an
     * {@link io.reactivex.rxjava3.exceptions.OnErrorNotImplementedException OnErrorNotImplementedException}
     * and routed to the {@link RxJavaPlugins#onError(Throwable)} handler.
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code subscribe} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param onSuccess
     *            the {@code Consumer<T>} you have designed to accept the emission from the {@code Single}
     * @return the new {@link Disposable} instance that can be used for disposing the subscription at any time
     * @throws NullPointerException
     *             if {@code onSuccess} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/subscribe.html">ReactiveX operators documentation: Subscribe</a>
     * @see #subscribe(Consumer, Consumer, DisposableContainer)
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Disposable subscribe(@NonNull Consumer<? super T> onSuccess) {
        return subscribe(onSuccess, Functions.ON_ERROR_MISSING);
    }

    /**
     * Subscribes to a {@code Single} and provides callbacks to handle the item it emits or any error notification it
     * issues.
     * <p>
     * <img width="640" height="340" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.subscribe.cc.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code subscribe} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param onSuccess
     *            the {@code Consumer<T>} you have designed to accept the emission from the {@code Single}
     * @param onError
     *            the {@code Consumer<Throwable>} you have designed to accept any error notification from the
     *            {@code Single}
     * @return the new {@link Disposable} instance that can be used for disposing the subscription at any time
     * @throws NullPointerException
     *             if {@code onSuccess} or {@code onError} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/subscribe.html">ReactiveX operators documentation: Subscribe</a>
     * @see #subscribe(Consumer, Consumer, DisposableContainer)
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Disposable subscribe(@NonNull Consumer<? super T> onSuccess, @NonNull Consumer<? super Throwable> onError) {
        Objects.requireNonNull(onSuccess, "onSuccess is null");
        Objects.requireNonNull(onError, "onError is null");

        ConsumerSingleObserver<T> observer = new ConsumerSingleObserver<>(onSuccess, onError);
        subscribe(observer);
        return observer;
    }

    /**
     * Wraps the given onXXX callbacks into a {@link Disposable} {@link SingleObserver},
     * adds it to the given {@link DisposableContainer} and ensures, that if the upstream
     * terminates or this particular {@code Disposable} is disposed, the {@code SingleObserver} is removed
     * from the given container.
     * <p>
     * The {@code SingleObserver} will be removed after the callback for the terminal event has been invoked.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code subscribe} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param onSuccess the callback for upstream items
     * @param onError the callback for an upstream error if any
     * @param container the {@code DisposableContainer} (such as {@link CompositeDisposable}) to add and remove the
     *                  created {@code Disposable} {@code SingleObserver}
     * @return the {@code Disposable} that allows disposing the particular subscription.
     * @throws NullPointerException
     *             if {@code onSuccess}, {@code onError}
     *             or {@code container} is {@code null}
     * @since 3.1.0
     */
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Disposable subscribe(
            @NonNull Consumer<? super T> onSuccess,
            @NonNull Consumer<? super Throwable> onError,
            @NonNull DisposableContainer container) {
        Objects.requireNonNull(onSuccess, "onSuccess is null");
        Objects.requireNonNull(onError, "onError is null");
        Objects.requireNonNull(container, "container is null");

        DisposableAutoReleaseMultiObserver<T> observer = new DisposableAutoReleaseMultiObserver<>(
                container, onSuccess, onError, Functions.EMPTY_ACTION);
        container.add(observer);
        subscribe(observer);
        return observer;
    }

    @SchedulerSupport(SchedulerSupport.NONE)
    @Override
    public final void subscribe(@NonNull SingleObserver<? super T> observer) {
        Objects.requireNonNull(observer, "observer is null");

        observer = RxJavaPlugins.onSubscribe(this, observer);

        Objects.requireNonNull(observer, "The RxJavaPlugins.onSubscribe hook returned a null SingleObserver. Please check the handler provided to RxJavaPlugins.setOnSingleSubscribe for invalid null returns. Further reading: https://github.com/ReactiveX/RxJava/wiki/Plugins");

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
     * Implement this method in subclasses to handle the incoming {@link SingleObserver}s.
     * <p>There is no need to call any of the plugin hooks on the current {@code Single} instance or
     * the {@code SingleObserver}; all hooks and basic safeguards have been
     * applied by {@link #subscribe(SingleObserver)} before this method gets called.
     * @param observer the {@code SingleObserver} to handle, not {@code null}
     */
    protected abstract void subscribeActual(@NonNull SingleObserver<? super T> observer);

    /**
     * Subscribes a given {@link SingleObserver} (subclass) to this {@code Single} and returns the given
     * {@code SingleObserver} as is.
     * <p>
     * <img width="640" height="338" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.subscribeWith.png" alt="">
     * <p>Usage example:
     * <pre><code>
     * Single&lt;Integer&gt; source = Single.just(1);
     * CompositeDisposable composite = new CompositeDisposable();
     *
     * DisposableSingleObserver&lt;Integer&gt; ds = new DisposableSingleObserver&lt;&gt;() {
     *     // ...
     * };
     *
     * composite.add(source.subscribeWith(ds));
     * </code></pre>
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code subscribeWith} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <E> the type of the {@code SingleObserver} to use and return
     * @param observer the {@code SingleObserver} (subclass) to use and return, not {@code null}
     * @return the input {@code observer}
     * @throws NullPointerException if {@code observer} is {@code null}
     * @since 2.0
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <@NonNull E extends SingleObserver<? super T>> E subscribeWith(E observer) {
        subscribe(observer);
        return observer;
    }

    /**
     * Asynchronously subscribes {@link SingleObserver}s to this {@code Single} on the specified {@link Scheduler}.
     * <p>
     * <img width="640" height="305" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.subscribeOn.v3.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>You specify which {@code Scheduler} this operator will use.</dd>
     * </dl>
     *
     * @param scheduler
     *            the {@code Scheduler} to perform subscription actions on
     * @return the new {@code Single} instance
     * @throws NullPointerException if {@code scheduler} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/subscribeon.html">ReactiveX operators documentation: SubscribeOn</a>
     * @see <a href="http://www.grahamlea.com/2014/07/rxjava-threading-examples/">RxJava Threading Examples</a>
     * @see #observeOn
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    public final Single<T> subscribeOn(@NonNull Scheduler scheduler) {
        Objects.requireNonNull(scheduler, "scheduler is null");
        return RxJavaPlugins.onAssembly(new SingleSubscribeOn<>(this, scheduler));
    }

    /**
     * Measures the time (in milliseconds) between the subscription and success item emission
     * of the current {@code Single} and signals it as a tuple ({@link Timed})
     * success value.
     * <p>
     * <img width="640" height="466" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.timeInterval.png" alt="">
     * <p>
     * If the current {@code Single} fails, the resulting {@code Single} will
     * pass along the signal to the downstream. To measure the time to error,
     * use {@link #materialize()} and apply {@link #timeInterval()}.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code timeInterval} uses the {@code computation} {@link Scheduler}
     *  for determining the current time upon subscription and upon receiving the
     *  success item from the current {@code Single}.</dd>
     * </dl>
     * @return the new {@code Single} instance
     * @since 3.0.0
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.COMPUTATION)
    public final Single<Timed<T>> timeInterval() {
        return timeInterval(TimeUnit.MILLISECONDS, Schedulers.computation());
    }

    /**
     * Measures the time (in milliseconds) between the subscription and success item emission
     * of the current {@code Single} and signals it as a tuple ({@link Timed})
     * success value.
     * <p>
     * <img width="640" height="463" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.timeInterval.s.png" alt="">
     * <p>
     * If the current {@code Single} fails, the resulting {@code Single} will
     * pass along the signal to the downstream. To measure the time to error,
     * use {@link #materialize()} and apply {@link #timeInterval(Scheduler)}.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code timeInterval} uses the provided {@link Scheduler}
     *  for determining the current time upon subscription and upon receiving the
     *  success item from the current {@code Single}.</dd>
     * </dl>
     * @param scheduler the {@code Scheduler} used for providing the current time
     * @return the new {@code Single} instance
     * @throws NullPointerException if {@code scheduler} is {@code null}
     * @since 3.0.0
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    public final Single<Timed<T>> timeInterval(@NonNull Scheduler scheduler) {
        return timeInterval(TimeUnit.MILLISECONDS, scheduler);
    }

    /**
     * Measures the time between the subscription and success item emission
     * of the current {@code Single} and signals it as a tuple ({@link Timed})
     * success value.
     * <p>
     * <img width="640" height="466" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.timeInterval.png" alt="">
     * <p>
     * If the current {@code Single} fails, the resulting {@code Single} will
     * pass along the signals to the downstream. To measure the time to error,
     * use {@link #materialize()} and apply {@link #timeInterval(TimeUnit, Scheduler)}.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code timeInterval} uses the {@code computation} {@link Scheduler}
     *  for determining the current time upon subscription and upon receiving the
     *  success item from the current {@code Single}.</dd>
     * </dl>
     * @param unit the time unit for measurement
     * @return the new {@code Single} instance
     * @throws NullPointerException if {@code unit} is {@code null}
     * @since 3.0.0
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.COMPUTATION)
    public final Single<Timed<T>> timeInterval(@NonNull TimeUnit unit) {
        return timeInterval(unit, Schedulers.computation());
    }

    /**
     * Measures the time between the subscription and success item emission
     * of the current {@code Single} and signals it as a tuple ({@link Timed})
     * success value.
     * <p>
     * <img width="640" height="463" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.timeInterval.s.png" alt="">
     * <p>
     * If the current {@code Single} is empty or fails, the resulting {@code Single} will
     * pass along the signals to the downstream. To measure the time to termination,
     * use {@link #materialize()} and apply {@link #timeInterval(TimeUnit, Scheduler)}.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code timeInterval} uses the provided {@link Scheduler}
     *  for determining the current time upon subscription and upon receiving the
     *  success item from the current {@code Single}.</dd>
     * </dl>
     * @param unit the time unit for measurement
     * @param scheduler the {@code Scheduler} used for providing the current time
     * @return the new {@code Single} instance
     * @throws NullPointerException if {@code unit} or {@code scheduler} is {@code null}
     * @since 3.0.0
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    public final Single<Timed<T>> timeInterval(@NonNull TimeUnit unit, @NonNull Scheduler scheduler) {
        Objects.requireNonNull(unit, "unit is null");
        Objects.requireNonNull(scheduler, "scheduler is null");
        return RxJavaPlugins.onAssembly(new SingleTimeInterval<>(this, unit, scheduler, true));
    }

    /**
     * Combines the success value from the current {@code Single} with the current time (in milliseconds) of
     * its reception, using the {@code computation} {@link Scheduler} as time source,
     * then signals them as a {@link Timed} instance.
     * <p>
     * <img width="640" height="465" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.timestamp.png" alt="">
     * <p>
     * If the current {@code Single} is empty or fails, the resulting {@code Single} will
     * pass along the signals to the downstream. To get the timestamp of the error,
     * use {@link #materialize()} and apply {@link #timestamp()}.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code timestamp} uses the {@code computation} {@code Scheduler}
     *  for determining the current time upon receiving the
     *  success item from the current {@code Single}.</dd>
     * </dl>
     * @return the new {@code Single} instance
     * @since 3.0.0
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.COMPUTATION)
    public final Single<Timed<T>> timestamp() {
        return timestamp(TimeUnit.MILLISECONDS, Schedulers.computation());
    }

    /**
     * Combines the success value from the current {@code Single} with the current time (in milliseconds) of
     * its reception, using the given {@link Scheduler} as time source,
     * then signals them as a {@link Timed} instance.
     * <p>
     * <img width="640" height="465" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.timestamp.s.png" alt="">
     * <p>
     * If the current {@code Single} is empty or fails, the resulting {@code Single} will
     * pass along the signals to the downstream. To get the timestamp of the error,
     * use {@link #materialize()} and apply {@link #timestamp(Scheduler)}.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code timestamp} uses the provided {@code Scheduler}
     *  for determining the current time upon receiving the
     *  success item from the current {@code Single}.</dd>
     * </dl>
     * @param scheduler the {@code Scheduler} used for providing the current time
     * @return the new {@code Single} instance
     * @throws NullPointerException if {@code scheduler} is {@code null}
     * @since 3.0.0
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    public final Single<Timed<T>> timestamp(@NonNull Scheduler scheduler) {
        return timestamp(TimeUnit.MILLISECONDS, scheduler);
    }

    /**
     * Combines the success value from the current {@code Single} with the current time of
     * its reception, using the {@code computation} {@link Scheduler} as time source,
     * then signals it as a {@link Timed} instance.
     * <p>
     * <img width="640" height="465" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.timestamp.png" alt="">
     * <p>
     * If the current {@code Single} is empty or fails, the resulting {@code Single} will
     * pass along the signals to the downstream. To get the timestamp of the error,
     * use {@link #materialize()} and apply {@link #timestamp(TimeUnit)}.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code timestamp} uses the {@code computation} {@code Scheduler},
     *  for determining the current time upon receiving the
     *  success item from the current {@code Single}.</dd>
     * </dl>
     * @param unit the time unit for measurement
     * @return the new {@code Single} instance
     * @throws NullPointerException if {@code unit} is {@code null}
     * @since 3.0.0
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.COMPUTATION)
    public final Single<Timed<T>> timestamp(@NonNull TimeUnit unit) {
        return timestamp(unit, Schedulers.computation());
    }

    /**
     * Combines the success value from the current {@code Single} with the current time of
     * its reception, using the given {@link Scheduler} as time source,
     * then signals it as a {@link Timed} instance.
     * <p>
     * <img width="640" height="465" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.timestamp.s.png" alt="">
     * <p>
     * If the current {@code Single} is empty or fails, the resulting {@code Single} will
     * pass along the signals to the downstream. To get the timestamp of the error,
     * use {@link #materialize()} and apply {@link #timestamp(TimeUnit, Scheduler)}.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code timestamp} uses the provided {@code Scheduler},
     *  which is used for determining the current time upon receiving the
     *  success item from the current {@code Single}.</dd>
     * </dl>
     * @param unit the time unit for measurement
     * @param scheduler the {@code Scheduler} used for providing the current time
     * @return the new {@code Single} instance
     * @throws NullPointerException if {@code unit} or {@code scheduler} is {@code null}
     * @since 3.0.0
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    public final Single<Timed<T>> timestamp(@NonNull TimeUnit unit, @NonNull Scheduler scheduler) {
        Objects.requireNonNull(unit, "unit is null");
        Objects.requireNonNull(scheduler, "scheduler is null");
        return RxJavaPlugins.onAssembly(new SingleTimeInterval<>(this, unit, scheduler, false));
    }

    /**
     * Returns a {@code Single} that emits the item emitted by the current {@code Single} until a {@link CompletableSource} terminates. Upon
     * termination of {@code other}, this will emit a {@link CancellationException} rather than go to
     * {@link SingleObserver#onSuccess(Object)}.
     * <p>
     * <img width="640" height="333" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.takeUntil.c.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code takeUntil} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param other
     *            the {@code CompletableSource} whose termination will cause {@code takeUntil} to emit the item from the current
     *            {@code Single}
     * @return the new {@code Single} that emits the item emitted by the current {@code Single} until such time as {@code other} terminates.
     * @throws NullPointerException if {@code other} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/takeuntil.html">ReactiveX operators documentation: TakeUntil</a>
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Single<T> takeUntil(@NonNull CompletableSource other) {
        Objects.requireNonNull(other, "other is null");
        return takeUntil(new CompletableToFlowable<T>(other));
    }

    /**
     * Returns a {@code Single} that emits the item emitted by the current {@code Single} until a {@link Publisher} emits an item or completes. Upon
     * emission of an item from {@code other}, this will emit a {@link CancellationException} rather than go to
     * {@link SingleObserver#onSuccess(Object)}.
     * <p>
     * <img width="640" height="215" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.takeUntil.p.png" alt="">
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The {@code other} publisher is consumed in an unbounded fashion but will be
     *  cancelled after the first item it produced.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code takeUntil} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param other
     *            the {@code Publisher} whose first emitted item or completion will cause {@code takeUntil} to emit {@code CancellationException}
     *            if the current {@code Single} hasn't completed till then
     * @param <E>
     *            the type of items emitted by {@code other}
     * @return the new {@code Single} that emits the item emitted by the current {@code Single} until such time as {@code other} emits
     * its first item
     * @throws NullPointerException if {@code other} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/takeuntil.html">ReactiveX operators documentation: TakeUntil</a>
     */
    @BackpressureSupport(BackpressureKind.FULL)
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public final <@NonNull E> Single<T> takeUntil(@NonNull Publisher<E> other) {
        Objects.requireNonNull(other, "other is null");
        return RxJavaPlugins.onAssembly(new SingleTakeUntil<>(this, other));
    }

    /**
     * Returns a {@code Single} that emits the item emitted by the current {@code Single} until a second {@code Single} emits an item. Upon
     * emission of an item from {@code other}, this will emit a {@link CancellationException} rather than go to
     * {@link SingleObserver#onSuccess(Object)}.
     * <p>
     * <img width="640" height="314" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.takeUntil.s.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code takeUntil} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param other
     *            the {@code Single} whose emitted item will cause {@code takeUntil} to emit {@code CancellationException}
     *            if the current {@code Single} hasn't completed till then
     * @param <E>
     *            the type of item emitted by {@code other}
     * @return the new {@code Single} that emits the item emitted by the current {@code Single} until such time as {@code other} emits its item
     * @throws NullPointerException if {@code other} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/takeuntil.html">ReactiveX operators documentation: TakeUntil</a>
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public final <@NonNull E> Single<T> takeUntil(@NonNull SingleSource<? extends E> other) {
        Objects.requireNonNull(other, "other is null");
        return takeUntil(new SingleToFlowable<E>(other));
    }

    /**
     * Signals a {@link TimeoutException} if the current {@code Single} doesn't signal a success value within the
     * specified timeout window.
     * <p>
     * <img width="640" height="334" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.timeout.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code timeout} signals the {@code TimeoutException} on the {@code computation} {@link Scheduler}.</dd>
     * </dl>
     * @param timeout the timeout amount
     * @param unit the time unit
     * @return the new {@code Single} instance
     * @throws NullPointerException if {@code unit} is {@code null}
     * @since 2.0
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.COMPUTATION)
    @NonNull
    public final Single<T> timeout(long timeout, @NonNull TimeUnit unit) {
        return timeout0(timeout, unit, Schedulers.computation(), null);
    }

    /**
     * Signals a {@link TimeoutException} if the current {@code Single} doesn't signal a success value within the
     * specified timeout window.
     * <p>
     * <img width="640" height="334" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.timeout.s.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code timeout} signals the {@code TimeoutException} on the {@link Scheduler} you specify.</dd>
     * </dl>
     * @param timeout the timeout amount
     * @param unit the time unit
     * @param scheduler the target {@code Scheduler} where the timeout is awaited and the {@code TimeoutException}
     *                  signaled
     * @return the new {@code Single} instance
     * @throws NullPointerException if {@code unit} or {@code scheduler} is {@code null}
     * @since 2.0
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    @NonNull
    public final Single<T> timeout(long timeout, @NonNull TimeUnit unit, @NonNull Scheduler scheduler) {
        return timeout0(timeout, unit, scheduler, null);
    }

    /**
     * Runs the current {@code Single} and if it doesn't signal within the specified timeout window, it is
     * disposed and the other {@link SingleSource} subscribed to.
     * <p>
     * <img width="640" height="283" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.timeout.sb.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code timeout} subscribes to the other {@code SingleSource} on the {@link Scheduler} you specify.</dd>
     * </dl>
     * @param timeout the timeout amount
     * @param unit the time unit
     * @param scheduler the {@code Scheduler} where the timeout is awaited and the subscription to other happens
     * @param fallback the other {@code SingleSource} that gets subscribed to if the current {@code Single} times out
     * @return the new {@code Single} instance
     * @throws NullPointerException if {@code unit}, {@code scheduler} or {@code fallback} is {@code null}
     * @since 2.0
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    public final Single<T> timeout(long timeout, @NonNull TimeUnit unit, @NonNull Scheduler scheduler, @NonNull SingleSource<? extends T> fallback) {
        Objects.requireNonNull(fallback, "fallback is null");
        return timeout0(timeout, unit, scheduler, fallback);
    }

    /**
     * Runs the current {@code Single} and if it doesn't signal within the specified timeout window, it is
     * disposed and the other {@link SingleSource} subscribed to.
     * <p>
     * <img width="640" height="282" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.timeout.b.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code timeout} subscribes to the other {@code SingleSource} on
     *  the {@code computation} {@link Scheduler}.</dd>
     * </dl>
     * @param timeout the timeout amount
     * @param unit the time unit
     * @param fallback the other {@code SingleSource} that gets subscribed to if the current {@code Single} times out
     * @return the new {@code Single} instance
     * @throws NullPointerException
     *             if {@code fallback} or {@code unit} is {@code null}
     * @since 2.0
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.COMPUTATION)
    public final Single<T> timeout(long timeout, @NonNull TimeUnit unit, @NonNull SingleSource<? extends T> fallback) {
        Objects.requireNonNull(fallback, "fallback is null");
        return timeout0(timeout, unit, Schedulers.computation(), fallback);
    }

    private Single<T> timeout0(final long timeout, final TimeUnit unit, final Scheduler scheduler, final SingleSource<? extends T> fallback) {
        Objects.requireNonNull(unit, "unit is null");
        Objects.requireNonNull(scheduler, "scheduler is null");
        return RxJavaPlugins.onAssembly(new SingleTimeout<>(this, timeout, unit, scheduler, fallback));
    }

    /**
     * Calls the specified converter function during assembly time and returns its resulting value.
     * <p>
     * <img width="640" height="553" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.to.v3.png" alt="">
     * <p>
     * This allows fluent conversion to any other type.
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code to} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * <p>History: 2.1.7 - experimental
     * @param <R> the resulting object type
     * @param converter the function that receives the current {@code Single} instance and returns a value
     * @return the converted value
     * @throws NullPointerException if {@code converter} is {@code null}
     * @since 2.2
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public final <R> R to(@NonNull SingleConverter<T, ? extends R> converter) {
        return Objects.requireNonNull(converter, "converter is null").apply(this);
    }

    /**
     * Returns a {@link Completable} that ignores the success value of this {@code Single}
     * and signals {@code onComplete} instead.
     * <p>
     * <img width="640" height="436" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.ignoreElement.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code ignoreElement} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @return the new {@code Completable} instance
     * @since 2.1.13
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Completable ignoreElement() {
        return RxJavaPlugins.onAssembly(new CompletableFromSingle<>(this));
    }

    /**
     * Converts this {@code Single} into a {@link Flowable}.
     * <p>
     * <img width="640" height="462" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.toFlowable.v3.png" alt="">
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The returned {@code Flowable} honors the backpressure of the downstream consumer.</dd>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code toFlowable} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @return the new {@code Flowable} instance
     */
    @BackpressureSupport(BackpressureKind.FULL)
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @SuppressWarnings("unchecked")
    @NonNull
    public final Flowable<T> toFlowable() {
        if (this instanceof FuseToFlowable) {
            return ((FuseToFlowable<T>)this).fuseToFlowable();
        }
        return RxJavaPlugins.onAssembly(new SingleToFlowable<>(this));
    }

    /**
     * Returns a {@link Future} representing the single value emitted by this {@code Single}.
     * <p>
     * <img width="640" height="467" src="https://github.com/ReactiveX/RxJava/wiki/images/rx-operators/Single.toFuture.v3.png" alt="">
     * <p>
     * Cancelling the {@code Future} will cancel the subscription to the current {@code Single}.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code toFuture} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @return the new {@code Future} instance
     * @see <a href="http://reactivex.io/documentation/operators/to.html">ReactiveX documentation: To</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Future<T> toFuture() {
        return subscribeWith(new FutureMultiObserver<>());
    }

    /**
     * Converts this {@code Single} into a {@link Maybe}.
     * <p>
     * <img width="640" height="463" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.toMaybe.v3.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code toMaybe} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @return the new {@code Maybe} instance
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @SuppressWarnings("unchecked")
    @NonNull
    public final Maybe<T> toMaybe() {
        if (this instanceof FuseToMaybe) {
            return ((FuseToMaybe<T>)this).fuseToMaybe();
        }
        return RxJavaPlugins.onAssembly(new MaybeFromSingle<>(this));
    }
    /**
     * Converts this {@code Single} into an {@link Observable}.
     * <p>
     * <img width="640" height="305" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.toObservable.v3.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code toObservable} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @return the new {@code Observable} instance
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @SuppressWarnings("unchecked")
    @NonNull
    public final Observable<T> toObservable() {
        if (this instanceof FuseToObservable) {
            return ((FuseToObservable<T>)this).fuseToObservable();
        }
        return RxJavaPlugins.onAssembly(new SingleToObservable<>(this));
    }

    /**
     * Returns a {@code Single} which makes sure when a {@link SingleObserver} disposes the {@link Disposable},
     * that call is propagated up on the specified {@link Scheduler}.
     * <p>
     * <img width="640" height="693" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.unsubscribeOn.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code unsubscribeOn} calls {@code dispose()} of the upstream on the {@code Scheduler} you specify.</dd>
     * </dl>
     * <p>History: 2.0.9 - experimental
     * @param scheduler the target scheduler where to execute the disposal
     * @return the new {@code Single} instance
     * @throws NullPointerException if {@code scheduler} is {@code null}
     * @since 2.2
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    public final Single<T> unsubscribeOn(@NonNull Scheduler scheduler) {
        Objects.requireNonNull(scheduler, "scheduler is null");
        return RxJavaPlugins.onAssembly(new SingleUnsubscribeOn<>(this, scheduler));
    }

    /**
     * Returns a {@code Single} that emits the result of applying a specified function to the pair of items emitted by
     * the current {@code Single} and another specified {@link SingleSource}.
     * <p>
     * <img width="640" height="422" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.zipWith.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code zipWith} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <U>
     *            the type of items emitted by the {@code other} {@code Single}
     * @param <R>
     *            the type of items emitted by the resulting {@code Single}
     * @param other
     *            the other {@code SingleSource}
     * @param zipper
     *            a function that combines the pairs of items from the two {@code SingleSource}s to generate the items to
     *            be emitted by the resulting {@code Single}
     * @return the new {@code Single} that pairs up values from the current {@code Single} and the {@code other} {@code SingleSource}
     *         and emits the results of {@code zipFunction} applied to these pairs
     * @throws NullPointerException if {@code other} or {@code zipper} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/zip.html">ReactiveX operators documentation: Zip</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <@NonNull U, @NonNull R> Single<R> zipWith(@NonNull SingleSource<U> other, @NonNull BiFunction<? super T, ? super U, ? extends R> zipper) {
        return zip(this, other, zipper);
    }

    // -------------------------------------------------------------------------
    // Fluent test support, super handy and reduces test preparation boilerplate
    // -------------------------------------------------------------------------
    /**
     * Creates a {@link TestObserver} and subscribes it to this {@code Single}.
     * <p>
     * <img width="640" height="442" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.test.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code test} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @return the new {@code TestObserver} instance
     * @since 2.0
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
     * Creates a {@link TestObserver} optionally in cancelled state, then subscribes it to this {@code Single}.
     * <p>
     * <img width="640" height="482" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.test.b.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code test} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param dispose if {@code true}, the {@code TestObserver} will be cancelled before subscribing to this
     * {@code Single}.
     * @return the new {@code TestObserver} instance
     * @since 2.0
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

    @NonNull
    private static <T> Single<T> toSingle(@NonNull Flowable<T> source) {
        return RxJavaPlugins.onAssembly(new FlowableSingleSingle<>(source, null));
    }

    // -------------------------------------------------------------------------
    // JDK 8 Support
    // -------------------------------------------------------------------------

    /**
     * Signals the completion value or error of the given (hot) {@link CompletionStage}-based asynchronous calculation.
     * <p>
     * <img width="640" height="262" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/fromCompletionStage.s.png" alt="">
     * <p>
     * Note that the operator takes an already instantiated, running or terminated {@code CompletionStage}.
     * If the optional is to be created per consumer upon subscription, use {@link #defer(Supplier)}
     * around {@code fromCompletionStage}:
     * <pre><code>
     * Single.defer(() -&gt; Single.fromCompletionStage(createCompletionStage()));
     * </code></pre>
     * <p>
     * If the {@code CompletionStage} completes with {@code null}, the resulting {@code Single} is terminated with
     * a {@link NullPointerException}.
     * <p>
     * Canceling the flow can't cancel the execution of the {@code CompletionStage} because {@code CompletionStage}
     * itself doesn't support cancellation. Instead, the operator detaches from the {@code CompletionStage}.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code fromCompletionStage} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the element type of the {@code CompletionStage}
     * @param stage the {@code CompletionStage} to convert to {@code Single} and signal its success value or error
     * @return the new {@code Single} instance
     * @throws NullPointerException if {@code stage} is {@code null}
     * @since 3.0.0
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public static <@NonNull T> Single<@NonNull T> fromCompletionStage(@NonNull CompletionStage<T> stage) {
        Objects.requireNonNull(stage, "stage is null");
        return RxJavaPlugins.onAssembly(new SingleFromCompletionStage<>(stage));
    }

    /**
     * Maps the upstream success value into an {@link Optional} and emits the contained item if not empty as a {@link Maybe}.
     * <p>
     * <img width="640" height="323" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/mapOptional.s.png" alt="">
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
        return RxJavaPlugins.onAssembly(new SingleMapOptional<>(this, mapper));
    }

    /**
     * Signals the upstream success item (or error) via a {@link CompletionStage}.
     * <p>
     * <img width="640" height="321" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/toCompletionStage.s.png" alt="">
     * <p>
     * The upstream can be canceled by converting the resulting {@code CompletionStage} into
     * {@link CompletableFuture} via {@link CompletionStage#toCompletableFuture()} and
     * calling {@link CompletableFuture#cancel(boolean)} on it.
     * The upstream will be also cancelled if the resulting {@code CompletionStage} is converted to and
     * completed manually by {@link CompletableFuture#complete(Object)} or {@link CompletableFuture#completeExceptionally(Throwable)}.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code toCompletionStage} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @return the new {@code CompletionStage} instance
     * @since 3.0.0
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final CompletionStage<T> toCompletionStage() {
        return subscribeWith(new CompletionStageConsumer<>(false, null));
    }

    /**
     * Maps the upstream succecss value into a Java {@link Stream} and emits its
     * items to the downstream consumer as a {@link Flowable}.
     * <img width="640" height="247" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/flattenStreamAsFlowable.s.png" alt="">
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
        return RxJavaPlugins.onAssembly(new SingleFlattenStreamAsFlowable<>(this, mapper));
    }

    /**
     * Maps the upstream succecss value into a Java {@link Stream} and emits its
     * items to the downstream consumer as an {@link Observable}.
     * <p>
     * <img width="640" height="241" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/flattenStreamAsObservable.s.png" alt="">
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
        return RxJavaPlugins.onAssembly(new SingleFlattenStreamAsObservable<>(this, mapper));
    }
}
