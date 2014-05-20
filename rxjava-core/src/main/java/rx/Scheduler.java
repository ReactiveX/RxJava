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

/**
 * Represents an object that schedules units of work.
 * <p>
 * Common implementations can be found in {@link Schedulers}.
 * <p>
 * Why is this an abstract class instead of an interface?
 * <p>
 * <ol>
 * <li>Java doesn't support extension methods and there are many overload methods needing default
 * implementations.</li>
 * <li>Virtual extension methods aren't available until Java8 which RxJava will not set as a minimum target for
 * a long time.</li>
 * <li>If only an interface were used Scheduler implementations would then need to extend from an
 * AbstractScheduler pair that gives all of the functionality unless they intend on copy/pasting the
 * functionality.</li>
 * <li>Without virtual extension methods even additive changes are breaking and thus severely impede library
 * maintenance.</li>
 * </ol>
 */
public abstract class Scheduler {

    /**
     * Retrieve or create a new {@link Scheduler.Worker} that represents serial execution of actions.
     * <p>
     * When work is completed it should be unsubscribed using {@link Scheduler.Worker#unsubscribe()}.
     * <p>
     * Work on a {@link Scheduler.Worker} is guaranteed to be sequential.
     * 
     * @return Worker representing a serial queue of actions to be executed
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
         * <p>Note to implementors: non-positive {@code delayTime} should be regarded as
         * undelayed schedule, i.e., as if the {@link #schedule(rx.functions.Action0)} was called.
         * @param action
         *            the Action to schedule
         * @param delayTime
         *            time to wait before executing the action, non-positive values indicate an undelayed schedule
         * @param unit
         *            the time unit the delay time is given in
         * @return a subscription to be able to unsubscribe the action (unschedule it if not executed)
         */
        public abstract Subscription schedule(final Action0 action, final long delayTime, final TimeUnit unit);

        /**
         * Schedules a cancelable action to be executed periodically. This default implementation schedules
         * recursively and waits for actions to complete (instead of potentially executing long-running actions
         * concurrently). Each scheduler that can do periodic scheduling in a better way should override this.
         * <p>Note to implementors: non-positive {@code initialTime} and {@code period} should be regarded as
         * undelayed scheduling of the first and any subsequent executions.
         * 
         * @param action
         *            the Action to execute periodically
         * @param initialDelay
         *            time to wait before executing the action for the first time, 
         *            non-positive values indicate an undelayed schedule
         * @param period
         *            the time interval to wait each time in between executing the action,
         *            non-positive values indicate no delay between repeated schedules
         * @param unit
         *            the time unit the interval above is given in
         * @return a subscription to be able to unsubscribe the action (unschedule it if not executed)
         */
        public Subscription schedulePeriodically(final Action0 action, long initialDelay, long period, TimeUnit unit) {
            final long periodInNanos = unit.toNanos(period);
            final long startInNanos = TimeUnit.MILLISECONDS.toNanos(now()) + unit.toNanos(initialDelay);

            final Action0 recursiveAction = new Action0() {
                long count = 0;
                @Override
                public void call() {
                    if (!isUnsubscribed()) {
                        action.call();
                        long nextTick = startInNanos + (++count * periodInNanos);
                        schedule(this, nextTick - TimeUnit.MILLISECONDS.toNanos(now()), TimeUnit.NANOSECONDS);
                    }
                }
            };
            return schedule(recursiveAction, initialDelay, unit);
        }

        /**
         * @return the scheduler's notion of current absolute time in milliseconds.
         */
        public long now() {
            return System.currentTimeMillis();
        }
    }

    /**
     * Parallelism available to a Scheduler.
     * <p>
     * This defaults to {@code Runtime.getRuntime().availableProcessors()} but can be overridden for use cases
     * such as scheduling work on a computer cluster.
     * 
     * @return the scheduler's available degree of parallelism
     */
    public int parallelism() {
        return Runtime.getRuntime().availableProcessors();
    }

    /**
     * @return the scheduler's notion of current absolute time in milliseconds.
     */
    public long now() {
        return System.currentTimeMillis();
    }

}
