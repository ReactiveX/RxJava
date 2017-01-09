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

package io.reactivex.internal.operators.maybe;

import io.reactivex.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.disposables.DisposableHelper;
import io.reactivex.internal.functions.ObjectHelper;
import io.reactivex.internal.fuseable.HasUpstreamMaybeSource;

/**
 * Signals true if the source signals a value that is object-equals with the provided
 * value, false otherwise or for empty sources.
 *
 * @param <T> the value type
 */
public final class MaybeContains<T> extends Single<Boolean> implements HasUpstreamMaybeSource<T> {

    final MaybeSource<T> source;

    final Object value;

    public MaybeContains(MaybeSource<T> source, Object value) {
        this.source = source;
        this.value = value;
    }

    @Override
    public MaybeSource<T> source() {
        return source;
    }

    @Override
    protected void subscribeActual(SingleObserver<? super Boolean> observer) {
        source.subscribe(new ContainsMaybeObserver(observer, value));
    }

    static final class ContainsMaybeObserver implements MaybeObserver<Object>, Disposable {

        final SingleObserver<? super Boolean> actual;

        final Object value;

        Disposable d;

        ContainsMaybeObserver(SingleObserver<? super Boolean> actual, Object value) {
            this.actual = actual;
            this.value = value;
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
        public void onSubscribe(Disposable d) {
            if (DisposableHelper.validate(this.d, d)) {
                this.d = d;
                actual.onSubscribe(this);
            }
        }

        @Override
        public void onSuccess(Object value) {
            d = DisposableHelper.DISPOSED;
            actual.onSuccess(ObjectHelper.equals(value, this.value));
        }

        @Override
        public void onError(Throwable e) {
            d = DisposableHelper.DISPOSED;
            actual.onError(e);
        }

        @Override
        public void onComplete() {
            d = DisposableHelper.DISPOSED;
            actual.onSuccess(false);
        }
    }
}
