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

import java.util.concurrent.TimeUnit;

import rx.util.functions.Action0;
import rx.util.functions.Func0;
import rx.util.functions.Func1;
import rx.util.functions.Func2;

/**
 * Represents an object that schedules units of work.
 */
public interface Scheduler {

    /**
     * Schedules a cancelable action to be executed.
     * 
     * @param state State to pass into the action.
     * @param action Action to schedule.
     * @return a subscription to be able to unsubscribe from action.
     */
    <T> Subscription schedule(T state, Func2<Scheduler, T, Subscription> action);

    /**
     * Schedules a cancelable action to be executed.
     * 
     * @param action Action to schedule.
     * @return a subscription to be able to unsubscribe from action.
     */
    Subscription schedule(Func1<Scheduler, Subscription> action);
    
    /**
     * Schedules a cancelable action to be executed.
     * 
     * @param action
     *            action
     * @return a subscription to be able to unsubscribe from action.
     */
    Subscription schedule(Func0<Subscription> action);

    /**
     * Schedules an action to be executed.
     * 
     * @param action
     *            action
     * @return a subscription to be able to unsubscribe from action.
     */
    Subscription schedule(Action0 action);

    /**
     * Schedules a cancelable action to be executed in dueTime.
     * 
     * @param state State to pass into the action.
     * @param action Action to schedule.
     * @param dueTime Time the action is due for executing.
     * @param unit Time unit of the due time.
     * @return a subscription to be able to unsubscribe from action.
     */
    <T> Subscription schedule(T state, Func2<Scheduler, T, Subscription> action, long dueTime, TimeUnit unit);

    /**
     * Schedules a cancelable action to be executed in dueTime.
     * 
     * @param action Action to schedule.
     * @param dueTime Time the action is due for executing.
     * @param unit Time unit of the due time.
     * @return a subscription to be able to unsubscribe from action.
     */
    Subscription schedule(Func1<Scheduler, Subscription> action, long dueTime, TimeUnit unit);
    
    /**
     * Schedules an action to be executed in dueTime.
     * 
     * @param action
     *            action
     * @return a subscription to be able to unsubscribe from action.
     */
    Subscription schedule(Action0 action, long dueTime, TimeUnit unit);

    /**
     * Schedules a cancelable action to be executed in dueTime.
     * 
     * @param action
     *            action
     * @return a subscription to be able to unsubscribe from action.
     */
    Subscription schedule(Func0<Subscription> action, long dueTime, TimeUnit unit);

    /**
     * Schedules an action to be executed periodically.
     * 
     * @param action The action to execute periodically.
     * @param initialDelay Time to wait before executing the action for the first time.
     * @param period The time interval to wait each time in between executing the action.
     * @param unit The time unit the interval above is given in.
     * @return A subscription to be able to unsubscribe from action.
     */
    Subscription schedulePeriodically(Action0 action, long initialDelay, long period, TimeUnit unit);

    /**
     * Schedules a cancelable action to be executed periodically.
     * 
     * @param action The action to execute periodically.
     * @param initialDelay Time to wait before executing the action for the first time.
     * @param period The time interval to wait each time in between executing the action.
     * @param unit The time unit the interval above is given in.
     * @return A subscription to be able to unsubscribe from action.
     */
    Subscription schedulePeriodically(Func0<Subscription> action, long initialDelay, long period, TimeUnit unit);

    /**
     * Schedules a cancelable action to be executed periodically.
     * 
     * @param action The action to execute periodically.
     * @param initialDelay Time to wait before executing the action for the first time.
     * @param period The time interval to wait each time in between executing the action.
     * @param unit The time unit the interval above is given in.
     * @return A subscription to be able to unsubscribe from action.
     */
    Subscription schedulePeriodically(Func1<Scheduler, Subscription> action, long initialDelay, long period, TimeUnit unit);

    /**
     * Schedules a cancelable action to be executed periodically.
     *
     * @param state State to pass into the action.
     * @param action The action to execute periodically.
     * @param initialDelay Time to wait before executing the action for the first time.
     * @param period The time interval to wait each time in between executing the action.
     * @param unit The time unit the interval above is given in.
     * @return A subscription to be able to unsubscribe from action.
     */
    <T> Subscription schedulePeriodically(T state, Func2<Scheduler, T, Subscription> action, long initialDelay, long period, TimeUnit unit);

    /**
     * Returns the scheduler's notion of current time.
     */
    long now();

}
