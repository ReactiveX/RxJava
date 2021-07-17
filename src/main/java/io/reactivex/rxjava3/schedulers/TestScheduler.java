/*
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

package io.reactivex.rxjava3.schedulers;

import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.rxjava3.annotations.*;
import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.internal.disposables.EmptyDisposable;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;

/**
 * A special, non thread-safe scheduler for testing operators that require
 * a scheduler without introducing real concurrency and allows manually advancing
 * a virtual time.
 * <p>
 * By default, the tasks submitted via the various {@code schedule} methods are not
 * wrapped by the {@link RxJavaPlugins#onSchedule(Runnable)} hook. To enable this behavior,
 * create a {@code TestScheduler} via {@link #TestScheduler(boolean)} or {@link #TestScheduler(long, TimeUnit, boolean)}.
 */
public final class TestScheduler extends Scheduler {
    /** The ordered queue for the runnable tasks. */
    final Queue<TimedRunnable> queue = new PriorityBlockingQueue<>(11);
    /** Use the {@link RxJavaPlugins#onSchedule(Runnable)} hook when scheduling tasks. */
    final boolean useOnScheduleHook;
    /** The per-scheduler global order counter. */
    long counter;
    // Storing time in nanoseconds internally.
    volatile long time;

    /**
     * Creates a new TestScheduler with initial virtual time of zero.
     */
    public TestScheduler() {
        this(false);
    }

    /**
     * Creates a new TestScheduler with the option to use the
     * {@link RxJavaPlugins#onSchedule(Runnable)} hook when scheduling tasks.
     * <p>History: 3.0.10 - experimental
     * @param useOnScheduleHook if {@code true}, the tasks submitted to this
     *                          TestScheduler is wrapped via the
     *                          {@link RxJavaPlugins#onSchedule(Runnable)} hook
     * @since 3.1.0
     */
    public TestScheduler(boolean useOnScheduleHook) {
        this.useOnScheduleHook = useOnScheduleHook;
    }

    /**
     * Creates a new TestScheduler with the specified initial virtual time.
     *
     * @param delayTime
     *          the point in time to move the Scheduler's clock to
     * @param unit
     *          the units of time that {@code delayTime} is expressed in
     */
    public TestScheduler(long delayTime, TimeUnit unit) {
        this(delayTime, unit, false);
    }

    /**
     * Creates a new TestScheduler with the specified initial virtual time
     * and with the option to use the
     * {@link RxJavaPlugins#onSchedule(Runnable)} hook when scheduling tasks.
     * <p>History: 3.0.10 - experimental
     * @param delayTime
     *          the point in time to move the Scheduler's clock to
     * @param unit
     *          the units of time that {@code delayTime} is expressed in
     * @param useOnScheduleHook if {@code true}, the tasks submitted to this
     *                          TestScheduler is wrapped via the
     *                          {@link RxJavaPlugins#onSchedule(Runnable)} hook
     * @since 3.1.0
     */
    public TestScheduler(long delayTime, TimeUnit unit, boolean useOnScheduleHook) {
        time = unit.toNanos(delayTime);
        this.useOnScheduleHook = useOnScheduleHook;
    }

    static final class TimedRunnable implements Comparable<TimedRunnable> {

        final long time;
        final Runnable run;
        final TestWorker scheduler;
        final long count; // for differentiating tasks at same time

        TimedRunnable(TestWorker scheduler, long time, Runnable run, long count) {
            this.time = time;
            this.run = run;
            this.scheduler = scheduler;
            this.count = count;
        }

        @Override
        public String toString() {
            return String.format("TimedRunnable(time = %d, run = %s)", time, run.toString());
        }

        @Override
        public int compareTo(TimedRunnable o) {
            if (time == o.time) {
                return Long.compare(count, o.count);
            }
            return Long.compare(time, o.time);
        }
    }

    @Override
    public long now(@NonNull TimeUnit unit) {
        return unit.convert(time, TimeUnit.NANOSECONDS);
    }

    /**
     * Moves the Scheduler's clock forward by a specified amount of time.
     *
     * @param delayTime
     *          the amount of time to move the Scheduler's clock forward
     * @param unit
     *          the units of time that {@code delayTime} is expressed in
     */
    public void advanceTimeBy(long delayTime, TimeUnit unit) {
        advanceTimeTo(time + unit.toNanos(delayTime), TimeUnit.NANOSECONDS);
    }

    /**
     * Moves the Scheduler's clock to a particular moment in time.
     *
     * @param delayTime
     *          the point in time to move the Scheduler's clock to
     * @param unit
     *          the units of time that {@code delayTime} is expressed in
     */
    public void advanceTimeTo(long delayTime, TimeUnit unit) {
        long targetTime = unit.toNanos(delayTime);
        triggerActions(targetTime);
    }

    /**
     * Triggers any actions that have not yet been triggered and that are scheduled to be triggered at or
     * before this Scheduler's present time.
     */
    public void triggerActions() {
        triggerActions(time);
    }

    private void triggerActions(long targetTimeInNanoseconds) {
        for (;;) {
            TimedRunnable current = queue.peek();
            if (current == null || current.time > targetTimeInNanoseconds) {
                break;
            }
            // if scheduled time is 0 (immediate) use current virtual time
            time = current.time == 0 ? time : current.time;
            queue.remove(current);

            // Only execute if not unsubscribed
            if (!current.scheduler.disposed) {
                current.run.run();
            }
        }
        time = targetTimeInNanoseconds;
    }

    @NonNull
    @Override
    public Worker createWorker() {
        return new TestWorker();
    }

    final class TestWorker extends Worker {

        volatile boolean disposed;

        @Override
        public void dispose() {
            disposed = true;
        }

        @Override
        public boolean isDisposed() {
            return disposed;
        }

        @NonNull
        @Override
        public Disposable schedule(@NonNull Runnable run, long delayTime, @NonNull TimeUnit unit) {
            if (disposed) {
                return EmptyDisposable.INSTANCE;
            }
            if (useOnScheduleHook) {
                run = RxJavaPlugins.onSchedule(run);
            }
            final TimedRunnable timedAction = new TimedRunnable(this, time + unit.toNanos(delayTime), run, counter++);
            queue.add(timedAction);

            return new QueueRemove(timedAction);
        }

        @NonNull
        @Override
        public Disposable schedule(@NonNull Runnable run) {
            if (disposed) {
                return EmptyDisposable.INSTANCE;
            }
            if (useOnScheduleHook) {
                run = RxJavaPlugins.onSchedule(run);
            }
            final TimedRunnable timedAction = new TimedRunnable(this, 0, run, counter++);
            queue.add(timedAction);
            return new QueueRemove(timedAction);
        }

        @Override
        public long now(@NonNull TimeUnit unit) {
            return TestScheduler.this.now(unit);
        }

        final class QueueRemove extends AtomicReference<TimedRunnable> implements Disposable {

            private static final long serialVersionUID = -7874968252110604360L;

            QueueRemove(TimedRunnable timedAction) {
                this.lazySet(timedAction);
            }

            @Override
            public void dispose() {
                TimedRunnable tr = getAndSet(null);
                if (tr != null) {
                    queue.remove(tr);
                }
            }

            @Override
            public boolean isDisposed() {
                return get() == null;
            }
        }
    }
}
