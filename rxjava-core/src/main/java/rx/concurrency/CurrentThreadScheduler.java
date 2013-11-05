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
package rx.concurrency;

import java.util.PriorityQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import rx.Scheduler;
import rx.Subscription;
import rx.util.functions.Func2;

/**
 * Schedules work on the current thread but does not execute immediately. Work is put in a queue and executed after the current unit of work is completed.
 */
public class CurrentThreadScheduler extends Scheduler {
    private static final CurrentThreadScheduler INSTANCE = new CurrentThreadScheduler();

    public static CurrentThreadScheduler getInstance() {
        return INSTANCE;
    }

    private static final ThreadLocal<PriorityQueue<TimedAction>> QUEUE = new ThreadLocal<PriorityQueue<TimedAction>>();

    /* package accessible for unit tests */CurrentThreadScheduler() {
    }

    private final AtomicInteger counter = new AtomicInteger(0);

    @Override
    public <T> Subscription schedule(T state, Func2<? super Scheduler, ? super T, ? extends Subscription> action) {
        DiscardableAction<T> discardableAction = new DiscardableAction<T>(state, action);
        enqueue(discardableAction, now());
        return discardableAction;
    }

    @Override
    public <T> Subscription schedule(T state, Func2<? super Scheduler, ? super T, ? extends Subscription> action, long dueTime, TimeUnit unit) {
        long execTime = now() + unit.toMillis(dueTime);

        DiscardableAction<T> discardableAction = new DiscardableAction<T>(state, new SleepingAction<T>(action, this, execTime));
        enqueue(discardableAction, execTime);
        return discardableAction;
    }

    private void enqueue(DiscardableAction<?> action, long execTime) {
        PriorityQueue<TimedAction> queue = QUEUE.get();
        boolean exec = queue == null;

        if (exec) {
            queue = new PriorityQueue<TimedAction>();
            QUEUE.set(queue);
        }

        queue.add(new TimedAction(action, execTime, counter.incrementAndGet()));

        if (exec) {
            while (!queue.isEmpty()) {
                queue.poll().action.call(this);
            }

            QUEUE.set(null);
        }
    }

    private static class TimedAction implements Comparable<TimedAction> {
        final DiscardableAction<?> action;
        final Long execTime;
        final Integer count; // In case if time between enqueueing took less than 1ms

        private TimedAction(DiscardableAction<?> action, Long execTime, Integer count) {
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
