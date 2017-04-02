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
package io.reactivex.flowables;

import io.reactivex.Flowable;
import io.reactivex.annotations.Nullable;

/**
 * A {@link Flowable} that has been grouped by key, the value of which can be obtained with {@link #getKey()}.
 * <p>
 * <em>Note:</em> A {@link GroupedFlowable} will cache the items it is to emit until such time as it
 * is subscribed to. For this reason, in order to avoid memory leaks, you should not simply ignore those
 * {@code GroupedObservable}s that do not concern you. Instead, you can signal to them that they
 * may discard their buffers by applying an operator like {@link Flowable#take take}{@code (0)} to them.
 *
 * @param <K>
 *            the type of the key
 * @param <T>
 *            the type of the items emitted by the {@code GroupedFlowable}
 * @see Flowable#groupBy(io.reactivex.functions.Function)
 * @see <a href="http://reactivex.io/documentation/operators/groupby.html">ReactiveX documentation: GroupBy</a>
 */
public abstract class GroupedFlowable<K, T> extends Flowable<T> {

    final K key;

    /**
     * Constructs a GroupedFlowable with the given key.
     * @param key the key
     */
    protected GroupedFlowable(@Nullable K key) {
        this.key = key;
    }

    /**
     * Returns the key that identifies the group of items emitted by this {@code GroupedObservable}.
     *
     * @return the key that the items emitted by this {@code GroupedObservable} were grouped by
     */
    @Nullable
    public K getKey() {
        return key;
    }
}
