/**
 * Copyright 2014 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package rx.internal.schedulers;

import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

import rx.*;
import rx.exceptions.Exceptions;
import rx.functions.Action0;
import rx.internal.util.*;
import rx.plugins.*;
import rx.subscriptions.*;

import static rx.internal.util.PlatformDependent.ANDROID_API_VERSION_IS_NOT_ANDROID;

/**
 * @warn class description missing
 */
public class NewThreadWorker extends Scheduler.Worker implements Subscription {
    private final ScheduledExecutorService executor;
    private final RxJavaSchedulersHook schedulersHook;
    volatile boolean isUnsubscribed;
    /** The purge frequency in milliseconds. */
    private static final String FREQUENCY_KEY = "rx.scheduler.jdk6.purge-frequency-millis";
    /** Force the use of purge (true/false). */
    private static final String PURGE_FORCE_KEY = "rx.scheduler.jdk6.purge-force";
    private static final String PURGE_THREAD_PREFIX = "RxSchedulerPurge-";
    private static final boolean SHOULD_TRY_ENABLE_CANCEL_POLICY;
    /** The purge frequency in milliseconds. */
    public static final int PURGE_FREQUENCY;
    private static final ConcurrentHashMap<ScheduledThreadPoolExecutor, ScheduledThreadPoolExecutor> EXECUTORS;
    private static final AtomicReference<ScheduledExecutorService> PURGE;
    static {
        EXECUTORS = new ConcurrentHashMap<ScheduledThreadPoolExecutor, ScheduledThreadPoolExecutor>();
        PURGE = new AtomicReference<ScheduledExecutorService>();
        PURGE_FREQUENCY = Integer.getInteger(FREQUENCY_KEY, 1000);

        // Forces the use of purge even if setRemoveOnCancelPolicy is available
        final boolean purgeForce = Boolean.getBoolean(PURGE_FORCE_KEY);

        final int androidApiVersion = PlatformDependent.getAndroidApiVersion();

        // According to http://developer.android.com/reference/java/util/concurrent/ScheduledThreadPoolExecutor.html#setRemoveOnCancelPolicy(boolean)
        // setRemoveOnCancelPolicy available since Android API 21
        SHOULD_TRY_ENABLE_CANCEL_POLICY = !purgeForce
                && (androidApiVersion == ANDROID_API_VERSION_IS_NOT_ANDROID || androidApiVersion >= 21);
    }
    /** 
     * Registers the given executor service and starts the purge thread if not already started. 
     * <p>{@code public} visibility reason: called from other package(s) within RxJava
     * @param service a scheduled thread pool executor instance 
     */
    public static void registerExecutor(ScheduledThreadPoolExecutor service) {
        do {
            ScheduledExecutorService exec = PURGE.get();
            if (exec != null) {
                break;
            }
            exec = Executors.newScheduledThreadPool(1, new RxThreadFactory(PURGE_THREAD_PREFIX));
            if (PURGE.compareAndSet(null, exec)) {
                exec.scheduleAtFixedRate(new Runnable() {
                    @Override
                    public void run() {
                        purgeExecutors();
                    }
                }, PURGE_FREQUENCY, PURGE_FREQUENCY, TimeUnit.MILLISECONDS);
                
                break;
            } else {
                exec.shutdownNow();
            }
        } while (true);
        
        EXECUTORS.putIfAbsent(service, service);
    }
    /** 
     * Deregisters the executor service. 
     * <p>{@code public} visibility reason: called from other package(s) within RxJava
     * @param service a scheduled thread pool executor instance 
     */
    public static void deregisterExecutor(ScheduledExecutorService service) {
        EXECUTORS.remove(service);
    }

    /** Purges each registered executor and eagerly evicts shutdown executors. */
    static void purgeExecutors() {
        try {
            Iterator<ScheduledThreadPoolExecutor> it = EXECUTORS.keySet().iterator();
            while (it.hasNext()) {
                ScheduledThreadPoolExecutor exec = it.next();
                if (!exec.isShutdown()) {
                    exec.purge();
                } else {
                    it.remove();
                }
            }
        } catch (Throwable t) {
            Exceptions.throwIfFatal(t);
            RxJavaPlugins.getInstance().getErrorHandler().handleError(t);
        }
    }

    /**
     * Improves performance of {@link #tryEnableCancelPolicy(ScheduledExecutorService)}.
     * Also, it works even for inheritance: {@link Method} of base class can be invoked on the instance of child class.
     */
    private static volatile Object cachedSetRemoveOnCancelPolicyMethod;

    /**
     * Possible value of {@link #cachedSetRemoveOnCancelPolicyMethod} which means that cancel policy is not supported.
     */
     private static final Object SET_REMOVE_ON_CANCEL_POLICY_METHOD_NOT_SUPPORTED = new Object();

    /**
     * Tries to enable the Java 7+ setRemoveOnCancelPolicy.
     * <p>{@code public} visibility reason: called from other package(s) within RxJava.
     * If the method returns false, the {@link #registerExecutor(ScheduledThreadPoolExecutor)} may
     * be called to enable the backup option of purging the executors.
     * @param executor the executor to call setRemoveOnCaneclPolicy if available.
     * @return true if the policy was successfully enabled 
     */
    public static boolean tryEnableCancelPolicy(ScheduledExecutorService executor) {
        if (SHOULD_TRY_ENABLE_CANCEL_POLICY) {
            final boolean isInstanceOfScheduledThreadPoolExecutor = executor instanceof ScheduledThreadPoolExecutor;

            final Method methodToCall;

            if (isInstanceOfScheduledThreadPoolExecutor) {
                final Object localSetRemoveOnCancelPolicyMethod = cachedSetRemoveOnCancelPolicyMethod;

                if (localSetRemoveOnCancelPolicyMethod == SET_REMOVE_ON_CANCEL_POLICY_METHOD_NOT_SUPPORTED) {
                    return false;
                }

                if (localSetRemoveOnCancelPolicyMethod == null) {
                    Method method = findSetRemoveOnCancelPolicyMethod(executor);

                    cachedSetRemoveOnCancelPolicyMethod = method != null
                            ? method
                            : SET_REMOVE_ON_CANCEL_POLICY_METHOD_NOT_SUPPORTED;

                    methodToCall = method;
                } else {
                    methodToCall = (Method) localSetRemoveOnCancelPolicyMethod;
                }
            } else {
                methodToCall = findSetRemoveOnCancelPolicyMethod(executor);
            }

            if (methodToCall != null) {
                try {
                    methodToCall.invoke(executor, true);
                    return true;
                } catch (Exception e) {
                    RxJavaPlugins.getInstance().getErrorHandler().handleError(e);
                }
            }
        }

        return false;
    }

    /**
     * Tries to find {@code "setRemoveOnCancelPolicy(boolean)"} method in the class of passed executor.
     *
     * @param executor whose class will be used to search for required method.
     * @return {@code "setRemoveOnCancelPolicy(boolean)"} {@link Method}
     * or {@code null} if required {@link Method} was not found.
     */
    static Method findSetRemoveOnCancelPolicyMethod(ScheduledExecutorService executor) {
        // The reason for the loop is to avoid NoSuchMethodException being thrown on JDK 6
        // which is more costly than looping through ~70 methods.
        for (final Method method : executor.getClass().getMethods()) {
            if (method.getName().equals("setRemoveOnCancelPolicy")) {
                final Class<?>[] parameterTypes = method.getParameterTypes();

                if (parameterTypes.length == 1 && parameterTypes[0] == Boolean.TYPE) {
                    return method;
                }
            }
        }

        return null;
    }
    
    /* package */
    public NewThreadWorker(ThreadFactory threadFactory) {
        ScheduledExecutorService exec = Executors.newScheduledThreadPool(1, threadFactory);
        // Java 7+: cancelled future tasks can be removed from the executor thus avoiding memory leak
        boolean cancelSupported = tryEnableCancelPolicy(exec);
        if (!cancelSupported && exec instanceof ScheduledThreadPoolExecutor) {
            registerExecutor((ScheduledThreadPoolExecutor)exec);
        }
        schedulersHook = RxJavaPlugins.getInstance().getSchedulersHook();
        executor = exec;
    }

    @Override
    public Subscription schedule(final Action0 action) {
        return schedule(action, 0, null);
    }

    @Override
    public Subscription schedule(final Action0 action, long delayTime, TimeUnit unit) {
        if (isUnsubscribed) {
            return Subscriptions.unsubscribed();
        }
        return scheduleActual(action, delayTime, unit);
    }

    /**
     * Schedules the given action by wrapping it into a ScheduledAction on the
     * underlying ExecutorService, returning the ScheduledAction. 
     * @param action the action to wrap and schedule
     * @param delayTime the delay in execution
     * @param unit the time unit of the delay
     * @return the wrapper ScheduledAction
     */
    public ScheduledAction scheduleActual(final Action0 action, long delayTime, TimeUnit unit) {
        Action0 decoratedAction = schedulersHook.onSchedule(action);
        ScheduledAction run = new ScheduledAction(decoratedAction);
        Future<?> f;
        if (delayTime <= 0) {
            f = executor.submit(run);
        } else {
            f = executor.schedule(run, delayTime, unit);
        }
        run.add(f);

        return run;
    }
    public ScheduledAction scheduleActual(final Action0 action, long delayTime, TimeUnit unit, CompositeSubscription parent) {
        Action0 decoratedAction = schedulersHook.onSchedule(action);
        ScheduledAction run = new ScheduledAction(decoratedAction, parent);
        parent.add(run);

        Future<?> f;
        if (delayTime <= 0) {
            f = executor.submit(run);
        } else {
            f = executor.schedule(run, delayTime, unit);
        }
        run.add(f);

        return run;
    }
    
    public ScheduledAction scheduleActual(final Action0 action, long delayTime, TimeUnit unit, SubscriptionList parent) {
        Action0 decoratedAction = schedulersHook.onSchedule(action);
        ScheduledAction run = new ScheduledAction(decoratedAction, parent);
        parent.add(run);
        
        Future<?> f;
        if (delayTime <= 0) {
            f = executor.submit(run);
        } else {
            f = executor.schedule(run, delayTime, unit);
        }
        run.add(f);

        return run;
    }

    @Override
    public void unsubscribe() {
        isUnsubscribed = true;
        executor.shutdownNow();
        deregisterExecutor(executor);
    }

    @Override
    public boolean isUnsubscribed() {
        return isUnsubscribed;
    }
}
