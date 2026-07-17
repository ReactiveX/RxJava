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

/// Represents an unknown length iterable source which can be moved forward synchronously and obtain the current
/// item via a simple call.
/// <p>
/// No {@code hasNext} and {@code next} duplication. C# IEnumerator is way better in this regard.
/// @param <T> the element type of the source
/// @see IndexableSource
/// @since 4.0.0
public interface EnumerableSource<T> {

    /**
     * Synchronously obtains the next item or returns {@code false} if no
     * more items.
     * @return {@code true} it there is an item available which can be obtained via {@link #current()},
     *         {@code false} if no more items are available
     * @throws Throwable if there is a (processing) error while going to the next item synchronously
     */
    boolean nextSync() throws Throwable;

    /**
     * Returns the current item if {@link #nextSync()} returned {@code true} the previous call.
     * <p>
     * Calling before the first or after exhaustion of the source is an undefined behavior
     * @return the current item
     */
    T current(); // FIXME not sure about the name clash with Streamable.current
}
