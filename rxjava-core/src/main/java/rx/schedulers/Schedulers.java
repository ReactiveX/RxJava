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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import rx.Scheduler;
import rx.plugins.RxJavaPlugins;

/**
 * Static factory methods for creating Schedulers.
 */
public class Schedulers {

    private final Scheduler computationScheduler;
    private final Scheduler ioScheduler;
    private final Scheduler newThreadScheduler;

    private static final Schedulers INSTANCE = new Schedulers();

    private Schedulers() {
        Scheduler c = RxJavaPlugins.getInstance().getDefaultSchedulers().getComputationScheduler();
        if (c != null) {
            computationScheduler = c;
        } else {
            computationScheduler = executor(createComputationExecutor());
        }

        Scheduler io = RxJavaPlugins.getInstance().getDefaultSchedulers().getIOScheduler();
        if (io != null) {
            ioScheduler = io;
        } else {
            ioScheduler = executor(createIOExecutor());
        }

        Scheduler nt = RxJavaPlugins.getInstance().getDefaultSchedulers().getNewThreadScheduler();
        if (nt != null) {
            newThreadScheduler = nt;
        } else {
            newThreadScheduler = NewThreadScheduler.instance();
        }

    }

    /**
     * {@link Scheduler} that executes work immediately on the current thread.
     * 
     * @return {@link ImmediateScheduler} instance
     */
    public static Scheduler immediate() {
        return ImmediateScheduler.instance();
    }

    /**
     * {@link Scheduler} that queues work on the current thread to be executed after the current work completes.
     * 
     * @return {@link TrampolineScheduler} instance
     * @deprecated use {@link #trampoline()} instead
     */
    @Deprecated
    public static Scheduler currentThread() {
        return TrampolineScheduler.instance();
    }

    /**
     * {@link Scheduler} that queues work on the current thread to be executed after the current work completes.
     * 
     * @return {@link TrampolineScheduler} instance
     */
    public static Scheduler trampoline() {
        return TrampolineScheduler.instance();
    }

    /**
     * {@link Scheduler} that creates a new {@link Thread} for each unit of work.
     * 
     * @return {@link NewThreadScheduler} instance
     */
    public static Scheduler newThread() {
        return INSTANCE.newThreadScheduler;
    }

    /**
     * {@link Scheduler} that queues work on an {@link Executor}.
     * <p>
     * Note that this does not support scheduled actions with a delay.
     * 
     * @return {@link ExecutorScheduler} instance
     */
    public static Scheduler executor(Executor executor) {
        return new ExecutorScheduler(executor);
    }

    /**
     * {@link Scheduler} that queues work on an {@link ScheduledExecutorService}.
     * 
     * @return {@link ExecutorScheduler} instance
     */
    public static Scheduler executor(ScheduledExecutorService executor) {
        return new ExecutorScheduler(executor);
    }

    /**
     * {@link Scheduler} intended for computational work.
     * <p>
     * The implementation is backed by a {@link ScheduledExecutorService} thread-pool sized to the number of CPU
     * cores.
     * <p>
     * This can be used for event-loops, processing callbacks and other computational work.
     * <p>
     * Do not perform IO-bound work on this scheduler. Use {@link #io()} instead.
     * 
     * @return {@link ExecutorScheduler} for computation-bound work
     * @deprecated use {@link #computation()}
     */
    @Deprecated
    public static Scheduler threadPoolForComputation() {
        return computation();
    }

    /**
     * {@link Scheduler} intended for computational work.
     * <p>
     * This can be used for event-loops, processing callbacks and other computational work.
     * <p>
     * Do not perform IO-bound work on this scheduler. Use {@link #io()} instead.
     * 
     * @return {@link Scheduler} for computation-bound work
     */
    public static Scheduler computation() {
        return INSTANCE.computationScheduler;
    }

    /**
     * {@link Scheduler} intended for IO-bound work.
     * <p>
     * The implementation is backed by an {@link Executor} thread-pool that will grow as needed.
     * <p>
     * This can be used for asynchronously performing blocking IO.
     * <p>
     * Do not perform computational work on this scheduler. Use {@link #computation()} instead.
     * 
     * @return {@link ExecutorScheduler} for IO-bound work
     * @deprecated use {@link #io()} instead
     */
    @Deprecated
    public static Scheduler threadPoolForIO() {
        return io();
    }

    /**
     * {@link Scheduler} intended for IO-bound work.
     * <p>
     * The implementation is backed by an {@link Executor} thread-pool that will grow as needed.
     * <p>
     * This can be used for asynchronously performing blocking IO.
     * <p>
     * Do not perform computational work on this scheduler. Use {@link #computation()} instead.
     * 
     * @return {@link ExecutorScheduler} for IO-bound work
     */
    public static Scheduler io() {
        return INSTANCE.ioScheduler;
    }

    private static ScheduledExecutorService createComputationExecutor() {
        int cores = Runtime.getRuntime().availableProcessors();
        return Executors.newScheduledThreadPool(cores, new ThreadFactory() {
            final AtomicInteger counter = new AtomicInteger();

            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "RxComputationThreadPool-" + counter.incrementAndGet());
                t.setDaemon(true);
                return t;
            }
        });
    }

    private static Executor createIOExecutor() {
        Executor result = Executors.newCachedThreadPool(new ThreadFactory() {
            final AtomicLong counter = new AtomicLong();

            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "RxIOThreadPool-" + counter.incrementAndGet());
                t.setDaemon(true);
                return t;
            }
        });

        return result;
    }

    public static TestScheduler test() {
        return new TestScheduler();
    }
}
