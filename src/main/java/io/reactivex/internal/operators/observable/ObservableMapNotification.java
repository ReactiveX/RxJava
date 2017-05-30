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

import java.util.concurrent.Callable;

import io.reactivex.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.*;
import io.reactivex.functions.Function;
import io.reactivex.internal.disposables.DisposableHelper;
import io.reactivex.internal.functions.ObjectHelper;

public final class ObservableMapNotification<T, R> extends AbstractObservableWithUpstream<T, ObservableSource<? extends R>> {

    final Function<? super T, ? extends ObservableSource<? extends R>> onNextMapper;
    final Function<? super Throwable, ? extends ObservableSource<? extends R>> onErrorMapper;
    final Callable<? extends ObservableSource<? extends R>> onCompleteSupplier;

    public ObservableMapNotification(
            ObservableSource<T> source,
            Function<? super T, ? extends ObservableSource<? extends R>> onNextMapper,
            Function<? super Throwable, ? extends ObservableSource<? extends R>> onErrorMapper,
                    Callable<? extends ObservableSource<? extends R>> onCompleteSupplier) {
        super(source);
        this.onNextMapper = onNextMapper;
        this.onErrorMapper = onErrorMapper;
        this.onCompleteSupplier = onCompleteSupplier;
    }

    @Override
    public void subscribeActual(Observer<? super ObservableSource<? extends R>> t) {
        source.subscribe(new MapNotificationObserver<T, R>(t, onNextMapper, onErrorMapper, onCompleteSupplier));
    }

    static final class MapNotificationObserver<T, R>
    implements Observer<T>, Disposable {
        final Observer<? super ObservableSource<? extends R>> actual;
        final Function<? super T, ? extends ObservableSource<? extends R>> onNextMapper;
        final Function<? super Throwable, ? extends ObservableSource<? extends R>> onErrorMapper;
        final Callable<? extends ObservableSource<? extends R>> onCompleteSupplier;

        Disposable s;

        MapNotificationObserver(Observer<? super ObservableSource<? extends R>> actual,
                Function<? super T, ? extends ObservableSource<? extends R>> onNextMapper,
                Function<? super Throwable, ? extends ObservableSource<? extends R>> onErrorMapper,
                        Callable<? extends ObservableSource<? extends R>> onCompleteSupplier) {
            this.actual = actual;
            this.onNextMapper = onNextMapper;
            this.onErrorMapper = onErrorMapper;
            this.onCompleteSupplier = onCompleteSupplier;
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
            ObservableSource<? extends R> p;

            try {
                p = ObjectHelper.requireNonNull(onNextMapper.apply(t), "The onNext ObservableSource returned is null");
            } catch (Throwable e) {
                Exceptions.throwIfFatal(e);
                actual.onError(e);
                return;
            }

            actual.onNext(p);
        }

        @Override
        public void onError(Throwable t) {
            ObservableSource<? extends R> p;

            try {
                p = ObjectHelper.requireNonNull(onErrorMapper.apply(t), "The onError ObservableSource returned is null");
            } catch (Throwable e) {
                Exceptions.throwIfFatal(e);
                actual.onError(new CompositeException(t, e));
                return;
            }

            actual.onNext(p);
            actual.onComplete();
        }

        @Override
        public void onComplete() {
            ObservableSource<? extends R> p;

            try {
                p = ObjectHelper.requireNonNull(onCompleteSupplier.call(), "The onComplete ObservableSource returned is null");
            } catch (Throwable e) {
                Exceptions.throwIfFatal(e);
                actual.onError(e);
                return;
            }

            actual.onNext(p);
            actual.onComplete();
        }
    }
}
