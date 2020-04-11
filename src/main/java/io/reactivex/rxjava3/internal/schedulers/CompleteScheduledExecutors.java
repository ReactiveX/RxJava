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

package io.reactivex.rxjava3.internal.schedulers;

import java.util.concurrent.*;

/**
 * {@link CompleteScheduledExecutorService} factory.
 */
public class CompleteScheduledExecutors {
    public static CompleteScheduledExecutorService newSingleThreadExecutor() {
        return new CompleteScheduledThreadPoolExecutor(1);
    }

    public static CompleteScheduledExecutorService newSingleThreadExecutor(ThreadFactory threadFactory) {
        return new CompleteScheduledThreadPoolExecutor(1, threadFactory);
    }

    public static CompleteScheduledExecutorService newSingleThreadExecutor(RejectedExecutionHandler handler) {
        return new CompleteScheduledThreadPoolExecutor(1, handler);
    }

    public static CompleteScheduledExecutorService newSingleThreadExecutor(ThreadFactory threadFactory,
                                                                           RejectedExecutionHandler handler) {
        return new CompleteScheduledThreadPoolExecutor(1, threadFactory, handler);
    }

    public static CompleteScheduledExecutorService newThreadPoolExecutor(int corePoolSize) {
        return new CompleteScheduledThreadPoolExecutor(corePoolSize);
    }

    public static CompleteScheduledExecutorService newThreadPoolExecutor(int corePoolSize,
                                                                         ThreadFactory threadFactory) {
        return new CompleteScheduledThreadPoolExecutor(corePoolSize, threadFactory);
    }

    public static CompleteScheduledExecutorService newThreadPoolExecutor(int corePoolSize,
                                                                         RejectedExecutionHandler handler) {
        return new CompleteScheduledThreadPoolExecutor(corePoolSize, handler);
    }

    public static CompleteScheduledExecutorService newThreadPoolExecutor(int corePoolSize,
                                                                         ThreadFactory threadFactory,
                                                                         RejectedExecutionHandler handler) {
        return new CompleteScheduledThreadPoolExecutor(corePoolSize, threadFactory, handler);
    }

    /** Utility class. */
    private CompleteScheduledExecutors() {
        throw new IllegalStateException("No instances!");
    }
}
