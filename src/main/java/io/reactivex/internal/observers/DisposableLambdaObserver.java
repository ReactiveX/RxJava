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

package io.reactivex.internal.observers;

import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.functions.*;
import io.reactivex.internal.disposables.*;
import io.reactivex.plugins.RxJavaPlugins;

public final class DisposableLambdaObserver<T> implements Observer<T>, Disposable {
    final Observer<? super T> actual;
    final Consumer<? super Disposable> onSubscribe;
    final Action onDispose;

    Disposable s;

    public DisposableLambdaObserver(Observer<? super T> actual,
            Consumer<? super Disposable> onSubscribe,
            Action onDispose) {
        this.actual = actual;
        this.onSubscribe = onSubscribe;
        this.onDispose = onDispose;
    }

    @Override
    public void onSubscribe(Disposable s) {
        // this way, multiple calls to onSubscribe can show up in tests that use doOnSubscribe to validate behavior
        try {
            onSubscribe.accept(s);
        } catch (Throwable e) {
            Exceptions.throwIfFatal(e);
            s.dispose();
            this.s = DisposableHelper.DISPOSED;
            EmptyDisposable.error(e, actual);
            return;
        }
        if (DisposableHelper.validate(this.s, s)) {
            this.s = s;
            actual.onSubscribe(this);
        }
    }

    @Override
    public void onNext(T t) {
        actual.onNext(t);
    }

    @Override
    public void onError(Throwable t) {
        if (s != DisposableHelper.DISPOSED) {
            actual.onError(t);
        } else {
            RxJavaPlugins.onError(t);
        }
    }

    @Override
    public void onComplete() {
        if (s != DisposableHelper.DISPOSED) {
            actual.onComplete();
        }
    }


    @Override
    public void dispose() {
        try {
            onDispose.run();
        } catch (Throwable e) {
            Exceptions.throwIfFatal(e);
            RxJavaPlugins.onError(e);
        }
        s.dispose();
    }

    @Override
    public boolean isDisposed() {
        return s.isDisposed();
    }
}
