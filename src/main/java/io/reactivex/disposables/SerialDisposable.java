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

import io.reactivex.internal.disposables.*;

public final class SerialDisposable implements Disposable {
    final SerialResource<Disposable> resource;
    
    public SerialDisposable() {
        this.resource = new SerialResource<Disposable>(Disposables.consumeAndDispose());
    }
    
    public SerialDisposable(Disposable initialDisposable) {
        this.resource = new SerialResource<Disposable>(Disposables.consumeAndDispose(), initialDisposable);
    }

    
    public void set(Disposable d) {
        this.resource.setResource(d);
    }
    
    public Disposable get() {
        Object o = resource.getResource();
        if (o == null) {
            if (resource.isDisposed()) {
                return EmptyDisposable.INSTANCE;
            }
        }
        return (Disposable)o;
    }
    
    @Override
    public void dispose() {
        resource.dispose();
    }
    
    public boolean isDisposed() {
        return resource.isDisposed();
    }
}
