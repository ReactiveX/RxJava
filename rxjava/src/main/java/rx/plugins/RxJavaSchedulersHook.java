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

import rx.Scheduler;
import rx.functions.Action0;

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

    protected RxJavaSchedulersHook() {

    }

    private final static RxJavaSchedulersHook DEFAULT_INSTANCE = new RxJavaSchedulersHook();

    /**
     * Scheduler to return from {@link rx.schedulers.Schedulers#computation()} or null if default should be
     * used.
     *
     * This instance should be or behave like a stateless singleton;
     */
    public Scheduler getComputationScheduler() {
        return null;
    }

    /**
     * Scheduler to return from {@link rx.schedulers.Schedulers#io()} or null if default should be used.
     *
     * This instance should be or behave like a stateless singleton;
     */
    public Scheduler getIOScheduler() {
        return null;
    }

    /**
     * Scheduler to return from {@link rx.schedulers.Schedulers#newThread()} or null if default should be used.
     *
     * This instance should be or behave like a stateless singleton;
     */
    public Scheduler getNewThreadScheduler() {
        return null;
    }

    /**
     * Invoked before the Action is handed over to the scheduler.  Can be used for wrapping/decorating/logging.
     * The default is just a passthrough.
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
