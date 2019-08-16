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

package io.reactivex.rxjava3.internal.operators.maybe;

import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.internal.disposables.DisposableHelper;

public final class MaybeDelayWithCompletable<T> extends Maybe<T> {

    final MaybeSource<T> source;

    final CompletableSource other;

    public MaybeDelayWithCompletable(MaybeSource<T> source, CompletableSource other) {
        this.source = source;
        this.other = other;
    }

    @Override
    protected void subscribeActual(MaybeObserver<? super T> observer) {
        other.subscribe(new OtherObserver<T>(observer, source));
    }

    static final class OtherObserver<T>
    extends AtomicReference<Disposable>
    implements CompletableObserver, Disposable {
        private static final long serialVersionUID = 703409937383992161L;

        final MaybeObserver<? super T> downstream;

        final MaybeSource<T> source;

        OtherObserver(MaybeObserver<? super T> actual, MaybeSource<T> source) {
            this.downstream = actual;
            this.source = source;
        }

        @Override
        public void onSubscribe(Disposable d) {
            if (DisposableHelper.setOnce(this, d)) {

                downstream.onSubscribe(this);
            }
        }

        @Override
        public void onError(Throwable e) {
            downstream.onError(e);
        }

        @Override
        public void onComplete() {
            source.subscribe(new DelayWithMainObserver<T>(this, downstream));
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

    static final class DelayWithMainObserver<T> implements MaybeObserver<T> {

        final AtomicReference<Disposable> parent;

        final MaybeObserver<? super T> downstream;

        DelayWithMainObserver(AtomicReference<Disposable> parent, MaybeObserver<? super T> downstream) {
            this.parent = parent;
            this.downstream = downstream;
        }

        @Override
        public void onSubscribe(Disposable d) {
            DisposableHelper.replace(parent, d);
        }

        @Override
        public void onSuccess(T value) {
            downstream.onSuccess(value);
        }

        @Override
        public void onError(Throwable e) {
            downstream.onError(e);
        }

        @Override
        public void onComplete() {
            downstream.onComplete();
        }
    }
}
