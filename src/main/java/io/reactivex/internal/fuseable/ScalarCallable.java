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

import java.util.concurrent.Callable;

/**
 * A marker interface indicating that a scalar, constant value
 * is held by the implementing reactive type which can be
 * safely extracted during assembly time can be used for
 * optimization.
 * <p>
 * Implementors of {@link #call()} should not throw any exception.
 * <p>
 * Design note: the interface extends {@link Callable} because if a scalar
 * is safe to extract during assembly time, it is also safe to extract at
 * subscription time or later. This allows optimizations to deal with such
 * single-element sources uniformly.
 * <p>
 * @param <T> the scalar value type held by the implementing reactive type
 */
public interface ScalarCallable<T> extends Callable<T> {

    // overridden to remove the throws Exception
    @Override
    T call();
}
