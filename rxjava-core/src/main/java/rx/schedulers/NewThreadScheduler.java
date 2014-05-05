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

import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import rx.Scheduler;
import rx.Subscription;
import rx.functions.Action0;
import rx.subscriptions.CompositeSubscription;
import rx.subscriptions.MultipleAssignmentSubscription;
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
        return new NewThreadWorker(THREAD_FACTORY);
    }

    /* package */static class NewThreadWorker extends Scheduler.Worker implements Subscription {
        private final CompositeSubscription innerSubscription = new CompositeSubscription();
        private final ScheduledExecutorService executor;

        /* package */NewThreadWorker(ThreadFactory threadFactory) {
            executor = Executors.newScheduledThreadPool(1, threadFactory);
        }

        @Override
        public Subscription schedule(final Action0 action) {
            return schedule(action, null);
        }

        /* package */Subscription schedule(final Action0 action, final OnActionComplete onComplete) {
            return schedule(action, 0, null, onComplete);
        }

        @Override
        public Subscription schedule(final Action0 action, long delayTime, TimeUnit unit) {
            return schedule(action, delayTime, unit, null);
        }

        /* package */Subscription schedule(final Action0 action, long delayTime, TimeUnit unit, final OnActionComplete onComplete) {
            if (innerSubscription.isUnsubscribed()) {
                return Subscriptions.empty();
            }
            MultipleAssignmentSubscription mas = new MultipleAssignmentSubscription();
            innerSubscription.add(mas);
            ActionRunner run = new ActionRunner(action, mas, innerSubscription, onComplete);
            Future<?> f;
            if (delayTime <= 0) {
                f = executor.submit(run);
            } else {
                f = executor.schedule(run, delayTime, unit);
            }
            mas.set(Subscriptions.from(f));
            
            return new ActionCancel(mas, innerSubscription, onComplete);
        }

        /** Removes a subscription token from a composite. */
        static final class ActionCancel implements Subscription {
            final Subscription token;
            final CompositeSubscription parent;
            final OnActionComplete onComplete;
            final AtomicBoolean once;

            public ActionCancel(Subscription token, CompositeSubscription parent, OnActionComplete onComplete) {
                this.token = token;
                this.parent = parent;
                this.onComplete = onComplete;
                this.once = new AtomicBoolean();
            }

            @Override
            public void unsubscribe() {
                if (once.compareAndSet(false, true)) {
                    parent.remove(token);
                    if (onComplete != null) {
                        onComplete.complete(token);
                    }
                }
            }

            @Override
            public boolean isUnsubscribed() {
                return token.isUnsubscribed();
            }
            
        }
        /** Runs an action and removes the subscription token from the composite. */
        private static final class ActionRunner implements Runnable {
            private final Action0 action;
            private final Subscription token;
            private final CompositeSubscription parent;
            private final OnActionComplete onCompleted;

            public ActionRunner(Action0 action, Subscription token, CompositeSubscription parent,
                    OnActionComplete onCompleted) {
                this.action = action;
                this.token = token;
                this.parent = parent;
                this.onCompleted = onCompleted;
            }
            
            @Override
            public void run() {
                if (token.isUnsubscribed()) {
                    return;
                }
                try {
                    action.call();
                } finally {
                    parent.remove(token);
                    if (onCompleted != null) {
                        onCompleted.complete(token);
                    }
                }
            }
            
        }

        @Override
        public void unsubscribe() {
            executor.shutdown();
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
