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

package io.reactivex.internal.operators.observable;

import java.util.concurrent.atomic.*;

import io.reactivex.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.disposables.DisposableHelper;
import io.reactivex.internal.util.*;

/**
 * Merges an Observable and a Completable by emitting the items of the Observable and waiting until
 * both the Observable and Completable complete normally.
 *
 * @param <T> the element type of the Observable
 * @since 2.1.10 - experimental
 */
public final class ObservableMergeWithCompletable<T> extends AbstractObservableWithUpstream<T, T> {

    final CompletableSource other;

    public ObservableMergeWithCompletable(Observable<T> source, CompletableSource other) {
        super(source);
        this.other = other;
    }

    @Override
    protected void subscribeActual(Observer<? super T> observer) {
        MergeWithObserver<T> parent = new MergeWithObserver<T>(observer);
        observer.onSubscribe(parent);
        source.subscribe(parent);
        other.subscribe(parent.otherObserver);
    }

    static final class MergeWithObserver<T> extends AtomicInteger
    implements Observer<T>, Disposable {

        private static final long serialVersionUID = -4592979584110982903L;

        final Observer<? super T> actual;

        final AtomicReference<Disposable> mainDisposable;

        final OtherObserver otherObserver;

        final AtomicThrowable error;

        volatile boolean mainDone;

        volatile boolean otherDone;

        MergeWithObserver(Observer<? super T> actual) {
            this.actual = actual;
            this.mainDisposable = new AtomicReference<Disposable>();
            this.otherObserver = new OtherObserver(this);
            this.error = new AtomicThrowable();
        }

        @Override
        public void onSubscribe(Disposable d) {
            DisposableHelper.setOnce(mainDisposable, d);
        }

        @Override
        public void onNext(T t) {
            HalfSerializer.onNext(actual, t, this, error);
        }

        @Override
        public void onError(Throwable ex) {
            DisposableHelper.dispose(mainDisposable);
            HalfSerializer.onError(actual, ex, this, error);
        }

        @Override
        public void onComplete() {
            mainDone = true;
            if (otherDone) {
                HalfSerializer.onComplete(actual, this, error);
            }
        }

        @Override
        public boolean isDisposed() {
            return DisposableHelper.isDisposed(mainDisposable.get());
        }

        @Override
        public void dispose() {
            DisposableHelper.dispose(mainDisposable);
            DisposableHelper.dispose(otherObserver);
        }

        void otherError(Throwable ex) {
            DisposableHelper.dispose(mainDisposable);
            HalfSerializer.onError(actual, ex, this, error);
        }

        void otherComplete() {
            otherDone = true;
            if (mainDone) {
                HalfSerializer.onComplete(actual, this, error);
            }
        }

        static final class OtherObserver extends AtomicReference<Disposable>
        implements CompletableObserver {

            private static final long serialVersionUID = -2935427570954647017L;

            final MergeWithObserver<?> parent;

            OtherObserver(MergeWithObserver<?> parent) {
                this.parent = parent;
            }

            @Override
            public void onSubscribe(Disposable d) {
                DisposableHelper.setOnce(this, d);
            }

            @Override
            public void onError(Throwable e) {
                parent.otherError(e);
            }

            @Override
            public void onComplete() {
                parent.otherComplete();
            }
        }
    }
}
