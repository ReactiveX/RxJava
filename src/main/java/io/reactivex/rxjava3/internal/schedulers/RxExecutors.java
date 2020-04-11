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

import io.reactivex.rxjava3.exceptions.Exceptions;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;

import java.util.concurrent.*;

/**
 * Utility class of common Executor operations in RxJava.
 */
public final class RxExecutors {
    public static Future<Void> submit(Executor executor, Runnable runnable) {
        RunnableFuture<Void> futureTask = new FutureTask<>(runnable, null);
        futureTask = CompleteFutureTasks.newCompleteFutureTask(futureTask, EXECUTE_ERROR_HANDLER);
        executor.execute(futureTask);
        return futureTask;
    }

    public static Future<Void> submit(CompleteScheduledExecutorService executor, Runnable runnable) {
        return executor.submit(runnable, null, EXECUTE_ERROR_HANDLER);
    }

    public static Future<Void> schedule(CompleteScheduledExecutorService executor,
                                     Runnable runnable, long delay, TimeUnit unit) {
        return executor.schedule(runnable, null, delay, unit, EXECUTE_ERROR_HANDLER);
    }

    public static Future<Void> scheduleAtFixedRate(CompleteScheduledExecutorService executor,
                                                Runnable runnable, long initialDelay, long period, TimeUnit unit) {
        return executor.scheduleAtFixedRate(runnable, initialDelay, period, unit, EXECUTE_ERROR_HANDLER);
    }

    public static RunnableFuture<Void> makeFutureTask(Runnable runnable) {
        RunnableFuture<Void> futureTask = new FutureTask<>(runnable, null);
        futureTask = CompleteFutureTasks.newCompleteFutureTask(futureTask, EXECUTE_ERROR_HANDLER);
        return futureTask;
    }

    private static CompleteScheduledExecutorService.CompleteHandler<Void> EXECUTE_ERROR_HANDLER = future -> {
        try {
            future.get();
        } catch (InterruptedException e) {
            throw new IllegalStateException("won't happen, already completed");
        } catch (ExecutionException e) {
            Throwable ex = e.getCause();

            Exceptions.throwIfFatal(ex);
            RxJavaPlugins.onError(ex);
        }
    };

    /** Utility class. */
    private RxExecutors() {
        throw new IllegalStateException("No instances!");
    }
}
