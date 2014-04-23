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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import rx.Scheduler;
import rx.Subscription;
import rx.functions.Action0;
import rx.subscriptions.CompositeSubscription;
import rx.subscriptions.Subscriptions;

/**
 * Schedules work on a new thread.
 */
public class NewThreadScheduler extends Scheduler {

    private final static NewThreadScheduler INSTANCE = new NewThreadScheduler();
    private final static AtomicLong count = new AtomicLong();
    private final static ThreadFactory THREAD_FACTORY = new ThreadFactory() {

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, "RxNewThreadScheduler-" + count.incrementAndGet());
            t.setDaemon(true);
            return t;
        }
    };

    /* package */static NewThreadScheduler instance() {
        return INSTANCE;
    }

    private NewThreadScheduler() {

    }

    @Override
    public Worker createWorker() {
        return new EventLoopScheduler(THREAD_FACTORY);
    }

    /* package */static class EventLoopScheduler extends Scheduler.Worker implements Subscription {
        private final CompositeSubscription innerSubscription = new CompositeSubscription();
        private final ExecutorService executor;

        /* package */EventLoopScheduler(ThreadFactory threadFactory) {
            executor = Executors.newSingleThreadExecutor(threadFactory);
        }

        @Override
        public Subscription schedule(final Action0 action) {
            return schedule(action, null);
        }

        /* package */Subscription schedule(final Action0 action, final OnActionComplete onComplete) {
            if (innerSubscription.isUnsubscribed()) {
                // don't schedule, we are unsubscribed
                return Subscriptions.empty();
            }

            final AtomicReference<Subscription> sf = new AtomicReference<Subscription>();
            Subscription s = Subscriptions.from(executor.submit(new Runnable() {

                @Override
                public void run() {
                    try {
                        if (innerSubscription.isUnsubscribed()) {
                            return;
                        }
                        action.call();
                    } finally {
                        // remove the subscription now that we're completed
                        Subscription s = sf.get();
                        if (s != null) {
                            innerSubscription.remove(s);
                        }
                        if (onComplete != null) {
                            onComplete.complete(s);
                        }
                    }
                }
            }));

            sf.set(s);
            innerSubscription.add(s);
            return s;
        }

        @Override
        public Subscription schedule(final Action0 action, long delayTime, TimeUnit unit) {
            return schedule(action, delayTime, unit, null);
        }

        /* package */Subscription schedule(final Action0 action, long delayTime, TimeUnit unit, final OnActionComplete onComplete) {
            final AtomicReference<Subscription> sf = new AtomicReference<Subscription>();
            // we will use the system scheduler since it doesn't make sense to launch a new Thread and then sleep
            // we will instead schedule the event then launch the thread after the delay has passed
            ScheduledFuture<?> f = GenericScheduledExecutorService.getInstance().schedule(new Runnable() {

                @Override
                public void run() {
                    try {
                        if (innerSubscription.isUnsubscribed()) {
                            return;
                        }
                        // now that the delay is past schedule the work to be done for real on the UI thread
                        schedule(action);
                    } finally {
                        // remove the subscription now that we're completed
                        Subscription s = sf.get();
                        if (s != null) {
                            innerSubscription.remove(s);
                        }
                        if (onComplete != null) {
                            onComplete.complete(s);
                        }
                    }
                }
            }, delayTime, unit);

            // add the ScheduledFuture as a subscription so we can cancel the scheduled action if an unsubscribe happens
            Subscription s = Subscriptions.from(f);
            sf.set(s);
            innerSubscription.add(s);
            return s;
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

    /* package */static interface OnActionComplete {

        public void complete(Subscription s);

    }

}
