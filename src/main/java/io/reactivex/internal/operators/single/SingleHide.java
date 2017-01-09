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

package io.reactivex.internal.operators.single;

import io.reactivex.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.disposables.DisposableHelper;

public final class SingleHide<T> extends Single<T> {

    final SingleSource<? extends T> source;

    public SingleHide(SingleSource<? extends T> source) {
        this.source = source;
    }

    @Override
    protected void subscribeActual(SingleObserver<? super T> subscriber) {
        source.subscribe(new HideSingleObserver<T>(subscriber));
    }

    static final class HideSingleObserver<T> implements SingleObserver<T>, Disposable {

        final SingleObserver<? super T> actual;

        Disposable d;

        HideSingleObserver(SingleObserver<? super T> actual) {
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
        public void onSuccess(T value) {
            actual.onSuccess(value);
        }

        @Override
        public void onError(Throwable e) {
            actual.onError(e);
        }
    }

}
