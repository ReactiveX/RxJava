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

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.exceptions.Exceptions;
import io.reactivex.rxjava3.functions.BiFunction;
import io.reactivex.rxjava3.internal.disposables.DisposableHelper;
import io.reactivex.rxjava3.observers.SerializedObserver;

public final class ObservableWithLatestFrom<T, U, R> extends AbstractObservableWithUpstream<T, R> {
    final BiFunction<? super T, ? super U, ? extends R> combiner;
    final ObservableSource<? extends U> other;
    public ObservableWithLatestFrom(ObservableSource<T> source,
            BiFunction<? super T, ? super U, ? extends R> combiner, ObservableSource<? extends U> other) {
        super(source);
        this.combiner = combiner;
        this.other = other;
    }

    @Override
    public void subscribeActual(Observer<? super R> t) {
        final SerializedObserver<R> serial = new SerializedObserver<R>(t);
        final WithLatestFromObserver<T, U, R> wlf = new WithLatestFromObserver<T, U, R>(serial, combiner);

        serial.onSubscribe(wlf);

        other.subscribe(new WithLatestFromOtherObserver(wlf));

        source.subscribe(wlf);
    }

    static final class WithLatestFromObserver<T, U, R> extends AtomicReference<U> implements Observer<T>, Disposable {

        private static final long serialVersionUID = -312246233408980075L;

        final Observer<? super R> downstream;

        final BiFunction<? super T, ? super U, ? extends R> combiner;

        final AtomicReference<Disposable> upstream = new AtomicReference<Disposable>();

        final AtomicReference<Disposable> other = new AtomicReference<Disposable>();

        WithLatestFromObserver(Observer<? super R> actual, BiFunction<? super T, ? super U, ? extends R> combiner) {
            this.downstream = actual;
            this.combiner = combiner;
        }

        @Override
        public void onSubscribe(Disposable d) {
            DisposableHelper.setOnce(this.upstream, d);
        }

        @Override
        public void onNext(T t) {
            U u = get();
            if (u != null) {
                R r;
                try {
                    r = Objects.requireNonNull(combiner.apply(t, u), "The combiner returned a null value");
                } catch (Throwable e) {
                    Exceptions.throwIfFatal(e);
                    dispose();
                    downstream.onError(e);
                    return;
                }
                downstream.onNext(r);
            }
        }

        @Override
        public void onError(Throwable t) {
            DisposableHelper.dispose(other);
            downstream.onError(t);
        }

        @Override
        public void onComplete() {
            DisposableHelper.dispose(other);
            downstream.onComplete();
        }

        @Override
        public void dispose() {
            DisposableHelper.dispose(upstream);
            DisposableHelper.dispose(other);
        }

        @Override
        public boolean isDisposed() {
            return DisposableHelper.isDisposed(upstream.get());
        }

        public boolean setOther(Disposable o) {
            return DisposableHelper.setOnce(other, o);
        }

        public void otherError(Throwable e) {
            DisposableHelper.dispose(upstream);
            downstream.onError(e);
        }
    }

    final class WithLatestFromOtherObserver implements Observer<U> {
        private final WithLatestFromObserver<T, U, R> parent;

        WithLatestFromOtherObserver(WithLatestFromObserver<T, U, R> parent) {
            this.parent = parent;
        }

        @Override
        public void onSubscribe(Disposable d) {
            parent.setOther(d);
        }

        @Override
        public void onNext(U t) {
            parent.lazySet(t);
        }

        @Override
        public void onError(Throwable t) {
            parent.otherError(t);
        }

        @Override
        public void onComplete() {
            // nothing to do, the wlf will complete on its own pace
        }
    }
}
