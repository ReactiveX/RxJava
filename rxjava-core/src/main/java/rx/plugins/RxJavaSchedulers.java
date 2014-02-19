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
import rx.functions.Func0;

/**
 * Define alternate Scheduler implementations to be returned by the `Schedulers` factory methods.
 * <p>
 * See {@link RxJavaPlugins} or the RxJava GitHub Wiki for information on configuring plugins: <a
 * href="https://github.com/Netflix/RxJava/wiki/Plugins">https://github.com/Netflix/RxJava/wiki/Plugins</a>.
 */
public abstract class RxJavaSchedulers {

    /**
     * Factory of Scheduler to return from {@link Schedulers.computation()} or null if default should be used.
     */
    public abstract Func0<Scheduler> getComputationScheduler();

    /**
     * Factory of Scheduler to return from {@link Schedulers.io()} or null if default should be used.
     */
    public abstract Func0<Scheduler> getIOScheduler();

    /**
     * Factory of Scheduler to return from {@link Schedulers.newThread()} or null if default should be used.
     */
    public abstract Func0<Scheduler> getNewThreadScheduler();
}
