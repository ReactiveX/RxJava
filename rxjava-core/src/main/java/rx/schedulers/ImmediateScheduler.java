/**
 * Copyright 2014 Netflix, Inc.
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
import rx.functions.Action0;
import rx.subscriptions.BooleanSubscription;
import rx.subscriptions.Subscriptions;

/**
 * Executes work immediately on the current thread.
 */
public final class ImmediateScheduler extends Scheduler {
    private static final ImmediateScheduler INSTANCE = new ImmediateScheduler();

    /* package */static ImmediateScheduler instance() {
        return INSTANCE;
    }

    /* package accessible for unit tests */ImmediateScheduler() {
    }

    @Override
    public Worker createWorker() {
        return new InnerImmediateScheduler();
    }

    private class InnerImmediateScheduler extends Scheduler.Worker implements Subscription {

        final BooleanSubscription innerSubscription = new BooleanSubscription();

        @Override
        public Subscription schedule(Action0 action, long delayTime, TimeUnit unit) {
            // since we are executing immediately on this thread we must cause this thread to sleep
            long execTime = ImmediateScheduler.this.now() + unit.toMillis(delayTime);

            return schedule(new SleepingAction(action, this, execTime));
        }

        @Override
        public Subscription schedule(Action0 action) {
            action.call();
            return Subscriptions.empty();
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
