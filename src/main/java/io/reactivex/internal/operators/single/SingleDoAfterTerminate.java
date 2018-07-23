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

package io.reactivex.internal.operators.single;

import io.reactivex.Single;
import io.reactivex.SingleObserver;
import io.reactivex.SingleSource;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.functions.Action;
import io.reactivex.internal.disposables.DisposableHelper;
import io.reactivex.plugins.RxJavaPlugins;

/**
 * Calls an action after pushing the current item or an error to the downstream.
 * <p>History: 2.0.6 - experimental
 * @param <T> the value type
 * @since 2.1
 */
public final class SingleDoAfterTerminate<T> extends Single<T> {

    final SingleSource<T> source;

    final Action onAfterTerminate;

    public SingleDoAfterTerminate(SingleSource<T> source, Action onAfterTerminate) {
        this.source = source;
        this.onAfterTerminate = onAfterTerminate;
    }

    @Override
    protected void subscribeActual(SingleObserver<? super T> s) {
        source.subscribe(new DoAfterTerminateObserver<T>(s, onAfterTerminate));
    }

    static final class DoAfterTerminateObserver<T> implements SingleObserver<T>, Disposable {

        final SingleObserver<? super T> actual;

        final Action onAfterTerminate;

        Disposable d;

        DoAfterTerminateObserver(SingleObserver<? super T> actual, Action onAfterTerminate) {
            this.actual = actual;
            this.onAfterTerminate = onAfterTerminate;
        }

        @Override
        public void onSubscribe(Disposable d) {
            if (DisposableHelper.validate(this.d, d)) {
                this.d = d;

                actual.onSubscribe(this);
            }
        }

        @Override
        public void onSuccess(T t) {
            actual.onSuccess(t);

            onAfterTerminate();
        }

        @Override
        public void onError(Throwable e) {
            actual.onError(e);

            onAfterTerminate();
        }

        @Override
        public void dispose() {
            d.dispose();
        }

        @Override
        public boolean isDisposed() {
            return d.isDisposed();
        }

        private void onAfterTerminate() {
            try {
                onAfterTerminate.run();
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                RxJavaPlugins.onError(ex);
            }
        }
    }
}
