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
 * Thread pool implementation of {@link CompleteScheduledExecutorService}.
 */
public class CompleteScheduledThreadPoolExecutor
        extends ScheduledThreadPoolExecutor implements CompleteScheduledExecutorService {

    public CompleteScheduledThreadPoolExecutor(int corePoolSize) {
        super(corePoolSize);
    }

    public CompleteScheduledThreadPoolExecutor(int corePoolSize,
                                               ThreadFactory threadFactory) {
        super(corePoolSize, threadFactory);
    }

    public CompleteScheduledThreadPoolExecutor(int corePoolSize,
                                               RejectedExecutionHandler handler) {
        super(corePoolSize, handler);
    }

    public CompleteScheduledThreadPoolExecutor(int corePoolSize,
                                               ThreadFactory threadFactory,
                                               RejectedExecutionHandler handler) {
        super(corePoolSize, threadFactory, handler);
    }

    @Override
    public <V> Future<V> submit(Callable<V> task,
                                CompleteHandler<V> completeHandler) {
        return super.submit(
                new CompleteFutureTasks.CallableWithCompleteHandler<>(task, completeHandler));
    }

    @Override
    public <V> Future<V> submit(Runnable task, V result,
                                CompleteHandler<V> completeHandler) {
        return super.submit(
                new CompleteFutureTasks.CallableWithCompleteHandler<>(task ,result, completeHandler));
    }

    @Override
    public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit,
                                           CompleteHandler<V> completeHandler) {
        return super.schedule(
                new CompleteFutureTasks.CallableWithCompleteHandler<>(callable, completeHandler), delay, unit);
    }

    @Override
    public <V> ScheduledFuture<V> schedule(Runnable command, V result, long delay, TimeUnit unit,
                                           CompleteHandler<V> completeHandler) {
        return super.schedule(
                new CompleteFutureTasks.CallableWithCompleteHandler<>(command, result, completeHandler), delay, unit);
    }

    @Override
    @SuppressWarnings("unchecked")
    public ScheduledFuture<Void> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit,
                                                     CompleteHandler<Void> completeHandler) {
        return (ScheduledFuture<Void>) super.scheduleAtFixedRate(
                new CompleteFutureTasks.RunnableWithCompleteHandler(command, completeHandler), initialDelay, period, unit);
    }

    @Override
    @SuppressWarnings("unchecked")
    public ScheduledFuture<Void> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit,
                                                        CompleteHandler<Void> completeHandler) {
        return (ScheduledFuture<Void>) super.scheduleWithFixedDelay(
                new CompleteFutureTasks.RunnableWithCompleteHandler(command, completeHandler), initialDelay, delay, unit);
    }

    @Override
    protected <V> RunnableScheduledFuture<V> decorateTask(Callable<V> callable, RunnableScheduledFuture<V> task) {
        if (callable instanceof CompleteFutureTasks.CallableWithCompleteHandler) {
            return CompleteFutureTasks.newCompleteScheduledFutureTask(
                    task,
                    ((CompleteFutureTasks.CallableWithCompleteHandler<V>) callable).getCompleteHandler());
        }

        return super.decorateTask(callable, task);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected <V> RunnableScheduledFuture<V> decorateTask(Runnable runnable, RunnableScheduledFuture<V> task) {
        if (runnable instanceof CompleteFutureTasks.RunnableWithCompleteHandler) {
            return (RunnableScheduledFuture<V>) CompleteFutureTasks.newCompleteScheduledFutureTask(
                    (RunnableScheduledFuture<Void>) task,
                    ((CompleteFutureTasks.RunnableWithCompleteHandler) runnable).getCompleteHandler());
        }

        return super.decorateTask(runnable, task);
    }
}
