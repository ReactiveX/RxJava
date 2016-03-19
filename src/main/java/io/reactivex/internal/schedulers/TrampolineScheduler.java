/**
 * Copyright 2016 Netflix, Inc.
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

package io.reactivex.internal.schedulers;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import io.reactivex.Scheduler;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.disposables.EmptyDisposable;
import io.reactivex.internal.functions.Objects;
import io.reactivex.plugins.RxJavaPlugins;

/**
 * Schedules work on the current thread but does not execute immediately. Work is put in a queue and executed
 * after the current unit of work is completed.
 */
public final class TrampolineScheduler extends Scheduler {
    private static final TrampolineScheduler INSTANCE = new TrampolineScheduler();

    public static TrampolineScheduler instance() {
        return INSTANCE;
    }

    @Override
    public Worker createWorker() {
        return new TrampolineWorker();
    }

    /* package accessible for unit tests */TrampolineScheduler() {
    }
    
    @Override
    public Disposable scheduleDirect(Runnable run) {
        run.run();
        return EmptyDisposable.INSTANCE;
    }
    
    @Override
    public Disposable scheduleDirect(Runnable run, long delay, TimeUnit unit) {
        try {
            unit.sleep(delay);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            RxJavaPlugins.onError(ex);
        }
        return EmptyDisposable.INSTANCE;
    }

    static final class TrampolineWorker extends Scheduler.Worker implements Disposable {
        private final PriorityBlockingQueue<TimedRunnable> queue = new PriorityBlockingQueue<TimedRunnable>();
        
        private final AtomicInteger wip = new AtomicInteger();

        final AtomicInteger counter = new AtomicInteger();
        
        volatile boolean disposed;

        @Override
        public Disposable schedule(Runnable action) {
            return enqueue(action, now(TimeUnit.MILLISECONDS));
        }

        @Override
        public Disposable schedule(Runnable action, long delayTime, TimeUnit unit) {
            long execTime = now(TimeUnit.MILLISECONDS) + unit.toMillis(delayTime);

            return enqueue(new SleepingRunnable(action, this, execTime), execTime);
        }
        
        private Disposable enqueue(Runnable action, long execTime) {
            if (disposed) {
                return EmptyDisposable.INSTANCE;
            }
            final TimedRunnable timedRunnable = new TimedRunnable(action, execTime, counter.incrementAndGet());
            queue.add(timedRunnable);

            if (wip.getAndIncrement() == 0) {
                int missed = 1;
                for (;;) {
                    for (;;) {
                        final TimedRunnable polled = queue.poll();
                        if (polled == null) {
                            break;
                        }
                        if (!polled.disposed) {
                            polled.run.run();
                        }
                    }
                    missed = wip.addAndGet(-missed);
                    if (missed == 0) {
                        break;
                    }
                }
                
                return EmptyDisposable.INSTANCE;
            } else {
                // queue wasn't empty, a parent is already processing so we just add to the end of the queue
                return new Disposable() {
                    @Override
                    public void dispose() {
                        timedRunnable.disposed = true;
                        queue.remove(timedRunnable);
                    }
                };
            }
        }

        @Override
        public void dispose() {
            disposed = true;
        }
    }

    static final class TimedRunnable implements Comparable<TimedRunnable> {
        final Runnable run;
        final long execTime;
        final int count; // In case if time between enqueueing took less than 1ms
        
        volatile boolean disposed;

        private TimedRunnable(Runnable run, Long execTime, int count) {
            this.run = run;
            this.execTime = execTime;
            this.count = count;
        }

        @Override
        public int compareTo(TimedRunnable that) {
            int result = Objects.compare(execTime, that.execTime);
            if (result == 0) {
                return Objects.compare(count, that.count);
            }
            return result;
        }
    }
    
    static final class SleepingRunnable implements Runnable {
        private final Runnable run;
        private final TrampolineWorker worker;
        private final long execTime;

        public SleepingRunnable(Runnable run, TrampolineWorker worker, long execTime) {
            this.run = run;
            this.worker = worker;
            this.execTime = execTime;
        }

        @Override
        public void run() {
            if (worker.disposed) {
                return;
            }
            long t = worker.now(TimeUnit.MILLISECONDS);
            if (execTime > t) {
                long delay = execTime - t;
                if (delay > 0) {
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        RxJavaPlugins.onError(e);
                        return;
                    }
                }
            }

            if (worker.disposed) {
                return;
            }
            run.run();
        }
    }
}