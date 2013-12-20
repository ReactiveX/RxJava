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

import java.util.concurrent.TimeUnit;
import rx.Scheduler;
import rx.Subscription;
import rx.subscriptions.CompositeSubscription;
import rx.subscriptions.ForwardSubscription;
import rx.subscriptions.SerialSubscription;
import rx.subscriptions.Subscriptions;
import rx.util.functions.Func1;
import rx.util.functions.Func2;

/**
 * Do not re-enter the main scheduler's schedule() method as it will
 * unnecessarily chain the subscriptions of every invocation.
 */
public final class ReentrantScheduler extends Scheduler {
    final ReentrantSchedulerHelper scheduler;
    final ForwardSubscription scheduleSub;
    final ForwardSubscription actionSub;
    final CompositeSubscription composite;
    
    public ReentrantScheduler(
            ReentrantSchedulerHelper scheduler,
            ForwardSubscription scheduleSub,
            ForwardSubscription actionSub,
            CompositeSubscription composite) {
        this.scheduler = scheduler;
        this.scheduleSub = scheduleSub;
        this.actionSub = actionSub;
        this.composite = composite;
    }
    
    @Override
    public <T> Subscription schedule(T state, Func2<? super Scheduler, ? super T, ? extends Subscription> action) {
        if (composite.isUnsubscribed()) {
            // don't bother scheduling a task which wouldn't run anyway
            return Subscriptions.empty();
        }
        Subscription before = actionSub.getSubscription();
        final DiscardableAction<T> discardableAction = new DiscardableAction<T>(state, action);
        
        actionSub.compareExchange(before, discardableAction);
        
        Runnable r = new Runnable() {
            @Override
            public void run() {
                Subscription sbefore = actionSub.getSubscription();
                Subscription s = discardableAction.call(ReentrantScheduler.this);
                actionSub.compareExchange(sbefore, s);
            }
        };
        
        Subscription sbefore = scheduleSub.getSubscription();
        Subscription s = scheduler.scheduleTask(r);
        scheduleSub.compareExchange(sbefore, s);
        
        return s;
    }
    
    @Override
    public <T> Subscription schedule(T state, Func2<? super Scheduler, ? super T, ? extends Subscription> action, long delayTime, TimeUnit unit) {
        if (composite.isUnsubscribed()) {
            // don't bother scheduling a task which wouldn't run anyway
            return Subscriptions.empty();
        }
        
        Subscription before = actionSub.getSubscription();
        final DiscardableAction<T> discardableAction = new DiscardableAction<T>(state, action);
        actionSub.compareExchange(before, discardableAction);
        
        Runnable r = new Runnable() {
            @Override
            public void run() {
                Subscription sbefore = actionSub.getSubscription();
                Subscription s = discardableAction.call(ReentrantScheduler.this);
                actionSub.compareExchange(sbefore, s);
            }
        };
        Subscription sbefore = scheduleSub.getSubscription();
        Subscription s = scheduler.scheduleTask(r, delayTime, unit);;
        scheduleSub.compareExchange(sbefore, s);
        
        return s;
    }
    
    @Override
    public <T> Subscription schedulePeriodically(T state, Func2<? super Scheduler, ? super T, ? extends Subscription> action, long initialDelay, long period, TimeUnit unit) {
        if (composite.isUnsubscribed()) {
            // don't bother scheduling a task which wouldn't run anyway
            return Subscriptions.empty();
        }
        
        Subscription before = actionSub.getSubscription();
        final PeriodicAction<T> periodicAction = new PeriodicAction<T>(state, action);
        actionSub.compareExchange(before, periodicAction);
        
        Runnable r = new Runnable() {
            @Override
            public void run() {
                Subscription sbefore = actionSub.getSubscription();
                Subscription s = periodicAction.call(ReentrantScheduler.this);
                actionSub.compareExchange(sbefore, s);
            }
        };
        Subscription sbefore = scheduleSub.getSubscription();
        Subscription s = scheduler.scheduleTask(r, initialDelay, period, unit);
        scheduleSub.compareExchange(sbefore, s);
        
        return s;
    }
    /**
     * An action that calls the underlying function in a periodic environment.
     * @param <T> the state value type
     */
    private static final class PeriodicAction<T> implements Subscription, Func1<Scheduler, Subscription> {
        final T state;
        final Func2<? super Scheduler, ? super T, ? extends Subscription> underlying;
        final SerialSubscription ssub;
        
        public PeriodicAction(T state, Func2<? super Scheduler, ? super T, ? extends Subscription> underlying) {
            this.state = state;
            this.underlying = underlying;
            this.ssub = new SerialSubscription();
        }
        
        @Override
        public Subscription call(Scheduler scheduler) {
            if (!ssub.isUnsubscribed()) {
                Subscription s = underlying.call(scheduler, state);
                ssub.setSubscription(s);
                return ssub;
            }
            return Subscriptions.empty();
        }
        
        @Override
        public void unsubscribe() {
            ssub.unsubscribe();
        }
    }
    /**
     * Simple scheduler API used by the ReentrantScheduler to
     * communicate with the actual scheduler implementation.
     */
    public interface ReentrantSchedulerHelper {
        /**
         * Schedule a task to be run immediately and update the subscription
         * describing the schedule.
         * @param r the task to run immediately
         * @return the subscription to cancel the schedule
         */
        Subscription scheduleTask(Runnable r);
        
        /**
         * Schedule a task to be run after the delay time and update the subscription
         * describing the schedule.
         * @param r the task to schedule
         * @param delayTime the time to delay the execution
         * @param unit the time unit
         * @return the subscription to cancel the schedule
         */
        Subscription scheduleTask(Runnable r, long delayTime, TimeUnit unit);
        
        /**
         * Schedule a task to be run after the delay time and after
         * each period, then update the subscription describing the schedule.
         * @param r the task to schedule
         * @param initialDelay the initial delay of the schedule
         * @param period the between period of the schedule
         * @param unit the time unit
         * @return the subscription to cancel the schedule
         */
        Subscription scheduleTask(Runnable r, long initialDelay, long period, TimeUnit unit);
    }
}
