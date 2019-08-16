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

import java.util.NoSuchElementException;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.internal.disposables.DisposableHelper;
import io.reactivex.rxjava3.internal.fuseable.FuseToObservable;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;

public final class ObservableElementAtSingle<T> extends Single<T> implements FuseToObservable<T> {
    final ObservableSource<T> source;
    final long index;
    final T defaultValue;

    public ObservableElementAtSingle(ObservableSource<T> source, long index, T defaultValue) {
        this.source = source;
        this.index = index;
        this.defaultValue = defaultValue;
    }

    @Override
    public void subscribeActual(SingleObserver<? super T> t) {
        source.subscribe(new ElementAtObserver<T>(t, index, defaultValue));
    }

    @Override
    public Observable<T> fuseToObservable() {
        return RxJavaPlugins.onAssembly(new ObservableElementAt<T>(source, index, defaultValue, true));
    }

    static final class ElementAtObserver<T> implements Observer<T>, Disposable {
        final SingleObserver<? super T> downstream;
        final long index;
        final T defaultValue;

        Disposable upstream;

        long count;

        boolean done;

        ElementAtObserver(SingleObserver<? super T> actual, long index, T defaultValue) {
            this.downstream = actual;
            this.index = index;
            this.defaultValue = defaultValue;
        }

        @Override
        public void onSubscribe(Disposable d) {
            if (DisposableHelper.validate(this.upstream, d)) {
                this.upstream = d;
                downstream.onSubscribe(this);
            }
        }

        @Override
        public void dispose() {
            upstream.dispose();
        }

        @Override
        public boolean isDisposed() {
            return upstream.isDisposed();
        }

        @Override
        public void onNext(T t) {
            if (done) {
                return;
            }
            long c = count;
            if (c == index) {
                done = true;
                upstream.dispose();
                downstream.onSuccess(t);
                return;
            }
            count = c + 1;
        }

        @Override
        public void onError(Throwable t) {
            if (done) {
                RxJavaPlugins.onError(t);
                return;
            }
            done = true;
            downstream.onError(t);
        }

        @Override
        public void onComplete() {
            if (!done) {
                done = true;

                T v = defaultValue;

                if (v != null) {
                    downstream.onSuccess(v);
                } else {
                    downstream.onError(new NoSuchElementException());
                }
            }
        }
    }
}
