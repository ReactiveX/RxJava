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

package io.reactivex.rxjava3.internal.operators.maybe;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.internal.fuseable.ScalarSupplier;

/**
 * Signals a constant value.
 *
 * @param <T> the value type
 */
public final class MaybeJust<T> extends Maybe<T> implements ScalarSupplier<T> {

    final T value;

    public MaybeJust(T value) {
        this.value = value;
    }

    @Override
    protected void subscribeActual(MaybeObserver<? super T> observer) {
        observer.onSubscribe(Disposable.disposed());
        observer.onSuccess(value);
    }

    @Override
    public T get() {
        return value;
    }
}
