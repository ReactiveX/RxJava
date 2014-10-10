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

import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

import rx.Subscription;
import rx.exceptions.OnErrorNotImplementedException;
import rx.functions.Action0;
import rx.plugins.RxJavaPlugins;
import rx.subscriptions.CompositeSubscription;

/**
 * A {@code Runnable} that executes an {@code Action0} and can be cancelled. The analog is the
 * {@code Subscriber} in respect of an {@code Observer}.
 */
public final class ScheduledAction implements Runnable, Subscription {
    final CompositeSubscription cancel;
    final Action0 action;
    volatile int once;
    static final AtomicIntegerFieldUpdater<ScheduledAction> ONCE_UPDATER
            = AtomicIntegerFieldUpdater.newUpdater(ScheduledAction.class, "once");

    public ScheduledAction(Action0 action) {
        this.action = action;
        this.cancel = new CompositeSubscription();
    }

    @Override
    public void run() {
        try {
            action.call();
        } catch (Throwable e) {
            // nothing to do but print a System error as this is fatal and there is nowhere else to throw this
            IllegalStateException ie = null;
            if (e instanceof OnErrorNotImplementedException) {
                ie = new IllegalStateException("Exception thrown on Scheduler.Worker thread. Add `onError` handling.", e);
            } else {
                ie = new IllegalStateException("Fatal Exception thrown on Scheduler.Worker thread.", e);
            }
            ie.printStackTrace();
            RxJavaPlugins.getInstance().getErrorHandler().handleError(ie);
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
            cancel.unsubscribe();
        }
    }

    /**
     * @warn javadoc missing
     *
     * @param s
     * @warn param "s" undescribed
     */
    public void add(Subscription s) {
        cancel.add(s);
    }

    /**
     * Adds a parent {@link CompositeSubscription} to this {@code ScheduledAction} so when the action is
     * cancelled or terminates, it can remove itself from this parent.
     *
     * @param parent
     *            the parent {@code CompositeSubscription} to add
     */
    public void addParent(CompositeSubscription parent) {
        cancel.add(new Remover(this, parent));
    }

    /** Remove a child subscription from a composite when unsubscribing. */
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
