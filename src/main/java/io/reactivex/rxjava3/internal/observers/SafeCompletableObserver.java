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

import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.CompletableObserver;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.exceptions.*;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;

/**
 * Wraps another {@link CompletableObserver} and catches exceptions thrown by its
 * {@code onSubscribe}, {@code onError} or
 * {@code onComplete} methods despite the protocol forbids it.
 * <p>
 * Such exceptions are routed to the {@link RxJavaPlugins#onError(Throwable)} handler.
 *
 * @since 3.0.0
 */
public final class SafeCompletableObserver implements CompletableObserver {

    final CompletableObserver downstream;

    boolean onSubscribeFailed;

    public SafeCompletableObserver(CompletableObserver downstream) {
        this.downstream = downstream;
    }

    @Override
    public void onSubscribe(@NonNull Disposable d) {
        try {
            downstream.onSubscribe(d);
        } catch (Throwable ex) {
            Exceptions.throwIfFatal(ex);
            onSubscribeFailed = true;
            d.dispose();
            RxJavaPlugins.onError(ex);
        }
    }

    @Override
    public void onError(@NonNull Throwable e) {
        if (onSubscribeFailed) {
            RxJavaPlugins.onError(e);
        } else {
            try {
                downstream.onError(e);
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                RxJavaPlugins.onError(new CompositeException(e, ex));
            }
        }
    }

    @Override
    public void onComplete() {
        if (!onSubscribeFailed) {
            try {
                downstream.onComplete();
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                RxJavaPlugins.onError(ex);
            }
        }
    }
}
