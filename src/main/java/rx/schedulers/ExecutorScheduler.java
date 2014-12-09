/**
 * Copyright 2014 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package rx.schedulers;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import rx.Scheduler;
import rx.Subscription;
import rx.functions.Action0;
import rx.plugins.RxJavaPlugins;
import rx.subscriptions.CompositeSubscription;
import rx.subscriptions.MultipleAssignmentSubscription;
import rx.subscriptions.Subscriptions;

/**
 * Scheduler that wraps an Executor instance and establishes the Scheduler contract upon it.
 * <p>
 * Note that thread-hopping is unavoidable with this kind of Scheduler as we don't know about the underlying
 * threading behavior of the executor.
 */
/* public */final class ExecutorScheduler extends Scheduler {
    final Executor executor;
    public ExecutorScheduler(Executor executor) {
        this.executor = executor;
    }

    /**
     * @warn javadoc missing
     * @return
     */
    @Override
    public Worker createWorker() {
        return new ExecutorSchedulerWorker(executor);
    }

    /** Worker that schedules tasks on the executor indirectly through a trampoline mechanism. */
    static final class ExecutorSchedulerWorker extends Scheduler.Worker implements Runnable {
        final Executor executor;
        // TODO: use a better performing structure for task tracking
        final CompositeSubscription tasks;
        // TODO: use MpscLinkedQueue once available
        final ConcurrentLinkedQueue<ExecutorAction> queue; 
        final AtomicInteger wip;
        
        public ExecutorSchedulerWorker(Executor executor) {
            this.executor = executor;
            this.queue = new ConcurrentLinkedQueue<ExecutorAction>();
            this.wip = new AtomicInteger();
            this.tasks = new CompositeSubscription();
        }

        @Override
        public Subscription schedule(Action0 action) {
            if (isUnsubscribed()) {
                return Subscriptions.unsubscribed();
            }
            ExecutorAction ea = new ExecutorAction(action, tasks);
            tasks.add(ea);
            queue.offer(ea);
            if (wip.getAndIncrement() == 0) {
                try {
                    executor.execute(this);
                } catch (RejectedExecutionException t) {
                    // cleanup if rejected
                    tasks.remove(ea);
                    wip.decrementAndGet();
                    // report the error to the plugin
                    RxJavaPlugins.getInstance().getErrorHandler().handleError(t);
                    // throw it to the caller
                    throw t;
                }
            }
            
            return ea;
        }

        @Override
        public void run() {
            do {
                queue.poll().run();
            } while (wip.decrementAndGet() > 0);
        }
        
        @Override
        public Subscription schedule(final Action0 action, long delayTime, TimeUnit unit) {
            if (delayTime <= 0) {
                return schedule(action);
            }
            if (isUnsubscribed()) {
                return Subscriptions.unsubscribed();
            }
            ScheduledExecutorService service;
            if (executor instanceof ScheduledExecutorService) {
                service = (ScheduledExecutorService)executor;
            } else {
                service = GenericScheduledExecutorService.getInstance();
            }
            
            final MultipleAssignmentSubscription mas = new MultipleAssignmentSubscription();
            // tasks.add(mas); // Needs a removal without unsubscription
            
            try {
                Future<?> f = service.schedule(new Runnable() {
                    @Override
                    public void run() {
                        if (mas.isUnsubscribed()) {
                            return;
                        }
                        mas.set(schedule(action));
                        // tasks.delete(mas); // Needs a removal without unsubscription
                    }
                }, delayTime, unit);
                mas.set(Subscriptions.from(f));
            } catch (RejectedExecutionException t) {
                // report the rejection to plugins
                RxJavaPlugins.getInstance().getErrorHandler().handleError(t);
                throw t;
            }
            
            return mas;
        }

        @Override
        public boolean isUnsubscribed() {
            return tasks.isUnsubscribed();
        }

        @Override
        public void unsubscribe() {
            tasks.unsubscribe();
        }
        
    }

    /** Runs the actual action and maintains an unsubscription state. */
    static final class ExecutorAction implements Runnable, Subscription {
        final Action0 actual;
        final CompositeSubscription parent;
        volatile int unsubscribed;
        static final AtomicIntegerFieldUpdater<ExecutorAction> UNSUBSCRIBED_UPDATER
                = AtomicIntegerFieldUpdater.newUpdater(ExecutorAction.class, "unsubscribed");

        public ExecutorAction(Action0 actual, CompositeSubscription parent) {
            this.actual = actual;
            this.parent = parent;
        }

        @Override
        public void run() {
            if (isUnsubscribed()) {
                return;
            }
            try {
                actual.call();
            } catch (Throwable t) {
                RxJavaPlugins.getInstance().getErrorHandler().handleError(t);
                Thread thread = Thread.currentThread();
                thread.getUncaughtExceptionHandler().uncaughtException(thread, t);
            } finally {
                unsubscribe();
            }
        }
        @Override
        public boolean isUnsubscribed() {
            return unsubscribed != 0;
        }

        @Override
        public void unsubscribe() {
            if (UNSUBSCRIBED_UPDATER.compareAndSet(this, 0, 1)) {
                parent.remove(this);
            }
        }
        
    }
}
