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
import io.reactivex.disposables.*;
import io.reactivex.exceptions.Exceptions;

public final class ObservableFromCallable<T> extends Observable<T> {
    final Callable<? extends T> callable;
    public ObservableFromCallable(Callable<? extends T> callable) {
        this.callable = callable;
    }
    @Override
    public void subscribeActual(Observer<? super T> s) {
        Disposable d = Disposables.empty();
        s.onSubscribe(d);
        if (d.isDisposed()) {
            return;
        }
        T value;
        try {
            value = callable.call();
        } catch (Throwable e) {
            Exceptions.throwIfFatal(e);
            if (!d.isDisposed()) {
                s.onError(e);
            }
            return;
        }
        if (d.isDisposed()) {
            return;
        }
        if (value != null) {
            s.onNext(value);
            s.onComplete();
        } else {
            s.onError(new NullPointerException("Callable returned null"));
        }
    }
}
