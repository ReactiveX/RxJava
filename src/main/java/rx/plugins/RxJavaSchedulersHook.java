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

package rx.plugins;

import java.util.concurrent.ThreadFactory;
import rx.Scheduler;
import rx.annotations.Experimental;
import rx.functions.Action0;
import rx.internal.schedulers.CachedThreadScheduler;
import rx.internal.schedulers.EventLoopsScheduler;
import rx.internal.schedulers.NewThreadScheduler;
import rx.internal.util.RxThreadFactory;
import rx.schedulers.Schedulers;

/**
 * This plugin class provides 2 ways to customize {@link Scheduler} functionality
 * 1.  You may redefine entire schedulers, if you so choose.  To do so, override
 * the 3 methods that return Scheduler (io(), computation(), newThread()).
 * 2.  You may wrap/decorate an {@link Action0}, before it is handed off to a Scheduler.  The system-
 * supplied Schedulers (Schedulers.ioScheduler, Schedulers.computationScheduler,
 * Scheduler.newThreadScheduler) all use this hook, so it's a convenient way to
 * modify Scheduler functionality without redefining Schedulers wholesale.
 *
 * Also, when redefining Schedulers, you are free to use/not use the onSchedule decoration hook.
 * <p>
 * See {@link RxJavaPlugins} or the RxJava GitHub Wiki for information on configuring plugins:
 * <a href="https://github.com/ReactiveX/RxJava/wiki/Plugins">https://github.com/ReactiveX/RxJava/wiki/Plugins</a>.
 */
public class RxJavaSchedulersHook {

    /**
     * Create an instance of the default {@link Scheduler} used for {@link Schedulers#computation()}.
     * @return the created Scheduler instance
     */
    @Experimental
    public static Scheduler createComputationScheduler() {
        return createComputationScheduler(new RxThreadFactory("RxComputationScheduler-"));
    }

    /**
     * Create an instance of the default {@link Scheduler} used for {@link Schedulers#computation()}
     * except using {@code threadFactory} for thread creation.
     * @param threadFactory the factory to use for each worker thread
     * @return the created Scheduler instance
     */
    @Experimental
    public static Scheduler createComputationScheduler(ThreadFactory threadFactory) {
        if (threadFactory == null) throw new NullPointerException("threadFactory == null");
        return new EventLoopsScheduler(threadFactory);
    }

    /**
     * Create an instance of the default {@link Scheduler} used for {@link Schedulers#io()}.
     * @return the created Scheduler instance
     */
    @Experimental
    public static Scheduler createIoScheduler() {
        return createIoScheduler(new RxThreadFactory("RxIoScheduler-"));
    }

    /**
     * Create an instance of the default {@link Scheduler} used for {@link Schedulers#io()}
     * except using {@code threadFactory} for thread creation.
     * @param threadFactory the factory to use for each worker thread
     * @return the created Scheduler instance
     */
    @Experimental
    public static Scheduler createIoScheduler(ThreadFactory threadFactory) {
        if (threadFactory == null) throw new NullPointerException("threadFactory == null");
        return new CachedThreadScheduler(threadFactory);
    }

    /**
     * Create an instance of the default {@link Scheduler} used for {@link Schedulers#newThread()}.
     * @return the created Scheduler instance
     */
    @Experimental
    public static Scheduler createNewThreadScheduler() {
        return createNewThreadScheduler(new RxThreadFactory("RxNewThreadScheduler-"));
    }

    /**
     * Create an instance of the default {@link Scheduler} used for {@link Schedulers#newThread()}
     * except using {@code threadFactory} for thread creation.
     * @param threadFactory the factory to use for each worker thread
     * @return the created Scheduler instance
     */
    @Experimental
    public static Scheduler createNewThreadScheduler(ThreadFactory threadFactory) {
        if (threadFactory == null) throw new NullPointerException("threadFactory == null");
        return new NewThreadScheduler(threadFactory);
    }

    protected RxJavaSchedulersHook() {

    }

    private final static RxJavaSchedulersHook DEFAULT_INSTANCE = new RxJavaSchedulersHook();

    /**
     * Scheduler to return from {@link rx.schedulers.Schedulers#computation()} or null if default should be
     * used.
     *
     * This instance should be or behave like a stateless singleton;
     * @return the current computation scheduler instance
     */
    public Scheduler getComputationScheduler() {
        return null;
    }

    /**
     * Scheduler to return from {@link rx.schedulers.Schedulers#io()} or null if default should be used.
     *
     * This instance should be or behave like a stateless singleton;
     * @return the created Scheduler instance
     */
    public Scheduler getIOScheduler() {
        return null;
    }

    /**
     * Scheduler to return from {@link rx.schedulers.Schedulers#newThread()} or null if default should be used.
     *
     * This instance should be or behave like a stateless singleton;
     * @return the current new thread scheduler instance
     */
    public Scheduler getNewThreadScheduler() {
        return null;
    }

    /**
     * Invoked before the Action is handed over to the scheduler.  Can be used for wrapping/decorating/logging.
     * The default is just a pass through.
     * @param action action to schedule
     * @return wrapped action to schedule
     */
    public Action0 onSchedule(Action0 action) {
        return action;
    }

    public static RxJavaSchedulersHook getDefaultInstance() {
        return DEFAULT_INSTANCE;
    }
}
