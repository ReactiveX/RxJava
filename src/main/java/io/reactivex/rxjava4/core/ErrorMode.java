/*
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

package io.reactivex.rxjava4.core;

/**
 * Indicates when an error from the any of the involved sources should be handled.
 * <p>
 * Usually appears with {@code concat} and {@code concatMap} operators where the outer and inner source(s)
 * may error out in the middle of streaming and the user would like to finish the current source before
 * cancelling the rest and signaling the error(s) to the consumers.
 * @since 4.0.0
 */
public enum ErrorMode {
    /** Report the error immediately, cancelling the active sources. */
    IMMEDIATE,
    /** Report error after an inner source terminated. */
    BOUNDARY,
    /** Report the error after all sources terminated. */
    END
}
