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
package rx.schedulers;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import rx.Scheduler;
import rx.Subscription;
import rx.subscriptions.CompositeSubscription;
import rx.subscriptions.MultipleAssignmentSubscription;
import rx.subscriptions.Subscriptions;
import rx.util.functions.Func2;

/**
 * A {@link Scheduler} implementation that uses an {@link Executor} or {@link ScheduledExecutorService} implementation.
 * <p>
 * Note that if an {@link Executor} implementation is used instead of {@link ScheduledExecutorService} then a system-wide Timer will be used to handle delayed events.
 */
public class ExecutorScheduler extends Scheduler {
    private final Executor executor;

    public ExecutorScheduler(Executor executor) {
        this.executor = executor;
    }

    public ExecutorScheduler(ScheduledExecutorService executor) {
        this.executor = executor;
    }

    @Override
    public <T> Subscription schedulePeriodically(final T state, final Func2<? super Scheduler, ? super T, ? extends Subscription> action, long initialDelay, long period, TimeUnit unit) {
        if (executor instanceof ScheduledExecutorService) {
            final CompositeSubscription subscriptions = new CompositeSubscription();

            ScheduledFuture<?> f = ((ScheduledExecutorService) executor).scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {
                    Subscription s = action.call(ExecutorScheduler.this, state);
                    subscriptions.add(s);
                }
            }, initialDelay, period, unit);

            subscriptions.add(Subscriptions.from(f));
            return subscriptions;

        } else {
            return super.schedulePeriodically(state, action, initialDelay, period, unit);
        }
    }

    @Override
    public <T> Subscription schedule(final T state, final Func2<? super Scheduler, ? super T, ? extends Subscription> action, long delayTime, TimeUnit unit) {
        final DiscardableAction<T> discardableAction = new DiscardableAction<T>(state, action);
        final InnerExecutorScheduler _scheduler = new InnerExecutorScheduler(executor);

        // all subscriptions that may need to be unsubscribed
        final CompositeSubscription subscription = new CompositeSubscription(discardableAction, _scheduler);

        if (executor instanceof ScheduledExecutorService) {
            // we are a ScheduledExecutorService so can do proper scheduling
            ScheduledFuture<?> f = ((ScheduledExecutorService) executor).schedule(new Runnable() {
                @Override
                public void run() {
                    // when the delay has passed we now do the work on the actual scheduler
                    discardableAction.call(_scheduler);
                }
            }, delayTime, unit);
            // add the ScheduledFuture as a subscription so we can cancel the scheduled action if an unsubscribe happens
            subscription.add(Subscriptions.from(f));
        } else {
            // we are not a ScheduledExecutorService so can't directly schedule
            if (delayTime == 0) {
                // no delay so put on the thread-pool right now
                return schedule(state, action);
            } else {
                // there is a delay and this isn't a ScheduledExecutorService so we'll use a system-wide ScheduledExecutorService
                // to handle the scheduling and once it's ready then execute on this Executor
                ScheduledFuture<?> f = GenericScheduledExecutorService.getInstance().schedule(new Runnable() {

                    @Override
                    public void run() {
                        // now execute on the real Executor (by using the other overload that schedules for immediate execution)
                        _scheduler.schedule(state, action);
                    }
                }, delayTime, unit);
                // add the ScheduledFuture as a subscription so we can cancel the scheduled action if an unsubscribe happens
                subscription.add(Subscriptions.from(f));
            }
        }
        return subscription;
    }

    @Override
    public <T> Subscription schedule(T state, Func2<? super Scheduler, ? super T, ? extends Subscription> action) {
        CompositeSubscription s = new CompositeSubscription();
        final DiscardableAction<T> discardableAction = new DiscardableAction<T>(state, action);
        s.add(discardableAction);

        final InnerExecutorScheduler _scheduler = new InnerExecutorScheduler(executor);
        s.add(_scheduler);

        s.add(execute(executor, new Runnable() {
            @Override
            public void run() {
                discardableAction.call(_scheduler);
            }
        }));

        return s;
    }

    /**
     * Execute on the given Executor and retrieve a Subscription
     * 
     * @param executor
     * @param r
     * @return
     */
    private static Subscription execute(Executor executor, Runnable r) {
        // submit for immediate execution
        if (executor instanceof ExecutorService) {
            // we are an ExecutorService so get a Future back that supports unsubscribe
            Future<?> f = ((ExecutorService) executor).submit(r);
            // add the Future as a subscription so we can cancel the scheduled action if an unsubscribe happens
            return Subscriptions.from(f);
        } else {
            // we are the lowest common denominator so can't unsubscribe once we execute
            executor.execute(r);
            return Subscriptions.empty();
        }
    }

    private static class InnerExecutorScheduler extends Scheduler implements Subscription {

        private final MultipleAssignmentSubscription childSubscription = new MultipleAssignmentSubscription();
        private final Executor executor;

        InnerExecutorScheduler(Executor executor) {
            this.executor = executor;
        }

        @Override
        public <T> Subscription schedule(T state, Func2<? super Scheduler, ? super T, ? extends Subscription> action) {
            if (childSubscription.isUnsubscribed()) {
                return childSubscription;
            }

            CompositeSubscription s = new CompositeSubscription();
            final DiscardableAction<T> discardableAction = new DiscardableAction<T>(state, action);
            s.add(discardableAction);

            final Scheduler _scheduler = this;

            s.add(execute(executor, new Runnable() {

                @Override
                public void run() {
                    discardableAction.call(_scheduler);
                }
            }));

            // replace the InnerExecutorScheduler child subscription with this one
            childSubscription.set(s);
            /*
             * TODO: Consider what will happen if `schedule` is run concurrently instead of recursively
             * and we lose subscriptions as the `childSubscription` only remembers the last one scheduled.
             * 
             * Not obvious that this should ever happen. Can it?
             * 
             * benjchristensen => Haven't been able to come up with a valid test case to prove this as an issue
             * so it may not be.
             */

            return childSubscription;
        }

        @Override
        public <T> Subscription schedule(final T state, final Func2<? super Scheduler, ? super T, ? extends Subscription> action, long delayTime, TimeUnit unit) {
            if (childSubscription.isUnsubscribed()) {
                return childSubscription;
            }

            CompositeSubscription s = new CompositeSubscription();
            final DiscardableAction<T> discardableAction = new DiscardableAction<T>(state, action);
            s.add(discardableAction);

            final Scheduler _scheduler = this;

            if (executor instanceof ScheduledExecutorService) {
                // we are a ScheduledExecutorService so can do proper scheduling
                ScheduledFuture<?> f = ((ScheduledExecutorService) executor).schedule(new Runnable() {
                    @Override
                    public void run() {
                        // when the delay has passed we now do the work on the actual scheduler
                        discardableAction.call(_scheduler);
                    }
                }, delayTime, unit);
                // replace the InnerExecutorScheduler child subscription with this one
                childSubscription.set(Subscriptions.from(f));
            } else {
                // we are not a ScheduledExecutorService so can't directly schedule
                if (delayTime == 0) {
                    // no delay so put on the thread-pool right now
                    return schedule(state, action);
                } else {
                    // there is a delay and this isn't a ScheduledExecutorService so we'll use a system-wide ScheduledExecutorService
                    // to handle the scheduling and once it's ready then execute on this Executor
                    ScheduledFuture<?> f = GenericScheduledExecutorService.getInstance().schedule(new Runnable() {

                        @Override
                        public void run() {
                            // now execute on the real Executor (by using the other overload that schedules for immediate execution)
                            _scheduler.schedule(state, action);
                        }
                    }, delayTime, unit);
                    // replace the InnerExecutorScheduler child subscription with this one
                    childSubscription.set(Subscriptions.from(f));
                }
            }
            return childSubscription;
        }

        @Override
        public void unsubscribe() {
            childSubscription.unsubscribe();
        }

    }

}
