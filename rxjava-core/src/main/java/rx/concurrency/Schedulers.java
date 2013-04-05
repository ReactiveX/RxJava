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
package rx.concurrency;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import rx.Scheduler;

public class Schedulers {
    private static final ScheduledExecutorService COMPUTATION_EXECUTOR = createComputationExecutor();
    private static final ScheduledExecutorService IO_EXECUTOR = createIOExecutor();
    private static final int DEFAULT_MAX_IO_THREADS = 10;
    private static final int DEFAULT_KEEP_ALIVE_TIME = 10 * 1000; // 10 seconds

    private Schedulers() {

    }

    public static Scheduler immediate() {
        return ImmediateScheduler.getInstance();
    }

    public static Scheduler currentThread() {
        return CurrentThreadScheduler.getInstance();
    }

    public static Scheduler newThread() {
        return NewThreadScheduler.getInstance();
    }

    public static Scheduler executor(Executor executor) {
        return new ExecutorScheduler(executor);
    }

    public static Scheduler fromScheduledExecutorService(ScheduledExecutorService executor) {
        return new ExecutorScheduler(executor);
    }

    public static Scheduler threadPoolForComputation() {
        return fromScheduledExecutorService(COMPUTATION_EXECUTOR);
    }

    public static Scheduler threadPoolForIO() {
        return fromScheduledExecutorService(IO_EXECUTOR);
    }

    private static ScheduledExecutorService createComputationExecutor() {
        int cores = Runtime.getRuntime().availableProcessors();
        return Executors.newScheduledThreadPool(cores, new ThreadFactory() {
            final AtomicInteger counter = new AtomicInteger();

            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "RxComputationThreadPool-" + counter.incrementAndGet());
            }
        });
    }

    private static ScheduledExecutorService createIOExecutor() {
        ScheduledThreadPoolExecutor result = new ScheduledThreadPoolExecutor(DEFAULT_MAX_IO_THREADS, new ThreadFactory() {
            final AtomicInteger counter = new AtomicInteger();

            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "RxIOThreadPool-" + counter.incrementAndGet());
            }
        });

        result.setKeepAliveTime(DEFAULT_KEEP_ALIVE_TIME, TimeUnit.MILLISECONDS);
        result.allowCoreThreadTimeOut(true);

        return result;
    }
}
