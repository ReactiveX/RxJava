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
import io.reactivex.annotations.Experimental;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.disposables.DisposableHelper;

/**
 * Wraps a Single and exposes it as an Observable.
 *
 * @param <T> the value type
 */
public final class SingleToObservable<T> extends Observable<T> {

    final SingleSource<? extends T> source;

    public SingleToObservable(SingleSource<? extends T> source) {
        this.source = source;
    }

    @Override
    public void subscribeActual(final Observer<? super T> s) {
        source.subscribe(create(s));
    }

    /**
     * Creates a {@link SingleObserver} wrapper around a {@link Observer}.
     * @param <T> the value type
     * @param downstream the downstream {@code Observer} to talk to
     * @return the new SingleObserver instance
     * @since 2.1.11 - experimental
     */
    @Experimental
    public static <T> SingleObserver<T> create(Observer<? super T> downstream) {
        return new SingleToObservableObserver<T>(downstream);
    }

    static final class SingleToObservableObserver<T>
    implements SingleObserver<T>, Disposable {

        final Observer<? super T> actual;

        Disposable d;

        SingleToObservableObserver(Observer<? super T> actual) {
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
        public void onSuccess(T value) {
            actual.onNext(value);
            actual.onComplete();
        }

        @Override
        public void onError(Throwable e) {
            actual.onError(e);
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
