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

import java.util.function.Consumer;

import io.reactivex.disposables.Disposable;
import io.reactivex.internal.util.OpenHashSet;

/**
 * A set-based composite resource with custom disposer callback.
 *
 * @param <T> the resource type
 */
public final class SetCompositeResource<T> implements CompositeResource<T>, Disposable {
    final Consumer<? super T> disposer;
    
    /** Indicates this resource has been disposed. */
    volatile boolean disposed;
    
    /** The set of resources, accessed while holding a lock on this. */
    OpenHashSet<T> set;
    
    public SetCompositeResource(Consumer<? super T> disposer) {
        this.disposer = disposer;
    }
    
    @SafeVarargs
    public SetCompositeResource(Consumer<? super T> disposer, T... initialResources) {
        this(disposer);
        int n = initialResources.length;
        if (n != 0) {
            set = new OpenHashSet<>(n);
            for (T r : initialResources) {
                set.add(r);
            }
        }
    }
    
    public SetCompositeResource(Consumer<? super T> disposer, Iterable<? extends T> initialResources) {
        this(disposer);
        set = new OpenHashSet<>();
        for (T r : initialResources) {
            set.add(r);
        }
    }
    
    /**
     * Adds a new resource to this composite or disposes it if the composite has been disposed.
     * @param newResource the new resource to add, not-null (not checked)
     * @return
     */
    @Override
    public boolean add(T newResource) {
        if (!disposed) {
            synchronized (this) {
                if (!disposed) {
                    OpenHashSet<T> a = set;
                    if (a == null) {
                        a = new OpenHashSet<>(4);
                        set = a;
                    }
                    a.add(newResource);
                    return true;
                }
            }
        }
        disposer.accept(newResource);
        return false;
    }
    
    /**
     * Removes the given resource from this composite and calls the disposer if the resource
     * was indeed in the composite.
     * @param resource the resource to remove, not-null (not verified)
     * @return true if the resource was removed, false otherwise
     */
    @Override
    public boolean remove(T resource) {
        if (delete(resource)) {
            disposer.accept(resource);
            return true;
        }
        return false;
    }
    
    /**
     * Removes the given resource if contained within this composite but doesn't call the disposer for it.
     * @param resource the resource to delete, not-null (not verified)
     * @return
     */
    @Override
    public boolean delete(T resource) {
        if (disposed) {
            return false;
        }
        synchronized (this) {
            if (disposed) {
                return false;
            }
            OpenHashSet<T> a = set;
            if (a == null || a.isEmpty()) {
                return false;
            }
            
            return a.remove(resource);
        }
    }
    
    @Override
    public void dispose() {
        if (!disposed) {
            OpenHashSet<T> s;
            synchronized (this) {
                if (disposed) {
                    return;
                }
                disposed = true;
                s = set;
                set = null;
            }
            if (s != null) {
                s.forEach(disposer);
            }
        }
    }
    
    public boolean isDisposed() {
        return disposed;
    }
}
