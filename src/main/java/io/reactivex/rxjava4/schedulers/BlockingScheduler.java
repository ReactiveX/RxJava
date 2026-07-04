/*
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

package io.reactivex.rxjava4.schedulers;

import io.reactivex.rxjava4.core.Scheduler;
import io.reactivex.rxjava4.functions.Action;
import io.reactivex.rxjava4.internal.functions.Functions;
import io.reactivex.rxjava4.internal.schedulers.BlockingCurrentThreadScheduler;

/**
 * Holds onto a blocking scheduler instance and provides access to its {@link #execute()}
 * method and a way to obtain a pure {@link Scheduler} instance to be used as parameter.
 * <p>
 * <strong>Implementation note</strong><br>
 *           No need to instantiate this record by client applications, it serves as a way to
 *           give access to the {@code Scheduler} interface as well as the blocking-specific
 *           {@link #execute()} methods.
 * @param backingScheduler the scheduler instance
 * @since 4.0.0
 */
public record BlockingScheduler(BlockingCurrentThreadScheduler backingScheduler) {

    /**
     * Returns the Scheduler view to submit tasks to or use it as a parameter.
     * @return the Scheduler view of the underlying blocking current thread scheduler.
     */
    public Scheduler scheduler( ) {
        return backingScheduler;
    }

    /**
     * Begin executing the blocking event loop without any initial action.
     * <p>
     * This method will block until the {@link Scheduler#shutdown()} is invoked.
     * @see #execute(Action)
     */
    public void execute() {
        execute(Functions.EMPTY_ACTION);
    }

    /**
     * Begin executing the blocking event loop with the given initial action
     * (usually contain the rest of the 'main' method).
     * <p>
     * This method will block until the {@link Scheduler#shutdown()} is invoked.
     * @param action the action to execute
     */
    public void execute(Action action) {
        backingScheduler.execute(action);
    }

    /**
     * Shuts down the underlying blocking current thread scheduler
     */
    public void shutdown() {
        backingScheduler.shutdown();
    }
}
