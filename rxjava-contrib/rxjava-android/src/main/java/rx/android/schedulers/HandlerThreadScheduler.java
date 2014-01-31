/**
 * Copyright 2013 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package rx.android.schedulers;

import java.util.concurrent.TimeUnit;

import rx.Scheduler;
import rx.Subscription;
import rx.operators.SafeObservableSubscription;
import rx.util.functions.Func2;
import android.os.Handler;

/**
 * Schedules actions to run on an Android Handler thread.
 */
public class HandlerThreadScheduler extends Scheduler {

    private final Handler handler;

    /**
     * Constructs a {@link HandlerThreadScheduler} using the given {@link Handler}
     * @param handler {@link Handler} to use when scheduling actions
     */
    public HandlerThreadScheduler(Handler handler) {
        this.handler = handler;
    }

    /**
     * Calls {@link HandlerThreadScheduler#schedule(Object, rx.util.functions.Func2, long, java.util.concurrent.TimeUnit)}
     * with a delay of zero milliseconds.
     *
     * See {@link #schedule(Object, rx.util.functions.Func2, long, java.util.concurrent.TimeUnit)}
     */
    @Override
    public <T> Subscription schedule(final T state, final Func2<? super Scheduler, ? super T, ? extends Subscription> action) {
        return schedule(state, action, 0L, TimeUnit.MILLISECONDS);
    }

    /**
     * Calls {@link Handler#postDelayed(Runnable, long)} with a runnable that executes the given action.
     * @param state
     *            State to pass into the action.
     * @param action
     *            Action to schedule.
     * @param delayTime
     *            Time the action is to be delayed before executing.
     * @param unit
     *            Time unit of the delay time.
     * @return A Subscription from which one can unsubscribe from.
     */
    @Override
    public <T> Subscription schedule(final T state, final Func2<? super Scheduler, ? super T, ? extends Subscription> action, long delayTime, TimeUnit unit) {
        final SafeObservableSubscription subscription = new SafeObservableSubscription();
        final Scheduler _scheduler = this;
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                subscription.wrap(action.call(_scheduler, state));
            }
        }, unit.toMillis(delayTime));
        return subscription;
    }
}


