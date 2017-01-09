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

package io.reactivex.internal.util;

/**
 * Indicates when an error from the main source should be reported.
 */
public enum ErrorMode {
    /** Report the error immediately, cancelling the active inner source. */
    IMMEDIATE,
    /** Report error after an inner source terminated. */
    BOUNDARY,
    /** Report the error after all sources terminated. */
    END
}
