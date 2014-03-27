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
package rx.schedulers;

import rx.Scheduler;
import rx.Scheduler.Inner;
import rx.functions.Action1;

/* package */class SleepingAction implements Action1<Scheduler.Inner> {
    private final Action1<Scheduler.Inner> underlying;
    private final Scheduler scheduler;
    private final long execTime;

    public SleepingAction(Action1<Scheduler.Inner> underlying, Scheduler scheduler, long execTime) {
        this.underlying = underlying;
        this.scheduler = scheduler;
        this.execTime = execTime;
    }

    @Override
    public void call(Inner s) {
        if (s.isUnsubscribed()) {
            return;
        }
        if (execTime > scheduler.now()) {
            long delay = execTime - scheduler.now();
            if (delay > 0) {
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(e);
                }
            }
        }

        // after waking up check the subscription
        if (s.isUnsubscribed()) {
            return;
        }
        underlying.call(s);
    }
}
