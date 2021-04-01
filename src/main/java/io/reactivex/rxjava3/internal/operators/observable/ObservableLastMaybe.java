/*
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
import io.reactivex.rxjava3.internal.disposables.DisposableHelper;

/**
 * Consumes the source ObservableSource and emits its last item, the defaultItem
 * if empty or a NoSuchElementException if even the defaultItem is null.
 * 
 * @param <T> the value type
 */
public final class ObservableLastMaybe<T> extends Maybe<T> {

    final ObservableSource<T> source;

    public ObservableLastMaybe(ObservableSource<T> source) {
        this.source = source;
    }

    // TODO fuse back to Observable

    @Override
    protected void subscribeActual(MaybeObserver<? super T> observer) {
        source.subscribe(new LastObserver<>(observer));
    }

    static final class LastObserver<T> implements Observer<T>, Disposable {

        final MaybeObserver<? super T> downstream;

        Disposable upstream;

        T item;

        LastObserver(MaybeObserver<? super T> downstream) {
            this.downstream = downstream;
        }

        @Override
        public void dispose() {
            upstream.dispose();
            upstream = DisposableHelper.DISPOSED;
        }

        @Override
        public boolean isDisposed() {
            return upstream == DisposableHelper.DISPOSED;
        }

        @Override
        public void onSubscribe(Disposable d) {
            if (DisposableHelper.validate(this.upstream, d)) {
                this.upstream = d;

                downstream.onSubscribe(this);
            }
        }

        @Override
        public void onNext(T t) {
            item = t;
        }

        @Override
        public void onError(Throwable t) {
            upstream = DisposableHelper.DISPOSED;
            item = null;
            downstream.onError(t);
        }

        @Override
        public void onComplete() {
            upstream = DisposableHelper.DISPOSED;
            T v = item;
            if (v != null) {
                item = null;
                downstream.onSuccess(v);
            } else {
                downstream.onComplete();
            }
        }
    }
}
