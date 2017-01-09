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
import io.reactivex.internal.fuseable.HasUpstreamMaybeSource;
import io.reactivex.internal.observers.DeferredScalarDisposable;

/**
 * Wraps a MaybeSource and exposes it as an Observable, relaying signals in a backpressure-aware manner
 * and composes cancellation through.
 *
 * @param <T> the value type
 */
public final class MaybeToObservable<T> extends Observable<T> implements HasUpstreamMaybeSource<T> {

    final MaybeSource<T> source;

    public MaybeToObservable(MaybeSource<T> source) {
        this.source = source;
    }

    @Override
    public MaybeSource<T> source() {
        return source;
    }

    @Override
    protected void subscribeActual(Observer<? super T> s) {
        source.subscribe(new MaybeToFlowableSubscriber<T>(s));
    }

    static final class MaybeToFlowableSubscriber<T> extends DeferredScalarDisposable<T>
    implements MaybeObserver<T> {

        private static final long serialVersionUID = 7603343402964826922L;

        Disposable d;

        MaybeToFlowableSubscriber(Observer<? super T> actual) {
            super(actual);
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
            complete(value);
        }

        @Override
        public void onError(Throwable e) {
            error(e);
        }

        @Override
        public void onComplete() {
            complete();
        }

        @Override
        public void dispose() {
            super.dispose();
            d.dispose();
        }
    }
}
