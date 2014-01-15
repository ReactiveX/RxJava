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
package rx.concurrency;

import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;

import rx.Scheduler;

/**
 * Deprecated. Package changed from rx.concurrency to rx.schedulers.
 * 
 * @deprecated Use {@link rx.schedulers.Schedulers} instead. This will be removed before 1.0 release.
 */
@Deprecated
public class Schedulers {

    /**
     * {@link Scheduler} that executes work immediately on the current thread.
     * 
     * @return {@link ImmediateScheduler} instance
     */
    @Deprecated
    public static Scheduler immediate() {
        return rx.schedulers.ImmediateScheduler.getInstance();
    }

    /**
     * {@link Scheduler} that queues work on the current thread to be executed after the current work completes.
     * 
     * @return {@link CurrentThreadScheduler} instance
     */
    @Deprecated
    public static Scheduler currentThread() {
        return rx.schedulers.CurrentThreadScheduler.getInstance();
    }

    /**
     * {@link Scheduler} that creates a new {@link Thread} for each unit of work.
     * 
     * @return {@link NewThreadScheduler} instance
     */
    @Deprecated
    public static Scheduler newThread() {
        return rx.schedulers.NewThreadScheduler.getInstance();
    }

    /**
     * {@link Scheduler} that queues work on an {@link Executor}.
     * <p>
     * Note that this does not support scheduled actions with a delay.
     * 
     * @return {@link ExecutorScheduler} instance
     */
    @Deprecated
    public static Scheduler executor(Executor executor) {
        return new rx.schedulers.ExecutorScheduler(executor);
    }

    /**
     * {@link Scheduler} that queues work on an {@link ScheduledExecutorService}.
     * 
     * @return {@link ExecutorScheduler} instance
     */
    @Deprecated
    public static Scheduler executor(ScheduledExecutorService executor) {
        return new rx.schedulers.ExecutorScheduler(executor);
    }

    /**
     * {@link Scheduler} intended for computational work.
     * <p>
     * The implementation is backed by a {@link ScheduledExecutorService} thread-pool sized to the number of CPU cores.
     * <p>
     * This can be used for event-loops, processing callbacks and other computational work.
     * <p>
     * Do not perform IO-bound work on this scheduler. Use {@link #threadPoolForComputation()} instead.
     * 
     * @return {@link ExecutorScheduler} for computation-bound work.
     */
    @Deprecated
    public static Scheduler threadPoolForComputation() {
        return rx.schedulers.Schedulers.threadPoolForComputation();
    }

    /**
     * {@link Scheduler} intended for IO-bound work.
     * <p>
     * The implementation is backed by an {@link Executor} thread-pool that will grow as needed.
     * <p>
     * This can be used for asynchronously performing blocking IO.
     * <p>
     * Do not perform computational work on this scheduler. Use {@link #threadPoolForComputation()} instead.
     * 
     * @return {@link ExecutorScheduler} for IO-bound work.
     */
    @Deprecated
    public static Scheduler threadPoolForIO() {
        return rx.schedulers.Schedulers.threadPoolForIO();
    }

}
