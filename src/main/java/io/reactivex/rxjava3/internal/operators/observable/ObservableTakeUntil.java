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

import java.util.concurrent.atomic.*;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.internal.disposables.DisposableHelper;
import io.reactivex.rxjava3.internal.util.*;

public final class ObservableTakeUntil<T, U> extends AbstractObservableWithUpstream<T, T> {

    final ObservableSource<? extends U> other;

    public ObservableTakeUntil(ObservableSource<T> source, ObservableSource<? extends U> other) {
        super(source);
        this.other = other;
    }

    @Override
    public void subscribeActual(Observer<? super T> child) {
        TakeUntilMainObserver<T, U> parent = new TakeUntilMainObserver<T, U>(child);
        child.onSubscribe(parent);

        other.subscribe(parent.otherObserver);
        source.subscribe(parent);
    }

    static final class TakeUntilMainObserver<T, U> extends AtomicInteger
    implements Observer<T>, Disposable {

        private static final long serialVersionUID = 1418547743690811973L;

        final Observer<? super T> downstream;

        final AtomicReference<Disposable> upstream;

        final OtherObserver otherObserver;

        final AtomicThrowable error;

        TakeUntilMainObserver(Observer<? super T> downstream) {
            this.downstream = downstream;
            this.upstream = new AtomicReference<Disposable>();
            this.otherObserver = new OtherObserver();
            this.error = new AtomicThrowable();
        }

        @Override
        public void dispose() {
            DisposableHelper.dispose(upstream);
            DisposableHelper.dispose(otherObserver);
        }

        @Override
        public boolean isDisposed() {
            return DisposableHelper.isDisposed(upstream.get());
        }

        @Override
        public void onSubscribe(Disposable d) {
            DisposableHelper.setOnce(upstream, d);
        }

        @Override
        public void onNext(T t) {
            HalfSerializer.onNext(downstream, t, this, error);
        }

        @Override
        public void onError(Throwable e) {
            DisposableHelper.dispose(otherObserver);
            HalfSerializer.onError(downstream, e, this, error);
        }

        @Override
        public void onComplete() {
            DisposableHelper.dispose(otherObserver);
            HalfSerializer.onComplete(downstream, this, error);
        }

        void otherError(Throwable e) {
            DisposableHelper.dispose(upstream);
            HalfSerializer.onError(downstream, e, this, error);
        }

        void otherComplete() {
            DisposableHelper.dispose(upstream);
            HalfSerializer.onComplete(downstream, this, error);
        }

        final class OtherObserver extends AtomicReference<Disposable>
        implements Observer<U> {

            private static final long serialVersionUID = -8693423678067375039L;

            @Override
            public void onSubscribe(Disposable d) {
                DisposableHelper.setOnce(this, d);
            }

            @Override
            public void onNext(U t) {
                DisposableHelper.dispose(this);
                otherComplete();
            }

            @Override
            public void onError(Throwable e) {
                otherError(e);
            }

            @Override
            public void onComplete() {
                otherComplete();
            }

        }
    }

}
