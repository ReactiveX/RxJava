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

package io.reactivex.internal.schedulers;

import java.util.concurrent.TimeUnit;

import io.reactivex.Scheduler;
import io.reactivex.disposables.*;

/**
 * A Scheduler partially implementing the API by allowing only non-delayed, non-periodic
 * task execution on the current thread immediately.
 * <p>
 * Note that this doesn't support recursive scheduling and disposing the returned Disposable
 * has no effect (because when the schedule() method returns, the task has been already run).
 */
public final class ImmediateThinScheduler extends Scheduler {

    /**
     * The singleton instance of the immediate (thin) scheduler.
     */
    public static final Scheduler INSTANCE = new ImmediateThinScheduler();

    static final Worker WORKER = new ImmediateThinWorker();

    static final Disposable DISPOSED;

    static {
        DISPOSED = Disposables.empty();
        DISPOSED.dispose();
    }

    private ImmediateThinScheduler() {
        // singleton class
    }

    @Override
    public Disposable scheduleDirect(Runnable run) {
        run.run();
        return DISPOSED;
    }

    @Override
    public Disposable scheduleDirect(Runnable run, long delay, TimeUnit unit) {
        throw new UnsupportedOperationException("This scheduler doesn't support delayed execution");
    }

    @Override
    public Disposable schedulePeriodicallyDirect(Runnable run, long initialDelay, long period, TimeUnit unit) {
        throw new UnsupportedOperationException("This scheduler doesn't support periodic execution");
    }

    @Override
    public Worker createWorker() {
        return WORKER;
    }

    static final class ImmediateThinWorker extends Worker {

        @Override
        public void dispose() {
            // This worker is always stateless and won't track tasks
        }

        @Override
        public boolean isDisposed() {
            return false; // dispose() has no effect
        }

        @Override
        public Disposable schedule(Runnable run) {
            run.run();
            return DISPOSED;
        }

        @Override
        public Disposable schedule(Runnable run, long delay, TimeUnit unit) {
            throw new UnsupportedOperationException("This scheduler doesn't support delayed execution");
        }

        @Override
        public Disposable schedulePeriodically(Runnable run, long initialDelay, long period, TimeUnit unit) {
            throw new UnsupportedOperationException("This scheduler doesn't support periodic execution");
        }
    }
}
