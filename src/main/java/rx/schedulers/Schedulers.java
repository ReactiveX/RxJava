/**
 * Copyright 2014 Netflix, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package rx.schedulers;

import rx.Scheduler;
import rx.annotations.Experimental;
import rx.internal.schedulers.ExecutorScheduler;
import rx.internal.schedulers.GenericScheduledExecutorService;
import rx.internal.schedulers.SchedulerLifecycle;
import rx.internal.util.RxRingBuffer;
import rx.plugins.RxJavaPlugins;
import rx.plugins.RxJavaSchedulersHook;

import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Static factory methods for creating Schedulers.
 */
public final class Schedulers {

    private final Scheduler computationScheduler;
    private final Scheduler ioScheduler;
    private final Scheduler newThreadScheduler;

    private static final AtomicReference<Schedulers> INSTANCE = new AtomicReference<Schedulers>();

    private static Schedulers getInstance() {
        for (;;) {
            Schedulers current = INSTANCE.get();
            if (current != null) {
                return current;
            }
            current = new Schedulers();
            if (INSTANCE.compareAndSet(null, current)) {
                return current;
            } else {
                current.shutdownInstance();
            }
        }
    }

    private Schedulers() {
        RxJavaSchedulersHook hook = RxJavaPlugins.getInstance().getSchedulersHook();

        Scheduler c = hook.getComputationScheduler();
        if (c != null) {
            computationScheduler = c;
        } else {
            computationScheduler = RxJavaSchedulersHook.createComputationScheduler();
        }

        Scheduler io = hook.getIOScheduler();
        if (io != null) {
            ioScheduler = io;
        } else {
            ioScheduler = RxJavaSchedulersHook.createIoScheduler();
        }

        Scheduler nt = hook.getNewThreadScheduler();
        if (nt != null) {
            newThreadScheduler = nt;
        } else {
            newThreadScheduler = RxJavaSchedulersHook.createNewThreadScheduler();
        }
    }

    /**
     * Creates and returns a {@link Scheduler} that executes work immediately on the current thread.
     *
     * @return a {@link Scheduler} that executes work immediately
     */
    public static Scheduler immediate() {
        return rx.internal.schedulers.ImmediateScheduler.INSTANCE;
    }

    /**
     * Creates and returns a {@link Scheduler} that queues work on the current thread to be executed after the
     * current work completes.
     *
     * @return a {@link Scheduler} that queues work on the current thread
     */
    public static Scheduler trampoline() {
        return rx.internal.schedulers.TrampolineScheduler.INSTANCE;
    }

    /**
     * Creates and returns a {@link Scheduler} that creates a new {@link Thread} for each unit of work.
     * <p>
     * Unhandled errors will be delivered to the scheduler Thread's {@link java.lang.Thread.UncaughtExceptionHandler}.
     *
     * @return a {@link Scheduler} that creates new threads
     */
    public static Scheduler newThread() {
        return getInstance().newThreadScheduler;
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
        return getInstance().computationScheduler;
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
        return getInstance().ioScheduler;
    }

    /**
     * Creates and returns a {@code TestScheduler}, which is useful for debugging. It allows you to test
     * schedules of events by manually advancing the clock at whatever pace you choose.
     *
     * @return a {@code TestScheduler} meant for debugging
     */
    public static TestScheduler test() {
        return new TestScheduler();
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
     * Resets the current {@link Schedulers} instance.
     * This will re-init the cached schedulers on the next usage,
     * which can be useful in testing.
     */
    @Experimental
    public static void reset() {
        Schedulers s = INSTANCE.getAndSet(null);
        if (s != null) {
            s.shutdownInstance();
        }
    }
    
    /**
     * Starts those standard Schedulers which support the SchedulerLifecycle interface.
     * <p>The operation is idempotent and threadsafe.
     */
    /* public test only */ static void start() {
        Schedulers s = getInstance();
        
        s.startInstance();
        
        synchronized (s) {
            GenericScheduledExecutorService.INSTANCE.start();
            
            RxRingBuffer.SPSC_POOL.start();
            
            RxRingBuffer.SPMC_POOL.start();
        }
    }
    /**
     * Shuts down those standard Schedulers which support the SchedulerLifecycle interface.
     * <p>The operation is idempotent and threadsafe.
     */
    public static void shutdown() {
        Schedulers s = getInstance();
        s.shutdownInstance();

        synchronized (s) {
            GenericScheduledExecutorService.INSTANCE.shutdown();
            
            RxRingBuffer.SPSC_POOL.shutdown();

            RxRingBuffer.SPMC_POOL.shutdown();
        }
    }
    
    /**
     * Start the instance-specific schedulers.
     */
    synchronized void startInstance() {
        if (computationScheduler instanceof SchedulerLifecycle) {
            ((SchedulerLifecycle) computationScheduler).start();
        }
        if (ioScheduler instanceof SchedulerLifecycle) {
            ((SchedulerLifecycle) ioScheduler).start();
        }
        if (newThreadScheduler instanceof SchedulerLifecycle) {
            ((SchedulerLifecycle) newThreadScheduler).start();
        }
    }
    
    /**
     * Start the instance-specific schedulers.
     */
    synchronized void shutdownInstance() {
        if (computationScheduler instanceof SchedulerLifecycle) {
            ((SchedulerLifecycle) computationScheduler).shutdown();
        }
        if (ioScheduler instanceof SchedulerLifecycle) {
            ((SchedulerLifecycle) ioScheduler).shutdown();
        }
        if (newThreadScheduler instanceof SchedulerLifecycle) {
            ((SchedulerLifecycle) newThreadScheduler).shutdown();
        }
    }
}
