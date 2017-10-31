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

import java.util.concurrent.atomic.AtomicInteger;

import io.reactivex.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.disposables.SequentialDisposable;

public final class CompletableConcatArray extends Completable {
    final CompletableSource[] sources;

    public CompletableConcatArray(CompletableSource[] sources) {
        this.sources = sources;
    }

    @Override
    public void subscribeActual(CompletableObserver s) {
        ConcatInnerObserver inner = new ConcatInnerObserver(s, sources);
        s.onSubscribe(inner.sd);
        inner.next();
    }

    static final class ConcatInnerObserver extends AtomicInteger implements CompletableObserver {

        private static final long serialVersionUID = -7965400327305809232L;

        final CompletableObserver actual;
        final CompletableSource[] sources;

        int index;

        final SequentialDisposable sd;

        ConcatInnerObserver(CompletableObserver actual, CompletableSource[] sources) {
            this.actual = actual;
            this.sources = sources;
            this.sd = new SequentialDisposable();
        }

        @Override
        public void onSubscribe(Disposable d) {
            sd.replace(d);
        }

        @Override
        public void onError(Throwable e) {
            actual.onError(e);
        }

        @Override
        public void onComplete() {
            next();
        }

        void next() {
            if (sd.isDisposed()) {
                return;
            }

            if (getAndIncrement() != 0) {
                return;
            }

            CompletableSource[] a = sources;
            do {
                if (sd.isDisposed()) {
                    return;
                }

                int idx = index++;
                if (idx == a.length) {
                    actual.onComplete();
                    return;
                }

                a[idx].subscribe(this);
            } while (decrementAndGet() != 0);
        }
    }
}
