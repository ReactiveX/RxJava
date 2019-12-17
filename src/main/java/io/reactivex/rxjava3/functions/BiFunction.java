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

package io.reactivex.rxjava3.functions;

import io.reactivex.rxjava3.annotations.NonNull;

/**
 * A functional interface (callback) that computes a value based on multiple input values.
 * @param <T1> the first value type
 * @param <T2> the second value type
 * @param <R> the result type
 */
@FunctionalInterface
public interface BiFunction<T1, T2, R> {

    /**
     * Calculate a value based on the input values.
     * @param t1 the first value
     * @param t2 the second value
     * @return the result value
     * @throws Throwable if the implementation wishes to throw any type of exception
     */
    @NonNull
    R apply(@NonNull T1 t1, @NonNull T2 t2) throws Throwable;
}
