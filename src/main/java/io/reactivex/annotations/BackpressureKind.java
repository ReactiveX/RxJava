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

/**
 * Enumeration for various kinds of backpressure support.
 * @since 2.0
 */
public enum BackpressureKind {
    /**
     * The backpressure-related requests pass through this operator without change.
     */
    PASS_THROUGH,
    /**
     * The operator fully supports backpressure and may coordinate downstream requests
     * with upstream requests through batching, arbitration or by other means.
     */
    FULL,
    /**
     * The operator performs special backpressure management; see the associated javadoc.
     */
    SPECIAL,
    /**
     * The operator requests Long.MAX_VALUE from upstream but respects the backpressure
     * of the downstream.
     */
    UNBOUNDED_IN,
    /**
     * The operator will emit a MissingBackpressureException if the downstream didn't request
     * enough or in time.
     */
    ERROR,
    /**
     * The operator ignores all kinds of backpressure and may overflow the downstream.
     */
    NONE
}
