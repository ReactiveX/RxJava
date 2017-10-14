/**
 * Copyright (c) 2016-present, RxJava Contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See
 * the License for the specific language governing permissions and limitations under the License.
 */

package io.reactivex.annotations;

import java.lang.annotation.*;

import io.reactivex.schedulers.Schedulers;

/**
 * Indicates what kind of scheduler the class or method uses.
 * <p>
 * Constants are provided for instances from {@link Schedulers} as well as values for
 * {@linkplain #NONE not using a scheduler} and {@linkplain #CUSTOM a manually-specified scheduler}.
 * Libraries providing their own values should namespace them with their base package name followed
 * by a colon ({@code :}) and then a human-readable name (e.g., {@code com.example:ui-thread}).
 * @since 2.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface SchedulerSupport {
    /**
     * A special value indicating the operator/class doesn't use schedulers.
     */
    String NONE = "none";
    /**
     * A special value indicating the operator/class requires a scheduler to be manually specified.
     */
    String CUSTOM = "custom";

    // Built-in schedulers:
    /**
     * The operator/class runs on RxJava's {@linkplain Schedulers#computation() computation
     * scheduler} or takes timing information from it.
     */
    String COMPUTATION = "io.reactivex:computation";
    /**
     * The operator/class runs on RxJava's {@linkplain Schedulers#io() I/O scheduler} or takes
     * timing information from it.
     */
    String IO = "io.reactivex:io";
    /**
     * The operator/class runs on RxJava's {@linkplain Schedulers#newThread() new thread scheduler}
     * or takes timing information from it.
     */
    String NEW_THREAD = "io.reactivex:new-thread";
    /**
     * The operator/class runs on RxJava's {@linkplain Schedulers#trampoline() trampoline scheduler}
     * or takes timing information from it.
     */
    String TRAMPOLINE = "io.reactivex:trampoline";
    /**
     * The operator/class runs on RxJava's {@linkplain Schedulers#single() single scheduler}
     * or takes timing information from it.
     * @since 2.0.8 - experimental
     */
    @Experimental
    String SINGLE = "io.reactivex:single";

    /**
     * The kind of scheduler the class or method uses.
     * @return the name of the scheduler the class or method uses
     */
    String value();
}
