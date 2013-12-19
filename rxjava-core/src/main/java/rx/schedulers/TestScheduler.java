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

import rx.observables.TestableObservable;
import rx.observables.HotObservable;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import rx.Notification;
import rx.Observable;

import rx.Scheduler;
import rx.Subscription;
import rx.observables.ColdObservable;
import rx.observers.TestObserver;
import rx.observers.TestableObserver;
import rx.subscriptions.SingleAssignmentSubscription;
import rx.util.Recorded;
import rx.util.functions.Action0;
import rx.util.functions.Func0;
import rx.util.functions.Func2;

public class TestScheduler extends Scheduler {
    private final Queue<TimedAction<?>> queue = new PriorityQueue<TimedAction<?>>(11, new CompareActionsByTime());

    private static class TimedAction<T> {

        private final long time;
        private final Func2<? super Scheduler, ? super T, ? extends Subscription> action;
        private final T state;
        private final TestScheduler scheduler;
        private final AtomicBoolean isCancelled = new AtomicBoolean(false);

        private TimedAction(TestScheduler scheduler, long time, Func2<? super Scheduler, ? super T, ? extends Subscription> action, T state) {
            this.time = time;
            this.action = action;
            this.state = state;
            this.scheduler = scheduler;
        }

        public void cancel() {
            isCancelled.set(true);
        }

        @Override
        public String toString() {
            return String.format("TimedAction(time = %d, action = %s)", time, action.toString());
        }
    }

    private static class CompareActionsByTime implements Comparator<TimedAction<?>> {
        @Override
        public int compare(TimedAction<?> action1, TimedAction<?> action2) {
            return Long.valueOf(action1.time).compareTo(Long.valueOf(action2.time));
        }
    }

    // Storing time in nanoseconds internally.
    private long time;

    @Override
    public long now() {
        return TimeUnit.NANOSECONDS.toMillis(time);
    }

    public void advanceTimeBy(long delayTime, TimeUnit unit) {
        advanceTimeTo(time + unit.toNanos(delayTime), TimeUnit.NANOSECONDS);
    }

    public void advanceTimeTo(long delayTime, TimeUnit unit) {
        long targetTime = unit.toNanos(delayTime);
        triggerActions(targetTime);
    }

    public void triggerActions() {
        triggerActions(time);
    }

    @SuppressWarnings("unchecked")
    private void triggerActions(long targetTimeInNanos) {
        while (!queue.isEmpty()) {
            TimedAction<?> current = queue.peek();
            if (current.time > targetTimeInNanos) {
                break;
            }
            time = current.time;
            queue.remove();

            // Only execute if the TimedAction has not yet been cancelled
            if (!current.isCancelled.get()) {
                // because the queue can have wildcards we have to ignore the type T for the state
                ((Func2<Scheduler, Object, Subscription>) current.action).call(current.scheduler, current.state);
            }
        }
        time = targetTimeInNanos;
    }
    /**
     * Run the test scheduler until no more actions are queued.
     */
    @SuppressWarnings("unchecked")
    private void triggerAllActions() {
        while (!queue.isEmpty()) {
            TimedAction<?> current = queue.poll();
            time = current.time;
            if (!current.isCancelled.get()) {
                // because the queue can have wildcards we have to ignore the type T for the state
                ((Func2<Scheduler, Object, Subscription>) current.action).call(current.scheduler, current.state);
            }
        }
    }

    @Override
    public <T> Subscription schedule(T state, Func2<? super Scheduler, ? super T, ? extends Subscription> action) {
        return schedule(state, action, 0, TimeUnit.MILLISECONDS);
    }

    @Override
    public <T> Subscription schedule(T state, Func2<? super Scheduler, ? super T, ? extends Subscription> action, long delayTime, TimeUnit unit) {
        final TimedAction<T> timedAction = new TimedAction<T>(this, time + unit.toNanos(delayTime), action, state);
        queue.add(timedAction);

        return new Subscription() {
            @Override
            public void unsubscribe() {
                timedAction.cancel();
            }
        };
    }
    /**
     * Schedule a task with absolute time.
     * @param <T> the state type
     * @param state the state
     * @param timeInMillis the absolute time in milliseconds
     * @param action the action to schedule
     * @return the subscription to cancel the schedule
     */
    public <T> Subscription scheduleAbsolute(T state, 
            long timeInMillis, 
            Func2<? super Scheduler, ? super T, ? extends Subscription> action) {
        long delay = Math.max(0L, timeInMillis - now());
        return schedule(state, action, delay, TimeUnit.MILLISECONDS);
    }
    
    /**
     * Schedule a task with absolute time.
     * @param <T> the state type
     * @param timeInMillis the absolute time in milliseconds
     * @param action the action to schedule
     * @return the subscription to cancel the schedule
     */
    public <T> Subscription scheduleAbsolute(
            long timeInMillis, 
            Action0 action) {
        long delay = Math.max(0L, timeInMillis - now());
        return schedule(action, delay, TimeUnit.MILLISECONDS);
    }
    
    /**
     * Schedule a task with relative time.
     * @param <T> the state type
     * @param state the state
     * @param timeInMillis the relative time in milliseconds
     * @param action the action to schedule
     * @return the subscription to cancel the schedule
     */
    public <T> Subscription scheduleRelative(T state, 
            long timeInMillis, 
            Func2<? super Scheduler, ? super T, ? extends Subscription> action) {
        return schedule(state, action, timeInMillis, TimeUnit.MILLISECONDS);
    }
    
    /**
     * Schedule a task with relative time.
     * @param <T> the state type
     * @param timeInMillis the absolute time in milliseconds
     * @param action the action to schedule
     * @return the subscription to cancel the schedule
     */
    public <T> Subscription scheduleRelative(
            long timeInMillis, 
            Action0 action) {
        return schedule(action, timeInMillis, TimeUnit.MILLISECONDS);
    }
    
    /**
     * Creates a hot observable using the given timestamped notification messages.
     * @param <T> the value type
     * @param messages the messages
     * @return a testable observable
     */
    public <T> TestableObservable<T> createHotObservable(Recorded<Notification<T>>... messages) {
        return HotObservable.create(this, messages);
    }
    /**
     * Creates a hot observable using the given timestamped notification messages.
     * @param <T> the value type
     * @param messages the messages
     * @return a testable observable
     */
    public <T> TestableObservable<T> createHotObservable(Iterable<? extends Recorded<Notification<T>>> messages) {
        return HotObservable.create(this, messages);
    }
    /**
     * Creates a cold observable using the given timestamped notification messages.
     * @param <T> the value type
     * @param messages the messages
     * @return a testable observable
     */
    public <T> TestableObservable<T> createColdObservable(Recorded<Notification<T>>... messages) {
        return ColdObservable.create(this, messages);
    }
    /**
     * Creates a cold observable using the given timestamped notification messages.
     * @param <T> the value type
     * @param messages the messages
     * @return a testable observable
     */
    public <T> TestableObservable<T> createColdObservable(Iterable<? extends Recorded<Notification<T>>> messages) {
        return ColdObservable.create(this, messages);
    }
    /**
     * Creates an observer that records received notification messages and timestamps of those.
     * @param <T> the observed value type
     * @return an observer with recorded notifications
     */
    public <T> TestableObserver<T> createObserver() {
        return new TestObserver<T>(this);
    }
    /**
     * Creates an observer that records received notification messages and timestamps of those,
     * with the help of a type witness.
     * @param <T> the observed value type
     * @param typeWitness the type example to help the compiler infer types
     * @return an observer with recorded notifications
     */
    public <T> TestableObserver<T> createObserver(T typeWitness) {
        return new TestObserver<T>(this);
    }
    
    /**
     * Runs the test scheduler and uses the specified virtual times to
     * invoke the factory function, subscribe to it and then unsubscribe.
     * @param <T> the result value type
     * @param create the factory function that will be invoked on time {@code created} to
     *               return an observable.
     * @param createdMillis the virtual time when the factory method should be invoked
     * @param subscribedMillis the virtual time when the subscription should happen
     * @param unsubscribedMillis the virtual time when the subscription should be unsubscribed
     * @return a testable observer which records the subscription, unsubscription and event timestamps
     */
    public <T> TestableObserver<T> start(final Func0<? extends Observable<? extends T>> create, 
            long createdMillis, long subscribedMillis, long unsubscribedMillis) {
        final AtomicReference<Observable<? extends T>> source = new AtomicReference<Observable<? extends T>>();
        final SingleAssignmentSubscription sas = new SingleAssignmentSubscription();
        final TestableObserver<T> observer = createObserver();
        
        scheduleAbsolute(createdMillis, new Action0() {
            @Override
            public void call() {
                source.set(create.call());
            }
        });
        scheduleAbsolute(subscribedMillis, new Action0() {
            @Override
            public void call() {
                sas.set(source.get().subscribe(observer));
            }
        });
        scheduleAbsolute(unsubscribedMillis, new Action0() {
            @Override
            public void call() {
                sas.unsubscribe();
            }
        });
        
        triggerAllActions();
        
        return observer;
    }
}
