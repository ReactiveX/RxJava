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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import rx.Scheduler;
import rx.Subscription;
import rx.subscriptions.CompositeSubscription;
import rx.subscriptions.Subscriptions;
import rx.util.functions.Func2;

/**
 * Schedules work on a new thread.
 */
public class NewThreadScheduler extends Scheduler {

    private final static NewThreadScheduler INSTANCE = new NewThreadScheduler();
    private final static AtomicLong count = new AtomicLong();

    public static NewThreadScheduler getInstance() {
        return INSTANCE;
    }

    private NewThreadScheduler() {

    }

    private static class EventLoopScheduler extends Scheduler {
        private final ExecutorService executor;

        private EventLoopScheduler() {
            executor = Executors.newFixedThreadPool(1, new ThreadFactory() {

                @Override
                public Thread newThread(Runnable r) {
                    return new Thread(r, "RxNewThreadScheduler-" + count.incrementAndGet());
                }
            });
        }

        @Override
        public <T> Subscription schedule(T state, Func2<? super Scheduler, ? super T, ? extends Subscription> action) {
            final DiscardableAction<T> discardableAction = new DiscardableAction<T>(state, action);
            // all subscriptions that may need to be unsubscribed
            final CompositeSubscription subscription = new CompositeSubscription(discardableAction);
            
            final Scheduler _scheduler = this;
            subscription.add(Subscriptions.from(executor.submit(new Runnable() {

                @Override
                public void run() {
                    Subscription s = discardableAction.call(_scheduler);
                    subscription.add(s);
                }
            })));
            
            return subscription;
        }

        @Override
        public <T> Subscription schedule(final T state, final Func2<? super Scheduler, ? super T, ? extends Subscription> action, final long delayTime, final TimeUnit unit) {
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
            }, delayTime, unit);

            // add the ScheduledFuture as a subscription so we can cancel the scheduled action if an unsubscribe happens
            subscription.add(Subscriptions.from(f));

            return subscription;
        }

    }

    @Override
    public <T> Subscription schedule(T state, Func2<? super Scheduler, ? super T, ? extends Subscription> action) {
        EventLoopScheduler s = new EventLoopScheduler();
        return s.schedule(state, action);
    }

    @Override
    public <T> Subscription schedule(final T state, final Func2<? super Scheduler, ? super T, ? extends Subscription> action, long delay, TimeUnit unit) {
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
        subscription.add(Subscriptions.from(f));

        return subscription;
    }
}
