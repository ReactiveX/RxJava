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

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.exceptions.*;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.internal.disposables.DisposableHelper;

/**
 * Maps a value into a MaybeSource and relays its signal.
 *
 * @param <T> the source value type
 * @param <R> the result value type
 */
public final class MaybeFlatMapNotification<T, R> extends AbstractMaybeWithUpstream<T, R> {

    final Function<? super T, ? extends MaybeSource<? extends R>> onSuccessMapper;

    final Function<? super Throwable, ? extends MaybeSource<? extends R>> onErrorMapper;

    final Supplier<? extends MaybeSource<? extends R>> onCompleteSupplier;

    public MaybeFlatMapNotification(MaybeSource<T> source,
            Function<? super T, ? extends MaybeSource<? extends R>> onSuccessMapper,
            Function<? super Throwable, ? extends MaybeSource<? extends R>> onErrorMapper,
            Supplier<? extends MaybeSource<? extends R>> onCompleteSupplier) {
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

        final MaybeObserver<? super R> downstream;

        final Function<? super T, ? extends MaybeSource<? extends R>> onSuccessMapper;

        final Function<? super Throwable, ? extends MaybeSource<? extends R>> onErrorMapper;

        final Supplier<? extends MaybeSource<? extends R>> onCompleteSupplier;

        Disposable upstream;

        FlatMapMaybeObserver(MaybeObserver<? super R> actual,
                Function<? super T, ? extends MaybeSource<? extends R>> onSuccessMapper,
                Function<? super Throwable, ? extends MaybeSource<? extends R>> onErrorMapper,
                Supplier<? extends MaybeSource<? extends R>> onCompleteSupplier) {
            this.downstream = actual;
            this.onSuccessMapper = onSuccessMapper;
            this.onErrorMapper = onErrorMapper;
            this.onCompleteSupplier = onCompleteSupplier;
        }

        @Override
        public void dispose() {
            DisposableHelper.dispose(this);
            upstream.dispose();
        }

        @Override
        public boolean isDisposed() {
            return DisposableHelper.isDisposed(get());
        }

        @Override
        public void onSubscribe(Disposable d) {
            if (DisposableHelper.validate(this.upstream, d)) {
                this.upstream = d;

                downstream.onSubscribe(this);
            }
        }

        @Override
        public void onSuccess(T value) {
            MaybeSource<? extends R> source;

            try {
                source = Objects.requireNonNull(onSuccessMapper.apply(value), "The onSuccessMapper returned a null MaybeSource");
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                downstream.onError(ex);
                return;
            }

            source.subscribe(new InnerObserver());
        }

        @Override
        public void onError(Throwable e) {
            MaybeSource<? extends R> source;

            try {
                source = Objects.requireNonNull(onErrorMapper.apply(e), "The onErrorMapper returned a null MaybeSource");
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                downstream.onError(new CompositeException(e, ex));
                return;
            }

            source.subscribe(new InnerObserver());
        }

        @Override
        public void onComplete() {
            MaybeSource<? extends R> source;

            try {
                source = Objects.requireNonNull(onCompleteSupplier.get(), "The onCompleteSupplier returned a null MaybeSource");
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                downstream.onError(ex);
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
                downstream.onSuccess(value);
            }

            @Override
            public void onError(Throwable e) {
                downstream.onError(e);
            }

            @Override
            public void onComplete() {
                downstream.onComplete();
            }
        }
    }
}
