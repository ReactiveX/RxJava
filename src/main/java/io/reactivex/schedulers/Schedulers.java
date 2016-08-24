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

package io.reactivex.schedulers;

import java.util.concurrent.Executor;

import io.reactivex.Scheduler;
import io.reactivex.internal.schedulers.*;
import io.reactivex.plugins.RxJavaPlugins;

/**
 * Static factory methods for returning standard Scheduler instances.
 */
public final class Schedulers {
    static final Scheduler SINGLE;
    
    static final Scheduler COMPUTATION;
    
    static final Scheduler IO;
    
    static final Scheduler TRAMPOLINE;
    
    static final Scheduler NEW_THREAD;
    
    static {
        SINGLE = RxJavaPlugins.initSingleScheduler(new SingleScheduler());
        
        COMPUTATION = RxJavaPlugins.initComputationScheduler(new ComputationScheduler());
        
        IO = RxJavaPlugins.initIoScheduler(new IoScheduler());
        
        TRAMPOLINE = TrampolineScheduler.instance();
        
        NEW_THREAD = RxJavaPlugins.initNewThreadScheduler(NewThreadScheduler.instance());
    }
    
    private Schedulers() {
        throw new IllegalStateException("No instances");
    }
    
    /**
     * Creates and returns a {@link Scheduler} intended for computational work.
     * <p>
     * This can be used for event-loops, processing callbacks and other computational work.
     * <p>
     * Do not perform IO-bound work on this scheduler. Use {@link #io()} instead.
     * <p>
     * Unhandled errors will be delivered to the scheduler Thread's {@link java.lang.Thread.UncaughtExceptionHandler}.
     *
     * @return a {@link Scheduler} meant for computation-bound work
     */
    public static Scheduler computation() {
        return RxJavaPlugins.onComputationScheduler(COMPUTATION);
    }
    
    /**
     * Creates and returns a {@link Scheduler} intended for IO-bound work.
     * <p>
     * The implementation is backed by an {@link Executor} thread-pool that will grow as needed.
     * <p>
     * This can be used for asynchronously performing blocking IO.
     * <p>
     * Do not perform computational work on this scheduler. Use {@link #computation()} instead.
     * <p>
     * Unhandled errors will be delivered to the scheduler Thread's {@link java.lang.Thread.UncaughtExceptionHandler}.
     *
     * @return a {@link Scheduler} meant for IO-bound work
     */
    public static Scheduler io() {
        return RxJavaPlugins.onIoScheduler(IO);
    }

    /**
     * Creates and returns a {@link Scheduler} that queues work on the current thread to be executed after the
     * current work completes.
     *
     * @return a {@link Scheduler} that queues work on the current thread
     */
    public static Scheduler trampoline() {
        return TRAMPOLINE;
    }

    /**
     * Creates and returns a {@link Scheduler} that creates a new {@link Thread} for each unit of work.
     * <p>
     * Unhandled errors will be delivered to the scheduler Thread's {@link java.lang.Thread.UncaughtExceptionHandler}.
     *
     * @return a {@link Scheduler} that creates new threads
     */
    public static Scheduler newThread() {
        return RxJavaPlugins.onNewThreadScheduler(NEW_THREAD);
    }
    
    /**
     * Returns the common, single-thread backed Scheduler instance.
     * <p>
     * Uses:
     * <ul>
     * <li>main event loop</li>
     * <li>support Schedulers.from(Executor) and from(ExecutorService) with delayed scheduling</li>
     * <li>support benchmarks that pipeline data from the main thread to some other thread and 
     * avoid core-bashing of computation's round-robin nature</li>
     * </ul>
     * @return a {@link Scheduler} that shares a single backing thread.
     * @since 2.0
     */
    public static Scheduler single() {
        return RxJavaPlugins.onSingleScheduler(SINGLE);
    }
    
    /**
     * Converts an {@link Executor} into a new Scheduler instance.
     *
     * @param executor
     *          the executor to wrap
     * @return the new Scheduler wrapping the Executor
     */
    public static Scheduler from(Executor executor) {
        return new ExecutorScheduler(executor);
    }
    
    /**
     * Shuts down those standard Schedulers which support the SchedulerLifecycle interface.
     * <p>The operation is idempotent and thread-safe.
     */
    public static void shutdown() {
        computation().shutdown();
        io().shutdown();
        newThread().shutdown();
        single().shutdown();
        trampoline().shutdown();
        SchedulerPoolFactory.shutdown();
    }
    
    /**
     * Starts those standard Schedulers which support the SchedulerLifecycle interface.
     * <p>The operation is idempotent and thread-safe.
     */
    public static void start() {
        computation().start();
        io().start();
        newThread().start();
        single().start();
        trampoline().start();
        SchedulerPoolFactory.start();
    }
}
