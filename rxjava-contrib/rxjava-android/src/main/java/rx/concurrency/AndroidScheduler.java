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

import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import rx.Scheduler;
import rx.Subscription;
import rx.concurrency.ExecutorScheduler;
import rx.util.AtomicObservableSubscription;
import rx.util.functions.Func2;

import android.os.Handler;
import android.os.Looper;

/**
 * Executes work on the Android UI thread.
 * This scheduler should only be used to update the ui.
 */
public class AndroidScheduler extends Scheduler {

    private static final AndroidScheduler INSTANCE = new AndroidScheduler();

    public static AndroidScheduler getInstance() {
        return INSTANCE;
    }

    private AndroidScheduler() {
    }

    @Override
    public <T> Subscription schedule(final T state, final Func2<Scheduler, T, Subscription> action) {
        final AtomicObservableSubscription subscription = new AtomicObservableSubscription();
        final Scheduler _scheduler = this;

        Handler handler = new Handler(Looper.getMainLooper());

        handler.post(new Runnable() {
            @Override
            public void run() {
                subscription.wrap(action.call(_scheduler, state));
            }
        });

        return subscription;
    }

    @Override
    public <T> Subscription schedule(T state, Func2<Scheduler, T, Subscription> action, long dueTime, TimeUnit unit) {
        return null;
    }


}

