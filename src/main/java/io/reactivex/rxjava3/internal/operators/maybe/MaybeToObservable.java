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
import io.reactivex.rxjava3.internal.fuseable.HasUpstreamMaybeSource;
import io.reactivex.rxjava3.internal.observers.DeferredScalarDisposable;

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
    protected void subscribeActual(Observer<? super T> observer) {
        source.subscribe(create(observer));
    }

    /**
     * Creates a {@link MaybeObserver} wrapper around a {@link Observer}.
     * <p>History: 2.1.11 - experimental
     * @param <T> the value type
     * @param downstream the downstream {@code Observer} to talk to
     * @return the new MaybeObserver instance
     * @since 2.2
     */
    public static <T> MaybeObserver<T> create(Observer<? super T> downstream) {
        return new MaybeToObservableObserver<T>(downstream);
    }

    static final class MaybeToObservableObserver<T> extends DeferredScalarDisposable<T>
    implements MaybeObserver<T> {

        private static final long serialVersionUID = 7603343402964826922L;

        Disposable upstream;

        MaybeToObservableObserver(Observer<? super T> downstream) {
            super(downstream);
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
            upstream.dispose();
        }
    }
}
