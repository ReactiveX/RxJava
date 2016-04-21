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
package rx.internal.schedulers;

import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import rx.Scheduler;
import rx.Subscription;
import rx.functions.Action0;
import rx.subscriptions.BooleanSubscription;
import rx.subscriptions.Subscriptions;

/**
 * Schedules work on the current thread but does not execute immediately. Work is put in a queue and executed
 * after the current unit of work is completed.
 */
public final class TrampolineScheduler extends Scheduler {
    public static final TrampolineScheduler INSTANCE = new TrampolineScheduler();

    @Override
    public Worker createWorker() {
        return new InnerCurrentThreadScheduler();
    }

    private TrampolineScheduler() {
    }

    private static class InnerCurrentThreadScheduler extends Scheduler.Worker implements Subscription {

        final AtomicInteger counter = new AtomicInteger();
        final PriorityBlockingQueue<TimedAction> queue = new PriorityBlockingQueue<TimedAction>();
        private final BooleanSubscription innerSubscription = new BooleanSubscription();
        private final AtomicInteger wip = new AtomicInteger();

        InnerCurrentThreadScheduler() {
        }

        @Override
        public Subscription schedule(Action0 action) {
            return enqueue(action, now());
        }

        @Override
        public Subscription schedule(Action0 action, long delayTime, TimeUnit unit) {
            long execTime = now() + unit.toMillis(delayTime);

            return enqueue(new SleepingAction(action, this, execTime), execTime);
        }

        private Subscription enqueue(Action0 action, long execTime) {
            if (innerSubscription.isUnsubscribed()) {
                return Subscriptions.unsubscribed();
            }
            final TimedAction timedAction = new TimedAction(action, execTime, counter.incrementAndGet());
            queue.add(timedAction);

            if (wip.getAndIncrement() == 0) {
                do {
                    final TimedAction polled = queue.poll();
                    if (polled != null) {
                        polled.action.call();
                    }
                } while (wip.decrementAndGet() > 0);
                return Subscriptions.unsubscribed();
            } else {
                // queue wasn't empty, a parent is already processing so we just add to the end of the queue
                return Subscriptions.create(new Action0() {

                    @Override
                    public void call() {
                        queue.remove(timedAction);
                    }

                });
            }
        }

        @Override
        public void unsubscribe() {
            innerSubscription.unsubscribe();
        }

        @Override
        public boolean isUnsubscribed() {
            return innerSubscription.isUnsubscribed();
        }

    }

    private static final class TimedAction implements Comparable<TimedAction> {
        final Action0 action;
        final Long execTime;
        final int count; // In case if time between enqueueing took less than 1ms

        TimedAction(Action0 action, Long execTime, int count) {
            this.action = action;
            this.execTime = execTime;
            this.count = count;
        }

        @Override
        public int compareTo(TimedAction that) {
            int result = execTime.compareTo(that.execTime);
            if (result == 0) {
                return compare(count, that.count);
            }
            return result;
        }
    }

    // because I can't use Integer.compare from Java 7
    static int compare(int x, int y) {
        return (x < y) ? -1 : ((x == y) ? 0 : 1);
    }

}
