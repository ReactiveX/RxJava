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

import io.reactivex.rxjava4.disposables.Disposable;

/**
 * Interface called by the {@link Flowable#virtualTransform(VirtualTransformer, java.util.concurrent.ExecutorService)}
 * operator to generate any number of output values based of the current input of the upstream.
 *
 * @param <T> the source value type
 * @param <R> the result value type
 * @since 4.0.0
 */
@FunctionalInterface
public interface VirtualTransformer<T, R> {

    /**
     * Implement this method to generate any number of items via
     * {@link VirtualEmitter#emit(Object)}.
     * 
     * @param value the upstream value
     * @param emitter the emitter to use to generate result value(s)
     * @param stopper call to stop the upstream
     * @throws Throwable signaled as {@code onError} for the downstream.
     */
    void transform(T value, VirtualEmitter<R> emitter, Disposable stopper) throws Throwable;
}