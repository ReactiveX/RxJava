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

package io.reactivex.internal.subscribers.single;

import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.SingleSubscriber;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.CompositeException;
import io.reactivex.functions.Consumer;
import io.reactivex.internal.disposables.DisposableHelper;
import io.reactivex.internal.util.Exceptions;
import io.reactivex.plugins.RxJavaPlugins;

public final class ConsumerSingleSubscriber<T>
extends AtomicReference<Disposable>
implements SingleSubscriber<T>, Disposable {

    /** */
    private static final long serialVersionUID = -7012088219455310787L;

    final Consumer<? super T> onSuccess;
    
    final Consumer<? super Throwable> onError;

    public ConsumerSingleSubscriber(Consumer<? super T> onSuccess, Consumer<? super Throwable> onError) {
        this.onSuccess = onSuccess;
        this.onError = onError;
    }

    @Override
    public void onError(Throwable e) {
        try {
            onError.accept(e);
        } catch (Throwable ex) {
            Exceptions.throwIfFatal(ex);
            RxJavaPlugins.onError(new CompositeException(e, ex));
        }
    }
    
    @Override
    public void onSubscribe(Disposable d) {
        DisposableHelper.setOnce(this, d);
    }
    
    @Override
    public void onSuccess(T value) {
        try {
            onSuccess.accept(value);
        } catch (Throwable ex) {
            Exceptions.throwIfFatal(ex);
            RxJavaPlugins.onError(ex);
        }
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
