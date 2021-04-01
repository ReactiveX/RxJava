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

package io.reactivex.rxjava3.core;

import io.reactivex.rxjava3.annotations.NonNull;

/**
 * Represents a basic {@link Single} source base interface,
 * consumable via an {@link SingleObserver}.
 * <p>
 * This class also serves the base type for custom operators wrapped into
 * Single via {@link Single#create(SingleOnSubscribe)}.
 *
 * @param <T> the element type
 * @since 2.0
 */
@FunctionalInterface
public interface SingleSource<@NonNull T> {

    /**
     * Subscribes the given {@link SingleObserver} to this {@link SingleSource} instance.
     * @param observer the {@code SingleObserver}, not {@code null}
     * @throws NullPointerException if {@code observer} is {@code null}
     */
    void subscribe(@NonNull SingleObserver<? super T> observer);
}
