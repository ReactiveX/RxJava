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

import org.reactivestreams.Publisher;

import io.reactivex.rxjava3.annotations.*;
import io.reactivex.rxjava3.disposables.*;
import io.reactivex.rxjava3.exceptions.*;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.internal.functions.*;
import io.reactivex.rxjava3.internal.jdk8.*;
import io.reactivex.rxjava3.internal.observers.*;
import io.reactivex.rxjava3.internal.operators.flowable.*;
import io.reactivex.rxjava3.internal.operators.maybe.MaybeToObservable;
import io.reactivex.rxjava3.internal.operators.mixed.*;
import io.reactivex.rxjava3.internal.operators.observable.*;
import io.reactivex.rxjava3.internal.operators.single.SingleToObservable;
import io.reactivex.rxjava3.internal.util.*;
import io.reactivex.rxjava3.observables.*;
import io.reactivex.rxjava3.observers.*;
import io.reactivex.rxjava3.operators.ScalarSupplier;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.schedulers.*;

/**
 * The {@code Observable} class is the non-backpressured, optionally multi-valued base reactive class that
 * offers factory methods, intermediate operators and the ability to consume synchronous
 * and/or asynchronous reactive dataflows.
 * <p>
 * Many operators in the class accept {@link ObservableSource}(s), the base reactive interface
 * for such non-backpressured flows, which {@code Observable} itself implements as well.
 * <p>
 * The {@code Observable}'s operators, by default, run with a buffer size of 128 elements (see {@link Flowable#bufferSize()}),
 * that can be overridden globally via the system parameter {@code rx3.buffer-size}. Most operators, however, have
 * overloads that allow setting their internal buffer size explicitly.
 * <p>
 * The documentation for this class makes use of marble diagrams. The following legend explains these diagrams:
 * <p>
 * <img width="640" height="317" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/legend.v3.png" alt="">
 * <p>
 * The design of this class was derived from the
 * <a href="https://github.com/reactive-streams/reactive-streams-jvm">Reactive-Streams design and specification</a>
 * by removing any backpressure-related infrastructure and implementation detail, replacing the
 * {@code org.reactivestreams.Subscription} with {@link Disposable} as the primary means to dispose of
 * a flow.
 * <p>
 * The {@code Observable} follows the protocol
 * <pre><code>
 *      onSubscribe onNext* (onError | onComplete)?
 * </code></pre>
 * where
 * the stream can be disposed through the {@code Disposable} instance provided to consumers through
 * {@code Observer.onSubscribe}.
 * <p>
 * Unlike the {@code Observable} of version 1.x, {@link #subscribe(Observer)} does not allow external disposal
 * of a subscription and the {@link Observer} instance is expected to expose such capability.
 * <p>Example:
 * <pre><code>
 * Disposable d = Observable.just("Hello world!")
 *     .delay(1, TimeUnit.SECONDS)
 *     .subscribeWith(new DisposableObserver&lt;String&gt;() {
 *         &#64;Override public void onStart() {
 *             System.out.println("Start!");
 *         }
 *         &#64;Override public void onNext(String t) {
 *             System.out.println(t);
 *         }
 *         &#64;Override public void onError(Throwable t) {
 *             t.printStackTrace();
 *         }
 *         &#64;Override public void onComplete() {
 *             System.out.println("Done!");
 *         }
 *     });
 *
 * Thread.sleep(500);
 * // the sequence can now be disposed via dispose()
 * d.dispose();
 * </code></pre>
 *
 * @param <T>
 *            the type of the items emitted by the {@code Observable}
 * @see Flowable
 * @see io.reactivex.rxjava3.observers.DisposableObserver
 */
public abstract class Observable<@NonNull T> implements ObservableSource<T> {

    /**
     * Mirrors the one {@link ObservableSource} in an {@link Iterable} of several {@code ObservableSource}s that first either emits an item or sends
     * a termination notification.
     * <p>
     * <img width="640" height="505" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Observable.amb.png" alt="">
     * <p>
     * When one of the {@code ObservableSource}s signal an item or terminates first, all subscriptions to the other
     * {@code ObservableSource}s are disposed.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code amb} does not operate by default on a particular {@link Scheduler}.</dd>
     *  <dt><b>Error handling:</b></dt>
     *  <dd>
     *     If any of the losing {@code ObservableSource}s signals an error, the error is routed to the global
     *     error handler via {@link RxJavaPlugins#onError(Throwable)}.
     *  </dd>
     * </dl>
     *
     * @param <T> the common element type
     * @param sources
     *            an {@code Iterable} of {@code ObservableSource} sources competing to react first. A subscription to each source will
     *            occur in the same order as in the {@code Iterable}.
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code sources} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/amb.html">ReactiveX operators documentation: Amb</a>
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <@NonNull T> Observable<T> amb(@NonNull Iterable<@NonNull ? extends ObservableSource<? extends T>> sources) {
        Objects.requireNonNull(sources, "sources is null");
        return RxJavaPlugins.onAssembly(new ObservableAmb<>(null, sources));
    }

    /**
     * Mirrors the one {@link ObservableSource} in an array of several {@code ObservableSource}s that first either emits an item or sends
     * a termination notification.
     * <p>
     * <img width="640" height="505" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Observable.ambArray.png" alt="">
     * <p>
     * When one of the {@code ObservableSource}s signal an item or terminates first, all subscriptions to the other
     * {@code ObservableSource}s are disposed.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code ambArray} does not operate by default on a particular {@link Scheduler}.</dd>
     *  <dt><b>Error handling:</b></dt>
     *  <dd>
     *     If any of the losing {@code ObservableSource}s signals an error, the error is routed to the global
     *     error handler via {@link RxJavaPlugins#onError(Throwable)}.
     *  </dd>
     * </dl>
     *
     * @param <T> the common element type
     * @param sources
     *            an array of {@code ObservableSource} sources competing to react first. A subscription to each source will
     *            occur in the same order as in the array.
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code sources} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/amb.html">ReactiveX operators documentation: Amb</a>
     */
    @SuppressWarnings("unchecked")
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    @SafeVarargs
    public static <@NonNull T> Observable<T> ambArray(@NonNull ObservableSource<? extends T>... sources) {
        Objects.requireNonNull(sources, "sources is null");
        int len = sources.length;
        if (len == 0) {
            return empty();
        }
        if (len == 1) {
            return (Observable<T>)wrap(sources[0]);
        }
        return RxJavaPlugins.onAssembly(new ObservableAmb<>(sources, null));
    }

    /**
     * Returns the default 'island' size or capacity-increment hint for unbounded buffers.
     * <p>Delegates to {@link Flowable#bufferSize} but is public for convenience.
     * <p>The value can be overridden via system parameter {@code rx3.buffer-size}
     * <em>before</em> the {@link Flowable} class is loaded.
     * @return the default 'island' size or capacity-increment hint
     */
    @CheckReturnValue
    public static int bufferSize() {
        return Flowable.bufferSize();
    }

    /**
     * Combines a collection of source {@link ObservableSource}s by emitting an item that aggregates the latest values of each of
     * the returned {@code ObservableSource}s each time an item is received from any of the returned {@code ObservableSource}s, where this
     * aggregation is defined by a specified function.
     * <p>
     * Note on method signature: since Java doesn't allow creating a generic array with {@code new T[]}, the
     * implementation of this operator has to create an {@code Object[]} instead. Unfortunately, a
     * {@code Function<Integer[], R>} passed to the method would trigger a {@link ClassCastException}.
     * <p>
     * If any of the sources never produces an item but only terminates (normally or with an error), the
     * resulting sequence terminates immediately (normally or with all the errors accumulated till that point).
     * If that input source is also synchronous, other sources after it will not be subscribed to.
     * <p>
     * If the provided iterable of {@code ObservableSource}s is empty, the resulting sequence completes immediately without emitting
     * any items and without any calls to the combiner function.
     *
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/combineLatest.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code combineLatest} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T>
     *            the common base type of source values
     * @param <R>
     *            the result type
     * @param sources
     *            the collection of source {@code ObservableSource}s
     * @param combiner
     *            the aggregation function used to combine the items emitted by the returned {@code ObservableSource}s
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code sources} or {@code combiner} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/combinelatest.html">ReactiveX operators documentation: CombineLatest</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public static <@NonNull T, @NonNull R> Observable<R> combineLatest(
            @NonNull Iterable<@NonNull ? extends ObservableSource<? extends T>> sources,
            @NonNull Function<? super Object[], ? extends R> combiner) {
        return combineLatest(sources, combiner, bufferSize());
    }

    /**
     * Combines an {@link Iterable} of source {@link ObservableSource}s by emitting an item that aggregates the latest values of each of
     * the returned {@code ObservableSource}s each time an item is received from any of the returned {@code ObservableSource}s, where this
     * aggregation is defined by a specified function.
     * <p>
     * Note on method signature: since Java doesn't allow creating a generic array with {@code new T[]}, the
     * implementation of this operator has to create an {@code Object[]} instead. Unfortunately, a
     * {@code Function<Integer[], R>} passed to the method would trigger a {@link ClassCastException}.
     * <p>
     * If any of the sources never produces an item but only terminates (normally or with an error), the
     * resulting sequence terminates immediately (normally or with all the errors accumulated till that point).
     * If that input source is also synchronous, other sources after it will not be subscribed to.
     * <p>
     * If the provided {@code Iterable} of {@code ObservableSource}s is empty, the resulting sequence completes immediately without emitting
     * any items and without any calls to the combiner function.
     *
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/combineLatest.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code combineLatest} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T>
     *            the common base type of source values
     * @param <R>
     *            the result type
     * @param sources
     *            the collection of source {@code ObservableSource}s
     * @param combiner
     *            the aggregation function used to combine the items emitted by the returned {@code ObservableSource}s
     * @param bufferSize
     *            the expected number of row combination items to be buffered internally
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code sources} or {@code combiner} is {@code null}
     * @throws IllegalArgumentException if {@code bufferSize} is non-positive
     * @see <a href="http://reactivex.io/documentation/operators/combinelatest.html">ReactiveX operators documentation: CombineLatest</a>
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <@NonNull T, @NonNull R> Observable<R> combineLatest(
            @NonNull Iterable<@NonNull ? extends ObservableSource<? extends T>> sources,
            @NonNull Function<? super Object[], ? extends R> combiner, int bufferSize) {
        Objects.requireNonNull(sources, "sources is null");
        Objects.requireNonNull(combiner, "combiner is null");
        ObjectHelper.verifyPositive(bufferSize, "bufferSize");

        // the queue holds a pair of values so we need to double the capacity
        int s = bufferSize << 1;
        return RxJavaPlugins.onAssembly(new ObservableCombineLatest<>(null, sources, combiner, s, false));
    }

    /**
     * Combines an array of source {@link ObservableSource}s by emitting an item that aggregates the latest values of each of
     * the {@code ObservableSource}s each time an item is received from any of the returned {@code ObservableSource}s, where this
     * aggregation is defined by a specified function.
     * <p>
     * Note on method signature: since Java doesn't allow creating a generic array with {@code new T[]}, the
     * implementation of this operator has to create an {@code Object[]} instead. Unfortunately, a
     * {@code Function<Integer[], R>} passed to the method would trigger a {@link ClassCastException}.
     * <p>
     * If any of the sources never produces an item but only terminates (normally or with an error), the
     * resulting sequence terminates immediately (normally or with all the errors accumulated till that point).
     * If that input source is also synchronous, other sources after it will not be subscribed to.
     * <p>
     * If the provided array of {@code ObservableSource}s is empty, the resulting sequence completes immediately without emitting
     * any items and without any calls to the combiner function.
     *
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/combineLatest.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code combineLatestArray} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T>
     *            the common base type of source values
     * @param <R>
     *            the result type
     * @param sources
     *            the collection of source {@code ObservableSource}s
     * @param combiner
     *            the aggregation function used to combine the items emitted by the {@code ObservableSource}s
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code sources} or {@code combiner} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/combinelatest.html">ReactiveX operators documentation: CombineLatest</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public static <@NonNull T, @NonNull R> Observable<R> combineLatestArray(
            @NonNull ObservableSource<? extends T>[] sources,
            @NonNull Function<? super Object[], ? extends R> combiner) {
        return combineLatestArray(sources, combiner, bufferSize());
    }

    /**
     * Combines an array of source {@link ObservableSource}s by emitting an item that aggregates the latest values of each of
     * the {@code ObservableSource}s each time an item is received from any of the {@code ObservableSource}s, where this
     * aggregation is defined by a specified function.
     * <p>
     * Note on method signature: since Java doesn't allow creating a generic array with {@code new T[]}, the
     * implementation of this operator has to create an {@code Object[]} instead. Unfortunately, a
     * {@code Function<Integer[], R>} passed to the method would trigger a {@link ClassCastException}.
     * <p>
     * If any of the sources never produces an item but only terminates (normally or with an error), the
     * resulting sequence terminates immediately (normally or with all the errors accumulated till that point).
     * If that input source is also synchronous, other sources after it will not be subscribed to.
     * <p>
     * If the provided array of {@code ObservableSource}s is empty, the resulting sequence completes immediately without emitting
     * any items and without any calls to the combiner function.
     *
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/combineLatest.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code combineLatestArray} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T>
     *            the common base type of source values
     * @param <R>
     *            the result type
     * @param sources
     *            the collection of source {@code ObservableSource}s
     * @param combiner
     *            the aggregation function used to combine the items emitted by the {@code ObservableSource}s
     * @param bufferSize
     *            the expected number of row combination items to be buffered internally
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code sources} or {@code combiner} is {@code null}
     * @throws IllegalArgumentException if {@code bufferSize} is non-positive
     * @see <a href="http://reactivex.io/documentation/operators/combinelatest.html">ReactiveX operators documentation: CombineLatest</a>
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <@NonNull T, @NonNull R> Observable<R> combineLatestArray(
            @NonNull ObservableSource<? extends T>[] sources,
            @NonNull Function<? super Object[], ? extends R> combiner, int bufferSize) {
        Objects.requireNonNull(sources, "sources is null");
        if (sources.length == 0) {
            return empty();
        }
        Objects.requireNonNull(combiner, "combiner is null");
        ObjectHelper.verifyPositive(bufferSize, "bufferSize");

        // the queue holds a pair of values so we need to double the capacity
        int s = bufferSize << 1;
        return RxJavaPlugins.onAssembly(new ObservableCombineLatest<>(sources, null, combiner, s, false));
    }

    /**
     * Combines two source {@link ObservableSource}s by emitting an item that aggregates the latest values of each of the
     * {@code ObservableSource}s each time an item is received from either of the {@code ObservableSource}s, where this
     * aggregation is defined by a specified function.
     * <p>
     * If any of the sources never produces an item but only terminates (normally or with an error), the
     * resulting sequence terminates immediately (normally or with all the errors accumulated till that point).
     * If that input source is also synchronous, other sources after it will not be subscribed to.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/combineLatest.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code combineLatest} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T1> the element type of the first source
     * @param <T2> the element type of the second source
     * @param <R> the combined output type
     * @param source1
     *            the first source {@code ObservableSource}
     * @param source2
     *            the second source {@code ObservableSource}
     * @param combiner
     *            the aggregation function used to combine the items emitted by the {@code ObservableSource}s
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code source1}, {@code source2} or {@code combiner} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/combinelatest.html">ReactiveX operators documentation: CombineLatest</a>
     */
    @SuppressWarnings("unchecked")
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <@NonNull T1, @NonNull T2, @NonNull R> Observable<R> combineLatest(
            @NonNull ObservableSource<? extends T1> source1, @NonNull ObservableSource<? extends T2> source2,
            @NonNull BiFunction<? super T1, ? super T2, ? extends R> combiner) {
        Objects.requireNonNull(source1, "source1 is null");
        Objects.requireNonNull(source2, "source2 is null");
        Objects.requireNonNull(combiner, "combiner is null");
        return combineLatestArray(new ObservableSource[] { source1, source2 }, Functions.toFunction(combiner), bufferSize());
    }

    /**
     * Combines three source {@link ObservableSource}s by emitting an item that aggregates the latest values of each of the
     * {@code ObservableSource}s each time an item is received from any of the {@code ObservableSource}s, where this
     * aggregation is defined by a specified function.
     * <p>
     * If any of the sources never produces an item but only terminates (normally or with an error), the
     * resulting sequence terminates immediately (normally or with all the errors accumulated till that point).
     * If that input source is also synchronous, other sources after it will not be subscribed to.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/combineLatest.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code combineLatest} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T1> the element type of the first source
     * @param <T2> the element type of the second source
     * @param <T3> the element type of the third source
     * @param <R> the combined output type
     * @param source1
     *            the first source {@code ObservableSource}
     * @param source2
     *            the second source {@code ObservableSource}
     * @param source3
     *            the third source {@code ObservableSource}
     * @param combiner
     *            the aggregation function used to combine the items emitted by the {@code ObservableSource}s
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code source1}, {@code source2}, {@code source3} or {@code combiner} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/combinelatest.html">ReactiveX operators documentation: CombineLatest</a>
     */
    @SuppressWarnings("unchecked")
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <@NonNull T1, @NonNull T2, @NonNull T3, @NonNull R> Observable<R> combineLatest(
            @NonNull ObservableSource<? extends T1> source1, @NonNull ObservableSource<? extends T2> source2,
            @NonNull ObservableSource<? extends T3> source3,
            @NonNull Function3<? super T1, ? super T2, ? super T3, ? extends R> combiner) {
        Objects.requireNonNull(source1, "source1 is null");
        Objects.requireNonNull(source2, "source2 is null");
        Objects.requireNonNull(source3, "source3 is null");
        Objects.requireNonNull(combiner, "combiner is null");
        return combineLatestArray(new ObservableSource[] { source1, source2, source3 }, Functions.toFunction(combiner), bufferSize());
    }

    /**
     * Combines four source {@link ObservableSource}s by emitting an item that aggregates the latest values of each of the
     * {@code ObservableSource}s each time an item is received from any of the {@code ObservableSource}s, where this
     * aggregation is defined by a specified function.
     * <p>
     * If any of the sources never produces an item but only terminates (normally or with an error), the
     * resulting sequence terminates immediately (normally or with all the errors accumulated till that point).
     * If that input source is also synchronous, other sources after it will not be subscribed to.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/combineLatest.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code combineLatest} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T1> the element type of the first source
     * @param <T2> the element type of the second source
     * @param <T3> the element type of the third source
     * @param <T4> the element type of the fourth source
     * @param <R> the combined output type
     * @param source1
     *            the first source {@code ObservableSource}
     * @param source2
     *            the second source {@code ObservableSource}
     * @param source3
     *            the third source {@code ObservableSource}
     * @param source4
     *            the fourth source {@code ObservableSource}
     * @param combiner
     *            the aggregation function used to combine the items emitted by the {@code ObservableSource}s
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code source1}, {@code source2}, {@code source3},
     *                              {@code source4} or {@code combiner} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/combinelatest.html">ReactiveX operators documentation: CombineLatest</a>
     */
    @SuppressWarnings("unchecked")
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <@NonNull T1, @NonNull T2, @NonNull T3, @NonNull T4, @NonNull R> Observable<R> combineLatest(
            @NonNull ObservableSource<? extends T1> source1, @NonNull ObservableSource<? extends T2> source2,
            @NonNull ObservableSource<? extends T3> source3, @NonNull ObservableSource<? extends T4> source4,
            @NonNull Function4<? super T1, ? super T2, ? super T3, ? super T4, ? extends R> combiner) {
        Objects.requireNonNull(source1, "source1 is null");
        Objects.requireNonNull(source2, "source2 is null");
        Objects.requireNonNull(source3, "source3 is null");
        Objects.requireNonNull(source4, "source4 is null");
        Objects.requireNonNull(combiner, "combiner is null");
        return combineLatestArray(new ObservableSource[] { source1, source2, source3, source4 }, Functions.toFunction(combiner), bufferSize());
    }

    /**
     * Combines five source {@link ObservableSource}s by emitting an item that aggregates the latest values of each of the
     * {@code ObservableSource}s each time an item is received from any of the {@code ObservableSource}s, where this
     * aggregation is defined by a specified function.
     * <p>
     * If any of the sources never produces an item but only terminates (normally or with an error), the
     * resulting sequence terminates immediately (normally or with all the errors accumulated till that point).
     * If that input source is also synchronous, other sources after it will not be subscribed to.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/combineLatest.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code combineLatest} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T1> the element type of the first source
     * @param <T2> the element type of the second source
     * @param <T3> the element type of the third source
     * @param <T4> the element type of the fourth source
     * @param <T5> the element type of the fifth source
     * @param <R> the combined output type
     * @param source1
     *            the first source {@code ObservableSource}
     * @param source2
     *            the second source {@code ObservableSource}
     * @param source3
     *            the third source {@code ObservableSource}
     * @param source4
     *            the fourth source {@code ObservableSource}
     * @param source5
     *            the fifth source {@code ObservableSource}
     * @param combiner
     *            the aggregation function used to combine the items emitted by the {@code ObservableSource}s
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code source1}, {@code source2}, {@code source3},
     *                              {@code source4}, {@code source5} or {@code combiner} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/combinelatest.html">ReactiveX operators documentation: CombineLatest</a>
     */
    @SuppressWarnings("unchecked")
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <@NonNull T1, @NonNull T2, @NonNull T3, @NonNull T4, @NonNull T5, @NonNull R> Observable<R> combineLatest(
            @NonNull ObservableSource<? extends T1> source1, @NonNull ObservableSource<? extends T2> source2,
            @NonNull ObservableSource<? extends T3> source3, @NonNull ObservableSource<? extends T4> source4,
            @NonNull ObservableSource<? extends T5> source5,
            @NonNull Function5<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? extends R> combiner) {
        Objects.requireNonNull(source1, "source1 is null");
        Objects.requireNonNull(source2, "source2 is null");
        Objects.requireNonNull(source3, "source3 is null");
        Objects.requireNonNull(source4, "source4 is null");
        Objects.requireNonNull(source5, "source5 is null");
        Objects.requireNonNull(combiner, "combiner is null");
        return combineLatestArray(new ObservableSource[] { source1, source2, source3, source4, source5 }, Functions.toFunction(combiner), bufferSize());
    }

    /**
     * Combines six source {@link ObservableSource}s by emitting an item that aggregates the latest values of each of the
     * {@code ObservableSource}s each time an item is received from any of the {@code ObservableSource}s, where this
     * aggregation is defined by a specified function.
     * <p>
     * If any of the sources never produces an item but only terminates (normally or with an error), the
     * resulting sequence terminates immediately (normally or with all the errors accumulated till that point).
     * If that input source is also synchronous, other sources after it will not be subscribed to.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/combineLatest.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code combineLatest} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T1> the element type of the first source
     * @param <T2> the element type of the second source
     * @param <T3> the element type of the third source
     * @param <T4> the element type of the fourth source
     * @param <T5> the element type of the fifth source
     * @param <T6> the element type of the sixth source
     * @param <R> the combined output type
     * @param source1
     *            the first source {@code ObservableSource}
     * @param source2
     *            the second source {@code ObservableSource}
     * @param source3
     *            the third source {@code ObservableSource}
     * @param source4
     *            the fourth source {@code ObservableSource}
     * @param source5
     *            the fifth source {@code ObservableSource}
     * @param source6
     *            the sixth source {@code ObservableSource}
     * @param combiner
     *            the aggregation function used to combine the items emitted by the {@code ObservableSource}s
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code source1}, {@code source2}, {@code source3},
     *                              {@code source4}, {@code source5}, {@code source6} or {@code combiner} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/combinelatest.html">ReactiveX operators documentation: CombineLatest</a>
     */
    @SuppressWarnings("unchecked")
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <@NonNull T1, @NonNull T2, @NonNull T3, @NonNull T4, @NonNull T5, @NonNull T6, @NonNull R> Observable<R> combineLatest(
            @NonNull ObservableSource<? extends T1> source1, @NonNull ObservableSource<? extends T2> source2,
            @NonNull ObservableSource<? extends T3> source3, @NonNull ObservableSource<? extends T4> source4,
            @NonNull ObservableSource<? extends T5> source5, @NonNull ObservableSource<? extends T6> source6,
            @NonNull Function6<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? extends R> combiner) {
        Objects.requireNonNull(source1, "source1 is null");
        Objects.requireNonNull(source2, "source2 is null");
        Objects.requireNonNull(source3, "source3 is null");
        Objects.requireNonNull(source4, "source4 is null");
        Objects.requireNonNull(source5, "source5 is null");
        Objects.requireNonNull(source6, "source6 is null");
        Objects.requireNonNull(combiner, "combiner is null");
        return combineLatestArray(new ObservableSource[] { source1, source2, source3, source4, source5, source6 }, Functions.toFunction(combiner), bufferSize());
    }

    /**
     * Combines seven source {@link ObservableSource}s by emitting an item that aggregates the latest values of each of the
     * {@code ObservableSource}s each time an item is received from any of the {@code ObservableSource}s, where this
     * aggregation is defined by a specified function.
     * <p>
     * If any of the sources never produces an item but only terminates (normally or with an error), the
     * resulting sequence terminates immediately (normally or with all the errors accumulated till that point).
     * If that input source is also synchronous, other sources after it will not be subscribed to.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/combineLatest.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code combineLatest} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T1> the element type of the first source
     * @param <T2> the element type of the second source
     * @param <T3> the element type of the third source
     * @param <T4> the element type of the fourth source
     * @param <T5> the element type of the fifth source
     * @param <T6> the element type of the sixth source
     * @param <T7> the element type of the seventh source
     * @param <R> the combined output type
     * @param source1
     *            the first source {@code ObservableSource}
     * @param source2
     *            the second source {@code ObservableSource}
     * @param source3
     *            the third source {@code ObservableSource}
     * @param source4
     *            the fourth source {@code ObservableSource}
     * @param source5
     *            the fifth source {@code ObservableSource}
     * @param source6
     *            the sixth source {@code ObservableSource}
     * @param source7
     *            the seventh source {@code ObservableSource}
     * @param combiner
     *            the aggregation function used to combine the items emitted by the {@code ObservableSource}s
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code source1}, {@code source2}, {@code source3},
     *                              {@code source4}, {@code source5}, {@code source6},
     *                              {@code source7} or {@code combiner} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/combinelatest.html">ReactiveX operators documentation: CombineLatest</a>
     */
    @SuppressWarnings("unchecked")
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <@NonNull T1, @NonNull T2, @NonNull T3, @NonNull T4, @NonNull T5, @NonNull T6, @NonNull T7, @NonNull R> Observable<R> combineLatest(
            @NonNull ObservableSource<? extends T1> source1, @NonNull ObservableSource<? extends T2> source2,
            @NonNull ObservableSource<? extends T3> source3, @NonNull ObservableSource<? extends T4> source4,
            @NonNull ObservableSource<? extends T5> source5, @NonNull ObservableSource<? extends T6> source6,
            @NonNull ObservableSource<? extends T7> source7,
            @NonNull Function7<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? extends R> combiner) {
        Objects.requireNonNull(source1, "source1 is null");
        Objects.requireNonNull(source2, "source2 is null");
        Objects.requireNonNull(source3, "source3 is null");
        Objects.requireNonNull(source4, "source4 is null");
        Objects.requireNonNull(source5, "source5 is null");
        Objects.requireNonNull(source6, "source6 is null");
        Objects.requireNonNull(source7, "source7 is null");
        Objects.requireNonNull(combiner, "combiner is null");
        return combineLatestArray(new ObservableSource[] { source1, source2, source3, source4, source5, source6, source7 }, Functions.toFunction(combiner), bufferSize());
    }

    /**
     * Combines eight source {@link ObservableSource}s by emitting an item that aggregates the latest values of each of the
     * {@code ObservableSource}s each time an item is received from any of the {@code ObservableSource}s, where this
     * aggregation is defined by a specified function.
     * <p>
     * If any of the sources never produces an item but only terminates (normally or with an error), the
     * resulting sequence terminates immediately (normally or with all the errors accumulated till that point).
     * If that input source is also synchronous, other sources after it will not be subscribed to.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/combineLatest.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code combineLatest} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T1> the element type of the first source
     * @param <T2> the element type of the second source
     * @param <T3> the element type of the third source
     * @param <T4> the element type of the fourth source
     * @param <T5> the element type of the fifth source
     * @param <T6> the element type of the sixth source
     * @param <T7> the element type of the seventh source
     * @param <T8> the element type of the eighth source
     * @param <R> the combined output type
     * @param source1
     *            the first source {@code ObservableSource}
     * @param source2
     *            the second source {@code ObservableSource}
     * @param source3
     *            the third source {@code ObservableSource}
     * @param source4
     *            the fourth source {@code ObservableSource}
     * @param source5
     *            the fifth source {@code ObservableSource}
     * @param source6
     *            the sixth source {@code ObservableSource}
     * @param source7
     *            the seventh source {@code ObservableSource}
     * @param source8
     *            the eighth source {@code ObservableSource}
     * @param combiner
     *            the aggregation function used to combine the items emitted by the {@code ObservableSource}s
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code source1}, {@code source2}, {@code source3},
     *                              {@code source4}, {@code source5}, {@code source6},
     *                              {@code source7}, {@code source8} or {@code combiner} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/combinelatest.html">ReactiveX operators documentation: CombineLatest</a>
     */
    @SuppressWarnings("unchecked")
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <@NonNull T1, @NonNull T2, @NonNull T3, @NonNull T4, @NonNull T5, @NonNull T6, @NonNull T7, @NonNull T8, @NonNull R> Observable<R> combineLatest(
            @NonNull ObservableSource<? extends T1> source1, @NonNull ObservableSource<? extends T2> source2,
            @NonNull ObservableSource<? extends T3> source3, @NonNull ObservableSource<? extends T4> source4,
            @NonNull ObservableSource<? extends T5> source5, @NonNull ObservableSource<? extends T6> source6,
            @NonNull ObservableSource<? extends T7> source7, @NonNull ObservableSource<? extends T8> source8,
            @NonNull Function8<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? super T8, ? extends R> combiner) {
        Objects.requireNonNull(source1, "source1 is null");
        Objects.requireNonNull(source2, "source2 is null");
        Objects.requireNonNull(source3, "source3 is null");
        Objects.requireNonNull(source4, "source4 is null");
        Objects.requireNonNull(source5, "source5 is null");
        Objects.requireNonNull(source6, "source6 is null");
        Objects.requireNonNull(source7, "source7 is null");
        Objects.requireNonNull(source8, "source8 is null");
        Objects.requireNonNull(combiner, "combiner is null");
        return combineLatestArray(new ObservableSource[] { source1, source2, source3, source4, source5, source6, source7, source8 }, Functions.toFunction(combiner), bufferSize());
    }

    /**
     * Combines nine source {@link ObservableSource}s by emitting an item that aggregates the latest values of each of the
     * {@code ObservableSource}s each time an item is received from any of the {@code ObservableSource}s, where this
     * aggregation is defined by a specified function.
     * <p>
     * If any of the sources never produces an item but only terminates (normally or with an error), the
     * resulting sequence terminates immediately (normally or with all the errors accumulated till that point).
     * If that input source is also synchronous, other sources after it will not be subscribed to.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/combineLatest.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code combineLatest} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T1> the element type of the first source
     * @param <T2> the element type of the second source
     * @param <T3> the element type of the third source
     * @param <T4> the element type of the fourth source
     * @param <T5> the element type of the fifth source
     * @param <T6> the element type of the sixth source
     * @param <T7> the element type of the seventh source
     * @param <T8> the element type of the eighth source
     * @param <T9> the element type of the ninth source
     * @param <R> the combined output type
     * @param source1
     *            the first source {@code ObservableSource}
     * @param source2
     *            the second source {@code ObservableSource}
     * @param source3
     *            the third source {@code ObservableSource}
     * @param source4
     *            the fourth source {@code ObservableSource}
     * @param source5
     *            the fifth source {@code ObservableSource}
     * @param source6
     *            the sixth source {@code ObservableSource}
     * @param source7
     *            the seventh source {@code ObservableSource}
     * @param source8
     *            the eighth source {@code ObservableSource}
     * @param source9
     *            the ninth source {@code ObservableSource}
     * @param combiner
     *            the aggregation function used to combine the items emitted by the {@code ObservableSource}s
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code source1}, {@code source2}, {@code source3},
     *                              {@code source4}, {@code source5}, {@code source6},
     *                              {@code source7}, {@code source8}, {@code source9} or {@code combiner} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/combinelatest.html">ReactiveX operators documentation: CombineLatest</a>
     */
    @SuppressWarnings("unchecked")
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <@NonNull T1, @NonNull T2, @NonNull T3, @NonNull T4, @NonNull T5, @NonNull T6, @NonNull T7, @NonNull T8, @NonNull T9, @NonNull R> Observable<R> combineLatest(
            @NonNull ObservableSource<? extends T1> source1, @NonNull ObservableSource<? extends T2> source2,
            @NonNull ObservableSource<? extends T3> source3, @NonNull ObservableSource<? extends T4> source4,
            @NonNull ObservableSource<? extends T5> source5, @NonNull ObservableSource<? extends T6> source6,
            @NonNull ObservableSource<? extends T7> source7, @NonNull ObservableSource<? extends T8> source8,
            @NonNull ObservableSource<? extends T9> source9,
            @NonNull Function9<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? super T8, ? super T9, ? extends R> combiner) {
        Objects.requireNonNull(source1, "source1 is null");
        Objects.requireNonNull(source2, "source2 is null");
        Objects.requireNonNull(source3, "source3 is null");
        Objects.requireNonNull(source4, "source4 is null");
        Objects.requireNonNull(source5, "source5 is null");
        Objects.requireNonNull(source6, "source6 is null");
        Objects.requireNonNull(source7, "source7 is null");
        Objects.requireNonNull(source8, "source8 is null");
        Objects.requireNonNull(source9, "source9 is null");
        Objects.requireNonNull(combiner, "combiner is null");
        return combineLatestArray(new ObservableSource[] { source1, source2, source3, source4, source5, source6, source7, source8, source9 }, Functions.toFunction(combiner), bufferSize());
    }

    /**
     * Combines an array of {@link ObservableSource}s by emitting an item that aggregates the latest values of each of
     * the {@code ObservableSource}s each time an item is received from any of the {@code ObservableSource}s, where this
     * aggregation is defined by a specified function.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/combineLatestDelayError.v3.png" alt="">
     * <p>
     * Note on method signature: since Java doesn't allow creating a generic array with {@code new T[]}, the
     * implementation of this operator has to create an {@code Object[]} instead. Unfortunately, a
     * {@code Function<Integer[], R>} passed to the method would trigger a {@link ClassCastException}.
     * <p>
     * If any of the sources never produces an item but only terminates (normally or with an error), the
     * resulting sequence terminates immediately (normally or with all the errors accumulated till that point).
     * If that input source is also synchronous, other sources after it will not be subscribed to.
     * <p>
     * If the provided array of {@code ObservableSource}s is empty, the resulting sequence completes immediately without emitting
     * any items and without any calls to the combiner function.
     *
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code combineLatestArrayDelayError} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T>
     *            the common base type of source values
     * @param <R>
     *            the result type
     * @param sources
     *            the collection of source {@code ObservableSource}s
     * @param combiner
     *            the aggregation function used to combine the items emitted by the {@code ObservableSource}s
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code sources} or {@code combiner} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/combinelatest.html">ReactiveX operators documentation: CombineLatest</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public static <@NonNull T, @NonNull R> Observable<R> combineLatestArrayDelayError(
            @NonNull ObservableSource<? extends T>[] sources,
            @NonNull Function<? super Object[], ? extends R> combiner) {
        return combineLatestArrayDelayError(sources, combiner, bufferSize());
    }

    /**
     * Combines an array of {@link ObservableSource}s by emitting an item that aggregates the latest values of each of
     * the {@code ObservableSource}s each time an item is received from any of the {@code ObservableSource}s, where this
     * aggregation is defined by a specified function and delays any error from the sources until
     * all source {@code ObservableSource}s terminate.
     * <p>
     * Note on method signature: since Java doesn't allow creating a generic array with {@code new T[]}, the
     * implementation of this operator has to create an {@code Object[]} instead. Unfortunately, a
     * {@code Function<Integer[], R>} passed to the method would trigger a {@link ClassCastException}.
     * <p>
     * If any of the sources never produces an item but only terminates (normally or with an error), the
     * resulting sequence terminates immediately (normally or with all the errors accumulated till that point).
     * If that input source is also synchronous, other sources after it will not be subscribed to.
     * <p>
     * If the provided array of {@code ObservableSource}s is empty, the resulting sequence completes immediately without emitting
     * any items and without any calls to the combiner function.
     *
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/combineLatestDelayError.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code combineLatestArrayDelayError} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T>
     *            the common base type of source values
     * @param <R>
     *            the result type
     * @param sources
     *            the collection of source {@code ObservableSource}s
     * @param combiner
     *            the aggregation function used to combine the items emitted by the {@code ObservableSource}s
     * @param bufferSize
     *            the expected number of row combination items to be buffered internally
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code sources} or {@code combiner} is {@code null}
     * @throws IllegalArgumentException if {@code bufferSize} is non-positive
     * @see <a href="http://reactivex.io/documentation/operators/combinelatest.html">ReactiveX operators documentation: CombineLatest</a>
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <@NonNull T, @NonNull R> Observable<R> combineLatestArrayDelayError(@NonNull ObservableSource<? extends T>[] sources,
            @NonNull Function<? super Object[], ? extends R> combiner, int bufferSize) {
        Objects.requireNonNull(sources, "sources is null");
        Objects.requireNonNull(combiner, "combiner is null");
        ObjectHelper.verifyPositive(bufferSize, "bufferSize");
        if (sources.length == 0) {
            return empty();
        }
        // the queue holds a pair of values so we need to double the capacity
        int s = bufferSize << 1;
        return RxJavaPlugins.onAssembly(new ObservableCombineLatest<>(sources, null, combiner, s, true));
    }

    /**
     * Combines an {@link Iterable} of {@link ObservableSource}s by emitting an item that aggregates the latest values of each of
     * the {@code ObservableSource}s each time an item is received from any of the {@code ObservableSource}s, where this
     * aggregation is defined by a specified function and delays any error from the sources until
     * all source {@code ObservableSource}s terminate.
     * <p>
     * Note on method signature: since Java doesn't allow creating a generic array with {@code new T[]}, the
     * implementation of this operator has to create an {@code Object[]} instead. Unfortunately, a
     * {@code Function<Integer[], R>} passed to the method would trigger a {@link ClassCastException}.
     * <p>
     * If any of the sources never produces an item but only terminates (normally or with an error), the
     * resulting sequence terminates immediately (normally or with all the errors accumulated till that point).
     * If that input source is also synchronous, other sources after it will not be subscribed to.
     * <p>
     * If the provided iterable of {@code ObservableSource}s is empty, the resulting sequence completes immediately without emitting
     * any items and without any calls to the combiner function.
     *
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/combineLatestDelayError.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code combineLatestDelayError} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T>
     *            the common base type of source values
     * @param <R>
     *            the result type
     * @param sources
     *            the {@code Iterable} of source {@code ObservableSource}s
     * @param combiner
     *            the aggregation function used to combine the items emitted by the {@code ObservableSource}s
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code sources} or {@code combiner} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/combinelatest.html">ReactiveX operators documentation: CombineLatest</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public static <@NonNull T, @NonNull R> Observable<R> combineLatestDelayError(@NonNull Iterable<@NonNull ? extends ObservableSource<? extends T>> sources,
            @NonNull Function<? super Object[], ? extends R> combiner) {
        return combineLatestDelayError(sources, combiner, bufferSize());
    }

    /**
     * Combines an {@link Iterable} of {@link ObservableSource}s by emitting an item that aggregates the latest values of each of
     * the {@code ObservableSource}s each time an item is received from any of the {@code ObservableSource}s, where this
     * aggregation is defined by a specified function and delays any error from the sources until
     * all source {@code ObservableSource}s terminate.
     * <p>
     * Note on method signature: since Java doesn't allow creating a generic array with {@code new T[]}, the
     * implementation of this operator has to create an {@code Object[]} instead. Unfortunately, a
     * {@code Function<Integer[], R>} passed to the method would trigger a {@link ClassCastException}.
     * <p>
     * If any of the sources never produces an item but only terminates (normally or with an error), the
     * resulting sequence terminates immediately (normally or with all the errors accumulated till that point).
     * If that input source is also synchronous, other sources after it will not be subscribed to.
     * <p>
     * If the provided iterable of {@code ObservableSource}s is empty, the resulting sequence completes immediately without emitting
     * any items and without any calls to the combiner function.
     *
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/combineLatestDelayError.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code combineLatestDelayError} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T>
     *            the common base type of source values
     * @param <R>
     *            the result type
     * @param sources
     *            the collection of source {@code ObservableSource}s
     * @param combiner
     *            the aggregation function used to combine the items emitted by the {@code ObservableSource}s
     * @param bufferSize
     *            the expected number of row combination items to be buffered internally
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code sources} or {@code combiner} is {@code null}
     * @throws IllegalArgumentException if {@code bufferSize} is non-positive
     * @see <a href="http://reactivex.io/documentation/operators/combinelatest.html">ReactiveX operators documentation: CombineLatest</a>
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <@NonNull T, @NonNull R> Observable<R> combineLatestDelayError(@NonNull Iterable<@NonNull ? extends ObservableSource<? extends T>> sources,
            @NonNull Function<? super Object[], ? extends R> combiner, int bufferSize) {
        Objects.requireNonNull(sources, "sources is null");
        Objects.requireNonNull(combiner, "combiner is null");
        ObjectHelper.verifyPositive(bufferSize, "bufferSize");

        // the queue holds a pair of values so we need to double the capacity
        int s = bufferSize << 1;
        return RxJavaPlugins.onAssembly(new ObservableCombineLatest<>(null, sources, combiner, s, true));
    }

    /**
     * Concatenates elements of each {@link ObservableSource} provided via an {@link Iterable} sequence into a single sequence
     * of elements without interleaving them.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/concat.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code concat} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the common value type of the sources
     * @param sources the {@code Iterable} sequence of {@code ObservableSource}s
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code sources} is {@code null}
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <@NonNull T> Observable<T> concat(@NonNull Iterable<@NonNull ? extends ObservableSource<? extends T>> sources) {
        Objects.requireNonNull(sources, "sources is null");
        return fromIterable(sources).concatMapDelayError((Function)Functions.identity(), false, bufferSize());
    }

    /**
     * Returns an {@code Observable} that emits the items emitted by each of the {@link ObservableSource}s emitted by the
     * {@code ObservableSource}, one after the other, without interleaving them.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/concat.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code concat} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T> the common element base type
     * @param sources
     *            an {@code ObservableSource} that emits {@code ObservableSource}s
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code sources} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/concat.html">ReactiveX operators documentation: Concat</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public static <@NonNull T> Observable<T> concat(@NonNull ObservableSource<? extends ObservableSource<? extends T>> sources) {
        return concat(sources, bufferSize());
    }

    /**
     * Returns an {@code Observable} that emits the items emitted by each of the {@link ObservableSource}s emitted by the outer
     * {@code ObservableSource}, one after the other, without interleaving them.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/concat.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code concat} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T> the common element base type
     * @param sources
     *            an {@code ObservableSource} that emits {@code ObservableSource}s
     * @param bufferSize
     *            the number of inner {@code ObservableSource}s expected to be buffered.
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code sources} is {@code null}
     * @throws IllegalArgumentException if {@code bufferSize} is non-positive
     * @see <a href="http://reactivex.io/documentation/operators/concat.html">ReactiveX operators documentation: Concat</a>
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <@NonNull T> Observable<T> concat(@NonNull ObservableSource<? extends ObservableSource<? extends T>> sources, int bufferSize) {
        Objects.requireNonNull(sources, "sources is null");
        ObjectHelper.verifyPositive(bufferSize, "bufferSize");
        return RxJavaPlugins.onAssembly(new ObservableConcatMap(sources, Functions.identity(), bufferSize, ErrorMode.IMMEDIATE));
    }

    /**
     * Returns an {@code Observable} that emits the items emitted by two {@link ObservableSource}s, one after the other, without
     * interleaving them.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/concat.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code concat} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T> the common element base type
     * @param source1
     *            an {@code ObservableSource} to be concatenated
     * @param source2
     *            an {@code ObservableSource} to be concatenated
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code source1} or {@code source2} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/concat.html">ReactiveX operators documentation: Concat</a>
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <@NonNull T> Observable<T> concat(@NonNull ObservableSource<? extends T> source1, ObservableSource<? extends T> source2) {
        Objects.requireNonNull(source1, "source1 is null");
        Objects.requireNonNull(source2, "source2 is null");
        return concatArray(source1, source2);
    }

    /**
     * Returns an {@code Observable} that emits the items emitted by three {@link ObservableSource}s, one after the other, without
     * interleaving them.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/concat.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code concat} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T> the common element base type
     * @param source1
     *            an {@code ObservableSource} to be concatenated
     * @param source2
     *            an {@code ObservableSource} to be concatenated
     * @param source3
     *            an {@code ObservableSource} to be concatenated
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code source1}, {@code source2} or {@code source3} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/concat.html">ReactiveX operators documentation: Concat</a>
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <@NonNull T> Observable<T> concat(
            @NonNull ObservableSource<? extends T> source1, @NonNull ObservableSource<? extends T> source2,
            @NonNull ObservableSource<? extends T> source3) {
        Objects.requireNonNull(source1, "source1 is null");
        Objects.requireNonNull(source2, "source2 is null");
        Objects.requireNonNull(source3, "source3 is null");
        return concatArray(source1, source2, source3);
    }

    /**
     * Returns an {@code Observable} that emits the items emitted by four {@link ObservableSource}s, one after the other, without
     * interleaving them.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/concat.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code concat} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T> the common element base type
     * @param source1
     *            an {@code ObservableSource} to be concatenated
     * @param source2
     *            an {@code ObservableSource} to be concatenated
     * @param source3
     *            an {@code ObservableSource} to be concatenated
     * @param source4
     *            an {@code ObservableSource} to be concatenated
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code source1}, {@code source2}, {@code source3} or {@code source4} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/concat.html">ReactiveX operators documentation: Concat</a>
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <@NonNull T> Observable<T> concat(
            @NonNull ObservableSource<? extends T> source1, @NonNull ObservableSource<? extends T> source2,
            @NonNull ObservableSource<? extends T> source3, @NonNull ObservableSource<? extends T> source4) {
        Objects.requireNonNull(source1, "source1 is null");
        Objects.requireNonNull(source2, "source2 is null");
        Objects.requireNonNull(source3, "source3 is null");
        Objects.requireNonNull(source4, "source4 is null");
        return concatArray(source1, source2, source3, source4);
    }

    /**
     * Concatenates a variable number of {@link ObservableSource} sources.
     * <p>
     * Note: named this way because of overload conflict with {@code concat(ObservableSource<ObservableSource>)}
     * <p>
     * <img width="640" height="290" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/concatArray.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code concatArray} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param sources the array of sources
     * @param <T> the common base value type
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code sources} is {@code null}
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    @SafeVarargs
    public static <@NonNull T> Observable<T> concatArray(@NonNull ObservableSource<? extends T>... sources) {
        Objects.requireNonNull(sources, "sources is null");
        if (sources.length == 0) {
            return empty();
        }
        if (sources.length == 1) {
            return wrap((ObservableSource<T>)sources[0]);
        }
        return RxJavaPlugins.onAssembly(new ObservableConcatMap(fromArray(sources), Functions.identity(), bufferSize(), ErrorMode.BOUNDARY));
    }

    /**
     * Concatenates a variable number of {@link ObservableSource} sources and delays errors from any of them
     * till all terminate.
     * <p>
     * <img width="640" height="290" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/concatArray.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code concatArrayDelayError} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param sources the array of sources
     * @param <T> the common base value type
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code sources} is {@code null}
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    @SafeVarargs
    public static <@NonNull T> Observable<T> concatArrayDelayError(@NonNull ObservableSource<? extends T>... sources) {
        Objects.requireNonNull(sources, "sources is null");
        if (sources.length == 0) {
            return empty();
        }
        if (sources.length == 1) {
            @SuppressWarnings("unchecked")
            Observable<T> source = (Observable<T>)wrap(sources[0]);
            return source;
        }
        return concatDelayError(fromArray(sources));
    }

    /**
     * Concatenates an array of {@link ObservableSource}s eagerly into a single stream of values.
     * <p>
     * <img width="640" height="411" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/concatArrayEager.png" alt="">
     * <p>
     * Eager concatenation means that once a subscriber subscribes, this operator subscribes to all of the
     * {@code ObservableSource}s. The operator buffers the values emitted by these {@code ObservableSource}s and then drains them
     * in order, each one after the previous one completes.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This method does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the value type
     * @param sources an array of {@code ObservableSource}s that need to be eagerly concatenated
     * @return the new {@code Observable} instance with the specified concatenation behavior
     * @throws NullPointerException if {@code sources} is {@code null}
     * @since 2.0
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @SafeVarargs
    @NonNull
    public static <@NonNull T> Observable<T> concatArrayEager(@NonNull ObservableSource<? extends T>... sources) {
        return concatArrayEager(bufferSize(), bufferSize(), sources);
    }

    /**
     * Concatenates an array of {@link ObservableSource}s eagerly into a single stream of values.
     * <p>
     * <img width="640" height="495" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/concatArrayEager.nn.png" alt="">
     * <p>
     * Eager concatenation means that once a subscriber subscribes, this operator subscribes to all of the
     * {@code ObservableSource}s. The operator buffers the values emitted by these {@code ObservableSource}s and then drains them
     * in order, each one after the previous one completes.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This method does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the value type
     * @param sources an array of {@code ObservableSource}s that need to be eagerly concatenated
     * @param maxConcurrency the maximum number of concurrent subscriptions at a time, {@link Integer#MAX_VALUE}
     *                       is interpreted as indication to subscribe to all sources at once
     * @param bufferSize the number of elements expected from each {@code ObservableSource} to be buffered
     * @return the new {@code Observable} instance with the specified concatenation behavior
     * @throws NullPointerException if {@code sources} is {@code null}
     * @throws IllegalArgumentException if {@code maxConcurrency} or {@code bufferSize} is non-positive
     * @since 2.0
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    @SafeVarargs
    public static <@NonNull T> Observable<T> concatArrayEager(int maxConcurrency, int bufferSize, @NonNull ObservableSource<? extends T>... sources) {
        return fromArray(sources).concatMapEagerDelayError((Function)Functions.identity(), false, maxConcurrency, bufferSize);
    }

    /**
     * Concatenates an array of {@link ObservableSource}s eagerly into a single stream of values
     * and delaying any errors until all sources terminate.
     * <p>
     * <img width="640" height="354" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/concatArrayEagerDelayError.png" alt="">
     * <p>
     * Eager concatenation means that once a subscriber subscribes, this operator subscribes to all of the
     * {@code ObservableSource}s. The operator buffers the values emitted by these {@code ObservableSource}s
     * and then drains them in order, each one after the previous one completes.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This method does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the value type
     * @param sources an array of {@code ObservableSource}s that need to be eagerly concatenated
     * @return the new {@code Observable} instance with the specified concatenation behavior
     * @throws NullPointerException if {@code sources} is {@code null}
     * @since 2.2.1 - experimental
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @SafeVarargs
    @NonNull
    public static <@NonNull T> Observable<T> concatArrayEagerDelayError(@NonNull ObservableSource<? extends T>... sources) {
        return concatArrayEagerDelayError(bufferSize(), bufferSize(), sources);
    }

    /**
     * Concatenates an array of {@link ObservableSource}s eagerly into a single stream of values
     * and delaying any errors until all sources terminate.
     * <p>
     * <img width="640" height="460" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/concatArrayEagerDelayError.nn.png" alt="">
     * <p>
     * Eager concatenation means that once a subscriber subscribes, this operator subscribes to all of the
     * {@code ObservableSource}s. The operator buffers the values emitted by these {@code ObservableSource}s
     * and then drains them in order, each one after the previous one completes.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This method does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the value type
     * @param sources an array of {@code ObservableSource}s that need to be eagerly concatenated
     * @param maxConcurrency the maximum number of concurrent subscriptions at a time, {@link Integer#MAX_VALUE}
     *                       is interpreted as indication to subscribe to all sources at once
     * @param bufferSize the number of elements expected from each {@code ObservableSource} to be buffered
     * @return the new {@code Observable} instance with the specified concatenation behavior
     * @throws NullPointerException if {@code sources} is {@code null}
     * @throws IllegalArgumentException if {@code maxConcurrency} or {@code bufferSize} is non-positive
     * @since 2.2.1 - experimental
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    @SafeVarargs
    public static <@NonNull T> Observable<T> concatArrayEagerDelayError(int maxConcurrency, int bufferSize, @NonNull ObservableSource<? extends T>... sources) {
        return fromArray(sources).concatMapEagerDelayError((Function)Functions.identity(), true, maxConcurrency, bufferSize);
    }

    /**
     * Concatenates the {@link Iterable} sequence of {@link ObservableSource}s into a single {@code Observable} sequence
     *  by subscribing to each {@code ObservableSource}, one after the other, one at a time and delays any errors till
     *  the all inner {@code ObservableSource}s terminate.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/concatDelayError.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code concatDelayError} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T> the common element base type
     * @param sources the {@code Iterable} sequence of {@code ObservableSource}s
     * @return the new {@code Observable} with the concatenating behavior
     * @throws NullPointerException if {@code sources} is {@code null}
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <@NonNull T> Observable<T> concatDelayError(@NonNull Iterable<@NonNull ? extends ObservableSource<? extends T>> sources) {
        Objects.requireNonNull(sources, "sources is null");
        return concatDelayError(fromIterable(sources));
    }

    /**
     * Concatenates the {@link ObservableSource} sequence of {@code ObservableSource}s into a single {@code Observable} sequence
     * by subscribing to each inner {@code ObservableSource}, one after the other, one at a time and delays any errors till the
     *  all inner and the outer {@code ObservableSource}s terminate.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/concatDelayError.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code concatDelayError} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T> the common element base type
     * @param sources the {@code ObservableSource} sequence of {@code ObservableSource}s
     * @return the new {@code Observable} with the concatenating behavior
     * @throws NullPointerException if {@code sources} is {@code null}
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public static <@NonNull T> Observable<T> concatDelayError(@NonNull ObservableSource<? extends ObservableSource<? extends T>> sources) {
        return concatDelayError(sources, bufferSize(), true);
    }

    /**
     * Concatenates the {@link ObservableSource} sequence of {@code ObservableSource}s into a single sequence by subscribing to each inner {@code ObservableSource},
     * one after the other, one at a time and delays any errors till the all inner and the outer {@code ObservableSource}s terminate.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/concatDelayError.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code concatDelayError} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T> the common element base type
     * @param sources the {@code ObservableSource} sequence of {@code ObservableSource}s
     * @param bufferSize the number of inner {@code ObservableSource}s expected to be buffered
     * @param tillTheEnd if {@code true}, exceptions from the outer and all inner {@code ObservableSource}s are delayed to the end
     *                   if {@code false}, exception from the outer {@code ObservableSource} is delayed till the active {@code ObservableSource} terminates
     * @return the new {@code Observable} with the concatenating behavior
     * @throws NullPointerException if {@code sources} is {@code null}
     * @throws IllegalArgumentException if {@code bufferSize} is non-positive
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <@NonNull T> Observable<T> concatDelayError(@NonNull ObservableSource<? extends ObservableSource<? extends T>> sources, int bufferSize, boolean tillTheEnd) {
        Objects.requireNonNull(sources, "sources is null");
        ObjectHelper.verifyPositive(bufferSize, "bufferSize is null");
        return RxJavaPlugins.onAssembly(new ObservableConcatMap(sources, Functions.identity(), bufferSize, tillTheEnd ? ErrorMode.END : ErrorMode.BOUNDARY));
    }

    /**
     * Concatenates a sequence of {@link ObservableSource}s eagerly into a single stream of values.
     * <p>
     * <img width="640" height="422" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Observable.concatEager.i.png" alt="">
     * <p>
     * Eager concatenation means that once a subscriber subscribes, this operator subscribes to all of the
     * {@code ObservableSource}s. The operator buffers the values emitted by these {@code ObservableSource}s and then drains them
     * in order, each one after the previous one completes.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This method does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the value type
     * @param sources a sequence of {@code ObservableSource}s that need to be eagerly concatenated
     * @return the new {@code Observable} instance with the specified concatenation behavior
     * @throws NullPointerException if {@code sources} is {@code null}
     * @since 2.0
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public static <@NonNull T> Observable<T> concatEager(@NonNull Iterable<@NonNull ? extends ObservableSource<? extends T>> sources) {
        return concatEager(sources, bufferSize(), bufferSize());
    }

    /**
     * Concatenates a sequence of {@link ObservableSource}s eagerly into a single stream of values and
     * runs a limited number of inner sequences at once.
     * <p>
     * <img width="640" height="379" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Observable.concatEager.in.png" alt="">
     * <p>
     * Eager concatenation means that once a subscriber subscribes, this operator subscribes to all of the
     * {@code ObservableSource}s. The operator buffers the values emitted by these {@code ObservableSource}s and then drains them
     * in order, each one after the previous one completes.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This method does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the value type
     * @param sources a sequence of {@code ObservableSource}s that need to be eagerly concatenated
     * @param maxConcurrency the maximum number of concurrently running inner {@code ObservableSource}s; {@link Integer#MAX_VALUE}
     *                       is interpreted as all inner {@code ObservableSource}s can be active at the same time
     * @param bufferSize the number of elements expected from each inner {@code ObservableSource} to be buffered
     * @return the new {@code Observable} instance with the specified concatenation behavior
     * @throws NullPointerException if {@code sources} is {@code null}
     * @throws IllegalArgumentException if {@code maxConcurrency} or {@code bufferSize} is non-positive
     * @since 2.0
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public static <@NonNull T> Observable<T> concatEager(@NonNull Iterable<@NonNull ? extends ObservableSource<? extends T>> sources, int maxConcurrency, int bufferSize) {
        return fromIterable(sources).concatMapEagerDelayError((Function)Functions.identity(), false, maxConcurrency, bufferSize);
    }

    /**
     * Concatenates an {@link ObservableSource} sequence of {@code ObservableSource}s eagerly into a single stream of values.
     * <p>
     * <img width="640" height="495" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Observable.concatEager.o.png" alt="">
     * <p>
     * Eager concatenation means that once a subscriber subscribes, this operator subscribes to all of the
     * emitted source {@code ObservableSource}s as they are observed. The operator buffers the values emitted by these
     * {@code ObservableSource}s and then drains them in order, each one after the previous one completes.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This method does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the value type
     * @param sources a sequence of {@code ObservableSource}s that need to be eagerly concatenated
     * @return the new {@code Observable} instance with the specified concatenation behavior
     * @throws NullPointerException if {@code sources} is {@code null}
     * @since 2.0
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public static <@NonNull T> Observable<T> concatEager(@NonNull ObservableSource<? extends ObservableSource<? extends T>> sources) {
        return concatEager(sources, bufferSize(), bufferSize());
    }

    /**
     * Concatenates an {@link ObservableSource} sequence of {@code ObservableSource}s eagerly into a single stream of values
     * and runs a limited number of inner sequences at once.
     * 
     * <p>
     * <img width="640" height="442" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Observable.concatEager.on.png" alt="">
     * <p>
     * Eager concatenation means that once a subscriber subscribes, this operator subscribes to all of the
     * emitted source {@code ObservableSource}s as they are observed. The operator buffers the values emitted by these
     * {@code ObservableSource}s and then drains them in order, each one after the previous one completes.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This method does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the value type
     * @param sources a sequence of {@code ObservableSource}s that need to be eagerly concatenated
     * @param maxConcurrency the maximum number of concurrently running inner {@code ObservableSource}s; {@link Integer#MAX_VALUE}
     *                       is interpreted as all inner {@code ObservableSource}s can be active at the same time
     * @param bufferSize the number of inner {@code ObservableSource} expected to be buffered
     * @return the new {@code Observable} instance with the specified concatenation behavior
     * @throws NullPointerException if {@code sources} is {@code null}
     * @throws IllegalArgumentException if {@code maxConcurrency} or {@code bufferSize} is non-positive
     * @since 2.0
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public static <@NonNull T> Observable<T> concatEager(@NonNull ObservableSource<? extends ObservableSource<? extends T>> sources, int maxConcurrency, int bufferSize) {
        return wrap(sources).concatMapEager((Function)Functions.identity(), maxConcurrency, bufferSize);
    }

    /**
     * Concatenates a sequence of {@link ObservableSource}s eagerly into a single stream of values,
     * delaying errors until all the inner sequences terminate.
     * <p>
     * <img width="640" height="428" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Observable.concatEagerDelayError.i.png" alt="">
     * <p>
     * Eager concatenation means that once a subscriber subscribes, this operator subscribes to all of the
     * {@code ObservableSource}s. The operator buffers the values emitted by these {@code ObservableSource}s and then drains them
     * in order, each one after the previous one completes.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This method does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the value type
     * @param sources a sequence of {@code ObservableSource}s that need to be eagerly concatenated
     * @return the new {@code Observable} instance with the specified concatenation behavior
     * @throws NullPointerException if {@code sources} is {@code null}
     * @since 3.0.0
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public static <@NonNull T> Observable<T> concatEagerDelayError(@NonNull Iterable<@NonNull ? extends ObservableSource<? extends T>> sources) {
        return concatEagerDelayError(sources, bufferSize(), bufferSize());
    }

    /**
     * Concatenates a sequence of {@link ObservableSource}s eagerly into a single stream of values,
     * delaying errors until all the inner sequences terminate and runs a limited number of inner
     * sequences at once.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Observable.concatEagerDelayError.in.png" alt="">
     * <p>
     * Eager concatenation means that once a subscriber subscribes, this operator subscribes to all of the
     * {@code ObservableSource}s. The operator buffers the values emitted by these {@code ObservableSource}s and then drains them
     * in order, each one after the previous one completes.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This method does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the value type
     * @param sources a sequence of {@code ObservableSource}s that need to be eagerly concatenated
     * @param maxConcurrency the maximum number of concurrently running inner {@code ObservableSource}s; {@link Integer#MAX_VALUE}
     *                       is interpreted as all inner {@code ObservableSource}s can be active at the same time
     * @param bufferSize the number of elements expected from each inner {@code ObservableSource} to be buffered
     * @return the new {@code Observable} instance with the specified concatenation behavior
     * @throws NullPointerException if {@code sources} is {@code null}
     * @throws IllegalArgumentException if {@code maxConcurrency} or {@code bufferSize} is non-positive
     * @since 3.0.0
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public static <@NonNull T> Observable<T> concatEagerDelayError(@NonNull Iterable<@NonNull ? extends ObservableSource<? extends T>> sources, int maxConcurrency, int bufferSize) {
        return fromIterable(sources).concatMapEagerDelayError((Function)Functions.identity(), true, maxConcurrency, bufferSize);
    }

    /**
     * Concatenates an {@link ObservableSource} sequence of {@code ObservableSource}s eagerly into a single stream of values,
     * delaying errors until all the inner and the outer sequence terminate.
     * <p>
     * <img width="640" height="496" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Observable.concatEagerDelayError.o.png" alt="">
     * <p>
     * Eager concatenation means that once a subscriber subscribes, this operator subscribes to all of the
     * emitted source {@code ObservableSource}s as they are observed. The operator buffers the values emitted by these
     * {@code ObservableSource}s and then drains them in order, each one after the previous one completes.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This method does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the value type
     * @param sources a sequence of {@code ObservableSource}s that need to be eagerly concatenated
     * @return the new {@code Observable} instance with the specified concatenation behavior
     * @throws NullPointerException if {@code sources} is {@code null}
     * @since 3.0.0
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public static <@NonNull T> Observable<T> concatEagerDelayError(@NonNull ObservableSource<? extends ObservableSource<? extends T>> sources) {
        return concatEagerDelayError(sources, bufferSize(), bufferSize());
    }

    /**
     * Concatenates an {@link ObservableSource} sequence of {@code ObservableSource}s eagerly into a single stream of values,
     * delaying errors until all the inner and the outer sequence terminate and runs a limited number of inner sequences at once.
     * <p>
     * <img width="640" height="421" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Observable.concatEagerDelayError.on.png" alt="">
     * <p>
     * Eager concatenation means that once a subscriber subscribes, this operator subscribes to all of the
     * emitted source {@code ObservableSource}s as they are observed. The operator buffers the values emitted by these
     * {@code ObservableSource}s and then drains them in order, each one after the previous one completes.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This method does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the value type
     * @param sources a sequence of {@code ObservableSource}s that need to be eagerly concatenated
     * @param maxConcurrency the maximum number of concurrently running inner {@code ObservableSource}s; {@link Integer#MAX_VALUE}
     *                       is interpreted as all inner {@code ObservableSource}s can be active at the same time
     * @param bufferSize the number of inner {@code ObservableSource} expected to be buffered
     * @return the new {@code Observable} instance with the specified concatenation behavior
     * @throws NullPointerException if {@code sources} is {@code null}
     * @throws IllegalArgumentException if {@code maxConcurrency} or {@code bufferSize} is non-positive
     * @since 3.0.0
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public static <@NonNull T> Observable<T> concatEagerDelayError(@NonNull ObservableSource<? extends ObservableSource<? extends T>> sources, int maxConcurrency, int bufferSize) {
        return wrap(sources).concatMapEagerDelayError((Function)Functions.identity(), true, maxConcurrency, bufferSize);
    }

    /**
     * Provides an API (via a cold {@code Observable}) that bridges the reactive world with the callback-style world.
     * <p>
     * Example:
     * <pre><code>
     * Observable.&lt;Event&gt;create(emitter -&gt; {
     *     Callback listener = new Callback() {
     *         &#64;Override
     *         public void onEvent(Event e) {
     *             emitter.onNext(e);
     *             if (e.isLast()) {
     *                 emitter.onComplete();
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
     * Whenever an {@link Observer} subscribes to the returned {@code Observable}, the provided
     * {@link ObservableOnSubscribe} callback is invoked with a fresh instance of an {@link ObservableEmitter}
     * that will interact only with that specific {@code Observer}. If this {@code Observer}
     * disposes the flow (making {@link ObservableEmitter#isDisposed} return {@code true}),
     * other observers subscribed to the same returned {@code Observable} are not affected.
     * <p>
     * <img width="640" height="200" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/create.v3.png" alt="">
     * <p>
     * You should call the {@code ObservableEmitter}'s {@code onNext}, {@code onError} and {@code onComplete} methods in a serialized fashion. The
     * rest of its methods are thread-safe.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code create} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T> the element type
     * @param source the emitter that is called when an {@code Observer} subscribes to the returned {@code Observable}
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code source} is {@code null}
     * @see ObservableOnSubscribe
     * @see ObservableEmitter
     * @see Cancellable
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <@NonNull T> Observable<T> create(@NonNull ObservableOnSubscribe<T> source) {
        Objects.requireNonNull(source, "source is null");
        return RxJavaPlugins.onAssembly(new ObservableCreate<>(source));
    }

    /**
     * Returns an {@code Observable} that calls an {@link ObservableSource} factory to create an {@code ObservableSource} for each new {@link Observer}
     * that subscribes. That is, for each subscriber, the actual {@code ObservableSource} that subscriber observes is
     * determined by the factory function.
     * <p>
     * <img width="640" height="340" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/defer.v3.png" alt="">
     * <p>
     * The {@code defer} operator allows you to defer or delay emitting items from an {@code ObservableSource} until such time as an
     * {@code Observer} subscribes to the {@code ObservableSource}. This allows an {@code Observer} to easily obtain updates or a
     * refreshed version of the sequence.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code defer} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param supplier
     *            the {@code ObservableSource} factory function to invoke for each {@code Observer} that subscribes to the
     *            resulting {@code Observable}
     * @param <T>
     *            the type of the items emitted by the {@code ObservableSource}
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code supplier} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/defer.html">ReactiveX operators documentation: Defer</a>
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <@NonNull T> Observable<T> defer(@NonNull Supplier<? extends @NonNull ObservableSource<? extends T>> supplier) {
        Objects.requireNonNull(supplier, "supplier is null");
        return RxJavaPlugins.onAssembly(new ObservableDefer<>(supplier));
    }

    /**
     * Returns an {@code Observable} that emits no items to the {@link Observer} and immediately invokes its
     * {@link Observer#onComplete onComplete} method.
     * <p>
     * <img width="640" height="190" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/empty.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code empty} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T>
     *            the type of the items (ostensibly) emitted by the {@code Observable}
     * @return the shared {@code Observable} instance
     * @see <a href="http://reactivex.io/documentation/operators/empty-never-throw.html">ReactiveX operators documentation: Empty</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @SuppressWarnings("unchecked")
    @NonNull
    public static <@NonNull T> Observable<T> empty() {
        return RxJavaPlugins.onAssembly((Observable<T>) ObservableEmpty.INSTANCE);
    }

    /**
     * Returns an {@code Observable} that invokes an {@link Observer}'s {@link Observer#onError onError} method when the
     * {@code Observer} subscribes to it.
     * <p>
     * <img width="640" height="221" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/error.supplier.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code error} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param supplier
     *            a {@link Supplier} factory to return a {@link Throwable} for each individual {@code Observer}
     * @param <T>
     *            the type of the items (ostensibly) emitted by the {@code Observable}
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code supplier} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/empty-never-throw.html">ReactiveX operators documentation: Throw</a>
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <@NonNull T> Observable<T> error(@NonNull Supplier<? extends @NonNull Throwable> supplier) {
        Objects.requireNonNull(supplier, "supplier is null");
        return RxJavaPlugins.onAssembly(new ObservableError<>(supplier));
    }

    /**
     * Returns an {@code Observable} that invokes an {@link Observer}'s {@link Observer#onError onError} method when the
     * {@code Observer} subscribes to it.
     * <p>
     * <img width="640" height="221" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/error.item.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code error} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param throwable
     *            the particular {@link Throwable} to pass to {@link Observer#onError onError}
     * @param <T>
     *            the type of the items (ostensibly) emitted by the {@code Observable}
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code throwable} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/empty-never-throw.html">ReactiveX operators documentation: Throw</a>
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <@NonNull T> Observable<T> error(@NonNull Throwable throwable) {
        Objects.requireNonNull(throwable, "throwable is null");
        return error(Functions.justSupplier(throwable));
    }

    /**
     * Returns an {@code Observable} instance that runs the given {@link Action} for each {@link Observer} and
     * emits either its exception or simply completes.
     * <p>
     * <img width="640" height="287" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Observable.fromAction.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code fromAction} does not operate by default on a particular {@link Scheduler}.</dd>
     *  <dt><b>Error handling:</b></dt>
     *  <dd> If the {@code Action} throws an exception, the respective {@link Throwable} is
     *  delivered to the downstream via {@link Observer#onError(Throwable)},
     *  except when the downstream has canceled the resulting {@code Observable} source.
     *  In this latter case, the {@code Throwable} is delivered to the global error handler via
     *  {@link RxJavaPlugins#onError(Throwable)} as an {@link io.reactivex.rxjava3.exceptions.UndeliverableException UndeliverableException}.
     *  </dd>
     * </dl>
     * @param <T> the target type
     * @param action the {@code Action} to run for each {@code Observer}
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code action} is {@code null}
     * @since 3.0.0
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <@NonNull T> Observable<T> fromAction(@NonNull Action action) {
        Objects.requireNonNull(action, "action is null");
        return RxJavaPlugins.onAssembly(new ObservableFromAction<>(action));
    }

    /**
     * Converts an array into an {@link ObservableSource} that emits the items in the array.
     * <p>
     * <img width="640" height="315" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/from.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code fromArray} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param items
     *            the array of elements
     * @param <T>
     *            the type of items in the array and the type of items to be emitted by the resulting {@code Observable}
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code items} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/from.html">ReactiveX operators documentation: From</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    @SafeVarargs
    public static <@NonNull T> Observable<T> fromArray(@NonNull T... items) {
        Objects.requireNonNull(items, "items is null");
        if (items.length == 0) {
            return empty();
        }
        if (items.length == 1) {
            return just(items[0]);
        }
        return RxJavaPlugins.onAssembly(new ObservableFromArray<>(items));
    }

    /**
     * Returns an {@code Observable} that, when an observer subscribes to it, invokes a function you specify and then
     * emits the value returned from that function.
     * <p>
     * <img width="640" height="310" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/fromCallable.v3.png" alt="">
     * <p>
     * This allows you to defer the execution of the function you specify until an observer subscribes to the
     * {@code Observable}. That is to say, it makes the function "lazy."
     * <dl>
     *   <dt><b>Scheduler:</b></dt>
     *   <dd>{@code fromCallable} does not operate by default on a particular {@link Scheduler}.</dd>
     *   <dt><b>Error handling:</b></dt>
     *   <dd> If the {@link Callable} throws an exception, the respective {@link Throwable} is
     *   delivered to the downstream via {@link Observer#onError(Throwable)},
     *   except when the downstream has disposed the current {@code Observable} source.
     *   In this latter case, the {@code Throwable} is delivered to the global error handler via
     *   {@link RxJavaPlugins#onError(Throwable)} as an {@link UndeliverableException}.
     *   </dd>
     * </dl>
     * @param callable
     *         a function, the execution of which should be deferred; {@code fromCallable} will invoke this
     *         function only when an observer subscribes to the {@code Observable} that {@code fromCallable} returns
     * @param <T>
     *         the type of the item returned by the {@code Callable} and emitted by the {@code Observable}
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code callable} is {@code null}
     * @see #defer(Supplier)
     * @see #fromSupplier(Supplier)
     * @since 2.0
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <@NonNull T> Observable<T> fromCallable(@NonNull Callable<? extends T> callable) {
        Objects.requireNonNull(callable, "callable is null");
        return RxJavaPlugins.onAssembly(new ObservableFromCallable<>(callable));
    }

    /**
     * Wraps a {@link CompletableSource} into an {@code Observable}.
     * <p>
     * <img width="640" height="278" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Observable.fromCompletable.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code fromCompletable} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the target type
     * @param completableSource the {@code CompletableSource} to convert from
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code completableSource} is {@code null}
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <@NonNull T> Observable<T> fromCompletable(@NonNull CompletableSource completableSource) {
        Objects.requireNonNull(completableSource, "completableSource is null");
        return RxJavaPlugins.onAssembly(new ObservableFromCompletable<>(completableSource));
    }

    /**
     * Converts a {@link Future} into an {@code Observable}.
     * <p>
     * <img width="640" height="284" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/fromFuture.noarg.png" alt="">
     * <p>
     * The operator calls {@link Future#get()}, which is a blocking method, on the subscription thread.
     * It is recommended applying {@link #subscribeOn(Scheduler)} to move this blocking wait to a
     * background thread, and if the {@link Scheduler} supports it, interrupt the wait when the flow
     * is disposed.
     * <p>
     * Unlike 1.x, disposing the {@code Observable} won't cancel the future. If necessary, one can use composition to achieve the
     * cancellation effect: {@code futureObservableSource.doOnDispose(() -> future.cancel(true));}.
     * <p>
     * Also note that this operator will consume a {@link CompletionStage}-based {@code Future} subclass (such as
     * {@link CompletableFuture}) in a blocking manner as well. Use the {@link #fromCompletionStage(CompletionStage)}
     * operator to convert and consume such sources in a non-blocking fashion instead.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code fromFuture} does not operate by default on a particular {@code Scheduler}.</dd>
     * </dl>
     *
     * @param future
     *            the source {@code Future}
     * @param <T>
     *            the type of object that the {@code Future} returns, and also the type of item to be emitted by
     *            the resulting {@code Observable}
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code future} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/from.html">ReactiveX operators documentation: From</a>
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <@NonNull T> Observable<T> fromFuture(@NonNull Future<? extends T> future) {
        Objects.requireNonNull(future, "future is null");
        return RxJavaPlugins.onAssembly(new ObservableFromFuture<>(future, 0L, null));
    }

    /**
     * Converts a {@link Future} into an {@code Observable}, with a timeout on the {@code Future}.
     * <p>
     * <img width="640" height="287" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/fromFuture.timeout.png" alt="">
     * <p>
     * The operator calls {@link Future#get(long, TimeUnit)}, which is a blocking method, on the subscription thread.
     * It is recommended applying {@link #subscribeOn(Scheduler)} to move this blocking wait to a
     * background thread, and if the {@link Scheduler} supports it, interrupt the wait when the flow
     * is disposed.
     * <p>
     * Unlike 1.x, disposing the {@code Observable} won't cancel the future. If necessary, one can use composition to achieve the
     * cancellation effect: {@code futureObservableSource.doOnDispose(() -> future.cancel(true));}.
     * <p>
     * Also note that this operator will consume a {@link CompletionStage}-based {@code Future} subclass (such as
     * {@link CompletableFuture}) in a blocking manner as well. Use the {@link #fromCompletionStage(CompletionStage)}
     * operator to convert and consume such sources in a non-blocking fashion instead.
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
     *            the resulting {@code Observable}
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code future} or {@code unit} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/from.html">ReactiveX operators documentation: From</a>
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <@NonNull T> Observable<T> fromFuture(@NonNull Future<? extends T> future, long timeout, @NonNull TimeUnit unit) {
        Objects.requireNonNull(future, "future is null");
        Objects.requireNonNull(unit, "unit is null");
        return RxJavaPlugins.onAssembly(new ObservableFromFuture<>(future, timeout, unit));
    }

    /**
     * Converts an {@link Iterable} sequence into an {@code Observable} that emits the items in the sequence.
     * <p>
     * <img width="640" height="187" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/fromIterable.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code fromIterable} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param source
     *            the source {@code Iterable} sequence
     * @param <T>
     *            the type of items in the {@code Iterable} sequence and the type of items to be emitted by the
     *            resulting {@code Observable}
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code source} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/from.html">ReactiveX operators documentation: From</a>
     * @see #fromStream(Stream)
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <@NonNull T> Observable<T> fromIterable(@NonNull Iterable<? extends T> source) {
        Objects.requireNonNull(source, "source is null");
        return RxJavaPlugins.onAssembly(new ObservableFromIterable<>(source));
    }

    /**
     * Returns an {@code Observable} instance that when subscribed to, subscribes to the {@link MaybeSource} instance and
     * emits {@code onSuccess} as a single item or forwards any {@code onComplete} or
     * {@code onError} signal.
     * <p>
     * <img width="640" height="226" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Observable.fromMaybe.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code fromMaybe} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the value type of the {@code MaybeSource} element
     * @param maybe the {@code MaybeSource} instance to subscribe to, not {@code null}
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code maybe} is {@code null}
     * @since 3.0.0
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <@NonNull T> Observable<T> fromMaybe(@NonNull MaybeSource<T> maybe) {
        Objects.requireNonNull(maybe, "maybe is null");
        return RxJavaPlugins.onAssembly(new MaybeToObservable<>(maybe));
    }

    /**
     * Converts an arbitrary <em>Reactive Streams</em> {@link Publisher} into an {@code Observable}.
     * <p>
     * <img width="640" height="344" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/fromPublisher.o.png" alt="">
     * <p>
     * The {@code Publisher} must follow the
     * <a href="https://github.com/reactive-streams/reactive-streams-jvm#reactive-streams">Reactive-Streams specification</a>.
     * Violating the specification may result in undefined behavior.
     * <p>
     * If possible, use {@link #create(ObservableOnSubscribe)} to create a
     * source-like {@code Observable} instead.
     * <p>
     * Note that even though {@code Publisher} appears to be a functional interface, it
     * is not recommended to implement it through a lambda as the specification requires
     * state management that is not achievable with a stateless lambda.
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The source {@code publisher} is consumed in an unbounded fashion without applying any
     *  backpressure to it.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code fromPublisher} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the value type of the flow
     * @param publisher the {@code Publisher} to convert
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code publisher} is {@code null}
     * @see #create(ObservableOnSubscribe)
     */
    @BackpressureSupport(BackpressureKind.UNBOUNDED_IN)
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <@NonNull T> Observable<T> fromPublisher(@NonNull Publisher<? extends T> publisher) {
        Objects.requireNonNull(publisher, "publisher is null");
        return RxJavaPlugins.onAssembly(new ObservableFromPublisher<>(publisher));
    }

    /**
     * Returns an {@code Observable} instance that runs the given {@link Runnable} for each {@link Observer} and
     * emits either its unchecked exception or simply completes.
     * <p>
     * <img width="640" height="286" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Observable.fromRunnable.png" alt="">
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
     *  delivered to the downstream via {@link Observer#onError(Throwable)},
     *  except when the downstream has canceled the resulting {@code Observable} source.
     *  In this latter case, the {@code Throwable} is delivered to the global error handler via
     *  {@link RxJavaPlugins#onError(Throwable)} as an {@link io.reactivex.rxjava3.exceptions.UndeliverableException UndeliverableException}.
     *  </dd>
     * </dl>
     * @param <T> the target type
     * @param run the {@code Runnable} to run for each {@code Observer}
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code run} is {@code null}
     * @since 3.0.0
     * @see #fromAction(Action)
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <@NonNull T> Observable<T> fromRunnable(@NonNull Runnable run) {
        Objects.requireNonNull(run, "run is null");
        return RxJavaPlugins.onAssembly(new ObservableFromRunnable<>(run));
    }

    /**
     * Returns an {@code Observable} instance that when subscribed to, subscribes to the {@link SingleSource} instance and
     * emits {@code onSuccess} as a single item or forwards the {@code onError} signal.
     * <p>
     * <img width="640" height="341" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Observable.fromSingle.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code fromSingle} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the value type of the {@code SingleSource} element
     * @param source the {@code SingleSource} instance to subscribe to, not {@code null}
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code source} is {@code null}
     * @since 3.0.0
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <@NonNull T> Observable<T> fromSingle(@NonNull SingleSource<T> source) {
        Objects.requireNonNull(source, "source is null");
        return RxJavaPlugins.onAssembly(new SingleToObservable<>(source));
    }

    /**
     * Returns an {@code Observable} that, when an observer subscribes to it, invokes a supplier function you specify and then
     * emits the value returned from that function.
     * <p>
     * <img width="640" height="310" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Observable.fromSupplier.v3.png" alt="">
     * <p>
     * This allows you to defer the execution of the function you specify until an observer subscribes to the
     * {@code Observable}. That is to say, it makes the function "lazy."
     * <dl>
     *   <dt><b>Scheduler:</b></dt>
     *   <dd>{@code fromSupplier} does not operate by default on a particular {@link Scheduler}.</dd>
     *   <dt><b>Error handling:</b></dt>
     *   <dd> If the {@link Supplier} throws an exception, the respective {@link Throwable} is
     *   delivered to the downstream via {@link Observer#onError(Throwable)},
     *   except when the downstream has disposed the current {@code Observable} source.
     *   In this latter case, the {@code Throwable} is delivered to the global error handler via
     *   {@link RxJavaPlugins#onError(Throwable)} as an {@link UndeliverableException}.
     *   </dd>
     * </dl>
     * @param supplier
     *         a function, the execution of which should be deferred; {@code fromSupplier} will invoke this
     *         function only when an observer subscribes to the {@code Observable} that {@code fromSupplier} returns
     * @param <T>
     *         the type of the item emitted by the {@code Observable}
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code supplier} is {@code null}
     * @see #defer(Supplier)
     * @see #fromCallable(Callable)
     * @since 3.0.0
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <@NonNull T> Observable<T> fromSupplier(@NonNull Supplier<? extends T> supplier) {
        Objects.requireNonNull(supplier, "supplier is null");
        return RxJavaPlugins.onAssembly(new ObservableFromSupplier<>(supplier));
    }

    /**
     * Returns a cold, synchronous and stateless generator of values.
     * <p>
     * <img width="640" height="315" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/generate.2.v3.png" alt="">
     * <p>
     * Note that the {@link Emitter#onNext}, {@link Emitter#onError} and
     * {@link Emitter#onComplete} methods provided to the function via the {@link Emitter} instance should be called synchronously,
     * never concurrently and only while the function body is executing. Calling them from multiple threads
     * or outside the function call is not supported and leads to an undefined behavior.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code generate} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T> the generated value type
     * @param generator the {@link Consumer} called in a loop after a downstream {@link Observer} has
     * subscribed. The callback then should call {@code onNext}, {@code onError} or
     * {@code onComplete} to signal a value or a terminal event. Signaling multiple {@code onNext}
     * in a call will make the operator signal {@link IllegalStateException}.
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code generator} is {@code null}
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <@NonNull T> Observable<T> generate(@NonNull Consumer<Emitter<T>> generator) {
        Objects.requireNonNull(generator, "generator is null");
        return generate(Functions.nullSupplier(),
                ObservableInternalHelper.simpleGenerator(generator), Functions.emptyConsumer());
    }

    /**
     * Returns a cold, synchronous and stateful generator of values.
     * <p>
     * <img width="640" height="315" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/generate.2.v3.png" alt="">
     * <p>
     * Note that the {@link Emitter#onNext}, {@link Emitter#onError} and
     * {@link Emitter#onComplete} methods provided to the function via the {@link Emitter} instance should be called synchronously,
     * never concurrently and only while the function body is executing. Calling them from multiple threads
     * or outside the function call is not supported and leads to an undefined behavior.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code generate} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <S> the type of the per-{@link Observer} state
     * @param <T> the generated value type
     * @param initialState the {@link Supplier} to generate the initial state for each {@code Observer}
     * @param generator the {@link BiConsumer} called in a loop after a downstream {@code Observer} has
     * subscribed. The callback then should call {@code onNext}, {@code onError} or
     * {@code onComplete} to signal a value or a terminal event. Signaling multiple {@code onNext}
     * in a call will make the operator signal {@link IllegalStateException}.
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code initialState} or {@code generator} is {@code null}
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <@NonNull T, @NonNull S> Observable<T> generate(@NonNull Supplier<S> initialState, @NonNull BiConsumer<S, Emitter<T>> generator) {
        Objects.requireNonNull(generator, "generator is null");
        return generate(initialState, ObservableInternalHelper.simpleBiGenerator(generator), Functions.emptyConsumer());
    }

    /**
     * Returns a cold, synchronous and stateful generator of values.
     * <p>
     * <img width="640" height="315" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/generate.2.v3.png" alt="">
     * <p>
     * Note that the {@link Emitter#onNext}, {@link Emitter#onError} and
     * {@link Emitter#onComplete} methods provided to the function via the {@link Emitter} instance should be called synchronously,
     * never concurrently and only while the function body is executing. Calling them from multiple threads
     * or outside the function call is not supported and leads to an undefined behavior.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code generate} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <S> the type of the per-{@link Observer} state
     * @param <T> the generated value type
     * @param initialState the {@link Supplier} to generate the initial state for each {@code Observer}
     * @param generator the {@link BiConsumer} called in a loop after a downstream {@code Observer} has
     * subscribed. The callback then should call {@code onNext}, {@code onError} or
     * {@code onComplete} to signal a value or a terminal event. Signaling multiple {@code onNext}
     * in a call will make the operator signal {@link IllegalStateException}.
     * @param disposeState the {@link Consumer} that is called with the current state when the generator
     * terminates the sequence or it gets disposed
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code initialState}, {@code generator} or {@code disposeState} is {@code null}
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <@NonNull T, @NonNull S> Observable<T> generate(
            @NonNull Supplier<S> initialState,
            @NonNull BiConsumer<S, Emitter<T>> generator,
            @NonNull Consumer<? super S> disposeState) {
        Objects.requireNonNull(generator, "generator is null");
        return generate(initialState, ObservableInternalHelper.simpleBiGenerator(generator), disposeState);
    }

    /**
     * Returns a cold, synchronous and stateful generator of values.
     * <p>
     * <img width="640" height="315" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/generate.2.v3.png" alt="">
     * <p>
     * Note that the {@link Emitter#onNext}, {@link Emitter#onError} and
     * {@link Emitter#onComplete} methods provided to the function via the {@link Emitter} instance should be called synchronously,
     * never concurrently and only while the function body is executing. Calling them from multiple threads
     * or outside the function call is not supported and leads to an undefined behavior.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code generate} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <S> the type of the per-{@link Observer} state
     * @param <T> the generated value type
     * @param initialState the {@link Supplier} to generate the initial state for each {@code Observer}
     * @param generator the {@link BiConsumer} called in a loop after a downstream {@code Observer} has
     * subscribed. The callback then should call {@code onNext}, {@code onError} or
     * {@code onComplete} to signal a value or a terminal event and should return a (new) state for
     * the next invocation. Signaling multiple {@code onNext}
     * in a call will make the operator signal {@link IllegalStateException}.
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code initialState} or {@code generator} is {@code null}
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public static <@NonNull T, @NonNull S> Observable<T> generate(@NonNull Supplier<S> initialState, @NonNull BiFunction<S, Emitter<T>, S> generator) {
        return generate(initialState, generator, Functions.emptyConsumer());
    }

    /**
     * Returns a cold, synchronous and stateful generator of values.
     * <p>
     * <img width="640" height="315" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/generate.2.v3.png" alt="">
     * <p>
     * Note that the {@link Emitter#onNext}, {@link Emitter#onError} and
     * {@link Emitter#onComplete} methods provided to the function via the {@link Emitter} instance should be called synchronously,
     * never concurrently and only while the function body is executing. Calling them from multiple threads
     * or outside the function call is not supported and leads to an undefined behavior.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code generate} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <S> the type of the per-{@link Observer} state
     * @param <T> the generated value type
     * @param initialState the {@link Supplier} to generate the initial state for each {@code Observer}
     * @param generator the {@link BiConsumer} called in a loop after a downstream {@code Observer} has
     * subscribed. The callback then should call {@code onNext}, {@code onError} or
     * {@code onComplete} to signal a value or a terminal event and should return a (new) state for
     * the next invocation. Signaling multiple {@code onNext}
     * in a call will make the operator signal {@link IllegalStateException}.
     * @param disposeState the {@link Consumer} that is called with the current state when the generator
     * terminates the sequence or it gets disposed
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code initialState}, {@code generator} or {@code disposeState} is {@code null}
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <@NonNull T, @NonNull S> Observable<T> generate(@NonNull Supplier<S> initialState, @NonNull BiFunction<S, Emitter<T>, S> generator,
            @NonNull Consumer<? super S> disposeState) {
        Objects.requireNonNull(initialState, "initialState is null");
        Objects.requireNonNull(generator, "generator is null");
        Objects.requireNonNull(disposeState, "disposeState is null");
        return RxJavaPlugins.onAssembly(new ObservableGenerate<>(initialState, generator, disposeState));
    }

    /**
     * Returns an {@code Observable} that emits a {@code 0L} after the {@code initialDelay} and ever increasing numbers
     * after each {@code period} of time thereafter.
     * <p>
     * <img width="640" height="200" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/timer.p.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code interval} operates by default on the {@code computation} {@link Scheduler}.</dd>
     * </dl>
     *
     * @param initialDelay
     *            the initial delay time to wait before emitting the first value of 0L
     * @param period
     *            the period of time between emissions of the subsequent numbers
     * @param unit
     *            the time unit for both {@code initialDelay} and {@code period}
     * @return the new {@code Observable} instance
     * @see <a href="http://reactivex.io/documentation/operators/interval.html">ReactiveX operators documentation: Interval</a>
     * @throws NullPointerException if {@code unit} is {@code null}
     * @since 1.0.12
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.COMPUTATION)
    @NonNull
    public static Observable<Long> interval(long initialDelay, long period, @NonNull TimeUnit unit) {
        return interval(initialDelay, period, unit, Schedulers.computation());
    }

    /**
     * Returns an {@code Observable} that emits a {@code 0L} after the {@code initialDelay} and ever increasing numbers
     * after each {@code period} of time thereafter, on a specified {@link Scheduler}.
     * <p>
     * <img width="640" height="200" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/timer.ps.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>You specify which {@code Scheduler} this operator will use.</dd>
     * </dl>
     *
     * @param initialDelay
     *            the initial delay time to wait before emitting the first value of 0L
     * @param period
     *            the period of time between emissions of the subsequent numbers
     * @param unit
     *            the time unit for both {@code initialDelay} and {@code period}
     * @param scheduler
     *            the {@code Scheduler} on which the waiting happens and items are emitted
     * @return the new {@code Observable} instance
     * @see <a href="http://reactivex.io/documentation/operators/interval.html">ReactiveX operators documentation: Interval</a>
     * @since 1.0.12
     * @throws NullPointerException if {@code unit} or {@code scheduler} is {@code null}
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    public static Observable<Long> interval(long initialDelay, long period, @NonNull TimeUnit unit, @NonNull Scheduler scheduler) {
        Objects.requireNonNull(unit, "unit is null");
        Objects.requireNonNull(scheduler, "scheduler is null");

        return RxJavaPlugins.onAssembly(new ObservableInterval(Math.max(0L, initialDelay), Math.max(0L, period), unit, scheduler));
    }

    /**
     * Returns an {@code Observable} that emits a sequential number every specified interval of time.
     * <p>
     * <img width="640" height="195" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/interval.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code interval} operates by default on the {@code computation} {@link Scheduler}.</dd>
     * </dl>
     *
     * @param period
     *            the period size in time units (see below)
     * @param unit
     *            time units to use for the interval size
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code unit} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/interval.html">ReactiveX operators documentation: Interval</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.COMPUTATION)
    @NonNull
    public static Observable<Long> interval(long period, @NonNull TimeUnit unit) {
        return interval(period, period, unit, Schedulers.computation());
    }

    /**
     * Returns an {@code Observable} that emits a sequential number every specified interval of time, on a
     * specified {@link Scheduler}.
     * <p>
     * <img width="640" height="200" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/interval.s.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>You specify which {@code Scheduler} this operator will use.</dd>
     * </dl>
     *
     * @param period
     *            the period size in time units (see below)
     * @param unit
     *            time units to use for the interval size
     * @param scheduler
     *            the {@code Scheduler} to use for scheduling the items
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code unit} or {@code scheduler} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/interval.html">ReactiveX operators documentation: Interval</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    @NonNull
    public static Observable<Long> interval(long period, @NonNull TimeUnit unit, @NonNull Scheduler scheduler) {
        return interval(period, period, unit, scheduler);
    }

    /**
     * Signals a range of long values, the first after some initial delay and the rest periodically after.
     * <p>
     * The sequence completes immediately after the last value (start + count - 1) has been reached.
     * <p>
     * <img width="640" height="195" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/intervalRange.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code intervalRange} by default operates on the {@link Schedulers#computation() computation} {@link Scheduler}.</dd>
     * </dl>
     * @param start that start value of the range
     * @param count the number of values to emit in total, if zero, the operator emits an {@code onComplete} after the initial delay.
     * @param initialDelay the initial delay before signaling the first value (the start)
     * @param period the period between subsequent values
     * @param unit the unit of measure of the {@code initialDelay} and {@code period} amounts
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code unit} is {@code null}
     * @throws IllegalArgumentException
     *             if {@code count} is negative, or if {@code start} + {@code count} &minus; 1 exceeds
     *             {@link Long#MAX_VALUE}
     * @see #range(int, int)
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.COMPUTATION)
    public static Observable<Long> intervalRange(long start, long count, long initialDelay, long period, @NonNull TimeUnit unit) {
        return intervalRange(start, count, initialDelay, period, unit, Schedulers.computation());
    }

    /**
     * Signals a range of long values, the first after some initial delay and the rest periodically after.
     * <p>
     * The sequence completes immediately after the last value (start + count - 1) has been reached.
     * <p>
     * <img width="640" height="195" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/intervalRange.s.v3.png" alt="">     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>you provide the {@link Scheduler}.</dd>
     * </dl>
     * @param start that start value of the range
     * @param count the number of values to emit in total, if zero, the operator emits an {@code onComplete} after the initial delay.
     * @param initialDelay the initial delay before signaling the first value (the start)
     * @param period the period between subsequent values
     * @param unit the unit of measure of the {@code initialDelay} and {@code period} amounts
     * @param scheduler the target scheduler where the values and terminal signals will be emitted
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code unit} or {@code scheduler} is {@code null}
     * @throws IllegalArgumentException
     *             if {@code count} is negative, or if {@code start} + {@code count} &minus; 1 exceeds
     *             {@link Long#MAX_VALUE}
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    public static Observable<Long> intervalRange(long start, long count, long initialDelay, long period, @NonNull TimeUnit unit, @NonNull Scheduler scheduler) {
        if (count < 0) {
            throw new IllegalArgumentException("count >= 0 required but it was " + count);
        }

        if (count == 0L) {
            return Observable.<Long>empty().delay(initialDelay, unit, scheduler);
        }

        long end = start + (count - 1);
        if (start > 0 && end < 0) {
            throw new IllegalArgumentException("Overflow! start + count is bigger than Long.MAX_VALUE");
        }
        Objects.requireNonNull(unit, "unit is null");
        Objects.requireNonNull(scheduler, "scheduler is null");

        return RxJavaPlugins.onAssembly(new ObservableIntervalRange(start, end, Math.max(0L, initialDelay), Math.max(0L, period), unit, scheduler));
    }

    /**
     * Returns an {@code Observable} that signals the given (constant reference) item and then completes.
     * <p>
     * <img width="640" height="290" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/just.item.png" alt="">
     * <p>
     * Note that the item is taken and re-emitted as is and not computed by any means by {@code just}. Use {@link #fromCallable(Callable)}
     * to generate a single item on demand (when {@link Observer}s subscribe to it).
     * <p>
     * See the multi-parameter overloads of {@code just} to emit more than one (constant reference) items one after the other.
     * Use {@link #fromArray(Object...)} to emit an arbitrary number of items that are known upfront.
     * <p>
     * To emit the items of an {@link Iterable} sequence (such as a {@link java.util.List}), use {@link #fromIterable(Iterable)}.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code just} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param item
     *            the item to emit
     * @param <T>
     *            the type of that item
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code item} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/just.html">ReactiveX operators documentation: Just</a>
     * @see #just(Object, Object)
     * @see #fromCallable(Callable)
     * @see #fromArray(Object...)
     * @see #fromIterable(Iterable)
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <@NonNull T> Observable<T> just(@NonNull T item) {
        Objects.requireNonNull(item, "item is null");
        return RxJavaPlugins.onAssembly(new ObservableJust<>(item));
    }

    /**
     * Converts two items into an {@code Observable} that emits those items.
     * <p>
     * <img width="640" height="186" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/just.2.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code just} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param item1
     *            first item
     * @param item2
     *            second item
     * @param <T>
     *            the type of these items
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code item1} or {@code item2} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/just.html">ReactiveX operators documentation: Just</a>
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <@NonNull T> Observable<T> just(@NonNull T item1, @NonNull T item2) {
        Objects.requireNonNull(item1, "item1 is null");
        Objects.requireNonNull(item2, "item2 is null");

        return fromArray(item1, item2);
    }

    /**
     * Converts three items into an {@code Observable} that emits those items.
     * <p>
     * <img width="640" height="186" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/just.3.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code just} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param item1
     *            first item
     * @param item2
     *            second item
     * @param item3
     *            third item
     * @param <T>
     *            the type of these items
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code item1}, {@code item2} or {@code item3} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/just.html">ReactiveX operators documentation: Just</a>
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <@NonNull T> Observable<T> just(@NonNull T item1, @NonNull T item2, @NonNull T item3) {
        Objects.requireNonNull(item1, "item1 is null");
        Objects.requireNonNull(item2, "item2 is null");
        Objects.requireNonNull(item3, "item3 is null");

        return fromArray(item1, item2, item3);
    }

    /**
     * Converts four items into an {@code Observable} that emits those items.
     * <p>
     * <img width="640" height="186" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/just.4.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code just} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param item1
     *            first item
     * @param item2
     *            second item
     * @param item3
     *            third item
     * @param item4
     *            fourth item
     * @param <T>
     *            the type of these items
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code item1}, {@code item2}, {@code item3} or {@code item4} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/just.html">ReactiveX operators documentation: Just</a>
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <@NonNull T> Observable<T> just(@NonNull T item1, @NonNull T item2, @NonNull T item3, @NonNull T item4) {
        Objects.requireNonNull(item1, "item1 is null");
        Objects.requireNonNull(item2, "item2 is null");
        Objects.requireNonNull(item3, "item3 is null");
        Objects.requireNonNull(item4, "item4 is null");

        return fromArray(item1, item2, item3, item4);
    }

    /**
     * Converts five items into an {@code Observable} that emits those items.
     * <p>
     * <img width="640" height="186" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/just.5.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code just} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param item1
     *            first item
     * @param item2
     *            second item
     * @param item3
     *            third item
     * @param item4
     *            fourth item
     * @param item5
     *            fifth item
     * @param <T>
     *            the type of these items
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code item1}, {@code item2}, {@code item3},
     *                              {@code item4} or {@code item5} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/just.html">ReactiveX operators documentation: Just</a>
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <@NonNull T> Observable<T> just(@NonNull T item1, @NonNull T item2, @NonNull T item3, @NonNull T item4, @NonNull T item5) {
        Objects.requireNonNull(item1, "item1 is null");
        Objects.requireNonNull(item2, "item2 is null");
        Objects.requireNonNull(item3, "item3 is null");
        Objects.requireNonNull(item4, "item4 is null");
        Objects.requireNonNull(item5, "item5 is null");

        return fromArray(item1, item2, item3, item4, item5);
    }

    /**
     * Converts six items into an {@code Observable} that emits those items.
     * <p>
     * <img width="640" height="186" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/just.6.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code just} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param item1
     *            first item
     * @param item2
     *            second item
     * @param item3
     *            third item
     * @param item4
     *            fourth item
     * @param item5
     *            fifth item
     * @param item6
     *            sixth item
     * @param <T>
     *            the type of these items
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code item1}, {@code item2}, {@code item3},
     *                              {@code item4}, {@code item5} or {@code item6} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/just.html">ReactiveX operators documentation: Just</a>
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <@NonNull T> Observable<T> just(@NonNull T item1, @NonNull T item2, @NonNull T item3, @NonNull T item4, @NonNull T item5, @NonNull T item6) {
        Objects.requireNonNull(item1, "item1 is null");
        Objects.requireNonNull(item2, "item2 is null");
        Objects.requireNonNull(item3, "item3 is null");
        Objects.requireNonNull(item4, "item4 is null");
        Objects.requireNonNull(item5, "item5 is null");
        Objects.requireNonNull(item6, "item6 is null");

        return fromArray(item1, item2, item3, item4, item5, item6);
    }

    /**
     * Converts seven items into an {@code Observable} that emits those items.
     * <p>
     * <img width="640" height="186" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/just.7.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code just} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param item1
     *            first item
     * @param item2
     *            second item
     * @param item3
     *            third item
     * @param item4
     *            fourth item
     * @param item5
     *            fifth item
     * @param item6
     *            sixth item
     * @param item7
     *            seventh item
     * @param <T>
     *            the type of these items
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code item1}, {@code item2}, {@code item3},
     *                              {@code item4}, {@code item5}, {@code item6}
     *                              or {@code item7} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/just.html">ReactiveX operators documentation: Just</a>
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <@NonNull T> Observable<T> just(@NonNull T item1, @NonNull T item2, @NonNull T item3, @NonNull T item4, @NonNull T item5, @NonNull T item6, @NonNull T item7) {
        Objects.requireNonNull(item1, "item1 is null");
        Objects.requireNonNull(item2, "item2 is null");
        Objects.requireNonNull(item3, "item3 is null");
        Objects.requireNonNull(item4, "item4 is null");
        Objects.requireNonNull(item5, "item5 is null");
        Objects.requireNonNull(item6, "item6 is null");
        Objects.requireNonNull(item7, "item7 is null");

        return fromArray(item1, item2, item3, item4, item5, item6, item7);
    }

    /**
     * Converts eight items into an {@code Observable} that emits those items.
     * <p>
     * <img width="640" height="186" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/just.8.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code just} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param item1
     *            first item
     * @param item2
     *            second item
     * @param item3
     *            third item
     * @param item4
     *            fourth item
     * @param item5
     *            fifth item
     * @param item6
     *            sixth item
     * @param item7
     *            seventh item
     * @param item8
     *            eighth item
     * @param <T>
     *            the type of these items
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code item1}, {@code item2}, {@code item3},
     *                              {@code item4}, {@code item5}, {@code item6}
     *                              {@code item7} or {@code item8} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/just.html">ReactiveX operators documentation: Just</a>
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <@NonNull T> Observable<T> just(@NonNull T item1, @NonNull T item2, @NonNull T item3, @NonNull T item4, @NonNull T item5, @NonNull T item6, @NonNull T item7, @NonNull T item8) {
        Objects.requireNonNull(item1, "item1 is null");
        Objects.requireNonNull(item2, "item2 is null");
        Objects.requireNonNull(item3, "item3 is null");
        Objects.requireNonNull(item4, "item4 is null");
        Objects.requireNonNull(item5, "item5 is null");
        Objects.requireNonNull(item6, "item6 is null");
        Objects.requireNonNull(item7, "item7 is null");
        Objects.requireNonNull(item8, "item8 is null");

        return fromArray(item1, item2, item3, item4, item5, item6, item7, item8);
    }

    /**
     * Converts nine items into an {@code Observable} that emits those items.
     * <p>
     * <img width="640" height="186" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/just.9.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code just} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param item1
     *            first item
     * @param item2
     *            second item
     * @param item3
     *            third item
     * @param item4
     *            fourth item
     * @param item5
     *            fifth item
     * @param item6
     *            sixth item
     * @param item7
     *            seventh item
     * @param item8
     *            eighth item
     * @param item9
     *            ninth item
     * @param <T>
     *            the type of these items
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code item1}, {@code item2}, {@code item3},
     *                              {@code item4}, {@code item5}, {@code item6}
     *                              {@code item7}, {@code item8} or {@code item9} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/just.html">ReactiveX operators documentation: Just</a>
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <@NonNull T> Observable<T> just(@NonNull T item1, @NonNull T item2, @NonNull T item3, @NonNull T item4, @NonNull T item5, @NonNull T item6, @NonNull T item7, @NonNull T item8, @NonNull T item9) {
        Objects.requireNonNull(item1, "item1 is null");
        Objects.requireNonNull(item2, "item2 is null");
        Objects.requireNonNull(item3, "item3 is null");
        Objects.requireNonNull(item4, "item4 is null");
        Objects.requireNonNull(item5, "item5 is null");
        Objects.requireNonNull(item6, "item6 is null");
        Objects.requireNonNull(item7, "item7 is null");
        Objects.requireNonNull(item8, "item8 is null");
        Objects.requireNonNull(item9, "item9 is null");

        return fromArray(item1, item2, item3, item4, item5, item6, item7, item8, item9);
    }

    /**
     * Converts ten items into an {@code Observable} that emits those items.
     * <p>
     * <img width="640" height="186" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/just.10.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code just} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param item1
     *            first item
     * @param item2
     *            second item
     * @param item3
     *            third item
     * @param item4
     *            fourth item
     * @param item5
     *            fifth item
     * @param item6
     *            sixth item
     * @param item7
     *            seventh item
     * @param item8
     *            eighth item
     * @param item9
     *            ninth item
     * @param item10
     *            tenth item
     * @param <T>
     *            the type of these items
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code item1}, {@code item2}, {@code item3},
     *                              {@code item4}, {@code item5}, {@code item6}
     *                              {@code item7}, {@code item8}, {@code item9}
     *                              or {@code item10} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/just.html">ReactiveX operators documentation: Just</a>
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <@NonNull T> Observable<T> just(@NonNull T item1, @NonNull T item2, @NonNull T item3, @NonNull T item4, @NonNull T item5, @NonNull T item6, @NonNull T item7, @NonNull T item8, @NonNull T item9, @NonNull T item10) {
        Objects.requireNonNull(item1, "item1 is null");
        Objects.requireNonNull(item2, "item2 is null");
        Objects.requireNonNull(item3, "item3 is null");
        Objects.requireNonNull(item4, "item4 is null");
        Objects.requireNonNull(item5, "item5 is null");
        Objects.requireNonNull(item6, "item6 is null");
        Objects.requireNonNull(item7, "item7 is null");
        Objects.requireNonNull(item8, "item8 is null");
        Objects.requireNonNull(item9, "item9 is null");
        Objects.requireNonNull(item10, "item10 is null");

        return fromArray(item1, item2, item3, item4, item5, item6, item7, item8, item9, item10);
    }

    /**
     * Flattens an {@link Iterable} of {@link ObservableSource}s into one {@code Observable}, without any transformation, while limiting the
     * number of concurrent subscriptions to these {@code ObservableSource}s.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/merge.v3.png" alt="">
     * <p>
     * You can combine the items emitted by multiple {@code ObservableSource}s so that they appear as a single {@code ObservableSource}, by
     * using the {@code merge} method.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code merge} does not operate by default on a particular {@link Scheduler}.</dd>
     *  <dt><b>Error handling:</b></dt>
     *  <dd>If any of the returned {@code ObservableSource}s signal a {@link Throwable} via {@code onError}, the resulting
     *  {@code Observable} terminates with that {@code Throwable} and all other source {@code ObservableSource}s are disposed.
     *  If more than one {@code ObservableSource} signals an error, the resulting {@code Observable} may terminate with the
     *  first one's error or, depending on the concurrency of the sources, may terminate with a
     *  {@link CompositeException} containing two or more of the various error signals.
     *  {@code Throwable}s that didn't make into the composite will be sent (individually) to the global error handler via
     *  {@link RxJavaPlugins#onError(Throwable)} method as {@link UndeliverableException} errors. Similarly, {@code Throwable}s
     *  signaled by source(s) after the returned {@code Observable} has been disposed or terminated with a
     *  (composite) error will be sent to the same global error handler.
     *  Use {@link #mergeDelayError(Iterable, int, int)} to merge sources and terminate only when all source {@code ObservableSource}s
     *  have completed or failed with an error.
     *  </dd>
     * </dl>
     *
     * @param <T> the common element base type
     * @param sources
     *            the {@code Iterable} of {@code ObservableSource}s
     * @param maxConcurrency
     *            the maximum number of {@code ObservableSource}s that may be subscribed to concurrently
     * @param bufferSize
     *            the number of items expected from each inner {@code ObservableSource} to be buffered
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code sources} is {@code null}
     * @throws IllegalArgumentException
     *             if {@code maxConcurrency} or {@code bufferSize} is non-positive
     * @see <a href="http://reactivex.io/documentation/operators/merge.html">ReactiveX operators documentation: Merge</a>
     * @see #mergeDelayError(Iterable, int, int)
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public static <@NonNull T> Observable<T> merge(@NonNull Iterable<@NonNull ? extends ObservableSource<? extends T>> sources, int maxConcurrency, int bufferSize) {
        return fromIterable(sources).flatMap((Function)Functions.identity(), false, maxConcurrency, bufferSize);
    }

    /**
     * Flattens an array of {@link ObservableSource}s into one {@code Observable}, without any transformation, while limiting the
     * number of concurrent subscriptions to these {@code ObservableSource}s.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/merge.v3.png" alt="">
     * <p>
     * You can combine the items emitted by multiple {@code ObservableSource}s so that they appear as a single {@code ObservableSource}, by
     * using the {@code merge} method.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code mergeArray} does not operate by default on a particular {@link Scheduler}.</dd>
     *  <dt><b>Error handling:</b></dt>
     *  <dd>If any of the {@code ObservableSource}s signal a {@link Throwable} via {@code onError}, the resulting
     *  {@code Observable} terminates with that {@code Throwable} and all other source {@code ObservableSource}s are disposed.
     *  If more than one {@code ObservableSource} signals an error, the resulting {@code Observable} may terminate with the
     *  first one's error or, depending on the concurrency of the sources, may terminate with a
     *  {@link CompositeException} containing two or more of the various error signals.
     *  {@code Throwable}s that didn't make into the composite will be sent (individually) to the global error handler via
     *  {@link RxJavaPlugins#onError(Throwable)} method as {@link UndeliverableException} errors. Similarly, {@code Throwable}s
     *  signaled by source(s) after the returned {@code Observable} has been disposed or terminated with a
     *  (composite) error will be sent to the same global error handler.
     *  Use {@link #mergeArrayDelayError(int, int, ObservableSource...)} to merge sources and terminate only when all source {@code ObservableSource}s
     *  have completed or failed with an error.
     *  </dd>
     * </dl>
     *
     * @param <T> the common element base type
     * @param sources
     *            the array of {@code ObservableSource}s
     * @param maxConcurrency
     *            the maximum number of {@code ObservableSource}s that may be subscribed to concurrently
     * @param bufferSize
     *            the number of items expected from each inner {@code ObservableSource} to be buffered
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code sources} is {@code null}
     * @throws IllegalArgumentException
     *             if {@code maxConcurrency} or {@code bufferSize} is non-positive
     * @see <a href="http://reactivex.io/documentation/operators/merge.html">ReactiveX operators documentation: Merge</a>
     * @see #mergeArrayDelayError(int, int, ObservableSource...)
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    @SafeVarargs
    public static <@NonNull T> Observable<T> mergeArray(int maxConcurrency, int bufferSize, @NonNull ObservableSource<? extends T>... sources) {
        return fromArray(sources).flatMap((Function)Functions.identity(), false, maxConcurrency, bufferSize);
    }

    /**
     * Flattens an {@link Iterable} of {@link ObservableSource}s into one {@code Observable}, without any transformation.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/merge.v3.png" alt="">
     * <p>
     * You can combine the items emitted by multiple {@code ObservableSource}s so that they appear as a single {@code ObservableSource}, by
     * using the {@code merge} method.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code merge} does not operate by default on a particular {@link Scheduler}.</dd>
     *  <dt><b>Error handling:</b></dt>
     *  <dd>If any of the returned {@code ObservableSource}s signal a {@link Throwable} via {@code onError}, the resulting
     *  {@code Observable} terminates with that {@code Throwable} and all other source {@code ObservableSource}s are disposed.
     *  If more than one {@code ObservableSource} signals an error, the resulting {@code Observable} may terminate with the
     *  first one's error or, depending on the concurrency of the sources, may terminate with a
     *  {@link CompositeException} containing two or more of the various error signals.
     *  {@code Throwable}s that didn't make into the composite will be sent (individually) to the global error handler via
     *  {@link RxJavaPlugins#onError(Throwable)} method as {@link UndeliverableException} errors. Similarly, {@code Throwable}s
     *  signaled by source(s) after the returned {@code Observable} has been disposed or terminated with a
     *  (composite) error will be sent to the same global error handler.
     *  Use {@link #mergeDelayError(Iterable)} to merge sources and terminate only when all source {@code ObservableSource}s
     *  have completed or failed with an error.
     *  </dd>
     * </dl>
     *
     * @param <T> the common element base type
     * @param sources
     *            the {@code Iterable} of {@code ObservableSource}s
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code sources} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/merge.html">ReactiveX operators documentation: Merge</a>
     * @see #mergeDelayError(Iterable)
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public static <@NonNull T> Observable<T> merge(@NonNull Iterable<@NonNull ? extends ObservableSource<? extends T>> sources) {
        return fromIterable(sources).flatMap((Function)Functions.identity());
    }

    /**
     * Flattens an {@link Iterable} of {@link ObservableSource}s into one {@code Observable}, without any transformation, while limiting the
     * number of concurrent subscriptions to these {@code ObservableSource}s.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/merge.v3.png" alt="">
     * <p>
     * You can combine the items emitted by multiple {@code ObservableSource}s so that they appear as a single {@code ObservableSource}, by
     * using the {@code merge} method.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code merge} does not operate by default on a particular {@link Scheduler}.</dd>
     *  <dt><b>Error handling:</b></dt>
     *  <dd>If any of the returned {@code ObservableSource}s signal a {@link Throwable} via {@code onError}, the resulting
     *  {@code Observable} terminates with that {@code Throwable} and all other source {@code ObservableSource}s are disposed.
     *  If more than one {@code ObservableSource} signals an error, the resulting {@code Observable} may terminate with the
     *  first one's error or, depending on the concurrency of the sources, may terminate with a
     *  {@link CompositeException} containing two or more of the various error signals.
     *  {@code Throwable}s that didn't make into the composite will be sent (individually) to the global error handler via
     *  {@link RxJavaPlugins#onError(Throwable)} method as {@link UndeliverableException} errors. Similarly, {@code Throwable}s
     *  signaled by source(s) after the returned {@code Observable} has been disposed or terminated with a
     *  (composite) error will be sent to the same global error handler.
     *  Use {@link #mergeDelayError(Iterable, int)} to merge sources and terminate only when all source {@code ObservableSource}s
     *  have completed or failed with an error.
     *  </dd>
     * </dl>
     *
     * @param <T> the common element base type
     * @param sources
     *            the {@code Iterable} of {@code ObservableSource}s
     * @param maxConcurrency
     *            the maximum number of {@code ObservableSource}s that may be subscribed to concurrently
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code sources} is {@code null}
     * @throws IllegalArgumentException
     *             if {@code maxConcurrency} is less than or equal to 0
     * @see <a href="http://reactivex.io/documentation/operators/merge.html">ReactiveX operators documentation: Merge</a>
     * @see #mergeDelayError(Iterable, int)
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public static <@NonNull T> Observable<T> merge(@NonNull Iterable<@NonNull ? extends ObservableSource<? extends T>> sources, int maxConcurrency) {
        return fromIterable(sources).flatMap((Function)Functions.identity(), maxConcurrency);
    }

    /**
     * Flattens an {@link ObservableSource} that emits {@code ObservableSource}s into a single {@code Observable} that emits the items emitted by
     * those {@code ObservableSource}s, without any transformation.
     * <p>
     * <img width="640" height="370" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/merge.oo.v3.png" alt="">
     * <p>
     * You can combine the items emitted by multiple {@code ObservableSource}s so that they appear as a single {@code ObservableSource}, by
     * using the {@code merge} method.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code merge} does not operate by default on a particular {@link Scheduler}.</dd>
     *  <dt><b>Error handling:</b></dt>
     *  <dd>If any of the returned {@code ObservableSource}s signal a {@link Throwable} via {@code onError}, the resulting
     *  {@code Observable} terminates with that {@code Throwable} and all other source {@code ObservableSource}s are disposed.
     *  If more than one {@code ObservableSource} signals an error, the resulting {@code Observable} may terminate with the
     *  first one's error or, depending on the concurrency of the sources, may terminate with a
     *  {@link CompositeException} containing two or more of the various error signals.
     *  {@code Throwable}s that didn't make into the composite will be sent (individually) to the global error handler via
     *  {@link RxJavaPlugins#onError(Throwable)} method as {@link UndeliverableException} errors. Similarly, {@code Throwable}s
     *  signaled by source(s) after the returned {@code Observable} has been disposed or terminated with a
     *  (composite) error will be sent to the same global error handler.
     *  Use {@link #mergeDelayError(ObservableSource)} to merge sources and terminate only when all source {@code ObservableSource}s
     *  have completed or failed with an error.
     *  </dd>
     * </dl>
     *
     * @param <T> the common element base type
     * @param sources
     *            an {@code ObservableSource} that emits {@code ObservableSource}s
     * @return the new {@code Observable} instance
     * @see <a href="http://reactivex.io/documentation/operators/merge.html">ReactiveX operators documentation: Merge</a>
     * @throws NullPointerException if {@code sources} is {@code null}
     * @see #mergeDelayError(ObservableSource)
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @NonNull
    public static <@NonNull T> Observable<T> merge(@NonNull ObservableSource<? extends ObservableSource<? extends T>> sources) {
        Objects.requireNonNull(sources, "sources is null");
        return RxJavaPlugins.onAssembly(new ObservableFlatMap(sources, Functions.identity(), false, Integer.MAX_VALUE, bufferSize()));
    }

    /**
     * Flattens an {@link ObservableSource} that emits {@code ObservableSource}s into a single {@code Observable} that emits the items emitted by
     * those {@code ObservableSource}s, without any transformation, while limiting the maximum number of concurrent
     * subscriptions to these {@code ObservableSource}s.
     * <p>
     * <img width="640" height="370" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/merge.oo.v3.png" alt="">
     * <p>
     * You can combine the items emitted by multiple {@code ObservableSource}s so that they appear as a single {@code ObservableSource}, by
     * using the {@code merge} method.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code merge} does not operate by default on a particular {@link Scheduler}.</dd>
     *  <dt><b>Error handling:</b></dt>
     *  <dd>If any of the returned {@code ObservableSource}s signal a {@link Throwable} via {@code onError}, the resulting
     *  {@code Observable} terminates with that {@code Throwable} and all other source {@code ObservableSource}s are disposed.
     *  If more than one {@code ObservableSource} signals an error, the resulting {@code Observable} may terminate with the
     *  first one's error or, depending on the concurrency of the sources, may terminate with a
     *  {@link CompositeException} containing two or more of the various error signals.
     *  {@code Throwable}s that didn't make into the composite will be sent (individually) to the global error handler via
     *  {@link RxJavaPlugins#onError(Throwable)} method as {@link UndeliverableException} errors. Similarly, {@code Throwable}s
     *  signaled by source(s) after the returned {@code Observable} has been disposed or terminated with a
     *  (composite) error will be sent to the same global error handler.
     *  Use {@link #mergeDelayError(ObservableSource, int)} to merge sources and terminate only when all source {@code ObservableSource}s
     *  have completed or failed with an error.
     *  </dd>
     * </dl>
     *
     * @param <T> the common element base type
     * @param sources
     *            an {@code ObservableSource} that emits {@code ObservableSource}s
     * @param maxConcurrency
     *            the maximum number of {@code ObservableSource}s that may be subscribed to concurrently
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code sources} is {@code null}
     * @throws IllegalArgumentException
     *             if {@code maxConcurrency} is non-positive
     * @see <a href="http://reactivex.io/documentation/operators/merge.html">ReactiveX operators documentation: Merge</a>
     * @since 1.1.0
     * @see #mergeDelayError(ObservableSource, int)
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public static <@NonNull T> Observable<T> merge(@NonNull ObservableSource<? extends ObservableSource<? extends T>> sources, int maxConcurrency) {
        Objects.requireNonNull(sources, "sources is null");
        ObjectHelper.verifyPositive(maxConcurrency, "maxConcurrency");
        return RxJavaPlugins.onAssembly(new ObservableFlatMap(sources, Functions.identity(), false, maxConcurrency, bufferSize()));
    }

    /**
     * Flattens two {@link ObservableSource}s into a single {@code Observable}, without any transformation.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/merge.v3.png" alt="">
     * <p>
     * You can combine items emitted by multiple {@code ObservableSource}s so that they appear as a single {@code ObservableSource}, by
     * using the {@code merge} method.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code merge} does not operate by default on a particular {@link Scheduler}.</dd>
     *  <dt><b>Error handling:</b></dt>
     *  <dd>If any of the {@code ObservableSource}s signal a {@link Throwable} via {@code onError}, the resulting
     *  {@code Observable} terminates with that {@code Throwable} and all other source {@code ObservableSource}s are disposed.
     *  If more than one {@code ObservableSource} signals an error, the resulting {@code Observable} may terminate with the
     *  first one's error or, depending on the concurrency of the sources, may terminate with a
     *  {@link CompositeException} containing two or more of the various error signals.
     *  {@code Throwable}s that didn't make into the composite will be sent (individually) to the global error handler via
     *  {@link RxJavaPlugins#onError(Throwable)} method as {@link UndeliverableException} errors. Similarly, {@code Throwable}s
     *  signaled by source(s) after the returned {@code Observable} has been disposed or terminated with a
     *  (composite) error will be sent to the same global error handler.
     *  Use {@link #mergeDelayError(ObservableSource, ObservableSource)} to merge sources and terminate only when all source {@code ObservableSource}s
     *  have completed or failed with an error.
     *  </dd>
     * </dl>
     *
     * @param <T> the common element base type
     * @param source1
     *            an {@code ObservableSource} to be merged
     * @param source2
     *            an {@code ObservableSource} to be merged
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code source1} or {@code source2} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/merge.html">ReactiveX operators documentation: Merge</a>
     * @see #mergeDelayError(ObservableSource, ObservableSource)
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public static <@NonNull T> Observable<T> merge(@NonNull ObservableSource<? extends T> source1, @NonNull ObservableSource<? extends T> source2) {
        Objects.requireNonNull(source1, "source1 is null");
        Objects.requireNonNull(source2, "source2 is null");
        return fromArray(source1, source2).flatMap((Function)Functions.identity(), false, 2);
    }

    /**
     * Flattens three {@link ObservableSource}s into a single {@code Observable}, without any transformation.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/merge.v3.png" alt="">
     * <p>
     * You can combine items emitted by multiple {@code ObservableSource}s so that they appear as a single {@code ObservableSource}, by
     * using the {@code merge} method.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code merge} does not operate by default on a particular {@link Scheduler}.</dd>
     *  <dt><b>Error handling:</b></dt>
     *  <dd>If any of the {@code ObservableSource}s signal a {@link Throwable} via {@code onError}, the resulting
     *  {@code Observable} terminates with that {@code Throwable} and all other source {@code ObservableSource}s are disposed.
     *  If more than one {@code ObservableSource} signals an error, the resulting {@code Observable} may terminate with the
     *  first one's error or, depending on the concurrency of the sources, may terminate with a
     *  {@link CompositeException} containing two or more of the various error signals.
     *  {@code Throwable}s that didn't make into the composite will be sent (individually) to the global error handler via
     *  {@link RxJavaPlugins#onError(Throwable)} method as {@link UndeliverableException} errors. Similarly, {@code Throwable}s
     *  signaled by source(s) after the returned {@code Observable} has been disposed or terminated with a
     *  (composite) error will be sent to the same global error handler.
     *  Use {@link #mergeDelayError(ObservableSource, ObservableSource, ObservableSource)} to merge sources and terminate only when all source {@code ObservableSource}s
     *  have completed or failed with an error.
     *  </dd>
     * </dl>
     *
     * @param <T> the common element base type
     * @param source1
     *            an {@code ObservableSource} to be merged
     * @param source2
     *            an {@code ObservableSource} to be merged
     * @param source3
     *            an {@code ObservableSource} to be merged
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code source1}, {@code source2} or {@code source3} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/merge.html">ReactiveX operators documentation: Merge</a>
     * @see #mergeDelayError(ObservableSource, ObservableSource, ObservableSource)
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public static <@NonNull T> Observable<T> merge(
            @NonNull ObservableSource<? extends T> source1, @NonNull ObservableSource<? extends T> source2,
            @NonNull ObservableSource<? extends T> source3) {
        Objects.requireNonNull(source1, "source1 is null");
        Objects.requireNonNull(source2, "source2 is null");
        Objects.requireNonNull(source3, "source3 is null");
        return fromArray(source1, source2, source3).flatMap((Function)Functions.identity(), false, 3);
    }

    /**
     * Flattens four {@link ObservableSource}s into a single {@code Observable}, without any transformation.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/merge.v3.png" alt="">
     * <p>
     * You can combine items emitted by multiple {@code ObservableSource}s so that they appear as a single {@code ObservableSource}, by
     * using the {@code merge} method.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code merge} does not operate by default on a particular {@link Scheduler}.</dd>
     *  <dt><b>Error handling:</b></dt>
     *  <dd>If any of the {@code ObservableSource}s signal a {@link Throwable} via {@code onError}, the resulting
     *  {@code Observable} terminates with that {@code Throwable} and all other source {@code ObservableSource}s are disposed.
     *  If more than one {@code ObservableSource} signals an error, the resulting {@code Observable} may terminate with the
     *  first one's error or, depending on the concurrency of the sources, may terminate with a
     *  {@link CompositeException} containing two or more of the various error signals.
     *  {@code Throwable}s that didn't make into the composite will be sent (individually) to the global error handler via
     *  {@link RxJavaPlugins#onError(Throwable)} method as {@link UndeliverableException} errors. Similarly, {@code Throwable}s
     *  signaled by source(s) after the returned {@code Observable} has been disposed or terminated with a
     *  (composite) error will be sent to the same global error handler.
     *  Use {@link #mergeDelayError(ObservableSource, ObservableSource, ObservableSource, ObservableSource)} to merge sources and terminate only when all source {@code ObservableSource}s
     *  have completed or failed with an error.
     *  </dd>
     * </dl>
     *
     * @param <T> the common element base type
     * @param source1
     *            an {@code ObservableSource} to be merged
     * @param source2
     *            an {@code ObservableSource} to be merged
     * @param source3
     *            an {@code ObservableSource} to be merged
     * @param source4
     *            an {@code ObservableSource} to be merged
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code source1}, {@code source2}, {@code source3} or {@code source4} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/merge.html">ReactiveX operators documentation: Merge</a>
     * @see #mergeDelayError(ObservableSource, ObservableSource, ObservableSource, ObservableSource)
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public static <@NonNull T> Observable<T> merge(
            @NonNull ObservableSource<? extends T> source1, @NonNull ObservableSource<? extends T> source2,
            @NonNull ObservableSource<? extends T> source3, @NonNull ObservableSource<? extends T> source4) {
        Objects.requireNonNull(source1, "source1 is null");
        Objects.requireNonNull(source2, "source2 is null");
        Objects.requireNonNull(source3, "source3 is null");
        Objects.requireNonNull(source4, "source4 is null");
        return fromArray(source1, source2, source3, source4).flatMap((Function)Functions.identity(), false, 4);
    }

    /**
     * Flattens an array of {@link ObservableSource}s into one {@code Observable}, without any transformation.
     * <p>
     * <img width="640" height="370" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/merge.io.v3.png" alt="">
     * <p>
     * You can combine items emitted by multiple {@code ObservableSource}s so that they appear as a single {@code ObservableSource}, by
     * using the {@code merge} method.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code mergeArray} does not operate by default on a particular {@link Scheduler}.</dd>
     *  <dt><b>Error handling:</b></dt>
     *  <dd>If any of the {@code ObservableSource}s signal a {@link Throwable} via {@code onError}, the resulting
     *  {@code Observable} terminates with that {@code Throwable} and all other source {@code ObservableSource}s are disposed.
     *  If more than one {@code ObservableSource} signals an error, the resulting {@code Observable} may terminate with the
     *  first one's error or, depending on the concurrency of the sources, may terminate with a
     *  {@link CompositeException} containing two or more of the various error signals.
     *  {@code Throwable}s that didn't make into the composite will be sent (individually) to the global error handler via
     *  {@link RxJavaPlugins#onError(Throwable)} method as {@link UndeliverableException} errors. Similarly, {@code Throwable}s
     *  signaled by source(s) after the returned {@code Observable} has been disposed or terminated with a
     *  (composite) error will be sent to the same global error handler.
     *  Use {@link #mergeArrayDelayError(ObservableSource...)} to merge sources and terminate only when all source {@code ObservableSource}s
     *  have completed or failed with an error.
     *  </dd>
     * </dl>
     *
     * @param <T> the common element base type
     * @param sources
     *            the array of {@code ObservableSource}s
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code sources} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/merge.html">ReactiveX operators documentation: Merge</a>
     * @see #mergeArrayDelayError(ObservableSource...)
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    @SafeVarargs
    public static <@NonNull T> Observable<T> mergeArray(@NonNull ObservableSource<? extends T>... sources) {
        return fromArray(sources).flatMap((Function)Functions.identity(), sources.length);
    }

    /**
     * Flattens an {@link Iterable} of {@link ObservableSource}s into one {@code Observable}, in a way that allows an {@link Observer} to receive all
     * successfully emitted items from each of the returned {@code ObservableSource}s without being interrupted by an error
     * notification from one of them.
     * <p>
     * This behaves like {@link #merge(ObservableSource)} except that if any of the merged {@code ObservableSource}s notify of an
     * error via {@link Observer#onError onError}, {@code mergeDelayError} will refrain from propagating that
     * error notification until all of the merged {@code ObservableSource}s have finished emitting items.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/mergeDelayError.v3.png" alt="">
     * <p>
     * Even if multiple merged {@code ObservableSource}s send {@code onError} notifications, {@code mergeDelayError} will only
     * invoke the {@code onError} method of its {@code Observer}s once.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code mergeDelayError} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T> the common element base type
     * @param sources
     *            the {@code Iterable} of {@code ObservableSource}s
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code sources} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/merge.html">ReactiveX operators documentation: Merge</a>
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public static <@NonNull T> Observable<T> mergeDelayError(@NonNull Iterable<@NonNull ? extends ObservableSource<? extends T>> sources) {
        return fromIterable(sources).flatMap((Function)Functions.identity(), true);
    }

    /**
     * Flattens an {@link Iterable} of {@link ObservableSource}s into one {@code Observable}, in a way that allows an {@link Observer} to receive all
     * successfully emitted items from each of the returned {@code ObservableSource}s without being interrupted by an error
     * notification from one of them, while limiting the number of concurrent subscriptions to these {@code ObservableSource}s.
     * <p>
     * This behaves like {@link #merge(ObservableSource)} except that if any of the merged {@code ObservableSource}s notify of an
     * error via {@link Observer#onError onError}, {@code mergeDelayError} will refrain from propagating that
     * error notification until all of the merged {@code ObservableSource}s have finished emitting items.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/mergeDelayError.v3.png" alt="">
     * <p>
     * Even if multiple merged {@code ObservableSource}s send {@code onError} notifications, {@code mergeDelayError} will only
     * invoke the {@code onError} method of its {@code Observer}s once.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code mergeDelayError} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T> the common element base type
     * @param sources
     *            the {@code Iterable} of {@code ObservableSource}s
     * @param maxConcurrency
     *            the maximum number of {@code ObservableSource}s that may be subscribed to concurrently
     * @param bufferSize
     *            the number of items expected from each inner {@code ObservableSource} to be buffered
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code sources} is {@code null}
     * @throws IllegalArgumentException if {@code maxConcurrency} or {@code bufferSize} is non-positive
     * @see <a href="http://reactivex.io/documentation/operators/merge.html">ReactiveX operators documentation: Merge</a>
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public static <@NonNull T> Observable<T> mergeDelayError(@NonNull Iterable<@NonNull ? extends ObservableSource<? extends T>> sources, int maxConcurrency, int bufferSize) {
        return fromIterable(sources).flatMap((Function)Functions.identity(), true, maxConcurrency, bufferSize);
    }

    /**
     * Flattens an array of {@link ObservableSource}s into one {@code Observable}, in a way that allows an {@link Observer} to receive all
     * successfully emitted items from each of the {@code ObservableSource}s without being interrupted by an error
     * notification from one of them, while limiting the number of concurrent subscriptions to these {@code ObservableSource}s.
     * <p>
     * This behaves like {@link #merge(ObservableSource)} except that if any of the merged {@code ObservableSource}s notify of an
     * error via {@link Observer#onError onError}, {@code mergeDelayError} will refrain from propagating that
     * error notification until all of the merged {@code ObservableSource}s have finished emitting items.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/mergeDelayError.v3.png" alt="">
     * <p>
     * Even if multiple merged {@code ObservableSource}s send {@code onError} notifications, {@code mergeDelayError} will only
     * invoke the {@code onError} method of its {@code Observer}s once.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code mergeArrayDelayError} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T> the common element base type
     * @param sources
     *            the array of {@code ObservableSource}s
     * @param maxConcurrency
     *            the maximum number of {@code ObservableSource}s that may be subscribed to concurrently
     * @param bufferSize
     *            the number of items expected from each inner {@code ObservableSource} to be buffered
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code sources} is {@code null}
     * @throws IllegalArgumentException if {@code maxConcurrency} or {@code bufferSize} is non-positive
     * @see <a href="http://reactivex.io/documentation/operators/merge.html">ReactiveX operators documentation: Merge</a>
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    @SafeVarargs
    public static <@NonNull T> Observable<T> mergeArrayDelayError(int maxConcurrency, int bufferSize, @NonNull ObservableSource<? extends T>... sources) {
        return fromArray(sources).flatMap((Function)Functions.identity(), true, maxConcurrency, bufferSize);
    }

    /**
     * Flattens an {@link Iterable} of {@link ObservableSource}s into one {@code Observable}, in a way that allows an {@link Observer} to receive all
     * successfully emitted items from each of the returned {@code ObservableSource}s without being interrupted by an error
     * notification from one of them, while limiting the number of concurrent subscriptions to these {@code ObservableSource}s.
     * <p>
     * This behaves like {@link #merge(ObservableSource)} except that if any of the merged {@code ObservableSource}s notify of an
     * error via {@link Observer#onError onError}, {@code mergeDelayError} will refrain from propagating that
     * error notification until all of the merged {@code ObservableSource}s have finished emitting items.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/mergeDelayError.v3.png" alt="">
     * <p>
     * Even if multiple merged {@code ObservableSource}s send {@code onError} notifications, {@code mergeDelayError} will only
     * invoke the {@code onError} method of its {@code Observer}s once.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code mergeDelayError} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T> the common element base type
     * @param sources
     *            the {@code Iterable} of {@code ObservableSource}s
     * @param maxConcurrency
     *            the maximum number of {@code ObservableSource}s that may be subscribed to concurrently
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code sources} is {@code null}
     * @throws IllegalArgumentException if {@code maxConcurrency} is non-positive
     * @see <a href="http://reactivex.io/documentation/operators/merge.html">ReactiveX operators documentation: Merge</a>
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public static <@NonNull T> Observable<T> mergeDelayError(@NonNull Iterable<@NonNull ? extends ObservableSource<? extends T>> sources, int maxConcurrency) {
        return fromIterable(sources).flatMap((Function)Functions.identity(), true, maxConcurrency);
    }

    /**
     * Flattens an {@link ObservableSource} that emits {@code ObservableSource}s into one {@code Observable}, in a way that allows an {@link Observer} to
     * receive all successfully emitted items from all of the emitted {@code ObservableSource}s without being interrupted by
     * an error notification from one of them.
     * <p>
     * This behaves like {@link #merge(ObservableSource)} except that if any of the merged {@code ObservableSource}s notify of an
     * error via {@link Observer#onError onError}, {@code mergeDelayError} will refrain from propagating that
     * error notification until all of the merged {@code ObservableSource}s have finished emitting items.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/mergeDelayError.v3.png" alt="">
     * <p>
     * Even if multiple merged {@code ObservableSource}s send {@code onError} notifications, {@code mergeDelayError} will only
     * invoke the {@code onError} method of its {@code Observer}s once.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code mergeDelayError} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T> the common element base type
     * @param sources
     *            an {@code ObservableSource} that emits {@code ObservableSource}s
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code sources} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/merge.html">ReactiveX operators documentation: Merge</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @NonNull
    public static <@NonNull T> Observable<T> mergeDelayError(@NonNull ObservableSource<? extends ObservableSource<? extends T>> sources) {
        Objects.requireNonNull(sources, "sources is null");
        return RxJavaPlugins.onAssembly(new ObservableFlatMap(sources, Functions.identity(), true, Integer.MAX_VALUE, bufferSize()));
    }

    /**
     * Flattens an {@link ObservableSource} that emits {@code ObservableSource}s into one {@code Observable}, in a way that allows an {@link Observer} to
     * receive all successfully emitted items from all of the emitted {@code ObservableSource}s without being interrupted by
     * an error notification from one of them, while limiting the
     * number of concurrent subscriptions to these {@code ObservableSource}s.
     * <p>
     * This behaves like {@link #merge(ObservableSource)} except that if any of the merged {@code ObservableSource}s notify of an
     * error via {@link Observer#onError onError}, {@code mergeDelayError} will refrain from propagating that
     * error notification until all of the merged {@code ObservableSource}s have finished emitting items.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/mergeDelayError.v3.png" alt="">
     * <p>
     * Even if multiple merged {@code ObservableSource}s send {@code onError} notifications, {@code mergeDelayError} will only
     * invoke the {@code onError} method of its {@code Observer}s once.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code mergeDelayError} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T> the common element base type
     * @param sources
     *            an {@code ObservableSource} that emits {@code ObservableSource}s
     * @param maxConcurrency
     *            the maximum number of {@code ObservableSource}s that may be subscribed to concurrently
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code sources} is {@code null}
     * @throws IllegalArgumentException if {@code maxConcurrency} is non-positive
     * @see <a href="http://reactivex.io/documentation/operators/merge.html">ReactiveX operators documentation: Merge</a>
     * @since 2.0
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public static <@NonNull T> Observable<T> mergeDelayError(@NonNull ObservableSource<? extends ObservableSource<? extends T>> sources, int maxConcurrency) {
        Objects.requireNonNull(sources, "sources is null");
        ObjectHelper.verifyPositive(maxConcurrency, "maxConcurrency");
        return RxJavaPlugins.onAssembly(new ObservableFlatMap(sources, Functions.identity(), true, maxConcurrency, bufferSize()));
    }

    /**
     * Flattens two {@link ObservableSource}s into one {@code Observable}, in a way that allows an {@link Observer} to receive all
     * successfully emitted items from each of the {@code ObservableSource}s without being interrupted by an error
     * notification from one of them.
     * <p>
     * This behaves like {@link #merge(ObservableSource, ObservableSource)} except that if any of the merged {@code ObservableSource}s
     * notify of an error via {@link Observer#onError onError}, {@code mergeDelayError} will refrain from
     * propagating that error notification until all of the merged {@code ObservableSource}s have finished emitting items.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/mergeDelayError.v3.png" alt="">
     * <p>
     * Even if both merged {@code ObservableSource}s send {@code onError} notifications, {@code mergeDelayError} will only
     * invoke the {@code onError} method of its {@code Observer}s once.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code mergeDelayError} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T> the common element base type
     * @param source1
     *            an {@code ObservableSource} to be merged
     * @param source2
     *            an {@code ObservableSource} to be merged
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code source1} or {@code source2} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/merge.html">ReactiveX operators documentation: Merge</a>
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public static <@NonNull T> Observable<T> mergeDelayError(
            @NonNull ObservableSource<? extends T> source1, @NonNull ObservableSource<? extends T> source2) {
        Objects.requireNonNull(source1, "source1 is null");
        Objects.requireNonNull(source2, "source2 is null");
        return fromArray(source1, source2).flatMap((Function)Functions.identity(), true, 2);
    }

    /**
     * Flattens three {@link ObservableSource}s into one {@code Observable}, in a way that allows an {@link Observer} to receive all
     * successfully emitted items from all of the {@code ObservableSource}s without being interrupted by an error
     * notification from one of them.
     * <p>
     * This behaves like {@link #merge(ObservableSource, ObservableSource, ObservableSource)} except that if any of the merged
     * {@code ObservableSource}s notify of an error via {@link Observer#onError onError}, {@code mergeDelayError} will refrain
     * from propagating that error notification until all of the merged {@code ObservableSource}s have finished emitting
     * items.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/mergeDelayError.v3.png" alt="">
     * <p>
     * Even if multiple merged {@code ObservableSource}s send {@code onError} notifications, {@code mergeDelayError} will only
     * invoke the {@code onError} method of its {@code Observer}s once.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code mergeDelayError} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T> the common element base type
     * @param source1
     *            an {@code ObservableSource} to be merged
     * @param source2
     *            an {@code ObservableSource} to be merged
     * @param source3
     *            an {@code ObservableSource} to be merged
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code source1}, {@code source2} or {@code source3} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/merge.html">ReactiveX operators documentation: Merge</a>
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public static <@NonNull T> Observable<T> mergeDelayError(
            @NonNull ObservableSource<? extends T> source1, @NonNull ObservableSource<? extends T> source2,
            @NonNull ObservableSource<? extends T> source3) {
        Objects.requireNonNull(source1, "source1 is null");
        Objects.requireNonNull(source2, "source2 is null");
        Objects.requireNonNull(source3, "source3 is null");
        return fromArray(source1, source2, source3).flatMap((Function)Functions.identity(), true, 3);
    }

    /**
     * Flattens four {@link ObservableSource}s into one {@code Observable}, in a way that allows an {@link Observer} to receive all
     * successfully emitted items from all of the {@code ObservableSource}s without being interrupted by an error
     * notification from one of them.
     * <p>
     * This behaves like {@link #merge(ObservableSource, ObservableSource, ObservableSource, ObservableSource)} except that if any of
     * the merged {@code ObservableSource}s notify of an error via {@link Observer#onError onError}, {@code mergeDelayError}
     * will refrain from propagating that error notification until all of the merged {@code ObservableSource}s have finished
     * emitting items.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/mergeDelayError.v3.png" alt="">
     * <p>
     * Even if multiple merged {@code ObservableSource}s send {@code onError} notifications, {@code mergeDelayError} will only
     * invoke the {@code onError} method of its {@code Observer}s once.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code mergeDelayError} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T> the common element base type
     * @param source1
     *            an {@code ObservableSource} to be merged
     * @param source2
     *            an {@code ObservableSource} to be merged
     * @param source3
     *            an {@code ObservableSource} to be merged
     * @param source4
     *            an {@code ObservableSource} to be merged
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code source1}, {@code source2}, {@code source3} or {@code source4} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/merge.html">ReactiveX operators documentation: Merge</a>
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public static <@NonNull T> Observable<T> mergeDelayError(
            @NonNull ObservableSource<? extends T> source1, @NonNull ObservableSource<? extends T> source2,
            @NonNull ObservableSource<? extends T> source3, @NonNull ObservableSource<? extends T> source4) {
        Objects.requireNonNull(source1, "source1 is null");
        Objects.requireNonNull(source2, "source2 is null");
        Objects.requireNonNull(source3, "source3 is null");
        Objects.requireNonNull(source4, "source4 is null");
        return fromArray(source1, source2, source3, source4).flatMap((Function)Functions.identity(), true, 4);
    }

    /**
     * Flattens an array of {@link ObservableSource}s into one {@code Observable}, in a way that allows an {@link Observer} to receive all
     * successfully emitted items from each of the {@code ObservableSource}s without being interrupted by an error
     * notification from one of them.
     * <p>
     * This behaves like {@link #merge(ObservableSource)} except that if any of the merged {@code ObservableSource}s notify of an
     * error via {@link Observer#onError onError}, {@code mergeDelayError} will refrain from propagating that
     * error notification until all of the merged {@code ObservableSource}s have finished emitting items.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/mergeDelayError.v3.png" alt="">
     * <p>
     * Even if multiple merged {@code ObservableSource}s send {@code onError} notifications, {@code mergeDelayError} will only
     * invoke the {@code onError} method of its {@code Observer}s once.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code mergeArrayDelayError} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T> the common element base type
     * @param sources
     *            the array of {@code ObservableSource}s
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code sources} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/merge.html">ReactiveX operators documentation: Merge</a>
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    @SafeVarargs
    public static <@NonNull T> Observable<T> mergeArrayDelayError(@NonNull ObservableSource<? extends T>... sources) {
        return fromArray(sources).flatMap((Function)Functions.identity(), true, sources.length);
    }

    /**
     * Returns an {@code Observable} that never sends any items or notifications to an {@link Observer}.
     * <p>
     * <img width="640" height="185" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/never.v3.png" alt="">
     * <p>
     * The returned {@code Observable} is useful primarily for testing purposes.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code never} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T>
     *            the type of items (not) emitted by the {@code Observable}
     * @return the shared {@code Observable} instance
     * @see <a href="http://reactivex.io/documentation/operators/empty-never-throw.html">ReactiveX operators documentation: Never</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @SuppressWarnings("unchecked")
    @NonNull
    public static <@NonNull T> Observable<T> never() {
        return RxJavaPlugins.onAssembly((Observable<T>) ObservableNever.INSTANCE);
    }

    /**
     * Returns an {@code Observable} that emits a sequence of {@link Integer}s within a specified range.
     * <p>
     * <img width="640" height="195" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/range.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code range} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param start
     *            the value of the first {@code Integer} in the sequence
     * @param count
     *            the number of sequential {@code Integer}s to generate
     * @return the new {@code Observable} instance
     * @throws IllegalArgumentException
     *             if {@code count} is negative, or if {@code start} + {@code count} &minus; 1 exceeds
     *             {@link Integer#MAX_VALUE}
     * @see <a href="http://reactivex.io/documentation/operators/range.html">ReactiveX operators documentation: Range</a>
     * @see #rangeLong(long, long)
     * @see #intervalRange(long, long, long, long, TimeUnit)
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public static Observable<Integer> range(int start, int count) {
        if (count < 0) {
            throw new IllegalArgumentException("count >= 0 required but it was " + count);
        }
        if (count == 0) {
            return empty();
        }
        if (count == 1) {
            return just(start);
        }
        if ((long)start + (count - 1) > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Integer overflow");
        }
        return RxJavaPlugins.onAssembly(new ObservableRange(start, count));
    }

    /**
     * Returns an {@code Observable} that emits a sequence of {@link Long}s within a specified range.
     * <p>
     * <img width="640" height="195" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/rangeLong.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code rangeLong} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param start
     *            the value of the first {@code Long} in the sequence
     * @param count
     *            the number of sequential {@code Long}s to generate
     * @return the new {@code Observable} instance
     * @throws IllegalArgumentException
     *             if {@code count} is negative, or if {@code start} + {@code count} &minus; 1 exceeds
     *             {@link Long#MAX_VALUE}
     * @see <a href="http://reactivex.io/documentation/operators/range.html">ReactiveX operators documentation: Range</a>
     * @see #intervalRange(long, long, long, long, TimeUnit)
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public static Observable<Long> rangeLong(long start, long count) {
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

        return RxJavaPlugins.onAssembly(new ObservableRangeLong(start, count));
    }

    /**
     * Returns a {@link Single} that emits a {@link Boolean} value that indicates whether two {@link ObservableSource} sequences are the
     * same by comparing the items emitted by each {@code ObservableSource} pairwise.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/sequenceEqual.2.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code sequenceEqual} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param source1
     *            the first {@code ObservableSource} to compare
     * @param source2
     *            the second {@code ObservableSource} to compare
     * @param <T>
     *            the type of items emitted by each {@code ObservableSource}
     * @return the new {@code Single} instance
     * @throws NullPointerException if {@code source1} or {@code source2} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/sequenceequal.html">ReactiveX operators documentation: SequenceEqual</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public static <@NonNull T> Single<Boolean> sequenceEqual(@NonNull ObservableSource<? extends T> source1, @NonNull ObservableSource<? extends T> source2) {
        return sequenceEqual(source1, source2, ObjectHelper.equalsPredicate(), bufferSize());
    }

    /**
     * Returns a {@link Single} that emits a {@link Boolean} value that indicates whether two {@link ObservableSource} sequences are the
     * same by comparing the items emitted by each {@code ObservableSource} pairwise based on the results of a specified
     * equality function.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/sequenceEqual.2.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code sequenceEqual} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param source1
     *            the first {@code ObservableSource} to compare
     * @param source2
     *            the second {@code ObservableSource} to compare
     * @param isEqual
     *            a function used to compare items emitted by each {@code ObservableSource}
     * @param <T>
     *            the type of items emitted by each {@code ObservableSource}
     * @return the new {@code Single} instance
     * @throws NullPointerException if {@code source1}, {@code source2} or {@code isEqual} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/sequenceequal.html">ReactiveX operators documentation: SequenceEqual</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public static <@NonNull T> Single<Boolean> sequenceEqual(
            @NonNull ObservableSource<? extends T> source1, @NonNull ObservableSource<? extends T> source2,
            @NonNull BiPredicate<? super T, ? super T> isEqual) {
        return sequenceEqual(source1, source2, isEqual, bufferSize());
    }

    /**
     * Returns a {@link Single} that emits a {@link Boolean} value that indicates whether two {@link ObservableSource} sequences are the
     * same by comparing the items emitted by each {@code ObservableSource} pairwise based on the results of a specified
     * equality function.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/sequenceEqual.2.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code sequenceEqual} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param source1
     *            the first {@code ObservableSource} to compare
     * @param source2
     *            the second {@code ObservableSource} to compare
     * @param isEqual
     *            a function used to compare items emitted by each {@code ObservableSource}
     * @param bufferSize
     *            the number of items expected from the first and second source {@code ObservableSource} to be buffered
     * @param <T>
     *            the type of items emitted by each {@code ObservableSource}
     * @return the new {@code Single} instance
     * @throws NullPointerException if {@code source1}, {@code source2} or {@code isEqual} is {@code null}
     * @throws IllegalArgumentException if {@code bufferSize} is non-positive
     * @see <a href="http://reactivex.io/documentation/operators/sequenceequal.html">ReactiveX operators documentation: SequenceEqual</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public static <@NonNull T> Single<Boolean> sequenceEqual(
            @NonNull ObservableSource<? extends T> source1, @NonNull ObservableSource<? extends T> source2,
            @NonNull BiPredicate<? super T, ? super T> isEqual, int bufferSize) {
        Objects.requireNonNull(source1, "source1 is null");
        Objects.requireNonNull(source2, "source2 is null");
        Objects.requireNonNull(isEqual, "isEqual is null");
        ObjectHelper.verifyPositive(bufferSize, "bufferSize");
        return RxJavaPlugins.onAssembly(new ObservableSequenceEqualSingle<>(source1, source2, isEqual, bufferSize));
    }

    /**
     * Returns a {@link Single} that emits a {@link Boolean} value that indicates whether two {@link ObservableSource} sequences are the
     * same by comparing the items emitted by each {@code ObservableSource} pairwise.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/sequenceEqual.2.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code sequenceEqual} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param source1
     *            the first {@code ObservableSource} to compare
     * @param source2
     *            the second {@code ObservableSource} to compare
     * @param bufferSize
     *            the number of items expected from the first and second source {@code ObservableSource} to be buffered
     * @param <T>
     *            the type of items emitted by each {@code ObservableSource}
     * @return the new {@code Single} instance
     * @throws NullPointerException if {@code source1} or {@code source2} is {@code null}
     * @throws IllegalArgumentException if {@code bufferSize} is non-positive
     * @see <a href="http://reactivex.io/documentation/operators/sequenceequal.html">ReactiveX operators documentation: SequenceEqual</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public static <@NonNull T> Single<Boolean> sequenceEqual(@NonNull ObservableSource<? extends T> source1, @NonNull ObservableSource<? extends T> source2,
            int bufferSize) {
        return sequenceEqual(source1, source2, ObjectHelper.equalsPredicate(), bufferSize);
    }

    /**
     * Converts an {@link ObservableSource} that emits {@code ObservableSource}s into an {@code Observable} that emits the items emitted by the
     * most recently emitted of those {@code ObservableSource}s.
     * <p>
     * <img width="640" height="370" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/switchDo.v3.png" alt="">
     * <p>
     * {@code switchOnNext} subscribes to an {@code ObservableSource} that emits {@code ObservableSource}s. Each time it observes one of
     * these emitted {@code ObservableSource}s, the {@code ObservableSource} returned by {@code switchOnNext} begins emitting the items
     * emitted by that {@code ObservableSource}. When a new inner {@code ObservableSource} is emitted, {@code switchOnNext} stops emitting items
     * from the earlier-emitted {@code ObservableSource} and begins emitting items from the new one.
     * <p>
     * The resulting {@code Observable} completes if both the outer {@code ObservableSource} and the last inner {@code ObservableSource}, if any, complete.
     * If the outer {@code ObservableSource} signals an {@code onError}, the inner {@code ObservableSource} is disposed and the error delivered in-sequence.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code switchOnNext} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T> the item type
     * @param sources
     *            the {@code ObservableSource} that emits {@code ObservableSource}s
     * @param bufferSize
     *            the expected number of items to cache from the inner {@code ObservableSource}s
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code sources} is {@code null}
     * @throws IllegalArgumentException if {@code bufferSize} is non-positive
     * @see <a href="http://reactivex.io/documentation/operators/switch.html">ReactiveX operators documentation: Switch</a>
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public static <@NonNull T> Observable<T> switchOnNext(@NonNull ObservableSource<? extends ObservableSource<? extends T>> sources, int bufferSize) {
        Objects.requireNonNull(sources, "sources is null");
        ObjectHelper.verifyPositive(bufferSize, "bufferSize");
        return RxJavaPlugins.onAssembly(new ObservableSwitchMap(sources, Functions.identity(), bufferSize, false));
    }

    /**
     * Converts an {@link ObservableSource} that emits {@code ObservableSource}s into an {@code Observable} that emits the items emitted by the
     * most recently emitted of those {@code ObservableSource}s.
     * <p>
     * <img width="640" height="370" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/switchDo.v3.png" alt="">
     * <p>
     * {@code switchOnNext} subscribes to an {@code ObservableSource} that emits {@code ObservableSource}s. Each time it observes one of
     * these emitted {@code ObservableSource}s, the {@code ObservableSource} returned by {@code switchOnNext} begins emitting the items
     * emitted by that {@code ObservableSource}. When a new inner {@code ObservableSource} is emitted, {@code switchOnNext} stops emitting items
     * from the earlier-emitted {@code ObservableSource} and begins emitting items from the new one.
     * <p>
     * The resulting {@code Observable} completes if both the outer {@code ObservableSource} and the last inner {@code ObservableSource}, if any, complete.
     * If the outer {@code ObservableSource} signals an {@code onError}, the inner {@code ObservableSource} is disposed and the error delivered in-sequence.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code switchOnNext} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T> the item type
     * @param sources
     *            the {@code ObservableSource} that emits {@code ObservableSource}s
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code sources} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/switch.html">ReactiveX operators documentation: Switch</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public static <@NonNull T> Observable<T> switchOnNext(@NonNull ObservableSource<? extends ObservableSource<? extends T>> sources) {
        return switchOnNext(sources, bufferSize());
    }

    /**
     * Converts an {@link ObservableSource} that emits {@code ObservableSource}s into an {@code Observable} that emits the items emitted by the
     * most recently emitted of those {@code ObservableSource}s and delays any exception until all {@code ObservableSource}s terminate.
     * <p>
     * <img width="640" height="370" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/switchOnNextDelayError.v3.png" alt="">
     * <p>
     * {@code switchOnNext} subscribes to an {@code ObservableSource} that emits {@code ObservableSource}s. Each time it observes one of
     * these emitted {@code ObservableSource}s, the {@code ObservableSource} returned by {@code switchOnNext} begins emitting the items
     * emitted by that {@code ObservableSource}. When a new inner {@code ObservableSource} is emitted, {@code switchOnNext} stops emitting items
     * from the earlier-emitted {@code ObservableSource} and begins emitting items from the new one.
     * <p>
     * The resulting {@code Observable} completes if both the main {@code ObservableSource} and the last inner {@code ObservableSource}, if any, complete.
     * If the main {@code ObservableSource} signals an {@code onError}, the termination of the last inner {@code ObservableSource} will emit that error as is
     * or wrapped into a {@link CompositeException} along with the other possible errors the former inner {@code ObservableSource}s signaled.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code switchOnNextDelayError} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T> the item type
     * @param sources
     *            the {@code ObservableSource} that emits {@code ObservableSource}s
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code sources} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/switch.html">ReactiveX operators documentation: Switch</a>
     * @since 2.0
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public static <@NonNull T> Observable<T> switchOnNextDelayError(@NonNull ObservableSource<? extends ObservableSource<? extends T>> sources) {
        return switchOnNextDelayError(sources, bufferSize());
    }

    /**
     * Converts an {@link ObservableSource} that emits {@code ObservableSource}s into an {@code Observable} that emits the items emitted by the
     * most recently emitted of those {@code ObservableSource}s and delays any exception until all {@code ObservableSource}s terminate.
     * <p>
     * <img width="640" height="370" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/switchOnNextDelayError.v3.png" alt="">
     * <p>
     * {@code switchOnNext} subscribes to an {@code ObservableSource} that emits {@code ObservableSource}s. Each time it observes one of
     * these emitted {@code ObservableSource}s, the {@code ObservableSource} returned by {@code switchOnNext} begins emitting the items
     * emitted by that {@code ObservableSource}. When a new inner {@code ObservableSource} is emitted, {@code switchOnNext} stops emitting items
     * from the earlier-emitted {@code ObservableSource} and begins emitting items from the new one.
     * <p>
     * The resulting {@code Observable} completes if both the main {@code ObservableSource} and the last inner {@code ObservableSource}, if any, complete.
     * If the main {@code ObservableSource} signals an {@code onError}, the termination of the last inner {@code ObservableSource} will emit that error as is
     * or wrapped into a {@link CompositeException} along with the other possible errors the former inner {@code ObservableSource}s signaled.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code switchOnNextDelayError} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T> the item type
     * @param sources
     *            the {@code ObservableSource} that emits {@code ObservableSource}s
     * @param bufferSize
     *            the expected number of items to cache from the inner {@code ObservableSource}s
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code sources} is {@code null}
     * @throws IllegalArgumentException if {@code bufferSize} is non-positive
     * @see <a href="http://reactivex.io/documentation/operators/switch.html">ReactiveX operators documentation: Switch</a>
     * @since 2.0
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public static <@NonNull T> Observable<T> switchOnNextDelayError(@NonNull ObservableSource<? extends ObservableSource<? extends T>> sources, int bufferSize) {
        Objects.requireNonNull(sources, "sources is null");
        ObjectHelper.verifyPositive(bufferSize, "bufferSize");
        return RxJavaPlugins.onAssembly(new ObservableSwitchMap(sources, Functions.identity(), bufferSize, true));
    }

    /**
     * Returns an {@code Observable} that emits {@code 0L} after a specified delay, and then completes.
     * <p>
     * <img width="640" height="200" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/timer.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code timer} operates by default on the {@code computation} {@link Scheduler}.</dd>
     * </dl>
     *
     * @param delay
     *            the initial delay before emitting a single {@code 0L}
     * @param unit
     *            time units to use for {@code delay}
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code unit} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/timer.html">ReactiveX operators documentation: Timer</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.COMPUTATION)
    @NonNull
    public static Observable<Long> timer(long delay, @NonNull TimeUnit unit) {
        return timer(delay, unit, Schedulers.computation());
    }

    /**
     * Returns an {@code Observable} that emits {@code 0L} after a specified delay, on a specified {@link Scheduler}, and then
     * completes.
     * <p>
     * <img width="640" height="200" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/timer.s.v3.png" alt="">
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
     * @throws NullPointerException
     *             if {@code unit} or {@code scheduler} is {@code null}
     * @return the new {@code Observable} instance
     * @see <a href="http://reactivex.io/documentation/operators/timer.html">ReactiveX operators documentation: Timer</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    @NonNull
    public static Observable<Long> timer(long delay, @NonNull TimeUnit unit, @NonNull Scheduler scheduler) {
        Objects.requireNonNull(unit, "unit is null");
        Objects.requireNonNull(scheduler, "scheduler is null");

        return RxJavaPlugins.onAssembly(new ObservableTimer(Math.max(delay, 0L), unit, scheduler));
    }

    /**
     * Create an {@code Observable} by wrapping an {@link ObservableSource} <em>which has to be implemented according
     * to the {@code Observable} specification derived from the <b>Reactive Streams</b> specification by handling
     * disposal correctly; no safeguards are provided by the {@code Observable} itself</em>.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code unsafeCreate} by default doesn't operate on any particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the value type emitted
     * @param onSubscribe the {@code ObservableSource} instance to wrap
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code onSubscribe} is {@code null}
     * @throws IllegalArgumentException if the {@code onSubscribe} is already an {@code Observable}, use
     *                                  {@link #wrap(ObservableSource)} in this case
     * @see #wrap(ObservableSource)
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public static <@NonNull T> Observable<T> unsafeCreate(@NonNull ObservableSource<T> onSubscribe) {
        Objects.requireNonNull(onSubscribe, "onSubscribe is null");
        if (onSubscribe instanceof Observable) {
            throw new IllegalArgumentException("unsafeCreate(Observable) should be upgraded");
        }
        return RxJavaPlugins.onAssembly(new ObservableFromUnsafeSource<>(onSubscribe));
    }

    /**
     * Constructs an {@code Observable} that creates a dependent resource object, an {@link ObservableSource} with
     * that resource and calls the provided {@code resourceDisposer} function if this inner source terminates or the
     * downstream disposes the flow.
     * <p>
     * <img width="640" height="400" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/using.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code using} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T> the element type of the generated {@code Observable}
     * @param <D> the type of the resource associated with the output sequence
     * @param resourceSupplier
     *            the factory function to create a resource object that depends on the {@code ObservableSource}
     * @param sourceSupplier
     *            the factory function to create an {@code ObservableSource}
     * @param resourceCleanup
     *            the function that will dispose of the resource
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code resourceSupplier}, {@code sourceSupplier} or {@code resourceCleanup} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/using.html">ReactiveX operators documentation: Using</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public static <@NonNull T, @NonNull D> Observable<T> using(
            @NonNull Supplier<? extends D> resourceSupplier,
            @NonNull Function<? super D, ? extends ObservableSource<? extends T>> sourceSupplier,
            @NonNull Consumer<? super D> resourceCleanup) {
        return using(resourceSupplier, sourceSupplier, resourceCleanup, true);
    }

    /**
     * Constructs an {@code Observable} that creates a dependent resource object, an {@link ObservableSource} with
     * that resource and calls the provided {@code disposer} function if this inner source terminates or the
     * downstream disposes the flow; doing it before these end-states have been reached if {@code eager == true}, after otherwise.
     * <p>
     * <img width="640" height="400" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/using.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code using} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T> the element type of the generated {@code ObservableSource}
     * @param <D> the type of the resource associated with the output sequence
     * @param resourceSupplier
     *            the factory function to create a resource object that depends on the {@code ObservableSource}
     * @param sourceSupplier
     *            the factory function to create an {@code ObservableSource}
     * @param resourceCleanup
     *            the function that will dispose of the resource
     * @param eager
     *            If {@code true}, the resource disposal will happen either on a {@code dispose()} call before the upstream is disposed
     *            or just before the emission of a terminal event ({@code onComplete} or {@code onError}).
     *            If {@code false}, the resource disposal will happen either on a {@code dispose()} call after the upstream is disposed
     *            or just after the emission of a terminal event ({@code onComplete} or {@code onError}).
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code resourceSupplier}, {@code sourceSupplier} and {@code resourceCleanup} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/using.html">ReactiveX operators documentation: Using</a>
     * @since 2.0
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public static <@NonNull T, @NonNull D> Observable<T> using(
            @NonNull Supplier<? extends D> resourceSupplier,
            @NonNull Function<? super D, ? extends ObservableSource<? extends T>> sourceSupplier,
            @NonNull Consumer<? super D> resourceCleanup, boolean eager) {
        Objects.requireNonNull(resourceSupplier, "resourceSupplier is null");
        Objects.requireNonNull(sourceSupplier, "sourceSupplier is null");
        Objects.requireNonNull(resourceCleanup, "resourceCleanup is null");
        return RxJavaPlugins.onAssembly(new ObservableUsing<T, D>(resourceSupplier, sourceSupplier, resourceCleanup, eager));
    }

    /**
     * Wraps an {@link ObservableSource} into an {@code Observable} if not already an {@code Observable}.
     *
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code wrap} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T> the value type
     * @param source the {@code ObservableSource} instance to wrap or cast to {@code Observable}
     * @return the new {@code Observable} instance or the same as the source
     * @throws NullPointerException if {@code source} is {@code null}
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public static <@NonNull T> Observable<T> wrap(@NonNull ObservableSource<T> source) {
        Objects.requireNonNull(source, "source is null");
        if (source instanceof Observable) {
            return RxJavaPlugins.onAssembly((Observable<T>)source);
        }
        return RxJavaPlugins.onAssembly(new ObservableFromUnsafeSource<>(source));
    }

    /**
     * Returns an {@code Observable} that emits the results of a specified combiner function applied to combinations of
     * items emitted, in sequence, by an {@link Iterable} of other {@link ObservableSource}s.
     * <p>
     * {@code zip} applies this function in strict sequence, so the first item emitted by the resulting {@code Observable}
     * will be the result of the function applied to the first item emitted by each of the {@code ObservableSource}s;
     * the second item emitted by the resulting {@code Observable} will be the result of the function applied to the second
     * item emitted by each of those {@code ObservableSource}s; and so forth.
     * <p>
     * The resulting {@code Observable<R>} returned from {@code zip} will invoke {@code onNext} as many times as
     * the number of {@code onNext} invocations of the {@code ObservableSource} that emits the fewest items.
     * <p>
     * The operator subscribes to its sources in order they are specified and completes eagerly if
     * one of the sources is shorter than the rest while disposing the other sources. Therefore, it
     * is possible those other sources will never be able to run to completion (and thus not calling
     * {@code doOnComplete()}). This can also happen if the sources are exactly the same length; if
     * source A completes and B has been consumed and is about to complete, the operator detects A won't
     * be sending further values and it will dispose B immediately. For example:
     * <pre><code>zip(Arrays.asList(range(1, 5).doOnComplete(action1), range(6, 5).doOnComplete(action2)), (a) -&gt; a)</code></pre>
     * {@code action1} will be called but {@code action2} won't.
     * <br>To work around this termination property,
     * use {@link #doOnDispose(Action)} as well or use {@code using()} to do cleanup in case of completion
     * or a dispose() call.
     * <p>
     * Note on method signature: since Java doesn't allow creating a generic array with {@code new T[]}, the
     * implementation of this operator has to create an {@code Object[]} instead. Unfortunately, a
     * {@code Function<Integer[], R>} passed to the method would trigger a {@link ClassCastException}.
     *
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/zip.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code zip} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T> the common value type
     * @param <R> the zipped result type
     * @param sources
     *            an {@code Iterable} of source {@code ObservableSource}s
     * @param zipper
     *            a function that, when applied to an item emitted by each of the {@code ObservableSource}s, results in
     *            an item that will be emitted by the resulting {@code Observable}
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code sources} or {@code zipper} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/zip.html">ReactiveX operators documentation: Zip</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public static <@NonNull T, @NonNull R> Observable<R> zip(@NonNull Iterable<@NonNull ? extends ObservableSource<? extends T>> sources, @NonNull Function<? super Object[], ? extends R> zipper) {
        Objects.requireNonNull(zipper, "zipper is null");
        Objects.requireNonNull(sources, "sources is null");
        return RxJavaPlugins.onAssembly(new ObservableZip<>(null, sources, zipper, bufferSize(), false));
    }

    /**
     * Returns an {@code Observable} that emits the results of a specified combiner function applied to combinations of
     * items emitted, in sequence, by an {@link Iterable} of other {@link ObservableSource}s.
     * <p>
     * {@code zip} applies this function in strict sequence, so the first item emitted by the resulting {@code Observable}
     * will be the result of the function applied to the first item emitted by each of the {@code ObservableSource}s;
     * the second item emitted by the resulting {@code Observable} will be the result of the function applied to the second
     * item emitted by each of those {@code ObservableSource}s; and so forth.
     * <p>
     * The resulting {@code Observable<R>} returned from {@code zip} will invoke {@code onNext} as many times as
     * the number of {@code onNext} invocations of the {@code ObservableSource} that emits the fewest items.
     * <p>
     * The operator subscribes to its sources in order they are specified and completes eagerly if
     * one of the sources is shorter than the rest while disposing the other sources. Therefore, it
     * is possible those other sources will never be able to run to completion (and thus not calling
     * {@code doOnComplete()}). This can also happen if the sources are exactly the same length; if
     * source A completes and B has been consumed and is about to complete, the operator detects A won't
     * be sending further values and it will dispose B immediately. For example:
     * <pre><code>zip(Arrays.asList(range(1, 5).doOnComplete(action1), range(6, 5).doOnComplete(action2)), (a) -&gt; a)</code></pre>
     * {@code action1} will be called but {@code action2} won't.
     * <br>To work around this termination property,
     * use {@link #doOnDispose(Action)} as well or use {@code using()} to do cleanup in case of completion
     * or a dispose() call.
     * <p>
     * Note on method signature: since Java doesn't allow creating a generic array with {@code new T[]}, the
     * implementation of this operator has to create an {@code Object[]} instead. Unfortunately, a
     * {@code Function<Integer[], R>} passed to the method would trigger a {@link ClassCastException}.
     *
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/zipIterable.o.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code zip} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     *
     * @param sources
     *            an {@code Iterable} of source {@code ObservableSource}s
     * @param zipper
     *            a function that, when applied to an item emitted by each of the {@code ObservableSource}s, results in
     *            an item that will be emitted by the resulting {@code Observable}
     * @param delayError
     *            delay errors signaled by any of the {@code ObservableSource} until all {@code ObservableSource}s terminate
     * @param bufferSize
     *            the number of elements expected from each source {@code ObservableSource} to be buffered
     * @param <T> the common source value type
     * @param <R> the zipped result type
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code sources} or {@code zipper} is {@code null}
     * @throws IllegalArgumentException if {@code bufferSize} is non-positive
     * @see <a href="http://reactivex.io/documentation/operators/zip.html">ReactiveX operators documentation: Zip</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public static <@NonNull T, @NonNull R> Observable<R> zip(@NonNull Iterable<@NonNull ? extends ObservableSource<? extends T>> sources,
            @NonNull Function<? super Object[], ? extends R> zipper, boolean delayError,
            int bufferSize) {
        Objects.requireNonNull(zipper, "zipper is null");
        Objects.requireNonNull(sources, "sources is null");
        ObjectHelper.verifyPositive(bufferSize, "bufferSize");
        return RxJavaPlugins.onAssembly(new ObservableZip<>(null, sources, zipper, bufferSize, delayError));
    }

    /**
     * Returns an {@code Observable} that emits the results of a specified combiner function applied to combinations of
     * two items emitted, in sequence, by two other {@link ObservableSource}s.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/zip.v3.png" alt="">
     * <p>
     * {@code zip} applies this function in strict sequence, so the first item emitted by the resulting {@code Observable}
     * will be the result of the function applied to the first item emitted by {@code o1} and the first item
     * emitted by {@code o2}; the second item emitted by the resulting {@code Observable} will be the result of the function
     * applied to the second item emitted by {@code o1} and the second item emitted by {@code o2}; and so forth.
     * <p>
     * The resulting {@code Observable<R>} returned from {@code zip} will invoke {@link Observer#onNext onNext}
     * as many times as the number of {@code onNext} invocations of the {@code ObservableSource} that emits the fewest
     * items.
     * <p>
     * The operator subscribes to its sources in order they are specified and completes eagerly if
     * one of the sources is shorter than the rest while disposing the other sources. Therefore, it
     * is possible those other sources will never be able to run to completion (and thus not calling
     * {@code doOnComplete()}). This can also happen if the sources are exactly the same length; if
     * source A completes and B has been consumed and is about to complete, the operator detects A won't
     * be sending further values and it will dispose B immediately. For example:
     * <pre><code>zip(range(1, 5).doOnComplete(action1), range(6, 5).doOnComplete(action2), (a, b) -&gt; a + b)</code></pre>
     * {@code action1} will be called but {@code action2} won't.
     * <br>To work around this termination property,
     * use {@link #doOnDispose(Action)} as well or use {@code using()} to do cleanup in case of completion
     * or a dispose() call.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code zip} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T1> the value type of the first source
     * @param <T2> the value type of the second source
     * @param <R> the zipped result type
     * @param source1
     *            the first source {@code ObservableSource}
     * @param source2
     *            a second source {@code ObservableSource}
     * @param zipper
     *            a function that, when applied to an item emitted by each of the {@code ObservableSource}s, results
     *            in an item that will be emitted by the resulting {@code Observable}
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code source1}, {@code source2} or {@code zipper} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/zip.html">ReactiveX operators documentation: Zip</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public static <@NonNull T1, @NonNull T2, @NonNull R> Observable<R> zip(
            @NonNull ObservableSource<? extends T1> source1, @NonNull ObservableSource<? extends T2> source2,
            @NonNull BiFunction<? super T1, ? super T2, ? extends R> zipper) {
        Objects.requireNonNull(source1, "source1 is null");
        Objects.requireNonNull(source2, "source2 is null");
        Objects.requireNonNull(zipper, "zipper is null");
        return zipArray(Functions.toFunction(zipper), false, bufferSize(), source1, source2);
    }

    /**
     * Returns an {@code Observable} that emits the results of a specified combiner function applied to combinations of
     * two items emitted, in sequence, by two other {@link ObservableSource}s.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/zip.v3.png" alt="">
     * <p>
     * {@code zip} applies this function in strict sequence, so the first item emitted by the resulting {@code Observable}
     * will be the result of the function applied to the first item emitted by {@code o1} and the first item
     * emitted by {@code o2}; the second item emitted by the resulting {@code Observable} will be the result of the function
     * applied to the second item emitted by {@code o1} and the second item emitted by {@code o2}; and so forth.
     * <p>
     * The resulting {@code Observable<R>} returned from {@code zip} will invoke {@link Observer#onNext onNext}
     * as many times as the number of {@code onNext} invocations of the {@code ObservableSource} that emits the fewest
     * items.
     * <p>
     * The operator subscribes to its sources in order they are specified and completes eagerly if
     * one of the sources is shorter than the rest while disposing the other sources. Therefore, it
     * is possible those other sources will never be able to run to completion (and thus not calling
     * {@code doOnComplete()}). This can also happen if the sources are exactly the same length; if
     * source A completes and B has been consumed and is about to complete, the operator detects A won't
     * be sending further values and it will dispose B immediately. For example:
     * <pre><code>zip(range(1, 5).doOnComplete(action1), range(6, 5).doOnComplete(action2), (a, b) -&gt; a + b)</code></pre>
     * {@code action1} will be called but {@code action2} won't.
     * <br>To work around this termination property,
     * use {@link #doOnDispose(Action)} as well or use {@code using()} to do cleanup in case of completion
     * or a dispose() call.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code zip} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T1> the value type of the first source
     * @param <T2> the value type of the second source
     * @param <R> the zipped result type
     * @param source1
     *            the first source {@code ObservableSource}
     * @param source2
     *            a second source {@code ObservableSource}
     * @param zipper
     *            a function that, when applied to an item emitted by each of the {@code ObservableSource}s, results
     *            in an item that will be emitted by the resulting {@code Observable}
     * @param delayError delay errors from any of the {@code ObservableSource}s till the other terminates
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code source1}, {@code source2} or {@code zipper} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/zip.html">ReactiveX operators documentation: Zip</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public static <@NonNull T1, @NonNull T2, @NonNull R> Observable<R> zip(
            @NonNull ObservableSource<? extends T1> source1, @NonNull ObservableSource<? extends T2> source2,
            @NonNull BiFunction<? super T1, ? super T2, ? extends R> zipper, boolean delayError) {
        Objects.requireNonNull(source1, "source1 is null");
        Objects.requireNonNull(source2, "source2 is null");
        Objects.requireNonNull(zipper, "zipper is null");
        return zipArray(Functions.toFunction(zipper), delayError, bufferSize(), source1, source2);
    }

    /**
     * Returns an {@code Observable} that emits the results of a specified combiner function applied to combinations of
     * two items emitted, in sequence, by two other {@link ObservableSource}s.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/zip.v3.png" alt="">
     * <p>
     * {@code zip} applies this function in strict sequence, so the first item emitted by the resulting {@code Observable}
     * will be the result of the function applied to the first item emitted by {@code o1} and the first item
     * emitted by {@code o2}; the second item emitted by the resulting {@code Observable} will be the result of the function
     * applied to the second item emitted by {@code o1} and the second item emitted by {@code o2}; and so forth.
     * <p>
     * The resulting {@code Observable<R>} returned from {@code zip} will invoke {@link Observer#onNext onNext}
     * as many times as the number of {@code onNext} invocations of the {@code ObservableSource} that emits the fewest
     * items.
     * <p>
     * The operator subscribes to its sources in order they are specified and completes eagerly if
     * one of the sources is shorter than the rest while disposing the other sources. Therefore, it
     * is possible those other sources will never be able to run to completion (and thus not calling
     * {@code doOnComplete()}). This can also happen if the sources are exactly the same length; if
     * source A completes and B has been consumed and is about to complete, the operator detects A won't
     * be sending further values and it will dispose B immediately. For example:
     * <pre><code>zip(range(1, 5).doOnComplete(action1), range(6, 5).doOnComplete(action2), (a, b) -&gt; a + b)</code></pre>
     * {@code action1} will be called but {@code action2} won't.
     * <br>To work around this termination property,
     * use {@link #doOnDispose(Action)} as well or use {@code using()} to do cleanup in case of completion
     * or a dispose() call.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code zip} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T1> the value type of the first source
     * @param <T2> the value type of the second source
     * @param <R> the zipped result type
     * @param source1
     *            the first source {@code ObservableSource}
     * @param source2
     *            a second source {@code ObservableSource}
     * @param zipper
     *            a function that, when applied to an item emitted by each of the {@code ObservableSource}s, results
     *            in an item that will be emitted by the resulting {@code Observable}
     * @param delayError delay errors from any of the {@code ObservableSource}s till the other terminates
     * @param bufferSize the number of elements expected from each source {@code ObservableSource} to be buffered
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code source1}, {@code source2} or {@code zipper} is {@code null}
     * @throws IllegalArgumentException if {@code bufferSize} is non-positive
     * @see <a href="http://reactivex.io/documentation/operators/zip.html">ReactiveX operators documentation: Zip</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public static <@NonNull T1, @NonNull T2, @NonNull R> Observable<R> zip(
            @NonNull ObservableSource<? extends T1> source1, @NonNull ObservableSource<? extends T2> source2,
            @NonNull BiFunction<? super T1, ? super T2, ? extends R> zipper, boolean delayError, int bufferSize) {
        Objects.requireNonNull(source1, "source1 is null");
        Objects.requireNonNull(source2, "source2 is null");
        Objects.requireNonNull(zipper, "zipper is null");
        return zipArray(Functions.toFunction(zipper), delayError, bufferSize, source1, source2);
    }

    /**
     * Returns an {@code Observable} that emits the results of a specified combiner function applied to combinations of
     * three items emitted, in sequence, by three other {@link ObservableSource}s.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/zip.v3.png" alt="">
     * <p>
     * {@code zip} applies this function in strict sequence, so the first item emitted by the resulting {@code Observable}
     * will be the result of the function applied to the first item emitted by {@code o1}, the first item
     * emitted by {@code o2}, and the first item emitted by {@code o3}; the second item emitted by the resulting
     * {@code Observable} will be the result of the function applied to the second item emitted by {@code o1}, the
     * second item emitted by {@code o2}, and the second item emitted by {@code o3}; and so forth.
     * <p>
     * The resulting {@code Observable<R>} returned from {@code zip} will invoke {@link Observer#onNext onNext}
     * as many times as the number of {@code onNext} invocations of the {@code ObservableSource} that emits the fewest
     * items.
     * <p>
     * The operator subscribes to its sources in order they are specified and completes eagerly if
     * one of the sources is shorter than the rest while disposing the other sources. Therefore, it
     * is possible those other sources will never be able to run to completion (and thus not calling
     * {@code doOnComplete()}). This can also happen if the sources are exactly the same length; if
     * source A completes and B has been consumed and is about to complete, the operator detects A won't
     * be sending further values and it will dispose B immediately. For example:
     * <pre><code>zip(range(1, 5).doOnComplete(action1), range(6, 5).doOnComplete(action2), ..., (a, b, c) -&gt; a + b)</code></pre>
     * {@code action1} will be called but {@code action2} won't.
     * <br>To work around this termination property,
     * use {@link #doOnDispose(Action)} as well or use {@code using()} to do cleanup in case of completion
     * or a dispose() call.
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
     *            the first source {@code ObservableSource}
     * @param source2
     *            a second source {@code ObservableSource}
     * @param source3
     *            a third source {@code ObservableSource}
     * @param zipper
     *            a function that, when applied to an item emitted by each of the {@code ObservableSource}s, results in
     *            an item that will be emitted by the resulting {@code Observable}
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code source1}, {@code source2}, {@code source3} or {@code zipper} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/zip.html">ReactiveX operators documentation: Zip</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public static <@NonNull T1, @NonNull T2, @NonNull T3, @NonNull R> Observable<R> zip(
            @NonNull ObservableSource<? extends T1> source1, @NonNull ObservableSource<? extends T2> source2,
            @NonNull ObservableSource<? extends T3> source3,
            @NonNull Function3<? super T1, ? super T2, ? super T3, ? extends R> zipper) {
        Objects.requireNonNull(source1, "source1 is null");
        Objects.requireNonNull(source2, "source2 is null");
        Objects.requireNonNull(source3, "source3 is null");
        Objects.requireNonNull(zipper, "zipper is null");
        return zipArray(Functions.toFunction(zipper), false, bufferSize(), source1, source2, source3);
    }

    /**
     * Returns an {@code Observable} that emits the results of a specified combiner function applied to combinations of
     * four items emitted, in sequence, by four other {@link ObservableSource}s.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/zip.v3.png" alt="">
     * <p>
     * {@code zip} applies this function in strict sequence, so the first item emitted by the resulting {@code Observable}
     * will be the result of the function applied to the first item emitted by {@code o1}, the first item
     * emitted by {@code o2}, the first item emitted by {@code o3}, and the first item emitted by {@code 04};
     * the second item emitted by the resulting {@code Observable} will be the result of the function applied to the second
     * item emitted by each of those {@code ObservableSource}s; and so forth.
     * <p>
     * The resulting {@code Observable<R>} returned from {@code zip} will invoke {@link Observer#onNext onNext}
     * as many times as the number of {@code onNext} invocations of the {@code ObservableSource} that emits the fewest
     * items.
     * <p>
     * The operator subscribes to its sources in order they are specified and completes eagerly if
     * one of the sources is shorter than the rest while disposing the other sources. Therefore, it
     * is possible those other sources will never be able to run to completion (and thus not calling
     * {@code doOnComplete()}). This can also happen if the sources are exactly the same length; if
     * source A completes and B has been consumed and is about to complete, the operator detects A won't
     * be sending further values and it will dispose B immediately. For example:
     * <pre><code>zip(range(1, 5).doOnComplete(action1), range(6, 5).doOnComplete(action2), ..., (a, b, c, d) -&gt; a + b)</code></pre>
     * {@code action1} will be called but {@code action2} won't.
     * <br>To work around this termination property,
     * use {@link #doOnDispose(Action)} as well or use {@code using()} to do cleanup in case of completion
     * or a dispose() call.
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
     *            the first source {@code ObservableSource}
     * @param source2
     *            a second source {@code ObservableSource}
     * @param source3
     *            a third source {@code ObservableSource}
     * @param source4
     *            a fourth source {@code ObservableSource}
     * @param zipper
     *            a function that, when applied to an item emitted by each of the {@code ObservableSource}s, results in
     *            an item that will be emitted by the resulting {@code Observable}
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code source1}, {@code source2}, {@code source3},
     *                              {@code source4} or {@code zipper} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/zip.html">ReactiveX operators documentation: Zip</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public static <@NonNull T1, @NonNull T2, @NonNull T3, @NonNull T4, @NonNull R> Observable<R> zip(
            @NonNull ObservableSource<? extends T1> source1, @NonNull ObservableSource<? extends T2> source2,
            @NonNull ObservableSource<? extends T3> source3, @NonNull ObservableSource<? extends T4> source4,
            @NonNull Function4<? super T1, ? super T2, ? super T3, ? super T4, ? extends R> zipper) {
        Objects.requireNonNull(source1, "source1 is null");
        Objects.requireNonNull(source2, "source2 is null");
        Objects.requireNonNull(source3, "source3 is null");
        Objects.requireNonNull(source4, "source4 is null");
        Objects.requireNonNull(zipper, "zipper is null");
        return zipArray(Functions.toFunction(zipper), false, bufferSize(), source1, source2, source3, source4);
    }

    /**
     * Returns an {@code Observable} that emits the results of a specified combiner function applied to combinations of
     * five items emitted, in sequence, by five other {@link ObservableSource}s.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/zip.v3.png" alt="">
     * <p>
     * {@code zip} applies this function in strict sequence, so the first item emitted by the resulting {@code Observable}
     * will be the result of the function applied to the first item emitted by {@code o1}, the first item
     * emitted by {@code o2}, the first item emitted by {@code o3}, the first item emitted by {@code o4}, and
     * the first item emitted by {@code o5}; the second item emitted by the resulting {@code Observable} will be the result of
     * the function applied to the second item emitted by each of those {@code ObservableSource}s; and so forth.
     * <p>
     * The resulting {@code Observable<R>} returned from {@code zip} will invoke {@link Observer#onNext onNext}
     * as many times as the number of {@code onNext} invocations of the {@code ObservableSource} that emits the fewest
     * items.
     * <p>
     * The operator subscribes to its sources in order they are specified and completes eagerly if
     * one of the sources is shorter than the rest while disposing the other sources. Therefore, it
     * is possible those other sources will never be able to run to completion (and thus not calling
     * {@code doOnComplete()}). This can also happen if the sources are exactly the same length; if
     * source A completes and B has been consumed and is about to complete, the operator detects A won't
     * be sending further values and it will dispose B immediately. For example:
     * <pre><code>zip(range(1, 5).doOnComplete(action1), range(6, 5).doOnComplete(action2), ..., (a, b, c, d, e) -&gt; a + b)</code></pre>
     * {@code action1} will be called but {@code action2} won't.
     * <br>To work around this termination property,
     * use {@link #doOnDispose(Action)} as well or use {@code using()} to do cleanup in case of completion
     * or a dispose() call.
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
     *            the first source {@code ObservableSource}
     * @param source2
     *            a second source {@code ObservableSource}
     * @param source3
     *            a third source {@code ObservableSource}
     * @param source4
     *            a fourth source {@code ObservableSource}
     * @param source5
     *            a fifth source {@code ObservableSource}
     * @param zipper
     *            a function that, when applied to an item emitted by each of the {@code ObservableSource}s, results in
     *            an item that will be emitted by the resulting {@code Observable}
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code source1}, {@code source2}, {@code source3},
     *                              {@code source4}, {@code source5} or {@code zipper} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/zip.html">ReactiveX operators documentation: Zip</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public static <@NonNull T1, @NonNull T2, @NonNull T3, @NonNull T4, @NonNull T5, @NonNull R> Observable<R> zip(
            @NonNull ObservableSource<? extends T1> source1, @NonNull ObservableSource<? extends T2> source2, @NonNull ObservableSource<? extends T3> source3,
            @NonNull ObservableSource<? extends T4> source4, @NonNull ObservableSource<? extends T5> source5,
            @NonNull Function5<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? extends R> zipper) {
        Objects.requireNonNull(source1, "source1 is null");
        Objects.requireNonNull(source2, "source2 is null");
        Objects.requireNonNull(source3, "source3 is null");
        Objects.requireNonNull(source4, "source4 is null");
        Objects.requireNonNull(source5, "source5 is null");
        Objects.requireNonNull(zipper, "zipper is null");
        return zipArray(Functions.toFunction(zipper), false, bufferSize(), source1, source2, source3, source4, source5);
    }

    /**
     * Returns an {@code Observable} that emits the results of a specified combiner function applied to combinations of
     * six items emitted, in sequence, by six other {@link ObservableSource}s.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/zip.v3.png" alt="">
     * <p>
     * {@code zip} applies this function in strict sequence, so the first item emitted by the resulting {@code Observable}
     * will be the result of the function applied to the first item emitted by each source {@code ObservableSource}, the
     * second item emitted by the resulting {@code Observable} will be the result of the function applied to the second item
     * emitted by each of those {@code ObservableSource}s, and so forth.
     * <p>
     * The resulting {@code Observable<R>} returned from {@code zip} will invoke {@link Observer#onNext onNext}
     * as many times as the number of {@code onNext} invocations of the {@code ObservableSource} that emits the fewest
     * items.
     * <p>
     * The operator subscribes to its sources in order they are specified and completes eagerly if
     * one of the sources is shorter than the rest while disposing the other sources. Therefore, it
     * is possible those other sources will never be able to run to completion (and thus not calling
     * {@code doOnComplete()}). This can also happen if the sources are exactly the same length; if
     * source A completes and B has been consumed and is about to complete, the operator detects A won't
     * be sending further values and it will dispose B immediately. For example:
     * <pre><code>zip(range(1, 5).doOnComplete(action1), range(6, 5).doOnComplete(action2), ..., (a, b, c, d, e, f) -&gt; a + b)</code></pre>
     * {@code action1} will be called but {@code action2} won't.
     * <br>To work around this termination property,
     * use {@link #doOnDispose(Action)} as well or use {@code using()} to do cleanup in case of completion
     * or a dispose() call.
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
     *            the first source {@code ObservableSource}
     * @param source2
     *            a second source {@code ObservableSource}
     * @param source3
     *            a third source {@code ObservableSource}
     * @param source4
     *            a fourth source {@code ObservableSource}
     * @param source5
     *            a fifth source {@code ObservableSource}
     * @param source6
     *            a sixth source {@code ObservableSource}
     * @param zipper
     *            a function that, when applied to an item emitted by each of the {@code ObservableSource}s, results in
     *            an item that will be emitted by the resulting {@code Observable}
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code source1}, {@code source2}, {@code source3},
     *                              {@code source4}, {@code source5}, {@code source6} or {@code zipper} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/zip.html">ReactiveX operators documentation: Zip</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public static <@NonNull T1, @NonNull T2, @NonNull T3, @NonNull T4, @NonNull T5, @NonNull T6, @NonNull R> Observable<R> zip(
            @NonNull ObservableSource<? extends T1> source1, @NonNull ObservableSource<? extends T2> source2, @NonNull ObservableSource<? extends T3> source3,
            @NonNull ObservableSource<? extends T4> source4, @NonNull ObservableSource<? extends T5> source5, @NonNull ObservableSource<? extends T6> source6,
            @NonNull Function6<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? extends R> zipper) {
        Objects.requireNonNull(source1, "source1 is null");
        Objects.requireNonNull(source2, "source2 is null");
        Objects.requireNonNull(source3, "source3 is null");
        Objects.requireNonNull(source4, "source4 is null");
        Objects.requireNonNull(source5, "source5 is null");
        Objects.requireNonNull(source6, "source6 is null");
        Objects.requireNonNull(zipper, "zipper is null");
        return zipArray(Functions.toFunction(zipper), false, bufferSize(), source1, source2, source3, source4, source5, source6);
    }

    /**
     * Returns an {@code Observable} that emits the results of a specified combiner function applied to combinations of
     * seven items emitted, in sequence, by seven other {@link ObservableSource}s.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/zip.v3.png" alt="">
     * <p>
     * {@code zip} applies this function in strict sequence, so the first item emitted by the resulting {@code Observable}
     * will be the result of the function applied to the first item emitted by each source {@code ObservableSource}, the
     * second item emitted by the resulting {@code Observable} will be the result of the function applied to the second item
     * emitted by each of those {@code ObservableSource}s, and so forth.
     * <p>
     * The resulting {@code Observable<R>} returned from {@code zip} will invoke {@link Observer#onNext onNext}
     * as many times as the number of {@code onNext} invocations of the {@code ObservableSource} that emits the fewest
     * items.
     * <p>
     * The operator subscribes to its sources in order they are specified and completes eagerly if
     * one of the sources is shorter than the rest while disposing the other sources. Therefore, it
     * is possible those other sources will never be able to run to completion (and thus not calling
     * {@code doOnComplete()}). This can also happen if the sources are exactly the same length; if
     * source A completes and B has been consumed and is about to complete, the operator detects A won't
     * be sending further values and it will dispose B immediately. For example:
     * <pre><code>zip(range(1, 5).doOnComplete(action1), range(6, 5).doOnComplete(action2), ..., (a, b, c, d, e, f, g) -&gt; a + b)</code></pre>
     * {@code action1} will be called but {@code action2} won't.
     * <br>To work around this termination property,
     * use {@link #doOnDispose(Action)} as well or use {@code using()} to do cleanup in case of completion
     * or a dispose() call.
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
     *            the first source {@code ObservableSource}
     * @param source2
     *            a second source {@code ObservableSource}
     * @param source3
     *            a third source {@code ObservableSource}
     * @param source4
     *            a fourth source {@code ObservableSource}
     * @param source5
     *            a fifth source {@code ObservableSource}
     * @param source6
     *            a sixth source {@code ObservableSource}
     * @param source7
     *            a seventh source {@code ObservableSource}
     * @param zipper
     *            a function that, when applied to an item emitted by each of the {@code ObservableSource}s, results in
     *            an item that will be emitted by the resulting {@code Observable}
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code source1}, {@code source2}, {@code source3},
     *                              {@code source4}, {@code source5}, {@code source6},
     *                              {@code source7} or {@code zipper} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/zip.html">ReactiveX operators documentation: Zip</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public static <@NonNull T1, @NonNull T2, @NonNull T3, @NonNull T4, @NonNull T5, @NonNull T6, @NonNull T7, @NonNull R> Observable<R> zip(
            @NonNull ObservableSource<? extends T1> source1, @NonNull ObservableSource<? extends T2> source2, @NonNull ObservableSource<? extends T3> source3,
            @NonNull ObservableSource<? extends T4> source4, @NonNull ObservableSource<? extends T5> source5, @NonNull ObservableSource<? extends T6> source6,
            @NonNull ObservableSource<? extends T7> source7,
            @NonNull Function7<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? extends R> zipper) {
        Objects.requireNonNull(source1, "source1 is null");
        Objects.requireNonNull(source2, "source2 is null");
        Objects.requireNonNull(source3, "source3 is null");
        Objects.requireNonNull(source4, "source4 is null");
        Objects.requireNonNull(source5, "source5 is null");
        Objects.requireNonNull(source6, "source6 is null");
        Objects.requireNonNull(source7, "source7 is null");
        Objects.requireNonNull(zipper, "zipper is null");
        return zipArray(Functions.toFunction(zipper), false, bufferSize(), source1, source2, source3, source4, source5, source6, source7);
    }

    /**
     * Returns an {@code Observable} that emits the results of a specified combiner function applied to combinations of
     * eight items emitted, in sequence, by eight other {@link ObservableSource}s.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/zip.v3.png" alt="">
     * <p>
     * {@code zip} applies this function in strict sequence, so the first item emitted by the resulting {@code Observable}
     * will be the result of the function applied to the first item emitted by each source {@code ObservableSource}, the
     * second item emitted by the resulting {@code Observable} will be the result of the function applied to the second item
     * emitted by each of those {@code ObservableSource}s, and so forth.
     * <p>
     * The resulting {@code Observable<R>} returned from {@code zip} will invoke {@link Observer#onNext onNext}
     * as many times as the number of {@code onNext} invocations of the {@code ObservableSource} that emits the fewest
     * items.
     * <p>
     * The operator subscribes to its sources in order they are specified and completes eagerly if
     * one of the sources is shorter than the rest while disposing the other sources. Therefore, it
     * is possible those other sources will never be able to run to completion (and thus not calling
     * {@code doOnComplete()}). This can also happen if the sources are exactly the same length; if
     * source A completes and B has been consumed and is about to complete, the operator detects A won't
     * be sending further values and it will dispose B immediately. For example:
     * <pre><code>zip(range(1, 5).doOnComplete(action1), range(6, 5).doOnComplete(action2), ..., (a, b, c, d, e, f, g, h) -&gt; a + b)</code></pre>
     * {@code action1} will be called but {@code action2} won't.
     * <br>To work around this termination property,
     * use {@link #doOnDispose(Action)} as well or use {@code using()} to do cleanup in case of completion
     * or a dispose() call.
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
     *            the first source {@code ObservableSource}
     * @param source2
     *            a second source {@code ObservableSource}
     * @param source3
     *            a third source {@code ObservableSource}
     * @param source4
     *            a fourth source {@code ObservableSource}
     * @param source5
     *            a fifth source {@code ObservableSource}
     * @param source6
     *            a sixth source {@code ObservableSource}
     * @param source7
     *            a seventh source {@code ObservableSource}
     * @param source8
     *            an eighth source {@code ObservableSource}
     * @param zipper
     *            a function that, when applied to an item emitted by each of the {@code ObservableSource}s, results in
     *            an item that will be emitted by the resulting {@code Observable}
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code source1}, {@code source2}, {@code source3},
     *                              {@code source4}, {@code source5}, {@code source6},
     *                              {@code source7}, {@code source8} or {@code zipper} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/zip.html">ReactiveX operators documentation: Zip</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public static <@NonNull T1, @NonNull T2, @NonNull T3, @NonNull T4, @NonNull T5, @NonNull T6, @NonNull T7, @NonNull T8, @NonNull R> Observable<R> zip(
            @NonNull ObservableSource<? extends T1> source1, @NonNull ObservableSource<? extends T2> source2, @NonNull ObservableSource<? extends T3> source3,
            @NonNull ObservableSource<? extends T4> source4, @NonNull ObservableSource<? extends T5> source5, @NonNull ObservableSource<? extends T6> source6,
            @NonNull ObservableSource<? extends T7> source7, @NonNull ObservableSource<? extends T8> source8,
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
        return zipArray(Functions.toFunction(zipper), false, bufferSize(), source1, source2, source3, source4, source5, source6, source7, source8);
    }

    /**
     * Returns an {@code Observable} that emits the results of a specified combiner function applied to combinations of
     * nine items emitted, in sequence, by nine other {@link ObservableSource}s.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/zip.v3.png" alt="">
     * <p>
     * {@code zip} applies this function in strict sequence, so the first item emitted by the resulting {@code Observable}
     * will be the result of the function applied to the first item emitted by each source {@code ObservableSource}, the
     * second item emitted by the resulting {@code Observable} will be the result of the function applied to the second item
     * emitted by each of those {@code ObservableSource}s, and so forth.
     * <p>
     * The resulting {@code Observable<R>} returned from {@code zip} will invoke {@link Observer#onNext onNext}
     * as many times as the number of {@code onNext} invocations of the {@code ObservableSource} that emits the fewest
     * items.
     * <p>
     * The operator subscribes to its sources in order they are specified and completes eagerly if
     * one of the sources is shorter than the rest while disposing the other sources. Therefore, it
     * is possible those other sources will never be able to run to completion (and thus not calling
     * {@code doOnComplete()}). This can also happen if the sources are exactly the same length; if
     * source A completes and B has been consumed and is about to complete, the operator detects A won't
     * be sending further values and it will dispose B immediately. For example:
     * <pre><code>zip(range(1, 5).doOnComplete(action1), range(6, 5).doOnComplete(action2), ..., (a, b, c, d, e, f, g, h, i) -&gt; a + b)</code></pre>
     * {@code action1} will be called but {@code action2} won't.
     * <br>To work around this termination property,
     * use {@link #doOnDispose(Action)} as well or use {@code using()} to do cleanup in case of completion
     * or a dispose() call.
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
     * @param <T9> the value type of the ninth source
     * @param <R> the zipped result type
     * @param source1
     *            the first source {@code ObservableSource}
     * @param source2
     *            a second source {@code ObservableSource}
     * @param source3
     *            a third source {@code ObservableSource}
     * @param source4
     *            a fourth source {@code ObservableSource}
     * @param source5
     *            a fifth source {@code ObservableSource}
     * @param source6
     *            a sixth source {@code ObservableSource}
     * @param source7
     *            a seventh source {@code ObservableSource}
     * @param source8
     *            an eighth source {@code ObservableSource}
     * @param source9
     *            a ninth source {@code ObservableSource}
     * @param zipper
     *            a function that, when applied to an item emitted by each of the {@code ObservableSource}s, results in
     *            an item that will be emitted by the resulting {@code Observable}
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code source1}, {@code source2}, {@code source3},
     *                              {@code source4}, {@code source5}, {@code source6},
     *                              {@code source7}, {@code source8}, {@code source9} or {@code zipper} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/zip.html">ReactiveX operators documentation: Zip</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public static <@NonNull T1, @NonNull T2, @NonNull T3, @NonNull T4, @NonNull T5, @NonNull T6, @NonNull T7, @NonNull T8, @NonNull T9, @NonNull R> Observable<R> zip(
            @NonNull ObservableSource<? extends T1> source1, @NonNull ObservableSource<? extends T2> source2, @NonNull ObservableSource<? extends T3> source3,
            @NonNull ObservableSource<? extends T4> source4, @NonNull ObservableSource<? extends T5> source5, @NonNull ObservableSource<? extends T6> source6,
            @NonNull ObservableSource<? extends T7> source7, @NonNull ObservableSource<? extends T8> source8, @NonNull ObservableSource<? extends T9> source9,
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
        return zipArray(Functions.toFunction(zipper), false, bufferSize(), source1, source2, source3, source4, source5, source6, source7, source8, source9);
    }

    /**
     * Returns an {@code Observable} that emits the results of a specified combiner function applied to combinations of
     * items emitted, in sequence, by an array of other {@link ObservableSource}s.
     * <p>
     * {@code zip} applies this function in strict sequence, so the first item emitted by the resulting {@code Observable}
     * will be the result of the function applied to the first item emitted by each of the {@code ObservableSource}s;
     * the second item emitted by the resulting {@code Observable} will be the result of the function applied to the second
     * item emitted by each of those {@code ObservableSource}s; and so forth.
     * <p>
     * The resulting {@code Observable<R>} returned from {@code zip} will invoke {@code onNext} as many times as
     * the number of {@code onNext} invocations of the {@code ObservableSource} that emits the fewest items.
     * <p>
     * The operator subscribes to its sources in order they are specified and completes eagerly if
     * one of the sources is shorter than the rest while disposing the other sources. Therefore, it
     * is possible those other sources will never be able to run to completion (and thus not calling
     * {@code doOnComplete()}). This can also happen if the sources are exactly the same length; if
     * source A completes and B has been consumed and is about to complete, the operator detects A won't
     * be sending further values and it will dispose B immediately. For example:
     * <pre><code>zip(new ObservableSource[]{range(1, 5).doOnComplete(action1), range(6, 5).doOnComplete(action2)}, (a) -&gt;
     * a)</code></pre>
     * {@code action1} will be called but {@code action2} won't.
     * <br>To work around this termination property,
     * use {@link #doOnDispose(Action)} as well or use {@code using()} to do cleanup in case of completion
     * or a dispose() call.
     * <p>
     * Note on method signature: since Java doesn't allow creating a generic array with {@code new T[]}, the
     * implementation of this operator has to create an {@code Object[]} instead. Unfortunately, a
     * {@code Function<Integer[], R>} passed to the method would trigger a {@link ClassCastException}.
     *
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/zipArray.o.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code zipArray} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T> the common element type
     * @param <R> the result type
     * @param sources
     *            an array of source {@code ObservableSource}s
     * @param zipper
     *            a function that, when applied to an item emitted by each of the {@code ObservableSource}s, results in
     *            an item that will be emitted by the resulting {@code Observable}
     * @param delayError
     *            delay errors signaled by any of the {@code ObservableSource} until all {@code ObservableSource}s terminate
     * @param bufferSize
     *            the number of elements expected from each source {@code ObservableSource} to be buffered
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code sources} or {@code zipper} is {@code null}
     * @throws IllegalArgumentException if {@code bufferSize} is non-positive
     * @see <a href="http://reactivex.io/documentation/operators/zip.html">ReactiveX operators documentation: Zip</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @SafeVarargs
    @NonNull
    public static <@NonNull T, @NonNull R> Observable<R> zipArray(
            @NonNull Function<? super Object[], ? extends R> zipper,
            boolean delayError, int bufferSize,
            @NonNull ObservableSource<? extends T>... sources) {
        Objects.requireNonNull(sources, "sources is null");
        if (sources.length == 0) {
            return empty();
        }
        Objects.requireNonNull(zipper, "zipper is null");
        ObjectHelper.verifyPositive(bufferSize, "bufferSize");
        return RxJavaPlugins.onAssembly(new ObservableZip<>(sources, null, zipper, bufferSize, delayError));
    }

    // ***************************************************************************************************
    // Instance operators
    // ***************************************************************************************************

    /**
     * Returns a {@link Single} that emits a {@link Boolean} that indicates whether all of the items emitted by the current
     * {@code Observable} satisfy a condition.
     * <p>
     * <img width="640" height="265" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/all.o.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code all} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param predicate
     *            a function that evaluates an item and returns a {@code Boolean}
     * @return the new {@code Single} instance
     * @throws NullPointerException if {@code predicate} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/all.html">ReactiveX operators documentation: All</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Single<Boolean> all(@NonNull Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        return RxJavaPlugins.onAssembly(new ObservableAllSingle<>(this, predicate));
    }

    /**
     * Mirrors the current {@code Observable} or the other {@link ObservableSource} provided of which the first either emits an item or sends a termination
     * notification.
     * <p>
     * <img width="640" height="448" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Observable.ambWith.png" alt="">
     * <p>
     * When the current {@code Observable} signals an item or terminates first, the subscription to the other
     * {@code ObservableSource} is disposed. If the other {@code ObservableSource} signals an item or terminates first,
     * the subscription to the current {@code Observable} is disposed.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code ambWith} does not operate by default on a particular {@link Scheduler}.</dd>
     *  <dt><b>Error handling:</b></dt>
     *  <dd>
     *     If the losing {@code ObservableSource} signals an error, the error is routed to the global
     *     error handler via {@link RxJavaPlugins#onError(Throwable)}.
     *  </dd>
     * </dl>
     *
     * @param other
     *            an {@code ObservableSource} competing to react first. A subscription to this provided source will occur after
     *            subscribing to the current source.
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code other} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/amb.html">ReactiveX operators documentation: Amb</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Observable<T> ambWith(@NonNull ObservableSource<? extends T> other) {
        Objects.requireNonNull(other, "other is null");
        return ambArray(this, other);
    }

    /**
     * Returns a {@link Single} that emits {@code true} if any item emitted by the current {@code Observable} satisfies a
     * specified condition, otherwise {@code false}. <em>Note:</em> this always emits {@code false} if the
     * current {@code Observable} is empty.
     * <p>
     * <img width="640" height="320" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/any.2.v3.png" alt="">
     * <p>
     * In Rx.Net this is the {@code any} {@link Observer} but we renamed it in RxJava to better match Java naming
     * idioms.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code any} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param predicate
     *            the condition to test items emitted by the current {@code Observable}
     * @return the new {@code Single} instance
     * @throws NullPointerException if {@code predicate} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/contains.html">ReactiveX operators documentation: Contains</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Single<Boolean> any(@NonNull Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        return RxJavaPlugins.onAssembly(new ObservableAnySingle<>(this, predicate));
    }

    /**
     * Returns the first item emitted by the current {@code Observable}, or throws
     * {@link NoSuchElementException} if it emits no items.
     * <p>
     * <img width="640" height="413" src="https://github.com/ReactiveX/RxJava/wiki/images/rx-operators/blockingFirst.o.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code blockingFirst} does not operate by default on a particular {@link Scheduler}.</dd>
     *  <dt><b>Error handling:</b></dt>
     *  <dd>If the source signals an error, the operator wraps a checked {@link Exception}
     *  into {@link RuntimeException} and throws that. Otherwise, {@code RuntimeException}s and
     *  {@link Error}s are rethrown as they are.</dd>
     * </dl>
     *
     * @return the first item emitted by the current {@code Observable}
     * @throws NoSuchElementException
     *             if the current {@code Observable} emits no items
     * @see <a href="http://reactivex.io/documentation/operators/first.html">ReactiveX documentation: First</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final T blockingFirst() {
        BlockingFirstObserver<T> observer = new BlockingFirstObserver<>();
        subscribe(observer);
        T v = observer.blockingGet();
        if (v != null) {
            return v;
        }
        throw new NoSuchElementException();
    }

    /**
     * Returns the first item emitted by the current {@code Observable}, or a default value if it emits no
     * items.
     * <p>
     * <img width="640" height="329" src="https://github.com/ReactiveX/RxJava/wiki/images/rx-operators/blockingFirst.o.default.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code blockingFirst} does not operate by default on a particular {@link Scheduler}.</dd>
     *  <dt><b>Error handling:</b></dt>
     *  <dd>If the source signals an error, the operator wraps a checked {@link Exception}
     *  into {@link RuntimeException} and throws that. Otherwise, {@code RuntimeException}s and
     *  {@link Error}s are rethrown as they are.</dd>
     * </dl>
     *
     * @param defaultItem
     *            a default value to return if the current {@code Observable} emits no items
     * @return the first item emitted by the current {@code Observable}, or the default value if it emits no
     *         items
     * @throws NullPointerException if {@code defaultItem} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/first.html">ReactiveX documentation: First</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final T blockingFirst(@NonNull T defaultItem) {
        Objects.requireNonNull(defaultItem, "defaultItem is null");
        BlockingFirstObserver<T> observer = new BlockingFirstObserver<>();
        subscribe(observer);
        T v = observer.blockingGet();
        return v != null ? v : defaultItem;
    }

    /**
     * Consumes the current {@code Observable} in a blocking fashion and invokes the given
     * {@link Consumer} with each upstream item on the <em>current thread</em> until the
     * upstream terminates.
     * <p>
     * <img width="640" height="330" src="https://github.com/ReactiveX/RxJava/wiki/images/rx-operators/blockingForEach.o.v3.png" alt="">
     * <p>
     * <em>Note:</em> the method will only return if the upstream terminates or the current
     * thread is interrupted.
     * <p>
     * This method executes the {@code Consumer} on the current thread while
     * {@link #subscribe(Consumer)} executes the consumer on the original caller thread of the
     * sequence.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code blockingForEach} does not operate by default on a particular {@link Scheduler}.</dd>
     *  <dt><b>Error handling:</b></dt>
     *  <dd>If the source signals an error, the operator wraps a checked {@link Exception}
     *  into {@link RuntimeException} and throws that. Otherwise, {@code RuntimeException}s and
     *  {@link Error}s are rethrown as they are.</dd>
     * </dl>
     *
     * @param onNext
     *            the {@code Consumer} to invoke for each item emitted by the {@code Observable}
     * @throws NullPointerException if {@code onNext} is {@code null}
     * @throws RuntimeException
     *             if an error occurs
     * @see <a href="http://reactivex.io/documentation/operators/subscribe.html">ReactiveX documentation: Subscribe</a>
     * @see #subscribe(Consumer)
     * @see #blockingForEach(Consumer, int)
     */
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final void blockingForEach(@NonNull Consumer<? super T> onNext) {
        blockingForEach(onNext, bufferSize());
    }

    /**
     * Consumes the current {@code Observable} in a blocking fashion and invokes the given
     * {@link Consumer} with each upstream item on the <em>current thread</em> until the
     * upstream terminates.
     * <p>
     * <img width="640" height="330" src="https://github.com/ReactiveX/RxJava/wiki/images/rx-operators/blockingForEach.o.v3.png" alt="">
     * <p>
     * <em>Note:</em> the method will only return if the upstream terminates or the current
     * thread is interrupted.
     * <p>
     * This method executes the {@code Consumer} on the current thread while
     * {@link #subscribe(Consumer)} executes the consumer on the original caller thread of the
     * sequence.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code blockingForEach} does not operate by default on a particular {@link Scheduler}.</dd>
     *  <dt><b>Error handling:</b></dt>
     *  <dd>If the source signals an error, the operator wraps a checked {@link Exception}
     *  into {@link RuntimeException} and throws that. Otherwise, {@code RuntimeException}s and
     *  {@link Error}s are rethrown as they are.</dd>
     * </dl>
     *
     * @param onNext
     *            the {@code Consumer} to invoke for each item emitted by the {@code Observable}
     * @param capacityHint
     *            the number of items expected to be buffered (allows reducing buffer reallocations)
     * @throws NullPointerException if {@code onNext} is {@code null}
     * @throws IllegalArgumentException if {@code capacityHint} is non-positive
     * @throws RuntimeException
     *             if an error occurs; {@code Error}s and {@code RuntimeException}s are rethrown
     *             as they are, checked {@code Exception}s are wrapped into {@code RuntimeException}s
     * @see <a href="http://reactivex.io/documentation/operators/subscribe.html">ReactiveX documentation: Subscribe</a>
     * @see #subscribe(Consumer)
     */
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final void blockingForEach(@NonNull Consumer<? super T> onNext, int capacityHint) {
        Objects.requireNonNull(onNext, "onNext is null");
        Iterator<T> it = blockingIterable(capacityHint).iterator();
        while (it.hasNext()) {
            try {
                onNext.accept(it.next());
            } catch (Throwable e) {
                Exceptions.throwIfFatal(e);
                ((Disposable)it).dispose();
                throw ExceptionHelper.wrapOrThrow(e);
            }
        }
    }

    /**
     * Exposes the current {@code Observable} as an {@link Iterable} which, when iterated,
     * subscribes to the current {@code Observable} and blocks
     * until the current {@code Observable} emits items or terminates.
     * <p>
     * <img width="640" height="315" src="https://github.com/ReactiveX/RxJava/wiki/images/rx-operators/blockingIterable.o.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code blockingIterable} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @return the new {@code Iterable} instance
     * @see <a href="http://reactivex.io/documentation/operators/to.html">ReactiveX documentation: To</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Iterable<T> blockingIterable() {
        return blockingIterable(bufferSize());
    }

    /**
     * Exposes the current {@code Observable} as an {@link Iterable} which, when iterated,
     * subscribes to the current {@code Observable} and blocks
     * until the current {@code Observable} emits items or terminates.
     * <p>
     * <img width="640" height="315" src="https://github.com/ReactiveX/RxJava/wiki/images/rx-operators/blockingIterable.o.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code blockingIterable} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param capacityHint the expected number of items to be buffered
     * @return the new {@code Iterable} instance
     * @throws IllegalArgumentException if {@code capacityHint} is non-positive
     * @see <a href="http://reactivex.io/documentation/operators/to.html">ReactiveX documentation: To</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Iterable<T> blockingIterable(int capacityHint) {
        ObjectHelper.verifyPositive(capacityHint, "capacityHint");
        return new BlockingObservableIterable<>(this, capacityHint);
    }

    /**
     * Returns the last item emitted by the current {@code Observable}, or throws
     * {@link NoSuchElementException} if the current {@code Observable} emits no items.
     * <p>
     * <img width="640" height="315" src="https://github.com/ReactiveX/RxJava/wiki/images/rx-operators/blockingLast.o.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code blockingLast} does not operate by default on a particular {@link Scheduler}.</dd>
     *  <dt><b>Error handling:</b></dt>
     *  <dd>If the source signals an error, the operator wraps a checked {@link Exception}
     *  into {@link RuntimeException} and throws that. Otherwise, {@code RuntimeException}s and
     *  {@link Error}s are rethrown as they are.</dd>
     * </dl>
     *
     * @return the last item emitted by the current {@code Observable}
     * @throws NoSuchElementException
     *             if the current {@code Observable} emits no items
     * @see <a href="http://reactivex.io/documentation/operators/last.html">ReactiveX documentation: Last</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final T blockingLast() {
        BlockingLastObserver<T> observer = new BlockingLastObserver<>();
        subscribe(observer);
        T v = observer.blockingGet();
        if (v != null) {
            return v;
        }
        throw new NoSuchElementException();
    }

    /**
     * Returns the last item emitted by the current {@code Observable}, or a default value if it emits no
     * items.
     * <p>
     * <img width="640" height="310" src="https://github.com/ReactiveX/RxJava/wiki/images/rx-operators/blockingLastDefault.o.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code blockingLast} does not operate by default on a particular {@link Scheduler}.</dd>
     *  <dt><b>Error handling:</b></dt>
     *  <dd>If the source signals an error, the operator wraps a checked {@link Exception}
     *  into {@link RuntimeException} and throws that. Otherwise, {@code RuntimeException}s and
     *  {@link Error}s are rethrown as they are.</dd>
     * </dl>
     *
     * @param defaultItem
     *            a default value to return if the current {@code Observable} emits no items
     * @return the last item emitted by the {@code Observable}, or the default value if it emits no
     *         items
     * @throws NullPointerException if {@code defaultItem} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/last.html">ReactiveX documentation: Last</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final T blockingLast(@NonNull T defaultItem) {
        Objects.requireNonNull(defaultItem, "defaultItem is null");
        BlockingLastObserver<T> observer = new BlockingLastObserver<>();
        subscribe(observer);
        T v = observer.blockingGet();
        return v != null ? v : defaultItem;
    }

    /**
     * Returns an {@link Iterable} that returns the latest item emitted by the current {@code Observable},
     * waiting if necessary for one to become available.
     * <p>
     * <img width="640" height="350" src="https://github.com/ReactiveX/RxJava/wiki/images/rx-operators/blockingLatest.o.png" alt="">
     * <p>
     * If the current {@code Observable} produces items faster than {@code Iterator.next} takes them,
     * {@code onNext} events might be skipped, but {@code onError} or {@code onComplete} events are not.
     * <p>
     * Note also that an {@code onNext} directly followed by {@code onComplete} might hide the {@code onNext}
     * event.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code blockingLatest} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @return the new {@code Iterable} instance
     * @see <a href="http://reactivex.io/documentation/operators/first.html">ReactiveX documentation: First</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Iterable<T> blockingLatest() {
        return new BlockingObservableLatest<>(this);
    }

    /**
     * Returns an {@link Iterable} that always returns the item most recently emitted by the current
     * {@code Observable}.
     * <p>
     * <img width="640" height="426" src="https://github.com/ReactiveX/RxJava/wiki/images/rx-operators/blockingMostRecent.o.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code blockingMostRecent} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param initialItem
     *            the initial value that the {@code Iterable} sequence will yield if the current
     *            {@code Observable} has not yet emitted an item
     * @return the new {@code Iterable} instance
     * @throws NullPointerException if {@code initialItem} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/first.html">ReactiveX documentation: First</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Iterable<T> blockingMostRecent(@NonNull T initialItem) {
        Objects.requireNonNull(initialItem, "initialItem is null");
        return new BlockingObservableMostRecent<>(this, initialItem);
    }

    /**
     * Returns an {@link Iterable} that blocks until the current {@code Observable} emits another item, then
     * returns that item.
     * <p>
     * <img width="640" height="427" src="https://github.com/ReactiveX/RxJava/wiki/images/rx-operators/blockingNext.o.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code blockingNext} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @return the new {@code Iterable} instance
     * @see <a href="http://reactivex.io/documentation/operators/takelast.html">ReactiveX documentation: TakeLast</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Iterable<T> blockingNext() {
        return new BlockingObservableNext<>(this);
    }

    /**
     * If the current {@code Observable} completes after emitting a single item, return that item, otherwise
     * throw a {@link NoSuchElementException}.
     * <p>
     * <img width="640" height="315" src="https://github.com/ReactiveX/RxJava/wiki/images/rx-operators/blockingSingle.o.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code blockingSingle} does not operate by default on a particular {@link Scheduler}.</dd>
     *  <dt><b>Error handling:</b></dt>
     *  <dd>If the source signals an error, the operator wraps a checked {@link Exception}
     *  into {@link RuntimeException} and throws that. Otherwise, {@code RuntimeException}s and
     *  {@link Error}s are rethrown as they are.</dd>
     * </dl>
     *
     * @return the single item emitted by the current {@code Observable}
     * @see <a href="http://reactivex.io/documentation/operators/first.html">ReactiveX documentation: First</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final T blockingSingle() {
        T v = singleElement().blockingGet();
        if (v == null) {
            throw new NoSuchElementException();
        }
        return v;
    }

    /**
     * If the current {@code Observable} completes after emitting a single item, return that item; if it emits
     * more than one item, throw an {@link IllegalArgumentException}; if it emits no items, return a default
     * value.
     * <p>
     * <img width="640" height="315" src="https://github.com/ReactiveX/RxJava/wiki/images/rx-operators/blockingSingleDefault.o.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code blockingSingle} does not operate by default on a particular {@link Scheduler}.</dd>
     *  <dt><b>Error handling:</b></dt>
     *  <dd>If the source signals an error, the operator wraps a checked {@link Exception}
     *  into {@link RuntimeException} and throws that. Otherwise, {@code RuntimeException}s and
     *  {@link Error}s are rethrown as they are.</dd>
     * </dl>
     *
     * @param defaultItem
     *            a default value to return if the current {@code Observable} emits no items
     * @return the single item emitted by the current {@code Observable}, or the default value if it emits no
     *         items
     * @throws NullPointerException if {@code defaultItem} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/first.html">ReactiveX documentation: First</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final T blockingSingle(@NonNull T defaultItem) {
        return single(defaultItem).blockingGet();
    }

    /**
     * Returns a {@link Future} representing the only value emitted by the current {@code Observable}.
     * <p>
     * <img width="640" height="299" src="https://github.com/ReactiveX/RxJava/wiki/images/rx-operators/toFuture.o.png" alt="">
     * <p>
     * If the {@code Observable} emits more than one item, {@code Future} will receive an
     * {@link IndexOutOfBoundsException}. If the {@code Observable} is empty, {@code Future}
     * will receive an {@link NoSuchElementException}. The {@code Observable} source has to terminate in order
     * for the returned {@code Future} to terminate as well.
     * <p>
     * If the {@code Observable} may emit more than one item, use {@code Observable.toList().toFuture()}.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code toFuture} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @return the new {@code Future} instance
     * @see <a href="http://reactivex.io/documentation/operators/to.html">ReactiveX documentation: To</a>
     * @see #singleOrErrorStage()
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Future<T> toFuture() {
        return subscribeWith(new FutureObserver<>());
    }

    /**
     * Runs the current {@code Observable} to a terminal event, ignoring any values and rethrowing any exception.
     * <p>
     * <img width="640" height="270" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/blockingSubscribe.o.0.png" alt="">
     * <p>
     * Note that calling this method will block the caller thread until the upstream terminates
     * normally or with an error. Therefore, calling this method from special threads such as the
     * Android Main Thread or the Swing Event Dispatch Thread is not recommended.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code blockingSubscribe} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @since 2.0
     * @see #blockingSubscribe(Consumer)
     * @see #blockingSubscribe(Consumer, Consumer)
     * @see #blockingSubscribe(Consumer, Consumer, Action)
     */
    @SchedulerSupport(SchedulerSupport.NONE)
    public final void blockingSubscribe() {
        ObservableBlockingSubscribe.subscribe(this);
    }

    /**
     * Subscribes to the source and calls the given callbacks <strong>on the current thread</strong>.
     * <p>
     * <img width="640" height="394" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/blockingSubscribe.o.1.png" alt="">
     * <p>
     * If the {@code Observable} emits an error, it is wrapped into an
     * {@link OnErrorNotImplementedException}
     * and routed to the {@link RxJavaPlugins#onError(Throwable)} handler.
     * Using the overloads {@link #blockingSubscribe(Consumer, Consumer)}
     * or {@link #blockingSubscribe(Consumer, Consumer, Action)} instead is recommended.
     * <p>
     * Note that calling this method will block the caller thread until the upstream terminates
     * normally or with an error. Therefore, calling this method from special threads such as the
     * Android Main Thread or the Swing Event Dispatch Thread is not recommended.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code blockingSubscribe} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param onNext the callback action for each source value
     * @throws NullPointerException if {@code onNext} is {@code null}
     * @since 2.0
     * @see #blockingSubscribe(Consumer, Consumer)
     * @see #blockingSubscribe(Consumer, Consumer, Action)
     */
    @SchedulerSupport(SchedulerSupport.NONE)
    public final void blockingSubscribe(@NonNull Consumer<? super T> onNext) {
        ObservableBlockingSubscribe.subscribe(this, onNext, Functions.ON_ERROR_MISSING, Functions.EMPTY_ACTION);
    }

    /**
     * Subscribes to the source and calls the given callbacks <strong>on the current thread</strong>.
     * <p>
     * <img width="640" height="397" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/blockingSubscribe.o.2.png" alt="">
     * <p>
     * Note that calling this method will block the caller thread until the upstream terminates
     * normally or with an error. Therefore, calling this method from special threads such as the
     * Android Main Thread or the Swing Event Dispatch Thread is not recommended.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code blockingSubscribe} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param onNext the callback action for each source value
     * @param onError the callback action for an error event
     * @throws NullPointerException if {@code onNext} or {@code onError} is {@code null}
     * @since 2.0
     * @see #blockingSubscribe(Consumer, Consumer, Action)
     */
    @SchedulerSupport(SchedulerSupport.NONE)
    public final void blockingSubscribe(@NonNull Consumer<? super T> onNext, @NonNull Consumer<? super Throwable> onError) {
        ObservableBlockingSubscribe.subscribe(this, onNext, onError, Functions.EMPTY_ACTION);
    }

    /**
     * Subscribes to the source and calls the given callbacks <strong>on the current thread</strong>.
     * <p>
     * <img width="640" height="394" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/blockingSubscribe.o.png" alt="">
     * <p>
     * Note that calling this method will block the caller thread until the upstream terminates
     * normally or with an error. Therefore, calling this method from special threads such as the
     * Android Main Thread or the Swing Event Dispatch Thread is not recommended.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code blockingSubscribe} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param onNext the callback action for each source value
     * @param onError the callback action for an error event
     * @param onComplete the callback action for the completion event.
     * @throws NullPointerException if {@code onNext}, {@code onError} or {@code onComplete} is {@code null}
     * @since 2.0
     */
    @SchedulerSupport(SchedulerSupport.NONE)
    public final void blockingSubscribe(@NonNull Consumer<? super T> onNext, @NonNull Consumer<? super Throwable> onError, @NonNull Action onComplete) {
        ObservableBlockingSubscribe.subscribe(this, onNext, onError, onComplete);
    }

    /**
     * Subscribes to the source and calls the {@link Observer} methods <strong>on the current thread</strong>.
     * <p>
     * Note that calling this method will block the caller thread until the upstream terminates
     * normally, with an error or the {@code Observer} disposes the {@link Disposable} it receives via
     * {@link Observer#onSubscribe(Disposable)}.
     * Therefore, calling this method from special threads such as the
     * Android Main Thread or the Swing Event Dispatch Thread is not recommended.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code blockingSubscribe} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * The a dispose() call is composed through.
     * @param observer the {@code Observer} instance to forward events and calls to in the current thread
     * @throws NullPointerException if {@code observer} is {@code null}
     * @since 2.0
     */
    @SchedulerSupport(SchedulerSupport.NONE)
    public final void blockingSubscribe(@NonNull Observer<? super T> observer) {
        Objects.requireNonNull(observer, "observer is null");
        ObservableBlockingSubscribe.subscribe(this, observer);
    }

    /**
     * Returns an {@code Observable} that emits buffers of items it collects from the current {@code Observable}. The resulting
     * {@code Observable} emits connected, non-overlapping buffers, each containing {@code count} items. When the current
     * {@code Observable} completes, the resulting {@code Observable} emits the current buffer and propagates the notification
     * from the current {@code Observable}. Note that if the current {@code Observable} issues an {@code onError} notification
     * the event is passed on immediately without first emitting the buffer it is in the process of assembling.
     * <p>
     * <img width="640" height="320" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/buffer3.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This version of {@code buffer} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param count
     *            the maximum number of items in each buffer before it should be emitted
     * @return the new {@code Observable} instance
     * @throws IllegalArgumentException if {@code count} is non-positive
     * @see <a href="http://reactivex.io/documentation/operators/buffer.html">ReactiveX operators documentation: Buffer</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Observable<@NonNull List<T>> buffer(int count) {
        return buffer(count, count);
    }

    /**
     * Returns an {@code Observable} that emits buffers of items it collects from the current {@code Observable}. The resulting
     * {@code Observable} emits buffers every {@code skip} items, each containing {@code count} items. When the current
     * {@code Observable} completes, the resulting {@code Observable} emits the current buffer and propagates the notification
     * from the current {@code Observable}. Note that if the current {@code Observable} issues an {@code onError} notification
     * the event is passed on immediately without first emitting the buffer it is in the process of assembling.
     * <p>
     * <img width="640" height="320" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/buffer4.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This version of {@code buffer} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param count
     *            the maximum size of each buffer before it should be emitted
     * @param skip
     *            how many items emitted by the current {@code Observable} should be skipped before starting a new
     *            buffer. Note that when {@code skip} and {@code count} are equal, this is the same operation as
     *            {@link #buffer(int)}.
     * @return the new {@code Observable} instance
     * @throws IllegalArgumentException if {@code count} or {@code skip} is non-positive
     * @see <a href="http://reactivex.io/documentation/operators/buffer.html">ReactiveX operators documentation: Buffer</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Observable<@NonNull List<T>> buffer(int count, int skip) {
        return buffer(count, skip, ArrayListSupplier.asSupplier());
    }

    /**
     * Returns an {@code Observable} that emits buffers of items it collects from the current {@code Observable}. The resulting
     * {@code Observable} emits buffers every {@code skip} items, each containing {@code count} items. When the current
     * {@code Observable} completes, the resulting {@code Observable} emits the current buffer and propagates the notification
     * from the current {@code Observable}. Note that if the current {@code Observable} issues an {@code onError} notification
     * the event is passed on immediately without first emitting the buffer it is in the process of assembling.
     * <p>
     * <img width="640" height="320" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/buffer4.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This version of {@code buffer} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <U> the collection subclass type to buffer into
     * @param count
     *            the maximum size of each buffer before it should be emitted
     * @param skip
     *            how many items emitted by the current {@code Observable} should be skipped before starting a new
     *            buffer. Note that when {@code skip} and {@code count} are equal, this is the same operation as
     *            {@link #buffer(int)}.
     * @param bufferSupplier
     *            a factory function that returns an instance of the collection subclass to be used and returned
     *            as the buffer
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code bufferSupplier} is {@code null}
     * @throws IllegalArgumentException if {@code count} or {@code skip} is non-positive
     * @see <a href="http://reactivex.io/documentation/operators/buffer.html">ReactiveX operators documentation: Buffer</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <@NonNull U extends Collection<? super T>> Observable<U> buffer(int count, int skip, @NonNull Supplier<U> bufferSupplier) {
        ObjectHelper.verifyPositive(count, "count");
        ObjectHelper.verifyPositive(skip, "skip");
        Objects.requireNonNull(bufferSupplier, "bufferSupplier is null");
        return RxJavaPlugins.onAssembly(new ObservableBuffer<>(this, count, skip, bufferSupplier));
    }

    /**
     * Returns an {@code Observable} that emits buffers of items it collects from the current {@code Observable}. The resulting
     * {@code Observable} emits connected, non-overlapping buffers, each containing {@code count} items. When the current
     * {@code Observable} completes, the resulting {@code Observable} emits the current buffer and propagates the notification
     * from the current {@code Observable}. Note that if the current {@code Observable} issues an {@code onError} notification
     * the event is passed on immediately without first emitting the buffer it is in the process of assembling.
     * <p>
     * <img width="640" height="320" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/buffer3.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This version of {@code buffer} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <U> the collection subclass type to buffer into
     * @param count
     *            the maximum number of items in each buffer before it should be emitted
     * @param bufferSupplier
     *            a factory function that returns an instance of the collection subclass to be used and returned
     *            as the buffer
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code bufferSupplier} is {@code null}
     * @throws IllegalArgumentException if {@code count} is non-positive
     * @see <a href="http://reactivex.io/documentation/operators/buffer.html">ReactiveX operators documentation: Buffer</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <@NonNull U extends Collection<? super T>> Observable<U> buffer(int count, @NonNull Supplier<U> bufferSupplier) {
        return buffer(count, count, bufferSupplier);
    }

    /**
     * Returns an {@code Observable} that emits buffers of items it collects from the current {@code Observable}. The resulting
     * {@code Observable} starts a new buffer periodically, as determined by the {@code timeskip} argument. It emits
     * each buffer after a fixed timespan, specified by the {@code timespan} argument. When the current
     * {@code Observable} completes, the resulting {@code Observable} emits the current buffer and propagates the notification
     * from the current {@code Observable}. Note that if the current {@code Observable} issues an {@code onError} notification
     * the event is passed on immediately without first emitting the buffer it is in the process of assembling.
     * <p>
     * <img width="640" height="320" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/buffer7.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This version of {@code buffer} operates by default on the {@code computation} {@link Scheduler}.</dd>
     * </dl>
     *
     * @param timespan
     *            the period of time each buffer collects items before it is emitted
     * @param timeskip
     *            the period of time after which a new buffer will be created
     * @param unit
     *            the unit of time that applies to the {@code timespan} and {@code timeskip} arguments
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code unit} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/buffer.html">ReactiveX operators documentation: Buffer</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.COMPUTATION)
    @NonNull
    public final Observable<@NonNull List<T>> buffer(long timespan, long timeskip, @NonNull TimeUnit unit) {
        return buffer(timespan, timeskip, unit, Schedulers.computation(), ArrayListSupplier.asSupplier());
    }

    /**
     * Returns an {@code Observable} that emits buffers of items it collects from the current {@code Observable}. The resulting
     * {@code Observable} starts a new buffer periodically, as determined by the {@code timeskip} argument, and on the
     * specified {@code scheduler}. It emits each buffer after a fixed timespan, specified by the
     * {@code timespan} argument. When the current {@code Observable} completes, the resulting {@code Observable} emits the
     * current buffer and propagates the notification from the current {@code Observable}. Note that if the current
     * {@code Observable} issues an {@code onError} notification the event is passed on immediately without first emitting the
     * buffer it is in the process of assembling.
     * <p>
     * <img width="640" height="320" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/buffer7.s.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>You specify which {@link Scheduler} this operator will use.</dd>
     * </dl>
     *
     * @param timespan
     *            the period of time each buffer collects items before it is emitted
     * @param timeskip
     *            the period of time after which a new buffer will be created
     * @param unit
     *            the unit of time that applies to the {@code timespan} and {@code timeskip} arguments
     * @param scheduler
     *            the {@code Scheduler} to use when determining the end and start of a buffer
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code unit} or {@code scheduler} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/buffer.html">ReactiveX operators documentation: Buffer</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    @NonNull
    public final Observable<@NonNull List<T>> buffer(long timespan, long timeskip, @NonNull TimeUnit unit, @NonNull Scheduler scheduler) {
        return buffer(timespan, timeskip, unit, scheduler, ArrayListSupplier.asSupplier());
    }

    /**
     * Returns an {@code Observable} that emits buffers of items it collects from the current {@code Observable}. The resulting
     * {@code Observable} starts a new buffer periodically, as determined by the {@code timeskip} argument, and on the
     * specified {@code scheduler}. It emits each buffer after a fixed timespan, specified by the
     * {@code timespan} argument. When the current {@code Observable} completes, the resulting {@code Observable} emits the
     * current buffer and propagates the notification from the current {@code Observable}. Note that if the current
     * {@code Observable} issues an {@code onError} notification the event is passed on immediately without first emitting the
     * buffer it is in the process of assembling.
     * <p>
     * <img width="640" height="320" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/buffer7.s.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>You specify which {@link Scheduler} this operator will use.</dd>
     * </dl>
     *
     * @param <U> the collection subclass type to buffer into
     * @param timespan
     *            the period of time each buffer collects items before it is emitted
     * @param timeskip
     *            the period of time after which a new buffer will be created
     * @param unit
     *            the unit of time that applies to the {@code timespan} and {@code timeskip} arguments
     * @param scheduler
     *            the {@code Scheduler} to use when determining the end and start of a buffer
     * @param bufferSupplier
     *            a factory function that returns an instance of the collection subclass to be used and returned
     *            as the buffer
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code unit}, {@code scheduler} or {@code bufferSupplier} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/buffer.html">ReactiveX operators documentation: Buffer</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    @NonNull
    public final <@NonNull U extends Collection<? super T>> Observable<U> buffer(long timespan, long timeskip, @NonNull TimeUnit unit, @NonNull Scheduler scheduler, @NonNull Supplier<U> bufferSupplier) {
        Objects.requireNonNull(unit, "unit is null");
        Objects.requireNonNull(scheduler, "scheduler is null");
        Objects.requireNonNull(bufferSupplier, "bufferSupplier is null");
        return RxJavaPlugins.onAssembly(new ObservableBufferTimed<>(this, timespan, timeskip, unit, scheduler, bufferSupplier, Integer.MAX_VALUE, false));
    }

    /**
     * Returns an {@code Observable} that emits buffers of items it collects from the current {@code Observable}. The resulting
     * {@code Observable} emits connected, non-overlapping buffers, each of a fixed duration specified by the
     * {@code timespan} argument. When the current {@code Observable} completes, the resulting {@code Observable} emits the
     * current buffer and propagates the notification from the current {@code Observable}. Note that if the current
     * {@code Observable} issues an {@code onError} notification the event is passed on immediately without first emitting the
     * buffer it is in the process of assembling.
     * <p>
     * <img width="640" height="320" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/buffer5.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This version of {@code buffer} operates by default on the {@code computation} {@link Scheduler}.</dd>
     * </dl>
     *
     * @param timespan
     *            the period of time each buffer collects items before it is emitted and replaced with a new
     *            buffer
     * @param unit
     *            the unit of time that applies to the {@code timespan} argument
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code unit} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/buffer.html">ReactiveX operators documentation: Buffer</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.COMPUTATION)
    @NonNull
    public final Observable<@NonNull List<T>> buffer(long timespan, @NonNull TimeUnit unit) {
        return buffer(timespan, unit, Schedulers.computation(), Integer.MAX_VALUE);
    }

    /**
     * Returns an {@code Observable} that emits buffers of items it collects from the current {@code Observable}. The resulting
     * {@code Observable} emits connected, non-overlapping buffers, each of a fixed duration specified by the
     * {@code timespan} argument or a maximum size specified by the {@code count} argument (whichever is reached
     * first). When the current {@code Observable} completes, the resulting {@code Observable} emits the current buffer and
     * propagates the notification from the current {@code Observable}. Note that if the current {@code Observable} issues an
     * {@code onError} notification the event is passed on immediately without first emitting the buffer it is in the process of
     * assembling.
     * <p>
     * <img width="640" height="320" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/buffer6.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This version of {@code buffer} operates by default on the {@code computation} {@link Scheduler}.</dd>
     * </dl>
     *
     * @param timespan
     *            the period of time each buffer collects items before it is emitted and replaced with a new
     *            buffer
     * @param unit
     *            the unit of time which applies to the {@code timespan} argument
     * @param count
     *            the maximum size of each buffer before it is emitted
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code unit} is {@code null}
     * @throws IllegalArgumentException if {@code count} is non-positive
     * @see <a href="http://reactivex.io/documentation/operators/buffer.html">ReactiveX operators documentation: Buffer</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.COMPUTATION)
    @NonNull
    public final Observable<@NonNull List<T>> buffer(long timespan, @NonNull TimeUnit unit, int count) {
        return buffer(timespan, unit, Schedulers.computation(), count);
    }

    /**
     * Returns an {@code Observable} that emits buffers of items it collects from the current {@code Observable}. The resulting
     * {@code Observable} emits connected, non-overlapping buffers, each of a fixed duration specified by the
     * {@code timespan} argument as measured on the specified {@code scheduler}, or a maximum size specified by
     * the {@code count} argument (whichever is reached first). When the current {@code Observable} completes, the resulting
     * {@code Observable} emits the current buffer and propagates the notification from the current {@code Observable}. Note
     * that if the current {@code Observable} issues an {@code onError} notification the event is passed on immediately without
     * first emitting the buffer it is in the process of assembling.
     * <p>
     * <img width="640" height="320" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/buffer6.s.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>You specify which {@link Scheduler} this operator will use.</dd>
     * </dl>
     *
     * @param timespan
     *            the period of time each buffer collects items before it is emitted and replaced with a new
     *            buffer
     * @param unit
     *            the unit of time which applies to the {@code timespan} argument
     * @param scheduler
     *            the {@code Scheduler} to use when determining the end and start of a buffer
     * @param count
     *            the maximum size of each buffer before it is emitted
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code unit} or {@code scheduler} is {@code null}
     * @throws IllegalArgumentException if {@code count} is non-positive
     * @see <a href="http://reactivex.io/documentation/operators/buffer.html">ReactiveX operators documentation: Buffer</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    @NonNull
    public final Observable<@NonNull List<T>> buffer(long timespan, @NonNull TimeUnit unit, @NonNull Scheduler scheduler, int count) {
        return buffer(timespan, unit, scheduler, count, ArrayListSupplier.asSupplier(), false);
    }

    /**
     * Returns an {@code Observable} that emits buffers of items it collects from the current {@code Observable}. The resulting
     * {@code Observable} emits connected, non-overlapping buffers, each of a fixed duration specified by the
     * {@code timespan} argument as measured on the specified {@code scheduler}, or a maximum size specified by
     * the {@code count} argument (whichever is reached first). When the current {@code Observable} completes, the resulting
     * {@code Observable} emits the current buffer and propagates the notification from the current {@code Observable}. Note
     * that if the current {@code Observable} issues an {@code onError} notification the event is passed on immediately without
     * first emitting the buffer it is in the process of assembling.
     * <p>
     * <img width="640" height="320" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/buffer6.s.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>You specify which {@link Scheduler} this operator will use.</dd>
     * </dl>
     *
     * @param <U> the collection subclass type to buffer into
     * @param timespan
     *            the period of time each buffer collects items before it is emitted and replaced with a new
     *            buffer
     * @param unit
     *            the unit of time which applies to the {@code timespan} argument
     * @param scheduler
     *            the {@code Scheduler} to use when determining the end and start of a buffer
     * @param count
     *            the maximum size of each buffer before it is emitted
     * @param bufferSupplier
     *            a factory function that returns an instance of the collection subclass to be used and returned
     *            as the buffer
     * @param restartTimerOnMaxSize if {@code true}, the time window is restarted when the max capacity of the current buffer
     *            is reached
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code unit}, {@code scheduler} or {@code bufferSupplier} is {@code null}
     * @throws IllegalArgumentException if {@code count} is non-positive
     * @see <a href="http://reactivex.io/documentation/operators/buffer.html">ReactiveX operators documentation: Buffer</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    @NonNull
    public final <@NonNull U extends Collection<? super T>> Observable<U> buffer(
            long timespan, @NonNull TimeUnit unit,
            @NonNull Scheduler scheduler, int count,
            @NonNull Supplier<U> bufferSupplier,
            boolean restartTimerOnMaxSize) {
        Objects.requireNonNull(unit, "unit is null");
        Objects.requireNonNull(scheduler, "scheduler is null");
        Objects.requireNonNull(bufferSupplier, "bufferSupplier is null");
        ObjectHelper.verifyPositive(count, "count");
        return RxJavaPlugins.onAssembly(new ObservableBufferTimed<>(this, timespan, timespan, unit, scheduler, bufferSupplier, count, restartTimerOnMaxSize));
    }

    /**
     * Returns an {@code Observable} that emits buffers of items it collects from the current {@code Observable}. The resulting
     * {@code Observable} emits connected, non-overlapping buffers, each of a fixed duration specified by the
     * {@code timespan} argument and on the specified {@code scheduler}. When the current {@code Observable} completes,
     * the resulting {@code Observable} emits the current buffer and propagates the notification from the current
     * {@code Observable}. Note that if the current {@code Observable} issues an {@code onError} notification the event is passed on
     * immediately without first emitting the buffer it is in the process of assembling.
     * <p>
     * <img width="640" height="320" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/buffer5.s.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>You specify which {@link Scheduler} this operator will use.</dd>
     * </dl>
     *
     * @param timespan
     *            the period of time each buffer collects items before it is emitted and replaced with a new
     *            buffer
     * @param unit
     *            the unit of time which applies to the {@code timespan} argument
     * @param scheduler
     *            the {@code Scheduler} to use when determining the end and start of a buffer
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code unit} or {@code scheduler} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/buffer.html">ReactiveX operators documentation: Buffer</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    @NonNull
    public final Observable<@NonNull List<T>> buffer(long timespan, @NonNull TimeUnit unit, @NonNull Scheduler scheduler) {
        return buffer(timespan, unit, scheduler, Integer.MAX_VALUE, ArrayListSupplier.asSupplier(), false);
    }

    /**
     * Returns an {@code Observable} that emits buffers of items it collects from the current {@code Observable}. The resulting
     * {@code Observable} emits buffers that it creates when the specified {@code openingIndicator} {@link ObservableSource} emits an
     * item, and closes when the {@code ObservableSource} returned from {@code closingIndicator} emits an item. If any of the
     * current {@code Observable}, {@code openingIndicator} or {@code closingIndicator} issues an {@code onError} notification the
     * event is passed on immediately without first emitting the buffer it is in the process of assembling.
     * <p>
     * <img width="640" height="470" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/buffer2.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This version of {@code buffer} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <TOpening> the element type of the buffer-opening {@code ObservableSource}
     * @param <TClosing> the element type of the individual buffer-closing {@code ObservableSource}s
     * @param openingIndicator
     *            the {@code ObservableSource} that, when it emits an item, causes a new buffer to be created
     * @param closingIndicator
     *            the {@link Function} that is used to produce an {@code ObservableSource} for every buffer created. When this indicator
     *            {@code ObservableSource} emits an item, the associated buffer is emitted.
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code openingIndicator} or {@code closingIndicator} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/buffer.html">ReactiveX operators documentation: Buffer</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <@NonNull TOpening, @NonNull TClosing> Observable<@NonNull List<T>> buffer(
            @NonNull ObservableSource<? extends TOpening> openingIndicator,
            @NonNull Function<? super TOpening, ? extends ObservableSource<? extends TClosing>> closingIndicator) {
        return buffer(openingIndicator, closingIndicator, ArrayListSupplier.asSupplier());
    }

    /**
     * Returns an {@code Observable} that emits buffers of items it collects from the current {@code Observable}. The resulting
     * {@code Observable} emits buffers that it creates when the specified {@code openingIndicator} {@link ObservableSource} emits an
     * item, and closes when the {@code ObservableSource} returned from {@code closingIndicator} emits an item. If any of the
     * current {@code Observable}, {@code openingIndicator} or {@code closingIndicator} issues an {@code onError} notification the
     * event is passed on immediately without first emitting the buffer it is in the process of assembling.
     * <p>
     * <img width="640" height="470" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/buffer2.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This version of {@code buffer} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <U> the collection subclass type to buffer into
     * @param <TOpening> the element type of the buffer-opening {@code ObservableSource}
     * @param <TClosing> the element type of the individual buffer-closing {@code ObservableSource}s
     * @param openingIndicator
     *            the {@code ObservableSource} that, when it emits an item, causes a new buffer to be created
     * @param closingIndicator
     *            the {@link Function} that is used to produce an {@code ObservableSource} for every buffer created. When this indicator
     *            {@code ObservableSource} emits an item, the associated buffer is emitted.
     * @param bufferSupplier
     *            a factory function that returns an instance of the collection subclass to be used and returned
     *            as the buffer
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code openingIndicator}, {@code closingIndicator} or {@code bufferSupplier} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/buffer.html">ReactiveX operators documentation: Buffer</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <@NonNull TOpening, @NonNull TClosing, @NonNull U extends Collection<? super T>> Observable<U> buffer(
            @NonNull ObservableSource<? extends TOpening> openingIndicator,
            @NonNull Function<? super TOpening, ? extends ObservableSource<? extends TClosing>> closingIndicator,
            @NonNull Supplier<U> bufferSupplier) {
        Objects.requireNonNull(openingIndicator, "openingIndicator is null");
        Objects.requireNonNull(closingIndicator, "closingIndicator is null");
        Objects.requireNonNull(bufferSupplier, "bufferSupplier is null");
        return RxJavaPlugins.onAssembly(new ObservableBufferBoundary<T, U, TOpening, TClosing>(this, openingIndicator, closingIndicator, bufferSupplier));
    }

    /**
     * Returns an {@code Observable} that emits non-overlapping buffered items from the current {@code Observable} each time the
     * specified boundary {@link ObservableSource} emits an item.
     * <p>
     * <img width="640" height="395" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/buffer8.v3.png" alt="">
     * <p>
     * Completion of either the source or the boundary {@code ObservableSource} causes the returned {@code ObservableSource} to emit the
     * latest buffer and complete. If either the current {@code Observable} or the boundary {@code ObservableSource} issues an
     * {@code onError} notification the event is passed on immediately without first emitting the buffer it is in the process of
     * assembling.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This version of {@code buffer} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <B>
     *            the boundary value type (ignored)
     * @param boundaryIndicator
     *            the boundary {@code ObservableSource}
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code boundaryIndicator} is {@code null}
     * @see #buffer(ObservableSource, int)
     * @see <a href="http://reactivex.io/documentation/operators/buffer.html">ReactiveX operators documentation: Buffer</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <@NonNull B> Observable<@NonNull List<T>> buffer(@NonNull ObservableSource<B> boundaryIndicator) {
        return buffer(boundaryIndicator, ArrayListSupplier.asSupplier());
    }

    /**
     * Returns an {@code Observable} that emits non-overlapping buffered items from the current {@code Observable} each time the
     * specified boundary {@link ObservableSource} emits an item.
     * <p>
     * <img width="640" height="395" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/buffer8.v3.png" alt="">
     * <p>
     * Completion of either the source or the boundary {@code ObservableSource} causes the returned {@code ObservableSource} to emit the
     * latest buffer and complete. If either the current {@code Observable} or the boundary {@code ObservableSource} issues an
     * {@code onError} notification the event is passed on immediately without first emitting the buffer it is in the process of
     * assembling.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This version of {@code buffer} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <B>
     *            the boundary value type (ignored)
     * @param boundaryIndicator
     *            the boundary {@code ObservableSource}
     * @param initialCapacity
     *            the initial capacity of each buffer chunk
     * @return the new {@code Observable} instance
     * @see <a href="http://reactivex.io/documentation/operators/buffer.html">ReactiveX operators documentation: Buffer</a>
     * @throws NullPointerException if {@code boundaryIndicator} is {@code null}
     * @throws IllegalArgumentException if {@code initialCapacity} is non-positive
     * @see #buffer(ObservableSource)
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <@NonNull B> Observable<@NonNull List<T>> buffer(@NonNull ObservableSource<B> boundaryIndicator, int initialCapacity) {
        ObjectHelper.verifyPositive(initialCapacity, "initialCapacity");
        return buffer(boundaryIndicator, Functions.createArrayList(initialCapacity));
    }

    /**
     * Returns an {@code Observable} that emits non-overlapping buffered items from the current {@code Observable} each time the
     * specified boundary {@link ObservableSource} emits an item.
     * <p>
     * <img width="640" height="395" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/buffer8.v3.png" alt="">
     * <p>
     * Completion of either the source or the boundary {@code ObservableSource} causes the returned {@code ObservableSource} to emit the
     * latest buffer and complete. If either the current {@code Observable} or the boundary {@code ObservableSource} issues an
     * {@code onError} notification the event is passed on immediately without first emitting the buffer it is in the process of
     * assembling.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This version of {@code buffer} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <U> the collection subclass type to buffer into
     * @param <B>
     *            the boundary value type (ignored)
     * @param boundaryIndicator
     *            the boundary {@code ObservableSource}
     * @param bufferSupplier
     *            a factory function that returns an instance of the collection subclass to be used and returned
     *            as the buffer
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code boundaryIndicator} or {@code bufferSupplier} is {@code null}
     * @see #buffer(ObservableSource, int)
     * @see <a href="http://reactivex.io/documentation/operators/buffer.html">ReactiveX operators documentation: Buffer</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <@NonNull B, @NonNull U extends Collection<? super T>> Observable<U> buffer(@NonNull ObservableSource<B> boundaryIndicator, @NonNull Supplier<U> bufferSupplier) {
        Objects.requireNonNull(boundaryIndicator, "boundaryIndicator is null");
        Objects.requireNonNull(bufferSupplier, "bufferSupplier is null");
        return RxJavaPlugins.onAssembly(new ObservableBufferExactBoundary<>(this, boundaryIndicator, bufferSupplier));
    }

    /**
     * Returns an {@code Observable} that subscribes to the current {@code Observable} lazily, caches all of its events
     * and replays them, in the same order as received, to all the downstream observers.
     * <p>
     * <img width="640" height="410" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/cache.v3.png" alt="">
     * <p>
     * This is useful when you want an {@code Observable} to cache responses and you can't control the
     * subscribe/dispose behavior of all the {@link Observer}s.
     * <p>
     * The operator subscribes only when the first downstream observer subscribes and maintains
     * a single subscription towards the current {@code Observable}. In contrast, the operator family of {@link #replay()}
     * that return a {@link ConnectableObservable} require an explicit call to {@link ConnectableObservable#connect()}.
     * <p>
     * <em>Note:</em> You sacrifice the ability to dispose the origin when you use the {@code cache}
     * operator so be careful not to use this operator on {@code Observable}s that emit an infinite or very large number
     * of items that will use up memory.
     * A possible workaround is to apply {@code takeUntil} with a predicate or
     * another source before (and perhaps after) the application of {@code cache()}.
     * <pre><code>
     * AtomicBoolean shouldStop = new AtomicBoolean();
     *
     * source.takeUntil(v -&gt; shouldStop.get())
     *       .cache()
     *       .takeUntil(v -&gt; shouldStop.get())
     *       .subscribe(...);
     * </code></pre>
     * Since the operator doesn't allow clearing the cached values either, the possible workaround is
     * to forget all references to it via {@link #onTerminateDetach()} applied along with the previous
     * workaround:
     * <pre><code>
     * AtomicBoolean shouldStop = new AtomicBoolean();
     *
     * source.takeUntil(v -&gt; shouldStop.get())
     *       .onTerminateDetach()
     *       .cache()
     *       .takeUntil(v -&gt; shouldStop.get())
     *       .onTerminateDetach()
     *       .subscribe(...);
     * </code></pre>
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code cache} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @return the new {@code Observable} instance
     * @see <a href="http://reactivex.io/documentation/operators/replay.html">ReactiveX operators documentation: Replay</a>
     * @see #takeUntil(Predicate)
     * @see #takeUntil(ObservableSource)
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Observable<T> cache() {
        return cacheWithInitialCapacity(16);
    }

    /**
     * Returns an {@code Observable} that subscribes to the current {@code Observable} lazily, caches all of its events
     * and replays them, in the same order as received, to all the downstream observers.
     * <p>
     * <img width="640" height="410" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/cacheWithInitialCapacity.o.v3.png" alt="">
     * <p>
     * This is useful when you want an {@code Observable} to cache responses and you can't control the
     * subscribe/dispose behavior of all the {@link Observer}s.
     * <p>
     * The operator subscribes only when the first downstream observer subscribes and maintains
     * a single subscription towards the current {@code Observable}. In contrast, the operator family of {@link #replay()}
     * that return a {@link ConnectableObservable} require an explicit call to {@link ConnectableObservable#connect()}.
     * <p>
     * <em>Note:</em> You sacrifice the ability to dispose the origin when you use the {@code cache}
     * operator so be careful not to use this operator on {@code Observable}s that emit an infinite or very large number
     * of items that will use up memory.
     * A possible workaround is to apply `takeUntil` with a predicate or
     * another source before (and perhaps after) the application of {@code cache()}.
     * <pre><code>
     * AtomicBoolean shouldStop = new AtomicBoolean();
     *
     * source.takeUntil(v -&gt; shouldStop.get())
     *       .cache()
     *       .takeUntil(v -&gt; shouldStop.get())
     *       .subscribe(...);
     * </code></pre>
     * Since the operator doesn't allow clearing the cached values either, the possible workaround is
     * to forget all references to it via {@link #onTerminateDetach()} applied along with the previous
     * workaround:
     * <pre><code>
     * AtomicBoolean shouldStop = new AtomicBoolean();
     *
     * source.takeUntil(v -&gt; shouldStop.get())
     *       .onTerminateDetach()
     *       .cache()
     *       .takeUntil(v -&gt; shouldStop.get())
     *       .onTerminateDetach()
     *       .subscribe(...);
     * </code></pre>
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code cacheWithInitialCapacity} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * <p>
     * <em>Note:</em> The capacity hint is not an upper bound on cache size. For that, consider
     * {@link #replay(int)} in combination with {@link ConnectableObservable#autoConnect()} or similar.
     *
     * @param initialCapacity hint for number of items to cache (for optimizing underlying data structure)
     * @return the new {@code Observable} instance
     * @throws IllegalArgumentException if {@code initialCapacity} is non-positive
     * @see <a href="http://reactivex.io/documentation/operators/replay.html">ReactiveX operators documentation: Replay</a>
     * @see #takeUntil(Predicate)
     * @see #takeUntil(ObservableSource)
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Observable<T> cacheWithInitialCapacity(int initialCapacity) {
        ObjectHelper.verifyPositive(initialCapacity, "initialCapacity");
        return RxJavaPlugins.onAssembly(new ObservableCache<>(this, initialCapacity));
    }

    /**
     * Returns an {@code Observable} that emits the items emitted by the current {@code Observable}, converted to the specified
     * type.
     * <p>
     * <img width="640" height="305" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/cast.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code cast} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <U> the output value type cast to
     * @param clazz
     *            the target class type that {@code cast} will cast the items emitted by the current {@code Observable}
     *            into before emitting them from the resulting {@code Observable}
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code clazz} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/map.html">ReactiveX operators documentation: Map</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <@NonNull U> Observable<U> cast(@NonNull Class<U> clazz) {
        Objects.requireNonNull(clazz, "clazz is null");
        return map(Functions.castFunction(clazz));
    }

    /**
     * Collects items emitted by the finite source {@code Observable} into a single mutable data structure and returns
     * a {@link Single} that emits this structure.
     * <p>
     * <img width="640" height="330" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/collect.2.v3.png" alt="">
     * <p>
     * This is a simplified version of {@code reduce} that does not need to return the state on each pass.
     * <p>
     * Note that this operator requires the upstream to signal {@code onComplete} for the accumulator object to
     * be emitted. Sources that are infinite and never complete will never emit anything through this
     * operator and an infinite source may lead to a fatal {@link OutOfMemoryError}.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code collect} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <U> the accumulator and output type
     * @param initialItemSupplier
     *           the mutable data structure that will collect the items
     * @param collector
     *           a function that accepts the {@code state} and an emitted item, and modifies the accumulator accordingly
     *           accordingly
     * @return the new {@code Single} instance
     * @throws NullPointerException if {@code initialItemSupplier} or {@code collector} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/reduce.html">ReactiveX operators documentation: Reduce</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <@NonNull U> Single<U> collect(@NonNull Supplier<? extends U> initialItemSupplier, @NonNull BiConsumer<? super U, ? super T> collector) {
        Objects.requireNonNull(initialItemSupplier, "initialItemSupplier is null");
        Objects.requireNonNull(collector, "collector is null");
        return RxJavaPlugins.onAssembly(new ObservableCollectSingle<>(this, initialItemSupplier, collector));
    }

    /**
     * Collects items emitted by the finite source {@code Observable} into a single mutable data structure and returns
     * a {@link Single} that emits this structure.
     * <p>
     * <img width="640" height="330" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/collectInto.o.v3.png" alt="">
     * <p>
     * This is a simplified version of {@code reduce} that does not need to return the state on each pass.
     * <p>
     * Note that this operator requires the upstream to signal {@code onComplete} for the accumulator object to
     * be emitted. Sources that are infinite and never complete will never emit anything through this
     * operator and an infinite source may lead to a fatal {@link OutOfMemoryError}.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code collectInto} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <U> the accumulator and output type
     * @param initialItem
     *           the mutable data structure that will collect the items
     * @param collector
     *           a function that accepts the {@code state} and an emitted item, and modifies the accumulator accordingly
     *           accordingly
     * @return the new {@code Single} instance
     * @throws NullPointerException if {@code initialItem} or {@code collector} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/reduce.html">ReactiveX operators documentation: Reduce</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <@NonNull U> Single<U> collectInto(@NonNull U initialItem, @NonNull BiConsumer<? super U, ? super T> collector) {
        Objects.requireNonNull(initialItem, "initialItem is null");
        return collect(Functions.justSupplier(initialItem), collector);
    }

    /**
     * Transform the current {@code Observable} by applying a particular {@link ObservableTransformer} function to it.
     * <p>
     * This method operates on the {@code Observable} itself whereas {@link #lift} operates on the {@link ObservableSource}'s
     * {@link Observer}s.
     * <p>
     * If the operator you are creating is designed to act on the individual items emitted by the current
     * {@code Observable}, use {@link #lift}. If your operator is designed to transform the current {@code Observable} as a whole
     * (for instance, by applying a particular set of existing RxJava operators to it) use {@code compose}.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code compose} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <R> the value type of the output {@code ObservableSource}
     * @param composer implements the function that transforms the current {@code Observable}
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code composer} is {@code null}
     * @see <a href="https://github.com/ReactiveX/RxJava/wiki/Implementing-Your-Own-Operators">RxJava wiki: Implementing Your Own Operators</a>
     */
    @SuppressWarnings("unchecked")
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <@NonNull R> Observable<R> compose(@NonNull ObservableTransformer<? super T, ? extends R> composer) {
        return wrap(((ObservableTransformer<T, R>) Objects.requireNonNull(composer, "composer is null")).apply(this));
    }

    /**
     * Returns a new {@code Observable} that emits items resulting from applying a function that you supply to each item
     * emitted by the current {@code Observable}, where that function returns an {@link ObservableSource}, and then emitting the items
     * that result from concatenating those returned {@code ObservableSource}s.
     * <p>
     * <img width="640" height="305" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/concatMap.v3.png" alt="">
     * <p>
     * Note that there is no guarantee where the given {@code mapper} function will be executed; it could be on the subscribing thread,
     * on the upstream thread signaling the new item to be mapped or on the thread where the inner source terminates. To ensure
     * the {@code mapper} function is confined to a known thread, use the {@link #concatMap(Function, int, Scheduler)} overload.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code concatMap} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <R> the type of the inner {@code ObservableSource} sources and thus the output type
     * @param mapper
     *            a function that, when applied to an item emitted by the current {@code Observable}, returns an
     *            {@code ObservableSource}
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code mapper} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/flatmap.html">ReactiveX operators documentation: FlatMap</a>
     * @see #concatMap(Function, int, Scheduler)
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <@NonNull R> Observable<R> concatMap(@NonNull Function<? super T, ? extends ObservableSource<? extends R>> mapper) {
        return concatMap(mapper, 2);
    }

    /**
     * Returns a new {@code Observable} that emits items resulting from applying a function that you supply to each item
     * emitted by the current {@code Observable}, where that function returns an {@link ObservableSource}, and then emitting the items
     * that result from concatenating those returned {@code ObservableSource}s.
     * <p>
     * <img width="640" height="305" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/concatMap.v3.png" alt="">
     * <p>
     * Note that there is no guarantee where the given {@code mapper} function will be executed; it could be on the subscribing thread,
     * on the upstream thread signaling the new item to be mapped or on the thread where the inner source terminates. To ensure
     * the {@code mapper} function is confined to a known thread, use the {@link #concatMap(Function, int, Scheduler)} overload.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code concatMap} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <R> the type of the inner {@code ObservableSource} sources and thus the output type
     * @param mapper
     *            a function that, when applied to an item emitted by the current {@code Observable}, returns an
     *            {@code ObservableSource}
     * @param bufferSize
     *            the number of elements expected from the current {@code Observable} to be buffered
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code mapper} is {@code null}
     * @throws IllegalArgumentException if {@code bufferSize} is non-positive
     * @see <a href="http://reactivex.io/documentation/operators/flatmap.html">ReactiveX operators documentation: FlatMap</a>
     * @see #concatMap(Function, int, Scheduler)
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <@NonNull R> Observable<R> concatMap(@NonNull Function<? super T, ? extends ObservableSource<? extends R>> mapper, int bufferSize) {
        Objects.requireNonNull(mapper, "mapper is null");
        ObjectHelper.verifyPositive(bufferSize, "bufferSize");
        if (this instanceof ScalarSupplier) {
            @SuppressWarnings("unchecked")
            T v = ((ScalarSupplier<T>)this).get();
            if (v == null) {
                return empty();
            }
            return ObservableScalarXMap.scalarXMap(v, mapper);
        }
        return RxJavaPlugins.onAssembly(new ObservableConcatMap<>(this, mapper, bufferSize, ErrorMode.IMMEDIATE));
    }

    /**
     * Returns a new {@code Observable} that emits items resulting from applying a function that you supply to each item
     * emitted by the current {@code Observable}, where that function returns an {@link ObservableSource}, and then emitting the items
     * that result from concatenating those returned {@code ObservableSource}s.
     * <p>
     * <img width="640" height="305" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/concatMap.v3.png" alt="">
     * <p>
     * The difference between {@link #concatMap(Function, int)} and this operator is that this operator guarantees the {@code mapper}
     * function is executed on the specified scheduler.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code concatMap} executes the given {@code mapper} function on the provided {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <R> the type of the inner {@code ObservableSource} sources and thus the output type
     * @param mapper
     *            a function that, when applied to an item emitted by the current {@code Observable}, returns an
     *            {@code ObservableSource}
     * @param bufferSize
     *            the number of elements expected from the current {@code Observable} to be buffered
     * @param scheduler
     *            the scheduler where the {@code mapper} function will be executed
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code mapper} or {@code scheduler} is {@code null}
     * @throws IllegalArgumentException if {@code bufferSize} is non-positive
     * @since 3.0.0
     * @see <a href="http://reactivex.io/documentation/operators/flatmap.html">ReactiveX operators documentation: FlatMap</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    @NonNull
    public final <@NonNull R> Observable<R> concatMap(@NonNull Function<? super T, ? extends ObservableSource<? extends R>> mapper, int bufferSize, @NonNull Scheduler scheduler) {
        Objects.requireNonNull(mapper, "mapper is null");
        ObjectHelper.verifyPositive(bufferSize, "bufferSize");
        Objects.requireNonNull(scheduler, "scheduler is null");
        return RxJavaPlugins.onAssembly(new ObservableConcatMapScheduler<>(this, mapper, bufferSize, ErrorMode.IMMEDIATE, scheduler));
    }

    /**
     * Maps each of the items into an {@link ObservableSource}, subscribes to them one after the other,
     * one at a time and emits their values in order
     * while delaying any error from either this or any of the inner {@code ObservableSource}s
     * till all of them terminate.
     * <p>
     * <img width="640" height="348" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/concatMapDelayError.o.png" alt="">
     * <p>
     * Note that there is no guarantee where the given {@code mapper} function will be executed; it could be on the subscribing thread,
     * on the upstream thread signaling the new item to be mapped or on the thread where the inner source terminates. To ensure
     * the {@code mapper} function is confined to a known thread, use the {@link #concatMapDelayError(Function, boolean, int, Scheduler)} overload.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code concatMapDelayError} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <R> the result value type
     * @param mapper the function that maps the items of the current {@code Observable} into the inner {@code ObservableSource}s.
     * @return the new {@code Observable} instance with the concatenation behavior
     * @throws NullPointerException if {@code mapper} is {@code null}
     * @see #concatMapDelayError(Function, boolean, int, Scheduler)
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <@NonNull R> Observable<R> concatMapDelayError(@NonNull Function<? super T, ? extends ObservableSource<? extends R>> mapper) {
        return concatMapDelayError(mapper, true, bufferSize());
    }

    /**
     * Maps each of the items into an {@link ObservableSource}, subscribes to them one after the other,
     * one at a time and emits their values in order
     * while delaying any error from either this or any of the inner {@code ObservableSource}s
     * till all of them terminate.
     * <p>
     * <img width="640" height="348" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/concatMapDelayError.o.png" alt="">
     * <p>
     * Note that there is no guarantee where the given {@code mapper} function will be executed; it could be on the subscribing thread,
     * on the upstream thread signaling the new item to be mapped or on the thread where the inner source terminates. To ensure
     * the {@code mapper} function is confined to a known thread, use the {@link #concatMapDelayError(Function, boolean, int, Scheduler)} overload.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code concatMapDelayError} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <R> the result value type
     * @param mapper the function that maps the items of the current {@code Observable} into the inner {@code ObservableSource}s.
     * @param tillTheEnd
     *            if {@code true}, all errors from the outer and inner {@code ObservableSource} sources are delayed until the end,
     *            if {@code false}, an error from the main source is signaled when the current {@code Observable} source terminates
     * @param bufferSize
     *            the number of elements expected from the current {@code Observable} to be buffered
     * @return the new {@code Observable} instance with the concatenation behavior
     * @throws NullPointerException if {@code mapper} is {@code null}
     * @throws IllegalArgumentException if {@code bufferSize} is non-positive
     * @see #concatMapDelayError(Function, boolean, int, Scheduler)
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <@NonNull R> Observable<R> concatMapDelayError(@NonNull Function<? super T, ? extends ObservableSource<? extends R>> mapper,
            boolean tillTheEnd, int bufferSize) {
        Objects.requireNonNull(mapper, "mapper is null");
        ObjectHelper.verifyPositive(bufferSize, "bufferSize");
        if (this instanceof ScalarSupplier) {
            @SuppressWarnings("unchecked")
            T v = ((ScalarSupplier<T>)this).get();
            if (v == null) {
                return empty();
            }
            return ObservableScalarXMap.scalarXMap(v, mapper);
        }
        return RxJavaPlugins.onAssembly(new ObservableConcatMap<>(this, mapper, bufferSize, tillTheEnd ? ErrorMode.END : ErrorMode.BOUNDARY));
    }

    /**
     * Maps each of the items into an {@link ObservableSource}, subscribes to them one after the other,
     * one at a time and emits their values in order
     * while delaying any error from either this or any of the inner {@code ObservableSource}s
     * till all of them terminate.
     * <p>
     * <img width="640" height="348" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/concatMapDelayError.o.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code concatMapDelayError} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <R> the result value type
     * @param mapper the function that maps the items of the current {@code Observable} into the inner {@code ObservableSource}s.
     * @param tillTheEnd
     *            if {@code true}, all errors from the outer and inner {@code ObservableSource} sources are delayed until the end,
     *            if {@code false}, an error from the main source is signaled when the current {@code Observable} source terminates
     * @param bufferSize
     *            the number of elements expected from the current {@code Observable} to be buffered
     * @param scheduler
     *            the scheduler where the {@code mapper} function will be executed
     * @return the new {@code Observable} instance with the concatenation behavior
     * @throws NullPointerException if {@code mapper} or {@code scheduler} is {@code null}
     * @throws IllegalArgumentException if {@code bufferSize} is non-positive
     * @see #concatMapDelayError(Function, boolean, int)
     * @since 3.0.0
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    @NonNull
    public final <@NonNull R> Observable<R> concatMapDelayError(@NonNull Function<? super T, ? extends ObservableSource<? extends R>> mapper,
            boolean tillTheEnd, int bufferSize, @NonNull Scheduler scheduler) {
        Objects.requireNonNull(mapper, "mapper is null");
        ObjectHelper.verifyPositive(bufferSize, "bufferSize");
        Objects.requireNonNull(scheduler, "scheduler is null");
        return RxJavaPlugins.onAssembly(new ObservableConcatMapScheduler<>(this, mapper, bufferSize, tillTheEnd ? ErrorMode.END : ErrorMode.BOUNDARY, scheduler));
    }

    /**
     * Maps a sequence of values into {@link ObservableSource}s and concatenates these {@code ObservableSource}s eagerly into a single
     * {@code Observable} sequence.
     * <p>
     * Eager concatenation means that once a subscriber subscribes, this operator subscribes to all of the
     * current {@code Observable}s. The operator buffers the values emitted by these {@code ObservableSource}s and then drains them in
     * order, each one after the previous one completes.
     * <p>
     * <img width="640" height="361" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/concatMapEager.o.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This method does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <R> the value type
     * @param mapper the function that maps a sequence of values into a sequence of {@code ObservableSource}s that will be
     *               eagerly concatenated
     * @return the new {@code Observable} instance with the specified concatenation behavior
     * @throws NullPointerException if {@code mapper} is {@code null}
     * @since 2.0
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <@NonNull R> Observable<R> concatMapEager(@NonNull Function<? super T, ? extends ObservableSource<? extends R>> mapper) {
        return concatMapEager(mapper, Integer.MAX_VALUE, bufferSize());
    }

    /**
     * Maps a sequence of values into {@link ObservableSource}s and concatenates these {@code ObservableSource}s eagerly into a single
     * {@code Observable} sequence.
     * <p>
     * Eager concatenation means that once a subscriber subscribes, this operator subscribes to all of the
     * current {@code Observable}s. The operator buffers the values emitted by these {@code ObservableSource}s and then drains them in
     * order, each one after the previous one completes.
     * <p>
     * <img width="640" height="361" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/concatMapEager.o.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This method does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <R> the value type
     * @param mapper the function that maps a sequence of values into a sequence of {@code ObservableSource}s that will be
     *               eagerly concatenated
     * @param maxConcurrency the maximum number of concurrent subscribed {@code ObservableSource}s
     * @param bufferSize hints about the number of expected items from each inner {@code ObservableSource}, must be positive
     * @return the new {@code Observable} instance with the specified concatenation behavior
     * @throws NullPointerException if {@code mapper} is {@code null}
     * @throws IllegalArgumentException if {@code maxConcurrency} or {@code bufferSize} is non-positive
     * @since 2.0
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <@NonNull R> Observable<R> concatMapEager(@NonNull Function<? super T, ? extends ObservableSource<? extends R>> mapper,
            int maxConcurrency, int bufferSize) {
        Objects.requireNonNull(mapper, "mapper is null");
        ObjectHelper.verifyPositive(maxConcurrency, "maxConcurrency");
        ObjectHelper.verifyPositive(bufferSize, "bufferSize");
        return RxJavaPlugins.onAssembly(new ObservableConcatMapEager<>(this, mapper, ErrorMode.IMMEDIATE, maxConcurrency, bufferSize));
    }

    /**
     * Maps a sequence of values into {@link ObservableSource}s and concatenates these {@code ObservableSource}s eagerly into a single
     * {@code Observable} sequence.
     * <p>
     * Eager concatenation means that once a subscriber subscribes, this operator subscribes to all of the
     * current {@code Observable}s. The operator buffers the values emitted by these {@code ObservableSource}s and then drains them in
     * order, each one after the previous one completes.
     * <p>
     * <img width="640" height="390" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/concatMapEagerDelayError.o.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This method does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <R> the value type
     * @param mapper the function that maps a sequence of values into a sequence of {@code ObservableSource}s that will be
     *               eagerly concatenated
     * @param tillTheEnd
     *            if {@code true}, all errors from the outer and inner {@code ObservableSource} sources are delayed until the end,
     *            if {@code false}, an error from the main source is signaled when the current {@code Observable} source terminates
     * @return the new {@code Observable} instance with the specified concatenation behavior
     * @throws NullPointerException if {@code mapper} is {@code null}
     * @since 2.0
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <@NonNull R> Observable<R> concatMapEagerDelayError(@NonNull Function<? super T, ? extends ObservableSource<? extends R>> mapper,
            boolean tillTheEnd) {
        return concatMapEagerDelayError(mapper, tillTheEnd, Integer.MAX_VALUE, bufferSize());
    }

    /**
     * Maps a sequence of values into {@link ObservableSource}s and concatenates these {@code ObservableSource}s eagerly into a single
     * {@code Observable} sequence.
     * <p>
     * Eager concatenation means that once a subscriber subscribes, this operator subscribes to all of the
     * current {@code Observable}s. The operator buffers the values emitted by these {@code ObservableSource}s and then drains them in
     * order, each one after the previous one completes.
     * <p>
     * <img width="640" height="390" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/concatMapEagerDelayError.o.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This method does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <R> the value type
     * @param mapper the function that maps a sequence of values into a sequence of {@code ObservableSource}s that will be
     *               eagerly concatenated
     * @param tillTheEnd
     *               if {@code true}, exceptions from the current {@code Observable} and all the inner {@code ObservableSource}s are delayed until
     *               all of them terminate, if {@code false}, exception from the current {@code Observable} is delayed until the
     *               currently running {@code ObservableSource} terminates
     * @param maxConcurrency the maximum number of concurrent subscribed {@code ObservableSource}s
     * @param bufferSize
     *               the number of elements expected from the current {@code Observable} and each inner {@code ObservableSource} to be buffered
     * @return the new {@code Observable} instance with the specified concatenation behavior
     * @throws NullPointerException if {@code mapper} is {@code null}
     * @throws IllegalArgumentException if {@code maxConcurrency} or {@code bufferSize} is non-positive
     * @since 2.0
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <@NonNull R> Observable<R> concatMapEagerDelayError(@NonNull Function<? super T, ? extends ObservableSource<? extends R>> mapper,
            boolean tillTheEnd, int maxConcurrency, int bufferSize) {
        Objects.requireNonNull(mapper, "mapper is null");
        ObjectHelper.verifyPositive(maxConcurrency, "maxConcurrency");
        ObjectHelper.verifyPositive(bufferSize, "bufferSize");
        return RxJavaPlugins.onAssembly(new ObservableConcatMapEager<>(this, mapper, tillTheEnd ? ErrorMode.END : ErrorMode.BOUNDARY, maxConcurrency, bufferSize));
    }

    /**
     * Maps each element of the current {@code Observable} into {@link CompletableSource}s, subscribes to them one at a time in
     * order and waits until the upstream and all {@code CompletableSource}s complete.
     * <p>
     * <img width="640" height="506" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/concatMapCompletable.o.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code concatMapCompletable} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * <p>History: 2.1.6 - experimental
     * @param mapper
     *            a function that, when applied to an item emitted by the current {@code Observable}, returns a {@code CompletableSource}
     * @return the new {@link Completable} instance
     * @throws NullPointerException if {@code mapper} is {@code null}
     * @since 2.2
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Completable concatMapCompletable(@NonNull Function<? super T, ? extends CompletableSource> mapper) {
        return concatMapCompletable(mapper, 2);
    }

    /**
     * Maps each element of the current {@code Observable} into {@link CompletableSource}s, subscribes to them one at a time in
     * order and waits until the upstream and all {@code CompletableSource}s complete.
     * <p>
     * <img width="640" height="506" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/concatMapCompletable.o.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code concatMapCompletable} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * <p>History: 2.1.6 - experimental
     * @param mapper
     *            a function that, when applied to an item emitted by the current {@code Observable}, returns a {@code CompletableSource}
     *
     * @param capacityHint
     *            the number of upstream items expected to be buffered until the current {@code CompletableSource}, mapped from
     *            the current item, completes.
     * @return the new {@link Completable} instance
     * @throws NullPointerException if {@code mapper} is {@code null}
     * @throws IllegalArgumentException if {@code capacityHint} is non-positive
     * @since 2.2
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Completable concatMapCompletable(@NonNull Function<? super T, ? extends CompletableSource> mapper, int capacityHint) {
        Objects.requireNonNull(mapper, "mapper is null");
        ObjectHelper.verifyPositive(capacityHint, "capacityHint");
        return RxJavaPlugins.onAssembly(new ObservableConcatMapCompletable<>(this, mapper, ErrorMode.IMMEDIATE, capacityHint));
    }

    /**
     * Maps the upstream items into {@link CompletableSource}s and subscribes to them one after the
     * other terminates, delaying all errors till both the current {@code Observable} and all
     * inner {@code CompletableSource}s terminate.
     * <p>
     * <img width="640" height="305" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/concatMap.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code concatMapCompletableDelayError} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * <p>History: 2.1.11 - experimental
     * @param mapper the function called with the upstream item and should return
     *               a {@code CompletableSource} to become the next source to
     *               be subscribed to
     * @return the new {@link Completable} instance
     * @throws NullPointerException if {@code mapper} is {@code null}
     * @see #concatMapCompletable(Function, int)
     * @since 2.2
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Completable concatMapCompletableDelayError(@NonNull Function<? super T, ? extends CompletableSource> mapper) {
        return concatMapCompletableDelayError(mapper, true, 2);
    }

    /**
     * Maps the upstream items into {@link CompletableSource}s and subscribes to them one after the
     * other terminates, optionally delaying all errors till both the current {@code Observable} and all
     * inner {@code CompletableSource}s terminate.
     * <p>
     * <img width="640" height="305" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/concatMap.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code concatMapCompletableDelayError} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * <p>History: 2.1.11 - experimental
     * @param mapper the function called with the upstream item and should return
     *               a {@code CompletableSource} to become the next source to
     *               be subscribed to
     * @param tillTheEnd If {@code true}, errors from the current {@code Observable} or any of the
     *                   inner {@code CompletableSource}s are delayed until all
     *                   of them terminate. If {@code false}, an error from the current
     *                   {@code Observable} is delayed until the current inner
     *                   {@code CompletableSource} terminates and only then is
     *                   it emitted to the downstream.
     * @return the new {@link Completable} instance
     * @throws NullPointerException if {@code mapper} is {@code null}
     * @see #concatMapCompletable(Function)
     * @since 2.2
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Completable concatMapCompletableDelayError(@NonNull Function<? super T, ? extends CompletableSource> mapper, boolean tillTheEnd) {
        return concatMapCompletableDelayError(mapper, tillTheEnd, 2);
    }

    /**
     * Maps the upstream items into {@link CompletableSource}s and subscribes to them one after the
     * other terminates, optionally delaying all errors till both the current {@code Observable} and all
     * inner {@code CompletableSource}s terminate.
     * <p>
     * <img width="640" height="305" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/concatMap.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code concatMapCompletableDelayError} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * <p>History: 2.1.11 - experimental
     * @param mapper the function called with the upstream item and should return
     *               a {@code CompletableSource} to become the next source to
     *               be subscribed to
     * @param tillTheEnd If {@code true}, errors from the current {@code Observable} or any of the
     *                   inner {@code CompletableSource}s are delayed until all
     *                   of them terminate. If {@code false}, an error from the current
     *                   {@code Observable} is delayed until the current inner
     *                   {@code CompletableSource} terminates and only then is
     *                   it emitted to the downstream.
     * @param bufferSize The number of upstream items expected to be buffered so that fresh items are
     *                 ready to be mapped when a previous {@code CompletableSource} terminates.
     * @return the new {@link Completable} instance
     * @throws NullPointerException if {@code mapper} is {@code null}
     * @throws IllegalArgumentException if {@code bufferSize} is non-positive
     * @see #concatMapCompletable(Function, int)
     * @since 2.2
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Completable concatMapCompletableDelayError(@NonNull Function<? super T, ? extends CompletableSource> mapper, boolean tillTheEnd, int bufferSize) {
        Objects.requireNonNull(mapper, "mapper is null");
        ObjectHelper.verifyPositive(bufferSize, "bufferSize");
        return RxJavaPlugins.onAssembly(new ObservableConcatMapCompletable<>(this, mapper, tillTheEnd ? ErrorMode.END : ErrorMode.BOUNDARY, bufferSize));
    }

    /**
     * Returns an {@code Observable} that concatenate each item emitted by the current {@code Observable} with the values in an
     * {@link Iterable} corresponding to that item that is generated by a selector.
     * <p>
     * <img width="640" height="275" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/concatMapIterable.o.png" alt="">
     *
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code concatMapIterable} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <U>
     *            the type of item emitted by the resulting {@code Observable}
     * @param mapper
     *            a function that returns an {@code Iterable} sequence of values for when given an item emitted by the
     *            current {@code Observable}
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code mapper} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/flatmap.html">ReactiveX operators documentation: FlatMap</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <@NonNull U> Observable<U> concatMapIterable(@NonNull Function<? super T, @NonNull ? extends Iterable<? extends U>> mapper) {
        Objects.requireNonNull(mapper, "mapper is null");
        return RxJavaPlugins.onAssembly(new ObservableFlattenIterable<>(this, mapper));
    }

    /**
     * Maps the upstream items into {@link MaybeSource}s and subscribes to them one after the
     * other succeeds or completes, emits their success value if available or terminates immediately if
     * either the current {@code Observable} or the current inner {@code MaybeSource} fail.
     * <p>
     * <img width="640" height="305" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/concatMap.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code concatMapMaybe} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * <p>History: 2.1.11 - experimental
     * @param <R> the result type of the inner {@code MaybeSource}s
     * @param mapper the function called with the upstream item and should return
     *               a {@code MaybeSource} to become the next source to
     *               be subscribed to
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code mapper} is {@code null}
     * @see #concatMapMaybeDelayError(Function)
     * @see #concatMapMaybe(Function, int)
     * @since 2.2
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <@NonNull R> Observable<R> concatMapMaybe(@NonNull Function<? super T, ? extends MaybeSource<? extends R>> mapper) {
        return concatMapMaybe(mapper, 2);
    }

    /**
     * Maps the upstream items into {@link MaybeSource}s and subscribes to them one after the
     * other succeeds or completes, emits their success value if available or terminates immediately if
     * either the current {@code Observable} or the current inner {@code MaybeSource} fail.
     * <p>
     * <img width="640" height="305" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/concatMap.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code concatMapMaybe} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * <p>History: 2.1.11 - experimental
     * @param <R> the result type of the inner {@code MaybeSource}s
     * @param mapper the function called with the upstream item and should return
     *               a {@code MaybeSource} to become the next source to
     *               be subscribed to
     * @param bufferSize The number of upstream items expected to be buffered so that fresh items are
     *                 ready to be mapped when a previous {@code MaybeSource} terminates.
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code mapper} is {@code null}
     * @throws IllegalArgumentException if {@code bufferSize} is non-positive
     * @see #concatMapMaybe(Function)
     * @see #concatMapMaybeDelayError(Function, boolean, int)
     * @since 2.2
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <@NonNull R> Observable<R> concatMapMaybe(@NonNull Function<? super T, ? extends MaybeSource<? extends R>> mapper, int bufferSize) {
        Objects.requireNonNull(mapper, "mapper is null");
        ObjectHelper.verifyPositive(bufferSize, "bufferSize");
        return RxJavaPlugins.onAssembly(new ObservableConcatMapMaybe<>(this, mapper, ErrorMode.IMMEDIATE, bufferSize));
    }

    /**
     * Maps the upstream items into {@link MaybeSource}s and subscribes to them one after the
     * other terminates, emits their success value if available and delaying all errors
     * till both the current {@code Observable} and all inner {@code MaybeSource}s terminate.
     * <p>
     * <img width="640" height="305" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/concatMap.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code concatMapMaybeDelayError} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * <p>History: 2.1.11 - experimental
     * @param <R> the result type of the inner {@code MaybeSource}s
     * @param mapper the function called with the upstream item and should return
     *               a {@code MaybeSource} to become the next source to
     *               be subscribed to
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code mapper} is {@code null}
     * @see #concatMapMaybe(Function)
     * @see #concatMapMaybeDelayError(Function, boolean)
     * @since 2.2
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <@NonNull R> Observable<R> concatMapMaybeDelayError(@NonNull Function<? super T, ? extends MaybeSource<? extends R>> mapper) {
        return concatMapMaybeDelayError(mapper, true, 2);
    }

    /**
     * Maps the upstream items into {@link MaybeSource}s and subscribes to them one after the
     * other terminates, emits their success value if available and optionally delaying all errors
     * till both the current {@code Observable} and all inner {@code MaybeSource}s terminate.
     * <p>
     * <img width="640" height="305" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/concatMap.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code concatMapMaybeDelayError} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * <p>History: 2.1.11 - experimental
     * @param <R> the result type of the inner {@code MaybeSource}s
     * @param mapper the function called with the upstream item and should return
     *               a {@code MaybeSource} to become the next source to
     *               be subscribed to
     * @param tillTheEnd If {@code true}, errors from the current {@code Observable} or any of the
     *                   inner {@code MaybeSource}s are delayed until all
     *                   of them terminate. If {@code false}, an error from the current
     *                   {@code Observable} is delayed until the current inner
     *                   {@code MaybeSource} terminates and only then is
     *                   it emitted to the downstream.
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code mapper} is {@code null}
     * @see #concatMapMaybe(Function, int)
     * @see #concatMapMaybeDelayError(Function, boolean, int)
     * @since 2.2
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <@NonNull R> Observable<R> concatMapMaybeDelayError(@NonNull Function<? super T, ? extends MaybeSource<? extends R>> mapper, boolean tillTheEnd) {
        return concatMapMaybeDelayError(mapper, tillTheEnd, 2);
    }

    /**
     * Maps the upstream items into {@link MaybeSource}s and subscribes to them one after the
     * other terminates, emits their success value if available and optionally delaying all errors
     * till both the current {@code Observable} and all inner {@code MaybeSource}s terminate.
     * <p>
     * <img width="640" height="305" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/concatMap.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code concatMapMaybeDelayError} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * <p>History: 2.1.11 - experimental
     * @param <R> the result type of the inner {@code MaybeSource}s
     * @param mapper the function called with the upstream item and should return
     *               a {@code MaybeSource} to become the next source to
     *               be subscribed to
     * @param tillTheEnd If {@code true}, errors from the current {@code Observable} or any of the
     *                   inner {@code MaybeSource}s are delayed until all
     *                   of them terminate. If {@code false}, an error from the current
     *                   {@code Observable} is delayed until the current inner
     *                   {@code MaybeSource} terminates and only then is
     *                   it emitted to the downstream.
     * @param bufferSize The number of upstream items expected to be buffered so that fresh items are
     *                 ready to be mapped when a previous {@code MaybeSource} terminates.
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code mapper} is {@code null}
     * @throws IllegalArgumentException if {@code bufferSize} is non-positive
     * @see #concatMapMaybe(Function, int)
     * @since 2.2
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <@NonNull R> Observable<R> concatMapMaybeDelayError(@NonNull Function<? super T, ? extends MaybeSource<? extends R>> mapper, boolean tillTheEnd, int bufferSize) {
        Objects.requireNonNull(mapper, "mapper is null");
        ObjectHelper.verifyPositive(bufferSize, "bufferSize");
        return RxJavaPlugins.onAssembly(new ObservableConcatMapMaybe<>(this, mapper, tillTheEnd ? ErrorMode.END : ErrorMode.BOUNDARY, bufferSize));
    }

    /**
     * Maps the upstream items into {@link SingleSource}s and subscribes to them one after the
     * other succeeds, emits their success values or terminates immediately if
     * either the current {@code Observable} or the current inner {@code SingleSource} fail.
     * <p>
     * <img width="640" height="305" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/concatMap.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code concatMapSingle} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * <p>History: 2.1.11 - experimental
     * @param <R> the result type of the inner {@code SingleSource}s
     * @param mapper the function called with the upstream item and should return
     *               a {@code SingleSource} to become the next source to
     *               be subscribed to
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code mapper} is {@code null}
     * @see #concatMapSingleDelayError(Function)
     * @see #concatMapSingle(Function, int)
     * @since 2.2
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <@NonNull R> Observable<R> concatMapSingle(@NonNull Function<? super T, ? extends SingleSource<? extends R>> mapper) {
        return concatMapSingle(mapper, 2);
    }

    /**
     * Maps the upstream items into {@link SingleSource}s and subscribes to them one after the
     * other succeeds, emits their success values or terminates immediately if
     * either the current {@code Observable} or the current inner {@code SingleSource} fail.
     * <p>
     * <img width="640" height="305" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/concatMap.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code concatMapSingle} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * <p>History: 2.1.11 - experimental
     * @param <R> the result type of the inner {@code SingleSource}s
     * @param mapper the function called with the upstream item and should return
     *               a {@code SingleSource} to become the next source to
     *               be subscribed to
     * @param bufferSize The number of upstream items expected to be buffered so that fresh items are
     *                 ready to be mapped when a previous {@code SingleSource} terminates.
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code mapper} is {@code null}
     * @throws IllegalArgumentException if {@code bufferSize} is non-positive
     * @see #concatMapSingle(Function)
     * @see #concatMapSingleDelayError(Function, boolean, int)
     * @since 2.2
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <@NonNull R> Observable<R> concatMapSingle(@NonNull Function<? super T, ? extends SingleSource<? extends R>> mapper, int bufferSize) {
        Objects.requireNonNull(mapper, "mapper is null");
        ObjectHelper.verifyPositive(bufferSize, "bufferSize");
        return RxJavaPlugins.onAssembly(new ObservableConcatMapSingle<>(this, mapper, ErrorMode.IMMEDIATE, bufferSize));
    }

    /**
     * Maps the upstream items into {@link SingleSource}s and subscribes to them one after the
     * other succeeds or fails, emits their success values and delays all errors
     * till both the current {@code Observable} and all inner {@code SingleSource}s terminate.
     * <p>
     * <img width="640" height="305" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/concatMap.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code concatMapSingleDelayError} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * <p>History: 2.1.11 - experimental
     * @param <R> the result type of the inner {@code SingleSource}s
     * @param mapper the function called with the upstream item and should return
     *               a {@code SingleSource} to become the next source to
     *               be subscribed to
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code mapper} is {@code null}
     * @see #concatMapSingle(Function)
     * @see #concatMapSingleDelayError(Function, boolean)
     * @since 2.2
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <@NonNull R> Observable<R> concatMapSingleDelayError(@NonNull Function<? super T, ? extends SingleSource<? extends R>> mapper) {
        return concatMapSingleDelayError(mapper, true, 2);
    }

    /**
     * Maps the upstream items into {@link SingleSource}s and subscribes to them one after the
     * other succeeds or fails, emits their success values and optionally delays all errors
     * till both the current {@code Observable} and all inner {@code SingleSource}s terminate.
     * <p>
     * <img width="640" height="305" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/concatMap.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code concatMapSingleDelayError} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * <p>History: 2.1.11 - experimental
     * @param <R> the result type of the inner {@code SingleSource}s
     * @param mapper the function called with the upstream item and should return
     *               a {@code SingleSource} to become the next source to
     *               be subscribed to
     * @param tillTheEnd If {@code true}, errors from the current {@code Observable} or any of the
     *                   inner {@code SingleSource}s are delayed until all
     *                   of them terminate. If {@code false}, an error from the current
     *                   {@code Observable} is delayed until the current inner
     *                   {@code SingleSource} terminates and only then is
     *                   it emitted to the downstream.
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code mapper} is {@code null}
     * @see #concatMapSingle(Function, int)
     * @see #concatMapSingleDelayError(Function, boolean, int)
     * @since 2.2
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <@NonNull R> Observable<R> concatMapSingleDelayError(@NonNull Function<? super T, ? extends SingleSource<? extends R>> mapper, boolean tillTheEnd) {
        return concatMapSingleDelayError(mapper, tillTheEnd, 2);
    }

    /**
     * Maps the upstream items into {@link SingleSource}s and subscribes to them one after the
     * other succeeds or fails, emits their success values and optionally delays  errors
     * till both the current {@code Observable} and all inner {@code SingleSource}s terminate.
     * <p>
     * <img width="640" height="305" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/concatMap.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code concatMapSingleDelayError} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * <p>History: 2.1.11 - experimental
     * @param <R> the result type of the inner {@code SingleSource}s
     * @param mapper the function called with the upstream item and should return
     *               a {@code SingleSource} to become the next source to
     *               be subscribed to
     * @param tillTheEnd If {@code true}, errors from the current {@code Observable} or any of the
     *                   inner {@code SingleSource}s are delayed until all
     *                   of them terminate. If {@code false}, an error from the current
     *                   {@code Observable} is delayed until the current inner
     *                   {@code SingleSource} terminates and only then is
     *                   it emitted to the downstream.
     * @param bufferSize The number of upstream items expected to be buffered so that fresh items are
     *                 ready to be mapped when a previous {@code SingleSource} terminates.
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code mapper} is {@code null}
     * @throws IllegalArgumentException if {@code bufferSize} is non-positive
     * @see #concatMapSingle(Function, int)
     * @since 2.2
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <@NonNull R> Observable<R> concatMapSingleDelayError(@NonNull Function<? super T, ? extends SingleSource<? extends R>> mapper, boolean tillTheEnd, int bufferSize) {
        Objects.requireNonNull(mapper, "mapper is null");
        ObjectHelper.verifyPositive(bufferSize, "bufferSize");
        return RxJavaPlugins.onAssembly(new ObservableConcatMapSingle<>(this, mapper, tillTheEnd ? ErrorMode.END : ErrorMode.BOUNDARY, bufferSize));
    }

    /**
     * Returns an {@code Observable} that first emits the items emitted from the current {@code Observable}, then items
     * from the {@code other} {@link ObservableSource} without interleaving them.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/concat.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code concatWith} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param other
     *            an {@code ObservableSource} to be concatenated after the current
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code other} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/concat.html">ReactiveX operators documentation: Concat</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Observable<T> concatWith(@NonNull ObservableSource<? extends T> other) {
        Objects.requireNonNull(other, "other is null");
        return concat(this, other);
    }

    /**
     * Returns an {@code Observable} that emits the items from the current {@code Observable} followed by the success item or error event
     * of the {@code other} {@link SingleSource}.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/concat.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code concatWith} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * <p>History: 2.1.10 - experimental
     * @param other the {@code SingleSource} whose signal should be emitted after the current {@code Observable} completes normally.
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code other} is {@code null}
     * @since 2.2
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Observable<T> concatWith(@NonNull SingleSource<? extends T> other) {
        Objects.requireNonNull(other, "other is null");
        return RxJavaPlugins.onAssembly(new ObservableConcatWithSingle<>(this, other));
    }

    /**
     * Returns an {@code Observable} that emits the items from the current {@code Observable} followed by the success item or terminal events
     * of the other {@link MaybeSource}.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/concat.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code concatWith} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * <p>History: 2.1.10 - experimental
     * @param other the {@code MaybeSource} whose signal should be emitted after the current {@code Observable} completes normally.
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code other} is {@code null}
     * @since 2.2
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Observable<T> concatWith(@NonNull MaybeSource<? extends T> other) {
        Objects.requireNonNull(other, "other is null");
        return RxJavaPlugins.onAssembly(new ObservableConcatWithMaybe<>(this, other));
    }

    /**
     * Returns an {@code Observable} that emits items from the current {@code Observable} and when it completes normally, the
     * other {@link CompletableSource} is subscribed to and the returned {@code Observable} emits its terminal events.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/concat.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code concatWith} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * <p>History: 2.1.10 - experimental
     * @param other the {@code CompletableSource} to subscribe to once the current {@code Observable} completes normally
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code other} is {@code null}
     * @since 2.2
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Observable<T> concatWith(@NonNull CompletableSource other) {
        Objects.requireNonNull(other, "other is null");
        return RxJavaPlugins.onAssembly(new ObservableConcatWithCompletable<>(this, other));
    }

    /**
     * Returns a {@link Single} that emits a {@link Boolean} that indicates whether the current {@code Observable} emitted a
     * specified item.
     * <p>
     * <img width="640" height="320" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/contains.2.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code contains} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param item
     *            the item to search for in the emissions from the current {@code Observable}
     * @return the new {@code Single} instance
     * @throws NullPointerException if {@code item} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/contains.html">ReactiveX operators documentation: Contains</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Single<Boolean> contains(@NonNull Object item) {
        Objects.requireNonNull(item, "item is null");
        return any(Functions.equalsWith(item));
    }

    /**
     * Returns a {@link Single} that counts the total number of items emitted by the current {@code Observable} and emits
     * this count as a 64-bit {@link Long}.
     * <p>
     * <img width="640" height="310" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/count.2.v3.png" alt="">
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
        return RxJavaPlugins.onAssembly(new ObservableCountSingle<>(this));
    }

    /**
     * Returns an {@code Observable} that mirrors the current {@code Observable}, except that it drops items emitted by the
     * current {@code Observable} that are followed by another item within a computed debounce duration
     * denoted by an item emission or completion from a generated inner {@link ObservableSource} for that original item.
     * <p>
     * <img width="640" height="425" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/debounce.f.v3.png" alt="">
     * <p>
     * The delivery of the item happens on the thread of the first {@code onNext} or {@code onComplete}
     * signal of the generated {@code ObservableSource} sequence,
     * which if takes too long, a newer item may arrive from the upstream, causing the
     * generated sequence to get disposed, which may also interrupt any downstream blocking operation
     * (yielding an {@code InterruptedException}). It is recommended processing items
     * that may take long time to be moved to another thread via {@link #observeOn} applied after
     * {@code debounce} itself.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This version of {@code debounce} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <U>
     *            the debounce value type (ignored)
     * @param debounceIndicator
     *            function to return a sequence that indicates the throttle duration for each item via its own emission or completion
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code debounceIndicator} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/debounce.html">ReactiveX operators documentation: Debounce</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <@NonNull U> Observable<T> debounce(@NonNull Function<? super T, ? extends ObservableSource<U>> debounceIndicator) {
        Objects.requireNonNull(debounceIndicator, "debounceIndicator is null");
        return RxJavaPlugins.onAssembly(new ObservableDebounce<>(this, debounceIndicator));
    }

    /**
     * Returns an {@code Observable} that mirrors the current {@code Observable}, except that it drops items emitted by the
     * current {@code Observable} that are followed by newer items before a timeout value expires. The timer resets on
     * each emission.
     * <p>
     * <em>Note:</em> If items keep being emitted by the current {@code Observable} faster than the timeout then no items
     * will be emitted by the resulting {@code Observable}.
     * <p>
     * <img width="640" height="310" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/debounce.v3.png" alt="">
     * <p>
     * Delivery of the item after the grace period happens on the {@code computation} {@link Scheduler}'s
     * {@code Worker} which if takes too long, a newer item may arrive from the upstream, causing the
     * {@code Worker}'s task to get disposed, which may also interrupt any downstream blocking operation
     * (yielding an {@code InterruptedException}). It is recommended processing items
     * that may take long time to be moved to another thread via {@link #observeOn} applied after
     * {@code debounce} itself.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code debounce} operates by default on the {@code computation} {@code Scheduler}.</dd>
     * </dl>
     *
     * @param timeout
     *            the length of the window of time that must pass after the emission of an item from the current
     *            {@code Observable} in which the {@code Observable} emits no items in order for the item to be emitted by the
     *            resulting {@code Observable}
     * @param unit
     *            the unit of time for the specified {@code timeout}
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code unit} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/debounce.html">ReactiveX operators documentation: Debounce</a>
     * @see #throttleWithTimeout(long, TimeUnit)
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.COMPUTATION)
    @NonNull
    public final Observable<T> debounce(long timeout, @NonNull TimeUnit unit) {
        return debounce(timeout, unit, Schedulers.computation());
    }

    /**
     * Returns an {@code Observable} that mirrors the current {@code Observable}, except that it drops items emitted by the
     * current {@code Observable} that are followed by newer items before a timeout value expires on a specified
     * {@link Scheduler}. The timer resets on each emission.
     * <p>
     * <em>Note:</em> If items keep being emitted by the current {@code Observable} faster than the timeout then no items
     * will be emitted by the resulting {@code Observable}.
     * <p>
     * <img width="640" height="310" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/debounce.s.v3.png" alt="">
     * <p>
     * Delivery of the item after the grace period happens on the given {@code Scheduler}'s
     * {@code Worker} which if takes too long, a newer item may arrive from the upstream, causing the
     * {@code Worker}'s task to get disposed, which may also interrupt any downstream blocking operation
     * (yielding an {@code InterruptedException}). It is recommended processing items
     * that may take long time to be moved to another thread via {@link #observeOn} applied after
     * {@code debounce} itself.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>You specify which {@code Scheduler} this operator will use.</dd>
     * </dl>
     *
     * @param timeout
     *            the time each item has to be "the most recent" of those emitted by the current {@code Observable} to
     *            ensure that it's not dropped
     * @param unit
     *            the unit of time for the specified {@code timeout}
     * @param scheduler
     *            the {@code Scheduler} to use internally to manage the timers that handle the timeout for each
     *            item
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code unit} or {@code scheduler} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/debounce.html">ReactiveX operators documentation: Debounce</a>
     * @see #throttleWithTimeout(long, TimeUnit, Scheduler)
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    @NonNull
    public final Observable<T> debounce(long timeout, @NonNull TimeUnit unit, @NonNull Scheduler scheduler) {
        Objects.requireNonNull(unit, "unit is null");
        Objects.requireNonNull(scheduler, "scheduler is null");
        return RxJavaPlugins.onAssembly(new ObservableDebounceTimed<>(this, timeout, unit, scheduler));
    }

    /**
     * Returns an {@code Observable} that emits the items emitted by the current {@code Observable} or a specified default item
     * if the current {@code Observable} is empty.
     * <p>
     * <img width="640" height="305" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/defaultIfEmpty.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code defaultIfEmpty} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param defaultItem
     *            the item to emit if the current {@code Observable} emits no items
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code defaultItem} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/defaultifempty.html">ReactiveX operators documentation: DefaultIfEmpty</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Observable<T> defaultIfEmpty(@NonNull T defaultItem) {
        Objects.requireNonNull(defaultItem, "defaultItem is null");
        return switchIfEmpty(just(defaultItem));
    }

    /**
     * Returns an {@code Observable} that delays the emissions of the current {@code Observable} via
     * a per-item derived {@link ObservableSource}'s item emission or termination, on a per source item basis.
     * <p>
     * <img width="640" height="450" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/delay.o.v3.png" alt="">
     * <p>
     * <em>Note:</em> the resulting {@code Observable} will immediately propagate any {@code onError} notification
     * from the current {@code Observable}.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This version of {@code delay} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <U>
     *            the item delay value type (ignored)
     * @param itemDelayIndicator
     *            a function that returns an {@code ObservableSource} for each item emitted by the current {@code Observable}, which is
     *            then used to delay the emission of that item by the resulting {@code Observable} until the {@code ObservableSource}
     *            returned from {@code itemDelay} emits an item
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code itemDelayIndicator} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/delay.html">ReactiveX operators documentation: Delay</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <@NonNull U> Observable<T> delay(@NonNull Function<? super T, ? extends ObservableSource<U>> itemDelayIndicator) {
        Objects.requireNonNull(itemDelayIndicator, "itemDelayIndicator is null");
        return flatMap(ObservableInternalHelper.itemDelay(itemDelayIndicator));
    }

    /**
     * Returns an {@code Observable} that emits the items emitted by the current {@code Observable} shifted forward in time by a
     * specified delay. An error notification from the current {@code Observable} is not delayed.
     * <p>
     * <img width="640" height="310" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/delay.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This version of {@code delay} operates by default on the {@code computation} {@link Scheduler}.</dd>
     * </dl>
     *
     * @param time
     *            the delay to shift the source by
     * @param unit
     *            the {@link TimeUnit} in which {@code period} is defined
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code unit} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/delay.html">ReactiveX operators documentation: Delay</a>
     * @see #delay(long, TimeUnit, boolean)
     * @see #delay(long, TimeUnit, Scheduler)
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.COMPUTATION)
    @NonNull
    public final Observable<T> delay(long time, @NonNull TimeUnit unit) {
        return delay(time, unit, Schedulers.computation(), false);
    }

    /**
     * Returns an {@code Observable} that emits the items emitted by the current {@code Observable} shifted forward in time by a
     * specified delay. If {@code delayError} is {@code true}, error notifications will also be delayed.
     * <p>
     * <img width="640" height="310" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/delay.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This version of {@code delay} operates by default on the {@code computation} {@link Scheduler}.</dd>
     * </dl>
     *
     * @param time
     *            the delay to shift the source by
     * @param unit
     *            the {@link TimeUnit} in which {@code period} is defined
     * @param delayError
     *            if {@code true}, the upstream exception is signaled with the given delay, after all preceding normal elements,
     *            if {@code false}, the upstream exception is signaled immediately
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code unit} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/delay.html">ReactiveX operators documentation: Delay</a>
     * @see #delay(long, TimeUnit, Scheduler, boolean)
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.COMPUTATION)
    @NonNull
    public final Observable<T> delay(long time, @NonNull TimeUnit unit, boolean delayError) {
        return delay(time, unit, Schedulers.computation(), delayError);
    }

    /**
     * Returns an {@code Observable} that emits the items emitted by the current {@code Observable} shifted forward in time by a
     * specified delay. An error notification from the current {@code Observable} is not delayed.
     * <p>
     * <img width="640" height="310" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/delay.s.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>You specify which {@link Scheduler} this operator will use.</dd>
     * </dl>
     *
     * @param time
     *            the delay to shift the source by
     * @param unit
     *            the time unit of {@code delay}
     * @param scheduler
     *            the {@code Scheduler} to use for delaying
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code unit} or {@code scheduler} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/delay.html">ReactiveX operators documentation: Delay</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    @NonNull
    public final Observable<T> delay(long time, @NonNull TimeUnit unit, @NonNull Scheduler scheduler) {
        return delay(time, unit, scheduler, false);
    }

    /**
     * Returns an {@code Observable} that emits the items emitted by the current {@code Observable} shifted forward in time by a
     * specified delay. If {@code delayError} is {@code true}, error notifications will also be delayed.
     * <p>
     * <img width="640" height="310" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/delay.s.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>You specify which {@link Scheduler} this operator will use.</dd>
     * </dl>
     *
     * @param time
     *            the delay to shift the source by
     * @param unit
     *            the time unit of {@code delay}
     * @param scheduler
     *            the {@code Scheduler} to use for delaying
     * @param delayError
     *            if {@code true}, the upstream exception is signaled with the given delay, after all preceding normal elements,
     *            if {@code false}, the upstream exception is signaled immediately
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code unit} or {@code scheduler} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/delay.html">ReactiveX operators documentation: Delay</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    @NonNull
    public final Observable<T> delay(long time, @NonNull TimeUnit unit, @NonNull Scheduler scheduler, boolean delayError) {
        Objects.requireNonNull(unit, "unit is null");
        Objects.requireNonNull(scheduler, "scheduler is null");

        return RxJavaPlugins.onAssembly(new ObservableDelay<>(this, time, unit, scheduler, delayError));
    }

    /**
     * Returns an {@code Observable} that delays the subscription to and emissions from the current {@code Observable} via
     * {@link ObservableSource}s for the subscription itself and on a per-item basis.
     * <p>
     * <img width="640" height="450" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/delay.oo.v3.png" alt="">
     * <p>
     * <em>Note:</em> the resulting {@code Observable} will immediately propagate any {@code onError} notification
     * from the current {@code Observable}.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This version of {@code delay} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <U>
     *            the subscription delay value type (ignored)
     * @param <V>
     *            the item delay value type (ignored)
     * @param subscriptionIndicator
     *            a function that returns an {@code ObservableSource} that triggers the subscription to the current {@code Observable}
     *            once it emits any item
     * @param itemDelayIndicator
     *            a function that returns an {@code ObservableSource} for each item emitted by the current {@code Observable}, which is
     *            then used to delay the emission of that item by the resulting {@code Observable} until the {@code ObservableSource}
     *            returned from {@code itemDelay} emits an item
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code subscriptionIndicator} or {@code itemDelayIndicator} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/delay.html">ReactiveX operators documentation: Delay</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <@NonNull U, @NonNull V> Observable<T> delay(@NonNull ObservableSource<U> subscriptionIndicator,
            @NonNull Function<? super T, ? extends ObservableSource<V>> itemDelayIndicator) {
        return delaySubscription(subscriptionIndicator).delay(itemDelayIndicator);
    }

    /**
     * Returns an {@code Observable} that delays the subscription to the current {@code Observable}
     * until the other {@link ObservableSource} emits an element or completes normally.
     * <p>
     * <img width="640" height="310" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/delaySubscription.o.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This method does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <U> the value type of the other {@code Observable}, irrelevant
     * @param subscriptionIndicator the other {@code ObservableSource} that should trigger the subscription
     *        to the current {@code Observable}.
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code subscriptionIndicator} is {@code null}
     * @since 2.0
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <@NonNull U> Observable<T> delaySubscription(@NonNull ObservableSource<U> subscriptionIndicator) {
        Objects.requireNonNull(subscriptionIndicator, "subscriptionIndicator is null");
        return RxJavaPlugins.onAssembly(new ObservableDelaySubscriptionOther<>(this, subscriptionIndicator));
    }

    /**
     * Returns an {@code Observable} that delays the subscription to the current {@code Observable} by a given amount of time.
     * <p>
     * <img width="640" height="310" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/delaySubscription.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This version of {@code delaySubscription} operates by default on the {@code computation} {@link Scheduler}.</dd>
     * </dl>
     *
     * @param time
     *            the time to delay the subscription
     * @param unit
     *            the time unit of {@code delay}
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code unit} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/delay.html">ReactiveX operators documentation: Delay</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.COMPUTATION)
    @NonNull
    public final Observable<T> delaySubscription(long time, @NonNull TimeUnit unit) {
        return delaySubscription(time, unit, Schedulers.computation());
    }

    /**
     * Returns an {@code Observable} that delays the subscription to the current {@code Observable} by a given amount of time,
     * both waiting and subscribing on a given {@link Scheduler}.
     * <p>
     * <img width="640" height="310" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/delaySubscription.s.v3.png" alt="">
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
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code unit} or {@code scheduler} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/delay.html">ReactiveX operators documentation: Delay</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    @NonNull
    public final Observable<T> delaySubscription(long time, @NonNull TimeUnit unit, @NonNull Scheduler scheduler) {
        return delaySubscription(timer(time, unit, scheduler));
    }

    /**
     * Returns an {@code Observable} that reverses the effect of {@link #materialize materialize} by transforming the
     * {@link Notification} objects extracted from the source items via a selector function
     * into their respective {@link Observer} signal types.
     * <p>
     * <img width="640" height="335" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/dematerialize.v3.png" alt="">
     * <p>
     * The intended use of the {@code selector} function is to perform a
     * type-safe identity mapping (see example) on a source that is already of type
     * {@code Notification<T>}. The Java language doesn't allow
     * limiting instance methods to a certain generic argument shape, therefore,
     * a function is used to ensure the conversion remains type safe.
     * <p>
     * When the upstream signals an {@link Notification#createOnError(Throwable) onError} or
     * {@link Notification#createOnComplete() onComplete} item, the
     * returned {@code Observable} disposes of the flow and terminates with that type of terminal event:
     * <pre><code>
     * Observable.just(createOnNext(1), createOnComplete(), createOnNext(2))
     * .doOnDispose(() -&gt; System.out.println("Disposed!"));
     * .dematerialize(notification -&gt; notification)
     * .test()
     * .assertResult(1);
     * </code></pre>
     * If the upstream signals {@code onError} or {@code onComplete} directly, the flow is terminated
     * with the same event.
     * <pre><code>
     * Observable.just(createOnNext(1), createOnNext(2))
     * .dematerialize(notification -&gt; notification)
     * .test()
     * .assertResult(1, 2);
     * </code></pre>
     * If this behavior is not desired, the completion can be suppressed by applying {@link #concatWith(ObservableSource)}
     * with a {@link #never()} source.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code dematerialize} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * <p>History: 2.2.4 - experimental
     *
     * @param <R> the output value type
     * @param selector function that returns the upstream item and should return a {@code Notification} to signal
     * the corresponding {@code Observer} event to the downstream.
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code selector} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/materialize-dematerialize.html">ReactiveX operators documentation: Dematerialize</a>
     * @since 3.0.0
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <@NonNull R> Observable<R> dematerialize(@NonNull Function<? super T, Notification<R>> selector) {
        Objects.requireNonNull(selector, "selector is null");
        return RxJavaPlugins.onAssembly(new ObservableDematerialize<>(this, selector));
    }

    /**
     * Returns an {@code Observable} that emits all items emitted by the current {@code Observable} that are distinct
     * based on {@link Object#equals(Object)} comparison.
     * <p>
     * <img width="640" height="310" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/distinct.v3.png" alt="">
     * <p>
     * It is recommended the elements' class {@code T} in the flow overrides the default {@code Object.equals()}
     * and {@link Object#hashCode()} to provide meaningful comparison between items as the default Java
     * implementation only considers reference equivalence.
     * <p>
     * By default, {@code distinct()} uses an internal {@link HashSet} per {@link Observer} to remember
     * previously seen items and uses {@link java.util.Set#add(Object)} returning {@code false} as the
     * indicator for duplicates.
     * <p>
     * Note that this internal {@code HashSet} may grow unbounded as items won't be removed from it by
     * the operator. Therefore, using very long or infinite upstream (with very distinct elements) may lead
     * to {@link OutOfMemoryError}.
     * <p>
     * Customizing the retention policy can happen only by providing a custom {@link java.util.Collection} implementation
     * to the {@link #distinct(Function, Supplier)} overload.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code distinct} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @return the new {@code Observable} instance
     * @see <a href="http://reactivex.io/documentation/operators/distinct.html">ReactiveX operators documentation: Distinct</a>
     * @see #distinct(Function)
     * @see #distinct(Function, Supplier)
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Observable<T> distinct() {
        return distinct(Functions.identity(), Functions.createHashSet());
    }

    /**
     * Returns an {@code Observable} that emits all items emitted by the current {@code Observable} that are distinct according
     * to a key selector function and based on {@link Object#equals(Object)} comparison of the objects
     * returned by the key selector function.
     * <p>
     * <img width="640" height="310" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/distinct.key.v3.png" alt="">
     * <p>
     * It is recommended the keys' class {@code K} overrides the default {@code Object.equals()}
     * and {@link Object#hashCode()} to provide meaningful comparison between the key objects as the default
     * Java implementation only considers reference equivalence.
     * <p>
     * By default, {@code distinct()} uses an internal {@link HashSet} per {@link Observer} to remember
     * previously seen keys and uses {@link java.util.Set#add(Object)} returning {@code false} as the
     * indicator for duplicates.
     * <p>
     * Note that this internal {@code HashSet} may grow unbounded as keys won't be removed from it by
     * the operator. Therefore, using very long or infinite upstream (with very distinct keys) may lead
     * to {@link OutOfMemoryError}.
     * <p>
     * Customizing the retention policy can happen only by providing a custom {@link java.util.Collection} implementation
     * to the {@link #distinct(Function, Supplier)} overload.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code distinct} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <K> the key type
     * @param keySelector
     *            a function that projects an emitted item to a key value that is used to decide whether an item
     *            is distinct from another one or not
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code keySelector} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/distinct.html">ReactiveX operators documentation: Distinct</a>
     * @see #distinct(Function, Supplier)
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <@NonNull K> Observable<T> distinct(@NonNull Function<? super T, K> keySelector) {
        return distinct(keySelector, Functions.createHashSet());
    }

    /**
     * Returns an {@code Observable} that emits all items emitted by the current {@code Observable} that are distinct according
     * to a key selector function and based on {@link Object#equals(Object)} comparison of the objects
     * returned by the key selector function.
     * <p>
     * <img width="640" height="310" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/distinct.key.v3.png" alt="">
     * <p>
     * It is recommended the keys' class {@code K} overrides the default {@code Object.equals()}
     * and {@link Object#hashCode()}  to provide meaningful comparison between the key objects as
     * the default Java implementation only considers reference equivalence.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code distinct} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <K> the key type
     * @param keySelector
     *            a function that projects an emitted item to a key value that is used to decide whether an item
     *            is distinct from another one or not
     * @param collectionSupplier
     *            function called for each individual {@link Observer} to return a {@link Collection} subtype for holding the extracted
     *            keys and whose {@code add()} method's return indicates uniqueness.
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code keySelector} or {@code collectionSupplier} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/distinct.html">ReactiveX operators documentation: Distinct</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <@NonNull K> Observable<T> distinct(@NonNull Function<? super T, K> keySelector, @NonNull Supplier<? extends Collection<? super K>> collectionSupplier) {
        Objects.requireNonNull(keySelector, "keySelector is null");
        Objects.requireNonNull(collectionSupplier, "collectionSupplier is null");
        return RxJavaPlugins.onAssembly(new ObservableDistinct<>(this, keySelector, collectionSupplier));
    }

    /**
     * Returns an {@code Observable} that emits all items emitted by the current {@code Observable} that are distinct from their
     * immediate predecessors based on {@link Object#equals(Object)} comparison.
     * <p>
     * <img width="640" height="310" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/distinctUntilChanged.v3.png" alt="">
     * <p>
     * It is recommended the elements' class {@code T} in the flow overrides the default {@code Object.equals()} to provide
     * meaningful comparison between items as the default Java implementation only considers reference equivalence.
     * Alternatively, use the {@link #distinctUntilChanged(BiPredicate)} overload and provide a comparison function
     * in case the class {@code T} can't be overridden with custom {@code equals()} or the comparison itself
     * should happen on different terms or properties of the class {@code T}.
     * <p>
     * Note that the operator always retains the latest item from upstream regardless of the comparison result
     * and uses it in the next comparison with the next upstream item.
     * <p>
     * Note that if element type {@code T} in the flow is mutable, the comparison of the previous and current
     * item may yield unexpected results if the items are mutated externally. Common cases are mutable
     * {@link CharSequence}s or {@link List}s where the objects will actually have the same
     * references when they are modified and {@code distinctUntilChanged} will evaluate subsequent items as same.
     * To avoid such situation, it is recommended that mutable data is converted to an immutable one,
     * for example using {@code map(CharSequence::toString)} or {@code map(list -> Collections.unmodifiableList(new ArrayList<>(list)))}.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code distinctUntilChanged} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @return the new {@code Observable} instance
     * @see <a href="http://reactivex.io/documentation/operators/distinct.html">ReactiveX operators documentation: Distinct</a>
     * @see #distinctUntilChanged(BiPredicate)
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Observable<T> distinctUntilChanged() {
        return distinctUntilChanged(Functions.identity());
    }

    /**
     * Returns an {@code Observable} that emits all items emitted by the current {@code Observable} that are distinct from their
     * immediate predecessors, according to a key selector function and based on {@link Object#equals(Object)} comparison
     * of those objects returned by the key selector function.
     * <p>
     * <img width="640" height="310" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/distinctUntilChanged.key.v3.png" alt="">
     * <p>
     * It is recommended the keys' class {@code K} overrides the default {@code Object.equals()} to provide
     * meaningful comparison between the key objects as the default Java implementation only considers reference equivalence.
     * Alternatively, use the {@link #distinctUntilChanged(BiPredicate)} overload and provide a comparison function
     * in case the class {@code K} can't be overridden with custom {@code equals()} or the comparison itself
     * should happen on different terms or properties of the item class {@code T} (for which the keys can be
     * derived via a similar selector).
     * <p>
     * Note that the operator always retains the latest key from upstream regardless of the comparison result
     * and uses it in the next comparison with the next key derived from the next upstream item.
     * <p>
     * Note that if element type {@code T} in the flow is mutable, the comparison of the previous and current
     * item may yield unexpected results if the items are mutated externally. Common cases are mutable
     * {@link CharSequence}s or {@link List}s where the objects will actually have the same
     * references when they are modified and {@code distinctUntilChanged} will evaluate subsequent items as same.
     * To avoid such situation, it is recommended that mutable data is converted to an immutable one,
     * for example using {@code map(CharSequence::toString)} or {@code map(list -> Collections.unmodifiableList(new ArrayList<>(list)))}.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code distinctUntilChanged} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <K> the key type
     * @param keySelector
     *            a function that projects an emitted item to a key value that is used to decide whether an item
     *            is distinct from another one or not
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code keySelector} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/distinct.html">ReactiveX operators documentation: Distinct</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <@NonNull K> Observable<T> distinctUntilChanged(@NonNull Function<? super T, K> keySelector) {
        Objects.requireNonNull(keySelector, "keySelector is null");
        return RxJavaPlugins.onAssembly(new ObservableDistinctUntilChanged<>(this, keySelector, ObjectHelper.equalsPredicate()));
    }

    /**
     * Returns an {@code Observable} that emits all items emitted by the current {@code Observable} that are distinct from their
     * immediate predecessors when compared with each other via the provided comparator function.
     * <p>
     * <img width="640" height="310" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/distinctUntilChanged.v3.png" alt="">
     * <p>
     * Note that the operator always retains the latest item from upstream regardless of the comparison result
     * and uses it in the next comparison with the next upstream item.
     * <p>
     * Note that if element type {@code T} in the flow is mutable, the comparison of the previous and current
     * item may yield unexpected results if the items are mutated externally. Common cases are mutable
     * {@link CharSequence}s or {@link List}s where the objects will actually have the same
     * references when they are modified and {@code distinctUntilChanged} will evaluate subsequent items as same.
     * To avoid such situation, it is recommended that mutable data is converted to an immutable one,
     * for example using {@code map(CharSequence::toString)} or {@code map(list -> Collections.unmodifiableList(new ArrayList<>(list)))}.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code distinctUntilChanged} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param comparer the function that receives the previous item and the current item and is
     *                   expected to return {@code true} if the two are equal, thus skipping the current value.
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code comparer} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/distinct.html">ReactiveX operators documentation: Distinct</a>
     * @since 2.0
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Observable<T> distinctUntilChanged(@NonNull BiPredicate<? super T, ? super T> comparer) {
        Objects.requireNonNull(comparer, "comparer is null");
        return RxJavaPlugins.onAssembly(new ObservableDistinctUntilChanged<>(this, Functions.identity(), comparer));
    }

    /**
     * Calls the specified {@link Consumer} with the current item after this item has been emitted to the downstream.
     * <p>
     * Note that the {@code onAfterNext} action is shared between subscriptions and as such
     * should be thread-safe.
     * <p>
     * <img width="640" height="360" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/doAfterNext.o.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code doAfterNext} does not operate by default on a particular {@link Scheduler}.</dd>
     *  <dt><b>Operator-fusion:</b></dt>
     *  <dd>This operator supports boundary-limited synchronous or asynchronous queue-fusion.</dd>
     * </dl>
     * <p>History: 2.0.1 - experimental
     * @param onAfterNext the {@code Consumer} that will be called after emitting an item from upstream to the downstream
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code onAfterNext} is {@code null}
     * @since 2.1
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Observable<T> doAfterNext(@NonNull Consumer<? super T> onAfterNext) {
        Objects.requireNonNull(onAfterNext, "onAfterNext is null");
        return RxJavaPlugins.onAssembly(new ObservableDoAfterNext<>(this, onAfterNext));
    }

    /**
     * Registers an {@link Action} to be called when the current {@code Observable} invokes either
     * {@link Observer#onComplete onComplete} or {@link Observer#onError onError}.
     * <p>
     * <img width="640" height="310" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/doAfterTerminate.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code doAfterTerminate} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param onAfterTerminate
     *            an {@code Action} to be invoked after the current {@code Observable} finishes
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code onAfterTerminate} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/do.html">ReactiveX operators documentation: Do</a>
     * @see #doOnTerminate(Action)
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Observable<T> doAfterTerminate(@NonNull Action onAfterTerminate) {
        Objects.requireNonNull(onAfterTerminate, "onAfterTerminate is null");
        return doOnEach(Functions.emptyConsumer(), Functions.emptyConsumer(), Functions.EMPTY_ACTION, onAfterTerminate);
    }

    /**
     * Calls the specified action after the current {@code Observable} signals {@code onError} or {@code onCompleted} or gets disposed by
     * the downstream.
     * <p>In case of a race between a terminal event and a dispose call, the provided {@code onFinally} action
     * is executed once per subscription.
     * <p>Note that the {@code onFinally} action is shared between subscriptions and as such
     * should be thread-safe.
     * <p>
     * <img width="640" height="282" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/doFinally.o.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code doFinally} does not operate by default on a particular {@link Scheduler}.</dd>
     *  <dt><b>Operator-fusion:</b></dt>
     *  <dd>This operator supports boundary-limited synchronous or asynchronous queue-fusion.</dd>
     * </dl>
     * <p>History: 2.0.1 - experimental
     * @param onFinally the action called when the current {@code Observable} terminates or gets disposed
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code onFinally} is {@code null}
     * @since 2.1
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Observable<T> doFinally(@NonNull Action onFinally) {
        Objects.requireNonNull(onFinally, "onFinally is null");
        return RxJavaPlugins.onAssembly(new ObservableDoFinally<>(this, onFinally));
    }

    /**
     * Calls the given shared {@link Action} if the downstream disposes the sequence.
     * <p>
     * The action is shared between subscriptions and thus may be called concurrently from multiple
     * threads; the action must be thread safe.
     * <p>
     * If the action throws a runtime exception, that exception is rethrown by the {@code dispose()} call,
     * sometimes as a {@link CompositeException} if there were multiple exceptions along the way.
     * <p>
     * <img width="640" height="310" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/doOnDispose.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code doOnDispose} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param onDispose
     *            the action that gets called when the current {@code Observable}'s {@link Disposable} is disposed
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code onDispose} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/do.html">ReactiveX operators documentation: Do</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Observable<T> doOnDispose(@NonNull Action onDispose) {
        return doOnLifecycle(Functions.emptyConsumer(), onDispose);
    }

    /**
     * Returns an {@code Observable} that invokes an {@link Action} when the current {@code Observable} calls {@code onComplete}.
     * <p>
     * <img width="640" height="358" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/doOnComplete.o.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code doOnComplete} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param onComplete
     *            the action to invoke when the current {@code Observable} calls {@code onComplete}
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code onComplete} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/do.html">ReactiveX operators documentation: Do</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Observable<T> doOnComplete(@NonNull Action onComplete) {
        return doOnEach(Functions.emptyConsumer(), Functions.emptyConsumer(), onComplete, Functions.EMPTY_ACTION);
    }

    /**
     * Calls the appropriate {@code onXXX} consumer (shared between all {@link Observer}s) whenever a signal with the same type
     * passes through, before forwarding them to the downstream.
     * <p>
     * <img width="640" height="310" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/doOnEach.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code doOnEach} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param onNext the {@link Consumer} to invoke when the current {@code Observable} calls {@code onNext}
     * @param onError the {@code Consumer} to invoke when the current {@code Observable} calls {@code onError}
     * @param onComplete the {@link Action} to invoke when the current {@code Observable} calls {@code onComplete}
     * @param onAfterTerminate the {@code Action} to invoke when the current {@code Observable} calls {@code onAfterTerminate}
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code onNext}, {@code onError}, {@code onComplete} or {@code onAfterTerminate} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/do.html">ReactiveX operators documentation: Do</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    private Observable<T> doOnEach(@NonNull Consumer<? super T> onNext, @NonNull Consumer<? super Throwable> onError, @NonNull Action onComplete, @NonNull Action onAfterTerminate) {
        Objects.requireNonNull(onNext, "onNext is null");
        Objects.requireNonNull(onError, "onError is null");
        Objects.requireNonNull(onComplete, "onComplete is null");
        Objects.requireNonNull(onAfterTerminate, "onAfterTerminate is null");
        return RxJavaPlugins.onAssembly(new ObservableDoOnEach<>(this, onNext, onError, onComplete, onAfterTerminate));
    }

    /**
     * Returns an {@code Observable} that invokes a {@link Consumer} with the appropriate {@link Notification}
     * object when the current {@code Observable} signals an item or terminates.
     * <p>
     * <img width="640" height="310" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/doOnEach.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code doOnEach} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param onNotification
     *            the action to invoke for each item emitted by the current {@code Observable}
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code onNotification} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/do.html">ReactiveX operators documentation: Do</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Observable<T> doOnEach(@NonNull Consumer<? super Notification<T>> onNotification) {
        Objects.requireNonNull(onNotification, "onNotification is null");
        return doOnEach(
                Functions.notificationOnNext(onNotification),
                Functions.notificationOnError(onNotification),
                Functions.notificationOnComplete(onNotification),
                Functions.EMPTY_ACTION
            );
    }

    /**
     * Returns an {@code Observable} that forwards the items and terminal events of the current
     * {@code Observable} to its {@link Observer}s and to the given shared {@code Observer} instance.
     * <p>
     * In case the {@code onError} of the supplied observer throws, the downstream will receive a composite
     * exception containing the original exception and the exception thrown by {@code onError}. If either the
     * {@code onNext} or the {@code onComplete} method of the supplied observer throws, the downstream will be
     * terminated and will receive this thrown exception.
     * <p>
     * <img width="640" height="310" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/doOnEach.o.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code doOnEach} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param observer
     *            the observer to be notified about {@code onNext}, {@code onError} and {@code onComplete} events on its
     *            respective methods before the actual downstream {@code Observer} gets notified.
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code observer} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/do.html">ReactiveX operators documentation: Do</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Observable<T> doOnEach(@NonNull Observer<? super T> observer) {
        Objects.requireNonNull(observer, "observer is null");
        return doOnEach(
                ObservableInternalHelper.observerOnNext(observer),
                ObservableInternalHelper.observerOnError(observer),
                ObservableInternalHelper.observerOnComplete(observer),
                Functions.EMPTY_ACTION);
    }

    /**
     * Calls the given {@link Consumer} with the error {@link Throwable} if the current {@code Observable} failed before forwarding it to
     * the downstream.
     * <p>
     * In case the {@code onError} action throws, the downstream will receive a composite exception containing
     * the original exception and the exception thrown by {@code onError}.
     * <p>
     * <img width="640" height="355" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/doOnError.o.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code doOnError} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param onError
     *            the action to invoke if the current {@code Observable} calls {@code onError}
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code onError} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/do.html">ReactiveX operators documentation: Do</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Observable<T> doOnError(@NonNull Consumer<? super Throwable> onError) {
        return doOnEach(Functions.emptyConsumer(), onError, Functions.EMPTY_ACTION, Functions.EMPTY_ACTION);
    }

    /**
     * Calls the appropriate {@code onXXX} method (shared between all {@link Observer}s) for the lifecycle events of
     * the sequence (subscription, disposal).
     * <p>
     * <img width="640" height="310" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/doOnLifecycle.o.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code doOnLifecycle} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param onSubscribe
     *              a {@link Consumer} called with the {@link Disposable} sent via {@link Observer#onSubscribe(Disposable)}
     * @param onDispose
     *              called when the downstream disposes the {@code Disposable} via {@code dispose()}
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code onSubscribe} or {@code onDispose} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/do.html">ReactiveX operators documentation: Do</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Observable<T> doOnLifecycle(@NonNull Consumer<? super Disposable> onSubscribe, @NonNull Action onDispose) {
        Objects.requireNonNull(onSubscribe, "onSubscribe is null");
        Objects.requireNonNull(onDispose, "onDispose is null");
        return RxJavaPlugins.onAssembly(new ObservableDoOnLifecycle<>(this, onSubscribe, onDispose));
    }

    /**
     * Calls the given {@link Consumer} with the value emitted by the current {@code Observable} before forwarding it to the downstream.
     * <p>
     * <img width="640" height="360" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/doOnNext.o.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code doOnNext} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param onNext
     *            the action to invoke when the current {@code Observable} calls {@code onNext}
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code onNext} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/do.html">ReactiveX operators documentation: Do</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Observable<T> doOnNext(@NonNull Consumer<? super T> onNext) {
        return doOnEach(onNext, Functions.emptyConsumer(), Functions.EMPTY_ACTION, Functions.EMPTY_ACTION);
    }

    /**
     * Returns an {@code Observable} so that it invokes the given {@link Consumer} when the current {@code Observable} is subscribed from
     * its {@link Observer}s. Each subscription will result in an invocation of the given action except when the
     * current {@code Observable} is reference counted, in which case the current {@code Observable} will invoke
     * the given action for the first subscription.
     * <p>
     * <img width="640" height="390" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/doOnSubscribe.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code doOnSubscribe} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param onSubscribe
     *            the {@code Consumer} that gets called when an {@code Observer} subscribes to the current {@code Observable}
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code onSubscribe} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/do.html">ReactiveX operators documentation: Do</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Observable<T> doOnSubscribe(@NonNull Consumer<? super Disposable> onSubscribe) {
        return doOnLifecycle(onSubscribe, Functions.EMPTY_ACTION);
    }

    /**
     * Returns an {@code Observable} so that it invokes an action when the current {@code Observable} calls {@code onComplete} or
     * {@code onError}.
     * <p>
     * <img width="640" height="327" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/doOnTerminate.o.png" alt="">
     * <p>
     * This differs from {@code doAfterTerminate} in that this happens <em>before</em> the {@code onComplete} or
     * {@code onError} notification.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code doOnTerminate} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param onTerminate
     *            the action to invoke when the current {@code Observable} calls {@code onComplete} or {@code onError}
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code onTerminate} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/do.html">ReactiveX operators documentation: Do</a>
     * @see #doAfterTerminate(Action)
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Observable<T> doOnTerminate(@NonNull Action onTerminate) {
        Objects.requireNonNull(onTerminate, "onTerminate is null");
        return doOnEach(Functions.emptyConsumer(),
                Functions.actionConsumer(onTerminate), onTerminate,
                Functions.EMPTY_ACTION);
    }

    /**
     * Returns a {@link Maybe} that emits the single item at a specified index in a sequence of emissions from
     * the current {@code Observable} or completes if the current {@code Observable} signals fewer elements than index.
     * <p>
     * <img width="640" height="363" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/elementAt.o.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code elementAt} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param index
     *            the zero-based index of the item to retrieve
     * @return the new {@code Maybe} instance
     * @throws IndexOutOfBoundsException
     *             if {@code index} is negative
     * @see <a href="http://reactivex.io/documentation/operators/elementat.html">ReactiveX operators documentation: ElementAt</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Maybe<T> elementAt(long index) {
        if (index < 0) {
            throw new IndexOutOfBoundsException("index >= 0 required but it was " + index);
        }
        return RxJavaPlugins.onAssembly(new ObservableElementAtMaybe<>(this, index));
    }

    /**
     * Returns a {@link Single} that emits the item found at a specified index in a sequence of emissions from
     * the current {@code Observable}, or a default item if that index is out of range.
     * <p>
     * <img width="640" height="354" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/elementAtDefault.o.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code elementAt} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param index
     *            the zero-based index of the item to retrieve
     * @param defaultItem
     *            the default item
     * @return the new {@code Single} instance
     * @throws NullPointerException if {@code defaultItem} is {@code null}
     * @throws IndexOutOfBoundsException
     *             if {@code index} is negative
     * @see <a href="http://reactivex.io/documentation/operators/elementat.html">ReactiveX operators documentation: ElementAt</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Single<T> elementAt(long index, @NonNull T defaultItem) {
        if (index < 0) {
            throw new IndexOutOfBoundsException("index >= 0 required but it was " + index);
        }
        Objects.requireNonNull(defaultItem, "defaultItem is null");
        return RxJavaPlugins.onAssembly(new ObservableElementAtSingle<>(this, index, defaultItem));
    }

    /**
     * Returns a {@link Single} that emits the item found at a specified index in a sequence of emissions from the current {@code Observable}
     * or signals a {@link NoSuchElementException} if the current {@code Observable} signals fewer elements than index.
     * <p>
     * <img width="640" height="362" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/elementAtOrError.o.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code elementAtOrError} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param index
     *            the zero-based index of the item to retrieve
     * @return the new {@code Single} instance
     * @throws IndexOutOfBoundsException
     *             if {@code index} is negative
     * @see <a href="http://reactivex.io/documentation/operators/elementat.html">ReactiveX operators documentation: ElementAt</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Single<T> elementAtOrError(long index) {
        if (index < 0) {
            throw new IndexOutOfBoundsException("index >= 0 required but it was " + index);
        }
        return RxJavaPlugins.onAssembly(new ObservableElementAtSingle<>(this, index, null));
    }

    /**
     * Filters items emitted by the current {@code Observable} by only emitting those that satisfy a specified {@link Predicate}.
     * <p>
     * <img width="640" height="310" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/filter.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code filter} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param predicate
     *            a function that evaluates each item emitted by the current {@code Observable}, returning {@code true}
     *            if it passes the filter
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code predicate} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/filter.html">ReactiveX operators documentation: Filter</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Observable<T> filter(@NonNull Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        return RxJavaPlugins.onAssembly(new ObservableFilter<>(this, predicate));
    }

    /**
     * Returns a {@link Maybe} that emits only the very first item emitted by the current {@code Observable}, or
     * completes if the current {@code Observable} is empty.
     * <p>
     * <img width="640" height="286" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/firstElement.m.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code firstElement} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @return the new {@code Maybe} instance
     * @see <a href="http://reactivex.io/documentation/operators/first.html">ReactiveX operators documentation: First</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Maybe<T> firstElement() {
        return elementAt(0L);
    }

    /**
     * Returns a {@link Single} that emits only the very first item emitted by the current {@code Observable}, or a default item
     * if the current {@code Observable} completes without emitting any items.
     * <p>
     * <img width="640" height="285" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/first.s.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code first} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param defaultItem
     *            the default item to emit if the current {@code Observable} doesn't emit anything
     * @return the new {@code Single} instance
     * @throws NullPointerException if {@code defaultItem} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/first.html">ReactiveX operators documentation: First</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Single<T> first(@NonNull T defaultItem) {
        return elementAt(0L, defaultItem);
    }

    /**
     * Returns a {@link Single} that emits only the very first item emitted by the current {@code Observable} or
     * signals a {@link NoSuchElementException} if the current {@code Observable} is empty.
     * <p>
     * <img width="640" height="435" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/firstOrError.o.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code firstOrError} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @return the new {@code Single} instance
     * @see <a href="http://reactivex.io/documentation/operators/first.html">ReactiveX operators documentation: First</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Single<T> firstOrError() {
        return elementAtOrError(0L);
    }

    /**
     * Returns an {@code Observable} that emits items based on applying a function that you supply to each item emitted
     * by the current {@code Observable}, where that function returns an {@link ObservableSource}, and then merging those returned
     * {@code ObservableSource}s and emitting the results of this merger.
     * <p>
     * <img width="640" height="310" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/flatMap.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code flatMap} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <R> the value type of the inner {@code ObservableSource}s and the output type
     * @param mapper
     *            a function that, when applied to an item emitted by the current {@code Observable}, returns an
     *            {@code ObservableSource}
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code mapper} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/flatmap.html">ReactiveX operators documentation: FlatMap</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <@NonNull R> Observable<R> flatMap(@NonNull Function<? super T, ? extends ObservableSource<? extends R>> mapper) {
        return flatMap(mapper, false);
    }

    /**
     * Returns an {@code Observable} that emits items based on applying a function that you supply to each item emitted
     * by the current {@code Observable}, where that function returns an {@link ObservableSource}, and then merging those returned
     * {@code ObservableSource}s and emitting the results of this merger.
     * <p>
     * <img width="640" height="356" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/flatMapDelayError.o.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code flatMap} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <R> the value type of the inner {@code ObservableSource}s and the output type
     * @param mapper
     *            a function that, when applied to an item emitted by the current {@code Observable}, returns an
     *            {@code ObservableSource}
     * @param delayErrors
     *            if {@code true}, exceptions from the current {@code Observable} and all inner {@code ObservableSource}s are delayed until all of them terminate
     *            if {@code false}, the first one signaling an exception will terminate the whole sequence immediately
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code mapper} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/flatmap.html">ReactiveX operators documentation: FlatMap</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <@NonNull R> Observable<R> flatMap(@NonNull Function<? super T, ? extends ObservableSource<? extends R>> mapper, boolean delayErrors) {
        return flatMap(mapper, delayErrors, Integer.MAX_VALUE);
    }

    /**
     * Returns an {@code Observable} that emits items based on applying a function that you supply to each item emitted
     * by the current {@code Observable}, where that function returns an {@link ObservableSource}, and then merging those returned
     * {@code ObservableSource}s and emitting the results of this merger, while limiting the maximum number of concurrent
     * subscriptions to these {@code ObservableSource}s.
     * <p>
     * <img width="640" height="442" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/flatMapMaxConcurrency.o.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code flatMap} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <R> the value type of the inner {@code ObservableSource}s and the output type
     * @param mapper
     *            a function that, when applied to an item emitted by the current {@code Observable}, returns an
     *            {@code ObservableSource}
     * @param maxConcurrency
     *         the maximum number of {@code ObservableSource}s that may be subscribed to concurrently
     * @param delayErrors
     *            if {@code true}, exceptions from the current {@code Observable} and all inner {@code ObservableSource}s are delayed until all of them terminate
     *            if {@code false}, the first one signaling an exception will terminate the whole sequence immediately
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code mapper} is {@code null}
     * @throws IllegalArgumentException if {@code maxConcurrency} is non-positive
     * @see <a href="http://reactivex.io/documentation/operators/flatmap.html">ReactiveX operators documentation: FlatMap</a>
     * @since 2.0
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <@NonNull R> Observable<R> flatMap(@NonNull Function<? super T, ? extends ObservableSource<? extends R>> mapper, boolean delayErrors, int maxConcurrency) {
        return flatMap(mapper, delayErrors, maxConcurrency, bufferSize());
    }

    /**
     * Returns an {@code Observable} that emits items based on applying a function that you supply to each item emitted
     * by the current {@code Observable}, where that function returns an {@link ObservableSource}, and then merging those returned
     * {@code ObservableSource}s and emitting the results of this merger, while limiting the maximum number of concurrent
     * subscriptions to these {@code ObservableSource}s.
     * <p>
     * <img width="640" height="442" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/flatMapMaxConcurrency.o.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code flatMap} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <R> the value type of the inner {@code ObservableSource}s and the output type
     * @param mapper
     *            a function that, when applied to an item emitted by the current {@code Observable}, returns an
     *            {@code ObservableSource}
     * @param maxConcurrency
     *         the maximum number of {@code ObservableSource}s that may be subscribed to concurrently
     * @param delayErrors
     *            if {@code true}, exceptions from the current {@code Observable} and all inner {@code ObservableSource}s are delayed until all of them terminate
     *            if {@code false}, the first one signaling an exception will terminate the whole sequence immediately
     * @param bufferSize
     *            the number of elements expected from each inner {@code ObservableSource} to be buffered
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code mapper} is {@code null}
     * @throws IllegalArgumentException if {@code maxConcurrency} or {@code bufferSize} is non-positive
     * @see <a href="http://reactivex.io/documentation/operators/flatmap.html">ReactiveX operators documentation: FlatMap</a>
     * @since 2.0
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <@NonNull R> Observable<R> flatMap(@NonNull Function<? super T, ? extends ObservableSource<? extends R>> mapper,
            boolean delayErrors, int maxConcurrency, int bufferSize) {
        Objects.requireNonNull(mapper, "mapper is null");
        ObjectHelper.verifyPositive(maxConcurrency, "maxConcurrency");
        ObjectHelper.verifyPositive(bufferSize, "bufferSize");
        if (this instanceof ScalarSupplier) {
            @SuppressWarnings("unchecked")
            T v = ((ScalarSupplier<T>)this).get();
            if (v == null) {
                return empty();
            }
            return ObservableScalarXMap.scalarXMap(v, mapper);
        }
        return RxJavaPlugins.onAssembly(new ObservableFlatMap<>(this, mapper, delayErrors, maxConcurrency, bufferSize));
    }

    /**
     * Returns an {@code Observable} that applies a function to each item emitted or notification raised by the current
     * {@code Observable} and then flattens the {@link ObservableSource}s returned from these functions and emits the resulting items.
     * <p>
     * <img width="640" height="410" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/mergeMap.nce.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code flatMap} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <R>
     *            the result type
     * @param onNextMapper
     *            a function that returns an {@code ObservableSource} to merge for each item emitted by the current {@code Observable}
     * @param onErrorMapper
     *            a function that returns an {@code ObservableSource} to merge for an {@code onError} notification from the current
     *            {@code Observable}
     * @param onCompleteSupplier
     *            a function that returns an {@code ObservableSource} to merge for an {@code onComplete} notification from the current
     *            {@code Observable}
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code onNextMapper} or {@code onErrorMapper} or {@code onCompleteSupplier} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/flatmap.html">ReactiveX operators documentation: FlatMap</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <@NonNull R> Observable<R> flatMap(
            @NonNull Function<? super T, ? extends ObservableSource<? extends R>> onNextMapper,
            @NonNull Function<? super Throwable, ? extends ObservableSource<? extends R>> onErrorMapper,
            @NonNull Supplier<? extends ObservableSource<? extends R>> onCompleteSupplier) {
        Objects.requireNonNull(onNextMapper, "onNextMapper is null");
        Objects.requireNonNull(onErrorMapper, "onErrorMapper is null");
        Objects.requireNonNull(onCompleteSupplier, "onCompleteSupplier is null");
        return merge(new ObservableMapNotification<>(this, onNextMapper, onErrorMapper, onCompleteSupplier));
    }

    /**
     * Returns an {@code Observable} that applies a function to each item emitted or notification raised by the current
     * {@code Observable} and then flattens the {@link ObservableSource}s returned from these functions and emits the resulting items,
     * while limiting the maximum number of concurrent subscriptions to these {@code ObservableSource}s.
     * <p>
     * <img width="640" height="410" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/mergeMap.nce.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code flatMap} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <R>
     *            the result type
     * @param onNextMapper
     *            a function that returns an {@code ObservableSource} to merge for each item emitted by the current {@code Observable}
     * @param onErrorMapper
     *            a function that returns an {@code ObservableSource} to merge for an {@code onError} notification from the current
     *            {@code Observable}
     * @param onCompleteSupplier
     *            a function that returns an {@code ObservableSource} to merge for an {@code onComplete} notification from the current
     *            {@code Observable}
     * @param maxConcurrency
     *         the maximum number of {@code ObservableSource}s that may be subscribed to concurrently
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code onNextMapper} or {@code onErrorMapper} or {@code onCompleteSupplier} is {@code null}
     * @throws IllegalArgumentException if {@code maxConcurrency} is non-positive
     * @see <a href="http://reactivex.io/documentation/operators/flatmap.html">ReactiveX operators documentation: FlatMap</a>
     * @since 2.0
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <@NonNull R> Observable<R> flatMap(
            @NonNull Function<? super T, ? extends ObservableSource<? extends R>> onNextMapper,
            @NonNull Function<Throwable, ? extends ObservableSource<? extends R>> onErrorMapper,
            @NonNull Supplier<? extends ObservableSource<? extends R>> onCompleteSupplier,
            int maxConcurrency) {
        Objects.requireNonNull(onNextMapper, "onNextMapper is null");
        Objects.requireNonNull(onErrorMapper, "onErrorMapper is null");
        Objects.requireNonNull(onCompleteSupplier, "onCompleteSupplier is null");
        return merge(new ObservableMapNotification<>(this, onNextMapper, onErrorMapper, onCompleteSupplier), maxConcurrency);
    }

    /**
     * Returns an {@code Observable} that emits items based on applying a function that you supply to each item emitted
     * by the current {@code Observable}, where that function returns an {@link ObservableSource}, and then merging those returned
     * {@code ObservableSource}s and emitting the results of this merger, while limiting the maximum number of concurrent
     * subscriptions to these {@code ObservableSource}s.
     * <p>
     * <img width="640" height="442" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/flatMapMaxConcurrency.o.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code flatMap} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <R> the value type of the inner {@code ObservableSource}s and the output type
     * @param mapper
     *            a function that, when applied to an item emitted by the current {@code Observable}, returns an
     *            {@code ObservableSource}
     * @param maxConcurrency
     *         the maximum number of {@code ObservableSource}s that may be subscribed to concurrently
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code mapper} is {@code null}
     * @throws IllegalArgumentException if {@code maxConcurrency} is non-positive
     * @see <a href="http://reactivex.io/documentation/operators/flatmap.html">ReactiveX operators documentation: FlatMap</a>
     * @since 2.0
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <@NonNull R> Observable<R> flatMap(@NonNull Function<? super T, ? extends ObservableSource<? extends R>> mapper, int maxConcurrency) {
        return flatMap(mapper, false, maxConcurrency, bufferSize());
    }

    /**
     * Returns an {@code Observable} that emits the results of a specified function to the pair of values emitted by the
     * current {@code Observable} and the mapped inner {@link ObservableSource}.
     * <p>
     * <img width="640" height="390" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/mergeMap.r.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code flatMap} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <U>
     *            the type of items emitted by the collection {@code ObservableSource}
     * @param <R>
     *            the type of items emitted by the resulting {@code Observable}
     * @param mapper
     *            a function that returns an {@code ObservableSource} for each item emitted by the current {@code Observable}
     * @param combiner
     *            a function that combines one item emitted by each of the source and collection {@code ObservableSource}s and
     *            returns an item to be emitted by the resulting {@code Observable}
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code mapper} or {@code combiner} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/flatmap.html">ReactiveX operators documentation: FlatMap</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <@NonNull U, @NonNull R> Observable<R> flatMap(@NonNull Function<? super T, ? extends ObservableSource<? extends U>> mapper,
            @NonNull BiFunction<? super T, ? super U, ? extends R> combiner) {
        return flatMap(mapper, combiner, false, bufferSize(), bufferSize());
    }

    /**
     * Returns an {@code Observable} that emits the results of a specified function to the pair of values emitted by the
     * current {@code Observable} and the mapped inner {@link ObservableSource}.
     * <p>
     * <img width="640" height="390" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/mergeMap.r.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code flatMap} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <U>
     *            the type of items emitted by the collection {@code ObservableSource}
     * @param <R>
     *            the type of items emitted by the resulting {@code Observable}
     * @param mapper
     *            a function that returns an {@code ObservableSource} for each item emitted by the current {@code Observable}
     * @param combiner
     *            a function that combines one item emitted by each of the source and collection {@code ObservableSource}s and
     *            returns an item to be emitted by the resulting {@code Observable}
     * @param delayErrors
     *            if {@code true}, exceptions from the current {@code Observable} and all inner {@code ObservableSource}s are delayed until all of them terminate
     *            if {@code false}, the first one signaling an exception will terminate the whole sequence immediately
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code mapper} or {@code combiner} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/flatmap.html">ReactiveX operators documentation: FlatMap</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <@NonNull U, @NonNull R> Observable<R> flatMap(@NonNull Function<? super T, ? extends ObservableSource<? extends U>> mapper,
            @NonNull BiFunction<? super T, ? super U, ? extends R> combiner, boolean delayErrors) {
        return flatMap(mapper, combiner, delayErrors, bufferSize(), bufferSize());
    }

    /**
     * Returns an {@code Observable} that emits the results of a specified function to the pair of values emitted by the
     * current {@code Observable} and the mapped inner {@link ObservableSource}, while limiting the maximum number of concurrent
     * subscriptions to these {@code ObservableSource}s.
     * <p>
     * <img width="640" height="390" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/mergeMap.r.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code flatMap} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <U>
     *            the type of items emitted by the collection {@code ObservableSource}
     * @param <R>
     *            the type of items emitted by the resulting {@code Observable}
     * @param mapper
     *            a function that returns an {@code ObservableSource} for each item emitted by the current {@code Observable}
     * @param combiner
     *            a function that combines one item emitted by each of the source and collection {@code ObservableSource}s and
     *            returns an item to be emitted by the resulting {@code Observable}
     * @param maxConcurrency
     *         the maximum number of {@code ObservableSource}s that may be subscribed to concurrently
     * @param delayErrors
     *            if {@code true}, exceptions from the current {@code Observable} and all inner {@code ObservableSource}s are delayed until all of them terminate
     *            if {@code false}, the first one signaling an exception will terminate the whole sequence immediately
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code mapper} or {@code combiner} is {@code null}
     * @throws IllegalArgumentException if {@code maxConcurrency} is non-positive
     * @see <a href="http://reactivex.io/documentation/operators/flatmap.html">ReactiveX operators documentation: FlatMap</a>
     * @since 2.0
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <@NonNull U, @NonNull R> Observable<R> flatMap(@NonNull Function<? super T, ? extends ObservableSource<? extends U>> mapper,
            @NonNull BiFunction<? super T, ? super U, ? extends R> combiner, boolean delayErrors, int maxConcurrency) {
        return flatMap(mapper, combiner, delayErrors, maxConcurrency, bufferSize());
    }

    /**
     * Returns an {@code Observable} that emits the results of a specified function to the pair of values emitted by the
     * current {@code Observable} and the mapped inner {@link ObservableSource}, while limiting the maximum number of concurrent
     * subscriptions to these {@code ObservableSource}s.
     * <p>
     * <img width="640" height="390" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/mergeMap.r.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code flatMap} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <U>
     *            the type of items emitted by the collection {@code ObservableSource}
     * @param <R>
     *            the type of items emitted by the resulting {@code Observable}
     * @param mapper
     *            a function that returns an {@code ObservableSource} for each item emitted by the current {@code Observable}
     * @param combiner
     *            a function that combines one item emitted by each of the source and collection {@code ObservableSource}s and
     *            returns an item to be emitted by the resulting {@code Observable}
     * @param maxConcurrency
     *         the maximum number of {@code ObservableSource}s that may be subscribed to concurrently
     * @param delayErrors
     *            if {@code true}, exceptions from the current {@code Observable} and all inner {@code ObservableSource}s are delayed until all of them terminate
     *            if {@code false}, the first one signaling an exception will terminate the whole sequence immediately
     * @param bufferSize
     *            the number of elements expected from the inner {@code ObservableSource} to be buffered
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code mapper} or {@code combiner} is {@code null}
     * @throws IllegalArgumentException if {@code maxConcurrency} or {@code bufferSize} is non-positive
     * @see <a href="http://reactivex.io/documentation/operators/flatmap.html">ReactiveX operators documentation: FlatMap</a>
     * @since 2.0
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <@NonNull U, @NonNull R> Observable<R> flatMap(@NonNull Function<? super T, ? extends ObservableSource<? extends U>> mapper,
            @NonNull BiFunction<? super T, ? super U, ? extends R> combiner, boolean delayErrors, int maxConcurrency, int bufferSize) {
        Objects.requireNonNull(mapper, "mapper is null");
        Objects.requireNonNull(combiner, "combiner is null");
        return flatMap(ObservableInternalHelper.flatMapWithCombiner(mapper, combiner), delayErrors, maxConcurrency, bufferSize);
    }

    /**
     * Returns an {@code Observable} that emits the results of a specified function to the pair of values emitted by the
     * current {@code Observable} and the mapped inner {@link ObservableSource}, while limiting the maximum number of concurrent
     * subscriptions to these {@code ObservableSource}s.
     * <p>
     * <img width="640" height="390" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/mergeMap.r.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code flatMap} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <U>
     *            the type of items emitted by the collection {@code ObservableSource}
     * @param <R>
     *            the type of items emitted by the resulting {@code Observable}
     * @param mapper
     *            a function that returns an {@code ObservableSource} for each item emitted by the current {@code Observable}
     * @param combiner
     *            a function that combines one item emitted by each of the source and collection {@code ObservableSource}s and
     *            returns an item to be emitted by the resulting {@code Observable}
     * @param maxConcurrency
     *         the maximum number of {@code ObservableSource}s that may be subscribed to concurrently
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code mapper} or {@code combiner} is {@code null}
     * @throws IllegalArgumentException if {@code maxConcurrency} is non-positive
     * @see <a href="http://reactivex.io/documentation/operators/flatmap.html">ReactiveX operators documentation: FlatMap</a>
     * @since 2.0
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <@NonNull U, @NonNull R> Observable<R> flatMap(@NonNull Function<? super T, ? extends ObservableSource<? extends U>> mapper,
            @NonNull BiFunction<? super T, ? super U, ? extends R> combiner, int maxConcurrency) {
        return flatMap(mapper, combiner, false, maxConcurrency, bufferSize());
    }

    /**
     * Maps each element of the current {@code Observable} into {@link CompletableSource}s, subscribes to them and
     * waits until the upstream and all {@code CompletableSource}s complete.
     * <p>
     * <img width="640" height="424" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/flatMapCompletable.o.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code flatMapCompletable} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param mapper the function that received each source value and transforms them into {@code CompletableSource}s.
     * @throws NullPointerException if {@code mapper} is {@code null}
     * @return the new {@link Completable} instance
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Completable flatMapCompletable(@NonNull Function<? super T, ? extends CompletableSource> mapper) {
        return flatMapCompletable(mapper, false);
    }

    /**
     * Maps each element of the current {@code Observable} into {@link CompletableSource}s, subscribes to them and
     * waits until the upstream and all {@code CompletableSource}s complete, optionally delaying all errors.
     * <p>
     * <img width="640" height="362" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/flatMapCompletableDelayError.o.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code flatMapCompletable} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param mapper the function that received each source value and transforms them into {@code CompletableSource}s.
     * @param delayErrors if {@code true}, errors from the upstream and inner {@code CompletableSource}s are delayed until all of them
     * terminate.
     * @return the new {@link Completable} instance
     * @throws NullPointerException if {@code mapper} is {@code null}
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Completable flatMapCompletable(@NonNull Function<? super T, ? extends CompletableSource> mapper, boolean delayErrors) {
        Objects.requireNonNull(mapper, "mapper is null");
        return RxJavaPlugins.onAssembly(new ObservableFlatMapCompletableCompletable<>(this, mapper, delayErrors));
    }

    /**
     * Merges {@link Iterable}s generated by a mapper {@link Function} for each individual item emitted by
     * the current {@code Observable} into a single {@code Observable} sequence.
     * <p>
     * <img width="640" height="343" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/flatMapIterable.o.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code flatMapIterable} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <U>
     *            the output type and the element type of the {@code Iterable}s
     * @param mapper
     *            a function that returns an {@code Iterable} sequence of values for when given an item emitted by the
     *            current {@code Observable}
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code mapper} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/flatmap.html">ReactiveX operators documentation: FlatMap</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <@NonNull U> Observable<U> flatMapIterable(@NonNull Function<? super T, @NonNull ? extends Iterable<? extends U>> mapper) {
        Objects.requireNonNull(mapper, "mapper is null");
        return RxJavaPlugins.onAssembly(new ObservableFlattenIterable<>(this, mapper));
    }

    /**
     * Merges {@link Iterable}s generated by a mapper {@link Function} for each individual item emitted by
     * the current {@code Observable} into a single {@code Observable} sequence where the resulting items will
     * be the combination of the original item and each inner item of the respective {@code Iterable} as returned
     * by the {@code resultSelector} {@link BiFunction}.
     * <p>
     * <img width="640" height="410" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/flatMapIterable.o.r.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code flatMapIterable} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <U>
     *            the element type of the {@code Iterable}s
     * @param <V>
     *            the output type as determined by the {@code resultSelector} function
     * @param mapper
     *            a function that returns an {@code Iterable} sequence of values for each item emitted by the current
     *            {@code Observable}
     * @param combiner
     *            a function that returns an item based on the item emitted by the current {@code Observable} and the
     *            next item of the {@code Iterable} returned for that original item by the {@code mapper}
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code mapper} or {@code combiner} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/flatmap.html">ReactiveX operators documentation: FlatMap</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <@NonNull U, @NonNull V> Observable<V> flatMapIterable(@NonNull Function<? super T, @NonNull ? extends Iterable<? extends U>> mapper,
            @NonNull BiFunction<? super T, ? super U, ? extends V> combiner) {
        Objects.requireNonNull(mapper, "mapper is null");
        Objects.requireNonNull(combiner, "combiner is null");
        return flatMap(ObservableInternalHelper.flatMapIntoIterable(mapper), combiner, false, bufferSize(), bufferSize());
    }

    /**
     * Maps each element of the current {@code Observable} into {@link MaybeSource}s, subscribes to all of them
     * and merges their {@code onSuccess} values, in no particular order, into a single {@code Observable} sequence.
     * <p>
     * <img width="640" height="310" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/flatMapMaybe.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code flatMapMaybe} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <R> the result value type
     * @param mapper the function that received each source value and transforms them into {@code MaybeSource}s.
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code mapper} is {@code null}
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <@NonNull R> Observable<R> flatMapMaybe(@NonNull Function<? super T, ? extends MaybeSource<? extends R>> mapper) {
        return flatMapMaybe(mapper, false);
    }

    /**
     * Maps each element of the current {@code Observable} into {@link MaybeSource}s, subscribes to them
     * and merges their {@code onSuccess} values, in no particular order, into a single {@code Observable} sequence,
     * optionally delaying all errors.
     * <p>
     * <img width="640" height="310" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/flatMapMaybe.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code flatMapMaybe} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <R> the result value type
     * @param mapper the function that received each source value and transforms them into {@code MaybeSource}s.
     * @param delayErrors if {@code true}, errors from the upstream and inner {@code MaybeSource}s are delayed until all of them
     * terminate.
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code mapper} is {@code null}
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <@NonNull R> Observable<R> flatMapMaybe(@NonNull Function<? super T, ? extends MaybeSource<? extends R>> mapper, boolean delayErrors) {
        Objects.requireNonNull(mapper, "mapper is null");
        return RxJavaPlugins.onAssembly(new ObservableFlatMapMaybe<>(this, mapper, delayErrors));
    }

    /**
     * Maps each element of the current {@code Observable} into {@link SingleSource}s, subscribes to all of them
     * and merges their {@code onSuccess} values, in no particular order, into a single {@code Observable} sequence.
     * <p>
     * <img width="640" height="310" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/flatMapSingle.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code flatMapSingle} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <R> the result value type
     * @param mapper the function that received each source value and transforms them into {@code SingleSource}s.
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code mapper} is {@code null}
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <@NonNull R> Observable<R> flatMapSingle(@NonNull Function<? super T, ? extends SingleSource<? extends R>> mapper) {
        return flatMapSingle(mapper, false);
    }

    /**
     * Maps each element of the current {@code Observable} into {@link SingleSource}s, subscribes to them
     * and merges their {@code onSuccess} values, in no particular order, into a single {@code Observable} sequence,
     * optionally delaying all errors.
     * <p>
     * <img width="640" height="310" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/flatMapSingle.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code flatMapSingle} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <R> the result value type
     * @param mapper the function that received each source value and transforms them into {@code SingleSource}s.
     * @param delayErrors if {@code true}, errors from the upstream and inner {@code SingleSource}s are delayed until each of them
     * terminates.
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code mapper} is {@code null}
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <@NonNull R> Observable<R> flatMapSingle(@NonNull Function<? super T, ? extends SingleSource<? extends R>> mapper, boolean delayErrors) {
        Objects.requireNonNull(mapper, "mapper is null");
        return RxJavaPlugins.onAssembly(new ObservableFlatMapSingle<>(this, mapper, delayErrors));
    }

    /**
     * Subscribes to the {@link ObservableSource} and calls a {@link Consumer} for each item of the current {@code Observable}
     * on its emission thread.
     * <p>
     * <img width="640" height="264" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/forEach.o.png" alt="">
     * <p>
     * Alias to {@link #subscribe(Consumer)}
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code forEach} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param onNext
     *            the {@code Consumer} to execute for each item.
     * @return
     *            a {@link Disposable} that allows disposing the sequence if the current {@code Observable} runs asynchronously
     * @throws NullPointerException
     *             if {@code onNext} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/subscribe.html">ReactiveX operators documentation: Subscribe</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Disposable forEach(@NonNull Consumer<? super T> onNext) {
        return subscribe(onNext);
    }

    /**
     * Subscribes to the {@link ObservableSource} and calls a {@link Predicate} for each item of the current {@code Observable},
     * on its emission thread, until the predicate returns {@code false}.
     * <p>
     * <img width="640" height="273" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/forEachWhile.o.png" alt="">
     * <p>
     * If the {@code Observable} emits an error, it is wrapped into an
     * {@link OnErrorNotImplementedException}
     * and routed to the {@link RxJavaPlugins#onError(Throwable)} handler.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code forEachWhile} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param onNext
     *            the {@code Predicate} to execute for each item.
     * @return
     *            a {@link Disposable} that allows disposing the sequence if the current {@code Observable} runs asynchronously
     * @throws NullPointerException
     *             if {@code onNext} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/subscribe.html">ReactiveX operators documentation: Subscribe</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Disposable forEachWhile(@NonNull Predicate<? super T> onNext) {
        return forEachWhile(onNext, Functions.ON_ERROR_MISSING, Functions.EMPTY_ACTION);
    }

    /**
     * Subscribes to the {@link ObservableSource} and calls a {@link Predicate} for each item or a {@link Consumer} with the error
     * of the current {@code Observable}, on their original emission threads, until the predicate returns {@code false}.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code forEachWhile} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param onNext
     *            the {@code Predicate} to execute for each item.
     * @param onError
     *            the {@code Consumer} to execute when an error is emitted.
     * @return
     *            a {@link Disposable} that allows disposing the sequence if the current {@code Observable} runs asynchronously
     * @throws NullPointerException
     *             if {@code onNext} or {@code onError} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/subscribe.html">ReactiveX operators documentation: Subscribe</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Disposable forEachWhile(@NonNull Predicate<? super T> onNext, @NonNull Consumer<? super Throwable> onError) {
        return forEachWhile(onNext, onError, Functions.EMPTY_ACTION);
    }

    /**
     * Subscribes to the {@link ObservableSource} and calls a {@link Predicate} for each item, a {@link Consumer} with the error
     * or an {@link Action} upon completion of the current {@code Observable}, on their original emission threads,
     * until the predicate returns {@code false}.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code forEachWhile} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param onNext
     *            the {@code Predicate} to execute for each item.
     * @param onError
     *            the {@code Consumer} to execute when an error is emitted.
     * @param onComplete
     *            the {@code Action} to execute when completion is signaled.
     * @return
     *            a {@link Disposable} that allows disposing the sequence if the current {@code Observable} runs asynchronously
     * @throws NullPointerException
     *             if {@code onNext} or {@code onError} or {@code onComplete} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/subscribe.html">ReactiveX operators documentation: Subscribe</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Disposable forEachWhile(@NonNull Predicate<? super T> onNext, @NonNull Consumer<? super Throwable> onError,
            @NonNull Action onComplete) {
        Objects.requireNonNull(onNext, "onNext is null");
        Objects.requireNonNull(onError, "onError is null");
        Objects.requireNonNull(onComplete, "onComplete is null");

        ForEachWhileObserver<T> o = new ForEachWhileObserver<>(onNext, onError, onComplete);
        subscribe(o);
        return o;
    }

    /**
     * Groups the items emitted by the current {@code Observable} according to a specified criterion, and emits these
     * grouped items as {@link GroupedObservable}s.
     * <p>
     * <img width="640" height="360" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/groupBy.v3.png" alt="">
     * <p>
     * Each emitted {@code GroupedObservable} allows only a single {@link Observer} to subscribe to it during its
     * lifetime and if this {@code Observer} calls {@code dispose()} before the
     * source terminates, the next emission by the source having the same key will trigger a new
     * {@code GroupedObservable} emission.
     * <p>
     * <em>Note:</em> A {@code GroupedObservable} will cache the items it is to emit until such time as it
     * is subscribed to. For this reason, in order to avoid memory leaks, you should not simply ignore those
     * {@code GroupedObservable}s that do not concern you. Instead, you can signal to them that they may
     * discard their buffers by applying an operator like {@link #ignoreElements} to them.
     * <p>
     * Note also that ignoring groups or subscribing later (i.e., on another thread) will result in
     * so-called group abandonment where a group will only contain one element and the group will be
     * re-created over and over as new upstream items trigger a new group. The behavior is
     * a trade-off between no-dataloss, upstream cancellation and excessive group creation.
     *
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code groupBy} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param keySelector
     *            a function that extracts the key for each item
     * @param <K>
     *            the key type
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code keySelector} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/groupby.html">ReactiveX operators documentation: GroupBy</a>
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <@NonNull K> Observable<GroupedObservable<K, T>> groupBy(@NonNull Function<? super T, ? extends K> keySelector) {
        return groupBy(keySelector, (Function)Functions.identity(), false, bufferSize());
    }

    /**
     * Groups the items emitted by the current {@code Observable} according to a specified criterion, and emits these
     * grouped items as {@link GroupedObservable}s.
     * <p>
     * <img width="640" height="360" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/groupBy.v3.png" alt="">
     * <p>
     * Each emitted {@code GroupedObservable} allows only a single {@link Observer} to subscribe to it during its
     * lifetime and if this {@code Observer} calls {@code dispose()} before the
     * source terminates, the next emission by the source having the same key will trigger a new
     * {@code GroupedObservable} emission.
     * <p>
     * <em>Note:</em> A {@code GroupedObservable} will cache the items it is to emit until such time as it
     * is subscribed to. For this reason, in order to avoid memory leaks, you should not simply ignore those
     * {@code GroupedObservable}s that do not concern you. Instead, you can signal to them that they may
     * discard their buffers by applying an operator like {@link #ignoreElements} to them.
     * <p>
     * Note also that ignoring groups or subscribing later (i.e., on another thread) will result in
     * so-called group abandonment where a group will only contain one element and the group will be
     * re-created over and over as new upstream items trigger a new group. The behavior is
     * a trade-off between no-dataloss, upstream cancellation and excessive group creation.
     *
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code groupBy} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param keySelector
     *            a function that extracts the key for each item
     * @param <K>
     *            the key type
     * @param delayError
     *            if {@code true}, the exception from the current {@code Observable} is delayed in each group until that specific group emitted
     *            the normal values; if {@code false}, the exception bypasses values in the groups and is reported immediately.
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code keySelector} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/groupby.html">ReactiveX operators documentation: GroupBy</a>
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <@NonNull K> Observable<GroupedObservable<K, T>> groupBy(@NonNull Function<? super T, ? extends K> keySelector, boolean delayError) {
        return groupBy(keySelector, (Function)Functions.identity(), delayError, bufferSize());
    }

    /**
     * Groups the items emitted by the current {@code Observable} according to a specified criterion, and emits these
     * grouped items as {@link GroupedObservable}s.
     * <p>
     * <img width="640" height="360" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/groupBy.v3.png" alt="">
     * <p>
     * Each emitted {@code GroupedObservable} allows only a single {@link Observer} to subscribe to it during its
     * lifetime and if this {@code Observer} calls {@code dispose()} before the
     * source terminates, the next emission by the source having the same key will trigger a new
     * {@code GroupedObservable} emission.
     * <p>
     * <em>Note:</em> A {@code GroupedObservable} will cache the items it is to emit until such time as it
     * is subscribed to. For this reason, in order to avoid memory leaks, you should not simply ignore those
     * {@code GroupedObservable}s that do not concern you. Instead, you can signal to them that they may
     * discard their buffers by applying an operator like {@link #ignoreElements} to them.
     * <p>
     * Note also that ignoring groups or subscribing later (i.e., on another thread) will result in
     * so-called group abandonment where a group will only contain one element and the group will be
     * re-created over and over as new upstream items trigger a new group. The behavior is
     * a trade-off between no-dataloss, upstream cancellation and excessive group creation.
     *
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code groupBy} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param keySelector
     *            a function that extracts the key for each item
     * @param valueSelector
     *            a function that extracts the return element for each item
     * @param <K>
     *            the key type
     * @param <V>
     *            the element type
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code keySelector} or {@code valueSelector} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/groupby.html">ReactiveX operators documentation: GroupBy</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <@NonNull K, @NonNull V> Observable<GroupedObservable<K, V>> groupBy(@NonNull Function<? super T, ? extends K> keySelector,
            Function<? super T, ? extends V> valueSelector) {
        return groupBy(keySelector, valueSelector, false, bufferSize());
    }

    /**
     * Groups the items emitted by the current {@code Observable} according to a specified criterion, and emits these
     * grouped items as {@link GroupedObservable}s.
     * <p>
     * <img width="640" height="360" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/groupBy.v3.png" alt="">
     * <p>
     * Each emitted {@code GroupedObservable} allows only a single {@link Observer} to subscribe to it during its
     * lifetime and if this {@code Observer} calls {@code dispose()} before the
     * source terminates, the next emission by the source having the same key will trigger a new
     * {@code GroupedObservable} emission.
     * <p>
     * <em>Note:</em> A {@code GroupedObservable} will cache the items it is to emit until such time as it
     * is subscribed to. For this reason, in order to avoid memory leaks, you should not simply ignore those
     * {@code GroupedObservable}s that do not concern you. Instead, you can signal to them that they may
     * discard their buffers by applying an operator like {@link #ignoreElements} to them.
     * <p>
     * Note also that ignoring groups or subscribing later (i.e., on another thread) will result in
     * so-called group abandonment where a group will only contain one element and the group will be
     * re-created over and over as new upstream items trigger a new group. The behavior is
     * a trade-off between no-dataloss, upstream cancellation and excessive group creation.
     *
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code groupBy} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param keySelector
     *            a function that extracts the key for each item
     * @param valueSelector
     *            a function that extracts the return element for each item
     * @param <K>
     *            the key type
     * @param <V>
     *            the element type
     * @param delayError
     *            if {@code true}, the exception from the current {@code Observable} is delayed in each group until that specific group emitted
     *            the normal values; if {@code false}, the exception bypasses values in the groups and is reported immediately.
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code keySelector} or {@code valueSelector} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/groupby.html">ReactiveX operators documentation: GroupBy</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <@NonNull K, @NonNull V> Observable<GroupedObservable<K, V>> groupBy(@NonNull Function<? super T, ? extends K> keySelector,
            @NonNull Function<? super T, ? extends V> valueSelector, boolean delayError) {
        return groupBy(keySelector, valueSelector, delayError, bufferSize());
    }

    /**
     * Groups the items emitted by the current {@code Observable} according to a specified criterion, and emits these
     * grouped items as {@link GroupedObservable}s.
     * <p>
     * <img width="640" height="360" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/groupBy.v3.png" alt="">
     * <p>
     * Each emitted {@code GroupedObservable} allows only a single {@link Observer} to subscribe to it during its
     * lifetime and if this {@code Observer} calls {@code dispose()} before the
     * source terminates, the next emission by the source having the same key will trigger a new
     * {@code GroupedObservable} emission.
     * <p>
     * <em>Note:</em> A {@code GroupedObservable} will cache the items it is to emit until such time as it
     * is subscribed to. For this reason, in order to avoid memory leaks, you should not simply ignore those
     * {@code GroupedObservable}s that do not concern you. Instead, you can signal to them that they may
     * discard their buffers by applying an operator like {@link #ignoreElements} to them.
     * <p>
     * Note also that ignoring groups or subscribing later (i.e., on another thread) will result in
     * so-called group abandonment where a group will only contain one element and the group will be
     * re-created over and over as new upstream items trigger a new group. The behavior is
     * a trade-off between no-dataloss, upstream cancellation and excessive group creation.
     *
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code groupBy} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param keySelector
     *            a function that extracts the key for each item
     * @param valueSelector
     *            a function that extracts the return element for each item
     * @param delayError
     *            if {@code true}, the exception from the current {@code Observable} is delayed in each group until that specific group emitted
     *            the normal values; if {@code false}, the exception bypasses values in the groups and is reported immediately.
     * @param bufferSize
     *            the hint for how many {@code GroupedObservable}s and element in each {@code GroupedObservable} should be buffered
     * @param <K>
     *            the key type
     * @param <V>
     *            the element type
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code keySelector} or {@code valueSelector} is {@code null}
     * @throws IllegalArgumentException if {@code bufferSize} is non-positive
     * @see <a href="http://reactivex.io/documentation/operators/groupby.html">ReactiveX operators documentation: GroupBy</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <@NonNull K, @NonNull V> Observable<GroupedObservable<K, V>> groupBy(@NonNull Function<? super T, ? extends K> keySelector,
            @NonNull Function<? super T, ? extends V> valueSelector,
            boolean delayError, int bufferSize) {
        Objects.requireNonNull(keySelector, "keySelector is null");
        Objects.requireNonNull(valueSelector, "valueSelector is null");
        ObjectHelper.verifyPositive(bufferSize, "bufferSize");

        return RxJavaPlugins.onAssembly(new ObservableGroupBy<>(this, keySelector, valueSelector, bufferSize, delayError));
    }

    /**
     * Returns an {@code Observable} that correlates two {@link ObservableSource}s when they overlap in time and groups the results.
     * <p>
     * There are no guarantees in what order the items get combined when multiple
     * items from one or both source {@code ObservableSource}s overlap.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/groupJoin.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code groupJoin} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <TRight> the value type of the right {@code ObservableSource} source
     * @param <TLeftEnd> the element type of the left duration {@code ObservableSource}s
     * @param <TRightEnd> the element type of the right duration {@code ObservableSource}s
     * @param <R> the result type
     * @param other
     *            the other {@code ObservableSource} to correlate items from the current {@code Observable} with
     * @param leftEnd
     *            a function that returns an {@code ObservableSource} whose emissions indicate the duration of the values of
     *            the current {@code Observable}
     * @param rightEnd
     *            a function that returns an {@code ObservableSource} whose emissions indicate the duration of the values of
     *            the {@code right} {@code ObservableSource}
     * @param resultSelector
     *            a function that takes an item emitted by each {@code ObservableSource} and returns the value to be emitted
     *            by the resulting {@code Observable}
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code other}, {@code leftEnd}, {@code rightEnd} or {@code resultSelector} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/join.html">ReactiveX operators documentation: Join</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <@NonNull TRight, @NonNull TLeftEnd, @NonNull TRightEnd, @NonNull R> Observable<R> groupJoin(
            @NonNull ObservableSource<? extends TRight> other,
            @NonNull Function<? super T, ? extends ObservableSource<TLeftEnd>> leftEnd,
            @NonNull Function<? super TRight, ? extends ObservableSource<TRightEnd>> rightEnd,
            @NonNull BiFunction<? super T, ? super Observable<TRight>, ? extends R> resultSelector
    ) {
        Objects.requireNonNull(other, "other is null");
        Objects.requireNonNull(leftEnd, "leftEnd is null");
        Objects.requireNonNull(rightEnd, "rightEnd is null");
        Objects.requireNonNull(resultSelector, "resultSelector is null");
        return RxJavaPlugins.onAssembly(new ObservableGroupJoin<>(
                this, other, leftEnd, rightEnd, resultSelector));
    }

    /**
     * Hides the identity of the current {@code Observable} and its {@link Disposable}.
     * <p>
     * Allows hiding extra features such as {@link io.reactivex.rxjava3.subjects.Subject}'s
     * {@link Observer} methods or preventing certain identity-based
     * optimizations (fusion).
     * <p>
     * <img width="640" height="283" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/hide.o.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code hide} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @return the new {@code Observable} instance
     *
     * @since 2.0
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Observable<T> hide() {
        return RxJavaPlugins.onAssembly(new ObservableHide<>(this));
    }

    /**
     * Ignores all items emitted by the current {@code Observable} and only calls {@code onComplete} or {@code onError}.
     * <p>
     * <img width="640" height="305" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/ignoreElements.2.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code ignoreElements} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @return the new {@link Completable} instance
     * @see <a href="http://reactivex.io/documentation/operators/ignoreelements.html">ReactiveX operators documentation: IgnoreElements</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Completable ignoreElements() {
        return RxJavaPlugins.onAssembly(new ObservableIgnoreElementsCompletable<>(this));
    }

    /**
     * Returns a {@link Single} that emits {@code true} if the current {@code Observable} is empty, otherwise {@code false}.
     * <p>
     * In Rx.Net this is negated as the {@code any} {@link Observer} but we renamed this in RxJava to better match Java
     * naming idioms.
     * <p>
     * <img width="640" height="320" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/isEmpty.2.v3.png" alt="">
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
        return all(Functions.alwaysFalse());
    }

    /**
     * Correlates the items emitted by two {@link ObservableSource}s based on overlapping durations.
     * <p>
     * There are no guarantees in what order the items get combined when multiple
     * items from one or both source {@code ObservableSource}s overlap.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/join_.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code join} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <TRight> the value type of the right {@code ObservableSource} source
     * @param <TLeftEnd> the element type of the left duration {@code ObservableSource}s
     * @param <TRightEnd> the element type of the right duration {@code ObservableSource}s
     * @param <R> the result type
     * @param other
     *            the second {@code ObservableSource} to join items from
     * @param leftEnd
     *            a function to select a duration for each item emitted by the current {@code Observable}, used to
     *            determine overlap
     * @param rightEnd
     *            a function to select a duration for each item emitted by the {@code right} {@code ObservableSource}, used to
     *            determine overlap
     * @param resultSelector
     *            a function that computes an item to be emitted by the resulting {@code Observable} for any two
     *            overlapping items emitted by the two {@code ObservableSource}s
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code other}, {@code leftEnd}, {@code rightEnd} or {@code resultSelector} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/join.html">ReactiveX operators documentation: Join</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <@NonNull TRight, @NonNull TLeftEnd, @NonNull TRightEnd, @NonNull R> Observable<R> join(
            @NonNull ObservableSource<? extends TRight> other,
            @NonNull Function<? super T, ? extends ObservableSource<TLeftEnd>> leftEnd,
            @NonNull Function<? super TRight, ? extends ObservableSource<TRightEnd>> rightEnd,
            @NonNull BiFunction<? super T, ? super TRight, ? extends R> resultSelector
    ) {
        Objects.requireNonNull(other, "other is null");
        Objects.requireNonNull(leftEnd, "leftEnd is null");
        Objects.requireNonNull(rightEnd, "rightEnd is null");
        Objects.requireNonNull(resultSelector, "resultSelector is null");
        return RxJavaPlugins.onAssembly(new ObservableJoin<T, TRight, TLeftEnd, TRightEnd, R>(
                this, other, leftEnd, rightEnd, resultSelector));
    }

    /**
     * Returns a {@link Maybe} that emits the last item emitted by the current {@code Observable} or
     * completes if the current {@code Observable} is empty.
     * <p>
     * <img width="640" height="305" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/lastElement.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code lastElement} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @return the new {@code Maybe} instance
     * @see <a href="http://reactivex.io/documentation/operators/last.html">ReactiveX operators documentation: Last</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Maybe<T> lastElement() {
        return RxJavaPlugins.onAssembly(new ObservableLastMaybe<>(this));
    }

    /**
     * Returns a {@link Single} that emits only the last item emitted by the current {@code Observable}, or a default item
     * if the current {@code Observable} completes without emitting any items.
     * <p>
     * <img width="640" height="305" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/last.2.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code last} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param defaultItem
     *            the default item to emit if the current {@code Observable} is empty
     * @return the new {@code Single} instance
     * @throws NullPointerException if {@code defaultItem} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/last.html">ReactiveX operators documentation: Last</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Single<T> last(@NonNull T defaultItem) {
        Objects.requireNonNull(defaultItem, "defaultItem is null");
        return RxJavaPlugins.onAssembly(new ObservableLastSingle<>(this, defaultItem));
    }

    /**
     * Returns a {@link Single} that emits only the last item emitted by the current {@code Observable} or
     * signals a {@link NoSuchElementException} if the current {@code Observable} is empty.
     * <p>
     * <img width="640" height="236" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/lastOrError.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code lastOrError} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @return the new {@code Single} instance
     * @see <a href="http://reactivex.io/documentation/operators/last.html">ReactiveX operators documentation: Last</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Single<T> lastOrError() {
        return RxJavaPlugins.onAssembly(new ObservableLastSingle<>(this, null));
    }

    /**
     * <strong>This method requires advanced knowledge about building operators, please consider
     * other standard composition methods first;</strong>
     * Returns an {@code Observable} which, when subscribed to, invokes the {@link ObservableOperator#apply(Observer) apply(Observer)} method
     * of the provided {@link ObservableOperator} for each individual downstream {@link Observer} and allows the
     * insertion of a custom operator by accessing the downstream's {@code Observer} during this subscription phase
     * and providing a new {@code Observer}, containing the custom operator's intended business logic, that will be
     * used in the subscription process going further upstream.
     * <p>
     * Generally, such a new {@code Observer} will wrap the downstream's {@code Observer} and forwards the
     * {@code onNext}, {@code onError} and {@code onComplete} events from the upstream directly or according to the
     * emission pattern the custom operator's business logic requires. In addition, such operator can intercept the
     * flow control calls of {@code dispose} and {@code isDisposed} that would have traveled upstream and perform
     * additional actions depending on the same business logic requirements.
     * <p>
     * Example:
     * <pre><code>
     * // Step 1: Create the consumer type that will be returned by the ObservableOperator.apply():
     *
     * public final class CustomObserver&lt;T&gt; implements Observer&lt;T&gt;, Disposable {
     *
     *     // The downstream's Observer that will receive the onXXX events
     *     final Observer&lt;? super String&gt; downstream;
     *
     *     // The connection to the upstream source that will call this class' onXXX methods
     *     Disposable upstream;
     *
     *     // The constructor takes the downstream subscriber and usually any other parameters
     *     public CustomObserver(Observer&lt;? super String&gt; downstream) {
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
     *     public void onNext(T item) {
     *         String str = item.toString();
     *         if (str.length() &lt; 2) {
     *             downstream.onNext(str);
     *         }
     *         // Observable doesn't support backpressure, therefore, there is no
     *         // need or opportunity to call upstream.request(1) if an item
     *         // is not produced to the downstream
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
     * // Step 2: Create a class that implements the ObservableOperator interface and
     * //         returns the custom consumer type from above in its apply() method.
     * //         Such class may define additional parameters to be submitted to
     * //         the custom consumer type.
     *
     * final class CustomOperator&lt;T&gt; implements ObservableOperator&lt;String, T&gt; {
     *     &#64;Override
     *     public Observer&lt;T&gt; apply(Observer&lt;? super String&gt; downstream) {
     *         return new CustomObserver&lt;T&gt;(downstream);
     *     }
     * }
     *
     * // Step 3: Apply the custom operator via lift() in a flow by creating an instance of it
     * //         or reusing an existing one.
     *
     * Observable.range(5, 10)
     * .lift(new CustomOperator&lt;Integer&gt;())
     * .test()
     * .assertResult("5", "6", "7", "8", "9");
     * </code></pre>
     * <p>
     * Creating custom operators can be complicated and it is recommended one consults the
     * <a href="https://github.com/ReactiveX/RxJava/wiki/Writing-operators-for-2.0">RxJava wiki: Writing operators</a> page about
     * the tools, requirements, rules, considerations and pitfalls of implementing them.
     * <p>
     * Note that implementing custom operators via this {@code lift()} method adds slightly more overhead by requiring
     * an additional allocation and indirection per assembled flows. Instead, extending the abstract {@code Observable}
     * class and creating an {@link ObservableTransformer} with it is recommended.
     * <p>
     * Note also that it is not possible to stop the subscription phase in {@code lift()} as the {@code apply()} method
     * requires a non-{@code null} {@code Observer} instance to be returned, which is then unconditionally subscribed to
     * the current {@code Observable}. For example, if the operator decided there is no reason to subscribe to the
     * upstream source because of some optimization possibility or a failure to prepare the operator, it still has to
     * return an {@code Observer} that should immediately dispose the upstream's {@link Disposable} in its
     * {@code onSubscribe} method. Again, using an {@code ObservableTransformer} and extending the {@code Observable} is
     * a better option as {@link #subscribeActual} can decide to not subscribe to its upstream after all.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code lift} does not operate by default on a particular {@link Scheduler}, however, the
     *  {@code ObservableOperator} may use a {@code Scheduler} to support its own asynchronous behavior.</dd>
     * </dl>
     *
     * @param <R> the output value type
     * @param lifter the {@code ObservableOperator} that receives the downstream's {@code Observer} and should return
     *               an {@code Observer} with custom behavior to be used as the consumer for the current
     *               {@code Observable}.
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code lifter} is {@code null}
     * @see <a href="https://github.com/ReactiveX/RxJava/wiki/Writing-operators-for-2.0">RxJava wiki: Writing operators</a>
     * @see #compose(ObservableTransformer)
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <@NonNull R> Observable<R> lift(@NonNull ObservableOperator<? extends R, ? super T> lifter) {
        Objects.requireNonNull(lifter, "lifter is null");
        return RxJavaPlugins.onAssembly(new ObservableLift<>(this, lifter));
    }

    /**
     * Returns an {@code Observable} that applies a specified function to each item emitted by the current {@code Observable} and
     * emits the results of these function applications.
     * <p>
     * <img width="640" height="305" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/map.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code map} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <R> the output type
     * @param mapper
     *            a function to apply to each item emitted by the current {@code Observable}
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code mapper} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/map.html">ReactiveX operators documentation: Map</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <@NonNull R> Observable<R> map(@NonNull Function<? super T, ? extends R> mapper) {
        Objects.requireNonNull(mapper, "mapper is null");
        return RxJavaPlugins.onAssembly(new ObservableMap<>(this, mapper));
    }

    /**
     * Returns an {@code Observable} that represents all of the emissions <em>and</em> notifications from the current
     * {@code Observable} into emissions marked with their original types within {@link Notification} objects.
     * <p>
     * <img width="640" height="315" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/materialize.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code materialize} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @return the new {@code Observable} instance
     * @see <a href="http://reactivex.io/documentation/operators/materialize-dematerialize.html">ReactiveX operators documentation: Materialize</a>
     * @see #dematerialize(Function)
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Observable<Notification<T>> materialize() {
        return RxJavaPlugins.onAssembly(new ObservableMaterialize<>(this));
    }

    /**
     * Flattens the current {@code Observable} and another {@link ObservableSource} into a single {@code Observable} sequence, without any transformation.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/merge.v3.png" alt="">
     * <p>
     * You can combine items emitted by multiple {@code ObservableSource}s so that they appear as a single {@code ObservableSource}, by
     * using the {@code mergeWith} method.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code mergeWith} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param other
     *            an {@code ObservableSource} to be merged
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code other} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/merge.html">ReactiveX operators documentation: Merge</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Observable<T> mergeWith(@NonNull ObservableSource<? extends T> other) {
        Objects.requireNonNull(other, "other is null");
        return merge(this, other);
    }

    /**
     * Merges the sequence of items of the current {@code Observable} with the success value of the other {@link SingleSource}.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/merge.v3.png" alt="">
     * <p>
     * The success value of the other {@code SingleSource} can get interleaved at any point of the current
     * {@code Observable} sequence.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code mergeWith} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * <p>History: 2.1.10 - experimental
     * @param other the {@code SingleSource} whose success value to merge with
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code other} is {@code null}
     * @since 2.2
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Observable<T> mergeWith(@NonNull SingleSource<? extends T> other) {
        Objects.requireNonNull(other, "other is null");
        return RxJavaPlugins.onAssembly(new ObservableMergeWithSingle<>(this, other));
    }

    /**
     * Merges the sequence of items of the current {@code Observable} with the success value of the other {@link MaybeSource}
     * or waits both to complete normally if the {@code MaybeSource} is empty.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/merge.v3.png" alt="">
     * <p>
     * The success value of the other {@code MaybeSource} can get interleaved at any point of the current
     * {@code Observable} sequence.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code mergeWith} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * <p>History: 2.1.10 - experimental
     * @param other the {@code MaybeSource} which provides a success value to merge with or completes
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code other} is {@code null}
     * @since 2.2
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Observable<T> mergeWith(@NonNull MaybeSource<? extends T> other) {
        Objects.requireNonNull(other, "other is null");
        return RxJavaPlugins.onAssembly(new ObservableMergeWithMaybe<>(this, other));
    }

    /**
     * Relays the items of the current {@code Observable} and completes only when the other {@link CompletableSource} completes
     * as well.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/merge.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code mergeWith} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * <p>History: 2.1.10 - experimental
     * @param other the {@code CompletableSource} to await for completion
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code other} is {@code null}
     * @since 2.2
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Observable<T> mergeWith(@NonNull CompletableSource other) {
        Objects.requireNonNull(other, "other is null");
        return RxJavaPlugins.onAssembly(new ObservableMergeWithCompletable<>(this, other));
    }

    /**
     * Returns an {@code Observable} to perform the current {@code Observable}'s emissions and notifications on a specified {@link Scheduler},
     * asynchronously with an unbounded buffer with {@link Flowable#bufferSize()} "island size".
     *
     * <p>Note that {@code onError} notifications will cut ahead of {@code onNext} notifications on the emission thread if {@code Scheduler} is truly
     * asynchronous. If strict event ordering is required, consider using the {@link #observeOn(Scheduler, boolean)} overload.
     * <p>
     * <img width="640" height="308" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/observeOn.v3.png" alt="">
     * <p>
     * This operator keeps emitting as many signals as it can on the given {@code Scheduler}'s worker thread,
     * which may result in a longer than expected occupation of this thread. In other terms,
     * it does not allow per-signal fairness in case the worker runs on a shared underlying thread.
     * If such fairness and signal/work interleaving is preferred, use the delay operator with zero time instead.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>You specify which {@code Scheduler} this operator will use.</dd>
     * </dl>
     * <p>"Island size" indicates how large chunks the unbounded buffer allocates to store the excess elements waiting to be consumed
     * on the other side of the asynchronous boundary.
     *
     * @param scheduler
     *            the {@code Scheduler} to notify {@link Observer}s on
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code scheduler} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/observeon.html">ReactiveX operators documentation: ObserveOn</a>
     * @see <a href="http://www.grahamlea.com/2014/07/rxjava-threading-examples/">RxJava Threading Examples</a>
     * @see #subscribeOn
     * @see #observeOn(Scheduler, boolean)
     * @see #observeOn(Scheduler, boolean, int)
     * @see #delay(long, TimeUnit, Scheduler)
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    @NonNull
    public final Observable<T> observeOn(@NonNull Scheduler scheduler) {
        return observeOn(scheduler, false, bufferSize());
    }

    /**
     * Returns an {@code Observable} to perform the current {@code Observable}'s emissions and notifications on a specified {@link Scheduler},
     * asynchronously with an unbounded buffer with {@link Flowable#bufferSize()} "island size" and optionally delays {@code onError} notifications.
     * <p>
     * <img width="640" height="308" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/observeOn.v3.png" alt="">
     * <p>
     * This operator keeps emitting as many signals as it can on the given {@code Scheduler}'s worker thread,
     * which may result in a longer than expected occupation of this thread. In other terms,
     * it does not allow per-signal fairness in case the worker runs on a shared underlying thread.
     * If such fairness and signal/work interleaving is preferred, use the delay operator with zero time instead.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>You specify which {@code Scheduler} this operator will use.</dd>
     * </dl>
     * <p>"Island size" indicates how large chunks the unbounded buffer allocates to store the excess elements waiting to be consumed
     * on the other side of the asynchronous boundary.
     *
     * @param scheduler
     *            the {@code Scheduler} to notify {@link Observer}s on
     * @param delayError
     *            indicates if the {@code onError} notification may not cut ahead of {@code onNext} notification on the other side of the
     *            scheduling boundary. If {@code true}, a sequence ending in {@code onError} will be replayed in the same order as was received
     *            from the current {@code Observable}
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code scheduler} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/observeon.html">ReactiveX operators documentation: ObserveOn</a>
     * @see <a href="http://www.grahamlea.com/2014/07/rxjava-threading-examples/">RxJava Threading Examples</a>
     * @see #subscribeOn
     * @see #observeOn(Scheduler)
     * @see #observeOn(Scheduler, boolean, int)
     * @see #delay(long, TimeUnit, Scheduler, boolean)
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    @NonNull
    public final Observable<T> observeOn(@NonNull Scheduler scheduler, boolean delayError) {
        return observeOn(scheduler, delayError, bufferSize());
    }

    /**
     * Returns an {@code Observable} to perform the current {@code Observable}'s emissions and notifications on a specified {@link Scheduler},
     * asynchronously with an unbounded buffer of configurable "island size" and optionally delays {@code onError} notifications.
     * <p>
     * <img width="640" height="308" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/observeOn.v3.png" alt="">
     * <p>
     * This operator keeps emitting as many signals as it can on the given {@code Scheduler}'s worker thread,
     * which may result in a longer than expected occupation of this thread. In other terms,
     * it does not allow per-signal fairness in case the worker runs on a shared underlying thread.
     * If such fairness and signal/work interleaving is preferred, use the delay operator with zero time instead.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>You specify which {@code Scheduler} this operator will use.</dd>
     * </dl>
     * <p>"Island size" indicates how large chunks the unbounded buffer allocates to store the excess elements waiting to be consumed
     * on the other side of the asynchronous boundary. Values below 16 are not recommended in performance sensitive scenarios.
     *
     * @param scheduler
     *            the {@code Scheduler} to notify {@link Observer}s on
     * @param delayError
     *            indicates if the {@code onError} notification may not cut ahead of {@code onNext} notification on the other side of the
     *            scheduling boundary. If {@code true} a sequence ending in {@code onError} will be replayed in the same order as was received
     *            from upstream
     * @param bufferSize the size of the buffer.
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code scheduler} is {@code null}
     * @throws IllegalArgumentException if {@code bufferSize} is non-positive
     * @see <a href="http://reactivex.io/documentation/operators/observeon.html">ReactiveX operators documentation: ObserveOn</a>
     * @see <a href="http://www.grahamlea.com/2014/07/rxjava-threading-examples/">RxJava Threading Examples</a>
     * @see #subscribeOn
     * @see #observeOn(Scheduler)
     * @see #observeOn(Scheduler, boolean)
     * @see #delay(long, TimeUnit, Scheduler, boolean)
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    @NonNull
    public final Observable<T> observeOn(@NonNull Scheduler scheduler, boolean delayError, int bufferSize) {
        Objects.requireNonNull(scheduler, "scheduler is null");
        ObjectHelper.verifyPositive(bufferSize, "bufferSize");
        return RxJavaPlugins.onAssembly(new ObservableObserveOn<>(this, scheduler, delayError, bufferSize));
    }

    /**
     * Filters the items emitted by the current {@code Observable}, only emitting those of the specified type.
     * <p>
     * <img width="640" height="310" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/ofClass.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code ofType} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <U> the output type
     * @param clazz
     *            the class type to filter the items emitted by the current {@code Observable}
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code clazz} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/filter.html">ReactiveX operators documentation: Filter</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <@NonNull U> Observable<U> ofType(@NonNull Class<U> clazz) {
        Objects.requireNonNull(clazz, "clazz is null");
        return filter(Functions.isInstanceOf(clazz)).cast(clazz);
    }

    /**
     * Returns an {@code Observable} instance that if the current {@code Observable} emits an error, it will emit an {@code onComplete}
     * and swallow the throwable.
     * <p>
     * <img width="640" height="373" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Observable.onErrorComplete.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code onErrorComplete} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @return the new {@code Observable} instance
     * @since 3.0.0
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Observable<T> onErrorComplete() {
        return onErrorComplete(Functions.alwaysTrue());
    }

    /**
     * Returns an {@code Observable} instance that if the current {@code Observable} emits an error and the predicate returns
     * {@code true}, it will emit an {@code onComplete} and swallow the throwable.
     * <p>
     * <img width="640" height="215" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Observable.onErrorComplete.f.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code onErrorComplete} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param predicate the predicate to call when an {@link Throwable} is emitted which should return {@code true}
     * if the {@code Throwable} should be swallowed and replaced with an {@code onComplete}.
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code predicate} is {@code null}
     * @since 3.0.0
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Observable<T> onErrorComplete(@NonNull Predicate<? super Throwable> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");

        return RxJavaPlugins.onAssembly(new ObservableOnErrorComplete<>(this, predicate));
    }

    /**
     * Resumes the flow with an {@link ObservableSource} returned for the failure {@link Throwable} of the current {@code Observable} by a
     * function instead of signaling the error via {@code onError}.
     * <p>
     * <img width="640" height="310" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/onErrorResumeNext.v3.png" alt="">
     * <p>
     * By default, when an {@code ObservableSource} encounters an error that prevents it from emitting the expected item to
     * its {@link Observer}, the {@code ObservableSource} invokes its {@code Observer}'s {@code onError} method, and then quits
     * without invoking any more of its {@code Observer}'s methods. The {@code onErrorResumeNext} method changes this
     * behavior. If you pass a function that returns an {@code ObservableSource} ({@code resumeFunction}) to
     * {@code onErrorResumeNext}, if the original {@code ObservableSource} encounters an error, instead of invoking its
     * {@code Observer}'s {@code onError} method, it will instead relinquish control to the {@code ObservableSource} returned from
     * {@code resumeFunction}, which will invoke the {@code Observer}'s {@link Observer#onNext onNext} method if it is
     * able to do so. In such a case, because no {@code ObservableSource} necessarily invokes {@code onError}, the {@code Observer}
     * may never know that an error happened.
     * <p>
     * You can use this to prevent errors from propagating or to supply fallback data should errors be
     * encountered.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code onErrorResumeNext} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param fallbackSupplier
     *            a function that returns an {@code ObservableSource} that will take over if the current {@code Observable} encounters
     *            an error
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code fallbackSupplier} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/catch.html">ReactiveX operators documentation: Catch</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Observable<T> onErrorResumeNext(@NonNull Function<? super Throwable, ? extends ObservableSource<? extends T>> fallbackSupplier) {
        Objects.requireNonNull(fallbackSupplier, "fallbackSupplier is null");
        return RxJavaPlugins.onAssembly(new ObservableOnErrorNext<>(this, fallbackSupplier));
    }

    /**
     * Resumes the flow with the given {@link ObservableSource} when the current {@code Observable} fails instead of
     * signaling the error via {@code onError}.
     * <p>
     * <img width="640" height="310" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/onErrorResumeWith.v3.png" alt="">
     * <p>
     * By default, when an {@code ObservableSource} encounters an error that prevents it from emitting the expected item to
     * its {@link Observer}, the {@code ObservableSource} invokes its {@code Observer}'s {@code onError} method, and then quits
     * without invoking any more of its {@code Observer}'s methods. The {@code onErrorResumeWith} method changes this
     * behavior. If you pass another {@code ObservableSource} ({@code next}) to an {@code ObservableSource}'s
     * {@code onErrorResumeWith} method, if the original {@code ObservableSource} encounters an error, instead of invoking its
     * {@code Observer}'s {@code onError} method, it will instead relinquish control to {@code next} which
     * will invoke the {@code Observer}'s {@link Observer#onNext onNext} method if it is able to do so. In such a case,
     * because no {@code ObservableSource} necessarily invokes {@code onError}, the {@code Observer} may never know that an error
     * happened.
     * <p>
     * You can use this to prevent errors from propagating or to supply fallback data should errors be
     * encountered.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code onErrorResumeWith} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param fallback
     *            the next {@code ObservableSource} source that will take over if the current {@code Observable} encounters
     *            an error
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code fallback} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/catch.html">ReactiveX operators documentation: Catch</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Observable<T> onErrorResumeWith(@NonNull ObservableSource<? extends T> fallback) {
        Objects.requireNonNull(fallback, "fallback is null");
        return onErrorResumeNext(Functions.justFunction(fallback));
    }

    /**
     * Ends the flow with a last item returned by a function for the {@link Throwable} error signaled by the current
     * {@code Observable} instead of signaling the error via {@code onError}.
     * <p>
     * <img width="640" height="310" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/onErrorReturn.o.v3.png" alt="">
     * <p>
     * By default, when an {@link ObservableSource} encounters an error that prevents it from emitting the expected item to
     * its {@link Observer}, the {@code ObservableSource} invokes its {@code Observer}'s {@code onError} method, and then quits
     * without invoking any more of its {@code Observer}'s methods. The {@code onErrorReturn} method changes this
     * behavior. If you pass a function ({@code resumeFunction}) to an {@code ObservableSource}'s {@code onErrorReturn}
     * method, if the original {@code ObservableSource} encounters an error, instead of invoking its {@code Observer}'s
     * {@code onError} method, it will instead emit the return value of {@code resumeFunction}.
     * <p>
     * You can use this to prevent errors from propagating or to supply fallback data should errors be
     * encountered.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code onErrorReturn} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param itemSupplier
     *            a function that returns a single value that will be emitted along with a regular {@code onComplete} in case
     *            the current {@code Observable} signals an {@code onError} event
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code itemSupplier} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/catch.html">ReactiveX operators documentation: Catch</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Observable<T> onErrorReturn(@NonNull Function<? super Throwable, ? extends T> itemSupplier) {
        Objects.requireNonNull(itemSupplier, "itemSupplier is null");
        return RxJavaPlugins.onAssembly(new ObservableOnErrorReturn<>(this, itemSupplier));
    }

    /**
     * Ends the flow with the given last item when the current {@code Observable} fails instead of signaling the error via {@code onError}.
     * <p>
     * <img width="640" height="310" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/onErrorReturnItem.o.v3.png" alt="">
     * <p>
     * By default, when an {@link ObservableSource} encounters an error that prevents it from emitting the expected item to
     * its {@link Observer}, the {@code ObservableSource} invokes its {@code Observer}'s {@code onError} method, and then quits
     * without invoking any more of its {@code Observer}'s methods. The {@code onErrorReturn} method changes this
     * behavior. If you pass a function ({@code resumeFunction}) to an {@code ObservableSource}'s {@code onErrorReturn}
     * method, if the original {@code ObservableSource} encounters an error, instead of invoking its {@code Observer}'s
     * {@code onError} method, it will instead emit the return value of {@code resumeFunction}.
     * <p>
     * You can use this to prevent errors from propagating or to supply fallback data should errors be
     * encountered.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code onErrorReturnItem} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param item
     *            the value that is emitted along with a regular {@code onComplete} in case the current
     *            {@code Observable} signals an exception
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code item} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/catch.html">ReactiveX operators documentation: Catch</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Observable<T> onErrorReturnItem(@NonNull T item) {
        Objects.requireNonNull(item, "item is null");
        return onErrorReturn(Functions.justFunction(item));
    }

    /**
     * Nulls out references to the upstream producer and downstream {@link Observer} if
     * the sequence is terminated or downstream calls {@code dispose()}.
     * <p>
     * <img width="640" height="247" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/onTerminateDetach.o.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code onTerminateDetach} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @return the new {@code Observable} instance
     * the sequence is terminated or downstream calls {@code dispose()}
     * @since 2.0
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Observable<T> onTerminateDetach() {
        return RxJavaPlugins.onAssembly(new ObservableDetach<>(this));
    }

    /**
     * Returns a {@link ConnectableObservable}, which is a variety of {@link ObservableSource} that waits until its
     * {@link ConnectableObservable#connect connect} method is called before it begins emitting items to those
     * {@link Observer}s that have subscribed to it.
     * <p>
     * <img width="640" height="510" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/publishConnect.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code publish} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @return the new {@code ConnectableObservable} instance
     * @see <a href="http://reactivex.io/documentation/operators/publish.html">ReactiveX operators documentation: Publish</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final ConnectableObservable<T> publish() {
        return RxJavaPlugins.onAssembly(new ObservablePublish<>(this));
    }

    /**
     * Returns an {@code Observable} that emits the results of invoking a specified selector on items emitted by a
     * {@link ConnectableObservable} that shares a single subscription to the current {@code Observable} sequence.
     * <p>
     * <img width="640" height="647" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/publishFunction.o.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code publish} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <R>
     *            the type of items emitted by the resulting {@code Observable}
     * @param selector
     *            a function that can use the multicasted source sequence as many times as needed, without
     *            causing multiple subscriptions to the source sequence. {@link Observer}s to the given source will
     *            receive all notifications of the source from the time of the subscription forward.
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code selector} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/publish.html">ReactiveX operators documentation: Publish</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <@NonNull R> Observable<R> publish(@NonNull Function<? super Observable<T>, ? extends ObservableSource<R>> selector) {
        Objects.requireNonNull(selector, "selector is null");
        return RxJavaPlugins.onAssembly(new ObservablePublishSelector<>(this, selector));
    }

    /**
     * Returns a {@link Maybe} that applies a specified accumulator function to the first item emitted by the current
     * {@code Observable}, then feeds the result of that function along with the second item emitted by the current
     * {@code Observable} into the same function, and so on until all items have been emitted by the current and finite {@code Observable},
     * and emits the final result from the final call to your function as its sole item.
     * <p>
     * <img width="640" height="320" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/reduce.2.v3.png" alt="">
     * <p>
     * This technique, which is called "reduce" here, is sometimes called "aggregate," "fold," "accumulate,"
     * "compress," or "inject" in other programming contexts. Groovy, for instance, has an {@code inject} method
     * that does a similar operation on lists.
     * <p>
     * Note that this operator requires the upstream to signal {@code onComplete} for the accumulator object to
     * be emitted. Sources that are infinite and never complete will never emit anything through this
     * operator and an infinite source may lead to a fatal {@link OutOfMemoryError}.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code reduce} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param reducer
     *            an accumulator function to be invoked on each item emitted by the current {@code Observable}, whose
     *            result will be used in the next accumulator call
     * @return the new {@code Maybe} instance
     * @throws NullPointerException if {@code reducer} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/reduce.html">ReactiveX operators documentation: Reduce</a>
     * @see <a href="http://en.wikipedia.org/wiki/Fold_(higher-order_function)">Wikipedia: Fold (higher-order function)</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Maybe<T> reduce(@NonNull BiFunction<T, T, T> reducer) {
        Objects.requireNonNull(reducer, "reducer is null");
        return RxJavaPlugins.onAssembly(new ObservableReduceMaybe<>(this, reducer));
    }

    /**
     * Returns a {@link Single} that applies a specified accumulator function to the first item emitted by the current
     * {@code Observable} and a specified seed value, then feeds the result of that function along with the second item
     * emitted by the current {@code Observable} into the same function, and so on until all items have been emitted by the
     * current and finite {@code Observable}, emitting the final result from the final call to your function as its sole item.
     * <p>
     * <img width="640" height="325" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/reduceSeed.o.v3.png" alt="">
     * <p>
     * This technique, which is called "reduce" here, is sometimes called "aggregate," "fold," "accumulate,"
     * "compress," or "inject" in other programming contexts. Groovy, for instance, has an {@code inject} method
     * that does a similar operation on lists.
     * <p>
     * Note that the {@code seed} is shared among all subscribers to the resulting {@code Observable}
     * and may cause problems if it is mutable. To make sure each subscriber gets its own value, defer
     * the application of this operator via {@link #defer(Supplier)}:
     * <pre><code>
     * ObservableSource&lt;T&gt; source = ...
     * Single.defer(() -&gt; source.reduce(new ArrayList&lt;&gt;(), (list, item) -&gt; list.add(item)));
     *
     * // alternatively, by using compose to stay fluent
     *
     * source.compose(o -&gt;
     *     Observable.defer(() -&gt; o.reduce(new ArrayList&lt;&gt;(), (list, item) -&gt; list.add(item)).toObservable())
     * ).firstOrError();
     *
     * // or, by using reduceWith instead of reduce
     *
     * source.reduceWith(() -&gt; new ArrayList&lt;&gt;(), (list, item) -&gt; list.add(item)));
     * </code></pre>
     * <p>
     * Note that this operator requires the upstream to signal {@code onComplete} for the accumulator object to
     * be emitted. Sources that are infinite and never complete will never emit anything through this
     * operator and an infinite source may lead to a fatal {@link OutOfMemoryError}.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code reduce} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <R> the accumulator and output value type
     * @param seed
     *            the initial (seed) accumulator value
     * @param reducer
     *            an accumulator function to be invoked on each item emitted by the current {@code Observable}, the
     *            result of which will be used in the next accumulator call
     * @return the new {@code Single} instance
     * @throws NullPointerException if {@code seed} or {@code reducer} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/reduce.html">ReactiveX operators documentation: Reduce</a>
     * @see <a href="http://en.wikipedia.org/wiki/Fold_(higher-order_function)">Wikipedia: Fold (higher-order function)</a>
     * @see #reduceWith(Supplier, BiFunction)
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <@NonNull R> Single<R> reduce(R seed, @NonNull BiFunction<R, ? super T, R> reducer) {
        Objects.requireNonNull(seed, "seed is null");
        Objects.requireNonNull(reducer, "reducer is null");
        return RxJavaPlugins.onAssembly(new ObservableReduceSeedSingle<>(this, seed, reducer));
    }

    /**
     * Returns a {@link Single} that applies a specified accumulator function to the first item emitted by the current
     * {@code Observable} and a seed value derived from calling a specified {@code seedSupplier}, then feeds the result
     * of that function along with the second item emitted by the current {@code Observable} into the same function,
     * and so on until all items have been emitted by the current and finite {@code Observable}, emitting the final result
     * from the final call to your function as its sole item.
     * <p>
     * <img width="640" height="325" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/reduceWith.o.v3.png" alt="">
     * <p>
     * This technique, which is called "reduce" here, is sometimes called "aggregate," "fold," "accumulate,"
     * "compress," or "inject" in other programming contexts. Groovy, for instance, has an {@code inject} method
     * that does a similar operation on lists.
     * <p>
     * Note that this operator requires the upstream to signal {@code onComplete} for the accumulator object to
     * be emitted. Sources that are infinite and never complete will never emit anything through this
     * operator and an infinite source may lead to a fatal {@link OutOfMemoryError}.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code reduceWith} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <R> the accumulator and output value type
     * @param seedSupplier
     *            the {@link Supplier} that provides the initial (seed) accumulator value for each individual {@link Observer}
     * @param reducer
     *            an accumulator function to be invoked on each item emitted by the current {@code Observable}, the
     *            result of which will be used in the next accumulator call
     * @return the new {@code Single} instance
     * @throws NullPointerException if {@code seedSupplier} or {@code reducer} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/reduce.html">ReactiveX operators documentation: Reduce</a>
     * @see <a href="http://en.wikipedia.org/wiki/Fold_(higher-order_function)">Wikipedia: Fold (higher-order function)</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <@NonNull R> Single<R> reduceWith(@NonNull Supplier<R> seedSupplier, @NonNull BiFunction<R, ? super T, R> reducer) {
        Objects.requireNonNull(seedSupplier, "seedSupplier is null");
        Objects.requireNonNull(reducer, "reducer is null");
        return RxJavaPlugins.onAssembly(new ObservableReduceWithSingle<>(this, seedSupplier, reducer));
    }

    /**
     * Returns an {@code Observable} that repeats the sequence of items emitted by the current {@code Observable} indefinitely.
     * <p>
     * <img width="640" height="287" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/repeatInf.o.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code repeat} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @return the new {@code Observable} instance
     * @see <a href="http://reactivex.io/documentation/operators/repeat.html">ReactiveX operators documentation: Repeat</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Observable<T> repeat() {
        return repeat(Long.MAX_VALUE);
    }

    /**
     * Returns an {@code Observable} that repeats the sequence of items emitted by the current {@code Observable} at most
     * {@code count} times.
     * <p>
     * <img width="640" height="336" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/repeatCount.o.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code repeat} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param times
     *            the number of times the current {@code Observable} items are repeated, a count of 0 will yield an empty
     *            sequence
     * @return the new {@code Observable} instance
     * @throws IllegalArgumentException
     *             if {@code times} is negative
     * @see <a href="http://reactivex.io/documentation/operators/repeat.html">ReactiveX operators documentation: Repeat</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Observable<T> repeat(long times) {
        if (times < 0) {
            throw new IllegalArgumentException("times >= 0 required but it was " + times);
        }
        if (times == 0) {
            return empty();
        }
        return RxJavaPlugins.onAssembly(new ObservableRepeat<>(this, times));
    }

    /**
     * Returns an {@code Observable} that repeats the sequence of items emitted by the current {@code Observable} until
     * the provided stop function returns {@code true}.
     * <p>
     * <img width="640" height="263" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/repeatUntil.o.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code repeatUntil} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param stop
     *                a boolean supplier that is called when the current {@code Observable} completes;
     *                if it returns {@code true}, the returned {@code Observable} completes; if it returns {@code false},
     *                the current {@code Observable} is resubscribed.
     * @return the new {@code Observable} instance
     * @throws NullPointerException
     *             if {@code stop} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/repeat.html">ReactiveX operators documentation: Repeat</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Observable<T> repeatUntil(@NonNull BooleanSupplier stop) {
        Objects.requireNonNull(stop, "stop is null");
        return RxJavaPlugins.onAssembly(new ObservableRepeatUntil<>(this, stop));
    }

    /**
     * Returns an {@code Observable} that emits the same values as the current {@code Observable} with the exception of an
     * {@code onComplete}. An {@code onComplete} notification from the source will result in the emission of
     * a {@code void} item to the {@link ObservableSource} provided as an argument to the {@code notificationHandler}
     * function. If that {@code ObservableSource} calls {@code onComplete} or {@code onError} then {@code repeatWhen} will
     * call {@code onComplete} or {@code onError} on the child subscription. Otherwise, the current {@code Observable}
     * will be resubscribed.
     * <p>
     * <img width="640" height="430" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/repeatWhen.f.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code repeatWhen} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param handler
     *            receives an {@code ObservableSource} of notifications with which a user can complete or error, aborting the repeat.
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code handler} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/repeat.html">ReactiveX operators documentation: Repeat</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Observable<T> repeatWhen(@NonNull Function<? super Observable<Object>, ? extends ObservableSource<?>> handler) {
        Objects.requireNonNull(handler, "handler is null");
        return RxJavaPlugins.onAssembly(new ObservableRepeatWhen<>(this, handler));
    }

    /**
     * Returns a {@link ConnectableObservable} that shares a single subscription to the current {@code Observable}
     * that will replay all of its items and notifications to any future {@link Observer}. A connectable
     * {@code Observable} resembles an ordinary {@code Observable}, except that it does not begin emitting items when it is
     * subscribed to, but only when its {@code connect} method is called.
     * <p>
     * <img width="640" height="445" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/replay.o.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This version of {@code replay} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @return the new {@code ConnectableObservable} instance
     * @see <a href="http://reactivex.io/documentation/operators/replay.html">ReactiveX operators documentation: Replay</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final ConnectableObservable<T> replay() {
        return ObservableReplay.createFrom(this);
    }

    /**
     * Returns an {@code Observable} that emits items that are the results of invoking a specified selector on the items
     * emitted by a {@link ConnectableObservable} that shares a single subscription to the current {@code Observable}.
     * <p>
     * <img width="640" height="449" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/replay.o.f.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This version of {@code replay} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <R>
     *            the type of items emitted by the resulting {@code Observable}
     * @param selector
     *            the selector function, which can use the multicasted sequence as many times as needed, without
     *            causing multiple subscriptions to the current {@code Observable}
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code selector} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/replay.html">ReactiveX operators documentation: Replay</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <@NonNull R> Observable<R> replay(@NonNull Function<? super Observable<T>, ? extends ObservableSource<R>> selector) {
        Objects.requireNonNull(selector, "selector is null");
        return ObservableReplay.multicastSelector(ObservableInternalHelper.replaySupplier(this), selector);
    }

    /**
     * Returns an {@code Observable} that emits items that are the results of invoking a specified selector on items
     * emitted by a {@link ConnectableObservable} that shares a single subscription to the current {@code Observable},
     * replaying {@code bufferSize} notifications.
     * <p>
     * Note that due to concurrency requirements, {@code replay(bufferSize)} may hold strong references to more than
     * {@code bufferSize} source emissions.
     * <p>
     * <img width="640" height="392" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/replay.o.fn.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This version of {@code replay} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <R>
     *            the type of items emitted by the resulting {@code Observable}
     * @param selector
     *            the selector function, which can use the multicasted sequence as many times as needed, without
     *            causing multiple subscriptions to the current {@code Observable}
     * @param bufferSize
     *            the buffer size that limits the number of items the connectable {@code Observable} can replay
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code selector} is {@code null}
     * @throws IllegalArgumentException if {@code bufferSize} is non-positive
     * @see <a href="http://reactivex.io/documentation/operators/replay.html">ReactiveX operators documentation: Replay</a>
     * @see #replay(Function, int, boolean)
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <@NonNull R> Observable<R> replay(@NonNull Function<? super Observable<T>, ? extends ObservableSource<R>> selector, int bufferSize) {
        Objects.requireNonNull(selector, "selector is null");
        ObjectHelper.verifyPositive(bufferSize, "bufferSize");
        return ObservableReplay.multicastSelector(ObservableInternalHelper.replaySupplier(this, bufferSize, false), selector);
    }

    /**
     * Returns an {@code Observable} that emits items that are the results of invoking a specified selector on items
     * emitted by a {@link ConnectableObservable} that shares a single subscription to the current {@code Observable},
     * replaying {@code bufferSize} notifications.
     * <p>
     * Note that due to concurrency requirements, {@code replay(bufferSize)} may hold strong references to more than
     * {@code bufferSize} source emissions.
     * <p>
     * <img width="640" height="392" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/replay.o.fn.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This version of {@code replay} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <R>
     *            the type of items emitted by the resulting {@code Observable}
     * @param selector
     *            the selector function, which can use the multicasted sequence as many times as needed, without
     *            causing multiple subscriptions to the current {@code Observable}
     * @param bufferSize
     *            the buffer size that limits the number of items the connectable {@code Observable} can replay
     * @param eagerTruncate
     *            if {@code true}, whenever the internal buffer is truncated to the given bufferSize, the
     *            oldest item will be guaranteed dereferenced, thus avoiding unexpected retention
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code selector} is {@code null}
     * @throws IllegalArgumentException if {@code bufferSize} is non-positive
     * @see <a href="http://reactivex.io/documentation/operators/replay.html">ReactiveX operators documentation: Replay</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <@NonNull R> Observable<R> replay(@NonNull Function<? super Observable<T>, ? extends ObservableSource<R>> selector, int bufferSize, boolean eagerTruncate) {
        Objects.requireNonNull(selector, "selector is null");
        ObjectHelper.verifyPositive(bufferSize, "bufferSize");
        return ObservableReplay.multicastSelector(ObservableInternalHelper.replaySupplier(this, bufferSize, eagerTruncate), selector);
    }

    /**
     * Returns an {@code Observable} that emits items that are the results of invoking a specified selector on items
     * emitted by a {@link ConnectableObservable} that shares a single subscription to the current {@code Observable},
     * replaying no more than {@code bufferSize} items that were emitted within a specified time window.
     * <p>
     * Note that due to concurrency requirements, {@code replay(bufferSize)} may hold strong references to more than
     * {@code bufferSize} source emissions.
     * <p>
     * <img width="640" height="350" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/replay.o.fnt.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This version of {@code replay} operates by default on the {@code computation} {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <R>
     *            the type of items emitted by the resulting {@code Observable}
     * @param selector
     *            a selector function, which can use the multicasted sequence as many times as needed, without
     *            causing multiple subscriptions to the current {@code Observable}
     * @param bufferSize
     *            the buffer size that limits the number of items the connectable {@code Observable} can replay
     * @param time
     *            the duration of the window in which the replayed items must have been emitted
     * @param unit
     *            the time unit of {@code time}
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code selector} or {@code unit} is {@code null}
     * @throws IllegalArgumentException if {@code bufferSize} is non-positive
     * @see <a href="http://reactivex.io/documentation/operators/replay.html">ReactiveX operators documentation: Replay</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.COMPUTATION)
    @NonNull
    public final <@NonNull R> Observable<R> replay(@NonNull Function<? super Observable<T>, ? extends ObservableSource<R>> selector, int bufferSize, long time, @NonNull TimeUnit unit) {
        return replay(selector, bufferSize, time, unit, Schedulers.computation());
    }

    /**
     * Returns an {@code Observable} that emits items that are the results of invoking a specified selector on items
     * emitted by a {@link ConnectableObservable} that shares a single subscription to the current {@code Observable},
     * replaying no more than {@code bufferSize} items that were emitted within a specified time window.
     * <p>
     * Note that due to concurrency requirements, {@code replay(bufferSize)} may hold strong references to more than
     * {@code bufferSize} source emissions.
     * <p>
     * <img width="640" height="329" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/replay.o.fnts.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>You specify which {@link Scheduler} this operator will use.</dd>
     * </dl>
     *
     * @param <R>
     *            the type of items emitted by the resulting {@code Observable}
     * @param selector
     *            a selector function, which can use the multicasted sequence as many times as needed, without
     *            causing multiple subscriptions to the current {@code Observable}
     * @param bufferSize
     *            the buffer size that limits the number of items the connectable {@code Observable} can replay
     * @param time
     *            the duration of the window in which the replayed items must have been emitted
     * @param unit
     *            the time unit of {@code time}
     * @param scheduler
     *            the {@code Scheduler} that is the time source for the window
     * @return the new {@code Observable} instance
     * @throws IllegalArgumentException
     *             if {@code bufferSize} is non-positive
     * @throws NullPointerException if {@code selector}, {@code unit} or {@code scheduler} is {@code null}
     * @throws IllegalArgumentException if {@code bufferSize} is non-positive
     * @see <a href="http://reactivex.io/documentation/operators/replay.html">ReactiveX operators documentation: Replay</a>
     * @see #replay(Function, int, long, TimeUnit, Scheduler, boolean)
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    @NonNull
    public final <@NonNull R> Observable<R> replay(@NonNull Function<? super Observable<T>, ? extends ObservableSource<R>> selector, int bufferSize, long time, @NonNull TimeUnit unit, @NonNull Scheduler scheduler) {
        Objects.requireNonNull(selector, "selector is null");
        ObjectHelper.verifyPositive(bufferSize, "bufferSize");
        Objects.requireNonNull(unit, "unit is null");
        Objects.requireNonNull(scheduler, "scheduler is null");
        return ObservableReplay.multicastSelector(
                ObservableInternalHelper.replaySupplier(this, bufferSize, time, unit, scheduler, false), selector);
    }

    /**
     * Returns an {@code Observable} that emits items that are the results of invoking a specified selector on items
     * emitted by a {@link ConnectableObservable} that shares a single subscription to the current {@code Observable},
     * replaying no more than {@code bufferSize} items that were emitted within a specified time window.
     * <p>
     * Note that due to concurrency requirements, {@code replay(bufferSize)} may hold strong references to more than
     * {@code bufferSize} source emissions.
     * <p>
     * <img width="640" height="329" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/replay.o.fnts.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>You specify which {@link Scheduler} this operator will use.</dd>
     * </dl>
     *
     * @param <R>
     *            the type of items emitted by the resulting {@code Observable}
     * @param selector
     *            a selector function, which can use the multicasted sequence as many times as needed, without
     *            causing multiple subscriptions to the current {@code Observable}
     * @param bufferSize
     *            the buffer size that limits the number of items the connectable {@code Observable} can replay
     * @param time
     *            the duration of the window in which the replayed items must have been emitted
     * @param unit
     *            the time unit of {@code time}
     * @param scheduler
     *            the {@code Scheduler} that is the time source for the window
     * @param eagerTruncate
     *            if {@code true}, whenever the internal buffer is truncated to the given bufferSize/age, the
     *            oldest item will be guaranteed dereferenced, thus avoiding unexpected retention
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code selector}, {@code unit} or {@code scheduler} is {@code null}
     * @throws IllegalArgumentException
     *             if {@code bufferSize} is non-positive
     * @see <a href="http://reactivex.io/documentation/operators/replay.html">ReactiveX operators documentation: Replay</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    @NonNull
    public final <@NonNull R> Observable<R> replay(@NonNull Function<? super Observable<T>, ? extends ObservableSource<R>> selector, int bufferSize, long time, @NonNull TimeUnit unit, @NonNull Scheduler scheduler, boolean eagerTruncate) {
        Objects.requireNonNull(selector, "selector is null");
        ObjectHelper.verifyPositive(bufferSize, "bufferSize");
        Objects.requireNonNull(unit, "unit is null");
        Objects.requireNonNull(scheduler, "scheduler is null");
        return ObservableReplay.multicastSelector(
                ObservableInternalHelper.replaySupplier(this, bufferSize, time, unit, scheduler, eagerTruncate), selector);
    }

    /**
     * Returns an {@code Observable} that emits items that are the results of invoking a specified selector on items
     * emitted by a {@link ConnectableObservable} that shares a single subscription to the current {@code Observable},
     * replaying all items that were emitted within a specified time window.
     * <p>
     * <img width="640" height="394" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/replay.o.ft.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This version of {@code replay} operates by default on the {@code computation} {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <R>
     *            the type of items emitted by the resulting {@code Observable}
     * @param selector
     *            a selector function, which can use the multicasted sequence as many times as needed, without
     *            causing multiple subscriptions to the current {@code Observable}
     * @param time
     *            the duration of the window in which the replayed items must have been emitted
     * @param unit
     *            the time unit of {@code time}
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code selector} or {@code unit} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/replay.html">ReactiveX operators documentation: Replay</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.COMPUTATION)
    @NonNull
    public final <@NonNull R> Observable<R> replay(@NonNull Function<? super Observable<T>, ? extends ObservableSource<R>> selector, long time, @NonNull TimeUnit unit) {
        return replay(selector, time, unit, Schedulers.computation());
    }

    /**
     * Returns an {@code Observable} that emits items that are the results of invoking a specified selector on items
     * emitted by a {@link ConnectableObservable} that shares a single subscription to the current {@code Observable},
     * replaying all items that were emitted within a specified time window.
     * <p>
     * <img width="640" height="367" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/replay.o.fts.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>You specify which {@link Scheduler} this operator will use.</dd>
     * </dl>
     *
     * @param <R>
     *            the type of items emitted by the resulting {@code Observable}
     * @param selector
     *            a selector function, which can use the multicasted sequence as many times as needed, without
     *            causing multiple subscriptions to the current {@code Observable}
     * @param time
     *            the duration of the window in which the replayed items must have been emitted
     * @param unit
     *            the time unit of {@code time}
     * @param scheduler
     *            the scheduler that is the time source for the window
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code selector}, {@code unit} or {@code scheduler} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/replay.html">ReactiveX operators documentation: Replay</a>
     * @see #replay(Function, long, TimeUnit, Scheduler, boolean)
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    @NonNull
    public final <@NonNull R> Observable<R> replay(@NonNull Function<? super Observable<T>, ? extends ObservableSource<R>> selector, long time, @NonNull TimeUnit unit, @NonNull Scheduler scheduler) {
        Objects.requireNonNull(selector, "selector is null");
        Objects.requireNonNull(unit, "unit is null");
        Objects.requireNonNull(scheduler, "scheduler is null");
        return ObservableReplay.multicastSelector(ObservableInternalHelper.replaySupplier(this, time, unit, scheduler, false), selector);
    }

    /**
     * Returns an {@code Observable} that emits items that are the results of invoking a specified selector on items
     * emitted by a {@link ConnectableObservable} that shares a single subscription to the current {@code Observable},
     * replaying all items that were emitted within a specified time window.
     * <p>
     * <img width="640" height="367" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/replay.o.fts.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>You specify which {@link Scheduler} this operator will use.</dd>
     * </dl>
     *
     * @param <R>
     *            the type of items emitted by the resulting {@code Observable}
     * @param selector
     *            a selector function, which can use the multicasted sequence as many times as needed, without
     *            causing multiple subscriptions to the current {@code Observable}
     * @param time
     *            the duration of the window in which the replayed items must have been emitted
     * @param unit
     *            the time unit of {@code time}
     * @param scheduler
     *            the scheduler that is the time source for the window
     * @param eagerTruncate
     *            if {@code true}, whenever the internal buffer is truncated to the given age, the
     *            oldest item will be guaranteed dereferenced, thus avoiding unexpected retention
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code selector}, {@code unit} or {@code scheduler} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/replay.html">ReactiveX operators documentation: Replay</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    @NonNull
    public final <@NonNull R> Observable<R> replay(@NonNull Function<? super Observable<T>, ? extends ObservableSource<R>> selector, long time, @NonNull TimeUnit unit, @NonNull Scheduler scheduler, boolean eagerTruncate) {
        Objects.requireNonNull(selector, "selector is null");
        Objects.requireNonNull(unit, "unit is null");
        Objects.requireNonNull(scheduler, "scheduler is null");
        return ObservableReplay.multicastSelector(ObservableInternalHelper.replaySupplier(this, time, unit, scheduler, eagerTruncate), selector);
    }

    /**
     * Returns a {@link ConnectableObservable} that shares a single subscription to the current {@code Observable} that
     * replays at most {@code bufferSize} items emitted by the current {@code Observable}. A connectable {@code Observable} resembles
     * an ordinary {@code Observable}, except that it does not begin emitting items when it is subscribed to, but only
     * when its {@code connect} method is called.
     * <p>
     * <img width="640" height="445" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/replay.o.n.v3.png" alt="">
     * <p>
     * Note that due to concurrency requirements, {@code replay(bufferSize)} may hold strong references to more than
     * {@code bufferSize} source emissions.
     * To ensure no beyond-bufferSize items are referenced,
     * use the {@link #replay(int, boolean)} overload with {@code eagerTruncate = true}.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This version of {@code replay} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param bufferSize
     *            the buffer size that limits the number of items that can be replayed
     * @return the new {@code ConnectableObservable} instance
     * @throws IllegalArgumentException if {@code bufferSize} is non-positive
     * @see <a href="http://reactivex.io/documentation/operators/replay.html">ReactiveX operators documentation: Replay</a>
     * @see #replay(int, boolean)
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final ConnectableObservable<T> replay(int bufferSize) {
        ObjectHelper.verifyPositive(bufferSize, "bufferSize");
        return ObservableReplay.create(this, bufferSize, false);
    }

    /**
     * Returns a {@link ConnectableObservable} that shares a single subscription to the current {@code Observable} that
     * replays at most {@code bufferSize} items emitted by the current {@code Observable}. A connectable {@code Observable} resembles
     * an ordinary {@code Observable}, except that it does not begin emitting items when it is subscribed to, but only
     * when its {@code connect} method is called.
     * <p>
     * <img width="640" height="445" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/replay.o.n.v3.png" alt="">
     * <p>
     * Note that due to concurrency requirements, {@code replay(bufferSize)} may hold strong references to more than
     * {@code bufferSize} source emissions.
     * To ensure no beyond-bufferSize items are referenced, set {@code eagerTruncate = true}.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This version of {@code replay} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param bufferSize
     *            the buffer size that limits the number of items that can be replayed
     * @param eagerTruncate
     *            if {@code true}, whenever the internal buffer is truncated to the given bufferSize/age, the
     *            oldest item will be guaranteed dereferenced, thus avoiding unexpected retention
     * @return the new {@code ConnectableObservable} instance
     * @throws IllegalArgumentException if {@code bufferSize} is non-positive
     * @see <a href="http://reactivex.io/documentation/operators/replay.html">ReactiveX operators documentation: Replay</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final ConnectableObservable<T> replay(int bufferSize, boolean eagerTruncate) {
        ObjectHelper.verifyPositive(bufferSize, "bufferSize");
        return ObservableReplay.create(this, bufferSize, eagerTruncate);
    }

    /**
     * Returns a {@link ConnectableObservable} that shares a single subscription to the current {@code Observable} and
     * replays at most {@code bufferSize} items that were emitted during a specified time window. A connectable
     * {@code Observable} resembles an ordinary {@code Observable}, except that it does not begin emitting items when it is
     * subscribed to, but only when its {@code connect} method is called.
     * <p>
     * <img width="640" height="445" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/replay.o.nt.v3.png" alt="">
     * <p>
     * Note that due to concurrency requirements, {@code replay(bufferSize)} may hold strong references to more than
     * {@code bufferSize} source emissions.
     * To ensure no out-of-date or beyond-bufferSize items are referenced,
     * use the {@link #replay(int, long, TimeUnit, Scheduler, boolean)} overload with {@code eagerTruncate = true}.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This version of {@code replay} operates by default on the {@code computation} {@link Scheduler}.</dd>
     * </dl>
     *
     * @param bufferSize
     *            the buffer size that limits the number of items that can be replayed
     * @param time
     *            the duration of the window in which the replayed items must have been emitted
     * @param unit
     *            the time unit of {@code time}
     * @return the new {@code ConnectableObservable} instance
     * @throws NullPointerException if {@code unit} is {@code null}
     * @throws IllegalArgumentException if {@code bufferSize} is non-positive
     * @see <a href="http://reactivex.io/documentation/operators/replay.html">ReactiveX operators documentation: Replay</a>
     * @see #replay(int, long, TimeUnit, Scheduler, boolean)
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.COMPUTATION)
    @NonNull
    public final ConnectableObservable<T> replay(int bufferSize, long time, @NonNull TimeUnit unit) {
        return replay(bufferSize, time, unit, Schedulers.computation());
    }

    /**
     * Returns a {@link ConnectableObservable} that shares a single subscription to the current {@code Observable} and
     * that replays a maximum of {@code bufferSize} items that are emitted within a specified time window. A
     * connectable {@code Observable} resembles an ordinary {@code Observable}, except that it does not begin emitting items
     * when it is subscribed to, but only when its {@code connect} method is called.
     * <p>
     * Note that due to concurrency requirements, {@code replay(bufferSize)} may hold strong references to more than
     * {@code bufferSize} source emissions.
     * To ensure no out-of-date or beyond-bufferSize items are referenced,
     * use the {@link #replay(int, long, TimeUnit, Scheduler, boolean)} overload with {@code eagerTruncate = true}.
     * <p>
     * <img width="640" height="445" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/replay.o.nts.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>You specify which {@link Scheduler} this operator will use.</dd>
     * </dl>
     *
     * @param bufferSize
     *            the buffer size that limits the number of items that can be replayed
     * @param time
     *            the duration of the window in which the replayed items must have been emitted
     * @param unit
     *            the time unit of {@code time}
     * @param scheduler
     *            the scheduler that is used as a time source for the window
     * @return the new {@code ConnectableObservable} instance
     * @throws NullPointerException if {@code unit} or {@code scheduler} is {@code null}
     * @throws IllegalArgumentException
     *             if {@code bufferSize} is non-positive
     * @see <a href="http://reactivex.io/documentation/operators/replay.html">ReactiveX operators documentation: Replay</a>
     * @see #replay(int, long, TimeUnit, Scheduler, boolean)
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    @NonNull
    public final ConnectableObservable<T> replay(int bufferSize, long time, @NonNull TimeUnit unit, @NonNull Scheduler scheduler) {
        ObjectHelper.verifyPositive(bufferSize, "bufferSize");
        Objects.requireNonNull(unit, "unit is null");
        Objects.requireNonNull(scheduler, "scheduler is null");
        return ObservableReplay.create(this, time, unit, scheduler, bufferSize, false);
    }

    /**
     * Returns a {@link ConnectableObservable} that shares a single subscription to the current {@code Observable} and
     * that replays a maximum of {@code bufferSize} items that are emitted within a specified time window. A
     * connectable {@code Observable} resembles an ordinary {@code Observable}, except that it does not begin emitting items
     * when it is subscribed to, but only when its {@code connect} method is called.
     * <p>
     * <img width="640" height="445" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/replay.o.nts.v3.png" alt="">
     * <p>
     * Note that due to concurrency requirements, {@code replay(bufferSize)} may hold strong references to more than
     * {@code bufferSize} source emissions.
     * To ensure no out-of-date or beyond-bufferSize items
     * are referenced, set {@code eagerTruncate = true}.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>You specify which {@link Scheduler} this operator will use.</dd>
     * </dl>
     *
     * @param bufferSize
     *            the buffer size that limits the number of items that can be replayed
     * @param time
     *            the duration of the window in which the replayed items must have been emitted
     * @param unit
     *            the time unit of {@code time}
     * @param scheduler
     *            the scheduler that is used as a time source for the window
     * @return the new {@code ConnectableObservable} instance
     * @param eagerTruncate
     *            if {@code true}, whenever the internal buffer is truncated to the given bufferSize/age, the
     *            oldest item will be guaranteed dereferenced, thus avoiding unexpected retention
     * @throws NullPointerException if {@code unit} or {@code scheduler} is {@code null}
     * @throws IllegalArgumentException
     *             if {@code bufferSize} is non-positive
     * @see <a href="http://reactivex.io/documentation/operators/replay.html">ReactiveX operators documentation: Replay</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    @NonNull
    public final ConnectableObservable<T> replay(int bufferSize, long time, @NonNull TimeUnit unit, @NonNull Scheduler scheduler, boolean eagerTruncate) {
        ObjectHelper.verifyPositive(bufferSize, "bufferSize");
        Objects.requireNonNull(unit, "unit is null");
        Objects.requireNonNull(scheduler, "scheduler is null");
        return ObservableReplay.create(this, time, unit, scheduler, bufferSize, eagerTruncate);
    }

    /**
     * Returns a {@link ConnectableObservable} that shares a single subscription to the current {@code Observable} and
     * replays all items emitted by the current {@code Observable} within a specified time window. A connectable {@code Observable}
     * resembles an ordinary {@code Observable}, except that it does not begin emitting items when it is subscribed to,
     * but only when its {@code connect} method is called.
     * <p>
     * <img width="640" height="445" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/replay.o.t.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This version of {@code replay} operates by default on the {@code computation} {@link Scheduler}.</dd>
     * </dl>
     *
     * @param time
     *            the duration of the window in which the replayed items must have been emitted
     * @param unit
     *            the time unit of {@code time}
     * @return the new {@code ConnectableObservable} instance
     * @throws NullPointerException if {@code unit} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/replay.html">ReactiveX operators documentation: Replay</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.COMPUTATION)
    @NonNull
    public final ConnectableObservable<T> replay(long time, @NonNull TimeUnit unit) {
        return replay(time, unit, Schedulers.computation());
    }

    /**
     * Returns a {@link ConnectableObservable} that shares a single subscription to the current {@code Observable} and
     * replays all items emitted by the current {@code Observable} within a specified time window. A connectable {@code Observable}
     * resembles an ordinary {@code Observable}, except that it does not begin emitting items when it is subscribed to,
     * but only when its {@code connect} method is called.
     * <p>
     * <img width="640" height="445" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/replay.o.ts.v3.png" alt="">
     * <p>
     * Note that the internal buffer may retain strong references to the oldest item. To ensure no out-of-date items
     * are referenced, use the {@link #replay(long, TimeUnit, Scheduler, boolean)} overload with {@code eagerTruncate = true}.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>You specify which {@link Scheduler} this operator will use.</dd>
     * </dl>
     *
     * @param time
     *            the duration of the window in which the replayed items must have been emitted
     * @param unit
     *            the time unit of {@code time}
     * @param scheduler
     *            the {@code Scheduler} that is the time source for the window
     * @return the new {@code ConnectableObservable} instance
     * @throws NullPointerException if {@code unit} or {@code scheduler} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/replay.html">ReactiveX operators documentation: Replay</a>
     * @see #replay(long, TimeUnit, Scheduler, boolean)
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    @NonNull
    public final ConnectableObservable<T> replay(long time, @NonNull TimeUnit unit, @NonNull Scheduler scheduler) {
        Objects.requireNonNull(unit, "unit is null");
        Objects.requireNonNull(scheduler, "scheduler is null");
        return ObservableReplay.create(this, time, unit, scheduler, false);
    }

    /**
     * Returns a {@link ConnectableObservable} that shares a single subscription to the current {@code Observable} and
     * replays all items emitted by the current {@code Observable} within a specified time window. A connectable {@code Observable}
     * resembles an ordinary {@code Observable}, except that it does not begin emitting items when it is subscribed to,
     * but only when its {@code connect} method is called.
     * <p>
     * <img width="640" height="445" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/replay.o.ts.v3.png" alt="">
     * <p>
     * Note that the internal buffer may retain strong references to the oldest item. To ensure no out-of-date items
     * are referenced, set {@code eagerTruncate = true}.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>You specify which {@link Scheduler} this operator will use.</dd>
     * </dl>
     *
     * @param time
     *            the duration of the window in which the replayed items must have been emitted
     * @param unit
     *            the time unit of {@code time}
     * @param scheduler
     *            the {@code Scheduler} that is the time source for the window
     * @param eagerTruncate
     *            if {@code true}, whenever the internal buffer is truncated to the given bufferSize/age, the
     *            oldest item will be guaranteed dereferenced, thus avoiding unexpected retention
     * @return the new {@code ConnectableObservable} instance
     * @throws NullPointerException if {@code unit} or {@code scheduler} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/replay.html">ReactiveX operators documentation: Replay</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    @NonNull
    public final ConnectableObservable<T> replay(long time, @NonNull TimeUnit unit, @NonNull Scheduler scheduler, boolean eagerTruncate) {
        Objects.requireNonNull(unit, "unit is null");
        Objects.requireNonNull(scheduler, "scheduler is null");
        return ObservableReplay.create(this, time, unit, scheduler, eagerTruncate);
    }

    /**
     * Returns an {@code Observable} that mirrors the current {@code Observable}, resubscribing to it if it calls {@code onError}
     * (infinite retry count).
     * <p>
     * <img width="640" height="315" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/retry.v3.png" alt="">
     * <p>
     * If the current {@code Observable} calls {@link Observer#onError}, this method will resubscribe to the current
     * {@code Observable} rather than propagating the {@code onError} call.
     * <p>
     * Any and all items emitted by the current {@code Observable} will be emitted by the resulting {@code Observable}, even
     * those emitted during failed subscriptions. For example, if the current {@code Observable} fails at first but emits
     * {@code [1, 2]} then succeeds the second time and emits {@code [1, 2, 3, 4, 5]} then the complete sequence
     * of emissions and notifications would be {@code [1, 2, 1, 2, 3, 4, 5, onComplete]}.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code retry} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @return the new {@code Observable} instance
     * @see <a href="http://reactivex.io/documentation/operators/retry.html">ReactiveX operators documentation: Retry</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Observable<T> retry() {
        return retry(Long.MAX_VALUE, Functions.alwaysTrue());
    }

    /**
     * Returns an {@code Observable} that mirrors the current {@code Observable}, resubscribing to it if it calls {@code onError}
     * and the predicate returns {@code true} for that specific exception and retry count.
     * <p>
     * <img width="640" height="236" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/retry.o.ne.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code retry} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param predicate
     *            the predicate that determines if a resubscription may happen in case of a specific exception
     *            and retry count
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code predicate} is {@code null}
     * @see #retry()
     * @see <a href="http://reactivex.io/documentation/operators/retry.html">ReactiveX operators documentation: Retry</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Observable<T> retry(@NonNull BiPredicate<? super Integer, ? super Throwable> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");

        return RxJavaPlugins.onAssembly(new ObservableRetryBiPredicate<>(this, predicate));
    }

    /**
     * Returns an {@code Observable} that mirrors the current {@code Observable}, resubscribing to it if it calls {@code onError}
     * up to a specified number of retries.
     * <p>
     * <img width="640" height="327" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/retry.o.n.v3.png" alt="">
     * <p>
     * If the current {@code Observable} calls {@link Observer#onError}, this method will resubscribe to the current
     * {@code Observable} for a maximum of {@code count} resubscriptions rather than propagating the
     * {@code onError} call.
     * <p>
     * Any and all items emitted by the current {@code Observable} will be emitted by the resulting {@code Observable}, even
     * those emitted during failed subscriptions. For example, if the current {@code Observable} fails at first but emits
     * {@code [1, 2]} then succeeds the second time and emits {@code [1, 2, 3, 4, 5]} then the complete sequence
     * of emissions and notifications would be {@code [1, 2, 1, 2, 3, 4, 5, onComplete]}.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code retry} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param times
     *            the number of times to resubscribe if the current {@code Observable} fails
     * @return the new {@code Observable} instance
     * @throws IllegalArgumentException if {@code times} is negative
     * @see <a href="http://reactivex.io/documentation/operators/retry.html">ReactiveX operators documentation: Retry</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Observable<T> retry(long times) {
        return retry(times, Functions.alwaysTrue());
    }

    /**
     * Retries at most times or until the predicate returns {@code false}, whichever happens first.
     * <p>
     * <img width="640" height="270" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/retry.o.nfe.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code retry} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param times the number of times to resubscribe if the current {@code Observable} fails
     * @param predicate the predicate called with the failure {@link Throwable} and should return {@code true} to trigger a retry.
     * @throws NullPointerException if {@code predicate} is {@code null}
     * @throws IllegalArgumentException if {@code times} is negative
     * @return the new {@code Observable} instance
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Observable<T> retry(long times, @NonNull Predicate<? super Throwable> predicate) {
        if (times < 0) {
            throw new IllegalArgumentException("times >= 0 required but it was " + times);
        }
        Objects.requireNonNull(predicate, "predicate is null");

        return RxJavaPlugins.onAssembly(new ObservableRetryPredicate<>(this, times, predicate));
    }

    /**
     * Retries the current {@code Observable} if the predicate returns {@code true}.
     * <p>
     * <img width="640" height="249" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/retry.o.e.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code retry} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param predicate the predicate that receives the failure {@link Throwable} and should return {@code true} to trigger a retry.
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code predicate} is {@code null}
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Observable<T> retry(@NonNull Predicate<? super Throwable> predicate) {
        return retry(Long.MAX_VALUE, predicate);
    }

    /**
     * Retries until the given stop function returns {@code true}.
     * <p>
     * <img width="640" height="262" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/retryUntil.o.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code retryUntil} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param stop the function that should return {@code true} to stop retrying
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code stop} is {@code null}
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Observable<T> retryUntil(@NonNull BooleanSupplier stop) {
        Objects.requireNonNull(stop, "stop is null");
        return retry(Long.MAX_VALUE, Functions.predicateReverseFor(stop));
    }

    /**
     * Returns an {@code Observable} that emits the same values as the current {@code Observable} with the exception of an
     * {@code onError}. An {@code onError} notification from the source will result in the emission of a
     * {@link Throwable} item to the {@code Observable} provided as an argument to the {@code notificationHandler}
     * function. If that {@code Observable} calls {@code onComplete} or {@code onError} then {@code retry} will call
     * {@code onComplete} or {@code onError} on the child subscription. Otherwise, the current {@code Observable}
     * will be resubscribed.
     * <p>
     * <img width="640" height="430" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/retryWhen.f.v3.png" alt="">
     * <p>
     * Example:
     *
     * This retries 3 times, each time incrementing the number of seconds it waits.
     *
     * <pre><code>
     *  Observable.create((ObservableEmitter&lt;? super String&gt; s) -&gt; {
     *      System.out.println("subscribing");
     *      s.onError(new RuntimeException("always fails"));
     *  }).retryWhen(attempts -&gt; {
     *      return attempts.zipWith(Observable.range(1, 3), (n, i) -&gt; i).flatMap(i -&gt; {
     *          System.out.println("delay retry by " + i + " second(s)");
     *          return Observable.timer(i, TimeUnit.SECONDS);
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
     * Note that the inner {@link ObservableSource} returned by the handler function should signal
     * either {@code onNext}, {@code onError} or {@code onComplete} in response to the received
     * {@code Throwable} to indicate the operator should retry or terminate. If the upstream to
     * the operator is asynchronous, signaling {@code onNext} followed by {@code onComplete} immediately may
     * result in the sequence to be completed immediately. Similarly, if this inner
     * {@code ObservableSource} signals {@code onError} or {@code onComplete} while the upstream is
     * active, the sequence is terminated with the same signal immediately.
     * <p>
     * The following example demonstrates how to retry an asynchronous source with a delay:
     * <pre><code>
     * Observable.timer(1, TimeUnit.SECONDS)
     *     .doOnSubscribe(s -&gt; System.out.println("subscribing"))
     *     .map(v -&gt; { throw new RuntimeException(); })
     *     .retryWhen(errors -&gt; {
     *         AtomicInteger counter = new AtomicInteger();
     *         return errors
     *                   .takeWhile(e -&gt; counter.getAndIncrement() != 3)
     *                   .flatMap(e -&gt; {
     *                       System.out.println("delay retry by " + counter.get() + " second(s)");
     *                       return Observable.timer(counter.get(), TimeUnit.SECONDS);
     *                   });
     *     })
     *     .blockingSubscribe(System.out::println, System.out::println);
     * </code></pre>
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code retryWhen} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param handler
     *            receives an {@code Observable} of notifications with which a user can complete or error, aborting the
     *            retry
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code handler} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/retry.html">ReactiveX operators documentation: Retry</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Observable<T> retryWhen(
            @NonNull Function<? super Observable<Throwable>, ? extends ObservableSource<?>> handler) {
        Objects.requireNonNull(handler, "handler is null");
        return RxJavaPlugins.onAssembly(new ObservableRetryWhen<>(this, handler));
    }

    /**
     * Subscribes to the current {@code Observable} and wraps the given {@link Observer} into a {@link SafeObserver}
     * (if not already a {@code SafeObserver}) that
     * deals with exceptions thrown by a misbehaving {@code Observer} (that doesn't follow the
     * <em>Reactive Streams</em> specification).
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code safeSubscribe} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param observer the incoming {@code Observer} instance
     * @throws NullPointerException if {@code observer} is {@code null}
     */
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final void safeSubscribe(@NonNull Observer<? super T> observer) {
        Objects.requireNonNull(observer, "observer is null");
        if (observer instanceof SafeObserver) {
            subscribe(observer);
        } else {
            subscribe(new SafeObserver<>(observer));
        }
    }

    /**
     * Returns an {@code Observable} that emits the most recently emitted item (if any) emitted by the current {@code Observable}
     * within periodic time intervals.
     * <p>
     * <img width="640" height="305" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/sample.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code sample} operates by default on the {@code computation} {@link Scheduler}.</dd>
     * </dl>
     *
     * @param period
     *            the sampling rate
     * @param unit
     *            the {@link TimeUnit} in which {@code period} is defined
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code unit} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/sample.html">ReactiveX operators documentation: Sample</a>
     * @see #throttleLast(long, TimeUnit)
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.COMPUTATION)
    @NonNull
    public final Observable<T> sample(long period, @NonNull TimeUnit unit) {
        return sample(period, unit, Schedulers.computation());
    }

    /**
     * Returns an {@code Observable} that emits the most recently emitted item (if any) emitted by the current {@code Observable}
     * within periodic time intervals and optionally emit the very last upstream item when the upstream completes.
     * <p>
     * <img width="640" height="277" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/sample.emitlast.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code sample} operates by default on the {@code computation} {@link Scheduler}.</dd>
     * </dl>
     *
     * <p>History: 2.0.5 - experimental
     * @param period
     *            the sampling rate
     * @param unit
     *            the {@link TimeUnit} in which {@code period} is defined
     * @param emitLast
     *            if {@code true} and the upstream completes while there is still an unsampled item available,
     *            that item is emitted to downstream before completion
     *            if {@code false}, an unsampled last item is ignored.
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code unit} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/sample.html">ReactiveX operators documentation: Sample</a>
     * @see #throttleLast(long, TimeUnit)
     * @since 2.1
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.COMPUTATION)
    @NonNull
    public final Observable<T> sample(long period, @NonNull TimeUnit unit, boolean emitLast) {
        return sample(period, unit, Schedulers.computation(), emitLast);
    }

    /**
     * Returns an {@code Observable} that emits the most recently emitted item (if any) emitted by the current {@code Observable}
     * within periodic time intervals, where the intervals are defined on a particular {@link Scheduler}.
     * <p>
     * <img width="640" height="305" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/sample.s.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>You specify which {@code Scheduler} this operator will use.</dd>
     * </dl>
     *
     * @param period
     *            the sampling rate
     * @param unit
     *            the {@link TimeUnit} in which {@code period} is defined
     * @param scheduler
     *            the {@code Scheduler} to use when sampling
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code unit} or {@code scheduler} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/sample.html">ReactiveX operators documentation: Sample</a>
     * @see #throttleLast(long, TimeUnit, Scheduler)
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    @NonNull
    public final Observable<T> sample(long period, @NonNull TimeUnit unit, @NonNull Scheduler scheduler) {
        Objects.requireNonNull(unit, "unit is null");
        Objects.requireNonNull(scheduler, "scheduler is null");
        return RxJavaPlugins.onAssembly(new ObservableSampleTimed<>(this, period, unit, scheduler, false));
    }

    /**
     * Returns an {@code Observable} that emits the most recently emitted item (if any) emitted by the current {@code Observable}
     * within periodic time intervals, where the intervals are defined on a particular {@link Scheduler}
     *  and optionally emit the very last upstream item when the upstream completes.
     * <p>
     * <img width="640" height="277" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/sample.s.emitlast.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>You specify which {@code Scheduler} this operator will use.</dd>
     * </dl>
     *
     * <p>History: 2.0.5 - experimental
     * @param period
     *            the sampling rate
     * @param unit
     *            the {@link TimeUnit} in which {@code period} is defined
     * @param scheduler
     *            the {@code Scheduler} to use when sampling
     * @param emitLast
     *            if {@code true} and the upstream completes while there is still an unsampled item available,
     *            that item is emitted to downstream before completion
     *            if {@code false}, an unsampled last item is ignored.
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code unit} or {@code scheduler} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/sample.html">ReactiveX operators documentation: Sample</a>
     * @see #throttleLast(long, TimeUnit, Scheduler)
     * @since 2.1
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    @NonNull
    public final Observable<T> sample(long period, @NonNull TimeUnit unit, @NonNull Scheduler scheduler, boolean emitLast) {
        Objects.requireNonNull(unit, "unit is null");
        Objects.requireNonNull(scheduler, "scheduler is null");
        return RxJavaPlugins.onAssembly(new ObservableSampleTimed<>(this, period, unit, scheduler, emitLast));
    }

    /**
     * Returns an {@code Observable} that, when the specified {@code sampler} {@link ObservableSource} emits an item or completes,
     * emits the most recently emitted item (if any) emitted by the current {@code Observable} since the previous
     * emission from the {@code sampler} {@code ObservableSource}.
     * <p>
     * <img width="640" height="290" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/sample.o.nolast.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This version of {@code sample} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <U> the element type of the sampler {@code ObservableSource}
     * @param sampler
     *            the {@code ObservableSource} to use for sampling the current {@code Observable}
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code sampler} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/sample.html">ReactiveX operators documentation: Sample</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <@NonNull U> Observable<T> sample(@NonNull ObservableSource<U> sampler) {
        Objects.requireNonNull(sampler, "sampler is null");
        return RxJavaPlugins.onAssembly(new ObservableSampleWithObservable<>(this, sampler, false));
    }

    /**
     * Returns an {@code Observable} that, when the specified {@code sampler} {@link ObservableSource} emits an item or completes,
     * emits the most recently emitted item (if any) emitted by the current {@code Observable} since the previous
     * emission from the {@code sampler} {@code ObservableSource}
     * and optionally emit the very last upstream item when the upstream or other {@code ObservableSource} complete.
     * <p>
     * <img width="640" height="290" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/sample.o.emitlast.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This version of {@code sample} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * <p>History: 2.0.5 - experimental
     * @param <U> the element type of the sampler {@code ObservableSource}
     * @param sampler
     *            the {@code ObservableSource} to use for sampling the current {@code Observable}
     * @param emitLast
     *            if {@code true} and the upstream completes while there is still an unsampled item available,
     *            that item is emitted to downstream before completion
     *            if {@code false}, an unsampled last item is ignored.
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code sampler} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/sample.html">ReactiveX operators documentation: Sample</a>
     * @since 2.1
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <@NonNull U> Observable<T> sample(@NonNull ObservableSource<U> sampler, boolean emitLast) {
        Objects.requireNonNull(sampler, "sampler is null");
        return RxJavaPlugins.onAssembly(new ObservableSampleWithObservable<>(this, sampler, emitLast));
    }

    /**
     * Returns an {@code Observable} that emits the first value emitted by the current {@code Observable}, then emits one value
     * for each subsequent value emitted by the current {@code Observable}. Each emission after the first is the result of
     * applying the specified accumulator function to the previous emission and the corresponding value from the current {@code Observable}.
     * <p>
     * <img width="640" height="320" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/scan.v3.png" alt="">
     * <p>
     * This sort of function is sometimes called an accumulator.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code scan} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param accumulator
     *            an accumulator function to be invoked on each item emitted by the current {@code Observable}, whose
     *            result will be emitted to {@link Observer}s via {@link Observer#onNext onNext} and used in the
     *            next accumulator call
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code accumulator} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/scan.html">ReactiveX operators documentation: Scan</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Observable<T> scan(@NonNull BiFunction<T, T, T> accumulator) {
        Objects.requireNonNull(accumulator, "accumulator is null");
        return RxJavaPlugins.onAssembly(new ObservableScan<>(this, accumulator));
    }

    /**
     * Returns an {@code Observable} that emits the provided initial (seed) value, then emits one value for each value emitted
     * by the current {@code Observable}. Each emission after the first is the result of applying the specified accumulator
     * function to the previous emission and the corresponding value from the current {@code Observable}.
     * <p>
     * <img width="640" height="320" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/scanSeed.v3.png" alt="">
     * <p>
     * This sort of function is sometimes called an accumulator.
     * <p>
     * Note that the {@code Observable} that results from this method will emit {@code initialValue} as its first
     * emitted item.
     * <p>
     * Note that the {@code initialValue} is shared among all subscribers to the resulting {@code Observable}
     * and may cause problems if it is mutable. To make sure each subscriber gets its own value, defer
     * the application of this operator via {@link #defer(Supplier)}:
     * <pre><code>
     * ObservableSource&lt;T&gt; source = ...
     * Observable.defer(() -&gt; source.scan(new ArrayList&lt;&gt;(), (list, item) -&gt; list.add(item)));
     *
     * // alternatively, by using compose to stay fluent
     *
     * source.compose(o -&gt;
     *     Observable.defer(() -&gt; o.scan(new ArrayList&lt;&gt;(), (list, item) -&gt; list.add(item)))
     * );
     * </code></pre>
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code scan} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <R> the initial, accumulator and result type
     * @param initialValue
     *            the initial (seed) accumulator item
     * @param accumulator
     *            an accumulator function to be invoked on each item emitted by the current {@code Observable}, whose
     *            result will be emitted to {@link Observer}s via {@link Observer#onNext onNext} and used in the
     *            next accumulator call
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code initialValue} or {@code accumulator} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/scan.html">ReactiveX operators documentation: Scan</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <@NonNull R> Observable<R> scan(@NonNull R initialValue, @NonNull BiFunction<R, ? super T, R> accumulator) {
        Objects.requireNonNull(initialValue, "initialValue is null");
        return scanWith(Functions.justSupplier(initialValue), accumulator);
    }

    /**
     * Returns an {@code Observable} that emits the provided initial (seed) value, then emits one value for each value emitted
     * by the current {@code Observable}. Each emission after the first is the result of applying the specified accumulator
     * function to the previous emission and the corresponding value from the current {@code Observable}.
     * <p>
     * <img width="640" height="320" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/scanSeed.v3.png" alt="">
     * <p>
     * This sort of function is sometimes called an accumulator.
     * <p>
     * Note that the {@code Observable} that results from this method will emit the value returned
     * by the {@code seedSupplier} as its first item.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code scanWith} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <R> the initial, accumulator and result type
     * @param seedSupplier
     *            a {@link Supplier} that returns the initial (seed) accumulator item for each individual {@link Observer}
     * @param accumulator
     *            an accumulator function to be invoked on each item emitted by the current {@code Observable}, whose
     *            result will be emitted to {@code Observer}s via {@link Observer#onNext onNext} and used in the
     *            next accumulator call
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code seedSupplier} or {@code accumulator} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/scan.html">ReactiveX operators documentation: Scan</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <@NonNull R> Observable<R> scanWith(@NonNull Supplier<R> seedSupplier, @NonNull BiFunction<R, ? super T, R> accumulator) {
        Objects.requireNonNull(seedSupplier, "seedSupplier is null");
        Objects.requireNonNull(accumulator, "accumulator is null");
        return RxJavaPlugins.onAssembly(new ObservableScanSeed<>(this, seedSupplier, accumulator));
    }

    /**
     * Forces the current {@code Observable}'s emissions and notifications to be serialized and for it to obey
     * <a href="http://reactivex.io/documentation/contract.html">the {@code ObservableSource} contract</a> in other ways.
     * <p>
     * It is possible for an {@code Observable} to invoke its {@link Observer}s' methods asynchronously, perhaps from
     * different threads. This could make such an {@code Observable} poorly-behaved, in that it might try to invoke
     * {@code onComplete} or {@code onError} before one of its {@code onNext} invocations, or it might call
     * {@code onNext} from two different threads concurrently. You can force such an {@code Observable} to be
     * well-behaved and sequential by applying the {@code serialize} method to it.
     * <p>
     * <img width="640" height="400" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/synchronize.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code serialize} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @return the new {@code Observable} instance
     * @see <a href="http://reactivex.io/documentation/operators/serialize.html">ReactiveX operators documentation: Serialize</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Observable<T> serialize() {
        return RxJavaPlugins.onAssembly(new ObservableSerialized<>(this));
    }

    /**
     * Returns a new {@code Observable} that multicasts (and shares a single subscription to) the current {@code Observable}. As long as
     * there is at least one {@link Observer}, the current {@code Observable} will stay subscribed and keep emitting signals.
     * When all observers have disposed, the operator will dispose the subscription to the current {@code Observable}.
     * <p>
     * This is an alias for {@link #publish()}.{@link ConnectableObservable#refCount() refCount()}.
     * <p>
     * <img width="640" height="510" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/publishRefCount.o.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code share} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @return the new {@code Observable} instance
     * @see <a href="http://reactivex.io/documentation/operators/refcount.html">ReactiveX operators documentation: RefCount</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Observable<T> share() {
        return publish().refCount();
    }

    /**
     * Returns a {@link Maybe} that completes if the current {@code Observable} is empty or emits the single item
     * emitted by the current {@code Observable}, or signals an {@link IllegalArgumentException} if the current
     * {@code Observable} emits more than one item.
     * <p>
     * <img width="640" height="217" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/singleElement.o.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code singleElement} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @return the new {@code Maybe} instance
     * @see <a href="http://reactivex.io/documentation/operators/first.html">ReactiveX operators documentation: First</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Maybe<T> singleElement() {
        return RxJavaPlugins.onAssembly(new ObservableSingleMaybe<>(this));
    }

    /**
     * Returns a {@link Single} that emits the single item emitted by the current {@code Observable}, if the current {@code Observable}
     * emits only a single item, or a default item if the current {@code Observable} emits no items. If the current
     * {@code Observable} emits more than one item, an {@link IllegalArgumentException} is signaled instead.
     * <p>
     * <img width="640" height="315" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/single.2.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code single} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param defaultItem
     *            a default value to emit if the current {@code Observable} emits no item
     * @return the new {@code Single} instance
     * @throws NullPointerException if {@code defaultItem} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/first.html">ReactiveX operators documentation: First</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Single<T> single(@NonNull T defaultItem) {
        Objects.requireNonNull(defaultItem, "defaultItem is null");
        return RxJavaPlugins.onAssembly(new ObservableSingleSingle<>(this, defaultItem));
    }

    /**
     * Returns a {@link Single} that emits the single item emitted by the current {@code Observable} if it
     * emits only a single item, otherwise
     * if the current {@code Observable} completes without emitting any items or emits more than one item a
     * {@link NoSuchElementException} or {@link IllegalArgumentException} will be signaled respectively.
     * <p>
     * <img width="640" height="206" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/singleOrError.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code singleOrError} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @return the new {@code Single} instance
     * @see <a href="http://reactivex.io/documentation/operators/first.html">ReactiveX operators documentation: First</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Single<T> singleOrError() {
        return RxJavaPlugins.onAssembly(new ObservableSingleSingle<>(this, null));
    }

    /**
     * Returns an {@code Observable} that skips the first {@code count} items emitted by the current {@code Observable} and emits
     * the remainder.
     * <p>
     * <img width="640" height="305" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/skip.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This version of {@code skip} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param count
     *            the number of items to skip
     * @return the new {@code Observable} instance
     * @throws IllegalArgumentException if {@code count} is negative
     * @see <a href="http://reactivex.io/documentation/operators/skip.html">ReactiveX operators documentation: Skip</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Observable<T> skip(long count) {
        if (count < 0) {
            throw new IllegalArgumentException("count >= 0 expected but it was " + count);
        }
        if (count == 0) {
            return RxJavaPlugins.onAssembly(this);
        }
        return RxJavaPlugins.onAssembly(new ObservableSkip<>(this, count));
    }

    /**
     * Returns an {@code Observable} that skips values emitted by the current {@code Observable} before a specified time window
     * elapses.
     * <p>
     * <img width="640" height="305" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/skip.t.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code skip} does not operate on any particular scheduler but uses the current time
     *  from the {@code computation} {@link Scheduler}.</dd>
     * </dl>
     *
     * @param time
     *            the length of the time window to skip
     * @param unit
     *            the time unit of {@code time}
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code unit} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/skip.html">ReactiveX operators documentation: Skip</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.COMPUTATION)
    @NonNull
    public final Observable<T> skip(long time, @NonNull TimeUnit unit) {
        return skipUntil(timer(time, unit));
    }

    /**
     * Returns an {@code Observable} that skips values emitted by the current {@code Observable} before a specified time window
     * on a specified {@link Scheduler} elapses.
     * <p>
     * <img width="640" height="305" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/skip.ts.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>You specify which {@code Scheduler} this operator will use for the timed skipping</dd>
     * </dl>
     *
     * @param time
     *            the length of the time window to skip
     * @param unit
     *            the time unit of {@code time}
     * @param scheduler
     *            the {@code Scheduler} on which the timed wait happens
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code unit} or {@code scheduler} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/skip.html">ReactiveX operators documentation: Skip</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    @NonNull
    public final Observable<T> skip(long time, @NonNull TimeUnit unit, @NonNull Scheduler scheduler) {
        return skipUntil(timer(time, unit, scheduler));
    }

    /**
     * Returns an {@code Observable} that drops a specified number of items from the end of the sequence emitted by the
     * current {@code Observable}.
     * <p>
     * <img width="640" height="305" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/skipLast.v3.png" alt="">
     * <p>
     * This {@link Observer} accumulates a queue long enough to store the first {@code count} items. As more items are
     * received, items are taken from the front of the queue and emitted by the returned {@code Observable}. This causes
     * such items to be delayed.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This version of {@code skipLast} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param count
     *            number of items to drop from the end of the source sequence
     * @return the new {@code Observable} instance
     * @throws IllegalArgumentException
     *             if {@code count} is negative
     * @see <a href="http://reactivex.io/documentation/operators/skiplast.html">ReactiveX operators documentation: SkipLast</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Observable<T> skipLast(int count) {
        if (count < 0) {
            throw new IllegalArgumentException("count >= 0 required but it was " + count);
        }
        if (count == 0) {
            return RxJavaPlugins.onAssembly(this);
        }
        return RxJavaPlugins.onAssembly(new ObservableSkipLast<>(this, count));
    }

    /**
     * Returns an {@code Observable} that drops items emitted by the current {@code Observable} during a specified time window
     * before the source completes.
     * <p>
     * <img width="640" height="305" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/skipLast.t.v3.png" alt="">
     * <p>
     * Note: this action will cache the latest items arriving in the specified time window.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code skipLast} does not operate on any particular scheduler but uses the current time
     *  from the {@code trampoline} {@link Scheduler}.</dd>
     * </dl>
     *
     * @param time
     *            the length of the time window
     * @param unit
     *            the time unit of {@code time}
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code unit} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/skiplast.html">ReactiveX operators documentation: SkipLast</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.TRAMPOLINE)
    @NonNull
    public final Observable<T> skipLast(long time, @NonNull TimeUnit unit) {
        return skipLast(time, unit, Schedulers.trampoline(), false, bufferSize());
    }

    /**
     * Returns an {@code Observable} that drops items emitted by the current {@code Observable} during a specified time window
     * before the source completes.
     * <p>
     * <img width="640" height="305" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/skipLast.t.v3.png" alt="">
     * <p>
     * Note: this action will cache the latest items arriving in the specified time window.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code skipLast} does not operate on any particular scheduler but uses the current time
     *  from the {@code computation} {@link Scheduler}.</dd>
     * </dl>
     *
     * @param time
     *            the length of the time window
     * @param unit
     *            the time unit of {@code time}
     * @param delayError
     *            if {@code true}, an exception signaled by the current {@code Observable} is delayed until the regular elements are consumed
     *            by the downstream; if {@code false}, an exception is immediately signaled and all regular elements dropped
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code unit} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/skiplast.html">ReactiveX operators documentation: SkipLast</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.TRAMPOLINE)
    @NonNull
    public final Observable<T> skipLast(long time, @NonNull TimeUnit unit, boolean delayError) {
        return skipLast(time, unit, Schedulers.trampoline(), delayError, bufferSize());
    }

    /**
     * Returns an {@code Observable} that drops items emitted by the current {@code Observable} during a specified time window
     * (defined on a specified scheduler) before the source completes.
     * <p>
     * <img width="640" height="340" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/skipLast.ts.v3.png" alt="">
     * <p>
     * Note: this action will cache the latest items arriving in the specified time window.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>You specify which {@link Scheduler} this operator will use for tracking the current time</dd>
     * </dl>
     *
     * @param time
     *            the length of the time window
     * @param unit
     *            the time unit of {@code time}
     * @param scheduler
     *            the scheduler used as the time source
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code unit} or {@code scheduler} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/skiplast.html">ReactiveX operators documentation: SkipLast</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    @NonNull
    public final Observable<T> skipLast(long time, @NonNull TimeUnit unit, @NonNull Scheduler scheduler) {
        return skipLast(time, unit, scheduler, false, bufferSize());
    }

    /**
     * Returns an {@code Observable} that drops items emitted by the current {@code Observable} during a specified time window
     * (defined on a specified scheduler) before the source completes.
     * <p>
     * <img width="640" height="340" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/skipLast.ts.v3.png" alt="">
     * <p>
     * Note: this action will cache the latest items arriving in the specified time window.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>You specify which {@link Scheduler} this operator will use to track the current time</dd>
     * </dl>
     *
     * @param time
     *            the length of the time window
     * @param unit
     *            the time unit of {@code time}
     * @param scheduler
     *            the scheduler used as the time source
     * @param delayError
     *            if {@code true}, an exception signaled by the current {@code Observable} is delayed until the regular elements are consumed
     *            by the downstream; if {@code false}, an exception is immediately signaled and all regular elements dropped
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code unit} or {@code scheduler} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/skiplast.html">ReactiveX operators documentation: SkipLast</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    @NonNull
    public final Observable<T> skipLast(long time, @NonNull TimeUnit unit, @NonNull Scheduler scheduler, boolean delayError) {
        return skipLast(time, unit, scheduler, delayError, bufferSize());
    }

    /**
     * Returns an {@code Observable} that drops items emitted by the current {@code Observable} during a specified time window
     * (defined on a specified scheduler) before the source completes.
     * <p>
     * <img width="640" height="340" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/skipLast.ts.v3.png" alt="">
     * <p>
     * Note: this action will cache the latest items arriving in the specified time window.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>You specify which {@link Scheduler} this operator will use.</dd>
     * </dl>
     *
     * @param time
     *            the length of the time window
     * @param unit
     *            the time unit of {@code time}
     * @param scheduler
     *            the scheduler used as the time source
     * @param delayError
     *            if {@code true}, an exception signaled by the current {@code Observable} is delayed until the regular elements are consumed
     *            by the downstream; if {@code false}, an exception is immediately signaled and all regular elements dropped
     * @param bufferSize
     *            the hint about how many elements to expect to be skipped
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code unit} or {@code scheduler} is {@code null}
     * @throws IllegalArgumentException if {@code bufferSize} is non-positive
     * @see <a href="http://reactivex.io/documentation/operators/skiplast.html">ReactiveX operators documentation: SkipLast</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    @NonNull
    public final Observable<T> skipLast(long time, @NonNull TimeUnit unit, @NonNull Scheduler scheduler, boolean delayError, int bufferSize) {
        Objects.requireNonNull(unit, "unit is null");
        Objects.requireNonNull(scheduler, "scheduler is null");
        ObjectHelper.verifyPositive(bufferSize, "bufferSize");
        // the internal buffer holds pairs of (timestamp, value) so double the default buffer size
        int s = bufferSize << 1;
        return RxJavaPlugins.onAssembly(new ObservableSkipLastTimed<>(this, time, unit, scheduler, s, delayError));
    }

    /**
     * Returns an {@code Observable} that skips items emitted by the current {@code Observable} until a second {@link ObservableSource} emits
     * an item.
     * <p>
     * <img width="640" height="375" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/skipUntil.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code skipUntil} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <U> the element type of the other {@code ObservableSource}
     * @param other
     *            the second {@code ObservableSource} that has to emit an item before the current {@code Observable}'s elements begin
     *            to be mirrored by the resulting {@code Observable}
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code other} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/skipuntil.html">ReactiveX operators documentation: SkipUntil</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <@NonNull U> Observable<T> skipUntil(@NonNull ObservableSource<U> other) {
        Objects.requireNonNull(other, "other is null");
        return RxJavaPlugins.onAssembly(new ObservableSkipUntil<>(this, other));
    }

    /**
     * Returns an {@code Observable} that skips all items emitted by the current {@code Observable} as long as a specified
     * condition holds {@code true}, but emits all further source items as soon as the condition becomes {@code false}.
     * <p>
     * <img width="640" height="305" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/skipWhile.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code skipWhile} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param predicate
     *            a function to test each item emitted from the current {@code Observable}
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code predicate} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/skipwhile.html">ReactiveX operators documentation: SkipWhile</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Observable<T> skipWhile(@NonNull Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        return RxJavaPlugins.onAssembly(new ObservableSkipWhile<>(this, predicate));
    }

    /**
     * Returns an {@code Observable} that emits the events emitted by the current {@code Observable}, in a
     * sorted order. Each item emitted by the current {@code Observable} must implement {@link Comparable} with respect to all
     * other items in the sequence.
     * <p>
     * <img width="640" height="315" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/sorted.png" alt="">
     * <p>
     * If any item emitted by the current {@code Observable} does not implement {@code Comparable} with respect to
     * all other items emitted by the current {@code Observable}, no items will be emitted and the
     * sequence is terminated with a {@link ClassCastException}.
     *
     * <p>Note that calling {@code sorted} with long, non-terminating or infinite sources
     * might cause {@link OutOfMemoryError}
     *
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code sorted} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @return the new {@code Observable} instance
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Observable<T> sorted() {
        return toList().toObservable().map(Functions.listSorter(Functions.naturalComparator())).flatMapIterable(Functions.identity());
    }

    /**
     * Returns an {@code Observable} that emits the events emitted by the current {@code Observable}, in a
     * sorted order based on a specified comparison function.
     *
     * <p>Note that calling {@code sorted} with long, non-terminating or infinite sources
     * might cause {@link OutOfMemoryError}
     *
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code sorted} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param comparator
     *            a function that compares two items emitted by the current {@code Observable} and returns an {@code int}
     *            that indicates their sort order
     * @throws NullPointerException if {@code comparator} is {@code null}
     * @return the new {@code Observable} instance
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Observable<T> sorted(@NonNull Comparator<? super T> comparator) {
        Objects.requireNonNull(comparator, "comparator is null");
        return toList().toObservable().map(Functions.listSorter(comparator)).flatMapIterable(Functions.identity());
    }

    /**
     * Returns an {@code Observable} that emits the items in a specified {@link Iterable} before it begins to emit items
     * emitted by the current {@code Observable}.
     * <p>
     * <img width="640" height="315" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/startWith.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code startWithIterable} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param items
     *            an {@code Iterable} that contains the items you want the resulting {@code Observable} to emit first
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code items} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/startwith.html">ReactiveX operators documentation: StartWith</a>
     * @since 3.0.0
     * @see #startWithItem(Object)
     * @see #startWithArray(Object...)
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Observable<T> startWithIterable(@NonNull Iterable<? extends T> items) {
        return concatArray(fromIterable(items), this);
    }

    /**
     * Returns an {@code Observable} which first runs the other {@link CompletableSource}
     * then the current {@code Observable} if the other completed normally.
     * <p>
     * <img width="640" height="268" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Observable.startWith.c.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code startWith} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param other the other {@code CompletableSource} to run first
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code other} is {@code null}
     * @since 3.0.0
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Observable<T> startWith(@NonNull CompletableSource other) {
        Objects.requireNonNull(other, "other is null");
        return Observable.concat(Completable.wrap(other).<T>toObservable(), this);
    }

    /**
     * Returns an {@code Observable} which first runs the other {@link SingleSource}
     * then the current {@code Observable} if the other succeeded normally.
     * <p>
     * <img width="640" height="248" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Observable.startWith.s.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code startWith} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param other the other {@code SingleSource} to run first
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code other} is {@code null}
     * @since 3.0.0
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Observable<T> startWith(@NonNull SingleSource<T> other) {
        Objects.requireNonNull(other, "other is null");
        return Observable.concat(Single.wrap(other).toObservable(), this);
    }

    /**
     * Returns an {@code Observable} which first runs the other {@link MaybeSource}
     * then the current {@code Observable} if the other succeeded or completed normally.
     * <p>
     * <img width="640" height="168" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Observable.startWith.m.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code startWith} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param other the other {@code MaybeSource} to run first
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code other} is {@code null}
     * @since 3.0.0
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Observable<T> startWith(@NonNull MaybeSource<T> other) {
        Objects.requireNonNull(other, "other is null");
        return Observable.concat(Maybe.wrap(other).toObservable(), this);
    }

    /**
     * Returns an {@code Observable} that emits the items in a specified {@link ObservableSource} before it begins to emit
     * items emitted by the current {@code Observable}.
     * <p>
     * <img width="640" height="315" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/startWith.o.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code startWith} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param other
     *            an {@code ObservableSource} that contains the items you want the modified {@code ObservableSource} to emit first
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code other} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/startwith.html">ReactiveX operators documentation: StartWith</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Observable<T> startWith(@NonNull ObservableSource<? extends T> other) {
        Objects.requireNonNull(other, "other is null");
        return concatArray(other, this);
    }

    /**
     * Returns an {@code Observable} that emits a specified item before it begins to emit items emitted by the current
     * {@code Observable}.
     * <p>
     * <img width="640" height="315" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/startWith.item.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code startWithItem} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param item
     *            the item to emit first
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code item} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/startwith.html">ReactiveX operators documentation: StartWith</a>
     * @see #startWithArray(Object...)
     * @see #startWithIterable(Iterable)
     * @since 3.0.0
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Observable<T> startWithItem(@NonNull T item) {
        return concatArray(just(item), this);
    }

    /**
     * Returns an {@code Observable} that emits the specified items before it begins to emit items emitted by the current
     * {@code Observable}.
     * <p>
     * <img width="640" height="315" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/startWithArray.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code startWithArray} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param items
     *            the array of values to emit first
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code items} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/startwith.html">ReactiveX operators documentation: StartWith</a>
     * @see #startWithItem(Object)
     * @see #startWithIterable(Iterable)
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @SafeVarargs
    @NonNull
    public final Observable<T> startWithArray(@NonNull T... items) {
        Observable<T> fromArray = fromArray(items);
        if (fromArray == empty()) {
            return RxJavaPlugins.onAssembly(this);
        }
        return concatArray(fromArray, this);
    }

    /**
     * Subscribes to the current {@code Observable} and ignores {@code onNext} and {@code onComplete} emissions.
     * <p>
     * If the {@code Observable} emits an error, it is wrapped into an
     * {@link OnErrorNotImplementedException}
     * and routed to the {@link RxJavaPlugins#onError(Throwable)} handler.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code subscribe} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @return the new {@link Disposable} instance that can be used to dispose the subscription at any time
     * @see <a href="http://reactivex.io/documentation/operators/subscribe.html">ReactiveX operators documentation: Subscribe</a>
     * @see #subscribe(Consumer, Consumer, Action, DisposableContainer)
     */
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Disposable subscribe() {
        return subscribe(Functions.emptyConsumer(), Functions.ON_ERROR_MISSING, Functions.EMPTY_ACTION);
    }

    /**
     * Subscribes to the current {@code Observable} and provides a callback to handle the items it emits.
     * <p>
     * If the {@code Observable} emits an error, it is wrapped into an
     * {@link OnErrorNotImplementedException}
     * and routed to the {@link RxJavaPlugins#onError(Throwable)} handler.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code subscribe} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param onNext
     *             the {@code Consumer<T>} you have designed to accept emissions from the current {@code Observable}
     * @return the new {@link Disposable} instance that can be used to dispose the subscription at any time
     * @throws NullPointerException
     *             if {@code onNext} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/subscribe.html">ReactiveX operators documentation: Subscribe</a>
     * @see #subscribe(Consumer, Consumer, Action, DisposableContainer)
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Disposable subscribe(@NonNull Consumer<? super T> onNext) {
        return subscribe(onNext, Functions.ON_ERROR_MISSING, Functions.EMPTY_ACTION);
    }

    /**
     * Subscribes to the current {@code Observable} and provides callbacks to handle the items it emits and any error
     * notification it signals.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code subscribe} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param onNext
     *             the {@code Consumer<T>} you have designed to accept emissions from the current {@code Observable}
     * @param onError
     *             the {@code Consumer<Throwable>} you have designed to accept any error notification from the current
     *             {@code Observable}
     * @return the new {@link Disposable} instance that can be used to dispose the subscription at any time
     * @throws NullPointerException
     *             if {@code onNext} or {@code onError} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/subscribe.html">ReactiveX operators documentation: Subscribe</a>
     * @see #subscribe(Consumer, Consumer, Action, DisposableContainer)
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Disposable subscribe(@NonNull Consumer<? super T> onNext, @NonNull Consumer<? super Throwable> onError) {
        return subscribe(onNext, onError, Functions.EMPTY_ACTION);
    }

    /**
     * Subscribes to the current {@code Observable} and provides callbacks to handle the items it emits and any error or
     * completion notification it signals.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code subscribe} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param onNext
     *             the {@code Consumer<T>} you have designed to accept emissions from the current {@code Observable}
     * @param onError
     *             the {@code Consumer<Throwable>} you have designed to accept any error notification from the current
     *             {@code Observable}
     * @param onComplete
     *             the {@link Action} you have designed to accept a completion notification from the current
     *             {@code Observable}
     * @return the new {@link Disposable} instance that can be used to dispose the subscription at any time
     * @throws NullPointerException
     *             if {@code onNext}, {@code onError} or {@code onComplete} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/subscribe.html">ReactiveX operators documentation: Subscribe</a>
     * @see #subscribe(Consumer, Consumer, Action, DisposableContainer)
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Disposable subscribe(@NonNull Consumer<? super T> onNext, @NonNull Consumer<? super Throwable> onError,
            @NonNull Action onComplete) {
        Objects.requireNonNull(onNext, "onNext is null");
        Objects.requireNonNull(onError, "onError is null");
        Objects.requireNonNull(onComplete, "onComplete is null");

        LambdaObserver<T> ls = new LambdaObserver<>(onNext, onError, onComplete, Functions.emptyConsumer());

        subscribe(ls);

        return ls;
    }

    /**
     * Wraps the given onXXX callbacks into a {@link Disposable} {@link Observer},
     * adds it to the given {@code DisposableContainer} and ensures, that if the upstream
     * terminates or this particular {@code Disposable} is disposed, the {@code Observer} is removed
     * from the given container.
     * <p>
     * The {@code Observer} will be removed after the callback for the terminal event has been invoked.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code subscribe} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param onNext the callback for upstream items
     * @param onError the callback for an upstream error if any
     * @param onComplete the callback for the upstream completion if any
     * @param container the {@code DisposableContainer} (such as {@link CompositeDisposable}) to add and remove the
     *                  created {@code Disposable} {@code Observer}
     * @return the {@code Disposable} that allows disposing the particular subscription.
     * @throws NullPointerException
     *             if {@code onNext}, {@code onError},
     *             {@code onComplete} or {@code container} is {@code null}
     * @since 3.1.0
     */
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Disposable subscribe(
            @NonNull Consumer<? super T> onNext,
            @NonNull Consumer<? super Throwable> onError,
            @NonNull Action onComplete,
            @NonNull DisposableContainer container) {
        Objects.requireNonNull(onNext, "onNext is null");
        Objects.requireNonNull(onError, "onError is null");
        Objects.requireNonNull(onComplete, "onComplete is null");
        Objects.requireNonNull(container, "container is null");

        DisposableAutoReleaseObserver<T> observer = new DisposableAutoReleaseObserver<>(
                container, onNext, onError, onComplete);
        container.add(observer);
        subscribe(observer);
        return observer;
    }

    @SchedulerSupport(SchedulerSupport.NONE)
    @Override
    public final void subscribe(@NonNull Observer<? super T> observer) {
        Objects.requireNonNull(observer, "observer is null");
        try {
            observer = RxJavaPlugins.onSubscribe(this, observer);

            Objects.requireNonNull(observer, "The RxJavaPlugins.onSubscribe hook returned a null Observer. Please change the handler provided to RxJavaPlugins.setOnObservableSubscribe for invalid null returns. Further reading: https://github.com/ReactiveX/RxJava/wiki/Plugins");

            subscribeActual(observer);
        } catch (NullPointerException e) { // NOPMD
            throw e;
        } catch (Throwable e) {
            Exceptions.throwIfFatal(e);
            // can't call onError because no way to know if a Disposable has been set or not
            // can't call onSubscribe because the call might have set a Subscription already
            RxJavaPlugins.onError(e);

            NullPointerException npe = new NullPointerException("Actually not, but can't throw other exceptions due to RS");
            npe.initCause(e);
            throw npe;
        }
    }

    /**
     * Operator implementations (both source and intermediate) should implement this method that
     * performs the necessary business logic and handles the incoming {@link Observer}s.
     * <p>There is no need to call any of the plugin hooks on the current {@code Observable} instance or
     * the {@code Observer}; all hooks and basic safeguards have been
     * applied by {@link #subscribe(Observer)} before this method gets called.
     * @param observer the incoming {@code Observer}, never {@code null}
     */
    protected abstract void subscribeActual(@NonNull Observer<? super T> observer);

    /**
     * Subscribes a given {@link Observer} (subclass) to the current {@code Observable} and returns the given
     * {@code Observer} instance as is.
     * <p>Usage example:
     * <pre><code>
     * Observable&lt;Integer&gt; source = Observable.range(1, 10);
     * CompositeDisposable composite = new CompositeDisposable();
     *
     * DisposableObserver&lt;Integer&gt; ds = new DisposableObserver&lt;&gt;() {
     *     // ...
     * };
     *
     * composite.add(source.subscribeWith(ds));
     * </code></pre>
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code subscribeWith} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <E> the type of the {@code Observer} to use and return
     * @param observer the {@code Observer} (subclass) to use and return, not {@code null}
     * @return the input {@code observer}
     * @throws NullPointerException if {@code observer} is {@code null}
     * @since 2.0
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <@NonNull E extends Observer<? super T>> E subscribeWith(E observer) {
        subscribe(observer);
        return observer;
    }

    /**
     * Asynchronously subscribes {@link Observer}s to the current {@code Observable} on the specified {@link Scheduler}.
     * <p>
     * <img width="640" height="305" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/subscribeOn.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>You specify which {@code Scheduler} this operator will use.</dd>
     * </dl>
     *
     * @param scheduler
     *            the {@code Scheduler} to perform subscription actions on
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code scheduler} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/subscribeon.html">ReactiveX operators documentation: SubscribeOn</a>
     * @see <a href="http://www.grahamlea.com/2014/07/rxjava-threading-examples/">RxJava Threading Examples</a>
     * @see #observeOn
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    @NonNull
    public final Observable<T> subscribeOn(@NonNull Scheduler scheduler) {
        Objects.requireNonNull(scheduler, "scheduler is null");
        return RxJavaPlugins.onAssembly(new ObservableSubscribeOn<>(this, scheduler));
    }

    /**
     * Returns an {@code Observable} that emits the items emitted by the current {@code Observable} or the items of an alternate
     * {@link ObservableSource} if the current {@code Observable} is empty.
     * <p>
     * <img width="640" height="256" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/switchifempty.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code switchIfEmpty} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param other
     *              the alternate {@code ObservableSource} to subscribe to if the source does not emit any items
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code other} is {@code null}
     * @since 1.1.0
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Observable<T> switchIfEmpty(@NonNull ObservableSource<? extends T> other) {
        Objects.requireNonNull(other, "other is null");
        return RxJavaPlugins.onAssembly(new ObservableSwitchIfEmpty<>(this, other));
    }

    /**
     * Returns a new {@code Observable} by applying a function that you supply to each item emitted by the current
     * {@code Observable} that returns an {@link ObservableSource}, and then emitting the items emitted by the most recently emitted
     * of these {@code ObservableSource}s.
     * <p>
     * The resulting {@code Observable} completes if both the current {@code Observable} and the last inner {@code ObservableSource}, if any, complete.
     * If the current {@code Observable} signals an {@code onError}, the inner {@code ObservableSource} is disposed and the error delivered in-sequence.
     * <p>
     * <img width="640" height="350" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/switchMap.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code switchMap} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <R> the element type of the inner {@code ObservableSource}s and the output
     * @param mapper
     *            a function that, when applied to an item emitted by the current {@code Observable}, returns an
     *            {@code ObservableSource}
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code mapper} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/flatmap.html">ReactiveX operators documentation: FlatMap</a>
     * @see #switchMapDelayError(Function)
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <@NonNull R> Observable<R> switchMap(@NonNull Function<? super T, ? extends ObservableSource<? extends R>> mapper) {
        return switchMap(mapper, bufferSize());
    }

    /**
     * Returns a new {@code Observable} by applying a function that you supply to each item emitted by the current
     * {@code Observable} that returns an {@link ObservableSource}, and then emitting the items emitted by the most recently emitted
     * of these {@code ObservableSource}s.
     * <p>
     * The resulting {@code Observable} completes if both the current {@code Observable} and the last inner {@code ObservableSource}, if any, complete.
     * If the current {@code Observable} signals an {@code onError}, the inner {@code ObservableSource} is disposed and the error delivered in-sequence.
     * <p>
     * <img width="640" height="350" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/switchMap.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code switchMap} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <R> the element type of the inner {@code ObservableSource}s and the output
     * @param mapper
     *            a function that, when applied to an item emitted by the current {@code Observable}, returns an
     *            {@code ObservableSource}
     * @param bufferSize
     *            the number of elements expected from the current active inner {@code ObservableSource} to be buffered
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code mapper} is {@code null}
     * @throws IllegalArgumentException if {@code bufferSize} is non-positive
     * @see <a href="http://reactivex.io/documentation/operators/flatmap.html">ReactiveX operators documentation: FlatMap</a>
     * @see #switchMapDelayError(Function, int)
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <@NonNull R> Observable<R> switchMap(@NonNull Function<? super T, ? extends ObservableSource<? extends R>> mapper, int bufferSize) {
        Objects.requireNonNull(mapper, "mapper is null");
        ObjectHelper.verifyPositive(bufferSize, "bufferSize");
        if (this instanceof ScalarSupplier) {
            @SuppressWarnings("unchecked")
            T v = ((ScalarSupplier<T>)this).get();
            if (v == null) {
                return empty();
            }
            return ObservableScalarXMap.scalarXMap(v, mapper);
        }
        return RxJavaPlugins.onAssembly(new ObservableSwitchMap<>(this, mapper, bufferSize, false));
    }

    /**
     * Maps the items of the current {@code Observable} into {@link CompletableSource}s, subscribes to the newer one while
     * disposing the subscription to the previous {@code CompletableSource}, thus keeping at most one
     * active {@code CompletableSource} running.
     * <p>
     * <img width="640" height="522" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/switchMapCompletable.f.png" alt="">
     * <p>
     * Since a {@code CompletableSource} doesn't produce any items, the resulting reactive type of
     * this operator is a {@link Completable} that can only indicate successful completion or
     * a failure in any of the inner {@code CompletableSource}s or the failure of the current
     * {@code Observable}.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code switchMapCompletable} does not operate by default on a particular {@link Scheduler}.</dd>
     *  <dt><b>Error handling:</b></dt>
     *  <dd>If either the current {@code Observable} or the active {@code CompletableSource} signals an {@code onError},
     *  the resulting {@code Completable} is terminated immediately with that {@link Throwable}.
     *  Use the {@link #switchMapCompletableDelayError(Function)} to delay such inner failures until
     *  every inner {@code CompletableSource}s and the main {@code Observable} terminates in some fashion.
     *  If they fail concurrently, the operator may combine the {@code Throwable}s into a
     *  {@link CompositeException}
     *  and signal it to the downstream instead. If any inactivated (switched out) {@code CompletableSource}
     *  signals an {@code onError} late, the {@code Throwable}s will be signaled to the global error handler via
     *  {@link RxJavaPlugins#onError(Throwable)} method as {@link UndeliverableException} errors.
     *  </dd>
     * </dl>
     * <p>History: 2.1.11 - experimental
     * @param mapper the function called with each upstream item and should return a
     *               {@code CompletableSource} to be subscribed to and awaited for
     *               (non blockingly) for its terminal event
     * @return the new {@code Completable} instance
     * @throws NullPointerException if {@code mapper} is {@code null}
     * @see #switchMapCompletableDelayError(Function)
     * @since 2.2
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Completable switchMapCompletable(@NonNull Function<? super T, ? extends CompletableSource> mapper) {
        Objects.requireNonNull(mapper, "mapper is null");
        return RxJavaPlugins.onAssembly(new ObservableSwitchMapCompletable<>(this, mapper, false));
    }

    /**
     * Maps the upstream values into {@link CompletableSource}s, subscribes to the newer one while
     * disposing the subscription to the previous {@code CompletableSource}, thus keeping at most one
     * active {@code CompletableSource} running and delaying any main or inner errors until all
     * of them terminate.
     * <p>
     * <img width="640" height="453" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/switchMapCompletableDelayError.f.png" alt="">
     * <p>
     * Since a {@code CompletableSource} doesn't produce any items, the resulting reactive type of
     * this operator is a {@link Completable} that can only indicate successful completion or
     * a failure in any of the inner {@code CompletableSource}s or the failure of the current
     * {@code Observable}.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code switchMapCompletableDelayError} does not operate by default on a particular {@link Scheduler}.</dd>
     *  <dt><b>Error handling:</b></dt>
     *  <dd>The errors of the current {@code Observable} and all the {@code CompletableSource}s, who had the chance
     *  to run to their completion, are delayed until
     *  all of them terminate in some fashion. At this point, if there was only one failure, the respective
     *  {@link Throwable} is emitted to the downstream. It there were more than one failures, the
     *  operator combines all {@code Throwable}s into a {@link CompositeException}
     *  and signals that to the downstream.
     *  If any inactivated (switched out) {@code CompletableSource}
     *  signals an {@code onError} late, the {@code Throwable}s will be signaled to the global error handler via
     *  {@link RxJavaPlugins#onError(Throwable)} method as {@link UndeliverableException} errors.
     *  </dd>
     * </dl>
     * <p>History: 2.1.11 - experimental
     * @param mapper the function called with each upstream item and should return a
     *               {@code CompletableSource} to be subscribed to and awaited for
     *               (non blockingly) for its terminal event
     * @return the new {@code Completable} instance
     * @throws NullPointerException if {@code mapper} is {@code null}
     * @see #switchMapCompletable(Function)
     * @since 2.2
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Completable switchMapCompletableDelayError(@NonNull Function<? super T, ? extends CompletableSource> mapper) {
        Objects.requireNonNull(mapper, "mapper is null");
        return RxJavaPlugins.onAssembly(new ObservableSwitchMapCompletable<>(this, mapper, true));
    }

    /**
     * Maps the items of the current {@code Observable} into {@link MaybeSource}s and switches (subscribes) to the newer ones
     * while disposing the older ones (and ignoring their signals) and emits the latest success value of the current one if
     * available while failing immediately if the current {@code Observable} or any of the
     * active inner {@code MaybeSource}s fail.
     * <p>
     * <img width="640" height="531" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/switchMapMaybe.o.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code switchMapMaybe} does not operate by default on a particular {@link Scheduler}.</dd>
     *  <dt><b>Error handling:</b></dt>
     *  <dd>This operator terminates with an {@code onError} if the current {@code Observable} or any of
     *  the inner {@code MaybeSource}s fail while they are active. When this happens concurrently, their
     *  individual {@link Throwable} errors may get combined and emitted as a single
     *  {@link CompositeException}. Otherwise, a late
     *  (i.e., inactive or switched out) {@code onError} from the current {@code Observable} or from any of
     *  the inner {@code MaybeSource}s will be forwarded to the global error handler via
     *  {@link RxJavaPlugins#onError(Throwable)} as
     *  {@link UndeliverableException}</dd>
     * </dl>
     * <p>History: 2.1.11 - experimental
     * @param <R> the output value type
     * @param mapper the function called with the current upstream event and should
     *               return a {@code MaybeSource} to replace the current active inner source
     *               and get subscribed to.
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code mapper} is {@code null}
     * @see #switchMapMaybeDelayError(Function)
     * @since 2.2
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <@NonNull R> Observable<R> switchMapMaybe(@NonNull Function<? super T, ? extends MaybeSource<? extends R>> mapper) {
        Objects.requireNonNull(mapper, "mapper is null");
        return RxJavaPlugins.onAssembly(new ObservableSwitchMapMaybe<>(this, mapper, false));
    }

    /**
     * Maps the upstream items into {@link MaybeSource}s and switches (subscribes) to the newer ones
     * while disposing the older ones  (and ignoring their signals) and emits the latest success value of the current one if
     * available, delaying errors from the current {@code Observable} or the inner {@code MaybeSource}s until all terminate.
     * <p>
     * <img width="640" height="469" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/switchMapMaybeDelayError.o.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code switchMapMaybeDelayError} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * <p>History: 2.1.11 - experimental
     * @param <R> the output value type
     * @param mapper the function called with the current upstream event and should
     *               return a {@code MaybeSource} to replace the current active inner source
     *               and get subscribed to.
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code mapper} is {@code null}
     * @see #switchMapMaybe(Function)
     * @since 2.2
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <@NonNull R> Observable<R> switchMapMaybeDelayError(@NonNull Function<? super T, ? extends MaybeSource<? extends R>> mapper) {
        Objects.requireNonNull(mapper, "mapper is null");
        return RxJavaPlugins.onAssembly(new ObservableSwitchMapMaybe<>(this, mapper, true));
    }

    /**
     * Returns a new {@code Observable} by applying a function that you supply to each item emitted by the current
     * {@code Observable} that returns a {@link SingleSource}, and then emitting the item emitted by the most recently emitted
     * of these {@code SingleSource}s.
     * <p>
     * The resulting {@code Observable} completes if both the current {@code Observable} and the last inner {@code SingleSource}, if any, complete.
     * If the current {@code Observable} signals an {@code onError}, the inner {@code SingleSource} is disposed and the error delivered in-sequence.
     * <p>
     * <img width="640" height="532" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/switchMapSingle.o.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code switchMapSingle} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * <p>History: 2.0.8 - experimental
     * @param <R> the element type of the inner {@code SingleSource}s and the output
     * @param mapper
     *            a function that, when applied to an item emitted by the current {@code Observable}, returns a
     *            {@code SingleSource}
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code mapper} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/flatmap.html">ReactiveX operators documentation: FlatMap</a>
     * @see #switchMapSingleDelayError(Function)
     * @since 2.2
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <@NonNull R> Observable<R> switchMapSingle(@NonNull Function<? super T, ? extends SingleSource<? extends R>> mapper) {
        Objects.requireNonNull(mapper, "mapper is null");
        return RxJavaPlugins.onAssembly(new ObservableSwitchMapSingle<>(this, mapper, false));
    }

    /**
     * Returns a new {@code Observable} by applying a function that you supply to each item emitted by the current
     * {@code Observable} that returns a {@link SingleSource}, and then emitting the item emitted by the most recently emitted
     * of these {@code SingleSource}s and delays any error until all {@code SingleSource}s terminate.
     * <p>
     * The resulting {@code Observable} completes if both the current {@code Observable} and the last inner {@code SingleSource}, if any, complete.
     * If the current {@code Observable} signals an {@code onError}, the termination of the last inner {@code SingleSource} will emit that error as is
     * or wrapped into a {@link CompositeException} along with the other possible errors the former inner {@code SingleSource}s signaled.
     * <p>
     * <img width="640" height="467" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/switchMapSingleDelayError.o.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code switchMapSingleDelayError} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * <p>History: 2.0.8 - experimental
     * @param <R> the element type of the inner {@code SingleSource}s and the output
     * @param mapper
     *            a function that, when applied to an item emitted by the current {@code Observable}, returns a
     *            {@code SingleSource}
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code mapper} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/flatmap.html">ReactiveX operators documentation: FlatMap</a>
     * @see #switchMapSingle(Function)
     * @since 2.2
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <@NonNull R> Observable<R> switchMapSingleDelayError(@NonNull Function<? super T, ? extends SingleSource<? extends R>> mapper) {
        Objects.requireNonNull(mapper, "mapper is null");
        return RxJavaPlugins.onAssembly(new ObservableSwitchMapSingle<>(this, mapper, true));
    }

    /**
     * Returns a new {@code Observable} by applying a function that you supply to each item emitted by the current
     * {@code Observable} that returns an {@link ObservableSource}, and then emitting the items emitted by the most recently emitted
     * of these {@code ObservableSource}s and delays any error until all {@code ObservableSource}s terminate.
     * <p>
     * The resulting {@code Observable} completes if both the current {@code Observable} and the last inner {@code ObservableSource}, if any, complete.
     * If the current {@code Observable} signals an {@code onError}, the termination of the last inner {@code ObservableSource} will emit that error as is
     * or wrapped into a {@link CompositeException} along with the other possible errors the former inner {@code ObservableSource}s signaled.
     * <p>
     * <img width="640" height="350" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/switchMap.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code switchMapDelayError} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <R> the element type of the inner {@code ObservableSource}s and the output
     * @param mapper
     *            a function that, when applied to an item emitted by the current {@code Observable}, returns an
     *            {@code ObservableSource}
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code mapper} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/flatmap.html">ReactiveX operators documentation: FlatMap</a>
     * @see #switchMap(Function)
     * @since 2.0
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <@NonNull R> Observable<R> switchMapDelayError(@NonNull Function<? super T, ? extends ObservableSource<? extends R>> mapper) {
        return switchMapDelayError(mapper, bufferSize());
    }

    /**
     * Returns a new {@code Observable} by applying a function that you supply to each item emitted by the current
     * {@code Observable} that returns an {@link ObservableSource}, and then emitting the items emitted by the most recently emitted
     * of these {@code ObservableSource}s and delays any error until all {@code ObservableSource}s terminate.
     * <p>
     * The resulting {@code Observable} completes if both the current {@code Observable} and the last inner {@code ObservableSource}, if any, complete.
     * If the current {@code Observable} signals an {@code onError}, the termination of the last inner {@code ObservableSource} will emit that error as is
     * or wrapped into a {@link CompositeException} along with the other possible errors the former inner {@code ObservableSource}s signaled.
     * <p>
     * <img width="640" height="350" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/switchMap.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code switchMapDelayError} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <R> the element type of the inner {@code ObservableSource}s and the output
     * @param mapper
     *            a function that, when applied to an item emitted by the current {@code Observable}, returns an
     *            {@code ObservableSource}
     * @param bufferSize
     *            the number of elements expected from the current active inner {@code ObservableSource} to be buffered
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code mapper} is {@code null}
     * @throws IllegalArgumentException if {@code bufferSize} is non-positive
     * @see <a href="http://reactivex.io/documentation/operators/flatmap.html">ReactiveX operators documentation: FlatMap</a>
     * @see #switchMap(Function, int)
     * @since 2.0
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <@NonNull R> Observable<R> switchMapDelayError(@NonNull Function<? super T, ? extends ObservableSource<? extends R>> mapper, int bufferSize) {
        Objects.requireNonNull(mapper, "mapper is null");
        ObjectHelper.verifyPositive(bufferSize, "bufferSize");
        if (this instanceof ScalarSupplier) {
            @SuppressWarnings("unchecked")
            T v = ((ScalarSupplier<T>)this).get();
            if (v == null) {
                return empty();
            }
            return ObservableScalarXMap.scalarXMap(v, mapper);
        }
        return RxJavaPlugins.onAssembly(new ObservableSwitchMap<>(this, mapper, bufferSize, true));
    }

    /**
     * Returns an {@code Observable} that emits only the first {@code count} items emitted by the current {@code Observable}.
     * If the source emits fewer than {@code count} items then all of its items are emitted.
     * <p>
     * <img width="640" height="305" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/take.v3.png" alt="">
     * <p>
     * This method returns an {@code Observable} that will invoke a subscribing {@link Observer}'s
     * {@link Observer#onNext onNext} function a maximum of {@code count} times before invoking
     * {@link Observer#onComplete onComplete}.
     * <p>
     * Taking {@code 0} items from the current {@code Observable} will still subscribe to it, allowing the
     * subscription-time side-effects to happen there, but will be immediately disposed and the downstream completed
     * without any item emission.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This version of {@code take} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param count
     *            the maximum number of items to emit
     * @return the new {@code Observable} instance
     * @throws IllegalArgumentException if {@code count} is negative
     * @see <a href="http://reactivex.io/documentation/operators/take.html">ReactiveX operators documentation: Take</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Observable<T> take(long count) {
        if (count < 0) {
            throw new IllegalArgumentException("count >= 0 required but it was " + count);
        }
        return RxJavaPlugins.onAssembly(new ObservableTake<>(this, count));
    }

    /**
     * Returns an {@code Observable} that emits those items emitted by the current {@code Observable} before a specified time runs
     * out.
     * <p>
     * If time runs out before the {@code Observable} completes normally, the {@code onComplete} event will be
     * signaled on the default {@code computation} {@link Scheduler}.
     * <p>
     * <img width="640" height="305" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/take.t.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This version of {@code take} operates by default on the {@code computation} {@code Scheduler}.</dd>
     * </dl>
     *
     * @param time
     *            the length of the time window
     * @param unit
     *            the time unit of {@code time}
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code unit} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/take.html">ReactiveX operators documentation: Take</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Observable<T> take(long time, @NonNull TimeUnit unit) {
        return takeUntil(timer(time, unit));
    }

    /**
     * Returns an {@code Observable} that emits those items emitted by the current {@code Observable} before a specified time (on a
     * specified {@link Scheduler}) runs out.
     * <p>
     * If time runs out before the {@code Observable} completes normally, the {@code onComplete} event will be
     * signaled on the provided {@code Scheduler}.
     * <p>
     * <img width="640" height="305" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/take.ts.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>You specify which {@code Scheduler} this operator will use.</dd>
     * </dl>
     *
     * @param time
     *            the length of the time window
     * @param unit
     *            the time unit of {@code time}
     * @param scheduler
     *            the {@code Scheduler} used for time source
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code unit} or {@code scheduler} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/take.html">ReactiveX operators documentation: Take</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    @NonNull
    public final Observable<T> take(long time, @NonNull TimeUnit unit, @NonNull Scheduler scheduler) {
        return takeUntil(timer(time, unit, scheduler));
    }

    /**
     * Returns an {@code Observable} that emits at most the last {@code count} items emitted by the current {@code Observable}.
     * If the source emits fewer than {@code count} items then all of its items are emitted.
     * <p>
     * <img width="640" height="310" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/takeLast.n.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This version of {@code takeLast} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param count
     *            the maximum number of items to emit from the end of the sequence of items emitted by the current
     *            {@code Observable}
     * @return the new {@code Observable} instance
     * @throws IllegalArgumentException
     *             if {@code count} is negative
     * @see <a href="http://reactivex.io/documentation/operators/takelast.html">ReactiveX operators documentation: TakeLast</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Observable<T> takeLast(int count) {
        if (count < 0) {
            throw new IllegalArgumentException("count >= 0 required but it was " + count);
        }
        if (count == 0) {
            return RxJavaPlugins.onAssembly(new ObservableIgnoreElements<>(this));
        }
        if (count == 1) {
            return RxJavaPlugins.onAssembly(new ObservableTakeLastOne<>(this));
        }
        return RxJavaPlugins.onAssembly(new ObservableTakeLast<>(this, count));
    }

    /**
     * Returns an {@code Observable} that emits at most a specified number of items from the current {@code Observable} that were
     * emitted in a specified window of time before the current {@code Observable} completed.
     * <p>
     * <img width="640" height="310" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/takeLast.tn.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code takeLast} does not operate on any particular scheduler but uses the current time
     *  from the {@code trampoline} {@link Scheduler}.</dd>
     * </dl>
     *
     * @param count
     *            the maximum number of items to emit
     * @param time
     *            the length of the time window
     * @param unit
     *            the time unit of {@code time}
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code unit} is {@code null}
     * @throws IllegalArgumentException if {@code count} is negative
     * @see <a href="http://reactivex.io/documentation/operators/takelast.html">ReactiveX operators documentation: TakeLast</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.TRAMPOLINE)
    @NonNull
    public final Observable<T> takeLast(long count, long time, @NonNull TimeUnit unit) {
        return takeLast(count, time, unit, Schedulers.trampoline(), false, bufferSize());
    }

    /**
     * Returns an {@code Observable} that emits at most a specified number of items from the current {@code Observable} that were
     * emitted in a specified window of time before the current {@code Observable} completed, where the timing information is
     * provided by a given {@link Scheduler}.
     * <p>
     * <img width="640" height="310" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/takeLast.tns.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>You specify which {@code Scheduler} this operator will use for tracking the current time</dd>
     * </dl>
     *
     * @param count
     *            the maximum number of items to emit
     * @param time
     *            the length of the time window
     * @param unit
     *            the time unit of {@code time}
     * @param scheduler
     *            the {@code Scheduler} that provides the timestamps for the observed items
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code unit} or {@code scheduler} is {@code null}
     * @throws IllegalArgumentException
     *             if {@code count} is negative
     * @see <a href="http://reactivex.io/documentation/operators/takelast.html">ReactiveX operators documentation: TakeLast</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    @NonNull
    public final Observable<T> takeLast(long count, long time, @NonNull TimeUnit unit, @NonNull Scheduler scheduler) {
        return takeLast(count, time, unit, scheduler, false, bufferSize());
    }

    /**
     * Returns an {@code Observable} that emits at most a specified number of items from the current {@code Observable} that were
     * emitted in a specified window of time before the current {@code Observable} completed, where the timing information is
     * provided by a given {@link Scheduler}.
     * <p>
     * <img width="640" height="310" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/takeLast.tns.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>You specify which {@code Scheduler} this operator will use for tracking the current time</dd>
     * </dl>
     *
     * @param count
     *            the maximum number of items to emit
     * @param time
     *            the length of the time window
     * @param unit
     *            the time unit of {@code time}
     * @param scheduler
     *            the {@code Scheduler} that provides the timestamps for the observed items
     * @param delayError
     *            if {@code true}, an exception signaled by the current {@code Observable} is delayed until the regular elements are consumed
     *            by the downstream; if {@code false}, an exception is immediately signaled and all regular elements dropped
     * @param bufferSize
     *            the hint about how many elements to expect to be last
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code unit} or {@code scheduler} is {@code null}
     * @throws IllegalArgumentException
     *             if {@code count} is negative or {@code bufferSize} is non-positive
     * @see <a href="http://reactivex.io/documentation/operators/takelast.html">ReactiveX operators documentation: TakeLast</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    @NonNull
    public final Observable<T> takeLast(long count, long time, @NonNull TimeUnit unit, @NonNull Scheduler scheduler, boolean delayError, int bufferSize) {
        Objects.requireNonNull(unit, "unit is null");
        Objects.requireNonNull(scheduler, "scheduler is null");
        ObjectHelper.verifyPositive(bufferSize, "bufferSize");
        if (count < 0) {
            throw new IllegalArgumentException("count >= 0 required but it was " + count);
        }
        return RxJavaPlugins.onAssembly(new ObservableTakeLastTimed<>(this, count, time, unit, scheduler, bufferSize, delayError));
    }

    /**
     * Returns an {@code Observable} that emits the items from the current {@code Observable} that were emitted in a specified
     * window of time before the current {@code Observable} completed.
     * <p>
     * <img width="640" height="310" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/takeLast.t.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code takeLast} does not operate on any particular scheduler but uses the current time
     *  from the {@code trampoline} {@link Scheduler}.</dd>
     * </dl>
     *
     * @param time
     *            the length of the time window
     * @param unit
     *            the time unit of {@code time}
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code unit} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/takelast.html">ReactiveX operators documentation: TakeLast</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.TRAMPOLINE)
    @NonNull
    public final Observable<T> takeLast(long time, @NonNull TimeUnit unit) {
        return takeLast(time, unit, Schedulers.trampoline(), false, bufferSize());
    }

    /**
     * Returns an {@code Observable} that emits the items from the current {@code Observable} that were emitted in a specified
     * window of time before the current {@code Observable} completed.
     * <p>
     * <img width="640" height="310" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/takeLast.t.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code takeLast} does not operate on any particular scheduler but uses the current time
     *  from the {@code trampoline} {@link Scheduler}.</dd>
     * </dl>
     *
     * @param time
     *            the length of the time window
     * @param unit
     *            the time unit of {@code time}
     * @param delayError
     *            if {@code true}, an exception signaled by the current {@code Observable} is delayed until the regular elements are consumed
     *            by the downstream; if {@code false}, an exception is immediately signaled and all regular elements dropped
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code unit} is {@code null}
     * @throws IllegalArgumentException if {@code count} is non-positive
     * @see <a href="http://reactivex.io/documentation/operators/takelast.html">ReactiveX operators documentation: TakeLast</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.TRAMPOLINE)
    @NonNull
    public final Observable<T> takeLast(long time, @NonNull TimeUnit unit, boolean delayError) {
        return takeLast(time, unit, Schedulers.trampoline(), delayError, bufferSize());
    }

    /**
     * Returns an {@code Observable} that emits the items from the current {@code Observable} that were emitted in a specified
     * window of time before the current {@code Observable} completed, where the timing information is provided by a specified
     * {@link Scheduler}.
     * <p>
     * <img width="640" height="310" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/takeLast.ts.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>You specify which {@code Scheduler} this operator will use.</dd>
     * </dl>
     *
     * @param time
     *            the length of the time window
     * @param unit
     *            the time unit of {@code time}
     * @param scheduler
     *            the {@code Scheduler} that provides the timestamps for the observed items
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code unit} or {@code scheduler} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/takelast.html">ReactiveX operators documentation: TakeLast</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    @NonNull
    public final Observable<T> takeLast(long time, @NonNull TimeUnit unit, @NonNull Scheduler scheduler) {
        return takeLast(time, unit, scheduler, false, bufferSize());
    }

    /**
     * Returns an {@code Observable} that emits the items from the current {@code Observable} that were emitted in a specified
     * window of time before the current {@code Observable} completed, where the timing information is provided by a specified
     * {@link Scheduler}.
     * <p>
     * <img width="640" height="310" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/takeLast.ts.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>You specify which {@code Scheduler} this operator will use.</dd>
     * </dl>
     *
     * @param time
     *            the length of the time window
     * @param unit
     *            the time unit of {@code time}
     * @param scheduler
     *            the {@code Scheduler} that provides the timestamps for the observed items
     * @param delayError
     *            if {@code true}, an exception signaled by the current {@code Observable} is delayed until the regular elements are consumed
     *            by the downstream; if {@code false}, an exception is immediately signaled and all regular elements dropped
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code unit} or {@code scheduler} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/takelast.html">ReactiveX operators documentation: TakeLast</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    @NonNull
    public final Observable<T> takeLast(long time, @NonNull TimeUnit unit, @NonNull Scheduler scheduler, boolean delayError) {
        return takeLast(time, unit, scheduler, delayError, bufferSize());
    }

    /**
     * Returns an {@code Observable} that emits the items from the current {@code Observable} that were emitted in a specified
     * window of time before the current {@code Observable} completed, where the timing information is provided by a specified
     * {@link Scheduler}.
     * <p>
     * <img width="640" height="310" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/takeLast.ts.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>You specify which {@code Scheduler} this operator will use.</dd>
     * </dl>
     *
     * @param time
     *            the length of the time window
     * @param unit
     *            the time unit of {@code time}
     * @param scheduler
     *            the {@code Scheduler} that provides the timestamps for the observed items
     * @param delayError
     *            if {@code true}, an exception signaled by the current {@code Observable} is delayed until the regular elements are consumed
     *            by the downstream; if {@code false}, an exception is immediately signaled and all regular elements dropped
     * @param bufferSize
     *            the hint about how many elements to expect to be last
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code unit} or {@code scheduler} is {@code null}
     * @throws IllegalArgumentException if {@code bufferSize} is non-positive
     * @see <a href="http://reactivex.io/documentation/operators/takelast.html">ReactiveX operators documentation: TakeLast</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    @NonNull
    public final Observable<T> takeLast(long time, @NonNull TimeUnit unit, @NonNull Scheduler scheduler, boolean delayError, int bufferSize) {
        return takeLast(Long.MAX_VALUE, time, unit, scheduler, delayError, bufferSize);
    }

    /**
     * Returns an {@code Observable} that emits the items emitted by the current {@code Observable} until a second {@link ObservableSource}
     * emits an item or completes.
     * <p>
     * <img width="640" height="213" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Observable.takeUntil.o.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code takeUntil} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param other
     *            the {@code ObservableSource} whose first emitted item or completion will cause {@code takeUntil} to stop emitting items
     *            from the current {@code Observable}
     * @param <U>
     *            the type of items emitted by {@code other}
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code other} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/takeuntil.html">ReactiveX operators documentation: TakeUntil</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <@NonNull U> Observable<T> takeUntil(@NonNull ObservableSource<U> other) {
        Objects.requireNonNull(other, "other is null");
        return RxJavaPlugins.onAssembly(new ObservableTakeUntil<>(this, other));
    }

    /**
     * Returns an {@code Observable} that emits items emitted by the current {@code Observable}, checks the specified predicate
     * for each item, and then completes when the condition is satisfied.
     * <p>
     * <img width="640" height="305" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/takeUntil.p.v3.png" alt="">
     * <p>
     * The difference between this operator and {@link #takeWhile(Predicate)} is that here, the condition is
     * evaluated <em>after</em> the item is emitted.
     *
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code takeUntil} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param stopPredicate
     *            a function that evaluates an item emitted by the current {@code Observable} and returns a {@link Boolean}
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code stopPredicate} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/takeuntil.html">ReactiveX operators documentation: TakeUntil</a>
     * @see Observable#takeWhile(Predicate)
     * @since 1.1.0
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Observable<T> takeUntil(@NonNull Predicate<? super T> stopPredicate) {
        Objects.requireNonNull(stopPredicate, "stopPredicate is null");
        return RxJavaPlugins.onAssembly(new ObservableTakeUntilPredicate<>(this, stopPredicate));
    }

    /**
     * Returns an {@code Observable} that emits items emitted by the current {@code Observable} so long as each item satisfied a
     * specified condition, and then completes as soon as this condition is not satisfied.
     * <p>
     * <img width="640" height="305" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/takeWhile.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code takeWhile} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param predicate
     *            a function that evaluates an item emitted by the current {@code Observable} and returns a {@link Boolean}
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code predicate} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/takewhile.html">ReactiveX operators documentation: TakeWhile</a>
     * @see Observable#takeUntil(Predicate)
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Observable<T> takeWhile(@NonNull Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        return RxJavaPlugins.onAssembly(new ObservableTakeWhile<>(this, predicate));
    }

    /**
     * Returns an {@code Observable} that emits only the first item emitted by the current {@code Observable} during sequential
     * time windows of a specified duration.
     * <p>
     * This differs from {@link #throttleLast} in that this only tracks passage of time whereas
     * {@code throttleLast} ticks at scheduled intervals.
     * <p>
     * <img width="640" height="305" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/throttleFirst.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code throttleFirst} operates by default on the {@code computation} {@link Scheduler}.</dd>
     * </dl>
     *
     * @param windowDuration
     *            time to wait before emitting another item after emitting the last item
     * @param unit
     *            the unit of time of {@code windowDuration}
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code unit} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/sample.html">ReactiveX operators documentation: Sample</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.COMPUTATION)
    @NonNull
    public final Observable<T> throttleFirst(long windowDuration, @NonNull TimeUnit unit) {
        return throttleFirst(windowDuration, unit, Schedulers.computation());
    }

    /**
     * Returns an {@code Observable} that emits only the first item emitted by the current {@code Observable} during sequential
     * time windows of a specified duration, where the windows are managed by a specified {@link Scheduler}.
     * <p>
     * This differs from {@link #throttleLast} in that this only tracks passage of time whereas
     * {@code throttleLast} ticks at scheduled intervals.
     * <p>
     * <img width="640" height="305" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/throttleFirst.s.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>You specify which {@code Scheduler} this operator will use.</dd>
     * </dl>
     *
     * @param skipDuration
     *            time to wait before emitting another item after emitting the last item
     * @param unit
     *            the unit of time of {@code skipDuration}
     * @param scheduler
     *            the {@code Scheduler} to use internally to manage the timers that handle timeout for each
     *            event
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code unit} or {@code scheduler} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/sample.html">ReactiveX operators documentation: Sample</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    @NonNull
    public final Observable<T> throttleFirst(long skipDuration, @NonNull TimeUnit unit, @NonNull Scheduler scheduler) {
        Objects.requireNonNull(unit, "unit is null");
        Objects.requireNonNull(scheduler, "scheduler is null");
        return RxJavaPlugins.onAssembly(new ObservableThrottleFirstTimed<>(this, skipDuration, unit, scheduler));
    }

    /**
     * Returns an {@code Observable} that emits only the last item emitted by the current {@code Observable} during sequential
     * time windows of a specified duration.
     * <p>
     * This differs from {@link #throttleFirst} in that this ticks along at a scheduled interval whereas
     * {@code throttleFirst} does not tick, it just tracks passage of time.
     * <p>
     * <img width="640" height="305" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/throttleLast.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code throttleLast} operates by default on the {@code computation} {@link Scheduler}.</dd>
     * </dl>
     *
     * @param intervalDuration
     *            duration of windows within which the last item emitted by the current {@code Observable} will be
     *            emitted
     * @param unit
     *            the unit of time of {@code intervalDuration}
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code unit} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/sample.html">ReactiveX operators documentation: Sample</a>
     * @see #sample(long, TimeUnit)
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.COMPUTATION)
    @NonNull
    public final Observable<T> throttleLast(long intervalDuration, @NonNull TimeUnit unit) {
        return sample(intervalDuration, unit);
    }

    /**
     * Returns an {@code Observable} that emits only the last item emitted by the current {@code Observable} during sequential
     * time windows of a specified duration, where the duration is governed by a specified {@link Scheduler}.
     * <p>
     * This differs from {@link #throttleFirst} in that this ticks along at a scheduled interval whereas
     * {@code throttleFirst} does not tick, it just tracks passage of time.
     * <p>
     * <img width="640" height="305" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/throttleLast.s.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>You specify which {@code Scheduler} this operator will use.</dd>
     * </dl>
     *
     * @param intervalDuration
     *            duration of windows within which the last item emitted by the current {@code Observable} will be
     *            emitted
     * @param unit
     *            the unit of time of {@code intervalDuration}
     * @param scheduler
     *            the {@code Scheduler} to use internally to manage the timers that handle timeout for each
     *            event
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code unit} or {@code scheduler} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/sample.html">ReactiveX operators documentation: Sample</a>
     * @see #sample(long, TimeUnit, Scheduler)
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    @NonNull
    public final Observable<T> throttleLast(long intervalDuration, @NonNull TimeUnit unit, @NonNull Scheduler scheduler) {
        return sample(intervalDuration, unit, scheduler);
    }

    /**
     * Throttles items from the current {@code Observable} by first emitting the next
     * item from upstream, then periodically emitting the latest item (if any) when
     * the specified timeout elapses between them.
     * <p>
     * <img width="640" height="326" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/throttleLatest.png" alt="">
     * <p>
     * Unlike the option with {@link #throttleLatest(long, TimeUnit, boolean)}, the very last item being held back
     * (if any) is not emitted when the upstream completes.
     * <p>
     * If no items were emitted from the upstream during this timeout phase, the next
     * upstream item is emitted immediately and the timeout window starts from then.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code throttleLatest} operates by default on the {@code computation} {@link Scheduler}.</dd>
     * </dl>
     * <p>History: 2.1.14 - experimental
     * @param timeout the time to wait after an item emission towards the downstream
     *                before trying to emit the latest item from upstream again
     * @param unit    the time unit
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code unit} is {@code null}
     * @see #throttleLatest(long, TimeUnit, boolean)
     * @see #throttleLatest(long, TimeUnit, Scheduler)
     * @since 2.2
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.COMPUTATION)
    @NonNull
    public final Observable<T> throttleLatest(long timeout, @NonNull TimeUnit unit) {
        return throttleLatest(timeout, unit, Schedulers.computation(), false);
    }

    /**
     * Throttles items from the current {@code Observable} by first emitting the next
     * item from upstream, then periodically emitting the latest item (if any) when
     * the specified timeout elapses between them.
     * <p>
     * <img width="640" height="326" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/throttleLatest.e.png" alt="">
     * <p>
     * If no items were emitted from the upstream during this timeout phase, the next
     * upstream item is emitted immediately and the timeout window starts from then.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code throttleLatest} operates by default on the {@code computation} {@link Scheduler}.</dd>
     * </dl>
     * <p>History: 2.1.14 - experimental
     * @param timeout the time to wait after an item emission towards the downstream
     *                before trying to emit the latest item from upstream again
     * @param unit    the time unit
     * @param emitLast If {@code true}, the very last item from the upstream will be emitted
     *                 immediately when the upstream completes, regardless if there is
     *                 a timeout window active or not. If {@code false}, the very last
     *                 upstream item is ignored and the flow terminates.
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code unit} is {@code null}
     * @see #throttleLatest(long, TimeUnit, Scheduler, boolean)
     * @since 2.2
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.COMPUTATION)
    @NonNull
    public final Observable<T> throttleLatest(long timeout, @NonNull TimeUnit unit, boolean emitLast) {
        return throttleLatest(timeout, unit, Schedulers.computation(), emitLast);
    }

    /**
     * Throttles items from the current {@code Observable} by first emitting the next
     * item from upstream, then periodically emitting the latest item (if any) when
     * the specified timeout elapses between them.
     * <p>
     * <img width="640" height="326" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/throttleLatest.s.png" alt="">
     * <p>
     * Unlike the option with {@link #throttleLatest(long, TimeUnit, Scheduler, boolean)}, the very last item being held back
     * (if any) is not emitted when the upstream completes.
     * <p>
     * If no items were emitted from the upstream during this timeout phase, the next
     * upstream item is emitted immediately and the timeout window starts from then.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>You specify which {@link Scheduler} this operator will use.</dd>
     * </dl>
     * <p>History: 2.1.14 - experimental
     * @param timeout the time to wait after an item emission towards the downstream
     *                before trying to emit the latest item from upstream again
     * @param unit    the time unit
     * @param scheduler the {@code Scheduler} where the timed wait and latest item
     *                  emission will be performed
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code unit} or {@code scheduler} is {@code null}
     * @see #throttleLatest(long, TimeUnit, Scheduler, boolean)
     * @since 2.2
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    @NonNull
    public final Observable<T> throttleLatest(long timeout, @NonNull TimeUnit unit, @NonNull Scheduler scheduler) {
        return throttleLatest(timeout, unit, scheduler, false);
    }

    /**
     * Throttles items from the current {@code Observable} by first emitting the next
     * item from upstream, then periodically emitting the latest item (if any) when
     * the specified timeout elapses between them.
     * <p>
     * <img width="640" height="326" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/throttleLatest.se.png" alt="">
     * <p>
     * If no items were emitted from the upstream during this timeout phase, the next
     * upstream item is emitted immediately and the timeout window starts from then.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>You specify which {@link Scheduler} this operator will use.</dd>
     * </dl>
     * <p>History: 2.1.14 - experimental
     * @param timeout the time to wait after an item emission towards the downstream
     *                before trying to emit the latest item from upstream again
     * @param unit    the time unit
     * @param scheduler the {@code Scheduler} where the timed wait and latest item
     *                  emission will be performed
     * @param emitLast If {@code true}, the very last item from the upstream will be emitted
     *                 immediately when the upstream completes, regardless if there is
     *                 a timeout window active or not. If {@code false}, the very last
     *                 upstream item is ignored and the flow terminates.
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code unit} or {@code scheduler} is {@code null}
     * @since 2.2
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    @NonNull
    public final Observable<T> throttleLatest(long timeout, @NonNull TimeUnit unit, @NonNull Scheduler scheduler, boolean emitLast) {
        Objects.requireNonNull(unit, "unit is null");
        Objects.requireNonNull(scheduler, "scheduler is null");
        return RxJavaPlugins.onAssembly(new ObservableThrottleLatest<>(this, timeout, unit, scheduler, emitLast));
    }

    /**
     * Returns an {@code Observable} that mirrors the current {@code Observable}, except that it drops items emitted by the
     * current {@code Observable} that are followed by newer items before a timeout value expires. The timer resets on
     * each emission (alias to {@link #debounce(long, TimeUnit, Scheduler)}).
     * <p>
     * <em>Note:</em> If items keep being emitted by the current {@code Observable} faster than the timeout then no items
     * will be emitted by the resulting {@code Observable}.
     * <p>
     * <img width="640" height="305" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/throttleWithTimeout.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code throttleWithTimeout} operates by default on the {@code computation} {@link Scheduler}.</dd>
     * </dl>
     *
     * @param timeout
     *            the length of the window of time that must pass after the emission of an item from the current
     *            {@code Observable}, in which the current {@code Observable} emits no items, in order for the item to be emitted by the
     *            resulting {@code Observable}
     * @param unit
     *            the unit of time for the specified {@code timeout}
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code unit} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/debounce.html">ReactiveX operators documentation: Debounce</a>
     * @see #debounce(long, TimeUnit)
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.COMPUTATION)
    @NonNull
    public final Observable<T> throttleWithTimeout(long timeout, @NonNull TimeUnit unit) {
        return debounce(timeout, unit);
    }

    /**
     * Returns an {@code Observable} that mirrors the current {@code Observable}, except that it drops items emitted by the
     * current {@code Observable} that are followed by newer items before a timeout value expires on a specified
     * {@link Scheduler}. The timer resets on each emission (Alias to {@link #debounce(long, TimeUnit, Scheduler)}).
     * <p>
     * <em>Note:</em> If items keep being emitted by the current {@code Observable} faster than the timeout then no items
     * will be emitted by the resulting {@code Observable}.
     * <p>
     * <img width="640" height="305" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/throttleWithTimeout.s.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>You specify which {@code Scheduler} this operator will use.</dd>
     * </dl>
     *
     * @param timeout
     *            the length of the window of time that must pass after the emission of an item from the current
     *            {@code Observable}, in which the current {@code Observable} emits no items, in order for the item to be emitted by the
     *            resulting {@code Observable}
     * @param unit
     *            the unit of time for the specified {@code timeout}
     * @param scheduler
     *            the {@code Scheduler} to use internally to manage the timers that handle the timeout for each
     *            item
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code unit} or {@code scheduler} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/debounce.html">ReactiveX operators documentation: Debounce</a>
     * @see #debounce(long, TimeUnit, Scheduler)
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    @NonNull
    public final Observable<T> throttleWithTimeout(long timeout, @NonNull TimeUnit unit, @NonNull Scheduler scheduler) {
        return debounce(timeout, unit, scheduler);
    }

    /**
     * Returns an {@code Observable} that emits records of the time interval between consecutive items emitted by the
     * current {@code Observable}.
     * <p>
     * <img width="640" height="310" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/timeInterval.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code timeInterval} does not operate on any particular scheduler but uses the current time
     *  from the {@code computation} {@link Scheduler}.</dd>
     * </dl>
     *
     * @return the new {@code Observable} instance
     * @see <a href="http://reactivex.io/documentation/operators/timeinterval.html">ReactiveX operators documentation: TimeInterval</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Observable<Timed<T>> timeInterval() {
        return timeInterval(TimeUnit.MILLISECONDS, Schedulers.computation());
    }

    /**
     * Returns an {@code Observable} that emits records of the time interval between consecutive items emitted by the
     * current {@code Observable}, where this interval is computed on a specified {@link Scheduler}.
     * <p>
     * <img width="640" height="315" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/timeInterval.s.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>The operator does not operate on any particular scheduler but uses the current time
     *  from the specified {@code Scheduler}.</dd>
     * </dl>
     *
     * @param scheduler
     *            the {@code Scheduler} used to compute time intervals
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code scheduler} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/timeinterval.html">ReactiveX operators documentation: TimeInterval</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE) // Supplied scheduler is only used for creating timestamps.
    @NonNull
    public final Observable<Timed<T>> timeInterval(@NonNull Scheduler scheduler) {
        return timeInterval(TimeUnit.MILLISECONDS, scheduler);
    }

    /**
     * Returns an {@code Observable} that emits records of the time interval between consecutive items emitted by the
     * current {@code Observable}.
     * <p>
     * <img width="640" height="310" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/timeInterval.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code timeInterval} does not operate on any particular scheduler but uses the current time
     *  from the {@code computation} {@link Scheduler}.</dd>
     * </dl>
     *
     * @param unit the time unit for the current time
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code unit} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/timeinterval.html">ReactiveX operators documentation: TimeInterval</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Observable<Timed<T>> timeInterval(@NonNull TimeUnit unit) {
        return timeInterval(unit, Schedulers.computation());
    }

    /**
     * Returns an {@code Observable} that emits records of the time interval between consecutive items emitted by the
     * current {@code Observable}, where this interval is computed on a specified {@link Scheduler}.
     * <p>
     * <img width="640" height="315" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/timeInterval.s.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>The operator does not operate on any particular scheduler but uses the current time
     *  from the specified {@code Scheduler}.</dd>
     * </dl>
     *
     * @param unit the time unit for the current time
     * @param scheduler
     *            the {@code Scheduler} used to compute time intervals
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code unit} or {@code scheduler} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/timeinterval.html">ReactiveX operators documentation: TimeInterval</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE) // Supplied scheduler is only used for creating timestamps.
    @NonNull
    public final Observable<Timed<T>> timeInterval(@NonNull TimeUnit unit, @NonNull Scheduler scheduler) {
        Objects.requireNonNull(unit, "unit is null");
        Objects.requireNonNull(scheduler, "scheduler is null");
        return RxJavaPlugins.onAssembly(new ObservableTimeInterval<>(this, unit, scheduler));
    }

    /**
     * Returns an {@code Observable} that mirrors the current {@code Observable}, but notifies observers of a
     * {@link TimeoutException} if an item emitted by the current {@code Observable} doesn't arrive within a window of
     * time after the emission of the previous item, where that period of time is measured by an {@link ObservableSource} that
     * is a function of the previous item.
     * <p>
     * <img width="640" height="400" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/timeout3.v3.png" alt="">
     * <p>
     * Note: The arrival of the first source item is never timed out.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This version of {@code timeout} operates by default on the {@code immediate} {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <V>
     *            the timeout value type (ignored)
     * @param itemTimeoutIndicator
     *            a function that returns an {@code ObservableSource} for each item emitted by the current
     *            {@code Observable} and that determines the timeout window for the subsequent item
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code itemTimeoutIndicator} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/timeout.html">ReactiveX operators documentation: Timeout</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <@NonNull V> Observable<T> timeout(@NonNull Function<? super T, ? extends ObservableSource<V>> itemTimeoutIndicator) {
        return timeout0(null, itemTimeoutIndicator, null);
    }

    /**
     * Returns an {@code Observable} that mirrors the current {@code Observable}, but that switches to a fallback {@link ObservableSource} if
     * an item emitted by the current {@code Observable} doesn't arrive within a window of time after the emission of the
     * previous item, where that period of time is measured by an {@code ObservableSource} that is a function of the previous
     * item.
     * <p>
     * <img width="640" height="400" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/timeout4.v3.png" alt="">
     * <p>
     * Note: The arrival of the first source item is never timed out.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This version of {@code timeout} operates by default on the {@code immediate} {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <V>
     *            the timeout value type (ignored)
     * @param itemTimeoutIndicator
     *            a function that returns an {@code ObservableSource}, for each item emitted by the current {@code Observable}, that
     *            determines the timeout window for the subsequent item
     * @param fallback
     *            the fallback {@code ObservableSource} to switch to if the current {@code Observable} times out
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code itemTimeoutIndicator} or {@code fallback} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/timeout.html">ReactiveX operators documentation: Timeout</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <@NonNull V> Observable<T> timeout(@NonNull Function<? super T, ? extends ObservableSource<V>> itemTimeoutIndicator,
            @NonNull ObservableSource<? extends T> fallback) {
        Objects.requireNonNull(fallback, "fallback is null");
        return timeout0(null, itemTimeoutIndicator, fallback);
    }

    /**
     * Returns an {@code Observable} that mirrors the current {@code Observable} but applies a timeout policy for each emitted
     * item. If the next item isn't emitted within the specified timeout duration starting from its predecessor,
     * the resulting {@code Observable} terminates and notifies observers of a {@link TimeoutException}.
     * <p>
     * <img width="640" height="305" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/timeout.1.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This version of {@code timeout} operates by default on the {@code computation} {@link Scheduler}.</dd>
     * </dl>
     *
     * @param timeout
     *            maximum duration between emitted items before a timeout occurs
     * @param unit
     *            the unit of time that applies to the {@code timeout} argument.
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code unit} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/timeout.html">ReactiveX operators documentation: Timeout</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.COMPUTATION)
    @NonNull
    public final Observable<T> timeout(long timeout, @NonNull TimeUnit unit) {
        return timeout0(timeout, unit, null, Schedulers.computation());
    }

    /**
     * Returns an {@code Observable} that mirrors the current {@code Observable} but applies a timeout policy for each emitted
     * item. If the next item isn't emitted within the specified timeout duration starting from its predecessor,
     * the current {@code Observable} is disposed and the resulting {@code Observable} begins instead
     * to mirror a fallback {@link ObservableSource}.
     * <p>
     * <img width="640" height="305" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/timeout.2.v3.png" alt="">
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
     *            the fallback {@code ObservableSource} to use in case of a timeout
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code unit} or {@code fallback} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/timeout.html">ReactiveX operators documentation: Timeout</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.COMPUTATION)
    @NonNull
    public final Observable<T> timeout(long timeout, @NonNull TimeUnit unit, @NonNull ObservableSource<? extends T> fallback) {
        Objects.requireNonNull(fallback, "fallback is null");
        return timeout0(timeout, unit, fallback, Schedulers.computation());
    }

    /**
     * Returns an {@code Observable} that mirrors the current {@code Observable} but applies a timeout policy for each emitted
     * item using a specified {@link Scheduler}. If the next item isn't emitted within the specified timeout duration
     * starting from its predecessor, the current {@code Observable} is disposed and returned {@code Observable}
     * begins instead to mirror a fallback {@link ObservableSource}.
     * <p>
     * <img width="640" height="305" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/timeout.2s.v3.png" alt="">
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
     * @param fallback
     *            the {@code ObservableSource} to use as the fallback in case of a timeout
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code unit}, {@code scheduler} or {@code fallback} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/timeout.html">ReactiveX operators documentation: Timeout</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    @NonNull
    public final Observable<T> timeout(long timeout, @NonNull TimeUnit unit, @NonNull Scheduler scheduler, @NonNull ObservableSource<? extends T> fallback) {
        Objects.requireNonNull(fallback, "fallback is null");
        return timeout0(timeout, unit, fallback, scheduler);
    }

    /**
     * Returns an {@code Observable} that mirrors the current {@code Observable} but applies a timeout policy for each emitted
     * item, where this policy is governed on a specified {@link Scheduler}. If the next item isn't emitted within the
     * specified timeout duration starting from its predecessor, the resulting {@code Observable} terminates and
     * notifies observers of a {@link TimeoutException}.
     * <p>
     * <img width="640" height="305" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/timeout.1s.v3.png" alt="">
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
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code unit} or {@code scheduler} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/timeout.html">ReactiveX operators documentation: Timeout</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    @NonNull
    public final Observable<T> timeout(long timeout, @NonNull TimeUnit unit, @NonNull Scheduler scheduler) {
        return timeout0(timeout, unit, null, scheduler);
    }

    /**
     * Returns an {@code Observable} that mirrors the current {@code Observable}, but notifies observers of a
     * {@link TimeoutException} if either the first item emitted by the current {@code Observable} or any subsequent item
     * doesn't arrive within time windows defined by indicator {@link ObservableSource}s.
     * <p>
     * <img width="640" height="400" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/timeout5.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This version of {@code timeout} operates by default on the {@code immediate} {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <U>
     *            the first timeout value type (ignored)
     * @param <V>
     *            the subsequent timeout value type (ignored)
     * @param firstTimeoutIndicator
     *            a function that returns an {@code ObservableSource} that determines the timeout window for the first source
     *            item
     * @param itemTimeoutIndicator
     *            a function that returns an {@code ObservableSource} for each item emitted by the current {@code Observable} and that
     *            determines the timeout window in which the subsequent source item must arrive in order to
     *            continue the sequence
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code firstTimeoutIndicator} or {@code itemTimeoutIndicator} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/timeout.html">ReactiveX operators documentation: Timeout</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <@NonNull U, @NonNull V> Observable<T> timeout(@NonNull ObservableSource<U> firstTimeoutIndicator,
            @NonNull Function<? super T, ? extends ObservableSource<V>> itemTimeoutIndicator) {
        Objects.requireNonNull(firstTimeoutIndicator, "firstTimeoutIndicator is null");
        return timeout0(firstTimeoutIndicator, itemTimeoutIndicator, null);
    }

    /**
     * Returns an {@code Observable} that mirrors the current {@code Observable}, but switches to a fallback {@link ObservableSource} if either
     * the first item emitted by the current {@code Observable} or any subsequent item doesn't arrive within time windows
     * defined by indicator {@code ObservableSource}s.
     * <p>
     * <img width="640" height="400" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/timeout6.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This version of {@code timeout} operates by default on the {@code immediate} {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <U>
     *            the first timeout value type (ignored)
     * @param <V>
     *            the subsequent timeout value type (ignored)
     * @param firstTimeoutIndicator
     *            a function that returns an {@code ObservableSource} which determines the timeout window for the first source
     *            item
     * @param itemTimeoutIndicator
     *            a function that returns an {@code ObservableSource} for each item emitted by the current {@code Observable} and that
     *            determines the timeout window in which the subsequent source item must arrive in order to
     *            continue the sequence
     * @param fallback
     *            the fallback {@code ObservableSource} to switch to if the current {@code Observable} times out
     * @return the new {@code Observable} instance
     * @throws NullPointerException
     *             if {@code firstTimeoutIndicator}, {@code itemTimeoutIndicator} or {@code fallback} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/timeout.html">ReactiveX operators documentation: Timeout</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <@NonNull U, @NonNull V> Observable<T> timeout(
            @NonNull ObservableSource<U> firstTimeoutIndicator,
            @NonNull Function<? super T, ? extends ObservableSource<V>> itemTimeoutIndicator,
            @NonNull ObservableSource<? extends T> fallback) {
        Objects.requireNonNull(firstTimeoutIndicator, "firstTimeoutIndicator is null");
        Objects.requireNonNull(fallback, "fallback is null");
        return timeout0(firstTimeoutIndicator, itemTimeoutIndicator, fallback);
    }

    @NonNull
    private Observable<T> timeout0(long timeout, @NonNull TimeUnit unit,
            @Nullable ObservableSource<? extends T> fallback,
            @NonNull Scheduler scheduler) {
        Objects.requireNonNull(unit, "unit is null");
        Objects.requireNonNull(scheduler, "scheduler is null");
        return RxJavaPlugins.onAssembly(new ObservableTimeoutTimed<>(this, timeout, unit, scheduler, fallback));
    }

    @NonNull
    private <U, V> Observable<T> timeout0(
            @NonNull ObservableSource<U> firstTimeoutIndicator,
            @NonNull Function<? super T, ? extends ObservableSource<V>> itemTimeoutIndicator,
            @Nullable ObservableSource<? extends T> fallback) {
        Objects.requireNonNull(itemTimeoutIndicator, "itemTimeoutIndicator is null");
        return RxJavaPlugins.onAssembly(new ObservableTimeout<>(this, firstTimeoutIndicator, itemTimeoutIndicator, fallback));
    }

    /**
     * Returns an {@code Observable} that emits each item emitted by the current {@code Observable}, wrapped in a
     * {@link Timed} object.
     * <p>
     * <img width="640" height="310" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/timestamp.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code timestamp} does not operate on any particular scheduler but uses the current time
     *  from the {@code computation} {@link Scheduler}.</dd>
     * </dl>
     *
     * @return the new {@code Observable} instance
     * @see <a href="http://reactivex.io/documentation/operators/timestamp.html">ReactiveX operators documentation: Timestamp</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Observable<Timed<T>> timestamp() {
        return timestamp(TimeUnit.MILLISECONDS, Schedulers.computation());
    }

    /**
     * Returns an {@code Observable} that emits each item emitted by the current {@code Observable}, wrapped in a
     * {@link Timed} object whose timestamps are provided by a specified {@link Scheduler}.
     * <p>
     * <img width="640" height="310" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/timestamp.s.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This operator does not operate on any particular scheduler but uses the current time
     *  from the specified {@code Scheduler}.</dd>
     * </dl>
     *
     * @param scheduler
     *            the {@code Scheduler} to use as a time source
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code scheduler} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/timestamp.html">ReactiveX operators documentation: Timestamp</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE) // Supplied scheduler is only used for creating timestamps.
    @NonNull
    public final Observable<Timed<T>> timestamp(@NonNull Scheduler scheduler) {
        return timestamp(TimeUnit.MILLISECONDS, scheduler);
    }

    /**
     * Returns an {@code Observable} that emits each item emitted by the current {@code Observable}, wrapped in a
     * {@link Timed} object.
     * <p>
     * <img width="640" height="310" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/timestamp.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code timestamp} does not operate on any particular scheduler but uses the current time
     *  from the {@code computation} {@link Scheduler}.</dd>
     * </dl>
     *
     * @param unit the time unit for the current time
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code unit} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/timestamp.html">ReactiveX operators documentation: Timestamp</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Observable<Timed<T>> timestamp(@NonNull TimeUnit unit) {
        return timestamp(unit, Schedulers.computation());
    }

    /**
     * Returns an {@code Observable} that emits each item emitted by the current {@code Observable}, wrapped in a
     * {@link Timed} object whose timestamps are provided by a specified {@link Scheduler}.
     * <p>
     * <img width="640" height="310" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/timestamp.s.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This operator does not operate on any particular scheduler but uses the current time
     *  from the specified {@code Scheduler}.</dd>
     * </dl>
     *
     * @param unit the time unit for the current time
     * @param scheduler
     *            the {@code Scheduler} to use as a time source
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code unit} or {@code scheduler} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/timestamp.html">ReactiveX operators documentation: Timestamp</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE) // Supplied scheduler is only used for creating timestamps.
    @NonNull
    public final Observable<Timed<T>> timestamp(@NonNull TimeUnit unit, @NonNull Scheduler scheduler) {
        Objects.requireNonNull(unit, "unit is null");
        Objects.requireNonNull(scheduler, "scheduler is null");
        return map(Functions.timestampWith(unit, scheduler));
    }

    /**
     * Calls the specified converter function during assembly time and returns its resulting value.
     * <p>
     * This allows fluent conversion to any other type.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code to} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * <p>History: 2.1.7 - experimental
     * @param <R> the resulting object type
     * @param converter the function that receives the current {@code Observable} instance and returns a value
     * @return the converted value
     * @throws NullPointerException if {@code converter} is {@code null}
     * @since 2.2
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <@NonNull R> R to(@NonNull ObservableConverter<T, ? extends R> converter) {
        return Objects.requireNonNull(converter, "converter is null").apply(this);
    }

    /**
     * Returns a {@link Single} that emits a single item, a {@link List} composed of all the items emitted by the
     * current and finite {@code Observable}.
     * <p>
     * <img width="640" height="305" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/toList.2.v3.png" alt="">
     * <p>
     * Normally, an {@link ObservableSource} that returns multiple items will do so by invoking its {@link Observer}'s
     * {@link Observer#onNext onNext} method for each such item. You can change this behavior by having the
     * operator to compose a list of all of these items and then to invoke the {@link SingleObserver}'s {@code onSuccess}
     * method once, passing it the entire list, by calling the {@code Observable}'s {@code toList} method prior to
     * calling its {@link #subscribe} method.
     * <p>
     * Note that this operator requires the upstream to signal {@code onComplete} for the accumulated list to
     * be emitted. Sources that are infinite and never complete will never emit anything through this
     * operator and an infinite source may lead to a fatal {@link OutOfMemoryError}.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code toList} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @return the new {@code Single} instance
     * @see <a href="http://reactivex.io/documentation/operators/to.html">ReactiveX operators documentation: To</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Single<@NonNull List<T>> toList() {
        return toList(16);
    }

    /**
     * Returns a {@link Single} that emits a single item, a {@link List} composed of all the items emitted by the
     * current and finite {@code Observable}.
     * <p>
     * <img width="640" height="305" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/toList.2.v3.png" alt="">
     * <p>
     * Normally, an {@link ObservableSource} that returns multiple items will do so by invoking its {@link Observer}'s
     * {@link Observer#onNext onNext} method for each such item. You can change this behavior by having the
     * operator to compose a list of all of these items and then to invoke the {@link SingleObserver}'s {@code onSuccess}
     * method once, passing it the entire list, by calling the {@code Observable}'s {@code toList} method prior to
     * calling its {@link #subscribe} method.
     * <p>
     * Note that this operator requires the upstream to signal {@code onComplete} for the accumulated list to
     * be emitted. Sources that are infinite and never complete will never emit anything through this
     * operator and an infinite source may lead to a fatal {@link OutOfMemoryError}.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code toList} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param capacityHint
     *         the number of elements expected from the current {@code Observable}
     * @return the new {@code Single} instance
     * @throws IllegalArgumentException if {@code capacityHint} is non-positive
     * @see <a href="http://reactivex.io/documentation/operators/to.html">ReactiveX operators documentation: To</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Single<@NonNull List<T>> toList(int capacityHint) {
        ObjectHelper.verifyPositive(capacityHint, "capacityHint");
        return RxJavaPlugins.onAssembly(new ObservableToListSingle<>(this, capacityHint));
    }

    /**
     * Returns a {@link Single} that emits a single item, a {@link Collection} (subclass) composed of all the items emitted by the
     * finite upstream {@code Observable}.
     * <p>
     * <img width="640" height="365" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/toList.o.c.png" alt="">
     * <p>
     * Normally, an {@link ObservableSource} that returns multiple items will do so by invoking its {@link Observer}'s
     * {@link Observer#onNext onNext} method for each such item. You can change this behavior by having the
     * operator to compose a collection of all of these items and then to invoke the {@link SingleObserver}'s {@code onSuccess}
     * method once, passing it the entire collection, by calling the {@code Observable}'s {@code toList} method prior to
     * calling its {@link #subscribe} method.
     * <p>
     * Note that this operator requires the upstream to signal {@code onComplete} for the accumulated collection to
     * be emitted. Sources that are infinite and never complete will never emit anything through this
     * operator and an infinite source may lead to a fatal {@link OutOfMemoryError}.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code toList} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <U> the subclass of a collection of Ts
     * @param collectionSupplier
     *               the {@link Supplier} returning the collection (for each individual {@code Observer}) to be filled in
     * @return the new {@code Single} instance
     * @throws NullPointerException if {@code collectionSupplier} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/to.html">ReactiveX operators documentation: To</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <@NonNull U extends Collection<? super T>> Single<U> toList(@NonNull Supplier<U> collectionSupplier) {
        Objects.requireNonNull(collectionSupplier, "collectionSupplier is null");
        return RxJavaPlugins.onAssembly(new ObservableToListSingle<>(this, collectionSupplier));
    }

    /**
     * Returns a {@link Single} that emits a single {@link HashMap} containing all items emitted by the
     * current and finite {@code Observable}, mapped by the keys returned by a specified
     * {@code keySelector} function.
     * <p>
     * <img width="640" height="305" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/toMap.2.v3.png" alt="">
     * <p>
     * If more than one source item maps to the same key, the {@code HashMap} will contain the latest of those items.
     * <p>
     * Note that this operator requires the upstream to signal {@code onComplete} for the accumulated {@code HashMap} to
     * be emitted. Sources that are infinite and never complete will never emit anything through this
     * operator and an infinite source may lead to a fatal {@link OutOfMemoryError}.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code toMap} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <K> the key type of the Map
     * @param keySelector
     *            the function that extracts the key from a source item to be used in the {@code HashMap}
     * @return the new {@code Single} instance
     * @throws NullPointerException if {@code keySelector} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/to.html">ReactiveX operators documentation: To</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <@NonNull K> Single<@NonNull Map<K, T>> toMap(@NonNull Function<? super T, ? extends K> keySelector) {
        Objects.requireNonNull(keySelector, "keySelector is null");
        return collect(HashMapSupplier.asSupplier(), Functions.toMapKeySelector(keySelector));
    }

    /**
     * Returns a {@link Single} that emits a single {@link HashMap} containing values corresponding to items emitted by the
     * current and finite {@code Observable}, mapped by the keys and values returned by the given selector functions.
     * <p>
     * <img width="640" height="305" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/toMap.2.v3.png" alt="">
     * <p>
     * If more than one source item maps to the same key, the {@code HashMap} will contain a single entry that
     * corresponds to the latest of those items.
     * <p>
     * Note that this operator requires the upstream to signal {@code onComplete} for the accumulated {@code HashMap} to
     * be emitted. Sources that are infinite and never complete will never emit anything through this
     * operator and an infinite source may lead to a fatal {@link OutOfMemoryError}.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code toMap} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <K> the key type of the {@code HashMap}
     * @param <V> the value type of the {@code HashMap}
     * @param keySelector
     *            the function that extracts the key from a source item to be used in the {@code HashMap}
     * @param valueSelector
     *            the function that extracts the value from a source item to be used in the {@code HashMap}
     * @return the new {@code Single} instance
     * @throws NullPointerException if {@code keySelector} or {@code valueSelector} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/to.html">ReactiveX operators documentation: To</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <@NonNull K, @NonNull V> Single<Map<K, V>> toMap(
            @NonNull Function<? super T, ? extends K> keySelector,
            @NonNull Function<? super T, ? extends V> valueSelector) {
        Objects.requireNonNull(keySelector, "keySelector is null");
        Objects.requireNonNull(valueSelector, "valueSelector is null");
        return collect(HashMapSupplier.asSupplier(), Functions.toMapKeyValueSelector(keySelector, valueSelector));
    }

    /**
     * Returns a {@link Single} that emits a single {@link Map} (subclass), returned by a specified {@code mapFactory} function, that
     * contains keys and values extracted from the items, via selector functions, emitted by the current and finite {@code Observable}.
     * <p>
     * <img width="640" height="305" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/toMap.2.v3.png" alt="">
     * <p>
     * Note that this operator requires the upstream to signal {@code onComplete} for the accumulated {@code Map} to
     * be emitted. Sources that are infinite and never complete will never emit anything through this
     * operator and an infinite source may lead to a fatal {@link OutOfMemoryError}.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code toMap} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <K> the key type of the {@code Map}
     * @param <V> the value type of the {@code Map}
     * @param keySelector
     *            the function that extracts the key from a source item to be used in the {@code Map}
     * @param valueSelector
     *            the function that extracts the value from the source items to be used as value in the {@code Map}
     * @param mapSupplier
     *            the function that returns a {@code Map} instance to be used
     * @return the new {@code Single} instance
     * @throws NullPointerException if {@code keySelector}, {@code valueSelector} or {@code mapSupplier} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/to.html">ReactiveX operators documentation: To</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <@NonNull K, @NonNull V> Single<Map<K, V>> toMap(
            @NonNull Function<? super T, ? extends K> keySelector,
            @NonNull Function<? super T, ? extends V> valueSelector,
            @NonNull Supplier<? extends Map<K, V>> mapSupplier) {
        Objects.requireNonNull(keySelector, "keySelector is null");
        Objects.requireNonNull(valueSelector, "valueSelector is null");
        Objects.requireNonNull(mapSupplier, "mapSupplier is null");
        return collect(mapSupplier, Functions.toMapKeyValueSelector(keySelector, valueSelector));
    }

    /**
     * Returns a {@link Single} that emits a single {@link HashMap} that contains an {@link ArrayList} of items emitted by the
     * current and finite {@code Observable} keyed by a specified {@code keySelector} function.
     * <p>
     * <img width="640" height="305" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/toMultiMap.2.v3.png" alt="">
     * <p>
     * Note that this operator requires the upstream to signal {@code onComplete} for the accumulated {@code HashMap} to
     * be emitted. Sources that are infinite and never complete will never emit anything through this
     * operator and an infinite source may lead to a fatal {@link OutOfMemoryError}.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code toMultimap} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <K> the key type of the {@code HashMap}
     * @param keySelector
     *            the function that extracts the key from the source items to be used as key in the {@code HashMap}
     * @return the new {@code Single} instance
     * @throws NullPointerException if {@code keySelector} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/to.html">ReactiveX operators documentation: To</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <@NonNull K> Single<@NonNull Map<K, Collection<T>>> toMultimap(@NonNull Function<? super T, ? extends K> keySelector) {
        Function<? super T, ? extends T> valueSelector = Functions.identity();
        Supplier<Map<K, Collection<T>>> mapSupplier = HashMapSupplier.asSupplier();
        Function<K, List<T>> collectionFactory = ArrayListSupplier.asFunction();
        return toMultimap(keySelector, valueSelector, mapSupplier, collectionFactory);
    }

    /**
     * Returns a {@link Single} that emits a single {@link HashMap} that contains an {@link ArrayList} of values extracted by a
     * specified {@code valueSelector} function from items emitted by the current and finite {@code Observable},
     * keyed by a specified {@code keySelector} function.
     * <p>
     * <img width="640" height="305" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/toMultiMap.2.v3.png" alt="">
     * <p>
     * Note that this operator requires the upstream to signal {@code onComplete} for the accumulated {@code HashMap} to
     * be emitted. Sources that are infinite and never complete will never emit anything through this
     * operator and an infinite source may lead to a fatal {@link OutOfMemoryError}.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code toMultimap} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <K> the key type of the {@code HashMap}
     * @param <V> the value type of the {@code HashMap}
     * @param keySelector
     *            the function that extracts a key from the source items to be used as key in the {@code HashMap}
     * @param valueSelector
     *            the function that extracts a value from the source items to be used as value in the {@code HashMap}
     * @return the new {@code Single} instance
     * @throws NullPointerException if {@code keySelector} or {@code valueSelector} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/to.html">ReactiveX operators documentation: To</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <@NonNull K, @NonNull V> Single<@NonNull Map<K, Collection<V>>> toMultimap(@NonNull Function<? super T, ? extends K> keySelector, Function<? super T, ? extends V> valueSelector) {
        Supplier<Map<K, Collection<V>>> mapSupplier = HashMapSupplier.asSupplier();
        Function<K, List<V>> collectionFactory = ArrayListSupplier.asFunction();
        return toMultimap(keySelector, valueSelector, mapSupplier, collectionFactory);
    }

    /**
     * Returns a {@link Single} that emits a single {@code Map} (subclass), returned by a specified {@code mapFactory} function, that
     * contains a custom {@link Collection} of values, extracted by a specified {@code valueSelector} function from
     * items emitted by the current and finite {@code Observable}, and keyed by the {@code keySelector} function.
     * <p>
     * <img width="640" height="305" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/toMultiMap.2.v3.png" alt="">
     * <p>
     * Note that this operator requires the upstream to signal {@code onComplete} for the accumulated {@code Map} to
     * be emitted. Sources that are infinite and never complete will never emit anything through this
     * operator and an infinite source may lead to a fatal {@link OutOfMemoryError}.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code toMultimap} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <K> the key type of the {@code Map}
     * @param <V> the value type of the {@code Map}
     * @param keySelector
     *            the function that extracts a key from the source items to be used as the key in the {@code Map}
     * @param valueSelector
     *            the function that extracts a value from the source items to be used as the value in the {@code Map}
     * @param mapSupplier
     *            the function that returns a {@code Map} instance to be used
     * @param collectionFactory
     *            the function that returns a {@code Collection} instance for a particular key to be used in the {@code Map}
     * @return the new {@code Single} instance
     * @throws NullPointerException if {@code keySelector}, {@code valueSelector}, {@code mapSupplier} or {@code collectionFactory} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/to.html">ReactiveX operators documentation: To</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <@NonNull K, @NonNull V> Single<@NonNull Map<K, Collection<V>>> toMultimap(
            @NonNull Function<? super T, ? extends K> keySelector,
            @NonNull Function<? super T, ? extends V> valueSelector,
            @NonNull Supplier<? extends Map<K, Collection<V>>> mapSupplier,
            @NonNull Function<? super K, ? extends Collection<? super V>> collectionFactory) {
        Objects.requireNonNull(keySelector, "keySelector is null");
        Objects.requireNonNull(valueSelector, "valueSelector is null");
        Objects.requireNonNull(mapSupplier, "mapSupplier is null");
        Objects.requireNonNull(collectionFactory, "collectionFactory is null");
        return collect(mapSupplier, Functions.toMultimapKeyValueSelector(keySelector, valueSelector, collectionFactory));
    }

    /**
     * Returns a {@link Single} that emits a single {@link Map} (subclass), returned by a specified {@code mapFactory} function, that
     * contains an {@link ArrayList} of values, extracted by a specified {@code valueSelector} function from items
     * emitted by the current and finite {@code Observable} and keyed by the {@code keySelector} function.
     * <p>
     * <img width="640" height="305" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/toMultiMap.2.v3.png" alt="">
     * <p>
     * Note that this operator requires the upstream to signal {@code onComplete} for the accumulated {@code Map} to
     * be emitted. Sources that are infinite and never complete will never emit anything through this
     * operator and an infinite source may lead to a fatal {@link OutOfMemoryError}.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code toMultimap} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <K> the key type of the {@code Map}
     * @param <V> the value type of the {@code Map}
     * @param keySelector
     *            the function that extracts a key from the source items to be used as the key in the {@code Map}
     * @param valueSelector
     *            the function that extracts a value from the source items to be used as the value in the {@code Map}
     * @param mapSupplier
     *            the function that returns a {@code Map} instance to be used
     * @return the new {@code Single} instance
     * @throws NullPointerException if {@code keySelector}, {@code valueSelector} or {@code mapSupplier} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/to.html">ReactiveX operators documentation: To</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <@NonNull K, @NonNull V> Single<@NonNull Map<K, Collection<V>>> toMultimap(
            @NonNull Function<? super T, ? extends K> keySelector,
            @NonNull Function<? super T, ? extends V> valueSelector,
            @NonNull Supplier<Map<K, Collection<V>>> mapSupplier
    ) {
        return toMultimap(keySelector, valueSelector, mapSupplier, ArrayListSupplier.asFunction());
    }

    /**
     * Converts the current {@code Observable} into a {@link Flowable} by applying the specified backpressure strategy.
     * <p>
     * Marble diagrams for the various backpressure strategies are as follows:
     * <ul>
     * <li>{@link BackpressureStrategy#BUFFER}
     * <p>
     * <img width="640" height="274" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/toFlowable.o.buffer.png" alt="">
     * </li>
     * <li>{@link BackpressureStrategy#DROP}
     * <p>
     * <img width="640" height="389" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/toFlowable.o.drop.png" alt="">
     * </li>
     * <li>{@link BackpressureStrategy#LATEST}
     * <p>
     * <img width="640" height="297" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/toFlowable.o.latest.png" alt="">
     * </li>
     * <li>{@link BackpressureStrategy#ERROR}
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/toFlowable.o.error.png" alt="">
     * </li>
     * <li>{@link BackpressureStrategy#MISSING}
     * <p>
     * <img width="640" height="412" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/toFlowable.o.missing.png" alt="">
     * </li>
     * </ul>
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The operator applies the chosen backpressure strategy of {@link BackpressureStrategy} enum.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code toFlowable} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param strategy the backpressure strategy to apply
     * @return the new {@code Flowable} instance
     * @throws NullPointerException if {@code strategy} is {@code null}
     */
    @BackpressureSupport(BackpressureKind.SPECIAL)
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Flowable<T> toFlowable(@NonNull BackpressureStrategy strategy) {
        Objects.requireNonNull(strategy, "strategy is null");
        Flowable<T> f = new FlowableFromObservable<>(this);
        switch (strategy) {
            case DROP:
                return f.onBackpressureDrop();
            case LATEST:
                return f.onBackpressureLatest();
            case MISSING:
                return f;
            case ERROR:
                return RxJavaPlugins.onAssembly(new FlowableOnBackpressureError<>(f));
            default:
                return f.onBackpressureBuffer();
        }
    }

    /**
     * Returns a {@link Single} that emits a {@link List} that contains the items emitted by the current and finite {@code Observable}, in a
     * sorted order. Each item emitted by the current {@code Observable} must implement {@link Comparable} with respect to all
     * other items in the sequence.
     *
     * <p>
     * If any item emitted by the current {@code Observable} does not implement {@code Comparable} with respect to
     * all other items emitted by the current {@code Observable}, no items will be emitted and the
     * sequence is terminated with a {@link ClassCastException}.
     * <p>
     * <img width="640" height="310" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/toSortedList.2.v3.png" alt="">
     * <p>
     * Note that this operator requires the upstream to signal {@code onComplete} for the accumulated {@code List} to
     * be emitted. Sources that are infinite and never complete will never emit anything through this
     * operator and an infinite source may lead to a fatal {@link OutOfMemoryError}.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code toSortedList} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @return the new {@code Single} instance
     * @see <a href="http://reactivex.io/documentation/operators/to.html">ReactiveX operators documentation: To</a>
     * @see #toSortedList(int)
     * @see #toSortedList(Comparator)
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Single<@NonNull List<T>> toSortedList() {
        return toSortedList(Functions.naturalComparator());
    }

    /**
     * Returns a {@link Single} that emits a {@link List} that contains the items emitted by the current and finite {@code Observable}, in a
     * sorted order based on a specified comparison function.
     * <p>
     * <img width="640" height="310" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/toSortedList.f.2.v3.png" alt="">
     * <p>
     * Note that this operator requires the upstream to signal {@code onComplete} for the accumulated {@code List} to
     * be emitted. Sources that are infinite and never complete will never emit anything through this
     * operator and an infinite source may lead to a fatal {@link OutOfMemoryError}.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code toSortedList} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param comparator
     *            a function that compares two items emitted by the current {@code Observable} and returns an {@code int}
     *            that indicates their sort order
     * @return the new {@code Single} instance
     * @throws NullPointerException if {@code comparator} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/to.html">ReactiveX operators documentation: To</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Single<@NonNull List<T>> toSortedList(@NonNull Comparator<? super T> comparator) {
        Objects.requireNonNull(comparator, "comparator is null");
        return toList().map(Functions.listSorter(comparator));
    }

    /**
     * Returns a {@link Single} that emits a {@link List} that contains the items emitted by the current and finite {@code Observable}, in a
     * sorted order based on a specified comparison function.
     * <p>
     * <img width="640" height="310" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/toSortedList.f.2.v3.png" alt="">
     * <p>
     * Note that this operator requires the upstream to signal {@code onComplete} for the accumulated {@code List} to
     * be emitted. Sources that are infinite and never complete will never emit anything through this
     * operator and an infinite source may lead to a fatal {@link OutOfMemoryError}.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code toSortedList} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param comparator
     *            a function that compares two items emitted by the current {@code Observable} and returns an {@code int}
     *            that indicates their sort order
     * @param capacityHint
     *             the initial capacity of the {@code List} used to accumulate items before sorting
     * @return the new {@code Single} instance
     * @throws NullPointerException if {@code comparator} is {@code null}
     * @throws IllegalArgumentException if {@code capacityHint} is non-positive
     * @see <a href="http://reactivex.io/documentation/operators/to.html">ReactiveX operators documentation: To</a>
     * @since 2.0
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Single<@NonNull List<T>> toSortedList(@NonNull Comparator<? super T> comparator, int capacityHint) {
        Objects.requireNonNull(comparator, "comparator is null");
        return toList(capacityHint).map(Functions.listSorter(comparator));
    }

    /**
     * Returns a {@link Single} that emits a {@link List} that contains the items emitted by the current and finite {@code Observable}, in a
     * sorted order. Each item emitted by the current {@code Observable} must implement {@link Comparable} with respect to all
     * other items in the sequence.
     * <p>
     * If any item emitted by the current {@code Observable} does not implement {@code Comparable} with respect to
     * all other items emitted by the current {@code Observable}, no items will be emitted and the
     * sequence is terminated with a {@link ClassCastException}.
     * <p>
     * <img width="640" height="310" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/toSortedList.2.v3.png" alt="">
     * <p>
     * Note that this operator requires the upstream to signal {@code onComplete} for the accumulated {@code List} to
     * be emitted. Sources that are infinite and never complete will never emit anything through this
     * operator and an infinite source may lead to a fatal {@link OutOfMemoryError}.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code toSortedList} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param capacityHint
     *             the initial capacity of the {@code List} used to accumulate items before sorting
     * @return the new {@code Single} instance
     * @throws IllegalArgumentException if {@code capacityHint} is non-positive
     * @see <a href="http://reactivex.io/documentation/operators/to.html">ReactiveX operators documentation: To</a>
     * @since 2.0
     * @see #toSortedList(Comparator, int)
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Single<@NonNull List<T>> toSortedList(int capacityHint) {
        return toSortedList(Functions.naturalComparator(), capacityHint);
    }

    /**
     * Return an {@code Observable} that schedules the downstream {@link Observer}s' {@code dispose} calls
     * aimed at the current {@code Observable} on the given {@link Scheduler}.
     * <p>
     * <img width="640" height="453" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/unsubscribeOn.o.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>You specify which {@code Scheduler} this operator will use.</dd>
     * </dl>
     *
     * @param scheduler
     *            the {@code Scheduler} to perform the call to {@code dispose()} of the upstream {@link Disposable}
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code scheduler} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/subscribeon.html">ReactiveX operators documentation: SubscribeOn</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    @NonNull
    public final Observable<T> unsubscribeOn(@NonNull Scheduler scheduler) {
        Objects.requireNonNull(scheduler, "scheduler is null");
        return RxJavaPlugins.onAssembly(new ObservableUnsubscribeOn<>(this, scheduler));
    }

    /**
     * Returns an {@code Observable} that emits windows of items it collects from the current {@code Observable}. The resulting
     * {@code Observable} emits connected, non-overlapping windows, each containing {@code count} items. When the current
     * {@code Observable} completes or encounters an error, the resulting {@code Observable} emits the current window and
     * propagates the notification from the current {@code Observable}.
     * <p>
     * <img width="640" height="400" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/window3.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This version of {@code window} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param count
     *            the maximum size of each window before it should be emitted
     * @return the new {@code Observable} instance
     * @throws IllegalArgumentException if {@code count} is non-positive
     * @see <a href="http://reactivex.io/documentation/operators/window.html">ReactiveX operators documentation: Window</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Observable<Observable<T>> window(long count) {
        return window(count, count, bufferSize());
    }

    /**
     * Returns an {@code Observable} that emits windows of items it collects from the current {@code Observable}. The resulting
     * {@code Observable} emits windows every {@code skip} items, each containing no more than {@code count} items. When
     * the current {@code Observable} completes or encounters an error, the resulting {@code Observable} emits the current window
     * and propagates the notification from the current {@code Observable}.
     * <p>
     * <img width="640" height="365" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/window4.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This version of {@code window} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param count
     *            the maximum size of each window before it should be emitted
     * @param skip
     *            how many items need to be skipped before starting a new window. Note that if {@code skip} and
     *            {@code count} are equal this is the same operation as {@link #window(long)}.
     * @return the new {@code Observable} instance
     * @throws IllegalArgumentException if {@code count} or {@code skip} is non-positive
     * @see <a href="http://reactivex.io/documentation/operators/window.html">ReactiveX operators documentation: Window</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Observable<Observable<T>> window(long count, long skip) {
        return window(count, skip, bufferSize());
    }

    /**
     * Returns an {@code Observable} that emits windows of items it collects from the current {@code Observable}. The resulting
     * {@code Observable} emits windows every {@code skip} items, each containing no more than {@code count} items. When
     * the current {@code Observable} completes or encounters an error, the resulting {@code Observable} emits the current window
     * and propagates the notification from the current {@code Observable}.
     * <p>
     * <img width="640" height="365" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/window4.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This version of {@code window} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param count
     *            the maximum size of each window before it should be emitted
     * @param skip
     *            how many items need to be skipped before starting a new window. Note that if {@code skip} and
     *            {@code count} are equal this is the same operation as {@link #window(long)}.
     * @param bufferSize
     *            the capacity hint for the buffer in the inner windows
     * @return the new {@code Observable} instance
     * @throws IllegalArgumentException if {@code count}, {@code skip} or {@code bufferSize} is non-positive
     * @see <a href="http://reactivex.io/documentation/operators/window.html">ReactiveX operators documentation: Window</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Observable<Observable<T>> window(long count, long skip, int bufferSize) {
        ObjectHelper.verifyPositive(count, "count");
        ObjectHelper.verifyPositive(skip, "skip");
        ObjectHelper.verifyPositive(bufferSize, "bufferSize");
        return RxJavaPlugins.onAssembly(new ObservableWindow<>(this, count, skip, bufferSize));
    }

    /**
     * Returns an {@code Observable} that emits windows of items it collects from the current {@code Observable}. The resulting
     * {@code Observable} starts a new window periodically, as determined by the {@code timeskip} argument. It emits
     * each window after a fixed timespan, specified by the {@code timespan} argument. When the current
     * {@code Observable} completes or encounters an error, the resulting {@code Observable} emits the
     * current window and propagates the notification from the current {@code Observable}.
     * <p>
     * <img width="640" height="335" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/window7.v3.png" alt="">
     * <p>
     * Note that ignoring windows or subscribing later (i.e., on another thread) will result in
     * so-called window abandonment where a window may not contain any elements. In this case, subsequent
     * elements will be dropped until the condition for the next window boundary is satisfied. The behavior is
     * a trade-off for ensuring upstream cancellation can happen under some race conditions.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This version of {@code window} operates by default on the {@code computation} {@link Scheduler}.</dd>
     * </dl>
     *
     * @param timespan
     *            the period of time each window collects items before it should be emitted
     * @param timeskip
     *            the period of time after which a new window will be created
     * @param unit
     *            the unit of time that applies to the {@code timespan} and {@code timeskip} arguments
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code unit} is {@code null}
     * @throws IllegalArgumentException if {@code timespan} or {@code timeskip} is non-positive
     * @see <a href="http://reactivex.io/documentation/operators/window.html">ReactiveX operators documentation: Window</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.COMPUTATION)
    @NonNull
    public final Observable<Observable<T>> window(long timespan, long timeskip, @NonNull TimeUnit unit) {
        return window(timespan, timeskip, unit, Schedulers.computation(), bufferSize());
    }

    /**
     * Returns an {@code Observable} that emits windows of items it collects from the current {@code Observable}. The resulting
     * {@code Observable} starts a new window periodically, as determined by the {@code timeskip} argument. It emits
     * each window after a fixed timespan, specified by the {@code timespan} argument. When the current
     * {@code Observable} completes or encounters an error, the resulting {@code Observable} emits the
     * current window and propagates the notification from the current {@code Observable}.
     * <p>
     * <img width="640" height="335" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/window7.s.v3.png" alt="">
     * <p>
     * Note that ignoring windows or subscribing later (i.e., on another thread) will result in
     * so-called window abandonment where a window may not contain any elements. In this case, subsequent
     * elements will be dropped until the condition for the next window boundary is satisfied. The behavior is
     * a trade-off for ensuring upstream cancellation can happen under some race conditions.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>You specify which {@link Scheduler} this operator will use.</dd>
     * </dl>
     *
     * @param timespan
     *            the period of time each window collects items before it should be emitted
     * @param timeskip
     *            the period of time after which a new window will be created
     * @param unit
     *            the unit of time that applies to the {@code timespan} and {@code timeskip} arguments
     * @param scheduler
     *            the {@code Scheduler} to use when determining the end and start of a window
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code unit} or {@code scheduler} is {@code null}
     * @throws IllegalArgumentException if {@code timespan} or {@code timeskip} is non-positive
     * @see <a href="http://reactivex.io/documentation/operators/window.html">ReactiveX operators documentation: Window</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    @NonNull
    public final Observable<Observable<T>> window(long timespan, long timeskip, @NonNull TimeUnit unit, @NonNull Scheduler scheduler) {
        return window(timespan, timeskip, unit, scheduler, bufferSize());
    }

    /**
     * Returns an {@code Observable} that emits windows of items it collects from the current {@code Observable}. The resulting
     * {@code Observable} starts a new window periodically, as determined by the {@code timeskip} argument. It emits
     * each window after a fixed timespan, specified by the {@code timespan} argument. When the current
     * {@code Observable} completes or encounters an error, the resulting {@code Observable} emits the
     * current window and propagates the notification from the current {@code Observable}.
     * <p>
     * <img width="640" height="335" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/window7.s.v3.png" alt="">
     * <p>
     * Note that ignoring windows or subscribing later (i.e., on another thread) will result in
     * so-called window abandonment where a window may not contain any elements. In this case, subsequent
     * elements will be dropped until the condition for the next window boundary is satisfied. The behavior is
     * a trade-off for ensuring upstream cancellation can happen under some race conditions.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>You specify which {@link Scheduler} this operator will use.</dd>
     * </dl>
     *
     * @param timespan
     *            the period of time each window collects items before it should be emitted
     * @param timeskip
     *            the period of time after which a new window will be created
     * @param unit
     *            the unit of time that applies to the {@code timespan} and {@code timeskip} arguments
     * @param scheduler
     *            the {@code Scheduler} to use when determining the end and start of a window
     * @param bufferSize
     *            the capacity hint for the buffer in the inner windows
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code unit} or {@code scheduler} is {@code null}
     * @throws IllegalArgumentException if {@code timespan}, {@code timeskip} or {@code bufferSize} is non-positive
     * @see <a href="http://reactivex.io/documentation/operators/window.html">ReactiveX operators documentation: Window</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    @NonNull
    public final Observable<Observable<T>> window(long timespan, long timeskip, @NonNull TimeUnit unit, @NonNull Scheduler scheduler, int bufferSize) {
        ObjectHelper.verifyPositive(timespan, "timespan");
        ObjectHelper.verifyPositive(timeskip, "timeskip");
        ObjectHelper.verifyPositive(bufferSize, "bufferSize");
        Objects.requireNonNull(scheduler, "scheduler is null");
        Objects.requireNonNull(unit, "unit is null");
        return RxJavaPlugins.onAssembly(new ObservableWindowTimed<>(this, timespan, timeskip, unit, scheduler, Long.MAX_VALUE, bufferSize, false));
    }

    /**
     * Returns an {@code Observable} that emits windows of items it collects from the current {@code Observable}. The resulting
     * {@code Observable} emits connected, non-overlapping windows, each of a fixed duration specified by the
     * {@code timespan} argument. When the current {@code Observable} completes or encounters an error, the resulting
     * {@code Observable} emits the current window and propagates the notification from the current {@code Observable}.
     * <p>
     * <img width="640" height="375" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/window5.v3.png" alt="">
     * <p>
     * Note that ignoring windows or subscribing later (i.e., on another thread) will result in
     * so-called window abandonment where a window may not contain any elements. In this case, subsequent
     * elements will be dropped until the condition for the next window boundary is satisfied. The behavior is
     * a trade-off for ensuring upstream cancellation can happen under some race conditions.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This version of {@code window} operates by default on the {@code computation} {@link Scheduler}.</dd>
     * </dl>
     *
     * @param timespan
     *            the period of time each window collects items before it should be emitted and replaced with a
     *            new window
     * @param unit
     *            the unit of time that applies to the {@code timespan} argument
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code unit} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/window.html">ReactiveX operators documentation: Window</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.COMPUTATION)
    @NonNull
    public final Observable<Observable<T>> window(long timespan, @NonNull TimeUnit unit) {
        return window(timespan, unit, Schedulers.computation(), Long.MAX_VALUE, false);
    }

    /**
     * Returns an {@code Observable} that emits windows of items it collects from the current {@code Observable}. The resulting
     * {@code Observable} emits connected, non-overlapping windows, each of a fixed duration as specified by the
     * {@code timespan} argument or a maximum size as specified by the {@code count} argument (whichever is
     * reached first). When the current {@code Observable} completes or encounters an error, the resulting {@code Observable}
     * emits the current window and propagates the notification from the current {@code Observable}.
     * <p>
     * <img width="640" height="370" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/window6.v3.png" alt="">
     * <p>
     * Note that ignoring windows or subscribing later (i.e., on another thread) will result in
     * so-called window abandonment where a window may not contain any elements. In this case, subsequent
     * elements will be dropped until the condition for the next window boundary is satisfied. The behavior is
     * a trade-off for ensuring upstream cancellation can happen under some race conditions.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This version of {@code window} operates by default on the {@code computation} {@link Scheduler}.</dd>
     * </dl>
     *
     * @param timespan
     *            the period of time each window collects items before it should be emitted and replaced with a
     *            new window
     * @param unit
     *            the unit of time that applies to the {@code timespan} argument
     * @param count
     *            the maximum size of each window before it should be emitted
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code unit} is {@code null}
     * @throws IllegalArgumentException if {@code count} is non-positive
     * @see <a href="http://reactivex.io/documentation/operators/window.html">ReactiveX operators documentation: Window</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.COMPUTATION)
    @NonNull
    public final Observable<Observable<T>> window(long timespan, @NonNull TimeUnit unit,
            long count) {
        return window(timespan, unit, Schedulers.computation(), count, false);
    }

    /**
     * Returns an {@code Observable} that emits windows of items it collects from the current {@code Observable}. The resulting
     * {@code Observable} emits connected, non-overlapping windows, each of a fixed duration as specified by the
     * {@code timespan} argument or a maximum size as specified by the {@code count} argument (whichever is
     * reached first). When the current {@code Observable} completes or encounters an error, the resulting {@code Observable}
     * emits the current window and propagates the notification from the current {@code Observable}.
     * <p>
     * <img width="640" height="370" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/window6.v3.png" alt="">
     * <p>
     * Note that ignoring windows or subscribing later (i.e., on another thread) will result in
     * so-called window abandonment where a window may not contain any elements. In this case, subsequent
     * elements will be dropped until the condition for the next window boundary is satisfied. The behavior is
     * a trade-off for ensuring upstream cancellation can happen under some race conditions.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This version of {@code window} operates by default on the {@code computation} {@link Scheduler}.</dd>
     * </dl>
     *
     * @param timespan
     *            the period of time each window collects items before it should be emitted and replaced with a
     *            new window
     * @param unit
     *            the unit of time that applies to the {@code timespan} argument
     * @param count
     *            the maximum size of each window before it should be emitted
     * @param restart
     *            if {@code true}, when a window reaches the capacity limit, the timer is restarted as well
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code unit} is {@code null}
     * @throws IllegalArgumentException if {@code count} is non-positive
     * @see <a href="http://reactivex.io/documentation/operators/window.html">ReactiveX operators documentation: Window</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.COMPUTATION)
    @NonNull
    public final Observable<Observable<T>> window(long timespan, @NonNull TimeUnit unit,
            long count, boolean restart) {
        return window(timespan, unit, Schedulers.computation(), count, restart);
    }

    /**
     * Returns an {@code Observable} that emits windows of items it collects from the current {@code Observable}. The resulting
     * {@code Observable} emits connected, non-overlapping windows, each of a fixed duration as specified by the
     * {@code timespan} argument. When the current {@code Observable} completes or encounters an error, the resulting
     * {@code Observable} emits the current window and propagates the notification from the current {@code Observable}.
     * <p>
     * <img width="640" height="375" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/window5.s.v3.png" alt="">
     * <p>
     * Note that ignoring windows or subscribing later (i.e., on another thread) will result in
     * so-called window abandonment where a window may not contain any elements. In this case, subsequent
     * elements will be dropped until the condition for the next window boundary is satisfied. The behavior is
     * a trade-off for ensuring upstream cancellation can happen under some race conditions.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>You specify which {@link Scheduler} this operator will use.</dd>
     * </dl>
     *
     * @param timespan
     *            the period of time each window collects items before it should be emitted and replaced with a
     *            new window
     * @param unit
     *            the unit of time which applies to the {@code timespan} argument
     * @param scheduler
     *            the {@code Scheduler} to use when determining the end and start of a window
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code unit} or {@code scheduler} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/window.html">ReactiveX operators documentation: Window</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    @NonNull
    public final Observable<Observable<T>> window(long timespan, @NonNull TimeUnit unit,
            @NonNull Scheduler scheduler) {
        return window(timespan, unit, scheduler, Long.MAX_VALUE, false);
    }

    /**
     * Returns an {@code Observable} that emits windows of items it collects from the current {@code Observable}. The resulting
     * {@code Observable} emits connected, non-overlapping windows, each of a fixed duration specified by the
     * {@code timespan} argument or a maximum size specified by the {@code count} argument (whichever is reached
     * first). When the current {@code Observable} completes or encounters an error, the resulting {@code Observable} emits the
     * current window and propagates the notification from the current {@code Observable}.
     * <p>
     * <img width="640" height="370" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/window6.s.v3.png" alt="">
     * <p>
     * Note that ignoring windows or subscribing later (i.e., on another thread) will result in
     * so-called window abandonment where a window may not contain any elements. In this case, subsequent
     * elements will be dropped until the condition for the next window boundary is satisfied. The behavior is
     * a trade-off for ensuring upstream cancellation can happen under some race conditions.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>You specify which {@link Scheduler} this operator will use.</dd>
     * </dl>
     *
     * @param timespan
     *            the period of time each window collects items before it should be emitted and replaced with a
     *            new window
     * @param unit
     *            the unit of time which applies to the {@code timespan} argument
     * @param count
     *            the maximum size of each window before it should be emitted
     * @param scheduler
     *            the {@code Scheduler} to use when determining the end and start of a window
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code unit} or {@code scheduler} is {@code null}
     * @throws IllegalArgumentException if {@code count} is non-positive
     * @see <a href="http://reactivex.io/documentation/operators/window.html">ReactiveX operators documentation: Window</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    @NonNull
    public final Observable<Observable<T>> window(long timespan, @NonNull TimeUnit unit,
            @NonNull Scheduler scheduler, long count) {
        return window(timespan, unit, scheduler, count, false);
    }

    /**
     * Returns an {@code Observable} that emits windows of items it collects from the current {@code Observable}. The resulting
     * {@code Observable} emits connected, non-overlapping windows, each of a fixed duration specified by the
     * {@code timespan} argument or a maximum size specified by the {@code count} argument (whichever is reached
     * first). When the current {@code Observable} completes or encounters an error, the resulting {@code Observable} emits the
     * current window and propagates the notification from the current {@code Observable}.
     * <p>
     * <img width="640" height="370" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/window6.s.v3.png" alt="">
     * <p>
     * Note that ignoring windows or subscribing later (i.e., on another thread) will result in
     * so-called window abandonment where a window may not contain any elements. In this case, subsequent
     * elements will be dropped until the condition for the next window boundary is satisfied. The behavior is
     * a trade-off for ensuring upstream cancellation can happen under some race conditions.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>You specify which {@link Scheduler} this operator will use.</dd>
     * </dl>
     *
     * @param timespan
     *            the period of time each window collects items before it should be emitted and replaced with a
     *            new window
     * @param unit
     *            the unit of time which applies to the {@code timespan} argument
     * @param count
     *            the maximum size of each window before it should be emitted
     * @param scheduler
     *            the {@code Scheduler} to use when determining the end and start of a window
     * @param restart
     *            if {@code true}, when a window reaches the capacity limit, the timer is restarted as well
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code unit} or {@code scheduler} is {@code null}
     * @throws IllegalArgumentException if {@code count} is non-positive
     * @see <a href="http://reactivex.io/documentation/operators/window.html">ReactiveX operators documentation: Window</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    @NonNull
    public final Observable<Observable<T>> window(long timespan, @NonNull TimeUnit unit,
            @NonNull Scheduler scheduler, long count, boolean restart) {
        return window(timespan, unit, scheduler, count, restart, bufferSize());
    }

    /**
     * Returns an {@code Observable} that emits windows of items it collects from the current {@code Observable}. The resulting
     * {@code Observable} emits connected, non-overlapping windows, each of a fixed duration specified by the
     * {@code timespan} argument or a maximum size specified by the {@code count} argument (whichever is reached
     * first). When the current {@code Observable} completes or encounters an error, the resulting {@code Observable} emits the
     * current window and propagates the notification from the current {@code Observable}.
     * <p>
     * <img width="640" height="370" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/window6.s.v3.png" alt="">
     * <p>
     * Note that ignoring windows or subscribing later (i.e., on another thread) will result in
     * so-called window abandonment where a window may not contain any elements. In this case, subsequent
     * elements will be dropped until the condition for the next window boundary is satisfied. The behavior is
     * a trade-off for ensuring upstream cancellation can happen under some race conditions.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>You specify which {@link Scheduler} this operator will use.</dd>
     * </dl>
     *
     * @param timespan
     *            the period of time each window collects items before it should be emitted and replaced with a
     *            new window
     * @param unit
     *            the unit of time which applies to the {@code timespan} argument
     * @param count
     *            the maximum size of each window before it should be emitted
     * @param scheduler
     *            the {@code Scheduler} to use when determining the end and start of a window
     * @param restart
     *            if {@code true}, when a window reaches the capacity limit, the timer is restarted as well
     * @param bufferSize
     *            the capacity hint for the buffer in the inner windows
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code unit} or {@code scheduler} is {@code null}
     * @throws IllegalArgumentException if {@code count} or {@code bufferSize} is non-positive
     * @see <a href="http://reactivex.io/documentation/operators/window.html">ReactiveX operators documentation: Window</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    @NonNull
    public final Observable<Observable<T>> window(
            long timespan, @NonNull TimeUnit unit, @NonNull Scheduler scheduler,
            long count, boolean restart, int bufferSize) {
        ObjectHelper.verifyPositive(bufferSize, "bufferSize");
        Objects.requireNonNull(scheduler, "scheduler is null");
        Objects.requireNonNull(unit, "unit is null");
        ObjectHelper.verifyPositive(count, "count");
        return RxJavaPlugins.onAssembly(new ObservableWindowTimed<>(this, timespan, timespan, unit, scheduler, count, bufferSize, restart));
    }

    /**
     * Returns an {@code Observable} that emits non-overlapping windows of items it collects from the current {@code Observable}
     * where the boundary of each window is determined by the items emitted from a specified boundary-governing
     * {@link ObservableSource}.
     * <p>
     * <img width="640" height="475" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/window8.v3.png" alt="">
     * <p>
     * Note that ignoring windows or subscribing later (i.e., on another thread) will result in
     * so-called window abandonment where a window may not contain any elements. In this case, subsequent
     * elements will be dropped until the condition for the next window boundary is satisfied. The behavior is
     * a trade-off for ensuring upstream cancellation can happen under some race conditions.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This version of {@code window} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <B>
     *            the window element type (ignored)
     * @param boundaryIndicator
     *            an {@code ObservableSource} whose emitted items close and open windows
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code boundaryIndicator} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/window.html">ReactiveX operators documentation: Window</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <@NonNull B> Observable<Observable<T>> window(@NonNull ObservableSource<B> boundaryIndicator) {
        return window(boundaryIndicator, bufferSize());
    }

    /**
     * Returns an {@code Observable} that emits non-overlapping windows of items it collects from the current {@code Observable}
     * where the boundary of each window is determined by the items emitted from a specified boundary-governing
     * {@link ObservableSource}.
     * <p>
     * <img width="640" height="475" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/window8.v3.png" alt="">
     * <p>
     * Note that ignoring windows or subscribing later (i.e., on another thread) will result in
     * so-called window abandonment where a window may not contain any elements. In this case, subsequent
     * elements will be dropped until the condition for the next window boundary is satisfied. The behavior is
     * a trade-off for ensuring upstream cancellation can happen under some race conditions.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This version of {@code window} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <B>
     *            the window element type (ignored)
     * @param boundaryIndicator
     *            an {@code ObservableSource} whose emitted items close and open windows
     * @param bufferSize
     *            the capacity hint for the buffer in the inner windows
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code boundaryIndicator} is {@code null}
     * @throws IllegalArgumentException if {@code bufferSize} is non-positive
     * @see <a href="http://reactivex.io/documentation/operators/window.html">ReactiveX operators documentation: Window</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <@NonNull B> Observable<Observable<T>> window(@NonNull ObservableSource<B> boundaryIndicator, int bufferSize) {
        Objects.requireNonNull(boundaryIndicator, "boundaryIndicator is null");
        ObjectHelper.verifyPositive(bufferSize, "bufferSize");
        return RxJavaPlugins.onAssembly(new ObservableWindowBoundary<>(this, boundaryIndicator, bufferSize));
    }

    /**
     * Returns an {@code Observable} that emits windows of items it collects from the current {@code Observable}. The resulting
     * {@code Observable} emits windows that contain those items emitted by the current {@code Observable} between the time when
     * the {@code openingIndicator} {@link ObservableSource} emits an item and when the {@code ObservableSource} returned by
     * {@code closingIndicator} emits an item.
     * <p>
     * <img width="640" height="550" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/window2.v3.png" alt="">
     * <p>
     * Note that ignoring windows or subscribing later (i.e., on another thread) will result in
     * so-called window abandonment where a window may not contain any elements. In this case, subsequent
     * elements will be dropped until the condition for the next window boundary is satisfied. The behavior is
     * a trade-off for ensuring upstream cancellation can happen under some race conditions.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This version of {@code window} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <U> the element type of the window-opening {@code ObservableSource}
     * @param <V> the element type of the window-closing {@code ObservableSource}s
     * @param openingIndicator
     *            an {@code ObservableSource} that, when it emits an item, causes another window to be created
     * @param closingIndicator
     *            a {@link Function} that produces an {@code ObservableSource} for every window created. When this indicator {@code ObservableSource}
     *            emits an item, the associated window is completed
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code openingIndicator} or {@code closingIndicator} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/window.html">ReactiveX operators documentation: Window</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <@NonNull U, @NonNull V> Observable<Observable<T>> window(
            @NonNull ObservableSource<U> openingIndicator,
            @NonNull Function<? super U, ? extends ObservableSource<V>> closingIndicator) {
        return window(openingIndicator, closingIndicator, bufferSize());
    }

    /**
     * Returns an {@code Observable} that emits windows of items it collects from the current {@code Observable}. The resulting
     * {@code Observable} emits windows that contain those items emitted by the current {@code Observable} between the time when
     * the {@code openingIndicator} {@link ObservableSource} emits an item and when the {@code ObservableSource} returned by
     * {@code closingIndicator} emits an item.
     * <p>
     * <img width="640" height="550" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/window2.v3.png" alt="">
     * <p>
     * Note that ignoring windows or subscribing later (i.e., on another thread) will result in
     * so-called window abandonment where a window may not contain any elements. In this case, subsequent
     * elements will be dropped until the condition for the next window boundary is satisfied. The behavior is
     * a trade-off for ensuring upstream cancellation can happen under some race conditions.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This version of {@code window} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <U> the element type of the window-opening {@code ObservableSource}
     * @param <V> the element type of the window-closing {@code ObservableSource}s
     * @param openingIndicator
     *            an {@code ObservableSource} that, when it emits an item, causes another window to be created
     * @param closingIndicator
     *            a {@link Function} that produces an {@code ObservableSource} for every window created. When this indicator {@code ObservableSource}
     *            emits an item, the associated window is completed
     * @param bufferSize
     *            the capacity hint for the buffer in the inner windows
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code openingIndicator} or {@code closingIndicator} is {@code null}
     * @throws IllegalArgumentException if {@code bufferSize} is non-positive
     * @see <a href="http://reactivex.io/documentation/operators/window.html">ReactiveX operators documentation: Window</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <@NonNull U, @NonNull V> Observable<Observable<T>> window(
            @NonNull ObservableSource<U> openingIndicator,
            @NonNull Function<? super U, ? extends ObservableSource<V>> closingIndicator, int bufferSize) {
        Objects.requireNonNull(openingIndicator, "openingIndicator is null");
        Objects.requireNonNull(closingIndicator, "closingIndicator is null");
        ObjectHelper.verifyPositive(bufferSize, "bufferSize");
        return RxJavaPlugins.onAssembly(new ObservableWindowBoundarySelector<>(this, openingIndicator, closingIndicator, bufferSize));
    }

    /**
     * Merges the specified {@link ObservableSource} into the current {@code Observable} sequence by using the {@code resultSelector}
     * function only when the current {@code Observable} emits an item.
     *
     * <p>Note that this operator doesn't emit anything until the other source has produced at
     * least one value. The resulting emission only happens when the current {@code Observable} emits (and
     * not when the other source emits, unlike combineLatest).
     * If the other source doesn't produce any value and just completes, the sequence is completed immediately.
     * If the upstream completes before the other source has produced at least one value, the sequence completes
     * without emission.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/withLatestFrom.v3.png" alt="">
     *
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This operator, by default, doesn't run any particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <U> the element type of the other {@code ObservableSource}
     * @param <R> the result type of the combination
     * @param other
     *            the other {@code ObservableSource}
     * @param combiner
     *            the function to call when the current {@code Observable} emits an item and the other {@code ObservableSource} has already
     *            emitted an item, to generate the item to be emitted by the resulting {@code Observable}
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code other} or {@code combiner} is {@code null}
     * @since 2.0
     * @see <a href="http://reactivex.io/documentation/operators/combinelatest.html">ReactiveX operators documentation: CombineLatest</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <@NonNull U, @NonNull R> Observable<R> withLatestFrom(@NonNull ObservableSource<? extends U> other, @NonNull BiFunction<? super T, ? super U, ? extends R> combiner) {
        Objects.requireNonNull(other, "other is null");
        Objects.requireNonNull(combiner, "combiner is null");

        return RxJavaPlugins.onAssembly(new ObservableWithLatestFrom<T, U, R>(this, combiner, other));
    }

    /**
     * Combines the value emission from the current {@code Observable} with the latest emissions from the
     * other {@link ObservableSource}s via a function to produce the output item.
     *
     * <p>Note that this operator doesn't emit anything until all other sources have produced at
     * least one value. The resulting emission only happens when the current {@code Observable} emits (and
     * not when any of the other sources emit, unlike {@code combineLatest}).
     * If a source doesn't produce any value and just completes, the sequence is completed immediately.
     * If the upstream completes before all other sources have produced at least one value, the sequence completes
     * without emission.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/withLatestFrom.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This operator does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T1> the first other source's value type
     * @param <T2> the second other source's value type
     * @param <R> the result value type
     * @param source1 the first other {@code ObservableSource}
     * @param source2 the second other {@code ObservableSource}
     * @param combiner the function called with an array of values from each participating {@code ObservableSource}
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code source1}, {@code source2} or {@code combiner} is {@code null}
     * @since 2.0
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <@NonNull T1, @NonNull T2, @NonNull R> Observable<R> withLatestFrom(
            @NonNull ObservableSource<T1> source1, @NonNull ObservableSource<T2> source2,
            @NonNull Function3<? super T, ? super T1, ? super T2, R> combiner) {
        Objects.requireNonNull(source1, "source1 is null");
        Objects.requireNonNull(source2, "source2 is null");
        Objects.requireNonNull(combiner, "combiner is null");
        Function<Object[], R> f = Functions.toFunction(combiner);
        return withLatestFrom(new ObservableSource[] { source1, source2 }, f);
    }

    /**
     * Combines the value emission from the current {@code Observable} with the latest emissions from the
     * other {@link ObservableSource}s via a function to produce the output item.
     *
     * <p>Note that this operator doesn't emit anything until all other sources have produced at
     * least one value. The resulting emission only happens when the current {@code Observable} emits (and
     * not when any of the other sources emit, unlike combineLatest).
     * If a source doesn't produce any value and just completes, the sequence is completed immediately.
     * If the upstream completes before all other sources have produced at least one value, the sequence completes
     * without emission.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/withLatestFrom.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This operator does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T1> the first other source's value type
     * @param <T2> the second other source's value type
     * @param <T3> the third other source's value type
     * @param <R> the result value type
     * @param source1 the first other {@code ObservableSource}
     * @param source2 the second other {@code ObservableSource}
     * @param source3 the third other {@code ObservableSource}
     * @param combiner the function called with an array of values from each participating {@code ObservableSource}
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code source1}, {@code source2}, {@code source3} or {@code combiner} is {@code null}
     * @since 2.0
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <@NonNull T1, @NonNull T2, @NonNull T3, @NonNull R> Observable<R> withLatestFrom(
            @NonNull ObservableSource<T1> source1, @NonNull ObservableSource<T2> source2,
            @NonNull ObservableSource<T3> source3,
            @NonNull Function4<? super T, ? super T1, ? super T2, ? super T3, R> combiner) {
        Objects.requireNonNull(source1, "source1 is null");
        Objects.requireNonNull(source2, "source2 is null");
        Objects.requireNonNull(source3, "source3 is null");
        Objects.requireNonNull(combiner, "combiner is null");
        Function<Object[], R> f = Functions.toFunction(combiner);
        return withLatestFrom(new ObservableSource[] { source1, source2, source3 }, f);
    }

    /**
     * Combines the value emission from the current {@code Observable} with the latest emissions from the
     * other {@link ObservableSource}s via a function to produce the output item.
     *
     * <p>Note that this operator doesn't emit anything until all other sources have produced at
     * least one value. The resulting emission only happens when the current {@code Observable} emits (and
     * not when any of the other sources emit, unlike combineLatest).
     * If a source doesn't produce any value and just completes, the sequence is completed immediately.
     * If the upstream completes before all other sources have produced at least one value, the sequence completes
     * without emission.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/withLatestFrom.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This operator does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T1> the first other source's value type
     * @param <T2> the second other source's value type
     * @param <T3> the third other source's value type
     * @param <T4> the fourth other source's value type
     * @param <R> the result value type
     * @param source1 the first other {@code ObservableSource}
     * @param source2 the second other {@code ObservableSource}
     * @param source3 the third other {@code ObservableSource}
     * @param source4 the fourth other {@code ObservableSource}
     * @param combiner the function called with an array of values from each participating {@code ObservableSource}
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code source1}, {@code source2}, {@code source3},
     *                              {@code source4} or {@code combiner} is {@code null}
     * @since 2.0
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <@NonNull T1, @NonNull T2, @NonNull T3, @NonNull T4, @NonNull R> Observable<R> withLatestFrom(
            @NonNull ObservableSource<T1> source1, @NonNull ObservableSource<T2> source2,
            @NonNull ObservableSource<T3> source3, @NonNull ObservableSource<T4> source4,
            @NonNull Function5<? super T, ? super T1, ? super T2, ? super T3, ? super T4, R> combiner) {
        Objects.requireNonNull(source1, "source1 is null");
        Objects.requireNonNull(source2, "source2 is null");
        Objects.requireNonNull(source3, "source3 is null");
        Objects.requireNonNull(source4, "source4 is null");
        Objects.requireNonNull(combiner, "combiner is null");
        Function<Object[], R> f = Functions.toFunction(combiner);
        return withLatestFrom(new ObservableSource[] { source1, source2, source3, source4 }, f);
    }

    /**
     * Combines the value emission from the current {@code Observable} with the latest emissions from the
     * other {@link ObservableSource}s via a function to produce the output item.
     *
     * <p>Note that this operator doesn't emit anything until all other sources have produced at
     * least one value. The resulting emission only happens when the current {@code Observable} emits (and
     * not when any of the other sources emit, unlike combineLatest).
     * If a source doesn't produce any value and just completes, the sequence is completed immediately.
     * If the upstream completes before all other sources have produced at least one value, the sequence completes
     * without emission.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/withLatestFrom.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This operator does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <R> the result value type
     * @param others the array of other sources
     * @param combiner the function called with an array of values from each participating {@code ObservableSource}
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code others} or {@code combiner} is {@code null}
     * @since 2.0
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <@NonNull R> Observable<R> withLatestFrom(@NonNull ObservableSource<?>[] others, @NonNull Function<? super Object[], R> combiner) {
        Objects.requireNonNull(others, "others is null");
        Objects.requireNonNull(combiner, "combiner is null");
        return RxJavaPlugins.onAssembly(new ObservableWithLatestFromMany<>(this, others, combiner));
    }

    /**
     * Combines the value emission from the current {@code Observable} with the latest emissions from the
     * other {@link ObservableSource}s via a function to produce the output item.
     *
     * <p>Note that this operator doesn't emit anything until all other sources have produced at
     * least one value. The resulting emission only happens when the current {@code Observable} emits (and
     * not when any of the other sources emit, unlike {@code combineLatest}).
     * If a source doesn't produce any value and just completes, the sequence is completed immediately.
     * If the upstream completes before all other sources have produced at least one value, the sequence completes
     * without emission.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/withLatestFrom.v3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This operator does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <R> the result value type
     * @param others the iterable of other sources
     * @param combiner the function called with an array of values from each participating {@code ObservableSource}
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code others} or {@code combiner} is {@code null}
     * @since 2.0
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <@NonNull R> Observable<R> withLatestFrom(@NonNull Iterable<@NonNull ? extends ObservableSource<?>> others, @NonNull Function<? super Object[], R> combiner) {
        Objects.requireNonNull(others, "others is null");
        Objects.requireNonNull(combiner, "combiner is null");
        return RxJavaPlugins.onAssembly(new ObservableWithLatestFromMany<>(this, others, combiner));
    }

    /**
     * Returns an {@code Observable} that emits items that are the result of applying a specified function to pairs of
     * values, one each from the current {@code Observable} and a specified {@link Iterable} sequence.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/zip.i.v3.png" alt="">
     * <p>
     * Note that the {@code other} {@code Iterable} is evaluated as items are observed from the current {@code Observable}; it is
     * not pre-consumed. This allows you to zip infinite streams on either side.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code zipWith} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <U>
     *            the type of items in the {@code other} {@code Iterable}
     * @param <R>
     *            the type of items emitted by the resulting {@code Observable}
     * @param other
     *            the {@code Iterable} sequence
     * @param zipper
     *            a function that combines the pairs of items from the current {@code Observable} and the {@code Iterable} to generate
     *            the items to be emitted by the resulting {@code Observable}
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code other} or {@code zipper} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/zip.html">ReactiveX operators documentation: Zip</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <@NonNull U, @NonNull R> Observable<R> zipWith(@NonNull Iterable<U> other, @NonNull BiFunction<? super T, ? super U, ? extends R> zipper) {
        Objects.requireNonNull(other, "other is null");
        Objects.requireNonNull(zipper, "zipper is null");
        return RxJavaPlugins.onAssembly(new ObservableZipIterable<>(this, other, zipper));
    }

    /**
     * Returns an {@code Observable} that emits items that are the result of applying a specified function to pairs of
     * values, one each from the current {@code Observable} and another specified {@link ObservableSource}.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/zip.v3.png" alt="">
     * <p>
     * The operator subscribes to its sources in order they are specified and completes eagerly if
     * one of the sources is shorter than the rest while disposing the other sources. Therefore, it
     * is possible those other sources will never be able to run to completion (and thus not calling
     * {@code doOnComplete()}). This can also happen if the sources are exactly the same length; if
     * source A completes and B has been consumed and is about to complete, the operator detects A won't
     * be sending further values and it will dispose B immediately. For example:
     * <pre><code>range(1, 5).doOnComplete(action1).zipWith(range(6, 5).doOnComplete(action2), (a, b) -&gt; a + b)</code></pre>
     * {@code action1} will be called but {@code action2} won't.
     * <br>To work around this termination property,
     * use {@link #doOnDispose(Action)} as well or use {@code using()} to do cleanup in case of completion
     * or a dispose() call.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code zipWith} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <U>
     *            the type of items emitted by the {@code other} {@code ObservableSource}
     * @param <R>
     *            the type of items emitted by the resulting {@code Observable}
     * @param other
     *            the other {@code ObservableSource}
     * @param zipper
     *            a function that combines the pairs of items from the current {@code Observable} and the other {@code ObservableSource} to generate the items to
     *            be emitted by the resulting {@code Observable}
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code other} or {@code zipper} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/zip.html">ReactiveX operators documentation: Zip</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <@NonNull U, @NonNull R> Observable<R> zipWith(@NonNull ObservableSource<? extends U> other,
            @NonNull BiFunction<? super T, ? super U, ? extends R> zipper) {
        Objects.requireNonNull(other, "other is null");
        return zip(this, other, zipper);
    }

    /**
     * Returns an {@code Observable} that emits items that are the result of applying a specified function to pairs of
     * values, one each from the current {@code Observable} and another specified {@link ObservableSource}.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/zip.v3.png" alt="">
     * <p>
     * The operator subscribes to its sources in order they are specified and completes eagerly if
     * one of the sources is shorter than the rest while disposing the other sources. Therefore, it
     * is possible those other sources will never be able to run to completion (and thus not calling
     * {@code doOnComplete()}). This can also happen if the sources are exactly the same length; if
     * source A completes and B has been consumed and is about to complete, the operator detects A won't
     * be sending further values and it will dispose B immediately. For example:
     * <pre><code>range(1, 5).doOnComplete(action1).zipWith(range(6, 5).doOnComplete(action2), (a, b) -&gt; a + b)</code></pre>
     * {@code action1} will be called but {@code action2} won't.
     * <br>To work around this termination property,
     * use {@link #doOnDispose(Action)} as well or use {@code using()} to do cleanup in case of completion
     * or a dispose() call.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code zipWith} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <U>
     *            the type of items emitted by the {@code other} {@code ObservableSource}
     * @param <R>
     *            the type of items emitted by the resulting {@code Observable}
     * @param other
     *            the other {@code ObservableSource}
     * @param zipper
     *            a function that combines the pairs of items from the current {@code Observable} and the other {@code ObservableSource} to generate the items to
     *            be emitted by the resulting {@code Observable}
     * @param delayError
     *            if {@code true}, errors from the current {@code Observable} or the other {@code ObservableSource} is delayed until both terminate
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code other} or {@code zipper} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/zip.html">ReactiveX operators documentation: Zip</a>
     * @since 2.0
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <@NonNull U, @NonNull R> Observable<R> zipWith(@NonNull ObservableSource<? extends U> other,
            @NonNull BiFunction<? super T, ? super U, ? extends R> zipper, boolean delayError) {
        return zip(this, other, zipper, delayError);
    }

    /**
     * Returns an {@code Observable} that emits items that are the result of applying a specified function to pairs of
     * values, one each from the current {@code Observable} and another specified {@link ObservableSource}.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/zip.v3.png" alt="">
     * <p>
     * The operator subscribes to its sources in order they are specified and completes eagerly if
     * one of the sources is shorter than the rest while disposing the other sources. Therefore, it
     * is possible those other sources will never be able to run to completion (and thus not calling
     * {@code doOnComplete()}). This can also happen if the sources are exactly the same length; if
     * source A completes and B has been consumed and is about to complete, the operator detects A won't
     * be sending further values and it will dispose B immediately. For example:
     * <pre><code>range(1, 5).doOnComplete(action1).zipWith(range(6, 5).doOnComplete(action2), (a, b) -&gt; a + b)</code></pre>
     * {@code action1} will be called but {@code action2} won't.
     * <br>To work around this termination property,
     * use {@link #doOnDispose(Action)} as well or use {@code using()} to do cleanup in case of completion
     * or a dispose() call.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code zipWith} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <U>
     *            the type of items emitted by the {@code other} {@code ObservableSource}
     * @param <R>
     *            the type of items emitted by the resulting {@code Observable}
     * @param other
     *            the other {@code ObservableSource}
     * @param zipper
     *            a function that combines the pairs of items from the current {@code Observable} and the other {@code ObservableSource} to generate the items to
     *            be emitted by the resulting {@code Observable}
     * @param bufferSize
     *            the capacity hint for the buffer in the inner windows
     * @param delayError
     *            if {@code true}, errors from the current {@code Observable} or the other {@code ObservableSource} is delayed until both terminate
     * @return the new {@code Observable} instance
     * @see <a href="http://reactivex.io/documentation/operators/zip.html">ReactiveX operators documentation: Zip</a>
     * @throws NullPointerException if {@code other} or {@code zipper} is {@code null}
     * @throws IllegalArgumentException if {@code bufferSize} is non-positive
     * @since 2.0
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <@NonNull U, @NonNull R> Observable<R> zipWith(@NonNull ObservableSource<? extends U> other,
            @NonNull BiFunction<? super T, ? super U, ? extends R> zipper, boolean delayError, int bufferSize) {
        return zip(this, other, zipper, delayError, bufferSize);
    }

    // -------------------------------------------------------------------------
    // Fluent test support, super handy and reduces test preparation boilerplate
    // -------------------------------------------------------------------------
    /**
     * Creates a {@link TestObserver} and subscribes it to the current {@code Observable}.
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
    public final TestObserver<T> test() { // NoPMD
        TestObserver<T> to = new TestObserver<>();
        subscribe(to);
        return to;
    }

    /**
     * Creates a {@link TestObserver}, optionally disposes it and then subscribes
     * it to the current {@code Observable}.
     *
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code test} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param dispose indicates if the {@code TestObserver} should be disposed before
     *                it is subscribed to the current {@code Observable}
     * @return the new {@code TestObserver} instance
     * @since 2.0
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final TestObserver<T> test(boolean dispose) { // NoPMD
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
     * or an empty optional into an {@link #empty()} {@code Observable} instance.
     * <p>
     * <img width="640" height="335" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/fromOptional.o.png" alt="">
     * <p>
     * Note that the operator takes an already instantiated optional reference and does not
     * by any means create this original optional. If the optional is to be created per
     * consumer upon subscription, use {@link #defer(Supplier)} around {@code fromOptional}:
     * <pre><code>
     * Observable.defer(() -&gt; Observable.fromOptional(createOptional()));
     * </code></pre>
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code fromOptional} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the element type of the optional value
     * @param optional the optional value to convert into an {@code Observable}
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code optional} is {@code null}
     * @since 3.0.0
     * @see #just(Object)
     * @see #empty()
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public static <@NonNull T> Observable<@NonNull T> fromOptional(@NonNull Optional<T> optional) {
        Objects.requireNonNull(optional, "optional is null");
        return optional.map(Observable::just).orElseGet(Observable::empty);
    }

    /**
     * Signals the completion value or error of the given (hot) {@link CompletionStage}-based asynchronous calculation.
     * <p>
     * <img width="640" height="262" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/fromCompletionStage.o.png" alt="">
     * <p>
     * Note that the operator takes an already instantiated, running or terminated {@code CompletionStage}.
     * If the optional is to be created per consumer upon subscription, use {@link #defer(Supplier)}
     * around {@code fromCompletionStage}:
     * <pre><code>
     * Observable.defer(() -&gt; Observable.fromCompletionStage(createCompletionStage()));
     * </code></pre>
     * <p>
     * If the {@code CompletionStage} completes with {@code null}, a {@link NullPointerException} is signaled.
     * <p>
     * Canceling the flow can't cancel the execution of the {@code CompletionStage} because {@code CompletionStage}
     * itself doesn't support cancellation. Instead, the operator detaches from the {@code CompletionStage}.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code fromCompletionStage} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the element type of the {@code CompletionStage}
     * @param stage the {@code CompletionStage} to convert to {@code Observable} and signal its terminal value or error
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code stage} is {@code null}
     * @since 3.0.0
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public static <@NonNull T> Observable<@NonNull T> fromCompletionStage(@NonNull CompletionStage<T> stage) {
        Objects.requireNonNull(stage, "stage is null");
        return RxJavaPlugins.onAssembly(new ObservableFromCompletionStage<>(stage));
    }

    /**
     * Converts a {@link Stream} into a finite {@code Observable} and emits its items in the sequence.
     * <p>
     * <img width="640" height="407" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/fromStream.o.png" alt="">
     * <p>
     * The operator closes the {@code Stream} upon cancellation and when it terminates. The exceptions raised when
     * closing a {@code Stream} are routed to the global error handler ({@link RxJavaPlugins#onError(Throwable)}.
     * If a {@code Stream} should not be closed, turn it into an {@link Iterable} and use {@link #fromIterable(Iterable)}:
     * <pre><code>
     * Stream&lt;T&gt; stream = ...
     * Observable.fromIterable(stream::iterator);
     * </code></pre>
     * <p>
     * Note that {@code Stream}s can be consumed only once; any subsequent attempt to consume a {@code Stream}
     * will result in an {@link IllegalStateException}.
     * <p>
     * Primitive streams are not supported and items have to be boxed manually (e.g., via {@link IntStream#boxed()}):
     * <pre><code>
     * IntStream intStream = IntStream.rangeClosed(1, 10);
     * Observable.fromStream(intStream.boxed());
     * </code></pre>
     * <p>
     * {@code Stream} does not support concurrent usage so creating and/or consuming the same instance multiple times
     * from multiple threads can lead to undefined behavior.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code fromStream} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the element type of the source {@code Stream}
     * @param stream the {@code Stream} of values to emit
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code stream} is {@code null}
     * @since 3.0.0
     * @see #fromIterable(Iterable)
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public static <@NonNull T> Observable<@NonNull T> fromStream(@NonNull Stream<T> stream) {
        Objects.requireNonNull(stream, "stream is null");
        return RxJavaPlugins.onAssembly(new ObservableFromStream<>(stream));
    }

    /**
     * Maps each upstream value into an {@link Optional} and emits the contained item if not empty.
     * <p>
     * <img width="640" height="306" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/mapOptional.o.png" alt="">
     *
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code mapOptional} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <R> the non-{@code null} output type
     * @param mapper the function that receives the upstream item and should return a <em>non-empty</em> {@code Optional}
     * to emit as the output or an <em>empty</em> {@code Optional} to skip to the next upstream value
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code mapper} is {@code null}
     * @since 3.0.0
     * @see #map(Function)
     * @see #filter(Predicate)
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <@NonNull R> Observable<R> mapOptional(@NonNull Function<? super T, @NonNull Optional<? extends R>> mapper) {
        Objects.requireNonNull(mapper, "mapper is null");
        return RxJavaPlugins.onAssembly(new ObservableMapOptional<>(this, mapper));
    }

    /**
     * Collects the finite upstream's values into a container via a {@link Stream} {@link Collector} callback set and emits
     * it as the success result as a {@link Single}.
     * <p>
     * <img width="640" height="358" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/collector.o.png" alt="">
     *
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code collect} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <R> the non-{@code null} result type
     * @param <A> the intermediate container type used for the accumulation
     * @param collector the interface defining the container supplier, accumulator and finisher functions;
     * see {@link Collectors} for some standard implementations
     * @return the new {@code Single} instance
     * @throws NullPointerException if {@code collector} is {@code null}
     * @since 3.0.0
     * @see Collectors
     * @see #collect(Supplier, BiConsumer)
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <@NonNull R, @NonNull A> Single<R> collect(@NonNull Collector<? super T, A, R> collector) {
        Objects.requireNonNull(collector, "collector is null");
        return RxJavaPlugins.onAssembly(new ObservableCollectWithCollectorSingle<>(this, collector));
    }

    /**
     * Signals the first upstream item (or the default item if the upstream is empty) via
     * a {@link CompletionStage}.
     * <p>
     * <img width="640" height="313" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/firstStage.o.png" alt="">
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
     * CompletionStage&lt;Optional&lt;T&gt;&gt; stage = source.map(Optional::of).firstStage(Optional.empty());
     * </code></pre>
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code firstStage} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param defaultItem the item to signal if the upstream is empty
     * @return the new {@code CompletionStage} instance
     * @throws NullPointerException if {@code defaultItem} is {@code null}
     * @since 3.0.0
     * @see #firstOrErrorStage()
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final CompletionStage<T> firstStage(@Nullable T defaultItem) {
        return subscribeWith(new ObservableFirstStageObserver<>(true, defaultItem));
    }

    /**
     * Signals the only expected upstream item (or the default item if the upstream is empty)
     * or signals {@link IllegalArgumentException} if the upstream has more than one item
     * via a {@link CompletionStage}.
     * <p>
     * <img width="640" height="227" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/singleStage.o.png" alt="">
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
     * CompletionStage&lt;Optional&lt;T&gt;&gt; stage = source.map(Optional::of).singleStage(Optional.empty());
     * </code></pre>
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code singleStage} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param defaultItem the item to signal if the upstream is empty
     * @return the new {@code CompletionStage} instance
     * @throws NullPointerException if {@code defaultItem} is {@code null}
     * @since 3.0.0
     * @see #singleOrErrorStage()
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final CompletionStage<T> singleStage(@Nullable T defaultItem) {
        return subscribeWith(new ObservableSingleStageObserver<>(true, defaultItem));
    }

    /**
     * Signals the last upstream item (or the default item if the upstream is empty) via
     * a {@link CompletionStage}.
     * <p>
     * <img width="640" height="313" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/lastStage.o.png" alt="">
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
     * CompletionStage&lt;Optional&lt;T&gt;&gt; stage = source.map(Optional::of).lastStage(Optional.empty());
     * </code></pre>
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code lastStage} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param defaultItem the item to signal if the upstream is empty
     * @return the new {@code CompletionStage} instance
     * @throws NullPointerException if {@code defaultItem} is {@code null}
     * @since 3.0.0
     * @see #lastOrErrorStage()
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final CompletionStage<T> lastStage(@Nullable T defaultItem) {
        return subscribeWith(new ObservableLastStageObserver<>(true, defaultItem));
    }

    /**
     * Signals the first upstream item or a {@link NoSuchElementException} if the upstream is empty via
     * a {@link CompletionStage}.
     * <p>
     * <img width="640" height="341" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/firstOrErrorStage.o.png" alt="">
     * <p>
     * The upstream can be canceled by converting the resulting {@code CompletionStage} into
     * {@link CompletableFuture} via {@link CompletionStage#toCompletableFuture()} and
     * calling {@link CompletableFuture#cancel(boolean)} on it.
     * The upstream will be also cancelled if the resulting {@code CompletionStage} is converted to and
     * completed manually by {@link CompletableFuture#complete(Object)} or {@link CompletableFuture#completeExceptionally(Throwable)}.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code firstOrErrorStage} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @return the new {@code CompletionStage} instance
     * @since 3.0.0
     * @see #firstStage(Object)
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final CompletionStage<T> firstOrErrorStage() {
        return subscribeWith(new ObservableFirstStageObserver<>(false, null));
    }

    /**
     * Signals the only expected upstream item, a {@link NoSuchElementException} if the upstream is empty
     * or signals {@link IllegalArgumentException} if the upstream has more than one item
     * via a {@link CompletionStage}.
     * <p>
     * <img width="640" height="227" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/singleOrErrorStage.o.png" alt="">
     * <p>
     * The upstream can be canceled by converting the resulting {@code CompletionStage} into
     * {@link CompletableFuture} via {@link CompletionStage#toCompletableFuture()} and
     * calling {@link CompletableFuture#cancel(boolean)} on it.
     * The upstream will be also cancelled if the resulting {@code CompletionStage} is converted to and
     * completed manually by {@link CompletableFuture#complete(Object)} or {@link CompletableFuture#completeExceptionally(Throwable)}.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code singleOrErrorStage} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @return the new {@code CompletionStage} instance
     * @since 3.0.0
     * @see #singleStage(Object)
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final CompletionStage<T> singleOrErrorStage() {
        return subscribeWith(new ObservableSingleStageObserver<>(false, null));
    }

    /**
     * Signals the last upstream item or a {@link NoSuchElementException} if the upstream is empty via
     * a {@link CompletionStage}.
     * <p>
     * <img width="640" height="343" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/lastOrErrorStage.o.png" alt="">
     * <p>
     * The upstream can be canceled by converting the resulting {@code CompletionStage} into
     * {@link CompletableFuture} via {@link CompletionStage#toCompletableFuture()} and
     * calling {@link CompletableFuture#cancel(boolean)} on it.
     * The upstream will be also cancelled if the resulting {@code CompletionStage} is converted to and
     * completed manually by {@link CompletableFuture#complete(Object)} or {@link CompletableFuture#completeExceptionally(Throwable)}.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code lastOrErrorStage} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @return the new {@code CompletionStage} instance
     * @since 3.0.0
     * @see #lastStage(Object)
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final CompletionStage<T> lastOrErrorStage() {
        return subscribeWith(new ObservableLastStageObserver<>(false, null));
    }

    /**
     * Creates a sequential {@link Stream} to consume or process the current {@code Observable} in a blocking manner via
     * the Java {@code Stream} API.
     * <p>
     * <img width="640" height="399" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/blockingStream.o.png" alt="">
     * <p>
     * Cancellation of the upstream is done via {@link Stream#close()}, therefore, it is strongly recommended the
     * consumption is performed within a try-with-resources construct:
     * <pre><code>
     * Observable&lt;Integer&gt; source = Observable.range(1, 10)
     *        .subscribeOn(Schedulers.computation());
     *
     * try (Stream&lt;Integer&gt; stream = source.blockingStream()) {
     *     stream.limit(3).forEach(System.out::println);
     * }
     * </code></pre>
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code blockingStream} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @return the new {@code Stream} instance
     * @since 3.0.0
     * @see #blockingStream(int)
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Stream<T> blockingStream() {
        return blockingStream(bufferSize());
    }

    /**
     * Creates a sequential {@link Stream} to consume or process the current {@code Observable} in a blocking manner via
     * the Java {@code Stream} API.
     * <p>
     * <img width="640" height="399" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/blockingStream.oi.png" alt="">
     * <p>
     * Cancellation of the upstream is done via {@link Stream#close()}, therefore, it is strongly recommended the
     * consumption is performed within a try-with-resources construct:
     * <pre><code>
     * Observable&lt;Integer&gt; source = Observable.range(1, 10)
     *        .subscribeOn(Schedulers.computation());
     *
     * try (Stream&lt;Integer&gt; stream = source.blockingStream(4)) {
     *     stream.limit(3).forEach(System.out::println);
     * }
     * </code></pre>
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code blockingStream} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param capacityHint the expected number of items to be buffered
     * @return the new {@code Stream} instance
     * @throws IllegalArgumentException if {@code capacityHint} is non-positive
     * @since 3.0.0
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Stream<T> blockingStream(int capacityHint) {
        Iterator<T> iterator = blockingIterable(capacityHint).iterator();
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator, 0), false)
                .onClose(((Disposable) iterator)::dispose);
    }

    /**
     * Maps each upstream item into a {@link Stream} and emits the {@code Stream}'s items to the downstream in a sequential fashion.
     * <p>
     * <img width="640" height="299" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/concatMapStream.o.png" alt="">
     * <p>
     * Due to the blocking and sequential nature of Java {@code Stream}s, the streams are mapped and consumed in a sequential fashion
     * without interleaving (unlike a more general {@link #flatMap(Function)}). Therefore, {@code flatMapStream} and
     * {@code concatMapStream} are identical operators and are provided as aliases.
     * <p>
     * The operator closes the {@code Stream} upon cancellation and when it terminates. The exceptions raised when
     * closing a {@code Stream} are routed to the global error handler ({@link RxJavaPlugins#onError(Throwable)}.
     * If a {@code Stream} should not be closed, turn it into an {@link Iterable} and use {@link #concatMapIterable(Function)}:
     * <pre><code>
     * source.concatMapIterable(v -&gt; createStream(v)::iterator);
     * </code></pre>
     * <p>
     * Note that {@code Stream}s can be consumed only once; any subsequent attempt to consume a {@code Stream}
     * will result in an {@link IllegalStateException}.
     * <p>
     * Primitive streams are not supported and items have to be boxed manually (e.g., via {@link IntStream#boxed()}):
     * <pre><code>
     * source.concatMapStream(v -&gt; IntStream.rangeClosed(v + 1, v + 10).boxed());
     * </code></pre>
     * <p>
     * {@code Stream} does not support concurrent usage so creating and/or consuming the same instance multiple times
     * from multiple threads can lead to undefined behavior.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code concatMapStream} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <R> the element type of the {@code Stream}s and the result
     * @param mapper the function that receives an upstream item and should return a {@code Stream} whose elements
     * will be emitted to the downstream
     * @return the new {@code Observable} instance
     * @since 3.0.0
     * @throws NullPointerException if {@code mapper} is {@code null}
     * @see #concatMap(Function)
     * @see #concatMapIterable(Function)
     * @see #flatMapStream(Function)
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <@NonNull R> Observable<R> concatMapStream(@NonNull Function<? super T, @NonNull ? extends Stream<? extends R>> mapper) {
        return flatMapStream(mapper);
    }

    /**
     * Maps each upstream item into a {@link Stream} and emits the {@code Stream}'s items to the downstream in a sequential fashion.
     * <p>
     * <img width="640" height="299" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/flatMapStream.o.png" alt="">
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
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code flatMapStream} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <R> the element type of the {@code Stream}s and the result
     * @param mapper the function that receives an upstream item and should return a {@code Stream} whose elements
     * will be emitted to the downstream
     * @return the new {@code Observable} instance
     * @since 3.0.0
     * @throws NullPointerException if {@code mapper} is {@code null}
     * @see #flatMap(Function)
     * @see #flatMapIterable(Function)
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <@NonNull R> Observable<R> flatMapStream(@NonNull Function<? super T, @NonNull ? extends Stream<? extends R>> mapper) {
        Objects.requireNonNull(mapper, "mapper is null");
        return RxJavaPlugins.onAssembly(new ObservableFlatMapStream<>(this, mapper));
    }
}
