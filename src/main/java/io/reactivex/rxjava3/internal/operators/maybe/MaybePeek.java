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

package io.reactivex.rxjava3.internal.operators.maybe;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.exceptions.*;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.internal.disposables.*;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;

/**
 * Peeks into the lifecycle of a Maybe and MaybeObserver.
 *
 * @param <T> the value type
 */
public final class MaybePeek<T> extends AbstractMaybeWithUpstream<T, T> {

    final Consumer<? super Disposable> onSubscribeCall;

    final Consumer<? super T> onSuccessCall;

    final Consumer<? super Throwable> onErrorCall;

    final Action onCompleteCall;

    final Action onAfterTerminate;

    final Action onDisposeCall;

    public MaybePeek(MaybeSource<T> source, Consumer<? super Disposable> onSubscribeCall,
            Consumer<? super T> onSuccessCall, Consumer<? super Throwable> onErrorCall, Action onCompleteCall,
            Action onAfterTerminate, Action onDispose) {
        super(source);
        this.onSubscribeCall = onSubscribeCall;
        this.onSuccessCall = onSuccessCall;
        this.onErrorCall = onErrorCall;
        this.onCompleteCall = onCompleteCall;
        this.onAfterTerminate = onAfterTerminate;
        this.onDisposeCall = onDispose;
    }

    @Override
    protected void subscribeActual(MaybeObserver<? super T> observer) {
        source.subscribe(new MaybePeekObserver<T>(observer, this));
    }

    static final class MaybePeekObserver<T> implements MaybeObserver<T>, Disposable {
        final MaybeObserver<? super T> downstream;

        final MaybePeek<T> parent;

        Disposable upstream;

        MaybePeekObserver(MaybeObserver<? super T> actual, MaybePeek<T> parent) {
            this.downstream = actual;
            this.parent = parent;
        }

        @Override
        public void dispose() {
            try {
                parent.onDisposeCall.run();
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                RxJavaPlugins.onError(ex);
            }

            upstream.dispose();
            upstream = DisposableHelper.DISPOSED;
        }

        @Override
        public boolean isDisposed() {
            return upstream.isDisposed();
        }

        @Override
        public void onSubscribe(Disposable d) {
            if (DisposableHelper.validate(this.upstream, d)) {
                try {
                    parent.onSubscribeCall.accept(d);
                } catch (Throwable ex) {
                    Exceptions.throwIfFatal(ex);
                    d.dispose();
                    this.upstream = DisposableHelper.DISPOSED;
                    EmptyDisposable.error(ex, downstream);
                    return;
                }

                this.upstream = d;

                downstream.onSubscribe(this);
            }
        }

        @Override
        public void onSuccess(T value) {
            if (this.upstream == DisposableHelper.DISPOSED) {
                return;
            }
            try {
                parent.onSuccessCall.accept(value);
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                onErrorInner(ex);
                return;
            }
            this.upstream = DisposableHelper.DISPOSED;

            downstream.onSuccess(value);

            onAfterTerminate();
        }

        @Override
        public void onError(Throwable e) {
            if (this.upstream == DisposableHelper.DISPOSED) {
                RxJavaPlugins.onError(e);
                return;
            }

            onErrorInner(e);
        }

        void onErrorInner(Throwable e) {
            try {
                parent.onErrorCall.accept(e);
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                e = new CompositeException(e, ex);
            }

            this.upstream = DisposableHelper.DISPOSED;

            downstream.onError(e);

            onAfterTerminate();
        }

        @Override
        public void onComplete() {
            if (this.upstream == DisposableHelper.DISPOSED) {
                return;
            }

            try {
                parent.onCompleteCall.run();
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                onErrorInner(ex);
                return;
            }
            this.upstream = DisposableHelper.DISPOSED;

            downstream.onComplete();

            onAfterTerminate();
        }

        void onAfterTerminate() {
            try {
                parent.onAfterTerminate.run();
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                RxJavaPlugins.onError(ex);
            }
        }
    }
}
