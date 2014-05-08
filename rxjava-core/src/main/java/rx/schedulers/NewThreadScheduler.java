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

import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicLong;

import rx.Scheduler;
import rx.Subscription;
import rx.functions.Action0;
import rx.subscriptions.CompositeSubscription;
import rx.subscriptions.Subscriptions;

/**
 * Schedules work on a new thread.
 */
/* package */ class NewThreadScheduler extends Scheduler {

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
        return new NewThreadWorker(THREAD_FACTORY);
    }

    /* package */static class NewThreadWorker extends Scheduler.Worker implements Subscription {
        private final ScheduledExecutorService executor;
        private volatile boolean unsubscribed;

        /* package */NewThreadWorker(ThreadFactory threadFactory) {
            executor = Executors.newScheduledThreadPool(1, threadFactory);
        }

        @Override
        public Subscription schedule(final Action0 action) {
            return schedule(action, 0, null);
        }

        @Override
        public Subscription schedule(final Action0 action, long delayTime, TimeUnit unit) {
            if (unsubscribed) {
                return Subscriptions.empty();
            }
            return scheduleActual(action, delayTime, unit);
        }

        /* package */ScheduledAction scheduleActual(final Action0 action, long delayTime, TimeUnit unit) {
            ScheduledAction run = new ScheduledAction(action);
            Future<?> f;
            if (delayTime <= 0) {
                f = executor.submit(run);
            } else {
                f = executor.schedule(run, delayTime, unit);
            }
            run.add(Subscriptions.from(f));
            
            return run;
        }
        /** Remove a child subscription from a composite when unsubscribing. */
        private static final class Remover implements Subscription {
            final Subscription s;
            final CompositeSubscription parent;
            volatile int unsubscribed;
            private static final AtomicIntegerFieldUpdater<Remover> UNSUBSCRIBED_UPDATER =
                    AtomicIntegerFieldUpdater.newUpdater(Remover.class, "unsubscribed");
            
            public Remover(Subscription s, CompositeSubscription parent) {
                this.s = s;
                this.parent = parent;
            }
            
            @Override
            public boolean isUnsubscribed() {
                return unsubscribed == 1;
            }
            
            @Override
            public void unsubscribe() {
                if (UNSUBSCRIBED_UPDATER.compareAndSet(this, 0, 1)) {
                    parent.remove(s);
                }
            }
            
        }
        /** 
         * A runnable that executes an Action0 and can be cancelled
         * The analogue is the Subscriber in respect of an Observer.
         */
        public static final class ScheduledAction implements Runnable, Subscription {
            final CompositeSubscription cancel;
            final Action0 action;
            volatile int unsubscribed;
            private static final AtomicIntegerFieldUpdater<ScheduledAction> UNSUBSCRIBED_UPDATER =
                    AtomicIntegerFieldUpdater.newUpdater(ScheduledAction.class, "unsubscribed");

            public ScheduledAction(Action0 action) {
                this.action = action;
                this.cancel = new CompositeSubscription();
            }

            @Override
            public void run() {
                try {
                    action.call();
                } finally {
                    unsubscribe();
                }
            }

            @Override
            public boolean isUnsubscribed() {
                return unsubscribed == 1;
            }
            
            @Override
            public void unsubscribe() {
                if (UNSUBSCRIBED_UPDATER.compareAndSet(this, 0, 1)) {
                    cancel.unsubscribe();
                }
            }
            public void add(Subscription s) {
                cancel.add(s);
            }
            /** 
             * Adds a parent to this ScheduledAction so when it is 
             * cancelled or terminates, it can remove itself from this parent.
             * @param parent 
             */
            public void addParent(CompositeSubscription parent) {
                add(new Remover(this, parent));
            } 
        }

        @Override
        public void unsubscribe() {
            unsubscribed = true;
            executor.shutdownNow();
        }

        @Override
        public boolean isUnsubscribed() {
            return unsubscribed;
        }

    }
}
