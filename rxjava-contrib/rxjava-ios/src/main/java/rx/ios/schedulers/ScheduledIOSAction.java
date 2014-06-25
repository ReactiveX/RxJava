/**
 * Copyright 2014 Netflix, Inc.
 * Copyright 2014 Ashley Williams
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

package rx.ios.schedulers;

import org.robovm.apple.foundation.NSBlockOperation;
import org.robovm.apple.foundation.NSOperationQueue;
import rx.Subscription;
import rx.functions.Action0;
import rx.subscriptions.CompositeSubscription;

import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

/**
 * Based on {@code ScheduledAction} - A {@code Runnable} that executes an {@code Action0}
 * that can be cancelled.
 */
final class ScheduledIOSAction implements Runnable, Subscription {
    final CompositeSubscription cancel;
    final Action0 action;
    NSBlockOperation nsBlockOperation;
    final NSOperationQueue operationQueue;
    volatile int once;
    static final AtomicIntegerFieldUpdater<ScheduledIOSAction> ONCE_UPDATER
            = AtomicIntegerFieldUpdater.newUpdater(ScheduledIOSAction.class, "once");

    public ScheduledIOSAction(Action0 action, NSOperationQueue operationQueue) {
        this.action = action;
        this.operationQueue = operationQueue;
        this.cancel = new CompositeSubscription();

        nsBlockOperation = new NSBlockOperation();
    }

    @Override
    public void run() {
        try {

            final Runnable actionRunner = new Runnable() {
                @Override
                public void run() {
                    action.call();
                }
            };

            nsBlockOperation.addExecutionBlock$(actionRunner);

            /* Add operation to operation queue*/
            operationQueue.addOperation(nsBlockOperation);

        } finally {
            unsubscribe();
        }
    }

    @Override
    public boolean isUnsubscribed() {
        return cancel.isUnsubscribed();
    }

    @Override
    public void unsubscribe() {
        if (ONCE_UPDATER.compareAndSet(this, 0, 1)) {
            nsBlockOperation.cancel();
            cancel.unsubscribe();
            System.err.println("cancelled");
        }
    }

    /**
     * Adds a {@code Subscription} to the {@link CompositeSubscription} to be later cancelled on unsubscribe
     *
     * @param s subscription to add
     */
    public void add(Subscription s) {
        cancel.add(s);
    }

    /**
     * Adds a parent {@link rx.subscriptions.CompositeSubscription} to this {@code ScheduledIOSAction} so when
     * the action is cancelled or terminates, it can remove itself from this parent
     * @param parent the parent {@code CompositeSubscription} to add
     */
    public void addParent(CompositeSubscription parent) {
        cancel.add(new Remover(this, parent));
    }


    /**
     * Remove a child subscription from a composite when unsubscribing
     */
    private static final class Remover implements Subscription {
        final Subscription s;
        final CompositeSubscription parent;
        volatile int once;
        static final AtomicIntegerFieldUpdater<Remover> ONCE_UPDATER
                = AtomicIntegerFieldUpdater.newUpdater(Remover.class, "once");

        public Remover(Subscription s, CompositeSubscription parent) {
            this.s = s;
            this.parent = parent;
        }

        @Override
        public boolean isUnsubscribed() {
            return s.isUnsubscribed();
        }

        @Override
        public void unsubscribe() {
            if (ONCE_UPDATER.compareAndSet(this, 0, 1)) {
                parent.remove(s);
            }
        }
    }
}