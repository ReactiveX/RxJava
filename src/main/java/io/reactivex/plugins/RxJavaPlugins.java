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
import io.reactivex.exceptions.Exceptions;
import io.reactivex.functions.*;

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
    static volatile Function<Observable, Observable> onObservableAssembly;
    
    @SuppressWarnings("rawtypes")
    static volatile Function<Single, Single> onSingleAssembly;
    
    static volatile Function<Completable, Completable> onCompletableAssembly;
    
    @SuppressWarnings("rawtypes")
    static volatile BiFunction<Flowable, Subscriber, Subscriber> onFlowableSubscribe;

    @SuppressWarnings("rawtypes")
    static volatile BiFunction<Observable, Observer, Observer> onObservableSubscribe;

    @SuppressWarnings("rawtypes")
    static volatile BiFunction<Single, SingleSubscriber, SingleSubscriber> onSingleSubscribe;

    static volatile BiFunction<Completable, CompletableSubscriber, CompletableSubscriber> onCompletableSubscribe;

    /** Prevents changing the plugins. */
    private static volatile boolean lockdown;
    
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
    
    public static Function<Scheduler, Scheduler> getComputationSchedulerHandler() {
        return onComputationHandler;
    }
    
    public static Consumer<Throwable> getErrorHandler() {
        return errorHandler;
    }
    
    public static Function<Scheduler, Scheduler> getInitComputationSchedulerHandler() {
        return onInitComputationHandler;
    }

    public static Function<Scheduler, Scheduler> getInitIoSchedulerHandler() {
        return onInitIoHandler;
    }

    public static Function<Scheduler, Scheduler> getInitNewThreadSchedulerHandler() {
        return onInitNewThreadHandler;
    }

    public static Function<Scheduler, Scheduler> getInitSingleSchedulerHandler() {
        return onInitSingleHandler;
    }

    public static Function<Scheduler, Scheduler> getIoSchedulerHandler() {
        return onIoHandler;
    }

    public static Function<Scheduler, Scheduler> getNewThreadSchedulerHandler() {
        return onNewThreadHandler;
    }

    public static Function<Runnable, Runnable> getScheduleHandler() {
        return onScheduleHandler;
    }
    public static Function<Scheduler, Scheduler> getSingleSchedulerHandler() {
        return onSingleHandler;
    }

    public static Scheduler initComputationScheduler(Scheduler defaultScheduler) {
        Function<Scheduler, Scheduler> f = onInitComputationHandler;
        if (f == null) {
            return defaultScheduler;
        }
        return apply(f, defaultScheduler); // JIT will skip this
    }

    public static Scheduler initIoScheduler(Scheduler defaultScheduler) {
        Function<Scheduler, Scheduler> f = onInitIoHandler;
        if (f == null) {
            return defaultScheduler;
        }
        return apply(f, defaultScheduler);
    }

    public static Scheduler initNewThreadScheduler(Scheduler defaultScheduler) {
        Function<Scheduler, Scheduler> f = onInitNewThreadHandler;
        if (f == null) {
            return defaultScheduler;
        }
        return apply(f, defaultScheduler);
    }

    public static Scheduler initSingleScheduler(Scheduler defaultScheduler) {
        Function<Scheduler, Scheduler> f = onInitSingleHandler;
        if (f == null) {
            return defaultScheduler;
        }
        return apply(f, defaultScheduler);
    }

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
                if (error == null) {
                    error = new NullPointerException();
                }
                e.printStackTrace(); // NOPMD
            }
        } else {
            if (error == null) {
                error = new NullPointerException();
            }
        }
        error.printStackTrace(); // NOPMD
        
        UncaughtExceptionHandler handler = Thread.currentThread().getUncaughtExceptionHandler();
        handler.uncaughtException(Thread.currentThread(), error);
    }
    
    public static Scheduler onIoScheduler(Scheduler defaultScheduler) {
        Function<Scheduler, Scheduler> f = onIoHandler;
        if (f == null) {
            return defaultScheduler;
        }
        return apply(f, defaultScheduler);
    }

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

    public static Scheduler onSingleScheduler(Scheduler defaultScheduler) {
        Function<Scheduler, Scheduler> f = onSingleHandler;
        if (f == null) {
            return defaultScheduler;
        }
        return apply(f, defaultScheduler);
    }

    /**
     * Removes all handlers and resets the default behavior.
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
    }

    public static void setComputationSchedulerHandler(Function<Scheduler, Scheduler> handler) {
        if (lockdown) {
            throw new IllegalStateException("Plugins can't be changed anymore");
        }
        onComputationHandler = handler;
    }

    public static void setErrorHandler(Consumer<Throwable> handler) {
        if (lockdown) {
            throw new IllegalStateException("Plugins can't be changed anymore");
        }
        errorHandler = handler;
    }

    public static void setInitComputationSchedulerHandler(Function<Scheduler, Scheduler> handler) {
        if (lockdown) {
            throw new IllegalStateException("Plugins can't be changed anymore");
        }
        onInitComputationHandler = handler;
    }

    public static void setInitIoSchedulerHandler(Function<Scheduler, Scheduler> handler) {
        if (lockdown) {
            throw new IllegalStateException("Plugins can't be changed anymore");
        }
        onInitIoHandler = handler;
    }

    public static void setInitNewThreadSchedulerHandler(Function<Scheduler, Scheduler> handler) {
        if (lockdown) {
            throw new IllegalStateException("Plugins can't be changed anymore");
        }
        onInitNewThreadHandler = handler;
    }

    
    public static void setInitSingleSchedulerHandler(Function<Scheduler, Scheduler> handler) {
        if (lockdown) {
            throw new IllegalStateException("Plugins can't be changed anymore");
        }
        onInitSingleHandler = handler;
    }

    public static void setIoSchedulerHandler(Function<Scheduler, Scheduler> handler) {
        if (lockdown) {
            throw new IllegalStateException("Plugins can't be changed anymore");
        }
        onIoHandler = handler;
    }

    public static void setNewThreadSchedulerHandler(Function<Scheduler, Scheduler> handler) {
        if (lockdown) {
            throw new IllegalStateException("Plugins can't be changed anymore");
        }
        onNewThreadHandler = handler;
    }

    public static void setScheduleHandler(Function<Runnable, Runnable> handler) {
        if (lockdown) {
            throw new IllegalStateException("Plugins can't be changed anymore");
        }
        onScheduleHandler = handler;
    }

    public static void setSingleSchedulerHandler(Function<Scheduler, Scheduler> handler) {
        if (lockdown) {
            throw new IllegalStateException("Plugins can't be changed anymore");
        }
        onSingleHandler = handler;
    }

    /**
     * Rewokes the lockdown, only for testing purposes.
     */
    /* test. */static void unlock() {
        lockdown = false;
    }

    public static Function<Completable, Completable> getOnCompletableAssembly() {
        return onCompletableAssembly;
    }
    
    public static BiFunction<Completable, CompletableSubscriber, CompletableSubscriber> getOnCompletableSubscribe() {
        return onCompletableSubscribe;
    }
    
    @SuppressWarnings("rawtypes")
    public static Function<Flowable, Flowable> getOnFlowableAssembly() {
        return onFlowableAssembly;
    }
    
    @SuppressWarnings("rawtypes")
    public static BiFunction<Flowable, Subscriber, Subscriber> getOnFlowableSubscribe() {
        return onFlowableSubscribe;
    }
    
    @SuppressWarnings("rawtypes")
    public static Function<Single, Single> getOnSingleAssembly() {
        return onSingleAssembly;
    }
    
    @SuppressWarnings("rawtypes")
    public static BiFunction<Single, SingleSubscriber, SingleSubscriber> getOnSingleSubscribe() {
        return onSingleSubscribe;
    }
    
    @SuppressWarnings("rawtypes")
    public static Function<Observable, Observable> getOnObservableAssembly() {
        return onObservableAssembly;
    }
    
    @SuppressWarnings("rawtypes")
    public static BiFunction<Observable, Observer, Observer> getOnObservableSubscribe() {
        return onObservableSubscribe;
    }
    
    public static void setOnCompletableAssembly(Function<Completable, Completable> onCompletableAssembly) {
        if (lockdown) {
            throw new IllegalStateException("Plugins can't be changed anymore");
        }
        RxJavaPlugins.onCompletableAssembly = onCompletableAssembly;
    }
    
    public static void setOnCompletableSubscribe(
            BiFunction<Completable, CompletableSubscriber, CompletableSubscriber> onCompletableSubscribe) {
        if (lockdown) {
            throw new IllegalStateException("Plugins can't be changed anymore");
        }
        RxJavaPlugins.onCompletableSubscribe = onCompletableSubscribe;
    }
    
    @SuppressWarnings("rawtypes")
    public static void setOnFlowableAssembly(Function<Flowable, Flowable> onFlowableAssembly) {
        if (lockdown) {
            throw new IllegalStateException("Plugins can't be changed anymore");
        }
        RxJavaPlugins.onFlowableAssembly = onFlowableAssembly;
    }
    
    @SuppressWarnings("rawtypes")
    public static void setOnFlowableSubscribe(BiFunction<Flowable, Subscriber, Subscriber> onFlowableSubscribe) {
        if (lockdown) {
            throw new IllegalStateException("Plugins can't be changed anymore");
        }
        RxJavaPlugins.onFlowableSubscribe = onFlowableSubscribe;
    }
    
    @SuppressWarnings("rawtypes")
    public static void setOnObservableAssembly(Function<Observable, Observable> onObservableAssembly) {
        if (lockdown) {
            throw new IllegalStateException("Plugins can't be changed anymore");
        }
        RxJavaPlugins.onObservableAssembly = onObservableAssembly;
    }
    
    @SuppressWarnings("rawtypes")
    public static void setOnObservableSubscribe(
            BiFunction<Observable, Observer, Observer> onObservableSubscribe) {
        if (lockdown) {
            throw new IllegalStateException("Plugins can't be changed anymore");
        }
        RxJavaPlugins.onObservableSubscribe = onObservableSubscribe;
    }
    
    @SuppressWarnings("rawtypes")
    public static void setOnSingleAssembly(Function<Single, Single> onSingleAssembly) {
        if (lockdown) {
            throw new IllegalStateException("Plugins can't be changed anymore");
        }
        RxJavaPlugins.onSingleAssembly = onSingleAssembly;
    }
    
    @SuppressWarnings("rawtypes")
    public static void setOnSingleSubscribe(BiFunction<Single, SingleSubscriber, SingleSubscriber> onSingleSubscribe) {
        if (lockdown) {
            throw new IllegalStateException("Plugins can't be changed anymore");
        }
        RxJavaPlugins.onSingleSubscribe = onSingleSubscribe;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static <T> Subscriber<? super T> onSubscribe(Flowable<T> source, Subscriber<? super T> subscriber) {
        BiFunction<Flowable, Subscriber, Subscriber> f = onFlowableSubscribe;
        if (f != null) {
            return apply(f, source, subscriber);
        }
        return subscriber;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static <T> Observer<? super T> onSubscribe(Observable<T> source, Observer<? super T> observer) {
        BiFunction<Observable, Observer, Observer> f = onObservableSubscribe;
        if (f != null) {
            return apply(f, source, observer);
        }
        return observer;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static <T> SingleSubscriber<? super T> onSubscribe(Single<T> source, SingleSubscriber<? super T> subscriber) {
        BiFunction<Single, SingleSubscriber, SingleSubscriber> f = onSingleSubscribe;
        if (f != null) {
            return apply(f, source, subscriber);
        }
        return subscriber;
    }

    public static CompletableSubscriber onSubscribe(Completable source, CompletableSubscriber subscriber) {
        BiFunction<Completable, CompletableSubscriber, CompletableSubscriber> f = onCompletableSubscribe;
        if (f != null) {
            return apply(f, source, subscriber);
        }
        return subscriber;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static <T> Flowable<T> onAssembly(Flowable<T> source) {
        Function<Flowable, Flowable> f = onFlowableAssembly;
        if (f != null) {
            return apply(f, source);
        }
        return source;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static <T> Observable<T> onAssembly(Observable<T> source) {
        Function<Observable, Observable> f = onObservableAssembly;
        if (f != null) {
            return apply(f, source);
        }
        return source;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static <T> Single<T> onAssembly(Single<T> source) {
        Function<Single, Single> f = onSingleAssembly;
        if (f != null) {
            return apply(f, source);
        }
        return source;
    }

    public static Completable onAssembly(Completable source) {
        Function<Completable, Completable> f = onCompletableAssembly;
        if (f != null) {
            return apply(f, source);
        }
        return source;
    }

    /** Singleton consumer that calls RxJavaPlugins.onError. */
    static final Consumer<Throwable> CONSUME_BY_RXJAVA_PLUGIN = new Consumer<Throwable>() {
        @Override
        public void accept(Throwable e) {
            RxJavaPlugins.onError(e);
        }
    };
    
    /**
     * Returns a consumer which relays the received Throwable to RxJavaPlugins.onError().
     * @return the consumer
     */
    public static Consumer<Throwable> errorConsumer() {
        return CONSUME_BY_RXJAVA_PLUGIN;
    }
    
    /**
     * Wraps the call to the function in try-catch and propagates thrown
     * checked exceptions as runtimeexception.
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
            throw Exceptions.propagate(ex);
        }
    }

    /**
     * Wraps the call to the function in try-catch and propagates thrown
     * checked exceptions as runtimeexception.
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
            throw Exceptions.propagate(ex);
        }
    }

    /** Helper class, no instances. */
    private RxJavaPlugins() {
        throw new IllegalStateException("No instances!");
    }
}
