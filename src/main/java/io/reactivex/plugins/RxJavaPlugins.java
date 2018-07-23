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
package io.reactivex.plugins;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.concurrent.*;

import org.reactivestreams.Subscriber;

import io.reactivex.*;
import io.reactivex.annotations.*;
import io.reactivex.exceptions.*;
import io.reactivex.flowables.ConnectableFlowable;
import io.reactivex.functions.*;
import io.reactivex.internal.functions.ObjectHelper;
import io.reactivex.internal.schedulers.*;
import io.reactivex.internal.util.ExceptionHelper;
import io.reactivex.observables.ConnectableObservable;
import io.reactivex.parallel.ParallelFlowable;
import io.reactivex.schedulers.Schedulers;
/**
 * Utility class to inject handlers to certain standard RxJava operations.
 */
public final class RxJavaPlugins {
    @Nullable
    static volatile Consumer<? super Throwable> errorHandler;

    @Nullable
    static volatile Function<? super Runnable, ? extends Runnable> onScheduleHandler;

    @Nullable
    static volatile Function<? super Callable<Scheduler>, ? extends Scheduler> onInitComputationHandler;

    @Nullable
    static volatile Function<? super Callable<Scheduler>, ? extends Scheduler> onInitSingleHandler;

    @Nullable
    static volatile Function<? super Callable<Scheduler>, ? extends Scheduler> onInitIoHandler;

    @Nullable
    static volatile Function<? super Callable<Scheduler>, ? extends Scheduler> onInitNewThreadHandler;

    @Nullable
    static volatile Function<? super Scheduler, ? extends Scheduler> onComputationHandler;

    @Nullable
    static volatile Function<? super Scheduler, ? extends Scheduler> onSingleHandler;

    @Nullable
    static volatile Function<? super Scheduler, ? extends Scheduler> onIoHandler;

    @Nullable
    static volatile Function<? super Scheduler, ? extends Scheduler> onNewThreadHandler;

    @SuppressWarnings("rawtypes")
    @Nullable
    static volatile Function<? super Flowable, ? extends Flowable> onFlowableAssembly;

    @SuppressWarnings("rawtypes")
    @Nullable
    static volatile Function<? super ConnectableFlowable, ? extends ConnectableFlowable> onConnectableFlowableAssembly;

    @SuppressWarnings("rawtypes")
    @Nullable
    static volatile Function<? super Observable, ? extends Observable> onObservableAssembly;

    @SuppressWarnings("rawtypes")
    @Nullable
    static volatile Function<? super ConnectableObservable, ? extends ConnectableObservable> onConnectableObservableAssembly;

    @SuppressWarnings("rawtypes")
    @Nullable
    static volatile Function<? super Maybe, ? extends Maybe> onMaybeAssembly;

    @SuppressWarnings("rawtypes")
    @Nullable
    static volatile Function<? super Single, ? extends Single> onSingleAssembly;

    @Nullable
    static volatile Function<? super Completable, ? extends Completable> onCompletableAssembly;

    @SuppressWarnings("rawtypes")
    @Nullable
    static volatile Function<? super ParallelFlowable, ? extends ParallelFlowable> onParallelAssembly;

    @SuppressWarnings("rawtypes")
    @Nullable
    static volatile BiFunction<? super Flowable, ? super Subscriber, ? extends Subscriber> onFlowableSubscribe;

    @SuppressWarnings("rawtypes")
    @Nullable
    static volatile BiFunction<? super Maybe, ? super MaybeObserver, ? extends MaybeObserver> onMaybeSubscribe;

    @SuppressWarnings("rawtypes")
    @Nullable
    static volatile BiFunction<? super Observable, ? super Observer, ? extends Observer> onObservableSubscribe;

    @SuppressWarnings("rawtypes")
    @Nullable
    static volatile BiFunction<? super Single, ? super SingleObserver, ? extends SingleObserver> onSingleSubscribe;

    @Nullable
    static volatile BiFunction<? super Completable, ? super CompletableObserver, ? extends CompletableObserver> onCompletableSubscribe;

    @Nullable
    static volatile BooleanSupplier onBeforeBlocking;

    /** Prevents changing the plugins. */
    static volatile boolean lockdown;

    /**
     * If true, attempting to run a blockingX operation on a (by default)
     * computation or single scheduler will throw an IllegalStateException.
     */
    static volatile boolean failNonBlockingScheduler;

    /**
     * Prevents changing the plugins from then on.
     * <p>This allows container-like environments to prevent clients
     * messing with plugins.
     */
    public static void lockdown() {
        lockdown = true;
    }

    /**
     * Returns true if the plugins were locked down.
     * @return true if the plugins were locked down
     */
    public static boolean isLockdown() {
        return lockdown;
    }

    /**
     * Enables or disables the blockingX operators to fail
     * with an IllegalStateException on a non-blocking
     * scheduler such as computation or single.
     * <p>History: 2.0.5 - experimental
     * @param enable enable or disable the feature
     * @since 2.1
     */
    public static void setFailOnNonBlockingScheduler(boolean enable) {
        if (lockdown) {
            throw new IllegalStateException("Plugins can't be changed anymore");
        }
        failNonBlockingScheduler = enable;
    }

    /**
     * Returns true if the blockingX operators fail
     * with an IllegalStateException on a non-blocking scheduler
     * such as computation or single.
     * <p>History: 2.0.5 - experimental
     * @return true if the blockingX operators fail on a non-blocking scheduler
     * @since 2.1
     */
    public static boolean isFailOnNonBlockingScheduler() {
        return failNonBlockingScheduler;
    }

    /**
     * Returns the current hook function.
     * @return the hook function, may be null
     */
    @Nullable
    public static Function<? super Scheduler, ? extends Scheduler> getComputationSchedulerHandler() {
        return onComputationHandler;
    }

    /**
     * Returns the a hook consumer.
     * @return the hook consumer, may be null
     */
    @Nullable
    public static Consumer<? super Throwable> getErrorHandler() {
        return errorHandler;
    }

    /**
     * Returns the current hook function.
     * @return the hook function, may be null
     */
    @Nullable
    public static Function<? super Callable<Scheduler>, ? extends Scheduler> getInitComputationSchedulerHandler() {
        return onInitComputationHandler;
    }

    /**
     * Returns the current hook function.
     * @return the hook function, may be null
     */
    @Nullable
    public static Function<? super Callable<Scheduler>, ? extends Scheduler> getInitIoSchedulerHandler() {
        return onInitIoHandler;
    }

    /**
     * Returns the current hook function.
     * @return the hook function, may be null
     */
    @Nullable
    public static Function<? super Callable<Scheduler>, ? extends Scheduler> getInitNewThreadSchedulerHandler() {
        return onInitNewThreadHandler;
    }

    /**
     * Returns the current hook function.
     * @return the hook function, may be null
     */
    @Nullable
    public static Function<? super Callable<Scheduler>, ? extends Scheduler> getInitSingleSchedulerHandler() {
        return onInitSingleHandler;
    }

    /**
     * Returns the current hook function.
     * @return the hook function, may be null
     */
    @Nullable
    public static Function<? super Scheduler, ? extends Scheduler> getIoSchedulerHandler() {
        return onIoHandler;
    }

    /**
     * Returns the current hook function.
     * @return the hook function, may be null
     */
    @Nullable
    public static Function<? super Scheduler, ? extends Scheduler> getNewThreadSchedulerHandler() {
        return onNewThreadHandler;
    }

    /**
     * Returns the current hook function.
     * @return the hook function, may be null
     */
    @Nullable
    public static Function<? super Runnable, ? extends Runnable> getScheduleHandler() {
        return onScheduleHandler;
    }

    /**
     * Returns the current hook function.
     * @return the hook function, may be null
     */
    @Nullable
    public static Function<? super Scheduler, ? extends Scheduler> getSingleSchedulerHandler() {
        return onSingleHandler;
    }

    /**
     * Calls the associated hook function.
     * @param defaultScheduler a {@link Callable} which returns the hook's input value
     * @return the value returned by the hook, not null
     * @throws NullPointerException if the callable parameter or its result are null
     */
    @NonNull
    public static Scheduler initComputationScheduler(@NonNull Callable<Scheduler> defaultScheduler) {
        ObjectHelper.requireNonNull(defaultScheduler, "Scheduler Callable can't be null");
        Function<? super Callable<Scheduler>, ? extends Scheduler> f = onInitComputationHandler;
        if (f == null) {
            return callRequireNonNull(defaultScheduler);
        }
        return applyRequireNonNull(f, defaultScheduler); // JIT will skip this
    }

    /**
     * Calls the associated hook function.
     * @param defaultScheduler a {@link Callable} which returns the hook's input value
     * @return the value returned by the hook, not null
     * @throws NullPointerException if the callable parameter or its result are null
     */
    @NonNull
    public static Scheduler initIoScheduler(@NonNull Callable<Scheduler> defaultScheduler) {
        ObjectHelper.requireNonNull(defaultScheduler, "Scheduler Callable can't be null");
        Function<? super Callable<Scheduler>, ? extends Scheduler> f = onInitIoHandler;
        if (f == null) {
            return callRequireNonNull(defaultScheduler);
        }
        return applyRequireNonNull(f, defaultScheduler);
    }

    /**
     * Calls the associated hook function.
     * @param defaultScheduler a {@link Callable} which returns the hook's input value
     * @return the value returned by the hook, not null
     * @throws NullPointerException if the callable parameter or its result are null
     */
    @NonNull
    public static Scheduler initNewThreadScheduler(@NonNull Callable<Scheduler> defaultScheduler) {
        ObjectHelper.requireNonNull(defaultScheduler, "Scheduler Callable can't be null");
        Function<? super Callable<Scheduler>, ? extends Scheduler> f = onInitNewThreadHandler;
        if (f == null) {
            return callRequireNonNull(defaultScheduler);
        }
        return applyRequireNonNull(f, defaultScheduler);
    }

    /**
     * Calls the associated hook function.
     * @param defaultScheduler a {@link Callable} which returns the hook's input value
     * @return the value returned by the hook, not null
     * @throws NullPointerException if the callable parameter or its result are null
     */
    @NonNull
    public static Scheduler initSingleScheduler(@NonNull Callable<Scheduler> defaultScheduler) {
        ObjectHelper.requireNonNull(defaultScheduler, "Scheduler Callable can't be null");
        Function<? super Callable<Scheduler>, ? extends Scheduler> f = onInitSingleHandler;
        if (f == null) {
            return callRequireNonNull(defaultScheduler);
        }
        return applyRequireNonNull(f, defaultScheduler);
    }

    /**
     * Calls the associated hook function.
     * @param defaultScheduler the hook's input value
     * @return the value returned by the hook
     */
    @NonNull
    public static Scheduler onComputationScheduler(@NonNull Scheduler defaultScheduler) {
        Function<? super Scheduler, ? extends Scheduler> f = onComputationHandler;
        if (f == null) {
            return defaultScheduler;
        }
        return apply(f, defaultScheduler);
    }

    /**
     * Called when an undeliverable error occurs.
     * <p>
     * Undeliverable errors are those {@code Observer.onError()} invocations that are not allowed to happen on
     * the given consumer type ({@code Observer}, {@code Subscriber}, etc.) due to protocol restrictions
     * because the consumer has either disposed/cancelled its {@code Disposable}/{@code Subscription} or
     * has already terminated with an {@code onError()} or {@code onComplete()} signal.
     * <p>
     * By default, this global error handler prints the stacktrace via {@link Throwable#printStackTrace()}
     * and calls {@link java.lang.Thread.UncaughtExceptionHandler#uncaughtException(Thread, Throwable)}
     * on the current thread.
     * <p>
     * Note that on some platforms, the platform runtime terminates the current application with an error if such
     * uncaught exceptions happen. In this case, it is recommended the application installs a global error
     * handler via the {@link #setErrorHandler(Consumer)} plugin method.
     *
     * @param error the error to report
     * @see #getErrorHandler()
     * @see #setErrorHandler(Consumer)
     * @see <a href="https://github.com/ReactiveX/RxJava/wiki/What's-different-in-2.0#error-handling">Error handling Wiki</a>
     */
    public static void onError(@NonNull Throwable error) {
        Consumer<? super Throwable> f = errorHandler;

        if (error == null) {
            error = new NullPointerException("onError called with null. Null values are generally not allowed in 2.x operators and sources.");
        } else {
            if (!isBug(error)) {
                error = new UndeliverableException(error);
            }
        }

        if (f != null) {
            try {
                f.accept(error);
                return;
            } catch (Throwable e) {
                // Exceptions.throwIfFatal(e); TODO decide
                e.printStackTrace(); // NOPMD
                uncaught(e);
            }
        }

        error.printStackTrace(); // NOPMD
        uncaught(error);
    }

    /**
     * Checks if the given error is one of the already named
     * bug cases that should pass through {@link #onError(Throwable)}
     * as is.
     * @param error the error to check
     * @return true if the error should pass through, false if
     * it may be wrapped into an UndeliverableException
     */
    static boolean isBug(Throwable error) {
        // user forgot to add the onError handler in subscribe
        if (error instanceof OnErrorNotImplementedException) {
            return true;
        }
        // the sender didn't honor the request amount
        // it's either due to an operator bug or concurrent onNext
        if (error instanceof MissingBackpressureException) {
            return true;
        }
        // general protocol violations
        // it's either due to an operator bug or concurrent onNext
        if (error instanceof IllegalStateException) {
            return true;
        }
        // nulls are generally not allowed
        // likely an operator bug or missing null-check
        if (error instanceof NullPointerException) {
            return true;
        }
        // bad arguments, likely invalid user input
        if (error instanceof IllegalArgumentException) {
            return true;
        }
        // Crash while handling an exception
        if (error instanceof CompositeException) {
            return true;
        }
        // everything else is probably due to lifecycle limits
        return false;
    }

    static void uncaught(@NonNull Throwable error) {
        Thread currentThread = Thread.currentThread();
        UncaughtExceptionHandler handler = currentThread.getUncaughtExceptionHandler();
        handler.uncaughtException(currentThread, error);
    }

    /**
     * Calls the associated hook function.
     * @param defaultScheduler the hook's input value
     * @return the value returned by the hook
     */
    @NonNull
    public static Scheduler onIoScheduler(@NonNull Scheduler defaultScheduler) {
        Function<? super Scheduler, ? extends Scheduler> f = onIoHandler;
        if (f == null) {
            return defaultScheduler;
        }
        return apply(f, defaultScheduler);
    }

    /**
     * Calls the associated hook function.
     * @param defaultScheduler the hook's input value
     * @return the value returned by the hook
     */
    @NonNull
    public static Scheduler onNewThreadScheduler(@NonNull Scheduler defaultScheduler) {
        Function<? super Scheduler, ? extends Scheduler> f = onNewThreadHandler;
        if (f == null) {
            return defaultScheduler;
        }
        return apply(f, defaultScheduler);
    }

    /**
     * Called when a task is scheduled.
     * @param run the runnable instance
     * @return the replacement runnable
     */
    @NonNull
    public static Runnable onSchedule(@NonNull Runnable run) {
        ObjectHelper.requireNonNull(run, "run is null");

        Function<? super Runnable, ? extends Runnable> f = onScheduleHandler;
        if (f == null) {
            return run;
        }
        return apply(f, run);
    }

    /**
     * Calls the associated hook function.
     * @param defaultScheduler the hook's input value
     * @return the value returned by the hook
     */
    @NonNull
    public static Scheduler onSingleScheduler(@NonNull Scheduler defaultScheduler) {
        Function<? super Scheduler, ? extends Scheduler> f = onSingleHandler;
        if (f == null) {
            return defaultScheduler;
        }
        return apply(f, defaultScheduler);
    }

    /**
     * Removes all handlers and resets to default behavior.
     */
    public static void reset() {
        setErrorHandler(null);
        setScheduleHandler(null);

        setComputationSchedulerHandler(null);
        setInitComputationSchedulerHandler(null);

        setIoSchedulerHandler(null);
        setInitIoSchedulerHandler(null);

        setSingleSchedulerHandler(null);
        setInitSingleSchedulerHandler(null);

        setNewThreadSchedulerHandler(null);
        setInitNewThreadSchedulerHandler(null);

        setOnFlowableAssembly(null);
        setOnFlowableSubscribe(null);

        setOnObservableAssembly(null);
        setOnObservableSubscribe(null);

        setOnSingleAssembly(null);
        setOnSingleSubscribe(null);

        setOnCompletableAssembly(null);
        setOnCompletableSubscribe(null);

        setOnConnectableFlowableAssembly(null);
        setOnConnectableObservableAssembly(null);

        setOnMaybeAssembly(null);
        setOnMaybeSubscribe(null);

        setOnParallelAssembly(null);

        setFailOnNonBlockingScheduler(false);
        setOnBeforeBlocking(null);
    }

    /**
     * Sets the specific hook function.
     * @param handler the hook function to set, null allowed
     */
    public static void setComputationSchedulerHandler(@Nullable Function<? super Scheduler, ? extends Scheduler> handler) {
        if (lockdown) {
            throw new IllegalStateException("Plugins can't be changed anymore");
        }
        onComputationHandler = handler;
    }

    /**
     * Sets the specific hook function.
     * @param handler the hook function to set, null allowed
     */
    public static void setErrorHandler(@Nullable Consumer<? super Throwable> handler) {
        if (lockdown) {
            throw new IllegalStateException("Plugins can't be changed anymore");
        }
        errorHandler = handler;
    }

    /**
     * Sets the specific hook function.
     * @param handler the hook function to set, null allowed, but the function may not return null
     */
    public static void setInitComputationSchedulerHandler(@Nullable Function<? super Callable<Scheduler>, ? extends Scheduler> handler) {
        if (lockdown) {
            throw new IllegalStateException("Plugins can't be changed anymore");
        }
        onInitComputationHandler = handler;
    }

    /**
     * Sets the specific hook function.
     * @param handler the hook function to set, null allowed, but the function may not return null
     */
    public static void setInitIoSchedulerHandler(@Nullable Function<? super Callable<Scheduler>, ? extends Scheduler> handler) {
        if (lockdown) {
            throw new IllegalStateException("Plugins can't be changed anymore");
        }
        onInitIoHandler = handler;
    }

    /**
     * Sets the specific hook function.
     * @param handler the hook function to set, null allowed, but the function may not return null
     */
    public static void setInitNewThreadSchedulerHandler(@Nullable Function<? super Callable<Scheduler>, ? extends Scheduler> handler) {
        if (lockdown) {
            throw new IllegalStateException("Plugins can't be changed anymore");
        }
        onInitNewThreadHandler = handler;
    }

    /**
     * Sets the specific hook function.
     * @param handler the hook function to set, null allowed, but the function may not return null
     */
    public static void setInitSingleSchedulerHandler(@Nullable Function<? super Callable<Scheduler>, ? extends Scheduler> handler) {
        if (lockdown) {
            throw new IllegalStateException("Plugins can't be changed anymore");
        }
        onInitSingleHandler = handler;
    }

    /**
     * Sets the specific hook function.
     * @param handler the hook function to set, null allowed
     */
    public static void setIoSchedulerHandler(@Nullable Function<? super Scheduler, ? extends Scheduler> handler) {
        if (lockdown) {
            throw new IllegalStateException("Plugins can't be changed anymore");
        }
        onIoHandler = handler;
    }

    /**
     * Sets the specific hook function.
     * @param handler the hook function to set, null allowed
     */
    public static void setNewThreadSchedulerHandler(@Nullable Function<? super Scheduler, ? extends Scheduler> handler) {
        if (lockdown) {
            throw new IllegalStateException("Plugins can't be changed anymore");
        }
        onNewThreadHandler = handler;
    }

    /**
     * Sets the specific hook function.
     * @param handler the hook function to set, null allowed
     */
    public static void setScheduleHandler(@Nullable Function<? super Runnable, ? extends Runnable> handler) {
        if (lockdown) {
            throw new IllegalStateException("Plugins can't be changed anymore");
        }
        onScheduleHandler = handler;
    }

    /**
     * Sets the specific hook function.
     * @param handler the hook function to set, null allowed
     */
    public static void setSingleSchedulerHandler(@Nullable Function<? super Scheduler, ? extends Scheduler> handler) {
        if (lockdown) {
            throw new IllegalStateException("Plugins can't be changed anymore");
        }
        onSingleHandler = handler;
    }

    /**
     * Revokes the lockdown, only for testing purposes.
     */
    /* test. */static void unlock() {
        lockdown = false;
    }

    /**
     * Returns the current hook function.
     * @return the hook function, may be null
     */
    @Nullable
    public static Function<? super Completable, ? extends Completable> getOnCompletableAssembly() {
        return onCompletableAssembly;
    }

    /**
     * Returns the current hook function.
     * @return the hook function, may be null
     */
    @Nullable
    public static BiFunction<? super Completable, ? super CompletableObserver, ? extends CompletableObserver> getOnCompletableSubscribe() {
        return onCompletableSubscribe;
    }

    /**
     * Returns the current hook function.
     * @return the hook function, may be null
     */
    @SuppressWarnings("rawtypes")
    @Nullable
    public static Function<? super Flowable, ? extends Flowable> getOnFlowableAssembly() {
        return onFlowableAssembly;
    }

    /**
     * Returns the current hook function.
     * @return the hook function, may be null
     */
    @SuppressWarnings("rawtypes")
    @Nullable
    public static Function<? super ConnectableFlowable, ? extends ConnectableFlowable> getOnConnectableFlowableAssembly() {
        return onConnectableFlowableAssembly;
    }

    /**
     * Returns the current hook function.
     * @return the hook function, may be null
     */
    @Nullable
    @SuppressWarnings("rawtypes")
    public static BiFunction<? super Flowable, ? super Subscriber, ? extends Subscriber> getOnFlowableSubscribe() {
        return onFlowableSubscribe;
    }

    /**
     * Returns the current hook function.
     * @return the hook function, may be null
     */
    @Nullable
    @SuppressWarnings("rawtypes")
    public static BiFunction<? super Maybe, ? super MaybeObserver, ? extends MaybeObserver> getOnMaybeSubscribe() {
        return onMaybeSubscribe;
    }

    /**
     * Returns the current hook function.
     * @return the hook function, may be null
     */
    @Nullable
    @SuppressWarnings("rawtypes")
    public static Function<? super Maybe, ? extends Maybe> getOnMaybeAssembly() {
        return onMaybeAssembly;
    }

    /**
     * Returns the current hook function.
     * @return the hook function, may be null
     */
    @Nullable
    @SuppressWarnings("rawtypes")
    public static Function<? super Single, ? extends Single> getOnSingleAssembly() {
        return onSingleAssembly;
    }

    /**
     * Returns the current hook function.
     * @return the hook function, may be null
     */
    @Nullable
    @SuppressWarnings("rawtypes")
    public static BiFunction<? super Single, ? super SingleObserver, ? extends SingleObserver> getOnSingleSubscribe() {
        return onSingleSubscribe;
    }

    /**
     * Returns the current hook function.
     * @return the hook function, may be null
     */
    @Nullable
    @SuppressWarnings("rawtypes")
    public static Function<? super Observable, ? extends Observable> getOnObservableAssembly() {
        return onObservableAssembly;
    }

    /**
     * Returns the current hook function.
     * @return the hook function, may be null
     */
    @Nullable
    @SuppressWarnings("rawtypes")
    public static Function<? super ConnectableObservable, ? extends ConnectableObservable> getOnConnectableObservableAssembly() {
        return onConnectableObservableAssembly;
    }

    /**
     * Returns the current hook function.
     * @return the hook function, may be null
     */
    @Nullable
    @SuppressWarnings("rawtypes")
    public static BiFunction<? super Observable, ? super Observer, ? extends Observer> getOnObservableSubscribe() {
        return onObservableSubscribe;
    }

    /**
     * Sets the specific hook function.
     * @param onCompletableAssembly the hook function to set, null allowed
     */
    public static void setOnCompletableAssembly(@Nullable Function<? super Completable, ? extends Completable> onCompletableAssembly) {
        if (lockdown) {
            throw new IllegalStateException("Plugins can't be changed anymore");
        }
        RxJavaPlugins.onCompletableAssembly = onCompletableAssembly;
    }

    /**
     * Sets the specific hook function.
     * @param onCompletableSubscribe the hook function to set, null allowed
     */
    public static void setOnCompletableSubscribe(
            @Nullable BiFunction<? super Completable, ? super CompletableObserver, ? extends CompletableObserver> onCompletableSubscribe) {
        if (lockdown) {
            throw new IllegalStateException("Plugins can't be changed anymore");
        }
        RxJavaPlugins.onCompletableSubscribe = onCompletableSubscribe;
    }

    /**
     * Sets the specific hook function.
     * @param onFlowableAssembly the hook function to set, null allowed
     */
    @SuppressWarnings("rawtypes")
    public static void setOnFlowableAssembly(@Nullable Function<? super Flowable, ? extends Flowable> onFlowableAssembly) {
        if (lockdown) {
            throw new IllegalStateException("Plugins can't be changed anymore");
        }
        RxJavaPlugins.onFlowableAssembly = onFlowableAssembly;
    }

    /**
     * Sets the specific hook function.
     * @param onMaybeAssembly the hook function to set, null allowed
     */
    @SuppressWarnings("rawtypes")
    public static void setOnMaybeAssembly(@Nullable Function<? super Maybe, ? extends Maybe> onMaybeAssembly) {
        if (lockdown) {
            throw new IllegalStateException("Plugins can't be changed anymore");
        }
        RxJavaPlugins.onMaybeAssembly = onMaybeAssembly;
    }

    /**
     * Sets the specific hook function.
     * @param onConnectableFlowableAssembly the hook function to set, null allowed
     */
    @SuppressWarnings("rawtypes")
    public static void setOnConnectableFlowableAssembly(@Nullable Function<? super ConnectableFlowable, ? extends ConnectableFlowable> onConnectableFlowableAssembly) {
        if (lockdown) {
            throw new IllegalStateException("Plugins can't be changed anymore");
        }
        RxJavaPlugins.onConnectableFlowableAssembly = onConnectableFlowableAssembly;
    }

    /**
     * Sets the specific hook function.
     * @param onFlowableSubscribe the hook function to set, null allowed
     */
    @SuppressWarnings("rawtypes")
    public static void setOnFlowableSubscribe(@Nullable BiFunction<? super Flowable, ? super Subscriber, ? extends Subscriber> onFlowableSubscribe) {
        if (lockdown) {
            throw new IllegalStateException("Plugins can't be changed anymore");
        }
        RxJavaPlugins.onFlowableSubscribe = onFlowableSubscribe;
    }

    /**
     * Sets the specific hook function.
     * @param onMaybeSubscribe the hook function to set, null allowed
     */
    @SuppressWarnings("rawtypes")
    public static void setOnMaybeSubscribe(@Nullable BiFunction<? super Maybe, MaybeObserver, ? extends MaybeObserver> onMaybeSubscribe) {
        if (lockdown) {
            throw new IllegalStateException("Plugins can't be changed anymore");
        }
        RxJavaPlugins.onMaybeSubscribe = onMaybeSubscribe;
    }

    /**
     * Sets the specific hook function.
     * @param onObservableAssembly the hook function to set, null allowed
     */
    @SuppressWarnings("rawtypes")
    public static void setOnObservableAssembly(@Nullable Function<? super Observable, ? extends Observable> onObservableAssembly) {
        if (lockdown) {
            throw new IllegalStateException("Plugins can't be changed anymore");
        }
        RxJavaPlugins.onObservableAssembly = onObservableAssembly;
    }

    /**
     * Sets the specific hook function.
     * @param onConnectableObservableAssembly the hook function to set, null allowed
     */
    @SuppressWarnings("rawtypes")
    public static void setOnConnectableObservableAssembly(@Nullable Function<? super ConnectableObservable, ? extends ConnectableObservable> onConnectableObservableAssembly) {
        if (lockdown) {
            throw new IllegalStateException("Plugins can't be changed anymore");
        }
        RxJavaPlugins.onConnectableObservableAssembly = onConnectableObservableAssembly;
    }

    /**
     * Sets the specific hook function.
     * @param onObservableSubscribe the hook function to set, null allowed
     */
    @SuppressWarnings("rawtypes")
    public static void setOnObservableSubscribe(
            @Nullable BiFunction<? super Observable, ? super Observer, ? extends Observer> onObservableSubscribe) {
        if (lockdown) {
            throw new IllegalStateException("Plugins can't be changed anymore");
        }
        RxJavaPlugins.onObservableSubscribe = onObservableSubscribe;
    }

    /**
     * Sets the specific hook function.
     * @param onSingleAssembly the hook function to set, null allowed
     */
    @SuppressWarnings("rawtypes")
    public static void setOnSingleAssembly(@Nullable Function<? super Single, ? extends Single> onSingleAssembly) {
        if (lockdown) {
            throw new IllegalStateException("Plugins can't be changed anymore");
        }
        RxJavaPlugins.onSingleAssembly = onSingleAssembly;
    }

    /**
     * Sets the specific hook function.
     * @param onSingleSubscribe the hook function to set, null allowed
     */
    @SuppressWarnings("rawtypes")
    public static void setOnSingleSubscribe(@Nullable BiFunction<? super Single, ? super SingleObserver, ? extends SingleObserver> onSingleSubscribe) {
        if (lockdown) {
            throw new IllegalStateException("Plugins can't be changed anymore");
        }
        RxJavaPlugins.onSingleSubscribe = onSingleSubscribe;
    }

    /**
     * Calls the associated hook function.
     * @param <T> the value type
     * @param source the hook's input value
     * @param subscriber the subscriber
     * @return the value returned by the hook
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @NonNull
    public static <T> Subscriber<? super T> onSubscribe(@NonNull Flowable<T> source, @NonNull Subscriber<? super T> subscriber) {
        BiFunction<? super Flowable, ? super Subscriber, ? extends Subscriber> f = onFlowableSubscribe;
        if (f != null) {
            return apply(f, source, subscriber);
        }
        return subscriber;
    }

    /**
     * Calls the associated hook function.
     * @param <T> the value type
     * @param source the hook's input value
     * @param observer the observer
     * @return the value returned by the hook
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @NonNull
    public static <T> Observer<? super T> onSubscribe(@NonNull Observable<T> source, @NonNull Observer<? super T> observer) {
        BiFunction<? super Observable, ? super Observer, ? extends Observer> f = onObservableSubscribe;
        if (f != null) {
            return apply(f, source, observer);
        }
        return observer;
    }

    /**
     * Calls the associated hook function.
     * @param <T> the value type
     * @param source the hook's input value
     * @param observer the observer
     * @return the value returned by the hook
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @NonNull
    public static <T> SingleObserver<? super T> onSubscribe(@NonNull Single<T> source, @NonNull SingleObserver<? super T> observer) {
        BiFunction<? super Single, ? super SingleObserver, ? extends SingleObserver> f = onSingleSubscribe;
        if (f != null) {
            return apply(f, source, observer);
        }
        return observer;
    }

    /**
     * Calls the associated hook function.
     * @param source the hook's input value
     * @param observer the observer
     * @return the value returned by the hook
     */
    @NonNull
    public static CompletableObserver onSubscribe(@NonNull Completable source, @NonNull CompletableObserver observer) {
        BiFunction<? super Completable, ? super CompletableObserver, ? extends CompletableObserver> f = onCompletableSubscribe;
        if (f != null) {
            return apply(f, source, observer);
        }
        return observer;
    }

    /**
     * Calls the associated hook function.
     * @param <T> the value type
     * @param source the hook's input value
     * @param subscriber the subscriber
     * @return the value returned by the hook
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @NonNull
    public static <T> MaybeObserver<? super T> onSubscribe(@NonNull Maybe<T> source, @NonNull MaybeObserver<? super T> subscriber) {
        BiFunction<? super Maybe, ? super MaybeObserver, ? extends MaybeObserver> f = onMaybeSubscribe;
        if (f != null) {
            return apply(f, source, subscriber);
        }
        return subscriber;
    }

    /**
     * Calls the associated hook function.
     * @param <T> the value type
     * @param source the hook's input value
     * @return the value returned by the hook
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @NonNull
    public static <T> Maybe<T> onAssembly(@NonNull Maybe<T> source) {
        Function<? super Maybe, ? extends Maybe> f = onMaybeAssembly;
        if (f != null) {
            return apply(f, source);
        }
        return source;
    }

    /**
     * Calls the associated hook function.
     * @param <T> the value type
     * @param source the hook's input value
     * @return the value returned by the hook
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @NonNull
    public static <T> Flowable<T> onAssembly(@NonNull Flowable<T> source) {
        Function<? super Flowable, ? extends Flowable> f = onFlowableAssembly;
        if (f != null) {
            return apply(f, source);
        }
        return source;
    }

    /**
     * Calls the associated hook function.
     * @param <T> the value type
     * @param source the hook's input value
     * @return the value returned by the hook
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @NonNull
    public static <T> ConnectableFlowable<T> onAssembly(@NonNull ConnectableFlowable<T> source) {
        Function<? super ConnectableFlowable, ? extends ConnectableFlowable> f = onConnectableFlowableAssembly;
        if (f != null) {
            return apply(f, source);
        }
        return source;
    }

    /**
     * Calls the associated hook function.
     * @param <T> the value type
     * @param source the hook's input value
     * @return the value returned by the hook
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @NonNull
    public static <T> Observable<T> onAssembly(@NonNull Observable<T> source) {
        Function<? super Observable, ? extends Observable> f = onObservableAssembly;
        if (f != null) {
            return apply(f, source);
        }
        return source;
    }

    /**
     * Calls the associated hook function.
     * @param <T> the value type
     * @param source the hook's input value
     * @return the value returned by the hook
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @NonNull
    public static <T> ConnectableObservable<T> onAssembly(@NonNull ConnectableObservable<T> source) {
        Function<? super ConnectableObservable, ? extends ConnectableObservable> f = onConnectableObservableAssembly;
        if (f != null) {
            return apply(f, source);
        }
        return source;
    }

    /**
     * Calls the associated hook function.
     * @param <T> the value type
     * @param source the hook's input value
     * @return the value returned by the hook
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @NonNull
    public static <T> Single<T> onAssembly(@NonNull Single<T> source) {
        Function<? super Single, ? extends Single> f = onSingleAssembly;
        if (f != null) {
            return apply(f, source);
        }
        return source;
    }

    /**
     * Calls the associated hook function.
     * @param source the hook's input value
     * @return the value returned by the hook
     */
    @NonNull
    public static Completable onAssembly(@NonNull Completable source) {
        Function<? super Completable, ? extends Completable> f = onCompletableAssembly;
        if (f != null) {
            return apply(f, source);
        }
        return source;
    }

    /**
     * Sets the specific hook function.
     * <p>History: 2.0.6 - experimental; 2.1 - beta
     * @param handler the hook function to set, null allowed
     * @since 2.2
     */
    @SuppressWarnings("rawtypes")
    public static void setOnParallelAssembly(@Nullable Function<? super ParallelFlowable, ? extends ParallelFlowable> handler) {
        if (lockdown) {
            throw new IllegalStateException("Plugins can't be changed anymore");
        }
        onParallelAssembly = handler;
    }

    /**
     * Returns the current hook function.
     * <p>History: 2.0.6 - experimental; 2.1 - beta
     * @return the hook function, may be null
     * @since 2.2
     */
    @SuppressWarnings("rawtypes")
    @Nullable
    public static Function<? super ParallelFlowable, ? extends ParallelFlowable> getOnParallelAssembly() {
        return onParallelAssembly;
    }

    /**
     * Calls the associated hook function.
     * <p>History: 2.0.6 - experimental; 2.1 - beta
     * @param <T> the value type of the source
     * @param source the hook's input value
     * @return the value returned by the hook
     * @since 2.2
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @NonNull
    public static <T> ParallelFlowable<T> onAssembly(@NonNull ParallelFlowable<T> source) {
        Function<? super ParallelFlowable, ? extends ParallelFlowable> f = onParallelAssembly;
        if (f != null) {
            return apply(f, source);
        }
        return source;
    }

    /**
     * Called before an operator attempts a blocking operation
     * such as awaiting a condition or signal
     * and should return true to indicate the operator
     * should not block but throw an IllegalArgumentException.
     * <p>History: 2.0.5 - experimental
     * @return true if the blocking should be prevented
     * @see #setFailOnNonBlockingScheduler(boolean)
     * @since 2.1
     */
    public static boolean onBeforeBlocking() {
        BooleanSupplier f = onBeforeBlocking;
        if (f != null) {
            try {
                return f.getAsBoolean();
            } catch (Throwable ex) {
                throw ExceptionHelper.wrapOrThrow(ex);
            }
        }
        return false;
    }

    /**
     * Set the handler that is called when an operator attempts a blocking
     * await; the handler should return true to prevent the blocking
     * and to signal an IllegalStateException instead.
     * <p>History: 2.0.5 - experimental
     * @param handler the handler to set, null resets to the default handler
     * that always returns false
     * @see #onBeforeBlocking()
     * @since 2.1
     */
    public static void setOnBeforeBlocking(@Nullable BooleanSupplier handler) {
        if (lockdown) {
            throw new IllegalStateException("Plugins can't be changed anymore");
        }
        onBeforeBlocking = handler;
    }

    /**
     * Returns the current blocking handler or null if no custom handler
     * is set.
     * <p>History: 2.0.5 - experimental
     * @return the current blocking handler or null if not specified
     * @since 2.1
     */
    @Nullable
    public static BooleanSupplier getOnBeforeBlocking() {
        return onBeforeBlocking;
    }

    /**
     * Create an instance of the default {@link Scheduler} used for {@link Schedulers#computation()}
     * except using {@code threadFactory} for thread creation.
     * <p>History: 2.0.5 - experimental
     * @param threadFactory thread factory to use for creating worker threads. Note that this takes precedence over any
     *                      system properties for configuring new thread creation. Cannot be null.
     * @return the created Scheduler instance
     * @since 2.1
     */
    @NonNull
    public static Scheduler createComputationScheduler(@NonNull ThreadFactory threadFactory) {
        return new ComputationScheduler(ObjectHelper.requireNonNull(threadFactory, "threadFactory is null"));
    }

    /**
     * Create an instance of the default {@link Scheduler} used for {@link Schedulers#io()}
     * except using {@code threadFactory} for thread creation.
     * <p>History: 2.0.5 - experimental
     * @param threadFactory thread factory to use for creating worker threads. Note that this takes precedence over any
     *                      system properties for configuring new thread creation. Cannot be null.
     * @return the created Scheduler instance
     * @since 2.1
     */
    @NonNull
    public static Scheduler createIoScheduler(@NonNull ThreadFactory threadFactory) {
        return new IoScheduler(ObjectHelper.requireNonNull(threadFactory, "threadFactory is null"));
    }

    /**
     * Create an instance of the default {@link Scheduler} used for {@link Schedulers#newThread()}
     * except using {@code threadFactory} for thread creation.
     * <p>History: 2.0.5 - experimental
     * @param threadFactory thread factory to use for creating worker threads. Note that this takes precedence over any
     *                      system properties for configuring new thread creation. Cannot be null.
     * @return the created Scheduler instance
     * @since 2.1
     */
    @NonNull
    public static Scheduler createNewThreadScheduler(@NonNull ThreadFactory threadFactory) {
        return new NewThreadScheduler(ObjectHelper.requireNonNull(threadFactory, "threadFactory is null"));
    }

    /**
     * Create an instance of the default {@link Scheduler} used for {@link Schedulers#single()}
     * except using {@code threadFactory} for thread creation.
     * <p>History: 2.0.5 - experimental
     * @param threadFactory thread factory to use for creating worker threads. Note that this takes precedence over any
     *                      system properties for configuring new thread creation. Cannot be null.
     * @return the created Scheduler instance
     * @since 2.1
     */
    @NonNull
    public static Scheduler createSingleScheduler(@NonNull ThreadFactory threadFactory) {
        return new SingleScheduler(ObjectHelper.requireNonNull(threadFactory, "threadFactory is null"));
    }

    /**
     * Wraps the call to the function in try-catch and propagates thrown
     * checked exceptions as RuntimeException.
     * @param <T> the input type
     * @param <R> the output type
     * @param f the function to call, not null (not verified)
     * @param t the parameter value to the function
     * @return the result of the function call
     */
    @NonNull
    static <T, R> R apply(@NonNull Function<T, R> f, @NonNull T t) {
        try {
            return f.apply(t);
        } catch (Throwable ex) {
            throw ExceptionHelper.wrapOrThrow(ex);
        }
    }

    /**
     * Wraps the call to the function in try-catch and propagates thrown
     * checked exceptions as RuntimeException.
     * @param <T> the first input type
     * @param <U> the second input type
     * @param <R> the output type
     * @param f the function to call, not null (not verified)
     * @param t the first parameter value to the function
     * @param u the second parameter value to the function
     * @return the result of the function call
     */
    @NonNull
    static <T, U, R> R apply(@NonNull BiFunction<T, U, R> f, @NonNull T t, @NonNull U u) {
        try {
            return f.apply(t, u);
        } catch (Throwable ex) {
            throw ExceptionHelper.wrapOrThrow(ex);
        }
    }

    /**
     * Wraps the call to the Scheduler creation callable in try-catch and propagates thrown
     * checked exceptions as RuntimeException and enforces that result is not null.
     * @param s the {@link Callable} which returns a {@link Scheduler}, not null (not verified). Cannot return null
     * @return the result of the callable call, not null
     * @throws NullPointerException if the callable parameter returns null
     */
    @NonNull
    static Scheduler callRequireNonNull(@NonNull Callable<Scheduler> s) {
        try {
            return ObjectHelper.requireNonNull(s.call(), "Scheduler Callable result can't be null");
        } catch (Throwable ex) {
            throw ExceptionHelper.wrapOrThrow(ex);
        }
    }

    /**
     * Wraps the call to the Scheduler creation function in try-catch and propagates thrown
     * checked exceptions as RuntimeException and enforces that result is not null.
     * @param f the function to call, not null (not verified). Cannot return null
     * @param s the parameter value to the function
     * @return the result of the function call, not null
     * @throws NullPointerException if the function parameter returns null
     */
    @NonNull
    static Scheduler applyRequireNonNull(@NonNull Function<? super Callable<Scheduler>, ? extends Scheduler> f, Callable<Scheduler> s) {
        return ObjectHelper.requireNonNull(apply(f, s), "Scheduler Callable result can't be null");
    }

    /** Helper class, no instances. */
    private RxJavaPlugins() {
        throw new IllegalStateException("No instances!");
    }
}
