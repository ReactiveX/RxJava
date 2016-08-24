/**
 * Copyright 2016 Netflix, Inc.
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

package io.reactivex.internal.schedulers;

import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import io.reactivex.Scheduler;
import io.reactivex.disposables.*;
import io.reactivex.internal.disposables.EmptyDisposable;
import io.reactivex.plugins.RxJavaPlugins;

/**
 * Scheduler that creates and caches a set of thread pools and reuses them if possible.
 */
public final class IoScheduler extends Scheduler {
    private static final String WORKER_THREAD_NAME_PREFIX = "RxCachedThreadScheduler";
    static final RxThreadFactory WORKER_THREAD_FACTORY =
            new RxThreadFactory(WORKER_THREAD_NAME_PREFIX);

    private static final String EVICTOR_THREAD_NAME_PREFIX = "RxCachedWorkerPoolEvictor";
    static final RxThreadFactory EVICTOR_THREAD_FACTORY =
            new RxThreadFactory(EVICTOR_THREAD_NAME_PREFIX);

    private static final long KEEP_ALIVE_TIME = 60;
    private static final TimeUnit KEEP_ALIVE_UNIT = TimeUnit.SECONDS;
    
    static final ThreadWorker SHUTDOWN_THREAD_WORKER;
    final AtomicReference<CachedWorkerPool> pool;
    
    static final CachedWorkerPool NONE;
    static {
        NONE = new CachedWorkerPool(0, null);
        NONE.shutdown();

        SHUTDOWN_THREAD_WORKER = new ThreadWorker(new RxThreadFactory("RxCachedThreadSchedulerShutdown"));
        SHUTDOWN_THREAD_WORKER.dispose();
    }
    
    static final class CachedWorkerPool {
        private final long keepAliveTime;
        private final ConcurrentLinkedQueue<ThreadWorker> expiringWorkerQueue;
        final CompositeDisposable allWorkers;
        private final ScheduledExecutorService evictorService;
        private final Future<?> evictorTask;

        CachedWorkerPool(long keepAliveTime, TimeUnit unit) {
            this.keepAliveTime = unit != null ? unit.toNanos(keepAliveTime) : 0L;
            this.expiringWorkerQueue = new ConcurrentLinkedQueue<ThreadWorker>();
            this.allWorkers = new CompositeDisposable();

            ScheduledExecutorService evictor = null;
            Future<?> task = null;
            if (unit != null) {
                evictor = Executors.newScheduledThreadPool(1, EVICTOR_THREAD_FACTORY);
                try {
                    task = evictor.scheduleWithFixedDelay(
                            new Runnable() {
                                @Override
                                public void run() {
                                    evictExpiredWorkers();
                                }
                            }, this.keepAliveTime, this.keepAliveTime, TimeUnit.NANOSECONDS
                    );
                } catch (RejectedExecutionException ex) {
                    RxJavaPlugins.onError(ex);
                }
            }
            evictorService = evictor;
            evictorTask = task;
        }

        ThreadWorker get() {
            if (allWorkers.isDisposed()) {
                return SHUTDOWN_THREAD_WORKER;
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
                allWorkers.dispose();
            }
        }
    }

    public IoScheduler() {
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
    
    public int size() {
        return pool.get().allWorkers.size();
    }

    static final class EventLoopWorker extends Scheduler.Worker {
        private final CompositeDisposable tasks;
        private final CachedWorkerPool pool;
        private final ThreadWorker threadWorker;

        final AtomicBoolean once = new AtomicBoolean();

        EventLoopWorker(CachedWorkerPool pool) {
            this.pool = pool;
            this.tasks = new CompositeDisposable();
            this.threadWorker = pool.get();
        }

        @Override
        public void dispose() {
            if (once.compareAndSet(false, true)) {
                tasks.dispose();

                // releasing the pool should be the last action
                // should prevent pool reuse in case there is a blocking
                // action not responding to cancellation
//                threadWorker.scheduleDirect(() -> {
//                    pool.release(threadWorker);
//                }, 0, TimeUnit.MILLISECONDS);

                pool.release(threadWorker);
            }
        }

        @Override
        public boolean isDisposed() {
            return once.get();
        }

        @Override
        public Disposable schedule(Runnable action, long delayTime, TimeUnit unit) {
            if (tasks.isDisposed()) {
                // don't schedule, we are unsubscribed
                return EmptyDisposable.INSTANCE;
            }

            return threadWorker.scheduleActual(action, delayTime, unit, tasks);
        }
    }

    static final class ThreadWorker extends NewThreadWorker {
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