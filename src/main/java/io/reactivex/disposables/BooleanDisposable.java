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

import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

public final class BooleanDisposable implements Disposable {
    volatile Runnable run;

    static final AtomicReferenceFieldUpdater<BooleanDisposable, Runnable> RUN =
            AtomicReferenceFieldUpdater.newUpdater(BooleanDisposable.class, Runnable.class, "run");
    
    static final Runnable DISPOSED = () -> { };

    public BooleanDisposable() {
        this(() -> { });
    }
    
    public BooleanDisposable(Runnable run) {
        RUN.lazySet(this, run);
    }
    
    @Override
    public void dispose() {
        Runnable r = run;
        if (r != DISPOSED) {
            r = RUN.getAndSet(this, DISPOSED);
            if (r != DISPOSED) {
                r.run();
            }
        }
    }
    
    public boolean isDisposed() {
        return run == DISPOSED;
    }
}
