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

package io.reactivex.internal.operators.observable;

import java.util.Iterator;

import io.reactivex.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.functions.Function;
import io.reactivex.internal.disposables.DisposableHelper;
import io.reactivex.internal.functions.ObjectHelper;
import io.reactivex.plugins.RxJavaPlugins;

/**
 * Maps a sequence into an Iterable and emits its values.
 *
 * @param <T> the input value type to map to Iterable
 * @param <R> the element type of the Iterable and the output
 */
public final class ObservableFlattenIterable<T, R> extends AbstractObservableWithUpstream<T, R> {

    final Function<? super T, ? extends Iterable<? extends R>> mapper;

    public ObservableFlattenIterable(ObservableSource<T> source,
            Function<? super T, ? extends Iterable<? extends R>> mapper) {
        super(source);
        this.mapper = mapper;
    }

    @Override
    protected void subscribeActual(Observer<? super R> observer) {
        source.subscribe(new FlattenIterableObserver<T, R>(observer, mapper));
    }

    static final class FlattenIterableObserver<T, R> implements Observer<T>, Disposable {
        final Observer<? super R> actual;

        final Function<? super T, ? extends Iterable<? extends R>> mapper;

        Disposable d;

        FlattenIterableObserver(Observer<? super R> actual, Function<? super T, ? extends Iterable<? extends R>> mapper) {
            this.actual = actual;
            this.mapper = mapper;
        }

        @Override
        public void onSubscribe(Disposable d) {
            if (DisposableHelper.validate(this.d, d)) {
                this.d = d;

                actual.onSubscribe(this);
            }
        }

        @Override
        public void onNext(T value) {
            if (d == DisposableHelper.DISPOSED) {
                return;
            }

            Iterator<? extends R> it;

            try {
                it = mapper.apply(value).iterator();
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                d.dispose();
                onError(ex);
                return;
            }

            Observer<? super R> a = actual;

            for (;;) {
                boolean b;

                try {
                    b = it.hasNext();
                } catch (Throwable ex) {
                    Exceptions.throwIfFatal(ex);
                    d.dispose();
                    onError(ex);
                    return;
                }

                if (b) {
                    R v;

                    try {
                        v = ObjectHelper.requireNonNull(it.next(), "The iterator returned a null value");
                    } catch (Throwable ex) {
                        Exceptions.throwIfFatal(ex);
                        d.dispose();
                        onError(ex);
                        return;
                    }

                    a.onNext(v);
                } else {
                    break;
                }
            }
        }

        @Override
        public void onError(Throwable e) {
            if (d == DisposableHelper.DISPOSED) {
                RxJavaPlugins.onError(e);
                return;
            }
            d = DisposableHelper.DISPOSED;
            actual.onError(e);
        }

        @Override
        public void onComplete() {
            if (d == DisposableHelper.DISPOSED) {
                return;
            }
            d = DisposableHelper.DISPOSED;
            actual.onComplete();
        }

        @Override
        public boolean isDisposed() {
            return d.isDisposed();
        }

        @Override
        public void dispose() {
            d.dispose();
            d = DisposableHelper.DISPOSED;
        }
    }
}
