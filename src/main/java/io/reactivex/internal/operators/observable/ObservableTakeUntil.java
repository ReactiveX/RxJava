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

import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.disposables.*;
import io.reactivex.observers.SerializedObserver;

public final class ObservableTakeUntil<T, U> extends AbstractObservableWithUpstream<T, T> {
    final ObservableSource<? extends U> other;
    public ObservableTakeUntil(ObservableSource<T> source, ObservableSource<? extends U> other) {
        super(source);
        this.other = other;
    }
    @Override
    public void subscribeActual(Observer<? super T> child) {
        final SerializedObserver<T> serial = new SerializedObserver<T>(child);

        final ArrayCompositeDisposable frc = new ArrayCompositeDisposable(2);

        final TakeUntilObserver<T> tus = new TakeUntilObserver<T>(serial, frc);

        child.onSubscribe(frc);

        other.subscribe(new TakeUntil(frc, serial));

        source.subscribe(tus);
    }

    static final class TakeUntilObserver<T> extends AtomicBoolean implements Observer<T> {

        private static final long serialVersionUID = 3451719290311127173L;
        final Observer<? super T> actual;
        final ArrayCompositeDisposable frc;

        Disposable s;

        TakeUntilObserver(Observer<? super T> actual, ArrayCompositeDisposable frc) {
            this.actual = actual;
            this.frc = frc;
        }

        @Override
        public void onSubscribe(Disposable s) {
            if (DisposableHelper.validate(this.s, s)) {
                this.s = s;
                frc.setResource(0, s);
            }
        }

        @Override
        public void onNext(T t) {
            actual.onNext(t);
        }

        @Override
        public void onError(Throwable t) {
            frc.dispose();
            actual.onError(t);
        }

        @Override
        public void onComplete() {
            frc.dispose();
            actual.onComplete();
        }
    }

    final class TakeUntil implements Observer<U> {
        private final ArrayCompositeDisposable frc;
        private final SerializedObserver<T> serial;

        TakeUntil(ArrayCompositeDisposable frc, SerializedObserver<T> serial) {
            this.frc = frc;
            this.serial = serial;
        }

        @Override
        public void onSubscribe(Disposable s) {
            frc.setResource(1, s);
        }

        @Override
        public void onNext(U t) {
            frc.dispose();
            serial.onComplete();
        }

        @Override
        public void onError(Throwable t) {
            frc.dispose();
            serial.onError(t);
        }

        @Override
        public void onComplete() {
            frc.dispose();
            serial.onComplete();
        }
    }
}
