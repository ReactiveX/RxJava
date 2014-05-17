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
package rx.android.schedulers;

import java.util.concurrent.TimeUnit;

import rx.Scheduler;
import rx.Subscription;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.subscriptions.BooleanSubscription;
import rx.subscriptions.Subscriptions;
import android.os.Handler;

/**
 * Schedules actions to run on an Android Handler thread.
 */
public class HandlerThreadScheduler extends Scheduler {

    private final Handler handler;
    private final boolean immediate;

    /**
     * Constructs a {@link HandlerThreadScheduler} using the given {@link Handler}
     * @param handler
     *            {@link Handler} to use when scheduling actions
     * @param immediate
     *            if true, immediate actions scheduled from the same Thread will be called directly
     *            without posting to {@link Handler}
     */
    public HandlerThreadScheduler(Handler handler, boolean immediate) {
        this.handler = handler;
        this.immediate = immediate;
    }

    /**
     * Constructs a {@link HandlerThreadScheduler} using the given {@link Handler} and
     * {@link Handler}
     * 
     * @param handler
     *            {@link Handler} to use when scheduling actions
     */
    public HandlerThreadScheduler(Handler handler) {
        this(handler, false);
    }

    @Override
    public Worker createWorker() {
        return new InnerHandlerThreadScheduler(handler, immediate);
    }
    
    private static class InnerHandlerThreadScheduler extends Worker {

        private final Handler handler;
        private final boolean immediate;
        private BooleanSubscription innerSubscription = new BooleanSubscription();

        public InnerHandlerThreadScheduler(Handler handler, boolean immediate) {
            this.handler = handler;
            this.immediate = immediate;
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
        public Subscription schedule(final Action0 action, long delayTime, TimeUnit unit) {
            final Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    if (isUnsubscribed()) {
                        return;
                    }
                    action.call();
                }
            };
            handler.postDelayed(runnable, unit.toMillis(delayTime));
            return Subscriptions.create(new Action0() {

                @Override
                public void call() {
                    handler.removeCallbacks(runnable);
                    
                }
                
            });
        }

        @Override
        public Subscription schedule(final Action0 action) {
            if (immediate && handler.getLooper().getThread() == Thread.currentThread()) {
                action.call();
                return Subscriptions.empty();
            } else {
                return schedule(action, 0, TimeUnit.MILLISECONDS);
            }
        }

    }

}
