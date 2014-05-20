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
import rx.subscriptions.CompositeSubscription;
import rx.subscriptions.Subscriptions;

import java.util.Iterator;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/* package */class CachedThreadScheduler extends Scheduler {
    private static final class CachedWorkerPool {
        final ThreadFactory factory = new ThreadFactory() {
            final AtomicInteger counter = new AtomicInteger();

            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "RxCachedThreadScheduler-" + counter.incrementAndGet());
                t.setDaemon(true);
                return t;
            }
        };

        private final long keepAliveTime;
        private final ConcurrentLinkedQueue<PoolWorker> expiringQueue;
        private final ScheduledExecutorService evictExpiredWorkerExecutor;

        CachedWorkerPool(long keepAliveTime, TimeUnit unit) {
            this.keepAliveTime = unit.toNanos(keepAliveTime);
            this.expiringQueue = new ConcurrentLinkedQueue<PoolWorker>();

            evictExpiredWorkerExecutor = Executors.newScheduledThreadPool(1, new ThreadFactory() {
                final AtomicInteger counter = new AtomicInteger();

                @Override
                public Thread newThread(Runnable r) {
                    Thread t = new Thread(r, "RxCachedWorkerPoolEvictor-" + counter.incrementAndGet());
                    t.setDaemon(true);
                    return t;
                }
            });
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

        PoolWorker get() {
            while (!expiringQueue.isEmpty()) {
                PoolWorker poolWorker = expiringQueue.poll();
                if (poolWorker != null) {
                    return poolWorker;
                }
            }

            // No cached worker found, so create a new one.
            return new PoolWorker(factory);
        }

        void release(PoolWorker poolWorker) {
            // Refresh expire time before putting worker back in pool
            poolWorker.setExpirationTime(now() + keepAliveTime);

            expiringQueue.add(poolWorker);
        }

        void evictExpiredWorkers() {
            if (!expiringQueue.isEmpty()) {
                long currentTimestamp = now();

                Iterator<PoolWorker> poolWorkerIterator = expiringQueue.iterator();
                while (poolWorkerIterator.hasNext()) {
                    PoolWorker poolWorker = poolWorkerIterator.next();
                    if (poolWorker.getExpirationTime() <= currentTimestamp) {
                        poolWorkerIterator.remove();
                        poolWorker.unsubscribe();
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

    private static class EventLoopWorker extends Scheduler.Worker {
        private final CompositeSubscription innerSubscription = new CompositeSubscription();
        private final PoolWorker poolWorker;
        private final AtomicBoolean releasePoolWorkerOnce = new AtomicBoolean(false);

        EventLoopWorker(PoolWorker poolWorker) {
            this.poolWorker = poolWorker;
        }

        @Override
        public void unsubscribe() {
            if (releasePoolWorkerOnce.compareAndSet(false, true)) {
                // unsubscribe should be idempotent, so only do this once
                CachedWorkerPool.INSTANCE.release(poolWorker);
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
                return Subscriptions.empty();
            }

            NewThreadScheduler.NewThreadWorker.ScheduledAction s = poolWorker.scheduleActual(action, delayTime, unit);
            innerSubscription.add(s);
            s.addParent(innerSubscription);
            return s;
        }
    }

    private static final class PoolWorker extends NewThreadScheduler.NewThreadWorker {
        private long expirationTime;

        PoolWorker(ThreadFactory threadFactory) {
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
