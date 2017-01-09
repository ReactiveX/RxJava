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

package io.reactivex.internal.operators.observable;

import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.disposables.*;

public final class ObserverResourceWrapper<T> extends AtomicReference<Disposable> implements Observer<T>, Disposable {

    private static final long serialVersionUID = -8612022020200669122L;

    final Observer<? super T> actual;

    final AtomicReference<Disposable> subscription = new AtomicReference<Disposable>();

    public ObserverResourceWrapper(Observer<? super T> actual) {
        this.actual = actual;
    }

    @Override
    public void onSubscribe(Disposable s) {
        if (DisposableHelper.setOnce(subscription, s)) {
            actual.onSubscribe(this);
        }
    }

    @Override
    public void onNext(T t) {
        actual.onNext(t);
    }

    @Override
    public void onError(Throwable t) {
        dispose();
        actual.onError(t);
    }

    @Override
    public void onComplete() {
        dispose();
        actual.onComplete();
    }

    @Override
    public void dispose() {
        DisposableHelper.dispose(subscription);

        DisposableHelper.dispose(this);
    }

    @Override
    public boolean isDisposed() {
        return subscription.get() == DisposableHelper.DISPOSED;
    }

    public void setResource(Disposable resource) {
        DisposableHelper.set(this, resource);
    }
}
