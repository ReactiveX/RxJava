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
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.*;
import io.reactivex.functions.Function;
import io.reactivex.internal.disposables.DisposableHelper;
import io.reactivex.internal.functions.ObjectHelper;

/**
 * Maps a value into a MaybeSource and relays its signal.
 *
 * @param <T> the source value type
 * @param <R> the result value type
 */
public final class MaybeFlatMapNotification<T, R> extends AbstractMaybeWithUpstream<T, R> {

    final Function<? super T, ? extends MaybeSource<? extends R>> onSuccessMapper;

    final Function<? super Throwable, ? extends MaybeSource<? extends R>> onErrorMapper;

    final Callable<? extends MaybeSource<? extends R>> onCompleteSupplier;

    public MaybeFlatMapNotification(MaybeSource<T> source,
            Function<? super T, ? extends MaybeSource<? extends R>> onSuccessMapper,
            Function<? super Throwable, ? extends MaybeSource<? extends R>> onErrorMapper,
            Callable<? extends MaybeSource<? extends R>> onCompleteSupplier) {
        super(source);
        this.onSuccessMapper = onSuccessMapper;
        this.onErrorMapper = onErrorMapper;
        this.onCompleteSupplier = onCompleteSupplier;
    }

    @Override
    protected void subscribeActual(MaybeObserver<? super R> observer) {
        source.subscribe(new FlatMapMaybeObserver<T, R>(observer, onSuccessMapper, onErrorMapper, onCompleteSupplier));
    }

    static final class FlatMapMaybeObserver<T, R>
    extends AtomicReference<Disposable>
    implements MaybeObserver<T>, Disposable {


        private static final long serialVersionUID = 4375739915521278546L;

        final MaybeObserver<? super R> actual;

        final Function<? super T, ? extends MaybeSource<? extends R>> onSuccessMapper;

        final Function<? super Throwable, ? extends MaybeSource<? extends R>> onErrorMapper;

        final Callable<? extends MaybeSource<? extends R>> onCompleteSupplier;

        Disposable d;

        FlatMapMaybeObserver(MaybeObserver<? super R> actual,
                Function<? super T, ? extends MaybeSource<? extends R>> onSuccessMapper,
                Function<? super Throwable, ? extends MaybeSource<? extends R>> onErrorMapper,
                Callable<? extends MaybeSource<? extends R>> onCompleteSupplier) {
            this.actual = actual;
            this.onSuccessMapper = onSuccessMapper;
            this.onErrorMapper = onErrorMapper;
            this.onCompleteSupplier = onCompleteSupplier;
        }

        @Override
        public void dispose() {
            DisposableHelper.dispose(this);
            d.dispose();
        }

        @Override
        public boolean isDisposed() {
            return DisposableHelper.isDisposed(get());
        }

        @Override
        public void onSubscribe(Disposable d) {
            if (DisposableHelper.validate(this.d, d)) {
                this.d = d;

                actual.onSubscribe(this);
            }
        }

        @Override
        public void onSuccess(T value) {
            MaybeSource<? extends R> source;

            try {
                source = ObjectHelper.requireNonNull(onSuccessMapper.apply(value), "The onSuccessMapper returned a null MaybeSource");
            } catch (Exception ex) {
                Exceptions.throwIfFatal(ex);
                actual.onError(ex);
                return;
            }

            source.subscribe(new InnerObserver());
        }

        @Override
        public void onError(Throwable e) {
            MaybeSource<? extends R> source;

            try {
                source = ObjectHelper.requireNonNull(onErrorMapper.apply(e), "The onErrorMapper returned a null MaybeSource");
            } catch (Exception ex) {
                Exceptions.throwIfFatal(ex);
                actual.onError(new CompositeException(e, ex));
                return;
            }

            source.subscribe(new InnerObserver());
        }

        @Override
        public void onComplete() {
            MaybeSource<? extends R> source;

            try {
                source = ObjectHelper.requireNonNull(onCompleteSupplier.call(), "The onCompleteSupplier returned a null MaybeSource");
            } catch (Exception ex) {
                Exceptions.throwIfFatal(ex);
                actual.onError(ex);
                return;
            }

            source.subscribe(new InnerObserver());
        }

        final class InnerObserver implements MaybeObserver<R> {

            @Override
            public void onSubscribe(Disposable d) {
                DisposableHelper.setOnce(FlatMapMaybeObserver.this, d);
            }

            @Override
            public void onSuccess(R value) {
                actual.onSuccess(value);
            }

            @Override
            public void onError(Throwable e) {
                actual.onError(e);
            }

            @Override
            public void onComplete() {
                actual.onComplete();
            }
        }
    }
}
