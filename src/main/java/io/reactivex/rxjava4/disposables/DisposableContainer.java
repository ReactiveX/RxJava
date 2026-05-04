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
 * Common interface to add and remove disposables from a container.
 * @since 2.0
 */
public interface DisposableContainer extends Disposable {

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
     * Removes but does not dispose the given disposable if it is part of this
     * container.
     * @param d the disposable to remove, not null
     * @return true if the operation was successful
     */
    boolean delete(Disposable d);

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
     * Create a derived sub container that can get cancelled by this container,
     * but cancelling the subcontainer does not cancel this container.
     * @return the derived subcontainer
     * @since 4.0
     */
    DisposableContainer derive();

    /**
     * Registers a {@link Disposable} with this container so that it can be removed and disposed
     * via a simple {@link #dispose()} call to the returned Disposable.
     * @param d the disposable to register
     * @return the Disposable to trigger a {@link #remove(Disposable)}
     * @see #subscribe(Disposable) for non-disposing removal.
     * @since 4.0.0
     */
    default Disposable register(Disposable d) {
        add(d);
        return Disposable.fromRunnable(() -> remove(d));
    }

    /**
     * Registers a {@link Disposable} with this container so that it can be deleted, not disposed
     * via a simple {@link #dispose()} call to the returned Disposable.
     * @param d the disposable to register
     * @return the Disposable to trigger a {@link #remove(Disposable)}
     * @see #subscribe(Disposable) for non-disposing removal.
     * @since 4.0.0
     */
    default Disposable subscribe(Disposable d) {
        add(d);
        return Disposable.fromRunnable(() -> delete(d));
    }

    /**
     * The container implementation that just ignores everything, for
     * cases where the dispose signal has no side-effects to work with.
     * @since 4.0.0
     */
    static DisposableContainer NEVER = new NeverDisposableContainer();

    /**
     * Implementation of a never disposable container.
     * @since 4.0.0
     */
    static record NeverDisposableContainer() implements DisposableContainer {

        @Override
        public void dispose() {
            // Deliberately empty
        }

        @Override
        public boolean isDisposed() {
            // Who cares?
            return false;
        }

        @Override
        public boolean add(Disposable d) {
            // Who cares?
            return false;
        }

        @Override
        public boolean remove(Disposable d) {
            // Who cares?
            return false;
        }

        @Override
        public boolean delete(Disposable d) {
            // Who cares?
            return false;
        }

        @Override
        public void reset() {
            // Who cares?
        }

        @Override
        public void clear() {
            // Who cares?
        }

        @Override
        public DisposableContainer derive() {
            return NEVER;
        }
    }
}
