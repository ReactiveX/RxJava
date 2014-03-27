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
import java.util.concurrent.atomic.AtomicInteger;

import rx.Scheduler;
import rx.Subscription;
import rx.functions.Action1;
import rx.subscriptions.BooleanSubscription;

/**
 * Schedules work on the current thread but does not execute immediately. Work is put in a queue and executed after the current unit of work is completed.
 */
public class TrampolineScheduler extends Scheduler {
    private static final TrampolineScheduler INSTANCE = new TrampolineScheduler();

    /**
     * @deprecated Use Schedulers.trampoline();
     * @return
     */
    @Deprecated
    public static TrampolineScheduler getInstance() {
        return INSTANCE;
    }
    
    /* package */ static TrampolineScheduler instance() {
        return INSTANCE;
    }

    @Override
    public Subscription schedule(Action1<Scheduler.Inner> action) {
        InnerCurrentThreadScheduler inner = new InnerCurrentThreadScheduler();
        inner.schedule(action);
        return inner.innerSubscription;
    }

    @Override
    public Subscription schedule(Action1<Inner> action, long delayTime, TimeUnit unit) {
        InnerCurrentThreadScheduler inner = new InnerCurrentThreadScheduler();
        inner.schedule(action, delayTime, unit);
        return inner.innerSubscription;
    }

    /* package accessible for unit tests */TrampolineScheduler() {
    }

    private static final ThreadLocal<PriorityQueue<TimedAction>> QUEUE = new ThreadLocal<PriorityQueue<TimedAction>>();

    private final AtomicInteger counter = new AtomicInteger(0);

    private class InnerCurrentThreadScheduler extends Scheduler.Inner implements Subscription {

        private final BooleanSubscription innerSubscription = new BooleanSubscription();

        @Override
        public void schedule(Action1<Scheduler.Inner> action) {
            enqueue(action, now());
        }

        @Override
        public void schedule(Action1<Scheduler.Inner> action, long delayTime, TimeUnit unit) {
            long execTime = now() + unit.toMillis(delayTime);

            enqueue(new SleepingAction(action, TrampolineScheduler.this, execTime), execTime);
        }

        private void enqueue(Action1<Scheduler.Inner> action, long execTime) {
            if (innerSubscription.isUnsubscribed()) {
                return;
            }
            PriorityQueue<TimedAction> queue = QUEUE.get();
            boolean exec = queue == null;

            if (exec) {
                queue = new PriorityQueue<TimedAction>();
                QUEUE.set(queue);
            }

            queue.add(new TimedAction(action, execTime, counter.incrementAndGet()));

            if (exec) {
                while (!queue.isEmpty()) {
                    if (innerSubscription.isUnsubscribed()) {
                        return;
                    }
                    queue.poll().action.call(this);
                }

                QUEUE.set(null);
            }
        }

        @Override
        public void unsubscribe() {
            QUEUE.set(null); // this assumes we are calling unsubscribe from the same thread
            innerSubscription.unsubscribe();
        }

        @Override
        public boolean isUnsubscribed() {
            return innerSubscription.isUnsubscribed();
        }

    }

    private static class TimedAction implements Comparable<TimedAction> {
        final Action1<Scheduler.Inner> action;
        final Long execTime;
        final Integer count; // In case if time between enqueueing took less than 1ms

        private TimedAction(Action1<Scheduler.Inner> action, Long execTime, Integer count) {
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
