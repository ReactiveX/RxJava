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

import java.util.concurrent.Executor;

import rx.Scheduler;
import rx.plugins.RxJavaPlugins;

/**
 * Static factory methods for creating Schedulers.
 */
public final class Schedulers {

    private static final Object computationGuard = new Object();
    /** The computation scheduler instance, guarded by computationGuard. */
    private static volatile Scheduler computationScheduler;
    private static final Object ioGuard = new Object();
    /** The io scheduler instance, guarded by ioGuard. */
    private static volatile Scheduler ioScheduler;
    /** The new thread scheduler, fixed because it doesn't need to support shutdown. */
    private static final Scheduler newThreadScheduler;
    
    static {
        Scheduler nt = RxJavaPlugins.getInstance().getSchedulersHook().getNewThreadScheduler();
        if (nt != null) {
            newThreadScheduler = nt;
        } else {
            newThreadScheduler = NewThreadScheduler.instance();
        }
    }
    
    private Schedulers() {
        throw new IllegalStateException("No instances!");
    }

    /**
     * Creates and returns a {@link Scheduler} that executes work immediately on the current thread.
     * 
     * @return an {@link ImmediateScheduler} instance
     */
    public static Scheduler immediate() {
        return ImmediateScheduler.instance();
    }

    /**
     * Creates and returns a {@link Scheduler} that queues work on the current thread to be executed after the
     * current work completes.
     * 
     * @return a {@link TrampolineScheduler} instance
     */
    public static Scheduler trampoline() {
        return TrampolineScheduler.instance();
    }

    /**
     * Creates and returns a {@link Scheduler} that creates a new {@link Thread} for each unit of work.
     * <p>
     * Unhandled errors will be delivered to the scheduler Thread's {@link java.lang.Thread.UncaughtExceptionHandler}.
     *
     * @return a {@link NewThreadScheduler} instance
     */
    public static Scheduler newThread() {
        return newThreadScheduler;
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
        Scheduler s = computationScheduler;
        if (s != null) {
            return s;
        }
        synchronized (computationGuard) {
            if (computationScheduler == null) {
                Scheduler c = RxJavaPlugins.getInstance().getSchedulersHook().getComputationScheduler();
                if (c != null) {
                    computationScheduler = c;
                } else {
                    computationScheduler = new EventLoopsScheduler();
                }
            }
            return computationScheduler;
        }
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
        Scheduler s = ioScheduler;
        if (s != null) {
            return s;
        }
        synchronized (ioGuard) {
            if (ioScheduler == null) {
                Scheduler io = RxJavaPlugins.getInstance().getSchedulersHook().getIOScheduler();
                if (io != null) {
                    ioScheduler = io;
                } else {
                    ioScheduler = new CachedThreadScheduler();
                }
            }
            return ioScheduler;
        }
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
     * Shuts down the threads of the Computation and IO schedulers.
     * The newThread() scheduler doesn't need to be shut down as its workers shut themselves
     * down once they complete.
     */
    public static void shutdown() {
        synchronized (computationGuard) {
            if (computationScheduler != null) {
                computationScheduler.shutdown();
            }
            computationScheduler = null;
        }
        synchronized (ioGuard) {
            if (ioScheduler != null) {
                ioScheduler.shutdown();
            }
            ioScheduler = null;
        }
    }
    /**
     * Test support.
     * @return returns true if there is a computation scheduler instance available.
     */
    static boolean hasComputationScheduler() {
        return computationScheduler != null;
    }
    /**
     * Test support.
     * @return returns true if there is an io scheduler instance available.
     */
    static boolean hasIOScheduler() {
        return ioScheduler != null;
    }
}
