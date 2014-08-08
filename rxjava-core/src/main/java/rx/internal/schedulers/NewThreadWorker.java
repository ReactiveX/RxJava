/**
 * Copyright 2014 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package rx.internal.schedulers;

import rx.Scheduler;
import rx.Subscription;
import rx.functions.Action0;
import rx.plugins.RxJavaPlugins;
import rx.plugins.RxJavaScheduledRunnableWrapper;
import rx.subscriptions.Subscriptions;

import java.util.concurrent.*;

/**
 * @warn class description missing
 */
public class NewThreadWorker extends Scheduler.Worker implements Subscription {
    private final ScheduledExecutorService executor;
    private final RxJavaScheduledRunnableWrapper scheduledRunnableWrapper;
    volatile boolean isUnsubscribed;

    /* package */
    public NewThreadWorker(ThreadFactory threadFactory) {
        executor = Executors.newScheduledThreadPool(1, threadFactory);

        //plugin-defined strategy for wrapping a Runnable before it gets submitted
        scheduledRunnableWrapper = RxJavaPlugins.getInstance().getScheduledRunnableWrapper();
    }

    @Override
    public Subscription schedule(final Action0 action) {
        return schedule(action, 0, null);
    }

    @Override
    public Subscription schedule(final Action0 action, long delayTime, TimeUnit unit) {
        if (isUnsubscribed) {
            return Subscriptions.empty();
        }
        return scheduleActual(action, delayTime, unit);
    }

    /**
     * @warn javadoc missing
     * @param action
     * @param delayTime
     * @param unit
     * @return
     */
    public ScheduledAction scheduleActual(final Action0 action, long delayTime, TimeUnit unit) {
        ScheduledAction run = new ScheduledAction(action);
        Runnable toSubmit = scheduledRunnableWrapper.getRunnable(run);
        Future<?> f;
        if (delayTime <= 0) {
            f = executor.submit(toSubmit);
        } else {
            f = executor.schedule(toSubmit, delayTime, unit);
        }
        run.add(Subscriptions.from(f));

        return run;
    }

    @Override
    public void unsubscribe() {
        isUnsubscribed = true;
        executor.shutdownNow();
    }

    @Override
    public boolean isUnsubscribed() {
        return isUnsubscribed;
    }
}
