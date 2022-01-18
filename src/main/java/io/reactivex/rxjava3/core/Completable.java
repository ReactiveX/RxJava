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
import io.reactivex.rxjava3.internal.operators.maybe.*;
import io.reactivex.rxjava3.internal.operators.mixed.*;
import io.reactivex.rxjava3.internal.operators.single.SingleDelayWithCompletable;
import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * The {@code Completable} class represents a deferred computation without any value but
 * only indication for completion or exception.
 * <p>
 * {@code Completable} behaves similarly to {@link Observable} except that it can only emit either
 * a completion or error signal (there is no {@code onNext} or {@code onSuccess} as with the other
 * reactive types).
 * <p>
 * The {@code Completable} class implements the {@link CompletableSource} base interface and the default consumer
 * type it interacts with is the {@link CompletableObserver} via the {@link #subscribe(CompletableObserver)} method.
 * The {@code Completable} operates with the following sequential protocol:
 * <pre><code>
 *     onSubscribe (onError | onComplete)?
 * </code></pre>
 * <p>
 * Note that as with the {@code Observable} protocol, {@code onError} and {@code onComplete} are mutually exclusive events.
 * <p>
 * Like {@code Observable}, a running {@code Completable} can be stopped through the {@link Disposable} instance
 * provided to consumers through {@link CompletableObserver#onSubscribe}.
 * <p>
 * Like an {@code Observable}, a {@code Completable} is lazy, can be either "hot" or "cold", synchronous or
 * asynchronous. {@code Completable} instances returned by the methods of this class are <em>cold</em>
 * and there is a standard <em>hot</em> implementation in the form of a subject:
 * {@link io.reactivex.rxjava3.subjects.CompletableSubject CompletableSubject}.
 * <p>
 * The documentation for this class makes use of marble diagrams. The following legend explains these diagrams:
 * <p>
 * <img width="640" height="577" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.png" alt="">
 * <p>
 * See {@link Flowable} or {@code Observable} for the
 * implementation of the Reactive Pattern for a stream or vector of values.
 * <p>
 * Example:
 * <pre><code>
 * Disposable d = Completable.complete()
 *    .delay(10, TimeUnit.SECONDS, Schedulers.io())
 *    .subscribeWith(new DisposableCompletableObserver() {
 *        &#64;Override
 *        public void onStart() {
 *            System.out.println("Started");
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
 * Note that by design, subscriptions via {@link #subscribe(CompletableObserver)} can't be disposed
 * from the outside (hence the
 * {@code void} return of the {@link #subscribe(CompletableObserver)} method) and it is the
 * responsibility of the implementor of the {@code CompletableObserver} to allow this to happen.
 * RxJava supports such usage with the standard
 * {@link io.reactivex.rxjava3.observers.DisposableCompletableObserver DisposableCompletableObserver} instance.
 * For convenience, the {@link #subscribeWith(CompletableObserver)} method is provided as well to
 * allow working with a {@code CompletableObserver} (or subclass) instance to be applied with in
 * a fluent manner (such as in the example above).
 *
 * @see io.reactivex.rxjava3.observers.DisposableCompletableObserver
 */
public abstract class Completable implements CompletableSource {
    /**
     * Returns a {@code Completable} which terminates as soon as one of the source {@code Completable}s
     * terminates (normally or with an error) and disposes all other {@code Completable}s.
     * <p>
     * <img width="640" height="518" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.ambArray.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code ambArray} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param sources the array of source {@code Completable}s. A subscription to each source will
     *            occur in the same order as in this array.
     * @return the new {@code Completable} instance
     * @throws NullPointerException if {@code sources} is {@code null}
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    @SafeVarargs
    public static Completable ambArray(@NonNull CompletableSource... sources) {
        Objects.requireNonNull(sources, "sources is null");
        if (sources.length == 0) {
            return complete();
        }
        if (sources.length == 1) {
            return wrap(sources[0]);
        }

        return RxJavaPlugins.onAssembly(new CompletableAmb(sources, null));
    }

    /**
     * Returns a {@code Completable} which terminates as soon as one of the source {@code Completable}s in the {@link Iterable} sequence
     * terminates (normally or with an error) and disposes all other {@code Completable}s.
     * <p>
     * <img width="640" height="518" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.amb.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code amb} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param sources the {@code Iterable} of source {@code Completable}s. A subscription to each source will
     *            occur in the same order as in this {@code Iterable}.
     * @return the new {@code Completable} instance
     * @throws NullPointerException if {@code sources} is {@code null}
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static Completable amb(@NonNull Iterable<@NonNull ? extends CompletableSource> sources) {
        Objects.requireNonNull(sources, "sources is null");

        return RxJavaPlugins.onAssembly(new CompletableAmb(null, sources));
    }

    /**
     * Returns a {@code Completable} instance that completes immediately when subscribed to.
     * <p>
     * <img width="640" height="472" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.complete.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code complete} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @return the shared {@code Completable} instance
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static Completable complete() {
        return RxJavaPlugins.onAssembly(CompletableEmpty.INSTANCE);
    }

    /**
     * Returns a {@code Completable} which completes only when all sources complete, one after another.
     * <p>
     * <img width="640" height="284" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.concatArray.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code concatArray} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param sources the sources to concatenate
     * @return the new {@code Completable} instance
     * @throws NullPointerException if {@code sources} is {@code null}
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    @SafeVarargs
    public static Completable concatArray(@NonNull CompletableSource... sources) {
        Objects.requireNonNull(sources, "sources is null");
        if (sources.length == 0) {
            return complete();
        } else
        if (sources.length == 1) {
            return wrap(sources[0]);
        }
        return RxJavaPlugins.onAssembly(new CompletableConcatArray(sources));
    }

    /**
     * Returns a {@code Completable} which completes only when all sources complete, one after another.
     * <p>
     * <img width="640" height="324" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.concatArrayDelayError.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code concatArrayDelayError} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param sources the sources to concatenate
     * @return the new {@code Completable} instance
     * @throws NullPointerException if {@code sources} is {@code null}
     * @since 3.0.0
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    @SafeVarargs
    public static Completable concatArrayDelayError(@NonNull CompletableSource... sources) {
        return Flowable.fromArray(sources).concatMapCompletableDelayError(Functions.identity(), true, 2);
    }

    /**
     * Returns a {@code Completable} which completes only when all sources complete, one after another.
     * <p>
     * <img width="640" height="303" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.concat.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code concat} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param sources the sources to concatenate
     * @return the new {@code Completable} instance
     * @throws NullPointerException if {@code sources} is {@code null}
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static Completable concat(@NonNull Iterable<@NonNull ? extends CompletableSource> sources) {
        Objects.requireNonNull(sources, "sources is null");

        return RxJavaPlugins.onAssembly(new CompletableConcatIterable(sources));
    }

    /**
     * Returns a {@code Completable} which completes only when all sources complete, one after another.
     * <p>
     * <img width="640" height="238" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.concat.p.png" alt="">
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The returned {@code Completable} honors the backpressure of the downstream consumer
     *  and expects the other {@link Publisher} to honor it as well.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code concat} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param sources the sources to concatenate
     * @return the new {@code Completable} instance
     * @throws NullPointerException if {@code sources} is {@code null}
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @BackpressureSupport(BackpressureKind.FULL)
    @NonNull
    public static Completable concat(@NonNull Publisher<@NonNull ? extends CompletableSource> sources) {
        return concat(sources, 2);
    }

    /**
     * Returns a {@code Completable} which completes only when all sources complete, one after another.
     * <p>
     * <img width="640" height="238" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.concat.pn.png" alt="">
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The returned {@code Completable} honors the backpressure of the downstream consumer
     *  and expects the other {@link Publisher} to honor it as well.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code concat} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param sources the sources to concatenate
     * @param prefetch the number of sources to prefetch from the sources
     * @return the new {@code Completable} instance
     * @throws NullPointerException if {@code sources} is {@code null}
     * @throws IllegalArgumentException if {@code prefetch} is non-positive
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    @BackpressureSupport(BackpressureKind.FULL)
    public static Completable concat(@NonNull Publisher<@NonNull ? extends CompletableSource> sources, int prefetch) {
        Objects.requireNonNull(sources, "sources is null");
        ObjectHelper.verifyPositive(prefetch, "prefetch");
        return RxJavaPlugins.onAssembly(new CompletableConcat(sources, prefetch));
    }

    /**
     * Returns a {@code Completable} which completes only when all sources complete, one after another.
     * <p>
     * <img width="640" height="361" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.concatDelayError.i.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code concatDelayError} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param sources the sources to concatenate
     * @return the new {@code Completable} instance
     * @throws NullPointerException if {@code sources} is {@code null}
     * @since 3.0.0
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static Completable concatDelayError(@NonNull Iterable<@NonNull ? extends CompletableSource> sources) {
        return Flowable.fromIterable(sources).concatMapCompletableDelayError(Functions.identity());
    }

    /**
     * Returns a {@code Completable} which completes only when all sources complete, one after another.
     * <p>
     * <img width="640" height="396" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.concatDelayError.p.png" alt="">
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The returned {@code Completable} honors the backpressure of the downstream consumer
     *  and expects the other {@link Publisher} to honor it as well.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code concatDelayError} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param sources the sources to concatenate
     * @return the new {@code Completable} instance
     * @throws NullPointerException if {@code sources} is {@code null}
     * @since 3.0.0
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @BackpressureSupport(BackpressureKind.FULL)
    @NonNull
    public static Completable concatDelayError(@NonNull Publisher<@NonNull ? extends CompletableSource> sources) {
        return concatDelayError(sources, 2);
    }

    /**
     * Returns a {@code Completable} which completes only when all sources complete, one after another.
     * <p>
     * <img width="640" height="359" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.concatDelayError.pn.png" alt="">
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The returned {@code Completable} honors the backpressure of the downstream consumer
     *  and expects the other {@link Publisher} to honor it as well.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code concatDelayError} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param sources the sources to concatenate
     * @param prefetch the number of sources to prefetch from the sources
     * @return the new {@code Completable} instance
     * @throws NullPointerException if {@code sources} is {@code null}
     * @throws IllegalArgumentException if {@code prefetch} is non-positive
     * @since 3.0.0
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    @BackpressureSupport(BackpressureKind.FULL)
    public static Completable concatDelayError(@NonNull Publisher<@NonNull ? extends CompletableSource> sources, int prefetch) {
        return Flowable.fromPublisher(sources).concatMapCompletableDelayError(Functions.identity(), true, prefetch);
    }

    /**
     * Provides an API (via a cold {@code Completable}) that bridges the reactive world with the callback-style world.
     * <p>
     * <img width="640" height="442" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.create.png" alt="">
     * <p>
     * Example:
     * <pre><code>
     * Completable.create(emitter -&gt; {
     *     Callback listener = new Callback() {
     *         &#64;Override
     *         public void onEvent(Event e) {
     *             emitter.onComplete();
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
     * Whenever a {@link CompletableObserver} subscribes to the returned {@code Completable}, the provided
     * {@link CompletableOnSubscribe} callback is invoked with a fresh instance of a {@link CompletableEmitter}
     * that will interact only with that specific {@code CompletableObserver}. If this {@code CompletableObserver}
     * disposes the flow (making {@link CompletableEmitter#isDisposed} return {@code true}),
     * other observers subscribed to the same returned {@code Completable} are not affected.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code create} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param source the emitter that is called when a {@code CompletableObserver} subscribes to the returned {@code Completable}
     * @return the new {@code Completable} instance
     * @throws NullPointerException if {@code source} is {@code null}
     * @see CompletableOnSubscribe
     * @see Cancellable
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static Completable create(@NonNull CompletableOnSubscribe source) {
        Objects.requireNonNull(source, "source is null");
        return RxJavaPlugins.onAssembly(new CompletableCreate(source));
    }

    /**
     * Compares two {@link CompletableSource}s and emits {@code true} via a {@link Single} if both complete.
     * <p>
     * <img width="640" height="187" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.sequenceEqual.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code sequenceEqual} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param source1 the first {@code CompletableSource} instance
     * @param source2 the second {@code CompletableSource} instance
     * @return the new {@code Single} instance
     * @throws NullPointerException if {@code source1} or {@code source2} is {@code null}
     * @since 3.0.0
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static Single<Boolean> sequenceEqual(@NonNull CompletableSource source1, @NonNull CompletableSource source2) { // NOPMD
        Objects.requireNonNull(source1, "source1 is null");
        Objects.requireNonNull(source2, "source2 is null");
        return mergeArrayDelayError(source1, source2).andThen(Single.just(true));
    }

    /**
     * Constructs a {@code Completable} instance by wrapping the given source callback
     * <strong>without any safeguards; you should manage the lifecycle and response
     * to downstream disposal</strong>.
     * <p>
     * <img width="640" height="260" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.unsafeCreate.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code unsafeCreate} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param onSubscribe the callback which will receive the {@link CompletableObserver} instances
     * when the {@code Completable} is subscribed to.
     * @return the new {@code Completable} instance
     * @throws NullPointerException if {@code onSubscribe} is {@code null}
     * @throws IllegalArgumentException if {@code source} is a {@code Completable}
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static Completable unsafeCreate(@NonNull CompletableSource onSubscribe) {
        Objects.requireNonNull(onSubscribe, "onSubscribe is null");
        if (onSubscribe instanceof Completable) {
            throw new IllegalArgumentException("Use of unsafeCreate(Completable)!");
        }
        return RxJavaPlugins.onAssembly(new CompletableFromUnsafeSource(onSubscribe));
    }

    /**
     * Defers the subscription to a {@code Completable} instance returned by a supplier.
     * <p>
     * <img width="640" height="298" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.defer.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code defer} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param supplier the supplier that returns the {@code Completable} that will be subscribed to.
     * @return the new {@code Completable} instance
     * @throws NullPointerException if {@code supplier} is {@code null}
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static Completable defer(@NonNull Supplier<? extends @NonNull CompletableSource> supplier) {
        Objects.requireNonNull(supplier, "supplier is null");
        return RxJavaPlugins.onAssembly(new CompletableDefer(supplier));
    }

    /**
     * Creates a {@code Completable} which calls the given error supplier for each subscriber
     * and emits its returned {@link Throwable}.
     * <p>
     * <img width="640" height="462" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.error.f.png" alt="">
     * <p>
     * If the {@code errorSupplier} returns {@code null}, the downstream {@link CompletableObserver}s will receive a
     * {@link NullPointerException}.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code error} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param supplier the error supplier, not {@code null}
     * @return the new {@code Completable} instance
     * @throws NullPointerException if {@code supplier} is {@code null}
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static Completable error(@NonNull Supplier<? extends @NonNull Throwable> supplier) {
        Objects.requireNonNull(supplier, "supplier is null");
        return RxJavaPlugins.onAssembly(new CompletableErrorSupplier(supplier));
    }

    /**
     * Creates a {@code Completable} instance that emits the given {@link Throwable} exception to subscribers.
     * <p>
     * <img width="640" height="462" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.error.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code error} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param throwable the {@code Throwable} instance to emit, not {@code null}
     * @return the new {@code Completable} instance
     * @throws NullPointerException if {@code throwable} is {@code null}
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static Completable error(@NonNull Throwable throwable) {
        Objects.requireNonNull(throwable, "throwable is null");
        return RxJavaPlugins.onAssembly(new CompletableError(throwable));
    }

    /**
     * Returns a {@code Completable} instance that runs the given {@link Action} for each {@link CompletableObserver} and
     * emits either an exception or simply completes.
     * <p>
     * <img width="640" height="297" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.fromAction.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code fromAction} does not operate by default on a particular {@link Scheduler}.</dd>
     *  <dt><b>Error handling:</b></dt>
     *  <dd> If the {@code Action} throws an exception, the respective {@link Throwable} is
     *  delivered to the downstream via {@link CompletableObserver#onError(Throwable)},
     *  except when the downstream has disposed this {@code Completable} source.
     *  In this latter case, the {@code Throwable} is delivered to the global error handler via
     *  {@link RxJavaPlugins#onError(Throwable)} as an {@link io.reactivex.rxjava3.exceptions.UndeliverableException UndeliverableException}.
     *  </dd>
     * </dl>
     * @param action the {@code Action} to run for each subscribing {@code CompletableObserver}
     * @return the new {@code Completable} instance
     * @throws NullPointerException if {@code action} is {@code null}
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static Completable fromAction(@NonNull Action action) {
        Objects.requireNonNull(action, "action is null");
        return RxJavaPlugins.onAssembly(new CompletableFromAction(action));
    }

    /**
     * Returns a {@code Completable} which when subscribed, executes the {@link Callable} function, ignores its
     * normal result and emits {@code onError} or {@code onComplete} only.
     * <p>
     * <img width="640" height="286" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.fromCallable.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code fromCallable} does not operate by default on a particular {@link Scheduler}.</dd>
     *  <dt><b>Error handling:</b></dt>
     *  <dd> If the {@code Callable} throws an exception, the respective {@link Throwable} is
     *  delivered to the downstream via {@link CompletableObserver#onError(Throwable)},
     *  except when the downstream has disposed this {@code Completable} source.
     *  In this latter case, the {@code Throwable} is delivered to the global error handler via
     *  {@link RxJavaPlugins#onError(Throwable)} as an {@link io.reactivex.rxjava3.exceptions.UndeliverableException UndeliverableException}.
     *  </dd>
     * </dl>
     * @param callable the {@code Callable} instance to execute for each subscribing {@link CompletableObserver}
     * @return the new {@code Completable} instance
     * @throws NullPointerException if {@code callable} is {@code null}
     * @see #defer(Supplier)
     * @see #fromSupplier(Supplier)
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static Completable fromCallable(@NonNull Callable<?> callable) {
        Objects.requireNonNull(callable, "callable is null");
        return RxJavaPlugins.onAssembly(new CompletableFromCallable(callable));
    }

    /**
     * Returns a {@code Completable} instance that reacts to the termination of the given {@link Future} in a blocking fashion.
     * <p>
     * <img width="640" height="628" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.fromFuture.png" alt="">
     * <p>
     * Note that disposing the {@code Completable} won't cancel the {@code Future}.
     * Use {@link #doOnDispose(Action)} and call {@link Future#cancel(boolean)} in the
     * {@link Action}.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code fromFuture} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param future the {@code Future} to react to
     * @return the new {@code Completable} instance
     * @throws NullPointerException if {@code future} is {@code null}
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static Completable fromFuture(@NonNull Future<?> future) {
        Objects.requireNonNull(future, "future is null");
        return fromAction(Functions.futureAction(future));
    }

    /**
     * Returns a {@code Completable} instance that when subscribed to, subscribes to the {@link MaybeSource} instance and
     * emits an {@code onComplete} event if the maybe emits {@code onSuccess}/{@code onComplete} or forwards any
     * {@code onError} events.
     * <p>
     * <img width="640" height="235" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.fromMaybe.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code fromMaybe} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * <p>History: 2.1.17 - beta
     * @param <T> the value type of the {@code MaybeSource} element
     * @param maybe the {@code MaybeSource} instance to subscribe to, not {@code null}
     * @return the new {@code Completable} instance
     * @throws NullPointerException if {@code maybe} is {@code null}
     * @since 2.2
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <@NonNull T> Completable fromMaybe(@NonNull MaybeSource<T> maybe) {
        Objects.requireNonNull(maybe, "maybe is null");
        return RxJavaPlugins.onAssembly(new MaybeIgnoreElementCompletable<>(maybe));
    }

    /**
     * Returns a {@code Completable} instance that runs the given {@link Runnable} for each {@link CompletableObserver} and
     * emits either its unchecked exception or simply completes.
     * <p>
     * <img width="640" height="297" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.fromRunnable.png" alt="">
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
     *  delivered to the downstream via {@link CompletableObserver#onError(Throwable)},
     *  except when the downstream has disposed this {@code Completable} source.
     *  In this latter case, the {@code Throwable} is delivered to the global error handler via
     *  {@link RxJavaPlugins#onError(Throwable)} as an {@link io.reactivex.rxjava3.exceptions.UndeliverableException UndeliverableException}.
     *  </dd>
     * </dl>
     * @param run the {@code Runnable} to run for each {@code CompletableObserver}
     * @return the new {@code Completable} instance
     * @throws NullPointerException if {@code run} is {@code null}
     * @see #fromAction(Action)
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static Completable fromRunnable(@NonNull Runnable run) {
        Objects.requireNonNull(run, "run is null");
        return RxJavaPlugins.onAssembly(new CompletableFromRunnable(run));
    }

    /**
     * Returns a {@code Completable} instance that subscribes to the given {@link ObservableSource}, ignores all values and
     * emits only the terminal event.
     * <p>
     * <img width="640" height="414" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.fromObservable.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code fromObservable} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the type of the {@code ObservableSource}
     * @param observable the {@code ObservableSource} instance to subscribe to, not {@code null}
     * @return the new {@code Completable} instance
     * @throws NullPointerException if {@code observable} is {@code null}
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <@NonNull T> Completable fromObservable(@NonNull ObservableSource<T> observable) {
        Objects.requireNonNull(observable, "observable is null");
        return RxJavaPlugins.onAssembly(new CompletableFromObservable<>(observable));
    }

    /**
     * Returns a {@code Completable} instance that subscribes to the given {@link Publisher}, ignores all values and
     * emits only the terminal event.
     * <p>
     * <img width="640" height="422" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.fromPublisher.png" alt="">
     * <p>
     * The {@code Publisher} must follow the
     * <a href="https://github.com/reactive-streams/reactive-streams-jvm#reactive-streams">Reactive-Streams specification</a>.
     * Violating the specification may result in undefined behavior.
     * <p>
     * If possible, use {@link #create(CompletableOnSubscribe)} to create a
     * source-like {@code Completable} instead.
     * <p>
     * Note that even though {@code Publisher} appears to be a functional interface, it
     * is not recommended to implement it through a lambda as the specification requires
     * state management that is not achievable with a stateless lambda.
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The returned {@code Completable} honors the backpressure of the downstream consumer
     *  and expects the other {@code Publisher} to honor it as well.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code fromPublisher} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the type of the {@code Publisher}
     * @param publisher the {@code Publisher} instance to subscribe to, not {@code null}
     * @return the new {@code Completable} instance
     * @throws NullPointerException if {@code publisher} is {@code null}
     * @see #create(CompletableOnSubscribe)
     */
    @CheckReturnValue
    @NonNull
    @BackpressureSupport(BackpressureKind.UNBOUNDED_IN)
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <@NonNull T> Completable fromPublisher(@NonNull Publisher<T> publisher) {
        Objects.requireNonNull(publisher, "publisher is null");
        return RxJavaPlugins.onAssembly(new CompletableFromPublisher<>(publisher));
    }

    /**
     * Returns a {@code Completable} instance that when subscribed to, subscribes to the {@link SingleSource} instance and
     * emits a completion event if the single emits {@code onSuccess} or forwards any {@code onError} events.
     * <p>
     * <img width="640" height="356" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.fromSingle.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code fromSingle} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the value type of the {@code SingleSource}
     * @param single the {@code SingleSource} instance to subscribe to, not {@code null}
     * @return the new {@code Completable} instance
     * @throws NullPointerException if {@code single} is {@code null}
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <@NonNull T> Completable fromSingle(@NonNull SingleSource<T> single) {
        Objects.requireNonNull(single, "single is null");
        return RxJavaPlugins.onAssembly(new CompletableFromSingle<>(single));
    }

    /**
     * Returns a {@code Completable} which when subscribed, executes the {@link Supplier} function, ignores its
     * normal result and emits {@code onError} or {@code onComplete} only.
     * <p>
     * <img width="640" height="286" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.fromSupplier.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code fromSupplier} does not operate by default on a particular {@link Scheduler}.</dd>
     *  <dt><b>Error handling:</b></dt>
     *  <dd> If the {@code Supplier} throws an exception, the respective {@link Throwable} is
     *  delivered to the downstream via {@link CompletableObserver#onError(Throwable)},
     *  except when the downstream has disposed this {@code Completable} source.
     *  In this latter case, the {@code Throwable} is delivered to the global error handler via
     *  {@link RxJavaPlugins#onError(Throwable)} as an {@link io.reactivex.rxjava3.exceptions.UndeliverableException UndeliverableException}.
     *  </dd>
     * </dl>
     * @param supplier the {@code Supplier} instance to execute for each {@link CompletableObserver}
     * @return the new {@code Completable} instance
     * @throws NullPointerException if {@code supplier} is {@code null}
     * @see #defer(Supplier)
     * @see #fromCallable(Callable)
     * @since 3.0.0
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static Completable fromSupplier(@NonNull Supplier<?> supplier) {
        Objects.requireNonNull(supplier, "supplier is null");
        return RxJavaPlugins.onAssembly(new CompletableFromSupplier(supplier));
    }

    /**
     * Returns a {@code Completable} instance that subscribes to all sources at once and
     * completes only when all source {@link CompletableSource}s complete or one of them emits an error.
     * <p>
     * <img width="640" height="270" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.mergeArray.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code mergeArray} does not operate by default on a particular {@link Scheduler}.</dd>
     *  <dt><b>Error handling:</b></dt>
     *  <dd>If any of the source {@code CompletableSource}s signal a {@link Throwable} via {@code onError}, the resulting
     *  {@code Completable} terminates with that {@code Throwable} and all other source {@code CompletableSource}s are disposed.
     *  If more than one {@code CompletableSource} signals an error, the resulting {@code Completable} may terminate with the
     *  first one's error or, depending on the concurrency of the sources, may terminate with a
     *  {@link CompositeException} containing two or more of the various error signals.
     *  {@code Throwable}s that didn't make into the composite will be sent (individually) to the global error handler via
     *  {@link RxJavaPlugins#onError(Throwable)} method as {@link UndeliverableException} errors. Similarly, {@code Throwable}s
     *  signaled by source(s) after the returned {@code Completable} has been disposed or terminated with a
     *  (composite) error will be sent to the same global error handler.
     *  Use {@link #mergeArrayDelayError(CompletableSource...)} to merge sources and terminate only when all source {@code CompletableSource}s
     *  have completed or failed with an error.
     *  </dd>
     * </dl>
     * @param sources the array of {@code CompletableSource}s.
     * @return the new {@code Completable} instance
     * @throws NullPointerException if {@code sources} is {@code null}
     * @see #mergeArrayDelayError(CompletableSource...)
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    @SafeVarargs
    public static Completable mergeArray(@NonNull CompletableSource... sources) {
        Objects.requireNonNull(sources, "sources is null");
        if (sources.length == 0) {
            return complete();
        } else
        if (sources.length == 1) {
            return wrap(sources[0]);
        }
        return RxJavaPlugins.onAssembly(new CompletableMergeArray(sources));
    }

    /**
     * Returns a {@code Completable} instance that subscribes to all sources at once and
     * completes only when all source {@link CompletableSource}s complete or one of them emits an error.
     * <p>
     * <img width="640" height="311" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.merge.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code merge} does not operate by default on a particular {@link Scheduler}.</dd>
     *  <dt><b>Error handling:</b></dt>
     *  <dd>If any of the source {@code CompletableSource}s signal a {@link Throwable} via {@code onError}, the resulting
     *  {@code Completable} terminates with that {@code Throwable} and all other source {@code CompletableSource}s are disposed.
     *  If more than one {@code CompletableSource} signals an error, the resulting {@code Completable} may terminate with the
     *  first one's error or, depending on the concurrency of the sources, may terminate with a
     *  {@link CompositeException} containing two or more of the various error signals.
     *  {@code Throwable}s that didn't make into the composite will be sent (individually) to the global error handler via
     *  {@link RxJavaPlugins#onError(Throwable)} method as {@link UndeliverableException} errors. Similarly, {@code Throwable}s
     *  signaled by source(s) after the returned {@code Completable} has been disposed or terminated with a
     *  (composite) error will be sent to the same global error handler.
     *  Use {@link #mergeDelayError(Iterable)} to merge sources and terminate only when all source {@code CompletableSource}s
     *  have completed or failed with an error.
     *  </dd>
     * </dl>
     * @param sources the {@link Iterable} sequence of {@code CompletableSource}s.
     * @return the new {@code Completable} instance
     * @throws NullPointerException if {@code sources} is {@code null}
     * @see #mergeDelayError(Iterable)
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static Completable merge(@NonNull Iterable<@NonNull ? extends CompletableSource> sources) {
        Objects.requireNonNull(sources, "sources is null");
        return RxJavaPlugins.onAssembly(new CompletableMergeIterable(sources));
    }

    /**
     * Returns a {@code Completable} instance that subscribes to all sources at once and
     * completes only when all source {@link CompletableSource}s complete or one of them emits an error.
     * <p>
     * <img width="640" height="336" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.merge.p.png" alt="">
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The operator consumes the given {@link Publisher} in an unbounded manner
     *  (requesting {@link Long#MAX_VALUE} upfront).</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code merge} does not operate by default on a particular {@link Scheduler}.</dd>
     *  <dt><b>Error handling:</b></dt>
     *  <dd>If any of the source {@code CompletableSource}s signal a {@link Throwable} via {@code onError}, the resulting
     *  {@code Completable} terminates with that {@code Throwable} and all other source {@code CompletableSource}s are disposed.
     *  If more than one {@code CompletableSource} signals an error, the resulting {@code Completable} may terminate with the
     *  first one's error or, depending on the concurrency of the sources, may terminate with a
     *  {@link CompositeException} containing two or more of the various error signals.
     *  {@code Throwable}s that didn't make into the composite will be sent (individually) to the global error handler via
     *  {@link RxJavaPlugins#onError(Throwable)} method as {@link UndeliverableException} errors. Similarly, {@code Throwable}s
     *  signaled by source(s) after the returned {@code Completable} has been disposed or terminated with a
     *  (composite) error will be sent to the same global error handler.
     *  Use {@link #mergeDelayError(Publisher)} to merge sources and terminate only when all source {@code CompletableSource}s
     *  have completed or failed with an error.
     *  </dd>
     * </dl>
     * @param sources the {@code Publisher} sequence of {@code CompletableSource}s.
     * @return the new {@code Completable} instance
     * @throws NullPointerException if {@code sources} is {@code null}
     * @see #mergeDelayError(Publisher)
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @BackpressureSupport(BackpressureKind.UNBOUNDED_IN)
    @NonNull
    public static Completable merge(@NonNull Publisher<@NonNull ? extends CompletableSource> sources) {
        return merge0(sources, Integer.MAX_VALUE, false);
    }

    /**
     * Returns a {@code Completable} instance that keeps subscriptions to a limited number of sources at once and
     * completes only when all source {@link CompletableSource}s complete or one of them emits an error.
     * <p>
     * <img width="640" height="269" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.merge.pn.png" alt="">
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The operator consumes the given {@link Publisher} in a bounded manner,
     *  requesting {@code maxConcurrency} items first, then keeps requesting as
     *  many more as the inner {@code CompletableSource}s terminate.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code merge} does not operate by default on a particular {@link Scheduler}.</dd>
     *  <dt><b>Error handling:</b></dt>
     *  <dd>If any of the source {@code CompletableSource}s signal a {@link Throwable} via {@code onError}, the resulting
     *  {@code Completable} terminates with that {@code Throwable} and all other source {@code CompletableSource}s are disposed.
     *  If more than one {@code CompletableSource} signals an error, the resulting {@code Completable} may terminate with the
     *  first one's error or, depending on the concurrency of the sources, may terminate with a
     *  {@link CompositeException} containing two or more of the various error signals.
     *  {@code Throwable}s that didn't make into the composite will be sent (individually) to the global error handler via
     *  {@link RxJavaPlugins#onError(Throwable)} method as {@link UndeliverableException} errors. Similarly, {@code Throwable}s
     *  signaled by source(s) after the returned {@code Completable} has been disposed or terminated with a
     *  (composite) error will be sent to the same global error handler.
     *  Use {@link #mergeDelayError(Publisher, int)} to merge sources and terminate only when all source {@code CompletableSource}s
     *  have completed or failed with an error.
     *  </dd>
     * </dl>
     * @param sources the {@code Publisher} sequence of {@code CompletableSource}s.
     * @param maxConcurrency the maximum number of concurrent subscriptions
     * @return the new {@code Completable} instance
     * @throws NullPointerException if {@code sources} is {@code null}
     * @throws IllegalArgumentException if {@code maxConcurrency} is less than 1
     * @see #mergeDelayError(Publisher, int)
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @BackpressureSupport(BackpressureKind.FULL)
    @NonNull
    public static Completable merge(@NonNull Publisher<@NonNull ? extends CompletableSource> sources, int maxConcurrency) {
        return merge0(sources, maxConcurrency, false);
    }

    /**
     * Returns a {@code Completable} instance that keeps subscriptions to a limited number of {@link CompletableSource}s at once and
     * completes only when all source {@code CompletableSource}s terminate in one way or another, combining any exceptions
     * signaled by either the source {@link Publisher} or the inner {@code CompletableSource} instances.
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The operator consumes the given {@code Publisher} in a bounded manner,
     *  requesting {@code maxConcurrency} items first, then keeps requesting as
     *  many more as the inner {@code CompletableSource}s terminate.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code merge0} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param sources the {@code Publisher} sequence of {@code CompletableSource}s.
     * @param maxConcurrency the maximum number of concurrent subscriptions
     * @param delayErrors delay all errors from the main source and from the inner {@code CompletableSource}s?
     * @return the new {@code Completable} instance
     * @throws NullPointerException if {@code sources} is {@code null}
     * @throws IllegalArgumentException if {@code maxConcurrency} is less than 1
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    @BackpressureSupport(BackpressureKind.FULL)
    private static Completable merge0(@NonNull Publisher<@NonNull ? extends CompletableSource> sources, int maxConcurrency, boolean delayErrors) {
        Objects.requireNonNull(sources, "sources is null");
        ObjectHelper.verifyPositive(maxConcurrency, "maxConcurrency");
        return RxJavaPlugins.onAssembly(new CompletableMerge(sources, maxConcurrency, delayErrors));
    }

    /**
     * Returns a {@code Completable} that subscribes to all {@link CompletableSource}s in the source array and delays
     * any error emitted by any of the inner {@code CompletableSource}s until all of
     * them terminate in a way or another.
     * <p>
     * <img width="640" height="430" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.mergeArrayDelayError.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code mergeArrayDelayError} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param sources the array of {@code CompletableSource}s
     * @return the new {@code Completable} instance
     * @throws NullPointerException if {@code sources} is {@code null}
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    @SafeVarargs
    public static Completable mergeArrayDelayError(@NonNull CompletableSource... sources) {
        Objects.requireNonNull(sources, "sources is null");
        return RxJavaPlugins.onAssembly(new CompletableMergeArrayDelayError(sources));
    }

    /**
     * Returns a {@code Completable} that subscribes to all {@link CompletableSource}s in the source sequence and delays
     * any error emitted by any of the inner {@code CompletableSource}s until all of
     * them terminate in a way or another.
     * <p>
     * <img width="640" height="476" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.mergeDelayError.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code mergeDelayError} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param sources the sequence of {@code CompletableSource}s
     * @return the new {@code Completable} instance
     * @throws NullPointerException if {@code sources} is {@code null}
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static Completable mergeDelayError(@NonNull Iterable<@NonNull ? extends CompletableSource> sources) {
        Objects.requireNonNull(sources, "sources is null");
        return RxJavaPlugins.onAssembly(new CompletableMergeDelayErrorIterable(sources));
    }

    /**
     * Returns a {@code Completable} that subscribes to all {@link CompletableSource}s in the source sequence and delays
     * any error emitted by either the sources {@link Publisher} or any of the inner {@code CompletableSource}s until all of
     * them terminate in a way or another.
     * <p>
     * <img width="640" height="466" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.mergeDelayError.p.png" alt="">
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The operator consumes the {@code Publisher} in an unbounded manner
     *  (requesting {@link Long#MAX_VALUE} from it).</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code mergeDelayError} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param sources the sequence of {@code CompletableSource}s
     * @return the new {@code Completable} instance
     * @throws NullPointerException if {@code sources} is {@code null}
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @BackpressureSupport(BackpressureKind.UNBOUNDED_IN)
    @NonNull
    public static Completable mergeDelayError(@NonNull Publisher<@NonNull ? extends CompletableSource> sources) {
        return merge0(sources, Integer.MAX_VALUE, true);
    }

    /**
     * Returns a {@code Completable} that subscribes to a limited number of inner {@link CompletableSource}s at once in
     * the source sequence and delays any error emitted by either the sources
     * {@link Publisher} or any of the inner {@code CompletableSource}s until all of
     * them terminate in a way or another.
     * <p>
     * <img width="640" height="440" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.mergeDelayError.pn.png" alt="">
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The operator requests {@code maxConcurrency} items from the {@code Publisher}
     *  upfront and keeps requesting as many more as many inner {@code CompletableSource}s terminate.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code mergeDelayError} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param sources the sequence of {@code CompletableSource}s
     * @param maxConcurrency the maximum number of concurrent subscriptions to have
     *                       at a time to the inner {@code CompletableSource}s
     * @return the new {@code Completable} instance
     * @throws NullPointerException if {@code sources} is {@code null}
     * @throws IllegalArgumentException if {@code maxConcurrency} is non-positive
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @BackpressureSupport(BackpressureKind.FULL)
    @NonNull
    public static Completable mergeDelayError(@NonNull Publisher<@NonNull ? extends CompletableSource> sources, int maxConcurrency) {
        return merge0(sources, maxConcurrency, true);
    }

    /**
     * Returns a {@code Completable} that never calls {@code onError} or {@code onComplete}.
     * <p>
     * <img width="640" height="512" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.never.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code never} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @return the singleton instance that never calls {@code onError} or {@code onComplete}
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public static Completable never() {
        return RxJavaPlugins.onAssembly(CompletableNever.INSTANCE);
    }

    /**
     * Returns a {@code Completable} instance that fires its {@code onComplete} event after the given delay elapsed.
     * <p>
     * <img width="640" height="413" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.timer.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code timer} does operate by default on the {@code computation} {@link Scheduler}.</dd>
     * </dl>
     * @param delay the delay time
     * @param unit the delay unit
     * @return the new {@code Completable} instance
     * @throws NullPointerException if {@code unit} is {@code null}
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.COMPUTATION)
    @NonNull
    public static Completable timer(long delay, @NonNull TimeUnit unit) {
        return timer(delay, unit, Schedulers.computation());
    }

    /**
     * Returns a {@code Completable} instance that fires its {@code onComplete} event after the given delay elapsed
     * by using the supplied {@link Scheduler}.
     * <p>
     * <img width="640" height="413" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.timer.s.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code timer} operates on the {@code Scheduler} you specify.</dd>
     * </dl>
     * @param delay the delay time
     * @param unit the delay unit
     * @param scheduler the {@code Scheduler} where to emit the {@code onComplete} event
     * @return the new {@code Completable} instance
     * @throws NullPointerException if {@code unit} or {@code scheduler} is {@code null}
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    public static Completable timer(long delay, @NonNull TimeUnit unit, @NonNull Scheduler scheduler) {
        Objects.requireNonNull(unit, "unit is null");
        Objects.requireNonNull(scheduler, "scheduler is null");
        return RxJavaPlugins.onAssembly(new CompletableTimer(delay, unit, scheduler));
    }

    /**
     * Creates a {@link NullPointerException} instance and sets the given {@link Throwable} as its initial cause.
     * @param ex the {@code Throwable} instance to use as cause, not {@code null} (not verified)
     * @return the new {@code NullPointerException}
     */
    private static NullPointerException toNpe(Throwable ex) {
        NullPointerException npe = new NullPointerException("Actually not, but can't pass out an exception otherwise...");
        npe.initCause(ex);
        return npe;
    }

    /**
     * Switches between {@link CompletableSource}s emitted by the source {@link Publisher} whenever
     * a new {@code CompletableSource} is emitted, disposing the previously running {@code CompletableSource},
     * exposing the setup as a {@code Completable} sequence.
     * <p>
     * <img width="640" height="518" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.switchOnNext.png" alt="">
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The {@code sources} {@code Publisher} is consumed in an unbounded manner (requesting {@link Long#MAX_VALUE}).</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code switchOnNext} does not operate by default on a particular {@link Scheduler}.</dd>
     *  <dt><b>Error handling:</b></dt>
     *  <dd>The returned sequence fails with the first error signaled by the {@code sources} {@code Publisher}
     *  or the currently running {@code CompletableSource}, disposing the rest. Late errors are
     *  forwarded to the global error handler via {@link RxJavaPlugins#onError(Throwable)}.</dd>
     * </dl>
     * @param sources the {@code Publisher} sequence of inner {@code CompletableSource}s to switch between
     * @return the new {@code Completable} instance
     * @throws NullPointerException if {@code sources} is {@code null}
     * @since 3.0.0
     * @see #switchOnNextDelayError(Publisher)
     * @see <a href="http://reactivex.io/documentation/operators/switch.html">ReactiveX operators documentation: Switch</a>
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    @BackpressureSupport(BackpressureKind.UNBOUNDED_IN)
    public static Completable switchOnNext(@NonNull Publisher<@NonNull ? extends CompletableSource> sources) {
        Objects.requireNonNull(sources, "sources is null");
        return RxJavaPlugins.onAssembly(new FlowableSwitchMapCompletablePublisher<>(sources, Functions.identity(), false));
    }

    /**
     * Switches between {@link CompletableSource}s emitted by the source {@link Publisher} whenever
     * a new {@code CompletableSource} is emitted, disposing the previously running {@code CompletableSource},
     * exposing the setup as a {@code Completable} sequence and delaying all errors from
     * all of them until all terminate.
     * <p>
     * <img width="640" height="415" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.switchOnNextDelayError.png" alt="">
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The {@code sources} {@code Publisher} is consumed in an unbounded manner (requesting {@link Long#MAX_VALUE}).</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code switchOnNextDelayError} does not operate by default on a particular {@link Scheduler}.</dd>
     *  <dt><b>Error handling:</b></dt>
     *  <dd>The returned {@code Completable} collects all errors emitted by either the {@code sources}
     *  {@code Publisher} or any inner {@code CompletableSource} and emits them as a {@link CompositeException}
     *  when all sources terminate. If only one source ever failed, its error is emitted as-is at the end.</dd>
     * </dl>
     * @param sources the {@code Publisher} sequence of inner {@code CompletableSource}s to switch between
     * @return the new {@code Completable} instance
     * @throws NullPointerException if {@code sources} is {@code null}
     * @since 3.0.0
     * @see #switchOnNext(Publisher)
     * @see <a href="http://reactivex.io/documentation/operators/switch.html">ReactiveX operators documentation: Switch</a>
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    @BackpressureSupport(BackpressureKind.UNBOUNDED_IN)
    public static Completable switchOnNextDelayError(@NonNull Publisher<@NonNull ? extends CompletableSource> sources) {
        Objects.requireNonNull(sources, "sources is null");
        return RxJavaPlugins.onAssembly(new FlowableSwitchMapCompletablePublisher<>(sources, Functions.identity(), true));
    }

    /**
     * Returns a {@code Completable} instance which manages a resource along
     * with a custom {@link CompletableSource} instance while the subscription is active.
     * <p>
     * <img width="640" height="389" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.using.png" alt="">
     * <p>
     * This overload disposes eagerly before the terminal event is emitted.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code using} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <R> the resource type
     * @param resourceSupplier the {@link Supplier} that returns a resource to be managed.
     * @param sourceSupplier the {@link Function} that given a resource returns a {@code CompletableSource} instance that will be subscribed to
     * @param resourceCleanup the {@link Consumer} that disposes the resource created by the resource supplier
     * @return the new {@code Completable} instance
     * @throws NullPointerException if {@code resourceSupplier}, {@code sourceSupplier}
     *                              or {@code resourceCleanup} is {@code null}
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public static <@NonNull R> Completable using(@NonNull Supplier<R> resourceSupplier,
            @NonNull Function<? super R, ? extends CompletableSource> sourceSupplier,
            @NonNull Consumer<? super R> resourceCleanup) {
        return using(resourceSupplier, sourceSupplier, resourceCleanup, true);
    }

    /**
     * Returns a {@code Completable} instance which manages a resource along
     * with a custom {@link CompletableSource} instance while the subscription is active and performs eager or lazy
     * resource disposition.
     * <p>
     * <img width="640" height="332" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.using.b.png" alt="">
     * <p>
     * If this overload performs a lazy disposal after the terminal event is emitted.
     * The exceptions thrown at this time will be delivered to the global {@link RxJavaPlugins#onError(Throwable)} handler only.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code using} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <R> the resource type
     * @param resourceSupplier the {@link Supplier} that returns a resource to be managed
     * @param sourceSupplier the {@link Function} that given a resource returns a non-{@code null}
     * {@code CompletableSource} instance that will be subscribed to
     * @param resourceCleanup the {@link Consumer} that disposes the resource created by the resource supplier
     * @param eager
     *            If {@code true} then resource disposal will happen either on a {@code dispose()} call before the upstream is disposed
     *            or just before the emission of a terminal event ({@code onComplete} or {@code onError}).
     *            If {@code false} the resource disposal will happen either on a {@code dispose()} call after the upstream is disposed
     *            or just after the emission of a terminal event ({@code onComplete} or {@code onError}).
     * @return the new {@code Completable} instance
     * @throws NullPointerException if {@code resourceSupplier}, {@code sourceSupplier}
     *                              or {@code resourceCleanup} is {@code null}
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <@NonNull R> Completable using(
            @NonNull Supplier<R> resourceSupplier,
            @NonNull Function<? super R, ? extends CompletableSource> sourceSupplier,
            @NonNull Consumer<? super R> resourceCleanup,
            boolean eager) {
        Objects.requireNonNull(resourceSupplier, "resourceSupplier is null");
        Objects.requireNonNull(sourceSupplier, "sourceSupplier is null");
        Objects.requireNonNull(resourceCleanup, "resourceCleanup is null");

        return RxJavaPlugins.onAssembly(new CompletableUsing<>(resourceSupplier, sourceSupplier, resourceCleanup, eager));
    }

    /**
     * Wraps the given {@link CompletableSource} into a {@code Completable}
     * if not already {@code Completable}.
     * <p>
     * <img width="640" height="354" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.wrap.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code wrap} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param source the source to wrap
     * @return the new wrapped or cast {@code Completable} instance
     * @throws NullPointerException if {@code source} is {@code null}
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static Completable wrap(@NonNull CompletableSource source) {
        Objects.requireNonNull(source, "source is null");
        if (source instanceof Completable) {
            return RxJavaPlugins.onAssembly((Completable)source);
        }
        return RxJavaPlugins.onAssembly(new CompletableFromUnsafeSource(source));
    }

    /**
     * Returns a {@code Completable} that emits the a terminated event of either this {@code Completable}
     * or the other {@link CompletableSource}, whichever fires first.
     * <p>
     * <img width="640" height="485" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.ambWith.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code ambWith} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param other the other {@code CompletableSource}, not {@code null}. A subscription to this provided source will occur after subscribing
     *            to the current source.
     * @return the new {@code Completable} instance
     * @throws NullPointerException if {@code other} is {@code null}
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Completable ambWith(@NonNull CompletableSource other) {
        Objects.requireNonNull(other, "other is null");
        return ambArray(this, other);
    }

    /**
     * Returns an {@link Observable} which will subscribe to this {@code Completable} and once that is completed then
     * will subscribe to the {@code next} {@link ObservableSource}. An error event from this {@code Completable} will be
     * propagated to the downstream observer and will result in skipping the subscription to the
     * next {@code ObservableSource}.
     * <p>
     * <img width="640" height="278" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.andThen.o.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code andThen} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the value type of the next {@code ObservableSource}
     * @param next the {@code ObservableSource} to subscribe after this {@code Completable} is completed, not {@code null}
     * @return the new {@code Observable} that composes this {@code Completable} and the next {@code ObservableSource}
     * @throws NullPointerException if {@code next} is {@code null}
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public final <@NonNull T> Observable<T> andThen(@NonNull ObservableSource<T> next) {
        Objects.requireNonNull(next, "next is null");
        return RxJavaPlugins.onAssembly(new CompletableAndThenObservable<>(this, next));
    }

    /**
     * Returns a {@link Flowable} which will subscribe to this {@code Completable} and once that is completed then
     * will subscribe to the {@code next} {@link Publisher}. An error event from this {@code Completable} will be
     * propagated to the downstream subscriber and will result in skipping the subscription to the next
     * {@code Publisher}.
     * <p>
     * <img width="640" height="249" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.andThen.p.png" alt="">
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The returned {@code Flowable} honors the backpressure of the downstream consumer
     *  and expects the other {@code Publisher} to honor it as well.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code andThen} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the value type of the next {@code Publisher}
     * @param next the {@code Publisher} to subscribe after this {@code Completable} is completed, not {@code null}
     * @return the new {@code Flowable} that composes this {@code Completable} and the next {@code Publisher}
     * @throws NullPointerException if {@code next} is {@code null}
     */
    @CheckReturnValue
    @NonNull
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerSupport.NONE)
    public final <@NonNull T> Flowable<T> andThen(@NonNull Publisher<T> next) {
        Objects.requireNonNull(next, "next is null");
        return RxJavaPlugins.onAssembly(new CompletableAndThenPublisher<>(this, next));
    }

    /**
     * Returns a {@link Single} which will subscribe to this {@code Completable} and once that is completed then
     * will subscribe to the {@code next} {@link SingleSource}. An error event from this {@code Completable} will be
     * propagated to the downstream observer and will result in skipping the subscription to the next
     * {@code SingleSource}.
     * <p>
     * <img width="640" height="437" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.andThen.s.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code andThen} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T> the value type of the next {@code SingleSource}
     * @param next the {@code SingleSource} to subscribe after this {@code Completable} is completed, not {@code null}
     * @return the new {@code Single} that composes this {@code Completable} and the next {@code SingleSource}
     * @throws NullPointerException if {@code next} is {@code null}
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public final <@NonNull T> Single<T> andThen(@NonNull SingleSource<T> next) {
        Objects.requireNonNull(next, "next is null");
        return RxJavaPlugins.onAssembly(new SingleDelayWithCompletable<>(next, this));
    }

    /**
     * Returns a {@link Maybe} which will subscribe to this {@code Completable} and once that is completed then
     * will subscribe to the {@code next} {@link MaybeSource}. An error event from this {@code Completable} will be
     * propagated to the downstream observer and will result in skipping the subscription to the next
     * {@code MaybeSource}.
     * <p>
     * <img width="640" height="281" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.andThen.m.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code andThen} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T> the value type of the next {@code MaybeSource}
     * @param next the {@code MaybeSource} to subscribe after this {@code Completable} is completed, not {@code null}
     * @return the new {@code Maybe} that composes this {@code Completable} and the next {@code MaybeSource}
     * @throws NullPointerException if {@code next} is {@code null}
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public final <@NonNull T> Maybe<T> andThen(@NonNull MaybeSource<T> next) {
        Objects.requireNonNull(next, "next is null");
        return RxJavaPlugins.onAssembly(new MaybeDelayWithCompletable<>(next, this));
    }

    /**
     * Returns a {@code Completable} that first runs this {@code Completable}
     * and then the other {@link CompletableSource}. An error event from this {@code Completable} will be
     * propagated to the downstream observer and will result in skipping the subscription to the next
     * {@code CompletableSource}.
     * <p>
     * <img width="640" height="437" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.andThen.c.png" alt="">
     * <p>
     * This is an alias for {@link #concatWith(CompletableSource)}.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code andThen} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param next the other {@code CompletableSource}, not {@code null}
     * @return the new {@code Completable} instance
     * @throws NullPointerException if {@code next} is {@code null}
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Completable andThen(@NonNull CompletableSource next) {
        Objects.requireNonNull(next, "next is null");
        return RxJavaPlugins.onAssembly(new CompletableAndThenCompletable(this, next));
    }

    /**
     * Subscribes to and awaits the termination of this {@code Completable} instance in a blocking manner and
     * rethrows any exception emitted.
     * <p>
     * <img width="640" height="433" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.blockingAwait.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code blockingAwait} does not operate by default on a particular {@link Scheduler}.</dd>
     *  <dt><b>Error handling:</b></dt>
     *  <dd>If the source signals an error, the operator wraps a checked {@link Exception}
     *  into {@link RuntimeException} and throws that. Otherwise, {@code RuntimeException}s and
     *  {@link Error}s are rethrown as they are.</dd>
     * </dl>
     * @throws RuntimeException wrapping an {@link InterruptedException} if the current thread is interrupted
     */
    @SchedulerSupport(SchedulerSupport.NONE)
    public final void blockingAwait() {
        BlockingMultiObserver<Void> observer = new BlockingMultiObserver<>();
        subscribe(observer);
        observer.blockingGet();
    }

    /**
     * Subscribes to and awaits the termination of this {@code Completable} instance in a blocking manner
     * with a specific timeout and rethrows any exception emitted within the timeout window.
     * <p>
     * <img width="640" height="348" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.blockingAwait.t.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code blockingAwait} does not operate by default on a particular {@link Scheduler}.</dd>
     *  <dt><b>Error handling:</b></dt>
     *  <dd>If the source signals an error, the operator wraps a checked {@link Exception}
     *  into {@link RuntimeException} and throws that. Otherwise, {@code RuntimeException}s and
     *  {@link Error}s are rethrown as they are.</dd>
     * </dl>
     * @param timeout the timeout value
     * @param unit the timeout unit
     * @return {@code true} if the this {@code Completable} instance completed normally within the time limit,
     * {@code false} if the timeout elapsed before this {@code Completable} terminated.
     * @throws RuntimeException wrapping an {@link InterruptedException} if the current thread is interrupted
     * @throws NullPointerException if {@code unit} is {@code null}
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public final boolean blockingAwait(long timeout, @NonNull TimeUnit unit) {
        Objects.requireNonNull(unit, "unit is null");
        BlockingMultiObserver<Void> observer = new BlockingMultiObserver<>();
        subscribe(observer);
        return observer.blockingAwait(timeout, unit);
    }

    /**
     * Subscribes to the current {@code Completable} and <em>blocks the current thread</em> until it terminates.
     * <p>
     * <img width="640" height="346" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.blockingSubscribe.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code blockingSubscribe} does not operate by default on a particular {@link Scheduler}.</dd>
     *  <dt><b>Error handling:</b></dt>
     *  <dd>If the current {@code Completable} signals an error,
     *  the {@link Throwable} is routed to the global error handler via {@link RxJavaPlugins#onError(Throwable)}.
     *  If the current thread is interrupted, an {@link InterruptedException} is routed to the same global error handler.
     *  </dd>
     * </dl>
     * @since 3.0.0
     * @see #blockingSubscribe(Action)
     * @see #blockingSubscribe(Action, Consumer)
     */
    @SchedulerSupport(SchedulerSupport.NONE)
    public final void blockingSubscribe() {
        blockingSubscribe(Functions.EMPTY_ACTION, Functions.ERROR_CONSUMER);
    }

    /**
     * Subscribes to the current {@code Completable} and calls given {@code onComplete} callback on the <em>current thread</em>
     * when it completes normally.
     * <p>
     * <img width="640" height="351" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.blockingSubscribe.a.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code blockingSubscribe} does not operate by default on a particular {@link Scheduler}.</dd>
     *  <dt><b>Error handling:</b></dt>
     *  <dd>If either the current {@code Completable} signals an error or {@code onComplete} throws,
     *  the respective {@link Throwable} is routed to the global error handler via {@link RxJavaPlugins#onError(Throwable)}.
     *  If the current thread is interrupted, an {@link InterruptedException} is routed to the same global error handler.
     *  </dd>
     * </dl>
     * @param onComplete the {@link Action} to call if the current {@code Completable} completes normally
     * @throws NullPointerException if {@code onComplete} is {@code null}
     * @since 3.0.0
     * @see #blockingSubscribe(Action, Consumer)
     */
    @SchedulerSupport(SchedulerSupport.NONE)
    public final void blockingSubscribe(@NonNull Action onComplete) {
        blockingSubscribe(onComplete, Functions.ERROR_CONSUMER);
    }

    /**
     * Subscribes to the current {@code Completable} and calls the appropriate callback on the <em>current thread</em>
     * when it terminates.
     * <p>
     * <img width="640" height="352" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.blockingSubscribe.ac.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code blockingSubscribe} does not operate by default on a particular {@link Scheduler}.</dd>
     *  <dt><b>Error handling:</b></dt>
     *  <dd>If either {@code onComplete} or {@code onError} throw, the {@link Throwable} is routed to the
     *  global error handler via {@link RxJavaPlugins#onError(Throwable)}.
     *  If the current thread is interrupted, the {@code onError} consumer is called with an {@link InterruptedException}.
     *  </dd>
     * </dl>
     * @param onComplete the {@link Action} to call if the current {@code Completable} completes normally
     * @param onError the {@link Consumer} to call if the current {@code Completable} signals an error
     * @throws NullPointerException if {@code onComplete} or {@code onError} is {@code null}
     * @since 3.0.0
     */
    @SchedulerSupport(SchedulerSupport.NONE)
    public final void blockingSubscribe(@NonNull Action onComplete, @NonNull Consumer<? super Throwable> onError) {
        Objects.requireNonNull(onComplete, "onComplete is null");
        Objects.requireNonNull(onError, "onError is null");
        BlockingMultiObserver<Void> observer = new BlockingMultiObserver<>();
        subscribe(observer);
        observer.blockingConsume(Functions.emptyConsumer(), onError, onComplete);
    }

    /**
     * Subscribes to the current {@code Completable} and calls the appropriate {@link CompletableObserver} method on the <em>current thread</em>.
     * <p>
     * <img width="640" height="468" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.blockingSubscribe.o.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code blockingSubscribe} does not operate by default on a particular {@link Scheduler}.</dd>
     *  <dt><b>Error handling:</b></dt>
     *  <dd>An {@code onError} signal is delivered to the {@link CompletableObserver#onError(Throwable)} method.
     *  If any of the {@code CompletableObserver}'s methods throw, the {@link RuntimeException} is propagated to the caller of this method.
     *  If the current thread is interrupted, an {@link InterruptedException} is delivered to {@code observer.onError}.
     *  </dd>
     * </dl>
     * @param observer the {@code CompletableObserver} to call methods on the current thread
     * @throws NullPointerException if {@code observer} is {@code null}
     * @since 3.0.0
     */
    @SchedulerSupport(SchedulerSupport.NONE)
    public final void blockingSubscribe(@NonNull CompletableObserver observer) {
        Objects.requireNonNull(observer, "observer is null");
        BlockingDisposableMultiObserver<Void> blockingObserver = new BlockingDisposableMultiObserver<>();
        observer.onSubscribe(blockingObserver);
        subscribe(blockingObserver);
        blockingObserver.blockingConsume(observer);
    }

    /**
     * Subscribes to this {@code Completable} only once, when the first {@link CompletableObserver}
     * subscribes to the result {@code Completable}, caches its terminal event
     * and relays/replays it to observers.
     * <p>
     * <img width="640" height="375" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.cache.png" alt="">
     * <p>
     * Note that this operator doesn't allow disposing the connection
     * of the upstream source.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code cache} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * <p>History: 2.0.4 - experimental
     * @return the new {@code Completable} instance
     * @since 2.1
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Completable cache() {
        return RxJavaPlugins.onAssembly(new CompletableCache(this));
    }

    /**
     * Calls the given transformer function with this instance and returns the function's resulting
     * {@link CompletableSource} wrapped with {@link #wrap(CompletableSource)}.
     * <p>
     * <img width="640" height="625" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.compose.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code compose} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param transformer the transformer function, not {@code null}
     * @return the new {@code Completable} instance
     * @throws NullPointerException if {@code transformer} is {@code null}
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Completable compose(@NonNull CompletableTransformer transformer) {
        return wrap(Objects.requireNonNull(transformer, "transformer is null").apply(this));
    }

    /**
     * Concatenates this {@code Completable} with another {@link CompletableSource}.
     * An error event from this {@code Completable} will be
     * propagated to the downstream observer and will result in skipping the subscription to the next
     * {@code CompletableSource}.
     * <p>
     * <img width="640" height="317" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.concatWith.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code concatWith} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param other the other {@code CompletableSource}, not {@code null}
     * @return the new {@code Completable} which subscribes to this and then the other {@code CompletableSource}
     * @throws NullPointerException if {@code other} is {@code null}
     * @see #andThen(CompletableSource)
     * @see #andThen(MaybeSource)
     * @see #andThen(ObservableSource)
     * @see #andThen(SingleSource)
     * @see #andThen(Publisher)
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Completable concatWith(@NonNull CompletableSource other) {
        Objects.requireNonNull(other, "other is null");
        return RxJavaPlugins.onAssembly(new CompletableAndThenCompletable(this, other));
    }

    /**
     * Returns a {@code Completable} which delays the emission of the completion event by the given time.
     * <p>
     * <img width="640" height="344" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.delay.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code delay} does operate by default on the {@code computation} {@link Scheduler}.</dd>
     * </dl>
     * @param time the delay time
     * @param unit the delay unit
     * @return the new {@code Completable} instance
     * @throws NullPointerException if {@code unit} is {@code null}
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.COMPUTATION)
    @NonNull
    public final Completable delay(long time, @NonNull TimeUnit unit) {
        return delay(time, unit, Schedulers.computation(), false);
    }

    /**
     * Returns a {@code Completable} which delays the emission of the completion event by the given time while
     * running on the specified {@link Scheduler}.
     * <p>
     * <img width="640" height="313" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.delay.s.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code delay} operates on the {@code Scheduler} you specify.</dd>
     * </dl>
     * @param time the delay time
     * @param unit the delay unit
     * @param scheduler the {@code Scheduler} to run the delayed completion on
     * @return the new {@code Completable} instance
     * @throws NullPointerException if {@code unit} or {@code scheduler} is {@code null}
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    @NonNull
    public final Completable delay(long time, @NonNull TimeUnit unit, @NonNull Scheduler scheduler) {
        return delay(time, unit, scheduler, false);
    }

    /**
     * Returns a {@code Completable} which delays the emission of the completion event, and optionally the error as well, by the given time while
     * running on the specified {@link Scheduler}.
     * <p>
     * <img width="640" height="253" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.delay.sb.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code delay} operates on the {@code Scheduler} you specify.</dd>
     * </dl>
     * @param time the delay time
     * @param unit the delay unit
     * @param scheduler the {@code Scheduler} to run the delayed completion on
     * @param delayError delay the error emission as well?
     * @return the new {@code Completable} instance
     * @throws NullPointerException if {@code unit} or {@code scheduler} is {@code null}
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    public final Completable delay(long time, @NonNull TimeUnit unit, @NonNull Scheduler scheduler, boolean delayError) {
        Objects.requireNonNull(unit, "unit is null");
        Objects.requireNonNull(scheduler, "scheduler is null");
        return RxJavaPlugins.onAssembly(new CompletableDelay(this, time, unit, scheduler, delayError));
    }

    /**
     * Returns a {@code Completable} that delays the subscription to the upstream by a given amount of time.
     * <p>
     * <img width="640" height="475" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.delaySubscription.t.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This version of {@code delaySubscription} operates by default on the {@code computation} {@link Scheduler}.</dd>
     * </dl>
     * <p>History: 2.2.3 - experimental
     *
     * @param time the time to delay the subscription
     * @param unit  the time unit of {@code delay}
     * @return the new {@code Completable} instance
     * @throws NullPointerException if {@code unit} is {@code null}
     * @since 3.0.0
     * @see <a href="http://reactivex.io/documentation/operators/delay.html">ReactiveX operators documentation: Delay</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.COMPUTATION)
    @NonNull
    public final Completable delaySubscription(long time, @NonNull TimeUnit unit) {
        return delaySubscription(time, unit, Schedulers.computation());
    }

    /**
     * Returns a {@code Completable} that delays the subscription to the upstream by a given amount of time,
     * both waiting and subscribing on a given {@link Scheduler}.
     * <p>
     * <img width="640" height="420" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.delaySubscription.ts.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>You specify which {@code Scheduler} this operator will use.</dd>
     * </dl>
     * <p>History: 2.2.3 - experimental
     * @param time     the time to delay the subscription
     * @param unit      the time unit of {@code delay}
     * @param scheduler the {@code Scheduler} on which the waiting and subscription will happen
     * @return the new {@code Completable} instance
     * @throws NullPointerException if {@code unit} or {@code scheduler} is {@code null}
     * @since 3.0.0
     * @see <a href="http://reactivex.io/documentation/operators/delay.html">ReactiveX operators documentation: Delay</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    @NonNull
    public final Completable delaySubscription(long time, @NonNull TimeUnit unit, @NonNull Scheduler scheduler) {
        return Completable.timer(time, unit, scheduler).andThen(this);
    }

    /**
     * Returns a {@code Completable} which calls the given {@code onComplete} {@link Action} if this {@code Completable} completes.
     * <p>
     * <img width="640" height="304" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.doOnComplete.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code doOnComplete} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param onComplete the {@code Action} to call when this emits an {@code onComplete} event
     * @return the new {@code Completable} instance
     * @throws NullPointerException if {@code onComplete} is {@code null}
     * @see #doFinally(Action)
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Completable doOnComplete(@NonNull Action onComplete) {
        return doOnLifecycle(Functions.emptyConsumer(), Functions.emptyConsumer(),
                onComplete, Functions.EMPTY_ACTION,
                Functions.EMPTY_ACTION, Functions.EMPTY_ACTION);
    }

    /**
     * Calls the shared {@link Action} if a {@link CompletableObserver} subscribed to the current
     * {@code Completable} disposes the common {@link Disposable} it received via {@code onSubscribe}.
     * <p>
     * <img width="640" height="589" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.doOnDispose.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code doOnDispose} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param onDispose the {@code Action} to call when the downstream observer disposes the subscription
     * @return the new {@code Completable} instance
     * @throws NullPointerException if {@code onDispose} is {@code null}
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Completable doOnDispose(@NonNull Action onDispose) {
        return doOnLifecycle(Functions.emptyConsumer(), Functions.emptyConsumer(),
                Functions.EMPTY_ACTION, Functions.EMPTY_ACTION,
                Functions.EMPTY_ACTION, onDispose);
    }

    /**
     * Returns a {@code Completable} which calls the given {@code onError} {@link Consumer} if this {@code Completable} emits an error.
     * <p>
     * <img width="640" height="304" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.doOnError.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code doOnError} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param onError the error {@code Consumer} receiving the upstream {@link Throwable} if the upstream signals it via {@code onError}
     * @return the new {@code Completable} instance
     * @throws NullPointerException if {@code onError} is {@code null}
     * @see #doFinally(Action)
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Completable doOnError(@NonNull Consumer<? super Throwable> onError) {
        return doOnLifecycle(Functions.emptyConsumer(), onError,
                Functions.EMPTY_ACTION, Functions.EMPTY_ACTION,
                Functions.EMPTY_ACTION, Functions.EMPTY_ACTION);
    }

    /**
     * Returns a {@code Completable} which calls the given {@code onEvent} {@link Consumer} with the {@link Throwable} for an {@code onError}
     * or {@code null} for an {@code onComplete} signal from this {@code Completable} before delivering the signal to the downstream.
     * <p>
     * <img width="640" height="305" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.doOnEvent.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code doOnEvent} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param onEvent the event {@code Consumer} that receives {@code null} for upstream
     *                completion or a {@code Throwable} if the upstream signaled an error
     * @return the new {@code Completable} instance
     * @throws NullPointerException if {@code onEvent} is {@code null}
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Completable doOnEvent(@NonNull Consumer<@Nullable ? super Throwable> onEvent) {
        Objects.requireNonNull(onEvent, "onEvent is null");
        return RxJavaPlugins.onAssembly(new CompletableDoOnEvent(this, onEvent));
    }

    /**
     * Calls the appropriate {@code onXXX} method (shared between all {@link CompletableObserver}s) for the lifecycle events of
     * the sequence (subscription, disposal).
     * <p>
     * <img width="640" height="257" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.doOnLifecycle.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code doOnLifecycle} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param onSubscribe
     *              a {@link Consumer} called with the {@link Disposable} sent via {@link CompletableObserver#onSubscribe(Disposable)}
     * @param onDispose
     *              called when the downstream disposes the {@code Disposable} via {@code dispose()}
     * @return the new {@code Completable} instance
     * @throws NullPointerException if {@code onSubscribe} or {@code onDispose} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/do.html">ReactiveX operators documentation: Do</a>
     * @since 3.0.0
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Completable doOnLifecycle(@NonNull Consumer<? super Disposable> onSubscribe, @NonNull Action onDispose) {
        return doOnLifecycle(onSubscribe, Functions.emptyConsumer(),
                Functions.EMPTY_ACTION, Functions.EMPTY_ACTION,
                Functions.EMPTY_ACTION, onDispose);
    }

    /**
     * Returns a {@code Completable} instance that calls the various callbacks upon the specific
     * lifecycle events.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code doOnLifecycle} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param onSubscribe the {@link Consumer} called when a {@link CompletableObserver} subscribes.
     * @param onError the {@code Consumer} called when this emits an {@code onError} event
     * @param onComplete the {@link Action} called just before when the current {@code Completable} completes normally
     * @param onTerminate the {@code Action} called just before this {@code Completable} terminates
     * @param onAfterTerminate the {@code Action} called after this {@code Completable} completes normally
     * @param onDispose the {@code Action} called when the downstream disposes the subscription
     * @return the new {@code Completable} instance
     * @throws NullPointerException if {@code onSubscribe}, {@code onError}, {@code onComplete}
     * {@code onTerminate}, {@code onAfterTerminate} or {@code onDispose} is {@code null}
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    private Completable doOnLifecycle(
            final Consumer<? super Disposable> onSubscribe,
            final Consumer<? super Throwable> onError,
            final Action onComplete,
            final Action onTerminate,
            final Action onAfterTerminate,
            final Action onDispose) {
        Objects.requireNonNull(onSubscribe, "onSubscribe is null");
        Objects.requireNonNull(onError, "onError is null");
        Objects.requireNonNull(onComplete, "onComplete is null");
        Objects.requireNonNull(onTerminate, "onTerminate is null");
        Objects.requireNonNull(onAfterTerminate, "onAfterTerminate is null");
        Objects.requireNonNull(onDispose, "onDispose is null");
        return RxJavaPlugins.onAssembly(new CompletablePeek(this, onSubscribe, onError, onComplete, onTerminate, onAfterTerminate, onDispose));
    }

    /**
     * Returns a {@code Completable} instance that calls the given {@code onSubscribe} callback with the disposable
     * that the downstream {@link CompletableObserver}s receive upon subscription.
     * <p>
     * <img width="640" height="304" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.doOnSubscribe.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code doOnSubscribe} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param onSubscribe the {@link Consumer} called when a downstream {@code CompletableObserver} subscribes
     * @return the new {@code Completable} instance
     * @throws NullPointerException if {@code onSubscribe} is {@code null}
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Completable doOnSubscribe(@NonNull Consumer<? super Disposable> onSubscribe) {
        return doOnLifecycle(onSubscribe, Functions.emptyConsumer(),
                Functions.EMPTY_ACTION, Functions.EMPTY_ACTION,
                Functions.EMPTY_ACTION, Functions.EMPTY_ACTION);
    }

    /**
     * Returns a {@code Completable} instance that calls the given {@code onTerminate} {@link Action} just before this {@code Completable}
     * completes normally or with an exception.
     * <p>
     * <img width="640" height="305" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.doOnTerminate.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code doOnTerminate} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param onTerminate the {@code Action} to call just before this {@code Completable} terminates
     * @return the new {@code Completable} instance
     * @throws NullPointerException if {@code onTerminate} is {@code null}
     * @see #doFinally(Action)
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Completable doOnTerminate(@NonNull Action onTerminate) {
        return doOnLifecycle(Functions.emptyConsumer(), Functions.emptyConsumer(),
                Functions.EMPTY_ACTION, onTerminate,
                Functions.EMPTY_ACTION, Functions.EMPTY_ACTION);
    }

    /**
     * Returns a {@code Completable} instance that calls the given {@code onAfterTerminate}  {@link Action} after this {@code Completable}
     * completes normally or with an exception.
     * <p>
     * <img width="640" height="305" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.doAfterTerminate.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code doAfterTerminate} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param onAfterTerminate the {@code Action} to call after this {@code Completable} terminates
     * @return the new {@code Completable} instance
     * @throws NullPointerException if {@code onAfterTerminate} is {@code null}
     * @see #doFinally(Action)
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Completable doAfterTerminate(@NonNull Action onAfterTerminate) {
        return doOnLifecycle(
                Functions.emptyConsumer(),
                Functions.emptyConsumer(),
                Functions.EMPTY_ACTION,
                Functions.EMPTY_ACTION,
                onAfterTerminate,
                Functions.EMPTY_ACTION);
    }
    /**
     * Calls the specified {@link Action} after this {@code Completable} signals {@code onError} or {@code onComplete} or gets disposed by
     * the downstream.
     * <p>
     * <img width="640" height="331" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.doFinally.png" alt="">
     * <p>
     * In case of a race between a terminal event and a dispose call, the provided {@code onFinally} action
     * is executed once per subscription.
     * <p>
     * Note that the {@code onFinally} action is shared between subscriptions and as such
     * should be thread-safe.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code doFinally} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * <p>History: 2.0.1 - experimental
     * @param onFinally the {@code Action} called when this {@code Completable} terminates or gets disposed
     * @return the new {@code Completable} instance
     * @throws NullPointerException if {@code onFinally} is {@code null}
     * @since 2.1
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Completable doFinally(@NonNull Action onFinally) {
        Objects.requireNonNull(onFinally, "onFinally is null");
        return RxJavaPlugins.onAssembly(new CompletableDoFinally(this, onFinally));
    }

    /**
     * <strong>This method requires advanced knowledge about building operators, please consider
     * other standard composition methods first;</strong>
     * Returns a {@code Completable} which, when subscribed to, invokes the {@link CompletableOperator#apply(CompletableObserver) apply(CompletableObserver)} method
     * of the provided {@link CompletableOperator} for each individual downstream {@link Completable} and allows the
     * insertion of a custom operator by accessing the downstream's {@link CompletableObserver} during this subscription phase
     * and providing a new {@code CompletableObserver}, containing the custom operator's intended business logic, that will be
     * used in the subscription process going further upstream.
     * <p>
     * <img width="640" height="313" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.lift.png" alt="">
     * <p>
     * Generally, such a new {@code CompletableObserver} will wrap the downstream's {@code CompletableObserver} and forwards the
     * {@code onError} and {@code onComplete} events from the upstream directly or according to the
     * emission pattern the custom operator's business logic requires. In addition, such operator can intercept the
     * flow control calls of {@code dispose} and {@code isDisposed} that would have traveled upstream and perform
     * additional actions depending on the same business logic requirements.
     * <p>
     * Example:
     * <pre><code>
     * // Step 1: Create the consumer type that will be returned by the CompletableOperator.apply():
     * 
     * public final class CustomCompletableObserver implements CompletableObserver, Disposable {
     *
     *     // The downstream's CompletableObserver that will receive the onXXX events
     *     final CompletableObserver downstream;
     *
     *     // The connection to the upstream source that will call this class' onXXX methods
     *     Disposable upstream;
     *
     *     // The constructor takes the downstream subscriber and usually any other parameters
     *     public CustomCompletableObserver(CompletableObserver downstream) {
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
     *     // Some operators may handle the upstream's error while others
     *     // could just forward it to the downstream.
     *     &#64;Override
     *     public void onError(Throwable throwable) {
     *         downstream.onError(throwable);
     *     }
     *
     *     // When the upstream completes, usually the downstream should complete as well.
     *     // In completable, this could also mean doing some side-effects
     *     &#64;Override
     *     public void onComplete() {
     *         System.out.println("Sequence completed");
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
     * // Step 2: Create a class that implements the CompletableOperator interface and
     * //         returns the custom consumer type from above in its apply() method.
     * //         Such class may define additional parameters to be submitted to
     * //         the custom consumer type.
     *
     * final class CustomCompletableOperator implements CompletableOperator {
     *     &#64;Override
     *     public CompletableObserver apply(CompletableObserver upstream) {
     *         return new CustomCompletableObserver(upstream);
     *     }
     * }
     *
     * // Step 3: Apply the custom operator via lift() in a flow by creating an instance of it
     * //         or reusing an existing one.
     *
     * Completable.complete()
     * .lift(new CustomCompletableOperator())
     * .test()
     * .assertResult();
     * </code></pre>
     * <p>
     * Creating custom operators can be complicated and it is recommended one consults the
     * <a href="https://github.com/ReactiveX/RxJava/wiki/Writing-operators-for-2.0">RxJava wiki: Writing operators</a> page about
     * the tools, requirements, rules, considerations and pitfalls of implementing them.
     * <p>
     * Note that implementing custom operators via this {@code lift()} method adds slightly more overhead by requiring
     * an additional allocation and indirection per assembled flows. Instead, extending the abstract {@code Completable}
     * class and creating a {@link CompletableTransformer} with it is recommended.
     * <p>
     * Note also that it is not possible to stop the subscription phase in {@code lift()} as the {@code apply()} method
     * requires a non-{@code null} {@code CompletableObserver} instance to be returned, which is then unconditionally subscribed to
     * the current {@code Completable}. For example, if the operator decided there is no reason to subscribe to the
     * upstream source because of some optimization possibility or a failure to prepare the operator, it still has to
     * return a {@code CompletableObserver} that should immediately dispose the upstream's {@link Disposable} in its
     * {@code onSubscribe} method. Again, using a {@code CompletableTransformer} and extending the {@code Completable} is
     * a better option as {@link #subscribeActual} can decide to not subscribe to its upstream after all.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code lift} does not operate by default on a particular {@link Scheduler}, however, the
     *  {@code CompletableOperator} may use a {@code Scheduler} to support its own asynchronous behavior.</dd>
     * </dl>
     *
     * @param onLift the {@code CompletableOperator} that receives the downstream's {@code CompletableObserver} and should return
     *               a {@code CompletableObserver} with custom behavior to be used as the consumer for the current
     *               {@code Completable}.
     * @return the new {@code Completable} instance
     * @throws NullPointerException if {@code onLift} is {@code null}
     * @see <a href="https://github.com/ReactiveX/RxJava/wiki/Writing-operators-for-2.0">RxJava wiki: Writing operators</a>
     * @see #compose(CompletableTransformer)
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Completable lift(@NonNull CompletableOperator onLift) {
        Objects.requireNonNull(onLift, "onLift is null");
        return RxJavaPlugins.onAssembly(new CompletableLift(this, onLift));
    }

    /**
     * Maps the signal types of this {@code Completable} into a {@link Notification} of the same kind
     * and emits it as a single success value to downstream.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/materialize.v3.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code materialize} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * <p>History: 2.2.4 - experimental
     * @param <T> the intended target element type of the {@code Notification}
     * @return the new {@link Single} instance
     * @since 3.0.0
     * @see Single#dematerialize(Function)
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <@NonNull T> Single<Notification<T>> materialize() {
        return RxJavaPlugins.onAssembly(new CompletableMaterialize<>(this));
    }

    /**
     * Returns a {@code Completable} which subscribes to this and the other {@link CompletableSource} and completes
     * when both of them complete or one emits an error.
     * <p>
     * <img width="640" height="442" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.mergeWith.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code mergeWith} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param other the other {@code CompletableSource} instance
     * @return the new {@code Completable} instance
     * @throws NullPointerException if {@code other} is {@code null}
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Completable mergeWith(@NonNull CompletableSource other) {
        Objects.requireNonNull(other, "other is null");
        return mergeArray(this, other);
    }

    /**
     * Returns a {@code Completable} which emits the terminal events from the thread of the specified {@link Scheduler}.
     * <p>
     * <img width="640" height="523" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.observeOn.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code observeOn} operates on a {@code Scheduler} you specify.</dd>
     * </dl>
     * @param scheduler the {@code Scheduler} to emit terminal events on
     * @return the new {@code Completable} instance
     * @throws NullPointerException if {@code scheduler} is {@code null}
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    public final Completable observeOn(@NonNull Scheduler scheduler) {
        Objects.requireNonNull(scheduler, "scheduler is null");
        return RxJavaPlugins.onAssembly(new CompletableObserveOn(this, scheduler));
    }

    /**
     * Returns a {@code Completable} instance that if this {@code Completable} emits an error, it will emit an {@code onComplete}
     * and swallow the upstream {@link Throwable}.
     * <p>
     * <img width="640" height="585" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.onErrorComplete.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code onErrorComplete} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @return the new {@code Completable} instance
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Completable onErrorComplete() {
        return onErrorComplete(Functions.alwaysTrue());
    }

    /**
     * Returns a {@code Completable} instance that if this {@code Completable} emits an error and the {@link Predicate} returns
     * {@code true}, it will emit an {@code onComplete} and swallow the {@link Throwable}.
     * <p>
     * <img width="640" height="283" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.onErrorComplete.f.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code onErrorComplete} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param predicate the {@code Predicate} to call when a {@code Throwable} is emitted which should return {@code true}
     * if the {@code Throwable} should be swallowed and replaced with an {@code onComplete}.
     * @return the new {@code Completable} instance
     * @throws NullPointerException if {@code predicate} is {@code null}
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Completable onErrorComplete(@NonNull Predicate<? super Throwable> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");

        return RxJavaPlugins.onAssembly(new CompletableOnErrorComplete(this, predicate));
    }

    /**
     * Returns a {@code Completable} instance that when encounters an error from this {@code Completable}, calls the
     * specified {@code mapper} {@link Function} that returns a {@link CompletableSource} instance for it and resumes the
     * execution with it.
     * <p>
     * <img width="640" height="426" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.onErrorResumeNext.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code onErrorResumeNext} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param fallbackSupplier the {@code mapper} {@code Function} that takes the error and should return a {@code CompletableSource} as
     * continuation.
     * @return the new {@code Completable} instance
     * @throws NullPointerException if {@code fallbackSupplier} is {@code null}
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Completable onErrorResumeNext(@NonNull Function<? super Throwable, ? extends CompletableSource> fallbackSupplier) {
        Objects.requireNonNull(fallbackSupplier, "fallbackSupplier is null");
        return RxJavaPlugins.onAssembly(new CompletableResumeNext(this, fallbackSupplier));
    }
    /**
     * Resumes the flow with the given {@link CompletableSource} when the current {@code Completable} fails instead of
     * signaling the error via {@code onError}.
     * <p>
     * <img width="640" height="409" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.onErrorResumeWith.png" alt="">
     * <p>
     * You can use this to prevent errors from propagating or to supply fallback data should errors be
     * encountered.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code onErrorResumeWith} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param fallback
     *            the next {@code CompletableSource} that will take over if the current {@code Completable} encounters
     *            an error
     * @return the new {@code Completable} instance
     * @throws NullPointerException if {@code fallback} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/catch.html">ReactiveX operators documentation: Catch</a>
     * @since 3.0.0
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Completable onErrorResumeWith(@NonNull CompletableSource fallback) {
        Objects.requireNonNull(fallback, "fallback is null");
        return onErrorResumeNext(Functions.justFunction(fallback));
    }

    /**
     * Ends the flow with a success item returned by a function for the {@link Throwable} error signaled by the current
     * {@code Completable} instead of signaling the error via {@code onError}.
     * <p>
     * <img width="640" height="567" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.onErrorReturn.png" alt="">
     * <p>
     * You can use this to prevent errors from propagating or to supply fallback data should errors be
     * encountered.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code onErrorReturn} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T> the item type to return on error
     * @param itemSupplier
     *            a function that returns a single value that will be emitted as success value
     *            the current {@code Completable} signals an {@code onError} event
     * @return the new {@link Maybe} instance
     * @throws NullPointerException if {@code itemSupplier} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/catch.html">ReactiveX operators documentation: Catch</a>
     * @since 3.0.0
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public final <@NonNull T> Maybe<T> onErrorReturn(@NonNull Function<? super Throwable, ? extends T> itemSupplier) {
        Objects.requireNonNull(itemSupplier, "itemSupplier is null");
        return RxJavaPlugins.onAssembly(new CompletableOnErrorReturn<>(this, itemSupplier));
    }

    /**
     * Ends the flow with the given success item when the current {@code Completable}
     * fails instead of signaling the error via {@code onError}.
     * <p>
     * <img width="640" height="567" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.onErrorReturnItem.png" alt="">
     * <p>
     * You can use this to prevent errors from propagating or to supply fallback data should errors be
     * encountered.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code onErrorReturnItem} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T> the item type to return on error
     * @param item
     *            the value that is emitted as {@code onSuccess} in case the current {@code Completable} signals an {@code onError}
     * @return the new {@link Maybe} instance
     * @throws NullPointerException if {@code item} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/catch.html">ReactiveX operators documentation: Catch</a>
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public final <@NonNull T> Maybe<T> onErrorReturnItem(@NonNull T item) {
        Objects.requireNonNull(item, "item is null");
        return onErrorReturn(Functions.justFunction(item));
    }

    /**
     * Nulls out references to the upstream producer and downstream {@link CompletableObserver} if
     * the sequence is terminated or downstream calls {@code dispose()}.
     * <p>
     * <img width="640" height="326" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.onTerminateDetach.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code onTerminateDetach} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * <p>History: 2.1.5 - experimental
     * @return the new {@code Completable} instance
     * @since 2.2
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Completable onTerminateDetach() {
        return RxJavaPlugins.onAssembly(new CompletableDetach(this));
    }

    /**
     * Returns a {@code Completable} that repeatedly subscribes to this {@code Completable} until disposed.
     * <p>
     * <img width="640" height="373" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.repeat.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code repeat} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @return the new {@code Completable} instance
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Completable repeat() {
        return fromPublisher(toFlowable().repeat());
    }

    /**
     * Returns a {@code Completable} that subscribes repeatedly at most the given number of times to this {@code Completable}.
     * <p>
     * <img width="640" height="408" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.repeat.n.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code repeat} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param times the number of times the re-subscription should happen
     * @return the new {@code Completable} instance
     * @throws IllegalArgumentException if {@code times} is negative
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Completable repeat(long times) {
        return fromPublisher(toFlowable().repeat(times));
    }

    /**
     * Returns a {@code Completable} that repeatedly subscribes to this {@code Completable} so long as the given
     * stop {@link BooleanSupplier} returns {@code false}.
     * <p>
     * <img width="640" height="381" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.repeatUntil.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code repeatUntil} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param stop the {@code BooleanSupplier} that should return {@code true} to stop resubscribing.
     * @return the new {@code Completable} instance
     * @throws NullPointerException if {@code stop} is {@code null}
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Completable repeatUntil(@NonNull BooleanSupplier stop) {
        return fromPublisher(toFlowable().repeatUntil(stop));
    }

    /**
     * Returns a {@code Completable} instance that repeats when the {@link Publisher} returned by the handler {@link Function}
     * emits an item or completes when this {@code Publisher} emits an {@code onComplete} event.
     * <p>
     * <img width="640" height="586" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.repeatWhen.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code repeatWhen} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param handler the {@code Function} that transforms the stream of values indicating the completion of
     * this {@code Completable} and returns a {@code Publisher} that emits items for repeating or completes to indicate the
     * repetition should stop
     * @return the new {@code Completable} instance
     * @throws NullPointerException if {@code handler} is {@code null}
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Completable repeatWhen(@NonNull Function<? super Flowable<Object>, @NonNull ? extends Publisher<@NonNull ?>> handler) {
        return fromPublisher(toFlowable().repeatWhen(handler));
    }

    /**
     * Returns a {@code Completable} that retries this {@code Completable} as long as it emits an {@code onError} event.
     * <p>
     * <img width="640" height="368" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.retry.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code retry} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @return the new {@code Completable} instance
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Completable retry() {
        return fromPublisher(toFlowable().retry());
    }

    /**
     * Returns a {@code Completable} that retries this {@code Completable} in case of an error as long as the {@code predicate}
     * returns {@code true}.
     * <p>
     * <img width="640" height="325" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.retry.ff.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code retry} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param predicate the {@link Predicate} called when this {@code Completable} emits an error with the repeat count and the latest {@link Throwable}
     * and should return {@code true} to retry.
     * @return the new {@code Completable} instance
     * @throws NullPointerException if {@code predicate} is {@code null}
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Completable retry(@NonNull BiPredicate<? super Integer, ? super Throwable> predicate) {
        return fromPublisher(toFlowable().retry(predicate));
    }

    /**
     * Returns a {@code Completable} that when this {@code Completable} emits an error, retries at most the given
     * number of times before giving up and emitting the last error.
     * <p>
     * <img width="640" height="451" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.retry.n.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code retry} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param times the number of times to resubscribe if the current {@code Completable} fails
     * @return the new {@code Completable} instance
     * @throws IllegalArgumentException if {@code times} is negative
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Completable retry(long times) {
        return fromPublisher(toFlowable().retry(times));
    }

    /**
     * Returns a {@code Completable} that when this {@code Completable} emits an error, retries at most times
     * or until the predicate returns {@code false}, whichever happens first and emitting the last error.
     * <p>
     * <img width="640" height="361" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.retry.nf.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code retry} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * <p>History: 2.1.8 - experimental
     * @param times the number of times to resubscribe if the current {@code Completable} fails
     * @param predicate the {@link Predicate} that is called with the latest {@link Throwable} and should return
     * {@code true} to indicate the returned {@code Completable} should resubscribe to this {@code Completable}.
     * @return the new {@code Completable} instance
     * @throws NullPointerException if {@code predicate} is {@code null}
     * @throws IllegalArgumentException if {@code times} is negative
     * @since 2.2
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Completable retry(long times, @NonNull Predicate<? super Throwable> predicate) {
        return fromPublisher(toFlowable().retry(times, predicate));
    }

    /**
     * Returns a {@code Completable} that when this {@code Completable} emits an error, calls the given predicate with
     * the latest {@link Throwable} to decide whether to resubscribe to the upstream or not.
     * <p>
     * <img width="640" height="336" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.retry.f.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code retry} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param predicate the {@link Predicate} that is called with the latest {@code Throwable} and should return
     * {@code true} to indicate the returned {@code Completable} should resubscribe to this {@code Completable}.
     * @return the new {@code Completable} instance
     * @throws NullPointerException if {@code predicate} is {@code null}
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Completable retry(@NonNull Predicate<? super Throwable> predicate) {
        return fromPublisher(toFlowable().retry(predicate));
    }

    /**
     * Retries until the given stop function returns {@code true}.
     * <p>
     * <img width="640" height="354" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.retryUntil.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code retryUntil} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param stop the function that should return {@code true} to stop retrying
     * @return the new {@code Completable} instance
     * @throws NullPointerException if {@code stop} is {@code null}
     * @since 3.0.0
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Completable retryUntil(@NonNull BooleanSupplier stop) {
        Objects.requireNonNull(stop, "stop is null");
        return retry(Long.MAX_VALUE, Functions.predicateReverseFor(stop));
    }

    /**
     * Returns a {@code Completable} which given a {@link Publisher} and when this {@code Completable} emits an error, delivers
     * that error through a {@link Flowable} and the {@code Publisher} should signal a value indicating a retry in response
     * or a terminal event indicating a termination.
     * <p>
     * <img width="640" height="586" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.retryWhen.png" alt="">
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
     * Completable.timer(1, TimeUnit.SECONDS)
     *     .doOnSubscribe(s -&gt; System.out.println("subscribing"))
     *     .doOnComplete(() -&gt; { throw new RuntimeException(); })
     *     .retryWhen(errors -&gt; {
     *         AtomicInteger counter = new AtomicInteger();
     *         return errors
     *                   .takeWhile(e -&gt; counter.getAndIncrement() != 3)
     *                   .flatMap(e -&gt; {
     *                       System.out.println("delay retry by " + counter.get() + " second(s)");
     *                       return Flowable.timer(counter.get(), TimeUnit.SECONDS);
     *                   });
     *     })
     *     .blockingAwait();
     * </code></pre>
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code retryWhen} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param handler the {@link Function} that receives a {@code Flowable} delivering {@code Throwable}s and should return a {@code Publisher} that
     * emits items to indicate retries or emits terminal events to indicate termination.
     * @return the new {@code Completable} instance
     * @throws NullPointerException if {@code handler} is {@code null}
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Completable retryWhen(@NonNull Function<? super Flowable<Throwable>, @NonNull ? extends Publisher<@NonNull ?>> handler) {
        return fromPublisher(toFlowable().retryWhen(handler));
    }

    /**
     * Wraps the given {@link CompletableObserver}, catches any {@link RuntimeException}s thrown by its
     * {@link CompletableObserver#onSubscribe(Disposable)}, {@link CompletableObserver#onError(Throwable)}
     * or {@link CompletableObserver#onComplete()} methods and routes those to the global
     * error handler via {@link RxJavaPlugins#onError(Throwable)}.
     * <p>
     * By default, the {@code Completable} protocol forbids the {@code onXXX} methods to throw, but some
     * {@code CompletableObserver} implementation may do it anyway, causing undefined behavior in the
     * upstream. This method and the underlying safe wrapper ensures such misbehaving consumers don't
     * disrupt the protocol.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code safeSubscribe} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param observer the potentially misbehaving {@code CompletableObserver}
     * @throws NullPointerException if {@code observer} is {@code null}
     * @see #subscribe(Action, Consumer)
     * @since 3.0.0
     */
    @SchedulerSupport(SchedulerSupport.NONE)
    public final void safeSubscribe(@NonNull CompletableObserver observer) {
        Objects.requireNonNull(observer, "observer is null");
        subscribe(new SafeCompletableObserver(observer));
    }

    /**
     * Returns a {@code Completable} which first runs the other {@link CompletableSource}
     * then the current {@code Completable} if the other completed normally.
     * <p>
     * <img width="640" height="437" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.startWith.c.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code startWith} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param other the other {@code CompletableSource} to run first
     * @return the new {@code Completable} instance
     * @throws NullPointerException if {@code other} is {@code null}
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Completable startWith(@NonNull CompletableSource other) {
        Objects.requireNonNull(other, "other is null");
        return concatArray(other, this);
    }

    /**
     * Returns a {@link Flowable} which first runs the other {@link SingleSource}
     * then the current {@code Completable} if the other succeeded normally.
     * <p>
     * <img width="640" height="388" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.startWith.s.png" alt="">
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The returned {@code Flowable} honors the backpressure of the downstream consumer.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code startWith} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the element type of the {@code other} {@code SingleSource}.
     * @param other the other {@code SingleSource} to run first
     * @return the new {@code Flowable} instance
     * @throws NullPointerException if {@code other} is {@code null}
     * @since 3.0.0
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    @BackpressureSupport(BackpressureKind.FULL)
    public final <@NonNull T> Flowable<T> startWith(@NonNull SingleSource<T> other) {
        Objects.requireNonNull(other, "other is null");
        return Flowable.concat(Single.wrap(other).toFlowable(), toFlowable());
    }

    /**
     * Returns a {@link Flowable} which first runs the other {@link MaybeSource}
     * then the current {@code Completable} if the other succeeded or completed normally.
     * <p>
     * <img width="640" height="266" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.startWith.m.png" alt="">
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The returned {@code Flowable} honors the backpressure of the downstream consumer.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code startWith} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the element type of the {@code other} {@code MaybeSource}.
     * @param other the other {@code MaybeSource} to run first
     * @return the new {@code Flowable} instance
     * @throws NullPointerException if {@code other} is {@code null}
     * @since 3.0.0
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    @BackpressureSupport(BackpressureKind.FULL)
    public final <@NonNull T> Flowable<T> startWith(@NonNull MaybeSource<T> other) {
        Objects.requireNonNull(other, "other is null");
        return Flowable.concat(Maybe.wrap(other).toFlowable(), toFlowable());
    }

    /**
     * Returns an {@link Observable} which first delivers the events
     * of the other {@link ObservableSource} then runs the current {@code Completable}.
     * <p>
     * <img width="640" height="289" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.startWith.o.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code startWith} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the value type
     * @param other the other {@code ObservableSource} to run first
     * @return the new {@code Observable} instance
     * @throws NullPointerException if {@code other} is {@code null}
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public final <@NonNull T> Observable<T> startWith(@NonNull ObservableSource<T> other) {
        Objects.requireNonNull(other, "other is null");
        return Observable.wrap(other).concatWith(this.toObservable());
    }

    /**
     * Returns a {@link Flowable} which first delivers the events
     * of the other {@link Publisher} then runs the current {@code Completable}.
     * <p>
     * <img width="640" height="250" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.startWith.p.png" alt="">
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The returned {@code Flowable} honors the backpressure of the downstream consumer
     *  and expects the other {@code Publisher} to honor it as well.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code startWith} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the value type
     * @param other the other {@code Publisher} to run first
     * @return the new {@code Flowable} instance
     * @throws NullPointerException if {@code other} is {@code null}
     */
    @CheckReturnValue
    @NonNull
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerSupport.NONE)
    public final <@NonNull T> Flowable<T> startWith(@NonNull Publisher<T> other) {
        Objects.requireNonNull(other, "other is null");
        return this.<T>toFlowable().startWith(other);
    }

    /**
     * Hides the identity of this {@code Completable} and its {@link Disposable}.
     * <p>
     * <img width="640" height="432" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.hide.png" alt="">
     * <p>
     * Allows preventing certain identity-based optimizations (fusion).
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code hide} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * <p>History: 2.0.5 - experimental
     * @return the new {@code Completable} instance
     * @since 2.1
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Completable hide() {
        return RxJavaPlugins.onAssembly(new CompletableHide(this));
    }

    /**
     * Subscribes to this {@code Completable} and returns a {@link Disposable} which can be used to dispose
     * the subscription.
     * <p>
     * <img width="640" height="352" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.subscribe.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code subscribe} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @return the new {@code Disposable} that can be used for disposing the subscription at any time
     * @see #subscribe(Action, Consumer, DisposableContainer)
     */
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Disposable subscribe() {
        EmptyCompletableObserver observer = new EmptyCompletableObserver();
        subscribe(observer);
        return observer;
    }

    @SchedulerSupport(SchedulerSupport.NONE)
    @Override
    public final void subscribe(@NonNull CompletableObserver observer) {
        Objects.requireNonNull(observer, "observer is null");
        try {

            observer = RxJavaPlugins.onSubscribe(this, observer);

            Objects.requireNonNull(observer, "The RxJavaPlugins.onSubscribe hook returned a null CompletableObserver. Please check the handler provided to RxJavaPlugins.setOnCompletableSubscribe for invalid null returns. Further reading: https://github.com/ReactiveX/RxJava/wiki/Plugins");

            subscribeActual(observer);
        } catch (NullPointerException ex) { // NOPMD
            throw ex;
        } catch (Throwable ex) {
            Exceptions.throwIfFatal(ex);
            RxJavaPlugins.onError(ex);
            throw toNpe(ex);
        }
    }

    /**
     * Implement this method to handle the incoming {@link CompletableObserver}s and
     * perform the business logic in your operator.
     * <p>There is no need to call any of the plugin hooks on the current {@code Completable} instance or
     * the {@code CompletableObserver}; all hooks and basic safeguards have been
     * applied by {@link #subscribe(CompletableObserver)} before this method gets called.
     * @param observer the {@code CompletableObserver} instance, never {@code null}
     */
    protected abstract void subscribeActual(@NonNull CompletableObserver observer);

    /**
     * Subscribes a given {@link CompletableObserver} (subclass) to this {@code Completable} and returns the given
     * {@code CompletableObserver} as is.
     * <p>
     * <img width="640" height="349" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.subscribeWith.png" alt="">
     * <p>Usage example:
     * <pre><code>
     * Completable source = Completable.complete().delay(1, TimeUnit.SECONDS);
     * CompositeDisposable composite = new CompositeDisposable();
     *
     * DisposableCompletableObserver ds = new DisposableCompletableObserver() {
     *     // ...
     * };
     *
     * composite.add(source.subscribeWith(ds));
     * </code></pre>
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code subscribeWith} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <E> the type of the {@code CompletableObserver} to use and return
     * @param observer the {@code CompletableObserver} (subclass) to use and return, not {@code null}
     * @return the input {@code observer}
     * @throws NullPointerException if {@code observer} is {@code null}
     * @since 2.0
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <@NonNull E extends CompletableObserver> E subscribeWith(E observer) {
        subscribe(observer);
        return observer;
    }

    /**
     * Subscribes to this {@code Completable} and calls back either the {@code onError} or {@code onComplete} functions.
     * <p>
     * <img width="640" height="352" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.subscribe.ff.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code subscribe} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param onComplete the {@link Action} that is called if the {@code Completable} completes normally
     * @param onError the {@link Consumer} that is called if this {@code Completable} emits an error
     * @return the new {@link Disposable} that can be used for disposing the subscription at any time
     * @throws NullPointerException if {@code onComplete} or {@code onError} is {@code null}
     * @see #subscribe(Action, Consumer, DisposableContainer)
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Disposable subscribe(@NonNull Action onComplete, @NonNull Consumer<? super Throwable> onError) {
        Objects.requireNonNull(onError, "onError is null");
        Objects.requireNonNull(onComplete, "onComplete is null");

        CallbackCompletableObserver observer = new CallbackCompletableObserver(onError, onComplete);
        subscribe(observer);
        return observer;
    }

    /**
     * Wraps the given onXXX callbacks into a {@link Disposable} {@link CompletableObserver},
     * adds it to the given {@link DisposableContainer} and ensures, that if the upstream
     * terminates or this particular {@code Disposable} is disposed, the {@code CompletableObserver} is removed
     * from the given composite.
     * <p>
     * The {@code CompletableObserver} will be removed after the callback for the terminal event has been invoked.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code subscribe} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param onError the callback for an upstream error
     * @param onComplete the callback for an upstream completion
     * @param container the {@code DisposableContainer} (such as {@link CompositeDisposable}) to add and remove the
     *                  created {@code Disposable} {@code CompletableObserver}
     * @return the {@code Disposable} that allows disposing the particular subscription.
     * @throws NullPointerException
     *             if {@code onComplete}, {@code onError}
     *             or {@code container} is {@code null}
     * @since 3.1.0
     */
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Disposable subscribe(
            @NonNull Action onComplete,
            @NonNull Consumer<? super Throwable> onError,
            @NonNull DisposableContainer container) {
        Objects.requireNonNull(onComplete, "onComplete is null");
        Objects.requireNonNull(onError, "onError is null");
        Objects.requireNonNull(container, "container is null");

        DisposableAutoReleaseMultiObserver<Void> observer = new DisposableAutoReleaseMultiObserver<>(
                container, Functions.emptyConsumer(), onError, onComplete);
        container.add(observer);
        subscribe(observer);
        return observer;
    }

    /**
     * Subscribes to this {@code Completable} and calls the given {@link Action} when this {@code Completable}
     * completes normally.
     * <p>
     * <img width="640" height="352" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.subscribe.f.png" alt="">
     * <p>
     * If the {@code Completable} emits an error, it is wrapped into an
     * {@link OnErrorNotImplementedException}
     * and routed to the global {@link RxJavaPlugins#onError(Throwable)} handler.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code subscribe} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param onComplete the {@code Action} called when this {@code Completable} completes normally
     * @return the new {@link Disposable} that can be used for disposing the subscription at any time
     * @throws NullPointerException if {@code onComplete} is {@code null}
     * @see #subscribe(Action, Consumer, DisposableContainer)
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Disposable subscribe(@NonNull Action onComplete) {
        return subscribe(onComplete, Functions.ON_ERROR_MISSING);
    }

    /**
     * Returns a {@code Completable} which subscribes the downstream subscriber on the specified scheduler, making
     * sure the subscription side-effects happen on that specific thread of the {@link Scheduler}.
     * <p>
     * <img width="640" height="686" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.subscribeOn.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code subscribeOn} operates on a {@code Scheduler} you specify.</dd>
     * </dl>
     * @param scheduler the {@code Scheduler} to subscribe on
     * @return the new {@code Completable} instance
     * @throws NullPointerException if {@code scheduler} is {@code null}
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    public final Completable subscribeOn(@NonNull Scheduler scheduler) {
        Objects.requireNonNull(scheduler, "scheduler is null");

        return RxJavaPlugins.onAssembly(new CompletableSubscribeOn(this, scheduler));
    }

    /**
     * Terminates the downstream if this or the other {@code Completable}
     * terminates (wins the termination race) while disposing the connection to the losing source.
     * <p>
     * <img width="640" height="468" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.takeuntil.c.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code takeUntil} does not operate by default on a particular {@link Scheduler}.</dd>
     *  <dt><b>Error handling:</b></dt>
     *  <dd>If both this and the other sources signal an error, only one of the errors
     *  is signaled to the downstream and the other error is signaled to the global
     *  error handler via {@link RxJavaPlugins#onError(Throwable)}.</dd>
     * </dl>
     * <p>History: 2.1.17 - experimental
     * @param other the other completable source to observe for the terminal signals
     * @return the new {@code Completable} instance
     * @throws NullPointerException if {@code other} is {@code null}
     * @since 2.2
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Completable takeUntil(@NonNull CompletableSource other) {
        Objects.requireNonNull(other, "other is null");

        return RxJavaPlugins.onAssembly(new CompletableTakeUntilCompletable(this, other));
    }

    /**
     * Returns a {@code Completabl}e that runs this {@code Completable} and emits a {@link TimeoutException} in case
     * this {@code Completable} doesn't complete within the given time.
     * <p>
     * <img width="640" height="348" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.timeout.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code timeout} signals the {@code TimeoutException} on the {@code computation} {@link Scheduler}.</dd>
     * </dl>
     * @param timeout the timeout value
     * @param unit the unit of {@code timeout}
     * @return the new {@code Completable} instance
     * @throws NullPointerException if {@code unit} is {@code null}
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.COMPUTATION)
    @NonNull
    public final Completable timeout(long timeout, @NonNull TimeUnit unit) {
        return timeout0(timeout, unit, Schedulers.computation(), null);
    }

    /**
     * Returns a {@code Completable} that runs this {@code Completable} and switches to the other {@link CompletableSource}
     * in case this {@code Completable} doesn't complete within the given time.
     * <p>
     * <img width="640" height="308" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.timeout.c.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code timeout} subscribes to the other {@code CompletableSource} on
     *  the {@code computation} {@link Scheduler}.</dd>
     * </dl>
     * @param timeout the timeout value
     * @param unit the unit of {@code timeout}
     * @param fallback the other {@code CompletableSource} instance to switch to in case of a timeout
     * @return the new {@code Completable} instance
     * @throws NullPointerException if {@code unit} or {@code fallback} is {@code null}
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.COMPUTATION)
    public final Completable timeout(long timeout, @NonNull TimeUnit unit, @NonNull CompletableSource fallback) {
        Objects.requireNonNull(fallback, "fallback is null");
        return timeout0(timeout, unit, Schedulers.computation(), fallback);
    }

    /**
     * Returns a {@code Completable} that runs this {@code Completable} and emits a {@link TimeoutException} in case
     * this {@code Completable} doesn't complete within the given time while "waiting" on the specified
     * {@link Scheduler}.
     * <p>
     * <img width="640" height="348" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.timeout.s.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code timeout} signals the {@code TimeoutException} on the {@code Scheduler} you specify.</dd>
     * </dl>
     * @param timeout the timeout value
     * @param unit the unit of {@code timeout}
     * @param scheduler the {@code Scheduler} to use to wait for completion and signal {@code TimeoutException}
     * @return the new {@code Completable} instance
     * @throws NullPointerException if {@code unit} or {@code scheduler} is {@code null}
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    @NonNull
    public final Completable timeout(long timeout, @NonNull TimeUnit unit, @NonNull Scheduler scheduler) {
        return timeout0(timeout, unit, scheduler, null);
    }

    /**
     * Returns a {@code Completable} that runs this {@code Completable} and switches to the other {@link CompletableSource}
     * in case this {@code Completable} doesn't complete within the given time while "waiting" on
     * the specified {@link Scheduler}.
     * <p>
     * <img width="640" height="308" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.timeout.sc.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code timeout} subscribes to the other {@code CompletableSource} on
     *  the {@code Scheduler} you specify.</dd>
     * </dl>
     * @param timeout the timeout value
     * @param unit the unit of {@code timeout}
     * @param scheduler the {@code Scheduler} to use to wait for completion
     * @param fallback the other {@code Completable} instance to switch to in case of a timeout
     * @return the new {@code Completable} instance
     * @throws NullPointerException if {@code unit}, {@code scheduler} or {@code fallback} is {@code null}
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    public final Completable timeout(long timeout, @NonNull TimeUnit unit, @NonNull Scheduler scheduler, @NonNull CompletableSource fallback) {
        Objects.requireNonNull(fallback, "fallback is null");
        return timeout0(timeout, unit, scheduler, fallback);
    }

    /**
     * Returns a {@code Completable} that runs this {@code Completable} and optionally switches to the other {@link CompletableSource}
     * in case this {@code Completable} doesn't complete within the given time while "waiting" on
     * the specified {@link Scheduler}.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>You specify the {@code Scheduler} this operator runs on.</dd>
     * </dl>
     * @param timeout the timeout value
     * @param unit the unit of {@code timeout}
     * @param scheduler the {@code Scheduler} to use to wait for completion
     * @param fallback the other {@code Completable} instance to switch to in case of a timeout,
     * if {@code null} a {@link TimeoutException} is emitted instead
     * @return the new {@code Completable} instance
     * @throws NullPointerException if {@code unit}, {@code scheduler} or {@code fallback} is {@code null}
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    private Completable timeout0(long timeout, TimeUnit unit, Scheduler scheduler, CompletableSource fallback) {
        Objects.requireNonNull(unit, "unit is null");
        Objects.requireNonNull(scheduler, "scheduler is null");
        return RxJavaPlugins.onAssembly(new CompletableTimeout(this, timeout, unit, scheduler, fallback));
    }

    /**
     * Calls the specified {@link CompletableConverter} function during assembly time and returns its resulting value.
     * <p>
     * <img width="640" height="751" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.to.png" alt="">
     * <p>
     * This allows fluent conversion to any other type.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code to} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * <p>History: 2.1.7 - experimental
     * @param <R> the resulting object type
     * @param converter the {@code CompletableConverter} that receives the current {@code Completable} instance and returns a value to be the result of {@code to()}
     * @return the converted value
     * @throws NullPointerException if {@code converter} is {@code null}
     * @since 2.2
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public final <R> R to(@NonNull CompletableConverter<? extends R> converter) {
        return Objects.requireNonNull(converter, "converter is null").apply(this);
    }

    /**
     * Returns a {@link Flowable} which when subscribed to subscribes to this {@code Completable} and
     * relays the terminal events to the downstream {@link Subscriber}.
     * <p>
     * <img width="640" height="585" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.toFlowable.png" alt="">
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The returned {@code Flowable} honors the backpressure of the downstream consumer.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code toFlowable} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the value type
     * @return the new {@code Flowable} instance
     */
    @CheckReturnValue
    @SuppressWarnings("unchecked")
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <@NonNull T> Flowable<T> toFlowable() {
        if (this instanceof FuseToFlowable) {
            return ((FuseToFlowable<T>)this).fuseToFlowable();
        }
        return RxJavaPlugins.onAssembly(new CompletableToFlowable<>(this));
    }
    /**
     * Returns a {@link Future} representing the termination of the current {@code Completable}
     * via a {@code null} value.
     * <p>
     * <img width="640" height="433" src="https://github.com/ReactiveX/RxJava/wiki/images/rx-operators/Completable.toFuture.png" alt="">
     * <p>
     * Cancelling the {@code Future} will cancel the subscription to the current {@code Completable}.
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
    public final Future<Void> toFuture() {
        return subscribeWith(new FutureMultiObserver<>());
    }

    /**
     * Converts this {@code Completable} into a {@link Maybe}.
     * <p>
     * <img width="640" height="585" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.toMaybe.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code toMaybe} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T> the value type
     * @return the new {@code Maybe} instance
     */
    @CheckReturnValue
    @SuppressWarnings("unchecked")
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <@NonNull T> Maybe<T> toMaybe() {
        if (this instanceof FuseToMaybe) {
            return ((FuseToMaybe<T>)this).fuseToMaybe();
        }
        return RxJavaPlugins.onAssembly(new MaybeFromCompletable<>(this));
    }

    /**
     * Returns an {@link Observable} which when subscribed to subscribes to this {@code Completable} and
     * relays the terminal events to the downstream {@link Observer}.
     * <p>
     * <img width="640" height="293" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.toObservable.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code toObservable} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the value type
     * @return the new {@code Observable} created
     */
    @CheckReturnValue
    @SuppressWarnings("unchecked")
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <@NonNull T> Observable<T> toObservable() {
        if (this instanceof FuseToObservable) {
            return ((FuseToObservable<T>)this).fuseToObservable();
        }
        return RxJavaPlugins.onAssembly(new CompletableToObservable<>(this));
    }

    /**
     * Converts this {@code Completable} into a {@link Single} which when this {@code Completable} completes normally,
     * calls the given {@link Supplier} and emits its returned value through {@code onSuccess}.
     * <p>
     * <img width="640" height="583" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.toSingle.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code toSingle} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the value type
     * @param completionValueSupplier the value supplier called when this {@code Completable} completes normally
     * @return the new {@code Single} instance
     * @throws NullPointerException if {@code completionValueSupplier} is {@code null}
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public final <@NonNull T> Single<T> toSingle(@NonNull Supplier<? extends T> completionValueSupplier) {
        Objects.requireNonNull(completionValueSupplier, "completionValueSupplier is null");
        return RxJavaPlugins.onAssembly(new CompletableToSingle<>(this, completionValueSupplier, null));
    }

    /**
     * Converts this {@code Completable} into a {@link Single} which when this {@code Completable} completes normally,
     * emits the given value through {@code onSuccess}.
     * <p>
     * <img width="640" height="583" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.toSingleDefault.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code toSingleDefault} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the value type
     * @param completionValue the value to emit when this {@code Completable} completes normally
     * @return the new {@code Single} instance
     * @throws NullPointerException if {@code completionValue} is {@code null}
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public final <@NonNull T> Single<T> toSingleDefault(T completionValue) {
        Objects.requireNonNull(completionValue, "completionValue is null");
        return RxJavaPlugins.onAssembly(new CompletableToSingle<>(this, null, completionValue));
    }

    /**
     * Returns a {@code Completable} which makes sure when an observer disposes the subscription, the
     * {@code dispose()} method is called on the specified {@link Scheduler}.
     * <p>
     * <img width="640" height="716" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.unsubscribeOn.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code unsubscribeOn} calls {@code dispose()} of the upstream on the {@code Scheduler} you specify.</dd>
     * </dl>
     * @param scheduler the target {@code Scheduler} where to execute the disposing
     * @return the new {@code Completable} instance
     * @throws NullPointerException if {@code scheduler} is {@code null}
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    public final Completable unsubscribeOn(@NonNull Scheduler scheduler) {
        Objects.requireNonNull(scheduler, "scheduler is null");
        return RxJavaPlugins.onAssembly(new CompletableDisposeOn(this, scheduler));
    }
    // -------------------------------------------------------------------------
    // Fluent test support, super handy and reduces test preparation boilerplate
    // -------------------------------------------------------------------------

    /**
     * Creates a {@link TestObserver} and subscribes
     * it to this {@code Completable}.
     * <p>
     * <img width="640" height="458" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.test.png" alt="">
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
    public final TestObserver<Void> test() {
        TestObserver<Void> to = new TestObserver<>();
        subscribe(to);
        return to;
    }

    /**
     * Creates a {@link TestObserver} optionally in cancelled state, then subscribes it to this {@code Completable}.
     * @param dispose if {@code true}, the {@code TestObserver} will be cancelled before subscribing to this
     * {@code Completable}.
     * <p>
     * <img width="640" height="499" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.test.b.png" alt="">
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
    public final TestObserver<Void> test(boolean dispose) {
        TestObserver<Void> to = new TestObserver<>();

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
     * Signals completion (or error) when the {@link CompletionStage} terminates.
     * <p>
     * <img width="640" height="262" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/fromCompletionStage.c.png" alt="">
     * <p>
     * Note that the operator takes an already instantiated, running or terminated {@code CompletionStage}.
     * If the optional is to be created per consumer upon subscription, use {@link #defer(Supplier)}
     * around {@code fromCompletionStage}:
     * <pre><code>
     * Maybe.defer(() -&gt; Completable.fromCompletionStage(createCompletionStage()));
     * </code></pre>
     * <p>
     * Canceling the flow can't cancel the execution of the {@code CompletionStage} because {@code CompletionStage}
     * itself doesn't support cancellation. Instead, the operator detaches from the {@code CompletionStage}.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code fromCompletionStage} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param stage the {@code CompletionStage} to convert to a {@code Completable} and
     *              signal {@code onComplete} or {@code onError} when the {@code CompletionStage} terminates normally or with a failure
     * @return the new {@code Completable} instance
     * @throws NullPointerException if {@code stage} is {@code null}
     * @since 3.0.0
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public static Completable fromCompletionStage(@NonNull CompletionStage<?> stage) {
        Objects.requireNonNull(stage, "stage is null");
        return RxJavaPlugins.onAssembly(new CompletableFromCompletionStage<>(stage));
    }

    /**
     * Signals the given default item when the upstream completes or signals the upstream error via
     * a {@link CompletionStage}.
     * <p>
     * <img width="640" height="323" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/toCompletionStage.c.png" alt="">
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
     * @param <T> the type of the default item to signal upon completion
     * @param defaultItem the item to signal upon completion
     * @return the new {@code CompletionStage} instance
     * @since 3.0.0
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <@Nullable T> CompletionStage<T> toCompletionStage(T defaultItem) {
        return subscribeWith(new CompletionStageConsumer<>(true, defaultItem));
    }
}
