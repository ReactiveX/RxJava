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
import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;
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
import rx.subscriptions.CompositeSubscription;
import rx.subscriptions.MultipleAssignmentSubscription;
import rx.subscriptions.SubscriptionQueue;
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
        final CompositeSubscription delayed = new CompositeSubscription();
        final SubscriptionQueue direct = new SubscriptionQueue();
        final PinningEventLoopScheduler parent;
        final int initialIndex;
        volatile int pinned = UNPINNED;
        final ReentrantReadWriteLock stealLock;
        final ReentrantReadWriteLock.ReadLock stealRead;
        final ReentrantReadWriteLock.WriteLock stealWrite;
        volatile boolean unsubscribed;
        
        public PinningEventLoopWorker(PinningEventLoopScheduler parent, int initialIndex) {
            this.parent = parent;
            this.initialIndex = initialIndex;
            this.stealLock = new ReentrantReadWriteLock();
            this.stealRead = stealLock.readLock();
            this.stealWrite = stealLock.writeLock();
        }
        
        @Override
        public Subscription schedule(Action0 action) {
            if (isUnsubscribed()) {
                return Subscriptions.empty();
            }
            PinnedAction pa = new PinnedAction(action, this);
            direct.add(pa);
            pa.setUnsubscriber(direct.createDequeuer(pa));
            
            schedule(pa);
            
            return pa;
        }

        @Override
        public Subscription schedule(Action0 action, long delayTime, TimeUnit unit) {
            if (delayTime <= 0) {
                return schedule(action);
            }
            if (isUnsubscribed()) {
                return Subscriptions.empty();
            }
            final PinnedAction pa = new PinnedAction(action, this);
            delayed.add(pa);
            pa.setUnsubscriber(new Remover(pa, delayed));
            
            final MultipleAssignmentSubscription mas = new MultipleAssignmentSubscription();
            
            Future<?> f = GenericScheduledExecutorService.getInstance().schedule(new Runnable() {

                @Override
                public void run() {
                    mas.set(pa);
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
            unsubscribed = true;
            delayed.unsubscribe();
            direct.unsubscribe();
        }

        @Override
        public boolean isUnsubscribed() {
            return unsubscribed;
        }
        public boolean casPinned(int expected, int newValue) {
            return PINNED_UPDATER.compareAndSet(this, expected, newValue);
        }
    }
    static final class PinnedAction implements Action0, Subscription {
        final Action0 action;
        final PinningEventLoopWorker parent;
        volatile boolean unsubscribed;
        final MultipleAssignmentSubscription mas;

        public PinnedAction(Action0 action, PinningEventLoopWorker parent) {
            this.action = action;
            this.parent = parent;
            this.mas = new MultipleAssignmentSubscription();
        }

        @Override
        public void call() {
            if (!unsubscribed) {
                try {
                    action.call();
                } finally {
                    unsubscribe();
                }
            }
        }

        @Override
        public boolean isUnsubscribed() {
            return unsubscribed;
        }

        @Override
        public void unsubscribe() {
            unsubscribed = true;
            mas.unsubscribe();
        }
        public void setUnsubscriber(Subscription s) {
            mas.set(s);
        }
    }
    /** Remove a child subscription from a composite when unsubscribing. */
    static final class Remover implements Subscription {
        final Subscription s;
        final CompositeSubscription parent;
        volatile boolean once;

        public Remover(Subscription s, CompositeSubscription parent) {
            this.s = s;
            this.parent = parent;
        }

        @Override
        public boolean isUnsubscribed() {
            return once;
        }

        @Override
        public void unsubscribe() {
            if (!once) {
                once = true;
                parent.remove(s);
            }
        }

    }
    static final class PinningThread extends Thread {
        final int index;
        final PinningEventLoopScheduler parent;
        final Queue<PinnedAction> queue;
        final ReentrantLock lock;
        final Condition condition;
        final Random random;
        final int otherCount;
        volatile boolean terminate;
        static final AtomicLong counter = new AtomicLong();
        static final long PARK_TIME = 1000;

        public PinningThread(int index, PinningEventLoopScheduler parent) {
            super("RxPinningThread-" + counter.incrementAndGet());
            this.index = index;
            this.parent = parent;
            //this.queue = new ConcurrentLinkedQueue<PinnedAction>();
            this.queue = new ConcurrentLinkedQueue<PinnedAction>();
            this.lock = new ReentrantLock();
            this.condition = lock.newCondition();
            this.random = new Random();
            this.otherCount = parent.threads.length - 1;
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
                takeActions();
                if (queue.isEmpty() && otherCount > 0) {
                    actions.clear();
                    int k = random.nextInt(otherCount);
                    if (k >= index) {
                        k++;
                    }
                    stealActions(parent.threads[k], actions);
                    executeAll(actions);
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
        void takeActions() {
            Iterator<PinnedAction> it = queue.iterator();
            while (it.hasNext()) {
                PinnedAction pa = it.next();
                int pi = pa.parent.pinned;
                if (pi == index || (pi == UNPINNED && pa.parent.casPinned(UNPINNED, index))) {
                    it.remove();
                    
                    execute(pa);
                }
            }
        }
        void stealActions(PinningThread other, List<PinnedAction> out) {
            Iterator<PinnedAction> it = other.queue.iterator();
            while (it.hasNext()) {
                PinnedAction pa = it.next();
                int pi = pa.parent.pinned;
                if (pi == UNPINNED && pa.parent.casPinned(UNPINNED, index)) {
                    steal(other, it, pa, out);
                    return;
                }
            }            
        }
        void steal(PinningThread other, Iterator<PinnedAction> it, PinnedAction pa, List<PinnedAction> out) {
            WriteLock wl = pa.parent.stealWrite;
            PinningEventLoopWorker pw = pa.parent;
            wl.lock();
            try {
                it.remove();
                out.add(pa);
                while (it.hasNext()) {
                    pa = it.next();
                    if (pa.parent == pw) {
                        it.remove();
                        out.add(pa);
                    }
                }
            } finally {
                wl.unlock();
            }
        }
        void execute(PinnedAction pa) {
            try {
                try {
                    pa.call();
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
            lock.lock();
            try {
                if (!queue.isEmpty()) {
                    condition.signal();
                }
            } finally {
                lock.unlock();
            }
        }
        public void awaitNonEmpty() throws InterruptedException {
            lock.lock();
            try {
                if (queue.isEmpty()) {
                    condition.awaitNanos(PARK_TIME);
                }
            } finally {
                lock.unlock();
            }
        }
        public void terminate() {
            terminate = true;
            interrupt();
        }
    }
}
