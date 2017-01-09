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

/**
 * Hides the identity of the wrapped ObservableSource and its Disposable.
 * @param <T> the value type
 *
 * @since 2.0
 */
public final class ObservableHide<T> extends AbstractObservableWithUpstream<T, T> {

    public ObservableHide(ObservableSource<T> source) {
        super(source);
    }

    @Override
    protected void subscribeActual(Observer<? super T> o) {
        source.subscribe(new HideDisposable<T>(o));
    }

    static final class HideDisposable<T> implements Observer<T>, Disposable {

        final Observer<? super T> actual;

        Disposable d;

        HideDisposable(Observer<? super T> actual) {
            this.actual = actual;
        }

        @Override
        public void dispose() {
            d.dispose();
        }

        @Override
        public boolean isDisposed() {
            return d.isDisposed();
        }

        @Override
        public void onSubscribe(Disposable d) {
            if (DisposableHelper.validate(this.d, d)) {
                this.d = d;
                actual.onSubscribe(this);
            }
        }

        @Override
        public void onNext(T t) {
            actual.onNext(t);
        }

        @Override
        public void onError(Throwable t) {
            actual.onError(t);
        }

        @Override
        public void onComplete() {
            actual.onComplete();
        }
    }
}
