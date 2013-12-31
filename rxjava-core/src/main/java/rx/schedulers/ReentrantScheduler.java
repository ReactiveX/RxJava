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
import rx.subscriptions.IncrementalSubscription;
import rx.subscriptions.SerialSubscription;
import rx.subscriptions.Subscriptions;
import rx.util.functions.Func1;
import rx.util.functions.Func2;

/**
 * Do not re-enter the main scheduler's schedule() method as it will
 * unnecessarily chain the subscriptions of every invocation.
 */
public final class ReentrantScheduler extends Scheduler {
    final Scheduler parent;
    final IncrementalSubscription scheduleSub;
    final IncrementalSubscription actionSub;
    final CompositeSubscription composite;
    
    public ReentrantScheduler(
            Scheduler parent,
            IncrementalSubscription scheduleSub,
            IncrementalSubscription actionSub,
            CompositeSubscription composite) {
        this.parent = parent;
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
        long index = actionSub.nextIndex();
        final DiscardableAction<T> discardableAction = new DiscardableAction<T>(state, action);
        
        actionSub.compareExchange(index, discardableAction, false);
        
        Runnable r = new RunTask(discardableAction);
        
        long sindex = scheduleSub.nextIndex();
        Subscription s = parent.scheduleRunnable(r);
        scheduleSub.compareExchange(sindex, s, false);
        
        return s;
    }
    
    @Override
    public <T> Subscription schedule(T state, Func2<? super Scheduler, ? super T, ? extends Subscription> action, long delayTime, TimeUnit unit) {
        if (composite.isUnsubscribed()) {
            // don't bother scheduling a task which wouldn't run anyway
            return Subscriptions.empty();
        }
        
        long index = actionSub.nextIndex();
        final DiscardableAction<T> discardableAction = new DiscardableAction<T>(state, action);
        actionSub.compareExchange(index, discardableAction, false);
        
        Runnable r = new RunTask(discardableAction);
        
        long sindex = scheduleSub.nextIndex();
        Subscription s = parent.scheduleRunnable(r, delayTime, unit);
        scheduleSub.compareExchange(sindex, s, false);
        
        return s;
    }
    
    @Override
    public <T> Subscription schedulePeriodically(T state, Func2<? super Scheduler, ? super T, ? extends Subscription> action, long initialDelay, long period, TimeUnit unit) {
        if (composite.isUnsubscribed()) {
            // don't bother scheduling a task which wouldn't run anyway
            return Subscriptions.empty();
        }

        long index = actionSub.nextIndex();
        final PeriodicAction<T> periodicAction = new PeriodicAction<T>(state, action);
        actionSub.compareExchange(index, periodicAction, false);
        
        Runnable r = new RunTask(periodicAction);
        
        long sindex = scheduleSub.nextIndex();
        Subscription s = parent.scheduleRunnable(r, initialDelay, period, unit);
        scheduleSub.compareExchange(sindex, s, false);
        
        return s;
    }
    /** The task runner. */
    private final class RunTask implements Runnable {
        final Func1<Scheduler, Subscription> action;

        public RunTask(Func1<Scheduler, Subscription> action) {
            this.action = action;
        }

        @Override
        public void run() {
            long index = actionSub.nextIndex();
            Subscription s = action.call(ReentrantScheduler.this);
            actionSub.compareExchange(index, s, false);
        }
        
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
}
