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

package io.reactivex.internal.fuseable;

import io.reactivex.Maybe;

/**
 * Interface indicating an operator implementation can be macro-fused back to Maybe in case
 * the operator goes from Maybe to some other reactive type and then the sequence calls
 * for toMaybe again:
 * <pre>
 * Single&lt;Integer> single = Maybe.just(1).isEmpty();
 * Maybe&lt;Integer> maybe = single.toMaybe();
 * </pre>
 *
 * The {@code Single.toMaybe()} will check for this interface and call the {@link #fuseToMaybe()}
 * to return a Maybe which could be the Maybe-specific implementation of isEmpty().
 * <p>
 * This causes a slight overhead in assembly time (1 instanceof check, 1 operator allocation and 1 dropped
 * operator) but does not incur the conversion overhead at runtime.
 *
 * @param <T> the value type
 */
public interface FuseToMaybe<T> {

    /**
     * Returns a (direct) Maybe for the operator.
     * <p>The implementation should handle the necessary RxJavaPlugins wrapping.
     * @return the Maybe instance
     */
    Maybe<T> fuseToMaybe();
}
