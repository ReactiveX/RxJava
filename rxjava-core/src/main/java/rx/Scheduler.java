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
package rx;

import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;

import rx.concurrency.TestScheduler;
import rx.subscriptions.Subscriptions;
import rx.util.functions.Action0;
import rx.util.functions.Func0;
import rx.util.functions.Func1;
import rx.util.functions.Func2;

/**
 * Represents an object that schedules units of work.
 * <p>
 * The methods left to implement are:
 * <ul>
 * <li>{@code <T> Subscription schedule(T state, Func2<Scheduler, T, Subscription> action, long delayTime, TimeUnit unit)}</li>
 * <li>{@code <T> Subscription schedule(T state, Func2<Scheduler, T, Subscription> action)}</li>
 * </ul>
 * <p>
 * Why is this an abstract class instead of an interface?
 * <p>
 * <ol>
 * <li>Java doesn't support extension methods and there are many overload methods needing default implementations.</li>
 * <li>Virtual extension methods aren't available until Java8 which RxJava will not set as a minimum target for a long time.</li>
 * <li>If only an interface were used Scheduler implementations would then need to extend from an AbstractScheduler pair that gives all of the functionality unless they intend on copy/pasting the
 * functionality.</li>
 * <li>Without virtual extension methods even additive changes are breaking and thus severely impede library maintenance.</li>
 * </ol>
 */
public abstract class Scheduler {

    /**
     * Schedules a cancelable action to be executed.
     * 
     * @param state
     *            State to pass into the action.
     * @param action
     *            Action to schedule.
     * @return a subscription to be able to unsubscribe from action.
     */
    public abstract <T> Subscription schedule(T state, Func2<Scheduler, T, Subscription> action);

    /**
     * Schedules a cancelable action to be executed in delayTime.
     * 
     * @param state
     *            State to pass into the action.
     * @param action
     *            Action to schedule.
     * @param delayTime
     *            Time the action is to be delayed before executing.
     * @param unit
     *            Time unit of the delay time.
     * @return a subscription to be able to unsubscribe from action.
     */
    public abstract <T> Subscription schedule(T state, Func2<Scheduler, T, Subscription> action, long delayTime, TimeUnit unit);

    /**
     * Schedules a cancelable action to be executed periodically.
     * This default implementation schedules recursively and waits for actions to complete (instead of potentially executing
     * long-running actions concurrently). Each scheduler that can do periodic scheduling in a better way should override this.
     *
     * @param state 
     *            State to pass into the action.
     * @param action 
     *            The action to execute periodically.
     * @param initialDelay 
     *            Time to wait before executing the action for the first time.
     * @param period 
     *            The time interval to wait each time in between executing the action.
     * @param unit 
     *            The time unit the interval above is given in.
     * @return A subscription to be able to unsubscribe from action.
     */
    public <T> Subscription schedulePeriodically(T state, final Func2<Scheduler, T, Subscription> action, long initialDelay, long period, TimeUnit unit) {
        final long periodInNanos = unit.toNanos(period);
        final AtomicBoolean complete = new AtomicBoolean();
  
        final Func2<Scheduler, T, Subscription> recursiveAction = new Func2<Scheduler, T, Subscription>() {
            @Override
            public Subscription call(Scheduler scheduler, T state0) {
                if (! complete.get()) {
                    long startedAt = now();
                    final Subscription sub1 = action.call(scheduler, state0);
                    long timeTakenByActionInNanos = TimeUnit.MILLISECONDS.toNanos(now() - startedAt);
                    final Subscription sub2 = schedule(state0, this, periodInNanos - timeTakenByActionInNanos, TimeUnit.NANOSECONDS);
                    return Subscriptions.create(new Action0() {
                        @Override
                        public void call() {
                            sub1.unsubscribe();
                            sub2.unsubscribe();
                        }
                    });
                }
                return Subscriptions.empty();
            }
        };
        final Subscription sub = schedule(state, recursiveAction, initialDelay, unit);
        return Subscriptions.create(new Action0() {
            @Override
            public void call() {
                complete.set(true);
                sub.unsubscribe();
            }
        });
    }
    
    /**
     * Schedules a cancelable action to be executed at dueTime.
     * 
     * @param state
     *            State to pass into the action.
     * @param action
     *            Action to schedule.
     * @param dueTime
     *            Time the action is to be executed. If in the past it will be executed immediately.
     * @return a subscription to be able to unsubscribe from action.
     */
    public <T> Subscription schedule(T state, Func2<Scheduler, T, Subscription> action, Date dueTime) {
        long scheduledTime = dueTime.getTime();
        long timeInFuture = scheduledTime - now();
        if (timeInFuture <= 0) {
            return schedule(state, action);
        } else {
            return schedule(state, action, timeInFuture, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * Schedules a cancelable action to be executed.
     * 
     * @param action
     *            Action to schedule.
     * @return a subscription to be able to unsubscribe from action.
     */
    public Subscription schedule(final Func1<Scheduler, Subscription> action) {
        return schedule(null, new Func2<Scheduler, Void, Subscription>() {

            @Override
            public Subscription call(Scheduler scheduler, @SuppressWarnings("unused") Void state) {
                return action.call(scheduler);
            }
        });
    }

    /**
     * Schedules a cancelable action to be executed.
     * 
     * @param action
     *            action
     * @return a subscription to be able to unsubscribe from action.
     */
    public Subscription schedule(final Func0<Subscription> action) {
        return schedule(null, new Func2<Scheduler, Void, Subscription>() {

            @Override
            public Subscription call(@SuppressWarnings("unused") Scheduler scheduler, @SuppressWarnings("unused") Void state) {
                return action.call();
            }
        });
    }

    /**
     * Schedules an action to be executed.
     * 
     * @param action
     *            action
     * @return a subscription to be able to unsubscribe from action.
     */
    public Subscription schedule(final Action0 action) {
        return schedule(null, new Func2<Scheduler, Void, Subscription>() {

            @Override
            public Subscription call(@SuppressWarnings("unused") Scheduler scheduler, @SuppressWarnings("unused") Void state) {
                action.call();
                return Subscriptions.empty();
            }
        });
    }

    /**
     * Schedules a cancelable action to be executed in delayTime.
     * 
     * @param action
     *            Action to schedule.
     * @param delayTime
     *            Time the action is to be delayed before executing.
     * @param unit
     *            Time unit of the delay time.
     * @return a subscription to be able to unsubscribe from action.
     */
    public Subscription schedule(final Func1<Scheduler, Subscription> action, long delayTime, TimeUnit unit) {
        return schedule(null, new Func2<Scheduler, Void, Subscription>() {

            @Override
            public Subscription call(Scheduler scheduler, @SuppressWarnings("unused") Void state) {
                return action.call(scheduler);
            }
        }, delayTime, unit);
    }

    /**
     * Schedules an action to be executed in delayTime.
     * 
     * @param action
     *            action
     * @return a subscription to be able to unsubscribe from action.
     */
    public Subscription schedule(final Action0 action, long delayTime, TimeUnit unit) {
        return schedule(null, new Func2<Scheduler, Void, Subscription>() {

            @Override
            public Subscription call(@SuppressWarnings("unused") Scheduler scheduler, @SuppressWarnings("unused") Void state) {
                action.call();
                return Subscriptions.empty();
            }
        }, delayTime, unit);
    }

    /**
     * Schedules a cancelable action to be executed in delayTime.
     * 
     * @param action
     *            action
     * @return a subscription to be able to unsubscribe from action.
     */
    public Subscription schedule(final Func0<Subscription> action, long delayTime, TimeUnit unit) {
        return schedule(null, new Func2<Scheduler, Void, Subscription>() {

            @Override
            public Subscription call(@SuppressWarnings("unused") Scheduler scheduler, @SuppressWarnings("unused") Void state) {
                return action.call();
            }
        }, delayTime, unit);
    }

    /**
     * Schedules a cancelable action to be executed periodically.
     * 
     * @param action 
     *            The action to execute periodically.
     * @param initialDelay 
     *            Time to wait before executing the action for the first time.
     * @param period 
     *            The time interval to wait each time in between executing the action.
     * @param unit 
     *            The time unit the interval above is given in.
     * @return A subscription to be able to unsubscribe from action.
     */
    public Subscription schedulePeriodically(final Func1<Scheduler, Subscription> action, long initialDelay, long period, TimeUnit unit) {
        return schedulePeriodically(null, new Func2<Scheduler, Void, Subscription>() {
            @Override
            public Subscription call(Scheduler scheduler, @SuppressWarnings("unused") Void state) {
                return action.call(scheduler);
            }
        }, initialDelay, period, unit);
    }

    /**
     * Schedules a cancelable action to be executed periodically.
     * 
     * @param action 
     *            The action to execute periodically.
     * @param initialDelay 
     *            Time to wait before executing the action for the first time.
     * @param period 
     *            The time interval to wait each time in between executing the action.
     * @param unit 
     *            The time unit the interval above is given in.
     * @return A subscription to be able to unsubscribe from action.
     */
    public Subscription schedulePeriodically(final Func0<Subscription> action, long initialDelay, long period, TimeUnit unit) {
        return schedulePeriodically(null, new Func2<Scheduler, Void, Subscription>() {
            @Override
            public Subscription call(@SuppressWarnings("unused") Scheduler scheduler, @SuppressWarnings("unused") Void state) {
                return action.call();
            }
        }, initialDelay, period, unit);
    }

    /**
     * Schedules an action to be executed periodically.
     * 
     * @param action 
     *            The action to execute periodically.
     * @param initialDelay 
     *            Time to wait before executing the action for the first time.
     * @param period 
     *            The time interval to wait each time in between executing the action.
     * @param unit 
     *            The time unit the interval above is given in.
     * @return A subscription to be able to unsubscribe from action.
     */
    public Subscription schedulePeriodically(final Action0 action, long initialDelay, long period, TimeUnit unit) {
        return schedulePeriodically(null, new Func2<Scheduler, Void, Subscription>() {
            @Override
            public Subscription call(@SuppressWarnings("unused") Scheduler scheduler, @SuppressWarnings("unused") Void state) {
                action.call();
                return Subscriptions.empty();
            }
        }, initialDelay, period, unit);
    }

    /**
     * Returns the scheduler's notion of current absolute time in milliseconds.
     */
    public long now() {
        return System.currentTimeMillis();
    }

    public static class UnitTest {
        @SuppressWarnings("unchecked") // mocking is unchecked, unfortunately
        @Test
        public void testPeriodicScheduling() {
            final Func1<Long, Void> calledOp = mock(Func1.class);
            
            final TestScheduler scheduler = new TestScheduler();
            scheduler.schedulePeriodically(new Action0() {
                @Override public void call() {
                    System.out.println(scheduler.now());
                    calledOp.call(scheduler.now());
                }
            }, 1, 2, TimeUnit.SECONDS);
            
            verify(calledOp, never()).call(anyLong());

            InOrder inOrder = Mockito.inOrder(calledOp);
            
            scheduler.advanceTimeBy(999L, TimeUnit.MILLISECONDS);
            inOrder.verify(calledOp, never()).call(anyLong());

            scheduler.advanceTimeBy(1L, TimeUnit.MILLISECONDS);
            inOrder.verify(calledOp, times(1)).call(1000L);
            
            scheduler.advanceTimeBy(1999L, TimeUnit.MILLISECONDS);
            inOrder.verify(calledOp, never()).call(3000L);
            
            scheduler.advanceTimeBy(1L, TimeUnit.MILLISECONDS);
            inOrder.verify(calledOp, times(1)).call(3000L);
            
            scheduler.advanceTimeBy(5L, TimeUnit.SECONDS);
            inOrder.verify(calledOp, times(1)).call(5000L);
            inOrder.verify(calledOp, times(1)).call(7000L);
        }
    }
}
