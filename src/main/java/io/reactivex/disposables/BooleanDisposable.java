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

import java.util.concurrent.atomic.AtomicReference;

public final class BooleanDisposable implements Disposable {
    final AtomicReference<Runnable> run = new AtomicReference<Runnable>();

    static final Runnable DISPOSED = new Runnable() {
        @Override
        public void run() { }
    };

    public BooleanDisposable() {
        this(new Runnable() {
            @Override
            public void run() { }
        });
    }
    
    public BooleanDisposable(Runnable run) {
        this.run.lazySet(run);
    }
    
    @Override
    public void dispose() {
        Runnable r = run.get();
        if (r != DISPOSED) {
            r = run.getAndSet(DISPOSED);
            if (r != DISPOSED) {
                r.run();
            }
        }
    }
    
    public boolean isDisposed() {
        return run.get() == DISPOSED;
    }
}
