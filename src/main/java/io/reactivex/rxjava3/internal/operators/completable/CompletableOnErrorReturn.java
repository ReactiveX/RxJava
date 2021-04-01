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

package io.reactivex.rxjava3.internal.operators.completable;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.exceptions.*;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.internal.disposables.DisposableHelper;

import java.util.Objects;

/**
 * Returns a value generated via a function if the main source signals an onError.
 * @param <T> the value type
 * @since 3.0.0
 */
public final class CompletableOnErrorReturn<T> extends Maybe<T> {

    final CompletableSource source;

    final Function<? super Throwable, ? extends T> valueSupplier;

    public CompletableOnErrorReturn(CompletableSource source,
            Function<? super Throwable, ? extends T> valueSupplier) {
        this.source = source;
        this.valueSupplier = valueSupplier;
    }

    @Override
    protected void subscribeActual(MaybeObserver<? super T> observer) {
        source.subscribe(new OnErrorReturnMaybeObserver<>(observer, valueSupplier));
    }

    static final class OnErrorReturnMaybeObserver<T> implements CompletableObserver, Disposable {

        final MaybeObserver<? super T> downstream;

        final Function<? super Throwable, ? extends T> itemSupplier;

        Disposable upstream;

        OnErrorReturnMaybeObserver(MaybeObserver<? super T> actual,
                Function<? super Throwable, ? extends T> itemSupplier) {
            this.downstream = actual;
            this.itemSupplier = itemSupplier;
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
        public void onSubscribe(Disposable d) {
            if (DisposableHelper.validate(this.upstream, d)) {
                this.upstream = d;

                downstream.onSubscribe(this);
            }
        }

        @Override
        public void onError(Throwable e) {
            T v;

            try {
                v = Objects.requireNonNull(itemSupplier.apply(e), "The itemSupplier returned a null value");
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                downstream.onError(new CompositeException(e, ex));
                return;
            }

            downstream.onSuccess(v);
        }

        @Override
        public void onComplete() {
            downstream.onComplete();
        }
    }
}
