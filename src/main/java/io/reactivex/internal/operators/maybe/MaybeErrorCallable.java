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

package io.reactivex.internal.operators.maybe;

import io.reactivex.*;
import io.reactivex.disposables.Disposables;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.functions.Supplier;
import io.reactivex.internal.functions.ObjectHelper;

/**
 * Signals a Throwable returned by a Supplier.
 *
 * @param <T> the value type
 */
public final class MaybeErrorCallable<T> extends Maybe<T> {

    final Supplier<? extends Throwable> errorSupplier;

    public MaybeErrorCallable(Supplier<? extends Throwable> errorSupplier) {
        this.errorSupplier = errorSupplier;
    }

    @Override
    protected void subscribeActual(MaybeObserver<? super T> observer) {
        observer.onSubscribe(Disposables.disposed());
        Throwable ex;

        try {
            ex = ObjectHelper.requireNonNull(errorSupplier.get(), "Supplier returned null throwable. Null values are generally not allowed in 2.x operators and sources.");
        } catch (Throwable ex1) {
            Exceptions.throwIfFatal(ex1);
            ex = ex1;
        }

        observer.onError(ex);
    }
}
