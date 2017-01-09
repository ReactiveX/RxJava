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
import io.reactivex.exceptions.*;
import io.reactivex.functions.Function;
import io.reactivex.internal.disposables.DisposableHelper;
import io.reactivex.internal.functions.ObjectHelper;

/**
 * Returns a value generated via a function if the main source signals an onError.
 * @param <T> the value type
 */
public final class MaybeOnErrorReturn<T> extends AbstractMaybeWithUpstream<T, T> {

    final Function<? super Throwable, ? extends T> valueSupplier;

    public MaybeOnErrorReturn(MaybeSource<T> source,
            Function<? super Throwable, ? extends T> valueSupplier) {
        super(source);
        this.valueSupplier = valueSupplier;
    }

    @Override
    protected void subscribeActual(MaybeObserver<? super T> observer) {
        source.subscribe(new OnErrorReturnMaybeObserver<T>(observer, valueSupplier));
    }

    static final class OnErrorReturnMaybeObserver<T> implements MaybeObserver<T>, Disposable {

        final MaybeObserver<? super T> actual;

        final Function<? super Throwable, ? extends T> valueSupplier;

        Disposable d;

        OnErrorReturnMaybeObserver(MaybeObserver<? super T> actual,
                Function<? super Throwable, ? extends T> valueSupplier) {
            this.actual = actual;
            this.valueSupplier = valueSupplier;
        }

        @Override
        public void dispose() {
            d.dispose();
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
        public void onSuccess(T value) {
            actual.onSuccess(value);
        }

        @Override
        public void onError(Throwable e) {
            T v;

            try {
                v = ObjectHelper.requireNonNull(valueSupplier.apply(e), "The valueSupplier returned a null value");
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                actual.onError(new CompositeException(e, ex));
                return;
            }

            actual.onSuccess(v);
        }

        @Override
        public void onComplete() {
            actual.onComplete();
        }
    }
}
