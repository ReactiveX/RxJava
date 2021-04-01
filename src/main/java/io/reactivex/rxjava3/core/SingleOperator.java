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
 * Interface to map/wrap a downstream {@link SingleObserver} to an upstream {@code SingleObserver}.
 *
 * @param <Downstream> the value type of the downstream
 * @param <Upstream> the value type of the upstream
 */
@FunctionalInterface
public interface SingleOperator<@NonNull Downstream, @NonNull Upstream> {
    /**
     * Applies a function to the child {@link SingleObserver} and returns a new parent {@code SingleObserver}.
     * @param observer the child {@code SingleObserver} instance
     * @return the parent {@code SingleObserver} instance
     * @throws Throwable on failure
     */
    @NonNull
    SingleObserver<? super Upstream> apply(@NonNull SingleObserver<? super Downstream> observer) throws Throwable;
}
