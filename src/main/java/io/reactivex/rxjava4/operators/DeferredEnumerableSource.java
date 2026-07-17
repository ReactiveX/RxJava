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

import java.util.concurrent.CompletionStage;

/// Represents an unknown length, deferred iterable source which can be moved forward synchronously
///  and obtain the current item via a simple call once it reports said iterable elements are ready
/// to be consumed.
/// <p>
/// No {@code hasNext} and {@code next} duplication. C# IEnumerator is way better in this regard.
/// @param <T> the element type of the source
/// @see IndexableSource
/// @since 4.0.0
public interface DeferredEnumerableSource<T> extends EnumerableSource<T> {

    /**
     * Returns true if the source is ready to be consumed via its
     * {@link EnumerableSource#nextSync()} and {@link EnumerableSource#current()}
     * methods.
     * @return the completion stage that indicates an empty {@code false} or a non-empty
     *         {@code true} enumerable source is now available
     */
    CompletionStage<Boolean> enumerableReady();
}
