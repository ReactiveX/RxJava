/*
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

package io.reactivex.rxjava3.internal.operators.completable;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.disposables.*;
import io.reactivex.rxjava3.exceptions.Exceptions;
import io.reactivex.rxjava3.functions.Supplier;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;

/**
 * Call a Supplier for each incoming CompletableObserver and signal completion or the thrown exception.
 * @since 3.0.0
 */
public final class CompletableFromSupplier extends Completable {

    final Supplier<?> supplier;

    public CompletableFromSupplier(Supplier<?> supplier) {
        this.supplier = supplier;
    }

    @Override
    protected void subscribeActual(CompletableObserver observer) {
        Disposable d = Disposable.empty();
        observer.onSubscribe(d);
        try {
            supplier.get();
        } catch (Throwable e) {
            Exceptions.throwIfFatal(e);
            if (!d.isDisposed()) {
                observer.onError(e);
            } else {
                RxJavaPlugins.onError(e);
            }
            return;
        }
        if (!d.isDisposed()) {
            observer.onComplete();
        }
    }
}
