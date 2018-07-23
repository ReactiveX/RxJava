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

import java.util.concurrent.*;

import org.reactivestreams.Publisher;

import io.reactivex.annotations.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.functions.*;
import io.reactivex.internal.functions.*;
import io.reactivex.internal.fuseable.*;
import io.reactivex.internal.observers.*;
import io.reactivex.internal.operators.completable.*;
import io.reactivex.internal.operators.maybe.*;
import io.reactivex.internal.operators.mixed.*;
import io.reactivex.internal.operators.single.SingleDelayWithCompletable;
import io.reactivex.internal.util.ExceptionHelper;
import io.reactivex.observers.TestObserver;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.schedulers.Schedulers;

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
 * Like {@link Observable}, a running {@code Completable} can be stopped through the {@link Disposable} instance
 * provided to consumers through {@link SingleObserver#onSubscribe}.
 * <p>
 * Like an {@code Observable}, a {@code Completable} is lazy, can be either "hot" or "cold", synchronous or
 * asynchronous. {@code Completable} instances returned by the methods of this class are <em>cold</em>
 * and there is a standard <em>hot</em> implementation in the form of a subject:
 * {@link io.reactivex.subjects.CompletableSubject CompletableSubject}.
 * <p>
 * The documentation for this class makes use of marble diagrams. The following legend explains these diagrams:
 * <p>
 * <img width="640" height="577" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.png" alt="">
 * <p>
 * See {@link Flowable} or {@link Observable} for the
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
 * Note that by design, subscriptions via {@link #subscribe(CompletableObserver)} can't be cancelled/disposed
 * from the outside (hence the
 * {@code void} return of the {@link #subscribe(CompletableObserver)} method) and it is the
 * responsibility of the implementor of the {@code CompletableObserver} to allow this to happen.
 * RxJava supports such usage with the standard
 * {@link io.reactivex.observers.DisposableCompletableObserver DisposableCompletableObserver} instance.
 * For convenience, the {@link #subscribeWith(CompletableObserver)} method is provided as well to
 * allow working with a {@code CompletableObserver} (or subclass) instance to be applied with in
 * a fluent manner (such as in the example above).
 *
 * @see io.reactivex.observers.DisposableCompletableObserver
 */
public abstract class Completable implements CompletableSource {
    /**
     * Returns a Completable which terminates as soon as one of the source Completables
     * terminates (normally or with an error) and cancels all other Completables.
     * <p>
     * <img width="640" height="518" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.ambArray.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code ambArray} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param sources the array of source Completables. A subscription to each source will
     *            occur in the same order as in this array.
     * @return the new Completable instance
     * @throws NullPointerException if sources is null
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public static Completable ambArray(final CompletableSource... sources) {
        ObjectHelper.requireNonNull(sources, "sources is null");
        if (sources.length == 0) {
            return complete();
        }
        if (sources.length == 1) {
            return wrap(sources[0]);
        }

        return RxJavaPlugins.onAssembly(new CompletableAmb(sources, null));
    }

    /**
     * Returns a Completable which terminates as soon as one of the source Completables
     * terminates (normally or with an error) and cancels all other Completables.
     * <p>
     * <img width="640" height="518" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.amb.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code amb} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param sources the array of source Completables. A subscription to each source will
     *            occur in the same order as in this Iterable.
     * @return the new Completable instance
     * @throws NullPointerException if sources is null
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public static Completable amb(final Iterable<? extends CompletableSource> sources) {
        ObjectHelper.requireNonNull(sources, "sources is null");

        return RxJavaPlugins.onAssembly(new CompletableAmb(null, sources));
    }

    /**
     * Returns a Completable instance that completes immediately when subscribed to.
     * <p>
     * <img width="640" height="472" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.complete.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code complete} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @return a Completable instance that completes immediately
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public static Completable complete() {
        return RxJavaPlugins.onAssembly(CompletableEmpty.INSTANCE);
    }

    /**
     * Returns a Completable which completes only when all sources complete, one after another.
     * <p>
     * <img width="640" height="283" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.concatArray.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code concatArray} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param sources the sources to concatenate
     * @return the Completable instance which completes only when all sources complete
     * @throws NullPointerException if sources is null
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public static Completable concatArray(CompletableSource... sources) {
        ObjectHelper.requireNonNull(sources, "sources is null");
        if (sources.length == 0) {
            return complete();
        } else
        if (sources.length == 1) {
            return wrap(sources[0]);
        }
        return RxJavaPlugins.onAssembly(new CompletableConcatArray(sources));
    }

    /**
     * Returns a Completable which completes only when all sources complete, one after another.
     * <p>
     * <img width="640" height="303" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.concat.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code concat} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param sources the sources to concatenate
     * @return the Completable instance which completes only when all sources complete
     * @throws NullPointerException if sources is null
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public static Completable concat(Iterable<? extends CompletableSource> sources) {
        ObjectHelper.requireNonNull(sources, "sources is null");

        return RxJavaPlugins.onAssembly(new CompletableConcatIterable(sources));
    }

    /**
     * Returns a Completable which completes only when all sources complete, one after another.
     * <p>
     * <img width="640" height="237" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.concat.p.png" alt="">
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The returned {@code Completable} honors the backpressure of the downstream consumer
     *  and expects the other {@code Publisher} to honor it as well.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code concat} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param sources the sources to concatenate
     * @return the Completable instance which completes only when all sources complete
     * @throws NullPointerException if sources is null
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @BackpressureSupport(BackpressureKind.FULL)
    public static Completable concat(Publisher<? extends CompletableSource> sources) {
        return concat(sources, 2);
    }

    /**
     * Returns a Completable which completes only when all sources complete, one after another.
     * <p>
     * <img width="640" height="237" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.concat.pn.png" alt="">
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The returned {@code Completable} honors the backpressure of the downstream consumer
     *  and expects the other {@code Publisher} to honor it as well.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code concat} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param sources the sources to concatenate
     * @param prefetch the number of sources to prefetch from the sources
     * @return the Completable instance which completes only when all sources complete
     * @throws NullPointerException if sources is null
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @BackpressureSupport(BackpressureKind.FULL)
    public static Completable concat(Publisher<? extends CompletableSource> sources, int prefetch) {
        ObjectHelper.requireNonNull(sources, "sources is null");
        ObjectHelper.verifyPositive(prefetch, "prefetch");
        return RxJavaPlugins.onAssembly(new CompletableConcat(sources, prefetch));
    }

    /**
     * Provides an API (via a cold Completable) that bridges the reactive world with the callback-style world.
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
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code create} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param source the emitter that is called when a CompletableObserver subscribes to the returned {@code Completable}
     * @return the new Completable instance
     * @see CompletableOnSubscribe
     * @see Cancellable
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public static Completable create(CompletableOnSubscribe source) {
        ObjectHelper.requireNonNull(source, "source is null");
        return RxJavaPlugins.onAssembly(new CompletableCreate(source));
    }

    /**
     * Constructs a Completable instance by wrapping the given source callback
     * <strong>without any safeguards; you should manage the lifecycle and response
     * to downstream cancellation/dispose</strong>.
     * <p>
     * <img width="640" height="260" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.unsafeCreate.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code unsafeCreate} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param source the callback which will receive the CompletableObserver instances
     * when the Completable is subscribed to.
     * @return the created Completable instance
     * @throws NullPointerException if source is null
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public static Completable unsafeCreate(CompletableSource source) {
        ObjectHelper.requireNonNull(source, "source is null");
        if (source instanceof Completable) {
            throw new IllegalArgumentException("Use of unsafeCreate(Completable)!");
        }
        return RxJavaPlugins.onAssembly(new CompletableFromUnsafeSource(source));
    }

    /**
     * Defers the subscription to a Completable instance returned by a supplier.
     * <p>
     * <img width="640" height="298" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.defer.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code defer} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param completableSupplier the supplier that returns the Completable that will be subscribed to.
     * @return the Completable instance
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public static Completable defer(final Callable<? extends CompletableSource> completableSupplier) {
        ObjectHelper.requireNonNull(completableSupplier, "completableSupplier");
        return RxJavaPlugins.onAssembly(new CompletableDefer(completableSupplier));
    }

    /**
     * Creates a Completable which calls the given error supplier for each subscriber
     * and emits its returned Throwable.
     * <p>
     * <img width="640" height="462" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.error.f.png" alt="">
     * <p>
     * If the errorSupplier returns null, the child CompletableObservers will receive a
     * NullPointerException.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code error} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param errorSupplier the error supplier, not null
     * @return the new Completable instance
     * @throws NullPointerException if errorSupplier is null
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public static Completable error(final Callable<? extends Throwable> errorSupplier) {
        ObjectHelper.requireNonNull(errorSupplier, "errorSupplier is null");
        return RxJavaPlugins.onAssembly(new CompletableErrorSupplier(errorSupplier));
    }

    /**
     * Creates a Completable instance that emits the given Throwable exception to subscribers.
     * <p>
     * <img width="640" height="462" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.error.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code error} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param error the Throwable instance to emit, not null
     * @return the new Completable instance
     * @throws NullPointerException if error is null
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public static Completable error(final Throwable error) {
        ObjectHelper.requireNonNull(error, "error is null");
        return RxJavaPlugins.onAssembly(new CompletableError(error));
    }


    /**
     * Returns a Completable instance that runs the given Action for each subscriber and
     * emits either an unchecked exception or simply completes.
     * <p>
     * <img width="640" height="297" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.fromAction.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code fromAction} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param run the runnable to run for each subscriber
     * @return the new Completable instance
     * @throws NullPointerException if run is null
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public static Completable fromAction(final Action run) {
        ObjectHelper.requireNonNull(run, "run is null");
        return RxJavaPlugins.onAssembly(new CompletableFromAction(run));
    }

    /**
     * Returns a Completable which when subscribed, executes the callable function, ignores its
     * normal result and emits onError or onComplete only.
     * <p>
     * <img width="640" height="286" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.fromCallable.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code fromCallable} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param callable the callable instance to execute for each subscriber
     * @return the new Completable instance
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public static Completable fromCallable(final Callable<?> callable) {
        ObjectHelper.requireNonNull(callable, "callable is null");
        return RxJavaPlugins.onAssembly(new CompletableFromCallable(callable));
    }

    /**
     * Returns a Completable instance that reacts to the termination of the given Future in a blocking fashion.
     * <p>
     * <img width="640" height="628" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.fromFuture.png" alt="">
     * <p>
     * Note that cancellation from any of the subscribers to this Completable will cancel the future.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code fromFuture} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param future the future to react to
     * @return the new Completable instance
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public static Completable fromFuture(final Future<?> future) {
        ObjectHelper.requireNonNull(future, "future is null");
        return fromAction(Functions.futureAction(future));
    }

    /**
     * Returns a Completable instance that when subscribed to, subscribes to the {@code Maybe} instance and
     * emits a completion event if the maybe emits {@code onSuccess}/{@code onComplete} or forwards any
     * {@code onError} events.
     * <p>
     * <img width="640" height="235" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.fromMaybe.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code fromMaybe} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * <p>History: 2.1.17 - beta
     * @param <T> the value type of the {@link MaybeSource} element
     * @param maybe the Maybe instance to subscribe to, not null
     * @return the new Completable instance
     * @throws NullPointerException if single is null
     * @since 2.2
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <T> Completable fromMaybe(final MaybeSource<T> maybe) {
        ObjectHelper.requireNonNull(maybe, "maybe is null");
        return RxJavaPlugins.onAssembly(new MaybeIgnoreElementCompletable<T>(maybe));
    }

    /**
     * Returns a Completable instance that runs the given Runnable for each subscriber and
     * emits either its exception or simply completes.
     * <p>
     * <img width="640" height="297" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.fromRunnable.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code fromRunnable} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param run the runnable to run for each subscriber
     * @return the new Completable instance
     * @throws NullPointerException if run is null
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public static Completable fromRunnable(final Runnable run) {
        ObjectHelper.requireNonNull(run, "run is null");
        return RxJavaPlugins.onAssembly(new CompletableFromRunnable(run));
    }

    /**
     * Returns a Completable instance that subscribes to the given Observable, ignores all values and
     * emits only the terminal event.
     * <p>
     * <img width="640" height="414" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.fromObservable.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code fromObservable} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the type of the Observable
     * @param observable the Observable instance to subscribe to, not null
     * @return the new Completable instance
     * @throws NullPointerException if flowable is null
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <T> Completable fromObservable(final ObservableSource<T> observable) {
        ObjectHelper.requireNonNull(observable, "observable is null");
        return RxJavaPlugins.onAssembly(new CompletableFromObservable<T>(observable));
    }

    /**
     * Returns a Completable instance that subscribes to the given publisher, ignores all values and
     * emits only the terminal event.
     * <p>
     * <img width="640" height="422" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.fromPublisher.png" alt="">
     * <p>
     * The {@link Publisher} must follow the
     * <a href="https://github.com/reactive-streams/reactive-streams-jvm#reactive-streams">Reactive-Streams specification</a>.
     * Violating the specification may result in undefined behavior.
     * <p>
     * If possible, use {@link #create(CompletableOnSubscribe)} to create a
     * source-like {@code Completable} instead.
     * <p>
     * Note that even though {@link Publisher} appears to be a functional interface, it
     * is not recommended to implement it through a lambda as the specification requires
     * state management that is not achievable with a stateless lambda.
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The returned {@code Completable} honors the backpressure of the downstream consumer
     *  and expects the other {@code Publisher} to honor it as well.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code fromPublisher} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the type of the publisher
     * @param publisher the Publisher instance to subscribe to, not null
     * @return the new Completable instance
     * @throws NullPointerException if publisher is null
     * @see #create(CompletableOnSubscribe)
     */
    @CheckReturnValue
    @BackpressureSupport(BackpressureKind.UNBOUNDED_IN)
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <T> Completable fromPublisher(final Publisher<T> publisher) {
        ObjectHelper.requireNonNull(publisher, "publisher is null");
        return RxJavaPlugins.onAssembly(new CompletableFromPublisher<T>(publisher));
    }

    /**
     * Returns a Completable instance that when subscribed to, subscribes to the Single instance and
     * emits a completion event if the single emits onSuccess or forwards any onError events.
     * <p>
     * <img width="640" height="356" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.fromSingle.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code fromSingle} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the value type of the Single
     * @param single the Single instance to subscribe to, not null
     * @return the new Completable instance
     * @throws NullPointerException if single is null
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <T> Completable fromSingle(final SingleSource<T> single) {
        ObjectHelper.requireNonNull(single, "single is null");
        return RxJavaPlugins.onAssembly(new CompletableFromSingle<T>(single));
    }

    /**
     * Returns a Completable instance that subscribes to all sources at once and
     * completes only when all source Completables complete or one of them emits an error.
     * <p>
     * <img width="640" height="270" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.mergeArray.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code mergeArray} does not operate by default on a particular {@link Scheduler}.</dd>
     *  <dt><b>Error handling:</b></dt>
     *  <dd>If any of the source {@code CompletableSource}s signal a {@code Throwable} via {@code onError}, the resulting
     *  {@code Completable} terminates with that {@code Throwable} and all other source {@code CompletableSource}s are cancelled.
     *  If more than one {@code CompletableSource} signals an error, the resulting {@code Completable} may terminate with the
     *  first one's error or, depending on the concurrency of the sources, may terminate with a
     *  {@code CompositeException} containing two or more of the various error signals.
     *  {@code Throwable}s that didn't make into the composite will be sent (individually) to the global error handler via
     *  {@link RxJavaPlugins#onError(Throwable)} method as {@code UndeliverableException} errors. Similarly, {@code Throwable}s
     *  signaled by source(s) after the returned {@code Completable} has been cancelled or terminated with a
     *  (composite) error will be sent to the same global error handler.
     *  Use {@link #mergeArrayDelayError(CompletableSource...)} to merge sources and terminate only when all source {@code CompletableSource}s
     *  have completed or failed with an error.
     *  </dd>
     * </dl>
     * @param sources the iterable sequence of sources.
     * @return the new Completable instance
     * @throws NullPointerException if sources is null
     * @see #mergeArrayDelayError(CompletableSource...)
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public static Completable mergeArray(CompletableSource... sources) {
        ObjectHelper.requireNonNull(sources, "sources is null");
        if (sources.length == 0) {
            return complete();
        } else
        if (sources.length == 1) {
            return wrap(sources[0]);
        }
        return RxJavaPlugins.onAssembly(new CompletableMergeArray(sources));
    }

    /**
     * Returns a Completable instance that subscribes to all sources at once and
     * completes only when all source Completables complete or one of them emits an error.
     * <p>
     * <img width="640" height="311" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.merge.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code merge} does not operate by default on a particular {@link Scheduler}.</dd>
     *  <dt><b>Error handling:</b></dt>
     *  <dd>If any of the source {@code CompletableSource}s signal a {@code Throwable} via {@code onError}, the resulting
     *  {@code Completable} terminates with that {@code Throwable} and all other source {@code CompletableSource}s are cancelled.
     *  If more than one {@code CompletableSource} signals an error, the resulting {@code Completable} may terminate with the
     *  first one's error or, depending on the concurrency of the sources, may terminate with a
     *  {@code CompositeException} containing two or more of the various error signals.
     *  {@code Throwable}s that didn't make into the composite will be sent (individually) to the global error handler via
     *  {@link RxJavaPlugins#onError(Throwable)} method as {@code UndeliverableException} errors. Similarly, {@code Throwable}s
     *  signaled by source(s) after the returned {@code Completable} has been cancelled or terminated with a
     *  (composite) error will be sent to the same global error handler.
     *  Use {@link #mergeDelayError(Iterable)} to merge sources and terminate only when all source {@code CompletableSource}s
     *  have completed or failed with an error.
     *  </dd>
     * </dl>
     * @param sources the iterable sequence of sources.
     * @return the new Completable instance
     * @throws NullPointerException if sources is null
     * @see #mergeDelayError(Iterable)
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public static Completable merge(Iterable<? extends CompletableSource> sources) {
        ObjectHelper.requireNonNull(sources, "sources is null");
        return RxJavaPlugins.onAssembly(new CompletableMergeIterable(sources));
    }

    /**
     * Returns a Completable instance that subscribes to all sources at once and
     * completes only when all source Completables complete or one of them emits an error.
     * <p>
     * <img width="640" height="336" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.merge.p.png" alt="">
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The returned {@code Completable} honors the backpressure of the downstream consumer
     *  and expects the other {@code Publisher} to honor it as well.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code merge} does not operate by default on a particular {@link Scheduler}.</dd>
     *  <dt><b>Error handling:</b></dt>
     *  <dd>If any of the source {@code CompletableSource}s signal a {@code Throwable} via {@code onError}, the resulting
     *  {@code Completable} terminates with that {@code Throwable} and all other source {@code CompletableSource}s are cancelled.
     *  If more than one {@code CompletableSource} signals an error, the resulting {@code Completable} may terminate with the
     *  first one's error or, depending on the concurrency of the sources, may terminate with a
     *  {@code CompositeException} containing two or more of the various error signals.
     *  {@code Throwable}s that didn't make into the composite will be sent (individually) to the global error handler via
     *  {@link RxJavaPlugins#onError(Throwable)} method as {@code UndeliverableException} errors. Similarly, {@code Throwable}s
     *  signaled by source(s) after the returned {@code Completable} has been cancelled or terminated with a
     *  (composite) error will be sent to the same global error handler.
     *  Use {@link #mergeDelayError(Publisher)} to merge sources and terminate only when all source {@code CompletableSource}s
     *  have completed or failed with an error.
     *  </dd>
     * </dl>
     * @param sources the iterable sequence of sources.
     * @return the new Completable instance
     * @throws NullPointerException if sources is null
     * @see #mergeDelayError(Publisher)
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @BackpressureSupport(BackpressureKind.UNBOUNDED_IN)
    public static Completable merge(Publisher<? extends CompletableSource> sources) {
        return merge0(sources, Integer.MAX_VALUE, false);
    }

    /**
     * Returns a Completable instance that keeps subscriptions to a limited number of sources at once and
     * completes only when all source Completables complete or one of them emits an error.
     * <p>
     * <img width="640" height="269" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.merge.pn.png" alt="">
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The returned {@code Completable} honors the backpressure of the downstream consumer
     *  and expects the other {@code Publisher} to honor it as well.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code merge} does not operate by default on a particular {@link Scheduler}.</dd>
     *  <dt><b>Error handling:</b></dt>
     *  <dd>If any of the source {@code CompletableSource}s signal a {@code Throwable} via {@code onError}, the resulting
     *  {@code Completable} terminates with that {@code Throwable} and all other source {@code CompletableSource}s are cancelled.
     *  If more than one {@code CompletableSource} signals an error, the resulting {@code Completable} may terminate with the
     *  first one's error or, depending on the concurrency of the sources, may terminate with a
     *  {@code CompositeException} containing two or more of the various error signals.
     *  {@code Throwable}s that didn't make into the composite will be sent (individually) to the global error handler via
     *  {@link RxJavaPlugins#onError(Throwable)} method as {@code UndeliverableException} errors. Similarly, {@code Throwable}s
     *  signaled by source(s) after the returned {@code Completable} has been cancelled or terminated with a
     *  (composite) error will be sent to the same global error handler.
     *  Use {@link #mergeDelayError(Publisher, int)} to merge sources and terminate only when all source {@code CompletableSource}s
     *  have completed or failed with an error.
     *  </dd>
     * </dl>
     * @param sources the iterable sequence of sources.
     * @param maxConcurrency the maximum number of concurrent subscriptions
     * @return the new Completable instance
     * @throws NullPointerException if sources is null
     * @throws IllegalArgumentException if maxConcurrency is less than 1
     * @see #mergeDelayError(Publisher, int)
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @BackpressureSupport(BackpressureKind.FULL)
    public static Completable merge(Publisher<? extends CompletableSource> sources, int maxConcurrency) {
        return merge0(sources, maxConcurrency, false);
    }

    /**
     * Returns a Completable instance that keeps subscriptions to a limited number of sources at once and
     * completes only when all source Completables terminate in one way or another, combining any exceptions
     * thrown by either the sources Observable or the inner Completable instances.
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The returned {@code Flowable} honors the backpressure of the downstream consumer
     *  and expects the other {@code Publisher} to honor it as well.
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
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @BackpressureSupport(BackpressureKind.FULL)
    private static Completable merge0(Publisher<? extends CompletableSource> sources, int maxConcurrency, boolean delayErrors) {
        ObjectHelper.requireNonNull(sources, "sources is null");
        ObjectHelper.verifyPositive(maxConcurrency, "maxConcurrency");
        return RxJavaPlugins.onAssembly(new CompletableMerge(sources, maxConcurrency, delayErrors));
    }

    /**
     * Returns a CompletableConsumable that subscribes to all Completables in the source array and delays
     * any error emitted by either the sources observable or any of the inner Completables until all of
     * them terminate in a way or another.
     * <p>
     * <img width="640" height="430" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.mergeArrayDelayError.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code mergeArrayDelayError} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param sources the array of Completables
     * @return the new Completable instance
     * @throws NullPointerException if sources is null
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public static Completable mergeArrayDelayError(CompletableSource... sources) {
        ObjectHelper.requireNonNull(sources, "sources is null");
        return RxJavaPlugins.onAssembly(new CompletableMergeDelayErrorArray(sources));
    }

    /**
     * Returns a Completable that subscribes to all Completables in the source sequence and delays
     * any error emitted by either the sources observable or any of the inner Completables until all of
     * them terminate in a way or another.
     * <p>
     * <img width="640" height="475" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.mergeDelayError.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code mergeDelayError} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param sources the sequence of Completables
     * @return the new Completable instance
     * @throws NullPointerException if sources is null
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public static Completable mergeDelayError(Iterable<? extends CompletableSource> sources) {
        ObjectHelper.requireNonNull(sources, "sources is null");
        return RxJavaPlugins.onAssembly(new CompletableMergeDelayErrorIterable(sources));
    }


    /**
     * Returns a Completable that subscribes to all Completables in the source sequence and delays
     * any error emitted by either the sources observable or any of the inner Completables until all of
     * them terminate in a way or another.
     * <p>
     * <img width="640" height="466" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.mergeDelayError.p.png" alt="">
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The returned {@code Completable} honors the backpressure of the downstream consumer
     *  and expects the other {@code Publisher} to honor it as well.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code mergeDelayError} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param sources the sequence of Completables
     * @return the new Completable instance
     * @throws NullPointerException if sources is null
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @BackpressureSupport(BackpressureKind.UNBOUNDED_IN)
    public static Completable mergeDelayError(Publisher<? extends CompletableSource> sources) {
        return merge0(sources, Integer.MAX_VALUE, true);
    }

    /**
     * Returns a Completable that subscribes to a limited number of inner Completables at once in
     * the source sequence and delays any error emitted by either the sources
     * observable or any of the inner Completables until all of
     * them terminate in a way or another.
     * <p>
     * <img width="640" height="440" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.mergeDelayError.pn.png" alt="">
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The returned {@code Completable} honors the backpressure of the downstream consumer
     *  and expects the other {@code Publisher} to honor it as well.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code mergeDelayError} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param sources the sequence of Completables
     * @param maxConcurrency the maximum number of concurrent subscriptions to Completables
     * @return the new Completable instance
     * @throws NullPointerException if sources is null
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @BackpressureSupport(BackpressureKind.FULL)
    public static Completable mergeDelayError(Publisher<? extends CompletableSource> sources, int maxConcurrency) {
        return merge0(sources, maxConcurrency, true);
    }

    /**
     * Returns a Completable that never calls onError or onComplete.
     * <p>
     * <img width="640" height="512" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.never.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code never} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @return the singleton instance that never calls onError or onComplete
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public static Completable never() {
        return RxJavaPlugins.onAssembly(CompletableNever.INSTANCE);
    }

    /**
     * Returns a Completable instance that fires its onComplete event after the given delay elapsed.
     * <p>
     * <img width="640" height="413" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.timer.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code timer} does operate by default on the {@code computation} {@link Scheduler}.</dd>
     * </dl>
     * @param delay the delay time
     * @param unit the delay unit
     * @return the new Completable instance
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.COMPUTATION)
    public static Completable timer(long delay, TimeUnit unit) {
        return timer(delay, unit, Schedulers.computation());
    }

    /**
     * Returns a Completable instance that fires its onComplete event after the given delay elapsed
     * by using the supplied scheduler.
     * <p>
     * <img width="640" height="413" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.timer.s.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code timer} operates on the {@link Scheduler} you specify.</dd>
     * </dl>
     * @param delay the delay time
     * @param unit the delay unit
     * @param scheduler the scheduler where to emit the complete event
     * @return the new Completable instance
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    public static Completable timer(final long delay, final TimeUnit unit, final Scheduler scheduler) {
        ObjectHelper.requireNonNull(unit, "unit is null");
        ObjectHelper.requireNonNull(scheduler, "scheduler is null");
        return RxJavaPlugins.onAssembly(new CompletableTimer(delay, unit, scheduler));
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
     * <img width="640" height="388" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.using.png" alt="">
     * <p>
     * This overload disposes eagerly before the terminal event is emitted.
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
    @CheckReturnValue
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
     * <img width="640" height="332" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.using.b.png" alt="">
     * <p>
     * If this overload performs a lazy cancellation after the terminal event is emitted.
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
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <R> Completable using(
            final Callable<R> resourceSupplier,
            final Function<? super R, ? extends CompletableSource> completableFunction,
            final Consumer<? super R> disposer,
            final boolean eager) {
        ObjectHelper.requireNonNull(resourceSupplier, "resourceSupplier is null");
        ObjectHelper.requireNonNull(completableFunction, "completableFunction is null");
        ObjectHelper.requireNonNull(disposer, "disposer is null");

        return RxJavaPlugins.onAssembly(new CompletableUsing<R>(resourceSupplier, completableFunction, disposer, eager));
    }

    /**
     * Wraps the given CompletableSource into a Completable
     * if not already Completable.
     * <p>
     * <img width="640" height="354" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.wrap.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code wrap} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param source the source to wrap
     * @return the source or its wrapper Completable
     * @throws NullPointerException if source is null
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public static Completable wrap(CompletableSource source) {
        ObjectHelper.requireNonNull(source, "source is null");
        if (source instanceof Completable) {
            return RxJavaPlugins.onAssembly((Completable)source);
        }
        return RxJavaPlugins.onAssembly(new CompletableFromUnsafeSource(source));
    }

    /**
     * Returns a Completable that emits the a terminated event of either this Completable
     * or the other Completable whichever fires first.
     * <p>
     * <img width="640" height="484" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.ambWith.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code ambWith} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param other the other Completable, not null. A subscription to this provided source will occur after subscribing
     *            to the current source.
     * @return the new Completable instance
     * @throws NullPointerException if other is null
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Completable ambWith(CompletableSource other) {
        ObjectHelper.requireNonNull(other, "other is null");
        return ambArray(this, other);
    }

    /**
     * Returns an Observable which will subscribe to this Completable and once that is completed then
     * will subscribe to the {@code next} ObservableSource. An error event from this Completable will be
     * propagated to the downstream subscriber and will result in skipping the subscription of the
     * Observable.
     * <p>
     * <img width="640" height="278" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.andThen.o.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code andThen} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the value type of the next ObservableSource
     * @param next the Observable to subscribe after this Completable is completed, not null
     * @return Observable that composes this Completable and next
     * @throws NullPointerException if next is null
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public final <T> Observable<T> andThen(ObservableSource<T> next) {
        ObjectHelper.requireNonNull(next, "next is null");
        return RxJavaPlugins.onAssembly(new CompletableAndThenObservable<T>(this, next));
    }

    /**
     * Returns a Flowable which will subscribe to this Completable and once that is completed then
     * will subscribe to the {@code next} Flowable. An error event from this Completable will be
     * propagated to the downstream subscriber and will result in skipping the subscription of the
     * Publisher.
     * <p>
     * <img width="640" height="249" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.andThen.p.png" alt="">
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The returned {@code Flowable} honors the backpressure of the downstream consumer
     *  and expects the other {@code Publisher} to honor it as well.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code andThen} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the value type of the next Publisher
     * @param next the Publisher to subscribe after this Completable is completed, not null
     * @return Flowable that composes this Completable and next
     * @throws NullPointerException if next is null
     */
    @CheckReturnValue
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerSupport.NONE)
    public final <T> Flowable<T> andThen(Publisher<T> next) {
        ObjectHelper.requireNonNull(next, "next is null");
        return RxJavaPlugins.onAssembly(new CompletableAndThenPublisher<T>(this, next));
    }

    /**
     * Returns a Single which will subscribe to this Completable and once that is completed then
     * will subscribe to the {@code next} SingleSource. An error event from this Completable will be
     * propagated to the downstream subscriber and will result in skipping the subscription of the
     * Single.
     * <p>
     * <img width="640" height="437" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.andThen.s.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code andThen} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T> the value type of the next SingleSource
     * @param next the Single to subscribe after this Completable is completed, not null
     * @return Single that composes this Completable and next
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public final <T> Single<T> andThen(SingleSource<T> next) {
        ObjectHelper.requireNonNull(next, "next is null");
        return RxJavaPlugins.onAssembly(new SingleDelayWithCompletable<T>(next, this));
    }

    /**
     * Returns a {@link Maybe} which will subscribe to this Completable and once that is completed then
     * will subscribe to the {@code next} MaybeSource. An error event from this Completable will be
     * propagated to the downstream subscriber and will result in skipping the subscription of the
     * Maybe.
     * <p>
     * <img width="640" height="280" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.andThen.m.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code andThen} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T> the value type of the next MaybeSource
     * @param next the Maybe to subscribe after this Completable is completed, not null
     * @return Maybe that composes this Completable and next
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public final <T> Maybe<T> andThen(MaybeSource<T> next) {
        ObjectHelper.requireNonNull(next, "next is null");
        return RxJavaPlugins.onAssembly(new MaybeDelayWithCompletable<T>(next, this));
    }

    /**
     * Returns a Completable that first runs this Completable
     * and then the other completable.
     * <p>
     * <img width="640" height="437" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.andThen.c.png" alt="">
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
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Completable andThen(CompletableSource next) {
        return concatWith(next);
    }

    /**
     * Calls the specified converter function during assembly time and returns its resulting value.
     * <p>
     * <img width="640" height="751" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.as.png" alt="">
     * <p>
     * This allows fluent conversion to any other type.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code as} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * <p>History: 2.1.7 - experimental
     * @param <R> the resulting object type
     * @param converter the function that receives the current Completable instance and returns a value
     * @return the converted value
     * @throws NullPointerException if converter is null
     * @since 2.2
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public final <R> R as(@NonNull CompletableConverter<? extends R> converter) {
        return ObjectHelper.requireNonNull(converter, "converter is null").apply(this);
    }

    /**
     * Subscribes to and awaits the termination of this Completable instance in a blocking manner and
     * rethrows any exception emitted.
     * <p>
     * <img width="640" height="432" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.blockingAwait.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code blockingAwait} does not operate by default on a particular {@link Scheduler}.</dd>
     *  <dt><b>Error handling:</b></dt>
     *  <dd>If the source signals an error, the operator wraps a checked {@link Exception}
     *  into {@link RuntimeException} and throws that. Otherwise, {@code RuntimeException}s and
     *  {@link Error}s are rethrown as they are.</dd>
     * </dl>
     * @throws RuntimeException wrapping an InterruptedException if the current thread is interrupted
     */
    @SchedulerSupport(SchedulerSupport.NONE)
    public final void blockingAwait() {
        BlockingMultiObserver<Void> observer = new BlockingMultiObserver<Void>();
        subscribe(observer);
        observer.blockingGet();
    }

    /**
     * Subscribes to and awaits the termination of this Completable instance in a blocking manner
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
     * @return true if the this Completable instance completed normally within the time limit,
     * false if the timeout elapsed before this Completable terminated.
     * @throws RuntimeException wrapping an InterruptedException if the current thread is interrupted
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public final boolean blockingAwait(long timeout, TimeUnit unit) {
        ObjectHelper.requireNonNull(unit, "unit is null");
        BlockingMultiObserver<Void> observer = new BlockingMultiObserver<Void>();
        subscribe(observer);
        return observer.blockingAwait(timeout, unit);
    }

    /**
     * Subscribes to this Completable instance and blocks until it terminates, then returns null or
     * the emitted exception if any.
     * <p>
     * <img width="640" height="435" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.blockingGet.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code blockingGet} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @return the throwable if this terminated with an error, null otherwise
     * @throws RuntimeException that wraps an InterruptedException if the wait is interrupted
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Throwable blockingGet() {
        BlockingMultiObserver<Void> observer = new BlockingMultiObserver<Void>();
        subscribe(observer);
        return observer.blockingGetError();
    }

    /**
     * Subscribes to this Completable instance and blocks until it terminates or the specified timeout
     * elapses, then returns null for normal termination or the emitted exception if any.
     * <p>
     * <img width="640" height="348" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.blockingGet.t.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code blockingGet} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param timeout the timeout value
     * @param unit the time unit
     * @return the throwable if this terminated with an error, null otherwise
     * @throws RuntimeException that wraps an InterruptedException if the wait is interrupted or
     * TimeoutException if the specified timeout elapsed before it
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Throwable blockingGet(long timeout, TimeUnit unit) {
        ObjectHelper.requireNonNull(unit, "unit is null");
        BlockingMultiObserver<Void> observer = new BlockingMultiObserver<Void>();
        subscribe(observer);
        return observer.blockingGetError(timeout, unit);
    }

    /**
     * Subscribes to this Completable only once, when the first CompletableObserver
     * subscribes to the result Completable, caches its terminal event
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
     * @return the new Completable instance
     * @since 2.1
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Completable cache() {
        return RxJavaPlugins.onAssembly(new CompletableCache(this));
    }

    /**
     * Calls the given transformer function with this instance and returns the function's resulting
     * Completable.
     * <p>
     * <img width="640" height="625" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.compose.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code compose} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param transformer the transformer function, not null
     * @return the Completable returned by the function
     * @throws NullPointerException if transformer is null
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Completable compose(CompletableTransformer transformer) {
        return wrap(ObjectHelper.requireNonNull(transformer, "transformer is null").apply(this));
    }

    /**
     * Concatenates this Completable with another Completable.
     * <p>
     * <img width="640" height="317" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.concatWith.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code concatWith} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param other the other Completable, not null
     * @return the new Completable which subscribes to this and then the other Completable
     * @throws NullPointerException if other is null
     * @see #andThen(MaybeSource)
     * @see #andThen(ObservableSource)
     * @see #andThen(SingleSource)
     * @see #andThen(Publisher)
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Completable concatWith(CompletableSource other) {
        ObjectHelper.requireNonNull(other, "other is null");
        return concatArray(this, other);
    }

    /**
     * Returns a Completable which delays the emission of the completion event by the given time.
     * <p>
     * <img width="640" height="343" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.delay.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code delay} does operate by default on the {@code computation} {@link Scheduler}.</dd>
     * </dl>
     * @param delay the delay time
     * @param unit the delay unit
     * @return the new Completable instance
     * @throws NullPointerException if unit is null
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.COMPUTATION)
    public final Completable delay(long delay, TimeUnit unit) {
        return delay(delay, unit, Schedulers.computation(), false);
    }

    /**
     * Returns a Completable which delays the emission of the completion event by the given time while
     * running on the specified scheduler.
     * <p>
     * <img width="640" height="313" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.delay.s.png" alt="">
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
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    public final Completable delay(long delay, TimeUnit unit, Scheduler scheduler) {
        return delay(delay, unit, scheduler, false);
    }

    /**
     * Returns a Completable which delays the emission of the completion event, and optionally the error as well, by the given time while
     * running on the specified scheduler.
     * <p>
     * <img width="640" height="253" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.delay.sb.png" alt="">
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
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    public final Completable delay(final long delay, final TimeUnit unit, final Scheduler scheduler, final boolean delayError) {
        ObjectHelper.requireNonNull(unit, "unit is null");
        ObjectHelper.requireNonNull(scheduler, "scheduler is null");
        return RxJavaPlugins.onAssembly(new CompletableDelay(this, delay, unit, scheduler, delayError));
    }

    /**
     * Returns a Completable which calls the given onComplete callback if this Completable completes.
     * <p>
     * <img width="640" height="304" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.doOnComplete.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code doOnComplete} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param onComplete the callback to call when this emits an onComplete event
     * @return the new Completable instance
     * @throws NullPointerException if onComplete is null
     * @see #doFinally(Action)
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Completable doOnComplete(Action onComplete) {
        return doOnLifecycle(Functions.emptyConsumer(), Functions.emptyConsumer(),
                onComplete, Functions.EMPTY_ACTION,
                Functions.EMPTY_ACTION, Functions.EMPTY_ACTION);
    }

    /**
     * Calls the shared {@code Action} if a CompletableObserver subscribed to the current
     * Completable disposes the common Disposable it received via onSubscribe.
     * <p>
     * <img width="640" height="589" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.doOnDispose.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code doOnDispose} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param onDispose the action to call when the child subscriber disposes the subscription
     * @return the new Completable instance
     * @throws NullPointerException if onDispose is null
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Completable doOnDispose(Action onDispose) {
        return doOnLifecycle(Functions.emptyConsumer(), Functions.emptyConsumer(),
                Functions.EMPTY_ACTION, Functions.EMPTY_ACTION,
                Functions.EMPTY_ACTION, onDispose);
    }

    /**
     * Returns a Completable which calls the given onError callback if this Completable emits an error.
     * <p>
     * <img width="640" height="304" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.doOnError.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code doOnError} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param onError the error callback
     * @return the new Completable instance
     * @throws NullPointerException if onError is null
     * @see #doFinally(Action)
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Completable doOnError(Consumer<? super Throwable> onError) {
        return doOnLifecycle(Functions.emptyConsumer(), onError,
                Functions.EMPTY_ACTION, Functions.EMPTY_ACTION,
                Functions.EMPTY_ACTION, Functions.EMPTY_ACTION);
    }

    /**
     * Returns a Completable which calls the given onEvent callback with the (throwable) for an onError
     * or (null) for an onComplete signal from this Completable before delivering said signal to the downstream.
     * <p>
     * <img width="640" height="305" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.doOnEvent.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code doOnEvent} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param onEvent the event callback
     * @return the new Completable instance
     * @throws NullPointerException if onEvent is null
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Completable doOnEvent(final Consumer<? super Throwable> onEvent) {
        ObjectHelper.requireNonNull(onEvent, "onEvent is null");
        return RxJavaPlugins.onAssembly(new CompletableDoOnEvent(this, onEvent));
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
     * @param onDispose the runnable called when the child disposes the subscription
     * @return the new Completable instance
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    private Completable doOnLifecycle(
            final Consumer<? super Disposable> onSubscribe,
            final Consumer<? super Throwable> onError,
            final Action onComplete,
            final Action onTerminate,
            final Action onAfterTerminate,
            final Action onDispose) {
        ObjectHelper.requireNonNull(onSubscribe, "onSubscribe is null");
        ObjectHelper.requireNonNull(onError, "onError is null");
        ObjectHelper.requireNonNull(onComplete, "onComplete is null");
        ObjectHelper.requireNonNull(onTerminate, "onTerminate is null");
        ObjectHelper.requireNonNull(onAfterTerminate, "onAfterTerminate is null");
        ObjectHelper.requireNonNull(onDispose, "onDispose is null");
        return RxJavaPlugins.onAssembly(new CompletablePeek(this, onSubscribe, onError, onComplete, onTerminate, onAfterTerminate, onDispose));
    }

    /**
     * Returns a Completable instance that calls the given onSubscribe callback with the disposable
     * that child subscribers receive on subscription.
     * <p>
     * <img width="640" height="304" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.doOnSubscribe.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code doOnSubscribe} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param onSubscribe the callback called when a child subscriber subscribes
     * @return the new Completable instance
     * @throws NullPointerException if onSubscribe is null
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Completable doOnSubscribe(Consumer<? super Disposable> onSubscribe) {
        return doOnLifecycle(onSubscribe, Functions.emptyConsumer(),
                Functions.EMPTY_ACTION, Functions.EMPTY_ACTION,
                Functions.EMPTY_ACTION, Functions.EMPTY_ACTION);
    }

    /**
     * Returns a Completable instance that calls the given onTerminate callback just before this Completable
     * completes normally or with an exception.
     * <p>
     * <img width="640" height="304" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.doOnTerminate.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code doOnTerminate} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param onTerminate the callback to call just before this Completable terminates
     * @return the new Completable instance
     * @see #doFinally(Action)
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Completable doOnTerminate(final Action onTerminate) {
        return doOnLifecycle(Functions.emptyConsumer(), Functions.emptyConsumer(),
                Functions.EMPTY_ACTION, onTerminate,
                Functions.EMPTY_ACTION, Functions.EMPTY_ACTION);
    }

    /**
     * Returns a Completable instance that calls the given onTerminate callback after this Completable
     * completes normally or with an exception.
     * <p>
     * <img width="640" height="304" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.doAfterTerminate.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code doAfterTerminate} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param onAfterTerminate the callback to call after this Completable terminates
     * @return the new Completable instance
     * @see #doFinally(Action)
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Completable doAfterTerminate(final Action onAfterTerminate) {
        return doOnLifecycle(
                Functions.emptyConsumer(),
                Functions.emptyConsumer(),
                Functions.EMPTY_ACTION,
                Functions.EMPTY_ACTION,
                onAfterTerminate,
                Functions.EMPTY_ACTION);
    }
    /**
     * Calls the specified action after this Completable signals onError or onComplete or gets disposed by
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
     * @param onFinally the action called when this Completable terminates or gets cancelled
     * @return the new Completable instance
     * @since 2.1
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Completable doFinally(Action onFinally) {
        ObjectHelper.requireNonNull(onFinally, "onFinally is null");
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
     *     public void onSubscribe(Disposable s) {
     *         if (upstream != null) {
     *             s.cancel();
     *         } else {
     *             upstream = s;
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
     * requires a non-null {@code CompletableObserver} instance to be returned, which is then unconditionally subscribed to
     * the upstream {@code Completable}. For example, if the operator decided there is no reason to subscribe to the
     * upstream source because of some optimization possibility or a failure to prepare the operator, it still has to
     * return a {@code CompletableObserver} that should immediately dispose the upstream's {@code Disposable} in its
     * {@code onSubscribe} method. Again, using a {@code CompletableTransformer} and extending the {@code Completable} is
     * a better option as {@link #subscribeActual} can decide to not subscribe to its upstream after all.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code lift} does not operate by default on a particular {@link Scheduler}, however, the
     *  {@link CompletableOperator} may use a {@code Scheduler} to support its own asynchronous behavior.</dd>
     * </dl>
     *
     * @param onLift the {@link CompletableOperator} that receives the downstream's {@code CompletableObserver} and should return
     *               a {@code CompletableObserver} with custom behavior to be used as the consumer for the current
     *               {@code Completable}.
     * @return the new Completable instance
     * @see <a href="https://github.com/ReactiveX/RxJava/wiki/Writing-operators-for-2.0">RxJava wiki: Writing operators</a>
     * @see #compose(CompletableTransformer)
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Completable lift(final CompletableOperator onLift) {
        ObjectHelper.requireNonNull(onLift, "onLift is null");
        return RxJavaPlugins.onAssembly(new CompletableLift(this, onLift));
    }

    /**
     * Returns a Completable which subscribes to this and the other Completable and completes
     * when both of them complete or one emits an error.
     * <p>
     * <img width="640" height="442" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.mergeWith.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code mergeWith} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param other the other Completable instance
     * @return the new Completable instance
     * @throws NullPointerException if other is null
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Completable mergeWith(CompletableSource other) {
        ObjectHelper.requireNonNull(other, "other is null");
        return mergeArray(this, other);
    }

    /**
     * Returns a Completable which emits the terminal events from the thread of the specified scheduler.
     * <p>
     * <img width="640" height="523" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.observeOn.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code observeOn} operates on a {@link Scheduler} you specify.</dd>
     * </dl>
     * @param scheduler the scheduler to emit terminal events on
     * @return the new Completable instance
     * @throws NullPointerException if scheduler is null
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    public final Completable observeOn(final Scheduler scheduler) {
        ObjectHelper.requireNonNull(scheduler, "scheduler is null");
        return RxJavaPlugins.onAssembly(new CompletableObserveOn(this, scheduler));
    }

    /**
     * Returns a Completable instance that if this Completable emits an error, it will emit an onComplete
     * and swallow the throwable.
     * <p>
     * <img width="640" height="585" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.onErrorComplete.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code onErrorComplete} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @return the new Completable instance
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Completable onErrorComplete() {
        return onErrorComplete(Functions.alwaysTrue());
    }

    /**
     * Returns a Completable instance that if this Completable emits an error and the predicate returns
     * true, it will emit an onComplete and swallow the throwable.
     * <p>
     * <img width="640" height="283" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.onErrorComplete.f.png" alt="">
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
    public final Completable onErrorComplete(final Predicate<? super Throwable> predicate) {
        ObjectHelper.requireNonNull(predicate, "predicate is null");

        return RxJavaPlugins.onAssembly(new CompletableOnErrorComplete(this, predicate));
    }

    /**
     * Returns a Completable instance that when encounters an error from this Completable, calls the
     * specified mapper function that returns another Completable instance for it and resumes the
     * execution with it.
     * <p>
     * <img width="640" height="426" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.onErrorResumeNext.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code onErrorResumeNext} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param errorMapper the mapper function that takes the error and should return a Completable as
     * continuation.
     * @return the new Completable instance
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Completable onErrorResumeNext(final Function<? super Throwable, ? extends CompletableSource> errorMapper) {
        ObjectHelper.requireNonNull(errorMapper, "errorMapper is null");
        return RxJavaPlugins.onAssembly(new CompletableResumeNext(this, errorMapper));
    }

    /**
     * Nulls out references to the upstream producer and downstream CompletableObserver if
     * the sequence is terminated or downstream calls dispose().
     * <p>
     * <img width="640" height="326" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.onTerminateDetach.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code onTerminateDetach} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * <p>History: 2.1.5 - experimental
     * @return a Completable which nulls out references to the upstream producer and downstream CompletableObserver if
     * the sequence is terminated or downstream calls dispose()
     * @since 2.2
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Completable onTerminateDetach() {
        return RxJavaPlugins.onAssembly(new CompletableDetach(this));
    }

    /**
     * Returns a Completable that repeatedly subscribes to this Completable until cancelled.
     * <p>
     * <img width="640" height="373" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.repeat.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code repeat} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @return the new Completable instance
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Completable repeat() {
        return fromPublisher(toFlowable().repeat());
    }

    /**
     * Returns a Completable that subscribes repeatedly at most the given times to this Completable.
     * <p>
     * <img width="640" height="408" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.repeat.n.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code repeat} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param times the number of times the resubscription should happen
     * @return the new Completable instance
     * @throws IllegalArgumentException if times is less than zero
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Completable repeat(long times) {
        return fromPublisher(toFlowable().repeat(times));
    }

    /**
     * Returns a Completable that repeatedly subscribes to this Completable so long as the given
     * stop supplier returns false.
     * <p>
     * <img width="640" height="381" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.repeatUntil.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code repeatUntil} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param stop the supplier that should return true to stop resubscribing.
     * @return the new Completable instance
     * @throws NullPointerException if stop is null
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Completable repeatUntil(BooleanSupplier stop) {
        return fromPublisher(toFlowable().repeatUntil(stop));
    }

    /**
     * Returns a Completable instance that repeats when the Publisher returned by the handler
     * emits an item or completes when this Publisher emits a completed event.
     * <p>
     * <img width="640" height="586" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.repeatWhen.png" alt="">
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
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Completable repeatWhen(Function<? super Flowable<Object>, ? extends Publisher<?>> handler) {
        return fromPublisher(toFlowable().repeatWhen(handler));
    }

    /**
     * Returns a Completable that retries this Completable as long as it emits an onError event.
     * <p>
     * <img width="640" height="368" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.retry.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code retry} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @return the new Completable instance
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Completable retry() {
        return fromPublisher(toFlowable().retry());
    }

    /**
     * Returns a Completable that retries this Completable in case of an error as long as the predicate
     * returns true.
     * <p>
     * <img width="640" height="325" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.retry.ff.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code retry} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param predicate the predicate called when this emits an error with the repeat count and the latest exception
     * and should return true to retry.
     * @return the new Completable instance
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Completable retry(BiPredicate<? super Integer, ? super Throwable> predicate) {
        return fromPublisher(toFlowable().retry(predicate));
    }

    /**
     * Returns a Completable that when this Completable emits an error, retries at most the given
     * number of times before giving up and emitting the last error.
     * <p>
     * <img width="640" height="451" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.retry.n.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code retry} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param times the number of times the returned Completable should retry this Completable
     * @return the new Completable instance
     * @throws IllegalArgumentException if times is negative
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Completable retry(long times) {
        return fromPublisher(toFlowable().retry(times));
    }

    /**
     * Returns a Completable that when this Completable emits an error, retries at most times
     * or until the predicate returns false, whichever happens first and emitting the last error.
     * <p>
     * <img width="640" height="361" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.retry.nf.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code retry} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * <p>History: 2.1.8 - experimental
     * @param times the number of times the returned Completable should retry this Completable
     * @param predicate the predicate that is called with the latest throwable and should return
     * true to indicate the returned Completable should resubscribe to this Completable.
     * @return the new Completable instance
     * @throws NullPointerException if predicate is null
     * @throws IllegalArgumentException if times is negative
     * @since 2.2
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Completable retry(long times, Predicate<? super Throwable> predicate) {
        return fromPublisher(toFlowable().retry(times, predicate));
    }

    /**
     * Returns a Completable that when this Completable emits an error, calls the given predicate with
     * the latest exception to decide whether to resubscribe to this or not.
     * <p>
     * <img width="640" height="336" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.retry.f.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code retry} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param predicate the predicate that is called with the latest throwable and should return
     * true to indicate the returned Completable should resubscribe to this Completable.
     * @return the new Completable instance
     * @throws NullPointerException if predicate is null
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Completable retry(Predicate<? super Throwable> predicate) {
        return fromPublisher(toFlowable().retry(predicate));
    }

    /**
     * Returns a Completable which given a Publisher and when this Completable emits an error, delivers
     * that error through a Flowable and the Publisher should signal a value indicating a retry in response
     * or a terminal event indicating a termination.
     * <p>
     * <img width="640" height="586" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.retryWhen.png" alt="">
     * <p>
     * Note that the inner {@code Publisher} returned by the handler function should signal
     * either {@code onNext}, {@code onError} or {@code onComplete} in response to the received
     * {@code Throwable} to indicate the operator should retry or terminate. If the upstream to
     * the operator is asynchronous, signalling onNext followed by onComplete immediately may
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
     * @param handler the handler that receives a Flowable delivering Throwables and should return a Publisher that
     * emits items to indicate retries or emits terminal events to indicate termination.
     * @return the new Completable instance
     * @throws NullPointerException if handler is null
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Completable retryWhen(Function<? super Flowable<Throwable>, ? extends Publisher<?>> handler) {
        return fromPublisher(toFlowable().retryWhen(handler));
    }

    /**
     * Returns a Completable which first runs the other Completable
     * then this completable if the other completed normally.
     * <p>
     * <img width="640" height="437" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.startWith.c.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code startWith} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param other the other completable to run first
     * @return the new Completable instance
     * @throws NullPointerException if other is null
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Completable startWith(CompletableSource other) {
        ObjectHelper.requireNonNull(other, "other is null");
        return concatArray(other, this);
    }

    /**
     * Returns an Observable which first delivers the events
     * of the other Observable then runs this CompletableConsumable.
     * <p>
     * <img width="640" height="289" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.startWith.o.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code startWith} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the value type
     * @param other the other Observable to run first
     * @return the new Observable instance
     * @throws NullPointerException if other is null
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public final <T> Observable<T> startWith(Observable<T> other) {
        ObjectHelper.requireNonNull(other, "other is null");
        return other.concatWith(this.<T>toObservable());
    }
    /**
     * Returns a Flowable which first delivers the events
     * of the other Publisher then runs this Completable.
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
     * @param other the other Publisher to run first
     * @return the new Flowable instance
     * @throws NullPointerException if other is null
     */
    @CheckReturnValue
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerSupport.NONE)
    public final <T> Flowable<T> startWith(Publisher<T> other) {
        ObjectHelper.requireNonNull(other, "other is null");
        return this.<T>toFlowable().startWith(other);
    }

    /**
     * Hides the identity of this Completable and its Disposable.
     * <p>
     * <img width="640" height="432" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.hide.png" alt="">
     * <p>
     * Allows preventing certain identity-based optimizations (fusion).
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code hide} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * <p>History: 2.0.5 - experimental
     * @return the new Completable instance
     * @since 2.1
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Completable hide() {
        return RxJavaPlugins.onAssembly(new CompletableHide(this));
    }

    /**
     * Subscribes to this CompletableConsumable and returns a Disposable which can be used to cancel
     * the subscription.
     * <p>
     * <img width="640" height="352" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.subscribe.png" alt="">
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
        ObjectHelper.requireNonNull(s, "s is null");
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
     * Implement this method to handle the incoming {@link CompletableObserver}s and
     * perform the business logic in your operator.
     * <p>There is no need to call any of the plugin hooks on the current {@code Completable} instance or
     * the {@code CompletableObserver}; all hooks and basic safeguards have been
     * applied by {@link #subscribe(CompletableObserver)} before this method gets called.
     * @param s the CompletableObserver instance, never null
     */
    protected abstract void subscribeActual(CompletableObserver s);

    /**
     * Subscribes a given CompletableObserver (subclass) to this Completable and returns the given
     * CompletableObserver as is.
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
     * @param <E> the type of the CompletableObserver to use and return
     * @param observer the CompletableObserver (subclass) to use and return, not null
     * @return the input {@code observer}
     * @throws NullPointerException if {@code observer} is null
     * @since 2.0
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public final <E extends CompletableObserver> E subscribeWith(E observer) {
        subscribe(observer);
        return observer;
    }

    /**
     * Subscribes to this Completable and calls back either the onError or onComplete functions.
     * <p>
     * <img width="640" height="352" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.subscribe.ff.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code subscribe} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param onComplete the runnable that is called if the Completable completes normally
     * @param onError the consumer that is called if this Completable emits an error
     * @return the Disposable that can be used for cancelling the subscription asynchronously
     * @throws NullPointerException if either callback is null
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Disposable subscribe(final Action onComplete, final Consumer<? super Throwable> onError) {
        ObjectHelper.requireNonNull(onError, "onError is null");
        ObjectHelper.requireNonNull(onComplete, "onComplete is null");

        CallbackCompletableObserver s = new CallbackCompletableObserver(onError, onComplete);
        subscribe(s);
        return s;
    }

    /**
     * Subscribes to this Completable and calls the given Action when this Completable
     * completes normally.
     * <p>
     * <img width="640" height="352" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.subscribe.f.png" alt="">
     * <p>
     * If the Completable emits an error, it is wrapped into an
     * {@link io.reactivex.exceptions.OnErrorNotImplementedException OnErrorNotImplementedException}
     * and routed to the RxJavaPlugins.onError handler.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code subscribe} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param onComplete the runnable called when this Completable completes normally
     * @return the Disposable that allows cancelling the subscription
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Disposable subscribe(final Action onComplete) {
        ObjectHelper.requireNonNull(onComplete, "onComplete is null");

        CallbackCompletableObserver s = new CallbackCompletableObserver(onComplete);
        subscribe(s);
        return s;
    }

    /**
     * Returns a Completable which subscribes the child subscriber on the specified scheduler, making
     * sure the subscription side-effects happen on that specific thread of the scheduler.
     * <p>
     * <img width="640" height="686" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.subscribeOn.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code subscribeOn} operates on a {@link Scheduler} you specify.</dd>
     * </dl>
     * @param scheduler the Scheduler to subscribe on
     * @return the new Completable instance
     * @throws NullPointerException if scheduler is null
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    public final Completable subscribeOn(final Scheduler scheduler) {
        ObjectHelper.requireNonNull(scheduler, "scheduler is null");

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
     * @return the new Completable instance
     * @since 2.2
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Completable takeUntil(CompletableSource other) {
        ObjectHelper.requireNonNull(other, "other is null");

        return RxJavaPlugins.onAssembly(new CompletableTakeUntilCompletable(this, other));
    }

    /**
     * Returns a Completable that runs this Completable and emits a TimeoutException in case
     * this Completable doesn't complete within the given time.
     * <p>
     * <img width="640" height="348" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.timeout.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code timeout} signals the TimeoutException on the {@code computation} {@link Scheduler}.</dd>
     * </dl>
     * @param timeout the timeout value
     * @param unit the timeout unit
     * @return the new Completable instance
     * @throws NullPointerException if unit is null
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.COMPUTATION)
    public final Completable timeout(long timeout, TimeUnit unit) {
        return timeout0(timeout, unit, Schedulers.computation(), null);
    }

    /**
     * Returns a Completable that runs this Completable and switches to the other Completable
     * in case this Completable doesn't complete within the given time.
     * <p>
     * <img width="640" height="308" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.timeout.c.png" alt="">
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
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.COMPUTATION)
    public final Completable timeout(long timeout, TimeUnit unit, CompletableSource other) {
        ObjectHelper.requireNonNull(other, "other is null");
        return timeout0(timeout, unit, Schedulers.computation(), other);
    }

    /**
     * Returns a Completable that runs this Completable and emits a TimeoutException in case
     * this Completable doesn't complete within the given time while "waiting" on the specified
     * Scheduler.
     * <p>
     * <img width="640" height="348" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.timeout.s.png" alt="">
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
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    public final Completable timeout(long timeout, TimeUnit unit, Scheduler scheduler) {
        return timeout0(timeout, unit, scheduler, null);
    }

    /**
     * Returns a Completable that runs this Completable and switches to the other Completable
     * in case this Completable doesn't complete within the given time while "waiting" on
     * the specified scheduler.
     * <p>
     * <img width="640" height="308" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.timeout.sc.png" alt="">
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
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    public final Completable timeout(long timeout, TimeUnit unit, Scheduler scheduler, CompletableSource other) {
        ObjectHelper.requireNonNull(other, "other is null");
        return timeout0(timeout, unit, scheduler, other);
    }

    /**
     * Returns a Completable that runs this Completable and optionally switches to the other Completable
     * in case this Completable doesn't complete within the given time while "waiting" on
     * the specified scheduler.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>You specify the {@link Scheduler} this operator runs on.</dd>
     * </dl>
     * @param timeout the timeout value
     * @param unit the timeout unit
     * @param scheduler the scheduler to use to wait for completion
     * @param other the other Completable instance to switch to in case of a timeout,
     * if null a TimeoutException is emitted instead
     * @return the new Completable instance
     * @throws NullPointerException if unit or scheduler
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    private Completable timeout0(long timeout, TimeUnit unit, Scheduler scheduler, CompletableSource other) {
        ObjectHelper.requireNonNull(unit, "unit is null");
        ObjectHelper.requireNonNull(scheduler, "scheduler is null");
        return RxJavaPlugins.onAssembly(new CompletableTimeout(this, timeout, unit, scheduler, other));
    }

    /**
     * Allows fluent conversion to another type via a function callback.
     * <p>
     * <img width="640" height="751" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.to.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code to} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <U> the output type
     * @param converter the function called with this which should return some other value.
     * @return the converted value
     * @throws NullPointerException if converter is null
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public final <U> U to(Function<? super Completable, U> converter) {
        try {
            return ObjectHelper.requireNonNull(converter, "converter is null").apply(this);
        } catch (Throwable ex) {
            Exceptions.throwIfFatal(ex);
            throw ExceptionHelper.wrapOrThrow(ex);
        }
    }

    /**
     * Returns a Flowable which when subscribed to subscribes to this Completable and
     * relays the terminal events to the subscriber.
     * <p>
     * <img width="640" height="585" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.toFlowable.png" alt="">
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The returned {@code Flowable} honors the backpressure of the downstream consumer.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code toFlowable} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the value type
     * @return the new Flowable instance
     */
    @CheckReturnValue
    @SuppressWarnings("unchecked")
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerSupport.NONE)
    public final <T> Flowable<T> toFlowable() {
        if (this instanceof FuseToFlowable) {
            return ((FuseToFlowable<T>)this).fuseToFlowable();
        }
        return RxJavaPlugins.onAssembly(new CompletableToFlowable<T>(this));
    }

    /**
     * Converts this Completable into a {@link Maybe}.
     * <p>
     * <img width="640" height="585" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.toMaybe.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code toMaybe} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T> the value type
     * @return a {@link Maybe} that only calls {@code onComplete} or {@code onError}, based on which one is
     *         called by the source Completable.
     */
    @CheckReturnValue
    @SuppressWarnings("unchecked")
    @SchedulerSupport(SchedulerSupport.NONE)
    public final <T> Maybe<T> toMaybe() {
        if (this instanceof FuseToMaybe) {
            return ((FuseToMaybe<T>)this).fuseToMaybe();
        }
        return RxJavaPlugins.onAssembly(new MaybeFromCompletable<T>(this));
    }

    /**
     * Returns an Observable which when subscribed to subscribes to this Completable and
     * relays the terminal events to the subscriber.
     * <p>
     * <img width="640" height="293" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.toObservable.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code toObservable} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the value type
     * @return the new Observable created
     */
    @CheckReturnValue
    @SuppressWarnings("unchecked")
    @SchedulerSupport(SchedulerSupport.NONE)
    public final <T> Observable<T> toObservable() {
        if (this instanceof FuseToObservable) {
            return ((FuseToObservable<T>)this).fuseToObservable();
        }
        return RxJavaPlugins.onAssembly(new CompletableToObservable<T>(this));
    }

    /**
     * Converts this Completable into a Single which when this Completable completes normally,
     * calls the given supplier and emits its returned value through onSuccess.
     * <p>
     * <img width="640" height="583" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.toSingle.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code toSingle} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the value type
     * @param completionValueSupplier the value supplier called when this Completable completes normally
     * @return the new Single instance
     * @throws NullPointerException if completionValueSupplier is null
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public final <T> Single<T> toSingle(final Callable<? extends T> completionValueSupplier) {
        ObjectHelper.requireNonNull(completionValueSupplier, "completionValueSupplier is null");
        return RxJavaPlugins.onAssembly(new CompletableToSingle<T>(this, completionValueSupplier, null));
    }

    /**
     * Converts this Completable into a Single which when this Completable completes normally,
     * emits the given value through onSuccess.
     * <p>
     * <img width="640" height="583" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.toSingleDefault.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code toSingleDefault} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the value type
     * @param completionValue the value to emit when this Completable completes normally
     * @return the new Single instance
     * @throws NullPointerException if completionValue is null
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public final <T> Single<T> toSingleDefault(final T completionValue) {
        ObjectHelper.requireNonNull(completionValue, "completionValue is null");
        return RxJavaPlugins.onAssembly(new CompletableToSingle<T>(this, null, completionValue));
    }

    /**
     * Returns a Completable which makes sure when a subscriber cancels the subscription, the
     * dispose is called on the specified scheduler.
     * <p>
     * <img width="640" height="716" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.unsubscribeOn.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code unsubscribeOn} calls dispose() of the upstream on the {@link Scheduler} you specify.</dd>
     * </dl>
     * @param scheduler the target scheduler where to execute the cancellation
     * @return the new Completable instance
     * @throws NullPointerException if scheduler is null
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    public final Completable unsubscribeOn(final Scheduler scheduler) {
        ObjectHelper.requireNonNull(scheduler, "scheduler is null");
        return RxJavaPlugins.onAssembly(new CompletableDisposeOn(this, scheduler));
    }
    // -------------------------------------------------------------------------
    // Fluent test support, super handy and reduces test preparation boilerplate
    // -------------------------------------------------------------------------

    /**
     * Creates a TestObserver and subscribes
     * it to this Completable.
     * <p>
     * <img width="640" height="458" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.test.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code test} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @return the new TestObserver instance
     * @since 2.0
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public final TestObserver<Void> test() {
        TestObserver<Void> to = new TestObserver<Void>();
        subscribe(to);
        return to;
    }

    /**
     * Creates a TestObserver optionally in cancelled state, then subscribes it to this Completable.
     * @param cancelled if true, the TestObserver will be cancelled before subscribing to this
     * Completable.
     * <p>
     * <img width="640" height="499" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Completable.test.b.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code test} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @return the new TestObserver instance
     * @since 2.0
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public final TestObserver<Void> test(boolean cancelled) {
        TestObserver<Void> to = new TestObserver<Void>();

        if (cancelled) {
            to.cancel();
        }
        subscribe(to);
        return to;
    }
}
