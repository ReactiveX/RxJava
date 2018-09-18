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

import io.reactivex.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.functions.Function;
import io.reactivex.internal.disposables.DisposableHelper;
import io.reactivex.internal.functions.ObjectHelper;

/**
 * Maps the upstream success value into some other value.
 *
 * @param <T> the upstream value type
 * @param <R> the downstream value type
 */
public final class MaybeMap<T, R> extends AbstractMaybeWithUpstream<T, R> {

    final Function<? super T, ? extends R> mapper;

    public MaybeMap(MaybeSource<T> source, Function<? super T, ? extends R> mapper) {
        super(source);
        this.mapper = mapper;
    }

    @Override
    protected void subscribeActual(MaybeObserver<? super R> observer) {
        source.subscribe(new MapMaybeObserver<T, R>(observer, mapper));
    }

    static final class MapMaybeObserver<T, R> implements MaybeObserver<T>, Disposable {

        final MaybeObserver<? super R> downstream;

        final Function<? super T, ? extends R> mapper;

        Disposable upstream;

        MapMaybeObserver(MaybeObserver<? super R> actual, Function<? super T, ? extends R> mapper) {
            this.downstream = actual;
            this.mapper = mapper;
        }

        @Override
        public void dispose() {
            Disposable d = this.upstream;
            this.upstream = DisposableHelper.DISPOSED;
            d.dispose();
        }

        @Override
        public boolean isDisposed() {
            return upstream.isDisposed();
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
            R v;

            try {
                v = ObjectHelper.requireNonNull(mapper.apply(value), "The mapper returned a null item");
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                downstream.onError(ex);
                return;
            }

            downstream.onSuccess(v);
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
