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

package io.reactivex.internal.operators.maybe;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.*;
import io.reactivex.annotations.Experimental;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.*;
import io.reactivex.functions.Function;
import io.reactivex.internal.disposables.DisposableHelper;
import io.reactivex.internal.functions.ObjectHelper;

/**
 * Maps the upstream {@code onSuccess}, {@code onError} and {@code onComplete} signals onto
 * a {@link MaybeSource} provided via callback functions and relays its signal to the downstream.
 *
 * @param <T> the upstream success item type
 * @param <R> the downstream success item type
 *
 * @since 2.2.1 - experimental
 */
@Experimental
public final class MaybeFlatMapEventSingle<T, R> extends Single<R> {

    final MaybeSource<T> source;

    final Function<? super T, ? extends SingleSource<? extends R>> onSuccessMapper;

    final Function<? super Throwable, ? extends SingleSource<? extends R>> onErrorMapper;

    final Callable<? extends SingleSource<? extends R>> onCompleteSupplier;

    public MaybeFlatMapEventSingle(MaybeSource<T> source,
            Function<? super T, ? extends SingleSource<? extends R>> onSuccessMapper,
            Function<? super Throwable, ? extends SingleSource<? extends R>> onErrorMapper,
            Callable<? extends SingleSource<? extends R>> onCompleteSupplier) {
        this.source = source;
        this.onSuccessMapper = onSuccessMapper;
        this.onErrorMapper = onErrorMapper;
        this.onCompleteSupplier = onCompleteSupplier;
    }

    @Override
    protected void subscribeActual(SingleObserver<? super R> observer) {
        MapEventObserver<T, R> parent = new MapEventObserver<T, R>(observer, onSuccessMapper, onErrorMapper, onCompleteSupplier);
        observer.onSubscribe(parent);
        source.subscribe(parent);
    }

    static final class MapEventObserver<T, R>
    implements MaybeObserver<T>, Disposable {

        final Function<? super T, ? extends SingleSource<? extends R>> onSuccessMapper;

        final Function<? super Throwable, ? extends SingleSource<? extends R>> onErrorMapper;

        final Callable<? extends SingleSource<? extends R>> onCompleteSupplier;

        final InnerObserver<R> inner;

        MapEventObserver(SingleObserver<? super R> downstream,
                Function<? super T, ? extends SingleSource<? extends R>> onSuccessMapper,
                Function<? super Throwable, ? extends SingleSource<? extends R>> onErrorMapper,
                Callable<? extends SingleSource<? extends R>> onCompleteSupplier) {
            this.inner = new InnerObserver<R>(downstream);
            this.onSuccessMapper = onSuccessMapper;
            this.onErrorMapper = onErrorMapper;
            this.onCompleteSupplier = onCompleteSupplier;
        }

        @Override
        public void onSubscribe(Disposable d) {
            DisposableHelper.setOnce(inner, d);
        }

        @Override
        public void onSuccess(T t) {
            SingleSource<? extends R> next;

            try {
                next = ObjectHelper.requireNonNull(onSuccessMapper.apply(t), "The onSuccessMapper returned a null MaybeSource");
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                inner.downstream.onError(ex);
                return;
            }

            next.subscribe(inner);
        }

        @Override
        public void onError(Throwable e) {
            SingleSource<? extends R> next;

            try {
                next = ObjectHelper.requireNonNull(onErrorMapper.apply(e), "The onErrorMapper returned a null MaybeSource");
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                inner.downstream.onError(new CompositeException(e, ex));
                return;
            }

            next.subscribe(inner);
        }

        @Override
        public void onComplete() {
            SingleSource<? extends R> next;

            try {
                next = ObjectHelper.requireNonNull(onCompleteSupplier.call(), "The onCompleteSupplier returned a null MaybeSource");
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                inner.downstream.onError(ex);
                return;
            }

            next.subscribe(inner);
        }

        @Override
        public void dispose() {
            DisposableHelper.dispose(inner);
        }

        @Override
        public boolean isDisposed() {
            return DisposableHelper.isDisposed(inner.get());
        }

        static final class InnerObserver<R> extends AtomicReference<Disposable>
        implements SingleObserver<R> {

            private static final long serialVersionUID = 3364181040722735182L;

            final SingleObserver<? super R> downstream;

            InnerObserver(SingleObserver<? super R> downstream) {
                this.downstream = downstream;
            }

            @Override
            public void onSubscribe(Disposable d) {
                DisposableHelper.replace(this, d);
            }

            @Override
            public void onSuccess(R t) {
                downstream.onSuccess(t);
            }

            @Override
            public void onError(Throwable e) {
                downstream.onError(e);
            }
        }
    }
}
