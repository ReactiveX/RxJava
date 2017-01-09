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
import io.reactivex.internal.util.EmptyComponent;

/**
 * Breaks the links between the upstream and the downstream (the Disposable and
 * the Observer references) when the sequence terminates or gets disposed.
 *
 * @param <T> the value type
 */
public final class ObservableDetach<T> extends AbstractObservableWithUpstream<T, T> {

    public ObservableDetach(ObservableSource<T> source) {
        super(source);
    }

    @Override
    protected void subscribeActual(Observer<? super T> s) {
        source.subscribe(new DetachObserver<T>(s));
    }

    static final class DetachObserver<T> implements Observer<T>, Disposable {

        Observer<? super T> actual;

        Disposable s;

        DetachObserver(Observer<? super T> actual) {
            this.actual = actual;
        }

        @Override
        public void dispose() {
            Disposable s = this.s;
            this.s = EmptyComponent.INSTANCE;
            this.actual = EmptyComponent.asObserver();
            s.dispose();
        }

        @Override
        public boolean isDisposed() {
            return s.isDisposed();
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
            actual.onNext(t);
        }

        @Override
        public void onError(Throwable t) {
            Observer<? super T> a = actual;
            this.s = EmptyComponent.INSTANCE;
            this.actual = EmptyComponent.asObserver();
            a.onError(t);
        }

        @Override
        public void onComplete() {
            Observer<? super T> a = actual;
            this.s = EmptyComponent.INSTANCE;
            this.actual = EmptyComponent.asObserver();
            a.onComplete();
        }
    }
}

