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

package io.reactivex.rxjava4.operators;

import io.reactivex.rxjava4.annotations.NonNull;
import io.reactivex.rxjava4.core.Streamer;

/// Represents a source which can be accessed via a zero-based index synchronously,
/// without going through the usual [Streamer#next()] calls to obtain the next item.
/// @param <T> the element type of the source
/// @since 4.0.0
public interface IndexableSource<T> {

    /**
     * Obtain an element from the given index.
     * Make sure you read only up to {@link #limit()}
     * @param index the index
     * @return the element at the specified index
     * @throws Throwable if the indexed access involves computation that can throw
     */
    @NonNull
    T elementAt(long index) throws Throwable;

    /**
     * Returns the limit of how many items can be obtained via [{@link #elementAt(long)}.
     * @return the index limit, exclusive
     */
    long limit();
}
