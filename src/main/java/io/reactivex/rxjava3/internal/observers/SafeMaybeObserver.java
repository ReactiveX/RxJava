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
import io.reactivex.rxjava3.core.MaybeObserver;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.exceptions.*;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;

/**
 * Wraps another {@link MaybeObserver} and catches exceptions thrown by its
 * {@code onSubscribe}, {@code onSuccess}, {@code onError} or
 * {@code onComplete} methods despite the protocol forbids it.
 * <p>
 * Such exceptions are routed to the {@link RxJavaPlugins#onError(Throwable)} handler.
 *
 * @param <T> the element type of the sequence
 * @since 3.0.0
 */
public final class SafeMaybeObserver<T> implements MaybeObserver<T> {

    final MaybeObserver<? super T> downstream;

    boolean onSubscribeFailed;

    public SafeMaybeObserver(MaybeObserver<? super T> downstream) {
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
    public void onSuccess(@NonNull T t) {
        if (!onSubscribeFailed) {
            try {
                downstream.onSuccess(t);
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                RxJavaPlugins.onError(ex);
            }
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
