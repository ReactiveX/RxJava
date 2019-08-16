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

package io.reactivex.rxjava3.internal.operators.completable;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.exceptions.*;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.internal.disposables.*;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;

public final class CompletablePeek extends Completable {

    final CompletableSource source;
    final Consumer<? super Disposable> onSubscribe;
    final Consumer<? super Throwable> onError;
    final Action onComplete;
    final Action onTerminate;
    final Action onAfterTerminate;
    final Action onDispose;

    public CompletablePeek(CompletableSource source, Consumer<? super Disposable> onSubscribe,
                           Consumer<? super Throwable> onError,
                           Action onComplete,
                           Action onTerminate,
                           Action onAfterTerminate,
                           Action onDispose) {
        this.source = source;
        this.onSubscribe = onSubscribe;
        this.onError = onError;
        this.onComplete = onComplete;
        this.onTerminate = onTerminate;
        this.onAfterTerminate = onAfterTerminate;
        this.onDispose = onDispose;
    }

    @Override
    protected void subscribeActual(final CompletableObserver observer) {

        source.subscribe(new CompletableObserverImplementation(observer));
    }

    final class CompletableObserverImplementation implements CompletableObserver, Disposable {

        final CompletableObserver downstream;

        Disposable upstream;

        CompletableObserverImplementation(CompletableObserver downstream) {
            this.downstream = downstream;
        }

        @Override
        public void onSubscribe(final Disposable d) {
            try {
                onSubscribe.accept(d);
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                d.dispose();
                this.upstream = DisposableHelper.DISPOSED;
                EmptyDisposable.error(ex, downstream);
                return;
            }
            if (DisposableHelper.validate(this.upstream, d)) {
                this.upstream = d;
                downstream.onSubscribe(this);
            }
        }

        @Override
        public void onError(Throwable e) {
            if (upstream == DisposableHelper.DISPOSED) {
                RxJavaPlugins.onError(e);
                return;
            }
            try {
                onError.accept(e);
                onTerminate.run();
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                e = new CompositeException(e, ex);
            }

            downstream.onError(e);

            doAfter();
        }

        @Override
        public void onComplete() {
            if (upstream == DisposableHelper.DISPOSED) {
                return;
            }

            try {
                onComplete.run();
                onTerminate.run();
            } catch (Throwable e) {
                Exceptions.throwIfFatal(e);
                downstream.onError(e);
                return;
            }

            downstream.onComplete();

            doAfter();
        }

        void doAfter() {
            try {
                onAfterTerminate.run();
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                RxJavaPlugins.onError(ex);
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
            upstream.dispose();
        }

        @Override
        public boolean isDisposed() {
            return upstream.isDisposed();
        }
    }
}
