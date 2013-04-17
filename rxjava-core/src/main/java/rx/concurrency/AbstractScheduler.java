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
import rx.util.functions.Func1;
import rx.util.functions.Func2;

/**
 * Default implementations of various convenience overload methods on the Scheduler.
 * <p>
 * The methods left to implement are:
 * <ul>
 * <li>{@code <T> Subscription schedule(T state, Func2<Scheduler, T, Subscription> action, long delayTime, TimeUnit unit)}</li>
 * <li>{@code <T> Subscription schedule(T state, Func2<Scheduler, T, Subscription> action)}</li>
 * </ul>
 * <p>
 * This is a utility class expected to be used by all {@link Scheduler} implementations since we can't yet rely on Java 8 default methods on the {@link Scheduler}.
 */
public abstract class AbstractScheduler implements Scheduler {

    @Override
    public Subscription schedule(final Action0 action) {
        return schedule(null, new Func2<Scheduler, Void, Subscription>() {

            @Override
            public Subscription call(Scheduler scheduler, Void t2) {
                action.call();
                return Subscriptions.empty();
            }
        });
    }

    @Override
    public Subscription schedule(final Func1<Scheduler, Subscription> action) {
        return schedule(null, new Func2<Scheduler, Void, Subscription>() {

            @Override
            public Subscription call(Scheduler scheduler, Void t2) {
                return action.call(scheduler);
            }
        });
    }

    @Override
    public Subscription schedule(final Action0 action, long delayTime, TimeUnit unit) {
        return schedule(null, new Func2<Scheduler, Void, Subscription>() {

            @Override
            public Subscription call(Scheduler scheduler, Void t2) {
                action.call();
                return Subscriptions.empty();
            }
        }, delayTime, unit);
    }

    @Override
    public Subscription schedule(final Func1<Scheduler, Subscription> action, long delayTime, TimeUnit unit) {
        return schedule(null, new Func2<Scheduler, Void, Subscription>() {

            @Override
            public Subscription call(Scheduler scheduler, Void t2) {
                return action.call(scheduler);
            }
        }, delayTime, unit);
    }

    @Override
    public Subscription schedule(final Func0<Subscription> action) {
        return schedule(null, new Func2<Scheduler, Void, Subscription>() {

            @Override
            public Subscription call(Scheduler scheduler, Void t2) {
                return action.call();
            }
        });
    }

    @Override
    public Subscription schedule(final Func0<Subscription> action, long delayTime, TimeUnit unit) {
        return schedule(null, new Func2<Scheduler, Void, Subscription>() {

            @Override
            public Subscription call(Scheduler scheduler, Void t2) {
                return action.call();
            }
        }, delayTime, unit);
    }

    @Override
    public long now() {
        return System.nanoTime();
    }

}
