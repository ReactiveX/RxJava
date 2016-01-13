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

import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.TimeUnit;

import rx.Scheduler;
import rx.Subscription;
import rx.functions.Action0;
import rx.subscriptions.BooleanSubscription;
import rx.subscriptions.Subscriptions;

/**
 * The {@code TestScheduler} is useful for debugging. It allows you to test schedules of events by manually
 * advancing the clock at whatever pace you choose.
 */
public class TestScheduler extends Scheduler {
    final Queue<TimedAction> queue = new PriorityQueue<TimedAction>(11, new CompareActionsByTime());
    static long counter = 0;

    private static final class TimedAction {

        final long time;
        final Action0 action;
        final Worker scheduler;
        private final long count = counter++; // for differentiating tasks at same time

        TimedAction(Worker scheduler, long time, Action0 action) {
            this.time = time;
            this.action = action;
            this.scheduler = scheduler;
        }

        @Override
        public String toString() {
            return String.format("TimedAction(time = %d, action = %s)", time, action.toString());
        }
    }

    private static class CompareActionsByTime implements Comparator<TimedAction> {
        CompareActionsByTime() {
        }

        @Override
        public int compare(TimedAction action1, TimedAction action2) {
            if (action1.time == action2.time) {
                return action1.count < action2.count ? -1 : ((action1.count > action2.count) ? 1 : 0);
            } else {
                return action1.time < action2.time ? -1 : ((action1.time > action2.time) ? 1 : 0);
            }
        }
    }

    // Storing time in nanoseconds internally.
    long time;

    @Override
    public long now() {
        return TimeUnit.NANOSECONDS.toMillis(time);
    }

    /**
     * Moves the Scheduler's clock forward by a specified amount of time.
     *
     * @param delayTime
     *          the amount of time to move the Scheduler's clock forward
     * @param unit
     *          the units of time that {@code delayTime} is expressed in
     */
    public void advanceTimeBy(long delayTime, TimeUnit unit) {
        advanceTimeTo(time + unit.toNanos(delayTime), TimeUnit.NANOSECONDS);
    }

    /**
     * Moves the Scheduler's clock to a particular moment in time.
     *
     * @param delayTime
     *          the point in time to move the Scheduler's clock to
     * @param unit
     *          the units of time that {@code delayTime} is expressed in
     */
    public void advanceTimeTo(long delayTime, TimeUnit unit) {
        long targetTime = unit.toNanos(delayTime);
        triggerActions(targetTime);
    }

    /**
     * Triggers any actions that have not yet been triggered and that are scheduled to be triggered at or
     * before this Scheduler's present time.
     */
    public void triggerActions() {
        triggerActions(time);
    }

    private void triggerActions(long targetTimeInNanos) {
        while (!queue.isEmpty()) {
            TimedAction current = queue.peek();
            if (current.time > targetTimeInNanos) {
                break;
            }
            // if scheduled time is 0 (immediate) use current virtual time
            time = current.time == 0 ? time : current.time;
            queue.remove();

            // Only execute if not unsubscribed
            if (!current.scheduler.isUnsubscribed()) {
                current.action.call();
            }
        }
        time = targetTimeInNanos;
    }

    @Override
    public Worker createWorker() {
        return new InnerTestScheduler();
    }

    private final class InnerTestScheduler extends Worker {

        private final BooleanSubscription s = new BooleanSubscription();

        InnerTestScheduler() {
        }

        @Override
        public void unsubscribe() {
            s.unsubscribe();
        }

        @Override
        public boolean isUnsubscribed() {
            return s.isUnsubscribed();
        }

        @Override
        public Subscription schedule(Action0 action, long delayTime, TimeUnit unit) {
            final TimedAction timedAction = new TimedAction(this, time + unit.toNanos(delayTime), action);
            queue.add(timedAction);
            return Subscriptions.create(new Action0() {

                @Override
                public void call() {
                    queue.remove(timedAction);
                }

            });
        }

        @Override
        public Subscription schedule(Action0 action) {
            final TimedAction timedAction = new TimedAction(this, 0, action);
            queue.add(timedAction);
            return Subscriptions.create(new Action0() {

                @Override
                public void call() {
                    queue.remove(timedAction);
                }

            });
        }

        @Override
        public long now() {
            return TestScheduler.this.now();
        }

    }

}
