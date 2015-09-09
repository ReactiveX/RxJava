/**
 * Copyright 2015 Netflix, Inc.
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

/**
 * Indicates what scheduler the method or class uses by default
 */
public enum SchedulerKind {
    /**
     * The operator/class doesn't use schedulers.
     */
    NONE,
    /**
     * The operator/class runs on the computation scheduler or takes timing information from it.
     */
    COMPUTATION,
    /**
     * The operator/class runs on the io scheduler or takes timing information from it.
     */
    IO,
    /**
     * The operator/class runs on the new thread scheduler or takes timing information from it.
     */
    NEW_THREAD,
    /**
     * The operator/class runs on the trampoline scheduler or takes timing information from it.
     */
    TRAMPOLINE,
    /**
     * The operator/class runs on the single scheduler or takes timing information from it.
     */
    SINGLE,
    /**
     * The operator/class requires a scheduler to be manually specified.
     */
    CUSTOM
}
