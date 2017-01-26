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

package io.reactivex.functions;

import io.reactivex.annotations.NonNull;

/**
 * A functional interface (callback) that returns true or false for the given input values.
 * @param <T1> the first value
 * @param <T2> the second value
 */
public interface BiPredicate<T1, T2> {

    /**
     * Test the given input values and return a boolean.
     * @param t1 the first value
     * @param t2 the second value
     * @return the boolean result
     * @throws Exception on error
     */
    boolean test(@NonNull T1 t1, @NonNull T2 t2) throws Exception;
}
