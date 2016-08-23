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

package io.reactivex.observers;

import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.disposables.*;
import io.reactivex.internal.functions.ObjectHelper;

/**
 * An abstract Observer that allows asynchronous cancellation of its subscription and associated resources.
 * 
 * <p>All pre-implemented final methods are thread-safe.
 * 
 * @param <T> the value type
 */
public abstract class ResourceObserver<T> implements Observer<T>, Disposable {
    /** The active subscription. */
    private final AtomicReference<Disposable> s = new AtomicReference<Disposable>();

    /** The resource composite, can be null. */
    private final ListCompositeDisposable resources = new ListCompositeDisposable();
    
    /**
     * Adds a resource to this ResourceObserver.
     * 
     * @param resource the resource to add
     * 
     * @throws NullPointerException if resource is null
     */
    public final void add(Disposable resource) {
        ObjectHelper.requireNonNull(resource, "resource is null");
        resources.add(resource);
    }
    
    @Override
    public final void onSubscribe(Disposable s) {
        if (!DisposableHelper.setOnce(this.s, s)) {
            onStart();
        }
    }
    
    /**
     * Called once the upstream sets a Subscription on this ResourceObserver.
     * 
     * <p>You can perform initialization at this moment. The default
     * implementation does nothing.
     */
    protected void onStart() {
    }
    
    /**
     * Cancels the main disposable (if any) and disposes the resources associated with
     * this ResourceObserver (if any).
     * 
     * <p>This method can be called before the upstream calls onSubscribe at which
     * case the main Disposable will be immediately disposed.
     */
    protected final void cancel() {
        if (DisposableHelper.dispose(s)) {
            resources.dispose();
        }
    }
    
    @Override
    public final void dispose() {
        cancel();
    }
    
    /**
     * Returns true if this ResourceObserver has been disposed/cancelled.
     * @return true if this ResourceObserver has been disposed/cancelled
     */
    @Override
    public final boolean isDisposed() {
        return DisposableHelper.isDisposed(s.get());
    }
}
