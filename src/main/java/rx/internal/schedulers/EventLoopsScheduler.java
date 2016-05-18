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
package rx.internal.schedulers;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

import rx.*;
import rx.functions.Action0;
import rx.internal.util.*;
import rx.subscriptions.*;

public final class EventLoopsScheduler extends Scheduler implements SchedulerLifecycle {
    /**
     * Key to setting the maximum number of computation scheduler threads.
     * Zero or less is interpreted as use available. Capped by available.
     */
    static final String KEY_MAX_THREADS = "rx.scheduler.max-computation-threads";
    /** The maximum number of computation scheduler threads. */
    static final int MAX_THREADS;
    static {
        int maxThreads = Integer.getInteger(KEY_MAX_THREADS, 0);
        int ncpu = Runtime.getRuntime().availableProcessors();
        int max;
        if (maxThreads <= 0 || maxThreads > ncpu) {
            max = ncpu;
        } else {
            max = maxThreads;
        }
        MAX_THREADS = max;
    }
    
    static final PoolWorker SHUTDOWN_WORKER;
    static {
        SHUTDOWN_WORKER = new PoolWorker(RxThreadFactory.NONE);
        SHUTDOWN_WORKER.unsubscribe();
    }
    
    static final class FixedSchedulerPool {
        final int cores;

        final PoolWorker[] eventLoops;
        long n;

        FixedSchedulerPool(ThreadFactory threadFactory, int maxThreads) {
            // initialize event loops
            this.cores = maxThreads;
            this.eventLoops = new PoolWorker[maxThreads];
            for (int i = 0; i < maxThreads; i++) {
                this.eventLoops[i] = new PoolWorker(threadFactory);
            }
        }

        public PoolWorker getEventLoop() {
            int c = cores;
            if (c == 0) {
                return SHUTDOWN_WORKER;
            }
            // simple round robin, improvements to come
            return eventLoops[(int)(n++ % c)];
        }
        
        public void shutdown() {
            for (PoolWorker w : eventLoops) {
                w.unsubscribe();
            }
        }
    }
    /** This will indicate no pool is active. */
    static final FixedSchedulerPool NONE = new FixedSchedulerPool(null, 0);

    final ThreadFactory threadFactory;
    final AtomicReference<FixedSchedulerPool> pool;
    
    /**
     * Create a scheduler with pool size equal to the available processor
     * count and using least-recent worker selection policy.
     * @param threadFactory the factory to use with the executors
     */
    public EventLoopsScheduler(ThreadFactory threadFactory) {
        this.threadFactory = threadFactory;
        this.pool = new AtomicReference<FixedSchedulerPool>(NONE);
        start();
    }
    
    @Override
    public Worker createWorker() {
        return new EventLoopWorker(pool.get().getEventLoop());
    }
    
    @Override
    public void start() {
        FixedSchedulerPool update = new FixedSchedulerPool(threadFactory, MAX_THREADS);
        if (!pool.compareAndSet(NONE, update)) {
            update.shutdown();
        }
    }
    
    @Override
    public void shutdown() {
        for (;;) {
            FixedSchedulerPool curr = pool.get();
            if (curr == NONE) {
                return;
            }
            if (pool.compareAndSet(curr, NONE)) {
                curr.shutdown();
                return;
            }
        }
    }
    
    /**
     * Schedules the action directly on one of the event loop workers
     * without the additional infrastructure and checking.
     * @param action the action to schedule
     * @return the subscription
     */
    public Subscription scheduleDirect(Action0 action) {
       PoolWorker pw = pool.get().getEventLoop();
       return pw.scheduleActual(action, -1, TimeUnit.NANOSECONDS);
    }

    private static class EventLoopWorker extends Scheduler.Worker {
        private final SubscriptionList serial = new SubscriptionList();
        private final CompositeSubscription timed = new CompositeSubscription();
        private final SubscriptionList both = new SubscriptionList(serial, timed);
        private final PoolWorker poolWorker;

        EventLoopWorker(PoolWorker poolWorker) {
            this.poolWorker = poolWorker;
            
        }

        @Override
        public void unsubscribe() {
            both.unsubscribe();
        }

        @Override
        public boolean isUnsubscribed() {
            return both.isUnsubscribed();
        }

        @Override
        public Subscription schedule(final Action0 action) {
            if (isUnsubscribed()) {
                return Subscriptions.unsubscribed();
            }

            return poolWorker.scheduleActual(new Action0() {
                @Override
                public void call() {
                    if (isUnsubscribed()) {
                        return;
                    }
                    action.call();
                }
            }, 0, null, serial);
        }

        @Override
        public Subscription schedule(final Action0 action, long delayTime, TimeUnit unit) {
            if (isUnsubscribed()) {
                return Subscriptions.unsubscribed();
            }

            return poolWorker.scheduleActual(new Action0() {
                @Override
                public void call() {
                    if (isUnsubscribed()) {
                        return;
                    }
                    action.call();
                }
            }, delayTime, unit, timed);
        }
    }

    static final class PoolWorker extends NewThreadWorker {
        PoolWorker(ThreadFactory threadFactory) {
            super(threadFactory);
        }
    }
}
