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
import rx.subscriptions.CompositeSubscription;
import rx.subscriptions.ForwardSubscription;
import rx.subscriptions.Subscriptions;
import rx.util.functions.Func2;

/**
 * A {@link Scheduler} implementation that uses an {@link Executor} or {@link ScheduledExecutorService} implementation.
 * <p>
 * Note that if an {@link Executor} implementation is used instead of {@link ScheduledExecutorService} then a system-wide Timer will be used to handle delayed events.
 */
public class ExecutorScheduler extends Scheduler implements ReentrantSchedulerHelper {
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
            CompositeSubscription subscription = new CompositeSubscription();
            final ForwardSubscription scheduleSub = new ForwardSubscription();
            final ForwardSubscription actionSub = new ForwardSubscription();
            subscription.add(scheduleSub);
            subscription.add(actionSub);

            final Scheduler _scheduler = new ReentrantScheduler(this, scheduleSub, actionSub, subscription);

            _scheduler.schedulePeriodically(state, action, initialDelay, period, unit);
            
            return subscription;

        } else {
            return super.schedulePeriodically(state, action, initialDelay, period, unit);
        }
    }

    @Override
    public <T> Subscription schedule(final T state, final Func2<? super Scheduler, ? super T, ? extends Subscription> action, long delayTime, TimeUnit unit) {
        CompositeSubscription subscription = new CompositeSubscription();
        final ForwardSubscription scheduleSub = new ForwardSubscription();
        final ForwardSubscription actionSub = new ForwardSubscription();
        subscription.add(scheduleSub);
        subscription.add(actionSub);
        
        final Scheduler _scheduler = new ReentrantScheduler(this, scheduleSub, actionSub, subscription);

        _scheduler.schedule(state, action, delayTime, unit);

        return subscription;
    }

    @Override
    public <T> Subscription schedule(T state, Func2<? super Scheduler, ? super T, ? extends Subscription> action) {
        // all subscriptions that may need to be unsubscribed
        CompositeSubscription subscription = new CompositeSubscription();
        final ForwardSubscription scheduleSub = new ForwardSubscription();
        final ForwardSubscription actionSub = new ForwardSubscription();
        subscription.add(scheduleSub);
        subscription.add(actionSub);
        
        final Scheduler _scheduler = new ReentrantScheduler(this, scheduleSub, actionSub, subscription);

        _scheduler.schedule(state, action);

        return subscription;
    }
    
    @Override
    public void scheduleTask(Runnable r, ForwardSubscription out, long delayTime, TimeUnit unit) {
        Subscription before = out.getSubscription();
        if (executor instanceof ScheduledExecutorService) {
            // we are a ScheduledExecutorService so can do proper scheduling
            ScheduledFuture<?> f = ((ScheduledExecutorService) executor).schedule(r, delayTime, unit);
            // add the ScheduledFuture as a subscription so we can cancel the scheduled action if an unsubscribe happens
            out.compareExchange(before, Subscriptions.from(f));
        } else {
            // we are not a ScheduledExecutorService so can't directly schedule
            if (delayTime == 0) {
                // no delay so put on the thread-pool right now
                scheduleTask(r, out);
            } else {
                // there is a delay and this isn't a ScheduledExecutorService so we'll use a system-wide ScheduledExecutorService
                // to handle the scheduling and once it's ready then execute on this Executor
                ScheduledFuture<?> f = GenericScheduledExecutorService.getInstance().schedule(r, delayTime, unit);
                // add the ScheduledFuture as a subscription so we can cancel the scheduled action if an unsubscribe happens
                out.compareExchange(before, Subscriptions.from(f));
            }
        }
    }
    
    @Override
    public void scheduleTask(Runnable r, ForwardSubscription out) {
        Subscription before = out.getSubscription();
        // submit for immediate execution
        if (executor instanceof ExecutorService) {
            // we are an ExecutorService so get a Future back that supports unsubscribe
            Future<?> f = ((ExecutorService) executor).submit(r);
            // add the Future as a subscription so we can cancel the scheduled action if an unsubscribe happens
            out.compareExchange(before, Subscriptions.from(f));
        } else {
            // we are the lowest common denominator so can't unsubscribe once we execute
            executor.execute(r);
            out.compareExchange(before, Subscriptions.empty());
        }
    }

    @Override
    public void scheduleTask(Runnable r, ForwardSubscription out, long initialDelay, long period, TimeUnit unit) {
        Subscription before = out.getSubscription();
        ScheduledFuture<?> f = ((ScheduledExecutorService) executor).scheduleAtFixedRate(r, initialDelay, period, unit);

        out.compareExchange(before, Subscriptions.from(f));
    }
    
}
