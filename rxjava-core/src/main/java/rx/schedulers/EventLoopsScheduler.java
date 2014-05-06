package rx.schedulers;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import rx.Scheduler;
import rx.Subscription;
import rx.functions.Action0;
import rx.schedulers.NewThreadScheduler.NewThreadWorker.Remover;
import rx.schedulers.NewThreadScheduler.NewThreadWorker.ScheduledAction;
import rx.subscriptions.CompositeSubscription;
import rx.subscriptions.Subscriptions;

/* package */class EventLoopsScheduler extends Scheduler {
    /** Manages a fixed number of workers. */
    static final class FixedSchedulerPool {
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
        long n;

        FixedSchedulerPool() {
            // initialize event loops
            this.cores = Runtime.getRuntime().availableProcessors();
            this.eventLoops = new PoolWorker[cores];
            for (int i = 0; i < cores; i++) {
                this.eventLoops[i] = new PoolWorker(factory);
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
    EventLoopsScheduler() {
        pool = new FixedSchedulerPool();
    }
    
    @Override
    public Worker createWorker() {
        return new EventLoopWorker(pool.getEventLoop());
    }

    private static class EventLoopWorker extends Scheduler.Worker {
        private final CompositeSubscription innerSubscription = new CompositeSubscription();
        private final PoolWorker poolWorker;

        EventLoopWorker(PoolWorker poolWorker) {
            this.poolWorker = poolWorker;
            
        }

        @Override
        public void unsubscribe() {
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
            
            ScheduledAction s = poolWorker.scheduleActual(action, delayTime, unit);
            innerSubscription.add(s);
            s.addParent(innerSubscription);
            return s;
        }
    }
    
    private static final class PoolWorker extends NewThreadScheduler.NewThreadWorker {
        PoolWorker(ThreadFactory threadFactory) {
            super(threadFactory);
        }
    }
}