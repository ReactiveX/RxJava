/**
 * Copyright 2016 Netflix, Inc.
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
import io.reactivex.functions.Consumer;
import io.reactivex.internal.util.TerminalAtomicsHelper;

/**
 * Holds onto resources with a custom disposer callback and replacing a resource calls
 * the disposer with the old value.
 * 
 * <p>This resource container disposable helps in avoiding the wrapping of other resources
 * into Disposables.
 * 
 * <p>Note that since the implementation leaks the methods of AtomicReference, one must be
 * careful to only call setResource and dispose on it. All other methods may lead to undefined behavior
 * and should be used by internal means only.
 * 
 * @param <T> the resource type
 */
public final class SerialResource<T> extends AtomicReference<Object> implements Disposable {
    /** */
    private static final long serialVersionUID = 5247635821051810205L;
    /** The callback to dispose the resource. */
    final Consumer<? super T> disposer;
    /** The indicator object that this container has been disposed. */
    static final Object DISPOSED = new Object();
    
    /**
     * Constructor with a custom disposer callback.
     * @param disposer
     */
    public SerialResource(Consumer<? super T> disposer) {
        this.disposer = disposer;
    }
    
    /**
     * Constructor with a custom disposer callback and the initial resource
     * @param disposer
     * @param initialResource
     */
    public SerialResource(Consumer<? super T> disposer, T initialResource) {
        this(disposer);
        lazySet(initialResource);
    }
    
    /**
     * Atomically replaces the current resource with the new resource but doesn't call the disposer
     * for it.
     * @param newResource the new resource to replace the old one
     * @return true if the set succeeded, false if the container is disposed
     */
    @SuppressWarnings("unchecked")
    public boolean setResource(T newResource) {
        return TerminalAtomicsHelper.set(this, newResource, DISPOSED, (Consumer<Object>)disposer);
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public void dispose() {
        TerminalAtomicsHelper.terminate(this, DISPOSED, (Consumer<Object>)disposer);
    }
    
    /**
     * Returns the current held resource or null if no resource
     * is set or the container has been disposed.
     * @return the currently held resource
     */
    @SuppressWarnings("unchecked")
    public T getResource() {
        Object d = get();
        if (d == DISPOSED) {
            return null;
        }
        return (T)d;
    }
    
    /**
     * Returns true if this resource has been disposed.
     * @return true if this resource has been disposed
     */
    public boolean isDisposed() {
        return get() == DISPOSED;
    }
}
