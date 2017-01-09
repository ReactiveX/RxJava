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
import java.util.concurrent.Callable;

import io.reactivex.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.functions.BiFunction;
import io.reactivex.internal.disposables.*;
import io.reactivex.plugins.RxJavaPlugins;

public final class ObservableScanSeed<T, R> extends AbstractObservableWithUpstream<T, R> {
    final BiFunction<R, ? super T, R> accumulator;
    final Callable<R> seedSupplier;

    public ObservableScanSeed(ObservableSource<T> source, Callable<R> seedSupplier, BiFunction<R, ? super T, R> accumulator) {
        super(source);
        this.accumulator = accumulator;
        this.seedSupplier = seedSupplier;
    }

    @Override
    public void subscribeActual(Observer<? super R> t) {
        R r;

        try {
            r = ObjectHelper.requireNonNull(seedSupplier.call(), "The seed supplied is null");
        } catch (Throwable e) {
            Exceptions.throwIfFatal(e);
            EmptyDisposable.error(e, t);
            return;
        }

        source.subscribe(new ScanSeedObserver<T, R>(t, accumulator, r));
    }

    static final class ScanSeedObserver<T, R> implements Observer<T>, Disposable {
        final Observer<? super R> actual;
        final BiFunction<R, ? super T, R> accumulator;

        R value;

        Disposable s;

        boolean done;

        ScanSeedObserver(Observer<? super R> actual, BiFunction<R, ? super T, R> accumulator, R value) {
            this.actual = actual;
            this.accumulator = accumulator;
            this.value = value;
        }

        @Override
        public void onSubscribe(Disposable s) {
            if (DisposableHelper.validate(this.s, s)) {
                this.s = s;

                actual.onSubscribe(this);

                actual.onNext(value);
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

            R v = value;

            R u;

            try {
                u = ObjectHelper.requireNonNull(accumulator.apply(v, t), "The accumulator returned a null value");
            } catch (Throwable e) {
                Exceptions.throwIfFatal(e);
                s.dispose();
                onError(e);
                return;
            }

            value = u;

            actual.onNext(u);
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
