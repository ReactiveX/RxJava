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
package rx.schedulers;

import java.util.concurrent.*;

import rx.*;
import rx.annotations.Experimental;
import rx.functions.Action0;
import rx.internal.schedulers.*;
import rx.plugins.RxJavaPlugins;
import rx.subscriptions.CompositeSubscription;

/**
 * Static factory methods for creating Schedulers.
 */
public final class Schedulers {

    private final Scheduler computationScheduler;
    private final Scheduler ioScheduler;
    private final Scheduler newThreadScheduler;

    private static final Schedulers INSTANCE = new Schedulers();

    private Schedulers() {
        Scheduler c = RxJavaPlugins.getInstance().getSchedulersHook().getComputationScheduler();
        if (c != null) {
            computationScheduler = c;
        } else {
            computationScheduler = new EventLoopsScheduler();
        }

        Scheduler io = RxJavaPlugins.getInstance().getSchedulersHook().getIOScheduler();
        if (io != null) {
            ioScheduler = io;
        } else {
            ioScheduler = new CachedThreadScheduler();
        }

        Scheduler nt = RxJavaPlugins.getInstance().getSchedulersHook().getNewThreadScheduler();
        if (nt != null) {
            newThreadScheduler = nt;
        } else {
            newThreadScheduler = NewThreadScheduler.instance();
        }
    }

    /**
     * Creates and returns a {@link Scheduler} that executes work immediately on the current thread.
     * 
     * @return an {@link ImmediateScheduler} instance
     */
    public static Scheduler immediate() {
        return ImmediateScheduler.instance();
    }

    /**
     * Creates and returns a {@link Scheduler} that queues work on the current thread to be executed after the
     * current work completes.
     * 
     * @return a {@link TrampolineScheduler} instance
     */
    public static Scheduler trampoline() {
        return TrampolineScheduler.instance();
    }

    /**
     * Creates and returns a {@link Scheduler} that creates a new {@link Thread} for each unit of work.
     * <p>
     * Unhandled errors will be delivered to the scheduler Thread's {@link java.lang.Thread.UncaughtExceptionHandler}.
     *
     * @return a {@link NewThreadScheduler} instance
     */
    public static Scheduler newThread() {
        return INSTANCE.newThreadScheduler;
    }

    /**
     * Creates and returns a {@link Scheduler} intended for computational work.
     * <p>
     * This can be used for event-loops, processing callbacks and other computational work.
     * <p>
     * Do not perform IO-bound work on this scheduler. Use {@link #io()} instead.
     * <p>
     * Unhandled errors will be delivered to the scheduler Thread's {@link java.lang.Thread.UncaughtExceptionHandler}.
     *
     * @return a {@link Scheduler} meant for computation-bound work
     */
    public static Scheduler computation() {
        return INSTANCE.computationScheduler;
    }

    /**
     * Creates and returns a {@link Scheduler} intended for IO-bound work.
     * <p>
     * The implementation is backed by an {@link Executor} thread-pool that will grow as needed.
     * <p>
     * This can be used for asynchronously performing blocking IO.
     * <p>
     * Do not perform computational work on this scheduler. Use {@link #computation()} instead.
     * <p>
     * Unhandled errors will be delivered to the scheduler Thread's {@link java.lang.Thread.UncaughtExceptionHandler}.
     *
     * @return a {@link Scheduler} meant for IO-bound work
     */
    public static Scheduler io() {
        return INSTANCE.ioScheduler;
    }

    /**
     * Creates and returns a {@code TestScheduler}, which is useful for debugging. It allows you to test
     * schedules of events by manually advancing the clock at whatever pace you choose.
     *
     * @return a {@code TestScheduler} meant for debugging
     */
    public static TestScheduler test() {
        return new TestScheduler();
    }

    /**
     * Converts an {@link Executor} into a new Scheduler instance.
     *
     * @param executor
     *          the executor to wrap
     * @return the new Scheduler wrapping the Executor
     */
    public static Scheduler from(Executor executor) {
        return new ExecutorScheduler(executor);
    }
    /**
     * Submit an Action0 to the specified executor service with the option to interrupt the task
     * on unsubscription and add it to a parent composite subscription.
     * @param executor the target executor service
     * @param action the action to execute
     * @param parent if not {@code null} the subscription representing the action is added to this composite with logic to remove it
     *            once the action completes or is unsubscribed.
     * @param interruptOnUnsubscribe if {@code false}, unsubscribing the task will not interrupt the task if it is running
     * @return the Subscription representing the scheduled action which is also added to the {@code parent} composite
     */
    @Experimental
    public static Subscription submitTo(ExecutorService executor, Action0 action, CompositeSubscription parent, boolean interruptOnUnsubscribe) {
        ScheduledAction sa = new ScheduledAction(action, interruptOnUnsubscribe);
        
        if (parent != null) {
            parent.add(sa);
            sa.addParent(parent);
        }
        
        Future<?> f = executor.submit(sa);
        sa.add(f);
        
        return sa;
    }
    /**
     * Submit an Action0 to the specified executor service with the given delay and the option to interrupt the task
     * on unsubscription and add it to a parent composite subscription.
     * @param executor the target executor service
     * @param action the action to execute
     * @param delay the delay value
     * @param unit the time unit of the delay value
     * @param parent if not {@code null} the subscription representing the action is added to this composite with logic to remove it
     *            once the action completes or is unsubscribed.
     * @param interruptOnUnsubscribe if {@code false}, unsubscribing the task will not interrupt the task if it is running
     * @return the Subscription representing the scheduled action which is also added to the {@code parent} composite
     */
    @Experimental
    public static Subscription submitTo(ScheduledExecutorService executor, Action0 action, long delay, TimeUnit unit, CompositeSubscription parent, boolean interruptOnUnsubscribe) {
        ScheduledAction sa = new ScheduledAction(action, interruptOnUnsubscribe);
        
        if (parent != null) {
            parent.add(sa);
            sa.addParent(parent);
        }
        
        Future<?> f = executor.schedule(sa, delay, unit);
        sa.add(f);
        
        return sa;
    }}
