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

package io.reactivex.rxjava3.internal.disposables;

import java.util.concurrent.atomic.AtomicReferenceArray;

import io.reactivex.rxjava3.disposables.Disposable;

/**
 * A composite disposable with a fixed number of slots.
 *
 * <p>Note that since the implementation leaks the methods of AtomicReferenceArray, one must be
 * careful to only call setResource, replaceResource and dispose on it. All other methods may lead to undefined behavior
 * and should be used by internal means only.
 */
public final class ArrayCompositeDisposable extends AtomicReferenceArray<Disposable> implements Disposable {

    private static final long serialVersionUID = 2746389416410565408L;

    public ArrayCompositeDisposable(int capacity) {
        super(capacity);
    }

    /**
     * Sets the resource at the specified index and disposes the old resource.
     * @param index the index of the resource to set
     * @param resource the new resource
     * @return true if the resource has ben set, false if the composite has been disposed
     */
    public boolean setResource(int index, Disposable resource) {
        for (;;) {
            Disposable o = get(index);
            if (o == DisposableHelper.DISPOSED) {
                resource.dispose();
                return false;
            }
            if (compareAndSet(index, o, resource)) {
                if (o != null) {
                    o.dispose();
                }
                return true;
            }
        }
    }

    /**
     * Replaces the resource at the specified index and returns the old resource.
     * @param index the index of the resource to replace
     * @param resource the new resource
     * @return the old resource, can be null
     */
    public Disposable replaceResource(int index, Disposable resource) {
        for (;;) {
            Disposable o = get(index);
            if (o == DisposableHelper.DISPOSED) {
                resource.dispose();
                return null;
            }
            if (compareAndSet(index, o, resource)) {
                return o;
            }
        }
    }

    @Override
    public void dispose() {
        if (get(0) != DisposableHelper.DISPOSED) {
            int s = length();
            for (int i = 0; i < s; i++) {
                Disposable o = get(i);
                if (o != DisposableHelper.DISPOSED) {
                    o = getAndSet(i, DisposableHelper.DISPOSED);
                    if (o != DisposableHelper.DISPOSED && o != null) {
                        o.dispose();
                    }
                }
            }
        }
    }

    @Override
    public boolean isDisposed() {
        return get(0) == DisposableHelper.DISPOSED;
    }
}
