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

import java.util.*;

import io.reactivex.exceptions.CompositeException;
import io.reactivex.internal.functions.Objects;
import io.reactivex.internal.util.*;

/**
 * A disposable container that can hold onto multiple other disposables.
 */
public final class CompositeDisposable implements Disposable {
    
    OpenHashSet<Disposable> resources;

    volatile boolean disposed;
    
    public CompositeDisposable() {
    }
    
    public CompositeDisposable(Disposable... resources) {
        Objects.requireNonNull(resources, "resources is null");
        this.resources = new OpenHashSet<Disposable>(resources.length + 1);
        for (Disposable d : resources) {
            Objects.requireNonNull(d, "Disposable item is null");
            this.resources.add(d);
        }
    }
    
    public CompositeDisposable(Iterable<? extends Disposable> resources) {
        Objects.requireNonNull(resources, "resources is null");
        for (Disposable d : resources) {
            Objects.requireNonNull(d, "Disposable item is null");
            this.resources.add(d);
        }
    }
    
    @Override
    public void dispose() {
        if (disposed) {
            return;
        }
        OpenHashSet<Disposable> set;
        synchronized (this) {
            if (disposed) {
                return;
            }
            disposed = true;
            set = resources;
            resources = null;
        }
        
        dispose(set);
    }
    
    @Override
    public boolean isDisposed() {
        return disposed;
    }
    
    public void add(Disposable d) {
        Objects.requireNonNull(d, "d is null");
        if (!disposed) {
            synchronized (this) {
                if (!disposed) {
                    OpenHashSet<Disposable> set = resources;
                    if (set == null) {
                        set = new OpenHashSet<Disposable>();
                        resources = set;
                    }
                    set.add(d);
                    return;
                }
            }
        }
        d.dispose();
    }

    public void addAll(Disposable... ds) {
        Objects.requireNonNull(ds, "ds is null");
        if (!disposed) {
            synchronized (this) {
                if (!disposed) {
                    OpenHashSet<Disposable> set = resources;
                    if (set == null) {
                        set = new OpenHashSet<Disposable>(ds.length + 1);
                        resources = set;
                    }
                    for (Disposable d : ds) {
                        Objects.requireNonNull(d, "d is null");
                        set.add(d);
                    }
                    return;
                }
            }
        }
        for (Disposable d : ds) {
            d.dispose();
        }
    }

    public void remove(Disposable d) {
        Objects.requireNonNull(d, "Disposable item is null");
        if (disposed) {
            return;
        }
        synchronized (this) {
            if (disposed) {
                return;
            }
            
            OpenHashSet<Disposable> set = resources;
            if (set == null || !set.remove(d)) {
                return;
            }
        }
        d.dispose();
    }
    
    public void delete(Disposable d) {
        Objects.requireNonNull(d, "Disposable item is null");
        if (disposed) {
            return;
        }
        synchronized (this) {
            if (disposed) {
                return;
            }
            
            OpenHashSet<Disposable> set = resources;
            if (set == null || !set.remove(d)) {
                return;
            }
        }
    }
    
    public void clear() {
        if (disposed) {
            return;
        }
        OpenHashSet<Disposable> set;
        synchronized (this) {
            if (disposed) {
                return;
            }
            
            set = resources;
            resources = null;
        }
        
        dispose(set);
    }
    
    void dispose(OpenHashSet<Disposable> set) {
        if (set == null) {
            return;
        }
        List<Throwable> errors = null;
        Object[] array = set.keys();
        for (Object o : array) {
            if (o instanceof Disposable) {
                try {
                    ((Disposable) o).dispose();
                } catch (Throwable ex) {
                    if (errors == null) {
                        errors = new ArrayList<Throwable>();
                    }
                    errors.add(ex);
                }
            }
        }
        if (errors != null) {
            if (errors.size() == 1) {
                throw Exceptions.propagate(errors.get(0));
            }
            throw new CompositeException(errors);
        }
    }
}
