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
 * A functional interface (callback) that accepts two values (of possibly different types).
 * @param <T1> the first value type
 * @param <T2> the second value type
 */
@FunctionalInterface
public interface BiConsumer<@NonNull T1, @NonNull T2> {

    /**
     * Performs an operation on the given values.
     * @param t1 the first value
     * @param t2 the second value
     * @throws Throwable if the implementation wishes to throw any type of exception
     */
    void accept(T1 t1, T2 t2) throws Throwable;
}
