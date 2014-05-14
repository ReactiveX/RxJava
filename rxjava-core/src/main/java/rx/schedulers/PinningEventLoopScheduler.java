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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;
import rx.Scheduler;
import rx.Subscription;
import rx.functions.Action0;
import rx.schedulers.NewThreadScheduler.NewThreadWorker.ScheduledAction;
import rx.subscriptions.CompositeSubscription;
import rx.subscriptions.MultipleAssignmentSubscription;
import rx.subscriptions.Subscriptions;

public final class PinningEventLoopScheduler extends Scheduler {
    static final int UNPINNED = -1;
    final PinningThread[] threads;
    long n;
    PinningEventLoopScheduler() {
        this.threads = new PinningThread[Runtime.getRuntime().availableProcessors()];
        for  (int i = 0; i < threads.length; i++) {
            PinningThread pt = new PinningThread(i, this);
            pt.setDaemon(true);
            threads[i] = pt;
        }
    }
    private void startAll() {
        for (PinningThread pt : threads) {
            pt.start();
        }
    }
    public static final PinningEventLoopScheduler INSTANCE;
    static {
        INSTANCE = new PinningEventLoopScheduler();
        INSTANCE.startAll();
    }
    @Override
    public Worker createWorker() {
        return new PinningEventLoopWorker(this, (int)(n++ % threads.length));
    }
    
    static final class PinningEventLoopWorker extends Scheduler.Worker {
        static final AtomicIntegerFieldUpdater<PinningEventLoopWorker> PINNED_UPDATER =
                AtomicIntegerFieldUpdater.newUpdater(PinningEventLoopWorker.class, "pinned");
        final CompositeSubscription innerSubsription = new CompositeSubscription();
        final PinningEventLoopScheduler parent;
        final int initialIndex;
        volatile int pinned = UNPINNED;
        final ReentrantReadWriteLock stealLock;
        final ReentrantReadWriteLock.ReadLock stealRead;
        final ReentrantReadWriteLock.WriteLock stealWrite;
        
        public PinningEventLoopWorker(PinningEventLoopScheduler parent, int initialIndex) {
            this.parent = parent;
            this.initialIndex = initialIndex;
            this.stealLock = new ReentrantReadWriteLock();
            this.stealRead = stealLock.readLock();
            this.stealWrite = stealLock.writeLock();
        }
        
        @Override
        public Subscription schedule(Action0 action) {
            PinnedAction pa = new PinnedAction(new ScheduledAction(action, innerSubsription), this);
            innerSubsription.add(pa.action);
            
            schedule(pa);
            
            return pa.action;
        }

        @Override
        public Subscription schedule(Action0 action, long delayTime, TimeUnit unit) {
            final PinnedAction pa = new PinnedAction(new ScheduledAction(action, innerSubsription), this);
            innerSubsription.add(pa.action);
            
            final MultipleAssignmentSubscription mas = new MultipleAssignmentSubscription();
            
            Future<?> f = GenericScheduledExecutorService.getInstance().schedule(new Runnable() {

                @Override
                public void run() {
                    mas.set(pa.action);
                    if (!mas.isUnsubscribed()) {
                        schedule(pa);
                    }
                }
                
            }, delayTime, unit);
            
            mas.set(Subscriptions.from(f));
            
            return mas;
        }
        
        void schedule(PinnedAction pa) {
            while (true) {
                int pi = pinned;
                if (pi == UNPINNED) {
                    if (stealRead.tryLock()) {
                        try {
                            parent.threads[initialIndex].offer(pa);
                        } finally {
                            stealRead.unlock();
                        }
                        break;
                    }
                } else {
                    parent.threads[pi].offer(pa);
                    break;
                }
            }
        }

        @Override
        public void unsubscribe() {
            innerSubsription.unsubscribe();
        }

        @Override
        public boolean isUnsubscribed() {
            return innerSubsription.isUnsubscribed();
        }
        public boolean casPinned(int expected, int newValue) {
            return PINNED_UPDATER.compareAndSet(this, expected, newValue);
        }
    }
    static final class PinnedAction {
        final ScheduledAction action;
        final PinningEventLoopWorker parent;
        /** Make sure the action is executed once even if queued multiple times due to steal. */
        volatile boolean once = true;

        public PinnedAction(ScheduledAction action, PinningEventLoopWorker parent) {
            this.action = action;
            this.parent = parent;
        }
    }
    static final class PinningThread extends Thread {
        final int index;
        final PinningEventLoopScheduler parent;
        final LinkedBlockingDeque<PinnedAction> queue;
        final ReentrantLock nonEmpty;
        final Condition hasItem;
        volatile boolean terminate;
        static final AtomicLong counter = new AtomicLong();

        public PinningThread(int index, PinningEventLoopScheduler parent) {
            super("RxPinningThread-" + counter.incrementAndGet());
            this.index = index;
            this.parent = parent;
            this.queue = new LinkedBlockingDeque<PinnedAction>();
            this.nonEmpty = new ReentrantLock();
            this.hasItem = nonEmpty.newCondition();
        }
        
        @Override
        public void run() {
            List<PinnedAction> actions = new ArrayList<PinnedAction>();
            while (!terminate) {
                try {
                    awaitNonEmpty();
                } catch (InterruptedException ex) {
                    // ignored
                }
                actions.clear();
                takeActions(queue, actions);
                executeAll(actions);
                actions.clear();
                if (queue.isEmpty()) {
                    for (int i = 0; i < parent.threads.length; i++) {
                        if (i != index) {
                            if (stealActions(parent.threads[i])) {
                                break;
                            }
                        }
                    }
                }
            }
        }
        void executeAll(Iterable<PinnedAction> actions) {
            for (PinnedAction pa : actions) {
                if (terminate) {
                    return;
                }
                execute(pa);
            }
        }
        void takeActions(Queue<PinnedAction> queue, List<PinnedAction> out) {
            Iterator<PinnedAction> it = queue.iterator();
            while (it.hasNext()) {
                PinnedAction pa = it.next();
                int pi = pa.parent.pinned;
                if (pi == index || pa.parent.casPinned(UNPINNED, index)) {
                    it.remove();
                    out.add(pa);
                }
            }
        }
        boolean stealActions(PinningThread other) {
            Iterator<PinnedAction> it = other.queue.iterator();
            while (it.hasNext()) {
                PinnedAction pa = it.next();
                int pi = pa.parent.pinned;
                if (pi == UNPINNED && pa.parent.casPinned(UNPINNED, index)) {
                    steal(other, it, pa);
                    return true;
                }
            }            
            return false;
        }
        void steal(PinningThread other, Iterator<PinnedAction> it, PinnedAction pa) {
            WriteLock wl = pa.parent.stealWrite;
            List<PinnedAction> out = new ArrayList<PinnedAction>();
            wl.lock();
            try {
                it.remove();
                out.add(pa);
                while (it.hasNext()) {
                    pa = it.next();
                    if (pa.parent.pinned == index) {
                        it.remove();
                        out.add(pa);
                    }
                }
            } finally {
                wl.unlock();
            }
            // add to the front of the queue
            for (int i = out.size() - 1; i >= 0; i--) {
                this.queue.offerFirst(out.get(i));
            }
        }
        void execute(PinnedAction pa) {
            if (!pa.once || pa.action.isUnsubscribed()) {
                return;
            }
            pa.once = false;
            try {
                try {
                    pa.action.run();
                } catch (Throwable t) {
                    // ignored
                }
            } finally {
                // clear interrupt flag
                if (Thread.currentThread().isInterrupted()) {
                    Thread.interrupted();
                }
            }
        }
        
        public void offer(PinnedAction pa) {
            queue.offer(pa);
            synchronized (this) {
                notify();
            }
        }
        public void awaitNonEmpty() throws InterruptedException {
            synchronized (this) {
                while (queue.isEmpty()) {
                    wait();
                }
            }
        }
        public void terminate() {
            terminate = true;
            interrupt();
        }
    }
}
