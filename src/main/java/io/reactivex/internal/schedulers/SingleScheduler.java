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
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

import io.reactivex.Scheduler;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.disposables.*;
import io.reactivex.plugins.RxJavaPlugins;

public final class SingleScheduler extends Scheduler {
    
    volatile ScheduledExecutorService executor;
    
    static final AtomicReferenceFieldUpdater<SingleScheduler, ScheduledExecutorService> EXECUTOR =
            AtomicReferenceFieldUpdater.newUpdater(SingleScheduler.class, ScheduledExecutorService.class, "executor");
    
    static final ScheduledExecutorService SHUTDOWN;
    static {
        SHUTDOWN = Executors.newScheduledThreadPool(0);
        SHUTDOWN.shutdown();
    }
    
    public SingleScheduler() {
        executor = createExecutor();
    }

    static ScheduledExecutorService createExecutor() {
        ScheduledExecutorService exec = Executors.newScheduledThreadPool(1, new RxThreadFactory("RxSingleScheduler-"));
        ((ScheduledThreadPoolExecutor)exec).setRemoveOnCancelPolicy(true);
        return exec;
    }
    
    @Override
    public void start() {
        ScheduledExecutorService next = null;
        for (;;) {
            ScheduledExecutorService current = executor;
            if (current != SHUTDOWN) {
                if (next != null) {
                    next.shutdown();
                }
                return;
            }
            if (next == null) {
                next = createExecutor();
            }
            if (EXECUTOR.compareAndSet(this, current, next)) {
                return;
            }
            
        }
    }
    
    @Override
    public void shutdown() {
        ScheduledExecutorService current = executor;
        if (current != SHUTDOWN) {
            current = EXECUTOR.getAndSet(this, SHUTDOWN);
            if (current != SHUTDOWN) {
                current.shutdownNow();
            }
        }
    }
    
    @Override
    public Worker createWorker() {
        return new ScheduledWorker(executor);
    }
    
    @Override
    public Disposable scheduleDirect(Runnable run, long delay, TimeUnit unit) {
        Runnable decoratedRun = RxJavaPlugins.onSchedule(run);
        try {
            Future<?> f;
            if (delay <= 0L) {
                f = executor.submit(decoratedRun);
            } else {
                f = executor.schedule(decoratedRun, delay, unit);
            }
            return () -> f.cancel(true);
        } catch (RejectedExecutionException ex) {
            RxJavaPlugins.onError(ex);
            return EmptyDisposable.INSTANCE;
        }
    }
    
    @Override
    public Disposable schedulePeriodicallyDirect(Runnable run, long initialDelay, long period, TimeUnit unit) {
        Runnable decoratedRun = RxJavaPlugins.onSchedule(run);
        try {
            Future<?> f = executor.scheduleAtFixedRate(decoratedRun, initialDelay, period, unit);
            return () -> f.cancel(true);
        } catch (RejectedExecutionException ex) {
            RxJavaPlugins.onError(ex);
            return EmptyDisposable.INSTANCE;
        }
    }
    
    static final class ScheduledWorker extends Scheduler.Worker {
        
        final ScheduledExecutorService executor;
        
        final SetCompositeResource<Disposable> tasks;
        
        volatile boolean disposed;
        
        public ScheduledWorker(ScheduledExecutorService executor) {
            this.executor = executor;
            this.tasks = new SetCompositeResource<>(Disposable::dispose);
        }
        
        @Override
        public Disposable schedule(Runnable run, long delay, TimeUnit unit) {
            if (disposed) {
                return EmptyDisposable.INSTANCE;
            }
            
            Runnable decoratedRun = RxJavaPlugins.onSchedule(run);
            
            ScheduledRunnable sr = new ScheduledRunnable(decoratedRun, tasks);
            tasks.add(sr);
            
            try {
                Future<?> f;
                if (delay <= 0L) {
                    f = executor.submit(sr);
                } else {
                    f = executor.schedule(sr, delay, unit);
                }
                
                sr.setFuture(f);
            } catch (RejectedExecutionException ex) {
                dispose();
                RxJavaPlugins.onError(ex);
                return EmptyDisposable.INSTANCE;
            }
            
            return sr;
        }
        
        @Override
        public void dispose() {
            if (!disposed) {
                disposed = true;
                tasks.dispose();
            }
        }
    }
}
