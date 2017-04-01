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

package io.reactivex.disposables;

import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.annotations.Nullable;
import io.reactivex.internal.disposables.DisposableHelper;

/**
 * A Disposable container that allows atomically updating/replacing the contained
 * Disposable with another Disposable, disposing the old one when updating plus
 * handling the disposition when the container itself is disposed.
 */
public final class SerialDisposable implements Disposable {
    final AtomicReference<Disposable> resource;

    /**
     * Constructs an empty SerialDisposable.
     */
    public SerialDisposable() {
        this.resource = new AtomicReference<Disposable>();
    }

    /**
     * Constructs a SerialDisposable with the given initial Disposable instance.
     * @param initialDisposable the initial Disposable instance to use, null allowed
     */
    public SerialDisposable(@Nullable Disposable initialDisposable) {
        this.resource = new AtomicReference<Disposable>(initialDisposable);
    }

    /**
     * Atomically: set the next disposable on this container and dispose the previous
     * one (if any) or dispose next if the container has been disposed.
     * @param next the Disposable to set, may be null
     * @return true if the operation succeeded, false if the container has been disposed
     * @see #replace(Disposable)
     */
    public boolean set(@Nullable Disposable next) {
        return DisposableHelper.set(resource, next);
    }

    /**
     * Atomically: set the next disposable on this container but don't dispose the previous
     * one (if any) or dispose next if the container has been disposed.
     * @param next the Disposable to set, may be null
     * @return true if the operation succeeded, false if the container has been disposed
     * @see #set(Disposable)
     */
    public boolean replace(@Nullable Disposable next) {
        return DisposableHelper.replace(resource, next);
    }

    /**
     * Returns the currently contained Disposable or null if this container is empty.
     * @return the current Disposable, may be null
     */
    @Nullable
    public Disposable get() {
        Disposable d = resource.get();
        if (d == DisposableHelper.DISPOSED) {
            return Disposables.disposed();
        }
        return d;
    }

    @Override
    public void dispose() {
        DisposableHelper.dispose(resource);
    }

    @Override
    public boolean isDisposed() {
        return DisposableHelper.isDisposed(resource.get());
    }
}
