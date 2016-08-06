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

package io.reactivex.internal.subscribers.completable;

import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.CompletableSubscriber;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.functions.Consumer;
import io.reactivex.internal.disposables.DisposableHelper;
import io.reactivex.plugins.RxJavaPlugins;

public final class CallbackCompletableSubscriber 
extends AtomicReference<Disposable> implements CompletableSubscriber, Disposable {

    /** */
    private static final long serialVersionUID = -4361286194466301354L;

    final Consumer<? super Throwable> onError;
    final Runnable onComplete;
    
    static final Consumer<? super Throwable> DEFAULT_ON_ERROR = new Consumer<Throwable>() {
        @Override
        public void accept(Throwable e) {
            RxJavaPlugins.onError(e);
        }
    };

    public CallbackCompletableSubscriber(Runnable onComplete) {
        this.onError = DEFAULT_ON_ERROR;
        this.onComplete = onComplete;
    }

    public CallbackCompletableSubscriber(Consumer<? super Throwable> onError, Runnable onComplete) {
        this.onError = onError;
        this.onComplete = onComplete;
    }

    @Override
    public void onComplete() {
        try {
            onComplete.run();
        } catch (Throwable ex) {
            Exceptions.throwIfFatal(ex);
            onError(ex);
        } finally {
            lazySet(DisposableHelper.DISPOSED);
        }
    }

    @Override
    public void onError(Throwable e) {
        try {
            onError.accept(e);
        } catch (Throwable ex) {
            Exceptions.throwIfFatal(ex);
            RxJavaPlugins.onError(ex);
        } finally {
            lazySet(DisposableHelper.DISPOSED);
        }
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
