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
import io.reactivex.rxjava3.plugins.RxJavaPlugins;

public final class ObservableElementAt<T> extends AbstractObservableWithUpstream<T, T> {
    final long index;
    final T defaultValue;
    final boolean errorOnFewer;

    public ObservableElementAt(ObservableSource<T> source, long index, T defaultValue, boolean errorOnFewer) {
        super(source);
        this.index = index;
        this.defaultValue = defaultValue;
        this.errorOnFewer = errorOnFewer;
    }

    @Override
    public void subscribeActual(Observer<? super T> t) {
        source.subscribe(new ElementAtObserver<T>(t, index, defaultValue, errorOnFewer));
    }

    static final class ElementAtObserver<T> implements Observer<T>, Disposable {
        final Observer<? super T> downstream;
        final long index;
        final T defaultValue;
        final boolean errorOnFewer;

        Disposable upstream;

        long count;

        boolean done;

        ElementAtObserver(Observer<? super T> actual, long index, T defaultValue, boolean errorOnFewer) {
            this.downstream = actual;
            this.index = index;
            this.defaultValue = defaultValue;
            this.errorOnFewer = errorOnFewer;
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
                downstream.onNext(t);
                downstream.onComplete();
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
                if (v == null && errorOnFewer) {
                    downstream.onError(new NoSuchElementException());
                } else {
                    if (v != null) {
                        downstream.onNext(v);
                    }
                    downstream.onComplete();
                }
            }
        }
    }
}
