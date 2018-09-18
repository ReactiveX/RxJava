/**
 * Copyright (c) 2016-present, RxJava Contributors.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See
 * the License for the specific language governing permissions and limitations under the License.
 */

package io.reactivex.schedulers;

import io.reactivex.annotations.*;
import io.reactivex.functions.Function;
import io.reactivex.plugins.RxJavaPlugins;

/**
 * Interface to indicate the implementor class wraps a {@code Runnable} that can
 * be accessed via {@link #getWrappedRunnable()}.
 * <p>
 * You can check if a {@link Runnable} task submitted to a {@link io.reactivex.Scheduler Scheduler} (or its
 * {@link io.reactivex.Scheduler.Worker Scheduler.Worker}) implements this interface and unwrap the
 * original {@code Runnable} instance. This could help to avoid hooking the same underlying {@code Runnable}
 * task in a custom {@link RxJavaPlugins#onSchedule(Runnable)} hook set via
 * the {@link RxJavaPlugins#setScheduleHandler(Function)} method multiple times due to internal delegation
 * of the default {@code Scheduler.scheduleDirect} or {@code Scheduler.Worker.schedule} methods.
 * <p>History: 2.1.7 - experimental
 * @since 2.2
 */
public interface SchedulerRunnableIntrospection {

    /**
     * Returns the wrapped action.
     *
     * @return the wrapped action. Cannot be null.
     */
    @NonNull
    Runnable getWrappedRunnable();
}
