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

import rx.annotations.Experimental;
import rx.functions.*;
import rx.internal.schedulers.*;
import rx.schedulers.Schedulers;

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
     * Unsubscribing the {@link Worker} cancels all outstanding work and allows resources cleanup.
     */
    public abstract static class Worker implements Subscription {

        /**
         * Schedules an Action for execution.
         *
         * @param action
         *            Action to schedule
         * @return a subscription to be able to prevent or cancel the execution of the action
         */
        public abstract Subscription schedule(Action0 action);

        /**
         * Schedules an Action for execution at some point in the future.
         * <p>
         * Note to implementors: non-positive {@code delayTime} should be regarded as non-delayed schedule, i.e.,
         * as if the {@link #schedule(rx.functions.Action0)} was called.
         *
         * @param action
         *            the Action to schedule
         * @param delayTime
         *            time to wait before executing the action; non-positive values indicate an non-delayed
         *            schedule
         * @param unit
         *            the time unit of {@code delayTime}
         * @return a subscription to be able to prevent or cancel the execution of the action
         */
        public abstract Subscription schedule(final Action0 action, final long delayTime, final TimeUnit unit);

        /**
         * Schedules a cancelable action to be executed periodically. This default implementation schedules
         * recursively and waits for actions to complete (instead of potentially executing long-running actions
         * concurrently). Each scheduler that can do periodic scheduling in a better way should override this.
         * <p>
         * Note to implementors: non-positive {@code initialTime} and {@code period} should be regarded as
         * non-delayed scheduling of the first and any subsequent executions.
         *
         * @param action
         *            the Action to execute periodically
         * @param initialDelay
         *            time to wait before executing the action for the first time; non-positive values indicate
         *            an non-delayed schedule
         * @param period
         *            the time interval to wait each time in between executing the action; non-positive values
         *            indicate no delay between repeated schedules
         * @param unit
         *            the time unit of {@code period}
         * @return a subscription to be able to prevent or cancel the execution of the action
         */
        public Subscription schedulePeriodically(final Action0 action, long initialDelay, long period, TimeUnit unit) {
            return SchedulePeriodicHelper.schedulePeriodically(this, action,
                    initialDelay, period, unit, null);
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

    /**
     * Allows the use of operators for controlling the timing around when
     * actions scheduled on workers are actually done. This makes it possible to
     * layer additional behavior on this {@link Scheduler}. The only parameter
     * is a function that flattens an {@link Observable} of {@link Observable}
     * of {@link Completable}s into just one {@link Completable}. There must be
     * a chain of operators connecting the returned value to the source
     * {@link Observable} otherwise any work scheduled on the returned
     * {@link Scheduler} will not be executed.
     * <p>
     * When {@link Scheduler#createWorker()} is invoked a {@link Observable} of
     * {@link Completable}s is onNext'd to the combinator to be flattened. If
     * the inner {@link Observable} is not immediately subscribed to an calls to
     * {@link Worker#schedule} are buffered. Once the {@link Observable} is
     * subscribed to actions are then onNext'd as {@link Completable}s.
     * <p>
     * Finally the actions scheduled on the parent {@link Scheduler} when the
     * inner most {@link Completable}s are subscribed to.
     * <p>
     * When the {@link Worker} is unsubscribed the {@link Completable} emits an
     * onComplete and triggers any behavior in the flattening operator. The
     * {@link Observable} and all {@link Completable}s give to the flattening
     * function never onError.
     * <p>
     * Limit the amount concurrency two at a time without creating a new fix
     * size thread pool:
     *
     * <pre>
     * Scheduler limitScheduler = Schedulers.computation().when(workers -> {
     *     // use merge max concurrent to limit the number of concurrent
     *     // callbacks two at a time
     *     return Completable.merge(Observable.merge(workers), 2);
     * });
     * </pre>
     * <p>
     * This is a slightly different way to limit the concurrency but it has some
     * interesting benefits and drawbacks to the method above. It works by
     * limited the number of concurrent {@link Worker}s rather than individual
     * actions. Generally each {@link Observable} uses its own {@link Worker}.
     * This means that this will essentially limit the number of concurrent
     * subscribes. The danger comes from using operators like
     * {@link Observable#zip(Observable, Observable, rx.functions.Func2)} where
     * subscribing to the first {@link Observable} could deadlock the
     * subscription to the second.
     *
     * <pre>
     * Scheduler limitScheduler = Schedulers.computation().when(workers -> {
     *     // use merge max concurrent to limit the number of concurrent
     *     // Observables two at a time
     *     return Completable.merge(Observable.merge(workers, 2));
     * });
     * </pre>
     *
     * Slowing down the rate to no more than than 1 a second. This suffers from
     * the same problem as the one above I could find an {@link Observable}
     * operator that limits the rate without dropping the values (aka leaky
     * bucket algorithm).
     *
     * <pre>
     * Scheduler slowScheduler = Schedulers.computation().when(workers -> {
     *     // use concatenate to make each worker happen one at a time.
     *     return Completable.concat(workers.map(actions -> {
     *         // delay the starting of the next worker by 1 second.
     *         return Completable.merge(actions.delaySubscription(1, TimeUnit.SECONDS));
     *    }));
     * });
     * </pre>
     *
     * @param <S> a Scheduler and a Subscription
     * @param combine the function that takes a two-level nested Observable sequence of a Completable and returns
     * the Completable that will be subscribed to and should trigger the execution of the scheduled Actions.
     * @return the Scheduler with the customized execution behavior
     */
    @SuppressWarnings("unchecked")
    @Experimental
    public <S extends Scheduler & Subscription> S when(Func1<Observable<Observable<Completable>>, Completable> combine) {
        return (S) new SchedulerWhen(combine, this);
    }
}
