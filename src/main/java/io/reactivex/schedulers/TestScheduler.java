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

import java.util.*;
import java.util.concurrent.TimeUnit;

import io.reactivex.Scheduler;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.disposables.EmptyDisposable;
import io.reactivex.internal.functions.Objects;

/**
 * A special, non thread-safe scheduler for testing operators that require
 * a scheduler without introducing real concurrency and allows manually advancing
 * a virtual time.
 */
public final class TestScheduler extends Scheduler {
    /** The ordered queue for the runnable tasks. */
    private final Queue<TimedRunnable> queue = new PriorityQueue<TimedRunnable>(11);
    /** The per-scheduler global order counter. */
    long counter;

    private static final class TimedRunnable implements Comparable<TimedRunnable> {

        private final long time;
        private final Runnable run;
        private final TestWorker scheduler;
        private final long count; // for differentiating tasks at same time

        private TimedRunnable(TestWorker scheduler, long time, Runnable run, long count) {
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
                return Objects.compare(count, o.count);
            }
            return Objects.compare(time, o.time);
        }
    }

    // Storing time in nanoseconds internally.
    private long time;

    @Override
    public long now(TimeUnit unit) {
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

    private void triggerActions(long targetTimeInNanos) {
        while (!queue.isEmpty()) {
            TimedRunnable current = queue.peek();
            if (current.time > targetTimeInNanos) {
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
        time = targetTimeInNanos;
    }

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
        public Disposable schedule(Runnable run, long delayTime, TimeUnit unit) {
            if (disposed) {
                return EmptyDisposable.INSTANCE;
            }
            final TimedRunnable timedAction = new TimedRunnable(this, time + unit.toNanos(delayTime), run, counter++);
            queue.add(timedAction);
            
            return new Disposable() {
                @Override
                public void dispose() {
                    queue.remove(timedAction);
                }
            };
        }

        @Override
        public Disposable schedule(Runnable run) {
            if (disposed) {
                return EmptyDisposable.INSTANCE;
            }
            final TimedRunnable timedAction = new TimedRunnable(this, 0, run, counter++);
            queue.add(timedAction);
            return new Disposable() {
                @Override
                public void dispose() {
                    queue.remove(timedAction);
                }
            };
        }

        @Override
        public long now(TimeUnit unit) {
            return TestScheduler.this.now(unit);
        }

    }
}
