package rx.schedulers;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import rx.Scheduler;
import rx.Subscription;
import rx.functions.Action0;
import rx.schedulers.NewThreadScheduler.OnActionComplete;
import rx.subscriptions.CompositeSubscription;
import rx.subscriptions.Subscriptions;

/* package */class EventLoopsScheduler extends Scheduler implements Subscription {
    /** Determines how the next worker is selected from the pool. */
    public enum WorkerSelectionPolicy {
        ROUND_ROBIN,
        LEAST_RECENT_USED
    }
    /** Manages a fixed number of workers. */
    static final class FixedSchedulerPool implements Subscription {
        final int cores;
        final ThreadFactory factory = new ThreadFactory() {
            final AtomicInteger counter = new AtomicInteger();

            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "RxComputationThreadPool-" + counter.incrementAndGet());
                t.setDaemon(true);
                return t;
            }
        };

        final PoolWorker[] eventLoops;
        /** Records when was the last time the event loop was accessed. */
        final AtomicReference<long[]> lastAccess;
        final WorkerSelectionPolicy policy;
        final AtomicLong n;
        final AtomicBoolean unsubscribed;

        FixedSchedulerPool() {
            this(Runtime.getRuntime().availableProcessors(), WorkerSelectionPolicy.LEAST_RECENT_USED);
        }
        FixedSchedulerPool(int numThreads, WorkerSelectionPolicy policy) {
            // initialize event loops
            this.policy = policy;
            this.unsubscribed = new AtomicBoolean();
            this.n = new AtomicLong();
            this.cores = numThreads;
            this.eventLoops = new PoolWorker[numThreads];
            this.lastAccess = new AtomicReference<long[]>(new long[numThreads]);
            for (int i = 0; i < numThreads; i++) {
                this.eventLoops[i] = new PoolWorker(factory, i, this);
            }
        }

        public PoolWorker getEventLoop() {
            if (policy == WorkerSelectionPolicy.ROUND_ROBIN) {
//            // round-robin selection (improvements to come)
                return eventLoops[(int)(n.getAndIncrement() % cores)];
            }
            // least recent used
            long[] oldState;
            long[] newState;
            int index;
            do {
                oldState = lastAccess.get();
                long time = Long.MAX_VALUE;
                index = -1;
                for (int i = 0; i < oldState.length; i++) {
                    long t = oldState[i];
                    if (t < time) {
                        index = i;
                        time = t;
                    }
                }
                newState = oldState.clone();
                newState[index] = now();
            } while (!lastAccess.compareAndSet(oldState, newState));
            return eventLoops[index];
        }
        /** Record the last access time of an Event loop. */
        public void access(int index) {
            if (policy == WorkerSelectionPolicy.ROUND_ROBIN) {
                // don't bother
                return;
            }
            long[] oldState;
            long[] newState;
            do {
                oldState = lastAccess.get();
                newState = oldState.clone();
                newState[index] = now();
            } while (!lastAccess.compareAndSet(oldState, newState));
        }
        /** The current time. */
        long now() {
            return System.currentTimeMillis();
        }

        @Override
        public void unsubscribe() {
            if (unsubscribed.compareAndSet(false, true)) {
                for (PoolWorker w : eventLoops) {
                    w.unsubscribe();
                }
            }
        }

        @Override
        public boolean isUnsubscribed() {
            return unsubscribed.get();
        }
        
    }

    final FixedSchedulerPool pool;
    
    /**
     * Create a scheduler with pool size equal to the available processor
     * count and using least-recent worker selection policy.
     */
    EventLoopsScheduler() {
        pool = new FixedSchedulerPool();
    }
    /**
     * Create a scheduler with pool size as requested and least-recent
     * worker selection policy
     * @param numThreads the number of threads
     */
    EventLoopsScheduler(int numThreads) {
        pool = new FixedSchedulerPool(numThreads, WorkerSelectionPolicy.LEAST_RECENT_USED);
    }
    /**
     * Create a scheduler with pool size and worker selection policy
     * as requested.
     * @param numThreads the number of threads
     * @param policy the worker selection policy
     */
    EventLoopsScheduler(int numThreads, WorkerSelectionPolicy policy) {
        pool = new FixedSchedulerPool(numThreads, policy);
    }
    
    @Override
    public Worker createWorker() {
        return new EventLoopWorker(pool.getEventLoop());
    }

    private static class EventLoopWorker extends Scheduler.Worker {
        private final CompositeSubscription innerSubscription = new CompositeSubscription();
        private final PoolWorker poolWorker;
        private final OnActionComplete onComplete;
        /** Unsubscribe and notify the pool only once. */
        private final AtomicBoolean once;

        EventLoopWorker(PoolWorker poolWorker) {
            this.once = new AtomicBoolean();
            this.poolWorker = poolWorker;
            this.onComplete = new OnActionComplete() {

                @Override
                public void complete(Subscription s) {
                    innerSubscription.remove(s);
                }

            };
        }

        @Override
        public void unsubscribe() {
            if (once.compareAndSet(false, true)) {
                innerSubscription.unsubscribe();
                poolWorker.access();
            }
        }

        @Override
        public boolean isUnsubscribed() {
            return innerSubscription.isUnsubscribed();
        }

        @Override
        public Subscription schedule(Action0 action) {
            if (innerSubscription.isUnsubscribed()) {
                // don't schedule, we are unsubscribed
                return Subscriptions.empty();
            }
            Subscription s = poolWorker.schedule(action, onComplete);
            innerSubscription.add(extractToken(s));
            return s;
        }
        @Override
        public Subscription schedule(Action0 action, long delayTime, TimeUnit unit) {
            if (innerSubscription.isUnsubscribed()) {
                // don't schedule, we are unsubscribed
                return Subscriptions.empty();
            }
            
            Subscription s = poolWorker.schedule(action, delayTime, unit, onComplete);
            innerSubscription.add(extractToken(s));
            return s;
        }
        /** Extract the actual cancellation token of the scheduled action from the returned object. */
        private Subscription extractToken(Subscription s) {
            if (s.getClass() != NewThreadScheduler.NewThreadWorker.ActionCancel.class) {
                // if we get here, then the underlying scheduler was terminated
                return s;
            }
            return ((NewThreadScheduler.NewThreadWorker.ActionCancel)s).token;
        }

    }
    
    private static final class PoolWorker extends NewThreadScheduler.NewThreadWorker {
        /** The last time this event loop did something. */
        final int index;
        final FixedSchedulerPool pool;
        PoolWorker(ThreadFactory threadFactory, int index, FixedSchedulerPool pool) {
            super(threadFactory);
            this.index = index;
            this.pool = pool;
        }
        void access() {
            pool.access(index);
        }
    }

    @Override
    public boolean isUnsubscribed() {
        return pool.isUnsubscribed();
    }

    @Override
    public void unsubscribe() {
        pool.unsubscribe();
    }

    @Override
    public int parallelism() {
        return pool.cores;
    }
}
