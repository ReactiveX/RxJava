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

import java.util.Iterator;
import java.util.Objects;

import io.reactivex.rxjava3.annotations.Nullable;
import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.exceptions.Exceptions;
import io.reactivex.rxjava3.internal.disposables.EmptyDisposable;
import io.reactivex.rxjava3.internal.observers.BasicQueueDisposable;

public final class ObservableFromIterable<T> extends Observable<T> {
    final Iterable<? extends T> source;
    public ObservableFromIterable(Iterable<? extends T> source) {
        this.source = source;
    }

    @Override
    public void subscribeActual(Observer<? super T> observer) {
        Iterator<? extends T> it;
        try {
            it = source.iterator();
        } catch (Throwable e) {
            Exceptions.throwIfFatal(e);
            EmptyDisposable.error(e, observer);
            return;
        }
        boolean hasNext;
        try {
            hasNext = it.hasNext();
        } catch (Throwable e) {
            Exceptions.throwIfFatal(e);
            EmptyDisposable.error(e, observer);
            return;
        }
        if (!hasNext) {
            EmptyDisposable.complete(observer);
            return;
        }

        FromIterableDisposable<T> d = new FromIterableDisposable<T>(observer, it);
        observer.onSubscribe(d);

        if (!d.fusionMode) {
            d.run();
        }
    }

    static final class FromIterableDisposable<T> extends BasicQueueDisposable<T> {

        final Observer<? super T> downstream;

        final Iterator<? extends T> it;

        volatile boolean disposed;

        boolean fusionMode;

        boolean done;

        boolean checkNext;

        FromIterableDisposable(Observer<? super T> actual, Iterator<? extends T> it) {
            this.downstream = actual;
            this.it = it;
        }

        void run() {
            boolean hasNext;

            do {
                if (isDisposed()) {
                    return;
                }
                T v;

                try {
                    v = Objects.requireNonNull(it.next(), "The iterator returned a null value");
                } catch (Throwable e) {
                    Exceptions.throwIfFatal(e);
                    downstream.onError(e);
                    return;
                }

                downstream.onNext(v);

                if (isDisposed()) {
                    return;
                }
                try {
                    hasNext = it.hasNext();
                } catch (Throwable e) {
                    Exceptions.throwIfFatal(e);
                    downstream.onError(e);
                    return;
                }
            } while (hasNext);

            if (!isDisposed()) {
                downstream.onComplete();
            }
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
            if (done) {
                return null;
            }
            if (checkNext) {
                if (!it.hasNext()) {
                    done = true;
                    return null;
                }
            } else {
                checkNext = true;
            }

            return Objects.requireNonNull(it.next(), "The iterator returned a null value");
        }

        @Override
        public boolean isEmpty() {
            return done;
        }

        @Override
        public void clear() {
            done = true;
        }

        @Override
        public void dispose() {
            disposed = true;
        }

        @Override
        public boolean isDisposed() {
            return disposed;
        }
    }
}
