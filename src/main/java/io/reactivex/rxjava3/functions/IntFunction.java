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
 * A functional interface (callback) that takes a primitive value and return value of type T.
 * @param <T> the returned value type
 */
@FunctionalInterface
public interface IntFunction<T> {
    /**
     * Calculates a value based on a primitive integer input.
     * @param i the input value
     * @return the result Object
     * @throws Throwable if the implementation wishes to throw any type of exception
     */
    @NonNull
    T apply(int i) throws Throwable;
}
