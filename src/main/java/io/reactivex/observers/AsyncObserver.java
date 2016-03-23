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
import io.reactivex.disposables.*;
import io.reactivex.internal.disposables.ListCompositeResource;
import io.reactivex.internal.functions.Objects;
import io.reactivex.internal.subscriptions.SubscriptionHelper;

/**
 * An abstract Subscriber implementation that allows asynchronous cancellation of its
 * subscription.
 * 
 * <p>This implementation let's you chose if the AsyncObserver manages resources or not,
 * thus saving memory on cases where there is no need for that.
 * 
 * <p>All pre-implemented final methods are thread-safe.
 * 
 * @param <T> the value type
 */
public abstract class AsyncObserver<T> implements Observer<T>, Disposable {
    /** The active subscription. */
    private final AtomicReference<Disposable> s = new AtomicReference<Disposable>();

    /** The resource composite, can be null. */
    private final ListCompositeResource<Disposable> resources;
    
    /** The cancelled subscription indicator. */
    private static final Disposable CANCELLED = new Disposable() {
        @Override
        public void dispose() { }
    };
    
    /**
     * Constructs an AsyncObserver with resource support.
     */
    public AsyncObserver() {
        this(true);
    }

    /**
     * Constructs an AsyncObserver and allows specifying if it should support resources or not.
     * @param withResources true if resource support should be on.
     */
    public AsyncObserver(boolean withResources) {
        this.resources = withResources ? new ListCompositeResource<Disposable>(Disposables.consumeAndDispose()) : null;
    }

    /**
     * Adds a resource to this AsyncObserver.
     * 
     * <p>Note that if the AsyncObserver doesn't manage resources, this method will
     * throw an IllegalStateException. Use {@link #supportsResources()} to determine if
     * this AsyncObserver manages resources or not.
     * 
     * @param resource the resource to add
     * 
     * @throws NullPointerException if resource is null
     * @throws IllegalStateException if this AsyncObserver doesn't manage resources
     * @see #supportsResources()
     */
    public final void add(Disposable resource) {
        Objects.requireNonNull(resource, "resource is null");
        if (resources != null) {
            add(resource);
        } else {
            resource.dispose();
            throw new IllegalStateException("This AsyncObserver doesn't manage additional resources");
        }
    }
    
    /**
     * Returns true if this AsyncObserver supports resources added via the add() method. 
     * @return true if this AsyncObserver supports resources added via the add() method
     * @see #add(Disposable)
     */
    public final boolean supportsResources() {
        return resources != null;
    }
    
    @Override
    public final void onSubscribe(Disposable s) {
        if (!this.s.compareAndSet(null, s)) {
            s.dispose();
            if (this.s.get() != CANCELLED) {
                SubscriptionHelper.reportDisposableSet();
            }
            return;
        }
        
        onStart();
    }
    
    /**
     * Called once the upstream sets a Subscription on this AsyncObserver.
     * 
     * <p>You can perform initialization at this moment. The default
     * implementation does nothing.
     */
    protected void onStart() {
    }
    
    /**
     * Cancels the main disposable (if any) and disposes the resources associated with
     * this AsyncObserver (if any).
     * 
     * <p>This method can be called before the upstream calls onSubscribe at which
     * case the main Disposable will be immediately disposed.
     */
    protected final void cancel() {
        Disposable a = s.get();
        if (a != CANCELLED) {
            a = s.getAndSet(CANCELLED);
            if (a != CANCELLED && a != null) {
                a.dispose();
                if (resources != null) {
                    resources.dispose();
                }
            }
        }
    }
    
    @Override
    public final void dispose() {
        cancel();
    }
    
    /**
     * Returns true if this AsyncObserver has been disposed/cancelled.
     * @return true if this AsyncObserver has been disposed/cancelled
     */
    public final boolean isDisposed() {
        return s == CANCELLED;
    }
}
