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

import rx.Scheduler;
import rx.Subscription;
import rx.functions.Action0;
import rx.internal.schedulers.NewThreadWorker;
import rx.internal.schedulers.ScheduledAction;
import rx.internal.util.RxThreadFactory;
import rx.subscriptions.CompositeSubscription;
import rx.subscriptions.Subscriptions;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

/* package */final class CachedThreadScheduler extends Scheduler {
    private static final String WORKER_THREAD_NAME_PREFIX = "RxCachedThreadScheduler-";
    private static final RxThreadFactory WORKER_THREAD_FACTORY =
            new RxThreadFactory(WORKER_THREAD_NAME_PREFIX);

    private static final String EVICTOR_THREAD_NAME_PREFIX = "RxCachedWorkerPoolEvictor-";
    private static final RxThreadFactory EVICTOR_THREAD_FACTORY =
            new RxThreadFactory(EVICTOR_THREAD_NAME_PREFIX);

    private static final class CachedWorkerPool {
        private final long keepAliveTime;
        private final ConcurrentLinkedQueue<ThreadWorker> expiringWorkerQueue;
        private final ScheduledExecutorService evictExpiredWorkerExecutor;

        CachedWorkerPool(long keepAliveTime, TimeUnit unit) {
            this.keepAliveTime = unit.toNanos(keepAliveTime);
            this.expiringWorkerQueue = new ConcurrentLinkedQueue<ThreadWorker>();

            evictExpiredWorkerExecutor = Executors.newScheduledThreadPool(1, EVICTOR_THREAD_FACTORY);
            evictExpiredWorkerExecutor.scheduleWithFixedDelay(
                    new Runnable() {
                        @Override
                        public void run() {
                            evictExpiredWorkers();
                        }
                    }, this.keepAliveTime, this.keepAliveTime, TimeUnit.NANOSECONDS
            );
        }

        private static CachedWorkerPool INSTANCE = new CachedWorkerPool(
                60L, TimeUnit.SECONDS
        );

        ThreadWorker get() {
            while (!expiringWorkerQueue.isEmpty()) {
                ThreadWorker threadWorker = expiringWorkerQueue.poll();
                if (threadWorker != null) {
                    return threadWorker;
                }
            }

            // No cached worker found, so create a new one.
            return new ThreadWorker(WORKER_THREAD_FACTORY);
        }

        void release(ThreadWorker threadWorker) {
            // Refresh expire time before putting worker back in pool
            threadWorker.setExpirationTime(now() + keepAliveTime);

            expiringWorkerQueue.offer(threadWorker);
        }

        void evictExpiredWorkers() {
            if (!expiringWorkerQueue.isEmpty()) {
                long currentTimestamp = now();

                for (ThreadWorker threadWorker : expiringWorkerQueue) {
                    if (threadWorker.getExpirationTime() <= currentTimestamp) {
                        if (expiringWorkerQueue.remove(threadWorker)) {
                            threadWorker.unsubscribe();
                        }
                    } else {
                        // Queue is ordered with the worker that will expire first in the beginning, so when we
                        // find a non-expired worker we can stop evicting.
                        break;
                    }
                }
            }
        }

        long now() {
            return System.nanoTime();
        }
    }

    @Override
    public Worker createWorker() {
        return new EventLoopWorker(CachedWorkerPool.INSTANCE.get());
    }

    private static final class EventLoopWorker extends Scheduler.Worker {
        private final CompositeSubscription innerSubscription = new CompositeSubscription();
        private final ThreadWorker threadWorker;
        volatile int once;
        static final AtomicIntegerFieldUpdater<EventLoopWorker> ONCE_UPDATER
                = AtomicIntegerFieldUpdater.newUpdater(EventLoopWorker.class, "once");

        EventLoopWorker(ThreadWorker threadWorker) {
            this.threadWorker = threadWorker;
        }

        @Override
        public void unsubscribe() {
            if (ONCE_UPDATER.compareAndSet(this, 0, 1)) {
                // unsubscribe should be idempotent, so only do this once
                CachedWorkerPool.INSTANCE.release(threadWorker);
            }
            innerSubscription.unsubscribe();
        }

        @Override
        public boolean isUnsubscribed() {
            return innerSubscription.isUnsubscribed();
        }

        @Override
        public Subscription schedule(Action0 action) {
            return schedule(action, 0, null);
        }

        @Override
        public Subscription schedule(Action0 action, long delayTime, TimeUnit unit) {
            if (innerSubscription.isUnsubscribed()) {
                // don't schedule, we are unsubscribed
                return Subscriptions.unsubscribed();
            }

            ScheduledAction s = threadWorker.scheduleActual(action, delayTime, unit);
            innerSubscription.add(s);
            s.addParent(innerSubscription);
            return s;
        }
    }

    private static final class ThreadWorker extends NewThreadWorker {
        private long expirationTime;

        ThreadWorker(ThreadFactory threadFactory) {
            super(threadFactory);
            this.expirationTime = 0L;
        }

        public long getExpirationTime() {
            return expirationTime;
        }

        public void setExpirationTime(long expirationTime) {
            this.expirationTime = expirationTime;
        }
    }
}
