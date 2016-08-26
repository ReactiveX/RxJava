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
package io.reactivex.plugins;

import java.lang.Thread.UncaughtExceptionHandler;

import org.reactivestreams.Subscriber;

import io.reactivex.*;
import io.reactivex.flowables.ConnectableFlowable;
import io.reactivex.functions.*;
import io.reactivex.internal.util.ExceptionHelper;
import io.reactivex.observables.ConnectableObservable;

/**
 * Utility class to inject handlers to certain standard RxJava operations.
 */
public final class RxJavaPlugins {
    
    static volatile Consumer<Throwable> errorHandler;
    
    static volatile Function<Runnable, Runnable> onScheduleHandler;

    static volatile Function<Scheduler, Scheduler> onInitComputationHandler;
    
    static volatile Function<Scheduler, Scheduler> onInitSingleHandler;
    
    static volatile Function<Scheduler, Scheduler> onInitIoHandler;

    static volatile Function<Scheduler, Scheduler> onInitNewThreadHandler;
    
    static volatile Function<Scheduler, Scheduler> onComputationHandler;
    
    static volatile Function<Scheduler, Scheduler> onSingleHandler;
    
    static volatile Function<Scheduler, Scheduler> onIoHandler;

    static volatile Function<Scheduler, Scheduler> onNewThreadHandler;
    
    @SuppressWarnings("rawtypes")
    static volatile Function<Flowable, Flowable> onFlowableAssembly;

    @SuppressWarnings("rawtypes")
    static volatile Function<ConnectableFlowable, ConnectableFlowable> onConnectableFlowableAssembly;

    @SuppressWarnings("rawtypes")
    static volatile Function<Observable, Observable> onObservableAssembly;

    @SuppressWarnings("rawtypes")
    static volatile Function<ConnectableObservable, ConnectableObservable> onConnectableObservableAssembly;

    @SuppressWarnings("rawtypes")
    static volatile Function<Single, Single> onSingleAssembly;
    
    static volatile Function<Completable, Completable> onCompletableAssembly;
    
    @SuppressWarnings("rawtypes")
    static volatile BiFunction<Flowable, Subscriber, Subscriber> onFlowableSubscribe;

    @SuppressWarnings("rawtypes")
    static volatile BiFunction<Observable, Observer, Observer> onObservableSubscribe;

    @SuppressWarnings("rawtypes")
    static volatile BiFunction<Single, SingleObserver, SingleObserver> onSingleSubscribe;

    static volatile BiFunction<Completable, CompletableObserver, CompletableObserver> onCompletableSubscribe;

    /** Prevents changing the plugins. */
    static volatile boolean lockdown;
    
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
     * Returns the current hook function.
     * @return the hook function, may be null
     */
    public static Function<Scheduler, Scheduler> getComputationSchedulerHandler() {
        return onComputationHandler;
    }
    
    /**
     * Returns the a hook consumer.
     * @return the hook consumer, may be null
     */
    public static Consumer<Throwable> getErrorHandler() {
        return errorHandler;
    }
    
    /**
     * Returns the current hook function.
     * @return the hook function, may be null
     */
    public static Function<Scheduler, Scheduler> getInitComputationSchedulerHandler() {
        return onInitComputationHandler;
    }

    /**
     * Returns the current hook function.
     * @return the hook function, may be null
     */
    public static Function<Scheduler, Scheduler> getInitIoSchedulerHandler() {
        return onInitIoHandler;
    }

    /**
     * Returns the current hook function.
     * @return the hook function, may be null
     */
    public static Function<Scheduler, Scheduler> getInitNewThreadSchedulerHandler() {
        return onInitNewThreadHandler;
    }

    /**
     * Returns the current hook function.
     * @return the hook function, may be null
     */
    public static Function<Scheduler, Scheduler> getInitSingleSchedulerHandler() {
        return onInitSingleHandler;
    }

    /**
     * Returns the current hook function.
     * @return the hook function, may be null
     */
    public static Function<Scheduler, Scheduler> getIoSchedulerHandler() {
        return onIoHandler;
    }

    /**
     * Returns the current hook function.
     * @return the hook function, may be null
     */
    public static Function<Scheduler, Scheduler> getNewThreadSchedulerHandler() {
        return onNewThreadHandler;
    }

    /**
     * Returns the current hook function.
     * @return the hook function, may be null
     */
    public static Function<Runnable, Runnable> getScheduleHandler() {
        return onScheduleHandler;
    }

    /**
     * Returns the current hook function.
     * @return the hook function, may be null
     */
    public static Function<Scheduler, Scheduler> getSingleSchedulerHandler() {
        return onSingleHandler;
    }

    /**
     * Calls the associated hook function.
     * @param defaultScheduler the hook's input value
     * @return the value returned by the hook
     */
    public static Scheduler initComputationScheduler(Scheduler defaultScheduler) {
        Function<Scheduler, Scheduler> f = onInitComputationHandler;
        if (f == null) {
            return defaultScheduler;
        }
        return apply(f, defaultScheduler); // JIT will skip this
    }

    /**
     * Calls the associated hook function.
     * @param defaultScheduler the hook's input value
     * @return the value returned by the hook
     */
    public static Scheduler initIoScheduler(Scheduler defaultScheduler) {
        Function<Scheduler, Scheduler> f = onInitIoHandler;
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
    public static Scheduler initNewThreadScheduler(Scheduler defaultScheduler) {
        Function<Scheduler, Scheduler> f = onInitNewThreadHandler;
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
    public static Scheduler initSingleScheduler(Scheduler defaultScheduler) {
        Function<Scheduler, Scheduler> f = onInitSingleHandler;
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
    public static Scheduler onComputationScheduler(Scheduler defaultScheduler) {
        Function<Scheduler, Scheduler> f = onComputationHandler;
        if (f == null) {
            return defaultScheduler;
        }
        return apply(f, defaultScheduler);
    }

    /**
     * Called when an undeliverable error occurs.
     * @param error the error to report
     */
    public static void onError(Throwable error) {
        Consumer<Throwable> f = errorHandler;
        if (f != null) {
            try {
                f.accept(error);
                return;
            } catch (Throwable e) {
                // Exceptions.throwIfFatal(e); TODO decide
                if (error == null) {
                    error = new NullPointerException();
                }
                e.printStackTrace(); // NOPMD
                uncaught(e);
            }
        } else {
            if (error == null) {
                error = new NullPointerException();
            }
        }
        error.printStackTrace(); // NOPMD
        uncaught(error);
    }
    
    static void uncaught(Throwable error) {
        Thread currentThread = Thread.currentThread();
        UncaughtExceptionHandler handler = currentThread.getUncaughtExceptionHandler();
        handler.uncaughtException(currentThread, error);
    }
    
    /**
     * Calls the associated hook function.
     * @param defaultScheduler the hook's input value
     * @return the value returned by the hook
     */
    public static Scheduler onIoScheduler(Scheduler defaultScheduler) {
        Function<Scheduler, Scheduler> f = onIoHandler;
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
    public static Scheduler onNewThreadScheduler(Scheduler defaultScheduler) {
        Function<Scheduler, Scheduler> f = onNewThreadHandler;
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
    public static Runnable onSchedule(Runnable run) {
        Function<Runnable, Runnable> f = onScheduleHandler;
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
    public static Scheduler onSingleScheduler(Scheduler defaultScheduler) {
        Function<Scheduler, Scheduler> f = onSingleHandler;
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
    }

    /**
     * Sets the specific hook function.
     * @param handler the hook function to set, null allowed
     */
    public static void setComputationSchedulerHandler(Function<Scheduler, Scheduler> handler) {
        if (lockdown) {
            throw new IllegalStateException("Plugins can't be changed anymore");
        }
        onComputationHandler = handler;
    }

    /**
     * Sets the specific hook function.
     * @param handler the hook function to set, null allowed
     */
    public static void setErrorHandler(Consumer<Throwable> handler) {
        if (lockdown) {
            throw new IllegalStateException("Plugins can't be changed anymore");
        }
        errorHandler = handler;
    }

    /**
     * Sets the specific hook function.
     * @param handler the hook function to set, null allowed
     */
    public static void setInitComputationSchedulerHandler(Function<Scheduler, Scheduler> handler) {
        if (lockdown) {
            throw new IllegalStateException("Plugins can't be changed anymore");
        }
        onInitComputationHandler = handler;
    }

    /**
     * Sets the specific hook function.
     * @param handler the hook function to set, null allowed
     */
    public static void setInitIoSchedulerHandler(Function<Scheduler, Scheduler> handler) {
        if (lockdown) {
            throw new IllegalStateException("Plugins can't be changed anymore");
        }
        onInitIoHandler = handler;
    }

    /**
     * Sets the specific hook function.
     * @param handler the hook function to set, null allowed
     */
    public static void setInitNewThreadSchedulerHandler(Function<Scheduler, Scheduler> handler) {
        if (lockdown) {
            throw new IllegalStateException("Plugins can't be changed anymore");
        }
        onInitNewThreadHandler = handler;
    }

    /**
     * Sets the specific hook function.
     * @param handler the hook function to set, null allowed
     */
    public static void setInitSingleSchedulerHandler(Function<Scheduler, Scheduler> handler) {
        if (lockdown) {
            throw new IllegalStateException("Plugins can't be changed anymore");
        }
        onInitSingleHandler = handler;
    }

    /**
     * Sets the specific hook function.
     * @param handler the hook function to set, null allowed
     */
    public static void setIoSchedulerHandler(Function<Scheduler, Scheduler> handler) {
        if (lockdown) {
            throw new IllegalStateException("Plugins can't be changed anymore");
        }
        onIoHandler = handler;
    }

    /**
     * Sets the specific hook function.
     * @param handler the hook function to set, null allowed
     */
    public static void setNewThreadSchedulerHandler(Function<Scheduler, Scheduler> handler) {
        if (lockdown) {
            throw new IllegalStateException("Plugins can't be changed anymore");
        }
        onNewThreadHandler = handler;
    }

    /**
     * Sets the specific hook function.
     * @param handler the hook function to set, null allowed
     */
    public static void setScheduleHandler(Function<Runnable, Runnable> handler) {
        if (lockdown) {
            throw new IllegalStateException("Plugins can't be changed anymore");
        }
        onScheduleHandler = handler;
    }

    /**
     * Sets the specific hook function.
     * @param handler the hook function to set, null allowed
     */
    public static void setSingleSchedulerHandler(Function<Scheduler, Scheduler> handler) {
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
    public static Function<Completable, Completable> getOnCompletableAssembly() {
        return onCompletableAssembly;
    }
    
    /**
     * Returns the current hook function.
     * @return the hook function, may be null
     */
    public static BiFunction<Completable, CompletableObserver, CompletableObserver> getOnCompletableSubscribe() {
        return onCompletableSubscribe;
    }
    
    /**
     * Returns the current hook function.
     * @return the hook function, may be null
     */
    @SuppressWarnings("rawtypes")
    public static Function<Flowable, Flowable> getOnFlowableAssembly() {
        return onFlowableAssembly;
    }

    /**
     * Returns the current hook function.
     * @return the hook function, may be null
     */
    @SuppressWarnings("rawtypes")
    public static Function<ConnectableFlowable, ConnectableFlowable> getOnConnectableFlowableAssembly() {
        return onConnectableFlowableAssembly;
    }

    /**
     * Returns the current hook function.
     * @return the hook function, may be null
     */
    @SuppressWarnings("rawtypes")
    public static BiFunction<Flowable, Subscriber, Subscriber> getOnFlowableSubscribe() {
        return onFlowableSubscribe;
    }
    
    /**
     * Returns the current hook function.
     * @return the hook function, may be null
     */
    @SuppressWarnings("rawtypes")
    public static Function<Single, Single> getOnSingleAssembly() {
        return onSingleAssembly;
    }
    
    /**
     * Returns the current hook function.
     * @return the hook function, may be null
     */
    @SuppressWarnings("rawtypes")
    public static BiFunction<Single, SingleObserver, SingleObserver> getOnSingleSubscribe() {
        return onSingleSubscribe;
    }
    
    /**
     * Returns the current hook function.
     * @return the hook function, may be null
     */
    @SuppressWarnings("rawtypes")
    public static Function<Observable, Observable> getOnObservableAssembly() {
        return onObservableAssembly;
    }
    
    /**
     * Returns the current hook function.
     * @return the hook function, may be null
     */
    @SuppressWarnings("rawtypes")
    public static Function<ConnectableObservable, ConnectableObservable> getOnConnectableObservableAssembly() {
        return onConnectableObservableAssembly;
    }
    
    /**
     * Returns the current hook function.
     * @return the hook function, may be null
     */
    @SuppressWarnings("rawtypes")
    public static BiFunction<Observable, Observer, Observer> getOnObservableSubscribe() {
        return onObservableSubscribe;
    }
    
    /**
     * Sets the specific hook function.
     * @param onCompletableAssembly the hook function to set, null allowed
     */
    public static void setOnCompletableAssembly(Function<Completable, Completable> onCompletableAssembly) {
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
            BiFunction<Completable, CompletableObserver, CompletableObserver> onCompletableSubscribe) {
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
    public static void setOnFlowableAssembly(Function<Flowable, Flowable> onFlowableAssembly) {
        if (lockdown) {
            throw new IllegalStateException("Plugins can't be changed anymore");
        }
        RxJavaPlugins.onFlowableAssembly = onFlowableAssembly;
    }
    
    /**
     * Sets the specific hook function.
     * @param onConnectableFlowableAssembly the hook function to set, null allowed
     */
    @SuppressWarnings("rawtypes")
    public static void setOnConnectableFlowableAssembly(Function<ConnectableFlowable, ConnectableFlowable> onConnectableFlowableAssembly) {
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
    public static void setOnFlowableSubscribe(BiFunction<Flowable, Subscriber, Subscriber> onFlowableSubscribe) {
        if (lockdown) {
            throw new IllegalStateException("Plugins can't be changed anymore");
        }
        RxJavaPlugins.onFlowableSubscribe = onFlowableSubscribe;
    }
    
    /**
     * Sets the specific hook function.
     * @param onObservableAssembly the hook function to set, null allowed
     */
    @SuppressWarnings("rawtypes")
    public static void setOnObservableAssembly(Function<Observable, Observable> onObservableAssembly) {
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
    public static void setOnConnectableObservableAssembly(Function<ConnectableObservable, ConnectableObservable> onConnectableObservableAssembly) {
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
            BiFunction<Observable, Observer, Observer> onObservableSubscribe) {
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
    public static void setOnSingleAssembly(Function<Single, Single> onSingleAssembly) {
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
    public static void setOnSingleSubscribe(BiFunction<Single, SingleObserver, SingleObserver> onSingleSubscribe) {
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
    public static <T> Subscriber<? super T> onSubscribe(Flowable<T> source, Subscriber<? super T> subscriber) {
        BiFunction<Flowable, Subscriber, Subscriber> f = onFlowableSubscribe;
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
    public static <T> Observer<? super T> onSubscribe(Observable<T> source, Observer<? super T> observer) {
        BiFunction<Observable, Observer, Observer> f = onObservableSubscribe;
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
    public static <T> SingleObserver<? super T> onSubscribe(Single<T> source, SingleObserver<? super T> observer) {
        BiFunction<Single, SingleObserver, SingleObserver> f = onSingleSubscribe;
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
    public static CompletableObserver onSubscribe(Completable source, CompletableObserver observer) {
        BiFunction<Completable, CompletableObserver, CompletableObserver> f = onCompletableSubscribe;
        if (f != null) {
            return apply(f, source, observer);
        }
        return observer;
    }

    /**
     * Calls the associated hook function.
     * @param <T> the value type
     * @param source the hook's input value
     * @return the value returned by the hook
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static <T> Flowable<T> onAssembly(Flowable<T> source) {
        Function<Flowable, Flowable> f = onFlowableAssembly;
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
    public static <T> ConnectableFlowable<T> onAssembly(ConnectableFlowable<T> source) {
        Function<ConnectableFlowable, ConnectableFlowable> f = onConnectableFlowableAssembly;
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
    public static <T> Observable<T> onAssembly(Observable<T> source) {
        Function<Observable, Observable> f = onObservableAssembly;
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
    public static <T> ConnectableObservable<T> onAssembly(ConnectableObservable<T> source) {
        Function<ConnectableObservable, ConnectableObservable> f = onConnectableObservableAssembly;
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
    public static <T> Single<T> onAssembly(Single<T> source) {
        Function<Single, Single> f = onSingleAssembly;
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
    public static Completable onAssembly(Completable source) {
        Function<Completable, Completable> f = onCompletableAssembly;
        if (f != null) {
            return apply(f, source);
        }
        return source;
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
    static <T, R> R apply(Function<T, R> f, T t) {
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
    static <T, U, R> R apply(BiFunction<T, U, R> f, T t, U u) {
        try {
            return f.apply(t, u);
        } catch (Throwable ex) {
            throw ExceptionHelper.wrapOrThrow(ex);
        }
    }

    /** Helper class, no instances. */
    private RxJavaPlugins() {
        throw new IllegalStateException("No instances!");
    }
}
