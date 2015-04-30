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

import rx.*;
import rx.functions.Action0;
import rx.internal.util.*;
import rx.subscriptions.*;

public class EventLoopsScheduler extends Scheduler {
    /** Manages a fixed number of workers. */
    private static final String THREAD_NAME_PREFIX = "RxComputationThreadPool-";
    private static final RxThreadFactory THREAD_FACTORY = new RxThreadFactory(THREAD_NAME_PREFIX);
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
    static final class FixedSchedulerPool {
        final int cores;

        final PoolWorker[] eventLoops;
        long n;

        FixedSchedulerPool() {
            // initialize event loops
            this.cores = MAX_THREADS;
            this.eventLoops = new PoolWorker[cores];
            for (int i = 0; i < cores; i++) {
                this.eventLoops[i] = new PoolWorker(THREAD_FACTORY);
            }
        }

        public PoolWorker getEventLoop() {
            // simple round robin, improvements to come
            return eventLoops[(int)(n++ % cores)];
        }
    }

    final FixedSchedulerPool pool;
    
    /**
     * Create a scheduler with pool size equal to the available processor
     * count and using least-recent worker selection policy.
     */
    public EventLoopsScheduler() {
        pool = new FixedSchedulerPool();
    }
    
    @Override
    public Worker createWorker() {
        return new EventLoopWorker(pool.getEventLoop());
    }
    
    /**
     * Schedules the action directly on one of the event loop workers
     * without the additional infrastructure and checking.
     * @param action the action to schedule
     * @return the subscription
     */
    public Subscription scheduleDirect(Action0 action) {
       PoolWorker pw = pool.getEventLoop();
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
        public Subscription schedule(Action0 action) {
            if (isUnsubscribed()) {
                return Subscriptions.unsubscribed();
            }
            ScheduledAction s = poolWorker.scheduleActual(action, 0, null, serial);
            
            return s;
        }
        @Override
        public Subscription schedule(Action0 action, long delayTime, TimeUnit unit) {
            if (isUnsubscribed()) {
                return Subscriptions.unsubscribed();
            }
            ScheduledAction s = poolWorker.scheduleActual(action, delayTime, unit, timed);
            
            return s;
        }
    }
    
    private static final class PoolWorker extends NewThreadWorker {
        PoolWorker(ThreadFactory threadFactory) {
            super(threadFactory);
        }
    }
}
