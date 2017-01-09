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
import io.reactivex.internal.fuseable.FuseToObservable;
import io.reactivex.plugins.RxJavaPlugins;

public final class ObservableCountSingle<T> extends Single<Long> implements FuseToObservable<Long> {
    final ObservableSource<T> source;
    public ObservableCountSingle(ObservableSource<T> source) {
        this.source = source;
    }

    @Override
    public void subscribeActual(SingleObserver<? super Long> t) {
        source.subscribe(new CountObserver(t));
    }

    @Override
    public Observable<Long> fuseToObservable() {
        return RxJavaPlugins.onAssembly(new ObservableCount<T>(source));
    }

    static final class CountObserver implements Observer<Object>, Disposable {
        final SingleObserver<? super Long> actual;

        Disposable d;

        long count;

        CountObserver(SingleObserver<? super Long> actual) {
            this.actual = actual;
        }

        @Override
        public void onSubscribe(Disposable d) {
            if (DisposableHelper.validate(this.d, d)) {
                this.d = d;
                actual.onSubscribe(this);
            }
        }


        @Override
        public void dispose() {
            d.dispose();
            d = DisposableHelper.DISPOSED;
        }

        @Override
        public boolean isDisposed() {
            return d.isDisposed();
        }

        @Override
        public void onNext(Object t) {
            count++;
        }

        @Override
        public void onError(Throwable t) {
            d = DisposableHelper.DISPOSED;
            actual.onError(t);
        }

        @Override
        public void onComplete() {
            d = DisposableHelper.DISPOSED;
            actual.onSuccess(count);
        }
    }
}
