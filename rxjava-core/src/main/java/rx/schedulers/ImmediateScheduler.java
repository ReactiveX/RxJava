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

import java.util.concurrent.TimeUnit;

import rx.Scheduler;
import rx.Subscription;
import rx.functions.Action1;
import rx.subscriptions.BooleanSubscription;

/**
 * Executes work immediately on the current thread.
 */
public final class ImmediateScheduler extends Scheduler {
    private static final ImmediateScheduler INSTANCE = new ImmediateScheduler();

    /**
     * @deprecated Use Schedulers.immediate();
     * @return
     */
    @Deprecated
    public static ImmediateScheduler getInstance() {
        return INSTANCE;
    }

    /* package */static ImmediateScheduler instance() {
        return INSTANCE;
    }

    /* package accessible for unit tests */ImmediateScheduler() {
    }

    @Override
    public Subscription schedule(Action1<Scheduler.Inner> action) {
        InnerImmediateScheduler inner = new InnerImmediateScheduler();
        inner.schedule(action);
        return inner.innerSubscription;
    }

    @Override
    public Subscription schedule(Action1<Inner> action, long delayTime, TimeUnit unit) {
        InnerImmediateScheduler inner = new InnerImmediateScheduler();
        inner.schedule(action, delayTime, unit);
        return inner.innerSubscription;
    }

    private class InnerImmediateScheduler extends Scheduler.Inner implements Subscription {

        final BooleanSubscription innerSubscription = new BooleanSubscription();

        @Override
        public void schedule(Action1<Scheduler.Inner> action, long delayTime, TimeUnit unit) {
            // since we are executing immediately on this thread we must cause this thread to sleep
            long execTime = now() + unit.toMillis(delayTime);

            schedule(new SleepingAction(action, ImmediateScheduler.this, execTime));
        }

        @Override
        public void schedule(Action1<Scheduler.Inner> action) {
            action.call(this);
        }

        @Override
        public void unsubscribe() {
            innerSubscription.unsubscribe();
        }

        @Override
        public boolean isUnsubscribed() {
            return innerSubscription.isUnsubscribed();
        }

    }

}
