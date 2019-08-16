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

package io.reactivex.rxjava3.internal.operators.single;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.exceptions.Exceptions;
import io.reactivex.rxjava3.functions.BiPredicate;

public final class SingleContains<T> extends Single<Boolean> {

    final SingleSource<T> source;

    final Object value;

    final BiPredicate<Object, Object> comparer;

    public SingleContains(SingleSource<T> source, Object value, BiPredicate<Object, Object> comparer) {
        this.source = source;
        this.value = value;
        this.comparer = comparer;
    }

    @Override
    protected void subscribeActual(final SingleObserver<? super Boolean> observer) {

        source.subscribe(new ContainsSingleObserver(observer));
    }

    final class ContainsSingleObserver implements SingleObserver<T> {

        private final SingleObserver<? super Boolean> downstream;

        ContainsSingleObserver(SingleObserver<? super Boolean> observer) {
            this.downstream = observer;
        }

        @Override
        public void onSubscribe(Disposable d) {
            downstream.onSubscribe(d);
        }

        @Override
        public void onSuccess(T v) {
            boolean b;

            try {
                b = comparer.test(v, value);
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                downstream.onError(ex);
                return;
            }
            downstream.onSuccess(b);
        }

        @Override
        public void onError(Throwable e) {
            downstream.onError(e);
        }

    }
}
