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

package io.reactivex.rxjava3.testsupport;

import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.disposables.*;

/**
 * Basic scheduler that produces an ever increasing {@link #now(TimeUnit)} value.
 * Use this scheduler only as a time source!
 */
public final class TimesteppingScheduler extends Scheduler {

    final class TimesteppingWorker extends Worker {
        @Override
        public void dispose() {
        }

        @Override
        public boolean isDisposed() {
            return false;
        }

        @Override
        public Disposable schedule(Runnable run, long delay, TimeUnit unit) {
            run.run();
            return Disposable.disposed();
        }

        @Override
        public long now(TimeUnit unit) {
            return TimesteppingScheduler.this.now(unit);
        }
    }

    public long time;

    public boolean stepEnabled = true;

    @Override
    public Worker createWorker() {
        return new TimesteppingWorker();
    }

    @Override
    public long now(TimeUnit unit) {
        if (stepEnabled) {
            return time++;
        }
        return time;
    }
}
