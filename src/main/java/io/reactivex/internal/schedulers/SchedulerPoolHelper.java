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

package io.reactivex.internal.schedulers;

import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.internal.util.Exceptions;

/**
 * Manages the purging of cancelled and delayed tasks considering platform specifics.
 */
public enum SchedulerPoolHelper {
    ;
    
    /** Key to force purging instead of using removeOnCancelPolicy if available. */
    private static final String FORCE_PURGE_KEY = "rx2.scheduler.purge-force";
    /**
     * Force periodic purging instead of removeOnCancelPolicy even if available.
     * Default {@code false}.
     */
    private static volatile boolean FORCE_PURGE;
    
    /** Key to the purge frequency parameter in milliseconds. */
    private static final String PURGE_FREQUENCY_KEY = "rx2.scheduler.purge-frequency";
    /** The purge frequency in milliseconds. */
    private static volatile int PURGE_FREQUENCY;
    /**
     * Holds onto the ScheduledExecutorService that periodically purges all known
     * ScheduledThreadPoolExecutors in POOLS.
     */
    static final AtomicReference<ScheduledExecutorService> PURGE_THREAD;
    /**
     * Holds onto the created ScheduledThreadPoolExecutors by this helper.
     */
    static final Map<ScheduledThreadPoolExecutor, ScheduledExecutorService> POOLS;
    
    /**
     * The reflective method used for setting the removeOnCancelPolicy (JDK 6 safe way).
     */
    static final Method SET_REMOVE_ON_CANCEL_POLICY;

    static final ThreadFactory PURGE_THREAD_FACTORY;
    /**
     * Initializes the static fields and figures out the settings for purging.
     */
    static {
        PURGE_THREAD = new AtomicReference<>();
        POOLS = new ConcurrentHashMap<>();
        PURGE_THREAD_FACTORY = new RxThreadFactory("RxSchedulerPurge-");

        Properties props = System.getProperties();
        
        boolean forcePurgeValue = false;
        Method removeOnCancelMethod = null;

        // this is necessary because force is turned on and off by tests on desktop
        try {
            removeOnCancelMethod = ScheduledThreadPoolExecutor.class.getMethod("setRemoveOnCancelPolicy", Boolean.TYPE);
        } catch (NoSuchMethodException | SecurityException e) {
            // if not present, no problem
            forcePurgeValue = true;
        }
        
        if (!forcePurgeValue && props.containsKey(FORCE_PURGE_KEY)) {
            forcePurgeValue = Boolean.getBoolean(FORCE_PURGE_KEY);
        }
        
        PURGE_FREQUENCY = Integer.getInteger(PURGE_FREQUENCY_KEY, 2000);
        
        FORCE_PURGE = forcePurgeValue;
        SET_REMOVE_ON_CANCEL_POLICY = removeOnCancelMethod;
        start();
    }
    
    /**
     * Returns the status of the force-purge settings.
     * @return the force purge settings
     */
    public static boolean forcePurge() {
        return FORCE_PURGE;
    }
    
    /**
     * Sets the force-purge settings.
     * <p>Note that enabling or disabling the force-purge by itself doesn't apply to 
     * existing schedulers and they have to be restarted. 
     * @param force the new force state
     */
    /* test */
    public static void forcePurge(boolean force) {
        FORCE_PURGE = force;
    }
    
    /**
     * Returns purge frequency in milliseconds.
     * @return purge frequency in milliseconds
     */
    public static int purgeFrequency() {
        return PURGE_FREQUENCY;
    }
    
    /**
     * Returns true if the platform supports removeOnCancelPolicy.
     * @return true if the platform supports removeOnCancelPolicy.
     */
    public static boolean isRemoveOnCancelPolicySupported() {
        return SET_REMOVE_ON_CANCEL_POLICY != null;
    }
    
    /**
     * Creates a single threaded ScheduledExecutorService and wires up all
     * necessary purging or removeOnCancelPolicy settings with it.
     * @param factory the thread factory to use
     * @return the created ScheduledExecutorService
     * @throws IllegalStateException if force-purge is not enabled yet the platform doesn't support removeOnCancelPolicy;
     * or Executors.newScheduledThreadPool doesn't return a ScheduledThreadPoolExecutor.
     */
    public static ScheduledExecutorService create(ThreadFactory factory) {
        ScheduledExecutorService exec = Executors.newScheduledThreadPool(1, factory);
        if (FORCE_PURGE) {
            if (exec instanceof ScheduledThreadPoolExecutor) {
                ScheduledThreadPoolExecutor e = (ScheduledThreadPoolExecutor) exec;
                POOLS.put(e, e);
            } else {
                throw new IllegalStateException("The Executors.newScheduledThreadPool didn't return a ScheduledThreadPoolExecutor.");
            }
        } else {
            Method m = SET_REMOVE_ON_CANCEL_POLICY;
            if (m == null) {
                throw new IllegalStateException("The ScheduledThreadPoolExecutor doesn't support the removeOnCancelPolicy and purging is not enabled.");
            }
            try {
                m.invoke(exec, true);
            } catch (IllegalAccessException | InvocationTargetException e) {
                Exceptions.propagate(e);
            }
        }
        
        return exec;
    }
    
    /**
     * Starts the purge thread and the periodic purging.
     */
    public static void start() {
        // if purge is not enabled don't do anything
        if (!FORCE_PURGE) {
            return;
        }
        for (;;) {
            ScheduledExecutorService curr = PURGE_THREAD.get();
            if (curr != null) {
                return;
            }
            ScheduledExecutorService next = Executors.newScheduledThreadPool(1, PURGE_THREAD_FACTORY);
            if (PURGE_THREAD.compareAndSet(null, next)) {
                
                next.scheduleAtFixedRate(SchedulerPoolHelper::doPurge, 
                        PURGE_FREQUENCY, PURGE_FREQUENCY, TimeUnit.MILLISECONDS);
                
                return;
            } else {
                next.shutdownNow();
            }
        }
    }
    
    /**
     * Shuts down the purge thread and forgets the known ScheduledExecutorServices.
     * <p>Note that this stops purging the known ScheduledExecutorServices which may be shut
     * down as well to appreciate a new forcePurge state.
     */
    public static void shutdown() {
        shutdown(true);
    }
    /**
     * Shuts down the purge thread and clears the known ScheduledExecutorServices from POOLS when
     * requested.
     * <p>Note that this stops purging the known ScheduledExecutorServices which may be shut
     * down as well to appreciate a new forcePurge state.
     * @param clear if true, the helper forgets all associated ScheduledExecutorServices
     */
    public static void shutdown(boolean clear) {
        for (;;) {
            ScheduledExecutorService curr = PURGE_THREAD.get();
            if (curr == null) {
                return;
            }
            if (PURGE_THREAD.compareAndSet(curr, null)) {
                curr.shutdownNow();
                if (clear) {
                    POOLS.clear();
                }
                return;
            }
        }
    }
    
    /**
     * Loops through the known ScheduledExecutors and removes the ones that were shut down
     * and purges the others
     */
    static void doPurge() {
        try {
            for (ScheduledThreadPoolExecutor e : new ArrayList<>(POOLS.keySet())) {
                if (e.isShutdown()) {
                    POOLS.remove(e);
                } else {
                    e.purge();
                }
            }
        } catch (Throwable ex) {
            // ignoring any error, just in case
        }
    }
    
    /**
     * Purges all known ScheduledExecutorServices immediately on the purge thread.
     */
    public static void purgeAsync() {
        ScheduledExecutorService exec = PURGE_THREAD.get();
        if (exec != null) {
            try {
                exec.submit(SchedulerPoolHelper::doPurge);
            } catch (RejectedExecutionException ex) {
                // ignored, we are in shutdown
            }
        }
    }
}
