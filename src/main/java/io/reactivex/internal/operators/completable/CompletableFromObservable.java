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

package io.reactivex.internal.operators.completable;

import io.reactivex.*;
import io.reactivex.disposables.Disposable;

public final class CompletableFromObservable<T> extends Completable {

    final ObservableSource<T> observable;

    public CompletableFromObservable(ObservableSource<T> observable) {
        this.observable = observable;
    }

    @Override
    protected void subscribeActual(final CompletableObserver s) {
        observable.subscribe(new CompletableFromObservableObserver<T>(s));
    }

    static final class CompletableFromObservableObserver<T> implements Observer<T> {
        final CompletableObserver co;

        CompletableFromObservableObserver(CompletableObserver co) {
            this.co = co;
        }

        @Override
        public void onSubscribe(Disposable d) {
            co.onSubscribe(d);
        }

        @Override
        public void onNext(T value) {
            // Deliberately ignored.
        }

        @Override
        public void onError(Throwable e) {
            co.onError(e);
        }

        @Override
        public void onComplete() {
            co.onComplete();
        }
    }
}
