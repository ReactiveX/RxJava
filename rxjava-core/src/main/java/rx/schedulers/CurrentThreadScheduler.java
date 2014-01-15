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

import java.util.PriorityQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import rx.Scheduler;
import rx.Subscription;
import rx.subscriptions.MultipleAssignmentSubscription;
import rx.util.functions.Func1;
import rx.util.functions.Func2;

/**
 * Schedules work on the current thread but does not execute immediately. Work is put in a queue and executed after the current unit of work is completed.
 */
public class CurrentThreadScheduler extends Scheduler {
    private static final CurrentThreadScheduler INSTANCE = new CurrentThreadScheduler();
    private static final AtomicLong counter = new AtomicLong(0);

    public static CurrentThreadScheduler getInstance() {
        return INSTANCE;
    }

    private static final ThreadLocal<PriorityQueue<TimedAction>> QUEUE = new ThreadLocal<PriorityQueue<TimedAction>>() {
        protected java.util.PriorityQueue<TimedAction> initialValue() {
            return new PriorityQueue<TimedAction>();
        };
    };

    private static final ThreadLocal<Boolean> PROCESSING = new ThreadLocal<Boolean>() {
        protected Boolean initialValue() {
            return Boolean.FALSE;
        };
    };

    /* package accessible for unit tests */CurrentThreadScheduler() {
    }

    @Override
    public <T> Subscription schedule(T state, Func2<? super Scheduler, ? super T, ? extends Subscription> action) {
        // immediately move to the InnerCurrentThreadScheduler
        InnerCurrentThreadScheduler innerScheduler = new InnerCurrentThreadScheduler();
        innerScheduler.schedule(state, action);
        enqueueFromOuter(innerScheduler, now());
        return innerScheduler;
    }

    @Override
    public <T> Subscription schedule(T state, Func2<? super Scheduler, ? super T, ? extends Subscription> action, long delayTime, TimeUnit unit) {
        long execTime = now() + unit.toMillis(delayTime);

        // create an inner scheduler and queue it for execution
        InnerCurrentThreadScheduler innerScheduler = new InnerCurrentThreadScheduler();
        innerScheduler.schedule(state, action, delayTime, unit);
        enqueueFromOuter(innerScheduler, execTime);
        return innerScheduler;
    }

    /*
     * This will accept InnerCurrentThreadScheduler instances and execute them in order they are received
     * and on each of them will loop internally until each is complete.
     */
    private void enqueueFromOuter(final InnerCurrentThreadScheduler innerScheduler, long execTime) {
        // Note that everything here is single-threaded so we won't have race conditions
        PriorityQueue<TimedAction> queue = QUEUE.get();
        queue.add(new TimedAction(new Func1<Scheduler, Subscription>() {

            @Override
            public Subscription call(Scheduler _) {
                // when the InnerCurrentThreadScheduler gets scheduled we want to process its tasks
                return innerScheduler.startProcessing();
            }
        }, execTime, counter.incrementAndGet()));

        // first time through starts the loop
        if (!PROCESSING.get()) {
            PROCESSING.set(Boolean.TRUE);
            while (!queue.isEmpty()) {
                queue.poll().action.call(innerScheduler);
            }
            PROCESSING.set(Boolean.FALSE);
        }
    }

    private static class InnerCurrentThreadScheduler extends Scheduler implements Subscription {
        private final MultipleAssignmentSubscription childSubscription = new MultipleAssignmentSubscription();
        private final PriorityQueue<TimedAction> innerQueue = new PriorityQueue<TimedAction>();

        @Override
        public <T> Subscription schedule(T state, Func2<? super Scheduler, ? super T, ? extends Subscription> action) {
            DiscardableAction<T> discardableAction = new DiscardableAction<T>(state, action);
            childSubscription.set(discardableAction);
            enqueue(discardableAction, now());
            return childSubscription;
        }

        @Override
        public <T> Subscription schedule(T state, Func2<? super Scheduler, ? super T, ? extends Subscription> action, long delayTime, TimeUnit unit) {
            long execTime = now() + unit.toMillis(delayTime);

            DiscardableAction<T> discardableAction = new DiscardableAction<T>(state, new SleepingAction<T>(action, this, execTime));
            childSubscription.set(discardableAction);
            enqueue(discardableAction, execTime);
            return childSubscription;
        }

        private void enqueue(Func1<Scheduler, Subscription> action, long execTime) {
            innerQueue.add(new TimedAction(action, execTime, counter.incrementAndGet()));
        }

        private Subscription startProcessing() {
            while (!innerQueue.isEmpty()) {
                innerQueue.poll().action.call(this);
            }
            return this;
        }

        @Override
        public void unsubscribe() {
            childSubscription.unsubscribe();
        }

    }

    /**
     * Use time to sort items so delayed actions are sorted to their appropriate position in the queue.
     */
    private static class TimedAction implements Comparable<TimedAction> {
        final Func1<Scheduler, Subscription> action;
        final Long execTime;
        final Long count; // In case if time between enqueueing took less than 1ms

        private TimedAction(Func1<Scheduler, Subscription> action, Long execTime, Long count) {
            this.action = action;
            this.execTime = execTime;
            this.count = count;
        }

        @Override
        public int compareTo(TimedAction that) {
            int result = execTime.compareTo(that.execTime);
            if (result == 0) {
                return count.compareTo(that.count);
            }
            return result;
        }
    }
}
