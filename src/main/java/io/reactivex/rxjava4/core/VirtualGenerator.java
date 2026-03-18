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

/**
 * Interface to implement to produce elements when asked by
 * {@link Flowable#virtaulCreate(VirtualGenerator, java.util.concurrent.ExecutorService)}.
 * <p>
 * To signal {@code onComplete}, return normally from {@link #generate(VirtualEmitter)}.
 * To signal {@code onError}, throw any exception from {@link #generate(VirtualEmitter)}.
 * @param <T> the element type generated
 * @since 4.0.0
 */
@FunctionalInterface
public interface VirtualGenerator<T> {

    /**
     * The method to implement and start emitting items.
     * @param emitter use {@link VirtualEmitter#emit(Object)} to generate values
     * @throws Throwable if the generator wishes to signal {@code onError}.
     */
    void generate(VirtualEmitter<T> emitter) throws Throwable;
}