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

package io.reactivex.disposables;

import java.util.Objects;
import java.util.concurrent.atomic.*;

public final class RefCountDisposable implements Disposable {

    volatile Disposable resource;
    static final AtomicReferenceFieldUpdater<RefCountDisposable, Disposable> RESOURCE =
            AtomicReferenceFieldUpdater.newUpdater(RefCountDisposable.class, Disposable.class, "resource");
    
    static final Disposable DISPOSED = () -> { };
    
    volatile int count;
    static final AtomicIntegerFieldUpdater<RefCountDisposable> COUNT =
            AtomicIntegerFieldUpdater.newUpdater(RefCountDisposable.class, "count");

    volatile int once;
    static final AtomicIntegerFieldUpdater<RefCountDisposable> ONCE =
            AtomicIntegerFieldUpdater.newUpdater(RefCountDisposable.class, "once");

    public RefCountDisposable(Disposable resource) {
        Objects.requireNonNull(resource);
        RESOURCE.lazySet(this, resource);
        COUNT.lazySet(this, 1);
    }
    
    @Override
    public void dispose() {
        if (ONCE.compareAndSet(this, 0, 1)) {
            if (COUNT.decrementAndGet(this) == 0) {
                disposeActual();
            }
        }
    }
    
    void disposeActual() {
        Disposable d = resource;
        if (d != DISPOSED) {
            d = RESOURCE.getAndSet(this, DISPOSED);
            if (d != DISPOSED && d != null) {
                d.dispose();
            }
        }
    }
    
    public Disposable get() {
        COUNT.getAndIncrement(this);
        return new InnerDisposable(this);
    }
    
    void release() {
        if (COUNT.decrementAndGet(this) == 0) {
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
