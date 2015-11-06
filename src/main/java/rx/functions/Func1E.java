/**
 * Copyright 2014 Netflix, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package rx.functions;

import rx.annotations.Experimental;

/**
 * Represents a functional interface that accepts a value and returns another value
 * or throws.
 *
 * @param <T> the input value type
 * @param <R> the output value type
 * @param <E> the exception type
 * 
 * @since experimental 
 */
@Experimental
public interface Func1E<T, R, E extends Exception> extends Function {
    /**
     * Applies this function to the given argument.
     *
     * @param t the function argument
     * @return the function result
     * @throws E the exception of the function
     */
    R call(T t) throws E;
}
