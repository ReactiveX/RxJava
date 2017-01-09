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
import io.reactivex.internal.fuseable.FuseToMaybe;
import io.reactivex.plugins.RxJavaPlugins;

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

        final CompletableObserver actual;

        Disposable d;

        IgnoreMaybeObserver(CompletableObserver actual) {
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
            d = DisposableHelper.DISPOSED;
            actual.onComplete();
        }

        @Override
        public void onError(Throwable e) {
            d = DisposableHelper.DISPOSED;
            actual.onError(e);
        }

        @Override
        public void onComplete() {
            d = DisposableHelper.DISPOSED;
            actual.onComplete();
        }

        @Override
        public boolean isDisposed() {
            return d.isDisposed();
        }

        @Override
        public void dispose() {
            d.dispose();
            d = DisposableHelper.DISPOSED;
        }

    }

}
