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
 * Interface to map/wrap a downstream observer to an upstream observer.
 */
@FunctionalInterface
public interface CompletableOperator {
    /**
     * Applies a function to the child {@link CompletableObserver} and returns a new parent {@code CompletableObserver}.
     * @param observer the child {@code CompletableObserver} instance
     * @return the parent {@code CompletableObserver} instance
     * @throws Throwable on failure
     */
    @NonNull
    CompletableObserver apply(@NonNull CompletableObserver observer) throws Throwable;
}
