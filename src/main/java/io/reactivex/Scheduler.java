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

package io.reactivex;

import java.util.concurrent.TimeUnit;

import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.internal.disposables.SequentialDisposable;
import io.reactivex.internal.util.ExceptionHelper;
import io.reactivex.plugins.RxJavaPlugins;

/**
 * A {@code Scheduler} is an object that specifies an API for scheduling
 * units of work with or without delays or periodically. 
 * You can get common instances of this class in {@link io.reactivex.schedulers.Schedulers}.
 */
public abstract class Scheduler {
    /**
     * The tolerance for a clock drift in nanoseconds where the periodic scheduler will rebase.
     * <p>
     * The associated system parameter, {@code rx.scheduler.drift-tolerance}, expects its value in minutes.
     */
    static final long CLOCK_DRIFT_TOLERANCE_NANOSECONDS;
    static {
        CLOCK_DRIFT_TOLERANCE_NANOSECONDS = TimeUnit.MINUTES.toNanos(
                Long.getLong("rx2.scheduler.drift-tolerance", 15));
    }

    /**
     * Returns the clock drift tolerance in nanoseconds.
     * <p>Related system property: {@code rx2.scheduler.drift-tolerance} in minutes
     * @return the tolerance in nanoseconds
     * @since 2.0
     */
    public static long clockDriftTolerance() {
        return CLOCK_DRIFT_TOLERANCE_NANOSECONDS;
    }
    
    
    /**
     * Retrieves or creates a new {@link Scheduler.Worker} that represents serial execution of actions.
     * <p>
     * When work is completed it should be unsubscribed using {@link Scheduler.Worker#dispose()}.
     * <p>
     * Work on a {@link Scheduler.Worker} is guaranteed to be sequential.
     *
     * @return a Worker representing a serial queue of actions to be executed
     */
    public abstract Worker createWorker();

    /**
     * Returns the 'current time' of the Scheduler in the specified time unit.
     * @param unit the time unit
     * @return the 'current time'
     * @since 2.0
     */
    public long now(TimeUnit unit) {
        return unit.convert(System.currentTimeMillis(), TimeUnit.MILLISECONDS);
    }

    /** 
     * Allows the Scheduler instance to start threads 
     * and accept tasks on them.
     * <p>Implementations should make sure the call is idempotent and thread-safe.
     * @since 2.0
     */
    public void start() {
        
    }
    
    /** 
     * Instructs the Scheduler instance to stop threads 
     * and stop accepting tasks on any outstanding Workers. 
     * <p>Implementations should make sure the call is idempotent and thread-safe.
     * @since 2.0
     */
    public void shutdown() {
        
    }
    
    /**
     * Schedules the given task on this scheduler non-delayed execution.
     * 
     * <p>
     * This method is safe to be called from multiple threads but there are no
     * ordering guarantees between tasks.
     * 
     * @param run the task to execute
     * 
     * @return the Disposable instance that let's one cancel this particular task.
     * @since 2.0
     */
    public Disposable scheduleDirect(Runnable run) {
        return scheduleDirect(run, 0L, TimeUnit.NANOSECONDS);
    }

    /**
     * Schedules the execution of the given task with the given delay amount.
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
    public Disposable scheduleDirect(Runnable run, long delay, TimeUnit unit) {
        final Worker w = createWorker();
        
        final Runnable decoratedRun = RxJavaPlugins.onSchedule(run);
        
        w.schedule(new Runnable() {
            @Override
            public void run() {
                try {
                    decoratedRun.run();
                } finally {
                    w.dispose();
                }
            }
        }, delay, unit);
        
        return w;
    }
    
    /**
     * Schedules a periodic execution of the given task with the given initial delay and period.
     * 
     * <p>
     * This method is safe to be called from multiple threads but there are no
     * ordering guarantees between tasks.
     * 
     * <p>
     * The periodic execution is at a fixed rate, that is, the first execution will be after the initial
     * delay, the second after initialDelay + period, the third after initialDelay + 2 * period, and so on.
     * 
     * @param run the task to schedule
     * @param initialDelay the initial delay amount, non-positive values indicate non-delayed scheduling
     * @param period the period at which the task should be re-executed
     * @param unit the unit of measure of the delay amount
     * @return the Disposable that let's one cancel this particular delayed task.
     * @since 2.0
     */
    public Disposable schedulePeriodicallyDirect(Runnable run, long initialDelay, long period, TimeUnit unit) {
        final Worker w = createWorker();
        
        final Runnable decoratedRun = RxJavaPlugins.onSchedule(run);

        PeriodicDirectTask periodicTask = new PeriodicDirectTask(decoratedRun, w);
        
        w.schedulePeriodically(periodicTask, initialDelay, period, unit);
        
        return periodicTask;
    }

    /**
     * Sequential Scheduler for executing actions on a single thread or event loop.
     * <p>
     * Unsubscribing the {@link Worker} cancels all outstanding work and allows resource cleanup.
     */
    public static abstract class Worker implements Disposable {
        /**
         * Schedules a Runnable for execution without delay.
         * 
         * <p>The default implementation delegates to {@link #schedule(Runnable, long, TimeUnit)}.
         * 
         * @param run
         *            Runnable to schedule
         * @return a Disposable to be able to unsubscribe the action (cancel it if not executed)
         */
        public Disposable schedule(Runnable run) {
            return schedule(run, 0L, TimeUnit.NANOSECONDS);
        }
        
        /**
         * Schedules an Runnable for execution at some point in the future.
         * <p>
         * Note to implementors: non-positive {@code delayTime} should be regarded as non-delayed schedule, i.e.,
         * as if the {@link #schedule(Runnable)} was called.
         *
         * @param run
         *            the Runnable to schedule
         * @param delay
         *            time to wait before executing the action; non-positive values indicate an non-delayed
         *            schedule
         * @param unit
         *            the time unit of {@code delayTime}
         * @return a Disposable to be able to unsubscribe the action (cancel it if not executed)
         */
        public abstract Disposable schedule(Runnable run, long delay, TimeUnit unit);

        /**
         * Schedules a cancelable action to be executed periodically. This default implementation schedules
         * recursively and waits for actions to complete (instead of potentially executing long-running actions
         * concurrently). Each scheduler that can do periodic scheduling in a better way should override this.
         * <p>
         * Note to implementors: non-positive {@code initialTime} and {@code period} should be regarded as
         * non-delayed scheduling of the first and any subsequent executions.
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
        public Disposable schedulePeriodically(Runnable run, final long initialDelay, final long period, final TimeUnit unit) {
            final SequentialDisposable first = new SequentialDisposable();

            final SequentialDisposable sd = new SequentialDisposable(first);
            
            final Runnable decoratedRun = RxJavaPlugins.onSchedule(run);

            final long periodInNanoseconds = unit.toNanos(period);
            final long firstNowNanoseconds = now(TimeUnit.NANOSECONDS);
            final long firstStartInNanoseconds = firstNowNanoseconds + unit.toNanos(initialDelay);

            first.replace(schedule(new PeriodicTask(firstStartInNanoseconds, decoratedRun, firstNowNanoseconds, sd,
                    periodInNanoseconds), initialDelay, unit));
            
            return sd;
        }
        
        /**
         * Returns the 'current time' of the Worker in the specified time unit.
         * @param unit the time unit
         * @return the 'current time'
         * @since 2.0
         */
        public long now(TimeUnit unit) {
            return unit.convert(System.currentTimeMillis(), TimeUnit.MILLISECONDS);
        }

        /**
         * Holds state and logic to calculate when the next delayed invocation
         * of this task has to happen (accounting for clock drifts).
         */
        final class PeriodicTask implements Runnable {
            final long firstStartInNanoseconds;
            final Runnable decoratedRun;
            final long firstNowNanoseconds;
            final SequentialDisposable sd;
            final long periodInNanoseconds;
            long count;
            long lastNowNanoseconds;
            long startInNanoseconds;

            PeriodicTask(long firstStartInNanoseconds, Runnable decoratedRun,
                    long firstNowNanoseconds, SequentialDisposable sd, long periodInNanoseconds) {
                this.firstStartInNanoseconds = firstStartInNanoseconds;
                this.decoratedRun = decoratedRun;
                this.firstNowNanoseconds = firstNowNanoseconds;
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
        }
    }
    
    static class PeriodicDirectTask 
    implements Runnable, Disposable {
        final Runnable run;

        final Worker worker;
        
        volatile boolean disposed;

        public PeriodicDirectTask(Runnable run, Worker worker) {
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
    }
}
