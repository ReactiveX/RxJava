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
import io.reactivex.rxjava3.exceptions.Exceptions;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.internal.disposables.DisposableHelper;

public final class SingleFlatMap<T, R> extends Single<R> {
    final SingleSource<? extends T> source;

    final Function<? super T, ? extends SingleSource<? extends R>> mapper;

    public SingleFlatMap(SingleSource<? extends T> source, Function<? super T, ? extends SingleSource<? extends R>> mapper) {
        this.mapper = mapper;
        this.source = source;
    }

    @Override
    protected void subscribeActual(SingleObserver<? super R> downstream) {
        source.subscribe(new SingleFlatMapCallback<T, R>(downstream, mapper));
    }

    static final class SingleFlatMapCallback<T, R>
    extends AtomicReference<Disposable>
    implements SingleObserver<T>, Disposable {
        private static final long serialVersionUID = 3258103020495908596L;

        final SingleObserver<? super R> downstream;

        final Function<? super T, ? extends SingleSource<? extends R>> mapper;

        SingleFlatMapCallback(SingleObserver<? super R> actual,
                Function<? super T, ? extends SingleSource<? extends R>> mapper) {
            this.downstream = actual;
            this.mapper = mapper;
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
        public void onSubscribe(Disposable d) {
            if (DisposableHelper.setOnce(this, d)) {
                downstream.onSubscribe(this);
            }
        }

        @Override
        public void onSuccess(T value) {
            SingleSource<? extends R> o;

            try {
                o = Objects.requireNonNull(mapper.apply(value), "The single returned by the mapper is null");
            } catch (Throwable e) {
                Exceptions.throwIfFatal(e);
                downstream.onError(e);
                return;
            }

            if (!isDisposed()) {
                o.subscribe(new FlatMapSingleObserver<R>(this, downstream));
            }
        }

        @Override
        public void onError(Throwable e) {
            downstream.onError(e);
        }

        static final class FlatMapSingleObserver<R> implements SingleObserver<R> {

            final AtomicReference<Disposable> parent;

            final SingleObserver<? super R> downstream;

            FlatMapSingleObserver(AtomicReference<Disposable> parent, SingleObserver<? super R> downstream) {
                this.parent = parent;
                this.downstream = downstream;
            }

            @Override
            public void onSubscribe(final Disposable d) {
                DisposableHelper.replace(parent, d);
            }

            @Override
            public void onSuccess(final R value) {
                downstream.onSuccess(value);
            }

            @Override
            public void onError(final Throwable e) {
                downstream.onError(e);
            }
        }
    }
}
