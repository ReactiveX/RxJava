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

package io.reactivex.rxjava3.core;

import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.annotations.*;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.exceptions.Exceptions;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.internal.disposables.*;
import io.reactivex.rxjava3.internal.schedulers.*;
import io.reactivex.rxjava3.internal.util.ExceptionHelper;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.schedulers.SchedulerRunnableIntrospection;

/**
 * A {@code Scheduler} is an object that specifies an API for scheduling
 * units of work provided in the form of {@link Runnable}s to be
 * executed without delay (effectively as soon as possible), after a specified time delay or periodically
 * and represents an abstraction over an asynchronous boundary that ensures
 * these units of work get executed by some underlying task-execution scheme
 * (such as custom Threads, event loop, {@link java.util.concurrent.Executor Executor} or Actor system)
 * with some uniform properties and guarantees regardless of the particular underlying
 * scheme.
 * <p>
 * You can get various standard, RxJava-specific instances of this class via
 * the static methods of the {@link io.reactivex.rxjava3.schedulers.Schedulers} utility class.
 * <p>
 * The so-called {@link Worker}s of a {@code Scheduler} can be created via the {@link #createWorker()} method which allow the scheduling
 * of multiple {@link Runnable} tasks in an isolated manner. {@code Runnable} tasks scheduled on a {@code Worker} are guaranteed to be
 * executed sequentially and in a non-overlapping fashion. Non-delayed {@code Runnable} tasks are guaranteed to execute in a
 * First-In-First-Out order but their execution may be interleaved with delayed tasks.
 * In addition, outstanding or running tasks can be cancelled together via
 * {@link Worker#dispose()} without affecting any other {@code Worker} instances of the same {@code Scheduler}.
 * <p>
 * Implementations of the {@link #scheduleDirect} and {@link Worker#schedule} methods are encouraged to call the {@link io.reactivex.rxjava3.plugins.RxJavaPlugins#onSchedule(Runnable)}
 * method to allow a scheduler hook to manipulate (wrap or replace) the original {@code Runnable} task before it is submitted to the
 * underlying task-execution scheme.
 * <p>
 * The default implementations of the {@code scheduleDirect} methods provided by this abstract class
 * delegate to the respective {@code schedule} methods in the {@link Worker} instance created via {@link #createWorker()}
 * for each individual {@link Runnable} task submitted. Implementors of this class are encouraged to provide
 * a more efficient direct scheduling implementation to avoid the time and memory overhead of creating such {@code Worker}s
 * for every task.
 * This delegation is done via special wrapper instances around the original {@code Runnable} before calling the respective
 * {@code Worker.schedule} method. Note that this can lead to multiple {@code RxJavaPlugins.onSchedule} calls and potentially
 * multiple hooks applied. Therefore, the default implementations of {@code scheduleDirect} (and the {@link Worker#schedulePeriodically(Runnable, long, long, TimeUnit)})
 * wrap the incoming {@code Runnable} into a class that implements the {@link io.reactivex.rxjava3.schedulers.SchedulerRunnableIntrospection}
 * interface which can grant access to the original or hooked {@code Runnable}, thus, a repeated {@code RxJavaPlugins.onSchedule}
 * can detect the earlier hook and not apply a new one over again.
 * <p>
 * The default implementation of {@link #now(TimeUnit)} and {@link Worker#now(TimeUnit)} methods to return current
 * {@link System#currentTimeMillis()} value in the desired time unit. Custom {@code Scheduler} implementations can override this
 * to provide specialized time accounting (such as virtual time to be advanced programmatically).
 * Note that operators requiring a {@code Scheduler} may rely on either of the {@code now()} calls provided by
 * {@code Scheduler} or {@code Worker} respectively, therefore, it is recommended they represent a logically
 * consistent source of the current time.
 * <p>
 * The default implementation of the {@link Worker#schedulePeriodically(Runnable, long, long, TimeUnit)} method uses
 * the {@link Worker#schedule(Runnable, long, TimeUnit)} for scheduling the {@code Runnable} task periodically.
 * The algorithm calculates the next absolute time when the task should run again and schedules this execution
 * based on the relative time between it and {@link Worker#now(TimeUnit)}. However, drifts or changes in the
 * system clock could affect this calculation either by scheduling subsequent runs too frequently or too far apart.
 * Therefore, the default implementation uses the {@link #clockDriftTolerance()} value (set via
 * {@code rx3.scheduler.drift-tolerance} in minutes) to detect a drift in {@link Worker#now(TimeUnit)} and
 * re-adjust the absolute/relative time calculation accordingly.
 * <p>
 * The default implementations of {@link #start()} and {@link #shutdown()} do nothing and should be overridden if the
 * underlying task-execution scheme supports stopping and restarting itself.
 * <p>
 * If the {@code Scheduler} is shut down or a {@code Worker} is disposed, the {@code schedule} methods
 * should return the {@link Disposable#disposed()} singleton instance indicating the shut down/disposed
 * state to the caller. Since the shutdown or dispose can happen from any thread, the {@code schedule} implementations
 * should make best effort to cancel tasks immediately after those tasks have been submitted to the
 * underlying task-execution scheme if the shutdown/dispose was detected after this submission.
 * <p>
 * All methods on the {@code Scheduler} and {@code Worker} classes should be thread safe.
 */
public abstract class Scheduler {
    /**
     * The tolerance for a clock drift in nanoseconds where the periodic scheduler will rebase.
     * <p>
     * The associated system parameter, {@code rx3.scheduler.drift-tolerance}, expects its value in minutes.
     */
    static final long CLOCK_DRIFT_TOLERANCE_NANOSECONDS;
    static {
        CLOCK_DRIFT_TOLERANCE_NANOSECONDS = TimeUnit.MINUTES.toNanos(
                Long.getLong("rx3.scheduler.drift-tolerance", 15));
    }

    /**
     * Returns the clock drift tolerance in nanoseconds.
     * <p>Related system property: {@code rx3.scheduler.drift-tolerance} in minutes.
     * @return the tolerance in nanoseconds
     * @since 2.0
     */
    public static long clockDriftTolerance() {
        return CLOCK_DRIFT_TOLERANCE_NANOSECONDS;
    }

    /**
     * Retrieves or creates a new {@link Scheduler.Worker} that represents sequential execution of actions.
     * <p>
     * When work is completed, the {@code Worker} instance should be released
     * by calling {@link Scheduler.Worker#dispose()} to avoid potential resource leaks in the
     * underlying task-execution scheme.
     * <p>
     * Work on a {@link Scheduler.Worker} is guaranteed to be sequential and non-overlapping.
     *
     * @return a Worker representing a serial queue of actions to be executed
     */
    @NonNull
    public abstract Worker createWorker();

    /**
     * Returns the 'current time' of the Scheduler in the specified time unit.
     * @param unit the time unit
     * @return the 'current time'
     * @since 2.0
     */
    public long now(@NonNull TimeUnit unit) {
        return unit.convert(System.currentTimeMillis(), TimeUnit.MILLISECONDS);
    }

    /**
     * Allows the Scheduler instance to start threads
     * and accept tasks on them.
     * <p>
     * Implementations should make sure the call is idempotent, thread-safe and
     * should not throw any {@code RuntimeException} if it doesn't support this
     * functionality.
     *
     * @since 2.0
     */
    public void start() {

    }

    /**
     * Instructs the Scheduler instance to stop threads,
     * stop accepting tasks on any outstanding {@link Worker} instances
     * and clean up any associated resources with this Scheduler.
     * <p>
     * Implementations should make sure the call is idempotent, thread-safe and
     * should not throw any {@code RuntimeException} if it doesn't support this
     * functionality.
     * @since 2.0
     */
    public void shutdown() {

    }

    /**
     * Schedules the given task on this Scheduler without any time delay.
     *
     * <p>
     * This method is safe to be called from multiple threads but there are no
     * ordering or non-overlapping guarantees between tasks.
     *
     * @param run the task to execute
     *
     * @return the Disposable instance that let's one cancel this particular task.
     * @since 2.0
     */
    @NonNull
    public Disposable scheduleDirect(@NonNull Runnable run) {
        return scheduleDirect(run, 0L, TimeUnit.NANOSECONDS);
    }

    /**
     * Schedules the execution of the given task with the given time delay.
     *
     * <p>
     * This method is safe to be called from multiple threads but there are no
     * ordering guarantees between tasks.
     *
     * @param run the task to schedule
     * @param delay the delay amount, non-positive values indicate non-delayed scheduling
     * @param unit the unit of measure of the delay amount
     * @return the Disposable that let's one cancel this particular delayed task.
     * @since 2.0
     */
    @NonNull
    public Disposable scheduleDirect(@NonNull Runnable run, long delay, @NonNull TimeUnit unit) {
        final Worker w = createWorker();

        final Runnable decoratedRun = RxJavaPlugins.onSchedule(run);

        DisposeTask task = new DisposeTask(decoratedRun, w);

        w.schedule(task, delay, unit);

        return task;
    }

    /**
     * Schedules a periodic execution of the given task with the given initial time delay and repeat period.
     *
     * <p>
     * This method is safe to be called from multiple threads but there are no
     * ordering guarantees between tasks.
     *
     * <p>
     * The periodic execution is at a fixed rate, that is, the first execution will be after the
     * {@code initialDelay}, the second after {@code initialDelay + period}, the third after
     * {@code initialDelay + 2 * period}, and so on.
     *
     * @param run the task to schedule
     * @param initialDelay the initial delay amount, non-positive values indicate non-delayed scheduling
     * @param period the period at which the task should be re-executed
     * @param unit the unit of measure of the delay amount
     * @return the Disposable that let's one cancel this particular delayed task.
     * @since 2.0
     */
    @NonNull
    public Disposable schedulePeriodicallyDirect(@NonNull Runnable run, long initialDelay, long period, @NonNull TimeUnit unit) {
        final Worker w = createWorker();

        final Runnable decoratedRun = RxJavaPlugins.onSchedule(run);

        PeriodicDirectTask periodicTask = new PeriodicDirectTask(decoratedRun, w);

        Disposable d = w.schedulePeriodically(periodicTask, initialDelay, period, unit);
        if (d == EmptyDisposable.INSTANCE) {
            return d;
        }

        return periodicTask;
    }

    /**
     * Allows the use of operators for controlling the timing around when
     * actions scheduled on workers are actually done. This makes it possible to
     * layer additional behavior on this {@link Scheduler}. The only parameter
     * is a function that flattens an {@link Flowable} of {@link Flowable}
     * of {@link Completable}s into just one {@link Completable}. There must be
     * a chain of operators connecting the returned value to the source
     * {@link Flowable} otherwise any work scheduled on the returned
     * {@link Scheduler} will not be executed.
     * <p>
     * When {@link Scheduler#createWorker()} is invoked a {@link Flowable} of
     * {@link Completable}s is onNext'd to the combinator to be flattened. If
     * the inner {@link Flowable} is not immediately subscribed to an calls to
     * {@link Worker#schedule} are buffered. Once the {@link Flowable} is
     * subscribed to actions are then onNext'd as {@link Completable}s.
     * <p>
     * Finally the actions scheduled on the parent {@link Scheduler} when the
     * inner most {@link Completable}s are subscribed to.
     * <p>
     * When the {@link Worker} is unsubscribed the {@link Completable} emits an
     * onComplete and triggers any behavior in the flattening operator. The
     * {@link Flowable} and all {@link Completable}s give to the flattening
     * function never onError.
     * <p>
     * Limit the amount concurrency two at a time without creating a new fix
     * size thread pool:
     *
     * <pre>
     * Scheduler limitScheduler = Schedulers.computation().when(workers -&gt; {
     *  // use merge max concurrent to limit the number of concurrent
     *  // callbacks two at a time
     *  return Completable.merge(Flowable.merge(workers), 2);
     * });
     * </pre>
     * <p>
     * This is a slightly different way to limit the concurrency but it has some
     * interesting benefits and drawbacks to the method above. It works by
     * limited the number of concurrent {@link Worker}s rather than individual
     * actions. Generally each {@link Flowable} uses its own {@link Worker}.
     * This means that this will essentially limit the number of concurrent
     * subscribes. The danger comes from using operators like
     * {@link Flowable#zip(org.reactivestreams.Publisher, org.reactivestreams.Publisher, io.reactivex.rxjava3.functions.BiFunction)} where
     * subscribing to the first {@link Flowable} could deadlock the
     * subscription to the second.
     *
     * <pre>
     * Scheduler limitScheduler = Schedulers.computation().when(workers -&gt; {
     *  // use merge max concurrent to limit the number of concurrent
     *  // Flowables two at a time
     *  return Completable.merge(Flowable.merge(workers, 2));
     * });
     * </pre>
     *
     * Slowing down the rate to no more than than 1 a second. This suffers from
     * the same problem as the one above I could find an {@link Flowable}
     * operator that limits the rate without dropping the values (aka leaky
     * bucket algorithm).
     *
     * <pre>
     * Scheduler slowScheduler = Schedulers.computation().when(workers -&gt; {
     *  // use concatenate to make each worker happen one at a time.
     *  return Completable.concat(workers.map(actions -&gt; {
     *      // delay the starting of the next worker by 1 second.
     *      return Completable.merge(actions.delaySubscription(1, TimeUnit.SECONDS));
     *  }));
     * });
     * </pre>
     *
     * <p>History: 2.0.1 - experimental
     * @param <S> a Scheduler and a Subscription
     * @param combine the function that takes a two-level nested Flowable sequence of a Completable and returns
     * the Completable that will be subscribed to and should trigger the execution of the scheduled Actions.
     * @return the Scheduler with the customized execution behavior
     * @since 2.1
     */
    @SuppressWarnings("unchecked")
    @NonNull
    public <S extends Scheduler & Disposable> S when(@NonNull Function<Flowable<Flowable<Completable>>, Completable> combine) {
        return (S) new SchedulerWhen(combine, this);
    }

    /**
     * Represents an isolated, sequential worker of a parent Scheduler for executing {@code Runnable} tasks on
     * an underlying task-execution scheme (such as custom Threads, event loop, {@link java.util.concurrent.Executor Executor} or Actor system).
     * <p>
     * Disposing the {@link Worker} should cancel all outstanding work and allows resource cleanup.
     * <p>
     * The default implementations of {@link #schedule(Runnable)} and {@link #schedulePeriodically(Runnable, long, long, TimeUnit)}
     * delegate to the abstract {@link #schedule(Runnable, long, TimeUnit)} method. Its implementation is encouraged to
     * track the individual {@code Runnable} tasks while they are waiting to be executed (with or without delay) so that
     * {@link #dispose()} can prevent their execution or potentially interrupt them if they are currently running.
     * <p>
     * The default implementation of the {@link #now(TimeUnit)} method returns current
     * {@link System#currentTimeMillis()} value in the desired time unit. Custom {@code Worker} implementations can override this
     * to provide specialized time accounting (such as virtual time to be advanced programmatically).
     * Note that operators requiring a scheduler may rely on either of the {@code now()} calls provided by
     * {@code Scheduler} or {@code Worker} respectively, therefore, it is recommended they represent a logically
     * consistent source of the current time.
     * <p>
     * The default implementation of the {@link #schedulePeriodically(Runnable, long, long, TimeUnit)} method uses
     * the {@link #schedule(Runnable, long, TimeUnit)} for scheduling the {@code Runnable} task periodically.
     * The algorithm calculates the next absolute time when the task should run again and schedules this execution
     * based on the relative time between it and {@link #now(TimeUnit)}. However, drifts or changes in the
     * system clock would affect this calculation either by scheduling subsequent runs too frequently or too far apart.
     * Therefore, the default implementation uses the {@link #clockDriftTolerance()} value (set via
     * {@code rx3.scheduler.drift-tolerance} in minutes) to detect a drift in {@link #now(TimeUnit)} and
     * re-adjust the absolute/relative time calculation accordingly.
     * <p>
     * If the {@code Worker} is disposed, the {@code schedule} methods
     * should return the {@link Disposable#disposed()} singleton instance indicating the disposed
     * state to the caller. Since the {@link #dispose()} call can happen on any thread, the {@code schedule} implementations
     * should make best effort to cancel tasks immediately after those tasks have been submitted to the
     * underlying task-execution scheme if the dispose was detected after this submission.
     * <p>
     * All methods on the {@code Worker} class should be thread safe.
     */
    public abstract static class Worker implements Disposable {
        /**
         * Schedules a Runnable for execution without any time delay.
         *
         * <p>The default implementation delegates to {@link #schedule(Runnable, long, TimeUnit)}.
         *
         * @param run
         *            Runnable to schedule
         * @return a Disposable to be able to unsubscribe the action (cancel it if not executed)
         */
        @NonNull
        public Disposable schedule(@NonNull Runnable run) {
            return schedule(run, 0L, TimeUnit.NANOSECONDS);
        }

        /**
         * Schedules an Runnable for execution at some point in the future specified by a time delay
         * relative to the current time.
         * <p>
         * Note to implementors: non-positive {@code delayTime} should be regarded as non-delayed schedule, i.e.,
         * as if the {@link #schedule(Runnable)} was called.
         *
         * @param run
         *            the Runnable to schedule
         * @param delay
         *            time to "wait" before executing the action; non-positive values indicate an non-delayed
         *            schedule
         * @param unit
         *            the time unit of {@code delayTime}
         * @return a Disposable to be able to unsubscribe the action (cancel it if not executed)
         */
        @NonNull
        public abstract Disposable schedule(@NonNull Runnable run, long delay, @NonNull TimeUnit unit);

        /**
         * Schedules a periodic execution of the given task with the given initial time delay and repeat period.
         * <p>
         * The default implementation schedules and reschedules the {@code Runnable} task via the
         * {@link #schedule(Runnable, long, TimeUnit)}
         * method over and over and at a fixed rate, that is, the first execution will be after the
         * {@code initialDelay}, the second after {@code initialDelay + period}, the third after
         * {@code initialDelay + 2 * period}, and so on.
         * <p>
         * Note to implementors: non-positive {@code initialTime} and {@code period} should be regarded as
         * non-delayed scheduling of the first and any subsequent executions.
         * In addition, a more specific {@code Worker} implementation should override this method
         * if it can perform the periodic task execution with less overhead (such as by avoiding the
         * creation of the wrapper and tracker objects upon each periodic invocation of the
         * common {@link #schedule(Runnable, long, TimeUnit)} method).
         *
         * @param run
         *            the Runnable to execute periodically
         * @param initialDelay
         *            time to wait before executing the action for the first time; non-positive values indicate
         *            an non-delayed schedule
         * @param period
         *            the time interval to wait each time in between executing the action; non-positive values
         *            indicate no delay between repeated schedules
         * @param unit
         *            the time unit of {@code period}
         * @return a Disposable to be able to unsubscribe the action (cancel it if not executed)
         */
        @NonNull
        public Disposable schedulePeriodically(@NonNull Runnable run, final long initialDelay, final long period, @NonNull final TimeUnit unit) {
            final SequentialDisposable first = new SequentialDisposable();

            final SequentialDisposable sd = new SequentialDisposable(first);

            final Runnable decoratedRun = RxJavaPlugins.onSchedule(run);

            final long periodInNanoseconds = unit.toNanos(period);
            final long firstNowNanoseconds = now(TimeUnit.NANOSECONDS);
            final long firstStartInNanoseconds = firstNowNanoseconds + unit.toNanos(initialDelay);

            Disposable d = schedule(new PeriodicTask(firstStartInNanoseconds, decoratedRun, firstNowNanoseconds, sd,
                    periodInNanoseconds), initialDelay, unit);

            if (d == EmptyDisposable.INSTANCE) {
                return d;
            }
            first.replace(d);

            return sd;
        }

        /**
         * Returns the 'current time' of the Worker in the specified time unit.
         * @param unit the time unit
         * @return the 'current time'
         * @since 2.0
         */
        public long now(@NonNull TimeUnit unit) {
            return unit.convert(System.currentTimeMillis(), TimeUnit.MILLISECONDS);
        }

        /**
         * Holds state and logic to calculate when the next delayed invocation
         * of this task has to happen (accounting for clock drifts).
         */
        final class PeriodicTask implements Runnable, SchedulerRunnableIntrospection {
            @NonNull
            final Runnable decoratedRun;
            @NonNull
            final SequentialDisposable sd;
            final long periodInNanoseconds;
            long count;
            long lastNowNanoseconds;
            long startInNanoseconds;

            PeriodicTask(long firstStartInNanoseconds, @NonNull Runnable decoratedRun,
                    long firstNowNanoseconds, @NonNull SequentialDisposable sd, long periodInNanoseconds) {
                this.decoratedRun = decoratedRun;
                this.sd = sd;
                this.periodInNanoseconds = periodInNanoseconds;
                lastNowNanoseconds = firstNowNanoseconds;
                startInNanoseconds = firstStartInNanoseconds;
            }

            @Override
            public void run() {
                decoratedRun.run();

                if (!sd.isDisposed()) {

                    long nextTick;

                    long nowNanoseconds = now(TimeUnit.NANOSECONDS);
                    // If the clock moved in a direction quite a bit, rebase the repetition period
                    if (nowNanoseconds + CLOCK_DRIFT_TOLERANCE_NANOSECONDS < lastNowNanoseconds
                            || nowNanoseconds >= lastNowNanoseconds + periodInNanoseconds + CLOCK_DRIFT_TOLERANCE_NANOSECONDS) {
                        nextTick = nowNanoseconds + periodInNanoseconds;
                        /*
                         * Shift the start point back by the drift as if the whole thing
                         * started count periods ago.
                         */
                        startInNanoseconds = nextTick - (periodInNanoseconds * (++count));
                    } else {
                        nextTick = startInNanoseconds + (++count * periodInNanoseconds);
                    }
                    lastNowNanoseconds = nowNanoseconds;

                    long delay = nextTick - nowNanoseconds;
                    sd.replace(schedule(this, delay, TimeUnit.NANOSECONDS));
                }
            }

            @Override
            public Runnable getWrappedRunnable() {
                return this.decoratedRun;
            }
        }
    }

    static final class PeriodicDirectTask
    implements Disposable, Runnable, SchedulerRunnableIntrospection {

        @NonNull
        final Runnable run;

        @NonNull
        final Worker worker;

        volatile boolean disposed;

        PeriodicDirectTask(@NonNull Runnable run, @NonNull Worker worker) {
            this.run = run;
            this.worker = worker;
        }

        @Override
        public void run() {
            if (!disposed) {
                try {
                    run.run();
                } catch (Throwable ex) {
                    Exceptions.throwIfFatal(ex);
                    worker.dispose();
                    throw ExceptionHelper.wrapOrThrow(ex);
                }
            }
        }

        @Override
        public void dispose() {
            disposed = true;
            worker.dispose();
        }

        @Override
        public boolean isDisposed() {
            return disposed;
        }

        @Override
        public Runnable getWrappedRunnable() {
            return run;
        }
    }

    static final class DisposeTask implements Disposable, Runnable, SchedulerRunnableIntrospection {

        @NonNull
        final Runnable decoratedRun;

        @NonNull
        final Worker w;

        @Nullable
        Thread runner;

        DisposeTask(@NonNull Runnable decoratedRun, @NonNull Worker w) {
            this.decoratedRun = decoratedRun;
            this.w = w;
        }

        @Override
        public void run() {
            runner = Thread.currentThread();
            try {
                decoratedRun.run();
            } finally {
                dispose();
                runner = null;
            }
        }

        @Override
        public void dispose() {
            if (runner == Thread.currentThread() && w instanceof NewThreadWorker) {
                ((NewThreadWorker)w).shutdown();
            } else {
                w.dispose();
            }
        }

        @Override
        public boolean isDisposed() {
            return w.isDisposed();
        }

        @Override
        public Runnable getWrappedRunnable() {
            return this.decoratedRun;
        }
    }
}
