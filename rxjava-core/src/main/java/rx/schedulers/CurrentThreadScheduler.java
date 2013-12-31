/**
 * Copyright 2013 Netflix, Inc.
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
import rx.subscriptions.CompositeSubscription;
import rx.subscriptions.MultipleAssignmentSubscription;
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

    private static final ThreadLocal<PriorityQueue<TimedAction>> QUEUE = new ThreadLocal<PriorityQueue<TimedAction>>();

    /* package accessible for unit tests */CurrentThreadScheduler() {
    }

    @Override
    public <T> Subscription schedule(T state, Func2<? super Scheduler, ? super T, ? extends Subscription> action) {
        // immediately move to the InnerCurrentThreadScheduler
        InnerCurrentThreadScheduler innerScheduler = new InnerCurrentThreadScheduler();
        DiscardableAction<T> discardableAction = new DiscardableAction<T>(state, action);
        enqueue(innerScheduler, discardableAction, now());
        return innerScheduler;
    }

    @Override
    public <T> Subscription schedule(T state, Func2<? super Scheduler, ? super T, ? extends Subscription> action, long dueTime, TimeUnit unit) {
        long execTime = now() + unit.toMillis(dueTime);

        // immediately move to the InnerCurrentThreadScheduler
        InnerCurrentThreadScheduler innerScheduler = new InnerCurrentThreadScheduler();
        DiscardableAction<T> discardableAction = new DiscardableAction<T>(state, new SleepingAction<T>(action, this, execTime));
        enqueue(innerScheduler, discardableAction, execTime);
        return discardableAction;
    }

    private static void enqueue(Scheduler scheduler, DiscardableAction<?> action, long execTime) {
        PriorityQueue<TimedAction> queue = QUEUE.get();
        boolean exec = queue == null;

        if (exec) {
            queue = new PriorityQueue<TimedAction>();
            QUEUE.set(queue);
        }

        queue.add(new TimedAction(action, execTime, counter.incrementAndGet()));

        if (exec) {
            while (!queue.isEmpty()) {
                queue.poll().action.call(scheduler);
            }

            QUEUE.set(null);
        }
    }

    private static class InnerCurrentThreadScheduler extends Scheduler implements Subscription {
        private final MultipleAssignmentSubscription childSubscription = new MultipleAssignmentSubscription();

        @Override
        public <T> Subscription schedule(T state, Func2<? super Scheduler, ? super T, ? extends Subscription> action) {
            DiscardableAction<T> discardableAction = new DiscardableAction<T>(state, action);
            childSubscription.set(discardableAction);
            enqueue(this, discardableAction, now());
            return childSubscription;
        }

        @Override
        public <T> Subscription schedule(T state, Func2<? super Scheduler, ? super T, ? extends Subscription> action, long delayTime, TimeUnit unit) {
            long execTime = now() + unit.toMillis(delayTime);

            DiscardableAction<T> discardableAction = new DiscardableAction<T>(state, action);
            childSubscription.set(discardableAction);
            enqueue(this, discardableAction, execTime);
            return childSubscription;
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
        final DiscardableAction<?> action;
        final Long execTime;
        final Long count; // In case if time between enqueueing took less than 1ms

        private TimedAction(DiscardableAction<?> action, Long execTime, Long count) {
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
