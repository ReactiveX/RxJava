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

public final class ObservableSkip<T> extends AbstractObservableWithUpstream<T, T> {
    final long n;
    public ObservableSkip(ObservableSource<T> source, long n) {
        super(source);
        this.n = n;
    }

    @Override
    public void subscribeActual(Observer<? super T> s) {
        source.subscribe(new SkipObserver<T>(s, n));
    }

    static final class SkipObserver<T> implements Observer<T>, Disposable {
        final Observer<? super T> actual;
        long remaining;

        Disposable d;

        SkipObserver(Observer<? super T> actual, long n) {
            this.actual = actual;
            this.remaining = n;
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
            if (remaining != 0L) {
                remaining--;
            } else {
                actual.onNext(t);
            }
        }

        @Override
        public void onError(Throwable t) {
            actual.onError(t);
        }

        @Override
        public void onComplete() {
            actual.onComplete();
        }

        @Override
        public void dispose() {
            d.dispose();
        }

        @Override
        public boolean isDisposed() {
            return d.isDisposed();
        }
    }
}
