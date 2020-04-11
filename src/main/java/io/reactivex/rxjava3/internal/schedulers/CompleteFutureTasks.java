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
 *  Factory of FutureTasks which will call CompleteHandler on complete.
 */
public class CompleteFutureTasks {
    /**
     * Builds a future task that calls completeHandle on complete.
     **/
    public static <V> RunnableFuture<V> newCompleteFutureTask(
            RunnableFuture<V> base, CompleteScheduledExecutorService.CompleteHandler<V> completeHandler) {
        return new CompleteFutureTask<>(base, completeHandler);
    }

    /**
     * Builds a scheduled future task that calls completeHandle on complete.
     */
    public static <V> RunnableScheduledFuture<V> newCompleteScheduledFutureTask(
            RunnableScheduledFuture<V> base, CompleteScheduledExecutorService.CompleteHandler<V> completeHandler) {
        return new CompleteScheduledFutureTask<>(base, completeHandler);
    }

    public static class CallableWithCompleteHandler<V> implements Callable<V> {
        public CallableWithCompleteHandler(
                Callable<V> base, CompleteScheduledExecutorService.CompleteHandler<V> completeHandler) {
            this.base = base;
            this.completeHandler = completeHandler;
        }

        public CallableWithCompleteHandler(
                Runnable runnable, V result, CompleteScheduledExecutorService.CompleteHandler<V> completeHandler) {
            this.base = () -> {
                runnable.run();
                return result;
            };
            this.completeHandler = completeHandler;
        }

        @Override
        public V call() throws Exception {
            return base.call();
        }

        public CompleteScheduledExecutorService.CompleteHandler<V> getCompleteHandler() {
            return completeHandler;
        }

        private final Callable<V> base;
        private final CompleteScheduledExecutorService.CompleteHandler<V> completeHandler;
    }

    public static class RunnableWithCompleteHandler implements Runnable {
        public RunnableWithCompleteHandler(
                Runnable base, CompleteScheduledExecutorService.CompleteHandler<Void> completeHandler) {
            this.base = base;
            this.completeHandler = completeHandler;
        }

        @Override
        public void run() {
            base.run();
        }

        public CompleteScheduledExecutorService.CompleteHandler<Void> getCompleteHandler() {
            return completeHandler;
        }

        private final Runnable base;
        private final CompleteScheduledExecutorService.CompleteHandler<Void> completeHandler;
    }

    private static class CompleteFutureTask<V> implements RunnableFuture<V> {
        public CompleteFutureTask(
                RunnableFuture<V> base, CompleteScheduledExecutorService.CompleteHandler<V> completeHandler) {
            this.base = base;
            this.completeHandler = completeHandler;

            this.seq = 0;
        }

        @Override
        public void run() {
            long s = ++seq;

            try {
                base.run();
            } finally {
                if (!base.isDone()) {
                    return;
                }

                // interesting case:
                // We may get the done state of next run if base is a periodic task and posted to another thread for
                // its next run.
                // A simple seq test will solve that. We even don't have to make seq volatile, because it is correctly
                // synchronized by isDone() and re-schedule locks!
                if (seq != s) {
                    return;
                }

                if (base.isCancelled()) {
                    return;
                }

                completeHandler.onComplete(this);
            }
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            return base.cancel(mayInterruptIfRunning);
        }

        @Override
        public boolean isCancelled() {
            return base.isCancelled();
        }

        @Override
        public boolean isDone() {
            return base.isDone();
        }

        @Override
        public V get() throws InterruptedException, ExecutionException {
            return base.get();
        }

        @Override
        public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            return base.get(timeout, unit);
        }

        private long seq;

        private final RunnableFuture<V> base;
        private final CompleteScheduledExecutorService.CompleteHandler<V> completeHandler;
    }

    private static class CompleteScheduledFutureTask<V> extends CompleteFutureTask<V>
            implements RunnableScheduledFuture<V> {
        public CompleteScheduledFutureTask(
                RunnableScheduledFuture<V> base, CompleteScheduledExecutorService.CompleteHandler<V> completeHandler) {
            super(base, completeHandler);
            this.base = base;
        }

        private final RunnableScheduledFuture<V> base;

        @Override
        public boolean isPeriodic() {
            return base.isPeriodic();
        }

        @Override
        public long getDelay(TimeUnit unit) {
            return base.getDelay(unit);
        }

        @Override
        public int compareTo(Delayed o) {
            return base.compareTo(o);
        }
    }

    /** Utility class. */
    private CompleteFutureTasks() {
        throw new IllegalStateException("No instances!");
    }
}
