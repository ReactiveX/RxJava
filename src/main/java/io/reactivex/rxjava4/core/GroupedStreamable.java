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

package io.reactivex.rxjava4.core;

import io.reactivex.rxjava4.annotations.*;

/**
 * An {@link Streamable} that has been grouped by key, the value of which can be obtained with {@link #getKey()}.
 * @param <K>
 *            the type of the key, can be null
 * @param <T>
 *            the type of the items emitted by the {@code GroupedStreamable}
 * @see Streamable#groupBy(io.reactivex.rxjava4.functions.Function)
 * @see <a href="http://reactivex.io/documentation/operators/groupby.html">ReactiveX documentation: GroupBy</a>
 * @since 4.0.0
 */
public abstract class GroupedStreamable<@Nullable K, @NonNull T> implements Streamable<T> {

    final K key;

    /**
     * Constructs a GroupedStreamable with the given key.
     * @param key the key
     */
    protected GroupedStreamable(@Nullable K key) {
        this.key = key;
    }

    /**
     * Returns the key that identifies the group of items emitted by this {@code GroupedStreamable}.
     *
     * @return the key that the items emitted by this {@code GroupedStreamable} were grouped by
     */
    @Nullable
    public K getKey() {
        return key;
    }

}
