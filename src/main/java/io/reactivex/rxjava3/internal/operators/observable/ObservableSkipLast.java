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

import java.util.ArrayDeque;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.internal.disposables.DisposableHelper;

public final class ObservableSkipLast<T> extends AbstractObservableWithUpstream<T, T> {
    final int skip;

    public ObservableSkipLast(ObservableSource<T> source, int skip) {
        super(source);
        this.skip = skip;
    }

    @Override
    public void subscribeActual(Observer<? super T> observer) {
        source.subscribe(new SkipLastObserver<T>(observer, skip));
    }

    static final class SkipLastObserver<T> extends ArrayDeque<T> implements Observer<T>, Disposable {

        private static final long serialVersionUID = -3807491841935125653L;
        final Observer<? super T> downstream;
        final int skip;

        Disposable upstream;

        SkipLastObserver(Observer<? super T> actual, int skip) {
            super(skip);
            this.downstream = actual;
            this.skip = skip;
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
        public void onNext(T t) {
            if (skip == size()) {
                downstream.onNext(poll());
            }
            offer(t);
        }

        @Override
        public void onError(Throwable t) {
            downstream.onError(t);
        }

        @Override
        public void onComplete() {
            downstream.onComplete();
        }
    }
}
