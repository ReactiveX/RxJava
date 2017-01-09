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

public final class ObservableMaterialize<T> extends AbstractObservableWithUpstream<T, Notification<T>> {


    public ObservableMaterialize(ObservableSource<T> source) {
        super(source);
    }

    @Override
    public void subscribeActual(Observer<? super Notification<T>> t) {
        source.subscribe(new MaterializeObserver<T>(t));
    }

    static final class MaterializeObserver<T> implements Observer<T>, Disposable {
        final Observer<? super Notification<T>> actual;

        Disposable s;

        MaterializeObserver(Observer<? super Notification<T>> actual) {
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
        public void dispose() {
            s.dispose();
        }

        @Override
        public boolean isDisposed() {
            return s.isDisposed();
        }

        @Override
        public void onNext(T t) {
            actual.onNext(Notification.createOnNext(t));
        }

        @Override
        public void onError(Throwable t) {
            Notification<T> v = Notification.createOnError(t);
            actual.onNext(v);
            actual.onComplete();
        }

        @Override
        public void onComplete() {
            Notification<T> v = Notification.createOnComplete();

            actual.onNext(v);
            actual.onComplete();
        }
    }
}
