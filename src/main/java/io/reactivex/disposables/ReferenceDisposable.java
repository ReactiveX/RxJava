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

package io.reactivex.disposables;

import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.annotations.NonNull;
import io.reactivex.internal.functions.ObjectHelper;

/**
 * Base class for Disposable containers that manage some other type that
 * has to be run when the container is disposed.
 *
 * @param <T> the type contained
 */
abstract class ReferenceDisposable<T> extends AtomicReference<T> implements Disposable {

    private static final long serialVersionUID = 6537757548749041217L;

    ReferenceDisposable(T value) {
        super(ObjectHelper.requireNonNull(value, "value is null"));
    }

    protected abstract void onDisposed(@NonNull T value);

    @Override
    public final void dispose() {
        T value = get();
        if (value != null) {
            value = getAndSet(null);
            if (value != null) {
                onDisposed(value);
            }
        }
    }

    @Override
    public final boolean isDisposed() {
        return get() == null;
    }
}
