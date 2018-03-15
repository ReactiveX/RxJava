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

package io.reactivex.internal.operators.completable;

import io.reactivex.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.disposables.DisposableHelper;
import io.reactivex.internal.observers.BasicQueueDisposable;

/**
 * Wraps a Completable and exposes it as an Observable.
 *
 * @param <T> the value type
 */
public final class CompletableToObservable<T> extends Observable<T> {

    final CompletableSource source;

    public CompletableToObservable(CompletableSource source) {
        this.source = source;
    }

    @Override
    protected void subscribeActual(Observer<? super T> observer) {
        source.subscribe(new ObserverCompletableObserver(observer));
    }

    static final class ObserverCompletableObserver extends BasicQueueDisposable<Void>
    implements CompletableObserver {

        final Observer<?> observer;

        Disposable upstream;

        ObserverCompletableObserver(Observer<?> observer) {
            this.observer = observer;
        }

        @Override
        public void onComplete() {
            observer.onComplete();
        }

        @Override
        public void onError(Throwable e) {
            observer.onError(e);
        }

        @Override
        public void onSubscribe(Disposable d) {
            if (DisposableHelper.validate(upstream, d)) {
                this.upstream = d;
                observer.onSubscribe(this);
            }
        }

        @Override
        public int requestFusion(int mode) {
            return mode & ASYNC;
        }

        @Override
        public Void poll() throws Exception {
            return null; // always empty
        }

        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        public void clear() {
            // always empty
        }

        @Override
        public void dispose() {
            upstream.dispose();
        }

        @Override
        public boolean isDisposed() {
            return upstream.isDisposed();
        }
    }
}
