/**
 * Copyright 2015 Netflix, Inc.
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

import java.util.function.*;

import org.reactivestreams.*;

import io.reactivex.Scheduler;

/**
 * Utility class to inject handlers to certain standard RxJava operations.
 */
public final class RxJavaPlugins {
    static final Consumer<Throwable> DEFAULT_ERROR_HANDLER = error -> {
        if (error != null) {
            error.printStackTrace();
        } else {
            new NullPointerException().printStackTrace();
        }
    };
    
    static volatile Consumer<Throwable> errorHandler = DEFAULT_ERROR_HANDLER;
    
    static volatile Function<Subscriber<Object>, Subscriber<Object>> onSubscribeHandler = e -> e;
    
    static volatile Function<Publisher<Object>, Publisher<Object>> onCreateHandler = e -> e;

    static volatile Function<Runnable, Runnable> onScheduleHandler = r -> r;

    static volatile Function<Scheduler, Scheduler> onInitComputationHandler = s -> s;
    
    static volatile Function<Scheduler, Scheduler> onInitSingleHandler = s -> s;
    
    static volatile Function<Scheduler, Scheduler> onInitIOHandler = s -> s;
    
    static volatile Function<Scheduler, Scheduler> onComputationHandler = s -> s;
    
    static volatile Function<Scheduler, Scheduler> onSingleHandler = s -> s;
    
    static volatile Function<Scheduler, Scheduler> onIOHandler = s -> s;
    
    public static Function<Scheduler, Scheduler> getComputationSchedulerHandler() {
        return onComputationHandler;
    }
    
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static <T> Function<Publisher<T>, Publisher<T>> getCreateHandler() {
        return (Function)onCreateHandler;
    }

    public static Consumer<Throwable> getErrorHandler() {
        return errorHandler;
    }
    
    public static Function<Scheduler, Scheduler> getInitComputationSchedulerHandler() {
        return onInitComputationHandler;
    }

    public static Function<Scheduler, Scheduler> getInitIOSchedulerHandler() {
        return onInitIOHandler;
    }
    
    public static Function<Scheduler, Scheduler> getInitSingleSchedulerHandler() {
        return onInitSingleHandler;
    }

    public static Function<Scheduler, Scheduler> getIOSchedulerHandler() {
        return onIOHandler;
    }
    
    public static Function<Runnable, Runnable> getScheduleHandler() {
        return onScheduleHandler;
    }
    public static Function<Scheduler, Scheduler> getSingleSchedulerHandler() {
        return onSingleHandler;
    }
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static <T> Function<Subscriber<T>, Subscriber<T>> getSubscribeHandler() {
        return (Function)onSubscribeHandler;
    }
    
    public static Scheduler initComputationScheduler(Scheduler defaultScheduler) {
        return onInitComputationHandler.apply(defaultScheduler);
    }

    public static Scheduler initIOScheduler(Scheduler defaultScheduler) {
        return onInitIOHandler.apply(defaultScheduler);
    }

    public static Scheduler initSingleScheduler(Scheduler defaultScheduler) {
        return onInitSingleHandler.apply(defaultScheduler);
    }

    public static Scheduler onComputationScheduler(Scheduler defaultScheduler) {
        return onComputationHandler.apply(defaultScheduler);
    }
    /**
     * Called when an Observable is created.
     * @param publisher
     * @return
     */
    @SuppressWarnings({ "unchecked", "rawtypes"})
    public static <T> Publisher<T> onCreate(Publisher<T> publisher) {
        return (Publisher)((Function)onCreateHandler).apply(publisher);
    }
    /**
     * Called when an undeliverable error occurs.
     * @param error the error to report
     */
    public static void onError(Throwable error) {
        try {
            errorHandler.accept(error);
        } catch (Throwable e) {
            error.addSuppressed(e);
            DEFAULT_ERROR_HANDLER.accept(e);
        }
    }
    
    public static Scheduler onIOScheduler(Scheduler defaultScheduler) {
        return onIOHandler.apply(defaultScheduler);
    }

    /**
     * Called when a task is scheduled.
     * @param run
     * @return
     */
    public static Runnable onSchedule(Runnable run) {
        return onScheduleHandler.apply(run);
    }

    public static Scheduler onSingleScheduler(Scheduler defaultScheduler) {
        return onSingleHandler.apply(defaultScheduler);
    }

    /**
     * Called when a subscriber subscribes to an observable.
     * @param subscriber
     * @return
     */
    @SuppressWarnings({ "unchecked", "rawtypes"})
    public static <T> Subscriber<T> onSubscribe(Subscriber<T> subscriber) {
        return (Subscriber)((Function)onSubscribeHandler).apply(subscriber);
    }

    /**
     * Removes all handlers and resets the default behavior.
     */
    public static void reset() {
        setCreateHandler(null);
        setErrorHandler(null);
        setScheduleHandler(null);
        setSubscribeHandler(null);
        
        setComputationSchedulerHandler(null);
        setInitComputationSchedulerHandler(null);
        setInitIOSchedulerHandler(null);
        setIOSchedulerHandler(null);
        setInitSingleSchedulerHandler(null);
        setSingleSchedulerHandler(null);
    }

    public static void setComputationSchedulerHandler(Function<Scheduler, Scheduler> handler) {
        if (handler == null) {
            onComputationHandler = s -> s;
        } else {
            onComputationHandler = handler;
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static <T> void setCreateHandler(Function<Publisher<T>, Publisher<T>> handler) {
        if (handler == null) {
            onCreateHandler = p -> p;
        } else {
            onCreateHandler = (Function)handler;
        }
    }

    public static void setErrorHandler(Consumer<Throwable> handler) {
        if (handler == null) {
            errorHandler = DEFAULT_ERROR_HANDLER;
        } else {
            errorHandler = handler;
        }
    }

    public static void setInitComputationSchedulerHandler(Function<Scheduler, Scheduler> handler) {
        if (handler == null) {
            onInitComputationHandler = s -> s;
        } else {
            onInitComputationHandler = handler;
        }
    }

    public static void setInitIOSchedulerHandler(Function<Scheduler, Scheduler> handler) {
        if (handler == null) {
            onInitIOHandler = s -> s;
        } else {
            onInitIOHandler = handler;
        }
    }

    public static void setInitSingleSchedulerHandler(Function<Scheduler, Scheduler> handler) {
        if (handler == null) {
            onInitSingleHandler = s -> s;
        } else {
            onInitSingleHandler = handler;
        }
    }

    public static void setIOSchedulerHandler(Function<Scheduler, Scheduler> handler) {
        if (handler == null) {
            onIOHandler = s -> s;
        } else {
            onIOHandler = handler;
        }
    }

    public static void setScheduleHandler(Function<Runnable, Runnable> handler) {
        if (handler == null) {
            onScheduleHandler = r -> r;
        } else {
            onScheduleHandler = handler;
        }
    }

    public static void setSingleSchedulerHandler(Function<Scheduler, Scheduler> handler) {
        if (handler == null) {
            onSingleHandler = s -> s;
        } else {
            onSingleHandler = handler;
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static <T> void setSubscribeHandler(Function<Subscriber<T>, Subscriber<T>> handler) {
        if (handler == null) {
            onSubscribeHandler = e -> e;
        } else {
            onSubscribeHandler = (Function)handler;
        }
    }

    private RxJavaPlugins() {
        throw new IllegalStateException("No instances!");
    }
}
