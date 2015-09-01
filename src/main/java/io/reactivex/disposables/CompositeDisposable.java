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

import io.reactivex.internal.disposables.SetCompositeResource;

/**
 * A disposable container that can hold onto multiple other disposables.
 */
public final class CompositeDisposable implements Disposable {
    
    final SetCompositeResource<Disposable> resources;
    
    public CompositeDisposable() {
        resources = new SetCompositeResource<>(Disposable::dispose);
    }
    
    public CompositeDisposable(Disposable... resources) {
        Objects.requireNonNull(resources);
        this.resources = new SetCompositeResource<>(Disposable::dispose, resources);
    }
    
    public CompositeDisposable(Iterable<? extends Disposable> resources) {
        Objects.requireNonNull(resources);
        this.resources = new SetCompositeResource<>(Disposable::dispose, resources);
    }
    
    @Override
    public void dispose() {
        resources.dispose();
    }
    
    public boolean isDisposed() {
        return resources.isDisposed();
    }
    
    public void add(Disposable d) {
        resources.add(d);
    }
    
    public void remove(Disposable d) {
        resources.remove(d);
    }
    
    public void clear() {
        resources.clear();
    }
}
