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

package io.reactivex.rxjava3.internal.operators.observable;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.internal.disposables.DisposableHelper;
import io.reactivex.rxjava3.internal.util.EmptyComponent;

/**
 * Breaks the links between the upstream and the downstream (the Disposable and
 * the Observer references) when the sequence terminates or gets disposed.
 *
 * @param <T> the value type
 */
public final class ObservableDetach<T> extends AbstractObservableWithUpstream<T, T> {

    public ObservableDetach(ObservableSource<T> source) {
        super(source);
    }

    @Override
    protected void subscribeActual(Observer<? super T> observer) {
        source.subscribe(new DetachObserver<T>(observer));
    }

    static final class DetachObserver<T> implements Observer<T>, Disposable {

        Observer<? super T> downstream;

        Disposable upstream;

        DetachObserver(Observer<? super T> downstream) {
            this.downstream = downstream;
        }

        @Override
        public void dispose() {
            Disposable d = this.upstream;
            this.upstream = EmptyComponent.INSTANCE;
            this.downstream = EmptyComponent.asObserver();
            d.dispose();
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
        public void onNext(T t) {
            downstream.onNext(t);
        }

        @Override
        public void onError(Throwable t) {
            Observer<? super T> a = downstream;
            this.upstream = EmptyComponent.INSTANCE;
            this.downstream = EmptyComponent.asObserver();
            a.onError(t);
        }

        @Override
        public void onComplete() {
            Observer<? super T> a = downstream;
            this.upstream = EmptyComponent.INSTANCE;
            this.downstream = EmptyComponent.asObserver();
            a.onComplete();
        }
    }
}

