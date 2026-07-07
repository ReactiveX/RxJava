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

package io.reactivex.rxjava4.disposables;

/**
 * Common interface to add and remove {@link Disposable}s from a container.
 * @since 2.0
 */
public interface DisposableContainer extends Disposable, StreamerCancellation {

    /**
     * Removes all contained {@link Disposable}s without disposing them, making this
     * container fresh.
     * @since 4.0.0
     */
    void reset();

    /**
     * Removes and disposes all contained {@link Disposable}s, making this container fresh
     * without disposing the entire container.
     */
    void clear();

    /**
     * The container implementation that just ignores everything, for
     * cases where the dispose signal has no side effects to work with.
     * @since 4.0.0
     */
    DisposableContainer NEVER = new NeverDisposableContainer();

}
