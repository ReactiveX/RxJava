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

import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.disposables.Disposable;

/**
 * A Disposable container that allows updating/replacing a Disposable
 * atomically and with respect of disposing the container itself.
 * <p>
 * The class extends AtomicReference directly so watch out for the API leak!
 * @since 2.0
 */
public final class SequentialDisposable
extends AtomicReference<Disposable>
implements Disposable {


    private static final long serialVersionUID = -754898800686245608L;

    /**
     * Constructs an empty SequentialDisposable.
     */
    public SequentialDisposable() {
        // nothing to do
    }

    /**
     * Construct a SequentialDisposable with the initial Disposable provided.
     * @param initial the initial disposable, null allowed
     */
    public SequentialDisposable(Disposable initial) {
        lazySet(initial);
    }

    /**
     * Atomically: set the next disposable on this container and dispose the previous
     * one (if any) or dispose next if the container has been disposed.
     * @param next the Disposable to set, may be null
     * @return true if the operation succeeded, false if the container has been disposed
     * @see #replace(Disposable)
     */
    public boolean update(Disposable next) {
        return DisposableHelper.set(this, next);
    }

    /**
     * Atomically: set the next disposable on this container but don't dispose the previous
     * one (if any) or dispose next if the container has been disposed.
     * @param next the Disposable to set, may be null
     * @return true if the operation succeeded, false if the container has been disposed
     * @see #update(Disposable)
     */
    public boolean replace(Disposable next) {
        return DisposableHelper.replace(this, next);
    }

    @Override
    public void dispose() {
        DisposableHelper.dispose(this);
    }

    @Override
    public boolean isDisposed() {
        return DisposableHelper.isDisposed(get());
    }
}
