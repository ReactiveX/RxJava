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
package rx.concurrency;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import rx.Scheduler;
import rx.Subscription;
import rx.subscriptions.CompositeSubscription;
import rx.subscriptions.Subscriptions;
import rx.util.functions.Func2;

/**
 * A {@link Scheduler} implementation that uses an {@link Executor} or {@link ScheduledExecutorService} implementation.
 * <p>
 * Note that if an {@link Executor} implementation is used instead of {@link ScheduledExecutorService} then a system-wide Timer will be used to handle delayed events.
 */
public class ExecutorScheduler extends AbstractScheduler {
    private final Executor executor;

    public ExecutorScheduler(Executor executor) {
        this.executor = executor;
    }

    public ExecutorScheduler(ScheduledExecutorService executor) {
        this.executor = executor;
    }

    @Override
    public <T> Subscription schedule(final T state, final Func2<Scheduler, T, Subscription> action, long delayTime, TimeUnit unit) {
        final DiscardableAction<T> discardableAction = new DiscardableAction<T>(state, action);
        final Scheduler _scheduler = this;
        // all subscriptions that may need to be unsubscribed
        final CompositeSubscription subscription = new CompositeSubscription(discardableAction);

        if (executor instanceof ScheduledExecutorService) {
            // we are a ScheduledExecutorService so can do proper scheduling
            ScheduledFuture<?> f = ((ScheduledExecutorService) executor).schedule(new Runnable() {
                @Override
                public void run() {
                    // when the delay has passed we now do the work on the actual scheduler
                    Subscription s = discardableAction.call(_scheduler);
                    // add the subscription to the CompositeSubscription so it is unsubscribed
                    subscription.add(s);
                }
            }, delayTime, unit);
            // add the ScheduledFuture as a subscription so we can cancel the scheduled action if an unsubscribe happens
            subscription.add(Subscriptions.create(f));
        } else {
            // we are not a ScheduledExecutorService so can't directly schedule
            if (delayTime == 0) {
                // no delay so put on the thread-pool right now
                Subscription s = schedule(state, action);
                // add the subscription to the CompositeSubscription so it is unsubscribed
                subscription.add(s);
            } else {
                // there is a delay and this isn't a ScheduledExecutorService so we'll use a system-wide ScheduledExecutorService
                // to handle the scheduling and once it's ready then execute on this Executor
                ScheduledFuture<?> f = GenericScheduledExecutorService.getInstance().schedule(new Runnable() {

                    @Override
                    public void run() {
                        // now execute on the real Executor (by using the other overload that schedules for immediate execution)
                        Subscription s = _scheduler.schedule(state, action);
                        // add the subscription to the CompositeSubscription so it is unsubscribed
                        subscription.add(s);
                    }
                }, delayTime, unit);
                // add the ScheduledFuture as a subscription so we can cancel the scheduled action if an unsubscribe happens
                subscription.add(Subscriptions.create(f));
            }
        }
        return subscription;
    }

    @Override
    public <T> Subscription schedule(T state, Func2<Scheduler, T, Subscription> action) {
        final DiscardableAction<T> discardableAction = new DiscardableAction<T>(state, action);
        final Scheduler _scheduler = this;
        // all subscriptions that may need to be unsubscribed
        final CompositeSubscription subscription = new CompositeSubscription(discardableAction);

        // work to be done on a thread
        Runnable r = new Runnable() {
            @Override
            public void run() {
                Subscription s = discardableAction.call(_scheduler);
                // add the subscription to the CompositeSubscription so it is unsubscribed
                subscription.add(s);
            }
        };

        // submit for immediate execution
        if (executor instanceof ExecutorService) {
            // we are an ExecutorService so get a Future back that supports unsubscribe
            Future<?> f = ((ExecutorService) executor).submit(r);
            // add the Future as a subscription so we can cancel the scheduled action if an unsubscribe happens
            subscription.add(Subscriptions.create(f));
        } else {
            // we are the lowest common denominator so can't unsubscribe once we execute
            executor.execute(r);
        }

        return subscription;

    }

}
