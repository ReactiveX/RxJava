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

package io.reactivex.rxjava3.internal.operators.observable;

import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.internal.disposables.DisposableHelper;

public final class ObserverResourceWrapper<T> extends AtomicReference<Disposable> implements Observer<T>, Disposable {

    private static final long serialVersionUID = -8612022020200669122L;

    final Observer<? super T> downstream;

    final AtomicReference<Disposable> upstream = new AtomicReference<Disposable>();

    public ObserverResourceWrapper(Observer<? super T> downstream) {
        this.downstream = downstream;
    }

    @Override
    public void onSubscribe(Disposable d) {
        if (DisposableHelper.setOnce(upstream, d)) {
            downstream.onSubscribe(this);
        }
    }

    @Override
    public void onNext(T t) {
        downstream.onNext(t);
    }

    @Override
    public void onError(Throwable t) {
        dispose();
        downstream.onError(t);
    }

    @Override
    public void onComplete() {
        dispose();
        downstream.onComplete();
    }

    @Override
    public void dispose() {
        DisposableHelper.dispose(upstream);

        DisposableHelper.dispose(this);
    }

    @Override
    public boolean isDisposed() {
        return upstream.get() == DisposableHelper.DISPOSED;
    }

    public void setResource(Disposable resource) {
        DisposableHelper.set(this, resource);
    }
}
