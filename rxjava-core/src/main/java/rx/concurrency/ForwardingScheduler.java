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

import rx.Scheduler;
import rx.Subscription;
import rx.util.functions.Action0;
import rx.util.functions.Func0;

import java.util.concurrent.TimeUnit;

public class ForwardingScheduler implements Scheduler {
    private final Scheduler underlying;

    public ForwardingScheduler(Scheduler underlying) {
        this.underlying = underlying;
    }

    @Override
    public Subscription schedule(Action0 action) {
        return underlying.schedule(action);
    }

    @Override
    public Subscription schedule(Func0<Subscription> action) {
        return underlying.schedule(action);
    }

    @Override
    public Subscription schedule(Action0 action, long dueTime, TimeUnit unit) {
        return underlying.schedule(action, dueTime, unit);
    }

    @Override
    public Subscription schedule(Func0<Subscription> action, long dueTime, TimeUnit unit) {
        return underlying.schedule(action, dueTime, unit);
    }

    @Override
    public long now() {
        return underlying.now();
    }
}