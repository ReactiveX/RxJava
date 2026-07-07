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

import io.reactivex.rxjava4.annotations.NonNull;
import io.reactivex.rxjava4.core.Streamable;

/**
 * Represents non-disposable view of a {@link DisposableContainer}
 * that allows synchronous testing for disposed state as well as allow
 * adding and removing {@link Disposable} resources to be
 * cleaned up when the full container is disposed.
 * <p>
 * This view is provided to prevent calling {@link DisposableContainer#dispose()}
 * in {@link Streamable#stream(StreamerCancellation)} implementations because
 * disposing a stream is the privilege of the caller/downstream.
 * <p>
 * Use the {@link #derive()} to create a sub-container with full disposability access.
 * <p>
 * This interface doesn't support {@link DisposableContainer#reset()} nor
 * {@link DisposableContainer#clear()} because it would allow accidentally removing another
 * operator's added/registered {@code Disposable}s.
 * @since 4.0.0
 */
public interface StreamerCancellation {
    /**
     * Returns true if this resource has been disposed.
     * @return true if this resource has been disposed
     */
    boolean isDisposed();

    /**
     * Adds a disposable to this container or disposes it if the
     * container has been disposed.
     * @param d the disposable to add, not null
     * @return true if successful, false if this container has been disposed
     */
    boolean add(@NonNull Disposable d);

    /**
     * Removes and disposes the given disposable if it is part of this
     * container.
     * @param d the disposable to remove and dispose, not null
     * @return true if the operation was successful
     */
    boolean remove(@NonNull Disposable d);

    /**
     * Removes but does not dispose the given disposable if it is part of this
     * container.
     * @param d the disposable to remove, not null
     * @return true if the operation was successful
     */
    boolean delete(@NonNull Disposable d);

    /**
     * Create a derived sub-container that can get cancelled by this container,
     * but disposing the sub-container does not dispose this container.
     * @return the derived sub-container
     */
    @NonNull
    DisposableContainer derive();

}
