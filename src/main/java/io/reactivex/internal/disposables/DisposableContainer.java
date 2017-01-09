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

package io.reactivex.internal.disposables;

import io.reactivex.disposables.Disposable;

/**
 * Common interface to add and remove disposables from a container.
 * @since 2.0
 */
public interface DisposableContainer {

    /**
     * Adds a disposable to this container or disposes it if the
     * container has been disposed.
     * @param d the disposable to add, not null
     * @return true if successful, false if this container has been disposed
     */
    boolean add(Disposable d);

    /**
     * Removes and disposes the given disposable if it is part of this
     * container.
     * @param d the disposable to remove and dispose, not null
     * @return true if the operation was successful
     */
    boolean remove(Disposable d);

    /**
     * Removes (but does not dispose) the given disposable if it is part of this
     * container.
     * @param d the disposable to remove, not null
     * @return true if the operation was successful
     */
    boolean delete(Disposable d);
}
