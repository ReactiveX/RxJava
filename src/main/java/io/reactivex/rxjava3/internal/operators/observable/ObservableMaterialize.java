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
import io.reactivex.rxjava3.internal.disposables.DisposableHelper;

public final class ObservableMaterialize<T> extends AbstractObservableWithUpstream<T, Notification<T>> {

    public ObservableMaterialize(ObservableSource<T> source) {
        super(source);
    }

    @Override
    public void subscribeActual(Observer<? super Notification<T>> t) {
        source.subscribe(new MaterializeObserver<T>(t));
    }

    static final class MaterializeObserver<T> implements Observer<T>, Disposable {
        final Observer<? super Notification<T>> downstream;

        Disposable upstream;

        MaterializeObserver(Observer<? super Notification<T>> downstream) {
            this.downstream = downstream;
        }

        @Override
        public void onSubscribe(Disposable d) {
            if (DisposableHelper.validate(this.upstream, d)) {
                this.upstream = d;
                downstream.onSubscribe(this);
            }
        }

        @Override
        public void dispose() {
            upstream.dispose();
        }

        @Override
        public boolean isDisposed() {
            return upstream.isDisposed();
        }

        @Override
        public void onNext(T t) {
            downstream.onNext(Notification.createOnNext(t));
        }

        @Override
        public void onError(Throwable t) {
            Notification<T> v = Notification.createOnError(t);
            downstream.onNext(v);
            downstream.onComplete();
        }

        @Override
        public void onComplete() {
            Notification<T> v = Notification.createOnComplete();

            downstream.onNext(v);
            downstream.onComplete();
        }
    }
}
