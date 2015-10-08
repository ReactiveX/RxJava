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

import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import rx.*;
import rx.functions.Action0;
import rx.internal.schedulers.*;
import rx.internal.util.RxThreadFactory;
import rx.subscriptions.*;

/* package */final class CachedThreadScheduler extends Scheduler implements SchedulerLifecycle {
    private static final String WORKER_THREAD_NAME_PREFIX = "RxCachedThreadScheduler-";
    private static final RxThreadFactory WORKER_THREAD_FACTORY =
            new RxThreadFactory(WORKER_THREAD_NAME_PREFIX);

    private static final String EVICTOR_THREAD_NAME_PREFIX = "RxCachedWorkerPoolEvictor-";
    private static final RxThreadFactory EVICTOR_THREAD_FACTORY =
            new RxThreadFactory(EVICTOR_THREAD_NAME_PREFIX);

    private static final long KEEP_ALIVE_TIME = 60;
    private static final TimeUnit KEEP_ALIVE_UNIT = TimeUnit.SECONDS;
    
    static final ThreadWorker SHUTDOWN_THREADWORKER;
    static {
        SHUTDOWN_THREADWORKER = new ThreadWorker(new RxThreadFactory("RxCachedThreadSchedulerShutdown-"));
        SHUTDOWN_THREADWORKER.unsubscribe();
    }
    
    private static final class CachedWorkerPool {
        private final long keepAliveTime;
        private final ConcurrentLinkedQueue<ThreadWorker> expiringWorkerQueue;
        private final CompositeSubscription allWorkers;
        private final ScheduledExecutorService evictorService;
        private final Future<?> evictorTask;

        CachedWorkerPool(long keepAliveTime, TimeUnit unit) {
            this.keepAliveTime = unit != null ? unit.toNanos(keepAliveTime) : 0L;
            this.expiringWorkerQueue = new ConcurrentLinkedQueue<ThreadWorker>();
            this.allWorkers = new CompositeSubscription();

            ScheduledExecutorService evictor = null;
            Future<?> task = null;
            if (unit != null) {
                evictor = Executors.newScheduledThreadPool(1, EVICTOR_THREAD_FACTORY);
                NewThreadWorker.tryEnableCancelPolicy(evictor);
                task = evictor.scheduleWithFixedDelay(
                        new Runnable() {
                            @Override
                            public void run() {
                                evictExpiredWorkers();
                            }
                        }, this.keepAliveTime, this.keepAliveTime, TimeUnit.NANOSECONDS
                );
            }
            evictorService = evictor;
            evictorTask = task;
        }

        ThreadWorker get() {
            if (allWorkers.isUnsubscribed()) {
                return SHUTDOWN_THREADWORKER;
            }
            while (!expiringWorkerQueue.isEmpty()) {
                ThreadWorker threadWorker = expiringWorkerQueue.poll();
                if (threadWorker != null) {
                    return threadWorker;
                }
            }

            // No cached worker found, so create a new one.
            ThreadWorker w = new ThreadWorker(WORKER_THREAD_FACTORY);
            allWorkers.add(w);
            return w;
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
                            allWorkers.remove(threadWorker);
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
        
        void shutdown() {
            try {
                if (evictorTask != null) {
                    evictorTask.cancel(true);
                }
                if (evictorService != null) {
                    evictorService.shutdownNow();
                }
            } finally {
                allWorkers.unsubscribe();
            }
        }
    }

    final AtomicReference<CachedWorkerPool> pool;
    
    static final CachedWorkerPool NONE;
    static {
        NONE = new CachedWorkerPool(0, null);
        NONE.shutdown();
    }
    
    public CachedThreadScheduler() {
        this.pool = new AtomicReference<CachedWorkerPool>(NONE);
        start();
    }
    
    @Override
    public void start() {
        CachedWorkerPool update = new CachedWorkerPool(KEEP_ALIVE_TIME, KEEP_ALIVE_UNIT);
        if (!pool.compareAndSet(NONE, update)) {
            update.shutdown();
        }
    }
    @Override
    public void shutdown() {
        for (;;) {
            CachedWorkerPool curr = pool.get();
            if (curr == NONE) {
                return;
            }
            if (pool.compareAndSet(curr, NONE)) {
                curr.shutdown();
                return;
            }
        }
    }
    
    @Override
    public Worker createWorker() {
        return new EventLoopWorker(pool.get());
    }

    private static final class EventLoopWorker extends Scheduler.Worker {
        private final CompositeSubscription innerSubscription = new CompositeSubscription();
        private final CachedWorkerPool pool;
        private final ThreadWorker threadWorker;
        @SuppressWarnings("unused")
        volatile int once;
        static final AtomicIntegerFieldUpdater<EventLoopWorker> ONCE_UPDATER
                = AtomicIntegerFieldUpdater.newUpdater(EventLoopWorker.class, "once");

        EventLoopWorker(CachedWorkerPool pool) {
            this.pool = pool;
            this.threadWorker = pool.get();
        }

        @Override
        public void unsubscribe() {
            if (ONCE_UPDATER.compareAndSet(this, 0, 1)) {
                // unsubscribe should be idempotent, so only do this once
                pool.release(threadWorker);
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