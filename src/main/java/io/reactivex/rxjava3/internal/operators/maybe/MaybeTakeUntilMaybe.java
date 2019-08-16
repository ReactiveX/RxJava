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
import io.reactivex.rxjava3.plugins.RxJavaPlugins;

/**
 * Relays the main source's event unless the other Maybe signals an item first or just completes
 * at which point the resulting Maybe is completed.
 *
 * @param <T> the value type
 * @param <U> the other's value type
 */
public final class MaybeTakeUntilMaybe<T, U> extends AbstractMaybeWithUpstream<T, T> {

    final MaybeSource<U> other;

    public MaybeTakeUntilMaybe(MaybeSource<T> source, MaybeSource<U> other) {
        super(source);
        this.other = other;
    }

    @Override
    protected void subscribeActual(MaybeObserver<? super T> observer) {
        TakeUntilMainMaybeObserver<T, U> parent = new TakeUntilMainMaybeObserver<T, U>(observer);
        observer.onSubscribe(parent);

        other.subscribe(parent.other);

        source.subscribe(parent);
    }

    static final class TakeUntilMainMaybeObserver<T, U>
    extends AtomicReference<Disposable> implements MaybeObserver<T>, Disposable {

        private static final long serialVersionUID = -2187421758664251153L;

        final MaybeObserver<? super T> downstream;

        final TakeUntilOtherMaybeObserver<U> other;

        TakeUntilMainMaybeObserver(MaybeObserver<? super T> downstream) {
            this.downstream = downstream;
            this.other = new TakeUntilOtherMaybeObserver<U>(this);
        }

        @Override
        public void dispose() {
            DisposableHelper.dispose(this);
            DisposableHelper.dispose(other);
        }

        @Override
        public boolean isDisposed() {
            return DisposableHelper.isDisposed(get());
        }

        @Override
        public void onSubscribe(Disposable d) {
            DisposableHelper.setOnce(this, d);
        }

        @Override
        public void onSuccess(T value) {
            DisposableHelper.dispose(other);
            if (getAndSet(DisposableHelper.DISPOSED) != DisposableHelper.DISPOSED) {
                downstream.onSuccess(value);
            }
        }

        @Override
        public void onError(Throwable e) {
            DisposableHelper.dispose(other);
            if (getAndSet(DisposableHelper.DISPOSED) != DisposableHelper.DISPOSED) {
                downstream.onError(e);
            } else {
                RxJavaPlugins.onError(e);
            }
        }

        @Override
        public void onComplete() {
            DisposableHelper.dispose(other);
            if (getAndSet(DisposableHelper.DISPOSED) != DisposableHelper.DISPOSED) {
                downstream.onComplete();
            }
        }

        void otherError(Throwable e) {
            if (DisposableHelper.dispose(this)) {
                downstream.onError(e);
            } else {
                RxJavaPlugins.onError(e);
            }
        }

        void otherComplete() {
            if (DisposableHelper.dispose(this)) {
                downstream.onComplete();
            }
        }

        static final class TakeUntilOtherMaybeObserver<U>
        extends AtomicReference<Disposable> implements MaybeObserver<U> {

            private static final long serialVersionUID = -1266041316834525931L;

            final TakeUntilMainMaybeObserver<?, U> parent;

            TakeUntilOtherMaybeObserver(TakeUntilMainMaybeObserver<?, U> parent) {
                this.parent = parent;
            }

            @Override
            public void onSubscribe(Disposable d) {
                DisposableHelper.setOnce(this, d);
            }

            @Override
            public void onSuccess(Object value) {
                parent.otherComplete();
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
