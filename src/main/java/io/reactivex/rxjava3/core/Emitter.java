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
package io.reactivex.rxjava3.core;

import io.reactivex.rxjava3.annotations.NonNull;

/**
 * Base interface for emitting signals in a push-fashion in various generator-like source
 * operators (create, generate).
 * <p>
 * Note that the {@link Emitter#onNext}, {@link Emitter#onError} and
 * {@link Emitter#onComplete} methods provided to the function via the {@link Emitter} instance should be called synchronously,
 * never concurrently. Calling them from multiple threads is not supported and leads to an
 * undefined behavior.
 *
 * @param <T> the value type emitted
 */
public interface Emitter<T> {

    /**
     * Signal a normal value.
     * @param value the value to signal, not {@code null}
     */
    void onNext(@NonNull T value);

    /**
     * Signal a {@link Throwable} exception.
     * @param error the {@code Throwable} to signal, not {@code null}
     */
    void onError(@NonNull Throwable error);

    /**
     * Signal a completion.
     */
    void onComplete();
}
