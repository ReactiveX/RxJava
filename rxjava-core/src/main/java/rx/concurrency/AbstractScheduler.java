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
import java.util.concurrent.atomic.AtomicBoolean;

import rx.Scheduler;
import rx.Subscription;
import rx.subscriptions.Subscriptions;
import rx.util.functions.Action0;
import rx.util.functions.Func0;
import rx.util.functions.Func1;
import rx.util.functions.Func2;

/* package */abstract class AbstractScheduler implements Scheduler {

    @Override
    public Subscription schedule(Action0 action) {
        return schedule(asFunc0(action));
    }

    @Override
    public Subscription schedule(final Func1<Scheduler, Subscription> action) {
        return schedule(func0ForwardingToFunc1(action));
    }

    @Override
    public <T> Subscription schedule(final T state, final Func2<Scheduler, T, Subscription> action) {
      return schedule(func0ForwardingToFunc2(action, state));
    }

    @Override
    public Subscription schedule(Action0 action, long dueTime, TimeUnit unit) {
        return schedule(asFunc0(action), dueTime, unit);
    }

    @Override
    public Subscription schedule(final Func1<Scheduler, Subscription> action, long dueTime, TimeUnit unit) {
        return schedule(func0ForwardingToFunc1(action), dueTime, unit);
    }

    @Override
    public <T> Subscription schedule(final T state, final Func2<Scheduler, T, Subscription> action, long dueTime, TimeUnit unit) {
        return schedule(func0ForwardingToFunc2(action, state), dueTime, unit);
    }
    
    @Override
    public Subscription schedulePeriodically(Action0 action, long initialDelay, long period, TimeUnit unit) {
        return schedulePeriodically(asFunc0(action), initialDelay, period, unit);
    }

    /**
     * This default implementation schedules recursively and waits for actions to complete (instead of potentially executing
     * long-running actions concurrently). Each scheduler that can do periodic scheduling in a better way should override this.
     */
    @Override
    public Subscription schedulePeriodically(final Func0<Subscription> action, long initialDelay, final long period, final TimeUnit unit) {
        final long periodInNanos = unit.toNanos(period);
        final AtomicBoolean complete = new AtomicBoolean();

        final Func0<Subscription> recursiveAction = new Func0<Subscription>() {
            @Override
            public Subscription call() {
                if (! complete.get()) {
                    long startedAt = System.nanoTime();
                    final Subscription sub1 = action.call();
                    long timeTakenByActionInNanos = System.nanoTime() - startedAt;
                    final Subscription sub2 = schedule(this, periodInNanos - timeTakenByActionInNanos, TimeUnit.NANOSECONDS);
                    return Subscriptions.create(new Action0() {
                        @Override
                        public void call() {
                            sub1.unsubscribe();
                            sub2.unsubscribe();
                        }
                    });
                }
                return Subscriptions.empty();
            }
        };
        final Subscription sub = schedule(recursiveAction, initialDelay, unit);
        return Subscriptions.create(new Action0() {
            @Override
            public void call() {
                complete.set(true);
                sub.unsubscribe();
            }
        });
    }

    @Override
    public Subscription schedulePeriodically(Func1<Scheduler, Subscription> action, long initialDelay, long period, TimeUnit unit) {
        return schedulePeriodically(func0ForwardingToFunc1(action), initialDelay, period, unit);
    }

    @Override
    public <T> Subscription schedulePeriodically(T state, Func2<Scheduler, T, Subscription> action, long initialDelay, long period, TimeUnit unit) {
        return schedulePeriodically(func0ForwardingToFunc2(action, state), initialDelay, period, unit);
    }

    @Override
    public long now() {
        return System.nanoTime();
    }

    @SuppressWarnings("static-method") // can't be done, of course, but Eclipse fails at detecting AbstractScheduler.this
    private Func0<Subscription> func0ForwardingToFunc1(final Func1<Scheduler, Subscription> func1) {
        return new Func0<Subscription>() {
            @Override
            public Subscription call() {
                return func1.call(AbstractScheduler.this);
            }
        };
    }

    @SuppressWarnings("static-method") // can't be done, of course, but Eclipse fails at detecting AbstractScheduler.this
    private <T> Func0<Subscription> func0ForwardingToFunc2(final Func2<Scheduler, T, Subscription> func2, final T state) {
        return new Func0<Subscription>() {
            @Override
            public Subscription call() {
                return func2.call(AbstractScheduler.this, state);
            }
        };
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
