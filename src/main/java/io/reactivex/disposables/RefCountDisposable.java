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

package io.reactivex.disposables;

import java.util.concurrent.atomic.*;

import io.reactivex.internal.disposables.DisposableHelper;
import io.reactivex.internal.functions.Objects;

/**
 * Keeps track of the sub-subscriptions and unsubscribes the underlying subscription once all sub-subscriptions
 * have unsubscribed.
 */
public final class RefCountDisposable implements Disposable {

    final AtomicReference<Disposable> resource = new AtomicReference<Disposable>();

    final AtomicInteger count = new AtomicInteger();

    final AtomicBoolean once = new AtomicBoolean();

    /**
     * Creates a {@code RefCountDisposable} by wrapping the given non-null {@code Subscription}.
     * 
     * @param resource
     *          the {@link Disposable} to wrap
     * @throws NullPointerException
     *          if {@code s} is {@code null}
     */
    public RefCountDisposable(Disposable resource) {
        Objects.requireNonNull(resource, "resource is null");
        this.resource.lazySet(resource);
        count.lazySet(1);
    }
    
    @Override
    public void dispose() {
        if (once.compareAndSet(false, true)) {
            release();
        }
    }

    /**
     * Returns a new sub-Disposable
     *
     * @return a new sub-Disposable.
     */
    public Disposable get() {
        count.getAndIncrement();
        return new InnerDisposable(this);
    }
    
    void release() {
        if (count.decrementAndGet() == 0) {
            DisposableHelper.dispose(resource);
        }
    }
    
    @Override
    public boolean isDisposed() {
        return resource.get() == DisposableHelper.DISPOSED;
    }
    
    static final class InnerDisposable extends ReferenceDisposable<RefCountDisposable> {
        /** */
        private static final long serialVersionUID = -6066815451193282256L;

        InnerDisposable(RefCountDisposable parent) {
            super(parent);
        }

        @Override
        protected void onDisposed(RefCountDisposable parent) {
            parent.release();
        }
    }
}
