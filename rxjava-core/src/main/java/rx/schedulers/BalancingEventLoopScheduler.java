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
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public final class BalancingEventLoopScheduler extends Scheduler {

    private final AtomicLong qNum = new AtomicLong(0);

    @Override
    public Worker createWorker() {
        WorkerQueue workerQueue = new WorkerQueue(qNum.getAndIncrement());
        CoreThreadWorkers.INSTANCE.assignCoreThread(workerQueue);
        return workerQueue;
    }

    // ToDo Remove this
    public int getNumRebalances() {
        return CoreThreadWorkers.INSTANCE.getNumRebalances();
    }

    private static final class CoreThreadWorkers {
        private final CoreThreadWorker[] CORE_THREAD_WORKERS;
        private final static int NUM_CORES = Runtime.getRuntime().availableProcessors();
        static final CoreThreadWorkers INSTANCE;
        static {
            INSTANCE = new CoreThreadWorkers();
            INSTANCE.start();
        }
        private int n=1;
        private long lastRebalancedAt=System.currentTimeMillis();
        private final long rebalanceInterval=100; // ToDo what's a good number?
        private int numRebalances=0;

        CoreThreadWorkers() {
            CORE_THREAD_WORKERS = new CoreThreadWorker[NUM_CORES];
            for(int c=0; c<NUM_CORES; c++) {
                CORE_THREAD_WORKERS[c] = new CoreThreadWorker(c, c==0);
                CORE_THREAD_WORKERS[c].setDaemon(true);
            }
        }
        private void start() {
            for (Thread t : CORE_THREAD_WORKERS) {
                t.start();
            }
        }
        
        void assignCoreThread(WorkerQueue queue) {
            // ToDo figure out the lightest loaded worker instead of n++
            CoreThreadWorker worker = CORE_THREAD_WORKERS[n++ % CORE_THREAD_WORKERS.length];
            worker.assignQueue(queue);
        }

        private boolean shouldRebalancePair(HotQueue from, HotQueue to) {
            return (from.totalLoad-from.qLoad) >= (to.totalLoad+from.qLoad);
        }

        int getNumRebalances() {
            return numRebalances;
        }

        void rebalanceQs() {
            if((lastRebalancedAt+rebalanceInterval)>System.currentTimeMillis())
                return;
            lastRebalancedAt=System.currentTimeMillis();
            List<HotQueue> hotQueues = new ArrayList<HotQueue>();
            boolean needRebalncing=false;
            for(int c=0; c<CORE_THREAD_WORKERS.length; c++) {
                HotQueue queue = CORE_THREAD_WORKERS[c].getHotQueue();
                if(queue==null)
                    queue = new HotQueue(c, null, 0, 0);
                else if(queue.qLoad>0)
                    needRebalncing=true;
                hotQueues.add(queue);
            }
            if(needRebalncing) {
                Collections.sort(hotQueues, new Comparator<HotQueue>() {
                    @Override
                    public int compare(HotQueue o1, HotQueue o2) {
                        if(o1.queue==null || o1.totalLoad==0)
                            return -1;
                        if(o2.queue==null || o2.totalLoad==0)
                            return 1;
                        if(o1.qLoad == 0)
                            return -1;
                        if(o2.qLoad == 0)
                            return 1;
                        return o1.totalLoad < o2.totalLoad ? -1 : (o1.totalLoad > o2.totalLoad ? 1 : 0);
                    }
                });
                boolean allDone=false;
                for(int h=hotQueues.size()-1; h>0 && !allDone; h--) {
                    if(hotQueues.get(h).qLoad == 0)
                        allDone = true;
                    else {
                        for(int b=0; b<h; b++) {
                            if(shouldRebalancePair(hotQueues.get(h), hotQueues.get(b))) {
                                hotQueues.get(b).totalLoad += hotQueues.get(h).qLoad;
                                CORE_THREAD_WORKERS[hotQueues.get(h).workerIndex].transferQueue(hotQueues.get(h).queue,
                                        CORE_THREAD_WORKERS[hotQueues.get(b).workerIndex]);
                                numRebalances++;
                            }
                        }
                    }
                }
            }
        }
    }

    private static final class HotQueue {
        final int workerIndex;
        WorkerQueue queue;
        long qLoad;
        long totalLoad;
        private HotQueue(int workerIndex, WorkerQueue queue, long qLoad, long totalLoad) {
            this.workerIndex = workerIndex;
            this.queue = queue;
            this.qLoad = qLoad;
            this.totalLoad = totalLoad;
        }
    }

    private static final class QueueTransferRequest {
        private final WorkerQueue queue;
        private final CoreThreadWorker worker;
        private QueueTransferRequest(WorkerQueue queue, CoreThreadWorker worker) {
            this.queue = queue;
            this.worker = worker;
        }
    }

    private static final class CoreThreadWorker extends Thread {
        private final int myIndex;
        private final List<WorkerQueue> workerQueues = new ArrayList<WorkerQueue>();
        private final int WORK_BUF_SIZE=10;
        private final int MAX_TIGHT_LOOP = 5000;
        private final List<Action0> workBuffer = new ArrayList<Action0>(WORK_BUF_SIZE);
        private final BlockingQueue<QueueTransferRequest> transferRequests = new LinkedBlockingQueue<QueueTransferRequest>();
        private volatile HotQueue hotQueue;
        private final boolean rebalancer;

        CoreThreadWorker(int myIndex, boolean rebalancer) {
            this.myIndex = myIndex;
            this.rebalancer = rebalancer;
        }

        private HotQueue getHotQueue() {
            return hotQueue;
        }

        @Override
        public void run() {
            while(true) {
                trasnferRequests();
                Iterator<WorkerQueue> iterator = workerQueues.iterator();
                long ttlLoad=0;
                long now=0;
                WorkerQueue hq=null;
                long hqLoad=0;
                boolean hasMoreThanOneQ=false;
                while(iterator.hasNext()) {
                    WorkerQueue wQ = iterator.next();
                    if(wQ.isUnsubscribed()) {
                        iterator.remove();
                    }
                    else {
                        int tightLoopCount = MAX_TIGHT_LOOP;
                        do {
                            workBuffer.clear();
                            int w = (wQ.theQueue.peek() == null)? 0 : wQ.theQueue.drainTo(workBuffer, WORK_BUF_SIZE);
                            if(w > 0) {
                                now = System.currentTimeMillis();
                                for(int a=0; a<w; a++) {
                                    try {
                                        workBuffer.get(a).call();
                                    }
                                    catch (Exception e) {} // protect against spurious exception
                                }
                                long latestLoad = System.currentTimeMillis()-now;
                                ttlLoad += latestLoad;
                                if(!hasMoreThanOneQ && iterator.hasNext())
                                    hasMoreThanOneQ=true;
                                if(hasMoreThanOneQ && (hq == null || latestLoad>hqLoad)) {
                                    hq = wQ;
                                    hqLoad = latestLoad;
                                }
                            } else {
                                break;
                            }
                        } while (--tightLoopCount > 0);
                    }
                }
                if(ttlLoad>0) {
                    hotQueue = new HotQueue(myIndex, hq, hqLoad, ttlLoad);
                }
                else {
                    hotQueue = null;
                    // didn't do any work, sleep some ??
                    //Thread.yield();
                    synchronized (this) {
                        try{this.wait(0, 50);} catch (InterruptedException ie) {}
                    }
                }
                if(rebalancer)
                    CoreThreadWorkers.INSTANCE.rebalanceQs();
            }
        }

        private void trasnferRequests() {
            if(transferRequests.peek() != null) {
                List<QueueTransferRequest> xferRequests = new ArrayList<QueueTransferRequest>(CoreThreadWorkers.NUM_CORES);
                int x = transferRequests.drainTo(xferRequests, CoreThreadWorkers.NUM_CORES);
                for(int transferred=0; transferred<x; transferred++) {
                    QueueTransferRequest qtr = xferRequests.get(transferred);
                    if(qtr.worker == null) { // Xfering in
                        workerQueues.add(qtr.queue);
                    }
                    else {
                        workerQueues.remove(qtr.queue);
                        qtr.worker.assignQueue(qtr.queue);
                    }
                }
            }
        }

        void assignQueue(WorkerQueue queue) {
            transferRequests.add(new QueueTransferRequest(queue, null));
        }

        void transferQueue(WorkerQueue queue, CoreThreadWorker worker) {
            transferRequests.add(new QueueTransferRequest(queue, worker));
        }
    }

    private static final class WorkerQueue extends Scheduler.Worker {
        private final long myId;
        private final BlockingQueue<Action0> theQueue = new LinkedBlockingQueue<Action0>();
        private final CompositeSubscription innerSubscription = new CompositeSubscription();
        private volatile CoreThreadWorker worker=null;

        WorkerQueue(long myId) {
            this.myId = myId;
        }

        void setWorker(CoreThreadWorker worker) {
            this.worker = worker;
        }

        @Override
        public Subscription schedule(final Action0 action) {
            if(innerSubscription.isUnsubscribed())
                return Subscriptions.empty();
            theQueue.offer(new Action0() {
                @Override
                public void call() {
                    try{action.call();} catch (Exception e) {}//protect against spurious exception from action
                }
            });
            if(innerSubscription.isUnsubscribed()) {
                theQueue.clear();
                return Subscriptions.empty();
            }
            if(worker != null) {
                synchronized (worker) {
                    worker.notify();
                }
            }
            return Subscriptions.create(new Action0() {
                @Override
                public void call() {
                    theQueue.remove(action);
                }
            });
        }

        @Override
        public Subscription schedule(final Action0 action, long delayTime, TimeUnit unit) {
            final AtomicReference<Subscription> sf = new AtomicReference<Subscription>();
            // we use the system scheduler since it doesn't make sense to launch a new Thread and then sleep
            // we will instead schedule the event then launch the thread after the delay has passed
            ScheduledFuture<?> f = GenericScheduledExecutorService.getInstance().schedule(new Runnable() {

                @Override
                public void run() {
                    try {
                        if (innerSubscription.isUnsubscribed()) {
                            return;
                        }
                        // now that the delay is past schedule the work to be done for real on the UI thread
                        schedule(action);
                    } finally {
                        // remove the subscription now that we're completed
                        Subscription s = sf.get();
                        if (s != null) {
                            innerSubscription.remove(s);
                        }
                    }
                }
            }, delayTime, unit);
            Subscription s = Subscriptions.from(f);
            sf.set(s);
            innerSubscription.add(s);
            return s;
        }

        @Override
        public void unsubscribe() {
            innerSubscription.unsubscribe();
            theQueue.clear();
        }

        @Override
        public boolean isUnsubscribed() {
            return innerSubscription.isUnsubscribed();
        }
    }

}