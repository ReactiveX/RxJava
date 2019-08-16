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

package io.reactivex.rxjava3.internal.operators.maybe;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.internal.disposables.DisposableHelper;
import io.reactivex.rxjava3.internal.fuseable.HasUpstreamSingleSource;

/**
 * Wrap a Single into a Maybe.
 *
 * @param <T> the value type
 */
public final class MaybeFromSingle<T> extends Maybe<T> implements HasUpstreamSingleSource<T> {

    final SingleSource<T> source;

    public MaybeFromSingle(SingleSource<T> source) {
        this.source = source;
    }

    @Override
    public SingleSource<T> source() {
        return source;
    }

    @Override
    protected void subscribeActual(MaybeObserver<? super T> observer) {
        source.subscribe(new FromSingleObserver<T>(observer));
    }

    static final class FromSingleObserver<T> implements SingleObserver<T>, Disposable {
        final MaybeObserver<? super T> downstream;

        Disposable upstream;

        FromSingleObserver(MaybeObserver<? super T> downstream) {
            this.downstream = downstream;
        }

        @Override
        public void dispose() {
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
            downstream.onSuccess(value);
        }

        @Override
        public void onError(Throwable e) {
            upstream = DisposableHelper.DISPOSED;
            downstream.onError(e);
        }
    }
}
