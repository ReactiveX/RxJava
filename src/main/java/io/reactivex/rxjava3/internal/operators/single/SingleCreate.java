/*
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

package io.reactivex.rxjava3.internal.operators.single;

import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.exceptions.Exceptions;
import io.reactivex.rxjava3.functions.Cancellable;
import io.reactivex.rxjava3.internal.disposables.*;
import io.reactivex.rxjava3.internal.util.ExceptionHelper;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;

public final class SingleCreate<T> extends Single<T> {

    final SingleOnSubscribe<T> source;

    public SingleCreate(SingleOnSubscribe<T> source) {
        this.source = source;
    }

    @Override
    protected void subscribeActual(SingleObserver<? super T> observer) {
        Emitter<T> parent = new Emitter<>(observer);
        observer.onSubscribe(parent);

        try {
            source.subscribe(parent);
        } catch (Throwable ex) {
            Exceptions.throwIfFatal(ex);
            parent.onError(ex);
        }
    }

    static final class Emitter<T>
    extends AtomicReference<Disposable>
    implements SingleEmitter<T>, Disposable {

        private static final long serialVersionUID = -2467358622224974244L;

        final SingleObserver<? super T> downstream;

        Emitter(SingleObserver<? super T> downstream) {
            this.downstream = downstream;
        }

        @Override
        public void onSuccess(T value) {
            if (get() != DisposableHelper.DISPOSED) {
                Disposable d = getAndSet(DisposableHelper.DISPOSED);
                if (d != DisposableHelper.DISPOSED) {
                    try {
                        if (value == null) {
                            downstream.onError(ExceptionHelper.createNullPointerException("onSuccess called with a null value."));
                        } else {
                            downstream.onSuccess(value);
                        }
                    } finally {
                        if (d != null) {
                            d.dispose();
                        }
                    }
                }
            }
        }

        @Override
        public void onError(Throwable t) {
            if (!tryOnError(t)) {
                RxJavaPlugins.onError(t);
            }
        }

        @Override
        public boolean tryOnError(Throwable t) {
            if (t == null) {
                t = ExceptionHelper.createNullPointerException("onError called with a null Throwable.");
            }
            if (get() != DisposableHelper.DISPOSED) {
                Disposable d = getAndSet(DisposableHelper.DISPOSED);
                if (d != DisposableHelper.DISPOSED) {
                    try {
                        downstream.onError(t);
                    } finally {
                        if (d != null) {
                            d.dispose();
                        }
                    }
                    return true;
                }
            }
            return false;
        }

        @Override
        public void setDisposable(Disposable d) {
            DisposableHelper.set(this, d);
        }

        @Override
        public void setCancellable(Cancellable c) {
            setDisposable(new CancellableDisposable(c));
        }

        @Override
        public void dispose() {
            DisposableHelper.dispose(this);
        }

        @Override
        public boolean isDisposed() {
            return DisposableHelper.isDisposed(get());
        }

        @Override
        public String toString() {
            return String.format("%s{%s}", getClass().getSimpleName(), super.toString());
        }
    }
}
