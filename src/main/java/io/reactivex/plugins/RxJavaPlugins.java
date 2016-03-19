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

import org.reactivestreams.*;

import io.reactivex.*;
import io.reactivex.functions.*;

/**
 * Utility class to inject handlers to certain standard RxJava operations.
 */
public final class RxJavaPlugins {
    
    static volatile Consumer<Throwable> errorHandler;
    
    static volatile Function<Subscriber<Object>, Subscriber<Object>> onSubscribeHandler;

    static volatile Function<Observer<Object>, Observer<Object>> onNbpSubscribeHandler;

    static volatile Function<Publisher<Object>, Publisher<Object>> onCreateHandler;

    static volatile Function<Runnable, Runnable> onScheduleHandler;

    static volatile Function<Scheduler, Scheduler> onInitComputationHandler;
    
    static volatile Function<Scheduler, Scheduler> onInitSingleHandler;
    
    static volatile Function<Scheduler, Scheduler> onInitIOHandler;

    static volatile Function<Scheduler, Scheduler> onInitNewThreadHandler;
    
    static volatile Function<Scheduler, Scheduler> onComputationHandler;
    
    static volatile Function<Scheduler, Scheduler> onSingleHandler;
    
    static volatile Function<Scheduler, Scheduler> onIOHandler;

    static volatile Function<Scheduler, Scheduler> onNewThreadHandler;

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

    public static Function<Scheduler, Scheduler> getInitNewThreadSchedulerHandler() {
        return onInitNewThreadHandler;
    }

    public static Function<Scheduler, Scheduler> getInitSingleSchedulerHandler() {
        return onInitSingleHandler;
    }

    public static Function<Scheduler, Scheduler> getIOSchedulerHandler() {
        return onIOHandler;
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
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static <T> Function<Subscriber<T>, Subscriber<T>> getSubscribeHandler() {
        return (Function)onSubscribeHandler;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static <T> Function<Observer<T>, Observer<T>> getNbpSubscribeHandler() {
        return (Function)onNbpSubscribeHandler;
    }

    public static Scheduler initComputationScheduler(Scheduler defaultScheduler) {
        Function<Scheduler, Scheduler> f = onInitComputationHandler;
        if (f == null) {
            return defaultScheduler;
        }
        return f.apply(defaultScheduler); // JIT will skip this
    }

    public static Scheduler initIOScheduler(Scheduler defaultScheduler) {
        Function<Scheduler, Scheduler> f = onInitIOHandler;
        if (f == null) {
            return defaultScheduler;
        }
        return f.apply(defaultScheduler);
    }

    public static Scheduler initNewThreadScheduler(Scheduler defaultScheduler) {
        Function<Scheduler, Scheduler> f = onInitNewThreadHandler;
        if (f == null) {
            return defaultScheduler;
        }
        return f.apply(defaultScheduler);
    }

    public static Scheduler initSingleScheduler(Scheduler defaultScheduler) {
        Function<Scheduler, Scheduler> f = onInitSingleHandler;
        if (f == null) {
            return defaultScheduler;
        }
        return f.apply(defaultScheduler);
    }

    public static Scheduler onComputationScheduler(Scheduler defaultScheduler) {
        Function<Scheduler, Scheduler> f = onComputationHandler;
        if (f == null) {
            return defaultScheduler;
        }
        return f.apply(defaultScheduler);
    }
    /**
     * Called when an Observable is created.
     * @param <T> the value type
     * @param publisher the original publisher
     * @return the replacement publisher
     */
    @SuppressWarnings({ "unchecked", "rawtypes"})
    public static <T> Publisher<T> onCreate(Publisher<T> publisher) {
        Function<Publisher<Object>, Publisher<Object>> f = onCreateHandler;
        if (f == null) {
            return publisher;
        }
        return (Publisher)((Function)f).apply(publisher);
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
                e.printStackTrace();
            }
        } else {
            if (error == null) {
                error = new NullPointerException();
            }
        }
        error.printStackTrace();
    }
    
    public static Scheduler onIOScheduler(Scheduler defaultScheduler) {
        Function<Scheduler, Scheduler> f = onIOHandler;
        if (f == null) {
            return defaultScheduler;
        }
        return f.apply(defaultScheduler);
    }

    public static Scheduler onNewThreadScheduler(Scheduler defaultScheduler) {
        Function<Scheduler, Scheduler> f = onNewThreadHandler;
        if (f == null) {
            return defaultScheduler;
        }
        return f.apply(defaultScheduler);
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
        return f.apply(run);
    }

    public static Scheduler onSingleScheduler(Scheduler defaultScheduler) {
        Function<Scheduler, Scheduler> f = onSingleHandler;
        if (f == null) {
            return defaultScheduler;
        }
        return f.apply(defaultScheduler);
    }

    /**
     * Called when a subscriber subscribes to an observable.
     * @param <T> the value type
     * @param subscriber the original subscriber
     * @return the subscriber replacement
     */
    @SuppressWarnings({ "unchecked", "rawtypes"})
    public static <T> Subscriber<T> onSubscribe(Subscriber<T> subscriber) {
        Function<Subscriber<Object>, Subscriber<Object>> f = onSubscribeHandler;
        if (f == null) {
            return subscriber;
        }
        return (Subscriber)((Function)f).apply(subscriber);
    }

    /**
     * Called when a subscriber subscribes to an observable.
     * @param <T> the value type
     * @param subscriber the original NbpSubscriber
     * @return the replacement NbpSubscriber
     */
    @SuppressWarnings({ "unchecked", "rawtypes"})
    public static <T> Observer<T> onNbpSubscribe(Observer<T> subscriber) {
        Function<Observer<Object>, Observer<Object>> f = onNbpSubscribeHandler;
        if (f == null) {
            return subscriber;
        }
        return (Observer)((Function)f).apply(subscriber);
    }

    /**
     * Called when a subscriber subscribes to an observable.
     * @param <T> the value type
     * @param subscriber the original subscriber
     * @return the replacement subscriber
     */
    @SuppressWarnings({ "unchecked", "rawtypes"})
    public static <T> Observer<T> onSubscribe(Observer<T> subscriber) {
        Function<Observer<Object>, Observer<Object>> f = onNbpSubscribeHandler;
        if (f == null) {
            return subscriber;
        }
        return (Observer)((Function)f).apply(subscriber);
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
        
        setIOSchedulerHandler(null);
        setInitIOSchedulerHandler(null);

        setSingleSchedulerHandler(null);
        setInitSingleSchedulerHandler(null);

        setNewThreadSchedulerHandler(null);
        setInitNewThreadSchedulerHandler(null);
    }

    public static void setComputationSchedulerHandler(Function<Scheduler, Scheduler> handler) {
        if (lockdown) {
            throw new IllegalStateException("Plugins can't be changed anymore");
        }
        onComputationHandler = handler;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static <T> void setCreateHandler(Function<Publisher<T>, Publisher<T>> handler) {
        if (lockdown) {
            throw new IllegalStateException("Plugins can't be changed anymore");
        }
        onCreateHandler = (Function)handler;
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

    public static void setInitIOSchedulerHandler(Function<Scheduler, Scheduler> handler) {
        if (lockdown) {
            throw new IllegalStateException("Plugins can't be changed anymore");
        }
        onInitIOHandler = handler;
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

    public static void setIOSchedulerHandler(Function<Scheduler, Scheduler> handler) {
        if (lockdown) {
            throw new IllegalStateException("Plugins can't be changed anymore");
        }
        onIOHandler = handler;
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

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static <T> void setSubscribeHandler(Function<Subscriber<T>, Subscriber<T>> handler) {
        if (lockdown) {
            throw new IllegalStateException("Plugins can't be changed anymore");
        }
        onSubscribeHandler = (Function)handler;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static <T> void setNbpSubscribeHandler(Function<Observer<T>, Observer<T>> handler) {
        if (lockdown) {
            throw new IllegalStateException("Plugins can't be changed anymore");
        }
        onNbpSubscribeHandler = (Function)handler;
    }

    /**
     * Rewokes the lockdown, only for testing purposes.
     */
    /* test. */static void unlock() {
        lockdown = false;
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
    public static final Consumer<Throwable> errorConsumer() {
        return CONSUME_BY_RXJAVA_PLUGIN;
    }
    
    /** Helper class, no instances. */
    private RxJavaPlugins() {
        throw new IllegalStateException("No instances!");
    }
}
