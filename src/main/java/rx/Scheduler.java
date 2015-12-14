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
package rx;

import java.util.concurrent.TimeUnit;

import rx.functions.Action0;
import rx.schedulers.Schedulers;
import rx.subscriptions.MultipleAssignmentSubscription;

/**
 * A {@code Scheduler} is an object that schedules units of work. You can find common implementations of this
 * class in {@link Schedulers}.
 */
public abstract class Scheduler {
/*
 * Why is this an abstract class instead of an interface?
 *
 *  : Java doesn't support extension methods and there are many overload methods needing default
 *    implementations.
 *
 *  : Virtual extension methods aren't available until Java8 which RxJava will not set as a minimum target for
 *    a long time.
 *
 *  : If only an interface were used Scheduler implementations would then need to extend from an
 *    AbstractScheduler pair that gives all of the functionality unless they intend on copy/pasting the
 *    functionality.
 *
 *  : Without virtual extension methods even additive changes are breaking and thus severely impede library
 *    maintenance.
 */

    /** 
     * The tolerance for a clock drift in nanoseconds where the periodic scheduler will rebase. 
     * <p>
     * The associated system parameter, {@code rx.scheduler.drift-tolerance}, expects its value in minutes.
     */
    static final long CLOCK_DRIFT_TOLERANCE_NANOS;
    static {
        CLOCK_DRIFT_TOLERANCE_NANOS = TimeUnit.MINUTES.toNanos(
                Long.getLong("rx.scheduler.drift-tolerance", 15));
    }
    
    /**
     * Retrieves or creates a new {@link Scheduler.Worker} that represents serial execution of actions.
     * <p>
     * When work is completed it should be unsubscribed using {@link Scheduler.Worker#unsubscribe()}.
     * <p>
     * Work on a {@link Scheduler.Worker} is guaranteed to be sequential.
     * 
     * @return a Worker representing a serial queue of actions to be executed
     */
    public abstract Worker createWorker();

    /**
     * Sequential Scheduler for executing actions on a single thread or event loop.
     * <p>
     * Unsubscribing the {@link Worker} unschedules all outstanding work and allows resources cleanup.
     */
    public abstract static class Worker implements Subscription {

        /**
         * Schedules an Action for execution.
         * 
         * @param action
         *            Action to schedule
         * @return a subscription to be able to unsubscribe the action (unschedule it if not executed)
         */
        public abstract Subscription schedule(Action0 action);

        /**
         * Schedules an Action for execution at some point in the future.
         * <p>
         * Note to implementors: non-positive {@code delayTime} should be regarded as undelayed schedule, i.e.,
         * as if the {@link #schedule(rx.functions.Action0)} was called.
         *
         * @param action
         *            the Action to schedule
         * @param delayTime
         *            time to wait before executing the action; non-positive values indicate an undelayed
         *            schedule
         * @param unit
         *            the time unit of {@code delayTime}
         * @return a subscription to be able to unsubscribe the action (unschedule it if not executed)
         */
        public abstract Subscription schedule(final Action0 action, final long delayTime, final TimeUnit unit);

        /**
         * Schedules a cancelable action to be executed periodically. This default implementation schedules
         * recursively and waits for actions to complete (instead of potentially executing long-running actions
         * concurrently). Each scheduler that can do periodic scheduling in a better way should override this.
         * <p>
         * Note to implementors: non-positive {@code initialTime} and {@code period} should be regarded as
         * undelayed scheduling of the first and any subsequent executions.
         * 
         * @param action
         *            the Action to execute periodically
         * @param initialDelay
         *            time to wait before executing the action for the first time; non-positive values indicate
         *            an undelayed schedule
         * @param period
         *            the time interval to wait each time in between executing the action; non-positive values
         *            indicate no delay between repeated schedules
         * @param unit
         *            the time unit of {@code period}
         * @return a subscription to be able to unsubscribe the action (unschedule it if not executed)
         */
        public Subscription schedulePeriodically(final Action0 action, long initialDelay, long period, TimeUnit unit) {
            final long periodInNanos = unit.toNanos(period);
            final long firstNowNanos = TimeUnit.MILLISECONDS.toNanos(now());
            final long firstStartInNanos = firstNowNanos + unit.toNanos(initialDelay);

            final MultipleAssignmentSubscription mas = new MultipleAssignmentSubscription();
            final Action0 recursiveAction = new Action0() {
                long count;
                long lastNowNanos = firstNowNanos;
                long startInNanos = firstStartInNanos;
                @Override
                public void call() {
                    if (!mas.isUnsubscribed()) {
                        action.call();
                        
                        long nextTick;
                        
                        long nowNanos = TimeUnit.MILLISECONDS.toNanos(now());
                        // If the clock moved in a direction quite a bit, rebase the repetition period
                        if (nowNanos + CLOCK_DRIFT_TOLERANCE_NANOS < lastNowNanos
                                || nowNanos >= lastNowNanos + periodInNanos + CLOCK_DRIFT_TOLERANCE_NANOS) {
                            nextTick = nowNanos + periodInNanos;
                            /* 
                             * Shift the start point back by the drift as if the whole thing
                             * started count periods ago.
                             */
                            startInNanos = nextTick - (periodInNanos * (++count));
                        } else {
                            nextTick = startInNanos + (++count * periodInNanos);
                        }
                        lastNowNanos = nowNanos;
                        
                        long delay = nextTick - nowNanos;
                        mas.set(schedule(this, delay, TimeUnit.NANOSECONDS));
                    }
                }
            };
            MultipleAssignmentSubscription s = new MultipleAssignmentSubscription();
            // Should call `mas.set` before `schedule`, or the new Subscription may replace the old one.
            mas.set(s);
            s.set(schedule(recursiveAction, initialDelay, unit));
            return mas;
        }

        /**
         * Gets the current time, in milliseconds, according to this Scheduler.
         *
         * @return the scheduler's notion of current absolute time in milliseconds
         */
        public long now() {
            return System.currentTimeMillis();
        }
    }

    /**
     * Gets the current time, in milliseconds, according to this Scheduler.
     *
     * @return the scheduler's notion of current absolute time in milliseconds
     */
    public long now() {
        return System.currentTimeMillis();
    }

}
