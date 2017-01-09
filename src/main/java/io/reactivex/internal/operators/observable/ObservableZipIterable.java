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

import io.reactivex.internal.functions.ObjectHelper;
import java.util.Iterator;

import io.reactivex.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.functions.BiFunction;
import io.reactivex.internal.disposables.*;
import io.reactivex.plugins.RxJavaPlugins;

public final class ObservableZipIterable<T, U, V> extends Observable<V> {
    final Observable<? extends T> source;
    final Iterable<U> other;
    final BiFunction<? super T, ? super U, ? extends V> zipper;

    public ObservableZipIterable(
            Observable<? extends T> source,
            Iterable<U> other, BiFunction<? super T, ? super U, ? extends V> zipper) {
        this.source = source;
        this.other = other;
        this.zipper = zipper;
    }

    @Override
    public void subscribeActual(Observer<? super V> t) {
        Iterator<U> it;

        try {
            it = ObjectHelper.requireNonNull(other.iterator(), "The iterator returned by other is null");
        } catch (Throwable e) {
            Exceptions.throwIfFatal(e);
            EmptyDisposable.error(e, t);
            return;
        }

        boolean b;

        try {
            b = it.hasNext();
        } catch (Throwable e) {
            Exceptions.throwIfFatal(e);
            EmptyDisposable.error(e, t);
            return;
        }

        if (!b) {
            EmptyDisposable.complete(t);
            return;
        }

        source.subscribe(new ZipIterableObserver<T, U, V>(t, it, zipper));
    }

    static final class ZipIterableObserver<T, U, V> implements Observer<T>, Disposable {
        final Observer<? super V> actual;
        final Iterator<U> iterator;
        final BiFunction<? super T, ? super U, ? extends V> zipper;

        Disposable s;

        boolean done;

        ZipIterableObserver(Observer<? super V> actual, Iterator<U> iterator,
                BiFunction<? super T, ? super U, ? extends V> zipper) {
            this.actual = actual;
            this.iterator = iterator;
            this.zipper = zipper;
        }

        @Override
        public void onSubscribe(Disposable s) {
            if (DisposableHelper.validate(this.s, s)) {
                this.s = s;
                actual.onSubscribe(this);
            }
        }


        @Override
        public void dispose() {
            s.dispose();
        }

        @Override
        public boolean isDisposed() {
            return s.isDisposed();
        }


        @Override
        public void onNext(T t) {
            if (done) {
                return;
            }

            U u;

            try {
                u = ObjectHelper.requireNonNull(iterator.next(), "The iterator returned a null value");
            } catch (Throwable e) {
                Exceptions.throwIfFatal(e);
                error(e);
                return;
            }

            V v;
            try {
                v = ObjectHelper.requireNonNull(zipper.apply(t, u), "The zipper function returned a null value");
            } catch (Throwable e) {
                Exceptions.throwIfFatal(e);
                error(e);
                return;
            }

            actual.onNext(v);

            boolean b;

            try {
                b = iterator.hasNext();
            } catch (Throwable e) {
                Exceptions.throwIfFatal(e);
                error(e);
                return;
            }

            if (!b) {
                done = true;
                s.dispose();
                actual.onComplete();
            }
        }

        void error(Throwable e) {
            done = true;
            s.dispose();
            actual.onError(e);
        }

        @Override
        public void onError(Throwable t) {
            if (done) {
                RxJavaPlugins.onError(t);
                return;
            }
            done = true;
            actual.onError(t);
        }

        @Override
        public void onComplete() {
            if (done) {
                return;
            }
            done = true;
            actual.onComplete();
        }

    }
}
