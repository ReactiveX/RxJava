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

package io.reactivex.rxjava3.internal.operators.maybe;

import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.exceptions.Exceptions;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.internal.disposables.*;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;

/**
 * Invokes callbacks upon {@code onSubscribe} from upstream and
 * {@code dispose} from downstream.
 *
 * @param <T> the element type of the flow
 * @since 3.0.0
 */
public final class MaybeDoOnLifecycle<T> extends AbstractMaybeWithUpstream<T, T> {

    final Consumer<? super Disposable> onSubscribe;

    final Action onDispose;

    public MaybeDoOnLifecycle(Maybe<T> upstream, Consumer<? super Disposable> onSubscribe,
            Action onDispose) {
        super(upstream);
        this.onSubscribe = onSubscribe;
        this.onDispose = onDispose;
    }

    @Override
    protected void subscribeActual(MaybeObserver<? super T> observer) {
        source.subscribe(new MaybeLifecycleObserver<>(observer, onSubscribe, onDispose));
    }

    static final class MaybeLifecycleObserver<T> implements MaybeObserver<T>, Disposable {

        final MaybeObserver<? super T> downstream;

        final Consumer<? super Disposable> onSubscribe;

        final Action onDispose;

        Disposable upstream;

        MaybeLifecycleObserver(MaybeObserver<? super T> downstream, Consumer<? super Disposable> onSubscribe, Action onDispose) {
            this.downstream = downstream;
            this.onSubscribe = onSubscribe;
            this.onDispose = onDispose;
        }

        @Override
        public void onSubscribe(@NonNull Disposable d) {
            // this way, multiple calls to onSubscribe can show up in tests that use doOnSubscribe to validate behavior
            try {
                onSubscribe.accept(d);
            } catch (Throwable e) {
                Exceptions.throwIfFatal(e);
                d.dispose();
                this.upstream = DisposableHelper.DISPOSED;
                EmptyDisposable.error(e, downstream);
                return;
            }
            if (DisposableHelper.validate(this.upstream, d)) {
                this.upstream = d;
                downstream.onSubscribe(this);
            }
        }

        @Override
        public void onSuccess(@NonNull T t) {
            if (upstream != DisposableHelper.DISPOSED) {
                upstream = DisposableHelper.DISPOSED;
                downstream.onSuccess(t);
            }
        }

        @Override
        public void onError(@NonNull Throwable e) {
            if (upstream != DisposableHelper.DISPOSED) {
                upstream = DisposableHelper.DISPOSED;
                downstream.onError(e);
            } else {
                RxJavaPlugins.onError(e);
            }
        }

        @Override
        public void onComplete() {
            if (upstream != DisposableHelper.DISPOSED) {
                upstream = DisposableHelper.DISPOSED;
                downstream.onComplete();
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
            upstream = DisposableHelper.DISPOSED;
        }

        @Override
        public boolean isDisposed() {
            return upstream.isDisposed();
        }
    }
}
