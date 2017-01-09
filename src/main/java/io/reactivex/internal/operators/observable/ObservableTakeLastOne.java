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
import io.reactivex.internal.disposables.DisposableHelper;

public final class ObservableTakeLastOne<T> extends AbstractObservableWithUpstream<T, T> {

    public ObservableTakeLastOne(ObservableSource<T> source) {
        super(source);
    }

    @Override
    public void subscribeActual(Observer<? super T> s) {
        source.subscribe(new TakeLastOneObserver<T>(s));
    }

    static final class TakeLastOneObserver<T> implements Observer<T>, Disposable {
        final Observer<? super T> actual;

        Disposable s;

        T value;

        TakeLastOneObserver(Observer<? super T> actual) {
            this.actual = actual;
        }

        @Override
        public void onSubscribe(Disposable s) {
            if (DisposableHelper.validate(this.s, s)) {
                this.s = s;
                actual.onSubscribe(this);
            }
        }

        @Override
        public void onNext(T t) {
            value = t;
        }

        @Override
        public void onError(Throwable t) {
            value = null;
            actual.onError(t);
        }

        @Override
        public void onComplete() {
                emit();
        }

        void emit() {
            T v = value;
            if (v != null) {
                value = null;
                actual.onNext(v);
            }
            actual.onComplete();
        }

        @Override
        public void dispose() {
            value = null;
            s.dispose();
        }

        @Override
        public boolean isDisposed() {
            return s.isDisposed();
        }
    }
}
