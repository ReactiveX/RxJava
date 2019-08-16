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

package io.reactivex.rxjava3.internal.observers;

import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.exceptions.Exceptions;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.internal.disposables.*;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;

public final class DisposableLambdaObserver<T> implements Observer<T>, Disposable {
    final Observer<? super T> downstream;
    final Consumer<? super Disposable> onSubscribe;
    final Action onDispose;

    Disposable upstream;

    public DisposableLambdaObserver(Observer<? super T> actual,
            Consumer<? super Disposable> onSubscribe,
            Action onDispose) {
        this.downstream = actual;
        this.onSubscribe = onSubscribe;
        this.onDispose = onDispose;
    }

    @Override
    public void onSubscribe(Disposable d) {
        // this way, multiple calls to onSubscribe can show up in tests that use doOnSubscribe to validate behavior
        try {
            onSubscribe.accept(d);
        } catch (Throwable e) {
            Exceptions.throwIfFatal(e);
            d.dispose();
            this.upstream = DisposableHelper.DISPOSED;
            EmptyDisposable.error(e, downstream);
            return;
        }
        if (DisposableHelper.validate(this.upstream, d)) {
            this.upstream = d;
            downstream.onSubscribe(this);
        }
    }

    @Override
    public void onNext(T t) {
        downstream.onNext(t);
    }

    @Override
    public void onError(Throwable t) {
        if (upstream != DisposableHelper.DISPOSED) {
            upstream = DisposableHelper.DISPOSED;
            downstream.onError(t);
        } else {
            RxJavaPlugins.onError(t);
        }
    }

    @Override
    public void onComplete() {
        if (upstream != DisposableHelper.DISPOSED) {
            upstream = DisposableHelper.DISPOSED;
            downstream.onComplete();
        }
    }

    @Override
    public void dispose() {
        Disposable d = upstream;
        if (d != DisposableHelper.DISPOSED) {
            upstream = DisposableHelper.DISPOSED;
            try {
                onDispose.run();
            } catch (Throwable e) {
                Exceptions.throwIfFatal(e);
                RxJavaPlugins.onError(e);
            }
            d.dispose();
        }
    }

    @Override
    public boolean isDisposed() {
        return upstream.isDisposed();
    }
}
