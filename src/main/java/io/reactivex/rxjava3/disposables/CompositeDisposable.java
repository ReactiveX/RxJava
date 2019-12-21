/**
 * Copyright (c) 2016-present, RxJava Contributors.
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
package io.reactivex.rxjava3.disposables;

import java.util.*;

import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.exceptions.*;
import io.reactivex.rxjava3.internal.util.*;

/**
 * A disposable container that can hold onto multiple other disposables and
 * offers O(1) add and removal complexity.
 */
public final class CompositeDisposable implements Disposable, DisposableContainer {

    OpenHashSet<Disposable> resources;

    volatile boolean disposed;

    /**
     * Creates an empty CompositeDisposable.
     */
    public CompositeDisposable() {
    }

    /**
     * Creates a CompositeDisposables with the given array of initial elements.
     * @param disposables the array of Disposables to start with
     * @throws NullPointerException if {@code disposables} or any of its array items is {@code null}
     */
    public CompositeDisposable(@NonNull Disposable... disposables) {
        Objects.requireNonNull(disposables, "disposables is null");
        this.resources = new OpenHashSet<Disposable>(disposables.length + 1);
        for (Disposable d : disposables) {
            Objects.requireNonNull(d, "A Disposable in the disposables array is null");
            this.resources.add(d);
        }
    }

    /**
     * Creates a CompositeDisposables with the given Iterable sequence of initial elements.
     * @param disposables the Iterable sequence of Disposables to start with
     * @throws NullPointerException if {@code disposables} or any of its items is {@code null}
     */
    public CompositeDisposable(@NonNull Iterable<? extends Disposable> disposables) {
        Objects.requireNonNull(disposables, "disposables is null");
        this.resources = new OpenHashSet<Disposable>();
        for (Disposable d : disposables) {
            Objects.requireNonNull(d, "A Disposable item in the disposables sequence is null");
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

    /**
     * Adds a disposable to this container or disposes it if the
     * container has been disposed.
     * @param disposable the disposable to add, not null
     * @return true if successful, false if this container has been disposed
     * @throws NullPointerException if {@code disposable} is {@code null}
     */
    @Override
    public boolean add(@NonNull Disposable disposable) {
        Objects.requireNonNull(disposable, "disposable is null");
        if (!disposed) {
            synchronized (this) {
                if (!disposed) {
                    OpenHashSet<Disposable> set = resources;
                    if (set == null) {
                        set = new OpenHashSet<Disposable>();
                        resources = set;
                    }
                    set.add(disposable);
                    return true;
                }
            }
        }
        disposable.dispose();
        return false;
    }

    /**
     * Atomically adds the given array of Disposables to the container or
     * disposes them all if the container has been disposed.
     * @param disposables the array of Disposables
     * @return true if the operation was successful, false if the container has been disposed
     * @throws NullPointerException if {@code disposables} or any of its array items is {@code null}
     */
    public boolean addAll(@NonNull Disposable... disposables) {
        Objects.requireNonNull(disposables, "disposables is null");
        if (!disposed) {
            synchronized (this) {
                if (!disposed) {
                    OpenHashSet<Disposable> set = resources;
                    if (set == null) {
                        set = new OpenHashSet<Disposable>(disposables.length + 1);
                        resources = set;
                    }
                    for (Disposable d : disposables) {
                        Objects.requireNonNull(d, "A Disposable in the disposables array is null");
                        set.add(d);
                    }
                    return true;
                }
            }
        }
        for (Disposable d : disposables) {
            d.dispose();
        }
        return false;
    }

    /**
     * Removes and disposes the given disposable if it is part of this
     * container.
     * @param disposable the disposable to remove and dispose, not null
     * @return true if the operation was successful
     */
    @Override
    public boolean remove(@NonNull Disposable disposable) {
        if (delete(disposable)) {
            disposable.dispose();
            return true;
        }
        return false;
    }

    /**
     * Removes (but does not dispose) the given disposable if it is part of this
     * container.
     * @param disposable the disposable to remove, not null
     * @return true if the operation was successful
     * @throws NullPointerException if {@code disposable} is {@code null}
     */
    @Override
    public boolean delete(@NonNull Disposable disposable) {
        Objects.requireNonNull(disposable, "disposables is null");
        if (disposed) {
            return false;
        }
        synchronized (this) {
            if (disposed) {
                return false;
            }

            OpenHashSet<Disposable> set = resources;
            if (set == null || !set.remove(disposable)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Atomically clears the container, then disposes all the previously contained Disposables.
     */
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

    /**
     * Returns the number of currently held Disposables.
     * @return the number of currently held Disposables
     */
    public int size() {
        if (disposed) {
            return 0;
        }
        synchronized (this) {
            if (disposed) {
                return 0;
            }
            OpenHashSet<Disposable> set = resources;
            return set != null ? set.size() : 0;
        }
    }

    /**
     * Dispose the contents of the OpenHashSet by suppressing non-fatal
     * Throwables till the end.
     * @param set the OpenHashSet to dispose elements of
     */
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
                    Exceptions.throwIfFatal(ex);
                    if (errors == null) {
                        errors = new ArrayList<Throwable>();
                    }
                    errors.add(ex);
                }
            }
        }
        if (errors != null) {
            if (errors.size() == 1) {
                throw ExceptionHelper.wrapOrThrow(errors.get(0));
            }
            throw new CompositeException(errors);
        }
    }
}
