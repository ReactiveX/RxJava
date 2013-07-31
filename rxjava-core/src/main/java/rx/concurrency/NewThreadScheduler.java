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

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import rx.Scheduler;
import rx.Subscription;
import rx.operators.SafeObservableSubscription;
import rx.subscriptions.CompositeSubscription;
import rx.subscriptions.Subscriptions;
import rx.util.functions.Func2;

/**
 * Schedules work on a new thread.
 */
public class NewThreadScheduler extends Scheduler {
    private static final NewThreadScheduler INSTANCE = new NewThreadScheduler();

    public static NewThreadScheduler getInstance() {
        return INSTANCE;
    }

    @Override
    public <T> Subscription schedule(final T state, final Func2<Scheduler, T, Subscription> action) {
        final SafeObservableSubscription subscription = new SafeObservableSubscription();
        final Scheduler _scheduler = this;

        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                subscription.wrap(action.call(_scheduler, state));
            }
        }, "RxNewThreadScheduler");

        t.start();

        return subscription;
    }

    @Override
    public <T> Subscription schedule(final T state, final Func2<Scheduler, T, Subscription> action, long delay, TimeUnit unit) {
        // we will use the system scheduler since it doesn't make sense to launch a new Thread and then sleep
        // we will instead schedule the event then launch the thread after the delay has passed
        final Scheduler _scheduler = this;
        final CompositeSubscription subscription = new CompositeSubscription();
        ScheduledFuture<?> f = GenericScheduledExecutorService.getInstance().schedule(new Runnable() {

            @Override
            public void run() {
                if (!subscription.isUnsubscribed()) {
                    // when the delay has passed we now do the work on the actual scheduler
                    Subscription s = _scheduler.schedule(state, action);
                    // add the subscription to the CompositeSubscription so it is unsubscribed
                    subscription.add(s);
                }
            }
        }, delay, unit);

        // add the ScheduledFuture as a subscription so we can cancel the scheduled action if an unsubscribe happens
        subscription.add(Subscriptions.create(f));

        return subscription;
    }
}
