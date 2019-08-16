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

package io.reactivex.rxjava3.internal.operators.mixed;

import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.internal.disposables.DisposableHelper;

/**
 * After Completable completes, it relays the signals
 * of the ObservableSource to the downstream observer.
 * 
 * @param <R> the result type of the ObservableSource and this operator
 * @since 2.1.15
 */
public final class CompletableAndThenObservable<R> extends Observable<R> {

    final CompletableSource source;

    final ObservableSource<? extends R> other;

    public CompletableAndThenObservable(CompletableSource source,
            ObservableSource<? extends R> other) {
        this.source = source;
        this.other = other;
    }

    @Override
    protected void subscribeActual(Observer<? super R> observer) {
        AndThenObservableObserver<R> parent = new AndThenObservableObserver<R>(observer, other);
        observer.onSubscribe(parent);
        source.subscribe(parent);
    }

    static final class AndThenObservableObserver<R>
    extends AtomicReference<Disposable>
    implements Observer<R>, CompletableObserver, Disposable {

        private static final long serialVersionUID = -8948264376121066672L;

        final Observer<? super R> downstream;

        ObservableSource<? extends R> other;

        AndThenObservableObserver(Observer<? super R> downstream, ObservableSource<? extends R> other) {
            this.other = other;
            this.downstream = downstream;
        }

        @Override
        public void onNext(R t) {
            downstream.onNext(t);
        }

        @Override
        public void onError(Throwable t) {
            downstream.onError(t);
        }

        @Override
        public void onComplete() {
            ObservableSource<? extends R> o = other;
            if (o == null) {
                downstream.onComplete();
            } else {
                other = null;
                o.subscribe(this);
            }
        }

        @Override
        public void dispose() {
            DisposableHelper.dispose(this);
        }

        @Override
        public boolean isDisposed() {
            return DisposableHelper.isDisposed(get());
        }

        @Override
        public void onSubscribe(Disposable d) {
            DisposableHelper.replace(this, d);
        }

    }
}
