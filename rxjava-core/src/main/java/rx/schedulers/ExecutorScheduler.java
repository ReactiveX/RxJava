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
package rx.schedulers;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import rx.Scheduler;
import rx.Subscription;
import rx.functions.Action1;
import rx.subscriptions.MultipleAssignmentSubscription;
import rx.subscriptions.Subscriptions;

/**
 * A {@link Scheduler} implementation that uses an {@link Executor} or {@link ScheduledExecutorService}
 * implementation.
 * <p>
 * Note that if an {@link Executor} implementation is used instead of {@link ScheduledExecutorService} then a
 * system-wide Timer will be used to handle delayed events.
 */
public class ExecutorScheduler extends Scheduler {
    private final Executor executor;

    /**
     * @deprecated Use Schedulers.executor();
     */
    @Deprecated
    public ExecutorScheduler(Executor executor) {
        this.executor = executor;
    }

    /**
     * @deprecated Use Schedulers.executor();
     */
    @Deprecated
    public ExecutorScheduler(ScheduledExecutorService executor) {
        this.executor = executor;
    }

    @Override
    public Subscription schedule(Action1<Scheduler.Inner> action) {
        InnerExecutorScheduler inner = new InnerExecutorScheduler();
        inner.schedule(action);
        return inner.innerSubscription;
    }

    @Override
    public Subscription schedule(Action1<Inner> action, long delayTime, TimeUnit unit) {
        InnerExecutorScheduler inner = new InnerExecutorScheduler();
        inner.schedule(action, delayTime, unit);
        return inner.innerSubscription;
    }

    @Override
    public Subscription schedulePeriodically(final Action1<Scheduler.Inner> action, long initialDelay, long period, TimeUnit unit) {
        if (executor instanceof ScheduledExecutorService) {
            final InnerExecutorScheduler inner = new InnerExecutorScheduler();
            ScheduledFuture<?> f = ((ScheduledExecutorService) executor).scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {
                    if (inner.isUnsubscribed()) {
                        // don't execute if unsubscribed
                        return;
                    }
                    action.call(inner);
                }
            }, initialDelay, period, unit);

            inner.innerSubscription.set(Subscriptions.from(f));
            return inner;
        } else {
            return super.schedulePeriodically(action, initialDelay, period, unit);
        }
    }

    private class InnerExecutorScheduler extends Scheduler.Inner {

        private final MultipleAssignmentSubscription innerSubscription = new MultipleAssignmentSubscription();

        @Override
        public void schedule(final Action1<Scheduler.Inner> action, long delayTime, TimeUnit unit) {
            if (innerSubscription.isUnsubscribed()) {
                // don't schedule, we are unsubscribed
                return;
            }

            final Inner _inner = this;
            if (executor instanceof ScheduledExecutorService) {
                // we are a ScheduledExecutorService so can do proper scheduling
                ScheduledFuture<?> f = ((ScheduledExecutorService) executor).schedule(new Runnable() {
                    @Override
                    public void run() {
                        if (innerSubscription.isUnsubscribed()) {
                            // don't execute if unsubscribed
                            return;
                        }
                        // when the delay has passed we now do the work on the actual scheduler
                        action.call(_inner);
                    }
                }, delayTime, unit);
                // add the ScheduledFuture as a subscription so we can cancel the scheduled action if an unsubscribe happens
                innerSubscription.set(Subscriptions.from(f));
            } else {
                // we are not a ScheduledExecutorService so can't directly schedule
                if (delayTime == 0) {
                    // no delay so put on the thread-pool right now
                    schedule(action);
                } else {
                    // there is a delay and this isn't a ScheduledExecutorService so we'll use a system-wide ScheduledExecutorService
                    // to handle the scheduling and once it's ready then execute on this Executor
                    ScheduledFuture<?> f = GenericScheduledExecutorService.getInstance().schedule(new Runnable() {

                        @Override
                        public void run() {
                            if (innerSubscription.isUnsubscribed()) {
                                // don't execute if unsubscribed
                                return;
                            }
                            // now execute on the real Executor (by using the other overload that schedules for immediate execution)
                            schedule(action);
                        }
                    }, delayTime, unit);
                    // add the ScheduledFuture as a subscription so we can cancel the scheduled action if an unsubscribe happens
                    innerSubscription.set(Subscriptions.from(f));
                }
            }
        }

        @Override
        public void schedule(final Action1<Scheduler.Inner> action) {
            if (innerSubscription.isUnsubscribed()) {
                // don't schedule, we are unsubscribed
                return;
            }

            // work to be done on a thread
            final Inner _inner = this;
            Runnable r = new Runnable() {
                @Override
                public void run() {
                    if (innerSubscription.isUnsubscribed()) {
                        // don't execute if unsubscribed
                        return;
                    }
                    action.call(_inner);
                }
            };

            // submit for immediate execution
            if (executor instanceof ExecutorService) {
                // we are an ExecutorService so get a Future back that supports unsubscribe
                Future<?> f = ((ExecutorService) executor).submit(r);
                // add the Future as a subscription so we can cancel the scheduled action if an unsubscribe happens
                innerSubscription.set(Subscriptions.from(f));
            } else {
                // we are the lowest common denominator so can't unsubscribe once we execute
                executor.execute(r);
            }
        }

        @Override
        public void unsubscribe() {
            innerSubscription.unsubscribe();
        }

        @Override
        public boolean isUnsubscribed() {
            return innerSubscription.isUnsubscribed();
        }

    }

}
