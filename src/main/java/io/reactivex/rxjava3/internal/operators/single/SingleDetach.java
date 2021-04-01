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

package io.reactivex.rxjava3.internal.operators.single;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.internal.disposables.DisposableHelper;

/**
 * Breaks the references between the upstream and downstream when the Maybe terminates.
 * <p>History: 2.1.5 - experimental
 * @param <T> the value type
 * @since 2.2
 */
public final class SingleDetach<T> extends Single<T> {

    final SingleSource<T> source;

    public SingleDetach(SingleSource<T> source) {
        this.source = source;
    }

    @Override
    protected void subscribeActual(SingleObserver<? super T> observer) {
        source.subscribe(new DetachSingleObserver<>(observer));
    }

    static final class DetachSingleObserver<T> implements SingleObserver<T>, Disposable {

        SingleObserver<? super T> downstream;

        Disposable upstream;

        DetachSingleObserver(SingleObserver<? super T> downstream) {
            this.downstream = downstream;
        }

        @Override
        public void dispose() {
            downstream = null;
            upstream.dispose();
            upstream = DisposableHelper.DISPOSED;
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
        public void onSuccess(T value) {
            upstream = DisposableHelper.DISPOSED;
            SingleObserver<? super T> a = downstream;
            if (a != null) {
                downstream = null;
                a.onSuccess(value);
            }
        }

        @Override
        public void onError(Throwable e) {
            upstream = DisposableHelper.DISPOSED;
            SingleObserver<? super T> a = downstream;
            if (a != null) {
                downstream = null;
                a.onError(e);
            }
        }
    }
}
