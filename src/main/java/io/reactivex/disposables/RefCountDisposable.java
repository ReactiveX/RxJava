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

import io.reactivex.internal.functions.Objects;

public final class RefCountDisposable implements Disposable {

    final AtomicReference<Disposable> resource = new AtomicReference<Disposable>();

    static final Disposable DISPOSED = new Disposable() {
        @Override
        public void dispose() { }
    };
    
    final AtomicInteger count = new AtomicInteger();

    final AtomicBoolean once = new AtomicBoolean();

    public RefCountDisposable(Disposable resource) {
        Objects.requireNonNull(resource, "resource is null");
        this.resource.lazySet(resource);
        count.lazySet(1);
    }
    
    @Override
    public void dispose() {
        if (once.compareAndSet(false, true)) {
            if (count.decrementAndGet() == 0) {
                disposeActual();
            }
        }
    }
    
    void disposeActual() {
        Disposable d = resource.get();
        if (d != DISPOSED) {
            d = resource.getAndSet(DISPOSED);
            if (d != DISPOSED && d != null) {
                d.dispose();
            }
        }
    }
    
    public Disposable get() {
        count.getAndIncrement();
        return new InnerDisposable(this);
    }
    
    void release() {
        if (count.decrementAndGet() == 0) {
            disposeActual();
        }
    }
    
    public boolean isDisposed() {
        return resource == DISPOSED;
    }
    
    static final class InnerDisposable extends AtomicBoolean implements Disposable {
        /** */
        private static final long serialVersionUID = -7435605952646106082L;

        final RefCountDisposable parent;
        
        public InnerDisposable(RefCountDisposable parent) {
            this.parent = parent;
        }
        
        @Override
        public void dispose() {
            if (!get() && compareAndSet(false, true)) {
                parent.release();
            }
        }
    }
}
