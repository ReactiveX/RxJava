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
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.*;
import io.reactivex.functions.Function;

public final class SingleOnErrorReturn<T> extends Single<T> {
    final SingleSource<? extends T> source;

    final Function<? super Throwable, ? extends T> valueSupplier;

    final T value;

    public SingleOnErrorReturn(SingleSource<? extends T> source,
            Function<? super Throwable, ? extends T> valueSupplier, T value) {
        this.source = source;
        this.valueSupplier = valueSupplier;
        this.value = value;
    }



    @Override
    protected void subscribeActual(final SingleObserver<? super T> s) {

        source.subscribe(new OnErrorReturn(s));
    }

    final class OnErrorReturn implements SingleObserver<T> {

        private final SingleObserver<? super T> observer;

        OnErrorReturn(SingleObserver<? super T> observer) {
            this.observer = observer;
        }

        @Override
        public void onError(Throwable e) {
            T v;

            if (valueSupplier != null) {
                try {
                    v = valueSupplier.apply(e);
                } catch (Throwable ex) {
                    Exceptions.throwIfFatal(ex);
                    observer.onError(new CompositeException(e, ex));
                    return;
                }
            } else {
                v = value;
            }

            if (v == null) {
                NullPointerException npe = new NullPointerException("Value supplied was null");
                npe.initCause(e);
                observer.onError(npe);
                return;
            }

            observer.onSuccess(v);
        }

        @Override
        public void onSubscribe(Disposable d) {
            observer.onSubscribe(d);
        }

        @Override
        public void onSuccess(T value) {
            observer.onSuccess(value);
        }

    }
}
