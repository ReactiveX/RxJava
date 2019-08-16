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
import io.reactivex.rxjava3.internal.fuseable.FuseToMaybe;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;

/**
 * Turns an onSuccess into an onComplete, onError and onComplete is relayed as is.
 *
 * @param <T> the value type
 */
public final class MaybeIgnoreElementCompletable<T> extends Completable implements FuseToMaybe<T> {

    final MaybeSource<T> source;

    public MaybeIgnoreElementCompletable(MaybeSource<T> source) {
        this.source = source;
    }

    @Override
    protected void subscribeActual(CompletableObserver observer) {
        source.subscribe(new IgnoreMaybeObserver<T>(observer));
    }

    @Override
    public Maybe<T> fuseToMaybe() {
        return RxJavaPlugins.onAssembly(new MaybeIgnoreElement<T>(source));
    }

    static final class IgnoreMaybeObserver<T> implements MaybeObserver<T>, Disposable {

        final CompletableObserver downstream;

        Disposable upstream;

        IgnoreMaybeObserver(CompletableObserver downstream) {
            this.downstream = downstream;
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
            downstream.onComplete();
        }

        @Override
        public void onError(Throwable e) {
            upstream = DisposableHelper.DISPOSED;
            downstream.onError(e);
        }

        @Override
        public void onComplete() {
            upstream = DisposableHelper.DISPOSED;
            downstream.onComplete();
        }

        @Override
        public boolean isDisposed() {
            return upstream.isDisposed();
        }

        @Override
        public void dispose() {
            upstream.dispose();
            upstream = DisposableHelper.DISPOSED;
        }

    }

}
