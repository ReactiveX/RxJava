/**
 * Copyright (c) 2016-present, RxJava Contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See
 * the License for the specific language governing permissions and limitations under the License.
 */

package io.reactivex.internal.schedulers;

import io.reactivex.Scheduler;
import io.reactivex.annotations.*;

/**
 * Allows retrieving multiple workers from the implementing
 * {@link io.reactivex.Scheduler} in a way that when asking for
 * at most the parallelism level of the Scheduler, those
 * {@link io.reactivex.Scheduler.Worker} instances will be running
 * with different backing threads.
 *
 * @since 2.1.8 - experimental
 */
@Experimental
public interface SchedulerMultiWorkerSupport {

    /**
     * Creates the given number of {@link io.reactivex.Scheduler.Worker} instances
     * that are possibly backed by distinct threads
     * and calls the specified {@code Consumer} with them.
     * @param number the number of workers to create, positive
     * @param callback the callback to send worker instances to
     */
    void createWorkers(int number, @NonNull WorkerCallback callback);

    /**
     * The callback interface for the {@link SchedulerMultiWorkerSupport#createWorkers(int, WorkerCallback)}
     * method.
     */
    interface WorkerCallback {
        /**
         * Called with the Worker index and instance.
         * @param index the worker index, zero-based
         * @param worker the worker instance
         */
        void onWorker(int index, @NonNull Scheduler.Worker worker);
    }
}
