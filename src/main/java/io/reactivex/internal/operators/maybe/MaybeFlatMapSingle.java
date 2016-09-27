/**
 * Copyright 2016 Netflix, Inc.
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

import io.reactivex.MaybeObserver;
import io.reactivex.MaybeSource;
import io.reactivex.Single;
import io.reactivex.SingleObserver;
import io.reactivex.SingleSource;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.functions.Function;
import io.reactivex.internal.disposables.DisposableHelper;
import io.reactivex.internal.functions.ObjectHelper;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Maps the success value of the source MaybeSource into a Single.
 * @param <T>
 */
public final class MaybeFlatMapSingle<T> extends Single<T> {

    final MaybeSource<T> source;

    final Function<? super T, ? extends SingleSource<T>> mapper;

    public MaybeFlatMapSingle(MaybeSource<T> source, Function<? super T, ? extends SingleSource<T>> mapper) {
        this.source = source;
        this.mapper = mapper;
    }

    @Override
    protected void subscribeActual(SingleObserver<? super T> s) {
        FlatMapMaybeObserver<T> parent = new FlatMapMaybeObserver<T>(s, mapper);
        s.onSubscribe(parent);
        source.subscribe(parent);
    }

    static final class FlatMapMaybeObserver<T>
    extends AtomicReference<Disposable>
    implements MaybeObserver<T>, Disposable {

        private static final long serialVersionUID = 4827726964688405508L;

        final SingleObserver actual;

        final Function<? super T, ? extends SingleSource<T>> mapper;

        FlatMapMaybeObserver(SingleObserver actual, Function<? super T, ? extends SingleSource<T>> mapper) {
            this.actual = actual;
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
            DisposableHelper.replace(this, d);
        }

        @Override
        public void onSuccess(T value) {
            SingleSource<T> ss;

            try {
                ss = ObjectHelper.requireNonNull(mapper.apply(value), "The mapper returned a null SingleSource");
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                onError(ex);
                return;
            }

            ss.subscribe(new FlatMapSingleObserver<T>(this, actual));
        }

        @Override
        public void onError(Throwable e) {
            actual.onError(e);
        }

        @Override
        public void onComplete() {
            // Ignored.
        }
    }

    static final class FlatMapSingleObserver<T> implements SingleObserver<T> {

        final AtomicReference<Disposable> parent;

        final SingleObserver<? super T> actual;

        FlatMapSingleObserver(AtomicReference<Disposable> parent, SingleObserver<? super T> actual) {
            this.parent = parent;
            this.actual = actual;
        }

        @Override
        public void onSubscribe(final Disposable d) {
            DisposableHelper.replace(parent, d);
        }

        @Override
        public void onSuccess(final T value) {
            actual.onSuccess(value);
        }

        @Override
        public void onError(final Throwable e) {
            actual.onError(e);
        }
    }
}
