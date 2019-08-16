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

package io.reactivex.rxjava3.internal.operators.completable;

import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.internal.disposables.DisposableHelper;

public final class CompletableAndThenCompletable extends Completable {

    final CompletableSource source;

    final CompletableSource next;

    public CompletableAndThenCompletable(CompletableSource source, CompletableSource next) {
        this.source = source;
        this.next = next;
    }

    @Override
    protected void subscribeActual(CompletableObserver observer) {
        source.subscribe(new SourceObserver(observer, next));
    }

    static final class SourceObserver
            extends AtomicReference<Disposable>
            implements CompletableObserver, Disposable {

        private static final long serialVersionUID = -4101678820158072998L;

        final CompletableObserver actualObserver;

        final CompletableSource next;

        SourceObserver(CompletableObserver actualObserver, CompletableSource next) {
            this.actualObserver = actualObserver;
            this.next = next;
        }

        @Override
        public void onSubscribe(Disposable d) {
            if (DisposableHelper.setOnce(this, d)) {
                actualObserver.onSubscribe(this);
            }
        }

        @Override
        public void onError(Throwable e) {
            actualObserver.onError(e);
        }

        @Override
        public void onComplete() {
            next.subscribe(new NextObserver(this, actualObserver));
        }

        @Override
        public void dispose() {
            DisposableHelper.dispose(this);
        }

        @Override
        public boolean isDisposed() {
            return DisposableHelper.isDisposed(get());
        }
    }

    static final class NextObserver implements CompletableObserver {

        final AtomicReference<Disposable> parent;

        final CompletableObserver downstream;

        NextObserver(AtomicReference<Disposable> parent, CompletableObserver downstream) {
            this.parent = parent;
            this.downstream = downstream;
        }

        @Override
        public void onSubscribe(Disposable d) {
            DisposableHelper.replace(parent, d);
        }

        @Override
        public void onComplete() {
            downstream.onComplete();
        }

        @Override
        public void onError(Throwable e) {
            downstream.onError(e);
        }
    }
}
