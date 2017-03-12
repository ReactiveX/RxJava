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

import java.util.Queue;
import java.util.concurrent.*;

import io.reactivex.Scheduler;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.*;
import io.reactivex.internal.disposables.EmptyDisposable;
import io.reactivex.internal.functions.ObjectHelper;

/**
 * A special, non thread-safe scheduler for testing operators that require
 * a scheduler without introducing real concurrency and allows manually advancing
 * a virtual time.
 */
public final class TestScheduler extends Scheduler {
    /** The ordered queue for the runnable tasks. */
    final Queue<TimedRunnable> queue = new PriorityBlockingQueue<TimedRunnable>(11);
    /** The per-scheduler global order counter. */
    long counter;
    // Storing time in nanoseconds internally.
    volatile long time;

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
                return ObjectHelper.compare(count, o.count);
            }
            return ObjectHelper.compare(time, o.time);
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
        while (!queue.isEmpty()) {
            TimedRunnable current = queue.peek();
            if (current.time > targetTimeInNanoseconds) {
                break;
            }
            // if scheduled time is 0 (immediate) use current virtual time
            time = current.time == 0 ? time : current.time;
            queue.remove();

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
            final TimedRunnable timedAction = new TimedRunnable(this, time + unit.toNanos(delayTime), run, counter++);
            queue.add(timedAction);

            return Disposables.fromRunnable(new QueueRemove(timedAction));
        }

        @NonNull
        @Override
        public Disposable schedule(@NonNull Runnable run) {
            if (disposed) {
                return EmptyDisposable.INSTANCE;
            }
            final TimedRunnable timedAction = new TimedRunnable(this, 0, run, counter++);
            queue.add(timedAction);
            return Disposables.fromRunnable(new QueueRemove(timedAction));
        }

        @Override
        public long now(@NonNull TimeUnit unit) {
            return TestScheduler.this.now(unit);
        }

        final class QueueRemove implements Runnable {
            final TimedRunnable timedAction;

            QueueRemove(TimedRunnable timedAction) {
                this.timedAction = timedAction;
            }

            @Override
            public void run() {
                queue.remove(timedAction);
            }
        }
    }
}
