/**
 * Copyright 2015 Netflix, Inc.
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

import java.util.concurrent.*;

import io.reactivex.Scheduler;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.disposables.*;
import io.reactivex.plugins.RxJavaPlugins;

/**
 * Base class that manages a single-threaded ScheduledExecutorService as a
 * worker but doesn't perform task-tracking operations.
 *
 */
public class NewThreadWorker extends Scheduler.Worker implements Disposable {
    private final ScheduledExecutorService executor;

    volatile boolean disposed;
    
    /* package */
    public NewThreadWorker(ThreadFactory threadFactory) {
        ScheduledExecutorService exec = Executors.newScheduledThreadPool(1, threadFactory);
        // Java 7+: cancelled future tasks can be removed from the executor thus avoiding memory leak
        if (exec instanceof ScheduledThreadPoolExecutor) {
            ((ScheduledThreadPoolExecutor)exec).setRemoveOnCancelPolicy(true);
        }
        executor = exec;
    }

    @Override
    public Disposable schedule(final Runnable run) {
        return schedule(run, 0, null);
    }

    @Override
    public Disposable schedule(final Runnable action, long delayTime, TimeUnit unit) {
        if (disposed) {
            return EmptyDisposable.INSTANCE;
        }
        return scheduleActual(action, delayTime, unit, null);
    }

    /**
     * Schedules the given runnable on the underlying executor directly and
     * returns its future wrapped into a Disposable.
     * @param run
     * @param delayTime
     * @param unit
     * @return
     */
    public Disposable scheduleDirect(final Runnable run, long delayTime, TimeUnit unit) {
        Runnable decoratedRun = RxJavaPlugins.onSchedule(run);
        try {
            Future<?> f;
            if (delayTime <= 0) {
                f = executor.submit(decoratedRun);
            } else {
                f = executor.schedule(decoratedRun, delayTime, unit);
            }
            return () -> f.cancel(true);
        } catch (RejectedExecutionException ex) {
            RxJavaPlugins.onError(ex);
            return EmptyDisposable.INSTANCE;
        }
    }

    /**
     * Schedules the given runnable periodically on the underlying executor directly
     * and returns its future wrapped into a Disposable.
     * @param run
     * @param initialDelay
     * @param period
     * @param unit
     * @return
     */
    public Disposable schedulePeriodicallyDirect(final Runnable run, long initialDelay, long period, TimeUnit unit) {
        Runnable decoratedRun = RxJavaPlugins.onSchedule(run);
        try {
            Future<?> f = executor.scheduleAtFixedRate(decoratedRun, initialDelay, period, unit);
            return () -> f.cancel(true);
        } catch (RejectedExecutionException ex) {
            RxJavaPlugins.onError(ex);
            return EmptyDisposable.INSTANCE;
        }
    }

    
    /**
     * Wraps the given runnable into a ScheduledRunnable and schedules it
     * on the underlying ScheduledExecutorService.
     * <p>If the schedule has been rejected, the ScheduledRunnable.wasScheduled will return
     * false.
     * @param action
     * @param delayTime
     * @param unit
     * @return
     */
    public ScheduledRunnable scheduleActual(final Runnable run, long delayTime, TimeUnit unit, CompositeResource<Disposable> parent) {
        Runnable decoratedRun = RxJavaPlugins.onSchedule(run);
        
        ScheduledRunnable sr = new ScheduledRunnable(decoratedRun, parent);
        
        if (parent != null) {
            if (!parent.add(sr)) {
                return sr;
            }
        }
        
        Future<?> f;
        try {
            if (delayTime <= 0) {
                f = executor.submit(run);
            } else {
                f = executor.schedule(run, delayTime, unit);
            }
            sr.setFuture(f);
        } catch (RejectedExecutionException ex) {
            RxJavaPlugins.onError(ex);
        }

        return sr;
    }

    @Override
    public void dispose() {
        if (!disposed) {
            disposed = true;
            executor.shutdownNow();
        }
    }
}