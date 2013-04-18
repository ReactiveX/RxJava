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

import java.util.concurrent.TimeUnit;

import rx.Scheduler;
import rx.Subscription;
import rx.util.functions.Func2;

/* package */class SleepingAction<T> implements Func2<Scheduler, T, Subscription> {
    private final Func2<Scheduler, T, Subscription> underlying;
    private final Scheduler scheduler;
    private final long execTime;

    public SleepingAction(Func2<Scheduler, T, Subscription> underlying, Scheduler scheduler, long timespan, TimeUnit timeUnit) {
        this.underlying = underlying;
        this.scheduler = scheduler;
        this.execTime = scheduler.now() + timeUnit.toMillis(timespan);
    }

    @Override
    public Subscription call(Scheduler s, T state) {
        if (execTime < scheduler.now()) {
            try {
                Thread.sleep(scheduler.now() - execTime);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        }

        return underlying.call(s, state);
    }
}
