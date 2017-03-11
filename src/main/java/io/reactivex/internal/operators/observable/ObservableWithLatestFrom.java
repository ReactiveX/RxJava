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

import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.functions.BiFunction;
import io.reactivex.internal.disposables.DisposableHelper;
import io.reactivex.internal.functions.ObjectHelper;
import io.reactivex.observers.SerializedObserver;

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

        other.subscribe(new WithLastFrom(wlf));

        source.subscribe(wlf);
    }

    static final class WithLatestFromObserver<T, U, R> extends AtomicReference<U> implements Observer<T>, Disposable {

        private static final long serialVersionUID = -312246233408980075L;

        final Observer<? super R> actual;

        final BiFunction<? super T, ? super U, ? extends R> combiner;

        final AtomicReference<Disposable> s = new AtomicReference<Disposable>();

        final AtomicReference<Disposable> other = new AtomicReference<Disposable>();

        WithLatestFromObserver(Observer<? super R> actual, BiFunction<? super T, ? super U, ? extends R> combiner) {
            this.actual = actual;
            this.combiner = combiner;
        }
        @Override
        public void onSubscribe(Disposable s) {
            DisposableHelper.setOnce(this.s, s);
        }

        @Override
        public void onNext(T t) {
            U u = get();
            if (u != null) {
                R r;
                try {
                    r = ObjectHelper.requireNonNull(combiner.apply(t, u), "The combiner returned a null value");
                } catch (Throwable e) {
                    Exceptions.throwIfFatal(e);
                    dispose();
                    actual.onError(e);
                    return;
                }
                actual.onNext(r);
            }
        }

        @Override
        public void onError(Throwable t) {
            DisposableHelper.dispose(other);
            actual.onError(t);
        }

        @Override
        public void onComplete() {
            DisposableHelper.dispose(other);
            actual.onComplete();
        }

        @Override
        public void dispose() {
            DisposableHelper.dispose(s);
            DisposableHelper.dispose(other);
        }

        @Override
        public boolean isDisposed() {
            return DisposableHelper.isDisposed(s.get());
        }

        public boolean setOther(Disposable o) {
            return DisposableHelper.setOnce(other, o);
        }

        public void otherError(Throwable e) {
            DisposableHelper.dispose(s);
            actual.onError(e);
        }
    }

    final class WithLastFrom implements Observer<U> {
        private final WithLatestFromObserver<T, U, R> wlf;

        WithLastFrom(WithLatestFromObserver<T, U, R> wlf) {
            this.wlf = wlf;
        }

        @Override
        public void onSubscribe(Disposable s) {
            wlf.setOther(s);
        }

        @Override
        public void onNext(U t) {
            wlf.lazySet(t);
        }

        @Override
        public void onError(Throwable t) {
            wlf.otherError(t);
        }

        @Override
        public void onComplete() {
            // nothing to do, the wlf will complete on its own pace
        }
    }
}
