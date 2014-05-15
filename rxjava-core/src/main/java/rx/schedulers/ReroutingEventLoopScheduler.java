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

import rx.Scheduler;
import rx.Subscription;
import rx.functions.Action0;
import rx.subscriptions.CompositeSubscription;
import rx.subscriptions.Subscriptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public final class ReroutingEventLoopScheduler extends Scheduler {
    @Override
    public Worker createWorker() {
        return WorkerQueues.INSTANCE.createWorkerQueue();
    }

    // ToDo REMOVE THIS
    public void printStats() {
        for(CoreThreadWorker worker: CoreThreadWorkers.INSTANCE.CORE_THREAD_WORKERS) {
            System.out.printf("%d: avgLatency=%10.2f  count=%d\n", worker.myIndex, worker.latencyCounts.getAverage(), worker.getCount());
        }
    }

    protected static final class CoreThreadWorkers {
        final AtomicLong counter = new AtomicLong();
        protected final CoreThreadWorker[] CORE_THREAD_WORKERS;
        protected final static int NUM_CORES = Runtime.getRuntime().availableProcessors();
        protected static final CoreThreadWorkers INSTANCE = new CoreThreadWorkers();
        private final double[] latencies = new double[NUM_CORES];
        private final AtomicBoolean canReroute = new AtomicBoolean(true);
        private int rerouter=-1;
        private long lastReroutingAt=0;
        private final long ReroutingDelayIntervalMillis = 100;

        private CoreThreadWorkers() {
            CORE_THREAD_WORKERS = new CoreThreadWorker[NUM_CORES];
            for(int i=0; i<NUM_CORES; i++) {
                CORE_THREAD_WORKERS[i] = new CoreThreadWorker(i);
            }
        }

        private CoreThreadWorker getNextWorker() {
            return getNextWorkerToRouteTo();
        }

        protected CoreThreadWorker getNextWorkerToRouteTo() {
            return CORE_THREAD_WORKERS[(int)(counter.getAndIncrement() % NUM_CORES)];
        }

        protected int getRerouteDestination(int from, double[] latencyValues) {
            return -1;
        }

        int shouldRerouteTo(int theWorker) {
            if(!canReroute.compareAndSet(true, false))
                return -1;
            if(rerouter >= 0)
                return -1;
            if(lastReroutingAt > (System.currentTimeMillis()-ReroutingDelayIntervalMillis))
                return -1;
            try {
                for(int i=0; i<NUM_CORES; i++) {
                    latencies[i] = CORE_THREAD_WORKERS[i].latencyCounts.getAverage();
                }
                int dest = getRerouteDestination(theWorker, latencies);
                if(dest>=0)
                    rerouter = theWorker;
                return dest;
            }
            finally {
                canReroute.set(true);
            }
        }

        void endReroutring(int theWorker) {
            if(rerouter == theWorker) {
                lastReroutingAt = System.currentTimeMillis();
                rerouter = -1;
            }
        }

    }

    protected static final class WorkerQueueToReRoute {
        long qIndex;

    }

    private static final class AverageOfN {
        private final int MAX_LATENCY_COUNTS=10;
        private final List<Long> list = new ArrayList<Long>(MAX_LATENCY_COUNTS+1);
        private long sum=0;
        void add(long val) {
            list.add(val);
            if(list.size()>=MAX_LATENCY_COUNTS) {
                long v = list.remove(0);
                sum -= v;
            }
            sum += val;
        }
        double getAverage() {
            if(list.isEmpty())
                return 0.0;
            return sum/list.size();
        }
    }

    protected static final class CoreThreadWorker {
        private final int myIndex;
        private final ScheduledExecutorService executor;
        private volatile int counter = 0;
        private AverageOfN latencyCounts = new AverageOfN();
        //private List<Long> latencyCounts = new ArrayList<Long>(MAX_LATENCY_COUNTS+1);
        private Map<Long, AverageOfN> queueCounts = new HashMap<Long, AverageOfN>();
        private long latencyEvalInterval=100; // ToDo what's a good number?
        private static final double EPS=0.25;

        CoreThreadWorker(final int index) {
            this.myIndex = index;
            executor = new ScheduledThreadPoolExecutor(1, new ThreadFactory() {
                @Override
                public Thread newThread(Runnable r) {
                    Thread t = new Thread(r, "RxScheduledExecutorPool-" + index);
                    t.setDaemon(true);
                    return t;
                }
            });
            latencyEvalWrapper();
        }

        private void latencyEvalWrapper() {
            executor.schedule(new Runnable() {
                @Override
                public void run() {
                    scheduleLatencyEval(System.currentTimeMillis());
                }
            }, latencyEvalInterval, TimeUnit.MILLISECONDS);
        }

        private void endRerouting() {
            CoreThreadWorkers.INSTANCE.endReroutring(myIndex);
        }

        private void scheduleLatencyEval(final long start) {
            executor.submit(new Runnable() {
                @Override
                public void run() {
                    latencyCounts.add(System.currentTimeMillis() - start);
                    if(queueCounts.size()>1) {
                        int q=0;
                        long maxQ=-1;
                        double max=0.0;
                        for(Map.Entry<Long, AverageOfN> entry: queueCounts.entrySet()) {
                            if(entry.getValue().getAverage()>EPS) {
                                q++;
                                if(entry.getValue().getAverage()>max) {
                                    max = entry.getValue().getAverage();
                                    maxQ = entry.getKey();
                                }
                            }
                        }
                        if(q>1) {
                            int dest = CoreThreadWorkers.INSTANCE.shouldRerouteTo(myIndex);
                            if(dest>=0) {
                                if(dest==myIndex)
                                    endRerouting();
                                else {
                                    // reroute maxQ
                                    final long reroutedQ = maxQ;
                                    WorkerQueues.INSTANCE.getWorkerQueue(reroutedQ).rerouteTo(dest, new Action0() {
                                        @Override
                                        public void call() {
                                            queueCounts.remove(reroutedQ);
                                            endRerouting();
                                        }
                                    });
                                }
                            }
                        }
                    }
                    latencyEvalWrapper();
                }
            });
        }

        long getCount() {
            return counter;
        }

        Future<?> submit(final long workerQNum, final Runnable runnable) {
            return executor.submit(new Runnable() {
                @Override
                public void run() {
                    counter++;
                    if(queueCounts.get(workerQNum) == null) {
                        queueCounts.put(workerQNum, new AverageOfN());
                    }
                    queueCounts.get(workerQNum).add(1L);
                    runnable.run();
                }
            });
        }

        Future<?> schedule(final long workerQNum, final Runnable runnable, long delayTime, TimeUnit unit) {
            return executor.schedule(new Runnable() {
                @Override
                public void run() {
                    counter++;
                    if(queueCounts.get(workerQNum) == null) {
                        queueCounts.put(workerQNum, new AverageOfN());
                    }
                    queueCounts.get(workerQNum).add(1L);
                    runnable.run();
                }}, delayTime, unit);
        }

        void remove(final long workerQNum) {
            executor.submit(new Runnable() {
                @Override
                public void run() {
                    queueCounts.remove(workerQNum);
                }
            });
        }
    }

    private static final class WorkerQueues {
        static final WorkerQueues INSTANCE = new WorkerQueues();
        private final ConcurrentMap<Long, WorkerQueue> workerQMap;
        final AtomicLong queueCounter;

        private WorkerQueues() {
            workerQMap = new ConcurrentHashMap<Long, WorkerQueue>();
            queueCounter = new AtomicLong();
        }

        WorkerQueue createWorkerQueue() {
            long idx = queueCounter.getAndIncrement();
            WorkerQueue workerQueue = new WorkerQueue(idx, CoreThreadWorkers.INSTANCE.getNextWorker());
            workerQMap.put(idx, workerQueue);
            return workerQueue;
        }

        WorkerQueue getWorkerQueue(long idx) {
            return workerQMap.get(idx);
        }

        void removeWorker(long idx) {
            workerQMap.remove(idx);
        }
    }

    // ToDo: confirm that Worker.schedule(...) methods are called sequentially
    private static final class WorkerQueue extends Scheduler.Worker {
        private final long myIndex;
        private volatile CoreThreadWorker worker;
        private final CompositeSubscription innerSubscription = new CompositeSubscription();
        private volatile CoreThreadWorker transferDest=null;
        private volatile Action0 onTransferComplete=null;
        private boolean transferInitiated=false;
        private final List<Action0> transferQueue = new LinkedList<Action0>();
        private final AtomicBoolean transferCompletionReady = new AtomicBoolean(false);

        WorkerQueue(long myIndex, CoreThreadWorker worker) {
            this.myIndex = myIndex;
            this.worker = worker;
        }

        void rerouteTo(int dest, Action0 onComplete) {
            onTransferComplete = onComplete;
            transferInitiated = false;
            transferDest = CoreThreadWorkers.INSTANCE.CORE_THREAD_WORKERS[dest];
        }

        private void setupReroute() {
            if(transferInitiated)
                return;
            transferInitiated = true;
            transferQueue.clear();
            transferCompletionReady.set(false);
            worker.submit(myIndex, new Runnable() {
                @Override
                public void run() {
                    transferCompletionReady.set(true);
                }
            });
        }

        private void markRerouteComplete() {
            worker = transferDest;
            transferDest = null;
        }

        private boolean rerouteIfNeeded(final Action0 action) {
            if(transferDest == null)
                return false;
            setupReroute();
            if(transferCompletionReady.get()) {
                // finish the reroute
                // ToDo subscriptions need to be managed correctly!!!
//                for(Action0 a: transferQueue) {
//                    transferDest.submit(myIndex, a);
//                }
                markRerouteComplete();
                onTransferComplete.call();
            }
            else
                transferQueue.add(action);
            return true;
        }

        @Override
        public Subscription schedule(final Action0 action) {
//            if(rerouteIfNeeded(action))
//                return SomeUsefulSubscription;
            final AtomicReference<Subscription> sf = new AtomicReference<Subscription>();
            Subscription s = Subscriptions.from(worker.submit(myIndex, new Runnable() {
                @Override
                public void run() {
                    Subscription s = sf.get();
                    try {
                        if(innerSubscription.isUnsubscribed() || (s != null && s.isUnsubscribed())) {
                            return;
                        }
                        action.call();
                    }
                    finally {
                        if (s != null) {
                            innerSubscription.remove(s);
                        }
                    }
                }
            }));
            sf.set(s);
            innerSubscription.add(s);
            return s;
        }

        @Override
        public Subscription schedule(final Action0 action, long delayTime, TimeUnit unit) {
//            if(rerouteIfNeeded(action)) // need to handle delayed action
//                return SomeUsefulSubscription;
            final AtomicReference<Subscription> sf = new AtomicReference<Subscription>();
            Subscription s = Subscriptions.from(worker.schedule(myIndex, new Runnable() {
                @Override
                public void run() {
                    Subscription s = sf.get();
                    try {
                        if (innerSubscription.isUnsubscribed() || (s != null && s.isUnsubscribed())) {
                            return;
                        }
                        action.call();
                    } finally {
                        if (s != null) {
                            innerSubscription.remove(s);
                        }
                    }
                }
            }, delayTime, unit));
            sf.set(s);
            innerSubscription.add(s);
            return s;
        }

        @Override
        public void unsubscribe() {
            innerSubscription.unsubscribe();
            WorkerQueues.INSTANCE.removeWorker(myIndex);
            worker.remove(myIndex);
        }

        @Override
        public boolean isUnsubscribed() {
            return innerSubscription.isUnsubscribed();
        }
    }
}