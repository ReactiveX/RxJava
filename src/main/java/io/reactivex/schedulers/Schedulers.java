/**
 * Copyright (c) 2016-present, RxJava Contributors.
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

import io.reactivex.Scheduler;
import io.reactivex.annotations.NonNull;
import io.reactivex.internal.schedulers.*;
import io.reactivex.plugins.RxJavaPlugins;

import java.util.concurrent.*;

/**
 * Static factory methods for returning standard Scheduler instances.
 * <p>
 * The initial and runtime values of the various scheduler types can be overridden via the
 * {@code RxJavaPlugins.setInit(scheduler name)SchedulerHandler()} and
 * {@code RxJavaPlugins.set(scheduler name)SchedulerHandler()} respectively.
 * <p>
 * <strong>Supported system properties ({@code System.getProperty()}):</strong>
 * <ul>
 * <li>{@code rx2.io-priority} (int): sets the thread priority of the {@link #io()} Scheduler, default is {@link Thread#NORM_PRIORITY}</li>
 * <li>{@code rx2.computation-threads} (int): sets the number of threads in the {@link #computation()} Scheduler, default is the number of available CPUs</li>
 * <li>{@code rx2.computation-priority} (int): sets the thread priority of the {@link #computation()} Scheduler, default is {@link Thread#NORM_PRIORITY}</li>
 * <li>{@code rx2.newthread-priority} (int): sets the thread priority of the {@link #newThread()} Scheduler, default is {@link Thread#NORM_PRIORITY}</li>
 * <li>{@code rx2.single-priority} (int): sets the thread priority of the {@link #single()} Scheduler, default is {@link Thread#NORM_PRIORITY}</li>
 * <li>{@code rx2.purge-enabled} (boolean): enables periodic purging of all Scheduler's backing thread pools, default is false</li>
 * <li>{@code rx2.purge-period-seconds} (int): specifies the periodic purge interval of all Scheduler's backing thread pools, default is 1 second</li>
 * </ul>
 */
public final class Schedulers {
    @NonNull
    static final Scheduler SINGLE;

    @NonNull
    static final Scheduler COMPUTATION;

    @NonNull
    static final Scheduler IO;

    @NonNull
    static final Scheduler TRAMPOLINE;

    @NonNull
    static final Scheduler NEW_THREAD;

    static final class SingleHolder {
        static final Scheduler DEFAULT = new SingleScheduler();
    }

    static final class ComputationHolder {
        static final Scheduler DEFAULT = new ComputationScheduler();
    }

    static final class IoHolder {
        static final Scheduler DEFAULT = new IoScheduler();
    }

    static final class NewThreadHolder {
        static final Scheduler DEFAULT = new NewThreadScheduler();
    }

    static {
        SINGLE = RxJavaPlugins.initSingleScheduler(new SingleTask());

        COMPUTATION = RxJavaPlugins.initComputationScheduler(new ComputationTask());

        IO = RxJavaPlugins.initIoScheduler(new IOTask());

        TRAMPOLINE = TrampolineScheduler.instance();

        NEW_THREAD = RxJavaPlugins.initNewThreadScheduler(new NewThreadTask());
    }

    /** Utility class. */
    private Schedulers() {
        throw new IllegalStateException("No instances!");
    }

    /**
     * Returns a default, shared {@link Scheduler} instance intended for computational work.
     * <p>
     * This can be used for event-loops, processing callbacks and other computational work.
     * <p>
     * It is not recommended to perform blocking, IO-bound work on this scheduler. Use {@link #io()} instead.
     * <p>
     * The default instance has a backing pool of single-threaded {@link ScheduledExecutorService} instances equal to
     * the number of available processors ({@link java.lang.Runtime#availableProcessors()}) to the Java VM.
     * <p>
     * Unhandled errors will be delivered to the scheduler Thread's {@link java.lang.Thread.UncaughtExceptionHandler}.
     * <p>
     * This type of scheduler is less sensitive to leaking {@link io.reactivex.Scheduler.Worker} instances, although
     * not disposing a worker that has timed/delayed tasks not cancelled by other means may leak resources and/or
     * execute those tasks "unexpectedly".
     * <p>
     * If the {@link RxJavaPlugins#setFailOnNonBlockingScheduler(boolean)} is set to true, attempting to execute
     * operators that block while running on this scheduler will throw an {@link IllegalStateException}.
     * <p>
     * You can control certain properties of this standard scheduler via system properties that have to be set
     * before the {@link Schedulers} class is referenced in your code.
     * <p><strong>Supported system properties ({@code System.getProperty()}):</strong>
     * <ul>
     * <li>{@code rx2.computation-threads} (int): sets the number of threads in the {@link #computation()} Scheduler, default is the number of available CPUs</li>
     * <li>{@code rx2.computation-priority} (int): sets the thread priority of the {@link #computation()} Scheduler, default is {@link Thread#NORM_PRIORITY}</li>
     * </ul>
     * <p>
     * The default value of this scheduler can be overridden at initialization time via the
     * {@link RxJavaPlugins#setInitComputationSchedulerHandler(io.reactivex.functions.Function)} plugin method.
     * Note that due to possible initialization cycles, using any of the other scheduler-returning methods will
     * result in a {@code NullPointerException}.
     * Once the {@link Schedulers} class has been initialized, you can override the returned {@link Scheduler} instance
     * via the {@link RxJavaPlugins#setComputationSchedulerHandler(io.reactivex.functions.Function)} method.
     * <p>
     * It is possible to create a fresh instance of this scheduler with a custom ThreadFactory, via the
     * {@link RxJavaPlugins#createComputationScheduler(ThreadFactory)} method. Note that such custom
     * instances require a manual call to {@link Scheduler#shutdown()} to allow the JVM to exit or the
     * (J2EE) container to unload properly.
     * <p>Operators on the base reactive classes that use this scheduler are marked with the
     * &#64;{@link io.reactivex.annotations.SchedulerSupport SchedulerSupport}({@link io.reactivex.annotations.SchedulerSupport#COMPUTATION COMPUTATION})
     * annotation.
     * @return a {@link Scheduler} meant for computation-bound work
     */
    @NonNull
    public static Scheduler computation() {
        return RxJavaPlugins.onComputationScheduler(COMPUTATION);
    }

    /**
     * Returns a default, shared {@link Scheduler} instance intended for IO-bound work.
     * <p>
     * This can be used for asynchronously performing blocking IO.
     * <p>
     * The implementation is backed by a pool of single-threaded {@link ScheduledExecutorService} instances
     * that will try to reuse previoulsy started instances used by the worker
     * returned by {@link io.reactivex.Scheduler#createWorker()} but otherwise will start a new backing
     * {@link ScheduledExecutorService} instance. Note that this scheduler may create an unbounded number
     * of worker threads that can result in system slowdowns or {@code OutOfMemoryError}. Therefore, for casual uses
     * or when implementing an operator, the Worker instances must be disposed via {@link io.reactivex.Scheduler.Worker#dispose()}.
     * <p>
     * It is not recommended to perform computational work on this scheduler. Use {@link #computation()} instead.
     * <p>
     * Unhandled errors will be delivered to the scheduler Thread's {@link java.lang.Thread.UncaughtExceptionHandler}.
     * <p>
     * You can control certain properties of this standard scheduler via system properties that have to be set
     * before the {@link Schedulers} class is referenced in your code.
     * <p><strong>Supported system properties ({@code System.getProperty()}):</strong>
     * <ul>
     * <li>{@code rx2.io-priority} (int): sets the thread priority of the {@link #io()} Scheduler, default is {@link Thread#NORM_PRIORITY}</li>
     * </ul>
     * <p>
     * The default value of this scheduler can be overridden at initialization time via the
     * {@link RxJavaPlugins#setInitIoSchedulerHandler(io.reactivex.functions.Function)} plugin method.
     * Note that due to possible initialization cycles, using any of the other scheduler-returning methods will
     * result in a {@code NullPointerException}.
     * Once the {@link Schedulers} class has been initialized, you can override the returned {@link Scheduler} instance
     * via the {@link RxJavaPlugins#setIoSchedulerHandler(io.reactivex.functions.Function)} method.
     * <p>
     * It is possible to create a fresh instance of this scheduler with a custom ThreadFactory, via the
     * {@link RxJavaPlugins#createIoScheduler(ThreadFactory)} method. Note that such custom
     * instances require a manual call to {@link Scheduler#shutdown()} to allow the JVM to exit or the
     * (J2EE) container to unload properly.
     * <p>Operators on the base reactive classes that use this scheduler are marked with the
     * &#64;{@link io.reactivex.annotations.SchedulerSupport SchedulerSupport}({@link io.reactivex.annotations.SchedulerSupport#IO IO})
     * annotation.
     * @return a {@link Scheduler} meant for IO-bound work
     */
    @NonNull
    public static Scheduler io() {
        return RxJavaPlugins.onIoScheduler(IO);
    }

    /**
     * Returns a default, shared {@link Scheduler} instance whose {@link io.reactivex.Scheduler.Worker}
     * instances queue work and execute them in a FIFO manner on one of the participating threads.
     * <p>
     * The default implementation's {@link Scheduler#scheduleDirect(Runnable)} methods execute the tasks on the current thread
     * without any queueing and the timed overloads use blocking sleep as well.
     * <p>
     * Note that this scheduler can't be reliably used to return the execution of
     * tasks to the "main" thread. Such behavior requires a blocking-queueing scheduler currently not provided
     * by RxJava itself but may be found in external libraries.
     * <p>
     * This scheduler can't be overridden via an {@link RxJavaPlugins} method.
     * @return a {@link Scheduler} that queues work on the current thread
     */
    @NonNull
    public static Scheduler trampoline() {
        return TRAMPOLINE;
    }

    /**
     * Returns a default, shared {@link Scheduler} instance that creates a new {@link Thread} for each unit of work.
     * <p>
     * The default implementation of this scheduler creates a new, single-threaded {@link ScheduledExecutorService} for
     * each invocation of the {@link Scheduler#scheduleDirect(Runnable)} (plus its overloads) and {@link Scheduler#createWorker()}
     * methods, thus an unbounded number of worker threads may be created that can
     * result in system slowdowns or {@code OutOfMemoryError}. Therefore, for casual uses or when implementing an operator,
     * the Worker instances must be disposed via {@link io.reactivex.Scheduler.Worker#dispose()}.
     * <p>
     * Unhandled errors will be delivered to the scheduler Thread's {@link java.lang.Thread.UncaughtExceptionHandler}.
     * <p>
     * You can control certain properties of this standard scheduler via system properties that have to be set
     * before the {@link Schedulers} class is referenced in your code.
     * <p><strong>Supported system properties ({@code System.getProperty()}):</strong>
     * <ul>
     * <li>{@code rx2.newthread-priority} (int): sets the thread priority of the {@link #newThread()} Scheduler, default is {@link Thread#NORM_PRIORITY}</li>
     * </ul>
     * <p>
     * The default value of this scheduler can be overridden at initialization time via the
     * {@link RxJavaPlugins#setInitNewThreadSchedulerHandler(io.reactivex.functions.Function)} plugin method.
     * Note that due to possible initialization cycles, using any of the other scheduler-returning methods will
     * result in a {@code NullPointerException}.
     * Once the {@link Schedulers} class has been initialized, you can override the returned {@link Scheduler} instance
     * via the {@link RxJavaPlugins#setNewThreadSchedulerHandler(io.reactivex.functions.Function)} method.
     * <p>
     * It is possible to create a fresh instance of this scheduler with a custom ThreadFactory, via the
     * {@link RxJavaPlugins#createNewThreadScheduler(ThreadFactory)} method. Note that such custom
     * instances require a manual call to {@link Scheduler#shutdown()} to allow the JVM to exit or the
     * (J2EE) container to unload properly.
     * <p>Operators on the base reactive classes that use this scheduler are marked with the
     * &#64;{@link io.reactivex.annotations.SchedulerSupport SchedulerSupport}({@link io.reactivex.annotations.SchedulerSupport#NEW_THREAD NEW_TRHEAD})
     * annotation.
     * @return a {@link Scheduler} that creates new threads
     */
    @NonNull
    public static Scheduler newThread() {
        return RxJavaPlugins.onNewThreadScheduler(NEW_THREAD);
    }

    /**
     * Returns a default, shared, single-thread-backed {@link Scheduler} instance for work
     * requiring strongly-sequential execution on the same background thread.
     * <p>
     * Uses:
     * <ul>
     * <li>main event loop</li>
     * <li>support Schedulers.from(Executor) and from(ExecutorService) with delayed scheduling</li>
     * <li>support benchmarks that pipeline data from the main thread to some other thread and
     * avoid core-bashing of computation's round-robin nature</li>
     * </ul>
     * <p>
     * Unhandled errors will be delivered to the scheduler Thread's {@link java.lang.Thread.UncaughtExceptionHandler}.
     * <p>
     * This type of scheduler is less sensitive to leaking {@link io.reactivex.Scheduler.Worker} instances, although
     * not disposing a worker that has timed/delayed tasks not cancelled by other means may leak resources and/or
     * execute those tasks "unexpectedly".
     * <p>
     * If the {@link RxJavaPlugins#setFailOnNonBlockingScheduler(boolean)} is set to true, attempting to execute
     * operators that block while running on this scheduler will throw an {@link IllegalStateException}.
     * <p>
     * You can control certain properties of this standard scheduler via system properties that have to be set
     * before the {@link Schedulers} class is referenced in your code.
     * <p><strong>Supported system properties ({@code System.getProperty()}):</strong>
     * <ul>
     * <li>{@code rx2.single-priority} (int): sets the thread priority of the {@link #single()} Scheduler, default is {@link Thread#NORM_PRIORITY}</li>
     * </ul>
     * <p>
     * The default value of this scheduler can be overridden at initialization time via the
     * {@link RxJavaPlugins#setInitSingleSchedulerHandler(io.reactivex.functions.Function)} plugin method.
     * Note that due to possible initialization cycles, using any of the other scheduler-returning methods will
     * result in a {@code NullPointerException}.
     * Once the {@link Schedulers} class has been initialized, you can override the returned {@link Scheduler} instance
     * via the {@link RxJavaPlugins#setSingleSchedulerHandler(io.reactivex.functions.Function)} method.
     * <p>
     * It is possible to create a fresh instance of this scheduler with a custom ThreadFactory, via the
     * {@link RxJavaPlugins#createSingleScheduler(ThreadFactory)} method. Note that such custom
     * instances require a manual call to {@link Scheduler#shutdown()} to allow the JVM to exit or the
     * (J2EE) container to unload properly.
     * <p>Operators on the base reactive classes that use this scheduler are marked with the
     * &#64;{@link io.reactivex.annotations.SchedulerSupport SchedulerSupport}({@link io.reactivex.annotations.SchedulerSupport#SINGLE SINGLE})
     * annotation.
     * @return a {@link Scheduler} that shares a single backing thread.
     * @since 2.0
     */
    @NonNull
    public static Scheduler single() {
        return RxJavaPlugins.onSingleScheduler(SINGLE);
    }

    /**
     * Wraps an {@link Executor} into a new Scheduler instance and delegates {@code schedule()}
     * calls to it.
     * <p>
     * If the provided executor doesn't support any of the more specific standard Java executor
     * APIs, cancelling tasks scheduled by this scheduler can't be interrupted when they are
     * executing but only prevented from running prior to that. In addition, tasks scheduled with
     * a time delay or periodically will use the {@link #single()} scheduler for the timed waiting
     * before posting the actual task to the given executor.
     * <p>
     * If the provided executor supports the standard Java {@link ExecutorService} API,
     * cancelling tasks scheduled by this scheduler can be cancelled/interrupted by calling
     * {@link io.reactivex.disposables.Disposable#dispose()}. In addition, tasks scheduled with
     * a time delay or periodically will use the {@link #single()} scheduler for the timed waiting
     * before posting the actual task to the given executor.
     * <p>
     * If the provided executor supports the standard Java {@link ScheduledExecutorService} API,
     * cancelling tasks scheduled by this scheduler can be cancelled/interrupted by calling
     * {@link io.reactivex.disposables.Disposable#dispose()}. In addition, tasks scheduled with
     * a time delay or periodically will use the provided executor. Note, however, if the provided
     * {@code ScheduledExecutorService} instance is not single threaded, tasks scheduled
     * with a time delay close to each other may end up executing in different order than
     * the original schedule() call was issued. This limitation may be lifted in a future patch.
     * <p>
     * Starting, stopping and restarting this scheduler is not supported (no-op) and the provided
     * executor's lifecycle must be managed externally:
     * <pre><code>
     * ExecutorService exec = Executors.newSingleThreadedExecutor();
     * try {
     *     Scheduler scheduler = Schedulers.from(exec);
     *     Flowable.just(1)
     *        .subscribeOn(scheduler)
     *        .map(v -&gt; v + 1)
     *        .observeOn(scheduler)
     *        .blockingSubscribe(System.out::println);
     * } finally {
     *     exec.shutdown();
     * }
     * </code></pre>
     * <p>
     * This type of scheduler is less sensitive to leaking {@link io.reactivex.Scheduler.Worker} instances, although
     * not disposing a worker that has timed/delayed tasks not cancelled by other means may leak resources and/or
     * execute those tasks "unexpectedly".
     * <p>
     * Note that this method returns a new {@link Scheduler} instance, even for the same {@link Executor} instance.
     * @param executor
     *          the executor to wrap
     * @return the new Scheduler wrapping the Executor
     */
    @NonNull
    public static Scheduler from(@NonNull Executor executor) {
        return new ExecutorScheduler(executor);
    }

    /**
     * Shuts down the standard Schedulers.
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
     * Starts the standard Schedulers.
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

    static final class IOTask implements Callable<Scheduler> {
        @Override
        public Scheduler call() throws Exception {
            return IoHolder.DEFAULT;
        }
    }

    static final class NewThreadTask implements Callable<Scheduler> {
        @Override
        public Scheduler call() throws Exception {
            return NewThreadHolder.DEFAULT;
        }
    }

    static final class SingleTask implements Callable<Scheduler> {
        @Override
        public Scheduler call() throws Exception {
            return SingleHolder.DEFAULT;
        }
    }

    static final class ComputationTask implements Callable<Scheduler> {
        @Override
        public Scheduler call() throws Exception {
            return ComputationHolder.DEFAULT;
        }
    }
}
