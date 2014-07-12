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
import rx.subscriptions.Subscriptions;

import java.util.Iterator;
import java.util.concurrent.*;

/**
 * A {@code Runnable} that is used by Schedulers to schedule actions to be run on
 * a single thread in a serialized order.
 */
public class NewThreadWorker extends Scheduler.Worker implements Subscription, Runnable {
    private volatile boolean isUnsubscribed;
    private final BlockingQueue<ScheduledAction> actionQueue;
    private final Thread workerThread;

    /* package */
    public NewThreadWorker(ThreadFactory threadFactory) {
        actionQueue = new LinkedBlockingQueue<ScheduledAction>();
        workerThread = threadFactory.newThread(this);
        workerThread.start();
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
     * @param action the action to be scheduled and called
     * @param delayTime how long to wait before the action should be called
     * @param unit time unit of the delay before calling the action
     * @return A ScheduledAction instance that will execute the action and can be cancelled
     */
    public ScheduledAction scheduleActual(final Action0 action, long delayTime, TimeUnit unit) {
        final ScheduledAction run = new ScheduledAction(action);
        if (delayTime <= 0) {
            actionQueue.offer(run);
        } else {
            Future<?> f = GenericScheduledExecutorService.getInstance().schedule(new Runnable() {
                @Override
                public void run() {
                if (!run.isUnsubscribed()) {
                    actionQueue.offer(run);
                }
                }
            }, delayTime, unit);
            run.add(Subscriptions.from(f));
        }

        return run;
    }

    @Override
    public void unsubscribe() {
        isUnsubscribed = true;

        workerThread.interrupt();

        Iterator<ScheduledAction> scheduleActionIterator = actionQueue.iterator();
        while (scheduleActionIterator.hasNext()) {
            ScheduledAction scheduledAction = scheduleActionIterator.next();
            scheduleActionIterator.remove();
            scheduledAction.unsubscribe();
        }
    }

    @Override
    public boolean isUnsubscribed() {
        return isUnsubscribed;
    }

    @Override
    public void run() {
        while (!isUnsubscribed) {
            try {
                ScheduledAction scheduledAction = actionQueue.take();
                if (scheduledAction != null && !scheduledAction.isUnsubscribed()) {
                    scheduledAction.run();
                }
            } catch (InterruptedException ignored) {}
        }
    }
}
