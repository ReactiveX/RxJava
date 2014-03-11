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
package rx.android.schedulers;

import android.os.Handler;
import android.os.Looper;
import rx.Scheduler;
import rx.Subscription;
import rx.functions.Action1;
import rx.subscriptions.BooleanSubscription;

import java.util.concurrent.TimeUnit;

/**
 * Schedules actions to run on an Android Handler thread.
 */
public class HandlerThreadScheduler extends Scheduler {

    private final Handler handler;

    /**
     * Constructs a {@link HandlerThreadScheduler} using the given {@link Handler}
     *
     * @param handler
     *            {@link Handler} to use when scheduling actions
     */
    public HandlerThreadScheduler(Handler handler) {
        this.handler = handler;
    }

    /**
     * Calls {@link HandlerThreadScheduler#schedule(rx.functions.Action1, long, java.util.concurrent.TimeUnit)} with a delay of zero milliseconds.
     *
     * See {@link HandlerThreadScheduler#schedule(rx.functions.Action1, long, java.util.concurrent.TimeUnit)}
     */
    @Override
    public Subscription schedule(Action1<Inner> action) {
        return schedule(action, 0, TimeUnit.MILLISECONDS);
    }

    /**
     * Calls {@link Handler#postDelayed(Runnable, long)} with a runnable that executes the given action.
     *
     * @param action
     *            Action to schedule.
     * @param delayTime
     *            Time the action is to be delayed before executing.
     * @param unit
     *            Time unit of the delay time.
     * @return A Subscription from which one can unsubscribe from.
     */
    @Override
    public Subscription schedule(Action1<Inner> action, long delayTime, TimeUnit unit) {
        InnerHandlerThreadScheduler inner = new InnerHandlerThreadScheduler(handler);
        inner.schedule(action, delayTime, unit);
        return inner;
    }

    private static class InnerHandlerThreadScheduler extends Inner {

        private final Handler handler;
        private BooleanSubscription innerSubscription = new BooleanSubscription();
        private Inner _inner = this;

        public InnerHandlerThreadScheduler(Handler handler) {
            this.handler = handler;
        }

        @Override
        public void unsubscribe() {
            innerSubscription.unsubscribe();
        }

        @Override
        public boolean isUnsubscribed() {
            return innerSubscription.isUnsubscribed();
        }

        @Override
        public void schedule(final Action1<Inner> action, final long delayTime, final TimeUnit unit) {
            final long millis = unit.toMillis(delayTime);

            if (millis == 0 && handler.getLooper() == Looper.myLooper()) {
                action.call(_inner);
                return;
            }

            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (!_inner.isUnsubscribed()) {
                        action.call(_inner);
                    }
                }
            }, unit.toMillis(delayTime));
        }

        @Override
        public void schedule(final Action1<Inner> action) {
            schedule(action, 0, TimeUnit.MILLISECONDS);
        }

    }

}
