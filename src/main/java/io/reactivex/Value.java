/**
 * Copyright 2016 Netflix, Inc.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See
 * the License for the specific language governing permissions and limitations under the License.
 */

package io.reactivex;

/**
 * An observable value.
 *
 * @param <T> the value type
 */
public interface Value<T> {

    /**
     * Return current value, eventually blocks until value is present
     *
     * @return current value
     */
    T getValue();

    /**
     * Check if value is present
     *
     * @return true if value is present, false otherwise
     */
    boolean hasValue();

    /**
     * Return this value as observable.
     *
     * @return the value observable
     */
    Observable<T> asObservable();
}
