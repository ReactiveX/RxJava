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

package io.reactivex.rxjava3.internal.operators.single;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.exceptions.Exceptions;
import io.reactivex.rxjava3.functions.Supplier;
import io.reactivex.rxjava3.internal.disposables.EmptyDisposable;

import java.util.Objects;

public final class SingleDefer<T> extends Single<T> {

    final Supplier<? extends SingleSource<? extends T>> singleSupplier;

    public SingleDefer(Supplier<? extends SingleSource<? extends T>> singleSupplier) {
        this.singleSupplier = singleSupplier;
    }

    @Override
    protected void subscribeActual(SingleObserver<? super T> observer) {
        SingleSource<? extends T> next;

        try {
            next = Objects.requireNonNull(singleSupplier.get(), "The singleSupplier returned a null SingleSource");
        } catch (Throwable e) {
            Exceptions.throwIfFatal(e);
            EmptyDisposable.error(e, observer);
            return;
        }

        next.subscribe(observer);
    }

}
