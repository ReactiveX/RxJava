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

import io.reactivex.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.disposables.*;
import io.reactivex.observers.SerializedObserver;

public final class ObservableSkipUntil<T, U> extends AbstractObservableWithUpstream<T, T> {
    final ObservableSource<U> other;
    public ObservableSkipUntil(ObservableSource<T> source, ObservableSource<U> other) {
        super(source);
        this.other = other;
    }

    @Override
    public void subscribeActual(Observer<? super T> child) {

        final SerializedObserver<T> serial = new SerializedObserver<T>(child);

        final ArrayCompositeDisposable frc = new ArrayCompositeDisposable(2);

        serial.onSubscribe(frc);

        final SkipUntilObserver<T> sus = new SkipUntilObserver<T>(serial, frc);

        other.subscribe(new SkipUntil(frc, sus, serial));

        source.subscribe(sus);
    }

    static final class SkipUntilObserver<T> implements Observer<T> {

        final Observer<? super T> actual;
        final ArrayCompositeDisposable frc;

        Disposable s;

        volatile boolean notSkipping;
        boolean notSkippingLocal;

        SkipUntilObserver(Observer<? super T> actual, ArrayCompositeDisposable frc) {
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
            if (notSkippingLocal) {
                actual.onNext(t);
            } else
            if (notSkipping) {
                notSkippingLocal = true;
                actual.onNext(t);
            }
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

    final class SkipUntil implements Observer<U> {
        private final ArrayCompositeDisposable frc;
        private final SkipUntilObserver<T> sus;
        private final SerializedObserver<T> serial;
        Disposable s;

        SkipUntil(ArrayCompositeDisposable frc, SkipUntilObserver<T> sus, SerializedObserver<T> serial) {
            this.frc = frc;
            this.sus = sus;
            this.serial = serial;
        }

        @Override
        public void onSubscribe(Disposable s) {
            if (DisposableHelper.validate(this.s, s)) {
                this.s = s;
                frc.setResource(1, s);
            }
        }

        @Override
        public void onNext(U t) {
            s.dispose();
            sus.notSkipping = true;
        }

        @Override
        public void onError(Throwable t) {
            frc.dispose();
            serial.onError(t);
        }

        @Override
        public void onComplete() {
            sus.notSkipping = true;
        }
    }
}
