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
package rx.schedulers;

import java.util.concurrent.Future;
import java.util.concurrent.atomic.*;

import rx.Subscription;
import rx.exceptions.OnErrorNotImplementedException;
import rx.functions.Action0;
import rx.plugins.RxJavaPlugins;
import rx.subscriptions.CompositeSubscription;

/**
 * A {@code Runnable} that executes an {@code Action0}, allows associating resources with it and can be unsubscribed.
 * <p><b>System-wide properties:</b>
 * <ul>
 * <li>{@code io.reactivex.scheduler.interrupt-on-unsubscribe}
 * <dd>Use {@code Future.cancel(true)} to interrupt a running action? {@code "true"} (default) or {@code "false"}.</br>
 * </li>
 * </ul>
 * <h1>Usage</h1>
 * The main focus of this class to be submitted to an {@link java.util.concurrent.ExecutorService} and
 * to manage the cancellation with the returned {@link java.util.concurrent.Future} and (optionally)
 * with a parent {@link rx.subscriptions.CompositeSubscription}.
 * <p>
 * For example, when a new {@link rx.Scheduler.Worker} is to be implemented upon a custom scheduler,
 * the following set of calls need to happen in order to ensure the cancellation and tracking requirements
 * of the {@code Scheduler.Worker} is met:
 * <code><pre>
 * final CompositeSubscription parent = new CompositeSubscription();
 * final ExecutorService executor = ...
 * //...
 * &#64;Override
 * public Subscription schedule(Action0 action) {
 *     ScheduledAction sa = new ScheduledAction(action);
 *     
 *     // setup the tracking relation between the worker and the action
 *     parent.add(sa);
 *     sa.addParent(parent);
 *     
 *     // schedule the action
 *     Future<?> f = executor.submit(sa);
 *     
 *     // link the future with the action
 *     sa.add(f);
 *     
 *     return sa;
 * }
 * &#64;Override
 * public void unsubscribe() {
 *     // this will cancel all pending or running tasks
 *     parent.unsubscribe(); 
 * }
 * </pre></code>
 * <p>
 * Depening on the lifecycle of the {@code ExecutorService}, one can avoid tracking the {@code ScheduledAction}s
 * in case the service is shut down in the {@code unsubscribe()} call via {@code executor.shutdownNow()} since
 * this call cancels all pending tasks and interrupts running tasks. The implementation simplifies to the following:
 * <code><pre>
 * final ExecutorService executor = ...
 * //...
 * &#64;Override
 * public Subscription schedule(Action0 action) {
 *     ScheduledAction sa = new ScheduledAction(action);
 *     
 *     // schedule the action
 *     Future<?> f = executor.submit(sa);
 *     
 *     // link the future with the action
 *     sa.add(f);
 *     
 *     return sa;
 * }
 * &#64;Override
 * public void unsubscribe() {
 *     // this will cancel all pending or running tasks
 *     executor.shutdownNow();
 * }
 * </pre></code> 
 * <p>
 * In case a {@code Scheduler} implementation wants to decorate an existing scheduler, it becomes necessary
 * to track {@code ScheduledAction}s on the decorator's level. The following code demonstrates
 * the way of doing it:
 * <code><pre>
 * final CompositeSubscription outerParent = new CompositeSubscription();
 * final Scheduler.Worker actualWorker = ...
 * // ...
 * &#64;Override
 * public Subscription schedule(Action0 action) {
 *     Subscription s = actualWorker.schedule(action);
 *     
 *     // verify if it is indeed a Scheduled action
 *     if (s instanceof ScheduledAction) {
 *         ScheduledAction sa = (ScheduledAction)s;
 *         // order here is important: add the action first
 *         outerParent.add(sa);
 *         // so in case it has already finished, this addParent() will remove it immediately
 *         sa.addParent(outerParent);
 *     } else {
 *         throw new IllegalStateException("The worker didn't return a ScheduledAction");
 *     }
 *     
 *     return s;
 * }
 * &#64;Override
 * public void unsubscribe() {
 *     outerParent.unsubscribe();
 *     // release the actual worker in some way
 *     // actualWorker.unsubscribe();
 *     // somePool.returnObject(actualWorker);
 * }
 * </pre></code>
 * Note, however, if the {@code actualWorker} above didn't return a ScheduledAction, there is no
 * good way of untracking the returned {@code Subscription} (i.e., when to call {@code outerParent.remove(s)}).
 */
public final class ScheduledAction implements Runnable, Subscription {
    /** Indicates if the ScheduledActions should be interrupted if cancelled from another thread. */
    static final boolean INTERRUPT_ON_UNSUBSCRIBE;
    /** Key to the INTERRUPT_ON_UNSUBSCRIBE flag. */
    static final String KEY_INTERRUPT_ON_UNSUBSCRIBE = "io.reactivex.scheduler.interrupt-on-unsubscribe";
    static {
        String value = System.getProperty(KEY_INTERRUPT_ON_UNSUBSCRIBE);
        INTERRUPT_ON_UNSUBSCRIBE = value == null || "true".equalsIgnoreCase(value);
    }
    /** The composite to allow unsubscribing associated resources. */
    final CompositeSubscription cancel;
    /** The actual action to call. */
    final Action0 action;
    /** Holds the thread executing the action. Using the highest order bit to indicate if unsubscribe should interrupt or not. */
    volatile long thread;
    /** Updater to the {@link #thread} field. */
    static final AtomicLongFieldUpdater<ScheduledAction> THREAD
        = AtomicLongFieldUpdater.newUpdater(ScheduledAction.class, "thread");
    /** Flag to allow interrupts. */
    static final long ALLOW_CANCEL_INTERRUPT = Long.MIN_VALUE;
    /**
     * Creates a new instance of ScheduledAction by wrapping an existing Action0 instance
     * and allows interruption on unsubscription.
     * @param action the action to wrap
     */
    public ScheduledAction(Action0 action) {
        this(action, INTERRUPT_ON_UNSUBSCRIBE);
    }
    /**
     * Creates a new instance of ScheduledAction by wrapping an existing Action0 instance
     * and with the interrupt policy.
     * @param action the action to wrap
     * @param interruptOnUnsubscribe allows interrupting the action when unsubscribe happens
     * from a different thread the action is running on
     */
    public ScheduledAction(Action0 action, boolean interruptOnUnsubscribe) {
        this.action = action;
        this.cancel = new CompositeSubscription();
        THREAD.lazySet(this, interruptOnUnsubscribe ? ALLOW_CANCEL_INTERRUPT : 0);
    }

    @Override
    public void run() {
        try {
            saveCurrentThread();
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

    @Override
    public boolean isUnsubscribed() {
        return cancel.isUnsubscribed();
    }

    @Override
    public void unsubscribe() {
        if (!cancel.isUnsubscribed()) {
            cancel.unsubscribe();
        }
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
        cancel.add(new FutureCompleter(f));
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
    
    /**
     * Sets the interrupt-on-unsubscribe policy for this ScheduledAction.
     * @param allow {@code true} if unsubscribing this action from another thread should
     * interrupt the thread the action is running on. 
     */
    public void setInterruptOnUnsubscribe(boolean allow) {
        for (;;) {
            long t = thread;
            long u = allow ? (t | ALLOW_CANCEL_INTERRUPT) : (t & ~ALLOW_CANCEL_INTERRUPT);
            if (THREAD.compareAndSet(this, t, u)) {
                break;
            }
        }
    }
    /**
     * Returns the current state of the interrupt-on-unsubscribe policy.
     * @return {@code true} if unsubscribing this action from another thread will
     * interrupt the thread the action is running on
     */
    public boolean getInterruptOnUnsubscribe() {
        return (thread & ALLOW_CANCEL_INTERRUPT) != 0;
    }
    
    /** Atomically sets the current thread identifier and preserves the interruption allowed flag. */
    private void saveCurrentThread() {
        long current = Thread.currentThread().getId();
        for (;;) {
            long t = thread;
            long u = current | (t & ALLOW_CANCEL_INTERRUPT);
            if (THREAD.compareAndSet(this, t, u)) {
                break;
            }
        }
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
            long t = thread;
            long current = Thread.currentThread().getId();
            if ((t & (~ALLOW_CANCEL_INTERRUPT)) != current) {
                f.cancel((t & ALLOW_CANCEL_INTERRUPT) != 0L);
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
