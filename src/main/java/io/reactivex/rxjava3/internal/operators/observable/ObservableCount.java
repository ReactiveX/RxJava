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

public final class ObservableCount<T> extends AbstractObservableWithUpstream<T, Long> {
    public ObservableCount(ObservableSource<T> source) {
        super(source);
    }

    @Override
    public void subscribeActual(Observer<? super Long> t) {
        source.subscribe(new CountObserver(t));
    }

    static final class CountObserver implements Observer<Object>, Disposable {
        final Observer<? super Long> downstream;

        Disposable upstream;

        long count;

        CountObserver(Observer<? super Long> downstream) {
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
        public void dispose() {
            upstream.dispose();
        }

        @Override
        public boolean isDisposed() {
            return upstream.isDisposed();
        }

        @Override
        public void onNext(Object t) {
            count++;
        }

        @Override
        public void onError(Throwable t) {
            downstream.onError(t);
        }

        @Override
        public void onComplete() {
            downstream.onNext(count);
            downstream.onComplete();
        }
    }
}
