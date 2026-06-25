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

package io.reactivex.rxjava4.core.config;

import java.util.concurrent.ThreadFactory;

import io.reactivex.rxjava4.annotations.*;
import io.reactivex.rxjava4.internal.functions.ObjectHelper;
import io.reactivex.rxjava4.schedulers.Schedulers;

/**
 * Configuration record for {@link Schedulers#createParallel(ParallelSchedulerConfig)}.
 * @param parallelism the number of concurrent threads, default the number of CPUs.
 * @param tracking if true, tasks submitted to it will be tracked and can be en-masse disposed
 * @param priority the thread priority of the created platform threads. See {@link Thread#NORM_PRIORITY}.
 * @param threadNamePrefix the prefix to name the scheduler's threads
 * @param factory the customizable factory for the underlying Executor, if non-null, the priority and threadNamePrefix
 *                are ignored
 */
public record ParallelSchedulerConfig(
        int parallelism,
        boolean tracking,
        int priority,
        @NonNull String threadNamePrefix,
        @Nullable ThreadFactory factory) {

    /**
     * Default configuration: Available CPU parallelism, tracking on, normal thread priority
     * {@code RxParallelScheduler} naming and no custom {@link ThreadFactory}.
     */
    public static final ParallelSchedulerConfig DEFAULT = new ParallelSchedulerConfig(
            Runtime.getRuntime().availableProcessors(), true, Thread.NORM_PRIORITY,
            "RxParallelScheduler", null);

    /**
     * Creates a default config with the given parallelism,
     * normal priority, tracking and RxParallelScheduler thread name prefix.
     * @param parallelism the number of threads to work with in the scheduler
     */
    public ParallelSchedulerConfig(int parallelism) {
        this(parallelism, true, Thread.NORM_PRIORITY, "RxParallelScheduler", null);
    }

    /**
     * Creates a default config with the given parallelism,
     * normal priority, optionally tracking and RxParallelScheduler thread name prefix.
     * @param parallelism the number of threads to work with in the scheduler
     * @param tracking if true, tasks submitted to it will be tracked and can be en-masse disposed
     */
    public ParallelSchedulerConfig(int parallelism, boolean tracking) {
        this(parallelism, tracking, Thread.NORM_PRIORITY, "RxParallelScheduler", null);
    }

    /**
     * Creates a default config with the given parallelism,
     * normal priority, optionally tracking and RxParallelScheduler thread name prefix.
     * @param parallelism the number of threads to work with in the scheduler
     * @param threadNamePrefix the prefix to name the scheduler's threads
     */
    public ParallelSchedulerConfig(int parallelism, @NonNull String threadNamePrefix) {
        this(parallelism, true, Thread.NORM_PRIORITY, threadNamePrefix, null);
    }

    /**
     * Creates a default config with the given parallelism,
     * normal priority, optionally tracking and RxParallelScheduler thread name prefix.
     * @param parallelism the number of threads to work with in the scheduler
     * @param tracking if true, tasks submitted to it will be tracked and can be en-masse disposed
     * @param threadNamePrefix the prefix to name the scheduler's threads
     */
    public ParallelSchedulerConfig(int parallelism, boolean tracking, @NonNull String threadNamePrefix) {
        this(parallelism, tracking, Thread.NORM_PRIORITY, threadNamePrefix, null);
    }

    /**
     * Creates a default config with the given parallelism,
     * normal priority, optionally tracking and RxParallelScheduler thread name prefix.
     * @param parallelism the number of threads to work with in the scheduler
     * @param tracking if true, tasks submitted to it will be tracked and can be en-masse disposed
     * @param factory the customizable factory for the underlying Executor
     */
    public ParallelSchedulerConfig(int parallelism, boolean tracking, @NonNull ThreadFactory factory) {
        this(parallelism, tracking, Thread.NORM_PRIORITY, "", factory);
    }

    /**
     * Creates a fully configurable ParallelSchedulerConfig object.
     * @param parallelism the number of threads to work with in the scheduler
     * @param tracking if true, tasks submitted to it will be tracked and can be en-masse disposed
     * @param priority the thread priority of the created platform threads. See {@link Thread#NORM_PRIORITY}.
     * @param threadNamePrefix the prefix to name the scheduler's threads
     * @param factory the customizable factory for the underlying Executor, if non-null, the priority
     *                and threadNamePrefix are ignored
     */
    public ParallelSchedulerConfig {
        ObjectHelper.verifyPositive(parallelism, "parallelism");
    }
}
