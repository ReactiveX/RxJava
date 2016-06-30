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
package rx.internal.schedulers;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

import rx.Scheduler;
import rx.internal.util.RxThreadFactory;

/**
 * A default {@link ScheduledExecutorService} that can be used for scheduling actions when a {@link Scheduler} implementation doesn't have that ability.
 * <p>
 * For example if a {@link Scheduler} is given an {@link Executor} or {{@link ExecutorService} instead of {@link ScheduledExecutorService}.
 * <p>
 * NOTE: No actual work should be done on tasks submitted to this executor. Submit a task with the appropriate delay which then in turn invokes
 * the work asynchronously on the appropriate {@link Scheduler} implementation. This means for example that you would not use this approach
 * along with {@link TrampolineScheduler} or {@link ImmediateScheduler}.
 */
public final class GenericScheduledExecutorService implements SchedulerLifecycle {

    private static final String THREAD_NAME_PREFIX = "RxScheduledExecutorPool-";
    private static final RxThreadFactory THREAD_FACTORY = new RxThreadFactory(THREAD_NAME_PREFIX);

    private static final ScheduledExecutorService[] NONE = new ScheduledExecutorService[0];

    private static final ScheduledExecutorService SHUTDOWN;
    static {
        SHUTDOWN = Executors.newScheduledThreadPool(0);
        SHUTDOWN.shutdown();
    }

    /* Schedulers needs access to this in order to work with the lifecycle. */
    public final static GenericScheduledExecutorService INSTANCE = new GenericScheduledExecutorService();
    
    private final AtomicReference<ScheduledExecutorService[]> executor;

    /** We don't use atomics with this because thread-assignment is random anyway. */
    private static int roundRobin;
    
    private GenericScheduledExecutorService() {
        executor = new AtomicReference<ScheduledExecutorService[]>(NONE);
        start();
    }

    @Override
    public void start() {
        int count = Runtime.getRuntime().availableProcessors();
        if (count > 4) {
            count = count / 2;
        }
        // we don't need more than 8 to handle just scheduling and doing no work
        if (count > 8) {
            count = 8;
        }
        
        // A multi-threaded executor can reorder tasks, having a set of them
        // and handing one of those out on getInstance() ensures a proper order
        
        ScheduledExecutorService[] execs = new ScheduledExecutorService[count];
        for (int i = 0; i < count; i++) {
            execs[i] = Executors.newScheduledThreadPool(1, THREAD_FACTORY);
        }
        if (executor.compareAndSet(NONE, execs)) {
            for (ScheduledExecutorService exec : execs) {
                if (!NewThreadWorker.tryEnableCancelPolicy(exec)) {
                    if (exec instanceof ScheduledThreadPoolExecutor) {
                        NewThreadWorker.registerExecutor((ScheduledThreadPoolExecutor)exec);
                    }
                }
            }
        } else {
            for (ScheduledExecutorService exec : execs) {
                exec.shutdownNow();
            }
        }
    }
    
    @Override
    public void shutdown() {
        for (;;) {
            ScheduledExecutorService[] execs = executor.get();
            if (execs == NONE) {
                return;
            }
            if (executor.compareAndSet(execs, NONE)) {
                for (ScheduledExecutorService exec : execs) {
                    NewThreadWorker.deregisterExecutor(exec);
                    exec.shutdownNow();
                }
                return;
            }
        }
    }
    
    /**
     * Returns one of the single-threaded ScheduledExecutorService helper executors.
     * 
     * @return {@link ScheduledExecutorService} for generic use.
     */
    public static ScheduledExecutorService getInstance() {
        ScheduledExecutorService[] execs = INSTANCE.executor.get();
        if (execs == NONE) {
            return SHUTDOWN;
        }
        int r = roundRobin + 1;
        if (r >= execs.length) {
            r = 0;
        }
        roundRobin = r;
        return execs[r];
    }
}