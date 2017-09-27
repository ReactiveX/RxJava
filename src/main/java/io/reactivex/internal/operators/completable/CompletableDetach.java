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
import io.reactivex.annotations.Experimental;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.disposables.DisposableHelper;

/**
 * Breaks the references between the upstream and downstream when the Completable terminates.
 * 
 * @since 2.1.5 - experimental
 */
@Experimental
public final class CompletableDetach extends Completable {

    final CompletableSource source;

    public CompletableDetach(CompletableSource source) {
        this.source = source;
    }

    @Override
    protected void subscribeActual(CompletableObserver observer) {
        source.subscribe(new DetachCompletableObserver(observer));
    }

    static final class DetachCompletableObserver implements CompletableObserver, Disposable {

        CompletableObserver actual;

        Disposable d;

        DetachCompletableObserver(CompletableObserver actual) {
            this.actual = actual;
        }

        @Override
        public void dispose() {
            actual = null;
            d.dispose();
            d = DisposableHelper.DISPOSED;
        }

        @Override
        public boolean isDisposed() {
            return d.isDisposed();
        }

        @Override
        public void onSubscribe(Disposable d) {
            if (DisposableHelper.validate(this.d, d)) {
                this.d = d;

                actual.onSubscribe(this);
            }
        }

        @Override
        public void onError(Throwable e) {
            d = DisposableHelper.DISPOSED;
            CompletableObserver a = actual;
            if (a != null) {
                actual = null;
                a.onError(e);
            }
        }

        @Override
        public void onComplete() {
            d = DisposableHelper.DISPOSED;
            CompletableObserver a = actual;
            if (a != null) {
                actual = null;
                a.onComplete();
            }
        }
    }
}
