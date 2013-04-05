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

/**
 * Represents an object that schedules units of work.
 */
public interface Scheduler {

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
     * Returns the scheduler's notion of current time.
     */
    long now();

}
