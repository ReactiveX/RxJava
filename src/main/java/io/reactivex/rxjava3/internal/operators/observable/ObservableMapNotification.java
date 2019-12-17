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

package io.reactivex.rxjava3.internal.operators.observable;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.exceptions.*;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.internal.disposables.DisposableHelper;

import java.util.Objects;

public final class ObservableMapNotification<T, R> extends AbstractObservableWithUpstream<T, ObservableSource<? extends R>> {

    final Function<? super T, ? extends ObservableSource<? extends R>> onNextMapper;
    final Function<? super Throwable, ? extends ObservableSource<? extends R>> onErrorMapper;
    final Supplier<? extends ObservableSource<? extends R>> onCompleteSupplier;

    public ObservableMapNotification(
            ObservableSource<T> source,
            Function<? super T, ? extends ObservableSource<? extends R>> onNextMapper,
            Function<? super Throwable, ? extends ObservableSource<? extends R>> onErrorMapper,
            Supplier<? extends ObservableSource<? extends R>> onCompleteSupplier) {
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
        final Observer<? super ObservableSource<? extends R>> downstream;
        final Function<? super T, ? extends ObservableSource<? extends R>> onNextMapper;
        final Function<? super Throwable, ? extends ObservableSource<? extends R>> onErrorMapper;
        final Supplier<? extends ObservableSource<? extends R>> onCompleteSupplier;

        Disposable upstream;

        MapNotificationObserver(Observer<? super ObservableSource<? extends R>> actual,
                Function<? super T, ? extends ObservableSource<? extends R>> onNextMapper,
                Function<? super Throwable, ? extends ObservableSource<? extends R>> onErrorMapper,
                Supplier<? extends ObservableSource<? extends R>> onCompleteSupplier) {
            this.downstream = actual;
            this.onNextMapper = onNextMapper;
            this.onErrorMapper = onErrorMapper;
            this.onCompleteSupplier = onCompleteSupplier;
        }

        @Override
        public void onSubscribe(Disposable d) {
            if (DisposableHelper.validate(this.upstream, d)) {
                this.upstream = d;
                downstream.onSubscribe(this);
            }
        }

        @Override
        public void dispose() {
            upstream.dispose();
        }

        @Override
        public boolean isDisposed() {
            return upstream.isDisposed();
        }

        @Override
        public void onNext(T t) {
            ObservableSource<? extends R> p;

            try {
                p = Objects.requireNonNull(onNextMapper.apply(t), "The onNext ObservableSource returned is null");
            } catch (Throwable e) {
                Exceptions.throwIfFatal(e);
                downstream.onError(e);
                return;
            }

            downstream.onNext(p);
        }

        @Override
        public void onError(Throwable t) {
            ObservableSource<? extends R> p;

            try {
                p = Objects.requireNonNull(onErrorMapper.apply(t), "The onError ObservableSource returned is null");
            } catch (Throwable e) {
                Exceptions.throwIfFatal(e);
                downstream.onError(new CompositeException(t, e));
                return;
            }

            downstream.onNext(p);
            downstream.onComplete();
        }

        @Override
        public void onComplete() {
            ObservableSource<? extends R> p;

            try {
                p = Objects.requireNonNull(onCompleteSupplier.get(), "The onComplete ObservableSource returned is null");
            } catch (Throwable e) {
                Exceptions.throwIfFatal(e);
                downstream.onError(e);
                return;
            }

            downstream.onNext(p);
            downstream.onComplete();
        }
    }
}
