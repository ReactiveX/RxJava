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
import io.reactivex.rxjava3.exceptions.Exceptions;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.internal.disposables.DisposableHelper;

/**
 * Maps a value into a MaybeSource and relays its signal.
 *
 * @param <T> the source value type
 * @param <R> the result value type
 */
public final class MaybeFlatten<T, R> extends AbstractMaybeWithUpstream<T, R> {

    final Function<? super T, ? extends MaybeSource<? extends R>> mapper;

    public MaybeFlatten(MaybeSource<T> source, Function<? super T, ? extends MaybeSource<? extends R>> mapper) {
        super(source);
        this.mapper = mapper;
    }

    @Override
    protected void subscribeActual(MaybeObserver<? super R> observer) {
        source.subscribe(new FlatMapMaybeObserver<T, R>(observer, mapper));
    }

    static final class FlatMapMaybeObserver<T, R>
    extends AtomicReference<Disposable>
    implements MaybeObserver<T>, Disposable {

        private static final long serialVersionUID = 4375739915521278546L;

        final MaybeObserver<? super R> downstream;

        final Function<? super T, ? extends MaybeSource<? extends R>> mapper;

        Disposable upstream;

        FlatMapMaybeObserver(MaybeObserver<? super R> actual,
                Function<? super T, ? extends MaybeSource<? extends R>> mapper) {
            this.downstream = actual;
            this.mapper = mapper;
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
                source = Objects.requireNonNull(mapper.apply(value), "The mapper returned a null MaybeSource");
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
            downstream.onError(e);
        }

        @Override
        public void onComplete() {
            downstream.onComplete();
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
