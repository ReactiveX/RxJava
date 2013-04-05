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
import rx.subscriptions.Subscriptions;
import rx.util.functions.Action0;
import rx.util.functions.Func0;

/* package */abstract class AbstractScheduler implements Scheduler {

    @Override
    public Subscription schedule(Action0 action) {
        return schedule(asFunc0(action));
    }

    @Override
    public Subscription schedule(Action0 action, long dueTime, TimeUnit unit) {
        return schedule(asFunc0(action), dueTime, unit);
    }

    @Override
    public long now() {
        return System.nanoTime();
    }

    private static Func0<Subscription> asFunc0(final Action0 action) {
        return new Func0<Subscription>() {
            @Override
            public Subscription call() {
                action.call();
                return Subscriptions.empty();
            }
        };
    }

}
