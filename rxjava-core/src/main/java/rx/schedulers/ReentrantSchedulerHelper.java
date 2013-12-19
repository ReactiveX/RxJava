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

import java.util.concurrent.TimeUnit;
import rx.subscriptions.ForwardSubscription;

/**
 * Simple scheduler API used by the ReentrantScheduler to
 * communicate with the actual scheduler implementation.
 */
public interface ReentrantSchedulerHelper {
    /**
     * Schedule a task to be run immediately and update the subscription
     * describing the schedule.
     * @param r the task to run immediately
     * @param out the subscription holding the current schedule subscription
     */
    void scheduleTask(Runnable r, ForwardSubscription out);
    
    /**
     * Schedule a task to be run after the delay time and update the subscription
     * describing the schedule.
     * @param r the task to schedule
     * @param out the subscription holding the current schedule subscription
     * @param delayTime the time to delay the execution
     * @param unit the time unit
     */
    void scheduleTask(Runnable r, ForwardSubscription out, long delayTime, TimeUnit unit);
    
    /**
     * Schedule a task to be run after the delay time and after
     * each period, then update the subscription describing the schedule.
     * @param r the task to schedule
     * @param out the subscription holding the current schedule subscription
     * @param initialDelay the initial delay of the schedule
     * @param period the between period of the schedule
     * @param unit the time unit
     */
    void scheduleTask(Runnable r, ForwardSubscription out, long initialDelay, long period, TimeUnit unit);
}
