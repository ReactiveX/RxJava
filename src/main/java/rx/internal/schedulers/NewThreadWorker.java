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

import java.lang.reflect.Method;
import java.util.concurrent.*;

import rx.*;
import rx.functions.Action0;
import rx.plugins.*;
import rx.subscriptions.Subscriptions;

/**
 * @warn class description missing
 */
public class NewThreadWorker extends Scheduler.Worker implements Subscription {
    private final ScheduledExecutorService executor;
    private final RxJavaSchedulersHook schedulersHook;
    volatile boolean isUnsubscribed;

    /* package */
    public NewThreadWorker(ThreadFactory threadFactory) {
        executor = Executors.newScheduledThreadPool(1, threadFactory);
        // Java 7+: cancelled future tasks can be removed from the executor thus avoiding memory leak
        for (Method m : executor.getClass().getMethods()) {
            if (m.getName().equals("setRemoveOnCancelPolicy")
                    && m.getParameterTypes().length == 1
                    && m.getParameterTypes()[0] == Boolean.TYPE) {
                try {
                    m.invoke(executor, true);
                } catch (Exception ex) {
                    RxJavaPlugins.getInstance().getErrorHandler().handleError(ex);
                }
                break;
            }
        }
        schedulersHook = RxJavaPlugins.getInstance().getSchedulersHook();
    }

    @Override
    public Subscription schedule(final Action0 action) {
        return schedule(action, 0, null);
    }

    @Override
    public Subscription schedule(final Action0 action, long delayTime, TimeUnit unit) {
        if (isUnsubscribed) {
            return Subscriptions.unsubscribed();
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
        Action0 decoratedAction = schedulersHook.onSchedule(action);
        ScheduledAction run = new ScheduledAction(decoratedAction);
        Future<?> f;
        if (delayTime <= 0) {
            f = executor.submit(run);
        } else {
            f = executor.schedule(run, delayTime, unit);
        }
        run.add(f);

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
