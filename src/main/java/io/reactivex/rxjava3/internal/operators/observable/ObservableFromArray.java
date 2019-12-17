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

import io.reactivex.rxjava3.annotations.Nullable;
import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.internal.observers.BasicQueueDisposable;

import java.util.Objects;

public final class ObservableFromArray<T> extends Observable<T> {
    final T[] array;
    public ObservableFromArray(T[] array) {
        this.array = array;
    }

    @Override
    public void subscribeActual(Observer<? super T> observer) {
        FromArrayDisposable<T> d = new FromArrayDisposable<T>(observer, array);

        observer.onSubscribe(d);

        if (d.fusionMode) {
            return;
        }

        d.run();
    }

    static final class FromArrayDisposable<T> extends BasicQueueDisposable<T> {

        final Observer<? super T> downstream;

        final T[] array;

        int index;

        boolean fusionMode;

        volatile boolean disposed;

        FromArrayDisposable(Observer<? super T> actual, T[] array) {
            this.downstream = actual;
            this.array = array;
        }

        @Override
        public int requestFusion(int mode) {
            if ((mode & SYNC) != 0) {
                fusionMode = true;
                return SYNC;
            }
            return NONE;
        }

        @Nullable
        @Override
        public T poll() {
            int i = index;
            T[] a = array;
            if (i != a.length) {
                index = i + 1;
                return Objects.requireNonNull(a[i], "The array element is null");
            }
            return null;
        }

        @Override
        public boolean isEmpty() {
            return index == array.length;
        }

        @Override
        public void clear() {
            index = array.length;
        }

        @Override
        public void dispose() {
            disposed = true;
        }

        @Override
        public boolean isDisposed() {
            return disposed;
        }

        void run() {
            T[] a = array;
            int n = a.length;

            for (int i = 0; i < n && !isDisposed(); i++) {
                T value = a[i];
                if (value == null) {
                    downstream.onError(new NullPointerException("The element at index " + i + " is null"));
                    return;
                }
                downstream.onNext(value);
            }
            if (!isDisposed()) {
                downstream.onComplete();
            }
        }
    }
}
