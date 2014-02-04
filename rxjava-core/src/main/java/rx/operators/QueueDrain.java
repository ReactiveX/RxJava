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
package rx.operators;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import rx.Scheduler;
import rx.Subscription;
import rx.subscriptions.CompositeSubscription;
import rx.subscriptions.MultipleAssignmentSubscription;
import rx.util.functions.Action0;
import rx.util.functions.Action1;

/**
 * Action queue ensuring that only a single drain caller succeeds at a time.
 * This class can be used to execute work without the issues of reentrancy and
 * concurrency.
 */
public final class QueueDrain implements Runnable, Action0 {
    /** The number of work items. */
    private final AtomicInteger wip = new AtomicInteger();
    /** The action queue. */
    private final BlockingQueue<Action0> queue = new LinkedBlockingQueue<Action0>();
    /** The subscription to stop the queue processing. */
    private final Subscription k;
    /**
     * Constructor which takes a cancellation token.
     * @param k the cancellation token (aka subscription).
     */
    public QueueDrain(Subscription k) {
        this.k = k;
    }
    /**
     * Enqueue an action.
     * To execute any queued action, call {@link #tryDrain()} or
     * submit this instance to a {@code Scheduler.schedule()} method.
     * @param action the action to enqueue, not null
     */
    public void enqueue(Action0 action) {
        if (!isUnsubscribed()) {
            queue.add(action);
        }
    }
    /**
     * Try draining the queue and executing the actions in it.
     */
    public void tryDrain() {
        if (wip.incrementAndGet() > 1 || isUnsubscribed()) {
            return;
        }
        do {
            queue.poll().call();
        } while (wip.decrementAndGet() > 0 && !isUnsubscribed());
    }
    /**
     * Try draining the queue on the given scheduler.
     * The method ensures that only one thread is actively draining the
     * queue on the given scheduler.
     * @param scheduler the scheduler where the draining should happen
     * @param cs the composite subscription to track the schedule
     */
    public void tryDrainAsync(Scheduler scheduler, final CompositeSubscription cs) {
        if (wip.incrementAndGet() > 1 || isUnsubscribed()) {
            return;
        }
        // add tracking subscription only if schedule is run to avoid overfilling cs
        final MultipleAssignmentSubscription mas = new MultipleAssignmentSubscription();
        cs.add(mas);
        mas.set(scheduler.schedule(new Action1<Scheduler.Inner>() {
            @Override
            public void call(Scheduler.Inner o) {
                if (!isUnsubscribed()) {
                    do {
                        queue.poll().call();
                    } while (wip.decrementAndGet() > 0 && !isUnsubscribed());
                }
                cs.remove(mas);
            }
        }));
    }
    /** Check for unsubscription status. */
    private boolean isUnsubscribed() {
        return k.isUnsubscribed();
    }
    @Override
    public void run() {
        // to help the draining of the queue on a ThreadPool/Scheduler
        tryDrain();
    }

    @Override
    public void call() {
        tryDrain();
    }
    
}
