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

import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.functions.*;
import io.reactivex.internal.disposables.DisposableHelper;
import io.reactivex.internal.functions.ObjectHelper;

/**
 * Maps a source item to another MaybeSource then calls a BiFunction with the
 * original item and the secondary item to generate the final result.
 * 
 * @param <T> the main value type
 * @param <U> the second value type
 * @param <R> the result value type
 */
public final class MaybeFlatMapBiSelector<T, U, R> extends AbstractMaybeWithUpstream<T, R> {

    final Function<? super T, ? extends MaybeSource<? extends U>> mapper;

    final BiFunction<? super T, ? super U, ? extends R> resultSelector;

    public MaybeFlatMapBiSelector(MaybeSource<T> source,
            Function<? super T, ? extends MaybeSource<? extends U>> mapper,
            BiFunction<? super T, ? super U, ? extends R> resultSelector) {
        super(source);
        this.mapper = mapper;
        this.resultSelector = resultSelector;
    }

    @Override
    protected void subscribeActual(MaybeObserver<? super R> observer) {
        source.subscribe(new FlatMapBiMainObserver<T, U, R>(observer, mapper, resultSelector));
    }

    static final class FlatMapBiMainObserver<T, U, R>
    implements MaybeObserver<T>, Disposable {

        final Function<? super T, ? extends MaybeSource<? extends U>> mapper;

        final InnerObserver<T, U, R> inner;

        FlatMapBiMainObserver(MaybeObserver<? super R> actual,
                Function<? super T, ? extends MaybeSource<? extends U>> mapper,
                BiFunction<? super T, ? super U, ? extends R> resultSelector) {
            this.inner = new InnerObserver<T, U, R>(actual, resultSelector);
            this.mapper = mapper;
        }

        @Override
        public void dispose() {
            DisposableHelper.dispose(inner);
        }

        @Override
        public boolean isDisposed() {
            return DisposableHelper.isDisposed(inner.get());
        }

        @Override
        public void onSubscribe(Disposable d) {
            if (DisposableHelper.setOnce(inner, d)) {
                inner.actual.onSubscribe(this);
            }
        }

        @Override
        public void onSuccess(T value) {
            MaybeSource<? extends U> next;

            try {
                next = ObjectHelper.requireNonNull(mapper.apply(value), "The mapper returned a null MaybeSource");
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                inner.actual.onError(ex);
                return;
            }

            if (DisposableHelper.replace(inner, null)) {
                inner.value = value;
                next.subscribe(inner);
            }
        }

        @Override
        public void onError(Throwable e) {
            inner.actual.onError(e);
        }

        @Override
        public void onComplete() {
            inner.actual.onComplete();
        }

        static final class InnerObserver<T, U, R>
        extends AtomicReference<Disposable>
        implements MaybeObserver<U> {

            private static final long serialVersionUID = -2897979525538174559L;

            final MaybeObserver<? super R> actual;

            final BiFunction<? super T, ? super U, ? extends R> resultSelector;

            T value;

            InnerObserver(MaybeObserver<? super R> actual,
                    BiFunction<? super T, ? super U, ? extends R> resultSelector) {
                this.actual = actual;
                this.resultSelector = resultSelector;
            }

            @Override
            public void onSubscribe(Disposable d) {
                DisposableHelper.setOnce(this, d);
            }

            @Override
            public void onSuccess(U value) {
                T t = this.value;
                this.value = null;

                R r;

                try {
                    r = ObjectHelper.requireNonNull(resultSelector.apply(t, value), "The resultSelector returned a null value");
                } catch (Throwable ex) {
                    Exceptions.throwIfFatal(ex);
                    actual.onError(ex);
                    return;
                }

                actual.onSuccess(r);
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
