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

package io.reactivex.internal.operators.completable;

import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.functions.Cancellable;
import io.reactivex.internal.disposables.*;
import io.reactivex.plugins.RxJavaPlugins;

public final class CompletableCreate extends Completable {

    final CompletableOnSubscribe source;

    public CompletableCreate(CompletableOnSubscribe source) {
        this.source = source;
    }

    @Override
    protected void subscribeActual(CompletableObserver s) {
        Emitter parent = new Emitter(s);
        s.onSubscribe(parent);

        try {
            source.subscribe(parent);
        } catch (Throwable ex) {
            Exceptions.throwIfFatal(ex);
            parent.onError(ex);
        }
    }

    static final class Emitter
    extends AtomicReference<Disposable>
    implements CompletableEmitter, Disposable {

        private static final long serialVersionUID = -2467358622224974244L;

        final CompletableObserver actual;

        Emitter(CompletableObserver actual) {
            this.actual = actual;
        }

        @Override
        public void onComplete() {
            if (get() != DisposableHelper.DISPOSED) {
                Disposable d = getAndSet(DisposableHelper.DISPOSED);
                if (d != DisposableHelper.DISPOSED) {
                    try {
                        actual.onComplete();
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
                t = new NullPointerException("onError called with null. Null values are generally not allowed in 2.x operators and sources.");
            }
            if (get() != DisposableHelper.DISPOSED) {
                Disposable d = getAndSet(DisposableHelper.DISPOSED);
                if (d != DisposableHelper.DISPOSED) {
                    try {
                        actual.onError(t);
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
    }
}
