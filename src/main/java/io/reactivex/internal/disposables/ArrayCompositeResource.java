/**
 * Copyright 2015 Netflix, Inc.
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

import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.function.Consumer;

import io.reactivex.disposables.Disposable;

/**
 * A composite resource with a fixed number of slots.
 *
 * <p>Note that since the implementation leaks the methods of AtomicReferenceArray, one must be
 * careful to only call setResource, replaceResource and dispose on it. All other methods may lead to undefined behavior
 * and should be used by internal means only.
 * 
 * @param <T> the resource tpye
 */
public final class ArrayCompositeResource<T> extends AtomicReferenceArray<Object> implements Disposable {
    /** */
    private static final long serialVersionUID = 2746389416410565408L;

    final Consumer<? super T> disposer;

    static final Object DISPOSED = new Object();
    
    public ArrayCompositeResource(int capacity, Consumer<? super T> disposer) {
        super(capacity);
        this.disposer = disposer;
    }

    /**
     * Sets the resource at the specified index and disposes the old resource.
     * @param index
     * @param resource
     * @return true if the resource has ben set, false if the composite has been disposed
     */
    @SuppressWarnings("unchecked")
    public boolean setResource(int index, T resource) {
        for (;;) {
            Object o = get(index);
            if (o == DISPOSED) {
                disposer.accept(resource);
                return false;
            }
            if (compareAndSet(index, o, resource)) {
                if (o != null) {
                    disposer.accept((T)o);
                }
                return true;
            }
        }
    }
    
    /**
     * Replaces the resource at the specified index and returns the old resource.
     * @param index
     * @param resource
     * @return the old resource, can be null
     */
    @SuppressWarnings("unchecked")
    public T replaceResource(int index, T resource) {
        for (;;) {
            Object o = get(index);
            if (o == DISPOSED) {
                disposer.accept(resource);
                return null;
            }
            if (compareAndSet(index, o, resource)) {
                return (T)o;
            }
        }
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public void dispose() {
        if (get(0) != DISPOSED) {
            int s = length();
            for (int i = 0; i < s; i++) {
                Object o = get(i);
                if (o != DISPOSED) {
                    o = getAndSet(i, DISPOSED);
                    if (o != DISPOSED && o != null) {
                        disposer.accept((T)o);
                    }
                }
            }
        }
    }
    
    public boolean isDisposed() {
        return get(0) == DISPOSED;
    }
}
