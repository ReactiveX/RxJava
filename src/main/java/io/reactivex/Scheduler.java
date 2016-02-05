/**
 * Copyright 2016 Netflix, Inc.
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

package io.reactivex;

import java.util.concurrent.TimeUnit;

import io.reactivex.disposables.*;
import io.reactivex.internal.disposables.*;
import io.reactivex.internal.util.Exceptions;
import io.reactivex.plugins.RxJavaPlugins;

public abstract class Scheduler {
    
    public abstract Worker createWorker();

    /**
     * Returns the 'current time' of the Scheduler in the specified time unit.
     * @param unit the time unit
     * @return the 'current time'
     */
    public long now(TimeUnit unit) {
        return unit.convert(System.currentTimeMillis(), TimeUnit.MILLISECONDS);
    }

    /*
     * TODO Should the lifecycle methods be part of the public API?
     */
    public void start() {
        
    }
    
    public void shutdown() {
        
    }
    
    /*
     * TODO This helps reducing the memory usage for 
     * certain one-shot scheduling required operators (such as interval,
     * scalarjust + subscribeOn, etc.) but complicates the API
     * surface.
     * 
     * So either have default implementation in Scheduler or
     * have the operars check for xxxDirect() support and chose paths accordingly.
     */
    public Disposable scheduleDirect(Runnable run) {
        return scheduleDirect(run, 0L, TimeUnit.NANOSECONDS);
    }

    /**
     * Schedules the given runnable with the given delay directly on a worker of this scheduler.
     * <p>Override this method to provide an efficient implementation that,
     * for example, doesn't have extra tracking structures for such one-shot
     * executions.
     * @param run the runnable to schedule
     * @param delay the delay time
     * @param unit the delay unit
     * @return the disposable instance that can cancel the task
     */
    public Disposable scheduleDirect(Runnable run, long delay, TimeUnit unit) {
        final Worker w = createWorker();
        
        final Runnable decoratedRun = RxJavaPlugins.onSchedule(run);
        
        w.schedule(new Runnable() {
            @Override
            public void run() {
                try {
                    decoratedRun.run();
                } finally {
                    w.dispose();
                }
            }
        }, delay, unit);
        
        return w;
    }
    
    public Disposable schedulePeriodicallyDirect(Runnable run, long initialDelay, long period, TimeUnit unit) {
        final ArrayCompositeResource<Disposable> acr = new ArrayCompositeResource<Disposable>(2, Disposables.consumeAndDispose());
        final Worker w = createWorker();
        acr.lazySet(0, w);
        
        final Runnable decoratedRun = RxJavaPlugins.onSchedule(run);
        
        acr.setResource(1, w.schedulePeriodically(new Runnable() {
            @Override
            public void run() {
                try {
                    decoratedRun.run();
                } catch (Throwable e) {
                    // make sure the worker is released if the run crashes
                    acr.dispose();
                    throw Exceptions.propagate(e);
                }
            }
        }, initialDelay, period, unit));
        
        return acr;
    }

    public static abstract class Worker implements Disposable {

        public abstract Disposable schedule(Runnable run, long delay, TimeUnit unit);

        public Disposable schedule(Runnable run) {
            return schedule(run, 0L, TimeUnit.NANOSECONDS);
        }
        
        public Disposable schedulePeriodically(Runnable run, final long initialDelay, final long period, final TimeUnit unit) {
            final MultipleAssignmentResource<Disposable> first = new MultipleAssignmentResource<Disposable>(Disposables.consumeAndDispose());

            final MultipleAssignmentResource<Disposable> mar = new MultipleAssignmentResource<Disposable>(Disposables.consumeAndDispose(), first);
            
            final Runnable decoratedRun = RxJavaPlugins.onSchedule(run);
            
            first.setResource(schedule(new Runnable() {
                long lastNow = now(unit);
                long startTime = lastNow + initialDelay;
                long count;
                @Override
                public void run() {
                    decoratedRun.run();
                    
                    long t = now(unit);
                    long c = ++count;
                    
                    long targetTime = startTime + c * period;
                    
                    long delay;
                    // if the current time is less than last time
                    // avoid scheduling the next run too far in the future
                    if (t < lastNow) {
                        delay = period;
                        // TODO not sure about this correction
                        startTime -= lastNow - c * period;
                    }
                    // if the current time is ahead of the target time, 
                    // avoid scheduling a bunch of 0 delayed tasks
                    else if (t > targetTime) {
                        delay = period;
                        // TODO not sure about this correction
                        startTime += t - c * period;
                    } else {
                        delay = targetTime - t;
                    }
                    
                    lastNow = t;
                    
                    mar.setResource(schedule(this, delay, unit));
                }
            }, initialDelay, unit));
            
            return mar;
        }
        
        /**
         * Returns the 'current time' of the Worker in the specified time unit.
         * @param unit the time unit
         * @return the 'current time'
         */
        public long now(TimeUnit unit) {
            return unit.convert(System.currentTimeMillis(), TimeUnit.MILLISECONDS);
        }
        
    }
}
