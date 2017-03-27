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

import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.CompletableObserver;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.*;
import io.reactivex.functions.*;
import io.reactivex.internal.disposables.DisposableHelper;
import io.reactivex.plugins.RxJavaPlugins;

public final class CallbackCompletableObserver
extends AtomicReference<Disposable> implements CompletableObserver, Disposable, Consumer<Throwable> {


    private static final long serialVersionUID = -4361286194466301354L;

    final Consumer<? super Throwable> onError;
    final Action onComplete;

    public CallbackCompletableObserver(Action onComplete) {
        this.onError = this;
        this.onComplete = onComplete;
    }

    public CallbackCompletableObserver(Consumer<? super Throwable> onError, Action onComplete) {
        this.onError = onError;
        this.onComplete = onComplete;
    }

    @Override
    public void accept(Throwable e) {
        RxJavaPlugins.onError(new OnErrorNotImplementedException(e));
    }

    @Override
    public void onComplete() {
        try {
            onComplete.run();
        } catch (Throwable ex) {
            Exceptions.throwIfFatal(ex);
            RxJavaPlugins.onError(ex);
        }
        lazySet(DisposableHelper.DISPOSED);
    }

    @Override
    public void onError(Throwable e) {
        try {
            onError.accept(e);
        } catch (Throwable ex) {
            Exceptions.throwIfFatal(ex);
            RxJavaPlugins.onError(ex);
        }
        lazySet(DisposableHelper.DISPOSED);
    }

    @Override
    public void onSubscribe(Disposable d) {
        DisposableHelper.setOnce(this, d);
    }

    @Override
    public void dispose() {
        DisposableHelper.dispose(this);
    }

    @Override
    public boolean isDisposed() {
        return get() == DisposableHelper.DISPOSED;
    }
}
