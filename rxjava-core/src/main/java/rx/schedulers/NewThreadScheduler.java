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
import rx.functions.Action1;
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

    /**
     * @deprecated Use Schedulers.newThread();
     * @return
     */
    @Deprecated
    public static NewThreadScheduler getInstance() {
        return INSTANCE;
    }

    /* package */static NewThreadScheduler instance() {
        return INSTANCE;
    }

    private NewThreadScheduler() {

    }

    @Override
    public Subscription schedule(Action1<Scheduler.Inner> action) {
        EventLoopScheduler innerScheduler = new EventLoopScheduler();
        innerScheduler.schedule(action);
        return innerScheduler.innerSubscription;
    }

    @Override
    public Subscription schedule(Action1<Inner> action, long delayTime, TimeUnit unit) {
        EventLoopScheduler innerScheduler = new EventLoopScheduler();
        innerScheduler.schedule(action, delayTime, unit);
        return innerScheduler.innerSubscription;
    }

    private class EventLoopScheduler extends Scheduler.Inner implements Subscription {
        private final CompositeSubscription innerSubscription = new CompositeSubscription();
        private final ExecutorService executor;
        private final Inner _inner = this;

        private EventLoopScheduler() {
            executor = Executors.newSingleThreadExecutor(THREAD_FACTORY);
        }

        @Override
        public void schedule(final Action1<Scheduler.Inner> action) {
            if (innerSubscription.isUnsubscribed()) {
                // don't schedule, we are unsubscribed
                return;
            }

            final AtomicReference<Subscription> sf = new AtomicReference<Subscription>();
            Subscription s = Subscriptions.from(executor.submit(new Runnable() {

                @Override
                public void run() {
                    try {
                        if (innerSubscription.isUnsubscribed()) {
                            return;
                        }
                        action.call(_inner);
                    } finally {
                        // remove the subscription now that we're completed
                        Subscription s = sf.get();
                        if (s != null) {
                            innerSubscription.remove(s);
                        }
                    }
                }
            }));

            sf.set(s);
            innerSubscription.add(s);
        }

        @Override
        public void schedule(final Action1<Inner> action, long delayTime, TimeUnit unit) {
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
                    }
                }
            }, delayTime, unit);

            // add the ScheduledFuture as a subscription so we can cancel the scheduled action if an unsubscribe happens
            Subscription s = Subscriptions.from(f);
            sf.set(s);
            innerSubscription.add(s);
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
