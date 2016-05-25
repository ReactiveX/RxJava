/**
 * Copyright 2016 Netflix, Inc.
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

/**
 * Called by the ScheduledAction to remove itself from the parent tracking structure
 * and ask for the instantiation location of the parent Worker.
 */
public interface WorkerCallback {
    /**
     * Adds the specified action to the tracking structure.
     * @param action the action to add, not null
     */
    void add(ScheduledAction action);
    /**
     * Remove the specified action from the tracking structure.
     * @param action the action to remove, not null
     */
    void remove(ScheduledAction action);
    /**
     * Returns the Throwable exception representing the stacktrace
     * where the parent worker has been created or null
     * if worker tracking is disabled.
     * @return the Throwable or null
     */
    Throwable workerCreationSite();
}
