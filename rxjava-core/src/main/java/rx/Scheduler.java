/**
 * Copyright 2013 Netflix, Inc.
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

import rx.functions.Action1;

/**
 * Represents an object that schedules units of work.
 * <p>
 * The methods left to implement are:
 * <ul>
 * <li>{@code <T> Subscription schedule(T state, Func2<Scheduler, T, Subscription> action, long delayTime, TimeUnit unit)}</li>
 * <li>{@code <T> Subscription schedule(T state, Func2<Scheduler, T, Subscription> action)}</li>
 * </ul>
 * <p>
 * Why is this an abstract class instead of an interface?
 * <p>
 * <ol>
 * <li>Java doesn't support extension methods and there are many overload methods needing default
 *     implementations.</li>
 * <li>Virtual extension methods aren't available until Java8 which RxJava will not set as a minimum target for
 *     a long time.</li>
 * <li>If only an interface were used Scheduler implementations would then need to extend from an
 *     AbstractScheduler pair that gives all of the functionality unless they intend on copy/pasting the
 *     functionality.</li>
 * <li>Without virtual extension methods even additive changes are breaking and thus severely impede library
 *     maintenance.</li>
 * </ol>
 */
public abstract class Scheduler {

    /**
     * Schedules an Action on a new Scheduler instance (typically another thread) for execution.
     * 
     * @param action
     *            Action to schedule
     * @return a subscription to be able to unsubscribe from action
     */

    public abstract Subscription schedule(Action1<Scheduler.Inner> action);

    /**
     * Schedules an Action on a new Scheduler instance (typically another thread) for execution at some point
     * in the future.
     * 
     * @param action
     *            the Action to schedule
     * @param delayTime
     *            time to wait before executing the action
     * @param unit
     *            the time unit the delay time is given in
     * @return a subscription to be able to unsubscribe from action
     */
    public abstract Subscription schedule(final Action1<Scheduler.Inner> action, final long delayTime, final TimeUnit unit);

    /**
     * Schedules a cancelable action to be executed periodically. This default implementation schedules
     * recursively and waits for actions to complete (instead of potentially executing long-running actions
     * concurrently). Each scheduler that can do periodic scheduling in a better way should override this.
     * 
     * @param action
     *            the Action to execute periodically
     * @param initialDelay
     *            time to wait before executing the action for the first time
     * @param period
     *            the time interval to wait each time in between executing the action
     * @param unit
     *            the time unit the interval above is given in
     * @return a subscription to be able to unsubscribe from action
     */
    public Subscription schedulePeriodically(final Action1<Scheduler.Inner> action, long initialDelay, long period, TimeUnit unit) {
        final long periodInNanos = unit.toNanos(period);

        final Action1<Scheduler.Inner> recursiveAction = new Action1<Scheduler.Inner>() {
            @Override
            public void call(Inner inner) {
                if (!inner.isUnsubscribed()) {
                    long startedAt = now();
                    action.call(inner);
                    long timeTakenByActionInNanos = TimeUnit.MILLISECONDS.toNanos(now() - startedAt);
                    inner.schedule(this, periodInNanos - timeTakenByActionInNanos, TimeUnit.NANOSECONDS);
                }
            }
        };
        return schedule(recursiveAction, initialDelay, unit);
    }

    public final Subscription scheduleRecursive(final Action1<Recurse> action) {
        return schedule(new Action1<Inner>() {

            @Override
            public void call(Inner inner) {
                action.call(new Recurse(inner, action));
            }

        });
    }

    public static final class Recurse {
        private final Action1<Recurse> action;
        private final Inner inner;

        private Recurse(Inner inner, Action1<Recurse> action) {
            this.inner = inner;
            this.action = action;
        }

        /**
         * Schedule the current function for execution immediately.
         */
        public final void schedule() {
            final Recurse self = this;
            inner.schedule(new Action1<Inner>() {

                @Override
                public void call(Inner _inner) {
                    action.call(self);
                }

            });
        }

        /**
         * Schedule the current function for execution in the future.
         */
        public final void schedule(long delay, TimeUnit unit) {
            final Recurse self = this;
            inner.schedule(new Action1<Inner>() {

                @Override
                public void call(Inner _inner) {
                    action.call(self);
                }

            }, delay, unit);
        }
    }

    public abstract static class Inner implements Subscription {

        /**
         * Schedules an action to be executed in delayTime.
         * 
         * @param delayTime
         *            time the action is to be delayed before executing
         * @param unit
         *            time unit of the delay time
         */
        public abstract void schedule(Action1<Scheduler.Inner> action, long delayTime, TimeUnit unit);

        /**
         * Schedules a cancelable action to be executed in delayTime.
         * 
         */
        public abstract void schedule(Action1<Scheduler.Inner> action);

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
    public int degreeOfParallelism() {
        return Runtime.getRuntime().availableProcessors();
    }

    /**
     * @return the scheduler's notion of current absolute time in milliseconds.
     */
    public long now() {
        return System.currentTimeMillis();
    }

}
