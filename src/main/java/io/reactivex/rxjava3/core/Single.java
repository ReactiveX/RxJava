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

package io.reactivex.rxjava3.core;

import java.util.*;
import java.util.concurrent.*;

import org.reactivestreams.Publisher;

import io.reactivex.rxjava3.annotations.*;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.exceptions.Exceptions;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.internal.functions.*;
import io.reactivex.rxjava3.internal.fuseable.*;
import io.reactivex.rxjava3.internal.jdk8.*;
import io.reactivex.rxjava3.internal.observers.*;
import io.reactivex.rxjava3.internal.operators.completable.*;
import io.reactivex.rxjava3.internal.operators.flowable.*;
import io.reactivex.rxjava3.internal.operators.maybe.*;
import io.reactivex.rxjava3.internal.operators.mixed.SingleFlatMapObservable;
import io.reactivex.rxjava3.internal.operators.observable.*;
import io.reactivex.rxjava3.internal.operators.single.*;
import io.reactivex.rxjava3.internal.util.ErrorMode;
import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * The {@code Single} class implements the Reactive Pattern for a single value response.
 * <p>
 * {@code Single} behaves similarly to {@link Observable} except that it can only emit either a single successful
 * value or an error (there is no "onComplete" notification as there is for an {@link Observable}).
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
 * <img width="640" height="301" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.legend.png" alt="">
 * <p>
 * See {@link Flowable} or {@link Observable} for the
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
 *            the type of the item emitted by the Single
 * @since 2.0
 * @see io.reactivex.rxjava3.observers.DisposableSingleObserver
 */
public abstract class Single<T> implements SingleSource<T> {

    /**
     * Runs multiple SingleSources and signals the events of the first one that signals (disposing
     * the rest).
     * <p>
     * <img width="640" height="515" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.amb.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code amb} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the value type
     * @param sources the Iterable sequence of sources. A subscription to each source will
     *            occur in the same order as in this Iterable.
     * @return the new Single instance
     * @since 2.0
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <T> Single<T> amb(@NonNull Iterable<? extends SingleSource<? extends T>> sources) {
        Objects.requireNonNull(sources, "sources is null");
        return RxJavaPlugins.onAssembly(new SingleAmb<>(null, sources));
    }

    /**
     * Runs multiple SingleSources and signals the events of the first one that signals (disposing
     * the rest).
     * <p>
     * <img width="640" height="515" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.ambArray.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code ambArray} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the value type
     * @param sources the array of sources. A subscription to each source will
     *            occur in the same order as in this array.
     * @return the new Single instance
     * @since 2.0
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @SafeVarargs
    @NonNull
    public static <T> Single<T> ambArray(@NonNull SingleSource<? extends T>... sources) {
        if (sources.length == 0) {
            return error(SingleInternalHelper.<T>emptyThrower());
        }
        if (sources.length == 1) {
            @SuppressWarnings("unchecked")
            SingleSource<T> source = (SingleSource<T>)sources[0];
            return wrap(source);
        }
        return RxJavaPlugins.onAssembly(new SingleAmb<>(sources, null));
    }

    /**
     * Concatenate the single values, in a non-overlapping fashion, of the SingleSources provided by
     * an Iterable sequence.
     * <p>
     * <img width="640" height="319" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.concat.i.png" alt="">
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The returned {@code Flowable} honors the backpressure of the downstream consumer.</dd>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code concat} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the value type
     * @param sources the Iterable sequence of SingleSource instances
     * @return the new Flowable instance
     * @since 2.0
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    @BackpressureSupport(BackpressureKind.FULL)
    public static <T> Flowable<T> concat(@NonNull Iterable<? extends SingleSource<? extends T>> sources) {
        return concat(Flowable.fromIterable(sources));
    }

    /**
     * Concatenate the single values, in a non-overlapping fashion, of the SingleSources provided by
     * an Observable sequence.
     * <p>
     * <img width="640" height="319" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.concat.o.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code concat} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the value type
     * @param sources the ObservableSource of SingleSource instances
     * @return the new Observable instance
     * @since 2.0
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static <T> Observable<T> concat(@NonNull ObservableSource<? extends SingleSource<? extends T>> sources) {
        Objects.requireNonNull(sources, "sources is null");
        return RxJavaPlugins.onAssembly(new ObservableConcatMap(sources, SingleInternalHelper.toObservable(), 2, ErrorMode.IMMEDIATE));
    }

    /**
     * Concatenate the single values, in a non-overlapping fashion, of the SingleSources provided by
     * a Publisher sequence.
     * <p>
     * <img width="640" height="308" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.concat.p.png" alt="">
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The returned {@code Flowable} honors the backpressure of the downstream consumer
     *  and the sources {@code Publisher} is expected to honor it as well.</dd>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code concat} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the value type
     * @param sources the Publisher of SingleSource instances
     * @return the new Flowable instance
     * @since 2.0
     */
    @CheckReturnValue
    @NonNull
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <T> Flowable<T> concat(@NonNull Publisher<? extends SingleSource<? extends T>> sources) {
        return concat(sources, 2);
    }

    /**
     * Concatenate the single values, in a non-overlapping fashion, of the SingleSources provided by
     * a Publisher sequence and prefetched by the specified amount.
     * <p>
     * <img width="640" height="310" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.concat.pn.png" alt="">
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The returned {@code Flowable} honors the backpressure of the downstream consumer
     *  and the sources {@code Publisher} is expected to honor it as well.</dd>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code concat} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the value type
     * @param sources the Publisher of SingleSource instances
     * @param prefetch the number of SingleSources to prefetch from the Publisher
     * @return the new Flowable instance
     * @since 2.0
     */
    @CheckReturnValue
    @NonNull
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerSupport.NONE)
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static <T> Flowable<T> concat(@NonNull Publisher<? extends SingleSource<? extends T>> sources, int prefetch) {
        Objects.requireNonNull(sources, "sources is null");
        ObjectHelper.verifyPositive(prefetch, "prefetch");
        return RxJavaPlugins.onAssembly(new FlowableConcatMapPublisher(sources, SingleInternalHelper.toFlowable(), prefetch, ErrorMode.IMMEDIATE));
    }

    /**
     * Returns a Flowable that emits the items emitted by two Singles, one after the other.
     * <p>
     * <img width="640" height="366" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.concat.png" alt="">
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The returned {@code Flowable} honors the backpressure of the downstream consumer.</dd>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code concat} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T> the common value type
     * @param source1
     *            a Single to be concatenated
     * @param source2
     *            a Single to be concatenated
     * @return a Flowable that emits items emitted by the two source Singles, one after the other.
     * @see <a href="http://reactivex.io/documentation/operators/concat.html">ReactiveX operators documentation: Concat</a>
     */
    @CheckReturnValue
    @NonNull
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <T> Flowable<T> concat(
            @NonNull SingleSource<? extends T> source1, @NonNull SingleSource<? extends T> source2
     ) {
        Objects.requireNonNull(source1, "source1 is null");
        Objects.requireNonNull(source2, "source2 is null");
        return concat(Flowable.fromArray(source1, source2));
    }

    /**
     * Returns a Flowable that emits the items emitted by three Singles, one after the other.
     * <p>
     * <img width="640" height="366" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.concat.o3.png" alt="">
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The returned {@code Flowable} honors the backpressure of the downstream consumer.</dd>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code concat} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T> the common value type
     * @param source1
     *            a Single to be concatenated
     * @param source2
     *            a Single to be concatenated
     * @param source3
     *            a Single to be concatenated
     * @return a Flowable that emits items emitted by the three source Singles, one after the other.
     * @see <a href="http://reactivex.io/documentation/operators/concat.html">ReactiveX operators documentation: Concat</a>
     */
    @CheckReturnValue
    @NonNull
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <T> Flowable<T> concat(
            @NonNull SingleSource<? extends T> source1, @NonNull SingleSource<? extends T> source2,
            @NonNull SingleSource<? extends T> source3
     ) {
        Objects.requireNonNull(source1, "source1 is null");
        Objects.requireNonNull(source2, "source2 is null");
        Objects.requireNonNull(source3, "source3 is null");
        return concat(Flowable.fromArray(source1, source2, source3));
    }

    /**
     * Returns a Flowable that emits the items emitted by four Singles, one after the other.
     * <p>
     * <img width="640" height="362" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.concat.o4.png" alt="">
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The returned {@code Flowable} honors the backpressure of the downstream consumer.</dd>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code concat} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T> the common value type
     * @param source1
     *            a Single to be concatenated
     * @param source2
     *            a Single to be concatenated
     * @param source3
     *            a Single to be concatenated
     * @param source4
     *            a Single to be concatenated
     * @return a Flowable that emits items emitted by the four source Singles, one after the other.
     * @see <a href="http://reactivex.io/documentation/operators/concat.html">ReactiveX operators documentation: Concat</a>
     */
    @CheckReturnValue
    @NonNull
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <T> Flowable<T> concat(
            @NonNull SingleSource<? extends T> source1, @NonNull SingleSource<? extends T> source2,
            @NonNull SingleSource<? extends T> source3, @NonNull SingleSource<? extends T> source4
     ) {
        Objects.requireNonNull(source1, "source1 is null");
        Objects.requireNonNull(source2, "source2 is null");
        Objects.requireNonNull(source3, "source3 is null");
        Objects.requireNonNull(source4, "source4 is null");
        return concat(Flowable.fromArray(source1, source2, source3, source4));
    }

    /**
     * Concatenate the single values, in a non-overlapping fashion, of the SingleSources provided in
     * an array.
     * <p>
     * <img width="640" height="319" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.concatArray.png" alt="">
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The returned {@code Flowable} honors the backpressure of the downstream consumer.</dd>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code concatArray} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the value type
     * @param sources the array of SingleSource instances
     * @return the new Flowable instance
     * @since 2.0
     */
    @CheckReturnValue
    @NonNull
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerSupport.NONE)
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @SafeVarargs
    public static <T> Flowable<T> concatArray(@NonNull SingleSource<? extends T>... sources) {
        return RxJavaPlugins.onAssembly(new FlowableConcatMap(Flowable.fromArray(sources), SingleInternalHelper.toFlowable(), 2, ErrorMode.BOUNDARY));
    }

    /**
     * Concatenates a sequence of SingleSource eagerly into a single stream of values.
     * <p>
     * <img width="640" height="257" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.concatArrayEager.png" alt="">
     * <p>
     * Eager concatenation means that once a subscriber subscribes, this operator subscribes to all of the
     * source SingleSources. The operator buffers the value emitted by these SingleSources and then drains them
     * in order, each one after the previous one completes.
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The operator honors backpressure from downstream.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This method does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the value type
     * @param sources a sequence of Single that need to be eagerly concatenated
     * @return the new Flowable instance with the specified concatenation behavior
     */
    @BackpressureSupport(BackpressureKind.FULL)
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    @SafeVarargs
    public static <T> Flowable<T> concatArrayEager(@NonNull SingleSource<? extends T>... sources) {
        return Flowable.fromArray(sources).concatMapEager(SingleInternalHelper.<T>toFlowable());
    }

    /**
     * Concatenates a Publisher sequence of SingleSources eagerly into a single stream of values.
     * <p>
     * <img width="640" height="307" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.concatEager.p.png" alt="">
     * <p>
     * Eager concatenation means that once a subscriber subscribes, this operator subscribes to all of the
     * emitted source Publishers as they are observed. The operator buffers the values emitted by these
     * Publishers and then drains them in order, each one after the previous one completes.
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>Backpressure is honored towards the downstream and the outer Publisher is
     *  expected to support backpressure. Violating this assumption, the operator will
     *  signal {@link io.reactivex.rxjava3.exceptions.MissingBackpressureException}.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This method does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the value type
     * @param sources a sequence of Publishers that need to be eagerly concatenated
     * @return the new Publisher instance with the specified concatenation behavior
     */
    @BackpressureSupport(BackpressureKind.FULL)
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <T> Flowable<T> concatEager(@NonNull Publisher<? extends SingleSource<? extends T>> sources) {
        return Flowable.fromPublisher(sources).concatMapEager(SingleInternalHelper.<T>toFlowable());
    }

    /**
     * Concatenates a sequence of SingleSources eagerly into a single stream of values.
     * <p>
     * <img width="640" height="319" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.concatEager.i.png" alt="">
     * <p>
     * Eager concatenation means that once a subscriber subscribes, this operator subscribes to all of the
     * source SingleSources. The operator buffers the values emitted by these SingleSources and then drains them
     * in order, each one after the previous one completes.
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>Backpressure is honored towards the downstream.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This method does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the value type
     * @param sources a sequence of SingleSource that need to be eagerly concatenated
     * @return the new Flowable instance with the specified concatenation behavior
     */
    @BackpressureSupport(BackpressureKind.FULL)
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <T> Flowable<T> concatEager(@NonNull Iterable<? extends SingleSource<? extends T>> sources) {
        return Flowable.fromIterable(sources).concatMapEager(SingleInternalHelper.<T>toFlowable());
    }

    /**
     * Provides an API (via a cold Single) that bridges the reactive world with the callback-style world.
     * <p>
     * <img width="640" height="454" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.create.png" alt="">
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
     * disposes the flow (making {@link SingleEmitter#isDisposed} return true),
     * other observers subscribed to the same returned {@code Single} are not affected.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code create} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the value type
     * @param source the emitter that is called when a SingleObserver subscribes to the returned {@code Single}
     * @return the new Single instance
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
     * <img width="640" height="515" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.defer.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code defer} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the value type
     * @param singleSupplier the {@code Supplier} that is called for each individual {@code SingleObserver} and
     * returns a SingleSource instance to subscribe to
     * @return the new Single instance
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <T> Single<T> defer(@NonNull Supplier<? extends SingleSource<? extends T>> singleSupplier) {
        Objects.requireNonNull(singleSupplier, "singleSupplier is null");
        return RxJavaPlugins.onAssembly(new SingleDefer<>(singleSupplier));
    }

    /**
     * Signals a Throwable returned by the callback function for each individual SingleObserver.
     * <p>
     * <img width="640" height="283" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.error.c.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code error} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the value type
     * @param errorSupplier the Supplier that is called for each individual SingleObserver and
     * returns a Throwable instance to be emitted.
     * @return the new Single instance
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <T> Single<T> error(@NonNull Supplier<? extends Throwable> errorSupplier) {
        Objects.requireNonNull(errorSupplier, "errorSupplier is null");
        return RxJavaPlugins.onAssembly(new SingleError<T>(errorSupplier));
    }

    /**
     * Returns a Single that invokes a subscriber's {@link SingleObserver#onError onError} method when the
     * subscriber subscribes to it.
     * <p>
     * <img width="640" height="283" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.error.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code error} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param exception
     *            the particular Throwable to pass to {@link SingleObserver#onError onError}
     * @param <T>
     *            the type of the item (ostensibly) emitted by the Single
     * @return a Single that invokes the subscriber's {@link SingleObserver#onError onError} method when
     *         the subscriber subscribes to it
     * @see <a href="http://reactivex.io/documentation/operators/empty-never-throw.html">ReactiveX operators documentation: Throw</a>
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <T> Single<T> error(@NonNull Throwable exception) {
        Objects.requireNonNull(exception, "exception is null");
        return error(Functions.justSupplier(exception));
    }

    /**
     * Returns a {@link Single} that invokes passed function and emits its result for each new SingleObserver that subscribes.
     * <p>
     * Allows you to defer execution of passed function until SingleObserver subscribes to the {@link Single}.
     * It makes passed function "lazy".
     * Result of the function invocation will be emitted by the {@link Single}.
     * <p>
     * <img width="640" height="467" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.fromCallable.png" alt="">
     * <dl>
     *   <dt><b>Scheduler:</b></dt>
     *   <dd>{@code fromCallable} does not operate by default on a particular {@link Scheduler}.</dd>
     *   <dt><b>Error handling:</b></dt>
     *   <dd> If the {@link Callable} throws an exception, the respective {@link Throwable} is
     *   delivered to the downstream via {@link SingleObserver#onError(Throwable)},
     *   except when the downstream has disposed this {@code Single} source.
     *   In this latter case, the {@code Throwable} is delivered to the global error handler via
     *   {@link RxJavaPlugins#onError(Throwable)} as an {@link io.reactivex.rxjava3.exceptions.UndeliverableException UndeliverableException}.
     *   </dd>
     * </dl>
     *
     * @param callable
     *         function which execution should be deferred, it will be invoked when SingleObserver will subscribe to the {@link Single}.
     * @param <T>
     *         the type of the item emitted by the {@link Single}.
     * @return a {@link Single} whose {@link SingleObserver}s' subscriptions trigger an invocation of the given function.
     * @see #defer(Supplier)
     * @see #fromSupplier(Supplier)
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <@NonNull T> Single<T> fromCallable(@NonNull Callable<? extends T> callable) {
        Objects.requireNonNull(callable, "callable is null");
        return RxJavaPlugins.onAssembly(new SingleFromCallable<T>(callable));
    }

    /**
     * Converts a {@link Future} into a {@code Single}.
     * <p>
     * <img width="640" height="315" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.from.Future.png" alt="">
     * <p>
     * You can convert any object that supports the {@link Future} interface into a Single that emits the return
     * value of the {@link Future#get} method of that object, by passing the object into the {@code from}
     * method.
     * <p>
     * <em>Important note:</em> This Single is blocking; you cannot dispose it.
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code fromFuture} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param future
     *            the source {@link Future}
     * @param <T>
     *            the type of object that the {@link Future} returns, and also the type of item to be emitted by
     *            the resulting {@code Single}
     * @return a {@code Single} that emits the item from the source {@link Future}
     * @see <a href="http://reactivex.io/documentation/operators/from.html">ReactiveX operators documentation: From</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public static <@NonNull T> Single<T> fromFuture(@NonNull Future<? extends T> future) {
        return toSingle(Flowable.<T>fromFuture(future));
    }

    /**
     * Converts a {@link Future} into a {@code Single}, with a timeout on the Future.
     * <p>
     * <img width="640" height="315" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.from.Future.png" alt="">
     * <p>
     * You can convert any object that supports the {@link Future} interface into a {@code Single} that emits
     * the return value of the {@link Future#get} method of that object, by passing the object into the
     * {@code from} method.
     * <p>
     * <em>Important note:</em> This {@code Single} is blocking; you cannot dispose it.
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code fromFuture} does not operate by default on a particular {@link Scheduler}.</dd>
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
     *            the resulting {@code Single}
     * @return a {@code Single} that emits the item from the source {@link Future}
     * @see <a href="http://reactivex.io/documentation/operators/from.html">ReactiveX operators documentation: From</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public static <@NonNull T> Single<T> fromFuture(@NonNull Future<? extends T> future, long timeout, @NonNull TimeUnit unit) {
        return toSingle(Flowable.<T>fromFuture(future, timeout, unit));
    }

    /**
     * Converts a {@link Future} into a {@code Single}, with a timeout on the Future.
     * <p>
     * <img width="640" height="315" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.from.Future.png" alt="">
     * <p>
     * You can convert any object that supports the {@link Future} interface into a {@code Single} that emits
     * the return value of the {@link Future#get} method of that object, by passing the object into the
     * {@code from} method.
     * <p>
     * <em>Important note:</em> This {@code Single} is blocking; you cannot dispose it.
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>You specify the {@link Scheduler} where the blocking wait will happen.</dd>
     * </dl>
     *
     * @param future
     *            the source {@link Future}
     * @param timeout
     *            the maximum time to wait before calling {@code get}
     * @param unit
     *            the {@link TimeUnit} of the {@code timeout} argument
     * @param scheduler
     *            the Scheduler to use for the blocking wait
     * @param <T>
     *            the type of object that the {@link Future} returns, and also the type of item to be emitted by
     *            the resulting {@code Single}
     * @return a {@code Single} that emits the item from the source {@link Future}
     * @see <a href="http://reactivex.io/documentation/operators/from.html">ReactiveX operators documentation: From</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    @NonNull
    public static <@NonNull T> Single<T> fromFuture(@NonNull Future<? extends T> future, long timeout, @NonNull TimeUnit unit, @NonNull Scheduler scheduler) {
        return toSingle(Flowable.<T>fromFuture(future, timeout, unit, scheduler));
    }

    /**
     * Converts a {@link Future}, operating on a specified {@link Scheduler}, into a {@code Single}.
     * <p>
     * <img width="640" height="315" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.from.Future.s.png" alt="">
     * <p>
     * You can convert any object that supports the {@link Future} interface into a {@code Single} that emits
     * the return value of the {@link Future#get} method of that object, by passing the object into the
     * {@code from} method.
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>You specify which {@link Scheduler} this operator will use.</dd>
     * </dl>
     *
     * @param future
     *            the source {@link Future}
     * @param scheduler
     *            the {@link Scheduler} to wait for the Future on. Use a Scheduler such as
     *            {@link Schedulers#io()} that can block and wait on the Future
     * @param <T>
     *            the type of object that the {@link Future} returns, and also the type of item to be emitted by
     *            the resulting {@code Single}
     * @return a {@code Single} that emits the item from the source {@link Future}
     * @see <a href="http://reactivex.io/documentation/operators/from.html">ReactiveX operators documentation: From</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    @NonNull
    public static <@NonNull T> Single<T> fromFuture(@NonNull Future<? extends T> future, @NonNull Scheduler scheduler) {
        return toSingle(Flowable.<T>fromFuture(future, scheduler));
    }

    /**
     * Wraps a specific Publisher into a Single and signals its single element or error.
     * <p>If the source Publisher is empty, a NoSuchElementException is signalled. If
     * the source has more than one element, an IndexOutOfBoundsException is signalled.
     * <p>
     * The {@link Publisher} must follow the
     * <a href="https://github.com/reactive-streams/reactive-streams-jvm#reactive-streams">Reactive-Streams specification</a>.
     * Violating the specification may result in undefined behavior.
     * <p>
     * If possible, use {@link #create(SingleOnSubscribe)} to create a
     * source-like {@code Single} instead.
     * <p>
     * Note that even though {@link Publisher} appears to be a functional interface, it
     * is not recommended to implement it through a lambda as the specification requires
     * state management that is not achievable with a stateless lambda.
     * <p>
     * <img width="640" height="322" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.fromPublisher.png" alt="">
     * <dl>
     * <dt><b>Backpressure:</b></dt>
     * <dd>The {@code publisher} is consumed in an unbounded fashion but will be cancelled
     * if it produced more than one item.</dd>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code fromPublisher} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the value type
     * @param publisher the source Publisher instance, not null
     * @return the new Single instance
     * @see #create(SingleOnSubscribe)
     */
    @BackpressureSupport(BackpressureKind.UNBOUNDED_IN)
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <T> Single<T> fromPublisher(@NonNull Publisher<? extends T> publisher) {
        Objects.requireNonNull(publisher, "publisher is null");
        return RxJavaPlugins.onAssembly(new SingleFromPublisher<T>(publisher));
    }

    /**
     * Wraps a specific ObservableSource into a Single and signals its single element or error.
     * <p>If the ObservableSource is empty, a NoSuchElementException is signalled.
     * If the source has more than one element, an IndexOutOfBoundsException is signalled.
     * <p>
     * <img width="640" height="343" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.fromObservable.png" alt="">
     * <dl>
     *   <dt><b>Scheduler:</b></dt>
     *   <dd>{@code fromObservable} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param observableSource the source Observable, not null
     * @param <T>
     *         the type of the item emitted by the {@link Single}.
     * @return the new Single instance
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <T> Single<T> fromObservable(@NonNull ObservableSource<? extends T> observableSource) {
        Objects.requireNonNull(observableSource, "observableSource is null");
        return RxJavaPlugins.onAssembly(new ObservableSingleSingle<T>(observableSource, null));
    }

    /**
     * Returns a {@link Single} that invokes passed supplierfunction and emits its result
     * for each new SingleObserver that subscribes.
     * <p>
     * Allows you to defer execution of passed function until SingleObserver subscribes to the {@link Single}.
     * It makes passed function "lazy".
     * Result of the function invocation will be emitted by the {@link Single}.
     * <p>
     * <img width="640" height="467" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.fromSupplier.png" alt="">
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
     *         function which execution should be deferred, it will be invoked when SingleObserver will subscribe to the {@link Single}.
     * @param <T>
     *         the type of the item emitted by the {@link Single}.
     * @return a {@link Single} whose {@link SingleObserver}s' subscriptions trigger an invocation of the given function.
     * @see #defer(Supplier)
     * @see #fromCallable(Callable)
     * @since 3.0.0
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <@NonNull T> Single<T> fromSupplier(@NonNull Supplier<? extends T> supplier) {
        Objects.requireNonNull(supplier, "supplier is null");
        return RxJavaPlugins.onAssembly(new SingleFromSupplier<T>(supplier));
    }

    /**
     * Returns a {@code Single} that emits a specified item.
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
     * @return a {@code Single} that emits {@code item}
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
     * Merges an Iterable sequence of SingleSource instances into a single Flowable sequence,
     * running all SingleSources at once.
     * <p>
     * <img width="640" height="319" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.merge.i.png" alt="">
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The returned {@code Flowable} honors the backpressure of the downstream consumer.</dd>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code merge} does not operate by default on a particular {@link Scheduler}.</dd>
     *  <dt><b>Error handling:</b></dt>
     *  <dd>If any of the source {@code SingleSource}s signal a {@code Throwable} via {@code onError}, the resulting
     *  {@code Flowable} terminates with that {@code Throwable} and all other source {@code SingleSource}s are disposed.
     *  If more than one {@code SingleSource} signals an error, the resulting {@code Flowable} may terminate with the
     *  first one's error or, depending on the concurrency of the sources, may terminate with a
     *  {@code CompositeException} containing two or more of the various error signals.
     *  {@code Throwable}s that didn't make into the composite will be sent (individually) to the global error handler via
     *  {@link RxJavaPlugins#onError(Throwable)} method as {@code UndeliverableException} errors. Similarly, {@code Throwable}s
     *  signaled by source(s) after the returned {@code Flowable} has been cancelled or terminated with a
     *  (composite) error will be sent to the same global error handler.
     *  Use {@link #mergeDelayError(Iterable)} to merge sources and terminate only when all source {@code SingleSource}s
     *  have completed or failed with an error.
     *  </dd>
     * </dl>
     * @param <T> the common and resulting value type
     * @param sources the Iterable sequence of SingleSource sources
     * @return the new Flowable instance
     * @since 2.0
     * @see #mergeDelayError(Iterable)
     */
    @CheckReturnValue
    @NonNull
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <T> Flowable<T> merge(Iterable<? extends SingleSource<? extends T>> sources) {
        return merge(Flowable.fromIterable(sources));
    }

    /**
     * Merges a Flowable sequence of SingleSource instances into a single Flowable sequence,
     * running all SingleSources at once.
     * <p>
     * <img width="640" height="307" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.merge.p.png" alt="">
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The returned {@code Flowable} honors the backpressure of the downstream consumer.</dd>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code merge} does not operate by default on a particular {@link Scheduler}.</dd>
     *  <dt><b>Error handling:</b></dt>
     *  <dd>If any of the source {@code SingleSource}s signal a {@code Throwable} via {@code onError}, the resulting
     *  {@code Flowable} terminates with that {@code Throwable} and all other source {@code SingleSource}s are disposed.
     *  If more than one {@code SingleSource} signals an error, the resulting {@code Flowable} may terminate with the
     *  first one's error or, depending on the concurrency of the sources, may terminate with a
     *  {@code CompositeException} containing two or more of the various error signals.
     *  {@code Throwable}s that didn't make into the composite will be sent (individually) to the global error handler via
     *  {@link RxJavaPlugins#onError(Throwable)} method as {@code UndeliverableException} errors. Similarly, {@code Throwable}s
     *  signaled by source(s) after the returned {@code Flowable} has been cancelled or terminated with a
     *  (composite) error will be sent to the same global error handler.
     *  Use {@link #mergeDelayError(Publisher)} to merge sources and terminate only when all source {@code SingleSource}s
     *  have completed or failed with an error.
     *  </dd>
     * </dl>
     * @param <T> the common and resulting value type
     * @param sources the Flowable sequence of SingleSource sources
     * @return the new Flowable instance
     * @see #mergeDelayError(Publisher)
     * @since 2.0
     */
    @CheckReturnValue
    @NonNull
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerSupport.NONE)
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static <T> Flowable<T> merge(@NonNull Publisher<? extends SingleSource<? extends T>> sources) {
        Objects.requireNonNull(sources, "sources is null");
        return RxJavaPlugins.onAssembly(new FlowableFlatMapPublisher(sources, SingleInternalHelper.toFlowable(), false, Integer.MAX_VALUE, Flowable.bufferSize()));
    }

    /**
     * Flattens a {@code Single} that emits a {@code Single} into a single {@code Single} that emits the item
     * emitted by the nested {@code Single}, without any transformation.
     * <p>
     * <img width="640" height="412" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.merge.oo.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code merge} does not operate by default on a particular {@link Scheduler}.</dd>
     * <dd>The resulting {@code Single} emits the outer source's or the inner {@code SingleSource}'s {@code Throwable} as is.
     * Unlike the other {@code merge()} operators, this operator won't and can't produce a {@code CompositeException} because there is
     * only one possibility for the outer or the inner {@code SingleSource} to emit an {@code onError} signal.
     * Therefore, there is no need for a {@code mergeDelayError(SingleSource<SingleSource<T>>)} operator.
     * </dd>
     * </dl>
     *
     * @param <T> the value type of the sources and the output
     * @param source
     *            a {@code Single} that emits a {@code Single}
     * @return a {@code Single} that emits the item that is the result of flattening the {@code Single} emitted
     *         by {@code source}
     * @see <a href="http://reactivex.io/documentation/operators/merge.html">ReactiveX operators documentation: Merge</a>
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static <T> Single<T> merge(@NonNull SingleSource<? extends SingleSource<? extends T>> source) {
        Objects.requireNonNull(source, "source is null");
        return RxJavaPlugins.onAssembly(new SingleFlatMap<SingleSource<? extends T>, T>(source, (Function)Functions.identity()));
    }

    /**
     * Flattens two Singles into a single Flowable, without any transformation.
     * <p>
     * <img width="640" height="414" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.merge.png" alt="">
     * <p>
     * You can combine items emitted by multiple Singles so that they appear as a single Flowable, by
     * using the {@code merge} method.
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The returned {@code Flowable} honors the backpressure of the downstream consumer.</dd>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code merge} does not operate by default on a particular {@link Scheduler}.</dd>
     *  <dt><b>Error handling:</b></dt>
     *  <dd>If any of the source {@code SingleSource}s signal a {@code Throwable} via {@code onError}, the resulting
     *  {@code Flowable} terminates with that {@code Throwable} and all other source {@code SingleSource}s are disposed.
     *  If more than one {@code SingleSource} signals an error, the resulting {@code Flowable} may terminate with the
     *  first one's error or, depending on the concurrency of the sources, may terminate with a
     *  {@code CompositeException} containing two or more of the various error signals.
     *  {@code Throwable}s that didn't make into the composite will be sent (individually) to the global error handler via
     *  {@link RxJavaPlugins#onError(Throwable)} method as {@code UndeliverableException} errors. Similarly, {@code Throwable}s
     *  signaled by source(s) after the returned {@code Flowable} has been cancelled or terminated with a
     *  (composite) error will be sent to the same global error handler.
     *  Use {@link #mergeDelayError(SingleSource, SingleSource)} to merge sources and terminate only when all source {@code SingleSource}s
     *  have completed or failed with an error.
     *  </dd>
     * </dl>
     *
     * @param <T> the common value type
     * @param source1
     *            a SingleSource to be merged
     * @param source2
     *            a SingleSource to be merged
     * @return a Flowable that emits all of the items emitted by the source Singles
     * @see <a href="http://reactivex.io/documentation/operators/merge.html">ReactiveX operators documentation: Merge</a>
     * @see #mergeDelayError(SingleSource, SingleSource)
     */
    @CheckReturnValue
    @NonNull
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <T> Flowable<T> merge(
            @NonNull SingleSource<? extends T> source1, @NonNull SingleSource<? extends T> source2
     ) {
        Objects.requireNonNull(source1, "source1 is null");
        Objects.requireNonNull(source2, "source2 is null");
        return merge(Flowable.fromArray(source1, source2));
    }

    /**
     * Flattens three Singles into a single Flowable, without any transformation.
     * <p>
     * <img width="640" height="366" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.merge.o3.png" alt="">
     * <p>
     * You can combine items emitted by multiple Singles so that they appear as a single Flowable, by using
     * the {@code merge} method.
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The returned {@code Flowable} honors the backpressure of the downstream consumer.</dd>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code merge} does not operate by default on a particular {@link Scheduler}.</dd>
     *  <dt><b>Error handling:</b></dt>
     *  <dd>If any of the source {@code SingleSource}s signal a {@code Throwable} via {@code onError}, the resulting
     *  {@code Flowable} terminates with that {@code Throwable} and all other source {@code SingleSource}s are disposed.
     *  If more than one {@code SingleSource} signals an error, the resulting {@code Flowable} may terminate with the
     *  first one's error or, depending on the concurrency of the sources, may terminate with a
     *  {@code CompositeException} containing two or more of the various error signals.
     *  {@code Throwable}s that didn't make into the composite will be sent (individually) to the global error handler via
     *  {@link RxJavaPlugins#onError(Throwable)} method as {@code UndeliverableException} errors. Similarly, {@code Throwable}s
     *  signaled by source(s) after the returned {@code Flowable} has been cancelled or terminated with a
     *  (composite) error will be sent to the same global error handler.
     *  Use {@link #mergeDelayError(SingleSource, SingleSource, SingleSource)} to merge sources and terminate only when all source {@code SingleSource}s
     *  have completed or failed with an error.
     *  </dd>
     * </dl>
     *
     * @param <T> the common value type
     * @param source1
     *            a SingleSource to be merged
     * @param source2
     *            a SingleSource to be merged
     * @param source3
     *            a SingleSource to be merged
     * @return a Flowable that emits all of the items emitted by the source Singles
     * @see <a href="http://reactivex.io/documentation/operators/merge.html">ReactiveX operators documentation: Merge</a>
     * @see #mergeDelayError(SingleSource, SingleSource, SingleSource)
     */
    @CheckReturnValue
    @NonNull
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <T> Flowable<T> merge(
            @NonNull SingleSource<? extends T> source1, @NonNull SingleSource<? extends T> source2,
            @NonNull SingleSource<? extends T> source3
     ) {
        Objects.requireNonNull(source1, "source1 is null");
        Objects.requireNonNull(source2, "source2 is null");
        Objects.requireNonNull(source3, "source3 is null");
        return merge(Flowable.fromArray(source1, source2, source3));
    }

    /**
     * Flattens four Singles into a single Flowable, without any transformation.
     * <p>
     * <img width="640" height="362" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.merge.o4.png" alt="">
     * <p>
     * You can combine items emitted by multiple Singles so that they appear as a single Flowable, by using
     * the {@code merge} method.
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The returned {@code Flowable} honors the backpressure of the downstream consumer.</dd>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code merge} does not operate by default on a particular {@link Scheduler}.</dd>
     *  <dt><b>Error handling:</b></dt>
     *  <dd>If any of the source {@code SingleSource}s signal a {@code Throwable} via {@code onError}, the resulting
     *  {@code Flowable} terminates with that {@code Throwable} and all other source {@code SingleSource}s are disposed.
     *  If more than one {@code SingleSource} signals an error, the resulting {@code Flowable} may terminate with the
     *  first one's error or, depending on the concurrency of the sources, may terminate with a
     *  {@code CompositeException} containing two or more of the various error signals.
     *  {@code Throwable}s that didn't make into the composite will be sent (individually) to the global error handler via
     *  {@link RxJavaPlugins#onError(Throwable)} method as {@code UndeliverableException} errors. Similarly, {@code Throwable}s
     *  signaled by source(s) after the returned {@code Flowable} has been cancelled or terminated with a
     *  (composite) error will be sent to the same global error handler.
     *  Use {@link #mergeDelayError(SingleSource, SingleSource, SingleSource, SingleSource)} to merge sources and terminate only when all source {@code SingleSource}s
     *  have completed or failed with an error.
     *  </dd>
     * </dl>
     *
     * @param <T> the common value type
     * @param source1
     *            a SingleSource to be merged
     * @param source2
     *            a SingleSource to be merged
     * @param source3
     *            a SingleSource to be merged
     * @param source4
     *            a SingleSource to be merged
     * @return a Flowable that emits all of the items emitted by the source Singles
     * @see <a href="http://reactivex.io/documentation/operators/merge.html">ReactiveX operators documentation: Merge</a>
     * @see #mergeDelayError(SingleSource, SingleSource, SingleSource, SingleSource)
     */
    @CheckReturnValue
    @NonNull
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <T> Flowable<T> merge(
            @NonNull SingleSource<? extends T> source1, @NonNull SingleSource<? extends T> source2,
            @NonNull SingleSource<? extends T> source3, @NonNull SingleSource<? extends T> source4
     ) {
        Objects.requireNonNull(source1, "source1 is null");
        Objects.requireNonNull(source2, "source2 is null");
        Objects.requireNonNull(source3, "source3 is null");
        Objects.requireNonNull(source4, "source4 is null");
        return merge(Flowable.fromArray(source1, source2, source3, source4));
    }

    /**
     * Merges an Iterable sequence of SingleSource instances into a single Flowable sequence,
     * running all SingleSources at once and delaying any error(s) until all sources succeed or fail.
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
     * @param sources the Iterable sequence of SingleSource sources
     * @return the new Flowable instance
     * @see #merge(Iterable)
     * @since 2.2
     */
    @CheckReturnValue
    @NonNull
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <T> Flowable<T> mergeDelayError(@NonNull Iterable<? extends SingleSource<? extends T>> sources) {
        return mergeDelayError(Flowable.fromIterable(sources));
    }

    /**
     * Merges a Flowable sequence of SingleSource instances into a single Flowable sequence,
     * running all SingleSources at once and delaying any error(s) until all sources succeed or fail.
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
     * @param sources the Flowable sequence of SingleSource sources
     * @return the new Flowable instance
     * @see #merge(Publisher)
     * @since 2.2
     */
    @CheckReturnValue
    @NonNull
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerSupport.NONE)
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static <T> Flowable<T> mergeDelayError(@NonNull Publisher<? extends SingleSource<? extends T>> sources) {
        Objects.requireNonNull(sources, "sources is null");
        return RxJavaPlugins.onAssembly(new FlowableFlatMapPublisher(sources, SingleInternalHelper.toFlowable(), true, Integer.MAX_VALUE, Flowable.bufferSize()));
    }

    /**
     * Flattens two Singles into a single Flowable, without any transformation, delaying
     * any error(s) until all sources succeed or fail.
     * <p>
     * <img width="640" height="554" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.mergeDelayError.2.png" alt="">
     * <p>
     * You can combine items emitted by multiple Singles so that they appear as a single Flowable, by
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
     *            a SingleSource to be merged
     * @param source2
     *            a SingleSource to be merged
     * @return a Flowable that emits all of the items emitted by the source Singles
     * @see <a href="http://reactivex.io/documentation/operators/merge.html">ReactiveX operators documentation: Merge</a>
     * @see #merge(SingleSource, SingleSource)
     * @since 2.2
     */
    @CheckReturnValue
    @NonNull
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <T> Flowable<T> mergeDelayError(
            @NonNull SingleSource<? extends T> source1, @NonNull SingleSource<? extends T> source2
     ) {
        Objects.requireNonNull(source1, "source1 is null");
        Objects.requireNonNull(source2, "source2 is null");
        return mergeDelayError(Flowable.fromArray(source1, source2));
    }

    /**
     * Flattens three Singles into a single Flowable, without any transformation, delaying
     * any error(s) until all sources succeed or fail.
     * <p>
     * <img width="640" height="496" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.mergeDelayError.3.png" alt="">
     * <p>
     * You can combine items emitted by multiple Singles so that they appear as a single Flowable, by using
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
     *            a SingleSource to be merged
     * @param source2
     *            a SingleSource to be merged
     * @param source3
     *            a SingleSource to be merged
     * @return a Flowable that emits all of the items emitted by the source Singles
     * @see <a href="http://reactivex.io/documentation/operators/merge.html">ReactiveX operators documentation: Merge</a>
     * @see #merge(SingleSource, SingleSource, SingleSource)
     * @since 2.2
     */
    @CheckReturnValue
    @NonNull
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <T> Flowable<T> mergeDelayError(
            @NonNull SingleSource<? extends T> source1, @NonNull SingleSource<? extends T> source2,
            @NonNull SingleSource<? extends T> source3
     ) {
        Objects.requireNonNull(source1, "source1 is null");
        Objects.requireNonNull(source2, "source2 is null");
        Objects.requireNonNull(source3, "source3 is null");
        return mergeDelayError(Flowable.fromArray(source1, source2, source3));
    }

    /**
     * Flattens four Singles into a single Flowable, without any transformation, delaying
     * any error(s) until all sources succeed or fail.
     * <p>
     * <img width="640" height="509" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.mergeDelayError.4.png" alt="">
     * <p>
     * You can combine items emitted by multiple Singles so that they appear as a single Flowable, by using
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
     *            a SingleSource to be merged
     * @param source2
     *            a SingleSource to be merged
     * @param source3
     *            a SingleSource to be merged
     * @param source4
     *            a SingleSource to be merged
     * @return a Flowable that emits all of the items emitted by the source Singles
     * @see <a href="http://reactivex.io/documentation/operators/merge.html">ReactiveX operators documentation: Merge</a>
     * @see #merge(SingleSource, SingleSource, SingleSource, SingleSource)
     * @since 2.2
     */
    @CheckReturnValue
    @NonNull
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <T> Flowable<T> mergeDelayError(
            @NonNull SingleSource<? extends T> source1, @NonNull SingleSource<? extends T> source2,
            @NonNull SingleSource<? extends T> source3, @NonNull SingleSource<? extends T> source4
     ) {
        Objects.requireNonNull(source1, "source1 is null");
        Objects.requireNonNull(source2, "source2 is null");
        Objects.requireNonNull(source3, "source3 is null");
        Objects.requireNonNull(source4, "source4 is null");
        return mergeDelayError(Flowable.fromArray(source1, source2, source3, source4));
    }

    /**
     * Returns a singleton instance of a never-signaling Single (only calls onSubscribe).
     * <p>
     * <img width="640" height="244" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.never.png" alt="">
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
    public static <T> Single<T> never() {
        return RxJavaPlugins.onAssembly((Single<T>) SingleNever.INSTANCE);
    }

    /**
     * Signals success with 0L value after the given delay for each SingleObserver.
     * <p>
     * <img width="640" height="292" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.timer.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code timer} operates by default on the {@code computation} {@link Scheduler}.</dd>
     * </dl>
     * @param delay the delay amount
     * @param unit the time unit of the delay
     * @return the new Single instance
     * @since 2.0
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.COMPUTATION)
    @NonNull
    public static Single<Long> timer(long delay, @NonNull TimeUnit unit) {
        return timer(delay, unit, Schedulers.computation());
    }

    /**
     * Signals success with 0L value after the given delay for each SingleObserver.
     * <p>
     * <img width="640" height="292" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.timer.s.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>you specify the {@link Scheduler} to signal on.</dd>
     * </dl>
     * @param delay the delay amount
     * @param unit the time unit of the delay
     * @param scheduler the scheduler where the single 0L will be emitted
     * @return the new Single instance
     * @throws NullPointerException
     *             if unit is null, or
     *             if scheduler is null
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
     * Compares two SingleSources and emits true if they emit the same value (compared via Object.equals).
     * <p>
     * <img width="640" height="465" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.equals.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code equals} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the common value type
     * @param first the first SingleSource instance
     * @param second the second SingleSource instance
     * @return the new Single instance
     * @since 2.0
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <T> Single<Boolean> equals(@NonNull SingleSource<? extends T> first, @NonNull SingleSource<? extends T> second) { // NOPMD
        Objects.requireNonNull(first, "first is null");
        Objects.requireNonNull(second, "second is null");
        return RxJavaPlugins.onAssembly(new SingleEquals<>(first, second));
    }

    /**
     * <strong>Advanced use only:</strong> creates a Single instance without
     * any safeguards by using a callback that is called with a SingleObserver.
     * <p>
     * <img width="640" height="261" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.unsafeCreate.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code unsafeCreate} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the value type
     * @param onSubscribe the function that is called with the subscribing SingleObserver
     * @return the new Single instance
     * @throws IllegalArgumentException if {@code source} is a subclass of {@code Single}; such
     * instances don't need conversion and is possibly a port remnant from 1.x or one should use {@link #hide()}
     * instead.
     * @since 2.0
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <T> Single<T> unsafeCreate(@NonNull SingleSource<T> onSubscribe) {
        Objects.requireNonNull(onSubscribe, "onSubscribe is null");
        if (onSubscribe instanceof Single) {
            throw new IllegalArgumentException("unsafeCreate(Single) should be upgraded");
        }
        return RxJavaPlugins.onAssembly(new SingleFromUnsafeSource<>(onSubscribe));
    }

    /**
     * Allows using and disposing a resource while running a SingleSource instance generated from
     * that resource (similar to a try-with-resources).
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.using.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code using} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the value type of the SingleSource generated
     * @param <U> the resource type
     * @param resourceSupplier the Supplier called for each SingleObserver to generate a resource Object
     * @param singleFunction the function called with the returned resource
     *                  Object from {@code resourceSupplier} and should return a SingleSource instance
     *                  to be run by the operator
     * @param disposer the consumer of the generated resource that is called exactly once for
     *                  that particular resource when the generated SingleSource terminates
     *                  (successfully or with an error) or gets disposed.
     * @return the new Single instance
     * @since 2.0
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public static <T, U> Single<T> using(@NonNull Supplier<U> resourceSupplier,
            @NonNull Function<? super U, ? extends SingleSource<? extends T>> singleFunction,
            @NonNull Consumer<? super U> disposer) {
        return using(resourceSupplier, singleFunction, disposer, true);
    }

    /**
     * Allows using and disposing a resource while running a SingleSource instance generated from
     * that resource (similar to a try-with-resources).
     * <p>
     * <img width="640" height="325" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.using.b.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code using} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the value type of the SingleSource generated
     * @param <U> the resource type
     * @param resourceSupplier the Supplier called for each SingleObserver to generate a resource Object
     * @param singleFunction the function called with the returned resource
     *                  Object from {@code resourceSupplier} and should return a SingleSource instance
     *                  to be run by the operator
     * @param disposer the consumer of the generated resource that is called exactly once for
     *                  that particular resource when the generated SingleSource terminates
     *                  (successfully or with an error) or gets disposed.
     * @param eager
     *            If {@code true} then resource disposal will happen either on a {@code dispose()} call before the upstream is disposed
     *            or just before the emission of a terminal event ({@code onSuccess} or {@code onError}).
     *            If {@code false} the resource disposal will happen either on a {@code dispose()} call after the upstream is disposed
     *            or just after the emission of a terminal event ({@code onSuccess} or {@code onError}).
     * @return the new Single instance
     * @since 2.0
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <T, U> Single<T> using(
            @NonNull Supplier<U> resourceSupplier,
            @NonNull Function<? super U, ? extends SingleSource<? extends T>> singleFunction,
            @NonNull Consumer<? super U> disposer,
            boolean eager) {
        Objects.requireNonNull(resourceSupplier, "resourceSupplier is null");
        Objects.requireNonNull(singleFunction, "singleFunction is null");
        Objects.requireNonNull(disposer, "disposer is null");

        return RxJavaPlugins.onAssembly(new SingleUsing<>(resourceSupplier, singleFunction, disposer, eager));
    }

    /**
     * Wraps a SingleSource instance into a new Single instance if not already a Single
     * instance.
     * <p>
     * <img width="640" height="350" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.wrap.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code wrap} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the value type
     * @param source the source to wrap
     * @return the Single wrapper or the source cast to Single (if possible)
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <T> Single<T> wrap(@NonNull SingleSource<T> source) {
        Objects.requireNonNull(source, "source is null");
        if (source instanceof Single) {
            return RxJavaPlugins.onAssembly((Single<T>)source);
        }
        return RxJavaPlugins.onAssembly(new SingleFromUnsafeSource<>(source));
    }

    /**
     * Waits until all SingleSource sources provided by the Iterable sequence signal a success
     * value and calls a zipper function with an array of these values to return a result
     * to be emitted to downstream.
     * <p>
     * If the {@code Iterable} of {@link SingleSource}s is empty a {@link NoSuchElementException} error is signalled after subscription.
     * <p>
     * Note on method signature: since Java doesn't allow creating a generic array with {@code new T[]}, the
     * implementation of this operator has to create an {@code Object[]} instead. Unfortunately, a
     * {@code Function<Integer[], R>} passed to the method would trigger a {@code ClassCastException}.
     *
     * <p>
     * <img width="640" height="340" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.zip.i.png" alt="">
     * <p>
     * If any of the SingleSources signal an error, all other SingleSources get disposed and the
     * error emitted to downstream immediately.
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code zip} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the common value type
     * @param <R> the result value type
     * @param sources the Iterable sequence of SingleSource instances. An empty sequence will result in an
     *                {@code onError} signal of {@link NoSuchElementException}.
     * @param zipper the function that receives an array with values from each SingleSource
     *               and should return a value to be emitted to downstream
     * @return the new Single instance
     * @since 2.0
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <T, R> Single<R> zip(@NonNull Iterable<? extends SingleSource<? extends T>> sources, @NonNull Function<? super Object[], ? extends R> zipper) {
        Objects.requireNonNull(zipper, "zipper is null");
        Objects.requireNonNull(sources, "sources is null");
        return RxJavaPlugins.onAssembly(new SingleZipIterable<T, R>(sources, zipper));
    }

    /**
     * Returns a Single that emits the results of a specified combiner function applied to two items emitted by
     * two other Singles.
     * <p>
     * <img width="640" height="414" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.zip.n.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code zip} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T1> the first source Single's value type
     * @param <T2> the second source Single's value type
     * @param <R> the result value type
     * @param source1
     *            the first source Single
     * @param source2
     *            a second source Single
     * @param zipper
     *            a function that, when applied to the item emitted by each of the source Singles, results in an
     *            item that will be emitted by the resulting Single
     * @return a Single that emits the zipped results
     * @see <a href="http://reactivex.io/documentation/operators/zip.html">ReactiveX operators documentation: Zip</a>
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <T1, T2, R> Single<R> zip(
            @NonNull SingleSource<? extends T1> source1, @NonNull SingleSource<? extends T2> source2,
            @NonNull BiFunction<? super T1, ? super T2, ? extends R> zipper
     ) {
        Objects.requireNonNull(source1, "source1 is null");
        Objects.requireNonNull(source2, "source2 is null");
        return zipArray(Functions.toFunction(zipper), source1, source2);
    }

    /**
     * Returns a Single that emits the results of a specified combiner function applied to three items emitted
     * by three other Singles.
     * <p>
     * <img width="640" height="414" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.zip.n.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code zip} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T1> the first source Single's value type
     * @param <T2> the second source Single's value type
     * @param <T3> the third source Single's value type
     * @param <R> the result value type
     * @param source1
     *            the first source Single
     * @param source2
     *            a second source Single
     * @param source3
     *            a third source Single
     * @param zipper
     *            a function that, when applied to the item emitted by each of the source Singles, results in an
     *            item that will be emitted by the resulting Single
     * @return a Single that emits the zipped results
     * @see <a href="http://reactivex.io/documentation/operators/zip.html">ReactiveX operators documentation: Zip</a>
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <T1, T2, T3, R> Single<R> zip(
            @NonNull SingleSource<? extends T1> source1, @NonNull SingleSource<? extends T2> source2,
            @NonNull SingleSource<? extends T3> source3,
            @NonNull Function3<? super T1, ? super T2, ? super T3, ? extends R> zipper
     ) {
        Objects.requireNonNull(source1, "source1 is null");
        Objects.requireNonNull(source2, "source2 is null");
        Objects.requireNonNull(source3, "source3 is null");
        return zipArray(Functions.toFunction(zipper), source1, source2, source3);
    }

    /**
     * Returns a Single that emits the results of a specified combiner function applied to four items
     * emitted by four other Singles.
     * <p>
     * <img width="640" height="414" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.zip.n.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code zip} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T1> the first source Single's value type
     * @param <T2> the second source Single's value type
     * @param <T3> the third source Single's value type
     * @param <T4> the fourth source Single's value type
     * @param <R> the result value type
     * @param source1
     *            the first source Single
     * @param source2
     *            a second source Single
     * @param source3
     *            a third source Single
     * @param source4
     *            a fourth source Single
     * @param zipper
     *            a function that, when applied to the item emitted by each of the source Singles, results in an
     *            item that will be emitted by the resulting Single
     * @return a Single that emits the zipped results
     * @see <a href="http://reactivex.io/documentation/operators/zip.html">ReactiveX operators documentation: Zip</a>
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <T1, T2, T3, T4, R> Single<R> zip(
            @NonNull SingleSource<? extends T1> source1, @NonNull SingleSource<? extends T2> source2,
            @NonNull SingleSource<? extends T3> source3, @NonNull SingleSource<? extends T4> source4,
            @NonNull Function4<? super T1, ? super T2, ? super T3, ? super T4, ? extends R> zipper
     ) {
        Objects.requireNonNull(source1, "source1 is null");
        Objects.requireNonNull(source2, "source2 is null");
        Objects.requireNonNull(source3, "source3 is null");
        Objects.requireNonNull(source4, "source4 is null");
        return zipArray(Functions.toFunction(zipper), source1, source2, source3, source4);
    }

    /**
     * Returns a Single that emits the results of a specified combiner function applied to five items
     * emitted by five other Singles.
     * <p>
     * <img width="640" height="414" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.zip.n.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code zip} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T1> the first source Single's value type
     * @param <T2> the second source Single's value type
     * @param <T3> the third source Single's value type
     * @param <T4> the fourth source Single's value type
     * @param <T5> the fifth source Single's value type
     * @param <R> the result value type
     * @param source1
     *            the first source Single
     * @param source2
     *            a second source Single
     * @param source3
     *            a third source Single
     * @param source4
     *            a fourth source Single
     * @param source5
     *            a fifth source Single
     * @param zipper
     *            a function that, when applied to the item emitted by each of the source Singles, results in an
     *            item that will be emitted by the resulting Single
     * @return a Single that emits the zipped results
     * @see <a href="http://reactivex.io/documentation/operators/zip.html">ReactiveX operators documentation: Zip</a>
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <T1, T2, T3, T4, T5, R> Single<R> zip(
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
        return zipArray(Functions.toFunction(zipper), source1, source2, source3, source4, source5);
    }

    /**
     * Returns a Single that emits the results of a specified combiner function applied to six items
     * emitted by six other Singles.
     * <p>
     * <img width="640" height="414" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.zip.n.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code zip} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T1> the first source Single's value type
     * @param <T2> the second source Single's value type
     * @param <T3> the third source Single's value type
     * @param <T4> the fourth source Single's value type
     * @param <T5> the fifth source Single's value type
     * @param <T6> the sixth source Single's value type
     * @param <R> the result value type
     * @param source1
     *            the first source Single
     * @param source2
     *            a second source Single
     * @param source3
     *            a third source Single
     * @param source4
     *            a fourth source Single
     * @param source5
     *            a fifth source Single
     * @param source6
     *            a sixth source Single
     * @param zipper
     *            a function that, when applied to the item emitted by each of the source Singles, results in an
     *            item that will be emitted by the resulting Single
     * @return a Single that emits the zipped results
     * @see <a href="http://reactivex.io/documentation/operators/zip.html">ReactiveX operators documentation: Zip</a>
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <T1, T2, T3, T4, T5, T6, R> Single<R> zip(
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
        return zipArray(Functions.toFunction(zipper), source1, source2, source3, source4, source5, source6);
    }

    /**
     * Returns a Single that emits the results of a specified combiner function applied to seven items
     * emitted by seven other Singles.
     * <p>
     * <img width="640" height="414" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.zip.n.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code zip} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T1> the first source Single's value type
     * @param <T2> the second source Single's value type
     * @param <T3> the third source Single's value type
     * @param <T4> the fourth source Single's value type
     * @param <T5> the fifth source Single's value type
     * @param <T6> the sixth source Single's value type
     * @param <T7> the seventh source Single's value type
     * @param <R> the result value type
     * @param source1
     *            the first source Single
     * @param source2
     *            a second source Single
     * @param source3
     *            a third source Single
     * @param source4
     *            a fourth source Single
     * @param source5
     *            a fifth source Single
     * @param source6
     *            a sixth source Single
     * @param source7
     *            a seventh source Single
     * @param zipper
     *            a function that, when applied to the item emitted by each of the source Singles, results in an
     *            item that will be emitted by the resulting Single
     * @return a Single that emits the zipped results
     * @see <a href="http://reactivex.io/documentation/operators/zip.html">ReactiveX operators documentation: Zip</a>
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <T1, T2, T3, T4, T5, T6, T7, R> Single<R> zip(
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
        return zipArray(Functions.toFunction(zipper), source1, source2, source3, source4, source5, source6, source7);
    }

    /**
     * Returns a Single that emits the results of a specified combiner function applied to eight items
     * emitted by eight other Singles.
     * <p>
     * <img width="640" height="414" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.zip.n.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code zip} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T1> the first source Single's value type
     * @param <T2> the second source Single's value type
     * @param <T3> the third source Single's value type
     * @param <T4> the fourth source Single's value type
     * @param <T5> the fifth source Single's value type
     * @param <T6> the sixth source Single's value type
     * @param <T7> the seventh source Single's value type
     * @param <T8> the eighth source Single's value type
     * @param <R> the result value type
     * @param source1
     *            the first source Single
     * @param source2
     *            a second source Single
     * @param source3
     *            a third source Single
     * @param source4
     *            a fourth source Single
     * @param source5
     *            a fifth source Single
     * @param source6
     *            a sixth source Single
     * @param source7
     *            a seventh source Single
     * @param source8
     *            an eighth source Single
     * @param zipper
     *            a function that, when applied to the item emitted by each of the source Singles, results in an
     *            item that will be emitted by the resulting Single
     * @return a Single that emits the zipped results
     * @see <a href="http://reactivex.io/documentation/operators/zip.html">ReactiveX operators documentation: Zip</a>
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <T1, T2, T3, T4, T5, T6, T7, T8, R> Single<R> zip(
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
        return zipArray(Functions.toFunction(zipper), source1, source2, source3, source4, source5, source6, source7, source8);
    }

    /**
     * Returns a Single that emits the results of a specified combiner function applied to nine items
     * emitted by nine other Singles.
     * <p>
     * <img width="640" height="414" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.zip.n.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code zip} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T1> the first source Single's value type
     * @param <T2> the second source Single's value type
     * @param <T3> the third source Single's value type
     * @param <T4> the fourth source Single's value type
     * @param <T5> the fifth source Single's value type
     * @param <T6> the sixth source Single's value type
     * @param <T7> the seventh source Single's value type
     * @param <T8> the eighth source Single's value type
     * @param <T9> the ninth source Single's value type
     * @param <R> the result value type
     * @param source1
     *            the first source Single
     * @param source2
     *            a second source Single
     * @param source3
     *            a third source Single
     * @param source4
     *            a fourth source Single
     * @param source5
     *            a fifth source Single
     * @param source6
     *            a sixth source Single
     * @param source7
     *            a seventh source Single
     * @param source8
     *            an eighth source Single
     * @param source9
     *            a ninth source Single
     * @param zipper
     *            a function that, when applied to the item emitted by each of the source Singles, results in an
     *            item that will be emitted by the resulting Single
     * @return a Single that emits the zipped results
     * @see <a href="http://reactivex.io/documentation/operators/zip.html">ReactiveX operators documentation: Zip</a>
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <T1, T2, T3, T4, T5, T6, T7, T8, T9, R> Single<R> zip(
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
        return zipArray(Functions.toFunction(zipper), source1, source2, source3, source4, source5, source6, source7, source8, source9);
    }

    /**
     * Waits until all SingleSource sources provided via an array signal a success
     * value and calls a zipper function with an array of these values to return a result
     * to be emitted to downstream.
     * <p>
     * <img width="640" height="340" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.zipArray.png" alt="">
     * <p>
     * If the array of {@link SingleSource}s is empty a {@link NoSuchElementException} error is signalled immediately.
     * <p>
     * Note on method signature: since Java doesn't allow creating a generic array with {@code new T[]}, the
     * implementation of this operator has to create an {@code Object[]} instead. Unfortunately, a
     * {@code Function<Integer[], R>} passed to the method would trigger a {@code ClassCastException}.
     * <p>
     * If any of the SingleSources signal an error, all other SingleSources get disposed and the
     * error emitted to downstream immediately.
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code zipArray} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the common value type
     * @param <R> the result value type
     * @param sources the array of SingleSource instances. An empty sequence will result in an
     *                {@code onError} signal of {@link NoSuchElementException}.
     * @param zipper the function that receives an array with values from each SingleSource
     *               and should return a value to be emitted to downstream
     * @return the new Single instance
     * @since 2.0
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    @SafeVarargs
    public static <T, R> Single<R> zipArray(@NonNull Function<? super Object[], ? extends R> zipper, @NonNull SingleSource<? extends T>... sources) {
        Objects.requireNonNull(zipper, "zipper is null");
        Objects.requireNonNull(sources, "sources is null");
        if (sources.length == 0) {
            return error(new NoSuchElementException());
        }
        return RxJavaPlugins.onAssembly(new SingleZipArray<T, R>(sources, zipper));
    }

    /**
     * Signals the event of this or the other SingleSource whichever signals first.
     * <p>
     * <img width="640" height="463" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.ambWith.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code ambWith} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param other the other SingleSource to race for the first emission of success or error
     * @return the new Single instance. A subscription to this provided source will occur after subscribing
     *            to the current source.
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
     * Hides the identity of the current Single, including the Disposable that is sent
     * to the downstream via {@code onSubscribe()}.
     * <p>
     * <img width="640" height="458" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.hide.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code hide} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @return the new Single instance
     * @since 2.0
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Single<T> hide() {
        return RxJavaPlugins.onAssembly(new SingleHide<>(this));
    }

    /**
     * Transform a Single by applying a particular Transformer function to it.
     * <p>
     * <img width="640" height="612" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.compose.png" alt="">
     * <p>
     * This method operates on the Single itself whereas {@link #lift} operates on the Single's SingleObservers.
     * <p>
     * If the operator you are creating is designed to act on the individual item emitted by a Single, use
     * {@link #lift}. If your operator is designed to transform the source Single as a whole (for instance, by
     * applying a particular set of existing RxJava operators to it) use {@code compose}.
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code compose} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <R> the value type of the single returned by the transformer function
     * @param transformer the transformer function, not null
     * @return the source Single, transformed by the transformer function
     * @see <a href="https://github.com/ReactiveX/RxJava/wiki/Implementing-Your-Own-Operators">RxJava wiki: Implementing Your Own Operators</a>
     */
    @SuppressWarnings("unchecked")
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <R> Single<R> compose(@NonNull SingleTransformer<? super T, ? extends R> transformer) {
        return wrap(((SingleTransformer<T, R>) Objects.requireNonNull(transformer, "transformer is null")).apply(this));
    }

    /**
     * Stores the success value or exception from the current Single and replays it to late SingleObservers.
     * <p>
     * <img width="640" height="363" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.cache.png" alt="">
     * <p>
     * The returned Single subscribes to the current Single when the first SingleObserver subscribes.
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code cache} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @return the new Single instance
     * @since 2.0
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Single<T> cache() {
        return RxJavaPlugins.onAssembly(new SingleCache<>(this));
    }

    /**
     * Casts the success value of the current Single into the target type or signals a
     * ClassCastException if not compatible.
     * <p>
     * <img width="640" height="393" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.cast.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code cast} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <U> the target type
     * @param clazz the type token to use for casting the success result from the current Single
     * @return the new Single instance
     * @since 2.0
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public final <U> Single<U> cast(@NonNull Class<? extends U> clazz) {
        Objects.requireNonNull(clazz, "clazz is null");
        return map(Functions.castFunction(clazz));
    }

    /**
     * Returns a Flowable that emits the item emitted by the source Single, then the item emitted by the
     * specified Single.
     * <p>
     * <img width="640" height="335" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.concatWith.png" alt="">
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The returned {@code Flowable} honors the backpressure of the downstream consumer.</dd>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code concatWith} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param other
     *            a Single to be concatenated after the current
     * @return a Flowable that emits the item emitted by the source Single, followed by the item emitted by
     *         {@code t1}
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
     * Delays the emission of the success signal from the current Single by the specified amount.
     * An error signal will not be delayed.
     * <p>
     * <img width="640" height="457" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.delay.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code delay} operates by default on the {@code computation} {@link Scheduler}.</dd>
     * </dl>
     *
     * @param time the amount of time the success signal should be delayed for
     * @param unit the time unit
     * @return the new Single instance
     * @since 2.0
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.COMPUTATION)
    @NonNull
    public final Single<T> delay(long time, @NonNull TimeUnit unit) {
        return delay(time, unit, Schedulers.computation(), false);
    }

    /**
     * Delays the emission of the success or error signal from the current Single by the specified amount.
     * <p>
     * <img width="640" height="457" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.delay.e.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code delay} operates by default on the {@code computation} {@link Scheduler}.</dd>
     * </dl>
     * <p>History: 2.1.5 - experimental
     * @param time the amount of time the success or error signal should be delayed for
     * @param unit the time unit
     * @param delayError if true, both success and error signals are delayed. if false, only success signals are delayed.
     * @return the new Single instance
     * @since 2.2
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.COMPUTATION)
    @NonNull
    public final Single<T> delay(long time, @NonNull TimeUnit unit, boolean delayError) {
        return delay(time, unit, Schedulers.computation(), delayError);
    }

    /**
     * Delays the emission of the success signal from the current Single by the specified amount.
     * An error signal will not be delayed.
     * <p>
     * <img width="640" height="457" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.delay.s.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>you specify the {@link Scheduler} where the non-blocking wait and emission happens</dd>
     * </dl>
     *
     * @param time the amount of time the success signal should be delayed for
     * @param unit the time unit
     * @param scheduler the target scheduler to use for the non-blocking wait and emission
     * @return the new Single instance
     * @throws NullPointerException
     *             if unit is null, or
     *             if scheduler is null
     * @since 2.0
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    @NonNull
    public final Single<T> delay(long time, @NonNull TimeUnit unit, @NonNull Scheduler scheduler) {
        return delay(time, unit, scheduler, false);
    }

    /**
     * Delays the emission of the success or error signal from the current Single by the specified amount.
     * <p>
     * <img width="640" height="457" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.delay.se.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>you specify the {@link Scheduler} where the non-blocking wait and emission happens</dd>
     * </dl>
     * <p>History: 2.1.5 - experimental
     * @param time the amount of time the success or error signal should be delayed for
     * @param unit the time unit
     * @param scheduler the target scheduler to use for the non-blocking wait and emission
     * @param delayError if true, both success and error signals are delayed. if false, only success signals are delayed.
     * @return the new Single instance
     * @throws NullPointerException
     *             if unit is null, or
     *             if scheduler is null
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
     * Delays the actual subscription to the current Single until the given other CompletableSource
     * completes.
     * <p>
     * <img width="640" height="309" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.delaySubscription.c.png" alt="">
     * <p>If the delaying source signals an error, that error is re-emitted and no subscription
     * to the current Single happens.
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code delaySubscription} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param other the CompletableSource that has to complete before the subscription to the
     *              current Single happens
     * @return the new Single instance
     * @since 2.0
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Single<T> delaySubscription(@NonNull CompletableSource other) {
        Objects.requireNonNull(other, "other is null");
        return RxJavaPlugins.onAssembly(new SingleDelayWithCompletable<>(this, other));
    }

    /**
     * Delays the actual subscription to the current Single until the given other SingleSource
     * signals success.
     * <p>
     * <img width="640" height="309" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.delaySubscription.s.png" alt="">
     * <p>If the delaying source signals an error, that error is re-emitted and no subscription
     * to the current Single happens.
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code delaySubscription} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <U> the element type of the other source
     * @param other the SingleSource that has to complete before the subscription to the
     *              current Single happens
     * @return the new Single instance
     * @since 2.0
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public final <U> Single<T> delaySubscription(@NonNull SingleSource<U> other) {
        Objects.requireNonNull(other, "other is null");
        return RxJavaPlugins.onAssembly(new SingleDelayWithSingle<>(this, other));
    }

    /**
     * Delays the actual subscription to the current Single until the given other ObservableSource
     * signals its first value or completes.
     * <p>
     * <img width="640" height="214" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.delaySubscription.o.png" alt="">
     * <p>If the delaying source signals an error, that error is re-emitted and no subscription
     * to the current Single happens.
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code delaySubscription} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <U> the element type of the other source
     * @param other the ObservableSource that has to signal a value or complete before the
     *              subscription to the current Single happens
     * @return the new Single instance
     * @since 2.0
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public final <U> Single<T> delaySubscription(@NonNull ObservableSource<U> other) {
        Objects.requireNonNull(other, "other is null");
        return RxJavaPlugins.onAssembly(new SingleDelayWithObservable<>(this, other));
    }

    /**
     * Delays the actual subscription to the current Single until the given other Publisher
     * signals its first value or completes.
     * <p>
     * <img width="640" height="214" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.delaySubscription.p.png" alt="">
     * <p>If the delaying source signals an error, that error is re-emitted and no subscription
     * to the current Single happens.
     * <p>The other source is consumed in an unbounded manner (requesting {@link Long#MAX_VALUE} from it).
     * <dl>
     * <dt><b>Backpressure:</b></dt>
     * <dd>The {@code other} publisher is consumed in an unbounded fashion but will be
     * cancelled after the first item it produced.</dd>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code delaySubscription} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <U> the element type of the other source
     * @param other the Publisher that has to signal a value or complete before the
     *              subscription to the current Single happens
     * @return the new Single instance
     * @since 2.0
     */
    @BackpressureSupport(BackpressureKind.FULL)
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public final <U> Single<T> delaySubscription(@NonNull Publisher<U> other) {
        Objects.requireNonNull(other, "other is null");
        return RxJavaPlugins.onAssembly(new SingleDelayWithPublisher<>(this, other));
    }

    /**
     * Delays the actual subscription to the current Single until the given time delay elapsed.
     * <p>
     * <img width="640" height="472" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.delaySubscription.t.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code delaySubscription} does by default subscribe to the current Single
     * on the {@code computation} {@link Scheduler} after the delay.</dd>
     * </dl>
     * @param time the time amount to wait with the subscription
     * @param unit the time unit of the waiting
     * @return the new Single instance
     * @since 2.0
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.COMPUTATION)
    @NonNull
    public final Single<T> delaySubscription(long time, @NonNull TimeUnit unit) {
        return delaySubscription(time, unit, Schedulers.computation());
    }

    /**
     * Delays the actual subscription to the current Single until the given time delay elapsed.
     * <p>
     * <img width="640" height="420" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.delaySubscription.ts.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code delaySubscription} does by default subscribe to the current Single
     * on the {@link Scheduler} you provided, after the delay.</dd>
     * </dl>
     * @param time the time amount to wait with the subscription
     * @param unit the time unit of the waiting
     * @param scheduler the scheduler to wait on and subscribe on to the current Single
     * @return the new Single instance
     * @since 2.0
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    @NonNull
    public final Single<T> delaySubscription(long time, @NonNull TimeUnit unit, @NonNull Scheduler scheduler) {
        return delaySubscription(Observable.timer(time, unit, scheduler));
    }

    /**
     * Maps the {@link Notification} success value of this Single back into normal
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
     * return a {@link Notification} instance.
     * @return the new Maybe instance
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
     * <img width="640" height="460" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.doAfterSuccess.png" alt="">
     * <p>
     * Note that the {@code doAfterSuccess} action is shared between subscriptions and as such
     * should be thread-safe.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code doAfterSuccess} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * <p>History: 2.0.1 - experimental
     * @param onAfterSuccess the Consumer that will be called after emitting an item from upstream to the downstream
     * @return the new Single instance
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
     * Registers an {@link Action} to be called after this Single invokes either onSuccess or onError.
     * <p>
     * <img width="640" height="460" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.doAfterTerminate.png" alt="">
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
     *            an {@link Action} to be invoked when the source Single finishes
     * @return a Single that emits the same items as the source Single, then invokes the
     *         {@link Action}
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
     * Calls the specified action after this Single signals onSuccess or onError or gets disposed by
     * the downstream.
     * <p>In case of a race between a terminal event and a dispose call, the provided {@code onFinally} action
     * is executed once per subscription.
     * <p>Note that the {@code onFinally} action is shared between subscriptions and as such
     * should be thread-safe.
     * <p>
     * <img width="640" height="291" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.doFinally.png" alt="">
     * </p>
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code doFinally} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * <p>History: 2.0.1 - experimental
     * @param onFinally the action called when this Single terminates or gets disposed
     * @return the new Single instance
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
     * Calls the shared consumer with the Disposable sent through the onSubscribe for each
     * SingleObserver that subscribes to the current Single.
     * <p>
     * <img width="640" height="347" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.doOnSubscribe.png" alt="">
     * </p>
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code doOnSubscribe} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param onSubscribe the consumer called with the Disposable sent via onSubscribe
     * @return the new Single instance
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
     * Returns a Single instance that calls the given onTerminate callback
     * just before this Single completes normally or with an exception.
     * <p>
     * <img width="640" height="305" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/doOnTerminate.png" alt="">
     * <p>
     * This differs from {@code doAfterTerminate} in that this happens <em>before</em> the {@code onSuccess} or
     * {@code onError} notification.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code doOnTerminate} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * <p>History: 2.2.7 - experimental
     * @param onTerminate the action to invoke when the consumer calls {@code onSuccess} or {@code onError}
     * @return the new Single instance
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
     * Calls the shared consumer with the success value sent via onSuccess for each
     * SingleObserver that subscribes to the current Single.
     * <p>
     * <img width="640" height="347" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.doOnSuccess.2.png" alt="">
     * </p>
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code doOnSuccess} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param onSuccess the consumer called with the success value of onSuccess
     * @return the new Single instance
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
     * Calls the shared consumer with the error sent via onError or the value
     * via onSuccess for each SingleObserver that subscribes to the current Single.
     * <p>
     * <img width="640" height="264" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.doOnEvent.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code doOnEvent} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param onEvent the consumer called with the success value of onEvent
     * @return the new Single instance
     * @since 2.0
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Single<T> doOnEvent(@NonNull BiConsumer<? super T, ? super Throwable> onEvent) {
        Objects.requireNonNull(onEvent, "onEvent is null");
        return RxJavaPlugins.onAssembly(new SingleDoOnEvent<>(this, onEvent));
    }

    /**
     * Calls the shared consumer with the error sent via onError for each
     * SingleObserver that subscribes to the current Single.
     * <p>
     * <img width="640" height="349" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.doOnError.2.png" alt="">
     * </p>
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code doOnError} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param onError the consumer called with the success value of onError
     * @return the new Single instance
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
     * Calls the shared {@code Action} if a SingleObserver subscribed to the current Single
     * disposes the common Disposable it received via onSubscribe.
     * <p>
     * <img width="640" height="332" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.doOnDispose.png" alt="">
     * </p>
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code doOnDispose} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param onDispose the action called when the subscription is disposed
     * @return the new Single instance
     * @throws NullPointerException if onDispose is null
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
     * Filters the success item of the Single via a predicate function and emitting it if the predicate
     * returns true, completing otherwise.
     * <p>
     * <img width="640" height="457" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.filter.png" alt="">
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
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Maybe<T> filter(@NonNull Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        return RxJavaPlugins.onAssembly(new MaybeFilterSingle<>(this, predicate));
    }

    /**
     * Returns a Single that is based on applying a specified function to the item emitted by the source Single,
     * where that function returns a SingleSource.
     * <p>
     * <img width="640" height="300" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.flatMap.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code flatMap} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <R> the result value type
     * @param mapper
     *            a function that, when applied to the item emitted by the source Single, returns a SingleSource
     * @return the Single returned from {@code mapper} when applied to the item emitted by the source Single
     * @see <a href="http://reactivex.io/documentation/operators/flatmap.html">ReactiveX operators documentation: FlatMap</a>
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public final <R> Single<R> flatMap(@NonNull Function<? super T, ? extends SingleSource<? extends R>> mapper) {
        Objects.requireNonNull(mapper, "mapper is null");
        return RxJavaPlugins.onAssembly(new SingleFlatMap<>(this, mapper));
    }

    /**
     * Returns a Maybe that is based on applying a specified function to the item emitted by the source Single,
     * where that function returns a MaybeSource.
     * <p>
     * <img width="640" height="191" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.flatMapMaybe.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code flatMapMaybe} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <R> the result value type
     * @param mapper
     *            a function that, when applied to the item emitted by the source Single, returns a MaybeSource
     * @return the Maybe returned from {@code mapper} when applied to the item emitted by the source Single
     * @see <a href="http://reactivex.io/documentation/operators/flatmap.html">ReactiveX operators documentation: FlatMap</a>
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public final <R> Maybe<R> flatMapMaybe(@NonNull Function<? super T, ? extends MaybeSource<? extends R>> mapper) {
        Objects.requireNonNull(mapper, "mapper is null");
        return RxJavaPlugins.onAssembly(new SingleFlatMapMaybe<>(this, mapper));
    }

    /**
     * Returns a Flowable that emits items based on applying a specified function to the item emitted by the
     * source Single, where that function returns a Publisher.
     * <p>
     * <img width="640" height="305" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.flatMapPublisher.png" alt="">
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
     *            a function that, when applied to the item emitted by the source Single, returns a
     *            Flowable
     * @return the Flowable returned from {@code func} when applied to the item emitted by the source Single
     * @see <a href="http://reactivex.io/documentation/operators/flatmap.html">ReactiveX operators documentation: FlatMap</a>
     */
    @BackpressureSupport(BackpressureKind.FULL)
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public final <R> Flowable<R> flatMapPublisher(@NonNull Function<? super T, ? extends Publisher<? extends R>> mapper) {
        Objects.requireNonNull(mapper, "mapper is null");
        return RxJavaPlugins.onAssembly(new SingleFlatMapPublisher<>(this, mapper));
    }

    /**
     * Maps the success value of the upstream {@link Single} into an {@link Iterable} and emits its items as a
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
     *            the type of item emitted by the resulting Iterable
     * @param mapper
     *            a function that returns an Iterable sequence of values for when given an item emitted by the
     *            source Single
     * @return the new Flowable instance
     * @see <a href="http://reactivex.io/documentation/operators/flatmap.html">ReactiveX operators documentation: FlatMap</a>
     */
    @BackpressureSupport(BackpressureKind.FULL)
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public final <U> Flowable<U> flattenAsFlowable(@NonNull Function<? super T, ? extends Iterable<? extends U>> mapper) {
        Objects.requireNonNull(mapper, "mapper is null");
        return RxJavaPlugins.onAssembly(new SingleFlatMapIterableFlowable<>(this, mapper));
    }

    /**
     * Maps the success value of the upstream {@link Single} into an {@link Iterable} and emits its items as an
     * {@link Observable} sequence.
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
     *            source Single
     * @return the new Observable instance
     * @see <a href="http://reactivex.io/documentation/operators/flatmap.html">ReactiveX operators documentation: FlatMap</a>
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public final <@NonNull U> Observable<U> flattenAsObservable(@NonNull Function<@NonNull ? super T, @NonNull ? extends Iterable<? extends U>> mapper) {
        Objects.requireNonNull(mapper, "mapper is null");
        return RxJavaPlugins.onAssembly(new SingleFlatMapIterableObservable<>(this, mapper));
    }

    /**
     * Returns an Observable that is based on applying a specified function to the item emitted by the source Single,
     * where that function returns an ObservableSource.
     * <p>
     * <img width="640" height="300" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.flatMapObservable.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code flatMapObservable} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <R> the result value type
     * @param mapper
     *            a function that, when applied to the item emitted by the source Single, returns an ObservableSource
     * @return the Observable returned from {@code func} when applied to the item emitted by the source Single
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
     * source {@link Single}, where that function returns a {@link Completable}.
     * <p>
     * <img width="640" height="267" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.flatMapCompletable.png" alt="">
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
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Completable flatMapCompletable(@NonNull Function<? super T, ? extends CompletableSource> mapper) {
        Objects.requireNonNull(mapper, "mapper is null");
        return RxJavaPlugins.onAssembly(new SingleFlatMapCompletable<>(this, mapper));
    }

    /**
     * Waits in a blocking fashion until the current Single signals a success value (which is returned) or
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
     * requires a non-null {@code SingleObserver} instance to be returned, which is then unconditionally subscribed to
     * the upstream {@code Single}. For example, if the operator decided there is no reason to subscribe to the
     * upstream source because of some optimization possibility or a failure to prepare the operator, it still has to
     * return a {@code SingleObserver} that should immediately dispose the upstream's {@code Disposable} in its
     * {@code onSubscribe} method. Again, using a {@code SingleTransformer} and extending the {@code Single} is
     * a better option as {@link #subscribeActual} can decide to not subscribe to its upstream after all.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code lift} does not operate by default on a particular {@link Scheduler}, however, the
     *  {@link SingleOperator} may use a {@code Scheduler} to support its own asynchronous behavior.</dd>
     * </dl>
     *
     * @param <R> the output value type
     * @param lift the {@link SingleOperator} that receives the downstream's {@code SingleObserver} and should return
     *               a {@code SingleObserver} with custom behavior to be used as the consumer for the current
     *               {@code Single}.
     * @return the new Single instance
     * @see <a href="https://github.com/ReactiveX/RxJava/wiki/Writing-operators-for-2.0">RxJava wiki: Writing operators</a>
     * @see #compose(SingleTransformer)
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public final <R> Single<R> lift(@NonNull SingleOperator<? extends R, ? super T> lift) {
        Objects.requireNonNull(lift, "lift is null");
        return RxJavaPlugins.onAssembly(new SingleLift<T, R>(this, lift));
    }

    /**
     * Returns a Single that applies a specified function to the item emitted by the source Single and
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
     *            a function to apply to the item emitted by the Single
     * @return a Single that emits the item from the source Single, transformed by the specified function
     * @see <a href="http://reactivex.io/documentation/operators/map.html">ReactiveX operators documentation: Map</a>
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public final <@NonNull R> Single<R> map(@NonNull Function<? super T, ? extends R> mapper) {
        Objects.requireNonNull(mapper, "mapper is null");
        return RxJavaPlugins.onAssembly(new SingleMap<T, R>(this, mapper));
    }

    /**
     * Maps the signal types of this Single into a {@link Notification} of the same kind
     * and emits it as a single success value to downstream.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/materialize.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code materialize} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * <p>History: 2.2.4 - experimental
     * @return the new Single instance
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
     * Signals true if the current Single signals a success value that is Object-equals with the value
     * provided.
     * <p>
     * <img width="640" height="400" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.contains.png" alt="">
     * <p>
     * <img width="640" height="401" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.contains.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code contains} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param value the value to compare against the success value of this Single
     * @return the new Single instance
     * @since 2.0
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Single<Boolean> contains(@NonNull Object value) {
        return contains(value, ObjectHelper.equalsPredicate());
    }

    /**
     * Signals true if the current Single signals a success value that is equal with
     * the value provided by calling a bi-predicate.
     * <p>
     * <img width="640" height="401" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.contains.f.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code contains} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param value the value to compare against the success value of this Single
     * @param comparer the function that receives the success value of this Single, the value provided
     *                 and should return true if they are considered equal
     * @return the new Single instance
     * @since 2.0
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Single<Boolean> contains(@NonNull Object value, @NonNull BiPredicate<Object, Object> comparer) {
        Objects.requireNonNull(value, "value is null");
        Objects.requireNonNull(comparer, "comparer is null");
        return RxJavaPlugins.onAssembly(new SingleContains<>(this, value, comparer));
    }

    /**
     * Flattens this and another Single into a single Flowable, without any transformation.
     * <p>
     * <img width="640" height="415" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.mergeWith.png" alt="">
     * <p>
     * You can combine items emitted by multiple Singles so that they appear as a single Flowable, by using
     * the {@code mergeWith} method.
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The returned {@code Flowable} honors the backpressure of the downstream consumer.</dd>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code mergeWith} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param other
     *            a SingleSource to be merged
     * @return  that emits all of the items emitted by the source Singles
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
     * Modifies a Single to emit its item (or notify of its error) on a specified {@link Scheduler},
     * asynchronously.
     * <p>
     * <img width="640" height="305" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.observeOn.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>you specify which {@link Scheduler} this operator will use.</dd>
     * </dl>
     *
     * @param scheduler
     *            the {@link Scheduler} to notify subscribers on
     * @return the source Single modified so that its subscribers are notified on the specified
     *         {@link Scheduler}
     * @throws NullPointerException if scheduler is null
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
     * Instructs a Single to emit an item (returned by a specified function) rather than invoking
     * {@link SingleObserver#onError onError} if it encounters an error.
     * <p>
     * <img width="640" height="451" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.onErrorReturn.png" alt="">
     * <p>
     * By default, when a Single encounters an error that prevents it from emitting the expected item to its
     * subscriber, the Single invokes its subscriber's {@link SingleObserver#onError} method, and then quits
     * without invoking any more of its subscriber's methods. The {@code onErrorReturn} method changes this
     * behavior. If you pass a function ({@code resumeFunction}) to a Single's {@code onErrorReturn} method, if
     * the original Single encounters an error, instead of invoking its subscriber's
     * {@link SingleObserver#onError} method, it will instead emit the return value of {@code resumeFunction}.
     * <p>
     * You can use this to prevent errors from propagating or to supply fallback data should errors be
     * encountered.
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code onErrorReturn} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param resumeFunction
     *            a function that returns an item that the new Single will emit if the source Single encounters
     *            an error
     * @return the original Single with appropriately modified behavior
     * @see <a href="http://reactivex.io/documentation/operators/catch.html">ReactiveX operators documentation: Catch</a>
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Single<T> onErrorReturn(@NonNull Function<Throwable, ? extends T> resumeFunction) {
        Objects.requireNonNull(resumeFunction, "resumeFunction is null");
        return RxJavaPlugins.onAssembly(new SingleOnErrorReturn<>(this, resumeFunction, null));
    }

    /**
     * Signals the specified value as success in case the current Single signals an error.
     * <p>
     * <img width="640" height="451" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.onErrorReturnItem.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code onErrorReturnItem} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param value the value to signal if the current Single fails
     * @return the new Single instance
     * @since 2.0
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Single<T> onErrorReturnItem(@NonNull T value) {
        Objects.requireNonNull(value, "value is null");
        return RxJavaPlugins.onAssembly(new SingleOnErrorReturn<>(this, null, value));
    }

    /**
     * Instructs a Single to pass control to another Single rather than invoking
     * {@link SingleObserver#onError(Throwable)} if it encounters an error.
     * <p>
     * <img width="640" height="451" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.onErrorResumeWith.png" alt="">
     * <p>
     * By default, when a Single encounters an error that prevents it from emitting the expected item to
     * its {@link SingleObserver}, the Single invokes its SingleObserver's {@code onError} method, and then quits
     * without invoking any more of its SingleObserver's methods. The {@code onErrorResumeWith} method changes this
     * behavior. If you pass another Single ({@code resumeSingleInCaseOfError}) to a Single's
     * {@code onErrorResumeWith} method, if the original Single encounters an error, instead of invoking its
     * SingleObserver's {@code onError} method, it will instead relinquish control to {@code resumeSingleInCaseOfError} which
     * will invoke the SingleObserver's {@link SingleObserver#onSuccess onSuccess} method if it is able to do so. In such a case,
     * because no Single necessarily invokes {@code onError}, the SingleObserver may never know that an error
     * happened.
     * <p>
     * You can use this to prevent errors from propagating or to supply fallback data should errors be
     * encountered.
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code onErrorResumeWith} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param resumeSingleInCaseOfError a Single that will take control if source Single encounters an error.
     * @return the original Single, with appropriately modified behavior.
     * @see <a href="http://reactivex.io/documentation/operators/catch.html">ReactiveX operators documentation: Catch</a>
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Single<T> onErrorResumeWith(@NonNull SingleSource<? extends T> resumeSingleInCaseOfError) {
        Objects.requireNonNull(resumeSingleInCaseOfError, "resumeSingleInCaseOfError is null");
        return onErrorResumeNext(Functions.justFunction(resumeSingleInCaseOfError));
    }

    /**
     * Instructs a Single to pass control to another Single rather than invoking
     * {@link SingleObserver#onError(Throwable)} if it encounters an error.
     * <p>
     * <img width="640" height="451" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.onErrorResumeNext.f.png" alt="">
     * <p>
     * By default, when a Single encounters an error that prevents it from emitting the expected item to
     * its {@link SingleObserver}, the Single invokes its SingleObserver's {@code onError} method, and then quits
     * without invoking any more of its SingleObserver's methods. The {@code onErrorResumeNext} method changes this
     * behavior. If you pass a function that will return another Single ({@code resumeFunctionInCaseOfError}) to a Single's
     * {@code onErrorResumeNext} method, if the original Single encounters an error, instead of invoking its
     * SingleObserver's {@code onError} method, it will instead relinquish control to {@code resumeSingleInCaseOfError} which
     * will invoke the SingleObserver's {@link SingleObserver#onSuccess onSuccess} method if it is able to do so. In such a case,
     * because no Single necessarily invokes {@code onError}, the SingleObserver may never know that an error
     * happened.
     * <p>
     * You can use this to prevent errors from propagating or to supply fallback data should errors be
     * encountered.
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code onErrorResumeNext} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param resumeFunctionInCaseOfError a function that returns a Single that will take control if source Single encounters an error.
     * @return the original Single, with appropriately modified behavior.
     * @see <a href="http://reactivex.io/documentation/operators/catch.html">ReactiveX operators documentation: Catch</a>
     * @since .20
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Single<T> onErrorResumeNext(
            @NonNull Function<? super Throwable, ? extends SingleSource<? extends T>> resumeFunctionInCaseOfError) {
        Objects.requireNonNull(resumeFunctionInCaseOfError, "resumeFunctionInCaseOfError is null");
        return RxJavaPlugins.onAssembly(new SingleResumeNext<>(this, resumeFunctionInCaseOfError));
    }

    /**
     * Nulls out references to the upstream producer and downstream SingleObserver if
     * the sequence is terminated or downstream calls dispose().
     * <p>
     * <img width="640" height="346" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.onTerminateDetach.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code onTerminateDetach} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * <p>History: 2.1.5 - experimental
     * @return a Single which nulls out references to the upstream producer and downstream SingleObserver if
     * the sequence is terminated or downstream calls dispose()
     * @since 2.2
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Single<T> onTerminateDetach() {
        return RxJavaPlugins.onAssembly(new SingleDetach<>(this));
    }

    /**
     * Repeatedly re-subscribes to the current Single and emits each success value.
     * <p>
     * <img width="640" height="457" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.repeat.png" alt="">
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The returned {@code Flowable} honors the backpressure of the downstream consumer.</dd>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code repeat} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @return the new Flowable instance
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
     * Re-subscribes to the current Single at most the given number of times and emits each success value.
     * <p>
     * <img width="640" height="457" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.repeat.n.png" alt="">
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The returned {@code Flowable} honors the backpressure of the downstream consumer.</dd>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code repeat} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param times the number of times to re-subscribe to the current Single
     * @return the new Flowable instance
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
     * Re-subscribes to the current Single if
     * the Publisher returned by the handler function signals a value in response to a
     * value signalled through the Flowable the handle receives.
     * <p>
     * <img width="640" height="1478" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.repeatWhen.png" alt="">
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The returned {@code Flowable} honors the backpressure of the downstream consumer.
     *  The {@code Publisher} returned by the handler function is expected to honor backpressure as well.</dd>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code repeatWhen} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param handler the function that is called with a Flowable that signals a value when the Single
     *                signalled a success value and returns a Publisher that has to signal a value to
     *                trigger a resubscription to the current Single, otherwise the terminal signal of
     *                the Publisher will be the terminal signal of the sequence as well.
     * @return the new Flowable instance
     * @since 2.0
     */
    @BackpressureSupport(BackpressureKind.FULL)
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Flowable<T> repeatWhen(@NonNull Function<? super Flowable<Object>, ? extends Publisher<?>> handler) {
        return toFlowable().repeatWhen(handler);
    }

    /**
     * Re-subscribes to the current Single until the given BooleanSupplier returns true.
     * <p>
     * <img width="640" height="463" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.repeatUntil.png" alt="">
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The returned {@code Flowable} honors the backpressure of the downstream consumer.</dd>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code repeatUntil} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param stop the BooleanSupplier called after the current Single succeeds and if returns false,
     *             the Single is re-subscribed; otherwise the sequence completes.
     * @return the new Flowable instance
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
     * Repeatedly re-subscribes to the current Single indefinitely if it fails with an onError.
     * <p>
     * <img width="640" height="399" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.retry.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code retry} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @return the new Single instance
     * @since 2.0
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Single<T> retry() {
        return toSingle(toFlowable().retry());
    }

    /**
     * Repeatedly re-subscribe at most the specified times to the current Single
     * if it fails with an onError.
     * <p>
     * <img width="640" height="329" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.retry.n.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code retry} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param times the number of times to resubscribe if the current Single fails
     * @return the new Single instance
     * @since 2.0
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Single<T> retry(long times) {
        return toSingle(toFlowable().retry(times));
    }

    /**
     * Re-subscribe to the current Single if the given predicate returns true when the Single fails
     * with an onError.
     * <p>
     * <img width="640" height="230" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.retry.f2.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code retry} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param predicate the predicate called with the resubscription count and the failure Throwable
     *                  and should return true if a resubscription should happen
     * @return the new Single instance
     * @since 2.0
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Single<T> retry(@NonNull BiPredicate<? super Integer, ? super Throwable> predicate) {
        return toSingle(toFlowable().retry(predicate));
    }

    /**
     * Repeatedly re-subscribe at most times or until the predicate returns false, whichever happens first
     * if it fails with an onError.
     * <p>
     * <img width="640" height="259" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.retry.nf.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code retry} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * <p>History: 2.1.8 - experimental
     * @param times the number of times to resubscribe if the current Single fails
     * @param predicate the predicate called with the failure Throwable
     *                  and should return true if a resubscription should happen
     * @return the new Single instance
     * @since 2.2
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Single<T> retry(long times, @NonNull Predicate<? super Throwable> predicate) {
        return toSingle(toFlowable().retry(times, predicate));
    }

    /**
     * Re-subscribe to the current Single if the given predicate returns true when the Single fails
     * with an onError.
     * <p>
     * <img width="640" height="240" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.retry.f.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code retry} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param predicate the predicate called with the failure Throwable
     *                  and should return true if a resubscription should happen
     * @return the new Single instance
     * @since 2.0
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Single<T> retry(@NonNull Predicate<? super Throwable> predicate) {
        return toSingle(toFlowable().retry(predicate));
    }

    /**
     * Re-subscribes to the current Single if and when the Publisher returned by the handler
     * function signals a value.
     * <p>
     * <img width="640" height="405" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.retryWhen.png" alt="">
     * <p>
     * If the Publisher signals an {@code onComplete}, the resulting {@code Single} will signal a {@link NoSuchElementException}.
     * <p>
     * Note that the inner {@code Publisher} returned by the handler function should signal
     * either {@code onNext}, {@code onError} or {@code onComplete} in response to the received
     * {@code Throwable} to indicate the operator should retry or terminate. If the upstream to
     * the operator is asynchronous, signaling onNext followed by onComplete immediately may
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
     * @param handler the function that receives a Flowable of the error the Single emits and should
     *                return a Publisher that should signal a normal value (in response to the
     *                throwable the Flowable emits) to trigger a resubscription or signal an error to
     *                be the output of the resulting Single
     * @return the new Single instance
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Single<T> retryWhen(@NonNull Function<? super Flowable<Throwable>, ? extends Publisher<?>> handler) {
        return toSingle(toFlowable().retryWhen(handler));
    }

    /**
     * Subscribes to a Single but ignore its emission or notification.
     * <p>
     * <img width="640" height="340" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.subscribe.png" alt="">
     * <p>
     * If the Single emits an error, it is wrapped into an
     * {@link io.reactivex.rxjava3.exceptions.OnErrorNotImplementedException OnErrorNotImplementedException}
     * and routed to the RxJavaPlugins.onError handler.
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code subscribe} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @return a {@link Disposable} reference can request the {@link Single} stop work.
     * @see <a href="http://reactivex.io/documentation/operators/subscribe.html">ReactiveX operators documentation: Subscribe</a>
     */
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Disposable subscribe() {
        return subscribe(Functions.emptyConsumer(), Functions.ON_ERROR_MISSING);
    }

    /**
     * Subscribes to a Single and provides a composite callback to handle the item it emits
     * or any error notification it issues.
     * <p>
     * <img width="640" height="340" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.subscribe.c2.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code subscribe} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param onCallback
     *            the callback that receives either the success value or the failure Throwable
     *            (whichever is not null)
     * @return a {@link Disposable} reference can request the {@link Single} stop work.
     * @see <a href="http://reactivex.io/documentation/operators/subscribe.html">ReactiveX operators documentation: Subscribe</a>
     * @throws NullPointerException
     *             if {@code onCallback} is null
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Disposable subscribe(@NonNull BiConsumer<? super T, ? super Throwable> onCallback) {
        Objects.requireNonNull(onCallback, "onCallback is null");

        BiConsumerSingleObserver<T> observer = new BiConsumerSingleObserver<>(onCallback);
        subscribe(observer);
        return observer;
    }

    /**
     * Subscribes to a Single and provides a callback to handle the item it emits.
     * <p>
     * <img width="640" height="341" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.subscribe.c.png" alt="">
     * <p>
     * If the Single emits an error, it is wrapped into an
     * {@link io.reactivex.rxjava3.exceptions.OnErrorNotImplementedException OnErrorNotImplementedException}
     * and routed to the RxJavaPlugins.onError handler.
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code subscribe} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param onSuccess
     *            the {@code Consumer<T>} you have designed to accept the emission from the Single
     * @return a {@link Disposable} reference can request the {@link Single} stop work.
     * @throws NullPointerException
     *             if {@code onSuccess} is null
     * @see <a href="http://reactivex.io/documentation/operators/subscribe.html">ReactiveX operators documentation: Subscribe</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Disposable subscribe(@NonNull Consumer<? super T> onSuccess) {
        return subscribe(onSuccess, Functions.ON_ERROR_MISSING);
    }

    /**
     * Subscribes to a Single and provides callbacks to handle the item it emits or any error notification it
     * issues.
     * <p>
     * <img width="640" height="340" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.subscribe.cc.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code subscribe} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param onSuccess
     *            the {@code Consumer<T>} you have designed to accept the emission from the Single
     * @param onError
     *            the {@code Consumer<Throwable>} you have designed to accept any error notification from the
     *            Single
     * @return a {@link Disposable} reference can request the {@link Single} stop work.
     * @see <a href="http://reactivex.io/documentation/operators/subscribe.html">ReactiveX operators documentation: Subscribe</a>
     * @throws NullPointerException
     *             if {@code onSuccess} is null, or
     *             if {@code onError} is null
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
     * @param observer the SingleObserver to handle, not null
     */
    protected abstract void subscribeActual(@NonNull SingleObserver<? super T> observer);

    /**
     * Subscribes a given SingleObserver (subclass) to this Single and returns the given
     * SingleObserver as is.
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
     * @param <E> the type of the SingleObserver to use and return
     * @param observer the SingleObserver (subclass) to use and return, not null
     * @return the input {@code observer}
     * @throws NullPointerException if {@code observer} is null
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
     * Asynchronously subscribes subscribers to this Single on the specified {@link Scheduler}.
     * <p>
     * <img width="640" height="305" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.subscribeOn.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>You specify which {@link Scheduler} this operator will use.</dd>
     * </dl>
     *
     * @param scheduler
     *            the {@link Scheduler} to perform subscription actions on
     * @return the source Single modified so that its subscriptions happen on the specified {@link Scheduler}
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
     * Returns a Single that emits the item emitted by the source Single until a Completable terminates. Upon
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
     *            the Completable whose termination will cause {@code takeUntil} to emit the item from the source
     *            Single
     * @return a Single that emits the item emitted by the source Single until such time as {@code other} terminates.
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
     * Returns a Single that emits the item emitted by the source Single until a Publisher emits an item. Upon
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
     *            the Publisher whose first emitted item will cause {@code takeUntil} to emit the item from the source
     *            Single
     * @param <E>
     *            the type of items emitted by {@code other}
     * @return a Single that emits the item emitted by the source Single until such time as {@code other} emits
     * its first item
     * @see <a href="http://reactivex.io/documentation/operators/takeuntil.html">ReactiveX operators documentation: TakeUntil</a>
     */
    @BackpressureSupport(BackpressureKind.FULL)
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public final <E> Single<T> takeUntil(@NonNull Publisher<E> other) {
        Objects.requireNonNull(other, "other is null");
        return RxJavaPlugins.onAssembly(new SingleTakeUntil<>(this, other));
    }

    /**
     * Returns a Single that emits the item emitted by the source Single until a second Single emits an item. Upon
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
     *            the Single whose emitted item will cause {@code takeUntil} to emit the item from the source Single
     * @param <E>
     *            the type of item emitted by {@code other}
     * @return a Single that emits the item emitted by the source Single until such time as {@code other} emits its item
     * @see <a href="http://reactivex.io/documentation/operators/takeuntil.html">ReactiveX operators documentation: TakeUntil</a>
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public final <E> Single<T> takeUntil(@NonNull SingleSource<? extends E> other) {
        Objects.requireNonNull(other, "other is null");
        return takeUntil(new SingleToFlowable<E>(other));
    }

    /**
     * Signals a TimeoutException if the current Single doesn't signal a success value within the
     * specified timeout window.
     * <p>
     * <img width="640" height="364" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.timeout.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code timeout} signals the TimeoutException on the {@code computation} {@link Scheduler}.</dd>
     * </dl>
     * @param timeout the timeout amount
     * @param unit the time unit
     * @return the new Single instance
     * @since 2.0
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.COMPUTATION)
    @NonNull
    public final Single<T> timeout(long timeout, @NonNull TimeUnit unit) {
        return timeout0(timeout, unit, Schedulers.computation(), null);
    }

    /**
     * Signals a TimeoutException if the current Single doesn't signal a success value within the
     * specified timeout window.
     * <p>
     * <img width="640" height="334" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.timeout.s.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code timeout} signals the TimeoutException on the {@link Scheduler} you specify.</dd>
     * </dl>
     * @param timeout the timeout amount
     * @param unit the time unit
     * @param scheduler the target scheduler where the timeout is awaited and the TimeoutException
     *                  signalled
     * @return the new Single instance
     * @since 2.0
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    @NonNull
    public final Single<T> timeout(long timeout, @NonNull TimeUnit unit, @NonNull Scheduler scheduler) {
        return timeout0(timeout, unit, scheduler, null);
    }

    /**
     * Runs the current Single and if it doesn't signal within the specified timeout window, it is
     * disposed and the other SingleSource subscribed to.
     * <p>
     * <img width="640" height="283" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.timeout.sb.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code timeout} subscribes to the other SingleSource on the {@link Scheduler} you specify.</dd>
     * </dl>
     * @param timeout the timeout amount
     * @param unit the time unit
     * @param scheduler the scheduler where the timeout is awaited and the subscription to other happens
     * @param other the other SingleSource that gets subscribed to if the current Single times out
     * @return the new Single instance
     * @since 2.0
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    public final Single<T> timeout(long timeout, @NonNull TimeUnit unit, @NonNull Scheduler scheduler, @NonNull SingleSource<? extends T> other) {
        Objects.requireNonNull(other, "other is null");
        return timeout0(timeout, unit, scheduler, other);
    }

    /**
     * Runs the current Single and if it doesn't signal within the specified timeout window, it is
     * disposed and the other SingleSource subscribed to.
     * <p>
     * <img width="640" height="282" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.timeout.b.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code timeout} subscribes to the other SingleSource on
     *  the {@code computation} {@link Scheduler}.</dd>
     * </dl>
     * @param timeout the timeout amount
     * @param unit the time unit
     * @param other the other SingleSource that gets subscribed to if the current Single times out
     * @return the new Single instance
     * @throws NullPointerException
     *             if other is null, or
     *             if unit is null, or
     *             if scheduler is null
     * @since 2.0
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.COMPUTATION)
    public final Single<T> timeout(long timeout, @NonNull TimeUnit unit, @NonNull SingleSource<? extends T> other) {
        Objects.requireNonNull(other, "other is null");
        return timeout0(timeout, unit, Schedulers.computation(), other);
    }

    private Single<T> timeout0(final long timeout, final TimeUnit unit, final Scheduler scheduler, final SingleSource<? extends T> other) {
        Objects.requireNonNull(unit, "unit is null");
        Objects.requireNonNull(scheduler, "scheduler is null");
        return RxJavaPlugins.onAssembly(new SingleTimeout<>(this, timeout, unit, scheduler, other));
    }

    /**
     * Calls the specified converter function during assembly time and returns its resulting value.
     * <p>
     * <img width="640" height="553" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.to.png" alt="">
     * <p>
     * This allows fluent conversion to any other type.
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code to} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * <p>History: 2.1.7 - experimental
     * @param <R> the resulting object type
     * @param converter the function that receives the current Single instance and returns a value
     * @return the converted value
     * @throws NullPointerException if converter is null
     * @since 2.2
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public final <R> R to(@NonNull SingleConverter<T, ? extends R> converter) {
        return Objects.requireNonNull(converter, "converter is null").apply(this);
    }

    /**
     * Returns a {@link Completable} that ignores the success value of this {@link Single}
     * and signals {@code onComplete} instead.
     * <p>
     * <img width="640" height="436" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.ignoreElement.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code ignoreElement} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @return a {@link Completable} that signals {@code onComplete} on it's observer when the source {@link Single}
     *         calls {@code onSuccess}.
     * @since 2.1.13
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Completable ignoreElement() {
        return RxJavaPlugins.onAssembly(new CompletableFromSingle<>(this));
    }

    /**
     * Converts this Single into a {@link Flowable}.
     * <p>
     * <img width="640" height="462" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.toFlowable.png" alt="">
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The returned {@code Flowable} honors the backpressure of the downstream consumer.</dd>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code toFlowable} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @return a {@link Flowable} that emits a single item T or an error.
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
     * <img width="640" height="467" src="https://github.com/ReactiveX/RxJava/wiki/images/rx-operators/Single.toFuture.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code toFuture} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @return a {@link Future} that expects a single item to be emitted by this {@code Single}
     * @see <a href="http://reactivex.io/documentation/operators/to.html">ReactiveX documentation: To</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Future<T> toFuture() {
        return subscribeWith(new FutureSingleObserver<T>());
    }

    /**
     * Converts this Single into a {@link Maybe}.
     * <p>
     * <img width="640" height="463" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.toMaybe.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code toMaybe} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @return a {@link Maybe} that emits a single item T or an error.
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
     * Converts this Single into an {@link Observable}.
     * <p>
     * <img width="640" height="305" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.toObservable.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code toObservable} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @return an {@link Observable} that emits a single item T or an error.
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
     * Returns a Single which makes sure when a SingleObserver disposes the Disposable,
     * that call is propagated up on the specified scheduler.
     * <p>
     * <img width="640" height="693" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.unsubscribeOn.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code unsubscribeOn} calls dispose() of the upstream on the {@link Scheduler} you specify.</dd>
     * </dl>
     * <p>History: 2.0.9 - experimental
     * @param scheduler the target scheduler where to execute the disposal
     * @return the new Single instance
     * @throws NullPointerException if scheduler is null
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
     * Returns a Single that emits the result of applying a specified function to the pair of items emitted by
     * the source Single and another specified Single.
     * <p>
     * <img width="640" height="422" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.zipWith.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code zipWith} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <U>
     *            the type of items emitted by the {@code other} Single
     * @param <R>
     *            the type of items emitted by the resulting Single
     * @param other
     *            the other SingleSource
     * @param zipper
     *            a function that combines the pairs of items from the two SingleSources to generate the items to
     *            be emitted by the resulting Single
     * @return a Single that pairs up values from the source Single and the {@code other} SingleSource
     *         and emits the results of {@code zipFunction} applied to these pairs
     * @see <a href="http://reactivex.io/documentation/operators/zip.html">ReactiveX operators documentation: Zip</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <U, R> Single<R> zipWith(@NonNull SingleSource<U> other, @NonNull BiFunction<? super T, ? super U, ? extends R> zipper) {
        return zip(this, other, zipper);
    }

    // -------------------------------------------------------------------------
    // Fluent test support, super handy and reduces test preparation boilerplate
    // -------------------------------------------------------------------------
    /**
     * Creates a TestObserver and subscribes
     * it to this Single.
     * <p>
     * <img width="640" height="442" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.test.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code test} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @return the new TestObserver instance
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
     * Creates a TestObserver optionally in cancelled state, then subscribes it to this Single.
     * <p>
     * <img width="640" height="482" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.test.b.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code test} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param dispose if true, the TestObserver will be cancelled before subscribing to this
     * Single.
     * @return the new TestObserver instance
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
     * @param <T> the element type of the CompletionStage
     * @param stage the CompletionStage to convert to Single and signal its success value or error
     * @return the new Single instance
     * @since 3.0.0
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public static <T> Single<@NonNull T> fromCompletionStage(@NonNull CompletionStage<T> stage) {
        Objects.requireNonNull(stage, "stage is null");
        return RxJavaPlugins.onAssembly(new SingleFromCompletionStage<>(stage));
    }

    /**
     * Maps the upstream success value into an {@link Optional} and emits the contained item if not empty.
     * <p>
     * <img width="640" height="323" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/mapOptional.s.png" alt="">
     *
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code mapOptional} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <R> the non-null output type
     * @param mapper the function that receives the upstream success iteem and should return a <em>non-empty</em> {@code Optional}
     * to emit as the success output or an <em>empty</em> {@code Optional} to complete the {@code Maybe}
     * @return the new Maybe instance
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
     * @return the new CompletionStage instance
     * @since 3.0.0
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final CompletionStage<T> toCompletionStage() {
        return subscribeWith(new CompletionStageConsumer<>(false, null));
    }
}
