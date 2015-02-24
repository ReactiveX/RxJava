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

import java.util.concurrent.Future;
import java.util.concurrent.atomic.*;

import rx.Subscription;
import rx.exceptions.OnErrorNotImplementedException;
import rx.functions.Action0;
import rx.internal.util.SubscriptionList;
import rx.plugins.RxJavaPlugins;
import rx.subscriptions.CompositeSubscription;

/**
 * A {@code Runnable} that executes an {@code Action0} and can be cancelled. The analog is the
 * {@code Subscriber} in respect of an {@code Observer}.
 * <p><b>System-wide properties:</b>
 * <ul>
 * <li>{@code rx.scheduler.interrupt-on-unsubscribe}
 * <dd>Use {@code Future.cancel(true)} to interrupt a running action? {@code "true"} (default) or {@code "false"}.</br>
 * </li>
 * </ul>
 */
public final class ScheduledAction extends AtomicReference<Thread> implements Runnable, Subscription {
    /** */
    private static final long serialVersionUID = -3962399486978279857L;
    static final boolean DEFAULT_INTERRUPT_ON_UNSUBSCRIBE;
    static final String KEY_INTERRUPT_ON_UNSUBSCRIBE = "rx.scheduler.interrupt-on-unsubscribe";
    static {
        String interruptOnUnsubscribeValue = System.getProperty(KEY_INTERRUPT_ON_UNSUBSCRIBE);
        DEFAULT_INTERRUPT_ON_UNSUBSCRIBE = interruptOnUnsubscribeValue == null || "true".equals(interruptOnUnsubscribeValue);
    }
    final SubscriptionList cancel;
    final Action0 action;
    volatile int interruptOnUnsubscribe;
    static final AtomicIntegerFieldUpdater<ScheduledAction> INTERRUPT_ON_UNSUBSCRIBE
        = AtomicIntegerFieldUpdater.newUpdater(ScheduledAction.class, "interruptOnUnsubscribe");

    public ScheduledAction(Action0 action) {
        this(action, DEFAULT_INTERRUPT_ON_UNSUBSCRIBE);
    }
    
    public ScheduledAction(Action0 action, boolean interruptOnUnsubscribe) {
        this.action = action;
        this.cancel = new SubscriptionList();
        this.interruptOnUnsubscribe = interruptOnUnsubscribe ? 1 : 0;
    }

    @Override
    public void run() {
        try {
            lazySet(Thread.currentThread());
            action.call();
        } catch (Throwable e) {
            // nothing to do but print a System error as this is fatal and there is nowhere else to throw this
            IllegalStateException ie = null;
            if (e instanceof OnErrorNotImplementedException) {
                ie = new IllegalStateException("Exception thrown on Scheduler.Worker thread. Add `onError` handling.", e);
            } else {
                ie = new IllegalStateException("Fatal Exception thrown on Scheduler.Worker thread.", e);
            }
            RxJavaPlugins.getInstance().getErrorHandler().handleError(ie);
            Thread thread = Thread.currentThread();
            thread.getUncaughtExceptionHandler().uncaughtException(thread, ie);
        } finally {
            unsubscribe();
        }
    }

    /**
     * Sets the flag to indicate the underlying Future task should be interrupted on unsubscription or not.
     * @param interrupt the new interruptible status
     */
    public void setInterruptOnUnsubscribe(boolean interrupt) {
        INTERRUPT_ON_UNSUBSCRIBE.lazySet(this, interrupt ? 1 : 0);
    }
    /**
     * Returns {@code true} if the underlying Future task will be interrupted on unsubscription.
     * @return the current interruptible status
     */
    public boolean isInterruptOnUnsubscribe() {
        return interruptOnUnsubscribe != 0;
    }
    
    @Override
    public boolean isUnsubscribed() {
        return cancel.isUnsubscribed();
    }

    @Override
    public void unsubscribe() {
        cancel.unsubscribe();
    }

    /**
     * Adds a general Subscription to this {@code ScheduledAction} that will be unsubscribed
     * if the underlying {@code action} completes or the this scheduled action is cancelled.
     *
     * @param s the Subscription to add
     */
    public void add(Subscription s) {
        cancel.add(s);
    }

    /**
     * Adds the given Future to the unsubscription composite in order to support
     * cancelling the underlying task in the executor framework.
     * @param f the future to add
     */
    public void add(final Future<?> f) {
        add(new FutureCompleter(f));
    }
    
    /**
     * Adds a parent {@link CompositeSubscription} to this {@code ScheduledAction} so when the action is
     * cancelled or terminates, it can remove itself from this parent.
     *
     * @param parent
     *            the parent {@code CompositeSubscription} to add
     */
    public void addParent(CompositeSubscription parent) {
        add(new Remover(this, parent));
    }

    /**
     * Cancels the captured future if the caller of the call method
     * is not the same as the runner of the outer ScheduledAction to
     * prevent unnecessary self-interrupting if the unsubscription
     * happens from the same thread.
     */
    private final class FutureCompleter implements Subscription {
        private final Future<?> f;

        private FutureCompleter(Future<?> f) {
            this.f = f;
        }

        @Override
        public void unsubscribe() {
            if (ScheduledAction.this.get() != Thread.currentThread()) {
                f.cancel(interruptOnUnsubscribe != 0);
            } else {
                f.cancel(false);
            }
        }
        @Override
        public boolean isUnsubscribed() {
            return f.isCancelled();
        }
    }

    /** Remove a child subscription from a composite when unsubscribing. */
    private static final class Remover extends AtomicBoolean implements Subscription {
        /** */
        private static final long serialVersionUID = 247232374289553518L;
        final Subscription s;
        final CompositeSubscription parent;

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
            if (compareAndSet(false, true)) {
                parent.remove(s);
            }
        }

    }
}
