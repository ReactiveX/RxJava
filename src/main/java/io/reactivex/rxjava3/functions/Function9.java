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

package io.reactivex.rxjava3.functions;

import io.reactivex.rxjava3.annotations.NonNull;

/**
 * A functional interface (callback) that computes a value based on multiple input values.
 * @param <T1> the first value type
 * @param <T2> the second value type
 * @param <T3> the third value type
 * @param <T4> the fourth value type
 * @param <T5> the fifth value type
 * @param <T6> the sixth value type
 * @param <T7> the seventh value type
 * @param <T8> the eighth value type
 * @param <T9> the ninth value type
 * @param <R> the result type
 */
@FunctionalInterface
public interface Function9<@NonNull T1, @NonNull T2, @NonNull T3, @NonNull T4, @NonNull T5, @NonNull T6, @NonNull T7, @NonNull T8, @NonNull T9, @NonNull R> {
    /**
     * Calculate a value based on the input values.
     * @param t1 the first value
     * @param t2 the second value
     * @param t3 the third value
     * @param t4 the fourth value
     * @param t5 the fifth value
     * @param t6 the sixth value
     * @param t7 the seventh value
     * @param t8 the eighth value
     * @param t9 the ninth value
     * @return the result value
     * @throws Throwable if the implementation wishes to throw any type of exception
     */
    R apply(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6, T7 t7, T8 t8, T9 t9) throws Throwable;
}
