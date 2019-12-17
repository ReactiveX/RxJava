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

/**
 * A functional interface (callback) that provides a single value or
 * throws an exception.
 * <p>
 * This interface was added to allow throwing any subclass of {@link Throwable}s,
 * which is not directly possible with the Java standard {@link java.util.concurrent.Callable} interface.
 * @param <T> the value type returned
 * @since 3.0.0
 */
@FunctionalInterface
public interface Supplier<T> {

    /**
     * Produces a value or throws an exception.
     * @return the value produced
     * @throws Throwable if the implementation wishes to throw any type of exception
     */
    T get() throws Throwable;
}
