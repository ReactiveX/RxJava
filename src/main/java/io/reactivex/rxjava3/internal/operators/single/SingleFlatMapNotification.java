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

package io.reactivex.rxjava3.internal.operators.single;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.exceptions.*;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.internal.disposables.DisposableHelper;

/**
 * Maps a value into a SingleSource and relays its signal.
 *
 * @param <T> the source value type
 * @param <R> the result value type
 * @since 3.0.0
 */
public final class SingleFlatMapNotification<T, R> extends Single<R> {

    final SingleSource<T> source;

    final Function<? super T, ? extends SingleSource<? extends R>> onSuccessMapper;

    final Function<? super Throwable, ? extends SingleSource<? extends R>> onErrorMapper;

    public SingleFlatMapNotification(SingleSource<T> source,
            Function<? super T, ? extends SingleSource<? extends R>> onSuccessMapper,
            Function<? super Throwable, ? extends SingleSource<? extends R>> onErrorMapper) {
        this.source = source;
        this.onSuccessMapper = onSuccessMapper;
        this.onErrorMapper = onErrorMapper;
    }

    @Override
    protected void subscribeActual(SingleObserver<? super R> observer) {
        source.subscribe(new FlatMapSingleObserver<>(observer, onSuccessMapper, onErrorMapper));
    }

    static final class FlatMapSingleObserver<T, R>
    extends AtomicReference<Disposable>
    implements SingleObserver<T>, Disposable {

        private static final long serialVersionUID = 4375739915521278546L;

        final SingleObserver<? super R> downstream;

        final Function<? super T, ? extends SingleSource<? extends R>> onSuccessMapper;

        final Function<? super Throwable, ? extends SingleSource<? extends R>> onErrorMapper;

        Disposable upstream;

        FlatMapSingleObserver(SingleObserver<? super R> actual,
                Function<? super T, ? extends SingleSource<? extends R>> onSuccessMapper,
                Function<? super Throwable, ? extends SingleSource<? extends R>> onErrorMapper) {
            this.downstream = actual;
            this.onSuccessMapper = onSuccessMapper;
            this.onErrorMapper = onErrorMapper;
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
            SingleSource<? extends R> source;

            try {
                source = Objects.requireNonNull(onSuccessMapper.apply(value), "The onSuccessMapper returned a null SingleSource");
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                downstream.onError(ex);
                return;
            }

            if (!isDisposed()) {
                source.subscribe(new InnerObserver());
            }
        }

        @Override
        public void onError(Throwable e) {
            SingleSource<? extends R> source;

            try {
                source = Objects.requireNonNull(onErrorMapper.apply(e), "The onErrorMapper returned a null SingleSource");
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                downstream.onError(new CompositeException(e, ex));
                return;
            }

            if (!isDisposed()) {
                source.subscribe(new InnerObserver());
            }
        }

        final class InnerObserver implements SingleObserver<R> {

            @Override
            public void onSubscribe(Disposable d) {
                DisposableHelper.setOnce(FlatMapSingleObserver.this, d);
            }

            @Override
            public void onSuccess(R value) {
                downstream.onSuccess(value);
            }

            @Override
            public void onError(Throwable e) {
                downstream.onError(e);
            }
        }
    }
}
