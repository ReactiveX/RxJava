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

package io.reactivex.internal.operators.observable;

import java.util.concurrent.Callable;

import io.reactivex.*;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.internal.disposables.EmptyDisposable;

public final class ObservableError<T> extends Observable<T> {
    final Callable<? extends Throwable> errorSupplier;
    public ObservableError(Callable<? extends Throwable> errorSupplier) {
        this.errorSupplier = errorSupplier;
    }
    @Override
    public void subscribeActual(Observer<? super T> s) {
        Throwable error;
        try {
            error = errorSupplier.call();
        } catch (Throwable t) {
            Exceptions.throwIfFatal(t);
            error = t;
        }
        if (error == null) {
            error = new NullPointerException();
        }
        EmptyDisposable.error(error, s);
    }
}
