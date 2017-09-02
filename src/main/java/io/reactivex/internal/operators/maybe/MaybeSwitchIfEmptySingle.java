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

package io.reactivex.internal.operators.maybe;

import io.reactivex.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.disposables.DisposableHelper;
import io.reactivex.internal.fuseable.HasUpstreamMaybeSource;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Subscribes to the other source if the main source is empty.
 *
 * @param <T> the value type
 */
public final class MaybeSwitchIfEmptySingle<T> extends Single<T> implements HasUpstreamMaybeSource<T> {

    final MaybeSource<T> source;
    final SingleSource<? extends T> other;

    public MaybeSwitchIfEmptySingle(MaybeSource<T> source, SingleSource<? extends T> other) {
        this.source = source;
        this.other = other;
    }

    @Override
    public MaybeSource<T> source() {
        return source;
    }

    @Override
    protected void subscribeActual(SingleObserver<? super T> observer) {
        source.subscribe(new SwitchIfEmptyMaybeObserver<T>(observer, other));
    }

    static final class SwitchIfEmptyMaybeObserver<T>
    extends AtomicReference<Disposable>
    implements MaybeObserver<T>, Disposable {

        private static final long serialVersionUID = 4603919676453758899L;

        final SingleObserver<? super T> actual;

        final SingleSource<? extends T> other;

        SwitchIfEmptyMaybeObserver(SingleObserver<? super T> actual, SingleSource<? extends T> other) {
            this.actual = actual;
            this.other = other;
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
            if (DisposableHelper.setOnce(this, d)) {
                actual.onSubscribe(this);
            }
        }

        @Override
        public void onSuccess(T value) {
            actual.onSuccess(value);
        }

        @Override
        public void onError(Throwable e) {
            actual.onError(e);
        }

        @Override
        public void onComplete() {
            Disposable d = get();
            if (d != DisposableHelper.DISPOSED) {
                if (compareAndSet(d, null)) {
                    other.subscribe(new OtherSingleObserver<T>(actual, this));
                }
            }
        }

        static final class OtherSingleObserver<T> implements SingleObserver<T> {

            final SingleObserver<? super T> actual;

            final AtomicReference<Disposable> parent;
            OtherSingleObserver(SingleObserver<? super T> actual, AtomicReference<Disposable> parent) {
                this.actual = actual;
                this.parent = parent;
            }
            @Override
            public void onSubscribe(Disposable d) {
                DisposableHelper.setOnce(parent, d);
            }
            @Override
            public void onSuccess(T value) {
                actual.onSuccess(value);
            }
            @Override
            public void onError(Throwable e) {
                actual.onError(e);
            }
        }

    }
}