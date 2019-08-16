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

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.disposables.Disposable;

public final class ObservableIgnoreElements<T> extends AbstractObservableWithUpstream<T, T> {

    public ObservableIgnoreElements(ObservableSource<T> source) {
        super(source);
    }

    @Override
    public void subscribeActual(final Observer<? super T> t) {
        source.subscribe(new IgnoreObservable<T>(t));
    }

    static final class IgnoreObservable<T> implements Observer<T>, Disposable {
        final Observer<? super T> downstream;

        Disposable upstream;

        IgnoreObservable(Observer<? super T> t) {
            this.downstream = t;
        }

        @Override
        public void onSubscribe(Disposable d) {
            this.upstream = d;
            downstream.onSubscribe(this);
        }

        @Override
        public void onNext(T v) {
            // deliberately ignored
        }

        @Override
        public void onError(Throwable e) {
            downstream.onError(e);
        }

        @Override
        public void onComplete() {
            downstream.onComplete();
        }

        @Override
        public void dispose() {
            upstream.dispose();
        }

        @Override
        public boolean isDisposed() {
            return upstream.isDisposed();
        }
    }

}
